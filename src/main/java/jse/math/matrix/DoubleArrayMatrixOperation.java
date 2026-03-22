package jse.math.matrix;

import jse.math.IDataShell;
import jse.math.operation.ARRAY;
import jse.math.operation.DATA;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleUnaryOperator;

/**
 * 对于内部含有 double[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class DoubleArrayMatrixOperation extends AbstractMatrixOperation {
    /** 通用的一些运算 */
    @Override public void plus2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebePlus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void minus2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMinus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void lminus2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLMinus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void multiply2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMultiply2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void div2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeDiv2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void ldiv2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLDiv2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void mod2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMod2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMod2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void lmod2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMod2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLMod2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void operate2this(IMatrix aRHS, DoubleBinaryOperator aOpt) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize(), aOpt);
        else DATA.ebeDo2This(rThis::setIteratorCol, aRHS::iteratorCol, aOpt);
    }
    
    @Override public void plus2this     (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapPlus2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void minus2this    (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMinus2This   (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void lminus2this   (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLMinus2This  (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void multiply2this (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMultiply2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void div2this      (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapDiv2This     (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void ldiv2this     (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLDiv2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void mod2this      (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMod2This     (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void lmod2this     (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLMod2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void map2this      (DoubleUnaryOperator aOpt) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapDo2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aOpt);}
    
    @Override public IMatrix abs() {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_();
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapAbs2Dest(tDataL, tThis.internalDataShift(), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapAbs2Dest(tThis::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public void abs2this() {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapAbs2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    @Override public IMatrix negative() {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_();
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapNegative2Dest(tDataL, tThis.internalDataShift(), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapNegative2Dest(tThis::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public void negative2this() {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapNegative2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    
    /** 补充的一些运算 */
    @Override public void plus2dest(IMatrix aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        ebeCheck(tThis.nrows(), tThis.ncols(), aRHS.nrows(), aRHS.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebePlus2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebePlus2Dest(tThis::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);
    }
    @Override public void minus2dest(IMatrix aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        ebeCheck(tThis.nrows(), tThis.ncols(), aRHS.nrows(), aRHS.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMinus2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMinus2Dest(tThis::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);
    }
    @Override public void lminus2dest(IMatrix aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        ebeCheck(tThis.nrows(), tThis.ncols(), aRHS.nrows(), aRHS.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataR, IDataShell.internalDataShift(aRHS), tThis.internalData(), tThis.internalDataShift(), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMinus2Dest(aRHS::iteratorCol, tThis::iteratorCol, rDest::setIteratorCol);
    }
    @Override public void multiply2dest(IMatrix aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        ebeCheck(tThis.nrows(), tThis.ncols(), aRHS.nrows(), aRHS.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMultiply2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMultiply2Dest(tThis::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);
    }
    @Override public void div2dest(IMatrix aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        ebeCheck(tThis.nrows(), tThis.ncols(), aRHS.nrows(), aRHS.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeDiv2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeDiv2Dest(tThis::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);
    }
    @Override public void ldiv2dest(IMatrix aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        ebeCheck(tThis.nrows(), tThis.ncols(), aRHS.nrows(), aRHS.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataR, IDataShell.internalDataShift(aRHS), tThis.internalData(), tThis.internalDataShift(), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeDiv2Dest(aRHS::iteratorCol, tThis::iteratorCol, rDest::setIteratorCol);
    }
    @Override public void mod2dest(IMatrix aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        ebeCheck(tThis.nrows(), tThis.ncols(), aRHS.nrows(), aRHS.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMod2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMod2Dest(tThis::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol);
    }
    @Override public void lmod2dest(IMatrix aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        ebeCheck(tThis.nrows(), tThis.ncols(), aRHS.nrows(), aRHS.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMod2Dest(tDataR, IDataShell.internalDataShift(aRHS), tThis.internalData(), tThis.internalDataShift(), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMod2Dest(aRHS::iteratorCol, tThis::iteratorCol, rDest::setIteratorCol);
    }
    @Override public void operate2dest(IMatrix aRHS, IMatrix rDest, DoubleBinaryOperator aOpt) {
        DoubleArrayMatrix tThis = thisMatrix_();
        ebeCheck(tThis.nrows(), tThis.ncols(), aRHS.nrows(), aRHS.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeDo2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize(), aOpt);
        else DATA.ebeDo2Dest(tThis::iteratorCol, aRHS::iteratorCol, rDest::setIteratorCol, aOpt);
    }
    
    @Override public void plus2dest(double aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        mapCheck(tThis.nrows(), tThis.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapPlus2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapPlus2Dest(tThis::iteratorCol, aRHS, rDest::setIteratorCol);
    }
    @Override public void minus2dest(double aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        mapCheck(tThis.nrows(), tThis.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapMinus2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapMinus2Dest(tThis::iteratorCol, aRHS, rDest::setIteratorCol);
    }
    @Override public void lminus2dest(double aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        mapCheck(tThis.nrows(), tThis.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapLMinus2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapLMinus2Dest(tThis::iteratorCol, aRHS, rDest::setIteratorCol);
    }
    @Override public void multiply2dest(double aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        mapCheck(tThis.nrows(), tThis.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapMultiply2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapMultiply2Dest(tThis::iteratorCol, aRHS, rDest::setIteratorCol);
    }
    @Override public void div2dest(double aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        mapCheck(tThis.nrows(), tThis.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapDiv2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapDiv2Dest(tThis::iteratorCol, aRHS, rDest::setIteratorCol);
    }
    @Override public void ldiv2dest(double aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        mapCheck(tThis.nrows(), tThis.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapLDiv2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapLDiv2Dest(tThis::iteratorCol, aRHS, rDest::setIteratorCol);
    }
    @Override public void mod2dest(double aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        mapCheck(tThis.nrows(), tThis.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapMod2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapMod2Dest(tThis::iteratorCol, aRHS, rDest::setIteratorCol);
    }
    @Override public void lmod2dest(double aRHS, IMatrix rDest) {
        DoubleArrayMatrix tThis = thisMatrix_();
        mapCheck(tThis.nrows(), tThis.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapLMod2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapLMod2Dest(tThis::iteratorCol, aRHS, rDest::setIteratorCol);
    }
    @Override public void map2dest(IMatrix rDest, DoubleUnaryOperator aOpt) {
        DoubleArrayMatrix tThis = thisMatrix_();
        mapCheck(tThis.nrows(), tThis.ncols(), rDest.nrows(), rDest.ncols());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapDo2Dest(tThis.internalData(), tThis.internalDataShift(), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize(), aOpt);
        else DATA.mapDo2Dest(tThis::iteratorCol, rDest::setIteratorCol, aOpt);
    }
    
    
    @Override public void fill          (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapFill2This(rMatrix.internalData(), rMatrix.internalDataShift(), aRHS, rMatrix.internalDataSize());}
    @Override public void fill          (IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        ebeCheck(rThis.nrows(), rThis.ncols(), aRHS.nrows(), aRHS.ncols());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    
    @Override public double sum () {DoubleArrayMatrix rThis = thisMatrix_(); return ARRAY.sumOfThis (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    @Override public double mean() {DoubleArrayMatrix rThis = thisMatrix_(); return ARRAY.meanOfThis(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    @Override public double max () {DoubleArrayMatrix rThis = thisMatrix_(); return ARRAY.maxOfThis (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    @Override public double min () {DoubleArrayMatrix rThis = thisMatrix_(); return ARRAY.minOfThis (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    
    
    /** 方便内部使用，减少一些重复代码 */
    private DoubleArrayMatrix newMatrix_() {
        final DoubleArrayMatrix tThis = thisMatrix_();
        return newMatrix_(tThis.nrows(), tThis.ncols());
    }
    private static double @Nullable [] getIfHasSameOrderData_(IMatrix aThis, IMatrix aData) {
        if (aThis instanceof IDataShell) {
            Object tData = ((IDataShell<?>)aThis).getIfHasSameOrderData(aData);
            return (tData instanceof double[]) ? (double[])tData : null;
        } else {
            return null;
        }
    }
    
    /** stuff to override */
    @Override protected abstract DoubleArrayMatrix thisMatrix_();
    @Override protected abstract DoubleArrayMatrix newMatrix_(int aRowNum, int aColNum);
}
