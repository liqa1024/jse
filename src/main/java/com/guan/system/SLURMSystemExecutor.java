package com.guan.system;


import com.guan.code.UT;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * @author liqa
 * <p> 一般的 SLURM 实现，基于 SSH 的远程 Executor，因此针对的是使用 SSH 连接的远程 SLURM 服务器 </p>
 */
public class SLURMSystemExecutor extends AbstractNoPoolSystemExecutor<SSHSystemExecutor> {
    public final static String DEFAULT_OUTFILE_DIR = ".slurm/";
    public final static String DEFAULT_OUTFILE_PATH = DEFAULT_OUTFILE_DIR+"out-%n-%i"; // %n: unique job name, %i: index of job
    
    private int mCurrentJobIdx = 0;
    
    /// 构造函数部分
    private final long mSleepTime;
    private final boolean mNoConsoleOutput;
    private final String mUniqueJobName; // 注意一定要是独立的，避免相互干扰或者影响其他用户的结果
    private final @Nullable String mPartition;
    private final int mTaskNum; // 目前一个 Executor 固定一个 taskNumber，用不到变化的，打包起来也会更加方便
    private final int mMaxTaskNumPerNode;
    private final String mSqueueName;
    SLURMSystemExecutor(SSHSystemExecutor aSystemExecutor, int aParallelNum, long aSleepTime, boolean aNoConsoleOutput, String aUniqueJobName, @Nullable String aPartition, int aTaskNum, int aMaxTaskNumPerNode, @Nullable String aSqueueName) throws Exception {
        super(aSystemExecutor, aParallelNum);
        mSleepTime = aSleepTime;
        mNoConsoleOutput = aNoConsoleOutput;
        mUniqueJobName = aUniqueJobName;
        mPartition = aPartition;
        mTaskNum = aTaskNum;
        mMaxTaskNumPerNode = aMaxTaskNumPerNode;
        mSqueueName = aSqueueName==null? mEXE.mSSH.session().getUserName() : aSqueueName;
        // 需要初始化输出的文件夹
        // 注意初始化失败时需要抛出异常并且执行关闭操作
        try {
            mEXE.mSSH.makeDir(DEFAULT_OUTFILE_DIR);;
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
     *   "ParallelNumber": ${integerNumberOfParallelNumberForSubmitSystemUse},
     *   "IOThreadNumber": ${integerNumberOfThreadNumberForPutAndGetFilesUse},
     *   "SleepTime": ${integerNumberOfSleepTimeInMilliSecondForInternalLoop},
     *   "NoConsoleOutput": ${BooleanValueToControlOutputInformationToConsole},
     *
     *   "JobName": "${UniqueJobNameForThisExecutorUse}",
     *   "Partition": "${PartitionOfSlurmThisExecutorWillUse}",
     *   "TaskNumber": ${integerNumberOfTaskNumberForJob},
     *   "MaxTaskNumberPerNode": ${integerNumberOfMaxTaskNumberPerNodeOfThisSlurmServer},
     *   "SqueueName": "${usernameForSqueueUse}",
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
     * "ParallelNumber" > "parallelmumber" > "ParallelNum" > "parallelnum" > "pn"
     * "IOThreadNumber" > "iothreadnumber" > "IOThreadNum" > "iothreadnum" > "ion"
     * "SleepTime" > "sleeptime" > "stime" > "st"
     * "NoConsoleOutput" > "noconsoleoutput" > "NoOuput" > "nooutput" > "no"
     *
     * "JobName" > "jobname" > "job-name" > "J"
     * "Partition" > "partition" > "p"
     * "TaskNumber" > "tasknumber" > "TaskNum" > "tasknum" > "nTasks" > "ntasks" > "n"
     * "MaxTaskNumberPerNode" > "maxtasknumberpernode" > "MaxTaskNumPerNode" > "maxtasknumpernode" > "CoresPerNode" > "corespernode" > "ntaskspernode" > "ntasks-per-node"
     * "SqueueName" > "squeuename" > "squeue" > "s"
     *
     * "Username" > "username" > "user" > "u"
     * "Hostname" > "hostname" > "host" > "h"
     * "Port" > "port" > "p"
     * "Password" > "password" > "pw"
     * "KeyPath" > "keypath" > "key" > "k"
     * "CompressLevel" > "compresslevel" > "cl"
     * "LocalWorkingDir" > "localworkingdir" > "lwd"
     * "RemoteWorkingDir" > "remoteworkingdir" > "rwd" > "wd"
     * "BeforeCommand" > "beforecommand" > "bcommand" > "bc"
     * </pre>
     * 参数 "ParallelNumber" 未选定时默认为 1，参数 "IOThreadNumber" 未选定时不开启并行传输，
     * "SleepTime" 未选定时默认为 500（ms），"NoConsoleOutput" 未选定时默认为 false（即开启输出信息到控制台）
     * <p>
     * "JobName" 未选定时会是格式 "jTool@${RandomString}"，"Partition" 未选定时会使用 slurm 服务器上默认的分区，
     * "TaskNumber" 未选定时默认为 1，"MaxTaskNumberPerNode" 未选定时默认为 20，"SqueueName" 未选定时默认为登录使用的用户名
     * <p>
     * "Port" 未选定时默认为 22，"Password" 未选定时使用 publicKey 密钥认证，"KeyPath" 未选定时使用默认路径的密钥，
     * "CompressLevel" 未选定时不开启压缩，"LocalWorkingDir" 未选定时使用程序运行路径，
     * "RemoteWorkingDir" 未选定时使用 ssh 登录所在的路径
     * @author liqa
     */
    public SLURMSystemExecutor(                                                                                             Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1,               aArgs), getParallelNum(aArgs), getSleepTime(aArgs), getNoConsoleOutput(aArgs), getJobName(aArgs), getPartition(aArgs), getTaskNumber(aArgs), getMaxTaskNumberPerNode(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(int aParallelNum,                                                                            Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1,               aArgs), aParallelNum         , getSleepTime(aArgs), getNoConsoleOutput(aArgs), getJobName(aArgs), getPartition(aArgs), getTaskNumber(aArgs), getMaxTaskNumberPerNode(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(int aParallelNum, int aIOThreadNum,                                                          Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1, aIOThreadNum, aArgs), aParallelNum         , getSleepTime(aArgs), getNoConsoleOutput(aArgs), getJobName(aArgs), getPartition(aArgs), getTaskNumber(aArgs), getMaxTaskNumberPerNode(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(int aParallelNum, int aIOThreadNum, String aPartition,                                       Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1, aIOThreadNum, aArgs), aParallelNum         , getSleepTime(aArgs), getNoConsoleOutput(aArgs), getJobName(aArgs), aPartition         , getTaskNumber(aArgs), getMaxTaskNumberPerNode(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(int aParallelNum, int aIOThreadNum, String aPartition, int aTaskNum, int aMaxTaskNumPerNode, Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1, aIOThreadNum, aArgs), aParallelNum         , getSleepTime(aArgs), getNoConsoleOutput(aArgs), getJobName(aArgs), aPartition         , aTaskNum            , aMaxTaskNumPerNode            , getSqueueName(aArgs));}
    public SLURMSystemExecutor(                                    String aPartition, int aTaskNum, int aMaxTaskNumPerNode, Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1,               aArgs), getParallelNum(aArgs), getSleepTime(aArgs), getNoConsoleOutput(aArgs), getJobName(aArgs), aPartition         , aTaskNum            , aMaxTaskNumPerNode            , getSqueueName(aArgs));}
    public SLURMSystemExecutor(                                    String aPartition                                      , Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1,               aArgs), getParallelNum(aArgs), getSleepTime(aArgs), getNoConsoleOutput(aArgs), getJobName(aArgs), aPartition         , getTaskNumber(aArgs), getMaxTaskNumberPerNode(aArgs), getSqueueName(aArgs));}
    
    
    public static int       getParallelNum          (Map<?, ?> aArgs) {return ((Number) UT.Code.getWithDefault(aArgs, 1, "ParallelNumber", "parallelmumber", "ParallelNum", "parallelnum", "pn")).intValue();}
    public static long      getSleepTime            (Map<?, ?> aArgs) {return ((Number) UT.Code.getWithDefault(aArgs, 500, "SleepTime", "sleeptime", "stime", "st")).longValue();}
    public static boolean   getNoConsoleOutput      (Map<?, ?> aArgs) {return (Boolean) UT.Code.getWithDefault(aArgs, false, "NoConsoleOutput", "noconsoleoutput", "NoOuput", "nooutput", "no");}
    public static String    getJobName              (Map<?, ?> aArgs) {return (String)  UT.Code.getWithDefault(aArgs, "jTool@"+UT.Code.randID(), "JobName", "jobname", "job-name", "J");}
    public static @Nullable String getPartition     (Map<?, ?> aArgs) {return (String)  UT.Code.getWithDefault(aArgs, null, "Partition", "partition", "p");}
    public static int       getTaskNumber           (Map<?, ?> aArgs) {return ((Number) UT.Code.getWithDefault(aArgs, 1, "TaskNumber", "tasknumber", "TaskNum", "tasknum", "nTasks", "ntasks", "n")).intValue();}
    public static int       getMaxTaskNumberPerNode (Map<?, ?> aArgs) {return ((Number) UT.Code.getWithDefault(aArgs, 20, "MaxTaskNumberPerNode", "maxtasknumberpernode", "MaxTaskNumPerNode", "maxtasknumpernode", "CoresPerNode", "corespernode", "ntaskspernode", "ntasks-per-node")).intValue();}
    public static @Nullable String getSqueueName    (Map<?, ?> aArgs) {return (String)  UT.Code.getWithDefault(aArgs, null, "SqueueName", "squeuename", "squeue", "s");}
    
    
    
    
    /** AbstractNoPoolSystemExecutor stuffs */
    @Override protected boolean noConsoleOutput() {return mNoConsoleOutput;}
    @Override protected long sleepTime() {return mSleepTime;}
    
    @Override protected void putFiles(Iterable<String> aFiles) throws Exception {mEXE.putFiles(aFiles);}
    @Override protected void getFiles(Iterable<String> aFiles) throws Exception {mEXE.getFiles(aFiles);}
    
    /** 这里固定 SLURM 默认的输出路径 */
    @Override protected @Nullable String defaultOutFilePath() {return DEFAULT_OUTFILE_PATH;}
    @Override protected @Nullable String toRealOutFilePath(String aOutFilePath) {
        // 对输出文件路径提供一个简单的解码，这里只支持自定义的一些写法而不是 slurm 中的写法（无法简单获取到 slurm 的其他信息）
        // 并且任务计数也放在这里，因此不能保证任务数是有意义的，仅保证是不同的
        return aOutFilePath.replaceAll("%n", mUniqueJobName).replaceAll("%i", String.valueOf(mCurrentJobIdx++));
    }
    /** run 使用 srun 指令，submit 使用 sbatch 指令，内部提交一个运行 srun 的脚本 */
    @Override protected String getRunCommand(String aCommand, @Nullable String aOutFilePath) {
        int tNodeNum = (int)Math.ceil(mTaskNum / (double) mMaxTaskNumPerNode);
        // 组装指令
        List<String> rRunCommand = new ArrayList<>();
        rRunCommand.add("srun");
        rRunCommand.add("--nodes");             rRunCommand.add(String.valueOf(tNodeNum));
        rRunCommand.add("--job-name");          rRunCommand.add(mUniqueJobName);
        rRunCommand.add("--ntasks");            rRunCommand.add(String.valueOf(mTaskNum));
        rRunCommand.add("--ntasks-per-node");   rRunCommand.add(String.valueOf(mMaxTaskNumPerNode));
        rRunCommand.add("--wait");              rRunCommand.add("1000000");
        if (aOutFilePath != null && !aOutFilePath.isEmpty()) {
        rRunCommand.add("--output");            rRunCommand.add(aOutFilePath);
        }
        if (aOutFilePath != null && !aOutFilePath.isEmpty()) {
        rRunCommand.add("--partition");         rRunCommand.add(mPartition);
        }
        rRunCommand.add(aCommand);
        // 获得指令
        return String.join(" ", rRunCommand);
    }
    @Override protected String getSubmitCommand(String aCommand, @Nullable String aOutFilePath) {
        int tNodeNum = (int)Math.ceil(mTaskNum / (double) mMaxTaskNumPerNode);
        // 组装运行指令
        List<String> rRunCommand = new ArrayList<>();
        rRunCommand.add("srun");
        rRunCommand.add("--ntasks");            rRunCommand.add(String.valueOf(mTaskNum));
        rRunCommand.add("--ntasks-per-node");   rRunCommand.add(String.valueOf(mMaxTaskNumPerNode));
        rRunCommand.add("--wait");              rRunCommand.add("1000000");
        rRunCommand.add(aCommand);
        // 组装提交指令
        List<String> rSubmitCommand = new ArrayList<>();
        rSubmitCommand.add(String.format("echo -e '#!/bin/bash\\n%s'", String.join(" ", rRunCommand)));
        rSubmitCommand.add("|");
        rSubmitCommand.add("sbatch");
        rSubmitCommand.add("--nodes");              rSubmitCommand.add(String.valueOf(tNodeNum));
        rSubmitCommand.add("--job-name");           rSubmitCommand.add(mUniqueJobName);
        if (aOutFilePath != null && !aOutFilePath.isEmpty()) {
        rSubmitCommand.add("--output");             rSubmitCommand.add(aOutFilePath);
        }
        if (aOutFilePath != null && !aOutFilePath.isEmpty()) {
        rSubmitCommand.add("--partition");          rSubmitCommand.add(mPartition);
        }
        // 获得指令
        return String.join(" ", rSubmitCommand);
    }
    
    /** 使用 submit 指令后系统会给出输出，需要使用这个输出来获取对应任务的 ID 用于监控任务是否完成，返回 <= 0 的值代表提交任务失败 */
    @Override protected int getJobIDFromSystem(List<String> aOutList) {
        if (aOutList.isEmpty()) return -2;
        String tLine = aOutList.get(0); // 只需要读取一行
        // 返回任务号
        if (tLine.startsWith("Submitted batch job ")) return Integer.parseInt(tLine.substring(20));
        return -3;
    }
    /** 获取这个对象提交的任务中，正在执行的任务 id 列表，用来监控任务是否完成 */
    @Override protected @Nullable Set<Integer> getRunningJobIDsFromSystem() {
        // 组装指令，增加一个 echo 来标识指令执行成功
        String tCommand = String.format("squeue --noheader --user %s --format %%i; echo 'END'", mSqueueName);
        // 直接获取输出
        List<String> tLines = mEXE.system_str(tCommand);
        // 获取输出得到任务 ID
        Set<Integer> rJobIDs = new LinkedHashSet<>();
        boolean tValid = false;
        for (String tLine : tLines) {
            if (tLine.equals("END")) {tValid = true; break;}
            try {
                int tJobID = Integer.parseInt(tLine);
                rJobIDs.add(tJobID);
            } catch (Exception ignored) {}
        }
        if (tValid) return rJobIDs;
        // 不合法时会输出不合法的结果，由于这个操作是长期的且是允许的，因此会输出到 out 并且可以通过 noConsoleOutput 抑制
        if (!noConsoleOutput()) for (String tLine : tLines) if (!tLine.equals("END")) System.out.println(tLine);
        return null;
    }
    /** 取消指定任务 ID 的任务，返回是否取消成功（已经完成的也会返回 false）*/
    @Override protected boolean cancelJobFromSystem(int aJobID) {
        if (aJobID <= 0) return false;
        // 组装指令，增加一个 echo 来标识指令执行成功
        String tCommand = String.format("scancel --name %s %d; echo 'END'", mUniqueJobName, aJobID); // 保证只会取消这个对象提交的任务
        // 直接获取输出
        List<String> tLines = mEXE.system_str(tCommand);
        // 由于 slurm 的特性，不能保证获取到成功取消的信息，这里不去纠结这个
        boolean tValid = false;
        for (String tLine : tLines) {
            if (tLine.equals("END")) {tValid = true; break;}
            if (tLine.startsWith("scancel: error:")) {break;}
        }
        if (tValid) return true; // 不一定真的成功取消了，但是这里还是返回 true
        // 失败时输出失败的结果，由于这个操作是允许的，因此会输出到 out 并且可以通过 noConsoleOutput 抑制
        if (!noConsoleOutput()) for (String tLine : tLines) if (!tLine.equals("END")) System.out.println(tLine);
        return false;
    }
    /** 取消这个对象提交的所有任务，子类重写来优化避免重复的提交指令 */
    @Override protected synchronized void cancelThis_() {
        if (noConsoleOutput()) mEXE.system_NO(String.format("scancel --name %s", mUniqueJobName));
        else mEXE.system(String.format("scancel --name %s", mUniqueJobName));
    }
}
