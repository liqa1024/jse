package com.jtool.system;


import com.jtool.code.UT;
import com.jtool.io.IHasIOFiles;
import com.jtool.io.IOFiles;
import com.jtool.jobs.ILongTimeJobPool;
import com.jtool.math.MathEX;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

import static com.jtool.code.CS.*;

/**
 * @author liqa
 * <p> 一般的 SLURM 实现，基于 SSH 的远程 Executor，因此针对的是使用 SSH 连接的远程 SLURM 服务器 </p>
 */
public class SLURMSystemExecutor extends AbstractNoPoolSystemExecutor<SSHSystemExecutor> implements ILongTimeJobPool {
    /** 一些目录设定， %n: unique job name, %i: index of job，注意只有 OUTFILE_PATH 支持 %i */
    public final static String SPLIT_NODE_SCRIPT_PATH = WORKING_DIR+"splitNodeList.sh";
    public final static String BATCHED_SCRIPT_DIR = WORKING_DIR+"batched/";
    public final static String DEFAULT_OUTFILE_DIR = ".temp/slurm/";
    public final static String DEFAULT_OUTFILE_PATH = DEFAULT_OUTFILE_DIR+"out-%i-%n";
    
    /// 构造函数部分
    private final String mWorkingDir, mSplitNodeScriptPath, mBatchedScriptDir;
    
    private final long mSleepTime;
    private final String mUniqueJobName; // 注意一定要是独立的，避免相互干扰或者影响其他用户的结果
    private final @Nullable String mPartition;
    private final int mTaskNum; // 目前一个 Executor 固定一个 taskNumber，用不到变化的，打包起来也会更加方便
    private final int mMaxTaskNumPerNode;
    private final int mMaxNodeNum;
    private final String mSqueueName;
    SLURMSystemExecutor(SSHSystemExecutor aSystemExecutor, int aParallelNum, long aSleepTime, String aUniqueJobName, @Nullable String aPartition, int aTaskNum, int aMaxTaskNumPerNode, int aMaxNodeNum, @Nullable String aSqueueName) throws Exception {
        super(aSystemExecutor, aParallelNum);
        // 其余的初始化
        mSleepTime = aSleepTime;
        mUniqueJobName = aUniqueJobName;
        mPartition = aPartition;
        mTaskNum = aTaskNum;
        mMaxTaskNumPerNode = aMaxTaskNumPerNode;
        mMaxNodeNum = aMaxNodeNum;
        mSqueueName = aSqueueName==null? mEXE.mSSH.session().getUserName() : aSqueueName;
        // 需要初始化输出的文件夹
        mWorkingDir = WORKING_DIR.replaceAll("%n", mUniqueJobName);
        mSplitNodeScriptPath = SPLIT_NODE_SCRIPT_PATH.replaceAll("%n", mUniqueJobName);
        mBatchedScriptDir = BATCHED_SCRIPT_DIR.replaceAll("%n", mUniqueJobName);
        // 注意初始化失败时需要抛出异常并且执行关闭操作
        if (!(this.makeDir(mWorkingDir) && this.makeDir(mBatchedScriptDir) && this.makeDir(DEFAULT_OUTFILE_DIR))) {
            this.shutdown(); // 构造函数中不调用多态方法
            throw new IOException("Fail in Init makeDir");
        }
        // 从资源文件中创建已经准备好的 SplitNodeScript
        try (BufferedReader tReader = UT.IO.toReader(UT.IO.getResource("slurm/splitNodeList.sh")); PrintStream tPS = UT.IO.toPrintStream(mSplitNodeScriptPath)) {
            String tLine;
            while ((tLine = tReader.readLine()) != null) tPS.println(tLine);
            // 上传脚本文件并设置权限
            mEXE.system("chmod 777 "+mSplitNodeScriptPath, new IOFiles().putIFiles(IFILE_KEY, mSplitNodeScriptPath));
        } catch (Exception e) {
            // 出现任何错误直接抛出错误退出
            this.shutdown();
            throw e;
        }
    }
    @Override protected void shutdown_() {
        // 顺便删除自己的临时工作目录
        this.removeDir(mWorkingDir);
        // 需要在父类 shutdown 之前，因为之后 ssh 就关闭了
        super.shutdown_();
    }
    
    
    /** 保存参数部分，和输入格式完全一直，需要增加 mQueuedJobList 和 mJobList 的保存 */
    @ApiStatus.Internal
    @SuppressWarnings({"rawtypes", "unchecked", "SuspiciousIndentAfterControlStatement"})
    @Override public void save(Map rSaveTo) {
        // 保存前先暂停
        pause();
        // 先保存内部 SSH
        mEXE.save(rSaveTo);
        // 保存自身的额外参数
        if (mParallelNum > 1)
        rSaveTo.put("ParallelNumber", mParallelNum);
        rSaveTo.put("SleepTime", mSleepTime);
        rSaveTo.put("JobName", mUniqueJobName);
        if (mPartition != null && !mPartition.isEmpty())
        rSaveTo.put("Partition", mPartition);
        if (mTaskNum != 1)
        rSaveTo.put("TaskNumber", mTaskNum);
        rSaveTo.put("MaxTaskNumberPerNode", mMaxTaskNumPerNode);
        rSaveTo.put("MaxNodeNumber", mMaxNodeNum);
        if (!mSqueueName.equals(mEXE.mSSH.session().getUserName()))
        rSaveTo.put("SqueueName", mSqueueName);
        rSaveTo.put("JobNumber", jobNumber());
        // 保存 mQueuedJobList 和 mJobList
        saveQueuedJobList(rSaveTo);
        saveJobList(rSaveTo);
        // 保存完毕不自动解除暂停，保证文件一致性
    }
    
    /** 内部使用的完整 load 的方法，除了可以输入的设置属性，还会加载 mQueuedJobList，mJobList 等 */
    @ApiStatus.Internal
    public static SLURMSystemExecutor load(Map<?, ?> aLoadFrom) throws Exception {
        SLURMSystemExecutor rSLURM = new SLURMSystemExecutor(aLoadFrom);
        // 首先立刻暂停，进行其余属性的加载
        rSLURM.pause();
        // 加载这些属性
        rSLURM.loadQueuedJobList(aLoadFrom);
        rSLURM.loadJobList(aLoadFrom);
        rSLURM.setJobNumber(((Number)aLoadFrom.get("JobNumber")).intValue());
        // 解除暂停
        rSLURM.unpause();
        return rSLURM;
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
     *
     *   "JobName": "${UniqueJobNameForThisExecutorUse}",
     *   "Partition": "${PartitionOfSlurmThisExecutorWillUse}",
     *   "TaskNumber": ${integerNumberOfTaskNumberForJob},
     *   "MaxTaskNumberPerNode": ${integerNumberOfMaxTaskNumberPerNodeOfThisSlurmServer},
     *   "MaxNodeNumber": ${integerNumberOfMaxNodeNumberOfSingleJob},
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
     * "ParallelNumber" > "parallelnumber" > "ParallelNum" > "parallelnum" > "pn"
     * "IOThreadNumber" > "iothreadnumber" > "IOThreadNum" > "iothreadnum" > "ion"
     * "SleepTime" > "sleeptime" > "stime" > "st"
     *
     * "JobName" > "jobname" > "job-name" > "J"
     * "Partition" > "partition" > "p"
     * "TaskNumber" > "tasknumber" > "TaskNum" > "tasknum" > "nTasks" > "ntasks" > "n"
     * "MaxTaskNumberPerNode" > "maxtasknumberpernode" > "MaxTaskNumPerNode" > "maxtasknumpernode" > "CoresPerNode" > "corespernode" > "ntaskspernode" > "ntasks-per-node"
     * "MaxNodeNumber" > "maxnodenumber" > "MaxNodeNum" > "maxnodenum" > "nodes" > "N"
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
     * "JobName" 未选定时会是格式 "SLURM@${RandomString}"，"Partition" 未选定时会使用 slurm 服务器上默认的分区，
     * "TaskNumber" 未选定时默认为 1，"MaxTaskNumberPerNode" 未选定时默认为 20，"SqueueName" 未选定时默认为登录使用的用户名
     * <p>
     * "Port" 未选定时默认为 22，"Password" 未选定时使用 publicKey 密钥认证，"KeyPath" 未选定时使用默认路径的密钥，
     * "CompressLevel" 未选定时不开启压缩，"LocalWorkingDir" 未选定时使用程序运行路径，
     * "RemoteWorkingDir" 未选定时使用 ssh 登录所在的路径
     * @author liqa
     */
    public SLURMSystemExecutor(                                                                                             Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1,               aArgs), getParallelNum(aArgs), getSleepTime(aArgs), getJobName(aArgs), getPartition(aArgs), getTaskNumber(aArgs), getMaxTaskNumPerNode(aArgs), getMaxNodeNum(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(int aParallelNum,                                                                            Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1,               aArgs), aParallelNum         , getSleepTime(aArgs), getJobName(aArgs), getPartition(aArgs), getTaskNumber(aArgs), getMaxTaskNumPerNode(aArgs), getMaxNodeNum(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(int aParallelNum, int aIOThreadNum,                                                          Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1, aIOThreadNum, aArgs), aParallelNum         , getSleepTime(aArgs), getJobName(aArgs), getPartition(aArgs), getTaskNumber(aArgs), getMaxTaskNumPerNode(aArgs), getMaxNodeNum(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(int aParallelNum, int aIOThreadNum, String aPartition,                                       Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1, aIOThreadNum, aArgs), aParallelNum         , getSleepTime(aArgs), getJobName(aArgs), aPartition         , getTaskNumber(aArgs), getMaxTaskNumPerNode(aArgs), getMaxNodeNum(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(int aParallelNum, int aIOThreadNum, String aPartition, int aTaskNum, int aMaxTaskNumPerNode, Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1, aIOThreadNum, aArgs), aParallelNum         , getSleepTime(aArgs), getJobName(aArgs), aPartition         , aTaskNum            , aMaxTaskNumPerNode         , getMaxNodeNum(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(                                    String aPartition, int aTaskNum, int aMaxTaskNumPerNode, Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1,               aArgs), getParallelNum(aArgs), getSleepTime(aArgs), getJobName(aArgs), aPartition         , aTaskNum            , aMaxTaskNumPerNode         , getMaxNodeNum(aArgs), getSqueueName(aArgs));}
    public SLURMSystemExecutor(                                    String aPartition                                      , Map<?, ?> aArgs) throws Exception {this(new SSHSystemExecutor(-1,               aArgs), getParallelNum(aArgs), getSleepTime(aArgs), getJobName(aArgs), aPartition         , getTaskNumber(aArgs), getMaxTaskNumPerNode(aArgs), getMaxNodeNum(aArgs), getSqueueName(aArgs));}
    
    
    public static int       getParallelNum          (Map<?, ?> aArgs) {return ((Number) UT.Code.getWithDefault(aArgs, 1, "ParallelNumber", "parallelnumber", "ParallelNum", "parallelnum", "pn")).intValue();}
    public static long      getSleepTime            (Map<?, ?> aArgs) {return ((Number) UT.Code.getWithDefault(aArgs, 500, "SleepTime", "sleeptime", "stime", "st")).longValue();}
    public static String    getJobName              (Map<?, ?> aArgs) {return (String)  UT.Code.getWithDefault(aArgs, "SLURM@"+UT.Code.randID(), "JobName", "jobname", "job-name", "J");}
    public static @Nullable String getPartition     (Map<?, ?> aArgs) {return (String)  UT.Code.getWithDefault(aArgs, null, "Partition", "partition", "p");}
    public static int       getTaskNumber           (Map<?, ?> aArgs) {return ((Number) UT.Code.getWithDefault(aArgs, 1, "TaskNumber", "tasknumber", "TaskNum", "tasknum", "nTasks", "ntasks", "n")).intValue();}
    public static int       getMaxTaskNumPerNode    (Map<?, ?> aArgs) {return ((Number) UT.Code.getWithDefault(aArgs, 20, "MaxTaskNumberPerNode", "maxtasknumberpernode", "MaxTaskNumPerNode", "maxtasknumpernode", "CoresPerNode", "corespernode", "ntaskspernode", "ntasks-per-node")).intValue();}
    public static int       getMaxNodeNum           (Map<?, ?> aArgs) {return ((Number) UT.Code.getWithDefault(aArgs, 10, "MaxNodeNumber", "maxnodenumber", "MaxNodeNum", "maxnodenum", "nodes", "N")).intValue();}
    public static @Nullable String getSqueueName    (Map<?, ?> aArgs) {return (String)  UT.Code.getWithDefault(aArgs, null, "SqueueName", "squeuename", "squeue", "s");}
    
    
    
    
    /** AbstractNoPoolSystemExecutor stuffs */
    @Override protected int maxBatchSize() {
        // 根据 mTaskNum 和 mMaxTaskNumPerNode 来决定最大的一组的大小
        // 这里当 mTaskNum >= mMaxTaskNumPerNode 时，会直接固定到 1，即不能对这种任务进行打包
        if (mTaskNum >= mMaxTaskNumPerNode) return 1;
        return ((mMaxTaskNumPerNode-1) / mTaskNum) * mMaxNodeNum;
    }
    
    @Override protected long sleepTime() {return mSleepTime;}
    
    @Override protected void putFiles(Iterable<String> aFiles) throws Exception {mEXE.putFiles(aFiles);}
    @Override protected void getFiles(Iterable<String> aFiles) throws Exception {mEXE.getFiles(aFiles);}
    
    /** 这里固定 SLURM 默认的输出路径 */
    @Override protected @NotNull String defaultOutFilePath() {return DEFAULT_OUTFILE_PATH;}
    @Override protected @NotNull String toRealOutFilePath(String aOutFilePath) {
        // 对输出文件路径提供一个简单的解码，这里只支持自定义的一些写法而不是 slurm 中的写法（无法简单获取到 slurm 的其他信息）
        return aOutFilePath.replaceAll("%n", mUniqueJobName).replaceAll("%i", String.valueOf(jobNumber()));
    }
    /** run 使用 srun 指令，submit 使用 sbatch 指令，内部提交一个运行 srun 的脚本 */
    @Override protected String getRunCommand(String aCommand, @NotNull String aOutFilePath) {
        int tNodeNum = MathEX.Code.divup(mTaskNum, mMaxTaskNumPerNode);
        // 组装指令
        List<String> rRunCommand = new ArrayList<>();
        rRunCommand.add("srun");
        rRunCommand.add("--nodes");             rRunCommand.add(String.valueOf(tNodeNum));
        rRunCommand.add("--job-name");          rRunCommand.add(mUniqueJobName);
        rRunCommand.add("--ntasks");            rRunCommand.add(String.valueOf(mTaskNum));
        rRunCommand.add("--ntasks-per-node");   rRunCommand.add(String.valueOf(mMaxTaskNumPerNode));
        rRunCommand.add("--wait");              rRunCommand.add("1000000");
        rRunCommand.add("--output");            rRunCommand.add(aOutFilePath);
        if (mPartition != null && !mPartition.isEmpty()) {
        rRunCommand.add("--partition");         rRunCommand.add(mPartition);
        }
        rRunCommand.add(aCommand);
        // 获得指令
        return String.join(" ", rRunCommand);
    }
    @Override protected String getSubmitCommand(String aCommand, @NotNull String aOutFilePath) {
        int tNodeNum = MathEX.Code.divup(mTaskNum, mMaxTaskNumPerNode);
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
        rSubmitCommand.add("--output");             rSubmitCommand.add(aOutFilePath);
        if (mPartition != null && !mPartition.isEmpty()) {
        rSubmitCommand.add("--partition");          rSubmitCommand.add(mPartition);
        }
        // 获得指令
        return String.join(" ", rSubmitCommand);
    }
    @Override protected @Nullable String getBatchSubmitCommand(List<String> aCommands, IHasIOFiles aIOFiles) {
        // 批量提交的指令，会将多个指令打包成一个单一的指令，用于突破服务器的用户任务数上限
        // 原理为，使用 sbatch 提交一个 srun，sbatch 指定需要的节点数，srun 指定使用和节点数一样的核心，并指定每个节点一个核心，
        // srun 执行打包的脚本，在脚本中根据具体的 SLURM_PROCID 来获取自己的分组，使用 srun 来执行子任务，
        // 由于有些 slurm 会出现不同节点间相互抢资源的情况，还需要使用一个 mSplitNodeScriptPath 脚本来获取具体的实际节点列表，并手动指定使用的节点
        // 这里简单起见，都认为所有任务都是只支持 mpi 模式运行的，不考虑可以串行执行的情况
        
        // 计算需要的节点数目，需要预留一个核给 srun 本身
        int tNcmdsPerNode = (mMaxTaskNumPerNode-1) / mTaskNum;
        if (tNcmdsPerNode == 0) throw new RuntimeException(); // 理论不应该出现，因为会在父类打包的时候就避免这个问题
        int tNodeNum = MathEX.Code.divup(aCommands.size(), tNcmdsPerNode);
        tNcmdsPerNode = MathEX.Code.divup(aCommands.size(), tNodeNum);
        // 获取每个节点具体分配的任务数，保证分配均匀
        int[] tNcmdsPerNodeList = new int[tNodeNum];
        Arrays.fill(tNcmdsPerNodeList, tNcmdsPerNode);
        int tExceed = tNcmdsPerNode*tNodeNum - aCommands.size();
        for (int i = 0; i < tExceed; ++i) --tNcmdsPerNodeList[i];
        // 构建提交脚本文本
        List<String> rScriptLines = new ArrayList<>();
        rScriptLines.add("#!/bin/bash");
        // 获取节点列表
        rScriptLines.add(String.format("IFS=\" \" read -ra nodelist <<< \"$(./%s \"$SLURM_JOB_NODELIST\")\"", mSplitNodeScriptPath));
        rScriptLines.add("");
        // 不同节点分配不同任务
        rScriptLines.add("nodeid=${SLURM_PROCID}");
        rScriptLines.add("case \"${nodeid}\" in");
        int idx = 0;
        for (int tNodeID = 0; tNodeID < tNodeNum; ++tNodeID) {
            rScriptLines.add(String.format("%d)", tNodeID));
            for (int i = 0; i < tNcmdsPerNodeList[tNodeID]; ++i) {
                // 使用 srun 而不是 yhrun 来让这个脚本兼容 SLURM 系统的超算
                rScriptLines.add(String.format("  srun --nodelist \"${nodelist[%d]}\" --nodes 1 --ntasks %d --ntasks-per-node %d %s &", tNodeID, mTaskNum, mTaskNum, aCommands.get(idx)));
                ++idx;
            }
            rScriptLines.add("  ;;");
        }
        rScriptLines.add("*)");
        rScriptLines.add("  echo \"INVALID NODE ID: ${nodeid}\"");
        rScriptLines.add("  ;;");
        rScriptLines.add("esac");
        rScriptLines.add("wait"); // 等待上面的任务执行完成
        // 最后增加一个空行
        rScriptLines.add("");
        // 保存脚本，使用随机名称即可
        String tBatchedScriptPath = mBatchedScriptDir+UT.Code.randID()+".sh";
        try {
            UT.IO.write(tBatchedScriptPath, rScriptLines);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        // 附加脚本文件到输入文件
        aIOFiles.putIFiles(IFILE_KEY, tBatchedScriptPath);
        // 依旧设置输出文件为默认文件，在这里附加下载
        String tOutFilePath = toRealOutFilePath(defaultOutFilePath());
        aIOFiles.putOFiles(OUTPUT_FILE_KEY, tOutFilePath);
        // 组装提交的指令
        List<String> rRunCommand = new ArrayList<>();
        rRunCommand.add("srun");
        rRunCommand.add("--ntasks");            rRunCommand.add(String.valueOf(tNodeNum)); // 任务数为节点数
        rRunCommand.add("--ntasks-per-node");   rRunCommand.add("1"); // 每节点任务为 1
        rRunCommand.add("--wait");              rRunCommand.add("1000000");
        rRunCommand.add("bash");                rRunCommand.add(tBatchedScriptPath); // 使用 bash 指令来执行脚本，避免权限问题
        // 组装提交指令
        List<String> rSubmitCommand = new ArrayList<>();
        rSubmitCommand.add(String.format("echo -e '#!/bin/bash\\n%s'", String.join(" ", rRunCommand)));
        rSubmitCommand.add("|");
        rSubmitCommand.add("sbatch");
        rSubmitCommand.add("--nodes");              rSubmitCommand.add(String.valueOf(tNodeNum)); // 节点数依旧是节点数
        rSubmitCommand.add("--job-name");           rSubmitCommand.add(mUniqueJobName);
        rSubmitCommand.add("--output");             rSubmitCommand.add(tOutFilePath);
        if (mPartition != null && !mPartition.isEmpty()) {
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
        // 不合法时会输出不合法的结果，由于这个操作是长期的且是允许的，因此可以通过 noConsoleOutput 抑制
        if (!noConsoleOutput()) {
            System.err.println("WARNING: getRunningJobIDsFromSystem Fail, it is usually the network issue, the output from the remote server:");
            for (String tLine : tLines) if (!tLine.equals("END")) System.err.println(tLine);
        }
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
        // 失败时输出失败的结果，由于这个操作是允许的，因此可以通过 noConsoleOutput 抑制
        if (!noConsoleOutput()) {
            System.err.println("WARNING: cancelJobFromSystem Fail, the output from the remote server:");
            for (String tLine : tLines) if (!tLine.equals("END")) System.err.println(tLine);
        }
        return false;
    }
    /** 取消这个对象提交的所有任务，子类重写来优化避免重复的提交指令 */
    @Override protected synchronized void cancelThis_() {
        mEXE.system(String.format("scancel --name %s", mUniqueJobName));
    }
}
