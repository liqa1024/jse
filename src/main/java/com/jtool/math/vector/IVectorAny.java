package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import com.jtool.code.IHasLotIterator;
import com.jtool.code.ISetIterator;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 可自定义获取的向量类型的向量类 </p>
 * <p> 简单起见默认都是实向量，返回类型 double，而如果涉及复向量则会提供额外的接口获取复数部分 </p>
 */
public interface IVectorAny<V extends IVectorGetter> extends IVectorGetter, IHasLotIterator<IVectorGetter, Double> {
    /** Iterable stuffs，虽然不继承 Iterable 但是会提供相关的直接获取的接口方便直接使用 */
    Iterator<Double> iterator();
    ISetIterator<Double> setIterator();
    Iterator<Double> iteratorOf(IVectorGetter aContainer);
    
    default Iterable<Double> iterable() {return IVectorAny.this::iterator;}
    default List<Double> asList() {
        return new AbstractList<Double>() {
            @Override public Double get(int index) {return IVectorAny.this.get(index);}
            @Override public Double set(int index, Double element) {return getAndSet(index, element);}
            @Override public int size() {return IVectorAny.this.size();}
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
    
    /** 获得基于自身的向量生成器，生成按列排布的向量 */
    IVectorGenerator<V> generator();
    @VisibleForTesting default IVectorGenerator<V> gen() {return generator();}
    
    V copy();
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    IVectorSlicer<V> slicer();
    IVectorSlicer<IVector> refSlicer();
    
    /** 向量的运算操作，默认返回新的向量 */
    IVectorOperation<V> operation();
    @VisibleForTesting default IVectorOperation<V> opt() {return operation();}
    
    
    /** Groovy 的部分，增加向量基本的运算操作，由于不能重载 += 之类的变成向自身操作，因此会充斥着值拷贝，因此不推荐重性能的场景使用 */
    @VisibleForTesting V plus       (double aRHS);
    @VisibleForTesting V minus      (double aRHS);
    @VisibleForTesting V multiply   (double aRHS);
    @VisibleForTesting V div        (double aRHS);
    @VisibleForTesting V mod        (double aRHS);
    
    @VisibleForTesting V plus       (IVectorGetter aRHS);
    @VisibleForTesting V minus      (IVectorGetter aRHS);
    @VisibleForTesting V multiply   (IVectorGetter aRHS);
    @VisibleForTesting V div        (IVectorGetter aRHS);
    @VisibleForTesting V mod        (IVectorGetter aRHS);
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting double call(int aIdx);
    @VisibleForTesting double getAt(int aIdx);
    @VisibleForTesting void putAt(int aIdx, double aValue);
    
    @VisibleForTesting V call(List<Integer> aIndices);
    @VisibleForTesting V call(SliceType     aIndices);
    
    @VisibleForTesting V getAt(List<Integer> aIndices);
    @VisibleForTesting V getAt(SliceType     aIndices);
    @VisibleForTesting void putAt(List<Integer> aIndices, double aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(List<Integer> aIndices, IVectorGetter aVector);
    @VisibleForTesting void putAt(SliceType     aIndices, double aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aList);
    @VisibleForTesting void putAt(SliceType     aIndices, IVectorGetter aVector);
}
