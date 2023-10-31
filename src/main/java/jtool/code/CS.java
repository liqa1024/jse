package jtool.code;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import jtool.Main;
import jtool.atom.IXYZ;
import jtool.code.collection.AbstractCollections;
import jtool.iofile.IIOFiles;
import jtool.iofile.IOFiles;
import jtool.math.MathEX;
import jtool.parallel.CompletedFuture;
import jtool.system.*;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author liqa
 * <p> Class containing useful Constants </p>
 */
public class CS {
    /** a Random generator so I don't need to instantiate a new one all the time. */
    public final static Random RNGSUS = new Random(), RANDOM = RNGSUS;
    public final static int MAX_SEED = 2147483647;
    
    public final static Object NULL = null;
    
    public final static IXYZ BOX_ONE  = new IXYZ() {
        @Override public double x() {return 1.0;}
        @Override public double y() {return 1.0;}
        @Override public double z() {return 1.0;}
        /** print */
        @Override public String toString() {return "(1.0, 1.0, 1.0)";}
    };
    public final static IXYZ BOX_ZERO = new IXYZ() {
        @Override public double x() {return 0.0;}
        @Override public double y() {return 0.0;}
        @Override public double z() {return 0.0;}
        /** print */
        @Override public String toString() {return "(0.0, 0.0, 0.0)";}
    };
    
    public final static String WORKING_DIR = ".temp/%n/";
    
    public final static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    public final static String NO_LOG_LINUX = "/dev/null";
    public final static String NO_LOG_WIN = "NUL";
    public final static String NO_LOG = IS_WINDOWS ? NO_LOG_WIN : NO_LOG_LINUX;
    
    public final static int DEFAULT_THREAD_NUM = Runtime.getRuntime().availableProcessors();
    public final static int DEFAULT_CACHE_SIZE = DEFAULT_THREAD_NUM + 2;
    
    
    /** MathEX stuffs */
    public enum SliceType {ALL}
    public final static SliceType ALL = SliceType.ALL;
    
    /** Sleep time stuff, ms */
    public final static long
          INTERNAL_SLEEP_TIME = 1
        , SYNC_SLEEP_TIME = 10
        , SYNC_SLEEP_TIME_2 = 20
        , FILE_SYSTEM_SLEEP_TIME = 100
        , FILE_SYSTEM_SLEEP_TIME_2 = 200
        , SSH_SLEEP_TIME = 500
        , SSH_SLEEP_TIME_2 = 1000
        , FILE_SYSTEM_TIMEOUT = 10000
        ;
    
    
    /** AtomData stuffs */
    public final static String[] ATOM_DATA_KEYS_VELOCITY = {"vx", "vy", "vz"};
    public final static String[] ATOM_DATA_KEYS_XYZ = {"x", "y", "z"};
    public final static String[] ATOM_DATA_KEYS_XYZID = {"x", "y", "z", "id"};
    public final static String[] ATOM_DATA_KEYS_TYPE_XYZ = {"type", "x", "y", "z"};
    public final static String[] ATOM_DATA_KEYS_ID_TYPE_XYZ = {"id", "type", "x", "y", "z"};
    public final static String[] ALL_ATOM_DATA_KEYS = {"id", "type", "x", "y", "z", "vx", "vy", "vz"};
    public final static String[] STD_ATOM_DATA_KEYS = ATOM_DATA_KEYS_ID_TYPE_XYZ; // 标准 AtomData 包含信息格式为 id type x y z
    public final static int XYZ_X_COL = 0, XYZ_Y_COL = 1, XYZ_Z_COL = 2;
    public final static int XYZID_X_COL = 0, XYZID_Y_COL = 1, XYZID_Z_COL = 2, XYZID_ID_COL = 3;
    public final static int TYPE_XYZ_TYPE_COL = 0, TYPE_XYZ_X_COL = 1, TYPE_XYZ_Y_COL = 2, TYPE_XYZ_Z_COL = 3;
    public final static int ALL_ID_COL = 0, ALL_TYPE_COL = 1, ALL_X_COL = 2, ALL_Y_COL = 3, ALL_Z_COL = 4, ALL_VX_COL = 5, ALL_VY_COL = 6, ALL_VZ_COL = 7;
    public final static int STD_ID_COL = 0, STD_TYPE_COL = 1, STD_X_COL = 2, STD_Y_COL = 3, STD_Z_COL = 4;
    public final static int STD_VX_COL = 0, STD_VY_COL = 1, STD_VZ_COL = 2;
    
    public final static double R_NEAREST_MUL = 1.5;
    
    /** const arrays */
    public final static String[] ZL_STR = new String[0];
    public final static Object[] ZL_OBJ = new Object[0];
    public final static double[][] ZL_MAT = new double[0][];
    public final static double[]   ZL_VEC = new double[0];
    public final static int[]      ZL_INT = new int[0];
    public final static byte[]    ZL_BYTE = new byte[0];
    
    /** IOFiles Keys */
    public final static String
          OUTPUT_FILE_KEY = "<out>"
        , INFILE_SELF_KEY = "<self>"
        , OFILE_KEY = "<o>"
        , IFILE_KEY = "<i>"
        ;
    public enum SettingType {REMOVE, KEEP}
    public final static SettingType
          REMOVE = SettingType.REMOVE
        , KEEP = SettingType.KEEP
        ;
    
    /** Patterns */
    public final static Pattern BLANKS = Pattern.compile("\\s+");
    public final static Pattern COMMA = Pattern.compile(",");
    
    /** Relative atomic mass in this project */
    public final static Map<String, Double> MASS = (new ImmutableMap.Builder<String, Double>())
        .put("H" , 1.00794)
        .put("He", 4.002602)
        .put("Li", 6.941)
        .put("Be", 9.012182)
        .put("B" , 10.811)
        .put("C" , 12.0107)
        .put("N" , 14.0067)
        .put("O" , 15.9994)
        .put("F" , 18.998403)
        .put("Ne", 20.1797)
        .put("Na", 22.98976)
        .put("Mg", 24.3050)
        .put("Al", 26.98153)
        .put("Si", 28.0855)
        .put("P" , 30.97696)
        .put("S" , 32.065)
        .put("Cl", 35.453)
        .put("Ar", 39.948)
        .put("K" , 39.0983)
        .put("Ca", 40.078)
        .put("Sc", 44.95591)
        .put("Ti", 47.867)
        .put("V" , 50.9415)
        .put("Cr", 51.9962)
        .put("Mn", 54.93804)
        .put("Fe", 55.845)
        .put("Co", 58.93319)
        .put("Ni", 58.6934)
        .put("Cu", 63.546)
        .put("Zn", 65.38)
        .put("Ga", 69.723)
        .put("Ge", 72.64)
        .put("As", 74.92160)
        .put("Se", 78.96)
        .put("Br", 79.904)
        .put("Kr", 83.798)
        .put("Rb", 85.4678)
        .put("Sr", 87.62)
        .put("Y" , 88.90585)
        .put("Zr", 91.224)
        .put("Nb", 92.90638)
        .put("Mo", 95.96)
        .put("Tc", 98.0)
        .put("Ru", 101.07)
        .put("Rh", 102.9055)
        .put("Pd", 106.42)
        .put("Ag", 107.8682)
        .put("Cd", 112.441)
        .put("In", 114.818)
        .put("Sn", 118.710)
        .put("Sb", 121.760)
        .put("Te", 127.60)
        .put("I" , 126.9044)
        .put("Xe", 131.293)
        .put("Cs", 132.9054)
        .put("Ba", 137.327)
        .put("La", 138.9054)
        .put("Ce", 140.116)
        .put("Pr", 140.9076)
        .put("Nd", 144.242)
        .put("Pm", 145.0)
        .put("Sm", 150.36)
        .put("Eu", 151.964)
        .put("Gd", 157.25)
        .put("Tb", 158.9253)
        .put("Dy", 162.500)
        .put("Ho", 164.9303)
        .put("Er", 167.259)
        .put("Tm", 168.9342)
        .put("Yb", 173.054)
        .put("Lu", 174.9668)
        .put("Hf", 178.49)
        .put("Ta", 180.9478)
        .put("W" , 183.84)
        .put("Re", 186.207)
        .put("Os", 190.23)
        .put("Ir", 192.217)
        .put("Pt", 195.084)
        .put("Au", 196.9665)
        .put("Hg", 200.59)
        .put("Tl", 20.3833)
        .put("Pb", 207.2)
        .put("Bi", 208.9804)
        .put("Po", 210.0)
        .put("At", 210.0)
        .put("Rn", 220.0)
        .put("Fr", 223.0)
        .put("Ra", 226.0)
        .put("Ac", 227.0)
        .put("Th", 232.0380)
        .put("Pa", 231.0358)
        .put("U" , 238.0289)
        .put("Np", 237.0)
        .put("Pu", 244.0)
        .put("Am", 243.0)
        .put("Cm", 247.0)
        .put("Bk", 247.0)
        .put("Cf", 251.0)
        .put("Es", 252.0)
        .put("Fm", 257.0)
        .put("Md", 258.0)
        .put("No", 259.0)
        .put("Lr", 262.0)
        .put("Rf", 261.0)
        .put("Db", 262.0)
        .put("Sg", 266.0)
        .put("Bh", 264.0)
        .put("Hs", 277.0)
        .put("Mt", 268.0)
        .put("Ds", 271.0)
        .put("Rg", 272.0)
        .put("Cn", 285.0)
        .put("Uut",284.0)
        .put("Fl", 289.0)
        .put("UUp",288.0)
        .put("Lv", 292.0)
        .put("Uus",-1.0)
        .put("UUo",294.0)
        .build();
    /** Boltzmann constant */
    public final static double K_B = 0.0000861733262; // eV / K
    
    /** SystemExecutor Stuffs */
    public final static IIOFiles EPT_IOF = IOFiles.immutable();
    public final static IFutureJob SUC_FUTURE = new CompletedFutureJob(0);
    public final static IFutureJob ERR_FUTURE = new CompletedFutureJob(-1);
    public final static Future<List<Integer>> ERR_FUTURES = new CompletedFuture<>(Collections.singletonList(-1));
    public final static Future<List<String>> EPT_STR_FUTURE = new CompletedFuture<>(AbstractCollections.zl());
    public final static PrintStream NUL_PRINT_STREAM = new PrintStream(new OutputStream() {public void write(int b) {/**/}});
    
    /** 内部运行相关，使用子类分割避免冗余初始化 */
    public static class Exec {
        public final static ISystemExecutor EXE;
        public final static String JAR_PATH;
        static {
            // 先手动加载 UT，会自动重新设置工作目录，保证路径的正确性
            UT.IO.init();
            // 获取此 jar 的路径
            JAR_PATH = System.getProperty("java.class.path");
            // 创建默认 EXE，无内部线程池，windows 下使用 powershell 统一指令
            EXE = IS_WINDOWS ? new PowerShellSystemExecutor() : new LocalSystemExecutor();
            // 在程序结束时关闭 EXE
            Main.addGlobalAutoCloseable(EXE);
        }
    }
    
    /** SLURM 相关，使用子类分割避免冗余初始化 */
    public static class Slurm {
        public final static boolean IS_SLURM;
        public final static int PROCID;
        public final static int NTASKS;
        public final static int CORES_PER_NODE;
        public final static int CORES_PER_TASK;
        public final static int MAX_STEP_COUNT;
        public final static List<String> NODE_LIST;
        public final static ResourcesManager RESOURCES_MANAGER;
        
        
        /** slurm 的资源分配器，所有 slurm 资源申请统一先走这层防止资源分配失败 */
        public final static class ResourcesManager {
            private final Map<String, Integer> mAllResources;
            private int mRestStepCount;
            private ResourcesManager() {
                mAllResources = new HashMap<>();
                for (String tNode : NODE_LIST) mAllResources.put(tNode, CORES_PER_NODE);
                // 第一个节点有一个核已经分配给主进程
                mAllResources.computeIfPresent(NODE_LIST.get(0), (node, cores) -> cores-CORES_PER_TASK);
                // 移除自身消耗的作业步，预留 100 步给外部使用
                mRestStepCount = MAX_STEP_COUNT - 100;
            }
            /** 根据需要的核心数来分配资源，返回节点和对应的可用核心数 */
            public synchronized @Nullable Resource assignResource(final int aTaskNum) {
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
                rCommand.add("--cpus-per-task");    rCommand.add(String.valueOf(1));
                rCommand.add(aCommand);
                return String.join(" ", rCommand);
            }
        }
        /** slurm 的资源结构，限制很多已经尽力 */
        public final static class Resource {
            private final @Unmodifiable List<String> nodelist;
            private final int nodes;
            private final int ntasks;
            private final int ntasksPerNode;
            private final int[] ntasksPerNodeList; // internal usage
            
            private Resource(@Unmodifiable List<String> nodelist, int nodes, int ntasks, int ntasksPerNode, int[] ntasksPerNodeList) {
                this.nodelist = nodelist;
                this.nodes = nodes;
                this.ntasks = ntasks;
                this.ntasksPerNode = ntasksPerNode;
                this.ntasksPerNodeList = ntasksPerNodeList;
            }
        }
        
        
        static {
            // 获取 ID，如果失败则不是 slurm
            int tId = -1;
            try {tId = Integer.parseInt(System.getenv("SLURM_PROCID"));} catch (Exception ignored) {}
            PROCID = tId;
            IS_SLURM = PROCID >= 0;
            // 是 slurm 则从环境变量中读取后续参数，否则使用默认非法值
            if (IS_SLURM) {
                // 获取任务总数
                NTASKS = Integer.parseInt(System.getenv("SLURM_NTASKS"));
                
                // 获取每节点的核心数
                String tRawCoresPerNode = System.getenv("SLURM_JOB_CPUS_PER_NODE");
                // 目前仅支持单一的 CoresPerNode，对于有多个的情况会选取最小值
                Pattern tPattern = Pattern.compile("(\\d+)(\\([^)]+\\))?"); // 匹配整数部分和可选的括号部分
                Matcher tMatcher = tPattern.matcher(tRawCoresPerNode);
                int tCoresPerNode = -1;
                while (tMatcher.find()) {
                    int tResult = Integer.parseInt(tMatcher.group(1));
                    tCoresPerNode = (tCoresPerNode < 0) ? tResult : Math.min(tCoresPerNode, tResult);
                }
                CORES_PER_NODE = tCoresPerNode;
                
                // 获取每任务的核心数，可能为 null
                String tRawCoresPerTask = System.getenv("SLURM_CPUS_PER_TASK");
                CORES_PER_TASK = tRawCoresPerTask==null ? 1 : Integer.parseInt(tRawCoresPerTask);
                
                // 单个任务的作业步限制，不能获取，默认为此值
                MAX_STEP_COUNT = 40000;
                
                // 获取节点列表
                NODE_LIST = ImmutableList.copyOf(UT.Texts.splitNodeList(System.getenv("SLURM_NODELIST")));
                
                RESOURCES_MANAGER = new ResourcesManager();
            } else {
                NTASKS = -1;
                CORES_PER_NODE = -1;
                CORES_PER_TASK = -1;
                MAX_STEP_COUNT = -1;
                NODE_LIST = null;
                RESOURCES_MANAGER = null;
            }
        }
    }
}
