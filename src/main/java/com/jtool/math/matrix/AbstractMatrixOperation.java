package com.jtool.math.matrix;

import com.jtool.math.operation.AbstractDataOperation;
import com.jtool.math.vector.IVectorFull;

/**
 * 一般的实矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractMatrixOperation<M extends IMatrixFull<?, ?>, MS extends IMatrixFull<M, V>, V extends IVectorFull<?>> extends AbstractDataOperation<IMatrixFull<?, ?>, M, MS, IMatrixGetter> implements IDefaultMatrixOperation<M, MS, V> {
    /**/
}
