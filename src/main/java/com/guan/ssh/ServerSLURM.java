package com.guan.ssh;

import com.guan.code.Pair;
import com.guan.code.Task;
import com.guan.code.UT;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import org.jetbrains.annotations.ApiStatus;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author liqa
 * <p> 对于 SLURM 系统的服务器提供专门的接口方便任务提交 </p>
 * <p> 与 SSH 不同，可以把本身当作一个 SystemThreadPool </p>
 * <p> 相比 SSH 多一个 mMaxJobNumber 参数限制同时运行的任务数 </p>
 */
@ApiStatus.Obsolete
@SuppressWarnings({"UnusedReturnValue", "BusyWait"})
public final class ServerSLURM {
    static final int DEFAULT_TOLERANT = 3;
    
    private final ServerSSH mSSH;
    private final int mMaxJobNumber;
    private final int mMaxThisJobNumber; // 可能会存在共用的现象，可以单限制此对象同时运行的任务数目
    private final String mSqueueName; // 有些 SLURM 系统 squeue 的用户名和登录用户名不同
    
    private final Map<Integer, Pair<Task, Integer>> mJobIDList = new LinkedHashMap<>(); // <JobID, <afterTask, tolerant>>
    private final LinkedList<Pair<Pair<Task, Task>, String>> mCommandList = new LinkedList<>(); // <<beforeTask, afterTask>, command>
    private final ExecutorService mPool;
    private volatile boolean mDead = false;
    private volatile boolean mPause = false; // 可以暂停任务的提交
    private volatile boolean mKilled = false; // 直接强制杀死提交进程
    
    private long mSleepTime = 500; // ms 设置更高的值可以降低检测的频率
    // 各种提交任务的尝试次数类
    private final TolerantCounter mTolerantCounter = new TolerantCounter();
    // 保存提交的任务名称，不一定和真实的类名匹配（如果是 load 得到的）
    private String mJobName = "JOB-FROM-"+this;
    // 本地的镜像存储地址，会在任何修改后保存到此镜像
    private String mMirrorPath_ = null;
    private String mMirrorPath = null; // 由于 matlab 下运行时绝对路径获取会出现问题，虽然已经内部实用已经没有问题，但是为了避免第三方库的问题这里本地目录统一实用绝对路径
    // 镜像的加密密码
    private String mMirrorKey = null;
    // 远程服务器的镜像存储地址，用于检测是否有多个对象同时管理一个镜像
    private String mRemoteMirrorPath = null;
    
    /// hooks, 修改这个来实现重写，我也不知道这个方法是不是合理
    // 发生内部参数改变都需要调用一下这个函数
    @SuppressWarnings("CanBeFinal")
    Runnable doMemberChange = this::saveToMirror_; // 这样可以使得任何修改都会实时存储，可以处理意外杀死的情况
    
    /// 保存到文件以及从文件加载
    public void save(String aFilePath) throws Exception {save(aFilePath, null);}
    @SuppressWarnings("rawtypes")
    public static ServerSLURM load(String aFilePath) throws Exception {
        Map tJson = UT.IO.json2map(aFilePath);
        return load(tJson);
    }
    // 带有密码的读写
    @SuppressWarnings("rawtypes")
    public void save(String aFilePath, String aKey) throws Exception {
        // 保存到 json
        Map rJson = new LinkedHashMap();
        save(rJson);
        // slurm 由于有自动保存的功能，需要先备份旧的文件
        if (UT.IO.isFile(aFilePath)) UT.IO.copy(aFilePath, aFilePath+".bak");
        // 开始写入
        if (aKey != null && !aKey.isEmpty()) {
            UT.IO.map2json(rJson, aFilePath, aKey);
        } else {
            UT.IO.map2json(rJson, aFilePath);
        }
        // 写入完成删除备份文件
        if (UT.IO.isFile(aFilePath)) UT.IO.delete(aFilePath+".bak");
    }
    @SuppressWarnings("rawtypes")
    public static ServerSLURM load(String aFilePath, String aKey) throws Exception {
        Map tJson = UT.IO.json2map(aFilePath, aKey);
        return load(tJson);
    }
    // 偏向于内部使用的保存到 json 和从 json 读取
    @SuppressWarnings({"unchecked", "rawtypes"})
    public synchronized void save(Map rJson) {
        // synchronized 修饰防止保存过程中出现了修改
        // 保存需要暂停，防止重复提交
        pause();
        // 先保存 ssh
        Map rJsonSSH = new LinkedHashMap();
        rJson.put("SSH", rJsonSSH);
        mSSH.save(rJsonSSH);
        // 再保存 slurm
        Map rJsonSLURM = new LinkedHashMap();
        rJson.put("SLURM", rJsonSLURM);
        rJsonSLURM.put("MaxJobNumber", mMaxJobNumber);
        rJsonSLURM.put("SleepTime", mSleepTime);
        rJsonSLURM.put("JobName", mJobName);
        rJsonSLURM.put("Tolerant", mTolerantCounter.mTolerant);
        
        if (mMaxThisJobNumber < mMaxJobNumber)
            rJsonSLURM.put("MaxThisJobNumber", mMaxThisJobNumber);
        if (!mSqueueName.equals(mSSH.session().getUserName()))
            rJsonSLURM.put("SqueueName", mSqueueName);
        if (mMirrorPath_ != null)
            rJsonSLURM.put("MirrorPath", mMirrorPath_);
        if (mMirrorKey != null)
            rJsonSLURM.put("MirrorKey", mMirrorKey);
        if (mRemoteMirrorPath != null && !mRemoteMirrorPath.equals(".temp/mirror/"+mJobName))
            rJsonSLURM.put("RemoteMirrorPath", mRemoteMirrorPath);
        
        if (!mJobIDList.isEmpty()) {
            List rJsonJobIDList = new ArrayList();
            rJsonSLURM.put("JobIDList", rJsonJobIDList);
            // 按照 id，task 的顺序排列
            for (Map.Entry<Integer, Pair<Task, Integer>> tEntry : mJobIDList.entrySet()) {
                rJsonJobIDList.add(tEntry.getKey());
                Task tTask = tEntry.getValue().first;
                rJsonJobIDList.add((tTask instanceof SerializableTask)?tTask.toString():SerializableTask.Type.NULL.name());
            }
        }
        
        if (!mCommandList.isEmpty()) {
            List rJsonCommandList = new ArrayList();
            rJsonSLURM.put("CommandList", rJsonCommandList);
            // 按照 command, beforeTask, afterTask 的顺序排列
            for (Pair<Pair<Task, Task>, String> tPair : mCommandList) {
                rJsonCommandList.add(tPair.second);
                Task tBeforeTask = tPair.first.first;
                Task tAfterTask = tPair.first.second;
                rJsonCommandList.add((tBeforeTask instanceof SerializableTask)?tBeforeTask.toString():SerializableTask.Type.NULL.name());
                rJsonCommandList.add((tAfterTask  instanceof SerializableTask)?tAfterTask.toString() :SerializableTask.Type.NULL.name());
            }
        }
        
        // save 操作不自动解除暂停以防止重复提交
    }
    @SuppressWarnings("rawtypes")
    public static ServerSLURM load(Map aJson) throws Exception {
        // 先加载 ssh
        ServerSSH aSSH = ServerSSH.load((Map) aJson.get("SSH"));
        // 再加载 slurm
        Map tJsonSLURM = (Map) aJson.get("SLURM");
        int aMaxJobNumber = ((Number) tJsonSLURM.get("MaxJobNumber")).intValue();
        long aSleepTime = ((Number) tJsonSLURM.get("SleepTime")).longValue();
        String aJobName = (String) tJsonSLURM.get("JobName");
        int aTolerant = ((Number) tJsonSLURM.get("Tolerant")).intValue();
        String aSqueueName = tJsonSLURM.containsKey("SqueueName") ? (String) tJsonSLURM.get("SqueueName") : aSSH.session().getUserName();
        
        ServerSLURM rServerSLURM;
        if (tJsonSLURM.containsKey("MaxThisJobNumber")) rServerSLURM = new ServerSLURM(aSSH, aMaxJobNumber, ((Number) tJsonSLURM.get("MaxThisJobNumber")).intValue(), aSqueueName);
        else rServerSLURM = new ServerSLURM(aSSH, aMaxJobNumber, aSqueueName);
        // 获取后先暂停防止加载过程中发生了提交
        rServerSLURM.pause();
        
        rServerSLURM.setSleepTime(aSleepTime).setTolerant(aTolerant);
        rServerSLURM.mJobName = aJobName;
        
        // 获取任务队列
        if (tJsonSLURM.containsKey("JobIDList")) {
            List tJsonJobIDList = (List) tJsonSLURM.get("JobIDList");
            for (int i = 1; i < tJsonJobIDList.size(); i+=2)
                rServerSLURM.mJobIDList.put(((Number) tJsonJobIDList.get(i-1)).intValue(), new Pair<>(SerializableTask.fromString(rServerSLURM, (String) tJsonJobIDList.get(i)), DEFAULT_TOLERANT));
        }
        // 获取排队队列
        if (tJsonSLURM.containsKey("CommandList")) {
            List tJsonCommandList = (List) tJsonSLURM.get("CommandList");
            for (int i = 2; i < tJsonCommandList.size(); i+=3)
                rServerSLURM.mCommandList.add(new Pair<>(new Pair<>(SerializableTask.fromString(rServerSLURM, (String) tJsonCommandList.get(i-1)), SerializableTask.fromString(rServerSLURM, (String) tJsonCommandList.get(i))), (String) tJsonCommandList.get(i-2)));
        }
        // 最后加载 MirrorPath，会自动进行存储一次
        if (tJsonSLURM.containsKey("MirrorPath")) {
            String tMirrorKey = tJsonSLURM.containsKey("MirrorKey") ? (String) tJsonSLURM.get("MirrorKey") : null;
            if (!tJsonSLURM.containsKey("RemoteMirrorPath")) rServerSLURM.setMirror((String) tJsonSLURM.get("MirrorPath"), tMirrorKey);
            else rServerSLURM.setMirror((String) tJsonSLURM.get("MirrorPath"), tMirrorKey, (String) tJsonSLURM.get("RemoteMirrorPath"));
        }
        
        // 加载完成解除暂停
        rServerSLURM.unpause();
        return rServerSLURM;
    }
    
    private final Thread mHook; // ShutdownHook
    
    /// 构造函数以及获取方式（用来区分私钥登录以及密码登录）
    private ServerSLURM(ServerSSH aSSH, int aMaxJobNumber, String aSqueueName) {this(aSSH, aMaxJobNumber, aMaxJobNumber, aSqueueName);}
    private ServerSLURM(ServerSSH aSSH, int aMaxJobNumber, int aMaxThisJobNumber, String aSqueueName) {
        mSSH = aSSH;
        mSSH.doMemberChange = doMemberChange; // 重写 mSSH 的 doMemberChange
        mMaxJobNumber = aMaxJobNumber;
        mMaxThisJobNumber = aMaxThisJobNumber;
        mSqueueName = aSqueueName;
        // 初始化线程池
        mPool = Executors.newSingleThreadExecutor();
        // 提交长期任务，不断从 mCmdList 获取指令并执行
        mPool.execute(this::keepSubmitFromList_);
        // 在 JVM 意外关闭时手动执行杀死 kill
        mHook = new Thread(() -> kill(false));
        Runtime.getRuntime().addShutdownHook(mHook);
    }
    // 不提供密码则认为是私钥登录，提供密码则认为是密码登录，可能存在歧义的情况则会有 getPassword 方法专门指明
    public static ServerSLURM get        (int aMaxJobNumber, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    
    
    public static ServerSLURM get        (int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aRemoteWorkingDir, aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aRemoteWorkingDir, aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aRemoteWorkingDir, aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aRemoteWorkingDir, aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aRemoteWorkingDir, aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aRemoteWorkingDir, aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aRemoteWorkingDir, aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aRemoteWorkingDir, aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aRemoteWorkingDir, aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aRemoteWorkingDir, aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aRemoteWorkingDir, aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aRemoteWorkingDir, aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    
    
    public static ServerSLURM get        (int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, tSSH.session().getUserName());}
    
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aSqueueName);}
    
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM get        (int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getPassword(int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    public static ServerSLURM getKey     (int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, tSSH.session().getUserName());}
    
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname                             ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname                  ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort                  ) throws JSchException {ServerSSH tSSH = ServerSSH.get        (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort           ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM get        (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getPassword(String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aPassword) throws JSchException {ServerSSH tSSH = ServerSSH.getPassword(aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aPassword); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname,            String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname,        aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    public static ServerSLURM getKey     (String aSqueueName, int aMaxJobNumber, int aMaxThisJobNumber, String aLocalWorkingDir, String aRemoteWorkingDir, String aUsername, String aHostname, int aPort, String aKeyPath ) throws JSchException {ServerSSH tSSH = ServerSSH.getKey     (aLocalWorkingDir, aRemoteWorkingDir, aUsername, aHostname, aPort, aKeyPath ); return new ServerSLURM(tSSH, aMaxJobNumber, aMaxThisJobNumber, aSqueueName);}
    /// 提供通用的接口，这里直接返回内部的 SSH 省去重复的转发部分
    public ServerSSH ssh() {
        if (mDead) throw new RuntimeException("Can NOT get SSH from a Dead SLURM.");
        return mSSH;
    }
    private void shutdown_() {
        mDead = true;
        mPool.shutdown();
        Runtime.getRuntime().removeShutdownHook(mHook);
    }
    public void shutdown() {shutdown_();}
    public void shutdownNow() throws JSchException, IOException {
        cancelThis();
        shutdown_();
    }
    // 设置暂停，会挂起直到获得这个对象的锁，这样在外部调用后确实已经暂停，而在内部使用时不容易出现死锁
    public synchronized void pause() {mPause = true;}
    public void unpause() {mPause = false;}
    // 直接杀死这个对象，类似于系统层面的杀死进程，会直接关闭提交任务并且放弃监管远程服务器的任务而不是取消这些任务，从而使得 mirror 的内容冻结
    public void kill() {kill(true);}
    public void kill(boolean aWarning) {
        // 会先暂停保证正在进行的任务已经完成提交，保证镜像文件不会被这个对象再次修改
        pause();
        // 直接设置 mKilled 即可
        if (aWarning && mMirrorPath == null) System.out.println("WARNING: you killed a slurm without mirror, jobs submit from this may out of control!");
        mKilled = true;
        shutdown_();
    }
    // 一些参数设置
    public ServerSLURM setSleepTime(long aSleepTime) {
        if (mDead) throw new RuntimeException("Can NOT setSleepTime from a Dead SLURM.");
        mSleepTime = aSleepTime;
        doMemberChange.run();
        return this;
    }
    public ServerSLURM setTolerant(int aTolerant) {
        if (mDead) throw new RuntimeException("Can NOT setTolerant from a Dead SLURM.");
        mTolerantCounter.mTolerant = aTolerant;
        mTolerantCounter.mUsedTolerant = Math.min(mTolerantCounter.mUsedTolerant, mTolerantCounter.mTolerant);
        doMemberChange.run(); return this;
    }
    public ServerSLURM setMirrorRemote(String aPath, String aRemotePath) {return setMirror(aPath, null, aRemotePath);}
    public ServerSLURM setMirror(String aPath) {return setMirror(aPath, null);}
    public ServerSLURM setMirror(String aPath, String aKey) {return setMirror(aPath, aKey, ".temp/mirror/"+mJobName);}
    public ServerSLURM setMirror(String aPath, String aKey, String aRemotePath) {
        if (mDead) throw new RuntimeException("Can NOT setMirror from a Dead SLURM.");
        if (aPath.isEmpty()) {mMirrorPath_ = null; mMirrorPath = null; mRemoteMirrorPath = null; mMirrorKey = null; return this;}
        // 存储参数
        mMirrorPath_ = aPath;
        mMirrorPath = UT.IO.toAbsolutePath(aPath);
        mMirrorKey = aKey;
        if (aRemotePath.startsWith("~/")) aRemotePath = aRemotePath.substring(2); // JSch 不支持 ~
        mRemoteMirrorPath = aRemotePath;
        // 记录原本的暂停状态
        boolean oPause = mPause;
        // 尝试存储镜像文件
        try {
            save(aPath, aKey);
            // 需要先让远程镜像所在的路径合法
            Task tTask = task_validPath_(mRemoteMirrorPath);
            if (tTask != null && !tTask.call()) throw new RuntimeException();
            // 直接使用 echo 指令在远程服务器上创建镜像文件
            mSSH.system(String.format("echo -e '%s\\nEND' > %s", this, mRemoteMirrorPath)); // 最后一行存 end 避免读取一半的情况
        } catch (Exception e) {
            System.out.println("WARNING: set MirrorPath to "+aPath+" Fail, MirrorPath set to null.");
            mMirrorPath_ = null; mMirrorPath = null; mRemoteMirrorPath = null; mMirrorKey = null;
        } finally {mPause = oPause;} // 还原暂停状态（因为 save 会改变暂停状态，目前无论如何 save 都会完全暂停）
        return this;
    }
    
    /// 内部实用类
    // 内部的从队列中提交任务
    void keepSubmitFromList_() {
        while (true) {
            // 如果被杀死则直接结束（优先级最高）
            if (mKilled) break;
            // 由于检测任务是否完成也需要发送指令，简单起见这里直接限制提交频率为 0.5s 一次（默认）
            try {Thread.sleep(mSleepTime);} catch (InterruptedException e) {e.printStackTrace(); break;}
            // 如果已经暂停则直接跳过
            if (mPause) continue;
            // 开始提交任务相关事项，现在统一使用一个 this 锁来简化逻辑
            synchronized(this) {
                // 如果已经暂停则直接跳过，并行特有的两次检测
                if (mPause) continue;
                // 如果没有指令需要提交，并且没有正在执行的任务则需要考虑关闭线程
                if (mCommandList.isEmpty() && mJobIDList.isEmpty()) {if (mDead) break; else continue;}
                // 这里统一检查一次联机状态，如果重新连接失败直接跳过重试
                if (!mSSH.isConnecting()) try {mSSH.connect();} catch (JSchException e) {continue;}
                // 首先检测镜像是否合理
                if (mMirrorPath != null) {
                    final boolean[] tMirrorValid = {true}; boolean tCheckSuc = true;
                    try {tMirrorValid[0] = checkMirror_();} catch (Exception e) {tCheckSuc = false;}
                    mTolerantCounter.call(tCheckSuc, "check mirror: " + mRemoteMirrorPath, () -> tMirrorValid[0] = false);
                    // 首先判断镜像非法，如果非法则直接执行非法并跳过后续（如果在容忍次数内会认为镜像合法）
                    if (!tMirrorValid[0]) {doMirrorInvalid_(); continue;} // 现在在内部 kill 不会死锁了
                    // 如果检测失败也会跳过后续（一般是网络会有问题）
                    if (!tCheckSuc) continue;
                }
                // 获取正在执行的任务队列
                Set<Integer> tJobIDs;
                try {tJobIDs = jobIDs_();} catch (JSchException | IOException e) {continue;} // 获取失败则直接跳过重试
                // 更新正在执行的任务列表
                if (!mJobIDList.isEmpty()) {
                    // 将不存在 JobIDs 中的计数减一，因为可能因为网络问题导致 jobIDs_ 获取的结果不一定正确
                    for (Map.Entry<Integer, Pair<Task, Integer>> tEntry : mJobIDList.entrySet()) {
                        if (!tJobIDs.contains(tEntry.getKey())) --(tEntry.getValue().second);
                        else tEntry.getValue().second = DEFAULT_TOLERANT;
                    }
                    // 将计数小于 1 的移除
                    final Iterator<Pair<Task, Integer>> tIt = mJobIDList.values().iterator();
                    final boolean[] tAlive = {true};
                    while (tAlive[0] && tIt.hasNext()) {
                        Pair<Task, Integer> tPair = tIt.next();
                        if (tPair.second < 0) {
                            // 移除前先执行完成后的 task
                            Task tAfterSystem = tPair.first;
                            if (tAfterSystem != null) {
                                boolean tSuc;
                                try {tSuc = tAfterSystem.call();} catch (Exception e) {tSuc = false;}
                                mTolerantCounter.call(tSuc, "running after task: "+tAfterSystem, () -> {tIt.remove(); doMemberChange.run();}, () -> tAlive[0] = false, () -> {tIt.remove(); doMemberChange.run();});
                            } else {
                                tIt.remove(); doMemberChange.run();
                            }
                        }
                    }
                    // 如果期间发生了不成功的现象，则 tAlive 为 false，不再进行后续操作并重试
                    if (!tAlive[0]) continue;
                }
                // 准备提交任务，如果没有任务则跳过
                if (mCommandList.isEmpty()) continue;
                // 如果正在执行的任务列表超过限制，则不会提交
                if (tJobIDs.size() >= mMaxJobNumber) continue;
                // 如果此对象正在执行的任务超过限制，则不会提交
                if (mJobIDList.size() >= mMaxThisJobNumber) continue;
                // 获取和提交任务
                Pair<Pair<Task, Task>, String> tPair = mCommandList.peekFirst();
                if (tPair == null) continue;
                final Pair<Task, Task> tTasks = tPair.first;
                final String tCommand = tPair.second;
                if (tTasks.first != null) {
                    boolean tSuc;
                    try {tSuc = tTasks.first.call();} catch (Exception e) {tSuc = false;}
                    mTolerantCounter.call(tSuc, "running before task: "+tTasks.first, () -> {mCommandList.removeFirst(); doMemberChange.run();}, () -> {}, () -> {tTasks.first = null; doMemberChange.run();});
                    if (!tSuc) continue; // 只要不成功都需要跳过后续并重试
                }
                // 获取执行命令的通道
                ChannelExec tChannelExec = null;
                try {tChannelExec = mSSH.systemChannel(tCommand);} catch (JSchException ignored) {}
                mTolerantCounter.call(tChannelExec != null, "get ChannelExec: "+tCommand, () -> {mCommandList.removeFirst(); doMemberChange.run();});
                if (tChannelExec == null) continue; // 只要不成功都需要跳过后续并重试
                // 提交命令并且获取任务号
                int tJobID = getJobIDFromChannel_(tChannelExec);
                mTolerantCounter.call(tJobID > 0, "get JobID("+tJobID+"): "+tCommand, () -> {mCommandList.removeFirst(); doMemberChange.run();});
                if (tJobID <= 0) continue; // 只要不成功都需要跳过后续并重试
                // 成功获取，移出 mCommandList 并添加到 mJobIDList
                mCommandList.removeFirst();
                mJobIDList.put(tJobID, new Pair<>(tTasks.second, DEFAULT_TOLERANT));
                doMemberChange.run();
            }
        }
        // 最后关闭 SSH 通道
        mSSH.shutdown();
    }
    
    // 从 aChannelExec 中获取任务号，返回小于零的值表示获取失败。会在内部开启通道来获得输出，因此获取完成后会直接关闭通道
    static int getJobIDFromChannel_(ChannelExec aChannelExec) {
        InputStream tIn;
        try {tIn = aChannelExec.getInputStream();} catch (IOException e) {return -1;}
        // 开启通道获取输出
        try {aChannelExec.connect();} catch (JSchException e) {return -2;}
        BufferedReader tReader = new BufferedReader(new InputStreamReader(tIn));
        String tLine;
        try {tLine = tReader.readLine();} catch (IOException e) {return -3;} // 只需要读取一行
        try {tReader.close();} catch (IOException e) {return -4;}
        // 会在内部关闭通道
        aChannelExec.disconnect();
        // 返回任务号
        if (tLine != null && tLine.startsWith("Submitted batch job ")) return Integer.parseInt(tLine.substring(20));
        return -5;
    }
    // 获取创建输出文件所需要创建文件夹的 task，这里只用于减少重复代码
    Task task_validPath_(final String aPath) {
        int tEndIdx = aPath.lastIndexOf("/");
        if (tEndIdx > 0) { // 否则不用创建，认为 mRemoteWorkingDir 已经存在
            final String tDir = aPath.substring(0, tEndIdx+1);
            return mSSH.task_mkdir(tDir);
        }
        return null;
    }
    // 内部使用的保存到镜像的方法
    void saveToMirror_() {
        if (mMirrorPath == null) return;
        boolean oPause = mPause; // 记录原本的暂停状态
        try {save(mMirrorPath, mMirrorKey);} catch (Exception ignored) {}
        finally {mPause = oPause;} // 还原暂停状态（因为 save 会改变暂停状态，目前无论如何 save 都会完全暂停）
    }
    // 内部使用的检测镜像是否合法的方法，成功检测到失败会返回 false，由于各种原因无法确定检测结果则抛出错误
    boolean checkMirror_() throws JSchException, IOException, RuntimeException {
        // 直接通过在远程服务器上执行 cat 指令来读取镜像
        if (mMirrorPath == null || mRemoteMirrorPath == null) return true;
        // systemChannel 内部已经尝试了重连
        ChannelExec tChannelExec = mSSH.systemChannel(String.format("cat '%s'", mRemoteMirrorPath));
        // 获取输出得到对应的镜像 owner
        InputStream tIn = tChannelExec.getInputStream();
        tChannelExec.connect();
        BufferedReader tReader = new BufferedReader(new InputStreamReader(tIn));
        LinkedList<String> tLines = new LinkedList<>();
        String tLine;
        while ((tLine = tReader.readLine()) != null) tLines.addLast(tLine);
        // 读取完成关闭通道
        tChannelExec.disconnect();
        // 如果最后一行不是 END 说明输出流被打断或者不是标准镜像文件，不能确定是否合理，抛出异常
        while (!tLines.isEmpty()) {
            tLine = tLines.pollLast();
            if (tLine.equals("END")) break;
        }
        if (tLine == null) throw new RuntimeException("Invalid RemoteMirrorPath format");
        tLine = tLines.pollLast();
        return tLine != null && tLine.equals(this.toString());
    }
    // 内部使用的，当镜像非法时的处理措施，kill 自身并且输出警告
    void doMirrorInvalid_() {
        System.out.printf("WARNING: The mirror of this instance(%s) is invalid,\n", this);
        System.out.println("  so this instance has been killed, which may caused by the redundant instance of the mirror.");
        System.out.println("  All of the instance of this mirror will be killed, so you may get repeated messages.");
        System.out.println("  You need to kill the old instance before load the mirror.");
        System.out.flush();
        // 尝试设置远程的镜像非法，所有的冗余实例都会被杀死并收到杀死信息
        try {mSSH.system(String.format("echo -e 'INVALID\\nEND' > %s", mRemoteMirrorPath));} catch (JSchException | IOException ignored) {}
        kill();
    }
    
    /**
     * 通用的提交任务接口，底层使用 sbatch 来提交任务
     * 只需要指定需要运行的目标分区以及总的需要的节点数目
     * 一般来说需要在指令中自己使用 srun 来开始并行任务
     * 可以指定输出目录
     * 可以指定指令开始之前的 task 以及指令执行完成后的 task
     */
    public void submitSystem(String aCommand                                                        ) {submitSystem(aCommand, null);}
    public void submitSystem(String aCommand,                    int aNodeNumber                    ) {submitSystem(aCommand, null, aNodeNumber);}
    public void submitSystem(String aCommand,                    int aNodeNumber, String aOutputPath) {submitSystem(aCommand, null, aNodeNumber, aOutputPath);}
    public void submitSystem(String aCommand, String aPartition                                     ) {submitSystem(aCommand, aPartition, 1);}
    public void submitSystem(String aCommand, String aPartition, int aNodeNumber                    ) {submitSystem(aCommand, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    public void submitSystem(String aCommand, String aPartition, int aNodeNumber, String aOutputPath) {submitSystem(null, aCommand, aPartition, aNodeNumber, aOutputPath);}
    
    public void submitSystem(Task aBeforeSystem, String aCommand                                                        ) {submitSystem(aBeforeSystem, aCommand, null);}
    public void submitSystem(Task aBeforeSystem, String aCommand,                    int aNodeNumber                    ) {submitSystem(aBeforeSystem, aCommand, null, aNodeNumber);}
    public void submitSystem(Task aBeforeSystem, String aCommand,                    int aNodeNumber, String aOutputPath) {submitSystem(aBeforeSystem, aCommand, null, aNodeNumber, aOutputPath);}
    public void submitSystem(Task aBeforeSystem, String aCommand, String aPartition                                     ) {submitSystem(aBeforeSystem, aCommand, aPartition, 1);}
    public void submitSystem(Task aBeforeSystem, String aCommand, String aPartition, int aNodeNumber                    ) {submitSystem(aBeforeSystem, aCommand, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    public void submitSystem(Task aBeforeSystem, String aCommand, String aPartition, int aNodeNumber, String aOutputPath) {submitSystem(aBeforeSystem, null, aCommand, aPartition, aNodeNumber, aOutputPath);}
    
    public void submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand                                                        ) {submitSystem(aBeforeSystem, aAfterSystem, aCommand, null);}
    public void submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aNodeNumber                    ) {submitSystem(aBeforeSystem, aAfterSystem, aCommand, null, aNodeNumber);}
    public void submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aNodeNumber, String aOutputPath) {submitSystem(aBeforeSystem, aAfterSystem, aCommand, null, aNodeNumber, aOutputPath);}
    public void submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition                                     ) {submitSystem(aBeforeSystem, aAfterSystem, aCommand, aPartition, 1);}
    public void submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aNodeNumber                    ) {submitSystem(aBeforeSystem, aAfterSystem, aCommand, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    
    
    public Task task_submitSystem(String aCommand                                                        ) {return task_submitSystem(aCommand, null);}
    public Task task_submitSystem(String aCommand,                    int aNodeNumber                    ) {return task_submitSystem(aCommand, null, aNodeNumber);}
    public Task task_submitSystem(String aCommand,                    int aNodeNumber, String aOutputPath) {return task_submitSystem(aCommand, null, aNodeNumber, aOutputPath);}
    public Task task_submitSystem(String aCommand, String aPartition                                     ) {return task_submitSystem(aCommand, aPartition, 1);}
    public Task task_submitSystem(String aCommand, String aPartition, int aNodeNumber                    ) {return task_submitSystem(aCommand, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    public Task task_submitSystem(String aCommand, String aPartition, int aNodeNumber, String aOutputPath) {return task_submitSystem(null, aCommand, aPartition, aNodeNumber, aOutputPath);}
    
    public Task task_submitSystem(Task aBeforeSystem, String aCommand                                                        ) {return task_submitSystem(aBeforeSystem, aCommand, null);}
    public Task task_submitSystem(Task aBeforeSystem, String aCommand,                    int aNodeNumber                    ) {return task_submitSystem(aBeforeSystem, aCommand, null, aNodeNumber);}
    public Task task_submitSystem(Task aBeforeSystem, String aCommand,                    int aNodeNumber, String aOutputPath) {return task_submitSystem(aBeforeSystem, aCommand, null, aNodeNumber, aOutputPath);}
    public Task task_submitSystem(Task aBeforeSystem, String aCommand, String aPartition                                     ) {return task_submitSystem(aBeforeSystem, aCommand, aPartition, 1);}
    public Task task_submitSystem(Task aBeforeSystem, String aCommand, String aPartition, int aNodeNumber                    ) {return task_submitSystem(aBeforeSystem, aCommand, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    public Task task_submitSystem(Task aBeforeSystem, String aCommand, String aPartition, int aNodeNumber, String aOutputPath) {return task_submitSystem(aBeforeSystem, null, aCommand, aPartition, aNodeNumber, aOutputPath);}
    
    public Task task_submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand                                                        ) {return task_submitSystem(aBeforeSystem, aAfterSystem, aCommand, null);}
    public Task task_submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aNodeNumber                    ) {return task_submitSystem(aBeforeSystem, aAfterSystem, aCommand, null, aNodeNumber);}
    public Task task_submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aNodeNumber, String aOutputPath) {return task_submitSystem(aBeforeSystem, aAfterSystem, aCommand, null, aNodeNumber, aOutputPath);}
    public Task task_submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition                                     ) {return task_submitSystem(aBeforeSystem, aAfterSystem, aCommand, aPartition, 1);}
    public Task task_submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aNodeNumber                    ) {return task_submitSystem(aBeforeSystem, aAfterSystem, aCommand, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    
    
    public Task task_submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aNodeNumber, String aOutputPath) {
        return new SerializableTask(() -> {submitSystem(aBeforeSystem, aAfterSystem, aCommand, aPartition, aNodeNumber, aOutputPath); return true;}) {
            @Override public String toString() {return String.format("%s{%s:%s:%s:%s:%d:%s}", Type.SLURM_SUBMIT_SYSTEM.name(), aBeforeSystem==null?Type.NULL.name():aBeforeSystem.toString(), aAfterSystem==null?Type.NULL.name():aAfterSystem.toString(), aCommand, aPartition, aNodeNumber, aOutputPath);}
    };}
    public synchronized void submitSystem(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aNodeNumber, String aOutputPath) {
        if (mDead) throw new RuntimeException("Can NOT submitSbatch from a Dead SLURM.");
        aNodeNumber = Math.max(1, aNodeNumber);
        // 需要创建输出目录的文件夹
        aBeforeSystem = UT.Tasks.mergeTask(aBeforeSystem, task_validPath_(aOutputPath));
        // 组装指令
        aCommand = String.format("echo -e '#!/bin/bash\\n%s' | sbatch --nodes %d --output %s --job-name %s", aCommand, aNodeNumber, aOutputPath, mJobName);
        if (aPartition != null && !aPartition.isEmpty()) aCommand += String.format(" --partition %s", aPartition);
        // 添加指令到队列
        mCommandList.addLast(new Pair<>(new Pair<>(aBeforeSystem, aAfterSystem), aCommand));
        doMemberChange.run();
    }
    
    /**
     * 直接使用 sbatch 执行脚本的接口
     * 可以指定需要运行的目标分区以及总的需要的节点数目
     * 可以不指定需要的节点数目而在脚本中指定
     * 输入本地的脚本路径，首先会将其上传到服务器对应位置
     */
    public void submitBash(String aBashPath                                                        ) {submitBash(aBashPath, null);}
    public void submitBash(String aBashPath,                    int aNodeNumber                    ) {submitBash(aBashPath, null, aNodeNumber);}
    public void submitBash(String aBashPath,                    int aNodeNumber, String aOutputPath) {submitBash(aBashPath, null, aNodeNumber, aOutputPath);}
    public void submitBash(String aBashPath, String aPartition                                     ) {submitBash(aBashPath, aPartition, -1);}
    public void submitBash(String aBashPath, String aPartition, int aNodeNumber                    ) {submitBash(aBashPath, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    public void submitBash(String aBashPath, String aPartition, int aNodeNumber, String aOutputPath) {submitBash(null, aBashPath, aPartition, aNodeNumber, aOutputPath);}
    
    public void submitBash(Task aBeforeSystem, String aBashPath                                                        ) {submitBash(aBeforeSystem, aBashPath, null);}
    public void submitBash(Task aBeforeSystem, String aBashPath,                    int aNodeNumber                    ) {submitBash(aBeforeSystem, aBashPath, null, aNodeNumber);}
    public void submitBash(Task aBeforeSystem, String aBashPath,                    int aNodeNumber, String aOutputPath) {submitBash(aBeforeSystem, aBashPath, null, aNodeNumber, aOutputPath);}
    public void submitBash(Task aBeforeSystem, String aBashPath, String aPartition                                     ) {submitBash(aBeforeSystem, aBashPath, aPartition, -1);}
    public void submitBash(Task aBeforeSystem, String aBashPath, String aPartition, int aNodeNumber                    ) {submitBash(aBeforeSystem, aBashPath, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    public void submitBash(Task aBeforeSystem, String aBashPath, String aPartition, int aNodeNumber, String aOutputPath) {submitBash(aBeforeSystem, null, aBashPath, aPartition, aNodeNumber, aOutputPath);}
    
    public void submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath                                                        ) {submitBash(aBeforeSystem, aAfterSystem, aBashPath, null);}
    public void submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aNodeNumber                    ) {submitBash(aBeforeSystem, aAfterSystem, aBashPath, null, aNodeNumber);}
    public void submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aNodeNumber, String aOutputPath) {submitBash(aBeforeSystem, aAfterSystem, aBashPath, null, aNodeNumber, aOutputPath);}
    public void submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition                                     ) {submitBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, -1);}
    public void submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aNodeNumber                    ) {submitBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    
    
    public Task task_submitBash(String aBashPath                                                        ) {return task_submitBash(aBashPath, null);}
    public Task task_submitBash(String aBashPath,                    int aNodeNumber                    ) {return task_submitBash(aBashPath, null, aNodeNumber);}
    public Task task_submitBash(String aBashPath,                    int aNodeNumber, String aOutputPath) {return task_submitBash(aBashPath, null, aNodeNumber, aOutputPath);}
    public Task task_submitBash(String aBashPath, String aPartition                                     ) {return task_submitBash(aBashPath, aPartition, -1);}
    public Task task_submitBash(String aBashPath, String aPartition, int aNodeNumber                    ) {return task_submitBash(aBashPath, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    public Task task_submitBash(String aBashPath, String aPartition, int aNodeNumber, String aOutputPath) {return task_submitBash(null, aBashPath, aPartition, aNodeNumber, aOutputPath);}
    
    public Task task_submitBash(Task aBeforeSystem, String aBashPath                                                        ) {return task_submitBash(aBeforeSystem, aBashPath, null);}
    public Task task_submitBash(Task aBeforeSystem, String aBashPath,                    int aNodeNumber                    ) {return task_submitBash(aBeforeSystem, aBashPath, null, aNodeNumber);}
    public Task task_submitBash(Task aBeforeSystem, String aBashPath,                    int aNodeNumber, String aOutputPath) {return task_submitBash(aBeforeSystem, aBashPath, null, aNodeNumber, aOutputPath);}
    public Task task_submitBash(Task aBeforeSystem, String aBashPath, String aPartition                                     ) {return task_submitBash(aBeforeSystem, aBashPath, aPartition, -1);}
    public Task task_submitBash(Task aBeforeSystem, String aBashPath, String aPartition, int aNodeNumber                    ) {return task_submitBash(aBeforeSystem, aBashPath, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    public Task task_submitBash(Task aBeforeSystem, String aBashPath, String aPartition, int aNodeNumber, String aOutputPath) {return task_submitBash(aBeforeSystem, null, aBashPath, aPartition, aNodeNumber, aOutputPath);}
    
    public Task task_submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath                                                        ) {return task_submitBash(aBeforeSystem, aAfterSystem, aBashPath, null);}
    public Task task_submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aNodeNumber                    ) {return task_submitBash(aBeforeSystem, aAfterSystem, aBashPath, null, aNodeNumber);}
    public Task task_submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aNodeNumber, String aOutputPath) {return task_submitBash(aBeforeSystem, aAfterSystem, aBashPath, null, aNodeNumber, aOutputPath);}
    public Task task_submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition                                     ) {return task_submitBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, -1);}
    public Task task_submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aNodeNumber                    ) {return task_submitBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, aNodeNumber, ".temp/slurm/out-%j");}
    
    
    public Task task_submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aNodeNumber, String aOutputPath) {
        return new SerializableTask(() -> {submitBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, aNodeNumber, aOutputPath); return true;}) {
            @Override public String toString() {return String.format("%s{%s:%s:%s:%s:%d:%s}", Type.SLURM_SUBMIT_BASH.name(), aBeforeSystem==null?Type.NULL.name():aBeforeSystem.toString(), aAfterSystem==null?Type.NULL.name():aAfterSystem.toString(), aBashPath, aPartition, aNodeNumber, aOutputPath);}
    };}
    public synchronized void submitBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aNodeNumber, String aOutputPath) {
        if (mDead) throw new RuntimeException("Can NOT submitBash from a Dead SLURM.");
        // 需要创建输出目录的文件夹
        aBeforeSystem = UT.Tasks.mergeTask(aBeforeSystem, task_validPath_(aOutputPath));
        // 并且需要上传脚本
        aBeforeSystem = UT.Tasks.mergeTask(aBeforeSystem, mSSH.task_putFile(aBashPath));
        // 组装指令
        String tCommand = String.format("sbatch --output %s --job-name %s", aOutputPath, mJobName);
        if (aPartition != null && !aPartition.isEmpty()) tCommand += String.format(" --partition %s", aPartition);
        if (aNodeNumber > 0) tCommand += String.format(" --nodes %d", aNodeNumber);
        tCommand += String.format(" %s", aBashPath);
        // 添加指令到队列
        mCommandList.addLast(new Pair<>(new Pair<>(aBeforeSystem, aAfterSystem), tCommand));
        doMemberChange.run();
    }
    
    /**
     * 提供一个直接使用 srun 执行指令的接口，实际会使用 sbatch 将任务挂到后台
     * 输入具体的指令，分区，任务数目（并行数目），每节点的最多任务数（用于计算节点数目）
     * 可以指定输出目录
     */
    public void submitSrun(String aCommand                                                                                   ) {submitSrun(aCommand, null);}
    public void submitSrun(String aCommand,                    int aTaskNumber                                               ) {submitSrun(aCommand, null, aTaskNumber);}
    public void submitSrun(String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrun(aCommand, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public void submitSrun(String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrun(aCommand, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public void submitSrun(String aCommand, String aPartition                                                                ) {submitSrun(aCommand, aPartition, 1);}
    public void submitSrun(String aCommand, String aPartition, int aTaskNumber                                               ) {submitSrun(aCommand, aPartition, aTaskNumber, 20);}
    public void submitSrun(String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrun(aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    public void submitSrun(String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrun(null, aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    
    public void submitSrun(Task aBeforeSystem, String aCommand                                                                                   ) {submitSrun(aBeforeSystem, aCommand, null);}
    public void submitSrun(Task aBeforeSystem, String aCommand,                    int aTaskNumber                                               ) {submitSrun(aBeforeSystem, aCommand, null, aTaskNumber);}
    public void submitSrun(Task aBeforeSystem, String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrun(aBeforeSystem, aCommand, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public void submitSrun(Task aBeforeSystem, String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrun(aBeforeSystem, aCommand, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public void submitSrun(Task aBeforeSystem, String aCommand, String aPartition                                                                ) {submitSrun(aBeforeSystem, aCommand, aPartition, 1);}
    public void submitSrun(Task aBeforeSystem, String aCommand, String aPartition, int aTaskNumber                                               ) {submitSrun(aBeforeSystem, aCommand, aPartition, aTaskNumber, 20);}
    public void submitSrun(Task aBeforeSystem, String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrun(aBeforeSystem, aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    public void submitSrun(Task aBeforeSystem, String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrun(aBeforeSystem, null, aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    
    public void submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand                                                                                   ) {submitSrun(aBeforeSystem, aAfterSystem, aCommand, null);}
    public void submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aTaskNumber                                               ) {submitSrun(aBeforeSystem, aAfterSystem, aCommand, null, aTaskNumber);}
    public void submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrun(aBeforeSystem, aAfterSystem, aCommand, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public void submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrun(aBeforeSystem, aAfterSystem, aCommand, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public void submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition                                                                ) {submitSrun(aBeforeSystem, aAfterSystem, aCommand, aPartition, 1);}
    public void submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aTaskNumber                                               ) {submitSrun(aBeforeSystem, aAfterSystem, aCommand, aPartition, aTaskNumber, 20);}
    public void submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrun(aBeforeSystem, aAfterSystem, aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    
    
    public Task task_submitSrun(String aCommand                                                                                   ) {return task_submitSrun(aCommand, null);}
    public Task task_submitSrun(String aCommand,                    int aTaskNumber                                               ) {return task_submitSrun(aCommand, null, aTaskNumber);}
    public Task task_submitSrun(String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrun(aCommand, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public Task task_submitSrun(String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrun(aCommand, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public Task task_submitSrun(String aCommand, String aPartition                                                                ) {return task_submitSrun(aCommand, aPartition, 1);}
    public Task task_submitSrun(String aCommand, String aPartition, int aTaskNumber                                               ) {return task_submitSrun(aCommand, aPartition, aTaskNumber, 20);}
    public Task task_submitSrun(String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrun(aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    public Task task_submitSrun(String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrun(null, aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    
    public Task task_submitSrun(Task aBeforeSystem, String aCommand                                                                                   ) {return task_submitSrun(aBeforeSystem, aCommand, null);}
    public Task task_submitSrun(Task aBeforeSystem, String aCommand,                    int aTaskNumber                                               ) {return task_submitSrun(aBeforeSystem, aCommand, null, aTaskNumber);}
    public Task task_submitSrun(Task aBeforeSystem, String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrun(aBeforeSystem, aCommand, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public Task task_submitSrun(Task aBeforeSystem, String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrun(aBeforeSystem, aCommand, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public Task task_submitSrun(Task aBeforeSystem, String aCommand, String aPartition                                                                ) {return task_submitSrun(aBeforeSystem, aCommand, aPartition, 1);}
    public Task task_submitSrun(Task aBeforeSystem, String aCommand, String aPartition, int aTaskNumber                                               ) {return task_submitSrun(aBeforeSystem, aCommand, aPartition, aTaskNumber, 20);}
    public Task task_submitSrun(Task aBeforeSystem, String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrun(aBeforeSystem, aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    public Task task_submitSrun(Task aBeforeSystem, String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrun(aBeforeSystem, null, aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    
    public Task task_submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand                                                                                   ) {return task_submitSrun(aBeforeSystem, aAfterSystem, aCommand, null);}
    public Task task_submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aTaskNumber                                               ) {return task_submitSrun(aBeforeSystem, aAfterSystem, aCommand, null, aTaskNumber);}
    public Task task_submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrun(aBeforeSystem, aAfterSystem, aCommand, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public Task task_submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrun(aBeforeSystem, aAfterSystem, aCommand, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public Task task_submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition                                                                ) {return task_submitSrun(aBeforeSystem, aAfterSystem, aCommand, aPartition, 1);}
    public Task task_submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aTaskNumber                                               ) {return task_submitSrun(aBeforeSystem, aAfterSystem, aCommand, aPartition, aTaskNumber, 20);}
    public Task task_submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrun(aBeforeSystem, aAfterSystem, aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    
    
    public Task task_submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {
        return new SerializableTask(() -> {submitSrun(aBeforeSystem, aAfterSystem, aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath); return true;}) {
            @Override public String toString() {return String.format("%s{%s:%s:%s:%s:%d:%d:%s}", Type.SLURM_SUBMIT_SRUN.name(), aBeforeSystem==null?Type.NULL.name():aBeforeSystem.toString(), aAfterSystem==null?Type.NULL.name():aAfterSystem.toString(), aCommand, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    };}
    public void submitSrun(Task aBeforeSystem, Task aAfterSystem, String aCommand, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {
        if (mDead) throw new RuntimeException("Can NOT submitSrun from a Dead SLURM.");
        aTaskNumber = Math.max(1, aTaskNumber);
        aMaxTaskNumberPerNode = Math.max(1, aMaxTaskNumberPerNode);
        submitSystem(aBeforeSystem, aAfterSystem, String.format("srun --ntasks %d --ntasks-per-node %d --wait 1000000 %s", aTaskNumber, aMaxTaskNumberPerNode, aCommand), aPartition, (int)Math.ceil(aTaskNumber/(double)aMaxTaskNumberPerNode), aOutputPath);
    }
    
    /**
     * 提供一个直接使用 srun 来执行脚本的接口，实际会使用 sbatch 将任务挂到后台
     * 输入本地的脚本路径，首先会将其上传到服务器对应位置
     * 与 sbatch 的脚本不同，这里不能在脚本中指定参数
     */
    public void submitSrunBash(String aBashPath                                                                                   ) {submitSrunBash(aBashPath, null);}
    public void submitSrunBash(String aBashPath,                    int aTaskNumber                                               ) {submitSrunBash(aBashPath, null, aTaskNumber);}
    public void submitSrunBash(String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrunBash(aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public void submitSrunBash(String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrunBash(aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public void submitSrunBash(String aBashPath, String aPartition                                                                ) {submitSrunBash(aBashPath, aPartition, 1);}
    public void submitSrunBash(String aBashPath, String aPartition, int aTaskNumber                                               ) {submitSrunBash(aBashPath, aPartition, aTaskNumber, 20);}
    public void submitSrunBash(String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrunBash(aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    public void submitSrunBash(String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrunBash(null, aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    
    public void submitSrunBash(Task aBeforeSystem, String aBashPath                                                                                   ) {submitSrunBash(aBeforeSystem, aBashPath, null);}
    public void submitSrunBash(Task aBeforeSystem, String aBashPath,                    int aTaskNumber                                               ) {submitSrunBash(aBeforeSystem, aBashPath, null, aTaskNumber);}
    public void submitSrunBash(Task aBeforeSystem, String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrunBash(aBeforeSystem, aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public void submitSrunBash(Task aBeforeSystem, String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrunBash(aBeforeSystem, aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public void submitSrunBash(Task aBeforeSystem, String aBashPath, String aPartition                                                                ) {submitSrunBash(aBeforeSystem, aBashPath, aPartition, 1);}
    public void submitSrunBash(Task aBeforeSystem, String aBashPath, String aPartition, int aTaskNumber                                               ) {submitSrunBash(aBeforeSystem, aBashPath, aPartition, aTaskNumber, 20);}
    public void submitSrunBash(Task aBeforeSystem, String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrunBash(aBeforeSystem, aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    public void submitSrunBash(Task aBeforeSystem, String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrunBash(aBeforeSystem, null, aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    
    public void submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath                                                                                   ) {submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, null);}
    public void submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aTaskNumber                                               ) {submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, null, aTaskNumber);}
    public void submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public void submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public void submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition                                                                ) {submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, 1);}
    public void submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aTaskNumber                                               ) {submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, aTaskNumber, 20);}
    public void submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    
    
    public Task task_submitSrunBash(String aBashPath                                                                                   ) {return task_submitSrunBash(aBashPath, null);}
    public Task task_submitSrunBash(String aBashPath,                    int aTaskNumber                                               ) {return task_submitSrunBash(aBashPath, null, aTaskNumber);}
    public Task task_submitSrunBash(String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrunBash(aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public Task task_submitSrunBash(String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrunBash(aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public Task task_submitSrunBash(String aBashPath, String aPartition                                                                ) {return task_submitSrunBash(aBashPath, aPartition, 1);}
    public Task task_submitSrunBash(String aBashPath, String aPartition, int aTaskNumber                                               ) {return task_submitSrunBash(aBashPath, aPartition, aTaskNumber, 20);}
    public Task task_submitSrunBash(String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrunBash(aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    public Task task_submitSrunBash(String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrunBash(null, aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    
    public Task task_submitSrunBash(Task aBeforeSystem, String aBashPath                                                                                   ) {return task_submitSrunBash(aBeforeSystem, aBashPath, null);}
    public Task task_submitSrunBash(Task aBeforeSystem, String aBashPath,                    int aTaskNumber                                               ) {return task_submitSrunBash(aBeforeSystem, aBashPath, null, aTaskNumber);}
    public Task task_submitSrunBash(Task aBeforeSystem, String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrunBash(aBeforeSystem, aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public Task task_submitSrunBash(Task aBeforeSystem, String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrunBash(aBeforeSystem, aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public Task task_submitSrunBash(Task aBeforeSystem, String aBashPath, String aPartition                                                                ) {return task_submitSrunBash(aBeforeSystem, aBashPath, aPartition, 1);}
    public Task task_submitSrunBash(Task aBeforeSystem, String aBashPath, String aPartition, int aTaskNumber                                               ) {return task_submitSrunBash(aBeforeSystem, aBashPath, aPartition, aTaskNumber, 20);}
    public Task task_submitSrunBash(Task aBeforeSystem, String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrunBash(aBeforeSystem, aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    public Task task_submitSrunBash(Task aBeforeSystem, String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrunBash(aBeforeSystem, null, aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    
    public Task task_submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath                                                                                   ) {return task_submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, null);}
    public Task task_submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aTaskNumber                                               ) {return task_submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, null, aTaskNumber);}
    public Task task_submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode);}
    public Task task_submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath,                    int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {return task_submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, null, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    public Task task_submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition                                                                ) {return task_submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, 1);}
    public Task task_submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aTaskNumber                                               ) {return task_submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, aTaskNumber, 20);}
    public Task task_submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode                    ) {return task_submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, ".temp/slurm/out-%j");}
    
    
    public Task task_submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {
        return new SerializableTask(() -> {submitSrunBash(aBeforeSystem, aAfterSystem, aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath); return true;}) {
            @Override public String toString() {return String.format("%s{%s:%s:%s:%s:%d:%d:%s}", Type.SLURM_SUBMIT_SRUN_BASH.name(), aBeforeSystem==null?Type.NULL.name():aBeforeSystem.toString(), aAfterSystem==null?Type.NULL.name():aAfterSystem.toString(), aBashPath, aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);}
    };}
    public void submitSrunBash(Task aBeforeSystem, Task aAfterSystem, String aBashPath, String aPartition, int aTaskNumber, int aMaxTaskNumberPerNode, String aOutputPath) {
        if (mDead) throw new RuntimeException("Can NOT submitSrunBash from a Dead SLURM.");
        // 需要上传脚本
        aBeforeSystem = UT.Tasks.mergeTask(aBeforeSystem, mSSH.task_putFile(aBashPath));
        // 提交命令
        submitSrun(aBeforeSystem, aAfterSystem, String.format("bash %s", aBashPath), aPartition, aTaskNumber, aMaxTaskNumberPerNode, aOutputPath);
    }
    
    
    // 获取这个用户正在执行的任务，和这个类本身无关
    public int jobNumber() throws JSchException, IOException {if (mDead) throw new RuntimeException("Can NOT get jobNumber from a Dead SLURM."); return jobIDs().size();}
    // 获取这个用户正在执行的任务 ID 的列表
    public Set<Integer> jobIDs() throws JSchException, IOException {if (mDead) throw new RuntimeException("Can NOT get jobIDs from a Dead SLURM."); return jobIDs_();}
    Set<Integer> jobIDs_() throws JSchException, IOException {
        // 组装指令
        String tCommand = String.format("squeue --noheader --user %s --format %%i", mSqueueName);
        // systemChannel 内部已经尝试了重连
        ChannelExec tChannelExec = mSSH.systemChannel(tCommand);
        // 获取输出得到任务数目
        InputStream tIn = tChannelExec.getInputStream();
        tChannelExec.connect();
        BufferedReader tReader = new BufferedReader(new InputStreamReader(tIn));
        Set<Integer> rJobIDs = new LinkedHashSet<>();
        String tLine;
        while ((tLine = tReader.readLine()) != null) rJobIDs.add(Integer.parseInt(tLine));
        // 最后关闭通道
        tChannelExec.disconnect();
        return rJobIDs;
    }
    
    // 取消这个用户所有的任务
    public Task task_cancelAll() {return new SerializableTask(() -> {cancelAll(); return true;}) {
        @Override public String toString() {return Type.SLURM_CANCEL_ALL.name();}
    };}
    public synchronized void cancelAll() throws JSchException, IOException {
        if (mDead) throw new RuntimeException("Can NOT cancelAll from a Dead SLURM.");
        mCommandList.clear(); doMemberChange.run();
        mSSH.system(String.format("scancel --user %s --full", mSqueueName));
        mJobIDList.clear(); doMemberChange.run();
    }
    
    // 取消这个对象一共提交的所有任务
    public Task task_cancelThis() {return new SerializableTask(() -> {cancelThis(); return true;}) {
        @Override public String toString() {return Type.SLURM_CANCEL_THIS.name();}
    };}
    public synchronized void cancelThis() throws JSchException, IOException {
        if (mDead) throw new RuntimeException("Can NOT cancelThis from a Dead SLURM.");
        mCommandList.clear(); doMemberChange.run();
        mSSH.system(String.format("scancel --name %s", mJobName));
        mJobIDList.clear(); doMemberChange.run();
    }
    
    // 撤销上一步提交的任务（如果已经交上去则会失败）
    public synchronized Pair<Pair<Task, Task>, String> undo() {
        Pair<Pair<Task, Task>, String> tCommand;
        tCommand = mCommandList.pollLast(); doMemberChange.run();
        return tCommand;
    }
    
    /// 提供 SystemThreadPool 的相关接口
    public synchronized int getActiveCount() {return mJobIDList.size();}
    public synchronized int getQueueSize() {return mCommandList.size();}
    public void waitUntilDone() throws InterruptedException {while (getActiveCount() > 0 || getQueueSize() > 0) Thread.sleep(200);}
    public int getTaskNumber() {return getActiveCount() + getQueueSize();}
    
    public boolean awaitTermination() throws InterruptedException {return mPool.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);}
    public synchronized int[] getActiveJobIDs() {
        int[] tJobIDs = new int[mJobIDList.size()];
        int i = 0;
        for (int tJobID : mJobIDList.keySet()) {tJobIDs[i] = tJobID; ++i;}
        return tJobIDs;
    }
    public synchronized String[] getQueueCommands() {
        String[] tCommands = new String[mCommandList.size()];
        int i = 0;
        for (Pair<Pair<Task, Task>, String> tPair : mCommandList) {tCommands[i] = tPair.second; ++i;}
        return tCommands;
    }
    
    
    /// 提供一些基本内部类
    // 容忍次数计数器，可以通过重写输入的 Runnable 来实现具体的操作
    static class TolerantCounter {
        private int mTolerant = DEFAULT_TOLERANT;
        private int mUsedTolerant = 0;
        
        public void call(boolean aSuc, String aPrint, Runnable doOverTolerant) {call(aSuc, aPrint, doOverTolerant, () -> {}, () -> {});}
        public void call(boolean aSuc, String aPrint, Runnable doOverTolerant, Runnable doUnsuccess, Runnable doSuccess) {
            if (!aSuc) {
                ++mUsedTolerant;
                if (mUsedTolerant > mTolerant) {
                    mUsedTolerant = 0;
                    doOverTolerant.run();
                    System.out.println("WARNING: Fail more than "+mTolerant+" times in "+aPrint);
                    System.out.flush();
                }
                doUnsuccess.run();
            } else {
                mUsedTolerant = 0;
                doSuccess.run();
            }
        }
    }
    
    // 手动加载 UT，会自动重新设置工作目录，会在调用静态函数 get 或者 load 时自动加载保证路径的正确性
    static {UT.IO.init();}
}
