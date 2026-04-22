package jse.math.vector;

import jep.NDArray;
import jse.code.collection.IntList;
import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import static jse.math.vector.AbstractVector.*;


/**
 * 整数向量的一般实现
 * <p>
 * 现在合并了 ShiftIntVector 的功能，从而同时支持从数组任意位置开始操作
 * @author liqa
 */
public class IntVector extends IntArrayVector {
    /** 提供默认的创建 */
    public static IntVector ones(int aSize) {
        int[] tData = new int[aSize];
        Arrays.fill(tData, 1);
        return new IntVector(tData);
    }
    public static IntVector zeros(int aSize) {return new IntVector(new int[aSize]);}
    
    /** 提供 builder 方式的构建 */
    public static Builder builder() {return new Builder();}
    public static Builder builder(int aInitSize) {return new Builder(aInitSize);}
    public final static class Builder extends IntList {
        private final static int DEFAULT_INIT_SIZE = 8;
        private Builder() {super(DEFAULT_INIT_SIZE);}
        private Builder(int aInitSize) {super(aInitSize);}
        
        public IntVector build() {
            int[] tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new IntVector(mSize, tData);
        }
        
        /** Groovy stuffs */
        @Override public Builder append(int aValue) {return (Builder)super.append(aValue);}
        @Override public Builder appendAll(IIntVector aVector) {return (Builder)super.appendAll(aVector);}
        @Override @VisibleForTesting public Builder leftShift(int aValue) {return (Builder)super.leftShift(aValue);}
        @Override @VisibleForTesting public Builder leftShift(IIntVector aVector) {return (Builder)super.leftShift(aVector);}
    }
    
    private int mSize;
    private int mShift = 0;
    public IntVector(int aSize, int aShift, int[] aData) {
        super(aData);
        mSize = aSize;
        mShift = aShift;
    }
    public IntVector(int aSize, int[] aData) {
        super(aData);
        mSize = aSize;
    }
    public IntVector(int[] aData) {
        this(aData.length, aData);
    }
    
    /** 提供额外的接口来直接设置底层参数 */
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    @Override public void setInternalDataShift(int aShift) {mShift = aShift;}
    
    /** IIntVector stuffs */
    @Override public final int get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx+mShift];
    }
    @Override public final void set(int aIdx, int aValue) {
        rangeCheck(aIdx, mSize);
        mData[aIdx+mShift] = aValue;
    }
    @Override public final int getAndSet(int aIdx, int aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        int oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public final int size() {return mSize;}
    
    @Override protected final IntVector newZeros_(int aSize) {return IntVector.zeros(aSize);}
    @Override public final IntVector copy() {return (IntVector)super.copy();}
    
    @Override public final int @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof IntVector) return ((IntVector)aObj).mData;
        if (aObj instanceof IntList) return ((IntList)aObj).internalData();
        if (aObj instanceof int[]) return (int[])aObj;
        return null;
    }
    @Override public final NDArray<int[]> numpy() {
        return mShift==0 ? new NDArray<>(mData, mSize) : super.numpy();
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return mShift;}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link IntVector} */
    @Override public final IntVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new IntVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        aIdx1 += mShift;
        aIdx2 += mShift;
        int tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public final void increment(int aIdx) {
        rangeCheck(aIdx, mSize);
        ++mData[aIdx+mShift];
    }
    @Override public final int getAndIncrement(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx+mShift]++;
    }
    @Override public final void decrement(int aIdx) {
        rangeCheck(aIdx, mSize);
        --mData[aIdx+mShift];
    }
    @Override public final int getAndDecrement(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx+mShift]--;
    }
    
    @Override public final void add(int aIdx, int aDelta) {
        rangeCheck(aIdx, mSize);
        mData[aIdx+mShift] += aDelta;
    }
    @Override public final int getAndAdd(int aIdx, int aDelta) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        int tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public final void update(int aIdx, IntUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[aIdx] = aOpt.applyAsInt(mData[aIdx]);
    }
    @Override public final int getAndUpdate(int aIdx, IntUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        int tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsInt(tValue);
        return tValue;
    }
    @Override public final boolean isEmpty() {return mSize==0;}
    @Override public final int last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty IntVector");
        return mData[mSize-1+mShift];
    }
    @Override public final int first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty IntVector");
        return mData[mShift];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式）*/
    @Override public final IIntIterator iterator() {
        return new IIntIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntSetIterator setIterator() {
        return new IIntSetIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
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
