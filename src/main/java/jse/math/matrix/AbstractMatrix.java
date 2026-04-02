package jse.math.matrix;

import jep.NDArray;
import jse.cache.MatrixCache;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.ISlice;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetIterator;
import jse.code.iterator.IDoubleSetOnlyIterator;
import jse.math.vector.AbstractVector;
import jse.math.vector.IVector;
import jse.math.vector.RefVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

import static jse.code.CS.ALL;

public abstract class AbstractMatrix implements IMatrix {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d x %d Matrix:", nrows(), ncols()));
        for (IVector tRow : rows()) {
            rStr.append("\n");
            for (double tValue : tRow.iterable()) rStr.append(toString_(tValue));
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IDoubleIterator iteratorCol() {
        return new IDoubleIterator() {
            private final int mColNum = ncols();
            private final int mRowNum = nrows();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = get(mRow, mCol);
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleIterator iteratorRow() {
        return new IDoubleIterator() {
            private final int mColNum = ncols();
            private final int mRowNum = nrows();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = get(mRow, mCol);
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, ncols());
        return new IDoubleIterator() {
            private final int mRowNum = nrows();
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = get(mRow, aCol);
                    ++mRow;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, nrows());
        return new IDoubleIterator() {
            private final int mColNum = ncols();
            private int mCol = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = get(aRow, mCol);
                    ++mCol;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIteratorCol() {
        return new IDoubleSetIterator() {
            private final int mColNum = ncols();
            private final int mRowNum = nrows();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(double aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public double next() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                    return get(oRow, oCol);
                } else {
                    throw new NoSuchElementException();
                }
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
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                    AbstractMatrix.this.set(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIteratorRow() {
        return new IDoubleSetIterator() {
            private final int mColNum = ncols();
            private final int mRowNum = nrows();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(double aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public double next() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                    return get(oRow, oCol);
                } else {
                    throw new NoSuchElementException();
                }
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
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                    AbstractMatrix.this.set(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, ncols());
        return new IDoubleSetIterator() {
            private final int mRowNum = nrows();
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(double aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractMatrix.this.set(oRow, aCol, aValue);
            }
            @Override public double next() {
                if (hasNext()) {
                    oRow = mRow; ++mRow;
                    return get(oRow, aCol);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oRow = mRow; ++mRow;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oRow = mRow; ++mRow;
                    AbstractMatrix.this.set(oRow, aCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, nrows());
        return new IDoubleSetIterator() {
            private final int mColNum = ncols();
            private int mCol = 0, oCol = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(double aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractMatrix.this.set(aRow, oCol, aValue);
            }
            @Override public double next() {
                if (hasNext()) {
                    oCol = mCol; ++mCol;
                    return get(aRow, oCol);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; ++mCol;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oCol = mCol; ++mCol;
                    AbstractMatrix.this.set(aRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    
    /** 转换为其他类型 */
    @Override public List<List<Double>> asListCols() {return AbstractCollections.map(cols(), IVector::asList);}
    @Override public List<List<Double>> asListRows() {return AbstractCollections.map(rows(), IVector::asList);}
    @Override public IVector asVecCol() {
        return new RefVector() {
            private final int mRowNum = nrows(), mColNum = ncols();
            @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.get(aIdx%mRowNum, aIdx/mRowNum);}
            @Override public void set(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractMatrix.this.set(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.getAndSet(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IDoubleIterator iterator() {return iteratorCol();}
            @Override public IDoubleSetIterator setIterator() {return setIteratorCol();}
        };
    }
    @Override public IVector asVecRow() {
        return new RefVector() {
            private final int mRowNum = nrows(), mColNum = ncols();
            @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.get(aIdx/mColNum, aIdx%mColNum);}
            @Override public void set(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractMatrix.this.set(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.getAndSet(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IDoubleIterator iterator() {return iteratorRow();}
            @Override public IDoubleSetIterator setIterator() {return setIteratorRow();}
        };
    }
    
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NDArray<double[]> numpy() {
        final int tRowNum = nrows();
        final int tColNum = ncols();
        final int tSize = tRowNum*tColNum;
        double[] rData = new double[tSize];
        final IDoubleIterator it = iteratorRow();
        for (int i = 0; i < tSize; ++i) {
            rData[i] = it.next();
        }
        return new NDArray<>(rData, tRowNum, tColNum);
    }
    /** {@inheritDoc} */
    @Override public double[][] data() {
        final int tRowNum = nrows();
        final int tColNum = ncols();
        double[][] rMat = new double[tRowNum][tColNum];
        final IDoubleIterator it = iteratorRow();
        for (int row = 0; row < tRowNum; ++row) {
            double[] tRow = rMat[row];
            for (int col = 0; col < tColNum; ++col) tRow[col] = it.next();
        }
        return rMat;
    }
    @Override public ColumnMatrix toBufCol(boolean aAbort) {
        ColumnMatrix rBuf = MatrixCache.getMatCol(nrows(), ncols());
        if (aAbort) return rBuf;
        rBuf.fill(this);
        return rBuf;
    }
    @Override public RowMatrix toBufRow(boolean aAbort) {
        RowMatrix rBuf = MatrixCache.getMatRow(nrows(), ncols());
        if (aAbort) return rBuf;
        rBuf.fill(this);
        return rBuf;
    }
    @Override public void releaseBuf(@NotNull IMatrix aBuf, boolean aAbort) {
        if (!aAbort) fill(aBuf);
        MatrixCache.returnMat(aBuf);
    }
    
    /** 批量修改的接口，现在统一使用迭代器来填充 */
    @Override public final void fill(double aValue) {operation().fill(aValue);}
    @Override public final void fill(IMatrix aMatrix) {operation().fill(aMatrix);}
    @Override public final void fill(IMatrixGetter aMatrixGetter) {operation().fill(aMatrixGetter);}
    
    /** 同样这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    @Override public void fill(final double[][] aData) {
        final IDoubleSetOnlyIterator si = setIteratorRow();
        final int tRowNum = nrows();
        final int tColNum = ncols();
        for (int row = 0; row < tRowNum; ++row) {
            final double[] tRow = aData[row];
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRow[col]);
        }
    }
    @SuppressWarnings("unchecked")
    @Override public void fillWithRows(Iterable<?> aRows) {
        // 为了避免重复代码，这里直接使用 rows 来填充，毕竟这里速度也不关键
        final Iterator<?> tRowsIt = aRows.iterator();
        for (IVector row : rows()) {
            Object tRow = tRowsIt.next();
            if (tRow instanceof Iterable) {
                row.fill((Iterable<? extends Number>)tRow);
            } else
            if (tRow instanceof IVector) {
                row.fill((IVector)tRow);
            } else
            if (tRow instanceof double[]) {
                row.fill((double[])tRow);
            } else {
                throw new IllegalArgumentException("Type of Row Must be Collection<? extends Number>, IVector or double[]");
            }
        }
    }
    @SuppressWarnings("unchecked")
    @Override public void fillWithCols(Iterable<?> aCols) {
        // 为了避免重复代码，这里直接使用 cols 来填充，毕竟这里速度也不关键
        final Iterator<?> tColsIt = aCols.iterator();
        for (IVector col : cols()) {
            Object tCol = tColsIt.next();
            if (tCol instanceof Iterable) {
                col.fill((Iterable<? extends Number>)tCol);
            } else
            if (tCol instanceof IVector) {
                col.fill((IVector)tCol);
            } else
            if (tCol instanceof double[]) {
                col.fill((double[])tCol);
            } else {
                throw new IllegalArgumentException("Type of Column Must be Collection<? extends Number>, IVector or double[]");
            }
        }
    }
    
    @Override public final void assignCol(DoubleSupplier aSup) {operation().assignCol(aSup);}
    @Override public final void assignRow(DoubleSupplier aSup) {operation().assignRow(aSup);}
    @Override public final void forEachCol(DoubleConsumer aCon) {operation().forEachCol(aCon);}
    @Override public final void forEachRow(DoubleConsumer aCon) {operation().forEachRow(aCon);}
    
    static abstract class AbstractSize implements ISize {
        @Override public String toString() {return String.format("{nrows: %d, ncols: %d}", row(), col());}
    }
    @Override public ISize size() {
        return new AbstractSize() {
            @Override public int row() {return nrows();}
            @Override public int col() {return ncols();}
        };
    }
    
    public static void rangeCheckRow(int aRow, int aRowNum) {
        if (aRow<0 || aRow>=aRowNum) throw new IndexOutOfBoundsException("Row = " + aRow + ", RowNumber = " + aRowNum);
    }
    public static void rangeCheckCol(int aCol, int aColNum) {
        if (aCol<0 || aCol>=aColNum) throw new IndexOutOfBoundsException("Col = " + aCol + ", ColumnNumber = " + aColNum);
    }
    
    
    @Override public List<? extends IVector> rows() {
        return new AbstractRandomAccessList<IVector>() {
            @Override public int size() {return nrows();}
            @Override public IVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public IVector row(final int aRow) {
        rangeCheckRow(aRow, nrows());
        return new RefVector() {
            @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.get(aRow, aIdx);}
            @Override public void set(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractMatrix.this.set(aRow, aIdx, aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.getAndSet(aRow, aIdx, aValue);}
            @Override public int size() {return ncols();}
            @Override public IDoubleIterator iterator() {return iteratorRowAt(aRow);}
            @Override public IDoubleSetIterator setIterator() {return setIteratorRowAt(aRow);}
        };
    }
    @Override public List<? extends IVector> cols() {
        return new AbstractRandomAccessList<IVector>() {
            @Override public int size() {return ncols();}
            @Override public IVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public IVector col(final int aCol) {
        rangeCheckCol(aCol, ncols());
        return new RefVector() {
            @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.get(aIdx, aCol);}
            @Override public void set(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractMatrix.this.set(aIdx, aCol, aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.getAndSet(aIdx, aCol, aValue);}
            @Override public int size() {return nrows();}
            @Override public IDoubleIterator iterator() {return iteratorColAt(aCol);}
            @Override public IDoubleSetIterator setIterator() {return setIteratorColAt(aCol);}
        };
    }
    
    @Override public void update(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        double tValue = get(aRow, aCol);
        tValue = aOpt.applyAsDouble(tValue);
        set(aRow, aCol, tValue);
    }
    @Override public double getAndUpdate(int aRow, int aCol, DoubleUnaryOperator aOpt) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        double tValue = get(aRow, aCol);
        set(aRow, aCol, aOpt.applyAsDouble(tValue));
        return tValue;
    }
    
    
    @Override public IMatrix copy() {
        IMatrix rMatrix = newZeros_(nrows(), ncols());
        rMatrix.fill(this);
        return rMatrix;
    }
    
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    @Override public IMatrixSlicer slicer() {
        return new AbstractMatrixSlicer() {
            @Override protected IVector getIL(final int aSelectedRow, final ISlice aSelectedCols) {IVector rVector = newZerosVec_(aSelectedCols.size()); rVector.fill(refSlicer().get(aSelectedRow, aSelectedCols)); return rVector;}
            @Override protected IVector getLI(final ISlice aSelectedRows, final int aSelectedCol) {IVector rVector = newZerosVec_(aSelectedRows.size()); rVector.fill(refSlicer().get(aSelectedRows, aSelectedCol)); return rVector;}
            @Override protected IVector getIA(final int aSelectedRow) {IVector rVector = newZerosVec_(ncols()); rVector.fill(refSlicer().get(aSelectedRow, ALL)); return rVector;}
            @Override protected IVector getAI(final int aSelectedCol) {IVector rVector = newZerosVec_(nrows()); rVector.fill(refSlicer().get(ALL, aSelectedCol)); return rVector;}
            
            @Override protected IMatrix getLL(final ISlice aSelectedRows, final ISlice aSelectedCols) {IMatrix rMatrix = newZeros_(aSelectedRows.size(), aSelectedCols.size()); rMatrix.fill(refSlicer().get(aSelectedRows, aSelectedCols)); return rMatrix;}
            @Override protected IMatrix getLA(final ISlice aSelectedRows) {IMatrix rMatrix = newZeros_(aSelectedRows.size(), ncols()); rMatrix.fill(refSlicer().get(aSelectedRows, ALL)); return rMatrix;}
            @Override protected IMatrix getAL(final ISlice aSelectedCols) {IMatrix rMatrix = newZeros_(nrows()         , aSelectedCols.size()); rMatrix.fill(refSlicer().get(ALL, aSelectedCols)); return rMatrix;}
            @Override protected IMatrix getAA() {return copy();}
            
            @Override public IVector diag(final int aShift) {
                IVector rVector;
                if (aShift > 0) {
                    rVector = newZerosVec_(Math.min(nrows(), ncols()-aShift));
                    rVector.fill(i -> AbstractMatrix.this.get(i, i+aShift));
                } else
                if (aShift < 0) {
                    rVector = newZerosVec_(Math.min(nrows()+aShift, ncols()));
                    rVector.fill(i -> AbstractMatrix.this.get(i-aShift, i));
                } else {
                    rVector = newZerosVec_(Math.min(nrows(), ncols()));
                    rVector.fill(i -> AbstractMatrix.this.get(i, i));
                }
                return rVector;
            }
            
            @Override protected int thisRowNum_() {return nrows();}
            @Override protected int thisColNum_() {return ncols();}
        };
    }
    @Override public IMatrixSlicer refSlicer() {
        return new AbstractMatrixSlicer() {
            @Override protected IVector getIL(final int aSelectedRow, final ISlice aSelectedCols) {
                rangeCheckRow(aSelectedRow, nrows());
                return new RefVector() {
                    @Override public double get(int aIdx) {return AbstractMatrix.this.get(aSelectedRow, aSelectedCols.get(aIdx));}
                    @Override public void set(int aIdx, double aValue) {AbstractMatrix.this.set(aSelectedRow, aSelectedCols.get(aIdx), aValue);}
                    @Override public double getAndSet(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet(aSelectedRow, aSelectedCols.get(aIdx), aValue);}
                    @Override public int size() {return aSelectedCols.size();}
                };
            }
            @Override protected IVector getLI(final ISlice aSelectedRows, final int aSelectedCol) {
                rangeCheckCol(aSelectedCol, ncols());
                return new RefVector() {
                    @Override public double get(int aIdx) {return AbstractMatrix.this.get(aSelectedRows.get(aIdx), aSelectedCol);}
                    @Override public void set(int aIdx, double aValue) {AbstractMatrix.this.set(aSelectedRows.get(aIdx), aSelectedCol, aValue);}
                    @Override public double getAndSet(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet(aSelectedRows.get(aIdx), aSelectedCol, aValue);}
                    @Override public int size() {return aSelectedRows.size();}
                };
            }
            @Override protected IVector getIA(int aSelectedRow) {return row(aSelectedRow);}
            @Override protected IVector getAI(int aSelectedCol) {return col(aSelectedCol);}
            
            @Override protected IMatrix getLL(final ISlice aSelectedRows, final ISlice aSelectedCols) {
                return new RefMatrix() {
                    @Override public double get(int aRow, int aCol) {return AbstractMatrix.this.get(aSelectedRows.get(aRow), aSelectedCols.get(aCol));}
                    @Override public void set(int aRow, int aCol, double aValue) {AbstractMatrix.this.set(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public double getAndSet(int aRow, int aCol, double aValue) {return AbstractMatrix.this.getAndSet(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public int nrows() {return aSelectedRows.size();}
                    @Override public int ncols() {return aSelectedCols.size();}
                };
            }
            @Override protected IMatrix getLA(final ISlice aSelectedRows) {
                return new RefMatrix() {
                    @Override public double get(int aRow, int aCol) {return AbstractMatrix.this.get(aSelectedRows.get(aRow), aCol);}
                    @Override public void set(int aRow, int aCol, double aValue) {AbstractMatrix.this.set(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public double getAndSet(int aRow, int aCol, double aValue) {return AbstractMatrix.this.getAndSet(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public int nrows() {return aSelectedRows.size();}
                    @Override public int ncols() {return AbstractMatrix.this.ncols();}
                };
            }
            @Override protected IMatrix getAL(final ISlice aSelectedCols) {
                return new RefMatrix() {
                    @Override public double get(int aRow, int aCol) {return AbstractMatrix.this.get(aRow, aSelectedCols.get(aCol));}
                    @Override public void set(int aRow, int aCol, double aValue) {AbstractMatrix.this.set(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public double getAndSet(int aRow, int aCol, double aValue) {return AbstractMatrix.this.getAndSet(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public int nrows() {return AbstractMatrix.this.nrows();}
                    @Override public int ncols() {return aSelectedCols.size();}
                };
            }
            @Override protected IMatrix getAA() {
                return new RefMatrix() {
                    @Override public double get(int aRow, int aCol) {return AbstractMatrix.this.get(aRow, aCol);}
                    @Override public void set(int aRow, int aCol, double aValue) {AbstractMatrix.this.set(aRow, aCol, aValue);}
                    @Override public double getAndSet(int aRow, int aCol, double aValue) {return AbstractMatrix.this.getAndSet(aRow, aCol, aValue);}
                    @Override public int nrows() {return AbstractMatrix.this.nrows();}
                    @Override public int ncols() {return AbstractMatrix.this.ncols();}
                };
            }
            
            @Override public IVector diag(final int aShift) {
                if (aShift > 0) {
                    return new RefVector() {
                        @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.get(aIdx, aIdx+aShift);}
                        @Override public void set(int aIdx, double aValue)  {AbstractVector.rangeCheck(aIdx, size()); AbstractMatrix.this.set(aIdx, aIdx+aShift, aValue);}
                        @Override public double getAndSet(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.getAndSet(aIdx, aIdx+aShift, aValue);}
                        @Override public int size() {return Math.min(nrows(), ncols()-aShift);}
                    };
                } else
                if (aShift < 0) {
                    return new RefVector() {
                        @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.get(aIdx-aShift, aIdx);}
                        @Override public void set(int aIdx, double aValue)  {AbstractVector.rangeCheck(aIdx, size()); AbstractMatrix.this.set(aIdx-aShift, aIdx, aValue);}
                        @Override public double getAndSet(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.getAndSet(aIdx-aShift, aIdx, aValue);}
                        @Override public int size() {return Math.min(nrows()+aShift, ncols());}
                    };
                } else {
                    return new RefVector() {
                        @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.get(aIdx, aIdx);}
                        @Override public void set(int aIdx, double aValue)  {AbstractVector.rangeCheck(aIdx, size()); AbstractMatrix.this.set(aIdx, aIdx, aValue);}
                        @Override public double getAndSet(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractMatrix.this.getAndSet(aIdx, aIdx, aValue);}
                        @Override public int size() {return Math.min(nrows(), ncols());}
                    };
                }
            }
            
            @Override protected int thisRowNum_() {return nrows();}
            @Override protected int thisColNum_() {return ncols();}
        };
    }
    
    /** 矩阵的运算器 */
    @Override public IMatrixOperation operation() {
        return new AbstractMatrixOperation() {
            @Override protected IMatrix thisMatrix_() {return AbstractMatrix.this;}
            @Override protected IMatrix newMatrix_(int aRowNum, int aColNum) {return newZeros_(aRowNum, aColNum);}
            @Override protected IVector newVector_(int aSize) {return newZerosVec_(aSize);}
        };
    }
    
    
    /** Groovy 的部分，增加矩阵基本的运算操作 */
    @Override public final IMatrix plus         (double aRHS) {return operation().plus    (aRHS);}
    @Override public final IMatrix minus        (double aRHS) {return operation().minus   (aRHS);}
    @Override public final IMatrix multiply     (double aRHS) {return operation().multiply(aRHS);}
    @Override public final IMatrix div          (double aRHS) {return operation().div     (aRHS);}
    @Override public final IMatrix mod          (double aRHS) {return operation().mod     (aRHS);}
    
    @Override public final IMatrix plus         (IMatrix aRHS) {return operation().plus    (aRHS);}
    @Override public final IMatrix minus        (IMatrix aRHS) {return operation().minus   (aRHS);}
    @Override public final IMatrix multiply     (IMatrix aRHS) {return operation().multiply(aRHS);}
    @Override public final IMatrix div          (IMatrix aRHS) {return operation().div     (aRHS);}
    @Override public final IMatrix mod          (IMatrix aRHS) {return operation().mod     (aRHS);}
    
    @Override public final void plus2this       (double aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (double aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (double aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (double aRHS) {operation().div2this     (aRHS);}
    @Override public final void mod2this        (double aRHS) {operation().mod2this     (aRHS);}
    
    @Override public final void plus2this       (IMatrix aRHS) {operation().plus2this    (aRHS);}
    @Override public final void minus2this      (IMatrix aRHS) {operation().minus2this   (aRHS);}
    @Override public final void multiply2this   (IMatrix aRHS) {operation().multiply2this(aRHS);}
    @Override public final void div2this        (IMatrix aRHS) {operation().div2this     (aRHS);}
    @Override public final void mod2this        (IMatrix aRHS) {operation().mod2this     (aRHS);}
    
    @Override public final IMatrix abs() {return operation().abs();}
    @Override public final void abs2this() {operation().abs2this();}
    @Override public final IMatrix negative() {return operation().negative();}
    @Override public final void negative2this() {operation().negative2this();}
    
    /** stuff to override */
    @Override public abstract double get(int aRow, int aCol);
    @Override public abstract void set(int aRow, int aCol, double aValue);
    @Override public abstract double getAndSet(int aRow, int aCol, double aValue); // 返回修改前的值
    @Override public abstract int nrows();
    @Override public abstract int ncols();
    protected abstract IMatrix newZeros_(int aRowNum, int aColNum);
    protected IVector newZerosVec_(int aSize) {return Vector.zeros(aSize);}
    
    protected String toString_(double aValue) {return String.format("   %.4g", aValue);}
}
