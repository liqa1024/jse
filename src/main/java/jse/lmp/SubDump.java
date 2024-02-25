package jse.lmp;

import jse.math.table.ITable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

@VisibleForTesting
public final class SubDump extends SubLammpstrj {
    SubDump(long aTimeStep, String[] aBoxBounds, Box aBox, ITable aAtomData) {super(aTimeStep, aBoxBounds, aBox, aAtomData);}
}
