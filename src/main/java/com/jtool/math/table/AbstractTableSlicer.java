package com.jtool.math.table;

import com.google.common.collect.ImmutableList;
import com.jtool.code.CS.SliceType;
import com.jtool.code.UT;
import com.jtool.code.filter.IFilter;
import com.jtool.code.filter.IIndexFilter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public abstract class AbstractTableSlicer implements ITableSlicer {
    @Override public final ITable get(int[]         aSelectedRows, int[]         aSelectedCols) {return getLL(UT.Code.asList(aSelectedRows), UT.Code.asList(aSelectedCols));}
    @Override public final ITable get(List<Integer> aSelectedRows, int[]         aSelectedCols) {return getLL(aSelectedRows, UT.Code.asList(aSelectedCols));}
    @Override public final ITable get(int[]         aSelectedRows, List<Integer> aSelectedCols) {return getLL(UT.Code.asList(aSelectedRows), aSelectedCols);}
    @Override public final ITable get(List<Integer> aSelectedRows, List<Integer> aSelectedCols) {return getLL(aSelectedRows, aSelectedCols);}
    @Override public final ITable get(SliceType     aSelectedRows, int[]         aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(UT.Code.asList(aSelectedCols));}
    @Override public final ITable get(SliceType     aSelectedRows, List<Integer> aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(aSelectedCols);}
    @Override public final ITable get(int[]         aSelectedRows, SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(UT.Code.asList(aSelectedRows));}
    @Override public final ITable get(List<Integer> aSelectedRows, SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(aSelectedRows);}
    @Override public final ITable get(SliceType     aSelectedRows, SliceType     aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getAA();}
    @Override public final ITable get(int           aSelectedRow , int[]         aSelectedCols) {return getLL(Collections.singletonList(aSelectedRow), UT.Code.asList(aSelectedCols));}
    @Override public final ITable get(int           aSelectedRow , List<Integer> aSelectedCols) {return getLL(Collections.singletonList(aSelectedRow), aSelectedCols);}
    @Override public final ITable get(int           aSelectedRow , SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(Collections.singletonList(aSelectedRow));}
    @Override public final ITable get(int[]         aSelectedRows, int           aSelectedCol ) {return getLL(UT.Code.asList(aSelectedRows), Collections.singletonList(aSelectedCol));}
    @Override public final ITable get(List<Integer> aSelectedRows, int           aSelectedCol ) {return getLL(aSelectedRows, Collections.singletonList(aSelectedCol));}
    @Override public final ITable get(SliceType     aSelectedRows, int           aSelectedCol ) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(Collections.singletonList(aSelectedCol));}
    @Override public final ITable get(int           aSelectedRow , int           aSelectedCol ) {return getLL(Collections.singletonList(aSelectedRow), Collections.singletonList(aSelectedCol));}
    
    @Override public final ITable get(int[]         aSelectedRows, String[]      aSelectedCols) {return get(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(List<Integer> aSelectedRows, String[]      aSelectedCols) {return get(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(SliceType     aSelectedRows, String[]      aSelectedCols) {return get(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(int           aSelectedRow , String[]      aSelectedCols) {return get(aSelectedRow , S2L(aSelectedCols));}
    @Override public final ITable get(int[]         aSelectedRows, String        aSelectedCol ) {return get(aSelectedRows, S2L(aSelectedCol ));}
    @Override public final ITable get(List<Integer> aSelectedRows, String        aSelectedCol ) {return get(aSelectedRows, S2L(aSelectedCol ));}
    @Override public final ITable get(SliceType     aSelectedRows, String        aSelectedCol ) {return get(aSelectedRows, S2L(aSelectedCol ));}
    @Override public final ITable get(int           aSelectedRow , String        aSelectedCol ) {return get(aSelectedRow , S2L(aSelectedCol ));}
    
    final static String COL_MSG = "SelectedCols Must be a Filter or int[] or List<Integer> or String[] or ALL";
    final static String ROL_MSG = "SelectedRows Must be a Filter or int[] or List<Integer> or ALL";
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final ITable get(IIndexFilter  aSelectedRows, int[]           aSelectedCols) {return get(IIndexFilter.fixedFilter(thisRowNum_(), aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IIndexFilter  aSelectedRows, List<Integer>   aSelectedCols) {return get(IIndexFilter.fixedFilter(thisRowNum_(), aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IIndexFilter  aSelectedRows, SliceType       aSelectedCols) {return get(IIndexFilter.fixedFilter(thisRowNum_(), aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IIndexFilter  aSelectedRows, int             aSelectedCol ) {return get(IIndexFilter.fixedFilter(thisRowNum_(), aSelectedRows), aSelectedCol);}
    @Override public final ITable get(int[]         aSelectedRows, IFilter<String> aSelectedCols) {return get(aSelectedRows, S2L(IFilter.filter(thisHeads_(), aSelectedCols)));}
    @Override public final ITable get(List<Integer> aSelectedRows, IFilter<String> aSelectedCols) {return get(aSelectedRows, S2L(IFilter.filter(thisHeads_(), aSelectedCols)));}
    @Override public final ITable get(SliceType     aSelectedRows, IFilter<String> aSelectedCols) {return get(aSelectedRows, S2L(IFilter.filter(thisHeads_(), aSelectedCols)));}
    @Override public final ITable get(int           aSelectedRow , IFilter<String> aSelectedCols) {return get(aSelectedRow , S2L(IFilter.filter(thisHeads_(), aSelectedCols)));}
    @Override public final ITable get(IIndexFilter  aSelectedRows, IFilter<String> aSelectedCols) {return get(IIndexFilter.fixedFilter(thisRowNum_(), aSelectedRows), S2L(IFilter.filter(thisHeads_(), aSelectedCols)));}
    
    private List<Integer> S2L(String[] aSelectedCols) {return S2L(Arrays.asList(aSelectedCols));}
    private List<Integer> S2L(Iterable<String> aSelectedCols) {
        List<Integer> rSelectedCols = new ArrayList<>();
        for (String tHead : aSelectedCols) {
            int tCol = head2col_(tHead);
            if (tCol >= 0) rSelectedCols.add(tCol);
        }
        return rSelectedCols;
    }
    private List<Integer> S2L(String aSelectedCol) {
        int tCol = head2col_(aSelectedCol);
        return tCol >= 0 ? Collections.singletonList(tCol) : ImmutableList.of();
    }
    
    /** stuff to override */
    protected abstract ITable getLL(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    protected abstract ITable getLA(List<Integer> aSelectedRows);
    protected abstract ITable getAL(List<Integer> aSelectedCols);
    protected abstract ITable getAA();
    
    protected abstract int thisRowNum_();
    protected abstract int head2col_(String aHead);
    protected abstract Iterable<String> thisHeads_();
}
