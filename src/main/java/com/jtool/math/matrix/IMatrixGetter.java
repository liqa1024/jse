package com.jtool.math.matrix;

@FunctionalInterface
public interface IMatrixGetter<T extends Number> {
    T get(int aRow, int aCol);
}
