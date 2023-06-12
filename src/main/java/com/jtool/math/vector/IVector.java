package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import com.jtool.code.IHasLotIterator;
import com.jtool.code.ISetIterator;
import com.jtool.code.operator.IOperator1;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 简单起见默认都是实向量，返回类型 double，而如果涉及复向量则会提供额外的接口获取复数部分 </p>
 */
public interface IVector extends IVectorGetter, IVectorSetter, IHasLotIterator<IVectorGetter, Double> {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    Iterator<Double> iterator();
    ISetIterator<Double> setIterator();
    Iterator<Double> iteratorOf(IVectorGetter aContainer);
    
    default Iterable<Double> iterable() {return IVector.this::iterator;}
    default List<Double> asList() {
        return new AbstractList<Double>() {
            @Override public Double get(int index) {return IVector.this.get(index);}
            @Override public Double set(int index, Double element) {return getAndSet(index, element);}
            @Override public int size() {return IVector.this.size();}
        };
    }
    
    
    /** 转为兼容性更好的 double[] */
    double[] data();
    
    /** 批量修改的接口 */
    void fill(double aValue);
    void fill(IVectorGetter aVectorGetter);
    void fill(double[] aData);
    void fill(Iterable<? extends Number> aList);
    
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
        else return get(size()-1);
    }
    default double first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty Vector");
        else return get(0);
    }
    
    /** 附加一些额外的单元素操作，放在这里而不是 operation 因为这些方法理论和 set，get 之类的处于同样地位 */
    void increment_(int aIdx);
    double getAndIncrement_(int aIdx);
    double incrementAndGet_(int aIdx);
    void decrement_(int aIdx);
    double getAndDecrement_(int aIdx);
    double decrementAndGet_(int aIdx);
    void add_(int aIdx, double aDelta);
    double getAndAdd_(int aIdx, double aDelta);
    double addAndGet_(int aIdx, double aDelta);
    void update_(int aIdx, IOperator1<Double> aOpt);
    double getAndUpdate_(int aIdx, IOperator1<Double> aOpt);
    double updateAndGet_(int aIdx, IOperator1<Double> aOpt);
    
    void increment(int aIdx);
    double getAndIncrement(int aIdx);
    double incrementAndGet(int aIdx);
    void decrement(int aIdx);
    double getAndDecrement(int aIdx);
    double decrementAndGet(int aIdx);
    void add(int aIdx, double aDelta);
    double getAndAdd(int aIdx, double aDelta);
    double addAndGet(int aIdx, double aDelta);
    void update(int aIdx, IOperator1<Double> aOpt);
    double getAndUpdate(int aIdx, IOperator1<Double> aOpt);
    double updateAndGet(int aIdx, IOperator1<Double> aOpt);
    
    
    /** 现在不再提供生成器，只提供直接创建相同类型的全零的向量的接口，特殊矩阵的创建请使用 {@link Vectors} */
    default IVector newZeros() {return newZeros(size());}
    IVector newZeros(int aSize);
    
    IVector copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    IVectorSlicer slicer();
    IVectorSlicer refSlicer();
    
    /** 向量的运算操作，默认返回新的向量 */
    IVectorOperation operation();
    @VisibleForTesting default IVectorOperation opt() {return operation();}
    
    
    /** Groovy 的部分，增加向量基本的运算操作，现在也归入内部使用 */
    IVector plus     (double aRHS);
    IVector minus    (double aRHS);
    IVector multiply (double aRHS);
    IVector div      (double aRHS);
    IVector mod      (double aRHS);
    
    IVector plus     (IVectorGetter aRHS);
    IVector minus    (IVectorGetter aRHS);
    IVector multiply (IVectorGetter aRHS);
    IVector div      (IVectorGetter aRHS);
    IVector mod      (IVectorGetter aRHS);
    
    /** 注意这些 2this 操作并没有重载 groovy 中的 += 之类的运算符 */
    void plus2this      (double aRHS);
    void minus2this     (double aRHS);
    void multiply2this  (double aRHS);
    void div2this       (double aRHS);
    void mod2this       (double aRHS);
    
    void plus2this      (IVectorGetter aRHS);
    void minus2this     (IVectorGetter aRHS);
    void multiply2this  (IVectorGetter aRHS);
    void div2this       (IVectorGetter aRHS);
    void mod2this       (IVectorGetter aRHS);
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting double call(int aIdx);
    @VisibleForTesting double getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, double aValue);
    
    @VisibleForTesting IVector call(List<Integer> aIndices);
    @VisibleForTesting IVector call(SliceType     aIndices);
    
    @VisibleForTesting IVector getAt(List<Integer> aIndices);
    @VisibleForTesting IVector getAt(SliceType     aIndices);
    @VisibleForTesting void putAt(List<Integer> aIndices, double aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, IVectorGetter aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, double aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, IVectorGetter aVector);
}
