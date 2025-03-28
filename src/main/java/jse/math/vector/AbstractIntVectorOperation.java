package jse.math.vector;

import jse.code.functional.ISwapper;
import jse.code.random.IRandom;
import jse.math.operation.DATA;

import java.util.function.*;

import static jse.code.CS.RANDOM;
import static jse.math.vector.AbstractVector.rangeCheck;
import static jse.math.vector.AbstractVectorOperation.ebeCheck;

public abstract class AbstractIntVectorOperation implements IIntVectorOperation {
    /** 通用的一些运算 */
    @Override public IIntVector plus       (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); IIntVector rVector = newVector_(); DATA.ebePlus2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector minus      (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); IIntVector rVector = newVector_(); DATA.ebeMinus2Dest   (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector lminus     (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); IIntVector rVector = newVector_(); DATA.ebeMinus2Dest   (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IIntVector multiply   (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); IIntVector rVector = newVector_(); DATA.ebeMultiply2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector div        (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); IIntVector rVector = newVector_(); DATA.ebeDiv2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector ldiv       (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); IIntVector rVector = newVector_(); DATA.ebeDiv2Dest     (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IIntVector mod        (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); IIntVector rVector = newVector_(); DATA.ebeMod2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector lmod       (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); IIntVector rVector = newVector_(); DATA.ebeMod2Dest     (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IIntVector operate    (IIntVector aRHS, IntBinaryOperator aOpt) {ebeCheck(thisVector_().size(), aRHS.size()); IIntVector rVector = newVector_(); DATA.ebeDo2Dest(thisVector_(), aRHS, rVector, aOpt); return rVector;}
    
    @Override public IIntVector plus       (int aRHS) {IIntVector rVector = newVector_(); DATA.mapPlus2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector minus      (int aRHS) {IIntVector rVector = newVector_(); DATA.mapMinus2Dest   (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector lminus     (int aRHS) {IIntVector rVector = newVector_(); DATA.mapLMinus2Dest  (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector multiply   (int aRHS) {IIntVector rVector = newVector_(); DATA.mapMultiply2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector div        (int aRHS) {IIntVector rVector = newVector_(); DATA.mapDiv2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector ldiv       (int aRHS) {IIntVector rVector = newVector_(); DATA.mapLDiv2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector mod        (int aRHS) {IIntVector rVector = newVector_(); DATA.mapMod2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector lmod       (int aRHS) {IIntVector rVector = newVector_(); DATA.mapLMod2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IIntVector map        (IntUnaryOperator aOpt) {IIntVector rVector = newVector_(); DATA.mapDo2Dest(thisVector_(), rVector, aOpt); return rVector;}
    
    @Override public void plus2this     (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebePlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeLDiv2This    (thisVector_(), aRHS);}
    @Override public void mod2this      (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeMod2This     (thisVector_(), aRHS);}
    @Override public void lmod2this     (IIntVector aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeLMod2This    (thisVector_(), aRHS);}
    @Override public void operate2this  (IIntVector aRHS, IntBinaryOperator aOpt) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeDo2This(thisVector_(), aRHS, aOpt);}
    
    @Override public void plus2this     (int aRHS) {DATA.mapPlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (int aRHS) {DATA.mapMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (int aRHS) {DATA.mapLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (int aRHS) {DATA.mapMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (int aRHS) {DATA.mapDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (int aRHS) {DATA.mapLDiv2This    (thisVector_(), aRHS);}
    @Override public void mod2this      (int aRHS) {DATA.mapMod2This     (thisVector_(), aRHS);}
    @Override public void lmod2this     (int aRHS) {DATA.mapLMod2This    (thisVector_(), aRHS);}
    @Override public void map2this      (IntUnaryOperator aOpt) {DATA.mapDo2This(thisVector_(), aOpt);}
    
    @Override public IIntVector abs() {IIntVector rVector = newVector_(); DATA.mapAbs2Dest(thisVector_(), rVector); return rVector;}
    @Override public void abs2this() {DATA.mapAbs2This(thisVector_());}
    @Override public IIntVector negative() {IIntVector rVector = newVector_(); DATA.mapNegative2Dest(thisVector_(), rVector); return rVector;}
    @Override public void negative2this() {DATA.mapNegative2This(thisVector_());}
    
    @Override public void fill          (int                aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill          (IIntVector         aRHS) {ebeCheck(thisVector_().size(), aRHS.size()); DATA.ebeFill2This (thisVector_(), aRHS);}
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
    
    @Override public final void shuffle() {shuffle(RANDOM);}
    @Override public void shuffle(IRandom aRng) {
        final IIntVector tThis = thisVector_();
        final int tSize = tThis.size();
        for (int i = tSize; i > 1; --i) {
            tThis.swap(i-1, aRng.nextInt(i));
        }
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private IIntVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    protected abstract IIntVector thisVector_();
    protected abstract IIntVector newVector_(int aSize);
}
