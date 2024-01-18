package jtool.math.vector;

import jtool.code.iterator.IDoubleIterator;
import jtool.code.iterator.IDoubleSetIterator;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.*;

/**
 * @author liqa
 * <p> 反向检索数组的向量，注意没有经过 shift，因此检索的数组区间依旧是 0 ~ size()-1 </p>
 * <p> 由于是反向检索的，由于优化内部实际许多操作会是反向进行的 </p>
 */
public final class ReverseVector extends DoubleArrayVector {
    /** 提供默认的创建 */
    public static ReverseVector ones(int aSize) {
        double[] tData = new double[aSize];
        Arrays.fill(tData, 1.0);
        return new ReverseVector(tData);
    }
    public static ReverseVector zeros(int aSize) {return new ReverseVector(new double[aSize]);}
    
    
    private int mSize;
    private int mSizeMM;
    public ReverseVector(int aSize, double[] aData) {super(aData); mSize = aSize; mSizeMM = mSize-1;}
    public ReverseVector(double[] aData) {this(aData.length, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public ReverseVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, mData.length, aSize); mSizeMM = mSize-1; return this;}
    public int dataLength() {return mData.length;}
    
    /** IVector stuffs */
    @Override public double get_(int aIdx) {return mData[mSizeMM-aIdx];}
    @Override public void set_(int aIdx, double aValue) {mData[mSizeMM-aIdx] = aValue;}
    @Override public double getAndSet_(int aIdx, double aValue) {
        aIdx = mSizeMM-aIdx;
        double oValue = mData[aIdx];
        mData[aIdx] = aValue;
        return oValue;
    }
    @Override public int size() {return mSize;}
    
    @Override protected ReverseVector newZeros_(int aSize) {return ReverseVector.zeros(aSize);}
    @Override public ReverseVector copy() {
        ReverseVector rVector = ReverseVector.zeros(mSize);
        rVector.fill(this);
        return rVector;
    }
    
    @Override public ReverseVector newShell() {return new ReverseVector(mSize, null);}
    @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ReverseVector) return ((ReverseVector)aObj).mData;
        if (aObj instanceof ShiftReverseVector) return ((ShiftReverseVector)aObj).mData;
        return null;
    }
    @Override public boolean isReverse() {return true;}
    
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftReverseVector} */
    @Override public ShiftReverseVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftReverseVector(aToIdx-aFromIdx, mSize-aToIdx, mData);
    }
    
    /** Optimize stuffs，引用反转直接返回 {@link Vector} */
    @Override public IVectorOperation operation() {
        return new DoubleArrayVectorOperation_() {
            @Override public void fill(IVectorGetter aRHS) {
                for (int i = mSizeMM, j = 0; i >= 0; --i, ++j) mData[i] = aRHS.get(j);
            }
            @Override public void assign(DoubleSupplier aSup) {
                for (int i = mSizeMM; i >= 0; --i) mData[i] = aSup.getAsDouble();
            }
            @Override public void forEach(DoubleConsumer aCon) {
                for (int i = mSizeMM; i >= 0; --i) aCon.accept(mData[i]);
            }
            @Override public Vector refReverse() {
                return new Vector(mSize, mData);
            }
        };
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override void swap_(int aIdx1, int aIdx2) {
        aIdx1 = mSizeMM-aIdx1;
        aIdx2 = mSizeMM-aIdx2;
        double tValue = mData[aIdx2];
        mData[aIdx2] = mData[aIdx1];
        mData[aIdx1] = tValue;
    }
    
    @Override public void increment_(int aIdx) {++mData[mSizeMM-aIdx];}
    @Override public double getAndIncrement_(int aIdx) {return mData[mSizeMM-aIdx]++;}
    @Override public void decrement_(int aIdx) {--mData[mSizeMM-aIdx];}
    @Override public double getAndDecrement_(int aIdx) {return mData[mSizeMM-aIdx]--;}
    
    @Override public void add_(int aIdx, double aDelta) {mData[mSizeMM-aIdx] += aDelta;}
    @Override public double getAndAdd_(int aIdx, double aDelta) {
        aIdx = mSizeMM-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] += aDelta;
        return tValue;
    }
    @Override public void update_(int aIdx, DoubleUnaryOperator aOpt) {
        aIdx = mSizeMM-aIdx;
        mData[aIdx] = aOpt.applyAsDouble(mData[aIdx]);
    }
    @Override public double getAndUpdate_(int aIdx, DoubleUnaryOperator aOpt) {
        aIdx = mSizeMM-aIdx;
        double tValue = mData[aIdx];
        mData[aIdx] = aOpt.applyAsDouble(tValue);
        return tValue;
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IDoubleIterator iterator() {
        return new IDoubleIterator() {
            private int mIdx = mSizeMM;
            @Override public boolean hasNext() {return mIdx >= 0;}
            @Override public double next() {
                if (hasNext()) {
                    double tNext = mData[mIdx];
                    --mIdx;
                    return tNext;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
    @Override public IDoubleSetIterator setIterator() {
        return new IDoubleSetIterator() {
            private int mIdx = mSizeMM, oIdx = -1;
            @Override public boolean hasNext() {return mIdx >= 0;}
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[oIdx] = aValue;
            }
            @Override public double next() {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                    return mData[oIdx];
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx;
                    --mIdx;
                    mData[oIdx] = aValue;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
