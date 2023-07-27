package com.jtool.lmp;

import com.jtool.code.UT;
import com.jtool.code.collection.Pair;
import com.jtool.iofile.IHasIOFiles;
import com.jtool.iofile.IInFile;
import com.jtool.system.ISystemExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.Future;

import static com.jtool.code.CS.Exec.EXE;
import static com.jtool.code.CS.FILE_SYSTEM_SLEEP_TIME;
import static com.jtool.code.CS.WORKING_DIR;

/**
 * 长时的 lammps 运行器，开启一个长时挂起的 lammps 程序来运行，可以绕过 system 指令限制
 * @author liqa
 */
public final class LongTimeLmpExecutor implements ILmpExecutor {
    private final static long DEFAULT_FILE_SYSTEM_WAIT_TIME = 0;
    private final static int TOLERANT = 3;
    
    private final String mWorkingDir;
    
    private final Map<Pair<String, Future<Integer>>, Boolean> mLongTimeLmps;
    private final ISystemExecutor mEXE;
    private final String mLmpExe; // 仅用于重启长时 lammps
    
    private boolean mDoNotClose;
    private long mFileSystemWaitTime;
    private long mSleepTime;
    
    public LongTimeLmpExecutor(ISystemExecutor aEXE, boolean aDoNotClose, String aLmpExe, @Nullable String aLogPath, int aMaxParallelNum) throws Exception {
        mEXE = aEXE;
        mDoNotClose = aDoNotClose;
        mLmpExe = aLmpExe;
        mLongTimeLmps = new HashMap<>();
        mFileSystemWaitTime = DEFAULT_FILE_SYSTEM_WAIT_TIME;
        mSleepTime = FILE_SYSTEM_SLEEP_TIME;
        // 设置一下工作目录
        mWorkingDir = WORKING_DIR.replaceAll("%n", "LTLMP@"+UT.Code.randID());
        // 提交长时的 lammps 任务
        IInFile tLongTimeInFile = LmpIn.LONG_TIME();
        try {
            for (int i = 0; i < aMaxParallelNum; ++i) {
                // 获取长时任务的 lammps 输入文件
                String tLongTimeLmpDir = mWorkingDir+"LMP@"+UT.Code.randID()+"/";
                String tLongTimeInPath = tLongTimeLmpDir+"main";
                // 设置目录
                tLongTimeInFile.put("vBufferPath", tLongTimeLmpDir+"buffer");
                tLongTimeInFile.put("vInPath", tLongTimeLmpDir+"in");
                tLongTimeInFile.put("vShutdownPath", tLongTimeLmpDir+"shutdown");
                // 输出为 in 文件
                tLongTimeInFile.write(tLongTimeInPath);
                // 组装指令
                String tCommand = mLmpExe + " -in " + tLongTimeInPath;
                // 运行，内部保证会考虑到 tLongTimeInFile 易失的问题
                Future<Integer> tLongTimeLmpTask = aLogPath==null ? mEXE.submitSystem(tCommand, tLongTimeInFile) : mEXE.submitSystem(tCommand, aMaxParallelNum>1 ? aLogPath+"-"+i : aLogPath, tLongTimeInFile);
                // 设置资源
                mLongTimeLmps.put(new Pair<>(tLongTimeLmpDir, tLongTimeLmpTask), false);
            }
        } catch (Exception e) {
            // 虽然这样后续即使设置了不要关闭也会关闭，不很好处理因此不考虑
            this.shutdown();
            throw e;
        }
    }
    public LongTimeLmpExecutor(ISystemExecutor aEXE, String aLmpExe, String aLogPath, int aMaxParallelNum) throws Exception {this(aEXE, false, aLmpExe, aLogPath, aMaxParallelNum);}
    public LongTimeLmpExecutor(ISystemExecutor aEXE, String aLmpExe,                  int aMaxParallelNum) throws Exception {this(aEXE, aLmpExe, null, aMaxParallelNum);}
    public LongTimeLmpExecutor(ISystemExecutor aEXE, String aLmpExe, String aLogPath                     ) throws Exception {this(aEXE, aLmpExe, aLogPath, 1);}
    public LongTimeLmpExecutor(ISystemExecutor aEXE, String aLmpExe                                      ) throws Exception {this(aEXE, aLmpExe, null);}
    public LongTimeLmpExecutor(                      String aLmpExe, String aLogPath, int aMaxParallelNum) throws Exception {this(EXE, true, aLmpExe, aLogPath, aMaxParallelNum);}
    public LongTimeLmpExecutor(                      String aLmpExe,                  int aMaxParallelNum) throws Exception {this(aLmpExe, null, aMaxParallelNum);}
    public LongTimeLmpExecutor(                      String aLmpExe, String aLogPath                     ) throws Exception {this(aLmpExe, aLogPath, 1);}
    public LongTimeLmpExecutor(                      String aLmpExe                                      ) throws Exception {this(aLmpExe, null);}
    
    
    /** 是否在关闭此实例时顺便关闭内部 exe */
    public LongTimeLmpExecutor setDoNotClose(boolean aDoNotClose) {mDoNotClose = aDoNotClose; return this;}
    public LongTimeLmpExecutor setFileSystemWaitTime(long aFileSystemWaitTime) {mFileSystemWaitTime = aFileSystemWaitTime; return this;}
    public LongTimeLmpExecutor setSleepTime(long aSleepTime) {mSleepTime = aSleepTime; return this;}
    
    @Override public ISystemExecutor exec() {return mEXE;}
    private void printStackTrace(Throwable aThrowable) {if (!mEXE.noERROutput()) aThrowable.printStackTrace();}
    
    /** 内部使用的向任务分配资源的方法 */
    private synchronized @Nullable Pair<String, Future<Integer>> assignLmp_() {
        for (Map.Entry<Pair<String, Future<Integer>>, Boolean> tEntry : mLongTimeLmps.entrySet()) {
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
        mLongTimeLmps.put(aLmp, false);
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
    @Override public int run(String aInFile, IHasIOFiles aIOFiles) {
        if (mDead) throw new RuntimeException("Can NOT run from this Dead LmpExecutor.");
        // 先尝试获取资源
        Pair<String, Future<Integer>> tLmp;
        try {tLmp = assignLmp();}
        catch (InterruptedException e) {printStackTrace(e); return -1;}
        // 拷贝到需要的 in 文件位置
        try {UT.IO.copy(aInFile, tLmp.first+"in");}
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
        try {aInFile.write(tLmp.first+"in");}
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
    private int run_(Pair<String, Future<Integer>> aLmp, IHasIOFiles aIOFiles) {
        try {
            // 注意到 lammps 本身输出时不能自动创建文件夹，因此需要手动先合法化输出文件夹
            Set<String> rODirs = new HashSet<>();
            for (String aOFile : aIOFiles.getOFiles()) {
                int tEndIdx = aOFile.lastIndexOf("/");
                if (tEndIdx > 0) rODirs.add(aOFile.substring(0, tEndIdx+1));
            }
            for (String aODir : rODirs) mEXE.makeDir(aODir);
            
            String tLmpInPath = aLmp.first+"in";
            // 向目录放入 in 文件，需要这样操作
            if (mEXE.needSyncIOFiles()) mEXE.putFiles(UT.Code.merge(tLmpInPath, aIOFiles.getIFiles()));
            // 等待执行完成，注意对于特殊系统，需要设置等待时间等待文件系统同步
            if (mFileSystemWaitTime > 0) Thread.sleep(mFileSystemWaitTime);
            int tTolerant = TOLERANT;
            while (mEXE.isFile(tLmpInPath)) {
                // 每次都检查一下 lammps 进程是否存活，如果挂了则需要重启（带有容忍度）
                if (aLmp.second.isDone()) {
                    --tTolerant;
                    if (!mEXE.noERROutput()) {
                        int tExitValue;
                        try {tExitValue = aLmp.second.get();} catch (Exception e) {tExitValue = -1;}
                        System.err.println("WARNING: Long-Time Lammps in '"+aLmp.first+"' Dead Unexpectedly, exit value: "+tExitValue+", try to run again...");
                        System.err.println("WARNING: Note that rerunning Lammps will NOT have log file.");
                        if (tTolerant < 0) System.err.println("ERROR: Long-Time Lammps in '"+aLmp.first+"' Dead Unexpectedly more than "+TOLERANT+" times");
                    }
                    if (tTolerant < 0) {
                        // 无法重启的 lammps 依旧直接归还，下次运行依旧会再次尝试重启，只是这个运行会失败
                        returnLmp(aLmp);
                        return -1;
                    } else {
                        // 尝试重启，移除 shutdown 文件（如果存在），并重新拷贝输入文件（可能被意外删除）
                        String tLmpShutdownPath = aLmp.first+"shutdown";
                        if (mEXE.isFile(tLmpShutdownPath)) {
                            mEXE.delete(tLmpShutdownPath);
                        } else if (mEXE.isDir(tLmpShutdownPath)) {
                            mEXE.removeDir(tLmpShutdownPath);
                        }
                        IInFile tLongTimeInFile = LmpIn.LONG_TIME();
                        String tLmpMainPath = aLmp.first+"main";
                        tLongTimeInFile.put("vBufferPath", aLmp.first+"buffer");
                        tLongTimeInFile.put("vInPath", tLmpInPath);
                        tLongTimeInFile.put("vShutdownPath", tLmpShutdownPath);
                        tLongTimeInFile.write(tLmpMainPath);
                        String tCommand = mLmpExe + " -in " + tLmpMainPath;
                        // 重新指定程序 future，方便起见重新开始的程序不再记录 log，因为目前 api 下会直接覆盖掉原本 log
                        aLmp.second = mEXE.submitSystem(tCommand, tLongTimeInFile);
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
    @Override public void shutdown() {
        mDead = true;
        for (Pair<String, Future<Integer>> tPair : mLongTimeLmps.keySet()) {
            String tShutdownPath = tPair.first+"shutdown";
            try {
                UT.IO.write(tShutdownPath, "");
                if (mEXE.needSyncIOFiles()) mEXE.putFiles(Collections.singleton(tShutdownPath));
            } catch (Exception ignored) {}
        }
        try {
            UT.IO.removeDir(mWorkingDir);
            if (mEXE.needSyncIOFiles()) mEXE.removeDir(mWorkingDir);
        } catch (Exception ignored) {}
        if (!mDoNotClose) {
            mEXE.shutdown();
        }
    }
}
