package jtool.code.collection;

import jtool.code.functional.IBooleanConsumer1;
import jtool.math.IDataShell;
import jtool.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.NoSuchElementException;

import static jtool.code.CS.ZL_BOOL;

/**
 * 通用的使用 {@code boolean[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class BooleanList implements IDataShell<boolean[]> {
    protected boolean[] mData;
    protected int mSize = 0;
    private BooleanList(int aSize, boolean[] aData) {mSize = aSize; mData = aData;}
    public BooleanList() {mData = ZL_BOOL; mSize = 0;}
    public BooleanList(int aInitSize) {mData = new boolean[aInitSize];}
    
    public boolean get(int aIdx) {
        if (aIdx >= mSize) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return mData[aIdx];
    }
    public void set(int aIdx, boolean aValue) {
        if (aIdx >= mSize) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        mData[aIdx] = aValue;
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public boolean last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty BooleanList");
        return mData[mSize-1];
    }
    public boolean first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty BooleanList");
        return mData[0];
    }
    
    public void add(boolean aValue) {
        if (mData.length == 0) {
            mData = new boolean[1];
        } else
        if (mData.length <= mSize) {
            boolean[] oData = mData;
            mData = new boolean[oData.length * 2];
            System.arraycopy(oData, 0, mData, 0, oData.length);
        }
        mData[mSize] = aValue;
        ++mSize;
    }
    public void trimToSize() {
        if (mData.length != mSize) {
            boolean[] oData = mData;
            mData = new boolean[mSize];
            System.arraycopy(oData, 0, mData, 0, mSize);
        }
    }
    public void clear() {
        mSize = 0;
    }
    public void forEach(IBooleanConsumer1 aCon) {
        for (int i = 0; i < mSize; ++i) aCon.run(mData[i]);
    }
    
    public List<Boolean> asList() {
        return new AbstractRandomAccessList<Boolean>() {
            @Override public Boolean get(int index) {return BooleanList.this.get(index);}
            @Override public Boolean set(int index, Boolean element) {boolean oValue = BooleanList.this.get(index); BooleanList.this.set(index, element); return oValue;}
            @Override public int size() {return BooleanList.this.size();}
            @Override public boolean add(Boolean element) {BooleanList.this.add(element); return true;}
        };
    }
    @ApiStatus.Experimental
    public @Unmodifiable List<Boolean> asConstList() {
        return new AbstractRandomAccessList<Boolean>() {
            @Override public Boolean get(int index) {return BooleanList.this.get(index);}
            @Override public int size() {return BooleanList.this.size();}
        };
    }
    public ILogicalVector asVec() {
        return new RefLogicalVector() {
            @Override public boolean get_(int aIdx) {return mData[aIdx];}
            @Override public void set_(int aIdx, boolean aValue) {mData[aIdx] = aValue;}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public ILogicalVector asConstVec() {
        return new RefLogicalVector() {
            @Override public boolean get_(int aIdx) {return mData[aIdx];}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public LogicalVector copy2vec() {
        LogicalVector rVector = LogicalVector.zeros(mSize);
        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
        return rVector;
    }
    
    
    /** IDataShell stuffs */
    @Override  public int internalDataSize() {return size();}
    @Override public void setInternalData(boolean[] aData) {mData = aData;}
    @Override public BooleanList newShell() {return new BooleanList(mSize, null);}
    @Override public boolean[] internalData() {return mData;}
    @ApiStatus.Internal @Override public boolean @Nullable [] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof BooleanList) return ((BooleanList)aObj).mData;
        if (aObj instanceof LogicalVector) return ((LogicalVector)aObj).internalData();
        if (aObj instanceof boolean[]) return (boolean[])aObj;
        return null;
    }
}
