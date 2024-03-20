package jse.math.matrix;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import jse.math.vector.IIntVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * @author liqa
 * <p> 专用的整数矩阵 </p>
 * <p> 由于完全实现工作量较大，这里暂只实现用到的接口 </p>
 * <p> 当然为了后续完善的方便，结构依旧保持一致 </p>
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
    
    /** 转为兼容性更好的 int[][] */
    int[][] data();
    
    /**
     * 通用的转换成 {@link ColumnIntMatrix} 或 {@link RowIntMatrix} 的方法，借鉴了 jni 中相关函数的实现思路，
     * 对于 {@link ColumnIntMatrix} 或 {@link RowIntMatrix} 会直接转换，而其他类型会使用缓存；
     * <p>
     * 使用完成后调用 {@link #releaseBuf} 来释放数据，此时会将更改应用到数据中，
     * 而对于使用缓存的类型会归还缓存；
     * <p>
     * aAbort 参数用于指定是否抛弃数据，对于 {@link #toBufCol} 或 {@link #toBufRow} 则不需要获取到原始数据（仅写入并且会全部写入），
     * 对于 {@link #releaseBuf} 则会忽略掉 aBuf 的修改（仅读取）；
     * <p>
     * 显而易见，对于 {@link ColumnIntMatrix} 或 {@link RowIntMatrix} ，aAbort 参数不会有任何影响，
     * 而对于其他类型，aAbort 参数可以对复杂工况做优化。
     * @author liqa
     */
    @ApiStatus.Experimental ColumnIntMatrix toBufCol(boolean aAbort);
    @ApiStatus.Experimental RowIntMatrix    toBufRow(boolean aAbort);
    @ApiStatus.Experimental void releaseBuf(@NotNull IIntMatrix aBuf, boolean aAbort);
    @ApiStatus.Experimental default ColumnIntMatrix toBufCol() {return toBufCol(false);}
    @ApiStatus.Experimental default RowIntMatrix    toBufRow() {return toBufRow(false);}
    @ApiStatus.Experimental default void releaseBuf(@NotNull IIntMatrix aBuf) {releaseBuf(aBuf, false);}
    
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
    int rowNumber();
    int columnNumber();
    int get(int aRow, int aCol);
    int getAndSet(int aRow, int aCol, int aValue); // 返回修改前的值
    void set(int aRow, int aCol, int aValue);
    IMatrix.ISize size();
    default @VisibleForTesting int nrows() {return rowNumber();}
    default @VisibleForTesting int ncols() {return columnNumber();}
    
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
    @VisibleForTesting default IIntMatrixOperation opt() {return operation();}
}
