package com.jtool.math.vector;

import com.jtool.code.UT;

import java.util.AbstractList;
import java.util.Iterator;

/**
 * 一般向量的接口的默认实现，用来方便返回抽象的向量
 * @author liqa
 */
public abstract class AbstractVector<T extends Number> extends AbstractList<T> implements IVector<T> {
    /** 转为兼容性更好的 double[] */
    @Override public double[] vec() {return UT.Code.toData(this);}
    
    
    /** 批量修改的接口 */
    @Override public void fill(Number aValue) {
        int tSize = size();
        for (int i = 0; i < tSize; ++i) set_(i, aValue);
    }
    @Override public void fillWith(Iterable<? extends Number> aList) {
        int tSize = size();
        Iterator<? extends Number> tIt = aList.iterator();
        int i = 0;
        while (i < tSize && tIt.hasNext()) {
            set_(i, tIt.next());
            ++i;
        }
    }
    @Override public void fillWith(IVectorGetter<? extends Number> aVectorGetter) {
        int tSize = size();
        for (int i = 0; i < tSize; ++i) set_(i, aVectorGetter.get(i));
    }
    
    @Override public T get(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return get_(aIdx);
    }
    @Override public T getAndSet(int aIdx, Number aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndSet_(aIdx, aValue);
    }
    /** List 的 set 在这里是 getAndSet 的逻辑 */
    @Override public T set(int aIdx, Number aValue) {return getAndSet(aIdx, aValue);}
    /** 专门提供一个仅 set 的接口 */
    @Override public void setOnly(int aIdx, Number aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        set(aIdx, aValue);
    }
    
    
    
    /** stuff to override */
    public abstract T get_(int aIdx);
    public abstract void set_(int aIdx, Number aValue);
    public abstract T getAndSet_(int aIdx, Number aValue);
    public abstract int size();
}
