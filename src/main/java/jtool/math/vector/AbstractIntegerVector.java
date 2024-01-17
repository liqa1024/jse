package jtool.math.vector;

import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.functional.IIntegerConsumer1;
import jtool.code.functional.IIntegerOperator1;
import jtool.code.functional.IIntegerSupplier;
import jtool.code.iterator.IIntegerIterator;
import jtool.code.iterator.IIntegerSetIterator;
import org.jetbrains.annotations.NotNull;

import java.util.*;

import static jtool.math.vector.AbstractVector.subVecRangeCheck;

public abstract class AbstractIntegerVector implements IIntegerVector {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d-length Integer Vector:", size()));
        rStr.append("\n");
        forEach(v -> rStr.append(toString_(v)));
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IIntegerIterator iterator() {
        return new IIntegerIterator() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = get_(mIdx);
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntegerSetIterator setIterator() {
        return new IIntegerSetIterator() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                set_(oIdx, aValue);
            }
            @Override public int next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    return get_(oIdx);
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
            @Override public Integer get(int index) {return AbstractIntegerVector.this.get(index);}
            @Override public Integer set(int index, Integer element) {return getAndSet(index, element);}
            @Override public int size() {return AbstractIntegerVector.this.size();}
            @Override public @NotNull Iterator<Integer> iterator() {return AbstractIntegerVector.this.iterator().toIterator();}
        };
    }
    @Override public IVector asVec() {
        return new RefVector() {
            @Override public double get_(int aIdx) {return AbstractIntegerVector.this.get_(aIdx);}
            @Override public void set_(int aIdx, double aValue) {AbstractIntegerVector.this.set_(aIdx, (int)aValue);}
            public double getAndSet_(int aIdx, double aValue) {return AbstractIntegerVector.this.getAndSet_(aIdx, (int)aValue);}
            @Override public int size() {return AbstractIntegerVector.this.size();}
        };
    }
    
    /** 批量修改的接口 */
    @Override public final void fill(int aValue) {operation().fill(aValue);}
    @Override public final void fill(IIntegerVector aVector) {operation().fill(aVector);}
    @Override public final void fill(IIntegerVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    @Override public final void fill(Iterable<Integer> aList) {
        final Iterator<Integer> it = aList.iterator();
        assign(it::next);
    }
    @Override public void fill(int[] aData) {
        final IIntegerSetIterator si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(aData[idx]);
            ++idx;
        }
    }
    @Override public final void assign(IIntegerSupplier aSup) {operation().assign(aSup);}
    @Override public final void forEach(IIntegerConsumer1 aCon) {operation().forEach(aCon);}
    
    @Override public int get(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return get_(aIdx);
    }
    @Override public int getAndSet(int aIdx, int aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndSet_(aIdx, aValue);
    }
    @Override public void set(int aIdx, int aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        set_(aIdx, aValue);
    }
    
    @Override public void increment_(int aIdx) {
        int tValue = get_(aIdx);
        ++tValue;
        set_(aIdx, tValue);
    }
    @Override public int getAndIncrement_(int aIdx) {
        int tValue = get_(aIdx);
        set_(aIdx, tValue+1);
        return tValue;
    }
    @Override public void decrement_(int aIdx) {
        int tValue = get_(aIdx);
        --tValue;
        set_(aIdx, tValue);
    }
    @Override public int getAndDecrement_(int aIdx) {
        int tValue = get_(aIdx);
        set_(aIdx, tValue-1);
        return tValue;
    }
    @Override public void add_(int aIdx, int aDelta) {
        int tValue = get_(aIdx);
        tValue += aDelta;
        set_(aIdx, tValue);
    }
    @Override public int getAndAdd_(int aIdx, int aDelta) {
        int tValue = get_(aIdx);
        set_(aIdx, tValue+aDelta);
        return tValue;
    }
    @Override public void update_(int aIdx, IIntegerOperator1 aOpt) {
        int tValue = get_(aIdx);
        tValue = aOpt.cal(tValue);
        set_(aIdx, tValue);
    }
    @Override public int getAndUpdate_(int aIdx, IIntegerOperator1 aOpt) {
        int tValue = get_(aIdx);
        set_(aIdx, aOpt.cal(tValue));
        return tValue;
    }
    
    @Override public void increment(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        increment_(aIdx);
    }
    @Override public int getAndIncrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndIncrement_(aIdx);
    }
    @Override public void decrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        decrement_(aIdx);
    }
    @Override public int getAndDecrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndDecrement_(aIdx);
    }
    @Override public void add(int aIdx, int aDelta) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        add_(aIdx, aDelta);
    }
    @Override public int getAndAdd(int aIdx, int aDelta) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndAdd_(aIdx, aDelta);
    }
    @Override public void update(int aIdx, IIntegerOperator1 aOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        update_(aIdx, aOpt);
    }
    @Override public int getAndUpdate(int aIdx, IIntegerOperator1 aOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndUpdate_(aIdx, aOpt);
    }
    
    @Override public IIntegerVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, size());
        return new RefIntegerVector() {
            /** 由于一开始有边界检查，所以这里不再需要边检检查 */
            @Override public int get_(int aIdx) {return AbstractIntegerVector.this.get_(aIdx+aFromIdx);}
            @Override public void set_(int aIdx, int aValue) {AbstractIntegerVector.this.set_(aIdx+aFromIdx, aValue);}
            @Override public int getAndSet_(int aIdx, int aValue) {return AbstractIntegerVector.this.getAndSet_(aIdx+aFromIdx, aValue);}
            @Override public int size() {return aToIdx-aFromIdx;}
        };
    }
    
    /** 向量的运算器 */
    @Override public IIntegerVectorOperation operation() {
        return new AbstractIntegerVectorOperation() {
            @Override protected IIntegerVector thisVector_() {return AbstractIntegerVector.this;}
        };
    }
    
    /** 向量基本的运算操作 */
    @Override public final void sort() {operation().sort();}
    @Override public final void sort(Comparator<? super Integer> aComp) {operation().sort(aComp);}
    @Override public final void shuffle() {operation().shuffle();}
    @Override public final void shuffle(Random aRng) {operation().shuffle(aRng::nextInt);}
    @Override public final void shuffle(IIntegerOperator1 aRng) {operation().shuffle(aRng);}
    
    
    /** stuff to override */
    public abstract int get_(int aIdx);
    public abstract void set_(int aIdx, int aValue);
    public abstract int getAndSet_(int aIdx, int aValue);
    public abstract int size();
    
    protected String toString_(int aValue) {return " "+aValue;}
}
