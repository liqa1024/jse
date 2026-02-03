package jse.clib;

import jse.code.IO;
import org.jetbrains.annotations.Nullable;

import static jse.code.OS.EXEC;
import static jse.code.OS.IS_WINDOWS;

/**
 * 部分库需要的 NVCC 支持，注意区分 {@link jse.gpu.CudaCore}
 * 这里只进行系统库的检测和警告，不负责编译 jni
 * @author liqa
 */
public class NVCC {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link NVCC} 相关的参数是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link NVCC} 相关的参数 */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(EXE_PATH);
        }
    }
    
    /** 自动检测到的 nvcc 可执行路径 */
    public final static String EXE_PATH;
    /** 拼接后可以执行的命令，对于 windows 和 linux 专门适配 */
    public final static String EXE_CMD;
    /** 是否有自动检测到 nvcc */
    public final static boolean VALID;
    
    
    private static boolean FIRST_PRINT = true;
    /** 由于总是需要路径来确定库路径，提供一个延迟打印接口，仅第一次构建时打印提示；由于是可选的，下载相关的提示也延迟输出 */
    public static void printInfo() {
        if (!FIRST_PRINT) return;
        FIRST_PRINT = false;
        if (EXE_PATH!=null) {
            System.out.printf(IO.Text.cyan("CUDA INFO:")+" Use NVCC in %s\n", EXE_PATH);
        } else {
            System.err.println(IO.Text.red("CUDA INIT ERROR:")+" No NVCC found,");
            System.err.println("  You can download from: "+IO.Text.underline("https://developer.nvidia.com/cuda-downloads"));
        }
    }
    
    private static @Nullable String getExePath_() {
        // 检测环境变量的 nvcc
        EXEC.setNoSTDOutput().setNoERROutput();
        boolean tNoCuda = EXEC.system("nvcc --version") != 0;
        EXEC.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCuda) return null;
        String tCudaPath;
        if (IS_WINDOWS) {
            tCudaPath = EXEC.system_str("(Get-Command nvcc).Path").get(0);
        } else {
            tCudaPath = EXEC.system_str("which nvcc").get(0);
        }
        return IO.exists(tCudaPath) ? tCudaPath : null;
    }
    
    static {
        InitHelper.INITIALIZED = true;
        try {EXE_PATH = getExePath_();}
        catch (Exception e) {throw new RuntimeException(e);}
        if (EXE_PATH!=null) {
            EXE_CMD = (IS_WINDOWS?"& \"":"\"") + EXE_PATH + "\"";
            VALID = true;
        } else {
            EXE_CMD = null;
            VALID = false;
        }
    }
}
