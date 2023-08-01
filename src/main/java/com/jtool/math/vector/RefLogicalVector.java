package com.jtool.math.vector;

/**
 * 一般向量的接口的默认实现，实际返回向量类型为 {@link LogicalVector}，用来方便实现抽象的向量
 * @author liqa
 */
public abstract class RefLogicalVector extends AbstractLogicalVector {
    @Override public final ILogicalVector newZeros(int aSize) {return LogicalVector.zeros(aSize);}
    
    /** stuff to override */
    public abstract boolean get_(int aIdx);
    @Override public void set_(int aIdx, boolean aValue) {throw new UnsupportedOperationException("set");}
    @Override public boolean getAndSet_(int aIdx, boolean aValue) {
        boolean oValue = get_(aIdx);
        set_(aIdx, aValue);
        return oValue;
    }
    public abstract int size();
}
