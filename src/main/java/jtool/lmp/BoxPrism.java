package jtool.lmp;

import jtool.atom.IXYZ;
import org.jetbrains.annotations.NotNull;

public class BoxPrism extends Box {
    private final double mXY, mXZ, mYZ;
    
    public BoxPrism(double aSize, double aXY, double aXZ, double aYZ) {super(aSize); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(double aX, double aY, double aZ, double aXY, double aXZ, double aYZ) {super(aX, aY, aZ); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi, double aXY, double aXZ, double aYZ) {super(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(@NotNull IXYZ aBox, double aXY, double aXZ, double aYZ) {super(aBox); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(@NotNull IXYZ aBoxLo, @NotNull IXYZ aBoxHi, double aXY, double aXZ, double aYZ) {super(aBoxLo, aBoxHi); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(Box aBox, double aXY, double aXZ, double aYZ) {super(aBox); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public BoxPrism(BoxPrism aBoxPrism) {super(aBoxPrism); mXY = aBoxPrism.mXY; mXZ = aBoxPrism.mXZ; mYZ = aBoxPrism.mYZ;}
    
    /// 获取属性
    public final double xy() {return mXY;}
    public final double xz() {return mXZ;}
    public final double yz() {return mYZ;}
    
    @Override public BoxPrism copy() {return new BoxPrism(this);}
    @Override protected Type type() {return Type.PRISM;}
    
    @Override public String toString() {
        return String.format("{boxlo: (%.4g, %.4g, %.4g), boxhi: (%.4g, %.4g, %.4g), xy: %.4g, xz: %.4g, yz: %.4g}",
                             boxLo().x(), boxLo().y(), boxLo().z(), boxHi().x(), boxHi().y(), boxHi().z(), mXY, mXZ, mYZ);
    }
}
