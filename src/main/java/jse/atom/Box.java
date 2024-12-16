package jse.atom;

import org.jetbrains.annotations.NotNull;

/**
 * 一般的模拟盒实现，此实现只考虑了正交的模拟盒
 * <p>
 * 直接通过：
 * <pre> {@code
 * def box = new Box(x, y, z)
 * } </pre>
 * 来创建一个正交的模拟盒对象
 *
 * @see BoxPrism BoxPrism: 一般的三斜的模拟盒实现
 * @see IBox IBox: 通用的模拟盒接口
 * @author liqa
 */
public final class Box implements IBox {
    private final @NotNull XYZ mBox;
    
    /**
     * 通过一个已有的 {@link IXYZ} 创建一个模拟盒对象，三个方向值对应模拟盒三个边长
     * @param aBox 已有的任意的 xyz 坐标对象
     * @see IXYZ
     */
    public Box(@NotNull IXYZ aBox) {mBox = new XYZ(aBox);}
    /**
     * 创建一个模拟盒对象
     * @param aX 模拟盒 x 方向边长
     * @param aY 模拟盒 y 方向边长
     * @param aZ 模拟盒 z 方向边长
     */
    public Box(double aX, double aY, double aZ) {mBox = new XYZ(aX, aY, aZ);}
    
    /** @return {@inheritDoc} */
    @Override public Box copy() {return new Box(mBox);}
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #a()
     * @see IXYZ#x()
     */
    @Override public double ax() {return mBox.mX;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #b()
     * @see IXYZ#y()
     */
    @Override public double by() {return mBox.mY;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #c()
     * @see IXYZ#z()
     */
    @Override public double cz() {return mBox.mZ;}
    
    /** @return 此模拟盒的字符串表示，这里转换只保留 4 位有效数字（不影响实际精度）*/
    @Override public String toString() {
        return String.format("(%.4g, %.4g, %.4g)", mBox.mX, mBox.mY, mBox.mZ);
    }
}
