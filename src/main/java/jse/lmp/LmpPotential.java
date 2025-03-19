package jse.lmp;

import jse.atom.*;
import jse.code.collection.ISlice;
import jse.math.MathEX;
import jse.math.matrix.RowMatrix;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vector;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static jse.code.CS.*;

/**
 * 基于 {@link NativeLmp} 实现的势函数，用来方便使用 lammps
 * 计算各种 lammps 中支持的势。
 * <p>
 * 仅用于直接计算属性，如果希望进行 lammps 模拟则应当直接使用
 * {@link NativeLmp} 中的相关接口。
 * <p>
 * 会自动进行单位转换，确保和通用 {@link IPotential}
 * 的结果一致，即力和应力的单位会统一通过能量和距离来得到。
 * 例如对于 {@code metal} 单位，lammps 压力单位为
 * {@code bar}，而这里会统一进行单位转换，确保单位为 {@code eV/Å^3}
 *
 * @see NativeLmp NativeLmp: 原生调用 lammps 接口
 * @see IPotential IPotential: 通用的势函数接口
 * @author liqa
 */
public class LmpPotential implements IPotential {
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link LmpPotential} 相关的 JNI 库是否已经初始化完成，主要用于辅助初始化 {@link NativeLmp} */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link LmpPotential} 相关的 JNI 库，主要用于辅助初始化 {@link NativeLmp} */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            if (!INITIALIZED) String.valueOf(_INIT_FLAG);
        }
    }
    private final static boolean _INIT_FLAG;
    static {
        InitHelper.INITIALIZED = true;
        // 确保 NativeLmp 已经确实初始化
        NativeLmp.InitHelper.init();
        _INIT_FLAG = false;
    }
    
    private final static String[] LMP_ARGS = {"-log", "none", "-screen", "none"};
    private final static String DEFAULT_UNITS = "metal";
    /** 将 lammps metal 单位制中的 bar 转换成 ev/Å^3 需要乘的倍数 */
    public final static double BAR_TO_EV = UNITS.get("bar");
    /** 将 lammps real 单位制中的 atmospheres 转换成 (kcal/mol)/Å^3 需要乘的倍数 */
    public final static double ATM_TO_KCAL = 1.01325*BAR_TO_EV*EV_TO_KCAL;
    /** 将 lammps electron 单位制中的 Pascal 转换成 Hartree/Bohr^3 需要乘的倍数 */
    public final static double PA_TO_HARTREE = UNITS.get("Pascal") / UNITS.get("Hartree") * MathEX.Fast.pow3(UNITS.get("Bohr"));
    
    private final @NotNull String mPairStyle;
    private final @NotNull String mPairCoeff;
    private final NativeLmp mLmp;
    /**
     * 根据输入的 aPairStyle 和 aPairCoeff 创建一个原生调用 lammps 计算的势函数
     * @param aPairStyle 希望使用的 lammps 中的 pair 样式，对应 lammps 命令 {@code pair_style}
     * @param aPairCoeff lammps pair 需要设置的参数，对应 lammps 命令 {@code pair_coeff}
     * @param aComm 希望使用的 {@link MPI.Comm}，默认为 {@code null}，当有 MPI 支持时会采用 {@link MPI.Comm#WORLD}
     */
    @SuppressWarnings("NullableProblems")
    public LmpPotential(String aPairStyle, String aPairCoeff, @Nullable MPI.Comm aComm) throws LmpException {
        if (aPairStyle==null || aPairCoeff==null) throw new NullPointerException();
        mPairStyle = aPairStyle;
        mPairCoeff = aPairCoeff;
        mLmp = new NativeLmp(LMP_ARGS, aComm);
    }
    /**
     * 根据输入的 aPairStyle 和 aPairCoeff 创建一个原生调用 lammps 计算的势函数
     * @param aPairStyle 希望使用的 lammps 中的 pair 样式，对应 lammps 命令 {@code pair_style}
     * @param aPairCoeff lammps pair 需要设置的参数，对应 lammps 命令 {@code pair_coeff}
     */
    public LmpPotential(String aPairStyle, String aPairCoeff) throws LmpException {this(aPairStyle, aPairCoeff, null);}
    
    private @Nullable String mBeforeCommands = null;
    public LmpPotential setBeforeCommands(String aCommands) {mBeforeCommands = aCommands; return this;}
    private @Nullable String mLastCommands = null;
    public LmpPotential setLastCommands(String aCommands) {mLastCommands = aCommands; return this;}
    
    private String mUnits = DEFAULT_UNITS;
    /**
     * 设置内部 lammps 计算采用的单位，默认为 {@code metal}。
     * <p>
     * 注意设置单位仅仅影响能量和长度的单位，压强和力会统一采用推导的形式。
     * 因此计算输出的单位不一定会是 lammps 设定的单位，例如对于 {@code metal}
     * 单位，lammps 压力单位为 {@code bar}，而这里会统一进行单位转换，确保压力单位为
     * {@code eV/Å^3}（其余单位恰好一致）
     *
     * @param aUnits 需要设置的单位
     * @return 自身方便链式调用
     */
    public LmpPotential setUnits(String aUnits) {mUnits = aUnits; return this;}
    /** @return 内部 lammps 计算采用的单位，默认为 {@code metal} */
    public String units() {return mUnits;}
    
    private double validStressUnit(double aLmpStress) {
        switch (mUnits) {
        case "metal": {return aLmpStress*BAR_TO_EV;}
        case "real": {return aLmpStress*ATM_TO_KCAL;}
        case "electron": {return aLmpStress*PA_TO_HARTREE;}
        default: {return aLmpStress;}
        }
    }
    @SuppressWarnings("SuspiciousNameCombination")
    private void rotateVirial(IBox aBoxIn, IBox aBoxOut, XYZ rBuf0, XYZ rBuf1, XYZ rBuf2) {
        // 应力需要这样旋转变换两次
        aBoxIn.toDirect(rBuf0);
        aBoxIn.toDirect(rBuf1);
        aBoxIn.toDirect(rBuf2);
        aBoxOut.toCartesian(rBuf0);
        aBoxOut.toCartesian(rBuf1);
        aBoxOut.toCartesian(rBuf2);
        double
        tV = rBuf0.mY; rBuf0.mY = rBuf1.mX; rBuf1.mX = tV;
        tV = rBuf0.mZ; rBuf0.mZ = rBuf2.mX; rBuf2.mX = tV;
        tV = rBuf1.mZ; rBuf1.mZ = rBuf2.mY; rBuf2.mY = tV;
        aBoxIn.toDirect(rBuf0);
        aBoxIn.toDirect(rBuf1);
        aBoxIn.toDirect(rBuf2);
        aBoxOut.toCartesian(rBuf0);
        aBoxOut.toCartesian(rBuf1);
        aBoxOut.toCartesian(rBuf2);
    }
    
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
        mLmp.shutdown();
    }
    
    /** 常规的 lammps 不支持计算部分原子能量，因此会直接抛出 {@link UnsupportedOperationException} */
    @Override public double calEnergyAt(IAtomData aAPC, ISlice aIndices) {throw new UnsupportedOperationException();}
    
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
    @Override public void calEnergyForceVirials(IAtomData aAtomData, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ) throws LmpException, MPIException {
        if (mDead) throw new IllegalStateException("This Potential is dead");
        // 统一判断需要的类型
        final boolean tRequirePreAtomEnergy = rEnergies!=null && rEnergies.size()!=1;
        final boolean tRequireTotalEnergy = rEnergies!=null && rEnergies.size()==1;
        final boolean tRequireForce = rForcesX!=null || rForcesY!=null || rForcesZ!=null;
        final boolean tRequirePreAtomStress = (rVirialsXX!=null && rVirialsXX.size()!=1) || (rVirialsYY!=null && rVirialsYY.size()!=1) || (rVirialsZZ!=null && rVirialsZZ.size()!=1) || (rVirialsXY!=null && rVirialsXY.size()!=1) || (rVirialsXZ!=null && rVirialsXZ.size()!=1) || (rVirialsYZ!=null && rVirialsYZ.size()!=1);
        final boolean tRequireTotalStress = (rVirialsXX!=null && rVirialsXX.size()==1) || (rVirialsYY!=null && rVirialsYY.size()==1) || (rVirialsZZ!=null && rVirialsZZ.size()==1) || (rVirialsXY!=null && rVirialsXY.size()==1) || (rVirialsXZ!=null && rVirialsXZ.size()==1) || (rVirialsYZ!=null && rVirialsYZ.size()==1);
        // 调用 lammps 计算
        mLmp.clear();
        if (mBeforeCommands != null) mLmp.commands(mBeforeCommands);
        mLmp.command("units  "+mUnits);
        mLmp.command("boundary  p p p");
        mLmp.loadData(aAtomData, true); // 统一不需要 id 信息，简化排序问题
        // 补充可能不存在的质量信息，只是例行设置，不影响结果
        final int tAtomTypeNum = aAtomData.atomTypeNumber();
        for (int tType = 1; tType <= tAtomTypeNum; ++tType) {
            double tMass = aAtomData.mass(tType);
            if (Double.isNaN(tMass)) mLmp.command(String.format("mass  %d 1.0", tType));
        }
        mLmp.command("pair_style   "+mPairStyle);
        mLmp.command("pair_coeff   "+mPairCoeff);
        if (mLastCommands != null) mLmp.commands(mLastCommands);
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
        mLmp.command("thermo_style  custom step "+String.join(" ", rThermoStyle));
        // 按需增加对应的 compute
        if (tRequirePreAtomEnergy) {
            mLmp.command("compute eng_atom all pe/atom");
        }
        if (tRequirePreAtomStress) {
            mLmp.command("compute stress_atom all stress/atom NULL");
        }
        // 通过 run 0 来触发计算
        mLmp.command("run  0");
        // 直接获取结果
        double tEnergy = Double.NaN;
        if (tRequireTotalEnergy) {
            tEnergy = mLmp.thermoOf("pe");
        }
        double tVirialXX = Double.NaN, tVirialYY = Double.NaN, tVirialZZ = Double.NaN, tVirialXY = Double.NaN, tVirialXZ = Double.NaN, tVirialYZ = Double.NaN;
        if (tRequireTotalStress) {
            final double tVolume = aAtomData.volume();
            tVirialXX = validStressUnit(mLmp.thermoOf("pxx"))*tVolume;
            tVirialYY = validStressUnit(mLmp.thermoOf("pyy"))*tVolume;
            tVirialZZ = validStressUnit(mLmp.thermoOf("pzz"))*tVolume;
            tVirialXY = validStressUnit(mLmp.thermoOf("pxy"))*tVolume;
            tVirialXZ = validStressUnit(mLmp.thermoOf("pxz"))*tVolume;
            tVirialYZ = validStressUnit(mLmp.thermoOf("pyz"))*tVolume;
        }
        RowMatrix tForces = null;
        if (tRequireForce) {
            tForces = mLmp.atomDataOf("f");
        }
        Vector tEnergies = null;
        if (tRequirePreAtomEnergy) {
            tEnergies = mLmp.computeOf("eng_atom", NativeLmp.LMP_STYLE_ATOM, NativeLmp.LMP_TYPE_VECTOR).asVecRow();
        }
        RowMatrix tVirials = null;
        if (tRequirePreAtomStress) {
            tVirials = mLmp.computeOf("stress_atom", NativeLmp.LMP_STYLE_ATOM, NativeLmp.LMP_TYPE_ARRAY);
            tVirials.negative2this();
            tVirials.operation().map2this(this::validStressUnit);
        }
        // lammps 会乱序，需要重新排序，这里可以确定可以按照 id 来排序
        IntVector tLmpIdx2Idx = null;
        if (tRequireForce || tRequirePreAtomEnergy || tRequirePreAtomStress) {
            tLmpIdx2Idx = mLmp.atomIntDataOf("id").asVecRow();
            tLmpIdx2Idx.minus2this(1);
        }
        mLmp.clear();
        // 如果模拟盒不是 lmpstyle，还需要对力以及压力进行转换
        IBox aBox = aAtomData.box();
        if (!aBox.isLmpStyle()) {
            LmpBox tBox = LmpBox.of(aBox);
            XYZ tBuf0 = new XYZ(), tBuf1 = new XYZ(), tBuf2 = new XYZ();
            final int tAtomNum = aAtomData.atomNumber();
            if (tRequireForce) for (int i = 0; i < tAtomNum; ++i) {
                assert tForces != null;
                tBuf0.setXYZ(tForces.get(i, 0), tForces.get(i, 1), tForces.get(i, 2));
                tBox.toDirect(tBuf0);
                aBox.toCartesian(tBuf0);
                tForces.set(i, 0, tBuf0.mX);
                tForces.set(i, 1, tBuf0.mY);
                tForces.set(i, 2, tBuf0.mZ);
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
                assert tVirials != null;
                tBuf0.setXYZ(tVirials.get(i, 0), tVirials.get(i, 3), tVirials.get(i, 4));
                tBuf1.setXYZ(tVirials.get(i, 3), tVirials.get(i, 1), tVirials.get(i, 5));
                tBuf2.setXYZ(tVirials.get(i, 4), tVirials.get(i, 5), tVirials.get(i, 2));
                rotateVirial(tBox, aBox, tBuf0, tBuf1, tBuf2);
                tVirials.set(i, 0, tBuf0.mX); tVirials.set(i, 1, tBuf1.mY); tVirials.set(i, 2, tBuf2.mZ);
                tVirials.set(i, 3, tBuf0.mY); tVirials.set(i, 4, tBuf0.mZ); tVirials.set(i, 5, tBuf1.mZ);
            }
        }
        // 设置结果输出
        if (rEnergies!=null) {if (rEnergies.size()==1) {rEnergies.set(0, tEnergy);} else {assert tEnergies!=null && tLmpIdx2Idx!=null; rEnergies.putAt(tLmpIdx2Idx, tEnergies);}}
        if (rForcesX!=null) {assert tForces!=null; rForcesX.putAt(tLmpIdx2Idx, tForces.col(0));}
        if (rForcesY!=null) {assert tForces!=null; rForcesY.putAt(tLmpIdx2Idx, tForces.col(1));}
        if (rForcesZ!=null) {assert tForces!=null; rForcesZ.putAt(tLmpIdx2Idx, tForces.col(2));}
        if (rVirialsXX!=null) {if (rVirialsXX.size()==1) {rVirialsXX.set(0, tVirialXX);} else {assert tVirials!=null && tLmpIdx2Idx!=null; rVirialsXX.putAt(tLmpIdx2Idx, tVirials.col(0));}}
        if (rVirialsYY!=null) {if (rVirialsYY.size()==1) {rVirialsYY.set(0, tVirialYY);} else {assert tVirials!=null && tLmpIdx2Idx!=null; rVirialsYY.putAt(tLmpIdx2Idx, tVirials.col(1));}}
        if (rVirialsZZ!=null) {if (rVirialsZZ.size()==1) {rVirialsZZ.set(0, tVirialZZ);} else {assert tVirials!=null && tLmpIdx2Idx!=null; rVirialsZZ.putAt(tLmpIdx2Idx, tVirials.col(2));}}
        if (rVirialsXY!=null) {if (rVirialsXY.size()==1) {rVirialsXY.set(0, tVirialXY);} else {assert tVirials!=null && tLmpIdx2Idx!=null; rVirialsXY.putAt(tLmpIdx2Idx, tVirials.col(3));}}
        if (rVirialsXZ!=null) {if (rVirialsXZ.size()==1) {rVirialsXZ.set(0, tVirialXZ);} else {assert tVirials!=null && tLmpIdx2Idx!=null; rVirialsXZ.putAt(tLmpIdx2Idx, tVirials.col(4));}}
        if (rVirialsYZ!=null) {if (rVirialsYZ.size()==1) {rVirialsYZ.set(0, tVirialYZ);} else {assert tVirials!=null && tLmpIdx2Idx!=null; rVirialsYZ.putAt(tLmpIdx2Idx, tVirials.col(5));}}
    }
}
