package jse.math.vector;

import jse.code.collection.LongList;
import jse.code.iterator.ILongIterator;
import jse.code.iterator.ILongSetIterator;
import jse.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.LongUnaryOperator;

import static jse.math.vector.AbstractVector.*;

/**
 * @author liqa
 * <p> 支持将内部的 long[] 进行平移访问的 LongVector，理论拥有和 {@link LongVector} 几乎一样的性能 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link LongVector} </p>
 */
public final class ShiftLongVector extends LongArrayVector {
    private int mSize;
    private int mShift;
    public ShiftLongVector(int aSize, int aShift, long[] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftLongVector(int aShift, long[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public int shift() {return mShift;}
    public ShiftLongVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length-mShift, aSize); return this;}
    public ShiftLongVector setShift(int aShift) {mShift = MathEX.Code.toRange(0, mData.length-mSize, aShift); return this;}
    public int dataLength() {return mData.length;}
    
    /** ILogicalVector stuffs */
    @Override public long get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx + mShift];}
    @Override public void set(int aIdx, long aValue) {rangeCheck(aIdx, mSize); mData[aIdx + mShift] = aValue;}
    @Override public long getAndSet(int aIdx, long aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        long oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected LongVector newZeros_(int aSize) {return LongVector.zeros(aSize);}
    @Override public LongVector copy() {return (LongVector)super.copy();}
    
    @Override public ShiftLongVector newShell() {return new ShiftLongVector(mSize, mShift, null);}
    @Override public long @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof LongVector) return ((LongVector)aObj).mData;
        if (aObj instanceof ShiftLongVector) return ((ShiftLongVector)aObj).mData;
        if (aObj instanceof LongList) return ((LongList)aObj).internalData();
        if (aObj instanceof long[]) return (long[])aObj;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return shift();}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftLongVector} */
    @Override public ShiftLongVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftLongVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        aIdx1 += mShift;
        aIdx2 += mShift;
        long tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void increment(int aIdx) {rangeCheck(aIdx, mSize); ++mData[aIdx + mShift];}
    @Override public long getAndIncrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx + mShift]++;}
    @Override public void decrement(int aIdx) {rangeCheck(aIdx, mSize); --mData[aIdx + mShift];}
    @Override public long getAndDecrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx + mShift]--;}
    
    @Override public void add(int aIdx, long aDelta) {rangeCheck(aIdx, mSize); mData[aIdx + mShift] += aDelta;}
    @Override public long getAndAdd(int aIdx, long aDelta) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        long tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public void update(int aIdx, LongUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[aIdx] = aOpt.applyAsLong(mData[aIdx]);
    }
    @Override public long getAndUpdate(int aIdx, LongUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        long tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsLong(tValue);
        return tValue;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public long last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty LongVector");
        return mData[mSize-1+mShift];
    }
    @Override public long first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty LongVector");
        return mData[mShift];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public ILongIterator iterator() {
        return new ILongIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
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
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
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
