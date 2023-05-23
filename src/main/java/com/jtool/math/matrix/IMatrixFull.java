package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.math.vector.IVectorGenerator;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 在原本的矩阵接口上扩展更多高级功能 </p>
 */
public interface IMatrixFull<T extends Number, M extends IMatrix<T>, V extends IVector<T>> extends IMatrix<T> {
    
    /** 获得基于自身的向量生成器，生成按列排布的向量 */
    IVectorGenerator<V> generatorVec();
    
    /** 获得基于自身的矩阵生成器，方便构造相同大小的同样的矩阵 */
    IMatrixGenerator<M> generatorMat();
    @VisibleForTesting default IMatrixGenerator<M> gen() {return generatorMat();}
    @VisibleForTesting default IVectorGenerator<V> genVec() {return generatorVec();}
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    IMatrixSlicer<M, V> slicer();
    IMatrixSlicer<IMatrix<T>, IVector<T>> refSlicer();
    
    /** Groovy 的部分，增加矩阵切片操作 */
    @VisibleForTesting M call(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    @VisibleForTesting M call(SliceType     aSelectedRows, List<Integer> aSelectedCols);
    @VisibleForTesting M call(List<Integer> aSelectedRows, SliceType     aSelectedCols);
    @VisibleForTesting M call(SliceType     aSelectedRows, SliceType     aSelectedCols);
    @VisibleForTesting V call(int           aSelectedRow , List<Integer> aSelectedCols);
    @VisibleForTesting V call(int           aSelectedRow , SliceType     aSelectedCols);
    @VisibleForTesting V call(List<Integer> aSelectedRows, int           aSelectedCol );
    @VisibleForTesting V call(SliceType     aSelectedRows, int           aSelectedCol );
    
    @VisibleForTesting IMatrixRowFull_<T, V> getAt(int aRow);
    @VisibleForTesting IMatrixRows_<T, V, M> getAt(SliceType aSelectedRows);
    @VisibleForTesting IMatrixRows_<T, V, M> getAt(List<Integer> aSelectedRows);
    
    /** 用来实现矩阵双重方括号索引，并且约束只能使用两个括号 */
    @ApiStatus.Internal interface IMatrixRowFull_<T extends Number, V extends IVector<T>> extends IMatrixRow_<T> {
        @VisibleForTesting V getAt(SliceType aSelectedCols);
        @VisibleForTesting V getAt(List<Integer> aSelectedCols);
        @VisibleForTesting void putAt(SliceType aSelectedCols, Number aValue);
        @VisibleForTesting void putAt(SliceType aSelectedCols, Iterable<? extends Number> aVec);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, Number aValue);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, Iterable<? extends Number> aVec);
    }
    @ApiStatus.Internal interface IMatrixRows_<T extends Number, V extends IVector<T>, M extends IMatrix<T>> {
        @VisibleForTesting V getAt(int aCol);
        @VisibleForTesting M getAt(SliceType aSelectedCols);
        @VisibleForTesting M getAt(List<Integer> aSelectedCols);
        @VisibleForTesting void putAt(int aCol, Number aValue);
        @VisibleForTesting void putAt(int aCol, Iterable<? extends Number> aVec);
        @VisibleForTesting void putAt(SliceType aSelectedCols, Number aValue);
        @VisibleForTesting void putAt(SliceType aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows);
        @VisibleForTesting void putAt(SliceType aSelectedCols, IMatrix<? extends Number> aMatrix);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, Number aValue);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, Iterable<? extends Iterable<? extends Number>> aRows);
        @VisibleForTesting void putAt(List<Integer> aSelectedCols, IMatrix<? extends Number> aMatrix);
    }
}
