package jse.math.vector;

import jep.NDArray;
import jse.code.collection.BooleanList;
import jse.code.functional.IBooleanUnaryOperator;
import jse.code.iterator.IBooleanIterator;
import jse.code.iterator.IBooleanSetIterator;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;

import static jse.math.vector.AbstractVector.*;

/**
 * 逻辑向量的一般实现
 * <p>
 * 现在合并了 ShiftLogicalVector 的功能，从而同时支持从数组任意位置开始操作
 * @author liqa
 */
public class LogicalVector extends BooleanArrayVector {
    /** 提供默认的创建 */
    public static LogicalVector ones(int aSize) {
        boolean[] tData = new boolean[aSize];
        Arrays.fill(tData, true);
        return new LogicalVector(tData);
    }
    public static LogicalVector zeros(int aSize) {return new LogicalVector(new boolean[aSize]);}
    
    /** 提供 builder 方式的构建 */
    public static Builder builder() {return new Builder();}
    public static Builder builder(int aInitSize) {return new Builder(aInitSize);}
    public final static class Builder extends BooleanList {
        private final static int DEFAULT_INIT_SIZE = 8;
        private Builder() {super(DEFAULT_INIT_SIZE);}
        private Builder(int aInitSize) {super(aInitSize);}
        
        public LogicalVector build() {
            boolean[] tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new LogicalVector(mSize, tData);
        }
        
        /** Groovy stuffs */
        @Override public Builder append(boolean aValue) {return (Builder)super.append(aValue);}
        @Override public Builder appendAll(ILogicalVector aVector) {return (Builder)super.appendAll(aVector);}
        @Override @VisibleForTesting public Builder leftShift(boolean aValue) {return (Builder)super.leftShift(aValue);}
        @Override @VisibleForTesting public Builder leftShift(ILogicalVector aVector) {return (Builder)super.leftShift(aVector);}
    }
    
    private int mSize;
    private int mShift = 0;
    public LogicalVector(int aSize, int aShift, boolean[] aData) {
        super(aData);
        mSize = aSize;
        mShift = aShift;
    }
    public LogicalVector(int aSize, boolean[] aData) {
        super(aData);
        mSize = aSize;
    }
    public LogicalVector(boolean[] aData) {
        this(aData.length, aData);
    }
    
    /** 提供额外的接口来直接设置底层参数 */
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    @Override public void setInternalDataShift(int aShift) {mShift = aShift;}
    
    /** ILogicalVector stuffs */
    @Override public final boolean get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx+mShift];
    }
    @Override public final void set(int aIdx, boolean aValue) {
        rangeCheck(aIdx, mSize);
        mData[aIdx+mShift] = aValue;
    }
    @Override public final boolean getAndSet(int aIdx, boolean aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        boolean oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public final int size() {
        return mSize;
    }
    
    @Override protected final LogicalVector newZeros_(int aSize) {return LogicalVector.zeros(aSize);}
    @Override public final LogicalVector copy() {
        LogicalVector rVector = LogicalVector.zeros(mSize);
        rVector.fill(this);
        return rVector;
    }
    
    @Override public final boolean @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof LogicalVector) return ((LogicalVector)aObj).mData;
        if (aObj instanceof BooleanList) return ((BooleanList)aObj).internalData();
        if (aObj instanceof boolean[]) return (boolean[])aObj;
        return null;
    }
    @Override public final NDArray<boolean[]> numpy() {
        return mShift==0 ? new NDArray<>(mData, mSize) : super.numpy();
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return mShift;}
    
    /** Optimize stuffs，subVec 切片直接返回  {@link LogicalVector} */
    @Override public final LogicalVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new LogicalVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        aIdx1 += mShift;
        aIdx2 += mShift;
        boolean tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public final void flip(int aIdx) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[aIdx] = !mData[aIdx];
    }
    @Override public final boolean getAndFlip(int aIdx) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        boolean tValue = mData[aIdx];
        mData[aIdx] = !tValue;
        return tValue;
    }
    @Override public final void update(int aIdx, IBooleanUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[aIdx] = aOpt.applyAsBoolean(mData[aIdx]);
    }
    @Override public final boolean getAndUpdate(int aIdx, IBooleanUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        boolean tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsBoolean(tValue);
        return tValue;
    }
    @Override public final boolean isEmpty() {
        return mSize==0;
    }
    @Override public final boolean last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty LogicalVector");
        return mData[mSize-1+mShift];
    }
    @Override public final boolean first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty LogicalVector");
        return mData[mShift];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public final IBooleanIterator iterator() {
        return new IBooleanIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IBooleanSetIterator setIterator() {
        return new IBooleanSetIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
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
