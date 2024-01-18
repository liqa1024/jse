package jtool.math.vector;

import jtool.code.functional.ISwapper;
import jtool.math.IDataShell;
import jtool.math.operation.ARRAY;
import jtool.math.operation.DATA;

public abstract class IntegerArrayVectorOperation extends AbstractIntegerVectorOperation {
    @Override public void fill              (int aRHS) {IntArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill              (IIntVector aRHS) {
        final IntArrayVector rThis = thisVector_();
        int[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    
    /** 排序不自己实现 */
    @Override public void sort() {
        final IntArrayVector rThis = thisVector_();
        ARRAY.sort(rThis.internalData(), rThis.internalDataShift(), rThis.internalDataSize());
        if (rThis.isReverse()) reverse2this();
    }
    @Override public void bisort(ISwapper aSwapper) {
        final IntArrayVector rThis = thisVector_();
        final int tSize = rThis.internalDataSize();
        ARRAY.biSort(rThis.internalData(), rThis.internalDataShift(), tSize, aSwapper.undata(rThis));
        if (rThis.isReverse()) {
            reverse2this();
            DATA.reverse2This(aSwapper, tSize);
        }
    }
    
    /** stuff to override */
    @Override protected abstract IntArrayVector thisVector_();
}
