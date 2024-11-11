package jse.clib;

import jse.code.OS;
import jse.code.SP;
import jse.code.UT;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

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
    
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(HOME);
        }
    }
    
    public final static class Conf {
        /** 手动设置 torch 的 home 目录，可以设置为自己的 python 环境中安装的 torch，从而避免重复安装 */
        public static @Nullable String HOME = OS.env("JSE_TORCH_HOME");
    }
    
    public final static String VERSION = "2.5.1";
    
    public final static String HOME;
    public final static String LIB_DIR;
    public final static String[] LIB_PATHS;
    /** torch 不需要 llib_path 这种路径，而是直接通过 cmake 的支持，在 cmake 中设置一下路径即可自动链接 */
    public final static String CMAKE_DIR;
    /** 顺便表明了链接顺序 */
    private final static String[] LIB_NAMES = IS_WINDOWS ? new String[]{"asmjit", "libiomp5md", "fbgemm", "c10", "uv", "torch_cpu", "torch"} : new String[]{"torch"};
    
    
    private static void initTorch_() throws IOException {
        // 首先获取源码路径，这里直接检测是否是 torch-$VERSION 开头
        UT.IO.makeDir(PYTHON_PKG_DIR);
        String[] tList = UT.IO.list(PYTHON_PKG_DIR);
        boolean tHasTorchPkg = false;
        for (String tName : tList) if (tName.startsWith("torch-"+VERSION)) {
            tHasTorchPkg = true; break;
        }
        // 如果没有 ase 包则直接下载，指定版本 VERSION 避免因为更新造成的问题
        if (!tHasTorchPkg) {
            System.out.printf("TORCH INIT INFO: No torch package in %s, downloading...\n", PYTHON_PKG_DIR);
            SP.Python.downloadPackage("torch=="+VERSION);
            System.out.println("TORCH INIT INFO: torch package downloading finished");
        }
        // 安装 torch 包
        System.out.println("TORCH INIT INFO: Installing torch from package...");
        SP.Python.installPackage("torch=="+VERSION);
        System.out.println("TORCH INIT INFO: torch Installing finished");
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
