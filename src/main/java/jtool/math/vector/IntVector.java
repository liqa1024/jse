package jtool.math.vector;

import jtool.code.collection.IntList;
import jtool.code.iterator.IIntIterator;
import jtool.code.iterator.IIntSetIterator;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.IntUnaryOperator;

import static jtool.math.vector.AbstractVector.subVecRangeCheck;


/**
 * @author liqa
 * <p> 整数向量的一般实现 </p>
 */
public final class IntVector extends IntArrayVector {
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
            return new IntVector(mSize, mData);
        }
    }
    
    private int mSize;
    public IntVector(int aSize, int[] aData) {super(aData); mSize = aSize;}
    public IntVector(int[] aData) {this(aData.length, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public IntVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length, aSize); return this;}
    public int dataLength() {return mData.length;}
    
    /** IIntegerVector stuffs */
    @Override protected int get_(int aIdx) {return mData[aIdx];}
    @Override protected void set_(int aIdx, int aValue) {mData[aIdx] = aValue;}
    @Override protected int getAndSet_(int aIdx, int aValue) {
        int oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected IntVector newZeros_(int aSize) {return IntVector.zeros(aSize);}
    @Override public IntVector copy() {return (IntVector)super.copy();}
    
    @Override public IntVector newShell() {return new IntVector(mSize, null);}
    @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof IntVector) return ((IntVector)aObj).mData;
        if (aObj instanceof ShiftIntVector) return ((ShiftIntVector)aObj).mData;
        if (aObj instanceof IntList) return ((IntList)aObj).internalData();
        if (aObj instanceof int[]) return (int[])aObj;
        return null;
    }
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftIntVector} */
    @Override public IntArrayVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return aFromIdx==0 ? new IntVector(aToIdx, mData) : new ShiftIntVector(aToIdx-aFromIdx, aFromIdx, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override protected void swap_(int aIdx1, int aIdx2) {
        int tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override protected void increment_(int aIdx) {++mData[aIdx];}
    @Override protected int getAndIncrement_(int aIdx) {return mData[aIdx]++;}
    @Override protected void decrement_(int aIdx) {--mData[aIdx];}
    @Override protected int getAndDecrement_(int aIdx) {return mData[aIdx]--;}
    
    @Override protected void add_(int aIdx, int aDelta) {mData[aIdx] += aDelta;}
    @Override protected int getAndAdd_(int aIdx, int aDelta) {
        int tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override protected void update_(int aIdx, IntUnaryOperator aOpt) {
        mData[aIdx] = aOpt.applyAsInt(mData[aIdx]);
    }
    @Override protected int getAndUpdate_(int aIdx, IntUnaryOperator aOpt) {
        int tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsInt(tValue);
        return tValue;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public double last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty IntVector");
        return mData[mSize-1];
    }
    @Override public double first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty IntVector");
        return mData[0];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式）*/
    @Override public IIntIterator iterator() {
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
    @Override public IIntSetIterator setIterator() {
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
