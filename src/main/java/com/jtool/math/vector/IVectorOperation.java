package com.jtool.math.vector;


import com.jtool.math.operation.IDataOperation;

/**
 * 任意的矩阵的运算
 * @author liqa
 * @param <V> 返回向量类型
 * @param <T> 自身向量的元素类型
 */
public interface IVectorOperation<V extends IVectorGetter<? extends Number>, T extends Number> extends IDataOperation<V, IVectorGetter<? extends Number>, T, Number> {
    /**/
}
