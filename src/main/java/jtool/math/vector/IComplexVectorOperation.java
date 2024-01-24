package jtool.math.vector;

import jtool.code.functional.IBinaryFullOperator;
import jtool.code.functional.IDoubleBinaryConsumer;
import jtool.code.functional.IUnaryFullOperator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import groovy.lang.Closure;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.*;

/**
 * 任意的复向量的运算
 * <p>
 * 这里统一只提供复数特色的运算，如需仅操作实部或虚部则使用 real()，imag() 方法获取对应的引用向量；
 * @author liqa
 */
public interface IComplexVectorOperation {
    IComplexVector plus     (IComplexVector aRHS);
    IComplexVector minus    (IComplexVector aRHS);
    IComplexVector lminus   (IComplexVector aRHS);
    IComplexVector multiply (IComplexVector aRHS);
    IComplexVector div      (IComplexVector aRHS);
    IComplexVector ldiv     (IComplexVector aRHS);
    IComplexVector operate  (IComplexVector aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    IComplexVector plus     (IVector aRHS);
    IComplexVector minus    (IVector aRHS);
    IComplexVector lminus   (IVector aRHS);
    IComplexVector multiply (IVector aRHS);
    IComplexVector div      (IVector aRHS);
    IComplexVector ldiv     (IVector aRHS);
    IComplexVector operate  (IVector aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt);
    
    IComplexVector plus     (IComplexDouble aRHS);
    IComplexVector minus    (IComplexDouble aRHS);
    IComplexVector lminus   (IComplexDouble aRHS);
    IComplexVector multiply (IComplexDouble aRHS);
    IComplexVector div      (IComplexDouble aRHS);
    IComplexVector ldiv     (IComplexDouble aRHS);
    IComplexVector plus     (double aRHS);
    IComplexVector minus    (double aRHS);
    IComplexVector lminus   (double aRHS);
    IComplexVector multiply (double aRHS);
    IComplexVector div      (double aRHS);
    IComplexVector ldiv     (double aRHS);
    IComplexVector map      (IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    
    void plus2this          (IComplexVector aRHS);
    void minus2this         (IComplexVector aRHS);
    void lminus2this        (IComplexVector aRHS);
    void multiply2this      (IComplexVector aRHS);
    void div2this           (IComplexVector aRHS);
    void ldiv2this          (IComplexVector aRHS);
    void operate2this       (IComplexVector aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    void plus2this          (IVector aRHS);
    void minus2this         (IVector aRHS);
    void lminus2this        (IVector aRHS);
    void multiply2this      (IVector aRHS);
    void div2this           (IVector aRHS);
    void ldiv2this          (IVector aRHS);
    void operate2this       (IVector aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt);
    
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
    
    IComplexVector negative();
    void negative2this();
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill               (IComplexDouble aRHS);
    void fill               (double aRHS);
    void fill               (IComplexVector aRHS);
    void fill               (IVector aRHS);
    void fill               (IComplexVectorGetter aRHS);
    void fill               (IVectorGetter aRHS);
    void assign             (Supplier<? extends IComplexDouble> aSup);
    void assign             (DoubleSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach            (Consumer<? super ComplexDouble> aCon);
    void forEach            (IDoubleBinaryConsumer aCon);
    /** Groovy stuffs */
    void fill               (Closure<?> aGroovyTask);
    void assign             (Closure<?> aGroovyTask);
    void forEach            (Closure<?> aGroovyTask);
    
    ComplexDouble sum       ();
    ComplexDouble mean      ();
    ComplexDouble prod      ();
    ComplexDouble stat      (IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    
    IComplexVector cumsum   ();
    IComplexVector cummean  ();
    IComplexVector cumprod  ();
    IComplexVector cumstat  (IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    
    /** 这里定义 a.dot(b) = a * b' */
    ComplexDouble dot   (IComplexVector aRHS);
    ComplexDouble dot   (IVector aRHS);
    double        dot   ();
    double        norm  ();
    IVector       abs   ();
    
    IComplexVector reverse     ();
    IComplexVector refReverse  ();
    void reverse2this();
    @VisibleForTesting default IComplexVector refreverse() {return refReverse();}
    
    /** 较为复杂的运算，只有遇到时专门增加，主要避免 IOperator2 使用需要新建 ComplexDouble */
    void mplus2this      (IComplexVector aRHS, double aMul);
    void mplus2this      (IComplexVector aRHS, IComplexDouble aMul);
}
