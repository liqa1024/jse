package com.jtool.math;


/**
 * 任意的通用的数据和外壳的转换，用来方便进行不同数据类型的转换而不发生值拷贝，
 * 也用于运算来直接操作底层数据从而进行优化
 * @author liqa
 */
public interface IDataShell<T, D> {
    void setData2this(D aData);
    T newShell();
    D getData();
    D getIfHasSameOrderData(Object aObj);
    /** 返回需要使用的 data 长度，因为可能会通过 setData 导致 data 过长 */
    int dataSize();
    /** 返回 data 开头需要平移的长度，部分结构支持传入任意数组然后从中间开始访问 */
    default int shiftSize() {return 0;}
    
    static int shiftSize(Object aObj) {
        if (aObj instanceof IDataShell) return ((IDataShell<?, ?>)aObj).shiftSize();
        return 0;
    }
}
