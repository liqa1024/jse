package jse.lmp;

import jse.atom.IAtomData;
import jse.atom.MultiFrameParameterCalculator;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.math.MathEX;
import jse.math.function.Func1;
import jse.math.function.IFunc1;
import jse.math.vector.IVector;
import jse.parallel.AbstractHasAutoShutdown;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import jse.system.ISystemExecutor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Collection;
import java.util.function.Supplier;

import static jse.code.OS.EXE;
import static jse.code.Conf.WORKING_DIR_OF;

/**
 * 使用 lammps 来进行参数计算
 * <p>
 * 默认使用可读性更好的 data 文件作为输入输出，
 * 内部临时文件使用二进制的 restart 文件，
 * 可以减少各种类型文件的排列组合的接口
 * @author liqa
 */
@ApiStatus.Obsolete
public class LmpParameterCalculator extends AbstractHasAutoShutdown {
    private final String mWorkingDir;
    
    private final ILmpExecutor mLMP;
    private final boolean mIsTempLmp; // 标记此 lmp 是否是临时创建的，如果是则无论如何都要自动关闭
    private final String mPairStyle, mPairCoeff;
    
    /** MPI stuffs */
    private final @Nullable MPI.Comm mComm;
    private final int mRoot;
    
    /** 保留兼容 */
    LmpParameterCalculator(ILmpExecutor aLMP, String aPairStyle, String aPairCoeff) {this(aLMP, false, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {this(new LmpExecutor(EXE, aLmpExe, aLogPath).setDoNotShutdown(true), true, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(String aLmpExe, String aPairStyle, String aPairCoeff) {this(aLmpExe, null, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(ISystemExecutor aEXE, String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {this(new LmpExecutor(aEXE, aLmpExe, aLogPath), true, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(ISystemExecutor aEXE, String aLmpExe, String aPairStyle, String aPairCoeff) {this(aEXE, aLmpExe, null, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(@Nullable MPI.Comm aComm, int aRoot, ILmpExecutor aLMP, String aPairStyle, String aPairCoeff) {this(aComm, aRoot, aLMP, false, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(@Nullable MPI.Comm aComm, int aRoot, String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {this(aComm, aRoot, new LmpExecutor(EXE, aLmpExe, aLogPath).setDoNotShutdown(true), true, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(@Nullable MPI.Comm aComm, int aRoot, String aLmpExe, String aPairStyle, String aPairCoeff) {this(aComm, aRoot, aLmpExe, null, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(@Nullable MPI.Comm aComm, int aRoot, ISystemExecutor aEXE, String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {this(aComm, aRoot, new LmpExecutor(aEXE, aLmpExe, aLogPath), true, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(@Nullable MPI.Comm aComm, int aRoot, ISystemExecutor aEXE, String aLmpExe, String aPairStyle, String aPairCoeff) {this(aComm, aRoot, aEXE, aLmpExe, null, aPairStyle, aPairCoeff);}
    LmpParameterCalculator(ILmpExecutor aLMP, boolean aIsTempLmp, String aPairStyle, String aPairCoeff) {
        this(MPI.InitHelper.initialized() ? MPI.Comm.WORLD : null, 0, aLMP, aIsTempLmp, aPairStyle, aPairCoeff);
    }
    LmpParameterCalculator(@Nullable MPI.Comm aComm, int aRoot, ILmpExecutor aLMP, boolean aIsTempLmp, String aPairStyle, String aPairCoeff) {
        mComm = aComm;
        mRoot = aRoot;
        mLMP = aLMP;
        mIsTempLmp = aIsTempLmp;
        mPairStyle = aPairStyle;
        mPairCoeff = aPairCoeff;
        // 最后设置一下工作目录，这里一定要求相对路径
        mWorkingDir = UT.IO.toRelativePath(WORKING_DIR_OF("LPC@"+UT.Code.randID()));
    }
    
    /**
     * 创建一个 lammps 的参数计算器
     * @author liqa
     * @param aLMP 执行 lammps 的运行器
     * @param aPairStyle lammps 输入文件使用的势场类型
     * @param aPairCoeff lammps 输入文件使用的势场参数
     */
    public static LmpParameterCalculator of(ILmpExecutor aLMP, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aLMP, aPairStyle, aPairCoeff);}
    public static LmpParameterCalculator of(String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aLmpExe, aLogPath, aPairStyle, aPairCoeff);}
    public static LmpParameterCalculator of(String aLmpExe, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aLmpExe, aPairStyle, aPairCoeff);}
    public static LmpParameterCalculator of(ISystemExecutor aEXE, String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aEXE, aLmpExe, aLogPath, aPairStyle, aPairCoeff);}
    public static LmpParameterCalculator of(ISystemExecutor aEXE, String aLmpExe, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aEXE, aLmpExe, aPairStyle, aPairCoeff);}
    public static LmpParameterCalculator of(@Nullable MPI.Comm aComm, int aRoot, ILmpExecutor aLMP, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aComm, aRoot, aLMP, aPairStyle, aPairCoeff);}
    public static LmpParameterCalculator of(@Nullable MPI.Comm aComm, int aRoot, String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aComm, aRoot, aLmpExe, aLogPath, aPairStyle, aPairCoeff);}
    public static LmpParameterCalculator of(@Nullable MPI.Comm aComm, int aRoot, String aLmpExe, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aComm, aRoot, aLmpExe, aPairStyle, aPairCoeff);}
    public static LmpParameterCalculator of(@Nullable MPI.Comm aComm, int aRoot, ISystemExecutor aEXE, String aLmpExe, @Nullable String aLogPath, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aComm, aRoot, aEXE, aLmpExe, aLogPath, aPairStyle, aPairCoeff);}
    public static LmpParameterCalculator of(@Nullable MPI.Comm aComm, int aRoot, ISystemExecutor aEXE, String aLmpExe, String aPairStyle, String aPairCoeff) {return new LmpParameterCalculator(aComm, aRoot, aEXE, aLmpExe, aPairStyle, aPairCoeff);}
    
    
    private void runRoot_(Runnable aRunnable) {
        if (mComm != null) {
            try {
                if (mComm.rank() == mRoot) aRunnable.run();
                mComm.barrier();
            } catch (MPIException e) {throw new RuntimeException(e);}
        } else {
            aRunnable.run();
        }
    }
    private int randSeed_() {
        if (mComm != null) {
            try {return UT.Code.randSeed(mComm, mRoot);}
            catch (MPIException e) {throw new RuntimeException(e);}
        } else {
            return UT.Code.randSeed();
        }
    }
    private String randID_() {
        if (mComm != null) {
            try {return UT.Code.randID(mComm, mRoot);}
            catch (MPIException e) {throw new RuntimeException(e);}
        } else {
            return UT.Code.randID();
        }
    }
    private void exceptionCheck(int aExitValue) {
        if (mComm != null) {
            try {
                int tExitValue = mComm.reduceI(aExitValue, MPI.Op.BOR, mRoot);
                if (mComm.rank()==mRoot && tExitValue!=0) throw new RuntimeException("LAMMPS run Failed, Exit Value: " + tExitValue);
                mComm.barrier();
            } catch (MPIException e) {
                throw new RuntimeException(e);
            }
        } else {
            if (aExitValue != 0) throw new RuntimeException("LAMMPS run Failed, Exit Value: "+aExitValue);
        }
    }
    
    /** 是否在关闭此实例时顺便关闭内部 lmp */
    @Override public LmpParameterCalculator setDoNotShutdown(boolean aDoNotShutdown) {
        if (mIsTempLmp) {
            // 如果 lmp 是临时的，则此设置只影响 lmp 内部的 exe
            mLMP.setDoNotShutdown(aDoNotShutdown);
        } else {
            // 如果 lmp 是初始化时传入的，则直接影响 lmp 即可
            setDoNotShutdown_(aDoNotShutdown);
        }
        return this;
    }
    
    /** 程序结束时删除自己的临时工作目录，并且会关闭 EXE */
    private volatile boolean mDead = false;
    @Override protected void shutdown_() {
        mDead = true;
        runRoot_(() -> {
            try {
                UT.IO.removeDir(mWorkingDir);
                if (mLMP.exec().needSyncIOFiles()) mLMP.exec().removeDir(mWorkingDir);
            } catch (Exception ignored) {}
        });
    }
    @Override protected void shutdownInternal_() {mLMP.shutdown();}
    @Override protected void closeInternal_() {mLMP.close();}
    
    
    
    /**
     * 将输入的原子数据在给定温度下进行熔融操作，将熔融结果输出到 aOutDataPath
     * <p>
     * 会忽略速度信息
     * @author liqa
     * @param aInDataPath 模拟作为输入的 data 文件路径
     * @param aOutDataPath 输出的 data 文件路径
     * @param aTemperature 熔融温度，K
     * @param aRunStep 模拟步骤数，默认为 100000
     * @param aTimestep 模拟的时间步长，默认为 0.002
     */
    public void runMelt(String aInDataPath, String aOutDataPath, double aTemperature, int aRunStep, double aTimestep) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 构造输入文件
        LmpIn rLmpIn = LmpIn.DATA2DATA_MELT_NPT_Cu();
        rLmpIn.put("vT", aTemperature);
        rLmpIn.put("vTimestep", aTimestep);
        rLmpIn.put("vRunStep", aRunStep);
        rLmpIn.put("pair_style", mPairStyle);
        rLmpIn.put("pair_coeff", mPairCoeff);
        rLmpIn.put("vSeed", randSeed_());
        rLmpIn.put("vInDataPath", aInDataPath);
        rLmpIn.put("vOutDataPath", aOutDataPath);
        
        // 运行 lammps 获取输出
        int tExitValue = mLMP.run(rLmpIn);
        // 这里失败直接报错
        exceptionCheck(tExitValue);
    }
    public void runMelt(String aInDataPath, String aOutDataPath, double aTemperature, int aRunStep) {runMelt(aInDataPath, aOutDataPath, aTemperature, aRunStep, 0.002);}
    public void runMelt(String aInDataPath, String aOutDataPath, double aTemperature) {runMelt(aInDataPath, aOutDataPath, aTemperature, 100000);}
    
    public void runMelt(Lmpdat aLmpdat, String aOutDataPath, double aTemperature, int aRunStep, double aTimestep) {
        // 由于可能存在外部并行，data 需要一个独立的名称
        String tInDataPath = mWorkingDir+"data@"+randID_();
        runRoot_(() -> {
            try {aLmpdat.write(tInDataPath);}
            catch (IOException e) {throw new RuntimeException(e);}
        });
        runMelt(tInDataPath, aOutDataPath, aTemperature, aRunStep, aTimestep);
    }
    public void runMelt(Lmpdat aLmpdat, String aOutDataPath, double aTemperature, int aRunStep) {runMelt(aLmpdat, aOutDataPath, aTemperature, aRunStep, 0.002);}
    public void runMelt(Lmpdat aLmpdat, String aOutDataPath, double aTemperature) {runMelt(aLmpdat, aOutDataPath, aTemperature, 100000);}
    
    public void runMelt(IAtomData aAtomData, IVector                      aMasses, String aOutDataPath, double aTemperature, int aRunStep, double aTimestep) {runMelt(Lmpdat.fromAtomData(aAtomData, aMasses), aOutDataPath, aTemperature, aRunStep, aTimestep);}
    public void runMelt(IAtomData aAtomData, Collection<? extends Number> aMasses, String aOutDataPath, double aTemperature, int aRunStep, double aTimestep) {runMelt(Lmpdat.fromAtomData(aAtomData, aMasses), aOutDataPath, aTemperature, aRunStep, aTimestep);}
    public void runMelt(IAtomData aAtomData, double[]                     aMasses, String aOutDataPath, double aTemperature, int aRunStep, double aTimestep) {runMelt(Lmpdat.fromAtomData(aAtomData, aMasses), aOutDataPath, aTemperature, aRunStep, aTimestep);}
    public void runMelt(IAtomData aAtomData, IVector                      aMasses, String aOutDataPath, double aTemperature, int aRunStep) {runMelt(Lmpdat.fromAtomData(aAtomData, aMasses), aOutDataPath, aTemperature, aRunStep);}
    public void runMelt(IAtomData aAtomData, Collection<? extends Number> aMasses, String aOutDataPath, double aTemperature, int aRunStep) {runMelt(Lmpdat.fromAtomData(aAtomData, aMasses), aOutDataPath, aTemperature, aRunStep);}
    public void runMelt(IAtomData aAtomData, double[]                     aMasses, String aOutDataPath, double aTemperature, int aRunStep) {runMelt(Lmpdat.fromAtomData(aAtomData, aMasses), aOutDataPath, aTemperature, aRunStep);}
    public void runMelt(IAtomData aAtomData, IVector                      aMasses, String aOutDataPath, double aTemperature) {runMelt(Lmpdat.fromAtomData(aAtomData, aMasses), aOutDataPath, aTemperature);}
    public void runMelt(IAtomData aAtomData, Collection<? extends Number> aMasses, String aOutDataPath, double aTemperature) {runMelt(Lmpdat.fromAtomData(aAtomData, aMasses), aOutDataPath, aTemperature);}
    public void runMelt(IAtomData aAtomData, double[]                     aMasses, String aOutDataPath, double aTemperature) {runMelt(Lmpdat.fromAtomData(aAtomData, aMasses), aOutDataPath, aTemperature);}
    
    /**
     * 将输入的原子数据在给定温度下进行熔融操作，将每步结构都输出到 aDumpPath
     * <p>
     * 会忽略速度信息
     * @author liqa
     * @param aInDataPath 模拟作为输入的 data 文件路径
     * @param aDumpPath 输出的 dump 文件路径
     * @param aTemperature 熔融温度，K
     * @param aRunStep 模拟步骤数，默认为 100000
     * @param aDumpStep 模拟每步结构输出的间隔数，默认为 1000，和 thermo 间隔同步
     * @param aTimestep 模拟的时间步长，默认为 0.002
     */
    public void runMeltDump(String aInDataPath, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep, double aTimestep) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 构造输入文件
        LmpIn rLmpIn = LmpIn.DATA2DUMP_MELT_NPT_Cu();
        rLmpIn.put("vT", aTemperature);
        rLmpIn.put("vTimestep", aTimestep);
        rLmpIn.put("vRunStep", aRunStep);
        rLmpIn.put("vThermoStep", aDumpStep);
        rLmpIn.put("vDumpStep", aDumpStep);
        rLmpIn.put("pair_style", mPairStyle);
        rLmpIn.put("pair_coeff", mPairCoeff);
        rLmpIn.put("vSeed", randSeed_());
        rLmpIn.put("vInDataPath", aInDataPath);
        rLmpIn.put("vDumpPath", aDumpPath);
        
        // 运行 lammps 获取输出
        int tExitValue = mLMP.run(rLmpIn);
        // 这里失败直接报错
        exceptionCheck(tExitValue);
    }
    public void runMeltDump(String aInDataPath, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep) {runMeltDump(aInDataPath, aDumpPath, aTemperature, aRunStep, aDumpStep, 0.002);}
    public void runMeltDump(String aInDataPath, String aDumpPath, double aTemperature, int aRunStep) {runMeltDump(aInDataPath, aDumpPath, aTemperature, aRunStep, 1000);}
    public void runMeltDump(String aInDataPath, String aDumpPath, double aTemperature) {runMeltDump(aInDataPath, aDumpPath, aTemperature, 100000);}
    
    public void runMeltDump(Lmpdat aLmpdat, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep, double aTimestep) {
        // 由于可能存在外部并行，data 需要一个独立的名称
        String tInDataPath = mWorkingDir+"data@"+randID_();
        runRoot_(() -> {
            try {aLmpdat.write(tInDataPath);}
            catch (IOException e) {throw new RuntimeException(e);}
        });
        runMeltDump(tInDataPath, aDumpPath, aTemperature, aRunStep, aDumpStep, aTimestep);
    }
    public void runMeltDump(Lmpdat aLmpdat, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep) {runMeltDump(aLmpdat, aDumpPath, aTemperature, aRunStep, aDumpStep, 0.002);}
    public void runMeltDump(Lmpdat aLmpdat, String aDumpPath, double aTemperature, int aRunStep) {runMeltDump(aLmpdat, aDumpPath, aTemperature, aRunStep, 1000);}
    public void runMeltDump(Lmpdat aLmpdat, String aDumpPath, double aTemperature) {runMeltDump(aLmpdat, aDumpPath, aTemperature, 100000);}
    
    public void runMeltDump(IAtomData aAtomData, IVector                      aMasses, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep, double aTimestep) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature, aRunStep, aDumpStep, aTimestep);}
    public void runMeltDump(IAtomData aAtomData, Collection<? extends Number> aMasses, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep, double aTimestep) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature, aRunStep, aDumpStep, aTimestep);}
    public void runMeltDump(IAtomData aAtomData, double[]                     aMasses, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep, double aTimestep) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature, aRunStep, aDumpStep, aTimestep);}
    public void runMeltDump(IAtomData aAtomData, IVector                      aMasses, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature, aRunStep, aDumpStep);}
    public void runMeltDump(IAtomData aAtomData, Collection<? extends Number> aMasses, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature, aRunStep, aDumpStep);}
    public void runMeltDump(IAtomData aAtomData, double[]                     aMasses, String aDumpPath, double aTemperature, int aRunStep, int aDumpStep) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature, aRunStep, aDumpStep);}
    public void runMeltDump(IAtomData aAtomData, IVector                      aMasses, String aDumpPath, double aTemperature, int aRunStep) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature, aRunStep);}
    public void runMeltDump(IAtomData aAtomData, Collection<? extends Number> aMasses, String aDumpPath, double aTemperature, int aRunStep) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature, aRunStep);}
    public void runMeltDump(IAtomData aAtomData, double[]                     aMasses, String aDumpPath, double aTemperature, int aRunStep) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature, aRunStep);}
    public void runMeltDump(IAtomData aAtomData, IVector                      aMasses, String aDumpPath, double aTemperature) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature);}
    public void runMeltDump(IAtomData aAtomData, Collection<? extends Number> aMasses, String aDumpPath, double aTemperature) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature);}
    public void runMeltDump(IAtomData aAtomData, double[]                     aMasses, String aDumpPath, double aTemperature) {runMeltDump(Lmpdat.fromAtomData(aAtomData, aMasses), aDumpPath, aTemperature);}
    
    
    /** 内部使用的熔融操作，输出 restart 文件 */
    public void runMelt_(String aInDataPath, String aOutRestartPath, double aTemperature, double aTimestep, int aRunStep) {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 构造输入文件
        LmpIn rLmpIn = LmpIn.DATA2RESTART_MELT_NPT_Cu();
        rLmpIn.put("vT", aTemperature);
        rLmpIn.put("vTimestep", aTimestep);
        rLmpIn.put("vRunStep", aRunStep);
        rLmpIn.put("pair_style", mPairStyle);
        rLmpIn.put("pair_coeff", mPairCoeff);
        rLmpIn.put("vSeed", randSeed_());
        rLmpIn.put("vInDataPath", aInDataPath);
        rLmpIn.put("vOutRestartPath", aOutRestartPath);
        
        // 运行 lammps 获取输出
        int tExitValue = mLMP.run(rLmpIn);
        // 这里失败直接报错
        exceptionCheck(tExitValue);
    }
    public void runMelt_(Lmpdat aLmpdat, String aOutRestartPath, double aTemperature, double aTimestep, int aRunStep) {
        // 由于可能存在外部并行，data 需要一个独立的名称
        String tInDataPath = mWorkingDir+"data@"+randID_();
        runRoot_(() -> {
            try {aLmpdat.write(tInDataPath);}
            catch (IOException e) {throw new RuntimeException(e);}
        });
        runMelt_(tInDataPath, aOutRestartPath, aTemperature, aTimestep, aRunStep);
    }
    
    
    /** 用于进行多次计算的通用子计算器，可以减少一些通用参数造成的重载 */
    public interface ISubCalculator<T> {
        /**
         * 和 {@link Lmpdat} 等数据类似的将密度归一化的接口，
         * 返回自身支持链式调用
         */
        ISubCalculator<T> setDenseNormalized();
        
        /**
         * 执行计算，采用 {@link IAtomData} 中获取 MPC
         * 类似的写法来设置仅计算某个种类以及线程数
         * @param aType 指定仅计算某个种类的结果
         * @param aThreadNum 指定线程数，默认为 1
         * @return 计算结果
         */
        T calType(int aType, int aThreadNum);
        T cal    (           int aThreadNum);
        default T calType(int aType) {return calType(aType, 1);}
        default T cal    (         ) {return cal(1);}
    }
    
    
    /**
     * 计算 MSD (Mean Square Displacement)，
     * 会自动分划两组不同步长的 dump 分别计算
     * <p>
     * 会忽略速度信息统一进行预热处理
     * @author liqa
     * @param aInDataPath 模拟作为输入的 data 文件路径
     * @param aTemperature 模拟的温度，K
     * @param aN 期望计算的时间点数目，默认为 20
     * @param aTimestep 模拟的时间步长，默认为 0.002
     * @param aTimeGap 进行平均的时间间隔，认为这个时间间隔后的系统不再相关，默认为 100*aTimestep
     * @param aMaxStepNum 期望的最大模拟步骤数，默认为 100000
     * @return 计算 MSD 的子计算器
     */
    public ISubCalculator<IFunc1> calMSD(String aInDataPath, double aTemperature, int aN, double aTimestep, double aTimeGap, int aMaxStepNum) throws Exception {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        // 由于可能存在外部并行，restart 需要一个独立的名称
        String tRestartPath = mWorkingDir+"restart@"+randID_();
        // 先做预热处理
        runMelt_(aInDataPath, tRestartPath, aTemperature, aTimestep, 10000);
        // 然后计算 msd
        return calMSD_(tRestartPath, aTemperature, aN, aTimestep, aTimeGap, aMaxStepNum);
    }
    public ISubCalculator<IFunc1> calMSD(String aInDataPath, double aTemperature, int aN, double aTimestep, double aTimeGap) throws Exception {return calMSD(aInDataPath, aTemperature, aN, aTimestep, aTimeGap, 100000);}
    public ISubCalculator<IFunc1> calMSD(String aInDataPath, double aTemperature, int aN, double aTimestep) throws Exception {return calMSD(aInDataPath, aTemperature, aN, aTimestep, 100*aTimestep);}
    public ISubCalculator<IFunc1> calMSD(String aInDataPath, double aTemperature, int aN) throws Exception {return calMSD(aInDataPath, aTemperature, aN, 0.002);}
    public ISubCalculator<IFunc1> calMSD(String aInDataPath, double aTemperature) throws Exception {return calMSD(aInDataPath, aTemperature, 20);}
    
    public ISubCalculator<IFunc1> calMSD(Lmpdat aLmpdat, double aTemperature, int aN, double aTimestep, double aTimeGap, int aMaxStepNum) throws Exception {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        // 由于可能存在外部并行，restart 需要一个独立的名称
        String tLmpRestartPath = mWorkingDir+"restart@"+randID_();
        // 先做预热处理
        runMelt_(aLmpdat, tLmpRestartPath, aTemperature, aTimestep, 10000);
        // 然后计算 msd
        return calMSD_(tLmpRestartPath, aTemperature, aN, aTimestep, aTimeGap, aMaxStepNum);
    }
    public ISubCalculator<IFunc1> calMSD(Lmpdat aLmpdat, double aTemperature, int aN, double aTimestep, double aTimeGap) throws Exception {return calMSD(aLmpdat, aTemperature, aN, aTimestep, aTimeGap, 100000);}
    public ISubCalculator<IFunc1> calMSD(Lmpdat aLmpdat, double aTemperature, int aN, double aTimestep) throws Exception {return calMSD(aLmpdat, aTemperature, aN, aTimestep, 100*aTimestep);}
    public ISubCalculator<IFunc1> calMSD(Lmpdat aLmpdat, double aTemperature, int aN) throws Exception {return calMSD(aLmpdat, aTemperature, aN, 0.002);}
    public ISubCalculator<IFunc1> calMSD(Lmpdat aLmpdat, double aTemperature) throws Exception {return calMSD(aLmpdat, aTemperature, 20);}
    
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, IVector                      aMasses, double aTemperature, int aN, double aTimestep, double aTimeGap, int aMaxStepNum) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN, aTimestep, aTimeGap, aMaxStepNum);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, Collection<? extends Number> aMasses, double aTemperature, int aN, double aTimestep, double aTimeGap, int aMaxStepNum) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN, aTimestep, aTimeGap, aMaxStepNum);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, double[]                     aMasses, double aTemperature, int aN, double aTimestep, double aTimeGap, int aMaxStepNum) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN, aTimestep, aTimeGap, aMaxStepNum);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, IVector                      aMasses, double aTemperature, int aN, double aTimestep, double aTimeGap) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN, aTimestep, aTimeGap);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, Collection<? extends Number> aMasses, double aTemperature, int aN, double aTimestep, double aTimeGap) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN, aTimestep, aTimeGap);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, double[]                     aMasses, double aTemperature, int aN, double aTimestep, double aTimeGap) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN, aTimestep, aTimeGap);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, IVector                      aMasses, double aTemperature, int aN, double aTimestep) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN, aTimestep);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, Collection<? extends Number> aMasses, double aTemperature, int aN, double aTimestep) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN, aTimestep);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, double[]                     aMasses, double aTemperature, int aN, double aTimestep) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN, aTimestep);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, IVector                      aMasses, double aTemperature, int aN) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, Collection<? extends Number> aMasses, double aTemperature, int aN) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, double[]                     aMasses, double aTemperature, int aN) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature, aN);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, IVector                      aMasses, double aTemperature) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, Collection<? extends Number> aMasses, double aTemperature) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature);}
    public ISubCalculator<IFunc1> calMSD(IAtomData aAtomData, double[]                     aMasses, double aTemperature) throws Exception {return calMSD(Lmpdat.fromAtomData(aAtomData, aMasses), aTemperature);}
    
    /** 内部使用的 restart 输入的不做预热处理的方法 */
    public ISubCalculator<IFunc1> calMSD_(String aInRestartPath, double aTemperature, int aN, final double aTimestep, final double aTimeGap, int aMaxStepNum) throws Exception {
        if (mDead) throw new RuntimeException("This Calculator is dead");
        
        // 由于可能存在外部并行，dump 需要一个独立的名称
        String tLmpDumpPath = mWorkingDir+"dump@"+randID_();
        
        // 构造输入文件
        LmpIn rLmpIn = LmpIn.RESTART2DUMP_MELT_NPT_Cu();
        rLmpIn.put("vT", aTemperature);
        rLmpIn.put("vTimestep", aTimestep);
        rLmpIn.put("pair_style", mPairStyle);
        rLmpIn.put("pair_coeff", mPairCoeff);
        rLmpIn.put("vInRestartPath", aInRestartPath);
        rLmpIn.put("vDumpPath", tLmpDumpPath);
        
        // 尝试获取短时数据
        Lammpstrj tShortLammpstrj = null; int tShortN = -1;
        // 尝试分段，长时间固定取 200 帧
        final int tLongDumpStep = MathEX.Code.divup(aMaxStepNum, 200);
        // 判断是否需要短时的，只要长间距大于 50 则需要
        if (tLongDumpStep > 50) {
            // 此时固定取 500 帧
            int tShortDumpStep = MathEX.Code.divup(tLongDumpStep, 500);
            // 固定最小间距为 10
            if (tShortDumpStep < 10) tShortDumpStep = 10;
            // 计算短时需要的分点
            tShortN = (int)Math.round(MathEX.Fast.log(tLongDumpStep*0.2) / MathEX.Fast.log(aMaxStepNum*0.08) * (aN-1));
            if (tShortN > 0) {
                aN -= tShortN; ++tShortN;
                // 进行短时分段的统计，这里要保证能“刚好”接上
                rLmpIn.put("vRunStep", tShortDumpStep*500);
                rLmpIn.put("vDumpStep", tShortDumpStep);
                // 运行 lammps 获取输出
                int tExitValue = mLMP.run(rLmpIn);
                // 这里失败直接报错
                exceptionCheck(tExitValue);
                // 读取结果并删除文件
                tShortLammpstrj = Lammpstrj.read(tLmpDumpPath);
                mLMP.exec().delete(tLmpDumpPath);
            }
        }
        
        // 获取长时数据
        rLmpIn.put("vRunStep", aMaxStepNum);
        rLmpIn.put("vDumpStep", tLongDumpStep);
        // 运行 lammps 获取输出
        int tExitValue = mLMP.run(rLmpIn);
        // 这里失败直接报错
        exceptionCheck(tExitValue);
        // 读取结果并删除文件
        Lammpstrj tLongLammpstrj = Lammpstrj.read(tLmpDumpPath);
        mLMP.exec().delete(tLmpDumpPath);
        
        
        // 返回子计算器
        final @Nullable Lammpstrj fShortLammpstrj = tShortLammpstrj; final int fShortN = tShortN;
        final @NotNull  Lammpstrj fLongLammpstrj = tLongLammpstrj; final int fLongN = aN;
        return new ISubCalculator<IFunc1>() {
            private IFunc1 calMSD_(@Nullable Supplier<@NotNull MultiFrameParameterCalculator> aShortSupplier, @NotNull Supplier<@NotNull MultiFrameParameterCalculator> aLongSupplier) {
                // 计算短时和长时的 MSD
                IFunc1 tLongMSD;
                try (MultiFrameParameterCalculator tLongMFPC = aLongSupplier.get()) {tLongMSD = tLongMFPC.calMSD(fLongN, aTimeGap);}
                if (aShortSupplier == null) return tLongMSD;
                IFunc1 tShortMSD;
                try (MultiFrameParameterCalculator tShortMFPC = aShortSupplier.get()) {tShortMSD = tShortMFPC.calMSD(fShortN, aTimeGap, aTimestep*tLongDumpStep*2.0);}
                // 这里直接构造 Func1
                IFunc1 rMSD = Func1.zeros(fLongN+fShortN-1, i -> (i < fShortN) ? tShortMSD.getX(i) : tLongMSD.getX(i-fShortN+1));
                // 直接遍历设置，连接处平均
                for (int i = 0; i < fShortN; ++i) rMSD.set(i, tShortMSD.get(i));
                rMSD.update(fShortN-1, v -> (v + tLongMSD.get(0))*0.5);
                for (int i = 1, j = fShortN; i < fLongN; ++i, ++j) rMSD.set(j, tLongMSD.get(i));
                // 返回结果
                return rMSD;
            }
            
            @Override public ISubCalculator<IFunc1> setDenseNormalized() {
                if (fShortLammpstrj != null) fShortLammpstrj.setDenseNormalized();
                fLongLammpstrj.setDenseNormalized();
                return this;
            }
            @Override public IFunc1 calType(final int aType, final int aThreadNum) {
                return calMSD_(fShortLammpstrj==null ? null : ()->MultiFrameParameterCalculator.of(AbstractCollections.map(fShortLammpstrj.asList(), atomData -> atomData.operation().filterType(aType)), fShortLammpstrj.size()>1 ? aTimestep*(fShortLammpstrj.get(1).timeStep()-fShortLammpstrj.get(0).timeStep()) : aTimestep, aThreadNum), ()->MultiFrameParameterCalculator.of(AbstractCollections.map(fLongLammpstrj.asList(), atomData -> atomData.operation().filterType(aType)), fLongLammpstrj.size()>1 ? aTimestep*(fLongLammpstrj.get(1).timeStep()-fLongLammpstrj.get(0).timeStep()) : aTimestep, aThreadNum));
            }
            @Override public IFunc1 cal(final int aThreadNum) {
                return calMSD_(fShortLammpstrj==null ? null : ()->MultiFrameParameterCalculator.of(fShortLammpstrj.asList(), fShortLammpstrj.size()>1 ? aTimestep*(fShortLammpstrj.get(1).timeStep()-fShortLammpstrj.get(0).timeStep()) : aTimestep, aThreadNum), ()->MultiFrameParameterCalculator.of(fLongLammpstrj.asList(), fLongLammpstrj.size()>1 ? aTimestep*(fLongLammpstrj.get(1).timeStep()-fLongLammpstrj.get(0).timeStep()) : aTimestep, aThreadNum));
            }
        };
    }
}
