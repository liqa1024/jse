package com.jtool.math.matrix;

import com.jtool.math.IDataShell;
import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 内部存储 double[] 的矩阵，会加速相关的运算 </p>
 * <p> 由于没有需要的实现，暂时略去中间的 RealMatrix 这一层 </p>
 */
public abstract class DoubleArrayMatrix extends AbstractMatrix implements IDataShell<double[]> {
    protected double[] mData;
    protected DoubleArrayMatrix(double[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setData2this(double[] aData) {mData = aData;}
    @Override public double[] getData() {return mData;}
    @Override public int dataSize() {return columnNumber()*rowNumber();}
    
    
    protected class DoubleArrayMatrixOperation_ extends DoubleArrayMatrixOperation {
        @Override protected DoubleArrayMatrix thisMatrix_() {return DoubleArrayMatrix.this;}
        @Override protected DoubleArrayMatrix newMatrix_(ISize aSize) {return newZeros(aSize.row(), aSize.col());}
    }
    
    /** 矩阵运算实现 */
    @Override public IMatrixOperation operation() {return new DoubleArrayMatrixOperation_();}
    
    
    /** stuff to override */
    public abstract DoubleArrayMatrix newZeros(int aRowNum, int aColNum);
    public abstract DoubleArrayMatrix newShell();
    public abstract double @Nullable[] getIfHasSameOrderData(Object aObj);
}
