package jtool.math.vector;

import jtool.code.CS.SliceType;
import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.collection.ISlice;
import jtool.code.functional.IIndexFilter;
import jtool.code.functional.*;
import jtool.code.iterator.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;

import static jtool.math.vector.AbstractVector.*;

/**
 * @author liqa
 * <p> 通用的逻辑向量类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractLogicalVector implements ILogicalVector {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d-length Logical Vector:", size()));
        rStr.append("\n");
        forEach(v -> rStr.append(toString_(v)));
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IBooleanIterator iterator() {
        return new IBooleanIterator() {
            private final int mSize = size();
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = get(mIdx);
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IBooleanSetIterator setIterator() {
        return new IBooleanSetIterator() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractLogicalVector.this.set(oIdx, aValue);
            }
            @Override public boolean next() {
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
    @Override public List<Boolean> asList() {
        return new AbstractRandomAccessList<Boolean>() {
            @Override public Boolean get(int index) {return AbstractLogicalVector.this.get(index);}
            @Override public Boolean set(int index, Boolean element) {return getAndSet(index, element);}
            @Override public int size() {return AbstractLogicalVector.this.size();}
            @Override public @NotNull Iterator<Boolean> iterator() {return AbstractLogicalVector.this.iterator().toIterator();}
        };
    }
    @Override public IVector asVec() {
        return new RefVector() {
            @Override public double get(int aIdx) {return AbstractLogicalVector.this.get(aIdx) ? 1.0 : 0.0;}
            @Override public void set(int aIdx, double aValue) {AbstractLogicalVector.this.set(aIdx, aValue!=0.0);}
            @Override public double getAndSet(int aIdx, double aValue) {return AbstractLogicalVector.this.getAndSet(aIdx, aValue!=0.0) ? 1.0 : 0.0;}
            @Override public int size() {return AbstractLogicalVector.this.size();}
        };
    }
    
    /** 转为兼容性更好的 boolean[] */
    @Override public boolean[] data() {
        final int tSize = size();
        boolean[] rData = new boolean[tSize];
        final IBooleanIterator it = iterator();
        for (int i = 0; i < tSize; ++i) rData[i] = it.next();
        return rData;
    }
    
    /** ISwapper stuffs */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, size());
        set(aIdx1, getAndSet(aIdx2, get(aIdx1)));
    }
    
    
    
    /** 批量修改的接口 */
    @Override public final void fill(boolean aValue) {operation().fill(aValue);}
    @Override public final void fill(ILogicalVector aVector) {operation().fill(aVector);}
    @Override public final void fill(ILogicalVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    @Override public final void fill(Iterable<Boolean> aList) {
        final Iterator<Boolean> it = aList.iterator();
        assign(it::next);
    }
    @Override public void fill(boolean[] aData) {
        final IBooleanSetIterator si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(aData[idx]);
            ++idx;
        }
    }
    @Override public final void assign(BooleanSupplier aSup) {operation().assign(aSup);}
    @Override public final void forEach(IBooleanConsumer aCon) {operation().forEach(aCon);}
    
    
    @Override public void flip(int aIdx) {
        rangeCheck(aIdx, size());
        set(aIdx, !get(aIdx));
    }
    @Override public boolean getAndFlip(int aIdx) {
        rangeCheck(aIdx, size());
        boolean tValue = get(aIdx);
        set(aIdx, !tValue);
        return tValue;
    }
    @Override public void update(int aIdx, IBooleanUnaryOperator aOpt) {
        rangeCheck(aIdx, size());
        boolean tValue = get(aIdx);
        tValue = aOpt.applyAsBoolean(tValue);
        set(aIdx, tValue);
    }
    @Override public boolean getAndUpdate(int aIdx, IBooleanUnaryOperator aOpt) {
        rangeCheck(aIdx, size());
        boolean tValue = get(aIdx);
        set(aIdx, aOpt.applyAsBoolean(tValue));
        return tValue;
    }
    
    
    @Override public ILogicalVector copy() {
        ILogicalVector rVector = newZeros_(size());
        rVector.fill(this);
        return rVector;
    }
    
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    @Override public ILogicalVectorSlicer slicer() {
        return new AbstractLogicalVectorSlicer() {
            @Override protected ILogicalVector getL(final ISlice aIndices) {ILogicalVector rVector = newZeros_(aIndices.size()); rVector.fill(i -> AbstractLogicalVector.this.get(aIndices.get(i))); return rVector;}
            @Override protected ILogicalVector getA() {return copy();}
            
            @Override protected int thisSize_() {return size();}
        };
    }
    @Override public ILogicalVectorSlicer refSlicer() {
        return new AbstractLogicalVectorSlicer() {
            @Override protected ILogicalVector getL(final ISlice aIndices) {
                return new RefLogicalVector() {
                    @Override public boolean get(int aIdx) {return AbstractLogicalVector.this.get(aIndices.get(aIdx));}
                    @Override public void set(int aIdx, boolean aValue) {AbstractLogicalVector.this.set(aIndices.get(aIdx), aValue);}
                    @Override public boolean getAndSet(int aIdx, boolean aValue) {return AbstractLogicalVector.this.getAndSet(aIndices.get(aIdx), aValue);}
                    @Override public int size() {return aIndices.size();}
                };
            }
            @Override protected ILogicalVector getA() {
                return new RefLogicalVector() {
                    @Override public boolean get(int aIdx) {return AbstractLogicalVector.this.get(aIdx);}
                    @Override public void set(int aIdx, boolean aValue) {AbstractLogicalVector.this.set(aIdx, aValue);}
                    @Override public boolean getAndSet(int aIdx, boolean aValue) {return AbstractLogicalVector.this.getAndSet(aIdx, aValue);}
                    @Override public int size() {return AbstractLogicalVector.this.size();}
                };
            }
            
            @Override protected int thisSize_() {return size();}
        };
    }
    @Override public ILogicalVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, size());
        return new RefLogicalVector() {
            /** 由于一开始有边界检查，所以这里不再需要边检检查 */
            @Override public boolean get(int aIdx) {rangeCheck(aIdx, size()); return AbstractLogicalVector.this.get(aIdx+aFromIdx);}
            @Override public void set(int aIdx, boolean aValue) {rangeCheck(aIdx, size()); AbstractLogicalVector.this.set(aIdx+aFromIdx, aValue);}
            @Override public boolean getAndSet(int aIdx, boolean aValue) {rangeCheck(aIdx, size()); return AbstractLogicalVector.this.getAndSet(aIdx+aFromIdx, aValue);}
            @Override public int size() {return aToIdx-aFromIdx;}
        };
    }
    
    
    /** 向量的运算器 */
    @Override public ILogicalVectorOperation operation() {
        return new AbstractLogicalVectorOperation() {
            @Override protected ILogicalVector thisVector_() {return AbstractLogicalVector.this;}
            @Override protected ILogicalVector newVector_(int aSize) {return newZeros_(aSize);}
        };
    }
    
    
    /** Groovy 的部分，增加向量基本的运算操作 */
    @Override public ILogicalVector and (boolean aRHS) {return operation().and(aRHS);}
    @Override public ILogicalVector or  (boolean aRHS) {return operation().or (aRHS);}
    @Override public ILogicalVector xor (boolean aRHS) {return operation().xor(aRHS);}
    
    @Override public ILogicalVector and (ILogicalVector aRHS) {return operation().and(aRHS);}
    @Override public ILogicalVector or  (ILogicalVector aRHS) {return operation().or (aRHS);}
    @Override public ILogicalVector xor (ILogicalVector aRHS) {return operation().xor(aRHS);}
    @Override public ILogicalVector not () {return operation().not();}
    
    @Override public final void and2this(boolean aRHS) {operation().and2this(aRHS);}
    @Override public final void or2this (boolean aRHS) {operation().or2this (aRHS);}
    @Override public final void xor2this(boolean aRHS) {operation().xor2this(aRHS);}
    
    @Override public final void and2this(ILogicalVector aRHS) {operation().and2this(aRHS);}
    @Override public final void or2this (ILogicalVector aRHS) {operation().or2this (aRHS);}
    @Override public final void xor2this(ILogicalVector aRHS) {operation().xor2this(aRHS);}
    @Override public final void not2this() {operation().not2this();}
    
     @Override public final boolean all  () {return operation().all  ();}
     @Override public final boolean any  () {return operation().any  ();}
     @Override public final int     count() {return operation().count();}
    
    /** Groovy 的部分，增加矩阵切片操作 */
    @VisibleForTesting @Override public boolean call(int aIdx) {return get(aIdx);}
    @VisibleForTesting @Override public ILogicalVector call(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILogicalVector call(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILogicalVector call(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILogicalVector call(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public ILogicalVector getAt(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILogicalVector getAt(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILogicalVector getAt(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public ILogicalVector getAt(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, boolean aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, Iterable<Boolean> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, ILogicalVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, boolean aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, Iterable<Boolean> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, ILogicalVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, boolean aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, Iterable<Boolean> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, ILogicalVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, boolean aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, Iterable<Boolean> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, ILogicalVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public boolean getAt(int aIdx) {return get((aIdx < 0) ? (size()+aIdx) : aIdx);}
    @VisibleForTesting @Override public void putAt(int aIdx, boolean aValue) {set((aIdx < 0) ? (size()+aIdx) : aIdx, aValue);}
    
    
    /** stuff to override */
    public abstract boolean get(int aIdx);
    public abstract void set(int aIdx, boolean aValue);
    public abstract boolean getAndSet(int aIdx, boolean aValue);
    public abstract int size();
    protected abstract ILogicalVector newZeros_(int aSize);
    
    protected String toString_(boolean aValue) {return " "+(aValue?"T":"F");}
}
