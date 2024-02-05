package jse.code.iterator;

import java.util.Objects;
import java.util.function.LongConsumer;

@FunctionalInterface
public interface IHasLongIterator {
    ILongIterator iterator();
    
    default Iterable<Long> iterable() {return () -> iterator().toIterator();}
    
    /** Iterable like stuffs */
    default void forEach(LongConsumer aCon) {
        Objects.requireNonNull(aCon);
        final ILongIterator it = iterator();
        while (it.hasNext()) aCon.accept(it.next());
    }
    
    default IHasDoubleIterator asDouble() {return () -> IHasLongIterator.this.iterator().asDouble();}
}
