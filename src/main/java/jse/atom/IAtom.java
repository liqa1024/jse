package jse.atom;

import jep.NDArray;
import jse.code.CS;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 通用的原子接口，通过 {@link #x()}, {@link #y()},
 * {@link #z()} 来获取具体坐标值；通过 {@link #id()}
 * 来获取原子的 id，{@link #type()} 来获取原子的种类编号。
 * <p>
 * {@code id} 和 {@code type} 和 lammps 保持一致从 {@code 1} 开始索引
 * <p>
 * 继承 {@link IXYZ} 从而自动可以使用三维坐标的相关运算
 *
 * @see IXYZ IXYZ: 通用的三维坐标接口
 * @see ISettableAtom ISettableAtom: 可以修改的原子接口
 * @see Atom Atom: 一般的原子实现
 * @see AtomID AtomID: 包含 id 的原子实现
 * @see AtomFull AtomFull: 包含速度信息的原子实现
 * @author liqa
 */
public interface IAtom extends IXYZ {
    /** @return 此原子的 x 坐标值 */
    @Override double x();
    /** @return 此原子的 y 坐标值 */
    @Override double y();
    /** @return 此原子的 z 坐标值 */
    @Override double z();
    
    /**
     * 获取原子的 id 信息 (对应 lammps 中的原子 id)。
     * <p>
     * 可能存在原子不包含 id 信息 (甚至是大多数情况)，则会尝试返回
     * {@code index()+1}，而当索引 {@link #index()} 也不存在时则会返回
     * {@code -1}。
     * <p>
     * 现在 jse 的一般实现原子 {@link Atom} 统一不再包含 id
     * 信息，因此会返回 {@code -1}；而在原子数据 {@link IAtomData} 中的原子由于索引
     * {@link #index()} 总是合法，因此会返回 {@code >= 1} 的值。
     * 可以根据 {@link #hasID()} 查看此原子是否真的包含 id 信息
     * <p>
     * 可以使用 {@link AtomID} 或者 {@link AtomFull} 来创建一个包含 id 信息的原子
     *
     * @return 此原子的 id，从 1 开始
     * @see #hasID()
     * @see #index()
     * @see AtomID
     */
    default int id() {return -1;}
    /** @return 此原子是否真实包含 id 信息 */
    default boolean hasID() {return false;}
    
    /**
     * @return 此原子连接的键的信息，如果不存在则会返回
     * {@code null}；jse 实现统一会保证键连接是完全的，即
     * {@code i-j} 键对于 {@code i} 和 {@code j} 统一都会包含
     *
     * @see #hasBond()
     */
    default List<? extends IBond> bonds() {return null;}
    /** @return 此原子是否包含键信息 */
    default boolean hasBond() {return false;}
    
    /** @return 此原子的种类编号，从 1 开始 (对应 lammps 中的原子 type) */
    int type();
    /**
     * @return 此原子在 {@link IAtomData} 中的索引位置，如果不存在对应原子数据则会返回 {@code -1}
     * @see IAtomData
     */
    default int index() {return -1;}
    /** @return 此原子是否包含索引信息 */
    default boolean hasIndex() {return index()>=0;}
    
    /** @return {@inheritDoc} */
    @Override ISettableAtom copy();
    
    /**
     * @return 此原子的 x 方向速度值，如果不存在速度则会返回 {@code 0.0}
     * @see #hasVelocity()
     * @see AtomFull AtomFull: 包含速度信息的原子
     */
    default double vx() {return 0.0;}
    /**
     * @return 此原子的 y 方向速度值，如果不存在速度则会返回 {@code 0.0}
     * @see #hasVelocity()
     * @see AtomFull AtomFull: 包含速度信息的原子
     */
    default double vy() {return 0.0;}
    /**
     * @return 此原子的 z 方向速度值，如果不存在速度则会返回 {@code 0.0}
     * @see #hasVelocity()
     * @see AtomFull AtomFull: 包含速度信息的原子
     */
    default double vz() {return 0.0;}
    /**
     * @return 此原子三个方向的速度值组成的 {@link IXYZ} 对象，如果不存在速度则会返回 {@code [0.0, 0.0, 0.0]}
     * @see #hasVelocity()
     * @see IXYZ
     * @see AtomFull AtomFull: 包含速度信息的原子
     */
    default IXYZ vxyz() {return new XYZ(vx(), vy(), vz());}
    /** @return 此原子是否真实包含速度信息 */
    default boolean hasVelocity() {return false;}
    /** @deprecated use {@link #hasVelocity()} */
    @Deprecated default boolean hasVelocities() {return hasVelocity();}
    
    /**
     * @return 此原子的元素符号，如果不存在元素符号信息则会返回
     * {@code null}；目前 jse 默认实现原子 ({@link Atom}, {@link AtomID}, {@link AtomFull})
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
     * {@link Double#NaN}；目前 jse 默认实现原子 ({@link Atom}, {@link AtomID}, {@link AtomFull})
     * 不会包含元素符号信息以及质量信息，因此只有在原子数据
     * {@link IAtomData} 中获取到的原子才有可能包含此信息
     *
     * @see #hasMass()
     * @see IAtomData#masses()
     */
    default double mass() {return Double.NaN;}
    /** @return 此原子是否包含质量信息 */
    default boolean hasMass() {return false;}
    
    
    /// numpy stuffs
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 顺序为 {@code x, y, z, id, type, vx, vy, vz} 的 {@link NDArray}
     * @see IAtomData#numpy()
     * @see CS#ATOM_DATA_KEYS
     */
    @Override default NDArray<double[]> numpy() {return new NDArray<>(data(), 8);}
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 顺序为 {@code x, y, z} 的 {@link NDArray}
     * @see IAtomData#numpyXYZ()
     * @see CS#ATOM_DATA_KEYS_XYZ
     */
    default NDArray<double[]> numpyXYZ() {return new NDArray<>(dataXYZ(), 3);}
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 顺序为 {@code x, y, z, id} 的 {@link NDArray}
     * @see IAtomData#numpyXYZID()
     * @see CS#ATOM_DATA_KEYS_XYZID
     */
    default NDArray<double[]> numpyXYZID() {return new NDArray<>(dataXYZID(), 4);}
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 顺序为 {@code id, type, x, y, z} 的 {@link NDArray}
     * @see IAtomData#numpySTD()
     * @see CS#STD_ATOM_DATA_KEYS
     */
    default NDArray<double[]> numpySTD() {return new NDArray<>(dataSTD(), 5);}
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 顺序为 {@code id, type, x, y, z, vx, vy, vz} 的 {@link NDArray}
     * @see IAtomData#numpyAll()
     * @see CS#ALL_ATOM_DATA_KEYS
     */
    default NDArray<double[]> numpyAll() {return new NDArray<>(dataAll(), 8);}
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 顺序为 {@code vx, vy, vz} 的 {@link NDArray}
     * @see IAtomData#numpyVelocities()
     * @see CS#ATOM_DATA_KEYS_VELOCITY
     */
    default NDArray<double[]> numpyVelocities() {return new NDArray<>(dataVelocities(), 3);}
    
    
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
