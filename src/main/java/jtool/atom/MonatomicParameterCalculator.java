package jtool.atom;

import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.collection.IntegerList;
import jtool.code.functional.IIndexFilter;
import jtool.code.functional.IIntegerConsumer1;
import jtool.code.functional.IOperator1;
import jtool.code.iterator.IDoubleIterator;
import jtool.code.iterator.IDoubleSetIterator;
import jtool.math.ComplexDouble;
import jtool.math.MathEX;
import jtool.math.function.FixBoundFunc1;
import jtool.math.function.Func1;
import jtool.math.function.IFunc1;
import jtool.math.function.IZeroBoundFunc1;
import jtool.math.matrix.IComplexMatrix;
import jtool.math.matrix.IMatrix;
import jtool.math.matrix.RowComplexMatrix;
import jtool.math.vector.Vector;
import jtool.math.vector.*;
import jtool.parallel.*;
import jtoolex.voronoi.VoronoiBuilder;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jtool.atom.NeighborListGetter.DEFAULT_CELL_STEP;
import static jtool.code.CS.R_NEAREST_MUL;
import static jtool.code.UT.Code.newBox;
import static jtool.math.MathEX.*;

/**
 * @author liqa
 * <p> 单原子的相关的参数的计算器 </p>
 * <p> 存储 atomDataXYZ，并且暂存对应的 NeighborListGetter 来加速计算 </p>
 * <p> 认为所有边界都是周期边界条件 </p>
 * <p> 会存在一些重复的代码，为了可读性不进一步消去 </p>
 * <p> 所有成员都是只读的，即使目前没有硬性限制 </p>
 * <p> 此类线程不安全（主要由于近邻列表缓存），但不同实例之间线程安全 </p>
 */
public class MonatomicParameterCalculator extends AbstractThreadPool<ParforThreadPool> {
    public static int BUFFER_NL_NUM = 8; // 缓存近邻列表从而避免重复计算距离，这里设置缓存的大小，设置为 0 关闭缓存；即使现在优化了近邻列表获取，缓存依旧能大幅加速近邻遍历
    public static double BUFFER_NL_RMAX = 4.0; // 最大的缓存近邻截断半径关于单位距离的倍率，过高的值的近邻列表缓存对内存是个灾难
    
    private IMatrix mAtomDataXYZ; // 现在改为 Matrix 存储，每行为一个原子的 xyz 数据
    private final IXYZ mBox;
    
    private final int mAtomNum;
    private final double mRou; // 粒子数密度
    private final double mUnitLen; // 平均单个原子的距离
    
    private NeighborListGetter mNL;
    private final long mInitThreadID;
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {
        mDead = true; super.shutdown();
        mNL.shutdown(); // 内部保证执行后内部的 mAtomDataXYZ 以及置为 null
        // 此时 MPC 关闭，归还 mAtomDataXYZ，这种写法保证永远能获取到 mAtomDataXYZ 时都是合法的
        // 只有相同线程关闭才会归还
        long tThreadID = Thread.currentThread().getId();
        if (tThreadID == mInitThreadID) {
            IMatrix oAtomDataXYZ = mAtomDataXYZ;
            mAtomDataXYZ = null;
            MatrixCache.returnMat(oAtomDataXYZ);
        } else {
            System.err.println("WARNING: ThreadID of shutdown() and init should be SAME in MonatomicParameterCalculator");
        }
        if (tThreadID == mInitBufferNLThreadID) {
            if (mBufferedNL != null) {
                BufferedNL oBufferedNL = mBufferedNL;
                mBufferedNL = null;
                sBufferedNLCache.returnObject(oBufferedNL);
            }
        }
    }
    @Override public void shutdownNow() {shutdown();}
    @Override public boolean isShutdown() {return mDead;}
    @Override public boolean isTerminated() {return mDead;}
    
    
    /**
     * 根据输入数据直接创建 MPC
     * @param aAtomDataXYZ 粒子数据，这里只需要知道 xyz 坐标即可
     * @param aBox 模拟盒大小；现在也统一认为所有输入的原子坐标都经过了 shift
     * @param aThreadNum MPC 进行计算会使用的线程数
     * @param aCellStep 内部用于加速近邻搜索的 LinkedCell 不同 Cell 大小的步长
     */
    public MonatomicParameterCalculator(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBox, int aThreadNum, double aCellStep) {
        super(new ParforThreadPool(aThreadNum));
        
        // 获取模拟盒数据
        mBox = newBox(aBox);
        
        // 获取合适的 XYZ 数据
        mAtomDataXYZ = getValidAtomDataXYZ_(aAtomDataXYZ);
        mAtomNum = aAtomDataXYZ.size();
        
        // 计算单位长度供内部使用
        mRou = mAtomNum / mBox.prod();
        mUnitLen = Fast.cbrt(1.0/mRou);
        
        mNL = new NeighborListGetter(mAtomDataXYZ, mAtomNum, mBox, aCellStep);
        mInitThreadID = Thread.currentThread().getId();
    }
    public MonatomicParameterCalculator(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBox) {this(aAtomDataXYZ, aBox, 1);}
    public MonatomicParameterCalculator(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBox, int aThreadNum) {this(aAtomDataXYZ, aBox, aThreadNum, DEFAULT_CELL_STEP);}
    
    public MonatomicParameterCalculator(IAtomData aAtomData) {this(aAtomData, 1);}
    public MonatomicParameterCalculator(IAtomData aAtomData, int aThreadNum) {this(aAtomData, aThreadNum, DEFAULT_CELL_STEP);}
    public MonatomicParameterCalculator(IAtomData aAtomData, int aThreadNum, double aCellStep) {this(aAtomData.asList(), aAtomData.box(), aThreadNum, aCellStep);}
    
    /** 主要用于内部使用 */
    MonatomicParameterCalculator(int aAtomNum, IXYZ aBox, int aThreadNum, IOperator1<IMatrix, IMatrix> aXYZValidOpt) {
        super(new ParforThreadPool(aThreadNum));
        mAtomNum = aAtomNum;
        mBox = aBox;
        mAtomDataXYZ = aXYZValidOpt.cal(MatrixCache.getMatRow(aAtomNum, 3));
        // 计算单位长度供内部使用
        mRou = mAtomNum / mBox.prod();
        mUnitLen = Fast.cbrt(1.0/mRou);
        mNL = new NeighborListGetter(mAtomDataXYZ, mAtomNum, mBox, DEFAULT_CELL_STEP);
        mInitThreadID = Thread.currentThread().getId();
    }
    
    
    /** 内部使用方法，用来将 aAtomDataXYZ 转换成内部存储的格式，并且处理精度问题造成的超出边界问题 */
    private IMatrix getValidAtomDataXYZ_(Collection<? extends IXYZ> aAtomDataXYZ) {
        int tSize = aAtomDataXYZ.size();
        // 尝试先获取缓存的临时变量
        IMatrix tXYZMat = MatrixCache.getMatRow(tSize, 3);
        
        // 遍历设置，顺便由于 lammps 精度的问题，需要将超出边界的进行平移
        XYZ tBox = XYZ.toXYZ(mBox);
        IDoubleSetIterator si = tXYZMat.setIteratorRow();
        for (IXYZ tXYZ : aAtomDataXYZ) {
            double tX = tXYZ.x();
            if      (tX <  0.0    ) tX += tBox.mX;
            else if (tX >= tBox.mX) tX -= tBox.mX;
            si.nextAndSet(tX);
            
            double tY = tXYZ.y();
            if      (tY <  0.0    ) tY += tBox.mY;
            else if (tY >= tBox.mY) tY -= tBox.mY;
            si.nextAndSet(tY);
            
            double tZ = tXYZ.z();
            if      (tZ <  0.0    ) tZ += tBox.mZ;
            else if (tZ >= tBox.mZ) tZ -= tBox.mZ;
            si.nextAndSet(tZ);
        }
        
        return tXYZMat;
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
    public MonatomicParameterCalculator setCellStep(double sCellStep) {if (sCellStep != mNL.getCellStep()) {mNL.shutdown(); mNL = new NeighborListGetter(mAtomDataXYZ, mAtomNum, mBox, sCellStep);} return this;}
    
    
    /// 获取信息
    public int atomNum() {return mAtomNum;}
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
        final List<? extends IVector> dn = VectorCache.getZeros(aN, nThreads());
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(mAtomNum, (i, threadID) -> {
            mNL.forEachNeighbor(i, aRMax - dr*0.5, true, (x, y, z, idx, dis2) -> {
                int tIdx = (int) Math.ceil((Fast.sqrt(dis2) - dr*0.5) / dr);
                if (tIdx > 0 && tIdx < aN) dn.get(threadID).increment_(tIdx);
            });
        });
        
        // 获取结果
        IFunc1 gr = FixBoundFunc1.zeros(0, dr, aN).setBound(0.0, 1.0);
        for (IVector subDn : dn) gr.f().plus2this(subDn);
        final double rou = dr * mAtomNum*0.5 * mRou; // mAtomNum*0.5 为对所有原子求和需要进行的平均
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*rou)));
        
        // 归还临时变量
        VectorCache.returnVec(dn);
        
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
     * @return gr 函数
     */
    public IFunc1 calRDF_AB(final IMatrix aAtomDataXYZ, int aAtomNum, int aN, final double aRMax) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final List<? extends IVector> dn = VectorCache.getZeros(aN, nThreads());
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(aAtomNum, (i, threadID) -> {
            mNL.forEachNeighbor(new XYZ(aAtomDataXYZ.row(i)), aRMax - dr*0.5, (x, y, z, idx, dis2) -> {
                int tIdx = (int) Math.ceil((Fast.sqrt(dis2) - dr*0.5) / dr);
                if (tIdx > 0 && tIdx < aN) dn.get(threadID).increment_(tIdx);
            });
        });
        
        
        // 获取结果
        IFunc1 gr = FixBoundFunc1.zeros(0, dr, aN).setBound(0.0, 1.0);
        for (IVector subDn : dn) gr.f().plus2this(subDn);
        final double rou = dr * aAtomNum * mRou; // aAtomDataXYZ.size() 为对所有原子求和需要进行的平均
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*rou)));
        
        // 归还临时变量
        VectorCache.returnVec(dn);
        
        // 修复截断数据
        gr.set_(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF_AB(Collection<? extends IXYZ>  aAtomDataXYZ) {return calRDF_AB(aAtomDataXYZ, 160);}
    public IFunc1 calRDF_AB(Collection<? extends IXYZ>  aAtomDataXYZ, int aN) {return calRDF_AB(aAtomDataXYZ, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB(Collection<? extends IXYZ>  aAtomDataXYZ, int aN, final double aRMax) {IMatrix tAtomDataXYZ = getValidAtomDataXYZ_(aAtomDataXYZ); IFunc1 tOut = calRDF_AB(tAtomDataXYZ, aAtomDataXYZ.size(), aN, aRMax); MatrixCache.returnMat(tAtomDataXYZ); return tOut;}
    public IFunc1 calRDF_AB(MonatomicParameterCalculator aMPC                                    ) {return calRDF_AB(aMPC, 160);}
    public IFunc1 calRDF_AB(MonatomicParameterCalculator aMPC        , int aN                    ) {return calRDF_AB(aMPC, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB(MonatomicParameterCalculator aMPC        , int aN, final double aRMax) {return calRDF_AB(aMPC.mAtomDataXYZ, aMPC.mAtomNum, aN, aRMax);} // aMPC 的 mAtomDataXYZ 都已经经过平移并且合理化
    
    
    
    /**
     * 使用带有一定展宽的高斯分布代替直接计数来计算 RDF
     * @author liqa
     * @param aN 指定分划的份数，这里需要更多的份数来得到合适的结果，默认为 1000
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @param aSigmaMul 高斯分布的一个标准差宽度对应的分划份数，默认为 4
     * @return gr 函数
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
            mNL.forEachNeighbor(i, aRMax+tRShift, true, (x, y, z, idx, dis2) -> {
                tDeltaG[threadID].setX0(Fast.sqrt(dis2));
                dn[threadID].plus2this(tDeltaG[threadID]);
            });
        });
        
        // 获取结果
        IFunc1 gr = dn[0];
        for (int i = 1; i < dn.length; ++i) gr.plus2this(dn[i]);
        final double rou = mAtomNum*0.5 * mRou; // mAtomNum*0.5 为对所有原子求和需要进行的平均
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*rou)));
        
        // 修复截断数据
        gr.set_(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF_G(                                         ) {return calRDF_G(1000);}
    public IFunc1 calRDF_G(int aN                                   ) {return calRDF_G(aN, mUnitLen*6);}
    public IFunc1 calRDF_G(int aN, final double aRMax               ) {return calRDF_G(aN, aRMax, 4);}
    
    
    public IFunc1 calRDF_AB_G(final IMatrix aAtomDataXYZ, int aAtomNum, int aN, final double aRMax, int aSigmaMul) {
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
        pool().parfor(aAtomNum, (i, threadID) -> {
            mNL.forEachNeighbor(new XYZ(aAtomDataXYZ.row(i)), aRMax+tRShift, (x, y, z, idx, dis2) -> {
                tDeltaG[threadID].setX0(Fast.sqrt(dis2));
                dn[threadID].plus2this(tDeltaG[threadID]);
            });
        });
        
        // 获取结果
        IFunc1 gr = dn[0];
        for (int i = 1; i < dn.length; ++i) gr.plus2this(dn[i]);
        final double rou = aAtomNum * mRou; // aAtomDataXYZ.size() 为对所有原子求和需要进行的平均
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*rou)));
        
        // 修复截断数据
        gr.set_(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF_AB_G(Collection<? extends IXYZ>  aAtomDataXYZ) {return calRDF_AB_G(aAtomDataXYZ, 1000);}
    public IFunc1 calRDF_AB_G(Collection<? extends IXYZ>  aAtomDataXYZ, int aN) {return calRDF_AB_G(aAtomDataXYZ, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB_G(Collection<? extends IXYZ>  aAtomDataXYZ, int aN, final double aRMax) {return calRDF_AB_G(aAtomDataXYZ, aN, aRMax, 4);}
    public IFunc1 calRDF_AB_G(Collection<? extends IXYZ>  aAtomDataXYZ, int aN, final double aRMax, int aSigmaMul) {IMatrix tAtomDataXYZ = getValidAtomDataXYZ_(aAtomDataXYZ); IFunc1 tOut = calRDF_AB_G(tAtomDataXYZ, aAtomDataXYZ.size(), aN, aRMax, aSigmaMul); MatrixCache.returnMat(tAtomDataXYZ); return tOut;}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC                                                   ) {return calRDF_AB_G(aMPC, 1000);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC        , int aN                                   ) {return calRDF_AB_G(aMPC, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC        , int aN, final double aRMax               ) {return calRDF_AB_G(aMPC, aN, aRMax, 4);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC        , int aN, final double aRMax, int aSigmaMul) {return calRDF_AB_G(aMPC.mAtomDataXYZ, aMPC.mAtomNum, aN, aRMax, aSigmaMul);} // aMPC 的 mAtomDataXYZ 都已经经过了平移
    
    
    
    /**
     * 计算 SF（structural factor，即 S(q)），结构参数，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aQMax 额外指定最大计算的 q 的位置（默认为 6 倍单位长度）
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @param aQMin 可以手动指定最小的截断的 q（由于 pbc 的原因，过小的结果发散）
     * @return Sq 函数
     */
    public IFunc1 calSF(double aQMax, int aN, final double aRMax, double aQMin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dq = (aQMax-aQMin)/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final IFunc1[] Hq = new IFunc1[nThreads()];
        for (int i = 0; i < Hq.length; ++i) Hq[i] = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        
        // 使用 mNL 的通用获取近邻的方法，因为 SF 需要使用方形半径内的所有距离（曼哈顿距离）
        pool().parfor(mAtomNum, (i, threadID) -> {
            final XYZ cXYZ = new XYZ(mAtomDataXYZ.row(i));
            mNL.forEachNeighborMHT(i, aRMax, true, (x, y, z, idx, disMHT) -> {
                double dis = cXYZ.distance(x, y, z);
                Hq[threadID].operation().mapFull2this((H, q) -> (H + Fast.sin(q*dis)/(q*dis)));
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
     * @return Sq 函数
     */
    public IFunc1 calSF_AB(final IMatrix aAtomDataXYZ, int aAtomNum, double aQMax, int aN, final double aRMax, double aQMin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dq = (aQMax-aQMin)/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final IFunc1[] Hq = new IFunc1[nThreads()];
        for (int i = 0; i < Hq.length; ++i) Hq[i] = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        
        // 使用 mNL 的通用获取近邻的方法，因为 SF 需要使用方形半径内的所有距离（曼哈顿距离）
        pool().parfor(aAtomNum, (i, threadID) -> {
            final XYZ cXYZ = new XYZ(aAtomDataXYZ.row(i));
            mNL.forEachNeighborMHT(cXYZ, aRMax, (x, y, z, idx, disMHT) -> {
                double dis = cXYZ.distance(x, y, z);
                Hq[threadID].operation().mapFull2this((H, q) -> (H + Fast.sin(q*dis)/(q*dis)));
            });
        });
        
        // 获取结果
        IFunc1 Sq = Hq[0];
        for (int i = 1; i < Hq.length; ++i) Sq.plus2this(Hq[i]);
        Sq.div2this(Fast.sqrt(mAtomNum*aAtomNum));
        Sq.plus2this(1.0);
        
        // 修复截断数据
        Sq.set_(0, 0.0);
        // 输出
        return Sq;
    }
    public IFunc1 calSF_AB(Collection<? extends IXYZ>  aAtomDataXYZ) {return calSF_AB(aAtomDataXYZ, 2.0*PI/mUnitLen * 6.0);}
    public IFunc1 calSF_AB(Collection<? extends IXYZ>  aAtomDataXYZ, double aQMax) {return calSF_AB(aAtomDataXYZ, aQMax, 160);}
    public IFunc1 calSF_AB(Collection<? extends IXYZ>  aAtomDataXYZ, double aQMax, int aN) {return calSF_AB(aAtomDataXYZ, aQMax, aN, mUnitLen*6);}
    public IFunc1 calSF_AB(Collection<? extends IXYZ>  aAtomDataXYZ, double aQMax, int aN, final double aRMax) {return calSF_AB(aAtomDataXYZ, aQMax, aN, aRMax, 2.0*PI/mUnitLen * 0.6);}
    public IFunc1 calSF_AB(Collection<? extends IXYZ>  aAtomDataXYZ, double aQMax, int aN, final double aRMax, double aQMin) {IMatrix tAtomDataXYZ = getValidAtomDataXYZ_(aAtomDataXYZ); IFunc1 tOut = calSF_AB(tAtomDataXYZ, aAtomDataXYZ.size(), aQMax, aN, aRMax, aQMin); MatrixCache.returnMat(tAtomDataXYZ); return tOut;}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC                                                                ) {return calSF_AB(aMPC, 2.0*PI/mUnitLen * 6.0);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax                                          ) {return calSF_AB(aMPC, aQMax, 160);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax, int aN                                  ) {return calSF_AB(aMPC, aQMax, aN, mUnitLen*6);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax, int aN, final double aRMax              ) {return calSF_AB(aMPC, aQMax, aN, aRMax, 2.0*PI/mUnitLen * 0.6);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC        , double aQMax, int aN, final double aRMax, double aQMin) {return calSF_AB(aMPC.mAtomDataXYZ, aMPC.mAtomNum, aQMax, aN, aRMax, aQMin);}
    
    
    
    /// gr 和 Sq 的相互转换，由于依旧需要体系的原子数密度，因此还是都移动到 MPC 中
    /**
     * 转换 g(r) 到 S(q)，这是主要计算 S(q) 的方法
     * @author liqa
     * @param aGr the matrix form of g(r)
     * @param aRou the atom number density（默认会选择本 MPC 得到的密度）
     * @param aN the split number of output（默认为 160）
     * @param aQMax the max q of output S(q)（默认为 7.6 倍 gr 第一峰对应的距离）
     * @param aQMin the min q of output S(q)（默认为 0.5 倍 gr 第一峰对应的距离）
     * @return the structural factor, S(q)
     */
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRou, int aN, double aQMax, double aQMin) {
        double dq = (aQMax-aQMin)/aN;
        
        IFunc1 Sq = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        Sq.fill(aGr.operation().refConvolveFull((gr, r, q) -> (r * (gr-1.0) * Fast.sin(q*r) / q)));
        Sq.multiply2this(4.0*PI*aRou);
        Sq.plus2this(1.0);
        
        Sq.set_(0, 0.0);
        return Sq;
    }
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRou, int aN, double aQMax) {return RDF2SF(aGr, aRou, aN, aQMax, 2.0*PI/aGr.operation().maxX() * 0.5);}
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRou, int aN              ) {return RDF2SF(aGr, aRou, aN, 2.0*PI/aGr.operation().maxX()* 7.6, 2.0*PI/aGr.operation().maxX() * 0.5);}
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRou                      ) {return RDF2SF(aGr, aRou, 160);}
    public        IFunc1 RDF2SF(IFunc1 aGr                                   ) {return RDF2SF(aGr, mRou);}
    
    
    /**
     * 转换 S(q) 到 g(r)
     * @author liqa
     * @param aSq the matrix form of S(q)
     * @param aRou the atom number density（默认会选择本 MPC 得到的密度）
     * @param aN the split number of output（默认为 160）
     * @param aRMax the max r of output g(r)（默认为 7.6 倍 Sq 第一峰对应的距离）
     * @param aRMin the min r of output g(r)（默认为 0.5 倍 Sq 第一峰对应的距离）
     * @return the radial distribution function, g(r)
     */
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRou, int aN, double aRMax, double aRMin) {
        double dr = (aRMax-aRMin)/aN;
        
        IFunc1 gr = FixBoundFunc1.zeros(aRMin, dr, aN+1).setBound(0.0, 1.0);
        gr.fill(aSq.operation().refConvolveFull((Sq, q, r) -> (q * (Sq-1.0) * Fast.sin(q*r) / r)));
        gr.multiply2this(1.0/(2.0*PI*PI*aRou));
        gr.plus2this(1.0);
        
        gr.set_(0, 0.0);
        return gr;
    }
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRou, int aN, double aRMax) {return SF2RDF(aSq, aRou, aN, aRMax, 2.0*PI/aSq.operation().maxX() * 0.5);}
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRou, int aN              ) {return SF2RDF(aSq, aRou, aN, 2.0*PI/aSq.operation().maxX() * 7.6, 2.0*PI/aSq.operation().maxX() * 0.5);}
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRou                      ) {return SF2RDF(aSq, aRou, 160);}
    public        IFunc1 SF2RDF(IFunc1 aSq                                   ) {return SF2RDF(aSq, mRou);}
    
    
    
    /**
     * 直接获取近邻列表的 api，不包括自身
     * @author liqa
     */
    public List<Integer> getNeighborList(int aIdx, double aRMax, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntegerList @Nullable[] tNL = getValidBufferedNL_(aRMax, aNnn, false);
        if (tNL != null) return tNL[aIdx].asConstList();
        // 如果为 null 则尝试全部遍历一次缓存所有近邻列表
        final IntegerList @Nullable[] tNLToBuffer = getNLWhichNeedBuffer_(aRMax, aNnn, false);
        if (tNLToBuffer != null) {
            for (int i = 0; i < mAtomNum; ++i) {
                final IntegerList subNL = tNLToBuffer[i];
                mNL.forEachNeighbor(i, aRMax, aNnn, (x, y, z, idx, dis2) -> subNL.add(idx));
            }
            return tNLToBuffer[aIdx].asConstList();
        }
        // 否则直接遍历指定 idx 即可
        final List<Integer> rNeighborList = new ArrayList<>();
        mNL.forEachNeighbor(aIdx, aRMax, aNnn, (x, y, z, idx, dis2) -> rNeighborList.add(idx));
        return rNeighborList;
    }
    public List<Integer> getNeighborList(int aIdx, double aRMax) {return getNeighborList(aIdx, aRMax, -1);}
    public List<Integer> getNeighborList(int aIdx              ) {return getNeighborList(aIdx, mUnitLen*R_NEAREST_MUL);}
    
    public List<Integer> getNeighborList(IXYZ aXYZ, double aRMax, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 为了方便统一拷贝一次输入 XYZ
        XYZ tXYZ = new XYZ(aXYZ);
        // 由于 lammps 精度的问题，需要将超出边界的进行平移
        XYZ tBox = XYZ.toXYZ(mBox);
        if      (tXYZ.mX <  0.0    ) tXYZ.mX += tBox.mX;
        else if (tXYZ.mX >= tBox.mX) tXYZ.mX -= tBox.mX;
        if      (tXYZ.mY <  0.0    ) tXYZ.mY += tBox.mY;
        else if (tXYZ.mY >= tBox.mY) tXYZ.mY -= tBox.mY;
        if      (tXYZ.mZ <  0.0    ) tXYZ.mZ += tBox.mZ;
        else if (tXYZ.mZ >= tBox.mZ) tXYZ.mZ -= tBox.mZ;
        
        final List<Integer> rNeighborList = new ArrayList<>();
        mNL.forEachNeighbor(tXYZ, aRMax, aNnn, (x, y, z, idx, dis2) -> rNeighborList.add(idx));
        return rNeighborList;
    }
    public List<Integer> getNeighborList(IXYZ aXYZ, double aRMax) {return getNeighborList(aXYZ, aRMax, -1);}
    public List<Integer> getNeighborList(IXYZ aXYZ              ) {return getNeighborList(aXYZ, mUnitLen*R_NEAREST_MUL);}
    
    
    /** 用于分割模拟盒，判断给定 XYZ 或者 idx 处的原子是否在需要考虑的区域中 */
    private class MPIInfo implements IAutoShutdown {
        private final double mXLo, mXHi, mYLo, mYHi, mZLo, mZHi;
        final MPI.Comm mComm;
        final int mRank, mSize;
        final int mSizeX, mSizeY, mSizeZ;
        MPIInfo(MPI.Comm aComm) throws MPI.Error {
            mComm = aComm;
            mRank = mComm.rank();
            mSize = mComm.size();
            int tSizeRest = mSize;
            // 使用这个方法来获取每个方向的分划数
            Deque<Integer> rFactors = new ArrayDeque<>();
            for (int tFactor = 2; tFactor <= tSizeRest; ++tFactor) {
                while (tSizeRest % tFactor == 0) {
                    rFactors.addFirst(tFactor); // 直接使用 addFirst 来实现逆序的作用
                    tSizeRest /= tFactor;
                }
            }
            int rSizeX = 1, rSizeY = 1, rSizeZ = 1;
            for (int tFactor : rFactors) {
                if (rSizeX <= rSizeY && rSizeX <= rSizeZ) {
                    rSizeX *= tFactor;
                } else
                if (rSizeY <= rSizeX && rSizeY <= rSizeZ) {
                    rSizeY *= tFactor;
                } else {
                    rSizeZ *= tFactor;
                }
            }
            mSizeX = rSizeX; mSizeY = rSizeY; mSizeZ = rSizeZ;
            // 根据分划数获取对应的 mXLo, mXhi, mYLo, mYHi, mZLo, mZHi
            int tI = mRank/mSizeZ/mSizeY;
            int tJ = mRank/mSizeZ%mSizeY;
            int tK = mRank%mSizeZ;
            XYZ tCellSize = mBox.div(mSizeX, mSizeY, mSizeZ);
            mXLo = tI * tCellSize.mX; mXHi = mXLo + tCellSize.mX;
            mYLo = tJ * tCellSize.mY; mYHi = mYLo + tCellSize.mY;
            mZLo = tK * tCellSize.mZ; mZHi = mZLo + tCellSize.mZ;
        }
        
        /** Regin stuffs */
        @SuppressWarnings("RedundantIfStatement")
        boolean inRegin(IXYZ aXYZ) {
            double tX = aXYZ.x();
            if (tX < mXLo || tX >= mXHi) return false;
            double tY = aXYZ.y();
            if (tY < mYLo || tY >= mYHi) return false;
            double tZ = aXYZ.z();
            if (tZ < mZLo || tZ >= mZHi) return false;
            return true;
        }
        @SuppressWarnings("RedundantIfStatement")
        boolean inRegin(int aIdx) {
            double tX = mAtomDataXYZ.get(aIdx, 0);
            if (tX < mXLo || tX >= mXHi) return false;
            double tY = mAtomDataXYZ.get(aIdx, 1);
            if (tY < mYLo || tY >= mYHi) return false;
            double tZ = mAtomDataXYZ.get(aIdx, 2);
            if (tZ < mZLo || tZ >= mZHi) return false;
            return true;
        }
        boolean inRegin(double aX, double aY, double aZ) {
            return (aX >= mXLo) && (aX < mXHi) && (aY >= mYLo) && (aY < mYHi) && (aZ >= mZLo) && (aZ < mZHi);
        }
        
        /** Gather stuffs */
        private boolean mInitialized = false;
        private int[] mCounts;
        private int[] mDispls;
        private int[] mBuf2Idx;
        private void init_() {
            if (mDead) throw new RuntimeException("This MPIInfo is dead");
            if (mInitialized) return;
            mInitialized = true;
            mCounts = IntegerArrayCache.getZeros(mSize);
            final int[][] rBuf2Idx = new int[mSize][];
            IntegerArrayCache.getArrayTo(mAtomNum, mSize, (i, array) -> rBuf2Idx[i] = array);
            // 遍历所有的原子统计位置
            XYZ tCellSize = mBox.div(mSizeX, mSizeY, mSizeZ);
            IDoubleIterator it = mAtomDataXYZ.iteratorRow();
            for (int i = 0; i < mAtomNum; ++i) {
                double tX = it.next();
                double tY = it.next();
                double tZ = it.next();
                int tI = (int) Math.floor(tX / tCellSize.mX);
                int tJ = (int) Math.floor(tY / tCellSize.mY);
                int tK = (int) Math.floor(tZ / tCellSize.mZ);
                int tRank = (tI*mSizeY*mSizeZ + tJ*mSizeZ + tK); // 这里的排序和 LinkedCell 不同，但是和新的 AbstractAtoms 的一直，为了避免问题这里暂时不去改 LinkedCell 的排序
                int tCount = mCounts[tRank];
                rBuf2Idx[tRank][tCount] = i;
                ++mCounts[tRank];
            }
            mBuf2Idx = IntegerArrayCache.getArray(mAtomNum);
            mDispls = IntegerArrayCache.getArray(mSize);
            mDispls[0] = 0;
            for (int i = 0; i < mSize; ++i) {
                int tStart = mDispls[i];
                int tCount = mCounts[i];
                System.arraycopy(rBuf2Idx[i], 0, mBuf2Idx, tStart, tCount);
                if (i != mSize-1) mDispls[i+1] = tStart + tCount;
            }
            IntegerArrayCache.returnArrayFrom(mSize, i -> rBuf2Idx[i]);
        }
        int[] counts() {
            if (!mInitialized) init_();
            return mCounts;
        }
        int[] displs() {
            if (!mInitialized) init_();
            return mDispls;
        }
        int[] buf2idx() {
            if (!mInitialized) init_();
            return mBuf2Idx;
        }
        
        private boolean mDead = false;
        @Override public void shutdown() {
            if (mDead) return;
            mDead = true;
            if (mInitialized) {
                IntegerArrayCache.returnArray(mCounts);
                IntegerArrayCache.returnArray(mDispls);
                IntegerArrayCache.returnArray(mBuf2Idx);
            }
        }
    }
    
    
    private static class BufferedNL {
        private final static int INIT_NL_SIZE = 16;
        
        private final IVector mBufferedNLRMax;
        private final boolean[] mBufferedNLHalf;
        private final int[] mBufferedNLMPI;
        private final int[] mBufferedNLNnn;
        private final IntegerList[][] mBufferedNL;
        private int mAtomNum;
        private int mSize;
        private int mIdx;
        
        BufferedNL(int aSize, int aAtomNum) {
            mAtomNum = aAtomNum;
            mSize = aSize;
            mIdx = 0;
            mBufferedNLRMax = Vectors.NaN(mSize);
            mBufferedNLHalf = new boolean[mSize];
            mBufferedNLMPI = new int[mSize];
            mBufferedNLNnn = new int[mSize];
            mBufferedNL = new IntegerList[mSize][];
        }
        
        int size() {return mSize;}
        void setSize(int aSize) {mSize = MathEX.Code.toRange(0, mBufferedNL.length, aSize);}
        int dataLength() {return mBufferedNL.length;}
        void reset() {mIdx = 0; mBufferedNLRMax.fill(Double.NaN);}
        void setAtomNum(int aAtomNum) {mAtomNum = aAtomNum;}
        
        
        /**
         * 根据参数获取合适的 NL 用于缓存，
         * 此时 null 表示没有合适的缓存的近邻列表
         */
        IntegerList @Nullable[] getValidBufferedNL(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
            // 直接遍历搜索
            for (int i = 0; i < mSize; ++i) {
                if (mBufferedNLNnn[i]==aNnn && mBufferedNLHalf[i]==aHalf && mBufferedNLMPI[i]==aMPISize) {
                    IntegerList @Nullable[] tNL = mBufferedNL[i];
                    if (tNL == null) continue;
                    double tNLRMax = mBufferedNLRMax.get(i);
                    if (Double.isNaN(tNLRMax)) continue;
                    if (MathEX.Code.numericEqual(tNLRMax, aRMax)) return tNL;
                }
            }
            return null;
        }
        
        /**
         * 根据参数获取合适的 NL 用于缓存，
         * 此时不会返回 null
         */
        IntegerList @NotNull[] getValidNLToBuffer(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
            // 这里直接根据 mIdx 返回
            mBufferedNLRMax.set(mIdx, aRMax);
            mBufferedNLHalf[mIdx] = aHalf;
            mBufferedNLMPI[mIdx]= aMPISize;
            mBufferedNLNnn[mIdx] = aNnn;
            @Nullable IntegerList @Nullable[] tNL = mBufferedNL[mIdx];
            if (tNL==null || tNL.length<mAtomNum) {
                IntegerList @Nullable[] oNL = tNL;
                tNL = new IntegerList[mAtomNum];
                if (oNL != null) System.arraycopy(oNL, 0, tNL, 0, oNL.length);
            }
            mBufferedNL[mIdx] = tNL;
            for (int i = 0; i < mAtomNum; ++i) {
                @Nullable IntegerList subNL = tNL[i];
                if (subNL == null) {
                    subNL = new IntegerList(INIT_NL_SIZE);
                    tNL[i] = subNL;
                } else {
                    subNL.clear();
                }
            }
            ++mIdx;
            if (mIdx == mSize) mIdx = 0;
            return tNL;
        }
    }
    
    // 简单处理这里直接缓存整个对象，而内部不再缓存
    private @Nullable BufferedNL mBufferedNL = null;
    private long mInitBufferNLThreadID = -1;
    private final static IObjectPool<BufferedNL> sBufferedNLCache = new ThreadLocalObjectCachePool<>();
    
    private void initBufferNL_() {
        if (mBufferedNL != null) return;
        if (BUFFER_NL_NUM <= 0) return;
        mBufferedNL = sBufferedNLCache.getObject();
        if (mBufferedNL==null || mBufferedNL.dataLength()<BUFFER_NL_NUM) {
            mBufferedNL = new BufferedNL(BUFFER_NL_NUM, mAtomNum);
        } else {
            mBufferedNL.setSize(BUFFER_NL_NUM);
            mBufferedNL.setAtomNum(mAtomNum);
            mBufferedNL.reset();
        }
        mInitBufferNLThreadID = Thread.currentThread().getId();
        if (mInitBufferNLThreadID != mInitThreadID) System.err.println("WARNING: ThreadID of initBufferNL() and init should be SAME in MonatomicParameterCalculator");
    }
    
    /**
     * 根据参数获取合适的 NL 用于缓存，
     * 此时 null 表示关闭了近邻列表缓存或者没有合适的缓存的近邻列表
     */
    private IntegerList @Nullable[] getValidBufferedNL_(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
        initBufferNL_();
        if (mBufferedNL == null) return null;
        return mBufferedNL.getValidBufferedNL(aRMax, aNnn, aHalf, aMPISize);
    }
    private IntegerList @Nullable[] getValidBufferedNL_(double aRMax, int aNnn, boolean aHalf) {
        return getValidBufferedNL_(aRMax, aNnn, aHalf, 1);
    }
    /**
     * 根据参数获取合适的 NL 用于缓存，
     * 此时 null 表示已经有了缓存或者关闭了近邻列表缓存或者参数非法（近邻半径过大）
     */
    private IntegerList @Nullable[] getNLWhichNeedBuffer_(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
        initBufferNL_();
        if (mBufferedNL == null) return null;
        if (aRMax > BUFFER_NL_RMAX*mUnitLen) return null;
        if (mBufferedNL.getValidBufferedNL(aRMax, aNnn, aHalf, aMPISize) != null) return null;
        return mBufferedNL.getValidNLToBuffer(aRMax, aNnn, aHalf, aMPISize);
    }
    private IntegerList @Nullable[] getNLWhichNeedBuffer_(double aRMax, int aNnn, boolean aHalf) {
        return getNLWhichNeedBuffer_(aRMax, aNnn, aHalf, 1);
    }
    
    /** 会自动使用缓存的近邻列表遍历，用于减少重复代码 */
    private void forEachNeighbor(IntegerList @Nullable[] aNL, int aIdx, double aRMax, int aNnn, boolean aHalf, IIntegerConsumer1 aIdxDo) {
        if (aNL != null) {
            // 如果 aNL 不为 null，则直接使用 aNL 遍历
            aNL[aIdx].forEach(aIdxDo);
        } else {
            // aNL 为 null，则使用 mNL 完整遍历
            mNL.forEachNeighbor(aIdx, aRMax, aNnn, aHalf, (x, y, z, idx, dis2) -> aIdxDo.run(idx));
        }
    }
    private void forEachNeighbor(IntegerList @Nullable[] aNL, int aIdx, double aRMax, int aNnn, boolean aHalf, IIndexFilter aRegion, IIntegerConsumer1 aIdxDo) {
        if (aNL != null) {
            // 如果 aNL 不为 null，则直接使用 aNL 遍历
            aNL[aIdx].forEach(aIdxDo);
        } else {
            // aNL 为 null，则使用 mNL 完整遍历
            mNL.forEachNeighbor(aIdx, aRMax, aNnn, aHalf, aRegion, (x, y, z, idx, dis2) -> aIdxDo.run(idx));
        }
    }
    
    
    /**
     * 计算所有粒子的近邻球谐函数的平均，即 Qlm；
     * 返回一个复数矩阵，行为原子，列为 m
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 主要用于内部使用，由于对象较大这里返回 cache 的值，
     * 从而可以通过 {@link ComplexMatrixCache#returnMat} 来实现对象重复利用
     * @author liqa
     * @param aL 计算具体 Qlm 值的下标，即 Q4m: l = 4, Q6m: l = 6
     * @param aRNearest 用来搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return Qlm 组成的复向量数组
     */
    public IComplexMatrix calYlmMean(final int aL, double aRNearest, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aL < 0) throw new IllegalArgumentException("Input l MUST be Non-Negative, input: "+aL);
        
        // 构造用于并行的暂存数组，注意需要初始值为 0.0
        final List<? extends IComplexMatrix> rDestPar = ComplexMatrixCache.getZerosRow(mAtomNum, aL+aL+1, nThreads());
        // 统计近邻数用于求平均，同样也需要为并行使用数组
        final List<? extends IVector> tNNPar = VectorCache.getZeros(mAtomNum, nThreads());
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnn<=0;
        
        // 全局暂存 Y 和 P 的数组，这样可以用来防止重复获取来提高效率
        final List<? extends IComplexVector> tYPar = ComplexVectorCache.getVec(aL+aL+1, nThreads());
        final List<Vector> tBufPPar = VectorCache.getVec((aL+2)*(aL+1)/2, nThreads());
        
        // 获取需要缓存的近邻列表
        final IntegerList @Nullable[] tNLToBuffer = getNLWhichNeedBuffer_(aRNearest, aNnn, aHalf);
        
        // 遍历计算 Qlm，只对这个最耗时的部分进行并行优化
        pool().parfor(mAtomNum, (i, threadID) -> {
            // 先获取这个线程的 Qlm, tNN
            final IComplexMatrix Qlm = rDestPar.get(threadID);
            final IVector tNN = tNNPar.get(threadID);
            final IComplexVector tY = tYPar.get(threadID);
            final Vector tBufP = tBufPPar.get(threadID);
            // 一次计算一行
            final IComplexVector Qlmi = Qlm.row(i);
            final XYZ cXYZ = new XYZ(mAtomDataXYZ.row(i));
            // 遍历近邻计算 Ylm
            mNL.forEachNeighbor(i, aRNearest, aNnn, aHalf, (x, y, z, idx, dis2) -> {
                // 计算角度
                double dx = x - cXYZ.mX;
                double dy = y - cXYZ.mY;
                double dz = z - cXYZ.mZ;
                double theta = Fast.acos(dz / Fast.sqrt(dis2));
                double disXY = Fast.hypot(dx, dy);
                double phi = (dy > 0) ? Fast.acos(dx / disXY) : (2.0*PI - Fast.acos(dx / disXY));
                
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                IComplexVector Qlmj = null;
                if (aHalf) {
                    Qlmj = Qlm.row(idx);
                }
                // 计算 Y 并累加，考虑对称性只需要算 m=0~l 的部分
                Func.sphericalHarmonicsTL2Dest(aL, theta, phi, tY, tBufP);
                Qlmi.plus2this(tY);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) Qlmj.plus2this(tY);
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[i].add(idx);}
                
                // 统计近邻数
                tNN.increment_(i);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) {
                    tNN.increment_(idx);
                }
            });
        });
        
        // 获取结果
        IVector tNN = tNNPar.get(0);
        for (int i = 1; i < tNNPar.size(); ++i) tNN.plus2this(tNNPar.get(i));
        IComplexMatrix Qlm = rDestPar.get(0);
        for (int i = 1; i < rDestPar.size(); ++i) {
            IComplexMatrix subQlm = rDestPar.get(i);
            Qlm.plus2this(subQlm);
        }
        // 根据近邻数平均得到 Qlm
        for (int i = 0; i < mAtomNum; ++i) Qlm.row(i).div2this(tNN.get_(i));
        
        // 归还临时变量
        for (int i = 1; i < rDestPar.size(); ++i) ComplexMatrixCache.returnMat(rDestPar.get(i));
        VectorCache.returnVec(tNNPar);
        ComplexVectorCache.returnVec(tYPar);
        VectorCache.returnVec(tBufPPar);
        
        return Qlm;
    }
    public IComplexMatrix calYlmMean(int aL, double aRNearest) {return calYlmMean(aL, aRNearest, -1);}
    public IComplexMatrix calYlmMean(int aL                  ) {return calYlmMean(aL, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的计算 计算所有粒子的近邻球谐函数的平均，即 Qlm */
    private IComplexMatrix calYlmMean_MPI(boolean aNoGather, MPIInfo aMPIInfo, final int aL, double aRNearest, int aNnn) throws MPI.Error {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aL < 0) throw new IllegalArgumentException("Input l MUST be Non-Negative, input: "+aL);
        
        // 构造用于输出的暂存数组，注意需要初始值为 0.0
        final RowComplexMatrix Qlm = ComplexMatrixCache.getZerosRow(mAtomNum, aL+aL+1);
        // 统计近邻数用于求平均
        final IVector tNN = VectorCache.getZeros(mAtomNum);
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnn<=0;
        
        // 全局暂存 Y 和 P 的数组，这样可以用来防止重复获取来提高效率
        final IComplexVector tY = ComplexVectorCache.getVec(aL+aL+1);
        final Vector tBufP = VectorCache.getVec((aL+2)*(aL+1)/2);
        
        // 获取需要缓存的近邻列表
        final IntegerList @Nullable[] tNLToBuffer = getNLWhichNeedBuffer_(aRNearest, aNnn, aHalf, aMPIInfo.mSize);
        
        // 遍历计算 Qlm，这里直接判断原子位置是否是需要计算的然后跳过
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 一次计算一行
            final IComplexVector Qlmi = Qlm.row(i);
            final XYZ cXYZ = new XYZ(mAtomDataXYZ.row(i));
            // 遍历近邻计算 Ylm
            final int fI = i;
            mNL.forEachNeighbor(fI, aRNearest, aNnn, aHalf, aMPIInfo::inRegin, (x, y, z, idx, dis2) -> {
                // 计算角度
                double dx = x - cXYZ.mX;
                double dy = y - cXYZ.mY;
                double dz = z - cXYZ.mZ;
                double theta = Fast.acos(dz / Fast.sqrt(dis2));
                double disXY = Fast.hypot(dx, dy);
                double phi = (dy > 0) ? Fast.acos(dx / disXY) : (2.0*PI - Fast.acos(dx / disXY));
                
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                IComplexVector Qlmj = null;
                if (tHalfStat) {
                    Qlmj = Qlm.row(idx);
                }
                // 计算 Y 并累加，考虑对称性只需要算 m=0~l 的部分
                Func.sphericalHarmonicsTL2Dest(aL, theta, phi, tY, tBufP);
                Qlmi.plus2this(tY);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (tHalfStat) Qlmj.plus2this(tY);
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
                
                // 统计近邻数
                tNN.increment_(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (tHalfStat) {
                    tNN.increment_(idx);
                }
            });
        }
        
        // 根据近邻数平均得到 Qlm
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            Qlm.row(i).div2this(tNN.get_(i));
        }
        // 归还临时变量
        VectorCache.returnVec(tNN);
        ComplexVectorCache.returnVec(tY);
        VectorCache.returnVec(tBufP);
        
        // 收集所有进程将统计到的 Qlm，现在可以直接一起同步保证效率
        if (!aNoGather) {
            int tMul = aL+aL+1;
            // 先创建临时的同步数组
            RowComplexMatrix rBuf = ComplexMatrixCache.getMatRow(mAtomNum, tMul);
            // 获取 buf2idx, counts 和 displs 来将需要的数据放入 rBuf 指定位置
            int[] tBuf2Idx = aMPIInfo.buf2idx();
            int[] tCounts = aMPIInfo.counts();
            int[] tDispls = aMPIInfo.displs();
            final int tStart = tDispls[aMPIInfo.mRank];
            final int tEnd = tStart + tCounts[aMPIInfo.mRank];
            for (int i = tStart; i < tEnd; ++i) {
                rBuf.row(i).fill(Qlm.row(tBuf2Idx[i]));
            }
            // 使用 allgatherv 收集所有 buf，注意实际 tCounts 和 tDispls 需要增倍
            int[] tCountsMul = IntegerArrayCache.getArray(aMPIInfo.mSize);
            int[] tDisplsMul = IntegerArrayCache.getArray(aMPIInfo.mSize);
            for (int i = 0; i < aMPIInfo.mSize; ++i) tCountsMul[i] = tCounts[i] * tMul;
            for (int i = 0; i < aMPIInfo.mSize; ++i) tDisplsMul[i] = tDispls[i] * tMul;
            double[][] tData = rBuf.internalData();
            aMPIInfo.mComm.allgatherv(tData[0], tCountsMul, tDisplsMul);
            aMPIInfo.mComm.allgatherv(tData[1], tCountsMul, tDisplsMul);
            IntegerArrayCache.returnArray(tCountsMul);
            IntegerArrayCache.returnArray(tDisplsMul);
            // 将在 buf 中的数据重新设回 Qlm
            for (int i = 0; i < mAtomNum; ++i) {
                Qlm.row(tBuf2Idx[i]).fill(rBuf.row(i));
            }
            // 归还临时数组
            ComplexMatrixCache.returnMat(rBuf);
        }
        
        return Qlm;
    }
    public IComplexMatrix calYlmMean_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPI.Error {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calYlmMean_MPI(aNoGather, tMPIInfo, aL, aRNearest, aNnn);}}
    public IComplexMatrix calYlmMean_MPI(MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPI.Error {return calYlmMean_MPI(false, aComm, aL, aRNearest, aNnn);}
    
    
    /**
     * 在 Qlm 基础上再次对所有近邻做一次平均，即 qlm；
     * 返回一个复数矩阵，行为原子，列为 m
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     * <p>
     * 主要用于内部使用，由于对象较大这里返回 cache 的值，
     * 从而可以通过 {@link ComplexMatrixCache#returnMat} 来实现对象重复利用
     * @author liqa
     * @param aL 计算具体 qlm 值的下标，即 q4m: l = 4, q6m: l = 6
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 的最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return qlm 组成的复向量数组
     */
    public IComplexMatrix calQlmMean(int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) {
        // 直接全部平均一遍分两步算
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aL < 0) throw new IllegalArgumentException("Input l MUST be Non-Negative, input: "+aL);
        
        final IComplexMatrix Qlm = calYlmMean(aL, aRNearestY, aNnnY);
        final IComplexMatrix qlm = ComplexMatrixCache.getZerosRow(mAtomNum, aL+aL+1);
        
        // 统计近邻数用于求平均（增加一个自身）
        final IVector tNN = VectorCache.getVec(mAtomNum);
        tNN.fill(1.0);
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnQ<=0;
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntegerList @Nullable[] tNL = getValidBufferedNL_(aRNearestQ, aNnnQ, aHalf);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntegerList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestQ, aNnnQ, aHalf) : null;
        
        // 遍历计算 qlm
        for (int i = 0; i < mAtomNum; ++i) {
            // 一次计算一行
            final IComplexVector qlmi = qlm.row(i);
            final IComplexVector Qlmi = Qlm.row(i);
            
            // 先累加自身（不直接拷贝矩阵因为以后会改成复数向量的数组）
            qlmi.fill(Qlmi);
            // 再累加近邻
            final int fI = i;
            forEachNeighbor(tNL, fI, aRNearestQ, aNnnQ, aHalf, idx -> {
                // 直接按行累加即可
                qlmi.plus2this(Qlm.row(idx));
                // 如果开启 half 遍历的优化，对称的对面的粒子也要进行累加
                if (aHalf) {
                    qlm.row(idx).plus2this(Qlmi);
                }
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
                
                // 统计近邻数
                tNN.increment_(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) {
                    tNN.increment_(idx);
                }
            });
        }
        // 根据近邻数平均得到 qlm
        for (int i = 0; i < mAtomNum; ++i) qlm.row(i).div2this(tNN.get_(i));
        
        // 归还临时变量
        VectorCache.returnVec(tNN);
        ComplexMatrixCache.returnMat(Qlm);
        
        return qlm;
    }
    public IComplexMatrix calQlmMean(int aL, double aRNearest, int aNnn) {return calQlmMean(aL, aRNearest, aNnn, aRNearest, aNnn);}
    public IComplexMatrix calQlmMean(int aL, double aRNearest          ) {return calQlmMean(aL, aRNearest, -1);}
    public IComplexMatrix calQlmMean(int aL                            ) {return calQlmMean(aL, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的在 Qlm 基础上再次对所有近邻做一次平均，即 qlm */
    private IComplexMatrix calQlmMean_MPI(boolean aNoGather, final MPIInfo aMPIInfo, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPI.Error {
        // 直接全部平均一遍分两步算
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aL < 0) throw new IllegalArgumentException("Input l MUST be Non-Negative, input: "+aL);
        
        final IComplexMatrix Qlm = calYlmMean_MPI(false, aMPIInfo, aL, aRNearestY, aNnnY);
        final RowComplexMatrix qlm = ComplexMatrixCache.getZerosRow(mAtomNum, aL+aL+1);
        
        // 统计近邻数用于求平均（增加一个自身）
        final IVector tNN = VectorCache.getVec(mAtomNum);
        tNN.fill(1.0);
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnQ<=0;
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntegerList @Nullable[] tNL = getValidBufferedNL_(aRNearestQ, aNnnQ, aHalf, aMPIInfo.mSize);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntegerList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestQ, aNnnQ, aHalf, aMPIInfo.mSize) : null;
        
        // 遍历计算 Qlm，这里直接判断原子位置是否是需要计算的然后跳过
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 一次计算一行
            final IComplexVector qlmi = qlm.row(i);
            final IComplexVector Qlmi = Qlm.row(i);
            
            // 先累加自身（不直接拷贝矩阵因为以后会改成复数向量的数组）
            qlmi.fill(Qlmi);
            // 再累加近邻
            final int fI = i;
            forEachNeighbor(tNL, fI, aRNearestQ, aNnnQ, aHalf, aMPIInfo::inRegin, idx -> {
                // 直接按行累加即可
                qlmi.plus2this(Qlm.row(idx));
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                if (tHalfStat) {
                    qlm.row(idx).plus2this(Qlmi);
                }
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
                
                // 统计近邻数
                tNN.increment_(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (tHalfStat) {
                    tNN.increment_(idx);
                }
            });
        }
        // 根据近邻数平均得到 qlm
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            qlm.row(i).div2this(tNN.get_(i));
        }
        
        // 归还临时变量
        VectorCache.returnVec(tNN);
        ComplexMatrixCache.returnMat(Qlm);

        // 收集所有进程将统计到的 qlm，现在可以直接一起同步保证效率
        if (!aNoGather) {
            int tMul = aL+aL+1;
            // 先创建临时的同步数组
            RowComplexMatrix rBuf = ComplexMatrixCache.getMatRow(mAtomNum, tMul);
            // 获取 buf2idx, counts 和 displs 来将需要的数据放入 rBuf 指定位置
            int[] tBuf2Idx = aMPIInfo.buf2idx();
            int[] tCounts = aMPIInfo.counts();
            int[] tDispls = aMPIInfo.displs();
            final int tStart = tDispls[aMPIInfo.mRank];
            final int tEnd = tStart + tCounts[aMPIInfo.mRank];
            for (int i = tStart; i < tEnd; ++i) {
                rBuf.row(i).fill(qlm.row(tBuf2Idx[i]));
            }
            // 使用 allgatherv 收集所有 buf，注意实际 tCounts 和 tDispls 需要增倍
            int[] tCountsMul = IntegerArrayCache.getArray(aMPIInfo.mSize);
            int[] tDisplsMul = IntegerArrayCache.getArray(aMPIInfo.mSize);
            for (int i = 0; i < aMPIInfo.mSize; ++i) tCountsMul[i] = tCounts[i] * tMul;
            for (int i = 0; i < aMPIInfo.mSize; ++i) tDisplsMul[i] = tDispls[i] * tMul;
            double[][] tData = rBuf.internalData();
            aMPIInfo.mComm.allgatherv(tData[0], tCountsMul, tDisplsMul);
            aMPIInfo.mComm.allgatherv(tData[1], tCountsMul, tDisplsMul);
            IntegerArrayCache.returnArray(tCountsMul);
            IntegerArrayCache.returnArray(tDisplsMul);
            // 将在 buf 中的数据重新设回 qlm
            for (int i = 0; i < mAtomNum; ++i) {
                qlm.row(tBuf2Idx[i]).fill(rBuf.row(i));
            }
            // 归还临时数组
            ComplexMatrixCache.returnMat(rBuf);
        }
        
        return qlm;
    }
    public IComplexMatrix calQlmMean_MPI(boolean aNoGather, final MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPI.Error {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calQlmMean_MPI(aNoGather, tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    public IComplexMatrix calQlmMean_MPI(MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPI.Error {return calQlmMean_MPI(false, aComm, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}
    
    
    /**
     * 计算所有粒子的原始的 BOOP（local Bond Orientational Order Parameters, Ql），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * @author liqa
     * @param aL 计算具体 Q 值的下标，即 Q4: l = 4, Q6: l = 6
     * @param aRNearest 用来搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return Ql 组成的向量
     */
    public IVector calBOOP(int aL, double aRNearest, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        IComplexMatrix Qlm = calYlmMean(aL, aRNearest, aNnn);
        
        // 直接求和
        IVector Ql = VectorCache.getVec(mAtomNum);
        for (int i = 0; i < mAtomNum; ++i) {
            // 直接计算复向量的点乘
            double tDot = Qlm.row(i).operation().dot();
            // 使用这个公式设置 Ql
            Ql.set_(i, Fast.sqrt(4.0*PI*tDot/(double)(aL+aL+1)));
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(Qlm);
        
        // 返回最终计算结果
        return Ql;
    }
    public IVector calBOOP(int aL, double aRNearest) {return calBOOP(aL, aRNearest, -1);}
    public IVector calBOOP(int aL                  ) {return calBOOP(aL, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的计算所有粒子的原始的 BOOP */
    private IVector calBOOP_MPI(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aRNearest, int aNnn) throws MPI.Error {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        IComplexMatrix Qlm = calYlmMean_MPI(true, aMPIInfo, aL, aRNearest, aNnn);
        
        // 直接求和
        Vector Ql = VectorCache.getVec(mAtomNum);
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 直接计算复向量的点乘
            double tDot = Qlm.row(i).operation().dot();
            // 使用这个公式设置 Ql
            Ql.set_(i, Fast.sqrt(4.0*PI*tDot/(double)(aL+aL+1)));
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(Qlm);
        
        // 收集所有进程将统计到的 Ql
        if (!aNoGather) {
            // 先创建临时的同步数组
            Vector rBuf = VectorCache.getVec(mAtomNum);
            // 获取 buf2idx, counts 和 displs 来将需要的数据放入 rBuf 指定位置
            int[] tBuf2Idx = aMPIInfo.buf2idx();
            int[] tCounts = aMPIInfo.counts();
            int[] tDispls = aMPIInfo.displs();
            final int tStart = tDispls[aMPIInfo.mRank];
            final int tEnd = tStart + tCounts[aMPIInfo.mRank];
            for (int i = tStart; i < tEnd; ++i) {
                rBuf.set(i, Ql.get(tBuf2Idx[i]));
            }
            // 使用 allgatherv 收集所有 buf
            double[] tData = rBuf.internalData();
            aMPIInfo.mComm.allgatherv(tData, tCounts, tDispls);
            // 将在 buf 中的数据重新设回 qlm
            for (int i = 0; i < mAtomNum; ++i) {
                Ql.set(tBuf2Idx[i], rBuf.get(i));
            }
            // 归还临时数组
            VectorCache.returnVec(rBuf);
        }
        
        // 返回最终计算结果
        return Ql;
    }
    public IVector calBOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPI.Error {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calBOOP_MPI(aNoGather, tMPIInfo, aL, aRNearest, aNnn);}}
    public IVector calBOOP_MPI(MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPI.Error {return calBOOP_MPI(false, aComm, aL, aRNearest, aNnn);}
    public IVector calBOOP_MPI(MPI.Comm aComm, int aL, double aRNearest          ) throws MPI.Error {return calBOOP_MPI(aComm, aL, aRNearest, -1);}
    public IVector calBOOP_MPI(MPI.Comm aComm, int aL                            ) throws MPI.Error {return calBOOP_MPI(aComm, aL, mUnitLen*R_NEAREST_MUL);}
    public IVector calBOOP_MPI(                int aL, double aRNearest, int aNnn) throws MPI.Error {return calBOOP_MPI(MPI.Comm.WORLD, aL, aRNearest, aNnn);}
    public IVector calBOOP_MPI(                int aL, double aRNearest          ) throws MPI.Error {return calBOOP_MPI(MPI.Comm.WORLD, aL, aRNearest);}
    public IVector calBOOP_MPI(                int aL                            ) throws MPI.Error {return calBOOP_MPI(MPI.Comm.WORLD, aL);}
    
    
    /**
     * 计算所有粒子的三阶形式的 BOOP（local Bond Orientational Order Parameters, Wl），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * @author liqa
     * @param aL 计算具体 W 值的下标，即 W4: l = 4, W6: l = 6
     * @param aRNearest 用来搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return Wl 组成的向量
     */
    public IVector calBOOP3(int aL, double aRNearest, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        IComplexMatrix Qlm = calYlmMean(aL, aRNearest, aNnn);
        
        // 计算三阶的乘积
        IVector Wl = VectorCache.getVec(mAtomNum);
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector Qlmi = Qlm.row(i);
            // 分母为复向量的点乘
            double rDiv = Qlmi.operation().dot();
            rDiv = Fast.sqrt(rDiv);
            rDiv = Fast.pow3(rDiv);
            // 分子需要这样计算，这里只保留实数（虚数部分为 0）
            double rMul = 0.0;
            for (int tM1 = -aL; tM1 <= aL; ++tM1) for (int tM2 = -aL; tM2 <= aL; ++tM2) {
                int tM3 = -tM1-tM2;
                if (tM3<=aL && tM3>=-aL) {
                    // 计算乘积，注意使用复数乘法
                    ComplexDouble subMul = Qlmi.get_(tM1+aL);
                    subMul.multiply2this(Qlmi.get_(tM2+aL));
                    subMul.multiply2this(Qlmi.get_(tM3+aL));
                    subMul.multiply2this(Func.wigner3j_(aL, aL, aL, tM1, tM2, tM3));
                    // 累加到分子，这里只统计实数部分（虚数部分为 0）
                    rMul += subMul.mReal;
                }
            }
            // 最后求模量设置结果
            Wl.set_(i, rMul/rDiv);
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(Qlm);
        
        // 返回最终计算结果
        return Wl;
    }
    public IVector calBOOP3(int aL, double aRNearest) {return calBOOP3(aL, aRNearest, -1);}
    public IVector calBOOP3(int aL                  ) {return calBOOP3(aL, mUnitLen*R_NEAREST_MUL);}
    
    
    /**
     * 计算所有粒子的 ABOOP（Averaged local Bond Orientational Order Parameters, ql），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     * @author liqa
     * @param aL 计算具体 q 值的下标，即 q4: l = 4, q6: l = 6
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return ql 组成的向量
     */
    public IVector calABOOP(int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        IComplexMatrix qlm = calQlmMean(aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);
        
        // 直接求和
        IVector ql = VectorCache.getVec(mAtomNum);
        for (int i = 0; i < mAtomNum; ++i) {
            // 直接计算复向量的点乘
            double tDot = qlm.row(i).operation().dot();
            // 使用这个公式设置 ql
            ql.set_(i, Fast.sqrt(4.0*PI*tDot/(double)(aL+aL+1)));
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(qlm);
        
        // 返回最终计算结果
        return ql;
    }
    public IVector calABOOP(int aL, double aRNearest, int aNnn) {return calABOOP(aL, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calABOOP(int aL, double aRNearest          ) {return calABOOP(aL, aRNearest, -1);}
    public IVector calABOOP(int aL                            ) {return calABOOP(aL, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的计算所有粒子的 ABOOP */
    private IVector calABOOP_MPI(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPI.Error {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        IComplexMatrix qlm = calQlmMean_MPI(true, aMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);
        
        // 直接求和
        Vector ql = VectorCache.getVec(mAtomNum);
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 直接计算复向量的点乘
            double tDot = qlm.row(i).operation().dot();
            // 使用这个公式设置 ql
            ql.set_(i, Fast.sqrt(4.0*PI*tDot/(double)(aL+aL+1)));
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(qlm);
        
        // 收集所有进程将统计到的 ql
        if (!aNoGather) {
            // 先创建临时的同步数组
            Vector rBuf = VectorCache.getVec(mAtomNum);
            // 获取 buf2idx, counts 和 displs 来将需要的数据放入 rBuf 指定位置
            int[] tBuf2Idx = aMPIInfo.buf2idx();
            int[] tCounts = aMPIInfo.counts();
            int[] tDispls = aMPIInfo.displs();
            final int tStart = tDispls[aMPIInfo.mRank];
            final int tEnd = tStart + tCounts[aMPIInfo.mRank];
            for (int i = tStart; i < tEnd; ++i) {
                rBuf.set(i, ql.get(tBuf2Idx[i]));
            }
            // 使用 allgatherv 收集所有 buf
            double[] tData = rBuf.internalData();
            aMPIInfo.mComm.allgatherv(tData, tCounts, tDispls);
            // 将在 buf 中的数据重新设回 qlm
            for (int i = 0; i < mAtomNum; ++i) {
                ql.set(tBuf2Idx[i], rBuf.get(i));
            }
            // 归还临时数组
            VectorCache.returnVec(rBuf);
        }
        
        // 返回最终计算结果
        return ql;
    }
    public IVector calABOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPI.Error {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calABOOP_MPI(aNoGather, tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPI.Error {return calABOOP_MPI(false, aComm, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPI.Error {return calABOOP_MPI(aComm, aL, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL, double aRNearest          ) throws MPI.Error {return calABOOP_MPI(aComm, aL, aRNearest, -1);}
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL                            ) throws MPI.Error {return calABOOP_MPI(aComm, aL, mUnitLen*R_NEAREST_MUL);}
    public IVector calABOOP_MPI(                int aL, double aRNearest, int aNnn) throws MPI.Error {return calABOOP_MPI(MPI.Comm.WORLD, aL, aRNearest, aNnn);}
    public IVector calABOOP_MPI(                int aL, double aRNearest          ) throws MPI.Error {return calABOOP_MPI(MPI.Comm.WORLD, aL, aRNearest);}
    public IVector calABOOP_MPI(                int aL                            ) throws MPI.Error {return calABOOP_MPI(MPI.Comm.WORLD, aL);}
    
    
    /**
     * 计算所有粒子的三阶形式的 ABOOP（Averaged local Bond Orientational Order Parameters, wl），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     * @author liqa
     * @param aL 计算具体 w 值的下标，即 w4: l = 4, w6: l = 6
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return wl 组成的向量
     */
    public IVector calABOOP3(int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        IComplexMatrix qlm = calQlmMean(aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);
        
        // 计算 wl，这里同样不去考虑减少重复代码
        IVector wl = VectorCache.getVec(mAtomNum);
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector qlmi = qlm.row(i);
            // 分母为复向量的点乘，等于实部虚部分别点乘
            double rDiv = qlmi.operation().dot();
            rDiv = Fast.sqrt(rDiv);
            rDiv = Fast.pow3(rDiv);
            // 分子需要这样计算，这里只保留实数（虚数部分为 0）
            double rMul = 0.0;
            for (int tM1 = -aL; tM1 <= aL; ++tM1) for (int tM2 = -aL; tM2 <= aL; ++tM2) {
                int tM3 = -tM1-tM2;
                if (tM3<=aL && tM3>=-aL) {
                    // 计算乘积，注意使用复数乘法
                    ComplexDouble subMul = qlmi.get_(tM1+aL);
                    subMul.multiply2this(qlmi.get_(tM2+aL));
                    subMul.multiply2this(qlmi.get_(tM3+aL));
                    subMul.multiply2this(Func.wigner3j_(aL, aL, aL, tM1, tM2, tM3));
                    // 累加到分子，这里只统计实数部分（虚数部分为 0）
                    rMul += subMul.mReal;
                }
            }
            // 最后求模量设置结果
            wl.set_(i, rMul/rDiv);
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(qlm);
        
        // 返回最终计算结果
        return wl;
    }
    public IVector calABOOP3(int aL, double aRNearest, int aNnn) {return calABOOP3(aL, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calABOOP3(int aL, double aRNearest          ) {return calABOOP3(aL, aRNearest, -1);}
    public IVector calABOOP3(int aL                            ) {return calABOOP3(aL, mUnitLen*R_NEAREST_MUL);}
    
    
    /**
     * 通过 bond order parameter（Ql）来计算结构中每个原子的连接数目，
     * 输出结果为按照输入原子顺序排列的向量，数值为连接数目；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>,
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl</a>
     * @author liqa
     * @param aL 计算具体 Q 值的下标，即 Q4: l = 4, Q6: l = 6
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestS 用来计算 Sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 Sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数目组成的逻辑向量
     */
    public IVector calConnectCountBOOP(int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix Qlm = calYlmMean(aL, aRNearestY, aNnnY);
        
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnS<=0;
        // 统计连接数
        final IVector tConnectCount = VectorCache.getZeros(mAtomNum);
        
        // 注意需要先对 Qlm 归一化
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector Qlmi = Qlm.row(i);
            Qlmi.div2this(Qlmi.operation().norm());
        }
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntegerList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntegerList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf) : null;
        
        // 计算近邻上 qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) {
            // 统一获取行向量
            final IComplexVector Qlmi = Qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor(tNL, fI, aRNearestS, aNnnS, aHalf, idx -> {
                // 统一获取行向量
                IComplexVector Qlmj = Qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = Qlmi.operation().dot(Qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectCount.increment_(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                    if (aHalf) {
                        tConnectCount.increment_(idx);
                    }
                }
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
            });
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(Qlm);
        
        // 返回最终计算结果
        return tConnectCount;
    }
    public IVector calConnectCountBOOP(int aL, double aConnectThreshold, double aRNearest, int aNnn) {return calConnectCountBOOP(aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectCountBOOP(int aL, double aConnectThreshold, double aRNearest          ) {return calConnectCountBOOP(aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectCountBOOP(int aL, double aConnectThreshold                            ) {return calConnectCountBOOP(aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的 BOOP 连接数目 */
    private IVector calConnectCountBOOP_MPI(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPI.Error {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix Qlm = calYlmMean_MPI(false, aMPIInfo, aL, aRNearestY, aNnnY);
        
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnS<=0;
        // 统计连接数
        final Vector tConnectCount = VectorCache.getZeros(mAtomNum);
        
        // 注意需要先对 Qlm 归一化
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector Qlmi = Qlm.row(i);
            Qlmi.div2this(Qlmi.operation().norm());
        }
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntegerList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntegerList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize) : null;
        
        // 计算近邻上 Qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 统一获取行向量
            final IComplexVector Qlmi = Qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor(tNL, fI, aRNearestS, aNnnS, aHalf, aMPIInfo::inRegin, idx -> {
                // 统一获取行向量
                IComplexVector Qlmj = Qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = Qlmi.operation().dot(Qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectCount.increment_(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                    boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                    if (tHalfStat) {
                        tConnectCount.increment_(idx);
                    }
                }
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
            });
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(Qlm);
        
        // 收集所有进程将统计到的连接数
        if (!aNoGather) {
            // 先创建临时的同步数组
            Vector rBuf = VectorCache.getVec(mAtomNum);
            // 获取 buf2idx, counts 和 displs 来将需要的数据放入 rBuf 指定位置
            int[] tBuf2Idx = aMPIInfo.buf2idx();
            int[] tCounts = aMPIInfo.counts();
            int[] tDispls = aMPIInfo.displs();
            final int tStart = tDispls[aMPIInfo.mRank];
            final int tEnd = tStart + tCounts[aMPIInfo.mRank];
            for (int i = tStart; i < tEnd; ++i) {
                rBuf.set(i, tConnectCount.get(tBuf2Idx[i]));
            }
            // 使用 allgatherv 收集所有 buf
            double[] tData = rBuf.internalData();
            aMPIInfo.mComm.allgatherv(tData, tCounts, tDispls);
            // 将在 buf 中的数据重新设回 qlm
            for (int i = 0; i < mAtomNum; ++i) {
                tConnectCount.set(tBuf2Idx[i], rBuf.get(i));
            }
            // 归还临时数组
            VectorCache.returnVec(rBuf);
        }
        
        // 返回最终计算结果
        return tConnectCount;
    }
    public IVector calConnectCountBOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPI.Error {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountBOOP_MPI(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}}
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPI.Error {return calConnectCountBOOP_MPI(false, aComm, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPI.Error {return calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest          ) throws MPI.Error {return calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold                            ) throws MPI.Error {return calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    public IVector calConnectCountBOOP_MPI(                int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPI.Error {return calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    public IVector calConnectCountBOOP_MPI(                int aL, double aConnectThreshold, double aRNearest          ) throws MPI.Error {return calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    public IVector calConnectCountBOOP_MPI(                int aL, double aConnectThreshold                            ) throws MPI.Error {return calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 通过 Averaged bond order parameter（ql）来计算结构中每个原子的连接数目，
     * 输出结果为按照输入原子顺序排列的向量，数值为连接数目；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     * @author liqa
     * @param aL 计算具体 q 值的下标，即 q4: l = 4, q6: l = 6
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @param aRNearestS 用来计算 sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数目组成的逻辑向量
     */
    public IVector calConnectCountABOOP(int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix qlm = calQlmMean(aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);
        
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnS<=0;
        // 统计连接数，这里同样不去考虑减少重复代码
        final IVector tConnectCount = VectorCache.getZeros(mAtomNum);
        
        // 注意需要先对 qlm 归一化
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector qlmi = qlm.row(i);
            qlmi.div2this(qlmi.operation().norm());
        }
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntegerList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntegerList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf) : null;
        
        // 计算近邻上 qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) {
            // 统一获取行向量
            final IComplexVector qlmi = qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor(tNL, fI, aRNearestS, aNnnS, aHalf, idx -> {
                // 统一获取行向量
                IComplexVector qlmj = qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = qlmi.operation().dot(qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectCount.increment_(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                    if (aHalf) {
                        tConnectCount.increment_(idx);
                    }
                }
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
            });
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(qlm);
        
        // 返回最终计算结果
        return tConnectCount;
    }
    public IVector calConnectCountABOOP(int aL, double aConnectThreshold, double aRNearest, int aNnn) {return calConnectCountABOOP(aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectCountABOOP(int aL, double aConnectThreshold, double aRNearest          ) {return calConnectCountABOOP(aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectCountABOOP(int aL, double aConnectThreshold                            ) {return calConnectCountABOOP(aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的 BOOP 连接数目 */
    private IVector calConnectCountABOOP_MPI(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPI.Error {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix qlm = calQlmMean_MPI(false, aMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);
        
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnS<=0;
        // 统计连接数，这里同样不去考虑减少重复代码
        final Vector tConnectCount = VectorCache.getZeros(mAtomNum);
        
        // 注意需要先对 qlm 归一化
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector qlmi = qlm.row(i);
            qlmi.div2this(qlmi.operation().norm());
        }
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntegerList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntegerList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize) : null;
        
        // 计算近邻上 qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 统一获取行向量
            final IComplexVector qlmi = qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor(tNL, fI, aRNearestS, aNnnS, aHalf, aMPIInfo::inRegin, idx -> {
                // 统一获取行向量
                IComplexVector qlmj = qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = qlmi.operation().dot(qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectCount.increment_(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                    boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                    if (tHalfStat) {
                        tConnectCount.increment_(idx);
                    }
                }
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
            });
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(qlm);
        
        // 收集所有进程将统计到的连接数
        if (!aNoGather) {
            // 先创建临时的同步数组
            Vector rBuf = VectorCache.getVec(mAtomNum);
            // 获取 buf2idx, counts 和 displs 来将需要的数据放入 rBuf 指定位置
            int[] tBuf2Idx = aMPIInfo.buf2idx();
            int[] tCounts = aMPIInfo.counts();
            int[] tDispls = aMPIInfo.displs();
            final int tStart = tDispls[aMPIInfo.mRank];
            final int tEnd = tStart + tCounts[aMPIInfo.mRank];
            for (int i = tStart; i < tEnd; ++i) {
                rBuf.set(i, tConnectCount.get(tBuf2Idx[i]));
            }
            // 使用 allgatherv 收集所有 buf
            double[] tData = rBuf.internalData();
            aMPIInfo.mComm.allgatherv(tData, tCounts, tDispls);
            // 将在 buf 中的数据重新设回 qlm
            for (int i = 0; i < mAtomNum; ++i) {
                tConnectCount.set(tBuf2Idx[i], rBuf.get(i));
            }
            // 归还临时数组
            VectorCache.returnVec(rBuf);
        }
        
        // 返回最终计算结果
        return tConnectCount;
    }
    public IVector calConnectCountABOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPI.Error {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountABOOP_MPI(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}}
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPI.Error {return calConnectCountABOOP_MPI(false, aComm, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPI.Error {return calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest          ) throws MPI.Error {return calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold                            ) throws MPI.Error {return calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    public IVector calConnectCountABOOP_MPI(                int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPI.Error {return calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    public IVector calConnectCountABOOP_MPI(                int aL, double aConnectThreshold, double aRNearest          ) throws MPI.Error {return calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    public IVector calConnectCountABOOP_MPI(                int aL, double aConnectThreshold                            ) throws MPI.Error {return calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 具体通过 Q6 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.5
     * @param aSolidThreshold 用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值，默认为 7
     * @param aRNearest 用来搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量
     */
    public ILogicalVector checkSolidQ6(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {IVector tConnectCount = calConnectCountBOOP(6, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectCount.greaterOrEqual(aSolidThreshold); VectorCache.returnVec(tConnectCount); return tIsSolid;}
    public ILogicalVector checkSolidQ6(double aConnectThreshold, int aSolidThreshold, double aRNearest          ) {return checkSolidQ6(aConnectThreshold, aSolidThreshold, aRNearest, -1);}
    public ILogicalVector checkSolidQ6(double aConnectThreshold, int aSolidThreshold                            ) {return checkSolidQ6(aConnectThreshold, aSolidThreshold, mUnitLen*R_NEAREST_MUL);}
    public ILogicalVector checkSolidQ6(                                                                         ) {return checkSolidQ6(0.5, 7);}
    
    /**
     * 具体通过 Q4 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl</a>
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.35
     * @param aSolidThreshold 用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值，默认为 6
     * @param aRNearest 用来搜索的最近邻半径。默认为 R_NEAREST_MUL 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量
     */
    public ILogicalVector checkSolidQ4(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {IVector tConnectCount = calConnectCountBOOP(4, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectCount.greaterOrEqual(aSolidThreshold); VectorCache.returnVec(tConnectCount); return tIsSolid;}
    public ILogicalVector checkSolidQ4(double aConnectThreshold, int aSolidThreshold, double aRNearest          ) {return checkSolidQ4(aConnectThreshold, aSolidThreshold, aRNearest, -1);}
    public ILogicalVector checkSolidQ4(double aConnectThreshold, int aSolidThreshold                            ) {return checkSolidQ4(aConnectThreshold, aSolidThreshold, mUnitLen*R_NEAREST_MUL);}
    public ILogicalVector checkSolidQ4(                                                                         ) {return checkSolidQ4(0.35, 6);}
    
    
    public interface IVoronoiCalculator extends List<VoronoiBuilder.IVertex>, RandomAccess {
        IVoronoiCalculator setNoWarning(boolean aNoWarning);
        IVoronoiCalculator setNoWarning();
        IVoronoiCalculator setAreaThreshold(double aAreaThreshold);
        IVoronoiCalculator setLengthThreshold(double aLengthThreshold);
        IVoronoiCalculator setAreaThresholdAbs(double aAreaThresholdAbs);
        IVoronoiCalculator setLengthThresholdAbs(double aLengthThresholdAbs);
        IVoronoiCalculator setIndexLength(int aIndexLength);
    }
    private static abstract class AbstractVoronoiCalculator extends AbstractRandomAccessList<VoronoiBuilder.IVertex> implements IVoronoiCalculator {
        final VoronoiBuilder mBuilder;
        AbstractVoronoiCalculator(VoronoiBuilder aBuilder) {mBuilder = aBuilder;}
        @Override public final AbstractVoronoiCalculator setNoWarning(boolean aNoWarning) {mBuilder.setNoWarning(aNoWarning); return this;}
        @Override public final AbstractVoronoiCalculator setNoWarning() {mBuilder.setNoWarning(); return this;}
        @Override public final AbstractVoronoiCalculator setAreaThreshold(double aAreaThreshold) {mBuilder.setAreaThreshold(aAreaThreshold); return this;}
        @Override public final AbstractVoronoiCalculator setLengthThreshold(double aLengthThreshold) {mBuilder.setLengthThreshold(aLengthThreshold); return this;}
        @Override public final AbstractVoronoiCalculator setAreaThresholdAbs(double aAreaThresholdAbs) {mBuilder.setAreaThresholdAbs(aAreaThresholdAbs); return this;}
        @Override public final AbstractVoronoiCalculator setLengthThresholdAbs(double aLengthThresholdAbs) {mBuilder.setLengthThresholdAbs(aLengthThresholdAbs); return this;}
        @Override public final AbstractVoronoiCalculator setIndexLength(int aIndexLength) {mBuilder.setIndexLength(aIndexLength); return this;}
    }
    
    
    /**
     * 计算 Voronoi 图并获取各种参数，
     * 由于内部实现是串行的，因此此方法不受线程数影响
     * <p>
     * 简单使用额外的镜像原子的方式处理周期边界条件，
     * 因此可能会出现不准确的情况，此时需要增加 aRCutOff
     * <p>
     * References:
     * <a href="https://ieeexplore.ieee.org/document/4276112">
     * Computing the 3D Voronoi Diagram Robustly: An Easy Explanation </a>
     * and
     * <a href="https://github.com/Hellblazer/Voronoi-3D">
     * Hellblazer/Voronoi-3D </a>
     * @author liqa
     * @param aRCutOff 外围周期边界增加的镜像粒子的半径，默认为 3 倍单位长度
     * @param aNoWarning 是否关闭错误警告，默认为 false
     * @param aIndexLength voronoi 参数的存储长度，默认为 9
     * @param aAreaThreshold 过小面积的阈值（相对值），默认为 0.0（不处理）
     * @param aLengthThreshold 过小长度的阈值（相对值），默认为 0.0（不处理）
     * @return Voronoi 分析的参数
     */
    public IVoronoiCalculator calVoronoi(double aRCutOff, boolean aNoWarning, int aIndexLength, double aAreaThreshold, double aLengthThreshold) {
        final VoronoiBuilder rBuilder = new VoronoiBuilder().setNoWarning(aNoWarning).setIndexLength(aIndexLength).setAreaThreshold(aAreaThreshold).setLengthThreshold(aLengthThreshold);
        // 先增加内部原本的粒子，根据 cell 的顺序添加可以加速 voronoi 的构造
        final int[] idx2voronoi = new int[mAtomNum];
        mNL.forEachCell(aRCutOff, idx -> {
            idx2voronoi[idx] = rBuilder.sizeVertex();
            // 原则上 VoronoiBuilder.insert 内部也会进行一次拷贝避免坐标被意外修改，但是旧版本没有，这样写可以兼顾效率和旧版兼容
            rBuilder.insert(mAtomDataXYZ.get(idx, 0), mAtomDataXYZ.get(idx, 1), mAtomDataXYZ.get(idx, 2));
        });
        // 然后增加一些镜像粒子保证 PBC 下的准确性
        mNL.forEachMirrorCell(aRCutOff, (x, y, z, idx) -> rBuilder.insert(x, y, z));
        // 注意需要进行一次重新排序保证顺序和原子的顺序相同
        return new AbstractVoronoiCalculator(rBuilder) {
            @Override public int size() {return mAtomNum;}
            @Override public VoronoiBuilder.IVertex get(int aIdx) {return mBuilder.getVertex(idx2voronoi[aIdx]);}
        };
    }
    public IVoronoiCalculator calVoronoi(double aRCutOff, boolean aNoWarning, int aIndexLength, double aAreaThreshold) {return calVoronoi(aRCutOff, aNoWarning, aIndexLength, aAreaThreshold, 0.0);}
    public IVoronoiCalculator calVoronoi(double aRCutOff, boolean aNoWarning, int aIndexLength) {return calVoronoi(aRCutOff, aNoWarning, aIndexLength, 0.0);}
    public IVoronoiCalculator calVoronoi(double aRCutOff, boolean aNoWarning) {return calVoronoi(aRCutOff, aNoWarning, 9);}
    public IVoronoiCalculator calVoronoi(double aRCutOff) {return calVoronoi(aRCutOff, false);}
    public IVoronoiCalculator calVoronoi() {return calVoronoi(mUnitLen*3.0);}
    
    
    /**
     * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
     * 或称为此原子的指纹（FingerPrints）， 主要用于作为机器学习的输入向量
     * <p>
     * References:
     * <a href="https://arxiv.org/abs/2211.03350v3">
     * Computing the 3D Voronoi Diagram Robustly: An Easy Explanation </a>
     * @author Su Rui, liqa
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCutOff 截断半径
     * @return 原子指纹矩阵组成的数组，n 为行，l 为列，因此 asVecRow 即为原本定义的基
     */
    @ApiStatus.Experimental
    public List<? extends IMatrix> calFPSuRui(final int aNMax, final int aLMax, final double aRCutOff) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aNMax < 0) throw new IllegalArgumentException("Input n_max MUST be Non-Negative, input: "+aNMax);
        if (aLMax < 0) throw new IllegalArgumentException("Input l_max MUST be Non-Negative, input: "+aLMax);
        
        final List<? extends IMatrix> rFingerPrints = MatrixCache.getMatRow(aNMax+1, aLMax+1, mAtomNum);
        
        // 获取需要缓存的近邻列表
        final IntegerList @Nullable[] tNLToBuffer = getNLWhichNeedBuffer_(aRCutOff, -1, false);
        
        // 理论上只需要遍历一半从而加速这个过程，但由于实现较麻烦且占用过多内存，这里不考虑
        pool().parfor(mAtomNum, (i, threadID) -> {
            // 需要存储所有的 l，n，m 的值来统一进行近邻求和
            final IComplexVector[][] cnlm = new IComplexVector[aNMax+1][aLMax+1];
            for (int tN = 0; tN <= aNMax; ++tN) for (int tL = 0; tL <= aLMax; ++tL) {
                cnlm[tN][tL] = ComplexVectorCache.getZeros(tL+tL+1);
            }
            final XYZ cXYZ = new XYZ(mAtomDataXYZ.row(i));
            // 遍历近邻计算 Ylm, Rn, fc
            mNL.forEachNeighbor(i, aRCutOff, false, (x, y, z, idx, dis2) -> {
                double dis = Fast.sqrt(dis2);
                // 计算角度
                double dx = x - cXYZ.mX;
                double dy = y - cXYZ.mY;
                double dz = z - cXYZ.mZ;
                double theta = Fast.acos(dz / dis);
                double disXY = Fast.hypot(dx, dy);
                double phi = (dy > 0) ? Fast.acos(dx / disXY) : (2.0*PI - Fast.acos(dx / disXY));
                
                // 计算截断函数 fc
                double fc = dis>=aRCutOff ? 0.0 : Fast.powFast(1.0 - Fast.pow2(dis/aRCutOff), 4);
                // 统一遍历一次计算 Rn
                final double tX = 1.0 - 2.0*dis/aRCutOff;
                IVector Rn = Vectors.from(aNMax+1, n -> Func.chebyshev_(n, tX));
                
                // 遍历求 n，l 的情况，这里这样遍历来减少球谐函数的计算频率，因此这个基组理论上有很多冗余信息
                for (int tL = 0; tL <= aLMax; ++tL) {
                    // 计算 Y 并累加，考虑对称性只需要算 m=0~l 的部分
                    for (int tM = 0; tM <= tL; ++tM) {
                        int tColP =  tM+tL;
                        int tColN = -tM+tL;
                        ComplexDouble tY = Func.sphericalHarmonics_(tL, tM, theta, phi);
                        for (int tN = 0; tN <= aNMax; ++tN) {
                            // 得到 cnlm 向量
                            IComplexVector cijm = cnlm[tN][tL];
                            // 乘上 fc，Rn 系数
                            ComplexDouble cijk = tY.multiply(fc*Rn.get_(tN));
                            cijm.add_(tColP, cijk);
                            // m < 0 的部分直接利用对称性求
                            if (tM != 0) {
                                cijk.conj2this();
                                if ((tM&1)==1) cijk.negative2this();
                                cijm.add_(tColN, cijk);
                            }
                        }
                    }
                }
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[i].add(idx);}
            });
            // 做标量积消去 m 项，得到此原子的 FP
            IMatrix tFP = rFingerPrints.get(i);
            for (int tN = 0; tN <= aNMax; ++tN) for (int tL = 0; tL <= aLMax; ++tL) {
                tFP.set_(tN, tL, (4.0*PI/(double)(tL+tL+1)) * cnlm[tN][tL].operation().dot());
            }
            // 归还临时变量
            for (IComplexVector[] cilm : cnlm) for (IComplexVector cijm : cilm) {
                ComplexVectorCache.returnVec(cijm);
            }
        });
        
        return rFingerPrints;
    }
}
