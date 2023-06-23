package com.jtool.rareevent;


import com.jtool.atom.IHasAtomData;
import com.jtool.parallel.IAutoShutdown;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 使用向前通量采样法（Forward Flux Sampling，FFS）来对稀有事件的概率进行采样；
 * 只用于技术上的简单实现，如果需要保留演化的路径信息请使用 BG
 * @author liqa
 * @param <T> 路径上每个点的类型，对于 lammps 模拟则是原子结构信息 {@link IHasAtomData}
 */
public class ForwardFluxSampling<T> implements Runnable, IAutoShutdown {
    private final static double DEFAULT_MIN_PROB = 0.05;
    
    private final BufferedFullPathGenerator<T> mFullPathGenerator;
    
    private final IVector mSurfaces;
    private final double mSurfaceA;
    private final int mN0;
    
    private final int mN; // 界面数目-1，即 n
    private final Random mRNG = new Random(); // 独立的随机数生成器
    
    private double mMinProb; // 用来限制统计时间，第二个过程每步的最低概率，默认为 max(0.05, 1/N0)，无论如何不会低于 1/N0
    
    /**
     * 创建一个通用的 FFS 运算器
     * @author liqa
     * @param aPathGenerator 任意的路径生成器
     * @param aParameterCalculator 对于路径上一个点的 λ 的计算器
     * @param aParameterThreadNum 参数计算的并行数目
     * @param aSurfaceA 对于 A 有一个专门的分界面，因为需要频繁使用因此专门拿出来，要求 A <= λ0
     * @param aSurfaces 分割相空间的分界面，有 λ0 < λ1 < λ2 < ... < λn == B
     * @param aN0 每个界面的统计数目
     */
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator, int aParameterThreadNum, double aSurfaceA,                      IVector aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator, aParameterThreadNum), aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator, int aParameterThreadNum, double aSurfaceA, Collection<? extends Number> aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator, aParameterThreadNum), aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator, int aParameterThreadNum, double aSurfaceA,                     double[] aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator, aParameterThreadNum), aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator,                          double aSurfaceA,                      IVector aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator                     ), aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator,                          double aSurfaceA, Collection<? extends Number> aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator                     ), aSurfaceA, Vectors.from(aSurfaces), aN0);}
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator,                          double aSurfaceA,                     double[] aSurfaces, int aN0) {this(new BufferedFullPathGenerator<>(aPathGenerator, aParameterCalculator                     ), aSurfaceA, Vectors.from(aSurfaces), aN0);}
    
    ForwardFluxSampling(BufferedFullPathGenerator<T> aFullPathGenerator, double aSurfaceA, IVector aSurfaces, int aN0) {
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
        mMinProb = Math.max(DEFAULT_MIN_PROB, 1.0/(double)mN0);
        
        // 计算过程需要的量的初始化
        mN = mSurfaces.size() - 1;
        mStep2PointNum = Vectors.zeros(mN);
        mPi = Vectors.NaN(mN);
    }
    
    
    /** 参数设置，用来减少构造函数的重载数目，返回自身来支持链式调用 */
    public ForwardFluxSampling<T> setMinProb(double aMinProb) {mMinProb = Math.max(aMinProb, 1.0/(double)mN0); return this;}
    /** 是否在关闭此实例时顺便关闭输入的生成器和计算器 */
    public ForwardFluxSampling<T> setDoNotClose(boolean aDoNotClose) {mFullPathGenerator.setDoNotClose(aDoNotClose); return this;}
    /** 可以从中间开始，此时则会直接跳过第一步（对于合法输入） */
    public ForwardFluxSampling<T> setStep(int aStep, Iterable<? extends T> aPointsOnLambda) {
        mStep = aStep;
        mPointsOnLambda.clear();
        for (T tPoint : aPointsOnLambda) mPointsOnLambda.add(new Point(tPoint));
        return this;
    }
    
    /** 记录父节点的点，可以用来方便获取演化路径 */
    private class Point {
        /** 方便起见这里不用 OOP 结构，仅内部使用 */
        final @Nullable Point parent;
        final T value;
        Point(@Nullable Point parent, T value) {this.parent = parent; this.value = value;}
        Point(T aValue) {this(null, aValue);}
    }
    
    /** 统计信息 */
    private double mTotTime0 = 0.0; // 第一个过程中的总时间，注意不是 A 第一次到达 λ0 的时间，因此得到的 mK0 不是 A 到 λ0 的速率
    private List<Point> mPointsOnLambda = new ArrayList<>(), oPointsOnLambda = new ArrayList<>(); // 第一次从 A 到达 λi 的那些点
    
    private double mK0 = Double.NaN; // A 到 λ0 的轨迹通量，速率单位但是注意不是 A 到 λ0 的速率
    private final IVector mPi; // i 到 i+1 而不是返回 A 的概率
    
    /** 记录一下每个过程使用的点的数目 */
    private int mStep1PointNum = 0;
    private final IVector mStep2PointNum;
    
    /** 统计一个路径所有的从 A 第一次到达 λ0 的点以及这个过程总共花费的时间 */
    private int statA2Lambda0_() {
        int tStep1PointNum = 0;
        // 获取初始路径的迭代器
        ITimeAndParameterIterator<T> tPathInit = mFullPathGenerator.fullPathInit();
        T tPoint;
        double tLambda;
        // 不再需要检测 hasNext，内部保证永远都有 next
        while (mPointsOnLambda.size() < mN0) {
            // 首先找到到达 A 的起始位置，一般来说直接初始化的点都会在 A，但是不一定
            Point tRoot;
            while (true) {
                tPoint = tPathInit.next();
                ++tStep1PointNum;
                // 检测是否到达 A
                tLambda = tPathInit.lambda();
                if (tLambda <= mSurfaceA) {
                    // 记录根节点
                    tRoot = new Point(tPoint);
                    break;
                }
                // 如果到达 B 则重新回到 A，这里使用重新获取 PathInit 来实现，这样保证永远都会回到 A
                if (tLambda >= mSurfaces.last()) {
                    // 重设路径之前记得先保存旧的时间
                    mTotTime0 += tPathInit.timeConsumed();
                    tPathInit = mFullPathGenerator.fullPathInit();
                }
            }
            // 找到起始点后开始记录穿过 λ0 的点
            while (true) {
                tPoint = tPathInit.next();
                ++tStep1PointNum;
                // 判断是否有穿过 λ0
                tLambda = tPathInit.lambda();
                if (tLambda >= mSurfaces.first()) {
                    // 如果有穿过 λ0 则需要记录这些点
                    mPointsOnLambda.add(new Point(tRoot, tPoint));
                    break;
                }
            }
            // 跳出后回到最初，需要一直查找下次重新到达 A 才开始统计
        }
        // 最后统计所有的耗时
        mTotTime0 += tPathInit.timeConsumed();
        return tStep1PointNum;
    }
    
    /** 统计一个路径所有的从 λi 第一次到达 λi+1 的点 */
    private int statLambda2Next_(Point aStart, double aLambdaNext) {
        int tStep2PointNum = 0;
        // 获取从 aStart 开始的路径的迭代器
        ITimeAndParameterIterator<T> tPathFrom = mFullPathGenerator.fullPathFrom(aStart.value);
        
        T tPoint;
        double tLambda;
        // 不再需要检测 hasNext，内部保证永远都有 next
        while (true) {
            tPoint = tPathFrom.next();
            ++tStep2PointNum;
            tLambda = tPathFrom.lambda();
            // 判断是否穿过了 λi+1
            if (tLambda >= aLambdaNext) {
                // 如果有穿过 λi+1 则需要记录这些点
                mPointsOnLambda.add(new Point(aStart, tPoint));
                break;
            }
            // 判断是否穿过了 A
            if (tLambda <= mSurfaceA) {
                // 穿过 A 直接跳过结束这个路径即可
                break;
            }
        }
        // 此时如果路径没有结束，还可以继续统计，即一个路径可以包含多个从 A 到 λi+1，或者包含之后的 λi+1 到 λi+2 等信息
        // 对于 FFS 的采样方式，考虑到存储这些路径需要的空间，这里不去做这些操作
        return tStep2PointNum;
    }
    
    
    /** 一个简单的实现，目前暂时不考虑并行的情况 */
    private int mStep = -1; // 记录运行的步骤，i
    private boolean mFinished = false;
    public void run() {
        if (mFinished) return;
        // 实际分为两个过程，第一个过程首先统计轨迹通量（flux of trajectories）
        if (mStep < 0) {
            // 统计从 A 到达 λ0，运行直到达到次数超过 mN0
            mStep1PointNum += statA2Lambda0_();
            // 获取第一个过程的统计结果
            mK0 = mPointsOnLambda.size() / mTotTime0;
            // 第一个过程完成
            mStep = 0;
        } else
        // 第二个过程会从 λi 上的点随机选取运行直到到达 λi+1 或者返回 A，注意依旧需要将耗时增加到 mTotTime 中
        if (mStep < mN) {
            // 先将上一步得到的 λi 交换到 oPointsOnLambda 作为初始
            List<Point> tPointsOnLambda = oPointsOnLambda;
            oPointsOnLambda = mPointsOnLambda;
            mPointsOnLambda = tPointsOnLambda; mPointsOnLambda.clear();
            // 获取 Mi
            int tMi = 0;
            while (mPointsOnLambda.size() < mN0) {
                // 随机选取一个初始点获取之后的路径，并统计结果
                Point tPointI = oPointsOnLambda.get(mRNG.nextInt(oPointsOnLambda.size()));
                mStep2PointNum.add_(mStep, statLambda2Next_(tPointI, mSurfaces.get_(mStep+1)));
                ++tMi;
                // 如果统计得到的概率小于预定值，那么下一步的结果多样性会大大降低，并且统计效率也非常低，这里直接跳出
                if (tMi > mN0/mMinProb) {
                    System.err.println("ERROR: P(λi+1|λi) will less than MinProb(= max(0.05, 1/N0) in default) anyway,");
                    System.err.println("which will seriously reduce the accuracy or efficiency, so the FFS is stopped.");
                    System.err.println("Try larger N0 or smaller surface distance.");
                    mFinished = true;
                    break;
                }
            }
            // 获取概率统计结果
            mPi.set_(mStep, mPointsOnLambda.size() / (double)tMi);
            // 第二个过程这一步完成
            ++mStep;
        } else {
            mFinished = true;
        }
    }
    public boolean finished() {return mFinished;}
    
    /** 获取结果的接口 */
    public double getProb(int aIdx) {return mPi.get(aIdx);}
    public double getK0() {return mK0;}
    public double getK() {return mK0 * mPi.product();}
    
    public int step1PointNum() {return mStep1PointNum;}
    public int step2PointNum(int aIdx) {return (int)mStep2PointNum.get(aIdx);}
    public int totalPointNum() {return mStep1PointNum + (int)mStep2PointNum.sum();}
    
    /** 利用保存的 parent 获取演化路径 */
    public LinkedList<T> pickPath() {return pickPath(mRNG.nextInt(mPointsOnLambda.size()));}
    public LinkedList<T> pickPath(int aIdx) {
        LinkedList<T> rPath = new LinkedList<>();
        Point tPoint = mPointsOnLambda.get(aIdx);
        do {
            rPath.addFirst(tPoint.value);
            tPoint = tPoint.parent;
        } while (tPoint != null);
        return rPath;
    }
    
    /**程序结束时会顺便关闭内部的 mFullPathGenerator，通过切换不同的 mFullPathGenerator 来调整实际输入的生成器是否会顺便关闭 */
    @Override public void shutdown() {mFullPathGenerator.shutdown();}
}
