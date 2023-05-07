package com.guan.ssh;

import com.guan.code.Decryptor;
import com.guan.code.Encryptor;
import com.guan.code.Task;
import com.guan.code.UT;
import com.jcraft.jsch.*;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * @author liqa
 * <p> 使用 ssh 连接到服务器 </p>
 * <p> 创建时自动连接服务器，自动跳过初次登录的 "yes/no" 询问 </p>
 * <p> 提供提交指令，断开自动重连，同步目标文件夹等功能 </p>
 * <p> 由于免密登录只支持经典的 openssh 密钥（即需要生成时加上 -m pem），因此还提供密码登录的支持，
 * 但依旧不建议使用密码登录，因为会在代码中出现明文密码 </p>
 */
@SuppressWarnings({"UnusedReturnValue", "BusyWait"})
public final class ServerSSH {
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
    private boolean mDead = false;
    
    /// hooks, 修改这个来实现重写，我也不知道这个方法是不是合理
    // 发生内部参数改变都需要调用一下这个函数
    Runnable doMemberChange = () -> {};
    
    /// 保存到文件以及从文件加载
    @SuppressWarnings("rawtypes")
    public void save(String aFilePath) throws IOException {
        Map rJson = new LinkedHashMap();
        save(rJson);
        Writer tWriter = UT.IO.toWriter(aFilePath);
        (new JsonBuilder(rJson)).writeTo(tWriter);
        tWriter.close();
    }
    @SuppressWarnings("rawtypes")
    public static ServerSSH load(String aFilePath) throws Exception {
        Reader tReader = UT.IO.toReader(aFilePath);
        Map tJson = (Map) (new JsonSlurper()).parse(tReader);
        tReader.close();
        return load(tJson);
    }
    // 带有密码的读写
    @SuppressWarnings("rawtypes")
    public void save(String aFilePath, String aKey) throws Exception {
        Map rJson = new LinkedHashMap();
        save(rJson);
        Encryptor tEncryptor = new Encryptor(aKey);
        UT.IO.write(aFilePath, tEncryptor.getData((new JsonBuilder(rJson)).toString()));
    }
    @SuppressWarnings("rawtypes")
    public static ServerSSH load(String aFilePath, String aKey) throws Exception {
        Decryptor tDecryptor = new Decryptor(aKey);
        Map tJson = (Map) (new JsonSlurper()).parseText(tDecryptor.get(UT.IO.readAllBytes(aFilePath)));
        return load(tJson);
    }
    // 偏向于内部使用的保存到 json 和从 json 读取
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
    public static ServerSSH load(Map aJson) throws Exception {
        String aUsername = (String) aJson.get("Username");
        String aHostname = (String) aJson.get("Hostname");
        int aPort = ((Number) aJson.get("Port")).intValue();
        
        String aLocalWorkingDir = null;
        String aRemoteWorkingDir = null;
        String aPassword = null;
        String aKeyPath = null;
        if (aJson.containsKey("LocalWorkingDir"))  aLocalWorkingDir  = (String) aJson.get("LocalWorkingDir");
        if (aJson.containsKey("RemoteWorkingDir")) aRemoteWorkingDir = (String) aJson.get("RemoteWorkingDir");
        if (aJson.containsKey("Password"))         aPassword         = (String) aJson.get("Password");
        if (aJson.containsKey("KeyPath"))          aKeyPath          = (String) aJson.get("KeyPath");
        
        ServerSSH rServerSSH;
        if (aPassword!=null) rServerSSH = getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword);
        else if (aKeyPath!=null) rServerSSH = getKey(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath);
        else rServerSSH = get(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort);
        
        if (aJson.containsKey("CompressLevel")) rServerSSH.setCompressionLevel(((Number) aJson.get("CompressLevel")).intValue());
        if (aJson.containsKey("BeforeCommand")) rServerSSH.setBeforeSystem((String) aJson.get("BeforeCommand"));
        
        return rServerSSH;
    }
    
    /// 构造函数以及获取方式（用来区分私钥登录以及密码登录）
    private ServerSSH(String aUsername, String aHostname, int aPort) {
        mJsch = new JSch();
        try {mSession = mJsch.getSession(aUsername, aHostname, aPort);} catch (JSchException e) {throw new RuntimeException(e);}
        session().setConfig("StrictHostKeyChecking", "no");
    }
    // 不提供密码则认为是私钥登录，提供密码则认为是密码登录，可能存在歧义的情况则会有 getPassword 方法专门指明
    public static ServerSSH get        (String aUsername, String aHostname                             ) {return get        (aUsername, aHostname, 22);}
    public static ServerSSH get        (String aUsername, String aHostname, int aPort                  ) {return getKey     (aUsername, aHostname, aPort, System.getProperty("user.home")+"/.ssh/id_rsa");}
    public static ServerSSH get        (String aUsername, String aHostname, int aPort, String aPassword) {return getPassword(aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getPassword(String aUsername, String aHostname,            String aPassword) {return getPassword(aUsername, aHostname, 22, aPassword);}
    public static ServerSSH getPassword(String aUsername, String aHostname, int aPort, String aPassword) {return getPassword(null, aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getKey     (String aUsername, String aHostname,            String aKeyPath ) {return getKey     (aUsername, aHostname, 22, aKeyPath);}
    public static ServerSSH getKey     (String aUsername, String aHostname, int aPort, String aKeyPath ) {return getKey     (null, aUsername, aHostname, aPort, aKeyPath);}
    
    public static ServerSSH get        (String aRemoteWorkingDir, String aUsername, String aHostname                             ) {return get        (aRemoteWorkingDir, aUsername, aHostname, 22);}
    public static ServerSSH get        (String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) {return getKey     (aRemoteWorkingDir, aUsername, aHostname, aPort, System.getProperty("user.home")+"/.ssh/id_rsa");}
    public static ServerSSH get        (String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) {return getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getPassword(String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) {return getPassword(aRemoteWorkingDir, aUsername, aHostname, 22, aPassword);}
    public static ServerSSH getPassword(String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) {return getPassword(null, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getKey     (String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) {return getKey     (aRemoteWorkingDir, aUsername, aHostname, 22, aKeyPath);}
    public static ServerSSH getKey     (String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) {return getKey     (null, aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath);}
    
    public static ServerSSH get        (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname                             ) {return get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, 22);}
    public static ServerSSH get        (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) {return getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, System.getProperty("user.home")+"/.ssh/id_rsa");}
    public static ServerSSH get        (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) {return getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPassword);}
    public static ServerSSH get        (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) {return getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getPassword(String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) {return getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, 22, aPassword);}
    public static ServerSSH getPassword(String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) {
        ServerSSH rServerSSH = new ServerSSH(aUsername, aHostname, aPort).setLocalWorkingDir(aLocalWorkingDir).setRemoteWorkingDir(aRemoteWorkingDir);
        rServerSSH.mSession.setPassword(aPassword);
        rServerSSH.mPassword = aPassword;
        rServerSSH.mSession.setConfig("PreferredAuthentications", "password");
        try {rServerSSH.mSession.connect();} catch (JSchException e) {e.printStackTrace();}
        return rServerSSH;
    }
    public static ServerSSH getKey     (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath) {return getKey(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, 22, aKeyPath);}
    public static ServerSSH getKey     (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath) {
        ServerSSH rServerSSH = new ServerSSH(aUsername, aHostname, aPort).setLocalWorkingDir(aLocalWorkingDir).setRemoteWorkingDir(aRemoteWorkingDir);
        try {rServerSSH.mJsch.addIdentity(aKeyPath);} catch (JSchException e) {e.printStackTrace();}
        rServerSSH.mKeyPath = aKeyPath;
        rServerSSH.mSession.setConfig("PreferredAuthentications", "publickey");
        try {rServerSSH.mSession.connect();} catch (JSchException e) {e.printStackTrace();}
        return rServerSSH;
    }
    // 修改本地路径和远程路径
    public ServerSSH setLocalWorkingDir(String aLocalWorkingDir) {
        if (mDead) throw new RuntimeException("Can NOT setLocalWorkingDir from a Dead SSH.");
        if (aLocalWorkingDir == null) aLocalWorkingDir = "";
        mLocalWorkingDir_ = aLocalWorkingDir;
        aLocalWorkingDir = UT.IO.toAbsolutePath(aLocalWorkingDir);
        if (!aLocalWorkingDir.isEmpty() && !aLocalWorkingDir.endsWith("/") && !aLocalWorkingDir.endsWith("\\")) aLocalWorkingDir += "/";
        mLocalWorkingDir = aLocalWorkingDir;
        doMemberChange.run();
        return this;
    }
    public ServerSSH setRemoteWorkingDir(String aRemoteWorkingDir) {
        if (mDead) throw new RuntimeException("Can NOT setRemoteWorkingDir from a Dead SSH.");
        if (aRemoteWorkingDir == null) aRemoteWorkingDir = "";
        mRemoteWorkingDir_ = aRemoteWorkingDir;
        if (!aRemoteWorkingDir.isEmpty() && !aRemoteWorkingDir.endsWith("/") && !aRemoteWorkingDir.endsWith("\\")) aRemoteWorkingDir += "/";
        if (aRemoteWorkingDir.startsWith("~/")) aRemoteWorkingDir = aRemoteWorkingDir.substring(2); // JSch 不支持 ~
        mRemoteWorkingDir = aRemoteWorkingDir;
        doMemberChange.run();
        return this;
    }
    // 设置数据传输的压缩等级
    public ServerSSH setCompressionLevel(int aCompressionLevel) throws Exception {
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
        doMemberChange.run();
        session().rekey();
        return this;
    }
    // 设置执行 system 之前的附加指令
    public ServerSSH setBeforeSystem(String aCommand) {
        if (mDead) throw new RuntimeException("Can NOT setBeforeSystem from a Dead SSH.");
        mBeforeCommand = aCommand;
        doMemberChange.run();
        return this;
    }
    // 设置密码
    public ServerSSH setPassword(String aPassword) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT setPassword from a Dead SSH.");
        mJsch.removeAllIdentity(); // 移除旧的认证
        session().setPassword(aPassword);
        mPassword = aPassword;
        mKeyPath = null;
        session().setConfig("PreferredAuthentications", "password");
        doMemberChange.run();
        session().rekey();
        return this;
    }
    // 设置密钥路径
    public ServerSSH setKey(String aKeyPath) throws Exception {
        if (mDead) throw new RuntimeException("Can NOT setKey from a Dead SSH.");
        mJsch.removeAllIdentity(); // 移除旧的认证
        mJsch.addIdentity(aKeyPath);
        mPassword = null;
        mKeyPath = aKeyPath;
        session().setConfig("PreferredAuthentications", "publickey");
        doMemberChange.run();
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
    public void shutdown() {
        mDead = true;
        session().disconnect();
    }
    // 获取和修改 mSession，需要增加同步来保证每个线程获得的 mSession 都是合适的
    public synchronized Session session() {return mSession;}
    
    /// 实用方法
    // 提交命令
    public Task task_system(final String aCommand) {return new SerializableTask(() -> {system(aCommand); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.SYSTEM.name(), aCommand);}
    };}
    public void system(String aCommand) throws JSchException, IOException {
        if (mDead) throw new RuntimeException("Can NOT system from a Dead SSH.");
        // systemChannel 内部已经尝试了重连
        ChannelExec tChannelExec = systemChannel(aCommand);
        // 获取输入流并且输出到命令行，期间会挂起程序
        InputStream tIn = tChannelExec.getInputStream();
        tChannelExec.connect();
        BufferedReader tReader = UT.IO.toReader(tIn);
        String tLine;
        while ((tLine = tReader.readLine()) != null) System.out.println(tLine);
        // 最后关闭通道
        tChannelExec.disconnect();
    }
    // 提交命令的获取指令频道的结构，主要是内部使用，需要手动连接和关闭
    public ChannelExec systemChannel(String aCommand) throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT get systemChannel from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取执行指令的频道
        ChannelExec tChannelExec = (ChannelExec) session().openChannel("exec");
        tChannelExec.setInputStream(null);
        tChannelExec.setErrStream(System.err);
        if (mBeforeCommand != null && !mBeforeCommand.isEmpty()) aCommand = String.format("%s;%s", mBeforeCommand, aCommand);
        aCommand = String.format("cd %s;%s", mRemoteWorkingDir, aCommand); // 所有指令都会先 cd 到 mRemoteWorkingDir 再执行
        tChannelExec.setCommand(aCommand);
        return tChannelExec;
    }
    // 上传目录到服务器
    public Task task_putDir(final String aDir) {return new SerializableTask(() -> {putDir(aDir); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.PUT_DIR.name(), aDir);}
    };}
    public void putDir(String aDir) throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT putDir from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        if (aDir.equals(".")) aDir = "";
        if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
        // 递归子文件夹传输文件
        (new RecurseLocalDir(this, aDir) {
            @Override public boolean initRemoteDir(String aRemoteDir) {return makeDir_(tChannelSftp, aRemoteDir);}
            @Override public void doFile(String aLocalFile, String aRemoteDir) {try {tChannelSftp.put(aLocalFile, aRemoteDir);} catch (SftpException ignored) {}}
        }).run();
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 从服务器下载目录
    public Task task_getDir(final String aDir) {return new SerializableTask(() -> {getDir(aDir); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.GET_DIR.name(), aDir);}
    };}
    public void getDir(String aDir) throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT getDir from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        if (aDir.equals(".")) aDir = "";
        if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
        // 递归子文件夹传输文件
        (new RecurseRemoteDir(this, aDir, tChannelSftp){
            @Override public boolean initLocalDir(String aLocalDir) {return UT.IO.mkdir(aLocalDir);}
            @Override public void doFile(String aRemoteFile, String aLocalDir) {try {tChannelSftp.get(aRemoteFile, aLocalDir);} catch (SftpException ignored) {}}
        }).run();
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 清空服务器的文件夹内容，但是不删除文件夹
    public Task task_clearDir(final String aDir) {return new SerializableTask(() -> {clearDir(aDir); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.CLEAR_DIR.name(), aDir);}
    };}
    public void clearDir(String aDir) throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT clearDir from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        if (aDir.equals(".")) aDir = "";
        if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
        // 递归子文件夹删除文件
        (new RecurseRemoteDir(this, aDir, tChannelSftp, false){
            @Override public void doFile(String aRemoteFile, String aLocalDir) {try {tChannelSftp.rm(aRemoteFile);} catch (SftpException ignored) {}}
        }).run();
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 递归删除远程服务器的文件夹
    public Task task_rmdir(final String aDir) {return task_removeDir(aDir);}
    public Task task_removeDir(final String aDir) {return new SerializableTask(() -> {rmdir(aDir); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.REMOVE_DIR.name(), aDir);}
    };}
    public void rmdir(String aDir) throws JSchException {removeDir(aDir);}
    public void removeDir(String aDir) throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT rmdir from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        if (aDir.equals(".")) aDir = "";
        if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
        // 递归子文件夹来删除
        (new RecurseRemoteDir(this, aDir, tChannelSftp, false){
            @Override public void doFile(String aRemoteFile, String aLocalDir) {try {tChannelSftp.rm(aRemoteFile);} catch (SftpException ignored) {}}
            @Override public void doDirFinal(String aRemoteDir, String aLocalDir) {try {tChannelSftp.rmdir(aRemoteDir);} catch (SftpException ignored) {}}
        }).run();
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 在远程服务器创建文件夹，支持跨文件夹创建文件夹。不同于一般的 mkdir，这里如果原本的目录存在会返回 true
    public Task task_mkdir(final String aDir) {return task_makeDir(aDir);}
    public Task task_makeDir(final String aDir) {return new SerializableTask(() -> mkdir(aDir)) {
        @Override public String toString() {return String.format("%s{%s}", Type.MAKE_DIR.name(), aDir);}
    };}
    public boolean mkdir(String aDir) throws JSchException {return makeDir(aDir);}
    public boolean makeDir(String aDir) throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT mkdir from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        if (aDir.equals(".")) aDir = "";
        if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
        String tRemoteDir = mRemoteWorkingDir+aDir;
        // 创建文件夹
        boolean tSuc = makeDir_(tChannelSftp, tRemoteDir);
        // 最后关闭通道
        tChannelSftp.disconnect();
        return tSuc;
    }
    // 判断输入是否是远程服务器的文件夹
    public boolean isDir(String aDir) throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT use isDir from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        if (aDir.equals(".")) aDir = "";
        if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
        String tRemoteDir = mRemoteWorkingDir+aDir;
        // 获取结果
        boolean tOut = isDir_(tChannelSftp, tRemoteDir);
        // 最后关闭通道
        tChannelSftp.disconnect();
        return tOut;
    }
    // 上传单个文件
    public Task task_putFile(final String aFilePath) {return new SerializableTask(() -> {putFile(aFilePath); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.PUT_FILE.name(), aFilePath);}
    };}
    public void putFile(String aFilePath) throws JSchException, SftpException {
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        // 检测文件路径是否合法
        String tLocalFile = mLocalWorkingDir+aFilePath;
        if (!UT.IO.isFile(tLocalFile)) throw new RuntimeException("Invalid File Path: "+aFilePath);
        // 创建目标文件夹
        String tRemoteDir = mRemoteWorkingDir;
        int tEndIdx = aFilePath.lastIndexOf("/");
        if (tEndIdx > 0) { // 否则不用创建，认为 mRemoteWorkingDir 已经存在
            tRemoteDir += aFilePath.substring(0, tEndIdx+1);
            if (!ServerSSH.makeDir_(tChannelSftp, tRemoteDir)) throw new RuntimeException("Fail when create remote dir: " + tRemoteDir);
        }
        // 上传文件
        tChannelSftp.put(tLocalFile, tRemoteDir);
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 下载单个文件
    public Task task_getFile(final String aFilePath) {return new SerializableTask(() -> {getFile(aFilePath); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.GET_FILE.name(), aFilePath);}
    };}
    public void getFile(String aFilePath) throws JSchException, SftpException, IOException {
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        // 检测文件路径是否合法
        String tRemoteDir = mRemoteWorkingDir+aFilePath;
        if (!isFile_(tChannelSftp, tRemoteDir)) throw new RuntimeException("Invalid File Path: "+aFilePath);
        // 创建目标文件夹
        String tLocalDir = mLocalWorkingDir;
        int tEndIdx = aFilePath.lastIndexOf("/");
        if (tEndIdx > 0) { // 否则不用创建，认为 mLocalWorkingDir 已经存在
            tLocalDir += aFilePath.substring(0, tEndIdx+1);
            if (!UT.IO.mkdir(tLocalDir)) throw new RuntimeException("Fail when create local dir: "+tLocalDir);
        }
        // 下载文件
        tChannelSftp.get(tRemoteDir, tLocalDir);
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    
    // 判断输入是否是远程服务器的文件
    public boolean isFile(String aPath) throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT use isFile from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        String tRemotePath = mRemoteWorkingDir+aPath;
        // 获取结果
        boolean tOut = isFile_(tChannelSftp, tRemotePath);
        // 最后关闭通道
        tChannelSftp.disconnect();
        return tOut;
    }
    
    // 上传目录到服务器的并发版本，理论会更快
    public Task task_putDir(final String aDir, final int aThreadNumber) {return new SerializableTask(() -> {putDir(aDir, aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%s:%d}", Type.PUT_DIR_PAR.name(), aDir, aThreadNumber);}
    };}
    public void putDir(String aDir, int aThreadNumber) throws JSchException, InterruptedException {
        if (mDead) throw new RuntimeException("Can NOT putDir from a Dead SSH.");
        // 创建并发线程池，会自动尝试重新连接
        final SftpPool tSftpPool = new SftpPool(this, aThreadNumber);
        // 获取文件传输通道，还是需要一个专门的频道来串行执行创建文件夹
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        if (aDir.equals(".")) aDir = "";
        if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
        // 递归子文件夹传输文件
        (new RecurseLocalDir(this, aDir) {
            @Override public boolean initRemoteDir(String aRemoteDir) {return makeDir_(tChannelSftp, aRemoteDir);}
            @Override public void doFile(String aLocalFile, String aRemoteDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.put(aLocalFile, aRemoteDir);} catch (SftpException ignored) {}});}
        }).run();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 从服务器下载目录的并发版本，理论会更快
    public Task task_getDir(final String aDir, final int aThreadNumber) {return new SerializableTask(() -> {getDir(aDir, aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%s:%d}", Type.GET_DIR_PAR.name(), aDir, aThreadNumber);}
    };}
    public void getDir(String aDir, int aThreadNumber) throws JSchException, InterruptedException {
        if (mDead) throw new RuntimeException("Can NOT getDir from a Dead SSH.");
        // 创建并发线程池，会自动尝试重新连接
        final SftpPool tSftpPool = new SftpPool(this, aThreadNumber);
        // 获取文件传输通道，需要一个专门的频道来串行执行获取目录等操作
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        if (aDir.equals(".")) aDir = "";
        if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
        // 递归子文件夹传输文件
        (new RecurseRemoteDir(this, aDir, tChannelSftp){
            @Override public boolean initLocalDir(String aLocalDir) {return UT.IO.mkdir(aLocalDir);}
            @Override public void doFile(String aRemoteFile, String aLocalDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.get(aRemoteFile, aLocalDir);} catch (SftpException ignored) {}});}
        }).run();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 清空服务器的文件夹内容的并发版本，理论会更快
    public Task task_clearDir(final String aDir, final int aThreadNumber) {return new SerializableTask(() -> {clearDir(aDir, aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%s:%d}", Type.CLEAR_DIR_PAR.name(), aDir, aThreadNumber);}
    };}
    public void clearDir(String aDir, int aThreadNumber) throws JSchException, InterruptedException {
        if (mDead) throw new RuntimeException("Can NOT clearDir from a Dead SSH.");
        // 创建并发线程池，会自动尝试重新连接
        final SftpPool tSftpPool = new SftpPool(this, aThreadNumber);
        // 获取文件传输通道，需要一个专门的频道来串行执行获取目录等操作
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        if (aDir.equals(".")) aDir = "";
        if (!aDir.isEmpty() && !aDir.endsWith("/")) aDir += "/";
        // 递归子文件夹删除文件
        (new RecurseRemoteDir(this, aDir, tChannelSftp, false){
            @Override public void doFile(String aRemoteFile, String aLocalDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.rm(aRemoteFile);} catch (SftpException ignored) {}});}
        }).run();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 上传整个工作目录到服务器，过滤掉 '.'，'_' 开头的文件和文件夹，只提供并行版本
    public Task task_putWorkingDir() {return new SerializableTask(() -> {putWorkingDir(); return true;}) {
        @Override public String toString() {return Type.PUT_WORKING_DIR.name();}
    };}
    public Task task_putWorkingDir(final int aThreadNumber) {return new SerializableTask(() -> {putWorkingDir(aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%d}", Type.PUT_WORKING_DIR_PAR.name(), aThreadNumber);}
    };}
    public void putWorkingDir() throws JSchException, InterruptedException {putWorkingDir(4);}
    public void putWorkingDir(int aThreadNumber) throws JSchException, InterruptedException {
        if (mDead) throw new RuntimeException("Can NOT putWorkingDir from a Dead SSH.");
        // 如果本地目录是用户目录（获取工作目录失败）则禁止此操作
        if (UT.IO.samePath(mLocalWorkingDir, System.getProperty("user.home")) || mLocalWorkingDir.equals("/")) throw new RuntimeException("Can NOT putWorkingDir when LocalWorkingDir is: \""+mLocalWorkingDir+"\"");
        // 创建并发线程池，会自动尝试重新连接
        final SftpPool tSftpPool = new SftpPool(this, aThreadNumber);
        // 获取文件传输通道，还是需要一个专门的频道来串行执行创建文件夹
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        // 递归子文件夹传输文件
        (new RecurseLocalDir(this, "") {
            @Override public boolean initRemoteDir(String aRemoteDir) {return makeDir_(tChannelSftp, aRemoteDir);}
            @Override public void doFile(String aLocalFile, String aRemoteDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.put(aLocalFile, aRemoteDir);} catch (SftpException ignored) {}});}
            @Override public boolean dirFilter(String aLocalDirName) {return !aLocalDirName.startsWith(".") && !aLocalDirName.startsWith("_");}
            @Override public boolean fileFilter(String aLocalFileName) {return !aLocalFileName.startsWith(".") && !aLocalFileName.startsWith("_");}
        }).run();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 从服务器下载整个工作目录到本地，过滤掉 '.'，'_' 开头的文件和文件夹，只提供并行版本
    public Task task_getWorkingDir() {return new SerializableTask(() -> {getWorkingDir(); return true;}) {
        @Override public String toString() {return Type.GET_WORKING_DIR.name();}
    };}
    public Task task_getWorkingDir(final int aThreadNumber) {return new SerializableTask(() -> {getWorkingDir(aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%d}", Type.GET_WORKING_DIR_PAR.name(), aThreadNumber);}
    };}
    public void getWorkingDir() throws JSchException, InterruptedException {getWorkingDir(4);}
    public void getWorkingDir(int aThreadNumber) throws JSchException, InterruptedException {
        if (mDead) throw new RuntimeException("Can NOT getWorkingDir from a Dead SSH.");
        // 如果远程目录是默认值则不允许此操作
        if (mRemoteWorkingDir.isEmpty() || mRemoteWorkingDir.equals("/")) throw new RuntimeException("Can NOT getWorkingDir when RemoteWorkingDir is: \""+mRemoteWorkingDir+"\"");
        // 创建并发线程池，会自动尝试重新连接
        final SftpPool tSftpPool = new SftpPool(this, aThreadNumber);
        // 获取文件传输通道，需要一个专门的频道来串行执行获取目录等操作
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        // 递归子文件夹传输文件
        (new RecurseRemoteDir(this, "", tChannelSftp) {
            @Override public boolean initLocalDir(String aLocalDir) {return UT.IO.mkdir(aLocalDir);}
            @Override public void doFile(String aRemoteFile, String aLocalDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.get(aRemoteFile, aLocalDir);} catch (SftpException ignored) {}});}
            @Override public boolean dirFilter(String aRemoteDirName) {return !aRemoteDirName.startsWith(".") && !aRemoteDirName.startsWith("_");}
            @Override public boolean fileFilter(String aRemoteFileName) {return !aRemoteFileName.startsWith(".") && !aRemoteFileName.startsWith("_");}
        }).run();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 清空整个远程服务器的工作区，注意会删除文件夹，等价于 rmdir(".");
    public Task task_clearWorkingDir() {return new SerializableTask(() -> {clearWorkingDir(); return true;}) {
        @Override public String toString() {return Type.CLEAR_WORKING_DIR.name();}
    };}
    public Task task_clearWorkingDir(final int aThreadNumber) {return new SerializableTask(() -> {clearWorkingDir(aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%d}", Type.CLEAR_WORKING_DIR_PAR.name(), aThreadNumber);}
    };}
    public void clearWorkingDir() throws JSchException, InterruptedException {clearWorkingDir(4);}
    public void clearWorkingDir(int aThreadNumber) throws JSchException, InterruptedException {
        if (mDead) throw new RuntimeException("Can NOT clearWorkingDir from a Dead SSH.");
        // 如果远程目录是默认值则不允许此操作
        if (mRemoteWorkingDir.isEmpty() || mRemoteWorkingDir.equals("/")) throw new RuntimeException("Can NOT clearWorkingDir when RemoteWorkingDir is: \""+mRemoteWorkingDir+"\"");
        // 创建并发线程池，会自动尝试重新连接
        final SftpPool tSftpPool = new SftpPool(this, aThreadNumber);
        // 获取文件传输通道，需要一个专门的频道来串行执行获取目录等操作
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        // 需要删除的文件夹列表，由于是并发操作的，文件夹需要最后串行删除一次
        final List<String> tDirList = new ArrayList<>();
        // 递归子文件夹来删除
        (new RecurseRemoteDir(this, "", tChannelSftp, false){
            @Override public void doFile(String aRemoteFile, String aLocalDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.rm(aRemoteFile);} catch (SftpException ignored) {}});}
            @Override public void doDirFinal(String aRemoteDir, String aLocalDir) {tDirList.add(aRemoteDir);}
        }).run();
        // 先关闭 pool，等待文件全部删除完
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
        // 再遍历删除所有文件夹
        for (String tRemoteDir : tDirList) {try {tChannelSftp.rmdir(tRemoteDir);} catch (SftpException ignored) {}}
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    
    
    /// 内部方法，这里统一认为目录结尾有 '/'，且不会自动添加
    // 判断是否是文件夹，无论是什么情况报错都返回 false
    static boolean isDir_(ChannelSftp aChannelSftp, String aDir) {
        SftpATTRS tAttrs = null;
        try {tAttrs = aChannelSftp.stat(aDir);} catch (SftpException ignored) {}
        return tAttrs != null && tAttrs.isDir();
    }
    // 判断是否是文件，无论是什么情况报错都返回 false
    static boolean isFile_(ChannelSftp aChannelSftp, String aPath) {
        SftpATTRS tAttrs = null;
        try {tAttrs = aChannelSftp.stat(aPath);} catch (SftpException ignored) {}
        return tAttrs != null && !tAttrs.isDir();
    }
    // 在远程服务器创建文件夹，实现跨文件夹创建文件夹。不同于一般的 mkdir，这里如果原本的目录存在会返回 true（主要是为了编程和使用的方便）
    static boolean makeDir_(ChannelSftp aChannelSftp, String aDir) {
        if (isDir_(aChannelSftp, aDir)) return true;
        // 如果目录不存在，则需要创建目录
        boolean tSuc = true;
        int tEndIdx = aDir.lastIndexOf("/", aDir.length()-2);
        if (tEndIdx > 0) {
            String tParent = aDir.substring(0, tEndIdx+1);
            // 递归创建上级目录
            tSuc = makeDir_(aChannelSftp, tParent);
        }
        // 创建当前目录
        try {aChannelSftp.mkdir(aDir);} catch (SftpException e) {return false;}
        return tSuc;
    }
    // 内部实用类，递归的对本地文件夹进行操作，会同时记录对应的远程目录，减少重复代码
    static class RecurseLocalDir implements Runnable {
        private final ServerSSH mSSH;
        private final String mDir;
        private final boolean mCheckDirValid;
        public RecurseLocalDir(ServerSSH aSSH, String aDir) {this(aSSH, aDir, true);}
        public RecurseLocalDir(ServerSSH aSSH, String aDir, boolean aCheckDirValid) {mSSH = aSSH; mDir = aDir; mCheckDirValid = aCheckDirValid;}
        
        @Override public void run() {
            String tLocalDir = mSSH.mLocalWorkingDir + mDir;
            if (!UT.IO.isDir(tLocalDir)) {if (mCheckDirValid) throw new RuntimeException("Invalid Dir: " + mDir); return;}
            doDir(tLocalDir, mSSH.mRemoteWorkingDir + mDir);
        }
        private void doDir(String aLocalDir, String aRemoteDir) {
            String[] tLocalFiles = UT.IO.list(aLocalDir);
            if (tLocalFiles == null) return;
            if (initRemoteDir(aRemoteDir)) for (String tName : tLocalFiles) {
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                String tFileOrDir = aLocalDir+tName;
                if (UT.IO.isDir(tFileOrDir)) {if (dirFilter(tName)) doDir(tFileOrDir+"/", aRemoteDir+tName+"/");}
                else if (UT.IO.isFile(tFileOrDir)) {if (fileFilter(tName)) doFile(tFileOrDir, aRemoteDir);}
            }
            doDirFinal(aLocalDir, aRemoteDir);
        }
        
        // stuff to override
        public boolean initRemoteDir(String aRemoteDir) {return true;} // 开始遍历本地文件夹之前初始化对应的远程文件夹，返回 false 则表示此远程文件夹初始失败，不会进行后续的遍历此文件夹操作
        public void doFile(String aLocalFile, String aRemoteDir) {/**/} // 对于此本地文件夹内的文件进行操作
        public void doDirFinal(String aLocalDir, String aRemoteDir) {/**/} // 最后对此本地文件夹进行操作
        public boolean dirFilter(String aLocalDirName) {return true;} // 文件夹过滤器，返回 true 才会执行后续操作
        public boolean fileFilter(String aLocalFileName) {return true;} // 文件过滤器，返回 true 才会执行后续操作
    }
    // 内部实用类，递归的对远程文件夹进行操作，会同时记录对应的本地目录，减少重复代码。需要一个 channel 来获取远程文件夹的列表
    static class RecurseRemoteDir implements Runnable {
        private final ServerSSH mSSH;
        private final String mDir;
        private final ChannelSftp mChannelSftp;
        private final boolean mCheckDirValid;
        public RecurseRemoteDir(ServerSSH aSSH, String aDir, ChannelSftp aChannelSftp) {this(aSSH, aDir, aChannelSftp, true);}
        public RecurseRemoteDir(ServerSSH aSSH, String aDir, ChannelSftp aChannelSftp, boolean aCheckDirValid) {mSSH = aSSH; mDir = aDir; mChannelSftp = aChannelSftp; mCheckDirValid = aCheckDirValid;}
    
        @Override public void run() {
            String tRemoteDir = mSSH.mRemoteWorkingDir+mDir;
            if (!isDir_(mChannelSftp, tRemoteDir)) {if (mCheckDirValid) throw new RuntimeException("Invalid Dir: " + mDir); return;}
            doDir(tRemoteDir, mSSH.mLocalWorkingDir+mDir);
        }
        @SuppressWarnings("unchecked")
        private void doDir(String aRemoteDir, String aLocalDir) {
            Vector<ChannelSftp.LsEntry> tRemoteFiles = null;
            try {tRemoteFiles = mChannelSftp.ls(aRemoteDir);} catch (SftpException ignored) {}
            if (tRemoteFiles == null) return;
            if (initLocalDir(aLocalDir)) for (ChannelSftp.LsEntry tFile : tRemoteFiles) {
                String tName = tFile.getFilename();
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                if (tFile.getAttrs().isDir()) {if (dirFilter(tName)) doDir(aRemoteDir+tName+"/", aLocalDir+tName+"/");}
                else {if (fileFilter(tName)) doFile(aRemoteDir+tName, aLocalDir);}
            }
            doDirFinal(aRemoteDir, aLocalDir);
        }
        
        // stuff to override
        public boolean initLocalDir(String aLocalDir) {return true;} // 开始遍历远程文件夹之前初始化对应的本地文件夹，返回 false 则表示此本地文件夹初始失败，不会进行后续的遍历此文件夹操作
        public void doFile(String aRemoteFile, String aLocalDir) {/**/} // 对于此远程文件夹内的文件进行操作
        public void doDirFinal(String aRemoteDir, String aLocalDir) {/**/} // 最后对此远程文件夹进行操作
        public boolean dirFilter(String aRemoteDirName) {return true;} // 文件夹过滤器，返回 true 才会执行后续操作
        public boolean fileFilter(String aRemoteFileName) {return true;} // 文件过滤器，返回 true 才会执行后续操作
    }
    
    /// 并发部分
    // 类似线程池的 Sftp 通道，可以重写实现提交任务并且并发的上传和下载
    static class SftpPool {
        interface ISftpTask {void doTask(ChannelSftp aChannelSftp);}
        private final LinkedList<ISftpTask> mTaskList = new LinkedList<>();
        private final ExecutorService mPool;
        private boolean mDead = false;
        
        SftpPool(ServerSSH aSSH, int aThreadNumber) throws JSchException {
            // 会尝试一次重新连接
            if (!aSSH.isConnecting()) aSSH.connect();
            // 初始化线程池
            mPool = Executors.newFixedThreadPool(aThreadNumber);
            // 提交长期任务
            for (int i = 0; i < aThreadNumber; ++i) {
                final ChannelSftp tChannelSftp = (ChannelSftp) aSSH.session().openChannel("sftp");
                mPool.execute(() -> {
                    try {tChannelSftp.connect();} catch (JSchException e) {tChannelSftp.disconnect(); throw new RuntimeException(e);}
                    // 每个 Sftp 都从 mTaskList 中竞争获取 task 并执行
                    while (true) {
                        ISftpTask tTask;
                        synchronized (mTaskList) {tTask = mTaskList.pollFirst();}
                        if (tTask != null) tTask.doTask(tChannelSftp);
                        else {
                            if (mDead) break;
                            // 否则继续等待任务输入
                            try {Thread.sleep(50);} catch (InterruptedException e) {e.printStackTrace(); break;}
                        }
                    }
                    // 最后关闭通道
                    tChannelSftp.disconnect();
                });
            }
        }
        
        void shutdown() {mDead = true; mPool.shutdown();}
        boolean awaitTermination() throws InterruptedException {return mPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);}
        void submit(ISftpTask aSftpTask) {
            if (mDead) throw new RuntimeException("Can NOT submit tasks to a Dead SftpPool.");
            synchronized (mTaskList) {mTaskList.addLast(aSftpTask);}
        }
    }
    // 由于一个 channel 只能执行一个指令，这里直接使用线程池来实现 system 的并发，接口和 SystemThreadPool 保持一致
    public SystemPool pool(int aThreadNumber) {if (mDead) throw new RuntimeException("Can NOT get pool from a Dead SSH."); return new SystemPool(aThreadNumber);}
    class SystemPool {
        private final ThreadPoolExecutor mPool;
        
        public SystemPool(int aThreadNumber) {
            mPool = new ThreadPoolExecutor(aThreadNumber, aThreadNumber, 0, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>());
        }
        public Future<?> submitSystem(String aCommand) {
            return mPool.submit(() -> {try {system(aCommand);} catch (JSchException | IOException e) {throw new RuntimeException(e);}});
        }
        public void waitUntilDone() throws InterruptedException {
            while (mPool.getActiveCount() > 0 || mPool.getQueue().size() > 0) Thread.sleep(200);
        }
        public int getTaskNumber() {
            return mPool.getActiveCount() + mPool.getQueue().size();
        }
        
        public void shutdown() {mPool.shutdown();}
        public List<Runnable> shutdownNow() {return mPool.shutdownNow();}
    }
    
    // 手动加载 UT，会自动重新设置工作目录，会在调用静态函数 get 或者 load 时自动加载保证路径的正确性
    static {UT.IO.init();}
}
