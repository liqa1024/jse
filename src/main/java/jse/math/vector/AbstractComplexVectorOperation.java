package jse.math.vector;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import groovy.transform.stc.SimpleType;
import jse.code.functional.IBinaryFullOperator;
import jse.code.functional.IDoubleBinaryConsumer;
import jse.code.functional.IUnaryFullOperator;
import jse.code.iterator.IComplexDoubleIterator;
import jse.code.iterator.IDoubleIterator;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.MathEX;
import jse.math.operation.DATA;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import static jse.math.vector.AbstractVector.rangeCheck;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 * @author liqa
 */
public abstract class AbstractComplexVectorOperation implements IComplexVectorOperation {
    /** 通用的一些运算 */
    @Override public IComplexVector plus        (IComplexVector aRHS) {IComplexVector rVector = newVector_(); DATA.ebePlus2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector minus       (IComplexVector aRHS) {IComplexVector rVector = newVector_(); DATA.ebeMinus2Dest   (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector lminus      (IComplexVector aRHS) {IComplexVector rVector = newVector_(); DATA.ebeMinus2Dest   (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IComplexVector multiply    (IComplexVector aRHS) {IComplexVector rVector = newVector_(); DATA.ebeMultiply2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector div         (IComplexVector aRHS) {IComplexVector rVector = newVector_(); DATA.ebeDiv2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector ldiv        (IComplexVector aRHS) {IComplexVector rVector = newVector_(); DATA.ebeDiv2Dest     (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IComplexVector operate     (IComplexVector aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {IComplexVector rVector = newVector_(); DATA.ebeDo2Dest(thisVector_(), aRHS, rVector, aOpt); return rVector;}
    @Override public IComplexVector plus        (IVector        aRHS) {IComplexVector rVector = newVector_(); DATA.ebePlus2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector minus       (IVector        aRHS) {IComplexVector rVector = newVector_(); DATA.ebeMinus2Dest   (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector lminus      (IVector        aRHS) {IComplexVector rVector = newVector_(); DATA.ebeMinus2Dest   (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IComplexVector multiply    (IVector        aRHS) {IComplexVector rVector = newVector_(); DATA.ebeMultiply2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector div         (IVector        aRHS) {IComplexVector rVector = newVector_(); DATA.ebeDiv2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector ldiv        (IVector        aRHS) {IComplexVector rVector = newVector_(); DATA.ebeDiv2Dest     (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IComplexVector operate     (IVector        aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {IComplexVector rVector = newVector_(); DATA.ebeDo2Dest(thisVector_(), aRHS, rVector, aOpt); return rVector;}
    
    @Override public IComplexVector plus        (IComplexDouble aRHS) {IComplexVector rVector = newVector_(); DATA.mapPlus2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector minus       (IComplexDouble aRHS) {IComplexVector rVector = newVector_(); DATA.mapMinus2Dest   (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector lminus      (IComplexDouble aRHS) {IComplexVector rVector = newVector_(); DATA.mapLMinus2Dest  (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector multiply    (IComplexDouble aRHS) {IComplexVector rVector = newVector_(); DATA.mapMultiply2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector div         (IComplexDouble aRHS) {IComplexVector rVector = newVector_(); DATA.mapDiv2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector ldiv        (IComplexDouble aRHS) {IComplexVector rVector = newVector_(); DATA.mapLDiv2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector plus        (double         aRHS) {IComplexVector rVector = newVector_(); DATA.mapPlus2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector minus       (double         aRHS) {IComplexVector rVector = newVector_(); DATA.mapMinus2Dest   (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector lminus      (double         aRHS) {IComplexVector rVector = newVector_(); DATA.mapLMinus2Dest  (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector multiply    (double         aRHS) {IComplexVector rVector = newVector_(); DATA.mapMultiply2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector div         (double         aRHS) {IComplexVector rVector = newVector_(); DATA.mapDiv2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector ldiv        (double         aRHS) {IComplexVector rVector = newVector_(); DATA.mapLDiv2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IComplexVector map         (IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {IComplexVector rVector = newVector_(); DATA.mapDo2Dest(thisVector_(), rVector, aOpt); return rVector;}
    
    @Override public void plus2this     (IComplexVector aRHS) {DATA.ebePlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (IComplexVector aRHS) {DATA.ebeMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (IComplexVector aRHS) {DATA.ebeLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (IComplexVector aRHS) {DATA.ebeMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (IComplexVector aRHS) {DATA.ebeDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (IComplexVector aRHS) {DATA.ebeLDiv2This    (thisVector_(), aRHS);}
    @Override public void operate2this  (IComplexVector aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {DATA.ebeDo2This(thisVector_(), aRHS, aOpt);}
    @Override public void plus2this     (IVector        aRHS) {DATA.ebePlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (IVector        aRHS) {DATA.ebeMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (IVector        aRHS) {DATA.ebeLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (IVector        aRHS) {DATA.ebeMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (IVector        aRHS) {DATA.ebeDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (IVector        aRHS) {DATA.ebeLDiv2This    (thisVector_(), aRHS);}
    @Override public void operate2this  (IVector        aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, Double> aOpt) {DATA.ebeDo2This(thisVector_(), aRHS, aOpt);}
    
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
    @Override public void map2this      (IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {DATA.mapDo2This(thisVector_(), aOpt);}
    
    @Override public IComplexVector negative() {IComplexVector rVector = newVector_(); DATA.mapNegative2Dest(thisVector_(), rVector); return rVector;}
    @Override public void negative2this() {DATA.mapNegative2This(thisVector_());}
    
    @Override public void fill          (IComplexDouble                     aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill          (double                             aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill          (IComplexVector                     aRHS) {DATA.ebeFill2This (thisVector_(), aRHS);}
    @Override public void fill          (IVector                            aRHS) {DATA.ebeFill2This (thisVector_(), aRHS);}
    @Override public void assign        (Supplier<? extends IComplexDouble> aSup) {DATA.assign2This  (thisVector_(), aSup);}
    @Override public void assign        (DoubleSupplier                     aSup) {DATA.assign2This  (thisVector_(), aSup);}
    @Override public void forEach       (Consumer<? super ComplexDouble>    aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void forEach       (IDoubleBinaryConsumer              aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill          (IComplexVectorGetter               aRHS) {DATA.vecFill2This (thisVector_(), aRHS);}
    @Override public void fill          (IVectorGetter                      aRHS) {DATA.vecFill2This (thisVector_(), aRHS);}
    /** Groovy stuffs */
    @Override public void fill          (@ClosureParams(value=SimpleType.class, options="int") Closure<?> aGroovyTask) {DATA.vecFill2This(thisVector_(), aGroovyTask);}
    @Override public void assign        (Closure<?> aGroovyTask) {DATA.assign2This  (thisVector_(), aGroovyTask);}
    @Override public void forEach       (@ClosureParams(value=FromString.class, options={"ComplexDouble", "double,double"}) Closure<?> aGroovyTask) {DATA.forEachOfThis(thisVector_(), aGroovyTask);}
    
    @Override public ComplexDouble sum () {return DATA.sumOfThis (thisVector_());}
    @Override public ComplexDouble mean() {return DATA.meanOfThis(thisVector_());}
    @Override public ComplexDouble prod() {return DATA.prodOfThis(thisVector_());}
    @Override public ComplexDouble stat(IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {return DATA.statOfThis(thisVector_(), aOpt);}
    
    @Override public IComplexVector cumsum () {IComplexVector rVector = newVector_(); DATA.cumsum2Dest (thisVector_(), rVector      ); return rVector;}
    @Override public IComplexVector cummean() {IComplexVector rVector = newVector_(); DATA.cummean2Dest(thisVector_(), rVector      ); return rVector;}
    @Override public IComplexVector cumprod() {IComplexVector rVector = newVector_(); DATA.cumprod2Dest(thisVector_(), rVector      ); return rVector;}
    @Override public IComplexVector cumstat(IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {IComplexVector rVector = newVector_(); DATA.cumstat2Dest(thisVector_(), rVector, aOpt); return rVector;}
    
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
        IVector rVector = newRealVector_();
        final IComplexDoubleIterator it = thisVector_().iterator();
        rVector.assign(() -> {
            it.nextOnly();
            double tReal = it.real(), tImag = it.imag();
            return MathEX.Fast.hypot(tReal, tImag);
        });
        return rVector;
    }
    
    
    @Override public IComplexVector reverse() {IComplexVector rVector = newVector_(); DATA.reverse2Dest(thisVector_(), rVector); return rVector;}
    @Override public IComplexVector refReverse() {
        return new RefComplexVector() {
            private final IComplexVector mThis = thisVector_();
            @Override public double getReal(int aIdx) {rangeCheck(aIdx, size()); return mThis.getReal(mThis.size()-1-aIdx);}
            @Override public double getImag(int aIdx) {rangeCheck(aIdx, size()); return mThis.getImag(mThis.size()-1-aIdx);}
            @Override public void set(int aIdx, double aReal, double aImag) {rangeCheck(aIdx, size()); mThis.set(mThis.size()-1-aIdx, aReal, aImag);}
            @Override public void setReal(int aIdx, double aReal) {rangeCheck(aIdx, size()); mThis.setReal(mThis.size()-1-aIdx, aReal);}
            @Override public void setImag(int aIdx, double aImag) {rangeCheck(aIdx, size()); mThis.setImag(mThis.size()-1-aIdx, aImag);}
            @Override public ComplexDouble getAndSet(int aIdx, double aReal, double aImag) {rangeCheck(aIdx, size()); return mThis.getAndSet(mThis.size()-1-aIdx, aReal, aImag);}
            @Override public double getAndSetReal(int aIdx, double aReal) {rangeCheck(aIdx, size()); return mThis.getAndSetReal(mThis.size()-1-aIdx, aReal);}
            @Override public double getAndSetImag(int aIdx, double aImag) {rangeCheck(aIdx, size()); return mThis.getAndSetImag(mThis.size()-1-aIdx, aImag);}
            @Override public int size() {return mThis.size();}
        };
    }
    @Override public void reverse2this() {DATA.reverse2This(thisVector_());}
    
    
    @Override public void mplus2this(IComplexVector aRHS, double aMul) {DATA.mapMultiplyThenEbePlus2This(thisVector_(), aRHS, aMul);}
    @Override public void mplus2this(IComplexVector aRHS, IComplexDouble aMul) {DATA.mapMultiplyThenEbePlus2This(thisVector_(), aRHS, aMul);}
    
    
    /** 方便内部使用，减少一些重复代码 */
    private IComplexVector newVector_() {return newVector_(thisVector_().size());}
    private IVector newRealVector_() {return newRealVector_(thisVector_().size());}
    
    /** stuff to override */
    protected abstract IComplexVector thisVector_();
    protected abstract IComplexVector newVector_(int aSize);
    protected IVector newRealVector_(int aSize) {return Vector.zeros(aSize);}
}
