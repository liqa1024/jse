package jse.atom;

import org.jetbrains.annotations.NotNull;

public class BoxPrism implements IBox {
    private final @NotNull XYZ mA, mB, mC;
    public BoxPrism(@NotNull IXYZ aA, @NotNull IXYZ aB, @NotNull IXYZ aC) {mA = new XYZ(aA); mB = new XYZ(aB); mC = new XYZ(aC);}
    
    @Override public boolean isPrism() {return true;}
    @Override public IXYZ a() {return mA;}
    @Override public IXYZ b() {return mB;}
    @Override public IXYZ c() {return mC;}
    @Override public double volume() {return mA.mixed(mB, mC);}
    
    @Override public IBox copy() {return new BoxPrism(mA, mB, mC);}
    
    @Override public boolean isLmpStyle() {return false;}
    
    /** 为了加速运算，内部会缓存中间变量，因此原则上 mA，mB，mC 都是不能修改的，虽然为了性能这里没有那么严格 */
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
    
    @Override public String toString() {
        return String.format("a: (%.4g, %.4g, %.4g)\n", mA.mX, mA.mY, mA.mZ)
             + String.format("b: (%.4g, %.4g, %.4g)\n", mB.mX, mB.mY, mB.mZ)
             + String.format("c: (%.4g, %.4g, %.4g)"  , mC.mX, mC.mY, mC.mZ);
    }
}
