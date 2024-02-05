package jse.math.matrix;

import jse.math.IComplexDouble;

@FunctionalInterface
public interface IComplexMatrixGetter {
    IComplexDouble get(int aRow, int aCol);
    default double getReal(int aRow, int aCol) {return get(aRow, aCol).real();}
    default double getImag(int aRow, int aCol) {return get(aRow, aCol).imag();}
}
