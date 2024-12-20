package jse.atom;

import groovy.lang.DeprecationException;

/**
 * 一般的原子实现，现在只包含 {@code x, y, z, type} 信息。
 * <p>
 * 直接通过：
 * <pre> {@code
 * def atom = new Atom(x, y, z, type)
 * } </pre>
 * 来创建一个自定义的原子
 * <p>
 * 除了通用的 {@link #x()}, {@link #y()}, {@link #z()}, {@link #type()}
 * 方法来获取属性，还可以通过直接访问成员变量 {@link #mX}, {@link #mY},
 * {@link #mZ}, {@link #mType} 来获取以及修改。
 * <p>
 * 为了使用方便，现在 {@link Atom} 统一不再包含 id 信息，如果需要
 * id 信息的原子，则使用 {@link AtomID} 和 {@link AtomFull}
 *
 * @see IAtom IAtom: 通用的原子接口
 * @see ISettableAtom ISettableAtom: 可设置原子属性的原子接口
 * @see AtomID AtomID: 包含 id 信息的原子实现
 * @see AtomFull AtomFull: 包含更多信息的原子实现
 * @author liqa
 */
public class Atom extends AbstractSettableAtom {
    public double mX, mY, mZ;
    public int mType;
    /**
     * 创建一个原子对象
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     * @param aType 原子的种类编号
     */
    public Atom(double aX, double aY, double aZ, int aType) {
        mX = aX; mY = aY; mZ = aZ;
        mType = aType;
    }
    /**
     * 创建一个不包含种类属性的原子对象，{@code type==1}
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     */
    public Atom(double aX, double aY, double aZ) {this(aX, aY, aZ, 1);}
    /**
     * 创建一个默认的原子对象，{@code type==1}，
     * 坐标值都为 {@code 0.0}
     */
    public Atom() {this(0.0, 0.0, 0.0);}
    /**
     * 通过一个 {@link IXYZ} 对象创建一个原子对象
     * @param aXYZ 原子的 xyz 坐标
     * @param aType 原子的种类编号
     * @see IXYZ
     */
    public Atom(IXYZ aXYZ, int aType) {this(aXYZ.x(), aXYZ.y(), aXYZ.z(), aType);}
    /**
     * 通过一个 {@link IXYZ} 对象创建一个不包含种类属性的原子对象，{@code type==1}
     * @param aXYZ 原子的 xyz 坐标
     * @see IXYZ
     */
    public Atom(IXYZ aXYZ) {this(aXYZ, 1);}
    /**
     * 直接通过一个原子创建一个新的原子对象
     * @param aAtom 已有的任意原子对象
     * @see IAtom
     */
    public Atom(IAtom aAtom) {this(aAtom.x(), aAtom.y(), aAtom.z(), aAtom.type());}
    /**
     * 旧版包含 {@code id} 信息的创建原子对象接口
     * @deprecated use {@link AtomID} or remove {@code aID}
     */
    @Deprecated public Atom(double aX, double aY, double aZ, int aID, int aType) {
        throw new DeprecationException("`Atom` no longer contains `id` information. Remove the `id` parameter or use `AtomID`");
    }
    /**
     * 旧版包含 {@code id} 信息的创建原子对象接口
     * @deprecated use {@link AtomID} or remove {@code aID}
     */
    @Deprecated public Atom(IXYZ aXYZ, int aID, int aType) {
        throw new DeprecationException("`Atom` no longer contains `id` information. Remove the `id` parameter or use `AtomID`");
    }
    /** @return {@inheritDoc} */
    @Override public Atom copy() {return new Atom(this);}
    
    /** @return {@inheritDoc} */
    @Override public double x() {return mX;}
    /** @return {@inheritDoc} */
    @Override public double y() {return mY;}
    /** @return {@inheritDoc} */
    @Override public double z() {return mZ;}
    /** @return {@inheritDoc} */
    @Override public int type() {return mType;}
    /** @return 此原子在 {@link IAtomData} 中的索引位置，对于这个独立的 Atom 则永远会返回 {@code -1} */
    @Override public final int index() {return -1;}
    /** @return 此原子是否包含在 {@link IAtomData} 中的索引信息，对于这个独立的 Atom 则永远会返回 {@code false} */
    @Override public final boolean hasIndex() {return false;}
    
    /// ISettableAtom stuffs
    /**
     * {@inheritDoc}
     * @param aX {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public Atom setX(double aX) {mX = aX; return this;}
    /**
     * {@inheritDoc}
     * @param aY {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public Atom setY(double aY) {mY = aY; return this;}
    /**
     * {@inheritDoc}
     * @param aZ {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public Atom setZ(double aZ) {mZ = aZ; return this;}
    /**
     * {@inheritDoc}
     * @param aX {@inheritDoc}
     * @param aY {@inheritDoc}
     * @param aZ {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public Atom setXYZ(double aX, double aY, double aZ) {mX = aX; mY = aY; mZ = aZ; return this;}
    /**
     * {@inheritDoc}
     * @param aXYZ {@inheritDoc}
     * @return {@inheritDoc}
     * @see IXYZ
     */
    @Override public Atom setXYZ(IXYZ aXYZ) {return setXYZ(aXYZ.x(), aXYZ.y(), aXYZ.z());}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public Atom setType(int aType) {mType = aType; return this;}
}
