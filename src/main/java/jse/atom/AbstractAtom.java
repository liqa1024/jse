package jse.atom;

/**
 * 增加一个中间层来统一 {@code toString()} 方法的实现
 * @author liqa
 */
public abstract class AbstractAtom implements IAtom {
    /** print */
    @Override public String toString() {
        return hasVelocity() ?
            String.format("{id: %d, type: %d, xyz: (%.4g, %.4g, %.4g), vxvyvz: (%.4g, %.4g, %.4g)}", id(), type(), x(), y(), z(), vx(), vy(), vz()) :
            String.format("{id: %d, type: %d, xyz: (%.4g, %.4g, %.4g)}", id(), type(), x(), y(), z())
            ;
    }
    public Atom copy() {return hasVelocity() ? new AtomFull(this) : new Atom(this);}
}
