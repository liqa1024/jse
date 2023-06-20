package com.jtool.rareevent.lmp;

import com.jtool.atom.IHasAtomData;
import com.jtool.code.UT;
import com.jtool.iofile.IHasIOFiles;
import com.jtool.lmp.Lammpstrj;
import com.jtool.lmp.Lammpstrj.SubLammpstrj;
import com.jtool.lmp.LmpIn;
import com.jtool.lmp.Lmpdat;
import com.jtool.rareevent.IPathGenerator;
import com.jtool.system.ISystemExecutor;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;

import static com.jtool.code.CS.*;


/**
 * 一种路径生成器，通过运行 lammps 生成 Dump 格式的路径；
 * 特别注意方法的线程安全要求
 * @author liqa
 */
public class DumpPathGenerator implements IPathGenerator<SubLammpstrj>, AutoCloseable {
    // 暂时直接硬编码这些参数，同样避免滥用的同时简化使用
    private final static String PAIR_STYLE = "eam/alloy";
    private final static String PAIR_COEFF = "* * lmp/potential/ZrCu.lammps.eam Cu Zr";
    private final static String LMP_EXE = "lmp_ann";
    private final static double[] MASSES = new double[] {MASS.get("Cu"), MASS.get("Zr")};
    
    private final String mWorkingDir;
    
    private final ISystemExecutor mEXE; // 执行 lammps 的运行器
    private final SubLammpstrj mInitPoint; // 用于初始的原子数据
    private final LmpIn mGenDumpIn; // 创建 dump 文件的 lammps 输入文件
    private final double mTimestep; // 每步的实际时间步长，影响输入文件和统计使用的时间，最终时间单位和输入文件的 unit 一致
    public DumpPathGenerator(ISystemExecutor aEXE, IHasAtomData aInitAtomData, double aTimestep, int aDumpStep, int aPathLength) {
        mEXE = aEXE;
        mInitPoint = Lammpstrj.fromAtomData(aInitAtomData).get(0);
        mTimestep = aTimestep;
        
        // 统一设置输入文件，这里直接使用内置的输入文件，避免滥用的同时可以简化使用
        mGenDumpIn = LmpIn.DUMP_MELT_NPT_Cu();
        mGenDumpIn.put("vTimestep", mTimestep);
        mGenDumpIn.put("vDumpStep", aDumpStep);
        mGenDumpIn.put("vRunStep", aPathLength*aDumpStep);
        // 暂时直接硬编码势场
        mGenDumpIn.put("pair_style", PAIR_STYLE);
        mGenDumpIn.put("pair_coeff", PAIR_COEFF);
        
        // 最后设置一下工作目录
        mWorkingDir = WORKING_DIR.replaceAll("%n", "DUMP_GEN@"+UT.Code.randID());
    }
    
    /** IPathGenerator stuff */
    @Override public SubLammpstrj initPoint() {return mInitPoint;}
    @Override public List<SubLammpstrj> pathFrom(SubLammpstrj aStart) {
        try {
            // 由于存在并行，需要在工作目录中创建临时的路径生成的目录
            String tLmpDir = mWorkingDir+"LMP@"+UT.Code.randID()+"/";
            // 一些路径的初始化
            String tLmpInPath = tLmpDir+"in";
            String tLmpDataPath = tLmpDir+"data";
            String tLmpDumpPath = tLmpDir+"dump";
            // 先根据输入创建 Lmpdat 并写入，注意需要再设置一下种类数，因为 dump 不会保留种类数，对于恰好缺少种类的情况会出错
            Lmpdat.fromAtomData(aStart, MASSES).setAtomTypeNum(MASSES.length).write(tLmpDataPath);
            // 设置输入 data 路径和输出 dump 路径，考虑要线程安全这里要串行设置并且设置完成后拷贝结果
            IHasIOFiles tIOFiles;
            synchronized (this) {
                mGenDumpIn.put("vInDataPath", tLmpDataPath);
                mGenDumpIn.put("vDumpPath", tLmpDumpPath);
                mGenDumpIn.write(tLmpInPath);
                tIOFiles = mGenDumpIn.copy(); // 由于 InFile 实际内部存在复杂的引用，不能简单的直接完整拷贝
            }
            // 组装指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add(LMP_EXE);
            rCommand.add("-in"); rCommand.add(tLmpInPath);
            rCommand.add(">");   rCommand.add(NO_LOG); // 不需要 log
            // 执行指令
            mEXE.system(String.join(" ", rCommand), tIOFiles);
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
    public void shutdown() {
        try {
            UT.IO.removeDir(mWorkingDir);
            mEXE.removeDir(mWorkingDir);
        } catch (Exception ignored) {}
        mEXE.shutdown();
    }
    /** AutoClosable stuffs */
    @VisibleForTesting public void close() {shutdown();}
}
