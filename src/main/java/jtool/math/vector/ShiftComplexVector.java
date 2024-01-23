package jtool.math.vector;

import jtool.code.collection.ComplexDoubleList;
import jtool.code.functional.IUnaryFullOperator;
import jtool.code.iterator.IComplexDoubleIterator;
import jtool.code.iterator.IComplexDoubleSetIterator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.NoSuchElementException;
import java.util.function.DoubleUnaryOperator;

import static jtool.math.vector.AbstractVector.*;

/**
 * @author liqa
 * <p> 支持将内部的 double[] 进行平移访问的 Vector，理论拥有和 {@link Vector} 几乎一样的性能 </p>
 * <p> 仅用于临时操作，因此由此返回的新对象类型依旧为 {@link Vector} </p>
 */
public final class ShiftComplexVector extends BiDoubleArrayVector {
    private int mSize;
    private int mShift;
    public ShiftComplexVector(int aSize, int aShift, double[][] aData) {super(aData); mSize = aSize; mShift = aShift;}
    public ShiftComplexVector(int aShift, double[][] aData) {this(aData.length-aShift, aShift, aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public int shift() {return mShift;}
    public ShiftComplexVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, dataLength()-mShift, aSize); return this;}
    public ShiftComplexVector setShift(int aShift) {mShift = MathEX.Code.toRange(0, dataLength()-mSize, aShift); return this;}
    public int dataLength() {return Math.min(mData[0].length, mData[1].length);}
    
    /** IComplexVector stuffs */
    @Override public ComplexDouble get(int aIdx) {rangeCheck(aIdx, mSize); aIdx += mShift; return new ComplexDouble(mData[0][aIdx], mData[1][aIdx]);}
    @Override public double getReal(int aIdx) {rangeCheck(aIdx, mSize); return mData[0][aIdx+mShift];}
    @Override public double getImag(int aIdx) {rangeCheck(aIdx, mSize); return mData[1][aIdx+mShift];}
    @Override public void set(int aIdx, IComplexDouble aValue) {rangeCheck(aIdx, mSize); aIdx += mShift; mData[0][aIdx] = aValue.real(); mData[1][aIdx] = aValue.imag();}
    @Override public void set(int aIdx, ComplexDouble aValue) {rangeCheck(aIdx, mSize); aIdx += mShift; mData[0][aIdx] = aValue.mReal; mData[1][aIdx] = aValue.mImag;}
    @Override public void set(int aIdx, double aValue) {rangeCheck(aIdx, mSize); aIdx += mShift; mData[0][aIdx] = aValue; mData[1][aIdx] = 0.0;}
    @Override public void setReal(int aIdx, double aReal) {rangeCheck(aIdx, mSize); mData[0][aIdx+mShift] = aReal;}
    @Override public void setImag(int aIdx, double aImag) {rangeCheck(aIdx, mSize); mData[1][aIdx+mShift] = aImag;}
    @Override public ComplexDouble getAndSet(int aIdx, IComplexDouble aValue) {rangeCheck(aIdx, mSize); aIdx += mShift; ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]); mData[0][aIdx] = aValue.real(); mData[1][aIdx] = aValue.imag(); return oValue;}
    @Override public ComplexDouble getAndSet(int aIdx, ComplexDouble aValue) {rangeCheck(aIdx, mSize); aIdx += mShift; ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]); mData[0][aIdx] = aValue.mReal; mData[1][aIdx] = aValue.mImag; return oValue;}
    @Override public ComplexDouble getAndSet(int aIdx, double aValue) {rangeCheck(aIdx, mSize); aIdx += mShift; ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]); mData[0][aIdx] = aValue; mData[1][aIdx] = 0.0; return oValue;}
    @Override public double getAndSetReal(int aIdx, double aReal) {rangeCheck(aIdx, mSize); aIdx += mShift; double oReal = mData[0][aIdx]; mData[0][aIdx] = aReal; return oReal;}
    @Override public double getAndSetImag(int aIdx, double aImag) {rangeCheck(aIdx, mSize); aIdx += mShift; double oImag = mData[1][aIdx]; mData[1][aIdx] = aImag; return oImag;}
    @Override public int size() {return mSize;}
    
    @Override protected ComplexVector newZeros_(int aSize) {return ComplexVector.zeros(aSize);}
    @Override public ComplexVector copy() {
        ComplexVector rVector = ComplexVector.zeros(mSize);
        rVector.fill(this);
        return rVector;
    }
    
    @Override public ShiftComplexVector newShell() {return new ShiftComplexVector(mSize, mShift, null);}
    @Override public double @Nullable[][] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ComplexVector) return ((ComplexVector)aObj).mData;
        if (aObj instanceof ShiftComplexVector) return ((ShiftComplexVector)aObj).mData;
        if (aObj instanceof ComplexDoubleList) return ((ComplexDoubleList)aObj).internalData();
        if (aObj instanceof double[][]) {
            double[][] tData = (double[][])aObj;
            if (tData.length==2 && tData[0]!=null && tData[1]!=null) return tData;
        }
        return null;
    }
    /** 需要指定平移的距离保证优化运算的正确性 */
    @Override public int internalDataShift() {return shift();}
    
    
    /** Optimize stuffs，real()，imag() 直接返回 {@link ShiftVector} */
    @Override public ShiftVector real() {return new ShiftVector(mSize, mShift, mData[0]);}
    @Override public ShiftVector imag() {return new ShiftVector(mSize, mShift, mData[1]);}
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftComplexVector} */
    @Override public ShiftComplexVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return new ShiftComplexVector(aToIdx-aFromIdx, aFromIdx+mShift, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void swap(int aIdx1, int aIdx2) {
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
    
    @Override public void add(int aIdx, IComplexDouble aDelta) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[0][aIdx] += aDelta.real();
        mData[1][aIdx] += aDelta.imag();
    }
    @Override public void add(int aIdx, ComplexDouble aDelta) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        mData[0][aIdx] += aDelta.mReal;
        mData[1][aIdx] += aDelta.mImag;
    }
    @Override public void add(int aIdx, double aDelta) {rangeCheck(aIdx, mSize); mData[0][aIdx+mShift] += aDelta;}
    @Override public void addImag(int aIdx, double aImag) {rangeCheck(aIdx, mSize); mData[1][aIdx+mShift] += aImag;}
    @Override public void update(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealData[aIdx], tImagData[aIdx]));
        tRealData[aIdx] = tValue.real();
        tImagData[aIdx] = tValue.imag();
    }
    @Override public void updateReal(int aIdx, DoubleUnaryOperator aRealOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tRealData = mData[0];
        tRealData[aIdx] = aRealOpt.applyAsDouble(tRealData[aIdx]);
    }
    @Override public void updateImag(int aIdx, DoubleUnaryOperator aImagOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tImagData = mData[1];
        tImagData[aIdx] = aImagOpt.applyAsDouble(tImagData[aIdx]);
    }
    @Override public ComplexDouble getAndUpdate(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
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
    @Override public double getAndUpdateReal(int aIdx, DoubleUnaryOperator aRealOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tRealData = mData[0];
        double oReal = tRealData[aIdx];
        tRealData[aIdx] = aRealOpt.applyAsDouble(oReal);
        return oReal;
    }
    @Override public double getAndUpdateImag(int aIdx, DoubleUnaryOperator aImagOpt) {
        rangeCheck(aIdx, mSize);
        aIdx += mShift;
        final double[] tImagData = mData[1];
        double oImag = tImagData[aIdx];
        tImagData[aIdx] = aImagOpt.applyAsDouble(oImag);
        return oImag;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public ComplexDouble last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty ComplexVector");
        int tLastIdx = mSize-1+mShift;
        return new ComplexDouble(mData[0][tLastIdx], mData[1][tLastIdx]);
    }
    @Override public ComplexDouble first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty ComplexVector");
        return new ComplexDouble(mData[0][mShift], mData[1][mShift]);
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IComplexDoubleIterator iterator() {
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
    @Override public IComplexDoubleSetIterator setIterator() {
        return new IComplexDoubleSetIterator() {
            private final int mEnd = mSize + mShift;
            private int mIdx = mShift, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mEnd;}
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
            @Override public void setComplexDouble(double aReal, double aImag) {
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
