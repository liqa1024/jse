package com.jtool.lmp;

import com.jtool.atom.IHasXYZ;
import com.jtool.atom.XYZ;
import org.jetbrains.annotations.NotNull;

import static com.jtool.code.CS.BOX_ONE;
import static com.jtool.code.CS.BOX_ZERO;
import static com.jtool.code.UT.Code.toBOX;

public class BoxPrism extends Box {
    private final double mXY, mXZ, mYZ;
    
    public BoxPrism(double aSize, double aXY, double aXZ, double aYZ) {super(aSize); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(double aX, double aY, double aZ, double aXY, double aXZ, double aYZ) {super(aX, aY, aZ); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi, double aXY, double aXZ, double aYZ) {super(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(@NotNull IHasXYZ aBox, double aXY, double aXZ, double aYZ) {super(aBox); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(@NotNull IHasXYZ aBoxLo, @NotNull IHasXYZ aBoxHi, double aXY, double aXZ, double aYZ) {super(aBoxLo, aBoxHi); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(Box aBox, double aXY, double aXZ, double aYZ) {super(aBox); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(BoxPrism aBoxPrism) {super(aBoxPrism); mXY = aBoxPrism.mXY; mXZ = aBoxPrism.mXZ; mYZ = aBoxPrism.mYZ;}
    
    /// 获取属性
    public double xy() {return mXY;}
    public double xz() {return mXZ;}
    public double yz() {return mYZ;}
    
    @Override public BoxPrism copy() {return new BoxPrism(this);}
    @Override protected Type type() {return Type.PRISM;}
}
