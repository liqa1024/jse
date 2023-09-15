package com.jtool.math.vector;

import com.jtool.math.IComplexDouble;

@FunctionalInterface
public interface IComplexVectorGetter {
    IComplexDouble get(int aIdx);
    default double getReal(int aIdx) {return get(aIdx).real();}
    default double getImag(int aIdx) {return get(aIdx).imag();}
}
