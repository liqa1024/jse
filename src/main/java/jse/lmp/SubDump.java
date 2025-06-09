package jse.lmp;

import jse.math.table.Table;
import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class SubDump extends SubLammpstrj {
    private SubDump(long aTimeStep, String[] aBoxBounds, LmpBox aBox, Table aAtomData) {super(aTimeStep, aBoxBounds, aBox, aAtomData);}
}
