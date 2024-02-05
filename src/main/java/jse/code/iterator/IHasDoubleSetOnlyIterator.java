package jse.code.iterator;

import java.util.function.DoubleSupplier;

import java.util.Objects;

@FunctionalInterface
public interface IHasDoubleSetOnlyIterator {
    IDoubleSetOnlyIterator setIterator();
    
    /** Iterable like stuffs */
    default void assign(DoubleSupplier aSup) {
        Objects.requireNonNull(aSup);
        final IDoubleSetOnlyIterator si = setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsDouble());
    }
}
