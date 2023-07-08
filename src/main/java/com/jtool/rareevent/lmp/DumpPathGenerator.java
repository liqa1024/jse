package com.jtool.rareevent.lmp;

import com.jtool.atom.IHasAtomData;
import com.jtool.code.UT;
import com.jtool.iofile.IHasIOFiles;
import com.jtool.lmp.Lammpstrj;
import com.jtool.lmp.Lammpstrj.SubLammpstrj;
import com.jtool.lmp.LmpIn;
import com.jtool.lmp.Lmpdat;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;
import com.jtool.rareevent.IPathGenerator;
import com.jtool.system.ISystemExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import static com.jtool.code.CS.*;


/**
 * 一种路径生成器，通过运行 lammps 生成 Dump 格式的路径；
 * 特别注意方法的线程安全要求
 * @author liqa
 */
public class DumpPathGenerator implements IPathGenerator<SubLammpstrj> {
    private final String mWorkingDir;
    
    private final Random mRNG = new Random();
    private final ISystemExecutor mEXE;
    private final String mLmpExe;
    private final Lammpstrj mInitPoints;
    private final LmpIn mGenDumpIn;
    private final double mTimestep;
    private final IVector mMesses;
    
    /**
     * 创建一个输出 dump 路径的生成器
     * @author liqa
     * @param aEXE 执行 lammps 的运行器
     * @param aLmpExe 执行 lammps 的指令
     * @param aInitAtomDataList  用于初始的原子数据
     * @param aMesses 每个种类的原子对应的摩尔质量，用于创建 data 文件，且长度指定原子种类数目
     * @param aTemperature 创建 dump 路径的温度
     * @param aPairStyle lammps 输入文件使用的势场类型
     * @param aPairCoeff lammps 输入文件使用的势场参数
     * @param aTimestep 每步的实际时间步长，影响输入文件和统计使用的时间，最终时间单位和输入文件的 unit 一致，默认为 0.002
     * @param aDumpStep 每隔多少模拟步输出一个 dump，默认为 10
     * @param aPathLength 一次创建的路径的长度，默认为 20
     */
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, int aPathLength) {this(aEXE, aLmpExe, aInitAtomDataList, Vectors.from(aMesses), getGenDumpIn(aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aPathLength), aTimestep);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep                 ) {this(aEXE, aLmpExe, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, 20);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep                                ) {this(aEXE, aLmpExe, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff                                                  ) {this(aEXE, aLmpExe, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, 0.002);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, int aPathLength) {this(aEXE, aLmpExe, aInitAtomDataList, Vectors.from(aMesses), getGenDumpIn(aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aPathLength), aTimestep);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep                 ) {this(aEXE, aLmpExe, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, 20);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep                                ) {this(aEXE, aLmpExe, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff                                                  ) {this(aEXE, aLmpExe, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, 0.002);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, int aPathLength) {this(aEXE, aLmpExe, aInitAtomDataList, Vectors.from(aMesses), getGenDumpIn(aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aPathLength), aTimestep);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep                 ) {this(aEXE, aLmpExe, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, 20);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep                                ) {this(aEXE, aLmpExe, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10);}
    public DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff                                                  ) {this(aEXE, aLmpExe, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, 0.002);}
    
    DumpPathGenerator(ISystemExecutor aEXE, String aLmpExe, Iterable<? extends IHasAtomData> aInitAtomDataList, IVector aMesses, LmpIn aGenDumpIn, double aTimestep) {
        mEXE = aEXE.setNoSTDOutput(); // 不需要输出
        mLmpExe = aLmpExe;
        mInitPoints = Lammpstrj.fromAtomData(aInitAtomDataList);
        mTimestep = aTimestep;
        mMesses = aMesses;
        mGenDumpIn = aGenDumpIn;
        // 最后设置一下工作目录
        mWorkingDir = WORKING_DIR.replaceAll("%n", "DUMP_GEN@"+UT.Code.randID());
    }
    
    private static LmpIn getGenDumpIn(double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, int aPathLength) {
        LmpIn rGenDumpIn = LmpIn.DUMP_MELT_NPT_Cu();
        rGenDumpIn.put("vT", aTemperature);
        rGenDumpIn.put("vTimestep", aTimestep);
        rGenDumpIn.put("vDumpStep", aDumpStep);
        rGenDumpIn.put("vRunStep", aPathLength*aDumpStep);
        // 还需设置势场
        rGenDumpIn.put("pair_style", aPairStyle);
        rGenDumpIn.put("pair_coeff", aPairCoeff);
        return rGenDumpIn;
    }
    
    
    /** IPathGenerator stuff */
    private int mIdx = 0;
    @Override public synchronized SubLammpstrj initPoint() {
        SubLammpstrj tPoint = mInitPoints.get(mIdx);
        ++mIdx;
        if (mIdx == mInitPoints.size()) mIdx = 0;
        return tPoint;
    }
    @Override public List<SubLammpstrj> pathFrom(SubLammpstrj aStart) {
        try {
            // 由于存在并行，需要在工作目录中创建临时的路径生成的目录
            String tLmpDir = mWorkingDir+"LMP@"+UT.Code.randID()+"/";
            // 一些路径的初始化
            String tLmpInPath = tLmpDir+"in";
            String tLmpDataPath = tLmpDir+"data";
            String tLmpDumpPath = tLmpDir+"dump";
            // 先根据输入创建 Lmpdat 并写入，注意需要再设置一下种类数，因为 dump 不会保留种类数，对于恰好缺少种类的情况会出错
            Lmpdat.fromAtomData_(aStart, mMesses).setAtomTypeNum(mMesses.size()).write(tLmpDataPath);
            // 设置输入 data 路径和输出 dump 路径，考虑要线程安全这里要串行设置并且设置完成后拷贝结果
            IHasIOFiles tIOFiles;
            synchronized (this) {
                mGenDumpIn.put("vSeed", mRNG.nextInt(MAX_SEED)); // 需要有不同的初始速度
                mGenDumpIn.put("vInDataPath", tLmpDataPath);
                mGenDumpIn.put("vDumpPath", tLmpDumpPath);
                mGenDumpIn.write(tLmpInPath);
                tIOFiles = mGenDumpIn.copy(); // 由于 InFile 实际内部存在复杂的引用，不能简单的直接完整拷贝
            }
            // 组装指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add(mLmpExe);
            rCommand.add("-in"); rCommand.add(tLmpInPath);
            // 执行指令
            int tExitValue = mEXE.system(String.join(" ", rCommand), tIOFiles);
            if (tExitValue != 0) throw new Exception("LAMMPS run Failed, Exit Value: "+tExitValue);
            // 理论现在已经获取到了 dump 文件，读取
            Lammpstrj tDump = Lammpstrj.read(tLmpDumpPath);
            // 返回之前，先删除临时文件夹
            UT.IO.removeDir(tLmpDir);
            // 返回结果
            return tDump;
        } catch (Exception e) {
            // 还是抛出 RuntimeException，这样至少 try-with-resources 的写法能正常的捕获错误
            // 之前被 err 流意外关闭误导，实际 RuntimeException 还是需要抛出一下
            throw new RuntimeException(e);
        }
    }
    
    @Override public double timeOf(SubLammpstrj aPoint) {return aPoint.timeStep() * mTimestep;}
    
    
    /** 程序结束时删除自己的临时工作目录，并且会关闭 EXE */
    @Override public void shutdown() {
        try {
            UT.IO.removeDir(mWorkingDir);
            mEXE.removeDir(mWorkingDir);
        } catch (Exception ignored) {}
        mEXE.shutdown();
    }
}
