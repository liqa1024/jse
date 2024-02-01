package jtool.math.vector;

import jtool.code.functional.ISwapper;
import jtool.math.IDataShell;
import jtool.math.operation.ARRAY;
import jtool.math.operation.DATA;

import java.util.function.DoubleBinaryOperator;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

public abstract class LongArrayVectorOperation extends AbstractLongVectorOperation {
    @Override public void fill          (long aRHS) {LongArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill          (ILongVector aRHS) {
        final LongArrayVector rThis = thisVector_();
        long[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    @Override public void fill          (ILongVectorGetter aRHS) {LongArrayVector rThis = thisVector_(); ARRAY.vecFill2This (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aRHS);}
    @Override public void assign        (LongSupplier      aSup) {LongArrayVector rThis = thisVector_(); ARRAY.assign2This  (rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aSup);}
    @Override public void forEach       (LongConsumer      aCon) {LongArrayVector rThis = thisVector_(); ARRAY.forEachOfThis(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize(), rThis.isReverse(), aCon);}
    
    @Override public long   sum ()                      {LongArrayVector tThis = thisVector_(); return ARRAY.sumOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double mean()                      {LongArrayVector tThis = thisVector_(); return ARRAY.meanOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double prod()                      {LongArrayVector tThis = thisVector_(); return ARRAY.prodOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public long   max ()                      {LongArrayVector tThis = thisVector_(); return ARRAY.maxOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public long   min ()                      {LongArrayVector tThis = thisVector_(); return ARRAY.minOfThis (tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize());}
    @Override public double stat(DoubleBinaryOperator aOpt) {LongArrayVector tThis = thisVector_(); return ARRAY.statOfThis(tThis.internalData(), tThis.internalDataShift(), tThis.internalDataSize(), aOpt);}
    
    @Override public ILongVector reverse() {
        LongArrayVector tThis = thisVector_();
        LongArrayVector rVector = newVector_();
        long[] tDataL = rVector.getIfHasSameOrderData(tThis);
        if (tDataL != null) ARRAY.reverse2Dest(tDataL, tThis.internalDataShift(), rVector.internalData(), rVector.internalDataShift(), rVector.internalDataSize());
        else DATA.reverse2Dest(tThis, rVector);
        return rVector;
    }
    
    /** 排序不自己实现 */
    @Override public void sort() {
        final LongArrayVector rThis = thisVector_();
        ARRAY.sort(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());
        if (rThis.isReverse()) reverse2this();
    }
    @Override public void biSort(ISwapper aSwapper) {
        final LongArrayVector rThis = thisVector_();
        final int tSize = rThis.internalDataSize();
        ARRAY.biSort(rThis.internalData(), rThis.internalDataShift(), tSize, aSwapper.undata(rThis));
        if (rThis.isReverse()) {
            reverse2this();
            DATA.reverse2This(aSwapper, tSize);
        }
    }
    
    
    /** 方便内部使用，减少一些重复代码 */
    private LongArrayVector newVector_() {return newVector_(thisVector_().size());}
    
    /** stuff to override */
    @Override protected abstract LongArrayVector thisVector_();
    @Override protected abstract LongArrayVector newVector_(int aSize);
}
