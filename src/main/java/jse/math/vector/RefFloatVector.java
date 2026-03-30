package jse.math.vector;

import static jse.math.vector.AbstractVector.rangeCheck;

/**
 * 一般向量的接口的默认实现，实际返回向量类型为 {@link FloatVector}，用来方便实现抽象的向量
 * @author liqa
 */
public abstract class RefFloatVector extends AbstractFloatVector {
    @Override protected final IFloatVector newZeros_(int aSize) {return FloatVector.zeros(aSize);}
    
    /** stuff to override */
    @Override public abstract float get(int aIdx);
    @Override public void set(int aIdx, float aValue) {throw new UnsupportedOperationException("set");}
    @Override public float getAndSet(int aIdx, float aValue) {
        rangeCheck(aIdx, size());
        float oValue = get(aIdx);
        set(aIdx, aValue);
        return oValue;
    }
    @Override public abstract int size();
}
