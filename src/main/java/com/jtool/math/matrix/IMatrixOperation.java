package com.jtool.math.matrix;


import com.jtool.code.operator.IOperator1;
import com.jtool.code.operator.IOperator2;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * 任意的矩阵的运算
 * @author liqa
 */
public interface IMatrixOperation {
    /** 通用的一些运算 */
    IMatrix ebePlus         (IMatrixGetter aLHS, IMatrixGetter aRHS);
    IMatrix ebeMinus        (IMatrixGetter aLHS, IMatrixGetter aRHS);
    IMatrix ebeMultiply     (IMatrixGetter aLHS, IMatrixGetter aRHS);
    IMatrix ebeDiv          (IMatrixGetter aLHS, IMatrixGetter aRHS);
    IMatrix ebeMod          (IMatrixGetter aLHS, IMatrixGetter aRHS);
    IMatrix ebeDo           (IMatrixGetter aLHS, IMatrixGetter aRHS, IOperator2<Double> aOpt);
    
    IMatrix mapPlus         (IMatrixGetter aLHS, double aRHS);
    IMatrix mapMinus        (IMatrixGetter aLHS, double aRHS);
    IMatrix mapLMinus       (IMatrixGetter aLHS, double aRHS);
    IMatrix mapMultiply     (IMatrixGetter aLHS, double aRHS);
    IMatrix mapDiv          (IMatrixGetter aLHS, double aRHS);
    IMatrix mapLDiv         (IMatrixGetter aLHS, double aRHS);
    IMatrix mapMod          (IMatrixGetter aLHS, double aRHS);
    IMatrix mapLMod         (IMatrixGetter aLHS, double aRHS);
    IMatrix mapDo           (IMatrixGetter aLHS, IOperator1<Double> aOpt);
    
    void ebePlus2this       (IMatrixGetter aRHS);
    void ebeMinus2this      (IMatrixGetter aRHS);
    void ebeLMinus2this     (IMatrixGetter aRHS);
    void ebeMultiply2this   (IMatrixGetter aRHS);
    void ebeDiv2this        (IMatrixGetter aRHS);
    void ebeLDiv2this       (IMatrixGetter aRHS);
    void ebeMod2this        (IMatrixGetter aRHS);
    void ebeLMod2this       (IMatrixGetter aRHS);
    void ebeDo2this         (IMatrixGetter aRHS, IOperator2<Double> aOpt);
    
    void mapPlus2this       (double aRHS);
    void mapMinus2this      (double aRHS);
    void mapLMinus2this     (double aRHS);
    void mapMultiply2this   (double aRHS);
    void mapDiv2this        (double aRHS);
    void mapLDiv2this       (double aRHS);
    void mapMod2this        (double aRHS);
    void mapLMod2this       (double aRHS);
    void mapDo2this         (IOperator1<Double> aOpt);
    
    void mapFill2this       (double aRHS);
    void ebeFill2this       (IMatrixGetter aRHS);
    
    double sum();
    double mean();
    double max();
    double min();
    
    
    /** 矩阵的一些额外的运算 */
    IVector sumOfCols();
    IVector sumOfRows();
    IVector meanOfCols();
    IVector meanOfRows();
    
    IMatrix transpose();
    @VisibleForTesting default IMatrix T() {return transpose();}
    
    IMatrix refTranspose();
    @VisibleForTesting default IMatrix refT() {return refTranspose();}
    
    boolean isDiag();
    
    IVector diag();
    IVector refDiag();
}
