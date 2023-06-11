package com.jtool.math.vector;

import com.jtool.code.ISetIterator;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
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
            @Override public ShiftVector refReverse() {
                return new ShiftVector(mSize, mShift, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void increment_(int aIdx) {++mData[totShift-aIdx];}
    @Override public double getAndIncrement_(int aIdx) {return mData[totShift-aIdx]++;}
    @Override public double incrementAndGet_(int aIdx) {return ++mData[totShift-aIdx];}
    @Override public void decrement_(int aIdx) {--mData[totShift-aIdx];}
    @Override public double getAndDecrement_(int aIdx) {return mData[totShift-aIdx]--;}
    @Override public double decrementAndGet_(int aIdx) {return --mData[totShift-aIdx];}
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public Iterator<Double> iterator() {
        return new Iterator<Double>() {
            private int mIdx = totShift;
            @Override public boolean hasNext() {return mIdx >= mShift;}
            @Override public Double next() {
                if (hasNext()) {
                    Double tNext = mData[mIdx];
                    --mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ISetIterator<Double> setIterator() {
        return new ISetIterator<Double>() {
            private int mIdx = totShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx >= mShift;}
            @Override public void set(Double e) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = e;
            }
            @Override public Double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                    mData[oIdx] = e;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                    double oValue = mData[oIdx];
                    mData[oIdx] = e;
                    return oValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
