package jse.math.matrix;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import jep.NDArray;
import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import jse.math.vector.IIntVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * 整数矩阵，返回类型 {@code int}
 * @author liqa
 */
public interface IIntMatrix extends IIntMatrixGetter {
    /** Iterable stuffs，现在指定具体行列会仅遍历此行或者列，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便使用 */
    IIntIterator iteratorCol();
    IIntIterator iteratorRow();
    IIntIterator iteratorColAt(int aCol);
    IIntIterator iteratorRowAt(int aRow);
    IIntSetIterator setIteratorCol();
    IIntSetIterator setIteratorRow();
    IIntSetIterator setIteratorColAt(int aCol);
    IIntSetIterator setIteratorRowAt(int aRow);
    
    default Iterable<Integer> iterableCol() {return () -> iteratorCol().toIterator();}
    default Iterable<Integer> iterableRow() {return () -> iteratorRow().toIterator();}
    
    List<List<Integer>> asListCols();
    List<List<Integer>> asListRows();
    IMatrix asMat();
    
    IIntVector asVecCol();
    IIntVector asVecRow();
    
    /**
     * 转换为 numpy 的数组 {@link NDArray}，在 java 侧根据具体向量类型可能不会进行值拷贝，由于
     * {@link NDArray} 内部实现特性，在 python 中总是会再经历一次值拷贝，此时使用不会有引用问题。
     * @return numpy 的数组 {@link NDArray}
     */
    NDArray<int[]> numpy();
    /** 转为兼容性更好的 {@code int[][]} */
    int[][] data();
    
    /** 批量修改的接口 */
    void fill(int aValue);
    void fill(IIntMatrix aMatrix);
    void fill(IIntMatrixGetter aMatrixGetter);
    void fill(int[][] aData);
    default void fill(Iterable<?> aRows) {fillWithRows(aRows);}
    void fillWithRows(Iterable<?> aRows);
    void fillWithCols(Iterable<?> aCols);
    void assignCol(IntSupplier aSup);
    void assignRow(IntSupplier aSup);
    void forEachCol(IntConsumer aCon);
    void forEachRow(IntConsumer aCon);
    /** Groovy stuff */
    default void fill(@ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {fill((i, j) -> aGroovyTask.call(i, j).intValue());}
    default void assignCol(final Closure<? extends Number> aGroovyTask) {assignCol(() -> aGroovyTask.call().intValue());}
    default void assignRow(final Closure<? extends Number> aGroovyTask) {assignRow(() -> aGroovyTask.call().intValue());}
    
    /** 访问和修改部分，自带的接口 */
    int nrows();
    int ncols();
    @Override int get(int aRow, int aCol);
    int getAndSet(int aRow, int aCol, int aValue); // 返回修改前的值
    void set(int aRow, int aCol, int aValue);
    IMatrix.ISize size();
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    void update(int aRow, int aCol, IntUnaryOperator aOpt);
    int getAndUpdate(int aRow, int aCol, IntUnaryOperator aOpt);
    
    List<? extends IIntVector> rows();
    IIntVector row(int aRow);
    List<? extends IIntVector> cols();
    IIntVector col(int aCol);
    
    
    IIntMatrix copy();
    
    /** 矩阵的运算操作，默认返回新的矩阵 */
    IIntMatrixOperation operation();
    @VisibleForTesting default IIntMatrixOperation op() {return operation();}
}
