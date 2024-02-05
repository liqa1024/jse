package jse.lmp;

import jse.io.IIOFiles;
import jse.io.IInFile;
import jse.parallel.AbstractHasAutoShutdown;
import jse.parallel.MPI;
import jse.system.ISystemExecutor;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import static jse.code.CS.Exec.EXE;

/**
 * 基于 {@link NativeLmp} 的 lammps 运行器实现
 * @author liqa
 */
@ApiStatus.Experimental
public class NativeLmpExecutor extends AbstractHasAutoShutdown implements ILmpExecutor {
    @Override public ISystemExecutor exec() {return EXE;}
    
    protected final NativeLmp mLmp;
    private boolean mNoERROutput = false;
    
    public NativeLmpExecutor(NativeLmp aLmp) {mLmp = aLmp;}
    public NativeLmpExecutor(String[] aArgs, @Nullable MPI.Comm aComm) throws NativeLmp.Error {this(new NativeLmp(aArgs, aComm));}
    public NativeLmpExecutor(String[] aArgs) throws NativeLmp.Error {this(new NativeLmp(aArgs));}
    public NativeLmpExecutor() throws NativeLmp.Error {this(new NativeLmp());}
    /** 是否在关闭此实例时顺便关闭内部 NativeLmp */
    @Override public NativeLmpExecutor setDoNotShutdown(boolean aDoNotShutdown) {setDoNotShutdown_(aDoNotShutdown); return this;}
    /** 是否输出详细的错误信息，这在 MPI 情况下会比较有用 */
    public NativeLmpExecutor setNoERROutput() {return setNoERROutput(true);}
    public NativeLmpExecutor setNoERROutput(boolean aNoERROutput) {mNoERROutput = aNoERROutput; return this;}
    public boolean noERROutput() {return mNoERROutput;}
    
    protected void printStackTrace_(Exception aException) {
        if (!mNoERROutput) aException.printStackTrace(System.err);
    }
    
    
    @Override public int run(IInFile aInFile) {
        try {
            mLmp.file(aInFile);
            mLmp.clear();
            return 0;
        } catch (Exception e) {
            printStackTrace_(e);
            return -1;
        }
    }
    @Override public int run(String aInFile, IIOFiles aIOFiles) {
        try {
            mLmp.file(aInFile);
            mLmp.clear();
            return 0;
        } catch (Exception e) {
            printStackTrace_(e);
            return -1;
        }
    }
    
    @Override protected void shutdownInternal_() {mLmp.shutdown();}
    @Override protected void closeInternal_() {mLmp.close();}
}
