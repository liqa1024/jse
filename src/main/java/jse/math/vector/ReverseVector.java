package jse.math.vector;

import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetIterator;
import jse.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.*;

/**
 * @author liqa
 * <p> 反向检索数组的向量，注意没有经过 shift，因此检索的数组区间依旧是 0 ~ size()-1 </p>
 * <p> 由于是反向检索的，由于优化内部实际许多操作会是反向进行的 </p>
 */
public final class ReverseVector extends DoubleArrayVector {
    /** 提供默认的创建 */
    public static ReverseVector ones(int aSize) {
        double[] tData = new double[aSize];
        Arrays.fill(tData, 1.0);
        return new ReverseVector(tData);
    }
    public static ReverseVector zeros(int aSize) {return new ReverseVector(new double[aSize]);}
    
    
    private int mSize;
    private int mSizeMM;
    public ReverseVector(int aSize, double[] aData) {super(aData); mSize = aSize; mSizeMM = mSize-1;}
    public ReverseVector(double[] aData) {this(aData.length, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public ReverseVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length, aSize); mSizeMM = mSize-1; return this;}
    public int dataLength() {return mData.length;}
    
    /** IVector stuffs */
    @Override public double get(int aIdx) {rangeCheck(aIdx, mSize); return mData[mSizeMM-aIdx];}
    @Override public void set(int aIdx, double aValue) {rangeCheck(aIdx, mSize); mData[mSizeMM-aIdx] = aValue;}
    @Override public double getAndSet(int aIdx, double aValue) {
        rangeCheck(aIdx, mSize);
        aIdx = mSizeMM-aIdx;
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected ReverseVector newZeros_(int aSize) {return ReverseVector.zeros(aSize);}
    @Override public ReverseVector copy() {return (ReverseVector)super.copy();}
    
    @Override public ReverseVector newShell() {return new ReverseVector(mSize, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ReverseVector) return ((ReverseVector)aObj).mData;
        if (aObj instanceof ShiftReverseVector) return ((ShiftReverseVector)aObj).mData;
        return null;
    }
    @Override public boolean isReverse() {return true;}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftReverseVector} */
    @Override public ShiftReverseVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftReverseVector(aToIdx-aFromIdx, mSize-aToIdx, mData);
    }
    
    /** Optimize stuffs，引用反转直接返回 {@link Vector} */
    @Override public IVectorOperation operation() {
        return new DoubleArrayVectorOperation_() {
            @Override public Vector refReverse() {
                return new Vector(mSize, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        aIdx1 = mSizeMM-aIdx1;
        aIdx2 = mSizeMM-aIdx2;
        double tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void increment(int aIdx) {rangeCheck(aIdx, mSize); ++mData[mSizeMM-aIdx];}
    @Override public double getAndIncrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[mSizeMM-aIdx]++;}
    @Override public void decrement(int aIdx) {rangeCheck(aIdx, mSize); --mData[mSizeMM-aIdx];}
    @Override public double getAndDecrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[mSizeMM-aIdx]--;}
    
    @Override public void add(int aIdx, double aDelta) {rangeCheck(aIdx, mSize); mData[mSizeMM-aIdx] += aDelta;}
    @Override public double getAndAdd(int aIdx, double aDelta) {
        rangeCheck(aIdx, mSize);
        aIdx = mSizeMM-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public void update(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx = mSizeMM-aIdx;
        mData[aIdx] = aOpt.applyAsDouble(mData[aIdx]);
    }
    @Override public double getAndUpdate(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx = mSizeMM-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IDoubleIterator iterator() {
        return new IDoubleIterator() {
            private int mIdx = mSizeMM;
            @Override public boolean hasNext() {return mIdx >= 0;}
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
            private int mIdx = mSizeMM, oIdx = -1;
            @Override public boolean hasNext() {return mIdx >= 0;}
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
