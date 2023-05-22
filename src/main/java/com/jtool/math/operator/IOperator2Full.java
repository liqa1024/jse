package com.jtool.math.operator;

@FunctionalInterface
public interface IOperator2Full<R, TL, TR> {
    R cal(TL aLHS, TR aRHS);
}
