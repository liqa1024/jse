package jtool.code.collection;

import jtool.code.functional.IConsumer1;
import jtool.code.functional.IDoubleConsumer2;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.IDataShell;
import jtool.math.vector.ComplexVector;
import jtool.math.vector.IComplexVector;
import jtool.math.vector.RefComplexVector;
import jtool.math.vector.ShiftComplexVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;

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
    
    public ComplexDouble get(int aIdx) {
        if (aIdx >= mSize) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return new ComplexDouble(mData[0][aIdx], mData[1][aIdx]);
    }
    public void set(int aIdx, IComplexDouble aValue) {
        if (aIdx >= mSize) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
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
    public void forEach(IConsumer1<? super ComplexDouble> aCon) {
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        for (int i = 0; i < mSize; ++i) {
            aCon.run(new ComplexDouble(tRealData[i], tImagData[i]));
        }
    }
    public void forEach(IDoubleConsumer2 aCon) {
        final double[] tRealData = mData[0];
        final double[] tImagData = mData[1];
        for (int i = 0; i < mSize; ++i) {
            aCon.run(tRealData[i], tImagData[i]);
        }
    }
    
    public List<ComplexDouble> asList() {
        return new AbstractRandomAccessList<ComplexDouble>() {
            @Override public ComplexDouble get(int index) {return ComplexDoubleList.this.get(index);}
            @Override public ComplexDouble set(int index, ComplexDouble element) {ComplexDouble oValue = ComplexDoubleList.this.get(index); ComplexDoubleList.this.set(index, element); return oValue;}
            @Override public int size() {return ComplexDoubleList.this.size();}
            @Override public boolean add(ComplexDouble element) {ComplexDoubleList.this.add(element); return true;}
        };
    }
    public IComplexVector asVec() {
        return new RefComplexVector() {
            @Override public double getReal_(int aIdx) {return mData[0][aIdx];}
            @Override public double getImag_(int aIdx) {return mData[1][aIdx];}
            @Override public void setReal_(int aIdx, double aReal) {mData[0][aIdx] = aReal;}
            @Override public void setImag_(int aIdx, double aImag) {mData[1][aIdx] = aImag;}
            @Override public int size() {return mSize;}
        };
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
