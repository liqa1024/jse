package jse.atom;

import jse.atom.data.DataXYZ;
import jse.code.collection.NewCollections;
import jse.math.MathEX;
import jse.math.vector.IVector;
import jse.math.vector.Vector;

import java.util.ArrayList;
import java.util.List;

/**
 * 通用的 xyz 三维坐标接口，通过 {@link #x()}, {@link #y()},
 * {@link #z()} 来获取具体坐标值，并提供许多常见的运算。
 * <p>
 * 注意区分 xyz 数据格式 {@link DataXYZ}
 *
 * @see XYZ XYZ: 三维坐标的一般实现
 * @see ISettableXYZ ISettableXYZ: 可以修改的 xyz 三维坐标接口
 * @see DataXYZ DataXYZ: xyz 数据格式
 * @author liqa
 */
public interface IXYZ {
    /** @return 此三维坐标的 x 值 */
    double x();
    /** @return 此三维坐标的 y 值 */
    double y();
    /** @return 此三维坐标的 z 值 */
    double z();
    /** @return 自身的拷贝 */
    default IXYZ copy() {return new XYZ(this);}
    
    /** @return 兼容性更高的 double[] 类型，会进行一次值拷贝 */
    default double[] data() {return new double[] {x(), y(), z()};}
    /**
     * 转换为 {@link Vector} 类型，由于 xyz 本身很小，这里只提供会进行一次值拷贝的 {@code to} 接口
     * @return 存储 xyz 的 {@link Vector} 数据
     * @see Vector
     * @see IVector
     */
    default Vector toVec() {return new Vector(data());}
    /**
     * 转为 {@link List}，由于 xyz 本身很小，这里只提供会进行一次值拷贝的 {@code to} 接口
     * @return 存储 xyz 的 {@link List} 数据
     * @see List
     */
    default ArrayList<Double> toList() {return NewCollections.from(data());}
    
    
    /// 提供一些运算；使用重载而不是 instanceof，即只优化可以在编译期间判断的情况
    /** @return {@code x() + y() + z()} */
    default double sum() {return x() + y() + z();}
    /** @return {@code (x() + y() + z())/3} */
    default double mean() {return (x() + y() + z())/3.0;}
    /** @return {@code x() * y() * z()} */
    default double prod() {return x() * y() * z();}
    /** @return {@code min(min(x(), y()), z())} */
    default double min() {return Math.min(Math.min(x(), y()), z());}
    /** @return {@code max(max(x(), y()), z())} */
    default double max() {return Math.max(Math.max(x(), y()), z());}
    
    /** @return {@code x()*x() + y()*y() + z()*z()} */
    default double dot() {
        double tX = x();
        double tY = y();
        double tZ = z();
        return tX*tX + tY*tY + tZ*tZ;
    }
    /** @return {@code x()*aRHS.x() + y()*aRHS.y() + z()*aRHS.z()} */
    default double dot(IXYZ aRHS) {return dot(aRHS.x(), aRHS.y(), aRHS.z());}
    /** @return {@code x()*aRHS.x() + y()*aRHS.y() + z()*aRHS.z()} */
    default double dot(XYZ aRHS) {return dot(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** @return {@code x()*aX + y()*aY + z()*aZ} */
    default double dot(double aX, double aY, double aZ) {return x()*aX + y()*aY + z()*aZ;}
    
    /** @return {@code new XYZ(y()*aRHS.z() - aRHS.y()*z(), z()*aRHS.x() - aRHS.z()*x(), x()*aRHS.y() - aRHS.x()*y())} */
    default XYZ cross(IXYZ aRHS) {return cross(aRHS.x(), aRHS.y(), aRHS.z());}
    /** @return {@code new XYZ(y()*aRHS.z() - aRHS.y()*z(), z()*aRHS.x() - aRHS.z()*x(), x()*aRHS.y() - aRHS.x()*y())} */
    default XYZ cross(XYZ aRHS) {return cross(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** @return {@code new XYZ(y()*aZ - aY*z(), z()*aX - aZ*x(), x()*aY - aX*y())} */
    default XYZ cross(double aX, double aY, double aZ) {
        double tX = x();
        double tY = y();
        double tZ = z();
        return new XYZ(tY*aZ - aY*tZ, tZ*aX - aZ*tX, tX*aY - aX*tY);
    }
    
    /** @return {@code cross(aCross).dot(aDot)} */
    default double mixed(IXYZ aCross, IXYZ aDot) {return mixed(aCross.x(), aCross.y(), aCross.z(), aDot.x(), aDot.y(), aDot.z());}
    /** @return {@code cross(aCross).dot(aDot)} */
    default double mixed(XYZ aCross, XYZ aDot) {return mixed(aCross.mX, aCross.mY, aCross.mZ, aDot.mX, aDot.mY, aDot.mZ);}
    /** @return {@code cross(aCX, aCY, aCZ).dot(aDX, aDY, aDZ)} */
    default double mixed(double aCX, double aCY, double aCZ, double aDX, double aDY, double aDZ) {
        double tX = x();
        double tY = y();
        double tZ = z();
        return (tY*aCZ - aCY*tZ)*aDX + (tZ*aCX - aCZ*tX)*aDY + (tX*aCY - aCX*tY)*aDZ;
    }
    
    /** @return {@code new XYZ(-x(), -y(), -z())} */
    default XYZ negative() {return new XYZ(-x(), -y(), -z());}
    /**
     * @return {@code new XYZ(abs(x()), abs(y()), abs(z()))}
     * @see Math#abs(double)
     */
    default XYZ abs() {return new XYZ(Math.abs(x()), Math.abs(y()), Math.abs(z()));}
    /**
     * @return {@code sqrt(x()*x() + y()*y() + z()*z())}
     * @see Math#sqrt(double)
     */
    default double norm() {return MathEX.Fast.hypot(x(), y(), z());}
    /**
     * @return {@code abs(x()) + abs(y()) + abs(z())}
     * @see Math#abs(double)
     */
    default double norm1() {return Math.abs(x()) + Math.abs(y()) + Math.abs(z());}
    
    /// 使用和 Groovy 重载运算符相同的名称，可以顺便实现重载运算符操作
    /** @return {@code new XYZ(x()+aRHS.x(), y()+aRHS.y(), z()+aRHS.z())} */
    default XYZ plus(IXYZ aRHS) {return plus(aRHS.x(), aRHS.y(), aRHS.z());}
    /** @return {@code new XYZ(x()+aRHS.x(), y()+aRHS.y(), z()+aRHS.z())} */
    default XYZ plus(XYZ aRHS) {return plus(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** @return {@code new XYZ(x()+aX, y()+aY, z()+aZ)} */
    default XYZ plus(double aX, double aY, double aZ) {return new XYZ(x()+aX, y()+aY, z()+aZ);}
    /** @return {@code new XYZ(x()+aRHS, y()+aRHS, z()+aRHS)} */
    default XYZ plus(double aRHS) {return new XYZ(x()+aRHS, y()+aRHS, z()+aRHS);}
    
    /** @return {@code new XYZ(x()-aRHS.x(), y()-aRHS.y(), z()-aRHS.z())} */
    default XYZ minus(IXYZ aRHS) {return minus(aRHS.x(), aRHS.y(), aRHS.z());}
    /** @return {@code new XYZ(x()-aRHS.x(), y()-aRHS.y(), z()-aRHS.z())} */
    default XYZ minus(XYZ aRHS) {return minus(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** @return {@code new XYZ(x()-aX, y()-aY, z()-aZ)} */
    default XYZ minus(double aX, double aY, double aZ) {return new XYZ(x()-aX, y()-aY, z()-aZ);}
    /** @return {@code new XYZ(x()-aRHS, y()-aRHS, z()-aRHS)} */
    default XYZ minus(double aRHS) {return new XYZ(x()-aRHS, y()-aRHS, z()-aRHS);}
    
    /** @return {@code new XYZ(aRHS.x()-x(), aRHS.y()-y(), aRHS.z()-z())} */
    default XYZ lminus(IXYZ aRHS) {return lminus(aRHS.x(), aRHS.y(), aRHS.z());}
    /** @return {@code new XYZ(aRHS.x()-x(), aRHS.y()-y(), aRHS.z()-z())} */
    default XYZ lminus(XYZ aRHS) {return lminus(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** @return {@code new XYZ(aX-x(), aY-y(), aZ-z())} */
    default XYZ lminus(double aX, double aY, double aZ) {return new XYZ(aX-x(), aY-y(), aZ-z());}
    /** @return {@code new XYZ(aRHS-x(), aRHS-y(), aRHS-z())} */
    default XYZ lminus(double aRHS) {return new XYZ(aRHS-x(), aRHS-y(), aRHS-z());}
    
    /** @return {@code new XYZ(x()*aRHS.x(), y()*aRHS.y(), z()*aRHS.z())} */
    default XYZ multiply(IXYZ aRHS) {return multiply(aRHS.x(), aRHS.y(), aRHS.z());}
    /** @return {@code new XYZ(x()*aRHS.x(), y()*aRHS.y(), z()*aRHS.z())} */
    default XYZ multiply(XYZ aRHS) {return multiply(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** @return {@code new XYZ(x()*aX, y()*aY, z()*aZ)} */
    default XYZ multiply(double aX, double aY, double aZ) {return new XYZ(x()*aX, y()*aY, z()*aZ);}
    /** @return {@code new XYZ(x()*aRHS, y()*aRHS, z()*aRHS)} */
    default XYZ multiply(double aRHS) {return new XYZ(x()*aRHS, y()*aRHS, z()*aRHS);}
    
    /** @return {@code new XYZ(x()/aRHS.x(), y()/aRHS.y(), z()/aRHS.z())} */
    default XYZ div(IXYZ aRHS) {return div(aRHS.x(), aRHS.y(), aRHS.z());}
    /** @return {@code new XYZ(x()/aRHS.x(), y()/aRHS.y(), z()/aRHS.z())} */
    default XYZ div(XYZ aRHS) {return div(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** @return {@code new XYZ(x()/aX, y()/aY, z()/aZ)} */
    default XYZ div(double aX, double aY, double aZ) {return new XYZ(x()/aX, y()/aY, z()/aZ);}
    /** @return {@code new XYZ(x()/aRHS, y()/aRHS, z()/aRHS)} */
    default XYZ div(double aRHS) {return new XYZ(x()/aRHS, y()/aRHS, z()/aRHS);}
    
    /** @return {@code new XYZ(aRHS.x()/x(), aRHS.y()/y(), aRHS.z()/z())} */
    default XYZ ldiv(IXYZ aRHS) {return ldiv(aRHS.x(), aRHS.y(), aRHS.z());}
    /** @return {@code new XYZ(aRHS.x()/x(), aRHS.y()/y(), aRHS.z()/z())} */
    default XYZ ldiv(XYZ aRHS) {return ldiv(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** @return {@code new XYZ(aX/x(), aY/y(), aZ/z())} */
    default XYZ ldiv(double aX, double aY, double aZ) {return new XYZ(aX/x(), aY/y(), aZ/z());}
    /** @return {@code new XYZ(aRHS/x(), aRHS/y(), aRHS/z())} */
    default XYZ ldiv(double aRHS) {return new XYZ(aRHS/x(), aRHS/y(), aRHS/z());}
    
    
    /**
     * @return {@code this.minus(aRHS).dot()}
     * @see #dot()
     * @see #minus(IXYZ)
     */
    default double distance2(IXYZ aRHS) {return distance2(aRHS.x(), aRHS.y(), aRHS.z());}
    /**
     * @return {@code this.minus(aRHS).dot()}
     * @see #dot()
     * @see #minus(XYZ)
     */
    default double distance2(XYZ aRHS) {return distance2(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /**
     * @return {@code this.minus(aX, aY, aZ).dot()}
     * @see #dot()
     * @see #minus(double, double, double)
     */
    default double distance2(double aX, double aY, double aZ) {
        aX -= x();
        aY -= y();
        aZ -= z();
        return aX*aX + aY*aY + aZ*aZ;
    }
    
    /**
     * @return {@code sqrt(this.distance2(aRHS))}
     * @see #distance2(IXYZ)
     * @see Math#sqrt(double)
     */
    default double distance(IXYZ aRHS) {return distance(aRHS.x(), aRHS.y(), aRHS.z());}
    /**
     * @return {@code sqrt(this.distance2(aRHS))}
     * @see #distance2(XYZ)
     * @see Math#sqrt(double)
     */
    default double distance(XYZ aRHS) {return distance(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /**
     * @return {@code sqrt(this.distance2(aX, aY, aZ))}
     * @see #distance2(double, double, double)
     * @see Math#sqrt(double)
     */
    default double distance(double aX, double aY, double aZ) {return MathEX.Fast.hypot(x()-aX, y()-aY, z()-aZ);}
    /**
     * @return {@code sqrtQuick(this.distance2(aRHS))}
     * @see #distance2(IXYZ)
     * @see MathEX.Fast#sqrtQuick(double)
     */
    default double distanceQuick(IXYZ aRHS) {return MathEX.Fast.sqrtQuick(distance2(aRHS));}
    /**
     * @return {@code sqrtQuick(this.distance2(aRHS))}
     * @see #distance2(XYZ)
     * @see MathEX.Fast#sqrtQuick(double)
     */
    default double distanceQuick(XYZ aRHS) {return MathEX.Fast.sqrtQuick(distance2(aRHS));}
    /**
     * @return {@code sqrtQuick(this.distance2(aX, aY, aZ))}
     * @see #distance2(double, double, double)
     * @see MathEX.Fast#sqrtQuick(double)
     */
    default double distanceQuick(double aX, double aY, double aZ) {return MathEX.Fast.sqrtQuick(distance2(aX, aY, aZ));}
    
    /**
     * MHT: ManHaTtan distance, 曼哈顿距离
     * @return {@code this.minus(aRHS).abs().sum()}
     * @see #abs()
     * @see #sum()
     * @see #minus(IXYZ)
     */
    default double distanceMHT(IXYZ aRHS) {return distanceMHT(aRHS.x(), aRHS.y(), aRHS.z());}
    /**
     * MHT: ManHaTtan distance, 曼哈顿距离
     * @return {@code this.minus(aRHS).abs().sum()}
     * @see #abs()
     * @see #sum()
     * @see #minus(XYZ)
     */
    default double distanceMHT(XYZ aRHS) {return distanceMHT(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /**
     * MHT: ManHaTtan distance, 曼哈顿距离
     * @return {@code this.minus(aX, aY, aZ).abs().sum()}
     * @see #abs()
     * @see #sum()
     * @see #minus(double, double, double)
     */
    default double distanceMHT(double aX, double aY, double aZ) {return Math.abs(x()-aX) + Math.abs(y()-aY) + Math.abs(z()-aZ);}
    
    /**
     * @return {@code numericEqual(x(), aRHS.x()) && numericEqual(y(), aRHS.y()) && numericEqual(z(), aRHS.z())}
     * @see MathEX.Code#numericEqual(double, double)
     */
    default boolean numericEqual(IXYZ aRHS) {return numericEqual(aRHS.x(), aRHS.y(), aRHS.z());}
    /**
     * @return {@code numericEqual(x(), aRHS.x()) && numericEqual(y(), aRHS.y()) && numericEqual(z(), aRHS.z())}
     * @see MathEX.Code#numericEqual(double, double)
     */
    default boolean numericEqual(XYZ aRHS) {return numericEqual(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /**
     * @return {@code numericEqual(x(), aX) && numericEqual(y(), aY) && numericEqual(z(), aZ)}
     * @see MathEX.Code#numericEqual(double, double)
     */
    default boolean numericEqual(double aX, double aY, double aZ) {
        return MathEX.Code.numericEqual(x(), aX) && MathEX.Code.numericEqual(y(), aY) && MathEX.Code.numericEqual(z(), aZ);
    }
}
