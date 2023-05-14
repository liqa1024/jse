package com.guan.system;

import com.guan.code.UT;
import com.guan.ssh.ServerSSH;
import com.jcraft.jsch.ChannelExec;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.util.Map;


/**
 * @author liqa
 * <p> 在 ssh 服务器上执行指令的简单实现 </p>
 */
public class SSHSystemExecutor extends RemoteSystemExecutor {
    @Deprecated public static SSHSystemExecutor get_(int aThreadNum, ServerSSH aSSH) throws Exception {return new SSHSystemExecutor(aThreadNum, 2, aSSH);}
    
    final ServerSSH mSSH;
    int mIOThreadNum;
    SSHSystemExecutor(int aThreadNum, int aIOThreadNum, ServerSSH aSSH) throws Exception {
        super(aThreadNum>0 ? newPool(aThreadNum) : SERIAL_EXECUTOR);
        mIOThreadNum = aIOThreadNum; mSSH = aSSH;
        // 需要初始化一下远程的工作目录，只需要创建目录即可，因为原本 ssh 设计时不是这样初始化的
        // 注意初始化失败时需要抛出异常并且执行关闭操作
        try {
            mSSH.makeDir(".");
        } catch (Exception e) {
            this.shutdown(); // 构造函数中不调用多态方法
            throw e;
        }
    }
    
    /**
     * 为了简化初始化参数设定的构造函数，使用 json 或者 Map 的输入来进行初始化（类似 python 的方式）
     * <p>
     * 格式为：
     * <pre><code>
     * {
     *   "ThreadNumber": ${integerNumberOfThreadNumberForSubmitSystemUse},
     *   "IOThreadNumber": ${integerNumberOfThreadNumberForPutAndGetFilesUse},
     *   "Username": "${yourUserName}",
     *   "Hostname": "${ipOfHost}",
     *   "Port": ${integerNumberOfPort},
     *   "Password": "${yourPassword}",
     *   "KeyPath": "path/to/the/public/key",
     *   "CompressLevel": ${integerNumberOfCompressLevel}
     *   "LocalWorkingDir": "path/to/the/local/working/dir",
     *   "RemoteWorkingDir": "path/to/the/remote/working/dir",
     *   "BeforeCommand": "${commandBeforeAnyCommand}"
     * }
     * </code></pre>
     * 其中名称大小写敏感（因为实现起来比较麻烦），但是存在简写，简写优先级为：
     * <pre>
     *   "ThreadNumber" > "threadnumber" > "ThreadNum" > "threadnum" > "nThreads" > "nthreads" > "n"
     *   "IOThreadNumber" > "iothreadnumber" > "IOThreadNum" > "iothreadnum" > "ion"
     *   "Username" > "username" > "user" > "u"
     *   "Hostname" > "hostname" > "host" > "h"
     *   "Port" > "port" > "p"
     *   "Password" > "password" > "pw"
     *   "KeyPath" > "keypath" > "key" > "k"
     *   "CompressLevel" > "compresslevel" > "cl"
     *   "LocalWorkingDir" > "localworkingdir" > "lwd"
     *   "RemoteWorkingDir" > "remoteworkingdir" > "rwd" > "wd"
     *   "BeforeCommand" > "beforecommand" > "bcommand" > "bc"
     * </pre>
     * 参数 "ThreadNumber" 未选定时不使用线程池，参数 "IOThreadNumber" 未选定时不开启并行传输，"Port" 未选定时默认为 22，
     * "Password" 未选定时使用 publicKey 密钥认证，"KeyPath" 未选定时使用默认路径的密钥，
     * "CompressLevel" 未选定时不开启压缩，"LocalWorkingDir" 未选定时使用程序运行路径，
     * "RemoteWorkingDir" 未选定时使用 ssh 登录所在的路径
     * @author liqa
     */
    public SSHSystemExecutor(                                  Map<?, ?> aArgs) throws Exception {this(getThreadNum(aArgs), getIOThreadNum(aArgs), ServerSSH.load(aArgs));}
    public SSHSystemExecutor(int aThreadNum,                   Map<?, ?> aArgs) throws Exception {this(aThreadNum, getIOThreadNum(aArgs), ServerSSH.load(aArgs));}
    public SSHSystemExecutor(int aThreadNum, int aIOThreadNum, Map<?, ?> aArgs) throws Exception {this(aThreadNum, aIOThreadNum, ServerSSH.load(aArgs));}
    
    public final static String[] THREAD_NUMBER_KEYS = {"ThreadNumber", "threadnumber", "ThreadNum", "threadnum", "nThreads", "nthreads", "n"};
    public final static String[] IO_THREAD_NUMBER_KEYS = {"IOThreadNumber", "iothreadnumber", "IOThreadNum", "iothreadnum", "ion"};
    public static int getThreadNum(Map<?, ?> aArgs) {return ((Number)UT.Code.getWithDefault(aArgs, -1, (Object[])THREAD_NUMBER_KEYS)).intValue();}
    public static int getIOThreadNum(Map<?, ?> aArgs) {return ((Number)UT.Code.getWithDefault(aArgs, -1, (Object[])IO_THREAD_NUMBER_KEYS)).intValue();}
    
    
    
    /** 通过 ssh 直接执行命令 */
    @SuppressWarnings("BusyWait")
    @Override public int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln) {
        int tExitValue = -1;
        ChannelExec tChannel = null;
        try (IPrintln tPrintln = aPrintln.get()) {
            tChannel = mSSH.systemChannel(aCommand);
            if (tPrintln != null) {
                try (BufferedReader tReader = UT.IO.toReader(tChannel.getInputStream())) {
                    tChannel.connect();
                    String tLine;
                    while ((tLine = tReader.readLine()) != null) tPrintln.println(tLine);
                }
            } else {
                tChannel.connect();
            }
            // 手动等待直到结束
            while (!tChannel.isEOF()) Thread.sleep(100);
            // 获取退出代码
            tExitValue = tChannel.getExitStatus();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (tChannel != null) tChannel.disconnect();
        }
        return tExitValue;
    }
    @Override protected final void putFiles(Iterable<String> aFiles) throws Exception {
        if (mIOThreadNum>0) mSSH.putFiles(aFiles, mIOThreadNum); else mSSH.putFiles(aFiles);
    }
    @Override protected final void getFiles(Iterable<String> aFiles) throws Exception {
        if (mIOThreadNum>0) mSSH.getFiles(aFiles, mIOThreadNum); else mSSH.getFiles(aFiles);
    }
    
    
    /** 需要重写 shutdown 方法将内部 ssh 的关闭包含进去 */
    @Override public void shutdown() {mSSH.shutdown(); super.shutdown();}
    @Override public void shutdownNow() {mSSH.shutdown(); super.shutdownNow();}
}
