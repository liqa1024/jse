package jtool.math.vector;

import jtool.code.collection.IntegerList;
import jtool.code.functional.IIntegerConsumer1;
import jtool.code.functional.IIntegerOperator1;
import jtool.code.functional.IIntegerSupplier;
import jtool.code.iterator.IIntegerIterator;
import jtool.code.iterator.IIntegerSetIterator;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

import static jtool.math.vector.AbstractVector.subVecRangeCheck;

/**
 * @author liqa
 * <p> 支持将内部的 int[] 进行平移访问的 IntegerVector，理论拥有和 {@link IntegerVector} 几乎一样的性能 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link IntegerVector} </p>
 */
public final class ShiftIntegerVector extends IntegerArrayVector {
    private int mSize;
    private int mShift;
    public ShiftIntegerVector(int aSize, int aShift, int[] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftIntegerVector(int aShift, int[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public int shift() {return mShift;}
    public ShiftIntegerVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length-mShift, aSize); return this;}
    public ShiftIntegerVector setShift(int aShift) {mShift = MathEX.Code.toRange(0, mData.length-mSize, aShift); return this;}
    public int dataLength() {return mData.length;}
    
    /** ILogicalVector stuffs */
    @Override public int get_(int aIdx) {return mData[aIdx + mShift];}
    @Override public void set_(int aIdx, int aValue) {mData[aIdx + mShift] = aValue;}
    @Override public int getAndSet_(int aIdx, int aValue) {
        aIdx += mShift;
        int oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    
    @Override public ShiftIntegerVector newShell() {return new ShiftIntegerVector(mSize, mShift, null);}
    @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof IntegerVector) return ((IntegerVector)aObj).mData;
        if (aObj instanceof ShiftIntegerVector) return ((ShiftIntegerVector)aObj).mData;
        if (aObj instanceof IntegerList) return ((IntegerList)aObj).internalData();
        if (aObj instanceof int[]) return (int[])aObj;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return shift();}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftIntegerVector} */
    @Override public ShiftIntegerVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftIntegerVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速遍历 */
    @Override public IIntegerVectorOperation operation() {
        return new IntegerArrayVectorOperation_() {
            @Override public void fill(IIntegerVectorGetter aRHS) {
                final int tEnd = mSize + mShift;
                for (int i = mShift, j = 0; i < tEnd; ++i, ++j) mData[i] = aRHS.get(j);
            }
            @Override public void assign(IIntegerSupplier aSup) {
                final int tEnd = mSize + mShift;
                for (int i = mShift; i < tEnd; ++i) mData[i] = aSup.get();
            }
            @Override public void forEach(IIntegerConsumer1 aCon) {
                final int tEnd = mSize + mShift;
                for (int i = mShift; i < tEnd; ++i) aCon.run(mData[i]);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void increment_(int aIdx) {++mData[aIdx + mShift];}
    @Override public int getAndIncrement_(int aIdx) {return mData[aIdx + mShift]++;}
    @Override public void decrement_(int aIdx) {--mData[aIdx + mShift];}
    @Override public int getAndDecrement_(int aIdx) {return mData[aIdx + mShift]--;}
    
    @Override public void add_(int aIdx, int aDelta) {mData[aIdx + mShift] += aDelta;}
    @Override public int getAndAdd_(int aIdx, int aDelta) {
        aIdx += mShift;
        int tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public void update_(int aIdx, IIntegerOperator1 aOpt) {
        aIdx += mShift;
        mData[aIdx] = aOpt.cal(mData[aIdx]);
    }
    @Override public int getAndUpdate_(int aIdx, IIntegerOperator1 aOpt) {
        aIdx += mShift;
        int tValue = mData[aIdx];
        mData[aIdx] = aOpt.cal(tValue);
        return tValue;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IIntegerIterator iterator() {
        return new IIntegerIterator() {
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
    @Override public IIntegerSetIterator setIterator() {
        return new IIntegerSetIterator() {
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
