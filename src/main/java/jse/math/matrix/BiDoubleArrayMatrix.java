package jse.math.matrix;

import jse.math.IDataShell;
import jse.math.vector.ComplexVector;
import jse.math.vector.IComplexVector;
import org.jetbrains.annotations.Nullable;

public abstract class BiDoubleArrayMatrix extends AbstractComplexMatrix implements IDataShell<double[][]> {
    protected double[][] mData;
    protected BiDoubleArrayMatrix(double[][] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setInternalData(double[][] aData) {mData = aData;}
    @Override public double[][] internalData() {return mData;}
    @Override public int internalDataSize() {return columnNumber()*rowNumber();}
    
    protected class BiDoubleArrayMatrixOperation_ extends BiDoubleArrayMatrixOperation {
        @Override protected BiDoubleArrayMatrix thisMatrix_() {return BiDoubleArrayMatrix.this;}
        @Override protected BiDoubleArrayMatrix newMatrix_(int aRowNum, int aColNum) {return newZeros_(aRowNum, aColNum);}
    }
    
    /** 矩阵运算实现 */
    @Override public IComplexMatrixOperation operation() {return new BiDoubleArrayMatrixOperation_();}
    
    /** 严谨起见重写此方法不允许子类修改 */
    @Override protected final IComplexVector newZerosVec_(int aSize) {return ComplexVector.zeros(aSize);}
    
    
    @Override public BiDoubleArrayMatrix copy() {return (BiDoubleArrayMatrix)super.copy();}
    
    /** stuff to override */
    protected abstract BiDoubleArrayMatrix newZeros_(int aRowNum, int aColNum);
    public abstract BiDoubleArrayMatrix newShell();
    public abstract double @Nullable[][] getIfHasSameOrderData(Object aObj);
}

