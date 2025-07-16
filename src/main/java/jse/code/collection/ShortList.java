package jse.code.collection;

import jep.NDArray;
import jse.code.functional.IShortConsumer;
//import jse.code.iterator.IShortIterator;
import jse.math.IDataShell;
//import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;

import static jse.code.CS.ZL_SHORT;
import static jse.code.collection.DoubleList.rangeCheck;

/**
 * 通用的使用 {@code short[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class ShortList implements IDataShell<short[]> {
    protected short[] mData;
    protected int mSize = 0;
    private ShortList(int aSize, short[] aData) {mSize = aSize; mData = aData;}
    public ShortList() {mData = ZL_SHORT;}
    public ShortList(int aInitSize) {mData = new short[aInitSize];}
    
    /**
     * 转换为 numpy 的数组 {@link NDArray}，在 java 侧根据具体向量类型可能不会进行值拷贝，由于
     * {@link NDArray} 内部实现特性，在 python 中总是会再经历一次值拷贝，此时使用不会有引用问题。
     * @return numpy 的数组 {@link NDArray}
     */
    public final NDArray<short[]> numpy() {
        return new NDArray<>(mData, mSize);
    }
    
    public short get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx];
    }
    public void set(int aIdx, short aValue) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aValue;
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public short last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty ShortList");
        return mData[mSize-1];
    }
    public short first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty ShortList");
        return mData[0];
    }
    
    private void grow_(int aMinCapacity) {
        final int tLen = mData.length;
        short[] oData = mData;
        int tCapacity = Math.max(aMinCapacity, tLen + (tLen>>1));
        mData = new short[tCapacity];
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
    
//    public void addAll(IShortVector aVector) {
//        final int aSize = aVector.size();
//        final int tSize = mSize+aSize;
//        if (tSize > mData.length) grow_(tSize);
//        short @Nullable[] aData = getIfHasSameOrderData(aVector);
//        if (aData != null) {
//            System.arraycopy(aData, IDataShell.internalDataShift(aVector), mData, mSize, aSize);
//        } else {
//            IShortIterator it = aVector.iterator();
//            for (int i = mSize; i < tSize; ++i) mData[i] = it.next();
//        }
//        mSize = tSize;
//    }
//    public void addAll(int aSize, IShortVectorGetter aVectorGetter) {
//        final int tSize = mSize+aSize;
//        if (tSize > mData.length) grow_(tSize);
//        for (int i = mSize, j = 0; i < tSize; ++i, ++j) mData[i] = aVectorGetter.get(j);
//        mSize = tSize;
//    }
    
    public void add(short aValue) {
        if (mData.length <= mSize) grow_(mSize+1);
        mData[mSize] = aValue;
        ++mSize;
    }
    public void trimToSize() {
        if (mData.length != mSize) {
            short[] oData = mData;
            mData = new short[mSize];
            System.arraycopy(oData, 0, mData, 0, mSize);
        }
    }
    public void clear() {
        mSize = 0;
    }
    public void forEach(IShortConsumer aCon) {
        for (int i = 0; i < mSize; ++i) aCon.accept(mData[i]);
    }
    
    public List<Short> asList() {
        return new AbstractRandomAccessList<Short>() {
            @Override public Short get(int index) {return ShortList.this.get(index);}
            @Override public Short set(int index, Short element) {short oValue = ShortList.this.get(index); ShortList.this.set(index, element); return oValue;}
            @Override public int size() {return mSize;}
            @Override public boolean add(Short element) {ShortList.this.add(element); return true;}
        };
    }
//    public ShortVector asVec() {
//        return new ShortVector(mSize, mData);
//    }
//    public ShortVector copy2vec() {
//        ShortVector rVector = ShortVector.zeros(mSize);
//        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
//        return rVector;
//    }
    @ApiStatus.Experimental
    public ShortList copy() {
        short[] tData = new short[mSize];
        System.arraycopy(mData, 0, tData, 0, mSize);
        return new ShortList(mSize, tData);
    }
    
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof ShortList)) return false;
        
        ShortList tList = (ShortList)aRHS;
        final int tSize = mSize;
        if (tSize != tList.mSize) return false;
        final short[] lData = mData;
        final short[] rData = tList.mData;
        for (int i = 0; i < tSize; ++i) {
            if (lData[i] != rData[i]) return false;
        }
        return true;
    }
    @Override public final int hashCode() {
        final int tSize = mSize;
        final short[] tData = mData;
        int rHashCode = 1;
        for (int i = 0; i < tSize; ++i) {
            rHashCode = 31*rHashCode + Short.hashCode(tData[i]);
        }
        return rHashCode;
    }
    
    
    /** IDataShell stuffs */
    @Override public int internalDataSize() {return size();}
    @Override public void setInternalData(short[] aData) {mData = aData;}
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    
    @Override public short[] internalData() {return mData;}
    @ApiStatus.Internal @Override public short @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ShortList) return ((ShortList)aObj).mData;
//        if (aObj instanceof ShortVector) return ((ShortVector)aObj).internalData();
//        if (aObj instanceof ShiftShortVector) return ((ShiftShortVector)aObj).internalData();
        if (aObj instanceof short[]) return (short[])aObj;
        return null;
    }
    
    /** Groovy stuffs */
    public ShortList append(short aValue) {add(aValue); return this;}
//    public ShortList appendAll(IShortVector aVector) {addAll(aVector); return this;}
    @VisibleForTesting public ShortList leftShift(short aValue) {return append(aValue);}
//    @VisibleForTesting public ShortList leftShift(IShortVector aVector) {return appendAll(aVector);}
    @VisibleForTesting public final short getAt(int aIdx) {return get(aIdx);}
    @VisibleForTesting public final void putAt(int aIdx, short aValue) {set(aIdx, aValue);}
}
