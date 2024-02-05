package jse.code.collection;

import jse.math.IDataShell;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.DoubleConsumer;

import static jse.code.CS.ZL_VEC;

/**
 * 通用的使用 {@code double[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class DoubleList implements IDataShell<double[]> {
    static void rangeCheck(int aIdx, int aSize) {
        if (aIdx<0 || aIdx>=aSize) throw new IndexOutOfBoundsException("Index = " + aIdx + ", Size = " + aSize);
    }
    
    protected double[] mData;
    protected int mSize = 0;
    private DoubleList(int aSize, double[] aData) {mSize = aSize; mData = aData;}
    public DoubleList() {mData = ZL_VEC;}
    public DoubleList(int aInitSize) {mData = new double[aInitSize];}
    
    public double get(int aIdx) {
        rangeCheck(aIdx, mSize);
        return mData[aIdx];
    }
    public void set(int aIdx, double aValue) {
        rangeCheck(aIdx, mSize);
        mData[aIdx] = aValue;
    }
    public int size() {return mSize;}
    /** 用于方便访问 */
    public boolean isEmpty() {return mSize==0;}
    public double last() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access last() element from an empty DoubleList");
        return mData[mSize-1];
    }
    public double first() {
        if (isEmpty()) throw new NoSuchElementException("Cannot access first() element from an empty DoubleList");
        return mData[0];
    }
    
    public void add(double aValue) {
        final int tLen = mData.length;
        if (tLen == 0) {
            mData = new double[1];
        } else
        if (tLen <= mSize) {
            double[] oData = mData;
            mData = new double[tLen + Math.max(1, tLen>>1)];
            System.arraycopy(oData, 0, mData, 0, tLen);
        }
        mData[mSize] = aValue;
        ++mSize;
    }
    public void trimToSize() {
        if (mData.length != mSize) {
            double[] oData = mData;
            mData = new double[mSize];
            System.arraycopy(oData, 0, mData, 0, mSize);
        }
    }
    public void clear() {
        mSize = 0;
    }
    public void forEach(DoubleConsumer aCon) {
        for (int i = 0; i < mSize; ++i) aCon.accept(mData[i]);
    }
    
    public List<Double> asList() {
        return new AbstractRandomAccessList<Double>() {
            @Override public Double get(int index) {return DoubleList.this.get(index);}
            @Override public Double set(int index, Double element) {double oValue = DoubleList.this.get(index); DoubleList.this.set(index, element); return oValue;}
            @Override public int size() {return mSize;}
            @Override public boolean add(Double element) {DoubleList.this.add(element); return true;}
        };
    }
    @ApiStatus.Experimental
    public @Unmodifiable List<Double> asConstList() {
        return new AbstractRandomAccessList<Double>() {
            @Override public Double get(int index) {return DoubleList.this.get(index);}
            @Override public int size() {return DoubleList.this.size();}
        };
    }
    public IVector asVec() {
        return new RefVector() {
            @Override public double get(int aIdx) {return DoubleList.this.get(aIdx);}
            @Override public void set(int aIdx, double aValue) {DoubleList.this.set(aIdx, aValue);}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public IVector asConstVec() {
        return new RefVector() {
            @Override public double get(int aIdx) {return DoubleList.this.get(aIdx);}
            @Override public int size() {return mSize;}
        };
    }
    @ApiStatus.Experimental
    public Vector copy2vec() {
        Vector rVector = Vectors.zeros(mSize);
        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
        return rVector;
    }
    
    
    /** IDataShell stuffs */
    @Override  public int internalDataSize() {return size();}
    @Override public void setInternalData(double[] aData) {mData = aData;}
    @Override public DoubleList newShell() {return new DoubleList(mSize, null);}
    @Override public double[] internalData() {return mData;}
    @ApiStatus.Internal @Override public double @Nullable[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof DoubleList) return ((DoubleList)aObj).mData;
        if (aObj instanceof Vector) return ((Vector)aObj).internalData();
        if (aObj instanceof ShiftVector) return ((ShiftVector)aObj).internalData();
        if (aObj instanceof double[]) return (double[])aObj;
        return null;
    }
}
