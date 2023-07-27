package com.jtool.ssh;

import com.jcraft.jsch.*;
import com.jtool.code.UT;
import com.jtool.parallel.ExecutorsEX;
import com.jtool.parallel.IAutoShutdown;
import com.jtool.parallel.IExecutorEX;

import java.util.LinkedList;
import java.util.Map;
import java.util.Vector;

import static com.jtool.code.CS.FILE_SYSTEM_SLEEP_TIME;


/**
 * @author liqa
 * <p> 使用 ssh 连接到服务器 </p>
 * <p> 创建时自动连接服务器，自动跳过初次登录的 "yes/no" 询问 </p>
 * <p> 提供提交指令，断开自动重连，同步目标文件夹等功能 </p>
 * <p> 由于免密登录只支持经典的 openssh 密钥（即需要生成时加上 -m pem），因此还提供密码登录的支持，
 * 但依旧不建议使用密码登录，因为会在代码中出现明文密码 </p>
 * <p>
 * <p> 更加简洁的实现，现在只保留需要的一些功能 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public final class SSHCore implements IAutoShutdown {
    // 本地和远程的工作目录
    private String mLocalWorkingDir_;
    private String mRemoteWorkingDir_;
    String mLocalWorkingDir; // 由于 matlab 下运行时绝对路径获取会出现问题，虽然已经内部实用已经没有问题，但是为了避免第三方库的问题这里本地目录统一使用绝对路径
    String mRemoteWorkingDir;
    // jsch stuffs
    final JSch mJsch;
    private Session mSession;
    // 为了实现断开重连需要暂存密码
    private String mPassword = null;
    // 暂存密钥路径以供保存和加载
    private String mKeyPath = null;
    // 在 system 之前执行的指令
    private String mBeforeCommand = null;
    // 记录是否已经被关闭
    private volatile boolean mDead = false;
    
    /** 内部使用的保存到 json 和从 json 读取 */
    @SuppressWarnings({"unchecked", "rawtypes"})
    public void save(Map rJson) {
        rJson.put("Username", session().getUserName());
        rJson.put("Hostname", session().getHost());
        rJson.put("Port", session().getPort());
        
        if (!mLocalWorkingDir_.isEmpty())
            rJson.put("LocalWorkingDir", mLocalWorkingDir_);
        if (!mRemoteWorkingDir_.isEmpty())
            rJson.put("RemoteWorkingDir", mRemoteWorkingDir_);
        if (mPassword!=null)
            rJson.put("Password", mPassword);
        if (mKeyPath!=null && !mKeyPath.isEmpty() && !mKeyPath.equals(System.getProperty("user.home")+"/.ssh/id_rsa"))
            rJson.put("KeyPath", mKeyPath);
        if (mBeforeCommand!=null)
            rJson.put("BeforeCommand", mBeforeCommand);
        
        int tCompressLevel = -1;
        if (!session().getConfig("compression.c2s").equals("none")) tCompressLevel = Integer.parseInt(session().getConfig("compression_level"));
        if (tCompressLevel > 0)
            rJson.put("CompressLevel", tCompressLevel);
    }
    @SuppressWarnings("rawtypes")
    public static SSHCore load(Map aJson) throws Exception {
        String aUsername = (String) UT.Code.get(aJson, "Username", "username", "user", "u");
        String aHostname = (String) UT.Code.get(aJson, "Hostname", "hostname", "host", "h");
        int aPort = ((Number) UT.Code.getWithDefault(aJson, 22, "Port", "port", "p")).intValue();
        
        String aLocalWorkingDir  = (String) UT.Code.get(aJson, "LocalWorkingDir", "localworkingdir", "lwd");
        String aRemoteWorkingDir = (String) UT.Code.get(aJson, "RemoteWorkingDir", "remoteworkingdir", "rwd", "wd");
        String aPassword         = (String) UT.Code.get(aJson, "Password", "password", "pw");
        String aKeyPath          = (String) UT.Code.getWithDefault(aJson, System.getProperty("user.home")+"/.ssh/id_rsa", "KeyPath", "keypath", "key", "k");
        
        SSHCore rServerSSH = null;
        try {
            rServerSSH = new SSHCore(aUsername, aHostname, aPort).setLocalWorkingDir(aLocalWorkingDir).setRemoteWorkingDir(aRemoteWorkingDir);
            // 如果有密码使用密码连接，否则使用密钥连接
            if (aPassword!=null) {
                rServerSSH.mSession.setPassword(aPassword);
                rServerSSH.mPassword = aPassword;
                rServerSSH.mSession.setConfig("PreferredAuthentications", "password");
            } else {
                rServerSSH.mJsch.addIdentity(aKeyPath);
                rServerSSH.mKeyPath = aKeyPath;
                rServerSSH.mSession.setConfig("PreferredAuthentications", "publickey");
            }
            rServerSSH.mSession.connect();
            
            Object tCompressLevel = UT.Code.get(aJson, "CompressLevel", "compresslevel", "cl");
            Object tBeforeCommand = UT.Code.get(aJson, "BeforeCommand", "beforecommand", "bcommand", "bc");
            
            if (tCompressLevel != null) rServerSSH.setCompressionLevel(((Number)tCompressLevel).intValue());
            if (tBeforeCommand != null) rServerSSH.setBeforeSystem((String)tBeforeCommand);
        } catch (Exception e) {
            // 获取失败会自动关闭
            if (rServerSSH != null) rServerSSH.shutdown();
            throw e;
        }
        
        return rServerSSH;
    }
    
    /** 内部构造函数 */
    private SSHCore(String aUsername, String aHostname, int aPort) throws JSchException {
        mJsch = new JSch();
        mSession = mJsch.getSession(aUsername, aHostname, aPort);
        session().setConfig("StrictHostKeyChecking", "no");
    }
    // 修改本地路径和远程路径
    public SSHCore setLocalWorkingDir(String aLocalWorkingDir) {
        if (mDead) throw new RuntimeException("Can NOT setLocalWorkingDir from a Dead SSH.");
        if (aLocalWorkingDir == null) aLocalWorkingDir = "";
        mLocalWorkingDir_ = aLocalWorkingDir;
        aLocalWorkingDir = UT.IO.toAbsolutePath(aLocalWorkingDir);
        if (!aLocalWorkingDir.isEmpty() && !aLocalWorkingDir.endsWith("/") && !aLocalWorkingDir.endsWith("\\")) aLocalWorkingDir += "/";
        mLocalWorkingDir = aLocalWorkingDir;
        return this;
    }
    public SSHCore setRemoteWorkingDir(String aRemoteWorkingDir) {
        if (mDead) throw new RuntimeException("Can NOT setRemoteWorkingDir from a Dead SSH.");
        if (aRemoteWorkingDir == null) aRemoteWorkingDir = "";
        mRemoteWorkingDir_ = aRemoteWorkingDir;
        if (!aRemoteWorkingDir.isEmpty() && !aRemoteWorkingDir.endsWith("/") && !aRemoteWorkingDir.endsWith("\\")) aRemoteWorkingDir += "/";
        if (aRemoteWorkingDir.startsWith("~/")) aRemoteWorkingDir = aRemoteWorkingDir.substring(2); // JSch 不支持 ~
        mRemoteWorkingDir = aRemoteWorkingDir;
        return this;
    }
    // 设置数据传输的压缩等级
    public SSHCore setCompressionLevel(int aCompressionLevel) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT setCompressionLevel from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 根据输入设置压缩等级
        if (aCompressionLevel > 0) {
            session().setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
            session().setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
            session().setConfig("compression_level", String.valueOf(aCompressionLevel));
        } else {
            session().setConfig("compression.s2c", "none");
            session().setConfig("compression.c2s", "none");
        }
        session().rekey();
        return this;
    }
    // 设置执行 system 之前的附加指令
    public SSHCore setBeforeSystem(String aCommand) {
        if (mDead) throw new RuntimeException("Can NOT setBeforeSystem from a Dead SSH.");
        mBeforeCommand = aCommand;
        return this;
    }
    // 设置密码
    public SSHCore setPassword(String aPassword) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT setPassword from a Dead SSH.");
        mJsch.removeAllIdentity(); // 移除旧的认证
        session().setPassword(aPassword);
        mPassword = aPassword;
        mKeyPath = null;
        session().setConfig("PreferredAuthentications", "password");
        session().rekey();
        return this;
    }
    // 设置密钥路径
    public SSHCore setKey(String aKeyPath) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT setKey from a Dead SSH.");
        mJsch.removeAllIdentity(); // 移除旧的认证
        mJsch.addIdentity(aKeyPath);
        mPassword = null;
        mKeyPath = aKeyPath;
        session().setConfig("PreferredAuthentications", "publickey");
        session().rekey();
        return this;
    }
    
    
    /// 基本方法
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean isConnecting() {return session().isConnected();}
    // 整个 connect 过程都需要同步，避免连接到一半其他线程获取到非法的 session
    public synchronized void connect() throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT reconnect a Dead SSH.");
        if (!mSession.isConnected()) {
            Session oSession = mSession;
            mSession = mJsch.getSession(oSession.getUserName(), oSession.getHost(), oSession.getPort());
            mSession.setPassword(mPassword);
            mSession.setConfig("PreferredAuthentications", oSession.getConfig("PreferredAuthentications"));
            mSession.setConfig("StrictHostKeyChecking", oSession.getConfig("StrictHostKeyChecking"));
            mSession.setConfig("compression.s2c", oSession.getConfig("compression.s2c"));
            mSession.setConfig("compression.c2s", oSession.getConfig("compression.c2s"));
            mSession.setConfig("compression_level", oSession.getConfig("compression_level"));
            mSession.connect();
        }
    }
    public void disconnect() {session().disconnect();}
    @Override public void shutdown() {
        mDead = true;
        session().disconnect();
    }
    // 获取和修改 mSession，需要增加同步来保证每个线程获得的 mSession 都是合适的
    public synchronized Session session() {return mSession;}
    
    
    /// 实用方法
    /** 提交命令的获取指令频道的结构，主要是内部使用，需要手动连接和关闭 */
    public ChannelExec systemChannel(String aCommand, boolean aNoERROutput) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT get systemChannel from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取执行指令的频道
        ChannelExec tChannelExec = (ChannelExec)session().openChannel("exec");
        tChannelExec.setInputStream(null);
        if (!aNoERROutput) tChannelExec.setErrStream(System.err, true); // 注意一定要设置不要关闭，啊，意外关闭其实是不好检测的啊
        if (mBeforeCommand != null && !mBeforeCommand.isEmpty()) aCommand = String.format("%s;%s", mBeforeCommand, aCommand);
        aCommand = String.format("cd %s;%s", mRemoteWorkingDir, aCommand); // 所有指令都会先 cd 到 mRemoteWorkingDir 再执行
        tChannelExec.setCommand(aCommand);
        return tChannelExec;
        // 不需要考虑失败时关闭连接的情况，因为还没有建立连接
    }
    
    /** 递归删除远程服务器的文件夹 */
    public void removeDir(String aDir) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT rmdir from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = null;
        try {
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            if (aDir.equals(".")) aDir = "";
            if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
            String tRemoteDir = mRemoteWorkingDir+aDir;
            // 如果没有此文件夹则直接退出
            if (!isDir_(tChannelSftp, tRemoteDir)) return;
            // 递归子文件夹来删除
            removeDir_(tChannelSftp, tRemoteDir);
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    /** 在远程服务器创建文件夹，支持跨文件夹创建文件夹 */
    public void makeDir(String aDir) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT mkdir from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = null;
        try {
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            if (aDir.equals(".")) aDir = "";
            if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
            String tRemoteDir = mRemoteWorkingDir+aDir;
            // 创建文件夹
            makeDir_(tChannelSftp, tRemoteDir);
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    /** 判断输入是否是远程服务器的文件夹 */
    public boolean isDir(String aDir) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT use isDir from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = null;
        try {
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            if (aDir.equals(".")) aDir = "";
            if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
            String tRemoteDir = mRemoteWorkingDir+aDir;
            // 获取结果
            return isDir_(tChannelSftp, tRemoteDir);
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    /** 判断输入是否是远程服务器的文件 */
    public boolean isFile(String aPath) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT use isFile from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = null;
        try {
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            String tRemotePath = mRemoteWorkingDir+aPath;
            // 获取结果
            return isFile_(tChannelSftp, tRemotePath);
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    /** 删除一个远程服务器的文件 */
    public void delete(String aPath) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT use delete from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = null;
        try {
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            String tRemotePath = mRemoteWorkingDir+aPath;
            // 移除文件
            delete_(tChannelSftp, tRemotePath);
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    /** 合法化一个路径 */
    public void validPath(String aPath) throws Exception  {
        if (mDead) throw new RuntimeException("Can NOT use validPath from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = null;
        try {
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            String tRemoteDir = mRemoteWorkingDir;
            int tEndIdx = aPath.lastIndexOf("/");
            if (tEndIdx > 0) { // 否则不用创建，认为 mRemoteWorkingDir 已经存在
                tRemoteDir += aPath.substring(0, tEndIdx+1);
                makeDir_(tChannelSftp, tRemoteDir);
            }
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    
    
    /**
     * 上传下载多个文件，主要使用的 ssh 方法，一次传输多个文件不用重新连接，创建通道等。
     * 由于是重新写的，会更加注意错误出现后的通道自动关闭，原本没有在源码中使用的方法不再维护
     * @author liqa
     */
    public void putFiles(Iterable<String> aFilePaths) throws Exception {
        ChannelSftp tChannelSftp = null;
        try {
            // 会尝试一次重新连接
            if (!isConnecting()) connect();
            // 获取文件传输通道
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            for (String tFilePath : aFilePaths) if (tFilePath!=null && !tFilePath.isEmpty()) {
                // 检测文件路径是否合法，这里非法路径直接跳过
                String tLocalFile = mLocalWorkingDir+tFilePath;
                if (UT.IO.isFile(tLocalFile)) {
                    // 创建目标文件夹
                    String tRemoteDir = mRemoteWorkingDir;
                    int tEndIdx = tFilePath.lastIndexOf("/");
                    if (tEndIdx > 0) { // 否则不用创建，认为 mRemoteWorkingDir 已经存在
                        tRemoteDir += tFilePath.substring(0, tEndIdx + 1);
                        makeDir_(tChannelSftp, tRemoteDir);
                    }
                    // 上传文件
                    tChannelSftp.put(tLocalFile, tRemoteDir);
                }
            }
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    public void getFiles(Iterable<String> aFilePaths) throws Exception {
        ChannelSftp tChannelSftp = null;
        try {
            // 会尝试一次重新连接
            if (!isConnecting()) connect();
            // 获取文件传输通道
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            for (String tFilePath : aFilePaths) if (tFilePath!=null && !tFilePath.isEmpty()) {
                // 检测文件路径是否合法，这里非法路径直接跳过
                String tRemoteDir = mRemoteWorkingDir + tFilePath;
                if (isFile_(tChannelSftp, tRemoteDir)) {
                    // 创建目标文件夹
                    String tLocalDir = mLocalWorkingDir;
                    int tEndIdx = tFilePath.lastIndexOf("/");
                    if (tEndIdx > 0) { // 否则不用创建，认为 mLocalWorkingDir 已经存在
                        tLocalDir += tFilePath.substring(0, tEndIdx + 1);
                        UT.IO.makeDir(tLocalDir);
                    }
                    // 下载文件
                    tChannelSftp.get(tRemoteDir, tLocalDir);
                }
            }
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    /** 并行版本 */
    public void putFiles(Iterable<String> aFilePaths, int aThreadNumber) throws Exception {
        SftpPool tSftpPool = null;
        try {
            // 创建并发线程池，会自动尝试重新连接
            tSftpPool = new SftpPool(this, aThreadNumber);
            for (final String tFilePath : aFilePaths) if (tFilePath!=null && !tFilePath.isEmpty()) {
                tSftpPool.submit(aChannelSftp -> {
                    // 检测文件路径是否合法，这里非法路径直接跳过
                    String tLocalFile = mLocalWorkingDir+tFilePath;
                    if (UT.IO.isFile(tLocalFile)) {
                        // 创建目标文件夹
                        String tRemoteDir = mRemoteWorkingDir;
                        int tEndIdx = tFilePath.lastIndexOf("/");
                        if (tEndIdx > 0) { // 否则不用创建，认为 mRemoteWorkingDir 已经存在
                            tRemoteDir += tFilePath.substring(0, tEndIdx + 1);
                            SSHCore.makeDir_(aChannelSftp, tRemoteDir);
                        }
                        // 上传文件
                        aChannelSftp.put(tLocalFile, tRemoteDir);
                    }
                });
            }
            // 等待执关闭线程池后等待执行完毕
            tSftpPool.shutdown();
            tSftpPool.awaitTermination();
        } finally {
            // 最后关闭通道，不奢求立刻关闭
            if (tSftpPool != null) tSftpPool.shutdown();
        }
    }
    public void getFiles(Iterable<String> aFilePaths, int aThreadNumber) throws Exception {
        SftpPool tSftpPool = null;
        try {
            // 创建并发线程池，会自动尝试重新连接
            tSftpPool = new SftpPool(this, aThreadNumber);
            for (final String tFilePath : aFilePaths) if (tFilePath!=null && !tFilePath.isEmpty()) {
                tSftpPool.submit(aChannelSftp -> {
                    // 检测文件路径是否合法，这里非法路径直接跳过
                    String tRemoteDir = mRemoteWorkingDir + tFilePath;
                    if (isFile_(aChannelSftp, tRemoteDir)) {
                        // 创建目标文件夹
                        String tLocalDir = mLocalWorkingDir;
                        int tEndIdx = tFilePath.lastIndexOf("/");
                        if (tEndIdx > 0) { // 否则不用创建，认为 mLocalWorkingDir 已经存在
                            tLocalDir += tFilePath.substring(0, tEndIdx + 1);
                            UT.IO.makeDir(tLocalDir);
                        }
                        // 下载文件
                        aChannelSftp.get(tRemoteDir, tLocalDir);
                    }
                });
            }
            // 等待执关闭线程池后等待执行完毕
            tSftpPool.shutdown();
            tSftpPool.awaitTermination();
        } finally {
            // 最后关闭通道，不奢求立刻关闭
            if (tSftpPool != null) tSftpPool.shutdown();
        }
    }
    
    
    /// 内部方法，这里统一认为目录结尾有 '/'，且不会自动添加
    /** 判断是否是文件夹，无论是什么情况报错都返回 false */
    private static boolean isDir_(ChannelSftp aChannelSftp, String aDir) {
        SftpATTRS tAttrs = null;
        try {tAttrs = aChannelSftp.stat(aDir);} catch (SftpException ignored) {}
        return tAttrs != null && tAttrs.isDir();
    }
    /** 判断是否是文件，无论是什么情况报错都返回 false */
    private static boolean isFile_(ChannelSftp aChannelSftp, String aPath) {
        SftpATTRS tAttrs = null;
        try {tAttrs = aChannelSftp.stat(aPath);} catch (SftpException ignored) {}
        return tAttrs != null && !tAttrs.isDir();
    }
    /** 判断是否是文件，无论是什么情况报错都返回 false */
    private static void delete_(ChannelSftp aChannelSftp, String aPath) throws Exception {
        SftpATTRS tAttrs = null;
        try {tAttrs = aChannelSftp.stat(aPath);} catch (SftpException ignored) {}
        if (tAttrs != null && !tAttrs.isDir()) aChannelSftp.rm(aPath);
    }
    /** 在远程服务器创建文件夹，实现跨文件夹创建文件夹 */
    private static void makeDir_(ChannelSftp aChannelSftp, String aDir) throws Exception {
        if (isDir_(aChannelSftp, aDir)) return;
        // 如果目录不存在，则需要创建目录
        int tEndIdx = aDir.lastIndexOf("/", aDir.length()-2);
        if (tEndIdx > 0) {
            String tParent = aDir.substring(0, tEndIdx+1);
            // 递归创建上级目录
            makeDir_(aChannelSftp, tParent);
        }
        // 创建当前目录
        aChannelSftp.mkdir(aDir);
    }
    /** 递归删除远程服务器的文件夹 */
    @SuppressWarnings("unchecked")
    private static void removeDir_(ChannelSftp aChannelSftp, String aDir) throws Exception {
        Vector<ChannelSftp.LsEntry> tFiles = aChannelSftp.ls(aDir);
        if (tFiles == null) return;
        for (ChannelSftp.LsEntry tFile : tFiles) {
            String tName = tFile.getFilename();
            if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
            if (tFile.getAttrs().isDir()) {removeDir_(aChannelSftp, aDir+tName+"/");}
            else {aChannelSftp.rm(aDir+tName);}
        }
        aChannelSftp.rmdir(aDir);
    }
    
    
    /// 并发部分
    /** 类似线程池的 Sftp 通道，可以重写实现提交任务并且并发的上传和下载 */
    private static class SftpPool {
        interface ISftpTask {void doTask(ChannelSftp aChannelSftp) throws Exception;}
        private final LinkedList<ISftpTask> mTaskList = new LinkedList<>();
        private volatile boolean mDead = false;
        private final IExecutorEX mPool;
        
        @SuppressWarnings("BusyWait")
        SftpPool(SSHCore aSSH, int aThreadNumber) throws Exception {
            mPool = ExecutorsEX.newFixedThreadPool(aThreadNumber);
            try {
                // 会尝试一次重新连接
                if (!aSSH.isConnecting()) aSSH.connect();
                // 提交长期任务
                for (int i = 0; i < aThreadNumber; ++i) {
                    final ChannelSftp tChannelSftp = (ChannelSftp) aSSH.session().openChannel("sftp");
                    mPool.execute(() -> {
                        try {
                            tChannelSftp.connect();
                            // 每个 Sftp 都从 mTaskList 中竞争获取 task 并执行
                            while (true) {
                                ISftpTask tTask;
                                synchronized (mTaskList) {tTask = mTaskList.pollFirst();}
                                if (tTask != null) {
                                    tTask.doTask(tChannelSftp);
                                } else {
                                    if (mDead) break;
                                    // 否则继续等待任务输入
                                    Thread.sleep(FILE_SYSTEM_SLEEP_TIME);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            // 最后关闭通道
                            tChannelSftp.disconnect();
                        }
                    });
                }
            } catch (Exception e) {
                this.shutdown();
                throw e;
            }
        }
        
        public void shutdown() {mDead = true; mPool.shutdown();}
        public void awaitTermination() throws InterruptedException {mPool.awaitTermination();}
        
        private void submit(ISftpTask aSftpTask) {
            if (mDead) throw new RuntimeException("Can NOT submit tasks to a Dead SftpPool.");
            synchronized (mTaskList) {mTaskList.addLast(aSftpTask);}
        }
    }
    
    // 手动加载 UT，会自动重新设置工作目录，会在调用静态函数 get 或者 load 时自动加载保证路径的正确性
    static {UT.IO.init();}
}
