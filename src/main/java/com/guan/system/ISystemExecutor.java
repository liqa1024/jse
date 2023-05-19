package com.guan.system;


import com.guan.io.IHasIOFiles;
import com.guan.parallel.IHasThreadPool;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.List;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> 通用的系统指令执行器接口，实现类似 matlab 的 system 指令功能，
 * 在此基础上增加了类似 Executor 的功能，可以后台运行等 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public interface ISystemExecutor extends IHasThreadPool {
    /** 用来处理各种程序因为输出的目录不存在而报错的情况，方便在任何位置直接创建目录或者移除目录 */
    boolean makeDir(String aDir);
    @VisibleForTesting default boolean mkdir(String aDir) {return makeDir(aDir);}
    @ApiStatus.Internal boolean removeDir(String aDir);
    @VisibleForTesting @ApiStatus.Internal default boolean rmdir(String aDir) {return removeDir(aDir);}
    
    /** 砍掉原本的 _NO 接口，改为直接设置是否输出到控制台 */
    default ISystemExecutor setNoConsoleOutput() {return setNoConsoleOutput(true);}
    ISystemExecutor setNoConsoleOutput(boolean aNoConsoleOutput);
    boolean noConsoleOutput();
    
    int system(String aCommand                     );
    int system(String aCommand, String aOutFilePath);
    /** 在原本的 system 基础上，增加了附加更多输入输出文件的功能，使用 IHasIOFiles 来附加 */
    int system(String aCommand                     , IHasIOFiles aIOFiles);
    int system(String aCommand, String aOutFilePath, IHasIOFiles aIOFiles);
    
    /** submit stuffs */
    Future<Integer> submitSystem(String aCommand                                           );
    Future<Integer> submitSystem(String aCommand, String aOutFilePath                      );
    Future<Integer> submitSystem(String aCommand                     , IHasIOFiles aIOFiles);
    Future<Integer> submitSystem(String aCommand, String aOutFilePath, IHasIOFiles aIOFiles);
    
    /** BatchSubmit stuffs，不获取输出，Future 获取到的退出码数组大小不一定是 put 的总数（可能会发生合并）*/
    Future<List<Integer>> getSubmit();
    void putSubmit(String aCommand);
    void putSubmit(String aCommand, IHasIOFiles aIOFiles);
    
    
    /** 获取字符串输出而不是退出代码 */
    List<String> system_str(String aCommand);
    List<String> system_str(String aCommand, IHasIOFiles aIOFiles);
    Future<List<String>> submitSystem_str(String aCommand);
    Future<List<String>> submitSystem_str(String aCommand, IHasIOFiles aIOFiles);
}
