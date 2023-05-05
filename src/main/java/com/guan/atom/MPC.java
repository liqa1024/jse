package com.guan.atom;

@Deprecated
public final class MPC extends MonatomicParameterCalculator {
    public MPC(IHasOrthogonalXYZ aHasOrthogonalXYZ) {super(aHasOrthogonalXYZ);}
    public MPC(IHasOrthogonalXYZ aHasOrthogonalXYZ, int aThreadNum) {super(aHasOrthogonalXYZ, aThreadNum);}
    public MPC(IHasOrthogonalXYZ aHasOrthogonalXYZ, int aThreadNum, double aCellStep) {super(aHasOrthogonalXYZ, aThreadNum, aCellStep);}
    public MPC(double[][] aAtomDataXYZ) {super(aAtomDataXYZ);}
    public MPC(double[][] aAtomDataXYZ, double[] aBox) {super(aAtomDataXYZ, aBox);}
    public MPC(double[][] aAtomDataXYZ, double[] aBoxLo, double[] aBoxHi) {super(aAtomDataXYZ, aBoxLo, aBoxHi);}
    public MPC(double[][] aAtomDataXYZ, double[] aBoxLo, double[] aBoxHi, int aThreadNum) {super(aAtomDataXYZ, aBoxLo, aBoxHi, aThreadNum);}
}
