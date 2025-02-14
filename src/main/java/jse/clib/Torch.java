package jse.clib;

import jse.code.OS;
import jse.code.SP;
import jse.code.UT;
import org.jetbrains.annotations.Nullable;

import static jse.code.Conf.LIB_NAME_IN;
import static jse.code.OS.IS_WINDOWS;
import static jse.code.SP.PYTHON_LIB_DIR;
import static jse.code.SP.PYTHON_PKG_DIR;

/**
 * pytorch 库的初始化，这里使用 pip 来安装，
 * 可以同时安装 python 接口和 c++ 接口
 * <p>
 * 目前只考虑 cpu 版本，对于 python 部分应该不影响
 * @author liqa
 */
public class Torch {
    private Torch() {}
    
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link Torch} 库是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link Torch} 库 */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(HOME);
        }
    }
    
    public final static class Conf {
        /**
         * 手动设置 torch 的 home 目录，可以设置为自己的 python 环境中安装的 torch，从而避免重复安装
         * <p>
         * 也可使用环境变量 {@code JSE_TORCH_HOME} 来设置
         */
        public static @Nullable String HOME = OS.env("JSE_TORCH_HOME");
        /** 指定此值来指定下载的 torch 版本 */
        public static String INDEX_URL = "https://download.pytorch.org/whl/cpu";
    }
    
    /** 当前 {@link Torch} 所使用的版本号 */
    public final static String VERSION = "2.5.1";
    
    /** 当前 {@link Torch} 库的根目录，结尾一定存在 {@code '/'} */
    public final static String HOME;
    /** 当前 {@link Torch} 库所在的文件夹路径，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR;
    /** 当前 {@link Torch} 库的动态库路径，除了本体还会存在依赖，按照需要链接的顺序排序 */
    public final static String[] LIB_PATHS;
    /** 当前 {@link Torch} 库需要链接的依赖目录，torch 不需要 llib_path 这种路径，而是直接通过 cmake 的支持，在 cmake 中设置一下路径即可自动链接 */
    public final static String CMAKE_DIR;
    /** 顺便表明了链接顺序 */
    private final static String[] LIB_NAMES = IS_WINDOWS ? new String[]{"asmjit", "libiomp5md", "fbgemm", "c10", "uv", "torch_cpu", "torch"} : new String[]{"torch"};
    
    
    private static void initTorch_() throws Exception {
        // 首先获取源码路径，这里直接检测是否是 torch-$VERSION 开头
        UT.IO.makeDir(PYTHON_PKG_DIR);
        String[] tList = UT.IO.list(PYTHON_PKG_DIR);
        boolean tHasTorchPkg = false;
        for (String tName : tList) if (tName.startsWith("torch-"+VERSION)) {
            tHasTorchPkg = true; break;
        }
        // 这里先尝试直接安装，直接通过 pip 的安装来进行检测其余是否匹配
        if (tHasTorchPkg) {
            // 安装 torch 包
            System.out.println("TORCH INIT INFO: Installing torch from package...");
            int tExitCode = SP.Python.installPackage("torch=="+VERSION);
            if (tExitCode != 0) {
                System.err.println("TORCH INIT WARNING: torch install Failed: " + tExitCode);
                System.err.println("    This may be caused by no correct version.");
            } else {
                System.out.println("TORCH INIT INFO: torch install finished");
                return;
            }
        }
        // 尝试下载包
        System.out.printf("TORCH INIT INFO: No correct torch package in %s, downloading...\n", PYTHON_PKG_DIR);
        int tExitCode = SP.Python.downloadPackage("torch=="+VERSION, null, null, Conf.INDEX_URL);
        if (tExitCode != 0) {
            throw new Exception("TORCH INIT ERROR: torch download Failed: " + tExitCode);
        }
        System.out.println("TORCH INIT INFO: torch package downloading finished");
        // 再次安装 torch 包
        System.out.println("TORCH INIT INFO: Re-installing torch from package...");
        tExitCode = SP.Python.installPackage("torch=="+VERSION);
        if (tExitCode != 0) {
            throw new Exception("TORCH INIT ERROR: torch install Failed: " + tExitCode);
        }
        System.out.println("TORCH INIT INFO: torch install finished");
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        // 如果设置了自定义的 TORCH_HOME，并且改为使用此处的 torch；
        // 这里简单处理，不去检测是否合理（或者后续有检测则统一检测方式）
        HOME = Conf.HOME!=null ? UT.IO.toInternalValidDir(Conf.HOME) : PYTHON_LIB_DIR+"torch/";
        LIB_DIR = HOME+"lib/";
        CMAKE_DIR = HOME+"share/cmake/Torch/";
        
        // 如果不存在 torch 目录需要重新通过 pip 来安装
        if (!UT.IO.isDir(HOME)) {
            System.out.println("TORCH INIT INFO: torch not found. Reinstalling...");
            try {initTorch_();}
            catch (Exception e) {throw new RuntimeException(e);}
        }
        // 先统一查找一下 torch 的 c++ 库路径
        LIB_PATHS = new String[LIB_NAMES.length];
        for (int i = 0; i < LIB_NAMES.length; ++i) {
            LIB_PATHS[i] = LIB_DIR + LIB_NAME_IN(LIB_DIR, LIB_NAMES[i]);
        }
        // 这里顺便加载 torch 的 c++ 库，简单测试对 python 的使用似乎没有影响
        for (String tLibPath : LIB_PATHS) {
            System.load(UT.IO.toAbsolutePath(tLibPath));
        }
    }
    
}
