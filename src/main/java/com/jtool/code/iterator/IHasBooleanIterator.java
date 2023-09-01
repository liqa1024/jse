package com.jtool.code.iterator;

import com.jtool.code.functional.IBooleanConsumer1;

import java.util.Objects;

@FunctionalInterface
public interface IHasBooleanIterator {
    IBooleanIterator iterator();
    
    default Iterable<Boolean> iterable() {return () -> iterator().toIterator();}
    
    /** Iterable like stuffs */
    default void forEach(IBooleanConsumer1 aCon) {
        Objects.requireNonNull(aCon);
        final IBooleanIterator it = iterator();
        while (it.hasNext()) aCon.run(it.next());
    }
}
