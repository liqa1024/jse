package jse.atom;

import jse.cache.*;
import jse.code.CS;
import jse.code.collection.AbstractCollections;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.code.functional.IIndexFilter;
import jse.code.functional.IUnaryFullOperator;
import jse.math.ComplexDouble;
import jse.math.MathEX;
import jse.math.function.FixBoundFunc1;
import jse.math.function.Func1;
import jse.math.function.IFunc1;
import jse.math.function.IZeroBoundFunc1;
import jse.math.matrix.IComplexMatrix;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowComplexMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.parallel.*;
import org.jetbrains.annotations.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.IntConsumer;

import static jse.code.CS.R_NEAREST_MUL;
import static jse.math.MathEX.*;

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
    private final IBox mBox;
    
    private final int mAtomNum;
    private IIntVector mAtomNumType; // 统计某个种类的原子数目
    private IIntVector mTypeVec; // 统计所有的原子种类
    private final int mAtomTypeNum; // 统计所有的原子种类数目
    private final double mVolume; // 模拟盒体积
    private final double mRho; // 粒子数密度
    private final double mUnitLen; // 平均单个原子的距离
    
    private final NeighborListGetter mNL;
    private final long mInitThreadID;
    
    /** IThreadPoolContainer stuffs */
    private volatile boolean mDead = false;
    @Override public void shutdown() {
        mDead = true; super.shutdown();
        mNL.shutdown(); // 内部保证执行后内部的 mAtomDataXYZ 已经置为 null
        // 此时 MPC 关闭，归还 mAtomDataXYZ，这种写法保证永远能获取到 mAtomDataXYZ 时都是合法的
        // 只有相同线程关闭才会归还
        long tThreadID = Thread.currentThread().getId();
        if (tThreadID == mInitThreadID) {
            IMatrix oAtomDataXYZ = mAtomDataXYZ;
            IIntVector oAtomNumType = mAtomNumType;
            IIntVector oTypeVec = mTypeVec;
            mAtomDataXYZ = null;
            mAtomNumType = null;
            mTypeVec = null;
            MatrixCache.returnMat(oAtomDataXYZ);
            IntVectorCache.returnVec(oAtomNumType);
            IntVectorCache.returnVec(oTypeVec);
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
    /** ParforThreadPool close 时不需要 awaitTermination */
    @ApiStatus.Internal @Override public void close() {shutdown();}
    
    /** @deprecated use {@link #of} */ @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
    MonatomicParameterCalculator(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {
        super(new ParforThreadPool(aThreadNum));
        
        // 获取模拟盒数据
        mBox = aAtomData.box().copy(); // 最大限度防止外部修改
        
        // 获取合适的 XYZ 数据和原子种类信息
        mAtomNum = aAtomData.atomNumber();
        mAtomTypeNum = aAtomData.atomTypeNumber();
        mAtomDataXYZ = MatrixCache.getMatRow(mAtomNum, 3);
        mTypeVec = IntVectorCache.getVec(mAtomNum);
        mAtomNumType = IntVectorCache.getZeros(mAtomTypeNum);
        XYZ tBuf = new XYZ();
        for (int i = 0; i < mAtomNum; ++i) {
            IAtom tAtom = aAtomData.atom(i);
            setValidXYZ_(mAtomDataXYZ, tAtom, i, tBuf);
            int tType = tAtom.type();
            mTypeVec.set(i, tType);
            mAtomNumType.increment(tType-1);
        }
        
        // 计算单位长度供内部使用
        mVolume = mBox.volume();
        mRho = mAtomNum / mVolume;
        mUnitLen = Fast.cbrt(1.0/ mRho);
        
        mNL = new NeighborListGetter(mAtomDataXYZ, mAtomNum, mBox);
        mInitThreadID = Thread.currentThread().getId();
    }
    /** @deprecated use {@link #of} */ @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
    MonatomicParameterCalculator(IAtomData aAtomData) {this(aAtomData, 1);}
    
    
    /**
     * 根据输入数据直接创建 MPC
     * @param aAtomData 原子数据
     * @param aThreadNum MPC 进行计算会使用的线程数
     */
    public static MonatomicParameterCalculator of(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return new MonatomicParameterCalculator(aAtomData, aThreadNum);}
    public static MonatomicParameterCalculator of(IAtomData aAtomData) {return new MonatomicParameterCalculator(aAtomData);}
    
    /** 自动关闭接口 */
    public static <T> T withOf(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, IUnaryFullOperator<? extends T, ? super MonatomicParameterCalculator> aDoLater) {try (MonatomicParameterCalculator tMPC = new MonatomicParameterCalculator(aAtomData, aThreadNum)) {return aDoLater.apply(tMPC);}}
    public static <T> T withOf(IAtomData aAtomData, IUnaryFullOperator<? extends T, ? super MonatomicParameterCalculator> aDoLater) {try (MonatomicParameterCalculator tMPC = new MonatomicParameterCalculator(aAtomData)) {return aDoLater.apply(tMPC);}}
    
    
    /** 内部使用方法，用来将 aAtomDataXYZ 转换成内部存储的格式，并且处理精度问题造成的超出边界问题 */
    @ApiStatus.Internal static void setValidXYZ_(IBox aBox, IMatrix rXYZMat, IXYZ aXYZ, int aRow, XYZ rBuf) {
        if (aBox.isPrism()) {
            // 斜方情况需要转为 Direct 再 wrap，
            // 完事后再转回 Cartesian
            rBuf.setXYZ(aXYZ);
            aBox.toDirect(rBuf);
            if      (rBuf.mX <  0.0) {do {++rBuf.mX;} while (rBuf.mX <  0.0);}
            else if (rBuf.mX >= 1.0) {do {--rBuf.mX;} while (rBuf.mX >= 1.0);}
            if      (rBuf.mY <  0.0) {do {++rBuf.mY;} while (rBuf.mY <  0.0);}
            else if (rBuf.mY >= 1.0) {do {--rBuf.mY;} while (rBuf.mY >= 1.0);}
            if      (rBuf.mZ <  0.0) {do {++rBuf.mZ;} while (rBuf.mZ <  0.0);}
            else if (rBuf.mZ >= 1.0) {do {--rBuf.mZ;} while (rBuf.mZ >= 1.0);}
            aBox.toCartesian(rBuf);
            rXYZMat.set(aRow, 0, rBuf.mX);
            rXYZMat.set(aRow, 1, rBuf.mY);
            rXYZMat.set(aRow, 2, rBuf.mZ);
        } else {
            double tX = aXYZ.x(), tY = aXYZ.y(), tZ = aXYZ.z();
            if      (tX <  0.0     ) {do {tX += aBox.x();} while (tX <  0.0     );}
            else if (tX >= aBox.x()) {do {tX -= aBox.x();} while (tX >= aBox.x());}
            if      (tY <  0.0     ) {do {tY += aBox.y();} while (tY <  0.0     );}
            else if (tY >= aBox.y()) {do {tY -= aBox.y();} while (tY >= aBox.y());}
            if      (tZ <  0.0     ) {do {tZ += aBox.z();} while (tZ <  0.0     );}
            else if (tZ >= aBox.z()) {do {tZ -= aBox.z();} while (tZ >= aBox.z());}
            rXYZMat.set(aRow, 0, tX);
            rXYZMat.set(aRow, 1, tY);
            rXYZMat.set(aRow, 2, tZ);
        }
    }
    private void setValidXYZ_(IMatrix rXYZMat, IXYZ aXYZ, int aRow, XYZ rBuf) {
        setValidXYZ_(mBox, rXYZMat, aXYZ, aRow, rBuf);
    }
    private IMatrix getValidAtomDataXYZ_(Collection<? extends IXYZ> aAtomDataXYZ) {
        int tSize = aAtomDataXYZ.size();
        // 尝试先获取缓存的临时变量
        IMatrix rXYZMat = MatrixCache.getMatRow(tSize, 3);
        XYZ tBuf = new XYZ();
        int row = 0;
        for (IXYZ tXYZ : aAtomDataXYZ) {
            setValidXYZ_(rXYZMat, tXYZ, row, tBuf);
            ++row;
        }
        return rXYZMat;
    }
    
    
    /// 参数设置
    /**
     * 修改线程数，如果相同则不会进行任何操作
     * @param aThreadNum 线程数目
     * @return 返回自身用于链式调用
     */
    public MonatomicParameterCalculator setThreadNumber(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNum)  {if (aThreadNum != threadNumber()) setPool(new ParforThreadPool(aThreadNum)); return this;}
    
    
    /// 获取信息
    public int atomNumber() {return mAtomNum;}
    public double unitLen() {return mUnitLen;}
    public double volume() {return mVolume;}
    public double rho() {return mRho;}
    public double rho(int aType) {return mAtomNumType.get(aType-1) / mVolume;}
    public double birho(int aTypeA, int aTypeB) {return Fast.sqrt(rho(aTypeA)*rho(aTypeB));}
    public double birho(MonatomicParameterCalculator aMPC) {return Fast.sqrt(mRho*aMPC.mRho);}
    /** @deprecated use {@link #atomNumber} */
    @Deprecated public final int atomNum() {return atomNumber();}
    /** @deprecated use {@link #rho} */
    @Deprecated public final double rou() {return rho();}
    /** @deprecated use {@link #rho} */
    @Deprecated public final double rou(int aType) {return rho(aType);}
    /** @deprecated use {@link #birho} */
    public final double birou(int aTypeA, int aTypeB) {return birho(aTypeA, aTypeB);}
    /** @deprecated use {@link #birho} */
    public final double birou(MonatomicParameterCalculator aMPC) {return birho(aMPC);}
    /** 补充运算时使用 */
    @ApiStatus.Internal public NeighborListGetter nl_() {return mNL;}
    @ApiStatus.Internal public IMatrix atomDataXYZ_() {return mAtomDataXYZ;}
    
    
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
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final IFunc1[] dnPar = new IFunc1[threadNumber()];
        for (int i = 0; i < dnPar.length; ++i) dnPar[i] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(mAtomNum, (i, threadID) -> {
            final IFunc1 dn = dnPar[threadID];
            mNL.forEachNeighbor(i, aRMax - dr*0.5, true, (x, y, z, idx, dis2) -> {
                dn.updateNear(Fast.sqrt(dis2), g->g+1);
            });
        });
        
        // 获取结果
        IFunc1 gr = dnPar[0];
        for (int i = 1; i < dnPar.length; ++i) gr.plus2this(dnPar[i]);
        final double rho = dr * mAtomNum*0.5 * mRho; // mAtomNum*0.5 为对所有原子求和需要进行的平均
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*rho)));
        
        // 修复截断数据
        gr.set(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF(int aN) {return calRDF(aN, mUnitLen*6);}
    public IFunc1 calRDF(      ) {return calRDF(160);}
    
    
    private IFunc1 calRDF_AB_(final IMatrix aAtomDataXYZ, int aAtomNum, int aN, final double aRMax) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final IFunc1[] dnPar = new IFunc1[threadNumber()];
        for (int i = 0; i < dnPar.length; ++i) dnPar[i] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(aAtomNum, (i, threadID) -> {
            final IFunc1 dn = dnPar[threadID];
            final XYZ cXYZ = new XYZ(aAtomDataXYZ.row(i));
            mNL.forEachNeighbor(cXYZ, aRMax - dr*0.5, (x, y, z, idx, dis2) -> {
                // 当类型相同时可能存在相同原子
                if (!cXYZ.numericEqual(x, y, z)) {
                    dn.updateNear(Fast.sqrt(dis2), g->g+1);
                }
            });
        });
        
        // 获取结果
        IFunc1 gr = dnPar[0];
        for (int i = 1; i < dnPar.length; ++i) gr.plus2this(dnPar[i]);
        final double rho = dr * aAtomNum * mRho; // aAtomDataXYZ.size() 为对所有原子求和需要进行的平均
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*rho)));
        
        // 修复截断数据
        gr.set(0, 0.0);
        // 输出
        return gr;
    }
    /**
     * 计算自身与输入的 aAtomDataXYZ 之间的 RDF，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aAtomDataXYZ 另一个元素的 xyz 坐标组成的列表，或者输入 aMPC 即计算两个 MPC 之间的 RDF
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @return gr 函数
     */
    public IFunc1 calRDF_AB(Collection<? extends IXYZ> aAtomDataXYZ, int aN, final double aRMax) {
        IMatrix tAtomDataXYZ = getValidAtomDataXYZ_(aAtomDataXYZ);
        IFunc1 tOut = calRDF_AB_(tAtomDataXYZ, aAtomDataXYZ.size(), aN, aRMax);
        MatrixCache.returnMat(tAtomDataXYZ);
        return tOut;
    }
    public IFunc1 calRDF_AB(Collection<? extends IXYZ> aAtomDataXYZ, int aN) {return calRDF_AB(aAtomDataXYZ, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB(Collection<? extends IXYZ> aAtomDataXYZ        ) {return calRDF_AB(aAtomDataXYZ, 160);}
    public IFunc1 calRDF_AB(MonatomicParameterCalculator aMPC, int aN, final double aRMax) {return calRDF_AB_(aMPC.mAtomDataXYZ, aMPC.mAtomNum, aN, aRMax);} // aMPC 的 mAtomDataXYZ 都已经经过平移并且合理化
    public IFunc1 calRDF_AB(MonatomicParameterCalculator aMPC, int aN                    ) {return calRDF_AB(aMPC, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB(MonatomicParameterCalculator aMPC                            ) {return calRDF_AB(aMPC, 160);}
    
    
    /**
     * 计算两种种类之间的 RDF，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aTypeA 种类 A
     * @param aTypeB 种类 B
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @return gr 函数
     */
    public IFunc1 calRDF_AB(final int aTypeA, final int aTypeB, int aN, final double aRMax) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final IFunc1[] dnPar = new IFunc1[threadNumber()];
        for (int i = 0; i < dnPar.length; ++i) dnPar[i] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(mAtomNum, (i, threadID) -> {
            int tTypeI = mTypeVec.get(i);
            if (tTypeI==aTypeA || tTypeI==aTypeB) {
                final int tTypeJ = tTypeI==aTypeA ? aTypeB : aTypeA;
                final IFunc1 dn = dnPar[threadID];
                mNL.forEachNeighbor(i, aRMax - dr*0.5, true, (x, y, z, idx, dis2) -> {
                    if (mTypeVec.get(idx) == tTypeJ) {
                        dn.updateNear(Fast.sqrt(dis2), g->g+1);
                    }
                });
            }
        });
        
        // 获取结果
        IFunc1 gr = dnPar[0];
        for (int i = 1; i < dnPar.length; ++i) gr.plus2this(dnPar[i]);
        double rho = dr * mAtomNumType.get(aTypeA-1) * mAtomNumType.get(aTypeB-1) / mVolume;
        if (aTypeA == aTypeB) rho *= 0.5;
        final double fRho = rho;
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*fRho)));
        
        // 修复截断数据
        gr.set(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF_AB(int aTypeA, int aTypeB, int aN) {return calRDF_AB(aTypeA, aTypeB, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB(int aTypeA, int aTypeB        ) {return calRDF_AB(aTypeA, aTypeB, 160);}
    
    
    /**
     * 计算所有种类之间的 RDF (radial distribution function，即 g(r))，
     * 只计算一个固定结构的值，因此不包含温度信息。
     * <p>
     * 所有的 {@code g_AB(r)} 直接排列成一个列表，
     * 先遍历 {@code typeB} 后遍历 {@code typeA}，且保持
     * {@code typeB <= typeA}，也就是说，如果需要访问给定 {@code (typeA, typeB)}
     * 的 {@code g_AB(r)}，需要使用：
     * <pre> {@code
     * def grAll = mpc.calAllRDF(...)
     * int idx = (typeA*(typeA-1)).intdiv(2) + typeB
     * def grAB = grAll[idx]
     * } </pre>
     * 来获取（注意到 {@code idx == 0} 对应所有种类都考虑的
     * g(r)，并且这里认为所有的种类都是从 1 开始的）。
     * <p>
     * 当只有一个种类时不进行单独种类的计算，即此时返回的向量长度一定为 1
     * @author liqa
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @return 所有 gr 函数组成的列表
     */
    public List<? extends IFunc1> calAllRDF(int aN, final double aRMax) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 当只有一个种类时不进行单独种类的计算
        if (mAtomTypeNum == 1) return Collections.singletonList(calRDF(aN, aRMax));
        
        final double dr = aRMax/aN;
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final List<IFunc1[]> dnAllPar = NewCollections.from(threadNumber(), i -> {
            IFunc1[] dnAll = new IFunc1[(mAtomTypeNum*(mAtomTypeNum+1))/2 + 1];
            for (int j = 0; j < dnAll.length; ++j) {
                dnAll[j] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
            }
            return dnAll;
        });
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(mAtomNum, (i, threadID) -> {
            final int tTypeA = mTypeVec.get(i);
            final IFunc1[] dnAll = dnAllPar.get(threadID);
            mNL.forEachNeighbor(i, aRMax - dr*0.5, true, (x, y, z, idx, dis2) -> {
                double dis = Fast.sqrt(dis2);
                dnAll[0].updateNear(dis, g->g+1);
                int tTypeB = mTypeVec.get(idx);
                dnAll[(tTypeA*(tTypeA-1))/2 + tTypeB].updateNear(dis, g->g+1);
            });
        });
        
        // 获取结果
        Iterator<IFunc1[]> it = dnAllPar.iterator();
        IFunc1[] grAll = it.next();
        it.forEachRemaining(dnAll -> {
            for (int i = 0; i < grAll.length; ++i) grAll[i].plus2this(dnAll[i]);
        });
        double rho = dr * mAtomNum*0.5 * mRho; // mAtomNum*0.5 为对所有原子求和需要进行的平均
        final double fRho = rho;
        grAll[0].operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*fRho)));
        int idx = 1;
        for (int typeAmm = 0; typeAmm < mAtomTypeNum; ++typeAmm) for (int typeBmm = 0; typeBmm <= typeAmm; ++typeBmm) {
            rho = dr * mAtomNumType.get(typeAmm) * mAtomNumType.get(typeBmm) / mVolume;
            if (typeAmm == typeBmm) rho *= 0.5;
            final double fRhoAB = rho;
            grAll[idx].operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*fRhoAB)));
            ++idx;
        }
        
        // 修复截断数据
        for (IFunc1 gr : grAll) gr.set(0, 0.0);
        // 输出
        return AbstractCollections.from(grAll);
    }
    public List<? extends IFunc1> calAllRDF(int aN) {return calAllRDF(aN, mUnitLen*6);}
    public List<? extends IFunc1> calAllRDF(      ) {return calAllRDF(160);}
    
    
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
        final IFunc1[] dnPar = new IFunc1[threadNumber()];
        for (int i = 0; i < dnPar.length; ++i) dnPar[i] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
        
        // 并行需要线程数个独立的 DeltaG
        final IZeroBoundFunc1[] tDeltaGPar = new IZeroBoundFunc1[threadNumber()];
        for (int i = 0; i < tDeltaGPar.length; ++i) tDeltaGPar[i] = Func1.deltaG(dr*aSigmaMul, 0.0, aSigmaMul);
        // 需要增加一个额外的偏移保证外部边界的统计正确性
        final double tRShift = -tDeltaGPar[0].zeroBoundL();
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(mAtomNum, (i, threadID) -> {
            final IFunc1 dn = dnPar[threadID];
            final IZeroBoundFunc1 tDeltaG = tDeltaGPar[threadID];
            mNL.forEachNeighbor(i, aRMax+tRShift, true, (x, y, z, idx, dis2) -> {
                tDeltaG.setX0(Fast.sqrt(dis2));
                dn.plus2this(tDeltaG);
            });
        });
        
        // 获取结果
        IFunc1 gr = dnPar[0];
        for (int i = 1; i < dnPar.length; ++i) gr.plus2this(dnPar[i]);
        final double rho = mAtomNum*0.5 * mRho; // mAtomNum*0.5 为对所有原子求和需要进行的平均
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*rho)));
        
        // 修复截断数据
        gr.set(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF_G(int aN, final double aRMax) {return calRDF_G(aN, aRMax, 4);}
    public IFunc1 calRDF_G(int aN                    ) {return calRDF_G(aN, mUnitLen*6);}
    public IFunc1 calRDF_G(                          ) {return calRDF_G(1000);}
    
    
    private IFunc1 calRDF_AB_G_(final IMatrix aAtomDataXYZ, int aAtomNum, int aN, final double aRMax, int aSigmaMul) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final IFunc1[] dnPar = new IFunc1[threadNumber()];
        for (int i = 0; i < dnPar.length; ++i) dnPar[i] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
        
        // 并行需要线程数个独立的 DeltaG
        final IZeroBoundFunc1[] tDeltaGPar = new IZeroBoundFunc1[threadNumber()];
        for (int i = 0; i < tDeltaGPar.length; ++i) tDeltaGPar[i] = Func1.deltaG(dr*aSigmaMul, 0.0, aSigmaMul);
        // 需要增加一个额外的偏移保证外部边界的统计正确性
        final double tRShift = -tDeltaGPar[0].zeroBoundL();
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(aAtomNum, (i, threadID) -> {
            final IFunc1 dn = dnPar[threadID];
            final IZeroBoundFunc1 tDeltaG = tDeltaGPar[threadID];
            final XYZ cXYZ = new XYZ(aAtomDataXYZ.row(i));
            mNL.forEachNeighbor(cXYZ, aRMax+tRShift, (x, y, z, idx, dis2) -> {
                // 当类型相同时可能存在相同原子
                if (!cXYZ.numericEqual(x, y, z)) {
                    tDeltaG.setX0(Fast.sqrt(dis2));
                    dn.plus2this(tDeltaG);
                }
            });
        });
        
        // 获取结果
        IFunc1 gr = dnPar[0];
        for (int i = 1; i < dnPar.length; ++i) gr.plus2this(dnPar[i]);
        final double rho = aAtomNum * mRho; // aAtomDataXYZ.size() 为对所有原子求和需要进行的平均
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*rho)));
        
        // 修复截断数据
        gr.set(0, 0.0);
        // 输出
        return gr;
    }
    /**
     * 使用带有一定展宽的高斯分布代替直接计数来计算自身与输入的 aAtomDataXYZ 之间的 RDF
     * @author liqa
     * @param aAtomDataXYZ 另一个元素的 xyz 坐标组成的列表，或者输入 aMPC 即计算两个 MPC 之间的 RDF
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @param aSigmaMul 高斯分布的一个标准差宽度对应的分划份数，默认为 4
     * @return gr 函数
     */
    public IFunc1 calRDF_AB_G(Collection<? extends IXYZ> aAtomDataXYZ, int aN, double aRMax, int aSigmaMul) {
        IMatrix tAtomDataXYZ = getValidAtomDataXYZ_(aAtomDataXYZ);
        IFunc1 tOut = calRDF_AB_G_(tAtomDataXYZ, aAtomDataXYZ.size(), aN, aRMax, aSigmaMul);
        MatrixCache.returnMat(tAtomDataXYZ);
        return tOut;
    }
    public IFunc1 calRDF_AB_G(Collection<? extends IXYZ> aAtomDataXYZ, int aN, double aRMax) {return calRDF_AB_G(aAtomDataXYZ, aN, aRMax, 4);}
    public IFunc1 calRDF_AB_G(Collection<? extends IXYZ> aAtomDataXYZ, int aN              ) {return calRDF_AB_G(aAtomDataXYZ, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB_G(Collection<? extends IXYZ> aAtomDataXYZ                      ) {return calRDF_AB_G(aAtomDataXYZ, 1000);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC, int aN, double aRMax, int aSigmaMul) {return calRDF_AB_G_(aMPC.mAtomDataXYZ, aMPC.mAtomNum, aN, aRMax, aSigmaMul);} // aMPC 的 mAtomDataXYZ 都已经经过了平移
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC, int aN, double aRMax               ) {return calRDF_AB_G(aMPC, aN, aRMax, 4);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC, int aN                             ) {return calRDF_AB_G(aMPC, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB_G(MonatomicParameterCalculator aMPC                                     ) {return calRDF_AB_G(aMPC, 1000);}
    
    
    /**
     * 使用带有一定展宽的高斯分布代替直接计数来计算两种种类之间的 RDF
     * @author liqa
     * @param aTypeA 种类 A
     * @param aTypeB 种类 B
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @param aSigmaMul 高斯分布的一个标准差宽度对应的分划份数，默认为 4
     * @return gr 函数
     */
    public IFunc1 calRDF_AB_G(final int aTypeA, final int aTypeB, int aN, final double aRMax, int aSigmaMul) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dr = aRMax/aN;
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final IFunc1[] dnPar = new IFunc1[threadNumber()];
        for (int i = 0; i < dnPar.length; ++i) dnPar[i] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
        
        // 并行需要线程数个独立的 DeltaG
        final IZeroBoundFunc1[] tDeltaGPar = new IZeroBoundFunc1[threadNumber()];
        for (int i = 0; i < tDeltaGPar.length; ++i) tDeltaGPar[i] = Func1.deltaG(dr*aSigmaMul, 0.0, aSigmaMul);
        // 需要增加一个额外的偏移保证外部边界的统计正确性
        final double tRShift = -tDeltaGPar[0].zeroBoundL();
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(mAtomNum, (i, threadID) -> {
            int tTypeI = mTypeVec.get(i);
            if (tTypeI==aTypeA || tTypeI==aTypeB) {
                final int tTypeJ = tTypeI==aTypeA ? aTypeB : aTypeA;
                final IFunc1 dn = dnPar[threadID];
                final IZeroBoundFunc1 tDeltaG = tDeltaGPar[threadID];
                mNL.forEachNeighbor(i, aRMax+tRShift, true, (x, y, z, idx, dis2) -> {
                    if (mTypeVec.get(idx) == tTypeJ) {
                        tDeltaG.setX0(Fast.sqrt(dis2));
                        dn.plus2this(tDeltaG);
                    }
                });
            }
        });
        
        // 获取结果
        IFunc1 gr = dnPar[0];
        for (int i = 1; i < dnPar.length; ++i) gr.plus2this(dnPar[i]);
        double rho = mAtomNumType.get(aTypeA-1) * mAtomNumType.get(aTypeB-1) / mVolume;
        if (aTypeA == aTypeB) rho *= 0.5;
        final double fRho = rho;
        gr.operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*fRho)));
        
        // 修复截断数据
        gr.set(0, 0.0);
        // 输出
        return gr;
    }
    public IFunc1 calRDF_AB_G(int aTypeA, int aTypeB, int aN, double aRMax) {return calRDF_AB_G(aTypeA, aTypeB, aN, aRMax, 4);}
    public IFunc1 calRDF_AB_G(int aTypeA, int aTypeB, int aN              ) {return calRDF_AB_G(aTypeA, aTypeB, aN, mUnitLen*6);}
    public IFunc1 calRDF_AB_G(int aTypeA, int aTypeB                      ) {return calRDF_AB_G(aTypeA, aTypeB, 160);}
    
    
    /**
     * 使用带有一定展宽的高斯分布代替直接计数来计算所有种类之间的 RDF，
     * 只计算一个固定结构的值，因此不包含温度信息。
     * <p>
     * 所有的 {@code g_AB(r)} 直接排列成一个列表，
     * 先遍历 {@code typeB} 后遍历 {@code typeA}，且保持
     * {@code typeB <= typeA}，也就是说，如果需要访问给定 {@code (typeA, typeB)}
     * 的 {@code g_AB(r)}，需要使用：
     * <pre> {@code
     * def grAll = mpc.calAllRDF_G(...)
     * int idx = (typeA*(typeA-1)).intdiv(2) + typeB
     * def grAB = grAll[idx]
     * } </pre>
     * 来获取（注意到 {@code idx == 0} 对应所有种类都考虑的
     * g(r)，并且这里认为所有的种类都是从 1 开始的）。
     * <p>
     * 当只有一个种类时不进行单独种类的计算，即此时返回的向量长度一定为 1
     * @author liqa
     * @param aN 指定分划的份数（默认为 160）
     * @param aRMax 指定计算的最大半径（默认为 6 倍单位长度）
     * @param aSigmaMul 高斯分布的一个标准差宽度对应的分划份数，默认为 4
     * @return 所有 gr 函数组成的列表
     */
    public List<? extends IFunc1> calAllRDF_G(int aN, final double aRMax, int aSigmaMul) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 当只有一个种类时不进行单独种类的计算
        if (mAtomTypeNum == 1) return Collections.singletonList(calRDF_G(aN, aRMax, aSigmaMul));
        
        final double dr = aRMax/aN;
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final List<IFunc1[]> dnAllPar = NewCollections.from(threadNumber(), i -> {
            IFunc1[] dnAll = new IFunc1[(mAtomTypeNum*(mAtomTypeNum+1))/2 + 1];
            for (int j = 0; j < dnAll.length; ++j) {
                dnAll[j] = FixBoundFunc1.zeros(0.0, dr, aN).setBound(0.0, 1.0);
            }
            return dnAll;
        });
        
        // 并行需要线程数个独立的 DeltaG
        final IZeroBoundFunc1[] tDeltaGPar = new IZeroBoundFunc1[threadNumber()];
        for (int i = 0; i < tDeltaGPar.length; ++i) tDeltaGPar[i] = Func1.deltaG(dr*aSigmaMul, 0.0, aSigmaMul);
        // 需要增加一个额外的偏移保证外部边界的统计正确性
        final double tRShift = -tDeltaGPar[0].zeroBoundL();
        
        // 使用 mNL 的专门获取近邻距离的方法
        pool().parfor(mAtomNum, (i, threadID) -> {
            final int tTypeA = mTypeVec.get(i);
            final IFunc1[] dnAll = dnAllPar.get(threadID);
            final IZeroBoundFunc1 tDeltaG = tDeltaGPar[threadID];
            mNL.forEachNeighbor(i, aRMax+tRShift, true, (x, y, z, idx, dis2) -> {
                tDeltaG.setX0(Fast.sqrt(dis2));
                dnAll[0].plus2this(tDeltaG);
                int tTypeB = mTypeVec.get(idx);
                dnAll[(tTypeA*(tTypeA-1))/2 + tTypeB].plus2this(tDeltaG);
            });
        });
        
        // 获取结果
        Iterator<IFunc1[]> it = dnAllPar.iterator();
        IFunc1[] grAll = it.next();
        it.forEachRemaining(dnAll -> {
            for (int i = 0; i < grAll.length; ++i) grAll[i].plus2this(dnAll[i]);
        });
        double rho = mAtomNum*0.5 * mRho; // mAtomNum*0.5 为对所有原子求和需要进行的平均
        final double fRho = rho;
        grAll[0].operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*fRho)));
        int idx = 1;
        for (int typeAmm = 0; typeAmm < mAtomTypeNum; ++typeAmm) for (int typeBmm = 0; typeBmm <= typeAmm; ++typeBmm) {
            rho = mAtomNumType.get(typeAmm) * mAtomNumType.get(typeBmm) / mVolume;
            if (typeAmm == typeBmm) rho *= 0.5;
            final double fRhoAB = rho;
            grAll[idx].operation().mapFull2this((g, r) -> (g / (r*r*4.0*PI*fRhoAB)));
            ++idx;
        }
        
        // 修复截断数据
        for (IFunc1 gr : grAll) gr.set(0, 0.0);
        // 输出
        return AbstractCollections.from(grAll);
    }
    public List<? extends IFunc1> calAllRDF_G(int aN, double aRMax) {return calAllRDF_G(aN, aRMax, 4);}
    public List<? extends IFunc1> calAllRDF_G(int aN              ) {return calAllRDF_G(aN, mUnitLen*6);}
    public List<? extends IFunc1> calAllRDF_G(                    ) {return calAllRDF_G(160);}
    
    
    
    /**
     * 计算 SF（structural factor，即 S(q)），只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aN 指定分划的份数（默认为 160）
     * @param aQMax 额外指定最大计算的 q 的位置（默认为 6 倍单位长度）
     * @param aQMin 可以手动指定最小的截断的 q（由于 pbc 的原因，过小的结果发散）
     * @return Sq 函数
     */
    public IFunc1 calSF(int aN, double aQMax, double aQMin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dq = (aQMax-aQMin)/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final IFunc1[] HqPar = new IFunc1[threadNumber()];
        for (int i = 0; i < HqPar.length; ++i) HqPar[i] = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        
        // 需要这样遍历才能得到正确结果
        pool().parfor(mAtomNum, (i, threadID) -> {
            XYZ cXYZ = new XYZ(mAtomDataXYZ.row(i));
            IFunc1 Hq = HqPar[threadID];
            for (int j = 0; j < i; ++j) {
                final double dis = cXYZ.distance(mAtomDataXYZ.get(j, 0), mAtomDataXYZ.get(j, 1), mAtomDataXYZ.get(j, 2));
                Hq.operation().mapFull2this((H, q) -> (H + Fast.sin(q*dis)/(q*dis)));
            }
        });
        
        // 获取结果
        IFunc1 Sq = HqPar[0];
        for (int i = 1; i < HqPar.length; ++i) Sq.plus2this(HqPar[i]);
        Sq.div2this(mAtomNum*0.5);
        Sq.plus2this(1.0);
        
        // 修复截断数据
        Sq.set(0, 0.0);
        // 输出
        return Sq;
    }
    public IFunc1 calSF(int aN, double aQMax) {return calSF(aN, aQMax, 2.0*PI/mUnitLen * 0.6);}
    public IFunc1 calSF(int aN              ) {return calSF(aN, 2.0*PI/mUnitLen * 6.0);}
    public IFunc1 calSF(                    ) {return calSF(160);}
    
    
    private IFunc1 calSF_AB_(final IMatrix aAtomDataXYZ, int aAtomNum, int aN, double aQMax, double aQMin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dq = (aQMax-aQMin)/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final IFunc1[] HqPar = new IFunc1[threadNumber()];
        for (int i = 0; i < HqPar.length; ++i) HqPar[i] = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        
        // 需要这样遍历才能得到正确结果
        pool().parfor(aAtomNum, (i, threadID) -> {
            XYZ cXYZ = new XYZ(aAtomDataXYZ.row(i));
            IFunc1 Hq = HqPar[threadID];
            for (int j = 0; j < mAtomNum; ++j) {
                double tX = mAtomDataXYZ.get(j, 0);
                double tY = mAtomDataXYZ.get(j, 1);
                double tZ = mAtomDataXYZ.get(j, 2);
                // 当类型相同时可能存在相同原子
                if (!cXYZ.numericEqual(tX, tY, tZ)) {
                    final double dis = cXYZ.distance(tX, tY, tZ);
                    Hq.operation().mapFull2this((H, q) -> (H + Fast.sin(q*dis)/(q*dis)));
                }
            }
        });
        
        // 获取结果
        IFunc1 Sq = HqPar[0];
        for (int i = 1; i < HqPar.length; ++i) Sq.plus2this(HqPar[i]);
        Sq.div2this(Fast.sqrt(mAtomNum*aAtomNum));
        Sq.plus2this(1.0);
        
        // 修复截断数据
        Sq.set(0, 0.0);
        // 输出
        return Sq;
    }
    /**
     * 计算自身与输入的 aAtomDataXYZ 之间的 SF，只计算一个固定结构的值，因此不包含温度信息
     * @param aAtomDataXYZ 另一个元素的 xyz 坐标组成的列表，或者输入 aMPC 即计算两个 MPC 之间的 RDF
     * @param aN 指定分划的份数（默认为 160）
     * @param aQMax 额外指定最大计算的 q 的位置
     * @param aQMin 手动指定最小的截断的 q
     * @return Sq 函数
     */
    public IFunc1 calSF_AB(Collection<? extends IXYZ> aAtomDataXYZ, int aN, double aQMax, double aQMin) {
        IMatrix tAtomDataXYZ = getValidAtomDataXYZ_(aAtomDataXYZ);
        IFunc1 tOut = calSF_AB_(tAtomDataXYZ, aAtomDataXYZ.size(), aN, aQMax, aQMin);
        MatrixCache.returnMat(tAtomDataXYZ);
        return tOut;
    }
    public IFunc1 calSF_AB(Collection<? extends IXYZ> aAtomDataXYZ, int aN, double aQMax) {return calSF_AB(aAtomDataXYZ, aN, aQMax, 2.0*PI/mUnitLen * 0.6);}
    public IFunc1 calSF_AB(Collection<? extends IXYZ> aAtomDataXYZ, int aN              ) {return calSF_AB(aAtomDataXYZ, aN, 2.0*PI/mUnitLen * 6.0);}
    public IFunc1 calSF_AB(Collection<? extends IXYZ> aAtomDataXYZ                      ) {return calSF_AB(aAtomDataXYZ, 160);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC, int aN, double aQMax, double aQMin) {return calSF_AB_(aMPC.mAtomDataXYZ, aMPC.mAtomNum, aN, aQMax, aQMin);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC, int aN, double aQMax              ) {return calSF_AB(aMPC, aN, aQMax, 2.0*PI/mUnitLen * 0.6);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC, int aN                            ) {return calSF_AB(aMPC, aN, 2.0*PI/mUnitLen * 6.0);}
    public IFunc1 calSF_AB(MonatomicParameterCalculator aMPC                                    ) {return calSF_AB(aMPC, 160);}
    
    /**
     * 计算两种种类之间的 SF，只计算一个固定结构的值，因此不包含温度信息
     * @author liqa
     * @param aTypeA 种类 A
     * @param aTypeB 种类 B
     * @param aN 指定分划的份数（默认为 160）
     * @param aQMax 额外指定最大计算的 q 的位置
     * @param aQMin 手动指定最小的截断的 q
     * @return Sq 函数
     */
    public IFunc1 calSF_AB(final int aTypeA, final int aTypeB, int aN, double aQMax, double aQMin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final double dq = (aQMax-aQMin)/aN;
        // 这里的 parfor 支持不同线程直接写入不同位置而不需要加锁
        final IFunc1[] HqPar = new IFunc1[threadNumber()];
        for (int i = 0; i < HqPar.length; ++i) HqPar[i] = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        
        // 需要这样遍历才能得到正确结果
        pool().parfor(mAtomNum, (i, threadID) -> {
            int tTypeI = mTypeVec.get(i);
            if (tTypeI==aTypeA || tTypeI==aTypeB) {
                int tTypeJ = tTypeI==aTypeA ? aTypeB : aTypeA;
                XYZ cXYZ = new XYZ(mAtomDataXYZ.row(i));
                IFunc1 Hq = HqPar[threadID];
                for (int j = 0; j < i; ++j) if (mTypeVec.get(j) == tTypeJ) {
                    final double dis = cXYZ.distance(mAtomDataXYZ.get(j, 0), mAtomDataXYZ.get(j, 1), mAtomDataXYZ.get(j, 2));
                    Hq.operation().mapFull2this((H, q) -> (H + Fast.sin(q*dis)/(q*dis)));
                }
            }
        });
        
        // 获取结果
        IFunc1 Sq = HqPar[0];
        for (int i = 1; i < HqPar.length; ++i) Sq.plus2this(HqPar[i]);
        double tDiv = Fast.sqrt(mAtomNumType.get(aTypeA-1) * mAtomNumType.get(aTypeB-1));
        if (aTypeA == aTypeB) tDiv *= 0.5;
        Sq.div2this(tDiv);
        Sq.plus2this(1.0);
        
        // 修复截断数据
        Sq.set(0, 0.0);
        // 输出
        return Sq;
    }
    public IFunc1 calSF_AB(int aTypeA, int aTypeB, int aN, double aQMax) {return calSF_AB(aTypeA, aTypeB, aN, aQMax, 2.0*PI/mUnitLen * 0.6);}
    public IFunc1 calSF_AB(int aTypeA, int aTypeB, int aN              ) {return calSF_AB(aTypeA, aTypeB, aN, 2.0*PI/mUnitLen * 6);}
    public IFunc1 calSF_AB(int aTypeA, int aTypeB                      ) {return calSF_AB(aTypeA, aTypeB, 160);}
    
    
    /**
     * 计算所有种类之间的 SF（structural factor，即 S(q)），
     * 只计算一个固定结构的值，因此不包含温度信息。
     * <p>
     * 所有的 {@code S_AB(q)} 直接排列成一个列表，
     * 先遍历 {@code typeB} 后遍历 {@code typeA}，且保持
     * {@code typeB <= typeA}，也就是说，如果需要访问给定 {@code (typeA, typeB)}
     * 的 {@code S_AB(q)}，需要使用：
     * <pre> {@code
     * def SqAll = mpc.calAllSF(...)
     * int idx = (typeA*(typeA-1)).intdiv(2) + typeB
     * def SqAB = SqAll[idx]
     * } </pre>
     * 来获取（注意到 {@code idx == 0} 对应所有种类都考虑的
     * S(q)，并且这里认为所有的种类都是从 1 开始的）。
     * <p>
     * 当只有一个种类时不进行单独种类的计算，即此时返回的向量长度一定为 1
     * @author liqa
     * @param aN 指定分划的份数（默认为 160）
     * @param aQMax 额外指定最大计算的 q 的位置（默认为 6 倍单位长度）
     * @param aQMin 可以手动指定最小的截断的 q（由于 pbc 的原因，过小的结果发散）
     * @return 所有 Sq 函数组成的列表
     */
    public List<? extends IFunc1> calAllSF(int aN, double aQMax, double aQMin) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 当只有一个种类时不进行单独种类的计算
        if (mAtomTypeNum == 1) return Collections.singletonList(calSF(aN, aQMax, aQMin));
        
        final double dq = (aQMax-aQMin)/aN;
        // 这里需要使用 IFunc 来进行函数的相关运算操作
        final List<IFunc1[]> HqAllPar = NewCollections.from(threadNumber(), i -> {
            IFunc1[] HqAll = new IFunc1[(mAtomTypeNum*(mAtomTypeNum+1))/2 + 1];
            for (int j = 0; j < HqAll.length; ++j) {
                HqAll[j] = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
            }
            return HqAll;
        });
        // 累加量缓存，可以避免重复计算
        final List<? extends IVector> tDeltaPar = VectorCache.getVec(aN+1, threadNumber());
        
        // 需要这样遍历才能得到正确结果
        pool().parfor(mAtomNum, (i, threadID) -> {
            XYZ cXYZ = new XYZ(mAtomDataXYZ.row(i));
            int tTypeA = mTypeVec.get(i);
            IFunc1[] HqAll = HqAllPar.get(threadID);
            IVector tDelta = tDeltaPar.get(threadID);
            for (int j = 0; j < i; ++j) {
                final double dis = cXYZ.distance(mAtomDataXYZ.get(j, 0), mAtomDataXYZ.get(j, 1), mAtomDataXYZ.get(j, 2));
                tDelta.operation().operate2this(HqAll[0].x(), (any, q) -> Fast.sin(q*dis)/(q*dis));
                HqAll[0].f().plus2this(tDelta);
                int tTypeB = mTypeVec.get(j);
                HqAll[(tTypeA*(tTypeA-1))/2 + tTypeB].f().plus2this(tDelta);
            }
        });
        
        // 获取结果
        Iterator<IFunc1[]> it = HqAllPar.iterator();
        IFunc1[] SqAll = it.next();
        it.forEachRemaining(HqAll -> {
            for (int i = 0; i < HqAll.length; ++i) SqAll[i].plus2this(HqAll[i]);
        });
        SqAll[0].div2this(mAtomNum*0.5);
        int idx = 1;
        for (int typeAmm = 0; typeAmm < mAtomTypeNum; ++typeAmm) for (int typeBmm = 0; typeBmm <= typeAmm; ++typeBmm) {
            double tDiv = Fast.sqrt(mAtomNumType.get(typeAmm) * mAtomNumType.get(typeBmm));
            if (typeAmm == typeBmm) tDiv *= 0.5;
            SqAll[idx].div2this(tDiv);
            ++idx;
        }
        for (IFunc1 Sq : SqAll) Sq.plus2this(1.0);
        
        // 归还临时变量
        VectorCache.returnVec(tDeltaPar);
        
        // 修复截断数据
        for (IFunc1 Sq : SqAll) Sq.set(0, 0.0);
        // 输出
        return AbstractCollections.from(SqAll);
    }
    public List<? extends IFunc1> calAllSF(int aN, double aQMax) {return calAllSF(aN, aQMax, 2.0*PI/mUnitLen * 0.6);}
    public List<? extends IFunc1> calAllSF(int aN              ) {return calAllSF(aN, 2.0*PI/mUnitLen * 6);}
    public List<? extends IFunc1> calAllSF(                    ) {return calAllSF(160);}
    
    
    
    /// gr 和 Sq 的相互转换，由于依旧需要体系的原子数密度，因此还是都移动到 MPC 中
    /**
     * 转换 g(r) 到 S(q)，这是主要计算 S(q) 的方法
     * @author liqa
     * @param aGr the matrix form of g(r)
     * @param aRho the atom number density（默认会选择本 MPC 得到的密度）
     * @param aN the split number of output（默认为 160）
     * @param aQMax the max q of output S(q)（默认为 7.6 倍 gr 第一峰对应的距离）
     * @param aQMin the min q of output S(q)（默认为 0.5 倍 gr 第一峰对应的距离）
     * @return the structural factor, S(q)
     */
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRho, int aN, double aQMax, double aQMin) {
        double dq = (aQMax-aQMin)/aN;
        
        IFunc1 Sq = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        Sq.fill(aGr.operation().refConvolveFull((gr, r, q) -> (r * (gr-1.0) * Fast.sin(q*r) / q)));
        Sq.multiply2this(4.0*PI*aRho);
        Sq.plus2this(1.0);
        
        Sq.set(0, 0.0);
        return Sq;
    }
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRho, int aN, double aQMax) {return RDF2SF(aGr, aRho, aN, aQMax, 2.0*PI/aGr.operation().maxX() * 0.5);}
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRho, int aN              ) {return RDF2SF(aGr, aRho, aN, 2.0*PI/aGr.operation().maxX()* 7.6, 2.0*PI/aGr.operation().maxX() * 0.5);}
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRho                      ) {return RDF2SF(aGr, aRho, 160);}
    public        IFunc1 RDF2SF(IFunc1 aGr                                   ) {return RDF2SF(aGr, mRho);}
    
    
    /**
     * 转换 S(q) 到 g(r)
     * @author liqa
     * @param aSq the matrix form of S(q)
     * @param aRho the atom number density（默认会选择本 MPC 得到的密度）
     * @param aN the split number of output（默认为 160）
     * @param aRMax the max r of output g(r)（默认为 7.6 倍 Sq 第一峰对应的距离）
     * @param aRMin the min r of output g(r)（默认为 0.5 倍 Sq 第一峰对应的距离）
     * @return the radial distribution function, g(r)
     */
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRho, int aN, double aRMax, double aRMin) {
        double dr = (aRMax-aRMin)/aN;
        
        IFunc1 gr = FixBoundFunc1.zeros(aRMin, dr, aN+1).setBound(0.0, 1.0);
        gr.fill(aSq.operation().refConvolveFull((Sq, q, r) -> (q * (Sq-1.0) * Fast.sin(q*r) / r)));
        gr.multiply2this(1.0/(2.0*PI*PI*aRho));
        gr.plus2this(1.0);
        
        gr.set(0, 0.0);
        return gr;
    }
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRho, int aN, double aRMax) {return SF2RDF(aSq, aRho, aN, aRMax, 2.0*PI/aSq.operation().maxX() * 0.5);}
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRho, int aN              ) {return SF2RDF(aSq, aRho, aN, 2.0*PI/aSq.operation().maxX() * 7.6, 2.0*PI/aSq.operation().maxX() * 0.5);}
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRho                      ) {return SF2RDF(aSq, aRho, 160);}
    public        IFunc1 SF2RDF(IFunc1 aSq                                   ) {return SF2RDF(aSq, mRho);}
    
    
    
    /**
     * 直接获取近邻列表的 api，不包括自身
     * @author liqa
     */
    public IIntVector getNeighborList(int aIdx, double aRMax, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntList @Nullable[] tNL = getValidBufferedNL_(aRMax, aNnn, false);
        if (tNL != null) return tNL[aIdx].copy2vec();
        
        // 如果为 null 则直接遍历指定 idx，如果需要重复使用则直接在外部缓存即可
        final IntVector.Builder rNL = IntVector.builder();
        mNL.forEachNeighbor(aIdx, aRMax, aNnn, (x, y, z, idx, dis2) -> rNL.add(idx));
        return rNL.build();
    }
    public IIntVector getNeighborList(int aIdx, double aRMax) {return getNeighborList(aIdx, aRMax, -1);}
    public IIntVector getNeighborList(int aIdx              ) {return getNeighborList(aIdx, mUnitLen*R_NEAREST_MUL);}
    
    public IIntVector getNeighborList(IXYZ aXYZ, double aRMax, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 由于 lammps 精度的问题，需要将超出边界的进行平移
        if (mBox.isPrism()) {
            // 斜方情况需要转为 Direct 再 wrap，
            // 完事后再转回 Cartesian
            XYZ tBuf = new XYZ(aXYZ);
            mBox.toDirect(tBuf);
            if      (tBuf.mX <  0.0) {do {++tBuf.mX;} while (tBuf.mX <  0.0);}
            else if (tBuf.mX >= 1.0) {do {--tBuf.mX;} while (tBuf.mX >= 1.0);}
            if      (tBuf.mY <  0.0) {do {++tBuf.mY;} while (tBuf.mY <  0.0);}
            else if (tBuf.mY >= 1.0) {do {--tBuf.mY;} while (tBuf.mY >= 1.0);}
            if      (tBuf.mZ <  0.0) {do {++tBuf.mZ;} while (tBuf.mZ <  0.0);}
            else if (tBuf.mZ >= 1.0) {do {--tBuf.mZ;} while (tBuf.mZ >= 1.0);}
            mBox.toCartesian(tBuf);
            aXYZ = tBuf;
        } else {
            XYZ tBox = XYZ.toXYZ(mBox);
            double tX = aXYZ.x(), tY = aXYZ.y(), tZ = aXYZ.z();
            if      (tX <  0.0    ) {do {tX += tBox.mX;} while (tX <  0.0    );}
            else if (tX >= tBox.mX) {do {tX -= tBox.mX;} while (tX >= tBox.mX);}
            if      (tY <  0.0    ) {do {tY += tBox.mY;} while (tY <  0.0    );}
            else if (tY >= tBox.mY) {do {tY -= tBox.mY;} while (tY >= tBox.mY);}
            if      (tZ <  0.0    ) {do {tZ += tBox.mZ;} while (tZ <  0.0    );}
            else if (tZ >= tBox.mZ) {do {tZ -= tBox.mZ;} while (tZ >= tBox.mZ);}
        }
        
        final IntVector.Builder rNL = IntVector.builder();
        mNL.forEachNeighbor(aXYZ, aRMax, aNnn, (x, y, z, idx, dis2) -> rNL.add(idx));
        return rNL.build();
    }
    public IIntVector getNeighborList(IXYZ aXYZ, double aRMax) {return getNeighborList(aXYZ, aRMax, -1);}
    public IIntVector getNeighborList(IXYZ aXYZ              ) {return getNeighborList(aXYZ, mUnitLen*R_NEAREST_MUL);}
    
    
    /** 用于分割模拟盒，判断给定 XYZ 或者 idx 处的原子是否在需要考虑的区域中 */
    private class MPIInfo implements IAutoShutdown {
        final MPI.Comm mComm;
        final int mRank, mSize;
        final XYZ mCellSize;
        final int mSizeX, mSizeY, mSizeZ;
        private final double mXLo, mXHi, mYLo, mYHi, mZLo, mZHi;
        MPIInfo(MPI.Comm aComm) throws MPIException {
            // 这里简单处理，MPI 只支持非斜方的模拟盒
            if (mBox.isPrism()) throw new IllegalArgumentException("MonatomicParameterCalculator only provides MPI support for orthogonal box");
            mComm = aComm;
            mRank = mComm.rank();
            mSize = mComm.size();
            int tSizeRest = mSize;
            // 使用这个方法来获取每个方向的分划数
            IntList rFactors = new IntList();
            for (int tFactor = 2; tFactor <= tSizeRest; ++tFactor) {
                while (tSizeRest % tFactor == 0) {
                    rFactors.add(tFactor);
                    tSizeRest /= tFactor;
                }
            }
            int rSizeX = 1, rSizeY = 1, rSizeZ = 1;
            // 直接这样逆序遍历，应该没有效率损失
            for (int i = rFactors.size()-1; i >= 0; --i) {
                int tFactor = rFactors.get(i);
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
            int tI = mRank%mSizeX;
            int tJ = mRank/mSizeX%mSizeY;
            int tK = mRank/mSizeX/mSizeY;
            mCellSize = mBox.div(mSizeX, mSizeY, mSizeZ);
            mXLo = tI * mCellSize.mX; mXHi = mXLo + mCellSize.mX;
            mYLo = tJ * mCellSize.mY; mYHi = mYLo + mCellSize.mY;
            mZLo = tK * mCellSize.mZ; mZHi = mZLo + mCellSize.mZ;
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
        @SuppressWarnings("RedundantIfStatement")
        private boolean inEdge_(double aX, double aY, double aZ, double aRMax) {
            double tRestX = aX % mCellSize.mX;
            if (tRestX <= aRMax || tRestX >= mCellSize.mX-aRMax) return true;
            double tRestY = aY % mCellSize.mY;
            if (tRestY <= aRMax || tRestY >= mCellSize.mY-aRMax) return true;
            double tRestZ = aZ % mCellSize.mZ;
            if (tRestZ <= aRMax || tRestZ >= mCellSize.mZ-aRMax) return true;
            return false;
        }
        
        private boolean mInitialized = false;
        private double mEdgeRMax = Double.NaN;
        private IntVector mCounts;
        private IntVector mDispls;
        private IntVector mBuf2Idx;
        private void initCountsAll_() {initCountsEdge_(Double.NaN);}
        private void initCountsEdge_(double aRMax) {
            if (mDead) throw new RuntimeException("This MPIInfo is dead");
            final boolean tInitAll = Double.isNaN(aRMax);
            if (tInitAll) {
                if (mInitialized && Double.isNaN(mEdgeRMax)) return;
            } else {
                if (mInitialized && !Double.isNaN(mEdgeRMax) && MathEX.Code.numericEqual(mEdgeRMax, aRMax)) return;
            }
            // 如果已经初始化过记得归还旧资源
            if (mInitialized) {
                IntVectorCache.returnVec(mCounts);
                IntVectorCache.returnVec(mDispls);
                IntVectorCache.returnVec(mBuf2Idx);
            }
            mInitialized = true;
            mEdgeRMax = aRMax;
            mCounts = IntVectorCache.getZeros(mSize);
            final int[][] rBuf2Idx = new int[mSize][];
            IntArrayCache.getArrayTo(mAtomNum, mSize, (i, array) -> rBuf2Idx[i] = array);
            // 遍历所有的原子统计位置
            for (int i = 0; i < mAtomNum; ++i) {
                double tX = mAtomDataXYZ.get(i, 0);
                double tY = mAtomDataXYZ.get(i, 1);
                double tZ = mAtomDataXYZ.get(i, 2);
                // 如果设置了 aRMax 则跳过在中间的原子即可
                if (!tInitAll && !inEdge_(tX, tY, tZ, aRMax)) continue;
                int tI = MathEX.Code.floor2int(tX / mCellSize.mX);
                int tJ = MathEX.Code.floor2int(tY / mCellSize.mY);
                int tK = MathEX.Code.floor2int(tZ / mCellSize.mZ);
                // 现在排序和 LinkedCell 保持相同了
                int tRank = (tI + tJ*mSizeX + tK*mSizeX*mSizeY);
                int tCount = mCounts.get(tRank);
                rBuf2Idx[tRank][tCount] = i;
                mCounts.increment(tRank);
            }
            mBuf2Idx = IntVectorCache.getVec(mAtomNum);
            mDispls = IntVectorCache.getVec(mSize+1); // Displs 增加一个长度来存储所有长度，并且可以不用在循环中判断
            mDispls.set(0, 0);
            for (int i = 0; i < mSize; ++i) {
                int tStart = mDispls.get(i);
                int tCount = mCounts.get(i);
                int tEnd = tStart+tCount;
                mBuf2Idx.subVec(tStart, tEnd).fill(rBuf2Idx[i]);
                mDispls.set(i+1, tEnd);
            }
            IntArrayCache.returnArrayFrom(mSize, i -> rBuf2Idx[i]);
        }
        
        /** 收集所有数据到所有进程，主要用于最终输出 */
        void allgather(IComplexMatrix rData) throws MPIException {
            allgather(rData, Double.NaN);
        }
        void allgather(IVector rData) throws MPIException {
            allgather(rData, Double.NaN);
        }
        
        /** 收集边界数据到所有线程，主要用于近邻搜索中获取必要的数据；这种实现比 send/recv 更加简单高效 */
        void allgather(IComplexMatrix rData, double aRMax) throws MPIException {
            // 如果超过 cell 的半径则设置全部同步
            if (Double.isNaN(aRMax) || aRMax+aRMax>=mCellSize.min()) initCountsAll_();
            else initCountsEdge_(aRMax);
            
            final int tMul = rData.columnNumber();
            final int tBufSize = mDispls.last();
            // 先创建临时的同步数组
            RowComplexMatrix rBuf = ComplexMatrixCache.getMatRow(tBufSize, tMul);
            try {
                // 通过 buf2idx, counts 和 displs 来将需要的数据放入 rBuf 指定位置
                final int tStart = mDispls.get(mRank);
                final int tEnd = tStart + mCounts.get(mRank);
                for (int i = tStart; i < tEnd; ++i) {
                    rBuf.row(i).fill(rData.row(mBuf2Idx.get(i)));
                }
                // 使用 allgatherv 收集所有 buf，注意实际 mCounts 和 mDispls 需要临时增倍；
                // 这样会导致线程不安全，当然这里 MPIInfo 都是在调用计算函数时临时创建的，因此也不存在这个问题
                mCounts.multiply2this(tMul);
                mDispls.multiply2this(tMul);
                try {
                    double[][] tData = rBuf.internalData();
                    mComm.allgatherv(tData[0], mCounts.internalData(), mDispls.internalData());
                    mComm.allgatherv(tData[1], mCounts.internalData(), mDispls.internalData());
                } finally {
                    mCounts.div2this(tMul);
                    mDispls.div2this(tMul);
                }
                // 将在 buf 中的数据重新设回 rData
                for (int i = 0; i < tBufSize; ++i) {
                    rData.row(mBuf2Idx.get(i)).fill(rBuf.row(i));
                }
            } finally {
                // 归还临时数组
                ComplexMatrixCache.returnMat(rBuf);
            }
        }
        @SuppressWarnings("SameParameterValue")
        void allgather(IVector rData, double aRMax) throws MPIException {
            // 如果超过 cell 的半径则设置全部同步
            if (Double.isNaN(aRMax) || aRMax+aRMax>=mCellSize.min()) initCountsAll_();
            else initCountsEdge_(aRMax);
            
            final int tBufSize = mDispls.last();
            // 先创建临时的同步数组
            Vector rBuf = VectorCache.getVec(tBufSize);
            try {
                // 通过 buf2idx, counts 和 displs 来将需要的数据放入 rBuf 指定位置
                final int tStart = mDispls.get(mRank);
                final int tEnd = tStart + mCounts.get(mRank);
                for (int i = tStart; i < tEnd; ++i) {
                    rBuf.set(i, rData.get(mBuf2Idx.get(i)));
                }
                // 使用 allgatherv 收集所有 buf
                double[] tData = rBuf.internalData();
                mComm.allgatherv(tData, mCounts.internalData(), mDispls.internalData());
                // 将在 buf 中的数据重新设回 qlm
                for (int i = 0; i < tBufSize; ++i) {
                    rData.set(mBuf2Idx.get(i), rBuf.get(i));
                }
            } finally {
                // 归还临时数组
                VectorCache.returnVec(rBuf);
            }
        }
        
        private boolean mDead = false;
        @Override public void shutdown() {
            if (mDead) return;
            mDead = true;
            if (mInitialized) {
                IntVectorCache.returnVec(mCounts);
                IntVectorCache.returnVec(mDispls);
                IntVectorCache.returnVec(mBuf2Idx);
            }
        }
    }
    
    
    private static class BufferedNL {
        private final static int INIT_NL_SIZE = 16;
        
        private final IVector mBufferedNLRMax;
        private final boolean[] mBufferedNLHalf;
        private final int[] mBufferedNLMPI;
        private final int[] mBufferedNLNnn;
        private final IntList[][] mBufferedNL;
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
            mBufferedNL = new IntList[mSize][];
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
        IntList @Nullable[] getValidBufferedNL(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
            // 直接遍历搜索
            for (int i = 0; i < mSize; ++i) {
                if (mBufferedNLNnn[i]==aNnn && mBufferedNLHalf[i]==aHalf && mBufferedNLMPI[i]==aMPISize) {
                    IntList @Nullable[] tNL = mBufferedNL[i];
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
        IntList @NotNull[] getValidNLToBuffer(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
            // 这里直接根据 mIdx 返回
            mBufferedNLRMax.set(mIdx, aRMax);
            mBufferedNLHalf[mIdx] = aHalf;
            mBufferedNLMPI[mIdx]= aMPISize;
            mBufferedNLNnn[mIdx] = aNnn;
            @Nullable IntList @Nullable[] tNL = mBufferedNL[mIdx];
            if (tNL==null || tNL.length<mAtomNum) {
                IntList @Nullable[] oNL = tNL;
                tNL = new IntList[mAtomNum];
                if (oNL != null) System.arraycopy(oNL, 0, tNL, 0, oNL.length);
            }
            mBufferedNL[mIdx] = tNL;
            for (int i = 0; i < mAtomNum; ++i) {
                @Nullable IntList subNL = tNL[i];
                if (subNL == null) {
                    subNL = new IntList(INIT_NL_SIZE);
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
    @ApiStatus.Internal public IntList @Nullable[] getValidBufferedNL_(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
        initBufferNL_();
        if (mBufferedNL == null) return null;
        return mBufferedNL.getValidBufferedNL(aRMax, aNnn, aHalf, aMPISize);
    }
    @ApiStatus.Internal public IntList @Nullable[] getValidBufferedNL_(double aRMax, int aNnn, boolean aHalf) {
        return getValidBufferedNL_(aRMax, aNnn, aHalf, 1);
    }
    /**
     * 根据参数获取合适的 NL 用于缓存，
     * 此时 null 表示已经有了缓存或者关闭了近邻列表缓存或者参数非法（近邻半径过大）
     */
    @ApiStatus.Internal public IntList @Nullable[] getNLWhichNeedBuffer_(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
        initBufferNL_();
        if (mBufferedNL == null) return null;
        if (aRMax > BUFFER_NL_RMAX*mUnitLen) return null;
        if (mBufferedNL.getValidBufferedNL(aRMax, aNnn, aHalf, aMPISize) != null) return null;
        return mBufferedNL.getValidNLToBuffer(aRMax, aNnn, aHalf, aMPISize);
    }
    @ApiStatus.Internal public IntList @Nullable[] getNLWhichNeedBuffer_(double aRMax, int aNnn, boolean aHalf) {
        return getNLWhichNeedBuffer_(aRMax, aNnn, aHalf, 1);
    }
    
    /** 会自动使用缓存的近邻列表遍历，用于减少重复代码 */
    @ApiStatus.Internal public void forEachNeighbor_(IntList @Nullable[] aNL, int aIdx, double aRMax, int aNnn, boolean aHalf, IntConsumer aIdxDo) {
        if (aNL != null) {
            // 如果 aNL 不为 null，则直接使用 aNL 遍历
            aNL[aIdx].forEach(aIdxDo);
        } else {
            // aNL 为 null，则使用 mNL 完整遍历
            mNL.forEachNeighbor(aIdx, aRMax, aNnn, aHalf, (x, y, z, idx, dis2) -> aIdxDo.accept(idx));
        }
    }
    @ApiStatus.Internal public void forEachNeighbor_(IntList @Nullable[] aNL, int aIdx, double aRMax, int aNnn, boolean aHalf, IIndexFilter aRegion, IntConsumer aIdxDo) {
        if (aNL != null) {
            // 如果 aNL 不为 null，则直接使用 aNL 遍历
            aNL[aIdx].forEach(aIdxDo);
        } else {
            // aNL 为 null，则使用 mNL 完整遍历
            mNL.forEachNeighbor(aIdx, aRMax, aNnn, aHalf, aRegion, (x, y, z, idx, dis2) -> aIdxDo.accept(idx));
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
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return Qlm 组成的复向量数组
     */
    public IComplexMatrix calYlmMean(final int aL, double aRNearest, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aL < 0) throw new IllegalArgumentException("Input l MUST be Non-Negative, input: "+aL);
        
        // 构造用于并行的暂存数组，注意需要初始值为 0.0
        final List<? extends IComplexMatrix> rDestPar = ComplexMatrixCache.getZerosRow(mAtomNum, aL+aL+1, threadNumber());
        // 统计近邻数用于求平均，同样也需要为并行使用数组
        final List<? extends IVector> tNNPar = VectorCache.getZeros(mAtomNum, threadNumber());
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnn<=0;
        
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final List<? extends IComplexVector> tYPar = ComplexVectorCache.getVec(aL+aL+1, threadNumber());
        
        // 获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = getNLWhichNeedBuffer_(aRNearest, aNnn, aHalf);
        
        // 遍历计算 Qlm，只对这个最耗时的部分进行并行优化
        pool().parfor(mAtomNum, (i, threadID) -> {
            // 先获取这个线程的 Qlm, tNN
            final IComplexMatrix Qlm = rDestPar.get(threadID);
            final IVector tNN = tNNPar.get(threadID);
            final IComplexVector tY = tYPar.get(threadID);
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
                double cosPhi = dx / Fast.hypot(dx, dy);
                // 注意避免 NaN 以及由于精度越界的情况
                if (Double.isNaN(cosPhi)) cosPhi = 1.0;
                cosPhi = Code.toRange(-1.0, 1.0, cosPhi);
                double phi = (dy > 0) ? Fast.acos(cosPhi) : (2.0*PI - Fast.acos(cosPhi));
                
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                IComplexVector Qlmj = null;
                if (aHalf) {
                    Qlmj = Qlm.row(idx);
                }
                // 计算 Y 并累加，考虑对称性只需要算 m=0~l 的部分
                Func.sphericalHarmonics2Dest(aL, theta, phi, tY);
                Qlmi.plus2this(tY);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) Qlmj.plus2this(tY);
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[i].add(idx);}
                
                // 统计近邻数
                tNN.increment(i);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) {
                    tNN.increment(idx);
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
        for (int i = 0; i < mAtomNum; ++i) Qlm.row(i).div2this(tNN.get(i));
        
        // 归还临时变量
        for (int i = 1; i < rDestPar.size(); ++i) ComplexMatrixCache.returnMat(rDestPar.get(i));
        VectorCache.returnVec(tNNPar);
        ComplexVectorCache.returnVec(tYPar);
        
        return Qlm;
    }
    public IComplexMatrix calYlmMean(int aL, double aRNearest) {return calYlmMean(aL, aRNearest, -1);}
    public IComplexMatrix calYlmMean(int aL                  ) {return calYlmMean(aL, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的计算 计算所有粒子的近邻球谐函数的平均，即 Qlm */
    private IComplexMatrix calYlmMean_MPI_(boolean aNoGather, MPIInfo aMPIInfo, final int aL, double aRNearest, int aNnn) throws MPIException {
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
        
        // 获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = getNLWhichNeedBuffer_(aRNearest, aNnn, aHalf, aMPIInfo.mSize);
        
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
                double cosPhi = dx / Fast.hypot(dx, dy);
                // 注意避免 NaN 以及由于精度越界的情况
                if (Double.isNaN(cosPhi)) cosPhi = 1.0;
                cosPhi = Code.toRange(-1.0, 1.0, cosPhi);
                double phi = (dy > 0) ? Fast.acos(cosPhi) : (2.0*PI - Fast.acos(cosPhi));
                
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                IComplexVector Qlmj = null;
                if (tHalfStat) {
                    Qlmj = Qlm.row(idx);
                }
                // 计算 Y 并累加，考虑对称性只需要算 m=0~l 的部分
                Func.sphericalHarmonics2Dest(aL, theta, phi, tY);
                Qlmi.plus2this(tY);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (tHalfStat) Qlmj.plus2this(tY);
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
                
                // 统计近邻数
                tNN.increment(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (tHalfStat) {
                    tNN.increment(idx);
                }
            });
        }
        
        // 根据近邻数平均得到 Qlm
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            Qlm.row(i).div2this(tNN.get(i));
        }
        // 归还临时变量
        VectorCache.returnVec(tNN);
        ComplexVectorCache.returnVec(tY);
        
        // 收集所有进程将统计到的 Qlm，现在可以直接一起同步保证效率
        if (!aNoGather) aMPIInfo.allgather(Qlm);
        
        return Qlm;
    }
    private IComplexMatrix calYlmMean_MPI_(MPIInfo aMPIInfo, int aL, double aRNearest, int aNnn) throws MPIException {return calYlmMean_MPI_(aMPIInfo.mSize==1, aMPIInfo, aL, aRNearest, aNnn);}
    public IComplexMatrix calYlmMean_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calYlmMean_MPI_(aNoGather, tMPIInfo, aL, aRNearest, aNnn);}}
    public IComplexMatrix calYlmMean_MPI(MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calYlmMean_MPI_(tMPIInfo, aL, aRNearest, aNnn);}}
    
    
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
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
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
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestQ, aNnnQ, aHalf);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestQ, aNnnQ, aHalf) : null;
        
        // 遍历计算 qlm
        for (int i = 0; i < mAtomNum; ++i) {
            // 一次计算一行
            final IComplexVector qlmi = qlm.row(i);
            final IComplexVector Qlmi = Qlm.row(i);
            
            // 先累加自身（不直接拷贝矩阵因为以后会改成复数向量的数组）
            qlmi.fill(Qlmi);
            // 再累加近邻
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestQ, aNnnQ, aHalf, idx -> {
                // 直接按行累加即可
                qlmi.plus2this(Qlm.row(idx));
                // 如果开启 half 遍历的优化，对称的对面的粒子也要进行累加
                if (aHalf) {
                    qlm.row(idx).plus2this(Qlmi);
                }
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
                
                // 统计近邻数
                tNN.increment(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) {
                    tNN.increment(idx);
                }
            });
        }
        // 根据近邻数平均得到 qlm
        for (int i = 0; i < mAtomNum; ++i) qlm.row(i).div2this(tNN.get(i));
        
        // 归还临时变量
        VectorCache.returnVec(tNN);
        ComplexMatrixCache.returnMat(Qlm);
        
        return qlm;
    }
    public IComplexMatrix calQlmMean(int aL, double aRNearest, int aNnn) {return calQlmMean(aL, aRNearest, aNnn, aRNearest, aNnn);}
    public IComplexMatrix calQlmMean(int aL, double aRNearest          ) {return calQlmMean(aL, aRNearest, -1);}
    public IComplexMatrix calQlmMean(int aL                            ) {return calQlmMean(aL, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的在 Qlm 基础上再次对所有近邻做一次平均，即 qlm */
    private IComplexMatrix calQlmMean_MPI_(boolean aNoGather, final MPIInfo aMPIInfo, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {
        // 直接全部平均一遍分两步算
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aL < 0) throw new IllegalArgumentException("Input l MUST be Non-Negative, input: "+aL);
        
        final IComplexMatrix Qlm = calYlmMean_MPI_(true, aMPIInfo, aL, aRNearestY, aNnnY);
        aMPIInfo.allgather(Qlm, aRNearestQ); // 手动同步边界的数据用于计算 qlm
        final RowComplexMatrix qlm = ComplexMatrixCache.getZerosRow(mAtomNum, aL+aL+1);
        
        // 统计近邻数用于求平均（增加一个自身）
        final IVector tNN = VectorCache.getVec(mAtomNum);
        tNN.fill(1.0);
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnQ<=0;
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestQ, aNnnQ, aHalf, aMPIInfo.mSize);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestQ, aNnnQ, aHalf, aMPIInfo.mSize) : null;
        
        // 遍历计算 Qlm，这里直接判断原子位置是否是需要计算的然后跳过
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 一次计算一行
            final IComplexVector qlmi = qlm.row(i);
            final IComplexVector Qlmi = Qlm.row(i);
            
            // 先累加自身（不直接拷贝矩阵因为以后会改成复数向量的数组）
            qlmi.fill(Qlmi);
            // 再累加近邻
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestQ, aNnnQ, aHalf, aMPIInfo::inRegin, idx -> {
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
                tNN.increment(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (tHalfStat) {
                    tNN.increment(idx);
                }
            });
        }
        // 根据近邻数平均得到 qlm
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            qlm.row(i).div2this(tNN.get(i));
        }
        
        // 归还临时变量
        VectorCache.returnVec(tNN);
        ComplexMatrixCache.returnMat(Qlm);

        // 收集所有进程将统计到的 qlm，现在可以直接一起同步保证效率
        if (!aNoGather) aMPIInfo.allgather(qlm);
        
        return qlm;
    }
    private IComplexMatrix calQlmMean_MPI_(MPIInfo aMPIInfo, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {return calQlmMean_MPI_(aMPIInfo.mSize==1, aMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}
    public IComplexMatrix calQlmMean_MPI(boolean aNoGather, final MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calQlmMean_MPI_(aNoGather, tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    public IComplexMatrix calQlmMean_MPI(MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calQlmMean_MPI_(tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    
    
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
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
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
            Ql.set(i, Fast.sqrt(4.0*PI*tDot/(double)(aL+aL+1)));
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(Qlm);
        
        // 返回最终计算结果
        return Ql;
    }
    public IVector calBOOP(int aL, double aRNearest) {return calBOOP(aL, aRNearest, -1);}
    public IVector calBOOP(int aL                  ) {return calBOOP(aL, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的计算所有粒子的原始的 BOOP */
    private IVector calBOOP_MPI_(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aRNearest, int aNnn) throws MPIException {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        IComplexMatrix Qlm = calYlmMean_MPI_(true, aMPIInfo, aL, aRNearest, aNnn);
        
        // 直接求和
        Vector Ql = VectorCache.getVec(mAtomNum);
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 直接计算复向量的点乘
            double tDot = Qlm.row(i).operation().dot();
            // 使用这个公式设置 Ql
            Ql.set(i, Fast.sqrt(4.0*PI*tDot/(double)(aL+aL+1)));
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(Qlm);
        
        // 收集所有进程将统计到的 Ql
        if (!aNoGather) aMPIInfo.allgather(Ql);
        
        // 返回最终计算结果
        return Ql;
    }
    private IVector calBOOP_MPI_(MPIInfo aMPIInfo, int aL, double aRNearest, int aNnn) throws MPIException {return calBOOP_MPI_(aMPIInfo.mSize==1, aMPIInfo, aL, aRNearest, aNnn);}
    public IVector calBOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calBOOP_MPI_(aNoGather, tMPIInfo, aL, aRNearest, aNnn);}}
    public IVector calBOOP_MPI(MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calBOOP_MPI_(tMPIInfo, aL, aRNearest, aNnn);}}
    public IVector calBOOP_MPI(MPI.Comm aComm, int aL, double aRNearest          ) throws MPIException {return calBOOP_MPI(aComm, aL, aRNearest, -1);}
    public IVector calBOOP_MPI(MPI.Comm aComm, int aL                            ) throws MPIException {return calBOOP_MPI(aComm, aL, mUnitLen*R_NEAREST_MUL);}
    public IVector calBOOP_MPI(                int aL, double aRNearest, int aNnn) throws MPIException {return calBOOP_MPI(MPI.Comm.WORLD, aL, aRNearest, aNnn);}
    public IVector calBOOP_MPI(                int aL, double aRNearest          ) throws MPIException {return calBOOP_MPI(MPI.Comm.WORLD, aL, aRNearest);}
    public IVector calBOOP_MPI(                int aL                            ) throws MPIException {return calBOOP_MPI(MPI.Comm.WORLD, aL);}
    
    
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
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
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
                    ComplexDouble subMul = Qlmi.get(tM1+aL);
                    subMul.multiply2this(Qlmi.get(tM2+aL));
                    subMul.multiply2this(Qlmi.get(tM3+aL));
                    subMul.multiply2this(Func.wigner3j(aL, aL, aL, tM1, tM2, tM3));
                    // 累加到分子，这里只统计实数部分（虚数部分为 0）
                    rMul += subMul.mReal;
                }
            }
            // 最后求模量设置结果
            Wl.set(i, rMul/rDiv);
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
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
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
            ql.set(i, Fast.sqrt(4.0*PI*tDot/(double)(aL+aL+1)));
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
    private IVector calABOOP_MPI_(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        IComplexMatrix qlm = calQlmMean_MPI_(true, aMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);
        
        // 直接求和
        Vector ql = VectorCache.getVec(mAtomNum);
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 直接计算复向量的点乘
            double tDot = qlm.row(i).operation().dot();
            // 使用这个公式设置 ql
            ql.set(i, Fast.sqrt(4.0*PI*tDot/(double)(aL+aL+1)));
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(qlm);
        
        // 收集所有进程将统计到的 ql
        if (!aNoGather) aMPIInfo.allgather(ql);
        
        // 返回最终计算结果
        return ql;
    }
    private IVector calABOOP_MPI_(MPIInfo aMPIInfo, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {return calABOOP_MPI_(aMPIInfo.mSize==1, aMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}
    public IVector calABOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calABOOP_MPI_(aNoGather, tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calABOOP_MPI_(tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {return calABOOP_MPI(aComm, aL, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL, double aRNearest          ) throws MPIException {return calABOOP_MPI(aComm, aL, aRNearest, -1);}
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL                            ) throws MPIException {return calABOOP_MPI(aComm, aL, mUnitLen*R_NEAREST_MUL);}
    public IVector calABOOP_MPI(                int aL, double aRNearest, int aNnn) throws MPIException {return calABOOP_MPI(MPI.Comm.WORLD, aL, aRNearest, aNnn);}
    public IVector calABOOP_MPI(                int aL, double aRNearest          ) throws MPIException {return calABOOP_MPI(MPI.Comm.WORLD, aL, aRNearest);}
    public IVector calABOOP_MPI(                int aL                            ) throws MPIException {return calABOOP_MPI(MPI.Comm.WORLD, aL);}
    
    
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
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
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
                    ComplexDouble subMul = qlmi.get(tM1+aL);
                    subMul.multiply2this(qlmi.get(tM2+aL));
                    subMul.multiply2this(qlmi.get(tM3+aL));
                    subMul.multiply2this(Func.wigner3j(aL, aL, aL, tM1, tM2, tM3));
                    // 累加到分子，这里只统计实数部分（虚数部分为 0）
                    rMul += subMul.mReal;
                }
            }
            // 最后求模量设置结果
            wl.set(i, rMul/rDiv);
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
     * 通过 bond orientational order parameter（Ql）来计算结构中每个原子的连接数目，
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
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestS 用来计算 Sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 Sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数目组成的向量
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
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf) : null;
        
        // 计算近邻上 qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) {
            // 统一获取行向量
            final IComplexVector Qlmi = Qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestS, aNnnS, aHalf, idx -> {
                // 统一获取行向量
                IComplexVector Qlmj = Qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = Qlmi.operation().dot(Qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectCount.increment(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                    if (aHalf) {
                        tConnectCount.increment(idx);
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
    private IVector calConnectCountBOOP_MPI_(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix Qlm = calYlmMean_MPI_(true, aMPIInfo, aL, aRNearestY, aNnnY);
        aMPIInfo.allgather(Qlm, aRNearestS); // 手动同步边界的数据用于计算 Sij
        
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
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize) : null;
        
        // 计算近邻上 Qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 统一获取行向量
            final IComplexVector Qlmi = Qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestS, aNnnS, aHalf, aMPIInfo::inRegin, idx -> {
                // 统一获取行向量
                IComplexVector Qlmj = Qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = Qlmi.operation().dot(Qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectCount.increment(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                    boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                    if (tHalfStat) {
                        tConnectCount.increment(idx);
                    }
                }
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
            });
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(Qlm);
        
        // 收集所有进程将统计到的连接数
        if (!aNoGather) aMPIInfo.allgather(tConnectCount);
        
        // 返回最终计算结果
        return tConnectCount;
    }
    private IVector calConnectCountBOOP_MPI_(MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {return calConnectCountBOOP_MPI_(aMPIInfo.mSize==1, aMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}
    public IVector calConnectCountBOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountBOOP_MPI_(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}}
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountBOOP_MPI_(tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}}
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest          ) throws MPIException {return calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold                            ) throws MPIException {return calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    public IVector calConnectCountBOOP_MPI(                int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    public IVector calConnectCountBOOP_MPI(                int aL, double aConnectThreshold, double aRNearest          ) throws MPIException {return calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    public IVector calConnectCountBOOP_MPI(                int aL, double aConnectThreshold                            ) throws MPIException {return calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 通过 Averaged bond orientational order parameter（ql）来计算结构中每个原子的连接数目，
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
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @param aRNearestS 用来计算 sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数目组成的向量
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
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf) : null;
        
        // 计算近邻上 qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) {
            // 统一获取行向量
            final IComplexVector qlmi = qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestS, aNnnS, aHalf, idx -> {
                // 统一获取行向量
                IComplexVector qlmj = qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = qlmi.operation().dot(qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectCount.increment(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                    if (aHalf) {
                        tConnectCount.increment(idx);
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
    private IVector calConnectCountABOOP_MPI_(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix qlm = calQlmMean_MPI_(true, aMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);
        aMPIInfo.allgather(qlm, aRNearestS); // 手动同步边界的数据用于计算 sij
        
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
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize) : null;
        
        // 计算近邻上 qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 统一获取行向量
            final IComplexVector qlmi = qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestS, aNnnS, aHalf, aMPIInfo::inRegin, idx -> {
                // 统一获取行向量
                IComplexVector qlmj = qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = qlmi.operation().dot(qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectCount.increment(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                    boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                    if (tHalfStat) {
                        tConnectCount.increment(idx);
                    }
                }
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
            });
        }
        
        // 计算完成归还缓存数据
        ComplexMatrixCache.returnMat(qlm);
        
        // 收集所有进程将统计到的连接数
        if (!aNoGather) aMPIInfo.allgather(tConnectCount);
        
        // 返回最终计算结果
        return tConnectCount;
    }
    private IVector calConnectCountABOOP_MPI_(MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {return calConnectCountABOOP_MPI_(aMPIInfo.mSize==1, aMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}
    public IVector calConnectCountABOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountABOOP_MPI_(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}}
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountABOOP_MPI_(tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}}
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest          ) throws MPIException {return calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold                            ) throws MPIException {return calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    public IVector calConnectCountABOOP_MPI(                int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    public IVector calConnectCountABOOP_MPI(                int aL, double aConnectThreshold, double aRNearest          ) throws MPIException {return calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    public IVector calConnectCountABOOP_MPI(                int aL, double aConnectThreshold                            ) throws MPIException {return calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 通过 bond orientational order parameter（Ql）来计算结构中每个原子的连接数占所有近邻数的比例值，
     * 输出结果为按照输入原子顺序排列的向量，数值为 0~1 的比例值；
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
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestS 用来计算 Sij 的搜索的最近邻半径，会使用此值对应的近邻总数作为分母。默认为 aRNearestY
     * @param aNnnS 用来计算 Sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数占所有近邻数的比例值组成的向量
     */
    public IVector calConnectRatioBOOP(int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix Qlm = calYlmMean(aL, aRNearestY, aNnnY);
        
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnS<=0;
        // 统计连接数
        final IVector tConnectRatio = VectorCache.getZeros(mAtomNum);
        // 统计近邻数用于求平均
        final IVector tNN = VectorCache.getZeros(mAtomNum);
        
        // 注意需要先对 Qlm 归一化
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector Qlmi = Qlm.row(i);
            Qlmi.div2this(Qlmi.operation().norm());
        }
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf) : null;
        
        // 计算近邻上 qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) {
            // 统一获取行向量
            final IComplexVector Qlmi = Qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestS, aNnnS, aHalf, idx -> {
                // 统一获取行向量
                IComplexVector Qlmj = Qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = Qlmi.operation().dot(Qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectRatio.increment(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                    if (aHalf) {
                        tConnectRatio.increment(idx);
                    }
                }
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
                
                // 统计近邻数
                tNN.increment(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) {
                    tNN.increment(idx);
                }
            });
        }
        // 除以近邻数得到比例
        tConnectRatio.div2this(tNN);
        
        // 计算完成归还缓存数据
        VectorCache.returnVec(tNN);
        ComplexMatrixCache.returnMat(Qlm);
        
        // 返回最终计算结果
        return tConnectRatio;
    }
    public IVector calConnectRatioBOOP(int aL, double aConnectThreshold, double aRNearest, int aNnn) {return calConnectRatioBOOP(aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectRatioBOOP(int aL, double aConnectThreshold, double aRNearest          ) {return calConnectRatioBOOP(aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectRatioBOOP(int aL, double aConnectThreshold                            ) {return calConnectRatioBOOP(aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的 BOOP 连接数目 */
    private IVector calConnectRatioBOOP_MPI_(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix Qlm = calYlmMean_MPI_(true, aMPIInfo, aL, aRNearestY, aNnnY);
        aMPIInfo.allgather(Qlm, aRNearestS); // 手动同步边界的数据用于计算 Sij
        
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnS<=0;
        // 统计连接数
        final Vector tConnectRatio = VectorCache.getZeros(mAtomNum);
        // 统计近邻数用于求平均
        final IVector tNN = VectorCache.getZeros(mAtomNum);
        
        // 注意需要先对 Qlm 归一化
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector Qlmi = Qlm.row(i);
            Qlmi.div2this(Qlmi.operation().norm());
        }
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize) : null;
        
        // 计算近邻上 Qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 统一获取行向量
            final IComplexVector Qlmi = Qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestS, aNnnS, aHalf, aMPIInfo::inRegin, idx -> {
                // 统一获取行向量
                IComplexVector Qlmj = Qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = Qlmi.operation().dot(Qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectRatio.increment(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                    boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                    if (tHalfStat) {
                        tConnectRatio.increment(idx);
                    }
                }
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
                
                // 统计近邻数
                tNN.increment(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) {
                    tNN.increment(idx);
                }
            });
        }
        // 除以近邻数得到比例
        tConnectRatio.div2this(tNN);
        
        // 计算完成归还缓存数据
        VectorCache.returnVec(tNN);
        ComplexMatrixCache.returnMat(Qlm);
        
        // 收集所有进程将统计到的连接数
        if (!aNoGather) aMPIInfo.allgather(tConnectRatio);
        
        // 返回最终计算结果
        return tConnectRatio;
    }
    private IVector calConnectRatioBOOP_MPI_(MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {return calConnectRatioBOOP_MPI_(aMPIInfo.mSize==1, aMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}
    public IVector calConnectRatioBOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectRatioBOOP_MPI_(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}}
    public IVector calConnectRatioBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectRatioBOOP_MPI_(tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}}
    public IVector calConnectRatioBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectRatioBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectRatioBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest          ) throws MPIException {return calConnectRatioBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectRatioBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold                            ) throws MPIException {return calConnectRatioBOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    public IVector calConnectRatioBOOP_MPI(                int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectRatioBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    public IVector calConnectRatioBOOP_MPI(                int aL, double aConnectThreshold, double aRNearest          ) throws MPIException {return calConnectRatioBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    public IVector calConnectRatioBOOP_MPI(                int aL, double aConnectThreshold                            ) throws MPIException {return calConnectRatioBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 通过 Averaged bond orientational order parameter（ql）来计算结构中每个原子的连接数占所有近邻数的比例值，
     * 输出结果为按照输入原子顺序排列的向量，数值为 0~1 的比例值；
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
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @param aRNearestS 用来计算 sij 的搜索的最近邻半径，会使用此值对应的近邻总数作为分母。默认为 aRNearestY
     * @param aNnnS 用来计算 sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数占所有近邻数的比例值组成的向量
     */
    public IVector calConnectRatioABOOP(int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix qlm = calQlmMean(aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);
        
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnS<=0;
        // 统计连接数，这里同样不去考虑减少重复代码
        final IVector tConnectRatio = VectorCache.getZeros(mAtomNum);
        // 统计近邻数用于求平均
        final IVector tNN = VectorCache.getZeros(mAtomNum);
        
        // 注意需要先对 qlm 归一化
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector qlmi = qlm.row(i);
            qlmi.div2this(qlmi.operation().norm());
        }
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf) : null;
        
        // 计算近邻上 qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) {
            // 统一获取行向量
            final IComplexVector qlmi = qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestS, aNnnS, aHalf, idx -> {
                // 统一获取行向量
                IComplexVector qlmj = qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = qlmi.operation().dot(qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectRatio.increment(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                    if (aHalf) {
                        tConnectRatio.increment(idx);
                    }
                }
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
                
                // 统计近邻数
                tNN.increment(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) {
                    tNN.increment(idx);
                }
            });
        }
        // 除以近邻数得到比例
        tConnectRatio.div2this(tNN);
        
        // 计算完成归还缓存数据
        VectorCache.returnVec(tNN);
        ComplexMatrixCache.returnMat(qlm);
        
        // 返回最终计算结果
        return tConnectRatio;
    }
    public IVector calConnectRatioABOOP(int aL, double aConnectThreshold, double aRNearest, int aNnn) {return calConnectRatioABOOP(aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectRatioABOOP(int aL, double aConnectThreshold, double aRNearest          ) {return calConnectRatioABOOP(aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectRatioABOOP(int aL, double aConnectThreshold                            ) {return calConnectRatioABOOP(aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    
    /** MPI 版本的 BOOP 连接数目 */
    private IVector calConnectRatioABOOP_MPI_(boolean aNoGather, MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        final IComplexMatrix qlm = calQlmMean_MPI_(true, aMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);
        aMPIInfo.allgather(qlm, aRNearestS); // 手动同步边界的数据用于计算 sij
        
        // 如果限制了 aNnn 需要关闭 half 遍历的优化
        final boolean aHalf = aNnnS<=0;
        // 统计连接数，这里同样不去考虑减少重复代码
        final Vector tConnectRatio = VectorCache.getZeros(mAtomNum);
        // 统计近邻数用于求平均
        final IVector tNN = VectorCache.getZeros(mAtomNum);
        
        // 注意需要先对 qlm 归一化
        for (int i = 0; i < mAtomNum; ++i) {
            IComplexVector qlmi = qlm.row(i);
            qlmi.div2this(qlmi.operation().norm());
        }
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntList @Nullable[] tNL = getValidBufferedNL_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize);
        // 如果为 null 还需要获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = tNL==null ? getNLWhichNeedBuffer_(aRNearestS, aNnnS, aHalf, aMPIInfo.mSize) : null;
        
        // 计算近邻上 qlm 的标量积，根据标量积来统计连接数
        for (int i = 0; i < mAtomNum; ++i) if (aMPIInfo.inRegin(i)) {
            // 统一获取行向量
            final IComplexVector qlmi = qlm.row(i);
            // 遍历近邻计算连接数
            final int fI = i;
            forEachNeighbor_(tNL, fI, aRNearestS, aNnnS, aHalf, aMPIInfo::inRegin, idx -> {
                // 统一获取行向量
                IComplexVector qlmj = qlm.row(idx);
                // 计算复向量的点乘
                ComplexDouble Sij = qlmi.operation().dot(qlmj);
                // 取模量来判断是否连接
                if (Sij.norm() > aConnectThreshold) {
                    tConnectRatio.increment(fI);
                    // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                    boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                    if (tHalfStat) {
                        tConnectRatio.increment(idx);
                    }
                }
                
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[fI].add(idx);}
                
                // 统计近邻数
                tNN.increment(fI);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                if (aHalf) {
                    tNN.increment(idx);
                }
            });
        }
        // 除以近邻数得到比例
        tConnectRatio.div2this(tNN);
        
        // 计算完成归还缓存数据
        VectorCache.returnVec(tNN);
        ComplexMatrixCache.returnMat(qlm);
        
        // 收集所有进程将统计到的连接数
        if (!aNoGather) aMPIInfo.allgather(tConnectRatio);
        
        // 返回最终计算结果
        return tConnectRatio;
    }
    private IVector calConnectRatioABOOP_MPI_(MPIInfo aMPIInfo, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {return calConnectRatioABOOP_MPI_(aMPIInfo.mSize==1, aMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}
    public IVector calConnectRatioABOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectRatioABOOP_MPI_(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}}
    public IVector calConnectRatioABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectRatioABOOP_MPI_(tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}}
    public IVector calConnectRatioABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectRatioABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn, aRNearest, aNnn);}
    public IVector calConnectRatioABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest          ) throws MPIException {return calConnectRatioABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    public IVector calConnectRatioABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold                            ) throws MPIException {return calConnectRatioABOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    public IVector calConnectRatioABOOP_MPI(                int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectRatioABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    public IVector calConnectRatioABOOP_MPI(                int aL, double aConnectThreshold, double aRNearest          ) throws MPIException {return calConnectRatioABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    public IVector calConnectRatioABOOP_MPI(                int aL, double aConnectThreshold                            ) throws MPIException {return calConnectRatioABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 具体通过 {@link #calConnectCountBOOP} 且 {@code l = 6} 来检测结构中类似固体的部分，
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
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量
     */
    public ILogicalVector checkSolidConnectCount6(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {IVector tConnectCount = calConnectCountBOOP(6, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectCount.greaterOrEqual(aSolidThreshold); VectorCache.returnVec(tConnectCount); return tIsSolid;}
    public ILogicalVector checkSolidConnectCount6(double aConnectThreshold, int aSolidThreshold, double aRNearest          ) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold, aRNearest, -1);}
    public ILogicalVector checkSolidConnectCount6(double aConnectThreshold, int aSolidThreshold                            ) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold, mUnitLen*R_NEAREST_MUL);}
    public ILogicalVector checkSolidConnectCount6(                                                                         ) {return checkSolidConnectCount6(0.5, 7);}
    
    /**@deprecated use {@link #checkSolidConnectCount6} */ @Deprecated public ILogicalVector checkSolidQ6(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold, aRNearest, aNnn);}
    /**@deprecated use {@link #checkSolidConnectCount6} */ @Deprecated public ILogicalVector checkSolidQ6(double aConnectThreshold, int aSolidThreshold, double aRNearest          ) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold, aRNearest);}
    /**@deprecated use {@link #checkSolidConnectCount6} */ @Deprecated public ILogicalVector checkSolidQ6(double aConnectThreshold, int aSolidThreshold                            ) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold);}
    /**@deprecated use {@link #checkSolidConnectCount6} */ @Deprecated public ILogicalVector checkSolidQ6(                                                                         ) {return checkSolidConnectCount6();}
    
    /**
     * 具体通过 {@link #calConnectRatioBOOP} 且 {@code l = 6} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.58
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量
     */
    public ILogicalVector checkSolidConnectRatio6(double aConnectThreshold, double aRNearest, int aNnn) {IVector tConnectRatio = calConnectRatioBOOP(6, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectRatio.greaterOrEqual(0.5); VectorCache.returnVec(tConnectRatio); return tIsSolid;}
    public ILogicalVector checkSolidConnectRatio6(double aConnectThreshold, double aRNearest          ) {return checkSolidConnectRatio6(aConnectThreshold, aRNearest, -1);}
    public ILogicalVector checkSolidConnectRatio6(double aConnectThreshold                            ) {return checkSolidConnectRatio6(aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    public ILogicalVector checkSolidConnectRatio6(                                                    ) {return checkSolidConnectRatio6(0.58);}
    
    @VisibleForTesting public ILogicalVector checkSolidS6(double aConnectThreshold, double aRNearest, int aNnn) {return checkSolidConnectRatio6(aConnectThreshold, aRNearest, aNnn);}
    @VisibleForTesting public ILogicalVector checkSolidS6(double aConnectThreshold, double aRNearest          ) {return checkSolidConnectRatio6(aConnectThreshold, aRNearest);}
    @VisibleForTesting public ILogicalVector checkSolidS6(double aConnectThreshold                            ) {return checkSolidConnectRatio6(aConnectThreshold);}
    @VisibleForTesting public ILogicalVector checkSolidS6(                                                    ) {return checkSolidConnectRatio6();}
    
    
    /**
     * 具体通过 {@link #calConnectCountBOOP} 且 {@code l = 4} 来检测结构中类似固体的部分，
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
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量
     */
    public ILogicalVector checkSolidConnectCount4(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {IVector tConnectCount = calConnectCountBOOP(4, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectCount.greaterOrEqual(aSolidThreshold); VectorCache.returnVec(tConnectCount); return tIsSolid;}
    public ILogicalVector checkSolidConnectCount4(double aConnectThreshold, int aSolidThreshold, double aRNearest          ) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold, aRNearest, -1);}
    public ILogicalVector checkSolidConnectCount4(double aConnectThreshold, int aSolidThreshold                            ) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold, mUnitLen*R_NEAREST_MUL);}
    public ILogicalVector checkSolidConnectCount4(                                                                         ) {return checkSolidConnectCount4(0.35, 6);}
    
    /**@deprecated use {@link #checkSolidConnectCount4} */ @Deprecated public ILogicalVector checkSolidQ4(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold, aRNearest, aNnn);}
    /**@deprecated use {@link #checkSolidConnectCount4} */ @Deprecated public ILogicalVector checkSolidQ4(double aConnectThreshold, int aSolidThreshold, double aRNearest          ) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold, aRNearest);}
    /**@deprecated use {@link #checkSolidConnectCount4} */ @Deprecated public ILogicalVector checkSolidQ4(double aConnectThreshold, int aSolidThreshold                            ) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold);}
    /**@deprecated use {@link #checkSolidConnectCount4} */ @Deprecated public ILogicalVector checkSolidQ4(                                                                         ) {return checkSolidConnectCount4();}
    
    /**
     * 具体通过 {@link #calConnectRatioBOOP} 且 {@code l = 4} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.50
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量
     */
    public ILogicalVector checkSolidConnectRatio4(double aConnectThreshold, double aRNearest, int aNnn) {IVector tConnectRatio = calConnectRatioBOOP(4, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectRatio.greaterOrEqual(0.5); VectorCache.returnVec(tConnectRatio); return tIsSolid;}
    public ILogicalVector checkSolidConnectRatio4(double aConnectThreshold, double aRNearest          ) {return checkSolidConnectRatio4(aConnectThreshold, aRNearest, -1);}
    public ILogicalVector checkSolidConnectRatio4(double aConnectThreshold                            ) {return checkSolidConnectRatio4(aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    public ILogicalVector checkSolidConnectRatio4(                                                    ) {return checkSolidConnectRatio4(0.50);}
    
    @VisibleForTesting public ILogicalVector checkSolidS4(double aConnectThreshold, double aRNearest, int aNnn) {return checkSolidConnectRatio4(aConnectThreshold, aRNearest, aNnn);}
    @VisibleForTesting public ILogicalVector checkSolidS4(double aConnectThreshold, double aRNearest          ) {return checkSolidConnectRatio4(aConnectThreshold, aRNearest);}
    @VisibleForTesting public ILogicalVector checkSolidS4(double aConnectThreshold                            ) {return checkSolidConnectRatio4(aConnectThreshold);}
    @VisibleForTesting public ILogicalVector checkSolidS4(                                                    ) {return checkSolidConnectRatio4();}
    
    
    /**
     * 一种基于 Chebyshev 多项式和球谐函数将原子局域环境展开成一个基组的方法，
     * 主要用于作为机器学习的输入向量；这是 NNAP 中默认使用的原子基组。
     * <p>
     * References:
     * <a href="https://arxiv.org/abs/2211.03350v3">
     * Computing the 3D Voronoi Diagram Robustly: An Easy Explanation </a>
     * <p>
     * @author Su Rui, liqa
     * @param aNMax Chebyshev 多项式选取的最大阶数
     * @param aLMax 球谐函数中 l 选取的最大阶数
     * @param aRCutOff 截断半径
     * @return 原子指纹矩阵组成的数组，n 为行，l 为列，因此 asVecRow 即为原本定义的基；如果存在超过一个种类则输出行数翻倍
     */
    public List<RowMatrix> calBasisNNAP(final int aNMax, final int aLMax, final double aRCutOff) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        if (aNMax < 0) throw new IllegalArgumentException("Input n_max MUST be Non-Negative, input: "+aNMax);
        if (aLMax < 0) throw new IllegalArgumentException("Input l_max MUST be Non-Negative, input: "+aLMax);
        
        final int tSizeN = mAtomTypeNum>1 ? aNMax+aNMax+2 : aNMax+1;
        final List<RowMatrix> rFingerPrints = MatrixCache.getMatRow(tSizeN, aLMax+1, mAtomNum);
        
        // 需要存储所有的 l，n，m 的值来统一进行近邻求和
        final IComplexVector[][] cnlmPar = new IComplexVector[threadNumber()][tSizeN];
        for (IComplexVector[] cnlm : cnlmPar) for (int tN = 0; tN < tSizeN; ++tN) {
            cnlm[tN] = ComplexVectorCache.getVec((aLMax+1)*(aLMax+1));
        }
        // 全局暂存 Y 的数组，这样可以用来防止重复获取来提高效率
        final List<? extends IComplexVector> tYPar = ComplexVectorCache.getVec((aLMax+1)*(aLMax+1), threadNumber());
        
        // 获取需要缓存的近邻列表
        final IntList @Nullable[] tNLToBuffer = getNLWhichNeedBuffer_(aRCutOff, -1, false);
        
        // 理论上只需要遍历一半从而加速这个过程，但由于实现较麻烦且占用过多内存（所有近邻的 Ylm, Rn, fc 都要存，会随着截断半径增加爆炸增涨），这里不考虑
        pool().parfor(mAtomNum, (i, threadID) -> {
            // 获取线程独立的变量
            final IComplexVector[] cnlm = cnlmPar[threadID];
            for (int tN = 0; tN < tSizeN; ++tN) {
                cnlm[tN].fill(0.0); // 需要手动置为 0，后面直接对近邻进行累加
            }
            final IComplexVector tY = tYPar.get(threadID);
            
            final XYZ cXYZ = new XYZ(mAtomDataXYZ.row(i));
            // 遍历近邻计算 Ylm, Rn, fc
            mNL.forEachNeighbor(i, aRCutOff, false, (x, y, z, idx, dis2) -> {
                double dis = Fast.sqrt(dis2);
                // 计算角度
                double dx = x - cXYZ.mX;
                double dy = y - cXYZ.mY;
                double dz = z - cXYZ.mZ;
                double theta = Fast.acos(dz / dis);
                double cosPhi = dx / Fast.hypot(dx, dy);
                // 注意避免 NaN 以及由于精度越界的情况
                if (Double.isNaN(cosPhi)) cosPhi = 1.0;
                cosPhi = Code.toRange(-1.0, 1.0, cosPhi);
                double phi = (dy > 0) ? Fast.acos(cosPhi) : (2.0*PI - Fast.acos(cosPhi));
                
                // 计算种类的权重
                int type = mTypeVec.get(idx);
                double wt = ((type&1)==1) ? type : -type;
                // 计算截断函数 fc
                double fc = dis>=aRCutOff ? 0.0 : Fast.powFast(1.0 - Fast.pow2(dis/aRCutOff), 4);
                // 统一遍历一次计算 Rn
                final double tX = 1.0 - 2.0*dis/aRCutOff;
                IVector Rn = Vectors.from(aNMax+1, n -> Func.chebyshev(n, tX));
                
                // 遍历求 n，l 的情况
                Func.sphericalHarmonicsFull2Dest(aLMax, theta, phi, tY);
                for (int tN = 0; tN <= aNMax; ++tN) {
                    // 现在提供了 mplus2this 支持将数乘到 tY 中后再加到 cijm，可以不用中间变量；
                    // 虽然看起来和使用 operate2this 效率基本一致，即使后者理论上应该还会创建一些 DoubleComplex；
                    // 总之至少没有反向优化，并且这样包装后更加不吃编译器的优化，也不存在一大坨 lambda 表达式，以及传入的 DoubleComplex 一定不是引用等这种约定
                    double tMul = fc * Rn.get(tN);
                    cnlm[tN].operation().mplus2this(tY, tMul);
                    if (mAtomTypeNum > 1) cnlm[tN+aNMax+1].operation().mplus2this(tY, wt*tMul);
                }
                // 统计近邻
                if (tNLToBuffer != null) {tNLToBuffer[i].add(idx);}
            });
            // 做标量积消去 m 项，得到此原子的 FP
            IMatrix tFP = rFingerPrints.get(i);
            for (int tN = 0; tN < tSizeN; ++tN) for (int tL = 0; tL <= aLMax; ++tL) {
                // 根据 sphericalHarmonicsFull2Dest 的约定这里需要这样索引
                int tStart = tL*tL;
                int tLen = tL+tL+1;
                tFP.set(tN, tL, (4.0*PI/(double)tLen) * cnlm[tN].subVec(tStart, tStart+tLen).operation().dot());
            }
        });
        
        // 归还临时变量
        for (IComplexVector[] cnlm : cnlmPar) for (IComplexVector cilm : cnlm) {
            ComplexVectorCache.returnVec(cilm);
        }
        ComplexVectorCache.returnVec(tYPar);
        
        return rFingerPrints;
    }
}
