package jtool.math.vector;

import jtool.code.functional.IOperator1;
import jtool.code.functional.IOperator2;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.IDataShell;
import jtool.math.operation.ARRAY;
import jtool.math.operation.DATA;

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
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebePlus2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebePlus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector minus(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector lminus(IComplexVector aRHS) {
        final BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataR, IDataShell.internalDataShift(aRHS), tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMinus2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IComplexVector multiply(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMultiply2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMultiply2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector div(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IComplexVector ldiv(IComplexVector aRHS) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[][] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataR, IDataShell.internalDataShift(aRHS), tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeDiv2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IComplexVector operate(IComplexVector aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        BiDoubleArrayVector tThis = thisVector_();
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
    @Override public IComplexVector map(IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        BiDoubleArrayVector tThis = thisVector_();
        BiDoubleArrayVector rVector = newVector_();
        double[][] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDo2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize(), aOpt);
        else DATA.mapDo2Dest(tThis, rVector, aOpt);
        return rVector;
    }
    
    @Override public void plus2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebePlus2This(rThis, aRHS);
    }
    @Override public void minus2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMinus2This(rThis, aRHS);
    }
    @Override public void lminus2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLMinus2This(rThis, aRHS);
    }
    @Override public void multiply2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMultiply2This(rThis, aRHS);
    }
    @Override public void div2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeDiv2This(rThis, aRHS);
    }
    @Override public void ldiv2this(IComplexVector aRHS) {
        BiDoubleArrayVector rThis = thisVector_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLDiv2This(rThis, aRHS);
    }
    @Override public void operate2this(IComplexVector aRHS, IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        BiDoubleArrayVector rThis = thisVector_();
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
    @Override public void map2this      (IOperator1<? extends IComplexDouble, ? super ComplexDouble> aOpt) {BiDoubleArrayVector rThis = thisVector_(); ARRAY.mapDo2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aOpt);}
    
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
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    
    @Override public ComplexDouble sum () {BiDoubleArrayVector tThis = thisVector_(); return ARRAY.sumOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public ComplexDouble mean() {BiDoubleArrayVector tThis = thisVector_(); return ARRAY.meanOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public ComplexDouble prod() {BiDoubleArrayVector tThis = thisVector_(); return ARRAY.prodOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public ComplexDouble stat(IOperator2<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {BiDoubleArrayVector tThis = thisVector_(); return ARRAY.statOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize(), aOpt);}
    
    
    /** 向量的一些额外的运算 */
    @Override public ComplexDouble dot(IComplexVector aRHS) {
        final BiDoubleArrayVector tThis = thisVector_();
        final double[][] tDataR = tThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            final double[][] tDataL = tThis.internalData();
            final int tShiftL = tThis.internalDataShift();
            final int tEndL = tThis.internalDataSize() + tShiftL;
            final int tShiftR = IDataShell.internalDataShift(aRHS);
            
            final double[] tRealDataL = tDataL[0], tImagDataL = tDataL[1];
            final double[] tRealDataR = tDataR[0], tImagDataR = tDataR[1];
            
            ComplexDouble rDot = new ComplexDouble();
            if (tShiftL == tShiftR) {
                for (int i = tShiftL; i < tEndL; ++i) {
                    double lReal = tRealDataL[i], lImag = tImagDataL[i];
                    double rReal = tRealDataR[i], rImag = tImagDataR[i];
                    rDot.mReal += (lReal*rReal + lImag*rImag);
                    rDot.mImag += (lImag*rReal - lReal*rImag);
                }
            } else {
                for (int i = tShiftL, j = tShiftR; i < tEndL; ++i, ++j) {
                    double lReal = tRealDataL[i], lImag = tImagDataL[i];
                    double rReal = tRealDataR[j], rImag = tImagDataR[j];
                    rDot.mReal += (lReal*rReal + lImag*rImag);
                    rDot.mImag += (lImag*rReal - lReal*rImag);
                }
            }
            return rDot;
        } else {
            return super.dot(aRHS);
        }
    }
    @Override public double dot() {
        final BiDoubleArrayVector tThis = thisVector_();
        final double[][] tData = tThis.internalData();
        final int tShift = tThis.internalDataShift();
        final int tEnd = tThis.internalDataSize() + tShift;
        
        final double[] tRealData = tData[0], tImagData = tData[1];
        
        double rDot = 0.0;
        for (int i = tShift; i < tEnd; ++i) {
            double tReal = tRealData[i], tImag = tImagData[i];
            rDot += (tReal*tReal + tImag*tImag);
        }
        return rDot;
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private BiDoubleArrayVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    @Override protected abstract BiDoubleArrayVector thisVector_();
    @Override protected abstract BiDoubleArrayVector newVector_(int aSize);
}
