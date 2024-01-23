package jtool.vasp;

import jtool.math.matrix.IMatrix;
import jtool.math.vector.IIntVector;
import org.jetbrains.annotations.Nullable;

public interface IVaspCommonData {
    String dataName();
    String @Nullable[] atomTypes();
    IIntVector atomNumbers();
    IMatrix vaspBox();
    double vaspBoxScale();
    boolean isCartesian();
    boolean isDiagBox();
    @Nullable IIntVector ids();
}
