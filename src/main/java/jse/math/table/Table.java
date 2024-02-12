package jse.math.table;

import jse.code.UT;
import jse.code.collection.AbstractRandomAccessList;
import jse.code.collection.NewCollections;
import jse.math.IDataShell;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RefMatrix;
import jse.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 方便直接使用 csv 读取结果的数据格式
 * @author liqa
 */
public final class Table extends AbstractTable implements IDataShell<List<Vector>> {
    /** 提供默认的创建 */
    public static Table zeros(final int aRowNum, String... aHeads) {
        if (aHeads==null || aHeads.length==0) return new Table(aRowNum);
        else return new Table(aRowNum, aHeads, NewCollections.from(aHeads.length, col -> Vector.zeros(aRowNum)));
    }
    public static Table zeros(final int aRowNum, int aColNum) {
        return new Table(aRowNum, aColNum, NewCollections.from(aColNum, col -> Vector.zeros(aRowNum)));
    }
    
    /** 内部数据为按列排序的 Vector，可以轻松实现扩展列 */
    private final int mRowNum;
    private List<Vector> mData;
    /** 这些构造函数主要用于避免重复值拷贝数据 */
    Table(int aRowNum, List<String> aHeads, List<Vector> aData) {
        super(aHeads);
        mRowNum = aRowNum;
        mData = aData;
    }
    public Table(int aRowNum, String[] aHeads, List<Vector> aData) {
        super(aHeads);
        mRowNum = aRowNum;
        mData = aData;
    }
    public Table(int aRowNum, int aColNum, List<Vector> aData) {
        super(aColNum);
        mRowNum = aRowNum;
        mData = aData;
    }
    public Table(String[] aHeads, List<Vector> aData) {this(UT.Code.first(aData).size(), aHeads, aData);}
    public Table(int aRowNum, List<Vector> aData) {this(aRowNum, aData.size(), aData);}
    public Table(List<Vector> aData) {this(UT.Code.first(aData).size(), aData.size(), aData);}
    public Table(int aRowNum) {this(aRowNum, 0, new ArrayList<>());}
    
    
    /** 重写这些接口避免过多的嵌套 */
    @Override public double get(int aRow, String aHead) {return mData.get(mHead2Idx.get(aHead)).get(aRow);}
    @Override public void set(int aRow, String aHead, double aValue) {mData.get(mHead2Idx.get(aHead)).set(aRow, aValue);}
    @Override public List<Vector> cols() {
        // 主要用于避免被意外修改
        return new AbstractRandomAccessList<Vector>() {
            @Override public int size() {return mData.size();}
            @Override public Vector get(int aCol) {return mData.get(aCol);}
        };
    }
    @Override public Vector col(String aHead) {return mData.get(mHead2Idx.get(aHead));}
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mData.size();}
    
    @Override public Table copy() {
        Table rTable = new Table(mRowNum, NewCollections.from(mHeads), NewCollections.from(columnNumber(), col -> Vector.zeros(mRowNum)));
        rTable.asMatrix().fill(asMatrix());
        return rTable;
    }
    
    /** AbstractTable stuffs */
    @Override public IMatrix asMatrix() {
        return new RefMatrix() {
            @Override public double get(int aRow, int aCol) {return mData.get(aCol).get(aRow);}
            @Override public void set(int aRow, int aCol, double aValue) {mData.get(aCol).set(aRow, aValue);}
            @Override public double getAndSet(int aRow, int aCol, double aValue) {return mData.get(aCol).getAndSet(aRow, aValue);}
            @Override public int rowNumber() {return mRowNum;}
            @Override public int columnNumber() {return mData.size();}
            
            /** 重写这些接口来加速部分操作，为了避免意料外的问题这里不重写所有可以加速的操作 */
            @Override public List<Vector> cols() {return Table.this.cols();}
            @Override public Vector col(int aCol) {return mData.get(aCol);}
        };
    }
    @Override protected Vector newColumn_(String aHead) {
        // 先扩展 map
        mHead2Idx.put(aHead, mHeads.size());
        mHeads.add(aHead);
        Vector tCol = Vector.zeros(mRowNum);
        mData.add(tCol);
        return tCol;
    }
    
    /** DataShell stuffs */
    @Override public void setInternalData(List<Vector> aData) {mData = aData;}
    @Override public List<Vector> internalData() {return mData;}
    @Override public int internalDataSize() {return mRowNum;}
    @Override public Table newShell() {return new Table(mRowNum, NewCollections.from(mHeads), null);}
    @Override public @Nullable List<Vector> getIfHasSameOrderData(Object aObj) {
        // 简单处理，这里认为所有 Table 内部数据都不是相同 order 的
        return null;
    }
}
