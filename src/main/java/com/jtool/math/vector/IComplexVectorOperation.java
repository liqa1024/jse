package com.jtool.math.vector;


import com.jtool.code.functional.*;
import com.jtool.code.iterator.IComplexDoubleSetOnlyIterator;
import com.jtool.math.ComplexDouble;
import com.jtool.math.IComplexDouble;
import groovy.lang.Closure;

import java.util.function.Supplier;

/**
 * 任意的复向量的运算
 * <p>
 * 这里统一只提供复数特色的运算，如需仅操作实部或虚部则使用 real()，imag() 方法获取对应的引用向量；
 * @author liqa
 */
public interface IComplexVectorOperation {
    /** 通用的一些运算 */
    IComplexVector plus     (IComplexVectorGetter aRHS);
    IComplexVector minus    (IComplexVectorGetter aRHS);
    IComplexVector lminus   (IComplexVectorGetter aRHS);
    IComplexVector multiply (IComplexVectorGetter aRHS);
    IComplexVector div      (IComplexVectorGetter aRHS);
    IComplexVector ldiv     (IComplexVectorGetter aRHS);
    IComplexVector operate  (IComplexVectorGetter aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    
    IComplexVector plus     (IVectorGetter aRHS);
    IComplexVector minus    (IVectorGetter aRHS);
    IComplexVector lminus   (IVectorGetter aRHS);
    IComplexVector multiply (IVectorGetter aRHS);
    IComplexVector div      (IVectorGetter aRHS);
    IComplexVector ldiv     (IVectorGetter aRHS);
    IComplexVector operate  (IVectorGetter aRHS, IDoubleOperator2 aOpt);
    
    IComplexVector plus     (IComplexDouble aRHS);
    IComplexVector minus    (IComplexDouble aRHS);
    IComplexVector lminus   (IComplexDouble aRHS);
    IComplexVector multiply (IComplexDouble aRHS);
    IComplexVector div      (IComplexDouble aRHS);
    IComplexVector ldiv     (IComplexDouble aRHS);
    IComplexVector map      (IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt);
    
    IComplexVector plus     (double aRHS);
    IComplexVector minus    (double aRHS);
    IComplexVector lminus   (double aRHS);
    IComplexVector multiply (double aRHS);
    IComplexVector div      (double aRHS);
    IComplexVector ldiv     (double aRHS);
    IComplexVector map      (IDoubleOperator1 aOpt);
    
    void plus2this          (IComplexVectorGetter aRHS);
    void minus2this         (IComplexVectorGetter aRHS);
    void lminus2this        (IComplexVectorGetter aRHS);
    void multiply2this      (IComplexVectorGetter aRHS);
    void div2this           (IComplexVectorGetter aRHS);
    void ldiv2this          (IComplexVectorGetter aRHS);
    void operate2this       (IComplexVectorGetter aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    
    void plus2this          (IVectorGetter aRHS);
    void minus2this         (IVectorGetter aRHS);
    void lminus2this        (IVectorGetter aRHS);
    void multiply2this      (IVectorGetter aRHS);
    void div2this           (IVectorGetter aRHS);
    void ldiv2this          (IVectorGetter aRHS);
    void operate2this       (IVectorGetter aRHS, IDoubleOperator2 aOpt);
    
    void plus2this          (double aRHS);
    void minus2this         (double aRHS);
    void lminus2this        (double aRHS);
    void multiply2this      (double aRHS);
    void div2this           (double aRHS);
    void ldiv2this          (double aRHS);
    void map2this           (IDoubleOperator1 aOpt);
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill               (IComplexDouble aRHS);
    void fill               (double aRHS);
    void fill               (IComplexVectorGetter aRHS);
    void fill               (IVectorGetter aRHS);
    void assign             (Supplier<? extends IComplexDouble> aSup);
    void assign             (IDoubleSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach            (IConsumer1<? super ComplexDouble> aCon);
    void forEach            (IDoubleConsumer2 aCon);
    /** Groovy stuffs */
    void assign             (Closure<?> aGroovyTask);
    void forEach            (Closure<?> aGroovyTask);
    
    ComplexDouble sum       ();
    ComplexDouble mean      ();
    ComplexDouble prod      ();
    ComplexDouble stat      (IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    
    IComplexVector cumsum   ();
    IComplexVector cummean  ();
    IComplexVector cumprod  ();
    IComplexVector cumstat  (IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt);
    
    /** 向量的一些额外的运算 */
    ComplexDouble dot   (IVectorGetter aRHS);
    ComplexDouble dot   (IComplexVectorGetter aRHS);
    double        dot   ();
    double        norm  ();
    IVector       abs   ();
    
    IComplexVector reverse     ();
    IComplexVector refReverse  ();
}
