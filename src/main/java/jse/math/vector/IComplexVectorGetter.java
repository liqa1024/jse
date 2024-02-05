package jse.math.vector;

import jse.math.IComplexDouble;

@FunctionalInterface
public interface IComplexVectorGetter {
    IComplexDouble get(int aIdx);
    default double getReal(int aIdx) {return get(aIdx).real();}
    default double getImag(int aIdx) {return get(aIdx).imag();}
}
