package jse.code.collection;

import jse.code.iterator.IIntIterator;
import jse.math.IDataShell;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.IntConsumer;

import static jse.code.CS.ZL_INT;
import static jse.code.collection.DoubleList.rangeCheck;

/**
 * 通用的使用 {@code int[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class IntList implements ISlice, IDataShell<int[]> {
    protected int[] mData;
    protected int mSize = 0;
    private IntList(int aSize, int[] aData) {mSize = aSize; mData = aData;}
    public IntList() {mData = ZL_INT;}
    public IntList(int aInitSize) {mData = new int[aInitSize];}
    
    public int get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx];
    }
    public void set(int aIdx, int aValue) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aValue;
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public int last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty IntList");
        return mData[mSize-1];
    }
    public int first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty IntList");
        return mData[0];
    }
    
    private void grow_(int aMinCapacity) {
        final int tLen = mData.length;
        int[] oData = mData;
        int tCapacity = Math.max(aMinCapacity, tLen + (tLen>>1));
        mData = new int[tCapacity];
        System.arraycopy(oData, 0, mData, 0, tLen);
    }
    
    /** 高性能接口，在末尾直接增加 aLen 个零，这将只进行扩容操作而不会赋值 */
    @ApiStatus.Experimental
    public void addZeros(int aLen) {
        int tSize = mSize+aLen;
        if (tSize > mData.length) grow_(tSize);
        mSize = tSize;
    }
    
    public void addAll(IIntVector aVector) {
        final int aSize = aVector.size();
        final int tSize = mSize+aSize;
        if (tSize > mData.length) grow_(tSize);
        int @Nullable[] aData = getIfHasSameOrderData(aVector);
        if (aData != null) {
            System.arraycopy(aData, IDataShell.internalDataShift(aVector), mData, mSize, aSize);
        } else {
            IIntIterator it = aVector.iterator();
            for (int i = mSize; i < tSize; ++i) mData[i] = it.next();
        }
        mSize = tSize;
    }
    public void addAll(int aSize, IIntVectorGetter aVectorGetter) {
        final int tSize = mSize+aSize;
        if (tSize > mData.length) grow_(tSize);
        for (int i = mSize, j = 0; i < tSize; ++i, ++j) mData[i] = aVectorGetter.get(j);
        mSize = tSize;
    }
    
    public void add(int aValue) {
        if (mData.length <= mSize) grow_(mSize+1);
        mData[mSize] = aValue;
        ++mSize;
    }
    public void trimToSize() {
        if (mData.length != mSize) {
            int[] oData = mData;
            mData = new int[mSize];
            System.arraycopy(oData, 0, mData, 0, mSize);
        }
    }
    public void clear() {
        mSize = 0;
    }
    public void forEach(IntConsumer aCon) {
        for (int i = 0; i < mSize; ++i) aCon.accept(mData[i]);
    }
    
    public List<Integer> asList() {
        return new AbstractRandomAccessList<Integer>() {
            @Override public Integer get(int index) {return IntList.this.get(index);}
            @Override public Integer set(int index, Integer element) {int oValue = IntList.this.get(index); IntList.this.set(index, element); return oValue;}
            @Override public int size() {return mSize;}
            @Override public boolean add(Integer element) {IntList.this.add(element); return true;}
        };
    }
    public IntVector asVec() {
        return new IntVector(mSize, mData);
    }
    public IntVector copy2vec() {
        IntVector rVector = IntVector.zeros(mSize);
        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
        return rVector;
    }
    @ApiStatus.Experimental
    public IntList copy() {
        int[] tData = new int[mSize];
        System.arraycopy(mData, 0, tData, 0, mSize);
        return new IntList(mSize, tData);
    }
    @ApiStatus.Experimental
    public void removeLast() {
        if (isEmpty()) throw new NoSuchElementException("Cannot removeLast() from an empty IntList");
        --mSize;
    }
    
    
    /** IDataShell stuffs */
    @Override public int internalDataSize() {return size();}
    @Override public void setInternalData(int[] aData) {mData = aData;}
    @Override public IntList newShell() {return new IntList(mSize, null);}
    @Override public int[] internalData() {return mData;}
    @ApiStatus.Internal @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof IntList) return ((IntList)aObj).mData;
        if (aObj instanceof IntVector) return ((IntVector)aObj).internalData();
        if (aObj instanceof ShiftIntVector) return ((ShiftIntVector)aObj).internalData();
        if (aObj instanceof int[]) return (int[])aObj;
        return null;
    }
    
    
    /** Groovy stuffs */
    public IntList append(int aValue) {add(aValue); return this;}
    public IntList appendAll(IIntVector aVector) {addAll(aVector); return this;}
    @VisibleForTesting public IntList leftShift(int aValue) {return append(aValue);}
    @VisibleForTesting public IntList leftShift(IIntVector aVector) {return appendAll(aVector);}
    @VisibleForTesting public final int getAt(int aIdx) {return get(aIdx);}
    @VisibleForTesting public final void putAt(int aIdx, int aValue) {set(aIdx, aValue);}
}
