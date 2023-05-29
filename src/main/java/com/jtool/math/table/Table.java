package com.jtool.math.table;


import com.jtool.math.matrix.AbstractMatrix;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vector;

import java.util.*;

/**
 * 方便直接使用 csv 读取结果的数据格式
 * @author liqa
 */
public final class Table extends AbstractMatrix implements IMatrixTable {
    private final String[] mHeads;
    private final List<double[]> mData;
    private final Map<String, Integer> mHead2Idx;
    private final boolean mNoHead;
    
    public Table(String[] aHeads, List<double[]> aData) {
        mNoHead = false;
        mHeads = aHeads; mData = aData;
        mHead2Idx = new HashMap<>();
        for (int i = 0; i < mHeads.length; ++i) mHead2Idx.put(mHeads[i], i);
    }
    public Table(List<double[]> aData) {
        mNoHead = true;
        mData = aData;
        int tColNum = aData.get(0).length;
        mHeads = new String[tColNum];
        mHead2Idx = new HashMap<>();
        for (int i = 0; i < tColNum; ++i) {
            mHeads[i] = "C"+i; // 虽然 idea 默认读取 csv 是从 1 开始，这里为了和 getCol，getRow 保持一致，还是从 0 开始
            mHead2Idx.put(mHeads[i], i);
        }
    }
    
    /** ITable stuffs */
    @Override public boolean noHead() {return mNoHead;}
    @Override public List<String> heads() {return Arrays.asList(mHeads);}
    @Override public IVector get(String aHead) {return col(mHead2Idx.get(aHead));}
    @Override public boolean containsHead(String aHead) {return mHead2Idx.containsKey(aHead);}
    @Override public boolean setHead(String aOldHead, String aNewHead) {
        if (mHead2Idx.containsKey(aOldHead) && !mHead2Idx.containsKey(aNewHead)) {
            int tIdx = mHead2Idx.get(aOldHead);
            mHead2Idx.put(aNewHead, tIdx);
            return true;
        } else {
            return false;
        }
    }
    
    /** AbstractMatrix stuffs */
    @Override public double get_(int aRow, int aCol) {return mData.get(aRow)[aCol];}
    @Override public void set_(int aRow, int aCol, double aValue) {mData.get(aRow)[aCol] = aValue;}
    @Override public double getAndSet_(int aRow, int aCol, double aValue) {
        double[] tRow = mData.get(aRow);
        double oValue = tRow[aCol];
        tRow[aCol] = aValue;
        return oValue;
    }
    @Override public int rowNumber() {return mData.size();}
    @Override public int columnNumber() {return mHeads.length;}
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public IVector row(final int aRow) {
        if (aRow<0 || aRow>=rowNumber()) throw new IndexOutOfBoundsException("Row: "+aRow);
        return new Vector(mHeads.length, mData.get(aRow));
    }
}
