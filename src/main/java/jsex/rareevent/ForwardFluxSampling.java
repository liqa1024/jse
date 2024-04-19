package jsex.rareevent;

import com.google.common.collect.ImmutableMap;
import jse.atom.IAtomData;
import jse.code.CS;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.vector.*;
import jse.parallel.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.util.*;

import static jse.code.CS.RANDOM;

/**
 * 使用向前通量采样法（Forward Flux Sampling，FFS）来对稀有事件的概率进行采样；
 * 只用于技术上的简单实现，如果需要保留演化的路径信息请使用 BG
 * @author liqa
 * @param <T> 路径上每个点的类型，对于 lammps 模拟则是原子结构信息 {@link IAtomData}
 */
@ApiStatus.Experimental
public class ForwardFluxSampling<T> extends AbstractThreadPool<ParforThreadPool> implements Runnable, IHasAutoShutdown {
    private final static long DEFAULT_MAX_PATH_MUL = 100;
    private final static double DEFAULT_CUTOFF = 0.01;
    private final static int DEFAULT_MAX_STAT_TIMES = 10;
    
    private final IFullPathGenerator<T> mFullPathGenerator;
    
    private final IVector mSurfaces;
    private final double mSurfaceA;
    private final int mN0;
    /** 过程 1 需要的点的数目的倍数，更高的值可以保证结果有更好的统计性能 */
    private int mStep1Mul;
    
    /** 界面数目-1，即 n */
    private final int mN;
    /** 可定义的主随机数生成器，默认为 {@link CS#RANDOM} */
    private Random mRNG = RANDOM;
    /** 此 FFS 是否是竞争性的，默认开启，当自定义了种子后会自动关闭保证结果一致 */
    private boolean mNoCompetitive = false;
    /** 是否在运行时显示进度条 */
    private boolean mProgressBar = false;
    
    /** 用来限制统计时间，（第二个过程）每步统计的最大路径数目，默认为 100 * N0 */
    private long mMaxPathNum;
    /** 用来将过低权重的点截断，将更多的资源用于统计高权重的点 */
    private double mCutoff;
    /** 用于限制对高权重的点多次统计的次数，避免统计点过多 */
    private int mMaxStatTimes;
    
    /** 路径演化到上一界面后进行裁剪的概率，默认为 0.0（关闭），用于优化从中间界面回到 A 的长期路径 */
    private double mPruningProb;
    /** 开始进行裁剪的阈值，用来消除噪声的影响，默认为 1（即不添加阈值） */
    private int mPruningThreshold;
    /** 是否开启第一个过程的剪枝，对于某些特殊情况第一过程剪枝会导致统计出现系统偏差，默认开启 */
    private boolean mStep1Pruning;
    
    /**
     * 创建一个通用的 FFS 运算器
     * @author liqa
     * @param aFullPathGenerator 任意的路径生成器
     * @param aThreadNum FFS 的并行数，默认为 1，不开启并行
     * @param aSurfaceA 对于 A 有一个专门的分界面，因为需要频繁使用因此专门拿出来，要求 {@code A <= λ0}
     * @param aSurfaces 分割相空间的分界面，有 {@code λ0 < λ1 < λ2 < ... < λn == B}
     * @param aN0 每个界面的统计数目
     */
    public ForwardFluxSampling(IFullPathGenerator<T> aFullPathGenerator, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, double aSurfaceA,                      IVector aSurfaces, int aN0) {this(true, aFullPathGenerator, aThreadNum, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IFullPathGenerator<T> aFullPathGenerator, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, double aSurfaceA, Collection<? extends Number> aSurfaces, int aN0) {this(true, aFullPathGenerator, aThreadNum, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IFullPathGenerator<T> aFullPathGenerator, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, double aSurfaceA,                     double[] aSurfaces, int aN0) {this(true, aFullPathGenerator, aThreadNum, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IFullPathGenerator<T> aFullPathGenerator,                                                      double aSurfaceA,                      IVector aSurfaces, int aN0) {this(true, aFullPathGenerator, 1, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IFullPathGenerator<T> aFullPathGenerator,                                                      double aSurfaceA, Collection<? extends Number> aSurfaces, int aN0) {this(true, aFullPathGenerator, 1, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IFullPathGenerator<T> aFullPathGenerator,                                                      double aSurfaceA,                     double[] aSurfaces, int aN0) {this(true, aFullPathGenerator, 1, aSurfaceA, Vectors.from(aSurfaces), aN0);}
    
    ForwardFluxSampling(boolean aFlag, IFullPathGenerator<T> aFullPathGenerator, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, double aSurfaceA, IVector aSurfaces, int aN0) {
        // FFS 这里固定采用非竞争的 ParforThreadPool，因为 parfor 都只有线程数的任务
        super(new ParforThreadPool(aThreadNum, true));
        
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
        mStep1Pruning = true;
        
        // 计算过程需要的量的初始化
        mN = mSurfaces.size() - 1;
        mStep2PointNum = LongVector.zeros(mN);
        mStep2PathNum = LongVector.zeros(mN);
        mPi = Vectors.NaN(mN);
        
        mPointsOnLambda = new ArrayList<>(mN0);
        oPointsOnLambda = new ArrayList<>(mN0);
        mMovedPoints = LogicalVector.zeros(mN0);
    }
    
    
    /** 参数设置，用来减少构造函数的重载数目，返回自身来支持链式调用 */
    public ForwardFluxSampling<T> setRNG(long aSeed) {mRNG = new Random(aSeed); mNoCompetitive = true; return this;}
    public ForwardFluxSampling<T> setStep1Mul(int aStep1Mul) {mStep1Mul = Math.max(1, aStep1Mul); return this;}
    public ForwardFluxSampling<T> setMaxPathNum(long aMaxPathNum) {mMaxPathNum = Math.max(mN0, aMaxPathNum); return this;}
    public ForwardFluxSampling<T> setCutoff(double aCutoff) {mCutoff = MathEX.Code.toRange(0.0, 0.5, aCutoff); return this;}
    public ForwardFluxSampling<T> setMaxStatTimes(int aMaxStatTimes) {mMaxStatTimes = Math.max(1, aMaxStatTimes); return this;}
    public ForwardFluxSampling<T> setPruningProb(double aPruningProb) {mPruningProb = MathEX.Code.toRange(0.0, 1.0, aPruningProb); return this;}
    public ForwardFluxSampling<T> setPruningThreshold(int aPruningThreshold) {mPruningThreshold = Math.max(1, aPruningThreshold); return this;}
    public ForwardFluxSampling<T> setStep1Pruning(boolean aStep1Pruning) {mStep1Pruning = aStep1Pruning; return this;}
    public ForwardFluxSampling<T> disableStep1Pruning() {return setStep1Pruning(false);}
    public ForwardFluxSampling<T> setNoCompetitive(boolean aNoCompetitive) {mNoCompetitive = aNoCompetitive; return this;}
    public ForwardFluxSampling<T> setNoCompetitive() {return setNoCompetitive(true);}
    public ForwardFluxSampling<T> setProgressBar(boolean aProgressBar) {mProgressBar = aProgressBar; return this;}
    public ForwardFluxSampling<T> setProgressBar() {return setProgressBar(true);}
    /** 是否在关闭此实例时顺便关闭输入的生成器和计算器 */
    private boolean mDoNotShutdown = false;
    @Override public ForwardFluxSampling<T> setDoNotShutdown(boolean aDoNotShutdown) {mDoNotShutdown = aDoNotShutdown; return this;}
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
                        tP0 *= UT.Code.doubleValue((Number)pi.next());
                    } else {
                        System.err.println("WARNING: surfaces from restData is NOT compatible with the surfaces from this instance!!!");
                        tSurfaceCompat = false;
                        break;
                    }
                }
            }
            // 设置 k0
            if (tSurfaceCompat && aRestData.containsKey("k0")) {
                mK0 = UT.Code.doubleValue((Number)aRestData.get("k0")) * tP0;
            }
            // 设置概率
            if (tSurfaceCompat) {
                for (int i = 0; i < mN && pi.hasNext() && si.hasNext(); ++i) {
                    if (MathEX.Code.numericEqual(((Number)si.next()).doubleValue(), mSurfaces.get(i+1))) {
                        mPi.set(i, UT.Code.doubleValue((Number)pi.next()));
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
        Point(T value, double lambda, double multiple) {
            this(null, value, lambda, multiple);
        }
        Point(T value, double lambda) {
            this(null, value, lambda, 1.0);
        }
    }
    
    /** 统计信息 */
    private final List<Point> mPointsOnLambda, oPointsOnLambda; // 第一次从 A 到达 λi 的那些点
    private ILogicalVector mMovedPoints;
    
    private double mK0 = Double.NaN; // A 到 λ0 的轨迹通量，速率单位但是注意不是 A 到 λ0 的速率
    private final IVector mPi; // i 到 i+1 而不是返回 A 的概率
    
    /** 记录一下每个过程使用的点的数目 */
    private long mStep1PointNum = 0;
    private long mStep1PathNum = 0;
    private final ILongVector mStep2PointNum;
    private final ILongVector mStep2PathNum;
    
    
    /** 期望向后运行直到 lambdaA 的路径，在向前运行时会进行剪枝，用于过程 1 使用 */
    private class BackwardPath implements IAutoShutdown {
        private final ITimeAndParameterIterator<? extends T> mPath;
        private final LocalRandom mRNG_;
        private @Nullable Point mCurrentPoint = null;
        private double mTimeConsumed = 0.0;
        private long mPointNum = 0;
        /** pruning stuffs */
        private int mPruningIndex = mPruningThreshold;
        private double mPruningMul = 1.0;
        
        private boolean mDead = false;
        @Override public void shutdown() {
            if (mDead) return;
            mPath.shutdown();
            mDead = true;
        }
        
        
        private BackwardPath(long aSeed) {
            mRNG_ = new LocalRandom(aSeed);
            // 现在自动构造这个路径，不直接使用传入的 aSeed 可以保证随机数生成器的独立性
            mPath = mFullPathGenerator.fullPathInit(mRNG_.nextLong());
        }
        double timeConsumed() {return mTimeConsumed;}
        long pointNum() {return mPointNum;}
        
        /** 运行 mPath 直到到达 aLambda0，没有另一种情况以及 pruning */
        @NotNull Point nextUntilReachLambda0() {
            if (mCurrentPoint == null || mDead) throw new RuntimeException();
            double oTimeConsumed = mPath.timeConsumed();
            Point tRoot = mCurrentPoint;
            while (true) {
                T tRawPoint = mPath.next(); ++mPointNum;
                // 判断是否有穿过 λ0
                double tLambda = mPath.lambda();
                if (tLambda >= mSurfaces.first()) {
                    // 记录耗时
                    mTimeConsumed += (mPath.timeConsumed() - oTimeConsumed) * tRoot.multiple;
                    // 如果有穿过 λ0 则返回这个点
                    mCurrentPoint = new Point(tRoot, tRawPoint, tLambda, tRoot.multiple);
                    return mCurrentPoint;
                }
            }
        }
        /** 运行 mPath 直到到达 aLambdaA 或者到达 aLambdaB，这里存在 pruning */
        @Nullable Point nextUntilReachLambdaAOrLambdaB(boolean aStatTime) {
            if (mDead) throw new RuntimeException();
            double oTimeConsumed = aStatTime ? mPath.timeConsumed() : Double.NaN;
            while (true) {
                T tRawPoint = mPath.next(); ++mPointNum;
                double tLambda = mPath.lambda();
                // 检测如果到达 A 则返回新的点
                if (tLambda <= mSurfaceA) {
                    // 记录耗时
                    if (aStatTime) mTimeConsumed += (mPath.timeConsumed() - oTimeConsumed) * mPruningMul;
                    // 到达 A 则返回新的点
                    mCurrentPoint = new Point(tRawPoint, tLambda, mPruningMul);
                    return mCurrentPoint;
                }
                // 检测如果到达 B 则返回 null 中断路径
                if (tLambda >= mSurfaces.last()) {
                    // 记录耗时
                    if (aStatTime) mTimeConsumed += (mPath.timeConsumed() - oTimeConsumed) * mPruningMul;
                    // 到达 B 则返回 null 中断路径
                    mCurrentPoint = null;
                    shutdown(); // 中断后不再有 path，从而让 next 相关操作非法
                    return null;
                }
                // pruning，如果向前则有概率直接中断，这里直接 return null 来标记 pruning 的情况
                if (mStep1Pruning && mPruningProb > 0.0 && mPruningIndex < mSurfaces.size() && tLambda >= mSurfaces.get(mPruningIndex)) {
                    // 无论如何先增加 mPruningIndex
                    ++mPruningIndex;
                    // 并且更新耗时
                    if (aStatTime) {
                        double tTimeConsumed = mPath.timeConsumed();
                        mTimeConsumed += (tTimeConsumed - oTimeConsumed) * mPruningMul;
                        oTimeConsumed = tTimeConsumed;
                    }
                    // 有 mPruningProb 中断，由 pruning 中断的情况直接返回 null
                    if (mRNG_.nextDouble() < mPruningProb) {
                        mCurrentPoint = null;
                        shutdown(); // 中断后不再有 path，从而让 next 相关操作非法
                        return null;
                    }
                    // 否则增加权重
                    mPruningMul /= (1.0 - mPruningProb);
                }
            }
        }
        @Nullable Point nextUntilReachLambdaAOrLambdaB() {return nextUntilReachLambdaAOrLambdaB(true);}
    }
    
    
    /** 记录过程 1 返回的信息，这样来保证方法的独立性 */
    private static class Step1Return {
        final double N0Eff; // N_{0}^{eff}, 第一个过程等价采样到的 N0 数目，这是考虑了 Pruning 的结果
        final double totTime0; // 第一个过程中的总时间，注意不是 A 第一次到达 λ0 的时间，因此得到的 mK0 不是 A 到 λ0 的速率
        final long pointNum;
        final long pathNum;
        
        Step1Return(double N0Eff, double totTime0, long pointNum, long pathNum) {
            this.N0Eff = N0Eff; this.totTime0 = totTime0;
            this.pointNum = pointNum; this.pathNum = pathNum;
        }
    }
    
    /** 统计一个路径所有的从 A 第一次到达 λ0 的点以及这个过程总共花费的时间 */
    Step1Return doStep1(int aN0, List<Point> rPointsOnLambda, long aSeed) {
        LocalRandom tRNG = new LocalRandom(aSeed);
        
        long rPathNum = 0;
        long rPointNum = 0;
        double rN0Eff = 0.0;
        double rTotTime0 = 0.0;
        // 获取初始路径的迭代器，并由此找到到达 A 的起始位置，一般来说直接初始化的点都会在 A，但是不一定
        BackwardPath tPath = new BackwardPath(tRNG.nextLong()); ++rPathNum;
        Point tRoot = tPath.nextUntilReachLambdaAOrLambdaB(false); // 此过程不会记录耗时
        try {
            while (tRoot == null) {
                rPointNum += tPath.pointNum();
                tPath.close();
                tPath = new BackwardPath(tRNG.nextLong()); ++rPathNum;
                tRoot = tPath.nextUntilReachLambdaAOrLambdaB(false); // 此过程不会记录耗时
            }
            
            // 开始一般情况处理
            while (true) {
                // 找到起始点后开始记录穿过 λ0 的点
                Point tPoint = tPath.nextUntilReachLambda0();
                if (!mNoCompetitive) {
                    // 如果是竞争的需要进行同步
                    synchronized (this) {
                        rPointsOnLambda.add(tPoint);
                    }
                } else {
                    rPointsOnLambda.add(tPoint);
                }
                rN0Eff += tPoint.multiple;
                if (mProgressBar) UT.Timer.progressBar();
                // 每记录一个点查看总数是否达标
                if (!mNoCompetitive) {
                    // 如果是竞争的需要进行同步
                    synchronized (this) {
                        if (rPointsOnLambda.size() >= aN0) break;
                    }
                } else {
                    if (rPointsOnLambda.size() >= aN0) break;
                }
                // 如果此时恰好到达 B 则重新回到 A（对于只有一个界面的情况）
                if (tPoint.lambda >= mSurfaces.last()) {
                    // 重设路径之前记得先保存旧的时间
                    rTotTime0 += tPath.timeConsumed();
                    // 重设路径以及根节点
                    do {
                        rPointNum += tPath.pointNum();
                        tPath.close();
                        tPath = new BackwardPath(tRNG.nextLong()); ++rPathNum;
                        tRoot = tPath.nextUntilReachLambdaAOrLambdaB(false); // 此过程不会记录耗时
                    } while (tRoot == null);
                    // 直接重新找下一个 λ0
                    continue;
                }
                // 再一直查找下次重新到达 A
                tRoot = tPath.nextUntilReachLambdaAOrLambdaB();
                // 如果没有回到 A 则需要重设路径
                if (tRoot==null || tRoot.lambda > mSurfaceA) {
                    // 重设路径之前记得先保存旧的时间
                    rTotTime0 += tPath.timeConsumed();
                    do {
                        rPointNum += tPath.pointNum();
                        tPath.close();
                        tPath = new BackwardPath(tRNG.nextLong()); ++rPathNum;
                        tRoot = tPath.nextUntilReachLambdaAOrLambdaB(false); // 此过程不会记录耗时
                    } while (tRoot == null);
                }
            }
            // 最后统计所有的耗时
            rTotTime0 += tPath.timeConsumed();
            rPointNum += tPath.pointNum();
        } finally {
            tPath.close();
        }
        return new Step1Return(rN0Eff, rTotTime0, rPointNum, rPathNum);
    }
    
    
    /** 期望向前运行直到下一个界面的路径，在向后运行时会进行剪枝，用于过程 2 使用 */
    private class ForwardPath implements IAutoShutdown {
        private final ITimeAndParameterIterator<? extends T> mPath;
        private final LocalRandom mRNG_;
        private final Point mStart;
        private long mPointNum = 0;
        /** pruning stuffs */
        private int mPruningIndex = mStep-mPruningThreshold;
        private double mPruningMul = 1.0;
        
        private boolean mDead = false;
        @Override public void shutdown() {
            if (mDead) return;
            mPath.shutdown();
            mDead = true;
        }
        
        private ForwardPath(Point aStart, long aSeed) {
            mRNG_ = new LocalRandom(aSeed);
            mStart = aStart;
            // 现在自动构造这个路径，不直接使用传入的 aSeed 可以保证随机数生成器的独立性
            mPath = mFullPathGenerator.fullPathFrom(aStart.value, mRNG_.nextLong());
        }
        long pointNum() {return mPointNum;}
        
        /** 运行 mPath 直到到达 aLambdaNext 或者到达 aLambdaA，这里存在 pruning */
        @Nullable Point nextUntilReachLambdaNextOrLambdaA() {
            if (mDead) throw new RuntimeException();
            final double tLambdaNext = mSurfaces.get(mStep+1);
            // 为了不改变约定，这里直接跳过已经经过特殊考虑的第一个相同的点
            mPath.next();
            // 不再需要检测 hasNext，内部保证永远都有 next
            while (true) {
                T tRawPoint = mPath.next(); ++mPointNum;
                double tLambda = mPath.lambda();
                // 判断是否穿过了 λi+1
                if (tLambda >= tLambdaNext) {
                    // 如果有穿过 λi+1 则需要记录这些点，现在 pruning 的倍率也会记录到这里
                    return new Point(mStart, tRawPoint, tLambda, mStart.multiple*mPruningMul);
                }
                // 判断是否穿过了 A
                if (tLambda <= mSurfaceA) {
                    // 穿过 A 直接返回 null
                    shutdown(); // 中断后不再有 path，从而让 next 相关操作非法
                    return null;
                }
                // 修剪，如果向前则有概率直接中断，现在第二个过程的修剪概率存在一个上限
                if (mPruningProb > 0.0 && mPruningIndex >= 0 && tLambda <= mSurfaces.get(mPruningIndex)) {
                    double tPruningProb = Math.min(mPruningProb, 1.0-getProb(mPruningIndex));
                    // 无论如何先减少 mPruningIndex
                    --mPruningIndex;
                    if (tPruningProb > 0.0) {
                        if (mRNG_.nextDouble() < tPruningProb) {
                            // 有 mPruningProb 中断，直接返回 null
                            shutdown(); // 中断后不再有 path，从而让 next 相关操作非法
                            return null;
                        } else {
                            // 否则需要增加权重
                            mPruningMul /= (1.0 - tPruningProb);
                        }
                    }
                }
            }
        }
    }
    
    /** 记录过程 2 返回的信息，这样来保证方法的独立性 */
    private static class Step2Return {
        final double Mi;
        final double NippEff; // // N_{i+1}^{eff}, 第一个过程等价采样到的 N_{i+1} 数目，这是考虑了 Pruning 的结果
        final long pointNum;
        final long pathNum;
        final boolean reachMaxPathNum;
        
        Step2Return(double Mi, double NippEff, long pointNum, long pathNum, boolean reachMaxPathNum) {
            this.Mi = Mi; this.NippEff = NippEff;
            this.pointNum = pointNum; this.pathNum = pathNum;
            this.reachMaxPathNum = reachMaxPathNum;
        }
    }
    
    /** 统计一个路径所有的从 λi 第一次到达 λi+1 的点 */
    private Step2Return doStep2(int aNipp, List<Point> rPointsOnLambda, long aMaxPathNum, long aSeed) {
        final double tLambdaNext = mSurfaces.get(mStep+1);
        LocalRandom tRNG = new LocalRandom(aSeed);
        
        long rPathNum = 0;
        long rPointNum = 0;
        double rMi = 0.0;
        double rNippEff = 0.0;
        boolean rReachMaxPathNum = false;
        
        while (true) {
            // 获取开始点
            Point tStart;
            // 由于涉及了标记的修改过程，这里需要串行处理；虽然涉及了线程间竞争的问题，但是这个不会影响结果
            synchronized (this) {
                // 现在统一改回随机获取，由于统计时考虑了权重这里不需要考虑权重
                int tIndex = tRNG.nextInt(oPointsOnLambda.size());
                tStart = oPointsOnLambda.get(tIndex); ++rPointNum;
                // 第一个点特殊处理，如果第一个点就已经穿过了 λi+1，则需要记录这个点，并且要保证这个点只会在下一个面上出现一次；
                // 为了修正误差，会增加其倍数 multiple 来增加其统计时的权重
                if (tStart.lambda >= tLambdaNext) {
                    // 对于相同的点则通过增加 multiple 的方式增加其统计权重，同时保证样本多样性
                    if (mMovedPoints.get(tIndex)) {
                        assert tStart.parent != null;
                        tStart.multiple += tStart.parent.multiple;
                    } else {
                        tStart = new Point(tStart);
                        assert tStart.parent != null;
                        rPointsOnLambda.add(tStart);
                        oPointsOnLambda.set(tIndex, tStart);
                        mMovedPoints.set(tIndex, true);
                        if (mProgressBar) UT.Timer.progressBar();
                    }
                    // 此路径结束，增加统计结果，将 tStart 设为 null 标记需要重新选取
                    rMi += tStart.parent.multiple;
                    rNippEff += tStart.parent.multiple;
                    ++rPathNum;
                    tStart = null;
                }
            }
            // 构造向前路径
            if (tStart != null) try (ForwardPath tPath = new ForwardPath(tStart, tRNG.nextLong())) {
                // 获取下一个点
                Point tNext = tPath.nextUntilReachLambdaNextOrLambdaA();
                // 无论什么结果都需要增加 Mi
                rMi += tStart.multiple;
                ++rPathNum;
                rPointNum += tPath.pointNum();
                // 如果有穿过 λi+1 则需要记录这些点并增加 rNippEff
                if (tNext != null) {
                    if (!mNoCompetitive) {
                        // 如果是竞争的需要进行同步
                        synchronized (this) {
                            rPointsOnLambda.add(tNext);
                        }
                    } else {
                        rPointsOnLambda.add(tNext);
                    }
                    rNippEff += tNext.multiple;
                    if (mProgressBar) UT.Timer.progressBar();
                }
            }
            // 这里统一检测点的总数是否达标，以及中断条件
            if (!mNoCompetitive) {
                // 如果是竞争的需要进行同步
                synchronized (this) {
                    if (rPointsOnLambda.size() >= aNipp) break;
                }
            } else {
                if (rPointsOnLambda.size() >= aNipp) break;
            }
            // 如果使用的路径数超过设定也直接退出
            if (rPathNum > aMaxPathNum) {rReachMaxPathNum = true; break;}
        }
        
        return new Step2Return(rMi, rNippEff, rPointNum, rPathNum, rReachMaxPathNum);
    }
    
    private long[] genSeeds_(int aSize) {
        long[] rSeeds = new long[aSize];
        for (int i = 0; i < aSize; ++i) rSeeds[i] = mRNG.nextLong();
        return rSeeds;
    }
    
    /** 一个简单的实现 */
    private int mStep = -1; // 记录运行的步骤，i
    private boolean mFinished = false;
    private boolean mStepFinished = true; // 标记此步骤是否正常完成结束而不是中断或者正在运行中
    @SuppressWarnings("unchecked")
    public void run() {
        if (mFinished) return;
        // 实际分为两个过程，第一个过程首先统计轨迹通量（flux of trajectories）
        if (mStep < 0) {
            mStepFinished = false;
            
            int tThreadNum = pool().threadNumber();
            // 每个线程独立的返回值
            final Step1Return[] tStep1ReturnBuffer = new Step1Return[tThreadNum];
            // 统计从 A 到达 λ0，运行直到采集到的点数目超过 mN0*mStep1Mul
            if (!mNoCompetitive) {
                // 竞争的写法，每个线程共用 mPointsOnLambda 并同步检测容量是否达标
                // 在竞争的情况下不需要统一生成种子
                if (mProgressBar) UT.Timer.progressBar("step1", (long)mN0*mStep1Mul);
                pool().parfor(tThreadNum, i -> {
                    tStep1ReturnBuffer[i] = doStep1(mN0*mStep1Mul, mPointsOnLambda, mRNG.nextLong());
                });
            } else {
                // 非竞争的写法，每个线程都分别采集到 mN0*mStep1Mul/nThreads 才算结束
                final int subN0 = MathEX.Code.divup(mN0*mStep1Mul, tThreadNum);
                // 每个线程存放到独立的点 List 上
                final List<Point>[] tPointsOnLambdaBuffer = (List<Point>[]) new List[tThreadNum];
                tPointsOnLambdaBuffer[0] = mPointsOnLambda;
                for (int i = 1; i < tThreadNum; ++i) tPointsOnLambdaBuffer[i] = new ArrayList<>(subN0);
                // 为了保证结果可重复，这里统一为每个线程生成一个种子，用于内部创建 LocalRandom
                final long[] tSeeds = genSeeds_(tThreadNum);
                if (mProgressBar) UT.Timer.progressBar("step1", (long)subN0*tThreadNum);
                pool().parfor(tThreadNum, i -> {
                    tStep1ReturnBuffer[i] = doStep1(subN0, tPointsOnLambdaBuffer[i], tSeeds[i]);
                });
                for (int i = 1; i < tThreadNum; ++i) mPointsOnLambda.addAll(tPointsOnLambdaBuffer[i]);
            }
            // 获取第一个过程的统计结果
            double rN0Eff = 0, rTotTime0 = 0.0;
            for (Step1Return tStep1Return : tStep1ReturnBuffer) {
                rN0Eff += tStep1Return.N0Eff;
                rTotTime0 += tStep1Return.totTime0;
                mStep1PathNum += tStep1Return.pathNum;
                mStep1PointNum += tStep1Return.pointNum;
            }
            mK0 = rN0Eff / rTotTime0;
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
            // 截断过小的值，这里简单起见直接将其按照 multiple 降序排序然后进行截断
            mPointsOnLambda.sort(Comparator.comparingDouble((Point p) -> p.multiple).reversed());
            double rCutoffValue = 0.0;
            while (mPointsOnLambda.size()*3 > mN0*2) {
                rCutoffValue += UT.Code.last(mPointsOnLambda).multiple;
                if (rCutoffValue > mCutoff*mN0) break;
                UT.Code.removeLast(mPointsOnLambda);
            }
            // 遍历添加到 oPointsOnLambda，现在顺便统一将下一步会开始的点的高权重的点进行拆分，拥有更多次数的统计来得到更好的统计效果
            oPointsOnLambda.clear();
            double tLambdaNext = mSurfaces.get(mStep+1);
            for (Point tPoint : mPointsOnLambda) {
                if (tPoint.lambda<tLambdaNext && tPoint.multiple>2.0) {
                    // 减少倍率并增加拷贝样本
                    int tStatTimes = Math.min(MathEX.Code.floor2int(tPoint.multiple), mMaxStatTimes);
                    if (tStatTimes > 1) {
                        //noinspection RedundantCast
                        tPoint.multiple /= (double)tStatTimes;
                    }
                    for (int i = 0; i < tStatTimes; ++i) oPointsOnLambda.add(tPoint);
                } else {
                    // 正常转移
                    oPointsOnLambda.add(tPoint);
                }
            }
            mPointsOnLambda.clear();
            // 这里保证 mMovedPoints 长度永远合法
            if (oPointsOnLambda.size() > mMovedPoints.size()) {
                mMovedPoints = LogicalVector.zeros(oPointsOnLambda.size()+ threadNumber());
            }
            mMovedPoints.fill(false);
            
            int tThreadNum = pool().threadNumber();
            // 每个线程独立的返回值
            final Step2Return[] tStep2ReturnBuffer = new Step2Return[tThreadNum];
            // 统计从 λi 第一次到达 λi+1 的点
            if (!mNoCompetitive) {
                // 竞争的写法，每个线程共用 mPointsOnLambda 并同步检测容量是否达标
                final long subMaxPathNum = MathEX.Code.divup(mMaxPathNum, tThreadNum);
                // 在竞争的情况下不需要统一生成种子
                if (mProgressBar) UT.Timer.progressBar("step2, i="+mStep, mN0);
                pool().parfor(tThreadNum, i -> {
                    tStep2ReturnBuffer[i] = doStep2(mN0, mPointsOnLambda, subMaxPathNum, mRNG.nextLong());
                });
            } else {
                // 非竞争的方式，每个线程都分别采集到 mN0/nThreads 才算结束
                final int subNipp = MathEX.Code.divup(mN0, tThreadNum);
                final long subMaxPathNum = MathEX.Code.divup(mMaxPathNum, tThreadNum);
                // 每个线程存放到独立的点 List 上
                final List<Point>[] tPointsOnLambdaBuffer = (List<Point>[]) new List[tThreadNum];
                tPointsOnLambdaBuffer[0] = mPointsOnLambda;
                for (int i = 1; i < tThreadNum; ++i) tPointsOnLambdaBuffer[i] = new ArrayList<>(subNipp);
                // 为了保证结果可重复，这里统一为每个线程生成一个种子，用于内部创建 LocalRandom
                final long[] tSeeds = genSeeds_(tThreadNum);
                if (mProgressBar) UT.Timer.progressBar("step2, i="+mStep, (long)subNipp*tThreadNum);
                pool().parfor(tThreadNum, i -> {
                    tStep2ReturnBuffer[i] = doStep2(subNipp, tPointsOnLambdaBuffer[i], subMaxPathNum, tSeeds[i]);
                });
                for (int i = 1; i < tThreadNum; ++i) mPointsOnLambda.addAll(tPointsOnLambdaBuffer[i]);
            }
            // 获取第二个过程的统计结果
            double rMi = 0, rNippEff = 0.0;
            for (Step2Return tStep2Return : tStep2ReturnBuffer) {
                rMi += tStep2Return.Mi;
                rNippEff += tStep2Return.NippEff;
                mStep2PathNum.add(mStep, tStep2Return.pathNum);
                mStep2PointNum.add(mStep, tStep2Return.pointNum);
                // 如果使用的路径数超过设定则直接退出
                if (tStep2Return.reachMaxPathNum && !mFinished) {
                    System.err.println("ERROR: MaxPathNum("+mMaxPathNum+") reached, so the FFS is stopped.");
                    System.err.println("Try larger N0 or smaller surface distance.");
                    mFinished = true;
                }
            }
            // 获取概率统计结果
            mPi.set(mStep, rNippEff / rMi);
            
            // 归一化得到的面上所有的点的权重
            double rMean = 0.0;
            for (Point tPoint : mPointsOnLambda) rMean += tPoint.multiple;
            rMean /= mPointsOnLambda.size();
            for (Point tPoint : mPointsOnLambda) tPoint.multiple /= rMean;
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
    public long step2PointNum(int aIdx) {return mStep2PointNum.get(aIdx);}
    public long step2PathNum(int aIdx) {return mStep2PathNum.get(aIdx);}
    public long totalPointNum() {return mStep1PointNum + mStep2PointNum.sum();}
    public long totalPathNum() {return mStep1PathNum + mStep2PathNum.sum();}
    
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
    
    public Map<String, Object> restData() {
        return ImmutableMap.<String, Object>builder()
            .put("lambdas", AbstractCollections.map(mPointsOnLambda, point -> point.lambda))
            .put("multiples", AbstractCollections.map(mPointsOnLambda, point -> point.multiple))
            .put("k0", Double.isNaN(mK0) ? null : mK0)
            .put("prob", AbstractCollections.map(mPi.asList(), prob -> (Double.isNaN(prob) ? null : prob)))
            .put("surfaceA", mSurfaceA)
            .put("surfaces", mSurfaces.asList())
            .build();
    }
    
    /** 程序结束时会顺便关闭内部的 mFullPathGenerator */
    @Override public void shutdown() {super.shutdown(); if (!mDoNotShutdown) mFullPathGenerator.shutdown();}
    /** ParforThreadPool close 时不需要 awaitTermination */
    @ApiStatus.Internal @Override public void close() {super.shutdown(); if (!mDoNotShutdown) mFullPathGenerator.close();}
}
