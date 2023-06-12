package com.jtool.math.vector;

import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.operator.IDoubleOperator1;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;

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
    
    
    private final int mSize;
    private final int mSizeMM;
    public ReverseVector(int aSize, double[] aData) {super(aData); mSize = aSize; mSizeMM = mSize-1;}
    public ReverseVector(double[] aData) {this(aData.length, aData);}
    
    /** IVector stuffs */
    @Override public double get_(int aIdx) {return mData[mSizeMM-aIdx];}
    @Override public void set_(int aIdx, double aValue) {mData[mSizeMM-aIdx] = aValue;}
    @Override public double getAndSet_(int aIdx, double aValue) {
        aIdx = mSizeMM-aIdx;
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override public ReverseVector newZeros(int aSize) {return ReverseVector.zeros(aSize);}
    
    @Override public ReverseVector newShell() {return new ReverseVector(mSize, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ReverseVector) return ((ReverseVector)aObj).mData;
        if (aObj instanceof ShiftReverseVector) return ((ShiftReverseVector)aObj).mData;
        return null;
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
    @Override public void increment_(int aIdx) {++mData[mSizeMM-aIdx];}
    @Override public double getAndIncrement_(int aIdx) {return mData[mSizeMM-aIdx]++;}
    @Override public double incrementAndGet_(int aIdx) {return ++mData[mSizeMM-aIdx];}
    @Override public void decrement_(int aIdx) {--mData[mSizeMM-aIdx];}
    @Override public double getAndDecrement_(int aIdx) {return mData[mSizeMM-aIdx]--;}
    @Override public double decrementAndGet_(int aIdx) {return --mData[mSizeMM-aIdx];}
    
    @Override public void add_(int aIdx, double aDelta) {mData[mSizeMM-aIdx] += aDelta;}
    @Override public double getAndAdd_(int aIdx, double aDelta) {
        aIdx = mSizeMM-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public double addAndGet_(int aIdx, double aDelta) {
        aIdx = mSizeMM-aIdx;
        double tValue = mData[aIdx];
        tValue += aDelta;
        mData[aIdx] = tValue;
        return tValue;
    }
    @Override public void update_(int aIdx, IDoubleOperator1 aOpt) {
        aIdx = mSizeMM-aIdx;
        mData[aIdx] = aOpt.cal(mData[aIdx]);
    }
    @Override public double getAndUpdate_(int aIdx, IDoubleOperator1 aOpt) {
        aIdx = mSizeMM-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.cal(tValue);
        return tValue;
    }
    @Override public double updateAndGet_(int aIdx, IDoubleOperator1 aOpt) {
        aIdx = mSizeMM-aIdx;
        double tValue = mData[aIdx];
        tValue = aOpt.cal(tValue);
        mData[aIdx] = tValue;
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
            @Override public double getNextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                    double oValue = mData[oIdx];
                    mData[oIdx] = aValue;
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
