package com.jtool.math.matrix;

import com.jtool.math.IDataGenerator;

import java.util.concurrent.Callable;

/**
 * 通用的任意矩阵的生成器
 * @author liqa
 * @param <M> 生成的矩阵类型
 */
public interface IMatrixGenerator<M> extends IDataGenerator<M> {
    M from(IMatrixGetter<? extends Number> aMatrixGetter);
    
    M ones(int aRowNum, int aColNum);
    M zeros(int aRowNum, int aColNum);
    M from(int aRowNum, int aColNum, Callable<? extends Number> aCall);
    M from(int aRowNum, int aColNum, IMatrixGetter<? extends Number> aMatrixGetter);
}
