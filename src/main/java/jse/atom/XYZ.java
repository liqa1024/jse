package jse.atom;

import jse.math.MathEX;
import jse.math.vector.IVector;

/**
 * {@link IXYZ} 的一般实现，考虑到效率这里可以直接访问内部成员，从而避免多态函数调用的损失
 * @author liqa
 */
public final class XYZ extends AbstractSettableXYZ {
    /**
     * Convert IXYZ to XYZ to optimise, result should be read only!
     * @author liqa
     */
    public static XYZ toXYZ(IXYZ aXYZ) {
        return (aXYZ instanceof XYZ) ? (XYZ)aXYZ : new XYZ(aXYZ);
    }
    
    public double mX, mY, mZ;
    public XYZ() {this(0.0, 0.0, 0.0);}
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
    
    @Override public double x() {return mX;}
    @Override public double y() {return mY;}
    @Override public double z() {return mZ;}
    
    /** 批量设置的接口，返回自身方便链式调用 */
    @Override public XYZ setX(double aX) {mX = aX; return this;}
    @Override public XYZ setY(double aY) {mY = aY; return this;}
    @Override public XYZ setZ(double aZ) {mZ = aZ; return this;}
    @Override public XYZ setXYZ(double aX, double aY, double aZ) {mX = aX; mY = aY; mZ = aZ; return this;}
    @Override public XYZ setXYZ(IXYZ aXYZ) {return setXYZ(aXYZ.x(), aXYZ.y(), aXYZ.z());}
    
    /** 重写这些运算来优化多态调用的损失 */
    @Override public double prod() {return mX * mY * mZ;}
    @Override public double min() {return Math.min(Math.min(mX, mY), mZ);}
    @Override public double max() {return Math.max(Math.max(mX, mY), mZ);}
    
    @Override public double dot() {
        double tX = mX;
        double tY = mY;
        double tZ = mZ;
        return tX*tX + tY*tY + tZ*tZ;
    }
    @Override public double dot(double aX, double aY, double aZ) {return mX*aX + mY*aY + mZ*aZ;}
    
    @Override public XYZ cross(double aX, double aY, double aZ) {return new XYZ(mY*aZ - aY*mZ, mZ*aX - aZ*mX, mX*aY - aX*mY);}
    @Override public void cross2this(IXYZ aRHS) {cross2this(aRHS.x(), aRHS.y(), aRHS.z());}
    @Override public void cross2this(XYZ aRHS) {cross2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    @Override public void cross2this(double aX, double aY, double aZ) {
        double tX = mX;
        double tY = mY;
        double tZ = mZ;
        mX = tY*aZ - aY*tZ;
        mY = tZ*aX - aZ*tX;
        mZ = tX*aY - aX*tY;
    }
    
    @Override public double mixed(double aCX, double aCY, double aCZ, double aDX, double aDY, double aDZ) {return (mY*aCZ - aCY*mZ)*aDX + (mZ*aCX - aCZ*mX)*aDY + (mX*aCY - aCX*mY)*aDZ;}
    
    @Override public XYZ negative() {return new XYZ(-mX, -mY, -mZ);}
    @Override public void negative2this() {mX = -mX; mY = -mY; mZ = -mZ;}
    @Override public double norm() {return MathEX.Fast.hypot(mX, mY, mZ);}
    
    @Override public XYZ plus(double aX, double aY, double aZ) {return new XYZ(mX+aX, mY+aY, mZ+aZ);}
    @Override public XYZ plus(double aRHS) {return new XYZ(mX+aRHS, mY+aRHS, mZ+aRHS);}
    @Override public void plus2this(double aX, double aY, double aZ) {mX += aX; mY += aY; mZ += aZ;}
    @Override public void plus2this(double aRHS) {mX += aRHS; mY += aRHS; mZ += aRHS;}
    /** 也增加这个运算方便使用 */
    @Override public void mplus2this(double aX, double aY, double aZ, double aMul) {mX += aMul*aX; mY += aMul*aY; mZ += aMul*aZ;}
    
    @Override public XYZ minus(double aX, double aY, double aZ) {return new XYZ(mX-aX, mY-aY, mZ-aZ);}
    @Override public XYZ minus(double aRHS) {return new XYZ(mX-aRHS, mY-aRHS, mZ-aRHS);}
    @Override public void minus2this(double aX, double aY, double aZ) {mX -= aX; mY -= aY; mZ -= aZ;}
    @Override public void minus2this(double aRHS) {mX -= aRHS; mY -= aRHS; mZ -= aRHS;}
    
    @Override public XYZ lminus(double aX, double aY, double aZ) {return new XYZ(aX-mX, aY-mY, aZ-mZ);}
    @Override public XYZ lminus(double aRHS) {return new XYZ(aRHS-mX, aRHS-mY, aRHS-mZ);}
    @Override public void lminus2this(double aX, double aY, double aZ) {mX = aX-mX; mY = aY-mY; mZ = aZ-mZ;}
    @Override public void lminus2this(double aRHS) {mX = aRHS-mX; mY = aRHS-mY; mZ = aRHS-mZ;}
    
    @Override public XYZ multiply(double aX, double aY, double aZ) {return new XYZ(mX*aX, mY*aY, mZ*aZ);}
    @Override public XYZ multiply(double aRHS) {return new XYZ(mX*aRHS, mY*aRHS, mZ*aRHS);}
    @Override public void multiply2this(double aX, double aY, double aZ) {mX *= aX; mY *= aY; mZ *= aZ;}
    @Override public void multiply2this(double aRHS) {mX *= aRHS; mY *= aRHS; mZ *= aRHS;}
    
    @Override public XYZ div(double aX, double aY, double aZ) {return new XYZ(mX/aX, mY/aY, mZ/aZ);}
    @Override public XYZ div(double aRHS) {return new XYZ(mX/aRHS, mY/aRHS, mZ/aRHS);}
    @Override public void div2this(double aX, double aY, double aZ) {mX /= aX; mY /= aY; mZ /= aZ;}
    @Override public void div2this(double aRHS) {mX /= aRHS; mY /= aRHS; mZ /= aRHS;}
    
    @Override public XYZ ldiv(double aX, double aY, double aZ) {return new XYZ(aX/mX, aY/mY, aZ/mZ);}
    @Override public XYZ ldiv(double aRHS) {return new XYZ(aRHS/mX, aRHS/mY, aRHS/mZ);}
    @Override public void ldiv2this(double aX, double aY, double aZ) {mX = aX/mX; mY = aY/mY; mZ = aZ/mZ;}
    @Override public void ldiv2this(double aRHS) {mX = aRHS/mX; mY = aRHS/mY; mZ = aRHS/mZ;}
    
    
    @Override public double distance2(double aX, double aY, double aZ) {
        aX -= mX;
        aY -= mY;
        aZ -= mZ;
        return aX*aX + aY*aY + aZ*aZ;
    }
    @Override public double distance(double aX, double aY, double aZ) {return MathEX.Fast.hypot(mX-aX, mY-aY, mZ-aZ);}
    @Override public double distanceMHT(double aX, double aY, double aZ)  {return Math.abs(mX-aX) + Math.abs(mY-aY) + Math.abs(mZ-aZ);}
    
    @Override public boolean numericEqual(double aX, double aY, double aZ) {
        return MathEX.Code.numericEqual(mX, aX) && MathEX.Code.numericEqual(mY, aY) && MathEX.Code.numericEqual(mZ, aZ);
    }
}
