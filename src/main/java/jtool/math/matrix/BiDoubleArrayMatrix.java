package jtool.math.matrix;

import jtool.math.IDataShell;
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
    }
    
    /** 矩阵运算实现 */
    @Override public IComplexMatrixOperation operation() {return new BiDoubleArrayMatrixOperation_();}
    
    
    /** stuff to override */
    public abstract BiDoubleArrayMatrix newShell();
    public abstract double @Nullable[][] getIfHasSameOrderData(Object aObj);
}

