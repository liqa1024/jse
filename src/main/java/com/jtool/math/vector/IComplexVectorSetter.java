package com.jtool.math.vector;

import com.jtool.math.IComplexDouble;

public interface IComplexVectorSetter {
    default void set(int aIdx, IComplexDouble aValue) {setReal(aIdx, aValue.real()); setImag(aIdx, aValue.imag());}
    default void set(int aIdx, double aValue) {setReal(aIdx, aValue); setImag(aIdx, 0.0);}
    void setReal(int aIdx, double aReal);
    void setImag(int aIdx, double aImag);
}
