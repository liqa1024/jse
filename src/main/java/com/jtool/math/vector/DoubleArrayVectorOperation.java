package com.jtool.math.vector;

import com.jtool.code.functional.IDoubleOperator1;
import com.jtool.code.functional.IDoubleOperator2;
import com.jtool.math.IDataShell;
import com.jtool.math.operation.ARRAY;
import com.jtool.math.operation.DATA;

/**
 * 对于内部含有 double[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class DoubleArrayVectorOperation extends AbstractVectorOperation {
    /** 通用的一些运算 */
    @Override public IVector plus(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebePlus2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebePlus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector minus(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector lminus(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataR, IDataShell.shiftSize(aRHS), tDataL, tThis.shiftSize(), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeMinus2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IVector multiply(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMultiply2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeMultiply2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector div(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector ldiv(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataR, IDataShell.shiftSize(aRHS), tDataL, tThis.shiftSize(), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeDiv2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IVector mod(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMod2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeMod2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector lmod(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMod2Dest(tDataR, IDataShell.shiftSize(aRHS), tDataL, tThis.shiftSize(), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeMod2Dest(aRHS, tThis, rVector);
        return rVector;
    }
    @Override public IVector operate(IVector aRHS, IDoubleOperator2 aOpt) {
        final DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);
        else DATA.ebeDo2Dest(tThis, aRHS, rVector, aOpt);
        return rVector;
    }
    
    @Override public IVector plus(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapPlus2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapPlus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector minus(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMinus2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector lminus(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMinus2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapLMinus2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector multiply(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMultiply2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapMultiply2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector div(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDiv2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector ldiv(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLDiv2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapLDiv2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector mod(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMod2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapMod2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector lmod(double aRHS) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMod2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapLMod2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public IVector map(IDoubleOperator1 aOpt) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDo2Dest(tDataL, tThis.shiftSize(), rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);
        else DATA.mapDo2Dest(tThis, rVector, aOpt);
        return rVector;
    }
    
    @Override public void plus2this(IVector aRHS) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebePlus2This(rThis, aRHS);
    }
    @Override public void minus2this(IVector aRHS) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeMinus2This(rThis, aRHS);
    }
    @Override public void lminus2this(IVector aRHS) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeLMinus2This(rThis, aRHS);
    }
    @Override public void multiply2this(IVector aRHS) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeMultiply2This(rThis, aRHS);
    }
    @Override public void div2this(IVector aRHS) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeDiv2This(rThis, aRHS);
    }
    @Override public void ldiv2this(IVector aRHS) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeLDiv2This(rThis, aRHS);
    }
    @Override public void mod2this(IVector aRHS) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMod2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeMod2This(rThis, aRHS);
    }
    @Override public void lmod2this(IVector aRHS) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMod2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeLMod2This(rThis, aRHS);
    }
    @Override public void operate2this(IVector aRHS, IDoubleOperator2 aOpt) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize(), aOpt);
        else DATA.ebeDo2This(rThis, aRHS, aOpt);
    }
    
    @Override public void plus2this     (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapPlus2This    (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void minus2this    (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapMinus2This   (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void lminus2this   (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapLMinus2This  (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void multiply2this (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapMultiply2This(rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void div2this      (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapDiv2This     (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void ldiv2this     (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapLDiv2This    (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void mod2this      (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapMod2This     (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void lmod2this     (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapLMod2This    (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void map2this      (IDoubleOperator1 aOpt) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapDo2This(rThis.getData(), rThis.shiftSize(), rThis.dataSize(), aOpt);}
    
    @Override public void fill          (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void fill          (IVector aRHS) {
        final DoubleArrayVector rThis = thisVector_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    
    @Override public double sum ()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.sumOfThis (tThis.getData(), tThis.shiftSize(), tThis.dataSize()      );}
    @Override public double mean()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.meanOfThis(tThis.getData(), tThis.shiftSize(), tThis.dataSize()      );}
    @Override public double prod()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.prodOfThis(tThis.getData(), tThis.shiftSize(), tThis.dataSize()      );}
    @Override public double max ()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.maxOfThis (tThis.getData(), tThis.shiftSize(), tThis.dataSize()      );}
    @Override public double min ()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.minOfThis (tThis.getData(), tThis.shiftSize(), tThis.dataSize()      );}
    @Override public double stat(IDoubleOperator2 aOpt) {DoubleArrayVector tThis = thisVector_(); return ARRAY.statOfThis(tThis.getData(), tThis.shiftSize(), tThis.dataSize(), aOpt);}
    
    
    @Override public IVector cumsum() {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cumsum2Dest(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize());
        else DATA.cumsum2Dest(tThis, rVector);
        return rVector;
    }
    @Override public IVector cummean() {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cummean2Dest(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize());
        else DATA.cummean2Dest(tThis, rVector);
        return rVector;
    }
    @Override public IVector cumprod() {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cumprod2Dest(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize());
        else DATA.cumprod2Dest(tThis, rVector);
        return rVector;
    }
    @Override public IVector cummax() {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cummax2Dest(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize());
        else DATA.cummax2Dest(tThis, rVector);
        return rVector;
    }
    @Override public IVector cummin() {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cummin2Dest(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize());
        else DATA.cummin2Dest(tThis, rVector);
        return rVector;
    }
    @Override public IVector cumstat(IDoubleOperator2 aOpt) {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_(tThis.size());
        double[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cumstat2Dest(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize(), aOpt);
        else DATA.cumstat2Dest(tThis, rVector, aOpt);
        return rVector;
    }
    
    
    /** 向量的一些额外的运算 */
    @Override public double dot(IVector aRHS) {
        final DoubleArrayVector tThis = thisVector_();
        final double[] tDataR = tThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) {
            final double[] tDataL = tThis.getData();
            final int tShiftL = tThis.shiftSize();
            final int tEndL = tThis.dataSize() + tShiftL;
            final int tShiftR = IDataShell.shiftSize(aRHS);
            
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
        final double[] tData = tThis.getData();
        final int tShift = tThis.shiftSize();
        final int tEnd = tThis.dataSize() + tShift;
        
        double rDot = 0.0;
        for (int i = tShift; i < tEnd; ++i) {
            double tValue = tData[i];
            rDot += tValue*tValue;
        }
        return rDot;
    }
    
    /** stuff to override */
    @Override protected abstract DoubleArrayVector thisVector_();
    @Override protected abstract DoubleArrayVector newVector_(int aSize);
}
