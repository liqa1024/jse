package jse.code.iterator;

@FunctionalInterface
public interface IHasLongSetIterator extends IHasLongSetOnlyIterator {
    ILongSetIterator setIterator();
}
