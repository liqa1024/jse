package jtool.math.matrix;

import jtool.math.IDataShell;
import jtool.math.operation.ARRAY;
import jtool.math.operation.DATA;

public abstract class BiDoubleArrayMatrixOperation extends AbstractComplexMatrixOperation {
    
    @Override public void plus2this(IComplexMatrix aRHS) {
        BiDoubleArrayMatrix rThis = thisMatrix_();
        double[][] tDataR = rThis.getIfHasSameOrderData(aRHS);
        if (tDataR != null) ARRAY.ebePlus2This(rThis.internalData(), rThis.internalDataShift(), tDataR, IDataShell.internalDataShift(aRHS), rThis.internalDataSize());
        else DATA.ebePlus2This(rThis::setIteratorCol, aRHS::iteratorCol);
    }
    
    /** stuff to override */
    @Override protected abstract BiDoubleArrayMatrix thisMatrix_();
}
