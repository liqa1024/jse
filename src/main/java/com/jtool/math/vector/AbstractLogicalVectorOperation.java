package com.jtool.math.vector;

import com.jtool.code.functional.*;
import com.jtool.math.operation.DATA;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 * @author liqa
 */
public abstract class AbstractLogicalVectorOperation implements ILogicalVectorOperation {
    /** 通用的一些运算 */
    @Override public ILogicalVector and     (final ILogicalVectorGetter aRHS) {final ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.ebeAnd2Dest(tThis, () -> tThis.iteratorOf(aRHS), rVector); return rVector;}
    @Override public ILogicalVector or      (final ILogicalVectorGetter aRHS) {final ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.ebeOr2Dest (tThis, () -> tThis.iteratorOf(aRHS), rVector); return rVector;}
    @Override public ILogicalVector xor     (final ILogicalVectorGetter aRHS) {final ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.ebeXor2Dest(tThis, () -> tThis.iteratorOf(aRHS), rVector); return rVector;}
    @Override public ILogicalVector operate (final ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt) {final ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.ebeDo2Dest(tThis, () -> tThis.iteratorOf(aRHS), rVector, aOpt); return rVector;}
    
    @Override public ILogicalVector and     (boolean aRHS) {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.mapAnd2Dest(tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector or      (boolean aRHS) {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.mapOr2Dest (tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector xor     (boolean aRHS) {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.mapXor2Dest(tThis, aRHS, rVector); return rVector;}
    @Override public ILogicalVector map     (IBooleanOperator1 aOpt) {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.mapDo2Dest(tThis, rVector, aOpt); return rVector;}
    
    @Override public void and2this          (final ILogicalVectorGetter aRHS) {final ILogicalVector rThis = thisVector_(); DATA.ebeAnd2This(rThis, () -> rThis.iteratorOf(aRHS));}
    @Override public void or2this           (final ILogicalVectorGetter aRHS) {final ILogicalVector rThis = thisVector_(); DATA.ebeOr2This (rThis, () -> rThis.iteratorOf(aRHS));}
    @Override public void xor2this          (final ILogicalVectorGetter aRHS) {final ILogicalVector rThis = thisVector_(); DATA.ebeXor2This(rThis, () -> rThis.iteratorOf(aRHS));}
    @Override public void operate2this      (final ILogicalVectorGetter aRHS, IBooleanOperator2 aOpt) {final ILogicalVector rThis = thisVector_(); DATA.ebeDo2This(rThis, () -> rThis.iteratorOf(aRHS), aOpt);}
    
    @Override public void and2this          (boolean aRHS) {DATA.mapAnd2This(thisVector_(), aRHS);}
    @Override public void or2this           (boolean aRHS) {DATA.mapOr2This (thisVector_(), aRHS);}
    @Override public void xor2this          (boolean aRHS) {DATA.mapXor2This(thisVector_(), aRHS);}
    @Override public void map2this          (IBooleanOperator1 aOpt) {DATA.mapDo2This(thisVector_(), aOpt);}
    
    @Override public ILogicalVector not     () {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.not2Dest(tThis, rVector); return rVector;}
    @Override public void not2this          () {DATA.not2This(thisVector_());}
    
    @Override public void fill              (boolean aRHS) {DATA.mapFill2This(thisVector_(), aRHS);}
    @Override public void fill              (final ILogicalVectorGetter aRHS) {final ILogicalVector rThis = thisVector_(); DATA.ebeFill2This(rThis, () -> rThis.iteratorOf(aRHS));}
    @Override public void assign            (IBooleanSupplier aSup) {DATA.assign2This(thisVector_(), aSup);}
    @Override public void forEach           (IBooleanConsumer1 aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    
    @Override public boolean        all     () {return DATA.allOfThis  (thisVector_());}
    @Override public boolean        any     () {return DATA.anyOfThis  (thisVector_());}
    @Override public int            count   () {return DATA.countOfThis(thisVector_());}
    
    @Override public ILogicalVector cumall  () {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.cumall2Dest  (tThis, rVector); return rVector;}
    @Override public ILogicalVector cumany  () {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(tThis.size()); DATA.cumany2Dest  (tThis, rVector); return rVector;}
    @Override public IVector        cumcount() {ILogicalVector tThis = thisVector_(); IVector    rVector = newRealVector_(tThis.size()); DATA.cumcount2Dest(tThis, rVector); return rVector;}
    
    
    @Override public ILogicalVector reverse() {
        ILogicalVector tVector = refReverse();
        ILogicalVector rVector = newVector_(tVector.size());
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
    
    
    
    /** stuff to override */
    protected abstract ILogicalVector thisVector_();
    protected abstract ILogicalVector newVector_(int aSize);
    protected IVector newRealVector_(int aSize) {return Vectors.zeros(aSize);}
}
