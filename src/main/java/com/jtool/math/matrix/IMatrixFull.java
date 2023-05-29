package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.IHasLotIterator;
import com.jtool.code.ISetIterator;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGenerator;
import com.jtool.math.vector.IVectorGetter;
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
public interface IMatrixFull<M extends IMatrixGetter, V extends IVectorGetter> extends IMatrixGetter, IHasLotIterator<IMatrixGetter, Double> {
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
    
    default Iterable<Double> iterable() {return IMatrixFull.this::iterator;}
    default List<List<Double>> asList() {
        return new AbstractList<List<Double>>() {
            private final List<IVector> mRows = rows();
            @Override public List<Double> get(int aRow) {return mRows.get(aRow).asList();}
            @Override public int size() {return mRows.size();}
        };
    }
    
    
    interface ISize {
        int row();
        int col();
    }
    
    /** 转为兼容性更好的 double[][]，默认直接使用 rows 转为 double[][] */
    double[][] mat();
    
    /** 批量修改的接口 */
    void fill(double aValue);
    void fill(double[][] aMat);
    default void fill(Iterable<? extends Iterable<? extends Number>> aRows) {fillWithRows(aRows);}
    void fill(IMatrixGetter aMatrixGetter);
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
    
    
    List<IVector> rows();
    IVector row(final int aRow);
    List<IVector> cols();
    IVector col(final int aCol);
    
    
    
    /** 获得基于自身的矩阵生成器，方便构造相同大小的同样的矩阵 */
    IMatrixGenerator<M> generator();
    @VisibleForTesting default IMatrixGenerator<M> gen() {return generator();}
    /** 还需要包含一个向量的生成器方便一些操作 */
    IVectorGenerator<V> generatorVec();
    @VisibleForTesting default IVectorGenerator<V> genVec() {return generatorVec();}
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    IMatrixSlicer<M, V> slicer();
    IMatrixSlicer<IMatrix, IVector> refSlicer();
    
    /** 矩阵的运算操作，默认返回新的矩阵 */
    IMatrixOperation<M, V> operation();
    @VisibleForTesting default IMatrixOperation<M, V> opt() {return operation();}
    
    /** Groovy 的部分，增加矩阵基本的运算操作，由于不能重载 += 之类的变成向自身操作，因此会充斥着值拷贝，因此不推荐重性能的场景使用 */
    @VisibleForTesting M plus       (double aRHS);
    @VisibleForTesting M minus      (double aRHS);
    @VisibleForTesting M multiply   (double aRHS);
    @VisibleForTesting M div        (double aRHS);
    @VisibleForTesting M mod        (double aRHS);
    
    @VisibleForTesting M plus       (IMatrixGetter aRHS);
    @VisibleForTesting M minus      (IMatrixGetter aRHS);
    @VisibleForTesting M multiply   (IMatrixGetter aRHS);
    @VisibleForTesting M div        (IMatrixGetter aRHS);
    @VisibleForTesting M mod        (IMatrixGetter aRHS);
    
    /** Groovy 的部分，重载一些运算符方便操作 */
    @VisibleForTesting double call(int aRow, int aCol);
    @VisibleForTesting M call(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    @VisibleForTesting M call(SliceType     aSelectedRows, List<Integer> aSelectedCols);
    @VisibleForTesting M call(List<Integer> aSelectedRows, SliceType     aSelectedCols);
    @VisibleForTesting M call(SliceType     aSelectedRows, SliceType     aSelectedCols);
    @VisibleForTesting V call(int           aSelectedRow , List<Integer> aSelectedCols);
    @VisibleForTesting V call(int           aSelectedRow , SliceType     aSelectedCols);
    @VisibleForTesting V call(List<Integer> aSelectedRows, int           aSelectedCol );
    @VisibleForTesting V call(SliceType     aSelectedRows, int           aSelectedCol );
    
    @VisibleForTesting IMatrixRow_<V> getAt(int aRow);
    @VisibleForTesting IMatrixRows_<V, M> getAt(SliceType aSelectedRows);
    @VisibleForTesting IMatrixRows_<V, M> getAt(List<Integer> aSelectedRows);
    
    /** 用来实现矩阵双重方括号索引，并且约束只能使用两个括号 */
    @ApiStatus.Internal interface IMatrixRow_<V extends IVectorGetter> {
        @VisibleForTesting double getAt(int aCol);
        @VisibleForTesting void putAt(int aCol, double aValue);
        
        @VisibleForTesting V getAt(SliceType aSelectedCols);
        @VisibleForTesting V getAt(List<Integer> aSelectedCols);
        @VisibleForTesting void putAt(SliceType aSelectedCols, double aValue);
        @VisibleForTesting void putAt(SliceType aSelectedCols, Iterable<? extends Number> aList);
        @VisibleForTesting void putAt(SliceType aSelectedCols, IVectorGetter aVector);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, double aValue);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, Iterable<? extends Number> aList);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, IVectorGetter aVector);
    }
    @ApiStatus.Internal interface IMatrixRows_<V extends IVectorGetter, M extends IMatrixGetter> {
        @VisibleForTesting V getAt(int aCol);
        @VisibleForTesting M getAt(SliceType aSelectedCols);
        @VisibleForTesting M getAt(List<Integer> aSelectedCols);
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
