package jtool.math.vector;

import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.iterator.ILongIterator;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.LongUnaryOperator;

public abstract class AbstractLongVector implements ILongVector {
    
    /** Iterator stuffs */
    @Override public ILongIterator iterator() {
        return new ILongIterator() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public long next() {
                if (hasNext()) {
                    long tNext = get_(mIdx);
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    /** 转换为其他类型 */
    @Override public List<Long> asList() {
        return new AbstractRandomAccessList<Long>() {
            @Override public Long get(int index) {return AbstractLongVector.this.get(index);}
            @Override public Long set(int index, Long element) {return getAndSet(index, element);}
            @Override public int size() {return AbstractLongVector.this.size();}
            @Override public @NotNull Iterator<Long> iterator() {return AbstractLongVector.this.iterator().toIterator();}
        };
    }
    @Override public IVector asVec() {
        return new RefVector() {
            @Override public double get(int aIdx) {return AbstractLongVector.this.get_(aIdx);}
            @Override public void set(int aIdx, double aValue) {AbstractLongVector.this.set_(aIdx, (long)aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {return AbstractLongVector.this.getAndSet_(aIdx, (long)aValue);}
            @Override public int size() {return AbstractLongVector.this.size();}
        };
    }
    
    /** ISwapper stuffs */
    @Override public void swap(int aIdx1, int aIdx2) {
        final int tSize = size();
        if (aIdx1<0 || aIdx1>=tSize) throw new IndexOutOfBoundsException(String.format("Index 1: %d", aIdx1));
        if (aIdx2<0 || aIdx2>=tSize) throw new IndexOutOfBoundsException(String.format("Index 2: %d", aIdx1));
        swap_(aIdx1, aIdx2);
    }
    protected void swap_(int aIdx1, int aIdx2) {
        set_(aIdx1, getAndSet_(aIdx2, get_(aIdx1)));
    }
    
    
    @Override public long get(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return get_(aIdx);
    }
    @Override public long getAndSet(int aIdx, long aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndSet_(aIdx, aValue);
    }
    @Override public void set(int aIdx, long aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        set_(aIdx, aValue);
    }
    
    protected void increment_(int aIdx) {
        long tValue = get_(aIdx);
        ++tValue;
        set_(aIdx, tValue);
    }
    protected long getAndIncrement_(int aIdx) {
        long tValue = get_(aIdx);
        set_(aIdx, tValue+1);
        return tValue;
    }
    protected void decrement_(int aIdx) {
        long tValue = get_(aIdx);
        --tValue;
        set_(aIdx, tValue);
    }
    protected long getAndDecrement_(int aIdx) {
        long tValue = get_(aIdx);
        set_(aIdx, tValue-1);
        return tValue;
    }
    protected void add_(int aIdx, long aDelta) {
        long tValue = get_(aIdx);
        tValue += aDelta;
        set_(aIdx, tValue);
    }
    protected long getAndAdd_(int aIdx, long aDelta) {
        long tValue = get_(aIdx);
        set_(aIdx, tValue+aDelta);
        return tValue;
    }
    protected void update_(int aIdx, LongUnaryOperator aOpt) {
        long tValue = get_(aIdx);
        tValue = aOpt.applyAsLong(tValue);
        set_(aIdx, tValue);
    }
    protected long getAndUpdate_(int aIdx, LongUnaryOperator aOpt) {
        long tValue = get_(aIdx);
        set_(aIdx, aOpt.applyAsLong(tValue));
        return tValue;
    }
    
    @Override public void increment(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        increment_(aIdx);
    }
    @Override public long getAndIncrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndIncrement_(aIdx);
    }
    @Override public void decrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        decrement_(aIdx);
    }
    @Override public long getAndDecrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndDecrement_(aIdx);
    }
    @Override public void add(int aIdx, long aDelta) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        add_(aIdx, aDelta);
    }
    @Override public long getAndAdd(int aIdx, long aDelta) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndAdd_(aIdx, aDelta);
    }
    @Override public void update(int aIdx, LongUnaryOperator aOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        update_(aIdx, aOpt);
    }
    @Override public long getAndUpdate(int aIdx, LongUnaryOperator aOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndUpdate_(aIdx, aOpt);
    }
    
    /** 向量的运算器 */
    @Override public ILongVectorOperation operation() {
        return new AbstractLongVectorOperation() {
            @Override protected ILongVector thisVector_() {return AbstractLongVector.this;}
        };
    }
    
    /** 向量基本的运算操作 */
    @Override public final double sum() {return operation().sum();}
    
    /** stuff to override */
    protected abstract long get_(int aIdx);
    protected abstract void set_(int aIdx, long aValue);
    protected abstract long getAndSet_(int aIdx, long aValue);
    public abstract int size();
    
    protected String toString_(long aValue) {return " "+aValue;}
}
