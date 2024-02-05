package jse.math.vector;

import groovy.lang.Closure;
import jse.code.CS.SliceType;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.ISlice;
import jse.code.functional.IDoubleBinaryConsumer;
import jse.code.functional.IIndexFilter;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IComplexDoubleIterator;
import jse.code.iterator.IComplexDoubleSetIterator;
import jse.code.iterator.IComplexDoubleSetOnlyIterator;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

import static jse.math.vector.AbstractVector.*;

/**
 * @author liqa
 * <p> 通用的向量类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractComplexVector implements IComplexVector {
    /** print */
    @Override public String toString() {
        final StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d-length Complex Vector:", size()));
        rStr.append("\n");
        forEach((real, imag) -> rStr.append(toString_(real, imag)));
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IComplexDoubleIterator iterator() {
        return new IComplexDoubleIterator() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return getReal(oIdx);
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return getImag(oIdx);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oIdx);}
        };
    }
    @Override public IComplexDoubleSetIterator setIterator() {
        return new IComplexDoubleSetIterator() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractComplexVector.this.set(oIdx, aReal, aImag);
            }
            @Override public void setReal(double aReal) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractComplexVector.this.setReal(oIdx, aReal);
            }
            @Override public void setImag(double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractComplexVector.this.setImag(oIdx, aImag);
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return getReal(oIdx);
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return getImag(oIdx);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oIdx);}
            @Override public void set(IComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractComplexVector.this.set(oIdx, aValue);
            }
            @Override public void set(ComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractComplexVector.this.set(oIdx, aValue);
            }
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                AbstractComplexVector.this.set(oIdx, aValue);
            }
        };
    }
    
    
    /** 转换为其他类型 */
    @Override public List<ComplexDouble> asList() {
        return new AbstractRandomAccessList<ComplexDouble>() {
            @Override public ComplexDouble get(int index) {return AbstractComplexVector.this.get(index);}
            @Override public ComplexDouble set(int index, ComplexDouble element) {return getAndSet(index, element);}
            @Override public int size() {return AbstractComplexVector.this.size();}
            @Override public @NotNull Iterator<ComplexDouble> iterator() {return AbstractComplexVector.this.iterator().toIterator();}
        };
    }
    
    /** 获取实部和虚部 */
    @Override public IVector real() {
        return new RefVector() {
            /** 这里不再需要二次边界检查 */
            @Override public double get(int aIdx) {return AbstractComplexVector.this.getReal(aIdx);}
            @Override public void set(int aIdx, double aValue) {AbstractComplexVector.this.setReal(aIdx, aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {return AbstractComplexVector.this.getAndSetReal(aIdx, aValue);}
            @Override public int size() {return AbstractComplexVector.this.size();}
        };
    }
    @Override public IVector imag() {
        return new RefVector() {
            /** 这里不再需要二次边界检查 */
            @Override public double get(int aIdx) {return AbstractComplexVector.this.getImag(aIdx);}
            @Override public void set(int aIdx, double aValue) {AbstractComplexVector.this.setImag(aIdx, aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {return AbstractComplexVector.this.getAndSetImag(aIdx, aValue);}
            @Override public int size() {return AbstractComplexVector.this.size();}
        };
    }
    
    @Override public IComplexVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, size());
        return new RefComplexVector() {
            /** 由于一开始有边界检查，所以这里不再需要边检检查 */
            @Override public double getReal(int aIdx) {rangeCheck(aIdx, size()); return AbstractComplexVector.this.getReal(aIdx+aFromIdx);}
            @Override public double getImag(int aIdx) {rangeCheck(aIdx, size()); return AbstractComplexVector.this.getImag(aIdx+aFromIdx);}
            @Override public void set(int aIdx, double aReal, double aImag) {rangeCheck(aIdx, size()); AbstractComplexVector.this.set(aIdx+aFromIdx, aReal, aImag);}
            @Override public void setReal(int aIdx, double aReal) {rangeCheck(aIdx, size()); AbstractComplexVector.this.setReal(aIdx+aFromIdx, aReal);}
            @Override public void setImag(int aIdx, double aImag) {rangeCheck(aIdx, size()); AbstractComplexVector.this.setImag(aIdx+aFromIdx, aImag);}
            @Override public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {rangeCheck(aIdx, size()); return AbstractComplexVector.this.getAndSet(aIdx+aFromIdx, aReal, aImag);}
            @Override public double getAndSetReal(int aIdx, double aReal) {rangeCheck(aIdx, size()); return AbstractComplexVector.this.getAndSetReal(aIdx+aFromIdx, aReal);}
            @Override public double getAndSetImag(int aIdx, double aImag) {rangeCheck(aIdx, size()); return AbstractComplexVector.this.getAndSetImag(aIdx+aFromIdx, aImag);}
            @Override public int size() {return aToIdx-aFromIdx;}
        };
    }
    
    /** 转为兼容性更好的 double[][] */
    @Override public double[][] data() {
        final int tSize = size();
        double[][] rData = new double[2][tSize];
        double[] rReal = rData[0];
        double[] rImag = rData[1];
        final IComplexDoubleIterator it = iterator();
        for (int i = 0; i < tSize; ++i) {
            it.nextOnly();
            rReal[i] = it.real();
            rImag[i] = it.imag();
        }
        return rData;
    }
    
    /** ISwapper stuffs */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, size());
        set(aIdx1, getAndSet(aIdx2, get(aIdx1)));
    }
    
    
    /** 批量修改的接口 */
    @Override public final void fill(IComplexDouble aValue) {operation().fill(aValue);}
    @Override public final void fill(double aValue) {operation().fill(aValue);}
    @Override public final void fill(IComplexVector aVector) {operation().fill(aVector);}
    @Override public final void fill(IVector aVector) {operation().fill(aVector);}
    @Override public final void fill(IComplexVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    @Override public final void fill(IVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    
    @Override public final void fill(Iterable<?> aList) {
        final Iterator<?> it = aList.iterator();
        final IComplexDoubleSetOnlyIterator si = setIterator();
        while (si.hasNext()) {
            // 直接先执行然后检测类型决定如何设置
            Object tObj = it.next();
            if (tObj instanceof IComplexDouble) si.nextAndSet((IComplexDouble)tObj);
            else if (tObj instanceof Number) si.nextAndSet(((Number)tObj).doubleValue());
            else si.nextAndSet(Double.NaN);
        }
    }
    @Override public void fill(double[][] aData) {
        final double[] tRealData = aData[0];
        final double[] tImagData = aData[1];
        final IComplexDoubleSetOnlyIterator si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(tRealData[idx], tImagData[idx]);
            ++idx;
        }
    }
    @Override public void fill(double[] aData) {
        final IComplexDoubleSetOnlyIterator si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(aData[idx]);
            ++idx;
        }
    }
    
    @Override public final void assign(Supplier<? extends IComplexDouble> aSup) {operation().assign(aSup);}
    @Override public final void assign(DoubleSupplier aSup) {operation().assign(aSup);}
    @Override public final void forEach(Consumer<? super ComplexDouble> aCon) {operation().forEach(aCon);}
    @Override public final void forEach(IDoubleBinaryConsumer aCon) {operation().forEach(aCon);}
    /** Groovy stuff */
    @Override public void fill(Closure<?> aGroovyTask) {operation().fill(aGroovyTask);}
    @Override public final void assign(Closure<?> aGroovyTask) {operation().assign(aGroovyTask);}
    @Override public final void forEach(Closure<?> aGroovyTask) {operation().forEach(aGroovyTask);}
    
    
    @Override public void add(int aIdx, IComplexDouble aDelta) {
        rangeCheck(aIdx, size());
        ComplexDouble tValue = get(aIdx);
        tValue.plus2this(aDelta);
        set(aIdx, tValue);
    }
    @Override public void add(int aIdx, ComplexDouble aDelta) {
        rangeCheck(aIdx, size());
        ComplexDouble tValue = get(aIdx);
        tValue.plus2this(aDelta);
        set(aIdx, tValue);
    }
    @Override public void add(int aIdx, double aDelta) {
        rangeCheck(aIdx, size());
        double tReal = getReal(aIdx);
        tReal += aDelta;
        setReal(aIdx, tReal);
    }
    @Override public void addImag(int aIdx, double aImag) {
        rangeCheck(aIdx, size());
        double tImag = getImag(aIdx);
        tImag += aImag;
        setImag(aIdx, tImag);
    }
    @Override public void update(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheck(aIdx, size());
        set(aIdx, aOpt.apply(get(aIdx)));
    }
    @Override public void updateReal(int aIdx, DoubleUnaryOperator aRealOpt) {
        rangeCheck(aIdx, size());
        setReal(aIdx, aRealOpt.applyAsDouble(getReal(aIdx)));
    }
    @Override public void updateImag(int aIdx, DoubleUnaryOperator aImagOpt) {
        rangeCheck(aIdx, size());
        setImag(aIdx, aImagOpt.applyAsDouble(getImag(aIdx)));
    }
    @Override public ComplexDouble getAndUpdate(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheck(aIdx, size());
        ComplexDouble oValue = get(aIdx);
        set(aIdx, aOpt.apply(new ComplexDouble(oValue))); // 用来防止意外的修改
        return oValue;
    }
    @Override public double getAndUpdateReal(int aIdx, DoubleUnaryOperator aRealOpt) {
        rangeCheck(aIdx, size());
        double tReal = getReal(aIdx);
        setReal(aIdx, aRealOpt.applyAsDouble(tReal));
        return tReal;
    }
    @Override public double getAndUpdateImag(int aIdx, DoubleUnaryOperator aImagOpt) {
        rangeCheck(aIdx, size());
        double tImag = getImag(aIdx);
        setImag(aIdx, aImagOpt.applyAsDouble(tImag));
        return tImag;
    }
    
    
    @Override public IComplexVector copy() {
        IComplexVector rVector = newZeros_(size());
        rVector.fill(this);
        return rVector;
    }
    
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    @Override public IComplexVectorSlicer slicer() {
        return new AbstractComplexVectorSlicer() {
            @Override protected IComplexVector getL(final ISlice aIndices) {IComplexVector rVector = newZeros_(aIndices.size()); rVector.fill(refSlicer().get(aIndices)); return rVector;}
            @Override protected IComplexVector getA() {return copy();}
            
            @Override protected int thisSize_() {return size();}
        };
    }
    @Override public IComplexVectorSlicer refSlicer() {
        return new AbstractComplexVectorSlicer() {
            @Override protected IComplexVector getL(final ISlice aIndices) {
                return new RefComplexVector() {
                    @Override public double getReal(int aIdx) {return AbstractComplexVector.this.getReal(aIndices.get(aIdx));}
                    @Override public double getImag(int aIdx) {return AbstractComplexVector.this.getImag(aIndices.get(aIdx));}
                    @Override public void set(int aIdx, double aReal, double aImag) {AbstractComplexVector.this.set(aIndices.get(aIdx), aReal, aImag);}
                    @Override public void setReal(int aIdx, double aReal) {AbstractComplexVector.this.setReal(aIndices.get(aIdx), aReal);}
                    @Override public void setImag(int aIdx, double aImag) {AbstractComplexVector.this.setImag(aIndices.get(aIdx), aImag);}
                    @Override public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {return AbstractComplexVector.this.getAndSet(aIndices.get(aIdx), aReal, aImag);}
                    @Override public double getAndSetReal(int aIdx, double aReal) {return AbstractComplexVector.this.getAndSetReal(aIndices.get(aIdx), aReal);}
                    @Override public double getAndSetImag(int aIdx, double aImag) {return AbstractComplexVector.this.getAndSetImag(aIndices.get(aIdx), aImag);}
                    @Override public int size() {return aIndices.size();}
                };
            }
            @Override protected IComplexVector getA() {
                return new RefComplexVector() {
                    @Override public double getReal(int aIdx) {return AbstractComplexVector.this.getReal(aIdx);}
                    @Override public double getImag(int aIdx) {return AbstractComplexVector.this.getImag(aIdx);}
                    @Override public void set(int aIdx, double aReal, double aImag) {AbstractComplexVector.this.set(aIdx, aReal, aImag);}
                    @Override public void setReal(int aIdx, double aReal) {AbstractComplexVector.this.setReal(aIdx, aReal);}
                    @Override public void setImag(int aIdx, double aImag) {AbstractComplexVector.this.setImag(aIdx, aImag);}
                    @Override public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {return AbstractComplexVector.this.getAndSet(aIdx, aReal, aImag);}
                    @Override public double getAndSetReal(int aIdx, double aReal) {return AbstractComplexVector.this.getAndSetReal(aIdx, aReal);}
                    @Override public double getAndSetImag(int aIdx, double aImag) {return AbstractComplexVector.this.getAndSetImag(aIdx, aImag);}
                    @Override public int size() {return AbstractComplexVector.this.size();}
                };
            }
            
            @Override protected int thisSize_() {return size();}
        };
    }
    
    
    /** 向量的运算器 */
    @Override public IComplexVectorOperation operation() {
        return new AbstractComplexVectorOperation() {
            @Override protected IComplexVector thisVector_() {return AbstractComplexVector.this;}
            @Override protected IComplexVector newVector_(int aSize) {return newZeros_(aSize);}
        };
    }
    
    
    /** Groovy 的部分，增加向量基本的运算操作 */
    @Override public final IComplexVector plus      (IComplexDouble aRHS) {return operation().plus    (aRHS);}
    @Override public final IComplexVector minus     (IComplexDouble aRHS) {return operation().minus   (aRHS);}
    @Override public final IComplexVector multiply  (IComplexDouble aRHS) {return operation().multiply(aRHS);}
    @Override public final IComplexVector div       (IComplexDouble aRHS) {return operation().div     (aRHS);}
    @Override public final IComplexVector plus      (double         aRHS) {return operation().plus    (aRHS);}
    @Override public final IComplexVector minus     (double         aRHS) {return operation().minus   (aRHS);}
    @Override public final IComplexVector multiply  (double         aRHS) {return operation().multiply(aRHS);}
    @Override public final IComplexVector div       (double         aRHS) {return operation().div     (aRHS);}
    
    @Override public final IComplexVector plus      (IComplexVector aRHS) {return operation().plus    (aRHS);}
    @Override public final IComplexVector minus     (IComplexVector aRHS) {return operation().minus   (aRHS);}
    @Override public final IComplexVector multiply  (IComplexVector aRHS) {return operation().multiply(aRHS);}
    @Override public final IComplexVector div       (IComplexVector aRHS) {return operation().div     (aRHS);}
    @Override public final IComplexVector plus      (IVector        aRHS) {return operation().plus    (aRHS);}
    @Override public final IComplexVector minus     (IVector        aRHS) {return operation().minus   (aRHS);}
    @Override public final IComplexVector multiply  (IVector        aRHS) {return operation().multiply(aRHS);}
    @Override public final IComplexVector div       (IVector        aRHS) {return operation().div     (aRHS);}
    
    @Override public final void plus2this       (IComplexDouble aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (IComplexDouble aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (IComplexDouble aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (IComplexDouble aRHS) {operation().div2this     (aRHS);}
    @Override public final void plus2this       (double         aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (double         aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (double         aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (double         aRHS) {operation().div2this     (aRHS);}
    
    @Override public final void plus2this       (IComplexVector aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (IComplexVector aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (IComplexVector aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (IComplexVector aRHS) {operation().div2this     (aRHS);}
    @Override public final void plus2this       (IVector        aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (IVector        aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (IVector        aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (IVector        aRHS) {operation().div2this     (aRHS);}
    
    @Override public final IComplexVector negative() {return operation().negative();}
    @Override public final void negative2this() {operation().negative2this();}
    
    /** Groovy 的部分，增加矩阵切片操作 */
    @VisibleForTesting @Override public ComplexDouble call(int aIdx) {return get(aIdx);}
    @VisibleForTesting @Override public IComplexVector call(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IComplexVector call(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IComplexVector call(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IComplexVector call(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public IComplexVector getAt(ISlice        aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IComplexVector getAt(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IComplexVector getAt(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public IComplexVector getAt(IIndexFilter  aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, IComplexDouble aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, ComplexDouble aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, double aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, IComplexVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(ISlice        aIndices, IVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, IComplexDouble aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, ComplexDouble aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, double aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, IComplexVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, IVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, IComplexDouble aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, ComplexDouble aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, double aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, IComplexVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, IVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, IComplexDouble aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, ComplexDouble aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, double aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList) {refSlicer().get(aIndices).fill(aList);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, IComplexVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    @VisibleForTesting @Override public void putAt(IIndexFilter  aIndices, IVector aVector) {refSlicer().get(aIndices).fill(aVector);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public ComplexDouble getAt(int aIdx) {return get((aIdx < 0) ? (size()+aIdx) : aIdx);}
    @VisibleForTesting @Override public void putAt(int aIdx, IComplexDouble aValue) {set((aIdx < 0) ? (size()+aIdx) : aIdx, aValue);}
    @VisibleForTesting @Override public void putAt(int aIdx, ComplexDouble aValue) {set((aIdx < 0) ? (size()+aIdx) : aIdx, aValue);}
    @VisibleForTesting @Override public void putAt(int aIdx, double aValue) {set((aIdx < 0) ? (size()+aIdx) : aIdx, aValue);}
    
    
    /** stuff to override */
    public ComplexDouble get(int aIdx) {return new ComplexDouble(getReal(aIdx), getImag(aIdx));}
    public abstract double getReal(int aIdx);
    public abstract double getImag(int aIdx);
    public void set(int aIdx, IComplexDouble aValue) {set(aIdx, aValue.real(), aValue.imag());}
    public void set(int aIdx, ComplexDouble aValue) {set(aIdx, aValue.mReal, aValue.mImag);}
    public void set(int aIdx, double aValue) {set(aIdx, aValue, 0.0);}
    public abstract void set(int aIdx, double aReal, double aImag);
    public abstract void setReal(int aIdx, double aReal);
    public abstract void setImag(int aIdx, double aImag);
    public ComplexDouble getAndSet(int aIdx, IComplexDouble aValue) {return getAndSet(aIdx, aValue.real(), aValue.imag());}
    public ComplexDouble getAndSet(int aIdx, ComplexDouble aValue) {return getAndSet(aIdx, aValue.mReal, aValue.mImag);}
    public ComplexDouble getAndSet(int aIdx, double aValue) {return getAndSet(aIdx, aValue, 0.0);}
    public abstract ComplexDouble getAndSet(int aIdx, double aReal, double aImag);
    public abstract double getAndSetReal(int aIdx, double aReal);
    public abstract double getAndSetImag(int aIdx, double aImag);
    
    public abstract int size();
    protected abstract IComplexVector newZeros_(int aSize);
    
    protected String toString_(double aReal, double aImag) {return Double.compare(aImag, 0.0)>=0 ? String.format("   %.4g + %.4gi", aReal, aImag) : String.format("   %.4g - %.4gi", aReal, -aImag);}
}
