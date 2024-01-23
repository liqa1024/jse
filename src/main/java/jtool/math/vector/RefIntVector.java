package jtool.math.vector;

import static jtool.math.vector.AbstractVector.rangeCheck;

/**
 * 一般向量的接口的默认实现，实际返回向量类型为 {@link IntVector}，用来方便实现抽象的向量
 * @author liqa
 */
public abstract class RefIntVector extends AbstractIntVector {
    @Override protected final IIntVector newZeros_(int aSize) {return IntVector.zeros(aSize);}
    
    /** stuff to override */
    public abstract int get(int aIdx);
    public void set(int aIdx, int aValue) {throw new UnsupportedOperationException("set");}
    public int getAndSet(int aIdx, int aValue) {
        rangeCheck(aIdx, size());
        int oValue = get(aIdx);
        set(aIdx, aValue);
        return oValue;
    }
    public abstract int size();
}
