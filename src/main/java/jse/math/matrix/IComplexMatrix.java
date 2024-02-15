package jse.math.matrix;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import jse.code.functional.IDoubleBinaryConsumer;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IComplexDoubleIterator;
import jse.code.iterator.IComplexDoubleSetIterator;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.vector.IComplexVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.function.*;

/**
 * @author liqa
 * <p> 专用的复数矩阵 </p>
 * <p> 由于完全实现工作量较大，这里暂只实现用到的接口 </p>
 * <p> 当然为了后续完善的方便，结构依旧保持一致 </p>
 */
public interface IComplexMatrix extends IComplexMatrixGetter {
    /** Iterable stuffs，现在指定具体行列会仅遍历此行或者列，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便使用 */
    IComplexDoubleIterator iteratorCol();
    IComplexDoubleIterator iteratorRow();
    IComplexDoubleIterator iteratorColAt(int aCol);
    IComplexDoubleIterator iteratorRowAt(int aRow);
    IComplexDoubleSetIterator setIteratorCol();
    IComplexDoubleSetIterator setIteratorRow();
    IComplexDoubleSetIterator setIteratorColAt(int aCol);
    IComplexDoubleSetIterator setIteratorRowAt(int aRow);
    
    default Iterable<ComplexDouble> iterableCol() {return () -> iteratorCol().toIterator();}
    default Iterable<ComplexDouble> iterableRow() {return () -> iteratorRow().toIterator();}
    
    List<List<ComplexDouble>> asListCols();
    List<List<ComplexDouble>> asListRows();
    
    IComplexVector asVecCol();
    IComplexVector asVecRow();
    
    /** 获取实部和虚部 */
    IMatrix real();
    IMatrix imag();
    
    /** 转为兼容性更好的 double[][][] */
    double[][][] data();
    
    /** 批量修改的接口 */
    void fill(IComplexDouble aValue);
    void fill(double aValue);
    void fill(IComplexMatrix aMatrix);
    void fill(IMatrix aMatrix);
    void fill(IComplexMatrixGetter aMatrixGetter);
    void fill(IMatrixGetter aMatrixGetter);
    void fill(double[][][] aData);
    void fill(double[][] aData);
    default void fill(Iterable<?> aRows) {fillWithRows(aRows);}
    void fillWithRows(Iterable<?> aRows);
    void fillWithCols(Iterable<?> aRows);
    void assignCol(Supplier<? extends IComplexDouble> aSup);
    void assignCol(DoubleSupplier aSup);
    void assignRow(Supplier<? extends IComplexDouble> aSup);
    void assignRow(DoubleSupplier aSup);
    void forEachCol(Consumer<? super ComplexDouble> aCon);
    void forEachCol(IDoubleBinaryConsumer aCon);
    void forEachRow(Consumer<? super ComplexDouble> aCon);
    void forEachRow(IDoubleBinaryConsumer aCon);
    /** Groovy stuff */
    void fill(@ClosureParams(value=FromString.class, options={"int,int"}) Closure<?> aGroovyTask);
    void assignCol(Closure<?> aGroovyTask);
    void assignRow(Closure<?> aGroovyTask);
    void forEachCol(@ClosureParams(value=FromString.class, options={"ComplexDouble", "double,double"}) Closure<?> aGroovyTask);
    void forEachRow(@ClosureParams(value=FromString.class, options={"ComplexDouble", "double,double"}) Closure<?> aGroovyTask);
    
    /** 访问和修改部分，自带的接口 */
    int rowNumber();
    int columnNumber();
    ComplexDouble get(int aRow, int aCol);
    double getReal(int aRow, int aCol);
    double getImag(int aRow, int aCol);
    void set(int aRow, int aCol, IComplexDouble aValue);
    void set(int aRow, int aCol, ComplexDouble aValue);
    void set(int aRow, int aCol, double aValue);
    void set(int aRow, int aCol, double aReal, double aImag);
    void setReal(int aRow, int aCol, double aReal);
    void setImag(int aRow, int aCol, double aImag);
    ComplexDouble getAndSet(int aRow, int aCol, IComplexDouble aValue); // 返回修改前的值
    ComplexDouble getAndSet(int aRow, int aCol, ComplexDouble aValue);
    ComplexDouble getAndSet(int aRow, int aCol, double aValue);
    ComplexDouble getAndSet(int aRow, int aCol, double aReal, double aImag);
    double getAndSetReal(int aRow, int aCol, double aReal);
    double getAndSetImag(int aRow, int aCol, double aImag);
    IMatrix.ISize size();
    default @VisibleForTesting int nrows() {return rowNumber();}
    default @VisibleForTesting int ncols() {return columnNumber();}
    
    /** 附加一些额外的单元素操作，对于一般的只提供一个 update 的接口 */
    void update(int aRow, int aCol, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    void updateReal(int aRow, int aCol, DoubleUnaryOperator aRealOpt);
    void updateImag(int aRow, int aCol, DoubleUnaryOperator aImagOpt);
    ComplexDouble getAndUpdate(int aRow, int aCol, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    double getAndUpdateReal(int aRow, int aCol, DoubleUnaryOperator aRealOpt);
    double getAndUpdateImag(int aRow, int aCol, DoubleUnaryOperator aImagOpt);
    
    List<IComplexVector> rows();
    IComplexVector row(int aRow);
    List<IComplexVector> cols();
    IComplexVector col(int aCol);
    
    
    IComplexMatrix copy();
    
    /** 矩阵的运算操作，默认返回新的矩阵 */
    IComplexMatrixOperation operation();
    @VisibleForTesting default IComplexMatrixOperation opt() {return operation();}
    
    /** Groovy 的部分，增加矩阵基本的运算操作，现在也归入内部使用 */
    IComplexMatrix plus     (IComplexDouble aRHS);
    IComplexMatrix minus    (IComplexDouble aRHS);
    IComplexMatrix multiply (IComplexDouble aRHS);
    IComplexMatrix div      (IComplexDouble aRHS);
    IComplexMatrix plus     (double aRHS);
    IComplexMatrix minus    (double aRHS);
    IComplexMatrix multiply (double aRHS);
    IComplexMatrix div      (double aRHS);
    
    IComplexMatrix plus     (IComplexMatrix aRHS);
    IComplexMatrix minus    (IComplexMatrix aRHS);
    IComplexMatrix multiply (IComplexMatrix aRHS);
    IComplexMatrix div      (IComplexMatrix aRHS);
    IComplexMatrix plus     (IMatrix aRHS);
    IComplexMatrix minus    (IMatrix aRHS);
    IComplexMatrix multiply (IMatrix aRHS);
    IComplexMatrix div      (IMatrix aRHS);
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    void plus2this      (IComplexDouble aRHS);
    void minus2this     (IComplexDouble aRHS);
    void multiply2this  (IComplexDouble aRHS);
    void div2this       (IComplexDouble aRHS);
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    
    void plus2this      (IComplexMatrix aRHS);
    void minus2this     (IComplexMatrix aRHS);
    void multiply2this  (IComplexMatrix aRHS);
    void div2this       (IComplexMatrix aRHS);
    void plus2this      (IMatrix aRHS);
    void minus2this     (IMatrix aRHS);
    void multiply2this  (IMatrix aRHS);
    void div2this       (IMatrix aRHS);
    
    IComplexMatrix negative();
    void negative2this();
}
