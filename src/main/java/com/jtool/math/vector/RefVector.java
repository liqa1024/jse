package com.jtool.math.vector;

/**
 * 一般向量的接口的默认实现，实际返回向量类型为 {@link Vector}，用来方便实现抽象的向量
 * @author liqa
 */
public abstract class RefVector extends AbstractVector {
    @Override protected final IVector newZeros_(int aSize) {return Vector.zeros(aSize);}
    
    /** stuff to override */
    public abstract double get_(int aIdx);
    @Override public void set_(int aIdx, double aValue) {throw new UnsupportedOperationException("set");}
    @Override public double getAndSet_(int aIdx, double aValue) {
        double oValue = get_(aIdx);
        set_(aIdx, aValue);
        return oValue;
    }
    public abstract int size();
}
