package com.guan.lmp;

import org.jetbrains.annotations.Nullable;

@Deprecated
public final class Data extends Lmpdat {
    public Data(int aAtomTypeNum, Box aBox, double @Nullable [] aMasses, double[][] aAtomData) {super(aAtomTypeNum, aBox, aMasses, aAtomData);}
    public Data(int aAtomTypeNum, Box aBox, double[][] aAtomData) {super(aAtomTypeNum, aBox, aAtomData);}
    public Data(int aAtomTypeNum, double[] aBox, double @Nullable [] aMasses, double[][] aAtomData) {super(aAtomTypeNum, aBox, aMasses, aAtomData);}
    public Data(int aAtomTypeNum, double[] aBoxLo, double[] aBoxHi, double @Nullable [] aMasses, double[][] aAtomData) {super(aAtomTypeNum, aBoxLo, aBoxHi, aMasses, aAtomData);}
}
