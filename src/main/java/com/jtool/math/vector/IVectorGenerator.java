package com.jtool.math.vector;

import com.jtool.math.IDataGenerator;


/**
 * 通用的任意向量的生成器
 * @author liqa
 * @param <V> 生成的向量类型
 */
public interface IVectorGenerator<V extends IVectorGetter> extends IDataGenerator<V> {
    V from(IVectorGetter aVectorGetter);
    
    V ones(int aSize);
    V zeros(int aSize);
    V from(int aSize, IVectorGetter aVectorGetter);
    
    /** 增加一些 Vector 特有的生成 */
    V sequence(double aStart, double aEnd);
    V sequence(double aStart, double aStep, double aEnd);
    V sequenceByStep(double aStart, double aStep);
    V sequenceByStep(double aStart, double aStep, int aN);
}
