package com.jtool.math.vector;

import com.jtool.code.functional.*;
import com.jtool.code.iterator.*;
import com.jtool.math.ComplexDouble;
import com.jtool.math.IComplexDouble;
import groovy.lang.Closure;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.function.Supplier;

/**
 * @author liqa
 * <p> 向量的一般实现 </p>
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
    public static class Builder {
        private final static int INIT_SIZE = 8;
        private double[][] mData = new double[2][INIT_SIZE];
        private int mSize = 0;
        private Builder() {}
        
        /** 在这里这个方法是必要的，虽然目前的约定下不会出现 {@code double aReal, double aImag} 两个参数的方法 */
        public void add(double aReal, double aImag) {
            if (mData.length <= mSize) {
                double[][] oData = mData;
                mData = new double[2][oData.length * 2];
                System.arraycopy(oData[0], 0, mData[0], 0, oData.length);
                System.arraycopy(oData[1], 0, mData[1], 0, oData.length);
            }
            mData[0][mSize] = aReal;
            mData[1][mSize] = aImag;
            ++mSize;
        }
        public void add(IComplexDouble aValue) {add(aValue.real(), aValue.imag());}
        public void add(double aValue) {add(aValue, 0.0);}
        public ComplexVector build() {
            return new ComplexVector(mSize, mData);
        }
        public void shrinkToFit() {
            if (mData[0].length != mSize) {
                double[][] oData = mData;
                mData = new double[2][mSize];
                System.arraycopy(oData[0], 0, mData[0], 0, mSize);
                System.arraycopy(oData[1], 0, mData[1], 0, mSize);
            }
        }
    }
    
    
    private final int mSize;
    public ComplexVector(int aSize, double[][] aData) {super(aData); mSize = aSize;}
    public ComplexVector(double[][] aData) {this(aData[0].length, aData);}
    
    /** IComplexVector stuffs */
    @Override public double getReal_(int aIdx) {return mData[0][aIdx];}
    @Override public double getImag_(int aIdx) {return mData[1][aIdx];}
    @Override public void setReal_(int aIdx, double aReal) {mData[0][aIdx] = aReal;}
    @Override public void setImag_(int aIdx, double aImag) {mData[1][aIdx] = aImag;}
    @Override public double getAndSetReal_(int aIdx, double aReal) {double oReal = mData[0][aIdx]; mData[0][aIdx] = aReal; return oReal;}
    @Override public double getAndSetImag_(int aIdx, double aImag) {double oImag = mData[1][aIdx]; mData[1][aIdx] = aImag; return oImag;}
    @Override public int size() {return mSize;}
    
    @Override public ComplexVector newZeros(int aSize) {return ComplexVector.zeros(aSize);}
    
    @Override public ComplexVector newShell() {return new ComplexVector(mSize, null);}
    @Override public double @Nullable[][] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ComplexVector) return ((ComplexVector)aObj).mData;
        if (aObj instanceof double[][]) {
            double[][] tData = (double[][])aObj;
            if (tData.length==2 && tData[0]!=null && tData[1]!=null) return tData;
        }
        return null;
    }
    
    /** Optimize stuffs，real()，imag() 直接返回 {@link Vector} */
    @Override public IVector real() {return new Vector(mSize, mData[0]);}
    @Override public IVector imag() {return new Vector(mSize, mData[1]);}
    
    
//    /** Optimize stuffs，重写加速遍历 */
//    @Override public IComplexVectorOperation operation() {
//        return new BiDoubleArrayVectorOperation_() {
//            @Override public void fill(IComplexVectorGetter aRHS) {
//                final double[] tRealData = mData[0];
//                final double[] tImagData = mData[1];
//                for (int i = 0; i < mSize; ++i) {
//                    IComplexDouble tValue = aRHS.get(i);
//                    tRealData[i] = tValue.real();
//                    tImagData[i] = tValue.imag();
//                }
//            }
//            @Override public void fill(IVectorGetter aRHS) {
//                final double[] tRealData = mData[0];
//                final double[] tImagData = mData[1];
//                for (int i = 0; i < mSize; ++i) {
//                    tRealData[i] = aRHS.get(i);
//                    tImagData[i] = 0.0;
//                }
//            }
//            @Override public void assign(Supplier<? extends IComplexDouble> aSup) {
//                final double[] tRealData = mData[0];
//                final double[] tImagData = mData[1];
//                for (int i = 0; i < mSize; ++i) {
//                    IComplexDouble tValue = aSup.get();
//                    tRealData[i] = tValue.real();
//                    tImagData[i] = tValue.imag();
//                }
//            }
//            @Override public void assign(IDoubleSupplier aSup) {
//                final double[] tRealData = mData[0];
//                final double[] tImagData = mData[1];
//                for (int i = 0; i < mSize; ++i) {
//                    tRealData[i] = aSup.get();
//                    tImagData[i] = 0.0;
//                }
//            }
//            @Override public void forEach(IConsumer1<? super ComplexDouble> aCon) {
//                final double[] tRealData = mData[0];
//                final double[] tImagData = mData[1];
//                for (int i = 0; i < mSize; ++i) {
//                    aCon.run(new ComplexDouble(tRealData[i], tImagData[i]));
//                }
//            }
//            @Override public void forEach(IDoubleConsumer2 aCon) {
//                final double[] tRealData = mData[0];
//                final double[] tImagData = mData[1];
//                for (int i = 0; i < mSize; ++i) {
//                    aCon.run(tRealData[i], tImagData[i]);
//                }
//            }
//            /** Groovy stuffs */
//            @Override public void fill(Closure<?> aGroovyTask) {
//                final double[] tRealData = mData[0];
//                final double[] tImagData = mData[1];
//                for (int i = 0; i < mSize; ++i) {
//                    // 直接先执行然后检测类型决定如何设置
//                    Object tObj = aGroovyTask.call(i);
//                    if (tObj instanceof IComplexDouble) {
//                        IComplexDouble tValue = (IComplexDouble)tObj;
//                        tRealData[i] = tValue.real();
//                        tImagData[i] = tValue.imag();
//                    } else
//                    if (tObj instanceof Number) {
//                        tRealData[i] = ((Number)tObj).doubleValue();
//                        tImagData[i] = 0.0;
//                    }
//                }
//            }
//            @Override public void assign(Closure<?> aGroovyTask) {
//                final double[] tRealData = mData[0];
//                final double[] tImagData = mData[1];
//                for (int i = 0; i < mSize; ++i) {
//                    // 直接先执行然后检测类型决定如何设置
//                    Object tObj = aGroovyTask.call();
//                    if (tObj instanceof IComplexDouble) {
//                        IComplexDouble tValue = (IComplexDouble)tObj;
//                        tRealData[i] = tValue.real();
//                        tImagData[i] = tValue.imag();
//                    } else
//                    if (tObj instanceof Number) {
//                        tRealData[i] = ((Number)tObj).doubleValue();
//                        tImagData[i] = 0.0;
//                    }
//                }
//            }
//        };
//    }
    
    /** Optimize stuffs，重写加速这些操作 */
    @Override public void add_(int aIdx, IComplexDouble aDelta) {
        mData[0][aIdx] += aDelta.real();
        mData[1][aIdx] += aDelta.imag();
    }
    @Override public void add_(int aIdx, double aDelta) {mData[0][aIdx] += aDelta;}
    @Override public void addImag_(int aIdx, double aImag) {mData[1][aIdx] += aImag;}
    @Override public void update_(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        IComplexDouble tValue = aOpt.cal(new ComplexDouble(tRealData[aIdx], tImagData[aIdx]));
        tRealData[aIdx] = tValue.real();
        tImagData[aIdx] = tValue.imag();
    }
    @Override public void updateReal_(int aIdx, IDoubleOperator1 aRealOpt) {
        final double[] tRealData = mData[0];
        tRealData[aIdx] = aRealOpt.cal(tRealData[aIdx]);
    }
    @Override public void updateImag_(int aIdx, IDoubleOperator1 aImagOpt) {
        final double[] tImagData = mData[1];
        tImagData[aIdx] = aImagOpt.cal(tImagData[aIdx]);
    }
    @Override public ComplexDouble getAndUpdate_(int aIdx, IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        ComplexDouble oValue = new ComplexDouble(tRealData[aIdx], tImagData[aIdx]);
        IComplexDouble tValue = aOpt.cal(new ComplexDouble(oValue)); // 用来防止意外的修改
        tRealData[aIdx] = tValue.real();
        tImagData[aIdx] = tValue.imag();
        return oValue;
    }
    @Override public double getAndUpdateReal_(int aIdx, IDoubleOperator1 aRealOpt) {
        final double[] tRealData = mData[0];
        double oReal = tRealData[aIdx];
        tRealData[aIdx] = aRealOpt.cal(oReal);
        return oReal;
    }
    @Override public double getAndUpdateImag_(int aIdx, IDoubleOperator1 aImagOpt) {
        final double[] tImagData = mData[1];
        double oImag = tImagData[aIdx];
        tImagData[aIdx] = aImagOpt.cal(oImag);
        return oImag;
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
            @Override public void set(double aValue) {
                if (oIdx < 0) throw new IllegalStateException();
                mData[0][oIdx] = aValue;
                mData[1][oIdx] = 0.0;
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
