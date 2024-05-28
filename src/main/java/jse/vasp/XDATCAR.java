package jse.vasp;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jse.atom.*;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractListWrapper;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.matrix.Matrices;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

import static jse.code.CS.MASS;
import static jse.code.CS.ZL_STR;


/**
 * @author liqa
 * <p> vasp 读取输出的多帧原子位置格式，每帧为 {@link POSCAR} </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 暂时不支持边界条件设置 </p>
 * <p> 暂时不考虑 Direct configuration 间距不为 1 的情况，因此 MFPC 不会统计两帧之间跳过的部分 </p>
 * <p> 返回的 {@link POSCAR} 共享原子位置但是不共享 mSelectiveDynamics（除了原子位置其余是否共享属于未定义）</p>
 * <p> 现在不再继承 {@link List}，因为 List 的接口太脏了 </p>
 * <p> 并且现在不再继承 {@link IAtomData}，如果需要使用单个 XDATCAR 直接使用 {@link POSCAR} </p>
 */
public class XDATCAR extends AbstractListWrapper<POSCAR, IAtomData, IMatrix> implements IVaspCommonData {
    public final static String DEFAULT_COMMENT = "VASP_XDATCAR_FROM_JSE";
    
    /** 这里统一存放通用数据保证所有帧这些一定是相同的 */
    private @Nullable String mComment;
    private String @Nullable[] mTypeNames;
    private IIntVector mAtomNumbers;
    private VaspBox mBox;
    /** 保存一份 id 列表，这样在 lmpdat 转为 poscar 时会继续保留 id 信息，XDATCAR 认为不能进行修改 */
    private final @Nullable IIntVector mIDs;
    private final @Nullable @Unmodifiable Map<Integer, Integer> mId2Index; // 原子的 id 转为存储在 AtomDataXYZ 的指标 index
    
    private boolean mIsCartesian; // 如果是 Cartesian 则是没有进行缩放的
    
    /** 用于通过字符获取每个种类的粒子数，考虑了可能有相同 key 的情况 */
    private final @NotNull Multimap<String, Integer> mKey2Type;
    
    XDATCAR(@Nullable String aComment, VaspBox aBox, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, List<IMatrix> aDirects, boolean aIsCartesian, @Nullable IIntVector aIDs) {
        super(aDirects);
        mComment = aComment;
        mTypeNames = aTypeNames;
        mAtomNumbers = aAtomNumbers;
        mBox = aBox;
        mIsCartesian = aIsCartesian;
        mIDs = aIDs;
        
        if (mIDs == null) {
            mId2Index = null;
        } else {
            int tSize = mIDs.size();
            mId2Index = new HashMap<>(tSize);
            for (int idx = 0; idx < tSize; ++idx) mId2Index.put(mIDs.get(idx), idx);
        }
        
        mKey2Type = ArrayListMultimap.create();
        if (mTypeNames != null) {
            int rType = 0;
            for (String tKey : mTypeNames) {
                ++rType;
                mKey2Type.put(tKey, rType);
            }
        }
    }
    XDATCAR(@Nullable String aComment, VaspBox aBox, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, IMatrix aFirstDirect, int aInitSize, boolean aIsCartesian, @Nullable IIntVector aIDs) {
        this(aComment, aBox, aTypeNames, aAtomNumbers, new ArrayList<>(aInitSize), aIsCartesian, aIDs);
        mList.add(aFirstDirect);
    }
    
    /** AbstractListWrapper stuffs */
    @Override protected final IMatrix toInternal_(IAtomData aAtomData) {return getDirect_(aAtomData);}
    @Override protected final POSCAR toOutput_(IMatrix aDirect) {return new POSCAR(this, false, aDirect);}
    
    
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    public XDATCAR append(IAtomData aAtomData) {return (XDATCAR)super.append(aAtomData);}
    public XDATCAR appendAll(Collection<? extends IAtomData> aAtomDataList) {return (XDATCAR)super.appendAll(aAtomDataList);}
    public XDATCAR appendFile(String aFilePath) throws IOException {
        // 原则上只有 box 完全相同才可以添加，
        // 现在简单处理，检测不同时会输出警告然后强行添加，不再进行任何转换
        final XDATCAR tXDATCAR = read(aFilePath);
        // 转换 mIsCartesian 使其一致
        if (mIsCartesian) tXDATCAR.setCartesian();
        else tXDATCAR.setDirect();
        // 检测 box 是否完全一致，如果不一致则输出警告
        if (mBox.isPrism() != tXDATCAR.mBox.isPrism()) {
            System.err.println("WARNING: Box type of appended XDATCAR ("+(tXDATCAR.mBox.isPrism() ? "prism" : "orthogonal")+") is different from the original box type ("+(mBox.isPrism() ? "prism" : "orthogonal")+")");
        } else
        if (!MathEX.Code.numericEqual(mBox.scale(), tXDATCAR.mBox.scale())
            || !MathEX.Code.numericEqual(mBox.iax(), tXDATCAR.mBox.iax())
            || !MathEX.Code.numericEqual(mBox.iay(), tXDATCAR.mBox.iay())
            || !MathEX.Code.numericEqual(mBox.iaz(), tXDATCAR.mBox.iaz())
            || !MathEX.Code.numericEqual(mBox.ibx(), tXDATCAR.mBox.ibx())
            || !MathEX.Code.numericEqual(mBox.iby(), tXDATCAR.mBox.iby())
            || !MathEX.Code.numericEqual(mBox.ibz(), tXDATCAR.mBox.ibz())
            || !MathEX.Code.numericEqual(mBox.icx(), tXDATCAR.mBox.icx())
            || !MathEX.Code.numericEqual(mBox.icy(), tXDATCAR.mBox.icy())
            || !MathEX.Code.numericEqual(mBox.icz(), tXDATCAR.mBox.icz())) {
            System.err.println("WARNING: Box of appended XDATCAR is different from the original box");
            System.err.println("Appended:");
            System.err.println(tXDATCAR.mBox);
            System.err.println("Original:");
            System.err.println(mBox);
        }
        mList.addAll(tXDATCAR.mList);
        return this;
    }
    /** groovy stuffs */
    @Override public XDATCAR leftShift(IAtomData aAtomData) {return (XDATCAR)super.leftShift(aAtomData);}
    @Override public XDATCAR leftShift(Collection<? extends IAtomData> aAtomDataList) {return (XDATCAR)super.leftShift(aAtomDataList);}
    
    
    /** 对于 XDATCAR 提供额外的实用接口 */
    public @Nullable String typeName(int aType) {return mTypeNames==null ? null : mTypeNames[aType-1];}
    public double mass(int aType) {
        @Nullable String tTypeName = typeName(aType);
        return tTypeName==null ? Double.NaN : MASS.getOrDefault(tTypeName, Double.NaN);
    }
    public int atomNumber(String aKey) {
        int rAtomNum = 0;
        for (int tType : mKey2Type.get(aKey)) rAtomNum += atomNumber(tType);
        return rAtomNum;
    }
    public int atomNumber(int aType) {return mAtomNumbers.get(aType-1);}
    /** @deprecated use {@link #atomNumber} or {@link #natoms} */ @Deprecated public final int atomNum(String aType) {return atomNumber(aType);}
    /** @deprecated use {@link #atomNumber} or {@link #natoms} */ @Deprecated public final int atomNum(int aType) {return atomNumber(aType);}
    /** 提供简写版本 */
    @VisibleForTesting public final int natoms(String aType) {return atomNumber(aType);}
    @VisibleForTesting public final int natoms(int aType) {return atomNumber(aType);}
    
    public @Override @Nullable String comment() {return mComment;}
    public @Override String @Nullable[] typeNames() {return mTypeNames;}
    public @Override IIntVector atomNumbers() {return mAtomNumbers;}
    public List<IMatrix> directs() {return mList;}
    public @Override boolean isCartesian() {return mIsCartesian;}
    public @Override @Nullable IIntVector ids() {return mIDs;}
    
    /** @deprecated use {@link #box} */ @Deprecated public VaspBox vaspBox() {return box();}
    /** @deprecated use {@link VaspBox#scale} */ @Deprecated public double vaspBoxScale() {return mBox.scale();}
    /** @deprecated use {@code !}{@link #isPrism} */ @Deprecated public boolean isDiagBox() {return !isPrism();}
    
    /** boxScale stuffs */
    public double boxScale() {return mBox.scale();}
    public XDATCAR setBoxScale(double aBoxScale) {mBox.setScale(aBoxScale); return this;}
    /** Groovy stuffs */
    @VisibleForTesting public double getBoxScale() {return mBox.getScale();}
    
    /** 对于 XDATCAR 这些接口也同样可以获取到 */
    public VaspBox box() {return mBox;}
    public double volume() {return mBox.volume();}
    public boolean isPrism() {return mBox.isPrism();}
    public boolean isLmpStyle() {return mBox.isLmpStyle();}
    public int atomNumber() {return mAtomNumbers.sum();}
    public int atomTypeNumber() {return mAtomNumbers.size();}
    /** @deprecated use {@link #atomNumber} or {@link #natoms} */ @Deprecated public final int atomNum() {return atomNumber();}
    /** @deprecated use {@link #atomNumber} or {@link #natoms} */ @Deprecated public final int atomTypeNum() {return atomTypeNumber();}
    /** 提供简写版本 */
    @VisibleForTesting public final int natoms() {return atomNumber();}
    @VisibleForTesting public final int ntypes() {return atomTypeNumber();}
    
    
    /** 支持直接修改 TypeNames，只会增大种类数，不会减少 */
    public XDATCAR setTypeNames(String... aTypeNames) {
        if (aTypeNames==null || aTypeNames.length==0) {
            mTypeNames = null;
            validKey2Type_();
            return this;
        }
        if (mTypeNames==null || aTypeNames.length>mTypeNames.length) mTypeNames = Arrays.copyOf(aTypeNames, aTypeNames.length);
        else System.arraycopy(aTypeNames, 0, mTypeNames, 0, aTypeNames.length);
        if (aTypeNames.length > mAtomNumbers.size()) {
            IIntVector oAtomNumbers = mAtomNumbers;
            mAtomNumbers = IntVector.zeros(aTypeNames.length);
            mAtomNumbers.subVec(0, oAtomNumbers.size()).fill(oAtomNumbers);
        }
        validKey2Type_();
        return this;
    }
    public XDATCAR setNoTypeName() {return setTypeNames(ZL_STR);}
    /** 设置原子种类数目 */
    public XDATCAR setAtomTypeNumber(int aAtomTypeNum) {
        int oTypeNum = mAtomNumbers.size();
        if (aAtomTypeNum == oTypeNum) return this;
        if (aAtomTypeNum < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            mAtomNumbers.set(aAtomTypeNum-1, mAtomNumbers.subVec(aAtomTypeNum-1, mAtomNumbers.size()).sum());
            mAtomNumbers = mAtomNumbers.subVec(0, aAtomTypeNum).copy();
            validKey2Type_();
            return this;
        }
        if (mTypeNames!=null && mTypeNames.length<aAtomTypeNum) {
            String[] rTypeNames = new String[aAtomTypeNum];
            System.arraycopy(mTypeNames, 0, rTypeNames, 0, mTypeNames.length);
            for (int tType = mTypeNames.length+1; tType <= aAtomTypeNum; ++tType) rTypeNames[tType-1] = "T" + tType;
            mTypeNames = rTypeNames;
        }
        IIntVector oAtomNumbers = mAtomNumbers;
        mAtomNumbers = IntVector.zeros(aAtomTypeNum);
        mAtomNumbers.subVec(0, oTypeNum).fill(oAtomNumbers);
        validKey2Type_();
        return this;
    }
    
    void validKey2Type_() {
        mKey2Type.clear();
        if (mTypeNames != null) {
            int rType = 0;
            for (String tKey : mTypeNames) {
                ++rType;
                mKey2Type.put(tKey, rType);
            }
        }
    }
    
    public XDATCAR setComment(@Nullable String aComment) {mComment = aComment; return this;}
    
    /** Groovy stuffs */
    @VisibleForTesting public String @Nullable[] getTypeNames() {return mTypeNames;}
    @VisibleForTesting public String getComment() {return mComment;}
    
    /** Cartesian 和 Direct 来回转换 */
    public XDATCAR setCartesian() {
        if (mIsCartesian) return this;
        // 这里绕过 scale 直接处理
        for (IMatrix tDirect : mList) {
            if (isPrism()) {
                IMatrix tIABC = mBox.iabc();
                tDirect.operation().matmul2this(tIABC);
                // cartesian 其实也需要考虑计算误差带来的出边界的问题
                final double tNorm = tIABC.asVecCol().operation().stat((norm, v) -> norm+Math.abs(v));
                tDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON*tNorm ? 0.0 : v);
            } else {
                tDirect.col(0).multiply2this(mBox.iax());
                tDirect.col(1).multiply2this(mBox.iby());
                tDirect.col(2).multiply2this(mBox.icz());
            }
        }
        mIsCartesian = true;
        return this;
    }
    public XDATCAR setDirect() {
        if (!mIsCartesian) return this;
        // 这里绕过 scale 直接处理
        IMatrix tInvIABC = mBox.inviabc();
        for (IMatrix tDirect : mList) {
            if (isPrism()) {
                tDirect.operation().matmul2this(tInvIABC);
                // direct 需要考虑计算误差带来的出边界的问题
                tDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON ? 0.0 : v);
            } else {
                tDirect.col(0).div2this(mBox.iax());
                tDirect.col(1).div2this(mBox.iby());
                tDirect.col(2).div2this(mBox.icz());
            }
        }
        mIsCartesian = false;
        return this;
    }
    /** 修改模拟盒类型 */
    public XDATCAR setBoxNormal() {
        if (!isPrism()) return this;
        VaspBox oBox = mBox;
        mBox = new VaspBox(mBox);
        // 如果是 direct 则不用转换数据
        if (!mIsCartesian) return this;
        // 如果原本的斜方模拟盒不存在斜方数据则直接返回
        if (MathEX.Code.numericEqual(oBox.iay(), 0.0) && MathEX.Code.numericEqual(oBox.iaz(), 0.0)
         && MathEX.Code.numericEqual(oBox.ibx(), 0.0) && MathEX.Code.numericEqual(oBox.ibz(), 0.0)
         && MathEX.Code.numericEqual(oBox.icx(), 0.0) && MathEX.Code.numericEqual(oBox.icy(), 0.0)) return this;
        // 否则将原子进行线性变换，这里绕过 scale 直接处理
        IMatrix oInvIABC = oBox.inviabc();
        for (IMatrix tDirect : mList) {
            tDirect.operation().matmul2this(oInvIABC);
            // 考虑计算误差带来的出边界的问题
            tDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON ? 0.0 : v);
            // 手动转换回到 cartesian
            tDirect.col(0).multiply2this(mBox.iax());
            tDirect.col(1).multiply2this(mBox.iby());
            tDirect.col(2).multiply2this(mBox.icz());
        }
        return this;
    }
    public XDATCAR setBoxPrism() {return setBoxPrism(0.0, 0.0, 0.0);}
    public XDATCAR setBoxPrism(double aIXY, double aIXZ, double aIYZ) {return setBoxPrism(0.0, 0.0, aIXY, 0.0, aIXZ, aIYZ);}
    public XDATCAR setBoxPrism(double aIAy, double aIAz, double aIBx, double aIBz, double aICx, double aICy) {
        VaspBox oBox = mBox;
        mBox = new VaspBoxPrism(mBox, aIAy, aIAz, aIBx, aIBz, aICx, aICy);
        // 如果是 direct 则不用转换数据
        if (!mIsCartesian) return this;
        // 现在必须要求倾斜因子相同才可以跳过设置
        if (MathEX.Code.numericEqual(oBox.iay(), aIAy) && MathEX.Code.numericEqual(oBox.iaz(), aIAz)
         && MathEX.Code.numericEqual(oBox.ibx(), aIBx) && MathEX.Code.numericEqual(oBox.ibz(), aIBz)
         && MathEX.Code.numericEqual(oBox.icx(), aICx) && MathEX.Code.numericEqual(oBox.icy(), aICy)) return this;
        // 否则将原子进行线性变换，这里绕过 scale 直接处理
        IMatrix oInvIABC = oBox.inviabc();
        IMatrix tIABC = mBox.iabc();
        final double tNorm = tIABC.asVecCol().operation().stat((norm, v) -> norm+Math.abs(v));
        for (IMatrix tDirect : mList) {
            if (oBox.isPrism()) {
                tDirect.operation().matmul2this(oInvIABC);
                // 考虑计算误差带来的出边界的问题
                tDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON ? 0.0 : v);
            } else {
                tDirect.col(0).div2this(oBox.iax());
                tDirect.col(1).div2this(oBox.iby());
                tDirect.col(2).div2this(oBox.icz());
            }
            // 手动转换回到 cartesian
            tDirect.operation().matmul2this(tIABC);
            // cartesian 其实也需要考虑计算误差带来的出边界的问题
            tDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON*tNorm ? 0.0 : v);
        }
        return this;
    }
    /** 调整模拟盒的 xyz 长度 */
    public XDATCAR setBoxXYZ(double aIX, double aIY, double aIZ) {
        VaspBox oBox = mBox;
        mBox = oBox.isPrism() ?
            new VaspBoxPrism(aIX, oBox.iay(), oBox.iaz(), oBox.ibx(), aIY, oBox.ibz(), oBox.icx(), oBox.icy(), aIZ, oBox.scale()) :
            new VaspBox(aIX, aIY, aIZ, oBox.scale());
        // 如果是 direct 则不用转换数据
        if (!mIsCartesian) return this;
        // 现在必须要求 xyz 长度相同才可以跳过设置
        if (MathEX.Code.numericEqual(oBox.iax(), aIX) && MathEX.Code.numericEqual(oBox.iby(), aIY) && MathEX.Code.numericEqual(oBox.icz(), aIZ)) return this;
        // 否则将原子进行线性变换，这里绕过 scale 直接处理
        if (oBox.isPrism()) {
            IMatrix oInvIABC = oBox.inviabc();
            IMatrix tIABC = mBox.iabc();
            final double tNorm = tIABC.asVecCol().operation().stat((norm, v) -> norm+Math.abs(v));
            for (IMatrix tDirect : mList) {
                tDirect.operation().matmul2this(oInvIABC);
                // 考虑计算误差带来的出边界的问题
                tDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON ? 0.0 : v);
                // 手动转换回到 cartesian
                tDirect.operation().matmul2this(tIABC);
                // cartesian 其实也需要考虑计算误差带来的出边界的问题
                tDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON*tNorm ? 0.0 : v);
            }
        } else {
            for (IMatrix tDirect : mList) {
                tDirect.col(0).div2this(oBox.iax());
                tDirect.col(1).div2this(oBox.iby());
                tDirect.col(2).div2this(oBox.icz());
                // 手动转换回到 cartesian
                tDirect.col(0).multiply2this(mBox.iax());
                tDirect.col(1).multiply2this(mBox.iby());
                tDirect.col(2).multiply2this(mBox.icz());
            }
        }
        return this;
    }
    /** 密度归一化 */
    public XDATCAR setDenseNormalized() {
        double tScale = MathEX.Fast.cbrt(volume() / atomNumber());
        // 直接通过调整 boxScale 来实现
        mBox.setScale(mBox.scale() / tScale);
        return this;
    }
    
    
    /** 截断开头一部分, 返回自身来支持链式调用 */
    public XDATCAR cutFront(int aLength) {
        if (aLength <= 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        List<IMatrix> oList = mList;
        mList = new ArrayList<>(oList.size() - aLength);
        Iterator<IMatrix> it = oList.listIterator(aLength);
        while (it.hasNext()) mList.add(it.next());
        return this;
    }
    /** 截断结尾一部分, 返回自身来支持链式调用 */
    public XDATCAR cutBack(int aLength) {
        if (aLength <= 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        for (int i = 0; i < aLength; ++i) UT.Code.removeLast(mList);
        return this;
    }
    
    /** 拷贝一份 XDATCAR */
    public XDATCAR copy() {
        List<IMatrix> rDirects = new ArrayList<>(mList.size());
        for (IMatrix tDirect : mList) rDirects.add(tDirect.copy());
        return new XDATCAR(mComment, mBox.copy(), POSCAR.copyTypeNames(mTypeNames), mAtomNumbers.copy(), rDirects, mIsCartesian, POSCAR.copyIDs(mIDs));
    }
    
    /// 创建 XDATCAR
    /** 从 IAtomData 来创建，对于 XDATCAR 可以支持容器的 aAtomData */
    public static XDATCAR fromAtomData(IAtomData aAtomData) {return fromAtomData(aAtomData, (aAtomData instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomData).typeNames() : ZL_STR);}
    public static XDATCAR fromAtomData(IAtomData aAtomData, String... aTypeNames) {return fromAtomData_(aAtomData, 1, aTypeNames);}
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList, (aAtomDataList instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomDataList).typeNames() : ZL_STR);}
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, String... aTypeNames) {return fromAtomDataList_(aAtomDataList, 1, aTypeNames);}
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList, (aAtomDataList instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomDataList).typeNames() : ZL_STR);}
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, String... aTypeNames) {return fromAtomDataList_(aAtomDataList, aAtomDataList.size(), aTypeNames);}
    static XDATCAR fromAtomData_(IAtomData aAtomData, int aInitSize, String[] aTypeNames) {
        if (aTypeNames==null) aTypeNames = ZL_STR;
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 Direct
        if (aAtomData instanceof POSCAR) {
            // POSCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            POSCAR tPOSCAR = (POSCAR)aAtomData;
            return new XDATCAR(tPOSCAR.comment(), tPOSCAR.box().copy(), POSCAR.copyTypeNames(tPOSCAR.typeNames()), tPOSCAR.atomNumbers().copy(), tPOSCAR.direct().copy(), aInitSize, tPOSCAR.isCartesian(), POSCAR.copyIDs(tPOSCAR.ids())).setTypeNames(aTypeNames);
        } else {
            // 一般的情况，这里直接遍历 atoms 来创建，这里需要按照 type 来排序
            IIntVector rIDs = IntVector.zeros(aAtomData.atomNumber());
            int tAtomTypeNum = Math.max(aAtomData.atomTypeNumber(), aTypeNames.length);
            IIntVector rAtomNumbers = IntVector.zeros(tAtomTypeNum);
            IMatrix rDirect = Matrices.zeros(aAtomData.atomNumber(), 3);
            int tIdx = 0;
            for (int tTypeMM = 0; tTypeMM < tAtomTypeNum; ++tTypeMM) {
                for (IAtom tAtom : aAtomData.atoms()) if (tAtom.type() == tTypeMM+1) {
                    rAtomNumbers.increment(tTypeMM);
                    rDirect.set(tIdx, 0, tAtom.x());
                    rDirect.set(tIdx, 1, tAtom.y());
                    rDirect.set(tIdx, 2, tAtom.z());
                    rIDs.set(tIdx, tAtom.id());
                    ++tIdx;
                }
            }
            // 现在转换会直接转成 Cartesian 来避免计算中的浮点误差
            IBox tBox = aAtomData.box();
            VaspBox rBox = tBox.isPrism() ?
                new VaspBoxPrism(tBox.ax(), tBox.ay(), tBox.az(), tBox.bx(), tBox.by(), tBox.bz(), tBox.cx(), tBox.cy(), tBox.cz()) :
                new VaspBox(tBox.x(), tBox.y(), tBox.z());
            if (aTypeNames.length!=0 && aTypeNames.length<tAtomTypeNum) {
                String[] rTypeNames = new String[tAtomTypeNum];
                System.arraycopy(aTypeNames, 0, rTypeNames, 0, aTypeNames.length);
                for (int tType = aTypeNames.length+1; tType <= tAtomTypeNum; ++tType) rTypeNames[tType-1] = "T" + tType;
                aTypeNames = rTypeNames;
            }
            return new XDATCAR(null, rBox, POSCAR.copyTypeNames(aTypeNames), rAtomNumbers, rDirect, aInitSize, true, rIDs);
        }
    }
    static XDATCAR fromAtomDataList_(Iterable<? extends IAtomData> aAtomDataList, int aInitSize, String[] aTypeNames) {
        // 直接不支持空的创建来简化实现的代码
        if (aAtomDataList == null) throw new IllegalArgumentException("XDATCAR do NOT support empty AtomDataList");
        Iterator<? extends IAtomData> it = aAtomDataList.iterator();
        if (!it.hasNext()) throw new IllegalArgumentException("XDATCAR do NOT support empty AtomDataList");
        IAtomData first = it.next();
        final XDATCAR rXDATCAR = fromAtomData_(first, aInitSize, ((aTypeNames==null || aTypeNames==ZL_STR) && (first instanceof IVaspCommonData)) ? ((IVaspCommonData)first).typeNames() : aTypeNames);
        it.forEachRemaining(rXDATCAR::append);
        return rXDATCAR;
    }
    
    IMatrix getDirect_(IAtomData aAtomData) {
        // 这里只考虑一般的情况，这里直接遍历 atoms 来创建，
        // 这里直接按照 mIDs 的顺序进行排序，不考虑 type 发生改变的情况
        IBox tBox = aAtomData.box();
        int tAtomNum = aAtomData.atomNumber();
        IMatrix rDirect = Matrices.zeros(tAtomNum, 3);
        XYZ tBuf = new XYZ();
        for (IAtom tAtom : aAtomData.atoms()) {
            int tIdx = mId2Index==null ? tAtom.id()-1 : mId2Index.get(tAtom.id());
            if (mIsCartesian) {
                rDirect.set(tIdx, 0, tAtom.x());
                rDirect.set(tIdx, 1, tAtom.y());
                rDirect.set(tIdx, 2, tAtom.z());
            } else
            if (!tBox.isPrism()) {
                rDirect.set(tIdx, 0, tAtom.x()/tBox.x());
                rDirect.set(tIdx, 1, tAtom.y()/tBox.y());
                rDirect.set(tIdx, 2, tAtom.z()/tBox.z());
            } else {
                tBuf.setXYZ(tAtom);
                tBox.toDirect(tBuf);
                rDirect.set(tIdx, 0, tBuf.mX);
                rDirect.set(tIdx, 1, tBuf.mY);
                rDirect.set(tIdx, 2, tBuf.mZ);
            }
        }
        rDirect.div2this(mBox.scale());
        return rDirect;
    }
    /** 对于 matlab 调用的兼容 */
    public static XDATCAR fromAtomData_compat(Object[] aAtomDataArray) {
        return fromAtomData_compat(aAtomDataArray, (String[])null);
    }
    public static XDATCAR fromAtomData_compat(Object[] aAtomDataArray, String... aTypeNames) {
        return fromAtomDataList(AbstractCollections.map(AbstractCollections.filter(AbstractCollections.from(aAtomDataArray), atomData -> (atomData instanceof IAtomData)), obj -> (IAtomData)obj), aTypeNames);
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static XDATCAR of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static XDATCAR of(IAtomData aAtomData, String... aTypeNames) {return fromAtomData(aAtomData, aTypeNames);}
    public static XDATCAR of(Iterable<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    public static XDATCAR of(Iterable<? extends IAtomData> aAtomDataList, String... aTypeNames) {return fromAtomDataList(aAtomDataList, aTypeNames);}
    public static XDATCAR of(Collection<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    public static XDATCAR of(Collection<? extends IAtomData> aAtomDataList, String... aTypeNames) {return fromAtomDataList(aAtomDataList, aTypeNames);}
    /** 再提供一个 IListWrapper 的接口保证 XDATCAR 也能输入 */
    public static XDATCAR of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList) {return fromAtomDataList(aAtomDataList.asList());}
    public static XDATCAR of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, String... aTypeNames) {return fromAtomDataList(aAtomDataList.asList(), aTypeNames);}
    /** matlab stuffs */
    public static XDATCAR of_compat(Object[] aAtomDataArray) {return fromAtomData_compat(aAtomDataArray);}
    public static XDATCAR of_compat(Object[] aAtomDataArray, String... aTypeNames) {return fromAtomData_compat(aAtomDataArray, aTypeNames);}
    
    
    /// 文件读写
    /**
     * 从文件 vasp 输出的 XDATCAR 文件中读取来实现初始化
     * @author liqa
     * @param aFilePath vasp 输出的 XDATCAR 文件路径
     * @return 读取得到的 XDATCAR 对象，如果文件不完整的帧会跳过
     * @throws IOException 如果读取失败
     */
    public static XDATCAR read(String aFilePath) throws IOException {try (BufferedReader tReader = UT.IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用 */
    static XDATCAR read_(BufferedReader aReader) throws IOException {
        String tLine;
        String[] tTokens;
        
        // 先读通用信息
        String aComment;
        VaspBox aBox;
        IVector aBoxA, aBoxB, aBoxC;
        double aBoxScale;
        String[] aTypeNames;
        IIntVector aAtomNumbers;
        boolean aIsCartesian;
        
        // 第一行为 Comment
        tLine = aReader.readLine();
        aComment = tLine;
        // 读取模拟盒信息
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Text.splitBlank(tLine);
        aBoxScale = Double.parseDouble(tTokens[0]);
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBoxA = UT.Text.str2data(tLine, 3);
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBoxB = UT.Text.str2data(tLine, 3);
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBoxC = UT.Text.str2data(tLine, 3);
        // 读取原子种类（可选）和对应数目的信息
        boolean tNoAtomType = false;
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Text.splitBlank(tLine);
        aTypeNames = tTokens;
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Text.splitBlank(tLine);
        try {
        final String[] fTokens = tTokens;
        aAtomNumbers = Vectors.fromInteger(fTokens.length, i -> Integer.parseInt(fTokens[i]));
        } catch (Exception e) {
        tNoAtomType = true;
        final String[] fTokens = aTypeNames;
        aAtomNumbers = Vectors.fromInteger(fTokens.length, i -> Integer.parseInt(fTokens[i]));
        aTypeNames = null;
        }
        if (!tNoAtomType) {
        tLine = aReader.readLine(); if (tLine == null) return null;
        }
        // 只支持 Direct 和 Cartesian
        aIsCartesian = UT.Text.containsIgnoreCase(tLine, "Cartesian");
        if (!aIsCartesian && !UT.Text.containsIgnoreCase(tLine, "Direct")) {
        throw new RuntimeException("Can ONLY read Direct or Cartesian XDATCAR");
        }
        
        // 再读取原子数据
        List<IMatrix> rDirects = new ArrayList<>();
        int tAtomNum = aAtomNumbers.sum();
        while (true) {
            IMatrix subDirect = RowMatrix.zeros(tAtomNum, 3);
            
            boolean tIsAtomDataReadFull = true;
            for (IVector tRow : subDirect.rows()) {
                tLine = aReader.readLine();
                if (tLine == null) {tIsAtomDataReadFull = false; break;}
                tRow.fill(UT.Text.str2data(tLine, 3));
            }
            if (!tIsAtomDataReadFull) break;
            
            rDirects.add(subDirect);
            // 跳过下一个的 Direct configuration，如果没有则终止循环
            tLine = aReader.readLine(); if (tLine == null) break;
        }
        
        // 判断是否是 prism 并据此创建 VaspBox
        boolean tNotPrism =
               MathEX.Code.numericEqual(aBoxA.get(1), 0.0) && MathEX.Code.numericEqual(aBoxA.get(2), 0.0)
            && MathEX.Code.numericEqual(aBoxB.get(0), 0.0) && MathEX.Code.numericEqual(aBoxB.get(2), 0.0)
            && MathEX.Code.numericEqual(aBoxC.get(0), 0.0) && MathEX.Code.numericEqual(aBoxC.get(1), 0.0)
            ;
        aBox = tNotPrism ?
            new VaspBox(aBoxA.get(0), aBoxB.get(1), aBoxC.get(2), aBoxScale) :
            new VaspBoxPrism(
                aBoxA.get(0), aBoxA.get(1), aBoxA.get(2),
                aBoxB.get(0), aBoxB.get(1), aBoxB.get(2),
                aBoxC.get(0), aBoxC.get(1), aBoxC.get(2),
                aBoxScale
            );
        return new XDATCAR(aComment, aBox, aTypeNames, aAtomNumbers, rDirects, aIsCartesian, null);
    }
    
    /**
     * 输出成 vasp 格式的 XDATCAR 文件，可以供 OVITO 等软件读取
     * @author liqa
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {try (UT.IO.IWriteln tWriteln = UT.IO.toWriteln(aFilePath)) {write_(tWriteln);}}
    /** 改为 {@link UT.IO.IWriteln} 而不是 {@code List<String>} 来避免过多内存占用 */
    void write_(UT.IO.IWriteln aWriteln) throws IOException {
        // 先输出通用信息
        aWriteln.writeln(mComment==null ? DEFAULT_COMMENT : mComment);
        aWriteln.writeln(String.valueOf(mBox.scale()));
        aWriteln.writeln(String.format("    %16.10g    %16.10g    %16.10g", mBox.iax(), mBox.iay(), mBox.iaz()));
        aWriteln.writeln(String.format("    %16.10g    %16.10g    %16.10g", mBox.ibx(), mBox.iby(), mBox.ibz()));
        aWriteln.writeln(String.format("    %16.10g    %16.10g    %16.10g", mBox.icx(), mBox.icy(), mBox.icz()));
        if (mTypeNames!=null && mTypeNames.length!=0) {
        aWriteln.writeln(String.join(" ", AbstractCollections.slice(AbstractCollections.map(mTypeNames, type -> String.format("%6s", type)), AbstractCollections.range(mAtomNumbers.size()))));
        }
        aWriteln.writeln(String.join(" ", AbstractCollections.map(mAtomNumbers.iterable(), number -> String.format("%6d", number))));
        // 再输出原子数据
        for (int i = 0; i < mList.size(); ++i) {
        aWriteln.writeln((mIsCartesian ? "Cartesian" : "Direct") + " configuration= " + (i+1));
        for (IVector subDirect : mList.get(i).rows()) {
        aWriteln.writeln(String.format("%16.10g    %16.10g    %16.10g", subDirect.get(0), subDirect.get(1), subDirect.get(2)));
        }}
    }
    
    
    
    /// 实用功能，这里依旧保留这种写法
    /**
     * 获取多帧原子参数的计算器，支持使用 MFPC 的简写来调用
     * @param aTimestep 实际两帧之间的时间步长
     * @param aType 指定此值来获取只有这个种类的原子的单原子计算器，用于计算只考虑一种元素的一些参数
     * @param aThreadNum 执行 MFPC 的线程数目
     * @return 获取到的 MFPC
     * @deprecated use {@link MultiFrameParameterCalculator#of} or {@link MFPC#of}
     */
    @Deprecated public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType, int aThreadNum) {return MultiFrameParameterCalculator.of(AbstractCollections.map(asList(), atomData -> atomData.operation().filterType(aType)), aTimestep, aThreadNum);}
    /** @deprecated use {@link MultiFrameParameterCalculator#of}*/ @Deprecated public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep                           ) {return MultiFrameParameterCalculator.of(asList()                                                                             , aTimestep            );}
    /** @deprecated use {@link MultiFrameParameterCalculator#of}*/ @Deprecated public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep,            int aThreadNum) {return MultiFrameParameterCalculator.of(asList()                                                                             , aTimestep, aThreadNum);}
    /** @deprecated use {@link MultiFrameParameterCalculator#of}*/ @Deprecated public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType                ) {return MultiFrameParameterCalculator.of(AbstractCollections.map(asList(), atomData -> atomData.operation().filterType(aType)), aTimestep            );}
    /** @deprecated use {@link MFPC#of}*/ @Deprecated @VisibleForTesting public MultiFrameParameterCalculator       getMFPC                             (double aTimestep                           ) {return MFPC.of(asList()                                                                             , aTimestep            );}
    /** @deprecated use {@link MFPC#of}*/ @Deprecated @VisibleForTesting public MultiFrameParameterCalculator       getMFPC                             (double aTimestep,            int aThreadNum) {return MFPC.of(asList()                                                                             , aTimestep, aThreadNum);}
    /** @deprecated use {@link MFPC#of}*/ @Deprecated @VisibleForTesting public MultiFrameParameterCalculator       getTypeMFPC                         (double aTimestep, int aType                ) {return MFPC.of(AbstractCollections.map(asList(), atomData -> atomData.operation().filterType(aType)), aTimestep            );}
    /** @deprecated use {@link MFPC#of}*/ @Deprecated @VisibleForTesting public MultiFrameParameterCalculator       getTypeMFPC                         (double aTimestep, int aType, int aThreadNum) {return MFPC.of(AbstractCollections.map(asList(), atomData -> atomData.operation().filterType(aType)), aTimestep, aThreadNum);}
}
