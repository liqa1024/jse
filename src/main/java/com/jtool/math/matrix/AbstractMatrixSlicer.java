package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.UT;
import com.jtool.math.vector.IVectorGetter;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMatrixSlicer<M extends IMatrixGetter, V extends IVectorGetter> implements IMatrixSlicer<M, V> {
    @Override public final M get(int[]         aSelectedRows, int[]         aSelectedCols) {return getLL(UT.Code.asList(aSelectedRows), UT.Code.asList(aSelectedCols));}
    @Override public final M get(List<Integer> aSelectedRows, int[]         aSelectedCols) {return getLL(aSelectedRows, UT.Code.asList(aSelectedCols));}
    @Override public final M get(int[]         aSelectedRows, List<Integer> aSelectedCols) {return getLL(UT.Code.asList(aSelectedRows), aSelectedCols);}
    @Override public final M get(List<Integer> aSelectedRows, List<Integer> aSelectedCols) {return getLL(aSelectedRows, aSelectedCols);}
    @Override public final M get(SliceType     aSelectedRows, int[]         aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(UT.Code.asList(aSelectedCols));}
    @Override public final M get(SliceType     aSelectedRows, List<Integer> aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(aSelectedCols);}
    @Override public final M get(int[]         aSelectedRows, SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(UT.Code.asList(aSelectedRows));}
    @Override public final M get(List<Integer> aSelectedRows, SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(aSelectedRows);}
    @Override public final M get(SliceType     aSelectedRows, SliceType     aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getAA();}
    @Override public final V get(int           aSelectedRow , int[]         aSelectedCols) {return getIL(aSelectedRow, UT.Code.asList(aSelectedCols));}
    @Override public final V get(int           aSelectedRow , List<Integer> aSelectedCols) {return getIL(aSelectedRow, aSelectedCols);}
    @Override public final V get(int           aSelectedRow , SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getIA(aSelectedRow);}
    @Override public final V get(int[]         aSelectedRows, int           aSelectedCol ) {return getLI(UT.Code.asList(aSelectedRows), aSelectedCol);}
    @Override public final V get(List<Integer> aSelectedRows, int           aSelectedCol ) {return getLI(aSelectedRows, aSelectedCol);}
    @Override public final V get(SliceType     aSelectedRows, int           aSelectedCol ) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAI(aSelectedCol);}
    
    final static String COL_MSG = "SelectedCols Must be a Filter or int[] or List<Integer> or ALL";
    final static String ROL_MSG = "SelectedRows Must be a Filter or int[] or List<Integer> or ALL";
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final M get(IRowFilter          aSelectedRows, int[]         aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final M get(IRowFilter          aSelectedRows, List<Integer> aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final M get(IRowFilter          aSelectedRows, SliceType     aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final V get(IRowFilter          aSelectedRows, int           aSelectedCol ) {return get(F2L(aSelectedRows), aSelectedCol );}
    @Override public final M get(IRowFilterWithIndex aSelectedRows, int[]         aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final M get(IRowFilterWithIndex aSelectedRows, List<Integer> aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final M get(IRowFilterWithIndex aSelectedRows, SliceType     aSelectedCols) {return get(F2L(aSelectedRows), aSelectedCols);}
    @Override public final V get(IRowFilterWithIndex aSelectedRows, int           aSelectedCol ) {return get(F2L(aSelectedRows), aSelectedCol );}
    
    @Override public final M get(int[]         aSelectedRows, IColFilter          aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final M get(List<Integer> aSelectedRows, IColFilter          aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final M get(SliceType     aSelectedRows, IColFilter          aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final V get(int           aSelectedRow , IColFilter          aSelectedCols) {return get(aSelectedRow , F2L(aSelectedCols));}
    @Override public final M get(int[]         aSelectedRows, IColFilterWithIndex aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final M get(List<Integer> aSelectedRows, IColFilterWithIndex aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final M get(SliceType     aSelectedRows, IColFilterWithIndex aSelectedCols) {return get(aSelectedRows, F2L(aSelectedCols));}
    @Override public final V get(int           aSelectedRow , IColFilterWithIndex aSelectedCols) {return get(aSelectedRow , F2L(aSelectedCols));}
    
    @Override public final M get(IRowFilter          aSelectedRows, IColFilter          aSelectedCols) {return get(F2L(aSelectedRows), F2L(aSelectedCols));}
    @Override public final M get(IRowFilterWithIndex aSelectedRows, IColFilter          aSelectedCols) {return get(F2L(aSelectedRows), F2L(aSelectedCols));}
    @Override public final M get(IRowFilter          aSelectedRows, IColFilterWithIndex aSelectedCols) {return get(F2L(aSelectedRows), F2L(aSelectedCols));}
    @Override public final M get(IRowFilterWithIndex aSelectedRows, IColFilterWithIndex aSelectedCols) {return get(F2L(aSelectedRows), F2L(aSelectedCols));}
    
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
    List<Integer> F2L(IColFilterWithIndex aSelectedCols) {
        List<Integer> rSelectedCols = new ArrayList<>();
        List<? extends IVectorGetter> tCols = thisCols_();
        int col = 0;
        for (IVectorGetter tCol : tCols) {
            if (aSelectedCols.accept(tCol, col)) rSelectedCols.add(col);
            ++col;
        }
        return rSelectedCols;
    }
    
    /** stuff to override */
    protected abstract V getIL(int aSelectedRow, List<Integer> aSelectedCols);
    protected abstract V getLI(List<Integer> aSelectedRows, int aSelectedCol);
    protected abstract V getIA(int aSelectedRow);
    protected abstract V getAI(int aSelectedCol);
    protected abstract M getLL(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    protected abstract M getLA(List<Integer> aSelectedRows);
    protected abstract M getAL(List<Integer> aSelectedCols);
    protected abstract M getAA();
    
    protected abstract List<? extends IVectorGetter> thisRows_();
    protected abstract List<? extends IVectorGetter> thisCols_();
}
