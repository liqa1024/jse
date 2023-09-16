package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import com.jtool.code.collection.AbstractRandomAccessList;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleSupplier;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.functional.IDoubleOperator1;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 通用的向量类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractVector implements IVector {
    /** print */
    @Override public String toString() {
        final StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d-length Vector:", size()));
        rStr.append("\n");
        forEach(v -> rStr.append(toString_(v)));
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IDoubleIterator iterator() {
        return new IDoubleIterator() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = get_(mIdx);
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIterator() {
        return new IDoubleSetIterator() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                set_(oIdx, aValue);
            }
            @Override public double next() {
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
    @Override public List<Double> asList() {
        return new AbstractRandomAccessList<Double>() {
            @Override public Double get(int index) {return AbstractVector.this.get(index);}
            @Override public Double set(int index, Double element) {return getAndSet(index, element);}
            @Override public int size() {return AbstractVector.this.size();}
            @Override public Iterator<Double> iterator() {return AbstractVector.this.iterator().toIterator();}
        };
    }
    
    /** 转为兼容性更好的 double[] */
    @Override public double[] data() {
        final int tSize = size();
        double[] rData = new double[tSize];
        final IDoubleIterator it = iterator();
        for (int i = 0; i < tSize; ++i) rData[i] = it.next();
        return rData;
    }
    
    
    /** 批量修改的接口 */
    @Override public final void fill(double aValue) {operation().fill(aValue);}
    @Override public final void fill(IVector aVector) {operation().fill(aVector);}
    @Override public final void fill(IVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    @Override public final void fill(Iterable<? extends Number> aList) {
        final Iterator<? extends Number> it = aList.iterator();
        assign(() -> it.next().doubleValue());
    }
    @Override public void fill(double[] aData) {
        final IDoubleSetIterator si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(aData[idx]);
            ++idx;
        }
    }
    @Override public final void assign(IDoubleSupplier aSup) {operation().assign(aSup);}
    @Override public final void forEach(IDoubleConsumer1 aCon) {operation().forEach(aCon);}
    
    
    @Override public double get(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return get_(aIdx);
    }
    @Override public double getAndSet(int aIdx, double aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndSet_(aIdx, aValue);
    }
    @Override public void set(int aIdx, double aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        set_(aIdx, aValue);
    }
    
    @Override public void increment_(int aIdx) {
        double tValue = get_(aIdx);
        ++tValue;
        set_(aIdx, tValue);
    }
    @Override public double getAndIncrement_(int aIdx) {
        double tValue = get_(aIdx);
        set_(aIdx, tValue+1);
        return tValue;
    }
    @Override public void decrement_(int aIdx) {
        double tValue = get_(aIdx);
        --tValue;
        set_(aIdx, tValue);
    }
    @Override public double getAndDecrement_(int aIdx) {
        double tValue = get_(aIdx);
        set_(aIdx, tValue-1);
        return tValue;
    }
    @Override public void add_(int aIdx, double aDelta) {
        double tValue = get_(aIdx);
        tValue += aDelta;
        set_(aIdx, tValue);
    }
    @Override public double getAndAdd_(int aIdx, double aDelta) {
        double tValue = get_(aIdx);
        set_(aIdx, tValue+aDelta);
        return tValue;
    }
    @Override public void update_(int aIdx, IDoubleOperator1 aOpt) {
        double tValue = get_(aIdx);
        tValue = aOpt.cal(tValue);
        set_(aIdx, tValue);
    }
    @Override public double getAndUpdate_(int aIdx, IDoubleOperator1 aOpt) {
        double tValue = get_(aIdx);
        set_(aIdx, aOpt.cal(tValue));
        return tValue;
    }
    
    @Override public void increment(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        increment_(aIdx);
    }
    @Override public double getAndIncrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndIncrement_(aIdx);
    }
    @Override public void decrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        decrement_(aIdx);
    }
    @Override public double getAndDecrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndDecrement_(aIdx);
    }
    @Override public void add(int aIdx, double aDelta) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        add_(aIdx, aDelta);
    }
    @Override public double getAndAdd(int aIdx, double aDelta) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndAdd_(aIdx, aDelta);
    }
    @Override public void update(int aIdx, IDoubleOperator1 aOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        update_(aIdx, aOpt);
    }
    @Override public double getAndUpdate(int aIdx, IDoubleOperator1 aOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndUpdate_(aIdx, aOpt);
    }
    
    
    
    @Override public final IVector copy() {
        IVector rVector = newZeros();
        rVector.fill(this);
        return rVector;
    }
    
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    @Override public IVectorSlicer slicer() {
        return new AbstractVectorSlicer() {
            @Override protected IVector getL(final List<Integer> aIndices) {IVector rVector = newZeros(aIndices.size()); rVector.fill(refSlicer().get(aIndices)); return rVector;}
            @Override protected IVector getA() {return copy();}
            
            @Override protected int thisSize_() {return size();}
        };
    }
    @Override public IVectorSlicer refSlicer() {
        return new AbstractVectorSlicer() {
            @Override protected IVector getL(final List<Integer> aIndices) {
                return new RefVector() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aIdx) {return AbstractVector.this.get(aIndices.get(aIdx));}
                    @Override public void set_(int aIdx, double aValue) {AbstractVector.this.set(aIndices.get(aIdx), aValue);}
                    @Override public double getAndSet_(int aIdx, double aValue) {return AbstractVector.this.getAndSet(aIndices.get(aIdx), aValue);}
                    @Override public int size() {return aIndices.size();}
                };
            }
            @Override protected IVector getA() {
                return new RefVector() {
                    /** 对于全部切片，则不再需要二次边界检查 */
                    @Override public double get_(int aIdx) {return AbstractVector.this.get_(aIdx);}
                    @Override public void set_(int aIdx, double aValue) {AbstractVector.this.set_(aIdx, aValue);}
                    @Override public double getAndSet_(int aIdx, double aValue) {return AbstractVector.this.getAndSet_(aIdx, aValue);}
                    @Override public int size() {return AbstractVector.this.size();}
                };
            }
            
            @Override protected int thisSize_() {return size();}
        };
    }
    
    
    /** 向量的运算器 */
    @Override public IVectorOperation operation() {
        return new AbstractVectorOperation() {
            @Override protected IVector thisVector_() {return AbstractVector.this;}
            @Override protected IVector newVector_(int aSize) {return newZeros(aSize);}
        };
    }
    
    
    /** Groovy 的部分，增加向量基本的运算操作 */
    @Override public final IVector plus         (double aRHS) {return operation().plus    (aRHS);}
    @Override public final IVector minus        (double aRHS) {return operation().minus   (aRHS);}
    @Override public final IVector multiply     (double aRHS) {return operation().multiply(aRHS);}
    @Override public final IVector div          (double aRHS) {return operation().div     (aRHS);}
    @Override public final IVector mod          (double aRHS) {return operation().mod     (aRHS);}
    
    @Override public final IVector plus         (IVector aRHS) {return operation().plus    (aRHS);}
    @Override public final IVector minus        (IVector aRHS) {return operation().minus   (aRHS);}
    @Override public final IVector multiply     (IVector aRHS) {return operation().multiply(aRHS);}
    @Override public final IVector div          (IVector aRHS) {return operation().div     (aRHS);}
    @Override public final IVector mod          (IVector aRHS) {return operation().mod     (aRHS);}
    
    @Override public final void plus2this       (double aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (double aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (double aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (double aRHS) {operation().div2this     (aRHS);}
    @Override public final void mod2this        (double aRHS) {operation().mod2this     (aRHS);}
    
    @Override public final void plus2this       (IVector aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (IVector aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (IVector aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (IVector aRHS) {operation().div2this     (aRHS);}
    @Override public final void mod2this        (IVector aRHS) {operation().mod2this     (aRHS);}
    
    @Override public final double sum   () {return operation().sum  ();}
    @Override public final double mean  () {return operation().mean ();}
    @Override public final double prod  () {return operation().prod ();}
    @Override public final double max   () {return operation().max  ();}
    @Override public final double min   () {return operation().min  ();}
    
    @Override public final ILogicalVector equal         (IVector aRHS) {return operation().equal         (aRHS);}
    @Override public final ILogicalVector greater       (IVector aRHS) {return operation().greater       (aRHS);}
    @Override public final ILogicalVector greaterOrEqual(IVector aRHS) {return operation().greaterOrEqual(aRHS);}
    @Override public final ILogicalVector less          (IVector aRHS) {return operation().less          (aRHS);}
    @Override public final ILogicalVector lessOrEqual   (IVector aRHS) {return operation().lessOrEqual   (aRHS);}
    
    @Override public final ILogicalVector equal         (double aRHS) {return operation().equal         (aRHS);}
    @Override public final ILogicalVector greater       (double aRHS) {return operation().greater       (aRHS);}
    @Override public final ILogicalVector greaterOrEqual(double aRHS) {return operation().greaterOrEqual(aRHS);}
    @Override public final ILogicalVector less          (double aRHS) {return operation().less          (aRHS);}
    @Override public final ILogicalVector lessOrEqual   (double aRHS) {return operation().lessOrEqual   (aRHS);}
    
    /** Groovy 的部分，增加矩阵切片操作 */
    @VisibleForTesting @Override public double call(int aIdx) {return get(aIdx);}
    @VisibleForTesting @Override public IVector call(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IVector call(SliceType     aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public IVector getAt(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IVector getAt(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IVector getAt(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, double aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, IVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, double aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, IVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, double aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, IVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public double getAt(int aIdx) {return get((aIdx < 0) ? (size()+aIdx) : aIdx);}
    @VisibleForTesting @Override public void putAt(int aIdx, double aValue) {set((aIdx < 0) ? (size()+aIdx) : aIdx, aValue);}
    
    
    /** stuff to override */
    public abstract double get_(int aIdx);
    public abstract void set_(int aIdx, double aValue);
    public abstract double getAndSet_(int aIdx, double aValue);
    public abstract int size();
    public abstract IVector newZeros(int aSize);
    
    protected String toString_(double aValue) {return String.format("   %.4g", aValue);}
}
