package com.jtool.math.vector;


import com.jtool.code.functional.*;

/**
 * 任意的逻辑向量的运算
 * @author liqa
 */
public interface ILogicalVectorOperation {
    ILogicalVector and      (ILogicalVectorGetter aRHS);
    ILogicalVector or       (ILogicalVectorGetter aRHS);
    ILogicalVector xor      (ILogicalVectorGetter aRHS);
    ILogicalVector operate  (ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt);
    
    ILogicalVector and      (boolean aRHS);
    ILogicalVector or       (boolean aRHS);
    ILogicalVector xor      (boolean aRHS);
    ILogicalVector map      (IBooleanOperator1 aOpt);
    
    void and2this           (ILogicalVectorGetter aRHS);
    void or2this            (ILogicalVectorGetter aRHS);
    void xor2this           (ILogicalVectorGetter aRHS);
    void operate2this       (ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt);
    
    void and2this           (boolean aRHS);
    void or2this            (boolean aRHS);
    void xor2this           (boolean aRHS);
    void map2this           (IBooleanOperator1 aRHS);
    
    ILogicalVector not      ();
    void not2this           ();
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill               (boolean aRHS);
    void fill               (ILogicalVectorGetter aRHS);
    void assign             (IBooleanSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach            (IBooleanConsumer1 aCon);
    
    boolean all             ();
    boolean any             ();
    int count               ();
    
    ILogicalVector cumall   ();
    ILogicalVector cumany   ();
    IVector cumcount        ();
    
    ILogicalVector reverse      ();
    ILogicalVector refReverse   ();
}
