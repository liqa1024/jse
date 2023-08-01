package com.jtool.math.vector;

import com.jtool.code.operator.IBooleanOperator1;
import com.jtool.code.operator.IBooleanOperator2;
import com.jtool.math.IDataShell;
import com.jtool.math.operation.ARRAY;
import com.jtool.math.operation.DATA;

/**
 * 对于内部含有 boolean[] 的向量的运算使用专门优化后的函数
 * @author liqa
 */
public abstract class BooleanArrayVectorOperation extends AbstractLogicalVectorOperation {
    /** 通用的一些运算 */
    @Override public ILogicalVector ebeAnd(ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS) {
        BooleanArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        boolean[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeAnd2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeAnd2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator());
        return rVector;
    }
    @Override public ILogicalVector ebeOr(ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS) {
        BooleanArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        boolean[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeOr2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeOr2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator());
        return rVector;
    }
    @Override public ILogicalVector ebeXor(ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS) {
        BooleanArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        boolean[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeXor2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.ebeXor2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator());
        return rVector;
    }
    @Override public ILogicalVector ebeDo(ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt) {
        BooleanArrayVector rVector = newVector_(newVectorSize_(aLHS, aRHS));
        boolean[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataL != null && tDataR != null) ARRAY.ebeDo2Dest_(tDataL, IDataShell.shiftSize(aLHS), tDataR, IDataShell.shiftSize(aRHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);
        else DATA.ebeDo2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator(), aOpt);
        return rVector;
    }
    
    @Override public ILogicalVector mapAnd(ILogicalVectorGetter aLHS, boolean aRHS) {
        BooleanArrayVector rVector = newVector_(newVectorSize_(aLHS));
        boolean[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapAnd2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapAnd2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public ILogicalVector mapOr(ILogicalVectorGetter aLHS, boolean aRHS) {
        BooleanArrayVector rVector = newVector_(newVectorSize_(aLHS));
        boolean[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapOr2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapOr2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public ILogicalVector mapXor(ILogicalVectorGetter aLHS, boolean aRHS) {
        BooleanArrayVector rVector = newVector_(newVectorSize_(aLHS));
        boolean[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapXor2Dest_(tDataL, IDataShell.shiftSize(aLHS), aRHS, rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.mapXor2Dest_(rVector.iteratorOf(aLHS), aRHS, rVector.setIterator());
        return rVector;
    }
    @Override public ILogicalVector mapDo(ILogicalVectorGetter aLHS, IBooleanOperator1 aOpt) {
        BooleanArrayVector rVector = newVector_(newVectorSize_(aLHS));
        boolean[] tDataL = rVector.getIfHasSameOrderData(aLHS);
        if (tDataL != null) ARRAY.mapDo2Dest_(tDataL, IDataShell.shiftSize(aLHS), rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);
        else DATA.mapDo2Dest_(rVector.iteratorOf(aLHS), rVector.setIterator(), aOpt);
        return rVector;
    }
    
    @Override public void ebeAnd2this(ILogicalVectorGetter aRHS) {
        BooleanArrayVector rVector = thisVector_();
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeAnd2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeAnd2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeOr2this(ILogicalVectorGetter aRHS) {
        BooleanArrayVector rVector = thisVector_();
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeOr2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeOr2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeXor2this(ILogicalVectorGetter aRHS) {
        BooleanArrayVector rVector = thisVector_();
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeXor2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeXor2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    @Override public void ebeDo2this(ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt) {
        BooleanArrayVector rVector = thisVector_();
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeDo2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize(), aOpt);
        else DATA.ebeDo2this_(rVector.setIterator(), rVector.iteratorOf(aRHS), aOpt);
    }
    
    @Override public void mapAnd2this       (boolean aRHS) {BooleanArrayVector rVector = thisVector_(); ARRAY.mapAnd2this_  (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapOr2this        (boolean aRHS) {BooleanArrayVector rVector = thisVector_(); ARRAY.mapOr2this_   (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapXor2this       (boolean aRHS) {BooleanArrayVector rVector = thisVector_(); ARRAY.mapXor2this_  (rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void mapDo2this        (IBooleanOperator1 aOpt) {BooleanArrayVector rVector = thisVector_(); ARRAY.mapDo2this_(rVector.getData(), rVector.shiftSize(), rVector.dataSize(), aOpt);}
    
    @Override public ILogicalVector not     (ILogicalVectorGetter aData) {
        BooleanArrayVector rVector = newVector_(newVectorSize_(aData));
        boolean[] tData = rVector.getIfHasSameOrderData(aData);
        if (tData != null) ARRAY.not2Dest_(tData, IDataShell.shiftSize(aData), rVector.getData(), rVector.shiftSize(), rVector.dataSize());
        else DATA.not2Dest_(rVector.iteratorOf(aData), rVector.setIterator());
        return rVector;
    }
    @Override public void not2this          () {BooleanArrayVector rVector = thisVector_(); ARRAY.not2this_(rVector.getData(), rVector.shiftSize(), rVector.dataSize());}
    
    @Override public void mapFill2this      (boolean aRHS) {BooleanArrayVector rVector = thisVector_(); ARRAY.mapFill2this_(rVector.getData(), rVector.shiftSize(), aRHS, rVector.dataSize());}
    @Override public void ebeFill2this      (ILogicalVectorGetter aRHS) {
        BooleanArrayVector rVector = thisVector_();
        boolean[] tDataR = rVector.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2this_(rVector.getData(), rVector.shiftSize(), tDataR, IDataShell.shiftSize(aRHS), rVector.dataSize());
        else DATA.ebeFill2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));
    }
    
    @Override public boolean        all     () {BooleanArrayVector tThis = thisVector_(); return ARRAY.allOfThis_  (tThis.getData(), tThis.shiftSize(), tThis.dataSize());}
    @Override public boolean        any     () {BooleanArrayVector tThis = thisVector_(); return ARRAY.anyOfThis_  (tThis.getData(), tThis.shiftSize(), tThis.dataSize());}
    @Override public int            count   () {BooleanArrayVector tThis = thisVector_(); return ARRAY.countOfThis_(tThis.getData(), tThis.shiftSize(), tThis.dataSize());}
    
    @Override public ILogicalVector cumall() {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cumall2Dest_(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize());
        else DATA.cumall2Dest_(tThis.iterator(), rVector.setIterator());
        return rVector;
    }
    @Override public ILogicalVector cumany() {
        BooleanArrayVector tThis = thisVector_();
        BooleanArrayVector rVector = newVector_(tThis.size());
        boolean[] rDest = tThis.getIfHasSameOrderData(rVector);
        if (rDest != null) ARRAY.cumany2Dest_(tThis.getData(), tThis.shiftSize(), rDest, rVector.shiftSize(), tThis.dataSize());
        else DATA.cumany2Dest_(tThis.iterator(), rVector.setIterator());
        return rVector;
    }
    @Override public IVector cumcount() {
        BooleanArrayVector tThis = thisVector_();
        double[] rDest = new double[tThis.size()];
        ARRAY.cumcount2Dest_(tThis.getData(), tThis.shiftSize(), rDest, 0, tThis.dataSize());
        return new Vector(rDest);
    }
    
    
    /** stuff to override */
    @Override protected abstract BooleanArrayVector thisVector_();
    @Override protected abstract BooleanArrayVector newVector_(int aSize);
}
