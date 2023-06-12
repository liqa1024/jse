package com.jtool.code.iterator;

/**
 * 仅支持设置的迭代器，在迭代过程只能设置不能访问
 * @author liqa
 */
public interface ISetOnlyIterator<E> {
    boolean hasNext();
    void nextOnly();
    void set(E e);
    /** 高性能接口，一次完成下一步和设置过程 */
    default void nextAndSet(E e) {
        nextOnly();
        set(e);
    }
}
