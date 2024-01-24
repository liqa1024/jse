package jtool.math.vector;

import jtool.code.functional.IBooleanBinaryOperator;
import jtool.code.functional.IBooleanConsumer;
import jtool.code.functional.IBooleanUnaryOperator;
import jtool.code.iterator.IBooleanIterator;
import jtool.math.operation.DATA;

import java.util.function.BooleanSupplier;

import static jtool.math.vector.AbstractVector.rangeCheck;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 * @author liqa
 */
public abstract class AbstractLogicalVectorOperation implements ILogicalVectorOperation {
    /** 通用的一些运算 */
    @Override public ILogicalVector and     (ILogicalVector aRHS) {ILogicalVector rVector = newVector_(); DATA.ebeAnd2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector or      (ILogicalVector aRHS) {ILogicalVector rVector = newVector_(); DATA.ebeOr2Dest (thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector xor     (ILogicalVector aRHS) {ILogicalVector rVector = newVector_(); DATA.ebeXor2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector operate (ILogicalVector aRHS, IBooleanBinaryOperator aOpt) {ILogicalVector rVector = newVector_(); DATA.ebeDo2Dest(thisVector_(), aRHS, rVector, aOpt); return rVector;}
    
    @Override public ILogicalVector and     (boolean aRHS) {ILogicalVector rVector = newVector_(); DATA.mapAnd2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector or      (boolean aRHS) {ILogicalVector rVector = newVector_(); DATA.mapOr2Dest (thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector xor     (boolean aRHS) {ILogicalVector rVector = newVector_(); DATA.mapXor2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector map     (IBooleanUnaryOperator aOpt) {ILogicalVector rVector = newVector_(); DATA.mapDo2Dest(thisVector_(), rVector, aOpt); return rVector;}
    
    @Override public void and2this          (ILogicalVector aRHS) {DATA.ebeAnd2This(thisVector_(), aRHS);}
    @Override public void or2this           (ILogicalVector aRHS) {DATA.ebeOr2This (thisVector_(), aRHS);}
    @Override public void xor2this          (ILogicalVector aRHS) {DATA.ebeXor2This(thisVector_(), aRHS);}
    @Override public void operate2this      (ILogicalVector aRHS, IBooleanBinaryOperator aOpt) {DATA.ebeDo2This(thisVector_(), aRHS, aOpt);}
    
    @Override public void and2this          (boolean aRHS) {DATA.mapAnd2This(thisVector_(), aRHS);}
    @Override public void or2this           (boolean aRHS) {DATA.mapOr2This (thisVector_(), aRHS);}
    @Override public void xor2this          (boolean aRHS) {DATA.mapXor2This(thisVector_(), aRHS);}
    @Override public void map2this          (IBooleanUnaryOperator aOpt) {DATA.mapDo2This(thisVector_(), aOpt);}
    
    @Override public ILogicalVector not     () {ILogicalVector tThis = thisVector_(); ILogicalVector rVector = newVector_(); DATA.not2Dest(tThis, rVector); return rVector;}
    @Override public void not2this          () {DATA.not2This(thisVector_());}
    
    @Override public void fill              (boolean              aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill              (ILogicalVector       aRHS) {DATA.ebeFill2This (thisVector_(), aRHS);}
    @Override public void assign            (BooleanSupplier      aSup) {DATA.assign2This  (thisVector_(), aSup);}
    @Override public void forEach           (IBooleanConsumer     aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill              (ILogicalVectorGetter aRHS) {DATA.vecFill2This (thisVector_(), aRHS);}
    
    @Override public boolean        all     () {return DATA.allOfThis  (thisVector_());}
    @Override public boolean        any     () {return DATA.anyOfThis  (thisVector_());}
    @Override public int            count   () {return DATA.countOfThis(thisVector_());}
    
    @Override public ILogicalVector cumall  () {ILogicalVector rVector = newVector_(); DATA.cumall2Dest  (thisVector_(), rVector); return rVector;}
    @Override public ILogicalVector cumany  () {ILogicalVector rVector = newVector_(); DATA.cumany2Dest  (thisVector_(), rVector); return rVector;}
    @Override public IVector        cumcount() {IVector    rVector = newRealVector_(); DATA.cumcount2Dest(thisVector_(), rVector); return rVector;}
    
    
    @Override public ILogicalVector reverse() {ILogicalVector rVector = newVector_(); DATA.reverse2Dest(thisVector_(), rVector); return rVector;}
    @Override public ILogicalVector refReverse() {
        return new RefLogicalVector() {
            private final ILogicalVector mThis = thisVector_();
            @Override public boolean get(int aIdx) {rangeCheck(aIdx, size()); return mThis.get(mThis.size()-1-aIdx);}
            @Override public void set(int aIdx, boolean aValue) {rangeCheck(aIdx, size()); mThis.set(mThis.size()-1-aIdx, aValue);}
            @Override public boolean getAndSet(int aIdx, boolean aValue) {rangeCheck(aIdx, size()); return mThis.getAndSet(mThis.size()-1-aIdx, aValue);}
            @Override public int size() {return mThis.size();}
        };
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private ILogicalVector newVector_() {return newVector_(thisVector_().size());}
    private IVector newRealVector_() {return newRealVector_(thisVector_().size());}
    
    /** stuff to override */
    protected abstract ILogicalVector thisVector_();
    protected abstract ILogicalVector newVector_(int aSize);
    protected IVector newRealVector_(int aSize) {return Vectors.zeros(aSize);}
}
