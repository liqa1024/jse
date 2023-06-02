package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import com.jtool.code.ISetIterator;
import com.jtool.code.UT;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 通用的向量类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractVectorAny<V extends IVectorAny<?>> implements IVectorAny<V> {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        final Iterator<Double> it = iterator();
        while (it.hasNext()) rStr.append(toString_(it.next()));
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public Iterator<Double> iterator() {
        return new Iterator<Double>() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public Double next() {
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
    @Override public ISetIterator<Double> setIterator() {
        return new ISetIterator<Double>() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(Double e) {
                if (oIdx < 0) throw new IllegalStateException();
                set_(oIdx, e);
            }
            @Override public Double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    return get_(oIdx);
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    set_(oIdx, e);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    double oValue = get_(oIdx);
                    set_(oIdx, e);
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public Iterator<Double> iteratorOf(final IVectorGetter aContainer) {
        if (aContainer instanceof IVectorAny) return ((IVectorAny<?>)aContainer).iterator();
        return new Iterator<Double>() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public Double next() {
                if (hasNext()) {
                    double tNext = aContainer.get(mIdx);
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    
    /** 转为兼容性更好的 double[] */
    @Override public double[] data() {return UT.Code.toData(asList());}
    
    
    /** 批量修改的接口 */
    @Override public final void fill(double aValue) {operation().mapFill2this(aValue);}
    @Override public final void fill(IVectorGetter aVectorGetter) {operation().ebeFill2this(aVectorGetter);}
    
    @Override public void fill(double[] aData) {
        final ISetIterator<Double> si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(aData[idx]);
            ++idx;
        }
    }
    @Override public void fill(Iterable<? extends Number> aList) {
        final ISetIterator<Double> si = setIterator();
        final Iterator<? extends Number> it = aList.iterator();
        while (si.hasNext()) si.nextAndSet(it.next().doubleValue());
    }
    
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
    @Override public double incrementAndGet_(int aIdx) {
        double tValue = get_(aIdx);
        ++tValue;
        set_(aIdx, tValue);
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
    @Override public double decrementAndGet_(int aIdx) {
        double tValue = get_(aIdx);
        --tValue;
        set_(aIdx, tValue);
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
    @Override public double incrementAndGet(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return incrementAndGet_(aIdx);
    }
    @Override public void decrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        decrement_(aIdx);
    }
    @Override public double getAndDecrement(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndDecrement_(aIdx);
    }
    @Override public double decrementAndGet(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return decrementAndGet_(aIdx);
    }
    
    
    /** 向量生成器部分 */
    protected class VectorGenerator extends AbstractVectorGenerator<V> {
        @Override protected Iterator<Double> thisIterator_() {return iterator();}
        @Override protected int thisSize_() {return size();}
        @Override public V zeros(int aSize) {return newZeros(aSize);}
    }
    
    @Override public IVectorGenerator<V> generator() {return new VectorGenerator();}
    
    @Override public final V copy() {return generator().same();}
    
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    @Override public IVectorSlicer<V> slicer() {
        return new AbstractVectorSlicer<V>() {
            @Override protected V getL(final List<Integer> aIndices) {return generator().from(aIndices.size(), i -> AbstractVectorAny.this.get(aIndices.get(i)));}
            @Override protected V getA() {return generator().same();}
            
            @Override protected Iterator<Double> thisIterator_() {return iterator();}
        };
    }
    @Override public IVectorSlicer<IVector> refSlicer() {
        return new AbstractVectorSlicer<IVector>() {
            @Override protected IVector getL(final List<Integer> aIndices) {
                return new AbstractVector() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aIdx) {return AbstractVectorAny.this.get(aIndices.get(aIdx));}
                    @Override public void set_(int aIdx, double aValue) {AbstractVectorAny.this.set(aIndices.get(aIdx), aValue);}
                    @Override public double getAndSet_(int aIdx, double aValue) {return AbstractVectorAny.this.getAndSet(aIndices.get(aIdx), aValue);}
                    @Override public int size() {return aIndices.size();}
                };
            }
            @Override protected IVector getA() {
                return new AbstractVector() {
                    /** 对于全部切片，则不再需要二次边界检查 */
                    @Override public double get_(int aIdx) {return AbstractVectorAny.this.get_(aIdx);}
                    @Override public void set_(int aIdx, double aValue) {AbstractVectorAny.this.set_(aIdx, aValue);}
                    @Override public double getAndSet_(int aIdx, double aValue) {return AbstractVectorAny.this.getAndSet_(aIdx, aValue);}
                    @Override public int size() {return AbstractVectorAny.this.size();}
                };
            }
            
            @Override protected Iterator<Double> thisIterator_() {return iterator();}
        };
    }
    
    
    /** Groovy 的部分，增加向量基本的运算操作，由于不能重载 += 之类的变成向自身操作，因此会充斥着值拷贝，因此不推荐重性能的场景使用 */
    @VisibleForTesting @Override public V plus      (double aRHS) {return operation().mapPlus       (this, aRHS);}
    @VisibleForTesting @Override public V minus     (double aRHS) {return operation().mapMinus      (this, aRHS);}
    @VisibleForTesting @Override public V multiply  (double aRHS) {return operation().mapMultiply   (this, aRHS);}
    @VisibleForTesting @Override public V div       (double aRHS) {return operation().mapDiv        (this, aRHS);}
    @VisibleForTesting @Override public V mod       (double aRHS) {return operation().mapMod        (this, aRHS);}
    
    @VisibleForTesting @Override public V plus      (IVectorGetter aRHS) {return operation().ebePlus      (this, aRHS);}
    @VisibleForTesting @Override public V minus     (IVectorGetter aRHS) {return operation().ebeMinus     (this, aRHS);}
    @VisibleForTesting @Override public V multiply  (IVectorGetter aRHS) {return operation().ebeMultiply  (this, aRHS);}
    @VisibleForTesting @Override public V div       (IVectorGetter aRHS) {return operation().ebeDiv       (this, aRHS);}
    @VisibleForTesting @Override public V mod       (IVectorGetter aRHS) {return operation().ebeMod       (this, aRHS);}
    
    /** Groovy 的部分，增加矩阵切片操作 */
    @VisibleForTesting @Override public double call(int aIdx) {return get(aIdx);}
    @VisibleForTesting @Override public V call(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public V call(SliceType     aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public V getAt(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public V getAt(SliceType     aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, double aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, IVectorGetter aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, double aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, IVectorGetter aVector) {refSlicer().get(aIndices).fill(aVector);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public double getAt(int aIdx) {return get((aIdx < 0) ? (size()+aIdx) : aIdx);}
    @VisibleForTesting @Override public void putAt(int aIdx, double aValue) {set((aIdx < 0) ? (size()+aIdx) : aIdx, aValue);}
    
    
    /** stuff to override */
    public abstract double get_(int aIdx);
    public abstract void set_(int aIdx, double aValue);
    public abstract double getAndSet_(int aIdx, double aValue);
    public abstract int size();
    
    public abstract IVectorOperation<V> operation();
    protected abstract V newZeros(int aSize);
    
    protected String toString_(double aValue) {return String.format(" %8.4g", aValue);}
}
