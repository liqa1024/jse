package jse.atom;

import jse.atom.data.DataXYZ;
import jse.math.MathEX;
import jse.math.vector.IVector;

/**
 * 一般的 xyz 三维坐标实现
 * <p>
 * 直接通过：
 * <pre> {@code
 * def xyz = new XYZ(x, y, z)
 * } </pre>
 * 来创建一个三维坐标点
 * <p>
 * 除了通用的 {@link #x()}, {@link #y()}, {@link #z()}
 * 方法来获取属性，还可以通过直接访问成员变量 {@link #mX},
 * {@link #mY}, {@link #mZ} 来获取以及修改。
 * <p>
 * 注意区分 xyz 数据格式 {@link DataXYZ}
 *
 * @see IXYZ IXYZ: 通用的 xyz 坐标
 * @see ISettableXYZ ISettableXYZ: 可修改坐标值的 xyz 坐标
 * @see DataXYZ DataXYZ: xyz 数据格式
 * @author liqa
 */
public final class XYZ extends AbstractSettableXYZ {
    /**
     * 直接转换一个通用的 {@link IXYZ} 成为 {@link XYZ}
     * 来保证一些优化能进行，如果检测到类型相同直接做类型转换，
     * 否则创建新的对象，因此结果原则上应当是只读的
     * @param aXYZ 需要转换的 xyz 对象
     * @return 转换后的 {@link XYZ} 对象
     */
    public static XYZ toXYZ(IXYZ aXYZ) {
        return (aXYZ instanceof XYZ) ? (XYZ)aXYZ : new XYZ(aXYZ);
    }
    
    public double mX, mY, mZ;
    /**
     * 创建一个默认的 xyz 对象，即 {@code new XYZ(0.0, 0.0, 0.0)}
     */
    public XYZ() {this(0.0, 0.0, 0.0);}
    /**
     * 创建一个 xyz 对象
     * @param aX x 坐标值
     * @param aY y 坐标值
     * @param aZ z 坐标值
     */
    public XYZ(double aX, double aY, double aZ) {
        mX = aX; mY = aY; mZ = aZ;
    }
    /**
     * 通过一个已有的 {@link IXYZ} 创建一个 xyz 对象
     * @param aXYZ 已有的任意的 xyz 坐标对象
     * @see IXYZ
     */
    public XYZ(IXYZ aXYZ) {
        mX = aXYZ.x(); mY = aXYZ.y(); mZ = aXYZ.z();
    }
    /**
     * 通过一个已有的 {@link XYZ} 创建一个 xyz 对象
     * @param aXYZ 已有的 xyz 坐标对象
     */
    public XYZ(XYZ aXYZ) {
        mX = aXYZ.mX; mY = aXYZ.mY; mZ = aXYZ.mZ;
    }
    /**
     * 通过一个已有的 {@link IVector} 创建一个 xyz 对象
     * @param aXYZ 已有的向量对象
     * @see IVector
     */
    public XYZ(IVector aXYZ) {
        mX = aXYZ.get(0); mY = aXYZ.get(1); mZ = aXYZ.get(2);
    }
    /** @return {@inheritDoc} */
    @Override public XYZ copy() {return new XYZ(this);}
    
    /** @return {@inheritDoc} */
    @Override public double x() {return mX;}
    /** @return {@inheritDoc} */
    @Override public double y() {return mY;}
    /** @return {@inheritDoc} */
    @Override public double z() {return mZ;}
    
    /**
     * {@inheritDoc}
     * @param aX {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public XYZ setX(double aX) {mX = aX; return this;}
    /**
     * {@inheritDoc}
     * @param aY {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public XYZ setY(double aY) {mY = aY; return this;}
    /**
     * {@inheritDoc}
     * @param aZ {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public XYZ setZ(double aZ) {mZ = aZ; return this;}
    /**
     * {@inheritDoc}
     * @param aX {@inheritDoc}
     * @param aY {@inheritDoc}
     * @param aZ {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public XYZ setXYZ(double aX, double aY, double aZ) {mX = aX; mY = aY; mZ = aZ; return this;}
    /**
     * {@inheritDoc}
     * @param aXYZ {@inheritDoc}
     * @return {@inheritDoc}
     * @see IXYZ
     */
    @Override public XYZ setXYZ(IXYZ aXYZ) {return setXYZ(aXYZ.x(), aXYZ.y(), aXYZ.z());}
    
    /** @return {@inheritDoc} */
    @Override public double sum() {return mX + mY + mZ;}
    /** @return {@inheritDoc} */
    @Override public double mean() {return (mX + mY + mZ)/3.0;}
    /** @return {@inheritDoc} */
    @Override public double prod() {return mX * mY * mZ;}
    /** @return {@inheritDoc} */
    @Override public double min() {return Math.min(Math.min(mX, mY), mZ);}
    /** @return {@inheritDoc} */
    @Override public double max() {return Math.max(Math.max(mX, mY), mZ);}
    
    /** @return {@inheritDoc} */
    @Override public double dot() {
        double tX = mX;
        double tY = mY;
        double tZ = mZ;
        return tX*tX + tY*tY + tZ*tZ;
    }
    /** @return {@inheritDoc} */
    @Override public double dot(double aX, double aY, double aZ) {return mX*aX + mY*aY + mZ*aZ;}
    
    /** @return {@inheritDoc} */
    @Override public XYZ cross(double aX, double aY, double aZ) {return new XYZ(mY*aZ - aY*mZ, mZ*aX - aZ*mX, mX*aY - aX*mY);}
    /** {@inheritDoc} */
    @Override public void cross2this(IXYZ aRHS) {cross2this(aRHS.x(), aRHS.y(), aRHS.z());}
    /** {@inheritDoc} */
    @Override public void cross2this(XYZ aRHS) {cross2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** {@inheritDoc} */
    @Override public void cross2this(double aX, double aY, double aZ) {
        double tX = mX;
        double tY = mY;
        double tZ = mZ;
        mX = tY*aZ - aY*tZ;
        mY = tZ*aX - aZ*tX;
        mZ = tX*aY - aX*tY;
    }
    
    /** @return {@inheritDoc} */
    @Override public double mixed(double aCX, double aCY, double aCZ, double aDX, double aDY, double aDZ) {return (mY*aCZ - aCY*mZ)*aDX + (mZ*aCX - aCZ*mX)*aDY + (mX*aCY - aCX*mY)*aDZ;}
    
    /** @return {@inheritDoc} */
    @Override public XYZ negative() {return new XYZ(-mX, -mY, -mZ);}
    /** {@inheritDoc} */
    @Override public void negative2this() {mX = -mX; mY = -mY; mZ = -mZ;}
    /** @return {@inheritDoc} */
    @Override public XYZ abs() {return new XYZ(Math.abs(mX), Math.abs(mY), Math.abs(mZ));}
    /** {@inheritDoc} */
    @Override public void abs2this()  {mX = Math.abs(mX); mY = Math.abs(mY); mZ = Math.abs(mZ);}
    /** @return {@inheritDoc} */
    @Override public double norm() {return MathEX.Fast.hypot(mX, mY, mZ);}
    /** @return {@inheritDoc} */
    @Override public double norm1() {return Math.abs(mX) + Math.abs(mY) + Math.abs(mZ);}
    
    /** @return {@inheritDoc} */
    @Override public XYZ plus(double aX, double aY, double aZ) {return new XYZ(mX+aX, mY+aY, mZ+aZ);}
    /** @return {@inheritDoc} */
    @Override public XYZ plus(double aRHS) {return new XYZ(mX+aRHS, mY+aRHS, mZ+aRHS);}
    /** {@inheritDoc} */
    @Override public void plus2this(double aX, double aY, double aZ) {mX += aX; mY += aY; mZ += aZ;}
    /** {@inheritDoc} */
    @Override public void plus2this(double aRHS) {mX += aRHS; mY += aRHS; mZ += aRHS;}
    /** {@inheritDoc} */
    @Override public void mplus2this(double aX, double aY, double aZ, double aMul) {mX += aMul*aX; mY += aMul*aY; mZ += aMul*aZ;}
    
    /** @return {@inheritDoc} */
    @Override public XYZ minus(double aX, double aY, double aZ) {return new XYZ(mX-aX, mY-aY, mZ-aZ);}
    /** @return {@inheritDoc} */
    @Override public XYZ minus(double aRHS) {return new XYZ(mX-aRHS, mY-aRHS, mZ-aRHS);}
    /** {@inheritDoc} */
    @Override public void minus2this(double aX, double aY, double aZ) {mX -= aX; mY -= aY; mZ -= aZ;}
    /** {@inheritDoc} */
    @Override public void minus2this(double aRHS) {mX -= aRHS; mY -= aRHS; mZ -= aRHS;}
    
    /** @return {@inheritDoc} */
    @Override public XYZ lminus(double aX, double aY, double aZ) {return new XYZ(aX-mX, aY-mY, aZ-mZ);}
    /** @return {@inheritDoc} */
    @Override public XYZ lminus(double aRHS) {return new XYZ(aRHS-mX, aRHS-mY, aRHS-mZ);}
    /** {@inheritDoc} */
    @Override public void lminus2this(double aX, double aY, double aZ) {mX = aX-mX; mY = aY-mY; mZ = aZ-mZ;}
    /** {@inheritDoc} */
    @Override public void lminus2this(double aRHS) {mX = aRHS-mX; mY = aRHS-mY; mZ = aRHS-mZ;}
    
    /** @return {@inheritDoc} */
    @Override public XYZ multiply(double aX, double aY, double aZ) {return new XYZ(mX*aX, mY*aY, mZ*aZ);}
    /** @return {@inheritDoc} */
    @Override public XYZ multiply(double aRHS) {return new XYZ(mX*aRHS, mY*aRHS, mZ*aRHS);}
    /** {@inheritDoc} */
    @Override public void multiply2this(double aX, double aY, double aZ) {mX *= aX; mY *= aY; mZ *= aZ;}
    /** {@inheritDoc} */
    @Override public void multiply2this(double aRHS) {mX *= aRHS; mY *= aRHS; mZ *= aRHS;}
    
    /** @return {@inheritDoc} */
    @Override public XYZ div(double aX, double aY, double aZ) {return new XYZ(mX/aX, mY/aY, mZ/aZ);}
    /** @return {@inheritDoc} */
    @Override public XYZ div(double aRHS) {return new XYZ(mX/aRHS, mY/aRHS, mZ/aRHS);}
    /** {@inheritDoc} */
    @Override public void div2this(double aX, double aY, double aZ) {mX /= aX; mY /= aY; mZ /= aZ;}
    /** {@inheritDoc} */
    @Override public void div2this(double aRHS) {mX /= aRHS; mY /= aRHS; mZ /= aRHS;}
    
    /** @return {@inheritDoc} */
    @Override public XYZ ldiv(double aX, double aY, double aZ) {return new XYZ(aX/mX, aY/mY, aZ/mZ);}
    /** @return {@inheritDoc} */
    @Override public XYZ ldiv(double aRHS) {return new XYZ(aRHS/mX, aRHS/mY, aRHS/mZ);}
    /** {@inheritDoc} */
    @Override public void ldiv2this(double aX, double aY, double aZ) {mX = aX/mX; mY = aY/mY; mZ = aZ/mZ;}
    /** {@inheritDoc} */
    @Override public void ldiv2this(double aRHS) {mX = aRHS/mX; mY = aRHS/mY; mZ = aRHS/mZ;}
    
    
    /** @return {@inheritDoc} */
    @Override public double distance2(double aX, double aY, double aZ) {
        aX -= mX;
        aY -= mY;
        aZ -= mZ;
        return aX*aX + aY*aY + aZ*aZ;
    }
    /** @return {@inheritDoc} */
    @Override public double distance(double aX, double aY, double aZ) {return MathEX.Fast.hypot(mX-aX, mY-aY, mZ-aZ);}
    /** @return {@inheritDoc} */
    @Override public double distanceMHT(double aX, double aY, double aZ) {return Math.abs(mX-aX) + Math.abs(mY-aY) + Math.abs(mZ-aZ);}
    
    /** @return {@inheritDoc} */
    @Override public boolean numericEqual(double aX, double aY, double aZ) {
        return MathEX.Code.numericEqual(mX, aX) && MathEX.Code.numericEqual(mY, aY) && MathEX.Code.numericEqual(mZ, aZ);
    }
}
