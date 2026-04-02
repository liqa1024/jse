package jse.math.vector;

import jse.math.IDataShell;
import org.jetbrains.annotations.Nullable;

public abstract class FloatArrayVector extends AbstractFloatVector implements IDataShell<float[]> {
    protected float[] mData;
    protected FloatArrayVector(float[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(float[] aData) {mData = aData;}
    @Override public float[] internalData() {return mData;}
    @Override public int internalDataSize() {return size();}
    
    protected class FloatArrayVectorOperation_ extends FloatArrayVectorOperation {
        @Override protected FloatArrayVector thisVector_() {return FloatArrayVector.this;}
        @Override protected FloatArrayVector newVector_(int aSize) {return FloatArrayVector.this.newZeros_(aSize);}
    }
    
    /** 向量运算实现 */
    @Override public IFloatVectorOperation operation() {return new FloatArrayVectorOperation_();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(float[] aData) {
        System.arraycopy(aData, 0, internalData(), internalDataShift(), internalDataSize());
    }
    
    /** Optimize stuffs，重写这些接口来加速获取 data 的过程 */
    @Override public float[] data() {
        final int tSize = internalDataSize();
        float[] rData = new float[tSize];
        System.arraycopy(internalData(), internalDataShift(), rData, 0, tSize);
        return rData;
    }
    
    @Override public FloatArrayVector copy() {return (FloatArrayVector)super.copy();}
    
    /** stuff to override */
    @Override protected abstract FloatArrayVector newZeros_(int aSize);
    
    @Override public abstract float @Nullable[] getIfHasSameOrderData(Object aObj);
}

