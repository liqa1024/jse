package jse.atom;

import org.jetbrains.annotations.NotNull;

public final class Box implements IBox {
    private final @NotNull XYZ mBox;
    public Box(@NotNull IXYZ aBox) {mBox = new XYZ(aBox);}
    public Box(double aX, double aY, double aZ) {mBox = new XYZ(aX, aY, aZ);}
    
    @Override public Box copy() {return new Box(mBox);}
    
    @Override public double ax() {return mBox.mX;}
    @Override public double by() {return mBox.mY;}
    @Override public double cz() {return mBox.mZ;}
    
    @Override public String toString() {
        return String.format("(%.4g, %.4g, %.4g)", mBox.mX, mBox.mY, mBox.mZ);
    }
    
    /** optimize stuffs */
    @Override public double volume() {return mBox.prod();}
    @Override public void toCartesian(XYZ rDirect) {rDirect.multiply2this(mBox);}
    @Override public void toDirect(XYZ rCartesian) {rCartesian.div2this(mBox);}
}
