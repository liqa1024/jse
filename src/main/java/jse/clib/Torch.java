package jse.clib;

import jse.code.IO;
import jse.code.OS;
import jse.code.SP;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.nio.charset.Charset;

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
@Deprecated
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
        public static @Nullable String INDEX_URL = "https://download.pytorch.org/whl/cpu";
    }
    
    /** 当前 {@link Torch} 所使用的版本号 */
    public final static String VERSION = "2.7.1";
    
    /** 当前 {@link Torch} 库的根目录，结尾一定存在 {@code '/'} */
    public final static String HOME;
    /** 当前 {@link Torch} 库所在的文件夹路径，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR;
    /** 当前 {@link Torch} 库的动态库路径，除了本体还会存在依赖，按照需要链接的顺序排序 */
    public final static String[] LIB_PATHS;
    /** 当前 {@link Torch} 库需要链接的依赖目录，torch 不需要 llib_path 这种路径，而是直接通过 cmake 的支持，在 cmake 中设置一下路径即可自动链接 */
    public final static String CMAKE_DIR;
    
    /// 记录需要链接的动态库名称，windows 需要手动按顺序指定对应的所有依赖
    private final static String[] LIB_NAMES = {"torch"};
    private final static String[] FULL_LIB_NAMES = {"asmjit", "libiomp5md", "fbgemm", "c10", "uv", "torch_cpu", "torch"};
    private final static String[] FULL_LIB_NAMES_CUDA = {
        "asmjit", "libiomp5md", "fbgemm", "c10", "uv", "nvJitLink_120_0", "cudart64_12", "cufft64_11", "cudnn64_9", "cupti64_2025.1.0",
        "cublasLt64_12", "cublas64_12", "c10_cuda", "cusparse64_12", "cusolver64_11", "torch_cpu", "torch_cuda", "torch"
    };
    
    
    private static void initTorch_() throws Exception {
        // 首先获取源码路径，这里直接检测是否是 torch-$VERSION 开头
        IO.makeDir(PYTHON_PKG_DIR);
        String[] tList = IO.list(PYTHON_PKG_DIR);
        boolean tHasTorchPkg = false;
        for (String tName : tList) if (tName.startsWith("torch-"+VERSION)) {
            tHasTorchPkg = true; break;
        }
        // 这里先尝试直接安装，直接通过 pip 的安装来进行检测其余是否匹配
        if (tHasTorchPkg) {
            // 安装 torch 包
            System.out.println(IO.Text.green("TORCH INIT INFO:")+" Installing torch from package...");
            int tExitCode = SP.Python.installPackage("torch=="+VERSION);
            if (tExitCode != 0) {
                System.err.println(IO.Text.yellow("TORCH INIT WARNING:")+" torch install Failed: " + tExitCode);
                System.err.println("    This may be caused by no correct version.");
            } else {
                System.out.println(IO.Text.green("TORCH INIT INFO:")+" torch install finished");
                return;
            }
        }
        // 尝试下载包
        System.out.printf(IO.Text.green("TORCH INIT INFO:")+" No correct torch package in %s, you can:\n", PYTHON_PKG_DIR);
        System.out.println("  - Set the environment variable `JSE_TORCH_HOME` to torch path, like: path/to/Python/Python312/Lib/site-packages/torch");
        System.out.printf( "  - Move the correct torch whl (%s) to %s, url: https://download.pytorch.org/whl/torch/\n", VERSION, PYTHON_PKG_DIR);
        System.out.println("  - Auto download torch by jse.");
        System.out.println(IO.Text.yellow("Download torch? (Y/n)"));
        BufferedReader tReader = IO.toReader(System.in, Charset.defaultCharset());
        String tLine = tReader.readLine();
        while (true) {
            if (tLine.equalsIgnoreCase("n")) {
                throw new Exception("No correct torch package in "+PYTHON_PKG_DIR);
            }
            if (tLine.isEmpty() || tLine.equalsIgnoreCase("y")) {
                break;
            }
            System.out.println(IO.Text.yellow("Download torch? (Y/n)"));
        }
        System.out.println(IO.Text.green("TORCH INIT INFO:")+" Downloading torch...");
        int tExitCode = SP.Python.downloadPackage("torch=="+VERSION, null, null, Conf.INDEX_URL);
        if (tExitCode != 0) {
            throw new Exception("torch download Failed: " + tExitCode);
        }
        System.out.println(IO.Text.green("TORCH INIT INFO:")+" torch package downloading finished");
        // 再次安装 torch 包
        System.out.println(IO.Text.green("TORCH INIT INFO:")+" Re-installing torch from package...");
        tExitCode = SP.Python.installPackage("torch=="+VERSION);
        if (tExitCode != 0) {
            throw new Exception("torch install Failed: " + tExitCode);
        }
        System.out.println(IO.Text.green("TORCH INIT INFO:")+" torch install finished");
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        // 如果设置了自定义的 TORCH_HOME，并且改为使用此处的 torch；
        // 这里简单处理，不去检测是否合理（或者后续有检测则统一检测方式）
        HOME = Conf.HOME!=null ? IO.toInternalValidDir(Conf.HOME) : PYTHON_LIB_DIR+"torch/";
        LIB_DIR = HOME+"lib/";
        CMAKE_DIR = HOME+"share/cmake/Torch/";
        
        // 如果不存在 torch 目录需要重新通过 pip 来安装
        if (!IO.isDir(HOME)) {
            System.out.println(IO.Text.green("TORCH INIT INFO:")+" torch not found. Reinstalling...");
            try {initTorch_();}
            catch (Exception e) {throw new RuntimeException(e);}
        }
        // 先统一查找一下 torch 的 c++ 库路径，自动检测 torch 是否是 cuda
        String[] tLibNames;
        if (!IS_WINDOWS) {
            tLibNames = LIB_NAMES;
        } else {
            String tCudaName = LIB_NAME_IN(LIB_DIR, "torch_cuda");
            tLibNames = tCudaName!=null ? FULL_LIB_NAMES_CUDA : FULL_LIB_NAMES;
        }
        LIB_PATHS = new String[tLibNames.length];
        for (int i = 0; i < tLibNames.length; ++i) {
            LIB_PATHS[i] = LIB_DIR + LIB_NAME_IN(LIB_DIR, tLibNames[i]);
        }
        // 这里顺便加载 torch 的 c++ 库，简单测试对 python 的使用似乎没有影响
        for (String tLibPath : LIB_PATHS) {
            System.load(IO.toAbsolutePath(tLibPath));
        }
    }
    
}
