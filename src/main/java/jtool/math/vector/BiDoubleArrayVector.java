package jtool.math.vector;

import jtool.math.IDataShell;
import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 内部存储 double[][] 的复向量，会加速相关的运算 </p>
 */
public abstract class BiDoubleArrayVector extends AbstractComplexVector implements IDataShell<double[][]> {
    protected double[][] mData;
    protected BiDoubleArrayVector(double[][] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(double[][] aData) {mData = aData;}
    @Override public double[][] internalData() {return mData;}
    @Override public int internalDataSize() {return size();}
    
    
    protected class BiDoubleArrayVectorOperation_ extends BiDoubleArrayVectorOperation {
        @Override protected BiDoubleArrayVector thisVector_() {return BiDoubleArrayVector.this;}
        @Override protected BiDoubleArrayVector newVector_(int aSize) {return newZeros_(aSize);}
    }

    /** 向量运算实现 */
    @Override public IComplexVectorOperation operation() {return new BiDoubleArrayVectorOperation_();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(double[][] aData) {
        double[][] rData = internalData();
        final int tShift = internalDataShift();
        final int tSize = internalDataSize();
        if (isReverse()) {
            double[] rRealData = rData[0];
            double[] rImagData = rData[1];
            double[] aRealData = aData[0];
            double[] aImagData = aData[1];
            for (int i = 0, j = tShift+tSize-1; i < tSize; ++i, --j) {
                rRealData[j] = aRealData[i];
                rImagData[j] = aImagData[i];
            }
        } else {
            System.arraycopy(aData[0], 0, rData[0], tShift, tSize);
            System.arraycopy(aData[1], 0, rData[1], tShift, tSize);
        }
    }
    @Override public void fill(double[] aData) {
        double[][] rData = internalData();
        final int tShift = internalDataShift();
        final int tSize = internalDataSize();
        if (isReverse()) {
            double[] rRealData = rData[0];
            for (int i = 0, j = tShift+tSize-1; i < tSize; ++i, --j) {
                rRealData[j] = aData[i];
            }
        } else {
            System.arraycopy(aData, 0, rData[0], tShift, tSize);
        }
        final int tEnd = tShift + tSize;
        final double[] tImagData = rData[1];
        for (int i = tShift; i < tEnd; ++i) tImagData[i] = 0.0;
    }
    
    /** Optimize stuffs，重写这些接口来加速获取 data 的过程 */
    @Override public double[][] data() {
        final int tShift = internalDataShift();
        final int tSize = internalDataSize();
        double[][] rData = new double[2][tSize];
        double[][] tData = internalData();
        if (isReverse()) {
            double[] rRealData = rData[0];
            double[] rImagData = rData[1];
            double[] tRealData = tData[0];
            double[] tImagData = tData[1];
            for (int i = 0, j = tShift+tSize-1; i < tSize; ++i, --j) {
                rRealData[i] = tRealData[j];
                rImagData[i] = tImagData[j];
            }
        } else {
            System.arraycopy(tData[0], tShift, rData[0], 0, tSize);
            System.arraycopy(tData[1], tShift, rData[1], 0, tSize);
        }
        return rData;
    }
    
    /** stuff to override */
    protected abstract BiDoubleArrayVector newZeros_(int aSize);
    public abstract BiDoubleArrayVector newShell();
    public abstract double @Nullable[][] getIfHasSameOrderData(Object aObj);
}
