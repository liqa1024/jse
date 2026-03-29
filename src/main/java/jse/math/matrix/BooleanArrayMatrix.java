package jse.math.matrix;

import jse.math.IDataShell;
import jse.math.vector.ILogicalVector;
import jse.math.vector.LogicalVector;
import org.jetbrains.annotations.Nullable;

/**
 * 内部存储 boolean[] 的矩阵，会加速相关的运算
 * @author liqa
 */
public abstract class BooleanArrayMatrix extends AbstractLogicalMatrix implements IDataShell<boolean[]> {
    protected boolean[] mData;
    protected BooleanArrayMatrix(boolean[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(boolean[] aData) {mData = aData;}
    @Override public boolean[] internalData() {return mData;}
    @Override public int internalDataSize() {return ncols()* nrows();}
    
    
    protected class BooleanArrayMatrixOperation_ extends BooleanArrayMatrixOperation {
        @Override protected BooleanArrayMatrix thisMatrix_() {return BooleanArrayMatrix.this;}
        @Override protected BooleanArrayMatrix newMatrix_(int aRowNum, int aColNum) {return newZeros_(aRowNum, aColNum);}
    }
    
    /** 严谨起见重写此方法不允许子类修改 */
    @Override protected final ILogicalVector newZerosVec_(int aSize) {return LogicalVector.zeros(aSize);}
    
    
    @Override public BooleanArrayMatrix copy() {return (BooleanArrayMatrix)super.copy();}
    
    /** stuff to override */
    @Override protected abstract BooleanArrayMatrix newZeros_(int aRowNum, int aColNum);
    
    @Override public abstract boolean @Nullable[] getIfHasSameOrderData(Object aObj);
}
