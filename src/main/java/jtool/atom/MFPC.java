package jtool.atom;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;

@VisibleForTesting
public final class MFPC extends MultiFrameParameterCalculator {
    public MFPC(Collection<? extends Collection<? extends IAtom>> aAtomDataList, Collection<? extends IXYZ> aBoxList, double aTimestep, int aThreadNum) {super(aAtomDataList, aBoxList, aTimestep, aThreadNum);}
    public MFPC(Collection<? extends Collection<? extends IAtom>> aAtomDataList, Collection<? extends IXYZ> aBoxList, double aTimestep) {super(aAtomDataList, aBoxList, aTimestep);}
    public MFPC(Collection<? extends IAtomData> aAtomDataList, double aTimestep) {super(aAtomDataList, aTimestep);}
    public MFPC(Collection<? extends IAtomData> aAtomDataList, double aTimestep, int aThreadNum) {super(aAtomDataList, aTimestep, aThreadNum);}
}
