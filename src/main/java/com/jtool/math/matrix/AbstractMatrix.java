package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.collection.AbstractCollections;
import com.jtool.code.collection.AbstractRandomAccessList;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleSupplier;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.functional.IDoubleOperator1;
import com.jtool.math.vector.*;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import static com.jtool.code.CS.ALL;


/**
 * @author liqa
 * <p> 通用的矩阵类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractMatrix implements IMatrix {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        rStr.append(String.format("%d x %d Matrix:", rowNumber(), columnNumber()));
        List<IVector> tRows = rows();
        for (IVector tRow : tRows) {
            rStr.append("\n");
            for (double tValue : tRow.iterable()) rStr.append(toString_(tValue));
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public IDoubleIterator iteratorCol() {
        return new IDoubleIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = get_(mRow, mCol);
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
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = get_(mRow, mCol);
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
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new IDoubleIterator() {
            private final int mRowNum = rowNumber();
            private int mRow = 0;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = get_(mRow, aCol);
                    ++mRow;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleIterator iteratorRowAt(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new IDoubleIterator() {
            private final int mColNum = columnNumber();
            private int mCol = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = get_(aRow, mCol);
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
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(double aValue) {
                if (oRow < 0) throw new IllegalStateException();
                set_(oRow, oCol, aValue);
            }
            @Override public double next() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {mRow = 0; ++mCol;}
                    return get_(oRow, oCol);
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
                    set_(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIteratorRow() {
        return new IDoubleSetIterator() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(double aValue) {
                if (oCol < 0) throw new IllegalStateException();
                set_(oRow, oCol, aValue);
            }
            @Override public double next() {
                if (hasNext()) {
                    oCol = mCol; oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {mCol = 0; ++mRow;}
                    return get_(oRow, oCol);
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
                    set_(oRow, oCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIteratorColAt(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new IDoubleSetIterator() {
            private final int mRowNum = rowNumber();
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(double aValue) {
                if (oRow < 0) throw new IllegalStateException();
                set_(oRow, aCol, aValue);
            }
            @Override public double next() {
                if (hasNext()) {
                    oRow = mRow; ++mRow;
                    return get_(oRow, aCol);
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
                    set_(oRow, aCol, aValue);
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIteratorRowAt(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new IDoubleSetIterator() {
            private final int mColNum = columnNumber();
            private int mCol = 0, oCol = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(double aValue) {
                if (oCol < 0) throw new IllegalStateException();
                set_(aRow, oCol, aValue);
            }
            @Override public double next() {
                if (hasNext()) {
                    oCol = mCol; ++mCol;
                    return get_(aRow, oCol);
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
                    set_(aRow, oCol, aValue);
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
            private final int mRowNum = rowNumber(), mColNum = columnNumber();
            @Override public double get_(int aIdx) {return AbstractMatrix.this.get_(aIdx%mRowNum, aIdx/mRowNum);}
            @Override public void set_(int aIdx, double aValue) {AbstractMatrix.this.set_(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet_(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IDoubleIterator iterator() {return iteratorCol();}
            @Override public IDoubleSetIterator setIterator() {return setIteratorCol();}
        };
    }
    @Override public IVector asVecRow() {
        return new RefVector() {
            private final int mRowNum = rowNumber(), mColNum = columnNumber();
            @Override public double get_(int aIdx) {return AbstractMatrix.this.get_(aIdx/mColNum, aIdx%mColNum);}
            @Override public void set_(int aIdx, double aValue) {AbstractMatrix.this.set_(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet_(aIdx/mColNum, aIdx%mColNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
            @Override public IDoubleIterator iterator() {return iteratorRow();}
            @Override public IDoubleSetIterator setIterator() {return setIteratorRow();}
        };
    }
    
    
    /** 转为兼容性更好的 double[][]，默认直接使用 asListRow 转为 double[][] */
    @Override public double[][] data() {
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        double[][] rMat = new double[tRowNum][tColNum];
        final IDoubleIterator it = iteratorRow();
        for (int row = 0; row < tRowNum; ++row) {
            final double[] tRow = rMat[row];
            for (int col = 0; col < tColNum; ++col) tRow[col] = it.next();
        }
        return rMat;
    }
    
    /** 批量修改的接口，现在统一使用迭代器来填充 */
    @Override public final void fill(double aValue) {operation().fill(aValue);}
    @Override public final void fill(IMatrix aMatrix) {operation().fill(aMatrix);}
    @Override public final void fill(IMatrixGetter aMatrixGetter) {operation().fill(aMatrixGetter);}
    
    /** 同样这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    @Override public void fill(final double[][] aData) {
        final IDoubleSetIterator si = setIteratorRow();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int row = 0; row < tRowNum; ++row) {
            final double[] tRow = aData[row];
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRow[col]);
        }
    }
    @Override public void fillWithRows(Iterable<? extends Iterable<? extends Number>> aRows) {
        final Iterator<? extends Iterable<? extends Number>> tRowsIt = aRows.iterator();
        final IDoubleSetIterator si = setIteratorRow();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int row = 0; row < tRowNum; ++row) {
            final Iterator<? extends Number> tRowIt = tRowsIt.next().iterator();
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRowIt.next().doubleValue());
        }
    }
    @Override public void fillWithCols(Iterable<? extends Iterable<? extends Number>> aCols) {
        final Iterator<? extends Iterable<? extends Number>> tColsIt = aCols.iterator();
        final IDoubleSetIterator si = setIteratorCol();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int col = 0; col < tColNum; ++col) {
            final Iterator<? extends Number> tColIt = tColsIt.next().iterator();
            for (int row = 0; row < tRowNum; ++row) si.nextAndSet(tColIt.next().doubleValue());
        }
    }
    
    @Override public final void assignCol(IDoubleSupplier aSup) {operation().assignCol(aSup);}
    @Override public final void assignRow(IDoubleSupplier aSup) {operation().assignRow(aSup);}
    @Override public final void forEachCol(IDoubleConsumer1 aCon) {operation().forEachCol(aCon);}
    @Override public final void forEachRow(IDoubleConsumer1 aCon) {operation().forEachRow(aCon);}
    
    
    @Override public double get(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return get_(aRow, aCol);
    }
    @Override public double getAndSet(int aRow, int aCol, double aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndSet_(aRow, aCol, aValue);
    }
    @Override public void set(int aRow, int aCol, double aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        set_(aRow, aCol, aValue);
    }
    @Override public ISize size() {
        return new ISize() {
            @Override public int row() {return rowNumber();}
            @Override public int col() {return columnNumber();}
        };
    }
    
    
    @Override public List<IVector> rows() {
        return new AbstractRandomAccessList<IVector>() {
            @Override public int size() {return rowNumber();}
            @Override public IVector get(int aRow) {return row(aRow);}
        };
    }
    @Override public IVector row(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new RefVector() {
            @Override public double get_(int aIdx) {return AbstractMatrix.this.get_(aRow, aIdx);}
            @Override public void set_(int aIdx, double aValue) {AbstractMatrix.this.set_(aRow, aIdx, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet_(aRow, aIdx, aValue);}
            @Override public int size() {return columnNumber();}
            @Override public IDoubleIterator iterator() {return iteratorRowAt(aRow);}
            @Override public IDoubleSetIterator setIterator() {return setIteratorRowAt(aRow);}
        };
    }
    @Override public List<IVector> cols() {
        return new AbstractRandomAccessList<IVector>() {
            @Override public int size() {return columnNumber();}
            @Override public IVector get(int aCol) {return col(aCol);}
        };
    }
    @Override public IVector col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new RefVector() {
            @Override public double get_(int aIdx) {return AbstractMatrix.this.get_(aIdx, aCol);}
            @Override public void set_(int aIdx, double aValue) {AbstractMatrix.this.set_(aIdx, aCol, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet_(aIdx, aCol, aValue);}
            @Override public int size() {return rowNumber();}
            @Override public IDoubleIterator iterator() {return iteratorColAt(aCol);}
            @Override public IDoubleSetIterator setIterator() {return setIteratorColAt(aCol);}
        };
    }
    
    @Override public void update_(int aRow, int aCol, IDoubleOperator1 aOpt) {
        double tValue = get_(aRow, aCol);
        tValue = aOpt.cal(tValue);
        set_(aRow, aCol, tValue);
    }
    @Override public double getAndUpdate_(int aRow, int aCol, IDoubleOperator1 aOpt) {
        double tValue = get_(aRow, aCol);
        set_(aRow, aCol, aOpt.cal(tValue));
        return tValue;
    }
    @Override public void update(int aRow, int aCol, IDoubleOperator1 aOpt) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        update_(aRow, aCol, aOpt);
    }
    @Override public double getAndUpdate(int aRow, int aCol, IDoubleOperator1 aOpt) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndUpdate_(aRow, aCol, aOpt);
    }
    
    
    
    @Override public final IMatrix copy() {
        IMatrix rMatrix = newZeros();
        rMatrix.fill(this);
        return rMatrix;
    }
    
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    @Override public IMatrixSlicer slicer() {
        return new AbstractMatrixSlicer() {
            @Override protected IVector getIL(final int aSelectedRow, final List<Integer> aSelectedCols) {IVector rVector = newZerosVec(aSelectedCols.size()); rVector.fill(refSlicer().get(aSelectedRow, aSelectedCols)); return rVector;}
            @Override protected IVector getLI(final List<Integer> aSelectedRows, final int aSelectedCol) {IVector rVector = newZerosVec(aSelectedRows.size()); rVector.fill(refSlicer().get(aSelectedRows, aSelectedCol)); return rVector;}
            @Override protected IVector getIA(final int aSelectedRow) {IVector rVector = newZerosVec(columnNumber()); rVector.fill(refSlicer().get(aSelectedRow, ALL)); return rVector;}
            @Override protected IVector getAI(final int aSelectedCol) {IVector rVector = newZerosVec(rowNumber()   ); rVector.fill(refSlicer().get(ALL, aSelectedCol)); return rVector;}
            
            @Override protected IMatrix getLL(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {IMatrix rMatrix = newZeros(aSelectedRows.size(), aSelectedCols.size()); rMatrix.fill(refSlicer().get(aSelectedRows, aSelectedCols)); return rMatrix;}
            @Override protected IMatrix getLA(final List<Integer> aSelectedRows) {IMatrix rMatrix = newZeros(aSelectedRows.size(), columnNumber()      ); rMatrix.fill(refSlicer().get(aSelectedRows, ALL)); return rMatrix;}
            @Override protected IMatrix getAL(final List<Integer> aSelectedCols) {IMatrix rMatrix = newZeros(rowNumber()         , aSelectedCols.size()); rMatrix.fill(refSlicer().get(ALL, aSelectedCols)); return rMatrix;}
            @Override protected IMatrix getAA() {return copy();}
            
            @Override public IVector diag(final int aShift) {
                IVector rVector;
                if (aShift > 0) {
                    rVector = newZerosVec(Math.min(rowNumber(), columnNumber()-aShift));
                    rVector.fill(i -> AbstractMatrix.this.get_(i, i+aShift));
                } else
                if (aShift < 0) {
                    rVector = newZerosVec(Math.min(rowNumber()+aShift, columnNumber()));
                    rVector.fill(i -> AbstractMatrix.this.get_(i-aShift, i));
                } else {
                    rVector = newZerosVec(Math.min(rowNumber(), columnNumber()));
                    rVector.fill(i -> AbstractMatrix.this.get_(i, i));
                }
                return rVector;
            }
            
            @Override protected int thisRowNum_() {return rowNumber();}
            @Override protected int thisColNum_() {return columnNumber();}
        };
    }
    @Override public IMatrixSlicer refSlicer() {
        return new AbstractMatrixSlicer() {
            @Override protected IVector getIL(final int aSelectedRow, final List<Integer> aSelectedCols) {
                if (aSelectedRow<0 || aSelectedRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aSelectedRow);
                return new RefVector() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aIdx) {return AbstractMatrix.this.get(aSelectedRow, aSelectedCols.get(aIdx));}
                    @Override public void set_(int aIdx, double aValue) {AbstractMatrix.this.set(aSelectedRow, aSelectedCols.get(aIdx), aValue);}
                    @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet(aSelectedRow, aSelectedCols.get(aIdx), aValue);}
                    @Override public int size() {return aSelectedCols.size();}
                };
            }
            @Override protected IVector getLI(final List<Integer> aSelectedRows, final int aSelectedCol) {
                if (aSelectedCol<0 || aSelectedCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aSelectedCol);
                return new RefVector() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aIdx) {return AbstractMatrix.this.get(aSelectedRows.get(aIdx), aSelectedCol);}
                    @Override public void set_(int aIdx, double aValue) {AbstractMatrix.this.set(aSelectedRows.get(aIdx), aSelectedCol, aValue);}
                    @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet(aSelectedRows.get(aIdx), aSelectedCol, aValue);}
                    @Override public int size() {return aSelectedRows.size();}
                };
            }
            @Override protected IVector getIA(int aSelectedRow) {return row(aSelectedRow);}
            @Override protected IVector getAI(int aSelectedCol) {return col(aSelectedCol);}
            
            @Override protected IMatrix getLL(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {
                return new RefMatrix() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aRow, int aCol) {return AbstractMatrix.this.get(aSelectedRows.get(aRow), aSelectedCols.get(aCol));}
                    @Override public void set_(int aRow, int aCol, double aValue) {AbstractMatrix.this.set(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public double getAndSet_(int aRow, int aCol, double aValue) {return AbstractMatrix.this.getAndSet(aSelectedRows.get(aRow), aSelectedCols.get(aCol), aValue);}
                    @Override public int rowNumber() {return aSelectedRows.size();}
                    @Override public int columnNumber() {return aSelectedCols.size();}
                };
            }
            @Override protected IMatrix getLA(final List<Integer> aSelectedRows) {
                return new RefMatrix() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aRow, int aCol) {return AbstractMatrix.this.get(aSelectedRows.get(aRow), aCol);}
                    @Override public void set_(int aRow, int aCol, double aValue) {AbstractMatrix.this.set(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public double getAndSet_(int aRow, int aCol, double aValue) {return AbstractMatrix.this.getAndSet(aSelectedRows.get(aRow), aCol, aValue);}
                    @Override public int rowNumber() {return aSelectedRows.size();}
                    @Override public int columnNumber() {return AbstractMatrix.this.columnNumber();}
                };
            }
            @Override protected IMatrix getAL(final List<Integer> aSelectedCols) {
                return new RefMatrix() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public double get_(int aRow, int aCol) {return AbstractMatrix.this.get(aRow, aSelectedCols.get(aCol));}
                    @Override public void set_(int aRow, int aCol, double aValue) {AbstractMatrix.this.set(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public double getAndSet_(int aRow, int aCol, double aValue) {return AbstractMatrix.this.getAndSet(aRow, aSelectedCols.get(aCol), aValue);}
                    @Override public int rowNumber() {return AbstractMatrix.this.rowNumber();}
                    @Override public int columnNumber() {return aSelectedCols.size();}
                };
            }
            @Override protected IMatrix getAA() {
                return new RefMatrix() {
                    /** 对于全部切片，则不再需要二次边界检查 */
                    @Override public double get_(int aRow, int aCol) {return AbstractMatrix.this.get_(aRow, aCol);}
                    @Override public void set_(int aRow, int aCol, double aValue) {AbstractMatrix.this.set_(aRow, aCol, aValue);}
                    @Override public double getAndSet_(int aRow, int aCol, double aValue) {return AbstractMatrix.this.getAndSet_(aRow, aCol, aValue);}
                    @Override public int rowNumber() {return AbstractMatrix.this.rowNumber();}
                    @Override public int columnNumber() {return AbstractMatrix.this.columnNumber();}
                };
            }
            
            @Override public IVector diag(final int aShift) {
                if (aShift > 0) {
                    return new RefVector() {
                        @Override public double get_(int aIdx) {return AbstractMatrix.this.get_(aIdx, aIdx+aShift);}
                        @Override public void set_(int aIdx, double aValue)  {AbstractMatrix.this.set_(aIdx, aIdx+aShift, aValue);}
                        @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet_(aIdx, aIdx+aShift, aValue);}
                        @Override public int size() {return Math.min(rowNumber(), columnNumber()-aShift);}
                    };
                } else
                if (aShift < 0) {
                    return new RefVector() {
                        @Override public double get_(int aIdx) {return AbstractMatrix.this.get_(aIdx-aShift, aIdx);}
                        @Override public void set_(int aIdx, double aValue)  {AbstractMatrix.this.set_(aIdx-aShift, aIdx, aValue);}
                        @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet_(aIdx-aShift, aIdx, aValue);}
                        @Override public int size() {return Math.min(rowNumber()+aShift, columnNumber());}
                    };
                } else {
                    return new RefVector() {
                        @Override public double get_(int aIdx) {return AbstractMatrix.this.get_(aIdx, aIdx);}
                        @Override public void set_(int aIdx, double aValue)  {AbstractMatrix.this.set_(aIdx, aIdx, aValue);}
                        @Override public double getAndSet_(int aIdx, double aValue) {return AbstractMatrix.this.getAndSet_(aIdx, aIdx, aValue);}
                        @Override public int size() {return Math.min(rowNumber(), columnNumber());}
                    };
                }
            }
            
            @Override protected int thisRowNum_() {return rowNumber();}
            @Override protected int thisColNum_() {return columnNumber();}
        };
    }
    
    /** 矩阵的运算器 */
    @Override public IMatrixOperation operation() {
        return new AbstractMatrixOperation() {
            @Override protected IMatrix thisMatrix_() {return AbstractMatrix.this;}
            @Override protected IMatrix newMatrix_(ISize aSize) {return newZeros(aSize.row(), aSize.col());}
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
    
    /** Groovy 的部分，增加矩阵切片操作 */
    @VisibleForTesting @Override public final double call(int aRow, int aCol) {return get(aRow, aCol);}
    @VisibleForTesting @Override public final IMatrix call(List<Integer> aSelectedRows, List<Integer> aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public final IMatrix call(SliceType     aSelectedRows, List<Integer> aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public final IMatrix call(List<Integer> aSelectedRows, SliceType     aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public final IMatrix call(SliceType     aSelectedRows, SliceType     aSelectedCols) {return slicer().get(aSelectedRows, aSelectedCols);}
    @VisibleForTesting @Override public final IVector call(int           aSelectedRow , List<Integer> aSelectedCols) {return slicer().get(aSelectedRow , aSelectedCols);}
    @VisibleForTesting @Override public final IVector call(int           aSelectedRow , SliceType     aSelectedCols) {return slicer().get(aSelectedRow , aSelectedCols);}
    @VisibleForTesting @Override public final IVector call(List<Integer> aSelectedRows, int           aSelectedCol ) {return slicer().get(aSelectedRows, aSelectedCol );}
    @VisibleForTesting @Override public final IVector call(SliceType     aSelectedRows, int           aSelectedCol ) {return slicer().get(aSelectedRows, aSelectedCol );}
    
    @VisibleForTesting @Override public final IMatrixRows_ getAt(SliceType aSelectedRows) {return new MatrixRowsA_(aSelectedRows);}
    @VisibleForTesting @Override public final IMatrixRows_ getAt(List<Integer> aSelectedRows) {return new MatrixRowsL_(aSelectedRows);}
    @VisibleForTesting @Override public final IMatrixRows_ getAt(IIndexFilter aSelectedRows) {return new MatrixRowsF_(aSelectedRows);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public final IMatrixRow_ getAt(int aRow) {return new MatrixRow_((aRow < 0) ? (rowNumber()+aRow) : aRow);}
    
    private class MatrixRow_ implements IMatrixRow_ {
        private final int mRow;
        private MatrixRow_(int aRow) {mRow = aRow;}
        
        @Override public IVector getAt(SliceType aSelectedCols) {return slicer().get(mRow, aSelectedCols);}
        @Override public IVector getAt(List<Integer> aSelectedCols) {return slicer().get(mRow, aSelectedCols);}
        @Override public IVector getAt(IIndexFilter aSelectedCols) {return slicer().get(mRow, aSelectedCols);}
        
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mRow, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Number> aList) {refSlicer().get(mRow, aSelectedCols).fill(aList);}
        @Override public void putAt(SliceType aSelectedCols, IVector aVector) {refSlicer().get(mRow, aSelectedCols).fill(aVector);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mRow, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Number> aList) {refSlicer().get(mRow, aSelectedCols).fill(aList);}
        @Override public void putAt(List<Integer> aSelectedCols, IVector aVector) {refSlicer().get(mRow, aSelectedCols).fill(aVector);}
        @Override public void putAt(IIndexFilter aSelectedCols, double aValue) {refSlicer().get(mRow, aSelectedCols).fill(aValue);}
        @Override public void putAt(IIndexFilter aSelectedCols, Iterable<? extends Number> aList) {refSlicer().get(mRow, aSelectedCols).fill(aList);}
        @Override public void putAt(IIndexFilter aSelectedCols, IVector aVector) {refSlicer().get(mRow, aSelectedCols).fill(aVector);}
        
        /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
        @Override public double getAt(int aCol) {return get(mRow, (aCol < 0) ? (columnNumber()+aCol) : aCol);}
        @Override public void putAt(int aCol, double aValue) {set(mRow, (aCol < 0) ? (columnNumber()+aCol) : aCol, aValue);}
    }
    private class MatrixRowsA_ implements IMatrixRows_ {
        private final SliceType mSelectedRows;
        private MatrixRowsA_(SliceType aSelectedRows) {mSelectedRows = aSelectedRows;}
        
        @Override public IMatrix getAt(SliceType aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public IMatrix getAt(List<Integer> aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public IMatrix getAt(IIndexFilter aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        
        @Override public void putAt(int aCol, Iterable<? extends Number> aList) {refSlicer().get(mSelectedRows, aCol).fill(aList);}
        @Override public void putAt(int aCol, IVector aVector) {refSlicer().get(mSelectedRows, aCol).fill(aVector);}
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWithRows(aRows);}
        @Override public void putAt(SliceType aSelectedCols, IMatrix aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWithRows(aRows);}
        @Override public void putAt(List<Integer> aSelectedCols, IMatrix aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(IIndexFilter aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(IIndexFilter aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWithRows(aRows);}
        @Override public void putAt(IIndexFilter aSelectedCols, IMatrix aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        
        /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
        @Override public IVector getAt(int aCol) {return slicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol);}
        @Override public void putAt(int aCol, double aValue) {refSlicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol).fill(aValue);}
    }
    private class MatrixRowsL_ implements IMatrixRows_ {
        private final List<Integer> mSelectedRows;
        private MatrixRowsL_(List<Integer> aSelectedRows) {mSelectedRows = aSelectedRows;}
        
        @Override public IMatrix getAt(SliceType aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public IMatrix getAt(List<Integer> aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public IMatrix getAt(IIndexFilter aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        
        @Override public void putAt(int aCol, Iterable<? extends Number> aList) {refSlicer().get(mSelectedRows, aCol).fill(aList);}
        @Override public void putAt(int aCol, IVector aVector) {refSlicer().get(mSelectedRows, aCol).fill(aVector);}
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWithRows(aRows);}
        @Override public void putAt(SliceType aSelectedCols, IMatrix aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWithRows(aRows);}
        @Override public void putAt(List<Integer> aSelectedCols, IMatrix aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(IIndexFilter aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(IIndexFilter aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWithRows(aRows);}
        @Override public void putAt(IIndexFilter aSelectedCols, IMatrix aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        
        /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
        @Override public IVector getAt(int aCol) {return slicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol);}
        @Override public void putAt(int aCol, double aValue) {refSlicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol).fill(aValue);}
    }
    private class MatrixRowsF_ implements IMatrixRows_ {
        private final IIndexFilter mSelectedRows;
        private MatrixRowsF_(IIndexFilter aSelectedRows) {mSelectedRows = aSelectedRows;}
        
        @Override public IMatrix getAt(SliceType aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public IMatrix getAt(List<Integer> aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public IMatrix getAt(IIndexFilter aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        
        @Override public void putAt(int aCol, Iterable<? extends Number> aList) {refSlicer().get(mSelectedRows, aCol).fill(aList);}
        @Override public void putAt(int aCol, IVector aVector) {refSlicer().get(mSelectedRows, aCol).fill(aVector);}
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWithRows(aRows);}
        @Override public void putAt(SliceType aSelectedCols, IMatrix aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWithRows(aRows);}
        @Override public void putAt(List<Integer> aSelectedCols, IMatrix aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(IIndexFilter aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(IIndexFilter aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fillWithRows(aRows);}
        @Override public void putAt(IIndexFilter aSelectedCols, IMatrix aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        
        /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
        @Override public IVector getAt(int aCol) {return slicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol);}
        @Override public void putAt(int aCol, double aValue) {refSlicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol).fill(aValue);}
    }
    
    /** stuff to override */
    public abstract double get_(int aRow, int aCol);
    public abstract void set_(int aRow, int aCol, double aValue);
    public abstract double getAndSet_(int aRow, int aCol, double aValue); // 返回修改前的值
    public abstract int rowNumber();
    public abstract int columnNumber();
    public abstract IMatrix newZeros(int aRowNum, int aColNum);
    public abstract IVector newZerosVec(int aSize);
    
    protected String toString_(double aValue) {return String.format("   %.4g", aValue);}
}
