package com.jtool.rareevent;


import com.jtool.atom.IHasAtomData;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;

import java.util.*;

/**
 * 使用向前通量采样法（Forward Flux Sampling，FFS）来对稀有事件的概率进行采样；
 * 只用于技术上的简单实现，如果需要保留演化的路径信息请使用 BG
 * @author liqa
 * @param <T> 路径上每个点的类型，对于 lammps 模拟则是原子结构信息 {@link IHasAtomData}
 */
public class ForwardFluxSampling<T> implements Runnable {
    /** 需要有一个路径生成器以及对于路径上一个点的 λ 的计算器 */
    final IFullPathGenerator<T> mFullPathGenerator;
    final IParameterCalculator<? super T> mParameterCalculator;
    /** 需要有一个分割相空间的分界面，有 λ0 < λ1 < λ2 < ... < λn == B */
    final NavigableSet<Double> mSurfaces;
    /** 对于 A 有一个专门的分界面，因为需要频繁使用因此专门拿出来，要求 A <= λ0 */
    final double mSurfaceA;
    /** 每个界面的统计数目 */
    final int mN0;
    /** 独立的随机数生成器 */
    private final Random mRNG = new Random();
    
    public ForwardFluxSampling(IFullPathGenerator<T> aFullPathGenerator, IParameterCalculator<? super T> aParameterCalculator, double aSurfaceA, Iterable<? extends Number> aSurfaces, int aN0) {
        mFullPathGenerator = aFullPathGenerator; mParameterCalculator = aParameterCalculator;
        mSurfaceA = aSurfaceA;
        mSurfaces = new TreeSet<>();
        for (Number tLambda : aSurfaces) mSurfaces.add(tLambda.doubleValue());
        if (mSurfaces.size() < 1) throw new IllegalArgumentException("Surfaces Must at least have one element");
        if (mSurfaces.first() < mSurfaceA) throw new IllegalArgumentException("SurfaceA Must be the Lowest");
        mN0 = aN0;
    }
    
    public ForwardFluxSampling(IPathGenerator<T> aPathGenerator, IParameterCalculator<? super T> aParameterCalculator, double aSurfaceA, Iterable<? extends Number> aSurfaces, int aN0) {
        this(new BufferedFullPathGenerator<>(aPathGenerator), aParameterCalculator, aSurfaceA, aSurfaces, aN0);
    }
    
    
    /** 统计信息 */
    private double mTotTime0; // 第一个过程中的总时间，注意不是 A 第一次到达 λ0 的时间，因此得到的 mK0 不是 A 到 λ0 的速率
    private List<T> mPointsOnLambda; // 第一次从 A 到达 λi 的那些点
    
    private double mK0; // A 到 λ0 的轨迹通量，速率单位但是注意不是 A 到 λ0 的速率
    private IVector mPi; // i 到 i+1 而不是返回 A 的概率
    
    /** 记录一下每个过程使用的点的数目 */
    private int mStep1PointNum;
    private IVector mStep2PointNum;
    
    /** 统计一个路径所有的从 A 第一次到达 λ0 的点以及这个过程总共花费的时间 */
    private int statA2Lambda0_() {
        int tStep1PointNum = 0;
        // 获取初始路径的迭代器
        ITimeIterator<T> tPathInit = mFullPathGenerator.fullPathInit();
        T tPoint;
        double tLambda;
        // 不再需要检测 hasNext，内部保证永远都有 next
        while (mPointsOnLambda.size() < mN0) {
            // 首先找到到达 A 的起始位置，一般来说直接初始化的点都会在 A，但是不一定
            while (true) {
                tPoint = tPathInit.next();
                ++tStep1PointNum;
                // 检测是否到达 A
                tLambda = mParameterCalculator.lambdaOf(tPoint);
                if (tLambda <= mSurfaceA) break;
                // 如果到达 B 则重新回到 A，这里使用重新获取 PathInit 来实现，这样保证永远都会回到 A
                if (tLambda >= mSurfaces.last()) {
                    // 重设路径之前记得先保存旧的时间
                    mTotTime0 += tPathInit.timeConsumed();
                    tPathInit = mFullPathGenerator.fullPathInit();
                }
            }
            // 找到起始点后开始记录穿过 λ0 的点，简单起见目前暂不保留路径本身
            while (true) {
                tPoint = tPathInit.next();
                ++tStep1PointNum;
                // 判断是否有穿过 λ0
                tLambda = mParameterCalculator.lambdaOf(tPoint);
                if (tLambda >= mSurfaces.first()) {
                    // 如果有穿过 λ0 则需要记录这些点
                    mPointsOnLambda.add(tPoint);
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
    private int statLambda2Next_(T aStart, double aLambdaNext) {
        int tStep2PointNum = 0;
        // 获取从 aStart 开始的路径的迭代器
        Iterator<T> tPathFrom = mFullPathGenerator.fullPathFrom(aStart);
        
        T tPoint;
        double tLambda;
        // 不再需要检测 hasNext，内部保证永远都有 next
        while (true) {
            tPoint = tPathFrom.next();
            ++tStep2PointNum;
            tLambda = mParameterCalculator.lambdaOf(tPoint);
            // 判断是否穿过了 λi+1
            if (tLambda >= aLambdaNext) {
                // 如果有穿过 λi+1 则需要记录这些点
                mPointsOnLambda.add(tPoint);
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
    public void run() {
        // 实际分为两个过程，第一个过程首先统计轨迹通量（flux of trajectories）
        // 统计从 A 到达 λ0，运行直到达到次数超过 mN0
        // 在这里设置统计量的初始，保证独立性
        mStep1PointNum = 0;
        mTotTime0 = 0.0;
        mPointsOnLambda = new ArrayList<>();
        mStep1PointNum += statA2Lambda0_();
        // 获取第一个过程的统计结果
        mK0 = mPointsOnLambda.size() / mTotTime0;
        
        
        // 第二个过程会从 λi 上的点随机选取运行直到到达 λi+1 或者返回 A，注意依旧需要将耗时增加到 mTotTime 中
        // 获取边界面的信息
        int n = mSurfaces.size() - 1;
        // 同样，在这里设置统计量的初始，保证独立性
        List<T> oPointsOnLambda = new ArrayList<>();
        mStep2PointNum = Vectors.zeros(n);
        mPi = Vectors.zeros(n);
        // 由于是 Set 存储的，需要使用迭代器获取 λ 数值
        Iterator<Double> tLambdaIt = mSurfaces.iterator();
        tLambdaIt.next(); // λ0
        for (int i = 0; i < n; ++i) {
            // 先将上一步得到的 λi 交换到 oPointsOnLambda 作为初始
            List<T> tPointsOnLambda = oPointsOnLambda;
            oPointsOnLambda = mPointsOnLambda;
            mPointsOnLambda = tPointsOnLambda; mPointsOnLambda.clear();
            // 获取 λi+1
            double tLambda = tLambdaIt.next();
            // 获取 Mi
            int tMi = 0;
            while (mPointsOnLambda.size() < mN0) {
                // 随机选取一个初始点获取之后的路径，并统计结果
                T tPointI = oPointsOnLambda.get(mRNG.nextInt(oPointsOnLambda.size()));
                mStep2PointNum.set_(i, mStep2PointNum.get_(i) + statLambda2Next_(tPointI, tLambda));
                ++tMi;
            }
            // 获取概率统计结果
            mPi.set_(i, mPointsOnLambda.size() / (double)tMi);
        }
    }
    
    /** 获取结果的接口 */
    public double getProb(int aIdx) {return mPi.get(aIdx);}
    public double getK0() {return mK0;}
    public double getK() {return mK0 * mPi.operation().product();}
    
    public int step1PointNum() {return mStep1PointNum;}
    public int step2PointNum(int aIdx) {return (int)mStep2PointNum.get(aIdx);}
    public int totalPointNum() {return mStep1PointNum + (int)mStep2PointNum.operation().sum();}
}
