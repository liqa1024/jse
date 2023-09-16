package com.jtool.math.vector;

import com.jtool.code.functional.*;
import com.jtool.code.iterator.IComplexDoubleIterator;
import com.jtool.code.iterator.IComplexDoubleSetOnlyIterator;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.math.ComplexDouble;
import com.jtool.math.IComplexDouble;
import com.jtool.math.MathEX;
import com.jtool.math.operation.DATA;
import groovy.lang.Closure;

import java.util.function.Supplier;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 * @author liqa
 */
public abstract class AbstractComplexVectorOperation implements IComplexVectorOperation {
    /** 通用的一些运算 */
    @Override public IComplexVector plus        (IComplexVector aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebePlus2Dest    (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector minus       (IComplexVector aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeMinus2Dest   (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector lminus      (IComplexVector aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeMinus2Dest   (aRHS, tThis, rVector); return rVector;}
    @Override public IComplexVector multiply    (IComplexVector aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeMultiply2Dest(tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector div         (IComplexVector aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeDiv2Dest     (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector ldiv        (IComplexVector aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeDiv2Dest     (aRHS, tThis, rVector); return rVector;}
    @Override public IComplexVector operate     (IComplexVector aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {final IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeDo2Dest(tThis, aRHS, rVector, aOpt); return rVector;}
    @Override public IComplexVector plus        (IVector        aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebePlus2Dest    (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector minus       (IVector        aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeMinus2Dest   (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector lminus      (IVector        aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeMinus2Dest   (aRHS, tThis, rVector); return rVector;}
    @Override public IComplexVector multiply    (IVector        aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeMultiply2Dest(tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector div         (IVector        aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeDiv2Dest     (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector ldiv        (IVector        aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeDiv2Dest     (aRHS, tThis, rVector); return rVector;}
    @Override public IComplexVector operate     (IVector        aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {final IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.ebeDo2Dest(tThis, aRHS, rVector, aOpt); return rVector;}
    
    @Override public IComplexVector plus       (IComplexDouble aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapPlus2Dest       (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector minus      (IComplexDouble aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapMinus2Dest      (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector lminus     (IComplexDouble aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapLMinus2Dest     (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector multiply   (IComplexDouble aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapMultiply2Dest   (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector div        (IComplexDouble aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapDiv2Dest        (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector ldiv       (IComplexDouble aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapLDiv2Dest       (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector plus       (double         aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapPlus2Dest       (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector minus      (double         aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapMinus2Dest      (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector lminus     (double         aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapLMinus2Dest     (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector multiply   (double         aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapMultiply2Dest   (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector div        (double         aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapDiv2Dest        (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector ldiv       (double         aRHS) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapLDiv2Dest       (tThis, aRHS, rVector); return rVector;}
    @Override public IComplexVector map        (IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.mapDo2Dest(tThis, rVector, aOpt); return rVector;}
    
    @Override public void plus2this     (IComplexVector aRHS) {DATA.ebePlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (IComplexVector aRHS) {DATA.ebeMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (IComplexVector aRHS) {DATA.ebeLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (IComplexVector aRHS) {DATA.ebeMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (IComplexVector aRHS) {DATA.ebeDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (IComplexVector aRHS) {DATA.ebeLDiv2This    (thisVector_(), aRHS);}
    @Override public void operate2this  (IComplexVector aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {DATA.ebeDo2This(thisVector_(), aRHS, aOpt);}
    @Override public void plus2this     (IVector        aRHS) {DATA.ebePlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (IVector        aRHS) {DATA.ebeMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (IVector        aRHS) {DATA.ebeLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (IVector        aRHS) {DATA.ebeMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (IVector        aRHS) {DATA.ebeDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (IVector        aRHS) {DATA.ebeLDiv2This    (thisVector_(), aRHS);}
    @Override public void operate2this  (IVector        aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {DATA.ebeDo2This(thisVector_(), aRHS, aOpt);}
    
    @Override public void plus2this     (IComplexDouble aRHS) {DATA.mapPlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (IComplexDouble aRHS) {DATA.mapMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (IComplexDouble aRHS) {DATA.mapLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (IComplexDouble aRHS) {DATA.mapMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (IComplexDouble aRHS) {DATA.mapDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (IComplexDouble aRHS) {DATA.mapLDiv2This    (thisVector_(), aRHS);}
    @Override public void plus2this     (double         aRHS) {DATA.mapPlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (double         aRHS) {DATA.mapMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (double         aRHS) {DATA.mapLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (double         aRHS) {DATA.mapMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (double         aRHS) {DATA.mapDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (double         aRHS) {DATA.mapLDiv2This    (thisVector_(), aRHS);}
    @Override public void map2this      (IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {DATA.mapDo2This(thisVector_(), aOpt);}
    
    @Override public void fill          (IComplexDouble aRHS) {DATA.mapFill2This(thisVector_(), aRHS);}
    @Override public void fill          (double aRHS) {DATA.mapFill2This(thisVector_(), aRHS);}
    @Override public void fill          (IComplexVector aRHS) {DATA.ebeFill2This(thisVector_(), aRHS);}
    @Override public void fill          (IVector aRHS) {DATA.ebeFill2This(thisVector_(), aRHS);}
    @Override public void assign        (Supplier<? extends IComplexDouble> aSup) {DATA.assign2This(thisVector_(), aSup);}
    @Override public void assign        (IDoubleSupplier aSup) {DATA.assign2This(thisVector_(), aSup);}
    @Override public void forEach       (IConsumer1<? super ComplexDouble> aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void forEach       (IDoubleConsumer2 aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill          (IComplexVectorGetter aRHS) {
        final IComplexVector tThis = thisVector_();
        final IComplexDoubleSetOnlyIterator si = tThis.setIterator();
        final int tSize = tThis.size();
        for (int i = 0; i < tSize; ++i) si.nextAndSet(aRHS.get(i));
    }
    @Override public void fill          (IVectorGetter aRHS) {
        final IComplexVector tThis = thisVector_();
        final IComplexDoubleSetOnlyIterator si = tThis.setIterator();
        final int tSize = tThis.size();
        for (int i = 0; i < tSize; ++i) si.nextAndSet(aRHS.get(i));
    }
    /** Groovy stuffs */
    @Override public void fill          (Closure<?> aGroovyTask) {
        final IComplexVector tThis = thisVector_();
        final IComplexDoubleSetOnlyIterator si = tThis.setIterator();
        final int tSize = tThis.size();
        for (int i = 0; i < tSize; ++i) {
            // 直接先执行然后检测类型决定如何设置
            Object tObj = aGroovyTask.call(i);
            if (tObj instanceof IComplexDouble) si.nextAndSet((IComplexDouble)tObj);
            else if (tObj instanceof Number) si.nextAndSet(((Number)tObj).doubleValue());
        }
    }
    @Override public void assign        (Closure<?> aGroovyTask) {
        final IComplexDoubleSetOnlyIterator si = thisVector_().setIterator();
        while (si.hasNext()) {
            // 直接先执行然后检测类型决定如何设置
            Object tObj = aGroovyTask.call();
            if (tObj instanceof IComplexDouble) si.nextAndSet((IComplexDouble)tObj);
            else if (tObj instanceof Number) si.nextAndSet(((Number)tObj).doubleValue());
        }
    }
    @Override public void forEach       (Closure<?> aGroovyTask) {
        int tN = aGroovyTask.getMaximumNumberOfParameters();
        switch (tN) {
        case 1: {forEach(value -> aGroovyTask.call(value)); return;}
        case 2: {forEach((real, imag) -> aGroovyTask.call(real, imag)); return;}
        default: throw new IllegalArgumentException("Parameters Number of forEach in ComplexVector Must be 1 or 2");
        }
    }
    
    @Override public ComplexDouble sum () {return DATA.sumOfThis (thisVector_());}
    @Override public ComplexDouble mean() {return DATA.meanOfThis(thisVector_());}
    @Override public ComplexDouble prod() {return DATA.prodOfThis(thisVector_());}
    @Override public ComplexDouble stat(IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {return DATA.statOfThis(thisVector_(), aOpt);}
    
    @Override public IComplexVector cumsum () {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.cumsum2Dest (tThis, rVector      ); return rVector;}
    @Override public IComplexVector cummean() {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.cummean2Dest(tThis, rVector      ); return rVector;}
    @Override public IComplexVector cumprod() {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.cumprod2Dest(tThis, rVector      ); return rVector;}
    @Override public IComplexVector cumstat(IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {IComplexVector tThis = thisVector_(); IComplexVector rVector = newVector_(tThis.size()); DATA.cumstat2Dest(tThis, rVector, aOpt); return rVector;}
    
    /** 向量的一些额外的运算 */
    @Override public ComplexDouble dot(IComplexVector aRHS) {
        final IComplexVector tThis = thisVector_();
        final IComplexDoubleIterator li = tThis.iterator();
        final IComplexDoubleIterator ri = aRHS.iterator();
        ComplexDouble rDot = new ComplexDouble();
        while (li.hasNext()) {
            li.nextOnly();
            ri.nextOnly();
            double lReal = li.real(), lImag = li.imag();
            double rReal = ri.real(), rImag = ri.imag();
            rDot.mReal += (lReal*rReal + lImag*rImag);
            rDot.mImag += (lImag*rReal - lReal*rImag);
        }
        return rDot;
    }
    @Override public ComplexDouble dot(IVector aRHS) {
        final IComplexVector tThis = thisVector_();
        final IComplexDoubleIterator li = tThis.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        ComplexDouble rDot = new ComplexDouble();
        while (li.hasNext()) {
            li.nextOnly();
            double tRHS = ri.next();
            rDot.mReal += (li.real()*tRHS);
            rDot.mImag += (li.imag()*tRHS);
        }
        return rDot;
    }
    @Override public double dot() {
        final IComplexDoubleIterator it = thisVector_().iterator();
        double rDot = 0.0;
        while (it.hasNext()) {
            it.nextOnly();
            double tReal = it.real(), tImag = it.imag();
            rDot += (tReal*tReal + tImag*tImag);
        }
        return rDot;
    }
    @Override public double norm() {return MathEX.Fast.sqrt(dot());}
    @Override public IVector abs() {
        final IComplexVector tThis = thisVector_();
        IVector rVector = newRealVector_(tThis.size());
        final IComplexDoubleIterator it = tThis.iterator();
        rVector.assign(() -> {
            it.nextOnly();
            double tReal = it.real(), tImag = it.imag();
            return MathEX.Fast.sqrt(tReal*tReal + tImag*tImag);
        });
        return rVector;
    }
    
    
    @Override public IComplexVector reverse() {
        IComplexVector tVector = refReverse();
        IComplexVector rVector = newVector_(tVector.size());
        rVector.fill(tVector);
        return rVector;
    }
    @Override public IComplexVector refReverse() {
        return new RefComplexVector() {
            private final IComplexVector mThis = thisVector_();
            @Override public double getReal_(int aIdx) {return mThis.getReal_(mThis.size()-1-aIdx);}
            @Override public double getImag_(int aIdx) {return mThis.getImag_(mThis.size()-1-aIdx);}
            @Override public void setReal_(int aIdx, double aReal) {mThis.setReal_(mThis.size()-1-aIdx, aReal);}
            @Override public void setImag_(int aIdx, double aImag) {mThis.setImag_(mThis.size()-1-aIdx, aImag);}
            @Override public double getAndSetReal_(int aIdx, double aReal) {return mThis.getAndSetReal_(mThis.size()-1-aIdx, aReal);}
            @Override public double getAndSetImag_(int aIdx, double aImag) {return mThis.getAndSetImag_(mThis.size()-1-aIdx, aImag);}
            @Override public int size() {return mThis.size();}
        };
    }
    
    
    /** stuff to override */
    protected abstract IComplexVector thisVector_();
    protected abstract IComplexVector newVector_(int aSize);
    protected IVector newRealVector_(int aSize) {return Vector.zeros(aSize);}
}
