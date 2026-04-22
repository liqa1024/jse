package jse.math.matrix;

import groovy.lang.Closure;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.ComplexDoubleList;
import jse.code.functional.IDoubleBinaryConsumer;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IComplexDoubleIterator;
import jse.code.iterator.IComplexDoubleSetIterator;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.operation.ARRAY;
import jse.math.vector.*;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;


/**
 * 按照行排序的复数矩阵
 * @author liqa
 */
public class RowComplexMatrix extends BiDoubleArrayMatrix {
    /** 提供默认的创建 */
    public static RowComplexMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RowComplexMatrix ones(int aNumRows, int aNumCols) {
        double[][] tData = new double[2][aNumRows*aNumCols];
        Arrays.fill(tData[0], 1.0);
        return new RowComplexMatrix(aNumRows, aNumCols, tData);
    }
    public static RowComplexMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RowComplexMatrix zeros(int aNumRows, int aNumCols) {return new RowComplexMatrix(aNumRows, aNumCols, new double[2][aNumRows*aNumCols]);}
    
    /** 也提供 builder 方式的构建 */
    public static Builder builder(int aNumCols) {return new Builder(aNumCols);}
    public static Builder builder(int aNumCols, int aInitSize) {return new Builder(aNumCols, aInitSize);}
    public final static class Builder {
        private final static int DEFAULT_INIT_SIZE = 8;
        private ComplexDoubleList mData;
        private final int mNumCols;
        private Builder(int aNumCols) {mNumCols = aNumCols; mData = new ComplexDoubleList(Math.max(aNumCols, DEFAULT_INIT_SIZE));}
        private Builder(int aNumCols, int aInitSize) {mNumCols = aNumCols; mData = new ComplexDoubleList(Math.max(aNumCols, aInitSize));}
        
        public void addRow(IComplexVector aRow) {mData.addAll(aRow);}
        public void addRow(IVector aRow) {mData.addAll(aRow);}
        public void addRow(IComplexVectorGetter aRowGetter) {mData.addAll(mNumCols, aRowGetter);}
        public void addRow(IVectorGetter aRowGetter) {mData.addAll(mNumCols, aRowGetter);}
        public void trimToSize() {mData.trimToSize();}
        
        public RowComplexMatrix build() {
            ComplexDoubleList tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new RowComplexMatrix(tData.size()/mNumCols, mNumCols, tData.internalData());
        }
    }
    
    private final int mNumRows;
    private final int mNumCols;
    private final int mShift;
    
    public RowComplexMatrix(int aNumRows, int aNumCols, int aShift, double[][] aData) {
        super(aData);
        mNumRows = aNumRows;
        mNumCols = aNumCols;
        mShift = aShift;
    }
    public RowComplexMatrix(int aNumRows, int aNumCols, double[][] aData) {
        this(aNumRows, aNumCols, 0, aData);
    }
    public RowComplexMatrix(int aNumCols, double[][] aData) {
        this(Math.min(aData[0].length, aData[1].length)/aNumCols, aNumCols, aData);
    }
    
    
    /** IComplexMatrix stuffs */
    @Override public final ComplexDouble get(int aRow, int aCol) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        return new ComplexDouble(mData[0][tIdx], mData[1][tIdx]);
    }
    @Override public final double getReal(int aRow, int aCol) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        return mData[0][aCol + aRow*mNumCols + mShift];
    }
    @Override public final double getImag(int aRow, int aCol) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        return mData[1][aCol + aRow*mNumCols + mShift];
    }
    @Override public final void set(int aRow, int aCol, IComplexDouble aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        mData[0][tIdx] = aValue.real();
        mData[1][tIdx] = aValue.imag();
    }
    @Override public final void set(int aRow, int aCol, ComplexDouble aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        mData[0][tIdx] = aValue.mReal;
        mData[1][tIdx] = aValue.mImag;
    }
    @Override public final void set(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        mData[0][tIdx] = aValue;
        mData[1][tIdx] = 0.0;
    }
    @Override public final void set(int aRow, int aCol, double aReal, double aImag) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        mData[0][tIdx] = aReal;
        mData[1][tIdx] = aImag;
    }
    @Override public final void setReal(int aRow, int aCol, double aReal) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        mData[0][aCol + aRow*mNumCols + mShift] = aReal;
    }
    @Override public final void setImag(int aRow, int aCol, double aImag) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        mData[1][aCol + aRow*mNumCols + mShift] = aImag;
    }
    @Override public final ComplexDouble getAndSet(int aRow, int aCol, IComplexDouble aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        ComplexDouble oValue = new ComplexDouble(mData[0][tIdx], mData[1][tIdx]);
        mData[0][tIdx] = aValue.real();
        mData[1][tIdx] = aValue.imag();
        return oValue;
    }
    @Override public final ComplexDouble getAndSet(int aRow, int aCol, ComplexDouble aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        ComplexDouble oValue = new ComplexDouble(mData[0][tIdx], mData[1][tIdx]);
        mData[0][tIdx] = aValue.mReal;
        mData[1][tIdx] = aValue.mImag;
        return oValue;
    }
    @Override public final ComplexDouble getAndSet(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        ComplexDouble oValue = new ComplexDouble(mData[0][tIdx], mData[1][tIdx]);
        mData[0][tIdx] = aValue;
        mData[1][tIdx] = 0.0;
        return oValue;
    }
    @Override public final ComplexDouble getAndSet(int aRow, int aCol, double aReal, double aImag) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        ComplexDouble oValue = new ComplexDouble(mData[0][tIdx], mData[1][tIdx]);
        mData[0][tIdx] = aReal;
        mData[1][tIdx] = aImag;
        return oValue;
    }
    @Override public final double getAndSetReal(int aRow, int aCol, double aReal) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        double oReal = mData[0][tIdx];
        mData[0][tIdx] = aReal;
        return oReal;
    }
    @Override public final double getAndSetImag(int aRow, int aCol, double aImag) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        double oImag = mData[1][tIdx];
        mData[1][tIdx] = aImag;
        return oImag;
    }
    @Override public final int nrows() {return mNumRows;}
    @Override public final int ncols() {return mNumCols;}
    
    @Override protected RowComplexMatrix newZeros_(int aNumRows, int aNumCols) {return RowComplexMatrix.zeros(aNumRows, aNumCols);}
    @Override public RowComplexMatrix copy() {return (RowComplexMatrix)super.copy();}
    
    @Override public int internalDataSize() {return mNumRows*mNumCols;}
    @Override public int internalDataShift() {return mShift;}
    
    @Override public double @Nullable[][] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RowMatrix 并且列数相同才会返回 mData
        if (aObj instanceof RowComplexMatrix && ((RowComplexMatrix)aObj).mNumCols==mNumCols) return ((RowComplexMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public List<? extends ComplexVector> rows() {
        return new AbstractRandomAccessList<ComplexVector>() {
            @Override public int size() {return RowComplexMatrix.this.nrows();}
            @Override public ComplexVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public ComplexVector row(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new ComplexVector(mNumCols, aRow*mNumCols + mShift, mData);
    }
    
    /** Optimize stuffs，行向展开的向量直接返回 */
    @Override public ComplexVector asVecRow() {
        return new ComplexVector(mNumRows*mNumCols, mShift, mData);
    }
    
    /** Optimize stuffs，real()，imag() 直接返回 {@link RowMatrix} */
    @Override public final RowMatrix real() {
        return new RowMatrix(mNumRows, mNumCols, mShift, mData[0]);
    }
    @Override public final RowMatrix imag() {
        return new RowMatrix(mNumRows, mNumCols, mShift, mData[1]);
    }
    
    /** Optimize stuffs，引用转置直接返回 {@link ColumnComplexMatrix} */
    @Override public final IComplexMatrixOperation operation() {
        return new BiDoubleArrayMatrixOperation_() {
            @Override public void fill(IComplexMatrixGetter aRHS) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int idx = mShift;
                for (int row = 0; row < mNumRows; ++row) for (int col = 0; col < mNumCols; ++col) {
                    IComplexDouble tValue = aRHS.get(row, col);
                    tRealData[idx] = tValue.real();
                    tImagData[idx] = tValue.imag();
                    ++idx;
                }
            }
            @Override public void fill(IMatrixGetter aRHS) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int idx = mShift;
                for (int row = 0; row < mNumRows; ++row) for (int col = 0; col < mNumCols; ++col) {
                    tRealData[idx] = aRHS.get(row, col);
                    tImagData[idx] = 0.0;
                    ++idx;
                }
            }
            @Override public void assignRow(Supplier<? extends IComplexDouble> aSup) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) {
                    IComplexDouble tValue = aSup.get();
                    tRealData[i] = tValue.real();
                    tImagData[i] = tValue.imag();
                }
            }
            @Override public void assignRow(DoubleSupplier aSup) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) {
                    tRealData[i] = aSup.getAsDouble();
                    tImagData[i] = 0.0;
                }
            }
            @Override public void forEachRow(Consumer<? super ComplexDouble> aCon) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) aCon.accept(new ComplexDouble(tRealData[i], tImagData[i]));
            }
            @Override public void forEachRow(IDoubleBinaryConsumer aCon) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int rEnd = mNumRows*mNumCols + mShift;
                for (int i = mShift; i < rEnd; ++i) aCon.accept(tRealData[i], tImagData[i]);
            }
            /** Groovy stuffs */
            @Override public void fill(Closure<?> aGroovyTask) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int idx = mShift;
                for (int row = 0; row < mNumRows; ++row) for (int col = 0; col < mNumCols; ++col) {
                    // 直接先执行然后检测类型决定如何设置
                    Object tObj = aGroovyTask.call(row, col);
                    if (tObj instanceof IComplexDouble) {
                        IComplexDouble tValue = (IComplexDouble)tObj;
                        tRealData[idx] = tValue.real();
                        tImagData[idx] = tValue.imag();
                    } else
                    if (tObj instanceof Number) {
                        tRealData[idx] = ((Number)tObj).doubleValue();
                        tImagData[idx] = 0.0;
                    } else {
                        tRealData[idx] = Double.NaN;
                        tImagData[idx] = 0.0;
                    }
                    ++idx;
                }
            }
            @Override public void assignRow(Closure<?> aGroovyTask) {
                ARRAY.assign2This(internalData(), internalDataShift(), internalDataSize(), aGroovyTask);
            }
            
            @Override public ColumnComplexMatrix refTranspose() {
                return new ColumnComplexMatrix(mNumRows, mNumCols, mShift, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void update(int aRow, int aCol, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealData[tIdx], tImagData[tIdx]));
        tRealData[tIdx] = tValue.real();
        tImagData[tIdx] = tValue.imag();
    }
    @Override public final void updateReal(int aRow, int aCol,  DoubleUnaryOperator aRealOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        final double[] tRealData = mData[0];
        tRealData[tIdx] = aRealOpt.applyAsDouble(tRealData[tIdx]);
    }
    @Override public final void updateImag(int aRow, int aCol, DoubleUnaryOperator aImagOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        final double[] tImagData = mData[1];
        tImagData[tIdx] = aImagOpt.applyAsDouble(tImagData[tIdx]);
    }
    @Override public final ComplexDouble getAndUpdate(int aRow, int aCol, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        ComplexDouble oValue = new ComplexDouble(tRealData[tIdx], tImagData[tIdx]);
        IComplexDouble tValue = aOpt.apply(new ComplexDouble(oValue)); // 用来防止意外的修改
        tRealData[tIdx] = tValue.real();
        tImagData[tIdx] = tValue.imag();
        return oValue;
    }
    @Override public final double getAndUpdateReal(int aRow, int aCol,DoubleUnaryOperator aRealOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        final double[] tRealData = mData[0];
        double oReal = tRealData[tIdx];
        tRealData[tIdx] = aRealOpt.applyAsDouble(oReal);
        return oReal;
    }
    @Override public final double getAndUpdateImag(int aRow, int aCol,DoubleUnaryOperator aImagOpt) {
        rangeCheckRow(aRow, mNumRows);
        rangeCheckCol(aCol, mNumCols);
        int tIdx = aCol + aRow*mNumCols + mShift;
        final double[] tImagData = mData[1];
        double oImag = tImagData[tIdx];
        tImagData[tIdx] = aImagOpt.applyAsDouble(oImag);
        return oImag;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public final IComplexDoubleIterator iteratorCol() {
        return new IComplexDoubleIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mCol = 0;
            private int mIdx = mCol+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {
                nextOnly();
                return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);
            }
        };
    }
    @Override public final IComplexDoubleIterator iteratorRow() {
        return new IComplexDoubleIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {
                nextOnly();
                return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);
            }
        };
    }
    @Override public final IComplexDoubleIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IComplexDoubleIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = aCol+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {
                nextOnly();
                return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);
            }
        };
    }
    @Override public final IComplexDoubleIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IComplexDoubleIterator() {
            private final int mEnd = (aRow+1)*mNumCols + mShift;
            private int mIdx = aRow*mNumCols + mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {
                nextOnly();
                return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);
            }
        };
    }
    
    @Override public final IComplexDoubleSetIterator setIteratorCol() {
        return new IComplexDoubleSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mCol = 0;
            private int mIdx = mCol+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public void set(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            @Override public void setReal(double aReal) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
            }
            @Override public void setImag(double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[1][oIdx] = aImag;
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {
                nextOnly();
                return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);
            }
            @Override public void set(IComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.real();
                mData[1][oIdx] = aValue.imag();
            }
            @Override public void set(ComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.mReal;
                mData[1][oIdx] = aValue.mImag;
            }
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue;
                mData[1][oIdx] = 0.0;
            }
            @Override public void setRealImag(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(IComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                    mData[0][oIdx] = aValue.real();
                    mData[1][oIdx] = aValue.imag();
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(ComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                    mData[0][oIdx] = aValue.mReal;
                    mData[1][oIdx] = aValue.mImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                    mData[0][oIdx] = aValue;
                    mData[1][oIdx] = 0.0;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aReal, double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    mData[0][oIdx] = aReal;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetReal(double aReal) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                    mData[0][oIdx] = aReal;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetImag(double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    if (mIdx >= mEnd) {++mCol; mIdx = mCol+mShift;}
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IComplexDoubleSetIterator setIteratorRow() {
        return new IComplexDoubleSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            @Override public void setReal(double aReal) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
            }
            @Override public void setImag(double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[1][oIdx] = aImag;
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {
                nextOnly();
                return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);
            }
            @Override public void set(IComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.real();
                mData[1][oIdx] = aValue.imag();
            }
            @Override public void set(ComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.mReal;
                mData[1][oIdx] = aValue.mImag;
            }
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue;
                mData[1][oIdx] = 0.0;
            }
            @Override public void setRealImag(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(IComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aValue.real();
                    mData[1][oIdx] = aValue.imag();
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(ComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aValue.mReal;
                    mData[1][oIdx] = aValue.mImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aValue;
                    mData[1][oIdx] = 0.0;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aReal, double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aReal;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetReal(double aReal) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aReal;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetImag(double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IComplexDoubleSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mNumCols);
        return new IComplexDoubleSetIterator() {
            private final int mEnd = mNumRows*mNumCols + mShift;
            private int mIdx = aCol+mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            @Override public void setReal(double aReal) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
            }
            @Override public void setImag(double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[1][oIdx] = aImag;
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {
                nextOnly();
                return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);
            }
            @Override public void set(IComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.real();
                mData[1][oIdx] = aValue.imag();
            }
            @Override public void set(ComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.mReal;
                mData[1][oIdx] = aValue.mImag;
            }
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue;
                mData[1][oIdx] = 0.0;
            }
            @Override public void setRealImag(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(IComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    mData[0][oIdx] = aValue.real();
                    mData[1][oIdx] = aValue.imag();
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(ComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    mData[0][oIdx] = aValue.mReal;
                    mData[1][oIdx] = aValue.mImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    mData[0][oIdx] = aValue;
                    mData[1][oIdx] = 0.0;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aReal, double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    mData[0][oIdx] = aReal;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetReal(double aReal) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    mData[0][oIdx] = aReal;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetImag(double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mNumCols;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IComplexDoubleSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mNumRows);
        return new IComplexDoubleSetIterator() {
            private final int mEnd = (aRow+1)*mNumCols + mShift;
            private int mIdx = aRow*mNumCols + mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            @Override public void setReal(double aReal) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
            }
            @Override public void setImag(double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[1][oIdx] = aImag;
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {
                nextOnly();
                return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);
            }
            @Override public void set(IComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.real();
                mData[1][oIdx] = aValue.imag();
            }
            @Override public void set(ComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.mReal;
                mData[1][oIdx] = aValue.mImag;
            }
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue;
                mData[1][oIdx] = 0.0;
            }
            @Override public void setRealImag(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(IComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aValue.real();
                    mData[1][oIdx] = aValue.imag();
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(ComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aValue.mReal;
                    mData[1][oIdx] = aValue.mImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aValue;
                    mData[1][oIdx] = 0.0;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aReal, double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aReal;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetReal(double aReal) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[0][oIdx] = aReal;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetImag(double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; ++mIdx;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
