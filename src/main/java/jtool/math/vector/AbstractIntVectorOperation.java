package jtool.math.vector;

import jtool.code.functional.ISwapper;
import jtool.math.operation.DATA;

import java.util.function.*;

import static jtool.code.CS.RANDOM;
import static jtool.math.vector.AbstractVector.rangeCheck;

public abstract class AbstractIntVectorOperation implements IIntVectorOperation {
    @Override public void fill          (int                aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill          (IIntVector         aRHS) {DATA.ebeFill2This (thisVector_(), aRHS);}
    @Override public void assign        (IntSupplier        aSup) {DATA.assign2This  (thisVector_(), aSup);}
    @Override public void forEach       (IntConsumer        aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill          (IIntVectorGetter   aRHS) {DATA.vecFill2This (thisVector_(), aRHS);}
    
    @Override public int    sum  ()                         {return DATA.sumOfThis  (thisVector_()           );}
    @Override public long   exsum()                         {return DATA.exsumOfThis(thisVector_()           );}
    @Override public double mean ()                         {return DATA.meanOfThis (thisVector_().asDouble());}
    @Override public double prod ()                         {return DATA.prodOfThis (thisVector_().asDouble());}
    @Override public int    max  ()                         {return DATA.maxOfThis  (thisVector_()           );}
    @Override public int    min  ()                         {return DATA.minOfThis  (thisVector_()           );}
    @Override public double stat(DoubleBinaryOperator aOpt) {return DATA.statOfThis(thisVector_().asDouble(), aOpt);}
    
    
    /** 向量的一些额外的运算 */
    @Override public IIntVector reverse() {IIntVector rVector = newVector_(); DATA.reverse2Dest(thisVector_(), rVector); return rVector;}
    @Override public IIntVector refReverse() {
        return new RefIntVector() {
            private final IIntVector mThis = thisVector_();
            @Override public int get(int aIdx) {rangeCheck(aIdx, size()); return mThis.get(mThis.size()-1-aIdx);}
            @Override public void set(int aIdx, int aValue) {rangeCheck(aIdx, size()); mThis.set(mThis.size()-1-aIdx, aValue);}
            @Override public int getAndSet(int aIdx, int aValue) {rangeCheck(aIdx, size()); return mThis.getAndSet(mThis.size()-1-aIdx, aValue);}
            @Override public int size() {return mThis.size();}
        };
    }
    @Override public void reverse2this() {DATA.reverse2This(thisVector_());}
    
    /** 排序不自己实现 */
    @Override public void sort() {DATA.sort(thisVector_());}
    @Override public void sort(IntBinaryOperator aComp) {DATA.sort(thisVector_(), aComp);}
    @Override public void biSort(ISwapper aSwapper) {DATA.biSort(thisVector_(), aSwapper);}
    @Override public void biSort(ISwapper aSwapper, IntBinaryOperator aComp) {DATA.biSort(thisVector_(), aSwapper, aComp);}
    
    @Override public final void shuffle() {shuffle(RANDOM::nextInt);}
    @Override public void shuffle(IntUnaryOperator aRng) {
        final IIntVector tThis = thisVector_();
        final int tSize = tThis.size();
        for (int i = tSize; i > 1; --i) {
            tThis.swap(i-1, aRng.applyAsInt(i));
        }
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private IIntVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    protected abstract IIntVector thisVector_();
    protected abstract IIntVector newVector_(int aSize);
}
