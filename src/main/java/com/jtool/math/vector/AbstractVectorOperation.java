package com.jtool.math.vector;

import com.jtool.code.functional.*;
import com.jtool.code.iterator.IDoubleIterator;
import com.jtool.code.iterator.IDoubleSetIterator;
import com.jtool.math.MathEX;
import com.jtool.math.operation.DATA;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 * @author liqa
 */
public abstract class AbstractVectorOperation implements IVectorOperation {
    /** 通用的一些运算 */
    @Override public IVector plus       (IVector aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.ebePlus2Dest    (tThis, aRHS, rVector); return rVector;}
    @Override public IVector minus      (IVector aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.ebeMinus2Dest   (tThis, aRHS, rVector); return rVector;}
    @Override public IVector lminus     (IVector aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.ebeMinus2Dest   (aRHS, tThis, rVector); return rVector;}
    @Override public IVector multiply   (IVector aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.ebeMultiply2Dest(tThis, aRHS, rVector); return rVector;}
    @Override public IVector div        (IVector aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.ebeDiv2Dest     (tThis, aRHS, rVector); return rVector;}
    @Override public IVector ldiv       (IVector aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.ebeDiv2Dest     (aRHS, tThis, rVector); return rVector;}
    @Override public IVector mod        (IVector aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.ebeMod2Dest     (tThis, aRHS, rVector); return rVector;}
    @Override public IVector lmod       (IVector aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.ebeMod2Dest     (aRHS, tThis, rVector); return rVector;}
    @Override public IVector operate    (IVector aRHS, IDoubleOperator2 aOpt) {final IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.ebeDo2Dest(tThis, aRHS, rVector, aOpt); return rVector;}
    
    @Override public IVector plus       (double aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.mapPlus2Dest       (tThis, aRHS, rVector); return rVector;}
    @Override public IVector minus      (double aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.mapMinus2Dest      (tThis, aRHS, rVector); return rVector;}
    @Override public IVector lminus     (double aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.mapLMinus2Dest     (tThis, aRHS, rVector); return rVector;}
    @Override public IVector multiply   (double aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.mapMultiply2Dest   (tThis, aRHS, rVector); return rVector;}
    @Override public IVector div        (double aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.mapDiv2Dest        (tThis, aRHS, rVector); return rVector;}
    @Override public IVector ldiv       (double aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.mapLDiv2Dest       (tThis, aRHS, rVector); return rVector;}
    @Override public IVector mod        (double aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.mapMod2Dest        (tThis, aRHS, rVector); return rVector;}
    @Override public IVector lmod       (double aRHS) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.mapLMod2Dest       (tThis, aRHS, rVector); return rVector;}
    @Override public IVector map        (IDoubleOperator1 aOpt) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.mapDo2Dest(tThis, rVector, aOpt); return rVector;}
    
    @Override public void plus2this     (IVector aRHS) {DATA.ebePlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (IVector aRHS) {DATA.ebeMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (IVector aRHS) {DATA.ebeLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (IVector aRHS) {DATA.ebeMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (IVector aRHS) {DATA.ebeDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (IVector aRHS) {DATA.ebeLDiv2This    (thisVector_(), aRHS);}
    @Override public void mod2this      (IVector aRHS) {DATA.ebeMod2This     (thisVector_(), aRHS);}
    @Override public void lmod2this     (IVector aRHS) {DATA.ebeLMod2This    (thisVector_(), aRHS);}
    @Override public void operate2this  (IVector aRHS, IDoubleOperator2 aOpt) {DATA.ebeDo2This(thisVector_(), aRHS, aOpt);}
    
    @Override public void plus2this     (double aRHS) {DATA.mapPlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (double aRHS) {DATA.mapMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (double aRHS) {DATA.mapLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (double aRHS) {DATA.mapMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (double aRHS) {DATA.mapDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (double aRHS) {DATA.mapLDiv2This    (thisVector_(), aRHS);}
    @Override public void mod2this      (double aRHS) {DATA.mapMod2This     (thisVector_(), aRHS);}
    @Override public void lmod2this     (double aRHS) {DATA.mapLMod2This    (thisVector_(), aRHS);}
    @Override public void map2this      (IDoubleOperator1 aOpt) {DATA.mapDo2This(thisVector_(), aOpt);}
    
    @Override public void fill          (double aRHS) {DATA.mapFill2This(thisVector_(), aRHS);}
    @Override public void fill          (IVector aRHS) {DATA.ebeFill2This(thisVector_(), aRHS);}
    @Override public void assign        (IDoubleSupplier aSup) {DATA.assign2This(thisVector_(), aSup);}
    @Override public void forEach       (IDoubleConsumer1 aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill          (IVectorGetter aRHS) {
        final IVector tThis = thisVector_();
        final IDoubleSetIterator si = tThis.setIterator();
        final int tSize = tThis.size();
        for (int i = 0; i < tSize; ++i) si.nextAndSet(aRHS.get(i));
    }
    
    @Override public double sum ()                      {return DATA.sumOfThis  (thisVector_()      );}
    @Override public double mean()                      {return DATA.meanOfThis (thisVector_()      );}
    @Override public double prod()                      {return DATA.prodOfThis (thisVector_()      );}
    @Override public double max ()                      {return DATA.maxOfThis  (thisVector_()      );}
    @Override public double min ()                      {return DATA.minOfThis  (thisVector_()      );}
    @Override public double stat(IDoubleOperator2 aOpt) {return DATA.statOfThis (thisVector_(), aOpt);}
    
    @Override public IVector cumsum ()                      {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.cumsum2Dest    (tThis, rVector      ); return rVector;}
    @Override public IVector cummean()                      {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.cummean2Dest   (tThis, rVector      ); return rVector;}
    @Override public IVector cumprod()                      {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.cumprod2Dest   (tThis, rVector      ); return rVector;}
    @Override public IVector cummax ()                      {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.cummax2Dest    (tThis, rVector      ); return rVector;}
    @Override public IVector cummin ()                      {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.cummin2Dest    (tThis, rVector      ); return rVector;}
    @Override public IVector cumstat(IDoubleOperator2 aOpt) {IVector tThis = thisVector_(); IVector rVector = newVector_(tThis.size()); DATA.cumstat2Dest   (tThis, rVector, aOpt); return rVector;}
    
    /** 获取逻辑结果的运算 */
    @Override public ILogicalVector equal           (IVector aRHS) {final IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.ebeEqual2Dest         (tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector greater         (IVector aRHS) {final IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.ebeGreater2Dest       (tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector greaterOrEqual  (IVector aRHS) {final IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.ebeGreaterOrEqual2Dest(tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector less            (IVector aRHS) {final IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.ebeLess2Dest          (tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector lessOrEqual     (IVector aRHS) {final IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.ebeLessOrEqual2Dest   (tThis, aRHS, rVector); return rVector;}
    
    @Override public ILogicalVector equal           (double aRHS) {IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.mapEqual2Dest          (tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector greater         (double aRHS) {IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.mapGreater2Dest        (tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector greaterOrEqual  (double aRHS) {IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.mapGreaterOrEqual2Dest (tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector less            (double aRHS) {IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.mapLess2Dest           (tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector lessOrEqual     (double aRHS) {IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.mapLessOrEqual2Dest    (tThis, aRHS, rVector); return rVector;}
    
    @Override public ILogicalVector compare(IVector aRHS, IComparator aOpt) {final IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.ebeCompare2Dest(tThis, aRHS, rVector, aOpt); return rVector;}
    @Override public ILogicalVector check  (IChecker aOpt) {IVector tThis = thisVector_(); ILogicalVector rVector = newLogicalVector_(tThis.size()); DATA.mapCheck2Dest(tThis, rVector, aOpt); return rVector;}
    
    /** 向量的一些额外的运算 */
    @Override public double dot(IVector aRHS) {
        final IVector tThis = thisVector_();
        final IDoubleIterator il = tThis.iterator();
        final IDoubleIterator ir = aRHS.iterator();
        double rDot = 0.0;
        while (il.hasNext()) rDot += il.next()*ir.next();
        return rDot;
    }
    @Override public double dot() {
        final IDoubleIterator it = thisVector_().iterator();
        double rDot = 0.0;
        while (it.hasNext()) {
            double tValue = it.next();
            rDot += tValue*tValue;
        }
        return rDot;
    }
    @Override public double norm() {return MathEX.Fast.sqrt(dot());}
    
    @Override public IVector reverse() {
        IVector tVector = refReverse();
        IVector rVector = newVector_(tVector.size());
        rVector.fill(tVector);
        return rVector;
    }
    @Override public IVector refReverse() {
        return new RefVector() {
            private final IVector mThis = thisVector_();
            @Override public double get_(int aIdx) {return mThis.get_(mThis.size()-1-aIdx);}
            @Override public void set_(int aIdx, double aValue) {mThis.set_(mThis.size()-1-aIdx, aValue);}
            @Override public double getAndSet_(int aIdx, double aValue) {return mThis.getAndSet_(mThis.size()-1-aIdx, aValue);}
            @Override public int size() {return mThis.size();}
        };
    }
    
    
    /** stuff to override */
    protected abstract IVector thisVector_();
    protected abstract IVector newVector_(int aSize);
    protected ILogicalVector newLogicalVector_(int aSize) {return LogicalVector.zeros(aSize);}
}
