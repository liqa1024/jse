package jse.vasp;

import jse.math.vector.IIntVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Obsolete
public interface IVaspCommonData {
    @Nullable String comment();
    String @Nullable[] typeNames();
    @ApiStatus.Obsolete IIntVector atomNumbers();
    boolean isCartesian();
    VaspBox box();
}
