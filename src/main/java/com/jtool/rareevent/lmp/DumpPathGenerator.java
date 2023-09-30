package com.jtool.rareevent.lmp;

import com.jtool.atom.*;
import com.jtool.code.UT;
import com.jtool.code.collection.AbstractCollections;
import com.jtool.code.collection.AbstractRandomAccessList;
import com.jtool.iofile.IIOFiles;
import com.jtool.iofile.IInFile;
import com.jtool.lmp.ILmpExecutor;
import com.jtool.lmp.Lammpstrj;
import com.jtool.lmp.Lammpstrj.SubLammpstrj;
import com.jtool.lmp.LmpIn;
import com.jtool.lmp.Lmpdat;
import com.jtool.math.matrix.IMatrix;
import com.jtool.math.matrix.Matrices;
import com.jtool.math.vector.IVector;
import com.jtool.math.vector.Vectors;
import com.jtool.parallel.AbstractHasAutoShutdown;
import com.jtool.rareevent.IPathGenerator;

import java.util.*;

import static com.jtool.code.CS.*;
import static com.jtool.code.UT.Code.newBox;


/**
 * 一种路径生成器，通过运行 lammps 生成 Dump 格式的路径；
 * 特别注意方法的线程安全要求
 * @author liqa
 */
public class DumpPathGenerator extends AbstractHasAutoShutdown implements IPathGenerator<IAtomData> {
    private final static int TOLERANT = 3;
    
    private final String mWorkingDir;
    
    private final Random mRNG = new Random();
    private final ILmpExecutor mLMP;
    private final Lammpstrj mInitPoints;
    private final IInFile mGenDumpIn;
    private final double mTimestep;
    private final IVector mMesses;
    
    private int mTolerant = TOLERANT;
    
    /**
     * 创建一个输出 dump 路径的生成器
     * @author liqa
     * @param aLMP 执行 lammps 的运行器
     * @param aInitAtomDataList  用于初始的原子数据
     * @param aMesses 每个种类的原子对应的摩尔质量，用于创建 data 文件，且长度指定原子种类数目
     * @param aTemperature 创建 dump 路径的温度
     * @param aPairStyle lammps 输入文件使用的势场类型
     * @param aPairCoeff lammps 输入文件使用的势场参数
     * @param aTimestep 每步的实际时间步长，影响输入文件和统计使用的时间，最终时间单位和输入文件的 unit 一致，默认为 0.002
     * @param aDumpStep 每隔多少模拟步输出一个 dump，默认为 10
     * @param aPathLength 一次创建的路径的长度，默认为 20
     */
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, int aPathLength) {this(aLMP, aInitAtomDataList, Vectors.from(aMesses), getGenDumpIn(aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aPathLength), aTimestep);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep                 ) {this(aLMP, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, 20);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep                                ) {this(aLMP, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList,                      IVector aMesses, double aTemperature, String aPairStyle, String aPairCoeff                                                  ) {this(aLMP, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, 0.002);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, int aPathLength) {this(aLMP, aInitAtomDataList, Vectors.from(aMesses), getGenDumpIn(aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aPathLength), aTimestep);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep                 ) {this(aLMP, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, 20);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep                                ) {this(aLMP, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList, Collection<? extends Number> aMesses, double aTemperature, String aPairStyle, String aPairCoeff                                                  ) {this(aLMP, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, 0.002);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep, int aPathLength) {this(aLMP, aInitAtomDataList, Vectors.from(aMesses), getGenDumpIn(aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, aPathLength), aTimestep);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep, int aDumpStep                 ) {this(aLMP, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, aDumpStep, 20);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff, double aTimestep                                ) {this(aLMP, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, aTimestep, 10);}
    public DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList,                     double[] aMesses, double aTemperature, String aPairStyle, String aPairCoeff                                                  ) {this(aLMP, aInitAtomDataList, aMesses, aTemperature, aPairStyle, aPairCoeff, 0.002);}
    
    DumpPathGenerator(ILmpExecutor aLMP, Iterable<? extends IAtomData> aInitAtomDataList, IVector aMesses, IInFile aGenDumpIn, double aTimestep) {
        mLMP = aLMP;
        // 初始点也需要移除速度，保证会从不同路径开始
        mInitPoints = Lammpstrj.fromAtomDataList(AbstractCollections.map(aInitAtomDataList, this::reducedPoint));
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
        rGenDumpIn.put("vTdamp", aTimestep*50); // 增大控温的频率，因为都是短时的运行
        rGenDumpIn.put("vDumpStep", aDumpStep);
        rGenDumpIn.put("vRunStep", aPathLength*aDumpStep);
        // 还需设置势场
        rGenDumpIn.put("pair_style", aPairStyle);
        rGenDumpIn.put("pair_coeff", aPairCoeff);
        return rGenDumpIn;
    }
    
    /** 是否在关闭此实例时顺便关闭内部 exe */
    public DumpPathGenerator setDoNotShutdown(boolean aDoNotShutdown) {setDoNotShutdown_(aDoNotShutdown); return this;}
    
    
    /** IPathGenerator stuff */
    private int mIdx = 0;
    @Override public synchronized SubLammpstrj initPoint() {
        SubLammpstrj tPoint = mInitPoints.get(mIdx);
        ++mIdx;
        if (mIdx == mInitPoints.size()) mIdx = 0;
        return tPoint;
    }
    @Override public List<? extends IAtomData> pathFrom(IAtomData aStart) {
        // 由于存在并行，需要在工作目录中创建临时的路径生成的目录
        String tLmpDir = mWorkingDir+"LMP@"+UT.Code.randID()+"/";
        try {
            // 一些路径的初始化
            String tLmpInPath = tLmpDir+"in";
            String tLmpDataPath = tLmpDir+"data";
            String tLmpDumpPath = tLmpDir+"dump";
            // 先根据输入创建 Lmpdat 并写入，注意需要再设置一下种类数，因为 dump 不会保留种类数，对于恰好缺少种类的情况会出错
            Lmpdat.fromAtomData_(aStart, mMesses).setAtomTypeNum(mMesses.size()).write(tLmpDataPath);
            // 设置输入 data 路径和输出 dump 路径，考虑要线程安全这里要串行设置并且设置完成后拷贝结果
            IIOFiles tIOFiles;
            synchronized (this) {
                // 如果此点不带有速度则需要有不同的随机初始速度，否则不需要分配速度
                if (aStart.hasVelocities()) {
                    mGenDumpIn.put("velocity", REMOVE);
                } else {
                    mGenDumpIn.put("velocity", KEEP);
                    mGenDumpIn.put("vSeed", mRNG.nextInt(MAX_SEED));
                }
                mGenDumpIn.put("vInDataPath", tLmpDataPath);
                mGenDumpIn.put("vDumpPath", tLmpDumpPath);
                mGenDumpIn.write(tLmpInPath);
                tIOFiles = mGenDumpIn.copy(); // 由于 InFile 实际内部存在复杂的引用，不能简单的直接完整拷贝
            }
            // 运行 lammps
            int tExitValue = mLMP.run(tLmpInPath, tIOFiles);
            // 这里失败直接报错，后续会捕获到
            if (tExitValue != 0) throw new Exception("LAMMPS run Failed, Exit Value: "+tExitValue);
            // 理论现在已经获取到了 dump 文件，读取并返回结果
            Lammpstrj tLammpstrj = Lammpstrj.read(tLmpDumpPath);
            // 获取成功，重置容忍
            synchronized (this) {mTolerant = TOLERANT;}
            return tLammpstrj;
        } catch (Exception e) {
            // 出错了则减去容忍，重复失败才真正抛出错误
            synchronized (this) {
                --mTolerant;
                if (mTolerant < 0) throw new RuntimeException(e);
                else return Collections.singletonList(aStart);
            }
        } finally {
            // 最后删除临时文件夹
            try {
                UT.IO.removeDir(tLmpDir);
                if (mLMP.exec().needSyncIOFiles()) mLMP.exec().removeDir(tLmpDir);
            } catch (Exception ignored) {}
        }
    }
    
    @Override public double timeOf(IAtomData aPoint) {return (aPoint instanceof SubLammpstrj) ? ((SubLammpstrj)aPoint).timeStep()*mTimestep : 0.0;}
    
    @Override public IAtomData reducedPoint(IAtomData aPoint) {
        // 如果本来就没有速率则不需要执行此操作
        if (!aPoint.hasVelocities()) return aPoint;
        // 遍历拷贝数据，只需要坐标和种类数据
        final int tAtomNum = aPoint.atomNum();
        final IMatrix rData = Matrices.zeros(tAtomNum, ATOM_DATA_KEYS_TYPE_XYZ.length);
        int row = 0;
        for (IAtom tAtom : aPoint.atoms()) {
            rData.set_(row, TYPE_XYZ_TYPE_COL, tAtom.type());
            rData.set_(row, TYPE_XYZ_X_COL, tAtom.x());
            rData.set_(row, TYPE_XYZ_Y_COL, tAtom.y());
            rData.set_(row, TYPE_XYZ_Z_COL, tAtom.z());
            ++row;
        }
        return new AtomData(new AbstractRandomAccessList<IAtom>() {
            @Override public IAtom get(final int index) {
                return new IAtom() {
                    @Override public double x() {return rData.get(index, TYPE_XYZ_X_COL);}
                    @Override public double y() {return rData.get(index, TYPE_XYZ_Y_COL);}
                    @Override public double z() {return rData.get(index, TYPE_XYZ_Z_COL);}
                    @Override public int id() {return index+1;}
                    @Override public int type() {return (int)rData.get(index, TYPE_XYZ_TYPE_COL);}
                };
            }
            @Override public int size() {return tAtomNum;}
        }, aPoint.atomTypeNum(), newBox(aPoint.box()));
    }
    
    
    /** 程序结束时删除自己的临时工作目录，并且会关闭 EXE */
    @Override protected void shutdown_() {
        try {
            UT.IO.removeDir(mWorkingDir);
            if (mLMP.exec().needSyncIOFiles()) mLMP.exec().removeDir(mWorkingDir);
        } catch (Exception ignored) {}
    }
    @Override protected void shutdownInternal_() {
        mLMP.shutdown();
    }
}
