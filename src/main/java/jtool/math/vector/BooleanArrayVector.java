package jtool.math.vector;

import jtool.math.IDataShell;
import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 内部存储 boolean[] 的逻辑向量，会加速相关的运算 </p>
 */
public abstract class BooleanArrayVector extends AbstractLogicalVector implements IDataShell<boolean[]> {
    protected boolean[] mData;
    protected BooleanArrayVector(boolean[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(boolean[] aData) {mData = aData;}
    @Override public boolean[] internalData() {return mData;}
    @Override public int internalDataSize() {return size();}
    
    
    protected class BooleanArrayVectorOperation_ extends BooleanArrayVectorOperation {
        @Override protected BooleanArrayVector thisVector_() {return BooleanArrayVector.this;}
        @Override protected BooleanArrayVector newVector_(int aSize) {return newZeros_(aSize);}
    }
    
    /** 向量运算实现 */
    @Override public ILogicalVectorOperation operation() {return new BooleanArrayVectorOperation_();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(boolean[] aData) {System.arraycopy(aData, 0, internalData(), internalDataShift(), internalDataSize());}
    
    /** Optimize stuffs，重写这些接口来加速获取 data 的过程 */
    @Override public boolean[] data() {
        final int tSize = internalDataSize();
        boolean[] rData = new boolean[tSize];
        System.arraycopy(internalData(), internalDataShift(), rData, 0, tSize);
        return rData;
    }
    
    /** stuff to override */
    protected abstract BooleanArrayVector newZeros_(int aSize);
    public abstract BooleanArrayVector newShell();
    public abstract boolean @Nullable[] getIfHasSameOrderData(Object aObj);
}
