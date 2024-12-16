package jse.atom;

import org.jetbrains.annotations.VisibleForTesting;

/**
 * 可以设置属性值的原子接口，通过 {@link #setX(double)}, {@link #setY(double)},
 * {@link #setZ(double)} 来修改具体坐标值；通过 {@link #setID(int)}
 * 来修改原子的 {@code id}，{@link #setType(int)} 来修改原子的种类编号。
 * <p>
 * 进一步提供了 {@link #getX()}, {@link #getY()}, {@link #getZ()},
 * {@link #getId()}, {@link #getType()} 来获取属性，借助
 * <a href="https://groovy-lang.org/style-guide.html#_getters_and_setters">
 * groovy 的自动处理 Getter 和 Setter 功能</a>，可以直接这样修改：
 * <pre> {@code
 * atom.x += 1
 * atom.y *= 0.5
 * } </pre>
 * <p>
 * 继承 {@link ISettableXYZ} 从而自动可以使用三维坐标的相关运算
 *
 * @see IAtom IAtom: 通用的原子接口
 * @see ISettableXYZ ISettableXYZ: 可以设置的三维坐标接口
 * @see Atom Atom: 一般的原子实现
 * @see AtomFull AtomFull: 包含速度信息的原子实现
 * @author liqa
 */
public interface ISettableAtom extends IAtom, ISettableXYZ {
    /** @return {@inheritDoc} */
    ISettableAtom copy();
    
    /**
     * {@inheritDoc}
     * @param aX {@inheritDoc}
     * @return {@inheritDoc}
     */
    ISettableAtom setX(double aX);
    /**
     * {@inheritDoc}
     * @param aY {@inheritDoc}
     * @return {@inheritDoc}
     */
    ISettableAtom setY(double aY);
    /**
     * {@inheritDoc}
     * @param aZ {@inheritDoc}
     * @return {@inheritDoc}
     */
    ISettableAtom setZ(double aZ);
    /**
     * {@inheritDoc}
     * @param aX {@inheritDoc}
     * @param aY {@inheritDoc}
     * @param aZ {@inheritDoc}
     * @return {@inheritDoc}
     */
    default ISettableAtom setXYZ(double aX, double aY, double aZ) {return setX(aX).setY(aY).setZ(aZ);}
    /**
     * {@inheritDoc}
     * @param aXYZ {@inheritDoc}
     * @return {@inheritDoc}
     * @see IXYZ
     */
    default ISettableAtom setXYZ(IXYZ aXYZ) {return setXYZ(aXYZ.x(), aXYZ.y(), aXYZ.z());}
    
    /**
     * 设置原子 id 值，从 1 开始
     * @param aID 需要设置的 id 值
     * @return 自身方便链式调用
     */
    ISettableAtom setID(int aID);
    /**
     * 设置原子种类编号，从 1 开始
     * @param aType 需要设置的种类编号
     * @return 自身方便链式调用
     */
    ISettableAtom setType(int aType);
    
    /**
     * 设置原子 x 方向速度值
     * @param aVx 需要设置的速度值
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 当原子不存在速度信息
     * @see #hasVelocity()
     */
    default ISettableAtom setVx(double aVx) {throw new UnsupportedOperationException("setVx");}
    /**
     * 设置原子 y 方向速度值
     * @param aVy 需要设置的速度值
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 当原子不存在速度信息
     * @see #hasVelocity()
     */
    default ISettableAtom setVy(double aVy) {throw new UnsupportedOperationException("setVy");}
    /**
     * 设置原子 z 方向速度值
     * @param aVz 需要设置的速度值
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 当原子不存在速度信息
     * @see #hasVelocity()
     */
    default ISettableAtom setVz(double aVz) {throw new UnsupportedOperationException("setVz");}
    /**
     * 同时设置原子 xyz 三个方向的速度值，对于一些比较复杂的原子坐标实现，同时设置往往会更快
     * @param aVx 需要设置的 x 方向速度值
     * @param aVy 需要设置的 y 方向速度值
     * @param aVz 需要设置的 z 方向速度值
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 当原子不存在速度信息
     * @see #hasVelocity()
     */
    default ISettableAtom setVxyz(double aVx, double aVy, double aVz) {return setVx(aVx).setVy(aVy).setVz(aVz);}
    /**
     * 同时设置 xyz 三个方向的速度值，对于一些比较复杂的原子坐标实现，同时设置往往会更快
     * @param aVxyz 需要设置的速度向量
     * @return 自身方便链式调用
     * @throws UnsupportedOperationException 当原子不存在速度信息
     * @see #hasVelocity()
     * @see IXYZ
     */
    default ISettableAtom setVxyz(IXYZ aVxyz) {return setVxyz(aVxyz.x(), aVxyz.y(), aVxyz.z());}
    
    /// Groovy stuffs
    /** @see #x() */ @VisibleForTesting default double getX() {return x();}
    /** @see #y() */ @VisibleForTesting default double getY() {return y();}
    /** @see #z() */ @VisibleForTesting default double getZ() {return z();}
    /** @see #id() */ @VisibleForTesting default int getId() {return id();}
    /** @see #setID(int) */ @VisibleForTesting default ISettableAtom setId(int aID) {return setID(aID);}
    /** @see #type() */ @VisibleForTesting default int getType() {return type();}
    /** @see #vx() */ @VisibleForTesting default double getVx() {return vx();}
    /** @see #vy() */ @VisibleForTesting default double getVy() {return vy();}
    /** @see #vz() */ @VisibleForTesting default double getVz() {return vz();}
    /** @see #vxyz() */ @VisibleForTesting default IXYZ getVxyz() {return vxyz();}
}
