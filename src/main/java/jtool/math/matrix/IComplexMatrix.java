package jtool.math.matrix;

import jtool.code.iterator.IComplexDoubleIterator;
import jtool.code.iterator.IComplexDoubleSetIterator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.vector.IComplexVector;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 专用的复数矩阵 </p>
 * <p> 由于完全实现工作量较大，这里暂只实现用到的接口 </p>
 * <p> 当然为了后续完善的方便，结构依旧保持一致 </p>
 */
public interface IComplexMatrix {
    /** Iterable stuffs，现在指定具体行列会仅遍历此行或者列，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便使用 */
    IComplexDoubleIterator iteratorCol();
    IComplexDoubleSetIterator setIteratorCol();
    
    /** 访问和修改部分，自带的接口 */
    ComplexDouble get_(int aRow, int aCol);
    double getReal_(int aRow, int aCol);
    double getImag_(int aRow, int aCol);
    void set_(int aRow, int aCol, IComplexDouble aValue);
    void set_(int aRow, int aCol, ComplexDouble aValue);
    void set_(int aRow, int aCol, double aValue);
    void setReal_(int aRow, int aCol, double aReal);
    void setImag_(int aRow, int aCol, double aImag);
    ComplexDouble getAndSet_(int aRow, int aCol, IComplexDouble aValue); // 返回修改前的值
    ComplexDouble getAndSet_(int aRow, int aCol, ComplexDouble aValue);
    ComplexDouble getAndSet_(int aRow, int aCol, double aValue);
    double getAndSetReal_(int aRow, int aCol, double aReal);
    double getAndSetImag_(int aRow, int aCol, double aImag);
    int rowNumber();
    int columnNumber();
    
    ComplexDouble get(int aRow, int aCol);
    double getReal(int aRow, int aCol);
    double getImag(int aRow, int aCol);
    void set(int aRow, int aCol, IComplexDouble aValue);
    void set(int aRow, int aCol, ComplexDouble aValue);
    void set(int aRow, int aCol, double aValue);
    void setReal(int aRow, int aCol, double aReal);
    void setImag(int aRow, int aCol, double aImag);
    ComplexDouble getAndSet(int aRow, int aCol, IComplexDouble aValue);
    ComplexDouble getAndSet(int aRow, int aCol, ComplexDouble aValue);
    ComplexDouble getAndSet(int aRow, int aCol, double aValue);
    double getAndSetReal(int aRow, int aCol, double aReal);
    double getAndSetImag(int aRow, int aCol, double aImag);
    IMatrix.ISize size();
    default @VisibleForTesting int nrows() {return rowNumber();}
    default @VisibleForTesting int ncols() {return columnNumber();}
    
    
    List<IComplexVector> rows();
    IComplexVector row(int aRow);
    List<IComplexVector> cols();
    IComplexVector col(int aCol);
    
    
    /** 矩阵的运算操作，默认返回新的矩阵 */
    IComplexMatrixOperation operation();
    @VisibleForTesting default IComplexMatrixOperation opt() {return operation();}
    
    void plus2this      (IComplexMatrix aRHS);
}
