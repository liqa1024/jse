package jse.vasp;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import jse.atom.*;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
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


/**
 * @author liqa
 * <p> vasp 读取输出的多帧原子位置格式，每帧为 {@link POSCAR} </p>
 * <p> 包含读取写出的文本文件的格式 </p>
 * <p> 暂时不支持边界条件设置 </p>
 * <p> 暂时不考虑 Direct configuration 间距不为 1 的情况，因此 MFPC 不会统计两帧之间跳过的部分 </p>
 * <p> 返回的 {@link POSCAR} 共享原子位置但是不共享 mBoxScale 以及 mSelectiveDynamics（除了原子位置其余是否共享属于未定义）</p>
 */
public class XDATCAR extends AbstractMultiFrameSettableAtomData<POSCAR> implements IVaspCommonData {
    /** 这里统一存放通用数据保证所有帧这些一定是相同的 */
    private final String mDataName;
    private String @Nullable[] mAtomTypes;
    private IIntVector mAtomNumbers;
    private final IMatrix mBox;
    private final double mBoxScale;
    private final boolean mIsDiagBox;
    /** 保存一份 id 列表，这样在 lmpdat 转为 poscar 时会继续保留 id 信息，XDATCAR 认为不能进行修改 */
    private final @Nullable IIntVector mIDs;
    private final @Nullable @Unmodifiable Map<Integer, Integer> mId2Index; // 原子的 id 转为存储在 AtomDataXYZ 的指标 index
    
    private List<IMatrix> mDirects;
    private boolean mIsCartesian; // 如果是 Cartesian 则是没有进行缩放的
    
    /** 用于通过字符获取每个种类的粒子数，考虑了可能有相同 key 的情况 */
    private final @NotNull Multimap<String, Integer> mKey2Type;
    
    XDATCAR(String aDataName, IMatrix aBox, double aBoxScale, String @Nullable[] aAtomTypes, IIntVector aAtomNumbers, List<IMatrix> aDirects, boolean aIsCartesian, @Nullable IIntVector aIDs) {
        mDataName = aDataName;
        mAtomTypes = aAtomTypes;
        mAtomNumbers = aAtomNumbers;
        mBox = aBox;
        mBoxScale = aBoxScale;
        mDirects = aDirects;
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
        if (mAtomTypes != null) {
            int rType = 0;
            for (String tKey : mAtomTypes) {
                ++rType;
                mKey2Type.put(tKey, rType);
            }
        }
        
        mIsDiagBox = mBox.operation().isDiag();
    }
    XDATCAR(String aDataName, IMatrix aBox, double aBoxScale, String @Nullable[] aAtomTypes, IIntVector aAtomNumbers, IMatrix aFirstDirect, int aInitSize, boolean aIsCartesian, @Nullable IIntVector aIDs) {
        this(aDataName, aBox, aBoxScale, aAtomTypes, aAtomNumbers, new ArrayList<>(aInitSize), aIsCartesian, aIDs);
        mDirects.add(aFirstDirect);
    }
    
    /** AbstractList stuffs */
    @Override public int size() {return mDirects.size();}
    @Override public POSCAR get(int index) {
        return new POSCAR(this, false, mDirects.get(index));
    }
    @Override public POSCAR set(int index, POSCAR aPOSCAR) {
        // 只有正交的 XDATCAR 才能设置内部元素
        if (!mIsDiagBox) throw new RuntimeException("set is support Diagonal Box only");
        return new POSCAR(this, false, mDirects.set(index, getDirect_(aPOSCAR)));
    }
    @Override public boolean add(POSCAR aPOSCAR) {
        // 只有正交的 XDATCAR 才能设置内部元素
        if (!mIsDiagBox) throw new RuntimeException("add is support Diagonal Box only");
        return mDirects.add(getDirect_(aPOSCAR));
    }
    @Override public POSCAR remove(int aIndex) {
        return new POSCAR(this, false, mDirects.remove(aIndex));
    }
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    public XDATCAR append(IAtomData aAtomData) {
        // 只有正交的 XDATCAR 才能通过 AtomData 设置内部元素
        if (!mIsDiagBox) throw new RuntimeException("append is support Diagonal Box only");
        mDirects.add(getDirect_(aAtomData));
        return this;
    }
    public XDATCAR appendList(Iterable<? extends IAtomData> aAtomDataList) {
        // 只有正交的 XDATCAR 才能通过 AtomData 设置内部元素
        if (!mIsDiagBox) throw new RuntimeException("appendList is support Diagonal Box only");
        for (IAtomData tAtomData : aAtomDataList) mDirects.add(getDirect_(tAtomData));
        return this;
    }
    public XDATCAR appendFile(String aFilePath) throws IOException {
        // 原则上只有正交的或者 box 完全相同才可以添加，这里直接通过 refDirect_ 的内部进行判断，即使 box 不相同也会可以添加
        final XDATCAR tXDATCAR = read(aFilePath);
        mDirects.addAll(AbstractCollections.map(tXDATCAR.mDirects, direct -> refDirect_(tXDATCAR.mBox, tXDATCAR.mBoxScale, mBoxScale, direct, tXDATCAR.mIsCartesian, mIsCartesian)));
        return this;
    }
    /** Groovy stuffs，用于支持传入 IAtomData 来设置 */
    void set(int aIdx, IAtomData aAtomData) {
        // 只有正交的 XDATCAR 才能通过 AtomData 设置内部元素
        if (!mIsDiagBox) throw new RuntimeException("set is support Diagonal Box only");
        mDirects.set(aIdx, getDirect_(aAtomData));
    }
    @VisibleForTesting void putAt(int aIdx, IAtomData aAtomData) {set(aIdx, aAtomData);}
    
    
    /** 对于 XDATCAR 提供额外的实用接口 */
    public int atomNum(String aKey) {
        int rAtomNum = 0;
        for (int tType : mKey2Type.get(aKey)) rAtomNum += atomNum(tType);
        return rAtomNum;
    }
    public int atomNum(int aType) {return mAtomNumbers.get(aType-1);}
    public @Override String dataName() {return mDataName;}
    public @Override String @Nullable[] atomTypes() {return mAtomTypes;}
    public @Override IIntVector atomNumbers() {return mAtomNumbers;}
    public @Override IMatrix vaspBox() {return mBox;}
    public @Override double vaspBoxScale() {return mBoxScale;}
    public List<IMatrix> directs() {return mDirects;}
    public @Override boolean isCartesian() {return mIsCartesian;}
    public @Override boolean isDiagBox() {return mIsDiagBox;}
    public @Override @Nullable IIntVector ids() {return mIDs;}
    
    
    /** 支持直接修改 AtomTypes，只会增大种类数，不会减少 */
    public XDATCAR setAtomTypes(String... aAtomTypes) {
        if (aAtomTypes==null || aAtomTypes.length==0) {
            mAtomTypes = null;
            mKey2Type.clear();
            return this;
        }
        if (mAtomTypes==null || aAtomTypes.length>mAtomTypes.length) mAtomTypes = Arrays.copyOf(aAtomTypes, aAtomTypes.length);
        else System.arraycopy(aAtomTypes, 0, mAtomTypes, 0, aAtomTypes.length);
        if (aAtomTypes.length > mAtomNumbers.size()) {
            IIntVector oAtomNumbers = mAtomNumbers;
            mAtomNumbers = IntVector.zeros(aAtomTypes.length);
            mAtomNumbers.subVec(0, oAtomNumbers.size()).fill(oAtomNumbers);
        }
        mKey2Type.clear();
        int rType = 0;
        for (String tKey : mAtomTypes) {
            ++rType;
            mKey2Type.put(tKey, rType);
        }
        return this;
    }
    
    public XDATCAR setCartesian() {
        if (mIsCartesian) return this;
        // 注意如果是斜方的模拟盒则不能进行转换
        if (!mIsDiagBox) throw new RuntimeException("converting between Cartesian and Direct is temporarily support Diagonal Box only");
        for (IMatrix tDirect : mDirects) {
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
        for (IMatrix tDirect : mDirects) {
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
        if (aLength > mDirects.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        List<IMatrix> oData = mDirects;
        mDirects = new ArrayList<>(oData.size() - aLength);
        Iterator<IMatrix> it = oData.listIterator(aLength);
        while (it.hasNext()) mDirects.add(it.next());
        return this;
    }
    /** 截断结尾一部分, 返回自身来支持链式调用 */
    public XDATCAR cutBack(int aLength) {
        if (aLength <= 0) return this;
        if (aLength > mDirects.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        for (int i = 0; i < aLength; ++i) UT.Code.removeLast(mDirects);
        return this;
    }
    
    /** 拷贝一份 XDATCAR */
    @Override public XDATCAR copy() {
        List<IMatrix> rDirects = new ArrayList<>(mDirects.size());
        for (IMatrix subDirect : mDirects) rDirects.add(subDirect.copy());
        return new XDATCAR(mDataName, mBox.copy(), mBoxScale, POSCAR.copyTypes(mAtomTypes), mAtomNumbers.copy(), rDirects, mIsCartesian, POSCAR.copyIDs(mIDs));
    }
    
    /// 创建 XDATCAR
    /** 从 IAtomData 来创建，对于 XDATCAR 可以支持容器的 aAtomData */
    public static XDATCAR fromAtomData(IAtomData aAtomData) {return fromAtomData(aAtomData, (aAtomData instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomData).atomTypes() : null);}
    public static XDATCAR fromAtomData(IAtomData aAtomData, String... aAtomTypes) {return fromAtomData_(aAtomData, 1, aAtomTypes);}
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList, (aAtomDataList instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomDataList).atomTypes() : null);}
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, String... aAtomTypes) {return fromAtomDataList_(aAtomDataList, 1, aAtomTypes);}
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList, (aAtomDataList instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomDataList).atomTypes() : null);}
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, String... aAtomTypes) {return fromAtomDataList_(aAtomDataList, aAtomDataList.size(), aAtomTypes);}
    static XDATCAR fromAtomData_(IAtomData aAtomData, int aInitSize, String[] aAtomTypes) {
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 Direct
        if (aAtomData instanceof POSCAR) {
            // POSCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            POSCAR tPOSCAR = (POSCAR)aAtomData;
            return new XDATCAR(tPOSCAR.dataName(), tPOSCAR.vaspBox().copy(), tPOSCAR.vaspBoxScale(), POSCAR.copyTypes(aAtomTypes), tPOSCAR.atomNumbers().copy(), tPOSCAR.direct().copy(), aInitSize, tPOSCAR.isCartesian(), POSCAR.copyIDs(tPOSCAR.ids()));
        } else
        if (aAtomData instanceof XDATCAR) {
            // XDATCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            return fromAtomData_(((XDATCAR)aAtomData).defaultFrame(), aInitSize, aAtomTypes);
        } else {
            // 一般的情况，这里直接遍历 atoms 来创建，这里需要按照 type 来排序
            IIntVector rIDs = IntVector.zeros(aAtomData.atomNumber());
            int tAtomTypeNum = aAtomData.atomTypeNumber();
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
            return new XDATCAR(POSCAR.DEFAULT_DATA_NAME, Matrices.diag(aAtomData.box().data()), 1.0, POSCAR.copyTypes(aAtomTypes), rAtomNumbers, rDirect, aInitSize, true, rIDs);
        }
    }
    static XDATCAR fromAtomDataList_(Iterable<? extends IAtomData> aAtomDataList, int aInitSize, String[] aAtomTypes) {
        // 直接不支持空的创建来简化实现的代码
        if (aAtomDataList == null) throw new IllegalArgumentException("XDATCAR do NOT support empty AtomDataList");
        Iterator<? extends IAtomData> it = aAtomDataList.iterator();
        if (!it.hasNext()) throw new IllegalArgumentException("XDATCAR do NOT support empty AtomDataList");
        IAtomData first = it.next();
        final XDATCAR rXDATCAR = fromAtomData_(first, aInitSize, ((aAtomTypes==null || aAtomTypes.length==0) && (first instanceof IVaspCommonData)) ? ((IVaspCommonData)first).atomTypes() : aAtomTypes);
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
        if (aToCartesian) return new RefMatrix() {
            @Override public double get(int aRow, int aCol) {return aDirect.get(aRow, aCol) * aBox.get(aCol, aCol) * tScale;}
            @Override public void set(int aRow, int aCol, double aValue) {aDirect.set(aRow, aCol, aValue / aBox.get(aCol, aCol) / tScale);}
            @Override public int rowNumber() {return aDirect.rowNumber();}
            @Override public int columnNumber() {return aDirect.columnNumber();}
        };
        else return new RefMatrix() {
            @Override public double get(int aRow, int aCol) {return aDirect.get(aRow, aCol) / aBox.get(aCol, aCol) * tScale;}
            @Override public void set(int aRow, int aCol, double aValue) {aDirect.set(aRow, aCol, aValue * aBox.get(aCol, aCol) / tScale);}
            @Override public int rowNumber() {return aDirect.rowNumber();}
            @Override public int columnNumber() {return aDirect.columnNumber();}
        };
    }
    /** 对于 matlab 调用的兼容 */
    public static XDATCAR fromAtomData_compat(Object[] aAtomDataArray) {
        return fromAtomData_compat(aAtomDataArray, (String[])null);
    }
    public static XDATCAR fromAtomData_compat(Object[] aAtomDataArray, String... aAtomTypes) {
        return fromAtomDataList(AbstractCollections.map(AbstractCollections.filter(AbstractCollections.from(aAtomDataArray), atomData -> (atomData instanceof IAtomData)), obj -> (IAtomData)obj), aAtomTypes);
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static XDATCAR of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static XDATCAR of(IAtomData aAtomData, String... aAtomTypes) {return fromAtomData(aAtomData, aAtomTypes);}
    public static XDATCAR ofList(Iterable<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    public static XDATCAR ofList(Iterable<? extends IAtomData> aAtomDataList, String... aAtomTypes) {return fromAtomDataList(aAtomDataList, aAtomTypes);}
    public static XDATCAR ofList(Collection<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    public static XDATCAR ofList(Collection<? extends IAtomData> aAtomDataList, String... aAtomTypes) {return fromAtomDataList(aAtomDataList, aAtomTypes);}
    public static XDATCAR of_compat(Object[] aAtomDataArray) {return fromAtomData_compat(aAtomDataArray);}
    public static XDATCAR of_compat(Object[] aAtomDataArray, String... aAtomTypes) {return fromAtomData_compat(aAtomDataArray, aAtomTypes);}
    
    
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
    public static XDATCAR read_(BufferedReader aReader) throws IOException {
        // 先读通用信息
        String aDataName;
        IMatrix aBox;
        double aBoxScale;
        String[] aAtomTypes;
        IIntVector aAtomNumbers;
        boolean aIsCartesian;
        
        String tLine;
        String[] tTokens;
        // 第一行为 DataName
        tLine = aReader.readLine();
        aDataName = tLine;
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
        aAtomTypes = tTokens;
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Text.splitBlank(tLine);
        try {
        final String[] fTokens = tTokens;
        aAtomNumbers = Vectors.fromInteger(fTokens.length, i -> Integer.parseInt(fTokens[i]));
        } catch (Exception e) {
        tNoAtomType = true;
        final String[] fTokens = aAtomTypes;
        aAtomNumbers = Vectors.fromInteger(fTokens.length, i -> Integer.parseInt(fTokens[i]));
        aAtomTypes = null;
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
        return new XDATCAR(aDataName, aBox, aBoxScale, aAtomTypes, aAtomNumbers, rDirects, aIsCartesian, null);
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
            tWriteln.writeln(mDataName);
            tWriteln.writeln(String.valueOf(mBoxScale));
            tWriteln.writeln(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(0, 0), mBox.get(0, 1), mBox.get(0, 2)));
            tWriteln.writeln(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(1, 0), mBox.get(1, 1), mBox.get(1, 2)));
            tWriteln.writeln(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(2, 0), mBox.get(2, 1), mBox.get(2, 2)));
            if (mAtomTypes!=null && mAtomTypes.length!=0) {
            tWriteln.writeln(String.join(" ", AbstractCollections.map(mAtomTypes, type -> String.format("%6s", type))));
            }
            tWriteln.writeln(String.join(" ", AbstractCollections.map(mAtomNumbers.iterable(), number -> String.format("%6d", number))));
            // 再输出原子数据
            for (int i = 0; i < mDirects.size(); ++i) {
            tWriteln.writeln((mIsCartesian ? "Cartesian" : "Direct") + " configuration= " + (i+1));
            for (IVector subDirect : mDirects.get(i).rows()) {
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
    public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType, int aThreadNum) {return new MultiFrameParameterCalculator(AbstractCollections.map(this, atomData -> atomData.operation().filterType(aType)), aTimestep, aThreadNum);}
    public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep                           ) {return new MultiFrameParameterCalculator(this                                                                             , aTimestep            );}
    public MultiFrameParameterCalculator getMultiFrameParameterCalculator    (double aTimestep,            int aThreadNum) {return new MultiFrameParameterCalculator(this                                                                             , aTimestep, aThreadNum);}
    public MultiFrameParameterCalculator getTypeMultiFrameParameterCalculator(double aTimestep, int aType                ) {return new MultiFrameParameterCalculator(AbstractCollections.map(this, atomData -> atomData.operation().filterType(aType)), aTimestep            );}
    @VisibleForTesting public MultiFrameParameterCalculator getMFPC          (double aTimestep                           ) {return new MultiFrameParameterCalculator(this                                                                             , aTimestep            );}
    @VisibleForTesting public MultiFrameParameterCalculator getMFPC          (double aTimestep,            int aThreadNum) {return new MultiFrameParameterCalculator(this                                                                             , aTimestep, aThreadNum);}
    @VisibleForTesting public MultiFrameParameterCalculator getTypeMFPC      (double aTimestep, int aType                ) {return new MultiFrameParameterCalculator(AbstractCollections.map(this, atomData -> atomData.operation().filterType(aType)), aTimestep            );}
    @VisibleForTesting public MultiFrameParameterCalculator getTypeMFPC      (double aTimestep, int aType, int aThreadNum) {return new MultiFrameParameterCalculator(AbstractCollections.map(this, atomData -> atomData.operation().filterType(aType)), aTimestep, aThreadNum);}
}
