package jse.ase;

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
import jse.vasp.IVaspCommonData;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
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
 * @author liqa
 */
public class AseAtoms extends AbstractSettableAtomData {
    
    private IBox mBox;
    private final IIntVector mAtomicNumbers;
    private final IMatrix mPositions;
    private @Nullable IMatrix mMomenta = null;
    /** 用于通过原子序数获取每个种类的粒子数 */
    private final @NotNull Map<Integer, Integer> mAtomicNumber2Type;
    private final @NotNull IIntVector mType2AtomicNumber; // 第 0 个直接制空
    
    AseAtoms(IBox aBox, IIntVector aAtomicNumbers, IMatrix aPositions) {this(aBox, aAtomicNumbers, aPositions, null);}
    AseAtoms(IBox aBox, IIntVector aAtomicNumbers, IMatrix aPositions, @Nullable Map<Integer, Integer> aAtomicNumber2Type) {this(aBox, aAtomicNumbers, aPositions, aAtomicNumber2Type==null ? null : new HashMap<>(aAtomicNumber2Type), null);}
    private AseAtoms(IBox aBox, IIntVector aAtomicNumbers, IMatrix aPositions, @Nullable Map<Integer, Integer> aAtomicNumber2Type, @Nullable IIntVector aType2AtomicNumber) {
        mBox = aBox;
        mAtomicNumbers = aAtomicNumbers;
        mPositions = aPositions;
        
        if (aAtomicNumber2Type != null) {
            mAtomicNumber2Type = aAtomicNumber2Type;
        } else {
            mAtomicNumber2Type = new HashMap<>();
            mAtomicNumbers.forEach(atomicNumber -> {
                int tType = mAtomicNumber2Type.size() + 1;
                mAtomicNumber2Type.put(atomicNumber, tType);
            });
        }
        if (aType2AtomicNumber != null) {
            mType2AtomicNumber = aType2AtomicNumber;
        } else {
            mType2AtomicNumber = Vectors.range(UT.Code.max(mAtomicNumber2Type.values())+1);
            for (Map.Entry<Integer, Integer> tEntry : mAtomicNumber2Type.entrySet()) {
                mType2AtomicNumber.set(tEntry.getValue(), tEntry.getKey());
            }
        }
    }
    
    /// 获取属性
    @VisibleForTesting public IBox cell() {return box();}
    @VisibleForTesting public IIntVector numbers() {return atomicNumbers();}
    public IIntVector atomicNumbers() {return mAtomicNumbers;}
    public IMatrix positions() {return mPositions;}
    public @Nullable IMatrix momenta() {return mMomenta;}
    
    /** AbstractAtomData stuffs */
    @Override public boolean hasVelocities() {return mMomenta != null;}
    @Override public ISettableAtom atom(int aIdx) {
        return new AbstractSettableAtom() {
            @Override public double x() {return mPositions.get(aIdx, XYZ_X_COL);}
            @Override public double y() {return mPositions.get(aIdx, XYZ_Y_COL);}
            @Override public double z() {return mPositions.get(aIdx, XYZ_Z_COL);}
            @Override public int id() {return aIdx+1;}
            @Override public int type() {return mAtomicNumber2Type.get(mAtomicNumbers.get(aIdx));}
            @Override public int index() {return aIdx;}
            
            @Override public double vx() {return mMomenta==null?0.0:(mMomenta.get(aIdx, STD_VX_COL)/MASS.get(ATOMIC_NUMBER_TO_SYMBOL.get(mAtomicNumbers.get(aIdx))));}
            @Override public double vy() {return mMomenta==null?0.0:(mMomenta.get(aIdx, STD_VY_COL)/MASS.get(ATOMIC_NUMBER_TO_SYMBOL.get(mAtomicNumbers.get(aIdx))));}
            @Override public double vz() {return mMomenta==null?0.0:(mMomenta.get(aIdx, STD_VZ_COL)/MASS.get(ATOMIC_NUMBER_TO_SYMBOL.get(mAtomicNumbers.get(aIdx))));}
            @Override public boolean hasVelocities() {return mMomenta != null;}
            
            @Override public ISettableAtom setX(double aX) {mPositions.set(aIdx, XYZ_X_COL, aX); return this;}
            @Override public ISettableAtom setY(double aY) {mPositions.set(aIdx, XYZ_Y_COL, aY); return this;}
            @Override public ISettableAtom setZ(double aZ) {mPositions.set(aIdx, XYZ_Z_COL, aZ); return this;}
            @Override public ISettableAtom setID(int aID) {throw new UnsupportedOperationException("setID");}
            @Override public ISettableAtom setType(int aType) {
                // 这里超过原子种类数目直接抛出错误
                if (aType >= mType2AtomicNumber.size()) throw new UnsupportedOperationException("setType when type > ntypes, type: " + aType);
                mAtomicNumbers.set(aIdx, mType2AtomicNumber.get(aType));
                return this;
            }
            @Override public ISettableAtom setVx(double aVx) {
                if (mMomenta == null) throw new UnsupportedOperationException("setVx");
                mMomenta.set(aIdx, STD_VX_COL, aVx*MASS.get(ATOMIC_NUMBER_TO_SYMBOL.get(mAtomicNumbers.get(aIdx)))); return this;
            }
            @Override public ISettableAtom setVy(double aVy) {
                if (mMomenta == null) throw new UnsupportedOperationException("setVy");
                mMomenta.set(aIdx, STD_VY_COL, aVy*MASS.get(ATOMIC_NUMBER_TO_SYMBOL.get(mAtomicNumbers.get(aIdx)))); return this;
            }
            @Override public ISettableAtom setVz(double aVz) {
                if (mMomenta == null) throw new UnsupportedOperationException("setVz");
                mMomenta.set(aIdx, STD_VZ_COL, aVz*MASS.get(ATOMIC_NUMBER_TO_SYMBOL.get(mAtomicNumbers.get(aIdx)))); return this;
            }
        };
    }
    @Override public IBox box() {return mBox;}
    @Override public int atomNumber() {return mAtomicNumbers.size();}
    @Override public int atomTypeNumber() {return mType2AtomicNumber.size()-1;}
    @Override public AseAtoms setAtomTypeNumber(int aAtomTypeNum) {throw new UnsupportedOperationException("setAtomTypeNumber");}
    
    /** 转换为 python 中的 ase 的 Atoms */
    public PyObject toPyObject() {return toPyObject(SP.Python.interpreter());}
    public PyObject toPyObject(@NotNull jep.Interpreter aInterpreter) {
        aInterpreter.exec("from ase import Atoms");
        try (PyCallable tPyAtoms = aInterpreter.getValue("Atoms", PyCallable.class)) {
            return tPyAtoms.callAs(PyObject.class, Maps.of(
                "cell"     , new double[][] {{mBox.ax(), mBox.ay(), mBox.az()}, {mBox.bx(), mBox.by(), mBox.bz()}, {mBox.cx(), mBox.cy(), mBox.cz()}},
                "numbers"  , mAtomicNumbers.asList(),
                "positions", mPositions.asListRows(),
                "momenta"  , mMomenta==null ? null : mMomenta.asListRows()
            ));
        }
    }
    
    /** 拷贝一份 AseAtoms */
    @Override public AseAtoms copy() {return copy_(null);}
    private AseAtoms copy_(@Nullable Map<Integer, Integer> aAtomicNumber2Type) {
        AseAtoms rAseAtoms = new AseAtoms(mBox.copy(), mAtomicNumbers.copy(), mPositions.copy(), aAtomicNumber2Type==null ? mAtomicNumber2Type : aAtomicNumber2Type);
        if (mMomenta != null) rAseAtoms.mMomenta = mMomenta.copy();
        return rAseAtoms;
    }
    
    /** 从 PyObject 来创建 */
    public static AseAtoms fromPyObject(PyObject aPyAtoms) {
        NDArray<?> tPyCellArray;
        try (PyObject tPyCell = aPyAtoms.getAttr("cell", PyObject.class)) {
            tPyCellArray = tPyCell.getAttr("array", NDArray.class);
        }
        NDArray<?> tPyNumbers = aPyAtoms.getAttr("numbers", NDArray.class);
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
        AseAtoms rAseAtoms = new AseAtoms(rBox, new IntVector(tPyNumbers.getDimensions()[0], (int[])tPyNumbers.getData()), new RowMatrix(tPyPositions.getDimensions()[0], tPyPositions.getDimensions()[1], (double[])tPyPositions.getData()));
        if (tPyMomenta != null) rAseAtoms.mMomenta = new RowMatrix(tPyMomenta.getDimensions()[0], tPyMomenta.getDimensions()[1], (double[])tPyMomenta.getData());
        return rAseAtoms;
    }
    
    /** 从 IAtomData 来创建，一般来说 AseAtoms 需要一个额外的 aTypeNames 信息 */
    public static AseAtoms fromAtomData(IAtomData aAtomData) {return fromAtomData(aAtomData, (aAtomData instanceof IVaspCommonData) ? ((IVaspCommonData)aAtomData).typeNames() : ZL_STR);}
    public static AseAtoms fromAtomData(IAtomData aAtomData, String... aTypeNames) {
        Map<Integer, Integer> rAtomicNumber2Type = null;
        if (aTypeNames!=null && aTypeNames.length>0) {
            rAtomicNumber2Type = new HashMap<>();
            for (int i = 0; i < aTypeNames.length; ++i) {
                @Nullable Integer tAtomicNumber = SYMBOL_TO_ATOMIC_NUMBER.get(aTypeNames[i]);
                if (tAtomicNumber != null) rAtomicNumber2Type.put(tAtomicNumber, i+1);
            }
        }
        // 根据输入的 aAtomData 类型来具体判断需要如何获取 rAtomData
        if (aAtomData instanceof AseAtoms) {
            // AseAtoms 则直接获取即可（专门优化，保留完整模拟盒信息）
            return ((AseAtoms)aAtomData).copy_(rAtomicNumber2Type);
        } else {
            int tAtomTypeNumber = aAtomData.atomTypeNumber();
            // 先统一获取元素种类，如果没有元素种类输入则使用 type 作为种类
            IIntVector rType2AtomicNumber;
            if (rAtomicNumber2Type == null) {
                rType2AtomicNumber = Vectors.range(tAtomTypeNumber+1);
                rAtomicNumber2Type = new HashMap<>();
                for (int tType = 1; tType < tAtomTypeNumber; ++tType) {
                    rAtomicNumber2Type.put(tType, tType);
                }
            } else {
                rType2AtomicNumber = Vectors.range(Math.max(tAtomTypeNumber, aTypeNames.length)+1);
                for (Map.Entry<Integer, Integer> tEntry : rAtomicNumber2Type.entrySet()) {
                    rType2AtomicNumber.set(tEntry.getValue(), tEntry.getKey());
                }
            }
            // 直接遍历拷贝数据
            int tAtomNum = aAtomData.atomNumber();
            IntVector rAtomicNumbers = IntVector.zeros(tAtomNum);
            RowMatrix rPositions = RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_XYZ.length);
            @Nullable RowMatrix rMomenta = aAtomData.hasVelocities() ? RowMatrix.zeros(tAtomNum, ATOM_DATA_KEYS_VELOCITY.length) : null;
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
            AseAtoms rAseAtoms =  new AseAtoms(aAtomData.box().copy(), rAtomicNumbers, rPositions, rAtomicNumber2Type, rType2AtomicNumber);
            rAseAtoms.mMomenta = rMomenta;
            return rAseAtoms;
        }
    }
    /** 按照规范，这里还提供这种构造方式；目前暂不清楚何种更好，因此不做注解 */
    public static AseAtoms of(IAtomData aAtomData) {return fromAtomData(aAtomData);}
    public static AseAtoms of(IAtomData aAtomData, String... aTypeNames) {return fromAtomData(aAtomData, aTypeNames);}
    public static AseAtoms of(PyObject aPyAtoms) {return fromPyObject(aPyAtoms);}
}
