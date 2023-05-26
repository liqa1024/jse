package com.jtool.math.vector;

import com.jtool.code.CS.SliceType;
import com.jtool.code.UT;
import org.jetbrains.annotations.ApiStatus;

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
    default V get(int[]         aIndices) {return getL(UT.Code.asList(aIndices));}
    default V get(List<Integer> aIndices) {return getL(aIndices);}
    default V get(SliceType     aIndices) {if (aIndices != SliceType.ALL) throw new IllegalArgumentException(MSG); return getA();}
    
    // TODO 支持过滤器输入，代替没有 List<Boolean> 的缺陷
    
    String MSG = "Slice Indices Must be int[] or List<Integer> or ALL";
    
    /** stuff to override */
    @ApiStatus.Internal V getL(List<Integer> aIndices);
    @ApiStatus.Internal V getA();
}
