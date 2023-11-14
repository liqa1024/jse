package jtool.math.table;

import jtool.code.CS.SliceType;
import jtool.code.filter.IFilter;
import jtool.code.filter.IIndexFilter;

import java.util.List;

/**
 * 通用的列表切片器，接受 List 方便抽象的切片；
 * 和矩阵的切片不同，这里无论如何都会返回列表类型
 * @author liqa
 */
public interface ITableSlicer {
    /**
     * 为了代码简洁（因为 List 的内容被擦除不能重载），因此只支持 String 来切片，
     * 实际切片过程会将 Boolean 和 String 转成 Integer。
     * 还是使用多种输入排列组合方式来重载，可能会让实现比较复杂，但是是值得的
     */
    ITable get(int[]         aSelectedRows, String[]  aSelectedCols);
    ITable get(List<Integer> aSelectedRows, String[]  aSelectedCols);
    ITable get(SliceType     aSelectedRows, String[]  aSelectedCols);
    ITable get(int           aSelectedRow , String[]  aSelectedCols);
    ITable get(int[]         aSelectedRows, String    aSelectedCol );
    ITable get(List<Integer> aSelectedRows, String    aSelectedCol );
    ITable get(SliceType     aSelectedRows, String    aSelectedCol );
    ITable get(int           aSelectedRow , String    aSelectedCol );
    ITable get(int[]         aSelectedRows, SliceType aSelectedCols);
    ITable get(List<Integer> aSelectedRows, SliceType aSelectedCols);
    ITable get(SliceType     aSelectedRows, SliceType aSelectedCols);
    /** Groovy stuff */
    ITable get(int[]         aSelectedRows, Iterable<? extends CharSequence> aSelectedCols);
    ITable get(List<Integer> aSelectedRows, Iterable<? extends CharSequence> aSelectedCols);
    ITable get(SliceType     aSelectedRows, Iterable<? extends CharSequence> aSelectedCols);
    ITable get(int           aSelectedRow , Iterable<? extends CharSequence> aSelectedCols);
    ITable get(IIndexFilter  aSelectedRows, Iterable<? extends CharSequence> aSelectedCols);
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    ITable get(IIndexFilter  aSelectedRows, String[]        aSelectedCols);
    ITable get(IIndexFilter  aSelectedRows, String          aSelectedCol );
    ITable get(IIndexFilter  aSelectedRows, SliceType       aSelectedCols);
    ITable get(int[]         aSelectedRows, IFilter<String> aSelectedCols);
    ITable get(List<Integer> aSelectedRows, IFilter<String> aSelectedCols);
    ITable get(SliceType     aSelectedRows, IFilter<String> aSelectedCols);
    ITable get(int           aSelectedRow , IFilter<String> aSelectedCols);
    ITable get(IIndexFilter  aSelectedRows, IFilter<String> aSelectedCols);
}
