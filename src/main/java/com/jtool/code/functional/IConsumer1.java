package com.jtool.code.functional;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.function.Consumer;

@FunctionalInterface
public interface IConsumer1<T> extends Consumer<T> {
    void run(T aInput);
    
    @VisibleForTesting @Override default void accept(T t) {run(t);}
}
