package jse.math.matrix;

import jse.math.vector.Vector;

/**
 * 一般矩阵的接口的默认实现，实际返回矩阵类型为 {@link RowMatrix}，向量为 {@link Vector}，用来方便实现抽象的矩阵
 * @author liqa
 */
public abstract class RefMatrix extends AbstractMatrix {
    @Override protected final IMatrix newZeros_(int aRowNum, int aColNum) {return RowMatrix.zeros(aRowNum, aColNum);}
    
    /** stuff to override */
    @Override public abstract double get(int aRow, int aCol);
    @Override public void set(int aRow, int aCol, double aValue) {throw new UnsupportedOperationException("set");}
    @Override public double getAndSet(int aRow, int aCol, double aValue) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        double oValue = get(aRow, aCol);
        set(aRow, aCol, aValue);
        return oValue;
    }
    @Override public abstract int nrows();
    @Override public abstract int ncols();
}
