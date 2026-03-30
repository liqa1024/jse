package jse.math.vector;

import jse.code.collection.FloatList;
import jse.code.functional.IFloatUnaryOperator;
import jse.code.iterator.IFloatIterator;
import jse.code.iterator.IFloatSetIterator;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

import static jse.math.vector.AbstractVector.*;

/**
 * 支持将内部的 float[] 进行平移访问的 FloatVector，理论拥有和 {@link FloatVector} 几乎一样的性能
 * <p>
 * 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link FloatVector}
 * @author liqa
 */
public final class ShiftFloatVector extends FloatArrayVector {
    private int mSize;
    private int mShift;
    public ShiftFloatVector(int aSize, int aShift, float[] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftFloatVector(int aShift, float[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    @Override public void setInternalDataShift(int aShift) {mShift = aShift;}
    
    /** ILogicalVector stuffs */
    @Override public float get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx + mShift];}
    @Override public void set(int aIdx, float aValue) {rangeCheck(aIdx, mSize); mData[aIdx + mShift] = aValue;}
    @Override public float getAndSet(int aIdx, float aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        float oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected FloatVector newZeros_(int aSize) {return FloatVector.zeros(aSize);}
    @Override public FloatVector copy() {return (FloatVector)super.copy();}
    
    @Override public float @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof FloatVector) return ((FloatVector)aObj).mData;
        if (aObj instanceof ShiftFloatVector) return ((ShiftFloatVector)aObj).mData;
        if (aObj instanceof FloatList) return ((FloatList)aObj).internalData();
        if (aObj instanceof float[]) return (float[])aObj;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return mShift;}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftFloatVector} */
    @Override public ShiftFloatVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftFloatVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        aIdx1 += mShift;
        aIdx2 += mShift;
        float tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void update(int aIdx, IFloatUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[aIdx] = aOpt.applyAsFloat(mData[aIdx]);
    }
    @Override public float getAndUpdate(int aIdx, IFloatUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        float tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsFloat(tValue);
        return tValue;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public float last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty FloatVector");
        return mData[mSize-1+mShift];
    }
    @Override public float first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty FloatVector");
        return mData[mShift];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IFloatIterator iterator() {
        return new IFloatIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public float next() {
                if (hasNext()) {
                    float tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IFloatSetIterator setIterator() {
        return new IFloatSetIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(float aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public float next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(float aValue) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
