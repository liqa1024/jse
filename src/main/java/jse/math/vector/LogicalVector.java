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
 * @author liqa
 * <p> 逻辑向量的一般实现 </p>
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
        public Builder append(boolean aValue) {return (Builder)super.append(aValue);}
        public Builder appendAll(ILogicalVector aVector) {return (Builder)super.appendAll(aVector);}
        @VisibleForTesting public Builder leftShift(boolean aValue) {return (Builder)super.leftShift(aValue);}
        @VisibleForTesting public Builder leftShift(ILogicalVector aVector) {return (Builder)super.leftShift(aVector);}
    }
    
    private int mSize;
    public LogicalVector(int aSize, boolean[] aData) {super(aData); mSize = aSize;}
    public LogicalVector(boolean[] aData) {this(aData.length, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    
    /** ILogicalVector stuffs */
    @Override public final boolean get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx];}
    @Override public final void set(int aIdx, boolean aValue) {rangeCheck(aIdx, mSize); mData[aIdx] = aValue;}
    @Override public final boolean getAndSet(int aIdx, boolean aValue) {
        rangeCheck(aIdx, mSize);
        boolean oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public final int size() {return mSize;}
    
    @Override protected final LogicalVector newZeros_(int aSize) {return LogicalVector.zeros(aSize);}
    @Override public final LogicalVector copy() {
        LogicalVector rVector = LogicalVector.zeros(mSize);
        rVector.fill(this);
        return rVector;
    }
    
    @Override public final boolean @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof LogicalVector) return ((LogicalVector)aObj).mData;
        if (aObj instanceof ShiftLogicalVector) return ((ShiftLogicalVector)aObj).mData;
        if (aObj instanceof BooleanList) return ((BooleanList)aObj).internalData();
        if (aObj instanceof boolean[]) return (boolean[])aObj;
        return null;
    }
    @Override public final NDArray<boolean[]> numpy() {return new NDArray<>(mData, mSize);}
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftLogicalVector} */
    @Override public final BooleanArrayVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return aFromIdx==0 ? new LogicalVector(aToIdx, mData) : new ShiftLogicalVector(aToIdx-aFromIdx, aFromIdx, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        boolean tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public final void flip(int aIdx) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = !mData[aIdx];
    }
    @Override public final boolean getAndFlip(int aIdx) {
        rangeCheck(aIdx, mSize);
        boolean tValue = mData[aIdx];
        mData[aIdx] = !tValue;
        return tValue;
    }
    @Override public final void update(int aIdx, IBooleanUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aOpt.applyAsBoolean(mData[aIdx]);
    }
    @Override public final boolean getAndUpdate(int aIdx, IBooleanUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        boolean tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsBoolean(tValue);
        return tValue;
    }
    @Override public final boolean isEmpty() {return mSize==0;}
    @Override public final boolean last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty LogicalVector");
        return mData[mSize-1];
    }
    @Override public final boolean first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty LogicalVector");
        return mData[0];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public final IBooleanIterator iterator() {
        return new IBooleanIterator() {
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
