package jse.math.vector;


import jse.math.ComplexDouble;

import static jse.math.vector.AbstractVector.rangeCheck;

/**
 * 一般向量的接口的默认实现，实际返回向量类型为 {@link ComplexVector}，用来方便实现抽象的向量
 * @author liqa
 */
public abstract class RefComplexVector extends AbstractComplexVector {
    @Override protected final IComplexVector newZeros_(int aSize) {return ComplexVector.zeros(aSize);}
    
    /** stuff to override */
    public abstract double getReal(int aIdx);
    public abstract double getImag(int aIdx);
    public void set(int aIdx, double aReal, double aImag) {setReal(aIdx, aReal); setImag(aIdx, aImag);}
    public void setReal(int aIdx, double aReal) {throw new UnsupportedOperationException("set");}
    public void setImag(int aIdx, double aImag) {throw new UnsupportedOperationException("set");}
    public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {
        return new ComplexDouble(getAndSetReal(aIdx, aReal), getAndSetImag(aIdx, aImag));
    }
    public double getAndSetReal(int aIdx, double aReal) {
        rangeCheck(aIdx, size());
        double oReal = getReal(aIdx);
        setReal(aIdx, aReal);
        return oReal;
    }
    public double getAndSetImag(int aIdx, double aImag) {
        rangeCheck(aIdx, size());
        double oImag = getImag(aIdx);
        setImag(aIdx, aImag);
        return oImag;
    }
    public abstract int size();
}
