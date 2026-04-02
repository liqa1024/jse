package jse.math.vector;

import jep.NDArray;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.ISlice;
import jse.code.functional.IFloatConsumer;
import jse.code.functional.IFloatSupplier;
import jse.code.functional.IFloatUnaryOperator;
import jse.code.functional.IIndexFilter;
import jse.code.iterator.*;
import jse.math.SliceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static jse.math.vector.AbstractVector.*;

public abstract class AbstractFloatVector implements IFloatVector {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d-length Float Vector:", size()));
        rStr.append("\n");
        forEach(v -> rStr.append(toString_(v)));
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IFloatIterator iterator() {
        return new IFloatIterator() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public float next() {
                if (hasNext()) {
                    float tNext = get(mIdx);
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IFloatSetIterator setIterator() {
        return new IFloatSetIterator() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(float aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractFloatVector.this.set(oIdx, aValue);
            }
            @Override public float next() {
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
    @Override public List<Float> asList() {
        return new AbstractRandomAccessList<Float>() {
            @Override public Float get(int index) {return AbstractFloatVector.this.get(index);}
            @Override public Float set(int index, Float element) {return getAndSet(index, element);}
            @Override public int size() {return AbstractFloatVector.this.size();}
            @Override public @NotNull Iterator<Float> iterator() {return AbstractFloatVector.this.iterator().toIterator();}
        };
    }
    @Override public IVector asVec() {
        return new RefVector() {
            @Override public double get(int aIdx) {return AbstractFloatVector.this.get(aIdx);}
            @Override public void set(int aIdx, double aValue) {AbstractFloatVector.this.set(aIdx, (float)aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {return AbstractFloatVector.this.getAndSet(aIdx, (float)aValue);}
            @Override public int size() {return AbstractFloatVector.this.size();}
            @Override public IDoubleIterator iterator() {return AbstractFloatVector.this.iterator().asDouble();}
        };
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NDArray<float[]> numpy() {return new NDArray<>(data(), size());}
    /** {@inheritDoc} */
    @Override public float[] data() {
        final int tSize = size();
        float[] rData = new float[tSize];
        final IFloatIterator it = iterator();
        for (int i = 0; i < tSize; ++i) rData[i] = it.next();
        return rData;
    }
    
    /** ISwapper stuffs */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, size());
        set(aIdx1, getAndSet(aIdx2, get(aIdx1)));
    }
    
    /** 批量修改的接口 */
    @Override public final void fill(float aValue) {operation().fill(aValue);}
    @Override public final void fill(IFloatVector aVector) {operation().fill(aVector);}
    @Override public final void fill(IFloatVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    @Override public final void fill(Iterable<? extends Number> aList) {
        final Iterator<? extends Number> it = aList.iterator();
        assign(() -> it.next().floatValue());
    }
    @Override public void fill(float[] aData) {
        final IFloatSetIterator si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(aData[idx]);
            ++idx;
        }
    }
    @Override public final void assign(IFloatSupplier aSup) {operation().assign(aSup);}
    @Override public final void forEach(IFloatConsumer aCon) {operation().forEach(aCon);}
    
    @Override public void update(int aIdx, IFloatUnaryOperator aOpt) {
        rangeCheck(aIdx, size());
        float tValue = get(aIdx);
        tValue = aOpt.applyAsFloat(tValue);
        set(aIdx, tValue);
    }
    @Override public float getAndUpdate(int aIdx, IFloatUnaryOperator aOpt) {
        rangeCheck(aIdx, size());
        float tValue = get(aIdx);
        set(aIdx, aOpt.applyAsFloat(tValue));
        return tValue;
    }
    
    
    @Override public IFloatVector copy() {
        IFloatVector rVector = newZeros_(size());
        rVector.fill(this);
        return rVector;
    }
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    @Override public IFloatVectorSlicer slicer() {
        return new AbstractFloatVectorSlicer() {
            @Override protected IFloatVector getL(final ISlice aIndices) {IFloatVector rVector = newZeros_(aIndices.size()); rVector.fill(refSlicer().get(aIndices)); return rVector;}
            @Override protected IFloatVector getA() {return copy();}
            
            @Override protected int thisSize_() {return size();}
        };
    }
    @Override public IFloatVectorSlicer refSlicer() {
        return new AbstractFloatVectorSlicer() {
            @Override protected IFloatVector getL(final ISlice aIndices) {
                return new RefFloatVector() {
                    @Override public float get(int aIdx) {return AbstractFloatVector.this.get(aIndices.get(aIdx));}
                    @Override public void set(int aIdx, float aValue) {AbstractFloatVector.this.set(aIndices.get(aIdx), aValue);}
                    @Override public float getAndSet(int aIdx, float aValue) {return AbstractFloatVector.this.getAndSet(aIndices.get(aIdx), aValue);}
                    @Override public int size() {return aIndices.size();}
                };
            }
            @Override protected IFloatVector getA() {
                return new RefFloatVector() {
                    @Override public float get(int aIdx) {return AbstractFloatVector.this.get(aIdx);}
                    @Override public void set(int aIdx, float aValue) {AbstractFloatVector.this.set(aIdx, aValue);}
                    @Override public float getAndSet(int aIdx, float aValue) {return AbstractFloatVector.this.getAndSet(aIdx, aValue);}
                    @Override public int size() {return AbstractFloatVector.this.size();}
                };
            }
            
            @Override protected int thisSize_() {return size();}
        };
    }
    @Override public IFloatVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, size());
        return new RefFloatVector() {
            /** 由于一开始有边界检查，所以这里不再需要边检检查 */
            @Override public float get(int aIdx) {rangeCheck(aIdx, size()); return AbstractFloatVector.this.get(aIdx+aFromIdx);}
            @Override public void set(int aIdx, float aValue) {rangeCheck(aIdx, size()); AbstractFloatVector.this.set(aIdx+aFromIdx, aValue);}
            @Override public float getAndSet(int aIdx, float aValue) {rangeCheck(aIdx, size()); return AbstractFloatVector.this.getAndSet(aIdx+aFromIdx, aValue);}
            @Override public int size() {return aToIdx-aFromIdx;}
        };
    }
    
    /** 向量的运算器 */
    @Override public IFloatVectorOperation operation() {
        return new AbstractFloatVectorOperation() {
            @Override protected IFloatVector thisVector_() {return AbstractFloatVector.this;}
            @Override protected IFloatVector newVector_(int aSize) {return newZeros_(aSize);}
        };
    }
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting @Override public float call(int aIdx) {return get(aIdx);}
    @VisibleForTesting @Override public IFloatVector call(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IFloatVector call(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IFloatVector call(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IFloatVector call(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public IFloatVector getAt(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IFloatVector getAt(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IFloatVector getAt(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IFloatVector getAt(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, float aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, IFloatVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, float aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, IFloatVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, float aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, IFloatVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, float aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, IFloatVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public float getAt(int aIdx) {return get((aIdx < 0) ? (size()+aIdx) : aIdx);}
    @VisibleForTesting @Override public void putAt(int aIdx, float aValue) {set((aIdx < 0) ? (size()+aIdx) : aIdx, aValue);}
    
    
    /** stuff to override */
    @Override public abstract float get(int aIdx);
    @Override public abstract void set(int aIdx, float aValue);
    @Override public abstract float getAndSet(int aIdx, float aValue);
    @Override public abstract int size();
    protected abstract IFloatVector newZeros_(int aSize);
    
    protected String toString_(float aValue) {return String.format("   %.4g", aValue);}
}
