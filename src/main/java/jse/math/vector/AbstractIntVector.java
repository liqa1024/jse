package jse.math.vector;

import jse.math.SliceType;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.ISlice;
import jse.code.functional.IIndexFilter;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import jse.cache.IntVectorCache;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import static jse.math.vector.AbstractVector.*;

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
            @Override public IDoubleIterator iterator() {return AbstractIntVector.this.iterator().asDouble();}
        };
    }
    
    /** 转为兼容性更好的 int[] */
    @Override public int[] data() {
        final int tSize = size();
        int[] rData = new int[tSize];
        final IIntIterator it = iterator();
        for (int i = 0; i < tSize; ++i) rData[i] = it.next();
        return rData;
    }
    @Override public IntVector toBuf(boolean aAbort) {
        IntVector rBuf = IntVectorCache.getVec(size());
        if (aAbort) return rBuf;
        rBuf.fill(this);
        return rBuf;
    }
    @Override public void releaseBuf(@NotNull IIntVector aBuf, boolean aAbort) {
        if (!aAbort) fill(aBuf);
        IntVectorCache.returnVec(aBuf);
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
    @Override public final void fill(Iterable<? extends Number> aList) {
        final Iterator<? extends Number> it = aList.iterator();
        assign(() -> it.next().intValue());
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
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    @Override public IIntVectorSlicer slicer() {
        return new AbstractIntVectorSlicer() {
            @Override protected IIntVector getL(final ISlice aIndices) {IIntVector rVector = newZeros_(aIndices.size()); rVector.fill(refSlicer().get(aIndices)); return rVector;}
            @Override protected IIntVector getA() {return copy();}
            
            @Override protected int thisSize_() {return size();}
        };
    }
    @Override public IIntVectorSlicer refSlicer() {
        return new AbstractIntVectorSlicer() {
            @Override protected IIntVector getL(final ISlice aIndices) {
                return new RefIntVector() {
                    @Override public int get(int aIdx) {return AbstractIntVector.this.get(aIndices.get(aIdx));}
                    @Override public void set(int aIdx, int aValue) {AbstractIntVector.this.set(aIndices.get(aIdx), aValue);}
                    @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntVector.this.getAndSet(aIndices.get(aIdx), aValue);}
                    @Override public int size() {return aIndices.size();}
                };
            }
            @Override protected IIntVector getA() {
                return new RefIntVector() {
                    @Override public int get(int aIdx) {return AbstractIntVector.this.get(aIdx);}
                    @Override public void set(int aIdx, int aValue) {AbstractIntVector.this.set(aIdx, aValue);}
                    @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntVector.this.getAndSet(aIdx, aValue);}
                    @Override public int size() {return AbstractIntVector.this.size();}
                };
            }
            
            @Override protected int thisSize_() {return size();}
        };
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
            @Override protected IIntVector newVector_(int aSize) {return newZeros_(aSize);}
        };
    }
    
    /** 向量基本的运算操作 */
    @Override public final IIntVector plus         (int aRHS) {return operation().plus    (aRHS);}
    @Override public final IIntVector minus        (int aRHS) {return operation().minus   (aRHS);}
    @Override public final IIntVector multiply     (int aRHS) {return operation().multiply(aRHS);}
    @Override public final IIntVector div          (int aRHS) {return operation().div     (aRHS);}
    @Override public final IIntVector mod          (int aRHS) {return operation().mod     (aRHS);}
    
    @Override public final IIntVector plus         (IIntVector aRHS) {return operation().plus    (aRHS);}
    @Override public final IIntVector minus        (IIntVector aRHS) {return operation().minus   (aRHS);}
    @Override public final IIntVector multiply     (IIntVector aRHS) {return operation().multiply(aRHS);}
    @Override public final IIntVector div          (IIntVector aRHS) {return operation().div     (aRHS);}
    @Override public final IIntVector mod          (IIntVector aRHS) {return operation().mod     (aRHS);}
    
    @Override public final void plus2this       (int aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (int aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (int aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (int aRHS) {operation().div2this     (aRHS);}
    @Override public final void mod2this        (int aRHS) {operation().mod2this     (aRHS);}
    
    @Override public final void plus2this       (IIntVector aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (IIntVector aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (IIntVector aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (IIntVector aRHS) {operation().div2this     (aRHS);}
    @Override public final void mod2this        (IIntVector aRHS) {operation().mod2this     (aRHS);}
    
    @Override public final IIntVector negative() {return operation().negative();}
    @Override public final void negative2this() {operation().negative2this();}
    
    @Override public final int    sum   () {return operation().sum  ();}
    @Override public final double mean  () {return operation().mean ();}
    @Override public final double prod  () {return operation().prod ();}
    @Override public final int    max   () {return operation().max  ();}
    @Override public final int    min   () {return operation().min  ();}
    
    @Override public final void sort() {operation().sort();}
    @Override public final void shuffle() {operation().shuffle();}
    @Override public final void shuffle(Random aRng) {operation().shuffle(aRng::nextInt);}
    @Override public final void shuffle(IntUnaryOperator aRng) {operation().shuffle(aRng);}
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting @Override public int call(int aIdx) {return get(aIdx);}
    @VisibleForTesting @Override public IIntVector call(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IIntVector call(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IIntVector call(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IIntVector call(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public IIntVector getAt(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IIntVector getAt(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IIntVector getAt(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IIntVector getAt(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, int aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, IIntVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, int aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, IIntVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, int aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, IIntVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, int aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, IIntVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public int getAt(int aIdx) {return get((aIdx < 0) ? (size()+aIdx) : aIdx);}
    @VisibleForTesting @Override public void putAt(int aIdx, int aValue) {set((aIdx < 0) ? (size()+aIdx) : aIdx, aValue);}
    
    
    /** stuff to override */
    public abstract int get(int aIdx);
    public abstract void set(int aIdx, int aValue);
    public abstract int getAndSet(int aIdx, int aValue);
    public abstract int size();
    protected abstract IIntVector newZeros_(int aSize);
    
    protected String toString_(int aValue) {return " "+aValue;}
}
