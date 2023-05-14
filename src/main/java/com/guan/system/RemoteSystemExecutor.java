package com.guan.system;


import com.guan.io.IHasIOFiles;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> SystemExecutor 的另一种抽象实现，在远程运行的情况 </p>
 */
public abstract class RemoteSystemExecutor extends AbstractSystemExecutor {
    protected RemoteSystemExecutor(int aThreadNum) {super(aThreadNum);}
    public RemoteSystemExecutor() {this(1);}
    
    
    /**
     * 注意 IHasIOFiles 的结果是易失的，退出函数体后获取的结果可能会变，因此这里需要拷贝一份
     * @author liqa
     */
    @Override public final Future<Integer> submitSystem_NO(String aCommand                      , IHasIOFiles aIOFiles) {return submitSystem_NO_(aCommand, aIOFiles.copy());}
    @Override public final Future<Integer> submitSystem   (String aCommand                      , IHasIOFiles aIOFiles) {return submitSystem_   (aCommand, aIOFiles.copy());}
    @Override public final Future<Integer> submitSystem   (String aCommand, String aOutFilePath , IHasIOFiles aIOFiles) {return submitSystem_   (aCommand, aOutFilePath, aIOFiles.copy());}
    @Override public final Future<List<String>> submitSystem_str(String aCommand, IHasIOFiles aIOFiles) {return submitSystem_str_(aCommand, aIOFiles.copy());}
    
    
    protected Future<Integer> submitSystem_NO_(final String aCommand                            , final IHasIOFiles aIOFiles) {return pool().submit(() -> system_NO(aCommand, aIOFiles));}
    protected Future<Integer> submitSystem_   (final String aCommand                            , final IHasIOFiles aIOFiles) {return pool().submit(() -> system   (aCommand, aIOFiles));}
    protected Future<Integer> submitSystem_   (final String aCommand, final String aOutFilePath , final IHasIOFiles aIOFiles) {return pool().submit(() -> system   (aCommand, aOutFilePath, aIOFiles));}
    protected Future<List<String>> submitSystem_str_(final String aCommand, final IHasIOFiles aIOFiles) {return pool().submit(() -> system_str(aCommand, aIOFiles));}
    
    /** stuff to override */
    public abstract int system(String aCommand, @Nullable IPrintln aPrintln);
    public abstract int system(String aCommand, @Nullable IPrintln aPrintln, IHasIOFiles aIOFiles);
}
