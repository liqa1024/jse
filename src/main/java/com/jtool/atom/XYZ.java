package com.jtool.atom;

/**
 * {@link IHasXYZ} 的一般实现，考虑到效率这里可以直接访问内部成员，从而避免多态函数调用的损失
 * @author liqa
 */
public final class XYZ implements IHasXYZ {
    public double mX, mY, mZ;
    public XYZ(double aX, double aY, double aZ) {
        mX = aX; mY = aY; mZ = aZ;
    }
    public XYZ(IHasXYZ aXYZ) {
        mX = aXYZ.x(); mY = aXYZ.y(); mZ = aXYZ.z();
    }
    public XYZ(XYZ aXYZ) {
        mX = aXYZ.mX; mY = aXYZ.mY; mZ = aXYZ.mZ;
    }
    
    @Override public double x() {return mX;}
    @Override public double y() {return mY;}
    @Override public double z() {return mZ;}
    
    /** 重写这些运算来优化多态调用的损失 */
    @Override public double product() {
        return mX * mY * mZ;
    }
    @Override public XYZ minus(IHasXYZ aRHS) {
        return new XYZ(mX-aRHS.x(), mY-aRHS.y(), mZ-aRHS.z());
    }
    @Override public XYZ minus(double aX, double aY, double aZ) {
        return new XYZ(mX-aX, mY-aY, mZ-aZ);
    }
    @Override public XYZ minus(double aRHS) {
        return new XYZ(mX-aRHS, mY-aRHS, mZ-aRHS);
    }
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public XYZ minus(XYZ aRHS) {
        return new XYZ(mX-aRHS.mX, mY-aRHS.mY, mZ-aRHS.mZ);
    }
    
    @Override public XYZ multiply(double aRHS) {
        return new XYZ(mX*aRHS, mY*aRHS, mZ*aRHS);
    }
}
