package jse.code.iterator;

import jse.code.functional.IFloatSupplier;

import java.util.Objects;

@FunctionalInterface
public interface IHasFloatSetOnlyIterator {
    IFloatSetOnlyIterator setIterator();
    
    /** Iterable like stuffs */
    default void assign(IFloatSupplier aSup) {
        Objects.requireNonNull(aSup);
        final IFloatSetOnlyIterator si = setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsFloat());
    }
}
