package com.jtool.math.vector;


import com.jtool.math.operation.IDataOperation;

/**
 * 任意的实向量的运算
 * @author liqa
 * @param <V> 返回向量类型
 */
public interface IVectorOperation<V extends IVectorGetter> extends IDataOperation<V, IVectorGetter> {
    /** 向量的一些额外的运算 */
    V reverse();
    IVectorFull<?> refReverse();
}
