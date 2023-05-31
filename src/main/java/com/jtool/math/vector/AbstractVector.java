package com.jtool.math.vector;

/**
 * 一般向量的接口的默认实现，实际返回向量类型为 {@link Vector}，用来方便实现抽象的向量
 * @author liqa
 */
public abstract class AbstractVector extends AbstractVectorAny<Vector> implements IVector {
    @Override public final IVectorOperation<Vector> operation() {
        return new AbstractVectorOperation<Vector, AbstractVector>() {
            @Override protected AbstractVector thisInstance_() {return AbstractVector.this;}
            @Override public AbstractVector thisVector_() {return AbstractVector.this;}
            /** 通过输入来获取需要的大小 */
            @Override protected Vector newInstance_(IVectorGetter aData) {
                if (aData instanceof IVectorAny) return Vector.zeros(((IVectorAny<?>)aData).size());
                return Vector.zeros(size());
            }
            @Override protected Vector newInstance_(IVectorGetter aData1, IVectorGetter aData2) {
                if (aData1 instanceof IVectorAny) return Vector.zeros(((IVectorAny<?>)aData1).size());
                if (aData2 instanceof IVectorAny) return Vector.zeros(((IVectorAny<?>)aData2).size());
                return Vector.zeros(size());
            }
        };
    }
    
    @Override protected final Vector newZeros(int aSize) {return Vector.zeros(aSize);}
    
    /** stuff to override */
    public abstract double get_(int aIdx);
    @Override public void set_(int aIdx, double aValue) {throw new UnsupportedOperationException("set");}
    @Override public double getAndSet_(int aIdx, double aValue) {throw new UnsupportedOperationException("set");}
    public abstract int size();
}
