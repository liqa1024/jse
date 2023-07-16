package com.jtool.system;


import org.jetbrains.annotations.NotNull;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> SystemExecutor 的另一种抽象实现，在远程运行的情况 </p>
 */
public abstract class RemoteSystemExecutor extends AbstractSystemExecutor {
    public RemoteSystemExecutor() {super();}
    
    public final boolean needSyncIOFiles() {return true;}
    /** stuff to override */
    protected abstract Future<Integer> submitSystem__(String aCommand, @NotNull IPrintlnSupplier aPrintln);
    public abstract void putFiles(Iterable<String> aFiles) throws Exception;
    public abstract void getFiles(Iterable<String> aFiles) throws Exception;
}
