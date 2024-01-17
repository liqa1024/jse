package jtool.math.vector;

import jtool.code.CS.SliceType;
import jtool.code.collection.ISlice;
import jtool.code.functional.IIndexFilter;
import jtool.code.functional.IDoubleConsumer1;
import jtool.code.functional.IDoubleSupplier;
import jtool.code.iterator.*;
import jtool.code.functional.IDoubleOperator1;
import groovy.lang.Closure;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 简单起见默认都是实向量，返回类型 double </p>
 */
public interface IVector extends IHasDoubleIterator, IHasDoubleSetIterator, IVectorGetter {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    IDoubleIterator iterator();
    IDoubleSetIterator setIterator();
    
    default Iterable<Double> iterable() {return () -> iterator().toIterator();}
    List<Double> asList();
    
    /** 转为兼容性更好的 double[] */
    double[] data();
    
    /** 批量修改的接口 */
    void fill(double aValue);
    void fill(IVector aVector);
    void fill(IVectorGetter aVectorGetter);
    void fill(double[] aData);
    void fill(Iterable<? extends Number> aList);
    void assign(IDoubleSupplier aSup);
    void forEach(IDoubleConsumer1 aCon);
    /** Groovy stuff */
    default void fill(final Closure<? extends Number> aGroovyTask) {fill(i -> aGroovyTask.call(i).doubleValue());}
    default void assign(final Closure<? extends Number> aGroovyTask) {assign(() -> aGroovyTask.call().doubleValue());}
    
    /** 访问和修改部分，自带的接口 */
    double get_(int aIdx);
    void set_(int aIdx, double aValue);
    double getAndSet_(int aIdx, double aValue); // 返回修改前的值
    int size();
    
    double get(int aIdx);
    double getAndSet(int aIdx, double aValue);
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
    void increment_(int aIdx);
    double getAndIncrement_(int aIdx);
    void decrement_(int aIdx);
    double getAndDecrement_(int aIdx);
    void add_(int aIdx, double aDelta);
    double getAndAdd_(int aIdx, double aDelta);
    void update_(int aIdx, IDoubleOperator1 aOpt);
    double getAndUpdate_(int aIdx, IDoubleOperator1 aOpt);
    
    void increment(int aIdx);
    double getAndIncrement(int aIdx);
    void decrement(int aIdx);
    double getAndDecrement(int aIdx);
    void add(int aIdx, double aDelta);
    double getAndAdd(int aIdx, double aDelta);
    void update(int aIdx, IDoubleOperator1 aOpt);
    double getAndUpdate(int aIdx, IDoubleOperator1 aOpt);
    
    
    IVector copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    IVectorSlicer slicer();
    IVectorSlicer refSlicer();
    IVector subVec(int aFromIdx, int aToIdx);
    
    /** 向量的运算操作，默认返回新的向量 */
    IVectorOperation operation();
    @VisibleForTesting default IVectorOperation opt() {return operation();}
    
    
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
    
    IVector negative();
    void negative2this();
    
    /** 对于 Vector 将这些统计接口也直接放在这里方便使用 */
    double sum  ();
    double mean ();
    double prod ();
    double max  ();
    double min  ();
    
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
