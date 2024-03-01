package jse.math.vector;

import jse.code.functional.IChecker;
import jse.code.functional.IComparator;
import jse.code.functional.ISwapper;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.*;

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
    IVector operate     (IVector aRHS, DoubleBinaryOperator aOpt);
    
    IVector plus        (double aRHS);
    IVector minus       (double aRHS);
    IVector lminus      (double aRHS);
    IVector multiply    (double aRHS);
    IVector div         (double aRHS);
    IVector ldiv        (double aRHS);
    IVector mod         (double aRHS);
    IVector lmod        (double aRHS);
    IVector map         (DoubleUnaryOperator aOpt);
    
    void plus2this      (IVector aRHS);
    void minus2this     (IVector aRHS);
    void lminus2this    (IVector aRHS);
    void multiply2this  (IVector aRHS);
    void div2this       (IVector aRHS);
    void ldiv2this      (IVector aRHS);
    void mod2this       (IVector aRHS);
    void lmod2this      (IVector aRHS);
    void operate2this   (IVector aRHS, DoubleBinaryOperator aOpt);
    
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void lminus2this    (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void ldiv2this      (double aRHS);
    void mod2this       (double aRHS);
    void lmod2this      (double aRHS);
    void map2this       (DoubleUnaryOperator aOpt);
    
    IVector negative();
    void negative2this();
    
    /** 补充的一些运算 */
    void plus2dest      (IVector aRHS, IVector rDest);
    void minus2dest     (IVector aRHS, IVector rDest);
    void lminus2dest    (IVector aRHS, IVector rDest);
    void multiply2dest  (IVector aRHS, IVector rDest);
    void div2dest       (IVector aRHS, IVector rDest);
    void ldiv2dest      (IVector aRHS, IVector rDest);
    void mod2dest       (IVector aRHS, IVector rDest);
    void lmod2dest      (IVector aRHS, IVector rDest);
    void operate2dest   (IVector aRHS, IVector rDest, DoubleBinaryOperator aOpt);
    
    void plus2dest      (double aRHS, IVector rDest);
    void minus2dest     (double aRHS, IVector rDest);
    void lminus2dest    (double aRHS, IVector rDest);
    void multiply2dest  (double aRHS, IVector rDest);
    void div2dest       (double aRHS, IVector rDest);
    void ldiv2dest      (double aRHS, IVector rDest);
    void mod2dest       (double aRHS, IVector rDest);
    void lmod2dest      (double aRHS, IVector rDest);
    void map2dest       (IVector rDest, DoubleUnaryOperator aOpt);
    
    
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill           (double aRHS);
    void fill           (IVector aRHS);
    void fill           (IVectorGetter aRHS);
    void assign         (DoubleSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach        (DoubleConsumer aCon);
    
    double sum          ();
    double mean         ();
    double prod         ();
    double max          ();
    double min          ();
    double stat         (DoubleBinaryOperator aOpt);
    
    IVector cumsum      ();
    IVector cummean     ();
    IVector cumprod     ();
    IVector cummax      ();
    IVector cummin      ();
    IVector cumstat     (DoubleBinaryOperator aOpt);
    
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
    void reverse2this();
    @VisibleForTesting default IVector refreverse() {return refReverse();}
    
    /** 各种排序操作 */
    void sort();
    /** 注意 aComp 传入的为 index */
    void sort(IntBinaryOperator aComp);
    /** 使用自身作为 key 来进行排序，会顺便将自身也排序 */
    void biSort(ISwapper aSwapper);
    void biSort(ISwapper aSwapper, IntBinaryOperator aComp);
    @VisibleForTesting default void bisort(ISwapper aSwapper) {biSort(aSwapper);}
    @VisibleForTesting default void bisort(ISwapper aSwapper, IntBinaryOperator aComp) {biSort(aSwapper, aComp);}
}
