package jse.math.vector;

import jse.code.collection.BooleanList;
import jse.code.functional.IBooleanUnaryOperator;
import jse.code.iterator.IBooleanIterator;
import jse.code.iterator.IBooleanSetIterator;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

import static jse.math.vector.AbstractVector.*;

/**
 * @author liqa
 * <p> 支持将内部的 boolean[] 进行平移访问的 LogicalVector，理论拥有和 {@link LogicalVector} 几乎一样的性能 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link LogicalVector} </p>
 */
public final class ShiftLogicalVector extends BooleanArrayVector {
    private int mSize;
    private int mShift;
    public ShiftLogicalVector(int aSize, int aShift, boolean[] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftLogicalVector(int aShift, boolean[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    @Override public void setInternalDataShift(int aShift) {mShift = aShift;}
    
    /** ILogicalVector stuffs */
    @Override public boolean get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx + mShift];}
    @Override public void set(int aIdx, boolean aValue) {rangeCheck(aIdx, mSize); mData[aIdx + mShift] = aValue;}
    @Override public boolean getAndSet(int aIdx, boolean aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        boolean oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected LogicalVector newZeros_(int aSize) {return LogicalVector.zeros(aSize);}
    @Override public LogicalVector copy() {
        LogicalVector rVector = LogicalVector.zeros(mSize);
        rVector.fill(this);
        return rVector;
    }
    
    @Override public boolean @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof LogicalVector) return ((LogicalVector)aObj).mData;
        if (aObj instanceof ShiftLogicalVector) return ((ShiftLogicalVector)aObj).mData;
        if (aObj instanceof BooleanList) return ((BooleanList)aObj).internalData();
        if (aObj instanceof boolean[]) return (boolean[])aObj;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return mShift;}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftLogicalVector} */
    @Override public ShiftLogicalVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftLogicalVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        aIdx1 += mShift;
        aIdx2 += mShift;
        boolean tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void flip(int aIdx) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[aIdx] = !mData[aIdx];
    }
    @Override public boolean getAndFlip(int aIdx) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        boolean tValue = mData[aIdx];
        mData[aIdx] = !tValue;
        return tValue;
    }
    @Override public void update(int aIdx, IBooleanUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[aIdx] = aOpt.applyAsBoolean(mData[aIdx]);
    }
    @Override public boolean getAndUpdate(int aIdx, IBooleanUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        boolean tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsBoolean(tValue);
        return tValue;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public boolean last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty Vector");
        return mData[mSize-1+mShift];
    }
    @Override public boolean first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty Vector");
        return mData[mShift];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IBooleanIterator iterator() {
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
    @Override public IBooleanSetIterator setIterator() {
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
