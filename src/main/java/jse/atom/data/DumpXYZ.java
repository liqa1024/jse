package jse.atom.data;

import jse.atom.IAtomData;
import jse.code.UT;
import jse.code.collection.AbstractListWrapper;
import jse.code.collection.NewCollections;
import org.jetbrains.annotations.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;


/**
 * 多帧的 {@link DataXYZ}
 * @author liqa
 */
public class DumpXYZ extends AbstractListWrapper<DataXYZ, IAtomData, DataXYZ> {
    DumpXYZ() {super(NewCollections.zl());}
    DumpXYZ(DataXYZ... aData) {super(NewCollections.from(aData));}
    DumpXYZ(List<DataXYZ> aData) {super(aData);}
    
    /** AbstractListWrapper stuffs */
    @Override protected final DataXYZ toInternal_(IAtomData aAtomData) {return DataXYZ.fromAtomData(aAtomData);}
    @Override protected final DataXYZ toOutput_(DataXYZ aDataXYZ) {return aDataXYZ;}
    
    /** 提供更加易用的添加方法，返回自身支持链式调用 */
    @Override public DumpXYZ append(IAtomData aAtomData) {return (DumpXYZ)super.append(aAtomData);}
    @Override public DumpXYZ appendAll(Collection<? extends IAtomData> aAtomDataList) {return (DumpXYZ)super.appendAll(aAtomDataList);}
    public DumpXYZ appendFile(String aFilePath) throws IOException {
        mList.addAll(read(aFilePath).mList);
        return this;
    }
    /** groovy stuffs */
    @Override public DumpXYZ leftShift(IAtomData aAtomData) {return (DumpXYZ)super.leftShift(aAtomData);}
    @Override public DumpXYZ leftShift(Collection<? extends IAtomData> aAtomDataList) {return (DumpXYZ)super.leftShift(aAtomDataList);}
    
    /** 截断开头一部分, 返回自身来支持链式调用 */
    public DumpXYZ cutFront(@Range(from=0, to=Integer.MAX_VALUE) int aLength) {
        if (aLength == 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        List<DataXYZ> oList = mList;
        mList = new ArrayList<>(oList.size() - aLength);
        Iterator<DataXYZ> it = oList.listIterator(aLength);
        while (it.hasNext()) mList.add(it.next());
        return this;
    }
    /** 截断结尾一部分, 返回自身来支持链式调用 */
    public DumpXYZ cutBack(@Range(from=0, to=Integer.MAX_VALUE) int aLength) {
        if (aLength == 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        for (int i = 0; i < aLength; ++i) removeLast();
        return this;
    }
    
    /** 拷贝一份 Lammpstrj */
    public DumpXYZ copy() {
        List<DataXYZ> rData = new ArrayList<>(mList.size());
        for (DataXYZ subData : mList) rData.add(subData.copy());
        return new DumpXYZ(rData);
    }
    
    /// 创建 DumpXYZ
    /** 从 IAtomData 来创建，对于 DumpXYZ 可以支持容器的 aAtomData */
    public static DumpXYZ fromAtomData(IAtomData aAtomData) {
        return new DumpXYZ(DataXYZ.fromAtomData(aAtomData));
    }
    public static DumpXYZ fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new DumpXYZ();
        List<DataXYZ> rDumpXYZ = new ArrayList<>();
        for (IAtomData subAtomData : aAtomDataList) {
            rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData));
        }
        return new DumpXYZ(rDumpXYZ);
    }
    public static DumpXYZ fromAtomDataList(Collection<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new DumpXYZ();
        List<DataXYZ> rDumpXYZ = new ArrayList<>(aAtomDataList.size());
        for (IAtomData subAtomData : aAtomDataList) {
            rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData));
        }
        return new DumpXYZ(rDumpXYZ);
    }
    /** 对于 matlab 调用的兼容 */
    public static DumpXYZ fromAtomData_compat(Object[] aAtomDataArray) {
        if (aAtomDataArray==null || aAtomDataArray.length==0) return new DumpXYZ();
        List<DataXYZ> rDumpXYZ = new ArrayList<>();
        for (Object subAtomData : aAtomDataArray) if (subAtomData instanceof IAtomData) {
            rDumpXYZ.add(DataXYZ.fromAtomData((IAtomData)subAtomData));
        }
        return new DumpXYZ(rDumpXYZ);
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static DumpXYZ zl() {return new DumpXYZ();}
    public static DumpXYZ of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static DumpXYZ of(Iterable<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    public static DumpXYZ of(Collection<? extends IAtomData> aAtomDataList) {return fromAtomDataList(aAtomDataList);}
    /** 再提供一个 IListWrapper 的接口保证 DumpXYZ 也能输入 */
    public static DumpXYZ of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList) {return fromAtomDataList(aAtomDataList.asList());}
    /** matlab stuffs */
    public static DumpXYZ of_compat(Object[] aAtomDataArray) {return fromAtomData_compat(aAtomDataArray);}
    
    
    /// 文件读写
    /**
     * 从多个 XYZ 的文件读取
     * @author liqa
     * @param aFilePath XYZ 文件路径
     * @return 读取得到的 DumpXYZ 对象，如果文件不完整的帧会跳过
     * @throws IOException 如果读取失败
     */
    public static DumpXYZ read(String aFilePath) throws IOException {try (BufferedReader tReader = UT.IO.toReader(aFilePath)) {return read_(tReader);}}
    /** 改为 {@link BufferedReader} 而不是 {@code List<String>} 来避免过多内存占用 */
    static DumpXYZ read_(BufferedReader aReader) throws IOException {
        List<DataXYZ> rDumpXYZ = new ArrayList<>();
        DataXYZ tDataXYZ;
        while ((tDataXYZ = DataXYZ.read_(aReader)) != null) {
            rDumpXYZ.add(tDataXYZ);
        }
        return new DumpXYZ(rDumpXYZ);
    }
    
    /**
     * 输出成 lammps 格式的 dump 文件，可以供 OVITO 等软件读取
     * @author liqa
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {try (UT.IO.IWriteln tWriteln = UT.IO.toWriteln(aFilePath)) {write_(tWriteln);}}
    /** 改为 {@link UT.IO.IWriteln} 而不是 {@code List<String>} 来避免过多内存占用 */
    void write_(UT.IO.IWriteln aWriteln) throws IOException {
        for (DataXYZ tDataXYZ : mList) tDataXYZ.write_(aWriteln);
    }
}
