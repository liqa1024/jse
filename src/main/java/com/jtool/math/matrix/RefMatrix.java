package com.jtool.math.matrix;

import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vector;

/**
 * 一般矩阵的接口的默认实现，实际返回矩阵类型为 {@link ColumnMatrix}，向量为 {@link Vector}，用来方便实现抽象的矩阵
 * @author liqa
 */
public abstract class RefMatrix extends AbstractMatrix {
    @Override protected final IMatrix newZeros_(int aRowNum, int aColNum) {return ColumnMatrix.zeros(aRowNum, aColNum);}
    @Override protected final IVector newZeros_(int aSize) {return Vector.zeros(aSize);}
    
    /** stuff to override */
    public abstract double get_(int aRow, int aCol);
    @Override public void set_(int aRow, int aCol, double aValue) {throw new UnsupportedOperationException("set");}
    @Override public double getAndSet_(int aRow, int aCol, double aValue) {
        double oValue = get_(aRow, aCol);
        set_(aRow, aCol, aValue);
        return oValue;
    }
    public abstract int rowNumber();
    public abstract int columnNumber();
}
