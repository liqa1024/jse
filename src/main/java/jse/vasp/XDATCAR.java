package jse.vasp;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jse.atom.*;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractListWrapper;
import jse.code.collection.AbstractRandomAccessList;
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
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;
import java.util.function.IntUnaryOperator;

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
    
    private boolean mIsCartesian; // 如果是 Cartesian 则是没有进行缩放的
    
    /** 用于通过字符获取每个种类的粒子数，考虑了可能有相同 key 的情况 */
    private final @NotNull Multimap<String, Integer> mKey2Type;
    
    XDATCAR(@Nullable String aComment, VaspBox aBox, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, List<IMatrix> aDirects, boolean aIsCartesian) {
        super(aDirects);
        mComment = aComment;
        mTypeNames = aTypeNames;
        mAtomNumbers = aAtomNumbers;
        mBox = aBox;
        mIsCartesian = aIsCartesian;
        
        mKey2Type = ArrayListMultimap.create();
        if (mTypeNames != null) {
            int rType = 0;
            for (String tKey : mTypeNames) {
                ++rType;
                mKey2Type.put(tKey, rType);
            }
        }
    }
    XDATCAR(@Nullable String aComment, VaspBox aBox, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, IMatrix aFirstDirect, int aInitSize, boolean aIsCartesian) {
        this(aComment, aBox, aTypeNames, aAtomNumbers, new ArrayList<>(aInitSize), aIsCartesian);
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
            UT.Code.warning("Box type of appended XDATCAR ("+(tXDATCAR.mBox.isPrism() ? "prism" : "orthogonal")+") is different from the original box type ("+(mBox.isPrism() ? "prism" : "orthogonal")+")");
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
            UT.Code.warning("Box of appended XDATCAR is different from the original box\n" +
                            "Appended:\n" +
                            tXDATCAR.mBox + "\n" +
                            "Original:\n" +
                            mBox);
        }
        // 检测原子数目是否一致，否则输出警告
        if (!sameAtomNumbers_(tXDATCAR.mAtomNumbers)) {
            UT.Code.warning("Atom numbers of appended XDATCAR ("+tXDATCAR.mAtomNumbers.asList()+") is different from the original ("+mAtomNumbers.asList()+")");
        }
        // 检测原子种类数是否一致，否则输出警告
        if (!sameTypeNameOrder_(tXDATCAR.mTypeNames)) {
            UT.Code.warning("Symbols of appended XDATCAR ("+Arrays.toString(tXDATCAR.mTypeNames)+") is different from the original ("+Arrays.toString(mTypeNames)+")");
        }
        // 这里直接将后续数据直接添加，不做转化
        mList.addAll(tXDATCAR.mList);
        return this;
    }
    /** groovy stuffs */
    @Override public XDATCAR leftShift(IAtomData aAtomData) {return (XDATCAR)super.leftShift(aAtomData);}
    @Override public XDATCAR leftShift(Collection<? extends IAtomData> aAtomDataList) {return (XDATCAR)super.leftShift(aAtomDataList);}
    
    private boolean sameAtomNumbers_(IIntVector aAtomNumbersIn) {
        int tSize = aAtomNumbersIn.size();
        if (tSize > mAtomNumbers.size()) return false;
        for (int i = 0; i < tSize; ++i) {
            if (aAtomNumbersIn.get(i) != mAtomNumbers.get(i)) {
                return false;
            }
        }
        return true;
    }
    private boolean sameTypeNameOrder_(String[] aTypeNamesIn) {
        if (aTypeNamesIn==null || mTypeNames==null) return true;
        return IAtomData.sameSymbolOrder_(AbstractCollections.from(mTypeNames), AbstractCollections.from(aTypeNamesIn));
    }
    
    
    /// 获取属性
    public boolean hasSymbol() {return mTypeNames!=null;}
    public @Nullable String symbol(int aType) {return mTypeNames==null ? null : mTypeNames[aType-1];}
    public @Nullable List<@Nullable String> symbols() {
        if (!hasSymbol()) return null;
        return new AbstractRandomAccessList<@Nullable String>() {
            @Override public @Nullable String get(int index) {return symbol(index+1);}
            @Override public int size() {return atomTypeNumber();}
        };
    }
    public boolean hasMasse() {return hasSymbol();}
    public double mass(int aType) {
        @Nullable String tSymbol = symbol(aType);
        return tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
    }
    /** @deprecated use {@link #symbol(int)} */
    @Deprecated public @Nullable String typeName(int aType) {return symbol(aType);}
    public int atomNumber(String aKey) {
        int rAtomNum = 0;
        for (int tType : mKey2Type.get(aKey)) rAtomNum += atomNumber(tType);
        return rAtomNum;
    }
    public int atomNumber(int aType) {return mAtomNumbers.get(aType-1);}
    /** @deprecated use {@link #atomNumber(String)} or {@link #natoms(String)} */ @Deprecated public final int atomNum(String aType) {return atomNumber(aType);}
    /** @deprecated use {@link #atomNumber(int)} or {@link #natoms(int)} */ @Deprecated public final int atomNum(int aType) {return atomNumber(aType);}
    /** 提供简写版本 */
    @VisibleForTesting public final int natoms(String aType) {return atomNumber(aType);}
    @VisibleForTesting public final int natoms(int aType) {return atomNumber(aType);}
    
    public @Override @Nullable String comment() {return mComment;}
    public @Override String @Nullable[] typeNames() {return mTypeNames;}
    public @Override IIntVector atomNumbers() {return mAtomNumbers;}
    public List<IMatrix> directs() {return mList;}
    public @Override boolean isCartesian() {return mIsCartesian;}
    
    /** @deprecated use {@link #box()} */ @Deprecated public VaspBox vaspBox() {return box();}
    /** @deprecated use {@link VaspBox#scale()} */ @Deprecated public double vaspBoxScale() {return mBox.scale();}
    /** @deprecated use {@code !}{@link #isPrism()} */ @Deprecated public boolean isDiagBox() {return !isPrism();}
    
    /** 对于 XDATCAR 这些接口也同样可以获取到 */
    public VaspBox box() {return mBox;}
    public double volume() {return mBox.volume();}
    public boolean isPrism() {return mBox.isPrism();}
    public boolean isLmpStyle() {return mBox.isLmpStyle();}
    public int atomNumber() {return mAtomNumbers.sum();}
    public int atomTypeNumber() {return mAtomNumbers.size();}
    /** @deprecated use {@link #atomNumber()} or {@link #natoms()} */ @Deprecated public final int atomNum() {return atomNumber();}
    /** @deprecated use {@link #atomNumber()} or {@link #natoms()} */ @Deprecated public final int atomTypeNum() {return atomTypeNumber();}
    /** 提供简写版本 */
    @VisibleForTesting public final int natoms() {return atomNumber();}
    @VisibleForTesting public final int ntypes() {return atomTypeNumber();}
    
    /** XDATCAR 提供简单的修改模拟盒支持 */
    public XDATCAR setBox(double aX, double aY, double aZ) {return setBox(false, aX, aY, aZ);}
    public XDATCAR setBox(boolean aKeepAtomPosition, double aX, double aY, double aZ) {
        // 这里统一移除掉 scale 的数据，保证新的 box 合法性
        if (mIsCartesian) {
            for (IMatrix tDirect : mList) tDirect.multiply2this(mBox.scale());
        }
        IBox oBox = mBox;
        mBox = new VaspBox(aX, aY, aZ);
        validAtomPosition_(aKeepAtomPosition, oBox);
        return this;
    }
    public XDATCAR setBox(IXYZ aA, IXYZ aB, IXYZ aC) {return setBox(false, aA, aB, aC);}
    public XDATCAR setBox(boolean aKeepAtomPosition, IXYZ aA, IXYZ aB, IXYZ aC) {
        return setBox(aKeepAtomPosition,
                      aA.x(), aA.y(), aA.z(),
                      aB.x(), aB.y(), aB.z(),
                      aC.x(), aC.y(), aC.z());
    }
    public XDATCAR setBox(double aX, double aY, double aZ, double aXY, double aXZ, double aYZ) {return setBox(false, aX, aY, aZ, aXY, aXZ, aYZ);}
    public XDATCAR setBox(boolean aKeepAtomPosition, double aX, double aY, double aZ, double aXY, double aXZ, double aYZ) {
        return setBox(aKeepAtomPosition,
                      aX, 0.0, 0.0,
                      aXY, aY, 0.0,
                      aXZ, aYZ, aZ);
    }
    public XDATCAR setBox(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {return setBox(false, aAx, aAy, aAz, aBx, aBy, aBz, aCx, aCy, aCz);}
    public XDATCAR setBox(boolean aKeepAtomPosition, double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        // 这里统一移除掉 scale 的数据，保证新的 box 合法性
        if (mIsCartesian) {
            for (IMatrix tDirect : mList) tDirect.multiply2this(mBox.scale());
        }
        IBox oBox = mBox;
        mBox = new VaspBoxPrism(aAx, aAy, aAz, aBx, aBy, aBz, aCx, aCy, aCz);
        validAtomPosition_(aKeepAtomPosition, oBox);
        return this;
    }
    private void validAtomPosition_(boolean aKeepAtomPosition, IBox aOldBox) {
        // 对于 Cartesian 和 Direct 要分开讨论
        final int tAtomNum = atomNumber();
        XYZ tBuf = new XYZ();
        if (mIsCartesian) {
            if (aKeepAtomPosition) return;
            if (mBox.isPrism() || aOldBox.isPrism()) {
                for (IMatrix tDirect : mList) for (int i = 0; i < tAtomNum; ++i) {
                    tBuf.setXYZ(tDirect.get(i, 0), tDirect.get(i, 1), tDirect.get(i, 2));
                    // 这样转换两次即可实现线性变换
                    aOldBox.toDirect(tBuf);
                    mBox.toCartesian(tBuf);
                    tDirect.set(i, 0, tBuf.mX);
                    tDirect.set(i, 1, tBuf.mY);
                    tDirect.set(i, 2, tBuf.mZ);
                }
            } else {
                tBuf.setXYZ(mBox);
                tBuf.div2this(aOldBox);
                for (IMatrix tDirect : mList) {
                    tDirect.col(0).multiply2this(tBuf.mX);
                    tDirect.col(1).multiply2this(tBuf.mY);
                    tDirect.col(2).multiply2this(tBuf.mZ);
                }
            }
            return;
        }
        if (!aKeepAtomPosition) return;
        if (mBox.isPrism() || aOldBox.isPrism()) {
            for (IMatrix tDirect : mList) for (int i = 0; i < tAtomNum; ++i) {
                tBuf.setXYZ(tDirect.get(i, 0), tDirect.get(i, 1), tDirect.get(i, 2));
                // 对于 Direct 需要这样反向变换
                aOldBox.toCartesian(tBuf);
                mBox.toDirect(tBuf);
                tDirect.set(i, 0, tBuf.mX);
                tDirect.set(i, 1, tBuf.mY);
                tDirect.set(i, 2, tBuf.mZ);
            }
        } else {
            tBuf.setXYZ(aOldBox);
            tBuf.div2this(mBox);
            for (IMatrix tDirect : mList) {
                tDirect.col(0).multiply2this(tBuf.mX);
                tDirect.col(1).multiply2this(tBuf.mY);
                tDirect.col(2).multiply2this(tBuf.mZ);
            }
        }
    }
    
    /** 支持直接修改 TypeNames，只会增大种类数，不会减少 */
    public XDATCAR setSymbols(String... aTypeNames) {
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
    public XDATCAR setNoSymbol() {return setSymbols(ZL_STR);}
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
    /** @deprecated use {@link #setSymbols(String...)} */ @Deprecated public XDATCAR setTypeNames(String... aSymbols) {return setSymbols(aSymbols);}
    /** @deprecated use {@link #setNoSymbol()} */ @Deprecated public XDATCAR setNoTypeName() {return setNoSymbol();}
    
    public XDATCAR setComment(@Nullable String aComment) {mComment = aComment; return this;}
    
    /** Groovy stuffs */
    @VisibleForTesting public String @Nullable[] getTypeNames() {return mTypeNames;}
    @VisibleForTesting public @Nullable String getComment() {return mComment;}
    @VisibleForTesting public @Nullable List<@Nullable String> getSymbols() {return symbols();}
    
    /** Cartesian 和 Direct 来回转换 */
    public XDATCAR setCartesian() {
        if (mIsCartesian) return this;
        // 这里绕过 scale 直接处理
        if (isPrism()) {
            IMatrix tIABC = mBox.iabc();
            for (IMatrix tDirect : mList) {
                tDirect.operation().matmul2this(tIABC);
                // cartesian 其实也需要考虑计算误差带来的出边界的问题（当然此时在另一端的就不好修复了）
                final double tNorm = tIABC.asVecCol().operation().stat((norm, v) -> norm+Math.abs(v));
                tDirect.operation().map2this(v -> Math.abs(v)<MathEX.Code.DBL_EPSILON*tNorm ? 0.0 : v);
            }
        } else {
            for (IMatrix tDirect : mList) {
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
        if (isPrism()) {
            IMatrix tInvIABC = mBox.inviabc();
            for (IMatrix tDirect : mList) {
                tDirect.operation().matmul2this(tInvIABC);
                // direct 需要考虑计算误差带来的出边界的问题，现在支持自动靠近所有整数值
                tDirect.operation().map2this(v -> {
                    if (Math.abs(v) < MathEX.Code.DBL_EPSILON) return 0.0;
                    int tIntV = MathEX.Code.round2int(v);
                    if (MathEX.Code.numericEqual(v, tIntV)) return tIntV;
                    return v;
                });
            }
        } else {
            for (IMatrix tDirect : mList) {
                tDirect.col(0).div2this(mBox.iax());
                tDirect.col(1).div2this(mBox.iby());
                tDirect.col(2).div2this(mBox.icz());
            }
        }
        mIsCartesian = false;
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
        return new XDATCAR(mComment, mBox.copy(), POSCAR.copyTypeNames(mTypeNames), mAtomNumbers.copy(), rDirects, mIsCartesian);
    }
    
    /// 创建 XDATCAR
    /** 从 IAtomData 来创建，对于 XDATCAR 可以支持容器的 aAtomData */
    public static XDATCAR fromAtomData(IAtomData aAtomData) {
        @Nullable List<@Nullable String> tSymbols = aAtomData.symbols();
        return fromAtomData(aAtomData, tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
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
            return new XDATCAR(tPOSCAR.comment(), tPOSCAR.box().copy(), POSCAR.copyTypeNames(tPOSCAR.typeNames()), tPOSCAR.atomNumbers().copy(), tPOSCAR.direct().copy(), aInitSize, tPOSCAR.isCartesian()).setSymbols(aTypeNames);
        } else {
            // 一般的情况，这里直接遍历 atoms 来创建，这里需要按照 type 来排序
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
            return new XDATCAR(null, rBox, POSCAR.copyTypeNames(aTypeNames), rAtomNumbers, rDirect, aInitSize, true);
        }
    }
    static XDATCAR fromAtomDataList_(Iterable<? extends IAtomData> aAtomDataList, int aInitSize, String[] aTypeNames) {
        // 直接不支持空的创建来简化实现的代码
        if (aAtomDataList == null) throw new IllegalArgumentException("XDATCAR do NOT support empty AtomDataList");
        Iterator<? extends IAtomData> it = aAtomDataList.iterator();
        if (!it.hasNext()) throw new IllegalArgumentException("XDATCAR do NOT support empty AtomDataList");
        IAtomData first = it.next();
        @Nullable List<@Nullable String> tSymbols = first.symbols();
        final XDATCAR rXDATCAR = fromAtomData_(first, aInitSize, ((aTypeNames==null || aTypeNames==ZL_STR) && tSymbols!=null) ? tSymbols.toArray(ZL_STR) : aTypeNames);
        it.forEachRemaining(rXDATCAR::append);
        return rXDATCAR;
    }
    public static XDATCAR fromAtomData(IAtomData aAtomData, Collection<? extends CharSequence> aTypeNames) {return fromAtomData(aAtomData, UT.Text.toArray(aTypeNames));}
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {return fromAtomDataList(aAtomDataList, UT.Text.toArray(aTypeNames));}
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {return fromAtomDataList(aAtomDataList, UT.Text.toArray(aTypeNames));}
    
    IMatrix getDirect_(IAtomData aAtomData) {
        // 这里只考虑一般的情况，这里直接遍历 atoms 来创建，
        // 现在不再考虑 id 顺序（因为可能存在 type 发生改变的问题）
        IBox tBox = aAtomData.box();
        int tAtomNum = aAtomData.atomNumber();
        int tAtomTypeNum = aAtomData.atomTypeNumber();
        if (tAtomTypeNum > atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of AtomData: " + aAtomData.atomTypeNumber() + ", target: " + atomTypeNumber());
        IMatrix rDirect = Matrices.zeros(tAtomNum, 3);
        XYZ tBuf = new XYZ();
        int tIdx = 0;
        // 这里还是统一按照 type 进行排序；还需要检测 type 的 symbol 是否是一一对应的
        IntUnaryOperator tTypeMap = mTypeNames==null ? t->t : IAtomData.typeMap_(AbstractCollections.from(mTypeNames), aAtomData);
        int tMaxIdx = 0;
        for (int type = 1; type <= tAtomTypeNum; ++type) {
            tMaxIdx += atomNumber(type);
            for (IAtom tAtom : aAtomData.atoms()) if (tTypeMap.applyAsInt(tAtom.type()) == type) {
                // 简单检测原子数是否一致
                if (tIdx >= tMaxIdx) throw new IllegalArgumentException("Invalid atom number of type "+type);
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
                ++tIdx;
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
    public static XDATCAR of(IAtomData aAtomData, Collection<? extends CharSequence> aTypeNames) {return fromAtomData(aAtomData, aTypeNames);}
    public static XDATCAR of(Iterable<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    public static XDATCAR of(Iterable<? extends IAtomData> aAtomDataList, String... aTypeNames) {return fromAtomDataList(aAtomDataList, aTypeNames);}
    public static XDATCAR of(Iterable<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {return fromAtomDataList(aAtomDataList, aTypeNames);}
    public static XDATCAR of(Collection<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    public static XDATCAR of(Collection<? extends IAtomData> aAtomDataList, String... aTypeNames) {return fromAtomDataList(aAtomDataList, aTypeNames);}
    public static XDATCAR of(Collection<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {return fromAtomDataList(aAtomDataList, aTypeNames);}
    /** 再提供一个 IListWrapper 的接口保证 XDATCAR 也能输入 */
    public static XDATCAR of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList) {return fromAtomDataList(aAtomDataList.asList());}
    public static XDATCAR of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, String... aTypeNames) {return fromAtomDataList(aAtomDataList.asList(), aTypeNames);}
    public static XDATCAR of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {return fromAtomDataList(aAtomDataList.asList(), aTypeNames);}
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
        return new XDATCAR(aComment, aBox, aTypeNames, aAtomNumbers, rDirects, aIsCartesian);
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
        aWriteln.writeln(String.format("  %24.18g  %24.18g  %24.18g", mBox.iax(), mBox.iay(), mBox.iaz()));
        aWriteln.writeln(String.format("  %24.18g  %24.18g  %24.18g", mBox.ibx(), mBox.iby(), mBox.ibz()));
        aWriteln.writeln(String.format("  %24.18g  %24.18g  %24.18g", mBox.icx(), mBox.icy(), mBox.icz()));
        if (mTypeNames!=null && mTypeNames.length!=0) {
        aWriteln.writeln(String.join(" ", AbstractCollections.slice(AbstractCollections.map(mTypeNames, type -> String.format("%6s", type)), AbstractCollections.range(mAtomNumbers.size()))));
        }
        aWriteln.writeln(String.join(" ", AbstractCollections.map(mAtomNumbers.iterable(), number -> String.format("%6d", number))));
        // 再输出原子数据
        for (int i = 0; i < mList.size(); ++i) {
        aWriteln.writeln((mIsCartesian ? "Cartesian" : "Direct") + " configuration= " + (i+1));
        for (IVector subDirect : mList.get(i).rows()) {
        aWriteln.writeln(String.format("%24.18g  %24.18g  %24.18g", subDirect.get(0), subDirect.get(1), subDirect.get(2)));
        }}
    }
}
