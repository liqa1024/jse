package jse.math.vector;

import jse.code.collection.ComplexDoubleList;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IComplexDoubleIterator;
import jse.code.iterator.IComplexDoubleSetIterator;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.DoubleUnaryOperator;

import static jse.math.vector.AbstractVector.*;

/**
 * 复向量的一般实现
 * <p>
 * 现在合并了 ShiftComplexVector 的功能，从而同时支持从数组任意位置开始操作
 * @author liqa
 */
public class ComplexVector extends BiDoubleArrayVector {
    /** 提供默认的创建 */
    public static ComplexVector ones(int aSize) {
        double[][] tData = new double[2][aSize];
        Arrays.fill(tData[0], 1.0);
        return new ComplexVector(tData);
    }
    public static ComplexVector zeros(int aSize) {return new ComplexVector(new double[2][aSize]);}
    
    /** 提供 builder 方式的构建 */
    public static Builder builder() {return new Builder();}
    public static Builder builder(int aInitSize) {return new Builder(aInitSize);}
    public final static class Builder extends ComplexDoubleList {
        private final static int DEFAULT_INIT_SIZE = 8;
        private Builder() {this(DEFAULT_INIT_SIZE);}
        private Builder(int aInitSize) {super(aInitSize);}
        
        public ComplexVector build() {
            double[][] tData = Objects.requireNonNull(mData);
            mData = null; // 设为 null 防止再通过 Builder 来修改此数据
            return new ComplexVector(mSize, tData);
        }
        
        /** Groovy stuffs */
        @Override public Builder append(IComplexDouble aValue) {return (Builder)super.append(aValue);}
        @Override public Builder appendAll(IComplexVector aVector) {return (Builder)super.appendAll(aVector);}
        @Override @VisibleForTesting public Builder leftShift(IComplexDouble aValue) {return (Builder)super.leftShift(aValue);}
        @Override @VisibleForTesting public Builder leftShift(IComplexVector aVector) {return (Builder)super.leftShift(aVector);}
    }
    
    
    private int mSize;
    private int mShift = 0;
    public ComplexVector(int aSize, int aShift, double[][] aData) {
        super(aData);
        mSize = aSize;
        mShift = aShift;
    }
    public ComplexVector(int aSize, double[][] aData) {
        super(aData);
        mSize = aSize;
    }
    public ComplexVector(double[][] aData) {
        this(Math.min(aData[0].length, aData[1].length), aData);
    }
    
    /** 提供额外的接口来直接设置底层参数 */
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    @Override public void setInternalDataShift(int aShift) {mShift = aShift;}
    
    /** IComplexVector stuffs */
    @Override public final ComplexDouble get(int aIdx) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        return new ComplexDouble(mData[0][aIdx], mData[1][aIdx]);
    }
    @Override public final double getReal(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[0][aIdx+mShift];
    }
    @Override public final double getImag(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[1][aIdx+mShift];
    }
    @Override public final void set(int aIdx, IComplexDouble aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[0][aIdx] = aValue.real();
        mData[1][aIdx] = aValue.imag();
    }
    @Override public final void set(int aIdx, ComplexDouble aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[0][aIdx] = aValue.mReal;
        mData[1][aIdx] = aValue.mImag;
    }
    @Override public final void set(int aIdx, double aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[0][aIdx] = aValue;
        mData[1][aIdx] = 0.0;
    }
    @Override public final void set(int aIdx, double aReal, double aImag) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[0][aIdx] = aReal;
        mData[1][aIdx] = aImag;
    }
    @Override public final void setReal(int aIdx, double aReal) {
        rangeCheck(aIdx, mSize);
        mData[0][aIdx+mShift] = aReal;
    }
    @Override public final void setImag(int aIdx, double aImag) {
        rangeCheck(aIdx, mSize);
        mData[1][aIdx+mShift] = aImag;
    }
    @Override public final ComplexDouble getAndSet(int aIdx, IComplexDouble aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]);
        mData[0][aIdx] = aValue.real();
        mData[1][aIdx] = aValue.imag();
        return oValue;
    }
    @Override public final ComplexDouble getAndSet(int aIdx, ComplexDouble aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]);
        mData[0][aIdx] = aValue.mReal;
        mData[1][aIdx] = aValue.mImag;
        return oValue;
    }
    @Override public final ComplexDouble getAndSet(int aIdx, double aValue) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]);
        mData[0][aIdx] = aValue;
        mData[1][aIdx] = 0.0;
        return oValue;
    }
    @Override public final ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]);
        mData[0][aIdx] = aReal;
        mData[1][aIdx] = aImag;
        return oValue;
    }
    @Override public final double getAndSetReal(int aIdx, double aReal) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        double oReal = mData[0][aIdx];
        mData[0][aIdx] = aReal;
        return oReal;
    }
    @Override public final double getAndSetImag(int aIdx, double aImag) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        double oImag = mData[1][aIdx];
        mData[1][aIdx] = aImag;
        return oImag;
    }
    @Override public final int size() {
        return mSize;
    }
    
    @Override protected final ComplexVector newZeros_(int aSize) {return ComplexVector.zeros(aSize);}
    @Override public final ComplexVector copy() {
        ComplexVector rVector = ComplexVector.zeros(mSize);
        rVector.fill(this);
        return rVector;
    }
    
    @Override public final double @Nullable[][] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ComplexVector) return ((ComplexVector)aObj).mData;
        if (aObj instanceof ComplexDoubleList) return ((ComplexDoubleList)aObj).internalData();
        if (aObj instanceof double[][]) {
            double[][] tData = (double[][])aObj;
            if (tData.length==2 && tData[0]!=null && tData[1]!=null) return tData;
        }
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return mShift;}
    
    /** Optimize stuffs，real()，imag() 直接返回 {@link Vector} */
    @Override public final Vector real() {
        return new Vector(mSize, mData[0]);
    }
    @Override public final Vector imag() {
        return new Vector(mSize, mData[1]);
    }
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ComplexVector} */
    @Override public final ComplexVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ComplexVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public final void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
        aIdx1 += mShift;
        aIdx2 += mShift;
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        double tReal = tRealData[aIdx2];
        tRealData[aIdx2] = tRealData[aIdx1];
        tRealData[aIdx1] = tReal;
        double tImag = tImagData[aIdx2];
        tImagData[aIdx2] = tImagData[aIdx1];
        tImagData[aIdx1] = tImag;
    }
    
    @Override public final void add(int aIdx, IComplexDouble aDelta) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[0][aIdx] += aDelta.real();
        mData[1][aIdx] += aDelta.imag();
    }
    @Override public final void add(int aIdx, ComplexDouble aDelta) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[0][aIdx] += aDelta.mReal;
        mData[1][aIdx] += aDelta.mImag;
    }
    @Override public final void add(int aIdx, double aDelta) {
        rangeCheck(aIdx, mSize);
        mData[0][aIdx+mShift] += aDelta;
    }
    @Override public final void addImag(int aIdx, double aImag) {
        rangeCheck(aIdx, mSize);
        mData[1][aIdx+mShift] += aImag;
    }
    @Override public final void update(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealData[aIdx], tImagData[aIdx]));
        tRealData[aIdx] = tValue.real();
        tImagData[aIdx] = tValue.imag();
    }
    @Override public final void updateReal(int aIdx, DoubleUnaryOperator aRealOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tRealData = mData[0];
        tRealData[aIdx] = aRealOpt.applyAsDouble(tRealData[aIdx]);
    }
    @Override public final void updateImag(int aIdx, DoubleUnaryOperator aImagOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tImagData = mData[1];
        tImagData[aIdx] = aImagOpt.applyAsDouble(tImagData[aIdx]);
    }
    @Override public final ComplexDouble getAndUpdate(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        ComplexDouble oValue = new ComplexDouble(tRealData[aIdx], tImagData[aIdx]);
        IComplexDouble tValue = aOpt.apply(new ComplexDouble(oValue)); // 用来防止意外的修改
        tRealData[aIdx] = tValue.real();
        tImagData[aIdx] = tValue.imag();
        return oValue;
    }
    @Override public final double getAndUpdateReal(int aIdx, DoubleUnaryOperator aRealOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tRealData = mData[0];
        double oReal = tRealData[aIdx];
        tRealData[aIdx] = aRealOpt.applyAsDouble(oReal);
        return oReal;
    }
    @Override public final double getAndUpdateImag(int aIdx, DoubleUnaryOperator aImagOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tImagData = mData[1];
        double oImag = tImagData[aIdx];
        tImagData[aIdx] = aImagOpt.applyAsDouble(oImag);
        return oImag;
    }
    @Override public final boolean isEmpty() {
        return mSize==0;
    }
    @Override public final ComplexDouble last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty ComplexVector");
        int tLastIdx = mSize-1+mShift;
        return new ComplexDouble(mData[0][tLastIdx], mData[1][tLastIdx]);
    }
    @Override public final ComplexDouble first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty ComplexVector");
        return new ComplexDouble(mData[0][mShift], mData[1][mShift]);
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public final IComplexDoubleIterator iterator() {
        return new IComplexDoubleIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
        };
    }
    @Override public final IComplexDoubleSetIterator setIterator() {
        return new IComplexDoubleSetIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
            @Override public void set(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            @Override public void setReal(double aReal) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
            }
            @Override public void setImag(double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[1][oIdx] = aImag;
            }
            @Override public void nextOnly() {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public double real() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[0][oIdx];
            }
            @Override public double imag() {
                if (oIdx < 0) throw new IllegalStateException();
                return mData[1][oIdx];
            }
            
            /** 重写保证使用此类中的逻辑而不是 IComplexDoubleSetIterator，虽然是一致的 */
            @Override public ComplexDouble next() {nextOnly(); return new ComplexDouble(mData[0][oIdx], mData[1][oIdx]);}
            @Override public void set(IComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.real();
                mData[1][oIdx] = aValue.imag();
            }
            @Override public void set(ComplexDouble aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue.mReal;
                mData[1][oIdx] = aValue.mImag;
            }
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue;
                mData[1][oIdx] = 0.0;
            }
            @Override public void setRealImag(double aReal, double aImag) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aReal;
                mData[1][oIdx] = aImag;
            }
            
            /** 高性能接口重写来进行专门优化 */
            @Override public void nextAndSet(IComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    mData[0][oIdx] = aValue.real();
                    mData[1][oIdx] = aValue.imag();
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(ComplexDouble aValue) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    mData[0][oIdx] = aValue.mReal;
                    mData[1][oIdx] = aValue.mImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aValue) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    mData[0][oIdx] = aValue;
                    mData[1][oIdx] = 0.0;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSet(double aReal, double aImag) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    mData[0][oIdx] = aReal;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetReal(double aReal) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    mData[0][oIdx] = aReal;
                } else {
                    throw new NoSuchElementException();
                }
            }
            @Override public void nextAndSetImag(double aImag) {
                if (hasNext()) {
                    oIdx = mIdx;
                    ++mIdx;
                    mData[1][oIdx] = aImag;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }
}
