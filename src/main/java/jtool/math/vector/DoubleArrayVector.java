package jtool.math.vector;

import jtool.math.IDataShell;
import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 内部存储 double[] 的向量，会加速相关的运算 </p>
 */
public abstract class DoubleArrayVector extends AbstractVector implements IDataShell<double[]> {
    protected double[] mData;
    protected DoubleArrayVector(double[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(double[] aData) {mData = aData;}
    @Override public double[] internalData() {return mData;}
    @Override public int internalDataSize() {return size();}
    
    
    protected class DoubleArrayVectorOperation_ extends DoubleArrayVectorOperation {
        @Override protected DoubleArrayVector thisVector_() {return DoubleArrayVector.this;}
        @Override protected DoubleArrayVector newVector_(int aSize) {return newZeros_(aSize);}
    }
    
    /** 向量运算实现 */
    @Override public IVectorOperation operation() {return new DoubleArrayVectorOperation_();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(double[] aData) {
        if (isReverse()) {
            double[] rData = internalData();
            final int tShift = internalDataShift();
            final int tSize = internalDataSize();
            for (int i = 0, j = tShift+tSize-1; i < tSize; ++i, --j) {
                rData[j] = aData[i];
            }
        } else {
            System.arraycopy(aData, 0, internalData(), internalDataShift(), internalDataSize());
        }
    }
    
    /** Optimize stuffs，重写这些接口来加速获取 data 的过程 */
    @Override public double[] data() {
        final int tSize = internalDataSize();
        double[] rData = new double[tSize];
        if (isReverse()) {
            double[] tData = internalData();
            final int tShift = internalDataShift();
            for (int i = 0, j = tShift+tSize-1; i < tSize; ++i, --j) {
                rData[i] = tData[j];
            }
        } else {
            System.arraycopy(internalData(), internalDataShift(), rData, 0, tSize);
        }
        return rData;
    }
    
    @Override public DoubleArrayVector copy() {return (DoubleArrayVector)super.copy();}
    
    /** stuff to override */
    protected abstract DoubleArrayVector newZeros_(int aSize);
    public abstract DoubleArrayVector newShell();
    public abstract double @Nullable[] getIfHasSameOrderData(Object aObj);
}
