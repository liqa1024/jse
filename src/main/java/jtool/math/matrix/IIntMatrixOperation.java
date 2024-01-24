package jtool.math.matrix;


import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * 任意的整数矩阵的运算
 * @author liqa
 */
public interface IIntMatrixOperation {
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill           (int aRHS);
    void fill           (IIntMatrix aRHS);
    void fill           (IIntMatrixGetter aRHS);
    void assignCol      (IntSupplier aSup);
    void assignRow      (IntSupplier aSup);
    void forEachCol     (IntConsumer aCon);
    void forEachRow     (IntConsumer aCon);
    
    
    /** 矩阵的一些额外的运算 */
    IIntMatrix transpose();
    IIntMatrix refTranspose();
    @VisibleForTesting default IIntMatrix reftranspose() {return refTranspose();}
    
    boolean isDiag();
    @VisibleForTesting default boolean isdiag() {return isDiag();}
}
