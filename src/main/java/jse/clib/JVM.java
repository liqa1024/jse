package jse.clib;

import jse.code.IO;

import java.io.IOException;

import static jse.code.Conf.LIB_NAME_IN;
import static jse.code.Conf.LLIB_NAME_IN;
import static jse.code.OS.IS_MAC;
import static jse.code.OS.IS_WINDOWS;
import static jse.code.OS.JAVA_HOME_DIR;

/**
 * 一些 jni 库需要直接依赖 jvm 的动态库部分，这里获取并存储这些路径
 * @author liqa
 */
public class JVM {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link JVM} 是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link JVM} */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(INCLUDE_DIR);
        }
    }
    
    /** 当前 {@link JVM} 的 include 目录，结尾一定存在 {@code '/'} */
    public final static String INCLUDE_DIR;
    /** 当前 {@link JVM} 的 jni_md.h 所在目录，结尾一定存在 {@code '/'} */
    public final static String INCLUDE_MD_DIR;
    /** 当前 {@link JVM} 的动态库所在的目录，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR;
    /** 当前 {@link JVM} 的动态库路径 */
    public final static String LIB_PATH;
    /** 当前 {@link JVM} 需要链接的库所在的目录，结尾一定存在 {@code '/'}，对于 linux 则和 {@link #LIB_DIR} 一致 */
    public final static String LLIB_DIR;
    /** 当前 {@link JVM} 需要链接的库所在的路径，对于 linux 则和 {@link #LIB_PATH} 一致 */
    public final static String LLIB_PATH;
    
    static {
        InitHelper.INITIALIZED = true;
        
        INCLUDE_DIR = JAVA_HOME_DIR + "include/";
        // 优先使用系统判断，没有则自动检测任何目录
        String tMdName = IS_WINDOWS ? "win32" : (IS_MAC ? "darwin" : "linux");
        String tIncludeMdDir = INCLUDE_DIR + tMdName+"/";
        if (!IO.exists(tIncludeMdDir + "jni_md.h")) {
            String[] tNames;
            try {
                tNames = IO.list(INCLUDE_DIR);
            } catch (IOException e) {
                throw new RuntimeException("Fail to det list of \"" + INCLUDE_DIR + "\",\n" +
                                           "  this may be due to running jse under JRE instead of JDK");
            }
            tIncludeMdDir = null;
            for (String tName : tNames) {
                String tIncludeMdDir_ = INCLUDE_DIR + tName + "/";
                if (IO.isDir(tIncludeMdDir_)) {
                    tIncludeMdDir = tIncludeMdDir_;
                    break;
                }
            }
        }
        INCLUDE_MD_DIR = tIncludeMdDir;
        // lib 目录获取
        String tLibDir = JAVA_HOME_DIR + "bin/server/";
        String tLibName = LIB_NAME_IN(tLibDir, "jvm");
        if (tLibName == null) {
            tLibDir = JAVA_HOME_DIR + "bin/client/";
            tLibName = LIB_NAME_IN(tLibDir, "jvm");
        }
        if (tLibName == null) {
            tLibDir = JAVA_HOME_DIR + "lib/server/";
            tLibName = LIB_NAME_IN(tLibDir, "jvm");
        }
        if (tLibName == null) {
            tLibDir = JAVA_HOME_DIR + "lib/client/";
            tLibName = LIB_NAME_IN(tLibDir, "jvm");
        }
        if (tLibName == null) {
            throw new RuntimeException("No jvm lib in '$JAVA_HOME/bin/server/', '$JAVA_HOME/bin/client/', '$JAVA_HOME/lib/server/' or '$JAVA_HOME/lib/client/'");
        }
        LIB_DIR = tLibDir;
        LIB_PATH = tLibDir + tLibName;
        if (!IS_WINDOWS) {
            LLIB_DIR = LIB_DIR;
            LLIB_PATH = LIB_PATH;
        } else {
            LLIB_DIR = JAVA_HOME_DIR + "lib/";
            String tLLibName = LLIB_NAME_IN(LLIB_DIR, "jvm");
            if (tLLibName == null) throw new RuntimeException("No jvm llib in '$JAVA_HOME/lib/'");
            LLIB_PATH = LLIB_DIR + tLLibName;
        }
    }
}
