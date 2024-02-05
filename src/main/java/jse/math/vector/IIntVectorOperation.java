package jse.math.vector;

import jse.code.functional.ISwapper;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.*;

/**
 * 任意的整数向量的运算
 * @author liqa
 */
public interface IIntVectorOperation {
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill               (int aRHS);
    void fill               (IIntVector aRHS);
    void fill               (IIntVectorGetter aRHS);
    void assign             (IntSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach            (IntConsumer aCon);
    
    int    sum          ();
    long   exsum        ();
    double mean         ();
    double prod         ();
    int    max          ();
    int    min          ();
    double stat         (DoubleBinaryOperator aOpt);
    
    /** 向量的一些额外的运算 */
    IIntVector reverse     ();
    IIntVector refReverse  ();
    void reverse2this();
    @VisibleForTesting default IIntVector refreverse() {return refReverse();}
    
    /** 各种排序操作 */
    void sort();
    /** 注意 aComp 传入的为 index 而不是值 */
    void sort(IntBinaryOperator aComp);
    /** 使用自身作为 key 来进行排序，会顺便将自身也排序 */
    void biSort(ISwapper aSwapper);
    void biSort(ISwapper aSwapper, IntBinaryOperator aComp);
    @VisibleForTesting default void bisort(ISwapper aSwapper) {biSort(aSwapper);}
    @VisibleForTesting default void bisort(ISwapper aSwapper, IntBinaryOperator aComp) {biSort(aSwapper, aComp);}
    
    /** IntegerVector 特有的操作 */
    void shuffle();
    void shuffle(IntUnaryOperator aRng);
}
