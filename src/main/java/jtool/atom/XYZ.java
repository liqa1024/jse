package jtool.atom;

import jtool.math.MathEX;
import jtool.math.vector.IVector;

/**
 * {@link IXYZ} 的一般实现，考虑到效率这里可以直接访问内部成员，从而避免多态函数调用的损失
 * @author liqa
 */
public final class XYZ implements IXYZ {
    /**
     * Convert IXYZ to XYZ to optimise, result should be read only!
     * @author liqa
     */
    public static XYZ toXYZ(IXYZ aXYZ) {
        return (aXYZ instanceof XYZ) ? (XYZ)aXYZ : new XYZ(aXYZ);
    }
    
    public double mX, mY, mZ;
    public XYZ(double aX, double aY, double aZ) {
        mX = aX; mY = aY; mZ = aZ;
    }
    public XYZ(IXYZ aXYZ) {
        mX = aXYZ.x(); mY = aXYZ.y(); mZ = aXYZ.z();
    }
    public XYZ(XYZ aXYZ) {
        mX = aXYZ.mX; mY = aXYZ.mY; mZ = aXYZ.mZ;
    }
    public XYZ(IVector aXYZ) {
        mX = aXYZ.get(0); mY = aXYZ.get(1); mZ = aXYZ.get(2);
    }
    
    /** 批量设置的接口，返回自身方便链式调用 */
    public XYZ setXYZ(double aX, double aY, double aZ) {
        mX = aX; mY = aY; mZ = aZ;
        return this;
    }
    public XYZ setXYZ(IXYZ aXYZ) {
        mX = aXYZ.x(); mY = aXYZ.y(); mZ = aXYZ.z();
        return this;
    }
    public XYZ setXYZ(XYZ aXYZ) {
        mX = aXYZ.mX; mY = aXYZ.mY; mZ = aXYZ.mZ;
        return this;
    }
    
    /** print */
    @Override public String toString() {return String.format("(%.4g, %.4g, %.4g)", mX, mY, mZ);}
    
    
    @Override public double x() {return mX;}
    @Override public double y() {return mY;}
    @Override public double z() {return mZ;}
    
    /** 重写这些运算来优化多态调用的损失 */
    @Override public double prod() {return mX * mY * mZ;}
    @Override public double min() {return Math.min(Math.min(mX, mY), mZ);}
    @Override public double max() {return Math.max(Math.max(mX, mY), mZ);}
    
    @Override public XYZ cross(double aX, double aY, double aZ) {return new XYZ(mY*aZ - aY*mZ, mZ*aX - aZ*mX, mX*aY - aX*mY);}
    public XYZ cross(XYZ aRHS) {return cross(aRHS.mX, aRHS.mY, aRHS.mZ);}
    public void cross2this(IXYZ aRHS) {cross2this(aRHS.x(), aRHS.y(), aRHS.z());}
    public void cross2this(XYZ aRHS) {cross2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    public void cross2this(double aX, double aY, double aZ) {
        double tX = mX;
        double tY = mY;
        double tZ = mZ;
        mX = tY*aZ - aY*tZ;
        mY = tZ*aX - aZ*tX;
        mZ = tX*aY - aX*tY;
    }
    @Override public XYZ negative() {return new XYZ(-mX, -mY, -mZ);}
    public void negative2this() {mX = -mX; mY = -mY; mZ = -mZ;}
    @Override public double norm() {return MathEX.Fast.hypot(mX, mY, mZ);}
    
    @Override public XYZ plus(IXYZ aRHS) {return new XYZ(mX+aRHS.x(), mY+aRHS.y(), mZ+aRHS.z());}
    @Override public XYZ plus(double aX, double aY, double aZ) {return new XYZ(mX+aX, mY+aY, mZ+aZ);}
    @Override public XYZ plus(double aRHS) {return new XYZ(mX+aRHS, mY+aRHS, mZ+aRHS);}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public XYZ plus(XYZ aRHS) {return new XYZ(mX+aRHS.mX, mY+aRHS.mY, mZ+aRHS.mZ);}
    public void plus2this(XYZ aRHS) {mX += aRHS.mX; mY += aRHS.mY; mZ += aRHS.mZ;}
    public void plus2this(double aRHS) {mX += aRHS; mY += aRHS; mZ += aRHS;}
    
    @Override public XYZ minus(IXYZ aRHS) {return new XYZ(mX-aRHS.x(), mY-aRHS.y(), mZ-aRHS.z());}
    @Override public XYZ minus(double aX, double aY, double aZ) {return new XYZ(mX-aX, mY-aY, mZ-aZ);}
    @Override public XYZ minus(double aRHS) {return new XYZ(mX-aRHS, mY-aRHS, mZ-aRHS);}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public XYZ minus(XYZ aRHS) {return new XYZ(mX-aRHS.mX, mY-aRHS.mY, mZ-aRHS.mZ);}
    public void minus2this(XYZ aRHS) {mX -= aRHS.mX; mY -= aRHS.mY; mZ -= aRHS.mZ;}
    public void minus2this(double aRHS) {mX -= aRHS; mY -= aRHS; mZ -= aRHS;}
    
    @Override public XYZ multiply(IXYZ aRHS) {return new XYZ(mX*aRHS.x(), mY*aRHS.y(), mZ*aRHS.z());}
    @Override public XYZ multiply(double aX, double aY, double aZ) {return new XYZ(mX*aX, mY*aY, mZ*aZ);}
    @Override public XYZ multiply(double aRHS) {return new XYZ(mX*aRHS, mY*aRHS, mZ*aRHS);}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public XYZ multiply(XYZ aRHS) {return new XYZ(mX*aRHS.mX, mY*aRHS.mY, mZ*aRHS.mZ);}
    public void multiply2this(XYZ aRHS) {mX *= aRHS.mX; mY *= aRHS.mY; mZ *= aRHS.mZ;}
    public void multiply2this(double aRHS) {mX *= aRHS; mY *= aRHS; mZ *= aRHS;}
    
    @Override public XYZ div(IXYZ aRHS) {return new XYZ(mX/aRHS.x(), mY/aRHS.y(), mZ/aRHS.z());}
    @Override public XYZ div(double aX, double aY, double aZ) {return new XYZ(mX/aX, mY/aY, mZ/aZ);}
    @Override public XYZ div(double aRHS) {return new XYZ(mX/aRHS, mY/aRHS, mZ/aRHS);}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public XYZ div(XYZ aRHS) {return new XYZ(mX/aRHS.mX, mY/aRHS.mY, mZ/aRHS.mZ);}
    public void div2this(XYZ aRHS) {mX /= aRHS.mX; mY /= aRHS.mY; mZ /= aRHS.mZ;}
    public void div2this(double aRHS) {mX /= aRHS; mY /= aRHS; mZ /= aRHS;}
    
    
    @Override public double distance2(IXYZ aRHS) {
        double tX = mX - aRHS.x();
        double tY = mY - aRHS.y();
        double tZ = mZ - aRHS.z();
        return tX*tX + tY*tY + tZ*tZ;
    }
    @Override public double distance2(double aX, double aY, double aZ) {
        aX -= mX;
        aY -= mY;
        aZ -= mZ;
        return aX*aX + aY*aY + aZ*aZ;
    }
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public double distance2(XYZ aRHS) {
        double tX = mX - aRHS.mX;
        double tY = mY - aRHS.mY;
        double tZ = mZ - aRHS.mZ;
        return tX*tX + tY*tY + tZ*tZ;
    }
    @Override public double distance(IXYZ aRHS) {return MathEX.Fast.hypot(mX-aRHS.x(), mY-aRHS.y(), mZ-aRHS.z());}
    @Override public double distance(double aX, double aY, double aZ) {return MathEX.Fast.hypot(mX-aX, mY-aY, mZ-aZ);}
    public double distance(XYZ aRHS) {return MathEX.Fast.hypot(mX-aRHS.mX, mY-aRHS.mY, mZ-aRHS.mZ);}
    public double distanceQuick(XYZ aRHS) {return MathEX.Fast.sqrtQuick(distance2(aRHS));}
    
    @Override public double distanceMHT(IXYZ aRHS) {return Math.abs(mX-aRHS.x()) + Math.abs(mY-aRHS.y()) + Math.abs(mZ-aRHS.z());}
    @Override public double distanceMHT(double aX, double aY, double aZ) {
        aX -= mX;
        aY -= mY;
        aZ -= mZ;
        return Math.abs(aX) + Math.abs(aY) + Math.abs(aZ);
    }
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    public double distanceMHT(XYZ aRHS) {return Math.abs(mX-aRHS.mX) + Math.abs(mY-aRHS.mY) + Math.abs(mZ-aRHS.mZ);}
}
