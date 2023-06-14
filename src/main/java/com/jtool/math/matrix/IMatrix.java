package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.iterator.IDoubleSetOnlyIterator;
import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGetter;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 可自定义获取的矩阵类型的矩阵类 </p>
 * <p> 简单起见默认都是实矩阵，返回类型 double，而如果涉及复矩阵则会提供额外的接口获取复数部分 </p>
 */
public interface IMatrix extends IMatrixGetter, IMatrixSetter {
    /** Iterable stuffs，现在指定具体行列会仅遍历此行或者列，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便使用 */
    IDoubleIterator colIterator();
    IDoubleIterator rowIterator();
    IDoubleIterator colIterator(int aCol);
    IDoubleIterator rowIterator(int aRow);
    IDoubleSetIterator colSetIterator();
    IDoubleSetIterator rowSetIterator();
    IDoubleSetIterator colSetIterator(int aCol);
    IDoubleSetIterator rowSetIterator(int aRow);
    IDoubleIterator colIteratorOf(IMatrixGetter aContainer);
    IDoubleIterator rowIteratorOf(IMatrixGetter aContainer);
    IDoubleIterator colIteratorOf(int aCol, IMatrixGetter aContainer);
    IDoubleIterator rowIteratorOf(int aRow, IMatrixGetter aContainer);
    IDoubleSetOnlyIterator colSetIteratorOf(IMatrixSetter aContainer);
    IDoubleSetOnlyIterator rowSetIteratorOf(IMatrixSetter aContainer);
    IDoubleSetOnlyIterator colSetIteratorOf(int aCol, IMatrixSetter aContainer);
    IDoubleSetOnlyIterator rowSetIteratorOf(int aRow, IMatrixSetter aContainer);
    
    default Iterable<Double> colIterable() {return () -> colIterator().toIterator();}
    default Iterable<Double> rowIterable() {return () -> rowIterator().toIterator();}
    
    List<List<Double>> asListCols();
    List<List<Double>> asListRows();
    
    IVector asVecCol();
    IVector asVecRow();
    
    interface ISize {
        int row();
        int col();
    }
    
    /** 转为兼容性更好的 double[][] */
    double[][] data();
    
    /** 批量修改的接口 */
    void fill(double aValue);
    void fill(IMatrixGetter aMatrixGetter);
    void fill(double[][] aData);
    default void fill(Iterable<? extends Iterable<? extends Number>> aRows) {fillWithRows(aRows);}
    void fillWithRows(Iterable<? extends Iterable<? extends Number>> aRows);
    void fillWithCols(Iterable<? extends Iterable<? extends Number>> aCols);
    
    /** 访问和修改部分，自带的接口 */
    double get_(int aRow, int aCol);
    void set_(int aRow, int aCol, double aValue);
    double getAndSet_(int aRow, int aCol, double aValue); // 返回修改前的值
    int rowNumber();
    int columnNumber();
    
    double get(int aRow, int aCol);
    double getAndSet(int aRow, int aCol, double aValue);
    void set(int aRow, int aCol, double aValue);
    ISize size();
    default @VisibleForTesting int nrows() {return rowNumber();}
    default @VisibleForTesting int ncols() {return columnNumber();}
    
    /** 附加一些额外的单元素操作，放在这里而不是 operation 因为这些方法理论和 set，get 之类的处于同样地位  */
    void increment_(int aRow, int aCol);
    double getAndIncrement_(int aRow, int aCol);
    double incrementAndGet_(int aRow, int aCol);
    void decrement_(int aRow, int aCol);
    double getAndDecrement_(int aRow, int aCol);
    double decrementAndGet_(int aRow, int aCol);
    void add_(int aRow, int aCol, double aDelta);
    double getAndAdd_(int aRow, int aCol, double aDelta);
    double addAndGet_(int aRow, int aCol, double aDelta);
    void update_(int aRow, int aCol, IDoubleOperator1 aOpt);
    double getAndUpdate_(int aRow, int aCol, IDoubleOperator1 aOpt);
    double updateAndGet_(int aRow, int aCol, IDoubleOperator1 aOpt);
    
    void increment(int aRow, int aCol);
    double getAndIncrement(int aRow, int aCol);
    double incrementAndGet(int aRow, int aCol);
    void decrement(int aRow, int aCol);
    double getAndDecrement(int aRow, int aCol);
    double decrementAndGet(int aRow, int aCol);
    void add(int aRow, int aCol, double aDelta);
    double getAndAdd(int aRow, int aCol, double aDelta);
    double addAndGet(int aRow, int aCol, double aDelta);
    void update(int aRow, int aCol, IDoubleOperator1 aOpt);
    double getAndUpdate(int aRow, int aCol, IDoubleOperator1 aOpt);
    double updateAndGet(int aRow, int aCol, IDoubleOperator1 aOpt);
    
    
    List<IVector> rows();
    IVector row(int aRow);
    List<IVector> cols();
    IVector col(int aCol);
    
    
    
    /** 现在不再提供生成器，只提供直接创建相同类型的全零的矩阵的接口，特殊矩阵的创建请使用 {@link Matrices} */
    default IMatrix newZeros() {return newZeros(rowNumber(), columnNumber());}
    IMatrix newZeros(int aRowNum, int aColNum);
    IVector newZerosVec(int aSize);
    
    IMatrix copy();
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    IMatrixSlicer slicer();
    IMatrixSlicer refSlicer();
    
    /** 矩阵的运算操作，默认返回新的矩阵 */
    IMatrixOperation operation();
    @VisibleForTesting default IMatrixOperation opt() {return operation();}
    
    /** Groovy 的部分，增加矩阵基本的运算操作，现在也归入内部使用 */
    IMatrix plus     (double aRHS);
    IMatrix minus    (double aRHS);
    IMatrix multiply (double aRHS);
    IMatrix div      (double aRHS);
    IMatrix mod      (double aRHS);
    
    IMatrix plus     (IMatrixGetter aRHS);
    IMatrix minus    (IMatrixGetter aRHS);
    IMatrix multiply (IMatrixGetter aRHS);
    IMatrix div      (IMatrixGetter aRHS);
    IMatrix mod      (IMatrixGetter aRHS);
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void mod2this       (double aRHS);
    
    void plus2this      (IMatrixGetter aRHS);
    void minus2this     (IMatrixGetter aRHS);
    void multiply2this  (IMatrixGetter aRHS);
    void div2this       (IMatrixGetter aRHS);
    void mod2this       (IMatrixGetter aRHS);
    
    
    /** Groovy 的部分，重载一些运算符方便操作 */
    @VisibleForTesting double call(int aRow, int aCol);
    @VisibleForTesting IMatrix call(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    @VisibleForTesting IMatrix call(SliceType     aSelectedRows, List<Integer> aSelectedCols);
    @VisibleForTesting IMatrix call(List<Integer> aSelectedRows, SliceType     aSelectedCols);
    @VisibleForTesting IMatrix call(SliceType     aSelectedRows, SliceType     aSelectedCols);
    @VisibleForTesting IVector call(int           aSelectedRow , List<Integer> aSelectedCols);
    @VisibleForTesting IVector call(int           aSelectedRow , SliceType     aSelectedCols);
    @VisibleForTesting IVector call(List<Integer> aSelectedRows, int           aSelectedCol );
    @VisibleForTesting IVector call(SliceType     aSelectedRows, int           aSelectedCol );
    
    @VisibleForTesting IMatrixRow_ getAt(int aRow);
    @VisibleForTesting IMatrixRows_ getAt(SliceType aSelectedRows);
    @VisibleForTesting IMatrixRows_ getAt(List<Integer> aSelectedRows);
    
    /** 用来实现矩阵双重方括号索引，并且约束只能使用两个括号 */
    @ApiStatus.Internal interface IMatrixRow_ {
        @VisibleForTesting double getAt(int aCol);
        @VisibleForTesting void putAt(int aCol, double aValue);
        
        @VisibleForTesting IVector getAt(SliceType aSelectedCols);
        @VisibleForTesting IVector getAt(List<Integer> aSelectedCols);
        @VisibleForTesting void putAt(SliceType aSelectedCols, double aValue);
        @VisibleForTesting void putAt(SliceType aSelectedCols, Iterable<? extends Number> aList);
        @VisibleForTesting void putAt(SliceType aSelectedCols, IVectorGetter aVector);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, double aValue);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, Iterable<? extends Number> aList);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, IVectorGetter aVector);
    }
    @ApiStatus.Internal interface IMatrixRows_ {
        @VisibleForTesting IVector getAt(int aCol);
        @VisibleForTesting IMatrix getAt(SliceType aSelectedCols);
        @VisibleForTesting IMatrix getAt(List<Integer> aSelectedCols);
        @VisibleForTesting void putAt(int aCol, double aValue);
        @VisibleForTesting void putAt(int aCol, Iterable<? extends Number> aList);
        @VisibleForTesting void putAt(int aCol, IVectorGetter aVector);
        @VisibleForTesting void putAt(SliceType aSelectedCols, double aValue);
        @VisibleForTesting void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows);
        @VisibleForTesting void putAt(SliceType aSelectedCols, IMatrixGetter aMatrix);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, double aValue);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, IMatrixGetter aMatrix);
    }
}
