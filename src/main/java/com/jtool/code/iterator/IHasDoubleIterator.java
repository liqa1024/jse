package com.jtool.code.iterator;

import com.jtool.code.functional.IDoubleConsumer1;

import java.util.Objects;

@FunctionalInterface
public interface IHasDoubleIterator {
    IDoubleIterator iterator();
    
    default Iterable<Double> iterable() {return () -> iterator().toIterator();}
    
    /** Iterable like stuffs */
    default void forEach(IDoubleConsumer1 aCon) {
        Objects.requireNonNull(aCon);
        final IDoubleIterator it = iterator();
        while (it.hasNext()) aCon.run(it.next());
    }
}
