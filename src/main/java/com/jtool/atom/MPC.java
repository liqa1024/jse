package com.jtool.atom;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;

@VisibleForTesting
public final class MPC extends MonatomicParameterCalculator {
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBox, int aThreadNum, double aCellStep) {super(aAtomDataXYZ, aBox, aThreadNum, aCellStep);}
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ) {super(aAtomDataXYZ);}
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBox) {super(aAtomDataXYZ, aBox);}
    public MPC(Collection<? extends IXYZ> aAtomDataXYZ, IXYZ aBox, int aThreadNum) {super(aAtomDataXYZ, aBox, aThreadNum);}
    public MPC(IAtomData aAtomData) {super(aAtomData);}
    public MPC(IAtomData aAtomData, int aThreadNum) {super(aAtomData, aThreadNum);}
    public MPC(IAtomData aAtomData, int aThreadNum, double aCellStep) {super(aAtomData, aThreadNum, aCellStep);}
}
