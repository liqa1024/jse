package jse.vasp;

import jse.atom.IAtomData;
import jse.code.IO;
import jse.code.UT;
import jse.code.collection.AbstractListWrapper;
import jse.code.collection.IListGetter;
import jse.code.collection.NewCollections;
import org.jetbrains.annotations.Range;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.*;

/**
 * 多帧的 {@link POSCAR}，现在支持现代的非共享 box 的
 * xdatcar，并只对旧的共享版本提供兼容读取支持
 * @author liqa
 */
public class XDATCAR extends AbstractListWrapper<POSCAR, IAtomData, POSCAR> {
    public final static String DEFAULT_COMMENT = "VASP_XDATCAR_FROM_JSE";
    XDATCAR() {super(NewCollections.zl());}
    XDATCAR(POSCAR... aData) {super(NewCollections.from(aData));}
    XDATCAR(List<POSCAR> aData) {super(aData);}
    
    /** AbstractListWrapper stuffs */
    @Override protected final POSCAR toInternal_(IAtomData aAtomData) {return POSCAR.fromAtomData(aAtomData);}
    @Override protected final POSCAR toOutput_(POSCAR aPOSCAR) {return aPOSCAR;}
    
    
    /**
     * 添加一帧原子数据，会读取原子数据值并值拷贝
     * @param aAtomData 需要添加的任意的原子数据
     * @return 自身方便链式调用
     * @see IAtomData
     */
    public XDATCAR append(IAtomData aAtomData) {
        return (XDATCAR)super.append(aAtomData);
    }
    /**
     * 添加多帧原子数据，会遍历读取原子数据并值拷贝
     * @param aAtomDataList 需要添加的任意的原子数据组成的列表
     * @return 自身方便链式调用
     * @see Collection
     */
    public XDATCAR appendAll(Collection<? extends IAtomData> aAtomDataList) {
        return (XDATCAR)super.appendAll(aAtomDataList);
    }
    /**
     * 直接添加一个 XDATCAR 文件
     * @param aFilePath XDATCAR 文件路径
     * @return 自身方便链式调用
     * @throws IOException 当读取失败
     * @see #read(String)
     */
    public XDATCAR appendFile(String aFilePath) throws IOException {
        mList.addAll(read(aFilePath).mList);
        return this;
    }
    /// groovy stuffs
    /** @see #append(IAtomData) */
    @Override public XDATCAR leftShift(IAtomData aAtomData) {
        return (XDATCAR)super.leftShift(aAtomData);
    }
    /** @see #appendAll(Collection) */
    @Override public XDATCAR leftShift(Collection<? extends IAtomData> aAtomDataList) {
        return (XDATCAR)super.leftShift(aAtomDataList);
    }
    
    /**
     * 截断 XDATCAR 开头的一部分，主要用于移除未平衡的部分
     * @param aLength 需要截断的帧数
     * @return 自身方便链式调用
     */
    public XDATCAR cutFront(int aLength) {
        if (aLength <= 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        List<POSCAR> oList = mList;
        mList = new ArrayList<>(oList.size() - aLength);
        Iterator<POSCAR> it = oList.listIterator(aLength);
        while (it.hasNext()) mList.add(it.next());
        return this;
    }
    /**
     * 截断 XDATCAR 结尾的一部分
     * @param aLength 需要截断的帧数
     * @return 自身方便链式调用
     */
    public XDATCAR cutBack(int aLength) {
        if (aLength <= 0) return this;
        if (aLength > mList.size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aLength));
        for (int i = 0; i < aLength; ++i) UT.Code.removeLast(mList);
        return this;
    }
    /**
     * 等间距截取 XDATCAR，主要用于针对大 dump 减少内存和计算压力
     * @param aStep 需要选取的间隔
     * @return 自身方便链式调用
     */
    public XDATCAR step(@Range(from=1, to=Integer.MAX_VALUE) int aStep) {
        if (aStep == 1) return this;
        List<POSCAR> oList = mList;
        final int tSize = oList.size();
        mList = new ArrayList<>(tSize / aStep);
        for (int i = 0; i < tSize; i+=aStep) {
            mList.add(oList.get(i));
        }
        return this;
    }
    
    /** 拷贝一份 XDATCAR */
    public XDATCAR copy() {
        List<POSCAR> rData = new ArrayList<>(mList.size());
        for (POSCAR subData : mList) rData.add(subData.copy());
        return new XDATCAR(rData);
    }
    
    /// 创建 XDATCAR
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 XDATCAR 数据（内部只有一帧）
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #fromAtomData(IAtomData, String...)} 来手动指定元素符号信息
     * <p>
     * {@link #of(IAtomData)} 为等价的别名方法
     *
     * @param aAtomData 输入的原子数据
     * @return 创建的 XDATCAR 数据
     * @see POSCAR#fromAtomData(IAtomData)
     * @see #of(IAtomData)
     * @see #fromAtomData(IAtomData, String...)
     */
    public static XDATCAR fromAtomData(IAtomData aAtomData) {
        return new XDATCAR(POSCAR.fromAtomData(aAtomData));
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 XDATCAR 数据（内部只有一帧）
     * <p>
     * {@link #of(IAtomData, String...)} 为等价的别名方法
     *
     * @param aAtomData 输入的原子数据
     * @param aTypeNames 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see POSCAR#fromAtomData(IAtomData, String...)
     * @see #of(IAtomData, String...)
     * @see #fromAtomData(IAtomData)
     */
    public static XDATCAR fromAtomData(IAtomData aAtomData, String... aTypeNames) {
        return new XDATCAR(POSCAR.fromAtomData(aAtomData, aTypeNames));
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #fromAtomDataList(Iterable, String...)} 来手动指定元素符号信息
     * <p>
     * {@link #of(Iterable)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的 XDATCAR 数据
     * @see #of(Iterable)
     * @see #fromAtomDataList(Iterable, String...)
     */
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new XDATCAR();
        List<POSCAR> rXDATCAR = new ArrayList<>();
        for (IAtomData subAtomData : aAtomDataList) {
            rXDATCAR.add(POSCAR.fromAtomData(subAtomData));
        }
        return new XDATCAR(rXDATCAR);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     * <p>
     * {@link #of(Iterable, String...)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aTypeNames 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see #of(Iterable, String...)
     * @see #fromAtomDataList(Iterable)
     */
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, String... aTypeNames) {
        return fromAtomDataList(aAtomDataList, i -> aTypeNames);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     * <p>
     * {@link #of(Iterable, IListGetter)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aTypeNamesGetter 可选的指定帧对应的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see #of(Iterable, IListGetter)
     * @see #fromAtomDataList(Iterable)
     */
    @SuppressWarnings("unchecked")
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, IListGetter<Object> aTypeNamesGetter) {
        if (aAtomDataList == null) return new XDATCAR();
        List<POSCAR> rXDATCAR = new ArrayList<>();
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            Object tTypeNames = aTypeNamesGetter.get(i);
            if (tTypeNames instanceof String[]) {
                rXDATCAR.add(POSCAR.fromAtomData(subAtomData, (String[])tTypeNames));
            } else
            if (tTypeNames instanceof Collection) {
                rXDATCAR.add(POSCAR.fromAtomData(subAtomData, (Collection<? extends CharSequence>)tTypeNames));
            } else {
                rXDATCAR.add(POSCAR.fromAtomData(subAtomData));
            }
            ++i;
        }
        return new XDATCAR(rXDATCAR);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #fromAtomDataList(Collection, String...)} 来手动指定元素符号信息
     * <p>
     * {@link #of(Collection)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的 XDATCAR 数据
     * @see #of(Collection)
     * @see #fromAtomDataList(Collection, String...)
     */
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList) {
        if (aAtomDataList == null) return new XDATCAR();
        List<POSCAR> rXDATCAR = new ArrayList<>(aAtomDataList.size());
        for (IAtomData subAtomData : aAtomDataList) {
            rXDATCAR.add(POSCAR.fromAtomData(subAtomData));
        }
        return new XDATCAR(rXDATCAR);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     * <p>
     * {@link #of(Collection, String...)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aTypeNames 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see #of(Collection, String...)
     * @see #fromAtomDataList(Collection)
     */
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, String... aTypeNames) {
        return fromAtomDataList(aAtomDataList, i -> aTypeNames);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     * <p>
     * {@link #of(Collection, IListGetter)} 为等价的别名方法
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aTypeNamesGetter 可选的指定帧对应的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see #of(Collection, IListGetter)
     * @see #fromAtomDataList(Collection)
     */
    @SuppressWarnings("unchecked")
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, IListGetter<Object> aTypeNamesGetter) {
        if (aAtomDataList == null) return new XDATCAR();
        List<POSCAR> rXDATCAR = new ArrayList<>(aAtomDataList.size());
        int i = 0;
        for (IAtomData subAtomData : aAtomDataList) {
            Object tTypeNames = aTypeNamesGetter.get(i);
            if (tTypeNames instanceof String[]) {
                rXDATCAR.add(POSCAR.fromAtomData(subAtomData, (String[])tTypeNames));
            } else
            if (tTypeNames instanceof Collection) {
                rXDATCAR.add(POSCAR.fromAtomData(subAtomData, (Collection<? extends CharSequence>)tTypeNames));
            } else {
                rXDATCAR.add(POSCAR.fromAtomData(subAtomData));
            }
            ++i;
        }
        return new XDATCAR(rXDATCAR);
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #fromAtomData(IAtomData, String...)
     * @see Collection
     */
    public static XDATCAR fromAtomData(IAtomData aAtomData, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomData(aAtomData, IO.Text.toArray(aTypeNames));
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #fromAtomDataList(Iterable, String...)
     * @see Collection
     */
    public static XDATCAR fromAtomDataList(Iterable<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomDataList(aAtomDataList, IO.Text.toArray(aTypeNames));
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #fromAtomDataList(Collection, String...)
     * @see Collection
     */
    public static XDATCAR fromAtomDataList(Collection<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomDataList(aAtomDataList, IO.Text.toArray(aTypeNames));
    }
    /**
     * 对于 matlab 调用的兼容方法
     * @see #fromAtomDataList(Collection)
     */
    public static XDATCAR fromAtomData_compat(Object[] aAtomDataArray) {
        if (aAtomDataArray==null || aAtomDataArray.length==0) return new XDATCAR();
        List<POSCAR> rXDATCAR = new ArrayList<>();
        for (Object subAtomData : aAtomDataArray) if (subAtomData instanceof IAtomData) {
            rXDATCAR.add(POSCAR.fromAtomData((IAtomData)subAtomData));
        }
        return new XDATCAR(rXDATCAR);
    }
    /**
     * 对于 matlab 调用的兼容方法
     * @see #fromAtomDataList(Collection, String...)
     */
    public static XDATCAR fromAtomData_compat(Object[] aAtomDataArray, String... aTypeNames) {
        if (aAtomDataArray==null || aAtomDataArray.length==0) return new XDATCAR();
        List<POSCAR> rXDATCAR = new ArrayList<>();
        for (Object subAtomData : aAtomDataArray) if (subAtomData instanceof IAtomData) {
            rXDATCAR.add(POSCAR.fromAtomData((IAtomData)subAtomData, aTypeNames));
        }
        return new XDATCAR(rXDATCAR);
    }
    /**
     * 创建一个空的 XDATCAR 数据
     * @return 新创建的空的 {@link XDATCAR}
     */
    public static XDATCAR zl() {
        return new XDATCAR();
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 XDATCAR 数据（内部只有一帧）
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #of(IAtomData, String...)} 来手动指定元素符号信息
     *
     * @param aAtomData 输入的原子数据
     * @return 创建的 XDATCAR 数据
     * @see POSCAR#fromAtomData(IAtomData)
     * @see #of(IAtomData, String...)
     */
    public static XDATCAR of(IAtomData aAtomData) {
        return fromAtomData(aAtomData);
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 XDATCAR 数据（内部只有一帧）
     *
     * @param aAtomData 输入的原子数据
     * @param aTypeNames 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see POSCAR#of(IAtomData, String...)
     * @see #of(IAtomData)
     */
    public static XDATCAR of(IAtomData aAtomData, String... aTypeNames) {
        return fromAtomData(aAtomData, aTypeNames);
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #of(IAtomData, String...)
     * @see Collection
     */
    public static XDATCAR of(IAtomData aAtomData, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomData(aAtomData, aTypeNames);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #of(Iterable, String...)} 来手动指定元素符号信息
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的 XDATCAR 数据
     * @see #of(Iterable, String...)
     */
    public static XDATCAR of(Iterable<? extends IAtomData> aAtomDataList) {
        return fromAtomDataList(aAtomDataList);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aTypeNames 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see #of(Iterable)
     */
    public static XDATCAR of(Iterable<? extends IAtomData> aAtomDataList, String... aTypeNames) {
        return fromAtomDataList(aAtomDataList, aTypeNames);
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #of(Iterable, String...)
     * @see Collection
     */
    public static XDATCAR of(Iterable<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomDataList(aAtomDataList, aTypeNames);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbolsGetter 可选的指定帧对应的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see #of(Iterable)
     */
    public static XDATCAR of(Iterable<? extends IAtomData> aAtomDataList, IListGetter<Object> aSymbolsGetter) {
        return fromAtomDataList(aAtomDataList, aSymbolsGetter);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #of(Collection, String...)} 来手动指定元素符号信息
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @return 创建的 XDATCAR 数据
     * @see #of(Collection, String...)
     */
    public static XDATCAR of(Collection<? extends IAtomData> aAtomDataList) {
        return fromAtomDataList(aAtomDataList);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aTypeNames 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see #of(Collection)
     */
    public static XDATCAR of(Collection<? extends IAtomData> aAtomDataList, String... aTypeNames) {
        return fromAtomDataList(aAtomDataList, aTypeNames);
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #of(Collection, String...)
     * @see Collection
     */
    public static XDATCAR of(Collection<? extends IAtomData> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomDataList(aAtomDataList, aTypeNames);
    }
    /**
     * 通过一般的原子数据 {@link IAtomData} 组成的列表来创建一个 XDATCAR 数据
     *
     * @param aAtomDataList 输入的原子数据组成的列表
     * @param aSymbolsGetter 可选的指定帧对应的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 XDATCAR 数据
     * @see #of(Collection)
     */
    public static XDATCAR of(Collection<? extends IAtomData> aAtomDataList, IListGetter<Object> aSymbolsGetter) {
        return fromAtomDataList(aAtomDataList, aSymbolsGetter);
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link XDATCAR} 也能直接输入
     * @see #of(Collection)
     * @see AbstractListWrapper
     */
    public static XDATCAR of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList) {
        return fromAtomDataList(aAtomDataList.asList());
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link XDATCAR} 也能直接输入
     * @see #of(Collection, String...)
     * @see AbstractListWrapper
     */
    public static XDATCAR of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, String... aTypeNames) {
        return fromAtomDataList(aAtomDataList.asList(), aTypeNames);
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link XDATCAR} 也能直接输入
     * @see #of(Collection, Collection)
     * @see AbstractListWrapper
     */
    public static XDATCAR of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, Collection<? extends CharSequence> aTypeNames) {
        return fromAtomDataList(aAtomDataList.asList(), aTypeNames);
    }
    /**
     * 传入 {@link AbstractListWrapper} 形式的创建，保证
     * {@link XDATCAR} 也能直接输入
     * @see #of(Collection, IListGetter)
     * @see AbstractListWrapper
     */
    public static XDATCAR of(AbstractListWrapper<? extends IAtomData, ?, ?> aAtomDataList, IListGetter<Object> aSymbolsGetter) {
        return fromAtomDataList(aAtomDataList.asList(), aSymbolsGetter);
    }
    /// matlab stuffs
    /**
     * 对于 matlab 调用的兼容方法
     * @see #of(Collection)
     */
    public static XDATCAR of_compat(Object[] aAtomDataArray) {
        return fromAtomData_compat(aAtomDataArray);
    }
    /**
     * 对于 matlab 调用的兼容方法
     * @see #of(Collection, String...)
     */
    public static XDATCAR of_compat(Object[] aAtomDataArray, String... aSymbols) {
        return fromAtomData_compat(aAtomDataArray, aSymbols);
    }
    
    /// 文件读写
    /**
     * 从文件 vasp 输出的 XDATCAR 文件中读取来实现初始化
     * @author liqa
     * @param aFilePath vasp 输出的 XDATCAR 文件路径
     * @return 读取得到的 XDATCAR 对象，如果文件不完整的帧会跳过
     * @throws IOException 如果读取失败
     */
    public static XDATCAR read(String aFilePath) throws IOException {
        try (BufferedReader tReader = IO.toReader(aFilePath)) {return read(tReader);}
    }
    /**
     * 提供使用 {@link BufferedReader} 的流式接口
     * @param aReader 需要的读取流
     * @throws IOException 如果写入文件失败
     */
    public static XDATCAR read(BufferedReader aReader) throws IOException {
        List<POSCAR> rXDATCAR = new ArrayList<>();
        // 针对旧版的共享 box 头的读取兼容
        POSCAR.Header tHeader = new POSCAR.Header();
        POSCAR tPOSCAR;
        while ((tPOSCAR = POSCAR.read_(aReader, tHeader)) != null) {
            rXDATCAR.add(tPOSCAR);
        }
        return new XDATCAR(rXDATCAR);
    }
    
    /**
     * 输出成 vasp 格式的 XDATCAR 文件，可以供 OVITO 等软件读取
     * @author liqa
     * @param aFilePath 需要输出的路径
     * @throws IOException 如果写入文件失败
     */
    public void write(String aFilePath) throws IOException {
        try (IO.IWriteln tWriteln = IO.toWriteln(aFilePath)) {write(tWriteln);}
    }
    /**
     * 提供使用 {@link IO.IWriteln} 的流式接口
     * @param aWriteln 需要写入的流
     * @throws IOException 如果写入文件失败
     */
    public void write(IO.IWriteln aWriteln) throws IOException {
        int tConf = 1;
        for (POSCAR tPOSCAR : mList) {
            tPOSCAR.write_(aWriteln, tConf, DEFAULT_COMMENT);
            ++tConf;
        }
    }
}
