package com.jtool.math.matrix;


import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleOperator1;
import com.jtool.code.functional.IDoubleOperator2;
import com.jtool.code.functional.IDoubleSupplier;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * 任意的矩阵的运算
 * @author liqa
 */
public interface IMatrixOperation {
    /** 通用的一些运算 */
    IMatrix plus        (IMatrixGetter aRHS);
    IMatrix minus       (IMatrixGetter aRHS);
    IMatrix lminus      (IMatrixGetter aRHS);
    IMatrix multiply    (IMatrixGetter aRHS);
    IMatrix div         (IMatrixGetter aRHS);
    IMatrix ldiv        (IMatrixGetter aRHS);
    IMatrix mod         (IMatrixGetter aRHS);
    IMatrix lmod        (IMatrixGetter aRHS);
    IMatrix operate     (IMatrixGetter aRHS, IDoubleOperator2 aOpt);
    
    IMatrix plus        (double aRHS);
    IMatrix minus       (double aRHS);
    IMatrix lminus      (double aRHS);
    IMatrix multiply    (double aRHS);
    IMatrix div         (double aRHS);
    IMatrix ldiv        (double aRHS);
    IMatrix mod         (double aRHS);
    IMatrix lmod        (double aRHS);
    IMatrix map         (IDoubleOperator1 aOpt);
    
    void plus2this      (IMatrixGetter aRHS);
    void minus2this     (IMatrixGetter aRHS);
    void lminus2this    (IMatrixGetter aRHS);
    void multiply2this  (IMatrixGetter aRHS);
    void div2this       (IMatrixGetter aRHS);
    void ldiv2this      (IMatrixGetter aRHS);
    void mod2this       (IMatrixGetter aRHS);
    void lmod2this      (IMatrixGetter aRHS);
    void operate2this   (IMatrixGetter aRHS, IDoubleOperator2 aOpt);
    
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void lminus2this    (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void ldiv2this      (double aRHS);
    void mod2this       (double aRHS);
    void lmod2this      (double aRHS);
    void map2this       (IDoubleOperator1 aOpt);
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill           (double aRHS);
    void fill           (IMatrixGetter aRHS);
    void assignCol      (IDoubleSupplier aSup);
    void assignRow      (IDoubleSupplier aSup);
    void forEachCol     (IDoubleConsumer1 aCon);
    void forEachRow     (IDoubleConsumer1 aCon);
    
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
    @VisibleForTesting default IMatrix T() {return transpose();}
    
    IMatrix refTranspose();
    @VisibleForTesting default IMatrix refT() {return refTranspose();}
    
    boolean isDiag();
}
