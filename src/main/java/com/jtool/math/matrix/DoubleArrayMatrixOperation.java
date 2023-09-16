package com.jtool.math.matrix;

import com.jtool.code.functional.IDoubleOperator1;
import com.jtool.code.functional.IDoubleOperator2;
import com.jtool.math.IDataShell;
import com.jtool.math.operation.ARRAY;
import com.jtool.math.operation.DATA;

/**
 * 对于内部含有 double[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class DoubleArrayMatrixOperation extends AbstractMatrixOperation {
    /** 通用的一些运算 */
    @Override public IMatrix plus(IMatrix aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebePlus2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebePlus2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix minus(IMatrix aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeMinus2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix lminus(IMatrix aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataR, IDataShell.shiftSize(aRHS), tDataL, tThis.shiftSize(), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeMinus2Dest(aRHS::iteratorCol, tThis::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix multiply(IMatrix aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMultiply2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeMultiply2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix div(IMatrix aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeDiv2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix ldiv(IMatrix aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataR, IDataShell.shiftSize(aRHS), tDataL, tThis.shiftSize(), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeDiv2Dest(aRHS::iteratorCol, tThis::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix mod(IMatrix aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMod2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeMod2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix lmod(IMatrix aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeMod2Dest(tDataR, IDataShell.shiftSize(aRHS), tDataL, tThis.shiftSize(), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.ebeMod2Dest(aRHS::iteratorCol, tThis::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix operate(IMatrix aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        double[] tDataR = rMatrix.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize(), aOpt);
        else DATA.ebeDo2Dest(tThis::iteratorCol, aRHS::iteratorCol, rMatrix::setIteratorCol, aOpt);
        return rMatrix;
    }
    
    @Override public IMatrix plus(double aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapPlus2Dest(tDataL, tThis.shiftSize(), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapPlus2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix minus(double aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMinus2Dest(tDataL, tThis.shiftSize(), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapMinus2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix lminus(double aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMinus2Dest(tDataL, tThis.shiftSize(), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapLMinus2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix multiply(double aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMultiply2Dest(tDataL, tThis.shiftSize(), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapMultiply2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix div(double aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDiv2Dest(tDataL, tThis.shiftSize(), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapDiv2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix ldiv(double aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLDiv2Dest(tDataL, tThis.shiftSize(), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapLDiv2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix mod(double aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapMod2Dest(tDataL, tThis.shiftSize(), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapMod2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix lmod(double aRHS) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapLMod2Dest(tDataL, tThis.shiftSize(), aRHS, rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapLMod2Dest(tThis::iteratorCol, aRHS, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public IMatrix map(IDoubleOperator1 aOpt) {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDo2Dest(tDataL, tThis.shiftSize(), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize(), aOpt);
        else DATA.mapDo2Dest(tThis::iteratorCol, rMatrix::setIteratorCol, aOpt);
        return rMatrix;
    }
    
    @Override public void plus2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebePlus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void minus2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeMinus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void lminus2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeLMinus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void multiply2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeMultiply2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void div2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeDiv2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void ldiv2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeLDiv2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void mod2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMod2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeMod2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void lmod2this(IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMod2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeLMod2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    @Override public void operate2this(IMatrix aRHS, IDoubleOperator2 aOpt) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize(), aOpt);
        else DATA.ebeDo2This(rThis::setIteratorCol, aRHS::iteratorCol, aOpt);
    }
    
    @Override public void plus2this     (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapPlus2This       (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void minus2this    (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMinus2This      (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void lminus2this   (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLMinus2This     (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void multiply2this (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMultiply2This   (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void div2this      (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapDiv2This        (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void ldiv2this     (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLDiv2This       (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void mod2this      (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapMod2This        (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void lmod2this     (double aRHS) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapLMod2This       (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void map2this      (IDoubleOperator1 aOpt) {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapDo2This(rThis.getData(), rThis.shiftSize(), rThis.dataSize(), aOpt);}
    
    @Override public IMatrix negative() {
        DoubleArrayMatrix tThis = thisMatrix_();
        DoubleArrayMatrix rMatrix = newMatrix_(tThis.size());
        double[] tDataL = rMatrix.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapNegative2Dest(tDataL, tThis.shiftSize(), rMatrix.getData(), rMatrix.shiftSize(), rMatrix.dataSize());
        else DATA.mapNegative2Dest(tThis::iteratorCol, rMatrix::setIteratorCol);
        return rMatrix;
    }
    @Override public void negative2this() {DoubleArrayMatrix rThis = thisMatrix_(); ARRAY.mapNegative2This(rThis.getData(), rThis.shiftSize(), rThis.dataSize());}
    
    @Override public void fill          (double aRHS) {DoubleArrayMatrix rMatrix = thisMatrix_(); ARRAY.mapFill2This(rMatrix.getData(), rMatrix.shiftSize(), aRHS, rMatrix.dataSize());}
    @Override public void fill          (IMatrix aRHS) {
        DoubleArrayMatrix rThis = thisMatrix_();
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeFill2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    
    @Override public double sum () {DoubleArrayMatrix rThis = thisMatrix_(); return ARRAY.sumOfThis (rThis.getData(), rThis.shiftSize(), rThis.dataSize());}
    @Override public double mean() {DoubleArrayMatrix rThis = thisMatrix_(); return ARRAY.meanOfThis(rThis.getData(), rThis.shiftSize(), rThis.dataSize());}
    @Override public double max () {DoubleArrayMatrix rThis = thisMatrix_(); return ARRAY.maxOfThis (rThis.getData(), rThis.shiftSize(), rThis.dataSize());}
    @Override public double min () {DoubleArrayMatrix rThis = thisMatrix_(); return ARRAY.minOfThis (rThis.getData(), rThis.shiftSize(), rThis.dataSize());}
    
    
    /** stuff to override */
    @Override protected abstract DoubleArrayMatrix thisMatrix_();
    @Override protected abstract DoubleArrayMatrix newMatrix_(IMatrix.ISize aSize);
}
