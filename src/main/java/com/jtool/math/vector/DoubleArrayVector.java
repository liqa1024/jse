package com.jtool.math.vector;

import com.jtool.math.IDataShell;
import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 内部存储 double[] 的向量，会加速相关的运算 </p>
 */
public abstract class DoubleArrayVector extends AbstractVector implements IDataShell<double[]> {
    protected double[] mData;
    protected DoubleArrayVector(double[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setData2this(double[] aData) {mData = aData;}
    @Override public double[] getData() {return mData;}
    @Override public int dataSize() {return size();}
    
    
    protected class DoubleArrayVectorOperation_ extends DoubleArrayVectorOperation {
        @Override protected DoubleArrayVector thisVector_() {return DoubleArrayVector.this;}
        @Override protected DoubleArrayVector newVector_(int aSize) {return newZeros(aSize);}
    }
    
    /** 向量运算实现 */
    @Override public IVectorOperation operation() {return new DoubleArrayVectorOperation_();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(double[] aData) {System.arraycopy(aData, 0, getData(), shiftSize(), dataSize());}
    
    /** Optimize stuffs，重写这些接口来加速获取 data 的过程 */
    @Override public double[] data() {
        final int tSize = dataSize();
        double[] rData = new double[tSize];
        System.arraycopy(getData(), shiftSize(), rData, 0, tSize);
        return rData;
    }
    
    /** stuff to override */
    public abstract DoubleArrayVector newZeros(int aSize);
    public abstract DoubleArrayVector newShell();
    public abstract double @Nullable[] getIfHasSameOrderData(Object aObj);
}
