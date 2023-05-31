package com.jtool.math.vector;

import com.jtool.math.IDataShell;
import com.jtool.math.operation.DoubleArrayOperation;
import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 内部存储 double[] 的向量，会加速相关的运算 </p>
 */
public abstract class DoubleArrayVector<V extends DoubleArrayVector<?>> extends AbstractVectorAny<V> implements IDataShell<double[]> {
    protected double[] mData;
    protected DoubleArrayVector(double[] aData) {mData = aData;}
    
    /** DataShell stuffs */
    @Override public void setData2this(double[] aData) {mData = aData;}
    @Override public double[] getData() {return mData;}
    @Override public int dataSize() {return size();}
    
    
    protected class DoubleArrayVectorOperation extends DoubleArrayOperation<DoubleArrayVector<?>, V, DoubleArrayVector<V>, IVectorGetter> implements IDefaultVectorOperation<V, DoubleArrayVector<V>> {
        @Override protected DoubleArrayVector<V> thisInstance_() {return DoubleArrayVector.this;}
        @Override public DoubleArrayVector<V> thisVector_() {return DoubleArrayVector.this;}
        /** 通过输入来获取需要的大小 */
        @Override protected V newInstance_(IVectorGetter aData) {
            if (aData instanceof IVectorAny) return newZeros(((IVectorAny<?>)aData).size());
            return newZeros(size());
        }
        @Override protected V newInstance_(IVectorGetter aData1, IVectorGetter aData2) {
            if (aData1 instanceof IVectorAny) return newZeros(((IVectorAny<?>)aData1).size());
            if (aData2 instanceof IVectorAny) return newZeros(((IVectorAny<?>)aData2).size());
            return newZeros(size());
        }
    }
    
    /** 向量运算实现 */
    @Override public DoubleArrayVectorOperation operation() {return new DoubleArrayVectorOperation();}
    
    /** Optimize stuffs，重写这些接口来加速批量填充过程 */
    @Override public void fill(double[] aData) {System.arraycopy(aData, 0, getData(), shiftSize(), dataSize());}
    
    /** Optimize stuffs，重写 same 接口专门优化拷贝部分 */
    @Override public IVectorGenerator<V> generator() {
        return new VectorGenerator() {
            @Override public V same() {
                V rVector = zeros();
                double[] rData = getIfHasSameOrderData(rVector);
                if (rData != null) {
                    System.arraycopy(getData(), shiftSize(), rData, rVector.shiftSize(), rVector.dataSize());
                } else {
                    rVector.fill(DoubleArrayVector.this);
                }
                return rVector;
            }
        };
    }
    
    /** stuff to override */
    public abstract DoubleArrayVector<V> newShell();
    public abstract double @Nullable[] getIfHasSameOrderData(Object aObj);
}
