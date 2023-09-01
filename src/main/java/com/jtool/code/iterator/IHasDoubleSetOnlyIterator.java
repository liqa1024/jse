package com.jtool.code.iterator;

import com.jtool.code.functional.IDoubleSupplier;

import java.util.Objects;

@FunctionalInterface
public interface IHasDoubleSetOnlyIterator {
    IDoubleSetOnlyIterator setIterator();
    
    /** Iterable like stuffs */
    default void assign(IDoubleSupplier aSup) {
        Objects.requireNonNull(aSup);
        final IDoubleSetOnlyIterator si = setIterator();
        while (si.hasNext()) si.nextAndSet(aSup.get());
    }
}
