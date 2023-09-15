package com.jtool.code.functional;

@FunctionalInterface
public interface IConsumer2<TL, TR> {
    void run(TL aLHS, TR aRHS);
}
