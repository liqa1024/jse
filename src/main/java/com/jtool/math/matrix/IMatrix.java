package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.iterator.IDoubleSetOnlyIterator;
import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGetter;
import com.jtool.math.vector.RefVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.List;

/**
 * @author liqa
 * <p> 可自定义获取的矩阵类型的矩阵类 </p>
 * <p> 简单起见默认都是实矩阵，返回类型 double，而如果涉及复矩阵则会提供额外的接口获取复数部分 </p>
 */
public interface IMatrix extends IMatrixGetter, IMatrixSetter {
    /** Iterable stuffs，未指定直接遍历按照列方向；虽然不继承 Iterable 但是会提供相关的直接获取的接口方便使用 */
    default IDoubleIterator iterator() {return colIterator();}
    default IDoubleIterator colIterator() {return colIterator(0);}
    default IDoubleIterator rowIterator() {return rowIterator(0);}
    IDoubleIterator colIterator(int aCol);
    IDoubleIterator rowIterator(int aRow);
    default IDoubleSetIterator setIterator() {return colSetIterator();}
    default IDoubleSetIterator colSetIterator() {return colSetIterator(0);}
    default IDoubleSetIterator rowSetIterator() {return rowSetIterator(0);}
    IDoubleSetIterator colSetIterator(int aCol);
    IDoubleSetIterator rowSetIterator(int aRow);
    default IDoubleIterator iteratorOf(IMatrixGetter aContainer) {return colIteratorOf(aContainer);}
    default IDoubleIterator colIteratorOf(IMatrixGetter aContainer) {return colIteratorOf(0, aContainer);}
    default IDoubleIterator rowIteratorOf(IMatrixGetter aContainer) {return rowIteratorOf(0, aContainer);}
    IDoubleIterator colIteratorOf(int aCol, IMatrixGetter aContainer);
    IDoubleIterator rowIteratorOf(int aRow, IMatrixGetter aContainer);
    default IDoubleSetOnlyIterator setIteratorOf(IMatrixSetter aContainer) {return colSetIteratorOf(aContainer);}
    default IDoubleSetOnlyIterator colSetIteratorOf(IMatrixSetter aContainer) {return colSetIteratorOf(0, aContainer);}
    default IDoubleSetOnlyIterator rowSetIteratorOf(IMatrixSetter aContainer) {return rowSetIteratorOf(0, aContainer);}
    IDoubleSetOnlyIterator colSetIteratorOf(int aCol, IMatrixSetter aContainer);
    IDoubleSetOnlyIterator rowSetIteratorOf(int aRow, IMatrixSetter aContainer);
    
    default Iterable<Double> iterable() {return () -> iterator().toIterator();}
    default List<List<Double>> asList() {
        return new AbstractList<List<Double>>() {
            private final List<IVector> mRows = rows();
            @Override public List<Double> get(int aRow) {return mRows.get(aRow).asList();}
            @Override public int size() {return mRows.size();}
        };
    }
    default IVector asVec() {
        return new RefVector() {
            private final int mRowNum = rowNumber(), mColNum = columnNumber();
            @Override public double get_(int aIdx) {return IMatrix.this.get_(aIdx%mRowNum, aIdx/mRowNum);}
            @Override public void set_(int aIdx, double aValue) {IMatrix.this.set_(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return IMatrix.this.getAndSet_(aIdx%mRowNum, aIdx/mRowNum, aValue);}
            @Override public int size() {return mRowNum * mColNum;}
        };
    }
    
    
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
