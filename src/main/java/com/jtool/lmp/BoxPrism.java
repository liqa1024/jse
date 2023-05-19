package com.jtool.lmp;

import com.jtool.math.MathEX;
import org.jetbrains.annotations.NotNull;

import static com.jtool.code.CS.BOX_ONE;
import static com.jtool.code.CS.BOX_ZERO;

public class BoxPrism extends Box {
    final double mXY, mXZ, mYZ;
    
    public BoxPrism(double aSize, double aXY, double aXZ, double aYZ) {super(aSize); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(double aX, double aY, double aZ, double aXY, double aXZ, double aYZ) {super(aX, aY, aZ); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi, double aXY, double aXZ, double aYZ) {super(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(double @NotNull[] aBox, double aXY, double aXZ, double aYZ) {super(aBox); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(double @NotNull[] aBoxLo, double @NotNull[] aBoxHi, double aXY, double aXZ, double aYZ) {super(aBoxLo, aBoxHi); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    
    /// 获取属性
    public double xy() {return mXY;}
    public double xz() {return mXZ;}
    public double yz() {return mYZ;}
    
    @Override public BoxPrism copy() {return new BoxPrism(mBoxLo==BOX_ZERO?BOX_ZERO:MathEX.Vec.copy(mBoxLo), mBoxHi==BOX_ONE?BOX_ONE:MathEX.Vec.copy(mBoxHi), mXY, mXZ, mYZ);}
    
    @Override protected Type type() {return Type.PRISM;}
}
