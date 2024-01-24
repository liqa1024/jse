package jtool.math.matrix;

import jtool.code.collection.AbstractCollections;
import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.iterator.IDoubleIterator;
import jtool.code.iterator.IIntIterator;
import jtool.code.iterator.IIntSetIterator;
import jtool.math.vector.IIntVector;
import jtool.math.vector.IntVector;
import jtool.math.vector.RefIntVector;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import static jtool.math.matrix.AbstractMatrix.rangeCheckCol;
import static jtool.math.matrix.AbstractMatrix.rangeCheckRow;

public abstract class AbstractIntMatrix implements IIntMatrix {
    /** print */
    @Override public String toString() {
        final StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d x %d Integer Matrix:", rowNumber(), columnNumber()));
        List<IIntVector> tRows = rows();
        for (IIntVector tRow : tRows) {
            rStr.append("\n");
            tRow.forEach(v -> rStr.append(toString_(v)));
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IIntIterator iteratorCol() {
        return new IIntIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = get(mRow, mCol);
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntIterator iteratorRow() {
        return new IIntIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = get(mRow, mCol);
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntIterator iteratorColAt(final int aCol) {
        rangeCheckCol(aCol, columnNumber());
        return new IIntIterator() {
            private final int mRowNum = rowNumber();
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mRowNum;}
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
        rangeCheckRow(aRow, rowNumber());
        return new IIntIterator() {
            private final int mColNum = columnNumber();
            private int mCol = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
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
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(int aValue) {
                if (oRow < 0) throw new IllegalStateException();
                AbstractIntMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                    AbstractIntMatrix.this.set(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntSetIterator setIteratorRow() {
        return new IIntSetIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(int aValue) {
                if (oCol < 0) throw new IllegalStateException();
                AbstractIntMatrix.this.set(oRow, oCol, aValue);
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                    AbstractIntMatrix.this.set(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IIntSetIterator setIteratorColAt(final int aCol) {
        rangeCheckCol(aCol, columnNumber());
        return new IIntSetIterator() {
            private final int mRowNum = rowNumber();
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
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
        rangeCheckRow(aRow, rowNumber());
        return new IIntSetIterator() {
            private final int mColNum = columnNumber();
            private int mCol = 0, oCol = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
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
            @Override public int rowNumber() {return AbstractIntMatrix.this.rowNumber();}
            @Override public int columnNumber() {return AbstractIntMatrix.this.columnNumber();}
            @Override public IDoubleIterator iteratorCol() {return AbstractIntMatrix.this.iteratorCol().asDouble();}
            @Override public IDoubleIterator iteratorRow() {return AbstractIntMatrix.this.iteratorRow().asDouble();}
            @Override public IDoubleIterator iteratorColAt(int aCol) {return AbstractIntMatrix.this.iteratorColAt(aCol).asDouble();}
            @Override public IDoubleIterator iteratorRowAt(int aRow) {return AbstractIntMatrix.this.iteratorRowAt(aRow).asDouble();}
        };
    }
    @Override public IIntVector asVecCol() {
        return new RefIntVector() {
            private final int mRowNum = rowNumber(), mColNum = columnNumber();
            @Override public int get(int aIdx) {return AbstractIntMatrix.this.get(aIdx%mRowNum, aIdx/mRowNum);}
            @Override public void set(int aIdx, int aValue) {AbstractIntMatrix.this.set(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntMatrix.this.getAndSet(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IIntIterator iterator() {return iteratorCol();}
            @Override public IIntSetIterator setIterator() {return setIteratorCol();}
        };
    }
    @Override public IIntVector asVecRow() {
        return new RefIntVector() {
            private final int mRowNum = rowNumber(), mColNum = columnNumber();
            @Override public int get(int aIdx) {return AbstractIntMatrix.this.get(aIdx/mColNum, aIdx%mColNum);}
            @Override public void set(int aIdx, int aValue) {AbstractIntMatrix.this.set(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntMatrix.this.getAndSet(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IIntIterator iterator() {return iteratorRow();}
            @Override public IIntSetIterator setIterator() {return setIteratorRow();}
        };
    }
    
    
    /** 转为兼容性更好的 int[][]，默认直接使用 asListRow 转为 int[][] */
    @Override public int[][] data() {
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        int[][] rMat = new int[tRowNum][tColNum];
        final IIntIterator it = iteratorRow();
        for (int row = 0; row < tRowNum; ++row) {
            final int[] tRow = rMat[row];
            for (int col = 0; col < tColNum; ++col) tRow[col] = it.next();
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
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int row = 0; row < tRowNum; ++row) {
            final int[] tRow = aData[row];
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRow[col]);
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
        return new IMatrix.ISize() {
            @Override public int row() {return rowNumber();}
            @Override public int col() {return columnNumber();}
        };
    }
    
    
    @Override public List<IIntVector> rows() {
        return new AbstractRandomAccessList<IIntVector>() {
            @Override public int size() {return rowNumber();}
            @Override public IIntVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public IIntVector row(final int aRow) {
        rangeCheckRow(aRow, rowNumber());
        return new RefIntVector() {
            @Override public int get(int aIdx) {return AbstractIntMatrix.this.get(aRow, aIdx);}
            @Override public void set(int aIdx, int aValue) {AbstractIntMatrix.this.set(aRow, aIdx, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntMatrix.this.getAndSet(aRow, aIdx, aValue);}
            @Override public int size() {return columnNumber();}
            @Override public IIntIterator iterator() {return iteratorRowAt(aRow);}
            @Override public IIntSetIterator setIterator() {return setIteratorRowAt(aRow);}
        };
    }
    @Override public List<IIntVector> cols() {
        return new AbstractRandomAccessList<IIntVector>() {
            @Override public int size() {return columnNumber();}
            @Override public IIntVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public IIntVector col(final int aCol) {
        rangeCheckCol(aCol, columnNumber());
        return new RefIntVector() {
            @Override public int get(int aIdx) {return AbstractIntMatrix.this.get(aIdx, aCol);}
            @Override public void set(int aIdx, int aValue) {AbstractIntMatrix.this.set(aIdx, aCol, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {return AbstractIntMatrix.this.getAndSet(aIdx, aCol, aValue);}
            @Override public int size() {return rowNumber();}
            @Override public IIntIterator iterator() {return iteratorColAt(aCol);}
            @Override public IIntSetIterator setIterator() {return setIteratorColAt(aCol);}
        };
    }
    
    @Override public void update(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, rowNumber());
        rangeCheckCol(aCol, columnNumber());
        int tValue = get(aRow, aCol);
        tValue = aOpt.applyAsInt(tValue);
        set(aRow, aCol, tValue);
    }
    @Override public int getAndUpdate(int aRow, int aCol, IntUnaryOperator aOpt) {
        rangeCheckRow(aRow, rowNumber());
        rangeCheckCol(aCol, columnNumber());
        int tValue = get(aRow, aCol);
        set(aRow, aCol, aOpt.applyAsInt(tValue));
        return tValue;
    }
    
    
    @Override public IIntMatrix copy() {
        IIntMatrix rMatrix = newZeros_(rowNumber(), columnNumber());
        rMatrix.fill(this);
        return rMatrix;
    }
    
    /** 矩阵的运算器 */
    @Override public IIntMatrixOperation operation() {
        return new AbstractIntMatrixOperation() {
            @Override protected IIntMatrix thisMatrix_() {return AbstractIntMatrix.this;}
            @Override protected IIntMatrix newMatrix_(int aRowNum, int aColNum) {return newZeros_(aRowNum, aColNum);}
            @Override protected IIntVector newVector_(int aSize) {return newZerosVec_(aSize);}
        };
    }
    
    
    /** stuff to override */
    public abstract int get(int aRow, int aCol);
    public abstract void set(int aRow, int aCol, int aValue);
    public abstract int getAndSet(int aRow, int aCol, int aValue); // 返回修改前的值
    public abstract int rowNumber();
    public abstract int columnNumber();
    protected abstract IIntMatrix newZeros_(int aRowNum, int aColNum);
    protected IIntVector newZerosVec_(int aSize) {return IntVector.zeros(aSize);}
    
    protected String toString_(int aValue) {return " "+aValue;}
}
