package jtool.math.vector;

import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.iterator.IIntIterator;
import jtool.code.iterator.IIntSetIterator;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import static jtool.math.vector.AbstractVector.*;

public abstract class AbstractIntVector implements IIntVector {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d-length Integer Vector:", size()));
        rStr.append("\n");
        forEach(v -> rStr.append(toString_(v)));
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IIntIterator iterator() {
        return new IIntIterator() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = get(mIdx);
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntSetIterator setIterator() {
        return new IIntSetIterator() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractIntVector.this.set(oIdx, aValue);
            }
            @Override public int next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    return get(oIdx);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    /** 转换为其他类型 */
    @Override public List<Integer> asList() {
        return new AbstractRandomAccessList<Integer>() {
            @Override public Integer get(int index) {return AbstractIntVector.this.get(index);}
            @Override public Integer set(int index, Integer element) {return getAndSet(index, element);}
            @Override public int size() {return AbstractIntVector.this.size();}
            @Override public @NotNull Iterator<Integer> iterator() {return AbstractIntVector.this.iterator().toIterator();}
        };
    }
    @Override public IVector asVec() {
        return new RefVector() {
            @Override public double get(int aIdx) {return AbstractIntVector.this.get(aIdx);}
            @Override public void set(int aIdx, double aValue) {AbstractIntVector.this.set(aIdx, (int)aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {return AbstractIntVector.this.getAndSet(aIdx, (int)aValue);}
            @Override public int size() {return AbstractIntVector.this.size();}
        };
    }
    
    /** ISwapper stuffs */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, size());
        set(aIdx1, getAndSet(aIdx2, get(aIdx1)));
    }
    
    /** 批量修改的接口 */
    @Override public final void fill(int aValue) {operation().fill(aValue);}
    @Override public final void fill(IIntVector aVector) {operation().fill(aVector);}
    @Override public final void fill(IIntVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    @Override public final void fill(Iterable<Integer> aList) {
        final Iterator<Integer> it = aList.iterator();
        assign(it::next);
    }
    @Override public void fill(int[] aData) {
        final IIntSetIterator si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(aData[idx]);
            ++idx;
        }
    }
    @Override public final void assign(IntSupplier aSup) {operation().assign(aSup);}
    @Override public final void forEach(IntConsumer aCon) {operation().forEach(aCon);}
    
    
    @Override public void increment(int aIdx) {
        rangeCheck(aIdx, size());
        int tValue = get(aIdx);
        ++tValue;
        set(aIdx, tValue);
    }
    @Override public int getAndIncrement(int aIdx) {
        rangeCheck(aIdx, size());
        int tValue = get(aIdx);
        set(aIdx, tValue+1);
        return tValue;
    }
    @Override public void decrement(int aIdx) {
        rangeCheck(aIdx, size());
        int tValue = get(aIdx);
        --tValue;
        set(aIdx, tValue);
    }
    @Override public int getAndDecrement(int aIdx) {
        rangeCheck(aIdx, size());
        int tValue = get(aIdx);
        set(aIdx, tValue-1);
        return tValue;
    }
    @Override public void add(int aIdx, int aDelta) {
        rangeCheck(aIdx, size());
        int tValue = get(aIdx);
        tValue += aDelta;
        set(aIdx, tValue);
    }
    @Override public int getAndAdd(int aIdx, int aDelta) {
        rangeCheck(aIdx, size());
        int tValue = get(aIdx);
        set(aIdx, tValue+aDelta);
        return tValue;
    }
    @Override public void update(int aIdx, IntUnaryOperator aOpt) {
        rangeCheck(aIdx, size());
        int tValue = get(aIdx);
        tValue = aOpt.applyAsInt(tValue);
        set(aIdx, tValue);
    }
    @Override public int getAndUpdate(int aIdx, IntUnaryOperator aOpt) {
        rangeCheck(aIdx, size());
        int tValue = get(aIdx);
        set(aIdx, aOpt.applyAsInt(tValue));
        return tValue;
    }
    
    
    @Override public IIntVector copy() {
        IIntVector rVector = newZeros_(size());
        rVector.fill(this);
        return rVector;
    }
    
    @Override public IIntVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, size());
        return new RefIntVector() {
            /** 由于一开始有边界检查，所以这里不再需要边检检查 */
            @Override public int get(int aIdx) {rangeCheck(aIdx, size()); return AbstractIntVector.this.get(aIdx+aFromIdx);}
            @Override public void set(int aIdx, int aValue) {rangeCheck(aIdx, size()); AbstractIntVector.this.set(aIdx+aFromIdx, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {rangeCheck(aIdx, size()); return AbstractIntVector.this.getAndSet(aIdx+aFromIdx, aValue);}
            @Override public int size() {return aToIdx-aFromIdx;}
        };
    }
    
    /** 向量的运算器 */
    @Override public IIntVectorOperation operation() {
        return new AbstractIntVectorOperation() {
            @Override protected IIntVector thisVector_() {return AbstractIntVector.this;}
        };
    }
    
    /** 向量基本的运算操作 */
    @Override public final double sum() {return operation().sum();}
    
    @Override public final void sort() {operation().sort();}
    @Override public final void shuffle() {operation().shuffle();}
    @Override public final void shuffle(Random aRng) {operation().shuffle(aRng::nextInt);}
    @Override public final void shuffle(IntUnaryOperator aRng) {operation().shuffle(aRng);}
    
    
    /** stuff to override */
    public abstract int get(int aIdx);
    public abstract void set(int aIdx, int aValue);
    public abstract int getAndSet(int aIdx, int aValue);
    public abstract int size();
    protected abstract IIntVector newZeros_(int aSize);
    
    protected String toString_(int aValue) {return " "+aValue;}
}
