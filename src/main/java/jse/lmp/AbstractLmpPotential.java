package jse.lmp;

import jse.atom.IAtomData;
import jse.atom.IBox;
import jse.atom.IPotential;
import jse.atom.XYZ;
import jse.code.collection.ISlice;
import jse.math.MathEX;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static jse.code.CS.EV_TO_KCAL;
import static jse.code.CS.UNITS;

/**
 * 通用的 lammps 势函数抽象类，用于减少重复代码
 * @author liqa
 */
@SuppressWarnings({"UnknownLanguage", "RedundantSuppression"})
abstract class AbstractLmpPotential implements IPotential {
    final static String DEFAULT_UNITS = "metal";
    /** 将 lammps metal 单位制中的 bar 转换成 ev/Å^3 需要乘的倍数 */
    public final static double BAR_TO_EV = UNITS.get("bar");
    /** 将 lammps real 单位制中的 atmospheres 转换成 (kcal/mol)/Å^3 需要乘的倍数 */
    public final static double ATM_TO_KCAL = 1.01325*BAR_TO_EV*EV_TO_KCAL;
    /** 将 lammps electron 单位制中的 Pascal 转换成 Hartree/Bohr^3 需要乘的倍数 */
    public final static double PA_TO_HARTREE = UNITS.get("Pascal") / UNITS.get("Hartree") * MathEX.Fast.pow3(UNITS.get("Bohr"));
    
    @NotNull String mPairStyle;
    @NotNull String[] mPairCoeff;
    /**
     * 根据输入的 aPairStyle 和 aPairCoeff 创建一个运行 lammps 计算的势函数
     * @param aPairStyle 希望使用的 lammps 中的 pair 样式，对应 lammps 命令 {@code pair_style}
     * @param aPairCoeff lammps pair 需要设置的参数，对应 lammps 命令 {@code pair_coeff}
     */
    @SuppressWarnings("NullableProblems")
    public AbstractLmpPotential(String aPairStyle, String... aPairCoeff) {
        if (aPairStyle==null || aPairCoeff==null) throw new NullPointerException();
        for (String tPairCoeff : aPairCoeff) {
            if (tPairCoeff == null) throw new NullPointerException();
        }
        mPairStyle = aPairStyle;
        mPairCoeff = aPairCoeff;
    }
    /**
     * 设置 lammps 中的 pair 样式，对应 lammps 命令 {@code pair_style}
     * @param aPairStyle 需要设置的 {@code pair_style}
     * @return 自身方便链式调用
     */
    public AbstractLmpPotential setPairStyle(String aPairStyle) {
        if (aPairStyle==null) throw new NullPointerException();
        mPairStyle = aPairStyle;
        return this;
    }
    /**
     * 设置 lammps pair 需要设置的参数，对应 lammps 命令 {@code pair_coeff}
     * @param aPairCoeff 需要设置的 {@code pair_coeff}
     * @return 自身方便链式调用
     */
    public AbstractLmpPotential setPairCoeff(String... aPairCoeff) {
        if (aPairCoeff==null) throw new NullPointerException();
        for (String tPairCoeff : aPairCoeff) {
            if (tPairCoeff == null) throw new NullPointerException();
        }
        mPairCoeff = aPairCoeff;
        return this;
    }
    
    @Language("lmpin") @Nullable String mBeforeCommands = null;
    /**
     * 设置需要在 lammps 运行最开始执行的命令，可以用来进行加载插件等初始化，通过换行符
     * {@code \n} 来输入多个命令
     * @param aCommands 需要设置的最开始执行的 lammps 命令
     * @return 自身方便链式调用
     */
    public AbstractLmpPotential setBeforeCommands(@Language("lmpin") String aCommands) {mBeforeCommands = aCommands; return this;}
    @Language("lmpin") @Nullable String mLastCommands = null;
    /**
     * 设置需要在 lammps 运行最后执行的命令，可以用来设置 {@code pair_modify}
     * 等命令，通过换行符 {@code \n} 来输入多个命令
     * @param aCommands 需要设置的最后执行的 lammps 命令
     * @return 自身方便链式调用
     */
    public AbstractLmpPotential setLastCommands(@Language("lmpin") String aCommands) {mLastCommands = aCommands; return this;}
    
    String mUnits = DEFAULT_UNITS;
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
    public AbstractLmpPotential setUnits(String aUnits) {mUnits = aUnits; return this;}
    /** @return 内部 lammps 计算采用的单位，默认为 {@code metal} */
    public String units() {return mUnits;}
    
    double validStressUnit(double aLmpStress) {
        switch (mUnits) {
        case "metal": {return aLmpStress*BAR_TO_EV;}
        case "real": {return aLmpStress*ATM_TO_KCAL;}
        case "electron": {return aLmpStress*PA_TO_HARTREE;}
        default: {return aLmpStress;}
        }
    }
    @SuppressWarnings("SuspiciousNameCombination")
    void rotateVirial(IBox aBoxIn, IBox aBoxOut, XYZ rBuf0, XYZ rBuf1, XYZ rBuf2) {
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
    
    /** 常规的 lammps 不支持计算部分原子能量，因此会直接抛出 {@link UnsupportedOperationException} */
    @Override public double calEnergyAt(IAtomData aAPC, ISlice aIndices) {throw new UnsupportedOperationException();}
}
