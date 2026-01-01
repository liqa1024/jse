package jse.clib;

import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.Conf.LLIB_NAME_IN;
import static jse.code.Conf.VERSION_MASK;
import static jse.code.OS.*;

/**
 * 现在将 lammps 核心部分独立出来，注意区分 {@link jse.lmp.NativeLmp}
 * @author liqa
 */
public class LmpCore {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link LmpCore} 相关的参数是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link LmpCore} 相关的参数 */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(LIB_PATH);
        }
    }
    
    /**
     * 使用这个子类来进行一些配置参数的设置，
     * 使用子类的成员不会触发 {@link LmpCore} 的静态初始化
     */
    public final static class Conf {
        /**
         * 设置 lammps 的 build 目录，现在直接指定对应的 lammps 源码目录；
         * 如果内部包含 build 目录并且有合适的格式，则不会尝试重新编译源码
         */
        public static String HOME = OS.env("JSE_LMP_HOME");
        
        /**
         * 指定需要下载的 lammps 的 tag，只在下载时有用
         */
        public static String TAG = OS.env("JSE_LMP_TAG");
        private final static String DEFAULT_TAG = "stable_22Jul2025_update2";
        
        /**
         * 控制在 windows 上编译时是否开启 /MT 选项，
         * 新版 lammps 有时需要开启 /MT 才能正常运行，但这个操作非常不安全；
         * 更加安全的做法是更换兼容的 jdk 版本，例如
         * <a href="https://learn.microsoft.com/zh-cn/java/openjdk/download">微软构建的 OpenJDK</a>
         */
        public static boolean USE_MT = false;
        
        /**
         * 自定义构建 lammps 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_LMP");
        
        /**
         * 自定义构建 lammps 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_LMP"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_LMP", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_LMP"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_LMP"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 自定义构建 lammps 相关库（lmpjni，lmpplugin）需要共享的
         * cmake 参数设置，会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING_SHARE = OS.envMap("JSE_CMAKE_SETTING_LMP_SHARE");
        
        /**
         * 是否将 lammps 动态库提升到全局，这会保证一些 lammps 的模块能找到 lammps 库；
         * 一般只有在 unix 上会遇到这个问题
         */
        public static boolean DLOPEN = OS.envZ("JSE_LMP_DLOPEN", !IS_WINDOWS);
    }
    
    public final static String ROOT = JAR_DIR+"lmp/";
    public final static String INCLUDE_DIR;
    public final static String LIB_DIR;
    public final static String LIB_PATH;
    public final static String LLIB_PATH;
    public final static String EXE_PATH;
    public final static String EXE_CMD;
    private final static String BUILD_DIR_NAME = IS_WINDOWS ? "build-win" : (IS_MAC ? "build-mac" : "build");
    
    
    private static @Nullable String findLmpExe() {
        try {
            for (String tName : IO.list(LIB_DIR)) {
                if (tName.contains("lmp")) {
                    // windows 要求后缀为 .exe，其余要求没有后缀
                    if (IS_WINDOWS) {
                        if (tName.endsWith(".exe")) return tName;
                    } else {
                        if (!tName.contains(".")) return tName;
                    }
                }
            }
        } catch (IOException e) {
            // 失败时返回 null 而不抛出错误
            return null;
        }
        return null;
    }
    
    
    static {
        InitHelper.INITIALIZED = true;
        
        // 依赖 MPICore
        MPICore.InitHelper.init();
        // 这样来统一增加 nativelmp 需要的默认额外设置
        Map<String, String> rCmakeSettingLmp = new LinkedHashMap<>();
        // 这里先添加一个简单的环境变量设置的 lammps pkg
        @Nullable String tLmpPkgStr = OS.env("JSE_LMP_PKG");
        if (tLmpPkgStr!=null) {
            String[] tLmpPkgs = jse.code.IO.Text.splitStr(tLmpPkgStr);
            for (String tLmpPkg : tLmpPkgs) {
                rCmakeSettingLmp.put("PKG_"+tLmpPkg, "ON");
            }
        }
        rCmakeSettingLmp.putAll(Conf.CMAKE_SETTING);
        rCmakeSettingLmp.putAll(Conf.CMAKE_SETTING_SHARE);
        rCmakeSettingLmp.put("BUILD_SHARED_LIBS", "ON");
        rCmakeSettingLmp.put("LAMMPS_EXCEPTIONS:BOOL", "ON");
        rCmakeSettingLmp.put("PKG_PLUGIN", "ON"); // 现在统一打开插件支持
        rCmakeSettingLmp.put("CMAKE_BUILD_TYPE", "Release");
        // 初始化 LIB_DIR 和 INCLUDE_DIR
        String tLmpHome = null;
        String tLmpBuildDir = null;
        if (Conf.HOME != null) {
            tLmpHome = jse.code.IO.toInternalValidDir(jse.code.IO.toAbsolutePath(Conf.HOME));
            tLmpBuildDir = tLmpHome + BUILD_DIR_NAME+"/";
            INCLUDE_DIR = tLmpBuildDir + "includes/";
            LIB_DIR = tLmpBuildDir + "lib/";
        } else {
            // 否则直接版本隔离，采用内部 lammps
            String tLmpDir = ROOT+"core/" + UT.Code.uniqueID(OS.OS_NAME, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, MPICore.EXE_PATH, Conf.CMAKE_C_COMPILER, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_FLAGS, rCmakeSettingLmp) + "/";
            INCLUDE_DIR = tLmpDir + "includes/";
            LIB_DIR = tLmpDir + "lib/";
        }
        final @Nullable String fLmpHome = tLmpHome;
        final @Nullable String fLmpBuildDir = tLmpBuildDir;
        String tLmpTag = Conf.TAG==null ? Conf.DEFAULT_TAG : Conf.TAG;
        final String tLmpCachePath = JNIUtil.PKG_DIR + "lammps-"+ tLmpTag +".zip";
        final Callable<Void> tCacheValider = () -> {
            if (jse.code.IO.exists(tLmpCachePath)) return null;
            System.out.printf("LMP_CORE INIT INFO: No correct lammps source code in %s\n", JNIUtil.PKG_DIR);
            System.out.println("Auto download lammps? (Y/n)");
            BufferedReader tReader = jse.code.IO.toReader(System.in, Charset.defaultCharset());
            String tLine = tReader.readLine();
            while (true) {
                if (tLine.equalsIgnoreCase("n")) {
                    throw new Exception("LMP_CORE INIT ERROR: user interrupted");
                }
                if (tLine.isEmpty() || tLine.equalsIgnoreCase("y")) {
                    break;
                }
                System.out.println("Auto download lammps? (Y/n)");
            }
            String tLmpUri = String.format("https://github.com/lammps/lammps/archive/refs/tags/%s.zip", tLmpTag);
            System.out.println("LMP_CORE INIT INFO: Downloading the source code...");
            try {
                jse.code.IO.copy(URI.create(tLmpUri).toURL(), tLmpCachePath);
            } catch (Exception e) {
                System.err.println("LMP_CORE INIT ERROR: Auto download lammps fail, you can:\n");
                System.err.printf("  1. Download lammps source code manually from %s\n", tLmpUri);
                System.err.printf("  2. Put it into %s\n", tLmpCachePath);
                System.err.println("  3. Rerun this program\n");
                System.err.println("The following is the exception stack information.");
                throw e;
            }
            System.out.println("LMP_CORE INIT INFO: lammps source code downloading finished.");
            return null;
        };
        final JNIUtil.IDirIniter tUnzipLmp = wd -> {
            // 现在直接解压到输入目录
            jse.code.IO.zip2dir(tLmpCachePath, wd);
            for (String tName : jse.code.IO.list(wd)) {
                String tLmpSrcDir = wd + tName + "/";
                if (jse.code.IO.isDir(tLmpSrcDir)) {
                    return tLmpSrcDir;
                }
            }
            return null;
        };
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("lammps", "LMP_CORE", LIB_DIR, rCmakeSettingLmp)
            .setMT(Conf.USE_MT)
            .setEnvChecker(() -> {
                // 在这里输出没有 mpi 的警告，保证无 mpi 情况下只会警告一次
                MPICore.printInfo();
                if (!MPICore.VALID) {
                    System.out.println("LMP_CORE INIT INFO: No MPI support,");
                    System.out.println("Build lammps without MPI support? (y/N)");
                    BufferedReader tReader = jse.code.IO.toReader(System.in, Charset.defaultCharset());
                    String tLine = tReader.readLine();
                    while (!tLine.equalsIgnoreCase("y")) {
                        if (tLine.isEmpty() || tLine.equalsIgnoreCase("n")) {
                            throw new Exception("LMP_CORE INIT ERROR: No MPI support.");
                        }
                        System.out.println("Build lammps without MPI support? (y/N)");
                    }
                }
            })
            .setSrcDirIniter(wd -> {
                // 对于是否有 fLmpHome 采用不同逻辑
                if (fLmpHome!=null) {
                    // 如果压根没有 fLmpHome 目录，则直接从源码解压
                    if (!jse.code.IO.isDir(fLmpHome)) {
                        tCacheValider.call();
                        String tLmpSrcDir = tUnzipLmp.init(wd); // 依旧解压到工作目录，但是这里进行一次移动
                        // 移动到需要的目录
                        try {
                            jse.code.IO.move(tLmpSrcDir, fLmpHome);
                        } catch (Exception e) {
                            // 移动失败则尝试直接拷贝整个目录
                            jse.code.IO.copyDir(tLmpSrcDir, fLmpHome); // 不需要清除旧目录，因为编译完成会自动清理
                        }
                    }
                    return fLmpHome;
                } else {
                    // 此时采用完全版本独立的 lammps，因此直接解压到工作目录
                    tCacheValider.call();
                    return tUnzipLmp.init(wd);
                }})
            .setBuildDirIniter(sd -> {
                // 这样重写 build 目录的初始化，保证 build 目录一定和上面定义一致
                String tBuildDir = fLmpBuildDir==null ? (sd+BUILD_DIR_NAME+"/") : fLmpBuildDir;
                jse.code.IO.makeDir(tBuildDir);
                return tBuildDir;})
            .setPostBuildDir(bd -> {
                // 这里拷贝一份 includes 文件夹
                if (fLmpBuildDir==null) {
                    jse.code.IO.copyDir(bd + "includes/", INCLUDE_DIR);
                }})
            .setCmakeInitDir("../cmake")
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setCmakeLineOp(null)
            .get();
        @Nullable String tLmpLLibName = LLIB_NAME_IN(LIB_DIR, "lammps");
        LLIB_PATH = tLmpLLibName==null ? LIB_PATH : (LIB_DIR+tLmpLLibName);
        @Nullable String tLmpExeName = findLmpExe();
        EXE_PATH = tLmpExeName==null ? null : (LIB_DIR+tLmpExeName);
        EXE_CMD = EXE_PATH==null ? null : ((IS_WINDOWS?"&\"":"\"") + EXE_PATH + "\"");
        // 设置库路径
        System.load(jse.code.IO.toAbsolutePath(LIB_PATH));
        // 部分情况需要将 lammps 库提升到全局范围，主要用于保证部分 lammps 的插件总是能找到 lammps 库本身
        if (Conf.DLOPEN) Dlfcn.dlopen(LIB_PATH);
    }
}
