package jse.code.collection;

import jse.code.iterator.ILongIterator;
import jse.math.IDataShell;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.LongConsumer;

import static jse.code.CS.ZL_LONG;
import static jse.code.collection.DoubleList.rangeCheck;

/**
 * 通用的使用 {@code long[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class LongList implements IDataShell<long[]> {
    protected long[] mData;
    protected int mSize = 0;
    private LongList(int aSize, long[] aData) {mSize = aSize; mData = aData;}
    public LongList() {mData = ZL_LONG;}
    public LongList(int aInitSize) {mData = new long[aInitSize];}
    
    public long get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx];
    }
    public void set(int aIdx, long aValue) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aValue;
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public long last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty LongList");
        return mData[mSize-1];
    }
    public long first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty LongList");
        return mData[0];
    }
    
    private void grow_(int aMinCapacity) {
        final int tLen = mData.length;
        long[] oData = mData;
        int tCapacity = Math.max(aMinCapacity, tLen + (tLen>>1));
        mData = new long[tCapacity];
        System.arraycopy(oData, 0, mData, 0, mSize);
    }
    
    /** 高性能接口，在末尾直接增加 aLen 个零，这将只进行扩容操作而不会赋值 */
    @ApiStatus.Experimental
    public void addZeros(int aLen) {
        int tSize = mSize+aLen;
        if (tSize > mData.length) {
            grow_(tSize);
        } else {
            for (int i = mSize; i<tSize; ++i) mData[i] = 0;
        }
        mSize = tSize;
    }
    public void ensureCapacity(int aMinCapacity) {
        if (aMinCapacity > mData.length) grow_(aMinCapacity);
    }
    
    public void addAll(ILongVector aVector) {
        final int aSize = aVector.size();
        final int tSize = mSize+aSize;
        if (tSize > mData.length) grow_(tSize);
        long @Nullable[] aData = getIfHasSameOrderData(aVector);
        if (aData != null) {
            System.arraycopy(aData, IDataShell.internalDataShift(aVector), mData, mSize, aSize);
        } else {
            ILongIterator it = aVector.iterator();
            for (int i = mSize; i < tSize; ++i) mData[i] = it.next();
        }
        mSize = tSize;
    }
    public void addAll(int aSize, ILongVectorGetter aVectorGetter) {
        final int tSize = mSize+aSize;
        if (tSize > mData.length) grow_(tSize);
        for (int i = mSize, j = 0; i < tSize; ++i, ++j) mData[i] = aVectorGetter.get(j);
        mSize = tSize;
    }
    
    public void add(long aValue) {
        if (mData.length <= mSize) grow_(mSize+1);
        mData[mSize] = aValue;
        ++mSize;
    }
    public void trimToSize() {
        if (mData.length != mSize) {
            long[] oData = mData;
            mData = new long[mSize];
            System.arraycopy(oData, 0, mData, 0, mSize);
        }
    }
    public void clear() {
        mSize = 0;
    }
    public void forEach(LongConsumer aCon) {
        for (int i = 0; i < mSize; ++i) aCon.accept(mData[i]);
    }
    
    public List<Long> asList() {
        return new AbstractRandomAccessList<Long>() {
            @Override public Long get(int index) {return LongList.this.get(index);}
            @Override public Long set(int index, Long element) {long oValue = LongList.this.get(index); LongList.this.set(index, element); return oValue;}
            @Override public int size() {return mSize;}
            @Override public boolean add(Long element) {LongList.this.add(element); return true;}
        };
    }
    public LongVector asVec() {
        return new LongVector(mSize, mData);
    }
    public LongVector copy2vec() {
        LongVector rVector = LongVector.zeros(mSize);
        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
        return rVector;
    }
    @ApiStatus.Experimental
    public LongList copy() {
        long[] tData = new long[mSize];
        System.arraycopy(mData, 0, tData, 0, mSize);
        return new LongList(mSize, tData);
    }
    
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof LongList)) return false;
        
        LongList tList = (LongList)aRHS;
        final int tSize = mSize;
        if (tSize != tList.mSize) return false;
        final long[] lData = mData;
        final long[] rData = tList.mData;
        for (int i = 0; i < tSize; ++i) {
            if (lData[i] != rData[i]) return false;
        }
        return true;
    }
    @Override public final int hashCode() {
        final int tSize = mSize;
        final long[] tData = mData;
        int rHashCode = 1;
        for (int i = 0; i < tSize; ++i) {
            rHashCode = 31*rHashCode + Long.hashCode(tData[i]);
        }
        return rHashCode;
    }
    
    
    /** IDataShell stuffs */
    @Override public int internalDataSize() {return size();}
    @Override public void setInternalData(long[] aData) {mData = aData;}
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    
    @Override public long[] internalData() {return mData;}
    @ApiStatus.Internal @Override public long @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof LongList) return ((LongList)aObj).mData;
        if (aObj instanceof LongVector) return ((LongVector)aObj).internalData();
        if (aObj instanceof ShiftLongVector) return ((ShiftLongVector)aObj).internalData();
        if (aObj instanceof long[]) return (long[])aObj;
        return null;
    }
    
    /** Groovy stuffs */
    public LongList append(long aValue) {add(aValue); return this;}
    public LongList appendAll(ILongVector aVector) {addAll(aVector); return this;}
    @VisibleForTesting public LongList leftShift(long aValue) {return append(aValue);}
    @VisibleForTesting public LongList leftShift(ILongVector aVector) {return appendAll(aVector);}
    @VisibleForTesting public final long getAt(int aIdx) {return get(aIdx);}
    @VisibleForTesting public final void putAt(int aIdx, long aValue) {set(aIdx, aValue);}
}
