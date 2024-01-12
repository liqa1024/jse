package jtool.lmp;

import jtool.io.IIOFiles;
import jtool.io.IInFile;
import jtool.parallel.AbstractHasAutoShutdown;
import jtool.parallel.MPI;
import jtool.system.ISystemExecutor;
import org.jetbrains.annotations.ApiStatus;

import static jtool.code.CS.Exec.EXE;

/**
 * 基于 {@link NativeLmp} 的 lammps 运行器实现；
 * 由于 MPI 环境下不同进程下随机数流不同的问题，这里暂不能使用
 * @author liqa
 */
@ApiStatus.Experimental
public class NativeLmpExecutor extends AbstractHasAutoShutdown implements ILmpExecutor {
    @Override public ISystemExecutor exec() {return EXE;}
    
    private final NativeLmp mLmp;
    public NativeLmpExecutor(NativeLmp aLmp) {mLmp = aLmp;}
    public NativeLmpExecutor(String[] aArgs, long aComm) throws NativeLmp.Error {this(new NativeLmp(aArgs, aComm));}
    public NativeLmpExecutor(String[] aArgs, MPI.Comm aComm) throws NativeLmp.Error {this(new NativeLmp(aArgs, aComm));}
    public NativeLmpExecutor(String[] aArgs) throws NativeLmp.Error {this(new NativeLmp(aArgs));}
    public NativeLmpExecutor() throws NativeLmp.Error {this(new NativeLmp());}
    /** 是否在关闭此实例时顺便关闭内部 NativeLmp */
    @Override public NativeLmpExecutor setDoNotShutdown(boolean aDoNotShutdown) {setDoNotShutdown_(aDoNotShutdown); return this;}
    
    @Override public int run(IInFile aInFile) {
        try {
            mLmp.file(aInFile);
            mLmp.clear();
            return 0;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return -1;
        }
    }
    @Override public int run(String aInFile, IIOFiles aIOFiles) {
        try {
            mLmp.file(aInFile);
            mLmp.clear();
            return 0;
        } catch (Exception e) {
            e.printStackTrace(System.err);
            return -1;
        }
    }
    
    @Override protected void shutdownInternal_() {mLmp.shutdown();}
    @Override protected void closeInternal_() {mLmp.close();}
}
