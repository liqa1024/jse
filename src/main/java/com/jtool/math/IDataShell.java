package com.jtool.math;


/**
 * 任意的通用的数据和外壳的转换，用来方便进行不同数据类型的转换而不发生值拷贝
 * @author liqa
 */
public interface IDataShell<T, D> {
    T setData(D aData);
    T newShell();
    D data();
}
