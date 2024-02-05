package jse.code.iterator;

import jse.code.functional.IBooleanConsumer;

import java.util.Objects;

@FunctionalInterface
public interface IHasBooleanIterator {
    IBooleanIterator iterator();
    
    default Iterable<Boolean> iterable() {return () -> iterator().toIterator();}
    
    /** Iterable like stuffs */
    default void forEach(IBooleanConsumer aCon) {
        Objects.requireNonNull(aCon);
        final IBooleanIterator it = iterator();
        while (it.hasNext()) aCon.accept(it.next());
    }
}
