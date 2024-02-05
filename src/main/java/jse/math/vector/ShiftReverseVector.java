package jse.math.vector;

import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetIterator;
import jse.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.*;

/**
 * @author liqa
 * <p> 反向检索数组的向量，注意有经过 shift，因此检索的数组区间是 shift ~ size()+shift-1 </p>
 * <p> 由于是反向检索的，由于优化内部实际许多操作会是反向进行的 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link ReverseVector} </p>
 */
public final class ShiftReverseVector extends DoubleArrayVector {
    private int mSize;
    private int mShift;
    private int totShift;
    public ShiftReverseVector(int aSize, int aShift, double[] aData) {super(aData); mSize = aSize; mShift = aShift; totShift = mSize-1+mShift;}
    public ShiftReverseVector(int aShift, double[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public int shift() {return mShift;}
    public ShiftReverseVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length-mShift, aSize); totShift = mSize-1+mShift; return this;}
    public ShiftReverseVector setShift(int aShift) {mShift = MathEX.Code.toRange(0, mData.length-mSize, aShift); totShift = mSize-1+mShift; return this;}
    public int dataLength() {return mData.length;}
    
    /** IVector stuffs */
    @Override public double get(int aIdx) {rangeCheck(aIdx, mSize); return mData[totShift-aIdx];}
    @Override public void set(int aIdx, double aValue) {rangeCheck(aIdx, mSize); mData[totShift-aIdx] = aValue;}
    @Override public double getAndSet(int aIdx, double aValue) {
        rangeCheck(aIdx, mSize);
        aIdx = totShift-aIdx;
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected ReverseVector newZeros_(int aSize) {return ReverseVector.zeros(aSize);}
    @Override public ReverseVector copy() {return (ReverseVector)super.copy();}
    
    @Override public ShiftReverseVector newShell() {return new ShiftReverseVector(mSize, mShift, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ReverseVector) return ((ReverseVector)aObj).mData;
        if (aObj instanceof ShiftReverseVector) return ((ShiftReverseVector)aObj).mData;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return shift();}
    @Override public boolean isReverse() {return true;}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftReverseVector} */
    @Override public ShiftReverseVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftReverseVector(aToIdx-aFromIdx, mSize-aToIdx+mShift, mData);
    }
    
    /** Optimize stuffs，引用反转直接返回 {@link ShiftVector} */
    @Override public IVectorOperation operation() {
        return new DoubleArrayVectorOperation_() {
            @Override public ShiftVector refReverse() {
                return new ShiftVector(mSize, mShift, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        aIdx1 = totShift-aIdx1;
        aIdx2 = totShift-aIdx2;
        double tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void increment(int aIdx) {rangeCheck(aIdx, mSize); ++mData[totShift-aIdx];}
    @Override public double getAndIncrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[totShift-aIdx]++;}
    @Override public void decrement(int aIdx) {rangeCheck(aIdx, mSize); --mData[totShift-aIdx];}
    @Override public double getAndDecrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[totShift-aIdx]--;}
    
    @Override public void add(int aIdx, double aDelta) {rangeCheck(aIdx, mSize); mData[totShift-aIdx] += aDelta;}
    @Override public double getAndAdd(int aIdx, double aDelta) {
        rangeCheck(aIdx, mSize);
        aIdx = totShift-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public void update(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx = totShift-aIdx;
        mData[aIdx] = aOpt.applyAsDouble(mData[aIdx]);
    }
    @Override public double getAndUpdate(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx = totShift-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IDoubleIterator iterator() {
        return new IDoubleIterator() {
            private int mIdx = totShift;
            @Override public boolean hasNext() {return mIdx >= mShift;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    --mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIterator() {
        return new IDoubleSetIterator() {
            private int mIdx = totShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx >= mShift;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
