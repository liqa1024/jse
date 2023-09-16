package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleSupplier;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.functional.IDoubleOperator1;
import com.jtool.math.vector.IVector;
import groovy.lang.Closure;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 可自定义获取的矩阵类型的矩阵类 </p>
 * <p> 简单起见默认都是实矩阵，返回类型 double，而如果涉及复矩阵则会提供额外的接口获取复数部分 </p>
 */
public interface IMatrix extends IMatrixGetter {
    /** Iterable stuffs，现在指定具体行列会仅遍历此行或者列，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便使用 */
    IDoubleIterator iteratorCol();
    IDoubleIterator iteratorRow();
    IDoubleIterator iteratorColAt(int aCol);
    IDoubleIterator iteratorRowAt(int aRow);
    IDoubleSetIterator setIteratorCol();
    IDoubleSetIterator setIteratorRow();
    IDoubleSetIterator setIteratorColAt(int aCol);
    IDoubleSetIterator setIteratorRowAt(int aRow);
    
    default Iterable<Double> iterableCol() {return () -> iteratorCol().toIterator();}
    default Iterable<Double> iterableRow() {return () -> iteratorRow().toIterator();}
    
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
    void fill(IMatrix aMatrix);
    void fill(IMatrixGetter aMatrixGetter);
    void fill(double[][] aData);
    default void fill(Iterable<? extends Iterable<? extends Number>> aRows) {fillWithRows(aRows);}
    void fillWithRows(Iterable<? extends Iterable<? extends Number>> aRows);
    void fillWithCols(Iterable<? extends Iterable<? extends Number>> aCols);
    void assignCol(IDoubleSupplier aSup);
    void assignRow(IDoubleSupplier aSup);
    void forEachCol(IDoubleConsumer1 aCon);
    void forEachRow(IDoubleConsumer1 aCon);
    /** Groovy stuff */
    default void fill(final Closure<? extends Number> aGroovyTask) {fill((i, j) -> aGroovyTask.call(i, j).doubleValue());}
    default void assignCol(final Closure<? extends Number> aGroovyTask) {assignCol(() -> aGroovyTask.call().doubleValue());}
    default void assignRow(final Closure<? extends Number> aGroovyTask) {assignRow(() -> aGroovyTask.call().doubleValue());}
    
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
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    void update_(int aRow, int aCol, IDoubleOperator1 aOpt);
    double getAndUpdate_(int aRow, int aCol, IDoubleOperator1 aOpt);
    void update(int aRow, int aCol, IDoubleOperator1 aOpt);
    double getAndUpdate(int aRow, int aCol, IDoubleOperator1 aOpt);
    
    
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
    
    IMatrix plus     (IMatrix aRHS);
    IMatrix minus    (IMatrix aRHS);
    IMatrix multiply (IMatrix aRHS);
    IMatrix div      (IMatrix aRHS);
    IMatrix mod      (IMatrix aRHS);
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void mod2this       (double aRHS);
    
    void plus2this      (IMatrix aRHS);
    void minus2this     (IMatrix aRHS);
    void multiply2this  (IMatrix aRHS);
    void div2this       (IMatrix aRHS);
    void mod2this       (IMatrix aRHS);
    
    
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
    @VisibleForTesting IMatrixRows_ getAt(IIndexFilter aSelectedRows);
    
    /** 用来实现矩阵双重方括号索引，并且约束只能使用两个括号 */
    @ApiStatus.Internal interface IMatrixRow_ {
        @VisibleForTesting double getAt(int aCol);
        @VisibleForTesting void putAt(int aCol, double aValue);
        
        @VisibleForTesting IVector getAt(SliceType aSelectedCols);
        @VisibleForTesting IVector getAt(List<Integer> aSelectedCols);
        @VisibleForTesting IVector getAt(IIndexFilter  aSelectedCols);
        @VisibleForTesting void putAt(SliceType aSelectedCols, double aValue);
        @VisibleForTesting void putAt(SliceType aSelectedCols, Iterable<? extends Number> aList);
        @VisibleForTesting void putAt(SliceType aSelectedCols, IVector aVector);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, double aValue);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, Iterable<? extends Number> aList);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, IVector aVector);
        @VisibleForTesting void putAt(IIndexFilter aSelectedCols, double aValue);
        @VisibleForTesting void putAt(IIndexFilter aSelectedCols, Iterable<? extends Number> aList);
        @VisibleForTesting void putAt(IIndexFilter aSelectedCols, IVector aVector);
    }
    @ApiStatus.Internal interface IMatrixRows_ {
        @VisibleForTesting IVector getAt(int aCol);
        @VisibleForTesting IMatrix getAt(SliceType aSelectedCols);
        @VisibleForTesting IMatrix getAt(List<Integer> aSelectedCols);
        @VisibleForTesting IMatrix getAt(IIndexFilter  aSelectedCols);
        @VisibleForTesting void putAt(int aCol, double aValue);
        @VisibleForTesting void putAt(int aCol, Iterable<? extends Number> aList);
        @VisibleForTesting void putAt(int aCol, IVector aVector);
        @VisibleForTesting void putAt(SliceType aSelectedCols, double aValue);
        @VisibleForTesting void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows);
        @VisibleForTesting void putAt(SliceType aSelectedCols, IMatrix aMatrix);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, double aValue);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, IMatrix aMatrix);
        @VisibleForTesting void putAt(IIndexFilter aSelectedCols, double aValue);
        @VisibleForTesting void putAt(IIndexFilter aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows);
        @VisibleForTesting void putAt(IIndexFilter aSelectedCols, IMatrix aMatrix);
    }
}
