package com.guan.atom;

import com.guan.math.MathEX;
import com.guan.math.functional.IOperator1;
import com.guan.math.functional.IOperator1Full;
import com.guan.math.numerical.Func3;
import com.guan.parallel.AbstractHasThreadPool;
import com.guan.parallel.ParforThreadPool;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.guan.code.CS.BOX_ZERO;
import static com.guan.math.MathEX.*;

/**
 * @author liqa
 * <p> 特定原子结构的生成器 </p>
 * <p> 此类线程不安全，但不同实例间线程安全 </p>
 */
public class Generator extends AbstractHasThreadPool<ParforThreadPool> {
    private final static String[] ATOM_DATA_KEYS_XYZ = new String[] {"x", "y", "z"};
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {mDead = true; super.shutdown();}
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    // 独立的随机数生成器
    private final Random mRNG;
    
    public Generator() {this(new Random());}
    public Generator(int aThreadNum) {this(aThreadNum, new Random());}
    public Generator(int aThreadNum, long aSeed) {this(aThreadNum, new Random(aSeed));}
    
    Generator(Random aRNG) {this(1, aRNG);}
    Generator(int aThreadNum, Random aRNG) {this(new ParforThreadPool(aThreadNum), aRNG);}
    Generator(ParforThreadPool aPool, Random aRNG) {super(aPool); mRNG = aRNG;}
    
    /** 参数设置 */
    public Generator setThreadNum(int aThreadNum)  {if (aThreadNum!=nThreads()) setPool(new ParforThreadPool(aThreadNum)); return this;}
    
    
    /**
     * 根据给定数据创建 FCC 的 atomData
     * @author liqa
     * @param aCellSize FCC 晶胞的晶格常数 a
     * @param aReplicateX x 方向的重复次数
     * @param aReplicateY Y 方向的重复次数
     * @param aReplicateZ Z 方向的重复次数
     * @return 返回由此创建的 atomData
     */
    public IHasAtomData atomDataFCC(double aCellSize, int aReplicateX, int aReplicateY, int aReplicateZ) {
        if (mDead) throwRuntimeException("This Generator is dead");
        
        final double[] tBoxHi = new double[] {aCellSize*aReplicateX, aCellSize*aReplicateY, aCellSize*aReplicateZ};
        final double[][] tAtomData = new double[4*aReplicateX*aReplicateY*aReplicateZ][];
        
        int tIdx = 0;
        for (int i = 0; i < aReplicateX; ++i) for (int j = 0; j < aReplicateY; ++j) for (int k = 0; k < aReplicateZ; ++k) {
            double tX = aCellSize*i, tY = aCellSize*j, tZ = aCellSize*k;
            double tS = aCellSize*0.5;
            tAtomData[tIdx] = new double[] {tX   , tY   , tZ   }; ++tIdx;
            tAtomData[tIdx] = new double[] {tX+tS, tY+tS, tZ   }; ++tIdx;
            tAtomData[tIdx] = new double[] {tX+tS, tY   , tZ+tS}; ++tIdx;
            tAtomData[tIdx] = new double[] {tX   , tY+tS, tZ+tS}; ++tIdx;
        }
        
        return new AbstractAtomData() {
            @Override public String[] atomDataKeys() {return ATOM_DATA_KEYS_XYZ;}
            @Override public double[][] atomData() {return tAtomData;}
            @Override public double[] boxLo() {return BOX_ZERO;}
            @Override public double[] boxHi() {return tBoxHi;}
        };
    }
    public IHasAtomData atomDataFCC(double aCellSize, int aReplicate) {return atomDataFCC(aCellSize, aReplicate, aReplicate, aReplicate);}
    
    
    /**
     * 根据通用的过滤器 aFilter 来过滤 aAtomData，移除不满足 Filter 的粒子。
     * 注意返回的内容都是 aAtomData 的引用，因此如果需要修改还需要手动进行值拷贝
     * @author liqa
     * @param aFilter 自定义的过滤器，输入 double[] 为 AtomData 的完整一行，返回是否保留
     * @param aAtomData 需要过滤的 aAtomData
     * @return 过滤后的 AtomData
     */
    public IHasAtomData filterAtomData(IOperator1Full<Boolean, double[]> aFilter, final IHasAtomData aAtomData) {
        if (mDead) throwRuntimeException("This Generator is dead");
        
        double[][] oAtomData = aAtomData.atomData();
        List<double[]> rAtomData = new ArrayList<>(oAtomData.length);
        for (double[] subData : oAtomData) if (aFilter.cal(subData)) rAtomData.add(subData);
        final double[][] tAtomData = rAtomData.toArray(new double[0][]);
        
        return new AbstractAtomData() {
            @Override public String[] atomDataKeys() {return aAtomData.atomDataKeys();}
            @Override public double[][] atomData() {return tAtomData;}
            @Override public double[] boxLo() {return aAtomData.boxLo();}
            @Override public double[] boxHi() {return aAtomData.boxHi();}
        };
    }
    
    /**
     * 根据 aFunc3 原子位置对应的值来过滤 aAtomData。
     * 注意返回的内容都是 aAtomData 的引用，因此如果需要修改还需要手动进行值拷贝
     * @author liqa
     * @param aFilter 自定义的过滤器，输入 double 为 aFunc3 的值，返回是否保留
     * @param aFunc3 过滤指定的三维函数
     * @param aAtomData 需要过滤的 aAtomData
     * @return 过滤后的 AtomData
     */
    public IHasAtomData filterFunc3AtomData(final IOperator1Full<Boolean, Double> aFilter, final Func3 aFunc3, final IHasAtomData aAtomData) {
        if (mDead) throwRuntimeException("This Generator is dead");
        
        // 获取 xyz 的下标
        int[] tIdxXYZ = aAtomData.xyzCol();
        final int tX = tIdxXYZ[0], tY = tIdxXYZ[1], tZ = tIdxXYZ[2];
        // 获取边界，会进行缩放将 aAtomData 的边界和 Func3 的边界对上
        final double[] tBoxLo = aAtomData.boxLo(), tBoxHi = aAtomData.boxHi();
        final double tX0 = aFunc3.x0()                , tY0 = aFunc3.y0()                , tZ0 = aFunc3.z0()                ;
        final double tXe = tX0+aFunc3.dx()*aFunc3.Nx(), tYe = tY0+aFunc3.dy()*aFunc3.Ny(), tZe = tZ0+aFunc3.dz()*aFunc3.Nz();
        // 需要使用考虑了 pbc 的 subs，因为正边界处的插值需要考虑 pbc
        return filterAtomData(subAtomData -> aFilter.cal(aFunc3.subsPBC(
            (subAtomData[tX]-tBoxLo[0])/(tBoxHi[0]-tBoxLo[0])*(tXe-tX0) + tX0,
            (subAtomData[tY]-tBoxLo[1])/(tBoxHi[1]-tBoxLo[1])*(tYe-tY0) + tY0,
            (subAtomData[tZ]-tBoxLo[2])/(tBoxHi[2]-tBoxLo[2])*(tZe-tZ0) + tZ0)), aAtomData);
    }
    /**
     * 预设的一种阈值的 filter，只有当对应的 Func3 大于阈值 aThreshold 才会保留
     * @author liqa
     * @param aThreshold 设置的阈值，默认为 0
     * @param aFunc3 指定的 func3
     * @param aAtomData 需要过滤的 aAtomData
     * @return 过滤后的 AtomData
     */
    public IHasAtomData filterThresholdFunc3AtomData(final double aThreshold, Func3 aFunc3, IHasAtomData aAtomData) {return filterFunc3AtomData(u -> (u > aThreshold), aFunc3, aAtomData);}
    public IHasAtomData filterThresholdFunc3AtomData(Func3 aFunc3, IHasAtomData aAtomData) {return filterThresholdFunc3AtomData(0.0, aFunc3, aAtomData);}
    
    /**
     * 预设的一种按照概率保留的 filter，将 Func3 使用通用的 aToProb 转换成概率，并按照概率保留
     * @author liqa
     * @param aToProb 通用的转换成概率的函数，输出 0-1 的浮点数，不指定则直接使用 aFunc3
     * @param aFunc3 指定的 func3
     * @param aAtomData 需要过滤的 aAtomData
     * @return 过滤后的 AtomData
     */
    public IHasAtomData filterProbFunc3AtomData(final IOperator1<Double> aToProb, Func3 aFunc3, IHasAtomData aAtomData) {return filterFunc3AtomData(u -> (mRNG.nextDouble() < aToProb.cal(u)), aFunc3, aAtomData);}
    public IHasAtomData filterProbFunc3AtomData(Func3 aFunc3, IHasAtomData aAtomData) {return filterProbFunc3AtomData(u -> u, aFunc3, aAtomData);}
    /**
     * 对于 aFunc3 = u, u = c1 - c0, c1 + c0 = 1，选取 c1 的特殊情况
     * @author liqa
     * @param aFunc3 指定的 func3 = u
     * @param aAtomData 需要过滤的 aAtomData
     * @return 过滤后的 AtomData
     */
    public IHasAtomData filterProbUFunc3AtomData(Func3 aFunc3, IHasAtomData aAtomData) {return filterProbFunc3AtomData(u -> 0.5*(u+1.0), aFunc3, aAtomData);}
    
    
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
    public Func3 porousCahnHilliard(IOperator1<Double> aDfu, double aTheta, final Func3 aInitU, double aDt, int aSteps) {
        if (mDead) throwRuntimeException("This Generator is dead");
        
        final double tTheta2 = aTheta*aTheta;
        final int tBlockSize = aInitU.Nx()*aInitU.Ny();
        
        // 事先创建的每个线程的临时数组变量，避免重复创建数组（相比临时创建快大概 10%）
        final double[] tArrayTemp1 = new double[aInitU.data().length];
        final double[] tArrayTemp2 = new double[aInitU.data().length];
        
        return aInitU.shell().setData(Func.odeLastEuler((uu, t) -> { // Euler 法求解 ode，并且只保留最后的值
            Func3 tFunc31 = aInitU.shell().setData(uu);
            Func3 tFunc32 = aInitU.shell().setData(tArrayTemp1);
            Func.parlaplacian2Dest(pool(), tFunc31, tFunc32); // 计算 uu 的 laplacian，存储到 tFunc32 中
            Vec.parebeDo2Dest(pool(), tBlockSize, tFunc32.data(), uu, (lapU, u) -> (aDfu.cal(u) - tTheta2*lapU)); // 计算 df/du - θ^2Δu，结果存储到 tFunc32
            tFunc31.setData(tArrayTemp2);
            return Func.parlaplacian2Dest(pool(), tFunc32, tFunc31).data(); // 最后结果再做一次 laplacian，存储到 tArrayTemp2（tFunc31） 并返回
        }, aInitU.data(), aDt, aSteps));
    }
    public Func3 porousCahnHilliard(double aTheta, Func3 aInitU, double aDt, int aSteps) {return porousCahnHilliard(u -> u*u*u-u, aTheta, aInitU, aDt, aSteps);}
    public Func3 porousCahnHilliard(Func3 aInitU, double aDt, int aSteps) {return porousCahnHilliard(0.1, aInitU, aDt, aSteps);}
    public Func3 porousCahnHilliard(Func3 aInitU, int aSteps) {return porousCahnHilliard(aInitU, 0.0001, aSteps);}
    public Func3 porousCahnHilliard(int aN, double aMeshSize, double aDt, int aSteps) {
        double[] tData = new double[aN*aN*aN];
        Vec.mapDo2Dest(tData, v -> (mRNG.nextDouble()*0.001-0.0005));
        Func3 tInitU = new Func3(0.0, aMeshSize, aN, 0.0, aMeshSize, aN, 0.0, aMeshSize, tData);
        
        return porousCahnHilliard(tInitU, aDt, aSteps);
    }
    public Func3 porousCahnHilliard(int aN, double aMeshSize, int aSteps) {return porousCahnHilliard(aN, aMeshSize, 0.0001, aSteps);}
    public Func3 porousCahnHilliard(int aSteps) {return porousCahnHilliard(50, 0.1, aSteps);}
}
