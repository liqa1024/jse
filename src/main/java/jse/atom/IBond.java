package jse.atom;

import org.jetbrains.annotations.ApiStatus;

/**
 * 通用的键接口，通过 {@link #bondIndex()} 获取连接的原子索引。
 * <p>
 * 和 lammps 不同，这里的键都会直接连接原子的索引本身，而不考虑其 id，因此改变
 * id 不会改变键的连接情况。
 * <p>
 * {@code id} 和 {@code type} 和 lammps 保持一致从 {@code 1} 开始索引
 *
 * @author liqa
 */
@ApiStatus.Experimental
public interface IBond {
    /** @return 此键的种类编号，从 1 开始 (对应 lammps 中的 bond type) */
    int type();
    
    /**
     * 获取键的 id 信息 (对应 lammps 中的原子 id)。
     * <p>
     * 可能存在 bond 不包含 id 信息 (甚至是大多数情况)，则会直接返回 {@code -1}。
     *
     * @return 此键的 id，从 1 开始
     * @see #hasID()
     */
    default int id() {
        if (hasID()) throw new IllegalStateException();
        return -1;
    }
    /** @return 此键是否真实包含 id 信息 */
    default boolean hasID() {return false;}
    
    /** @return 此键连接的原子索引 */
    int bondIndex();
    /** @return 此键连接的原子 */
    IAtom bondAtom();
}
