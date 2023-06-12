package com.jtool.code.iterator;

import java.util.Iterator;

/**
 * 可以获取 ${@link ISetIterator} 的容器，并且可以获取指定类型 T 的迭代器
 * @author liqa
 * @param <T> 可以获取的对方适应自身的迭代器方法，一般是不能继承 Iterable 的类
 * @param <E> 容器元素类型
 */
public interface IFatIterable<T, E> extends Iterable<E> {
    Iterator<E> iteratorOf(T aContainer);
    ISetIterator<E> setIterator();
}
