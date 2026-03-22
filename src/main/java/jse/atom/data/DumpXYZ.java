package jse.atom.data;

import jse.atom.IAtomData;
import jse.code.IO;
import jse.code.collection.AbstractListWrapper;
import jse.code.collection.IListGetter;
import jse.code.collection.NewCollections;
import org.jetbrains.annotations.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * 多帧的 {@link DataXYZ}，通过：
 * <pre> {@code
 * def data = dump[i]
 * } </pre>
 * 来直接获取某一帧的 {@link DataXYZ} 数据，从而可以进行原子数据的相关操作，
 * 或者通过：
 * <pre> {@code
 * for (data in dump.asList()) {
 *     //...
 * }
 * } </pre>
 * 来将多帧数据转成 {@link List} 后进行遍历
 *
 * @see DataXYZ DataXYZ: XYZ 格式
 * @see #read(String) read(String): 读取指定路径的多帧 XYZ 格式原子数据
 * @see #write(String) write(String): 将此多帧 XYZ 原子数据写入指定路径
 * @see #of(IAtomData) of(IAtomData): 将任意的原子数据转换成多帧 XYZ 类型
 * @author liqa
 */
public class DumpXYZ extends AbstractListWrapper<DataXYZ, IAtomData, DataXYZ> {
    DumpXYZ() {super(NewCollections.zl());}
    DumpXYZ(DataXYZ... aData) {super(NewCollections.from(aData));}
    DumpXYZ(List<DataXYZ> aData) {super(aData);}
    
    /// AbstractListWrapper stuffs
    /** 将输入的原子数据转换成内部存储的数据，这里的实现会导致添加原子数据统一进行一次值拷贝 */
    @Override protected final DataXYZ toInternal_(IAtomData aAtomData) {return DataXYZ.fromAtomData(aAtomData);}
    /** 将内部存储的数据转换成外部访问会获取到的类型，这里的实现直接返回引用 */
    @Override protected final DataXYZ toOutput_(DataXYZ aDataXYZ) {return aDataXYZ;}
    
    /**
     * 添加一帧原子数据，会读取原子数据值并值拷贝
     * @param aAtomData 需要添加的任意的原子数据
     * @return 自身方便链式调用
     * @see IAtomData
     */
    @Override public DumpXYZ append(IAtomData aAtomData) {
        return (DumpXYZ)super.append(aAtomData);
    }
    /**
     * 添加多帧原子数据，会遍历读取原子数据并值拷贝
     * @param aAtomDataList 需要添加的任意的原子数据组成的列表
     * @return 自身方便链式调用
     * @see Collection
     */
    @Override public DumpXYZ appendAll(Collection<? extends IAtomData> aAtomDataList) {
        return (DumpXYZ)super.appendAll(aAtomDataList);
    }
    /**
     * 直接添加一个多帧 XYZ 文件
     * @param aFilePath 多帧的 XYZ 文件路径
     * @return 自身方便链式调用
     * @throws IOException 当读取失败
     * @see #read(String)
     */
    public DumpXYZ appendFile(String aFilePath) throws IOException {
        mList.addAll(read(aFilePath).mList);
        return this;
    }
    /// groovy stuffs
    /** @see #append(IAtomData) */
    @Override public DumpXYZ leftShift(IAtomData aAtomData) {
        return (DumpXYZ)super.leftShift(aAtomData);
    }
    /** @see #appendAll(Collection) */
    @Override public DumpXYZ leftShift(Collection<? extends IAtomData> aAtomDataList) {
        return (DumpXYZ)super.leftShift(aAtomDataList);
    }
    
    /**
     * 截断多帧 XYZ 开头的一部分，主要用于移除未平衡的部分
     * @param aLength 需要截断的帧数
     * @return 自身方便链式调用
     */
    public DumpXYZ cutFront(@Range(from=0, to=Integer.MAX_VALUE) int aLength) {
        if (aLength == 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        List<DataXYZ> oList = mList;
        mList = new ArrayList<>(oList.size() - aLength);
        Iterator<DataXYZ> it = oList.listIterator(aLength);
        while (it.hasNext()) mList.add(it.next());
        return this;
    }
    /**
     * 截断多帧 XYZ 结尾的一部分
     * @param aLength 需要截断的帧数
     * @return 自身方便链式调用
     */
    public DumpXYZ cutBack(@Range(from=0, to=Integer.MAX_VALUE) int aLength) {
        if (aLength == 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        for (int i = 0; i < aLength; ++i) removeLast();
        return this;
    }
    
    /** @return 自身的拷贝 */
    public DumpXYZ copy() {
        List<DataXYZ> rData = new ArrayList<>(mList.size());
        for (DataXYZ subData : mList) rData.add(subData.copy());
        return new DumpXYZ(rData);
    }
    
    /// 创建 DumpXYZ
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个多帧的 XYZ 数据（内部只有一帧）
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #fromAtomData(IAtomData, String...)} 来手动指定元素符号信息
     * <p>
     * {@link #of(IAtomData)} 为等价的别名方法
     *
     * @param aAtomData 输入的原子数据
     * @return 创建的多帧的 XYZ 数据
     * @see DataXYZ#fromAtomData(IAtomData)
     * @see #of(IAtomData)
     * @see #fromAtomData(IAtomData, String...)
     */
    public static DumpXYZ fromAtomData(IAtomData aAtomData) {
        return new DumpXYZ(DataXYZ.fromAtomData(aAtomData));
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个多帧的 XYZ 数据（内部只有一帧）
     * <p>
     * {@link #of(IAtomData, String...)} 为等价的别名方法
     *
     * @param aAtomData 输入的原子数据
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see DataXYZ#fromAtomData(IAtomData, String...)
     * @see #of(IAtomData, String...)
     * @see #fromAtomData(IAtomData)
     */
    public static DumpXYZ fromAtomData(IAtomData aAtomData, String... aSymbols) {
        return new DumpXYZ(DataXYZ.fromAtomData(aAtomData, aSymbols));
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #fromAtomDataList(Iterable, String...)} 来手动指定元素符号信息
     * <p>
     * {@link #of(Iterable)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Iterable)
     * @see #fromAtomDataList(Iterable, String...)
     */
    public static DumpXYZ fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new DumpXYZ();
        List<DataXYZ> rDumpXYZ = new ArrayList<>();
        for (IAtomData subAtomData : aAtomDataList) {
            rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData));
        }
        return new DumpXYZ(rDumpXYZ);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     * <p>
     * {@link #of(Iterable, String...)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Iterable, String...)
     * @see #fromAtomDataList(Iterable)
     */
    public static DumpXYZ fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, final String... aSymbols) {
        return fromAtomDataList(aAtomDataList, i -> aSymbols);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     * <p>
     * {@link #of(Iterable, IListGetter)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbolsGetter 可选的指定帧对应的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Iterable, IListGetter)
     * @see #fromAtomDataList(Iterable)
     */
    @SuppressWarnings("unchecked")
    public static DumpXYZ fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, IListGetter<Object> aSymbolsGetter) {
        if (aAtomDataList == null) return new DumpXYZ();
        List<DataXYZ> rDumpXYZ = new ArrayList<>();
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            Object tSymbols = aSymbolsGetter.get(i);
            if (tSymbols instanceof String[]) {
                rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData, (String[])tSymbols));
            } else
            if (tSymbols instanceof Collection) {
                rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData, (Collection<? extends CharSequence>)tSymbols));
            } else {
                rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData));
            }
            ++i;
        }
        return new DumpXYZ(rDumpXYZ);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #fromAtomDataList(Collection, String...)} 来手动指定元素符号信息
     * <p>
     * {@link #of(Collection)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Collection)
     * @see #fromAtomDataList(Collection, String...)
     */
    public static DumpXYZ fromAtomDataList(Collection<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new DumpXYZ();
        List<DataXYZ> rDumpXYZ = new ArrayList<>(aAtomDataList.size());
        for (IAtomData subAtomData : aAtomDataList) {
            rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData));
        }
        return new DumpXYZ(rDumpXYZ);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     * <p>
     * {@link #of(Collection, String...)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Collection, String...)
     * @see #fromAtomDataList(Collection)
     */
    public static DumpXYZ fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, final String... aSymbols) {
        return fromAtomDataList(aAtomDataList, i -> aSymbols);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     * <p>
     * {@link #of(Collection, IListGetter)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbolsGetter 可选的指定帧对应的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Collection, IListGetter)
     * @see #fromAtomDataList(Collection)
     */
    @SuppressWarnings("unchecked")
    public static DumpXYZ fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, IListGetter<Object> aSymbolsGetter) {
        if (aAtomDataList == null) return new DumpXYZ();
        List<DataXYZ> rDumpXYZ = new ArrayList<>(aAtomDataList.size());
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            Object tSymbols = aSymbolsGetter.get(i);
            if (tSymbols instanceof String[]) {
                rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData, (String[])tSymbols));
            } else
            if (tSymbols instanceof Collection) {
                rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData, (Collection<? extends CharSequence>)tSymbols));
            } else {
                rDumpXYZ.add(DataXYZ.fromAtomData(subAtomData));
            }
            ++i;
        }
        return new DumpXYZ(rDumpXYZ);
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #fromAtomData(IAtomData, String...)
     * @see Collection
     */
    public static DumpXYZ fromAtomData(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {
        return fromAtomData(aAtomData, IO.Text.toArray(aSymbols));
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #fromAtomDataList(Iterable, String...)
     * @see Collection
     */
    public static DumpXYZ fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aSymbols) {
        return fromAtomDataList(aAtomDataList, IO.Text.toArray(aSymbols));
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #fromAtomDataList(Collection, String...)
     * @see Collection
     */
    public static DumpXYZ fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aSymbols) {
        return fromAtomDataList(aAtomDataList, IO.Text.toArray(aSymbols));
    }
    /**
     * 对于 matlab 调用的兼容方法
     * @see #fromAtomDataList(Collection)
     */
    public static DumpXYZ fromAtomData_compat(Object[] aAtomDataArray) {
        if (aAtomDataArray==null || aAtomDataArray.length==0) return new DumpXYZ();
        List<DataXYZ> rDumpXYZ = new ArrayList<>();
        for (Object subAtomData : aAtomDataArray) if (subAtomData instanceof IAtomData) {
            rDumpXYZ.add(DataXYZ.fromAtomData((IAtomData)subAtomData));
        }
        return new DumpXYZ(rDumpXYZ);
    }
    /**
     * 对于 matlab 调用的兼容方法
     * @see #fromAtomDataList(Collection, String...)
     */
    public static DumpXYZ fromAtomData_compat(Object[] aAtomDataArray, String... aSymbols) {
        if (aAtomDataArray==null || aAtomDataArray.length==0) return new DumpXYZ();
        List<DataXYZ> rDumpXYZ = new ArrayList<>();
        for (Object subAtomData : aAtomDataArray) if (subAtomData instanceof IAtomData) {
            rDumpXYZ.add(DataXYZ.fromAtomData((IAtomData)subAtomData, aSymbols));
        }
        return new DumpXYZ(rDumpXYZ);
    }
    /**
     * 创建一个空的多帧 XYZ 数据
     * @return 新创建的空的 {@link DumpXYZ}
     */
    public static DumpXYZ zl() {
        return new DumpXYZ();
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个多帧的 XYZ 数据（内部只有一帧）
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #of(IAtomData, String...)} 来手动指定元素符号信息
     *
     * @param aAtomData 输入的原子数据
     * @return 创建的多帧的 XYZ 数据
     * @see DataXYZ#fromAtomData(IAtomData)
     * @see #of(IAtomData, String...)
     */
    public static DumpXYZ of(IAtomData aAtomData) {
        return fromAtomData(aAtomData);
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个多帧的 XYZ 数据（内部只有一帧）
     *
     * @param aAtomData 输入的原子数据
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see DataXYZ#of(IAtomData, String...)
     * @see #of(IAtomData)
     */
    public static DumpXYZ of(IAtomData aAtomData, String... aSymbols) {
        return fromAtomData(aAtomData, aSymbols);
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #of(IAtomData, String...)
     * @see Collection
     */
    public static DumpXYZ of(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {
        return fromAtomData(aAtomData, aSymbols);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #of(Iterable, String...)} 来手动指定元素符号信息
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Iterable, String...)
     */
    public static DumpXYZ of(Iterable<? extends IAtomData> aAtomDataList) {
        return fromAtomDataList(aAtomDataList);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Iterable)
     */
    public static DumpXYZ of(Iterable<? extends IAtomData> aAtomDataList, String... aSymbols) {
        return fromAtomDataList(aAtomDataList, aSymbols);
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #of(Iterable, String...)
     * @see Collection
     */
    public static DumpXYZ of(Iterable<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aSymbols) {
        return fromAtomDataList(aAtomDataList, aSymbols);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbolsGetter 可选的指定帧对应的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Iterable)
     */
    public static DumpXYZ of(Iterable<? extends IAtomData> aAtomDataList, IListGetter<Object> aSymbolsGetter) {
        return fromAtomDataList(aAtomDataList, aSymbolsGetter);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #of(Collection, String...)} 来手动指定元素符号信息
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Collection, String...)
     */
    public static DumpXYZ of(Collection<? extends IAtomData> aAtomDataList) {
        return fromAtomDataList(aAtomDataList);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Collection)
     */
    public static DumpXYZ of(Collection<? extends IAtomData> aAtomDataList, String... aSymbols) {
        return fromAtomDataList(aAtomDataList, aSymbols);
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #of(Collection, String...)
     * @see Collection
     */
    public static DumpXYZ of(Collection<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aSymbols) {
        return fromAtomDataList(aAtomDataList, aSymbols);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个多帧的 XYZ 数据
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbolsGetter 可选的指定帧对应的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的多帧的 XYZ 数据
     * @see #of(Collection)
     */
    public static DumpXYZ of(Collection<? extends IAtomData> aAtomDataList, IListGetter<Object> aSymbolsGetter) {
        return fromAtomDataList(aAtomDataList, aSymbolsGetter);
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link DumpXYZ} 也能直接输入
     * @see #of(Collection)
     * @see AbstractListWrapper
     */
    public static DumpXYZ of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList) {
        return fromAtomDataList(aAtomDataList.asList());
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link DumpXYZ} 也能直接输入
     * @see #of(Collection, String...)
     * @see AbstractListWrapper
     */
    public static DumpXYZ of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, String... aSymbols) {
        return fromAtomDataList(aAtomDataList.asList(), aSymbols);
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link DumpXYZ} 也能直接输入
     * @see #of(Collection, Collection)
     * @see AbstractListWrapper
     */
    public static DumpXYZ of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, Collection<? extends CharSequence> aSymbols) {
        return fromAtomDataList(aAtomDataList.asList(), aSymbols);
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link DumpXYZ} 也能直接输入
     * @see #of(Collection, IListGetter)
     * @see AbstractListWrapper
     */
    public static DumpXYZ of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, IListGetter<Object> aSymbolsGetter) {
        return fromAtomDataList(aAtomDataList.asList(), aSymbolsGetter);
    }
    /// matlab stuffs
    /**
     * 对于 matlab 调用的兼容方法
     * @see #of(Collection)
     */
    public static DumpXYZ of_compat(Object[] aAtomDataArray) {
        return fromAtomData_compat(aAtomDataArray);
    }
    /**
     * 对于 matlab 调用的兼容方法
     * @see #of(Collection, String...)
     */
    public static DumpXYZ of_compat(Object[] aAtomDataArray, String... aSymbols) {
        return fromAtomData_compat(aAtomDataArray, aSymbols);
    }
    
    /// 文件读写
    /**
     * 从包含多帧的 XYZ 原子数据格式或者扩展的 XYZ 格式文件读取来初始化
     * @author liqa
     * @param aFilePath 多帧的 XYZ 文件路径
     * @return 读取得到的 {@link DumpXYZ} 对象，如果文件不完整的帧会跳过
     * @throws IOException 如果读取失败
     * @see #write(String)
     * @see DataXYZ#read(String)
     */
    public static DumpXYZ read(String aFilePath) throws IOException {
        try (BufferedReader tReader = IO.toReader(aFilePath)) {return read_(tReader);}
    }
    static DumpXYZ read_(BufferedReader aReader) throws IOException {
        List<DataXYZ> rDumpXYZ = new ArrayList<>();
        DataXYZ tDataXYZ;
        while ((tDataXYZ = DataXYZ.read_(aReader)) != null) {
            rDumpXYZ.add(tDataXYZ);
        }
        return new DumpXYZ(rDumpXYZ);
    }
    
    /**
     * 输出成标准的多帧的 XYZ 文件，会根据需要自动选择原始的 XYZ 格式或者扩展的 XYZ 格式
     * @author liqa
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     * @see #read(String)
     * @see DataXYZ#write(String)
     */
    public void write(String aFilePath) throws IOException {
        try (IO.IWriteln tWriteln = IO.toWriteln(aFilePath)) {write_(tWriteln);}
    }
    void write_(IO.IWriteln aWriteln) throws IOException {
        for (DataXYZ tDataXYZ : mList) tDataXYZ.write_(aWriteln);
    }
}
