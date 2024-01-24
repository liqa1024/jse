package jtool.math.matrix;

import jtool.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;
import java.util.function.*;

/**
 * 任意的矩阵的运算
 * @author liqa
 */
public interface IMatrixOperation {
    /** 通用的一些运算 */
    IMatrix plus        (IMatrix aRHS);
    IMatrix minus       (IMatrix aRHS);
    IMatrix lminus      (IMatrix aRHS);
    IMatrix multiply    (IMatrix aRHS);
    IMatrix div         (IMatrix aRHS);
    IMatrix ldiv        (IMatrix aRHS);
    IMatrix mod         (IMatrix aRHS);
    IMatrix lmod        (IMatrix aRHS);
    IMatrix operate     (IMatrix aRHS, DoubleBinaryOperator aOpt);
    
    IMatrix plus        (double aRHS);
    IMatrix minus       (double aRHS);
    IMatrix lminus      (double aRHS);
    IMatrix multiply    (double aRHS);
    IMatrix div         (double aRHS);
    IMatrix ldiv        (double aRHS);
    IMatrix mod         (double aRHS);
    IMatrix lmod        (double aRHS);
    IMatrix map         (DoubleUnaryOperator aOpt);
    
    void plus2this      (IMatrix aRHS);
    void minus2this     (IMatrix aRHS);
    void lminus2this    (IMatrix aRHS);
    void multiply2this  (IMatrix aRHS);
    void div2this       (IMatrix aRHS);
    void ldiv2this      (IMatrix aRHS);
    void mod2this       (IMatrix aRHS);
    void lmod2this      (IMatrix aRHS);
    void operate2this   (IMatrix aRHS, DoubleBinaryOperator aOpt);
    
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void lminus2this    (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void ldiv2this      (double aRHS);
    void mod2this       (double aRHS);
    void lmod2this      (double aRHS);
    void map2this       (DoubleUnaryOperator aOpt);
    
    IMatrix negative();
    void negative2this();
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill           (double aRHS);
    void fill           (IMatrix aRHS);
    void fill           (IMatrixGetter aRHS);
    void assignCol      (DoubleSupplier aSup);
    void assignRow      (DoubleSupplier aSup);
    void forEachCol     (DoubleConsumer aCon);
    void forEachRow     (DoubleConsumer aCon);
    
    double sum          ();
    double mean         ();
    double max          ();
    double min          ();
    
    
    /** 矩阵的一些额外的运算 */
    IVector sumOfCols   ();
    IVector sumOfRows   ();
    IVector meanOfCols  ();
    IVector meanOfRows  ();
    
    IMatrix transpose();
    IMatrix refTranspose();
    @VisibleForTesting default IMatrix reftranspose() {return refTranspose();}
    
    boolean isDiag();
    @VisibleForTesting default boolean isdiag() {return isDiag();}
}
