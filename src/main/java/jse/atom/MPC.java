package jse.atom;

import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;

@VisibleForTesting
public final class MPC extends MonatomicParameterCalculator {
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBox) {super(aAtomDataXYZ, aBox);}
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBox, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {super(aAtomDataXYZ, aBox, aThreadNum);}
    public MPC(IAtomData aAtomData) {super(aAtomData);}
    public MPC(IAtomData aAtomData, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {super(aAtomData, aThreadNum);}
}
