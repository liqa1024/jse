package com.jtool.math.vector;

import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.code.operator.IDoubleOperator1;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 向量的一般实现 </p>
 */
public final class Vector extends DoubleArrayVector {
    /** 提供默认的创建 */
    public static Vector ones(int aSize) {
        double[] tData = new double[aSize];
        Arrays.fill(tData, 1.0);
        return new Vector(tData);
    }
    public static Vector zeros(int aSize) {return new Vector(new double[aSize]);}
    
    
    private final int mSize;
    public Vector(int aSize, double[] aData) {super(aData); mSize = aSize;}
    public Vector(double[] aData) {this(aData.length, aData);}
    
    /** IVector stuffs */
    @Override public double get_(int aIdx) {return mData[aIdx];}
    @Override public void set_(int aIdx, double aValue) {mData[aIdx] = aValue;}
    @Override public double getAndSet_(int aIdx, double aValue) {
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override public Vector newZeros(int aSize) {return Vector.zeros(aSize);}
    
    @Override public Vector newShell() {return new Vector(mSize, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof Vector) return ((Vector)aObj).mData;
        if (aObj instanceof ShiftVector) return ((ShiftVector)aObj).mData;
        if (aObj instanceof double[]) return (double[])aObj;
        return null;
    }
    
    
    /** Optimize stuffs，引用反转直接返回 {@link ReverseVector} */
    @Override public IVectorOperation operation() {
        return new DoubleArrayVectorOperation_() {
            @Override public ReverseVector refReverse() {
                return new ReverseVector(mSize, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void increment_(int aIdx) {++mData[aIdx];}
    @Override public double getAndIncrement_(int aIdx) {return mData[aIdx]++;}
    @Override public double incrementAndGet_(int aIdx) {return ++mData[aIdx];}
    @Override public void decrement_(int aIdx) {--mData[aIdx];}
    @Override public double getAndDecrement_(int aIdx) {return mData[aIdx]--;}
    @Override public double decrementAndGet_(int aIdx) {return --mData[aIdx];}
    
    @Override public void add_(int aIdx, double aDelta) {mData[aIdx] += aDelta;}
    @Override public double getAndAdd_(int aIdx, double aDelta) {
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public double addAndGet_(int aIdx, double aDelta) {
        double tValue = mData[aIdx];
        tValue += aDelta;
        mData[aIdx] = tValue;
        return tValue;
    }
    @Override public void update_(int aIdx, IDoubleOperator1 aOpt) {
        mData[aIdx] = aOpt.cal(mData[aIdx]);
    }
    @Override public double getAndUpdate_(int aIdx, IDoubleOperator1 aOpt) {
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.cal(tValue);
        return tValue;
    }
    @Override public double updateAndGet_(int aIdx, IDoubleOperator1 aOpt) {
        double tValue = mData[aIdx];
        tValue = aOpt.cal(tValue);
        mData[aIdx] = tValue;
        return tValue;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IDoubleIterator iterator() {
        return new IDoubleIterator() {
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
            @Override public double getNextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
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
