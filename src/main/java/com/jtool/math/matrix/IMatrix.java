package com.jtool.math.matrix;

import com.jtool.code.UT;
import com.jtool.math.operator.IOperator1Full;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.List;

/**
 * @author liqa
 * <p> 通用的矩阵接口，使用泛型来方便实现任意数据类型的矩阵 </p>
 */
public interface IMatrix<T extends Number> {
    class Util {
        protected static <T extends Number> String toString_(IMatrix<T> aMatrix, IOperator1Full<String, T> aOpt) {
            StringBuilder rStr  = new StringBuilder();
            List<List<T>> tRows = aMatrix.rows();
            boolean tFirst = true;
            for (List<T> tRow : tRows) {
                if (!tFirst) rStr.append("\n");
                for (T tValue : tRow) rStr.append(aOpt.cal(tValue));
                tFirst = false;
            }
            return rStr.toString();
        }
    }
    
    
    interface ISize {
        int row();
        int col();
    }
    
    /** 转为兼容性更好的 double[][]，默认直接使用 rows 转为 double[][] */
    default double[][] mat() {return UT.Code.toMat(rows());}
    
    /** 访问和修改部分，自带的接口 */
    T get_(int aRow, int aCol);
    void set_(int aRow, int aCol, Number aValue);
    T getAndSet_(int aRow, int aCol, Number aValue); // 返回修改前的值
    int rowNumber();
    int columnNumber();
    
    default T get(int aRow, int aCol) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return get_(aRow, aCol);
    }
    default T getAndSet(int aRow, int aCol, Number aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        return getAndSet_(aRow, aCol, aValue);
    }
    default void set(int aRow, int aCol, Number aValue) {
        if (aRow<0 || aRow>=rowNumber() || aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException(String.format("Row: %d, Col: %d", aRow, aCol));
        set_(aRow, aCol, aValue);
    }
    default ISize size() {
        return new ISize() {
            @Override public int row() {return rowNumber();}
            @Override public int col() {return columnNumber();}
        };
    }
    default @VisibleForTesting int nrows() {return rowNumber();}
    default @VisibleForTesting int ncols() {return columnNumber();}
    
    
    default List<List<T>> rows() {
        return new AbstractList<List<T>>() {
            @Override public int size() {return rowNumber();}
            @Override public List<T> get(int aRow) {return row(aRow);}
        };
    }
    default List<T> row(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new AbstractList<T>() {
            @Override public int size() {return columnNumber();}
            @Override public T get(int aCol) {
                if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
                return get_(aRow, aCol);
            }
            @Override public T set(int aCol, T aValue) {
                if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
                return getAndSet_(aRow, aCol, aValue);
            }
        };
    }
    default List<List<T>> cols() {
        return new AbstractList<List<T>>() {
            @Override public int size() {return columnNumber();}
            @Override public List<T> get(int aCol) {return col(aCol);}
        };
    }
    default List<T> col(final int aCol) {
        if (aCol<0 || aCol>=columnNumber()) throw new IndexOutOfBoundsException("Col: "+aCol);
        return new AbstractList<T>() {
            @Override public int size() {return rowNumber();}
            @Override public T get(int aRow) {
                if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
                return get_(aRow, aCol);
            }
            @Override public T set(int aRow, T aValue) {
                if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
                return getAndSet_(aRow, aCol, aValue);
            }
        };
    }
    
    
    /** Groovy 的部分，重载一些运算符方便操作 */
    @VisibleForTesting default T call(int aRow, int aCol) {return get(aRow, aCol);}
    @VisibleForTesting default T getAt(List<Integer> aIndex) {
        if (aIndex.size() != 2) throw new IllegalArgumentException("Index Size Must be 2");
        return get(aIndex.get(0), aIndex.get(1));
    }
    @VisibleForTesting default void putAt(List<Integer> aIndex, Number aValue) {
        if (aIndex.size() != 2) throw new IllegalArgumentException("Index Size Must be 2");
        set(aIndex.get(0), aIndex.get(1), aValue);
    }
}
