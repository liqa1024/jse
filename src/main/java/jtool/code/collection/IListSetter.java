package jtool.code.collection;

@FunctionalInterface
public interface IListSetter<T> {
    void set(int aIdx, T aValue);
}
