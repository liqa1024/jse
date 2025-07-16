package jse.code.collection;

import jep.NDArray;
import jse.code.functional.IFloatConsumer;
//import jse.code.iterator.IFloatIterator;
import jse.math.IDataShell;
//import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;

import static jse.code.CS.ZL_FLOAT;

/**
 * 通用的使用 {@code float[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class FloatList implements IDataShell<float[]> {
    static void rangeCheck(int aIdx, int aSize) {
        if (aIdx<0 || aIdx>=aSize) throw new IndexOutOfBoundsException("Index = " + aIdx + ", Size = " + aSize);
    }
    
    protected float[] mData;
    protected int mSize = 0;
    private FloatList(int aSize, float[] aData) {mSize = aSize; mData = aData;}
    public FloatList() {mData = ZL_FLOAT;}
    public FloatList(int aInitSize) {mData = new float[aInitSize];}
    
    /**
     * 转换为 numpy 的数组 {@link NDArray}，在 java 侧根据具体向量类型可能不会进行值拷贝，由于
     * {@link NDArray} 内部实现特性，在 python 中总是会再经历一次值拷贝，此时使用不会有引用问题。
     * @return numpy 的数组 {@link NDArray}
     */
    public final NDArray<float[]> numpy() {
        return new NDArray<>(mData, mSize);
    }
    
    public float get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx];
    }
    public void set(int aIdx, float aValue) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aValue;
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public float last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty FloatList");
        return mData[mSize-1];
    }
    public float first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty FloatList");
        return mData[0];
    }
    
    private void grow_(int aMinCapacity) {
        final int tLen = mData.length;
        float[] oData = mData;
        int tCapacity = Math.max(aMinCapacity, tLen + (tLen>>1));
        mData = new float[tCapacity];
        System.arraycopy(oData, 0, mData, 0, mSize);
    }
    
    /** 高性能接口，在末尾直接增加 aLen 个零，这将只进行扩容操作而不会赋值 */
    @ApiStatus.Experimental
    public void addZeros(int aLen) {
        int tSize = mSize+aLen;
        if (tSize > mData.length) {
            grow_(tSize);
        } else {
            for (int i = mSize; i<tSize; ++i) mData[i] = 0.0f;
        }
        mSize = tSize;
    }
    public void ensureCapacity(int aMinCapacity) {
        if (aMinCapacity > mData.length) grow_(aMinCapacity);
    }
    
//    public void addAll(IVector aVector) {
//        final int aSize = aVector.size();
//        final int tSize = mSize+aSize;
//        if (tSize > mData.length) grow_(tSize);
//        float @Nullable[] aData = getIfHasSameOrderData(aVector);
//        if (aData != null) {
//            System.arraycopy(aData, IDataShell.internalDataShift(aVector), mData, mSize, aSize);
//        } else {
//            IFloatIterator it = aVector.iterator();
//            for (int i = mSize; i < tSize; ++i) mData[i] = it.next();
//        }
//        mSize = tSize;
//    }
//    public void addAll(int aSize, IVectorGetter aVectorGetter) {
//        final int tSize = mSize+aSize;
//        if (tSize > mData.length) grow_(tSize);
//        for (int i = mSize, j = 0; i < tSize; ++i, ++j) mData[i] = aVectorGetter.get(j);
//        mSize = tSize;
//    }
    
    public void add(float aValue) {
        if (mData.length <= mSize) grow_(mSize+1);
        mData[mSize] = aValue;
        ++mSize;
    }
    public void trimToSize() {
        if (mData.length != mSize) {
            float[] oData = mData;
            mData = new float[mSize];
            System.arraycopy(oData, 0, mData, 0, mSize);
        }
    }
    public void clear() {
        mSize = 0;
    }
    public void forEach(IFloatConsumer aCon) {
        for (int i = 0; i < mSize; ++i) aCon.accept(mData[i]);
    }
    
    public List<Float> asList() {
        return new AbstractRandomAccessList<Float>() {
            @Override public Float get(int index) {return FloatList.this.get(index);}
            @Override public Float set(int index, Float element) {float oValue = FloatList.this.get(index); FloatList.this.set(index, element); return oValue;}
            @Override public int size() {return mSize;}
            @Override public boolean add(Float element) {FloatList.this.add(element); return true;}
        };
    }
//    public Vector asVec() {
//        return new Vector(mSize, mData);
//    }
//    public Vector copy2vec() {
//        Vector rVector = Vectors.zeros(mSize);
//        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
//        return rVector;
//    }
    @ApiStatus.Experimental
    public FloatList copy() {
        float[] tData = new float[mSize];
        System.arraycopy(mData, 0, tData, 0, mSize);
        return new FloatList(mSize, tData);
    }
    
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof FloatList)) return false;
        
        FloatList tList = (FloatList)aRHS;
        final int tSize = mSize;
        if (tSize != tList.mSize) return false;
        final float[] lData = mData;
        final float[] rData = tList.mData;
        for (int i = 0; i < tSize; ++i) {
            if (Float.compare(lData[i], rData[i]) != 0) return false;
        }
        return true;
    }
    @Override public final int hashCode() {
        final int tSize = mSize;
        final float[] tData = mData;
        int rHashCode = 1;
        for (int i = 0; i < tSize; ++i) {
            rHashCode = 31*rHashCode + Float.hashCode(tData[i]);
        }
        return rHashCode;
    }
    
    /** IDataShell stuffs */
    @Override public int internalDataSize() {return size();}
    @Override public void setInternalData(float[] aData) {mData = aData;}
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    
    @Override public float[] internalData() {return mData;}
    @ApiStatus.Internal @Override public float @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof FloatList) return ((FloatList)aObj).mData;
        if (aObj instanceof float[]) return (float[])aObj;
        return null;
    }
    
    /** Groovy stuffs */
    public FloatList append(float aValue) {add(aValue); return this;}
//    public FloatList appendAll(IVector aVector) {addAll(aVector); return this;}
    @VisibleForTesting public FloatList leftShift(float aValue) {return append(aValue);}
//    @VisibleForTesting public FloatList leftShift(IVector aVector) {return appendAll(aVector);}
    @VisibleForTesting public final float getAt(int aIdx) {return get(aIdx);}
    @VisibleForTesting public final void putAt(int aIdx, float aValue) {set(aIdx, aValue);}
}
