package jse.math;


import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Array;

/**
 * 任意的通用的数据和外壳的转换，用来方便进行不同数据类型的转换而不发生值拷贝，
 * 也用于运算来直接操作底层数据从而进行优化
 * @author liqa
 */
public interface IDataShell<D> {
    void setInternalData(D aData);
    IDataShell<D> newShell();
    D internalData();
    /** 返回需要使用的 data 长度，因为可能会通过 setData 导致 data 过长 */
    int internalDataSize();
    /** 返回 data 开头需要平移的长度，部分结构支持传入任意数组然后从中间开始访问 */
    default int internalDataShift() {return 0;}
    /** 返回内部数组是否是倒序排列的，告知在进行遍历时最好使用倒序 */
    default boolean isReverse() {return false;}
    
    /** 返回内部数据如果拥有相同种类并且顺序相同，包含了 {@link #isReverse()} 也要相同 */
    @ApiStatus.Internal @Nullable D getIfHasSameOrderData(Object aObj);
    
    static int internalDataShift(Object aObj) {
        if (aObj instanceof IDataShell) return ((IDataShell<?>)aObj).internalDataShift();
        return 0;
    }
    static int internalDataSize(Object aObj) {
        if (aObj instanceof IDataShell) return ((IDataShell<?>)aObj).internalDataSize();
        else if (aObj.getClass().isArray()) return Array.getLength(aObj);
        return 0;
    }
    
    
    /** @deprecated use {@link #setInternalData} */   @Deprecated default void setData2this(D aData) {setInternalData(aData);}
    /** @deprecated use {@link #internalData} */      @Deprecated default D getData() {return internalData();}
    /** @deprecated use {@link #internalDataSize} */  @Deprecated default int dataSize() {return internalDataSize();}
    /** @deprecated use {@link #internalDataShift} */ @Deprecated default int shiftSize() {return internalDataShift();}
    
    /** 快速构造 */
    static <D> IDataShell<D> of(int aSize, D aData) {return new SimpleDataShell<>(aSize, aData);}
}
