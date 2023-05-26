package com.jtool.math.vector;

import com.jtool.math.IDataShell;

import java.util.Arrays;

/**
 * @author liqa
 * <p> 向量的一般实现 </p>
 */
public class RealVector extends AbstractVector<Double> implements IDataShell<RealVector, double[]> {
    /** 提供默认的创建 */
    public static RealVector ones(int aSize) {
        double[] tData = new double[aSize];
        Arrays.fill(tData, 1.0);
        return new RealVector(tData);
    }
    public static RealVector zeros(int aSize) {return new RealVector(new double[aSize]);}
    
    
    private double[] mData;
    private final int mSize;
    public RealVector(int aSize, double[] aData) {mSize = aSize; mData = aData;}
    public RealVector(double[] aData) {this(aData.length, aData);}
    
    /** DataShell stuffs */
    @Override public void setData2this(double[] aData) {mData = aData;}
    @Override public RealVector newShell() {return new RealVector(mSize, null);}
    @Override public double[] getData() {return mData;}
    @Override public double[] getIfHasSameOrderData(Object aObj) {
        if (aObj instanceof double[]) return (double[])aObj;
        if (aObj instanceof RealVector) return ((RealVector)aObj).mData;
        return null;
    }
    @Override public int dataSize() {return mSize;}
    
    /** IVector stuffs */
    @Override public Double get_(int aIdx) {return mData[aIdx];}
    @Override public void set_(int aIdx, Number aValue) {mData[aIdx] = aValue.doubleValue();}
    @Override public Double getAndSet_(int aIdx, Number aValue) {
        Double oValue = mData[aIdx];
        mData[aIdx] = aValue.doubleValue();
        return oValue;
    }
    @Override public int size() {return mSize;}
}
