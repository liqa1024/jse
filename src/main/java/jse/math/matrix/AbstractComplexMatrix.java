package jse.math.matrix;

import groovy.lang.Closure;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.functional.IDoubleBinaryConsumer;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IComplexDoubleIterator;
import jse.code.iterator.IComplexDoubleSetIterator;
import jse.code.iterator.IComplexDoubleSetOnlyIterator;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.vector.ComplexVector;
import jse.math.vector.IComplexVector;
import jse.math.vector.IVector;
import jse.math.vector.RefComplexVector;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Supplier;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;

public abstract class AbstractComplexMatrix implements IComplexMatrix {
    /** print */
    @Override public String toString() {
        final StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d x %d Complex Matrix:", rowNumber(), columnNumber()));
        List<IComplexVector> tRows = rows();
        for (IComplexVector tRow : tRows) {
            rStr.append("\n");
            tRow.forEach((real, imag) -> rStr.append(toString_(real, imag)));
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IComplexDoubleIterator iteratorCol() {
        return new IComplexDoubleIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oRow < 0) throw new IllegalStateException();
                return getReal(oRow, oCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag(oRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oRow, oCol);}
        };
    }
    @Override public IComplexDoubleIterator iteratorRow() {
        return new IComplexDoubleIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oRow < 0) throw new IllegalStateException();
                return getReal(oRow, oCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag(oRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oRow, oCol);}
        };
    }
    @Override public IComplexDoubleIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, columnNumber());
        return new IComplexDoubleIterator() {
            private final int mRowNum = rowNumber();
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oRow = mRow;
                    ++mRow;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oRow < 0) throw new IllegalStateException();
                return getReal(oRow, aCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag(oRow, aCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oRow, aCol);}
        };
    }
    @Override public IComplexDoubleIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, rowNumber());
        return new IComplexDoubleIterator() {
            private final int mColNum = columnNumber();
            private int mCol = 0, oCol = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol;
                    ++mCol;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oCol < 0) throw new IllegalStateException();
                return getReal(aRow, oCol);
            }
            @Override public double imag() {
                if (oCol < 0) throw new IllegalStateException();
                return getImag(aRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(aRow, oCol);}
        };
    }
    
    @Override public IComplexDoubleSetIterator setIteratorCol() {
        return new IComplexDoubleSetIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(double aReal, double aImag) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aReal, aImag);
            }
            @Override public void setReal(double aReal) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setReal(oRow, oCol, aReal);
            }
            @Override public void setImag(double aImag) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setImag(oRow, oCol, aImag);
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oRow < 0) throw new IllegalStateException();
                return getReal(oRow, oCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag(oRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oRow, oCol);}
            @Override public void set(IComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public void set(ComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public void set(double aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aValue);
            }
        };
    }
    @Override public IComplexDoubleSetIterator setIteratorRow() {
        return new IComplexDoubleSetIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(double aReal, double aImag) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aReal, aImag);
            }
            @Override public void setReal(double aReal) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setReal(oRow, oCol, aReal);
            }
            @Override public void setImag(double aImag) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setImag(oRow, oCol, aImag);
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oRow < 0) throw new IllegalStateException();
                return getReal(oRow, oCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag(oRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oRow, oCol);}
            @Override public void set(IComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public void set(ComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public void set(double aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, oCol, aValue);
            }
        };
    }
    @Override public IComplexDoubleSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, columnNumber());
        return new IComplexDoubleSetIterator() {
            private final int mRowNum = rowNumber();
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(double aReal, double aImag) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, aCol, aReal, aImag);
            }
            @Override public void setReal(double aReal) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setReal(oRow, aCol, aReal);
            }
            @Override public void setImag(double aImag) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setImag(oRow, aCol, aImag);
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oRow = mRow; ++mRow;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oRow < 0) throw new IllegalStateException();
                return getReal(oRow, aCol);
            }
            @Override public double imag() {
                if (oRow < 0) throw new IllegalStateException();
                return getImag(oRow, aCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(oRow, aCol);}
            @Override public void set(IComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, aCol, aValue);
            }
            @Override public void set(ComplexDouble aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, aCol, aValue);
            }
            @Override public void set(double aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(oRow, aCol, aValue);
            }
        };
    }
    @Override public IComplexDoubleSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, rowNumber());
        return new IComplexDoubleSetIterator() {
            private final int mColNum = columnNumber();
            private int mCol = 0, oCol = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(double aReal, double aImag) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(aRow, oCol, aReal, aImag);
            }
            @Override public void setReal(double aReal) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setReal(aRow, oCol, aReal);
            }
            @Override public void setImag(double aImag) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.setImag(aRow, oCol, aImag);
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; ++mCol;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (aRow < 0) throw new IllegalStateException();
                return getReal(aRow, oCol);
            }
            @Override public double imag() {
                if (aRow < 0) throw new IllegalStateException();
                return getImag(aRow, oCol);
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return get(aRow, oCol);}
            @Override public void set(IComplexDouble aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(aRow, oCol, aValue);
            }
            @Override public void set(ComplexDouble aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(aRow, oCol, aValue);
            }
            @Override public void set(double aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractComplexMatrix.this.set(aRow, oCol, aValue);
            }
        };
    }
    
    /** 转换为其他类型 */
    @Override public List<List<ComplexDouble>> asListCols() {return AbstractCollections.map(cols(), IComplexVector::asList);}
    @Override public List<List<ComplexDouble>> asListRows() {return AbstractCollections.map(rows(), IComplexVector::asList);}
    @Override public IComplexVector asVecCol() {
        return new RefComplexVector() {
            private final int mRowNum = rowNumber(), mColNum = columnNumber();
            @Override public double getReal(int aIdx) {return AbstractComplexMatrix.this.getReal(aIdx%mRowNum, aIdx/mRowNum);}
            @Override public double getImag(int aIdx) {return AbstractComplexMatrix.this.getImag(aIdx%mRowNum, aIdx/mRowNum);}
            @Override public void set(int aIdx, double aReal, double aImag) {AbstractComplexMatrix.this.set(aIdx%mRowNum, aIdx/mRowNum, aReal, aImag);}
            @Override public void setReal(int aIdx, double aReal) {AbstractComplexMatrix.this.setReal(aIdx%mRowNum, aIdx/mRowNum, aReal);}
            @Override public void setImag(int aIdx, double aImag) {AbstractComplexMatrix.this.setImag(aIdx%mRowNum, aIdx/mRowNum, aImag);}
            @Override public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {return AbstractComplexMatrix.this.getAndSet(aIdx%mRowNum, aIdx/mRowNum, aReal, aImag);}
            @Override public double getAndSetReal(int aIdx, double aReal) {return AbstractComplexMatrix.this.getAndSetReal(aIdx%mRowNum, aIdx/mRowNum, aReal);}
            @Override public double getAndSetImag(int aIdx, double aImag) {return AbstractComplexMatrix.this.getAndSetImag(aIdx%mRowNum, aIdx/mRowNum, aImag);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IComplexDoubleIterator iterator() {return iteratorCol();}
            @Override public IComplexDoubleSetIterator setIterator() {return setIteratorCol();}
            /** 这些接口用来减少索引的重复计算 */
            @Override public ComplexDouble get(int aIdx) {return AbstractComplexMatrix.this.get(aIdx%mRowNum, aIdx/mRowNum);}
            @Override public void set(int aIdx, IComplexDouble aValue) {AbstractComplexMatrix.this.set(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public void set(int aIdx, ComplexDouble aValue) {AbstractComplexMatrix.this.set(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public void set(int aIdx, double aValue) {AbstractComplexMatrix.this.set(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public ComplexDouble getAndSet(int aIdx, IComplexDouble aValue) {return AbstractComplexMatrix.this.getAndSet(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public ComplexDouble getAndSet(int aIdx, ComplexDouble aValue) {return AbstractComplexMatrix.this.getAndSet(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public ComplexDouble getAndSet(int aIdx, double aValue) {return AbstractComplexMatrix.this.getAndSet(aIdx%mRowNum, aIdx/mRowNum, aValue);}
        };
    }
    @Override public IComplexVector asVecRow() {
        return new RefComplexVector() {
            private final int mRowNum = rowNumber(), mColNum = columnNumber();
            @Override public double getReal(int aIdx) {return AbstractComplexMatrix.this.getReal(aIdx/mColNum, aIdx%mColNum);}
            @Override public double getImag(int aIdx) {return AbstractComplexMatrix.this.getImag(aIdx/mColNum, aIdx%mColNum);}
            @Override public void set(int aIdx, double aReal, double aImag) {AbstractComplexMatrix.this.set(aIdx/mColNum, aIdx%mColNum, aReal, aImag);}
            @Override public void setReal(int aIdx, double aReal) {AbstractComplexMatrix.this.setReal(aIdx/mColNum, aIdx%mColNum, aReal);}
            @Override public void setImag(int aIdx, double aImag) {AbstractComplexMatrix.this.setImag(aIdx/mColNum, aIdx%mColNum, aImag);}
            @Override public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {return AbstractComplexMatrix.this.getAndSet(aIdx/mColNum, aIdx%mColNum, aReal, aImag);}
            @Override public double getAndSetReal(int aIdx, double aReal) {return AbstractComplexMatrix.this.getAndSetReal(aIdx/mColNum, aIdx%mColNum, aReal);}
            @Override public double getAndSetImag(int aIdx, double aImag) {return AbstractComplexMatrix.this.getAndSetImag(aIdx/mColNum, aIdx%mColNum, aImag);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IComplexDoubleIterator iterator() {return iteratorRow();}
            @Override public IComplexDoubleSetIterator setIterator() {return setIteratorRow();}
            /** 这些接口用来减少索引的重复计算 */
            @Override public ComplexDouble get(int aIdx) {return AbstractComplexMatrix.this.get(aIdx/mColNum, aIdx%mColNum);}
            @Override public void set(int aIdx, IComplexDouble aValue) {AbstractComplexMatrix.this.set(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public void set(int aIdx, ComplexDouble aValue) {AbstractComplexMatrix.this.set(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public void set(int aIdx, double aValue) {AbstractComplexMatrix.this.set(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public ComplexDouble getAndSet(int aIdx, IComplexDouble aValue) {return AbstractComplexMatrix.this.getAndSet(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public ComplexDouble getAndSet(int aIdx, ComplexDouble aValue) {return AbstractComplexMatrix.this.getAndSet(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public ComplexDouble getAndSet(int aIdx, double aValue) {return AbstractComplexMatrix.this.getAndSet(aIdx/mColNum, aIdx%mColNum, aValue);}
        };
    }
    
    /** 获取实部和虚部 */
    @Override public IMatrix real() {
        return new RefMatrix() {
            /** 这里不再需要二次边界检查 */
            @Override public double get(int aRow, int aCol) {return AbstractComplexMatrix.this.getReal(aRow, aCol);}
            @Override public void set(int aRow, int aCol, double aValue) {AbstractComplexMatrix.this.setReal(aRow, aCol, aValue);}
            @Override public double getAndSet(int aRow, int aCol, double aValue) {return AbstractComplexMatrix.this.getAndSetReal(aRow, aCol, aValue);}
            @Override public int rowNumber() {return AbstractComplexMatrix.this.rowNumber();}
            @Override public int columnNumber() {return AbstractComplexMatrix.this.columnNumber();}
        };
    }
    @Override public IMatrix imag() {
        return new RefMatrix() {
            /** 这里不再需要二次边界检查 */
            @Override public double get(int aRow, int aCol) {return AbstractComplexMatrix.this.getImag(aRow, aCol);}
            @Override public void set(int aRow, int aCol, double aValue) {AbstractComplexMatrix.this.setImag(aRow, aCol, aValue);}
            @Override public double getAndSet(int aRow, int aCol, double aValue) {return AbstractComplexMatrix.this.getAndSetImag(aRow, aCol, aValue);}
            @Override public int rowNumber() {return AbstractComplexMatrix.this.rowNumber();}
            @Override public int columnNumber() {return AbstractComplexMatrix.this.columnNumber();}
        };
    }
    
    
    /** 转为兼容性更好的 double[][][]，默认直接使用 asListRow 转为 double[][] */
    @Override public double[][][] data() {
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        double[][][] rMat = new double[2][tRowNum][tColNum];
        double[][] rRealMat = rMat[0];
        double[][] rImagMat = rMat[1];
        final IComplexDoubleIterator it = iteratorRow();
        for (int row = 0; row < tRowNum; ++row) {
            double[] tRealRow = rRealMat[row];
            double[] tImagRow = rImagMat[row];
            for (int col = 0; col < tColNum; ++col) {
                it.nextOnly();
                tRealRow[col] = it.real();
                tImagRow[col] = it.imag();
            }
        }
        return rMat;
    }
    
    /** 批量修改的接口 */
    @Override public final void fill(IComplexDouble aValue) {operation().fill(aValue);}
    @Override public final void fill(double aValue) {operation().fill(aValue);}
    @Override public final void fill(IComplexMatrix aMatrix) {operation().fill(aMatrix);}
    @Override public final void fill(IMatrix aMatrix) {operation().fill(aMatrix);}
    @Override public final void fill(IComplexMatrixGetter aMatrixGetter) {operation().fill(aMatrixGetter);}
    @Override public final void fill(IMatrixGetter aMatrixGetter) {operation().fill(aMatrixGetter);}
    
    /** 同样这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    @Override public void fill(double[][][] aData) {
        final double[][] tRealData = aData[0];
        final double[][] tImagData = aData[1];
        final IComplexDoubleSetOnlyIterator si = setIteratorRow();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int row = 0; row < tRowNum; ++row) {
            final double[] tRealRow = tRealData[row];
            final double[] tImagRow = tImagData[row];
            for (int col = 0; col < tColNum; ++col) {
                si.nextAndSet(tRealRow[col], tImagRow[col]);
            }
        }
    }
    @Override public void fill(double[][] aData) {
        final IComplexDoubleSetOnlyIterator si = setIteratorRow();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int row = 0; row < tRowNum; ++row) {
            final double[] tRow = aData[row];
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRow[col]);
        }
    }
    @Override public void fillWithRows(Iterable<?> aRows) {
        // 为了避免重复代码，这里直接使用 rows 来填充，毕竟这里速度也不关键
        final Iterator<?> tRowsIt = aRows.iterator();
        for (IComplexVector row : rows()) {
            Object tRow = tRowsIt.next();
            if (tRow instanceof Iterable) {
                row.fill((Iterable<?>)tRow);
            } else
            if (tRow instanceof IComplexVector) {
                row.fill((IComplexVector)tRow);
            } else
            if (tRow instanceof IVector) {
                row.fill((IVector)tRow);
            } else
            if (tRow instanceof double[][]) {
                row.fill((double[][])tRow);
            } else
            if (tRow instanceof double[]) {
                row.fill((double[])tRow);
            } else {
                row.fill(Double.NaN);
            }
        }
    }
    @Override public void fillWithCols(Iterable<?> aCols) {
        // 为了避免重复代码，这里直接使用 cols 来填充，毕竟这里速度也不关键
        final Iterator<?> tColsIt = aCols.iterator();
        for (IComplexVector col : cols()) {
            Object tCol = tColsIt.next();
            if (tCol instanceof Iterable) {
                col.fill((Iterable<?>)tCol);
            } else
            if (tCol instanceof IComplexVector) {
                col.fill((IComplexVector)tCol);
            } else
            if (tCol instanceof IVector) {
                col.fill((IVector)tCol);
            } else
            if (tCol instanceof double[][]) {
                col.fill((double[][])tCol);
            } else
            if (tCol instanceof double[]) {
                col.fill((double[])tCol);
            } else {
                col.fill(Double.NaN);
            }
        }
    }
    
    @Override public final void assignCol(Supplier<? extends IComplexDouble> aSup) {operation().assignCol(aSup);}
    @Override public final void assignCol(DoubleSupplier aSup) {operation().assignCol(aSup);}
    @Override public final void assignRow(Supplier<? extends IComplexDouble> aSup) {operation().assignRow(aSup);}
    @Override public final void assignRow(DoubleSupplier aSup) {operation().assignRow(aSup);}
    @Override public final void forEachCol(Consumer<? super ComplexDouble> aCon) {operation().forEachCol(aCon);}
    @Override public final void forEachCol(IDoubleBinaryConsumer aCon) {operation().forEachCol(aCon);}
    @Override public final void forEachRow(Consumer<? super ComplexDouble> aCon) {operation().forEachRow(aCon);}
    @Override public final void forEachRow(IDoubleBinaryConsumer aCon) {operation().forEachRow(aCon);}
    
    /** Groovy stuff */
    @Override public final void fill(Closure<?> aGroovyTask) {operation().fill(aGroovyTask);}
    @Override public final void assignCol(Closure<?> aGroovyTask) {operation().assignCol(aGroovyTask);}
    @Override public final void assignRow(Closure<?> aGroovyTask) {operation().assignRow(aGroovyTask);}
    @Override public final void forEachCol(Closure<?> aGroovyTask) {operation().forEachCol(aGroovyTask);}
    @Override public final void forEachRow(Closure<?> aGroovyTask) {operation().forEachRow(aGroovyTask);}
    
    
    @Override public IMatrix.ISize size() {
        return new IMatrix.ISize() {
            @Override public int row() {return rowNumber();}
            @Override public int col() {return columnNumber();}
        };
    }
    
    
    @Override public List<IComplexVector> rows() {
        return new AbstractRandomAccessList<IComplexVector>() {
            @Override public int size() {return rowNumber();}
            @Override public IComplexVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public IComplexVector row(final int aRow) {
        rangeCheckRow(aRow, rowNumber());
        return new RefComplexVector() {
            /** 对于全部切片，则不再需要二次边界检查 */
            @Override public double getReal(int aIdx) {return AbstractComplexMatrix.this.getReal(aRow, aIdx);}
            @Override public double getImag(int aIdx) {return AbstractComplexMatrix.this.getImag(aRow, aIdx);}
            @Override public void set(int aIdx, double aReal, double aImag) {AbstractComplexMatrix.this.set(aRow, aIdx, aReal, aImag);}
            @Override public void setReal(int aIdx, double aReal) {AbstractComplexMatrix.this.setReal(aRow, aIdx, aReal);}
            @Override public void setImag(int aIdx, double aImag) {AbstractComplexMatrix.this.setImag(aRow, aIdx, aImag);}
            @Override public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {return AbstractComplexMatrix.this.getAndSet(aRow, aIdx, aReal, aImag);}
            @Override public double getAndSetReal(int aIdx, double aReal) {return AbstractComplexMatrix.this.getAndSetReal(aRow, aIdx, aReal);}
            @Override public double getAndSetImag(int aIdx, double aImag) {return AbstractComplexMatrix.this.getAndSetImag(aRow, aIdx, aImag);}
            @Override public int size() {return columnNumber();}
            @Override public IComplexDoubleIterator iterator() {return iteratorRowAt(aRow);}
            @Override public IComplexDoubleSetIterator setIterator() {return setIteratorRowAt(aRow);}
        };
    }
    @Override public List<IComplexVector> cols() {
        return new AbstractRandomAccessList<IComplexVector>() {
            @Override public int size() {return columnNumber();}
            @Override public IComplexVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public IComplexVector col(final int aCol) {
        rangeCheckCol(aCol, columnNumber());
        return new RefComplexVector() {
            /** 对于全部切片，则不再需要二次边界检查 */
            @Override public double getReal(int aIdx) {return AbstractComplexMatrix.this.getReal(aIdx, aCol);}
            @Override public double getImag(int aIdx) {return AbstractComplexMatrix.this.getImag(aIdx, aCol);}
            @Override public void set(int aIdx, double aReal, double aImag) {AbstractComplexMatrix.this.set(aIdx, aCol, aReal, aImag);}
            @Override public void setReal(int aIdx, double aReal) {AbstractComplexMatrix.this.setReal(aIdx, aCol, aReal);}
            @Override public void setImag(int aIdx, double aImag) {AbstractComplexMatrix.this.setImag(aIdx, aCol, aImag);}
            @Override public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {return AbstractComplexMatrix.this.getAndSet(aIdx, aCol, aReal, aImag);}
            @Override public double getAndSetReal(int aIdx, double aReal) {return AbstractComplexMatrix.this.getAndSetReal(aIdx, aCol, aReal);}
            @Override public double getAndSetImag(int aIdx, double aImag) {return AbstractComplexMatrix.this.getAndSetImag(aIdx, aCol, aImag);}
            @Override public int size() {return columnNumber();}
            @Override public IComplexDoubleIterator iterator() {return iteratorColAt(aCol);}
            @Override public IComplexDoubleSetIterator setIterator() {return setIteratorColAt(aCol);}
        };
    }
    
    @Override public void update(int aRow, int aCol, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheckRow(aRow, rowNumber());
        rangeCheckCol(aCol, columnNumber());
        set(aRow, aCol, aOpt.apply(get(aRow, aCol)));
    }
    @Override public void updateReal(int aRow, int aCol,  DoubleUnaryOperator aRealOpt) {
        rangeCheckRow(aRow, rowNumber());
        rangeCheckCol(aCol, columnNumber());
        setReal(aRow, aCol, aRealOpt.applyAsDouble(getReal(aRow, aCol)));
    }
    @Override public void updateImag(int aRow, int aCol, DoubleUnaryOperator aImagOpt) {
        rangeCheckRow(aRow, rowNumber());
        rangeCheckCol(aCol, columnNumber());
        setImag(aRow, aCol, aImagOpt.applyAsDouble(getImag(aRow, aCol)));
    }
    @Override public ComplexDouble getAndUpdate(int aRow, int aCol, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheckRow(aRow, rowNumber());
        rangeCheckCol(aCol, columnNumber());
        ComplexDouble oValue = get(aRow, aCol);
        set(aRow, aCol, aOpt.apply(new ComplexDouble(oValue))); // 用来防止意外的修改
        return oValue;
    }
    @Override public double getAndUpdateReal(int aRow, int aCol,DoubleUnaryOperator aRealOpt) {
        rangeCheckRow(aRow, rowNumber());
        rangeCheckCol(aCol, columnNumber());
        double tReal = getReal(aRow, aCol);
        setReal(aRow, aCol, aRealOpt.applyAsDouble(tReal));
        return tReal;
    }
    @Override public double getAndUpdateImag(int aRow, int aCol,DoubleUnaryOperator aImagOpt) {
        rangeCheckRow(aRow, rowNumber());
        rangeCheckCol(aCol, columnNumber());
        double tImag = getImag(aRow, aCol);
        setImag(aRow, aCol, aImagOpt.applyAsDouble(tImag));
        return tImag;
    }
    
    
    @Override public IComplexMatrix copy() {
        IComplexMatrix rMatrix = newZeros_(rowNumber(), columnNumber());
        rMatrix.fill(this);
        return rMatrix;
    }
    
    /** 矩阵的运算器 */
    @Override public IComplexMatrixOperation operation() {
        return new AbstractComplexMatrixOperation() {
            @Override protected IComplexMatrix thisMatrix_() {return AbstractComplexMatrix.this;}
            @Override protected IComplexMatrix newMatrix_(int aRowNum, int aColNum) {return newZeros_(aRowNum, aColNum);}
            @Override protected IComplexVector newVector_(int aSize) {return newZerosVec_(aSize);}
        };
    }
    
    /** Groovy 的部分，增加向量基本的运算操作 */
    @Override public final IComplexMatrix plus      (IComplexDouble aRHS) {return operation().plus    (aRHS);}
    @Override public final IComplexMatrix minus     (IComplexDouble aRHS) {return operation().minus   (aRHS);}
    @Override public final IComplexMatrix multiply  (IComplexDouble aRHS) {return operation().multiply(aRHS);}
    @Override public final IComplexMatrix div       (IComplexDouble aRHS) {return operation().div     (aRHS);}
    @Override public final IComplexMatrix plus      (double         aRHS) {return operation().plus    (aRHS);}
    @Override public final IComplexMatrix minus     (double         aRHS) {return operation().minus   (aRHS);}
    @Override public final IComplexMatrix multiply  (double         aRHS) {return operation().multiply(aRHS);}
    @Override public final IComplexMatrix div       (double         aRHS) {return operation().div     (aRHS);}
    
    @Override public final IComplexMatrix plus      (IComplexMatrix aRHS) {return operation().plus    (aRHS);}
    @Override public final IComplexMatrix minus     (IComplexMatrix aRHS) {return operation().minus   (aRHS);}
    @Override public final IComplexMatrix multiply  (IComplexMatrix aRHS) {return operation().multiply(aRHS);}
    @Override public final IComplexMatrix div       (IComplexMatrix aRHS) {return operation().div     (aRHS);}
    @Override public final IComplexMatrix plus      (IMatrix        aRHS) {return operation().plus    (aRHS);}
    @Override public final IComplexMatrix minus     (IMatrix        aRHS) {return operation().minus   (aRHS);}
    @Override public final IComplexMatrix multiply  (IMatrix        aRHS) {return operation().multiply(aRHS);}
    @Override public final IComplexMatrix div       (IMatrix        aRHS) {return operation().div     (aRHS);}
    
    @Override public final void plus2this       (IComplexDouble aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (IComplexDouble aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (IComplexDouble aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (IComplexDouble aRHS) {operation().div2this     (aRHS);}
    @Override public final void plus2this       (double         aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (double         aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (double         aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (double         aRHS) {operation().div2this     (aRHS);}
    
    @Override public final void plus2this       (IComplexMatrix aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (IComplexMatrix aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (IComplexMatrix aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (IComplexMatrix aRHS) {operation().div2this     (aRHS);}
    @Override public final void plus2this       (IMatrix        aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (IMatrix        aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (IMatrix        aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (IMatrix        aRHS) {operation().div2this     (aRHS);}
    
    @Override public final IComplexMatrix negative() {return operation().negative();}
    @Override public final void negative2this() {operation().negative2this();}
    
    
    /** stuff to override */
    public ComplexDouble get(int aRow, int aCol) {return new ComplexDouble(getReal(aRow, aCol), getImag(aRow, aCol));}
    public abstract double getReal(int aRow, int aCol);
    public abstract double getImag(int aRow, int aCol);
    public void set(int aRow, int aCol, IComplexDouble aValue) {set(aRow, aCol, aValue.real(), aValue.imag());}
    public void set(int aRow, int aCol, ComplexDouble aValue) {set(aRow, aCol, aValue.mReal, aValue.mImag);}
    public void set(int aRow, int aCol, double aValue) {set(aRow, aCol, aValue, 0.0);}
    public abstract void set(int aRow, int aCol, double aReal, double aImag);
    public abstract void setReal(int aRow, int aCol, double aReal);
    public abstract void setImag(int aRow, int aCol, double aImag);
    public ComplexDouble getAndSet(int aRow, int aCol, IComplexDouble aValue) {return getAndSet(aRow, aCol, aValue.real(), aValue.imag());}
    public ComplexDouble getAndSet(int aRow, int aCol, ComplexDouble aValue) {return getAndSet(aRow, aCol, aValue.mReal, aValue.mImag);}
    public ComplexDouble getAndSet(int aRow, int aCol, double aValue) {return getAndSet(aRow, aCol, aValue, 0.0);}
    public abstract ComplexDouble getAndSet(int aRow, int aCol, double aReal, double aImag);
    public abstract double getAndSetReal(int aRow, int aCol, double aReal);
    public abstract double getAndSetImag(int aRow, int aCol, double aImag);
    public abstract int rowNumber();
    public abstract int columnNumber();
    protected abstract IComplexMatrix newZeros_(int aRowNum, int aColNum);
    protected IComplexVector newZerosVec_(int aSize) {return ComplexVector.zeros(aSize);}
    
    protected String toString_(double aReal, double aImag) {return Double.compare(aImag, 0.0)>=0 ? String.format("   %.4g + %.4gi", aReal, aImag) : String.format("   %.4g - %.4gi", aReal, -aImag);}
}
