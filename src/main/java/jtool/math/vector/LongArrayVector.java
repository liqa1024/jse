package jtool.math.vector;

import jtool.math.IDataShell;
import org.jetbrains.annotations.Nullable;

public abstract class LongArrayVector extends AbstractLongVector implements IDataShell<long[]> {
    protected long[] mData;
    protected LongArrayVector(long[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(long[] aData) {mData = aData;}
    @Override public long[] internalData() {return mData;}
    @Override public int internalDataSize() {return size();}
    
    protected class LongArrayVectorOperation_ extends LongArrayVectorOperation {
        @Override protected LongArrayVector thisVector_() {return LongArrayVector.this;}
        @Override protected LongArrayVector newVector_(int aSize) {return LongArrayVector.this.newZeros_(aSize);}
    }
    
    /** 向量运算实现 */
    @Override public ILongVectorOperation operation() {return new LongArrayVectorOperation_();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(long[] aData) {
        if (isReverse()) {
            long[] rData = internalData();
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
    @Override public long[] data() {
        final int tSize = internalDataSize();
        long[] rData = new long[tSize];
        if (isReverse()) {
            long[] tData = internalData();
            final int tShift = internalDataShift();
            for (int i = 0, j = tShift+tSize-1; i < tSize; ++i, --j) {
                rData[i] = tData[j];
            }
        } else {
            System.arraycopy(internalData(), internalDataShift(), rData, 0, tSize);
        }
        return rData;
    }
    
    @Override public LongArrayVector copy() {return (LongArrayVector)super.copy();}
    
    /** stuff to override */
    protected abstract LongArrayVector newZeros_(int aSize);
    public abstract LongArrayVector newShell();
    public abstract long @Nullable[] getIfHasSameOrderData(Object aObj);
}

