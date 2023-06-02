package com.jtool.math.vector;

import com.jtool.code.ISetIterator;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 向量的一般实现 </p>
 */
public final class Vector extends DoubleArrayVector<Vector> implements IVector {
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
    
    @Override protected Vector newZeros(int aSize) {return Vector.zeros(aSize);}
    
    @Override public Vector newShell() {return new Vector(mSize, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof Vector) return ((Vector)aObj).mData;
        if (aObj instanceof ShiftVector) return ((ShiftVector)aObj).mData;
        if (aObj instanceof double[]) return (double[])aObj;
        return null;
    }
    
    
    /** Optimize stuffs，引用反转直接返回 {@link ReverseVector} */
    @Override public DoubleArrayVectorOperation operation() {
        return new DoubleArrayVectorOperation() {
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
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public Iterator<Double> iterator() {
        return new Iterator<Double>() {
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
