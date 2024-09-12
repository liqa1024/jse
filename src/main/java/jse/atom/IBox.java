package jse.atom;

import jse.math.MathEX;

/**
 * 通用的模拟盒类，现在包含更多信息，支持斜方的模拟盒；
 * 只支持右手系的基组
 * @author liqa
 */
public interface IBox extends IXYZ {
    /** 是否是斜方的，很多时候这只是一个标记，即使实际是正交的也可以获取到 {@code true} */
    default boolean isPrism() {return false;}
    /** 是否是 lammps 的风格，也就是 {@code ay = az = bz = 0}，当然很多时候这只是一个标记 */
    default boolean isLmpStyle() {return true;}
    
    /** 一般的接口，获取三个基，要求这三个基满足右手系，也就是 {@code (a x b) · c > 0} */
    default IXYZ a() {return new XYZ(ax(), ay(), az());}
    default IXYZ b() {return new XYZ(bx(), by(), bz());}
    default IXYZ c() {return new XYZ(cx(), cy(), cz());}
    /** 还需要提供一个复制的方法方便拷贝 */
    IBox copy();
    /** 提供直接获取基的某个元素的方法，现在这个为所有方法基础 */
    double ax();
    default double ay() {return 0.0;}
    default double az() {return 0.0;}
    default double bx() {return 0.0;}
    double by();
    default double bz() {return 0.0;}
    default double cx() {return 0.0;}
    default double cy() {return 0.0;}
    double cz();
    
    /** IXYZ stuffs */
    default double x() {return ax();}
    default double y() {return by();}
    default double z() {return cz();}
    
    /** lammps prism box stuffs */
    default double xy() {return bx();}
    default double xz() {return cx();}
    default double yz() {return cy();}
    
    default double volume() {
        if (isLmpStyle()) return prod();
        return a().mixed(b(), c());
    }
    
    
    default void wrapPBC(XYZ rXYZ) {
        toDirect(rXYZ);
        if (rXYZ.mX<0.0 || rXYZ.mX>=1.0) {rXYZ.mX -= MathEX.Code.floor(rXYZ.mX);}
        if (rXYZ.mY<0.0 || rXYZ.mY>=1.0) {rXYZ.mY -= MathEX.Code.floor(rXYZ.mY);}
        if (rXYZ.mZ<0.0 || rXYZ.mZ>=1.0) {rXYZ.mZ -= MathEX.Code.floor(rXYZ.mZ);}
        toCartesian(rXYZ);
    }
    /** 提供一个简单通用的坐标相互转换接口，这里不直接创建新的 XYZ 而是修改输入的 XYZ，可以保证性能，减少重复代码 */
    default void toCartesian(XYZ rDirect) {
        if (!isPrism()) {
            double tX = x(), tY = y(), tZ = z();
            rDirect.multiply2this(tX, tY, tZ);
            // cartesian 其实也需要考虑计算误差带来的出边界的问题（当然此时在另一端的就不好修复了）
            double tNorm = Math.abs(tX) + Math.abs(tY) + Math.abs(tZ);
            if (Math.abs(rDirect.mX) < MathEX.Code.DBL_EPSILON*tNorm) rDirect.mX = 0.0;
            if (Math.abs(rDirect.mY) < MathEX.Code.DBL_EPSILON*tNorm) rDirect.mY = 0.0;
            if (Math.abs(rDirect.mZ) < MathEX.Code.DBL_EPSILON*tNorm) rDirect.mZ = 0.0;
        } else
        if (isLmpStyle()) {
            double tX  =  x(), tY  =  y(), tZ  =  z();
            double tXY = xy(), tXZ = xz(), tYZ = yz();
            rDirect.setXYZ(
                tX*rDirect.mX + tXY*rDirect.mY + tXZ*rDirect.mZ,
                tY*rDirect.mY + tYZ*rDirect.mZ,
                tZ*rDirect.mZ
            );
            // cartesian 其实也需要考虑计算误差带来的出边界的问题（当然此时在另一端的就不好修复了）
            double tNorm = Math.abs(tX)  + Math.abs(tY)  + Math.abs(tZ)
                         + Math.abs(tXY) + Math.abs(tXZ) + Math.abs(tYZ);
            if (Math.abs(rDirect.mX) < MathEX.Code.DBL_EPSILON*tNorm) rDirect.mX = 0.0;
            if (Math.abs(rDirect.mY) < MathEX.Code.DBL_EPSILON*tNorm) rDirect.mY = 0.0;
            if (Math.abs(rDirect.mZ) < MathEX.Code.DBL_EPSILON*tNorm) rDirect.mZ = 0.0;
        } else {
            double tAx = ax(), tAy = ay(), tAz = az();
            double tBx = bx(), tBy = by(), tBz = bz();
            double tCx = cx(), tCy = cy(), tCz = cz();
            rDirect.setXYZ(
                tAx*rDirect.mX + tBx*rDirect.mY + tCx*rDirect.mZ,
                tAy*rDirect.mX + tBy*rDirect.mY + tCy*rDirect.mZ,
                tAz*rDirect.mX + tBz*rDirect.mY + tCz*rDirect.mZ
            );
            // cartesian 其实也需要考虑计算误差带来的出边界的问题（当然此时在另一端的就不好修复了）
            double tNorm = Math.abs(tAx) + Math.abs(tAy) + Math.abs(tAz)
                         + Math.abs(tBx) + Math.abs(tBy) + Math.abs(tBz)
                         + Math.abs(tCx) + Math.abs(tCy) + Math.abs(tCz);
            if (Math.abs(rDirect.mX) < MathEX.Code.DBL_EPSILON*tNorm) rDirect.mX = 0.0;
            if (Math.abs(rDirect.mY) < MathEX.Code.DBL_EPSILON*tNorm) rDirect.mY = 0.0;
            if (Math.abs(rDirect.mZ) < MathEX.Code.DBL_EPSILON*tNorm) rDirect.mZ = 0.0;
        }
    }
    default void toDirect(XYZ rCartesian) {
        if (!isPrism()) {
            rCartesian.div2this(this);
        } else
        if (isLmpStyle()) {
            double tX  =  x(), tY  =  y(), tZ  =  z();
            double tXY = xy(), tXZ = xz(), tYZ = yz();
            rCartesian.setXYZ(
                rCartesian.mX/tX - tXY*rCartesian.mY/(tX*tY) + (tXY*tYZ - tXZ*tY)*rCartesian.mZ/(tX*tY*tZ),
                rCartesian.mY/tY - tYZ*rCartesian.mZ/(tY*tZ),
                rCartesian.mZ/tZ
            );
        } else {
            // 默认实现简单处理，不缓存中间结果
            XYZ tA = XYZ.toXYZ(a());
            XYZ tB = XYZ.toXYZ(b());
            XYZ tC = XYZ.toXYZ(c());
            double tV = tA.mixed(tB, tC);
            rCartesian.setXYZ(
                tB.mixed(tC, rCartesian) / tV,
                tC.mixed(tA, rCartesian) / tV,
                tA.mixed(tB, rCartesian) / tV
            );
        }
        // direct 需要考虑计算误差带来的出边界的问题，现在支持自动靠近所有整数值
        if (Math.abs(rCartesian.mX) < MathEX.Code.DBL_EPSILON) {
            rCartesian.mX = 0.0;
        } else {
            int tIntX = MathEX.Code.round2int(rCartesian.mX);
            if (MathEX.Code.numericEqual(rCartesian.mX, tIntX)) rCartesian.mX = tIntX;
        }
        if (Math.abs(rCartesian.mY) < MathEX.Code.DBL_EPSILON) {
            rCartesian.mY = 0.0;
        } else {
            int tIntY = MathEX.Code.round2int(rCartesian.mY);
            if (MathEX.Code.numericEqual(rCartesian.mY, tIntY)) rCartesian.mY = tIntY;
        }
        if (Math.abs(rCartesian.mZ) < MathEX.Code.DBL_EPSILON) {
            rCartesian.mZ = 0.0;
        } else {
            int tIntZ = MathEX.Code.round2int(rCartesian.mZ);
            if (MathEX.Code.numericEqual(rCartesian.mZ, tIntZ)) rCartesian.mZ = tIntZ;
        }
    }
}
