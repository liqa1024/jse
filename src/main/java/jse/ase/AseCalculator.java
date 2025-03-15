package jse.ase;

import jep.JepException;
import jep.NDArray;
import jep.python.PyCallable;
import jep.python.PyObject;
import jse.atom.IAtomData;
import jse.atom.IPotential;
import jse.code.collection.ISlice;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * <a href="https://wiki.fysik.dtu.dk/ase/_modules/ase/calculators/calculator.html#Calculator">
 * {@code ase.calculators.calculator.Calculator} </a> 的 jse
 * 实现，继承了 {@link IPotential} 用于保证使用方法和 jse 中的势函数一致。
 * <p>
 * 此实现仅仅只是 ase 计算器对象的包装类，用于方便使用 jse 中计算能量和力的模式来使用 ase 计算器。
 * <p>
 * 要求系统有 python 环境并且安装了
 * <a href="https://wiki.fysik.dtu.dk/ase/">
 * Atomic Simulation Environment (ASE) </a>
 * <p>
 * 通过：
 * <pre> {@code
 * def jseCalc = AseCalculator.of(pyCalc)
 * } </pre>
 * 来将 {@link PyObject} 的 ase calculator 转换成 jse 的计算器（势函数），通过：
 * <pre> {@code
 * def pyCalc = jseCalc.asAseCalculator()
 * } </pre>
 * 通过 {@link IPotential} 中的通用接口来将势函数转换成 ase 计算器。
 *
 * @see IPotential IPotential: 势函数通用接口
 * @see PyObject
 * @author liqa
 */
public class AseCalculator implements IPotential {
    private final PyObject mCalc;
    private PyObject mLastAtoms = null;
    private final boolean mEnergySupport, mForceSupport, mStressSupport;
    private final boolean mPerAtomEnergySupport, mPerAtomStressSupport;
    AseCalculator(PyObject aCalc) {
        mCalc = aCalc;
        List<?> tImplementedProperties = mCalc.getAttr("implemented_properties", List.class);
        boolean tEnergySupport = false;
        boolean tForceSupport = false;
        boolean tStressSupport = false;
        boolean tPerAtomEnergySupport = false;
        boolean tPerAtomStressSupport = false;
        for (Object tProperty : tImplementedProperties) {
            if ("energy".equals(tProperty)) tEnergySupport = true;
            if ("forces".equals(tProperty)) tForceSupport = true;
            if ("stress".equals(tProperty)) tStressSupport = true;
            if ("energies".equals(tProperty)) tPerAtomEnergySupport = true;
            if ("stresses".equals(tProperty)) tPerAtomStressSupport = true;
        }
        mEnergySupport = tEnergySupport;
        mForceSupport = tForceSupport;
        mStressSupport = tStressSupport;
        mPerAtomEnergySupport = tPerAtomEnergySupport;
        mPerAtomStressSupport = tPerAtomStressSupport;
    }
    
    /**
     * 通过一个 ase 计算器的 python 对象创建一个兼容 jse 的
     * {@link IPotential} 的计算器包装对象
     * @param aCalc 需要包装的 ase 计算器
     * @return 兼容 {@link IPotential} 的 ase 计算器包装对象 {@link AseCalculator}
     */
    public static AseCalculator of(PyObject aCalc) {return new AseCalculator(aCalc);}
    
    /**
     * 获取上次进行计算内部创建的 ase Atoms 对象，用于方便获取计算的其他属性，
     * 或者借助 ase 来保存所有计算结果。
     * @return 上次进行计算内部创建的 ase Atoms 对象，如果没有进行任何计算则返回 {@code null}
     */
    public @Nullable PyObject lastAtoms() {return mLastAtoms;}
    
    /** @return {@inheritDoc} */
    @Override public boolean perAtomEnergySupport() {return mPerAtomEnergySupport;}
    /** @return {@inheritDoc} */
    @Override public boolean perAtomStressSupport() {return mPerAtomStressSupport;}
    
    /**
     * 转换为 ase 计算器，这里直接返回创建时使用的 ase 计算器对象
     * @return {@inheritDoc}
     */
    @Override public PyObject asAseCalculator() throws JepException {return mCalc;}
    
    /** 常规的 ase 计算器不支持计算部分原子能量，因此会直接抛出 {@link UnsupportedOperationException} */
    @Override public double calEnergyAt(IAtomData aAPC, ISlice aIndices) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomData {@inheritDoc}
     * @param rEnergies {@inheritDoc}
     * @param rForcesX {@inheritDoc}
     * @param rForcesY {@inheritDoc}
     * @param rForcesZ {@inheritDoc}
     * @param rVirialsXX {@inheritDoc}
     * @param rVirialsYY {@inheritDoc}
     * @param rVirialsZZ {@inheritDoc}
     * @param rVirialsXY {@inheritDoc}
     * @param rVirialsXZ {@inheritDoc}
     * @param rVirialsYZ {@inheritDoc}
     */
    @Override public void calEnergyForceVirials(IAtomData aAtomData, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ) throws JepException {
        // 这里为了兼容性，还是采用通过 atoms 来调用计算参数的实现方法
        AseAtoms tAtoms = AseAtoms.of(aAtomData);
        mLastAtoms = tAtoms.toPyObject();
        mLastAtoms.setAttr("calc", mCalc);
        // 按照难度逆序计算，可以利用 ase 计算器的缓存特性避免重复计算
        if (rVirialsXX!=null || rVirialsYY!=null || rVirialsZZ!=null || rVirialsXY!=null || rVirialsXZ!=null || rVirialsYZ!=null) {
            double tVolume = aAtomData.volume();
            if (!mStressSupport) throw new UnsupportedOperationException("calc stress not supported");
            NDArray<?> tPyStress;
            try (PyCallable tGetStress = mLastAtoms.getAttr("get_stress", PyCallable.class)) {
                tPyStress = tGetStress.callAs(NDArray.class);
            }
            Vector tStress = new Vector(tPyStress.getDimensions()[0], (double[])tPyStress.getData());
            if ((rVirialsXX==null||rVirialsXX.size()==1) && (rVirialsYY==null||rVirialsYY.size()==1) && (rVirialsZZ==null||rVirialsZZ.size()==1) &&
                (rVirialsXY==null||rVirialsXY.size()==1) && (rVirialsXZ==null||rVirialsXZ.size()==1) && (rVirialsYZ==null||rVirialsYZ.size()==1)) {
                    if (rVirialsXX != null) {rVirialsXX.set(0, -tStress.get(0)*tVolume);}
                    if (rVirialsYY != null) {rVirialsYY.set(0, -tStress.get(1)*tVolume);}
                    if (rVirialsZZ != null) {rVirialsZZ.set(0, -tStress.get(2)*tVolume);}
                    if (rVirialsXY != null) {rVirialsXY.set(0, -tStress.get(5)*tVolume);}
                    if (rVirialsXZ != null) {rVirialsXZ.set(0, -tStress.get(4)*tVolume);}
                    if (rVirialsYZ != null) {rVirialsYZ.set(0, -tStress.get(3)*tVolume);}
                } else {
                // ase 不同计算器的每原子压力定义可能不同，因此可能不会得到正确的位力，但这样统一处理可以保证使用 stress 相关接口获取到的依旧是 ase 中定义的应力
                if (!mPerAtomStressSupport) throw new UnsupportedOperationException("calc stresses not supported");
                NDArray<?> tPyStresses;
                try (PyCallable tGetStresses = mLastAtoms.getAttr("get_stresses", PyCallable.class)) {
                    tPyStresses = tGetStresses.callAs(NDArray.class);
                }
                RowMatrix tStresses = new RowMatrix(tPyStresses.getDimensions()[0], tPyStresses.getDimensions()[1], (double[])tPyStresses.getData());
                if (rVirialsXX != null) {if (rVirialsXX.size()==1) {rVirialsXX.set(0, -tStress.get(0)*tVolume);} else {rVirialsXX.fill(tStresses.col(0)); rVirialsXX.negative2this();}}
                if (rVirialsYY != null) {if (rVirialsYY.size()==1) {rVirialsYY.set(0, -tStress.get(1)*tVolume);} else {rVirialsYY.fill(tStresses.col(1)); rVirialsYY.negative2this();}}
                if (rVirialsZZ != null) {if (rVirialsZZ.size()==1) {rVirialsZZ.set(0, -tStress.get(2)*tVolume);} else {rVirialsZZ.fill(tStresses.col(2)); rVirialsZZ.negative2this();}}
                if (rVirialsXY != null) {if (rVirialsXY.size()==1) {rVirialsXY.set(0, -tStress.get(5)*tVolume);} else {rVirialsXY.fill(tStresses.col(5)); rVirialsXY.negative2this();}}
                if (rVirialsXZ != null) {if (rVirialsXZ.size()==1) {rVirialsXZ.set(0, -tStress.get(4)*tVolume);} else {rVirialsXZ.fill(tStresses.col(4)); rVirialsXZ.negative2this();}}
                if (rVirialsYZ != null) {if (rVirialsYZ.size()==1) {rVirialsYZ.set(0, -tStress.get(3)*tVolume);} else {rVirialsYZ.fill(tStresses.col(3)); rVirialsYZ.negative2this();}}
            }
        }
        if (rForcesX!=null || rForcesY!=null || rForcesZ!=null) {
            if (!mForceSupport) throw new UnsupportedOperationException("calc forces not supported");
            NDArray<?> tPyForces;
            try (PyCallable tGetForces = mLastAtoms.getAttr("get_forces", PyCallable.class)) {
                tPyForces = tGetForces.callAs(NDArray.class);
            }
            RowMatrix tForces = new RowMatrix(tPyForces.getDimensions()[0], tPyForces.getDimensions()[1], (double[])tPyForces.getData());
            if (rForcesX != null) {rForcesX.fill(tForces.col(0));}
            if (rForcesY != null) {rForcesY.fill(tForces.col(1));}
            if (rForcesZ != null) {rForcesZ.fill(tForces.col(2));}
        }
        if (rEnergies != null) {
            if (rEnergies.size() == 1) {
                if (!mEnergySupport) throw new UnsupportedOperationException("calc energy not supported");
                double tPyEnergy;
                try (PyCallable tGetEnergy = mLastAtoms.getAttr("get_potential_energy", PyCallable.class)) {
                    tPyEnergy = tGetEnergy.callAs(Number.class).doubleValue();
                }
                rEnergies.set(0, tPyEnergy);
            } else {
                if (!mPerAtomEnergySupport) throw new UnsupportedOperationException("calc energies not supported");
                NDArray<?> tPyEnergies;
                try (PyCallable tGetEnergies = mLastAtoms.getAttr("get_potential_energies", PyCallable.class)) {
                    tPyEnergies = tGetEnergies.callAs(NDArray.class);
                }
                Vector tEnergies = new Vector(tPyEnergies.getDimensions()[0], (double[])tPyEnergies.getData());
                rEnergies.fill(tEnergies);
            }
        }
    }
}
