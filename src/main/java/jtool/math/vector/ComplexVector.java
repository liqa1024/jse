package jtool.math.vector;

import jtool.code.collection.ComplexDoubleList;
import jtool.code.functional.IUnaryFullOperator;
import jtool.code.iterator.IComplexDoubleIterator;
import jtool.code.iterator.IComplexDoubleSetIterator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.MathEX;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.DoubleUnaryOperator;

import static jtool.math.vector.AbstractVector.*;

/**
 * @author liqa
 * <p> 复向量的一般实现 </p>
 */
public final class ComplexVector extends BiDoubleArrayVector {
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
            return new ComplexVector(mSize, mData);
        }
    }
    
    
    private int mSize;
    public ComplexVector(int aSize, double[][] aData) {super(aData); mSize = aSize;}
    public ComplexVector(double[][] aData) {this(Math.min(aData[0].length, aData[1].length), aData);}
    
    /** 提供额外的接口来直接设置底层参数 */
    public ComplexVector setSize(int aSize) {mSize = MathEX.Code.toRange(0, dataLength(), aSize); return this;}
    public int dataLength() {return Math.min(mData[0].length, mData[1].length);}
    
    /** IComplexVector stuffs */
    @Override public ComplexDouble get(int aIdx) {rangeCheck(aIdx, mSize); return new ComplexDouble(mData[0][aIdx], mData[1][aIdx]);}
    @Override public double getReal(int aIdx) {rangeCheck(aIdx, mSize); return mData[0][aIdx];}
    @Override public double getImag(int aIdx) {rangeCheck(aIdx, mSize); return mData[1][aIdx];}
    @Override public void set(int aIdx, IComplexDouble aValue) {rangeCheck(aIdx, mSize); mData[0][aIdx] = aValue.real(); mData[1][aIdx] = aValue.imag();}
    @Override public void set(int aIdx, ComplexDouble aValue) {rangeCheck(aIdx, mSize); mData[0][aIdx] = aValue.mReal; mData[1][aIdx] = aValue.mImag;}
    @Override public void set(int aIdx, double aValue) {rangeCheck(aIdx, mSize); mData[0][aIdx] = aValue; mData[1][aIdx] = 0.0;}
    @Override public void set(int aIdx, double aReal, double aImag) {rangeCheck(aIdx, mSize); mData[0][aIdx] = aReal; mData[1][aIdx] = aImag;}
    @Override public void setReal(int aIdx, double aReal) {rangeCheck(aIdx, mSize); mData[0][aIdx] = aReal;}
    @Override public void setImag(int aIdx, double aImag) {rangeCheck(aIdx, mSize); mData[1][aIdx] = aImag;}
    @Override public ComplexDouble getAndSet(int aIdx, IComplexDouble aValue) {rangeCheck(aIdx, mSize); ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]); mData[0][aIdx] = aValue.real(); mData[1][aIdx] = aValue.imag(); return oValue;}
    @Override public ComplexDouble getAndSet(int aIdx, ComplexDouble aValue) {rangeCheck(aIdx, mSize); ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]); mData[0][aIdx] = aValue.mReal; mData[1][aIdx] = aValue.mImag; return oValue;}
    @Override public ComplexDouble getAndSet(int aIdx, double aValue) {rangeCheck(aIdx, mSize); ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]); mData[0][aIdx] = aValue; mData[1][aIdx] = 0.0; return oValue;}
    @Override public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {rangeCheck(aIdx, mSize); ComplexDouble oValue = new ComplexDouble(mData[0][aIdx], mData[1][aIdx]); mData[0][aIdx] = aReal; mData[1][aIdx] = aImag; return oValue;}
    @Override public double getAndSetReal(int aIdx, double aReal) {rangeCheck(aIdx, mSize); double oReal = mData[0][aIdx]; mData[0][aIdx] = aReal; return oReal;}
    @Override public double getAndSetImag(int aIdx, double aImag) {rangeCheck(aIdx, mSize); double oImag = mData[1][aIdx]; mData[1][aIdx] = aImag; return oImag;}
    @Override public int size() {return mSize;}
    
    @Override protected ComplexVector newZeros_(int aSize) {return ComplexVector.zeros(aSize);}
    @Override public ComplexVector copy() {
        ComplexVector rVector = ComplexVector.zeros(mSize);
        rVector.fill(this);
        return rVector;
    }
    
    @Override public ComplexVector newShell() {return new ComplexVector(mSize, null);}
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
    
    
    /** Optimize stuffs，real()，imag() 直接返回 {@link Vector} */
    @Override public Vector real() {return new Vector(mSize, mData[0]);}
    @Override public Vector imag() {return new Vector(mSize, mData[1]);}
    
    /** Optimize stuffs，subVec 切片直接返回  {@link ShiftComplexVector} */
    @Override public BiDoubleArrayVector subVec(final int aFromIdx, final int aToIdx) {
        subVecRangeCheck(aFromIdx, aToIdx, mSize);
        return aFromIdx==0 ? new ComplexVector(aToIdx, mData) : new ShiftComplexVector(aToIdx-aFromIdx, aFromIdx, mData);
    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void swap(int aIdx1, int aIdx2) {
        biRangeCheck(aIdx1, aIdx2, mSize);
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
        mData[0][aIdx] += aDelta.real();
        mData[1][aIdx] += aDelta.imag();
    }
    @Override public void add(int aIdx, ComplexDouble aDelta) {
        rangeCheck(aIdx, mSize);
        mData[0][aIdx] += aDelta.mReal;
        mData[1][aIdx] += aDelta.mImag;
    }
    @Override public void add(int aIdx, double aDelta) {rangeCheck(aIdx, mSize); mData[0][aIdx] += aDelta;}
    @Override public void addImag(int aIdx, double aImag) {rangeCheck(aIdx, mSize); mData[1][aIdx] += aImag;}
    @Override public void update(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheck(aIdx, mSize);
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        IComplexDouble tValue = aOpt.apply(new ComplexDouble(tRealData[aIdx], tImagData[aIdx]));
        tRealData[aIdx] = tValue.real();
        tImagData[aIdx] = tValue.imag();
    }
    @Override public void updateReal(int aIdx, DoubleUnaryOperator aRealOpt) {
        rangeCheck(aIdx, mSize);
        final double[] tRealData = mData[0];
        tRealData[aIdx] = aRealOpt.applyAsDouble(tRealData[aIdx]);
    }
    @Override public void updateImag(int aIdx, DoubleUnaryOperator aImagOpt) {
        rangeCheck(aIdx, mSize);
        final double[] tImagData = mData[1];
        tImagData[aIdx] = aImagOpt.applyAsDouble(tImagData[aIdx]);
    }
    @Override public ComplexDouble getAndUpdate(int aIdx, IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        rangeCheck(aIdx, mSize);
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
        final double[] tRealData = mData[0];
        double oReal = tRealData[aIdx];
        tRealData[aIdx] = aRealOpt.applyAsDouble(oReal);
        return oReal;
    }
    @Override public double getAndUpdateImag(int aIdx, DoubleUnaryOperator aImagOpt) {
        rangeCheck(aIdx, mSize);
        final double[] tImagData = mData[1];
        double oImag = tImagData[aIdx];
        tImagData[aIdx] = aImagOpt.applyAsDouble(oImag);
        return oImag;
    }
    @Override public boolean isEmpty() {return mSize==0;}
    @Override public ComplexDouble last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty ComplexVector");
        int tSizeMM = mSize-1;
        return new ComplexDouble(mData[0][tSizeMM], mData[1][tSizeMM]);
    }
    @Override public ComplexDouble first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty ComplexVector");
        return new ComplexDouble(mData[0][0], mData[1][0]);
    }
    
    /** Optimize stuffs，重写迭代器来提高遍历速度（主要是省去隐函数的调用，以及保持和矩阵相同的写法格式） */
    @Override public IComplexDoubleIterator iterator() {
        return new IComplexDoubleIterator() {
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
            private int mIdx = 0, oIdx = -1;
            @Override public boolean hasNext() {return mIdx < mSize;}
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
