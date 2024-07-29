package jse.code;

import com.google.common.collect.ImmutableList;
import jse.Main;
import jse.math.MathEX;
import jse.system.BashSystemExecutor;
import jse.system.ISystemExecutor;
import jse.system.PowerShellSystemExecutor;
import org.jetbrains.annotations.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static jse.code.CS.ZL_STR;

/**
 * 系统相关操作，包括 jar 包的位置，执行系统指令，判断系统类型，获取工作目录等；
 * 现在变为独立的类而不是放在 {@link CS} 或 {@link UT} 中
 * @author liqa
 */
public class OS {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(WORKING_DIR);
        }
    }
    
    public final static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    public final static boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    public final static String NO_LOG_LINUX = "/dev/null";
    public final static String NO_LOG_WIN = "NUL";
    public final static String NO_LOG = IS_WINDOWS ? NO_LOG_WIN : NO_LOG_LINUX;
    
    public final static ISystemExecutor EXEC;
    public final static String JAR_PATH;
    public final static String JAR_DIR;
    public final static String USER_HOME;
    public final static String USER_HOME_DIR;
    public final static String WORKING_DIR;
    final static Path WORKING_DIR_PATH;
    
    static {
        InitHelper.INITIALIZED = true;
        // 先获取 user.home
        USER_HOME = System.getProperty("user.home"); // user.home 这里统一认为 user.home 就是绝对路径
        USER_HOME_DIR = UT.IO.toInternalValidDir(USER_HOME);
        // 然后通过执行指令来初始化 WORKING_DIR；
        // 这种写法可以保证有最大的兼容性，即使后续 EXE 可能非法（不是所有平台都有 bash）
        String wd = USER_HOME;
        Process tProcess = null;
        try {tProcess = Runtime.getRuntime().exec(IS_WINDOWS ? "cmd /c cd" : "pwd");}
        catch (IOException ignored) {}
        if (tProcess != null) {
            // 注意自 jdk18 起，默认 charset 统一成了 UTF-8，因此对于 cmd 需要手动指定为 GBK
            try (BufferedReader tReader = new BufferedReader(new InputStreamReader(tProcess.getInputStream(), IS_WINDOWS ? "GBK" : Charset.defaultCharset().name()))) {
                tProcess.waitFor();
                wd = tReader.readLine().trim();
            } catch (Exception ignored) {
            } finally {
                tProcess.destroy();
            }
        }
        // 全局修改工作目录为正确的目录
        System.setProperty("user.dir", wd);
        // jse 内部使用的 dir 需要末尾增加 `/`
        WORKING_DIR = UT.IO.toInternalValidDir(wd);
        WORKING_DIR_PATH = Paths.get(WORKING_DIR);
        
        // 获取此 jar 的路径
        // 默认这样获取而不是通过 System.getProperty("java.class.path")，为了避免此属性有多个 jar
        Path tJarPath;
        try {
            URI tJarURI = CS.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            tJarPath = WORKING_DIR_PATH.resolve(Paths.get(tJarURI));
        } catch (Exception e) {
            // 在 linux 中这个路径可能是相对路径，为了避免库安装错误这里统一获取一下绝对路径
            // 现在应该可以随意使用 UT.IO 而不会循环初始化
            tJarPath = UT.IO.toAbsolutePath_(System.getProperty("java.class.path"));
        }
        JAR_PATH = tJarPath.toString();
        Path tJarDirPath = tJarPath.getParent();
        String tJarDir = tJarDirPath==null ? "" : tJarDirPath.toString();
        tJarDir = UT.IO.toInternalValidDir(tJarDir);
        JAR_DIR = tJarDir;
        // 创建默认 EXE，无内部线程池，windows 下使用 powershell 而 linux 下使用 bash 统一指令；
        // 这种选择可以保证指令使用统一，即使这些终端不一定所有平台都有
        EXEC = IS_WINDOWS ? new PowerShellSystemExecutor() : new BashSystemExecutor();
        // 在程序结束时关闭 EXE
        Main.addGlobalAutoCloseable(EXEC);
    }
    
    
    /** 更加易用的获取环境变量的接口 */
    public static @Nullable String env(String aName) {
        try {return System.getenv(aName);}
        catch (Throwable ignored) {} // 获取失败不抛出错误，在 jse 中获取环境变量都是非必要的
        return null;
    }
    @Contract("_, !null -> !null")
    public static String env(String aName, String aDefault) {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : tEnv;
    }
    public static String @NotNull[] envPath(String aName) {
        return envPath(aName, ZL_STR);
    }
    @Contract("_, !null -> !null")
    public static String[] envPath(String aName, String[] aDefault) {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : tEnv.trim().split(File.pathSeparator);
    }
    public static int envI(String aName, int aDefault) throws NumberFormatException {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : Integer.parseInt(tEnv);
    }
    public static double envD(String aName, double aDefault) throws NumberFormatException {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : Double.parseDouble(tEnv);
    }
    public static boolean envZ(String aName, boolean aDefault) throws NumberFormatException {
        String tEnv = env(aName);
        if (tEnv == null) return aDefault;
        tEnv = tEnv.toLowerCase();
        switch(tEnv) {
        case "true" : case "t": case "on" : case "yes": case "1": {return true ;}
        case "false": case "f": case "off": case "no" : case "0": {return false;}
        default: {throw new NumberFormatException("For input string: \""+tEnv+"\"");}
        }
    }
    
    /** 提供这些接口方便外部调用使用 */
    @VisibleForTesting public static int system(String aCommand) {return EXEC.system(aCommand);}
    @VisibleForTesting public static int system(String aCommand, String aOutFilePath) {return EXEC.system(aCommand, aOutFilePath);}
    @VisibleForTesting public static Future<Integer> submitSystem(String aCommand) {return EXEC.submitSystem(aCommand);}
    @VisibleForTesting public static Future<Integer> submitSystem(String aCommand, String aOutFilePath) {return EXEC.submitSystem(aCommand, aOutFilePath);}
    @VisibleForTesting public static List<String> system_str(String aCommand) {return EXEC.system_str(aCommand);}
    @VisibleForTesting public static Future<List<String>> submitSystem_str(String aCommand) {return EXEC.submitSystem_str(aCommand);}
    /** @deprecated use {@link #EXEC} */  @VisibleForTesting @Deprecated public static ISystemExecutor exec() {return EXEC;}
    
    
    /** SLURM 相关，使用子类分割避免冗余初始化 */
    public static class Slurm {
        public final static boolean IS_SLURM;
        public final static boolean IS_SRUN;
        public final static int PROCID;
        public final static int NTASKS;
        public final static int CORES_PER_NODE;
        public final static int CORES_PER_TASK;
        public final static int MAX_STEP_COUNT;
        public final static int STEP_ID;
        public final static int JOB_ID;
        public final static String JOB_NAME;
        public final static int NODEID;
        public final static String NODENAME;
        public final static List<String> NODE_LIST;
        public final static ResourcesManager RESOURCES_MANAGER;
        
        
        /** slurm 的资源分配器，所有 slurm 资源申请统一先走这层防止资源分配失败 */
        public final static class ResourcesManager {
            private final Map<String, Integer> mAllResources;
            private int mRestStepCount;
            private boolean mWarning = false;
            private ResourcesManager() {
                mAllResources = new HashMap<>();
                for (String tNode : NODE_LIST) mAllResources.put(tNode, CORES_PER_NODE);
                // 如果是 srun 环境下则需要给自身预留资源，如果是没有设置 CORES_PER_TASK 的一般情况则输出警告，
                // 因为一般情况下可以直接使用 sbatch 提交运行 groovy 脚本（不会被杀掉）
                if (IS_SRUN) {
                    if (CORES_PER_TASK <= 0) {
                        mWarning = true;
                        mAllResources.computeIfPresent(NODE_LIST.get(0), (node, cores) -> cores-1);
                    } else {
                        mAllResources.computeIfPresent(NODE_LIST.get(0), (node, cores) -> cores-CORES_PER_TASK);
                    }
                }
                // 移除自身消耗的作业步，预留 100 步给外部使用
                mRestStepCount = MAX_STEP_COUNT - 100;
            }
            /** 根据需要的核心数来分配资源，返回节点和对应的可用核心数 */
            public synchronized @Nullable Resource assignResource(final int aTaskNum) {
                if (mWarning) {
                    mWarning = false; // 只警告一次
                    UT.Code.warning("It is UNSAFE to run jse using `srun` and then assignResource. You should run jse directly in the `sbatch` script.\n" +
                                    "If this jse needs special resources, please specified core number required by `-c`.");
                }
                // 计算至少需要的节点数目
                int tMinNodes = MathEX.Code.divup(aTaskNum, CORES_PER_NODE);
                // 超过节点数则直接分配失败，返回 null
                if (tMinNodes > NODE_LIST.size()) return null;
                // 一直增加节点数直到分配成功，slurm 不能过于灵活的分配，对于给定节点数分配情况是“唯一”的
                int tNodes = tMinNodes;
                while (tNodes <= NODE_LIST.size()) {
                    // 获取 slurm 下的分配结果
                    int tRestTasks = aTaskNum;
                    int tNTasksPerNode = aTaskNum / tNodes;
                    int[] tNTasksPerNodeList = new int[tNodes];
                    for (int i = 0; i < tNodes; ++i) {
                        tNTasksPerNodeList[i] = tNTasksPerNode;
                        tRestTasks -= tNTasksPerNode;
                    }
                    if (tRestTasks > 0) {
                        ++tNTasksPerNode;
                        int tIdx = 0;
                        while (tRestTasks > 0) {
                            ++tNTasksPerNodeList[tIdx]; --tRestTasks;
                            ++tIdx;
                        }
                    }
                    // 从所有资源中分配，获取节点列表
                    List<String> rNodeList = new ArrayList<>();
                    int tIdx = 0;
                    for (Map.Entry<String, Integer> tEntry : mAllResources.entrySet()) {
                        if (tEntry.getValue() >= tNTasksPerNodeList[tIdx]) {
                            rNodeList.add(tEntry.getKey());
                            ++tIdx;
                            if (tIdx == tNodes) break;
                        }
                    }
                    // 成功分配则减去资源，输出结果
                    if (rNodeList.size() == tNodes) {
                        tIdx = 0;
                        for (String tNode : rNodeList) {
                            final int tThisNodeNTasks = tNTasksPerNodeList[tIdx];
                            mAllResources.computeIfPresent(tNode, (node, cores) -> cores-tThisNodeNTasks);
                            ++tIdx;
                        }
                        return new Resource(rNodeList, tNodes, aTaskNum, tNTasksPerNode, tNTasksPerNodeList);
                    }
                    // 否则增加需要的节点数，重新分配
                    ++tNodes;
                }
                // 分配失败，返回 null
                return null;
            }
            
            /** 不指定时会分配目前最大核数的节点，保持不跨节点 */
            public synchronized @Nullable Resource assignResource() {
                if (mWarning) {
                    mWarning = false; // 只警告一次
                    UT.Code.warning("It is UNSAFE to run jse using `srun` and then assignResource. You should run jse directly in the `sbatch` script.\n" +
                                    "If this jse needs special resources, please specified core number required by `-c`.");
                }
                int tNTasks = 0;
                String tNode = null;
                for (Map.Entry<String, Integer> tEntry : mAllResources.entrySet()) {
                    int tRestCores = tEntry.getValue();
                    if (tRestCores > tNTasks) {
                        tNTasks = tRestCores;
                        tNode = tEntry.getKey();
                        if (tNTasks == CORES_PER_NODE) break;
                    }
                }
                // 成功分配则减去资源，输出结果
                if (tNTasks > 0) {
                    mAllResources.put(tNode, 0);
                    return new Resource(Collections.singletonList(tNode), 1, tNTasks, tNTasks, new int[] {tNTasks});
                }
                // 分配失败，返回 null
                return null;
            }
            
            /** 返回分配的资源 */
            public synchronized void returnResource(Resource aResource) {
                int tIdx = 0;
                for (String tNode : aResource.nodelist) {
                    final int tThisNodeNTasks = aResource.ntasksPerNodeList[tIdx];
                    mAllResources.computeIfPresent(tNode, (node, cores) -> cores+tThisNodeNTasks);
                    ++tIdx;
                }
            }
            
            /** 获取提交作业步的指令 */
            public synchronized @Nullable String creatJobStep(Resource aResource, String aCommand) {
                // 超过作业步限制直接禁止分配
                --mRestStepCount;
                if (mRestStepCount < 0) {mRestStepCount = 0; return null;}
                
                List<String> rCommand = new ArrayList<>();
                rCommand.add("srun");
                rCommand.add("--nodelist");         rCommand.add(String.join(",", aResource.nodelist));
                rCommand.add("--nodes");            rCommand.add(String.valueOf(aResource.nodes));
                rCommand.add("--ntasks");           rCommand.add(String.valueOf(aResource.ntasks));
                rCommand.add("--ntasks-per-node");  rCommand.add(String.valueOf(aResource.ntasksPerNode));
                if (CORES_PER_TASK > 0) {
                rCommand.add("--cpus-per-task");    rCommand.add(String.valueOf(1));
                }
                rCommand.add(aCommand);
                return String.join(" ", rCommand);
            }
        }
        /** slurm 的资源结构，限制很多已经尽力 */
        public final static class Resource {
            public final @Unmodifiable List<String> nodelist;
            public final int nodes;
            public final int ntasks;
            public final int ntasksPerNode;
            public final int[] ntasksPerNodeList; // internal usage
            
            private Resource(@Unmodifiable List<String> nodelist, int nodes, int ntasks, int ntasksPerNode, int[] ntasksPerNodeList) {
                this.nodelist = nodelist;
                this.nodes = nodes;
                this.ntasks = ntasks;
                this.ntasksPerNode = ntasksPerNode;
                this.ntasksPerNodeList = ntasksPerNodeList;
            }
        }
        
        
        static {
            // 获取节点列表，如果失败则不是 slurm
            String tRawNodeList = OS.env("SLURM_NODELIST");
            NODE_LIST = tRawNodeList==null ? null : ImmutableList.copyOf(UT.Text.splitNodeList(tRawNodeList));
            IS_SLURM = NODE_LIST != null;
            // 是 slurm 则从环境变量中读取后续参数，否则使用默认非法值
            if (IS_SLURM) {
                // 获取 ID
                PROCID = OS.envI("SLURM_PROCID", -1);
                // 获取作业 id 和作业名
                JOB_ID = OS.envI("SLURM_JOB_ID", -1);
                JOB_NAME = OS.env("SLURM_JOB_NAME");
                // 获取任务总数
                NTASKS = OS.envI("SLURM_NTASKS", -1);
                // 获取对应的 node id 和节点名
                NODEID = OS.envI("SLURM_NODEID", -1);
                NODENAME = OS.env("SLURMD_NODENAME");
                // 获取每任务的核心数，可能为 null；现在统一在没有时为 -1，这里已经用不到这个参数了
                CORES_PER_TASK = OS.envI("SLURM_CPUS_PER_TASK", -1);
                // 获取 srun step 的 id，只有开启 srun 后才有值
                STEP_ID = OS.envI("SLURM_STEP_ID", -1);
                IS_SRUN = STEP_ID >= 0;
                
                // 获取每节点的核心数
                String tRawCoresPerNode = OS.env("SLURM_JOB_CPUS_PER_NODE");
                if (tRawCoresPerNode == null) {
                    CORES_PER_NODE = -1;
                } else {
                    // 目前仅支持单一的 CoresPerNode，对于有多个的情况会选取最小值
                    Pattern tPattern = Pattern.compile("(\\d+)(\\([^)]+\\))?"); // 匹配整数部分和可选的括号部分
                    Matcher tMatcher = tPattern.matcher(tRawCoresPerNode);
                    int tCoresPerNode = -1;
                    while (tMatcher.find()) {
                        int tResult = Integer.parseInt(tMatcher.group(1));
                        tCoresPerNode = (tCoresPerNode < 0) ? tResult : Math.min(tCoresPerNode, tResult);
                    }
                    CORES_PER_NODE = tCoresPerNode;
                }
                
                // 单个任务的作业步限制，不能获取，默认为此值
                MAX_STEP_COUNT = 40000;
                
                RESOURCES_MANAGER = new ResourcesManager();
            } else {
                IS_SRUN = false;
                PROCID = -1;
                STEP_ID = -1;
                JOB_ID = -1;
                JOB_NAME = null;
                NTASKS = -1;
                NODEID = -1;
                NODENAME = null;
                CORES_PER_NODE = -1;
                CORES_PER_TASK = -1;
                MAX_STEP_COUNT = -1;
                RESOURCES_MANAGER = null;
            }
        }
    }
}
