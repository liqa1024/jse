package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * @author liqa
 * <p> 在原本的向量接口上扩展更多高级功能 </p>
 */
public interface IVectorFull<T extends Number, V extends IVector<T>> extends IVector<T> {
    /** 获得基于自身的向量生成器，生成按列排布的向量 */
    IVectorGenerator<V> generator();
    @VisibleForTesting default IVectorGenerator<V> gen() {return generator();}
    
    /** 切片操作，默认返回新的向量，refSlicer 则会返回引用的切片结果 */
    IVectorSlicer<V> slicer();
    IVectorSlicer<IVector<T>> refSlicer();
    
    /** 向量的运算操作，默认返回新的向量 */
    IVectorOperation<V, T> operation();
    @VisibleForTesting default IVectorOperation<V, T> opt() {return operation();}
    
    
    /** Groovy 的部分，增加向量切片操作 */
    @VisibleForTesting V call(List<Integer> aIndices);
    @VisibleForTesting V call(SliceType     aIndices);
    
    @VisibleForTesting V getAt(List<Integer> aIndices);
    @VisibleForTesting V getAt(SliceType     aIndices);
    @VisibleForTesting void putAt(List<Integer> aIndices, Number aValue);
    @VisibleForTesting void putAt(List<Integer> aIndices, Iterable<? extends Number> aVec);
    @VisibleForTesting void putAt(SliceType     aIndices, Number aValue);
    @VisibleForTesting void putAt(SliceType     aIndices, Iterable<? extends Number> aVec);
}
