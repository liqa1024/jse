package jse.code.collection;

import jse.code.iterator.IDoubleIterator;
import jse.math.IDataShell;
import jse.math.vector.*;
import jse.math.vector.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.*;
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
    
    private void grow_(int aMinCapacity) {
        final int tLen = mData.length;
        double[] oData = mData;
        int tCapacity = Math.max(aMinCapacity, tLen + (tLen>>1));
        mData = new double[tCapacity];
        System.arraycopy(oData, 0, mData, 0, tLen);
    }
    
    /** 高性能接口，在末尾直接增加 aLen 个零，这将只进行扩容操作而不会赋值 */
    @ApiStatus.Experimental
    public void addZeros(int aLen) {
        int tSize = mSize+aLen;
        if (tSize > mData.length) grow_(tSize);
        mSize = tSize;
    }
    
    public void addAll(IVector aVector) {
        final int aSize = aVector.size();
        final int tSize = mSize+aSize;
        if (tSize > mData.length) grow_(tSize);
        double @Nullable[] aData = getIfHasSameOrderData(aVector);
        if (aData != null) {
            System.arraycopy(aData, IDataShell.internalDataShift(aVector), mData, mSize, aSize);
        } else {
            IDoubleIterator it = aVector.iterator();
            for (int i = mSize; i < tSize; ++i) mData[i] = it.next();
        }
        mSize = tSize;
    }
    public void addAll(int aSize, IVectorGetter aVectorGetter) {
        final int tSize = mSize+aSize;
        if (tSize > mData.length) grow_(tSize);
        for (int i = mSize, j = 0; i < tSize; ++i, ++j) mData[i] = aVectorGetter.get(j);
        mSize = tSize;
    }
    
    public void add(double aValue) {
        if (mData.length <= mSize) grow_(mSize+1);
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
    public Vector asVec() {
        return new Vector(mSize, mData);
    }
    public Vector copy2vec() {
        Vector rVector = Vectors.zeros(mSize);
        System.arraycopy(mData, 0, rVector.internalData(), rVector.internalDataShift(), mSize);
        return rVector;
    }
    @ApiStatus.Experimental
    public DoubleList copy() {
        double[] tData = new double[mSize];
        System.arraycopy(mData, 0, tData, 0, mSize);
        return new DoubleList(mSize, tData);
    }
    
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof DoubleList)) return false;
        
        DoubleList tList = (DoubleList)aRHS;
        final int tSize = mSize;
        if (tSize != tList.mSize) return false;
        final double[] lData = mData;
        final double[] rData = tList.mData;
        for (int i = 0; i < tSize; ++i) {
            if (Double.compare(lData[i], rData[i]) != 0) return false;
        }
        return true;
    }
    @Override public final int hashCode() {
        final int tSize = mSize;
        final double[] tData = mData;
        int rHashCode = 1;
        for (int i = 0; i < tSize; ++i) {
            rHashCode = 31*rHashCode + Double.hashCode(tData[i]);
        }
        return rHashCode;
    }
    
    /** IDataShell stuffs */
    @Override public int internalDataSize() {return size();}
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
    
    /** Groovy stuffs */
    public DoubleList append(double aValue) {add(aValue); return this;}
    public DoubleList appendAll(IVector aVector) {addAll(aVector); return this;}
    @VisibleForTesting public DoubleList leftShift(double aValue) {return append(aValue);}
    @VisibleForTesting public DoubleList leftShift(IVector aVector) {return appendAll(aVector);}
    @VisibleForTesting public final double getAt(int aIdx) {return get(aIdx);}
    @VisibleForTesting public final void putAt(int aIdx, double aValue) {set(aIdx, aValue);}
}
