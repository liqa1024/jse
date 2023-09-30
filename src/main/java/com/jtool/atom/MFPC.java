package com.jtool.atom;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;

@VisibleForTesting
public final class MFPC extends MultiFrameParameterCalculator {
    public MFPC(Collection<? extends Collection<? extends IAtom>> aAtomDataList, IXYZ aBox, double aTimestep, int aThreadNum) {super(aAtomDataList, aBox, aTimestep, aThreadNum);}
    public MFPC(Collection<? extends Collection<? extends IAtom>> aAtomDataList, IXYZ aBox, double aTimestep) {super(aAtomDataList, aBox, aTimestep);}
    public MFPC(Collection<? extends IAtomData> aAtomDataList, double aTimestep) {super(aAtomDataList, aTimestep);}
    public MFPC(Collection<? extends IAtomData> aAtomDataList, double aTimestep, int aThreadNum) {super(aAtomDataList, aTimestep, aThreadNum);}
}
