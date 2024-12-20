package jse.atom;

/**
 * 增加一个中间层来统一 {@link #toString()} 方法的实现
 * @see IAtom
 * @author liqa
 */
public abstract class AbstractAtom implements IAtom {
    /** @return 此原子的字符串表示，这里转换只保留 4 位有效数字（不影响实际精度）*/
    @Override public String toString() {
        if (hasID()) {
            return hasVelocity() ?
                String.format("{id: %d, type: %d, xyz: (%.4g, %.4g, %.4g), vxvyvz: (%.4g, %.4g, %.4g)}", id(), type(), x(), y(), z(), vx(), vy(), vz()) :
                String.format("{id: %d, type: %d, xyz: (%.4g, %.4g, %.4g)}", id(), type(), x(), y(), z())
                ;
        } else {
            return hasVelocity() ?
                String.format("{type: %d, xyz: (%.4g, %.4g, %.4g), vxvyvz: (%.4g, %.4g, %.4g)}", type(), x(), y(), z(), vx(), vy(), vz()) :
                String.format("{type: %d, xyz: (%.4g, %.4g, %.4g)}", type(), x(), y(), z())
                ;
        }
    }
    /** @return {@inheritDoc} */
    public ISettableAtom copy() {return hasVelocity() ? new AtomFull(this) : (hasID() ? new AtomID(this) : new Atom(this));}
}
