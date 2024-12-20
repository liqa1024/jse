package jse.atom;

/**
 * 包含 id 信息的原子实现，一定程度作为旧版 {@link Atom} 兼容方案。
 * <p>
 * 直接通过：
 * <pre> {@code
 * def atom = new AtomID(x, y, z, id, type)
 * } </pre>
 * 来创建一个自定义的原子
 * <p>
 * 除了通用的 {@link #x()}, {@link #y()}, {@link #z()}, {@link #id()}, {@link #type()},
 * 方法来获取属性，还可以通过直接访问成员变量 {@link #mX}, {@link #mY}, {@link #mZ},
 * {@link #mID}, {@link #mType} 来获取以及修改。
 * <p>
 * 很多时候特定指定原子 id 是很繁琐和冗余的，此时可以直接使用 {@link Atom}
 * 来创建一个没有 id 的原子对象
 *
 * @see IAtom IAtom: 通用的原子接口
 * @see ISettableAtom ISettableAtom: 可设置原子属性的原子接口
 * @see Atom Atom: 一般的原子实现
 * @see AtomFull AtomFull: 包含更多信息的原子实现
 * @author liqa
 */
public class AtomID extends Atom {
    public int mID;
    /**
     * 创建一个原子对象
     * @param aX 原子的 x 坐标
     * @param aY 原子的 y 坐标
     * @param aZ 原子的 z 坐标
     * @param aID 原子的 id
     * @param aType 原子的种类编号
     */
    public AtomID(double aX, double aY, double aZ, int aID, int aType) {
        super(aX, aY, aZ, aType);
        mID = aID;
    }
    /**
     * 通过一个 {@link IXYZ} 对象创建一个原子对象
     * @param aXYZ 原子的 xyz 坐标
     * @param aID 原子的 id
     * @param aType 原子的种类编号
     * @see IXYZ
     */
    public AtomID(IXYZ aXYZ, int aID, int aType) {
        super(aXYZ, aType);
        mID = aID;
    }
    /**
     * 直接通过一个原子创建一个新的原子对象
     * @param aAtom 已有的任意原子对象
     * @see IAtom
     */
    public AtomID(IAtom aAtom) {
        super(aAtom);
        // 这里保持简单实现，依旧直接拷贝原本原子的 id 信息，和旧版一致的行为
        mID = aAtom.id();
    }
    /**
     * 创建一个默认的原子对象，{@code id==-1, type==1}，
     * 坐标值都为 {@code 0.0}
     */
    public AtomID() {
        super();
        mID = -1;
    }
    /** @return {@inheritDoc} */
    @Override public AtomID copy() {return new AtomID(this);}
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #hasID()
     * @see #index()
     */
    @Override public int id() {return mID;}
    /** @return {@inheritDoc} */
    @Override public boolean hasID() {return true;}
    /**
     * {@inheritDoc}
     * @param aID {@inheritDoc}
     * @return {@inheritDoc}
     * @see #hasID()
     */
    @Override public AtomID setID(int aID) {mID = aID; return this;}
}
