package jse.math.table;

import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.NewCollections;
import jse.math.IDataShell;
import jse.math.matrix.ColumnMatrix;
import jse.math.vector.ShiftVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 方便直接使用 csv 读取结果的数据格式
 * @author liqa
 */
public class Table extends AbstractTable implements IDataShell<DoubleList> {
    /** 提供默认的创建 */
    public static Table zeros(int aRowNum) {
        return new Table(aRowNum);
    }
    public static Table zeros(int aRowNum, String... aHeads) {
        if (aHeads==null || aHeads.length==0) {
            return new Table(aRowNum);
        } else {
            int tSize = aRowNum * aHeads.length;
            DoubleList tData = new DoubleList(tSize);
            tData.addZeros(tSize);
            return new Table(aRowNum, tData, aHeads);
        }
    }
    public static Table zeros(final int aRowNum, int aColNum) {
        int tSize = aRowNum * aColNum;
        DoubleList tData = new DoubleList(tSize);
        tData.addZeros(tSize);
        return new Table(aRowNum, aColNum, tData);
    }
    
    /** 现在内部数据改为 {@link DoubleList} */
    private final int mRowNum;
    private DoubleList mData;
    private @Nullable ColumnMatrix mMatrix = null;
    /** 这些构造函数主要用于避免重复值拷贝数据 */
    protected Table(int aRowNum, DoubleList aData, List<String> aHeads) {
        super(aHeads);
        mRowNum = aRowNum;
        mData = aData;
    }
    public Table(int aRowNum, DoubleList aData, String... aHeads) {
        super(aHeads);
        mRowNum = aRowNum;
        mData = aData;
    }
    public Table(int aRowNum, int aColNum, DoubleList aData) {
        super(aColNum);
        mRowNum = aRowNum;
        mData = aData;
    }
    public Table(int aRowNum, DoubleList aData) {this(aRowNum, 0, aData);}
    public Table(int aRowNum) {this(aRowNum, 0, new DoubleList());}
    
    
    /** 重写这些接口实现直接返回 {@link ShiftVector} */
    @Override public final Map<String, ShiftVector> cols() {
        return new AbstractMap<String, ShiftVector>() {
            @NotNull @Override public Set<Entry<String, ShiftVector>> entrySet() {
                return new AbstractSet<Entry<String, ShiftVector>>() {
                    @Override public @NotNull Iterator<Entry<String, ShiftVector>> iterator() {
                        return AbstractCollections.map(mHeads.iterator(), head -> new Entry<String, ShiftVector>(){
                            @Override public String getKey() {return head;}
                            @Override public ShiftVector getValue() {return col(head);}
                            @Override public ShiftVector setValue(ShiftVector value) {throw new UnsupportedOperationException("setValue");}
                        });
                    }
                    @Override public int size() {return columnNumber();}
                };
            }
            @Override public ShiftVector get(Object key) {return asMatrix().col(mHead2Idx.get(key));}
            @Override public boolean containsKey(Object key) {return mHead2Idx.containsKey(key);}
            @Override public ShiftVector remove(Object key) {throw new UnsupportedOperationException("remove");}
            @Override public int size() {return columnNumber();}
            @Override public void clear() {throw new UnsupportedOperationException("clear");}
            @Override public ShiftVector put(String key, ShiftVector value) {throw new UnsupportedOperationException("put");}
        };
    }
    @Override public final ShiftVector col(String aHead) {return asMatrix().col(mHead2Idx.get(aHead));}
    @Override public final int rowNumber() {return mRowNum;}
    
    @Override public Table copy() {
        return new Table(mRowNum, mData.copy(), NewCollections.from(mHeads));
    }
    
    /** AbstractTable stuffs */
    @Override public final ColumnMatrix asMatrix() {
        if (mMatrix == null) mMatrix = new ColumnMatrix(mRowNum, columnNumber(), mData.internalData());
        return mMatrix;
    }
    @Override protected final ShiftVector newColumn_(String aHead) {
        // 标记 null 表示旧矩阵失效
        mMatrix = null;
        // 先扩展 map
        mHead2Idx.put(aHead, mHeads.size());
        mHeads.add(aHead);
        // 然后扩展数据
        mData.addZeros(mRowNum);
        return asMatrix().col(columnNumber()-1);
    }
    
    /** DataShell stuffs */
    @Override public final void setInternalData(DoubleList aData) {mData = aData; mMatrix = null;}
    @Override public final DoubleList internalData() {mMatrix = null; return mData;}
    @Override public final int internalDataSize() {return mData.size();}
    @Override public Table newShell() {return new Table(mRowNum, null, NewCollections.from(mHeads));}
    @Override public @Nullable DoubleList getIfHasSameOrderData(Object aObj) {
        // 简单处理，这里认为所有 Table 内部数据都不是相同 order 的
        return null;
    }
}
