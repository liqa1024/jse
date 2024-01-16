package jtool.code.collection;

import jtool.code.functional.IDoubleConsumer1;
import jtool.math.IDataShell;
import jtool.math.vector.IVector;
import jtool.math.vector.RefVector;
import jtool.math.vector.ShiftVector;
import jtool.math.vector.Vector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.NoSuchElementException;

import static jtool.code.CS.ZL_VEC;

/**
 * 通用的使用 {@code double[]} 存储内部元素的 list，
 * 并且支持增长和随机访问
 */
public class DoubleList implements IDataShell<double[]> {
    protected double[] mData;
    protected int mSize = 0;
    private DoubleList(int aSize, double[] aData) {mSize = aSize; mData = aData;}
    public DoubleList() {mData = ZL_VEC; mSize = 0;}
    public DoubleList(int aInitSize) {mData = new double[aInitSize];}
    
    public double get(int aIdx) {
        if (aIdx >= mSize) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
        return mData[aIdx];
    }
    public void set(int aIdx, double aValue) {
        if (aIdx >= mSize) throw new IndexOutOfBoundsException(String.format("Index: %d", aIdx));
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
        if (mData.length == 0) {
            mData = new double[1];
        } else
        if (mData.length <= mSize) {
            double[] oData = mData;
            mData = new double[oData.length * 2];
            System.arraycopy(oData, 0, mData, 0, oData.length);
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
    public void forEach(IDoubleConsumer1 aCon) {
        for (int i = 0; i < mSize; ++i) aCon.run(mData[i]);
    }
    
    public List<Double> asList() {
        return new AbstractRandomAccessList<Double>() {
            @Override public Double get(int index) {return DoubleList.this.get(index);}
            @Override public Double set(int index, Double element) {double oValue = DoubleList.this.get(index); DoubleList.this.set(index, element); return oValue;}
            @Override public int size() {return DoubleList.this.size();}
            @Override public boolean add(Double element) {DoubleList.this.add(element); return true;}
        };
    }
    public IVector asVec() {
        return new RefVector() {
            @Override public double get_(int aIdx) {return mData[aIdx];}
            @Override public void set_(int aIdx, double aValue) {mData[aIdx] = aValue;}
            @Override public int size() {return mSize;}
        };
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
