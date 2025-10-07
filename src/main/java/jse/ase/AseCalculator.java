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
 * def jseCalc = new AseCalculator(pyCalc)
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
    private final boolean mKeepLastAtoms;
    private final boolean mEnergySupport, mForceSupport, mStressSupport;
    private final boolean mPerAtomEnergySupport, mPerAtomStressSupport;
    
    /**
     * 通过一个 ase 计算器的 python 对象创建一个兼容 jse 的
     * {@link IPotential} 的计算器包装对象
     * <p>
     * 当调用 {@link #shutdown()} 关闭时会同时尝试关闭内部的
     * ase 计算器引用，并移除 java 对此 python 对象的引用；
     * 这在大多数时候可以简化使用，例如在包装后不再需要保留原本的
     * calc 来后续手动关闭
     *
     * @param aCalc 需要包装的 ase 计算器
     * @param aKeepLastAtoms 是否在计算时保留创建的 atoms 对象，从而可以通过
     *                       {@link #lastAtoms()} 获取，进而得到其余性质以及进行其他操作，
     *                       这会阻止自动关闭临时 python 对象，进而增加垃圾回收的开销；默认为 {@code false}
     */
    public AseCalculator(PyObject aCalc, boolean aKeepLastAtoms) {
        mKeepLastAtoms = aKeepLastAtoms;
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
     * <p>
     * 当调用 {@link #shutdown()} 关闭时会同时尝试关闭内部的
     * ase 计算器引用，并移除 java 对此 python 对象的引用；
     * 这在大多数时候可以简化使用，例如在包装后不再需要保留原本的
     * calc 来后续手动关闭
     *
     * @param aCalc 需要包装的 ase 计算器
     */
    public AseCalculator(PyObject aCalc) {this(aCalc, false);}
    
    
    private boolean mDead = false;
    /** @return 此 ase 计算器是否已经关闭 */
    @Override public boolean isShutdown() {return mDead;}
    /**
     * 关闭此 ase 计算器，会时尝试调用内部引用的 ase
     * 计算器的 {@code release} 或 {@code shutdown}
     * 进行关闭，并同时会移除 java 对其的引用。
     */
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        // 尝试调用 release 和 shutdown 来关闭，由于 python 特性只能直接用 try 的写法
        try (PyCallable tRelease = mCalc.getAttr("release", PyCallable.class)) {
            tRelease.call();
        } catch (JepException e) {
            try (PyCallable tShutdown = mCalc.getAttr("shutdown", PyCallable.class)) {
                tShutdown.call();
            } catch (JepException ignored) {}
        }
        mCalc.close();
    }
    
    /**
     * 获取上次进行计算内部创建的 ase Atoms 对象，用于方便获取计算的其他属性，
     * 或者借助 ase 来保存所有计算结果。
     * @return 上次进行计算内部创建的 ase Atoms 对象，如果没有进行任何计算或者没开启保留则返回 {@code null}
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
    @Override public void calEnergyForceVirials(IAtomData aAtomData, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ, @Nullable IVector rVirialsYX, @Nullable IVector rVirialsZX, @Nullable IVector rVirialsZY) throws JepException {
        if (mDead) throw new IllegalStateException("This Potential is dead");
        final boolean tRequirePreAtomEnergy = rEnergies!=null && rEnergies.size()!=1;
        final boolean tRequireTotalEnergy = rEnergies!=null && rEnergies.size()==1;
        final boolean tRequireForce = rForcesX!=null || rForcesY!=null || rForcesZ!=null;
        final boolean tRequirePreAtomStress = (rVirialsXX!=null && rVirialsXX.size()!=1) || (rVirialsYY!=null && rVirialsYY.size()!=1) || (rVirialsZZ!=null && rVirialsZZ.size()!=1) || (rVirialsXY!=null && rVirialsXY.size()!=1) || (rVirialsXZ!=null && rVirialsXZ.size()!=1) || (rVirialsYZ!=null && rVirialsYZ.size()!=1);
        final boolean tRequireTotalStress = (rVirialsXX!=null && rVirialsXX.size()==1) || (rVirialsYY!=null && rVirialsYY.size()==1) || (rVirialsZZ!=null && rVirialsZZ.size()==1) || (rVirialsXY!=null && rVirialsXY.size()==1) || (rVirialsXZ!=null && rVirialsXZ.size()==1) || (rVirialsYZ!=null && rVirialsYZ.size()==1);
        // ase 计算器不支持 9 项压力，为了确保严谨语义要求输出时直接报错
        if (rVirialsYX!=null || rVirialsZX!=null || rVirialsZY!=null) throw new UnsupportedOperationException("ASE calculator not support 9 columns stresses");
        // 这里为了兼容性，还是采用通过 atoms 来调用计算参数的实现方法
        AseAtoms tAtoms = AseAtoms.of(aAtomData);
        PyObject tPyAtoms = tAtoms.toPyObject();
        if (mKeepLastAtoms) mLastAtoms = tPyAtoms;
        try {
            tPyAtoms.setAttr("calc", mCalc);
            // 按照难度逆序计算，可以利用 ase 计算器的缓存特性避免重复计算
            RowMatrix tVirials = null;
            if (tRequirePreAtomStress) {
                if (!mPerAtomStressSupport) throw new UnsupportedOperationException("calc stresses not supported");
                NDArray<?> tPyStresses;
                try (PyCallable tGetStresses = tPyAtoms.getAttr("get_stresses", PyCallable.class)) {
                    tPyStresses = tGetStresses.callAs(NDArray.class);
                }
                tVirials = new RowMatrix(tPyStresses.getDimensions()[0], tPyStresses.getDimensions()[1], (double[])tPyStresses.getData());
                tVirials.negative2this();
            }
            Vector tVirial = null;
            if (tRequireTotalStress) {
                if (!mStressSupport) throw new UnsupportedOperationException("calc stress not supported");
                NDArray<?> tPyStress;
                try (PyCallable tGetStress = tPyAtoms.getAttr("get_stress", PyCallable.class)) {
                    tPyStress = tGetStress.callAs(NDArray.class);
                }
                tVirial = new Vector(tPyStress.getDimensions()[0], (double[])tPyStress.getData());
                tVirial.negative2this();
                tVirial.multiply2this(aAtomData.volume());
            }
            if (rVirialsXX!=null) {if (rVirialsXX.size()==1) {assert tVirial!=null; rVirialsXX.set(0, tVirial.get(0));} else {assert tVirials!=null; rVirialsXX.fill(tVirials.col(0));}}
            if (rVirialsYY!=null) {if (rVirialsYY.size()==1) {assert tVirial!=null; rVirialsYY.set(0, tVirial.get(1));} else {assert tVirials!=null; rVirialsYY.fill(tVirials.col(1));}}
            if (rVirialsZZ!=null) {if (rVirialsZZ.size()==1) {assert tVirial!=null; rVirialsZZ.set(0, tVirial.get(2));} else {assert tVirials!=null; rVirialsZZ.fill(tVirials.col(2));}}
            if (rVirialsXY!=null) {if (rVirialsXY.size()==1) {assert tVirial!=null; rVirialsXY.set(0, tVirial.get(5));} else {assert tVirials!=null; rVirialsXY.fill(tVirials.col(5));}}
            if (rVirialsXZ!=null) {if (rVirialsXZ.size()==1) {assert tVirial!=null; rVirialsXZ.set(0, tVirial.get(4));} else {assert tVirials!=null; rVirialsXZ.fill(tVirials.col(4));}}
            if (rVirialsYZ!=null) {if (rVirialsYZ.size()==1) {assert tVirial!=null; rVirialsYZ.set(0, tVirial.get(3));} else {assert tVirials!=null; rVirialsYZ.fill(tVirials.col(3));}}
            
            RowMatrix tForces = null;
            if (tRequireForce) {
                if (!mForceSupport) throw new UnsupportedOperationException("calc forces not supported");
                NDArray<?> tPyForces;
                try (PyCallable tGetForces = tPyAtoms.getAttr("get_forces", PyCallable.class)) {
                    tPyForces = tGetForces.callAs(NDArray.class);
                }
                tForces = new RowMatrix(tPyForces.getDimensions()[0], tPyForces.getDimensions()[1], (double[])tPyForces.getData());
            }
            if (rForcesX!=null) {assert tForces!=null; rForcesX.fill(tForces.col(0));}
            if (rForcesY!=null) {assert tForces!=null; rForcesY.fill(tForces.col(1));}
            if (rForcesZ!=null) {assert tForces!=null; rForcesZ.fill(tForces.col(2));}
            
            Vector tEnergies = null;
            if (tRequirePreAtomEnergy) {
                if (!mPerAtomEnergySupport) throw new UnsupportedOperationException("calc energies not supported");
                NDArray<?> tPyEnergies;
                try (PyCallable tGetEnergies = tPyAtoms.getAttr("get_potential_energies", PyCallable.class)) {
                    tPyEnergies = tGetEnergies.callAs(NDArray.class);
                }
                tEnergies = new Vector(tPyEnergies.getDimensions()[0], (double[])tPyEnergies.getData());
            }
            double tEnergy = Double.NaN;
            if (tRequireTotalEnergy) {
                if (!mEnergySupport) throw new UnsupportedOperationException("calc energy not supported");
                try (PyCallable tGetEnergy = tPyAtoms.getAttr("get_potential_energy", PyCallable.class)) {
                    tEnergy = tGetEnergy.callAs(Number.class).doubleValue();
                }
            }
            if (rEnergies!=null) {if (rEnergies.size()==1) {rEnergies.set(0, tEnergy);} else {assert tEnergies!=null; rEnergies.fill(tEnergies);}}
        } finally {
            if (!mKeepLastAtoms) tPyAtoms.close();
        }
    }
}
