package com.jtool.math.table;


import com.jtool.math.matrix.IMatrix;
import com.jtool.math.vector.IVector;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.jtool.code.CS.ALL;

/**
 * 抽象的表格，实现一些通用的功能
 * @author liqa
 */
public abstract class AbstractTable implements ITable {
    private final String[] mHeads;
    private final Map<String, Integer> mHead2Idx;
    private final boolean mNoHead;
    
    public AbstractTable(String[] aHeads) {
        mNoHead = false;
        mHeads = aHeads;
        mHead2Idx = new HashMap<>();
        for (int i = 0; i < mHeads.length; ++i) mHead2Idx.put(mHeads[i], i);
    }
    public AbstractTable(int aColNum) {
        mNoHead = true;
        mHeads = new String[aColNum];
        mHead2Idx = new HashMap<>();
        for (int i = 0; i < aColNum; ++i) {
            mHeads[i] = "C"+i; // 虽然 idea 默认读取 csv 是从 1 开始，这里为了和 getCol，getRow 保持一致，还是从 0 开始
            mHead2Idx.put(mHeads[i], i);
        }
    }
    
    private String[] copyHeads_() {return Arrays.copyOf(mHeads, mHeads.length);}
    private String[] copyHeads_(List<Integer> aIndices) {
        String[] tOut = new String[aIndices.size()];
        for (int i = 0; i < tOut.length; ++i) tOut[i] = mHeads[aIndices.get(i)];
        return tOut;
    }
    
    /** ITable stuffs */
    @Override public boolean noHead() {return mNoHead;}
    @Override public List<String> heads() {return Arrays.asList(mHeads);}
    @Override public String getHead(int aCol) {return mHeads[aCol];}
    @Override public int getColumn(String aHead) {return mHead2Idx.getOrDefault(aHead, -1);}
    
    @Override public IVector get(String aHead) {return col(mHead2Idx.get(aHead));}
    @Override public double get(int aRow, String aHead) {return matrix().get(aRow, mHead2Idx.get(aHead));}
    @Override public boolean containsHead(String aHead) {return mHead2Idx.containsKey(aHead);}
    @Override public boolean setHead(String aOldHead, String aNewHead) {
        if (mHead2Idx.containsKey(aOldHead) && !mHead2Idx.containsKey(aNewHead)) {
            int tIdx = mHead2Idx.get(aOldHead);
            mHead2Idx.put(aNewHead, tIdx);
            mHeads[tIdx] = aNewHead;
            return true;
        } else {
            return false;
        }
    }
    @Override public double[][] data() {return matrix().data();}
    
    /** Matrix like stuffs */
    @Override public final List<IVector> rows() {return matrix().rows();}
    @Override public final IVector row(int aRow) {return matrix().row(aRow);}
    @Override public final List<IVector> cols() {return matrix().cols();}
    @Override public final IVector col(int aCol) {return matrix().col(aCol);}
    @Override public final int rowNumber() {return matrix().rowNumber();}
    @Override public final int columnNumber() {return mHeads.length;}
    
    @Override public ITable copy() {return Tables.from(matrix(), copyHeads_());}
    
    @Override public ITableSlicer slicer() {
        return new AbstractTableSlicer() {
            @Override protected ITable getLL(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {
                final IMatrix tMatrix = matrix().refSlicer().get(aSelectedRows, aSelectedCols);
                return Tables.from(tMatrix, copyHeads_(aSelectedCols));
            }
            @Override protected ITable getLA(final List<Integer> aSelectedRows) {
                final IMatrix tMatrix = matrix().refSlicer().get(aSelectedRows, ALL);
                return Tables.from(tMatrix, copyHeads_());
            }
            @Override protected ITable getAL(final List<Integer> aSelectedCols) {
                final IMatrix tMatrix = matrix().refSlicer().get(ALL, aSelectedCols);
                return Tables.from(tMatrix, copyHeads_(aSelectedCols));
            }
            @Override protected ITable getAA() {return Tables.from(matrix(), copyHeads_());}
            
            @Override protected int thisRowNum_() {return rowNumber();}
            @Override protected int head2col_(String aHead) {return getColumn(aHead);}
            @Override protected Iterable<String> thisHeads_() {return heads();}
        };
    }
    @Override public ITableSlicer refSlicer() {
        return new AbstractTableSlicer() {
            @Override protected ITable getLL(final List<Integer> aSelectedRows, final List<Integer> aSelectedCols) {
                final IMatrix tMatrix = matrix().refSlicer().get(aSelectedRows, aSelectedCols);
                return new AbstractTable(copyHeads_(aSelectedCols)) {@Override public IMatrix matrix() {return tMatrix;}};
            }
            @Override protected ITable getLA(List<Integer> aSelectedRows) {
                final IMatrix tMatrix = matrix().refSlicer().get(aSelectedRows, ALL);
                return new AbstractTable(copyHeads_()) {@Override public IMatrix matrix() {return tMatrix;}};
            }
            @Override protected ITable getAL(List<Integer> aSelectedCols) {
                final IMatrix tMatrix = matrix().refSlicer().get(ALL, aSelectedCols);
                return new AbstractTable(copyHeads_(aSelectedCols)) {@Override public IMatrix matrix() {return tMatrix;}};
            }
            
            @Override protected ITable getAA() {return AbstractTable.this;}
            
            @Override protected int thisRowNum_() {return rowNumber();}
            @Override protected int head2col_(String aHead) {return getColumn(aHead);}
            @Override protected Iterable<String> thisHeads_() {return heads();}
        };
    }
    
    /** stuff to override */
    public abstract IMatrix matrix();
}
