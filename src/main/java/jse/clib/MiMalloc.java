package jse.clib;

import jse.code.CS;
import jse.code.OS;
import jse.code.UT;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jse.code.OS.JAR_DIR;
import static jse.code.Conf.*;

/**
 * 其他 jni 库或者此项目需要依赖的 c 库；
 * 一种加速 c 中 malloc 和 free 的库。
 * @see <a href="https://github.com/microsoft/mimalloc"> microsoft/mimalloc </a>
 * @author liqa
 */
public class MiMalloc {
    private MiMalloc() {}
    
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link MiMalloc} 库是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link MiMalloc} 库 */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(HOME);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义构建 mimalloc 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new LinkedHashMap<>();
        
        /**
         * 自定义构建 mimalloc 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_C_COMPILER_MIMALLOC} 来设置
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_MIMALLOC"  , jse.code.Conf.CMAKE_C_COMPILER);
        /**
         * 自定义构建 mimalloc 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CXX_COMPILER_MIMALLOC} 来设置
         */
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_MIMALLOC", jse.code.Conf.CMAKE_CXX_COMPILER);
        /**
         * 自定义构建 mimalloc 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_C_FLAGS_MIMALLOC} 来设置
         */
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_MIMALLOC"     , jse.code.Conf.CMAKE_C_FLAGS);
        /**
         * 自定义构建 mimalloc 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CXX_FLAGS_MIMALLOC} 来设置
         */
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_MIMALLOC"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 重定向 mimalloc 动态库的路径，主要用于作为重定向的 mpijni, lmpjni 等库的依赖导入
         * <p>
         * 也可使用环境变量 {@code JSE_REDIRECT_MIMALLOC_LIB} 来设置
         */
        public static @Nullable String REDIRECT_MIMALLOC_LIB = OS.env("JSE_REDIRECT_MIMALLOC_LIB");
        /**
         * 重定向 mimalloc 用于链接的库的路径，主要用于作为重定向的 mpijni, lmpjni 等库的依赖导入
         * <p>
         * 也可使用环境变量 {@code JSE_REDIRECT_MIMALLOC_LLIB} 来设置
         */
        public static @Nullable String REDIRECT_MIMALLOC_LLIB = OS.env("JSE_REDIRECT_MIMALLOC_LLIB", REDIRECT_MIMALLOC_LIB);
    }
    
    
    /** 当前 {@link MiMalloc} 所使用的版本号 */
    public final static String VERSION = "2.1.7";
    
    /** 当前 {@link MiMalloc} 库的根目录，结尾一定存在 {@code '/'} */
    public final static String HOME = JAR_DIR+"mimalloc/" + UT.Code.uniqueID(CS.VERSION, MiMalloc.VERSION, Conf.CMAKE_C_COMPILER, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    /** 当前 {@link MiMalloc} 库所在的文件夹路径，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR = HOME+"lib/";
    /** 当前 {@link MiMalloc} 库的 include 目录路径，结尾一定存在 {@code '/'} */
    public final static String INCLUDE_DIR = HOME+"include/";
    /** 当前 {@link MiMalloc} 库的动态库路径 */
    public final static String LIB_PATH;
    /** 当前 {@link MiMalloc} 库用于链接的库的路径，对于 linux 则和 {@link #LIB_PATH} 一致 */
    public final static String LLIB_PATH;
    
    static {
        InitHelper.INITIALIZED = true;
        // 这样来统一增加 mimalloc 需要的默认额外设置，
        // 先添加额外设置，从而可以通过 Conf.CMAKE_SETTING 来覆盖这些设置
        Map<String, String> rCmakeSetting = new LinkedHashMap<>();
        rCmakeSetting.put("MI_BUILD_SHARED",  "ON");
        rCmakeSetting.put("MI_BUILD_STATIC",  "OFF");
        rCmakeSetting.put("MI_BUILD_OBJECT",  "OFF");
        rCmakeSetting.put("MI_BUILD_TESTS",   "OFF");
        rCmakeSetting.put("MI_OVERRIDE",      "OFF");
        rCmakeSetting.put("MI_WIN_REDIRECT",  "OFF");
        rCmakeSetting.put("MI_OSX_INTERPOSE", "OFF");
        rCmakeSetting.put("MI_OSX_ZONE",      "OFF");
        rCmakeSetting.putAll(Conf.CMAKE_SETTING);
        rCmakeSetting.put("CMAKE_BUILD_TYPE", "Release");
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("mimalloc", "MIMALLOC", LIB_DIR, rCmakeSetting)
            .setSrcDirIniter(wd -> {
                // 首先获取源码路径，这里直接从 resource 里输出
                String tMiZipPath = wd+"mimalloc-"+VERSION+".zip";
                UT.IO.copy(UT.IO.getResource("mimalloc/mimalloc-"+VERSION+".zip"), tMiZipPath);
                // 解压 mimalloc 包到临时目录，如果已经存在则直接清空此目录
                String tMiDir = wd+"mimalloc/";
                UT.IO.removeDir(tMiDir);
                UT.IO.zip2dir(tMiZipPath, tMiDir);
                // 手动拷贝头文件到指定目录，现在也放在这里
                UT.IO.copy(tMiDir+"include/mimalloc.h", INCLUDE_DIR+"mimalloc.h");
                return tMiDir;})
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setRedirectLibPath(Conf.REDIRECT_MIMALLOC_LIB)
            .setCmakeLineOpt(null)
            .get();
        if (Conf.REDIRECT_MIMALLOC_LIB == null) {
            @Nullable String tLLibName = LLIB_NAME_IN(LIB_DIR, "mimalloc");
            LLIB_PATH = tLLibName==null ? LIB_PATH : (LIB_DIR+tLLibName);
        } else {
            LLIB_PATH = Conf.REDIRECT_MIMALLOC_LLIB==null ? Conf.REDIRECT_MIMALLOC_LIB : Conf.REDIRECT_MIMALLOC_LLIB;
        }
        // 设置库路径
        System.load(UT.IO.toAbsolutePath(LIB_PATH));
    }
}
