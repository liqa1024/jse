package com.jtool.code.iterator;

import java.util.Iterator;


/**
 * 带有 set 函数的迭代器，可以在迭代过程中进行设置
 * @author liqa
 */
public interface ISetIterator<E> extends Iterator<E>, ISetOnlyIterator<E> {
}
