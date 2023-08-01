package com.jtool.math.vector;

import com.jtool.code.operator.IBooleanOperator1;
import com.jtool.code.operator.IBooleanOperator2;
import com.jtool.math.operation.DATA;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 * @author liqa
 */
public abstract class AbstractLogicalVectorOperation implements ILogicalVectorOperation {
    /** 通用的一些运算 */
    @Override public ILogicalVector ebeAnd  (ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS) {ILogicalVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebeAnd2Dest_  (rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator()); return rVector;}
    @Override public ILogicalVector ebeOr   (ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS) {ILogicalVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebeOr2Dest_   (rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator()); return rVector;}
    @Override public ILogicalVector ebeXor  (ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS) {ILogicalVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebeXor2Dest_  (rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator()); return rVector;}
    @Override public ILogicalVector ebeDo   (ILogicalVectorGetter aLHS, ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt) {ILogicalVector rVector = newVector_(newVectorSize_(aLHS, aRHS)); DATA.ebeDo2Dest_(rVector.iteratorOf(aLHS), rVector.iteratorOf(aRHS), rVector.setIterator(), aOpt); return rVector;}
    
    @Override public ILogicalVector mapAnd  (ILogicalVectorGetter aLHS, boolean aRHS) {ILogicalVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapAnd2Dest_ (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public ILogicalVector mapOr   (ILogicalVectorGetter aLHS, boolean aRHS) {ILogicalVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapOr2Dest_  (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public ILogicalVector mapXor  (ILogicalVectorGetter aLHS, boolean aRHS) {ILogicalVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapXor2Dest_ (rVector.iteratorOf(aLHS), aRHS, rVector.setIterator()); return rVector;}
    @Override public ILogicalVector mapDo   (ILogicalVectorGetter aLHS, IBooleanOperator1 aOpt) {ILogicalVector rVector = newVector_(newVectorSize_(aLHS)); DATA.mapDo2Dest_(rVector.iteratorOf(aLHS), rVector.setIterator(), aOpt); return rVector;}
    
    @Override public void ebeAnd2this       (ILogicalVectorGetter aRHS) {ILogicalVector rVector = thisVector_(); DATA.ebeAnd2this_  (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeOr2this        (ILogicalVectorGetter aRHS) {ILogicalVector rVector = thisVector_(); DATA.ebeOr2this_   (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeXor2this       (ILogicalVectorGetter aRHS) {ILogicalVector rVector = thisVector_(); DATA.ebeXor2this_  (rVector.setIterator(), rVector.iteratorOf(aRHS));}
    @Override public void ebeDo2this        (ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt) {ILogicalVector rVector = thisVector_(); DATA.ebeDo2this_(rVector.setIterator(), rVector.iteratorOf(aRHS), aOpt);}
    
    @Override public void mapAnd2this       (boolean aRHS) {DATA.mapAnd2this_   (thisVector_().setIterator(), aRHS);}
    @Override public void mapOr2this        (boolean aRHS) {DATA.mapOr2this_    (thisVector_().setIterator(), aRHS);}
    @Override public void mapXor2this       (boolean aRHS) {DATA.mapXor2this_   (thisVector_().setIterator(), aRHS);}
    @Override public void mapDo2this        (IBooleanOperator1 aOpt) {DATA.mapDo2this_(thisVector_().setIterator(), aOpt);}
    
    @Override public ILogicalVector not     (ILogicalVectorGetter aData) {ILogicalVector rVector = newVector_(newVectorSize_(aData)); DATA.not2Dest_(rVector.iteratorOf(aData), rVector.setIterator()); return rVector;}
    @Override public void not2this          () {DATA.not2this_(thisVector_().setIterator());}
    
    @Override public void mapFill2this      (boolean aRHS) {DATA.mapFill2this_(thisVector_().setIterator(), aRHS);}
    @Override public void ebeFill2this      (ILogicalVectorGetter aRHS) {ILogicalVector rVector = thisVector_(); DATA.ebeFill2this_(rVector.setIterator(), rVector.iteratorOf(aRHS));}
    
    @Override public boolean        all     () {return DATA.allOfThis_  (thisVector_().iterator());}
    @Override public boolean        any     () {return DATA.anyOfThis_  (thisVector_().iterator());}
    @Override public int            count   () {return DATA.countOfThis_(thisVector_().iterator());}
    
    @Override public ILogicalVector cumall  () {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.cumall2Dest_  (tThis.iterator(), rVector.setIterator()); return rVector;}
    @Override public ILogicalVector cumany  () {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.cumany2Dest_  (tThis.iterator(), rVector.setIterator()); return rVector;}
    @Override public IVector        cumcount() {ILogicalVector tThis = thisVector_(); IVector     rVector = Vectors.zeros(tThis.size()); DATA.cumcount2Dest_(tThis.iterator(), rVector.setIterator()); return rVector;}
    
    
    @Override public ILogicalVector reverse() {
        ILogicalVector tVector = refReverse();
        ILogicalVector rVector = thisVector_().newZeros(tVector.size());
        rVector.fill(tVector);
        return rVector;
    }
    @Override public ILogicalVector refReverse() {
        return new RefLogicalVector() {
            private final ILogicalVector mThis = thisVector_();
            @Override public boolean get_(int aIdx) {return mThis.get_(mThis.size()-1-aIdx);}
            @Override public void set_(int aIdx, boolean aValue) {mThis.set_(mThis.size()-1-aIdx, aValue);}
            @Override public boolean getAndSet_(int aIdx, boolean aValue) {return mThis.getAndSet_(mThis.size()-1-aIdx, aValue);}
            @Override public int size() {return mThis.size();}
        };
    }
    
    /** 内部实用函数 */
    protected int newVectorSize_(ILogicalVectorGetter aData) {
        if (aData instanceof ILogicalVector) return ((ILogicalVector)aData).size();
        return thisVector_().size();
    }
    protected int newVectorSize_(ILogicalVectorGetter aData1, ILogicalVectorGetter aData2) {
        if (aData1 instanceof ILogicalVector) return ((ILogicalVector)aData1).size();
        if (aData2 instanceof ILogicalVector) return ((ILogicalVector)aData2).size();
        return thisVector_().size();
    }
    
    
    /** stuff to override */
    protected abstract ILogicalVector thisVector_();
    protected abstract ILogicalVector newVector_(int aSize);
}
