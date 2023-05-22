package com.jtool.math;

import java.util.concurrent.Callable;

/**
 * 任意的通用的数据生成器
 * @author liqa
 */
public interface IDataGenerator<T> {
    T ones();
    T zeros();
    T same();
    T from(Callable<? extends Number> aCall);
}
