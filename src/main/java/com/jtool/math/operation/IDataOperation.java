package com.jtool.math.operation;


import com.jtool.code.operator.IOperator1;
import com.jtool.code.operator.IOperator2;

/**
 * 任意的通用的数据运算操作，为了代码一致性和 groovy 的重载运算符选择相同的名称；
 * 这里直接认为数据类型为 double，对于复数或者其他情况直接使用专门的另外的运算器即可
 * @author liqa
 * @param <R> 返回数据类型，一般有 R extends T（不强制）
 * @param <T> 输入的数据类型
 */
public interface IDataOperation<R, T> {
    R ebePlus       (T aLHS, T aRHS);
    R ebeMinus      (T aLHS, T aRHS);
    R ebeMultiply   (T aLHS, T aRHS);
    R ebeDiv        (T aLHS, T aRHS);
    R ebeMod        (T aLHS, T aRHS);
    R ebeDo         (T aLHS, T aRHS, IOperator2<Double> aOpt);
    
    R mapPlus       (T aLHS, double aRHS);
    R mapMinus      (T aLHS, double aRHS);
    R mapLMinus     (T aLHS, double aRHS);
    R mapMultiply   (T aLHS, double aRHS);
    R mapDiv        (T aLHS, double aRHS);
    R mapLDiv       (T aLHS, double aRHS);
    R mapMod        (T aLHS, double aRHS);
    R mapLMod       (T aLHS, double aRHS);
    R mapDo         (T aLHS, IOperator1<Double> aOpt);
    
    void ebePlus2this       (T aRHS);
    void ebeMinus2this      (T aRHS);
    void ebeLMinus2this     (T aRHS);
    void ebeMultiply2this   (T aRHS);
    void ebeDiv2this        (T aRHS);
    void ebeLDiv2this       (T aRHS);
    void ebeMod2this        (T aRHS);
    void ebeLMod2this       (T aRHS);
    void ebeDo2this         (T aRHS, IOperator2<Double> aOpt);
    
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
    void ebeFill2this       (T aRHS);
    
    double sum();
    double mean();
    double max();
    double min();
}
