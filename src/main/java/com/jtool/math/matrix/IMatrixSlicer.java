package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.math.vector.IVectorGetter;
import com.jtool.math.vector.IVector;

import java.util.List;

/**
 * 通用的矩阵切片器，接受 List 方便抽象的切片
 * @author liqa
 */
public interface IMatrixSlicer {
    /**
     * 为了代码简洁（因为 List 的内容被擦除不能重载），因此只支持 Integer 来切片，
     * 并且实际切片过程也会将 Boolean 转成 Integer。
     * 还是使用多种输入排列组合方式来重载，可能会让实现比较复杂，但是是值得的
     */
    IMatrix get(int[]         aSelectedRows, int[]         aSelectedCols);
    IMatrix get(List<Integer> aSelectedRows, int[]         aSelectedCols);
    IMatrix get(int[]         aSelectedRows, List<Integer> aSelectedCols);
    IMatrix get(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    IMatrix get(SliceType     aSelectedRows, int[]         aSelectedCols);
    IMatrix get(SliceType     aSelectedRows, List<Integer> aSelectedCols);
    IMatrix get(int[]         aSelectedRows, SliceType     aSelectedCols);
    IMatrix get(List<Integer> aSelectedRows, SliceType     aSelectedCols);
    IMatrix get(SliceType     aSelectedRows, SliceType     aSelectedCols);
    IVector get(int           aSelectedRow , int[]         aSelectedCols);
    IVector get(int           aSelectedRow , List<Integer> aSelectedCols);
    IVector get(int           aSelectedRow , SliceType     aSelectedCols);
    IVector get(int[]         aSelectedRows, int           aSelectedCol );
    IVector get(List<Integer> aSelectedRows, int           aSelectedCol );
    IVector get(SliceType     aSelectedRows, int           aSelectedCol );
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    IMatrix get(IIndexFilter  aSelectedRows, int[]         aSelectedCols);
    IMatrix get(IIndexFilter  aSelectedRows, List<Integer> aSelectedCols);
    IMatrix get(IIndexFilter  aSelectedRows, SliceType     aSelectedCols);
    IVector get(IIndexFilter  aSelectedRows, int           aSelectedCol );
    IMatrix get(int[]         aSelectedRows, IIndexFilter  aSelectedCols);
    IMatrix get(List<Integer> aSelectedRows, IIndexFilter  aSelectedCols);
    IMatrix get(SliceType     aSelectedRows, IIndexFilter  aSelectedCols);
    IVector get(int           aSelectedRow , IIndexFilter  aSelectedCols);
    IMatrix get(IIndexFilter  aSelectedRows, IIndexFilter  aSelectedCols);
    
    /** 现在获取对角放在切片操作中 */
    IVector diag();
    IVector diag(int aShift);
}
