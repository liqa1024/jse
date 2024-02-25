package jse.vasp;

import jse.math.matrix.IMatrix;
import jse.math.vector.IIntVector;
import org.jetbrains.annotations.Nullable;

public interface IVaspCommonData {
    @Nullable String comment();
    String @Nullable[] typeNames();
    IIntVector atomNumbers();
    IMatrix vaspBox();
    double vaspBoxScale();
    boolean isCartesian();
    boolean isDiagBox();
    @Nullable IIntVector ids();
}
