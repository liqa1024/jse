package com.jtool.math.vector;


import com.jtool.code.functional.*;

/**
 * 任意的实向量的运算
 * @author liqa
 */
public interface IVectorOperation {
    /** 通用的一些运算 */
    IVector plus        (IVector aRHS);
    IVector minus       (IVector aRHS);
    IVector lminus      (IVector aRHS);
    IVector multiply    (IVector aRHS);
    IVector div         (IVector aRHS);
    IVector ldiv        (IVector aRHS);
    IVector mod         (IVector aRHS);
    IVector lmod        (IVector aRHS);
    IVector operate     (IVector aRHS, IDoubleOperator2 aOpt);
    
    IVector plus        (double aRHS);
    IVector minus       (double aRHS);
    IVector lminus      (double aRHS);
    IVector multiply    (double aRHS);
    IVector div         (double aRHS);
    IVector ldiv        (double aRHS);
    IVector mod         (double aRHS);
    IVector lmod        (double aRHS);
    IVector map         (IDoubleOperator1 aOpt);
    
    void plus2this      (IVector aRHS);
    void minus2this     (IVector aRHS);
    void lminus2this    (IVector aRHS);
    void multiply2this  (IVector aRHS);
    void div2this       (IVector aRHS);
    void ldiv2this      (IVector aRHS);
    void mod2this       (IVector aRHS);
    void lmod2this      (IVector aRHS);
    void operate2this   (IVector aRHS, IDoubleOperator2 aOpt);
    
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void lminus2this    (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void ldiv2this      (double aRHS);
    void mod2this       (double aRHS);
    void lmod2this      (double aRHS);
    void map2this       (IDoubleOperator1 aOpt);
    
    IVector negative();
    void negative2this();
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill           (double aRHS);
    void fill           (IVector aRHS);
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
    ILogicalVector equal            (IVector aRHS);
    ILogicalVector greater          (IVector aRHS);
    ILogicalVector greaterOrEqual   (IVector aRHS);
    ILogicalVector less             (IVector aRHS);
    ILogicalVector lessOrEqual      (IVector aRHS);
    
    ILogicalVector equal            (double aRHS);
    ILogicalVector greater          (double aRHS);
    ILogicalVector greaterOrEqual   (double aRHS);
    ILogicalVector less             (double aRHS);
    ILogicalVector lessOrEqual      (double aRHS);
    
    ILogicalVector compare          (IVector aRHS, IComparator aOpt);
    ILogicalVector check            (IChecker aOpt);
    
    /** 向量的一些额外的运算 */
    double dot  (IVector aRHS);
    double dot  ();
    double norm ();
    
    IVector reverse     ();
    IVector refReverse  ();
}
