package jse.code.iterator;

@FunctionalInterface
public interface IHasIntSetIterator extends IHasIntSetOnlyIterator {
    IIntSetIterator setIterator();
}
