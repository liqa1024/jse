package jtool.vasp;

import jtool.atom.*;
import jtool.code.UT;
import jtool.code.collection.AbstractCollections;
import jtool.math.matrix.IMatrix;
import jtool.math.matrix.Matrices;
import jtool.math.matrix.RowMatrix;
import jtool.math.vector.IVector;
import jtool.math.vector.Vectors;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;

import static jtool.code.CS.ZL_STR;
import static jtool.code.UT.Code.toXYZ;


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
    private final String @NotNull[] mAtomTypes;
    private final IVector mAtomNumbers;
    private final IMatrix mBox;
    private final double mBoxScale;
    
    private List<IMatrix> mDirects;
    
    XDATCAR(String aDataName, IMatrix aBox, double aBoxScale, String @NotNull[] aAtomTypes, IVector aAtomNumbers, IMatrix aFirstDirect, int aInitSize) {
        mDataName = aDataName;
        mAtomTypes = aAtomTypes;
        mAtomNumbers = aAtomNumbers;
        mBox = aBox;
        mBoxScale = aBoxScale;
        mDirects = new ArrayList<>(aInitSize);
        mDirects.add(aFirstDirect);
    }
    XDATCAR(String aDataName, IMatrix aBox, double aBoxScale, String @NotNull[] aAtomTypes, IVector aAtomNumbers, List<IMatrix> aDirects) {
        mDataName = aDataName;
        mAtomTypes = aAtomTypes;
        mAtomNumbers = aAtomNumbers;
        mBox = aBox;
        mBoxScale = aBoxScale;
        mDirects = aDirects;
    }
    
    /** AbstractList stuffs */
    @Override public int size() {return mDirects.size();}
    @Override public POSCAR get(int index) {return new POSCAR(this, false, mDirects.get(index));}
    @Override public POSCAR set(int index, POSCAR aPOSCAR) {return new POSCAR(this, false, mDirects.set(index, aPOSCAR.direct()));}
    @Override public boolean add(POSCAR aPOSCAR) {return mDirects.add(aPOSCAR.direct());}
    @Override public POSCAR remove(int aIndex) {return new POSCAR(this, false, mDirects.remove(aIndex));}
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    public XDATCAR append(IAtomData aAtomData) {
        mDirects.add(getDirect_(aAtomData));
        return this;
    }
    public XDATCAR appendList(Iterable<? extends IAtomData> aAtomDataList) {
        for (IAtomData tAtomData : aAtomDataList) mDirects.add(getDirect_(tAtomData));
        return this;
    }
    public XDATCAR appendFile(String aFilePath) throws IOException {
        mDirects.addAll(read(aFilePath).mDirects);
        return this;
    }
    /** Groovy stuffs，用于支持传入 IAtomData 来设置 */
    void set(int aIdx, IAtomData aAtomData) {mDirects.set(aIdx, getDirect_(aAtomData));}
    @VisibleForTesting void putAt(int aIdx, IAtomData aAtomData) {set(aIdx, aAtomData);}
    
    
    /** 对于 XDATCAR 提供额外的实用接口 */
    public int atomNum(String aKey) {return defaultFrame().atomNum(aKey);}
    public int atomNum(int aType) {return defaultFrame().atomNum(aType);}
    public @Override String dataName() {return mDataName;}
    public @Override String @NotNull[] atomTypes() {return mAtomTypes;}
    public @Override IVector atomNumbers() {return mAtomNumbers;}
    public @Override IMatrix vaspBox() {return mBox;}
    public @Override double vaspBoxScale() {return mBoxScale;}
    public List<IMatrix> directs() {return mDirects;}
    
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
        return new XDATCAR(mDataName, mBox.copy(), mBoxScale, Arrays.copyOf(mAtomTypes, mAtomTypes.length), mAtomNumbers.copy(), rDirects);
    }
    
    /// 创建 XDATCAR
    /** 从 IAtomData 来创建，对于 XDATCAR 可以支持容器的 aAtomData */
    public static XDATCAR fromAtomData(IAtomData aAtomData) {return fromAtomData(aAtomData, (aAtomData instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomData).atomTypes() : ZL_STR);}
    public static XDATCAR fromAtomData(IAtomData aAtomData, String... aAtomTypes) {return fromAtomData_(aAtomData, 1, aAtomTypes);}
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList, (aAtomDataList instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomDataList).atomTypes() : ZL_STR);}
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, String... aAtomTypes) {return fromAtomDataList_(aAtomDataList, 1, aAtomTypes);}
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList, (aAtomDataList instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomDataList).atomTypes() : ZL_STR);}
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, String... aAtomTypes) {return fromAtomDataList_(aAtomDataList, aAtomDataList.size(), aAtomTypes);}
    static XDATCAR fromAtomData_(IAtomData aAtomData, int aInitSize, String[] aAtomTypes) {
        if (aAtomTypes == null) aAtomTypes = ZL_STR;
        if (aAtomTypes.length > 0) aAtomTypes = Arrays.copyOf(aAtomTypes, aAtomTypes.length);
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof IVaspCommonData) {
            // IVaspCommonData 则直接获取即可（专门优化，保留完整模拟盒信息等）
            IVaspCommonData tAtomData = (IVaspCommonData)aAtomData;
            return new XDATCAR(tAtomData.dataName(), tAtomData.vaspBox().copy(), tAtomData.vaspBoxScale(), aAtomTypes, tAtomData.atomNumbers().copy(), getDirectInternal_(aAtomData).copy(), aInitSize);
        } else {
            // 一般的情况，需要构造一下 AtomNumbers
            IVector rAtomNumbers = Vectors.zeros(aAtomData.atomTypeNum());
            for (IAtom tAtom : aAtomData.asList()) rAtomNumbers.increment_(tAtom.type()-1);
            return new XDATCAR(POSCAR.DEFAULT_DATA_NAME, Matrices.diag(aAtomData.box().data()), 1.0, aAtomTypes, rAtomNumbers, getDirect_(aAtomData), aInitSize);
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
    
    static IMatrix getDirect_(IAtomData aAtomData) {
        // 一般的情况，这里直接遍历 atoms 来创建，这里需要按照 type 来排序
        XYZ tBox = toXYZ(aAtomData.box());
        
        int tAtomTypeNum = aAtomData.atomTypeNum();
        IMatrix rDirect = Matrices.zeros(aAtomData.atomNum(), 3);
        int tIdx = 0;
        for (int tTypeMM = 0; tTypeMM < tAtomTypeNum; ++tTypeMM) {
            for (IAtom tAtom : aAtomData.asList()) if (tAtom.type() == tTypeMM+1) {
                rDirect.set(tIdx, 0, tAtom.x()/tBox.mX);
                rDirect.set(tIdx, 1, tAtom.y()/tBox.mY);
                rDirect.set(tIdx, 2, tAtom.z()/tBox.mZ);
                ++tIdx;
            }
        }
        return rDirect;
    }
    static IMatrix getDirectInternal_(IAtomData aAtomData) {
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 Direct
        if (aAtomData instanceof POSCAR) {
            // POSCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            return ((POSCAR)aAtomData).direct();
        } else
        if (aAtomData instanceof XDATCAR) {
            // XDATCAR 则直接获取即可（专门优化，保留完整模拟盒信息等）
            return ((XDATCAR)aAtomData).defaultFrame().direct();
        } else {
            throw new IllegalArgumentException();
        }
    }
    /** 对于 matlab 调用的兼容 */
    public static XDATCAR fromAtomData_compat(Object[] aAtomDataArray) {
        return fromAtomData_compat(aAtomDataArray, ZL_STR);
    }
    public static XDATCAR fromAtomData_compat(Object[] aAtomDataArray, String... aAtomTypes) {
        return fromAtomDataList(AbstractCollections.map(AbstractCollections.filter(AbstractCollections.from(aAtomDataArray), atomData -> (atomData instanceof IAtomData)), obj -> (IAtomData)obj), aAtomTypes);
    }
    
    
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
        IVector aAtomNumbers;
        
        String tLine;
        String[] tTokens;
        // 第一行为 DataName
        tLine = aReader.readLine();
        aDataName = tLine;
        // 读取模拟盒信息
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Texts.splitBlank(tLine);
        aBoxScale = Double.parseDouble(tTokens[0]);
        aBox = Matrices.zeros(3);
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBox.row(0).fill(UT.Texts.str2data(tLine, 3));
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBox.row(1).fill(UT.Texts.str2data(tLine, 3));
        tLine = aReader.readLine(); if (tLine == null) return null;
        aBox.row(2).fill(UT.Texts.str2data(tLine, 3));
        // 读取原子种类（可选）和对应数目的信息
        boolean tNoAtomType = false;
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Texts.splitBlank(tLine);
        aAtomTypes = tTokens;
        tLine = aReader.readLine(); if (tLine == null) return null; tTokens = UT.Texts.splitBlank(tLine);
        try {
        aAtomNumbers = Vectors.from(AbstractCollections.map(tTokens, Integer::parseInt));
        } catch (Exception e) {
        tNoAtomType = true;
        aAtomNumbers = Vectors.from(AbstractCollections.map(aAtomTypes, Integer::parseInt));
        aAtomTypes = ZL_STR;
        }
        if (!tNoAtomType) {
        tLine = aReader.readLine(); if (tLine == null) return null;
        }
        // 目前只支持 Direct，方便起见这里只进行一次检测
        if (!UT.Texts.containsIgnoreCase(tLine, "Direct")) {
        throw new RuntimeException("Can ONLY read Direct POSCAR temporarily");
        }
        
        // 再读取原子数据
        List<IMatrix> rDirects = new ArrayList<>();
        int tAtomNum = (int)aAtomNumbers.sum();
        while (true) {
            IMatrix subDirect = RowMatrix.zeros(tAtomNum, 3);
            
            boolean tIsAtomDataReadFull = true;
            for (IVector tRow : subDirect.rows()) {
                tLine = aReader.readLine();
                if (tLine == null) {tIsAtomDataReadFull = false; break;}
                tRow.fill(UT.Texts.str2data(tLine, 3));
            }
            if (!tIsAtomDataReadFull) break;
            
            rDirects.add(subDirect);
            // 跳过下一个的 Direct configuration，如果没有则终止循环
            tLine = aReader.readLine(); if (tLine == null) break;
        }
        return new XDATCAR(aDataName, aBox, aBoxScale, aAtomTypes, aAtomNumbers, rDirects);
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
        try (BufferedWriter tWriter = UT.IO.toWriter(aFilePath)) {
            // 先输出通用信息
            tWriter.write(mDataName); tWriter.newLine();
            tWriter.write(String.valueOf(mBoxScale)); tWriter.newLine();
            tWriter.write(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(0, 0), mBox.get(0, 1), mBox.get(0, 2))); tWriter.newLine();
            tWriter.write(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(1, 0), mBox.get(1, 1), mBox.get(1, 2))); tWriter.newLine();
            tWriter.write(String.format("    %16.10g    %16.10g    %16.10g", mBox.get(2, 0), mBox.get(2, 1), mBox.get(2, 2))); tWriter.newLine();
            if (mAtomTypes.length!=0) {
            tWriter.write(String.join(" ", AbstractCollections.map(mAtomTypes, type -> String.format("%6s", type)))); tWriter.newLine();
            }
            tWriter.write(String.join(" ", AbstractCollections.map(mAtomNumbers.iterable(), number -> String.format("%6d", number.intValue())))); tWriter.newLine();
            // 再输出原子数据
            for (int i = 0; i < mDirects.size(); ++i) {
            tWriter.write("Direct configuration= " + (i+1)); tWriter.newLine();
            for (IVector subDirect : mDirects.get(i).rows()) {
            tWriter.write(String.format("%16.10g    %16.10g    %16.10g", subDirect.get(0), subDirect.get(1), subDirect.get(2))); tWriter.newLine();
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
