package com.jtool.atom;

import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class MPC extends MonatomicParameterCalculator {
    public MPC(Iterable<? extends IHasXYZ> aAtomDataXYZ, XYZ aBoxLo, XYZ aBoxHi, int aThreadNum, double aCellStep) {super(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum, aCellStep);}
    public MPC(Iterable<? extends IHasXYZ> aAtomDataXYZ) {super(aAtomDataXYZ);}
    public MPC(Iterable<? extends IHasXYZ> aAtomDataXYZ, IHasXYZ aBox) {super(aAtomDataXYZ, aBox);}
    public MPC(Iterable<? extends IHasXYZ> aAtomDataXYZ, IHasXYZ aBoxLo, IHasXYZ aBoxHi) {super(aAtomDataXYZ, aBoxLo, aBoxHi);}
    public MPC(Iterable<? extends IHasXYZ> aAtomDataXYZ, IHasXYZ aBoxLo, IHasXYZ aBoxHi, int aThreadNum) {super(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum);}
    public MPC(Iterable<? extends IHasXYZ> aAtomDataXYZ, IHasXYZ aBoxLo, IHasXYZ aBoxHi, int aThreadNum, double aCellStep) {super(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum, aCellStep);}
    public MPC(IHasAtomData aAtomData) {super(aAtomData);}
    public MPC(IHasAtomData aAtomData, int aThreadNum) {super(aAtomData, aThreadNum);}
    public MPC(IHasAtomData aAtomData, int aThreadNum, double aCellStep) {super(aAtomData, aThreadNum, aCellStep);}
}
