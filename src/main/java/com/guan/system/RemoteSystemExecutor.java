package com.guan.system;


import com.guan.io.IHasIOFiles;
import com.guan.parallel.IExecutorEX;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

import static com.guan.code.CS.ERR_FUTURES;

/**
 * @author liqa
 * <p> SystemExecutor 的另一种抽象实现，在远程运行的情况 </p>
 */
public abstract class RemoteSystemExecutor extends AbstractThreadPoolSystemExecutor {
    protected RemoteSystemExecutor(IExecutorEX aPool) {super(aPool);}
    
    
    /** 注意 IHasIOFiles 的结果是易失的，退出函数体后获取的结果可能会变，因此这里需要拷贝一份 */
    @Override protected final Future<Integer> submitSystem_(String aCommand, @NotNull IPrintlnSupplier aPrintln, IHasIOFiles aIOFiles) {return submitSystem__(aCommand, aPrintln, aIOFiles.copy());}
    
    
    /** 一般的远程 system 实现，需要上传和下载文件 */
    @Override public final int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln, IHasIOFiles aIOFiles) {
        try {putFiles(aIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return -1;}
        int tExitValue = system_(aCommand, aPrintln);
        try {getFiles(aIOFiles.getOFiles());} catch (Exception e) {e.printStackTrace(); return tExitValue==0 ? -1 : tExitValue;}
        return tExitValue;
    }
    @Override protected Future<List<Integer>> batchSubmit_(Iterable<String> aCommands, final IHasIOFiles aIOFiles) {
        try {putFiles(aIOFiles.getIFiles());} catch (Exception e) {e.printStackTrace(); return ERR_FUTURES;}
        Future<List<Integer>> tFuture = batchSubmit_(aCommands);
        // 由于下载文件是必要的，因此使用这个方法来在 tFuture 完成时下载
        return CompletableFuture.supplyAsync(() -> {
            List<Integer> tExitValues = Collections.singletonList(-1);
            try {tExitValues = tFuture.get();} catch (Exception e) {e.printStackTrace();}
            try {getFiles(aIOFiles.getOFiles());} catch (Exception e) {e.printStackTrace();}
            return tExitValues;
        });
    }
    
    /** 一般的远程 submitSystem 实现，直接借用 system 的相关实现 */
    protected Future<Integer> submitSystem_ (final String aCommand, final @NotNull IPrintlnSupplier aPrintln                            ) {return pool().submit(() -> system_(aCommand, aPrintln          ));}
    protected Future<Integer> submitSystem__(final String aCommand, final @NotNull IPrintlnSupplier aPrintln, final IHasIOFiles aIOFiles) {return pool().submit(() -> system_(aCommand, aPrintln, aIOFiles));}
    
    /** stuff to override */
    protected abstract void putFiles(Iterable<String> aFiles) throws Exception;
    protected abstract void getFiles(Iterable<String> aFiles) throws Exception;
    public abstract int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln);
}
