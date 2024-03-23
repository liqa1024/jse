package jse.lmp;

import jse.code.UT;
import jse.io.IIOFiles;
import jse.io.IInFile;
import jse.parallel.AbstractHasAutoShutdown;
import jse.system.ISystemExecutor;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;

import static jse.code.OS.EXE;
import static jse.code.Conf.WORKING_DIR_OF;

/**
 * 一般的 lammps 运行器实现，使用输入的 ISystemExecutor 来执行
 * @author liqa
 */
public final class LmpExecutor extends AbstractHasAutoShutdown implements ILmpExecutor {
    private final String mWorkingDir;
    
    private final ISystemExecutor mEXE;
    private final String mLmpExe;
    private final @Nullable String mLogPath;
    
    public LmpExecutor(ISystemExecutor aEXE, String aLmpExe, @Nullable String aLogPath) {
        mEXE = aEXE;
        mLmpExe = aLmpExe;
        mLogPath = aLogPath;
        // 最后设置一下工作目录，这里一定要求相对路径
        mWorkingDir = UT.IO.toRelativePath(WORKING_DIR_OF("LMP@"+UT.Code.randID()));
    }
    public LmpExecutor(String aLmpExe, @Nullable String aLogPath) {this(EXE, aLmpExe, aLogPath); setDoNotShutdown_(true);}
    public LmpExecutor(String aLmpExe) {this(aLmpExe, null);}
    public LmpExecutor(ISystemExecutor aEXE, String aLmpExe) {this(aEXE, aLmpExe, null);}
    
    /** 是否在关闭此实例时顺便关闭内部 exe */
    @Override public LmpExecutor setDoNotShutdown(boolean aDoNotShutdown) {setDoNotShutdown_(aDoNotShutdown); return this;}
    
    @Override public ISystemExecutor exec() {return mEXE;}
    private void printStackTrace(Throwable aThrowable) {if (!mEXE.noERROutput()) aThrowable.printStackTrace(System.err);}
    
    
    @Override public int run(String aInFile, IIOFiles aIOFiles) {
        if (mDead) throw new RuntimeException("Can NOT run from this Dead LmpExecutor.");
        // 注意到 lammps 本身输出时不能自动创建文件夹，因此需要手动先合法化输出文件夹
        Set<String> rODirs = new HashSet<>();
        for (String aOFile : aIOFiles.getOFiles()) {
            int tEndIdx = aOFile.lastIndexOf("/");
            if (tEndIdx > 0) rODirs.add(aOFile.substring(0, tEndIdx+1));
        }
        try {for (String aODir : rODirs) mEXE.makeDir(aODir);}
        catch (Exception e) {printStackTrace(e); return -1;}
        // 组装指令
        String tCommand = mLmpExe + " -in " + aInFile;
        // 执行指令
        return mLogPath==null ? mEXE.system(tCommand, aIOFiles) : mEXE.system(tCommand, mLogPath, aIOFiles);
    }
    @Override public int run(IInFile aInFile) {
        if (mDead) throw new RuntimeException("Can NOT run from this Dead LmpExecutor.");
        // 由于存在并行，需要在工作目录中创建临时输入文件
        String tLmpInPath = mWorkingDir+"IN@"+UT.Code.randID();
        try {
            // 输入文件初始化
            aInFile.write(tLmpInPath);
            // 执行指令
            return run(tLmpInPath, aInFile);
        } catch (Exception e) {
            printStackTrace(e); return -1;
        } finally {
            try {
                UT.IO.delete(tLmpInPath);
                if (mEXE.needSyncIOFiles()) mEXE.delete(tLmpInPath);
            } catch (Exception ignored) {}
        }
    }
    
    /** 程序结束时删除自己的临时工作目录，并且会关闭 EXE */
    private volatile boolean mDead = false;
    @Override protected void shutdown_() {
        mDead = true;
        try {
            UT.IO.removeDir(mWorkingDir);
            if (mEXE.needSyncIOFiles()) mEXE.removeDir(mWorkingDir);
        } catch (Exception ignored) {}
    }
    @Override protected void shutdownInternal_() {mEXE.shutdown();}
    @Override protected void closeInternal_() {mEXE.close();}
}
