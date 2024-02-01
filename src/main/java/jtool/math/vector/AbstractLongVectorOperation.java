package jtool.math.vector;

import jtool.code.functional.ISwapper;
import jtool.math.operation.DATA;

import java.util.function.*;

import static jtool.math.vector.AbstractVector.rangeCheck;

public abstract class AbstractLongVectorOperation implements ILongVectorOperation {
    @Override public void fill          (long               aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill          (ILongVector        aRHS) {DATA.ebeFill2This (thisVector_(), aRHS);}
    @Override public void assign        (LongSupplier       aSup) {DATA.assign2This  (thisVector_(), aSup);}
    @Override public void forEach       (LongConsumer       aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill          (ILongVectorGetter  aRHS) {DATA.vecFill2This (thisVector_(), aRHS);}
    
    @Override public long   sum ()                      {return DATA.sumOfThis  (thisVector_()           );}
    @Override public double mean()                      {return DATA.meanOfThis (thisVector_().asDouble());}
    @Override public double prod()                      {return DATA.prodOfThis (thisVector_().asDouble());}
    @Override public long   max ()                      {return DATA.maxOfThis  (thisVector_()           );}
    @Override public long   min ()                      {return DATA.minOfThis  (thisVector_()           );}
    @Override public double stat(DoubleBinaryOperator aOpt) {return DATA.statOfThis(thisVector_().asDouble(), aOpt);}
    
    /** 向量的一些额外的运算 */
    @Override public ILongVector reverse() {ILongVector rVector = newVector_(); DATA.reverse2Dest(thisVector_(), rVector); return rVector;}
    @Override public ILongVector refReverse() {
        return new RefLongVector() {
            private final ILongVector mThis = thisVector_();
            @Override public long get(int aIdx) {rangeCheck(aIdx, size()); return mThis.get(mThis.size()-1-aIdx);}
            @Override public void set(int aIdx, long aValue) {rangeCheck(aIdx, size()); mThis.set(mThis.size()-1-aIdx, aValue);}
            @Override public long getAndSet(int aIdx, long aValue) {rangeCheck(aIdx, size()); return mThis.getAndSet(mThis.size()-1-aIdx, aValue);}
            @Override public int size() {return mThis.size();}
        };
    }
    @Override public void reverse2this() {DATA.reverse2This(thisVector_());}
    
    /** 排序不自己实现 */
    @Override public void sort() {DATA.sort(thisVector_());}
    @Override public void sort(IntBinaryOperator aComp) {DATA.sort(thisVector_(), aComp);}
    @Override public void biSort(ISwapper aSwapper) {DATA.biSort(thisVector_(), aSwapper);}
    @Override public void biSort(ISwapper aSwapper, IntBinaryOperator aComp) {DATA.biSort(thisVector_(), aSwapper, aComp);}
    
    
    /** 方便内部使用，减少一些重复代码 */
    private ILongVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    protected abstract ILongVector thisVector_();
    protected abstract ILongVector newVector_(int aSize);
}
