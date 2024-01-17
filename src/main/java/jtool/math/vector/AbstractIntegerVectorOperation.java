package jtool.math.vector;

import jtool.code.functional.IIntegerConsumer1;
import jtool.code.functional.IIntegerSupplier;
import jtool.code.iterator.IIntegerSetOnlyIterator;
import jtool.math.operation.DATA;

public abstract class AbstractIntegerVectorOperation implements IIntegerVectorOperation {
    @Override public void fill              (int                  aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill              (IIntegerVector       aRHS) {DATA.ebeFill2This (thisVector_(), aRHS);}
    @Override public void assign            (IIntegerSupplier     aSup) {DATA.assign2This  (thisVector_(), aSup);}
    @Override public void forEach           (IIntegerConsumer1    aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill              (IIntegerVectorGetter aRHS) {
        final IIntegerVector tThis = thisVector_();
        final IIntegerSetOnlyIterator si = tThis.setIterator();
        final int tSize = tThis.size();
        for (int i = 0; i < tSize; ++i) si.nextAndSet(aRHS.get(i));
    }
    
    /** stuff to override */
    protected abstract IIntegerVector thisVector_();
}
