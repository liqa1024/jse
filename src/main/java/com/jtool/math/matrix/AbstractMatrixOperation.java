package com.jtool.math.matrix;

import com.jtool.math.operation.AbstractDataOperation;
import com.jtool.math.vector.IVectorAny;

/**
 * 一般的实矩阵运算的实现，默认没有做任何优化
 */
public abstract class AbstractMatrixOperation<M extends IMatrixAny<?, ?>, MS extends IMatrixAny<M, V>, V extends IVectorAny<?>> extends AbstractDataOperation<IMatrixAny<?, ?>, M, MS, IMatrixGetter> implements IDefaultMatrixOperation<M, MS, V> {
    /**/
}
