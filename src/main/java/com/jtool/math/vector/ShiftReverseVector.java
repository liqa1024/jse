package com.jtool.math.vector;

import com.jtool.code.functional.IDoubleConsumer1;
import com.jtool.code.functional.IDoubleSupplier;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.functional.IDoubleOperator1;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 反向检索数组的向量，注意有经过 shift，因此检索的数组区间是 shift ~ size()+shift-1 </p>
 * <p> 由于是反向检索的，由于优化内部实际许多操作会是反向进行的 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link ReverseVector} </p>
 */
public final class ShiftReverseVector extends DoubleArrayVector {
    private final int mSize;
    private final int mShift;
    private final int totShift;
    public ShiftReverseVector(int aSize, int aShift, double[] aData) {super(aData); mSize = aSize; mShift = aShift; totShift = mSize-1+mShift;}
    public ShiftReverseVector(int aShift, double[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** IVector stuffs */
    @Override public double get_(int aIdx) {return mData[totShift-aIdx];}
    @Override public void set_(int aIdx, double aValue) {mData[totShift-aIdx] = aValue;}
    @Override public double getAndSet_(int aIdx, double aValue) {
        aIdx = totShift-aIdx;
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override public ReverseVector newZeros(int aSize) {return ReverseVector.zeros(aSize);}
    
    @Override public ShiftReverseVector newShell() {return new ShiftReverseVector(mSize, mShift, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ReverseVector) return ((ReverseVector)aObj).mData;
        if (aObj instanceof ShiftReverseVector) return ((ShiftReverseVector)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，引用反转直接返回 {@link ShiftVector} */
    @Override public IVectorOperation operation() {
        return new DoubleArrayVectorOperation_() {
            @Override public void fill(IVectorGetter aRHS) {
                for (int i = totShift; i >= mShift; --i) mData[i] = aRHS.get(i);
            }
            @Override public void assign(IDoubleSupplier aSup) {
                for (int i = totShift; i >= mShift; --i) mData[i] = aSup.get();
            }
            @Override public void forEach(IDoubleConsumer1 aCon) {
                for (int i = totShift; i >= mShift; --i) aCon.run(mData[i]);
            }
            @Override public ShiftVector refReverse() {
                return new ShiftVector(mSize, mShift, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void increment_(int aIdx) {++mData[totShift-aIdx];}
    @Override public double getAndIncrement_(int aIdx) {return mData[totShift-aIdx]++;}
    @Override public void decrement_(int aIdx) {--mData[totShift-aIdx];}
    @Override public double getAndDecrement_(int aIdx) {return mData[totShift-aIdx]--;}
    
    @Override public void add_(int aIdx, double aDelta) {mData[totShift-aIdx] += aDelta;}
    @Override public double getAndAdd_(int aIdx, double aDelta) {
        aIdx = totShift-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public void update_(int aIdx, IDoubleOperator1 aOpt) {
        aIdx = totShift-aIdx;
        mData[aIdx] = aOpt.cal(mData[aIdx]);
    }
    @Override public double getAndUpdate_(int aIdx, IDoubleOperator1 aOpt) {
        aIdx = totShift-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.cal(tValue);
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
