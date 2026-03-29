package jse.math.matrix;

import jse.math.IDataShell;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

/**
 * 内部存储 double[] 的矩阵，会加速相关的运算
 * @author liqa
 */
public abstract class DoubleArrayMatrix extends AbstractMatrix implements IDataShell<double[]> {
    protected double[] mData;
    protected DoubleArrayMatrix(double[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(double[] aData) {mData = aData;}
    @Override public double[] internalData() {return mData;}
    @Override public int internalDataSize() {return ncols()* nrows();}
    
    
    protected class DoubleArrayMatrixOperation_ extends DoubleArrayMatrixOperation {
        @Override protected DoubleArrayMatrix thisMatrix_() {return DoubleArrayMatrix.this;}
        @Override protected DoubleArrayMatrix newMatrix_(int aRowNum, int aColNum) {return newZeros_(aRowNum, aColNum);}
    }
    
    /** 矩阵运算实现 */
    @Override public IMatrixOperation operation() {return new DoubleArrayMatrixOperation_();}
    
    /** 严谨起见重写此方法不允许子类修改 */
    @Override protected final IVector newZerosVec_(int aSize) {return Vector.zeros(aSize);}
    
    
    @Override public DoubleArrayMatrix copy() {return (DoubleArrayMatrix)super.copy();}
    
    /** stuff to override */
    @Override protected abstract DoubleArrayMatrix newZeros_(int aRowNum, int aColNum);
    
    @Override public abstract double @Nullable[] getIfHasSameOrderData(Object aObj);
}
