package com.jtool.math;

import com.jtool.math.operator.IOperator1Full;

import java.util.concurrent.Callable;

public interface IDataGenerator1<T> extends IDataGenerator<T> {
    T from(IOperator1Full<? extends Number, Integer> aOpt);
    
    T ones(int aSize);
    T zeros(int aSize);
    T from(int aSize, Callable<? extends Number> aCall);
    T from(int aSize, IOperator1Full<? extends Number, Integer> aOpt);
}
