package jse.atom;

/**
 * 通用的模拟盒类，现在包含更多信息，支持斜方的模拟盒；
 * 只支持右手系的基组
 * @author liqa
 */
public interface IBox extends IXYZ {
    /** 是否是斜方的，很多时候这只是一个标记，即使实际是正交的也可以获取到 {@code true} */
    default boolean isPrism() {return false;}
    
    /** 一般的接口，获取三个基，要求这三个基满足右手系，也就是 {@code (a x b) · c > 0} */
    IXYZ a();
    IXYZ b();
    IXYZ c();
    default double volume() {
        if (!isPrism()) return prod();
        return a().mixed(b(), c());
    }
    /** 还需要提供一个复制的方法方便拷贝 */
    IBox copy();
    
    /** IXYZ stuffs */
    default double x() {return a().x();}
    default double y() {return b().y();}
    default double z() {return c().z();}
    
    /** lammps prism box stuffs */
    default double xy() {return b().y();}
    default double xz() {return c().x();}
    default double yz() {return c().y();}
    /** 是否是 lammps 的风格，也就是 {@code ay = az = bz = 0}，当然很多时候这只是一个标记 */
    default boolean isLmpStyle() {return true;}
    
    /** 提供一个简单通用的坐标相互转换接口，这里不直接创建新的 XYZ 而是修改输入的 XYZ，可以保证性能，减少重复代码 */
    default void toCartesian(XYZ rDirect) {
        if (isPrism()) {
            IXYZ tA = a();
            IXYZ tB = b();
            IXYZ tC = c();
            rDirect.setXYZ(
                tA.x()*rDirect.mX + tB.x()*rDirect.mY + tC.x()*rDirect.mZ,
                tA.y()*rDirect.mX + tB.y()*rDirect.mY + tC.y()*rDirect.mZ,
                tA.z()*rDirect.mX + tB.z()*rDirect.mY + tC.z()*rDirect.mZ
            );
        } else {
            rDirect.multiply2this(this);
        }
    }
    default void toDirect(XYZ rCartesian) {
        if (isPrism()) {
            // 默认实现不缓存中间结果
            XYZ tA = XYZ.toXYZ(a());
            XYZ tB = XYZ.toXYZ(b());
            XYZ tC = XYZ.toXYZ(c());
            double tV = tA.mixed(tB, tC);
            rCartesian.setXYZ(
                tB.mixed(tC, rCartesian) / tV,
                tC.mixed(tA, rCartesian) / tV,
                tA.mixed(tB, rCartesian) / tV
            );
        } else {
            rCartesian.div2this(this);
        }
    }
}
