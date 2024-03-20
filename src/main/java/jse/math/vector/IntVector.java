package jse.math.vector;

import jse.code.collection.IntList;
import jse.code.iterator.IIntIterator;
import jse.code.iterator.IIntSetIterator;
import jse.math.MathEX;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.IntUnaryOperator;

import static jse.math.vector.AbstractVector.*;


/**
 * @author liqa
 * <p> 整数向量的一般实现 </p>
 */
public class IntVector extends IntArrayVector {
    /** 提供默认的创建 */
    public static IntVector ones(int aSize) {
        int[] tData = new int[aSize];
        Arrays.fill(tData, 1);
        return new IntVector(tData);
    }
    public static IntVector zeros(int aSize) {return new IntVector(new int[aSize]);}
    
    /** 提供 builder 方式的构建 */
    public static Builder builder() {return new Builder();}
    public static Builder builder(int aInitSize) {return new Builder(aInitSize);}
    public final static class Builder extends IntList {
        private final static int DEFAULT_INIT_SIZE = 8;
        private Builder() {super(DEFAULT_INIT_SIZE);}
        private Builder(int aInitSize) {super(aInitSize);}
        
        public IntVector build() {
            int[] tData = mData;
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new IntVector(mSize, tData);
        }
    }
    
    private int mSize;
    public IntVector(int aSize, int[] aData) {super(aData); mSize = aSize;}
    public IntVector(int[] aData) {this(aData.length, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public final IntVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length, aSize); return this;}
    public final int dataLength() {return mData.length;}
    
    /** IIntegerVector stuffs */
    @Override public final int get(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx];}
    @Override public final void set(int aIdx, int aValue) {rangeCheck(aIdx, mSize); mData[aIdx] = aValue;}
    @Override public final int getAndSet(int aIdx, int aValue) {
        rangeCheck(aIdx, mSize);
        int oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public final int size() {return mSize;}
    
    @Override protected final IntVector newZeros_(int aSize) {return IntVector.zeros(aSize);}
    @Override public final IntVector copy() {return (IntVector)super.copy();}
    
    @Override public final IntVector newShell() {return new IntVector(mSize, null);}
    @Override public final int @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof IntVector) return ((IntVector)aObj).mData;
        if (aObj instanceof ShiftIntVector) return ((ShiftIntVector)aObj).mData;
        if (aObj instanceof IntList) return ((IntList)aObj).internalData();
        if (aObj instanceof int[]) return (int[])aObj;
        return null;
    }
    
    @Override public final IntVector toBuf(boolean aAbort) {return this;}
    @Override public final void releaseBuf(@NotNull IIntVector aBuf, boolean aAbort) {if (aBuf != this) super.releaseBuf(aBuf, aAbort);}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftIntVector} */
    @Override public final IntArrayVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return aFromIdx==0 ? new IntVector(aToIdx, mData) : new ShiftIntVector(aToIdx-aFromIdx, aFromIdx, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        int tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public final void increment(int aIdx) {rangeCheck(aIdx, mSize); ++mData[aIdx];}
    @Override public final int getAndIncrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx]++;}
    @Override public final void decrement(int aIdx) {rangeCheck(aIdx, mSize); --mData[aIdx];}
    @Override public final int getAndDecrement(int aIdx) {rangeCheck(aIdx, mSize); return mData[aIdx]--;}
    
    @Override public final void add(int aIdx, int aDelta) {rangeCheck(aIdx, mSize); mData[aIdx] += aDelta;}
    @Override public final int getAndAdd(int aIdx, int aDelta) {
        rangeCheck(aIdx, mSize);
        int tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public final void update(int aIdx, IntUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aOpt.applyAsInt(mData[aIdx]);
    }
    @Override public final int getAndUpdate(int aIdx, IntUnaryOperator aOpt) {
        rangeCheck(aIdx, mSize);
        int tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsInt(tValue);
        return tValue;
    }
    @Override public final boolean isEmpty() {return mSize==0;}
    @Override public final int last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty IntVector");
        return mData[mSize-1];
    }
    @Override public final int first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty IntVector");
        return mData[0];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式）*/
    @Override public final IIntIterator iterator() {
        return new IIntIterator() {
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public int next() {
                if (hasNext()) {
                    int tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public final IIntSetIterator setIterator() {
        return new IIntSetIterator() {
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
            @Override public void set(int aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public int next() {
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
            @Override public void nextAndSet(int aValue) {
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
