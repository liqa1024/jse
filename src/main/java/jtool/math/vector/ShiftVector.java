package jtool.math.vector;

import jtool.code.collection.DoubleList;
import jtool.code.iterator.IDoubleIterator;
import jtool.code.iterator.IDoubleSetIterator;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.*;

/**
 * @author liqa
 * <p> 支持将内部的 double[] 进行平移访问的 Vector，理论拥有和 {@link Vector} 几乎一样的性能 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link Vector} </p>
 */
public final class ShiftVector extends DoubleArrayVector {
    private int mSize;
    private int mShift;
    public ShiftVector(int aSize, int aShift, double[] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftVector(int aShift, double[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public int shift() {return mShift;}
    public ShiftVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length-mShift, aSize); return this;}
    public ShiftVector setShift(int aShift) {mShift = MathEX.Code.toRange(0, mData.length-mSize, aShift); return this;}
    public int dataLength() {return mData.length;}
    
    /** IVector stuffs */
    @Override public double get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx + mShift];}
    @Override public void set(int aIdx, double aValue) {rangeCheck(aIdx, mSize); mData[aIdx + mShift] = aValue;}
    @Override public double getAndSet(int aIdx, double aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected Vector newZeros_(int aSize) {return Vector.zeros(aSize);}
    @Override public Vector copy() {return (Vector)super.copy();}
    
    @Override public ShiftVector newShell() {return new ShiftVector(mSize, mShift, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof Vector) return ((Vector)aObj).mData;
        if (aObj instanceof ShiftVector) return ((ShiftVector)aObj).mData;
        if (aObj instanceof DoubleList) return ((DoubleList)aObj).internalData();
        if (aObj instanceof double[]) return (double[])aObj;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return shift();}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftVector} */
    @Override public ShiftVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，引用反转直接返回 {@link ShiftReverseVector} */
    @Override public IVectorOperation operation() {
        return new DoubleArrayVectorOperation_() {
            @Override public ShiftReverseVector refReverse() {
                return new ShiftReverseVector(mSize, mShift, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        aIdx1 += mShift;
        aIdx2 += mShift;
        double tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void increment(int aIdx) {rangeCheck(aIdx, mSize); ++mData[aIdx + mShift];}
    @Override public double getAndIncrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx + mShift]++;}
    @Override public void decrement(int aIdx) {rangeCheck(aIdx, mSize); --mData[aIdx + mShift];}
    @Override public double getAndDecrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx + mShift]--;}
    
    @Override public void add(int aIdx, double aDelta) {rangeCheck(aIdx, mSize); mData[aIdx + mShift] += aDelta;}
    @Override public double getAndAdd(int aIdx, double aDelta) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public void update(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[aIdx] = aOpt.applyAsDouble(mData[aIdx]);
    }
    @Override public double getAndUpdate(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public double last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty Vector");
        return mData[mSize-1+mShift];
    }
    @Override public double first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty Vector");
        return mData[mShift];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IDoubleIterator iterator() {
        return new IDoubleIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIterator() {
        return new IDoubleSetIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
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
            @Override public void nextAndSet(double aValue) {
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
