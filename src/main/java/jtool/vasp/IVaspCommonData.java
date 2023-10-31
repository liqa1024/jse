package jtool.vasp;

import jtool.math.matrix.IMatrix;
import jtool.math.vector.IVector;
import org.jetbrains.annotations.NotNull;

public interface IVaspCommonData {
    String dataName();
    String @NotNull[] atomTypes();
    IVector atomNumbers();
    IMatrix vaspBox();
    double vaspBoxScale();
}
