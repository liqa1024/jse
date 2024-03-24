package jse.atom;

import org.jetbrains.annotations.NotNull;

public final class BoxPrism implements IBox {
    private final @NotNull XYZ mA, mB, mC;
    public BoxPrism(@NotNull IXYZ aA, @NotNull IXYZ aB, @NotNull IXYZ aC) {mA = new XYZ(aA); mB = new XYZ(aB); mC = new XYZ(aC);}
    public BoxPrism(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        mA = new XYZ(aAx, aAy, aAz);
        mB = new XYZ(aBx, aBy, aBz);
        mC = new XYZ(aCx, aCy, aCz);
    }
    
    @Override public boolean isLmpStyle() {return false;}
    @Override public boolean isPrism() {return true;}
    
    @Override public double ax() {return mA.mX;}
    @Override public double ay() {return mA.mY;}
    @Override public double az() {return mA.mZ;}
    @Override public double bx() {return mB.mX;}
    @Override public double by() {return mB.mY;}
    @Override public double bz() {return mB.mZ;}
    @Override public double cx() {return mC.mX;}
    @Override public double cy() {return mC.mY;}
    @Override public double cz() {return mC.mZ;}
    
    @Override public BoxPrism copy() {return new BoxPrism(mA, mB, mC);}
    
    @Override public String toString() {
        return String.format("a: (%.4g, %.4g, %.4g)\n", mA.mX, mA.mY, mA.mZ)
             + String.format("b: (%.4g, %.4g, %.4g)\n", mB.mX, mB.mY, mB.mZ)
             + String.format("c: (%.4g, %.4g, %.4g)"  , mC.mX, mC.mY, mC.mZ);
    }
    
    /** optimize stuffs */
    @Override public double volume() {return mA.mixed(mB, mC);}
    
    /** 为了加速运算，内部会缓存中间变量，因此这个实现的 mA，mB，mC 都是不能修改的 */
    @Override public void toCartesian(XYZ rDirect) {
        rDirect.setXYZ(
            mA.mX*rDirect.mX + mB.mX*rDirect.mY + mC.mX*rDirect.mZ,
            mA.mY*rDirect.mX + mB.mY*rDirect.mY + mC.mY*rDirect.mZ,
            mA.mZ*rDirect.mX + mB.mZ*rDirect.mY + mC.mZ*rDirect.mZ
        );
    }
    private XYZ mBC = null, mCA = null, mAB = null;
    private double mV = Double.NaN;
    @Override public void toDirect(XYZ rCartesian) {
        if (mBC == null) {
            mBC = mB.cross(mC);
            mCA = mC.cross(mA);
            mAB = mA.cross(mB);
            mV = mA.mixed(mB, mC);
        }
        rCartesian.setXYZ(
            mBC.dot(rCartesian) / mV,
            mCA.dot(rCartesian) / mV,
            mAB.dot(rCartesian) / mV
        );
    }
}
