package com.jtool.code.iterator;

@FunctionalInterface
public interface IHasDoubleSetIterator extends IHasDoubleSetOnlyIterator {
    IDoubleSetIterator setIterator();
}
