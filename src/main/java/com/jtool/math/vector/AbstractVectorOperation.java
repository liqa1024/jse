package com.jtool.math.vector;

import com.jtool.math.operation.AbstractDataOperation;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 */
public abstract class AbstractVectorOperation<V extends IVectorFull<?>, VS extends IVectorFull<V>> extends AbstractDataOperation<IVectorFull<?>, V, VS, IVectorGetter> implements IDefaultVectorOperation<V, VS> {
    /**/
}
