package jse.atom;

/**
 * 增加一个中间层来统一 {@link #toString()} 方法的实现
 * @see IBond
 * @author liqa
 */
public abstract class AbstractBond implements IBond {
    /** @return 此原子键的字符串表示 */
    @Override public String toString() {
        if (hasID()) {
            return String.format("{id: %d, type: %d, bond_index: %d}", id(), type(), bondIndex());
        } else {
            return String.format("{type: %d, bond_index: %d}", type(), bondIndex());
        }
    }
}
