package jse.code.collection;

@FunctionalInterface
public interface IListGetter<R> {
    R get(int aIdx);
}
