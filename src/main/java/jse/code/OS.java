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
 * @see ISystemExecutor ISystemExecutor: 独立的系统命令执行器
 * @see IO IO: 文件操作工具类
 * @see OS.Slurm OS.Slurm: SLURM 针对对系统的下相关功能
 * @author liqa
 */
public class OS {
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link OS} 相关的静态常量是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link OS} 相关的静态常量值 */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(WORKING_DIR);
        }
    }
    
    /** 当前平台是否是 windows */
    public final static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    /** 当前平台是否是 macos */
    public final static boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    /** linux 上不进行输出的重定向路径名，{@code "/dev/null"} */
    public final static String NO_LOG_LINUX = "/dev/null";
    /** windows 上不进行输出的重定向路径名，{@code "NUL"} */
    public final static String NO_LOG_WIN = "NUL";
    /** 不进行输出的重定向路径名，windows 下为 {@code "NUL"}，linux 下为 {@code "/dev/null"} */
    public final static String NO_LOG = IS_WINDOWS ? NO_LOG_WIN : NO_LOG_LINUX;
    
    /** {@code System.getProperty("java.home")} 获取到的原始值，即运行此 jse 对应的 jdk 路径 */
    public final static String JAVA_HOME;
    /** {@link #JAVA_HOME} 内部合法化文件夹后的路径，可以直接拼接文件名 */
    public final static String JAVA_HOME_DIR;
    
    /** {@link OS} 内部执行系统指令使用的全局的 {@link ISystemExecutor}，在 linux 上为 {@link BashSystemExecutor}，windows 上为 {@link PowerShellSystemExecutor} */
    public final static ISystemExecutor EXEC;
    /** 此 jse 核心 jar 文件的路径 */
    public final static String JAR_PATH;
    /** 此 jse 核心 jar 文件所在的文件夹，已经内部合法化，可以直接拼接文件名 */
    public final static String JAR_DIR;
    /** {@code System.getProperty("user.home")} 获取到的原始值，即此用户的用户目录 */
    public final static String USER_HOME;
    /** {@link #USER_HOME} 内部合法化文件夹后的路径，可以直接拼接文件名 */
    public final static String USER_HOME_DIR;
    /**
     * 运行 jse 的目录（工作目录），已经内部合法化，可以直接拼接文件名
     * <p>
     * 在通过 matlab 调用 jar 使用时，获取到的工作目录会出错，
     * 因此这里会统一执行系统命令获取真实的工作目录。
     */
    public final static String WORKING_DIR;
    /** {@link Path} 版本的 {@link #WORKING_DIR} */
    final static Path WORKING_DIR_PATH;
    
    static {
        InitHelper.INITIALIZED = true;
        // 获取 java.home
        JAVA_HOME = System.getProperty("java.home"); // 这里统一认为 java.home 就是绝对路径
        JAVA_HOME_DIR = IO.toInternalValidDir(JAVA_HOME);
        // 先获取 user.home
        USER_HOME = System.getProperty("user.home"); // 这里统一认为 user.home 就是绝对路径
        USER_HOME_DIR = IO.toInternalValidDir(USER_HOME);
        // 然后通过执行指令来初始化 WORKING_DIR；
        // 这种写法可以保证有最大的兼容性，即使后续 EXE 可能非法（不是所有平台都有 bash）
        String wd = USER_HOME;
        Process tProcess = null;
        try {tProcess = new ProcessBuilder(IS_WINDOWS ? new String[]{"cmd", "/c", "cd"} : new String[]{"pwd"}).start();}
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
        WORKING_DIR = IO.toInternalValidDir(wd);
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
            tJarPath = IO.toAbsolutePath_(System.getProperty("java.class.path"));
        }
        JAR_PATH = tJarPath.toString();
        Path tJarDirPath = tJarPath.getParent();
        String tJarDir = tJarDirPath==null ? "" : tJarDirPath.toString();
        tJarDir = IO.toInternalValidDir(tJarDir);
        JAR_DIR = tJarDir;
        // 创建默认 EXE，无内部线程池，windows 下使用 powershell 而 linux 下使用 bash 统一指令；
        // 这种选择可以保证指令使用统一，即使这些终端不一定所有平台都有
        EXEC = IS_WINDOWS ? new PowerShellSystemExecutor() : new BashSystemExecutor();
        // 在程序结束时关闭 EXE
        Main.addGlobalAutoCloseable(EXEC);
    }
    
    
    /**
     * 获取指定名称的环境变量值，即 {@link System#getenv(String)}
     * 的包装方法，但是不再会抛出错误
     * @param aName 需要获取的环境变量名称
     * @return 环境变量值，如果不存在或者获取失败则会返回 {@code null}
     */
    public static @Nullable String env(String aName) {
        try {return System.getenv(aName);}
        catch (Throwable ignored) {} // 获取失败不抛出错误，在 jse 中获取环境变量都是非必要的
        return null;
    }
    /**
     * 获取指定名称的环境变量值，即 {@link System#getenv(String)}
     * 的包装方法，但是不再会抛出错误，并且在没有获取到的时候返回默认值
     * @param aName 需要获取的环境变量名称
     * @param aDefault 不存在值或者获取失败时使用的默认值
     * @return 环境变量值，如果不存在或者获取失败则会返回 aDefault
     */
    @Contract("_, !null -> !null")
    public static String env(String aName, String aDefault) {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : tEnv;
    }
    
    /**
     * 从环境变量获取路径值，认为路径使用 {@link File#pathSeparator} 分隔
     * @param aName 需要获取的路径环境变量名称
     * @return 路径数组，如果不存在或者获取失败则返回空数组
     */
    public static String @NotNull[] envPath(String aName) {
        return envPath(aName, ZL_STR);
    }
    /**
     * 从环境变量获取路径值，认为路径使用 {@link File#pathSeparator}
     * 分隔，并且在没有获取到的时候返回默认值
     * @param aName 需要获取的路径环境变量名称
     * @param aDefault 不存在值或者获取失败时使用的默认值
     * @return 路径数组，如果不存在或者获取失败则返回 aDefault
     */
    @Contract("_, !null -> !null")
    public static String[] envPath(String aName, String[] aDefault) {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : tEnv.trim().split(File.pathSeparator);
    }
    /**
     * 获取一个整数环境变量值，并且在没有获取到的时候返回默认值
     * @param aName 需要获取的整数环境变量名称
     * @param aDefault 不存在值或者获取失败时使用的默认值
     * @return 整数值，如果不存在或者获取失败则返回 aDefault
     * @throws NumberFormatException 如果环境变量值不能解析为整数
     */
    public static int envI(String aName, int aDefault) throws NumberFormatException {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : Integer.parseInt(tEnv);
    }
    /**
     * 获取一个浮点数环境变量值，并且在没有获取到的时候返回默认值
     * @param aName 需要获取的浮点数环境变量名称
     * @param aDefault 不存在值或者获取失败时使用的默认值
     * @return 浮点数值，如果不存在或者获取失败则返回 aDefault
     * @throws NumberFormatException 如果环境变量值不能解析为浮点数
     */
    public static double envD(String aName, double aDefault) throws NumberFormatException {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : Double.parseDouble(tEnv);
    }
    /**
     * 获取一个布尔环境变量值，并且在没有获取到的时候返回默认值
     * <p>
     * 其中 {@code "true", "t", "on", "yes", "1"} 都会认为是
     * {@code true}，{@code "false", "f", "off", "no", "0"} 都会认为是
     * {@code false}，不区分大小写
     * @param aName 需要获取的布尔环境变量名称
     * @param aDefault 不存在值或者获取失败时使用的默认值
     * @return 布尔值，如果不存在或者获取失败则返回 aDefault
     * @throws NumberFormatException 如果环境变量值不符合上述格式要求
     */
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
    
    /// ISystemExecutor stuffs
    /**
     * 通过终端执行一个系统命令，在 linux 上使用 bash，windows 上使用 powershell
     * @param aCommand 需要执行的命令文本
     * @return 指令的退出码
     * @see ISystemExecutor#system(String)
     */
    @VisibleForTesting public static int system(String aCommand) {return EXEC.system(aCommand);}
    /**
     * 通过终端执行一个系统命令，在 linux 上使用 bash，windows 上使用 powershell
     * @param aCommand 需要执行的命令文本
     * @param aOutFilePath 重定向输出文件路径
     * @return 指令的退出码
     * @see ISystemExecutor#system(String, String)
     */
    @VisibleForTesting public static int system(String aCommand, String aOutFilePath) {return EXEC.system(aCommand, aOutFilePath);}
    /**
     * 提交一个后台系统命令并运行，在 linux 上使用 bash，windows 上使用 powershell
     * @param aCommand 需要执行的命令文本
     * @return 获取指令退出码的异步计算结果 {@link Future}
     * @see ISystemExecutor#submitSystem(String)
     * @see Future
     */
    @VisibleForTesting public static Future<Integer> submitSystem(String aCommand) {return EXEC.submitSystem(aCommand);}
    /**
     * 提交一个后台系统命令并运行，在 linux 上使用 bash，windows 上使用 powershell
     * @param aCommand 需要执行的命令文本
     * @param aOutFilePath 重定向输出文件路径
     * @return 获取指令退出码的异步计算结果 {@link Future}
     * @see ISystemExecutor#submitSystem(String, String)
     * @see Future
     */
    @VisibleForTesting public static Future<Integer> submitSystem(String aCommand, String aOutFilePath) {return EXEC.submitSystem(aCommand, aOutFilePath);}
    /**
     * 通过终端执行一个系统命令，并获取输出到 {@link List}，在 linux 上使用 bash，windows 上使用 powershell
     * @param aCommand 需要执行的命令文本
     * @return 运行得到的输出，按行分隔
     * @see ISystemExecutor#system_str(String)
     */
    @VisibleForTesting public static List<String> system_str(String aCommand) {return EXEC.system_str(aCommand);}
    /**
     * 提交一个后台系统命令并运行，并获取输出到 {@link List}，在 linux 上使用 bash，windows 上使用 powershell
     * @param aCommand 需要执行的命令文本
     * @return 获取指令输出的异步计算结果 {@link Future}
     * @see ISystemExecutor#submitSystem_str(String)
     * @see Future
     */
    @VisibleForTesting public static Future<List<String>> submitSystem_str(String aCommand) {return EXEC.submitSystem_str(aCommand);}
    /** @deprecated use {@link #EXEC} */
    @VisibleForTesting @Deprecated public static ISystemExecutor exec() {return EXEC;}
    
    
    /**
     * 针对使用 SLURM 系统管理任务的相关功能，主要包含对 SLURM 中常用的环境变量的统一获取，自动资源分配，获取节点列表等
     * @see jse.system.SRUNSystemExecutor SRUNSystemExecutor: 使用 srun 执行命令的执行器
     * @author liqa
     */
    public static class Slurm {
        /** 是否是 SLURM 系统 */
        public final static boolean IS_SLURM;
        /** 是否是通过 srun 运行，如果已经是 srun 运行则通常不能再调用 srun */
        public final static boolean IS_SRUN;
        /** 在 srun 环境中当前进程的 id，对应环境变量 {@code SLURM_PROCID} */
        public final static int PROCID;
        /** 在 srun 环境中的所有进程总数，对应环境变量 {@code SLURM_NTASKS} */
        public final static int NTASKS;
        /** 当前分配到的资源中的每节点核心数，对于每个节点有不同核心数的情况下，为了兼容性会取最小值，对应环境变量 {@code SLURM_JOB_CPUS_PER_NODE} */
        public final static int CORES_PER_NODE;
        /** 分配给每个进程的核心数，没有设置时为 {@code -1}，对应环境变量 {@code SLURM_CPUS_PER_TASK} */
        public final static int CORES_PER_TASK;
        /** 单个任务的作业步限制，不能获取，默认为 40000 */
        public final static int MAX_STEP_COUNT;
        /** 在 srun 环境中当前的 step id，对应环境变量 {@code SLURM_STEP_ID} */
        public final static int STEP_ID;
        /** 当前的任务 id，对应环境变量 {@code SLURM_JOB_ID} */
        public final static int JOB_ID;
        /** 当前的任务名称，对应环境变量 {@code SLURM_JOB_NAME} */
        public final static String JOB_NAME;
        /** 当前任务所在节点 id，对应环境变量 {@code SLURM_NODEID} */
        public final static int NODEID;
        /** 当前任务所在节点名称，对应环境变量 {@code SLURMD_NODENAME} */
        public final static String NODENAME;
        /** 分配到的节点资源组成的列表，对应环境变量 {@code SLURM_NODELIST} */
        public final static List<String> NODE_LIST;
        /** 用来获取 SLURM 资源的全局资源管理器 */
        public final static ResourcesManager RESOURCES_MANAGER;
        
        
        /**
         * SLURM 资源管理器实现，这个资源管理器更改了 SLURM 分配资源的默认行为，
         * 给定需要的进程数后，会优先分配到单个节点上，从而避免跨节点的通讯
         * <p>
         * 通过 {@link ResourcesManager#assignResource(int)} 来尝试分配资源，使用
         * {@link ResourcesManager#creatJobStep(Resource, String)} 来为资源创建对应的提交任务命令，
         * 并使用 {@link ResourcesManager#returnResource(Resource)} 来归还资源
         */
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
            /**
             * 根据需要的核心数来分配资源，会尝试优先分配到一个节点上
             * @param aTaskNum 需要的任务数（进程数）
             * @return 分配得到的资源，包含分配得到的节点以及每节点的任务数，如果分配失败则返回 {@code null}
             */
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
            
            /**
             * 不指定时任务数的会分配资源，会使用目前最大核数的节点，并保持不跨节点
             * @return 分配得到的资源，包含分配得到的节点以及每节点的任务数，如果分配失败则返回 {@code null}
             */
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
            
            /**
             * 归还分配的资源
             * @param aResource 需要归还的资源
             */
            public synchronized void returnResource(@NotNull Resource aResource) {
                int tIdx = 0;
                for (String tNode : aResource.nodelist) {
                    final int tThisNodeNTasks = aResource.ntasksPerNodeList[tIdx];
                    mAllResources.computeIfPresent(tNode, (node, cores) -> cores+tThisNodeNTasks);
                    ++tIdx;
                }
            }
            
            /**
             * 通过资源和命令来创建提交任务的 srun 指令
             * @param aResource 需要使用的资源
             * @param aCommand 需要提交的命令
             * @return 使用当前资源的 srun 命令，如果创建失败则返回 {@code null}
             */
            public synchronized @Nullable String creatJobStep(@NotNull Resource aResource, String aCommand) {
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
                rCommand.add("--overlap"); // 这样就不强制要求环境变量设置 SLURM_OVERLAP=1 了
                rCommand.add(aCommand);
                return String.join(" ", rCommand);
            }
        }
        /** slurm 的资源结构 */
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
            NODE_LIST = tRawNodeList==null ? null : ImmutableList.copyOf(IO.Text.splitNodeList(tRawNodeList));
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
