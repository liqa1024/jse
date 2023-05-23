package com.jtool.math.matrix;

import com.jtool.code.UT;
import com.jtool.math.vector.AbstractVector;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

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
    
    /** 转为兼容性更好的 double[][]，默认直接使用 rows 转为 double[][] */
    @Override public double[][] mat() {return UT.Code.toMat(rows());}
    
    /** 批量修改的接口 */
    @Override public void fill(Number aValue) {
        int tRowNum = rowNumber();
        int tColNum = columnNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) set_(row, col, aValue);
    }
    @Override public void fillWith(Iterable<? extends Iterable<? extends Number>> aRows) {
        int tRowNum = rowNumber();
        int tColNum = columnNumber();
        Iterator<? extends Iterable<? extends Number>> tRowIt = aRows.iterator();
        int row = 0;
        while (row < tRowNum && tRowIt.hasNext()) {
            Iterable<? extends Number> tRow = tRowIt.next();
            Iterator<? extends Number> tIt = tRow.iterator();
            int col = 0;
            while (col < tColNum && tIt.hasNext()) {
                set_(row, col, tIt.next());
                ++col;
            }
            ++row;
        }
    }
    @Override public void fillWith(IMatrixGetter<? extends Number> aMatrixGetter) {
        int tRowNum = rowNumber();
        int tColNum = columnNumber();
        for (int col = 0; col < tColNum; ++col) for (int row = 0; row < tRowNum; ++row) set_(row, col, aMatrixGetter.get(row, col));
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
