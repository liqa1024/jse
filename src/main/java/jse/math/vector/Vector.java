package jse.math.vector;

import jep.NDArray;
import jse.code.collection.DoubleList;
import jse.code.iterator.IDoubleIterator;
import jse.code.iterator.IDoubleSetIterator;
import jse.math.matrix.ColumnMatrix;
import jse.math.matrix.RowMatrix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

/**
 * @author liqa
 * <p> 向量的一般实现 </p>
 */
public class Vector extends DoubleArrayVector {
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
            double[] tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new Vector(mSize, tData);
        }
        
        /** Groovy stuffs */
        public Builder append(double aValue) {return (Builder)super.append(aValue);}
        public Builder appendAll(IVector aVector) {return (Builder)super.appendAll(aVector);}
        @VisibleForTesting public Builder leftShift(double aValue) {return (Builder)super.leftShift(aValue);}
        @VisibleForTesting public Builder leftShift(IVector aVector) {return (Builder)super.leftShift(aVector);}
    }
    
    
    private int mSize;
    public Vector(int aSize, double[] aData) {super(aData); mSize = aSize;}
    public Vector(double[] aData) {this(aData.length, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    
    /** IVector stuffs */
    @Override public final double get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx];}
    @Override public final void set(int aIdx, double aValue) {rangeCheck(aIdx, mSize); mData[aIdx] = aValue;}
    @Override public final double getAndSet(int aIdx, double aValue) {
        rangeCheck(aIdx, mSize);
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public final int size() {return mSize;}
    
    @Override protected final Vector newZeros_(int aSize) {return Vector.zeros(aSize);}
    @Override public final Vector copy() {return (Vector)super.copy();}
    
    @Override public final double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof Vector) return ((Vector)aObj).mData;
        if (aObj instanceof ShiftVector) return ((ShiftVector)aObj).mData;
        if (aObj instanceof DoubleList) return ((DoubleList)aObj).internalData();
        if (aObj instanceof double[]) return (double[])aObj;
        return null;
    }
    
    @Override public final NDArray<double[]> numpy() {return new NDArray<>(mData, mSize);}
    @Override public final Vector toBuf(boolean aAbort) {return this;}
    @Override public final void releaseBuf(@NotNull IVector aBuf, boolean aAbort) {if (aBuf != this) super.releaseBuf(aBuf, aAbort);}
    
    
    /** Optimize stuffs，asMat 直接返回  {@link ColumnMatrix} */
    @Override public ColumnMatrix asMatCol() {
        return new ColumnMatrix(mSize, 1, mData);
    }
    @Override public RowMatrix asMatRow() {
        return new RowMatrix(1, mSize, mData);
    }
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftVector} */
    @Override public final DoubleArrayVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return aFromIdx==0 ? new Vector(aToIdx, mData) : new ShiftVector(aToIdx-aFromIdx, aFromIdx, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        double tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public final void increment(int aIdx) {rangeCheck(aIdx, mSize); ++mData[aIdx];}
    @Override public final double getAndIncrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx]++;}
    @Override public final void decrement(int aIdx) {rangeCheck(aIdx, mSize); --mData[aIdx];}
    @Override public final double getAndDecrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx]--;}
    
    @Override public final void add(int aIdx, double aDelta) {rangeCheck(aIdx, mSize); mData[aIdx] += aDelta;}
    @Override public final double getAndAdd(int aIdx, double aDelta) {
        rangeCheck(aIdx, mSize);
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public final void update(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aOpt.applyAsDouble(mData[aIdx]);
    }
    @Override public final double getAndUpdate(int aIdx, DoubleUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    @Override public final boolean isEmpty() {return mSize==0;}
    @Override public final double last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty Vector");
        return mData[mSize-1];
    }
    @Override public final double first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty Vector");
        return mData[0];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public final IDoubleIterator iterator() {
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
    @Override public final IDoubleSetIterator setIterator() {
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
