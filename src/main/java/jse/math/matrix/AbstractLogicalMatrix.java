package jse.math.matrix;

import jep.NDArray;
import jse.code.collection.AbstractCollections;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.functional.IBooleanConsumer;
import jse.code.functional.IBooleanUnaryOperator;
import jse.code.iterator.*;
import jse.math.vector.*;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.BooleanSupplier;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;

public abstract class AbstractLogicalMatrix implements ILogicalMatrix {
    /** print */
    @Override public String toString() {
        final StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d x %d Logical Matrix:", nrows(), ncols()));
        for (ILogicalVector tRow : rows()) {
            rStr.append("\n");
            tRow.forEach(v -> rStr.append(toString_(v)));
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IBooleanIterator iteratorCol() {
        return new IBooleanIterator() {
            private final int mColNum = ncols();
            private final int mRowNum = nrows();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = get(mRow, mCol);
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IBooleanIterator iteratorRow() {
        return new IBooleanIterator() {
            private final int mColNum = ncols();
            private final int mRowNum = nrows();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = get(mRow, mCol);
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IBooleanIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, ncols());
        return new IBooleanIterator() {
            private final int mRowNum = nrows();
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = get(mRow, aCol);
                    ++mRow;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IBooleanIterator iteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, nrows());
        return new IBooleanIterator() {
            private final int mColNum = ncols();
            private int mCol = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = get(aRow, mCol);
                    ++mCol;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IBooleanSetIterator setIteratorCol() {
        return new IBooleanSetIterator() {
            private final int mColNum = ncols();
            private final int mRowNum = nrows();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(boolean aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractLogicalMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                    AbstractLogicalMatrix.this.set(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IBooleanSetIterator setIteratorRow() {
        return new IBooleanSetIterator() {
            private final int mColNum = ncols();
            private final int mRowNum = nrows();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(boolean aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractLogicalMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                    AbstractLogicalMatrix.this.set(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IBooleanSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, ncols());
        return new IBooleanSetIterator() {
            private final int mRowNum = nrows();
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(boolean aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractLogicalMatrix.this.set(oRow, aCol, aValue);
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
                if (hasNext()) {
                    oRow = mRow; ++mRow;
                    AbstractLogicalMatrix.this.set(oRow, aCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IBooleanSetIterator setIteratorRowAt(final int aRow) {
        rangeCheckRow(aRow, nrows());
        return new IBooleanSetIterator() {
            private final int mColNum = ncols();
            private int mCol = 0, oCol = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(boolean aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractLogicalMatrix.this.set(aRow, oCol, aValue);
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
                if (hasNext()) {
                    oCol = mCol; ++mCol;
                    AbstractLogicalMatrix.this.set(aRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    
    /** 转换为其他类型 */
    @Override public List<List<Boolean>> asListCols() {return AbstractCollections.map(cols(), ILogicalVector::asList);}
    @Override public List<List<Boolean>> asListRows() {return AbstractCollections.map(rows(), ILogicalVector::asList);}
    @Override public IMatrix asMat() {
        return new RefMatrix() {
            @Override public double get(int aRow, int aCol) {return AbstractLogicalMatrix.this.get(aRow, aCol) ? 1.0 : 0.0;}
            @Override public void set(int aRow, int aCol, double aValue) {AbstractLogicalMatrix.this.set(aRow, aCol, aValue!=0.0);}
            @Override public double getAndSet(int aRow, int aCol, double aValue) {return AbstractLogicalMatrix.this.getAndSet(aRow, aCol, aValue!=0.0) ? 1.0 : 0.0;}
            @Override public int nrows() {return AbstractLogicalMatrix.this.nrows();}
            @Override public int ncols() {return AbstractLogicalMatrix.this.ncols();}
            @Override public IDoubleIterator iteratorCol() {return AbstractLogicalMatrix.this.iteratorCol().asDouble();}
            @Override public IDoubleIterator iteratorRow() {return AbstractLogicalMatrix.this.iteratorRow().asDouble();}
            @Override public IDoubleIterator iteratorColAt(int aCol) {return AbstractLogicalMatrix.this.iteratorColAt(aCol).asDouble();}
            @Override public IDoubleIterator iteratorRowAt(int aRow) {return AbstractLogicalMatrix.this.iteratorRowAt(aRow).asDouble();}
        };
    }
    @Override public IIntMatrix asIntMat() {
        return new RefIntMatrix() {
            @Override public int get(int aRow, int aCol) {return AbstractLogicalMatrix.this.get(aRow, aCol) ? 1 : 0;}
            @Override public void set(int aRow, int aCol, int aValue) {AbstractLogicalMatrix.this.set(aRow, aCol, aValue!=0);}
            @Override public int getAndSet(int aRow, int aCol, int aValue) {return AbstractLogicalMatrix.this.getAndSet(aRow, aCol, aValue!=0) ? 1 : 0;}
            @Override public int nrows() {return AbstractLogicalMatrix.this.nrows();}
            @Override public int ncols() {return AbstractLogicalMatrix.this.ncols();}
            @Override public IIntIterator iteratorCol() {return AbstractLogicalMatrix.this.iteratorCol().asInt();}
            @Override public IIntIterator iteratorRow() {return AbstractLogicalMatrix.this.iteratorRow().asInt();}
            @Override public IIntIterator iteratorColAt(int aCol) {return AbstractLogicalMatrix.this.iteratorColAt(aCol).asInt();}
            @Override public IIntIterator iteratorRowAt(int aRow) {return AbstractLogicalMatrix.this.iteratorRowAt(aRow).asInt();}
        };
    }
    @Override public ILogicalVector asVecCol() {
        return new RefLogicalVector() {
            private final int mRowNum = nrows(), mColNum = ncols();
            @Override public boolean get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractLogicalMatrix.this.get(aIdx%mRowNum, aIdx/mRowNum);}
            @Override public void set(int aIdx, boolean aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractLogicalMatrix.this.set(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public boolean getAndSet(int aIdx, boolean aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractLogicalMatrix.this.getAndSet(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IBooleanIterator iterator() {return iteratorCol();}
            @Override public IBooleanSetIterator setIterator() {return setIteratorCol();}
        };
    }
    @Override public ILogicalVector asVecRow() {
        return new RefLogicalVector() {
            private final int mRowNum = nrows(), mColNum = ncols();
            @Override public boolean get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractLogicalMatrix.this.get(aIdx/mColNum, aIdx%mColNum);}
            @Override public void set(int aIdx, boolean aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractLogicalMatrix.this.set(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public boolean getAndSet(int aIdx, boolean aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractLogicalMatrix.this.getAndSet(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IBooleanIterator iterator() {return iteratorRow();}
            @Override public IBooleanSetIterator setIterator() {return setIteratorRow();}
        };
    }
    
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NDArray<boolean[]> numpy() {
        final int tRowNum = nrows();
        final int tColNum = ncols();
        final int tSize = tRowNum*tColNum;
        boolean[] rData = new boolean[tSize];
        final IBooleanIterator it = iteratorRow();
        for (int i = 0; i < tSize; ++i) {
            rData[i] = it.next();
        }
        return new NDArray<>(rData, tRowNum, tColNum);
    }
    /** {@inheritDoc} */
    @Override public boolean[][] data() {
        final int tRowNum = nrows();
        final int tColNum = ncols();
        boolean[][] rMat = new boolean[tRowNum][tColNum];
        final IBooleanIterator it = iteratorRow();
        for (int row = 0; row < tRowNum; ++row) {
            final boolean[] tRow = rMat[row];
            for (int col = 0; col < tColNum; ++col) tRow[col] = it.next();
        }
        return rMat;
    }
    
    /** 批量修改的接口，现在统一使用迭代器来填充 */
    @Override public final void fill(boolean aValue) {operation().fill(aValue);}
    @Override public final void fill(ILogicalMatrix aMatrix) {operation().fill(aMatrix);}
    @Override public final void fill(ILogicalMatrixGetter aMatrixGetter) {operation().fill(aMatrixGetter);}
    
    /** 同样这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    @Override public void fill(final boolean[][] aData) {
        final IBooleanSetIterator si = setIteratorRow();
        final int tRowNum = nrows();
        final int tColNum = ncols();
        for (int row = 0; row < tRowNum; ++row) {
            final boolean[] tRow = aData[row];
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRow[col]);
        }
    }
    @SuppressWarnings("unchecked")
    @Override public void fillWithRows(Iterable<?> aRows) {
        // 为了避免重复代码，这里直接使用 rows 来填充，毕竟这里速度也不关键
        final Iterator<?> tRowsIt = aRows.iterator();
        for (ILogicalVector row : rows()) {
            Object tRow = tRowsIt.next();
            if (tRow instanceof Iterable) {
                row.fill((Iterable<Boolean>)tRow);
            } else
            if (tRow instanceof ILogicalVector) {
                row.fill((ILogicalVector)tRow);
            } else
            if (tRow instanceof boolean[]) {
                row.fill((boolean[])tRow);
            } else {
                throw new IllegalArgumentException(tRow.toString());
            }
        }
    }
    @SuppressWarnings("unchecked")
    @Override public void fillWithCols(Iterable<?> aCols) {
        // 为了避免重复代码，这里直接使用 cols 来填充，毕竟这里速度也不关键
        final Iterator<?> tColsIt = aCols.iterator();
        for (ILogicalVector col : cols()) {
            Object tCol = tColsIt.next();
            if (tCol instanceof Iterable) {
                col.fill((Iterable<Boolean>)tCol);
            } else
            if (tCol instanceof ILogicalVector) {
                col.fill((ILogicalVector)tCol);
            } else
            if (tCol instanceof boolean[]) {
                col.fill((boolean[])tCol);
            } else {
                throw new IllegalArgumentException(tCol.toString());
            }
        }
    }
    
    @Override public final void assignCol(BooleanSupplier aSup) {operation().assignCol(aSup);}
    @Override public final void assignRow(BooleanSupplier aSup) {operation().assignRow(aSup);}
    @Override public final void forEachCol(IBooleanConsumer aCon) {operation().forEachCol(aCon);}
    @Override public final void forEachRow(IBooleanConsumer aCon) {operation().forEachRow(aCon);}
    
    
    @Override public IMatrix.ISize size() {
        return new AbstractMatrix.AbstractSize() {
            @Override public int row() {return nrows();}
            @Override public int col() {return ncols();}
        };
    }
    
    
    @Override public List<? extends ILogicalVector> rows() {
        return new AbstractRandomAccessList<ILogicalVector>() {
            @Override public int size() {return nrows();}
            @Override public ILogicalVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public ILogicalVector row(final int aRow) {
        rangeCheckRow(aRow, nrows());
        return new RefLogicalVector() {
            @Override public boolean get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractLogicalMatrix.this.get(aRow, aIdx);}
            @Override public void set(int aIdx, boolean aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractLogicalMatrix.this.set(aRow, aIdx, aValue);}
            @Override public boolean getAndSet(int aIdx, boolean aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractLogicalMatrix.this.getAndSet(aRow, aIdx, aValue);}
            @Override public int size() {return ncols();}
            @Override public IBooleanIterator iterator() {return iteratorRowAt(aRow);}
            @Override public IBooleanSetIterator setIterator() {return setIteratorRowAt(aRow);}
        };
    }
    @Override public List<? extends ILogicalVector> cols() {
        return new AbstractRandomAccessList<ILogicalVector>() {
            @Override public int size() {return ncols();}
            @Override public ILogicalVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public ILogicalVector col(final int aCol) {
        rangeCheckCol(aCol, ncols());
        return new RefLogicalVector() {
            @Override public boolean get(int aIdx) {AbstractVector.rangeCheck(aIdx, size()); return AbstractLogicalMatrix.this.get(aIdx, aCol);}
            @Override public void set(int aIdx, boolean aValue) {AbstractVector.rangeCheck(aIdx, size()); AbstractLogicalMatrix.this.set(aIdx, aCol, aValue);}
            @Override public boolean getAndSet(int aIdx, boolean aValue) {AbstractVector.rangeCheck(aIdx, size()); return AbstractLogicalMatrix.this.getAndSet(aIdx, aCol, aValue);}
            @Override public int size() {return nrows();}
            @Override public IBooleanIterator iterator() {return iteratorColAt(aCol);}
            @Override public IBooleanSetIterator setIterator() {return setIteratorColAt(aCol);}
        };
    }
    
    @Override public void update(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        boolean tValue = get(aRow, aCol);
        tValue = aOpt.applyAsBoolean(tValue);
        set(aRow, aCol, tValue);
    }
    @Override public boolean getAndUpdate(int aRow, int aCol, IBooleanUnaryOperator aOpt) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        boolean tValue = get(aRow, aCol);
        set(aRow, aCol, aOpt.applyAsBoolean(tValue));
        return tValue;
    }
    
    
    @Override public ILogicalMatrix copy() {
        ILogicalMatrix rMatrix = newZeros_(nrows(), ncols());
        rMatrix.fill(this);
        return rMatrix;
    }
    
    /** 矩阵的运算器 */
    @Override public ILogicalMatrixOperation operation() {
        return new AbstractLogicalMatrixOperation() {
            @Override protected ILogicalMatrix thisMatrix_() {return AbstractLogicalMatrix.this;}
            @Override protected ILogicalMatrix newMatrix_(int aRowNum, int aColNum) {return newZeros_(aRowNum, aColNum);}
            @Override protected ILogicalVector newVector_(int aSize) {return newZerosVec_(aSize);}
        };
    }
    
    
    /** stuff to override */
    public abstract boolean get(int aRow, int aCol);
    public abstract void set(int aRow, int aCol, boolean aValue);
    public abstract boolean getAndSet(int aRow, int aCol, boolean aValue); // 返回修改前的值
    public abstract int nrows();
    public abstract int ncols();
    protected abstract ILogicalMatrix newZeros_(int aRowNum, int aColNum);
    protected ILogicalVector newZerosVec_(int aSize) {return LogicalVector.zeros(aSize);}
    
    protected String toString_(boolean aValue) {return " "+(aValue?"T":"F");}
}
