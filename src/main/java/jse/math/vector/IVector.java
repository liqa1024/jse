package jse.math.vector;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import jep.NDArray;
import jse.code.UT;
import jse.math.SliceType;
import jse.code.collection.ISlice;
import jse.code.functional.IIndexFilter;
import jse.code.functional.ISwapper;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetIterator;
import jse.code.iterator.IHasDoubleIterator;
import jse.code.iterator.IHasDoubleSetIterator;
import jse.math.matrix.IMatrix;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

/**
 * 实数向量，返回类型 {@code double}
 * @author liqa
 */
public interface IVector extends ISwapper, IHasDoubleIterator, IHasDoubleSetIterator, IVectorGetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    @Override IDoubleIterator iterator();
    @Override IDoubleSetIterator setIterator();
    
    @Override default Iterable<Double> iterable() {return () -> iterator().toIterator();}
    List<Double> asList();
    IIntVector asIntVec();
    
    /**
     * 转换为 numpy 的数组 {@link NDArray}，在 java 侧根据具体向量类型可能不会进行值拷贝，由于
     * {@link NDArray} 内部实现特性，在 python 中总是会再经历一次值拷贝，此时使用不会有引用问题。
     * @return numpy 的数组 {@link NDArray}
     */
    NDArray<double[]> numpy();
    /** 转为兼容性更好的 {@code double[]}，会进行一次值拷贝 */
    double[] data();
    
    /** ISwapper stuffs */
    @Override void swap(int aIdx1, int aIdx2);
    
    /** 批量修改的接口 */
    void fill(double aValue);
    void fill(IVector aVector);
    void fill(IVectorGetter aVectorGetter);
    void fill(double[] aData);
    void fill(Iterable<? extends Number> aList);
    @Override void assign(DoubleSupplier aSup);
    @Override void forEach(DoubleConsumer aCon);
    /** Groovy stuff */
    default void fill(@ClosureParams(value=SimpleType.class, options="int") final Closure<? extends Number> aGroovyTask) {fill(i -> UT.Code.doubleValue(aGroovyTask.call(i)));}
    default void assign(final Closure<? extends Number> aGroovyTask) {assign(() -> UT.Code.doubleValue(aGroovyTask.call()));}
    
    /** 访问和修改部分，自带的接口 */
    int size();
    @Override double get(int aIdx);
    double getAndSet(int aIdx, double aValue); // 返回修改前的值
    void set(int aIdx, double aValue);
    
    /** 用于方便访问 */
    default boolean isEmpty() {return size()==0;}
    default double last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty Vector");
        return get(size()-1);
    }
    default double first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty Vector");
        return get(0);
    }
    
    /** 附加一些额外的单元素操作，对于 Vector 由于适用范围更广，提供更多的接口 */
    void increment(int aIdx);
    double getAndIncrement(int aIdx);
    void decrement(int aIdx);
    double getAndDecrement(int aIdx);
    void add(int aIdx, double aDelta);
    double getAndAdd(int aIdx, double aDelta);
    void update(int aIdx, DoubleUnaryOperator aOpt);
    double getAndUpdate(int aIdx, DoubleUnaryOperator aOpt);
    
    IVector copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    IVectorSlicer slicer();
    IVectorSlicer refSlicer();
    IVector subVec(int aFromIdx, int aToIdx);
    
    /** 向量的运算操作，默认返回新的向量 */
    IVectorOperation operation();
    @VisibleForTesting default IVectorOperation op() {return operation();}
    
    /** {@link Double} stuffs，不做特殊优化 */
    ILogicalVector isNaN();
    ILogicalVector isInfinite();
    ILogicalVector isFinite();
    
    /** Groovy 的部分，增加向量基本的运算操作，现在也归入内部使用 */
    IVector plus     (double aRHS);
    IVector minus    (double aRHS);
    IVector multiply (double aRHS);
    IVector div      (double aRHS);
    IVector mod      (double aRHS);
    
    IVector plus     (IVector aRHS);
    IVector minus    (IVector aRHS);
    IVector multiply (IVector aRHS);
    IVector div      (IVector aRHS);
    IVector mod      (IVector aRHS);
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void mod2this       (double aRHS);
    
    void plus2this      (IVector aRHS);
    void minus2this     (IVector aRHS);
    void multiply2this  (IVector aRHS);
    void div2this       (IVector aRHS);
    void mod2this       (IVector aRHS);
    
    IVector abs();
    void abs2this();
    IVector negative();
    void negative2this();
    
    /** 对于 Vector 将这些统计接口也直接放在这里方便使用 */
    double sum  ();
    double mean ();
    double prod ();
    double max  ();
    double min  ();
    void sort();
    
    /** 比较运算，注意特地避开 equals */
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
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting double call(int aIdx);
    @VisibleForTesting double getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, double aValue);
    
    @VisibleForTesting IVector call(ISlice        aIndices);
    @VisibleForTesting IVector call(List<Integer> aIndices);
    @VisibleForTesting IVector call(SliceType     aIndices);
    @VisibleForTesting IVector call(IIndexFilter  aIndices);
    
    @VisibleForTesting IVector getAt(ISlice        aIndices);
    @VisibleForTesting IVector getAt(List<Integer> aIndices);
    @VisibleForTesting IVector getAt(SliceType     aIndices);
    @VisibleForTesting IVector getAt(IIndexFilter  aIndices);
    @VisibleForTesting void putAt(ISlice        aIndices, double aValue);
    @VisibleForTesting void putAt(ISlice        aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(ISlice        aIndices, IVector aVector);
    @VisibleForTesting void putAt(List<Integer> aIndices, double aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, IVector aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, double aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, IVector aVector);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, double aValue);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(IIndexFilter  aIndices, IVector aVector);
}
