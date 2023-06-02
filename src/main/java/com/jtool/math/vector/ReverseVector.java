package com.jtool.math.vector;

import com.jtool.code.ISetIterator;
import com.jtool.math.matrix.ColumnMatrix;
import com.jtool.math.matrix.DoubleArrayMatrix;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 反向检索数组的向量，注意没有经过 shift，因此检索的数组区间依旧是 0 ~ size()-1 </p>
 * <p> 由于是反向检索的，由于优化内部实际许多操作会是反向进行的 </p>
 */
public final class ReverseVector extends DoubleArrayVector<ReverseVector> {
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
    
    @Override protected ReverseVector newZeros(int aSize) {return ReverseVector.zeros(aSize);}
    
    @Override public ReverseVector newShell() {return new ReverseVector(mSize, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ReverseVector) return ((ReverseVector)aObj).mData;
        if (aObj instanceof ShiftReverseVector) return ((ShiftReverseVector)aObj).mData;
        return null;
    }
    
    
    /** Optimize stuffs，引用反转直接返回 {@link Vector} */
    @Override public DoubleArrayVectorOperation operation() {
        return new DoubleArrayVectorOperation() {
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
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public Iterator<Double> iterator() {
        return new Iterator<Double>() {
            private int mIdx = mSizeMM;
            @Override public boolean hasNext() {return mIdx >= 0;}
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
            private int mIdx = mSizeMM, oIdx = -1;
            @Override public boolean hasNext() {return mIdx >= 0;}
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
