package com.jtool.atom;

import com.jtool.code.filter.IDoubleFilter;
import com.jtool.code.filter.IFilter;
import com.jtool.code.functional.IDoubleOperator1;
import com.jtool.code.functional.IOperator1;
import com.jtool.math.MathEX;
import com.jtool.math.function.Func3;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;
import com.jtool.parallel.AbstractThreadPool;
import com.jtool.parallel.ParforThreadPool;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static com.jtool.math.MathEX.Func;
import static com.jtool.math.MathEX.Vec;

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
    public IAtomData atomDataFCC(double aCellSize, int aReplicateX, int aReplicateY, int aReplicateZ) {
        if (mDead) throw new RuntimeException("This Generator is dead");
        
        XYZ tBoxHi = new XYZ(aCellSize*aReplicateX, aCellSize*aReplicateY, aCellSize*aReplicateZ);
        List<IAtom> rAtoms = new ArrayList<>(4*aReplicateX*aReplicateY*aReplicateZ);
        
        int tID = 1;
        for (int i = 0; i < aReplicateX; ++i) for (int j = 0; j < aReplicateY; ++j) for (int k = 0; k < aReplicateZ; ++k) {
            double tX = aCellSize*i, tY = aCellSize*j, tZ = aCellSize*k;
            double tS = aCellSize*0.5;
            rAtoms.add(new Atom(tX   , tY   , tZ   , tID)); ++tID;
            rAtoms.add(new Atom(tX+tS, tY+tS, tZ   , tID)); ++tID;
            rAtoms.add(new Atom(tX+tS, tY   , tZ+tS, tID)); ++tID;
            rAtoms.add(new Atom(tX   , tY+tS, tZ+tS, tID)); ++tID;
        }
        
        return new AtomData(rAtoms, tBoxHi);
    }
    public IAtomData atomDataFCC(double aCellSize, int aReplicate) {return atomDataFCC(aCellSize, aReplicate, aReplicate, aReplicate);}
    
    
    
    /**
     * 根据通用的过滤器 aFilter 来过滤 aAtomData，修改粒子的种类
     * @author liqa
     * @param aAtomData 需要过滤的 aAtomData
     * @param aMinTypeNum 建议最小的种类数目
     * @param aFilter 自定义的过滤器，输入 {@link IAtom}，返回过滤后的 type
     * @return 过滤后的 AtomData
     */
    public IAtomData typeFilterAtomData(IAtomData aAtomData, int aMinTypeNum, IOperator1<Integer, IAtom> aFilter) {
        if (mDead) throw new RuntimeException("This Generator is dead");
        
        List<IAtom> rAtoms = new ArrayList<>(aAtomData.atomNum());
        
        int tAtomTypeNum = Math.max(aMinTypeNum, aAtomData.atomTypeNum());
        for (IAtom oAtom : aAtomData.atoms()) {
            Atom tAtom = new Atom(oAtom);
            // 更新粒子种类数目
            int tType = aFilter.cal(oAtom);
            if (tType > tAtomTypeNum) tAtomTypeNum = tType;
            tAtom.mType = tType;
            // 保存修改后的原子
            rAtoms.add(tAtom);
        }
        
        return new AtomData(rAtoms, tAtomTypeNum, aAtomData.box());
    }
    public IAtomData typeFilterAtomData(final IAtomData aAtomData, IOperator1<Integer, IAtom> aFilter) {return typeFilterAtomData(aAtomData, 1, aFilter);}
    
    /**
     * 根据给定的权重来随机修改原子种类，主要用于创建合金的初始结构
     * @author liqa
     */
    public IAtomData typeFilterWeightAtomData(IAtomData aAtomData, double... aTypeWeights) {
        // 特殊输入直接输出
        if (aTypeWeights == null || aTypeWeights.length == 0) return aAtomData;
        return typeFilterWeightAtomData(aAtomData, Vectors.from(aTypeWeights));
    }
    public IAtomData typeFilterWeightAtomData(final IAtomData aAtomData, IVector aTypeWeights) {
        double tTotWeight = aTypeWeights.sum();
        if (tTotWeight <= 0.0) return aAtomData;
        
        int tAtomNum = aAtomData.atomNum();
        int tMaxType = aTypeWeights.size();
        // 获得对应原子种类的 List
        final List<Integer> tTypeList = new ArrayList<>(tAtomNum+tMaxType);
        for (int tType = 1; tType <= tMaxType; ++tType) {
            // 计算这种种类的粒子数目
            long tSteps = Math.round((aTypeWeights.get_(tType-1) / tTotWeight) * tAtomNum);
            for (int i = 0; i < tSteps; ++i) tTypeList.add(tType);
        }
        // 简单处理，如果数量不够则添加最后一种种类
        while (tTypeList.size() < tAtomNum) tTypeList.add(tMaxType);
        // 随机打乱这些种类标记
        Collections.shuffle(tTypeList, mRNG);
        // 使用 typeFilter 获取种类修改后的 AtomData
        final AtomicInteger idx = new AtomicInteger();
        return typeFilterAtomData(aAtomData, tMaxType, atom -> tTypeList.get(idx.getAndIncrement()));
    }
    
    
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
        
        for (IAtom tAtom : aAtomData.atoms()) if (aFilter.accept(tAtom)) {
            rAtoms.add(tAtom);
        }
        
        return new AtomData(rAtoms, aAtomData.atomTypeNum(), aAtomData.box(), aAtomData.hasVelocities());
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
    public IAtomData filterProbFunc3AtomData(IAtomData aAtomData, Func3 aFunc3, final IDoubleOperator1 aToProb) {return filterFunc3AtomData(aAtomData, aFunc3, u -> (mRNG.nextDouble() < aToProb.cal(u)));}
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
    public Func3 porousCahnHilliard(IDoubleOperator1 aDfu, double aTheta, final Func3 aInitU, double aDt, int aSteps) {
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
            Vec.parebeDo2Dest(pool(), tBlockSize, tFunc32.data(), uu, (lapU, u) -> (aDfu.cal(u) - tTheta2*lapU)); // 计算 df/du - θ^2Δu，结果存储到 tFunc32
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
