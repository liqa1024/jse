package com.jtool.math.vector;


import com.jtool.code.operator.IBooleanOperator1;
import com.jtool.code.operator.IBooleanOperator2;

/**
 * 任意的逻辑向量的运算
 * @author liqa
 */
public interface ILogicalVectorOperation {
    ILogicalVector ebeAnd   (ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS);
    ILogicalVector ebeOr    (ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS);
    ILogicalVector ebeXor   (ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS);
    ILogicalVector ebeDo    (ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt);
    
    ILogicalVector mapAnd   (ILogicalVectorGetter aLHS, boolean aRHS);
    ILogicalVector mapOr    (ILogicalVectorGetter aLHS, boolean aRHS);
    ILogicalVector mapXor   (ILogicalVectorGetter aLHS, boolean aRHS);
    ILogicalVector mapDo    (ILogicalVectorGetter aLHS, IBooleanOperator1 aOpt);
    
    void ebeAnd2this    (ILogicalVectorGetter aRHS);
    void ebeOr2this     (ILogicalVectorGetter aRHS);
    void ebeXor2this    (ILogicalVectorGetter aRHS);
    void ebeDo2this     (ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt);
    
    void mapAnd2this    (boolean aRHS);
    void mapOr2this     (boolean aRHS);
    void mapXor2this    (boolean aRHS);
    void mapDo2this     (IBooleanOperator1 aRHS);
    
    ILogicalVector not  (ILogicalVectorGetter aData);
    void not2this       ();
    
    void mapFill2this   (boolean aRHS);
    void ebeFill2this   (ILogicalVectorGetter aRHS);
    
    boolean all();
    boolean any();
    int count();
    
    ILogicalVector cumall();
    ILogicalVector cumany();
    IVector cumcount();
    
    ILogicalVector reverse();
    ILogicalVector refReverse();
}
