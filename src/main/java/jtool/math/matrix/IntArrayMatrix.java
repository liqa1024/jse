package jtool.math.matrix;

import jtool.math.IDataShell;
import jtool.math.vector.IIntVector;
import jtool.math.vector.IntVector;
import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 内部存储 int[] 的矩阵，会加速相关的运算 </p>
 */
public abstract class IntArrayMatrix extends AbstractIntMatrix implements IDataShell<int[]> {
    protected int[] mData;
    protected IntArrayMatrix(int[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(int[] aData) {mData = aData;}
    @Override public int[] internalData() {return mData;}
    @Override public int internalDataSize() {return columnNumber()*rowNumber();}
    
    
    protected class IntArrayMatrixOperation_ extends IntArrayMatrixOperation {
        @Override protected IntArrayMatrix thisMatrix_() {return IntArrayMatrix.this;}
        @Override protected IntArrayMatrix newMatrix_(int aRowNum, int aColNum) {return newZeros_(aRowNum, aColNum);}
    }
    
    /** 严谨起见重写此方法不允许子类修改 */
    @Override protected final IIntVector newZerosVec_(int aSize) {return IntVector.zeros(aSize);}
    
    
    @Override public IntArrayMatrix copy() {return (IntArrayMatrix)super.copy();}
    
    /** stuff to override */
    protected abstract IntArrayMatrix newZeros_(int aRowNum, int aColNum);
    public abstract IntArrayMatrix newShell();
    public abstract int @Nullable[] getIfHasSameOrderData(Object aObj);
}
