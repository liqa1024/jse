package com.jtool.math.matrix;

import com.jtool.code.ISetIterator;
import com.jtool.code.UT;
import com.jtool.math.vector.AbstractVector;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 一般矩阵的接口的默认实现，用来方便返回抽象的矩阵
 * @author liqa
 */
public abstract class AbstractMatrix<T extends Number> implements IMatrix<T> {
    /** print */
    @Override public String toString() {
        StringBuilder rStr  = new StringBuilder();
        Iterable<? extends Iterable<T>> tRows = rows();
        boolean tFirst = true;
        for (Iterable<T> tRow : tRows) {
            if (!tFirst) rStr.append("\n");
            for (T tValue : tRow) rStr.append(toString_(tValue));
            tFirst = false;
        }
        return rStr.toString();
    }
    
    /** Iterator stuffs */
    @Override public Iterator<T> colIterator(final int aCol) {
        return new Iterator<T>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = aCol;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public T next() {
                if (hasNext()) {
                    T tNext = get_(mCol, mRow);
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    return tNext;
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public Iterator<T> rowIterator(final int aRow) {
        return new Iterator<T>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = aRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public T next() {
                if (hasNext()) {
                    T tNext = get_(mCol, mRow);
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    return tNext;
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public ISetIterator<T, Number> colSetIterator(final int aCol) {
        return new ISetIterator<T, Number>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = aCol, oCol = aCol;
            private int mRow = 0, oRow = -1;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public void set(Number e) {
                if (oRow < 0) throw new IllegalStateException();
                set_(oCol, oRow, e);
            }
            @Override public T next() {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    return get_(oCol, oRow);
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public ISetIterator<T, Number> rowSetIterator(final int aRow) {
        return new ISetIterator<T, Number>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0, oCol = -1;
            private int mRow = aRow, oRow = aRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public void set(Number e) {
                if (oCol < 0) throw new IllegalStateException();
                set_(oCol, oRow, e);
            }
            @Override public T next() {
                if (hasNext()) {
                    oCol = mCol;
                    oRow = mRow;
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    return get_(oCol, oRow);
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public Iterator<? extends Number> colIteratorOf(final int aCol, final IMatrixGetter<? extends Number> aContainer) {
        if (aContainer instanceof IMatrix) return ((IMatrix<?>)aContainer).colIterator(aCol);
        return new Iterator<Number>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = aCol;
            private int mRow = 0;
            @Override public boolean hasNext() {return mCol < mColNum;}
            @Override public Number next() {
                if (hasNext()) {
                    Number tNext = aContainer.get(mCol, mRow);
                    ++mRow;
                    if (mRow == mRowNum) {
                        mRow = 0;
                        ++mCol;
                    }
                    return tNext;
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public Iterator<? extends Number> rowIteratorOf(final int aRow, final IMatrixGetter<? extends Number> aContainer) {
        if (aContainer instanceof IMatrix) return ((IMatrix<?>)aContainer).rowIterator(aRow);
        return new Iterator<Number>() {
            private final int mColNum = columnNumber();
            private final int mRowNum = rowNumber();
            private int mCol = 0;
            private int mRow = aRow;
            @Override public boolean hasNext() {return mRow < mRowNum;}
            @Override public Number next() {
                if (hasNext()) {
                    Number tNext = aContainer.get(mCol, mRow);
                    ++mCol;
                    if (mCol == mColNum) {
                        mCol = 0;
                        ++mRow;
                    }
                    return tNext;
                }
                throw new NoSuchElementException();
            }
        };
    }
    
    
    /** 转为兼容性更好的 double[][]，默认直接使用 rows 转为 double[][] */
    @Override public double[][] mat() {return UT.Code.toMat(rows());}
    
    /** 批量修改的接口，现在统一使用迭代器来填充 */
    @Override public void fill(Number aValue) {
        final ISetIterator<T, Number> si = setIterator();
        while (si.hasNext()) {
            si.next();
            si.set(aValue);
        }
    }
    @Override public void fill(final double[][] aMat) {fillWith((row, col) -> aMat[row][col]);}
    @Override public void fill(Iterable<? extends Iterable<? extends Number>> aRows) {
        Iterator<? extends Iterable<? extends Number>> tRowIt = aRows.iterator();
        List<IVector<T>> rRows = rows();
        for (IVector<T> rRow : rRows) {
            Iterable<? extends Number> tRow = tRowIt.next();
            Iterator<? extends Number> it = tRow.iterator();
            ISetIterator<T, Number> si = rRow.setIterator();
            while (si.hasNext()) {
                si.next();
                si.set(it.next());
            }
        }
    }
    @Override public void fillWith(IMatrixGetter<? extends Number> aMatrixGetter) {
        final ISetIterator<T, Number> si = setIterator();
        final Iterator<? extends Number> it = iteratorOf(aMatrixGetter);
        while (si.hasNext()) {
            si.next();
            si.set(it.next());
        }
    }
    @Override public T get(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return get_(aRow, aCol);
    }
    @Override public T getAndSet(int aRow, int aCol, Number aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndSet_(aRow, aCol, aValue);
    }
    @Override public void set(int aRow, int aCol, Number aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        set_(aRow, aCol, aValue);
    }
    @Override public ISize size() {
        return new ISize() {
            @Override public int row() {return rowNumber();}
            @Override public int col() {return columnNumber();}
        };
    }
    
    
    @Override public List<IVector<T>> rows() {
        return new AbstractList<IVector<T>>() {
            @Override public int size() {return rowNumber();}
            @Override public IVector<T> get(int aRow) {return row(aRow);}
        };
    }
    @Override public IVector<T> row(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new AbstractVector<T>() {
            @Override public T get_(int aIdx) {return AbstractMatrix.this.get_(aRow, aIdx);}
            @Override public void set_(int aIdx, Number aValue) {AbstractMatrix.this.set_(aRow, aIdx, aValue);}
            @Override public T getAndSet_(int aIdx, Number aValue) {return AbstractMatrix.this.getAndSet_(aRow, aIdx, aValue);}
            @Override public int size() {return columnNumber();}
        };
    }
    @Override public List<IVector<T>> cols() {
        return new AbstractList<IVector<T>>() {
            @Override public int size() {return columnNumber();}
            @Override public IVector<T> get(int aCol) {return col(aCol);}
        };
    }
    @Override public IVector<T> col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new AbstractVector<T>() {
            @Override public T get_(int aIdx) {return AbstractMatrix.this.get_(aIdx, aCol);}
            @Override public void set_(int aIdx, Number aValue) {AbstractMatrix.this.set_(aIdx, aCol, aValue);}
            @Override public T getAndSet_(int aIdx, Number aValue) {return AbstractMatrix.this.getAndSet_(aIdx, aCol, aValue);}
            @Override public int size() {return rowNumber();}
        };
    }
    
    
    /** Groovy 的部分，重载一些运算符方便操作 */
    @VisibleForTesting @Override public T call(int aRow, int aCol) {return get(aRow, aCol);}
    @VisibleForTesting @Override public IMatrixRow_<T> getAt(int aRow) {return new MatrixRow_(aRow);}
    
    protected class MatrixRow_ implements IMatrixRow_<T> {
        protected final int mRow;
        protected MatrixRow_(int aRow) {mRow = aRow;}
        
        @Override public T getAt(int aCol) {return get(mRow, aCol);}
        @Override public void putAt(int aCol, Number aValue) {set(mRow, aCol, aValue);}
    }
    
    
    /** stuff to override */
    public abstract T get_(int aRow, int aCol);
    public abstract void set_(int aRow, int aCol, Number aValue);
    public abstract T getAndSet_(int aRow, int aCol, Number aValue); // 返回修改前的值
    public abstract int rowNumber();
    public abstract int columnNumber();
    
    protected String toString_(T aValue) {return String.format(" %8.4g", aValue.doubleValue());}
}
