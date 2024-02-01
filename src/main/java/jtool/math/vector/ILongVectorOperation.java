package jtool.math.vector;

import jtool.code.functional.ISwapper;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.DoubleBinaryOperator;
import java.util.function.IntBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * 任意的长整数向量的运算
 * @author liqa
 */
public interface ILongVectorOperation {
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill               (long aRHS);
    void fill               (ILongVector aRHS);
    void fill               (ILongVectorGetter aRHS);
    void assign             (LongSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach            (LongConsumer aCon);
    
    long   sum          ();
    double mean         ();
    double prod         ();
    long   max          ();
    long   min          ();
    double stat(DoubleBinaryOperator aOpt);
    
    /** 向量的一些额外的运算 */
    ILongVector reverse     ();
    ILongVector refReverse  ();
    void reverse2this();
    @VisibleForTesting default ILongVector refreverse() {return refReverse();}
    
    /** 各种排序操作 */
    void sort();
    /** 注意 aComp 传入的为 index 而不是值 */
    void sort(IntBinaryOperator aComp);
    /** 使用自身作为 key 来进行排序，会顺便将自身也排序 */
    void biSort(ISwapper aSwapper);
    void biSort(ISwapper aSwapper, IntBinaryOperator aComp);
    @VisibleForTesting default void bisort(ISwapper aSwapper) {biSort(aSwapper);}
    @VisibleForTesting default void bisort(ISwapper aSwapper, IntBinaryOperator aComp) {biSort(aSwapper, aComp);}
}
