package jse.ase;

import jep.JepException;
import jep.NDArray;
import jep.python.PyCallable;
import jep.python.PyObject;
import jse.atom.*;
import jse.code.IO;
import jse.code.SP;
import jse.code.UT;
import jse.math.MathEX;
import jse.math.matrix.IIntMatrix;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import org.jetbrains.annotations.*;

import java.util.*;

import static jse.code.CS.*;
import static jse.code.CS.STD_VZ_COL;

/**
 * {@code ase.Atoms} 的 jse 实现，继承了 {@link IAtomData}
 * 用于保证使用方法和 jse 中的原子结构类型一致。
 * <p>
 * 此实现会对 {@code ase.Atoms} 中的 {@code info, arrays, calc.results}
 * 数据进行值拷贝，保证频繁访问下的效率。
 * <p>
 * 要求系统有 python 环境并且安装了
 * <a href="https://wiki.fysik.dtu.dk/ase/">
 * Atomic Simulation Environment (ASE) </a>
 * <p>
 * 通过：
 * <pre> {@code
 * def jseAtoms = AseAtoms.of(pyAtoms)
 * } </pre>
 * 来将 {@link PyObject} 的 ase atoms 转换成 jse 的原子数据，通过：
 * <pre> {@code
 * def pyAtoms = jseAtoms.toPyObject()
 * } </pre>
 * 来将此对象转换成 {@link PyObject} 的 ase atoms
 * <p>
 * 对于 ase atoms 中的特殊属性，通过：
 * <pre> {@code
 * def value = atoms.info('info_key')
 * atoms.setInfo('info_key', value)
 *
 * def value = atoms.array('array_key')
 * atoms.setArray('array_key', value)
 *
 * def value = atoms.calcResult('calc_key')
 * atoms.setCalcResult('calc_key', value)
 * } </pre>
 * 来分别进行访问和设置，一般来说支持格式 {@code double, int, boolean, String, IVector, IIntVector,
 * IMatrix, IIntMatrix, String[], String[][]}，数据对应维度需要和对应 key 和属性类型进行适配
 * <p>
 * 和 python 对象的相互转换会丢失 jse 未支持的属性，如果希望使用这些属性则应该直接通过
 * {@link PyObject} 来使用。除了 jse 中统一开启的 PBC 外，目前已经几乎涵盖所有其他属性。
 * <p>
 * 由于 ase 读取的 atoms 是带有单位的，因此在读取文件时会自动进行一次单位转换；这在和 jse
 * 原子数据进行转换时会出现问题，因此这里将 ase atoms 转换成 jse 类型时会按照默认的单位转换统一将这个转换变换回去。
 *
 * @see IAtomData IAtomData: 原子数据类型通用接口
 * @see PyObject
 * @see #read(String) read(String): 通过 ase 来读取指定路径的原子结构数据
 * @see #write(String) write(String): 通过 ase 来将原子结构数据写入指定路径
 * @see #of(IAtomData) of(IAtomData): 将任意的原子数据转换成 AseAtoms 类型
 * @author liqa
 */
public class AseAtoms extends AbstractSettableAtomData {
    /** 注意 ase 的值是带有单位的，而不是直接和文件保持一致，这其实很自作聪明；为了尽量保持一致，这里统一对速度做一个转换，保证默认情况下会一致 */
    public final static double ASE_VEL_MUL = 1.0 / UNITS_ASE.get("fs") * 1e-3;
    
    private final int mNumAtoms;
    private IBox mCell;
    private final @NotNull Map<String, Object> mInfos;
    private final @NotNull Map<String, Object> mArrays;
    private final @NotNull Map<String, Object> mCalcResults;
    
    private @Nullable IIntVector mAtomicNumbers = null;
    private @Nullable IMatrix mPositions = null;
    private @Nullable IMatrix mMomenta = null;
    /** 用于通过原子序数获取每个种类的粒子数 */
    private final @NotNull Map<Integer, Integer> mAtomicNumber2Type;
    private @Nullable IIntVector mType2AtomicNumber; // 第 0 个直接制空
    
    AseAtoms(int aNumAtoms, IBox aCell, @NotNull Map<String, Object> aInfos, @NotNull Map<String, Object> aArrays, @NotNull Map<String, Object> aCalcResults) {
        mNumAtoms = aNumAtoms;
        mCell = aCell;
        mInfos = aInfos;
        mArrays = aArrays;
        mCalcResults = aCalcResults;
        
        Object tAtomicNumbers = aArrays.get("numbers");
        if (tAtomicNumbers instanceof IIntVector) mAtomicNumbers = (IIntVector)tAtomicNumbers;
        Object tPositions = aArrays.get("positions");
        if ((tPositions instanceof IMatrix) && ((IMatrix)tPositions).ncols()==3) mPositions = (IMatrix)tPositions;
        Object tMomenta = aArrays.get("momenta");
        if ((tMomenta instanceof IMatrix) && ((IMatrix)tMomenta).ncols()==3) mMomenta = (IMatrix)tMomenta;
        
        mAtomicNumber2Type = new HashMap<>();
        if (mAtomicNumbers == null) {
            mType2AtomicNumber = null;
        } else {
            mAtomicNumbers.forEach(atomicNumber -> {
                if (!mAtomicNumber2Type.containsKey(atomicNumber)) {
                    int tType = mAtomicNumber2Type.size() + 1;
                    mAtomicNumber2Type.put(atomicNumber, tType);
                }
            });
            mType2AtomicNumber = IntVector.zeros(mAtomicNumber2Type.size()+1);
            for (Map.Entry<Integer, Integer> tEntry : mAtomicNumber2Type.entrySet()) {
                mType2AtomicNumber.set(tEntry.getValue(), tEntry.getKey());
            }
        }
    }
    
    /// 获取属性
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasVelocity()
     */
    @Override public boolean hasVelocity() {return mMomenta!=null;}
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasSymbol()
     */
    @Override public boolean hasSymbol() {return mAtomicNumbers!=null;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#symbol()
     * @see IAtom#type()
     * @see #hasSymbol()
     */
    @Override public String symbol(int aType) {
        if (mAtomicNumbers == null) return null;
        assert mType2AtomicNumber != null;
        return ATOMIC_NUMBER_TO_SYMBOL.get(mType2AtomicNumber.get(aType));
    }
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasMass()
     */
    @Override public boolean hasMass() {return hasSymbol();}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#mass()
     * @see IAtom#type()
     * @see #hasMass()
     */
    @Override public double mass(int aType) {
        @Nullable String tSymbol = symbol(aType);
        return tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
    }
    
    /// ase atoms 特有的接口
    /** @see #box() */
    @VisibleForTesting public IBox cell() {return box();}
    /** @see #atomicNumbers() */
    @VisibleForTesting public IIntVector numbers() {return atomicNumbers();}
    /**
     * 直接获取所有的原子序数，对应 ase atoms 中的 {@code numbers()} 方法
     * @return 原子序数组成的向量，按照原子索引排序
     * @see IIntVector
     */
    public IIntVector atomicNumbers() {
        return mAtomicNumbers;
    }
    /**
     * 直接获取所有的原子位置，对应 ase atoms 中的 {@code positions()} 方法
     * @return 原子坐标组成的矩阵，按照原子索引按行排列，每行为 {@code x, y, z}
     * @see IMatrix
     */
    public IMatrix positions() {
        return mPositions;
    }
    /**
     * 直接获取所有的原子动量，对应 ase atoms 中的 {@code momenta()} 方法
     * @return 原子动量组成的矩阵，按照原子索引按行排列，每行为 {@code px, py, pz}
     * @see IMatrix
     */
    public @Nullable IMatrix momenta() {
        return mMomenta;
    }
    /**
     * 判断是否有某个 ase atoms {@code info}
     * @param aKey 此 info 的名称，区分大小写
     * @return 是否存在这个 info 值
     * @see #info(String)
     * @see #setInfo(String, Object)
     * @see #removeInfo(String)
     */
    public boolean hasInfo(String aKey) {
        return mInfos.containsKey(aKey);
    }
    /**
     * 直接获取所有的 ase atoms {@code info} 组成的 {@link Map}，方便直接遍历，原则上不允许修改
     * @return info 参量名称和具体 info 值组成的 {@link Map}
     * @see #info(String)
     * @see #setInfo(String, Object)
     * @see #removeInfo(String)
     */
    public @Unmodifiable Map<String,Object> infos() {
        return mInfos;
    }
    /**
     * 获取某个 ase atoms {@code info}
     * @param aKey 需要的 info 的名称，区分大小写
     * @return 具体 info 值
     * @see #hasInfo(String)
     * @see #setInfo(String, Object)
     * @see #removeInfo(String)
     */
    public Object info(String aKey) {
        return mInfos.get(aKey);
    }
    /**
     * 设置某个 ase atoms {@code info}
     * @param aKey 需要设置的 info 的名称，区分大小写
     * @param aValue 需要设置的 info 值
     * @return 自身方便链式调用
     * @see #hasInfo(String)
     * @see #info(String)
     * @see #removeInfo(String)
     */
    public AseAtoms setInfo(String aKey, Object aValue) {
        if (aValue == null) {
            mInfos.remove(aKey);
            return this;
        }
        mInfos.put(aKey, aValue);
        return this;
    }
    /**
     * 移除某个 ase atoms {@code info}
     * @param aKey 需要移除的 info 的名称，区分大小写
     * @return 移除的 info 的值，如果没有则会返回 {@code null}
     * @see #hasInfo(String)
     * @see #info(String)
     * @see #setInfo(String, Object)
     * @see Map#remove(Object)
     */
    public Object removeInfo(String aKey) {
        return mInfos.remove(aKey);
    }
    /**
     * 判断是否有某个 ase atoms {@code arrays}
     * @param aKey 此 array 的名称，区分大小写
     * @return 是否存在这个 array 值
     * @see #array(String)
     * @see #setArray(String, Object)
     * @see #removeArray(String)
     */
    public boolean hasArray(String aKey) {
        return mArrays.containsKey(aKey);
    }
    /**
     * 直接获取所有的 ase atoms {@code arrays} 组成的 {@link Map}，方便直接遍历，原则上不允许修改
     * @return array 名称和具体 array 值组成的 {@link Map}
     * @see #array(String)
     * @see #setArray(String, Object)
     * @see #removeArray(String)
     */
    public @Unmodifiable Map<String,Object> arrays() {
        return mArrays;
    }
    /**
     * 获取某个 ase atoms {@code arrays}，一般来说类型只能是
     * {@code IVector, IIntVector, IMatrix, IIntMatrix, String[],
     * String[][]}，按照原子索引按行排列
     *
     * @param aKey 需要的 array 的名称，区分大小写
     * @return 具体属性值
     * @see #hasArray(String)
     * @see #setArray(String, Object)
     * @see #removeArray(String)
     */
    public Object array(String aKey) {
        return mArrays.get(aKey);
    }
    /**
     * 设置某个 ase atoms {@code arrays}，一般来说类型只能是
     * {@code IVector, IIntVector, IMatrix, IIntMatrix, String[],
     * String[][]}，按照原子索引按行排列
     * <p>
     * 对于 ase atoms 中特定的参量名称 {@code numbers, positions, momenta}
     * 严格限制维数和类型
     * <p>
     * 此设置不会进行值拷贝
     *
     * @param aKey 需要设置的 array 的名称，区分大小写
     * @param aValue 需要设置的 array 值
     * @return 自身方便链式调用
     * @see #hasArray(String)
     * @see #array(String)
     * @see #removeArray(String)
     */
    public AseAtoms setArray(String aKey, Object aValue) {
        if (aValue == null) {
            removeArray(aKey);
            return this;
        }
        switch(aKey) {
        case "positions": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for positions, input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            if (tValue.ncols() != ATOM_DATA_KEYS_XYZ.length) throw new IllegalArgumentException("Data ncols mismatch, need: "+ATOM_DATA_KEYS_XYZ.length+", input: "+tValue.ncols());
            mPositions = tValue;
            mArrays.put(aKey, tValue);
            return this;
        }
        case "numbers": {
            if (!(aValue instanceof IIntVector)) throw new IllegalArgumentException("Value type MUST be IntVector for numbers, input: "+aValue.getClass().getName());
            IIntVector tValue = (IIntVector)aValue;
            if (tValue.size() != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.size());
            mAtomicNumbers = tValue;
            // 这里直接重置原有的 symbol order
            mAtomicNumber2Type.clear();
            mAtomicNumbers.forEach(atomicNumber -> {
                if (!mAtomicNumber2Type.containsKey(atomicNumber)) {
                    int tType = mAtomicNumber2Type.size() + 1;
                    mAtomicNumber2Type.put(atomicNumber, tType);
                }
            });
            mType2AtomicNumber = IntVector.zeros(mAtomicNumber2Type.size()+1);
            for (Map.Entry<Integer, Integer> tEntry : mAtomicNumber2Type.entrySet()) {
                mType2AtomicNumber.set(tEntry.getValue(), tEntry.getKey());
            }
            mArrays.put(aKey, tValue);
            return this;
        }
        case "momenta": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for momenta, input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            if (tValue.ncols() != ATOM_DATA_KEYS_VELOCITY.length) throw new IllegalArgumentException("Data ncols mismatch, need: "+ATOM_DATA_KEYS_VELOCITY.length+", input: "+tValue.ncols());
            mMomenta = tValue;
            mArrays.put(aKey, tValue);
            return this;
        }}
        if (aValue instanceof IVector) {
            IVector tValue = (IVector)aValue;
            if (tValue.size() != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.size());
            mArrays.put(aKey, tValue);
        } else
        if (aValue instanceof IIntVector) {
            IIntVector tValue = (IIntVector)aValue;
            if (tValue.size() != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.size());
            mArrays.put(aKey, tValue);
        } else
        if (aValue instanceof IMatrix) {
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            mArrays.put(aKey, tValue);
        } else
        if (aValue instanceof IIntMatrix) {
            IIntMatrix tValue = (IIntMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            mArrays.put(aKey, tValue);
        } else
        if (aValue instanceof String[]) {
            String[] tValue = (String[])aValue;
            if (tValue.length != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.length);
            mArrays.put(aKey, tValue);
        } else
        if (aValue instanceof String[][]) {
            String[][] tValue = (String[][])aValue;
            if (tValue.length != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.length);
            mArrays.put(aKey, tValue);
        } else
        if (aValue instanceof List) {
            List<?> tValue = (List<?>)aValue;
            if (tValue.size() != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.size());
            mArrays.put(aKey, tValue);
        } else
        if ((aValue instanceof NDArray) || (aValue instanceof PyObject)) {
            mArrays.put(aKey, aValue);
        } else {
            throw new IllegalArgumentException("Invalid ase atoms array type, input: "+aValue.getClass().getName());
        }
        return this;
    }
    /**
     * 移除某个 ase atoms {@code arrays}
     * @param aKey 需要移除的 array 的名称，区分大小写
     * @return 移除的 array 的值，如果没有则会返回 {@code null}
     * @see #hasArray(String)
     * @see #array(String)
     * @see #setArray(String, Object)
     * @see Map#remove(Object)
     */
    @SuppressWarnings("UnusedReturnValue")
    public Object removeArray(String aKey) {
        switch(aKey) {
        case "positions": {
            mPositions = null;
            return mArrays.remove(aKey);
        }
        case "numbers": {
            mAtomicNumbers = null;
            mAtomicNumber2Type.clear();
            mType2AtomicNumber = null;
            return mArrays.remove(aKey);
        }
        case "momenta": {
            mMomenta = null;
            return mArrays.remove(aKey);
        }}
        return mArrays.remove(aKey);
    }
    /**
     * 判断是否有某个 ase atoms {@code calc.results}
     * @param aKey 此 calc result 的名称，区分大小写
     * @return 是否存在这个 calc result 值
     * @see #calcResult(String)
     * @see #setCalcResult(String, Object)
     * @see #removeCalcResult(String)
     */
    public boolean hasCalcResult(String aKey) {
        return mCalcResults.containsKey(aKey);
    }
    /**
     * 直接获取所有的 ase atoms {@code calc.results} 组成的 {@link Map}，方便直接遍历，原则上不允许修改
     * @return calc result 名称和具体 calc result 值组成的 {@link Map}
     * @see #calcResult(String)
     * @see #setCalcResult(String, Object)
     * @see #removeCalcResult(String)
     */
    public @Unmodifiable Map<String,Object> calcResults() {
        return mCalcResults;
    }
    /**
     * 获取某个 ase atoms {@code calc.results}，一般来说类型只能是
     * {@code double, IVector, IMatrix}，根据具体的计算结果类型决定
     *
     * @param aKey 需要的 calc result 的名称，区分大小写
     * @return 具体 calc result 值
     * @see #hasCalcResult(String)
     * @see #setCalcResult(String, Object)
     * @see #removeCalcResult(String)
     */
    public Object calcResult(String aKey) {
        return mCalcResults.get(aKey);
    }
    /**
     * 设置某个 ase atoms {@code calc.results}，一般来说类型只能是
     * {@code double, IVector, IMatrix}，根据具体的计算结果类型决定
     * <p>
     * 对于 ase calc 中部分特定的结果名称 {@code energy, energies, forces,
     * stress, stresses, charges, free_energy}
     * 会严格限制维数和类型
     * <p>
     * 此设置不会进行值拷贝
     *
     * @param aKey 需要设置的 calc result 的名称，区分大小写
     * @param aValue 需要设置的 calc result 值
     * @return 自身方便链式调用
     * @see #hasCalcResult(String)
     * @see #calcResult(String)
     * @see #removeCalcResult(String)
     */
    public AseAtoms setCalcResult(String aKey, Object aValue) {
        if (aValue == null) {
            removeCalcResult(aKey);
            return this;
        }
        switch(aKey) {
        case "energy": case "free_energy": {
            if (!(aValue instanceof Number)) throw new IllegalArgumentException("Value type MUST be Number for "+aKey+", input: "+aValue.getClass().getName());
            mCalcResults.put(aKey, aValue);
            return this;
        }
        case "energies": case "charges": {
            if (!(aValue instanceof IVector)) throw new IllegalArgumentException("Value type MUST be Vector for "+aKey+", input: "+aValue.getClass().getName());
            IVector tValue = (IVector)aValue;
            if (tValue.size() != mNumAtoms) throw new IllegalArgumentException("Data size mismatch, need: "+mNumAtoms+", input: "+tValue.size());
            mCalcResults.put(aKey, tValue);
            return this;
        }
        case "forces": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for "+aKey+", input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            if (tValue.ncols() != ATOM_DATA_KEYS_XYZ.length) throw new IllegalArgumentException("Data ncols mismatch, need: "+ATOM_DATA_KEYS_XYZ.length+", input: "+tValue.ncols());
            mCalcResults.put(aKey, tValue);
            return this;
        }
        case "stress": {
            if (!(aValue instanceof IVector)) throw new IllegalArgumentException("Value type MUST be Vector for "+aKey+", input: "+aValue.getClass().getName());
            IVector tValue = (IVector)aValue;
            if (tValue.size() != 6) throw new IllegalArgumentException("Data size mismatch, need: 6, input: "+tValue.size());
            mCalcResults.put(aKey, tValue);
            return this;
        }
        case "stresses": {
            if (!(aValue instanceof IMatrix)) throw new IllegalArgumentException("Value type MUST be Matrix for "+aKey+", input: "+aValue.getClass().getName());
            IMatrix tValue = (IMatrix)aValue;
            if (tValue.nrows() != mNumAtoms) throw new IllegalArgumentException("Data nrows mismatch, need: "+mNumAtoms+", input: "+tValue.nrows());
            if (tValue.ncols() != 6) throw new IllegalArgumentException("Data ncols mismatch, need: "+6+", input: "+tValue.ncols());
            mCalcResults.put(aKey, tValue);
            return this;
        }}
        mCalcResults.put(aKey, aValue);
        return this;
    }
    /**
     * 移除某个 ase atoms {@code calc.results}
     * @param aKey 需要移除的 calc result 的名称，区分大小写
     * @return 移除的 calc result 的值，如果没有则会返回 {@code null}
     * @see #hasCalcResult(String)
     * @see #calcResult(String)
     * @see #setCalcResult(String, Object)
     * @see Map#remove(Object)
     */
    @SuppressWarnings("UnusedReturnValue")
    public Object removeCalcResult(String aKey) {
        return mCalcResults.remove(aKey);
    }
    
    /**
     * 直接调整元素符号的顺序，由于 ase atoms 中是采用一列原子序数来存储种类信息的，
     * 因此并没有指明元素种类的编号顺序，在 jse 中默认会使用元素符号出现的顺序来排列种类编号，
     * 而如果需要手动设置特定的编号顺序则需要通过类似 {@code data.setSymbolOrder('Cu', 'Zr')}
     * 的方式设置顺序
     * @param aSymbolOrder 需要的元素符号顺序
     * @return 自身方便链式调用
     * @throws IllegalArgumentException 如果包含非法的元素符号
     * @see #setSymbols(String...)
     */
    public AseAtoms setSymbolOrder(String... aSymbolOrder) {
        if (mAtomicNumbers == null) throw new UnsupportedOperationException("`setSymbolOrder` for AseAtoms without atomic number numbers data");
        assert mType2AtomicNumber != null;
        if (aSymbolOrder == null) aSymbolOrder = ZL_STR;
        if (aSymbolOrder.length > ntypes()) {
            IIntVector oType2AtomicNumber = mType2AtomicNumber;
            mType2AtomicNumber = Vectors.range(aSymbolOrder.length+1);
            mType2AtomicNumber.subVec(0, oType2AtomicNumber.size()).fill(oType2AtomicNumber);
        }
        for (int tType = 1; tType <= aSymbolOrder.length; ++tType) {
            String tSymbol = aSymbolOrder[tType-1];
            @Nullable Integer tAtomicNumber = SYMBOL_TO_ATOMIC_NUMBER.get(tSymbol);
            if (tAtomicNumber == null) throw new IllegalArgumentException("symbol: " + tSymbol);
            mType2AtomicNumber.set(tType, tAtomicNumber);
        }
        // 先更新 mAtomicNumber2Type
        validAtomicNumber2Type_();
        // 遍历一次 mAtomicNumbers 确保 mType2AtomicNumber 全部覆盖
        mAtomicNumbers.forEach(atomicNumber -> {
            if (!mAtomicNumber2Type.containsKey(atomicNumber)) {
                int tType = mAtomicNumber2Type.size() + 1;
                mAtomicNumber2Type.put(atomicNumber, tType);
            }
        });
        // 如果有缺失需要补充
        if (mAtomicNumber2Type.size() > mType2AtomicNumber.size()-1) {
            mType2AtomicNumber = IntVector.zeros(mAtomicNumber2Type.size()+1);
            for (Map.Entry<Integer, Integer> tEntry : mAtomicNumber2Type.entrySet()) {
                mType2AtomicNumber.set(tEntry.getValue(), tEntry.getKey());
            }
        }
        return this;
    }
    
    /**
     * {@inheritDoc}
     * @param aSymbols {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IllegalArgumentException 如果包含非法的元素符号
     * @see #symbols()
     * @see IAtom#symbol()
     * @see #setSymbolOrder(String...)
     */
    @Override public AseAtoms setSymbols(String... aSymbols) {
        if (mAtomicNumbers == null) throw new UnsupportedOperationException("`setSymbols` for AseAtoms without atomic number numbers data");
        assert mType2AtomicNumber != null;
        if (aSymbols==null || aSymbols.length==0) return this;
        if (aSymbols.length > ntypes()) {
            IIntVector oType2AtomicNumber = mType2AtomicNumber;
            mType2AtomicNumber = Vectors.range(aSymbols.length+1);
            mType2AtomicNumber.subVec(0, oType2AtomicNumber.size()).fill(oType2AtomicNumber);
        }
        for (int tType = 1; tType <= aSymbols.length; ++tType) {
            String tSymbol = aSymbols[tType-1];
            @Nullable Integer tAtomicNumber = SYMBOL_TO_ATOMIC_NUMBER.get(tSymbol);
            if (tAtomicNumber == null) throw new IllegalArgumentException("symbol: " + tSymbol);
            mType2AtomicNumber.set(tType, tAtomicNumber);
        }
        // 更新 mAtomicNumbers，此时需要旧的 mAtomicNumber2Type
        mAtomicNumbers.op().map2this(i -> mType2AtomicNumber.get(mAtomicNumber2Type.get(i)));
        validAtomicNumber2Type_();
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aNumTypes {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException 如果不包含元素符号信息
     * @see #ntypes()
     * @see IAtom#type()
     */
    @Override public AseAtoms setNtypes(int aNumTypes) {
        if (mAtomicNumbers == null) throw new UnsupportedOperationException("`setNtypes` for AseAtoms without atomic number numbers data");
        assert mType2AtomicNumber != null;
        int oTypeNum = ntypes();
        if (aNumTypes == oTypeNum) return this;
        if (aNumTypes < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            mAtomicNumbers.op().map2this(i -> {
                int tType = mAtomicNumber2Type.get(i);
                return tType> aNumTypes ? mType2AtomicNumber.get(aNumTypes) : i;
            });
            mType2AtomicNumber = mType2AtomicNumber.subVec(0, aNumTypes +1).copy();
            validAtomicNumber2Type_();
            return this;
        }
        IIntVector oType2AtomicNumber = mType2AtomicNumber;
        mType2AtomicNumber = Vectors.range(aNumTypes +1);
        mType2AtomicNumber.subVec(0, oTypeNum).fill(oType2AtomicNumber);
        validAtomicNumber2Type_();
        return this;
    }
    void validAtomicNumber2Type_() {
        assert mType2AtomicNumber != null;
        mAtomicNumber2Type.clear();
        int tAtomTypeNumber = ntypes();
        for (int tType = 1; tType <= tAtomTypeNumber; ++tType) {
            mAtomicNumber2Type.put(mType2AtomicNumber.get(tType), tType);
        }
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #hasVelocity()
     * @see #setHasVelocity()
     */
    @Override public AseAtoms setNoVelocity() {
        mMomenta = null;
        mArrays.remove("momenta");
        return this;
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #hasVelocity()
     * @see #setNoVelocity()
     */
    @Override public AseAtoms setHasVelocity() {
        if (mMomenta == null) {
            mMomenta = RowMatrix.zeros(this.natoms(), ATOM_DATA_KEYS_VELOCITY.length);
            mArrays.put("velo", mMomenta);
        }
        return this;
    }
    
    /// set box stuff
    @Override protected void setBox_(double aX, double aY, double aZ) {
        mCell = new Box(aX, aY, aZ);
    }
    @Override protected void setBox_(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        mCell = new BoxPrism(aAx, aAy, aAz, aBx, aBy, aBz, aCx, aCy, aCz);
    }
    @Override protected void scaleAtomPosition_(boolean aKeepAtomPosition, double aScale) {
        if (aKeepAtomPosition) return;
        if (mPositions != null) {
            mPositions.multiply2this(aScale);
        }
        if (mMomenta != null) {
            mMomenta.multiply2this(aScale);
        }
    }
    @Override protected void validAtomPosition_(boolean aKeepAtomPosition, IBox aOldBox) {
        if (aKeepAtomPosition) return;
        final int tAtomNum = this.natoms();
        XYZ tBuf = new XYZ();
        if (mCell.isPrism() || aOldBox.isPrism()) {
            for (int i = 0; i < tAtomNum; ++i) {
                if (mPositions != null) {
                    tBuf.setXYZ(mPositions.get(i, XYZ_X_COL), mPositions.get(i, XYZ_Y_COL), mPositions.get(i, XYZ_Z_COL));
                    // 这样转换两次即可实现线性变换
                    aOldBox.toDirect(tBuf);
                    mCell.toCartesian(tBuf);
                    mPositions.set(i, XYZ_X_COL, tBuf.mX);
                    mPositions.set(i, XYZ_Y_COL, tBuf.mY);
                    mPositions.set(i, XYZ_Z_COL, tBuf.mZ);
                }
                // 如果存在速度，则速度也需要做一次这样的变换
                if (mMomenta != null) {
                    tBuf.setXYZ(mMomenta.get(i, STD_VX_COL), mMomenta.get(i, STD_VY_COL), mMomenta.get(i, STD_VZ_COL));
                    aOldBox.toDirect(tBuf);
                    mCell.toCartesian(tBuf);
                    mMomenta.set(i, STD_VX_COL, tBuf.mX);
                    mMomenta.set(i, STD_VY_COL, tBuf.mY);
                    mMomenta.set(i, STD_VZ_COL, tBuf.mZ);
                }
            }
        } else {
            if (mPositions != null) {
                tBuf.setXYZ(mCell);
                tBuf.div2this(aOldBox);
                mPositions.col(XYZ_X_COL).multiply2this(tBuf.mX);
                mPositions.col(XYZ_Y_COL).multiply2this(tBuf.mY);
                mPositions.col(XYZ_Z_COL).multiply2this(tBuf.mZ);
            }
            // 如果存在速度，则速度也需要做一次这样的变换
            if (mMomenta != null) {
                mMomenta.col(STD_VX_COL).multiply2this(tBuf.mX);
                mMomenta.col(STD_VY_COL).multiply2this(tBuf.mY);
                mMomenta.col(STD_VZ_COL).multiply2this(tBuf.mZ);
            }
        }
    }
    
    /// AbstractAtomData stuffs
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     * @see ISettableAtom
     * @see #atom(int)
     * @see #setAtom(int, IAtom)
     */
    @Override public ISettableAtom atom(final int aIdx) {
        return new AbstractSettableAtom_() {
            @Override public int index() {return aIdx;}
            @Override public double x() {
                if (mPositions == null) throw new UnsupportedOperationException("`x` for AseAtoms without position data");
                return mPositions.get(aIdx, XYZ_X_COL);
            }
            @Override public double y() {
                if (mPositions == null) throw new UnsupportedOperationException("`y` for AseAtoms without position data");
                return mPositions.get(aIdx, XYZ_Y_COL);
            }
            @Override public double z() {
                if (mPositions == null) throw new UnsupportedOperationException("`z` for AseAtoms without position data");
                return mPositions.get(aIdx, XYZ_Z_COL);
            }
            @Override protected int type_() {
                if (mAtomicNumbers == null) return 1;
                return mAtomicNumber2Type.get(mAtomicNumbers.get(aIdx));
            }
            @Override protected double vx_() {assert mMomenta!=null; return mMomenta.get(aIdx, STD_VX_COL)/mass()/ASE_VEL_MUL;}
            @Override protected double vy_() {assert mMomenta!=null; return mMomenta.get(aIdx, STD_VY_COL)/mass()/ASE_VEL_MUL;}
            @Override protected double vz_() {assert mMomenta!=null; return mMomenta.get(aIdx, STD_VZ_COL)/mass()/ASE_VEL_MUL;}
            @Override public IXYZ vxyz() {
                if (mMomenta==null) return new XYZ(0.0, 0.0, 0.0);
                double tDiv = mass()*ASE_VEL_MUL;
                return new XYZ(mMomenta.get(aIdx, STD_VX_COL)/tDiv, mMomenta.get(aIdx, STD_VY_COL)/tDiv, mMomenta.get(aIdx, STD_VZ_COL)/tDiv);
            }
            
            @Override protected void setX_(double aX) {
                if (mPositions == null) throw new UnsupportedOperationException("`setX` for AseAtoms without position data");
                mPositions.set(aIdx, XYZ_X_COL, aX);
            }
            @Override protected void setY_(double aY) {
                if (mPositions == null) throw new UnsupportedOperationException("`setY` for AseAtoms without position data");
                mPositions.set(aIdx, XYZ_Y_COL, aY);
            }
            @Override protected void setZ_(double aZ) {
                if (mPositions == null) throw new UnsupportedOperationException("`setZ` for AseAtoms without position data");
                mPositions.set(aIdx, XYZ_Z_COL, aZ);
            }
            @Override protected void setType_(int aType) {
                if (mAtomicNumbers == null) throw new UnsupportedOperationException("`setType` for AseAtoms without atomic number numbers data");
                assert mType2AtomicNumber != null;
                mAtomicNumbers.set(aIdx, mType2AtomicNumber.get(aType));
            }
            @Override protected void setVx_(double aVx) {assert mMomenta!=null; mMomenta.set(aIdx, STD_VX_COL, aVx*mass()*ASE_VEL_MUL);}
            @Override protected void setVy_(double aVy) {assert mMomenta!=null; mMomenta.set(aIdx, STD_VY_COL, aVy*mass()*ASE_VEL_MUL);}
            @Override protected void setVz_(double aVz) {assert mMomenta!=null; mMomenta.set(aIdx, STD_VZ_COL, aVz*mass()*ASE_VEL_MUL);}
            @Override public ISettableAtom setVxyz(double aVx, double aVy, double aVz) {
                if (mMomenta == null) throw new UnsupportedOperationException("setVxyz");
                double tMul = mass()*ASE_VEL_MUL;
                mMomenta.set(aIdx, STD_VX_COL, aVx*tMul);
                mMomenta.set(aIdx, STD_VY_COL, aVy*tMul);
                mMomenta.set(aIdx, STD_VZ_COL, aVz*tMul);
                return this;
            }
            
            @Override public String symbol() {
                if (mAtomicNumbers == null) return null;
                assert mType2AtomicNumber != null;
                return ATOMIC_NUMBER_TO_SYMBOL.get(mAtomicNumbers.get(aIdx));
            }
            @Override public double mass() {
                @Nullable String tSymbol = symbol();
                return tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
            }
        };
    }
    /**
     * @return {@inheritDoc}
     * @see IBox
     */
    @Override public IBox box() {
        return mCell;
    }
    /** @return {@inheritDoc} */
    @Override public int natoms() {
        return mNumAtoms;
    }
    /** @return {@inheritDoc} */
    @Override public int ntypes() {
        if (mAtomicNumbers == null) return 1;
        assert mType2AtomicNumber != null;
        return mType2AtomicNumber.size()-1;
    }
    
    /**
     * 转换为 python 中的 ase 的 Atoms，这里直接统一开启 pbc
     * @return ase atoms 对应的 {@link PyObject}
     */
    public PyObject toPyObject() throws JepException {
        return toPyObject(false);
    }
    /**
     * 转换为 python 中的 ase 的 Atoms，这里直接统一开启 pbc
     * @param aLimited 是否限制属性转换，此时会只转换必要的坐标和原子需要信息，默认为 {@code false}
     * @return ase atoms 对应的 {@link PyObject}
     */
    public PyObject toPyObject(boolean aLimited) throws JepException {
        SP.Python.exec("from jsepy.atom import convertAseAtomsToPyObject");
        return (PyObject)SP.Python.invoke("convertAseAtomsToPyObject", this, aLimited);
    }
    
    /** @return {@inheritDoc} */
    @Override public AseAtoms copy() {
        AseAtoms tOut = new AseAtoms(mNumAtoms, mCell.copy(), copyData(mInfos), copyData(mArrays), copyData(mCalcResults));
        if (mType2AtomicNumber != null) {
            int ntypes = ntypes();
            String[] tSymbols = new String[ntypes];
            for (int type = 1; type < ntypes; ++type) {
                tSymbols[type-1] = symbol(type);
            }
            tOut.setSymbolOrder(tSymbols);
        }
        return tOut;
    }
    static Map<String, Object> copyData(Map<String, Object> aData) {
        Map<String, Object> rData = new LinkedHashMap<>();
        for (Map.Entry<String, Object> oEntry : aData.entrySet()) {
            Object oValue = oEntry.getValue();
            if (oValue == null) continue;
            if (oValue instanceof String[]) {
                String[] oValueStr = (String[])oValue;
                String[] tValue = new String[oValueStr.length];
                System.arraycopy(oValueStr, 0, tValue, 0, tValue.length);
                rData.put(oEntry.getKey(), tValue);
            } else
            if (oValue instanceof String[][]) {
                String[][] oValueStr = (String[][])oValue;
                int nrows = oValueStr.length;
                int ncols = oValueStr[0].length;
                String[][] tValue = new String[nrows][ncols];
                for (int i = 0; i < nrows; ++i) {
                    System.arraycopy(oValueStr[i], 0, tValue[i], 0, ncols);
                }
                rData.put(oEntry.getKey(), tValue);
            } else
            if (oValue instanceof IVector) {
                rData.put(oEntry.getKey(), ((IVector)oValue).copy());
            } else
            if (oValue instanceof IMatrix) {
                rData.put(oEntry.getKey(), ((IMatrix)oValue).copy());
            } else
            if (oValue instanceof IIntVector) {
                rData.put(oEntry.getKey(), ((IIntVector)oValue).copy());
            } else
            if (oValue instanceof IIntMatrix) {
                rData.put(oEntry.getKey(), ((IIntMatrix)oValue).copy());
            } else
            if (oValue instanceof ILogicalVector) {
                rData.put(oEntry.getKey(), ((ILogicalVector)oValue).copy());
            } else
            if ((oValue instanceof NDArray) || (oValue instanceof PyObject)) {
                String oKey = oEntry.getKey();
                UT.Code.warning("NDArray or PyObject data ("+oKey+") will not copy");
                rData.put(oKey, oValue);
            } else {
                rData.put(oEntry.getKey(), oValue);
            }
        }
        return rData;
    }
    
    
    /**
     * 通过一个 ase atoms 对应的 {@link PyObject} 来创建对应的 jse 版
     * {@link AseAtoms}，会重新拷贝一次内部数据加快频繁访问下的性能
     * <p>
     * {@link #of(PyObject)} 为等价的别名方法
     *
     * @param aPyAtoms 输入的 ase atoms 对象
     * @return 创建的 jse 版 AseAtoms
     * @see #of(PyObject)
     */
    public static AseAtoms fromPyObject(PyObject aPyAtoms) throws JepException {
        return fromPyObject(aPyAtoms, false);
    }
    /**
     * 通过一个 ase atoms 对应的 {@link PyObject} 来创建对应的 jse 版
     * {@link AseAtoms}，会重新拷贝一次内部数据加快频繁访问下的性能
     * <p>
     * {@link #of(PyObject)} 为等价的别名方法
     *
     * @param aPyAtoms 输入的 ase atoms 对象
     * @param aLimited 是否限制属性转换，此时会只转换必要的坐标和原子需要信息，默认为 {@code false}
     * @return 创建的 jse 版 AseAtoms
     * @see #of(PyObject)
     */
    public static AseAtoms fromPyObject(PyObject aPyAtoms, boolean aLimited) throws JepException {
        SP.Python.exec("from jsepy.atom import convertPyObjectToAseAtoms");
        return (AseAtoms)SP.Python.invoke("convertPyObjectToAseAtoms", aPyAtoms, aLimited);
    }
    @ApiStatus.Internal
    public static AseAtoms newAseAtoms_(int aNumAtoms,
                                        double aCellAx, double aCellAy, double aCellAz,
                                        double aCellBx, double aCellBy, double aCellBz,
                                        double aCellCx, double aCellCy, double aCellCz,
                                        Map<String, Object> aInfos, Map<String, Object> aArrays, Map<String, Object> aCalcResults) {
        boolean tNotPrism =
               MathEX.Code.numericEqual(aCellAy, 0.0) && MathEX.Code.numericEqual(aCellAz, 0.0)
            && MathEX.Code.numericEqual(aCellBx, 0.0) && MathEX.Code.numericEqual(aCellBz, 0.0)
            && MathEX.Code.numericEqual(aCellCx, 0.0) && MathEX.Code.numericEqual(aCellCy, 0.0)
            ;
        IBox tCell = tNotPrism ?
            new Box(aCellAx, aCellBy, aCellCz) :
            new BoxPrism(
                aCellAx, aCellAy, aCellAz,
                aCellBx, aCellBy, aCellBz,
                aCellCx, aCellCy, aCellCz
            );
        return new AseAtoms(aNumAtoms, tCell, aInfos, aArrays, aCalcResults);
    }
    
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 jse 版 {@link AseAtoms}
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #fromAtomData(IAtomData, String...)} 来手动指定元素符号信息
     * <p>
     * {@link #of(IAtomData)} 为等价的别名方法
     *
     * @param aAtomData 输入的原子数据
     * @return 创建的 jse 版 AseAtoms
     * @throws IllegalArgumentException 如果包含非法的元素符号
     * @see #of(IAtomData)
     * @see #fromAtomData(IAtomData, String...)
     */
    public static AseAtoms fromAtomData(IAtomData aAtomData) {
        @Nullable List<@Nullable String> tSymbols = aAtomData.symbols();
        return fromAtomData(aAtomData, tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 jse 版 {@link AseAtoms}
     * <p>
     * {@link #of(IAtomData, String...)} 为等价的别名方法
     *
     * @param aAtomData 输入的原子数据
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 jse 版 AseAtoms
     * @throws IllegalArgumentException 如果包含非法的元素符号
     * @see #of(IAtomData, String...)
     * @see #fromAtomData(IAtomData)
     */
    public static AseAtoms fromAtomData(IAtomData aAtomData, String... aSymbols) {
        if (aSymbols == null) aSymbols = ZL_STR;
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof AseAtoms) {
            // AseAtoms 则直接获取即可（专门优化，保留完整模拟盒信息）
            return ((AseAtoms)aAtomData).copy().setSymbols(aSymbols);
        } else {
            // 直接遍历拷贝数据
            int tNumAtoms = aAtomData.natoms();
            IntVector rAtomicNumbers = IntVector.zeros(tNumAtoms);
            RowMatrix rPositions = RowMatrix.zeros(tNumAtoms, ATOM_DATA_KEYS_XYZ.length);
            @Nullable RowMatrix rMomenta = aAtomData.hasVelocity() ? RowMatrix.zeros(tNumAtoms, ATOM_DATA_KEYS_VELOCITY.length) : null;
            for (int i = 0; i < tNumAtoms; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                int tType = tAtom.type();
                rAtomicNumbers.set(i, tType>aSymbols.length ? tType : SYMBOL_TO_ATOMIC_NUMBER.get(aSymbols[tType-1]));
                rPositions.set(i, XYZ_X_COL, tAtom.x());
                rPositions.set(i, XYZ_Y_COL, tAtom.y());
                rPositions.set(i, XYZ_Z_COL, tAtom.z());
                if (rMomenta != null) {
                    @Nullable String tSymbol = ATOMIC_NUMBER_TO_SYMBOL.get(rAtomicNumbers.get(i));
                    double tMass = tSymbol==null ? Double.NaN : MASS.getOrDefault(tSymbol, Double.NaN);
                    rMomenta.set(i, STD_VX_COL, tAtom.vx()*tMass);
                    rMomenta.set(i, STD_VY_COL, tAtom.vy()*tMass);
                    rMomenta.set(i, STD_VZ_COL, tAtom.vz()*tMass);
                }
            }
            if (rMomenta != null) rMomenta.multiply2this(ASE_VEL_MUL);
            Map<String, Object> rArrays = new LinkedHashMap<>();
            rArrays.put("numbers", rAtomicNumbers);
            rArrays.put("positions", rPositions);
            if (rMomenta != null) rArrays.put("momenta", rMomenta);
            return new AseAtoms(tNumAtoms, aAtomData.box().copy(), new LinkedHashMap<>(), rArrays, new LinkedHashMap<>()).setSymbolOrder(aSymbols);
        }
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #fromAtomData(IAtomData, String...)
     * @see Collection
     */
    public static AseAtoms fromAtomData(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {
        return fromAtomData(aAtomData, IO.Text.toArray(aSymbols));
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 jse 版 {@link AseAtoms}
     * <p>
     * 默认会尝试自动从 {@link IAtomData} 获取元素符号信息，使用
     * {@link #of(IAtomData, String...)} 来手动指定元素符号信息
     *
     * @param aAtomData 输入的原子数据
     * @return 创建的 jse 版 AseAtoms
     * @throws IllegalArgumentException 如果包含非法的元素符号
     * @see #of(IAtomData, String...)
     */
    public static AseAtoms of(IAtomData aAtomData) {
        return fromAtomData(aAtomData);
    }
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 jse 版 {@link AseAtoms}
     * @param aAtomData 输入的原子数据
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 jse 版 AseAtoms
     * @throws IllegalArgumentException 如果包含非法的元素符号
     * @see #of(IAtomData)
     */
    public static AseAtoms of(IAtomData aAtomData, String... aSymbols) {
        return fromAtomData(aAtomData, aSymbols);
    }
    /**
     * 传入列表形式元素符号的转换
     * @see #of(IAtomData, String...)
     * @see Collection
     */
    public static AseAtoms of(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {
        return fromAtomData(aAtomData, aSymbols);
    }
    /**
     * 通过一个 ase atoms 对应的 {@link PyObject} 来创建对应的 jse 版
     * {@link AseAtoms}，会重新拷贝一次内部数据加快频繁访问下的性能
     *
     * @param aPyAtoms 输入的 ase atoms 对象
     * @return 创建的 jse 版 AseAtoms
     */
    public static AseAtoms of(PyObject aPyAtoms) throws JepException {
        return fromPyObject(aPyAtoms);
    }
    /**
     * python 中使用的兼容接口，避免 jep 检测重载方法失效
     * @param aAnyData 任何的原子数据，可以是 jse 的 {@link IAtomData}，也可以是
     *                 ase atoms 对应的 {@link PyObject}
     * @return 创建的 jse 版 AseAtoms
     */
    public static AseAtoms of_compat(Object aAnyData) throws JepException {
        // 手动实现动态加载
        if (aAnyData instanceof PyObject) {
            return fromPyObject((PyObject)aAnyData);
        } else
        if (aAnyData instanceof IAtomData) {
            return fromAtomData((IAtomData)aAnyData);
        } else {
            throw new IllegalArgumentException("Invalid data type: " + aAnyData.getClass().getName());
        }
    }
    
    
    /// 直接基于 ase 的读写
    /**
     * 通过 ase 来读取原子数据文件
     * @param aFilePath 任意的原子数据文件路径，通过 ase 自动识别类型
     * @return 读取得到的 {@link AseAtoms} 对象，如果有任何问题应该会抛出 {@link JepException}
     * @see #read(String, Map)
     * @see #write(String)
     */
    public static AseAtoms read(String aFilePath) throws JepException {
        SP.Python.exec("from ase.io import read");
        try (PyCallable tPyRead = SP.Python.getValue("read", PyCallable.class)) {
            return fromPyObject(tPyRead.callAs(PyObject.class, aFilePath));
        }
    }
    /**
     * 通过 ase 来读取原子数据文件
     * @param aFilePath 任意的原子数据文件路径，通过 ase 自动识别类型
     * @param aKWArgs 传入 {@code ase.io.read} 的其余参数，可以用来指定文件类型等
     * @return 读取得到的 {@link AseAtoms} 对象或者多帧的 {@code List<AseAtoms>}，如果有任何问题应该会抛出 {@link JepException}
     * @see #write(String, Map)
     */
    public static Object read(String aFilePath, Map<String, Object> aKWArgs) throws JepException {
        SP.Python.exec("from ase.io import read");
        try (PyCallable tPyRead = SP.Python.getValue("read", PyCallable.class)) {
            Object tOut = tPyRead.call(new Object[]{aFilePath}, aKWArgs);
            if (tOut instanceof List) {
                List<AseAtoms> rOut = new ArrayList<>(((List<?>)tOut).size());
                for (Object tAtoms: (List<?>)tOut) {
                    rOut.add(fromPyObject((PyObject)tAtoms));
                }
                return rOut;
            } else {
                return fromPyObject((PyObject)tOut);
            }
        }
    }
    
    /**
     * 通过 ase 来写入原子数据文件
     * @param aFilePath 任意的原子数据文件路径，通过 ase 自动识别类型
     * @see #write(String, Map)
     * @see #read(String)
     */
    public void write(String aFilePath) throws JepException {
        try (PyObject tPyAtoms = toPyObject(); PyCallable tPyWrite = tPyAtoms.getAttr("write", PyCallable.class)) {
            tPyWrite.call(aFilePath);
        }
    }
    /**
     * 通过 ase 来写入原子数据文件
     * @param aFilePath 任意的原子数据文件路径，通过 ase 自动识别类型
     * @param aKWArgs 传入 {@code ase.io.read} 的其余参数，可以用来指定文件类型等
     * @see #read(String, Map)
     */
    public void write(String aFilePath, Map<String, Object> aKWArgs) throws JepException {
        try (PyObject tPyAtoms = toPyObject(); PyCallable tPyWrite = tPyAtoms.getAttr("write", PyCallable.class)) {
            tPyWrite.call(new Object[]{aFilePath}, aKWArgs);
        }
    }
}
