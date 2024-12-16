package jse.atom;

/**
 * 一般的原子实现，只包含 {@code x, y, z, id, type} 信息。
 * <p>
 * 直接通过：
 * <pre> {@code
 * def atom = new Atom(x, y, z, id, type)
 * } </pre>
 * 来创建一个自定义的原子
 * <p>
 * 除了通用的 {@link #x()}, {@link #y()}, {@link #z()}, {@link #id()}, {@link #type()}
 * 方法来获取属性，还可以通过直接访问成员变量 {@link #mX}, {@link #mY}, {@link #mZ},
 * {@link #mID}, {@link #mType} 来获取以及修改。
 *
 * @see IAtom IAtom: 通用的原子接口
 * @see ISettableAtom ISettableAtom: 可设置原子属性的原子接口
 * @see AtomFull AtomFull: 包含更多信息的原子实现
 * @author liqa
 */
public class Atom extends AbstractSettableAtom {
    public double mX, mY, mZ;
    public int mID, mType;
    /**
     * 创建一个原子对象
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     * @param aID 原子的 id
     * @param aType 原子的种类编号
     */
    public Atom(double aX, double aY, double aZ, int aID, int aType) {
        mX = aX; mY = aY; mZ = aZ;
        mID = aID; mType = aType;
    }
    /**
     * 通过一个 {@link IXYZ} 对象创建一个原子对象
     * @param aXYZ 原子的 xyz 坐标
     * @param aID 原子的 id
     * @param aType 原子的种类编号
     * @see IXYZ
     */
    public Atom(IXYZ aXYZ, int aID, int aType) {
        mX = aXYZ.x(); mY = aXYZ.y(); mZ = aXYZ.z();
        mID = aID; mType = aType;
    }
    /**
     * 直接通过一个原子创建一个新的原子对象
     * @param aAtom 已有的任意原子对象
     * @see IAtom
     */
    public Atom(IAtom aAtom) {
        mX = aAtom.x(); mY = aAtom.y(); mZ = aAtom.z();
        mID = aAtom.id(); mType = aAtom.type();
    }
    /**
     * 创建一个默认的原子对象，即 {@code new Atom(0.0, 0.0, 0.0, 1, 1)}
     */
    public Atom() {this(0.0, 0.0, 0.0, 1, 1);}
    
    /** @return {@inheritDoc} */
    @Override public double x() {return mX;}
    /** @return {@inheritDoc} */
    @Override public double y() {return mY;}
    /** @return {@inheritDoc} */
    @Override public double z() {return mZ;}
    
    /** @return {@inheritDoc} */
    @Override public int id() {return mID;}
    /** @return {@inheritDoc} */
    @Override public int type() {return mType;}
    
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
     * @param aID {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public Atom setID(int aID) {mID = aID; return this;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public Atom setType(int aType) {mType = aType; return this;}
}
