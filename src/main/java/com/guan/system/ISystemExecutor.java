package com.guan.system;


import com.guan.io.IHasIOFiles;
import com.guan.parallel.IHasThreadPool;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> 通用的系统指令执行器接口，实现类似 matlab 的 system 指令功能，
 * 在此基础上增加了类似 Executor 的功能，可以后台运行等 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public interface ISystemExecutor extends IHasThreadPool {
    int system_NO(String aCommand                     ); // No Output
    int system   (String aCommand                     );
    int system   (String aCommand, String aOutFilePath);
    
    /** 在原本的 system 基础上，增加了附加更多输入输出文件的功能，使用 IHasIOFiles 来附加 */
    int system_NO(String aCommand                     , IHasIOFiles aIOFiles); // No Output
    int system   (String aCommand                     , IHasIOFiles aIOFiles);
    int system   (String aCommand, String aOutFilePath, IHasIOFiles aIOFiles);
    
    /** submit stuffs */
    Future<Integer> submitSystem_NO(String aCommand                                           ); // No Output
    Future<Integer> submitSystem   (String aCommand                                           );
    Future<Integer> submitSystem   (String aCommand, String aOutFilePath                      );
    Future<Integer> submitSystem_NO(String aCommand                     , IHasIOFiles aIOFiles); // No Output
    Future<Integer> submitSystem   (String aCommand                     , IHasIOFiles aIOFiles);
    Future<Integer> submitSystem   (String aCommand, String aOutFilePath, IHasIOFiles aIOFiles);
    
    /** 获取字符串输出而不是退出代码 */
    List<String> system_str(String aCommand);
    List<String> system_str(String aCommand, IHasIOFiles aIOFiles);
    Future<List<String>> submitSystem_str(String aCommand);
    Future<List<String>> submitSystem_str(String aCommand, IHasIOFiles aIOFiles);
}
