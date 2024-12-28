package jse.atom;

import org.jetbrains.annotations.Range;

/** @deprecated use {@link AtomicParameterCalculator} or {@link APC} */
@Deprecated public class MonatomicParameterCalculator extends AtomicParameterCalculator {
    /** @deprecated use {@link #of(IAtomData)} */ @Deprecated protected MonatomicParameterCalculator(IAtomData aAtomData) {super(aAtomData);}
    /** @deprecated use {@link #of(IAtomData)} */ @Deprecated protected MonatomicParameterCalculator(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {super(aAtomData, aThreadNum);}
}
