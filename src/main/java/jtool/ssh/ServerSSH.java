package jtool.ssh;

import jtool.parallel.IAutoShutdown;
import jtool.code.UT;
import jtool.parallel.ExecutorsEX;
import jtool.parallel.IExecutorEX;
import jtool.system.SSHSystemExecutor;
import com.jcraft.jsch.*;
import groovy.json.JsonBuilder;
import groovy.json.JsonSlurper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.Callable;

import static jtool.code.CS.FILE_SYSTEM_SLEEP_TIME;


/**
 * @author liqa
 * <p> 使用 ssh 连接到服务器 </p>
 * <p> 创建时自动连接服务器，自动跳过初次登录的 "yes/no" 询问 </p>
 * <p> 提供提交指令，断开自动重连，同步目标文件夹等功能 </p>
 * <p> 由于免密登录只支持经典的 openssh 密钥（即需要生成时加上 -m pem），因此还提供密码登录的支持，
 * 但依旧不建议使用密码登录，因为会在代码中出现明文密码 </p>
 * <p>
 * <p> 不建议直接使用，现在对此基本停止维护，请改为使用更加成熟的 {@link SSHSystemExecutor} </p>
 */
@Deprecated
@SuppressWarnings({"UnusedReturnValue", "BusyWait"})
public final class ServerSSH implements IAutoShutdown {
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
    
    /// hooks, 修改这个来实现重写，我也不知道这个方法是不是合理
    // 发生内部参数改变都需要调用一下这个函数
    Runnable doMemberChange = () -> {};
    
    /// 保存到文件以及从文件加载
    @SuppressWarnings("rawtypes")
    public void save(String aFilePath) throws IOException {
        Map rJson = new LinkedHashMap();
        save(rJson);
        UT.IO.map2json(rJson, aFilePath);
    }
    @SuppressWarnings("rawtypes")
    public static ServerSSH load(String aFilePath) throws Exception {
        Map tJson = UT.IO.json2map(aFilePath);
        return load(tJson);
    }
    // 带有密码的读写
    @SuppressWarnings("rawtypes")
    public void save(String aFilePath, String aKey) throws Exception {
        Map rJson = new LinkedHashMap();
        save(rJson);
        jtool.io.Encryptor tEncryptor = new jtool.io.Encryptor(aKey);
        UT.IO.write(aFilePath, tEncryptor.getData((new JsonBuilder(rJson)).toString()));
    }
    @SuppressWarnings("rawtypes")
    public static ServerSSH load(String aFilePath, String aKey) throws Exception {
        jtool.io.Decryptor tDecryptor = new jtool.io.Decryptor(aKey);
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
        String aUsername = (String) UT.Code.get(aJson, "Username", "username", "user", "u");
        String aHostname = (String) UT.Code.get(aJson, "Hostname", "hostname", "host", "h");
        int aPort = ((Number) UT.Code.getWithDefault(aJson, 22, "Port", "port", "p")).intValue();
        
        String aLocalWorkingDir  = (String) UT.Code.get(aJson, "LocalWorkingDir", "localworkingdir", "lwd");
        String aRemoteWorkingDir = (String) UT.Code.get(aJson, "RemoteWorkingDir", "remoteworkingdir", "rwd", "wd");
        String aPassword         = (String) UT.Code.get(aJson, "Password", "password", "pw");
        String aKeyPath          = (String) UT.Code.get(aJson, "KeyPath", "keypath", "key", "k");
        
        
        ServerSSH rServerSSH = null;
        try {
            if (aPassword!=null) rServerSSH = getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword);
            else if (aKeyPath!=null) rServerSSH = getKey(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath);
            else rServerSSH = get(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort);
            
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
    
    /// 构造函数以及获取方式（用来区分私钥登录以及密码登录）
    private ServerSSH(String aUsername, String aHostname, int aPort) throws JSchException {
        mJsch = new JSch();
        mSession = mJsch.getSession(aUsername, aHostname, aPort);
        session().setConfig("StrictHostKeyChecking", "no");
    }
    // 不提供密码则认为是私钥登录，提供密码则认为是密码登录，可能存在歧义的情况则会有 getPassword 方法专门指明
    public static ServerSSH get        (String aUsername, String aHostname                             ) throws JSchException {return get        (aUsername, aHostname, 22);}
    public static ServerSSH get        (String aUsername, String aHostname, int aPort                  ) throws JSchException {return getKey     (aUsername, aHostname, aPort, System.getProperty("user.home")+"/.ssh/id_rsa");}
    public static ServerSSH get        (String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {return getPassword(aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getPassword(String aUsername, String aHostname,            String aPassword) throws JSchException {return getPassword(aUsername, aHostname, 22, aPassword);}
    public static ServerSSH getPassword(String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {return getPassword(null, aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getKey     (String aUsername, String aHostname,            String aKeyPath ) throws JSchException {return getKey     (aUsername, aHostname, 22, aKeyPath);}
    public static ServerSSH getKey     (String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {return getKey     (null, aUsername, aHostname, aPort, aKeyPath);}
    
    public static ServerSSH get        (String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {return get        (aRemoteWorkingDir, aUsername, aHostname, 22);}
    public static ServerSSH get        (String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {return getKey     (aRemoteWorkingDir, aUsername, aHostname, aPort, System.getProperty("user.home")+"/.ssh/id_rsa");}
    public static ServerSSH get        (String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {return getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getPassword(String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {return getPassword(aRemoteWorkingDir, aUsername, aHostname, 22, aPassword);}
    public static ServerSSH getPassword(String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {return getPassword(null, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getKey     (String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {return getKey     (aRemoteWorkingDir, aUsername, aHostname, 22, aKeyPath);}
    public static ServerSSH getKey     (String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {return getKey     (null, aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath);}
    
    public static ServerSSH get        (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {return get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, 22);}
    public static ServerSSH get        (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {return getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, System.getProperty("user.home")+"/.ssh/id_rsa");}
    public static ServerSSH get        (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {return getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPassword);}
    public static ServerSSH get        (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {return getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword);}
    public static ServerSSH getPassword(String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {return getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, 22, aPassword);}
    public static ServerSSH getPassword(String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {
        ServerSSH rServerSSH = new ServerSSH(aUsername, aHostname, aPort).setLocalWorkingDir(aLocalWorkingDir).setRemoteWorkingDir(aRemoteWorkingDir);
        rServerSSH.mSession.setPassword(aPassword);
        rServerSSH.mPassword = aPassword;
        rServerSSH.mSession.setConfig("PreferredAuthentications", "password");
        try {rServerSSH.mSession.connect();} catch (JSchException e) {e.printStackTrace(System.err);}
        return rServerSSH;
    }
    public static ServerSSH getKey     (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath) throws JSchException {return getKey(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, 22, aKeyPath);}
    public static ServerSSH getKey     (String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath) throws JSchException {
        ServerSSH rServerSSH = new ServerSSH(aUsername, aHostname, aPort).setLocalWorkingDir(aLocalWorkingDir).setRemoteWorkingDir(aRemoteWorkingDir);
        try {rServerSSH.mJsch.addIdentity(aKeyPath);} catch (JSchException e) {e.printStackTrace(System.err);}
        rServerSSH.mKeyPath = aKeyPath;
        rServerSSH.mSession.setConfig("PreferredAuthentications", "publickey");
        try {rServerSSH.mSession.connect();} catch (JSchException e) {e.printStackTrace(System.err);}
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
    @Override public void shutdown() {
        mDead = true;
        session().disconnect();
    }
    // 获取和修改 mSession，需要增加同步来保证每个线程获得的 mSession 都是合适的
    public synchronized Session session() {return mSession;}
    
    /// 实用方法
    // 提交命令
    public jtool.code.task.Task task_system(final String aCommand) {return new jtool.code.task.SerializableTask(() -> {system(aCommand); return true;}) {
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
    public ChannelExec systemChannel(String aCommand) throws JSchException {return systemChannel(aCommand, false);}
    public ChannelExec systemChannel(String aCommand, boolean aNoERROutput) throws JSchException {
        if (mDead) throw new RuntimeException("Can NOT get systemChannel from a Dead SSH.");
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取执行指令的频道
        ChannelExec tChannelExec = (ChannelExec) session().openChannel("exec");
        tChannelExec.setInputStream(null);
        if (!aNoERROutput) tChannelExec.setErrStream(System.err, true); // 注意一定要设置不要关闭，啊，意外关闭其实是不好检测的啊
        if (mBeforeCommand != null && !mBeforeCommand.isEmpty()) aCommand = String.format("%s;%s", mBeforeCommand, aCommand);
        aCommand = String.format("cd \"%s\";%s", mRemoteWorkingDir, aCommand); // 所有指令都会先 cd 到 mRemoteWorkingDir 再执行
        tChannelExec.setCommand(aCommand);
        return tChannelExec;
    }
    // 上传目录到服务器
    public jtool.code.task.Task task_putDir(final String aDir) {return new jtool.code.task.SerializableTask(() -> {putDir(aDir); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.PUT_DIR.name(), aDir);}
    };}
    public void putDir(String aDir) throws JSchException, IOException, SftpException {
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
            @Override public void initRemoteDir(String aRemoteDir) throws SftpException {makeDir_(tChannelSftp, aRemoteDir);}
            @Override public void doFile(String aLocalFile, String aRemoteDir) {try {tChannelSftp.put(aLocalFile, aRemoteDir);} catch (SftpException ignored) {}}
        }).call();
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 从服务器下载目录
    public jtool.code.task.Task task_getDir(final String aDir) {return new jtool.code.task.SerializableTask(() -> {getDir(aDir); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.GET_DIR.name(), aDir);}
    };}
    public void getDir(String aDir) throws JSchException, IOException {
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
            @Override public void initLocalDir(String aLocalDir) throws IOException {UT.IO.makeDir(aLocalDir);}
            @Override public void doFile(String aRemoteFile, String aLocalDir) {try {tChannelSftp.get(aRemoteFile, aLocalDir);} catch (SftpException ignored) {}}
        }).call();
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 清空服务器的文件夹内容，但是不删除文件夹
    public jtool.code.task.Task task_clearDir(final String aDir) {return new jtool.code.task.SerializableTask(() -> {clearDir(aDir); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.CLEAR_DIR.name(), aDir);}
    };}
    public void clearDir(String aDir) throws JSchException, IOException {
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
        }).call();
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 递归删除远程服务器的文件夹
    public jtool.code.task.Task task_rmdir(final String aDir) {return task_removeDir(aDir);}
    public jtool.code.task.Task task_removeDir(final String aDir) {return new jtool.code.task.SerializableTask(() -> {rmdir(aDir); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.REMOVE_DIR.name(), aDir);}
    };}
    public void rmdir(String aDir) throws JSchException, IOException {removeDir(aDir);}
    public void removeDir(String aDir) throws JSchException, IOException {
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
        }).call();
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 在远程服务器创建文件夹，支持跨文件夹创建文件夹。不同于一般的 mkdir，这里如果原本的目录存在会返回 true
    public jtool.code.task.Task task_mkdir(final String aDir) {return task_makeDir(aDir);}
    public jtool.code.task.Task task_makeDir(final String aDir) {return new jtool.code.task.SerializableTask(() -> {mkdir(aDir); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.MAKE_DIR.name(), aDir);}
    };}
    public void mkdir(String aDir) throws JSchException, SftpException {makeDir(aDir);}
    public void makeDir(String aDir) throws JSchException, SftpException {
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
        makeDir_(tChannelSftp, tRemoteDir);
        // 最后关闭通道
        tChannelSftp.disconnect();
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
    public jtool.code.task.Task task_putFile(final String aFilePath) {return new jtool.code.task.SerializableTask(() -> {putFile(aFilePath); return true;}) {
        @Override public String toString() {return String.format("%s{%s}", Type.PUT_FILE.name(), aFilePath);}
    };}
    public void putFile(String aFilePath) throws JSchException, SftpException, IOException {
        // 会尝试一次重新连接
        if (!isConnecting()) connect();
        // 获取文件传输通道
        ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        // 检测文件路径是否合法
        String tLocalFile = mLocalWorkingDir+aFilePath;
        if (!UT.IO.isFile(tLocalFile)) throw new IOException("Invalid File Path: "+aFilePath);
        // 创建目标文件夹
        String tRemoteDir = mRemoteWorkingDir;
        int tEndIdx = aFilePath.lastIndexOf("/");
        if (tEndIdx > 0) { // 否则不用创建，认为 mRemoteWorkingDir 已经存在
            tRemoteDir += aFilePath.substring(0, tEndIdx+1);
            ServerSSH.makeDir_(tChannelSftp, tRemoteDir);
        }
        // 上传文件
        tChannelSftp.put(tLocalFile, tRemoteDir);
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    // 下载单个文件
    public jtool.code.task.Task task_getFile(final String aFilePath) {return new jtool.code.task.SerializableTask(() -> {getFile(aFilePath); return true;}) {
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
        if (!isFile_(tChannelSftp, tRemoteDir)) throw new IOException("Invalid File Path: "+aFilePath);
        // 创建目标文件夹
        String tLocalDir = mLocalWorkingDir;
        int tEndIdx = aFilePath.lastIndexOf("/");
        if (tEndIdx > 0) { // 否则不用创建，认为 mLocalWorkingDir 已经存在
            tLocalDir += aFilePath.substring(0, tEndIdx+1);
            UT.IO.makeDir(tLocalDir);
        }
        // 下载文件
        tChannelSftp.get(tRemoteDir, tLocalDir);
        // 最后关闭通道
        tChannelSftp.disconnect();
    }
    
    
    /**
     * 上传下载多个文件，主要使用的 ssh 方法，一次传输多个文件不用重新连接，创建通道等。
     * 由于是重新写的，会更加注意错误出现后的通道自动关闭，原本没有在源码中使用的方法不再维护
     * @author liqa
     */
    public void putFiles(Iterable<String> aFilePaths) throws JSchException, SftpException, IOException {
        ChannelSftp tChannelSftp = null;
        try {
            // 会尝试一次重新连接
            if (!isConnecting()) connect();
            // 获取文件传输通道
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            for (String tFilePath : aFilePaths) if (tFilePath!=null && !tFilePath.isEmpty()) {
                // 检测文件路径是否合法
                String tLocalFile = mLocalWorkingDir+tFilePath;
                if (!UT.IO.isFile(tLocalFile)) throw new IOException("Invalid File Path: "+tFilePath);
                // 创建目标文件夹
                String tRemoteDir = mRemoteWorkingDir;
                int tEndIdx = tFilePath.lastIndexOf("/");
                if (tEndIdx > 0) { // 否则不用创建，认为 mRemoteWorkingDir 已经存在
                    tRemoteDir += tFilePath.substring(0, tEndIdx+1);
                    ServerSSH.makeDir_(tChannelSftp, tRemoteDir);
                }
                // 上传文件
                tChannelSftp.put(tLocalFile, tRemoteDir);
            }
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    public void getFiles(Iterable<String> aFilePaths) throws JSchException, SftpException, IOException {
        ChannelSftp tChannelSftp = null;
        try {
            // 会尝试一次重新连接
            if (!isConnecting()) connect();
            // 获取文件传输通道
            tChannelSftp = (ChannelSftp) session().openChannel("sftp");
            tChannelSftp.connect();
            for (String tFilePath : aFilePaths) if (tFilePath!=null && !tFilePath.isEmpty()) {
                // 检测文件路径是否合法
                String tRemoteDir = mRemoteWorkingDir + tFilePath;
                if (!isFile_(tChannelSftp, tRemoteDir)) throw new IOException("Invalid File Path: " + tFilePath);
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
        } finally {
            // 最后关闭通道
            if (tChannelSftp != null) tChannelSftp.disconnect();
        }
    }
    /** 并行版本 */
    public void putFiles(Iterable<String> aFilePaths, int aThreadNumber) throws JSchException, InterruptedException {
        SftpPool tSftpPool = null;
        try {
            // 创建并发线程池，会自动尝试重新连接
            tSftpPool = new SftpPool(this, aThreadNumber);
            for (final String tFilePath : aFilePaths) if (tFilePath!=null && !tFilePath.isEmpty()) {
                tSftpPool.submit(aChannelSftp -> {
                    // 检测文件路径是否合法
                    String tLocalFile = mLocalWorkingDir+tFilePath;
                    if (!UT.IO.isFile(tLocalFile)) throw new IOException("Invalid File Path: "+tFilePath);
                    // 创建目标文件夹
                    String tRemoteDir = mRemoteWorkingDir;
                    int tEndIdx = tFilePath.lastIndexOf("/");
                    if (tEndIdx > 0) { // 否则不用创建，认为 mRemoteWorkingDir 已经存在
                        tRemoteDir += tFilePath.substring(0, tEndIdx+1);
                        ServerSSH.makeDir_(aChannelSftp, tRemoteDir);
                    }
                    // 上传文件
                    aChannelSftp.put(tLocalFile, tRemoteDir);
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
    public void getFiles(Iterable<String> aFilePaths, int aThreadNumber) throws JSchException, InterruptedException {
        SftpPool tSftpPool = null;
        try {
            // 创建并发线程池，会自动尝试重新连接
            tSftpPool = new SftpPool(this, aThreadNumber);
            for (final String tFilePath : aFilePaths) if (tFilePath!=null && !tFilePath.isEmpty()) {
                tSftpPool.submit(aChannelSftp -> {
                    // 检测文件路径是否合法
                    String tRemoteDir = mRemoteWorkingDir + tFilePath;
                    if (!isFile_(aChannelSftp, tRemoteDir)) throw new IOException("Invalid File Path: " + tFilePath);
                    // 创建目标文件夹
                    String tLocalDir = mLocalWorkingDir;
                    int tEndIdx = tFilePath.lastIndexOf("/");
                    if (tEndIdx > 0) { // 否则不用创建，认为 mLocalWorkingDir 已经存在
                        tLocalDir += tFilePath.substring(0, tEndIdx + 1);
                        UT.IO.makeDir(tLocalDir);
                    }
                    // 下载文件
                    aChannelSftp.get(tRemoteDir, tLocalDir);
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
    public jtool.code.task.Task task_putDir(final String aDir, final int aThreadNumber) {return new jtool.code.task.SerializableTask(() -> {putDir(aDir, aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%s:%d}", Type.PUT_DIR_PAR.name(), aDir, aThreadNumber);}
    };}
    public void putDir(String aDir, int aThreadNumber) throws JSchException, InterruptedException, IOException, SftpException {
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
            @Override public void initRemoteDir(String aRemoteDir) throws SftpException {makeDir_(tChannelSftp, aRemoteDir);}
            @Override public void doFile(String aLocalFile, String aRemoteDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.put(aLocalFile, aRemoteDir);} catch (SftpException ignored) {}});}
        }).call();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 从服务器下载目录的并发版本，理论会更快
    public jtool.code.task.Task task_getDir(final String aDir, final int aThreadNumber) {return new jtool.code.task.SerializableTask(() -> {getDir(aDir, aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%s:%d}", Type.GET_DIR_PAR.name(), aDir, aThreadNumber);}
    };}
    public void getDir(String aDir, int aThreadNumber) throws JSchException, InterruptedException, IOException {
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
            @Override public void initLocalDir(String aLocalDir) throws IOException {UT.IO.makeDir(aLocalDir);}
            @Override public void doFile(String aRemoteFile, String aLocalDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.get(aRemoteFile, aLocalDir);} catch (SftpException ignored) {}});}
        }).call();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 清空服务器的文件夹内容的并发版本，理论会更快
    public jtool.code.task.Task task_clearDir(final String aDir, final int aThreadNumber) {return new jtool.code.task.SerializableTask(() -> {clearDir(aDir, aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%s:%d}", Type.CLEAR_DIR_PAR.name(), aDir, aThreadNumber);}
    };}
    public void clearDir(String aDir, int aThreadNumber) throws JSchException, InterruptedException, IOException {
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
        }).call();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 上传整个工作目录到服务器，过滤掉 '.'，'_' 开头的文件和文件夹，只提供并行版本
    public jtool.code.task.Task task_putWorkingDir() {return new jtool.code.task.SerializableTask(() -> {putWorkingDir(); return true;}) {
        @Override public String toString() {return Type.PUT_WORKING_DIR.name();}
    };}
    public jtool.code.task.Task task_putWorkingDir(final int aThreadNumber) {return new jtool.code.task.SerializableTask(() -> {putWorkingDir(aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%d}", Type.PUT_WORKING_DIR_PAR.name(), aThreadNumber);}
    };}
    public void putWorkingDir() throws JSchException, InterruptedException, IOException, SftpException {putWorkingDir(4);}
    public void putWorkingDir(int aThreadNumber) throws JSchException, InterruptedException, IOException, SftpException {
        if (mDead) throw new RuntimeException("Can NOT putWorkingDir from a Dead SSH.");
        // 如果本地目录是用户目录（获取工作目录失败）则禁止此操作
        if (UT.IO.samePath(mLocalWorkingDir, System.getProperty("user.home")) || mLocalWorkingDir.equals("/")) throw new IOException("Can NOT putWorkingDir when LocalWorkingDir is: \""+mLocalWorkingDir+"\"");
        // 创建并发线程池，会自动尝试重新连接
        final SftpPool tSftpPool = new SftpPool(this, aThreadNumber);
        // 获取文件传输通道，还是需要一个专门的频道来串行执行创建文件夹
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        // 递归子文件夹传输文件
        (new RecurseLocalDir(this, "") {
            @Override public void initRemoteDir(String aRemoteDir) throws SftpException {makeDir_(tChannelSftp, aRemoteDir);}
            @Override public void doFile(String aLocalFile, String aRemoteDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.put(aLocalFile, aRemoteDir);} catch (SftpException ignored) {}});}
            @Override public boolean dirFilter(String aLocalDirName) {return !aLocalDirName.startsWith(".") && !aLocalDirName.startsWith("_");}
            @Override public boolean fileFilter(String aLocalFileName) {return !aLocalFileName.startsWith(".") && !aLocalFileName.startsWith("_");}
        }).call();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 从服务器下载整个工作目录到本地，过滤掉 '.'，'_' 开头的文件和文件夹，只提供并行版本
    public jtool.code.task.Task task_getWorkingDir() {return new jtool.code.task.SerializableTask(() -> {getWorkingDir(); return true;}) {
        @Override public String toString() {return Type.GET_WORKING_DIR.name();}
    };}
    public jtool.code.task.Task task_getWorkingDir(final int aThreadNumber) {return new jtool.code.task.SerializableTask(() -> {getWorkingDir(aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%d}", Type.GET_WORKING_DIR_PAR.name(), aThreadNumber);}
    };}
    public void getWorkingDir() throws JSchException, InterruptedException, IOException {getWorkingDir(4);}
    public void getWorkingDir(int aThreadNumber) throws JSchException, InterruptedException, IOException {
        if (mDead) throw new RuntimeException("Can NOT getWorkingDir from a Dead SSH.");
        // 如果远程目录是默认值则不允许此操作
        if (mRemoteWorkingDir.isEmpty() || mRemoteWorkingDir.equals("/")) throw new IOException("Can NOT getWorkingDir when RemoteWorkingDir is: \""+mRemoteWorkingDir+"\"");
        // 创建并发线程池，会自动尝试重新连接
        final SftpPool tSftpPool = new SftpPool(this, aThreadNumber);
        // 获取文件传输通道，需要一个专门的频道来串行执行获取目录等操作
        final ChannelSftp tChannelSftp = (ChannelSftp) session().openChannel("sftp");
        tChannelSftp.connect();
        // 递归子文件夹传输文件
        (new RecurseRemoteDir(this, "", tChannelSftp) {
            @Override public void initLocalDir(String aLocalDir) throws IOException {UT.IO.makeDir(aLocalDir);}
            @Override public void doFile(String aRemoteFile, String aLocalDir) {tSftpPool.submit(aChannelSftp -> {try {aChannelSftp.get(aRemoteFile, aLocalDir);} catch (SftpException ignored) {}});}
            @Override public boolean dirFilter(String aRemoteDirName) {return !aRemoteDirName.startsWith(".") && !aRemoteDirName.startsWith("_");}
            @Override public boolean fileFilter(String aRemoteFileName) {return !aRemoteFileName.startsWith(".") && !aRemoteFileName.startsWith("_");}
        }).call();
        // 最后关闭通道
        tChannelSftp.disconnect();
        tSftpPool.shutdown();
        tSftpPool.awaitTermination();
    }
    // 清空整个远程服务器的工作区，注意会删除文件夹，等价于 rmdir(".");
    public jtool.code.task.Task task_clearWorkingDir() {return new jtool.code.task.SerializableTask(() -> {clearWorkingDir(); return true;}) {
        @Override public String toString() {return Type.CLEAR_WORKING_DIR.name();}
    };}
    public jtool.code.task.Task task_clearWorkingDir(final int aThreadNumber) {return new jtool.code.task.SerializableTask(() -> {clearWorkingDir(aThreadNumber); return true;}) {
        @Override public String toString() {return String.format("%s{%d}", Type.CLEAR_WORKING_DIR_PAR.name(), aThreadNumber);}
    };}
    public void clearWorkingDir() throws JSchException, InterruptedException, IOException {clearWorkingDir(4);}
    public void clearWorkingDir(int aThreadNumber) throws JSchException, InterruptedException, IOException {
        if (mDead) throw new RuntimeException("Can NOT clearWorkingDir from a Dead SSH.");
        // 如果远程目录是默认值则不允许此操作
        if (mRemoteWorkingDir.isEmpty() || mRemoteWorkingDir.equals("/")) throw new IOException("Can NOT clearWorkingDir when RemoteWorkingDir is: \""+mRemoteWorkingDir+"\"");
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
        }).call();
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
    static void makeDir_(ChannelSftp aChannelSftp, String aDir) throws SftpException {
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
    // 内部实用类，递归的对本地文件夹进行操作，会同时记录对应的远程目录，减少重复代码
    static class RecurseLocalDir implements Callable<Void> {
        private final ServerSSH mSSH;
        private final String mDir;
        private final boolean mCheckDirValid;
        public RecurseLocalDir(ServerSSH aSSH, String aDir) {this(aSSH, aDir, true);}
        public RecurseLocalDir(ServerSSH aSSH, String aDir, boolean aCheckDirValid) {mSSH = aSSH; mDir = aDir; mCheckDirValid = aCheckDirValid;}
        
        @Override public Void call() throws IOException, SftpException {
            String tLocalDir = mSSH.mLocalWorkingDir + mDir;
            if (!UT.IO.isDir(tLocalDir)) {if (mCheckDirValid) throw new IOException("Invalid Dir: " + mDir); return null;}
            doDir(tLocalDir, mSSH.mRemoteWorkingDir + mDir);
            return null;
        }
        private void doDir(String aLocalDir, String aRemoteDir) throws IOException, SftpException {
            String[] tLocalFiles = UT.IO.list(aLocalDir);
            initRemoteDir(aRemoteDir);
            for (String tName : tLocalFiles) {
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                String tFileOrDir = aLocalDir+tName;
                if (UT.IO.isDir(tFileOrDir)) {if (dirFilter(tName)) doDir(tFileOrDir+"/", aRemoteDir+tName+"/");}
                else if (UT.IO.isFile(tFileOrDir)) {if (fileFilter(tName)) doFile(tFileOrDir, aRemoteDir);}
            }
            doDirFinal(aLocalDir, aRemoteDir);
        }
        
        // stuff to override
        public void initRemoteDir(String aRemoteDir) throws SftpException {/**/} // 开始遍历本地文件夹之前初始化对应的远程文件夹，返回 false 则表示此远程文件夹初始失败，不会进行后续的遍历此文件夹操作
        public void doFile(String aLocalFile, String aRemoteDir) {/**/} // 对于此本地文件夹内的文件进行操作
        public void doDirFinal(String aLocalDir, String aRemoteDir) {/**/} // 最后对此本地文件夹进行操作
        public boolean dirFilter(String aLocalDirName) {return true;} // 文件夹过滤器，返回 true 才会执行后续操作
        public boolean fileFilter(String aLocalFileName) {return true;} // 文件过滤器，返回 true 才会执行后续操作
    }
    // 内部实用类，递归的对远程文件夹进行操作，会同时记录对应的本地目录，减少重复代码。需要一个 channel 来获取远程文件夹的列表
    static class RecurseRemoteDir implements Callable<Void>  {
        private final ServerSSH mSSH;
        private final String mDir;
        private final ChannelSftp mChannelSftp;
        private final boolean mCheckDirValid;
        public RecurseRemoteDir(ServerSSH aSSH, String aDir, ChannelSftp aChannelSftp) {this(aSSH, aDir, aChannelSftp, true);}
        public RecurseRemoteDir(ServerSSH aSSH, String aDir, ChannelSftp aChannelSftp, boolean aCheckDirValid) {mSSH = aSSH; mDir = aDir; mChannelSftp = aChannelSftp; mCheckDirValid = aCheckDirValid;}
    
        @Override public Void call() throws IOException {
            String tRemoteDir = mSSH.mRemoteWorkingDir+mDir;
            if (!isDir_(mChannelSftp, tRemoteDir)) {if (mCheckDirValid) throw new IOException("Invalid Dir: " + mDir); return null;}
            doDir(tRemoteDir, mSSH.mLocalWorkingDir+mDir);
            return null;
        }
        @SuppressWarnings("unchecked")
        private void doDir(String aRemoteDir, String aLocalDir) throws IOException {
            Vector<ChannelSftp.LsEntry> tRemoteFiles = null;
            try {tRemoteFiles = mChannelSftp.ls(aRemoteDir);} catch (SftpException ignored) {}
            if (tRemoteFiles == null) return;
            initLocalDir(aLocalDir);
            for (ChannelSftp.LsEntry tFile : tRemoteFiles) {
                String tName = tFile.getFilename();
                if (tName==null || tName.isEmpty() || tName.equals(".") || tName.equals("..")) continue;
                if (tFile.getAttrs().isDir()) {if (dirFilter(tName)) doDir(aRemoteDir+tName+"/", aLocalDir+tName+"/");}
                else {if (fileFilter(tName)) doFile(aRemoteDir+tName, aLocalDir);}
            }
            doDirFinal(aRemoteDir, aLocalDir);
        }
        
        // stuff to override
        public void initLocalDir(String aLocalDir) throws IOException {/**/} // 开始遍历远程文件夹之前初始化对应的本地文件夹，返回 false 则表示此本地文件夹初始失败，不会进行后续的遍历此文件夹操作
        public void doFile(String aRemoteFile, String aLocalDir) {/**/} // 对于此远程文件夹内的文件进行操作
        public void doDirFinal(String aRemoteDir, String aLocalDir) {/**/} // 最后对此远程文件夹进行操作
        public boolean dirFilter(String aRemoteDirName) {return true;} // 文件夹过滤器，返回 true 才会执行后续操作
        public boolean fileFilter(String aRemoteFileName) {return true;} // 文件过滤器，返回 true 才会执行后续操作
    }
    
    /// 并发部分
    // 类似线程池的 Sftp 通道，可以重写实现提交任务并且并发的上传和下载
    static class SftpPool {
        interface ISftpTask {void doTask(ChannelSftp aChannelSftp) throws Exception;}
        private final Deque<ISftpTask> mTaskQueue = new ArrayDeque<>();
        private volatile boolean mDead = false;
        private final IExecutorEX mPool;
        
        SftpPool(ServerSSH aSSH, int aThreadNumber) throws JSchException {
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
                                synchronized (mTaskQueue) {tTask = mTaskQueue.pollFirst();}
                                if (tTask != null) {
                                    tTask.doTask(tChannelSftp);
                                } else {
                                    if (mDead) break;
                                    // 否则继续等待任务输入
                                    Thread.sleep(FILE_SYSTEM_SLEEP_TIME);
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace(System.err);
                        } finally {
                            // 最后关闭通道
                            tChannelSftp.disconnect();
                        }
                    });
                }
            } catch (JSchException e) {
                this.shutdown();
                throw e;
            }
        }
        
        public void shutdown() {mDead = true; mPool.shutdown();}
        public void awaitTermination() throws InterruptedException {mPool.awaitTermination();}
        
        void submit(ISftpTask aSftpTask) {
            if (mDead) throw new RuntimeException("Can NOT submit tasks to a Dead SftpPool.");
            synchronized (mTaskQueue) {mTaskQueue.addLast(aSftpTask);}
        }
    }
    
    // 手动加载 UT，会自动重新设置工作目录，会在调用静态函数 get 或者 load 时自动加载保证路径的正确性
    static {UT.IO.init();}
}
