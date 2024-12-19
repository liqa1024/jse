package jse.atom;

/**
 * 包含更多信息的原子实现，目前包含 {@code x, y, z, id, type, vx, vy, vz}。
 * <p>
 * 目前只提供创建后赋值的方法来构造：
 * <pre> {@code
 * def atom = new AtomFull()
 * atom.setX(x).setY(y).setZ(z)
 * atom.setVx(vx).setVy(vy).setVy(vy)
 * } </pre>
 * <p>
 * 除了通用的 {@link #x()}, {@link #y()}, {@link #z()}, {@link #id()}, {@link #type()},
 * {@link #vx()}, {@link #vy()}, {@link #vz()} 方法来获取属性，还可以通过直接访问成员变量
 * {@link #mX}, {@link #mY}, {@link #mZ}, {@link #mID}, {@link #mType},
 * {@link #mVx}, {@link #mVy}, {@link #mVz} 来获取以及修改。
 *
 * @see IAtom IAtom: 通用的原子接口
 * @see ISettableAtom ISettableAtom: 可设置原子属性的原子接口
 * @see Atom Atom: 一般的原子实现
 * @see AtomID AtomID: 包含 id 信息的原子实现
 * @author liqa
 */
public class AtomFull extends AtomID {
    public double mVx, mVy, mVz;
    /**
     * 直接通过一个原子创建一个新的原子对象
     * @param aAtom 已有的任意原子对象
     * @see IAtom
     */
    public AtomFull(IAtom aAtom) {
        super(aAtom);
        mVx = aAtom.vx();
        mVy = aAtom.vy();
        mVz = aAtom.vz();
    }
    /**
     * 创建一个默认的原子对象，{@code id==-1, type==1}，
     * 其余参数都为 {@code 0.0}
     */
    public AtomFull() {
        super();
        mVx = 0.0; mVy = 0.0; mVz = 0.0;
    }
    /** @return {@inheritDoc} */
    @Override public AtomFull copy() {return new AtomFull(this);}
    
    /** @return {@inheritDoc} */
    @Override public double vx() {return mVx;}
    /** @return {@inheritDoc} */
    @Override public double vy() {return mVy;}
    /** @return {@inheritDoc} */
    @Override public double vz() {return mVz;}
    /** @return {@inheritDoc} */
    @Override public boolean hasVelocity() {return true;}
    
    /**
     * {@inheritDoc}
     * @param aVx {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AtomFull setVx(double aVx) {mVx = aVx; return this;}
    /**
     * {@inheritDoc}
     * @param aVy {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AtomFull setVy(double aVy) {mVy = aVy; return this;}
    /**
     * {@inheritDoc}
     * @param aVz {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AtomFull setVz(double aVz) {mVz = aVz; return this;}
    /**
     * {@inheritDoc}
     * @param aVx {@inheritDoc}
     * @param aVy {@inheritDoc}
     * @param aVz {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AtomFull setVxyz(double aVx, double aVy, double aVz) {mVx = aVx; mVy = aVy; mVz = aVz; return this;}
    /**
     * {@inheritDoc}
     * @param aVxyz {@inheritDoc}
     * @return {@inheritDoc}
     * @see IXYZ
     */
    @Override public AtomFull setVxyz(IXYZ aVxyz) {return setVxyz(aVxyz.x(), aVxyz.y(), aVxyz.z());}
}
