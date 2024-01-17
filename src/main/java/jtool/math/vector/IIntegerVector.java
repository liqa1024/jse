package jtool.math.vector;

import jtool.code.collection.ISlice;
import jtool.code.functional.IIntegerConsumer1;
import jtool.code.functional.IIntegerOperator1;
import jtool.code.functional.IIntegerSupplier;
import jtool.code.iterator.IHasIntegerIterator;
import jtool.code.iterator.IHasIntegerSetIterator;
import jtool.code.iterator.IIntegerIterator;
import jtool.code.iterator.IIntegerSetIterator;
import jtool.parallel.LocalRandom;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * @author liqa
 * <p> 专用的整数向量 </p>
 * <p> 由于完全实现工作量较大，这里暂只实现用到的接口 </p>
 * <p> 当然为了后续完善的方便，结构依旧保持一致 </p>
 */
public interface IIntegerVector extends ISlice, IHasIntegerIterator, IHasIntegerSetIterator, IIntegerVectorGetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    IIntegerIterator iterator();
    IIntegerSetIterator setIterator();
    
    default Iterable<Integer> iterable() {return () -> iterator().toIterator();}
    List<Integer> asList();
    IVector asVec();
    
    /** 批量修改的接口 */
    void fill(int aValue);
    void fill(IIntegerVector aVector);
    void fill(IIntegerVectorGetter aVectorGetter);
    void fill(int[] aData);
    void fill(Iterable<Integer> aList);
    void assign(IIntegerSupplier aSup);
    void forEach(IIntegerConsumer1 aCon);
    
    /** 访问和修改部分，自带的接口 */
    int get_(int aIdx);
    void set_(int aIdx, int aValue);
    int getAndSet_(int aIdx, int aValue); // 返回修改前的值
    int size();
    
    int get(int aIdx);
    int getAndSet(int aIdx, int aValue);
    void set(int aIdx, int aValue);
    
    /** 附加一些额外的单元素操作，对于 IntegerVector 由于适用范围更广，提供更多的接口 */
    void increment_(int aIdx);
    int getAndIncrement_(int aIdx);
    void decrement_(int aIdx);
    int getAndDecrement_(int aIdx);
    void add_(int aIdx, int aDelta);
    int getAndAdd_(int aIdx, int aDelta);
    void update_(int aIdx, IIntegerOperator1 aOpt);
    int getAndUpdate_(int aIdx, IIntegerOperator1 aOpt);
    
    void increment(int aIdx);
    int getAndIncrement(int aIdx);
    void decrement(int aIdx);
    int getAndDecrement(int aIdx);
    void add(int aIdx, int aDelta);
    int getAndAdd(int aIdx, int aDelta);
    void update(int aIdx, IIntegerOperator1 aOpt);
    int getAndUpdate(int aIdx, IIntegerOperator1 aOpt);
    
    IIntegerVector subVec(int aFromIdx, int aToIdx);
    
    /** 向量的运算操作，默认返回新的向量 */
    IIntegerVectorOperation operation();
    @VisibleForTesting default IIntegerVectorOperation opt() {return operation();}
    
    /** 增加向量基本的运算操作以及 IntegerVector 特有的操作，现在也归入内部使用 */
    void sort();
    void sort(Comparator<? super Integer> aComp);
    void shuffle();
    void shuffle(Random aRng);
    void shuffle(IIntegerOperator1 aRng);
}
