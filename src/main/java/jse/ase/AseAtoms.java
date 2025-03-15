package jse.ase;

import jep.JepException;
import jep.NDArray;
import jep.python.PyCallable;
import jep.python.PyObject;
import jse.atom.*;
import jse.code.IO;
import jse.code.SP;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jse.code.CS.*;
import static jse.code.CS.STD_VZ_COL;

/**
 * {@code ase.Atoms} 的 jse 实现，继承了 {@link IAtomData}
 * 用于保证使用方法和 jse 中的原子结构类型一致。
 * <p>
 * 此实现会对 {@code ase.Atoms} 中常用的数据进行值拷贝，保证频繁访问下的效率。
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
 * 和 python 对象的相互转换会丢失 jse 未支持的属性，如果希望使用这些属性则应该直接通过
 * {@link PyObject} 来使用
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
    public final static double ASE_VEL_MUL = MathEX.Fast.sqrt(1.660539040e-27 / 1.6021766208e-19) * 1.0e2;
    
    private IBox mBox;
    private final IIntVector mAtomicNumbers;
    private final IMatrix mPositions;
    private @Nullable IMatrix mMomenta = null;
    /** 用于通过原子序数获取每个种类的粒子数 */
    private final @NotNull Map<Integer, Integer> mAtomicNumber2Type;
    private @NotNull IIntVector mType2AtomicNumber; // 第 0 个直接制空
    
    AseAtoms(IBox aBox, IIntVector aAtomicNumbers, IMatrix aPositions) {
        mBox = aBox;
        mAtomicNumbers = aAtomicNumbers;
        mPositions = aPositions;
        
        mAtomicNumber2Type = new HashMap<>();
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
    private AseAtoms(IBox aBox, IIntVector aAtomicNumbers, IMatrix aPositions, @NotNull IIntVector aType2AtomicNumber) {
        mBox = aBox;
        mAtomicNumbers = aAtomicNumbers;
        mPositions = aPositions;
        
        mType2AtomicNumber = aType2AtomicNumber;
        mAtomicNumber2Type = new HashMap<>();
        int tAtomTypeNumber = mType2AtomicNumber.size()-1;
        for (int tType = 1; tType <= tAtomTypeNumber; ++tType) {
            mAtomicNumber2Type.put(mType2AtomicNumber.get(tType), tType);
        }
    }
    
    /// 获取属性
    /** @see #box() */
    @VisibleForTesting public IBox cell() {return box();}
    /** @see #atomicNumbers() */
    @VisibleForTesting public IIntVector numbers() {return atomicNumbers();}
    /**
     * 直接获取所有的原子序数，对应 ase atoms 中的 {@code numbers()} 方法
     * @return 原子序数组成的向量，按照原子索引排序
     * @see IIntVector
     */
    public IIntVector atomicNumbers() {return mAtomicNumbers;}
    /**
     * 直接获取所有的原子位置，对应 ase atoms 中的 {@code positions()} 方法
     * @return 原子坐标组成的矩阵，按照原子索引按行排列，每行为 {@code x, y, z}
     * @see IMatrix
     */
    public IMatrix positions() {return mPositions;}
    /**
     * 直接获取所有的原子动量，对应 ase atoms 中的 {@code momenta()} 方法
     * @return 原子动量组成的矩阵，按照原子索引按行排列，每行为 {@code px, py, pz}
     * @see IMatrix
     */
    public @Nullable IMatrix momenta() {return mMomenta;}
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasVelocity()
     */
    @Override public boolean hasVelocity() {return mMomenta != null;}
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasSymbol()
     */
    @Override public boolean hasSymbol() {return true;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}
     * @see IAtom#symbol()
     * @see IAtom#type()
     * @see #hasSymbol()
     */
    @Override public String symbol(int aType) {return ATOMIC_NUMBER_TO_SYMBOL.get(mType2AtomicNumber.get(aType));}
    /**
     * @return {@inheritDoc}
     * @see IAtom#hasMass()
     */
    @Override public boolean hasMass() {return true;}
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
        if (aSymbolOrder == null) aSymbolOrder = ZL_STR;
        if (aSymbolOrder.length > atomTypeNumber()) {
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
        if (aSymbols==null || aSymbols.length==0) return this;
        if (aSymbols.length > atomTypeNumber()) {
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
        mAtomicNumbers.opt().map2this(i -> mType2AtomicNumber.get(mAtomicNumber2Type.get(i)));
        validAtomicNumber2Type_();
        return this;
    }
    /**
     * {@inheritDoc}
     * @param aAtomTypeNum {@inheritDoc}
     * @return {@inheritDoc}
     * @throws UnsupportedOperationException 如果不包含元素符号信息
     * @see #atomTypeNumber()
     * @see IAtom#type()
     */
    @Override public AseAtoms setAtomTypeNumber(int aAtomTypeNum) {
        int oTypeNum = atomTypeNumber();
        if (aAtomTypeNum == oTypeNum) return this;
        if (aAtomTypeNum < oTypeNum) {
            // 现在支持设置更小的值，更大的种类会直接截断
            mAtomicNumbers.opt().map2this(i -> {
                int tType = mAtomicNumber2Type.get(i);
                return tType>aAtomTypeNum ? mType2AtomicNumber.get(aAtomTypeNum) : i;
            });
            mType2AtomicNumber = mType2AtomicNumber.subVec(0, aAtomTypeNum+1).copy();
            validAtomicNumber2Type_();
            return this;
        }
        IIntVector oType2AtomicNumber = mType2AtomicNumber;
        mType2AtomicNumber = Vectors.range(aAtomTypeNum+1);
        mType2AtomicNumber.subVec(0, oTypeNum).fill(oType2AtomicNumber);
        validAtomicNumber2Type_();
        return this;
    }
    void validAtomicNumber2Type_() {
        mAtomicNumber2Type.clear();
        int tAtomTypeNumber = atomTypeNumber();
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
    @Override public AseAtoms setNoVelocity() {mMomenta = null; return this;}
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     * @see #hasVelocity()
     * @see #setNoVelocity()
     */
    @Override public AseAtoms setHasVelocity() {if (mMomenta == null) {mMomenta = RowMatrix.zeros(atomNumber(), ATOM_DATA_KEYS_VELOCITY.length);} return this;}
    
    /// set box stuff
    @Override protected void setBox_(double aX, double aY, double aZ) {
        mBox = new Box(aX, aY, aZ);
    }
    @Override protected void setBox_(double aAx, double aAy, double aAz, double aBx, double aBy, double aBz, double aCx, double aCy, double aCz) {
        mBox = new BoxPrism(aAx, aAy, aAz, aBx, aBy, aBz, aCx, aCy, aCz);
    }
    @Override protected void scaleAtomPosition_(boolean aKeepAtomPosition, double aScale) {
        if (aKeepAtomPosition) return;
        mPositions.multiply2this(aScale);
        if (mMomenta != null) {
            mMomenta.multiply2this(aScale);
        }
    }
    @Override protected void validAtomPosition_(boolean aKeepAtomPosition, IBox aOldBox) {
        if (aKeepAtomPosition) return;
        final int tAtomNum = atomNumber();
        XYZ tBuf = new XYZ();
        if (mBox.isPrism() || aOldBox.isPrism()) {
            for (int i = 0; i < tAtomNum; ++i) {
                tBuf.setXYZ(mPositions.get(i, XYZ_X_COL), mPositions.get(i, XYZ_Y_COL), mPositions.get(i, XYZ_Z_COL));
                // 这样转换两次即可实现线性变换
                aOldBox.toDirect(tBuf);
                mBox.toCartesian(tBuf);
                mPositions.set(i, XYZ_X_COL, tBuf.mX);
                mPositions.set(i, XYZ_Y_COL, tBuf.mY);
                mPositions.set(i, XYZ_Z_COL, tBuf.mZ);
                // 如果存在速度，则速度也需要做一次这样的变换
                if (mMomenta != null) {
                    tBuf.setXYZ(mMomenta.get(i, STD_VX_COL), mMomenta.get(i, STD_VY_COL), mMomenta.get(i, STD_VZ_COL));
                    aOldBox.toDirect(tBuf);
                    mBox.toCartesian(tBuf);
                    mMomenta.set(i, STD_VX_COL, tBuf.mX);
                    mMomenta.set(i, STD_VY_COL, tBuf.mY);
                    mMomenta.set(i, STD_VZ_COL, tBuf.mZ);
                }
            }
        } else {
            tBuf.setXYZ(mBox);
            tBuf.div2this(aOldBox);
            mPositions.col(XYZ_X_COL).multiply2this(tBuf.mX);
            mPositions.col(XYZ_Y_COL).multiply2this(tBuf.mY);
            mPositions.col(XYZ_Z_COL).multiply2this(tBuf.mZ);
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
            @Override public double x() {return mPositions.get(aIdx, XYZ_X_COL);}
            @Override public double y() {return mPositions.get(aIdx, XYZ_Y_COL);}
            @Override public double z() {return mPositions.get(aIdx, XYZ_Z_COL);}
            @Override protected int type_() {return mAtomicNumber2Type.get(mAtomicNumbers.get(aIdx));}
            @Override protected double vx_() {assert mMomenta!=null; return mMomenta.get(aIdx, STD_VX_COL)/mass()/ASE_VEL_MUL;}
            @Override protected double vy_() {assert mMomenta!=null; return mMomenta.get(aIdx, STD_VY_COL)/mass()/ASE_VEL_MUL;}
            @Override protected double vz_() {assert mMomenta!=null; return mMomenta.get(aIdx, STD_VZ_COL)/mass()/ASE_VEL_MUL;}
            @Override public IXYZ vxyz() {
                if (mMomenta==null) return new XYZ(0.0, 0.0, 0.0);
                double tDiv = mass()*ASE_VEL_MUL;
                return new XYZ(mMomenta.get(aIdx, STD_VX_COL)/tDiv, mMomenta.get(aIdx, STD_VY_COL)/tDiv, mMomenta.get(aIdx, STD_VZ_COL)/tDiv);
            }
            
            @Override protected void setX_(double aX) {mPositions.set(aIdx, XYZ_X_COL, aX);}
            @Override protected void setY_(double aY) {mPositions.set(aIdx, XYZ_Y_COL, aY);}
            @Override protected void setZ_(double aZ) {mPositions.set(aIdx, XYZ_Z_COL, aZ);}
            @Override protected void setType_(int aType) {mAtomicNumbers.set(aIdx, mType2AtomicNumber.get(aType));}
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
            
            @Override public String symbol() {return ATOMIC_NUMBER_TO_SYMBOL.get(mAtomicNumbers.get(aIdx));}
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
    @Override public IBox box() {return mBox;}
    /** @return {@inheritDoc} */
    @Override public int atomNumber() {return mAtomicNumbers.size();}
    /** @return {@inheritDoc} */
    @Override public int atomTypeNumber() {return mType2AtomicNumber.size()-1;}
    
    /**
     * 转换为 python 中的 ase 的 Atoms，这里直接统一开启 pbc
     * @return ase atoms 对应的 {@link PyObject}
     */
    public PyObject toPyObject() throws JepException {
        SP.Python.exec("from ase import Atoms");
        try (PyCallable tPyAtoms = SP.Python.getValue("Atoms", PyCallable.class)) {
            return tPyAtoms.callAs(PyObject.class, Maps.of(
                "cell"     , new double[][] {{mBox.ax(), mBox.ay(), mBox.az()}, {mBox.bx(), mBox.by(), mBox.bz()}, {mBox.cx(), mBox.cy(), mBox.cz()}},
                "pbc"      , true,
                "numbers"  , mAtomicNumbers.asList(),
                "positions", mPositions.asListRows(),
                "momenta"  , mMomenta==null ? null : mMomenta.asListRows()
            ));
        }
    }
    
    /** @return {@inheritDoc} */
    @Override public AseAtoms copy() {
        AseAtoms rAseAtoms = new AseAtoms(mBox.copy(), mAtomicNumbers.copy(), mPositions.copy(), mType2AtomicNumber);
        if (mMomenta != null) rAseAtoms.mMomenta = mMomenta.copy();
        return rAseAtoms;
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
        NDArray<?> tPyCellArray;
        try (PyObject tPyCell = aPyAtoms.getAttr("cell", PyObject.class)) {
            tPyCellArray = tPyCell.getAttr("array", NDArray.class);
        }
        NDArray<?> tPyNumbers;
        try {
            tPyNumbers = aPyAtoms.getAttr("numbers", NDArray.class);
        } catch (JepException e) {
            // 对于某些特殊的 ndarray jep 不能自动转换，这就很麻烦了；
            // 这对于非主解释器情况下会出现问题，但是这里不考虑
            SP.Python.setValue("__JSE_ASEATOMS_pyatoms__", aPyAtoms);
            tPyNumbers = SP.Python.getValue("__JSE_ASEATOMS_pyatoms__.numbers.astype('int32', casting='unsafe')", NDArray.class);
            SP.Python.removeValue("__JSE_ASEATOMS_pyatoms__");
        }
        NDArray<?> tPyPositions = aPyAtoms.getAttr("positions", NDArray.class);
        @Nullable NDArray<?> tPyMomenta = null;
        try (PyCallable tHas = aPyAtoms.getAttr("has", PyCallable.class)) {
            if (tHas.callAs(Boolean.class, "momenta")) try (PyCallable tGetMomenta = aPyAtoms.getAttr("get_momenta", PyCallable.class)) {
                tPyMomenta = tGetMomenta.callAs(NDArray.class);
            }
        }
        RowMatrix tCellMat = new RowMatrix(tPyCellArray.getDimensions()[0], tPyCellArray.getDimensions()[1], (double[])tPyCellArray.getData());
        boolean tNotPrism =
               MathEX.Code.numericEqual(tCellMat.get(0, 1), 0.0) && MathEX.Code.numericEqual(tCellMat.get(0, 2), 0.0)
            && MathEX.Code.numericEqual(tCellMat.get(1, 0), 0.0) && MathEX.Code.numericEqual(tCellMat.get(1, 2), 0.0)
            && MathEX.Code.numericEqual(tCellMat.get(2, 0), 0.0) && MathEX.Code.numericEqual(tCellMat.get(2, 1), 0.0)
            ;
        IBox rBox = tNotPrism ?
            new Box(tCellMat.get(0, 0), tCellMat.get(1, 1), tCellMat.get(2, 2)) :
            new BoxPrism(
                tCellMat.get(0, 0), tCellMat.get(0, 1), tCellMat.get(0, 2),
                tCellMat.get(1, 0), tCellMat.get(1, 1), tCellMat.get(1, 2),
                tCellMat.get(2, 0), tCellMat.get(2, 1), tCellMat.get(2, 2)
            );
        // numpy 的整数 ndarray 实际上不确定是 long[] 还是 int[]，因此要这样处理，当然这会很烦；
        // 实际上，一般来说，在 linux 上为 long[] 而 windows 上为 int[]，而在 numpy2 中则统一为了 long[]
        IIntVector tNumbers;
        Object tPyNumbersData = tPyNumbers.getData();
        if (tPyNumbersData instanceof int[]) {
            tNumbers = new IntVector(tPyNumbers.getDimensions()[0], (int[])tPyNumbersData);
        } else
        if (tPyNumbersData instanceof long[]) {
            tNumbers = new LongVector(tPyNumbers.getDimensions()[0], (long[])tPyNumbersData).asIntVec();
        } else
        if (tPyNumbersData instanceof double[]) {
            tNumbers = new Vector(tPyNumbers.getDimensions()[0], (double[])tPyNumbersData).asIntVec();
        } else {
            // 同样这对于非主解释器情况下会出现问题，但是这里不考虑
            SP.Python.setValue("__JSE_ASEATOMS_pyatoms__", aPyAtoms);
            tPyNumbers = SP.Python.getValue("__JSE_ASEATOMS_pyatoms__.numbers.astype('int32', casting='unsafe')", NDArray.class);
            SP.Python.removeValue("__JSE_ASEATOMS_pyatoms__");
            tPyNumbersData = tPyNumbers.getData();
            tNumbers = new IntVector(tPyNumbers.getDimensions()[0], (int[])tPyNumbersData);
        }
        AseAtoms rAseAtoms = new AseAtoms(rBox, tNumbers, new RowMatrix(tPyPositions.getDimensions()[0], tPyPositions.getDimensions()[1], (double[])tPyPositions.getData()));
        if (tPyMomenta != null) rAseAtoms.mMomenta = new RowMatrix(tPyMomenta.getDimensions()[0], tPyMomenta.getDimensions()[1], (double[])tPyMomenta.getData());
        return rAseAtoms;
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
            IIntVector rType2AtomicNumber = Vectors.range(Math.max(aSymbols.length, aAtomData.atomTypeNumber())+1);
            for (int tType = 1; tType <= aSymbols.length; ++tType) {
                String tSymbol = aSymbols[tType-1];
                @Nullable Integer tAtomicNumber = SYMBOL_TO_ATOMIC_NUMBER.get(tSymbol);
                if (tAtomicNumber == null) throw new IllegalArgumentException("symbol: " + tSymbol);
                rType2AtomicNumber.set(tType, tAtomicNumber);
            }
            // 直接遍历拷贝数据
            int tAtomNum = aAtomData.atomNumber();
            IntVector rAtomicNumbers = IntVector.zeros(tAtomNum);
            RowMatrix rPositions = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_XYZ.length);
            @Nullable RowMatrix rMomenta = aAtomData.hasVelocity() ? RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_VELOCITY.length) : null;
            for (int i = 0; i < tAtomNum; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                rAtomicNumbers.set(i, rType2AtomicNumber.get(tAtom.type()));
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
            AseAtoms rAseAtoms =  new AseAtoms(aAtomData.box().copy(), rAtomicNumbers, rPositions, rType2AtomicNumber);
            rAseAtoms.mMomenta = rMomenta;
            return rAseAtoms;
        }
    }
    /**
     * 传入列表形式元素符号的创建
     * @see #fromAtomData(IAtomData, String...)
     * @see Collection
     */
    public static AseAtoms fromAtomData(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {return fromAtomData(aAtomData, IO.Text.toArray(aSymbols));}
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
    public static AseAtoms of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    /**
     * 通过一个一般的原子数据 {@link IAtomData} 来创建一个 jse 版 {@link AseAtoms}
     * @param aAtomData 输入的原子数据
     * @param aSymbols 可选的元素符号信息，默认会自动通过输入原子数据获取
     * @return 创建的 jse 版 AseAtoms
     * @throws IllegalArgumentException 如果包含非法的元素符号
     * @see #of(IAtomData)
     */
    public static AseAtoms of(IAtomData aAtomData, String... aSymbols) {return fromAtomData(aAtomData, aSymbols);}
    /**
     * 传入列表形式元素符号的转换
     * @see #of(IAtomData, String...)
     * @see Collection
     */
    public static AseAtoms of(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {return fromAtomData(aAtomData, aSymbols);}
    /**
     * 通过一个 ase atoms 对应的 {@link PyObject} 来创建对应的 jse 版
     * {@link AseAtoms}，会重新拷贝一次内部数据加快频繁访问下的性能
     *
     * @param aPyAtoms 输入的 ase atoms 对象
     * @return 创建的 jse 版 AseAtoms
     */
    public static AseAtoms of(PyObject aPyAtoms) throws JepException {return fromPyObject(aPyAtoms);}
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
     * @return 读取得到的 {@link AseAtoms} 对象，如果有任何问题应该会抛出 {@link JepException}
     * @see #write(String, Map)
     */
    public static AseAtoms read(String aFilePath, Map<String, Object> aKWArgs) throws JepException {
        SP.Python.exec("from ase.io import read");
        try (PyCallable tPyRead = SP.Python.getValue("read", PyCallable.class)) {
            return fromPyObject(tPyRead.callAs(PyObject.class, new Object[]{aFilePath}, aKWArgs));
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
