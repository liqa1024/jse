package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.ISetIterator;
import com.jtool.code.UT;
import com.jtool.code.operator.IOperator1;
import com.jtool.math.vector.*;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;


/**
 * @author liqa
 * <p> 通用的矩阵类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractMatrix implements IMatrix {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        List<IVector> tRows = rows();
        boolean tFirst = true;
        for (IVector tRow : tRows) {
            if (!tFirst) rStr.append("\n");
            for (double tValue : tRow.iterable()) rStr.append(toString_(tValue));
            tFirst = false;
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public Iterator<Double> colIterator(final int aCol) {
        return new Iterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = aCol;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public Double next() {
                if (hasNext()) {
                    double tNext = get_(mRow, mCol);
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public Iterator<Double> rowIterator(final int aRow) {
        return new Iterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = aRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public Double next() {
                if (hasNext()) {
                    double tNext = get_(mRow, mCol);
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ISetIterator<Double> colSetIterator(final int aCol) {
        return new ISetIterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = aCol, oCol = aCol;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(Double e) {
                if (oRow < 0) throw new IllegalStateException();
                set_(oRow, oCol, e);
            }
            @Override public Double next() {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    return get_(oRow, oCol);
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(Double e) {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    set_(oRow, oCol, e);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    double oValue = get_(oRow, oCol);
                    set_(oRow, oCol, e);
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ISetIterator<Double> rowSetIterator(final int aRow) {
        return new ISetIterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = aRow, oRow = aRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(Double e) {
                if (oCol < 0) throw new IllegalStateException();
                set_(oRow, oCol, e);
            }
            @Override public Double next() {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    return get_(oRow, oCol);
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(Double e) {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    set_(oRow, oCol, e);
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    double oValue = get_(oRow, oCol);
                    set_(oRow, oCol, e);
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public Iterator<Double> colIteratorOf(final int aCol, final IMatrixGetter aContainer) {
        if (aContainer instanceof IMatrix) return ((IMatrix)aContainer).colIterator(aCol);
        return new Iterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = aCol;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public Double next() {
                if (hasNext()) {
                    double tNext = aContainer.get(mRow, mCol);
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public Iterator<Double> rowIteratorOf(final int aRow, final IMatrixGetter aContainer) {
        if (aContainer instanceof IMatrix) return ((IMatrix)aContainer).rowIterator(aRow);
        return new Iterator<Double>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = aRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public Double next() {
                if (hasNext()) {
                    double tNext = aContainer.get(mRow, mCol);
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    
    
    /** 转为兼容性更好的 double[][]，默认直接使用 asList 转为 double[][] */
    @Override public double[][] data() {return UT.Code.toMat(asList());}
    
    /** 批量修改的接口，现在统一使用迭代器来填充 */
    @Override public final void fill(double aValue) {operation().mapFill2this(aValue);}
    @Override public final void fill(IMatrixGetter aMatrixGetter) {operation().ebeFill2this(aMatrixGetter);}
    
    /** 同样这里改为直接用迭代器遍历实现而不去调用对应向量的运算，中等的优化程度 */
    @Override public void fill(final double[][] aData) {
        final ISetIterator<Double> si = rowSetIterator();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int row = 0; row < tRowNum; ++row) {
            final double[] tRow = aData[row];
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRow[col]);
        }
    }
    @Override public void fillWithRows(Iterable<? extends Iterable<? extends Number>> aRows) {
        final Iterator<? extends Iterable<? extends Number>> tRowsIt = aRows.iterator();
        final ISetIterator<Double> si = rowSetIterator();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int row = 0; row < tRowNum; ++row) {
            final Iterator<? extends Number> tRowIt = tRowsIt.next().iterator();
            for (int col = 0; col < tColNum; ++col) si.nextAndSet(tRowIt.next().doubleValue());
        }
    }
    @Override public void fillWithCols(Iterable<? extends Iterable<? extends Number>> aCols) {
        final Iterator<? extends Iterable<? extends Number>> tColsIt = aCols.iterator();
        final ISetIterator<Double> si = colSetIterator();
        final int tRowNum = rowNumber();
        final int tColNum = columnNumber();
        for (int col = 0; col < tColNum; ++col) {
            final Iterator<? extends Number> tColIt = tColsIt.next().iterator();
            for (int row = 0; row < tRowNum; ++row) si.nextAndSet(tColIt.next().doubleValue());
        }
    }
    
    
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
        return new AbstractList<IVector>() {
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
        };
    }
    @Override public List<IVector> cols() {
        return new AbstractList<IVector>() {
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
        };
    }
    
    @Override public void increment_(int aRow, int aCol) {
        double tValue = get_(aRow, aCol);
        ++tValue;
        set_(aRow, aCol, tValue);
    }
    @Override public double getAndIncrement_(int aRow, int aCol) {
        double tValue = get_(aRow, aCol);
        set_(aRow, aCol, tValue+1);
        return tValue;
    }
    @Override public double incrementAndGet_(int aRow, int aCol) {
        double tValue = get_(aRow, aCol);
        ++tValue;
        set_(aRow, aCol, tValue);
        return tValue;
    }
    @Override public void decrement_(int aRow, int aCol) {
        double tValue = get_(aRow, aCol);
        --tValue;
        set_(aRow, aCol, tValue);
    }
    @Override public double getAndDecrement_(int aRow, int aCol) {
        double tValue = get_(aRow, aCol);
        set_(aRow, aCol, tValue-1);
        return tValue;
    }
    @Override public double decrementAndGet_(int aRow, int aCol) {
        double tValue = get_(aRow, aCol);
        --tValue;
        set_(aRow, aCol, tValue);
        return tValue;
    }
    @Override public void add_(int aRow, int aCol, double aDelta) {
        double tValue = get_(aRow, aCol);
        tValue += aDelta;
        set_(aRow, aCol, tValue);
    }
    @Override public double getAndAdd_(int aRow, int aCol, double aDelta) {
        double tValue = get_(aRow, aCol);
        set_(aRow, aCol, tValue+aDelta);
        return tValue;
    }
    @Override public double addAndGet_(int aRow, int aCol, double aDelta) {
        double tValue = get_(aRow, aCol);
        tValue += aDelta;
        set_(aRow, aCol, tValue);
        return tValue;
    }
    @Override public void update_(int aRow, int aCol, IOperator1<Double> aOpt) {
        double tValue = get_(aRow, aCol);
        tValue = aOpt.cal(tValue);
        set_(aRow, aCol, tValue);
    }
    @Override public double getAndUpdate_(int aRow, int aCol, IOperator1<Double> aOpt) {
        double tValue = get_(aRow, aCol);
        set_(aRow, aCol, aOpt.cal(tValue));
        return tValue;
    }
    @Override public double updateAndGet_(int aRow, int aCol, IOperator1<Double> aOpt) {
        double tValue = get_(aRow, aCol);
        tValue = aOpt.cal(tValue);
        set_(aRow, aCol, tValue);
        return tValue;
    }
    
    @Override public void increment(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        increment_(aRow, aCol);
    }
    @Override public double getAndIncrement(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndIncrement_(aRow, aCol);
    }
    @Override public double incrementAndGet(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return incrementAndGet_(aRow, aCol);
    }
    @Override public void decrement(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        decrement_(aRow, aCol);
    }
    @Override public double getAndDecrement(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndDecrement_(aRow, aCol);
    }
    @Override public double decrementAndGet(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return decrementAndGet_(aRow, aCol);
    }
    @Override public void add(int aRow, int aCol, double aDelta) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        add_(aRow, aCol, aDelta);
    }
    @Override public double getAndAdd(int aRow, int aCol, double aDelta) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndAdd_(aRow, aCol, aDelta);
    }
    @Override public double addAndGet(int aRow, int aCol, double aDelta) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return addAndGet_(aRow, aCol, aDelta);
    }
    @Override public void update(int aRow, int aCol, IOperator1<Double> aOpt) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        update_(aRow, aCol, aOpt);
    }
    @Override public double getAndUpdate(int aRow, int aCol, IOperator1<Double> aOpt) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndUpdate_(aRow, aCol, aOpt);
    }
    @Override public double updateAndGet(int aRow, int aCol, IOperator1<Double> aOpt) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return updateAndGet_(aRow, aCol, aOpt);
    }
    
    
    
    @Override public IMatrix copy() {
        IMatrix rMatrix = newZeros();
        final ISetIterator<Double> si = rMatrix.setIterator();
        final Iterator<Double> it = iterator();
        while (si.hasNext()) si.nextAndSet(it.next());
        return rMatrix;
    }
    
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    @Override public IMatrixSlicer slicer() {
        return new AbstractMatrixSlicer() {
            @Override protected IVector getIL(final int aSelectedRow, final List<Integer> aSelectedCols) {IVector rVector = newZerosVec(aSelectedCols.size()); rVector.fill(i -> AbstractMatrix.this.get(aSelectedRow, aSelectedCols.get(i))); return rVector;}
            @Override protected IVector getLI(final List<Integer> aSelectedRows, final int aSelectedCol) {IVector rVector = newZerosVec(aSelectedRows.size()); rVector.fill(i -> AbstractMatrix.this.get(aSelectedRows.get(i), aSelectedCol)); return rVector;}
            @Override protected IVector getIA(final int aSelectedRow) {IVector rVector = newZerosVec(columnNumber()); rVector.fill(col -> AbstractMatrix.this.get(aSelectedRow, col)); return rVector;}
            @Override protected IVector getAI(final int aSelectedCol) {IVector rVector = newZerosVec(rowNumber()   ); rVector.fill(row -> AbstractMatrix.this.get(row, aSelectedCol)); return rVector;}
            
            @Override protected IMatrix getLL(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {IMatrix rMatrix = newZeros(aSelectedRows.size(), aSelectedCols.size()); rMatrix.fill((row, col) -> AbstractMatrix.this.get(aSelectedRows.get(row), aSelectedCols.get(col))); return rMatrix;}
            @Override protected IMatrix getLA(final List<Integer> aSelectedRows) {IMatrix rMatrix = newZeros(aSelectedRows.size(), columnNumber()      ); rMatrix.fill((row, col) -> AbstractMatrix.this.get(aSelectedRows.get(row), col)); return rMatrix;}
            @Override protected IMatrix getAL(final List<Integer> aSelectedCols) {IMatrix rMatrix = newZeros(rowNumber()         , aSelectedCols.size()); rMatrix.fill((row, col) -> AbstractMatrix.this.get(row, aSelectedCols.get(col))); return rMatrix;}
            @Override protected IMatrix getAA() {return copy();}
            
            @Override protected List<IVector> thisRows_() {return rows();}
            @Override protected List<IVector> thisCols_() {return cols();}
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
            
            @Override protected List<IVector> thisRows_() {return rows();}
            @Override protected List<IVector> thisCols_() {return cols();}
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
    @Override public final IMatrix plus     (double aRHS) {return operation().mapPlus       (this, aRHS);}
    @Override public final IMatrix minus    (double aRHS) {return operation().mapMinus      (this, aRHS);}
    @Override public final IMatrix multiply (double aRHS) {return operation().mapMultiply   (this, aRHS);}
    @Override public final IMatrix div      (double aRHS) {return operation().mapDiv        (this, aRHS);}
    @Override public final IMatrix mod      (double aRHS) {return operation().mapMod        (this, aRHS);}
    
    @Override public final IMatrix plus     (IMatrixGetter aRHS) {return operation().ebePlus    (this, aRHS);}
    @Override public final IMatrix minus    (IMatrixGetter aRHS) {return operation().ebeMinus   (this, aRHS);}
    @Override public final IMatrix multiply (IMatrixGetter aRHS) {return operation().ebeMultiply(this, aRHS);}
    @Override public final IMatrix div      (IMatrixGetter aRHS) {return operation().ebeDiv     (this, aRHS);}
    @Override public final IMatrix mod      (IMatrixGetter aRHS) {return operation().ebeMod     (this, aRHS);}
    
    @Override public final void plus2this       (double aRHS) {operation().mapPlus2this     (aRHS);}
    @Override public final void minus2this      (double aRHS) {operation().mapMinus2this    (aRHS);}
    @Override public final void multiply2this   (double aRHS) {operation().mapMultiply2this (aRHS);}
    @Override public final void div2this        (double aRHS) {operation().mapDiv2this      (aRHS);}
    @Override public final void mod2this        (double aRHS) {operation().mapMod2this      (aRHS);}
    
    @Override public final void plus2this       (IMatrixGetter aRHS) {operation().ebePlus2this      (aRHS);}
    @Override public final void minus2this      (IMatrixGetter aRHS) {operation().ebeMinus2this     (aRHS);}
    @Override public final void multiply2this   (IMatrixGetter aRHS) {operation().ebeMultiply2this  (aRHS);}
    @Override public final void div2this        (IMatrixGetter aRHS) {operation().ebeDiv2this       (aRHS);}
    @Override public final void mod2this        (IMatrixGetter aRHS) {operation().ebeMod2this       (aRHS);}
    
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
    @VisibleForTesting @Override public final IMatrixRows_ getAt(List<Integer> aSelectedRows)  {return new MatrixRowsL_(aSelectedRows);}
    
    /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
    @VisibleForTesting @Override public final IMatrixRow_ getAt(int aRow) {return new MatrixRow_((aRow < 0) ? (rowNumber()+aRow) : aRow);}
    
    private class MatrixRow_ implements IMatrixRow_ {
        private final int mRow;
        private MatrixRow_(int aRow) {mRow = aRow;}
        
        @Override public IVector getAt(SliceType aSelectedCols) {return slicer().get(mRow, aSelectedCols);}
        @Override public IVector getAt(List<Integer> aSelectedCols) {return slicer().get(mRow, aSelectedCols);}
        
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mRow, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Number> aList) {refSlicer().get(mRow, aSelectedCols).fill(aList);}
        @Override public void putAt(SliceType aSelectedCols, IVectorGetter aVector) {refSlicer().get(mRow, aSelectedCols).fill(aVector);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mRow, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Number> aList) {refSlicer().get(mRow, aSelectedCols).fill(aList);}
        @Override public void putAt(List<Integer> aSelectedCols, IVectorGetter aVector) {refSlicer().get(mRow, aSelectedCols).fill(aVector);}
        
        /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
        @Override public double getAt(int aCol) {return get(mRow, (aCol < 0) ? (columnNumber()+aCol) : aCol);}
        @Override public void putAt(int aCol, double aValue) {set(mRow, (aCol < 0) ? (columnNumber()+aCol) : aCol, aValue);}
    }
    private class MatrixRowsA_ implements IMatrixRows_ {
        private final SliceType mSelectedRows;
        private MatrixRowsA_(SliceType aSelectedRows) {mSelectedRows = aSelectedRows;}
        
        @Override public IMatrix getAt(SliceType aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public IMatrix getAt(List<Integer> aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        
        @Override public void putAt(int aCol, Iterable<? extends Number> aList) {refSlicer().get(mSelectedRows, aCol).fill(aList);}
        @Override public void putAt(int aCol, IVectorGetter aVector) {refSlicer().get(mSelectedRows, aCol).fill(aVector);}
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aRows);}
        @Override public void putAt(SliceType aSelectedCols, IMatrixGetter aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aRows);}
        @Override public void putAt(List<Integer> aSelectedCols, IMatrixGetter aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        
        /** 对于 groovy 的单个数的方括号索引（python like），提供负数索引支持，注意对于数组索引不提供这个支持 */
        @Override public IVector getAt(int aCol) {return slicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol);}
        @Override public void putAt(int aCol, double aValue) {refSlicer().get(mSelectedRows, (aCol < 0) ? (columnNumber()+aCol) : aCol).fill(aValue);}
    }
    private class MatrixRowsL_ implements IMatrixRows_ {
        private final List<Integer> mSelectedRows;
        private MatrixRowsL_(List<Integer> aSelectedRows) {mSelectedRows = aSelectedRows;}
        
        @Override public IMatrix getAt(SliceType aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        @Override public IMatrix getAt(List<Integer> aSelectedCols) {return slicer().get(mSelectedRows, aSelectedCols);}
        
        @Override public void putAt(int aCol, Iterable<? extends Number> aList) {refSlicer().get(mSelectedRows, aCol).fill(aList);}
        @Override public void putAt(int aCol, IVectorGetter aVector) {refSlicer().get(mSelectedRows, aCol).fill(aVector);}
        @Override public void putAt(SliceType aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aRows);}
        @Override public void putAt(SliceType aSelectedCols, IMatrixGetter aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        @Override public void putAt(List<Integer> aSelectedCols, double aValue) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aValue);}
        @Override public void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aRows);}
        @Override public void putAt(List<Integer> aSelectedCols, IMatrixGetter aMatrix) {refSlicer().get(mSelectedRows, aSelectedCols).fill(aMatrix);}
        
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
    
    protected String toString_(double aValue) {return String.format(" %8.4g", aValue);}
}
