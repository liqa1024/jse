package jtool.math.vector;

import jtool.math.IDataShell;
import org.jetbrains.annotations.Nullable;

public abstract class IntArrayVector extends AbstractIntVector implements IDataShell<int[]> {
    protected int[] mData;
    protected IntArrayVector(int[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(int[] aData) {mData = aData;}
    @Override public int[] internalData() {return mData;}
    @Override public int internalDataSize() {return size();}
    
    protected class IntArrayVectorOperation_ extends IntArrayVectorOperation {
        @Override protected IntArrayVector thisVector_() {return IntArrayVector.this;}
        @Override protected IntArrayVector newVector_(int aSize) {return IntArrayVector.this.newZeros_(aSize);}
    }
    
    /** 向量运算实现 */
    @Override public IIntVectorOperation operation() {return new IntArrayVectorOperation_();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(int[] aData) {
        if (isReverse()) {
            int[] rData = internalData();
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
    @Override public int[] data() {
        final int tSize = internalDataSize();
        int[] rData = new int[tSize];
        if (isReverse()) {
            int[] tData = internalData();
            final int tShift = internalDataShift();
            for (int i = 0, j = tShift+tSize-1; i < tSize; ++i, --j) {
                rData[i] = tData[j];
            }
        } else {
            System.arraycopy(internalData(), internalDataShift(), rData, 0, tSize);
        }
        return rData;
    }
    
    @Override public IntArrayVector copy() {return (IntArrayVector)super.copy();}
    
    /** stuff to override */
    protected abstract IntArrayVector newZeros_(int aSize);
    public abstract IntArrayVector newShell();
    public abstract int @Nullable[] getIfHasSameOrderData(Object aObj);
}

