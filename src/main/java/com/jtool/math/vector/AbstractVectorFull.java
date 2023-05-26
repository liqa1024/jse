package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Iterator;
import java.util.List;

/**
 * @author liqa
 * <p> 通用的向量类，由于默认实现比较复杂，并且涉及到重写 Object 的成员，因此部分方法放入抽象类中 </p>
 */
public abstract class AbstractVectorFull<T extends Number, V extends IVector<T>> extends AbstractVector<T> implements IVectorFull<T, V> {
    
    /** 向量生成器部分 */
    protected class VectorGenerator extends AbstractVectorGenerator<T, V> {
        @Override protected Iterator<T> thisIterator_() {return iterator();}
        @Override protected int thisSize_() {return size();}
        @Override public V zeros(int aSize) {return newZeros(aSize);}
    }
    
    @Override public IVectorGenerator<V> generator() {return new VectorGenerator();}
    
    
    /** 切片操作，默认返回新的矩阵，refSlicer 则会返回引用的切片结果 */
    @Override public IVectorSlicer<V> slicer() {
        return new IVectorSlicer<V>() {
            @Override public V getL(final List<Integer> aIndices) {return generator().from(aIndices.size(), i -> AbstractVectorFull.this.get(aIndices.get(i)));}
            @Override public V getA() {return generator().same();}
        };
    }
    @Override public IVectorSlicer<IVector<T>> refSlicer() {
        return new IVectorSlicer<IVector<T>>() {
            @Override public IVector<T> getL(final List<Integer> aIndices) {
                return new AbstractVector<T>() {
                    /** 方便起见，依旧使用带有边界检查的方法，保证一般方法的边界检测永远生效 */
                    @Override public T get_(int aIdx) {return AbstractVectorFull.this.get(aIndices.get(aIdx));}
                    @Override public void set_(int aIdx, Number aValue) {AbstractVectorFull.this.setOnly(aIndices.get(aIdx), aValue);}
                    @Override public T getAndSet_(int aIdx, Number aValue) {return AbstractVectorFull.this.getAndSet(aIndices.get(aIdx), aValue);}
                    @Override public int size() {return aIndices.size();}
                };
            }
            @Override public IVector<T> getA() {return AbstractVectorFull.this;}
        };
    }
    
    
    /** Groovy 的部分，增加矩阵切片操作 */
    @VisibleForTesting @Override public V call(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public V call(SliceType     aIndices) {return slicer().get(aIndices);}
    
    @VisibleForTesting @Override public V getAt(List<Integer> aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public V getAt(SliceType     aIndices) {return slicer().get(aIndices);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, Number aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(List<Integer> aIndices, Iterable<? extends Number> aVec) {refSlicer().get(aIndices).fill(aVec);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, Number aValue) {refSlicer().get(aIndices).fill(aValue);}
    @VisibleForTesting @Override public void putAt(SliceType     aIndices, Iterable<? extends Number> aVec) {refSlicer().get(aIndices).fill(aVec);}
    
    
    /** stuff to override */
    public abstract IVectorOperation<V, T> operation();
    protected abstract V newZeros(int aSize);
}
