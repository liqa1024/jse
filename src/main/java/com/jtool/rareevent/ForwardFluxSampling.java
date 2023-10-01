package com.jtool.rareevent;


import com.jtool.atom.IAtomData;
import com.jtool.code.collection.AbstractCollections;
import com.jtool.math.MathEX;
import com.jtool.math.vector.ILogicalVector;
import com.jtool.math.vector.LogicalVector;
import com.jtool.parallel.AbstractThreadPool;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;
import com.jtool.parallel.IHasAutoShutdown;
import com.jtool.parallel.ParforThreadPool;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;

/**
 * 使用向前通量采样法（Forward Flux Sampling，FFS）来对稀有事件的概率进行采样；
 * 只用于技术上的简单实现，如果需要保留演化的路径信息请使用 BG
 * @author liqa
 * @param <T> 路径上每个点的类型，对于 lammps 模拟则是原子结构信息 {@link IAtomData}
 */
public class ForwardFluxSampling<T> extends AbstractThreadPool<ParforThreadPool> implements Runnable, IHasAutoShutdown {
    private final static int DEFAULT_MAX_PATH_MUL = 100;
    private final static double DEFAULT_CUTOFF = 0.01;
    private final static int DEFAULT_MAX_STAT_TIMES = 10;
    
    private final BufferedFullPathGenerator<T> mFullPathGenerator;
    
    private final IVector mSurfaces;
    private final double mSurfaceA;
    private final int mN0;
    private int mStep1Mul; // 过程 1 需要的点的数目的倍数，更高的值可以保证结果有更好的统计性能
    
    private final int mN; // 界面数目-1，即 n
    private final Random mRNG = new Random(); // 独立的随机数生成器
    
    private int mMaxPathNum; // 用来限制统计时间，（第二个过程）每步统计的最大路径数目，默认为 100 * N0
    private double mCutoff; // 用来将过低权重的点截断，将更多的资源用于统计高权重的点
    private int mMaxStatTimes; // 用于限制对高权重的点多次统计的次数，避免统计点过多
    
    private double mPruningProb; // 路径演化到上一界面后进行裁剪的概率，默认为 0.0（关闭），用于优化从中间界面回到 A 的长期路径
    private int mPruningThreshold; // 开始进行裁剪的阈值，用来消除噪声的影响，默认为 1（即不添加阈值）
    
    /**
     * 创建一个通用的 FFS 运算器
     * @author liqa
     * @param aPathGenerator 任意的路径生成器
     * @param aParameterCalculator 对于路径上一个点的 λ 的计算器
     * @param aThreadNum FFS 的并行数，默认为 1，不开启并行
     * @param aSurfaceA 对于 A 有一个专门的分界面，因为需要频繁使用因此专门拿出来，要求 {@code A <= λ0}
     * @param aSurfaces 分割相空间的分界面，有 {@code λ0 < λ1 < λ2 < ... < λn == B}
     * @param aN0 每个界面的统计数目
     */
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator, int aThreadNum, double aSurfaceA,                      IVector aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator), aThreadNum, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator, int aThreadNum, double aSurfaceA, Collection<? extends Number> aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator), aThreadNum, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator, int aThreadNum, double aSurfaceA,                     double[] aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator), aThreadNum, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator,                 double aSurfaceA,                      IVector aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator), 1, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator,                 double aSurfaceA, Collection<? extends Number> aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator), 1, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator,                 double aSurfaceA,                     double[] aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator), 1, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    
    ForwardFluxSampling(BufferedFullPathGenerator<T> aFullPathGenerator, int aThreadNum, double aSurfaceA, IVector aSurfaces, int aN0) {
        super(new ParforThreadPool(aThreadNum));
        
        mFullPathGenerator = aFullPathGenerator;
        mSurfaceA = aSurfaceA;
        // 检查界面输入是否合法
        if (aSurfaces.isEmpty()) throw new IllegalArgumentException("Surfaces Must at least have one element");
        double oLambda = Double.NaN;
        for (double tLambda : aSurfaces.iterable()) {
            if (Double.isNaN(oLambda)) {
                if (tLambda < mSurfaceA) throw new IllegalArgumentException("SurfaceA Must be the Lowest");
            } else {
                if (tLambda <= oLambda) throw new IllegalArgumentException("Surfaces Must be increasing");
            }
            oLambda = tLambda;
        }
        mSurfaces = aSurfaces;
        mN0 = aN0;
        mStep1Mul = 1;
        mMaxPathNum = DEFAULT_MAX_PATH_MUL * mN0;
        mCutoff = DEFAULT_CUTOFF;
        mMaxStatTimes = DEFAULT_MAX_STAT_TIMES;
        mPruningProb = 0.0;
        mPruningThreshold = 1;
        
        // 计算过程需要的量的初始化
        mN = mSurfaces.size() - 1;
        mStep2PointNum = Vectors.zeros(mN);
        mStep2PathNum = Vectors.zeros(mN);
        mPi = Vectors.NaN(mN);
        
        mPointsOnLambda = new ArrayList<>(mN0);
        oPointsOnLambda = new ArrayList<>(mN0);
        mMovedPoints = LogicalVector.zeros(mN0);
    }
    
    
    /** 参数设置，用来减少构造函数的重载数目，返回自身来支持链式调用 */
    public ForwardFluxSampling<T> setStep1Mul(int aStep1Mul) {mStep1Mul = Math.max(1, aStep1Mul); return this;}
    public ForwardFluxSampling<T> setMaxPathNum(int aMaxPathNum) {mMaxPathNum = Math.max(mN0, aMaxPathNum); return this;}
    public ForwardFluxSampling<T> setCutoff(double aCutoff) {mCutoff = MathEX.Code.toRange(0.0, 0.5, aCutoff); return this;}
    public ForwardFluxSampling<T> setMaxStatTimes(int aMaxStatTimes) {mMaxStatTimes = Math.max(1, aMaxStatTimes); return this;}
    public ForwardFluxSampling<T> setPruningProb(double aPruningProb) {mPruningProb = MathEX.Code.toRange(0.0, 1.0, aPruningProb); return this;}
    public ForwardFluxSampling<T> setPruningThreshold(int aPruningThreshold) {mPruningThreshold = Math.max(1, aPruningThreshold); return this;}
    /** 是否在关闭此实例时顺便关闭输入的生成器和计算器 */
    public ForwardFluxSampling<T> setDoNotShutdown(boolean aDoNotShutdown) {mFullPathGenerator.setDoNotShutdown(aDoNotShutdown); return this;}
    /** 可以从中间开始，此时则会直接跳过第一步（对于合法输入）*/
    public ForwardFluxSampling<T> setStep(int aStep, Iterable<? extends T> aPointsOnLambda, Map<?, ?> aRestData) {
        // 对于输入的合法性进行检测（界面需要兼容，这里只考虑现在省略了一些开头的界面以及完全不省略的情况）
        boolean tSurfaceCompat = true;
        if (aRestData.containsKey("surfaceA")) {
            double tSurfaceA = ((Number)aRestData.get("surfaceA")).doubleValue();
            if (!MathEX.Code.numericEqual(mSurfaceA, tSurfaceA)) {
                System.err.println("WARNING: surfaceA from restData("+tSurfaceA+") is not equal to the value from this instance("+mSurfaceA+")!!!");
                tSurfaceCompat = false;
            }
        } else {
            tSurfaceCompat = false;
        }
        // 设置一些已有的概率值
        if (tSurfaceCompat && aRestData.containsKey("surfaces") && aRestData.containsKey("prob")) {
            // 方便起见，这里不检查界面输入的合法性，也不考虑 nan 的情况（直接设置即可，因为原本也都是 nan）
            List<?> tSurfaces = (List<?>)aRestData.get("surfaces");
            List<?> tPi = (List<?>)aRestData.get("prob");
            // 统计可能省略的界面
            double tP0 = 1.0;
            double tLambda0 = mSurfaces.first();
            final Iterator<?> si = tSurfaces.iterator();
            final Iterator<?> pi = tPi.iterator();
            while (si.hasNext()) {
                double tSurface = ((Number)si.next()).doubleValue();
                if (MathEX.Code.numericEqual(tSurface, tLambda0)) {
                    // 现在已经将省略的界面对齐，跳出循环
                    break;
                } else {
                    // 此界面被省略，累计概率到 k0
                    if (MathEX.Code.numericLess(tSurface, tLambda0) && pi.hasNext()) {
                        Number tProb = (Number)pi.next();
                        tP0 *= (tProb==null ? Double.NaN : tProb.doubleValue());
                    } else {
                        System.err.println("WARNING: surfaces from restData is NOT compatible with the surfaces from this instance!!!");
                        tSurfaceCompat = false;
                        break;
                    }
                }
            }
            // 设置 k0
            if (tSurfaceCompat && aRestData.containsKey("k0")) {
                Number tK0 = (Number)aRestData.get("k0");
                mK0 = (tK0==null ? Double.NaN : tK0.doubleValue()) * tP0;
            }
            // 设置概率
            if (tSurfaceCompat) {
                for (int i = 0; i < mN && pi.hasNext() && si.hasNext(); ++i) {
                    if (MathEX.Code.numericEqual(((Number)si.next()).doubleValue(), mSurfaces.get_(i+1))) {
                        mPi.set_(i, ((Number)pi.next()).doubleValue());
                    } else {
                        System.err.println("WARNING: surfaces from restData is NOT compatible with the surfaces from this instance!!!");
                        //noinspection UnusedAssignment
                        tSurfaceCompat = false;
                        break;
                    }
                }
            }
        }
        // 设置此步的点以及附加属性
        mStep = aStep;
        mPointsOnLambda.clear();
        List<?> tLambdas = (List<?>)aRestData.get("lambdas");
        List<?> tMultiples = (List<?>)aRestData.get("multiples");
        int tIdx = 0;
        for (T tRawPoint : aPointsOnLambda) {
            Point tPoint = new Point(tRawPoint, ((Number)tLambdas.get(tIdx)).doubleValue(), ((Number)tMultiples.get(tIdx)).doubleValue());
            mPointsOnLambda.add(tPoint);
            ++tIdx;
        }
        return this;
    }
    
    
    /** 记录父节点的点，可以用来方便获取演化路径 */
    private class Point {
        /** 方便起见这里不用 OOP 结构，仅内部使用 */
        final @Nullable Point parent;
        final T value;
        final double lambda;
        /** 由于存在阶乘关系，需要使用 double 避免溢出 */
        double multiple;
        
        Point(@Nullable Point parent, T value, double lambda, double multiple) {
            this.parent = parent;
            this.value = value;
            this.lambda = lambda;
            this.multiple = multiple;
        }
        Point(Point parent) {
            this(parent, parent.value, parent.lambda, parent.multiple);
        }
        Point(Point parent, T value, double lambda) {
            this(parent, value, lambda, parent.multiple);
        }
        Point(T value, double lambda, double multiple) {
            this(null, value, lambda, multiple);
        }
        Point(T value, double lambda) {
            this(null, value, lambda, 1.0);
        }
    }
    
    /** 统计信息 */
    private double mN0Eff = 0; // N_{0}^{eff}, 第一个过程等价采样到的 N0 数目，这是考虑了 Pruning 的结果
    private double mTotTime0 = 0.0; // 第一个过程中的总时间，注意不是 A 第一次到达 λ0 的时间，因此得到的 mK0 不是 A 到 λ0 的速率
    private final List<Point> mPointsOnLambda, oPointsOnLambda; // 第一次从 A 到达 λi 的那些点
    private ILogicalVector mMovedPoints;
    
    private double mK0 = Double.NaN; // A 到 λ0 的轨迹通量，速率单位但是注意不是 A 到 λ0 的速率
    private final IVector mPi; // i 到 i+1 而不是返回 A 的概率
    
    private double mMi = 0;
    private double mNippEff = 0; // N_{i+1}^{eff}, 第一个过程等价采样到的 N_{i+1} 数目，这是考虑了 Pruning 的结果
    private int mPathNum = 0;
    
    /** 记录一下每个过程使用的点的数目 */
    private long mStep1PointNum = 0;
    private long mStep1PathNum = 0;
    private final IVector mStep2PointNum;
    private final IVector mStep2PathNum;
    
    /** 统计一个路径所有的从 A 第一次到达 λ0 的点以及这个过程总共花费的时间 */
    private void statA2Lambda0_() {
        long tStep1PointNum = 0;
        // 获取初始路径的迭代器
        ITimeAndParameterIterator<T> tPathInit = mFullPathGenerator.fullPathInit();
        T tRawPoint;
        double tLambda;
        // 不再需要检测 hasNext，内部保证永远都有 next
        // 首先找到到达 A 的起始位置，一般来说直接初始化的点都会在 A，但是不一定；这时需要特殊处理，不记录这段时间
        Point tRoot;
        // 这个过程也需要 Pruning，但是不考虑时间因此这里不需要记录倍率
        int tPruningIndex = mPruningThreshold;
        while (true) {
            tRawPoint = tPathInit.next();
            ++tStep1PointNum;
            // 检测是否到达 A
            tLambda = tPathInit.lambda();
            if (tLambda <= mSurfaceA) {
                // 记录根节点
                tRoot = new Point(tRawPoint, tLambda);
                break;
            }
            // 如果到达 B 则重新回到 A，这里直接 return 来实现
            if (tLambda >= mSurfaces.last()) {
                // 这里不保留消耗时间直接返回
                synchronized (this) {
                    mStep1PointNum += tStep1PointNum;
                    ++mStep1PathNum;
                }
                return;
            }
            // 修剪，如果向前则有概率直接中断
            if (mPruningProb > 0.0 && tPruningIndex < mSurfaces.size() && tLambda >= mSurfaces.get_(tPruningIndex)) {
                // 无论如何先增加 tPruningIndex
                ++tPruningIndex;
                if (mRNG.nextDouble() < mPruningProb) {
                    // 有 mPruningProb 中断，不保留消耗时间直接返回
                    synchronized (this) {
                        mStep1PointNum += tStep1PointNum;
                        ++mStep1PathNum;
                    }
                    return;
                }
                // 这里不需要记录权重
            }
        }
        
        // 开始一般情况处理
        // 这个过程也增加 Pruning
        tPruningIndex = mPruningThreshold;
        double tPruningMul = 1.0;
        // 先记录目前为止消耗的时间，不包含到统计中
        double oTimeConsumed = tPathInit.timeConsumed();
        while (true) {
            // 找到起始点后开始记录穿过 λ0 的点
            while (true) {
                tRawPoint = tPathInit.next();
                ++tStep1PointNum;
                // 判断是否有穿过 λ0
                tLambda = tPathInit.lambda();
                if (tLambda >= mSurfaces.first()) {
                    // 如果有穿过 λ0 则需要记录这些点
                    synchronized (this) {
                        mPointsOnLambda.add(new Point(tRoot, tRawPoint, tLambda));
                        mN0Eff += tPruningMul;
                    }
                    // 如果到达 B 则重新回到 A，这里直接 return 来实现（对于只有一个界面的情况）
                    if (tLambda >= mSurfaces.last()) {
                        // 重设路径之前记得先保存旧的时间
                        synchronized (this) {
                            mTotTime0 += (tPathInit.timeConsumed()-oTimeConsumed)*tPruningMul;
                            mStep1PointNum += tStep1PointNum;
                            ++mStep1PathNum;
                        }
                        return;
                    }
                    break;
                }
            }
            // 每记录一个点查看总数是否达标
            synchronized (this) {if (mPointsOnLambda.size() >= mN0*mStep1Mul) break;}
            // 再一直查找下次重新到达 A
            while (true) {
                tRawPoint = tPathInit.next();
                ++tStep1PointNum;
                // 检测是否到达 A
                tLambda = tPathInit.lambda();
                if (tLambda <= mSurfaceA) {
                    // 记录根节点
                    tRoot = new Point(tRawPoint, tLambda);
                    break;
                }
                // 如果到达 B 则重新回到 A，这里直接 return 来实现
                if (tLambda >= mSurfaces.last()) {
                    // 重设路径之前记得先保存旧的时间
                    synchronized (this) {
                        mTotTime0 += (tPathInit.timeConsumed()-oTimeConsumed)*tPruningMul;
                        mStep1PointNum += tStep1PointNum;
                        ++mStep1PathNum;
                    }
                    return;
                }
                // 修剪，如果向前则有概率直接中断
                if (mPruningProb > 0.0 && tPruningIndex < mSurfaces.size() && tLambda >= mSurfaces.get_(tPruningIndex)) {
                    // 无论如何先增加 tPruningIndex
                    ++tPruningIndex;
                    if (mRNG.nextDouble() < mPruningProb) {
                        // 有 mPruningProb 中断，记得先保存旧的时间
                        synchronized (this) {
                            mTotTime0 += (tPathInit.timeConsumed()-oTimeConsumed)*tPruningMul;
                            mStep1PointNum += tStep1PointNum;
                            ++mStep1PathNum;
                        }
                        return;
                    } else {
                        // 否则需要增加权重，在此之前先记录已经消耗的时间
                        double tTimeConsumed = tPathInit.timeConsumed();
                        synchronized (this) {mTotTime0 += (tTimeConsumed-oTimeConsumed)*tPruningMul;}
                        oTimeConsumed = tTimeConsumed;
                        // 增加权重，此段之后的时间消耗会增加权重
                        tPruningMul /= (1.0 - mPruningProb);
                    }
                }
            }
        }
        // 最后统计所有的耗时
        synchronized (this) {
            mTotTime0 += (tPathInit.timeConsumed()-oTimeConsumed)*tPruningMul;
            mStep1PointNum += tStep1PointNum;
            ++mStep1PathNum;
        }
    }
    
    /** 统计一个路径所有的从 λi 第一次到达 λi+1 的点 */
    private void statLambda2Next_() {
        double tLambdaNext = mSurfaces.get_(mStep+1);
        
        long tStep2PointNum = 0;
        // 统一获取开始点，并且串行处理第一个点的特殊情况，避免并行造成的问题
        Point tStart;
        synchronized (this) {
            // 采用直接遍历的方式减少误差，由于统计时考虑了权重这里不需要考虑权重
            int tIndex = mPathNum % oPointsOnLambda.size();
            tStart = oPointsOnLambda.get(tIndex);
            ++mPathNum;
            // 第一个点特殊处理，如果第一个点就已经穿过了 λi+1，则需要记录这个点，并且要保证这个点只会在下一个面上出现一次
            // 为了修正误差，会增加其倍数 multiple 来增加其统计时的权重
            ++tStep2PointNum;
            if (tStart.lambda >= tLambdaNext) {
                // 对于相同的点则通过增加 multiple 的方式增加其统计权重，同时保证样本多样性
                if (mMovedPoints.get(tIndex)) {
                    assert tStart.parent != null;
                    tStart.multiple += tStart.parent.multiple;
                } else {
                    tStart = new Point(tStart);
                    assert tStart.parent != null;
                    mPointsOnLambda.add(tStart);
                    oPointsOnLambda.set(tIndex, tStart);
                    mMovedPoints.set(tIndex, true);
                }
                mMi += tStart.parent.multiple;
                mNippEff += tStart.parent.multiple;
                synchronized (this) {
                    mStep2PointNum.add_(mStep, tStep2PointNum);
                    mStep2PathNum.set_(mStep, mPathNum);
                }
                return;
            }
        }
        // 从 tStart 开始统计到达下一个界面的概率，更高权重的进行更多的统计次数保证结果准确性
        int tStatTimes = tStart.multiple>2.0 ? (int)Math.min(Math.floor(tStart.multiple), mMaxStatTimes) : 1;
        double subMul = tStart.multiple / (double)tStatTimes;
        for (int i = 0; i < tStatTimes; ++i) {
            // Pruning 相关量需要每次循环重新设置
            int tPruningIndex = mStep-mPruningThreshold;
            double tPruningMul = 1.0;
            
            // 获取从 tStart 开始的路径的迭代器
            ITimeAndParameterIterator<T> tPathFrom = mFullPathGenerator.fullPathFrom(tStart.value);
            // 为了不改变约定，这里直接跳过上面已经经过特殊考虑的第一个相同的点
            tPathFrom.next();
            
            T tRawPoint;
            double tLambda;
            // 不再需要检测 hasNext，内部保证永远都有 next
            while (true) {
                tRawPoint = tPathFrom.next();
                ++tStep2PointNum;
                tLambda = tPathFrom.lambda();
                // 判断是否穿过了 λi+1
                if (tLambda >= tLambdaNext) {
                    // 如果有穿过 λi+1 则需要记录这些点
                    synchronized (this) {
                        mPointsOnLambda.add(new Point(tStart, tRawPoint, tLambda, subMul));
                        mMi += subMul;
                        mNippEff += subMul*tPruningMul;
                    }
                    break;
                }
                // 判断是否穿过了 A
                if (tLambda <= mSurfaceA) {
                    // 穿过 A 直接跳过结束这个路径即可
                    synchronized (this) {
                        mMi += subMul;
                    }
                    break;
                }
                // 修剪，如果向前则有概率直接中断
                if (mPruningProb > 0.0 && tPruningIndex >= 0 && tLambda <= mSurfaces.get_(tPruningIndex)) {
                    // 无论如何先减少 tPruningIndex
                    --tPruningIndex;
                    if (mRNG.nextDouble() < mPruningProb) {
                        // 有 mPruningProb 中断
                        synchronized (this) {
                            mMi += subMul;
                        }
                        break;
                    } else {
                        // 否则需要增加权重
                        tPruningMul /= (1.0 - mPruningProb);
                    }
                }
            }
            // 此时如果路径没有结束，还可以继续统计，即一个路径可以包含多个从 A 到 λi+1，或者包含之后的 λi+1 到 λi+2 等信息
            // 对于 FFS 的采样方式，考虑到存储这些路径需要的空间，这里不去做这些操作
            synchronized (this) {
                mStep2PointNum.add_(mStep, tStep2PointNum);
                mStep2PathNum.set_(mStep, mPathNum);
            }
        }
    }
    
    
    /** 一个简单的实现 */
    private int mStep = -1; // 记录运行的步骤，i
    private boolean mFinished = false;
    private boolean mStepFinished = true; // 标记此步骤是否正常完成结束而不是中断或者正在运行中
    public void run() {
        if (mFinished) return;
        // 实际分为两个过程，第一个过程首先统计轨迹通量（flux of trajectories）
        if (mStep < 0) {
            mStepFinished = false;
            // 初始化统计量
            mN0Eff = 0; mTotTime0 = 0.0;
            // 统计从 A 到达 λ0，运行直到达到次数超过 mN0*mStep1Mul
            pool().parwhile(() -> mPointsOnLambda.size()<mN0*mStep1Mul, this::statA2Lambda0_);
            // 获取第一个过程的统计结果
            mK0 = mN0Eff / mTotTime0;
            // 第一个过程完成（前提没有在运行过程中中断）
            if (!mFinished) {
                mStepFinished = true;
                mStep = 0;
                if (mStep >= mN) mFinished = true;
            }
            return;
        }
        // 第二个过程会从 λi 上的点随机选取运行直到到达 λi+1 或者返回 A，注意依旧需要将耗时增加到 mTotTime 中
        if (mStep < mN) {
            mStepFinished = false;
            // 截断过小的值，使用 TreeMap 记录最小的值的索引，保证截断符合约束
            double rCutoffValue = 0.0;
            NavigableMap<Double, Integer> rCutoffIndex = new TreeMap<>();
            for (int i = 0; i < mPointsOnLambda.size(); ++i) {
                Point tPoint = mPointsOnLambda.get(i);
                if (tPoint.multiple < mCutoff) {
                    rCutoffIndex.put(tPoint.multiple, i);
                    rCutoffValue += tPoint.multiple;
                }
                if (rCutoffValue > mCutoff*mN0 || rCutoffIndex.size()*2 > mN0) {
                    rCutoffValue -= rCutoffIndex.pollLastEntry().getKey();
                }
            }
            // 遍历两次将需要留下的设置到 oPointsOnLambda
            oPointsOnLambda.clear();
            if (mPointsOnLambda.size() > mMovedPoints.size()) {
                // 这里保证 mMovedPoints 长度永远合法
                mMovedPoints = LogicalVector.zeros(mPointsOnLambda.size()+nThreads());
            }
            mMovedPoints.fill(true);
            for (int index : rCutoffIndex.values()) mMovedPoints.set(index, false);
            for (int i = 0; i < mPointsOnLambda.size(); ++i) {
                if (mMovedPoints.get(i)) oPointsOnLambda.add(mPointsOnLambda.get(i));
            }
            mMovedPoints.fill(false);
            mPointsOnLambda.clear();
            // 打乱顺序
            Collections.shuffle(oPointsOnLambda, mRNG);
            
            // 初始化统计量
            mMi = 0; mNippEff = 0; mPathNum = 0;
            // 获取 M_i, N_{i+1}
            pool().parwhile(() -> (mPointsOnLambda.size()<mN0 && !mFinished), () -> {
                // 选取一个初始点获取之后的路径，并统计结果
                statLambda2Next_();
                synchronized (this) {
                    // 如果使用的路径数超过设定则直接退出
                    if (mPathNum > mMaxPathNum && !mFinished) {
                        System.err.println("ERROR: MaxPathNum("+mMaxPathNum+") reached, so the FFS is stopped.");
                        System.err.println("Try larger N0 or smaller surface distance.");
                        mFinished = true;
                    }
                }
            });
            // 归一化得到的面上所有的点的权重
            double rMean = 0.0;
            for (Point tPoint : mPointsOnLambda) rMean += tPoint.multiple;
            rMean /= mPointsOnLambda.size();
            for (Point tPoint : mPointsOnLambda) tPoint.multiple /= rMean;
            // 获取概率统计结果
            mPi.set_(mStep, mNippEff / mMi);
            // 第二个过程这一步完成（前提没有在运行过程中中断）
            if (!mFinished) {
                mStepFinished = true;
                ++mStep;
                if (mStep >= mN) mFinished = true;
            }
        }
    }
    public boolean finished() {return mFinished;}
    public boolean stepFinished() {return mStepFinished;}
    
    /** 获取结果的接口 */
    public double getProb(int aIdx) {return mPi.get(aIdx);}
    public double getK0() {return mK0;}
    public double getK() {return mK0 * mPi.prod();}
    
    public long step1PointNum() {return mStep1PointNum;}
    public long step1PathNum() {return mStep1PathNum;}
    public long step2PointNum(int aIdx) {return (long)mStep2PointNum.get(aIdx);}
    public long step2PathNum(int aIdx) {return (long)mStep2PathNum.get(aIdx);}
    public long totalPointNum() {return mStep1PointNum + (long)mStep2PointNum.sum();}
    public long totalPathNum() {return mStep1PathNum + (long)mStep2PathNum.sum();}
    
    /** 利用保存的 parent 获取演化路径 */
    public Deque<T> pickPath() {
        // 根据 multiple 为概率选择路径
        double tTotalMul = 0.0;
        for (Point tPoint : mPointsOnLambda) tTotalMul += tPoint.multiple;
        double tRand = mRNG.nextDouble() * tTotalMul;
        tTotalMul = 0.0;
        int idx = 0;
        for (; idx < mPointsOnLambda.size(); ++idx) {
            tTotalMul += mPointsOnLambda.get(idx).multiple;
            if (tTotalMul > tRand) break;
        }
        return pickPath(idx);
    }
    public Deque<T> pickPath(int aIdx) {
        Deque<T> rPath = new ArrayDeque<>();
        Point tPoint = mPointsOnLambda.get(aIdx);
        do {
            if (tPoint.value != rPath.peekFirst()) {
                rPath.addFirst(tPoint.value);
            }
            tPoint = tPoint.parent;
        } while (tPoint != null);
        return rPath;
    }
    public List<T> pointsOnLambda() {
        return AbstractCollections.map(mPointsOnLambda, point -> point.value);
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    public Map restData() {
        Map rSaveTo = new HashMap();
        rSaveTo.put("lambdas", AbstractCollections.map(mPointsOnLambda, point -> point.lambda));
        rSaveTo.put("multiples", AbstractCollections.map(mPointsOnLambda, point -> point.multiple));
        rSaveTo.put("k0", Double.isNaN(mK0) ? null : mK0);
        rSaveTo.put("prob", AbstractCollections.map(mPi.asList(), prob -> (Double.isNaN(prob) ? null : prob)));
        rSaveTo.put("surfaceA", mSurfaceA);
        rSaveTo.put("surfaces", mSurfaces.asList());
        return rSaveTo;
    }
    
    /**程序结束时会顺便关闭内部的 mFullPathGenerator，通过切换不同的 mFullPathGenerator 来调整实际输入的生成器是否会顺便关闭 */
    @Override public void shutdown() {super.shutdown(); mFullPathGenerator.shutdown();}
    /** 注意有些类（如线程池）的 close 逻辑不完全和 shutdown 相同，这里需要专门使用内部的 close */
    @VisibleForTesting @Override public void close() {super.close(); mFullPathGenerator.close();}
}
