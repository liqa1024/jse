package jtool.lmp;

import jtool.math.matrix.RowMatrix;
import jtool.math.vector.IVector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class Data extends Lmpdat {
    public Data(int aAtomTypeNum, Box aBox, @Nullable IVector aMasses, RowMatrix aAtomData, @Nullable RowMatrix aVelocities) {super(aAtomTypeNum, aBox, aMasses, aAtomData, aVelocities);}
}
