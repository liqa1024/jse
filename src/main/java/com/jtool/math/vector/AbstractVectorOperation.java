package com.jtool.math.vector;

import com.jtool.math.operation.AbstractDataOperation;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 */
public abstract class AbstractVectorOperation<V extends IVectorAny<?>, VS extends IVectorAny<V>> extends AbstractDataOperation<IVectorAny<?>, V, VS, IVectorGetter> implements IDefaultVectorOperation<V, VS> {
    /**/
}
