package jse.atom;

import jse.code.CS;
import jse.code.functional.IDoubleFilter;
import jse.code.functional.IFilter;
import jse.math.MathEX;
import jse.math.function.Func3;
import jse.parallel.AbstractThreadPool;
import jse.parallel.ParforThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

import static jse.code.CS.RANDOM;
import static jse.code.UT.Code.newBox;
import static jse.math.MathEX.Func;
import static jse.math.MathEX.Vec;

/**
 * @author liqa
 * <p> 特定原子结构的生成器 </p>
 * <p> 此类线程不安全，但不同实例间线程安全 </p>
 */
@Deprecated
public class Generator extends AbstractThreadPool<ParforThreadPool> {
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {mDead = true; super.shutdown();}
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    /** 可定义的随机数生成器，默认为 {@link CS#RANDOM} */
    private final Random mRNG;
    
    public Generator() {this(RANDOM);}
    public Generator(int aThreadNum) {this(aThreadNum, RANDOM);}
    public Generator(int aThreadNum, long aSeed) {this(aThreadNum, new Random(aSeed));}
    
    Generator(Random aRNG) {this(1, aRNG);}
    Generator(int aThreadNum, Random aRNG) {this(new ParforThreadPool(aThreadNum), aRNG);}
    Generator(ParforThreadPool aPool, Random aRNG) {super(aPool); mRNG = aRNG;}
    
    /** 参数设置 */
    public Generator setThreadNumber(int aThreadNum)  {if (aThreadNum != threadNumber()) setPool(new ParforThreadPool(aThreadNum)); return this;}
    
    
    
    /**
     * 根据通用的过滤器 aFilter 来过滤 aAtomData，移除不满足 Filter 的粒子。
     * 注意这里返回的都是引用的结果
     * @author liqa
     * @param aAtomData 需要过滤的 aAtomData
     * @param aFilter 自定义的过滤器，输入 {@link IAtom}，返回是否保留
     * @return 过滤后的 AtomData
     */
    public IAtomData filterAtomData(IAtomData aAtomData, IFilter<IAtom> aFilter) {
        if (mDead) throw new RuntimeException("This Generator is dead");
        
        List<IAtom> rAtoms = new ArrayList<>();
        
        for (IAtom tAtom : aAtomData.asList()) if (aFilter.accept(tAtom)) {
            rAtoms.add(tAtom);
        }
        
        return new AtomData(rAtoms, aAtomData.atomTypeNum(), newBox(aAtomData.box()), aAtomData.hasVelocities());
    }
    
    /**
     * 根据 aFunc3 原子位置对应的值来过滤 aAtomData。
     * 注意返回的内容都是 aAtomData 的引用，因此如果需要修改还需要手动进行值拷贝
     * @author liqa
     * @param aAtomData 需要过滤的 aAtomData
     * @param aFunc3 过滤指定的三维函数
     * @param aFilter 自定义的过滤器，输入 double 为 aFunc3 的值，返回是否保留
     * @return 过滤后的 AtomData
     */
    public IAtomData filterFunc3AtomData(final IAtomData aAtomData, final Func3 aFunc3, final IDoubleFilter aFilter) {
        if (mDead) throw new RuntimeException("This Generator is dead");
        
        // 获取边界，会进行缩放将 aAtomData 的边界和 Func3 的边界对上
        final IXYZ tBox = aAtomData.box();
        final double tX0 = aFunc3.x0()                , tY0 = aFunc3.y0()                , tZ0 = aFunc3.z0()                ;
        final double tXe = tX0+aFunc3.dx()*aFunc3.Nx(), tYe = tY0+aFunc3.dy()*aFunc3.Ny(), tZe = tZ0+aFunc3.dz()*aFunc3.Nz();
        // 需要使用考虑了 pbc 的 subs，因为正边界处的插值需要考虑 pbc
        return filterAtomData(aAtomData, atom -> aFilter.accept(aFunc3.subsPBC(
            atom.x()/tBox.x()*(tXe-tX0) + tX0,
            atom.y()/tBox.y()*(tYe-tY0) + tY0,
            atom.z()/tBox.z()*(tZe-tZ0) + tZ0)));
    }
    /**
     * 预设的一种阈值的 filter，只有当对应的 Func3 大于阈值 aThreshold 才会保留
     * @author liqa
     * @param aAtomData 需要过滤的 aAtomData
     * @param aFunc3 指定的 func3
     * @param aThreshold 设置的阈值，默认为 0
     * @return 过滤后的 AtomData
     */
    public IAtomData filterThresholdFunc3AtomData(IAtomData aAtomData, Func3 aFunc3, final double aThreshold) {return filterFunc3AtomData(aAtomData, aFunc3, u -> (u > aThreshold));}
    public IAtomData filterThresholdFunc3AtomData(IAtomData aAtomData, Func3 aFunc3) {return filterThresholdFunc3AtomData(aAtomData, aFunc3, 0.0);}
    
    /**
     * 预设的一种按照概率保留的 filter，将 Func3 使用通用的 aToProb 转换成概率，并按照概率保留
     * @author liqa
     * @param aAtomData 需要过滤的 aAtomData
     * @param aFunc3 指定的 func3
     * @param aToProb 通用的转换成概率的函数，输出 0-1 的浮点数，不指定则直接使用 aFunc3
     * @return 过滤后的 AtomData
     */
    public IAtomData filterProbFunc3AtomData(IAtomData aAtomData, Func3 aFunc3, final DoubleUnaryOperator aToProb) {return filterFunc3AtomData(aAtomData, aFunc3, u -> (mRNG.nextDouble() < aToProb.applyAsDouble(u)));}
    public IAtomData filterProbFunc3AtomData(IAtomData aAtomData, Func3 aFunc3) {return filterProbFunc3AtomData(aAtomData, aFunc3, u -> u);}
    /**
     * 对于 aFunc3 = u, u = c1 - c0, c1 + c0 = 1，选取 c1 的特殊情况
     * @author liqa
     * @param aAtomData 需要过滤的 aAtomData
     * @param aFunc3 指定的 func3 = u
     * @return 过滤后的 AtomData
     */
    public IAtomData filterProbUFunc3AtomData(IAtomData aAtomData, Func3 aFunc3) {return filterProbFunc3AtomData(aAtomData, aFunc3, u -> 0.5*(u+1.0));}
    
    
    /**
     * 使用 Cahn-Hilliard 方程获取多孔材料的最通用的函数，方程具体形式为：
     * du/dt = Δ[df/du - θ^2Δu]，
     * 只用于结构生成，看起来没有详细的物理或者单位概念
     * <p>
     * 支持并行，使用 {@link MathEX.Par}.setThreadNum(nThreads) 来设置并行数目
     * @author liqa
     * @param aDfu 方程中 df/du 的形式，默认为 d/du(0.25*(u^2 - 1)^2) = u^3 - u
     * @param aTheta 方程中的 θ，默认为 θ = 0.1
     * @param aInitU 初始情况的 u，默认直接完全随机生成即可（在 0 附近增加极小的噪音，网格尺寸默认选取 0.1，大小默认选取 50x50x50）
     * @param aDt 迭代的步长，默认选取 0.0001（为了保证收敛，越小的网络尺寸需要越小的步长）
     * @param aSteps 迭代的步数
     * @return 返回最终得到的 u
     */
    public Func3 porousCahnHilliard(DoubleUnaryOperator aDfu, double aTheta, final Func3 aInitU, double aDt, int aSteps) {
        if (mDead) throw new RuntimeException("This Generator is dead");
        
        final double tTheta2 = aTheta*aTheta;
        final int tBlockSize = aInitU.Nx()*aInitU.Ny();
        
        // 事先创建的每个线程的临时数组变量，避免重复创建数组（相比临时创建快大概 10%）
        final double[] tArrayTemp1 = new double[aInitU.data().length];
        final double[] tArrayTemp2 = new double[aInitU.data().length];
        
        return aInitU.shell().setData(Func.odeLastEuler((uu, t) -> { // Euler 法求解 ode，并且只保留最后的值
            Func3 tFunc31 = aInitU.shell().setData(uu);
            Func3 tFunc32 = aInitU.shell().setData(tArrayTemp1);
            Func.parlaplacian2Dest(pool(), tFunc31, tFunc32); // 计算 uu 的 laplacian，存储到 tFunc32 中
            Vec.parebeDo2Dest(pool(), tBlockSize, tFunc32.data(), uu, (lapU, u) -> (aDfu.applyAsDouble(u) - tTheta2*lapU)); // 计算 df/du - θ^2Δu，结果存储到 tFunc32
            tFunc31.setData(tArrayTemp2);
            return Func.parlaplacian2Dest(pool(), tFunc32, tFunc31).data(); // 最后结果再做一次 laplacian，存储到 tArrayTemp2（tFunc31） 并返回
        }, aInitU.data(), aDt, aSteps));
    }
    public Func3 porousCahnHilliard(double aTheta, Func3 aInitU, double aDt, int aSteps) {return porousCahnHilliard(u -> u*u*u-u, aTheta, aInitU, aDt, aSteps);}
    public Func3 porousCahnHilliard(Func3 aInitU, double aDt, int aSteps) {return porousCahnHilliard(0.1, aInitU, aDt, aSteps);}
    public Func3 porousCahnHilliard(Func3 aInitU, int aSteps) {return porousCahnHilliard(aInitU, 0.0001, aSteps);}
    public Func3 porousCahnHilliard(int aN, double aMeshSize, final double aC, double aDt, int aSteps) {
        double[] tData = new double[aN*aN*aN];
        Vec.mapDo2Dest(tData, v -> aC+(mRNG.nextDouble()*0.001-0.0005));
        Func3 tInitU = new Func3(0.0, aMeshSize, aN, 0.0, aMeshSize, aN, 0.0, aMeshSize, tData);
        
        return porousCahnHilliard(tInitU, aDt, aSteps);
    }
    public Func3 porousCahnHilliard(int aN, double aMeshSize, double aC, int aSteps) {return porousCahnHilliard(aN, aMeshSize, aC, 0.0001, aSteps);}
    public Func3 porousCahnHilliard(int aN, double aMeshSize, int aSteps) {return porousCahnHilliard(aN, aMeshSize, 0.0, aSteps);}
    public Func3 porousCahnHilliard(int aSteps) {return porousCahnHilliard(50, 0.1, aSteps);}
}
