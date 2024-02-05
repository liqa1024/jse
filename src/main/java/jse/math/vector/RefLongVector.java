package jse.math.vector;

import static jse.math.vector.AbstractVector.rangeCheck;

/**
 * 一般向量的接口的默认实现，实际返回向量类型为 {@link LongVector}，用来方便实现抽象的向量
 * @author liqa
 */
public abstract class RefLongVector extends AbstractLongVector {
    @Override protected final ILongVector newZeros_(int aSize) {return LongVector.zeros(aSize);}
    
    /** stuff to override */
    public abstract long get(int aIdx);
    public void set(int aIdx, long aValue) {throw new UnsupportedOperationException("set");}
    public long getAndSet(int aIdx, long aValue) {
        rangeCheck(aIdx, size());
        long oValue = get(aIdx);
        set(aIdx, aValue);
        return oValue;
    }
    public abstract int size();
}
