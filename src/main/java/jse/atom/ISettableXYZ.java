package jse.atom;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * 可以设置 xyz 值的三维坐标接口，通过 {@link #setX(double)}, {@link #setY(double)},
 * {@link #setZ(double)} 来修改具体坐标值，并提供更多涉及修改自身的运算。
 * <p>
 * 进一步提供了 {@link #getX()}, {@link #getY()}, {@link #getZ()} 来获取坐标值，借助
 * <a href="https://groovy-lang.org/style-guide.html#_getters_and_setters">
 * groovy 的自动处理 Getter 和 Setter 功能</a>，可以直接这样修改：
 * <pre> {@code
 * xyz.x += 1
 * xyz.y *= 0.5
 * } </pre>
 *
 * @see XYZ XYZ: 三维坐标的一般实现
 * @see IXYZ IXYZ: 通用的 xyz 三维坐标接口
 * @author liqa
 */
public interface ISettableXYZ extends IXYZ {
    /**
     * 设置 x 坐标值
     * @param aX 需要设置的 x 坐标
     * @return 自身方便链式调用
     */
    ISettableXYZ setX(double aX);
    /**
     * 设置 y 坐标值
     * @param aY 需要设置的 y 坐标
     * @return 自身方便链式调用
     */
    ISettableXYZ setY(double aY);
    /**
     * 设置 z 坐标值
     * @param aZ 需要设置的 z 坐标
     * @return 自身方便链式调用
     */
    ISettableXYZ setZ(double aZ);
    
    /**
     * 同时设置 xyz 三个坐标值，对于一些比较复杂的原子坐标实现，同时设置往往会更快
     * @param aX 需要设置的 x 坐标
     * @param aY 需要设置的 y 坐标
     * @param aZ 需要设置的 z 坐标
     * @return 自身方便链式调用
     */
    default ISettableXYZ setXYZ(double aX, double aY, double aZ) {return setX(aX).setY(aY).setZ(aZ);}
    /**
     * 同时设置 xyz 三个坐标值，对于一些比较复杂的原子坐标实现，同时设置往往会更快
     * @param aXYZ 需要设置的 xyz 坐标
     * @return 自身方便链式调用
     * @see IXYZ
     */
    default ISettableXYZ setXYZ(IXYZ aXYZ) {return setXYZ(aXYZ.x(), aXYZ.y(), aXYZ.z());}
    
    /// Groovy stuffs
    /** @see #x() */ @VisibleForTesting default double getX() {return x();}
    /** @see #y() */ @VisibleForTesting default double getY() {return y();}
    /** @see #z() */ @VisibleForTesting default double getZ() {return z();}
    
    /** 将 {@link #cross(IXYZ)} 的运算结果直接设置到自身 */
    default void cross2this(IXYZ aRHS) {cross2this(aRHS.x(), aRHS.y(), aRHS.z());}
    /** 将 {@link #cross(XYZ)} 的运算结果直接设置到自身 */
    default void cross2this(XYZ aRHS) {cross2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** 将 {@link #cross(double, double, double)} 的运算结果直接设置到自身 */
    default void cross2this(double aX, double aY, double aZ) {
        double tX = x();
        double tY = y();
        double tZ = z();
        setXYZ(
            tY*aZ - aY*tZ,
            tZ*aX - aZ*tX,
            tX*aY - aX*tY
        );
    }
    
    /** 将 {@link #negative()} 的运算结果直接设置到自身 */
    default void negative2this() {setXYZ(-x(), -y(), -z());}
    /** 将 {@link #abs()} 的运算结果直接设置到自身 */
    default void abs2this()  {setXYZ(Math.abs(x()), Math.abs(y()), Math.abs(z()));}
    
    /** 将 {@link #plus(IXYZ)} 的运算结果直接设置到自身 */
    default void plus2this(IXYZ aRHS) {plus2this(aRHS.x(), aRHS.y(), aRHS.z());}
    /** 将 {@link #plus(XYZ)} 的运算结果直接设置到自身 */
    default void plus2this(XYZ aRHS) {plus2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** 将 {@link #plus(double, double, double)} 的运算结果直接设置到自身 */
    default void plus2this(double aX, double aY, double aZ) {setXYZ(x()+aX, y()+aY, z()+aZ);}
    /** 将 {@link #plus(double)} 的运算结果直接设置到自身 */
    default void plus2this(double aRHS) {setXYZ(x()+aRHS, y()+aRHS, z()+aRHS);}
    
    /**
     * 运算 {@code this.plus2this(aRHS.multiply(aMul))}
     * @see #plus2this(IXYZ)
     * @see #multiply(double)
     */
    default void mplus2this(IXYZ aRHS, double aMul) {mplus2this(aRHS.x(), aRHS.y(), aRHS.z(), aMul);}
    /**
     * 运算 {@code this.plus2this(aRHS.multiply(aMul))}
     * @see #plus2this(XYZ)
     * @see #multiply(double)
     */
    default void mplus2this(XYZ aRHS, double aMul) {mplus2this(aRHS.mX, aRHS.mY, aRHS.mZ, aMul);}
    /**
     * 运算 {@code this.plus2this(aX*aMul, aY*aMul, aZ*aMul)}
     * @see #plus2this(double, double, double)
     */
    default void mplus2this(double aX, double aY, double aZ, double aMul) {setXYZ(x() + aMul*aX, y() + aMul*aY, z() + aMul*aZ);}
    
    /** 将 {@link #minus(IXYZ)} 的运算结果直接设置到自身 */
    default void minus2this(IXYZ aRHS) {minus2this(aRHS.x(), aRHS.y(), aRHS.z());}
    /** 将 {@link #minus(XYZ)} 的运算结果直接设置到自身 */
    default void minus2this(XYZ aRHS) {minus2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** 将 {@link #minus(double, double, double)} 的运算结果直接设置到自身 */
    default void minus2this(double aX, double aY, double aZ) {setXYZ(x()-aX, y()-aY, z()-aZ);}
    /** 将 {@link #minus(double)} 的运算结果直接设置到自身 */
    default void minus2this(double aRHS) {setXYZ(x()-aRHS, y()-aRHS, z()-aRHS);}
    
    /** 将 {@link #lminus(IXYZ)} 的运算结果直接设置到自身 */
    default void lminus2this(IXYZ aRHS) {lminus2this(aRHS.x(), aRHS.y(), aRHS.z());}
    /** 将 {@link #lminus(XYZ)} 的运算结果直接设置到自身 */
    default void lminus2this(XYZ aRHS) {lminus2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** 将 {@link #lminus(double, double, double)} 的运算结果直接设置到自身 */
    default void lminus2this(double aX, double aY, double aZ) {setXYZ(aX-x(), aY-y(), aZ-z());}
    /** 将 {@link #lminus(double)} 的运算结果直接设置到自身 */
    default void lminus2this(double aRHS) {setXYZ(aRHS-x(), aRHS-y(), aRHS-z());}
    
    /** 将 {@link #multiply(IXYZ)} 的运算结果直接设置到自身 */
    default void multiply2this(IXYZ aRHS) {multiply2this(aRHS.x(), aRHS.y(), aRHS.z());}
    /** 将 {@link #multiply(XYZ)} 的运算结果直接设置到自身 */
    default void multiply2this(XYZ aRHS) {multiply2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** 将 {@link #multiply(double, double, double)} 的运算结果直接设置到自身 */
    default void multiply2this(double aX, double aY, double aZ) {setXYZ(x()*aX, y()*aY, z()*aZ);}
    /** 将 {@link #multiply(double)} 的运算结果直接设置到自身 */
    default void multiply2this(double aRHS) {setXYZ(x()*aRHS, y()*aRHS, z()*aRHS);}
    
    /** 将 {@link #div(IXYZ)} 的运算结果直接设置到自身 */
    default void div2this(IXYZ aRHS) {div2this(aRHS.x(), aRHS.y(), aRHS.z());}
    /** 将 {@link #div(XYZ)} 的运算结果直接设置到自身 */
    default void div2this(XYZ aRHS) {div2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** 将 {@link #div(double, double, double)} 的运算结果直接设置到自身 */
    default void div2this(double aX, double aY, double aZ) {setXYZ(x()/aX, y()/aY, z()/aZ);}
    /** 将 {@link #div(double)} 的运算结果直接设置到自身 */
    default void div2this(double aRHS) {setXYZ(x()/aRHS, y()/aRHS, z()/aRHS);}
    
    /** 将 {@link #ldiv(IXYZ)} 的运算结果直接设置到自身 */
    default void ldiv2this(IXYZ aRHS) {ldiv2this(aRHS.x(), aRHS.y(), aRHS.z());}
    /** 将 {@link #ldiv(XYZ)} 的运算结果直接设置到自身 */
    default void ldiv2this(XYZ aRHS) {ldiv2this(aRHS.mX, aRHS.mY, aRHS.mZ);}
    /** 将 {@link #ldiv(double, double, double)} 的运算结果直接设置到自身 */
    default void ldiv2this(double aX, double aY, double aZ) {setXYZ(aX/x(), aY/y(), aZ/z());}
    /** 将 {@link #ldiv(double)} 的运算结果直接设置到自身 */
    default void ldiv2this(double aRHS) {setXYZ(aRHS/x(), aRHS/y(), aRHS/z());}
}
