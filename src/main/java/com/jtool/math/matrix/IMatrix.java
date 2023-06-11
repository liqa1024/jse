package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.IHasLotIterator;
import com.jtool.code.ISetIterator;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGetter;
import com.jtool.math.vector.RefVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

/**
 * @author liqa
 * <p> 可自定义获取的矩阵类型的矩阵类 </p>
 * <p> 简单起见默认都是实矩阵，返回类型 double，而如果涉及复矩阵则会提供额外的接口获取复数部分 </p>
 */
public interface IMatrix extends IMatrixGetter, IHasLotIterator<IMatrixGetter, Double> {
    /** Iterable stuffs，未指定直接遍历按照列方向；虽然不继承 Iterable 但是会提供相关的直接获取的接口方便使用 */
    default Iterator<Double> iterator() {return colIterator();}
    default Iterator<Double> colIterator() {return colIterator(0);}
    default Iterator<Double> rowIterator() {return rowIterator(0);}
    Iterator<Double> colIterator(int aCol);
    Iterator<Double> rowIterator(int aRow);
    default ISetIterator<Double> setIterator() {return colSetIterator();}
    default ISetIterator<Double> colSetIterator() {return colSetIterator(0);}
    default ISetIterator<Double> rowSetIterator() {return rowSetIterator(0);}
    ISetIterator<Double> colSetIterator(int aCol);
    ISetIterator<Double> rowSetIterator(int aRow);
    default Iterator<Double> iteratorOf(IMatrixGetter aContainer) {return colIteratorOf(aContainer);}
    default Iterator<Double> colIteratorOf(IMatrixGetter aContainer) {return colIteratorOf(0, aContainer);}
    default Iterator<Double> rowIteratorOf(IMatrixGetter aContainer) {return rowIteratorOf(0, aContainer);}
    Iterator<Double> colIteratorOf(int aCol, IMatrixGetter aContainer);
    Iterator<Double> rowIteratorOf(int aRow, IMatrixGetter aContainer);
    
    default Iterable<Double> iterable() {return IMatrix.this::iterator;}
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
    
    /** 发现还需要这些操作 */
    void increment_(int aRow, int aCol);
    double getAndIncrement_(int aRow, int aCol);
    double incrementAndGet_(int aRow, int aCol);
    void decrement_(int aRow, int aCol);
    double getAndDecrement_(int aRow, int aCol);
    double decrementAndGet_(int aRow, int aCol);
    
    void increment(int aRow, int aCol);
    double getAndIncrement(int aRow, int aCol);
    double incrementAndGet(int aRow, int aCol);
    void decrement(int aRow, int aCol);
    double getAndDecrement(int aRow, int aCol);
    double decrementAndGet(int aRow, int aCol);
    
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
    
    /** Groovy 的部分，增加矩阵基本的运算操作，由于不能重载 += 之类的变成向自身操作，因此会充斥着值拷贝，因此不推荐重性能的场景使用 */
    @VisibleForTesting IMatrix plus     (double aRHS);
    @VisibleForTesting IMatrix minus    (double aRHS);
    @VisibleForTesting IMatrix multiply (double aRHS);
    @VisibleForTesting IMatrix div      (double aRHS);
    @VisibleForTesting IMatrix mod      (double aRHS);
    
    @VisibleForTesting IMatrix plus     (IMatrixGetter aRHS);
    @VisibleForTesting IMatrix minus    (IMatrixGetter aRHS);
    @VisibleForTesting IMatrix multiply (IMatrixGetter aRHS);
    @VisibleForTesting IMatrix div      (IMatrixGetter aRHS);
    @VisibleForTesting IMatrix mod      (IMatrixGetter aRHS);
    
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
