package jse.math.vector;

import static jse.math.vector.AbstractVector.rangeCheck;

/**
 * 一般向量的接口的默认实现，实际返回向量类型为 {@link IntVector}，用来方便实现抽象的向量
 * @author liqa
 */
public abstract class RefIntVector extends AbstractIntVector {
    @Override protected final IIntVector newZeros_(int aSize) {return IntVector.zeros(aSize);}
    
    /** stuff to override */
    @Override public abstract int get(int aIdx);
    @Override public void set(int aIdx, int aValue) {throw new UnsupportedOperationException("set");}
    @Override public int getAndSet(int aIdx, int aValue) {
        rangeCheck(aIdx, size());
        int oValue = get(aIdx);
        set(aIdx, aValue);
        return oValue;
    }
    @Override public abstract int size();
}
