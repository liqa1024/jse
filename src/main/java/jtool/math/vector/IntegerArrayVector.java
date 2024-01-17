package jtool.math.vector;

import jtool.math.IDataShell;
import org.jetbrains.annotations.Nullable;

public abstract class IntegerArrayVector extends AbstractIntegerVector implements IDataShell<int[]> {
    protected int[] mData;
    protected IntegerArrayVector(int[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(int[] aData) {mData = aData;}
    @Override public int[] internalData() {return mData;}
    @Override public int internalDataSize() {return size();}
    
    protected class IntegerArrayVectorOperation_ extends IntegerArrayVectorOperation {
        @Override protected IntegerArrayVector thisVector_() {return IntegerArrayVector.this;}
    }
    
    /** 向量运算实现 */
    @Override public IIntegerVectorOperation operation() {return new IntegerArrayVectorOperation_();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(int[] aData) {System.arraycopy(aData, 0, internalData(), internalDataShift(), internalDataSize());}
    
    /** stuff to override */
    public abstract IntegerArrayVector newShell();
    public abstract int @Nullable[] getIfHasSameOrderData(Object aObj);
}

