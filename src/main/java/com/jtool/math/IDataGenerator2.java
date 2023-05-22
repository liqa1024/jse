package com.jtool.math;

import com.jtool.math.operator.IOperator2Full;

import java.util.concurrent.Callable;

public interface IDataGenerator2<T> extends IDataGenerator<T> {
    T from(IOperator2Full<? extends Number, Integer, Integer> aOpt);
    
    T ones(int aRowNum, int aColNum);
    T zeros(int aRowNum, int aColNum);
    T from(int aRowNum, int aColNum, Callable<? extends Number> aCall);
    T from(int aRowNum, int aColNum, IOperator2Full<? extends Number, Integer, Integer> aOpt);
}
