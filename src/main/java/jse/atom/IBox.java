package jse.atom;

import jse.lmp.LmpBox;
import jse.math.MathEX;

/**
 * 通用的模拟盒接口，现在支持斜方的模拟盒；
 * 对于通用情况的使用，{@link #a()}, {@link #b()}, {@link #c()}
 * 对应模拟盒三个基向量。
 * <p>
 * 只支持右手系的基向量，也就是要求 {@code a().cross(b()).dot(c()) > 0}
 * <p>
 * jse 的模拟盒通用实现下，基向量都从原点出发，对于存在下边界的情况（例如
 * lammps 的模拟盒），这里会自动消去这些下边界，或者通过 {@link LmpBox}
 * 中特定的 {@link LmpBox#xlo()}, {@link LmpBox#xhi()} 等接口来获取到实际的上下边界。
 * <p>
 * 继承 {@link IXYZ}，其中 x,y,z 对应于 {@link #ax()}, {@link #by()}, {@link #cz()}
 *
 * @see Box Box: 一般的正交的模拟盒实现
 * @see BoxPrism BoxPrism: 一般的三斜的模拟盒实现
 * @see IXYZ IXYZ: 一般的三维向量接口
 * @see LmpBox LmpBox: 对于包含上下边界的 lammps 模拟盒实现
 * @author liqa
 */
public interface IBox extends IXYZ {
    /**
     * @return 此模拟盒是否是三斜的；
     * 这只是一个模拟盒类型检测，即可能存在模拟盒类型是三斜的，但是非对角项都为
     * 0，此时依旧会返回 {@code true}
     */
    default boolean isPrism() {return false;}
    /**
     * @return 此模拟盒是否是下三角的（满足 lammps 的风格）；
     * 这只是一个模拟盒类型检测，即可能存在模拟盒类型不是 lammps 风格的，但是上三角非对焦元都为
     * 0，此时依旧会返回 {@code false}
     */
    default boolean isLmpStyle() {return true;}
    
    /**
     * @return 此模拟盒的第一个基向量 {@code a}
     * @see IXYZ
     */
    default IXYZ a() {return new XYZ(ax(), ay(), az());}
    /**
     * @return 此模拟盒的第二个基向量 {@code b}
     * @see IXYZ
     */
    default IXYZ b() {return new XYZ(bx(), by(), bz());}
    /**
     * @return 此模拟盒的第三个基向量 {@code c}
     * @see IXYZ
     */
    default IXYZ c() {return new XYZ(cx(), cy(), cz());}
    
    /** @return {@inheritDoc} */
    @Override IBox copy();
    
    /**
     * 调用此方法可以避免创建临时 xyz 对象
     * @return {@code a().x()}
     * @see #a()
     * @see IXYZ#x()
     */
    double ax();
    /**
     * 调用此方法可以避免创建临时 xyz 对象
     * @return {@code a().y()}
     * @see #a()
     * @see IXYZ#y()
     */
    default double ay() {return 0.0;}
    /**
     * 调用此方法可以避免创建临时 xyz 对象
     * @return {@code a().z()}
     * @see #a()
     * @see IXYZ#z()
     */
    default double az() {return 0.0;}
    
    /**
     * 调用此方法可以避免创建临时 xyz 对象
     * @return {@code b().x()}
     * @see #b()
     * @see IXYZ#x()
     */
    default double bx() {return 0.0;}
    /**
     * 调用此方法可以避免创建临时 xyz 对象
     * @return {@code b().y()}
     * @see #b()
     * @see IXYZ#y()
     */
    double by();
    /**
     * 调用此方法可以避免创建临时 xyz 对象
     * @return {@code b().z()}
     * @see #b()
     * @see IXYZ#z()
     */
    default double bz() {return 0.0;}
    
    /**
     * 调用此方法可以避免创建临时 xyz 对象
     * @return {@code c().x()}
     * @see #c()
     * @see IXYZ#x()
     */
    default double cx() {return 0.0;}
    /**
     * 调用此方法可以避免创建临时 xyz 对象
     * @return {@code c().y()}
     * @see #c()
     * @see IXYZ#y()
     */
    default double cy() {return 0.0;}
    /**
     * 调用此方法可以避免创建临时 xyz 对象
     * @return {@code c().z()}
     * @see #c()
     * @see IXYZ#z()
     */
    double cz();
    
    /// IXYZ stuffs
    /** @return {@link #ax()} */ @Override default double x() {return ax();}
    /** @return {@link #by()} */ @Override default double y() {return by();}
    /** @return {@link #cz()} */ @Override default double z() {return cz();}
    
    /// lammps prism box stuffs
    /** @return {@link #bx()} */ default double xy() {return bx();}
    /** @return {@link #cx()} */ default double xz() {return cx();}
    /** @return {@link #cy()} */ default double yz() {return cy();}
    
    /** @return 此模拟盒的体积 */
    default double volume() {
        if (isLmpStyle()) return prod();
        return a().mixed(b(), c());
    }
    
    /**
     * 将输入的 xyz 坐标根据周期边界条件变换到此模拟盒内
     * @param rXYZ 输入的需要变换的坐标值，结果会直接修改到内部
     * @see XYZ
     */
    default void wrapPBC(XYZ rXYZ) {
        toDirect(rXYZ);
        if (rXYZ.mX<0.0 || rXYZ.mX>=1.0) {rXYZ.mX -= MathEX.Code.floor(rXYZ.mX);}
        if (rXYZ.mY<0.0 || rXYZ.mY>=1.0) {rXYZ.mY -= MathEX.Code.floor(rXYZ.mY);}
        if (rXYZ.mZ<0.0 || rXYZ.mZ>=1.0) {rXYZ.mZ -= MathEX.Code.floor(rXYZ.mZ);}
        toCartesian(rXYZ);
    }
    /**
     * 将输入的 scaled xyz 坐标（direct）转换成没有 scaled 坐标（笛卡尔坐标）
     * <p>
     * 注意 jse 原子的坐标统一是没有 scaled 的笛卡尔坐标
     * @param rDirect 输入的需要变换的 scaled xyz 坐标值，结果会直接修改到内部
     * @see XYZ
     */
    default void toCartesian(XYZ rDirect) {
        if (!isPrism()) {
            double tX = x(), tY = y(), tZ = z();
            rDirect.multiply2this(tX, tY, tZ);
        } else
        if (isLmpStyle()) {
            double tX  =  x(), tY  =  y(), tZ  =  z();
            double tXY = xy(), tXZ = xz(), tYZ = yz();
            rDirect.setXYZ(
                tX*rDirect.mX + tXY*rDirect.mY + tXZ*rDirect.mZ,
                tY*rDirect.mY + tYZ*rDirect.mZ,
                tZ*rDirect.mZ
            );
        } else {
            double tAx = ax(), tAy = ay(), tAz = az();
            double tBx = bx(), tBy = by(), tBz = bz();
            double tCx = cx(), tCy = cy(), tCz = cz();
            rDirect.setXYZ(
                tAx*rDirect.mX + tBx*rDirect.mY + tCx*rDirect.mZ,
                tAy*rDirect.mX + tBy*rDirect.mY + tCy*rDirect.mZ,
                tAz*rDirect.mX + tBz*rDirect.mY + tCz*rDirect.mZ
            );
        }
    }
    /**
     * 将输入的没有 scaled xyz 坐标（笛卡尔坐标）转换成 scaled 坐标（direct）
     * <p>
     * 注意 jse 原子的坐标统一是没有 scaled 的笛卡尔坐标
     * @param rCartesian 输入的需要变换没有 scaled xyz 笛卡尔坐标值，结果会直接修改到内部
     * @see XYZ
     */
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
            if (tIntX!=0 && MathEX.Code.numericEqual(rCartesian.mX, tIntX)) rCartesian.mX = tIntX;
        }
        if (Math.abs(rCartesian.mY) < MathEX.Code.DBL_EPSILON) {
            rCartesian.mY = 0.0;
        } else {
            int tIntY = MathEX.Code.round2int(rCartesian.mY);
            if (tIntY!=0 && MathEX.Code.numericEqual(rCartesian.mY, tIntY)) rCartesian.mY = tIntY;
        }
        if (Math.abs(rCartesian.mZ) < MathEX.Code.DBL_EPSILON) {
            rCartesian.mZ = 0.0;
        } else {
            int tIntZ = MathEX.Code.round2int(rCartesian.mZ);
            if (tIntZ!=0 && MathEX.Code.numericEqual(rCartesian.mZ, tIntZ)) rCartesian.mZ = tIntZ;
        }
    }
}
