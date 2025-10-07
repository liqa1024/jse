package jse.atom;

import com.google.common.collect.Lists;
import jep.JepException;
import jep.python.PyObject;
import jse.ase.AseAtoms;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.code.SP;
import jse.code.collection.ISlice;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 通用的势函数接口，用来统一计算原子结构的能量，力，压强的接口。
 * 功能类似 <a href="https://wiki.fysik.dtu.dk/ase/development/calculators.html">
 * ASE Calculator </a>，但这里实现不依靠原子结构，并且不缓存计算结果。
 * 因此每次计算都直接传入原子数据 {@link IAtomData} 实时计算。
 * <p>
 * 支持通过 {@link #asAseCalculator()} 来将此势函数转换为一个
 * ase 计算器，用来接入使用 ase 计算器的代码。
 *
 * @see IAtomData IAtomData: 通用的原子数据接口
 * @see IPairPotential IPairPotential: 通用的基于截断半径内原子相互作用（pair）实现的势函数
 * @author liqa
 */
public interface IPotential extends IAutoShutdown {
    /**
     * 检测此势函数是否已经关闭，默认永远为 {@code false}（即使手动调用了
     * {@link #shutdown()}），即默认不会去进行是否关闭的检测；
     * 重写此函数来在调用计算时检测是否关闭
     * @return 此势函数是否已经关闭
     */
    default boolean isShutdown() {return false;}
    @Override default void shutdown() {/**/}
    
    /** @return 是否支持计算每原子的能量 */
    default boolean perAtomEnergySupport() {return true;}
    /** @return 是否支持计算每原子的压力 */
    default boolean perAtomStressSupport() {return true;}
    /** @return 是否支持计算 9 列的每原子压力 */
    default boolean centroidPerAtomStressSupport() {return false;}
    
    /**
     * 转换为一个 <a href="https://wiki.fysik.dtu.dk/ase/development/calculators.html">
     * ase 的计算器 </a>，可以方便接入已有的代码直接计算；这里计算的压力统一按照 ase 的排序，也就是
     * {@code [xx, yy, zz, yz, xz, xy]}，确保兼容
     * <p>
     * 为了支持需要关闭的势函数，创建的 ase 计算器也提供了 {@code shutdown()} 方法来进行关闭，
     * 此时也会同步关闭内部引用的此势函数。不会实现 {@code __del__} 方法自动关闭，避免 java 这边的引用意外关闭。
     *
     * @return ase 计算器的 python 对象
     */
    default PyObject asAseCalculator() throws JepException {
        SP.Python.exec("from jsepy.atom import PotentialCalculator");
        return (PyObject)SP.Python.invoke("PotentialCalculator", this);
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
            if (tProperty.equals("energy") || tProperty.equals("energies")) tRequireEnergy = true;
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
                return rResults;
            }
            Vector tEnergies = calEnergies(tAtoms);
            double tEnergy = tEnergies.sum();
            rResults.put("energy", tEnergy);
            rResults.put("energies", tEnergies.numpy());
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
        rResults.put("energies", rEnergies.numpy());
        }
        rResults.put("forces", rForces.numpy());
        rResults.put("stress", rStress.numpy());
        if (tRequirePreAtomStress) {
        rResults.put("stresses", rStresses.numpy());
        }
        VectorCache.returnVec(rEnergies);
        MatrixCache.returnMat(rForces);
        VectorCache.returnVec(rStress);
        MatrixCache.returnMat(rStresses);
        return rResults;
    }
    
    
    /**
     * 通过此势函数计算给定原子数据 {@link IAtomData} 中每个原子的能量值
     * @param aAtomData 需要计算能量的原子数据
     * @return 每个原子能量组成的向量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default Vector calEnergies(IAtomData aAtomData) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        Vector rEnergies = VectorCache.getVec(aAtomData.atomNumber());
        calEnergyForceVirials(aAtomData, rEnergies, null, null, null, null, null, null, null, null, null);
        return rEnergies;
    }
    
    /**
     * 使用此势函数计算给定原子数据 {@link IAtomData} 的总能量
     * @param aAtomData 需要计算总能量的原子数据
     * @return 总能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergy(IAtomData aAtomData) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        Vector rTotEng = VectorCache.getVec(1);
        calEnergyForceVirials(aAtomData, rTotEng, null, null, null, null, null, null, null, null, null);
        double tTotEng = rTotEng.get(0);
        VectorCache.returnVec(rTotEng);
        return tTotEng;
    }
    
    /**
     * 使用此势函数计算给定原子数据 {@link IAtomData} 中每个原子的受力
     * @param aAtomData 需要计算力的原子数据
     * @return 每个原子力组成的矩阵，按行排列
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default RowMatrix calForces(IAtomData aAtomData) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        RowMatrix rForces = MatrixCache.getMatRow(aAtomData.atomNumber(), 3);
        calEnergyForceVirials(aAtomData, null, rForces.col(0), rForces.col(1), rForces.col(2), null, null, null, null, null, null);
        return rForces;
    }
    
    /**
     * 使用此势函数计算给定原子数据 {@link IAtomData} 中所有原子的单独应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * <p>
     * 每原子位力的定义具有一定任意性，这里的实现优先采用 GPUMD 中使用的更具对称性的定义，
     * 在多体势的情况下可能会和 LAMMPS 存在出入。具体可参考：
     * <a href="https://arxiv.org/abs/1503.06565">
     * Force and heat current formulas for many-body potentials in molecular dynamics simulation with
     * applications to thermal conductivity calculations </a>
     *
     * @param aAtomData 需要计算应力的原子数据
     * @param aIdealGas 是否考虑理想气体部分（速度效应部分），默认为 {@code false}
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz, yx, zx, zy]} 顺序排列的应力向量，
     *         如果不支持 9 列的输出则只输出 {@code [xx, yy, zz, xy, xz, yz]}
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default List<Vector> calStresses(IAtomData aAtomData, boolean aIdealGas) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        final int tAtomNum = aAtomData.atomNumber();
        final boolean tCentroid = centroidPerAtomStressSupport();
        final int tColNum = tCentroid ? 9 : 6;
        List<Vector> rStresses = VectorCache.getVec(tAtomNum, tColNum);
        calEnergyForceVirials(aAtomData, null, null, null, null, rStresses.get(0), rStresses.get(1), rStresses.get(2), rStresses.get(3), rStresses.get(4), rStresses.get(5),
                              tCentroid?rStresses.get(6):null, tCentroid?rStresses.get(7):null, tCentroid?rStresses.get(8):null);
        for (int i = 0; i < tColNum; ++i) {
            rStresses.get(i).operation().negative2this();
        }
        if (!aIdealGas || !aAtomData.hasMass() || !aAtomData.hasVelocity()) return rStresses;
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
            rStresses.get(0).add(i, -tMass * vx*vx);
            rStresses.get(1).add(i, -tMass * vy*vy);
            rStresses.get(2).add(i, -tMass * vz*vz);
            rStresses.get(3).add(i, -tMass * vx*vy);
            rStresses.get(4).add(i, -tMass * vx*vz);
            rStresses.get(5).add(i, -tMass * vy*vz);
            if (tCentroid) {
                rStresses.get(6).add(i, -tMass * vy*vx);
                rStresses.get(7).add(i, -tMass * vz*vx);
                rStresses.get(8).add(i, -tMass * vz*vy);
            }
        }
        return rStresses;
    }
    /**
     * 使用此势函数计算给定原子数据 {@link IAtomData} 中所有原子的单独应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * <p>
     * 每原子位力的定义具有一定任意性，这里的实现优先采用 GPUMD 中使用的更具对称性的定义，
     * 在多体势的情况下可能会和 LAMMPS 存在出入。具体可参考：
     * <a href="https://arxiv.org/abs/1503.06565">
     * Force and heat current formulas for many-body potentials in molecular dynamics simulation with
     * applications to thermal conductivity calculations </a>
     *
     * @param aAtomData 需要计算应力的原子数据
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz, yx, zx, zy]} 顺序排列的应力向量，
     *         如果不支持 9 列的输出则只输出 {@code [xx, yy, zz, xy, xz, yz]}
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default List<Vector> calStresses(IAtomData aAtomData) throws Exception {return calStresses(aAtomData, false);}
    
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
        List<Vector> rStresses = VectorCache.getVec(1, 6);
        calEnergyForceVirials(aAtomData, null, null, null, null, rStresses.get(0), rStresses.get(1), rStresses.get(2), rStresses.get(3), rStresses.get(4), rStresses.get(5));
        double rStressXX = -rStresses.get(0).get(0);
        double rStressYY = -rStresses.get(1).get(0);
        double rStressZZ = -rStresses.get(2).get(0);
        double rStressXY = -rStresses.get(3).get(0);
        double rStressXZ = -rStresses.get(4).get(0);
        double rStressYZ = -rStresses.get(5).get(0);
        VectorCache.returnVec(rStresses);
        double tVolume = aAtomData.volume();
        if (!aIdealGas || !aAtomData.hasMass() || !aAtomData.hasVelocity()) {
            return Lists.newArrayList(rStressXX/tVolume, rStressYY/tVolume, rStressZZ/tVolume, rStressXY/tVolume, rStressXZ/tVolume, rStressYZ/tVolume);
        }
        // 累加速度项，这里需要消去整体的平动
        final int tAtomNum = aAtomData.atomNumber();
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
        return Lists.newArrayList(rStressXX/tVolume, rStressYY/tVolume, rStressZZ/tVolume, rStressXY/tVolume, rStressXZ/tVolume, rStressYZ/tVolume);
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
     * 通过此势函数计算给定原子数据 {@link IAtomData} 指定原子的总能量
     * @param aAtomData 需要计算能量的原子数据
     * @param aIndices 需要计算的原子的索引（从 0 开始）
     * @return 指定原子的总能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    double calEnergyAt(IAtomData aAtomData, ISlice aIndices) throws Exception;
    
    /**
     * 计算移动前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @param aRestoreData 计算完成后是否还原 {@link ISettableAtomData} 的状态，默认为
     *                     {@code true}；如果关闭则会在 aAtomData 中保留移动后的结构
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(ISettableAtomData aAtomData, int aI, double aDx, double aDy, double aDz, boolean aRestoreData) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        double oEng = calEnergy(aAtomData);
        ISettableAtom tAtom = aAtomData.atom(aI);
        tAtom.plus2this(aDx, aDy, aDz);
        double nEng = calEnergy(aAtomData);
        if (aRestoreData) {
            tAtom.minus2this(aDx, aDy, aDz);
        }
        return nEng - oEng;
    }
    /**
     * 计算移动前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(ISettableAtomData aAtomData, int aI, double aDx, double aDy, double aDz) throws Exception {
        return calEnergyDiffMove(aAtomData, aI, aDx, aDy, aDz, true);
    }
    /**
     * 计算移动前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
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
        return calEnergyDiffMove(aAtomData.copy(), aI, aDx, aDy, aDz, false);
    }
    /**
     * 计算移动前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 移动原子的索引
     * @param aDxyz xyz 方向移动的距离
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(ISettableAtomData aAtomData, int aI, IXYZ aDxyz, boolean aRestoreData) throws Exception {return calEnergyDiffMove(aAtomData, aI, aDxyz.x(), aDxyz.y(), aDxyz.z(), aRestoreData);}
    /**
     * 计算移动前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 移动原子的索引
     * @param aDxyz xyz 方向移动的距离
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(ISettableAtomData aAtomData, int aI, IXYZ aDxyz) throws Exception {return calEnergyDiffMove(aAtomData, aI, aDxyz.x(), aDxyz.y(), aDxyz.z());}
    /**
     * 计算移动前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 移动原子的索引
     * @param aDxyz xyz 方向移动的距离
     * @return 移动后能量 - 移动前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffMove(IAtomData aAtomData, int aI, IXYZ aDxyz) throws Exception {return calEnergyDiffMove(aAtomData, aI, aDxyz.x(), aDxyz.y(), aDxyz.z());}
    
    /**
     * 计算交换种类前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @param aRestoreData 计算完成后是否还原 {@link ISettableAtomData} 的状态，默认为
     *                     {@code true}；如果关闭则会在 aAtomData 中保留交换后的结构
     * @return 交换后能量 - 交换前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffSwap(ISettableAtomData aAtomData, int aI, int aJ, boolean aRestoreData) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        ISettableAtom tAtomI = aAtomData.atom(aI);
        ISettableAtom tAtomJ = aAtomData.atom(aJ);
        int oTypeI = tAtomI.type();
        int oTypeJ = tAtomJ.type();
        if (oTypeI == oTypeJ) return 0.0;
        double oEng = calEnergy(aAtomData);
        tAtomI.setType(oTypeJ);
        tAtomJ.setType(oTypeI);
        double nEng = calEnergy(aAtomData);
        if (aRestoreData) {
            tAtomI.setType(oTypeI);
            tAtomJ.setType(oTypeJ);
        }
        return nEng - oEng;
    }
    /**
     * 计算交换种类前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @return 交换后能量 - 交换前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffSwap(ISettableAtomData aAtomData, int aI, int aJ) throws Exception {
        return calEnergyDiffSwap(aAtomData, aI, aJ, true);
    }
    /**
     * 计算交换种类前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @return 交换后能量 - 交换前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffSwap(IAtomData aAtomData, int aI, int aJ) throws Exception {
        return calEnergyDiffSwap(aAtomData.copy(), aI, aJ, false);
    }
    
    /**
     * 计算翻转某个元素种类前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 需要翻转种类的原子索引
     * @param aType 此原子需要翻转的种类编号，对应输入原子数据原始的种类编号，没有经过 aTypeMap（如果有的话）
     * @param aRestoreData 计算完成后是否还原 {@link ISettableAtomData} 的状态，默认为
     *                     {@code true}；如果关闭则会在 aAtomData 中保留翻转后的结构
     * @return 翻转后能量 - 翻转前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffFlip(ISettableAtomData aAtomData, int aI, int aType, boolean aRestoreData) throws Exception {
        if (isShutdown()) throw new IllegalStateException("This Potential is dead");
        ISettableAtom tAtom = aAtomData.atom(aI);
        int oType = tAtom.type();
        if (oType == aType) return 0.0;
        double oEng = calEnergy(aAtomData);
        tAtom.setType(aType);
        double nEng = calEnergy(aAtomData);
        if (aRestoreData) {
            tAtom.setType(oType);
        }
        return nEng - oEng;
    }
    /**
     * 计算翻转某个元素种类前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 需要翻转种类的原子索引
     * @param aType 此原子需要翻转的种类编号，对应输入原子数据原始的种类编号，没有经过 aTypeMap（如果有的话）
     * @return 翻转后能量 - 翻转前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffFlip(ISettableAtomData aAtomData, int aI, int aType) throws Exception {
        return calEnergyDiffFlip(aAtomData, aI, aType, true);
    }
    /**
     * 计算翻转某个元素种类前后的能量差，是否会考虑截断半径的优化则取决于具体的势函数的实现
     *
     * @param aAtomData 需要计算能量差的原子数据
     * @param aI 需要翻转种类的原子索引
     * @param aType 此原子需要翻转的种类编号，对应输入原子数据原始的种类编号，没有经过 aTypeMap（如果有的话）
     * @return 翻转后能量 - 翻转前能量
     * @throws Exception 特殊实现下可选的抛出异常
     */
    default double calEnergyDiffFlip(IAtomData aAtomData, int aI, int aType) throws Exception {
        return calEnergyDiffFlip(aAtomData.copy(), aI, aType, false);
    }
    
    /**
     * 使用此势函数计算所有需要的性质，需要注意的是，这里位力需要采用
     * lammps 一致的定义，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * <p>
     * 每原子位力的定义具有一定任意性，这里的实现优先采用 GPUMD 中使用的更具对称性的定义，
     * 在多体势的情况下可能会和 LAMMPS 存在出入。具体可参考：
     * <a href="https://arxiv.org/abs/1503.06565">
     * Force and heat current formulas for many-body potentials in molecular dynamics simulation with
     * applications to thermal conductivity calculations </a>
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
     * @param rVirialsYX 存储计算输出的 yx 分量的每原子位力值，默认为 {@code null} 表示不需要此值
     * @param rVirialsZX 存储计算输出的 zx 分量的每原子位力值，默认为 {@code null} 表示不需要此值
     * @param rVirialsZY 存储计算输出的 zy 分量的每原子位力值，默认为 {@code null} 表示不需要此值
     * @throws Exception 特殊实现下可选的抛出异常
     */
    void calEnergyForceVirials(IAtomData aAtomData, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ, @Nullable IVector rVirialsYX, @Nullable IVector rVirialsZX, @Nullable IVector rVirialsZY) throws Exception;
    /**
     * 使用此势函数计算所有需要的性质，需要注意的是，这里位力需要采用
     * lammps 一致的定义，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * <p>
     * 每原子位力的定义具有一定任意性，这里的实现优先采用 GPUMD 中使用的更具对称性的定义，
     * 在多体势的情况下可能会和 LAMMPS 存在出入。具体可参考：
     * <a href="https://arxiv.org/abs/1503.06565">
     * Force and heat current formulas for many-body potentials in molecular dynamics simulation with
     * applications to thermal conductivity calculations </a>
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
        calEnergyForceVirials(aAtomData, rEnergies, rForcesX, rForcesY, rForcesZ, rVirialsXX, rVirialsYY, rVirialsZZ, rVirialsXY, rVirialsXZ, rVirialsYZ, null, null, null);
    }
}
