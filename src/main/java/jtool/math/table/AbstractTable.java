package jtool.math.table;


import jtool.code.collection.ISlice;
import jtool.code.collection.NewCollections;
import jtool.math.matrix.IMatrix;
import jtool.math.vector.IVector;
import jtool.math.vector.IVectorGetter;

import java.util.*;

import static jtool.code.CS.ALL;
import static jtool.code.CS.ZL_STR;

/**
 * 抽象的表格，实现一些通用的功能
 * @author liqa
 */
public abstract class AbstractTable implements ITable {
    protected final List<String> mHeads;
    protected final Map<String, Integer> mHead2Idx;
    
    AbstractTable(List<String> aHeads) {
        mHeads = aHeads;
        int tSize = aHeads.size();
        mHead2Idx = new HashMap<>(tSize);
        for (int i = 0; i < tSize; ++i) mHead2Idx.put(mHeads.get(i), i);
    }
    public AbstractTable(String[] aHeads) {
        this(NewCollections.from(aHeads));
    }
    public AbstractTable(int aColNum) {
        // 虽然 idea 默认读取 csv 是从 1 开始，这里为了和 getCol，getRow 保持一致，还是从 0 开始
        this(NewCollections.from(aColNum, i -> "C"+i));
    }
    
    private String[] copyHeads_() {return mHeads.toArray(ZL_STR);}
    private String[] copyHeads_(ISlice aIndices) {
        String[] tOut = new String[aIndices.size()];
        for (int i = 0; i < tOut.length; ++i) tOut[i] = mHeads.get(aIndices.get(i));
        return tOut;
    }
    
    /** ITable stuffs */
    @Override public final String getHead(int aCol) {return mHeads.get(aCol);}
    @Override public final int getColumn(String aHead) {return mHead2Idx.getOrDefault(aHead, -1);}
    
    @Override public final IVector get(String aHead) {return col(aHead);}
    @Override public final void put(String aHead, double aValue) {
        if (mHead2Idx.containsKey(aHead)) get(aHead).fill(aValue);
        else newColumn_(aHead).fill(aValue);
    }
    @Override public final void put(String aHead, IVector aVector) {
        if (mHead2Idx.containsKey(aHead)) get(aHead).fill(aVector);
        else newColumn_(aHead).fill(aVector);
    }
    @Override public final void put(String aHead, IVectorGetter aVectorGetter) {
        if (mHead2Idx.containsKey(aHead)) get(aHead).fill(aVectorGetter);
        else newColumn_(aHead).fill(aVectorGetter);
    }
    @Override public final void put(String aHead, double[] aData) {
        if (mHead2Idx.containsKey(aHead)) get(aHead).fill(aData);
        else newColumn_(aHead).fill(aData);
    }
    @Override public final void put(String aHead, Iterable<? extends Number> aList) {
        if (mHead2Idx.containsKey(aHead)) get(aHead).fill(aList);
        else newColumn_(aHead).fill(aList);
    }
    @Override public final boolean containsHead(String aHead) {return mHead2Idx.containsKey(aHead);}
    @Override public final boolean setHead(String aOldHead, String aNewHead) {
        if (mHead2Idx.containsKey(aOldHead) && !mHead2Idx.containsKey(aNewHead)) {
            int tIdx = mHead2Idx.get(aOldHead);
            mHead2Idx.remove(aOldHead);
            mHead2Idx.put(aNewHead, tIdx);
            mHeads.set(tIdx, aNewHead);
            return true;
        } else {
            return false;
        }
    }
    @Override public final double[][] data() {return asMatrix().data();}
    
    /** Matrix like stuffs */
    @Override public double get(int aRow, String aHead) {return asMatrix().get(aRow, mHead2Idx.get(aHead));}
    @Override public void set(int aRow, String aHead, double aValue) {asMatrix().set(aRow, mHead2Idx.get(aHead), aValue);}
    @Override public List<IVector> rows() {return asMatrix().rows();}
    @Override public IVector row(int aRow) {return asMatrix().row(aRow);}
    @Override public List<IVector> cols() {return asMatrix().cols();}
    @Override public IVector col(String aHead) {return asMatrix().col(mHead2Idx.get(aHead));}
    @Override public int rowNumber() {return asMatrix().rowNumber();}
    @Override public int columnNumber() {return mHeads.size();}
    
    @Override public final ITable copy() {return Tables.from(asMatrix(), copyHeads_());}
    
    @Override public final ITableSlicer slicer() {
        return new AbstractTableSlicer() {
            @Override protected ITable getLL(final ISlice aSelectedRows, final ISlice aSelectedCols) {return Tables.from(asMatrix().refSlicer().get(aSelectedRows, aSelectedCols), copyHeads_(aSelectedCols));}
            @Override protected ITable getLA(final ISlice aSelectedRows) {return Tables.from(asMatrix().refSlicer().get(aSelectedRows, ALL), copyHeads_());}
            @Override protected ITable getAL(final ISlice aSelectedCols) {return Tables.from(asMatrix().refSlicer().get(ALL, aSelectedCols), copyHeads_(aSelectedCols));}
            @Override protected ITable getAA() {return Tables.from(asMatrix(), copyHeads_());}
            
            @Override protected int thisRowNum_() {return rowNumber();}
            @Override protected int head2col_(String aHead) {return getColumn(aHead);}
            @Override protected Iterable<String> thisHeads_() {return heads();}
        };
    }
    @Override public final ITableSlicer refSlicer() {
        return new AbstractTableSlicer() {
            @Override protected ITable getLL(final ISlice aSelectedRows, final ISlice aSelectedCols) {
                final IMatrix tMatrix = asMatrix().refSlicer().get(aSelectedRows, aSelectedCols);
                return new AbstractTable(NewCollections.slice(mHeads, aSelectedCols)) {@Override public IMatrix asMatrix() {return tMatrix;}};
            }
            @Override protected ITable getLA(ISlice aSelectedRows) {
                final IMatrix tMatrix = asMatrix().refSlicer().get(aSelectedRows, ALL);
                return new AbstractTable(NewCollections.from(mHeads)) {@Override public IMatrix asMatrix() {return tMatrix;}};
            }
            @Override protected ITable getAL(ISlice aSelectedCols) {
                final IMatrix tMatrix = asMatrix().refSlicer().get(ALL, aSelectedCols);
                return new AbstractTable(NewCollections.slice(mHeads, aSelectedCols)) {@Override public IMatrix asMatrix() {return tMatrix;}};
            }
            @Override protected ITable getAA() {
                final IMatrix tMatrix = asMatrix();
                return new AbstractTable(NewCollections.from(mHeads)) {@Override public IMatrix asMatrix() {return tMatrix;}};
            }
            
            @Override protected int thisRowNum_() {return rowNumber();}
            @Override protected int head2col_(String aHead) {return getColumn(aHead);}
            @Override protected Iterable<String> thisHeads_() {return heads();}
        };
    }
    
    /** stuff to override */
    public abstract IMatrix asMatrix();
    protected IVector newColumn_(String aHead) {throw new UnsupportedOperationException("newColumn");}
}
