package com.jtool.math.matrix;

import com.jtool.math.vector.Vector;

/**
 * 一般矩阵的接口的默认实现，实际返回矩阵类型为 {@link ColumnMatrix}，向量为 {@link Vector}，用来方便实现抽象的矩阵
 * @author liqa
 */
public abstract class AbstractMatrix extends AbstractMatrixAny<ColumnMatrix, Vector> implements IMatrix {
    @Override public final IMatrixOperation<ColumnMatrix, Vector> operation() {
        return new AbstractMatrixOperation<ColumnMatrix, AbstractMatrix, Vector>() {
            @Override protected AbstractMatrix thisInstance_() {return AbstractMatrix.this;}
            @Override public AbstractMatrix thisMatrix_() {return AbstractMatrix.this;}
            /** 通过输入来获取需要的大小 */
            @Override protected ColumnMatrix newInstance_(IMatrixGetter aData) {
                if (aData instanceof IMatrixAny) {
                    IMatrixAny<?, ?> tMatrix = (IMatrixAny<?, ?>)aData;
                    return ColumnMatrix.zeros(tMatrix.rowNumber(), tMatrix.columnNumber());
                }
                return ColumnMatrix.zeros(rowNumber(), columnNumber());
            }
            @Override protected ColumnMatrix newInstance_(IMatrixGetter aData1, IMatrixGetter aData2) {
                if (aData1 instanceof IMatrixAny) {
                    IMatrixAny<?, ?> tMatrix = (IMatrixAny<?, ?>)aData1;
                    return ColumnMatrix.zeros(tMatrix.rowNumber(), tMatrix.columnNumber());
                }
                if (aData2 instanceof IMatrixAny) {
                    IMatrixAny<?, ?> tMatrix = (IMatrixAny<?, ?>)aData2;
                    return ColumnMatrix.zeros(tMatrix.rowNumber(), tMatrix.columnNumber());
                }
                return ColumnMatrix.zeros(rowNumber(), columnNumber());
            }
        };
    }
    
    @Override protected final ColumnMatrix newZeros_(int aRowNum, int aColNum) {return ColumnMatrix.zeros(aRowNum, aColNum);}
    @Override protected final Vector newZeros_(int aSize) {return Vector.zeros(aSize);}
    
    
    /** stuff to override */
    public abstract double get_(int aRow, int aCol);
    @Override public void set_(int aRow, int aCol, double aValue) {throw new UnsupportedOperationException("set");}
    @Override public double getAndSet_(int aRow, int aCol, double aValue) {throw new UnsupportedOperationException("set");}
    public abstract int rowNumber();
    public abstract int columnNumber();
}
