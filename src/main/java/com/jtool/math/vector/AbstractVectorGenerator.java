package com.jtool.math.vector;

import com.jtool.code.ISetIterator;

import java.util.Iterator;

/**
 * 向量生成器的一般实现，主要实现一些重复的接口
 */
public abstract class AbstractVectorGenerator<T extends Number, V extends IVector<T>> implements IVectorGenerator<V> {
    @Override public V ones() {return ones(thisSize_());}
    @Override public V zeros() {return zeros(thisSize_());}
    @Override public V from(IVectorGetter<? extends Number> aVectorGetter) {return from(thisSize_(), aVectorGetter);}
    @Override public V same() {
        V rVector = zeros();
        final ISetIterator<T, Number> si = rVector.setIterator();
        final Iterator<T> it = thisIterator_();
        while (si.hasNext()) {
            si.next();
            si.set(it.next());
        }
        return rVector;
    }
    
    @Override public V ones(int aSize) {
        V rVector = zeros(aSize);
        rVector.fill(1);
        return rVector;
    }
    @Override public V from(int aSize, IVectorGetter<? extends Number> aVectorGetter) {
        V rVector = zeros(aSize);
        rVector.fillWith(aVectorGetter);
        return rVector;
    }
    
    
    /** stuff to override */
    protected abstract Iterator<T> thisIterator_();
    protected abstract int thisSize_();
    public abstract V zeros(int aSize);
}
