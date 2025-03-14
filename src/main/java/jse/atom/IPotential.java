package jse.atom;

import com.google.common.collect.Lists;
import jep.JepException;
import jep.NDArray;
import jep.python.PyObject;
import jse.ase.AseAtoms;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.code.SP;
import jse.code.collection.ISlice;
import jse.code.collection.IntList;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.IntUnaryOperator;

/**
 * 通用的势函数接口，用来统一计算原子结构的能量，力，压强的接口。
 * 功能类似 <a href="https://wiki.fysik.dtu.dk/ase/development/calculators.html">
 * ASE Calculator </a>，但这里实现不依靠原子结构，并且不缓存计算结果。
 * 因此每次计算都直接传入原子数据 {@link IAtomData} 实时计算。
 * <p>
 * 有些势函数依赖元素种类符号，因此存在符号接口 {@link IHasSymbol}，
 * 如果势函数实现了相关接口，且输入的原子数据 {@link IAtomData}
 * 包含符号信息，则会自动根据这些元素符号来重新映射种类。
 * <p>
 * 支持通过 {@link #asAseCalculator()} 来将此势函数转换为一个
 * ase 计算器，用来接入使用 ase 计算器的代码。
 * <p>
 * 目前实现统一采用 {@link AtomicParameterCalculator}
 * 来获取近邻列表信息
 *
 * @see IAtomData IAtomData: 通用的原子数据接口
 * @author liqa
 */
@ApiStatus.Experimental
public interface IPotential extends IHasSymbol, IAutoShutdown {
    
    /** @return {@inheritDoc} */
    @Override default int atomTypeNumber() {return 1;}
    /** @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类 */
    @Override default boolean hasSymbol() {return false;}
    /**
     * {@inheritDoc}
     * @param aType {@inheritDoc}
     * @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类
     */
    @Override default @Nullable String symbol(int aType) {return null;}
    /** @return {@inheritDoc}；如果存在则会自动根据元素符号重新映射种类 */
    @Override default @Nullable List<@Nullable String> symbols() {return IHasSymbol.super.symbols();}
    /**
     * 此势函数期望使用的线程数，默认永远为 {@code 1}，重写来修改内部自动创建 {@link AtomicParameterCalculator}
     * 时使用的线程数，但是否确实并行还是需要具体实现中使用 {@link AtomicParameterCalculator}
     * 的线程池来并行
     * @return 此势函数期望使用的线程数
     */
    default int threadNumber() {return 1;}
    /**
     * 检测此势函数是否已经关闭，默认永远为 {@code false}（即使手动调用了
     * {@link #shutdown()}），即默认不会去进行是否关闭的检测；
     * 重写此函数来在调用计算时检测是否关闭
     * @return 此势函数是否已经关闭
     */
    default boolean isShutdown() {return false;}
    @Override default void shutdown() {/**/}
    /**
     * 获取此势函数的（最大）截断半径，用来在计算单粒子移动、翻转、种类交换时获取较小的影响原子范围，
     * 从而加速这些计算。默认为 {@code -1}，表示不能进行这些优化
     * @return 势函数的（最大）截断半径
     */
    default double rcut() {return -1;}
    
    /** @return 是否支持计算每原子的能量，仅用于内部提示 */
    default boolean perAtomEnergySupport() {return true;}
    /** @return 是否支持计算每原子的压力，仅用于内部提示 */
    default boolean perAtomStressSupport() {return true;}
    
    /**
     * 转换为一个 <a href="https://wiki.fysik.dtu.dk/ase/development/calculators.html">
     * ase 的计算器 </a>，可以方便接入已有的代码直接计算；这里计算的压力统一按照 ase 的排序，也就是
     * {@code [xx, yy, zz, yz, xz, xy]}，确保兼容
     * <p>
     * 创建的 ase 计算器和此势函数对象为引用关系，因此两者只要关闭了其中一个，另一个也会同时关闭，因此也不实现
     * {@code __del__} 方法自动关闭，避免 java 这边的引用意外关闭。
     * @return ase 计算器的 python 对象
     */
    default PyObject asAseCalculator() throws JepException {return asAseCalculator(SP.Python.interpreter());}
    default PyObject asAseCalculator(@NotNull jep.Interpreter aInterpreter) throws JepException {
        aInterpreter.exec("from jsepy.atom import PotentialCalculator");
        return (PyObject)aInterpreter.invoke("PotentialCalculator", this);
    }
    @ApiStatus.Internal
    default Map<String, Object> calculate_(Map<String, Object> rResults, PyObject aPyAseAtoms, String[] aProperties, boolean aSystemChanges) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        boolean tAllInResults = true;
        for (String tProperty : aProperties) {
            if (!rResults.containsKey(tProperty)) {
                tAllInResults = false;
                break;
            }
        }
        if (!aSystemChanges && tAllInResults) return rResults;
        IAtomData tAtoms = AseAtoms.of(aPyAseAtoms);
        // 遍历统计需要的量
        boolean tRequireEnergy = false, tRequirePreAtomEnergy = false;
        boolean tRequireForces = false;
        boolean tRequireStress = false, tRequirePreAtomStress = false;;
        for (String tProperty : aProperties) {
            if (tProperty.equals("energy") || tProperty.equals("energies") || tProperty.equals("free_energy")) tRequireEnergy = true;
            if (tProperty.equals("forces")) tRequireForces = true;
            if (tProperty.equals("stress") || tProperty.equals("stresses")) tRequireStress = true;
            if (tProperty.equals("energies")) tRequirePreAtomEnergy = true;
            if (tProperty.equals("stresses")) tRequirePreAtomStress = true;
        }
        // 只需要能量则直接使用简单的计算能量接口
        if (!tRequireForces && !tRequireStress) {
            if (!tRequireEnergy) return rResults;
            if (!tRequirePreAtomEnergy) {
                double tEnergy = calEnergy(tAtoms);
                rResults.put("energy", tEnergy);
                rResults.put("free_energy", tEnergy);
                return rResults;
            }
            Vector tEnergies = calEnergies(tAtoms);
            double tEnergy = tEnergies.sum();
            rResults.put("energy", tEnergy);
            rResults.put("energies", new NDArray<>(tEnergies.internalData(), tEnergies.size()));
            rResults.put("free_energy", tEnergy);
            VectorCache.returnVec(tEnergies);
            return rResults;
        }
        // 其余情况则统一全部计算
        final int tAtomNum = tAtoms.atomNumber();
        Vector rEnergies = VectorCache.getZeros(tRequirePreAtomEnergy?tAtomNum:1);
        RowMatrix rForces = MatrixCache.getZerosRow(tAtomNum, 3);
        RowMatrix rStresses = MatrixCache.getZerosRow(tRequirePreAtomStress?tAtomNum:1, 6);
        calEnergyForceVirials(tAtoms, rEnergies, rForces.col(0), rForces.col(1), rForces.col(2),
                              rStresses.col(0), rStresses.col(1), rStresses.col(2), rStresses.col(5), rStresses.col(4), rStresses.col(3));
        rStresses.operation().negative2this();
        Vector rStress = VectorCache.getZeros(6);
        for (int i = 0; i < 6; ++i) {
            rStress.set(i, rStresses.col(i).sum());
        }
        rStress.div2this(tAtoms.volume());
        double tEnergy = rEnergies.sum();
        rResults.put("energy", tEnergy);
        if (tRequirePreAtomEnergy) {
        rResults.put("energies", new NDArray<>(rEnergies.internalData(), rEnergies.size()));
        }
        rResults.put("free_energy", tEnergy);
        rResults.put("forces", new NDArray<>(rForces.internalData(), rForces.rowNumber(), rForces.columnNumber()));
        rResults.put("stress", new NDArray<>(rStress.internalData(), rStress.size()));
        if (tRequirePreAtomStress) {
        rResults.put("stresses", new NDArray<>(rStresses.internalData(), rStresses.rowNumber(), rStresses.columnNumber()));
        }
        VectorCache.returnVec(rEnergies);
        MatrixCache.returnMat(rForces);
        VectorCache.returnVec(rStress);
        MatrixCache.returnMat(rStresses);
        return rResults;
    }
    
    
    /**
     * 通过此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 中每个原子的能量值
     * @param aAPC 需要计算能量的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 每个原子能量组成的向量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default Vector calEnergies(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", potential: " + atomTypeNumber());
        Vector rEnergies = VectorCache.getVec(aAPC.atomNumber());
        calEnergyForceVirials(aAPC, rEnergies, null, null, null, null, null, null, null, null, null, aTypeMap);
        return rEnergies;
    }
    /**
     * 通过此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 中每个原子的能量值
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergies(AtomicParameterCalculator, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算能量的原子参数计算器，主要通过此计算器来获取近邻列表
     * @return 每个原子能量组成的向量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default Vector calEnergies(AtomicParameterCalculator aAPC) throws Exception {return calEnergies(aAPC, type->type);}
    /**
     * 通过此势函数计算给定原子数据 {@link IAtomData} 中每个原子的能量值
     * @param aAtomData 需要计算能量的原子数据
     * @return 每个原子能量组成的向量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default Vector calEnergies(IAtomData aAtomData) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {return calEnergies(tAPC, tTypeMap);}
    }
    
    /**
     * 使用此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 的总能量
     * @param aAPC 需要计算总能量的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 总能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergy(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", potential: " + atomTypeNumber());
        Vector rTotEng = VectorCache.getVec(1);
        calEnergyForceVirials(aAPC, rTotEng, null, null, null, null, null, null, null, null, null, aTypeMap);
        double tTotEng = rTotEng.get(0);
        VectorCache.returnVec(rTotEng);
        return tTotEng;
    }
    /**
     * 使用此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 的总能量
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergy(AtomicParameterCalculator, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算总能量的原子参数计算器，主要通过此计算器来获取近邻列表
     * @return 总能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergy(AtomicParameterCalculator aAPC) throws Exception {return calEnergy(aAPC, type->type);}
    /**
     * 使用此势函数计算给定原子数据 {@link IAtomData} 的总能量
     * @param aAtomData 需要计算总能量的原子数据
     * @return 总能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergy(IAtomData aAtomData) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {return calEnergy(tAPC, tTypeMap);}
    }
    
    
    /**
     * 使用此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 中每个原子的受力
     * @param aAPC 需要计算力的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 每个原子力组成的矩阵，按行排列
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default RowMatrix calForces(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", potential: " + atomTypeNumber());
        RowMatrix rForces = MatrixCache.getMatRow(aAPC.atomNumber(), 3);
        calEnergyForceVirials(aAPC, null, rForces.col(0), rForces.col(1), rForces.col(2), null, null, null, null, null, null, aTypeMap);
        return rForces;
    }
    /**
     * 使用此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 中每个原子的受力
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calForces(AtomicParameterCalculator, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算力的原子参数计算器，主要通过此计算器来获取近邻列表
     * @return 每个原子力组成的矩阵，按行排列
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default RowMatrix calForces(AtomicParameterCalculator aAPC) throws Exception {return calForces(aAPC, type->type);}
    /**
     * 使用此势函数计算给定原子数据 {@link IAtomData} 中每个原子的受力
     * @param aAtomData 需要计算力的原子数据
     * @return 每个原子力组成的矩阵，按行排列
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default RowMatrix calForces(IAtomData aAtomData) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {return calForces(tAPC, tTypeMap);}
    }
    
    
    /**
     * 使用此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 中所有原子的单独应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAPC 需要计算应力的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力向量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default List<Vector> calStresses(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", potential: " + atomTypeNumber());
        List<Vector> rStresses = VectorCache.getVec(aAPC.atomNumber(), 6);
        calEnergyForceVirials(aAPC, null, null, null, null, rStresses.get(0), rStresses.get(1), rStresses.get(2), rStresses.get(3), rStresses.get(4), rStresses.get(5), aTypeMap);
        for (int i = 0; i < 6; ++i) {
            rStresses.get(i).operation().negative2this();
        }
        return rStresses;
    }
    /**
     * 使用此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 中所有原子的单独应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calStresses(AtomicParameterCalculator, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算应力的原子参数计算器，主要通过此计算器来获取近邻列表
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力向量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default List<Vector> calStresses(AtomicParameterCalculator aAPC) throws Exception {return calStresses(aAPC, type->type);}
    /**
     * 使用此势函数计算给定原子数据 {@link IAtomData} 中所有原子的单独应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAtomData 需要计算应力的原子数据
     * @param aIdealGas 是否考虑理想气体部分（速度效应部分），默认为 {@code false}
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力向量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default List<Vector> calStresses(IAtomData aAtomData, boolean aIdealGas) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        final int tAtomNum = aAtomData.atomNumber();
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {
            List<Vector> tStresses = calStresses(tAPC, tTypeMap);
            if (!aIdealGas || !aAtomData.hasMass() || !aAtomData.hasVelocity()) return tStresses;
            // 累加速度项，这里需要消去整体的平动
            double vxTot = 0.0, vyTot = 0.0, vzTot = 0.0;
            for (int i = 0; i < tAtomNum; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                vxTot += tAtom.vx(); vyTot += tAtom.vy(); vzTot += tAtom.vz();
            }
            vxTot /= (double)tAtomNum;
            vyTot /= (double)tAtomNum;
            vzTot /= (double)tAtomNum;
            for (int i = 0; i < tAtomNum; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                if (!tAtom.hasMass()) continue;
                double vx = tAtom.vx() - vxTot, vy = tAtom.vy() - vyTot, vz = tAtom.vz() - vzTot;
                double tMass = tAtom.mass();
                tStresses.get(0).add(i, -tMass * vx*vx);
                tStresses.get(1).add(i, -tMass * vy*vy);
                tStresses.get(2).add(i, -tMass * vz*vz);
                tStresses.get(3).add(i, -tMass * vx*vy);
                tStresses.get(4).add(i, -tMass * vx*vz);
                tStresses.get(5).add(i, -tMass * vy*vz);
            }
            return tStresses;
        }
    }
    
    /**
     * 使用此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 原子结构的应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAPC 需要计算应力的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力值
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default List<Double> calStress(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", potential: " + atomTypeNumber());
        List<Vector> rStresses = VectorCache.getVec(1, 6);
        calEnergyForceVirials(aAPC, null, null, null, null, rStresses.get(0), rStresses.get(1), rStresses.get(2), rStresses.get(3), rStresses.get(4), rStresses.get(5), aTypeMap);
        double tStressXX = -rStresses.get(0).get(0);
        double tStressYY = -rStresses.get(1).get(0);
        double tStressZZ = -rStresses.get(2).get(0);
        double tStressXY = -rStresses.get(3).get(0);
        double tStressXZ = -rStresses.get(4).get(0);
        double tStressYZ = -rStresses.get(5).get(0);
        VectorCache.returnVec(rStresses);
        double tVolume = aAPC.volume();
        tStressXX /= tVolume;
        tStressYY /= tVolume;
        tStressZZ /= tVolume;
        tStressXY /= tVolume;
        tStressXZ /= tVolume;
        tStressYZ /= tVolume;
        return Lists.newArrayList(tStressXX, tStressYY, tStressZZ, tStressXY, tStressXZ, tStressYZ);
    }
    /**
     * 使用此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 原子结构的应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calStresses(AtomicParameterCalculator, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算应力的原子参数计算器，主要通过此计算器来获取近邻列表
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力值
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default List<Double> calStress(AtomicParameterCalculator aAPC) throws Exception {return calStress(aAPC, type->type);}
    /**
     * 使用此势函数计算给定原子数据 {@link IAtomData} 原子结构的应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAtomData 需要计算应力的原子数据
     * @param aIdealGas 是否考虑理想气体部分（速度效应部分），默认为 {@code false}
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力值
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default List<Double> calStress(IAtomData aAtomData, boolean aIdealGas) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        final int tAtomNum = aAtomData.atomNumber();
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {
            List<Double> tStress = calStress(tAPC, tTypeMap);
            if (!aIdealGas || !aAtomData.hasMass() || !aAtomData.hasVelocity()) return tStress;
            double rStressXX = 0.0;
            double rStressYY = 0.0;
            double rStressZZ = 0.0;
            double rStressXY = 0.0;
            double rStressXZ = 0.0;
            double rStressYZ = 0.0;
            // 累加速度项，这里需要消去整体的平动
            double vxTot = 0.0, vyTot = 0.0, vzTot = 0.0;
            for (int i = 0; i < tAtomNum; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                vxTot += tAtom.vx(); vyTot += tAtom.vy(); vzTot += tAtom.vz();
            }
            vxTot /= (double)tAtomNum;
            vyTot /= (double)tAtomNum;
            vzTot /= (double)tAtomNum;
            for (int i = 0; i < tAtomNum; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                if (!tAtom.hasMass()) continue;
                double vx = tAtom.vx() - vxTot, vy = tAtom.vy() - vyTot, vz = tAtom.vz() - vzTot;
                double tMass = tAtom.mass();
                rStressXX -= (tMass * vx*vx);
                rStressYY -= (tMass * vy*vy);
                rStressZZ -= (tMass * vz*vz);
                rStressXY -= (tMass * vx*vy);
                rStressXZ -= (tMass * vx*vz);
                rStressYZ -= (tMass * vy*vz);
            }
            double tVolume = tAPC.volume();
            rStressXX /= tVolume;
            rStressYY /= tVolume;
            rStressZZ /= tVolume;
            rStressXY /= tVolume;
            rStressXZ /= tVolume;
            rStressYZ /= tVolume;
            tStress.set(0, tStress.get(0)+rStressXX);
            tStress.set(1, tStress.get(1)+rStressYY);
            tStress.set(2, tStress.get(2)+rStressZZ);
            tStress.set(3, tStress.get(3)+rStressXY);
            tStress.set(4, tStress.get(4)+rStressXZ);
            tStress.set(5, tStress.get(5)+rStressYZ);
            return tStress;
        }
    }
    /**
     * 使用此势函数计算给定原子数据 {@link IAtomData} 原子结构的应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAtomData 需要计算应力的原子数据
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力值
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default List<Double> calStress(IAtomData aAtomData) throws Exception {return calStress(aAtomData, false);}
    
    
    /**
     * 通过此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 指定原子的总能量
     * @param aAPC 需要计算能量的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aIndices 需要计算的原子的索引（从 0 开始）
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 指定原子的总能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    double calEnergyAt(AtomicParameterCalculator aAPC, ISlice aIndices, IntUnaryOperator aTypeMap) throws Exception;
    /**
     * 通过此势函数计算给定原子参数计算器 {@link AtomicParameterCalculator} 指定原子的总能量
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算能量的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aIndices 需要计算的原子的索引（从 0 开始）
     * @return 指定原子的总能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyAt(AtomicParameterCalculator aAPC, ISlice aIndices) throws Exception {return calEnergyAt(aAPC, aIndices, type->type);}
    /**
     * 通过此势函数计算给定原子数据 {@link IAtomData} 指定原子的总能量
     * @param aAtomData 需要计算能量的原子数据
     * @param aIndices 需要计算的原子的索引（从 0 开始）
     * @return 指定原子的总能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyAt(IAtomData aAtomData, ISlice aIndices) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {return calEnergyAt(tAPC, aIndices, tTypeMap);}
    }
    
    /**
     * 计算移动前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @param aRestoreAPC 计算完成后是否还原 {@link AtomicParameterCalculator} 的状态，默认为
     *                    {@code true}；如果关闭则会在 APC 中保留移动后的结构
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, double aDx, double aDy, double aDz, boolean aRestoreAPC, IntUnaryOperator aTypeMap) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", potential: " + atomTypeNumber());
        double tRCut = rcut();
        if (tRCut <= 0) {
            double oEng = calEnergy(aAPC, aTypeMap);
            double oX = aAPC.atomDataXYZ_().get(aI, 0), oY = aAPC.atomDataXYZ_().get(aI, 1), oZ = aAPC.atomDataXYZ_().get(aI, 2);
            double nEng = calEnergy(aAPC.setAtomXYZ(aI, oX+aDx, oY+aDy, oZ+aDx), aTypeMap);
            if (aRestoreAPC) aAPC.setAtomXYZ(aI, oX, oY, oZ);
            return nEng - oEng;
        }
        // 采用最大的截断半径从而包含所有可能涉及发生了能量变换的原子
        XYZ oXYZ = new XYZ(aAPC.atomDataXYZ_().row(aI));
        IIntVector oNL = aAPC.getNeighborList(oXYZ, tRCut);
        XYZ nXYZ = oXYZ.plus(aDx, aDy, aDz);
        IIntVector nNL = aAPC.getNeighborList(nXYZ, tRCut);
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(oNL.size());
        tNL.addAll(oNL);
        nNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        double oEng = calEnergyAt(aAPC, tNL, aTypeMap);
        double nEng = calEnergyAt(aAPC.setAtomXYZ(aI, nXYZ), tNL, aTypeMap);
        if (aRestoreAPC) aAPC.setAtomXYZ(aI, oXYZ);
        return nEng - oEng;
    }
    /**
     * 计算移动前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, double aDx, double aDy, double aDz, IntUnaryOperator aTypeMap) throws Exception {return calEnergyDiffMove(aAPC, aI, aDx, aDy, aDz, true, aTypeMap);}
    /**
     * 计算移动前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergyDiffMove(AtomicParameterCalculator, int, double, double, double, boolean, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @param aRestoreAPC 计算完成后是否还原 {@link AtomicParameterCalculator} 的状态，默认为
     *                    {@code true}；如果关闭则会在 APC 中保留移动后的结构
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, double aDx, double aDy, double aDz, boolean aRestoreAPC) throws Exception {return calEnergyDiffMove(aAPC, aI, aDx, aDy, aDz, aRestoreAPC, type->type);}
    /**
     * 计算移动前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergyDiffMove(AtomicParameterCalculator, int, double, double, double, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, double aDx, double aDy, double aDz) throws Exception {return calEnergyDiffMove(aAPC, aI, aDx, aDy, aDz, type->type);}
    /**
     * 计算移动前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 一般实现会在调用时重复创建 {@link AtomicParameterCalculator} 来构造近邻列表，可以通过直接使用
     * {@link #calEnergyDiffMove(AtomicParameterCalculator, int, double, double, double, IntUnaryOperator)}
     * 来避免这个过程，从而提高效率
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(IAtomData aAtomData, int aI, double aDx, double aDy, double aDz) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {return calEnergyDiffMove(tAPC, aI, aDx, aDy, aDz, false, tTypeMap);}
    }
    
    /**
     * 计算移动前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 移动原子的索引
     * @param aDxyz xyz 方向移动的距离
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, IXYZ aDxyz, IntUnaryOperator aTypeMap) throws Exception {return calEnergyDiffMove(aAPC, aI, aDxyz.x(), aDxyz.y(), aDxyz.z(), aTypeMap);}
    /**
     * 计算移动前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergyDiffMove(AtomicParameterCalculator, int, IXYZ, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 移动原子的索引
     * @param aDxyz xyz 方向移动的距离
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, IXYZ aDxyz) throws Exception {return calEnergyDiffMove(aAPC, aI, aDxyz, type->type);}
    /**
     * 计算移动前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 一般实现会在调用时重复创建 {@link AtomicParameterCalculator} 来构造近邻列表，可以通过直接使用
     * {@link #calEnergyDiffMove(AtomicParameterCalculator, int, IXYZ, IntUnaryOperator)}
     * 来避免这个过程，从而提高效率
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 移动原子的索引
     * @param aDxyz xyz 方向移动的距离
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(IAtomData aAtomData, int aI, IXYZ aDxyz) throws Exception {return calEnergyDiffMove(aAtomData, aI, aDxyz.x(), aDxyz.y(), aDxyz.z());}
    
    /**
     * 计算交换种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @param aRestoreAPC 计算完成后是否还原 {@link AtomicParameterCalculator} 的状态，默认为
     *                    {@code true}；如果关闭则会在 APC 中保留交换后的结构
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 交换后能量 - 交换前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffSwap(AtomicParameterCalculator aAPC, int aI, int aJ, boolean aRestoreAPC, IntUnaryOperator aTypeMap) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", potential: " + atomTypeNumber());
        int oTypeI = aAPC.atomType_().get(aI);
        int oTypeJ = aAPC.atomType_().get(aJ);
        if (oTypeI == oTypeJ) return 0.0;
        double tRCut = rcut();
        if (tRCut <= 0) {
            double oEng = calEnergy(aAPC, aTypeMap);
            double nEng = calEnergy(aAPC.setAtomType(aI, oTypeJ).setAtomType(aJ, oTypeI), aTypeMap);
            if (aRestoreAPC) aAPC.setAtomType(aI, oTypeI).setAtomType(aJ, oTypeJ);
            return nEng - oEng;
        }
        // 采用最大的截断半径从而包含所有可能涉及发生了能量变换的原子
        IIntVector iNL = aAPC.getNeighborList(aI, tRCut);
        IIntVector jNL = aAPC.getNeighborList(aJ, tRCut);
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(iNL.size()+1);
        tNL.add(aI);
        tNL.addAll(iNL);
        if (!tNL.contains(aJ)) tNL.add(aJ);
        jNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        double oEng = calEnergyAt(aAPC, tNL, aTypeMap);
        double nEng = calEnergyAt(aAPC.setAtomType(aI, oTypeJ).setAtomType(aJ, oTypeI), tNL, aTypeMap);
        if (aRestoreAPC) aAPC.setAtomType(aI, oTypeI).setAtomType(aJ, oTypeJ);
        return nEng - oEng;
    }
    /**
     * 计算交换种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 交换后能量 - 交换前能量
     */
    default double calEnergyDiffSwap(AtomicParameterCalculator aAPC, int aI, int aJ, IntUnaryOperator aTypeMap) throws Exception {return calEnergyDiffSwap(aAPC, aI, aJ, true, aTypeMap);}
    /**
     * 计算交换种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergyDiffSwap(AtomicParameterCalculator, int, int, boolean, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @param aRestoreAPC 计算完成后是否还原 {@link AtomicParameterCalculator} 的状态，默认为
     *                    {@code true}；如果关闭则会在 APC 中保留交换后的结构
     * @return 交换后能量 - 交换前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffSwap(AtomicParameterCalculator aAPC, int aI, int aJ, boolean aRestoreAPC) throws Exception {return calEnergyDiffSwap(aAPC, aI, aJ, aRestoreAPC, type->type);}
    /**
     * 计算交换种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergyDiffSwap(AtomicParameterCalculator, int, int, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @return 交换后能量 - 交换前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffSwap(AtomicParameterCalculator aAPC, int aI, int aJ) throws Exception {return calEnergyDiffSwap(aAPC, aI, aJ, type->type);}
    /**
     * 计算交换种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 一般实现会在调用时重复创建 {@link AtomicParameterCalculator} 来构造近邻列表，可以通过直接使用
     * {@link #calEnergyDiffSwap(AtomicParameterCalculator, int, int, IntUnaryOperator)}
     * 来避免这个过程，从而提高效率
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @return 交换后能量 - 交换前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffSwap(IAtomData aAtomData, int aI, int aJ) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {return calEnergyDiffSwap(tAPC, aI, aJ, false, tTypeMap);}
    }
    
    /**
     * 计算翻转摸个元素种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 需要翻转种类的原子索引
     * @param aType 此原子需要翻转的种类编号，对应输入原子数据原始的种类编号，没有经过 aTypeMap（如果有的话）
     * @param aRestoreAPC 计算完成后是否还原 {@link AtomicParameterCalculator} 的状态，默认为
     *                    {@code true}；如果关闭则会在 APC 中保留翻转后的结构
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 翻转后能量 - 翻转前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffFlip(AtomicParameterCalculator aAPC, int aI, int aType, boolean aRestoreAPC, IntUnaryOperator aTypeMap) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", potential: " + atomTypeNumber());
        int oType = aAPC.atomType_().get(aI);
        if (oType == aType) return 0.0;
        double tRCut = rcut();
        if (tRCut <= 0) {
            double oEng = calEnergy(aAPC, aTypeMap);
            double nEng = calEnergy(aAPC.setAtomType(aI, aType), aTypeMap);
            if (aRestoreAPC) aAPC.setAtomType(aI, oType);
            return nEng - oEng;
        }
        // 采用最大的截断半径从而包含所有可能涉及发生了能量变换的原子
        IIntVector iNL = aAPC.getNeighborList(aI, tRCut);
        // 增加一个自身，这里简单创建新的列表实现
        final IntList tNL = new IntList(iNL.size()+1);
        tNL.add(aI);
        tNL.addAll(iNL);
        double oEng = calEnergyAt(aAPC, tNL, aTypeMap);
        double nEng = calEnergyAt(aAPC.setAtomType(aI, aType), tNL, aTypeMap);
        if (aRestoreAPC) aAPC.setAtomType(aI, oType);
        return nEng - oEng;
    }
    /**
     * 计算翻转摸个元素种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 需要翻转种类的原子索引
     * @param aType 此原子需要翻转的种类编号，对应输入原子数据原始的种类编号，没有经过 aTypeMap（如果有的话）
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 翻转后能量 - 翻转前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffFlip(AtomicParameterCalculator aAPC, int aI, int aType, IntUnaryOperator aTypeMap) throws Exception {return calEnergyDiffFlip(aAPC, aI, aType, true, aTypeMap);}
    /**
     * 计算翻转摸个元素种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergyDiffFlip(AtomicParameterCalculator, int, int, boolean, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 需要翻转种类的原子索引
     * @param aType 此原子需要翻转的种类编号，对应输入原子数据原始的种类编号，没有经过 aTypeMap（如果有的话）
     * @param aRestoreAPC 计算完成后是否还原 {@link AtomicParameterCalculator} 的状态，默认为
     *                    {@code true}；如果关闭则会在 APC 中保留翻转后的结构
     * @return 翻转后能量 - 翻转前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffFlip(AtomicParameterCalculator aAPC, int aI, int aType, boolean aRestoreAPC) throws Exception {return calEnergyDiffFlip(aAPC, aI, aType, aRestoreAPC, type->type);}
    /**
     * 计算翻转摸个元素种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergyDiffFlip(AtomicParameterCalculator, int, int, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算能量差的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param aI 需要翻转种类的原子索引
     * @param aType 此原子需要翻转的种类编号，对应输入原子数据原始的种类编号，没有经过 aTypeMap（如果有的话）
     * @return 翻转后能量 - 翻转前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffFlip(AtomicParameterCalculator aAPC, int aI, int aType) throws Exception {return calEnergyDiffFlip(aAPC, aI, aType, type->type);}
    /**
     * 计算翻转摸个元素种类前后的能量差，默认实现会通过调用 {@link #calEnergyAt(AtomicParameterCalculator, ISlice, IntUnaryOperator)}
     * 来实现只计算移动影响的近邻列表中的原子，从而加快计算速度
     * <p>
     * 一般实现会在调用时重复创建 {@link AtomicParameterCalculator} 来构造近邻列表，可以通过直接使用
     * {@link #calEnergyDiffFlip(AtomicParameterCalculator, int, int, IntUnaryOperator)}
     * 来避免这个过程，从而提高效率
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 需要翻转种类的原子索引
     * @param aType 此原子需要翻转的种类编号，对应输入原子数据原始的种类编号，没有经过 aTypeMap（如果有的话）
     * @return 翻转后能量 - 翻转前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffFlip(IAtomData aAtomData, int aI, int aType) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {return calEnergyDiffFlip(tAPC, aI, aType, false, tTypeMap);}
    }
    
    
    /**
     * 使用此势函数计算所有需要的性质，需要注意的是，这里位力需要采用
     * lammps 一致的定义，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     *
     * @param aAPC 需要计算性质的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param rEnergies 存储计算输出的每原子能量值，{@code null} 表示不需要能量，长度为 {@code 1} 表示只需要体系的总能量
     * @param rForcesX 存储计算输出的 x 方向力值，{@code null} 表示不需要此值
     * @param rForcesY 存储计算输出的 y 方向力值，{@code null} 表示不需要此值
     * @param rForcesZ 存储计算输出的 z 方向力值，{@code null} 表示不需要此值
     * @param rVirialsXX 存储计算输出的 xx 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsYY 存储计算输出的 yy 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsZZ 存储计算输出的 zz 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsXY 存储计算输出的 xy 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsXZ 存储计算输出的 xz 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsYZ 存储计算输出的 yz 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @throws Exception 特殊实现下可选的抛出异常
     */
    void calEnergyForceVirials(AtomicParameterCalculator aAPC, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ, IntUnaryOperator aTypeMap) throws Exception;
    /**
     * 使用此势函数计算所有需要的性质，需要注意的是，这里位力需要采用
     * lammps 一致的定义，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * <p>
     * 此时由于参数计算器不会包含元素符号信息，因此不会尝试进行种类映射，为了保证种类正确可以使用
     * {@link #calEnergyForceVirials(AtomicParameterCalculator, IVector, IVector, IVector, IVector, IVector, IVector, IVector, IVector, IVector, IVector, IntUnaryOperator)}
     * 来手动指定种类编号的映射
     *
     * @param aAPC 需要计算性质的原子参数计算器，主要通过此计算器来获取近邻列表
     * @param rEnergies 存储计算输出的每原子能量值，{@code null} 表示不需要能量，长度为 {@code 1} 表示只需要体系的总能量
     * @param rForcesX 存储计算输出的 x 方向力值，{@code null} 表示不需要此值
     * @param rForcesY 存储计算输出的 y 方向力值，{@code null} 表示不需要此值
     * @param rForcesZ 存储计算输出的 z 方向力值，{@code null} 表示不需要此值
     * @param rVirialsXX 存储计算输出的 xx 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsYY 存储计算输出的 yy 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsZZ 存储计算输出的 zz 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsXY 存储计算输出的 xy 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsXZ 存储计算输出的 xz 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsYZ 存储计算输出的 yz 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default void calEnergyForceVirials(AtomicParameterCalculator aAPC, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ) throws Exception {
        calEnergyForceVirials(aAPC, rEnergies, rForcesX, rForcesY, rForcesZ, rVirialsXX, rVirialsYY, rVirialsZZ, rVirialsXY, rVirialsXZ, rVirialsYZ, type->type);
    }
    /**
     * 使用此势函数计算所有需要的性质，需要注意的是，这里位力需要采用
     * lammps 一致的定义，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     *
     * @param aAtomData 需要计算性质的原子数据
     * @param rEnergies 存储计算输出的每原子能量值，{@code null} 表示不需要能量，长度为 {@code 1} 表示只需要体系的总能量
     * @param rForcesX 存储计算输出的 x 方向力值，{@code null} 表示不需要此值
     * @param rForcesY 存储计算输出的 y 方向力值，{@code null} 表示不需要此值
     * @param rForcesZ 存储计算输出的 z 方向力值，{@code null} 表示不需要此值
     * @param rVirialsXX 存储计算输出的 xx 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsYY 存储计算输出的 yy 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsZZ 存储计算输出的 zz 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsXY 存储计算输出的 xy 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsXZ 存储计算输出的 xz 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @param rVirialsYZ 存储计算输出的 yz 分量的每原子位力值，{@code null} 表示不需要此值，长度为 {@code 1} 表示只需要此分量下体系的总位力值
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default void calEnergyForceVirials(IAtomData aAtomData, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, threadNumber())) {calEnergyForceVirials(tAPC, rEnergies, rForcesX, rForcesY, rForcesZ, rVirialsXX, rVirialsYY, rVirialsZZ, rVirialsXY, rVirialsXZ, rVirialsYZ, tTypeMap);}
    }
}
