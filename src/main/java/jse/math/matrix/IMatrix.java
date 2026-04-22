package jse.math.matrix;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import jep.NDArray;
import jse.code.UT;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetIterator;
import jse.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

/**
 * 实数矩阵，返回类型 {@code double}
 * @author liqa
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
    
    /**
     * 转换为 numpy 的数组 {@link NDArray}，在 java 侧根据具体向量类型可能不会进行值拷贝，由于
     * {@link NDArray} 内部实现特性，在 python 中总是会再经历一次值拷贝，此时使用不会有引用问题。
     * @return numpy 的数组 {@link NDArray}
     */
    NDArray<double[]> numpy();
    /** 转为兼容性更好的 {@code double[][]} */
    double[][] data();
    
    /** 批量修改的接口 */
    void fill(double aValue);
    void fill(IMatrix aMatrix);
    void fill(IMatrixGetter aMatrixGetter);
    void fill(double[][] aData);
    default void fill(Iterable<?> aRows) {fillWithRows(aRows);}
    void fillWithRows(Iterable<?> aRows);
    void fillWithCols(Iterable<?> aCols);
    void assignCol(DoubleSupplier aSup);
    void assignRow(DoubleSupplier aSup);
    void forEachCol(DoubleConsumer aCon);
    void forEachRow(DoubleConsumer aCon);
    /** Groovy stuff */
    default void fill(@ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {fill((i, j) -> UT.Code.doubleValue(aGroovyTask.call(i, j)));}
    default void assignCol(final Closure<? extends Number> aGroovyTask) {assignCol(() -> UT.Code.doubleValue(aGroovyTask.call()));}
    default void assignRow(final Closure<? extends Number> aGroovyTask) {assignRow(() -> UT.Code.doubleValue(aGroovyTask.call()));}
    
    /** 访问和修改部分，自带的接口 */
    int nrows();
    int ncols();
    @Override double get(int aRow, int aCol);
    double getAndSet(int aRow, int aCol, double aValue); // 返回修改前的值
    void set(int aRow, int aCol, double aValue);
    ISize size();
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    void update(int aRow, int aCol, DoubleUnaryOperator aOpt);
    double getAndUpdate(int aRow, int aCol, DoubleUnaryOperator aOpt);
    
    
    List<? extends IVector> rows();
    IVector row(int aRow);
    List<? extends IVector> cols();
    IVector col(int aCol);
    
    
    IMatrix copy();
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    IMatrixSlicer slicer();
    IMatrixSlicer refSlicer();
    
    /** 矩阵的运算操作，默认返回新的矩阵 */
    IMatrixOperation operation();
    @VisibleForTesting default IMatrixOperation op() {return operation();}
    
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
    
    IMatrix abs();
    void abs2this();
    IMatrix negative();
    void negative2this();
}
