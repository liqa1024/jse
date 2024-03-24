package jse.vasp;

import jse.math.vector.IIntVector;
import org.jetbrains.annotations.Nullable;

public interface IVaspCommonData {
    @Nullable String comment();
    String @Nullable[] typeNames();
    IIntVector atomNumbers();
    boolean isCartesian();
    @Nullable IIntVector ids();
    VaspBox box();
}
