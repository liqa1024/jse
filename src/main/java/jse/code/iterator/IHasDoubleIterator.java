package jse.code.iterator;

import jse.code.collection.AbstractCollections;

import java.util.Objects;
import java.util.function.DoubleConsumer;

@FunctionalInterface
public interface IHasDoubleIterator {
    IDoubleIterator iterator();
    
    default Iterable<Double> iterable() {return () -> iterator().toIterator();}
    
    /** Iterable like stuffs */
    default void forEach(DoubleConsumer aCon) {
        Objects.requireNonNull(aCon);
        final IDoubleIterator it = iterator();
        while (it.hasNext()) aCon.accept(it.next());
    }
    
    /** 通过 {@code Iterable<? extends Number>} 得到 */
    static IHasDoubleIterator of(final Iterable<? extends Number> aIterable) {
        return new IHasDoubleIterator() {
            @Override public IDoubleIterator iterator() {return IDoubleIterator.of(aIterable.iterator());}
            @Override public Iterable<Double> iterable() {return AbstractCollections.map(aIterable, Number::doubleValue);}
            @Override public void forEach(final DoubleConsumer aCon) {aIterable.forEach(v->aCon.accept(v.doubleValue()));}
        };
    }
}
