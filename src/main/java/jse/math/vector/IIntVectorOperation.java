package jse.math.vector;

import jse.code.functional.ISwapper;
import jse.code.random.IRandom;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.*;

/**
 * 任意的整数向量的运算
 * @author liqa
 */
public interface IIntVectorOperation {
    /** 通用的一些运算 */
    IIntVector plus     (IIntVector aRHS);
    IIntVector minus    (IIntVector aRHS);
    IIntVector lminus   (IIntVector aRHS);
    IIntVector multiply (IIntVector aRHS);
    IIntVector div      (IIntVector aRHS);
    IIntVector ldiv     (IIntVector aRHS);
    IIntVector mod      (IIntVector aRHS);
    IIntVector lmod     (IIntVector aRHS);
    IIntVector operate  (IIntVector aRHS, IntBinaryOperator aOpt);
    
    IIntVector plus     (int aRHS);
    IIntVector minus    (int aRHS);
    IIntVector lminus   (int aRHS);
    IIntVector multiply (int aRHS);
    IIntVector div      (int aRHS);
    IIntVector ldiv     (int aRHS);
    IIntVector mod      (int aRHS);
    IIntVector lmod     (int aRHS);
    IIntVector map      (IntUnaryOperator aOpt);
    
    void plus2this      (IIntVector aRHS);
    void minus2this     (IIntVector aRHS);
    void lminus2this    (IIntVector aRHS);
    void multiply2this  (IIntVector aRHS);
    void div2this       (IIntVector aRHS);
    void ldiv2this      (IIntVector aRHS);
    void mod2this       (IIntVector aRHS);
    void lmod2this      (IIntVector aRHS);
    void operate2this   (IIntVector aRHS, IntBinaryOperator aOpt);
    
    void plus2this      (int aRHS);
    void minus2this     (int aRHS);
    void lminus2this    (int aRHS);
    void multiply2this  (int aRHS);
    void div2this       (int aRHS);
    void ldiv2this      (int aRHS);
    void mod2this       (int aRHS);
    void lmod2this      (int aRHS);
    void map2this       (IntUnaryOperator aOpt);
    
    IIntVector abs();
    void abs2this();
    IIntVector negative();
    void negative2this();
    
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
    void shuffle(IRandom aRng);
}
