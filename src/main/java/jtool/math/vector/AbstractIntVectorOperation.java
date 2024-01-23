package jtool.math.vector;

import jtool.code.functional.ISwapper;
import jtool.code.iterator.IIntSetOnlyIterator;
import jtool.math.operation.DATA;

import java.util.function.IntBinaryOperator;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;

import static jtool.code.CS.RANDOM;

public abstract class AbstractIntVectorOperation implements IIntVectorOperation {
    @Override public void fill          (int                aRHS) {DATA.mapFill2This (thisVector_(), aRHS);}
    @Override public void fill          (IIntVector         aRHS) {DATA.ebeFill2This (thisVector_(), aRHS);}
    @Override public void assign        (IntSupplier        aSup) {DATA.assign2This  (thisVector_(), aSup);}
    @Override public void forEach       (IntConsumer        aCon) {DATA.forEachOfThis(thisVector_(), aCon);}
    @Override public void fill          (IIntVectorGetter   aRHS) {DATA.vecFill2This (thisVector_(), aRHS);}
    @Override public void reverse2this() {DATA.reverse2This(thisVector_());}
    
    @Override public double sum ()                      {return DATA.sumOfThis(thisVector_().asDouble());}
    
    /** 排序不自己实现 */
    @Override public void sort() {DATA.sort(thisVector_());}
    @Override public void sort(IntBinaryOperator aComp) {DATA.sort(thisVector_(), aComp);}
    @Override public void bisort(ISwapper aSwapper) {DATA.biSort(thisVector_(), aSwapper);}
    @Override public void bisort(ISwapper aSwapper, IntBinaryOperator aComp) {DATA.biSort(thisVector_(), aSwapper, aComp);}
    
    @Override public final void shuffle() {shuffle(RANDOM::nextInt);}
    @Override public void shuffle(IntUnaryOperator aRng) {
        final IIntVector tThis = thisVector_();
        final int tSize = tThis.size();
        for (int i = tSize; i > 1; --i) {
            tThis.swap(i-1, aRng.applyAsInt(i));
        }
    }
    
    /** stuff to override */
    protected abstract IIntVector thisVector_();
}
