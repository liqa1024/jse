package jse.atom;

import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.VisibleForTesting;

/** @see AtomicParameterCalculator */
@VisibleForTesting
public final class APC extends AtomicParameterCalculator {
    /** @deprecated use {@link #of(IAtomData)} */ @Deprecated private APC(IAtomData aAtomData) {super(aAtomData);}
    /** @deprecated use {@link #of(IAtomData)} */ @Deprecated private APC(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {super(aAtomData, aThreadNum);}
}
