package jse.lmp;

import jse.atom.IXYZ;
import org.jetbrains.annotations.NotNull;

public final class LmpBoxPrism extends LmpBox {
    private final double mXY, mXZ, mYZ;
    
    public LmpBoxPrism(double aX, double aY, double aZ, double aXY, double aXZ, double aYZ) {super(aX, aY, aZ); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public LmpBoxPrism(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi, double aXY, double aXZ, double aYZ) {super(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public LmpBoxPrism(@NotNull IXYZ aBox, double aXY, double aXZ, double aYZ) {super(aBox); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    public LmpBoxPrism(@NotNull IXYZ aBoxLo, @NotNull IXYZ aBoxHi, double aXY, double aXZ, double aYZ) {super(aBoxLo, aBoxHi); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    LmpBoxPrism(LmpBox aLmpBox, double aXY, double aXZ, double aYZ) {super(aLmpBox); mXY = aXY; mXZ = aXZ; mYZ = aYZ;}
    LmpBoxPrism(LmpBoxPrism aLmpBoxPrism) {super(aLmpBoxPrism); mXY = aLmpBoxPrism.mXY; mXZ = aLmpBoxPrism.mXZ; mYZ = aLmpBoxPrism.mYZ;}
    
    /** IBox stuffs */
    @Override public boolean isPrism() {return true;}
    @SuppressWarnings("RedundantMethodOverride")
    @Override public boolean isLmpStyle() {return true;}
    @Override public double bx() {return mXY;}
    @Override public double cx() {return mXZ;}
    @Override public double cy() {return mYZ;}
    
    @Override public LmpBoxPrism copy() {return new LmpBoxPrism(this);}
    
    @Override public String toString() {
        return String.format("{boxlo: (%.4g, %.4g, %.4g), boxhi: (%.4g, %.4g, %.4g), xy: %.4g, xz: %.4g, yz: %.4g}",
                             xlo(), ylo(), zlo(), xhi(), yhi(), zhi(), mXY, mXZ, mYZ);
    }
}
