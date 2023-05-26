package com.jtool.math.vector;

import com.jtool.code.ISetIterator;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 支持将内部的 double[] 进行平移访问的 Vector，理论拥有和 {@link RealVector} 几乎一样的性能 </p>
 */
public final class ShiftRealVector extends DoubleArrayVector<ShiftRealVector> {
    private final int mSize;
    private final int mShift;
    public ShiftRealVector(int aSize, int aShift, double[] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftRealVector(int aShift, double[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** IVector stuffs */
    @Override public Double get_(int aIdx) {return mData[aIdx + mShift];}
    @Override public void set_(int aIdx, Number aValue) {mData[aIdx + mShift] = aValue.doubleValue();}
    @Override public Double getAndSet_(int aIdx, Number aValue) {
        aIdx += mShift;
        Double oValue = mData[aIdx];
        mData[aIdx] = aValue.doubleValue();
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected ShiftRealVector newZeros(int aSize) {return new ShiftRealVector(aSize, 0, new double[aSize]);}
    
    @Override protected ShiftRealVector this_() {return this;}
    @Override public ShiftRealVector newShell() {return new ShiftRealVector(mSize, mShift, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof RealVector) return ((RealVector)aObj).mData;
        if (aObj instanceof ShiftRealVector) return ((ShiftRealVector)aObj).mData;
        if (aObj instanceof double[]) return (double[])aObj;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int shiftSize() {return mShift;}
    
    
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
                }
                throw new NoSuchElementException();
            }
        };
    }
    @Override public ISetIterator<Double, Number> setIterator() {
        return new ISetIterator<Double, Number>() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(Number e) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = e.doubleValue();
            }
            @Override public Double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    return mData[oIdx];
                }
                throw new NoSuchElementException();
            }
        };
    }
}
