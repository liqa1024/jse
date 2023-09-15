package com.jtool.math.vector;

import com.jtool.code.functional.IBooleanOperator1;
import com.jtool.code.functional.IBooleanOperator2;
import com.jtool.math.IDataShell;
import com.jtool.math.operation.ARRAY;
import com.jtool.math.operation.DATA;

/**
 * 对于内部含有 boolean[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class BooleanArrayVectorOperation extends AbstractLogicalVectorOperation {
    /** 通用的一些运算 */
    @Override public ILogicalVector and(ILogicalVector aRHS) {
        final BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeAnd2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeAnd2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector or(ILogicalVector aRHS) {
        final BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeOr2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeOr2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector xor(ILogicalVector aRHS) {
        final BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeXor2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeXor2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector operate(ILogicalVector aRHS, IBooleanOperator2 aOpt) {
        final BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest(tDataL, tThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);
        else DATA.ebeDo2Dest(tThis, aRHS, rVector, aOpt);
        return rVector;
    }
    
    @Override public ILogicalVector and(boolean aRHS) {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapAnd2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapAnd2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector or(boolean aRHS) {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapOr2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapOr2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector xor(boolean aRHS) {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapXor2Dest(tDataL, tThis.shiftSize(), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapXor2Dest(tThis, aRHS, rVector);
        return rVector;
    }
    @Override public ILogicalVector map(IBooleanOperator1 aOpt) {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.mapDo2Dest(tDataL, tThis.shiftSize(), rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);
        else DATA.mapDo2Dest(tThis, rVector, aOpt);
        return rVector;
    }
    
    @Override public void and2this(ILogicalVector aRHS) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeAnd2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeAnd2This(rThis, aRHS);
    }
    @Override public void or2this(ILogicalVector aRHS) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeOr2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeOr2This(rThis, aRHS);
    }
    @Override public void xor2this(ILogicalVector aRHS) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeXor2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeXor2This(rThis, aRHS);
    }
    @Override public void operate2this(ILogicalVector aRHS, IBooleanOperator2 aOpt) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize(), aOpt);
        else DATA.ebeDo2This(rThis, aRHS, aOpt);
    }
    
    @Override public void and2this          (boolean aRHS) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapAnd2This  (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void or2this           (boolean aRHS) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapOr2This   (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void xor2this          (boolean aRHS) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapXor2This  (rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void map2this          (IBooleanOperator1 aOpt) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapDo2This(rThis.getData(), rThis.shiftSize(), rThis.dataSize(), aOpt);}
    
    @Override public ILogicalVector not     () {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] tData = rVector.getIfHasSameOrderData(tThis);
        if (tData != null) ARRAY.not2Dest(tData, tThis.shiftSize(), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.not2Dest(tThis, rVector);
        return rVector;
    }
    @Override public void not2this          () {BooleanArrayVector rThis = thisVector_(); ARRAY.not2This(rThis.getData(), rThis.shiftSize(), rThis.dataSize());}
    
    @Override public void fill              (boolean aRHS) {BooleanArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.getData(), rThis.shiftSize(), aRHS, rThis.dataSize());}
    @Override public void fill              (ILogicalVector aRHS) {
        final BooleanArrayVector rThis = thisVector_();
        boolean[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.getData(), rThis.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rThis.dataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    
    @Override public boolean        all     () {BooleanArrayVector tThis = thisVector_(); return ARRAY.allOfThis  (tThis.getData(), tThis.shiftSize(), tThis.dataSize());}
    @Override public boolean        any     () {BooleanArrayVector tThis = thisVector_(); return ARRAY.anyOfThis  (tThis.getData(), tThis.shiftSize(), tThis.dataSize());}
    @Override public int            count   () {BooleanArrayVector tThis = thisVector_(); return ARRAY.countOfThis(tThis.getData(), tThis.shiftSize(), tThis.dataSize());}
    
    @Override public ILogicalVector cumall() {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cumall2Dest(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize());
        else DATA.cumall2Dest(tThis, rVector);
        return rVector;
    }
    @Override public ILogicalVector cumany() {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cumany2Dest(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize());
        else DATA.cumany2Dest(tThis, rVector);
        return rVector;
    }
    
    
    /** stuff to override */
    @Override protected abstract BooleanArrayVector thisVector_();
    @Override protected abstract BooleanArrayVector newVector_(int aSize);
}
