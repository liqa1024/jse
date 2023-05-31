package com.jtool.math.table;

import com.jtool.code.CS.SliceType;
import com.jtool.math.vector.IVectorGetter;

import java.util.List;

/**
 * 通用的列表切片器，接受 List 方便抽象的切片；
 * 和矩阵的切片不同，这里无论如何都会返回列表类型
 * @author liqa
 */
public interface ITableSlicer {
    /**
     * 为了代码简洁（因为 List 的内容被擦除不能重载），因此只支持 Integer 来切片，
     * 并且实际切片过程也会将 Boolean 转成 Integer。
     * 还是使用多种输入排列组合方式来重载，可能会让实现比较复杂，但是是值得的
     */
    ITable get(int[]         aSelectedRows, int[]         aSelectedCols);
    ITable get(List<Integer> aSelectedRows, int[]         aSelectedCols);
    ITable get(int[]         aSelectedRows, List<Integer> aSelectedCols);
    ITable get(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    ITable get(SliceType     aSelectedRows, int[]         aSelectedCols);
    ITable get(SliceType     aSelectedRows, List<Integer> aSelectedCols);
    ITable get(int[]         aSelectedRows, SliceType     aSelectedCols);
    ITable get(List<Integer> aSelectedRows, SliceType     aSelectedCols);
    ITable get(SliceType     aSelectedRows, SliceType     aSelectedCols);
    ITable get(int           aSelectedRow , int[]         aSelectedCols);
    ITable get(int           aSelectedRow , List<Integer> aSelectedCols);
    ITable get(int           aSelectedRow , SliceType     aSelectedCols);
    ITable get(int[]         aSelectedRows, int           aSelectedCol );
    ITable get(List<Integer> aSelectedRows, int           aSelectedCol );
    ITable get(SliceType     aSelectedRows, int           aSelectedCol );
    ITable get(int           aSelectedRow , int           aSelectedCol );
    /** Table 还支持列向使用 String 来切片 */
    ITable get(int[]         aSelectedRows, String[]      aSelectedCols);
    ITable get(List<Integer> aSelectedRows, String[]      aSelectedCols);
    ITable get(SliceType     aSelectedRows, String[]      aSelectedCols);
    ITable get(int           aSelectedRow , String[]      aSelectedCols);
    ITable get(int[]         aSelectedRows, String        aSelectedCol );
    ITable get(List<Integer> aSelectedRows, String        aSelectedCol );
    ITable get(SliceType     aSelectedRows, String        aSelectedCol );
    ITable get(int           aSelectedRow , String        aSelectedCol );
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>，List<String>} 的缺陷 */
    @FunctionalInterface interface IRowFilter          {boolean accept(IVectorGetter aRow);}
    @FunctionalInterface interface IRowFilterWithIndex {boolean accept(IVectorGetter aRow, int aIndex);}
    @FunctionalInterface interface IColFilter          {boolean accept(IVectorGetter aCol);}
    @FunctionalInterface interface IColFilterWithHead  {boolean accept(IVectorGetter aCol, String aHead);}
    
    ITable get(IRowFilter          aSelectedRows, int[]         aSelectedCols);
    ITable get(IRowFilter          aSelectedRows, List<Integer> aSelectedCols);
    ITable get(IRowFilter          aSelectedRows, SliceType     aSelectedCols);
    ITable get(IRowFilter          aSelectedRows, int           aSelectedCol );
    ITable get(IRowFilterWithIndex aSelectedRows, int[]         aSelectedCols);
    ITable get(IRowFilterWithIndex aSelectedRows, List<Integer> aSelectedCols);
    ITable get(IRowFilterWithIndex aSelectedRows, SliceType     aSelectedCols);
    ITable get(IRowFilterWithIndex aSelectedRows, int           aSelectedCol );
    
    ITable get(int[]         aSelectedRows, IColFilter          aSelectedCols);
    ITable get(List<Integer> aSelectedRows, IColFilter          aSelectedCols);
    ITable get(SliceType     aSelectedRows, IColFilter          aSelectedCols);
    ITable get(int           aSelectedRow , IColFilter          aSelectedCols);
    ITable get(int[]         aSelectedRows, IColFilterWithHead  aSelectedCols);
    ITable get(List<Integer> aSelectedRows, IColFilterWithHead  aSelectedCols);
    ITable get(SliceType     aSelectedRows, IColFilterWithHead  aSelectedCols);
    ITable get(int           aSelectedRow , IColFilterWithHead  aSelectedCols);
    
    ITable get(IRowFilter          aSelectedRows, IColFilter          aSelectedCols);
    ITable get(IRowFilterWithIndex aSelectedRows, IColFilter          aSelectedCols);
    ITable get(IRowFilter          aSelectedRows, IColFilterWithHead  aSelectedCols);
    ITable get(IRowFilterWithIndex aSelectedRows, IColFilterWithHead  aSelectedCols);
}
