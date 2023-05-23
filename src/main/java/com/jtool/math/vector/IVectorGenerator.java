package com.jtool.math.vector;

import com.jtool.math.IDataGenerator;

import java.util.concurrent.Callable;

/**
 * 通用的任意向量的生成器
 * @author liqa
 * @param <V> 生成的向量类型
 */
public interface IVectorGenerator<V> extends IDataGenerator<V> {
    V from(IVectorGetter<? extends Number> aVectorGetter);
    
    V ones(int aSize);
    V zeros(int aSize);
    V from(int aSize, Callable<? extends Number> aCall);
    V from(int aSize, IVectorGetter<? extends Number> aVectorGetter);
}
