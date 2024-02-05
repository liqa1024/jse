package jse.code.iterator;

@FunctionalInterface
public interface IHasBooleanSetIterator extends IHasBooleanSetOnlyIterator {
    IBooleanSetIterator setIterator();
}
