package jse.clib;

import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jse.code.CS.VERSION;
import static jse.code.OS.JAR_DIR;
import static jse.code.OS.JAVA_HOME;

/**
 * 直接调用 c 中 {@code <dlfcn.h>} 中的 {@code dlopen()}
 * 来加载动态库的类，最大增加 jni 库的兼容性。
 * <p>
 * 采用 jep 中完全一致的实现，即只提升库到 global 的水平，不做其他操作；
 * 对于 unix 以外的系统不会做任何操作，如果这样操作后依旧无法正常工作，
 * 那么依旧可能需要使用 {@code LD_PRELOAD} 来加载动态库。
 * @author liqa
 */
public class Dlfcn {
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link Dlfcn} 相关的 JNI 库是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link Dlfcn} 相关的 JNI 库 */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            if (!INITIALIZED) String.valueOf(LIB_PATH);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义构建 dlfcnjni 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_DLFCN");
        
        /**
         * 自定义构建 dlfcnjni 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_C_COMPILER_DLFCN} 来设置
         */
        public static @Nullable String CMAKE_C_COMPILER = OS.env("JSE_CMAKE_C_COMPILER_DLFCN", jse.code.Conf.CMAKE_C_COMPILER);
        /**
         * 自定义构建 dlfcnjni 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_C_FLAGS_DLFCN} 来设置
         */
        public static @Nullable String CMAKE_C_FLAGS    = OS.env("JSE_CMAKE_C_FLAGS_DLFCN"   , jse.code.Conf.CMAKE_C_FLAGS);
        
        /**
         * 重定向 dlfcnjni 动态库的路径，用于自定义编译这个库的过程，或者重新实现 dlfcnjni 的接口
         * <p>
         * 也可使用环境变量 {@code JSE_REDIRECT_DLFCN_LIB} 来设置
         */
        public static @Nullable String REDIRECT_DLFCN_LIB = OS.env("JSE_REDIRECT_DLFCN_LIB");
    }
    
    /** 当前 {@link Dlfcn} JNI 库所在的文件夹路径，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR = JAR_DIR+"dlfcn/" + UT.Code.uniqueID(OS.OS_NAME, JAVA_HOME, VERSION, Conf.CMAKE_C_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_SETTING) + "/";
    /** 当前 {@link Dlfcn} JNI 库的路径 */
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jse_clib_Dlfcn.c"
        , "jse_clib_Dlfcn.h"
    };
    
    static {
        InitHelper.INITIALIZED = true;
        LIB_PATH = new JNIUtil.LibBuilder("dlfcnjni", "DLFCN", LIB_DIR, Conf.CMAKE_SETTING)
            .setSrc("dlfcn", SRC_NAME)
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS)
            .setRedirectLibPath(Conf.REDIRECT_DLFCN_LIB)
            .setCmakeLineOp(null)
            .get();
        // 设置库路径，这里直接使用 System.load
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    /**
     * 调用 调用 c 中 {@code <dlfcn.h>} 中的 {@code dlopen()} 来加载动态库的类。
     * 这里简单处理，直接在 c 中实现这个打开部分，不把所有接口都实现；使用和 jep
     * 同样的实现方式，即只提升库到 global 的水平，不做其他操作
     * @param aPath 需要打开的库路径
     */
    public native static void dlopen(String aPath);
}
