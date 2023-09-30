package com.jtool.rareevent;

import java.util.Iterator;

/**
 * 内部使用的迭代器，除了一般的迭代，还可以返回到此步时经过的时间以及此步对应的参数
 * @author liqa
 */
public interface ITimeAndParameterIterator<E> extends Iterator<E> {
    double timeConsumed();
    double lambda();
}
