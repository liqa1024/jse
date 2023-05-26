package com.jtool.math.vector;

import com.jtool.math.IDataShell;
import com.jtool.math.operation.DoubleArrayOperation;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

/**
 * @author liqa
 * <p> 内部存储 double[] 的向量，会加速相关的运算 </p>
 * <p> 由于没有需要的实现，暂时略去中间的 RealVector 这一层 </p>
 */
public abstract class DoubleArrayVector<V extends DoubleArrayVector<?>> extends AbstractVectorFull<Double, V> implements IDataShell<V, double[]> {
    protected double[] mData;
    protected DoubleArrayVector(double[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setData2this(double[] aData) {mData = aData;}
    @Override public double[] getData() {return mData;}
    @Override public int dataSize() {return size();}
    
    
    protected class DoubleArrayVectorOperation extends DoubleArrayOperation<V, IVectorGetter<? extends Number>> implements IVectorOperation<V, Double> {
        @Override protected V thisInstance_() {return this_();}
        @Override protected V newInstance_() {return generator().zeros();}
    }
    
    /** 向量运算实现 */
    @Override public IVectorOperation<V, Double> operation() {return new DoubleArrayVectorOperation();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(Number aValue) {Arrays.fill(mData, aValue.doubleValue());}
    
    /** Optimize stuffs，重写 same 接口专门优化拷贝部分 */
    @Override public IVectorGenerator<V> generator() {
        return new VectorGenerator() {
            @Override public V same() {
                V rVector = zeros();
                double[] rData = getIfHasSameOrderData(rVector);
                if (rData != null) {
                    System.arraycopy(getData(), shiftSize(), rData, rVector.shiftSize(), rVector.dataSize());
                } else {
                    rVector.fillWith(DoubleArrayVector.this);
                }
                return rVector;
            }
        };
    }
    
    /** stuff to override */
    protected abstract V this_();
    public abstract V newShell();
    public abstract double @Nullable[] getIfHasSameOrderData(Object aObj);
}
