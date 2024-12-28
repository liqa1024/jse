package jse.atom;

import org.jetbrains.annotations.Range;

/** @deprecated use {@link AtomicParameterCalculator} or {@link APC} */
@Deprecated public final class MPC extends MonatomicParameterCalculator {
    /** @deprecated use {@link #of(IAtomData)} */ @Deprecated private MPC(IAtomData aAtomData) {super(aAtomData);}
    /** @deprecated use {@link #of(IAtomData)} */ @Deprecated private MPC(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {super(aAtomData, aThreadNum);}
}
