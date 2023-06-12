package com.jtool.math.vector;

import com.jtool.code.ISetIterator;
import com.jtool.code.operator.IOperator1;
import org.jetbrains.annotations.Nullable;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 支持将内部的 double[] 进行平移访问的 Vector，理论拥有和 {@link Vector} 几乎一样的性能 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link Vector} </p>
 */
public final class ShiftVector extends DoubleArrayVector {
    private final int mSize;
    private final int mShift;
    public ShiftVector(int aSize, int aShift, double[] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftVector(int aShift, double[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** IVector stuffs */
    @Override public double get_(int aIdx) {return mData[aIdx + mShift];}
    @Override public void set_(int aIdx, double aValue) {mData[aIdx + mShift] = aValue;}
    @Override public double getAndSet_(int aIdx, double aValue) {
        aIdx += mShift;
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override public Vector newZeros(int aSize) {return Vector.zeros(aSize);}
    
    @Override public ShiftVector newShell() {return new ShiftVector(mSize, mShift, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof Vector) return ((Vector)aObj).mData;
        if (aObj instanceof ShiftVector) return ((ShiftVector)aObj).mData;
        if (aObj instanceof double[]) return (double[])aObj;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int shiftSize() {return mShift;}
    
    
    /** Optimize stuffs，引用反转直接返回 {@link ShiftReverseVector} */
    @Override public IVectorOperation operation() {
        return new DoubleArrayVectorOperation_() {
            @Override public ShiftReverseVector refReverse() {
                return new ShiftReverseVector(mSize, mShift, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void increment_(int aIdx) {++mData[aIdx + mShift];}
    @Override public double getAndIncrement_(int aIdx) {return mData[aIdx + mShift]++;}
    @Override public double incrementAndGet_(int aIdx) {return ++mData[aIdx + mShift];}
    @Override public void decrement_(int aIdx) {--mData[aIdx + mShift];}
    @Override public double getAndDecrement_(int aIdx) {return mData[aIdx + mShift]--;}
    @Override public double decrementAndGet_(int aIdx) {return --mData[aIdx + mShift];}
    
    @Override public void add_(int aIdx, double aDelta) {mData[aIdx + mShift] += aDelta;}
    @Override public double getAndAdd_(int aIdx, double aDelta) {
        aIdx += mShift;
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public double addAndGet_(int aIdx, double aDelta) {
        aIdx += mShift;
        double tValue = mData[aIdx];
        tValue += aDelta;
        mData[aIdx] = tValue;
        return tValue;
    }
    @Override public void update_(int aIdx, IOperator1<Double> aOpt) {
        aIdx += mShift;
        mData[aIdx] = aOpt.cal(mData[aIdx]);
    }
    @Override public double getAndUpdate_(int aIdx, IOperator1<Double> aOpt) {
        aIdx += mShift;
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.cal(tValue);
        return tValue;
    }
    @Override public double updateAndGet_(int aIdx, IOperator1<Double> aOpt) {
        aIdx += mShift;
        double tValue = mData[aIdx];
        tValue = aOpt.cal(tValue);
        mData[aIdx] = tValue;
        return tValue;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public Iterator<Double> iterator() {
        return new Iterator<Double>() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public Double next() {
                if (hasNext()) {
                    Double tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public ISetIterator<Double> setIterator() {
        return new ISetIterator<Double>() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(Double e) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = e;
            }
            @Override public Double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    mData[oIdx] = e;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public Double getNextAndSet(Double e) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
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
