package com.guan.system;


import com.guan.parallel.IThreadPoolContainer;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> 通用的系统指令执行器接口，实现类似 matlab 的 system 指令功能，
 * 在此基础上增加了类似 Executor 的功能，可以后台运行等 </p>
 */
public interface ISystemExecutor extends IThreadPoolContainer {
    int system_NO(String aCommand); // No Output
    int system(String aCommand);
    int system(String aCommand, String aOutFilePath);
    int system(String aCommand, PrintStream aOutPrintStream);
    Future<Integer> submitSystem_NO(String aCommand); // No Output
    Future<Integer> submitSystem(String aCommand);
    Future<Integer> submitSystem(String aCommand, String aOutFilePath);
    Future<Integer> submitSystem(String aCommand, PrintStream aOutPrintStream);
    
    /** 在原本的 system 基础上，增加了附加相关的输入输出文件的功能，
     * 主要用于在远程服务器情况下，在执行任务之前和任务完成后进行同步，使用 separator 进行分割 */
    int system_NO(String aCommand,                              @Nullable Object aSeparator, String aFirstInFile, Object... aElse);
    int system   (String aCommand,                              @Nullable Object aSeparator, String aFirstInFile, Object... aElse);
    int system   (String aCommand, String      aOutFilePath   , @Nullable Object aSeparator, String aFirstInFile, Object... aElse);
    int system   (String aCommand, PrintStream aOutPrintStream, @Nullable Object aSeparator, String aFirstInFile, Object... aElse);
    int system_NO(String aCommand,                              @Nullable Object aSeparator1, @Nullable Object aSeparator2, String aFirstOutFile, Object... aElse);
    int system   (String aCommand,                              @Nullable Object aSeparator1, @Nullable Object aSeparator2, String aFirstOutFile, Object... aElse);
    int system   (String aCommand, String      aOutFilePath   , @Nullable Object aSeparator1, @Nullable Object aSeparator2, String aFirstOutFile, Object... aElse);
    int system   (String aCommand, PrintStream aOutPrintStream, @Nullable Object aSeparator1, @Nullable Object aSeparator2, String aFirstOutFile, Object... aElse);
    Future<Integer> submitSystem_NO(String aCommand                             , @Nullable Object aSeparator, String aFirstInFile, Object... aElse);
    Future<Integer> submitSystem   (String aCommand                             , @Nullable Object aSeparator, String aFirstInFile, Object... aElse);
    Future<Integer> submitSystem   (String aCommand, String      aOutFilePath   , @Nullable Object aSeparator, String aFirstInFile, Object... aElse);
    Future<Integer> submitSystem   (String aCommand, PrintStream aOutPrintStream, @Nullable Object aSeparator, String aFirstInFile, Object... aElse);
    Future<Integer> submitSystem_NO(String aCommand                             , @Nullable Object aSeparator1, @Nullable Object aSeparator2, String aFirstOutFile, Object... aElse);
    Future<Integer> submitSystem   (String aCommand                             , @Nullable Object aSeparator1, @Nullable Object aSeparator2, String aFirstOutFile, Object... aElse);
    Future<Integer> submitSystem   (String aCommand, String      aOutFilePath   , @Nullable Object aSeparator1, @Nullable Object aSeparator2, String aFirstOutFile, Object... aElse);
    Future<Integer> submitSystem   (String aCommand, PrintStream aOutPrintStream, @Nullable Object aSeparator1, @Nullable Object aSeparator2, String aFirstOutFile, Object... aElse);
}
