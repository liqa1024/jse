package jse.math.vector;

import jse.code.functional.IBooleanBinaryOperator;
import jse.code.functional.IBooleanConsumer;
import jse.code.functional.IBooleanUnaryOperator;
import jse.math.IDataShell;
import jse.math.operation.ARRAY;
import jse.math.operation.DATA;

import java.util.function.BooleanSupplier;

/**
 * 对于内部含有 boolean[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class BooleanArrayVectorOperation extends AbstractLogicalVectorOperation {
    /** 通用的一些运算 */
    @Override public ILogicalVector and(ILogicalVector aRHS) {
        final BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeAnd2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeAnd2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector or(ILogicalVector aRHS) {
        final BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeOr2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeOr2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector xor(ILogicalVector aRHS) {
        final BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeXor2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.ebeXor2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector operate(ILogicalVector aRHS, IBooleanBinaryOperator aOpt) {
        final BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest(tDataL, tThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize(), aOpt);
        else DATA.ebeDo2Dest(tThis, aRHS, rVector, aOpt);
        return rVector;
    }
    
    @Override public ILogicalVector and(boolean aRHS) {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapAnd2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapAnd2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector or(boolean aRHS) {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapOr2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapOr2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector xor(boolean aRHS) {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapXor2Dest(tDataL, tThis.internalDataShift(), aRHS, rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.mapXor2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector map(IBooleanUnaryOperator aOpt) {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDo2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize(), aOpt);
        else DATA.mapDo2Dest(tThis, rVector, aOpt);
        return rVector;
    }
    
    @Override public void and2this(ILogicalVector aRHS) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeAnd2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeAnd2This(rThis, aRHS);
    }
    @Override public void or2this(ILogicalVector aRHS) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeOr2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeOr2This(rThis, aRHS);
    }
    @Override public void xor2this(ILogicalVector aRHS) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeXor2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeXor2This(rThis, aRHS);
    }
    @Override public void operate2this(ILogicalVector aRHS, IBooleanBinaryOperator aOpt) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize(), aOpt);
        else DATA.ebeDo2This(rThis, aRHS, aOpt);
    }
    
    @Override public void and2this          (boolean aRHS) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapAnd2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void or2this           (boolean aRHS) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapOr2This (rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void xor2this          (boolean aRHS) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapXor2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void map2this(IBooleanUnaryOperator aOpt) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapDo2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), aOpt);}
    
    @Override public ILogicalVector not     () {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tData = rVector.getIfHasSameOrderData(tThis);
        if (tData != null) ARRAY.not2Dest(tData, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.not2Dest(tThis, rVector);
        return rVector;
    }
    @Override public void not2this          () {BooleanArrayVector rThis = thisVector_(); ARRAY.not2This(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());}
    
    @Override public void fill              (boolean aRHS) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill              (ILogicalVector aRHS) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    @Override public void fill              (ILogicalVectorGetter aRHS) {BooleanArrayVector rThis = thisVector_(); ARRAY.vecFill2This (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aRHS);}
    @Override public void assign            (BooleanSupplier      aSup) {BooleanArrayVector rThis = thisVector_(); ARRAY.assign2This  (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aSup);}
    @Override public void forEach           (IBooleanConsumer     aCon) {BooleanArrayVector rThis = thisVector_(); ARRAY.forEachOfThis(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aCon);}
    
    @Override public boolean        all     () {BooleanArrayVector tThis = thisVector_(); return ARRAY.allOfThis  (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public boolean        any     () {BooleanArrayVector tThis = thisVector_(); return ARRAY.anyOfThis  (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public int            count   () {BooleanArrayVector tThis = thisVector_(); return ARRAY.countOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    
    @Override public ILogicalVector reverse() {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_();
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.reverse2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.reverse2Dest(tThis, rVector);
        return rVector;
    }
    
    /** 方便内部使用，减少一些重复代码 */
    private BooleanArrayVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    @Override protected abstract BooleanArrayVector thisVector_();
    @Override protected abstract BooleanArrayVector newVector_(int aSize);
}
