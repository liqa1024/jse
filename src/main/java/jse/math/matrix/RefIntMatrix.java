package jse.math.matrix;

import jse.math.vector.IntVector;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;

/**
 * 一般矩阵的接口的默认实现，实际返回矩阵类型为 {@link ColumnIntMatrix}，向量为 {@link IntVector}，用来方便实现抽象的矩阵
 * @author liqa
 */
public abstract class RefIntMatrix extends AbstractIntMatrix {
    @Override protected final IIntMatrix newZeros_(int aRowNum, int aColNum) {return ColumnIntMatrix.zeros(aRowNum, aColNum);}
    
    /** stuff to override */
    public abstract int get(int aRow, int aCol);
    public void set(int aRow, int aCol, int aValue) {throw new UnsupportedOperationException("set");}
    public int getAndSet(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, rowNumber());
        rangeCheckCol(aCol, columnNumber());
        int oValue = get(aRow, aCol);
        set(aRow, aCol, aValue);
        return oValue;
    }
    public abstract int rowNumber();
    public abstract int columnNumber();
}
