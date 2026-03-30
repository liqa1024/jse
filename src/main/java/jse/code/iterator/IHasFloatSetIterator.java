package jse.code.iterator;

@FunctionalInterface
public interface IHasFloatSetIterator extends IHasFloatSetOnlyIterator {
    IFloatSetIterator setIterator();
}
