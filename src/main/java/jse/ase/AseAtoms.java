package jse.ase;

import jep.JepException;
import jep.NDArray;
import jep.python.PyCallable;
import jep.python.PyObject;
import jse.atom.*;
import jse.code.SP;
import jse.code.UT;
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
 * 基于 {@code ase.Atoms} 的 jse 实现，继承了 {@link IAtomData}
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
 * 通过：
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
 * @author liqa
 */
public class AseAtoms extends AbstractSettableAtomData {
    /** 注意 ase 的值是带有单位的，而不是直接和文件保持一致，这其实很自作聪明；为了尽量保持一致，这里统一对速度做一个转换，保证默认情况下会一致 */
    public final static double ASE_VEL_MUL = MathEX.Fast.sqrt(1.660539040e-27 / 1.6021766208e-19) * 1.0e2;
    
    private final IBox mBox;
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
    @VisibleForTesting public IBox cell() {return box();}
    @VisibleForTesting public IIntVector numbers() {return atomicNumbers();}
    public IIntVector atomicNumbers() {return mAtomicNumbers;}
    public IMatrix positions() {return mPositions;}
    public @Nullable IMatrix momenta() {return mMomenta;}
    @Override public boolean hasVelocity() {return mMomenta != null;}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return ATOMIC_NUMBER_TO_SYMBOL.get(mType2AtomicNumber.get(aType));}
    @Override public boolean hasMass() {return true;}
    @Override public double mass(int aType) {return MASS.get(symbol(aType));}
    
    /** 支持调整种类的顺序，这对于 ase atoms 比较重要 */
    public AseAtoms setSymbolOrder(String... aSymbolOrder) {
        if (aSymbolOrder == null) aSymbolOrder = ZL_STR;
        if (aSymbolOrder.length > atomTypeNumber()) {
            IIntVector oType2AtomicNumber = mType2AtomicNumber;
            mType2AtomicNumber = Vectors.range(aSymbolOrder.length+1);
            mType2AtomicNumber.subVec(0, oType2AtomicNumber.size()).fill(oType2AtomicNumber);
        }
        for (int tType = 1; tType <= aSymbolOrder.length; ++tType) {
            @Nullable Integer tAtomicNumber = SYMBOL_TO_ATOMIC_NUMBER.get(aSymbolOrder[tType-1]);
            if (tAtomicNumber != null) mType2AtomicNumber.set(tType, tAtomicNumber);
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
    
    /** 支持直接修改 symbols，只会增大种类数，不会减少；少于种类数时会保留旧值 */
    @Override public AseAtoms setSymbols(String... aSymbols) {
        if (aSymbols==null || aSymbols.length==0) return this;
        if (aSymbols.length > atomTypeNumber()) {
            IIntVector oType2AtomicNumber = mType2AtomicNumber;
            mType2AtomicNumber = Vectors.range(aSymbols.length+1);
            mType2AtomicNumber.subVec(0, oType2AtomicNumber.size()).fill(oType2AtomicNumber);
        }
        for (int tType = 1; tType <= aSymbols.length; ++tType) {
            @Nullable Integer tAtomicNumber = SYMBOL_TO_ATOMIC_NUMBER.get(aSymbols[tType-1]);
            if (tAtomicNumber != null) mType2AtomicNumber.set(tType, tAtomicNumber);
        }
        // 更新 mAtomicNumbers，此时需要旧的 mAtomicNumber2Type
        mAtomicNumbers.opt().map2this(i -> mType2AtomicNumber.get(mAtomicNumber2Type.get(i)));
        validAtomicNumber2Type_();
        return this;
    }
    /** 设置原子种类数目 */
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
    @Override public AseAtoms setNoVelocity() {mMomenta = null; return this;}
    @Override public AseAtoms setHasVelocity() {if (mMomenta == null) {mMomenta = RowMatrix.zeros(atomNumber(), ATOM_DATA_KEYS_VELOCITY.length);} return this;}
    
    /** AbstractAtomData stuffs */
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
            @Override public double mass() {return MASS.get(symbol());}
        };
    }
    @Override public IBox box() {return mBox;}
    @Override public int atomNumber() {return mAtomicNumbers.size();}
    @Override public int atomTypeNumber() {return mType2AtomicNumber.size()-1;}
    
    /** 转换为 python 中的 ase 的 Atoms */
    public PyObject toPyObject() throws JepException {return toPyObject(SP.Python.interpreter());}
    public PyObject toPyObject(@NotNull jep.Interpreter aInterpreter) throws JepException {
        aInterpreter.exec("from ase import Atoms");
        try (PyCallable tPyAtoms = aInterpreter.getValue("Atoms", PyCallable.class)) {
            return tPyAtoms.callAs(PyObject.class, Maps.of(
                "cell"     , new double[][] {{mBox.ax(), mBox.ay(), mBox.az()}, {mBox.bx(), mBox.by(), mBox.bz()}, {mBox.cx(), mBox.cy(), mBox.cz()}},
                "pbc"      , true,
                "numbers"  , mAtomicNumbers.asList(),
                "positions", mPositions.asListRows(),
                "momenta"  , mMomenta==null ? null : mMomenta.asListRows()
            ));
        }
    }
    
    /** 拷贝一份 AseAtoms */
    @Override public AseAtoms copy() {
        AseAtoms rAseAtoms = new AseAtoms(mBox.copy(), mAtomicNumbers.copy(), mPositions.copy(), mType2AtomicNumber);
        if (mMomenta != null) rAseAtoms.mMomenta = mMomenta.copy();
        return rAseAtoms;
    }
    
    /** 从 PyObject 来创建 */
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
    
    /** 从 IAtomData 来创建，一般来说 AseAtoms 需要一个额外的 aSymbols 信息 */
    public static AseAtoms fromAtomData(IAtomData aAtomData) {
        @Nullable List<@Nullable String> tSymbols = aAtomData.symbols();
        return fromAtomData(aAtomData, tSymbols==null ? ZL_STR : tSymbols.toArray(ZL_STR));
    }
    public static AseAtoms fromAtomData(IAtomData aAtomData, String... aSymbols) {
        if (aSymbols == null) aSymbols = ZL_STR;
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof AseAtoms) {
            // AseAtoms 则直接获取即可（专门优化，保留完整模拟盒信息）
            return ((AseAtoms)aAtomData).copy().setSymbols(aSymbols);
        } else {
            IIntVector rType2AtomicNumber = Vectors.range(Math.max(aSymbols.length, aAtomData.atomTypeNumber())+1);
            for (int tType = 1; tType <= aSymbols.length; ++tType) {
                @Nullable Integer tAtomicNumber = SYMBOL_TO_ATOMIC_NUMBER.get(aSymbols[tType-1]);
                if (tAtomicNumber != null) rType2AtomicNumber.set(tType, tAtomicNumber);
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
                    double tMass = MASS.get(ATOMIC_NUMBER_TO_SYMBOL.get(rAtomicNumbers.get(i)));
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
    public static AseAtoms fromAtomData(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {return fromAtomData(aAtomData, UT.Text.toArray(aSymbols));}
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static AseAtoms of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static AseAtoms of(IAtomData aAtomData, String... aTypeNames) {return fromAtomData(aAtomData, aTypeNames);}
    public static AseAtoms of(IAtomData aAtomData, Collection<? extends CharSequence> aSymbols) {return fromAtomData(aAtomData, aSymbols);}
    public static AseAtoms of(PyObject aPyAtoms) throws JepException {return fromPyObject(aPyAtoms);}
    /** 增加一个兼容的接口避免 python 中使用出错 */
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
    
    
    /** 直接基于 ase 的读写 */
    public static AseAtoms read(String aFilePath) throws JepException {return read(SP.Python.interpreter(), aFilePath);}
    public static AseAtoms read(@NotNull jep.Interpreter aInterpreter, String aFilePath) throws JepException {
        aInterpreter.exec("from ase.io import read");
        try (PyCallable tPyRead = aInterpreter.getValue("read", PyCallable.class)) {
            return fromPyObject(tPyRead.callAs(PyObject.class, aFilePath));
        }
    }
    public static AseAtoms read(String aFilePath, Map<String, Object> aKWArgs) throws JepException {return read(SP.Python.interpreter(), aFilePath, aKWArgs);}
    public static AseAtoms read(@NotNull jep.Interpreter aInterpreter, String aFilePath, Map<String, Object> aKWArgs) throws JepException {
        aInterpreter.exec("from ase.io import read");
        try (PyCallable tPyRead = aInterpreter.getValue("read", PyCallable.class)) {
            return fromPyObject(tPyRead.callAs(PyObject.class, new Object[]{aFilePath}, aKWArgs));
        }
    }
    
    public void write(String aFilePath) throws JepException {write(SP.Python.interpreter(), aFilePath);}
    public void write(@NotNull jep.Interpreter aInterpreter, String aFilePath) throws JepException {
        try (PyCallable tPyWrite = toPyObject(aInterpreter).getAttr("write", PyCallable.class)) {
            tPyWrite.call(aFilePath);
        }
    }
    public void write(String aFilePath, Map<String, Object> aKWArgs) throws JepException {write(SP.Python.interpreter(), aFilePath, aKWArgs);}
    public void write(@NotNull jep.Interpreter aInterpreter, String aFilePath, Map<String, Object> aKWArgs) throws JepException {
        try (PyCallable tPyWrite = toPyObject(aInterpreter).getAttr("write", PyCallable.class)) {
            tPyWrite.call(new Object[]{aFilePath}, aKWArgs);
        }
    }
}
