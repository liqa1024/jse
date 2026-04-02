package jse.math.matrix;

import jse.math.vector.IntVector;

import static jse.math.matrix.AbstractMatrix.rangeCheckCol;
import static jse.math.matrix.AbstractMatrix.rangeCheckRow;

/**
 * 一般矩阵的接口的默认实现，实际返回矩阵类型为 {@link RowLogicalMatrix}，向量为 {@link IntVector}，用来方便实现抽象的矩阵
 * @author liqa
 */
public abstract class RefLogicalMatrix extends AbstractLogicalMatrix {
    @Override protected final ILogicalMatrix newZeros_(int aRowNum, int aColNum) {return RowLogicalMatrix.zeros(aRowNum, aColNum);}
    
    /** stuff to override */
    @Override public abstract boolean get(int aRow, int aCol);
    @Override public void set(int aRow, int aCol, boolean aValue) {throw new UnsupportedOperationException("set");}
    @Override public boolean getAndSet(int aRow, int aCol, boolean aValue) {
        rangeCheckRow(aRow, nrows());
        rangeCheckCol(aCol, ncols());
        boolean oValue = get(aRow, aCol);
        set(aRow, aCol, aValue);
        return oValue;
    }
    @Override public abstract int nrows();
    @Override public abstract int ncols();
}
