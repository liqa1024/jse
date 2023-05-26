package com.jtool.math.matrix;

import com.jtool.math.IDataShell;
import com.jtool.math.operation.DoubleArrayOperation;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGetter;
import com.jtool.math.vector.IVectorOperation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author liqa
 * <p> 内部存储 double[] 的矩阵，会加速相关的运算 </p>
 * <p> 由于没有需要的实现，暂时略去中间的 RealMatrix 这一层 </p>
 */
public abstract class DoubleArrayMatrix<M extends DoubleArrayMatrix<?, ?>, V extends IVector<Double> & IDataShell<V, double[]>> extends AbstractMatrixFull<Double, M, V> implements IDataShell<M, double[]> {
    protected double[] mData;
    protected DoubleArrayMatrix(double[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setData2this(double[] aData) {mData = aData;}
    @Override public double[] getData() {return mData;}
    @Override public int dataSize() {return columnNumber()*rowNumber();}
    
    
    protected class DoubleArrayMatrixOperation extends DoubleArrayOperation<M, IMatrixGetter<? extends Number>> implements IMatrixOperation<M, Double> {
        @Override protected M thisInstance_() {return this_();}
        @Override protected M newInstance_() {return DoubleArrayMatrix.this.generator().zeros();}
    }
    protected class DoubleArrayVectorOperation extends DoubleArrayOperation<V, IVectorGetter<? extends Number>> implements IVectorOperation<V, Double> {
        @Override protected V thisInstance_() {throw new RuntimeException("Can NOT put Vector into Matrix");} // 对于这种 this 调用直接报错即可
        @Override protected V newInstance_() {return generatorVec().zeros();}
    }
    
    /** 矩阵运算实现 */
    @Override public IMatrixOperation<M, Double> operation() {return new DoubleArrayMatrixOperation();}
    @Override public IVectorOperation<V, Double> operationVec() {return new DoubleArrayVectorOperation();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(Number aValue) {Arrays.fill(mData, aValue.doubleValue());}
    
    /** Optimize stuffs，重写 same 接口专门优化拷贝部分 */
    @Override public IMatrixGenerator<M> generator() {
        return new MatrixGenerator() {
                @Override public M same() {
                M rMatrix = zeros();
                double[] rData = getIfHasSameOrderData(rMatrix);
                if (rData != null) {
                    System.arraycopy(getData(), shiftSize(), rData, rMatrix.shiftSize(), rMatrix.dataSize());
                } else {
                    rMatrix.fillWith(DoubleArrayMatrix.this);
                }
                return rMatrix;
            }
        };
    }
    
    /** stuff to override */
    protected abstract M this_();
    public abstract M newShell();
    public abstract double @Nullable[] getIfHasSameOrderData(Object aObj);
}
