package jse.math.matrix;

import jep.NDArray;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import jse.math.vector.AbstractVector;
import jse.math.vector.IIntVector;
import jse.math.vector.IntVector;
import jse.math.vector.RefIntVector;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;

public abstract class AbstractIntMatrix implements IIntMatrix {
    /** print */
    @Override public String toString() {
        final StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d x %d Integer Matrix:", nrows(), ncols()));
        for (IIntVector tRow : rows()) {
            rStr.append("\n");
            tRow.forEach(v -> rStr.append(toString_(v)));
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IIntIterator iteratorCol() {
        return new IIntIterator() {
            private final int mNumCols = ncols();
            private final int mNumRows = nrows();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = get(mRow, mCol);
                    ++mRow;
                    if (mRow == mNumRows) {mRow = 0; ++mCol;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntIterator iteratorRow() {
        return new IIntIterator() {
            private final int mNumCols = ncols();
            private final int mNumRows = nrows();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = get(mRow, mCol);
                    ++mCol;
                    if (mCol == mNumCols) {mCol = 0; ++mRow;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, ncols());
        return new IIntIterator() {
            private final int mNumRows = nrows();
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = get(mRow, aCol);
                    ++mRow;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, nrows());
        return new IIntIterator() {
            private final int mNumCols = ncols();
            private int mCol = 0;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = get(aRow, mCol);
                    ++mCol;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntSetIterator setIteratorCol() {
        return new IIntSetIterator() {
            private final int mNumCols = ncols();
            private final int mNumRows = nrows();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public void set(int aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractIntMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public int next() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mNumRows) {mRow = 0; ++mCol;}
                    return get(oRow, oCol);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mNumRows) {mRow = 0; ++mCol;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mNumRows) {mRow = 0; ++mCol;}
                    AbstractIntMatrix.this.set(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntSetIterator setIteratorRow() {
        return new IIntSetIterator() {
            private final int mNumCols = ncols();
            private final int mNumRows = nrows();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public void set(int aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractIntMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public int next() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mNumCols) {mCol = 0; ++mRow;}
                    return get(oRow, oCol);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mNumCols) {mCol = 0; ++mRow;}
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mNumCols) {mCol = 0; ++mRow;}
                    AbstractIntMatrix.this.set(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, ncols());
        return new IIntSetIterator() {
            private final int mNumRows = nrows();
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mNumRows;}
            @Override public void set(int aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractIntMatrix.this.set(oRow, aCol, aValue);
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oRow = mRow; ++mRow;
                    AbstractIntMatrix.this.set(oRow, aCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, nrows());
        return new IIntSetIterator() {
            private final int mNumCols = ncols();
            private int mCol = 0, oCol = -1;
            @Override public boolean hasNext() {return mCol < mNumCols;}
            @Override public void set(int aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractIntMatrix.this.set(aRow, oCol, aValue);
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oCol = mCol; ++mCol;
                    AbstractIntMatrix.this.set(aRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    
    /** 转换为其他类型 */
    @Override public List<List<Integer>> asListCols() {return AbstractCollections.map(cols(), IIntVector::asList);}
    @Override public List<List<Integer>> asListRows() {return AbstractCollections.map(rows(), IIntVector::asList);}
    @Override public IMatrix asMat() {
        return new RefMatrix() {
            @Override public double get(int aRow, int aCol) {return AbstractIntMatrix.this.get(aRow, aCol);}
            @Override public void set(int aRow, int aCol, double aValue) {AbstractIntMatrix.this.set(aRow, aCol, (int)aValue);}
            @Override public double getAndSet(int aRow, int aCol, double aValue) {return AbstractIntMatrix.this.getAndSet(aRow, aCol, (int)aValue);}
            @Override public int nrows() {return AbstractIntMatrix.this.nrows();}
            @Override public int ncols() {return AbstractIntMatrix.this.ncols();}
            @Override public IDoubleIterator iteratorCol() {return AbstractIntMatrix.this.iteratorCol().asDouble();}
            @Override public IDoubleIterator iteratorRow() {return AbstractIntMatrix.this.iteratorRow().asDouble();}
            @Override public IDoubleIterator iteratorColAt(int aCol) {return AbstractIntMatrix.this.iteratorColAt(aCol).asDouble();}
            @Override public IDoubleIterator iteratorRowAt(int aRow) {return AbstractIntMatrix.this.iteratorRowAt(aRow).asDouble();}
        };
    }
    @Override public IIntVector asVecCol() {
        return new RefIntVector() {
            private final int mNumRows = nrows(), mNumCols = ncols();
            @Override public int get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractIntMatrix.this.get(aIdx%mNumRows, aIdx/mNumRows);}
            @Override public void set(int aIdx, int aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractIntMatrix.this.set(aIdx%mNumRows, aIdx/mNumRows, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractIntMatrix.this.getAndSet(aIdx%mNumRows, aIdx/mNumRows, aValue);}
            @Override public int size() {return mNumRows * mNumCols;}
            @Override public IIntIterator iterator() {return iteratorCol();}
            @Override public IIntSetIterator setIterator() {return setIteratorCol();}
        };
    }
    @Override public IIntVector asVecRow() {
        return new RefIntVector() {
            private final int mNumRows = nrows(), mNumCols = ncols();
            @Override public int get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractIntMatrix.this.get(aIdx/mNumCols, aIdx%mNumCols);}
            @Override public void set(int aIdx, int aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractIntMatrix.this.set(aIdx/mNumCols, aIdx%mNumCols, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractIntMatrix.this.getAndSet(aIdx/mNumCols, aIdx%mNumCols, aValue);}
            @Override public int size() {return mNumRows * mNumCols;}
            @Override public IIntIterator iterator() {return iteratorRow();}
            @Override public IIntSetIterator setIterator() {return setIteratorRow();}
        };
    }
    
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NDArray<int[]> numpy() {
        final int tNumRows = nrows();
        final int tNumCols = ncols();
        final int tSize = tNumRows*tNumCols;
        int[] rData = new int[tSize];
        final IIntIterator it = iteratorRow();
        for (int i = 0; i < tSize; ++i) {
            rData[i] = it.next();
        }
        return new NDArray<>(rData, tNumRows, tNumCols);
    }
    /** {@inheritDoc} */
    @Override public int[][] data() {
        final int tNumRows = nrows();
        final int tNumCols = ncols();
        int[][] rMat = new int[tNumRows][tNumCols];
        final IIntIterator it = iteratorRow();
        for (int row = 0; row < tNumRows; ++row) {
            final int[] tRow = rMat[row];
            for (int col = 0; col < tNumCols; ++col) tRow[col] = it.next();
        }
        return rMat;
    }
    
    /** 批量修改的接口，现在统一使用迭代器来填充 */
    @Override public final void fill(int aValue) {operation().fill(aValue);}
    @Override public final void fill(IIntMatrix aMatrix) {operation().fill(aMatrix);}
    @Override public final void fill(IIntMatrixGetter aMatrixGetter) {operation().fill(aMatrixGetter);}
    
    /** 同样这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    @Override public void fill(final int[][] aData) {
        final IIntSetIterator si = setIteratorRow();
        final int tNumRows = nrows();
        final int tNumCols = ncols();
        for (int row = 0; row < tNumRows; ++row) {
            final int[] tRow = aData[row];
            for (int col = 0; col < tNumCols; ++col) si.nextAndSet(tRow[col]);
        }
    }
    @SuppressWarnings("unchecked")
    @Override public void fillWithRows(Iterable<?> aRows) {
        // 为了避免重复代码，这里直接使用 rows 来填充，毕竟这里速度也不关键
        final Iterator<?> tRowsIt = aRows.iterator();
        for (IIntVector row : rows()) {
            Object tRow = tRowsIt.next();
            if (tRow instanceof Iterable) {
                row.fill((Iterable<? extends Number>)tRow);
            } else
            if (tRow instanceof IIntVector) {
                row.fill((IIntVector)tRow);
            } else
            if (tRow instanceof int[]) {
                row.fill((int[])tRow);
            } else {
                throw new IllegalArgumentException(tRow.toString());
            }
        }
    }
    @SuppressWarnings("unchecked")
    @Override public void fillWithCols(Iterable<?> aCols) {
        // 为了避免重复代码，这里直接使用 cols 来填充，毕竟这里速度也不关键
        final Iterator<?> tColsIt = aCols.iterator();
        for (IIntVector col : cols()) {
            Object tCol = tColsIt.next();
            if (tCol instanceof Iterable) {
                col.fill((Iterable<? extends Number>)tCol);
            } else
            if (tCol instanceof IIntVector) {
                col.fill((IIntVector)tCol);
            } else
            if (tCol instanceof int[]) {
                col.fill((int[])tCol);
            } else {
                throw new IllegalArgumentException(tCol.toString());
            }
        }
    }
    
    @Override public final void assignCol(IntSupplier aSup) {operation().assignCol(aSup);}
    @Override public final void assignRow(IntSupplier aSup) {operation().assignRow(aSup);}
    @Override public final void forEachCol(IntConsumer aCon) {operation().forEachCol(aCon);}
    @Override public final void forEachRow(IntConsumer aCon) {operation().forEachRow(aCon);}
    
    
    @Override public IMatrix.ISize size() {
        return new AbstractMatrix.AbstractSize() {
            @Override public int row() {return nrows();}
            @Override public int col() {return ncols();}
        };
    }
    
    
    @Override public List<? extends IIntVector> rows() {
        return new AbstractRandomAccessList<IIntVector>() {
            @Override public int size() {return nrows();}
            @Override public IIntVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public IIntVector row(final int aRow) {
        rangeCheckRow(aRow, nrows());
        return new RefIntVector() {
            @Override public int get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractIntMatrix.this.get(aRow, aIdx);}
            @Override public void set(int aIdx, int aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractIntMatrix.this.set(aRow, aIdx, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractIntMatrix.this.getAndSet(aRow, aIdx, aValue);}
            @Override public int size() {return ncols();}
            @Override public IIntIterator iterator() {return iteratorRowAt(aRow);}
            @Override public IIntSetIterator setIterator() {return setIteratorRowAt(aRow);}
        };
    }
    @Override public List<? extends IIntVector> cols() {
        return new AbstractRandomAccessList<IIntVector>() {
            @Override public int size() {return ncols();}
            @Override public IIntVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public IIntVector col(final int aCol) {
        rangeCheckCol(aCol, ncols());
        return new RefIntVector() {
            @Override public int get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractIntMatrix.this.get(aIdx, aCol);}
            @Override public void set(int aIdx, int aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractIntMatrix.this.set(aIdx, aCol, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractIntMatrix.this.getAndSet(aIdx, aCol, aValue);}
            @Override public int size() {return nrows();}
            @Override public IIntIterator iterator() {return iteratorColAt(aCol);}
            @Override public IIntSetIterator setIterator() {return setIteratorColAt(aCol);}
        };
    }
    
    @Override public void update(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        int tValue = get(aRow, aCol);
        tValue = aOpt.applyAsInt(tValue);
        set(aRow, aCol, tValue);
    }
    @Override public int getAndUpdate(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        int tValue = get(aRow, aCol);
        set(aRow, aCol, aOpt.applyAsInt(tValue));
        return tValue;
    }
    
    
    @Override public IIntMatrix copy() {
        IIntMatrix rMatrix = newZeros_(nrows(), ncols());
        rMatrix.fill(this);
        return rMatrix;
    }
    
    /** 矩阵的运算器 */
    @Override public IIntMatrixOperation operation() {
        return new AbstractIntMatrixOperation() {
            @Override protected IIntMatrix thisMatrix_() {return AbstractIntMatrix.this;}
            @Override protected IIntMatrix newMatrix_(int aNumRows, int aNumCols) {return newZeros_(aNumRows, aNumCols);}
            @Override protected IIntVector newVector_(int aSize) {return newZerosVec_(aSize);}
        };
    }
    
    
    /** stuff to override */
    @Override public abstract int get(int aRow, int aCol);
    @Override public abstract void set(int aRow, int aCol, int aValue);
    @Override public abstract int getAndSet(int aRow, int aCol, int aValue); // 返回修改前的值
    @Override public abstract int nrows();
    @Override public abstract int ncols();
    protected abstract IIntMatrix newZeros_(int aNumRows, int aNumCols);
    protected IIntVector newZerosVec_(int aSize) {return IntVector.zeros(aSize);}
    
    protected String toString_(int aValue) {return " "+aValue;}
}
