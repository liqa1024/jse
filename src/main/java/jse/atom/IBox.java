package jse.atom;

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
    
    /** 提供一个简单通用的坐标相互转换接口，这里不直接创建新的 XYZ 而是修改输入的 XYZ，可以保证性能，减少重复代码 */
    default void toCartesian(XYZ rDirect) {
        if (!isPrism()) {
            rDirect.multiply2this(this);
        } else
        if (isLmpStyle()) {
            rDirect.setXYZ(
                x()*rDirect.mX + xy()*rDirect.mY + xz()*rDirect.mZ,
                y()*rDirect.mY + yz()*rDirect.mZ,
                z()*rDirect.mZ
            );
        } else {
            rDirect.setXYZ(
                ax()*rDirect.mX + bx()*rDirect.mY + cx()*rDirect.mZ,
                ay()*rDirect.mX + by()*rDirect.mY + cy()*rDirect.mZ,
                az()*rDirect.mX + bz()*rDirect.mY + cz()*rDirect.mZ
            );
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
    }
}
