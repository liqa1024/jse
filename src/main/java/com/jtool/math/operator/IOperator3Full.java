package com.jtool.math.operator;

@FunctionalInterface
public interface IOperator3Full<R, TA, TB, TC> {
    R cal(TA aA, TB aB, TC aC);
}
