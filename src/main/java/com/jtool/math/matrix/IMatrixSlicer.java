package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.math.vector.IVectorGetter;

import java.util.List;

/**
 * 通用的矩阵切片器，接受 List 方便抽象的切片
 * @author liqa
 * @param <M> 切片获取到的矩阵类型，注意可以不是自身的类型
 * @param <V> 切片成一维的返回向量类型
 */
public interface IMatrixSlicer<M extends IMatrixGetter, V extends IVectorGetter> {
    /**
     * 为了代码简洁（因为 List 的内容被擦除不能重载），因此只支持 Integer 来切片，
     * 并且实际切片过程也会将 Boolean 转成 Integer。
     * 还是使用多种输入排列组合方式来重载，可能会让实现比较复杂，但是是值得的
     */
    M get(int[]         aSelectedRows, int[]         aSelectedCols);
    M get(List<Integer> aSelectedRows, int[]         aSelectedCols);
    M get(int[]         aSelectedRows, List<Integer> aSelectedCols);
    M get(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    M get(SliceType     aSelectedRows, int[]         aSelectedCols);
    M get(SliceType     aSelectedRows, List<Integer> aSelectedCols);
    M get(int[]         aSelectedRows, SliceType     aSelectedCols);
    M get(List<Integer> aSelectedRows, SliceType     aSelectedCols);
    M get(SliceType     aSelectedRows, SliceType     aSelectedCols);
    V get(int           aSelectedRow , int[]         aSelectedCols);
    V get(int           aSelectedRow , List<Integer> aSelectedCols);
    V get(int           aSelectedRow , SliceType     aSelectedCols);
    V get(int[]         aSelectedRows, int           aSelectedCol );
    V get(List<Integer> aSelectedRows, int           aSelectedCol );
    V get(SliceType     aSelectedRows, int           aSelectedCol );
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @FunctionalInterface interface IRowFilter          {boolean accept(IVectorGetter aRow);}
    @FunctionalInterface interface IRowFilterWithIndex {boolean accept(IVectorGetter aRow, int aIndex);}
    @FunctionalInterface interface IColFilter          {boolean accept(IVectorGetter aCol);}
    @FunctionalInterface interface IColFilterWithIndex {boolean accept(IVectorGetter aCol, int aIndex);}
    
    M get(IRowFilter          aSelectedRows, int[]         aSelectedCols);
    M get(IRowFilter          aSelectedRows, List<Integer> aSelectedCols);
    M get(IRowFilter          aSelectedRows, SliceType     aSelectedCols);
    V get(IRowFilter          aSelectedRows, int           aSelectedCol );
    M get(IRowFilterWithIndex aSelectedRows, int[]         aSelectedCols);
    M get(IRowFilterWithIndex aSelectedRows, List<Integer> aSelectedCols);
    M get(IRowFilterWithIndex aSelectedRows, SliceType     aSelectedCols);
    V get(IRowFilterWithIndex aSelectedRows, int           aSelectedCol );
    
    M get(int[]         aSelectedRows, IColFilter          aSelectedCols);
    M get(List<Integer> aSelectedRows, IColFilter          aSelectedCols);
    M get(SliceType     aSelectedRows, IColFilter          aSelectedCols);
    V get(int           aSelectedRow , IColFilter          aSelectedCols);
    M get(int[]         aSelectedRows, IColFilterWithIndex aSelectedCols);
    M get(List<Integer> aSelectedRows, IColFilterWithIndex aSelectedCols);
    M get(SliceType     aSelectedRows, IColFilterWithIndex aSelectedCols);
    V get(int           aSelectedRow , IColFilterWithIndex aSelectedCols);
    
    M get(IRowFilter          aSelectedRows, IColFilter          aSelectedCols);
    M get(IRowFilterWithIndex aSelectedRows, IColFilter          aSelectedCols);
    M get(IRowFilter          aSelectedRows, IColFilterWithIndex aSelectedCols);
    M get(IRowFilterWithIndex aSelectedRows, IColFilterWithIndex aSelectedCols);
}
