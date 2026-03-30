package jse.math.vector;

import jep.NDArray;
import jse.code.collection.FloatList;
import jse.code.functional.IFloatUnaryOperator;
import jse.code.iterator.IFloatIterator;
import jse.code.iterator.IFloatSetIterator;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

import static jse.math.vector.AbstractVector.*;


/**
 * 单精度浮点数向量的一般实现
 * @author liqa
 */
public class FloatVector extends FloatArrayVector {
    /** 提供默认的创建 */
    public static FloatVector ones(int aSize) {
        float[] tData = new float[aSize];
        Arrays.fill(tData, 1);
        return new FloatVector(tData);
    }
    public static FloatVector zeros(int aSize) {return new FloatVector(new float[aSize]);}
    
    /** 提供 builder 方式的构建 */
    public static Builder builder() {return new Builder();}
    public static Builder builder(int aInitSize) {return new Builder(aInitSize);}
    public final static class Builder extends FloatList {
        private final static int DEFAULT_INIT_SIZE = 8;
        private Builder() {super(DEFAULT_INIT_SIZE);}
        private Builder(int aInitSize) {super(aInitSize);}

        public FloatVector build() {
            float[] tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new FloatVector(mSize, tData);
        }
        
        /** Groovy stuffs */
        @Override public Builder append(float aValue) {return (Builder)super.append(aValue);}
        @Override public Builder appendAll(IFloatVector aVector) {return (Builder)super.appendAll(aVector);}
        @Override @VisibleForTesting public Builder leftShift(float aValue) {return (Builder)super.leftShift(aValue);}
        @Override @VisibleForTesting public Builder leftShift(IFloatVector aVector) {return (Builder)super.leftShift(aVector);}
    }
    
    private int mSize;
    public FloatVector(int aSize, float[] aData) {super(aData); mSize = aSize;}
    public FloatVector(float[] aData) {this(aData.length, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    
    /** IIntegerVector stuffs */
    @Override public final float get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx];}
    @Override public final void set(int aIdx, float aValue) {rangeCheck(aIdx, mSize); mData[aIdx] = aValue;}
    @Override public final float getAndSet(int aIdx, float aValue) {
        rangeCheck(aIdx, mSize);
        float oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected final FloatVector newZeros_(int aSize) {return FloatVector.zeros(aSize);}
    @Override public final FloatVector copy() {return (FloatVector)super.copy();}
    
    @Override public final float @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof FloatVector) return ((FloatVector)aObj).mData;
        if (aObj instanceof ShiftFloatVector) return ((ShiftFloatVector)aObj).mData;
        if (aObj instanceof FloatList) return ((FloatList)aObj).internalData();
        if (aObj instanceof float[]) return (float[])aObj;
        return null;
    }
    @Override public final NDArray<float[]> numpy() {return new NDArray<>(mData, mSize);}
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftFloatVector} */
    @Override public final FloatArrayVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return aFromIdx==0 ? new FloatVector(aToIdx, mData) : new ShiftFloatVector(aToIdx-aFromIdx, aFromIdx, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        float tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public final void update(int aIdx, IFloatUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aOpt.applyAsFloat(mData[aIdx]);
    }
    @Override public final float getAndUpdate(int aIdx, IFloatUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        float tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsFloat(tValue);
        return tValue;
    }
    @Override public final boolean isEmpty() {return mSize==0;}
    @Override public final float last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty FloatVector");
        return mData[mSize-1];
    }
    @Override public final float first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty FloatVector");
        return mData[0];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式）*/
    @Override public final IFloatIterator iterator() {
        return new IFloatIterator() {
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
    @Override public final IFloatSetIterator setIterator() {
        return new IFloatSetIterator() {
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
