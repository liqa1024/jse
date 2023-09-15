package com.jtool.code.iterator;

@FunctionalInterface
public interface IHasComplexDoubleSetIterator extends IHasComplexDoubleSetOnlyIterator {
    IComplexDoubleSetIterator setIterator();
}
