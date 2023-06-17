package com.jtool.math.matrix;

import com.jtool.code.CS.SliceType;
import com.jtool.code.UT;
import com.jtool.code.filter.IIndexFilter;
import com.jtool.math.vector.IVector;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMatrixSlicer implements IMatrixSlicer {
    @Override public final IMatrix get(int[]         aSelectedRows, int[]         aSelectedCols) {return getLL(UT.Code.asList(aSelectedRows), UT.Code.asList(aSelectedCols));}
    @Override public final IMatrix get(List<Integer> aSelectedRows, int[]         aSelectedCols) {return getLL(aSelectedRows, UT.Code.asList(aSelectedCols));}
    @Override public final IMatrix get(int[]         aSelectedRows, List<Integer> aSelectedCols) {return getLL(UT.Code.asList(aSelectedRows), aSelectedCols);}
    @Override public final IMatrix get(List<Integer> aSelectedRows, List<Integer> aSelectedCols) {return getLL(aSelectedRows, aSelectedCols);}
    @Override public final IMatrix get(SliceType     aSelectedRows, int[]         aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(UT.Code.asList(aSelectedCols));}
    @Override public final IMatrix get(SliceType     aSelectedRows, List<Integer> aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(aSelectedCols);}
    @Override public final IMatrix get(int[]         aSelectedRows, SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(UT.Code.asList(aSelectedRows));}
    @Override public final IMatrix get(List<Integer> aSelectedRows, SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(aSelectedRows);}
    @Override public final IMatrix get(SliceType     aSelectedRows, SliceType     aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getAA();}
    @Override public final IVector get(int           aSelectedRow , int[]         aSelectedCols) {return getIL(aSelectedRow, UT.Code.asList(aSelectedCols));}
    @Override public final IVector get(int           aSelectedRow , List<Integer> aSelectedCols) {return getIL(aSelectedRow, aSelectedCols);}
    @Override public final IVector get(int           aSelectedRow , SliceType     aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getIA(aSelectedRow);}
    @Override public final IVector get(int[]         aSelectedRows, int           aSelectedCol ) {return getLI(UT.Code.asList(aSelectedRows), aSelectedCol);}
    @Override public final IVector get(List<Integer> aSelectedRows, int           aSelectedCol ) {return getLI(aSelectedRows, aSelectedCol);}
    @Override public final IVector get(SliceType     aSelectedRows, int           aSelectedCol ) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAI(aSelectedCol);}
    
    final static String COL_MSG = "SelectedCols Must be a Filter or int[] or List<Integer> or ALL";
    final static String ROL_MSG = "SelectedRows Must be a Filter or int[] or List<Integer> or ALL";
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final IMatrix get(IIndexFilter  aSelectedRows, int[]         aSelectedCols) {return get(IIndexFilter.filter(thisRowNum_(), aSelectedRows), aSelectedCols);}
    @Override public final IMatrix get(IIndexFilter  aSelectedRows, List<Integer> aSelectedCols) {return get(IIndexFilter.filter(thisRowNum_(), aSelectedRows), aSelectedCols);}
    @Override public final IMatrix get(IIndexFilter  aSelectedRows, SliceType     aSelectedCols) {return get(IIndexFilter.filter(thisRowNum_(), aSelectedRows), aSelectedCols);}
    @Override public final IVector get(IIndexFilter  aSelectedRows, int           aSelectedCol ) {return get(IIndexFilter.filter(thisRowNum_(), aSelectedRows), aSelectedCol);}
    @Override public final IMatrix get(int[]         aSelectedRows, IIndexFilter  aSelectedCols) {return get(aSelectedRows, IIndexFilter.filter(thisColNum_(), aSelectedCols));}
    @Override public final IMatrix get(List<Integer> aSelectedRows, IIndexFilter  aSelectedCols) {return get(aSelectedRows, IIndexFilter.filter(thisColNum_(), aSelectedCols));}
    @Override public final IMatrix get(SliceType     aSelectedRows, IIndexFilter  aSelectedCols) {return get(aSelectedRows, IIndexFilter.filter(thisColNum_(), aSelectedCols));}
    @Override public final IVector get(int           aSelectedRow , IIndexFilter  aSelectedCols) {return get(aSelectedRow , IIndexFilter.filter(thisColNum_(), aSelectedCols));}
    @Override public final IMatrix get(IIndexFilter  aSelectedRows, IIndexFilter  aSelectedCols) {return get(IIndexFilter.filter(thisRowNum_(), aSelectedRows), IIndexFilter.filter(thisColNum_(), aSelectedCols));}
    
    @Override public final IVector diag() {return diag(0);}
    
    
    /** stuff to override */
    protected abstract IVector getIL(int aSelectedRow, List<Integer> aSelectedCols);
    protected abstract IVector getLI(List<Integer> aSelectedRows, int aSelectedCol);
    protected abstract IVector getIA(int aSelectedRow);
    protected abstract IVector getAI(int aSelectedCol);
    protected abstract IMatrix getLL(List<Integer> aSelectedRows, List<Integer> aSelectedCols);
    protected abstract IMatrix getLA(List<Integer> aSelectedRows);
    protected abstract IMatrix getAL(List<Integer> aSelectedCols);
    protected abstract IMatrix getAA();
    public abstract IVector diag(int aShift);
    
    protected abstract int thisRowNum_();
    protected abstract int thisColNum_();
}
