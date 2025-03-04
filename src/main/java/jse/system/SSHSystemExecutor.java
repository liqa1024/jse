package jse.system;

import com.jcraft.jsch.ChannelExec;
import jse.code.UT;
import jse.code.io.ISavable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.*;

import static jse.code.CS.SSH_SLEEP_TIME;


/**
 * @author liqa
 * <p> 在 ssh 服务器上执行指令的简单实现 </p>
 */
public class SSHSystemExecutor extends RemoteSystemExecutor implements ISavable {
    final SSHCore mSSH;
    private final int mIOThreadNum;
    SSHSystemExecutor(int aIOThreadNum, SSHCore aSSH) throws Exception {
        super();
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
    
    @Override public final SSHSystemExecutor setWorkingDir(@Nullable String aWorkingDir) throws Exception {return setRemoteWorkingDir(aWorkingDir);}
    /** 这些属性支持创建后修改来方便使用 */
    public SSHSystemExecutor setLocalWorkingDir(String aLocalWorkingDir) {mSSH.setLocalWorkingDir(aLocalWorkingDir); return this;}
    public SSHSystemExecutor setRemoteWorkingDir(String aRemoteWorkingDir) throws Exception {mSSH.setRemoteWorkingDir(aRemoteWorkingDir); mSSH.makeDir("."); return this;}
    public SSHSystemExecutor setCompressLevel(int aCompressLevel) throws Exception {mSSH.setCompressLevel(aCompressLevel); return this;}
    public SSHSystemExecutor setBeforeCommand(String aBeforeCommand) {mSSH.setBeforeCommand(aBeforeCommand); return this;}
    
    /** 现在也支持使用 builder 来构造 */
    public static Builder builder() {return new Builder();}
    public final static class Builder {
        private Builder() {}
        int mIOThreadNum = -1;
        String mUsername; String mHostname; int mPort = 22;
        @Nullable String mLocalWorkingDir = null;
        @Nullable String mRemoteWorkingDir = null;
        @Nullable String mPassword = null;
        @Nullable String mKeyPath = null;
        int mCompressLevel = -1;
        @Nullable String mBeforeCommand = null;
        
        public Builder setIOThreadNumber(int aIOThreadNum) {mIOThreadNum = aIOThreadNum; return this;}
        
        public Builder setUsername(String aUsername) {mUsername = aUsername; return this;}
        public Builder setHostname(String aHostname) {mHostname = aHostname; return this;}
        public Builder setPort(int aPort) {mPort = aPort; return this;}
        public Builder setLocalWorkingDir(String aLocalWorkingDir) {mLocalWorkingDir = aLocalWorkingDir; return this;}
        public Builder setRemoteWorkingDir(String aRemoteWorkingDir) {mRemoteWorkingDir = aRemoteWorkingDir; return this;}
        public Builder setPassword(String aPassword) {mPassword = aPassword; return this;}
        public Builder setKeyPath(String aKeyPath) {mKeyPath = aKeyPath; return this;}
        public Builder setCompressLevel(int aCompressLevel) {mCompressLevel = aCompressLevel; return this;}
        public Builder setBeforeCommand(String aBeforeCommand) {mBeforeCommand = aBeforeCommand; return this;}
        
        public SSHSystemExecutor build() throws Exception {
            return new SSHSystemExecutor(mIOThreadNum, SSHCore.of(
                mUsername, mHostname, mPort,
                mLocalWorkingDir, mRemoteWorkingDir,
                mPassword, mKeyPath,
                mCompressLevel, mBeforeCommand));
        }
    }
    
    
    /** 保存参数部分，和输入格式完全一直 */
    @ApiStatus.Internal
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Override public void save(Map rSaveTo) {
        // 先保存内部 SSH
        mSSH.save(rSaveTo);
        // 注意如果是默认值则不要保存
        if (mIOThreadNum > 0) rSaveTo.put("IOThreadNumber", mIOThreadNum);
    }
    
    
    /**
     * 为了简化初始化参数设定的构造函数，使用 json 或者 Map 的输入来进行初始化（类似 python 的方式）
     * <p>
     * 格式为：
     * <pre><code>
     * {
     *   "IOThreadNumber": ${integerNumberOfThreadNumberForPutAndGetFilesUse},
     *
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
     *   "IOThreadNumber" {@code >} "iothreadnumber" {@code >} "IOThreadNum" {@code >} "iothreadnum" {@code >} "ion"
     *
     *   "Username" {@code >} "username" {@code >} "user" {@code >} "u"
     *   "Hostname" {@code >} "hostname" {@code >} "host" {@code >} "h"
     *   "Port" {@code >} "port" {@code >} "p"
     *   "Password" {@code >} "password" {@code >} "pw"
     *   "KeyPath" {@code >} "keypath" {@code >} "key" {@code >} "k"
     *   "CompressLevel" {@code >} "compresslevel" {@code >} "cl"
     *   "LocalWorkingDir" {@code >} "localworkingdir" {@code >} "lwd"
     *   "RemoteWorkingDir" {@code >} "remoteworkingdir" {@code >} "rwd" {@code >} "wd"
     *   "BeforeCommand" {@code >} "beforecommand" {@code >} "bcommand" {@code >} "bc"
     * </pre>
     * 参数 "IOThreadNumber" 未选定时不开启并行传输，"Port" 未选定时默认为 22，
     * "Password" 未选定时使用 publicKey 密钥认证，"KeyPath" 未选定时使用默认路径的密钥，
     * "CompressLevel" 未选定时不开启压缩，"LocalWorkingDir" 未选定时使用程序运行路径，
     * "RemoteWorkingDir" 未选定时使用 ssh 登录所在的路径
     * @author liqa
     */
    public SSHSystemExecutor(Map<?, ?> aArgs) throws Exception {this(getIOThreadNum(aArgs), SSHCore.load(aArgs));}
    
    private static int getIOThreadNum(Map<?, ?> aArgs) {return ((Number)UT.Code.getWithDefault(aArgs, -1, "IOThreadNumber", "iothreadnumber", "IOThreadNum", "iothreadnum", "ion")).intValue();}
    
    /** SSH 需要使用 ssh 来创建，现在不会本地同步创建 */
    @Override public final void validPath(String aPath) throws Exception {mSSH.validPath(aPath);}
    @Override public final void makeDir(String aDir) throws Exception {mSSH.makeDir(aDir);}
    @Override public final void removeDir(String aDir) throws Exception {mSSH.removeDir(aDir);}
    @Override public final void delete(String aPath) throws Exception {mSSH.delete(aPath);}
    @Override public final boolean isFile(String aFilePath) throws Exception {return mSSH.isFile(aFilePath);}
    @Override public final boolean isDir(String aDir) throws Exception {return mSSH.isDir(aDir);}
    @Override public final String @NotNull[] list(String aDir) throws Exception {return mSSH.list(aDir);}
    
    /** 通过 ssh 直接执行命令 */
    @Override protected Future<Integer> submitSystem__(String aCommand, @NotNull UT.IO.IWriteln aWriteln) {
        final SSHSystemFuture tFuture = new SSHSystemFuture(aCommand, aWriteln);
        // 增加结束时都断开连接的任务
        return toSystemFuture(tFuture, () -> {if (tFuture.mChannelExec != null) tFuture.mChannelExec.disconnect();});
    }
    @Override protected long sleepTime() {return SSH_SLEEP_TIME;}
    
    private final class SSHSystemFuture implements Future<Integer> {
        private final @Nullable ChannelExec mChannelExec;
        private volatile boolean mCancelled = false;
        private final @Nullable Future<Void> mOutTask;
        private SSHSystemFuture(String aCommand, final @NotNull UT.IO.IWriteln aWriteln) {
            // 执行指令
            ChannelExec tChannelExec;
            try {tChannelExec = mSSH.systemChannel(aCommand, noERROutput());}
            catch (Exception e) {printStackTrace(e); tChannelExec = null;}
            mChannelExec = tChannelExec;
            if (mChannelExec == null) {
                mOutTask = null;
                return;
            }
            
            // 由于 jsch 的输入流是临时创建的，因此可以不去获取输入流来避免流死锁
            final boolean tNoSTDOutput = noSTDOutput();
            if (aWriteln==STD_OUT_WRITELN || tNoSTDOutput) {
                if (!tNoSTDOutput) mChannelExec.setOutputStream(System.out, true);
                try {mChannelExec.connect();}
                catch (Exception e) {printStackTrace(e);}
                mOutTask = null;
            } else {
                mOutTask = UT.Par.runAsync(() -> {
                    try (BufferedReader tReader = UT.IO.toReader(mChannelExec.getInputStream(), StandardCharsets.UTF_8); UT.IO.IWriteln tWriteln = aWriteln) {
                        mChannelExec.connect();
                        String tLine;
                        while ((tLine = tReader.readLine()) != null) tWriteln.writeln(tLine);
                    } catch (Exception e) {printStackTrace(e);}
                });
            }
        }
        
        @Override public boolean cancel(boolean mayInterruptIfRunning) {
            if (mChannelExec == null) return false;
            if (mayInterruptIfRunning && mChannelExec.isConnected()) {
                if (!mChannelExec.isClosed() && !mChannelExec.isEOF()) {
                    try {mChannelExec.sendSignal("2");} catch (Exception ignored) {}
                }
                mChannelExec.disconnect();
                mCancelled = true;
                return true;
            }
            return false;
        }
        @Override public boolean isCancelled() {return mCancelled;}
        @Override public boolean isDone() {return mChannelExec==null || mChannelExec.isEOF() || mChannelExec.isClosed() || !mChannelExec.isConnected();}
        @SuppressWarnings("BusyWait")
        @Override public Integer get() throws InterruptedException, ExecutionException {
            if (mChannelExec == null) return -1;
            while (mChannelExec.isConnected() && !mChannelExec.isClosed() && !mChannelExec.isEOF()) Thread.sleep(SSH_SLEEP_TIME);
            if (mOutTask != null) mOutTask.get();
            if (mCancelled) throw new CancellationException();
            int tExitValue = mChannelExec.getExitStatus();
            mChannelExec.disconnect();
            return tExitValue;
        }
        @SuppressWarnings("BusyWait")
        @Override public Integer get(long timeout, @NotNull TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            if (mChannelExec == null) return -1;
            long tic = System.nanoTime();
            long tRestTime = unit.toNanos(timeout);
            while (mChannelExec.isConnected() && !mChannelExec.isClosed() && !mChannelExec.isEOF()) {
                Thread.sleep(SSH_SLEEP_TIME);
                long toc = System.nanoTime();
                tRestTime -= toc - tic;
                tic = toc;
                if (tRestTime <= 0) throw new TimeoutException();
            }
            if (mOutTask != null) mOutTask.get(tRestTime, TimeUnit.NANOSECONDS);
            if (mCancelled) throw new CancellationException();
            int tExitValue = mChannelExec.getExitStatus();
            mChannelExec.disconnect();
            return tExitValue;
        }
    }
    
    
    @Override protected final void putFiles_(Iterable<String> aFiles) throws Exception {
        if (mIOThreadNum>0) {mSSH.putFiles(aFiles, mIOThreadNum);} else {mSSH.putFiles(aFiles);}
    }
    @Override protected final void getFiles_(Iterable<String> aFiles) throws Exception {
        if (mIOThreadNum>0) {mSSH.getFiles(aFiles, mIOThreadNum);} else {mSSH.getFiles(aFiles);}
    }
    /** 需要重写 shutdownFinal 方法将内部 ssh 的关闭包含进去 */
    @Override protected void shutdownFinal() {mSSH.shutdown();}
}
