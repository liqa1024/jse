package jse.math.vector;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import jep.NDArray;
import jse.math.SliceType;
import jse.code.collection.ISlice;
import jse.code.functional.IIndexFilter;
import jse.code.functional.ISwapper;
import jse.code.iterator.IHasLongIterator;
import jse.code.iterator.IHasLongSetIterator;
import jse.code.iterator.ILongIterator;
import jse.code.iterator.ILongSetIterator;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.*;

/**
 * 长整数向量，返回类型 {@code long}
 * @author liqa
 */
public interface ILongVector extends ISwapper, IHasLongIterator, IHasLongSetIterator, ILongVectorGetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    @Override ILongIterator iterator();
    @Override ILongSetIterator setIterator();
    
    @Override default Iterable<Long> iterable() {return () -> iterator().toIterator();}
    List<Long> asList();
    IVector asVec();
    IIntVector asIntVec();
    
    /**
     * 转换为 numpy 的数组 {@link NDArray}，在 java 侧根据具体向量类型可能不会进行值拷贝，由于
     * {@link NDArray} 内部实现特性，在 python 中总是会再经历一次值拷贝，此时使用不会有引用问题。
     * @return numpy 的数组 {@link NDArray}
     */
    NDArray<long[]> numpy();
    /** 转为兼容性更好的 {@code long[]} */
    long[] data();
    
    /** ISwapper stuffs */
    @Override void swap(int aIdx1, int aIdx2);
    
    /** 批量修改的接口 */
    void fill(long aValue);
    void fill(ILongVector aVector);
    void fill(ILongVectorGetter aVectorGetter);
    void fill(long[] aData);
    void fill(Iterable<? extends Number> aList);
    @Override void assign(LongSupplier aSup);
    @Override void forEach(LongConsumer aCon);
    /** Groovy stuff */
    default void fill(@ClosureParams(value=SimpleType.class, options="int") final Closure<? extends Number> aGroovyTask) {fill(i -> aGroovyTask.call(i).longValue());}
    default void assign(final Closure<? extends Number> aGroovyTask) {assign(() -> aGroovyTask.call().longValue());}
    
    /** 访问和修改部分，自带的接口 */
    int size();
    @Override long get(int aIdx);
    long getAndSet(int aIdx, long aValue); // 返回修改前的值
    void set(int aIdx, long aValue);
    
    /** 用于方便访问 */
    default boolean isEmpty() {return size()==0;}
    default long last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty LongVector");
        return get(size()-1);
    }
    default long first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty LongVector");
        return get(0);
    }
    
    /** 附加一些额外的单元素操作，对于 IntegerVector 由于适用范围更广，提供更多的接口 */
    void increment(int aIdx);
    long getAndIncrement(int aIdx);
    void decrement(int aIdx);
    long getAndDecrement(int aIdx);
    void add(int aIdx, long aDelta);
    long getAndAdd(int aIdx, long aDelta);
    void update(int aIdx, LongUnaryOperator aOpt);
    long getAndUpdate(int aIdx, LongUnaryOperator aOpt);
    
    ILongVector copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    ILongVectorSlicer slicer();
    ILongVectorSlicer refSlicer();
    ILongVector subVec(int aFromIdx, int aToIdx);
    
    /** 向量的运算操作，默认返回新的向量 */
    ILongVectorOperation operation();
    @VisibleForTesting default ILongVectorOperation op() {return operation();}
    
    /** 增加向量基本的运算操作，现在也归入内部使用 */
    long   sum  ();
    double mean ();
    double prod ();
    long   max  ();
    long   min  ();
    void sort();
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting long call(int aIdx);
    @VisibleForTesting long getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, long aValue);
    
    @VisibleForTesting ILongVector call(ISlice        aIndices);
    @VisibleForTesting ILongVector call(List<Integer> aIndices);
    @VisibleForTesting ILongVector call(SliceType     aIndices);
    @VisibleForTesting ILongVector call(IIndexFilter  aIndices);
    
    @VisibleForTesting ILongVector getAt(ISlice        aIndices);
    @VisibleForTesting ILongVector getAt(List<Integer> aIndices);
    @VisibleForTesting ILongVector getAt(SliceType     aIndices);
    @VisibleForTesting ILongVector getAt(IIndexFilter  aIndices);
    @VisibleForTesting void putAt(ISlice        aIndices, long aValue);
    @VisibleForTesting void putAt(ISlice        aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(ISlice        aIndices, ILongVector aVector);
    @VisibleForTesting void putAt(List<Integer> aIndices, long aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, ILongVector aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, long aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, ILongVector aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, long aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, ILongVector aVector);
}
