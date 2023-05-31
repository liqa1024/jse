package com.jtool.lmp;

import com.jtool.math.table.ITable;
import com.jtool.math.vector.IVector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class Data extends Lmpdat {
    Data(int aAtomTypeNum, Box aBox, @Nullable IVector aMasses, ITable aAtomData) {super(aAtomTypeNum, aBox, aMasses, aAtomData);}
}
