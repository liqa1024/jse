package com.jtool.math.matrix;

import com.jtool.math.IDataShell;
import com.jtool.math.operation.DoubleArrayOperation;
import com.jtool.math.vector.IVectorAny;
import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 内部存储 double[] 的矩阵，会加速相关的运算 </p>
 * <p> 由于没有需要的实现，暂时略去中间的 RealMatrix 这一层 </p>
 */
public abstract class DoubleArrayMatrix<M extends DoubleArrayMatrix<?, ?>, V extends IVectorAny<?>> extends AbstractMatrixAny<M, V> implements IDataShell<double[]> {
    protected double[] mData;
    protected DoubleArrayMatrix(double[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setData2this(double[] aData) {mData = aData;}
    @Override public double[] getData() {return mData;}
    @Override public int dataSize() {return columnNumber()*rowNumber();}
    
    
    protected class DoubleArrayMatrixOperation extends DoubleArrayOperation<DoubleArrayMatrix<?, ?>, M, DoubleArrayMatrix<M, V>, IMatrixGetter> implements IDefaultMatrixOperation<M, DoubleArrayMatrix<M, V>, V> {
        @Override protected DoubleArrayMatrix<M, V> thisInstance_() {return DoubleArrayMatrix.this;}
        @Override public DoubleArrayMatrix<M, V> thisMatrix_() {return DoubleArrayMatrix.this;}
        /** 通过输入来获取需要的大小 */
        @Override protected M newInstance_(IMatrixGetter aData) {
            if (aData instanceof IMatrixAny) {
                IMatrixAny<?, ?> tMatrix = (IMatrixAny<?, ?>)aData;
                return newZeros_(tMatrix.rowNumber(), tMatrix.columnNumber());
            }
            return newZeros_(rowNumber(), columnNumber());
        }
        @Override protected M newInstance_(IMatrixGetter aData1, IMatrixGetter aData2) {
            if (aData1 instanceof IMatrixAny) {
                IMatrixAny<?, ?> tMatrix = (IMatrixAny<?, ?>)aData1;
                return newZeros_(tMatrix.rowNumber(), tMatrix.columnNumber());
            }
            if (aData2 instanceof IMatrixAny) {
                IMatrixAny<?, ?> tMatrix = (IMatrixAny<?, ?>)aData2;
                return newZeros_(tMatrix.rowNumber(), tMatrix.columnNumber());
            }
            return newZeros_(rowNumber(), columnNumber());
        }
    }
    
    /** 矩阵运算实现 */
    @Override public DoubleArrayMatrixOperation operation() {return new DoubleArrayMatrixOperation();}
    
    
    /** Optimize stuffs，重写 same 接口专门优化拷贝部分 */
    @Override public IMatrixGenerator<M> generator() {
        return new MatrixGenerator() {
                @Override public M same() {
                M rMatrix = zeros();
                final double[] rData = getIfHasSameOrderData(rMatrix);
                if (rData != null) {
                    System.arraycopy(getData(), shiftSize(), rData, rMatrix.shiftSize(), rMatrix.dataSize());
                } else {
                    rMatrix.fill(DoubleArrayMatrix.this);
                }
                return rMatrix;
            }
        };
    }
    
    /** stuff to override */
    public abstract DoubleArrayMatrix<M, V> newShell();
    public abstract double @Nullable[] getIfHasSameOrderData(Object aObj);
}
