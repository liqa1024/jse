package jse.math.vector;

import jse.code.functional.IChecker;
import jse.code.functional.IComparator;
import jse.code.functional.ISwapper;
import jse.code.iterator.IDoubleIterator;
import jse.math.MathEX;
import jse.math.operation.DATA;

import java.util.function.*;

/**
 * 一般的实向量运算的实现，默认没有做任何优化
 * @author liqa
 */
public abstract class AbstractVectorOperation implements IVectorOperation {
    /** 通用的一些运算 */
    @Override public IVector plus       (IVector aRHS) {IVector rVector = newVector_(); DATA.ebePlus2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector minus      (IVector aRHS) {IVector rVector = newVector_(); DATA.ebeMinus2Dest   (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector lminus     (IVector aRHS) {IVector rVector = newVector_(); DATA.ebeMinus2Dest   (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IVector multiply   (IVector aRHS) {IVector rVector = newVector_(); DATA.ebeMultiply2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector div        (IVector aRHS) {IVector rVector = newVector_(); DATA.ebeDiv2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector ldiv       (IVector aRHS) {IVector rVector = newVector_(); DATA.ebeDiv2Dest     (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IVector mod        (IVector aRHS) {IVector rVector = newVector_(); DATA.ebeMod2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector lmod       (IVector aRHS) {IVector rVector = newVector_(); DATA.ebeMod2Dest     (aRHS, thisVector_(), rVector); return rVector;}
    @Override public IVector operate    (IVector aRHS, DoubleBinaryOperator aOpt) {IVector rVector = newVector_(); DATA.ebeDo2Dest(thisVector_(), aRHS, rVector, aOpt); return rVector;}
    
    @Override public IVector plus       (double aRHS) {IVector rVector = newVector_(); DATA.mapPlus2Dest       (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector minus      (double aRHS) {IVector rVector = newVector_(); DATA.mapMinus2Dest      (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector lminus     (double aRHS) {IVector rVector = newVector_(); DATA.mapLMinus2Dest     (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector multiply   (double aRHS) {IVector rVector = newVector_(); DATA.mapMultiply2Dest   (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector div        (double aRHS) {IVector rVector = newVector_(); DATA.mapDiv2Dest        (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector ldiv       (double aRHS) {IVector rVector = newVector_(); DATA.mapLDiv2Dest       (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector mod        (double aRHS) {IVector rVector = newVector_(); DATA.mapMod2Dest        (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector lmod       (double aRHS) {IVector rVector = newVector_(); DATA.mapLMod2Dest       (thisVector_(), aRHS, rVector); return rVector;}
    @Override public IVector map        (DoubleUnaryOperator aOpt) {IVector rVector = newVector_(); DATA.mapDo2Dest(thisVector_(), rVector, aOpt); return rVector;}
    
    @Override public void plus2this     (IVector aRHS) {DATA.ebePlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (IVector aRHS) {DATA.ebeMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (IVector aRHS) {DATA.ebeLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (IVector aRHS) {DATA.ebeMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (IVector aRHS) {DATA.ebeDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (IVector aRHS) {DATA.ebeLDiv2This    (thisVector_(), aRHS);}
    @Override public void mod2this      (IVector aRHS) {DATA.ebeMod2This     (thisVector_(), aRHS);}
    @Override public void lmod2this     (IVector aRHS) {DATA.ebeLMod2This    (thisVector_(), aRHS);}
    @Override public void operate2this  (IVector aRHS, DoubleBinaryOperator aOpt) {DATA.ebeDo2This(thisVector_(), aRHS, aOpt);}
    
    @Override public void plus2this     (double aRHS) {DATA.mapPlus2This    (thisVector_(), aRHS);}
    @Override public void minus2this    (double aRHS) {DATA.mapMinus2This   (thisVector_(), aRHS);}
    @Override public void lminus2this   (double aRHS) {DATA.mapLMinus2This  (thisVector_(), aRHS);}
    @Override public void multiply2this (double aRHS) {DATA.mapMultiply2This(thisVector_(), aRHS);}
    @Override public void div2this      (double aRHS) {DATA.mapDiv2This     (thisVector_(), aRHS);}
    @Override public void ldiv2this     (double aRHS) {DATA.mapLDiv2This    (thisVector_(), aRHS);}
    @Override public void mod2this      (double aRHS) {DATA.mapMod2This     (thisVector_(), aRHS);}
    @Override public void lmod2this     (double aRHS) {DATA.mapLMod2This    (thisVector_(), aRHS);}
    @Override public void map2this      (DoubleUnaryOperator aOpt) {DATA.mapDo2This(thisVector_(), aOpt);}
    
    @Override public IVector negative() {IVector rVector = newVector_(); DATA.mapNegative2Dest(thisVector_(), rVector); return rVector;}
    @Override public void negative2this() {DATA.mapNegative2This(thisVector_());}
    
    @Override public void fill          (double         aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill          (IVector        aRHS) {DATA.ebeFill2This (thisVector_(), aRHS);}
    @Override public void assign        (DoubleSupplier aSup) {DATA.assign2This  (thisVector_(), aSup);}
    @Override public void forEach       (DoubleConsumer aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill          (IVectorGetter  aRHS) {DATA.vecFill2This (thisVector_(), aRHS);}
    
    @Override public double sum ()                      {return DATA.sumOfThis  (thisVector_()      );}
    @Override public double mean()                      {return DATA.meanOfThis (thisVector_()      );}
    @Override public double prod()                      {return DATA.prodOfThis (thisVector_()      );}
    @Override public double max ()                      {return DATA.maxOfThis  (thisVector_()      );}
    @Override public double min ()                      {return DATA.minOfThis  (thisVector_()      );}
    @Override public double stat(DoubleBinaryOperator aOpt) {return DATA.statOfThis (thisVector_(), aOpt);}
    
    @Override public IVector cumsum ()                      {IVector rVector = newVector_(); DATA.cumsum2Dest    (thisVector_(), rVector      ); return rVector;}
    @Override public IVector cummean()                      {IVector rVector = newVector_(); DATA.cummean2Dest   (thisVector_(), rVector      ); return rVector;}
    @Override public IVector cumprod()                      {IVector rVector = newVector_(); DATA.cumprod2Dest   (thisVector_(), rVector      ); return rVector;}
    @Override public IVector cummax ()                      {IVector rVector = newVector_(); DATA.cummax2Dest    (thisVector_(), rVector      ); return rVector;}
    @Override public IVector cummin ()                      {IVector rVector = newVector_(); DATA.cummin2Dest    (thisVector_(), rVector      ); return rVector;}
    @Override public IVector cumstat(DoubleBinaryOperator aOpt) {IVector rVector = newVector_(); DATA.cumstat2Dest   (thisVector_(), rVector, aOpt); return rVector;}
    
    /** 获取逻辑结果的运算 */
    @Override public ILogicalVector equal           (IVector aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.ebeEqual2Dest         (thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector greater         (IVector aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.ebeGreater2Dest       (thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector greaterOrEqual  (IVector aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.ebeGreaterOrEqual2Dest(thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector less            (IVector aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.ebeLess2Dest          (thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector lessOrEqual     (IVector aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.ebeLessOrEqual2Dest   (thisVector_(), aRHS, rVector); return rVector;}
    
    @Override public ILogicalVector equal           (double aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.mapEqual2Dest          (thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector greater         (double aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.mapGreater2Dest        (thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector greaterOrEqual  (double aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.mapGreaterOrEqual2Dest (thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector less            (double aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.mapLess2Dest           (thisVector_(), aRHS, rVector); return rVector;}
    @Override public ILogicalVector lessOrEqual     (double aRHS) {ILogicalVector rVector = newLogicalVector_(); DATA.mapLessOrEqual2Dest    (thisVector_(), aRHS, rVector); return rVector;}
    
    @Override public ILogicalVector compare(IVector aRHS, IComparator aOpt) {ILogicalVector rVector = newLogicalVector_(); DATA.ebeCompare2Dest(thisVector_(), aRHS, rVector, aOpt); return rVector;}
    @Override public ILogicalVector check  (IChecker aOpt) {ILogicalVector rVector = newLogicalVector_(); DATA.mapCheck2Dest(thisVector_(), rVector, aOpt); return rVector;}
    
    /** 向量的一些额外的运算 */
    @Override public double dot(IVector aRHS) {
        final IVector tThis = thisVector_();
        final IDoubleIterator li = tThis.iterator();
        final IDoubleIterator ri = aRHS.iterator();
        double rDot = 0.0;
        while (li.hasNext()) rDot += li.next()*ri.next();
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
    
    @Override public IVector reverse() {IVector rVector = newVector_(); DATA.reverse2Dest(thisVector_(), rVector); return rVector;}
    @Override public IVector refReverse() {
        return new RefVector() {
            private final IVector mThis = thisVector_();
            @Override public double get(int aIdx) {rangeCheck(aIdx, size()); return mThis.get(mThis.size()-1-aIdx);}
            @Override public void set(int aIdx, double aValue) {rangeCheck(aIdx, size()); mThis.set(mThis.size()-1-aIdx, aValue);}
            @Override public double getAndSet(int aIdx, double aValue) {rangeCheck(aIdx, size()); return mThis.getAndSet(mThis.size()-1-aIdx, aValue);}
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
    private IVector newVector_() {return newVector_(thisVector_().size());}
    private ILogicalVector newLogicalVector_() {return newLogicalVector_(thisVector_().size());}
    
    /** stuff to override */
    protected abstract IVector thisVector_();
    protected abstract IVector newVector_(int aSize);
    protected ILogicalVector newLogicalVector_(int aSize) {return LogicalVector.zeros(aSize);}
}
