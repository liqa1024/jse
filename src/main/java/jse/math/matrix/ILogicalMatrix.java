package jse.math.matrix;

import jep.NDArray;
import jse.code.functional.IBooleanConsumer;
import jse.code.functional.IBooleanUnaryOperator;
import jse.code.iterator.IBooleanIterator;
import jse.code.iterator.IBooleanSetIterator;
import jse.math.vector.ILogicalVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.function.BooleanSupplier;

/**
 * 逻辑矩阵，返回类型 {@code boolean}
 * @author liqa
 */
public interface ILogicalMatrix extends ILogicalMatrixGetter {
    /** Iterable stuffs，现在指定具体行列会仅遍历此行或者列，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便使用 */
    IBooleanIterator iteratorCol();
    IBooleanIterator iteratorRow();
    IBooleanIterator iteratorColAt(int aCol);
    IBooleanIterator iteratorRowAt(int aRow);
    IBooleanSetIterator setIteratorCol();
    IBooleanSetIterator setIteratorRow();
    IBooleanSetIterator setIteratorColAt(int aCol);
    IBooleanSetIterator setIteratorRowAt(int aRow);
    
    default Iterable<Boolean> iterableCol() {return () -> iteratorCol().toIterator();}
    default Iterable<Boolean> iterableRow() {return () -> iteratorRow().toIterator();}
    
    List<List<Boolean>> asListCols();
    List<List<Boolean>> asListRows();
    IMatrix asMat();
    IIntMatrix asIntMat();
    
    ILogicalVector asVecCol();
    ILogicalVector asVecRow();
    
    /**
     * 转换为 numpy 的数组 {@link NDArray}，在 java 侧根据具体向量类型可能不会进行值拷贝，由于
     * {@link NDArray} 内部实现特性，在 python 中总是会再经历一次值拷贝，此时使用不会有引用问题。
     * @return numpy 的数组 {@link NDArray}
     */
    NDArray<boolean[]> numpy();
    /** 转为兼容性更好的 {@code boolean[][]} */
    boolean[][] data();
    
    /** 批量修改的接口 */
    void fill(boolean aValue);
    void fill(ILogicalMatrix aMatrix);
    void fill(ILogicalMatrixGetter aMatrixGetter);
    void fill(boolean[][] aData);
    default void fill(Iterable<?> aRows) {fillWithRows(aRows);}
    void fillWithRows(Iterable<?> aRows);
    void fillWithCols(Iterable<?> aCols);
    void assignCol(BooleanSupplier aSup);
    void assignRow(BooleanSupplier aSup);
    void forEachCol(IBooleanConsumer aCon);
    void forEachRow(IBooleanConsumer aCon);
    
    /** 访问和修改部分，自带的接口 */
    int nrows();
    int ncols();
    @Override boolean get(int aRow, int aCol);
    boolean getAndSet(int aRow, int aCol, boolean aValue); // 返回修改前的值
    void set(int aRow, int aCol, boolean aValue);
    IMatrix.ISize size();
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    void update(int aRow, int aCol, IBooleanUnaryOperator aOpt);
    boolean getAndUpdate(int aRow, int aCol, IBooleanUnaryOperator aOpt);
    
    List<? extends ILogicalVector> rows();
    ILogicalVector row(int aRow);
    List<? extends ILogicalVector> cols();
    ILogicalVector col(int aCol);
    
    
    ILogicalMatrix copy();
    
    /** 矩阵的运算操作，默认返回新的矩阵 */
    ILogicalMatrixOperation operation();
    @VisibleForTesting default ILogicalMatrixOperation op() {return operation();}
}
