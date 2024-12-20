package jse.math.vector;

import jse.code.collection.LongList;
import jse.code.iterator.ILongIterator;
import jse.code.iterator.ILongSetIterator;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.LongUnaryOperator;

import static jse.math.vector.AbstractVector.*;


/**
 * @author liqa
 * <p> 整数向量的一般实现 </p>
 */
public final class LongVector extends LongArrayVector {
    /** 提供默认的创建 */
    public static LongVector ones(int aSize) {
        long[] tData = new long[aSize];
        Arrays.fill(tData, 1);
        return new LongVector(tData);
    }
    public static LongVector zeros(int aSize) {return new LongVector(new long[aSize]);}
    
    /** 提供 builder 方式的构建 */
    public static Builder builder() {return new Builder();}
    public static Builder builder(int aInitSize) {return new Builder(aInitSize);}
    public final static class Builder extends LongList {
        private final static int DEFAULT_INIT_SIZE = 8;
        private Builder() {super(DEFAULT_INIT_SIZE);}
        private Builder(int aInitSize) {super(aInitSize);}

        public LongVector build() {
            long[] tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new LongVector(mSize, tData);
        }
        
        /** Groovy stuffs */
        public Builder append(long aValue) {return (Builder)super.append(aValue);}
        public Builder appendAll(ILongVector aVector) {return (Builder)super.appendAll(aVector);}
        @VisibleForTesting public Builder leftShift(long aValue) {return (Builder)super.leftShift(aValue);}
        @VisibleForTesting public Builder leftShift(ILongVector aVector) {return (Builder)super.leftShift(aVector);}
    }
    
    private int mSize;
    public LongVector(int aSize, long[] aData) {super(aData); mSize = aSize;}
    public LongVector(long[] aData) {this(aData.length, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public LongVector setSize(int aSize) {mSize = aSize; return this;}
    public int dataLength() {return mData.length;}
    
    /** IIntegerVector stuffs */
    @Override public long get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx];}
    @Override public void set(int aIdx, long aValue) {rangeCheck(aIdx, mSize); mData[aIdx] = aValue;}
    @Override public long getAndSet(int aIdx, long aValue) {
        rangeCheck(aIdx, mSize);
        long oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected LongVector newZeros_(int aSize) {return LongVector.zeros(aSize);}
    @Override public LongVector copy() {return (LongVector)super.copy();}
    
    @Override public LongVector newShell() {return new LongVector(mSize, null);}
    @Override public long @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof LongVector) return ((LongVector)aObj).mData;
        if (aObj instanceof ShiftLongVector) return ((ShiftLongVector)aObj).mData;
        if (aObj instanceof LongList) return ((LongList)aObj).internalData();
        if (aObj instanceof long[]) return (long[])aObj;
        return null;
    }
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftLongVector} */
    @Override public LongArrayVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return aFromIdx==0 ? new LongVector(aToIdx, mData) : new ShiftLongVector(aToIdx-aFromIdx, aFromIdx, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        long tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void increment(int aIdx) {rangeCheck(aIdx, mSize); ++mData[aIdx];}
    @Override public long getAndIncrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx]++;}
    @Override public void decrement(int aIdx) {rangeCheck(aIdx, mSize); --mData[aIdx];}
    @Override public long getAndDecrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx]--;}
    
    @Override public void add(int aIdx, long aDelta) {rangeCheck(aIdx, mSize); mData[aIdx] += aDelta;}
    @Override public long getAndAdd(int aIdx, long aDelta) {
        rangeCheck(aIdx, mSize);
        long tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public void update(int aIdx, LongUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aOpt.applyAsLong(mData[aIdx]);
    }
    @Override public long getAndUpdate(int aIdx, LongUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        long tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsLong(tValue);
        return tValue;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public long last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty LongVector");
        return mData[mSize-1];
    }
    @Override public long first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty LongVector");
        return mData[0];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式）*/
    @Override public ILongIterator iterator() {
        return new ILongIterator() {
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public long next() {
                if (hasNext()) {
                    long tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ILongSetIterator setIterator() {
        return new ILongSetIterator() {
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(long aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public long next() {
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
            @Override public void nextAndSet(long aValue) {
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
