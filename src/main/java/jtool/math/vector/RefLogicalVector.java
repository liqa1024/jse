package jtool.math.vector;

import static jtool.math.vector.AbstractVector.rangeCheck;

/**
 * 一般向量的接口的默认实现，实际返回向量类型为 {@link LogicalVector}，用来方便实现抽象的向量
 * @author liqa
 */
public abstract class RefLogicalVector extends AbstractLogicalVector {
    @Override protected final ILogicalVector newZeros_(int aSize) {return LogicalVector.zeros(aSize);}
    
    /** stuff to override */
    public abstract boolean get(int aIdx);
    public void set(int aIdx, boolean aValue) {throw new UnsupportedOperationException("set");}
    public boolean getAndSet(int aIdx, boolean aValue) {
        rangeCheck(aIdx, size());
        boolean oValue = get(aIdx);
        set(aIdx, aValue);
        return oValue;
    }
    public abstract int size();
}
