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
    
    @Override public XYZ plus(IXYZ aRHS) {return new XYZ(mX+aRHS.x(), mY+aRHS.y(), mZ+aRHS.z());}
    @Override public XYZ plus(XYZ aRHS) {return new XYZ(mX+aRHS.mX, mY+aRHS.mY, mZ+aRHS.mZ);}
    @Override public XYZ plus(double aX, double aY, double aZ) {return new XYZ(mX+aX, mY+aY, mZ+aZ);}
    @Override public XYZ plus(double aRHS) {return new XYZ(mX+aRHS, mY+aRHS, mZ+aRHS);}
    @Override public void plus2this(IXYZ aRHS) {mX += aRHS.x(); mY += aRHS.y(); mZ += aRHS.z();}
    @Override public void plus2this(XYZ aRHS) {mX += aRHS.mX; mY += aRHS.mY; mZ += aRHS.mZ;}
    @Override public void plus2this(double aX, double aY, double aZ) {mX += aX; mY += aY; mZ += aZ;}
    @Override public void plus2this(double aRHS) {mX += aRHS; mY += aRHS; mZ += aRHS;}
    /** 也增加这个运算方便使用 */
    @Override public void mplus2this(IXYZ aRHS, double aMul) {mX += aMul*aRHS.x(); mY += aMul*aRHS.y(); mZ += aMul*aRHS.z();}
    @Override public void mplus2this(XYZ aRHS, double aMul) {mX += aMul*aRHS.mX; mY += aMul*aRHS.mY; mZ += aMul*aRHS.mZ;}
    
    @Override public XYZ minus(IXYZ aRHS) {return new XYZ(mX-aRHS.x(), mY-aRHS.y(), mZ-aRHS.z());}
    @Override public XYZ minus(XYZ aRHS) {return new XYZ(mX-aRHS.mX, mY-aRHS.mY, mZ-aRHS.mZ);}
    @Override public XYZ minus(double aX, double aY, double aZ) {return new XYZ(mX-aX, mY-aY, mZ-aZ);}
    @Override public XYZ minus(double aRHS) {return new XYZ(mX-aRHS, mY-aRHS, mZ-aRHS);}
    @Override public void minus2this(IXYZ aRHS) {mX -= aRHS.x(); mY -= aRHS.y(); mZ -= aRHS.z();}
    @Override public void minus2this(XYZ aRHS) {mX -= aRHS.mX; mY -= aRHS.mY; mZ -= aRHS.mZ;}
    @Override public void minus2this(double aX, double aY, double aZ) {mX -= aX; mY -= aY; mZ -= aZ;}
    @Override public void minus2this(double aRHS) {mX -= aRHS; mY -= aRHS; mZ -= aRHS;}
    
    @Override public XYZ lminus(IXYZ aRHS) {return new XYZ(aRHS.x()-mX, aRHS.y()-mY, aRHS.z()-mZ);}
    @Override public XYZ lminus(XYZ aRHS) {return new XYZ(aRHS.mX-mX, aRHS.mY-mY, aRHS.mZ-mZ);}
    @Override public XYZ lminus(double aX, double aY, double aZ) {return new XYZ(aX-mX, aY-mY, aZ-mZ);}
    @Override public XYZ lminus(double aRHS) {return new XYZ(aRHS-mX, aRHS-mY, aRHS-mZ);}
    @Override public void lminus2this(IXYZ aRHS) {mX = aRHS.x()-mX; mY = aRHS.y()-mY; mZ = aRHS.z()-mZ;}
    @Override public void lminus2this(XYZ aRHS) {mX = aRHS.mX-mX; mY = aRHS.mY-mY; mZ = aRHS.mZ-mZ;}
    @Override public void lminus2this(double aX, double aY, double aZ) {mX = aX-mX; mY = aY-mY; mZ = aZ-mZ;}
    @Override public void lminus2this(double aRHS) {mX = aRHS-mX; mY = aRHS-mY; mZ = aRHS-mZ;}
    
    @Override public XYZ multiply(IXYZ aRHS) {return new XYZ(mX*aRHS.x(), mY*aRHS.y(), mZ*aRHS.z());}
    @Override public XYZ multiply(XYZ aRHS) {return new XYZ(mX*aRHS.mX, mY*aRHS.mY, mZ*aRHS.mZ);}
    @Override public XYZ multiply(double aX, double aY, double aZ) {return new XYZ(mX*aX, mY*aY, mZ*aZ);}
    @Override public XYZ multiply(double aRHS) {return new XYZ(mX*aRHS, mY*aRHS, mZ*aRHS);}
    @Override public void multiply2this(IXYZ aRHS) {mX *= aRHS.x(); mY *= aRHS.y(); mZ *= aRHS.z();}
    @Override public void multiply2this(XYZ aRHS) {mX *= aRHS.mX; mY *= aRHS.mY; mZ *= aRHS.mZ;}
    @Override public void multiply2this(double aX, double aY, double aZ) {mX *= aX; mY *= aY; mZ *= aZ;}
    @Override public void multiply2this(double aRHS) {mX *= aRHS; mY *= aRHS; mZ *= aRHS;}
    
    @Override public XYZ div(IXYZ aRHS) {return new XYZ(mX/aRHS.x(), mY/aRHS.y(), mZ/aRHS.z());}
    @Override public XYZ div(XYZ aRHS) {return new XYZ(mX/aRHS.mX, mY/aRHS.mY, mZ/aRHS.mZ);}
    @Override public XYZ div(double aX, double aY, double aZ) {return new XYZ(mX/aX, mY/aY, mZ/aZ);}
    @Override public XYZ div(double aRHS) {return new XYZ(mX/aRHS, mY/aRHS, mZ/aRHS);}
    @Override public void div2this(IXYZ aRHS) {mX /= aRHS.x(); mY /= aRHS.y(); mZ /= aRHS.z();}
    @Override public void div2this(XYZ aRHS) {mX /= aRHS.mX; mY /= aRHS.mY; mZ /= aRHS.mZ;}
    @Override public void div2this(double aX, double aY, double aZ) {mX /= aX; mY /= aY; mZ /= aZ;}
    @Override public void div2this(double aRHS) {mX /= aRHS; mY /= aRHS; mZ /= aRHS;}
    
    @Override public XYZ ldiv(IXYZ aRHS) {return new XYZ(aRHS.x()/mX, aRHS.y()/mY, aRHS.z()/mZ);}
    @Override public XYZ ldiv(XYZ aRHS) {return new XYZ(aRHS.mX/mX, aRHS.mY/mY, aRHS.mZ/mZ);}
    @Override public XYZ ldiv(double aX, double aY, double aZ) {return new XYZ(aX/mX, aY/mY, aZ/mZ);}
    @Override public XYZ ldiv(double aRHS) {return new XYZ(aRHS/mX, aRHS/mY, aRHS/mZ);}
    @Override public void ldiv2this(IXYZ aRHS) {mX = aRHS.x()/mX; mY = aRHS.y()/mY; mZ = aRHS.z()/mZ;}
    @Override public void ldiv2this(XYZ aRHS) {mX = aRHS.mX/mX; mY = aRHS.mY/mY; mZ = aRHS.mZ/mZ;}
    @Override public void ldiv2this(double aX, double aY, double aZ) {mX = aX/mX; mY = aY/mY; mZ = aZ/mZ;}
    @Override public void ldiv2this(double aRHS) {mX = aRHS/mX; mY = aRHS/mY; mZ = aRHS/mZ;}
    
    
    @Override public double distance2(IXYZ aRHS) {
        double tX = mX - aRHS.x();
        double tY = mY - aRHS.y();
        double tZ = mZ - aRHS.z();
        return tX*tX + tY*tY + tZ*tZ;
    }
    @Override public double distance2(XYZ aRHS) {
        double tX = mX - aRHS.mX;
        double tY = mY - aRHS.mY;
        double tZ = mZ - aRHS.mZ;
        return tX*tX + tY*tY + tZ*tZ;
    }
    @Override public double distance2(double aX, double aY, double aZ) {
        aX -= mX;
        aY -= mY;
        aZ -= mZ;
        return aX*aX + aY*aY + aZ*aZ;
    }
    @Override public double distance(IXYZ aRHS) {return MathEX.Fast.hypot(mX-aRHS.x(), mY-aRHS.y(), mZ-aRHS.z());}
    @Override public double distance(XYZ aRHS) {return MathEX.Fast.hypot(mX-aRHS.mX, mY-aRHS.mY, mZ-aRHS.mZ);}
    @Override public double distance(double aX, double aY, double aZ) {return MathEX.Fast.hypot(mX-aX, mY-aY, mZ-aZ);}
    
    @Override public double distanceMHT(IXYZ aRHS) {return Math.abs(mX-aRHS.x()) + Math.abs(mY-aRHS.y()) + Math.abs(mZ-aRHS.z());}
    @Override public double distanceMHT(XYZ aRHS) {return Math.abs(mX-aRHS.mX) + Math.abs(mY-aRHS.mY) + Math.abs(mZ-aRHS.mZ);}
    @Override public double distanceMHT(double aX, double aY, double aZ) {
        aX -= mX;
        aY -= mY;
        aZ -= mZ;
        return Math.abs(aX) + Math.abs(aY) + Math.abs(aZ);
    }
}
