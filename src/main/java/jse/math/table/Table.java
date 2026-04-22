package jse.math.table;

import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.NewCollections;
import jse.math.IDataShell;
import jse.math.matrix.ColumnMatrix;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * 方便直接使用 csv 读取结果的数据格式
 * @author liqa
 */
public class Table extends AbstractTable implements IDataShell<DoubleList> {
    /** 提供默认的创建 */
    public static Table zeros(int aNumRows) {
        return new Table(aNumRows);
    }
    public static Table zeros(int aNumRows, String... aHeads) {
        if (aHeads==null || aHeads.length==0) {
            return new Table(aNumRows);
        } else {
            int tSize = aNumRows * aHeads.length;
            DoubleList tData = new DoubleList();
            tData.addZeros(tSize);
            return new Table(aNumRows, tData, aHeads);
        }
    }
    public static Table zeros(final int aNumRows, int aNumCols) {
        int tSize = aNumRows * aNumCols;
        DoubleList tData = new DoubleList();
        tData.addZeros(tSize);
        return new Table(aNumRows, aNumCols, tData);
    }
    
    /** 现在内部数据改为 {@link DoubleList} */
    private final int mNumRows;
    private DoubleList mData;
    private @Nullable ColumnMatrix mMatrix = null;
    /** 这些构造函数主要用于避免重复值拷贝数据 */
    protected Table(int aNumRows, DoubleList aData, List<String> aHeads) {
        super(aHeads);
        mNumRows = aNumRows;
        mData = aData;
    }
    public Table(int aNumRows, DoubleList aData, String... aHeads) {
        super(aHeads);
        mNumRows = aNumRows;
        mData = aData;
    }
    public Table(int aNumRows, int aNumCols, DoubleList aData) {
        super(aNumCols);
        mNumRows = aNumRows;
        mData = aData;
    }
    public Table(int aNumRows, DoubleList aData) {this(aNumRows, 0, aData);}
    public Table(int aNumRows) {this(aNumRows, 0, new DoubleList());}
    
    
    /** 重写这些接口实现直接返回 {@link Vector} */
    @Override public final Map<String, Vector> asMap() {
        return new AbstractMap<String, Vector>() {
            @NotNull @Override public Set<Entry<String, Vector>> entrySet() {
                return new AbstractSet<Entry<String, Vector>>() {
                    @Override public @NotNull Iterator<Entry<String, Vector>> iterator() {
                        return AbstractCollections.map(mHeads.iterator(), head -> new Entry<String, Vector>(){
                            @Override public String getKey() {return head;}
                            @Override public Vector getValue() {return col(head);}
                            @Override public Vector setValue(Vector value) {throw new UnsupportedOperationException("setValue");}
                        });
                    }
                    @Override public int size() {return ncols();}
                };
            }
            @Override public Vector get(Object key) {return asMatrix().col(mHead2Idx.get(key));}
            @Override public boolean containsKey(Object key) {return mHead2Idx.containsKey(key);}
            @Override public Vector remove(Object key) {throw new UnsupportedOperationException("remove");}
            @Override public int size() {return ncols();}
            @Override public void clear() {throw new UnsupportedOperationException("clear");}
            @Override public Vector put(String key, Vector value) {throw new UnsupportedOperationException("put");}
        };
    }
    @Override public final List<? extends Vector> cols() {return asMatrix().cols();}
    @Override public final Vector col(String aHead) {return asMatrix().col(mHead2Idx.get(aHead));}
    @Override public final int nrows() {return mNumRows;}
    
    @Override public Table copy() {
        return new Table(mNumRows, mData.copy(), NewCollections.from(mHeads));
    }
    
    /** AbstractTable stuffs */
    @Override public final ColumnMatrix asMatrix() {
        if (mMatrix == null) mMatrix = new ColumnMatrix(mNumRows, ncols(), mData.internalData());
        return mMatrix;
    }
    @Override protected final Vector newColumn_(String aHead) {
        // 标记 null 表示旧矩阵失效
        mMatrix = null;
        // 先扩展 map
        mHead2Idx.put(aHead, mHeads.size());
        mHeads.add(aHead);
        // 然后扩展数据
        mData.addZeros(mNumRows);
        return asMatrix().col(ncols()-1);
    }
    
    /** DataShell stuffs */
    @Override public final void setInternalData(DoubleList aData) {mData = aData; mMatrix = null;}
    @Override public final DoubleList internalData() {mMatrix = null; return mData;}
    @Override public final int internalDataSize() {return mData.size();}
    
    @Override public @Nullable DoubleList getIfHasSameOrderData(Object aObj) {
        // 简单处理，这里认为所有 Table 内部数据都不是相同 order 的
        return null;
    }
}
