package jtool.math.table;

import jtool.code.CS.SliceType;
import jtool.code.collection.AbstractCollections;
import jtool.code.collection.ISlice;
import jtool.code.collection.IntegerList;
import jtool.code.collection.NewCollections;
import jtool.code.functional.IFilter;
import jtool.code.functional.IIndexFilter;

import java.util.Arrays;
import java.util.List;

public abstract class AbstractTableSlicer implements ITableSlicer {
    private ITable get_(ISlice        aSelectedRows, ISlice aSelectedCols) {return getLL(aSelectedRows, aSelectedCols);}
    private ITable get_(int[]         aSelectedRows, ISlice aSelectedCols) {return getLL(ISlice.of(aSelectedRows), aSelectedCols);}
    private ITable get_(List<Integer> aSelectedRows, ISlice aSelectedCols) {return getLL(ISlice.of(aSelectedRows), aSelectedCols);}
    private ITable get_(SliceType     aSelectedRows, ISlice aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); return getAL(aSelectedCols);}
    private ITable get_(int           aSelectedRow , ISlice aSelectedCols) {return getLL(ISlice.of(aSelectedRow), aSelectedCols);}
    
    @Override public final ITable get(ISlice        aSelectedRows, String[]  aSelectedCols) {return get_(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(int[]         aSelectedRows, String[]  aSelectedCols) {return get_(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(List<Integer> aSelectedRows, String[]  aSelectedCols) {return get_(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(SliceType     aSelectedRows, String[]  aSelectedCols) {return get_(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(int           aSelectedRow , String[]  aSelectedCols) {return get_(aSelectedRow , S2L(aSelectedCols));}
    @Override public final ITable get(ISlice        aSelectedRows, String    aSelectedCol ) {return get_(aSelectedRows, S2L(aSelectedCol ));}
    @Override public final ITable get(int[]         aSelectedRows, String    aSelectedCol ) {return get_(aSelectedRows, S2L(aSelectedCol ));}
    @Override public final ITable get(List<Integer> aSelectedRows, String    aSelectedCol ) {return get_(aSelectedRows, S2L(aSelectedCol ));}
    @Override public final ITable get(SliceType     aSelectedRows, String    aSelectedCol ) {return get_(aSelectedRows, S2L(aSelectedCol ));}
    @Override public final ITable get(int           aSelectedRow , String    aSelectedCol ) {return get_(aSelectedRow , S2L(aSelectedCol ));}
    @Override public final ITable get(ISlice        aSelectedRows, SliceType aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(aSelectedRows);}
    @Override public final ITable get(int[]         aSelectedRows, SliceType aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(ISlice.of(aSelectedRows));}
    @Override public final ITable get(List<Integer> aSelectedRows, SliceType aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(ISlice.of(aSelectedRows));}
    @Override public final ITable get(SliceType     aSelectedRows, SliceType aSelectedCols) {if (aSelectedRows != SliceType.ALL) throw new IllegalArgumentException(ROL_MSG); if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getAA();}
    @Override public final ITable get(int           aSelectedRow , SliceType aSelectedCols) {if (aSelectedCols != SliceType.ALL) throw new IllegalArgumentException(COL_MSG); return getLA(ISlice.of(aSelectedRow));}
    /** Groovy stuff */
    @Override public final ITable get(ISlice        aSelectedRows, Iterable<? extends CharSequence> aSelectedCols) {return get_(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(int[]         aSelectedRows, Iterable<? extends CharSequence> aSelectedCols) {return get_(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(List<Integer> aSelectedRows, Iterable<? extends CharSequence> aSelectedCols) {return get_(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(SliceType     aSelectedRows, Iterable<? extends CharSequence> aSelectedCols) {return get_(aSelectedRows, S2L(aSelectedCols));}
    @Override public final ITable get(int           aSelectedRow , Iterable<? extends CharSequence> aSelectedCols) {return get_(aSelectedRow , S2L(aSelectedCols));}
    @Override public final ITable get(IIndexFilter  aSelectedRows, Iterable<? extends CharSequence> aSelectedCols) {return get(NewCollections.filterInteger(thisRowNum_(), aSelectedRows), aSelectedCols);}
    
    final static String COL_MSG = "SelectedCols Must be a Filter or int[] or List<Integer> or String[] or ALL";
    final static String ROL_MSG = "SelectedRows Must be a Filter or int[] or List<Integer> or ALL";
    
    /** 支持过滤器输入，代替没有 {@code List<Boolean>} 的缺陷 */
    @Override public final ITable get(IIndexFilter  aSelectedRows, String[]        aSelectedCols) {return get(NewCollections.filterInteger(thisRowNum_(), aSelectedRows), aSelectedCols);}
    @Override public final ITable get(IIndexFilter  aSelectedRows, String          aSelectedCol ) {return get(NewCollections.filterInteger(thisRowNum_(), aSelectedRows), aSelectedCol );}
    @Override public final ITable get(IIndexFilter  aSelectedRows, SliceType       aSelectedCols) {return get(NewCollections.filterInteger(thisRowNum_(), aSelectedRows), aSelectedCols);}
    @Override public final ITable get(ISlice        aSelectedRows, IFilter<String> aSelectedCols) {return get_(aSelectedRows, S2L(AbstractCollections.filter(thisHeads_(), aSelectedCols)));}
    @Override public final ITable get(int[]         aSelectedRows, IFilter<String> aSelectedCols) {return get_(aSelectedRows, S2L(AbstractCollections.filter(thisHeads_(), aSelectedCols)));}
    @Override public final ITable get(List<Integer> aSelectedRows, IFilter<String> aSelectedCols) {return get_(aSelectedRows, S2L(AbstractCollections.filter(thisHeads_(), aSelectedCols)));}
    @Override public final ITable get(SliceType     aSelectedRows, IFilter<String> aSelectedCols) {return get_(aSelectedRows, S2L(AbstractCollections.filter(thisHeads_(), aSelectedCols)));}
    @Override public final ITable get(int           aSelectedRow , IFilter<String> aSelectedCols) {return get_(aSelectedRow , S2L(AbstractCollections.filter(thisHeads_(), aSelectedCols)));}
    @Override public final ITable get(IIndexFilter  aSelectedRows, IFilter<String> aSelectedCols) {return get_(NewCollections.filterInteger(thisRowNum_(), aSelectedRows), S2L(AbstractCollections.filter(thisHeads_(), aSelectedCols)));}
    
    private ISlice S2L(String[] aSelectedCols) {return S2L(Arrays.asList(aSelectedCols));}
    private ISlice S2L(Iterable<? extends CharSequence> aSelectedCols) {
        IntegerList rSelectedCols = new IntegerList();
        for (CharSequence tHead : aSelectedCols) {
            int tCol = head2col_(tHead.toString());
            if (tCol >= 0) rSelectedCols.add(tCol);
        }
        return rSelectedCols;
    }
    private ISlice S2L(String aSelectedCol) {
        int tCol = head2col_(aSelectedCol);
        return tCol >= 0 ? ISlice.of(tCol) : ISlice.zl();
    }
    
    /** stuff to override */
    protected abstract ITable getLL(ISlice aSelectedRows, ISlice aSelectedCols);
    protected abstract ITable getLA(ISlice aSelectedRows);
    protected abstract ITable getAL(ISlice aSelectedCols);
    protected abstract ITable getAA();
    
    protected abstract int thisRowNum_();
    protected abstract int head2col_(String aHead);
    protected abstract Iterable<String> thisHeads_();
}
