package jse.atom;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;

@VisibleForTesting
public final class MFPC extends MultiFrameParameterCalculator {
    /** @deprecated use {@link #of} */ private MFPC(Collection<? extends IAtomData> aAtomDataList, double aTimestep) {super(aAtomDataList, aTimestep);}
    /** @deprecated use {@link #of} */ private MFPC(Collection<? extends IAtomData> aAtomDataList, double aTimestep, int aThreadNum) {super(aAtomDataList, aTimestep, aThreadNum);}
}
