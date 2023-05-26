package com.jtool.math.vector;

import com.jtool.code.ISetIterator;
import com.jtool.code.UT;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * 一般向量的接口的默认实现，用来方便返回抽象的向量
 * @author liqa
 */
public abstract class AbstractVector<T extends Number> extends AbstractList<T> implements IVector<T> {
    /** Iterator stuffs */
    @Override public Iterator<T> iterator() {
        return new Iterator<T>() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public T next() {
                if (hasNext()) {
                    T tNext = get_(mIdx);
                    ++mIdx;
                    return tNext;
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public ISetIterator<T, Number> setIterator() {
        return new ISetIterator<T, Number>() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(Number e) {
                if (oIdx < 0) throw new IllegalStateException();
                set_(oIdx, e);
            }
            @Override public T next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    return get_(oIdx);
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public Iterator<? extends Number> iteratorOf(final IVectorGetter<? extends Number> aContainer) {
        if (aContainer instanceof IVector) return ((IVector<?>)aContainer).iterator();
        return new Iterator<Number>() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public Number next() {
                if (hasNext()) {
                    Number tNext = aContainer.get(mIdx);
                    ++mIdx;
                    return tNext;
                }
                throw new NoSuchElementException();
            }
        };
    }
    
    
    /** 转为兼容性更好的 double[] */
    @Override public double[] vec() {return UT.Code.toData(this);}
    
    
    /** 批量修改的接口 */
    @Override public void fill(Number aValue) {
        final ISetIterator<T, Number> si = setIterator();
        while (si.hasNext()) {
            si.next();
            si.set(aValue);
        }
    }
    @Override public void fill(double[] aVec) {fillWith(i -> aVec[i]);}
    @Override public void fill(Iterable<? extends Number> aList) {
        final ISetIterator<T, Number> si = setIterator();
        final Iterator<? extends Number> it = aList.iterator();
        while (si.hasNext()) {
            si.next();
            si.set(it.next());
        }
    }
    @Override public void fillWith(IVectorGetter<? extends Number> aVectorGetter) {
        final ISetIterator<T, Number> si = setIterator();
        final Iterator<? extends Number> it = iteratorOf(aVectorGetter);
        while (si.hasNext()) {
            si.next();
            si.set(it.next());
        }
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
    
    
    /** Groovy 的部分，重载一些运算符方便操作 */
    @VisibleForTesting @Override public T call(int aIdx) {return get(aIdx);}
    @VisibleForTesting @Override public T getAt(int aIdx) {return get(aIdx);}
    @VisibleForTesting @Override public void putAt(int aIdx, Number aValue) {setOnly(aIdx, aValue);}
    
    
    
    /** stuff to override */
    public abstract T get_(int aIdx);
    public abstract void set_(int aIdx, Number aValue);
    public abstract T getAndSet_(int aIdx, Number aValue);
    public abstract int size();
}
