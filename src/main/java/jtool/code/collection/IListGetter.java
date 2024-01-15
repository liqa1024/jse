package jtool.code.collection;

@FunctionalInterface
public interface IListGetter<R> {
    R get(int aIdx);
}
