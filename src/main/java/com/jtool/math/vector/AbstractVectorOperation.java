package com.jtool.math.vector;

import com.jtool.code.operator.IDoubleOperator1;
import com.jtool.code.operator.IDoubleOperator2;
import com.jtool.math.operation.DATA;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 * @author liqa
 */
public abstract class AbstractVectorOperation implements IVectorOperation {
    /** 通用的一些运算 */
    @Override public IVector ebePlus        (IVectorGetter aLHS, IVectorGetter aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebePlus2Dest_      (rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator()); return rVector;}
    @Override public IVector ebeMinus       (IVectorGetter aLHS, IVectorGetter aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebeMinus2Dest_     (rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator()); return rVector;}
    @Override public IVector ebeMultiply    (IVectorGetter aLHS, IVectorGetter aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebeMultiply2Dest_  (rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator()); return rVector;}
    @Override public IVector ebeDiv         (IVectorGetter aLHS, IVectorGetter aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebeDiv2Dest_       (rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator()); return rVector;}
    @Override public IVector ebeMod         (IVectorGetter aLHS, IVectorGetter aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebeMod2Dest_       (rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator()); return rVector;}
    @Override public IVector ebeDo          (IVectorGetter aLHS, IVectorGetter aRHS, IDoubleOperator2 aOpt) {IVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebeDo2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator(), aOpt); return rVector;}
    
    @Override public IVector mapPlus        (IVectorGetter aLHS, double aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapPlus2Dest_       (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public IVector mapMinus       (IVectorGetter aLHS, double aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapMinus2Dest_      (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public IVector mapLMinus      (IVectorGetter aLHS, double aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapLMinus2Dest_     (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public IVector mapMultiply    (IVectorGetter aLHS, double aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapMultiply2Dest_   (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public IVector mapDiv         (IVectorGetter aLHS, double aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapDiv2Dest_        (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public IVector mapLDiv        (IVectorGetter aLHS, double aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapLDiv2Dest_       (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public IVector mapMod         (IVectorGetter aLHS, double aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapMod2Dest_        (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public IVector mapLMod        (IVectorGetter aLHS, double aRHS) {IVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapLMod2Dest_       (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public IVector mapDo          (IVectorGetter aLHS, IDoubleOperator1 aOpt) {IVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapDo2Dest_(rVector.iteratorOf(aLHS), rVector.setIterator(), aOpt); return rVector;}
    
    @Override public void ebePlus2this      (IVectorGetter aRHS) {IVector rVector = thisVector_(); DATA.ebePlus2this_    (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeMinus2this     (IVectorGetter aRHS) {IVector rVector = thisVector_(); DATA.ebeMinus2this_   (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeLMinus2this    (IVectorGetter aRHS) {IVector rVector = thisVector_(); DATA.ebeLMinus2this_  (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeMultiply2this  (IVectorGetter aRHS) {IVector rVector = thisVector_(); DATA.ebeMultiply2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeDiv2this       (IVectorGetter aRHS) {IVector rVector = thisVector_(); DATA.ebeDiv2this_     (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeLDiv2this      (IVectorGetter aRHS) {IVector rVector = thisVector_(); DATA.ebeLDiv2this_    (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeMod2this       (IVectorGetter aRHS) {IVector rVector = thisVector_(); DATA.ebeMod2this_     (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeLMod2this      (IVectorGetter aRHS) {IVector rVector = thisVector_(); DATA.ebeLMod2this_    (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeDo2this        (IVectorGetter aRHS, IDoubleOperator2 aOpt) {IVector rVector = thisVector_(); DATA.ebeDo2this_(rVector.setIterator(), rVector.iteratorOf(aRHS), aOpt);}
    
    @Override public void mapPlus2this      (double aRHS) {DATA.mapPlus2this_       (thisVector_().setIterator(), aRHS);}
    @Override public void mapMinus2this     (double aRHS) {DATA.mapMinus2this_      (thisVector_().setIterator(), aRHS);}
    @Override public void mapLMinus2this    (double aRHS) {DATA.mapLMinus2this_     (thisVector_().setIterator(), aRHS);}
    @Override public void mapMultiply2this  (double aRHS) {DATA.mapMultiply2this_   (thisVector_().setIterator(), aRHS);}
    @Override public void mapDiv2this       (double aRHS) {DATA.mapDiv2this_        (thisVector_().setIterator(), aRHS);}
    @Override public void mapLDiv2this      (double aRHS) {DATA.mapLDiv2this_       (thisVector_().setIterator(), aRHS);}
    @Override public void mapMod2this       (double aRHS) {DATA.mapMod2this_        (thisVector_().setIterator(), aRHS);}
    @Override public void mapLMod2this      (double aRHS) {DATA.mapLMod2this_       (thisVector_().setIterator(), aRHS);}
    @Override public void mapDo2this        (IDoubleOperator1 aOpt) {DATA.mapDo2this_(thisVector_().setIterator(), aOpt);}
    
    @Override public void mapFill2this      (double aRHS) {DATA.mapFill2this_(thisVector_().setIterator(), aRHS);}
    @Override public void ebeFill2this      (IVectorGetter aRHS) {IVector rVector = thisVector_(); DATA.ebeFill2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));}
    
    @Override public double sum ()    {return DATA.sumOfThis_    (thisVector_().iterator());}
    @Override public double mean()    {return DATA.meanOfThis_   (thisVector_().iterator());}
    @Override public double product() {return DATA.productOfThis_(thisVector_().iterator());}
    @Override public double max ()    {return DATA.maxOfThis_    (thisVector_().iterator());}
    @Override public double min ()    {return DATA.minOfThis_    (thisVector_().iterator());}
    
    
    /** 向量的一些额外的运算 */
    @Override public IVector reverse() {
        IVector tVector = refReverse();
        IVector rVector = thisVector_().newZeros(tVector.size());
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
    
    /** 内部实用函数 */
    protected int newVectorSize_(IVectorGetter aData) {
        if (aData instanceof IVector) return ((IVector)aData).size();
        return thisVector_().size();
    }
    protected int newVectorSize_(IVectorGetter aData1, IVectorGetter aData2) {
        if (aData1 instanceof IVector) return ((IVector)aData1).size();
        if (aData2 instanceof IVector) return ((IVector)aData2).size();
        return thisVector_().size();
    }
    
    
    /** stuff to override */
    protected abstract IVector thisVector_();
    protected abstract IVector newVector_(int aSize);
}
