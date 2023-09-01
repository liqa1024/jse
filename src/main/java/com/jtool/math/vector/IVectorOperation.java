package com.jtool.math.vector;


import com.jtool.code.functional.*;

/**
 * 任意的实向量的运算
 * @author liqa
 */
public interface IVectorOperation {
    /** 通用的一些运算 */
    IVector plus        (IVectorGetter aRHS);
    IVector minus       (IVectorGetter aRHS);
    IVector lminus      (IVectorGetter aRHS);
    IVector multiply    (IVectorGetter aRHS);
    IVector div         (IVectorGetter aRHS);
    IVector ldiv        (IVectorGetter aRHS);
    IVector mod         (IVectorGetter aRHS);
    IVector lmod        (IVectorGetter aRHS);
    IVector operate     (IVectorGetter aRHS, IDoubleOperator2 aOpt);
    
    IVector plus        (double aRHS);
    IVector minus       (double aRHS);
    IVector lminus      (double aRHS);
    IVector multiply    (double aRHS);
    IVector div         (double aRHS);
    IVector ldiv        (double aRHS);
    IVector mod         (double aRHS);
    IVector lmod        (double aRHS);
    IVector map         (IDoubleOperator1 aOpt);
    
    void plus2this      (IVectorGetter aRHS);
    void minus2this     (IVectorGetter aRHS);
    void lminus2this    (IVectorGetter aRHS);
    void multiply2this  (IVectorGetter aRHS);
    void div2this       (IVectorGetter aRHS);
    void ldiv2this      (IVectorGetter aRHS);
    void mod2this       (IVectorGetter aRHS);
    void lmod2this      (IVectorGetter aRHS);
    void operate2this   (IVectorGetter aRHS, IDoubleOperator2 aOpt);
    
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
    void fill           (IVectorGetter aRHS);
    void assign         (IDoubleSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach        (IDoubleConsumer1 aCon);
    
    double sum          ();
    double mean         ();
    double prod         ();
    double max          ();
    double min          ();
    double stat         (IDoubleOperator2 aOpt);
    
    IVector cumsum      ();
    IVector cummean     ();
    IVector cumprod     ();
    IVector cummax      ();
    IVector cummin      ();
    IVector cumstat     (IDoubleOperator2 aOpt);
    
    /** 获取逻辑结果的运算 */
    ILogicalVector equal            (IVectorGetter aRHS);
    ILogicalVector greater          (IVectorGetter aRHS);
    ILogicalVector greaterOrEqual   (IVectorGetter aRHS);
    ILogicalVector less             (IVectorGetter aRHS);
    ILogicalVector lessOrEqual      (IVectorGetter aRHS);
    
    ILogicalVector equal            (double aRHS);
    ILogicalVector greater          (double aRHS);
    ILogicalVector greaterOrEqual   (double aRHS);
    ILogicalVector less             (double aRHS);
    ILogicalVector lessOrEqual      (double aRHS);
    
    ILogicalVector compare          (IVectorGetter aRHS, IComparator aOpt);
    ILogicalVector check            (IChecker aOpt);
    
    /** 向量的一些额外的运算 */
    double dot  (IVectorGetter aRHS);
    double dot  ();
    double norm ();
    
    IVector reverse     ();
    IVector refReverse  ();
}
