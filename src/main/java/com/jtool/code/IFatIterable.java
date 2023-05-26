package com.jtool.code;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.UnaryOperator;

/**
 * 可以获取 ${@link ISetIterator} 的容器，并且可以获取指定类型 T 的迭代器
 * 提供了一个 replaceAll 接口（java），不会实际使用
 * @author liqa
 * @param <T> 可以获取的对方适应自身的迭代器方法，一般是不能继承 Iterable 的类
 * @param <TE> 获取到 T 的迭代器的最低元素类型，一般有 E extends TE（不强制）
 * @param <E> 容器元素类型，一般有 E extends S
 * @param <S> 用于设置的输入类型
 */
public interface IFatIterable<T, TE, E extends S, S> extends Iterable<E> {
    Iterator<? extends TE> iteratorOf(T aContainer);
    ISetIterator<E, S> setIterator();
    
    default void replaceAll(UnaryOperator<E> operator) {
        Objects.requireNonNull(operator);
        final ISetIterator<E, S> si = setIterator();
        while (si.hasNext()) {
            si.set(operator.apply(si.next()));
        }
    }
}
