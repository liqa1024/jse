package jse.code.collection;

import jse.code.functional.IDoubleBinaryConsumer;
import jse.code.iterator.IComplexDoubleIterator;
import jse.code.iterator.IDoubleIterator;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.IDataShell;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static jse.code.collection.DoubleList.rangeCheck;

/**
 * 通用的使用 {@code double[][]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class ComplexDoubleList implements IDataShell<double[][]> {
    public final static double[][] ZL_BIDOU = new double[2][0];
    
    protected double[][] mData;
    protected int mSize = 0;
    private ComplexDoubleList(int aSize, double[][] aData) {mSize = aSize; mData = aData;}
    public ComplexDoubleList() {mData = ZL_BIDOU; mSize = 0;}
    public ComplexDoubleList(int aInitSize) {mData = new double[2][aInitSize];}
    
    public double getReal(int aIdx) {rangeCheck(aIdx, mSize); return mData[0][aIdx];}
    public double getImag(int aIdx) {rangeCheck(aIdx, mSize); return mData[1][aIdx];}
    public ComplexDouble get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return new ComplexDouble(mData[0][aIdx], mData[1][aIdx]);
    }
    public void set(int aIdx, double aReal, double aImag) {rangeCheck(aIdx, mSize); mData[0][aIdx] = aReal; mData[1][aIdx] = aImag;}
    public void setReal(int aIdx, double aReal) {rangeCheck(aIdx, mSize); mData[0][aIdx] = aReal;}
    public void setImag(int aIdx, double aImag) {rangeCheck(aIdx, mSize); mData[1][aIdx] = aImag;}
    public void set(int aIdx, IComplexDouble aValue) {
        rangeCheck(aIdx, mSize);
        mData[0][aIdx] = aValue.real();
        mData[1][aIdx] = aValue.imag();
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public ComplexDouble last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty ComplexDoubleList");
        int tSizeMM = mSize-1;
        return new ComplexDouble(mData[0][tSizeMM], mData[1][tSizeMM]);
    }
    public ComplexDouble first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty ComplexDoubleList");
        return new ComplexDouble(mData[0][0], mData[1][0]);
    }
    
    private void grow_(int aMinCapacity) {
        final int tLen = mData[0].length;
        double[][] oData = mData;
        int tCapacity = Math.max(aMinCapacity, tLen + (tLen>>1));
        mData = new double[2][tCapacity];
        System.arraycopy(oData[0], 0, mData[0], 0, mSize);
        System.arraycopy(oData[1], 0, mData[1], 0, mSize);
    }
    
    /** 高性能接口，在末尾直接增加 aLen 个零，这将只进行扩容操作而不会赋值 */
    @ApiStatus.Experimental
    public void addZeros(int aLen) {
        int tSize = mSize+aLen;
        if (tSize > mData[0].length) {
            grow_(tSize);
        } else {
            final double[] tReal = mData[0];
            final double[] tImag = mData[1];
            for (int i = mSize; i<tSize; ++i) {
                tReal[i] = 0.0;
                tImag[i] = 0.0;
            }
        }
        mSize = tSize;
    }
    public void ensureCapacity(int aMinCapacity) {
        if (aMinCapacity > mData[0].length) grow_(aMinCapacity);
    }
    
    public void addAll(IComplexVector aVector) {
        final int aSize = aVector.size();
        final int tSize = mSize+aSize;
        if (tSize > mData[0].length) grow_(tSize);
        double @Nullable[][] aData = getIfHasSameOrderData(aVector);
        if (aData != null) {
            final int tShift = IDataShell.internalDataShift(aVector);
            System.arraycopy(aData[0], tShift, mData[0], mSize, aSize);
            System.arraycopy(aData[1], tShift, mData[1], mSize, aSize);
        } else {
            final double[] tReal = mData[0];
            final double[] tImag = mData[1];
            IComplexDoubleIterator it = aVector.iterator();
            for (int i = mSize; i < tSize; ++i) {
                it.nextOnly();
                tReal[i] = it.real();
                tImag[i] = it.imag();
            }
        }
        mSize = tSize;
    }
    public void addAll(IVector aVector) {
        final int aSize = aVector.size();
        final int tSize = mSize+aSize;
        if (tSize > mData[0].length) grow_(tSize);
        final double[] tReal = mData[0];
        final double[] tImag = mData[1];
        IDoubleIterator it = aVector.iterator();
        for (int i = mSize; i < tSize; ++i) {
            tReal[i] = it.next();
            tImag[i] = 0.0; // 虽然说理论不用设置，但是保证 setInternalData 后也能正常这里也同样设置
        }
        mSize = tSize;
    }
    public void addAll(int aSize, IComplexVectorGetter aVectorGetter) {
        final int tSize = mSize+aSize;
        if (tSize > mData[0].length) grow_(tSize);
        final double[] tReal = mData[0];
        final double[] tImag = mData[1];
        for (int i = mSize, j = 0; i < tSize; ++i, ++j) {
            IComplexDouble tValue = aVectorGetter.get(j);
            tReal[i] = tValue.real();
            tImag[i] = tValue.imag();
        }
        mSize = tSize;
    }
    public void addAll(int aSize, IVectorGetter aVectorGetter) {
        final int tSize = mSize+aSize;
        if (tSize > mData[0].length) grow_(tSize);
        final double[] tReal = mData[0];
        final double[] tImag = mData[1];
        for (int i = mSize, j = 0; i < tSize; ++i, ++j) {
            tReal[i] = aVectorGetter.get(j);
            tImag[i] = 0.0; // 虽然说理论不用设置，但是保证 setInternalData 后也能正常这里也同样设置
        }
        mSize = tSize;
    }
    
    /** 在这里这个方法是必要的，虽然目前的约定下不会出现 {@code double aReal, double aImag} 两个参数的方法 */
    public void add(double aReal, double aImag) {
        if (mData[0].length <= mSize) grow_(mSize+1);
        mData[0][mSize] = aReal;
        mData[1][mSize] = aImag;
        ++mSize;
    }
    public void add(IComplexDouble aValue) {add(aValue.real(), aValue.imag());}
    public void add(double aValue) {add(aValue, 0.0);}
    public void trimToSize() {
        if (mData[0].length != mSize) {
            double[][] oData = mData;
            mData = new double[2][mSize];
            System.arraycopy(oData[0], 0, mData[0], 0, mSize);
            System.arraycopy(oData[1], 0, mData[1], 0, mSize);
        }
    }
    public void clear() {
        mSize = 0;
    }
    public void forEach(Consumer<? super ComplexDouble> aCon) {
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        for (int i = 0; i < mSize; ++i) {
            aCon.accept(new ComplexDouble(tRealData[i], tImagData[i]));
        }
    }
    public void forEach(IDoubleBinaryConsumer aCon) {
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        for (int i = 0; i < mSize; ++i) {
            aCon.accept(tRealData[i], tImagData[i]);
        }
    }
    
    public List<ComplexDouble> asList() {
        return new AbstractRandomAccessList<ComplexDouble>() {
            @Override public ComplexDouble get(int index) {return ComplexDoubleList.this.get(index);}
            @Override public ComplexDouble set(int index, ComplexDouble element) {ComplexDouble oValue = ComplexDoubleList.this.get(index); ComplexDoubleList.this.set(index, element); return oValue;}
            @Override public int size() {return mSize;}
            @Override public boolean add(ComplexDouble element) {ComplexDoubleList.this.add(element); return true;}
        };
    }
    public ComplexVector asVec() {
        return new ComplexVector(mSize, mData);
    }
    public ComplexVector copy2vec() {
        ComplexVector rVector = ComplexVector.zeros(mSize);
        double[][] rData = rVector.internalData();
        int tShift = rVector.internalDataShift();
        System.arraycopy(mData[0], 0, rData[0], tShift, mSize);
        System.arraycopy(mData[1], 0, rData[1], tShift, mSize);
        return rVector;
    }
    @ApiStatus.Experimental
    public ComplexDoubleList copy() {
        double[][] tData = new double[2][mSize];
        System.arraycopy(mData[0], 0, tData[0], 0, mSize);
        System.arraycopy(mData[1], 0, tData[1], 0, mSize);
        return new ComplexDoubleList(mSize, tData);
    }
    
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof ComplexDoubleList)) return false;
        
        ComplexDoubleList tList = (ComplexDoubleList)aRHS;
        final int tSize = mSize;
        if (tSize != tList.mSize) return false;
        final double[] lDataReal = mData[0], lDataImag = mData[1];
        final double[] rDataReal = tList.mData[0], rDataImag = tList.mData[1];
        for (int i = 0; i < tSize; ++i) {
            if (Double.compare(lDataReal[i], rDataReal[i])!=0 || Double.compare(lDataImag[i], rDataImag[i])!=0) return false;
        }
        return true;
    }
    @Override public final int hashCode() {
        final int tSize = mSize;
        final double[] lDataReal = mData[0], lDataImag = mData[1];
        int rHashCode = 1;
        for (int i = 0; i < tSize; ++i) {
            rHashCode = 31*rHashCode + ComplexDouble.hashCode(lDataReal[i], lDataImag[i]);
        }
        return rHashCode;
    }
    
    
    /** IDataShell stuffs */
    @Override public int internalDataSize() {return size();}
    @Override public void setInternalData(double[][] aData) {mData = aData;}
    @Override public void setInternalDataSize(int aSize) {mSize = aSize;}
    
    @Override public double[][] internalData() {return mData;}
    @ApiStatus.Internal @Override public double @Nullable[][] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof ComplexDoubleList) return ((ComplexDoubleList)aObj).mData;
        if (aObj instanceof ComplexVector) return ((ComplexVector)aObj).internalData();
        if (aObj instanceof ShiftComplexVector) return ((ShiftComplexVector)aObj).internalData();
        if (aObj instanceof double[][]) {
            double[][] tData = (double[][])aObj;
            if (tData.length==2 && tData[0]!=null && tData[1]!=null) return tData;
        }
        return null;
    }
    
    /** Groovy stuffs */
    public ComplexDoubleList append(IComplexDouble aValue) {add(aValue); return this;}
    public ComplexDoubleList appendAll(IComplexVector aVector) {addAll(aVector); return this;}
    @VisibleForTesting public ComplexDoubleList leftShift(IComplexDouble aValue) {return append(aValue);}
    @VisibleForTesting public ComplexDoubleList leftShift(IComplexVector aVector) {return appendAll(aVector);}
    @VisibleForTesting public final ComplexDouble getAt(int aIdx) {return get(aIdx);}
    @VisibleForTesting public final void putAt(int aIdx, IComplexDouble aValue) {set(aIdx, aValue);}
}
