package jse.atom;

import org.jetbrains.annotations.NotNull;

public class Box implements IBox {
    private final @NotNull XYZ mBox;
    public Box(@NotNull IXYZ aBox) {mBox = new XYZ(aBox);}
    public Box(double aX, double aY, double aZ) {mBox = new XYZ(aX, aY, aZ);}
    
    @Override public IXYZ a() {return new XYZ(mBox.mX, 0.0, 0.0);}
    @Override public IXYZ b() {return new XYZ(0.0, mBox.mY, 0.0);}
    @Override public IXYZ c() {return new XYZ(0.0, 0.0, mBox.mZ);}
    @Override public double volume() {return mBox.prod();}
    @Override public IBox copy() {return new Box(mBox);}
    
    @Override public double x() {return mBox.mX;}
    @Override public double y() {return mBox.mY;}
    @Override public double z() {return mBox.mZ;}
    
    @Override public void toCartesian(XYZ rDirect) {rDirect.multiply2this(mBox);}
    @Override public void toDirect(XYZ rCartesian) {rCartesian.div2this(mBox);}
    
    @Override public String toString() {
        return String.format("(%.4g, %.4g, %.4g)", mBox.mX, mBox.mY, mBox.mZ);
    }
}
