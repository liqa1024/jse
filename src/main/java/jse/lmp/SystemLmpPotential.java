package jse.lmp;

import jse.atom.IAtomData;
import jse.atom.IBox;
import jse.atom.IPotential;
import jse.atom.XYZ;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.math.table.ITable;
import jse.math.vector.*;
import jse.system.ISystemExecutor;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static jse.code.Conf.WORKING_DIR_OF;

/**
 * 通过执行系统命令执行 lammps 实现的 lammps 势函数，用来方便使用
 * lammps 计算各种 lammps 中支持的势。
 * <p>
 * 由于需要通过系统命令执行，因此相比 {@link LmpPotential}
 * 效率更低，但会有更高的兼容性。
 * <p>
 * 仅用于直接计算属性，如果希望进行 lammps 模拟则应当直接使用
 * {@link ISystemExecutor} 来直接运行 lammps。
 * <p>
 * 会自动进行单位转换，确保和通用 {@link IPotential}
 * 的结果一致，即力和应力的单位会统一通过能量和距离来得到。
 * 例如对于 {@code metal} 单位，lammps 压力单位为
 * {@code bar}，而这里会统一进行单位转换，确保单位为 {@code eV/Å^3}
 * <p>
 * 此类线程安全，包括多个线程同时访问同一个实例
 *
 * @see LmpPotential LmpPotential: 原生调用 lammps 实现的 lammps 势函数
 * @see IPotential IPotential: 通用的势函数接口
 * @author liqa
 */
public class SystemLmpPotential extends AbstractLmpPotential {
    private final String mWorkingDir;
    private final ISystemExecutor mExec;
    private final boolean mShutdownExec;
    /**
     * 根据输入的 aPairStyle 和 aPairCoeff 创建一个命令运行 lammps 计算的势函数
     * @param aPairStyle 希望使用的 lammps 中的 pair 样式，对应 lammps 命令 {@code pair_style}
     * @param aPairCoeff lammps pair 需要设置的参数，对应 lammps 命令 {@code pair_coeff}
     * @param aExec 希望使用的系统命令执行器 {@link ISystemExecutor}，默认为 {@link OS#EXEC}
     * @param aShutdownExec 是否会在关闭此势函数时，自动关闭内部的系统执行器，默认在手动传入 aExec 时为 {@code true}，不传入时为 {@code false}
     */
    public SystemLmpPotential(String aPairStyle, String aPairCoeff, ISystemExecutor aExec, boolean aShutdownExec) throws IOException {
        super(aPairStyle, aPairCoeff);
        mExec = aExec;
        mShutdownExec = aShutdownExec;
        // 使用相对路径提高 exec 的兼容性
        mWorkingDir = WORKING_DIR_OF("LMP@"+ UT.Code.randID(), true);
        IO.makeDir(mWorkingDir);
    }
    /**
     * 根据输入的 aPairStyle 和 aPairCoeff 创建一个命令运行 lammps 计算的势函数
     * @param aPairStyle 希望使用的 lammps 中的 pair 样式，对应 lammps 命令 {@code pair_style}
     * @param aPairCoeff lammps pair 需要设置的参数，对应 lammps 命令 {@code pair_coeff}
     * @param aExec 希望使用的系统命令执行器 {@link ISystemExecutor}，默认为 {@link OS#EXEC}，默认在关闭时会同时自动关闭
     */
    public SystemLmpPotential(String aPairStyle, String aPairCoeff, ISystemExecutor aExec) throws IOException {this(aPairStyle, aPairCoeff, aExec, true);}
    /**
     * 根据输入的 aPairStyle 和 aPairCoeff 创建一个命令运行 lammps 计算的势函数
     * @param aPairStyle 希望使用的 lammps 中的 pair 样式，对应 lammps 命令 {@code pair_style}
     * @param aPairCoeff lammps pair 需要设置的参数，对应 lammps 命令 {@code pair_coeff}
     */
    public SystemLmpPotential(String aPairStyle, String aPairCoeff) throws IOException {this(aPairStyle, aPairCoeff, OS.EXEC, false);}
    
    private String mLmpCommand = "lmp";
    /**
     * 设置运行 lammps 的命令，默认为 {@code lmp}
     * @param aLmpCommand 需要设置的运行 lammps 的命令
     * @return 自身方便链式调用
     */
    public SystemLmpPotential setLmpCommand(String aLmpCommand) {mLmpCommand = aLmpCommand; return this;}
    
    private boolean mDead = false;
    /** @return 此 lammps 势函数是否已经关闭 */
    @Override public boolean isShutdown() {return mDead;}
    /**
     * 关闭此 lammps 势函数，会同时关闭内部使用的
     * {@link NativeLmp} 对象
     */
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        try {
            IO.removeDir(mWorkingDir);
        } catch (Exception ignored) {}
        if (mShutdownExec) mExec.shutdown();
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
    @Override public void calEnergyForceVirials(IAtomData aAtomData, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ) throws IOException {
        if (mDead) throw new IllegalStateException("This Potential is dead");
        // 统一判断需要的类型
        final boolean tRequirePreAtomEnergy = rEnergies!=null && rEnergies.size()!=1;
        final boolean tRequireTotalEnergy = rEnergies!=null && rEnergies.size()==1;
        final boolean tRequireForce = rForcesX!=null || rForcesY!=null || rForcesZ!=null;
        final boolean tRequirePreAtomStress = (rVirialsXX!=null && rVirialsXX.size()!=1) || (rVirialsYY!=null && rVirialsYY.size()!=1) || (rVirialsZZ!=null && rVirialsZZ.size()!=1) || (rVirialsXY!=null && rVirialsXY.size()!=1) || (rVirialsXZ!=null && rVirialsXZ.size()!=1) || (rVirialsYZ!=null && rVirialsYZ.size()!=1);
        final boolean tRequireTotalStress = (rVirialsXX!=null && rVirialsXX.size()==1) || (rVirialsYY!=null && rVirialsYY.size()==1) || (rVirialsZZ!=null && rVirialsZZ.size()==1) || (rVirialsXY!=null && rVirialsXY.size()==1) || (rVirialsXZ!=null && rVirialsXZ.size()==1) || (rVirialsYZ!=null && rVirialsYZ.size()==1);
        String tUniqueID = UT.Code.randID();
        // 事先准备输入 data 文件
        Lmpdat tData = Lmpdat.of(aAtomData, Vectors.ones(aAtomData.atomTypeNumber()));
        tData.ids().fill(i -> i+1); // 清空可能存在的 id，简化排序问题
        String tDataPath = mWorkingDir+"data-"+tUniqueID;
        tData.write(tDataPath);
        // 准备输入 in 文件
        List<String> rLmpIn = new ArrayList<>();
        if (mBeforeCommands != null) rLmpIn.add(mBeforeCommands);
        rLmpIn.add("units  "+mUnits);
        rLmpIn.add("boundary  p p p");
        rLmpIn.add("read_data  "+tDataPath);
        rLmpIn.add("pair_style   "+mPairStyle);
        rLmpIn.add("pair_coeff   "+mPairCoeff);
        if (mLastCommands != null) rLmpIn.add(mLastCommands);
        // 增加这个 thermo 确保势能和应力可以获取到
        List<String> rThermoStyle = new ArrayList<>(8);
        if (tRequireTotalEnergy) {
            rThermoStyle.add("pe");
        }
        if (tRequireTotalStress) {
            rThermoStyle.add("pxx");
            rThermoStyle.add("pyy");
            rThermoStyle.add("pzz");
            rThermoStyle.add("pxy");
            rThermoStyle.add("pxz");
            rThermoStyle.add("pyz");
        }
        rLmpIn.add("thermo_style  custom step "+String.join(" ", rThermoStyle));
        rLmpIn.add("thermo  1");
        rLmpIn.add("thermo_modify  format float %24.18g"); // 调整输出精度
        // 按需增加对应的 compute
        if (tRequirePreAtomEnergy) {
            rLmpIn.add("compute eng_atom all pe/atom");
        }
        if (tRequirePreAtomStress) {
            rLmpIn.add("compute stress_atom all stress/atom NULL");
        }
        String tDumpPath = mWorkingDir+"dump-"+tUniqueID;
        List<String> rDumpCustom = new ArrayList<>(10);
        if (tRequireForce) {
            rDumpCustom.add("fx");
            rDumpCustom.add("fy");
            rDumpCustom.add("fz");
        }
        if (tRequirePreAtomEnergy) {
            rDumpCustom.add("c_eng_atom");
        }
        if (tRequirePreAtomStress) {
            rDumpCustom.add("c_stress_atom[1]");
            rDumpCustom.add("c_stress_atom[2]");
            rDumpCustom.add("c_stress_atom[3]");
            rDumpCustom.add("c_stress_atom[4]");
            rDumpCustom.add("c_stress_atom[5]");
            rDumpCustom.add("c_stress_atom[6]");
        }
        rLmpIn.add("dump  1 all custom 1 "+tDumpPath+" id "+String.join(" ", rDumpCustom));
        rLmpIn.add("dump_modify  1 format float %24.18g"); // 调整输出精度
        // 通过 run 0 来触发计算
        rLmpIn.add("run  0");
        // 运行 lammps
        String tInPath = mWorkingDir+"in-"+tUniqueID;
        IO.write(tInPath, rLmpIn);
        String tLogPath = mWorkingDir+"log-"+tUniqueID;
        int tExitCode = mExec.system(mLmpCommand+" -in "+tInPath+" -log "+tLogPath+" -screen none");
        if (tExitCode != 0) throw new RuntimeException("Lammps run failed, exit code: " + tExitCode);
        
        // 直接获取结果
        Thermo tLog = Thermo.read(tLogPath);
        ITable tDump = SubLammpstrj.read(tDumpPath).asTable();
        double tEnergy = Double.NaN;
        if (tRequireTotalEnergy) {
            tEnergy = tLog.get(0, "PotEng");
        }
        double tVirialXX = Double.NaN, tVirialYY = Double.NaN, tVirialZZ = Double.NaN, tVirialXY = Double.NaN, tVirialXZ = Double.NaN, tVirialYZ = Double.NaN;
        if (tRequireTotalStress) {
            final double tVolume = aAtomData.volume();
            tVirialXX = validStressUnit(tLog.get(0, "Pxx"))*tVolume;
            tVirialYY = validStressUnit(tLog.get(0, "Pyy"))*tVolume;
            tVirialZZ = validStressUnit(tLog.get(0, "Pzz"))*tVolume;
            tVirialXY = validStressUnit(tLog.get(0, "Pxy"))*tVolume;
            tVirialXZ = validStressUnit(tLog.get(0, "Pxz"))*tVolume;
            tVirialYZ = validStressUnit(tLog.get(0, "Pyz"))*tVolume;
        }
        IVector tForcesX = null, tForcesY = null, tForcesZ = null;
        if (tRequireForce) {
            tForcesX = tDump.col("fx");
            tForcesY = tDump.col("fy");
            tForcesZ = tDump.col("fz");
        }
        IVector tEnergies = null;
        if (tRequirePreAtomEnergy) {
            tEnergies = tDump.col("c_eng_atom");
        }
        IVector tVirialsXX = null, tVirialsYY = null, tVirialsZZ = null, tVirialsXY = null, tVirialsXZ = null, tVirialsYZ = null;
        if (tRequirePreAtomStress) {
            tVirialsXX = tDump.col("c_stress_atom[1]"); tVirialsXX.negative2this(); tVirialsXX.operation().map2this(this::validStressUnit);
            tVirialsYY = tDump.col("c_stress_atom[2]"); tVirialsYY.negative2this(); tVirialsYY.operation().map2this(this::validStressUnit);
            tVirialsZZ = tDump.col("c_stress_atom[3]"); tVirialsZZ.negative2this(); tVirialsZZ.operation().map2this(this::validStressUnit);
            tVirialsXY = tDump.col("c_stress_atom[4]"); tVirialsXY.negative2this(); tVirialsXY.operation().map2this(this::validStressUnit);
            tVirialsXZ = tDump.col("c_stress_atom[5]"); tVirialsXZ.negative2this(); tVirialsXZ.operation().map2this(this::validStressUnit);
            tVirialsYZ = tDump.col("c_stress_atom[6]"); tVirialsYZ.negative2this(); tVirialsYZ.operation().map2this(this::validStressUnit);
        }
        // lammps 会乱序，需要重新排序，这里可以确定可以按照 id 来排序
        IIntVector tLmpIdx2Idx = null;
        if (tRequireForce || tRequirePreAtomEnergy || tRequirePreAtomStress) {
            tLmpIdx2Idx = tDump.col("id").asIntVec().copy();
            tLmpIdx2Idx.minus2this(1);
        }
        // 如果模拟盒不是 lmpstyle，还需要对力以及压力进行转换
        IBox aBox = aAtomData.box();
        if (!aBox.isLmpStyle()) {
            LmpBox tBox = tData.box();
            XYZ tBuf0 = new XYZ(), tBuf1 = new XYZ(), tBuf2 = new XYZ();
            final int tAtomNum = aAtomData.atomNumber();
            if (tRequireForce) for (int i = 0; i < tAtomNum; ++i) {
                assert tForcesX!=null && tForcesY!=null && tForcesZ!=null;
                tBuf0.setXYZ(tForcesX.get(i), tForcesY.get(i), tForcesZ.get(i));
                tBox.toDirect(tBuf0);
                aBox.toCartesian(tBuf0);
                tForcesX.set(i, tBuf0.mX);
                tForcesY.set(i, tBuf0.mY);
                tForcesZ.set(i, tBuf0.mZ);
            }
            if (tRequireTotalStress) {
                tBuf0.setXYZ(tVirialXX, tVirialXY, tVirialXZ);
                tBuf1.setXYZ(tVirialXY, tVirialYY, tVirialYZ);
                tBuf2.setXYZ(tVirialXZ, tVirialYZ, tVirialZZ);
                rotateVirial(tBox, aBox, tBuf0, tBuf1, tBuf2);
                tVirialXX = tBuf0.mX; tVirialYY = tBuf1.mY; tVirialZZ = tBuf2.mZ;
                tVirialXY = tBuf0.mY; tVirialXZ = tBuf0.mZ; tVirialYZ = tBuf1.mZ;
            }
            if (tRequirePreAtomStress) for (int i = 0; i < tAtomNum; ++i) {
                assert tVirialsXX!=null && tVirialsYY!=null && tVirialsZZ!=null && tVirialsXY!=null && tVirialsXZ!=null && tVirialsYZ!=null;
                tBuf0.setXYZ(tVirialsXX.get(i), tVirialsXY.get(i), tVirialsXZ.get(i));
                tBuf1.setXYZ(tVirialsXY.get(i), tVirialsYY.get(i), tVirialsYZ.get(i));
                tBuf2.setXYZ(tVirialsXZ.get(i), tVirialsYZ.get(i), tVirialsZZ.get(i));
                rotateVirial(tBox, aBox, tBuf0, tBuf1, tBuf2);
                tVirialsXX.set(i, tBuf0.mX); tVirialsYY.set(i, tBuf1.mY); tVirialsZZ.set(i, tBuf2.mZ);
                tVirialsXY.set(i, tBuf0.mY); tVirialsXZ.set(i, tBuf0.mZ); tVirialsYZ.set(i, tBuf1.mZ);
            }
        }
        // 设置结果输出
        if (rEnergies!=null) {if (rEnergies.size()==1) {rEnergies.set(0, tEnergy);} else {assert tEnergies!=null; rEnergies.putAt(tLmpIdx2Idx, tEnergies);}}
        if (rForcesX!=null) {assert tForcesX!=null; rForcesX.putAt(tLmpIdx2Idx, tForcesX);}
        if (rForcesY!=null) {assert tForcesY!=null; rForcesY.putAt(tLmpIdx2Idx, tForcesY);}
        if (rForcesZ!=null) {assert tForcesZ!=null; rForcesZ.putAt(tLmpIdx2Idx, tForcesZ);}
        if (rVirialsXX!=null) {if (rVirialsXX.size()==1) {rVirialsXX.set(0, tVirialXX);} else {assert tVirialsXX!=null; rVirialsXX.putAt(tLmpIdx2Idx, tVirialsXX);}}
        if (rVirialsYY!=null) {if (rVirialsYY.size()==1) {rVirialsYY.set(0, tVirialYY);} else {assert tVirialsYY!=null; rVirialsYY.putAt(tLmpIdx2Idx, tVirialsYY);}}
        if (rVirialsZZ!=null) {if (rVirialsZZ.size()==1) {rVirialsZZ.set(0, tVirialZZ);} else {assert tVirialsZZ!=null; rVirialsZZ.putAt(tLmpIdx2Idx, tVirialsZZ);}}
        if (rVirialsXY!=null) {if (rVirialsXY.size()==1) {rVirialsXY.set(0, tVirialXY);} else {assert tVirialsXY!=null; rVirialsXY.putAt(tLmpIdx2Idx, tVirialsXY);}}
        if (rVirialsXZ!=null) {if (rVirialsXZ.size()==1) {rVirialsXZ.set(0, tVirialXZ);} else {assert tVirialsXZ!=null; rVirialsXZ.putAt(tLmpIdx2Idx, tVirialsXZ);}}
        if (rVirialsYZ!=null) {if (rVirialsYZ.size()==1) {rVirialsYZ.set(0, tVirialYZ);} else {assert tVirialsYZ!=null; rVirialsYZ.putAt(tLmpIdx2Idx, tVirialsYZ);}}
    }
}
