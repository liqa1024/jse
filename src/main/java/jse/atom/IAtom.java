package jse.atom;

import jse.code.CS;
import org.jetbrains.annotations.Nullable;

/**
 * 通用的原子接口，通过 {@link #x()}, {@link #y()},
 * {@link #z()} 来获取具体坐标值；通过 {@link #id()}
 * 来获取原子的 {@code id}，{@link #type()} 来获取原子的种类编号。
 * <p>
 * {@code id} 和 {@code type} 和 lammps 保持一致从 {@code 1} 开始索引
 * <p>
 * 继承 {@link IXYZ} 从而自动可以使用三维坐标的相关运算
 *
 * @see IXYZ IXYZ: 通用的三维坐标接口
 * @see ISettableAtom ISettableAtom: 可以修改的原子接口
 * @see Atom Atom: 一般的原子实现
 * @see AtomFull AtomFull: 包含速度信息的原子实现
 * @author liqa
 */
public interface IAtom extends IXYZ {
    /** @return 此原子的 x 坐标值 */
    double x();
    /** @return 此原子的 y 坐标值 */
    double y();
    /** @return 此原子的 z 坐标值 */
    double z();
    /** @return 此原子的 id，从 1 开始 (对应 lammps 中的原子 id) */
    int id();
    /** @return 此原子的种类编号，从 1 开始 (对应 lammps 中的原子 type) */
    int type();
    /**
     * @return 此原子在 {@link IAtomData} 中的索引位置，如果不存在对应原子数据则会返回 {@code -1}
     * @see IAtomData
     */
    default int index() {return -1;}
    
    /** @return {@inheritDoc} */
    IAtom copy();
    
    /**
     * @return 此原子的 x 方向速度值，如果不存在速度则会返回 {@code 0.0}
     * @see #hasVelocity()
     */
    default double vx() {return 0.0;}
    /**
     * @return 此原子的 y 方向速度值，如果不存在速度则会返回 {@code 0.0}
     * @see #hasVelocity()
     */
    default double vy() {return 0.0;}
    /**
     * @return 此原子的 z 方向速度值，如果不存在速度则会返回 {@code 0.0}
     * @see #hasVelocity()
     */
    default double vz() {return 0.0;}
    /**
     * @return 此原子三个方向的速度值组成的 {@link IXYZ} 对象，如果不存在速度则会返回 {@code [0.0, 0.0, 0.0]}
     * @see #hasVelocity()
     * @see IXYZ
     */
    default IXYZ vxyz() {return new XYZ(vx(), vy(), vz());}
    /** @return 此原子是否真实包含速度信息 */
    default boolean hasVelocity() {return false;}
    /** @deprecated use {@link #hasVelocity()} */
    @Deprecated default boolean hasVelocities() {return hasVelocity();}
    
    /**
     * @return 此原子的元素符号，如果不存在元素符号信息则会返回
     * {@code null}；目前 jse 默认实现原子 ({@link Atom}, {@link AtomFull})
     * 不会包含元素符号信息以及质量信息，因此只有在原子数据
     * {@link IAtomData} 中获取到的原子才有可能包含此信息
     *
     * @see #hasSymbol()
     * @see IAtomData#symbols()
     */
    default @Nullable String symbol() {return null;}
    /** @return 此原子是否包含元素符号信息 */
    default boolean hasSymbol() {return false;}
    /**
     * @return 此原子的质量，如果不存在质量信息则会返回
     * {@link Double#NaN}；目前 jse 默认实现原子 ({@link Atom}, {@link AtomFull})
     * 不会包含元素符号信息以及质量信息，因此只有在原子数据
     * {@link IAtomData} 中获取到的原子才有可能包含此信息
     *
     * @see #hasMass()
     * @see IAtomData#masses()
     */
    default double mass() {return Double.NaN;}
    /** @return 此原子是否包含质量信息 */
    default boolean hasMass() {return false;}
    
    
    /// data stuffs
    /**
     * 直接获取 {@code double[]} 数据，会进行一次值拷贝
     * @return 顺序为 {@code x, y, z, id, type, vx, vy, vz} 的 {@code double[]} 数组
     * @see IAtomData#data()
     * @see CS#ATOM_DATA_KEYS
     */
    @Override default double[] data() {return new double[] {x(), y(), z(), id(), type(), vx(), vy(), vz()};}
    /**
     * 直接获取 {@code double[]} 数据，会进行一次值拷贝
     * @return 顺序为 {@code x, y, z} 的 {@code double[]} 数组
     * @see IAtomData#dataXYZ()
     * @see CS#ATOM_DATA_KEYS_XYZ
     */
    default double[] dataXYZ() {return new double[] {x(), y(), z()};}
    /**
     * 直接获取 {@code double[]} 数据，会进行一次值拷贝
     * @return 顺序为 {@code x, y, z, id} 的 {@code double[]} 数组
     * @see IAtomData#dataXYZID()
     * @see CS#ATOM_DATA_KEYS_XYZID
     */
    default double[] dataXYZID() {return new double[] {x(), y(), z(), id()};}
    /**
     * 直接获取 {@code double[]} 数据，会进行一次值拷贝
     * @return 顺序为 {@code id, type, x, y, z} 的 {@code double[]} 数组
     * @see IAtomData#dataSTD()
     * @see CS#STD_ATOM_DATA_KEYS
     */
    default double[] dataSTD() {return new double[] {id(), type(), x(), y(), z()};}
    /**
     * 直接获取 {@code double[]} 数据，会进行一次值拷贝
     * @return 顺序为 {@code id, type, x, y, z, vx, vy, vz} 的 {@code double[]} 数组
     * @see IAtomData#dataAll()
     * @see CS#ALL_ATOM_DATA_KEYS
     */
    default double[] dataAll() {return new double[] {id(), type(), x(), y(), z(), vx(), vy(), vz()};}
    /**
     * 直接获取 {@code double[]} 数据，会进行一次值拷贝
     * @return 顺序为 {@code vx, vy, vz} 的 {@code double[]} 数组
     * @see IAtomData#dataVelocities()
     * @see CS#ATOM_DATA_KEYS_VELOCITY
     */
    default double[] dataVelocities() {return new double[] {vx(), vy(), vz()};}
}
