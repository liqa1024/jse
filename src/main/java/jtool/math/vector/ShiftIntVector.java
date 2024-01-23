package jtool.math.vector;

import jtool.code.collection.IntList;
import jtool.code.iterator.IIntIterator;
import jtool.code.iterator.IIntSetIterator;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.IntUnaryOperator;

import static jtool.math.vector.AbstractVector.subVecRangeCheck;

/**
 * @author liqa
 * <p> 支持将内部的 int[] 进行平移访问的 IntegerVector，理论拥有和 {@link IntVector} 几乎一样的性能 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link IntVector} </p>
 */
public final class ShiftIntVector extends IntArrayVector {
    private int mSize;
    private int mShift;
    public ShiftIntVector(int aSize, int aShift, int[] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftIntVector(int aShift, int[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public int shift() {return mShift;}
    public ShiftIntVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length-mShift, aSize); return this;}
    public ShiftIntVector setShift(int aShift) {mShift = MathEX.Code.toRange(0, mData.length-mSize, aShift); return this;}
    public int dataLength() {return mData.length;}
    
    /** ILogicalVector stuffs */
    @Override protected int get_(int aIdx) {return mData[aIdx + mShift];}
    @Override protected void set_(int aIdx, int aValue) {mData[aIdx + mShift] = aValue;}
    @Override protected int getAndSet_(int aIdx, int aValue) {
        aIdx += mShift;
        int oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected IntVector newZeros_(int aSize) {return IntVector.zeros(aSize);}
    @Override public IntVector copy() {return (IntVector)super.copy();}
    
    @Override public ShiftIntVector newShell() {return new ShiftIntVector(mSize, mShift, null);}
    @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof IntVector) return ((IntVector)aObj).mData;
        if (aObj instanceof ShiftIntVector) return ((ShiftIntVector)aObj).mData;
        if (aObj instanceof IntList) return ((IntList)aObj).internalData();
        if (aObj instanceof int[]) return (int[])aObj;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return shift();}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftIntVector} */
    @Override public ShiftIntVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftIntVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override protected void swap_(int aIdx1, int aIdx2) {
        aIdx1 += mShift;
        aIdx2 += mShift;
        int tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override protected void increment_(int aIdx) {++mData[aIdx + mShift];}
    @Override protected int getAndIncrement_(int aIdx) {return mData[aIdx + mShift]++;}
    @Override protected void decrement_(int aIdx) {--mData[aIdx + mShift];}
    @Override protected int getAndDecrement_(int aIdx) {return mData[aIdx + mShift]--;}
    
    @Override protected void add_(int aIdx, int aDelta) {mData[aIdx + mShift] += aDelta;}
    @Override protected int getAndAdd_(int aIdx, int aDelta) {
        aIdx += mShift;
        int tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override protected void update_(int aIdx, IntUnaryOperator aOpt) {
        aIdx += mShift;
        mData[aIdx] = aOpt.applyAsInt(mData[aIdx]);
    }
    @Override protected int getAndUpdate_(int aIdx, IntUnaryOperator aOpt) {
        aIdx += mShift;
        int tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsInt(tValue);
        return tValue;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public double last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty IntVector");
        return mData[mSize-1+mShift];
    }
    @Override public double first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty IntVector");
        return mData[mShift];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IIntIterator iterator() {
        return new IIntIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
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
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
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
