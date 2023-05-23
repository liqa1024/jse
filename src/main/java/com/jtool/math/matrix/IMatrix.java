package com.jtool.math.matrix;

import com.jtool.math.operator.IOperator2Full;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 通用的矩阵接口，使用泛型来方便实现任意数据类型的矩阵 </p>
 */
public interface IMatrix<T extends Number> extends IMatrixGetter<T>, IMatrixSetter {
    interface ISize {
        int row();
        int col();
    }
    
    /** 转为兼容性更好的 double[][]，默认直接使用 rows 转为 double[][] */
    double[][] mat();
    
    /** 批量修改的接口 */
    void fill(Number aValue);
    void fillWith(Iterable<? extends Iterable<? extends Number>> aRows);
    void fillWith(IMatrixGetter<? extends Number> aMatrixGetter);
    
    /** 访问和修改部分，自带的接口 */
    T get_(int aRow, int aCol);
    void set_(int aRow, int aCol, Number aValue);
    T getAndSet_(int aRow, int aCol, Number aValue); // 返回修改前的值
    int rowNumber();
    int columnNumber();
    
    T get(int aRow, int aCol);
    T getAndSet(int aRow, int aCol, Number aValue);
    void set(int aRow, int aCol, Number aValue);
    ISize size();
    default @VisibleForTesting int nrows() {return rowNumber();}
    default @VisibleForTesting int ncols() {return columnNumber();}
    
    
    List<IVector<T>> rows();
    IVector<T> row(final int aRow);
    List<IVector<T>> cols();
    IVector<T> col(final int aCol);
    
    
    /** Groovy 的部分，重载一些运算符方便操作 */
    @VisibleForTesting T call(int aRow, int aCol);
    @VisibleForTesting IMatrixRow_<T> getAt(int aRow);
    
    /** 用来实现矩阵双重方括号索引，并且约束只能使用两个括号 */
    @ApiStatus.Internal interface IMatrixRow_<T extends Number> {
        @VisibleForTesting T getAt(int aCol);
        @VisibleForTesting void putAt(int aCol, Number aValue);
    }
}
