package com.jtool.math.vector;

import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.code.operator.IDoubleOperator2;
import com.jtool.math.IDataShell;
import com.jtool.math.operation.ARRAY;
import com.jtool.math.operation.DATA;

/**
 * 对于内部含有 double[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class DoubleArrayVectorOperation extends AbstractVectorOperation {
    /** 通用的一些运算 */
    @Override public IVector ebePlus(IVectorGetter aLHS, IVectorGetter aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebePlus2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebePlus2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator());
        return rVector;
    }
    @Override public IVector ebeMinus(IVectorGetter aLHS, IVectorGetter aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeMinus2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator());
        return rVector;
    }
    @Override public IVector ebeMultiply(IVectorGetter aLHS, IVectorGetter aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMultiply2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeMultiply2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator());
        return rVector;
    }
    @Override public IVector ebeDiv(IVectorGetter aLHS, IVectorGetter aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeDiv2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator());
        return rVector;
    }
    @Override public IVector ebeMod(IVectorGetter aLHS, IVectorGetter aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMod2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeMod2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator());
        return rVector;
    }
    @Override public IVector ebeDo(IVectorGetter aLHS, IVectorGetter aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);
        else DATA.ebeDo2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator(), aOpt);
        return rVector;
    }
    
    @Override public IVector mapPlus(IVectorGetter aLHS, double aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapPlus2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapPlus2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public IVector mapMinus(IVectorGetter aLHS, double aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapMinus2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapMinus2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public IVector mapLMinus(IVectorGetter aLHS, double aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapLMinus2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapLMinus2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public IVector mapMultiply(IVectorGetter aLHS, double aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapMultiply2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapMultiply2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public IVector mapDiv(IVectorGetter aLHS, double aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapDiv2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapDiv2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public IVector mapLDiv(IVectorGetter aLHS, double aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapLDiv2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapLDiv2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public IVector mapMod(IVectorGetter aLHS, double aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapMod2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapMod2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public IVector mapLMod(IVectorGetter aLHS, double aRHS) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapLMod2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapLMod2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public IVector mapDo(IVectorGetter aLHS, IDoubleOperator1 aOpt) {
        DoubleArrayVector rVector = newVector_(newVectorSize_(aLHS));
        double[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapDo2Dest_(tDataL, IDataShell.shiftSize(aLHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);
        else DATA.mapDo2Dest_(rVector.iteratorOf(aLHS), rVector.setIterator(), aOpt);
        return rVector;
    }
    
    @Override public void ebePlus2this(IVectorGetter aRHS) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebePlus2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeMinus2this(IVectorGetter aRHS) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeMinus2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeLMinus2this(IVectorGetter aRHS) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeLMinus2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeMultiply2this(IVectorGetter aRHS) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeMultiply2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeDiv2this(IVectorGetter aRHS) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeDiv2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeLDiv2this(IVectorGetter aRHS) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeLDiv2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeMod2this(IVectorGetter aRHS) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMod2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeMod2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeLMod2this(IVectorGetter aRHS) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMod2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeLMod2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeDo2this(IVectorGetter aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize(), aOpt);
        else DATA.ebeDo2this_(rVector.setIterator(), rVector.iteratorOf(aRHS), aOpt);
    }
    
    @Override public void mapPlus2this      (double aRHS) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapPlus2this_       (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapMinus2this     (double aRHS) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapMinus2this_      (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapLMinus2this    (double aRHS) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapLMinus2this_     (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapMultiply2this  (double aRHS) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapMultiply2this_   (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapDiv2this       (double aRHS) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapDiv2this_        (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapLDiv2this      (double aRHS) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapLDiv2this_       (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapMod2this       (double aRHS) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapMod2this_        (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapLMod2this      (double aRHS) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapLMod2this_       (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapDo2this        (IDoubleOperator1 aOpt) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapDo2this_(rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);}
    
    @Override public void mapFill2this      (double aRHS) {DoubleArrayVector rVector = thisVector_(); ARRAY.mapFill2this_(rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void ebeFill2this      (IVectorGetter aRHS) {
        DoubleArrayVector rVector = thisVector_();
        double[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeFill2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    
    @Override public double sum    () {DoubleArrayVector rVector = thisVector_(); return ARRAY.sumOfThis_       (rVector.getData(), rVector.shiftSize(), rVector.dataSize());}
    @Override public double mean   () {DoubleArrayVector rVector = thisVector_(); return ARRAY.meanOfThis_      (rVector.getData(), rVector.shiftSize(), rVector.dataSize());}
    @Override public double product() {DoubleArrayVector rVector = thisVector_(); return ARRAY.productOfThis_   (rVector.getData(), rVector.shiftSize(), rVector.dataSize());}
    @Override public double max    () {DoubleArrayVector rVector = thisVector_(); return ARRAY.maxOfThis_       (rVector.getData(), rVector.shiftSize(), rVector.dataSize());}
    @Override public double min    () {DoubleArrayVector rVector = thisVector_(); return ARRAY.minOfThis_       (rVector.getData(), rVector.shiftSize(), rVector.dataSize());}
    
    
    /** stuff to override */
    @Override protected abstract DoubleArrayVector thisVector_();
    @Override protected abstract DoubleArrayVector newVector_(int aSize);
}
