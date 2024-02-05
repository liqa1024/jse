package jse.code.iterator;

import java.util.function.DoubleConsumer;

import java.util.Objects;

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
}
