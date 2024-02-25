package jse.vasp;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jse.atom.IAtom;
import jse.atom.IAtomData;
import jse.atom.MultiFrameParameterCalculator;
import jse.atom.XYZ;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractListWrapper;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.matrix.Matrices;
import jse.math.matrix.RefMatrix;
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
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import static jse.code.CS.MASS;


/**
 * @author liqa
 * <p> vasp 读取输出的多帧原子位置格式，每帧为 {@link POSCAR} </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 暂时不支持边界条件设置 </p>
 * <p> 暂时不考虑 Direct configuration 间距不为 1 的情况，因此 MFPC 不会统计两帧之间跳过的部分 </p>
 * <p> 返回的 {@link POSCAR} 共享原子位置但是不共享 mBoxScale 以及 mSelectiveDynamics（除了原子位置其余是否共享属于未定义）</p>
 * <p> 现在不再继承 {@link List}，因为 List 的接口太脏了 </p>
 * <p> 并且现在不再继承 {@link IAtomData}，如果需要使用单个 XDATCAR 直接使用 {@link POSCAR} </p>
 */
public class XDATCAR extends AbstractListWrapper<POSCAR, IAtomData, IMatrix> implements IVaspCommonData {
    public final static String DEFAULT_COMMENT = "VASP_XDATCAR_FROM_JSE";
    
    /** 这里统一存放通用数据保证所有帧这些一定是相同的 */
    private @Nullable String mComment;
    private String @Nullable[] mTypeNames;
    private IIntVector mAtomNumbers;
    private final IMatrix mBox;
    private double mBoxScale;
    private final boolean mIsDiagBox;
    /** 保存一份 id 列表，这样在 lmpdat 转为 poscar 时会继续保留 id 信息，XDATCAR 认为不能进行修改 */
    private final @Nullable IIntVector mIDs;
    private final @Nullable @Unmodifiable Map<Integer, Integer> mId2Index; // 原子的 id 转为存储在 AtomDataXYZ 的指标 index
    
    private boolean mIsCartesian; // 如果是 Cartesian 则是没有进行缩放的
    
    /** 用于通过字符获取每个种类的粒子数，考虑了可能有相同 key 的情况 */
    private final @NotNull Multimap<String, Integer> mKey2Type;
    
    XDATCAR(@Nullable String aComment, IMatrix aBox, double aBoxScale, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, List<IMatrix> aDirects, boolean aIsCartesian, @Nullable IIntVector aIDs) {
        super(aDirects);
        mComment = aComment;
        mTypeNames = aTypeNames;
        mAtomNumbers = aAtomNumbers;
        mBox = aBox;
        mBoxScale = aBoxScale;
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
        
        mIsDiagBox = mBox.operation().isDiag();
    }
    XDATCAR(@Nullable String aComment, IMatrix aBox, double aBoxScale, String @Nullable[] aTypeNames, IIntVector aAtomNumbers, IMatrix aFirstDirect, int aInitSize, boolean aIsCartesian, @Nullable IIntVector aIDs) {
        this(aComment, aBox, aBoxScale, aTypeNames, aAtomNumbers, new ArrayList<>(aInitSize), aIsCartesian, aIDs);
        mList.add(aFirstDirect);
    }
    
    /** AbstractListWrapper stuffs */
    @Override protected final IMatrix toInternal_(IAtomData aAtomData) {return getDirect_(aAtomData);}
    @Override protected final POSCAR toOutput_(IMatrix aDirect) {return new POSCAR(this, false, aDirect);}
    
    
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    public XDATCAR append(IAtomData aAtomData) {
        // 只有正交的 XDATCAR 才能通过 AtomData 设置内部元素
        if (!mIsDiagBox) throw new RuntimeException("append is support Diagonal Box only");
        mList.add(getDirect_(aAtomData));
        return this;
    }
    public XDATCAR appendList(Iterable<? extends IAtomData> aAtomDataList) {
        // 只有正交的 XDATCAR 才能通过 AtomData 设置内部元素
        if (!mIsDiagBox) throw new RuntimeException("appendList is support Diagonal Box only");
        for (IAtomData tAtomData : aAtomDataList) mList.add(getDirect_(tAtomData));
        return this;
    }
    public XDATCAR appendFile(String aFilePath) throws IOException {
        // 原则上只有正交的或者 box 完全相同才可以添加，这里直接通过 refDirect_ 的内部进行判断，即使 box 不相同也会可以添加
        final XDATCAR tXDATCAR = read(aFilePath);
        mList.addAll(AbstractCollections.map(tXDATCAR.mList, direct -> refDirect_(tXDATCAR.mBox, tXDATCAR.mBoxScale, mBoxScale, direct, tXDATCAR.mIsCartesian, mIsCartesian)));
        return this;
    }
    
    
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
    /** 保留旧名称兼容，当时起名太随意了，居然这么久都没发现 */
    @Deprecated public final int atomNum(String aType) {return atomNumber(aType);}
    @Deprecated public final int atomNum(int aType) {return atomNumber(aType);}
    /** 提供简写版本 */
    @VisibleForTesting public final int natoms(String aType) {return atomNumber(aType);}
    @VisibleForTesting public final int natoms(int aType) {return atomNumber(aType);}
    
    public @Override @Nullable String comment() {return mComment;}
    public @Override String @Nullable[] typeNames() {return mTypeNames;}
    public @Override IIntVector atomNumbers() {return mAtomNumbers;}
    public @Override IMatrix vaspBox() {return mBox;}
    public @Override double vaspBoxScale() {return mBoxScale;}
    public List<IMatrix> directs() {return mList;}
    public @Override boolean isCartesian() {return mIsCartesian;}
    public @Override boolean isDiagBox() {return mIsDiagBox;}
    public @Override @Nullable IIntVector ids() {return mIDs;}
    
    /** Groovy stuffs */
    @VisibleForTesting public String @Nullable[] getTypeNames() {return mTypeNames;}
    @VisibleForTesting public String getComment() {return mComment;}
    @VisibleForTesting public double getBoxScale() {return mBoxScale;}
    
    
    /** 支持直接修改 TypeNames，只会增大种类数，不会减少 */
    public XDATCAR setTypeNames(String... aTypeNames) {
        if (aTypeNames==null || aTypeNames.length==0) {
            mTypeNames = null;
            mKey2Type.clear();
            return this;
        }
        if (mTypeNames==null || aTypeNames.length>mTypeNames.length) mTypeNames = Arrays.copyOf(aTypeNames, aTypeNames.length);
        else System.arraycopy(aTypeNames, 0, mTypeNames, 0, aTypeNames.length);
        if (aTypeNames.length > mAtomNumbers.size()) {
            IIntVector oAtomNumbers = mAtomNumbers;
            mAtomNumbers = IntVector.zeros(aTypeNames.length);
            mAtomNumbers.subVec(0, oAtomNumbers.size()).fill(oAtomNumbers);
        }
        mKey2Type.clear();
        int rType = 0;
        for (String tKey : mTypeNames) {
            ++rType;
            mKey2Type.put(tKey, rType);
        }
        return this;
    }
    
    public XDATCAR setComment(@Nullable String aComment) {mComment = aComment; return this;}
    public XDATCAR setBoxScale(double aBoxScale) {mBoxScale = aBoxScale; return this;}
    
    /** Cartesian 和 Direct 来回转换 */
    public XDATCAR setCartesian() {
        if (mIsCartesian) return this;
        // 注意如果是斜方的模拟盒则不能进行转换
        if (!mIsDiagBox) throw new RuntimeException("converting between Cartesian and Direct is temporarily support Diagonal Box only");
        for (IMatrix tDirect : mList) {
            tDirect.col(0).multiply2this(mBox.get(0, 0));
            tDirect.col(1).multiply2this(mBox.get(1, 1));
            tDirect.col(2).multiply2this(mBox.get(2, 2));
        }
        mIsCartesian = true;
        return this;
    }
    public XDATCAR setDirect() {
        if (!mIsCartesian) return this;
        // 注意如果是斜方的模拟盒则不能进行转换
        if (!mIsDiagBox) throw new RuntimeException("converting between Cartesian and Direct is temporarily support Diagonal Box only");
        for (IMatrix tDirect : mList) {
            tDirect.col(0).div2this(mBox.get(0, 0));
            tDirect.col(1).div2this(mBox.get(1, 1));
            tDirect.col(2).div2this(mBox.get(2, 2));
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
        return new XDATCAR(mComment, mBox.copy(), mBoxScale, POSCAR.copyTypeNames(mTypeNames), mAtomNumbers.copy(), rDirects, mIsCartesian, POSCAR.copyIDs(mIDs));
    }
    
    /// 创建 XDATCAR
    /** 从 IAtomData 来创建，对于 XDATCAR 可以支持容器的 aAtomData */
    public static XDATCAR fromAtomData(IAtomData aAtomData) {return fromAtomData(aAtomData, (aAtomData instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomData).typeNames() : null);}
    public static XDATCAR fromAtomData(IAtomData aAtomData, String... aTypeNames) {return fromAtomData_(aAtomData, 1, aTypeNames);}
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList, (aAtomDataList instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomDataList).typeNames() : null);}
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, String... aTypeNames) {return fromAtomDataList_(aAtomDataList, 1, aTypeNames);}
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList, (aAtomDataList instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomDataList).typeNames() : null);}
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, String... aTypeNames) {return fromAtomDataList_(aAtomDataList, aAtomDataList.size(), aTypeNames);}
    static XDATCAR fromAtomData_(IAtomData aAtomData, int aInitSize, String[] aTypeNames) {
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 Direct
        if (aAtomData instanceof POSCAR) {
            // POSCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            POSCAR tPOSCAR = (POSCAR)aAtomData;
            int tAtomTypeNum = Math.max(tPOSCAR.atomNumbers().size(), aTypeNames.length);
            IIntVector tAtomNumbers = IntVector.zeros(tAtomTypeNum);
            tAtomNumbers.subVec(0, tPOSCAR.atomNumbers().size()).fill(tPOSCAR.atomNumbers());
            return new XDATCAR(tPOSCAR.comment(), tPOSCAR.vaspBox().copy(), tPOSCAR.vaspBoxScale(), POSCAR.copyTypeNames(aTypeNames), tAtomNumbers, tPOSCAR.direct().copy(), aInitSize, tPOSCAR.isCartesian(), POSCAR.copyIDs(tPOSCAR.ids()));
        } else {
            // 一般的情况，这里直接遍历 atoms 来创建，这里需要按照 type 来排序
            IIntVector rIDs = IntVector.zeros(aAtomData.atomNumber());
            int tAtomTypeNum = Math.max(aAtomData.atomTypeNumber(), aTypeNames.length);
            IIntVector rAtomNumbers = IntVector.zeros(tAtomTypeNum);
            IMatrix rDirect = Matrices.zeros(aAtomData.atomNumber(), 3);
            int tIdx = 0;
            for (int tTypeMM = 0; tTypeMM < tAtomTypeNum; ++tTypeMM) {
                for (IAtom tAtom : aAtomData.asList()) if (tAtom.type() == tTypeMM+1) {
                    rAtomNumbers.increment(tTypeMM);
                    rDirect.set(tIdx, 0, tAtom.x());
                    rDirect.set(tIdx, 1, tAtom.y());
                    rDirect.set(tIdx, 2, tAtom.z());
                    rIDs.set(tIdx, tAtom.id());
                    ++tIdx;
                }
            }
            return new XDATCAR(null, Matrices.diag(aAtomData.box().data()), 1.0, POSCAR.copyTypeNames(aTypeNames), rAtomNumbers, rDirect, aInitSize, true, rIDs);
        }
    }
    static XDATCAR fromAtomDataList_(Iterable<? extends IAtomData> aAtomDataList, int aInitSize, String[] aTypeNames) {
        // 直接不支持空的创建来简化实现的代码
        if (aAtomDataList == null) throw new IllegalArgumentException("XDATCAR do NOT support empty AtomDataList");
        Iterator<? extends IAtomData> it = aAtomDataList.iterator();
        if (!it.hasNext()) throw new IllegalArgumentException("XDATCAR do NOT support empty AtomDataList");
        IAtomData first = it.next();
        final XDATCAR rXDATCAR = fromAtomData_(first, aInitSize, ((aTypeNames==null || aTypeNames.length==0) && (first instanceof IVaspCommonData)) ? ((IVaspCommonData)first).typeNames() : aTypeNames);
        it.forEachRemaining(rXDATCAR::append);
        return rXDATCAR;
    }
    
    IMatrix getDirect_(IAtomData aAtomData) {
        // 这里只考虑一般的情况，这里直接遍历 atoms 来创建，
        // 这里直接按照 mIDs 的顺序进行排序，不考虑 type 发生改变的情况
        XYZ tBox = XYZ.toXYZ(aAtomData.box());
        int tAtomNum = aAtomData.atomNumber();
        IMatrix rDirect = Matrices.zeros(tAtomNum, 3);
        for (IAtom tAtom : aAtomData.asList()) {
            int tIdx = mId2Index==null ? tAtom.id()-1 : mId2Index.get(tAtom.id());
            rDirect.set(tIdx, 0, mIsCartesian ? tAtom.x() : tAtom.x()/tBox.mX);
            rDirect.set(tIdx, 1, mIsCartesian ? tAtom.y() : tAtom.y()/tBox.mY);
            rDirect.set(tIdx, 2, mIsCartesian ? tAtom.z() : tAtom.z()/tBox.mZ);
        }
        rDirect.div2this(mBoxScale);
        return rDirect;
    }
    static IMatrix refDirect_(final IMatrix aBox, double aSrcBoxScale, double aToBoxScale, final IMatrix aDirect, boolean aSrcCartesian, boolean aToCartesian) {
        if (aSrcCartesian==aToCartesian && MathEX.Code.numericEqual(aSrcBoxScale, aToBoxScale)) return aDirect;
        // 并且这里还需要将其转换成正交的情况
        if (!aBox.operation().isDiag()) throw new RuntimeException("refDirect is temporarily support Diagonal Box only");
        
        final double tScale = aSrcBoxScale / aToBoxScale;
        if (aSrcCartesian == aToCartesian) {
            return new RefMatrix() {
                @Override public double get(int aRow, int aCol) {return aDirect.get(aRow, aCol) * tScale;}
                @Override public void set(int aRow, int aCol, double aValue) {aDirect.set(aRow, aCol, aValue / tScale);}
                @Override public int rowNumber() {return aDirect.rowNumber();}
                @Override public int columnNumber() {return aDirect.columnNumber();}
            };
        } else
        if (aToCartesian) {
            return new RefMatrix() {
                @Override public double get(int aRow, int aCol) {return aDirect.get(aRow, aCol) * aBox.get(aCol, aCol) * tScale;}
                @Override public void set(int aRow, int aCol, double aValue) {aDirect.set(aRow, aCol, aValue / aBox.get(aCol, aCol) / tScale);}
                @Override public int rowNumber() {return aDirect.rowNumber();}
                @Override public int columnNumber() {return aDirect.columnNumber();}
            };
        } else {
            return new RefMatrix() {
                @Override public double get(int aRow, int aCol) {return aDirect.get(aRow, aCol) / aBox.get(aCol, aCol) * tScale;}
                @Override public void set(int aRow, int aCol, double aValue) {aDirect.set(aRow, aCol, aValue * aBox.get(aCol, aCol) / tScale);}
                @Override public int rowNumber() {return aDirect.rowNumber();}
                @Override public int columnNumber() {return aDirect.columnNumber();}
            };
        }
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
        // 先读通用信息
        String aComment;
        IMatrix aBox;
        double aBoxScale;
        String[] aTypeNames;
        IIntVector aAtomNumbers;
        boolean aIsCartesian;
        
        String tLine;
        String[] tTokens;
        // 第一行为 Comment
        tLine = aReader.readLine();
        aComment = tLine;
        // 读取模拟盒信息
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Text.splitBlank(tLine);
        aBoxScale = Double.parseDouble(tTokens[0]);
        aBox = Matrices.zeros(3);
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBox.row(0).fill(UT.Text.str2data(tLine, 3));
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBox.row(1).fill(UT.Text.str2data(tLine, 3));
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBox.row(2).fill(UT.Text.str2data(tLine, 3));
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
        return new XDATCAR(aComment, aBox, aBoxScale, aTypeNames, aAtomNumbers, rDirects, aIsCartesian, null);
    }
    
    /**
     * 输出成 vasp 格式的 XDATCAR 文件，可以供 OVITO 等软件读取
     * <p>
     * 改为 {@link BufferedWriter} 而不是 {@code List<String>} 来避免过多内存占用
     * @author liqa
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {
        try (UT.IO.IWriteln tWriteln = UT.IO.toWriteln(aFilePath)) {
            // 先输出通用信息
            tWriteln.writeln(mComment==null ? DEFAULT_COMMENT : mComment);
            tWriteln.writeln(String.valueOf(mBoxScale));
            tWriteln.writeln(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(0, 0), mBox.get(0, 1), mBox.get(0, 2)));
            tWriteln.writeln(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(1, 0), mBox.get(1, 1), mBox.get(1, 2)));
            tWriteln.writeln(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(2, 0), mBox.get(2, 1), mBox.get(2, 2)));
            if (mTypeNames!=null && mTypeNames.length!=0) {
            tWriteln.writeln(String.join(" ", AbstractCollections.map(mTypeNames, type -> String.format("%6s", type))));
            }
            tWriteln.writeln(String.join(" ", AbstractCollections.map(mAtomNumbers.iterable(), number -> String.format("%6d", number))));
            // 再输出原子数据
            for (int i = 0; i < mList.size(); ++i) {
            tWriteln.writeln((mIsCartesian ? "Cartesian" : "Direct") + " configuration= " + (i+1));
            for (IVector subDirect : mList.get(i).rows()) {
            tWriteln.writeln(String.format("%16.10g    %16.10g    %16.10g", subDirect.get(0), subDirect.get(1), subDirect.get(2)));
            }}
        }
    }
    
    
    
    /// 实用功能，这里依旧保留这种写法
    /**
     * 获取多帧原子参数的计算器，支持使用 MFPC 的简写来调用
     * @param aTimestep 实际两帧之间的时间步长
     * @param aType 指定此值来获取只有这个种类的原子的单原子计算器，用于计算只考虑一种元素的一些参数
     * @param aThreadNum 执行 MFPC 的线程数目
     * @return 获取到的 MFPC
     */
    public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType, int aThreadNum) {return new MultiFrameParameterCalculator(AbstractCollections.map(asList(), atomData -> atomData.operation().filterType(aType)), aTimestep, aThreadNum);}
    public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep                           ) {return new MultiFrameParameterCalculator(asList()                                                                             , aTimestep            );}
    public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep,            int aThreadNum) {return new MultiFrameParameterCalculator(asList()                                                                             , aTimestep, aThreadNum);}
    public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType                ) {return new MultiFrameParameterCalculator(AbstractCollections.map(asList(), atomData -> atomData.operation().filterType(aType)), aTimestep            );}
    @VisibleForTesting public MultiFrameParameterCalculator getMFPC          (double aTimestep                           ) {return new MultiFrameParameterCalculator(asList()                                                                             , aTimestep            );}
    @VisibleForTesting public MultiFrameParameterCalculator getMFPC          (double aTimestep,            int aThreadNum) {return new MultiFrameParameterCalculator(asList()                                                                             , aTimestep, aThreadNum);}
    @VisibleForTesting public MultiFrameParameterCalculator getTypeMFPC      (double aTimestep, int aType                ) {return new MultiFrameParameterCalculator(AbstractCollections.map(asList(), atomData -> atomData.operation().filterType(aType)), aTimestep            );}
    @VisibleForTesting public MultiFrameParameterCalculator getTypeMFPC      (double aTimestep, int aType, int aThreadNum) {return new MultiFrameParameterCalculator(AbstractCollections.map(asList(), atomData -> atomData.operation().filterType(aType)), aTimestep, aThreadNum);}
}
