package jse.code.iterator;

import java.util.Objects;
import java.util.function.BooleanSupplier;

@FunctionalInterface
public interface IHasBooleanSetOnlyIterator {
    IBooleanSetOnlyIterator setIterator();
    
    /** Iterable like stuffs */
    default void assign(BooleanSupplier aSup) {
        Objects.requireNonNull(aSup);
        final IBooleanSetOnlyIterator si = setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsBoolean());
    }
}
