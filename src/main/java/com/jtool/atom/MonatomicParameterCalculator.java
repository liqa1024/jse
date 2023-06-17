package com.jtool.atom;

import com.jtool.math.ComplexDouble;
import com.jtool.math.function.FixBoundFunc1;
import com.jtool.math.function.Func1;
import com.jtool.math.function.IFunc1;
import com.jtool.math.function.IZeroBoundFunc1;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.matrix.Matrices;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;
import com.jtool.parallel.AbstractHasThreadPool;
import com.jtool.parallel.ParforThreadPool;

import java.util.ArrayList;
import java.util.List;

import static com.jtool.code.CS.*;
import static com.jtool.math.MathEX.*;

/**
 * @author liqa
 * <p> 单原子的相关的参数的计算器 </p>
 * <p> 存储 atomDataXYZ，并且暂存对应的 NeighborListGetter 来加速计算 </p>
 * <p> 认为所有边界都是周期边界条件 </p>
 * <p> 会存在一些重复的代码，为了可读性不进一步消去 </p>
 * <p> 所有成员都是只读的，即使目前没有硬性限制 </p>
 */
public class MonatomicParameterCalculator extends AbstractHasThreadPool<ParforThreadPool> {
    /** 都使用 XYZ 存储来保证运算速度 */
    private XYZ[] mAtomDataXYZ;
    private final XYZ mBox;
    private final XYZ mBoxLo; // 用来记录数据是否经过了 shift
    
    private final int mAtomNum;
    private final double mRou; // 粒子数密度
    private final double mUnitLen; // 平均单个原子的距离
    
    private NeighborListGetter mNL;
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {shutdown_(); System.gc();}
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    
    public void shutdown_() {mDead = true; mAtomDataXYZ = null; super.shutdown(); mNL.shutdown_();}
    
    
    /**
     * 根据输入数据直接创建 MPC
     * @param aAtomDataXYZ 粒子数据，这里只需要知道 xyz 坐标即可
     * @param aBoxLo 模拟盒的下界，会据此将输入 aAtomDataXYZ 平移
     * @param aBoxHi 模拟盒的上界
     * @param aThreadNum MPC 进行计算会使用的线程数
     * @param aCellStep 内部用于加速近邻搜索的 LinkedCell 不同 Cell 大小的步长
     */
    public MonatomicParameterCalculator(Iterable<? extends IHasXYZ> aAtomDataXYZ, XYZ aBoxLo, XYZ aBoxHi, int aThreadNum, double aCellStep) {
        super(new ParforThreadPool(aThreadNum));
        
        // 获取模拟盒数据
        mBoxLo = aBoxLo;
        mBox = aBoxLo==BOX_ZERO ? aBoxHi : aBoxHi.minus(aBoxLo);
        
        // 获取合适的 XYZ[] 数据
        mAtomDataXYZ = toValidAtomDataXYZ_(aAtomDataXYZ);
        
        // 计算单位长度供内部使用
        mAtomNum = mAtomDataXYZ.length;
        mRou = mAtomNum / mBox.product();
        mUnitLen = Fast.cbrt(1.0/mRou);
        
        mNL = new NeighborListGetter(mAtomDataXYZ, mBox, aCellStep);
    }
    public MonatomicParameterCalculator(Iterable<? extends IHasXYZ> aAtomDataXYZ                                                                  ) {this(aAtomDataXYZ, BOX_ONE);}
    public MonatomicParameterCalculator(Iterable<? extends IHasXYZ> aAtomDataXYZ, IHasXYZ aBox                                                    ) {this(aAtomDataXYZ, BOX_ZERO, aBox);}
    public MonatomicParameterCalculator(Iterable<? extends IHasXYZ> aAtomDataXYZ, IHasXYZ aBoxLo, IHasXYZ aBoxHi                                  ) {this(aAtomDataXYZ, aBoxLo, aBoxHi, 1);}
    public MonatomicParameterCalculator(Iterable<? extends IHasXYZ> aAtomDataXYZ, IHasXYZ aBoxLo, IHasXYZ aBoxHi, int aThreadNum                  ) {this(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum, 2.0);}
    public MonatomicParameterCalculator(Iterable<? extends IHasXYZ> aAtomDataXYZ, IHasXYZ aBoxLo, IHasXYZ aBoxHi, int aThreadNum, double aCellStep) {this(aAtomDataXYZ, TO_BOX(aBoxLo), TO_BOX(aBoxHi), aThreadNum, aCellStep);}
    
    public MonatomicParameterCalculator(IHasAtomData aAtomData                                  ) {this(aAtomData, 1);}
    public MonatomicParameterCalculator(IHasAtomData aAtomData, int aThreadNum                  ) {this(aAtomData, aThreadNum, 2.0);}
    public MonatomicParameterCalculator(IHasAtomData aAtomData, int aThreadNum, double aCellStep) {this(aAtomData.atoms(), aAtomData.boxLo(), aAtomData.boxHi(), aThreadNum, aCellStep);}
    
    
    
    /** 内部使用方法，用来将 aAtomDataXYZ 转换成 XYZ[] 并且处理精度问题造成的超出边界问题 */
    private XYZ[] toValidAtomDataXYZ_(Iterable<? extends IHasXYZ> aAtomDataXYZ) {
        // 对传入的数据进行一次值拷贝转为更快的 XYZ[]
        List<XYZ> rAtomDataXYZ = new ArrayList<>();
        for (IHasXYZ tXYZ : aAtomDataXYZ) rAtomDataXYZ.add(new XYZ(tXYZ));
        XYZ[] tOutXYZ = rAtomDataXYZ.toArray(new XYZ[0]);
        
        // mBoxLo 不为零则需要将数据 shift
        if (mBoxLo != BOX_ZERO) for (XYZ tXYZ : tOutXYZ) tXYZ.minus2this(mBoxLo);
        
        // 由于 lammps 精度的问题，需要将超出边界的进行平移
        for (XYZ tXYZ : tOutXYZ) {
            if      (tXYZ.mX <  0.0    ) tXYZ.mX += mBox.mX;
            else if (tXYZ.mX >= mBox.mX) tXYZ.mX -= mBox.mX;
            if      (tXYZ.mY <  0.0    ) tXYZ.mY += mBox.mY;
            else if (tXYZ.mY >= mBox.mY) tXYZ.mY -= mBox.mY;
            if      (tXYZ.mZ <  0.0    ) tXYZ.mZ += mBox.mZ;
            else if (tXYZ.mZ >= mBox.mZ) tXYZ.mZ -= mBox.mZ;
        }
        
        return tOutXYZ;
    }
    
    
    /// 参数设置
    /**
     * 修改线程数，如果相同则不会进行任何操作
     * @param aThreadNum 线程数目
     * @return 返回自身用于链式调用
     */
    public MonatomicParameterCalculator setThreadNum(int aThreadNum)  {if (aThreadNum!=nThreads()) setPool(new ParforThreadPool(aThreadNum)); return this;}
    /**
     * 修改 LinkedCell 的步长，如果相同则不会进行任何操作
     * @param sCellStep 步长，默认为 2.0，需要大于 1.1，理论上越小速度会越快，同时会消耗更多的内存
     * @return 返回自身用于链式调用
     */
    public MonatomicParameterCalculator setCellStep(double sCellStep) {if (sCellStep != mNL.getCellStep()) {mNL.shutdown(); mNL = new NeighborListGetter(mAtomDataXYZ, mBox, sCellStep);} return this;}
    
    
    /// 获取信息
    public double unitLen() {return mUnitLen;}
    public double rou() {return mRou;}
    public double rou(MonatomicParameterCalculator aMPC) {return Fast.sqrt(mRou*aMPC.mRou);}
    
    
    /// 计算方法
    /**
     * 计算 RDF (radial distribution function，即 g(r))，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @return gr 函数
     */
    public IFunc1 calRDF(int aN, final double aRMax) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final IVector[] dn = new IVector[nThreads()];
        for (int i = 0; i < dn.length; ++i) dn[i] = Vectors.zeros(aN);
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(mAtomNum, (i, threadID) -> {
            mNL.forEachNeighbor(i, aRMax - dr*0.5, true, (x, y, z, idx, dis) -> {
                int tIdx = (int) Math.ceil((dis - dr*0.5) / dr);
                if (tIdx > 0 && tIdx < aN) dn[threadID].increment_(tIdx);
            });
        });
        
        // 获取结果
        IFunc1 gr = new FixBoundFunc1(0, dr, dn[0].data()).setBound(0.0, 1.0);
        for (int i = 1; i < dn.length; ++i) gr.f().plus2this(dn[i]);
        final double rou = dr * mAtomNum*0.5 * mRou; // mAtomNum*0.5 为对所有原子求和需要进行的平均
        gr.div2this(r -> r*r*4.0*PI*rou);
        
        // 修复截断数据
        gr.set_(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF(                          ) {return calRDF(160);}
    public IFunc1 calRDF(int aN                    ) {return calRDF(aN, mUnitLen*6);}
    
    
    /**
     * 计算自身与输入的 aAtomDataXYZ 之间的 RDF，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aAtomDataXYZ 另一个元素的 xyz 坐标组成的矩阵，或者输入 aMPC 即计算两个 MPC 之间的 RDF，如果初始化使用的 Box 则认为 aAtomDataXYZ 未经过平移，否则认为 aAtomDataXYZ 已经经过了平移。对于 MPC 没有这个问题
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @return gr 以及对应横坐标 r 构成的矩阵，排成两列，gr 在前 r 在后
     */
    public IFunc1 calRDF_AB(final XYZ[] aAtomDataXYZ, int aN, final double aRMax) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final IVector[] dn = new IVector[nThreads()];
        for (int i = 0; i < dn.length; ++i) dn[i] = Vectors.zeros(aN);
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(aAtomDataXYZ.length, (i, threadID) -> {
            mNL.forEachNeighbor(aAtomDataXYZ[i], aRMax - dr*0.5, (x, y, z, idx, dis) -> {
                int tIdx = (int) Math.ceil((dis - dr*0.5) / dr);
                if (tIdx > 0 && tIdx < aN) dn[threadID].increment_(tIdx);
            });
        });
        
        
        // 获取结果
        IFunc1 gr = new FixBoundFunc1(0, dr, dn[0].data()).setBound(0.0, 1.0);
        for (int i = 1; i < dn.length; ++i) gr.f().plus2this(dn[i]);
        final double rou = dr * aAtomDataXYZ.length * mRou; // aAtomDataXYZ.length 为对所有原子求和需要进行的平均
        gr.div2this(r -> r*r*4.0*PI*rou);
        
        // 修复截断数据
        gr.set_(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF_AB(Iterable<? extends IHasXYZ>  aAtomDataXYZ                            ) {return calRDF_AB(aAtomDataXYZ, 160);}
    public IFunc1 calRDF_AB(Iterable<? extends IHasXYZ>  aAtomDataXYZ, int aN                    ) {return calRDF_AB(aAtomDataXYZ, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB(Iterable<? extends IHasXYZ>  aAtomDataXYZ, int aN, final double aRMax) {return calRDF_AB(toValidAtomDataXYZ_(aAtomDataXYZ), aN, aRMax);}
    public IFunc1 calRDF_AB(MonatomicParameterCalculator aMPC                                    ) {return calRDF_AB(aMPC, 160);}
    public IFunc1 calRDF_AB(MonatomicParameterCalculator aMPC        , int aN                    ) {return calRDF_AB(aMPC, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB(MonatomicParameterCalculator aMPC        , int aN, final double aRMax) {return calRDF_AB(aMPC.mAtomDataXYZ, aN, aRMax);} // aMPC 的 mAtomDataXYZ 都已经经过平移并且合理化
    
    
    
    /**
     * 使用带有一定展宽的高斯分布代替直接计数来计算 RDF
     * @author liqa
     * @param aN 指定分划的份数，这里需要更多的份数来得到合适的结果，默认为 1000
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @param aSigmaMul 高斯分布的一个标准差宽度对应的分划份数，默认为 4
     * @return gr 以及对应横坐标 r 构成的矩阵，排成两列，gr 在前 r 在后
     */
    public IFunc1 calRDF_G(int aN, final double aRMax, int aSigmaMul) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final IFunc1[] dn = new IFunc1[nThreads()];
        for (int i = 0; i < dn.length; ++i) dn[i] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
        
        // 并行需要线程数个独立的 DeltaG
        final IZeroBoundFunc1[] tDeltaG = new IZeroBoundFunc1[nThreads()];
        for (int i = 0; i < tDeltaG.length; ++i) tDeltaG[i] = Func1.deltaG(dr*aSigmaMul, 0.0, aSigmaMul);
        // 需要增加一个额外的偏移保证外部边界的统计正确性
        final double tRShift = tDeltaG[0].zeroBoundR();
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(mAtomNum, (i, threadID) -> {
            mNL.forEachNeighbor(i, aRMax+tRShift, true, (x, y, z, idx, dis) -> {
                tDeltaG[threadID].setX0(dis);
                dn[threadID].plus2this(tDeltaG[threadID]);
            });
        });
        
        // 获取结果
        IFunc1 gr = dn[0];
        for (int i = 1; i < dn.length; ++i) gr.plus2this(dn[i]);
        final double rou = mAtomNum*0.5 * mRou; // mAtomNum*0.5 为对所有原子求和需要进行的平均
        gr.div2this(r -> r*r*4.0*PI*rou);
        
        // 修复截断数据
        gr.set_(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF_G(                                         ) {return calRDF_G(1000);}
    public IFunc1 calRDF_G(int aN                                   ) {return calRDF_G(aN, mUnitLen*6);}
    public IFunc1 calRDF_G(int aN, final double aRMax               ) {return calRDF_G(aN, aRMax, 4);}
    
    
    public IFunc1 calRDF_AB_G(final XYZ[] aAtomDataXYZ, int aN, final double aRMax, int aSigmaMul) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final IFunc1[] dn = new IFunc1[nThreads()];
        for (int i = 0; i < dn.length; ++i) dn[i] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
        
        // 并行需要线程数个独立的 DeltaG
        final IZeroBoundFunc1[] tDeltaG = new IZeroBoundFunc1[nThreads()];
        for (int i = 0; i < tDeltaG.length; ++i) tDeltaG[i] = Func1.deltaG(dr*aSigmaMul, 0.0, aSigmaMul);
        // 需要增加一个额外的偏移保证外部边界的统计正确性
        final double tRShift = tDeltaG[0].zeroBoundR();
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(aAtomDataXYZ.length, (i, threadID) -> {
            mNL.forEachNeighbor(aAtomDataXYZ[i], aRMax+tRShift, (x, y, z, idx, dis) -> {
                tDeltaG[threadID].setX0(dis);
                dn[threadID].plus2this(tDeltaG[threadID]);
            });
        });
        
        // 获取结果
        IFunc1 gr = dn[0];
        for (int i = 1; i < dn.length; ++i) gr.plus2this(dn[i]);
        final double rou = aAtomDataXYZ.length * mRou; // aAtomDataXYZ.length 为对所有原子求和需要进行的平均
        gr.div2this(r -> r*r*4.0*PI*rou);
        
        // 修复截断数据
        gr.set_(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF_AB_G(Iterable<? extends IHasXYZ>  aAtomDataXYZ                                           ) {return calRDF_AB_G(aAtomDataXYZ, 1000);}
    public IFunc1 calRDF_AB_G(Iterable<? extends IHasXYZ>  aAtomDataXYZ, int aN                                   ) {return calRDF_AB_G(aAtomDataXYZ, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB_G(Iterable<? extends IHasXYZ>  aAtomDataXYZ, int aN, final double aRMax               ) {return calRDF_AB_G(aAtomDataXYZ, aN, aRMax, 4);}
    public IFunc1 calRDF_AB_G(Iterable<? extends IHasXYZ>  aAtomDataXYZ, int aN, final double aRMax, int aSigmaMul) {return calRDF_AB_G(toValidAtomDataXYZ_(aAtomDataXYZ), aN, aRMax, aSigmaMul);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC                                                   ) {return calRDF_AB_G(aMPC, 1000);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC        , int aN                                   ) {return calRDF_AB_G(aMPC, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC        , int aN, final double aRMax               ) {return calRDF_AB_G(aMPC, aN, aRMax, 4);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC        , int aN, final double aRMax, int aSigmaMul) {return calRDF_AB_G(aMPC.mAtomDataXYZ, aN, aRMax, aSigmaMul);} // aMPC 的 mAtomDataXYZ 都已经经过了平移
    
    
    
    /**
     * 计算 SF（structural factor，即 S(q)），结构参数，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aQMax 额外指定最大计算的 q 的位置（默认为 6 倍单位长度）
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @param aQMin 可以手动指定最小的截断的 q（由于 pbc 的原因，过小的结果发散）
     * @return Sq 以及对应横坐标 q 构成的矩阵，排成两列，Sq 在前 q 在后
     */
    public IFunc1 calSF(double aQMax, int aN, final double aRMax, double aQMin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dq = (aQMax-aQMin)/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final IFunc1[] Hq = new IFunc1[nThreads()];
        for (int i = 0; i < Hq.length; ++i) Hq[i] = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        
        // 使用 mNL 的通用获取近邻的方法，因为 SF 需要使用方形半径内的所有距离（曼哈顿距离）
        pool().parfor(mAtomNum, (i, threadID) -> {
            final XYZ cXYZ = mAtomDataXYZ[i];
            mNL.forEachNeighborMHT(i, aRMax, true, (x, y, z, idx, disMHT) -> {
                double dis = cXYZ.distance(x, y, z);
                Hq[threadID].plus2this(q -> Fast.sin(q*dis)/(q*dis));
            });
        });
        
        // 获取结果
        IFunc1 Sq = Hq[0];
        for (int i = 1; i < Hq.length; ++i) Sq.plus2this(Hq[i]);
        Sq.div2this(mAtomNum*0.5);
        Sq.plus2this(1.0);
        
        // 修复截断数据
        Sq.set_(0, 0.0);
        // 输出
        return Sq;
    }
    public IFunc1 calSF(double aQMax, int aN, final double aRMax) {return calSF(aQMax, aN, aRMax, 2.0*PI/mUnitLen * 0.6);}
    public IFunc1 calSF(double aQMax, int aN                    ) {return calSF(aQMax, aN, mUnitLen*6);}
    public IFunc1 calSF(double aQMax                            ) {return calSF(aQMax, 160);}
    public IFunc1 calSF(                                        ) {return calSF(2.0*PI/mUnitLen * 6.0);}
    
    
    /**
     * 计算自身与输入的 aAtomDataXYZ 之间的 SF，只计算一个固定结构的值，因此不包含温度信息
     * @param aAtomDataXYZ 另一个元素的 xyz 坐标组成的矩阵，或者输入 aMPC 即计算两个 MPC 之间的 RDF，如果初始化使用的 Box 则认为 aAtomDataXYZ 未经过平移，否则认为 aAtomDataXYZ 已经经过了平移。对于 MPC 没有这个问题
     * @param aQMax 额外指定最大计算的 q 的位置
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @param aQMin 手动指定最小的截断的 q
     * @return Sq 以及对应横坐标 q 构成的矩阵，排成两列，Sq 在前 q 在后
     */
    public IFunc1 calSF_AB(final XYZ[] aAtomDataXYZ, double aQMax, int aN, final double aRMax, double aQMin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        
        final double dq = (aQMax-aQMin)/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final IFunc1[] Hq = new IFunc1[nThreads()];
        for (int i = 0; i < Hq.length; ++i) Hq[i] = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        
        // 使用 mNL 的通用获取近邻的方法，因为 SF 需要使用方形半径内的所有距离（曼哈顿距离）
        pool().parfor(aAtomDataXYZ.length, (i, threadID) -> {
            final XYZ cXYZ = aAtomDataXYZ[i];
            mNL.forEachNeighborMHT(cXYZ, aRMax, (x, y, z, idx, disMHT) -> {
                double dis = cXYZ.distance(x, y, z);
                Hq[threadID].plus2this(q -> Fast.sin(q*dis)/(q*dis));
            });
        });
        
        // 获取结果
        IFunc1 Sq = Hq[0];
        for (int i = 1; i < Hq.length; ++i) Sq.plus2this(Hq[i]);
        Sq.div2this(Fast.sqrt(mAtomNum*aAtomDataXYZ.length));
        Sq.plus2this(1.0);
        
        // 修复截断数据
        Sq.set_(0, 0.0);
        // 输出
        return Sq;
    }
    public IFunc1 calSF_AB(Iterable<? extends IHasXYZ>  aAtomDataXYZ                                                        ) {return calSF_AB(aAtomDataXYZ, 2.0*PI/mUnitLen * 6.0);}
    public IFunc1 calSF_AB(Iterable<? extends IHasXYZ>  aAtomDataXYZ, double aQMax                                          ) {return calSF_AB(aAtomDataXYZ, aQMax, 160);}
    public IFunc1 calSF_AB(Iterable<? extends IHasXYZ>  aAtomDataXYZ, double aQMax, int aN                                  ) {return calSF_AB(aAtomDataXYZ, aQMax, aN, mUnitLen*6);}
    public IFunc1 calSF_AB(Iterable<? extends IHasXYZ>  aAtomDataXYZ, double aQMax, int aN, final double aRMax              ) {return calSF_AB(aAtomDataXYZ, aQMax, aN, aRMax, 2.0*PI/mUnitLen * 0.6);}
    public IFunc1 calSF_AB(Iterable<? extends IHasXYZ>  aAtomDataXYZ, double aQMax, int aN, final double aRMax, double aQMin) {return calSF_AB(toValidAtomDataXYZ_(aAtomDataXYZ), aQMax, aN, aRMax, aQMin);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC                                                                ) {return calSF_AB(aMPC, 2.0*PI/mUnitLen * 6.0);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax                                          ) {return calSF_AB(aMPC, aQMax, 160);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax, int aN                                  ) {return calSF_AB(aMPC, aQMax, aN, mUnitLen*6);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax, int aN, final double aRMax              ) {return calSF_AB(aMPC, aQMax, aN, aRMax, 2.0*PI/mUnitLen * 0.6);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax, int aN, final double aRMax, double aQMin) {return calSF_AB(aMPC.mAtomDataXYZ, aQMax, aN, aRMax, aQMin);}
    
    
    
    /// gr 和 Sq 的相互转换，由于依旧需要体系的原子数密度，因此还是都移动到 MPC 中
    /**
     * 转换 g(r) 到 S(q)，这是主要计算 S(q) 的方法
     * @author liqa
     * @param aGr the matrix form of g(r)
     * @param aRou the atom number density（默认会选择本 MPC 得到的密度）
     * @param aN the split number of output（默认为 160）
     * @param aQMax the max q of output S(q)（默认为 6 倍单位距离）
     * @param aQMin the min q of output S(q)（默认为 0.4 倍单位距离）
     * @return the structural factor, S(q)
     */
    public IFunc1 RDF2SF(IFunc1 aGr, double aRou, int aN, double aQMax, double aQMin) {
        double dq = (aQMax-aQMin)/aN;
        
        IFunc1 Sq = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        Sq.fill(aGr.operation().refConvolveFull((gr, r, q) -> (r * (gr-1.0) * Fast.sin(q*r) / q)));
        Sq.multiply2this(4.0*PI*aRou);
        Sq.plus2this(1.0);
        
        Sq.set_(0, 0.0);
        return Sq;
    }
    public IFunc1 RDF2SF(IFunc1 aGr, double aRou, int aN, double aQMax) {return RDF2SF(aGr, aRou, aN, aQMax, 2.0*PI/mUnitLen * 0.4);}
    public IFunc1 RDF2SF(IFunc1 aGr, double aRou, int aN              ) {return RDF2SF(aGr, aRou, aN, 2.0*PI/mUnitLen * 6.0, 2.0*PI/mUnitLen * 0.4);}
    public IFunc1 RDF2SF(IFunc1 aGr, double aRou                      ) {return RDF2SF(aGr, aRou, 160);}
    public IFunc1 RDF2SF(IFunc1 aGr                                   ) {return RDF2SF(aGr, mRou);}
    
    
    /**
     * 转换 S(q) 到 g(r)
     * @author liqa
     * @param aSq the matrix form of S(q)
     * @param aRou the atom number density（默认会选择本 MPC 得到的密度）
     * @param aN the split number of output（默认为 160）
     * @param aRMax the max r of output g(r)（默认为 6 倍单位距离）
     * @param aRMin the min r of output g(r)（默认为 0.4 倍单位距离）
     * @return the radial distribution function, g(r)
     */
    public IFunc1 SF2RDF(IFunc1 aSq, double aRou, int aN, double aRMax, double aRMin) {
        double dr = (aRMax-aRMin)/aN;
        
        IFunc1 gr = FixBoundFunc1.zeros(aRMin, dr, aN+1).setBound(0.0, 1.0);
        gr.fill(aSq.operation().refConvolveFull((Sq, q, r) -> (q * (Sq-1.0) * Fast.sin(q*r) / r)));
        gr.multiply2this(1.0/(2.0*PI*PI*aRou));
        gr.plus2this(1.0);
        
        gr.set_(0, 0.0);
        return gr;
    }
    public IFunc1 SF2RDF(IFunc1 aSq, double aRou, int aN, double aRMax) {return SF2RDF(aSq, aRou, aN, aRMax, mUnitLen * 0.4);}
    public IFunc1 SF2RDF(IFunc1 aSq, double aRou, int aN              ) {return SF2RDF(aSq, aRou, aN, mUnitLen * 6.0, mUnitLen * 0.4);}
    public IFunc1 SF2RDF(IFunc1 aSq, double aRou                      ) {return SF2RDF(aSq, aRou, 160);}
    public IFunc1 SF2RDF(IFunc1 aSq                                   ) {return SF2RDF(aSq, mRou);}
    
    
    /**
     * 计算所有粒子的 AOOP（Averaged local bond Orientational Order Parameters），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     * @author liqa
     * @param aL 计算具体 q 值的下标，即 q4: l = 4, q6: l = 6
     * @param aNN Neighbor list Number，统计的近邻数目，默认为 12
     * @param aRMaxBegin 初始用来搜索近邻的半径，越小搜索的目标越少，可能会更快，
     * 但过小会达不到 aNN，从而会内部增大此半径重新搜索，此时耗时不会进一步降低。默认为 1.5 倍单位长度
     * @return ql 组成的向量
     */
    public IVector calAOOP(final int aL, final int aNN, final double aRMaxBegin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 先遍历一次计算所有的球谐函数 Y（复数）以及记录对应的近邻列表
        // 由于目前还没有实现复数运算，再搭一套复数库工作量较大，这里暂时使用两个实向量来存储
        final IMatrix tYAllReal = Matrices.zeros(mAtomNum, aL+aL+1);
        final IMatrix tYAllImag = Matrices.zeros(mAtomNum, aL+aL+1);
        @SuppressWarnings("unchecked")
        final List<Integer>[] tNNListAll = new List[mAtomNum]; // 注意不能使用 List<xxx> 然后在 parfor 中 add，因为线程不安全
        
        // 遍历计算 YMean
        pool().parfor(mAtomNum, (i, threadID) -> {
            final XYZ cXYZ = mAtomDataXYZ[i];
            final List<Integer> rNNList = new ArrayList<>(aNN);
            mNL.forEachNeighborNN(i, aNN, aRMaxBegin, (x, y, z, idx, dis) -> {
                // 计算角度
                double dx = x - cXYZ.mX;
                double dy = y - cXYZ.mY;
                double dz = z - cXYZ.mZ;
                
                double theta = Fast.acos(dz / dis);
                double disXY = Fast.sqrt(dx*dx + dy*dy);
                double phi = (dy > 0) ? Fast.acos(dx / disXY) : (2.0*PI - Fast.acos(dx / disXY));
                
                // 计算 Y 并累加
                for (int tM = -aL; tM <= aL; ++tM) {
                    ComplexDouble tY = Func.sphericalHarmonics_(aL, tM, theta, phi);
                    tYAllReal.add_(i, tM+aL, tY.real);
                    tYAllImag.add_(i, tM+aL, tY.imag);
                }
                
                // 顺便统计近邻列表
                rNNList.add(idx);
            });
            // 汇总近邻列表
            tNNListAll[i] = rNNList;
        });
        
        // 先根据近邻数平均
        tYAllReal.operation().mapDiv2this(aNN);
        tYAllImag.operation().mapDiv2this(aNN);
        
        // 在近邻的基础上再进行一次平均
        IVector ql = Vectors.zeros(mAtomNum);
        for (int i = 0; i < mAtomNum; ++i) {
            double rSum = 0.0;
            // 对于每个 m 分别累加
            for (int tM = -aL; tM <= aL; ++tM) {
                int tCol = tM+aL;
                // 先累加自身
                double tYReal = tYAllReal.get_(i, tCol);
                double tYImag = tYAllImag.get_(i, tCol);
                // 再累加近邻
                tYReal += tYAllReal.refSlicer().get(tNNListAll[i], tCol).operation().sum();
                tYImag += tYAllImag.refSlicer().get(tNNListAll[i], tCol).operation().sum();
                // 求平均
                tYReal /= aNN+1;
                tYImag /= aNN+1;
                // 最后求模量添加到 rSum
                rSum += tYReal*tYReal + tYImag*tYImag;
            }
            // 使用这个公式设置 ql
            ql.set_(i, 4.0*PI*rSum/(double)(aL+aL+1));
        }
        
        // 返回最终计算结果
        return ql;
    }
    public IVector calAOOP(int aL, int aNN) {return calAOOP(aL, aNN, mUnitLen*1.5);}
    public IVector calAOOP(int aL         ) {return calAOOP(aL, 12);}
}
