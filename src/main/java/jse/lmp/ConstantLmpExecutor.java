package jse.lmp;

import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.Pair;
import jse.io.IIOFiles;
import jse.io.IInFile;
import jse.parallel.AbstractHasAutoShutdown;
import jse.system.ISystemExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static jse.code.OS.EXE;
import static jse.code.CS.FILE_SYSTEM_SLEEP_TIME;
import static jse.code.CS.FILE_SYSTEM_TIMEOUT;
import static jse.code.Conf.WORKING_DIR_OF;

/**
 * 长时的 lammps 运行器，开启一个长时挂起的 lammps 程序来运行，可以绕过 system 指令限制
 * @author liqa
 */
public final class ConstantLmpExecutor extends AbstractHasAutoShutdown implements ILmpExecutor {
    private final static long DEFAULT_FILE_SYSTEM_WAIT_TIME = 0;
    private final static int TOLERANT = 3;
    /** 一些目录设定， %n: unique job name, %i: index of lammps，注意只有 OUTFILE_PATH 支持 %i */
    private final static String DEFAULT_OUTFILE_DIR = ".temp/ltlmp/";
    private final static String DEFAULT_OUTFILE_PATH = DEFAULT_OUTFILE_DIR+"out-%i-%n";
    
    private final String mWorkingDir;
    
    private final Map<Pair<String, Future<Integer>>, Boolean> mConstantLmpProcess;
    private final ISystemExecutor mEXE;
    
    /** 这些仅用于重启长时 lammps */
    private final String mLmpExe;
    private final String mLogPath;
    private int mLmpIndex = -1;
    
    private long mFileSystemWaitTime;
    private long mSleepTime;
    
    public ConstantLmpExecutor(ISystemExecutor aEXE, boolean aDoNotShutdown, String aLmpExe, @NotNull String aLogPath, int aMaxParallelNum) throws Exception {
        mEXE = aEXE;
        setDoNotShutdown_(aDoNotShutdown);
        mLmpExe = aLmpExe;
        mConstantLmpProcess = new HashMap<>();
        mFileSystemWaitTime = DEFAULT_FILE_SYSTEM_WAIT_TIME;
        mSleepTime = FILE_SYSTEM_SLEEP_TIME;
        // 设置一下工作目录，这里一定要求相对路径
        String tUniqueJobName = "CLMP@"+UT.Code.randID();
        mWorkingDir = UT.IO.toRelativePath(WORKING_DIR_OF(tUniqueJobName));
        mLogPath = aLogPath.replaceAll("%n", tUniqueJobName);
        // 提交长时的 lammps 任务
        IInFile tConstantInFile = LmpIn.CONSTANT();
        try {
            for (int i = 0; i < aMaxParallelNum; ++i) {
                // 获取长时任务的 lammps 目录
                String tConstantLmpDir = mWorkingDir+"LMP@"+UT.Code.randID()+"/";
                // 运行
                Future<Integer> tConstantLmpTask = submitConstantLmp(tConstantInFile, tConstantLmpDir);
                // 设置资源
                mConstantLmpProcess.put(new Pair<>(tConstantLmpDir, tConstantLmpTask), false);
            }
        } catch (Exception e) {
            // 虽然这样后续即使设置了不要关闭也会关闭，但也不很好处理因此不考虑
            this.shutdown();
            throw e;
        }
    }
    public ConstantLmpExecutor(ISystemExecutor aEXE, String aLmpExe, String aLogPath, int aMaxParallelNum) throws Exception {this(aEXE, false, aLmpExe, aLogPath, aMaxParallelNum);}
    public ConstantLmpExecutor(ISystemExecutor aEXE, String aLmpExe,                  int aMaxParallelNum) throws Exception {this(aEXE, aLmpExe, DEFAULT_OUTFILE_PATH, aMaxParallelNum);}
    public ConstantLmpExecutor(ISystemExecutor aEXE, String aLmpExe, String aLogPath                     ) throws Exception {this(aEXE, aLmpExe, aLogPath, 1);}
    public ConstantLmpExecutor(ISystemExecutor aEXE, String aLmpExe                                      ) throws Exception {this(aEXE, aLmpExe, DEFAULT_OUTFILE_PATH);}
    public ConstantLmpExecutor(                      String aLmpExe, String aLogPath, int aMaxParallelNum) throws Exception {this(EXE, true, aLmpExe, aLogPath, aMaxParallelNum);}
    public ConstantLmpExecutor(                      String aLmpExe,                  int aMaxParallelNum) throws Exception {this(aLmpExe, DEFAULT_OUTFILE_PATH, aMaxParallelNum);}
    public ConstantLmpExecutor(                      String aLmpExe, String aLogPath                     ) throws Exception {this(aLmpExe, aLogPath, 1);}
    public ConstantLmpExecutor(                      String aLmpExe                                      ) throws Exception {this(aLmpExe, DEFAULT_OUTFILE_PATH);}
    
    
    /** 是否在关闭此实例时顺便关闭内部 exe */
    @Override public ConstantLmpExecutor setDoNotShutdown(boolean aDoNotShutdown) {setDoNotShutdown_(aDoNotShutdown); return this;}
    public ConstantLmpExecutor setFileSystemWaitTime(long aFileSystemWaitTime) {mFileSystemWaitTime = aFileSystemWaitTime; return this;}
    public ConstantLmpExecutor setSleepTime(long aSleepTime) {mSleepTime = aSleepTime; return this;}
    
    @Override public ISystemExecutor exec() {return mEXE;}
    private void printStackTrace(Throwable aThrowable) {if (!mEXE.noERROutput()) aThrowable.printStackTrace(System.err);}
    
    private synchronized Future<Integer> submitConstantLmp(IInFile aConstantInFile, String aConstantLmpDir) throws IOException {
        ++mLmpIndex;
        // 一些通用的输入文件的设置
        String tConstantInPath = aConstantLmpDir+"main";
        aConstantInFile.put("vBufferPath", aConstantLmpDir+"buffer");
        aConstantInFile.put("vInPath", aConstantLmpDir+"in");
        aConstantInFile.put("vShutdownPath", aConstantLmpDir+"shutdown");
        aConstantInFile.write(tConstantInPath);
        // 组装指令
        String tCommand = mLmpExe + " -in " + tConstantInPath;
        // 提交运行，内部保证会考虑到 tConstantInFile 易失的问题；对于 constant 的，默认情况同 slurm 会输出到文件而不是控制台
        return mEXE.submitSystem(tCommand, mLogPath.replaceAll("%i", String.valueOf(mLmpIndex)), aConstantInFile);
    }
    
    
    /** 内部使用的向任务分配资源的方法 */
    private synchronized @Nullable Pair<String, Future<Integer>> assignLmp_() {
        for (Map.Entry<Pair<String, Future<Integer>>, Boolean> tEntry : mConstantLmpProcess.entrySet()) {
            if (!tEntry.getValue()) {
                tEntry.setValue(true);
                return tEntry.getKey();
            }
        }
        // 所有节点的任务都分配满了，输出 null
        return null;
    }
    /** 内部使用的任务完成归还资源的方法 */
    private synchronized void returnLmp(Pair<String, Future<Integer>> aLmp) {
        mConstantLmpProcess.put(aLmp, false);
    }
    
    private @NotNull Pair<String, Future<Integer>> assignLmp() throws InterruptedException {
        // 先尝试获取资源
        Pair<String, Future<Integer>> tLmp = assignLmp_();
        if (tLmp == null && !mEXE.noERROutput()) {
            System.err.println("WARNING: Can NOT to assign resource for this Long-Time Lammps Executor temporarily, this exec blocks until there are any free resource.");
            System.err.println("It may be caused by too large number of parallels.");
        }
        while (tLmp == null) {
            Thread.sleep(FILE_SYSTEM_SLEEP_TIME);
            tLmp = assignLmp_();
        }
        return tLmp;
    }
    
    
    /** 运行则直接通过将输入文件放入指定目录来实现 */
    @Override public int run(String aInFile, IIOFiles aIOFiles) {
        if (mDead) throw new RuntimeException("Can NOT run from this Dead LmpExecutor.");
        // 先尝试获取资源
        Pair<String, Future<Integer>> tLmp;
        try {tLmp = assignLmp();}
        catch (InterruptedException e) {printStackTrace(e); return -1;}
        // 拷贝到需要的 in 文件位置
        try {UT.IO.copy(aInFile, tLmp.mFirst+"in");}
        catch (Exception e) {
            printStackTrace(e);
            // 出错则归还资源
            returnLmp(tLmp);
            return -1;
        }
        return run_(tLmp, aIOFiles);
    }
    @Override public int run(IInFile aInFile) {
        if (mDead) throw new RuntimeException("Can NOT run from this Dead LmpExecutor.");
        // 先尝试获取资源
        Pair<String, Future<Integer>> tLmp;
        try {tLmp = assignLmp();}
        catch (InterruptedException e) {printStackTrace(e); return -1;}
        // 输入文件初始化
        try {aInFile.write(tLmp.mFirst+"in");}
        catch (Exception e) {
            printStackTrace(e);
            // 出错则归还资源
            returnLmp(tLmp);
            return -1;
        }
        // 执行指令
        return run_(tLmp, aInFile);
    }
    @SuppressWarnings("BusyWait")
    private int run_(Pair<String, Future<Integer>> aLmp, IIOFiles aIOFiles) {
        try {
            // 注意到 lammps 本身输出时不能自动创建文件夹，因此需要手动先合法化输出文件夹
            Set<String> rODirs = new HashSet<>();
            for (String aOFile : aIOFiles.getOFiles()) {
                int tEndIdx = aOFile.lastIndexOf("/");
                if (tEndIdx > 0) rODirs.add(aOFile.substring(0, tEndIdx+1));
            }
            for (String aODir : rODirs) mEXE.makeDir(aODir);
            
            String tLmpInPath = aLmp.mFirst+"in";
            // 向目录放入 in 文件，需要这样操作
            if (mEXE.needSyncIOFiles()) mEXE.putFiles(AbstractCollections.merge(tLmpInPath, aIOFiles.getIFiles()));
            // 等待执行完成，注意对于特殊系统，需要设置等待时间等待文件系统同步
            if (mFileSystemWaitTime > 0) Thread.sleep(mFileSystemWaitTime);
            int tTolerant = TOLERANT;
            while (mEXE.isFile(tLmpInPath)) {
                // 每次都检查一下 lammps 进程是否存活，如果挂了则需要重启（带有容忍度）
                if (aLmp.mSecond.isDone()) {
                    --tTolerant;
                    if (!mEXE.noERROutput()) {
                        int tExitValue;
                        try {tExitValue = aLmp.mSecond.get();} catch (Exception e) {tExitValue = -1;}
                        System.err.println("WARNING: Long-Time Lammps in '"+aLmp.mFirst+"' Dead Unexpectedly, exit value: "+tExitValue+", try to run again...");
                        if (tTolerant < 0) System.err.println("ERROR: Long-Time Lammps in '"+aLmp.mFirst+"' Dead Unexpectedly more than "+TOLERANT+" times");
                    }
                    if (tTolerant < 0) {
                        // 无法重启的 lammps 依旧直接归还，下次运行依旧会再次尝试重启，只是这个运行会失败
                        returnLmp(aLmp);
                        return -1;
                    } else {
                        // 尝试重启，移除 shutdown 文件（如果存在），并重新拷贝输入文件（可能被意外删除）
                        String tLmpShutdownPath = aLmp.mFirst+"shutdown";
                        if (mEXE.isFile(tLmpShutdownPath)) {
                            mEXE.delete(tLmpShutdownPath);
                        } else if (mEXE.isDir(tLmpShutdownPath)) {
                            mEXE.removeDir(tLmpShutdownPath);
                        }
                        // 重新指定程序 future
                        aLmp.mSecond = submitConstantLmp(LmpIn.CONSTANT(), aLmp.mFirst);
                    }
                }
                Thread.sleep(mSleepTime);
            }
            if (mFileSystemWaitTime > 0) Thread.sleep(mFileSystemWaitTime);
            // 执行完成后下载输出文件
            if (mEXE.needSyncIOFiles()) mEXE.getFiles(aIOFiles.getOFiles());
        } catch (Exception e) {
            printStackTrace(e);
            return -1;
        } finally {
            // 无论怎样归还资源
            returnLmp(aLmp);
            // 不需要手动删除输入文件因为 lammps 内部在完成时自动删除了
        }
        // 正常结束返回 0
        return 0;
    }
    
    
    /** 程序结束时删除自己的临时工作目录，并且会结束 lammps，关闭 EXE */
    private volatile boolean mDead = false;
    @Override protected void shutdown_() {
        mDead = true;
        for (Pair<String, Future<Integer>> tLMP : mConstantLmpProcess.keySet()) {
            String tShutdownPath = tLMP.mFirst+"shutdown";
            try {
                UT.IO.writeText(tShutdownPath, "");
                if (mEXE.needSyncIOFiles()) mEXE.putFiles(Collections.singletonList(tShutdownPath));
            } catch (Exception ignored) {}
        }
        // 注意需要等待程序结束后再删除文件夹，如果没有结束则手动强制结束
        for (Pair<String, Future<Integer>> tLMP : mConstantLmpProcess.keySet()) {
            try {tLMP.mSecond.get(FILE_SYSTEM_TIMEOUT, TimeUnit.MILLISECONDS);}
            catch (Exception e) {tLMP.mSecond.cancel(true);}
        }
        try {
            UT.IO.removeDir(mWorkingDir);
            if (mEXE.needSyncIOFiles()) mEXE.removeDir(mWorkingDir);
        } catch (Exception ignored) {}
    }
    @Override protected void shutdownInternal_() {mEXE.shutdown();}
    @Override protected void closeInternal_() {mEXE.close();}
}
