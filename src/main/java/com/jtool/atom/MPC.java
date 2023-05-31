package com.jtool.atom;

import org.jetbrains.annotations.VisibleForTesting;

@VisibleForTesting
public final class MPC extends MonatomicParameterCalculator {
    public MPC(IOrthogonalXYZData aHasOrthogonalXYZ) {super(aHasOrthogonalXYZ);}
    public MPC(IOrthogonalXYZData aHasOrthogonalXYZ, int aThreadNum) {super(aHasOrthogonalXYZ, aThreadNum);}
    public MPC(IOrthogonalXYZData aHasOrthogonalXYZ, int aThreadNum, double aCellStep) {super(aHasOrthogonalXYZ, aThreadNum, aCellStep);}
    public MPC(double[][] aAtomDataXYZ) {super(aAtomDataXYZ);}
    public MPC(double[][] aAtomDataXYZ, double[] aBox) {super(aAtomDataXYZ, aBox);}
    public MPC(double[][] aAtomDataXYZ, double[] aBoxLo, double[] aBoxHi) {super(aAtomDataXYZ, aBoxLo, aBoxHi);}
    public MPC(double[][] aAtomDataXYZ, double[] aBoxLo, double[] aBoxHi, int aThreadNum) {super(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum);}
}
