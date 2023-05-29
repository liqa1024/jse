package com.jtool.math.vector;

import com.jtool.code.ISetIterator;

import java.util.Iterator;

/**
 * 向量生成器的一般实现，主要实现一些重复的接口
 */
public abstract class AbstractVectorGenerator<V extends IVectorFull<?>> implements IVectorGenerator<V> {
    @Override public V ones() {return ones(thisSize_());}
    @Override public V zeros() {return zeros(thisSize_());}
    @Override public V from(IVectorGetter aVectorGetter) {return from(thisSize_(), aVectorGetter);}
    @Override public V same() {
        V rVector = zeros();
        final ISetIterator<Double> si = rVector.setIterator();
        final Iterator<Double> it = thisIterator_();
        while (si.hasNext()) si.nextAndSet(it.next());
        return rVector;
    }
    
    @Override public V ones(int aSize) {
        V rVector = zeros(aSize);
        rVector.fill(1);
        return rVector;
    }
    @Override public V from(int aSize, IVectorGetter aVectorGetter) {
        V rVector = zeros(aSize);
        rVector.fill(aVectorGetter);
        return rVector;
    }
    
    
    
    @Override public V sequence(double aStart, double aEnd) {
        int tSize = thisSize_();
        return sequence_(aStart, (aEnd-aStart)/(tSize-1), tSize);
    }
    @Override public V sequence(double aStart, double aStep, double aEnd) {
        int tSize = (int)Math.floor((aEnd-aStart)/aStep) + 1;
        return sequence_(aStart, aStep, tSize);
    }
    
    
    @Override public V sequence_(double aStart, double aStep, int aN) {
        final V rVector = zeros(aN);
        final ISetIterator<Double> si = rVector.setIterator();
        double tValue = aStart;
        while (si.hasNext()) {
            si.nextAndSet(tValue);
            tValue += aStep;
        }
        return rVector;
    }
    
    
    /** stuff to override */
    protected abstract Iterator<Double> thisIterator_();
    protected abstract int thisSize_();
    public abstract V zeros(int aSize);
}
