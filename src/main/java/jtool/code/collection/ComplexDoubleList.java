package jtool.code.collection;

import jtool.code.functional.IDoubleBinaryConsumer;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.IDataShell;
import jtool.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static jtool.code.collection.DoubleList.rangeCheck;

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
    
    /** 在这里这个方法是必要的，虽然目前的约定下不会出现 {@code double aReal, double aImag} 两个参数的方法 */
    public void add(double aReal, double aImag) {
        int tLen = mData[0].length;
        if (tLen == 0) {
            mData = new double[2][1];
        } else
        if (tLen <= mSize) {
            double[][] oData = mData;
            mData = new double[2][tLen * 2];
            System.arraycopy(oData[0], 0, mData[0], 0, tLen);
            System.arraycopy(oData[1], 0, mData[1], 0, tLen);
        }
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
    @ApiStatus.Experimental
    public @Unmodifiable List<ComplexDouble> asConstList() {
        return new AbstractRandomAccessList<ComplexDouble>() {
            @Override public ComplexDouble get(int index) {return ComplexDoubleList.this.get(index);}
            @Override public int size() {return mSize;}
            @Override public boolean add(ComplexDouble element) {ComplexDoubleList.this.add(element); return true;}
        };
    }
    public IComplexVector asVec() {
        return new RefComplexVector() {
            @Override public double getReal(int aIdx) {return ComplexDoubleList.this.getReal(aIdx);}
            @Override public double getImag(int aIdx) {return ComplexDoubleList.this.getImag(aIdx);}
            @Override public void set(int aIdx, double aReal, double aImag) {ComplexDoubleList.this.set(aIdx, aReal, aImag);}
            @Override public void setReal(int aIdx, double aReal) {ComplexDoubleList.this.setReal(aIdx, aReal);}
            @Override public void setImag(int aIdx, double aImag) {ComplexDoubleList.this.setImag(aIdx, aImag);}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public IComplexVector asConstVec() {
        return new RefComplexVector() {
            @Override public double getReal(int aIdx) {return ComplexDoubleList.this.getReal(aIdx);}
            @Override public double getImag(int aIdx) {return ComplexDoubleList.this.getImag(aIdx);}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public ComplexVector copy2vec() {
        ComplexVector rVector = ComplexVector.zeros(mSize);
        double[][] rData = rVector.internalData();
        int tShift = rVector.internalDataShift();
        System.arraycopy(mData[0], 0, rData[0], tShift, mSize);
        System.arraycopy(mData[1], 0, rData[1], tShift, mSize);
        return rVector;
    }
    
    
    /** IDataShell stuffs */
    @Override  public int internalDataSize() {return size();}
    @Override public void setInternalData(double[][] aData) {mData = aData;}
    @Override public ComplexDoubleList newShell() {return new ComplexDoubleList(mSize, null);}
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
}
