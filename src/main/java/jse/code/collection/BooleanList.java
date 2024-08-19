package jse.code.collection;

import jse.code.functional.IBooleanConsumer;
import jse.code.iterator.IBooleanIterator;
import jse.math.IDataShell;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;

import static jse.code.CS.ZL_BOOL;
import static jse.code.collection.DoubleList.rangeCheck;

/**
 * 通用的使用 {@code boolean[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class BooleanList implements IDataShell<boolean[]> {
    protected boolean[] mData;
    protected int mSize = 0;
    private BooleanList(int aSize, boolean[] aData) {mSize = aSize; mData = aData;}
    public BooleanList() {mData = ZL_BOOL;}
    public BooleanList(int aInitSize) {mData = new boolean[aInitSize];}
    
    public boolean get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx];
    }
    public void set(int aIdx, boolean aValue) {
        rangeCheck(aIdx, mSize);
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
    
    private void grow_(int aMinCapacity) {
        final int tLen = mData.length;
        boolean[] oData = mData;
        int tCapacity = Math.max(aMinCapacity, tLen + (tLen>>1));
        mData = new boolean[tCapacity];
        System.arraycopy(oData, 0, mData, 0, tLen);
    }
    
    /** 高性能接口，在末尾直接增加 aLen 个零，这将只进行扩容操作而不会赋值 */
    @ApiStatus.Experimental
    public void addZeros(int aLen) {
        int tSize = mSize+aLen;
        if (tSize > mData.length) grow_(tSize);
        mSize = tSize;
    }
    
    public void addAll(ILogicalVector aVector) {
        final int aSize = aVector.size();
        final int tSize = mSize+aSize;
        if (tSize > mData.length) grow_(tSize);
        boolean @Nullable[] aData = getIfHasSameOrderData(aVector);
        if (aData != null) {
            System.arraycopy(aData, IDataShell.internalDataShift(aVector), mData, mSize, aSize);
        } else {
            IBooleanIterator it = aVector.iterator();
            for (int i = mSize; i < tSize; ++i) mData[i] = it.next();
        }
        mSize = tSize;
    }
    public void addAll(int aSize, ILogicalVectorGetter aVectorGetter) {
        final int tSize = mSize+aSize;
        if (tSize > mData.length) grow_(tSize);
        for (int i = mSize, j = 0; i < tSize; ++i, ++j) mData[i] = aVectorGetter.get(j);
        mSize = tSize;
    }
    
    public void add(boolean aValue) {
        if (mData.length <= mSize) grow_(mSize+1);
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
    public void forEach(IBooleanConsumer aCon) {
        for (int i = 0; i < mSize; ++i) aCon.accept(mData[i]);
    }
    
    public List<Boolean> asList() {
        return new AbstractRandomAccessList<Boolean>() {
            @Override public Boolean get(int index) {return BooleanList.this.get(index);}
            @Override public Boolean set(int index, Boolean element) {boolean oValue = BooleanList.this.get(index); BooleanList.this.set(index, element); return oValue;}
            @Override public int size() {return mSize;}
            @Override public boolean add(Boolean element) {BooleanList.this.add(element); return true;}
        };
    }
    public LogicalVector asVec() {
        return new LogicalVector(mSize, mData);
    }
    public LogicalVector copy2vec() {
        LogicalVector rVector = LogicalVector.zeros(mSize);
        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
        return rVector;
    }
    @ApiStatus.Experimental
    public BooleanList copy() {
        boolean[] tData = new boolean[mSize];
        System.arraycopy(mData, 0, tData, 0, mSize);
        return new BooleanList(mSize, tData);
    }
    
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof BooleanList)) return false;
        
        BooleanList tList = (BooleanList)aRHS;
        final int tSize = mSize;
        if (tSize != tList.mSize) return false;
        final boolean[] lData = mData;
        final boolean[] rData = tList.mData;
        for (int i = 0; i < tSize; ++i) {
            if (lData[i] != rData[i]) return false;
        }
        return true;
    }
    @Override public final int hashCode() {
        final int tSize = mSize;
        final boolean[] tData = mData;
        int rHashCode = 1;
        for (int i = 0; i < tSize; ++i) {
            rHashCode = 31*rHashCode + Boolean.hashCode(tData[i]);
        }
        return rHashCode;
    }
    
    /** IDataShell stuffs */
    @Override public int internalDataSize() {return size();}
    @Override public void setInternalData(boolean[] aData) {mData = aData;}
    @Override public BooleanList newShell() {return new BooleanList(mSize, null);}
    @Override public boolean[] internalData() {return mData;}
    @ApiStatus.Internal @Override public boolean @Nullable [] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof BooleanList) return ((BooleanList)aObj).mData;
        if (aObj instanceof LogicalVector) return ((LogicalVector)aObj).internalData();
        if (aObj instanceof ShiftLogicalVector) return ((ShiftLogicalVector)aObj).internalData();
        if (aObj instanceof boolean[]) return (boolean[])aObj;
        return null;
    }
    
    /** Groovy stuffs */
    public BooleanList append(boolean aValue) {add(aValue); return this;}
    public BooleanList appendAll(ILogicalVector aVector) {addAll(aVector); return this;}
    @VisibleForTesting public BooleanList leftShift(boolean aValue) {return append(aValue);}
    @VisibleForTesting public BooleanList leftShift(ILogicalVector aVector) {return appendAll(aVector);}
    @VisibleForTesting public final boolean getAt(int aIdx) {return get(aIdx);}
    @VisibleForTesting public final void putAt(int aIdx, boolean aValue) {set(aIdx, aValue);}
}
