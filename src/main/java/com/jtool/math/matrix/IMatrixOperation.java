package com.jtool.math.matrix;


import com.jtool.math.operation.IDataOperation;
import com.jtool.math.vector.IVectorGetter;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * 任意的矩阵的运算
 * @author liqa
 * @param <M> 返回矩阵类型
 * @param <V> 返回的向量类型
 */
public interface IMatrixOperation<M extends IMatrixGetter, V extends IVectorGetter> extends IDataOperation<M, IMatrixGetter> {
    /** 矩阵的一些额外的运算 */
    V sumOfCols();
    V sumOfRows();
    V meanOfCols();
    V meanOfRows();
    
    M transpose();
    @VisibleForTesting default M T() {return transpose();}
}
