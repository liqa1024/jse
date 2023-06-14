package com.jtool.system;


import com.jtool.code.UT;
import com.jtool.iofile.IHasIOFiles;
import com.jtool.parallel.IExecutorEX;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Future;

/**
 * @author liqa
 * <p> SystemExecutor 的一般实现，直接在本地运行 </p>
 */
public class LocalSystemExecutor extends AbstractThreadPoolSystemExecutor {
    public LocalSystemExecutor(int aThreadNum) {this(newPool(aThreadNum));}
    public LocalSystemExecutor() {this(SERIAL_EXECUTOR);}
    
    protected LocalSystemExecutor(IExecutorEX aPool) {super(aPool); mRuntime = Runtime.getRuntime();}
    final Runtime mRuntime;
    
    /** 本地则在本地创建目录即可 */
    @Override public final void makeDir(String aDir) throws IOException {UT.IO.makeDir(aDir);}
    @Override public final void removeDir(String aDir) throws IOException {UT.IO.removeDir(aDir);}
    
    @Override public int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln) {
        // 对于空指令专门优化，不执行操作
        if (aCommand == null || aCommand.isEmpty()) return -1;
        
        int tExitValue;
        Process tProcess = null;
        try (IPrintln tPrintln = aPrintln.get()) {
            // 执行指令
            tProcess = mRuntime.exec(aCommand);
            // 设置错误输出流，直接另开一个线程管理
            final Process fProcess = tProcess;
            new Thread(() -> {
                try (BufferedReader tErrReader = UT.IO.toReader(fProcess.getErrorStream())) {
                    boolean tERROutPut = !noERROutput();
                    // 对于 Process，由于内部已经有 buffered 输出流，因此必须要获取输出流并遍历，避免发生流死锁
                    String tLine;
                    while ((tLine = tErrReader.readLine()) != null) {
                        if (tERROutPut) System.err.println(tLine);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
            // 读取执行的输出（由于内部会对输出自动 buffer，获取 stream 和执行的顺序不重要）
            try (BufferedReader tOutReader = UT.IO.toReader(fProcess.getInputStream())) {
                boolean tSTDOutPut = !noSTDOutput();
                // 对于 Process，由于内部已经有 buffered 输出流，因此必须要获取输出流并遍历，避免发生流死锁
                String tLine;
                while ((tLine = tOutReader.readLine()) != null) {
                    if (tSTDOutPut) tPrintln.println(tLine);
                }
            }
            // 等待执行完成
            tExitValue = fProcess.waitFor();
        } catch (Exception e) {
            tExitValue = -1;
            e.printStackTrace();
        } finally {
            // 无论程序如何结束都停止进程
            if (tProcess != null) tProcess.destroyForcibly();
        }
        return tExitValue;
    }
    /** 这样保证只会在执行的时候获取 println */
    @Override protected Future<Integer> submitSystem_(final String aCommand, final @NotNull IPrintlnSupplier aPrintln) {return pool().submit(() -> system_(aCommand, aPrintln));}
    
    
    /** 对于本地的带有 IOFiles 的没有区别 */
    @Override public int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln, IHasIOFiles aIOFiles) {return system_(aCommand, aPrintln);}
    @Override protected Future<Integer> submitSystem_(String aCommand, @NotNull IPrintlnSupplier aPrintln, IHasIOFiles aIOFiles) {return submitSystem_(aCommand, aPrintln);}
    @Override public Future<List<Integer>> batchSubmit_(Iterable<String> aCommands, IHasIOFiles aIOFiles) {return batchSubmit_(aCommands);}
}
