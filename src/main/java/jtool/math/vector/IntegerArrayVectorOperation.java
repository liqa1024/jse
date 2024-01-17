package jtool.math.vector;

import jtool.math.IDataShell;
import jtool.math.operation.ARRAY;
import jtool.math.operation.DATA;

public abstract class IntegerArrayVectorOperation extends AbstractIntegerVectorOperation {
    @Override public void fill              (int aRHS) {IntegerArrayVector rThis = thisVector_(); ARRAY.mapFill2This(rThis.internalData(), rThis.internalDataShift(), aRHS, rThis.internalDataSize());}
    @Override public void fill              (IIntegerVector aRHS) {
        final IntegerArrayVector rThis = thisVector_();
        int[] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebeFill2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebeFill2This(rThis, aRHS);
    }
    
    /** stuff to override */
    @Override protected abstract IntegerArrayVector thisVector_();
}
