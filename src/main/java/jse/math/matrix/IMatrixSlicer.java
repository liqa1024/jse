package jse.math.matrix;

import jse.code.CS.SliceType;
import jse.code.collection.ISlice;
import jse.code.functional.IIndexFilter;
import jse.math.vector.IVector;

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
    IMatrix get(ISlice        aSelectedRows, ISlice        aSelectedCols);
    IMatrix get(ISlice        aSelectedRows, int[]         aSelectedCols);
    IMatrix get(ISlice        aSelectedRows, List<Integer> aSelectedCols);
    IMatrix get(ISlice        aSelectedRows, SliceType     aSelectedCols);
    IMatrix get(int[]         aSelectedRows, ISlice        aSelectedCols);
    IMatrix get(int[]         aSelectedRows, int[]         aSelectedCols);
    IMatrix get(int[]         aSelectedRows, List<Integer> aSelectedCols);
    IMatrix get(int[]         aSelectedRows, SliceType     aSelectedCols);
    IMatrix get(List<Integer> aSelectedRows, ISlice        aSelectedCols);
    IMatrix get(List<Integer> aSelectedRows, int[]         aSelectedCols);
    IMatrix get(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    IMatrix get(List<Integer> aSelectedRows, SliceType     aSelectedCols);
    IMatrix get(SliceType     aSelectedRows, ISlice        aSelectedCols);
    IMatrix get(SliceType     aSelectedRows, int[]         aSelectedCols);
    IMatrix get(SliceType     aSelectedRows, List<Integer> aSelectedCols);
    IMatrix get(SliceType     aSelectedRows, SliceType     aSelectedCols);
    IVector get(int           aSelectedRow , ISlice        aSelectedCols);
    IVector get(int           aSelectedRow , int[]         aSelectedCols);
    IVector get(int           aSelectedRow , List<Integer> aSelectedCols);
    IVector get(int           aSelectedRow , SliceType     aSelectedCols);
    IVector get(ISlice        aSelectedRows, int           aSelectedCol );
    IVector get(int[]         aSelectedRows, int           aSelectedCol );
    IVector get(List<Integer> aSelectedRows, int           aSelectedCol );
    IVector get(SliceType     aSelectedRows, int           aSelectedCol );
    
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    IMatrix get(IIndexFilter  aSelectedRows, ISlice        aSelectedCols);
    IMatrix get(IIndexFilter  aSelectedRows, int[]         aSelectedCols);
    IMatrix get(IIndexFilter  aSelectedRows, List<Integer> aSelectedCols);
    IMatrix get(IIndexFilter  aSelectedRows, SliceType     aSelectedCols);
    IMatrix get(ISlice        aSelectedRows, IIndexFilter  aSelectedCols);
    IMatrix get(int[]         aSelectedRows, IIndexFilter  aSelectedCols);
    IMatrix get(List<Integer> aSelectedRows, IIndexFilter  aSelectedCols);
    IMatrix get(SliceType     aSelectedRows, IIndexFilter  aSelectedCols);
    IMatrix get(IIndexFilter  aSelectedRows, IIndexFilter  aSelectedCols);
    IVector get(int           aSelectedRow , IIndexFilter  aSelectedCols);
    IVector get(IIndexFilter  aSelectedRows, int           aSelectedCol );
    
    /** 现在获取对角放在切片操作中 */
    IVector diag();
    IVector diag(int aShift);
}
