package com.jtool.math.table;

import com.jtool.code.CS.SliceType;
import com.jtool.code.filter.IFilter;
import com.jtool.code.filter.IIndexFilter;
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
    ITable get(IIndexFilter  aSelectedRows, int[]           aSelectedCols);
    ITable get(IIndexFilter  aSelectedRows, List<Integer>   aSelectedCols);
    ITable get(IIndexFilter  aSelectedRows, SliceType       aSelectedCols);
    ITable get(IIndexFilter  aSelectedRows, int             aSelectedCol );
    ITable get(int[]         aSelectedRows, IFilter<String> aSelectedCols);
    ITable get(List<Integer> aSelectedRows, IFilter<String> aSelectedCols);
    ITable get(SliceType     aSelectedRows, IFilter<String> aSelectedCols);
    ITable get(int           aSelectedRow , IFilter<String> aSelectedCols);
    ITable get(IIndexFilter  aSelectedRows, IFilter<String> aSelectedCols);
}
