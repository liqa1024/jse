package jtool.math.vector;

import jtool.code.collection.BooleanList;
import jtool.code.functional.IBooleanConsumer;
import jtool.code.functional.IBooleanUnaryOperator;
import jtool.code.functional.IBooleanSupplier;
import jtool.code.iterator.IBooleanIterator;
import jtool.code.iterator.IBooleanSetIterator;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;

import static jtool.math.vector.AbstractVector.subVecRangeCheck;

/**
 * @author liqa
 * <p> 支持将内部的 boolean[] 进行平移访问的 LogicalVector，理论拥有和 {@link LogicalVector} 几乎一样的性能 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link LogicalVector} </p>
 */
public final class ShiftLogicalVector extends BooleanArrayVector {
    private int mSize;
    private int mShift;
    public ShiftLogicalVector(int aSize, int aShift, boolean[] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftLogicalVector(int aShift, boolean[] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public int shift() {return mShift;}
    public ShiftLogicalVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length-mShift, aSize); return this;}
    public ShiftLogicalVector setShift(int aShift) {mShift = MathEX.Code.toRange(0, mData.length-mSize, aShift); return this;}
    public int dataLength() {return mData.length;}
    
    /** ILogicalVector stuffs */
    @Override public boolean get_(int aIdx) {return mData[aIdx + mShift];}
    @Override public void set_(int aIdx, boolean aValue) {mData[aIdx + mShift] = aValue;}
    @Override public boolean getAndSet_(int aIdx, boolean aValue) {
        aIdx += mShift;
        boolean oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected LogicalVector newZeros_(int aSize) {return LogicalVector.zeros(aSize);}
    @Override public LogicalVector copy() {
        LogicalVector rVector = LogicalVector.zeros(mSize);
        rVector.fill(this);
        return rVector;
    }
    
    @Override public ShiftLogicalVector newShell() {return new ShiftLogicalVector(mSize, mShift, null);}
    @Override public boolean @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof LogicalVector) return ((LogicalVector)aObj).mData;
        if (aObj instanceof ShiftLogicalVector) return ((ShiftLogicalVector)aObj).mData;
        if (aObj instanceof BooleanList) return ((BooleanList)aObj).internalData();
        if (aObj instanceof boolean[]) return (boolean[])aObj;
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return shift();}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftLogicalVector} */
    @Override public ShiftLogicalVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftLogicalVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速遍历 */
    @Override public ILogicalVectorOperation operation() {
        return new BooleanArrayVectorOperation_() {
            @Override public void fill(ILogicalVectorGetter aRHS) {
                final int tEnd = mSize + mShift;
                for (int i = mShift, j = 0; i < tEnd; ++i, ++j) mData[i] = aRHS.get(j);
            }
            @Override public void assign(IBooleanSupplier aSup) {
                final int tEnd = mSize + mShift;
                for (int i = mShift; i < tEnd; ++i) mData[i] = aSup.getAsBoolean();
            }
            @Override public void forEach(IBooleanConsumer aCon) {
                final int tEnd = mSize + mShift;
                for (int i = mShift; i < tEnd; ++i) aCon.accept(mData[i]);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override void swap_(int aIdx1, int aIdx2) {
        aIdx1 += mShift;
        aIdx2 += mShift;
        boolean tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void flip_(int aIdx) {
        aIdx += mShift;
        mData[aIdx] = !mData[aIdx];
    }
    @Override public boolean getAndFlip_(int aIdx) {
        aIdx += mShift;
        boolean tValue = mData[aIdx];
        mData[aIdx] = !tValue;
        return tValue;
    }
    @Override public void update_(int aIdx, IBooleanUnaryOperator aOpt) {
        aIdx += mShift;
        mData[aIdx] = aOpt.applyAsBoolean(mData[aIdx]);
    }
    @Override public boolean getAndUpdate_(int aIdx, IBooleanUnaryOperator aOpt) {
        aIdx += mShift;
        boolean tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsBoolean(tValue);
        return tValue;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public boolean last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty Vector");
        return mData[mSize-1+mShift];
    }
    @Override public boolean first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty Vector");
        return mData[mShift];
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IBooleanIterator iterator() {
        return new IBooleanIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public boolean next() {
                if (hasNext()) {
                    boolean tNext = mData[mIdx];
                    ++mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IBooleanSetIterator setIterator() {
        return new IBooleanSetIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(boolean aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public boolean next() {
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
            @Override public void nextAndSet(boolean aValue) {
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
