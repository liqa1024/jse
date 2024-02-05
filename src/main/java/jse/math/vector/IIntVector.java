package jse.math.vector;

import groovy.lang.Closure;
import jse.code.CS.SliceType;
import jse.code.collection.ISlice;
import jse.code.functional.IIndexFilter;
import jse.code.functional.ISwapper;
import jse.code.iterator.IHasIntIterator;
import jse.code.iterator.IHasIntSetIterator;
import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

/**
 * @author liqa
 * <p> 专用的整数向量 </p>
 * <p> 由于完全实现工作量较大，这里暂只实现用到的接口 </p>
 * <p> 当然为了后续完善的方便，结构依旧保持一致 </p>
 */
public interface IIntVector extends ISwapper, ISlice, IHasIntIterator, IHasIntSetIterator, IIntVectorGetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    IIntIterator iterator();
    IIntSetIterator setIterator();
    
    default Iterable<Integer> iterable() {return () -> iterator().toIterator();}
    List<Integer> asList();
    IVector asVec();
    
    /** 转为兼容性更好的 int[] */
    int[] data();
    
    /** ISwapper stuffs */
    void swap(int aIdx1, int aIdx2);
    
    /** 批量修改的接口 */
    void fill(int aValue);
    void fill(IIntVector aVector);
    void fill(IIntVectorGetter aVectorGetter);
    void fill(int[] aData);
    void fill(Iterable<? extends Number> aList);
    void assign(IntSupplier aSup);
    void forEach(IntConsumer aCon);
    /** Groovy stuff */
    default void fill(final Closure<? extends Number> aGroovyTask) {fill(i -> aGroovyTask.call(i).intValue());}
    default void assign(final Closure<? extends Number> aGroovyTask) {assign(() -> aGroovyTask.call().intValue());}
    
    /** 访问和修改部分，自带的接口 */
    int size();
    int get(int aIdx);
    int getAndSet(int aIdx, int aValue); // 返回修改前的值
    void set(int aIdx, int aValue);
    
    /** 用于方便访问 */
    default boolean isEmpty() {return size()==0;}
    default int last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty IntVector");
        return get(size()-1);
    }
    default int first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty IntVector");
        return get(0);
    }
    
    /** 附加一些额外的单元素操作，对于 IntegerVector 由于适用范围更广，提供更多的接口 */
    void increment(int aIdx);
    int getAndIncrement(int aIdx);
    void decrement(int aIdx);
    int getAndDecrement(int aIdx);
    void add(int aIdx, int aDelta);
    int getAndAdd(int aIdx, int aDelta);
    void update(int aIdx, IntUnaryOperator aOpt);
    int getAndUpdate(int aIdx, IntUnaryOperator aOpt);
    
    IIntVector copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    IIntVectorSlicer slicer();
    IIntVectorSlicer refSlicer();
    IIntVector subVec(int aFromIdx, int aToIdx);
    
    /** 向量的运算操作，默认返回新的向量 */
    IIntVectorOperation operation();
    @VisibleForTesting default IIntVectorOperation opt() {return operation();}
    
    /** 增加向量基本的运算操作以及 IntegerVector 特有的操作，现在也归入内部使用 */
    int    sum  ();
    double mean ();
    double prod ();
    int    max  ();
    int    min  ();
    
    void sort();
    void shuffle();
    void shuffle(Random aRng);
    void shuffle(IntUnaryOperator aRng);
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting int call(int aIdx);
    @VisibleForTesting int getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, int aValue);
    
    @VisibleForTesting IIntVector call(ISlice        aIndices);
    @VisibleForTesting IIntVector call(List<Integer> aIndices);
    @VisibleForTesting IIntVector call(SliceType     aIndices);
    @VisibleForTesting IIntVector call(IIndexFilter  aIndices);
    
    @VisibleForTesting IIntVector getAt(ISlice        aIndices);
    @VisibleForTesting IIntVector getAt(List<Integer> aIndices);
    @VisibleForTesting IIntVector getAt(SliceType     aIndices);
    @VisibleForTesting IIntVector getAt(IIndexFilter  aIndices);
    @VisibleForTesting void putAt(ISlice        aIndices, int aValue);
    @VisibleForTesting void putAt(ISlice        aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(ISlice        aIndices, IIntVector aVector);
    @VisibleForTesting void putAt(List<Integer> aIndices, int aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, IIntVector aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, int aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, IIntVector aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, int aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IIntVector aVector);
}
