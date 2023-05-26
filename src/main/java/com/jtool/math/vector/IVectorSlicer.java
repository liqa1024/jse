package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;

import java.util.List;

/**
 * 通用的向量切片器，接受 List 方便抽象的切片
 * @author liqa
 * @param <V> 被切片的向量类型
 */
public interface IVectorSlicer<V extends IVectorGetter<? extends Number>> {
    /**
     * 为了代码简洁（因为 List 的内容被擦除不能重载），因此只支持 Integer 来切片，
     * 并且实际切片过程也会将 Boolean 转成 Integer。
     * 还是使用多种输入排列组合方式来重载，可能会让实现比较复杂，但是是值得的
     */
    V get(int[]         aIndices);
    V get(List<Integer> aIndices);
    V get(SliceType     aIndices);
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @FunctionalInterface interface IFilter {boolean accept(Number aValue);}
    @FunctionalInterface interface IFilterWithIndex {boolean accept(Number aValue, int aIndex);}
    V get(IFilter          aIndices);
    V get(IFilterWithIndex aIndices);
}
