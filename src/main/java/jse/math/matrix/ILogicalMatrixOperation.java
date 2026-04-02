package jse.math.matrix;


import jse.code.functional.IBooleanConsumer;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.BooleanSupplier;

/**
 * 任意的逻辑矩阵的运算
 * @author liqa
 */
public interface ILogicalMatrixOperation {
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill           (boolean aRHS);
    void fill           (ILogicalMatrix aRHS);
    void fill           (ILogicalMatrixGetter aRHS);
    void assignCol      (BooleanSupplier aSup);
    void assignRow      (BooleanSupplier aSup);
    void forEachCol     (IBooleanConsumer aCon);
    void forEachRow     (IBooleanConsumer aCon);
    
    
    /** 矩阵的一些额外的运算 */
    ILogicalMatrix transpose();
    ILogicalMatrix refTranspose();
    @VisibleForTesting default ILogicalMatrix reftranspose() {return refTranspose();}
    
    boolean isDiag();
    @VisibleForTesting default boolean isdiag() {return isDiag();}
}
