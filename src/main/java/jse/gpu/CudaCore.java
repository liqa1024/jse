package jse.gpu;

import jse.Main;
import jse.clib.Compiler;
import jse.clib.JNIUtil;
import jse.clib.MiMalloc;
import jse.clib.NVCC;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.Conf.VERSION_MASK;
import static jse.code.OS.JAR_DIR;
import static jse.code.OS.JAVA_HOME;

/**
 * 针对 cuda gpu 的通用支持，主要是创建 gpu 数组，调用自定义的核函数，获取结果等
 * <p>
 * 此类提供核心的 jni 接口转发，其他类进行包装后提供 OOP 式使用
 *
 * @author liqa
 */
@ApiStatus.Experimental
public class CudaCore {
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link CudaCore} 相关的 JNI 库是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link CudaCore} 相关的 JNI 库 */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            if (!INITIALIZED) String.valueOf(LIB_PATH);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义构建 cudacore 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_CUDA");
        
        /**
         * 自定义构建 cudacore 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CXX_COMPILER_CUDA} 来设置
         */
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_CUDA", jse.code.Conf.CMAKE_CXX_COMPILER);
        /**
         * 自定义构建 cudacore 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CXX_FLAGS_CUDA} 来设置
         */
        public static @Nullable String CMAKE_CXX_FLAGS = OS.env("JSE_CMAKE_CXX_FLAGS_CUDA", jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 对于 cudacore，是否使用 {@link MiMalloc} 来加速内存分配，
         * 这对于 java 数组和 cuda 数组的转换会很有效
         * <p>
         * 也可使用环境变量 {@code JSE_USE_MIMALLOC_CUDA} 来设置
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_CUDA", jse.code.Conf.USE_MIMALLOC);
    }
    
    /** 当前 {@link CudaCore} JNI 库所在的文件夹路径，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR = JAR_DIR+"gpu/cuda/" + UT.Code.uniqueID(OS.OS_NAME, Compiler.EXE_PATH, NVCC.EXE_PATH, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    /** 当前 {@link CudaCore} JNI 库的路径 */
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jse_gpu_CudaCore.cpp"
        , "jse_gpu_CudaCore.h"
        , "jse_gpu_FloatCudaPointer.cpp"
        , "jse_gpu_FloatCudaPointer.h"
        , "cudacore_util.h"
    };
    
    static {
        InitHelper.INITIALIZED = true;
        // 依赖 NVCC
        NVCC.InitHelper.init();
        if (!NVCC.VALID) {
            NVCC.printInfo();
            throw new RuntimeException("No CUDA support.");
        }
        LIB_PATH = new JNIUtil.LibBuilder("cudacore", "CUDA", LIB_DIR, Conf.CMAKE_SETTING)
            .setSrc("cudacore", SRC_NAME)
            .setEnvChecker(NVCC::printInfo) // 在这里输出 nvcc 信息，保证只在第一次构建时输出一次；可能存在和 cmake 检测不一致的问题
            .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setUseMiMalloc(Conf.USE_MIMALLOC)
            .get();
        // 设置库路径，这里直接使用 System.load
        System.load(IO.toAbsolutePath(LIB_PATH));
        // 在 JVM 关闭时总是调用 cudaDeviceSynchronize，避免错误被吞掉
        Main.addGlobalAutoCloseable(CudaCore::cudaDeviceSynchronize);
    }
    public static native void cudaDeviceSynchronize() throws CudaException;
    
    static native long cudaMalloc(int aCount) throws CudaException;
    static native void cudaFree(long aPtr) throws CudaException;
    static native void cudaMemcpyH2D(long aSrc, long rDest, int aCount) throws CudaException;
    static native void cudaMemcpyD2H(long aSrc, long rDest, int aCount) throws CudaException;
    static native void cudaMemcpyD2D(long aSrc, long rDest, int aCount) throws CudaException;
}
