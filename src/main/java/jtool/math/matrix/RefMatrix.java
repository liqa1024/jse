package jtool.math.matrix;

import jtool.math.vector.Vector;

/**
 * 一般矩阵的接口的默认实现，实际返回矩阵类型为 {@link ColumnMatrix}，向量为 {@link Vector}，用来方便实现抽象的矩阵
 * @author liqa
 */
public abstract class RefMatrix extends AbstractMatrix {
    @Override protected final IMatrix newZeros_(int aRowNum, int aColNum) {return ColumnMatrix.zeros(aRowNum, aColNum);}
    
    /** stuff to override */
    public abstract double get(int aRow, int aCol);
    public void set(int aRow, int aCol, double aValue) {throw new UnsupportedOperationException("set");}
    public double getAndSet(int aRow, int aCol, double aValue) {
        double oValue = get(aRow, aCol);
        set(aRow, aCol, aValue);
        return oValue;
    }
    public abstract int rowNumber();
    public abstract int columnNumber();
}
