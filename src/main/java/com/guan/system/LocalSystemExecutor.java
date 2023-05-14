package com.guan.system;


import com.guan.io.IHasIOFiles;
import com.guan.code.UT;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> SystemExecutor 的一般实现，直接在本地运行 </p>
 */
public class LocalSystemExecutor extends AbstractSystemExecutor {
    public LocalSystemExecutor(int aThreadNum) {super(aThreadNum); mRuntime = Runtime.getRuntime();}
    public LocalSystemExecutor() {this(1);}
    final Runtime mRuntime;
    
    
    @Override public int system(String aCommand, @Nullable IPrintln aPrintln) {
        int tExitValue;
        Process tProcess = null;
        try {
            // 执行指令
            tProcess = mRuntime.exec(aCommand);
            // 读取执行的输出（由于内部会对输出自动 buffer，获取 stream 和执行的顺序不重要）
            if (aPrintln != null) try (BufferedReader tReader = UT.IO.toReader(tProcess.getInputStream())) {
                String tLine;
                while ((tLine = tReader.readLine()) != null) {
                    aPrintln.println(tLine);
                }
            }
            // 等待执行完成
            tExitValue = tProcess.waitFor();
        } catch (Exception e) {
            tExitValue = -1;
            e.printStackTrace();
        } finally {
            // 无论程序如何结束都停止进程
            if (tProcess != null) tProcess.destroyForcibly();
        }
        return tExitValue;
    }
    
    /** 对于本地的带有 IOFiles 的没有区别 */
    @Override public int system(String aCommand, @Nullable IPrintln aPrintln, IHasIOFiles aIOFiles) {return system(aCommand, aPrintln);}
    
    
    /** 对于本地直接忽略 IHasIOFiles */
    @Override public Future<Integer> submitSystem_NO(final String aCommand                            , IHasIOFiles aIOFiles) {return submitSystem_NO(aCommand);}
    @Override public Future<Integer> submitSystem   (final String aCommand                            , IHasIOFiles aIOFiles) {return submitSystem(aCommand);}
    @Override public Future<Integer> submitSystem   (final String aCommand, final String aOutFilePath , IHasIOFiles aIOFiles) {return submitSystem(aCommand, aOutFilePath);}
    @Override public Future<List<String>> submitSystem_str(final String aCommand, IHasIOFiles aIOFiles) {return submitSystem_str(aCommand);}
}
