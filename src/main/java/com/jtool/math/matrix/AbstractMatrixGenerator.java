package com.jtool.math.matrix;

import com.jtool.code.ISetIterator;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGenerator;
import com.jtool.math.vector.IVectorGetter;

import java.util.Iterator;

/**
 * 矩阵生成器的一般实现，主要实现一些重复的接口
 */
public abstract class AbstractMatrixGenerator<T extends Number, M extends IMatrix<T>> implements IMatrixGenerator<M> {
    @Override public M ones() {return ones(thisRowNumber_(), thisColumnNumber_());}
    @Override public M zeros() {return zeros(thisRowNumber_(), thisColumnNumber_());}
    @Override public M from(IMatrixGetter<? extends Number> aMatrixGetter) {return from(thisRowNumber_(), thisColumnNumber_(), aMatrixGetter);}
    
    @Override public M same() {
        M rMatrix = zeros();
        final ISetIterator<T, Number> si = rMatrix.setIterator();
        final Iterator<T> it = thisIterator_();
        while (si.hasNext()) {
            si.next();
            si.set(it.next());
        }
        return rMatrix;
    }
    @Override public M ones(int aRowNum, int aColNum) {
        M rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fill(1);
        return rMatrix;
    }
    @Override public M from(int aRowNum, int aColNum, IMatrixGetter<? extends Number> aMatrixGetter) {
        M rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fillWith(aMatrixGetter);
        return rMatrix;
    }
    
    /** stuff to override */
    protected abstract Iterator<T> thisIterator_();
    protected abstract int thisRowNumber_();
    protected abstract int thisColumnNumber_();
    public abstract M zeros(int aRowNum, int aColNum);
}
