package jtool.math.matrix;


import groovy.lang.Closure;
import jtool.code.functional.IBinaryFullOperator;
import jtool.code.functional.IDoubleBinaryConsumer;
import jtool.code.functional.IUnaryFullOperator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * 任意的复数矩阵的运算
 * @author liqa
 */
public interface IComplexMatrixOperation {
    /** 通用的一些运算 */
    IComplexMatrix plus     (IComplexMatrix aRHS);
    IComplexMatrix minus    (IComplexMatrix aRHS);
    IComplexMatrix lminus   (IComplexMatrix aRHS);
    IComplexMatrix multiply (IComplexMatrix aRHS);
    IComplexMatrix div      (IComplexMatrix aRHS);
    IComplexMatrix ldiv     (IComplexMatrix aRHS);
    IComplexMatrix operate  (IComplexMatrix aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    IComplexMatrix plus     (IMatrix aRHS);
    IComplexMatrix minus    (IMatrix aRHS);
    IComplexMatrix lminus   (IMatrix aRHS);
    IComplexMatrix multiply (IMatrix aRHS);
    IComplexMatrix div      (IMatrix aRHS);
    IComplexMatrix ldiv     (IMatrix aRHS);
    IComplexMatrix operate  (IMatrix aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt);
    
    IComplexMatrix plus     (IComplexDouble aRHS);
    IComplexMatrix minus    (IComplexDouble aRHS);
    IComplexMatrix lminus   (IComplexDouble aRHS);
    IComplexMatrix multiply (IComplexDouble aRHS);
    IComplexMatrix div      (IComplexDouble aRHS);
    IComplexMatrix ldiv     (IComplexDouble aRHS);
    IComplexMatrix plus     (double aRHS);
    IComplexMatrix minus    (double aRHS);
    IComplexMatrix lminus   (double aRHS);
    IComplexMatrix multiply (double aRHS);
    IComplexMatrix div      (double aRHS);
    IComplexMatrix ldiv     (double aRHS);
    IComplexMatrix map      (IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    
    void plus2this          (IComplexMatrix aRHS);
    void minus2this         (IComplexMatrix aRHS);
    void lminus2this        (IComplexMatrix aRHS);
    void multiply2this      (IComplexMatrix aRHS);
    void div2this           (IComplexMatrix aRHS);
    void ldiv2this          (IComplexMatrix aRHS);
    void operate2this       (IComplexMatrix aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    void plus2this          (IMatrix aRHS);
    void minus2this         (IMatrix aRHS);
    void lminus2this        (IMatrix aRHS);
    void multiply2this      (IMatrix aRHS);
    void div2this           (IMatrix aRHS);
    void ldiv2this          (IMatrix aRHS);
    void operate2this       (IMatrix aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt);
    
    void plus2this          (IComplexDouble aRHS);
    void minus2this         (IComplexDouble aRHS);
    void lminus2this        (IComplexDouble aRHS);
    void multiply2this      (IComplexDouble aRHS);
    void div2this           (IComplexDouble aRHS);
    void ldiv2this          (IComplexDouble aRHS);
    void plus2this          (double aRHS);
    void minus2this         (double aRHS);
    void lminus2this        (double aRHS);
    void multiply2this      (double aRHS);
    void div2this           (double aRHS);
    void ldiv2this          (double aRHS);
    void map2this           (IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    
    IComplexMatrix negative();
    void negative2this();
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill               (IComplexDouble aRHS);
    void fill               (double aRHS);
    void fill               (IComplexMatrix aRHS);
    void fill               (IMatrix aRHS);
    void fill               (IComplexMatrixGetter aRHS);
    void fill               (IMatrixGetter aRHS);
    void assignCol          (Supplier<? extends IComplexDouble> aSup);
    void assignCol          (DoubleSupplier aSup);
    void assignRow          (Supplier<? extends IComplexDouble> aSup);
    void assignRow          (DoubleSupplier aSup);
    void forEachCol         (Consumer<? super ComplexDouble> aCon);
    void forEachCol         (IDoubleBinaryConsumer aCon);
    void forEachRow         (Consumer<? super ComplexDouble> aCon);
    void forEachRow         (IDoubleBinaryConsumer aCon);
    /** Groovy stuffs */
    void fill               (Closure<?> aGroovyTask);
    void assignCol          (Closure<?> aGroovyTask);
    void assignRow          (Closure<?> aGroovyTask);
    void forEachCol         (Closure<?> aGroovyTask);
    void forEachRow         (Closure<?> aGroovyTask);
    
    
    /** 矩阵的一些额外的运算 */
    IComplexMatrix transpose();
    IComplexMatrix refTranspose();
    @VisibleForTesting default IComplexMatrix reftranspose() {return refTranspose();}
    
    boolean isDiag();
    @VisibleForTesting default boolean isdiag() {return isDiag();}
}
