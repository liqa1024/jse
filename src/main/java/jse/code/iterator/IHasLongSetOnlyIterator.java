package jse.code.iterator;

import java.util.Objects;
import java.util.function.LongSupplier;

@FunctionalInterface
public interface IHasLongSetOnlyIterator {
    ILongSetOnlyIterator setIterator();
    
    /** Iterable like stuffs */
    default void assign(LongSupplier aSup) {
        Objects.requireNonNull(aSup);
        final ILongSetOnlyIterator si = setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsLong());
    }
}
