package com.guan.math;


import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * 方便直接使用 csv 读取结果的数据格式
 * @author liqa
 */
public class Table extends AbstractMap<String, List<Double>> {
    private final String[] mHand;
    private final List<double[]> mData;
    private final Map<String, Integer> mHand2Idx;
    private final boolean mNoHand;
    
    public Table(String[] aHand, List<double[]> aData) {
        mNoHand = false;
        mHand = aHand; mData = aData;
        mHand2Idx = new HashMap<>();
        for (int i = 0; i < mHand.length; ++i) mHand2Idx.put(mHand[i], i);
    }
    public Table(List<double[]> aData) {
        mNoHand = true;
        mData = aData;
        int tColNum = aData.get(0).length;
        mHand = new String[tColNum];
        mHand2Idx = new HashMap<>();
        for (int i = 0; i < tColNum; ++i) {
            mHand[i] = "C"+i; // 虽然 idea 默认读取 csv 是从 1 开始，这里为了和 getCol，getRow 保持一致，还是从 0 开始
            mHand2Idx.put(mHand[i], i);
        }
    }
    
    public List<Double> col(int aCol) {
        return new AbstractList<Double>() {
            @Override public Double get(int index) {return mData.get(index)[aCol];}
            @Override public int size() {return mData.size();}
        };
    }
    public List<Double> row(int aRow) {
        return new AbstractList<Double>() {
            private final double[] mRow = mData.get(aRow);
            @Override public Double get(int index) {return mRow[index];}
            @Override public int size() {return mRow.length;}
        };
    }
    public List<List<Double>> cols() {
        return new AbstractList<List<Double>>() {
            @Override public List<Double> get(int index) {return col(index);}
            @Override public int size() {return mHand.length;}
        };
    }
    public List<List<Double>> rows() {
        return new AbstractList<List<Double>>() {
            @Override public List<Double> get(int index) {return row(index);}
            @Override public int size() {return mData.size();}
        };
    }
    public boolean noHand() {return mNoHand;}
    
    
    /** map stuffs */
    @NotNull @Override public Set<Entry<String, List<Double>>> entrySet() {
        return new AbstractSet<Entry<String, List<Double>>>() {
            @Override
            public Iterator<Entry<String, List<Double>>> iterator() {
                return new Iterator<Entry<String, List<Double>>>() {
                    private int mIdx = 0;
                    @Override public boolean hasNext() {return mIdx < mHand.length;}
                    @Override public Entry<String, List<Double>> next() {
                        final int tIdx = mIdx; ++mIdx;
                        return new Entry<String, List<Double>>() {
                            @Override public String getKey() {return mHand[tIdx];}
                            @Override public List<Double> getValue() {return col(tIdx);}
                            @Override public List<Double> setValue(List<Double> value) {throw new UnsupportedOperationException("setValue");}
                        };
                    }
                };
            }
            @Override public int size() {return mHand.length;}
        };
    }
    @Override public final List<Double> put(String key, List<Double> value) {throw new UnsupportedOperationException("put");}
    @Override public final List<Double> get(Object key) {return col(mHand2Idx.get(key));}
    @Override public final boolean containsKey(Object key) {return mHand2Idx.containsKey(key);}
    @Override public final List<Double> remove(Object key) {throw new UnsupportedOperationException("remove");}
    @Override public final int size() {return mHand.length;}
    @Override public final void clear() {throw new UnsupportedOperationException("clear");}
}
