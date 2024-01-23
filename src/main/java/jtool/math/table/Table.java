package jtool.math.table;


import jtool.code.UT;
import jtool.code.collection.AbstractRandomAccessList;
import jtool.code.collection.NewCollections;
import jtool.math.matrix.*;
import jtool.math.vector.IVector;
import jtool.math.vector.Vectors;

import java.util.ArrayList;
import java.util.List;

/**
 * 方便直接使用 csv 读取结果的数据格式
 * @author liqa
 */
public final class Table extends AbstractTable {
    /** 提供默认的创建 */
    public static Table zeros(final int aRowNum, String... aHeads) {
        if (aHeads==null || aHeads.length==0) return new Table(aRowNum);
        else return new Table(aRowNum, aHeads, NewCollections.from(aHeads.length, col -> Vectors.zeros(aRowNum)));
    }
    public static Table zeros(final int aRowNum, int aColNum) {
        return new Table(aRowNum, aColNum, NewCollections.from(aColNum, col -> Vectors.zeros(aRowNum)));
    }
    
    /** 内部数据为按列排序的 IVector，可以轻松实现扩展列 */
    private final int mRowNum;
    private final List<IVector> mData;
    /** 这些构造函数主要用于避免重复值拷贝数据 */
    public Table(int aRowNum, String[] aHeads, List<IVector> aData) {
        super(aHeads);
        mRowNum = aRowNum;
        mData = aData;
    }
    public Table(int aRowNum, int aColNum, List<IVector> aData) {
        super(aColNum);
        mRowNum = aRowNum;
        mData = aData;
    }
    public Table(String[] aHeads, List<IVector> aData) {this(UT.Code.first(aData).size(), aHeads, aData);}
    public Table(int aRowNum, List<IVector> aData) {this(aRowNum, aData.size(), aData);}
    public Table(List<IVector> aData) {this(UT.Code.first(aData).size(), aData.size(), aData);}
    public Table(int aRowNum) {this(aRowNum, 0, new ArrayList<>());}
    
    
    /** 重写这些接口避免过多的嵌套 */
    @Override public double get(int aRow, String aHead) {return mData.get(mHead2Idx.get(aHead)).get(aRow);}
    @Override public void set(int aRow, String aHead, double aValue) {mData.get(mHead2Idx.get(aHead)).set(aRow, aValue);}
    @Override public List<IVector> cols() {
        // 主要用于避免被意外修改
        return new AbstractRandomAccessList<IVector>() {
            @Override public int size() {return mData.size();}
            @Override public IVector get(int aCol) {return mData.get(aCol);}
        };
    }
    @Override public IVector col(String aHead) {return mData.get(mHead2Idx.get(aHead));}
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mData.size();}
    
    /** AbstractTable stuffs */
    @Override public IMatrix asMatrix() {
        return new RefMatrix() {
            @Override public double get(int aRow, int aCol) {return mData.get(aCol).get(aRow);}
            @Override public void set(int aRow, int aCol, double aValue) {mData.get(aCol).set(aRow, aValue);}
            @Override public double getAndSet(int aRow, int aCol, double aValue) {return mData.get(aCol).getAndSet(aRow, aValue);}
            @Override public int rowNumber() {return mRowNum;}
            @Override public int columnNumber() {return mData.size();}
            
            /** 重写这些接口来加速部分操作，为了避免意料外的问题这里不重写所有可以加速的操作 */
            @Override public IVector col(int aCol) {return mData.get(aCol);}
        };
    }
    @Override protected IVector newColumn_(String aHead) {
        // 先扩展 map
        mHead2Idx.put(aHead, mHeads.size());
        mHeads.add(aHead);
        IVector tCol = Vectors.zeros(mRowNum);
        mData.add(tCol);
        return tCol;
    }
}
