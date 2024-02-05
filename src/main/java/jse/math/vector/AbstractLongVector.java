package jse.math.vector;

import jse.code.CS.SliceType;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.ISlice;
import jse.code.functional.IIndexFilter;
import jse.code.iterator.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.*;

import static jse.math.vector.AbstractVector.*;

public abstract class AbstractLongVector implements ILongVector {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d-length Integer Vector:", size()));
        rStr.append("\n");
        forEach(v -> rStr.append(toString_(v)));
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public ILongIterator iterator() {
        return new ILongIterator() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public long next() {
                if (hasNext()) {
                    long tNext = get(mIdx);
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ILongSetIterator setIterator() {
        return new ILongSetIterator() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(long aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractLongVector.this.set(oIdx, aValue);
            }
            @Override public long next() {
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
            @Override public double get(int aIdx) {return AbstractLongVector.this.get(aIdx);}
            @Override public void set(int aIdx, double aValue) {AbstractLongVector.this.set(aIdx, (long)aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {return AbstractLongVector.this.getAndSet(aIdx, (long)aValue);}
            @Override public int size() {return AbstractLongVector.this.size();}
            @Override public IDoubleIterator iterator() {return AbstractLongVector.this.iterator().asDouble();}
        };
    }
    
    /** 转为兼容性更好的 long[] */
    @Override public long[] data() {
        final int tSize = size();
        long[] rData = new long[tSize];
        final ILongIterator it = iterator();
        for (int i = 0; i < tSize; ++i) rData[i] = it.next();
        return rData;
    }
    
    /** ISwapper stuffs */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, size());
        set(aIdx1, getAndSet(aIdx2, get(aIdx1)));
    }
    
    /** 批量修改的接口 */
    @Override public final void fill(long aValue) {operation().fill(aValue);}
    @Override public final void fill(ILongVector aVector) {operation().fill(aVector);}
    @Override public final void fill(ILongVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    @Override public final void fill(Iterable<? extends Number> aList) {
        final Iterator<? extends Number> it = aList.iterator();
        assign(() -> it.next().intValue());
    }
    @Override public void fill(long[] aData) {
        final ILongSetIterator si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(aData[idx]);
            ++idx;
        }
    }
    @Override public final void assign(LongSupplier aSup) {operation().assign(aSup);}
    @Override public final void forEach(LongConsumer aCon) {operation().forEach(aCon);}
    
    
    @Override public void increment(int aIdx) {
        rangeCheck(aIdx, size());
        long tValue = get(aIdx);
        ++tValue;
        set(aIdx, tValue);
    }
    @Override public long getAndIncrement(int aIdx) {
        rangeCheck(aIdx, size());
        long tValue = get(aIdx);
        set(aIdx, tValue+1);
        return tValue;
    }
    @Override public void decrement(int aIdx) {
        rangeCheck(aIdx, size());
        long tValue = get(aIdx);
        --tValue;
        set(aIdx, tValue);
    }
    @Override public long getAndDecrement(int aIdx) {
        rangeCheck(aIdx, size());
        long tValue = get(aIdx);
        set(aIdx, tValue-1);
        return tValue;
    }
    @Override public void add(int aIdx, long aDelta) {
        rangeCheck(aIdx, size());
        long tValue = get(aIdx);
        tValue += aDelta;
        set(aIdx, tValue);
    }
    @Override public long getAndAdd(int aIdx, long aDelta) {
        rangeCheck(aIdx, size());
        long tValue = get(aIdx);
        set(aIdx, tValue+aDelta);
        return tValue;
    }
    @Override public void update(int aIdx, LongUnaryOperator aOpt) {
        rangeCheck(aIdx, size());
        long tValue = get(aIdx);
        tValue = aOpt.applyAsLong(tValue);
        set(aIdx, tValue);
    }
    @Override public long getAndUpdate(int aIdx, LongUnaryOperator aOpt) {
        rangeCheck(aIdx, size());
        long tValue = get(aIdx);
        set(aIdx, aOpt.applyAsLong(tValue));
        return tValue;
    }
    
    
    @Override public ILongVector copy() {
        ILongVector rVector = newZeros_(size());
        rVector.fill(this);
        return rVector;
    }
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    @Override public ILongVectorSlicer slicer() {
        return new AbstractLongVectorSlicer() {
            @Override protected ILongVector getL(final ISlice aIndices) {ILongVector rVector = newZeros_(aIndices.size()); rVector.fill(refSlicer().get(aIndices)); return rVector;}
            @Override protected ILongVector getA() {return copy();}
            
            @Override protected int thisSize_() {return size();}
        };
    }
    @Override public ILongVectorSlicer refSlicer() {
        return new AbstractLongVectorSlicer() {
            @Override protected ILongVector getL(final ISlice aIndices) {
                return new RefLongVector() {
                    @Override public long get(int aIdx) {return AbstractLongVector.this.get(aIndices.get(aIdx));}
                    @Override public void set(int aIdx, long aValue) {AbstractLongVector.this.set(aIndices.get(aIdx), aValue);}
                    @Override public long getAndSet(int aIdx, long aValue) {return AbstractLongVector.this.getAndSet(aIndices.get(aIdx), aValue);}
                    @Override public int size() {return aIndices.size();}
                };
            }
            @Override protected ILongVector getA() {
                return new RefLongVector() {
                    @Override public long get(int aIdx) {return AbstractLongVector.this.get(aIdx);}
                    @Override public void set(int aIdx, long aValue) {AbstractLongVector.this.set(aIdx, aValue);}
                    @Override public long getAndSet(int aIdx, long aValue) {return AbstractLongVector.this.getAndSet(aIdx, aValue);}
                    @Override public int size() {return AbstractLongVector.this.size();}
                };
            }
            
            @Override protected int thisSize_() {return size();}
        };
    }
    @Override public ILongVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, size());
        return new RefLongVector() {
            /** 由于一开始有边界检查，所以这里不再需要边检检查 */
            @Override public long get(int aIdx) {rangeCheck(aIdx, size()); return AbstractLongVector.this.get(aIdx+aFromIdx);}
            @Override public void set(int aIdx, long aValue) {rangeCheck(aIdx, size()); AbstractLongVector.this.set(aIdx+aFromIdx, aValue);}
            @Override public long getAndSet(int aIdx, long aValue) {rangeCheck(aIdx, size()); return AbstractLongVector.this.getAndSet(aIdx+aFromIdx, aValue);}
            @Override public int size() {return aToIdx-aFromIdx;}
        };
    }
    
    /** 向量的运算器 */
    @Override public ILongVectorOperation operation() {
        return new AbstractLongVectorOperation() {
            @Override protected ILongVector thisVector_() {return AbstractLongVector.this;}
            @Override protected ILongVector newVector_(int aSize) {return newZeros_(aSize);}
        };
    }
    
    /** 向量基本的运算操作 */
    @Override public final long   sum   () {return operation().sum  ();}
    @Override public final double mean  () {return operation().mean ();}
    @Override public final double prod  () {return operation().prod ();}
    @Override public final long   max   () {return operation().max  ();}
    @Override public final long   min   () {return operation().min  ();}
    @Override public final void sort() {operation().sort();}
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting @Override public long call(int aIdx) {return get(aIdx);}
    @VisibleForTesting @Override public ILongVector call(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILongVector call(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILongVector call(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILongVector call(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public ILongVector getAt(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILongVector getAt(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILongVector getAt(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILongVector getAt(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, long aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, ILongVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, long aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, ILongVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, long aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, ILongVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, long aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, ILongVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public long getAt(int aIdx) {return get((aIdx < 0) ? (size()+aIdx) : aIdx);}
    @VisibleForTesting @Override public void putAt(int aIdx, long aValue) {set((aIdx < 0) ? (size()+aIdx) : aIdx, aValue);}
    
    
    /** stuff to override */
    public abstract long get(int aIdx);
    public abstract void set(int aIdx, long aValue);
    public abstract long getAndSet(int aIdx, long aValue);
    public abstract int size();
    protected abstract ILongVector newZeros_(int aSize);
    
    protected String toString_(long aValue) {return " "+aValue;}
}
