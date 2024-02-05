package jse.math.vector;


import jse.code.functional.IBooleanBinaryOperator;
import jse.code.functional.IBooleanConsumer;
import jse.code.functional.IBooleanUnaryOperator;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.BooleanSupplier;

/**
 * 任意的逻辑向量的运算
 * @author liqa
 */
public interface ILogicalVectorOperation {
    ILogicalVector and      (ILogicalVector aRHS);
    ILogicalVector or       (ILogicalVector aRHS);
    ILogicalVector xor      (ILogicalVector aRHS);
    ILogicalVector operate  (ILogicalVector aRHS, IBooleanBinaryOperator aOpt);
    
    ILogicalVector and      (boolean aRHS);
    ILogicalVector or       (boolean aRHS);
    ILogicalVector xor      (boolean aRHS);
    ILogicalVector map      (IBooleanUnaryOperator aOpt);
    
    void and2this           (ILogicalVector aRHS);
    void or2this            (ILogicalVector aRHS);
    void xor2this           (ILogicalVector aRHS);
    void operate2this       (ILogicalVector aRHS, IBooleanBinaryOperator aOpt);
    
    void and2this           (boolean aRHS);
    void or2this            (boolean aRHS);
    void xor2this           (boolean aRHS);
    void map2this           (IBooleanUnaryOperator aRHS);
    
    ILogicalVector not      ();
    void not2this           ();
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill               (boolean aRHS);
    void fill               (ILogicalVector aRHS);
    void fill               (ILogicalVectorGetter aRHS);
    void assign             (BooleanSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach            (IBooleanConsumer aCon);
    
    boolean all             ();
    boolean any             ();
    int count               ();
    
    ILogicalVector cumall   ();
    ILogicalVector cumany   ();
    IVector cumcount        ();
    
    ILogicalVector reverse      ();
    ILogicalVector refReverse   ();
    void reverse2this();
    @VisibleForTesting default ILogicalVector refreverse() {return refReverse();}
}
