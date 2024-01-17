package jtool.math.vector;

import jtool.code.CS.SliceType;
import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.collection.ISlice;
import jtool.code.functional.IIndexFilter;
import jtool.code.functional.*;
import jtool.code.iterator.*;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import groovy.lang.Closure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

import static jtool.math.vector.AbstractVector.subVecRangeCheck;

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
                return getReal_(oIdx);
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return getImag_(oIdx);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get_(oIdx);}
        };
    }
    @Override public IComplexDoubleSetIterator setIterator() {
        return new IComplexDoubleSetIterator() {
            private final int mSize = size();
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void setReal(double aReal) {
                if (oIdx < 0) throw new IllegalStateException();
                setReal_(oIdx, aReal);
            }
            @Override public void setImag(double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                setImag_(oIdx, aImag);
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
                return getReal_(oIdx);
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return getImag_(oIdx);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get_(oIdx);}
            @Override public void set(IComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                set_(oIdx, aValue);
            }
            @Override public void set(ComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                set_(oIdx, aValue);
            }
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                set_(oIdx, aValue);
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
            @Override public double get_(int aIdx) {return AbstractComplexVector.this.getReal_(aIdx);}
            @Override public void set_(int aIdx, double aValue) {AbstractComplexVector.this.setReal_(aIdx, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return AbstractComplexVector.this.getAndSetReal_(aIdx, aValue);}
            @Override public int size() {return AbstractComplexVector.this.size();}
        };
    }
    @Override public IVector imag() {
        return new RefVector() {
            /** 这里不再需要二次边界检查 */
            @Override public double get_(int aIdx) {return AbstractComplexVector.this.getImag_(aIdx);}
            @Override public void set_(int aIdx, double aValue) {AbstractComplexVector.this.setImag_(aIdx, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return AbstractComplexVector.this.getAndSetImag_(aIdx, aValue);}
            @Override public int size() {return AbstractComplexVector.this.size();}
        };
    }
    
    @Override public IComplexVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, size());
        return new RefComplexVector() {
            /** 由于一开始有边界检查，所以这里不再需要边检检查 */
            @Override public double getReal_(int aIdx) {return AbstractComplexVector.this.getReal_(aIdx+aFromIdx);}
            @Override public double getImag_(int aIdx) {return AbstractComplexVector.this.getImag_(aIdx+aFromIdx);}
            @Override public void setReal_(int aIdx, double aReal) {AbstractComplexVector.this.setReal_(aIdx+aFromIdx, aReal);}
            @Override public void setImag_(int aIdx, double aImag) {AbstractComplexVector.this.setImag_(aIdx+aFromIdx, aImag);}
            @Override public double getAndSetReal_(int aIdx, double aReal) {return AbstractComplexVector.this.getAndSetReal_(aIdx+aFromIdx, aReal);}
            @Override public double getAndSetImag_(int aIdx, double aImag) {return AbstractComplexVector.this.getAndSetImag_(aIdx+aFromIdx, aImag);}
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
    
    
    /** 批量修改的接口 */
    @Override public final void fill(IComplexDouble aValue) {operation().fill(aValue);}
    @Override public final void fill(double aValue) {operation().fill(aValue);}
    @Override public final void fillReal(double aReal) {real().fill(aReal);}
    @Override public final void fillImag(double aImag) {imag().fill(aImag);}
    @Override public final void fill(IComplexVector aVector) {operation().fill(aVector);}
    @Override public final void fill(IVector aVector) {operation().fill(aVector);}
    @Override public final void fillReal(IVector aRealVector) {real().fill(aRealVector);}
    @Override public final void fillImag(IVector aImagVector) {imag().fill(aImagVector);}
    @Override public final void fill(IComplexVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    @Override public final void fill(IVectorGetter aVectorGetter) {operation().fill(aVectorGetter);}
    @Override public final void fillReal(IVectorGetter aRealGetter) {real().fill(aRealGetter);}
    @Override public final void fillImag(IVectorGetter aImagGetter) {imag().fill(aImagGetter);}
    @Override public final void fillReal(double[] aRealData) {real().fill(aRealData);}
    @Override public final void fillImag(double[] aImagData) {imag().fill(aImagData);}
    @Override public final void fillReal(Iterable<? extends Number> aRealList) {real().fill(aRealList);}
    @Override public final void fillImag(Iterable<? extends Number> aImagList) {imag().fill(aImagList);}
    
    @Override public final void fill(Iterable<? extends Number> aList) {
        final Iterator<? extends Number> it = aList.iterator();
        assign(() -> it.next().doubleValue());
    }
    @Override public void fill(double[] aData) {
        final IComplexDoubleSetIterator si = setIterator();
        int idx = 0;
        while (si.hasNext()) {
            si.nextAndSet(aData[idx]);
            ++idx;
        }
    }
    
    @Override public final void assign(Supplier<? extends IComplexDouble> aSup) {operation().assign(aSup);}
    @Override public final void assign(IDoubleSupplier aSup) {operation().assign(aSup);}
    @Override public final void assignReal(IDoubleSupplier aRealSup) {real().assign(aRealSup);}
    @Override public final void assignImag(IDoubleSupplier aImagSup) {imag().assign(aImagSup);}
    @Override public final void forEach(IConsumer1<? super ComplexDouble> aCon) {operation().forEach(aCon);}
    @Override public final void forEach(IDoubleConsumer2 aCon) {operation().forEach(aCon);}
    @Override public final void forEachReal(IDoubleConsumer1 aCon) {real().forEach(aCon);}
    @Override public final void forEachImag(IDoubleConsumer1 aCon) {imag().forEach(aCon);}
    /** Groovy stuff */
    @Override public void fill(Closure<?> aGroovyTask) {operation().fill(aGroovyTask);}
    @Override public void fillReal(Closure<? extends Number> aGroovyTask) {real().fill(aGroovyTask);}
    @Override public void fillImag(Closure<? extends Number> aGroovyTask) {imag().fill(aGroovyTask);}
    @Override public final void assign(Closure<?> aGroovyTask) {operation().assign(aGroovyTask);}
    @Override public final void forEach(Closure<?> aGroovyTask) {operation().forEach(aGroovyTask);}
    
    
    @Override public ComplexDouble get(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return get_(aIdx);
    }
    @Override public double getReal(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getReal_(aIdx);
    }
    @Override public double getImag(int aIdx) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getImag_(aIdx);
    }
    @Override public void set(int aIdx, IComplexDouble aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        set_(aIdx, aValue);
    }
    @Override public void set(int aIdx, ComplexDouble aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        set_(aIdx, aValue);
    }
    @Override public void set(int aIdx, double aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        set_(aIdx, aValue);
    }
    @Override public void setReal(int aIdx, double aReal) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        setReal_(aIdx, aReal);
    }
    @Override public void setImag(int aIdx, double aImag) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        setImag_(aIdx, aImag);
    }
    @Override public ComplexDouble getAndSet(int aIdx, IComplexDouble aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndSet_(aIdx, aValue);
    }
    @Override public ComplexDouble getAndSet(int aIdx, ComplexDouble aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndSet_(aIdx, aValue);
    }
    @Override public ComplexDouble getAndSet(int aIdx, double aValue) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndSet_(aIdx, aValue);
    }
    @Override public double getAndSetReal(int aIdx, double aReal) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndSetReal_(aIdx, aReal);
    }
    @Override public double getAndSetImag(int aIdx, double aImag) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndSetImag_(aIdx, aImag);
    }
    
    @Override public void add_(int aIdx, IComplexDouble aDelta) {
        ComplexDouble tValue = get_(aIdx);
        tValue.plus2this(aDelta);
        set_(aIdx, tValue);
    }
    @Override public void add_(int aIdx, ComplexDouble aDelta) {
        ComplexDouble tValue = get_(aIdx);
        tValue.plus2this(aDelta);
        set_(aIdx, tValue);
    }
    @Override public void add_(int aIdx, double aDelta) {
        double tReal = getReal_(aIdx);
        tReal += aDelta;
        setReal_(aIdx, tReal);
    }
    @Override public void addImag_(int aIdx, double aImag) {
        double tImag = getImag_(aIdx);
        tImag += aImag;
        setImag_(aIdx, tImag);
    }
    @Override public void update_(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {set_(aIdx, aOpt.cal(get_(aIdx)));}
    @Override public void updateReal_(int aIdx, IDoubleOperator1 aRealOpt) {setReal_(aIdx, aRealOpt.cal(getReal_(aIdx)));}
    @Override public void updateImag_(int aIdx, IDoubleOperator1 aImagOpt) {setImag_(aIdx, aImagOpt.cal(getImag_(aIdx)));}
    @Override public ComplexDouble getAndUpdate_(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        ComplexDouble oValue = get_(aIdx);
        set_(aIdx, aOpt.cal(new ComplexDouble(oValue))); // 用来防止意外的修改
        return oValue;
    }
    @Override public double getAndUpdateReal_(int aIdx, IDoubleOperator1 aRealOpt) {
        double tReal = getReal_(aIdx);
        setReal_(aIdx, aRealOpt.cal(tReal));
        return tReal;
    }
    @Override public double getAndUpdateImag_(int aIdx, IDoubleOperator1 aImagOpt) {
        double tImag = getImag_(aIdx);
        setImag_(aIdx, aImagOpt.cal(tImag));
        return tImag;
    }
    
    @Override public void add(int aIdx, IComplexDouble aDelta) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        add_(aIdx, aDelta);
    }
    @Override public void add(int aIdx, ComplexDouble aDelta) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        add_(aIdx, aDelta);
    }
    @Override public void add(int aIdx, double aDelta) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        add_(aIdx, aDelta);
    }
    @Override public void addImag(int aIdx, double aImag) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        addImag_(aIdx, aImag);
    }
    @Override public void update(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        update_(aIdx, aOpt);
    }
    @Override public void updateReal(int aIdx, IDoubleOperator1 aRealOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        updateReal_(aIdx, aRealOpt);
    }
    @Override public void updateImag(int aIdx, IDoubleOperator1 aImagOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        updateImag_(aIdx, aImagOpt);
    }
    @Override public ComplexDouble getAndUpdate(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndUpdate_(aIdx, aOpt);
    }
    @Override public double getAndUpdateReal(int aIdx, IDoubleOperator1 aRealOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndUpdateReal_(aIdx, aRealOpt);
    }
    @Override public double getAndUpdateImag(int aIdx, IDoubleOperator1 aImagOpt) {
        if (aIdx<0 || aIdx>=size()) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return getAndUpdateImag_(aIdx, aImagOpt);
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
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double getReal_(int aIdx) {return AbstractComplexVector.this.getReal(aIndices.get(aIdx));}
                    @Override public double getImag_(int aIdx) {return AbstractComplexVector.this.getImag(aIndices.get(aIdx));}
                    @Override public void setReal_(int aIdx, double aReal) {AbstractComplexVector.this.setReal(aIndices.get(aIdx), aReal);}
                    @Override public void setImag_(int aIdx, double aImag) {AbstractComplexVector.this.setImag(aIndices.get(aIdx), aImag);}
                    @Override public double getAndSetReal_(int aIdx, double aReal) {return AbstractComplexVector.this.getAndSetReal(aIndices.get(aIdx), aReal);}
                    @Override public double getAndSetImag_(int aIdx, double aImag) {return AbstractComplexVector.this.getAndSetImag(aIndices.get(aIdx), aImag);}
                    @Override public int size() {return aIndices.size();}
                };
            }
            @Override protected IComplexVector getA() {
                return new RefComplexVector() {
                    /** 对于全部切片，则不再需要二次边界检查 */
                    @Override public double getReal_(int aIdx) {return AbstractComplexVector.this.getReal_(aIdx);}
                    @Override public double getImag_(int aIdx) {return AbstractComplexVector.this.getImag_(aIdx);}
                    @Override public void setReal_(int aIdx, double aReal) {AbstractComplexVector.this.setReal_(aIdx, aReal);}
                    @Override public void setImag_(int aIdx, double aImag) {AbstractComplexVector.this.setImag_(aIdx, aImag);}
                    @Override public double getAndSetReal_(int aIdx, double aReal) {return AbstractComplexVector.this.getAndSetReal_(aIdx, aReal);}
                    @Override public double getAndSetImag_(int aIdx, double aImag) {return AbstractComplexVector.this.getAndSetImag_(aIdx, aImag);}
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
    public ComplexDouble get_(int aIdx) {return new ComplexDouble(getReal_(aIdx), getImag_(aIdx));}
    public abstract double getReal_(int aIdx);
    public abstract double getImag_(int aIdx);
    public void set_(int aIdx, IComplexDouble aValue) {setReal_(aIdx, aValue.real()); setImag_(aIdx, aValue.imag());}
    public void set_(int aIdx, ComplexDouble aValue) {setReal_(aIdx, aValue.mReal); setImag_(aIdx, aValue.mImag);}
    public void set_(int aIdx, double aValue) {setReal_(aIdx, aValue); setImag_(aIdx, 0.0);}
    public abstract void setReal_(int aIdx, double aReal);
    public abstract void setImag_(int aIdx, double aImag);
    public ComplexDouble getAndSet_(int aIdx, IComplexDouble aValue) {return new ComplexDouble(getAndSetReal_(aIdx, aValue.real()), getAndSetImag_(aIdx, aValue.imag()));}
    public ComplexDouble getAndSet_(int aIdx, ComplexDouble aValue) {return new ComplexDouble(getAndSetReal_(aIdx, aValue.mReal), getAndSetImag_(aIdx, aValue.mImag));}
    public ComplexDouble getAndSet_(int aIdx, double aValue) {return new ComplexDouble(getAndSetReal_(aIdx, aValue), getAndSetImag_(aIdx, 0.0));}
    public abstract double getAndSetReal_(int aIdx, double aReal);
    public abstract double getAndSetImag_(int aIdx, double aImag);
    
    public abstract int size();
    protected abstract IComplexVector newZeros_(int aSize);
    
    protected String toString_(double aReal, double aImag) {return Double.compare(aImag, 0.0)>=0 ? String.format("   %.4g + %.4gi", aReal, aImag) : String.format("   %.4g - %.4gi", aReal, -aImag);}
}
