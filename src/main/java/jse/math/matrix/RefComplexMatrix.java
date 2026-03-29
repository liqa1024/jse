package jse.math.matrix;


import jse.math.ComplexDouble;
import jse.math.vector.ComplexVector;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;

/**
 * 一般矩阵的接口的默认实现，实际返回矩阵类型为 {@link RowComplexMatrix}，向量为 {@link ComplexVector}，用来方便实现抽象的矩阵
 * @author liqa
 */
public abstract class RefComplexMatrix extends AbstractComplexMatrix {
    @Override protected final IComplexMatrix newZeros_(int aRowNum, int aColNum) {return RowComplexMatrix.zeros(aRowNum, aColNum);}
    
    /** stuff to override */
    @Override public abstract double getReal(int aRow, int aCol);
    @Override public abstract double getImag(int aRow, int aCol);
    @Override public void set(int aRow, int aCol, double aReal, double aImag) {setReal(aRow, aCol, aReal); setImag(aRow, aCol, aImag);}
    @Override public void setReal(int aRow, int aCol, double aReal) {throw new UnsupportedOperationException("set");}
    @Override public void setImag(int aRow, int aCol, double aImag) {throw new UnsupportedOperationException("set");}
    @Override public ComplexDouble getAndSet(int aRow, int aCol, double aReal, double aImag) {
        return new ComplexDouble(getAndSetReal(aRow, aCol, aReal), getAndSetImag(aRow, aCol, aImag));
    }
    @Override public double getAndSetReal(int aRow, int aCol, double aReal) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        double oReal = getReal(aRow, aCol);
        setReal(aRow, aCol, aReal);
        return oReal;
    }
    @Override public double getAndSetImag(int aRow, int aCol, double aImag) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        double oImag = getImag(aRow, aCol);
        setImag(aRow, aCol, aImag);
        return oImag;
    }
    @Override public abstract int nrows();
    @Override public abstract int ncols();
}
