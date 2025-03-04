package jse.system;

import jse.parallel.IThreadPool;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> 通用的系统指令执行器接口，实现类似 matlab 的 system 指令功能，
 * 在此基础上增加了类似 Executor 的功能，可以后台运行等 </p>
 * <p>
 * 要求这些方法是线程安全的，可以同一个实例并行运行同一个方法
 */
@SuppressWarnings("UnusedReturnValue")
public interface ISystemExecutor extends IThreadPool {
    /** 用来处理各种程序因为输出的目录不存在而报错的情况，方便在任何位置直接创建目录或者移除目录 */
    void validPath(String aPath) throws Exception;
    void makeDir(String aDir) throws Exception;
    @VisibleForTesting default void mkdir(String aDir) throws Exception {makeDir(aDir);}
    @ApiStatus.Internal void removeDir(String aDir) throws Exception;
    @VisibleForTesting @ApiStatus.Internal default void rmdir(String aDir) throws Exception {removeDir(aDir);}
    void delete(String aPath) throws Exception;
    boolean isFile(String aFilePath) throws Exception;
    boolean isDir(String aDir) throws Exception;
    String @NotNull[] list(String aDir) throws Exception;
    /** 单独的上传和下载的操作，用于方便使用 */
    void putFiles(Iterable<? extends CharSequence> aFiles) throws Exception;
    void getFiles(Iterable<? extends CharSequence> aFiles) throws Exception;
    void putFiles(String... aFiles) throws Exception;
    void getFiles(String... aFiles) throws Exception;
    
    
    /** 现在支持设置工作目录 */
    ISystemExecutor setWorkingDir(@Nullable String aWorkingDir) throws Exception;
    
    /** 砍掉原本的 _NO 接口，改为直接设置是否输出到控制台 */
    default ISystemExecutor setNoSTDOutput() {return setNoSTDOutput(true);}
    ISystemExecutor setNoSTDOutput(boolean aNoSTDOutput);
    boolean noSTDOutput();
    default ISystemExecutor setNoERROutput() {return setNoERROutput(true);}
    ISystemExecutor setNoERROutput(boolean aNoERROutput);
    boolean noERROutput();
    
    int system(String aCommand                     );
    int system(String aCommand, String aOutFilePath);
    
    /** submit stuffs */
    Future<Integer> submitSystem(String aCommand                     );
    Future<Integer> submitSystem(String aCommand, String aOutFilePath);
    
    /** 获取字符串输出而不是退出代码 */
    List<String> system_str(String aCommand);
    Future<List<String>> submitSystem_str(String aCommand);
}
