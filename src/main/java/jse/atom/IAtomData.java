package jse.atom;

import jep.NDArray;
import jse.code.CS;
import jse.math.vector.IVector;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;

/**
 * 通用的原子数据接口，此接口只开放了访问接口，没开放修改接口
 * <p>
 * 一般来说，通过 {@link #atoms()} 来获取内部原子组成的列表，通过 {@link #atom(int)}
 * 来获取指定索引的原子，有关系 {@code atoms()[i] == atom(i)}
 * <p>
 * jse 的原子原则上采用 lammps 的类似定义，即除了原子的 {@code xyz} 坐标外，还包含一个原子的 {@code id}
 * 以及原子的种类编号 {@code type}，其中 {@code id} 和 {@code type} 和 lammps
 * 保持一致也从 {@code 1} 开始索引（保证读取 lammps data 文件后获取的结果一致）；{@code id}
 * 可以有任意的顺序，并且可以不连续，而 {@code type} 则会保证 {@code data.atom(i).type() <= data.ntypes()}
 * <p>
 * 注意区分原子本身的 {@code id} 以及原子处于原子数据的位置索引
 * {@code idx}，对于没有 {@code id} 的原子数据则会采用 {@code idx+1} 作为 id
 * <p>
 * jse 的原子数据统一在一个从原点开始的模拟盒内，因此一般来说不会出现坐标为负值的原子；
 * 对于存在下边界模拟盒的原子数据（例如 lammps 的 data），这里获取到的坐标会自动减去这个下边界。
 * <p>
 * 一般情况下，jse 的原子数据不会进行单位转换，也就是会统一保留读取的数据的原始值
 *
 * @see ISettableAtomData ISettableAtomData: 对于可以修改的原子数据
 * @see AtomData AtomData: 对于原子数据的默认实现
 * @see SettableAtomData SettableAtomData: 对于可修改的原子数据的默认实现
 * @see IAtom IAtom: 对于 jse 中的原子定义
 * @see IBox IBox: 对于 jse 中的模拟盒定义
 * @author liqa
 */
public interface IAtomData extends IHasSymbol {
    /**
     * @return 内部原子引用组成的列表
     * @see IAtom
     */
    List<? extends IAtom> atoms();
    /** @deprecated use {@link #atoms()} */
    @Deprecated default List<? extends IAtom> asList() {return atoms();}
    /**
     * 直接获取指定索引的原子，可以避免一次创建匿名列表的过程；
     * 有关系 {@code atoms()[i] == atom(i)}
     * @param aIdx 需要获取原子的索引
     * @return 对应索引的原子引用
     * @see IAtom
     */
    IAtom atom(int aIdx);
    
    /** @return 原子总数 */
    int atomNumber();
    /** @see #atomNumber() */
    @VisibleForTesting default int natoms() {return atomNumber();}
    /** @deprecated use {@link #atomNumber()} or {@link #natoms()} */
    @Deprecated default int atomNum() {return atomNumber();}
    
    /** @return {@inheritDoc}，可以大于实际真实包含的原子种类数目 */
    @Override int atomTypeNumber();
    /** @see #atomTypeNumber() */
    @VisibleForTesting default int ntypes() {return atomTypeNumber();}
    /** @deprecated use {@link #atomTypeNumber()} or {@link #ntypes()} */
    @Deprecated default int atomTypeNum() {return atomTypeNumber();}
    
    /**
     * @return 原子键种类的总数，可以大于实际真实包含的原子键种类数目，当不存在键信息时返回 {@code 0}
     * @see #hasBond()
     */
    int bondTypeNumber();
    
    /**
     * @return 此原子数据对应的模拟盒
     * @see IBox
     */
    IBox box();
    /**
     * @return 此原子数据模拟盒的体系
     * @see IBox#volume()
     */
    default double volume() {return box().volume();}
    /**
     * @return 此原子数据模拟盒是否是三斜的；
     * 这只是一个模拟盒类型检测，即可能存在模拟盒类型是三斜的，但是非对角项都为
     * 0，此时依旧会返回 {@code true}
     * @see IBox#isPrism()
     */
    default boolean isPrism() {return box().isPrism();}
    /**
     * @return 此原子数据模拟盒是否是下三角的（满足 lammps 的风格）；
     * 这只是一个模拟盒类型检测，即可能存在模拟盒类型不是 lammps 风格的，但是上三角非对焦元都为
     * 0，此时依旧会返回 {@code false}
     * @see IBox#isLmpStyle()
     */
    default boolean isLmpStyle() {return box().isLmpStyle();}
    
    /**
     * @return 此原子数据是否真实包含 id 信息
     * @see IAtom#hasID()
     */
    boolean hasID();
    /**
     * @return 此原子数据是否包含键信息
     * @see IAtom#hasBond()
     */
    boolean hasBond();
    /**
     * @return 此原子数据是否包含键的 id 信息
     * @see IBond#hasID()
     */
    boolean hasBondID();
    /**
     * @return 此原子数据是否真实包含速度信息
     * @see IAtom#hasVelocity()
     */
    boolean hasVelocity();
    /** @deprecated use {@link #hasVelocity()} */
    @Deprecated default boolean hasVelocities() {return hasVelocity();}
    
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasSymbol()
     */
    @Override boolean hasSymbol();
    /**
     * @return {@inheritDoc}
     * @see IAtom#symbol()
     */
    @Override default @Nullable List<@Nullable String> symbols() {return IHasSymbol.super.symbols();}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#symbol()
     * @see IAtom#type()
     * @see #hasSymbol()
     */
    @Override @Nullable String symbol(int aType);
    
    /**
     * @return 此原子数据是否包含质量信息
     * @see IAtom#hasMass()
     */
    boolean hasMass();
    /**
     * @return 按照原子种类编号排序的质量向量，有
     * {@code masses().size() == ntypes()}；
     * 如果不存在质量信息则会返回 {@code null}
     * @see IVector
     * @see IAtom#mass()
     * @see #hasMass()
     */
    @Nullable IVector masses();
    /**
     * 直接获取指定种类编号下的质量，可以避免一次创建匿名列表的过程；
     * 有关系 {@code mass(type) == masses()[type-1]}
     * @param aType 需要查询的种类编号，从 {@code 1} 开始
     * @return 指定种类编号的质量，如果不存在质量信息则会返回 {@link Double#NaN}
     * @see IAtom#mass()
     * @see IAtom#type()
     * @see #hasMass()
     */
    double mass(int aType);
    
    /** @return 此原子数据的拷贝，拷贝结果都是可以修改的 */
    ISettableAtomData copy();
    
    /**
     * 获取原子数据的计算器，包含许多较为复杂的原子数据操作
     * <p>
     * 例如对当前原子数据进行扩胞：
     * <pre> {@code
     * def repeatData = data.operation().repeat(2, 3, 4)
     * } </pre>
     * @return 原子数据计算器
     * @see IAtomDataOperation
     * @see ISettableAtomDataOperation
     */
    IAtomDataOperation operation();
    /** @see #operation() */
    @VisibleForTesting default IAtomDataOperation op() {return operation();}
    /** @deprecated use {@link #op()} */
    @VisibleForTesting @Deprecated default IAtomDataOperation opt() {return operation();}
    
    
    /// numpy stuffs
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code x, y, z, id, type, vx, vy, vz}
     * @see IAtom#numpy()
     * @see CS#ATOM_DATA_KEYS
     */
    NDArray<double[]> numpy();
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code x, y, z}
     * @see IAtom#numpyXYZ()
     * @see CS#ATOM_DATA_KEYS_XYZ
     */
    NDArray<double[]> numpyXYZ();
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code x, y, z, id}
     * @see IAtom#numpyXYZID()
     * @see CS#ATOM_DATA_KEYS_XYZID
     */
    NDArray<double[]> numpyXYZID();
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code id, type, x, y, z}
     * @see IAtom#numpySTD()
     * @see CS#STD_ATOM_DATA_KEYS
     */
    NDArray<double[]> numpySTD();
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code id, type, x, y, z, vx, vy, vz}
     * @see IAtom#numpyAll()
     * @see CS#ALL_ATOM_DATA_KEYS
     */
    NDArray<double[]> numpyAll();
    /**
     * 直接获取 numpy 的 {@link NDArray} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code vx, vy, vz}
     * @see IAtom#numpyVelocities()
     * @see CS#ATOM_DATA_KEYS_VELOCITY
     */
    NDArray<double[]> numpyVelocities();
    
    
    /// data stuffs
    /**
     * 直接获取 {@code double[][]} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code x, y, z, id, type, vx, vy, vz}
     * @see IAtom#data()
     * @see CS#ATOM_DATA_KEYS
     */
    double[][] data();
    /**
     * 直接获取 {@code double[][]} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code x, y, z}
     * @see IAtom#dataXYZ()
     * @see CS#ATOM_DATA_KEYS_XYZ
     */
    double[][] dataXYZ();
    /**
     * 直接获取 {@code double[][]} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code x, y, z, id}
     * @see IAtom#dataXYZID()
     * @see CS#ATOM_DATA_KEYS_XYZID
     */
    double[][] dataXYZID();
    /**
     * 直接获取 {@code double[][]} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code id, type, x, y, z}
     * @see IAtom#dataSTD()
     * @see CS#STD_ATOM_DATA_KEYS
     */
    double[][] dataSTD();
    /**
     * 直接获取 {@code double[][]} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code id, type, x, y, z, vx, vy, vz}
     * @see IAtom#dataAll()
     * @see CS#ALL_ATOM_DATA_KEYS
     */
    double[][] dataAll();
    /**
     * 直接获取 {@code double[][]} 数据，会进行一次值拷贝
     * @return 按行排列的数组，每行对应一个原子，
     * 顺序为 {@code vx, vy, vz}
     * @see IAtom#dataVelocities()
     * @see CS#ATOM_DATA_KEYS_VELOCITY
     */
    double[][] dataVelocities();
    
    
    /** @deprecated use {@link AtomicParameterCalculator#of}*/ @Deprecated default AtomicParameterCalculator getTypeMonatomicParameterCalculator(int aType, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return AtomicParameterCalculator.of(operation().filterType(aType), aThreadNum);}
    /** @deprecated use {@link AtomicParameterCalculator#of}*/ @Deprecated default AtomicParameterCalculator getMonatomicParameterCalculator() {return AtomicParameterCalculator.of(this);}
    /** @deprecated use {@link AtomicParameterCalculator#of}*/ @Deprecated default AtomicParameterCalculator getMonatomicParameterCalculator(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return AtomicParameterCalculator.of(this                         , aThreadNum);}
    /** @deprecated use {@link AtomicParameterCalculator#of}*/ @Deprecated default AtomicParameterCalculator getTypeMonatomicParameterCalculator(int aType) {return AtomicParameterCalculator.of(operation().filterType(aType));}
    /** @deprecated use {@link APC#of}*/ @Deprecated @VisibleForTesting default AtomicParameterCalculator getMPC() {return APC.of(this);}
    /** @deprecated use {@link APC#of}*/ @Deprecated @VisibleForTesting default AtomicParameterCalculator getMPC(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return APC.of(this                         , aThreadNum);}
    /** @deprecated use {@link APC#of}*/ @Deprecated @VisibleForTesting default AtomicParameterCalculator getTypeMPC(int aType) {return APC.of(operation().filterType(aType));}
    /** @deprecated use {@link APC#of}*/ @Deprecated @VisibleForTesting default AtomicParameterCalculator getTypeMPC(int aType, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum) {return APC.of(operation().filterType(aType), aThreadNum);}
}
