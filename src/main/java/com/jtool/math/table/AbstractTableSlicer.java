package com.jtool.math.table;

import com.google.common.collect.ImmutableList;
import com.jtool.code.CS.SliceType;
import com.jtool.code.UT;
import com.jtool.math.vector.IVectorGetter;

import java.util.ArrayList;
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
    @Override public final ITable get(IRowFilter          aSelectedRows, int[]         aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IRowFilter          aSelectedRows, List<Integer> aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IRowFilter          aSelectedRows, SliceType     aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IRowFilter          aSelectedRows, int           aSelectedCol ) {return get(F2L(aSelectedRows), aSelectedCol );}
    @Override public final ITable get(IRowFilterWithIndex aSelectedRows, int[]         aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IRowFilterWithIndex aSelectedRows, List<Integer> aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IRowFilterWithIndex aSelectedRows, SliceType     aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IRowFilterWithIndex aSelectedRows, int           aSelectedCol ) {return get(F2L(aSelectedRows), aSelectedCol );}
    
    @Override public final ITable get(int[]         aSelectedRows, IColFilter          aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final ITable get(List<Integer> aSelectedRows, IColFilter          aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final ITable get(SliceType     aSelectedRows, IColFilter          aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final ITable get(int           aSelectedRow , IColFilter          aSelectedCols) {return get(aSelectedRow , F2L(aSelectedCols));}
    @Override public final ITable get(int[]         aSelectedRows, IColFilterWithHead  aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final ITable get(List<Integer> aSelectedRows, IColFilterWithHead  aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final ITable get(SliceType     aSelectedRows, IColFilterWithHead  aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final ITable get(int           aSelectedRow , IColFilterWithHead  aSelectedCols) {return get(aSelectedRow , F2L(aSelectedCols));}
    
    @Override public final ITable get(IRowFilter          aSelectedRows, IColFilter          aSelectedCols) {return get(F2L(aSelectedRows), F2L(aSelectedCols));}
    @Override public final ITable get(IRowFilterWithIndex aSelectedRows, IColFilter          aSelectedCols) {return get(F2L(aSelectedRows), F2L(aSelectedCols));}
    @Override public final ITable get(IRowFilter          aSelectedRows, IColFilterWithHead  aSelectedCols) {return get(F2L(aSelectedRows), F2L(aSelectedCols));}
    @Override public final ITable get(IRowFilterWithIndex aSelectedRows, IColFilterWithHead  aSelectedCols) {return get(F2L(aSelectedRows), F2L(aSelectedCols));}
    
    List<Integer> S2L(String[] aSelectedCols) {
        List<Integer> rSelectedCols = new ArrayList<>();
        for (String tHead : aSelectedCols) {
            int tCol = head2col_(tHead);
            if (tCol >= 0) rSelectedCols.add(tCol);
        }
        return rSelectedCols;
    }
    List<Integer> S2L(String aSelectedCol) {
        int tCol = head2col_(aSelectedCol);
        return tCol >= 0 ? Collections.singletonList(tCol) : ImmutableList.of();
    }
    
    List<Integer> F2L(IRowFilter aSelectedRows) {
        List<Integer> rSelectedRows = new ArrayList<>();
        List<? extends IVectorGetter> tRows = thisRows_();
        int row = 0;
        for (IVectorGetter tRow : tRows) {
            if (aSelectedRows.accept(tRow)) rSelectedRows.add(row);
            ++row;
        }
        return rSelectedRows;
    }
    List<Integer> F2L(IColFilter aSelectedCols) {
        List<Integer> rSelectedCols = new ArrayList<>();
        List<? extends IVectorGetter> tCols = thisCols_();
        int col = 0;
        for (IVectorGetter tCol : tCols) {
            if (aSelectedCols.accept(tCol)) rSelectedCols.add(col);
            ++col;
        }
        return rSelectedCols;
    }
    List<Integer> F2L(IRowFilterWithIndex aSelectedRows) {
        List<Integer> rSelectedRows = new ArrayList<>();
        List<? extends IVectorGetter> tRows = thisRows_();
        int row = 0;
        for (IVectorGetter tRow : tRows) {
            if (aSelectedRows.accept(tRow, row)) rSelectedRows.add(row);
            ++row;
        }
        return rSelectedRows;
    }
    List<Integer> F2L(IColFilterWithHead aSelectedCols) {
        List<Integer> rSelectedCols = new ArrayList<>();
        List<? extends IVectorGetter> tCols = thisCols_();
        int col = 0;
        for (IVectorGetter tCol : tCols) {
            if (aSelectedCols.accept(tCol, col2head_(col))) rSelectedCols.add(col);
            ++col;
        }
        return rSelectedCols;
    }
    
    /** stuff to override */
    protected abstract ITable getLL(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    protected abstract ITable getLA(List<Integer> aSelectedRows);
    protected abstract ITable getAL(List<Integer> aSelectedCols);
    protected abstract ITable getAA();
    
    protected abstract List<? extends IVectorGetter> thisRows_();
    protected abstract List<? extends IVectorGetter> thisCols_();
    protected abstract int head2col_(String aHead);
    protected abstract String col2head_(int aCol);
}
