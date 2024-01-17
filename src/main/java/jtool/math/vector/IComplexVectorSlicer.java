package jtool.math.vector;

import jtool.code.CS.SliceType;
import jtool.code.collection.ISlice;
import jtool.code.functional.IIndexFilter;

import java.util.List;

/**
 * 通用的向量切片器，接受 List 方便抽象的切片
 * @author liqa
 */
public interface IComplexVectorSlicer {
    /**
     * 为了代码简洁（因为 List 的内容被擦除不能重载），因此只支持 Integer 来切片，
     * 并且实际切片过程也会将 Boolean 转成 Integer。
     * 还是使用多种输入排列组合方式来重载，可能会让实现比较复杂，但是是值得的
     */
    IComplexVector get(ISlice        aIndices);
    IComplexVector get(int[]         aIndices);
    IComplexVector get(List<Integer> aIndices);
    IComplexVector get(SliceType     aIndices);
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    IComplexVector get(IIndexFilter aIndices);
}
