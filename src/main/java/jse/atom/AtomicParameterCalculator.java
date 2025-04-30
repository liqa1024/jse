package jse.atom;

import com.google.common.collect.Lists;
import jse.cache.*;
import jse.code.CS;
import jse.code.UT;
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
 * 原子参数计算器，目前包含了 jse 实现的所有参数计算功能；
 * 包括计算径向分布函数 {@link #calRDF}，结构因子函数
 * {@link #calSF}，键角序参量 {@link #calBOOP}/{@link #calABOOP}
 * 以及近邻列表获取 {@link #getNeighborList}/{@link #getFullNeighborList}。
 * <p>
 * 其余附加的功能会通过
 * <a href="https://blog.mrhaki.com/2013/01/groovy-goodness-adding-extra-methods.html">
 * Groovy Extension Modules </a>
 * 的方式来添加，例如 voronoi 分析 {@link jsex.voronoi.VoronoiExtensions#calVoronoi}，
 * nnap 基组的计算 {@link jsex.nnap.NNAPExtensions#calBasisNNAP} (实际 groovy
 * 中使用和其他参量函数一致)
 * <p>
 * 一般来说，直接使用 {@link #of(IAtomData)}
 * 来通过一个原子数据来创建一个的参数计算器，然后调用相关方法来进行计算：
 * <pre> {@code
 * def apc = APC.of(data)
 * def gr = apc.calRDF()
 * apc.shutdown()
 * } </pre>
 * 由此来计算此原子数据的 rdf，最终调用 {@link #shutdown()}
 * 来手动释放内部缓存资源以及线程池（现在不再强制要求调用，但在高频使用下手动释放可以提高性能）
 * <p>
 * 也可以通过 {@link #withOf(IAtomData, IUnaryFullOperator)}
 * 来创建一个自动关闭的 apc 并直接获取计算结果：
 * <pre> {@code
 * def gr = APC.withOf(data) {it.calRDF()}
 * } </pre>
 * 来直接计算 rdf 并自动关闭参数计算器
 * <p>
 * 此类线程不安全（主要由于近邻列表缓存），但不同实例之间线程安全
 *
 * @see IAtomData IAtomData: 关于 jse 中原子数据的实现和定义
 * @see APC APC: AtomicParameterCalculator 的简称
 * @see NeighborListGetter NeighborListGetter: jse 目前的近邻列表实现
 * @author liqa
 */
public class AtomicParameterCalculator extends AbstractThreadPool<ParforThreadPool> {
    /** 缓存近邻列表从而避免重复计算距离，这里设置缓存的大小，设置为 0 关闭缓存；即使现在优化了近邻列表获取，缓存依旧能大幅加速近邻遍历 */
    public static int BUFFER_NL_NUM = 8;
    /** 最大的缓存近邻截断半径关于单位距离的倍率，过高的值的近邻列表缓存对内存是个灾难 */
    public static double BUFFER_NL_RMAX = 4.0;
    
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
    private final Thread mInitThread;
    
    /// IThreadPoolContainer stuffs
    private volatile boolean mDead = false;
    /** 关闭这个参数计算器，现在不再强制要求手动关闭，但是手动调用可以提高性能 */
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true; super.shutdown();
        mNL.shutdown(); // 内部保证执行后内部的 mAtomDataXYZ 已经置为 null
        // 此时 APC 关闭，归还 mAtomDataXYZ，这种写法保证永远能获取到 mAtomDataXYZ 时都是合法的
        // 只有相同线程关闭才会归还
        Thread tThread = Thread.currentThread();
        if (tThread == mInitThread) {
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
            UT.Code.warning("Thread of shutdown() and init should be SAME in AtomicParameterCalculator");
        }
        if (tThread == mInitBufferNLThread) {
            if (mBufferedNL != null) {
                BufferedNL oBufferedNL = mBufferedNL;
                mBufferedNL = null;
                sBufferedNLCache.returnObject(oBufferedNL);
            }
        }
    }
    /** 立刻关闭参数计算器，这里和 {@link #shutdown()} 行为一致 */
    @Override public void shutdownNow() {shutdown();}
    /** @return 是否调用了关闭 */
    @Override public boolean isShutdown() {return mDead;}
    /** @return 是否真的完全结束了，这里和 {@link #isShutdown()} 行为一致 */
    @Override public boolean isTerminated() {return mDead;}
    /** {@link AutoCloseable} 实现，除了调用 {@link #shutdown()} 关闭还需要等待完全关闭，这里和 {@link #shutdown()} 行为一致 */
    @ApiStatus.Internal @Override public void close() {shutdown();}
    
    /** @deprecated use {@link #of(IAtomData)} */ @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
    AtomicParameterCalculator(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {
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
        mInitThread = Thread.currentThread();
    }
    /** @deprecated use {@link #of(IAtomData)} */ @SuppressWarnings("DeprecatedIsStillUsed") @Deprecated
    AtomicParameterCalculator(IAtomData aAtomData) {this(aAtomData, 1);}
    
    
    /**
     * 根据输入数据直接创建 APC
     * @param aAtomData 原子数据，会遍历读取原子数据进行值拷贝
     * @param aThreadNum APC 进行计算会使用的线程数，默认为 {@code 1}
     */
    public static AtomicParameterCalculator of(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return new AtomicParameterCalculator(aAtomData, aThreadNum);}
    /**
     * 根据输入数据直接创建 APC
     * @param aAtomData 原子数据，会遍历读取原子数据进行值拷贝
     */
    public static AtomicParameterCalculator of(IAtomData aAtomData) {return new AtomicParameterCalculator(aAtomData);}
    
    /**
     * 自动关闭的接口，例如通过：
     * <pre> {@code
     * def gr = APC.withOf(data) {it.calRDF()}
     * } </pre>
     * 来直接计算 rdf 并计算完成后自动关闭
     * @param aAtomData 原子数据，会遍历读取原子数据进行值拷贝
     * @param aThreadNum APC 进行计算会使用的线程数，默认为 {@code 1}
     * @param aDoLater 需要使用 APC 进行的相关操作，返回计算结果
     * @return {@code aDoLater} 输出的计算结果
     */
    public static <T> T withOf(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, IUnaryFullOperator<? extends T, ? super AtomicParameterCalculator> aDoLater) {try (AtomicParameterCalculator tAPC = new AtomicParameterCalculator(aAtomData, aThreadNum)) {return aDoLater.apply(tAPC);}}
    /**
     * 自动关闭的接口，例如通过：
     * <pre> {@code
     * def gr = APC.withOf(data) {it.calRDF()}
     * } </pre>
     * 来直接计算 rdf 并计算完成后自动关闭
     * @param aAtomData 原子数据，会遍历读取原子数据进行值拷贝
     * @param aDoLater 需要使用 APC 进行的相关操作，返回计算结果
     * @return {@code aDoLater} 输出的计算结果
     */
    public static <T> T withOf(IAtomData aAtomData, IUnaryFullOperator<? extends T, ? super AtomicParameterCalculator> aDoLater) {try (AtomicParameterCalculator tAPC = new AtomicParameterCalculator(aAtomData)) {return aDoLater.apply(tAPC);}}
    
    
    /// 内部使用方法，用来将 aAtomDataXYZ 转换成内部存储的格式，并且处理精度问题造成的超出边界问题
    private static void setValidXYZ_(IBox aBox, IMatrix rXYZMat, double aX, double aY, double aZ, int aRow, @NotNull XYZ rBuf) {
        rBuf.setXYZ(aX, aY, aZ);
        aBox.wrapPBC(rBuf);
        rXYZMat.set(aRow, 0, rBuf.mX);
        rXYZMat.set(aRow, 1, rBuf.mY);
        rXYZMat.set(aRow, 2, rBuf.mZ);
    }
    private void setValidXYZ_(IMatrix rXYZMat, IXYZ aXYZ, int aRow, @NotNull XYZ rBuf) {
        setValidXYZ_(mBox, rXYZMat, aXYZ.x(), aXYZ.y(), aXYZ.z(), aRow, rBuf);
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
    public AtomicParameterCalculator setThreadNumber(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNum)  {if (aThreadNum != threadNumber()) setPool(new ParforThreadPool(aThreadNum)); return this;}
    
    
    /// 获取信息
    /**
     * @return 原子数目信息
     * @see IAtomData#atomNumber()
     */
    public int atomNumber() {return mAtomNum;}
    /** @deprecated use {@link #atomNumber()} or {@link #natoms()} */
    @Deprecated public final int atomNum() {return atomNumber();}
    /** @see #atomNumber() */
    public final int natoms() {return atomNumber();}
    /**
     * 指定种类的原子数
     * @param aType 种类编号，从 1 开始
     * @return 原子数目信息值
     */
    public int atomNumber(int aType) {return mAtomNumType.get(aType-1);}
    /** @deprecated use {@link #atomNumber(int)} or {@link #natoms(int)} */
    @Deprecated public final int atomNum(int aType) {return atomNumber(aType);}
    /** @see #atomNumber(int) */
    public final int natoms(int aType) {return atomNumber(aType);}
    
    /**
     * @return 原子种类数目
     * @see IAtomData#atomTypeNumber()
     */
    public int atomTypeNumber() {return mAtomTypeNum;}
    /** @deprecated use {@link #atomTypeNumber()} or {@link #ntypes()} */
    @Deprecated public final int atomTypeNum() {return atomTypeNumber();}
    /** @see #atomTypeNumber() */
    public final int ntypes() {return atomTypeNumber();}
    
    /**
     * 单位长度，平均单个原子的距离，即 {@code cbrt(volume()/natoms())}
     * @return 单位长度值
     * @see Math#cbrt(double)
     */
    public double unitLen() {return mUnitLen;}
    /**
     * 原子数据的体积
     * @return 体积值
     * @see IAtomData#volume()
     */
    public double volume() {return mVolume;}
    
    /**
     * 原子数密度，即 {@code natoms()/volume()}
     * @return 原子数密度值
     */
    public double rho() {return mRho;}
    /** @deprecated use {@link #rho()} */
    @Deprecated public final double rou() {return rho();}
    /**
     * 指定种类的原子数密度，即 {@code natoms(aType)/volume()}
     * @param aType 种类编号，从 1 开始
     * @return 原子数密度值
     */
    public double rho(int aType) {return mAtomNumType.get(aType-1) / mVolume;}
    /** @deprecated use {@link #rho(int)} */
    @Deprecated public final double rou(int aType) {return rho(aType);}
    /**
     * 两个种类的混合原子数密度，即 {@code sqrt(rho(aTypeA)*rho(aTypeB))}
     * @param aTypeA 第一个种类的编号，从 1 开始
     * @param aTypeB 第二个种类的编号，从 1 开始
     * @return 混合原子数密度值
     */
    public double birho(int aTypeA, int aTypeB) {return Fast.sqrt(rho(aTypeA)*rho(aTypeB));}
    /** @deprecated use {@link #birho(int, int)} */
    public final double birou(int aTypeA, int aTypeB) {return birho(aTypeA, aTypeB);}
    /**
     * 和另一个 APC 的混合原子数密度，即 {@code sqrt(rho()*aAPC.rho())}
     * @param aAPC 另一个参数计算器
     * @return 混合原子数密度值
     */
    public double birho(AtomicParameterCalculator aAPC) {return Fast.sqrt(mRho*aAPC.mRho);}
    /** @deprecated use {@link #birho(AtomicParameterCalculator)} */
    public final double birou(AtomicParameterCalculator aAPC) {return birho(aAPC);}

    /// 现在支持合法修改 APC 中的原子位置和种类
    /**
     * 修改指定索引原子的坐标位置，会同步更新内部的近邻列表
     * <p>
     * 相比重新创建一个 APC，这种小的变化可以大大提高性能
     *
     * @param aIdx 需要修改的原子索引值
     * @param aXYZ 需要设置的新的 xyz 坐标
     * @return 自身方便链式调用
     * @see IXYZ
     */
    public AtomicParameterCalculator setAtomXYZ(int aIdx, IXYZ aXYZ) {return setAtomXYZ(aIdx, aXYZ.x(), aXYZ.y(), aXYZ.z());}
    /**
     * 修改指定索引原子的坐标位置，会同步更新内部的近邻列表
     * <p>
     * 相比重新创建一个 APC，这种小的变化可以大大提高性能
     *
     * @param aIdx 需要修改的原子索引值
     * @param aX 需要设置的新的 x 坐标
     * @param aY 需要设置的新的 y 坐标
     * @param aZ 需要设置的新的 z 坐标
     * @return 自身方便链式调用
     */
    public AtomicParameterCalculator setAtomXYZ(int aIdx, double aX, double aY, double aZ) {
        double oX = mAtomDataXYZ.get(aIdx, 0);
        double oY = mAtomDataXYZ.get(aIdx, 1);
        double oZ = mAtomDataXYZ.get(aIdx, 2);
        XYZ tBuf = new XYZ();
        setValidXYZ_(mBox, mAtomDataXYZ, aX, aY, aZ, aIdx, tBuf);
        mNL.updateAtomXYZ_(aIdx, oX, oY, oZ, tBuf);
        // 这里简单处理，直接清空缓存列表
        if (mBufferedNL != null) mBufferedNL.reset();
        return this;
    }
    /**
     * 修改指定索引原子的元素种类，这里只会更新内部的原子数计数
     * <p>
     * 相比重新创建一个 APC，这种小的变化可以大大提高性能
     *
     * @param aIdx 需要修改的原子索引值
     * @param aType 需要设置的新的种类编号
     * @return 自身方便链式调用
     */
    public AtomicParameterCalculator setAtomType(int aIdx, int aType) {
        // 简单更新
        if (aType > mAtomTypeNum) throw new IllegalArgumentException("input type ("+aType+") Must <= ntypes ("+mAtomTypeNum+")");
        mAtomNumType.decrement(mTypeVec.get(aIdx)-1);
        mTypeVec.set(aIdx, aType);
        mAtomNumType.increment(aType-1);
        return this;
    }
    /// 补充运算时使用
    /**
     * 外部为 APC 补充运算时使用，获取 APC 内部的并行线程池
     * @see ParforThreadPool
     */
    @ApiStatus.Internal public ParforThreadPool pool_() {return pool();}
    /**
     * 外部为 APC 补充运算时使用，获取 APC 内部的近邻列表获取器
     * @see NeighborListGetter
     */
    @ApiStatus.Internal public NeighborListGetter nl_() {return mNL;}
    /**
     * 外部为 APC 补充运算时使用，获取 APC 内部的原子坐标数据矩阵
     * @see IMatrix
     */
    @ApiStatus.Internal public IMatrix atomDataXYZ_() {return mAtomDataXYZ;}
    /**
     * 外部为 APC 补充运算时使用，获取 APC 内部的原子种类编号向量
     * @see IIntVector
     */
    @ApiStatus.Internal public IIntVector atomType_() {return mTypeVec;}
    
    
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
            mNL.forEachNeighbor(i, aRMax - dr*0.5, true, (dx, dy, dz, idx) -> {
                dn.updateNear(Fast.hypot(dx, dy, dz), g->g+1);
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
    /**
     * @return {@code calRDF(aN, unitLen()*6)}
     * @see #calRDF(int, double)
     * @see #unitLen()
     */
    public IFunc1 calRDF(int aN) {return calRDF(aN, mUnitLen*6);}
    /**
     * @return {@code calRDF(160, unitLen()*6)}
     * @see #calRDF(int, double)
     * @see #unitLen()
     */
    public IFunc1 calRDF() {return calRDF(160);}
    
    
    
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
                mNL.forEachNeighbor(i, aRMax - dr*0.5, true, (dx, dy, dz, idx) -> {
                    if (mTypeVec.get(idx) == tTypeJ) {
                        dn.updateNear(Fast.hypot(dx, dy, dz), g->g+1);
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
    /**
     * @return {@code calRDF_AB(aTypeA, aTypeB, aN, unitLen()*6)}
     * @see #calRDF_AB(int, int, int, double)
     * @see #unitLen()
     */
    public IFunc1 calRDF_AB(int aTypeA, int aTypeB, int aN) {return calRDF_AB(aTypeA, aTypeB, aN, mUnitLen*6);}
    /**
     * @return {@code calRDF_AB(aTypeA, aTypeB, 160, unitLen()*6)}
     * @see #calRDF_AB(int, int, int, double)
     * @see #unitLen()
     */
    public IFunc1 calRDF_AB(int aTypeA, int aTypeB) {return calRDF_AB(aTypeA, aTypeB, 160);}
    
    
    /**
     * 计算所有种类之间的 RDF (radial distribution function，即 g(r))，
     * 只计算一个固定结构的值，因此不包含温度信息。
     * <p>
     * 所有的 {@code g_AB(r)} 直接排列成一个列表，
     * 先遍历 {@code typeB} 后遍历 {@code typeA}，且保持
     * {@code typeB <= typeA}，也就是说，如果需要访问给定 {@code (typeA, typeB)}
     * 的 {@code g_AB(r)}，需要使用：
     * <pre> {@code
     * def grAll = apc.calAllRDF(...)
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
            mNL.forEachNeighbor(i, aRMax - dr*0.5, true, (dx, dy, dz, idx) -> {
                double dis = Fast.hypot(dx, dy, dz);
                dnAll[0].updateNear(dis, g->g+1);
                int tTypeB = mTypeVec.get(idx);
                int tIdx = tTypeB<=tTypeA ? ((tTypeA*(tTypeA-1))/2 + tTypeB) : ((tTypeB*(tTypeB-1))/2 + tTypeA);
                dnAll[tIdx].updateNear(dis, g->g+1);
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
    /**
     * @return {@code calAllRDF(aN, unitLen()*6)}
     * @see #calAllRDF(int, double)
     * @see #unitLen()
     */
    public List<? extends IFunc1> calAllRDF(int aN) {return calAllRDF(aN, mUnitLen*6);}
    /**
     * @return {@code calAllRDF(160, unitLen()*6)}
     * @see #calAllRDF(int, double)
     * @see #unitLen()
     */
    public List<? extends IFunc1> calAllRDF() {return calAllRDF(160);}
    
    
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
            mNL.forEachNeighbor(i, aRMax+tRShift, true, (dx, dy, dz, idx) -> {
                tDeltaG.setX0(Fast.hypot(dx, dy, dz));
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
    /**
     * @return {@code calRDF_G(aN, aRMax, 4)}
     * @see #calRDF_G(int, double, int)
     */
    public IFunc1 calRDF_G(int aN, double aRMax) {return calRDF_G(aN, aRMax, 4);}
    /**
     * @return {@code calRDF_G(aN, unitLen()*6, 4)}
     * @see #calRDF_G(int, double, int)
     * @see #unitLen()
     */
    public IFunc1 calRDF_G(int aN) {return calRDF_G(aN, mUnitLen*6);}
    /**
     * @return {@code calRDF_G(1000, unitLen()*6, 4)}
     * @see #calRDF_G(int, double, int)
     * @see #unitLen()
     */
    public IFunc1 calRDF_G() {return calRDF_G(1000);}
    
    
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
                mNL.forEachNeighbor(i, aRMax+tRShift, true, (dx, dy, dz, idx) -> {
                    if (mTypeVec.get(idx) == tTypeJ) {
                        tDeltaG.setX0(Fast.hypot(dx, dy, dz));
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
    /**
     * @return {@code calRDF_AB_G(aTypeA, aTypeB, aN, aRMax, 4)}
     * @see #calRDF_AB_G(int, int, int, double, int)
     */
    public IFunc1 calRDF_AB_G(int aTypeA, int aTypeB, int aN, double aRMax) {return calRDF_AB_G(aTypeA, aTypeB, aN, aRMax, 4);}
    /**
     * @return {@code calRDF_AB_G(aTypeA, aTypeB, aN, unitLen()*6, 4)}
     * @see #calRDF_AB_G(int, int, int, double, int)
     * @see #unitLen()
     */
    public IFunc1 calRDF_AB_G(int aTypeA, int aTypeB, int aN) {return calRDF_AB_G(aTypeA, aTypeB, aN, mUnitLen*6);}
    /**
     * @return {@code calRDF_AB_G(aTypeA, aTypeB, 1000, unitLen()*6, 4)}
     * @see #calRDF_AB_G(int, int, int, double, int)
     * @see #unitLen()
     */
    public IFunc1 calRDF_AB_G(int aTypeA, int aTypeB) {return calRDF_AB_G(aTypeA, aTypeB, 160);}
    
    
    /**
     * 使用带有一定展宽的高斯分布代替直接计数来计算所有种类之间的 RDF，
     * 只计算一个固定结构的值，因此不包含温度信息。
     * <p>
     * 所有的 {@code g_AB(r)} 直接排列成一个列表，
     * 先遍历 {@code typeB} 后遍历 {@code typeA}，且保持
     * {@code typeB <= typeA}，也就是说，如果需要访问给定 {@code (typeA, typeB)}
     * 的 {@code g_AB(r)}，需要使用：
     * <pre> {@code
     * def grAll = apc.calAllRDF_G(...)
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
            mNL.forEachNeighbor(i, aRMax+tRShift, true, (dx, dy, dz, idx) -> {
                tDeltaG.setX0(Fast.hypot(dx, dy, dz));
                dnAll[0].plus2this(tDeltaG);
                int tTypeB = mTypeVec.get(idx);
                int tIdx = tTypeB<=tTypeA ? ((tTypeA*(tTypeA-1))/2 + tTypeB) : ((tTypeB*(tTypeB-1))/2 + tTypeA);
                dnAll[tIdx].plus2this(tDeltaG);
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
    /**
     * @return {@code calAllRDF_G(aN, aRMax, 4)}
     * @see #calAllRDF_G(int, double, int)
     */
    public List<? extends IFunc1> calAllRDF_G(int aN, double aRMax) {return calAllRDF_G(aN, aRMax, 4);}
    /**
     * @return {@code calAllRDF_G(aN, unitLen()*6, 4)}
     * @see #calAllRDF_G(int, double, int)
     * @see #unitLen()
     */
    public List<? extends IFunc1> calAllRDF_G(int aN) {return calAllRDF_G(aN, mUnitLen*6);}
    /**
     * @return {@code calAllRDF_G(1000, unitLen()*6, 4)}
     * @see #calAllRDF_G(int, double, int)
     * @see #unitLen()
     */
    public List<? extends IFunc1> calAllRDF_G() {return calAllRDF_G(160);}
    
    
    
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
        
        // 输出
        return Sq;
    }
    /**
     * @return {@code calSF(aN, aQMax, 2.0*PI/unitLen() * 0.6)}
     * @see #calSF(int, double, double)
     * @see #unitLen()
     */
    public IFunc1 calSF(int aN, double aQMax) {return calSF(aN, aQMax, 2.0*PI/mUnitLen * 0.6);}
    /**
     * @return {@code calSF(aN, 2.0*PI/unitLen() * 6.0, 2.0*PI/unitLen() * 0.6)}
     * @see #calSF(int, double, double)
     * @see #unitLen()
     */
    public IFunc1 calSF(int aN) {return calSF(aN, 2.0*PI/mUnitLen * 6.0);}
    /**
     * @return {@code calSF(160, 2.0*PI/unitLen() * 6.0, 2.0*PI/unitLen() * 0.6)}
     * @see #calSF(int, double, double)
     * @see #unitLen()
     */
    public IFunc1 calSF() {return calSF(160);}
    
    
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
        
        // 输出
        return Sq;
    }
    /**
     * @return {@code calSF_AB(aTypeA, aTypeB, aN, aQMax, 2.0*PI/unitLen() * 0.6)}
     * @see #calSF_AB(int, int, int, double, double)
     * @see #unitLen()
     */
    public IFunc1 calSF_AB(int aTypeA, int aTypeB, int aN, double aQMax) {return calSF_AB(aTypeA, aTypeB, aN, aQMax, 2.0*PI/mUnitLen * 0.6);}
    /**
     * @return {@code calSF_AB(aTypeA, aTypeB, aN, 2.0*PI/unitLen() * 6.0, 2.0*PI/unitLen() * 0.6)}
     * @see #calSF_AB(int, int, int, double, double)
     * @see #unitLen()
     */
    public IFunc1 calSF_AB(int aTypeA, int aTypeB, int aN) {return calSF_AB(aTypeA, aTypeB, aN, 2.0*PI/mUnitLen * 6);}
    /**
     * @return {@code calSF_AB(aTypeA, aTypeB, 160, 2.0*PI/unitLen() * 6.0, 2.0*PI/unitLen() * 0.6)}
     * @see #calSF_AB(int, int, int, double, double)
     * @see #unitLen()
     */
    public IFunc1 calSF_AB(int aTypeA, int aTypeB) {return calSF_AB(aTypeA, aTypeB, 160);}
    
    
    /**
     * 计算所有种类之间的 SF（structural factor，即 S(q)），
     * 只计算一个固定结构的值，因此不包含温度信息。
     * <p>
     * 所有的 {@code S_AB(q)} 直接排列成一个列表，
     * 先遍历 {@code typeB} 后遍历 {@code typeA}，且保持
     * {@code typeB <= typeA}，也就是说，如果需要访问给定 {@code (typeA, typeB)}
     * 的 {@code S_AB(q)}，需要使用：
     * <pre> {@code
     * def SqAll = apc.calAllSF(...)
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
        
        // 输出
        return AbstractCollections.from(SqAll);
    }
    /**
     * @return {@code calAllSF(aN, aQMax, 2.0*PI/unitLen() * 0.6)}
     * @see #calAllSF(int, double, double)
     * @see #unitLen()
     */
    public List<? extends IFunc1> calAllSF(int aN, double aQMax) {return calAllSF(aN, aQMax, 2.0*PI/mUnitLen * 0.6);}
    /**
     * @return {@code calAllSF(aN, 2.0*PI/unitLen() * 6.0, 2.0*PI/unitLen() * 0.6)}
     * @see #calAllSF(int, double, double)
     * @see #unitLen()
     */
    public List<? extends IFunc1> calAllSF(int aN) {return calAllSF(aN, 2.0*PI/mUnitLen * 6);}
    /**
     * @return {@code calAllSF(160, 2.0*PI/unitLen() * 6.0, 2.0*PI/unitLen() * 0.6)}
     * @see #calAllSF(int, double, double)
     * @see #unitLen()
     */
    public List<? extends IFunc1> calAllSF() {return calAllSF(160);}
    
    
    
    /// gr 和 Sq 的相互转换，由于依旧需要体系的原子数密度，因此还是都移动到 APC 中
    /**
     * 转换 g(r) 到 S(q)，这是主要计算 S(q) 的方法
     * <p>
     * 当提供密度值 aRho 时，此方法是静态方法，可以直接通过类名使用；
     * 当没有提供密度值 aRho 时，则需要通过 {@link #rho()}
     * 来获取密度内部，因此此时为成员方法，需要通过实例的对象使用
     *
     * @author liqa
     * @param aGr the matrix form of g(r)
     * @param aRho the atom number density（默认会选择本 APC 得到的密度）
     * @param aN the split number of output（默认为 160）
     * @param aQMax the max q of output S(q)（默认为 7.6 倍 gr 第一峰对应的距离）
     * @param aQMin the min q of output S(q)（默认为 0.5 倍 gr 第一峰对应的距离）
     * @return the structural factor, S(q)
     * @see #rho()
     * @see #birho(int, int)
     */
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRho, int aN, double aQMax, double aQMin) {
        double dq = (aQMax-aQMin)/aN;
        
        IFunc1 Sq = FixBoundFunc1.zeros(aQMin, dq, aN+1).setBound(0.0, 1.0);
        Sq.fill(aGr.operation().refConvolveFull((gr, r, q) -> (r * (gr-1.0) * Fast.sin(q*r) / q)));
        Sq.multiply2this(4.0*PI*aRho);
        Sq.plus2this(1.0);
        
        return Sq;
    }
    /**
     * @return {@code RDF2SF(aGr, aRho, aN, aQMax, 2.0*PI/aGr.opt().maxX() * 0.5)}
     * @see #RDF2SF(IFunc1, double, int, double, double)
     */
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRho, int aN, double aQMax) {return RDF2SF(aGr, aRho, aN, aQMax, 2.0*PI/aGr.operation().maxX() * 0.5);}
    /**
     * @return {@code RDF2SF(aGr, aRho, aN, 2.0*PI/aGr.opt().maxX()* 7.6, 2.0*PI/aGr.opt().maxX() * 0.5)}
     * @see #RDF2SF(IFunc1, double, int, double, double)
     */
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRho, int aN) {return RDF2SF(aGr, aRho, aN, 2.0*PI/aGr.operation().maxX()* 7.6, 2.0*PI/aGr.operation().maxX() * 0.5);}
    /**
     * @return {@code RDF2SF(aGr, aRho, 160)}
     * @see #RDF2SF(IFunc1, double, int, double, double)
     */
    public static IFunc1 RDF2SF(IFunc1 aGr, double aRho) {return RDF2SF(aGr, aRho, 160);}
    /**
     * @return {@code RDF2SF(aGr, rho())}
     * @see #RDF2SF(IFunc1, double, int, double, double)
     */
    public IFunc1 RDF2SF(IFunc1 aGr) {return RDF2SF(aGr, mRho);}
    
    
    /**
     * 转换 S(q) 到 g(r)
     * <p>
     * 当提供密度值 aRho 时，此方法是静态方法，可以直接通过类名使用；
     * 当没有提供密度值 aRho 时，则需要通过 {@link #rho()}
     * 来获取密度内部，因此此时为成员方法，需要通过实例的对象使用
     *
     * @author liqa
     * @param aSq the matrix form of S(q)
     * @param aRho the atom number density（默认会选择本 APC 得到的密度）
     * @param aN the split number of output（默认为 160）
     * @param aRMax the max r of output g(r)（默认为 7.6 倍 Sq 第一峰对应的距离）
     * @param aRMin the min r of output g(r)（默认为 0.5 倍 Sq 第一峰对应的距离）
     * @return the radial distribution function, g(r)
     * @see #rho()
     * @see #birho(int, int)
     */
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRho, int aN, double aRMax, double aRMin) {
        double dr = (aRMax-aRMin)/aN;
        
        IFunc1 gr = FixBoundFunc1.zeros(aRMin, dr, aN+1).setBound(0.0, 1.0);
        gr.fill(aSq.operation().refConvolveFull((Sq, q, r) -> (q * (Sq-1.0) * Fast.sin(q*r) / r)));
        gr.multiply2this(1.0/(2.0*PI*PI*aRho));
        gr.plus2this(1.0);
        
        return gr;
    }
    /**
     * @return {@code SF2RDF(aSq, aRho, aN, aRMax, 2.0*PI/aSq.opt().maxX() * 0.5)}
     * @see #SF2RDF(IFunc1, double, int, double, double)
     */
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRho, int aN, double aRMax) {return SF2RDF(aSq, aRho, aN, aRMax, 2.0*PI/aSq.operation().maxX() * 0.5);}
    /**
     * @return {@code SF2RDF(aSq, aRho, aN, 2.0*PI/aSq.opt().maxX() * 7.6, 2.0*PI/aSq.opt().maxX() * 0.5)}
     * @see #SF2RDF(IFunc1, double, int, double, double)
     */
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRho, int aN) {return SF2RDF(aSq, aRho, aN, 2.0*PI/aSq.operation().maxX() * 7.6, 2.0*PI/aSq.operation().maxX() * 0.5);}
    /**
     * @return {@code SF2RDF(aSq, aRho, 160)}
     * @see #SF2RDF(IFunc1, double, int, double, double)
     */
    public static IFunc1 SF2RDF(IFunc1 aSq, double aRho) {return SF2RDF(aSq, aRho, 160);}
    /**
     * @return {@code SF2RDF(aSq, rho())}
     * @see #SF2RDF(IFunc1, double, int, double, double)
     */
    public IFunc1 SF2RDF(IFunc1 aSq) {return SF2RDF(aSq, mRho);}
    
    
    
    /// 直接获取近邻列表的 api，不包括自身
    /**
     * 获取给定索引原子的近邻原子索引组成的列表，不包括自身
     * <p>
     * 返回的近邻列表会经过值拷贝，因此可以直接修改不会影响
     * APC 内部的工作
     * <p>
     * 如果需要近邻原子的坐标需要使用
     * {@link #getFullNeighborList(int, double, int)}
     *
     * @author liqa
     * @param aIdx 需要获取近邻列表的原子索引
     * @param aRMax 近邻的最大截断半径
     * @param aNnn 需要的最近的近邻原子数目
     * @return 近邻原子索引组成的向量，不包括自身
     * @see IntVector
     */
    public IntVector getNeighborList(int aIdx, double aRMax, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 获取缓存近邻列表，这里只需要进行遍历 idx
        IntList @Nullable[] tNL = getValidBufferedNL_(aRMax, aNnn, false);
        if (tNL != null) return tNL[aIdx].copy2vec();
        
        // 如果为 null 则直接遍历指定 idx，如果需要重复使用则直接在外部缓存即可
        final IntVector.Builder rNL = IntVector.builder();
        mNL.forEachNeighbor(aIdx, aRMax, aNnn, (dx, dy, dz, idx) -> rNL.add(idx));
        return rNL.build();
    }
    /**
     * 获取给定索引原子的近邻原子索引组成的列表，不包括自身
     * <p>
     * 返回的近邻列表会经过值拷贝，因此可以直接修改不会影响
     * APC 内部的工作
     * <p>
     * 如果需要获取指定数目的最近的近邻原子列表，则使用
     * {@link #getNeighborList(int, double, int)}
     * 来增加一个参数 aNnn
     * <p>
     * 如果需要近邻原子的坐标需要使用
     * {@link #getFullNeighborList(int, double)}
     *
     * @author liqa
     * @param aIdx 需要获取近邻列表的原子索引
     * @param aRMax 近邻的最大截断半径
     * @return 近邻原子索引组成的向量，不包括自身
     * @see IntVector
     */
    public IntVector getNeighborList(int aIdx, double aRMax) {return getNeighborList(aIdx, aRMax, -1);}
    /**
     * @return {@code getNeighborList(aIdx, unitLen()*R_NEAREST_MUL)}
     * @see #getNeighborList(int, double)
     * @see CS#R_NEAREST_MUL
     */
    public IntVector getNeighborList(int aIdx) {return getNeighborList(aIdx, mUnitLen*R_NEAREST_MUL);}
    
    /**
     * 内部使用的直接通过三个坐标值获取近邻列表接口，
     * 目前来说如果需要类似功能则需使用 {@link #getNeighborList(IXYZ, double, int)}
     * @author liqa
     * @see #getNeighborList(IXYZ, double, int)
     */
    @ApiStatus.Internal public IntVector getNeighborList_(double aX, double aY, double aZ, double aRMax, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 由于 lammps 精度的问题，需要将超出边界的进行平移
        XYZ tBuf = new XYZ(aX, aY, aZ);
        mBox.wrapPBC(tBuf);
        aX = tBuf.mX; aY = tBuf.mY; aZ = tBuf.mZ;
        
        final IntVector.Builder rNL = IntVector.builder();
        mNL.forEachNeighbor(aX, aY, aZ, aRMax, aNnn, (dx, dy, dz, idx) -> rNL.add(idx));
        return rNL.build();
    }
    /**
     * 获取给定坐标近邻原子索引组成的列表，不会特意排除恰好位于输入坐标的点
     * <p>
     * 返回的近邻列表会经过值拷贝，因此可以直接修改不会影响
     * APC 内部的工作
     * <p>
     * 如果需要近邻原子的坐标需要使用
     * {@link #getFullNeighborList(IXYZ, double, int)}
     *
     * @author liqa
     * @param aXYZ 需要获取近邻列表的 xyz 坐标
     * @param aRMax 近邻的最大截断半径
     * @param aNnn 需要的最近的近邻原子数目
     * @return 近邻原子索引组成的向量
     * @see IntVector
     * @see IXYZ
     */
    public IntVector getNeighborList(IXYZ aXYZ, double aRMax, int aNnn) {return getNeighborList_(aXYZ.x(), aXYZ.y(), aXYZ.z(), aRMax, aNnn);}
    /**
     * 获取给定坐标近邻原子索引组成的列表，不会特意排除恰好位于输入坐标的点
     * <p>
     * 返回的近邻列表会经过值拷贝，因此可以直接修改不会影响
     * APC 内部的工作
     * <p>
     * 如果需要获取指定数目的最近的近邻原子列表，则使用
     * {@link #getNeighborList(IXYZ, double, int)}
     * 来增加一个参数 aNnn
     * <p>
     * 如果需要近邻原子的坐标需要使用
     * {@link #getFullNeighborList(IXYZ, double)}
     *
     * @author liqa
     * @param aXYZ 需要获取近邻列表的 xyz 坐标
     * @param aRMax 近邻的最大截断半径
     * @return 近邻原子索引组成的向量
     * @see IntVector
     * @see IXYZ
     */
    public IntVector getNeighborList(IXYZ aXYZ, double aRMax) {return getNeighborList(aXYZ, aRMax, -1);}
    /**
     * @return {@code getNeighborList(aXYZ, unitLen()*R_NEAREST_MUL)}
     * @see #getNeighborList(IXYZ, double)
     * @see CS#R_NEAREST_MUL
     */
    public IntVector getNeighborList(IXYZ aXYZ) {return getNeighborList(aXYZ, mUnitLen*R_NEAREST_MUL);}
    
    /**
     * 获取给定索引原子的近邻原子的坐标以及索引组成的列表，不包括自身
     * <p>
     * 返回的近邻列表会经过值拷贝，因此可以直接修改不会影响
     * APC 内部的工作
     * <p>
     * 使用此方法直接获取近邻原子坐标可以自动考虑 bpc
     * 下镜像原子的情况
     *
     * @author liqa
     * @param aIdx 需要获取近邻列表的原子索引
     * @param aRMax 近邻的最大截断半径
     * @param aNnn 需要的最近的近邻原子数目
     * @return 按照 {@code [x, y, z, idx]} 顺序排列的向量列表，不包括自身
     * @see Vector
     */
    public List<Vector> getFullNeighborList(int aIdx, double aRMax, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        final double cX = mAtomDataXYZ.get(aIdx, 0);
        final double cY = mAtomDataXYZ.get(aIdx, 1);
        final double cZ = mAtomDataXYZ.get(aIdx, 2);
        // 目前这种情况都需要遍历一下
        final Vector.Builder rNL = Vector.builder();
        final Vector.Builder rX = Vector.builder();
        final Vector.Builder rY = Vector.builder();
        final Vector.Builder rZ = Vector.builder();
        mNL.forEachNeighbor(aIdx, aRMax, aNnn, (dx, dy, dz, idx) -> {
            rNL.add(idx);
            rX.add(cX+dx);
            rY.add(cY+dy);
            rZ.add(cZ+dz);
        });
        return Lists.newArrayList(rX.build(), rY.build(), rZ.build(), rNL.build());
    }
    /**
     * 获取给定索引原子的近邻原子的坐标以及索引组成的列表，不包括自身
     * <p>
     * 返回的近邻列表会经过值拷贝，因此可以直接修改不会影响
     * APC 内部的工作
     * <p>
     * 如果需要获取指定数目的最近的近邻原子列表，则使用
     * {@link #getFullNeighborList(int, double, int)}
     * 来增加一个参数 aNnn
     * <p>
     * 使用此方法直接获取近邻原子坐标可以自动考虑 bpc
     * 下镜像原子的情况
     *
     * @author liqa
     * @param aIdx 需要获取近邻列表的原子索引
     * @param aRMax 近邻的最大截断半径
     * @return 按照 {@code [x, y, z, idx]} 顺序排列的向量列表，不包括自身
     * @see Vector
     */
    public List<Vector> getFullNeighborList(int aIdx, double aRMax) {return getFullNeighborList(aIdx, aRMax, -1);}
    /**
     * @return {@code getFullNeighborList(aIdx, unitLen()*R_NEAREST_MUL)}
     * @see #getFullNeighborList(int, double)
     * @see CS#R_NEAREST_MUL
     */
    public List<Vector> getFullNeighborList(int aIdx) {return getFullNeighborList(aIdx, mUnitLen*R_NEAREST_MUL);}
    
    /**
     * 内部使用的直接通过三个坐标值获取完整近邻列表接口，
     * 目前来说如果需要类似功能则需使用 {@link #getFullNeighborList(IXYZ, double, int)}
     * @author liqa
     * @see #getFullNeighborList(IXYZ, double, int)
     */
    @ApiStatus.Internal public List<Vector> getFullNeighborList_(double aX, double aY, double aZ, double aRMax, int aNnn) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 由于 lammps 精度的问题，需要将超出边界的进行平移
        XYZ tBuf = new XYZ(aX, aY, aZ);
        mBox.wrapPBC(tBuf);
        double tX = tBuf.mX, tY = tBuf.mY, tZ = tBuf.mZ;
        
        final Vector.Builder rNL = Vector.builder();
        final Vector.Builder rX = Vector.builder();
        final Vector.Builder rY = Vector.builder();
        final Vector.Builder rZ = Vector.builder();
        mNL.forEachNeighbor(tX, tY, tZ, aRMax, aNnn, (dx, dy, dz, idx) -> {
            rNL.add(idx);
            rX.add(aX+dx);
            rY.add(aY+dy);
            rZ.add(aZ+dz);
        });
        return Lists.newArrayList(rX.build(), rY.build(), rZ.build(), rNL.build());
    }
    /**
     * 获取给定坐标近邻原子的坐标以及索引组成的列表，不会特意排除恰好位于输入坐标的点
     * <p>
     * 返回的近邻列表会经过值拷贝，因此可以直接修改不会影响
     * APC 内部的工作
     * <p>
     * 使用此方法直接获取近邻原子坐标可以自动考虑 bpc
     * 下镜像原子的情况
     *
     * @author liqa
     * @param aXYZ 需要获取近邻列表的 xyz 坐标
     * @param aRMax 近邻的最大截断半径
     * @param aNnn 需要的最近的近邻原子数目
     * @return 按照 {@code [x, y, z, idx]} 顺序排列的向量列表
     * @see Vector
     * @see IXYZ
     */
    public List<Vector> getFullNeighborList(IXYZ aXYZ, double aRMax, int aNnn) {return getFullNeighborList_(aXYZ.x(), aXYZ.y(), aXYZ.z(), aRMax, aNnn);}
    /**
     * 获取给定坐标近邻原子索引组成的列表，不会特意排除恰好位于输入坐标的点
     * <p>
     * 返回的近邻列表会经过值拷贝，因此可以直接修改不会影响
     * APC 内部的工作
     * <p>
     * 如果需要获取指定数目的最近的近邻原子列表，则使用
     * {@link #getFullNeighborList(IXYZ, double, int)}
     * 来增加一个参数 aNnn
     * <p>
     * 使用此方法直接获取近邻原子坐标可以自动考虑 bpc
     * 下镜像原子的情况
     *
     * @author liqa
     * @param aXYZ 需要获取近邻列表的 xyz 坐标
     * @param aRMax 近邻的最大截断半径
     * @return 按照 {@code [x, y, z, idx]} 顺序排列的向量列表
     * @see Vector
     * @see IXYZ
     */
    public List<Vector> getFullNeighborList(IXYZ aXYZ, double aRMax) {return getFullNeighborList(aXYZ, aRMax, -1);}
    /**
     * @return {@code getFullNeighborList(aXYZ, unitLen()*R_NEAREST_MUL)}
     * @see #getFullNeighborList(IXYZ, double)
     * @see CS#R_NEAREST_MUL
     */
    public List<Vector> getFullNeighborList(IXYZ aXYZ) {return getFullNeighborList(aXYZ, mUnitLen*R_NEAREST_MUL);}
    
    
    /** 用于分割模拟盒，判断给定 XYZ 或者 idx 处的原子是否在需要考虑的区域中 */
    private class MPIInfo implements IAutoShutdown {
        final MPI.Comm mComm;
        final int mRank, mSize;
        final XYZ mCellSize;
        final int mSizeX, mSizeY, mSizeZ;
        private final double mXLo, mXHi, mYLo, mYHi, mZLo, mZHi;
        MPIInfo(MPI.Comm aComm) throws MPIException {
            // 这里简单处理，MPI 只支持非斜方的模拟盒
            if (mBox.isPrism()) throw new IllegalArgumentException("AtomicParameterCalculator only provides MPI support for orthogonal box");
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
        void setSize(int aSize) {mSize = aSize;}
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
    private @Nullable Thread mInitBufferNLThread = null;
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
        mInitBufferNLThread = Thread.currentThread();
        if (mInitBufferNLThread != mInitThread) UT.Code.warning("Thread of initBufferNL() and init should be SAME in AtomicParameterCalculator");
    }
    
    /// 根据参数获取合适的 NL 用于缓存，此时 null 表示关闭了近邻列表缓存或者没有合适的缓存的近邻列表
    @ApiStatus.Experimental
    @ApiStatus.Internal public IntList @Nullable[] getValidBufferedNL_(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
        initBufferNL_();
        if (mBufferedNL == null) return null;
        return mBufferedNL.getValidBufferedNL(aRMax, aNnn, aHalf, aMPISize);
    }
    @ApiStatus.Experimental
    @ApiStatus.Internal public IntList @Nullable[] getValidBufferedNL_(double aRMax, int aNnn, boolean aHalf) {
        return getValidBufferedNL_(aRMax, aNnn, aHalf, 1);
    }
    
    /// 根据参数获取合适的 NL 用于缓存，此时 null 表示已经有了缓存或者关闭了近邻列表缓存或者参数非法（近邻半径过大）
    @ApiStatus.Experimental
    @ApiStatus.Internal public IntList @Nullable[] getNLWhichNeedBuffer_(double aRMax, int aNnn, boolean aHalf, int aMPISize) {
        initBufferNL_();
        if (mBufferedNL == null) return null;
        if (aRMax > BUFFER_NL_RMAX*mUnitLen) return null;
        if (mBufferedNL.getValidBufferedNL(aRMax, aNnn, aHalf, aMPISize) != null) return null;
        return mBufferedNL.getValidNLToBuffer(aRMax, aNnn, aHalf, aMPISize);
    }
    @ApiStatus.Experimental
    @ApiStatus.Internal public IntList @Nullable[] getNLWhichNeedBuffer_(double aRMax, int aNnn, boolean aHalf) {
        return getNLWhichNeedBuffer_(aRMax, aNnn, aHalf, 1);
    }
    
    /// 会自动使用缓存的近邻列表遍历，用于减少重复代码
    @ApiStatus.Experimental
    @ApiStatus.Internal public void forEachNeighbor_(IntList @Nullable[] aNL, int aIdx, double aRMax, int aNnn, boolean aHalf, IntConsumer aIdxDo) {
        if (aNL != null) {
            // 如果 aNL 不为 null，则直接使用 aNL 遍历
            aNL[aIdx].forEach(aIdxDo);
        } else {
            // aNL 为 null，则使用 mNL 完整遍历
            mNL.forEachNeighbor(aIdx, aRMax, aNnn, aHalf, (dx, dy, dz, idx) -> aIdxDo.accept(idx));
        }
    }
    @ApiStatus.Experimental
    @ApiStatus.Internal public void forEachNeighbor_(IntList @Nullable[] aNL, int aIdx, double aRMax, int aNnn, boolean aHalf, IIndexFilter aRegion, IntConsumer aIdxDo) {
        if (aNL != null) {
            // 如果 aNL 不为 null，则直接使用 aNL 遍历
            aNL[aIdx].forEach(aIdxDo);
        } else {
            // aNL 为 null，则使用 mNL 完整遍历
            mNL.forEachNeighbor(aIdx, aRMax, aNnn, aHalf, aRegion, (dx, dy, dz, idx) -> aIdxDo.accept(idx));
        }
    }
    
    
    /**
     * 计算所有粒子的近邻球谐函数的平均，即 Qlm；
     * 返回一个复数矩阵，行为原子，列为 m
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 主要用于内部使用，由于对象较大这里返回 cache 的值，
     * 从而可以通过 {@link ComplexMatrixCache#returnMat(IComplexMatrix)} 来实现对象重复利用
     *
     * @author liqa
     * @param aL 计算具体 Qlm 值的下标，即 {@code Q4m: l = 4, Q6m: l = 6}
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return Qlm 组成的复矩阵，每行对应每个原子的结果
     * @see IComplexMatrix
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
            // 遍历近邻计算 Ylm
            mNL.forEachNeighbor(i, aRNearest, aNnn, aHalf, (dx, dy, dz, idx) -> {
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计
                IComplexVector Qlmj = null;
                if (aHalf) {
                    Qlmj = Qlm.row(idx);
                }
                // 计算 Y 并累加，考虑对称性只需要算 m=0~l 的部分
                Func.sphericalHarmonics2Dest3(aL, dx, dy, dz, tY);
                Qlmi.plus2this(tY);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计；
                // 注意反向的情况不一定对称，需要考虑 l
                if (aHalf) {
                    if ((aL&1)==1) Qlmj.minus2this(tY);
                    else Qlmj.plus2this(tY);
                }
                
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
    /**
     * 计算所有粒子的近邻球谐函数的平均，即 Qlm；
     * 返回一个复数矩阵，行为原子，列为 m
     * <p>
     * 通过 {@link #calYlmMean(int, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 主要用于内部使用，由于对象较大这里返回 cache 的值，
     * 从而可以通过 {@link ComplexMatrixCache#returnMat(IComplexMatrix)} 来实现对象重复利用
     *
     * @author liqa
     * @param aL 计算具体 Qlm 值的下标，即 {@code Q4m: l = 4, Q6m: l = 6}
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return Qlm 组成的复矩阵，每行对应每个原子的结果
     * @see IComplexMatrix
     */
    public IComplexMatrix calYlmMean(int aL, double aRNearest) {return calYlmMean(aL, aRNearest, -1);}
    /**
     * @return {@code calYlmMean(aL, unitLen()*R_NEAREST_MUL)}
     * @see #calYlmMean(int, double)
     * @see CS#R_NEAREST_MUL
     */
    public IComplexMatrix calYlmMean(int aL) {return calYlmMean(aL, mUnitLen*R_NEAREST_MUL);}
    
    /// MPI 版本的计算 计算所有粒子的近邻球谐函数的平均，即 Qlm
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
            // 遍历近邻计算 Ylm
            final int fI = i;
            mNL.forEachNeighbor(fI, aRNearest, aNnn, aHalf, aMPIInfo::inRegin, (dx, dy, dz, idx) -> {
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计，但如果不在区域内则不需要统计
                boolean tHalfStat = aHalf && aMPIInfo.inRegin(idx);
                IComplexVector Qlmj = null;
                if (tHalfStat) {
                    Qlmj = Qlm.row(idx);
                }
                // 计算 Y 并累加，考虑对称性只需要算 m=0~l 的部分
                Func.sphericalHarmonics2Dest3(aL, dx, dy, dz, tY);
                Qlmi.plus2this(tY);
                // 如果开启 half 遍历的优化，对称的对面的粒子也要增加这个统计；
                // 注意反向的情况不一定对称，需要考虑 l
                if (tHalfStat) {
                    if ((aL&1)==1) Qlmj.minus2this(tY);
                    else Qlmj.plus2this(tY);
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
    /**
     * MPI 版本的计算所有粒子的近邻球谐函数的平均，即 Qlm
     * <p>
     * 要求调用此方法的每个 MPI 进程中的原子数据都是完整且一致的，
     * 通过 aNoGather 参数控制输出结果是否同步，
     * 如果同步则每个进程都会得到一个相同且完整的计算结果
     *
     * @author liqa
     * @param aNoGather 是否关闭输出结果的同步，关闭可以减少进程通讯的损耗，默认不进行关闭（{@code false}）
     * @param aComm 希望使用的 MPI 通讯器，默认为 {@link MPI.Comm#WORLD}
     * @param aL 计算具体 Qlm 值的下标，即 {@code Q4m: l = 4, Q6m: l = 6}
     * @param aRNearest 用来搜索的最近邻半径
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）
     * @return Qlm 组成的复矩阵，每行对应每个原子的结果
     * @see #calYlmMean(int, double, int)
     * @see MPI
     * @see MPI.Comm
     */
    public IComplexMatrix calYlmMean_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calYlmMean_MPI_(aNoGather, tMPIInfo, aL, aRNearest, aNnn);}}
    /**
     * @return {@code calYlmMean_MPI(false, aComm, aL, aRNearest, aNnn)}
     * @see #calYlmMean_MPI(boolean, MPI.Comm, int, double, int)
     */
    public IComplexMatrix calYlmMean_MPI(MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calYlmMean_MPI_(tMPIInfo, aL, aRNearest, aNnn);}}
    
    
    /**
     * 在 Qlm 基础上再次对所有近邻做一次平均，即 qlm；
     * 返回一个复数矩阵，行为原子，列为 m
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 主要用于内部使用，由于对象较大这里返回 cache 的值，
     * 从而可以通过 {@link ComplexMatrixCache#returnMat} 来实现对象重复利用
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aL 计算具体 qlm 值的下标，即 {@code q4m: l = 4, q6m: l = 6}
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 的最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return qlm 组成的复矩阵，每行对应每个原子的结果
     * @see IComplexMatrix
     * @see #calYlmMean(int, double, int)
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
    /**
     * 在 Qlm 基础上再次对所有近邻做一次平均，即 qlm；
     * 返回一个复数矩阵，行为原子，列为 m
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 主要用于内部使用，由于对象较大这里返回 cache 的值，
     * 从而可以通过 {@link ComplexMatrixCache#returnMat} 来实现对象重复利用
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aL 计算具体 qlm 值的下标，即 {@code q4m: l = 4, q6m: l = 6}
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return qlm 组成的复矩阵，每行对应每个原子的结果
     * @see IComplexMatrix
     * @see #calYlmMean(int, double, int)
     */
    public IComplexMatrix calQlmMean(int aL, double aRNearest, int aNnn) {return calQlmMean(aL, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 在 Qlm 基础上再次对所有近邻做一次平均，即 qlm；
     * 返回一个复数矩阵，行为原子，列为 m
     * <p>
     * 通过 {@link #calQlmMean(int, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 主要用于内部使用，由于对象较大这里返回 cache 的值，
     * 从而可以通过 {@link ComplexMatrixCache#returnMat} 来实现对象重复利用
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aL 计算具体 qlm 值的下标，即 {@code q4m: l = 4, q6m: l = 6}
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return qlm 组成的复矩阵，每行对应每个原子的结果
     * @see IComplexMatrix
     * @see #calYlmMean(int, double)
     */
    public IComplexMatrix calQlmMean(int aL, double aRNearest) {return calQlmMean(aL, aRNearest, -1);}
    /**
     * @return {@code calQlmMean(aL, unitLen()*R_NEAREST_MUL)}
     * @see #calQlmMean(int, double)
     * @see CS#R_NEAREST_MUL
     */
    public IComplexMatrix calQlmMean(int aL) {return calQlmMean(aL, mUnitLen*R_NEAREST_MUL);}
    
    /// MPI 版本的在 Qlm 基础上再次对所有近邻做一次平均，即 qlm
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
    /**
     * MPI 版本的 Qlm 基础上再次对所有近邻做一次平均，即 qlm
     * <p>
     * 要求调用此方法的每个 MPI 进程中的原子数据都是完整且一致的，
     * 通过 aNoGather 参数控制输出结果是否同步，
     * 如果同步则每个进程都会得到一个相同且完整的计算结果
     *
     * @author liqa
     * @param aNoGather 是否关闭输出结果的同步，关闭可以减少进程通讯的损耗，默认不进行关闭（{@code false}）
     * @param aComm 希望使用的 MPI 通讯器，默认为 {@link MPI.Comm#WORLD}
     * @param aL 计算具体 qlm 值的下标，即 {@code q4m: l = 4, q6m: l = 6}
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径
     * @param aNnnY 用来计算 YlmMean 的最大的最近邻数目（Number of Nearest Neighbor list）
     * @param aRNearestQ 用来计算 QlmMean 搜索的最近邻半径
     * @param aNnnQ 用来计算 QlmMean 的最大的最近邻数目（Number of Nearest Neighbor list）
     * @return qlm 组成的复矩阵，每行对应每个原子的结果
     * @see #calQlmMean(int, double, int, double, int)
     * @see MPI
     * @see MPI.Comm
     */
    public IComplexMatrix calQlmMean_MPI(boolean aNoGather, final MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calQlmMean_MPI_(aNoGather, tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    /**
     * @return {@code calQlmMean_MPI(false, aComm, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ)}
     * @see #calQlmMean_MPI(boolean, MPI.Comm, int, double, int, double, int)
     */
    public IComplexMatrix calQlmMean_MPI(MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calQlmMean_MPI_(tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    
    
    /**
     * 计算所有粒子的原始的 BOOP（local Bond Orientational Order Parameters, Ql），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算对近邻平均过一次的键角序参量（ABOOP, ql），需要调用
     * {@link #calABOOP(int, double, int)}
     * <p>
     * References: <a href="http://dx.doi.org/10.1103/PhysRevB.28.784">
     * Bond-orientational order in liquids and glasses</a>,
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>
     *
     * @author liqa
     * @param aL 计算具体 Q 值的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return Ql 组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calABOOP(int, double, int)
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
    /**
     * 计算所有粒子的原始的 BOOP（local Bond Orientational Order Parameters, Ql），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 通过 {@link #calBOOP(int, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算对近邻平均过一次的键角序参量（ABOOP, ql），需要调用
     * {@link #calABOOP(int, double)}
     * <p>
     * References: <a href="http://dx.doi.org/10.1103/PhysRevB.28.784">
     * Bond-orientational order in liquids and glasses</a>,
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>
     *
     * @author liqa
     * @param aL 计算具体 Q 值的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return Ql 组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calABOOP(int, double)
     */
    public IVector calBOOP(int aL, double aRNearest) {return calBOOP(aL, aRNearest, -1);}
    /**
     * @return {@code calBOOP(aL, unitLen()*R_NEAREST_MUL)}
     * @see #calBOOP(int, double)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calBOOP(int aL) {return calBOOP(aL, mUnitLen*R_NEAREST_MUL);}
    
    /// MPI 版本的计算所有粒子的原始的 BOOP
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
    /**
     * MPI 版本的 BOOP 计算，即 Ql
     * <p>
     * 要求调用此方法的每个 MPI 进程中的原子数据都是完整且一致的，
     * 通过 aNoGather 参数控制输出结果是否同步，
     * 如果同步则每个进程都会得到一个相同且完整的计算结果
     *
     * @author liqa
     * @param aNoGather 是否关闭输出结果的同步，关闭可以减少进程通讯的损耗，默认不进行关闭（{@code false}）
     * @param aComm 希望使用的 MPI 通讯器，默认为 {@link MPI.Comm#WORLD}
     * @param aL 计算具体 Ql 值的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return Ql 组成的向量，按照原子数据中的原子排序
     * @see #calBOOP(int, double, int)
     * @see MPI
     * @see MPI.Comm
     */
    public IVector calBOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calBOOP_MPI_(aNoGather, tMPIInfo, aL, aRNearest, aNnn);}}
    /**
     * @return {@code calBOOP_MPI(false, aComm, aL, aRNearest, aNnn)}
     * @see #calBOOP_MPI(boolean, MPI.Comm, int, double, int)
     */
    public IVector calBOOP_MPI(MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calBOOP_MPI_(tMPIInfo, aL, aRNearest, aNnn);}}
    /**
     * 不做近邻数目限制版本的 {@link #calBOOP_MPI(MPI.Comm, int, double, int)}
     * @see #calBOOP_MPI(boolean, MPI.Comm, int, double, int)
     */
    public IVector calBOOP_MPI(MPI.Comm aComm, int aL, double aRNearest) throws MPIException {return calBOOP_MPI(aComm, aL, aRNearest, -1);}
    /**
     * @return {@code calBOOP_MPI(aComm, aL, unitLen()*R_NEAREST_MUL)}
     * @see #calBOOP_MPI(boolean, MPI.Comm, int, double, int)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calBOOP_MPI(MPI.Comm aComm, int aL) throws MPIException {return calBOOP_MPI(aComm, aL, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code calBOOP_MPI(MPI.Comm.WORLD, aL, aRNearest, aNnn)}
     * @see #calBOOP_MPI(boolean, MPI.Comm, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calBOOP_MPI(int aL, double aRNearest, int aNnn) throws MPIException {return calBOOP_MPI(MPI.Comm.WORLD, aL, aRNearest, aNnn);}
    /**
     * @return {@code calBOOP_MPI(MPI.Comm.WORLD, aL, aRNearest)}
     * @see #calBOOP_MPI(boolean, MPI.Comm, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calBOOP_MPI(int aL, double aRNearest) throws MPIException {return calBOOP_MPI(MPI.Comm.WORLD, aL, aRNearest);}
    /**
     * @return {@code calBOOP_MPI(MPI.Comm.WORLD, aL)}
     * @see #calBOOP_MPI(boolean, MPI.Comm, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calBOOP_MPI(int aL) throws MPIException {return calBOOP_MPI(MPI.Comm.WORLD, aL);}
    
    
    /**
     * 计算所有粒子的三阶形式的 BOOP（local Bond Orientational Order Parameters, Wl），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算对近邻平均过一次的键角序参量（ABOOP, wl），需要调用
     * {@link #calABOOP3(int, double, int)}
     * <p>
     * References: <a href="http://dx.doi.org/10.1103/PhysRevB.28.784">
     * Bond-orientational order in liquids and glasses</a>,
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>
     *
     * @author liqa
     * @param aL 计算具体 W 值的下标，即 {@code W4: l = 4, W6: l = 6}
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return Wl 组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calABOOP3(int, double, int)
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
    /**
     * 计算所有粒子的三阶形式的 BOOP（local Bond Orientational Order Parameters, Wl），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 通过 {@link #calBOOP3(int, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算对近邻平均过一次的键角序参量（ABOOP, wl），需要调用
     * {@link #calABOOP3(int, double)}
     * <p>
     * References: <a href="http://dx.doi.org/10.1103/PhysRevB.28.784">
     * Bond-orientational order in liquids and glasses</a>,
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>
     *
     * @author liqa
     * @param aL 计算具体 W 值的下标，即 {@code W4: l = 4, W6: l = 6}
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return Wl 组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calABOOP3(int, double)
     */
    public IVector calBOOP3(int aL, double aRNearest) {return calBOOP3(aL, aRNearest, -1);}
    /**
     * @return {@code calBOOP3(aL, unitLen()*R_NEAREST_MUL)}
     * @see #calBOOP3(int, double)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calBOOP3(int aL) {return calBOOP3(aL, mUnitLen*R_NEAREST_MUL);}
    
    
    /**
     * 计算所有粒子的 ABOOP（Averaged local Bond Orientational Order Parameters, ql），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算原始的键角序参量（BOOP, Ql），需要调用
     * {@link #calBOOP(int, double, int)}
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aL 计算具体 q 值的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return ql 组成的向量
     * @see IVector
     * @see #calBOOP(int, double, int)
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
    /**
     * 计算所有粒子的 ABOOP（Averaged local Bond Orientational Order Parameters, ql），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算原始的键角序参量（BOOP, Ql），需要调用
     * {@link #calBOOP(int, double, int)}
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aL 计算具体 q 值的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return ql 组成的向量
     * @see IVector
     * @see #calBOOP(int, double, int)
     */
    public IVector calABOOP(int aL, double aRNearest, int aNnn) {return calABOOP(aL, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 计算所有粒子的 ABOOP（Averaged local Bond Orientational Order Parameters, ql），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 通过 {@link #calABOOP(int, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算原始的键角序参量（BOOP, Ql），需要调用
     * {@link #calBOOP(int, double)}
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aL 计算具体 q 值的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return ql 组成的向量
     * @see IVector
     * @see #calBOOP(int, double)
     */
    public IVector calABOOP(int aL, double aRNearest) {return calABOOP(aL, aRNearest, -1);}
    /**
     * @return {@code calABOOP(aL, unitLen()*R_NEAREST_MUL)}
     * @see #calABOOP(int, double)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calABOOP(int aL) {return calABOOP(aL, mUnitLen*R_NEAREST_MUL);}
    
    
    /// MPI 版本的计算所有粒子的 ABOOP
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
    /**
     * MPI 版本的 ABOOP 计算，即 ql
     * <p>
     * 要求调用此方法的每个 MPI 进程中的原子数据都是完整且一致的，
     * 通过 aNoGather 参数控制输出结果是否同步，
     * 如果同步则每个进程都会得到一个相同且完整的计算结果
     *
     * @author liqa
     * @param aNoGather 是否关闭输出结果的同步，关闭可以减少进程通讯的损耗，默认不进行关闭（{@code false}）
     * @param aComm 希望使用的 MPI 通讯器，默认为 {@link MPI.Comm#WORLD}
     * @param aL 计算具体 qlm 值的下标，即 {@code q4m: l = 4, q6m: l = 6}
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return ql 组成的向量，按照原子数据中的原子排序
     * @see #calABOOP(int, double, int, double, int)
     * @see MPI
     * @see MPI.Comm
     */
    public IVector calABOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calABOOP_MPI_(aNoGather, tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    /**
     * @return {@code calABOOP_MPI(false, aComm, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ)}
     * @see #calABOOP_MPI(boolean, MPI.Comm, int, double, int, double, int)
     */
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calABOOP_MPI_(tMPIInfo, aL, aRNearestY, aNnnY, aRNearestQ, aNnnQ);}}
    /**
     * @return {@code calABOOP_MPI(aComm, aL, aRNearest, aNnn, aRNearest, aNnn)}
     * @see #calABOOP_MPI(boolean, MPI.Comm, int, double, int, double, int)
     */
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL, double aRNearest, int aNnn) throws MPIException {return calABOOP_MPI(aComm, aL, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 不做近邻数目限制版本的 {@link #calABOOP_MPI(MPI.Comm, int, double, int)}
     * @see #calABOOP_MPI(boolean, MPI.Comm, int, double, int, double, int)
     */
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL, double aRNearest) throws MPIException {return calABOOP_MPI(aComm, aL, aRNearest, -1);}
    /**
     * @return {@code calABOOP_MPI(aComm, aL, unitLen()*R_NEAREST_MUL)}
     * @see #calABOOP_MPI(boolean, MPI.Comm, int, double, int, double, int)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calABOOP_MPI(MPI.Comm aComm, int aL) throws MPIException {return calABOOP_MPI(aComm, aL, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code calABOOP_MPI(MPI.Comm.WORLD, aL, aRNearest, aNnn)}
     * @see #calABOOP_MPI(boolean, MPI.Comm, int, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calABOOP_MPI(int aL, double aRNearest, int aNnn) throws MPIException {return calABOOP_MPI(MPI.Comm.WORLD, aL, aRNearest, aNnn);}
    /**
     * @return {@code calABOOP_MPI(MPI.Comm.WORLD, aL, aRNearest)}
     * @see #calABOOP_MPI(boolean, MPI.Comm, int, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calABOOP_MPI(int aL, double aRNearest) throws MPIException {return calABOOP_MPI(MPI.Comm.WORLD, aL, aRNearest);}
    /**
     * @return {@code calABOOP_MPI(MPI.Comm.WORLD, aL)}
     * @see #calABOOP_MPI(boolean, MPI.Comm, int, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calABOOP_MPI(int aL) throws MPIException {return calABOOP_MPI(MPI.Comm.WORLD, aL);}
    
    
    /**
     * 计算所有粒子的三阶形式的 ABOOP（Averaged local Bond Orientational Order Parameters, wl），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算原始的键角序参量（BOOP, Wl），需要调用
     * {@link #calBOOP3(int, double, int)}
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aL 计算具体 w 值的下标，即 {@code w4: l = 4, w6: l = 6}
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return wl 组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calBOOP3(int, double, int)
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
    /**
     * 计算所有粒子的三阶形式的 ABOOP（Averaged local Bond Orientational Order Parameters, wl），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算原始的键角序参量（BOOP, Wl），需要调用
     * {@link #calBOOP3(int, double, int)}
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aL 计算具体 w 值的下标，即 {@code w4: l = 4, w6: l = 6}
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return wl 组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calBOOP3(int, double, int)
     */
    public IVector calABOOP3(int aL, double aRNearest, int aNnn) {return calABOOP3(aL, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 计算所有粒子的三阶形式的 ABOOP（Averaged local Bond Orientational Order Parameters, wl），
     * 输出结果为按照输入原子顺序排列的向量；
     * <p>
     * 通过 {@link #calABOOP3(int, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要计算原始的键角序参量（BOOP, Wl），需要调用
     * {@link #calBOOP3(int, double)}
     * <p>
     * Reference: <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aL 计算具体 w 值的下标，即 {@code w4: l = 4, w6: l = 6}
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return wl 组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calBOOP3(int, double)
     */
    public IVector calABOOP3(int aL, double aRNearest) {return calABOOP3(aL, aRNearest, -1);}
    /**
     * @return {@code calABOOP3(aL, unitLen()*R_NEAREST_MUL)}
     * @see #calABOOP3(int, double)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calABOOP3(int aL) {return calABOOP3(aL, mUnitLen*R_NEAREST_MUL);}
    
    
    /**
     * 通过类似键角序参量（Ql）的算法来计算结构中每个原子的连接数目，
     * 输出结果为按照输入原子顺序排列的向量，数值为连接数目；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用对近邻平均过一次的键角序参量（ABOOP, ql），需要调用
     * {@link #calConnectCountABOOP(int, double, double, int)}
     * <p>
     * References:
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>,
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl</a>
     *
     * @author liqa
     * @param aL 使用的 Ql 的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestS 用来计算 Sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 Sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数目组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectCountABOOP(int, double, double, int)
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
    /**
     * 通过类似键角序参量（BOOP, Ql）的算法来计算结构中每个原子的连接数目，
     * 输出结果为按照输入原子顺序排列的向量，数值为连接数目；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用对近邻平均过一次的键角序参量（ABOOP, ql），需要调用
     * {@link #calConnectCountABOOP(int, double, double, int)}
     * <p>
     * References:
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>,
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl</a>
     *
     * @author liqa
     * @param aL 使用的 Ql 的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后得到的连接数目组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectCountABOOP(int, double, double, int)
     */
    public IVector calConnectCountBOOP(int aL, double aConnectThreshold, double aRNearest, int aNnn) {return calConnectCountBOOP(aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 通过类似键角序参量（BOOP, Ql）的算法来计算结构中每个原子的连接数目，
     * 输出结果为按照输入原子顺序排列的向量，数值为连接数目；
     * <p>
     * 通过 {@link #calConnectCountBOOP(int, double, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用对近邻平均过一次的键角序参量（ABOOP, ql），需要调用
     * {@link #calConnectCountABOOP(int, double, double)}
     * <p>
     * References:
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>,
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl</a>
     *
     * @author liqa
     * @param aL 使用的 Ql 的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return 最后得到的连接数目组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectCountABOOP(int, double, double)
     */
    public IVector calConnectCountBOOP(int aL, double aConnectThreshold, double aRNearest) {return calConnectCountBOOP(aL, aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code calConnectCountBOOP(aL, aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #calConnectCountBOOP(int, double, double)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calConnectCountBOOP(int aL, double aConnectThreshold) {return calConnectCountBOOP(aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    
    /// MPI 版本的 BOOP 连接数目
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
    /**
     * MPI 版本的 BOOP 连接数计算
     * <p>
     * 要求调用此方法的每个 MPI 进程中的原子数据都是完整且一致的，
     * 通过 aNoGather 参数控制输出结果是否同步，
     * 如果同步则每个进程都会得到一个相同且完整的计算结果
     *
     * @author liqa
     * @param aNoGather 是否关闭输出结果的同步，关闭可以减少进程通讯的损耗，默认不进行关闭（{@code false}）
     * @param aComm 希望使用的 MPI 通讯器，默认为 {@link MPI.Comm#WORLD}
     * @param aL 使用的 Ql 的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestS 用来计算 Sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 Sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数目组成的向量，按照原子数据中的原子排序
     * @see #calConnectCountBOOP(int, double, double, int)
     * @see MPI
     * @see MPI.Comm
     */
    public IVector calConnectCountBOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountBOOP_MPI_(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}}
    /**
     * @return {@code calConnectCountBOOP_MPI(false, aComm, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS)}
     * @see #calConnectCountBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     */
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountBOOP_MPI_(tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}}
    /**
     * @return {@code calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn)}
     * @see #calConnectCountBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     */
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 不做近邻数目限制版本的 {@link #calConnectCountBOOP_MPI(MPI.Comm, int, double, double, int)}
     * @see #calConnectCountBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     */
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest) throws MPIException {return calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #calConnectCountBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calConnectCountBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold) throws MPIException {return calConnectCountBOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn)}
     * @see #calConnectCountBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectCountBOOP_MPI(int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    /**
     * @return {@code calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest)}
     * @see #calConnectCountBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectCountBOOP_MPI(int aL, double aConnectThreshold, double aRNearest) throws MPIException {return calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    /**
     * @return {@code calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold)}
     * @see #calConnectCountBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectCountBOOP_MPI(int aL, double aConnectThreshold) throws MPIException {return calConnectCountBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 通过类似平均的键角序参量（ABOOP, ql）的算法来计算结构中每个原子的连接数目，
     * 输出结果为按照输入原子顺序排列的向量，数值为连接数目；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用原始的键角序参量（BOOP, Ql），需要调用
     * {@link #calConnectCountBOOP(int, double, double, int)}
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     *
     * @author liqa
     * @param aL 使用的 ql 的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @param aRNearestS 用来计算 sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数目组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectCountBOOP(int, double, double, int)
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
    /**
     * 通过类似平均的键角序参量（ABOOP, ql）的算法来计算结构中每个原子的连接数目，
     * 输出结果为按照输入原子顺序排列的向量，数值为连接数目；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用原始的键角序参量（BOOP, Ql），需要调用
     * {@link #calConnectCountBOOP(int, double, double, int)}
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     *
     * @author liqa
     * @param aL 使用的 ql 的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后得到的连接数目组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectCountBOOP(int, double, double, int)
     */
    public IVector calConnectCountABOOP(int aL, double aConnectThreshold, double aRNearest, int aNnn) {return calConnectCountABOOP(aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 通过类似平均的键角序参量（ABOOP, ql）的算法来计算结构中每个原子的连接数目，
     * 输出结果为按照输入原子顺序排列的向量，数值为连接数目；
     * <p>
     * 通过 {@link #calConnectCountABOOP(int, double, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用原始的键角序参量（BOOP, Ql），需要调用
     * {@link #calConnectCountBOOP(int, double, double)}
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     *
     * @author liqa
     * @param aL 使用的 ql 的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return 最后得到的连接数目组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectCountBOOP(int, double, double)
     */
    public IVector calConnectCountABOOP(int aL, double aConnectThreshold, double aRNearest) {return calConnectCountABOOP(aL, aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code calConnectCountABOOP(aL, aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #calConnectCountABOOP(int, double, double)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calConnectCountABOOP(int aL, double aConnectThreshold) {return calConnectCountABOOP(aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    
    /// MPI 版本的 BOOP 连接数目
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
    /**
     * MPI 版本的 ABOOP 连接数计算
     * <p>
     * 要求调用此方法的每个 MPI 进程中的原子数据都是完整且一致的，
     * 通过 aNoGather 参数控制输出结果是否同步，
     * 如果同步则每个进程都会得到一个相同且完整的计算结果
     *
     * @author liqa
     * @param aNoGather 是否关闭输出结果的同步，关闭可以减少进程通讯的损耗，默认不进行关闭（{@code false}）
     * @param aComm 希望使用的 MPI 通讯器，默认为 {@link MPI.Comm#WORLD}
     * @param aL 使用的 ql 的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @param aRNearestS 用来计算 sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数目组成的向量，按照原子数据中的原子排序
     * @see #calConnectCountABOOP(int, double, double, int)
     * @see MPI
     * @see MPI.Comm
     */
    public IVector calConnectCountABOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountABOOP_MPI_(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}}
    /**
     * @return {@code calConnectCountABOOP_MPI(false, aComm, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS)}
     * @see #calConnectCountABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     */
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectCountABOOP_MPI_(tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}}
    /**
     * @return {@code calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn)}
     * @see #calConnectCountABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     */
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 不做近邻数目限制版本的 {@link #calConnectCountABOOP_MPI(MPI.Comm, int, double, double, int)}
     * @see #calConnectCountABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     */
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest) throws MPIException {return calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #calConnectCountABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calConnectCountABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold) throws MPIException {return calConnectCountABOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn)}
     * @see #calConnectCountABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectCountABOOP_MPI(int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    /**
     * @return {@code calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest)}
     * @see #calConnectCountABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectCountABOOP_MPI(int aL, double aConnectThreshold, double aRNearest) throws MPIException {return calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    /**
     * @return {@code calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold)}
     * @see #calConnectCountABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectCountABOOP_MPI(int aL, double aConnectThreshold) throws MPIException {return calConnectCountABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 通过类似键角序参量（BOOP, Ql）的算法来计算结构中每个原子的连接数占所有近邻数的比例值，
     * 输出结果为按照输入原子顺序排列的向量，数值为 0~1 的比例值；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用对近邻平均过一次的键角序参量（ABOOP, ql），需要调用
     * {@link #calConnectRatioABOOP(int, double, double, int)}
     * <p>
     * References:
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>,
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl</a>
     *
     * @author liqa
     * @param aL 使用的 Ql 的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestS 用来计算 Sij 的搜索的最近邻半径，会使用此值对应的近邻总数作为分母。默认为 aRNearestY
     * @param aNnnS 用来计算 Sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数占所有近邻数的比例值组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectRatioABOOP(int, double, double, int)
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
    /**
     * 通过类似键角序参量（BOOP, Ql）的算法来计算结构中每个原子的连接数占所有近邻数的比例值，
     * 输出结果为按照输入原子顺序排列的向量，数值为 0~1 的比例值；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用对近邻平均过一次的键角序参量（ABOOP, ql），需要调用
     * {@link #calConnectRatioABOOP(int, double, double, int)}
     * <p>
     * References:
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>,
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl</a>
     *
     * @author liqa
     * @param aL 使用的 Ql 的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后得到的连接数占所有近邻数的比例值组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectRatioABOOP(int, double, double, int)
     */
    public IVector calConnectRatioBOOP(int aL, double aConnectThreshold, double aRNearest, int aNnn) {return calConnectRatioBOOP(aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 通过类似键角序参量（BOOP, Ql）的算法来计算结构中每个原子的连接数占所有近邻数的比例值，
     * 输出结果为按照输入原子顺序排列的向量，数值为 0~1 的比例值；
     * <p>
     * 通过 {@link #calConnectRatioBOOP(int, double, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用对近邻平均过一次的键角序参量（ABOOP, ql），需要调用
     * {@link #calConnectRatioABOOP(int, double, double, int)}
     * <p>
     * References:
     * <a href="https://doi.org/10.1039/FD9960400093">
     * Simulation of homogeneous crystal nucleation close to coexistence</a>,
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl</a>
     *
     * @author liqa
     * @param aL 使用的 Ql 的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return 最后得到的连接数占所有近邻数的比例值组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectRatioABOOP(int, double, double, int)
     */
    public IVector calConnectRatioBOOP(int aL, double aConnectThreshold, double aRNearest) {return calConnectRatioBOOP(aL, aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code calConnectRatioBOOP(aL, aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #calConnectRatioBOOP(int, double, double)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calConnectRatioBOOP(int aL, double aConnectThreshold) {return calConnectRatioBOOP(aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    
    /// MPI 版本的 BOOP 连接比例
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
    /**
     * MPI 版本的 BOOP 连接比例计算
     * <p>
     * 要求调用此方法的每个 MPI 进程中的原子数据都是完整且一致的，
     * 通过 aNoGather 参数控制输出结果是否同步，
     * 如果同步则每个进程都会得到一个相同且完整的计算结果
     *
     * @author liqa
     * @param aNoGather 是否关闭输出结果的同步，关闭可以减少进程通讯的损耗，默认不进行关闭（{@code false}）
     * @param aComm 希望使用的 MPI 通讯器，默认为 {@link MPI.Comm#WORLD}
     * @param aL 使用的 Ql 的下标，即 {@code Q4: l = 4, Q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestS 用来计算 Sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 Sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接比例组成的向量，按照原子数据中的原子排序
     * @see #calConnectRatioBOOP(int, double, double, int)
     * @see MPI
     * @see MPI.Comm
     */
    public IVector calConnectRatioBOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectRatioBOOP_MPI_(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}}
    /**
     * @return {@code calConnectRatioBOOP_MPI(false, aComm, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS)}
     * @see #calConnectRatioBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     */
    public IVector calConnectRatioBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectRatioBOOP_MPI_(tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestS, aNnnS);}}
    /**
     * @return {@code calConnectRatioBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn)}
     * @see #calConnectRatioBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     */
    public IVector calConnectRatioBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectRatioBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 不做近邻数目限制版本的 {@link #calConnectRatioBOOP_MPI(MPI.Comm, int, double, double, int)}
     * @see #calConnectRatioBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     */
    public IVector calConnectRatioBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest) throws MPIException {return calConnectRatioBOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code calConnectRatioBOOP_MPI(aComm, aL, aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #calConnectRatioBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calConnectRatioBOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold) throws MPIException {return calConnectRatioBOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code calConnectRatioBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn)}
     * @see #calConnectRatioBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectRatioBOOP_MPI(int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectRatioBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    /**
     * @return {@code calConnectRatioBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest)}
     * @see #calConnectRatioBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectRatioBOOP_MPI(int aL, double aConnectThreshold, double aRNearest) throws MPIException {return calConnectRatioBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    /**
     * @return {@code calConnectRatioBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold)}
     * @see #calConnectRatioBOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectRatioBOOP_MPI(int aL, double aConnectThreshold) throws MPIException {return calConnectRatioBOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 通过类似平均的键角序参量（ABOOP, ql）的算法来计算结构中每个原子的连接数占所有近邻数的比例值，
     * 输出结果为按照输入原子顺序排列的向量，数值为 0~1 的比例值；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用原始的键角序参量（BOOP, Ql），需要调用
     * {@link #calConnectRatioBOOP(int, double, double, int)}
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     *
     * @author liqa
     * @param aL 使用的 ql 的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @param aRNearestS 用来计算 sij 的搜索的最近邻半径，会使用此值对应的近邻总数作为分母。默认为 aRNearestY
     * @param aNnnS 用来计算 sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数占所有近邻数的比例值组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectRatioBOOP(int, double, double, int)
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
    /**
     * 通过类似平均的键角序参量（ABOOP, ql）的算法来计算结构中每个原子的连接数占所有近邻数的比例值，
     * 输出结果为按照输入原子顺序排列的向量，数值为 0~1 的比例值；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用原始的键角序参量（BOOP, Ql），需要调用
     * {@link #calConnectRatioBOOP(int, double, double, int)}
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     *
     * @author liqa
     * @param aL 使用的 ql 的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后得到的连接数占所有近邻数的比例值组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectRatioBOOP(int, double, double, int)
     */
    public IVector calConnectRatioABOOP(int aL, double aConnectThreshold, double aRNearest, int aNnn) {return calConnectRatioABOOP(aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 通过类似平均的键角序参量（ABOOP, ql）的算法来计算结构中每个原子的连接数占所有近邻数的比例值，
     * 输出结果为按照输入原子顺序排列的向量，数值为 0~1 的比例值；
     * <p>
     * 通过 {@link #calConnectRatioABOOP(int, double, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * 为了统一接口这里同样返回 cache 的值，
     * 从而可以通过 {@link VectorCache#returnVec} 来实现对象重复利用
     * <p>
     * 如果需要使用原始的键角序参量（BOOP, Ql），需要调用
     * {@link #calConnectRatioBOOP(int, double, double)}
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>,
     *
     * @author liqa
     * @param aL 使用的 ql 的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearest 最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return 最后得到的连接数占所有近邻数的比例值组成的向量，按照原子数据中的原子排序
     * @see IVector
     * @see #calConnectRatioBOOP(int, double, double)
     */
    public IVector calConnectRatioABOOP(int aL, double aConnectThreshold, double aRNearest) {return calConnectRatioABOOP(aL, aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code calConnectRatioABOOP(aL, aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #calConnectRatioABOOP(int, double, double)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calConnectRatioABOOP(int aL, double aConnectThreshold) {return calConnectRatioABOOP(aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    
    /// MPI 版本的 BOOP 连接数目
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
    /**
     * MPI 版本的 ABOOP 连接比例计算
     * <p>
     * 要求调用此方法的每个 MPI 进程中的原子数据都是完整且一致的，
     * 通过 aNoGather 参数控制输出结果是否同步，
     * 如果同步则每个进程都会得到一个相同且完整的计算结果
     *
     * @author liqa
     * @param aNoGather 是否关闭输出结果的同步，关闭可以减少进程通讯的损耗，默认不进行关闭（{@code false}）
     * @param aComm 希望使用的 MPI 通讯器，默认为 {@link MPI.Comm#WORLD}
     * @param aL 使用的 ql 的下标，即 {@code q4: l = 4, q6: l = 6}
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值
     * @param aRNearestY 用来计算 YlmMean 的搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnnY 用来计算 YlmMean 的最大最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @param aRNearestQ 用来计算 QlmMean 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnQ 用来计算 QlmMean 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @param aRNearestS 用来计算 sij 的搜索的最近邻半径。默认为 aRNearestY
     * @param aNnnS 用来计算 sij 最大的最近邻数目（Number of Nearest Neighbor list）。默认为 aNnnY
     * @return 最后得到的连接数目组成的向量，按照原子数据中的原子排序
     * @see #calConnectRatioABOOP(int, double, double, int)
     * @see MPI
     * @see MPI.Comm
     */
    public IVector calConnectRatioABOOP_MPI(boolean aNoGather, MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectRatioABOOP_MPI_(aNoGather, tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}}
    /**
     * @return {@code calConnectRatioABOOP_MPI(false, aComm, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS)}
     * @see #calConnectRatioABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     */
    public IVector calConnectRatioABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearestY, int aNnnY, double aRNearestQ, int aNnnQ, double aRNearestS, int aNnnS) throws MPIException {try (MPIInfo tMPIInfo = new MPIInfo(aComm)) {return calConnectRatioABOOP_MPI_(tMPIInfo, aL, aConnectThreshold, aRNearestY, aNnnY, aRNearestQ, aNnnQ, aRNearestS, aNnnS);}}
    /**
     * @return {@code calConnectRatioABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn)}
     * @see #calConnectRatioABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     */
    public IVector calConnectRatioABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectRatioABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, aNnn, aRNearest, aNnn, aRNearest, aNnn);}
    /**
     * 不做近邻数目限制版本的 {@link #calConnectRatioABOOP_MPI(MPI.Comm, int, double, double, int)}
     * @see #calConnectRatioABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     */
    public IVector calConnectRatioABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold, double aRNearest) throws MPIException {return calConnectRatioABOOP_MPI(aComm, aL, aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code calConnectRatioABOOP_MPI(aComm, aL, aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #calConnectRatioABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     * @see CS#R_NEAREST_MUL
     */
    public IVector calConnectRatioABOOP_MPI(MPI.Comm aComm, int aL, double aConnectThreshold) throws MPIException {return calConnectRatioABOOP_MPI(aComm, aL, aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code calConnectRatioABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn)}
     * @see #calConnectRatioABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectRatioABOOP_MPI(int aL, double aConnectThreshold, double aRNearest, int aNnn) throws MPIException {return calConnectRatioABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest, aNnn);}
    /**
     * @return {@code calConnectRatioABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest)}
     * @see #calConnectRatioABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectRatioABOOP_MPI(int aL, double aConnectThreshold, double aRNearest) throws MPIException {return calConnectRatioABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold, aRNearest);}
    /**
     * @return {@code calConnectRatioABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold)}
     * @see #calConnectRatioABOOP_MPI(boolean, MPI.Comm, int, double, double, int, double, int, double, int)
     * @see MPI.Comm#WORLD
     */
    public IVector calConnectRatioABOOP_MPI(int aL, double aConnectThreshold) throws MPIException {return calConnectRatioABOOP_MPI(MPI.Comm.WORLD, aL, aConnectThreshold);}
    
    
    /**
     * 具体通过 {@link #calConnectCountBOOP(int, double, double)}
     * 且 {@code l = 6} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.5
     * @param aSolidThreshold 用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值，默认为 7
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量，按照原子数据中的原子排序
     * @see ILogicalVector
     * @see #calConnectCountBOOP(int, double, double, int)
     */
    public ILogicalVector checkSolidConnectCount6(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {IVector tConnectCount = calConnectCountBOOP(6, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectCount.greaterOrEqual(aSolidThreshold); VectorCache.returnVec(tConnectCount); return tIsSolid;}
    /**
     * 具体通过 {@link #calConnectCountBOOP(int, double, double)}
     * 且 {@code l = 6} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 通过 {@link #checkSolidConnectCount6(double, int, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.2977970">
     * Accurate determination of crystal structures based on averaged local bond order parameters</a>
     *
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.5
     * @param aSolidThreshold 用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值，默认为 7
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return 最后判断得到是否是固体组成的逻辑向量，按照原子数据中的原子排序
     * @see ILogicalVector
     * @see #calConnectCountBOOP(int, double, double)
     */
    public ILogicalVector checkSolidConnectCount6(double aConnectThreshold, int aSolidThreshold, double aRNearest) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold, aRNearest, -1);}
    /**
     * @return {@code checkSolidConnectCount6(aConnectThreshold, aSolidThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #checkSolidConnectCount6(double, int, double)
     * @see CS#R_NEAREST_MUL
     */
    public ILogicalVector checkSolidConnectCount6(double aConnectThreshold, int aSolidThreshold) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code checkSolidConnectCount6(0.5, 7, unitLen()*R_NEAREST_MUL)}
     * @see #checkSolidConnectCount6(double, int, double)
     * @see CS#R_NEAREST_MUL
     */
    public ILogicalVector checkSolidConnectCount6() {return checkSolidConnectCount6(0.5, 7);}
    
    /**@deprecated use {@link #checkSolidConnectCount6(double, int, double, int)} */
    @Deprecated public ILogicalVector checkSolidQ6(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold, aRNearest, aNnn);}
    /**@deprecated use {@link #checkSolidConnectCount6(double, int, double)} */
    @Deprecated public ILogicalVector checkSolidQ6(double aConnectThreshold, int aSolidThreshold, double aRNearest) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold, aRNearest);}
    /**@deprecated use {@link #checkSolidConnectCount6(double, int)} */
    @Deprecated public ILogicalVector checkSolidQ6(double aConnectThreshold, int aSolidThreshold) {return checkSolidConnectCount6(aConnectThreshold, aSolidThreshold);}
    /**@deprecated use {@link #checkSolidConnectCount6()} */
    @Deprecated public ILogicalVector checkSolidQ6() {return checkSolidConnectCount6();}
    
    /**
     * 具体通过 {@link #calConnectRatioBOOP(int, double, double)}
     * 且 {@code l = 6} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     *
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.58
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量，按照原子数据中的原子排序
     * @see ILogicalVector
     * @see #calConnectRatioBOOP(int, double, double, int)
     */
    public ILogicalVector checkSolidConnectRatio6(double aConnectThreshold, double aRNearest, int aNnn) {IVector tConnectRatio = calConnectRatioBOOP(6, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectRatio.greaterOrEqual(0.5); VectorCache.returnVec(tConnectRatio); return tIsSolid;}
    /**
     * 具体通过 {@link #calConnectRatioBOOP(int, double, double)}
     * 且 {@code l = 6} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 通过 {@link #checkSolidConnectRatio6(double, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     *
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.58
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return 最后判断得到是否是固体组成的逻辑向量，按照原子数据中的原子排序
     * @see ILogicalVector
     * @see #calConnectRatioBOOP(int, double, double)
     */
    public ILogicalVector checkSolidConnectRatio6(double aConnectThreshold, double aRNearest) {return checkSolidConnectRatio6(aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code checkSolidConnectRatio6(aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #checkSolidConnectRatio6(double, double)
     * @see CS#R_NEAREST_MUL
     */
    public ILogicalVector checkSolidConnectRatio6(double aConnectThreshold) {return checkSolidConnectRatio6(aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code checkSolidConnectRatio6(0.58, unitLen()*R_NEAREST_MUL)}
     * @see #checkSolidConnectRatio6(double, double)
     * @see CS#R_NEAREST_MUL
     */
    public ILogicalVector checkSolidConnectRatio6() {return checkSolidConnectRatio6(0.58);}
    
    /** @see #checkSolidConnectRatio6(double, double, int) */
    @VisibleForTesting public ILogicalVector checkSolidS6(double aConnectThreshold, double aRNearest, int aNnn) {return checkSolidConnectRatio6(aConnectThreshold, aRNearest, aNnn);}
    /** @see #checkSolidConnectRatio6(double, double) */
    @VisibleForTesting public ILogicalVector checkSolidS6(double aConnectThreshold, double aRNearest) {return checkSolidConnectRatio6(aConnectThreshold, aRNearest);}
    /** @see #checkSolidConnectRatio6(double) */
    @VisibleForTesting public ILogicalVector checkSolidS6(double aConnectThreshold) {return checkSolidConnectRatio6(aConnectThreshold);}
    /** @see #checkSolidConnectRatio6() */
    @VisibleForTesting public ILogicalVector checkSolidS6() {return checkSolidConnectRatio6();}
    
    
    /**
     * 具体通过 {@link #calConnectCountBOOP(int, double, double)}
     * 且 {@code l = 4} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl </a>
     *
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.35
     * @param aSolidThreshold 用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值，默认为 6
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量，按照原子数据中的原子排序
     * @see ILogicalVector
     * @see #calConnectRatioBOOP(int, double, double)
     */
    public ILogicalVector checkSolidConnectCount4(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {IVector tConnectCount = calConnectCountBOOP(4, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectCount.greaterOrEqual(aSolidThreshold); VectorCache.returnVec(tConnectCount); return tIsSolid;}
    /**
     * 具体通过 {@link #calConnectCountBOOP(int, double, double)}
     * 且 {@code l = 4} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 通过 {@link #checkSolidConnectCount4(double, int, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     * <p>
     * Reference:
     * <a href="https://doi.org/10.1063/1.1896348">
     * Rate of homogeneous crystal nucleation in molten NaCl </a>
     *
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.35
     * @param aSolidThreshold 用来根据最近邻原子中，连接数大于或等于此值则认为是固体的阈值，默认为 6
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return 最后判断得到是否是固体组成的逻辑向量，按照原子数据中的原子排序
     * @see ILogicalVector
     * @see #calConnectRatioBOOP(int, double, double)
     */
    public ILogicalVector checkSolidConnectCount4(double aConnectThreshold, int aSolidThreshold, double aRNearest) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold, aRNearest, -1);}
    /**
     * @return {@code checkSolidConnectCount4(aConnectThreshold, aSolidThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #checkSolidConnectCount4(double, int, double)
     * @see CS#R_NEAREST_MUL
     */
    public ILogicalVector checkSolidConnectCount4(double aConnectThreshold, int aSolidThreshold) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code checkSolidConnectCount4(0.35, 6, unitLen()*R_NEAREST_MUL)}
     * @see #checkSolidConnectCount4(double, int, double)
     * @see CS#R_NEAREST_MUL
     */
    public ILogicalVector checkSolidConnectCount4() {return checkSolidConnectCount4(0.35, 6);}
    
    /**@deprecated use {@link #checkSolidConnectCount4(double, int, double, int)} */
    @Deprecated public ILogicalVector checkSolidQ4(double aConnectThreshold, int aSolidThreshold, double aRNearest, int aNnn) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold, aRNearest, aNnn);}
    /**@deprecated use {@link #checkSolidConnectCount4(double, int, double)} */
    @Deprecated public ILogicalVector checkSolidQ4(double aConnectThreshold, int aSolidThreshold, double aRNearest) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold, aRNearest);}
    /**@deprecated use {@link #checkSolidConnectCount4(double, int)} */
    @Deprecated public ILogicalVector checkSolidQ4(double aConnectThreshold, int aSolidThreshold) {return checkSolidConnectCount4(aConnectThreshold, aSolidThreshold);}
    /**@deprecated use {@link #checkSolidConnectCount4()} */
    @Deprecated public ILogicalVector checkSolidQ4() {return checkSolidConnectCount4();}
    
    /**
     * 具体通过 {@link #calConnectRatioBOOP(int, double, double)}
     * 且 {@code l = 4} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     *
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.50
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @param aNnn 最大的最近邻数目（Number of Nearest Neighbor list）。默认不做限制
     * @return 最后判断得到是否是固体组成的逻辑向量
     * @see ILogicalVector
     * @see #calConnectRatioBOOP(int, double, double, int)
     */
    public ILogicalVector checkSolidConnectRatio4(double aConnectThreshold, double aRNearest, int aNnn) {IVector tConnectRatio = calConnectRatioBOOP(4, aConnectThreshold, aRNearest, aNnn); ILogicalVector tIsSolid = tConnectRatio.greaterOrEqual(0.5); VectorCache.returnVec(tConnectRatio); return tIsSolid;}
    /**
     * 具体通过 {@link #calConnectRatioBOOP(int, double, double)}
     * 且 {@code l = 4} 来检测结构中类似固体的部分，
     * 输出结果为按照输入原子顺序排列的布尔向量，true 表示判断为类似固体；
     * <p>
     * 通过 {@link #checkSolidConnectRatio4(double, double, int)}
     * 考虑 aNnn 可以增加结果的稳定性，但是会增加性能开销
     *
     * @author liqa
     * @param aConnectThreshold 用来判断两个原子是否是相连接的阈值，默认为 0.50
     * @param aRNearest 用来搜索的最近邻半径。默认为 {@link CS#R_NEAREST_MUL} 倍单位长度
     * @return 最后判断得到是否是固体组成的逻辑向量
     * @see ILogicalVector
     * @see #calConnectRatioBOOP(int, double, double, int)
     */
    public ILogicalVector checkSolidConnectRatio4(double aConnectThreshold, double aRNearest) {return checkSolidConnectRatio4(aConnectThreshold, aRNearest, -1);}
    /**
     * @return {@code checkSolidConnectRatio4(aConnectThreshold, unitLen()*R_NEAREST_MUL)}
     * @see #checkSolidConnectRatio4(double, double)
     * @see CS#R_NEAREST_MUL
     */
    public ILogicalVector checkSolidConnectRatio4(double aConnectThreshold) {return checkSolidConnectRatio4(aConnectThreshold, mUnitLen*R_NEAREST_MUL);}
    /**
     * @return {@code checkSolidConnectRatio4(0.58, unitLen()*R_NEAREST_MUL)}
     * @see #checkSolidConnectRatio4(double, double)
     * @see CS#R_NEAREST_MUL
     */
    public ILogicalVector checkSolidConnectRatio4() {return checkSolidConnectRatio4(0.50);}
    
    /** @see #checkSolidConnectRatio4(double, double, int) */
    @VisibleForTesting public ILogicalVector checkSolidS4(double aConnectThreshold, double aRNearest, int aNnn) {return checkSolidConnectRatio4(aConnectThreshold, aRNearest, aNnn);}
    /** @see #checkSolidConnectRatio4(double, double) */
    @VisibleForTesting public ILogicalVector checkSolidS4(double aConnectThreshold, double aRNearest) {return checkSolidConnectRatio4(aConnectThreshold, aRNearest);}
    /** @see #checkSolidConnectRatio4(double) */
    @VisibleForTesting public ILogicalVector checkSolidS4(double aConnectThreshold) {return checkSolidConnectRatio4(aConnectThreshold);}
    /** @see #checkSolidConnectRatio4() */
    @VisibleForTesting public ILogicalVector checkSolidS4() {return checkSolidConnectRatio4();}
}
