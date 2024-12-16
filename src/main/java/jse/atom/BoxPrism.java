package jse.atom;

import jse.math.MathEX;
import org.jetbrains.annotations.NotNull;

/**
 * 一般的三斜的模拟盒实现
 * <p>
 * 直接通过：
 * <pre> {@code
 * def box = new BoxPrism(a, b, c)
 * } </pre>
 * 来创建一个三斜的模拟盒对象
 *
 * @see Box Box: 一般的正交模拟盒实现
 * @see IBox IBox: 通用的模拟盒接口
 * @author liqa
 */
public final class BoxPrism implements IBox {
    private final @NotNull XYZ mA, mB, mC;
    
    /**
     * 通过三个基向量来创建一个三斜模拟盒对象，输入的 xyz 在内部都会统一进行一次值拷贝
     * <p>
     * 要求三个基向量满足右手系，即 {@code aA.cross(aB).dot(aC) > 0}
     *
     * @param aA 模拟盒第一个基向量
     * @param aB 模拟盒第二个基向量
     * @param aC 模拟盒第三个基向量
     *
     * @see IXYZ#cross(IXYZ)
     * @see IXYZ#dot(IXYZ)
     */
    public BoxPrism(@NotNull IXYZ aA, @NotNull IXYZ aB, @NotNull IXYZ aC) {
        mA = new XYZ(aA); mB = new XYZ(aB); mC = new XYZ(aC);
        // 现在直接在创建时初始化缓存，从根本上杜绝线程读取不安全的问题
        initCache_();
    }
    /**
     * 直接通过基向量的每个方向值类创建一个三斜模拟盒对象
     * @param aAx 模拟盒第一个基向量的 x 方向
     * @param aAy 模拟盒第一个基向量的 y 方向
     * @param aAz 模拟盒第一个基向量的 z 方向
     * @param aBx 模拟盒第二个基向量的 x 方向
     * @param aBy 模拟盒第二个基向量的 y 方向
     * @param aBz 模拟盒第二个基向量的 z 方向
     * @param aCx 模拟盒第三个基向量的 x 方向
     * @param aCy 模拟盒第三个基向量的 y 方向
     * @param aCz 模拟盒第三个基向量的 z 方向
     */
    public BoxPrism(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        mA = new XYZ(aAx, aAy, aAz);
        mB = new XYZ(aBx, aBy, aBz);
        mC = new XYZ(aCx, aCy, aCz);
        // 现在直接在创建时初始化缓存，从根本上杜绝线程读取不安全的问题
        initCache_();
    }
    /** @return {@inheritDoc} */
    @Override public BoxPrism copy() {return new BoxPrism(mA, mB, mC);}
    
    /**
     * @return 此模拟盒是否是下三角的（满足 lammps 的风格）；
     * 对于 {@link BoxPrism} 永远都是 {@code false}
     */
    @Override public boolean isLmpStyle() {return false;}
    /**
     * @return 此模拟盒是否是三斜的；
     * 对于 {@link BoxPrism} 永远都是 {@code true}
     */
    @Override public boolean isPrism() {return true;}
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #a()
     * @see IXYZ#x()
     */
    @Override public double ax() {return mA.mX;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #a()
     * @see IXYZ#y()
     */
    @Override public double ay() {return mA.mY;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #a()
     * @see IXYZ#z()
     */
    @Override public double az() {return mA.mZ;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #b()
     * @see IXYZ#x()
     */
    @Override public double bx() {return mB.mX;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #b()
     * @see IXYZ#y()
     */
    @Override public double by() {return mB.mY;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #b()
     * @see IXYZ#z()
     */
    @Override public double bz() {return mB.mZ;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #c()
     * @see IXYZ#x()
     */
    @Override public double cx() {return mC.mX;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #c()
     * @see IXYZ#y()
     */
    @Override public double cy() {return mC.mY;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #c()
     * @see IXYZ#z()
     */
    @Override public double cz() {return mC.mZ;}
    
    /** @return 此模拟盒的字符串表示，这里转换只保留 4 位有效数字（不影响实际精度）*/
    @Override public String toString() {
        return String.format("a: (%.4g, %.4g, %.4g)\n", mA.mX, mA.mY, mA.mZ)
             + String.format("b: (%.4g, %.4g, %.4g)\n", mB.mX, mB.mY, mB.mZ)
             + String.format("c: (%.4g, %.4g, %.4g)"  , mC.mX, mC.mY, mC.mZ);
    }
    
    
    /// optimize stuffs
    /** @return {@inheritDoc} */
    @Override public double volume() {return mA.mixed(mB, mC);}
    
    // 为了加速运算，内部会缓存中间变量，因此这个实现的 mA，mB，mC 都是不能修改的
    private XYZ mBC = null, mCA = null, mAB = null;
    private double mV = Double.NaN;
    private void initCache_() {
        mBC = mB.cross(mC);
        mCA = mC.cross(mA);
        mAB = mA.cross(mB);
        mV = mA.mixed(mB, mC);
    }
    /**
     * {@inheritDoc}
     * @param rCartesian {@inheritDoc}
     * @see XYZ
     */
    @Override public void toDirect(XYZ rCartesian) {
        rCartesian.setXYZ(
            mBC.dot(rCartesian) / mV,
            mCA.dot(rCartesian) / mV,
            mAB.dot(rCartesian) / mV
        );
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
