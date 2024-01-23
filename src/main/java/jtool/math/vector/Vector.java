package jtool.math.vector;

import jtool.code.collection.DoubleList;
import jtool.code.iterator.IDoubleIterator;
import jtool.code.iterator.IDoubleSetIterator;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.*;

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
    
    /** 提供 builder 方式的构建 */
    public static Builder builder() {return new Builder();}
    public static Builder builder(int aInitSize) {return new Builder(aInitSize);}
    public final static class Builder extends DoubleList {
        private final static int DEFAULT_INIT_SIZE = 8;
        private Builder() {super(DEFAULT_INIT_SIZE);}
        private Builder(int aInitSize) {super(aInitSize);}
        
        public Vector build() {
            return new Vector(mSize, mData);
        }
    }
    
    
    private int mSize;
    public Vector(int aSize, double[] aData) {super(aData); mSize = aSize;}
    public Vector(double[] aData) {this(aData.length, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public Vector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length, aSize); return this;}
    public int dataLength() {return mData.length;}
    
    /** IVector stuffs */
    @Override public double get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx];}
    @Override public void set(int aIdx, double aValue) {rangeCheck(aIdx, mSize); mData[aIdx] = aValue;}
    @Override public double getAndSet(int aIdx, double aValue) {
        rangeCheck(aIdx, mSize);
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected Vector newZeros_(int aSize) {return Vector.zeros(aSize);}
    @Override public Vector copy() {return (Vector)super.copy();}
    
    @Override public Vector newShell() {return new Vector(mSize, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof Vector) return ((Vector)aObj).mData;
        if (aObj instanceof ShiftVector) return ((ShiftVector)aObj).mData;
        if (aObj instanceof DoubleList) return ((DoubleList)aObj).internalData();
        if (aObj instanceof double[]) return (double[])aObj;
        return null;
    }
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftVector} */
    @Override public DoubleArrayVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return aFromIdx==0 ? new Vector(aToIdx, mData) : new ShiftVector(aToIdx-aFromIdx, aFromIdx, mData);
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
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        double tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void increment(int aIdx) {rangeCheck(aIdx, mSize); ++mData[aIdx];}
    @Override public double getAndIncrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx]++;}
    @Override public void decrement(int aIdx) {rangeCheck(aIdx, mSize); --mData[aIdx];}
    @Override public double getAndDecrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx]--;}
    
    @Override public void add(int aIdx, double aDelta) {rangeCheck(aIdx, mSize); mData[aIdx] += aDelta;}
    @Override public double getAndAdd(int aIdx, double aDelta) {
        rangeCheck(aIdx, mSize);
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public void update(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aOpt.applyAsDouble(mData[aIdx]);
    }
    @Override public double getAndUpdate(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public double last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty Vector");
        return mData[mSize-1];
    }
    @Override public double first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty Vector");
        return mData[0];
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
        };
    }
}
