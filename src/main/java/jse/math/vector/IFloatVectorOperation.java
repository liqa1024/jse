package jse.math.vector;

import jse.code.functional.IFloatConsumer;
import jse.code.functional.IFloatSupplier;
import org.jetbrains.annotations.VisibleForTesting;

/**
 * 任意的单精度向量的运算
 * @author liqa
 */
public interface IFloatVectorOperation {
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill               (float aRHS);
    void fill               (IFloatVector aRHS);
    void fill               (IFloatVectorGetter aRHS);
    void assign             (IFloatSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach            (IFloatConsumer aCon);
    
    /** 向量的一些额外的运算 */
    IFloatVector reverse    ();
    IFloatVector refReverse ();
    void reverse2this();
    @VisibleForTesting default IFloatVector refreverse() {return refReverse();}
}
