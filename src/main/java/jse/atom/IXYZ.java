package jse.atom;

import jse.code.collection.NewCollections;
import jse.math.MathEX;
import jse.math.vector.Vector;

import java.util.ArrayList;

/**
 * 通用的 XYZ 坐标接口，使用此接口还有一重含义时此 XYZ 是不建议修改的
 * <p>
 * 现在运算统一返回 {@link XYZ} 方便使用，并且也暗示这些方法会返回新的对象
 * @author liqa
 */
public interface IXYZ {
    double x();
    double y();
    double z();
    default IXYZ copy() {return new XYZ(this);}
    
    /** 转为兼容性更高的 double[] */
    default double[] data() {return new double[] {x(), y(), z()};}
    /** 转为 vector 和 list，由于 xyz 本身很小，这里直接进行值拷贝转换，不再提供 as 方法 */
    default Vector toVec() {return new Vector(data());}
    default ArrayList<Double> toList() {return NewCollections.from(data());}
    
    /** 提供一些运算，由于 XYZ 本身就很轻量，为了避免方法调用的损失，并且让实现起来比较简单，这里不增加中间层 operation */
    default double prod() {return x() * y() * z();}
    default double min() {return Math.min(Math.min(x(), y()), z());}
    default double max() {return Math.max(Math.max(x(), y()), z());}
    
    default double dot() {
        double tX = x();
        double tY = y();
        double tZ = z();
        return tX*tX + tY*tY + tZ*tZ;
    }
    default double dot(IXYZ aRHS) {return dot(aRHS.x(), aRHS.y(), aRHS.z());}
    default double dot(XYZ aRHS) {return dot(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default double dot(double aX, double aY, double aZ) {return x()*aX + y()*aY + z()*aZ;}
    
    default XYZ cross(IXYZ aRHS) {return cross(aRHS.x(), aRHS.y(), aRHS.z());}
    default XYZ cross(XYZ aRHS) {return cross(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default XYZ cross(double aX, double aY, double aZ) {
        double tX = x();
        double tY = y();
        double tZ = z();
        return new XYZ(tY*aZ - aY*tZ, tZ*aX - aZ*tX, tX*aY - aX*tY);
    }
    
    default double mixed(IXYZ aCross, IXYZ aDot) {return mixed(aCross.x(), aCross.y(), aCross.z(), aDot.x(), aDot.y(), aDot.z());}
    default double mixed(XYZ aCross, XYZ aDot) {return mixed(aCross.mX, aCross.mY, aCross.mZ, aDot.mX, aDot.mY, aDot.mZ);}
    default double mixed(double aCX, double aCY, double aCZ, double aDX, double aDY, double aDZ) {
        double tX = x();
        double tY = y();
        double tZ = z();
        return (tY*aCZ - aCY*tZ)*aDX + (tZ*aCX - aCZ*tX)*aDY + (tX*aCY - aCX*tY)*aDZ;
    }
    
    default XYZ negative() {return new XYZ(-x(), -y(), -z());}
    default double norm() {return MathEX.Fast.hypot(x(), y(), z());}
    
    /** 使用和 Groovy 重载运算符相同的名称，可以顺便实现重载运算符操作 */
    default XYZ plus(IXYZ aRHS) {return new XYZ(x()+aRHS.x(), y()+aRHS.y(), z()+aRHS.z());}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default XYZ plus(XYZ aRHS) {return new XYZ(x()+aRHS.mX, y()+aRHS.mY, z()+aRHS.mZ);}
    default XYZ plus(double aX, double aY, double aZ) {return new XYZ(x()+aX, y()+aY, z()+aZ);}
    default XYZ plus(double aRHS) {return new XYZ(x()+aRHS, y()+aRHS, z()+aRHS);}
    
    default XYZ minus(IXYZ aRHS) {return new XYZ(x()-aRHS.x(), y()-aRHS.y(), z()-aRHS.z());}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default XYZ minus(XYZ aRHS) {return new XYZ(x()-aRHS.mX, y()-aRHS.mY, z()-aRHS.mZ);}
    default XYZ minus(double aX, double aY, double aZ) {return new XYZ(x()-aX, y()-aY, z()-aZ);}
    default XYZ minus(double aRHS) {return new XYZ(x()-aRHS, y()-aRHS, z()-aRHS);}
    
    default XYZ lminus(IXYZ aRHS) {return new XYZ(aRHS.x()-x(), aRHS.y()-y(), aRHS.z()-z());}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default XYZ lminus(XYZ aRHS) {return new XYZ(aRHS.mX-x(), aRHS.mY-y(), aRHS.mZ-z());}
    default XYZ lminus(double aX, double aY, double aZ) {return new XYZ(aX-x(), aY-y(), aZ-z());}
    default XYZ lminus(double aRHS) {return new XYZ(aRHS-x(), aRHS-y(), aRHS-z());}
    
    default XYZ multiply(IXYZ aRHS) {return new XYZ(x()*aRHS.x(), y()*aRHS.y(), z()*aRHS.z());}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default XYZ multiply(XYZ aRHS) {return new XYZ(x()*aRHS.mX, y()*aRHS.mY, z()*aRHS.mZ);}
    default XYZ multiply(double aX, double aY, double aZ) {return new XYZ(x()*aX, y()*aY, z()*aZ);}
    default XYZ multiply(double aRHS) {return new XYZ(x()*aRHS, y()*aRHS, z()*aRHS);}
    
    default XYZ div(IXYZ aRHS) {return new XYZ(x()/aRHS.x(), y()/aRHS.y(), z()/aRHS.z());}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default XYZ div(XYZ aRHS) {return new XYZ(x()/aRHS.mX, y()/aRHS.mY, z()/aRHS.mZ);}
    default XYZ div(double aX, double aY, double aZ) {return new XYZ(x()/aX, y()/aY, z()/aZ);}
    default XYZ div(double aRHS) {return new XYZ(x()/aRHS, y()/aRHS, z()/aRHS);}
    
    default XYZ ldiv(IXYZ aRHS) {return new XYZ(aRHS.x()/x(), aRHS.y()/y(), aRHS.z()/z());}
    /** 使用重载而不是 instanceof，即只优化可以在编译期间判断的情况 */
    default XYZ ldiv(XYZ aRHS) {return new XYZ(aRHS.mX/x(), aRHS.mY/y(), aRHS.mZ/z());}
    default XYZ ldiv(double aX, double aY, double aZ) {return new XYZ(aX/x(), aY/y(), aZ/z());}
    default XYZ ldiv(double aRHS) {return new XYZ(aRHS/x(), aRHS/y(), aRHS/z());}
    
    
    
    default double distance2(IXYZ aRHS) {
        double tX = x() - aRHS.x();
        double tY = y() - aRHS.y();
        double tZ = z() - aRHS.z();
        return tX*tX + tY*tY + tZ*tZ;
    }
    default double distance2(XYZ aRHS) {
        double tX = x() - aRHS.mX;
        double tY = y() - aRHS.mY;
        double tZ = z() - aRHS.mZ;
        return tX*tX + tY*tY + tZ*tZ;
    }
    default double distance2(double aX, double aY, double aZ) {
        aX -= x();
        aY -= y();
        aZ -= z();
        return aX*aX + aY*aY + aZ*aZ;
    }
    
    default double distance(IXYZ aRHS) {return MathEX.Fast.hypot(x()-aRHS.x(), y()-aRHS.y(), z()-aRHS.z());}
    default double distance(XYZ aRHS) {return MathEX.Fast.hypot(x()-aRHS.mX, y()-aRHS.mY, z()-aRHS.mZ);}
    default double distance(double aX, double aY, double aZ) {return MathEX.Fast.hypot(x()-aX, y()-aY, z()-aZ);}
    default double distanceQuick(IXYZ aRHS) {return MathEX.Fast.sqrtQuick(distance2(aRHS));}
    default double distanceQuick(XYZ aRHS) {return MathEX.Fast.sqrtQuick(distance2(aRHS));}
    default double distanceQuick(double aX, double aY, double aZ) {return MathEX.Fast.sqrtQuick(distance2(aX, aY, aZ));}
    
    /**
     * MHT: ManHaTtan distance
     * 曼哈顿距离
     */
    default double distanceMHT(IXYZ aRHS) {return Math.abs(x()-aRHS.x()) + Math.abs(y()-aRHS.y()) + Math.abs(z()-aRHS.z());}
    default double distanceMHT(XYZ aRHS) {return Math.abs(x()-aRHS.mX) + Math.abs(y()-aRHS.mY) + Math.abs(z()-aRHS.mZ);}
    default double distanceMHT(double aX, double aY, double aZ) {return Math.abs(x()-aX) + Math.abs(y()-aY) + Math.abs(z()-aZ);}
    
    default boolean numericEqual(IXYZ aRHS) {return numericEqual(aRHS.x(), aRHS.y(), aRHS.z());}
    default boolean numericEqual(XYZ aRHS) {return numericEqual(aRHS.mX, aRHS.mY, aRHS.mZ);}
    default boolean numericEqual(double aX, double aY, double aZ) {
        return MathEX.Code.numericEqual(x(), aX) && MathEX.Code.numericEqual(y(), aY) && MathEX.Code.numericEqual(z(), aZ);
    }
}
