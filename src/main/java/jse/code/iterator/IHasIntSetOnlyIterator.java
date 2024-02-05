package jse.code.iterator;

import java.util.Objects;
import java.util.function.IntSupplier;

@FunctionalInterface
public interface IHasIntSetOnlyIterator {
    IIntSetOnlyIterator setIterator();
    
    /** Iterable like stuffs */
    default void assign(IntSupplier aSup) {
        Objects.requireNonNull(aSup);
        final IIntSetOnlyIterator si = setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.getAsInt());
    }
}
