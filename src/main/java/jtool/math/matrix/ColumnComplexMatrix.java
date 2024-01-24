package jtool.math.matrix;

import groovy.lang.Closure;
import jtool.code.functional.IDoubleBinaryConsumer;
import jtool.code.functional.IUnaryFullOperator;
import jtool.code.iterator.IComplexDoubleIterator;
import jtool.code.iterator.IComplexDoubleSetIterator;
import jtool.code.iterator.IDoubleIterator;
import jtool.code.iterator.IDoubleSetIterator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.operation.ARRAY;
import jtool.math.vector.ComplexVector;
import jtool.math.vector.ShiftComplexVector;
import jtool.math.vector.ShiftVector;
import jtool.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.*;

import static jtool.math.matrix.AbstractMatrix.rangeCheckCol;
import static jtool.math.matrix.AbstractMatrix.rangeCheckRow;


/**
 * @author liqa
 * <p> 矩阵一般实现，按照列排序 </p>
 */
public final class ColumnComplexMatrix extends BiDoubleArrayMatrix {
    /** 提供默认的创建 */
    public static ColumnComplexMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static ColumnComplexMatrix ones(int aRowNum, int aColNum) {
        double[][] tData = new double[2][aRowNum*aColNum];
        Arrays.fill(tData[0], 1.0);
        return new ColumnComplexMatrix(aRowNum, aColNum, tData);
    }
    public static ColumnComplexMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static ColumnComplexMatrix zeros(int aRowNum, int aColNum) {return new ColumnComplexMatrix(aRowNum, aColNum, new double[2][aRowNum*aColNum]);}
    
    
    private final int mRowNum;
    private final int mColNum;
    
    public ColumnComplexMatrix(int aRowNum, int aColNum, double[][] aData) {
        super(aData);
        mRowNum = aRowNum;
        mColNum = aColNum;
    }
    public ColumnComplexMatrix(int aRowNum, double[][] aData) {this(aRowNum, Math.min(aData[0].length, aData[1].length)/aRowNum, aData);}
    
    
    /** IComplexMatrix stuffs */
    @Override public ComplexDouble get(int aRow, int aCol) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); int tIdx = aRow + aCol*mRowNum; return new ComplexDouble(mData[0][tIdx], mData[1][tIdx]);}
    @Override public double getReal(int aRow, int aCol) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); return mData[0][aRow + aCol*mRowNum];}
    @Override public double getImag(int aRow, int aCol) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); return mData[1][aRow + aCol*mRowNum];}
    @Override public void set(int aRow, int aCol, IComplexDouble aValue) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); int tIdx = aRow + aCol*mRowNum; mData[0][tIdx] = aValue.real(); mData[1][tIdx] = aValue.imag();}
    @Override public void set(int aRow, int aCol, ComplexDouble aValue) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); int tIdx = aRow + aCol*mRowNum; mData[0][tIdx] = aValue.mReal; mData[1][tIdx] = aValue.mImag;}
    @Override public void set(int aRow, int aCol, double aValue) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); int tIdx = aRow + aCol*mRowNum; mData[0][tIdx] = aValue; mData[1][tIdx] = 0.0;}
    @Override public void setReal(int aRow, int aCol, double aReal) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); mData[0][aRow + aCol*mRowNum] = aReal;}
    @Override public void setImag(int aRow, int aCol, double aImag) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); mData[1][aRow + aCol*mRowNum] = aImag;}
    @Override public ComplexDouble getAndSet(int aRow, int aCol, IComplexDouble aValue) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); int tIdx = aRow + aCol*mRowNum; ComplexDouble oValue = new ComplexDouble(mData[0][tIdx], mData[1][tIdx]); mData[0][tIdx] = aValue.real(); mData[1][tIdx] = aValue.imag(); return oValue;}
    @Override public ComplexDouble getAndSet(int aRow, int aCol, ComplexDouble aValue) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); int tIdx = aRow + aCol*mRowNum; ComplexDouble oValue = new ComplexDouble(mData[0][tIdx], mData[1][tIdx]); mData[0][tIdx] = aValue.mReal; mData[1][tIdx] = aValue.mImag; return oValue;}
    @Override public ComplexDouble getAndSet(int aRow, int aCol, double aValue) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); int tIdx = aRow + aCol*mRowNum; ComplexDouble oValue = new ComplexDouble(mData[0][tIdx], mData[1][tIdx]); mData[0][tIdx] = aValue; mData[1][tIdx] = 0.0; return oValue;}
    @Override public double getAndSetReal(int aRow, int aCol, double aReal) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); int tIdx = aRow + aCol*mRowNum; double oReal = mData[0][tIdx]; mData[0][tIdx] = aReal; return oReal;}
    @Override public double getAndSetImag(int aRow, int aCol, double aImag) {rangeCheckRow(aRow, mRowNum); rangeCheckCol(aCol, mColNum); int tIdx = aRow + aCol*mRowNum; double oImag = mData[1][tIdx]; mData[1][tIdx] = aImag; return oImag;}
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mColNum;}
    
    @Override protected ColumnComplexMatrix newZeros_(int aRowNum, int aColNum) {return ColumnComplexMatrix.zeros(aRowNum, aColNum);}
    @Override public ColumnComplexMatrix copy() {return (ColumnComplexMatrix)super.copy();}
    
    @Override public ColumnComplexMatrix newShell() {return new ColumnComplexMatrix(mRowNum, mColNum, null);}
    @Override public double @Nullable[][] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 ColumnMatrix 并且行数相同才会返回 mData
        if (aObj instanceof ColumnComplexMatrix && ((ColumnComplexMatrix)aObj).mRowNum == mRowNum) return ((ColumnComplexMatrix)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，重写这个提高列向的索引速度 */
    @Override public ShiftComplexVector col(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new ShiftComplexVector(mRowNum, aCol*mRowNum, mData);
    }
    
    /** Optimize stuffs，列向展开的向量直接返回 */
    @Override public ComplexVector asVecCol() {return new ComplexVector(mRowNum*mColNum, mData);}
    
    /** Optimize stuffs，real()，imag() 直接返回 {@link ColumnMatrix} */
    @Override public ColumnMatrix real() {return new ColumnMatrix(mRowNum, mColNum, mData[0]);}
    @Override public ColumnMatrix imag() {return new ColumnMatrix(mRowNum, mColNum, mData[1]);}
    
    /** Optimize stuffs，引用转置直接返回 {@link RowComplexMatrix} */
    @Override public IComplexMatrixOperation operation() {
        return new BiDoubleArrayMatrixOperation_() {
            @Override public void fill(IComplexMatrixGetter aRHS) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int idx = 0;
                for (int col = 0; col < mColNum; ++col) for (int row = 0; row < mRowNum; ++row) {
                    IComplexDouble tValue = aRHS.get(row, col);
                    tRealData[idx] = tValue.real();
                    tImagData[idx] = tValue.imag();
                    ++idx;
                }
            }
            @Override public void fill(IMatrixGetter aRHS) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int idx = 0;
                for (int col = 0; col < mColNum; ++col) for (int row = 0; row < mRowNum; ++row) {
                    tRealData[idx] = aRHS.get(row, col);
                    tImagData[idx] = 0.0;
                    ++idx;
                }
            }
            @Override public void assignCol(Supplier<? extends IComplexDouble> aSup) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) {
                    IComplexDouble tValue = aSup.get();
                    tRealData[i] = tValue.real();
                    tImagData[i] = tValue.imag();
                }
            }
            @Override public void assignCol(DoubleSupplier aSup) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) {
                    tRealData[i] = aSup.getAsDouble();
                    tImagData[i] = 0.0;
                }
            }
            @Override public void forEachCol(Consumer<? super ComplexDouble> aCon) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) aCon.accept(new ComplexDouble(tRealData[i], tImagData[i]));
            }
            @Override public void forEachCol(IDoubleBinaryConsumer aCon) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int rEnd = mRowNum*mColNum;
                for (int i = 0; i < rEnd; ++i) aCon.accept(tRealData[i], tImagData[i]);
            }
            /** Groovy stuffs */
            @Override public void fill(Closure<?> aGroovyTask) {
                final double[] tRealData = mData[0];
                final double[] tImagData = mData[1];
                int idx = 0;
                for (int col = 0; col < mColNum; ++col) for (int row = 0; row < mRowNum; ++row) {
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
            @Override public void assignCol(Closure<?> aGroovyTask) {
                ARRAY.assign2This(internalData(), internalDataShift(), internalDataSize(), false, aGroovyTask);
            }
            
            @Override public RowComplexMatrix refTranspose() {
                return new RowComplexMatrix(mRowNum, mColNum, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void update(int aRow, int aCol, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealData[tIdx], tImagData[tIdx]));
        tRealData[tIdx] = tValue.real();
        tImagData[tIdx] = tValue.imag();
    }
    @Override public void updateReal(int aRow, int aCol,  DoubleUnaryOperator aRealOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        final double[] tRealData = mData[0];
        tRealData[tIdx] = aRealOpt.applyAsDouble(tRealData[tIdx]);
    }
    @Override public void updateImag(int aRow, int aCol, DoubleUnaryOperator aImagOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        final double[] tImagData = mData[1];
        tImagData[tIdx] = aImagOpt.applyAsDouble(tImagData[tIdx]);
    }
    @Override public ComplexDouble getAndUpdate(int aRow, int aCol, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        ComplexDouble oValue = new ComplexDouble(tRealData[tIdx], tImagData[tIdx]);
        IComplexDouble tValue = aOpt.apply(new ComplexDouble(oValue)); // 用来防止意外的修改
        tRealData[tIdx] = tValue.real();
        tImagData[tIdx] = tValue.imag();
        return oValue;
    }
    @Override public double getAndUpdateReal(int aRow, int aCol,DoubleUnaryOperator aRealOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        final double[] tRealData = mData[0];
        double oReal = tRealData[tIdx];
        tRealData[tIdx] = aRealOpt.applyAsDouble(oReal);
        return oReal;
    }
    @Override public double getAndUpdateImag(int aRow, int aCol,DoubleUnaryOperator aImagOpt) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aRow + aCol*mRowNum;
        final double[] tImagData = mData[1];
        double oImag = tImagData[tIdx];
        tImagData[tIdx] = aImagOpt.applyAsDouble(oImag);
        return oImag;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度 */
    @Override public IComplexDoubleIterator iteratorCol() {
        return new IComplexDoubleIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
        };
    }
    @Override public IComplexDoubleIterator iteratorRow() {
        return new IComplexDoubleIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = 0;
            private int mIdx = mRow, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
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
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
        };
    }
    @Override public IComplexDoubleIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new IComplexDoubleIterator() {
            private final int mEnd = (aCol+1)*mRowNum;
            private int mIdx = aCol*mRowNum, oIdx = -1;
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
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
        };
    }
    @Override public IComplexDoubleIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new IComplexDoubleIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aRow, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
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
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
        };
    }
    @Override public IComplexDoubleSetIterator setIteratorCol() {
        return new IComplexDoubleSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
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
            @Override public void setComplexDouble(double aReal, double aImag) {
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
    @Override public IComplexDoubleSetIterator setIteratorRow() {
        return new IComplexDoubleSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mRow = 0;
            private int mIdx = mRow, oIdx = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
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
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
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
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
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
            @Override public void setComplexDouble(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(IComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    mData[0][oIdx] = aValue.real();
                    mData[1][oIdx] = aValue.imag();
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(ComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    mData[0][oIdx] = aValue.mReal;
                    mData[1][oIdx] = aValue.mImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    mData[0][oIdx] = aValue;
                    mData[1][oIdx] = 0.0;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetReal(double aReal) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    mData[0][oIdx] = aReal;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetImag(double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    if (mIdx >= mSize) {++mRow; mIdx = mRow;}
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IComplexDoubleSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, mColNum);
        return new IComplexDoubleSetIterator() {
            private final int mEnd = (aCol+1)*mRowNum;
            private int mIdx = aCol*mRowNum, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
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
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
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
            @Override public void setComplexDouble(double aReal, double aImag) {
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
    @Override public IComplexDoubleSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new IComplexDoubleSetIterator() {
            private final int mSize = mRowNum * mColNum;
            private int mIdx = aRow, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
                    oIdx = mIdx; mIdx += mRowNum;
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
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
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
            @Override public void setComplexDouble(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(IComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    mData[0][oIdx] = aValue.real();
                    mData[1][oIdx] = aValue.imag();
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(ComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    mData[0][oIdx] = aValue.mReal;
                    mData[1][oIdx] = aValue.mImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    mData[0][oIdx] = aValue;
                    mData[1][oIdx] = 0.0;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetReal(double aReal) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    mData[0][oIdx] = aReal;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetImag(double aImag) {
                if (hasNext()) {
                    oIdx = mIdx; mIdx += mRowNum;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
