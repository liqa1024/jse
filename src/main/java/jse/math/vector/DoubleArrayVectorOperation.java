package jse.math.vector;

import jse.code.functional.ISwapper;
import jse.math.IDataShell;
import jse.math.operation.ARRAY;
import jse.math.operation.DATA;
import org.jetbrains.annotations.Nullable;

import java.util.function.DoubleBinaryOperator;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.DoubleUnaryOperator;

/**
 * 对于内部含有 double[] 的向量的运算使用专门优化后的函数
 * <p>
 * 目前只优化相同类型的向量之间的运算
 * @author liqa
 */
public abstract class DoubleArrayVectorOperation extends AbstractVectorOperation {
    /** 通用的一些运算 */
    @Override public void plus2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebePlus2This(rThis, aRHS);
    }
    @Override public void minus2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMinus2This(rThis, aRHS);
    }
    @Override public void lminus2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMinus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLMinus2This(rThis, aRHS);
    }
    @Override public void multiply2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMultiply2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMultiply2This(rThis, aRHS);
    }
    @Override public void div2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeDiv2This(rThis, aRHS);
    }
    @Override public void ldiv2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLDiv2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLDiv2This(rThis, aRHS);
    }
    @Override public void mod2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeMod2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeMod2This(rThis, aRHS);
    }
    @Override public void lmod2this(IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeLMod2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeLMod2This(rThis, aRHS);
    }
    @Override public void operate2this(IVector aRHS, DoubleBinaryOperator aOpt) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
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
    @Override public void map2this      (DoubleUnaryOperator aOpt) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapDo2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aOpt);}
    
    @Override public IVector negative() {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapNegative2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapNegative2Dest(tThis, rVector);
        return rVector;
    }
    @Override public void negative2this() {DoubleArrayVector rThis = thisVector_(); ARRAY.mapNegative2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    
    /** 补充的一些运算 */
    @Override public void plus2dest(IVector aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebePlus2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebePlus2Dest(tThis, aRHS, rDest);
    }
    @Override public void minus2dest(IVector aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMinus2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMinus2Dest(tThis, aRHS, rDest);
    }
    @Override public void lminus2dest(IVector aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMinus2Dest(tDataR, IDataShell.internalDataShift(aRHS), tThis.internalData(), tThis.internalDataShift(), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMinus2Dest(aRHS, tThis, rDest);
    }
    @Override public void multiply2dest(IVector aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMultiply2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMultiply2Dest(tThis, aRHS, rDest);
    }
    @Override public void div2dest(IVector aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeDiv2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeDiv2Dest(tThis, aRHS, rDest);
    }
    @Override public void ldiv2dest(IVector aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeDiv2Dest(tDataR, IDataShell.internalDataShift(aRHS), tThis.internalData(), tThis.internalDataShift(), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeDiv2Dest(aRHS, tThis, rDest);
    }
    @Override public void mod2dest(IVector aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMod2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMod2Dest(tThis, aRHS, rDest);
    }
    @Override public void lmod2dest(IVector aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeMod2Dest(tDataR, IDataShell.internalDataShift(aRHS), tThis.internalData(), tThis.internalDataShift(), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.ebeMod2Dest(aRHS, tThis, rDest);
    }
    @Override public void operate2dest(IVector aRHS, IVector rDest, DoubleBinaryOperator aOpt) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        double[] tDataR = getIfHasSameOrderData_(rDest, aRHS);
        if (rData != null && tDataR != null) ARRAY.ebeDo2Dest(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize(), aOpt);
        else DATA.ebeDo2Dest(tThis, aRHS, rDest, aOpt);
    }
    
    @Override public void plus2dest(double aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        mapCheck(tThis.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapPlus2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapPlus2Dest(tThis, aRHS, rDest);
    }
    @Override public void minus2dest(double aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        mapCheck(tThis.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapMinus2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapMinus2Dest(tThis, aRHS, rDest);
    }
    @Override public void lminus2dest(double aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        mapCheck(tThis.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapLMinus2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapLMinus2Dest(tThis, aRHS, rDest);
    }
    @Override public void multiply2dest(double aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        mapCheck(tThis.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapMultiply2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapMultiply2Dest(tThis, aRHS, rDest);
    }
    @Override public void div2dest(double aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        mapCheck(tThis.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapDiv2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapDiv2Dest(tThis, aRHS, rDest);
    }
    @Override public void ldiv2dest(double aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        mapCheck(tThis.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapLDiv2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapLDiv2Dest(tThis, aRHS, rDest);
    }
    @Override public void mod2dest(double aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        mapCheck(tThis.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapMod2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapMod2Dest(tThis, aRHS, rDest);
    }
    @Override public void lmod2dest(double aRHS, IVector rDest) {
        DoubleArrayVector tThis = thisVector_();
        mapCheck(tThis.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapLMod2Dest(tThis.internalData(), tThis.internalDataShift(), aRHS, rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize());
        else DATA.mapLMod2Dest(tThis, aRHS, rDest);
    }
    @Override public void map2dest(IVector rDest, DoubleUnaryOperator aOpt) {
        DoubleArrayVector tThis = thisVector_();
        mapCheck(tThis.size(), rDest.size());
        double[] rData = tThis.getIfHasSameOrderData(rDest);
        if (rData != null) ARRAY.mapDo2Dest(tThis.internalData(), tThis.internalDataShift(), rData, IDataShell.internalDataShift(rDest), tThis.internalDataSize(), aOpt);
        else DATA.mapDo2Dest(tThis, rDest, aOpt);
    }
    
    
    @Override public void fill          (double aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill          (IVector aRHS) {
        DoubleArrayVector rThis = thisVector_();
        ebeCheck(rThis.size(), aRHS.size());
        double[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    @Override public void fill          (IVectorGetter  aRHS) {DoubleArrayVector rThis = thisVector_(); ARRAY.vecFill2This (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aRHS);}
    @Override public void assign        (DoubleSupplier aSup) {DoubleArrayVector rThis = thisVector_(); ARRAY.assign2This  (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aSup);}
    @Override public void forEach       (DoubleConsumer aCon) {DoubleArrayVector rThis = thisVector_(); ARRAY.forEachOfThis(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aCon);}
    
    @Override public double sum ()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.sumOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double mean()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.meanOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double prod()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.prodOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double max ()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.maxOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double min ()                      {DoubleArrayVector tThis = thisVector_(); return ARRAY.minOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double stat(DoubleBinaryOperator aOpt) {DoubleArrayVector tThis = thisVector_(); return ARRAY.statOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize(), aOpt);}
    
    
    /** 向量的一些额外的运算 */
    @Override public double dot(IVector aRHS) {
        DoubleArrayVector tThis = thisVector_();
        ebeCheck(tThis.size(), aRHS.size());
        double[] tDataR = tThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) return ARRAY.dot(tThis.internalData(), tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), tThis.internalDataSize());
        else return super.dot(aRHS);
    }
    @Override public double dot() {
        DoubleArrayVector tThis = thisVector_();
        return ARRAY.dotOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());
    }
    
    @Override public IVector reverse() {
        DoubleArrayVector tThis = thisVector_();
        DoubleArrayVector rVector = newVector_();
        double[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.reverse2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.reverse2Dest(tThis, rVector);
        return rVector;
    }
    
    /** 排序不自己实现 */
    @Override public void sort() {
        DoubleArrayVector rThis = thisVector_();
        ARRAY.sort(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());
        if (rThis.isReverse()) reverse2this();
    }
    @Override public void biSort(ISwapper aSwapper) {
        DoubleArrayVector rThis = thisVector_();
        int tSize = rThis.internalDataSize();
        ARRAY.biSort(rThis.internalData(), rThis.internalDataShift(), tSize, aSwapper.undata(rThis));
        if (rThis.isReverse()) {
            reverse2this();
            DATA.reverse2This(aSwapper, tSize);
        }
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private DoubleArrayVector newVector_() {return newVector_(thisVector_().size());}
    private static double @Nullable[] getIfHasSameOrderData_(IVector aThis, IVector aData) {
        if (aThis instanceof IDataShell) {
            Object tData = ((IDataShell<?>) aThis).getIfHasSameOrderData(aData);
            return (tData instanceof double[]) ? (double[])tData : null;
        } else {
            return null;
        }
    }
    
    /** stuff to override */
    @Override protected abstract DoubleArrayVector thisVector_();
    @Override protected abstract DoubleArrayVector newVector_(int aSize);
}
