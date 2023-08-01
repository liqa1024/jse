package com.jtool.math.vector;

import com.jtool.code.iterator.IBooleanIterator;
import com.jtool.code.iterator.IBooleanSetIterator;
import com.jtool.code.operator.IBooleanOperator1;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;

/**
 * @author liqa
 * <p> 向量的一般实现 </p>
 */
public final class LogicalVector extends BooleanArrayVector {
    /** 提供默认的创建 */
    public static LogicalVector ones(int aSize) {
        boolean[] tData = new boolean[aSize];
        Arrays.fill(tData, true);
        return new LogicalVector(tData);
    }
    public static LogicalVector zeros(int aSize) {return new LogicalVector(new boolean[aSize]);}
    
    
    private final int mSize;
    public LogicalVector(int aSize, boolean[] aData) {super(aData); mSize = aSize;}
    public LogicalVector(boolean[] aData) {this(aData.length, aData);}
    
    /** ILogicalVector stuffs */
    @Override public boolean get_(int aIdx) {return mData[aIdx];}
    @Override public void set_(int aIdx, boolean aValue) {mData[aIdx] = aValue;}
    @Override public boolean getAndSet_(int aIdx, boolean aValue) {
        boolean oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override public LogicalVector newZeros(int aSize) {return LogicalVector.zeros(aSize);}
    
    @Override public LogicalVector newShell() {return new LogicalVector(mSize, null);}
    @Override public boolean @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof LogicalVector) return ((LogicalVector)aObj).mData;
        if (aObj instanceof boolean[]) return (boolean[])aObj;
        return null;
    }
    
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void flip_(int aIdx) {
        mData[aIdx] = !mData[aIdx];
    }
    @Override public boolean getAndFlip_(int aIdx) {
        boolean tValue = mData[aIdx];
        mData[aIdx] = !tValue;
        return tValue;
    }
    @Override public void update_(int aIdx, IBooleanOperator1 aOpt) {
        mData[aIdx] = aOpt.cal(mData[aIdx]);
    }
    @Override public boolean getAndUpdate_(int aIdx, IBooleanOperator1 aOpt) {
        boolean tValue = mData[aIdx];
        mData[aIdx] = aOpt.cal(tValue);
        return tValue;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IBooleanIterator iterator() {
        return new IBooleanIterator() {
            private int mIdx = 0;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
