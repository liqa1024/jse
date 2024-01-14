package jtool.math.vector;

import jtool.code.functional.IDoubleOperator1;
import jtool.code.functional.IDoubleOperator2;
import jtool.math.IDataShell;
import jtool.math.operation.ARRAY;
import jtool.math.operation.DATA;

/**
 * 对于内部含有 double[] 的向量的运算使用专门优化后的函数
 * <p>
 * 目前只优化相同类型的向量之间的运算
 * @author liqa
 */
public abstract class DoubleArrayVectorOperation extends AbstractVectorOperation {
    /** 通用的一些运算 */
    @Override public IVector plus(IVector aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebePlus2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebePlus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector minus(IVector aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector lminus(IVector aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataR, IDataShell.internalDataShift(aRHS), tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMinus2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IVector multiply(IVector aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMultiply2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMultiply2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector div(IVector aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector ldiv(IVector aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataR, IDataShell.internalDataShift(aRHS), tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeDiv2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IVector mod(IVector aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMod2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMod2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector lmod(IVector aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMod2Dest(tDataR, IDataShell.internalDataShift(aRHS), tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeMod2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IVector operate(IVector aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize(), aOpt);
        else DATA.ebeDo2Dest(tThis, aRHS, rVector, aOpt);
        return rVector;
    }
    
    @Override public IVector plus(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapPlus2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapPlus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector minus(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector lminus(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapLMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector multiply(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMultiply2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapMultiply2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector div(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector ldiv(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapLDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector mod(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMod2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapMod2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector lmod(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMod2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapLMod2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector map(IDoubleOperator1 aOpt) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDo2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize(), aOpt);
        else DATA.mapDo2Dest(tThis, rVector, aOpt);
        return rVector;
    }
    
    @Override public void plus2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebePlus2This(rThis, aRHS);
    }
    @Override public void minus2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMinus2This(rThis, aRHS);
    }
    @Override public void lminus2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLMinus2This(rThis, aRHS);
    }
    @Override public void multiply2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMultiply2This(rThis, aRHS);
    }
    @Override public void div2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeDiv2This(rThis, aRHS);
    }
    @Override public void ldiv2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLDiv2This(rThis, aRHS);
    }
    @Override public void mod2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMod2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMod2This(rThis, aRHS);
    }
    @Override public void lmod2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMod2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLMod2This(rThis, aRHS);
    }
    @Override public void operate2this(IVector aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize(), aOpt);
        else DATA.ebeDo2This(rThis, aRHS, aOpt);
    }
    
    @Override public void plus2this     (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapPlus2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void minus2this    (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapMinus2This   (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void lminus2this   (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapLMinus2This  (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void multiply2this (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapMultiply2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void div2this      (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapDiv2This     (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void ldiv2this     (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapLDiv2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void mod2this      (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapMod2This     (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void lmod2this     (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapLMod2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void map2this      (IDoubleOperator1 aOpt) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapDo2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aOpt);}
    
    @Override public IVector negative() {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapNegative2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapNegative2Dest(tThis, rVector);
        return rVector;
    }
    @Override public void negative2this() {DoubleArrayVector rThis = thisVector_(); ARRAY.mapNegative2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    
    @Override public void fill          (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill          (IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    
    @Override public double sum ()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.sumOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double mean()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.meanOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double prod()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.prodOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double max ()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.maxOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double min ()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.minOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double stat(IDoubleOperator2 aOpt) {DoubleArrayVector tThis = thisVector_(); return ARRAY.statOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize(), aOpt);}
    
    
    /** 向量的一些额外的运算 */
    @Override public double dot(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        final double[] tDataR = tThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            final double[] tDataL = tThis.internalData();
            final int tShiftL = tThis.internalDataShift();
            final int tEndL = tThis.internalDataSize() + tShiftL;
            final int tShiftR = IDataShell.internalDataShift(aRHS);
            
            double rDot = 0.0;
            if (tShiftL == tShiftR) for (int i = tShiftL; i < tEndL; ++i) rDot += tDataL[i]*tDataR[i];
            else for (int i = tShiftL, j = tShiftR; i < tEndL; ++i, ++j) rDot += tDataL[i]*tDataR[j];
            return rDot;
        } else {
            return super.dot(aRHS);
        }
    }
    @Override public double dot() {
        final DoubleArrayVector tThis = thisVector_();
        final double[] tData = tThis.internalData();
        final int tShift = tThis.internalDataShift();
        final int tEnd = tThis.internalDataSize() + tShift;
        
        double rDot = 0.0;
        for (int i = tShift; i < tEnd; ++i) {
            double tValue = tData[i];
            rDot += tValue*tValue;
        }
        return rDot;
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private DoubleArrayVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    @Override protected abstract DoubleArrayVector thisVector_();
    @Override protected abstract DoubleArrayVector newVector_(int aSize);
}
