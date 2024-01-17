package jtool.math.vector;

import jtool.code.functional.IIntegerConsumer1;
import jtool.code.functional.IIntegerOperator1;
import jtool.code.functional.IIntegerSupplier;

import java.util.Comparator;

/**
 * 任意的整数向量的运算
 * @author liqa
 */
public interface IIntegerVectorOperation {
    /** 这两个方法名默认是作用到自身的，这里为了保持 operation 的使用简洁不在函数名上特殊说明 */
    void fill               (int aRHS);
    void fill               (IIntegerVector aRHS);
    void fill               (IIntegerVectorGetter aRHS);
    void assign             (IIntegerSupplier aSup);
    /** 统一提供一个 for-each 运算来减少优化需要的重复代码 */
    void forEach            (IIntegerConsumer1 aCon);
    
    /** IntegerVector 特有的操作 */
    void sort();
    void sort(Comparator<? super Integer> aComp);
    void shuffle();
    void shuffle(IIntegerOperator1 aRng);
}
