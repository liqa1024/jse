package com.jtool.atom;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;

@VisibleForTesting
public final class MPC extends MonatomicParameterCalculator {
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, XYZ aBoxLo, XYZ aBoxHi, int aThreadNum, double aCellStep) {super(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum, aCellStep);}
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ) {super(aAtomDataXYZ);}
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBox) {super(aAtomDataXYZ, aBox);}
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBoxLo, IXYZ aBoxHi) {super(aAtomDataXYZ, aBoxLo, aBoxHi);}
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBoxLo, IXYZ aBoxHi, int aThreadNum) {super(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum);}
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBoxLo, IXYZ aBoxHi, int aThreadNum, double aCellStep) {super(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum, aCellStep);}
    public MPC(IAtomData aAtomData) {super(aAtomData);}
    public MPC(IAtomData aAtomData, int aThreadNum) {super(aAtomData, aThreadNum);}
    public MPC(IAtomData aAtomData, int aThreadNum, double aCellStep) {super(aAtomData, aThreadNum, aCellStep);}
}
