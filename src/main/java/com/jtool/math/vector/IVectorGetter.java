package com.jtool.math.vector;

@FunctionalInterface
public interface IVectorGetter<T extends Number> {
    T get(int aIdx);
}
