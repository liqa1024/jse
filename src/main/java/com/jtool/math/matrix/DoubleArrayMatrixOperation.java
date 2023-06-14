package com.jtool.math.matrix;

import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.code.operator.IDoubleOperator2;
import com.jtool.math.IDataShell;
import com.jtool.math.operation.ARRAY;
import com.jtool.math.operation.DATA;

/**
 * 对于内部含有 double[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class DoubleArrayMatrixOperation extends AbstractMatrixOperation {
    /** 通用的一些运算 */
    @Override public IMatrix ebePlus(IMatrixGetter aLHS, IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebePlus2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebePlus2Dest_(rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix ebeMinus(IMatrixGetter aLHS, IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeMinus2Dest_(rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix ebeMultiply(IMatrixGetter aLHS, IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMultiply2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeMultiply2Dest_(rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix ebeDiv(IMatrixGetter aLHS, IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeDiv2Dest_(rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix ebeMod(IMatrixGetter aLHS, IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMod2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeMod2Dest_(rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix ebeDo(IMatrixGetter aLHS, IMatrixGetter aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS, aRHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize(), aOpt);
        else DATA.ebeDo2Dest_(rMatrix.colIteratorOf(aLHS), rMatrix.colIteratorOf(aRHS), rMatrix.colSetIterator(), aOpt);
        return rMatrix;
    }
    
    @Override public IMatrix mapPlus(IMatrixGetter aLHS, double aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapPlus2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapPlus2Dest_(rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix mapMinus(IMatrixGetter aLHS, double aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapMinus2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapMinus2Dest_(rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix mapLMinus(IMatrixGetter aLHS, double aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapLMinus2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapLMinus2Dest_(rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix mapMultiply(IMatrixGetter aLHS, double aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapMultiply2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapMultiply2Dest_(rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix mapDiv(IMatrixGetter aLHS, double aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapDiv2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapDiv2Dest_(rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix mapLDiv(IMatrixGetter aLHS, double aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapLDiv2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapLDiv2Dest_(rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix mapMod(IMatrixGetter aLHS, double aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapMod2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapMod2Dest_(rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix mapLMod(IMatrixGetter aLHS, double aRHS) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapLMod2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapLMod2Dest_(rMatrix.colIteratorOf(aLHS), aRHS, rMatrix.colSetIterator());
        return rMatrix;
    }
    @Override public IMatrix mapDo(IMatrixGetter aLHS, IDoubleOperator1 aOpt) {
        DoubleArrayMatrix rMatrix = newMatrix_(newMatrixSize_(aLHS));
        double[] tDataL = rMatrix.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapDo2Dest_(tDataL, IDataShell.shiftSize(aLHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize(), aOpt);
        else DATA.mapDo2Dest_(rMatrix.colIteratorOf(aLHS), rMatrix.colSetIterator(), aOpt);
        return rMatrix;
    }
    
    @Override public void ebePlus2this(IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize());
        else DATA.ebePlus2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));
    }
    @Override public void ebeMinus2this(IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize());
        else DATA.ebeMinus2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));
    }
    @Override public void ebeLMinus2this(IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize());
        else DATA.ebeLMinus2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));
    }
    @Override public void ebeMultiply2this(IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize());
        else DATA.ebeMultiply2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));
    }
    @Override public void ebeDiv2this(IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize());
        else DATA.ebeDiv2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));
    }
    @Override public void ebeLDiv2this(IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize());
        else DATA.ebeLDiv2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));
    }
    @Override public void ebeMod2this(IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMod2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize());
        else DATA.ebeMod2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));
    }
    @Override public void ebeLMod2this(IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMod2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize());
        else DATA.ebeLMod2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));
    }
    @Override public void ebeDo2this(IMatrixGetter aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize(), aOpt);
        else DATA.ebeDo2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS), aOpt);
    }
    
    @Override public void mapPlus2this      (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapPlus2this_       (rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void mapMinus2this     (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapMinus2this_      (rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void mapLMinus2this    (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapLMinus2this_     (rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void mapMultiply2this  (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapMultiply2this_   (rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void mapDiv2this       (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapDiv2this_        (rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void mapLDiv2this      (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapLDiv2this_       (rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void mapMod2this       (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapMod2this_        (rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void mapLMod2this      (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapLMod2this_       (rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void mapDo2this        (IDoubleOperator1 aOpt) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapDo2this_(rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize(), aOpt);}
    
    @Override public void mapFill2this      (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapFill2this_(rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void ebeFill2this      (IMatrixGetter aRHS) {
        DoubleArrayMatrix rMatrix = thisMatrix_();
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2this_(rMatrix.getData(), rMatrix.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.dataSize());
        else DATA.ebeFill2this_(rMatrix.colSetIterator(), rMatrix.colIteratorOf(aRHS));
    }
    
    @Override public double sum () {DoubleArrayMatrix rMatrix = thisMatrix_(); return ARRAY.sumOfThis_  (rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());}
    @Override public double mean() {DoubleArrayMatrix rMatrix = thisMatrix_(); return ARRAY.meanOfThis_ (rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());}
    @Override public double max () {DoubleArrayMatrix rMatrix = thisMatrix_(); return ARRAY.maxOfThis_  (rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());}
    @Override public double min () {DoubleArrayMatrix rMatrix = thisMatrix_(); return ARRAY.minOfThis_  (rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());}
    
    
    /** stuff to override */
    @Override protected abstract DoubleArrayMatrix thisMatrix_();
    @Override protected abstract DoubleArrayMatrix newMatrix_(IMatrix.ISize aSize);
}
