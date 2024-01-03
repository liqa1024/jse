package jtool.math.matrix;

import jtool.math.operation.DATA;

public abstract class AbstractComplexMatrixOperation implements IComplexMatrixOperation {
    @Override public void plus2this(IComplexMatrix aRHS) {DATA.ebePlus2This(thisMatrix_()::setIteratorCol, aRHS::iteratorCol);}
    
    /** stuff to override */
    protected abstract IComplexMatrix thisMatrix_();
}
