package com.jtool.code;

import java.util.Iterator;


/**
 * 带有 set 函数的迭代器，可以在迭代过程中进行设置
 * @author liqa
 */
public interface ISetIterator<E> extends Iterator<E> {
    void set(E e);
    /** 高性能接口，一次完成下一步和设置过程 */
    default void nextAndSet(E e) {
        next();
        set(e);
    }
    /** 高性能接口，一次完成下一步和设置过程，并且会返回旧的值 */
    default E getNextAndSet(E e) {
        E oValue = next();
        set(e);
        return oValue;
    }
}
