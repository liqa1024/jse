package jtool.vasp;

import jtool.math.matrix.IMatrix;
import jtool.math.vector.IVector;
import org.jetbrains.annotations.Nullable;

public interface IVaspCommonData {
    String dataName();
    String @Nullable[] atomTypes();
    IVector atomNumbers();
    IMatrix vaspBox();
    double vaspBoxScale();
    boolean isCartesian();
    boolean isDiagBox();
    @Nullable IVector ids();
}
