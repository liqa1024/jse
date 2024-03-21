package jse.math.vector;

import groovy.lang.Closure;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.SimpleType;
import jse.code.functional.IBinaryFullOperator;
import jse.code.functional.IDoubleBinaryConsumer;
import jse.code.functional.IUnaryFullOperator;
import jse.math.ComplexDouble;
import jse.math.IComplexDouble;
import jse.math.IDataShell;
import jse.math.operation.ARRAY;
import jse.math.operation.DATA;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import static jse.math.vector.AbstractVectorOperation.ebeCheck;

/**
 * 对于内部含有 double[][] 的复向量的运算使用专门优化后的函数
 * <p>
 * 目前只优化相同类型的向量之间的运算
 * @author liqa
 */
public abstract class BiDoubleArrayVectorOperation extends AbstractComplexVectorOperation {
    /** 通用的一些运算 */
    @Override public IComplexVector plus(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size());
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebePlus2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebePlus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector minus(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size());
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector lminus(IComplexVector aRHS) {
        final BiDoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size());
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataR, IDataShell.internalDataShift(aRHS), tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMinus2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IComplexVector multiply(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size());
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMultiply2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMultiply2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector div(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size());
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector ldiv(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size());
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataR, IDataShell.internalDataShift(aRHS), tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeDiv2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IComplexVector operate(IComplexVector aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        BiDoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size());
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize(), aOpt);
        else DATA.ebeDo2Dest(tThis, aRHS, rVector, aOpt);
        return rVector;
    }
    
    @Override public IComplexVector plus(IComplexDouble aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapPlus2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapPlus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector minus(IComplexDouble aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector lminus(IComplexDouble aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapLMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector multiply(IComplexDouble aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMultiply2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapMultiply2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector div(IComplexDouble aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector ldiv(IComplexDouble aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapLDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector plus(double aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapPlus2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapPlus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector minus(double aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector lminus(double aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapLMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector multiply(double aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMultiply2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapMultiply2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector div(double aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector ldiv(double aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapLDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector map(IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDo2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize(), aOpt);
        else DATA.mapDo2Dest(tThis, rVector, aOpt);
        return rVector;
    }
    
    @Override public void plus2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebePlus2This(rThis, aRHS);
    }
    @Override public void minus2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMinus2This(rThis, aRHS);
    }
    @Override public void lminus2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLMinus2This(rThis, aRHS);
    }
    @Override public void multiply2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMultiply2This(rThis, aRHS);
    }
    @Override public void div2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeDiv2This(rThis, aRHS);
    }
    @Override public void ldiv2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLDiv2This(rThis, aRHS);
    }
    @Override public void operate2this(IComplexVector aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize(), aOpt);
        else DATA.ebeDo2This(rThis, aRHS, aOpt);
    }
    
    @Override public void plus2this     (IComplexDouble aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapPlus2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void minus2this    (IComplexDouble aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapMinus2This   (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void lminus2this   (IComplexDouble aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapLMinus2This  (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void multiply2this (IComplexDouble aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapMultiply2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void div2this      (IComplexDouble aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapDiv2This     (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void ldiv2this     (IComplexDouble aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapLDiv2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void plus2this     (double         aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapPlus2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void minus2this    (double         aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapMinus2This   (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void lminus2this   (double         aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapLMinus2This  (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void multiply2this (double         aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapMultiply2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void div2this      (double         aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapDiv2This     (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void ldiv2this     (double         aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapLDiv2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void map2this(IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapDo2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aOpt);}
    
    @Override public IComplexVector negative() {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapNegative2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapNegative2Dest(tThis, rVector);
        return rVector;
    }
    @Override public void negative2this() {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapNegative2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    
    @Override public void fill          (IComplexDouble aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill          (double aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill          (IComplexVector aRHS) {
        final BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    @Override public void fill          (IComplexVectorGetter               aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.vecFill2This (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aRHS);}
    @Override public void fill          (IVectorGetter                      aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.vecFill2This (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aRHS);}
    @Override public void assign        (Supplier<? extends IComplexDouble> aSup) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.assign2This  (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aSup);}
    @Override public void assign        (DoubleSupplier                     aSup) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.assign2This  (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aSup);}
    @Override public void forEach       (Consumer<? super ComplexDouble>    aCon) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.forEachOfThis(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aCon);}
    @Override public void forEach       (IDoubleBinaryConsumer              aCon) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.forEachOfThis(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aCon);}
    /** Groovy stuffs */
    @Override public void fill          (@ClosureParams(value=SimpleType.class, options="int") Closure<?> aRHS) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.vecFill2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aRHS);}
    @Override public void assign        (Closure<?>                         aSup) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.assign2This  (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aSup);}
    
    @Override public ComplexDouble sum () {BiDoubleArrayVector tThis = thisVector_(); return ARRAY.sumOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public ComplexDouble mean() {BiDoubleArrayVector tThis = thisVector_(); return ARRAY.meanOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public ComplexDouble prod() {BiDoubleArrayVector tThis = thisVector_(); return ARRAY.prodOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public ComplexDouble stat(IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {BiDoubleArrayVector tThis = thisVector_(); return ARRAY.statOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize(), aOpt);}
    
    
    /** 向量的一些额外的运算 */
    @Override public ComplexDouble dot(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size());
        double[][] tDataR = tThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) return ARRAY.dot(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), tThis.internalDataSize());
        else return super.dot(aRHS);
    }
    @Override public double dot() {
        BiDoubleArrayVector tThis = thisVector_();
        return ARRAY.dotOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());
    }
    
    @Override public IComplexVector reverse() {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.reverse2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.reverse2Dest(tThis, rVector);
        return rVector;
    }
    
    
    @Override public void mplus2this(IComplexVector aRHS, double aMul) {
        BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.mapMultiplyThenEbePlus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), aMul, rThis.internalDataSize());
        else DATA.mapMultiplyThenEbePlus2This(rThis, aRHS, aMul);
    }
    @Override public void mplus2this(IComplexVector aRHS, IComplexDouble aMul) {
        BiDoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.mapMultiplyThenEbePlus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), aMul, rThis.internalDataSize());
        else DATA.mapMultiplyThenEbePlus2This(rThis, aRHS, aMul);
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private BiDoubleArrayVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    @Override protected abstract BiDoubleArrayVector thisVector_();
    @Override protected abstract BiDoubleArrayVector newVector_(int aSize);
}
