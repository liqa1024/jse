package jse.lmp;

import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class Data extends Lmpdat {
    private Data(int aAtomTypeNum, Box aBox, @Nullable IVector aMasses, IntVector aAtomID, IntVector aAtomType, RowMatrix aAtomXYZ, @Nullable RowMatrix aVelocities) {super(aAtomTypeNum, aBox, aMasses, aAtomID, aAtomType, aAtomXYZ, aVelocities);}
}
