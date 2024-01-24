package jtool.math.matrix;

import jtool.code.functional.IBinaryFullOperator;
import jtool.code.functional.IUnaryFullOperator;
import jtool.math.ComplexDouble;
import jtool.math.IComplexDouble;
import jtool.math.IDataShell;
import jtool.math.operation.ARRAY;
import jtool.math.operation.DATA;

public abstract class BiDoubleArrayMatrixOperation extends AbstractComplexMatrixOperation {
    /** 通用的一些运算 */
    @Override public IComplexMatrix plus(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[][] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebePlus2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.ebePlus2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix minus(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[][] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.ebeMinus2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix lminus(IComplexMatrix aRHS) {
        final BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[][] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataR, IDataShell.internalDataShift(aRHS), tDataL, tThis.internalDataShift(), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.ebeMinus2Dest(aRHS::iteratorCol, tThis::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix multiply(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[][] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMultiply2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.ebeMultiply2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix div(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[][] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.ebeDiv2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix ldiv(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[][] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataR, IDataShell.internalDataShift(aRHS), tDataL, tThis.internalDataShift(), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.ebeDiv2Dest(aRHS::iteratorCol, tThis::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix operate(IComplexMatrix aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[][] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize(), aOpt);
        else DATA.ebeDo2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol, aOpt);
        return rMatrix;
    }
    
    @Override public IComplexMatrix plus(IComplexDouble aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapPlus2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapPlus2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix minus(IComplexDouble aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapMinus2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix lminus(IComplexDouble aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapLMinus2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix multiply(IComplexDouble aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMultiply2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapMultiply2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix div(IComplexDouble aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapDiv2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix ldiv(IComplexDouble aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapLDiv2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix plus(double aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapPlus2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapPlus2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix minus(double aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapMinus2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix lminus(double aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMinus2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapLMinus2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix multiply(double aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMultiply2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapMultiply2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix div(double aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapDiv2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix ldiv(double aRHS) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLDiv2Dest(tDataL, tThis.internalDataShift(), aRHS, rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapLDiv2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IComplexMatrix map(IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDo2Dest(tDataL, tThis.internalDataShift(), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize(), aOpt);
        else DATA.mapDo2Dest(tThis::iteratorCol, rMatrix::setIteratorCol, aOpt);
        return rMatrix;
    }
    
    @Override public void plus2this(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix rThis = thisMatrix_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebePlus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void minus2this(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix rThis = thisMatrix_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMinus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void lminus2this(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix rThis = thisMatrix_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLMinus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void multiply2this(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix rThis = thisMatrix_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMultiply2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void div2this(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix rThis = thisMatrix_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeDiv2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void ldiv2this(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix rThis = thisMatrix_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLDiv2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void operate2this(IComplexMatrix aRHS, IBinaryFullOperator<? extends IComplexDouble, ? super ComplexDouble, ? super ComplexDouble> aOpt) {
        BiDoubleArrayMatrix rThis = thisMatrix_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize(), aOpt);
        else DATA.ebeDo2This(rThis::setIteratorCol, aRHS::iteratorCol, aOpt);
    }
    
    @Override public void plus2this     (IComplexDouble aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapPlus2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void minus2this    (IComplexDouble aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMinus2This   (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void lminus2this   (IComplexDouble aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLMinus2This  (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void multiply2this (IComplexDouble aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMultiply2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void div2this      (IComplexDouble aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapDiv2This     (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void ldiv2this     (IComplexDouble aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLDiv2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void plus2this     (double         aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapPlus2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void minus2this    (double         aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMinus2This   (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void lminus2this   (double         aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLMinus2This  (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void multiply2this (double         aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMultiply2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void div2this      (double         aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapDiv2This     (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void ldiv2this     (double         aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLDiv2This    (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void map2this(IUnaryFullOperator<? extends IComplexDouble, ? super ComplexDouble> aOpt) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapDo2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aOpt);}
    
    @Override public IComplexMatrix negative() {
        BiDoubleArrayMatrix tThis = thisMatrix_();
        BiDoubleArrayMatrix rMatrix = newMatrix_();
        double[][] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapNegative2Dest(tDataL, tThis.internalDataShift(), rMatrix.internalData(), rMatrix.internalDataShift(), rMatrix.internalDataSize());
        else DATA.mapNegative2Dest(tThis::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public void negative2this() {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapNegative2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    
    @Override public void fill          (IComplexDouble aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill          (double aRHS) {BiDoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill          (IComplexMatrix aRHS) {
        final BiDoubleArrayMatrix rThis = thisMatrix_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private BiDoubleArrayMatrix newMatrix_() {
        final BiDoubleArrayMatrix tThis = thisMatrix_();
        return newMatrix_(tThis.rowNumber(), tThis.columnNumber());
    }
    
    /** stuff to override */
    @Override protected abstract BiDoubleArrayMatrix thisMatrix_();
    @Override protected abstract BiDoubleArrayMatrix newMatrix_(int aRowNum, int aColNum);
}
