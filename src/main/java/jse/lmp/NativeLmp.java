package jse.lmp;

import jse.atom.IAtom;
import jse.atom.IAtomData;
import jse.atom.IBox;
import jse.atom.IXYZ;
import jse.cache.DoubleArrayCache;
import jse.cache.IntArrayCache;
import jse.cache.IntMatrixCache;
import jse.cache.MatrixCache;
import jse.clib.*;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.math.matrix.*;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.*;

import static jse.code.OS.*;
import static jse.code.CS.*;
import static jse.code.Conf.*;

/**
 * 基于 jni 的调用本地原生 lammps 的类，
 * 使用类似 python 调用 lammps 的结构，
 * 使用 {@link MPI} 实现并行。
 * <p>
 * 由于 lammps 的特性，此类线程不安全，并且要求所有方法都由相同的线程调用
 * <p>
 * References:
 * <a href="https://docs.lammps.org/Python_module.html">
 * The lammps Python module </a>,
 * <a href="https://docs.lammps.org/Library.html">
 * LAMMPS Library Interfaces </a>,
 * @author liqa
 */
public class NativeLmp implements IAutoShutdown {
    
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(LMPJNI_LIB_PATH);
        }
    }
    
    /**
     * 使用这个子类来进行一些配置参数的设置，
     * 使用子类的成员不会触发 {@link NativeLmp} 的静态初始化
     * @author liqa
     */
    public final static class Conf {
        /**
         * 设置 lammps 的 build 目录，
         * 应该包含一个 includes 目录其中有所有的头文件，
         * 然后包含一个 lib 目录，其中有所有的二进制库文件
         */
        public static String LMP_HOME = OS.env("JSE_LMP_HOME");
        
        /**
         * 指定需要下载的 lammps 的 tag，
         * 只在下载时有用；
         * 这里简单实现，不使用 git 来自动识别最新稳定版本
         */
        public static String LMP_TAG = OS.env("JSE_LMP_TAG");
        private final static String DEFAULT_LMP_TAG = "stable_2Aug2023_update4";
        
        /**
         * 控制在 windows 上编译时是否开启 /MT 选项，
         * 新版 lammps 有时需要开启 /MT 才能正常运行，但这个应该非常不安全；
         * 更加安全的做法是更换兼容的 jdk 版本，例如
         * <a href="https://learn.microsoft.com/zh-cn/java/openjdk/download">微软构建的 OpenJDK</a>
         */
        public static boolean USE_MT = false;
        
        /**
         * 自定义构建 lammps 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new LinkedHashMap<>();
        
        /**
         * 自定义构建 lammps 以及 lmpjni 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_LMP"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_LMP", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_LMP"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_LMP"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 自定义构建 lmpjni 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING_LMPJNI = new LinkedHashMap<>();
        
        /**
         * 自定义构建 lmpjni 时使用的编译器，会覆盖上面的设置，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER_LMPJNI   = OS.env("JSE_CMAKE_C_COMPILER_LMPJNI");
        public static @Nullable String CMAKE_CXX_COMPILER_LMPJNI = OS.env("JSE_CMAKE_CXX_COMPILER_LMPJNI");
        public static @Nullable String CMAKE_C_FLAGS_LMPJNI      = OS.env("JSE_CMAKE_C_FLAGS_LMPJNI");
        public static @Nullable String CMAKE_CXX_FLAGS_LMPJNI    = OS.env("JSE_CMAKE_CXX_FLAGS_LMPJNI");
        
        /**
         * 对于 lmpjni，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
         * 这对于 java 数组和 c 数组的转换很有效
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_LMP", jse.code.Conf.USE_MIMALLOC);
        
        /** 重定向 lammps 动态库的路径，主要用于作为重定向的 lmpjni 库的依赖导入 */
        public static @Nullable String REDIRECT_LMP_LIB = OS.env("JSE_REDIRECT_LMP_LIB");
        public static @Nullable String REDIRECT_LMP_LLIB = OS.env("JSE_REDIRECT_LMP_LLIB");
        /** 重定向 lmpjni 动态库的路径，用于自定义编译这个库的过程，或者重新实现 lmpjni 的接口 */
        public static @Nullable String REDIRECT_LMPJNI_LIB = OS.env("JSE_REDIRECT_LMPJNI_LIB");
        
        /**
         * 是否在检测到库文件时依旧重新编译 lammps，
         * 在需要修改 lammps 包时很有用
         */
        public static boolean REBUILD = false;
        
        /**
         * 是否是旧版本的 lammps，具体来说大致为 18Sep2020 之前版本的 lammps，
         * 开启后会使用更老的 api，兼容性会更高
         */
        public static boolean IS_OLD = OS.envZ("JSE_LMP_IS_OLD", false);
        
        /**
         * lammps 是否有 exception 相关接口，
         * 对于新版的 lammps 总是存在，但对于旧版可能不会存在，
         * 关闭后可以保证编译通过
         */
        public static boolean HAS_EXCEPTIONS = OS.envZ("JSE_LMP_HAS_EXCEPTIONS", true);
        
        /**
         * lammps 的 lammps_has_error 接口是否有 NULL 支持，
         * 对于较旧的版本并不支持这个
         */
        public static boolean EXCEPTIONS_NULL_SUPPORT = OS.envZ("JSE_LMP_EXCEPTIONS_NULL_SUPPORT", true);
        
        /**
         * 是否将 lammps 动态库提升到全局，这会保证一些 lammps 的模块能找到 lammps 库；
         * 一般只有在 unix 上会遇到这个问题
         */
        public static boolean DLOPEN = OS.envZ("JSE_LMP_DLOPEN", !IS_WINDOWS);
    }
    
    private final static String LMP_ROOT = JAR_DIR+"lmp/";
    private final static String LMPJNI_LIB_DIR;
    private final static String LMPJNI_LIB_PATH;
    private final static String[] LMPJNI_SRC_NAME = {
          "jse_lmp_NativeLmp.c"
        , "jse_lmp_NativeLmp.h"
        , "jse_lmp_NativeLmpPointer.h"
    };
    public final static @Nullable String NATIVELMP_SRC_DIR; // 记录自动下载的 lammps 的目录，包含可以重新编译的 lammps 源码，和 Conf.LMP_HOME 的 null 恰好相反
    public final static @Nullable String NATIVELMP_TAG; // 真正的 lammps tag，如果设置了 Conf.LMP_HOME 同样就会变成 null
    public final static String NATIVELMP_HOME; // 真正的 lammps home，也就是 build 目录，永远不为 null
    public final static String NATIVELMP_INCLUDE_DIR;
    public final static String NATIVELMP_LIB_DIR;
    public final static String NATIVELMP_LIB_PATH;
    public final static String NATIVELMP_LLIB_PATH;
    private final static String NATIVE_DIR_NAME = "native", BUILD_DIR_NAME = IS_WINDOWS ? "build-win" : (IS_MAC ? "build-mac" : "build");
    
    private final static String EXECUTABLE_NAME;
    private final static String[] DEFAULT_ARGS;
    
    static {
        InitHelper.INITIALIZED = true;
        // 依赖 jniutil
        JNIUtil.InitHelper.init();
        
        // 先规范化 LMP_HOME 的格式
        if (Conf.LMP_HOME == null) {
            // 先统一设置 tag
            NATIVELMP_TAG = Conf.LMP_TAG==null ? Conf.DEFAULT_LMP_TAG : Conf.LMP_TAG;
            String tNativeDir = LMP_ROOT+NATIVE_DIR_NAME+"-"+NATIVELMP_TAG+"/";
            // 如果没有手动设定 LMP_TAG 则还会尝试不带 tag 的目录
            if (Conf.LMP_TAG==null && !IO.isDir(tNativeDir)) {
                String tShortLmpDir = LMP_ROOT+NATIVE_DIR_NAME+"/";
                if (IO.isDir(tShortLmpDir)) tNativeDir = tShortLmpDir;
            }
            NATIVELMP_SRC_DIR = tNativeDir;
            NATIVELMP_HOME = NATIVELMP_SRC_DIR+BUILD_DIR_NAME+"/";
        } else {
            NATIVELMP_TAG = null;
            NATIVELMP_SRC_DIR = null;
            NATIVELMP_HOME = IO.toInternalValidDir(IO.toAbsolutePath(Conf.LMP_HOME));
        }
        NATIVELMP_INCLUDE_DIR = NATIVELMP_HOME+"includes/";
        NATIVELMP_LIB_DIR = NATIVELMP_HOME+"lib/";
        
        // 这样来统一增加 nativelmp 需要的默认额外设置，
        // 先添加 Conf.CMAKE_SETTING，这样保证确定的优先级
        Map<String, String> rCmakeSettingNativeLmp = new LinkedHashMap<>(Conf.CMAKE_SETTING);
        rCmakeSettingNativeLmp.put("BUILD_SHARED_LIBS",      "ON");
        rCmakeSettingNativeLmp.put("LAMMPS_EXCEPTIONS:BOOL", "ON");
        rCmakeSettingNativeLmp.put("PKG_PLUGIN",             "ON"); // 现在统一打开插件支持
        rCmakeSettingNativeLmp.put("CMAKE_BUILD_TYPE",       "Release");
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        NATIVELMP_LIB_PATH = new JNIUtil.LibBuilder("lammps", "NATIVE_LMP", NATIVELMP_LIB_DIR, rCmakeSettingNativeLmp)
            .setMT(Conf.USE_MT)
            .setMPIChecker() // 现在也会检测 mpi
            .setSrcDirIniter(wd -> {
                // 如果有 NATIVELMP_SRC_DIR 但是不合法，则需要下载 lammps
                if (NATIVELMP_SRC_DIR!=null && !IO.isDir(NATIVELMP_SRC_DIR)) {
                    String tNativeLmpZipPath = wd+"lammps-"+NATIVELMP_TAG+".zip";
                    System.out.printf("NATIVE_LMP INIT INFO: No lammps in %s, you can:\n", NATIVELMP_SRC_DIR);
                    System.out.println("  - Set the environment variable `JSE_LMP_HOME` to lammps path,");
                    System.out.println("    where have $JSE_LMP_HOME/lib/liblammps.so, $JSE_LMP_HOME/includes/lammps/...");
                    System.out.println("  - Move your lammps to "+NATIVELMP_SRC_DIR);
                    System.out.printf( "  - Auto download lammps (%s) by jse.\n", NATIVELMP_TAG);
                    System.out.println("Download lammps? (Y/n)");
                    BufferedReader tReader = IO.toReader(System.in, Charset.defaultCharset());
                    String tLine = tReader.readLine();
                    while (true) {
                        if (tLine.equalsIgnoreCase("n")) {
                            throw new Exception("NATIVE_LMP INIT ERROR: No lammps in "+NATIVELMP_SRC_DIR);
                        }
                        if (tLine.isEmpty() || tLine.equalsIgnoreCase("y")) {
                            break;
                        }
                        System.out.println("Download lammps? (Y/n)");
                    }
                    System.out.println("NATIVE_LMP INIT INFO: Downloading the source code...");
                    IO.copy(URI.create(String.format("https://github.com/lammps/lammps/archive/refs/tags/%s.zip", NATIVELMP_TAG)).toURL(), tNativeLmpZipPath);
                    System.out.println("NATIVE_LMP INIT INFO: lammps source code downloading finished.");
                    // 解压 lammps 到临时目录，如果已经存在则直接清空此目录
                    String tNativeLmpTempSrcDir = wd+"temp/";
                    IO.removeDir(tNativeLmpTempSrcDir);
                    IO.zip2dir(tNativeLmpZipPath, tNativeLmpTempSrcDir);
                    String tNativeLmpTempSrcDir2 = null;
                    for (String tName : IO.list(tNativeLmpTempSrcDir)) {
                        String tNativeLmpTempDir1 = tNativeLmpTempSrcDir + tName + "/";
                        if (IO.isDir(tNativeLmpTempDir1)) {
                            tNativeLmpTempSrcDir2 = tNativeLmpTempDir1;
                            break;
                        }
                    }
                    if (tNativeLmpTempSrcDir2 == null) throw new Exception("NATIVE_LMP INIT ERROR: No lammps in "+tNativeLmpTempSrcDir);
                    // 移动到需要的目录
                    try {
                        IO.move(tNativeLmpTempSrcDir2, NATIVELMP_SRC_DIR);
                    } catch (Exception e) {
                        // 移动失败则尝试直接拷贝整个目录
                        IO.copyDir(tNativeLmpTempSrcDir2, NATIVELMP_SRC_DIR);
                    }
                }
                return NATIVELMP_SRC_DIR;})
            .setBuildDirIniter(sd -> {
                // 这样重写 build 目录的初始化，保证 build 目录一定是 NATIVELMP_HOME
                IO.makeDir(NATIVELMP_HOME);
                return NATIVELMP_HOME;})
            .setCmakeInitDir("../cmake")
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setRebuild(Conf.REBUILD).setRedirectLibPath(Conf.REDIRECT_LMP_LIB)
            .setCmakeLineOp(null)
            .get();
        if (Conf.REDIRECT_LMP_LIB == null) {
            @Nullable String tNativeLmpLLibName = LLIB_NAME_IN(NATIVELMP_LIB_DIR, "lammps");
            NATIVELMP_LLIB_PATH = tNativeLmpLLibName==null ? NATIVELMP_LIB_PATH : (NATIVELMP_LIB_DIR+tNativeLmpLLibName);
        } else {
            NATIVELMP_LLIB_PATH = Conf.REDIRECT_LMP_LLIB==null ? Conf.REDIRECT_LMP_LIB : Conf.REDIRECT_LMP_LLIB;
        }
        
        // 现在 uniqueID 不再包含这个 tag（确实当时也想到了），因为已经包含到了 LMP_HOME 中
        // 在这里初始化保证顺序合理
        LMPJNI_LIB_DIR = LMP_ROOT+"jni/" + UT.Code.uniqueID(JAVA_HOME, VERSION, NATIVELMP_HOME, Conf.USE_MIMALLOC, Conf.HAS_EXCEPTIONS, Conf.EXCEPTIONS_NULL_SUPPORT, Conf.CMAKE_C_COMPILER_LMPJNI, Conf.CMAKE_CXX_COMPILER_LMPJNI, Conf.CMAKE_C_FLAGS_LMPJNI, Conf.CMAKE_CXX_FLAGS_LMPJNI, Conf.CMAKE_SETTING_LMPJNI) + "/";
        // 这样来统一增加 lmpjni 需要的默认额外设置，
        // 先添加 Conf.CMAKE_SETTING_LMPJNI，这样保证确定的优先级
        Map<String, String> rCmakeSettingLmpJNI = new LinkedHashMap<>(Conf.CMAKE_SETTING_LMPJNI);
        rCmakeSettingLmpJNI.put("JSE_LAMMPS_IS_OLD",                  (Conf.IS_OLD                 ?"ON":"OFF"));
        rCmakeSettingLmpJNI.put("JSE_LAMMPS_HAS_EXCEPTIONS",          (Conf.HAS_EXCEPTIONS         ?"ON":"OFF"));
        rCmakeSettingLmpJNI.put("JSE_LAMMPS_EXCEPTIONS_NULL_SUPPORT", (Conf.EXCEPTIONS_NULL_SUPPORT?"ON":"OFF"));
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LMPJNI_LIB_PATH = new JNIUtil.LibBuilder("lmpjni", "LMP_JNI", LMPJNI_LIB_DIR, rCmakeSettingLmpJNI)
            .setMPIChecker() // 现在也会检测 mpi
            .setSrc("lmp", LMPJNI_SRC_NAME)
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER_LMPJNI).setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER_LMPJNI).setCmakeCFlags(Conf.CMAKE_C_FLAGS_LMPJNI).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS_LMPJNI)
            .setUseMiMalloc(Conf.USE_MIMALLOC).setRedirectLibPath(Conf.REDIRECT_LMPJNI_LIB)
            .setCmakeLineOp(line -> {
                // 替换其中的 lammps 库路径为设置好的路径
                line = line.replace("$ENV{JSE_LMP_INCLUDE_DIR}", NATIVELMP_INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                           .replace("$ENV{JSE_LMP_LIB_PATH}"   , NATIVELMP_LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
                return line;
            }).get();
        
        // 设置库路径
        System.load(IO.toAbsolutePath(NATIVELMP_LIB_PATH));
        System.load(IO.toAbsolutePath(LMPJNI_LIB_PATH));
        // 部分情况需要将 lammps 库提升到全局范围，主要用于保证部分 lammps 的插件总是能找到 lammps 库本身
        if (Conf.DLOPEN) Dlfcn.dlopen(NATIVELMP_LIB_PATH);
        
        // 设置 EXECUTABLE_NAME
        String tExecutableName = IO.toFileName(NATIVELMP_LIB_PATH);
        int tFirstDot = tExecutableName.indexOf(".");
        EXECUTABLE_NAME = tFirstDot>=0 ? tExecutableName.substring(0, tFirstDot) : tExecutableName;
        DEFAULT_ARGS = new String[] {EXECUTABLE_NAME, "-log", "none"};
        LAMMPS_LIB_MPI = lammpsLibMpi_();
    }
    
    
    private final NativeLmpPointer mLmpPtr;
    private final Thread mInitThead; // lammps 需要保证初始化时的线程和调用时的是相同的
    private boolean mDead = false;
    /**
     * Create an instance of the LAMMPS Java class.
     * <p>
     * This is a Java wrapper class that exposes the LAMMPS C-library interface to Java.
     * It either requires that LAMMPS has been compiled as shared library which is then
     * dynamically loaded via the jni, for example through the java {@code <init>} method.
     * When the class is instantiated it calls the {@code lammps_open()} function of the LAMMPS
     * C-library interface, which in turn will create an instance of the LAMMPS C++ class.
     * The handle to this C++ class is stored internally and automatically passed to the
     * calls to the C library interface.
     *
     * @param aArgs  array of command line arguments to be passed to the {@code lammps_open()}
     *               (or {@code lammps_open_no_mpi()} when no mpi or no init mpi) function.
     *               The executable name is automatically added.
     *
     * @param aComm MPI communicator as provided by {@link MPI} (or {@link MPI.Native}).
     *              null means use {@link MPI.Comm#WORLD} implicitly.
     *
     * @author liqa
     */
    public NativeLmp(String[] aArgs, @Nullable MPI.Comm aComm) throws LmpException {
        String[] tArgs = aArgs==null ? DEFAULT_ARGS : new String[aArgs.length+1];
        tArgs[0] = EXECUTABLE_NAME;
        if (aArgs != null) {
            for (int i = 0; i < aArgs.length; i++) {
                String tArg = aArgs[i];
                if (tArg==null) throw new NullPointerException();
                tArgs[i+1] = tArg;
            }
        }
        long tLmpPtr = aComm==null ? lammpsOpen_(tArgs) : lammpsOpen_(tArgs, aComm.ptr_());
        if (tLmpPtr==0 || tLmpPtr==-1) throw new LmpException("Failed to init a NativeLmp: "+tLmpPtr);
        mLmpPtr = new NativeLmpPointer(this, tLmpPtr);
        mInitThead = Thread.currentThread();
    }
    public NativeLmp(Collection<? extends CharSequence> aArgs, @Nullable MPI.Comm aComm) throws LmpException {this(IO.Text.toArray(aArgs), aComm);}
    public NativeLmp(String... aArgs) throws LmpException {this(aArgs, null);}
    public NativeLmp(@Nullable MPI.Comm aComm, String... aArgs) throws LmpException {this(aArgs, aComm);}
    public NativeLmp(@Nullable MPI.Comm aComm) throws LmpException {this((String[])null, aComm);}
    public NativeLmp() throws LmpException {this((String[])null);}
    private native static long lammpsOpen_(@NotNull String @NotNull[] aArgs, long aComm) throws LmpException;
    private native static long lammpsOpen_(@NotNull String @NotNull[] aArgs) throws LmpException;
    
    public boolean threadValid() {
        return Thread.currentThread() == mInitThead;
    }
    public void checkThread() throws LmpException {
        mLmpPtr.checkThread();
    }
    
    /**
     * Return a numerical representation of the LAMMPS version in use.
     * <p>
     * This is a wrapper around the {@code lammps_version()} function of the C-library interface.
     * @return version number
     */
    public int version() throws LmpException {
        checkThread();
        return lammpsVersion_(mLmpPtr.mPtr);
    }
    private native static int lammpsVersion_(long aLmpPtr) throws LmpException;
    
    /**
     * 获取 LAMMPS 的字符串版本，为 version.h 中定义的 {@code LAMMPS_VERSION}
     *
     * 主要用于 {@link LmpPlugin} 初始化使用
     * @return 字符串版本，如果 lammps 不支持则返回 {@code null}
     */
    public @Nullable String versionStr() throws LmpException {
        checkThread();
        return lammpsVersionStr_(mLmpPtr.mPtr);
    }
    private native static String lammpsVersionStr_(long aLmpPtr) throws LmpException;
    
    /**
     * Get the MPI communicator in use by the current LAMMPS instance
     * <p>
     * This is a wrapper around the {@code lammps_get_mpi_comm()} function of the Fortran-library interface,
     * and convert it to the C language representation by using {@code MPI_Comm_f2c()}.
     * <p>
     * It will return {@code null} if the LAMMPS library was compiled without MPI support.
     * @return the {@link MPI.Comm} of this NativeLmp
     */
    public MPI.Comm comm() throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        checkThread();
        return MPI.Comm.of(lammpsComm_(mLmpPtr.mPtr));
    }
    private native static long lammpsComm_(long aLmpPtr) throws LmpException;
    
    /** 在编译 lammps jni 库时是否有找到 mpi 支持，如果没有则很多操作不需要真的执行 mpi 通讯 */
    public final static boolean LAMMPS_LIB_MPI;
    private native static boolean lammpsLibMpi_();
    
    /**
     * Shut down the MPI communication like {@link MPI#shutdown}
     * <p>
     * This is a wrapper around the {@code lammps_mpi_finalize()} function of the C-library interface.
     */
    public static void shutdownMPI() throws LmpException {lammpsMpiFinalize_();}
    private native static void lammpsMpiFinalize_() throws LmpException;
    
    /**
     * Read LAMMPS commands from a file.
     * <p>
     * This is a wrapper around the {@code lammps_file()} function of the C-library interface.
     * It will open the file with the name/path file and process the LAMMPS commands line by line
     * until the end.
     * The function will return when the end of the file is reached.
     * @param aPath Name of the file/path with LAMMPS commands
     */
    public void file(String aPath) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aPath == null) throw new NullPointerException();
        checkThread();
        lammpsFile_(mLmpPtr.mPtr, IO.toAbsolutePath(aPath));
    }
    private native static void lammpsFile_(long aLmpPtr, @NotNull String aPath) throws LmpException;
    
    /**
     * Process a single LAMMPS input command from a string.
     * <p>
     * This is a wrapper around the {@code lammps_command()} function of the C-library interface.
     * @param aCmd a single lammps command
     */
    public void command(String aCmd) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aCmd == null) throw new NullPointerException();
        checkThread();
        lammpsCommand_(mLmpPtr.mPtr, aCmd);
    }
    private native static void lammpsCommand_(long aLmpPtr, @NotNull String aCmd) throws LmpException;
    
    /**
     * Process multiple LAMMPS input commands from a list of strings.
     * <p>
     * This is a wrapper around the {@code lammps_commands_list()} function of the C-library interface.
     * @param aCmds a list of lammps commands
     */
    public void commands(String... aCmds) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aCmds == null) throw new NullPointerException();
        for (String tCmd : aCmds) if (tCmd == null) throw new NullPointerException();
        checkThread();
        lammpsCommandsList_(mLmpPtr.mPtr, aCmds);
    }
    private native static void lammpsCommandsList_(long aLmpPtr, @NotNull String @NotNull[] aCmds) throws LmpException;
    
    /**
     * Process a block of LAMMPS input commands from a string.
     * <p>
     * This is a wrapper around the {@code lammps_commands_string()} function of the C-library interface.
     * @param aMultiCmd text block of lammps commands
     */
    public void commands(String aMultiCmd) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aMultiCmd == null) throw new NullPointerException();
        checkThread();
        lammpsCommandsString_(mLmpPtr.mPtr, aMultiCmd);
    }
    private native static void lammpsCommandsString_(long aLmpPtr, @NotNull String aMultiCmd) throws LmpException;
    
    /**
     * Get the total number of atoms in the LAMMPS instance.
     * <p>
     * This is a wrapper around the {@code lammps_get_natoms()} function of the C-library interface.
     * @return number of atoms
     */
    public long atomNumber() throws LmpException {
        checkThread();
        return (long)lammpsGetNatoms_(mLmpPtr.mPtr);
    }
    /** @deprecated use {@link #atomNumber} or {@link #natoms} */ @Deprecated public final long atomNum() throws LmpException {return atomNumber();}
    @VisibleForTesting public long natoms() throws LmpException {return atomNumber();}
    private native static double lammpsGetNatoms_(long aLmpPtr) throws LmpException;
    
    /**
     * Get the total number of atoms types in the LAMMPS instance.
     * @return number of atom types
     */
    public int atomTypeNumber() throws LmpException {return settingOf("ntypes");}
    /** @deprecated use {@link #atomTypeNumber} or {@link #ntypes} */ @Deprecated public final int atomTypeNum() throws LmpException {return atomTypeNumber();}
    @VisibleForTesting public int ntypes() throws LmpException {return atomTypeNumber();}
    
    /**
     * Get the local number of atoms in the LAMMPS instance.
     * @return number of “owned” atoms of the current MPI rank.
     */
    public int localAtomNumber() throws LmpException {return settingOf("nlocal");}
    /** @deprecated use {@link #localAtomNumber} or {@link #nlocal} */ @Deprecated public final int localAtomNum() throws LmpException {return localAtomNumber();}
    @VisibleForTesting public int nlocal() throws LmpException {return localAtomNumber();}
    
    /**
     * Extract simulation box parameters
     * <p>
     * This is a wrapper around the lammps_extract_box() function of the C-library interface.
     * Unlike in the C function, the result is returned a {@link LmpBox} object.
     * @return a {@link LmpBox} object.
     */
    public LmpBox box() throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        checkThread();
        double[] rBox = DoubleArrayCache.getArray(15);
        lammpsExtractBox_(mLmpPtr.mPtr, rBox);
        LmpBox tOut;
        if (settingOf("triclinic")==1) {
            tOut = new LmpBoxPrism(rBox[0], rBox[3], rBox[1], rBox[4], rBox[2], rBox[5], rBox[6], rBox[8], rBox[7]);
        } else {
            tOut = new LmpBox(rBox[0], rBox[3], rBox[1], rBox[4], rBox[2], rBox[5]);
        }
        DoubleArrayCache.returnArray(rBox);
        return tOut;
    }
    /**
     * {@code [xlo, ylo, zlo, xhi, yhi, zhi, xy, yz, xz, px, py, pz, bx, by, bz]}
     * <p>
     * {@code [0  , 1  , 2  , 3  , 4  , 5  , 6 , 7 , 8 , 9 , 10, 11, 12, 13, 14]}
     */
    private native static void lammpsExtractBox_(long aLmpPtr, double @NotNull[] rBox) throws LmpException;
    
    /**
     * Reset simulation box parameters
     * <p>
     * This is a wrapper around the {@code lammps_reset_box()} function of the C-library interface,
     * but in {@link LmpBoxPrism} order.
     */
    public void resetBox(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi, double aXY, double aXZ, double aYZ) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        checkThread();
        lammpsResetBox_(mLmpPtr.mPtr, aXlo, aYlo, aZlo, aXhi, aYhi, aZhi, aXY, aYZ, aXZ);
    }
    public void resetBox(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi) throws LmpException {
        resetBox(aXlo, aXhi, aYlo, aYhi, aZlo, aZhi, 0.0, 0.0, 0.0);
    }
    public void resetBox(double aXhi, double aYhi, double aZhi) throws LmpException {
        resetBox(0.0, aXhi, 0.0, aYhi, 0.0, aZhi);
    }
    public void resetBox(LmpBoxPrism aBoxPrism) throws LmpException {
        resetBox(aBoxPrism.xlo(), aBoxPrism.xhi(), aBoxPrism.ylo(), aBoxPrism.yhi(), aBoxPrism.zlo(), aBoxPrism.zhi(), aBoxPrism.xy(), aBoxPrism.xz(), aBoxPrism.yz());
    }
    public void resetBox(LmpBox aBox) throws LmpException {
        if (aBox instanceof LmpBoxPrism) {
            resetBox((LmpBoxPrism)aBox);
        } else {
            resetBox(aBox.xlo(), aBox.xhi(), aBox.ylo(), aBox.yhi(), aBox.zlo(), aBox.zhi());
        }
    }
    public void resetBox(IXYZ aBoxLo, IXYZ aBoxHi) throws LmpException {
        resetBox(aBoxLo.x(), aBoxHi.x(), aBoxLo.y(), aBoxHi.y(), aBoxLo.z(), aBoxHi.z());
    }
    public void resetBox(IXYZ aBox) throws LmpException {
        resetBox(aBox.x(), aBox.y(), aBox.z());
    }
    private native static void lammpsResetBox_(long aLmpPtr, double aXlo, double aYlo, double aZlo, double aXhi, double aYhi, double aZhi, double aXY, double aYZ, double aXZ) throws LmpException;
    
    /**
     * Get current value of a thermo keyword
     * <p>
     * This is a wrapper around the {@code lammps_get_thermo()} function of the C-library interface.
     * @param aName name of thermo keyword
     * @return value of thermo keyword
     */
    public double thermoOf(String aName) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        return lammpsGetThermo_(mLmpPtr.mPtr, aName);
    }
    private native static double lammpsGetThermo_(long aLmpPtr, @NotNull String aName) throws LmpException;
    
    /**
     * Query LAMMPS about global settings that can be expressed as an integer.
     * <p>
     * This is a wrapper around the {@code lammps_extract_setting()} function of the C-library interface.
     * <a href="https://docs.lammps.org/Library_properties.html#_CPPv422lammps_extract_settingPvPKc">
     * Its documentation </a> includes a list of the supported keywords.
     * @param aName name of the setting
     * @return value of the setting
     */
    public int settingOf(String aName) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        return lammpsExtractSetting_(mLmpPtr.mPtr, aName);
    }
    private native static int lammpsExtractSetting_(long aLmpPtr, @NotNull String aName) throws LmpException;
    
    /**
     * Gather the named per-atom, per-atom fix, per-atom compute,
     * or fix property/atom-based entities from all processes, unordered.
     * <p>
     * This is a wrapper around the {@code lammps_gather_concat()} function of the C-library interface.
     * <a href="https://docs.lammps.org/Classes_atom.html#_CPPv4N9LAMMPS_NS4Atom7extractEPKc">
     * Its documentation </a> includes a list of the supported keywords and their data types.
     * This function will try to auto-detect the data type by asking the library.
     * This function returns null if either the keyword is not recognized.
     * @param aName name of the property
     * @return RowMatrix of requested data
     * @see <a href="https://docs.lammps.org/Library_scatter.html#_CPPv420lammps_gather_concatPvPKciiPv">
     * lammps_gather_concat() </a>
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    public RowMatrix atomDataOf(String aName) throws LmpException, MPIException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        switch(aName) {
        case "mass":        {return localAtomDataOf(aName, 1, atomTypeNumber()+1, 1);}
        case "id":          {return fullAtomDataOf(aName, false, 1);}
        case "type":        {return fullAtomDataOf(aName, false, 1);}
        case "mask":        {return fullAtomDataOf(aName, false, 1);}
        case "image":       {return fullAtomDataOf(aName, false, 1);}
        case "x":           {return fullAtomDataOf(aName, true , 3);}
        case "v":           {return fullAtomDataOf(aName, true , 3);}
        case "f":           {return fullAtomDataOf(aName, true , 3);}
        case "molecule":    {return fullAtomDataOf(aName, false, 1);}
        case "q":           {return fullAtomDataOf(aName, true , 1);}
        case "mu":          {return fullAtomDataOf(aName, true , 3);}
        case "omega":       {return fullAtomDataOf(aName, true , 3);}
        case "angmom":      {return fullAtomDataOf(aName, true , 3);}
        case "torque":      {return fullAtomDataOf(aName, true , 3);}
        case "radius":      {return fullAtomDataOf(aName, true , 1);}
        case "rmass":       {return fullAtomDataOf(aName, true , 1);}
        case "ellipsoid":   {return fullAtomDataOf(aName, false, 1);}
        case "line":        {return fullAtomDataOf(aName, false, 1);}
        case "tri":         {return fullAtomDataOf(aName, false, 1);}
        case "body":        {return fullAtomDataOf(aName, false, 1);}
        case "quat":        {return fullAtomDataOf(aName, true , 4);}
        case "temperature": {return fullAtomDataOf(aName, true , 1);}
        case "heatflow":    {return fullAtomDataOf(aName, true , 1);}
        default: {
            if (aName.startsWith("i_")) {
                return fullAtomDataOf(aName, false, 1);
            } else
            if (aName.startsWith("d_")) {
                return fullAtomDataOf(aName, true , 1);
            } else {
                throw new IllegalArgumentException("Unexpected name: \""+aName+"\", use fullAtomDataOf(\""+aName+"\", aIsDouble, aRowNum, aColNum) to gather this atom data.");
            }
        }}
    }
    @SuppressWarnings("DuplicateBranchesInSwitch")
    public RowIntMatrix atomIntDataOf(String aName) throws LmpException, MPIException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        switch(aName) {
        case "id":          {return fullAtomIntDataOf(aName, 1);}
        case "type":        {return fullAtomIntDataOf(aName, 1);}
        case "mask":        {return fullAtomIntDataOf(aName, 1);}
        case "image":       {return fullAtomIntDataOf(aName, 1);}
        case "x":           {throw new IllegalArgumentException("Data type of \"x\" is double, use atomDataOf(\"x\") to gather this atom data.");}
        case "v":           {throw new IllegalArgumentException("Data type of \"v\" is double, use atomDataOf(\"v\") to gather this atom data.");}
        case "f":           {throw new IllegalArgumentException("Data type of \"f\" is double, use atomDataOf(\"f\") to gather this atom data.");}
        case "molecule":    {return fullAtomIntDataOf(aName, 1);}
        case "q":           {throw new IllegalArgumentException("Data type of \"q\" is double, use atomDataOf(\"q\") to gather this atom data.");}
        case "mu":          {throw new IllegalArgumentException("Data type of \"mu\" is double, use atomDataOf(\"mu\") to gather this atom data.");}
        case "omega":       {throw new IllegalArgumentException("Data type of \"omega\" is double, use atomDataOf(\"omega\") to gather this atom data.");}
        case "angmom":      {throw new IllegalArgumentException("Data type of \"angmom\" is double, use atomDataOf(\"angmom\") to gather this atom data.");}
        case "torque":      {throw new IllegalArgumentException("Data type of \"torque\" is double, use atomDataOf(\"torque\") to gather this atom data.");}
        case "radius":      {throw new IllegalArgumentException("Data type of \"radius\" is double, use atomDataOf(\"radius\") to gather this atom data.");}
        case "rmass":       {throw new IllegalArgumentException("Data type of \"rmass\" is double, use atomDataOf(\"rmass\") to gather this atom data.");}
        case "ellipsoid":   {return fullAtomIntDataOf(aName, 1);}
        case "line":        {return fullAtomIntDataOf(aName, 1);}
        case "tri":         {return fullAtomIntDataOf(aName, 1);}
        case "body":        {return fullAtomIntDataOf(aName, 1);}
        case "quat":        {throw new IllegalArgumentException("Data type of \"quat\" is double, use atomDataOf(\"quat\") to gather this atom data.");}
        case "temperature": {throw new IllegalArgumentException("Data type of \"temperature\" is double, use atomDataOf(\"temperature\") to gather this atom data.");}
        case "heatflow":    {throw new IllegalArgumentException("Data type of \"heatflow\" is double, use atomDataOf(\"heatflow\") to gather this atom data.");}
        default: {
            if (aName.startsWith("i_")) {
                return fullAtomIntDataOf(aName, 1);
            } else
            if (aName.startsWith("d_")) {
                throw new IllegalArgumentException("Data type of \""+aName+"\" is double, use atomDataOf(\""+aName+"\") to gather this atom data.");
            } else {
                throw new IllegalArgumentException("Unexpected name: \""+aName+"\", use fullAtomIntDataOf(\""+aName+"\", aRowNum, aColNum) to gather this atom data.");
            }
        }}
    }
    
    /**
     * Gather the named per-atom, per-atom fix, per-atom compute,
     * or fix property/atom-based entities from all processes, unordered.
     * <p>
     * This is a wrapper around the {@code lammps_extract_atom()} function of the C-library interface.
     * @param aName name of the property
     * @param aIsDouble false for int, true for double
     * @param aColNum column number of Matrix of requested data
     * @return RowMatrix of requested data, row number is always atomNum.
     */
    public RowMatrix fullAtomDataOf(String aName, boolean aIsDouble, int aColNum) throws LmpException, MPIException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        RowMatrix rData = MatrixCache.getMatRow((int)atomNumber(), aColNum);
        lammpsGatherConcat_(mLmpPtr.mPtr, aName, aIsDouble, aColNum, rData.internalData());
        return rData;
    }
    public RowIntMatrix fullAtomIntDataOf(String aName, int aColNum) throws LmpException, MPIException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        RowIntMatrix rData = IntMatrixCache.getMatRow((int)atomNumber(), aColNum);
        lammpsGatherConcatInt_(mLmpPtr.mPtr, aName, aColNum, rData.internalData());
        return rData;
    }
    /**
     * 获取此进程的原子数据而不进行收集操作，
     * 似乎 mass 需要使用此方法才能合法获取；
     * 这里支持 BigBig 包因此需要 int 类型的 aDataType
     * <p>
     * This is a wrapper around the {@code lammps_extract_atom()} function of the C-library interface.
     * @param aName name of the property
     * @param aDataType 0 for int, 1 for double, 2 for int64_t when LAMMPS_BIGBIG is defined, 3 for int64_t anyway
     * @param aColNum column number of Matrix of requested data
     * @param aRowNum row number of Matrix of requested data
     * @return RowMatrix of requested data
     */
    public RowMatrix localAtomDataOf(String aName, int aDataType, int aRowNum, int aColNum) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        RowMatrix rData = MatrixCache.getMatRow(aRowNum, aColNum);
        lammpsExtractAtom_(mLmpPtr.mPtr, aName, aDataType, aRowNum, aColNum, rData.internalData());
        return rData;
    }
    public RowIntMatrix localAtomIntDataOf(String aName, int aDataType, int aRowNum, int aColNum) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        RowIntMatrix rData = IntMatrixCache.getMatRow(aRowNum, aColNum);
        lammpsExtractAtomInt_(mLmpPtr.mPtr, aName, aDataType, aRowNum, aColNum, rData.internalData());
        return rData;
    }
    
    /**
     * 直接获取原子数据的 C 指针，用于只获取部分数据而不是整体，
     * 可以避免整体值拷贝带来的损耗。
     * @param aName name of the property
     * @return {@link CPointer} of lammps internal atom data
     */
    public CPointer localAtomCPointerDataOf(String aName) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        return new CPointer(lammpsExtractAtomCPointer_(mLmpPtr.mPtr, aName));
    }
    private native static void lammpsGatherConcat_(long aLmpPtr, @NotNull String aName, boolean aIsDouble, int aCount, double @NotNull[] rData) throws LmpException, MPIException;
    private native static void lammpsGatherConcatInt_(long aLmpPtr, @NotNull String aName, int aCount, int @NotNull[] rData) throws LmpException, MPIException;
    private native static void lammpsExtractAtom_(long aLmpPtr, @NotNull String aName, int aDataType, int aAtomNum, int aCount, double @NotNull[] rData) throws LmpException;
    private native static void lammpsExtractAtomInt_(long aLmpPtr, @NotNull String aName, int aDataType, int aAtomNum, int aCount, int @NotNull[] rData) throws LmpException;
    private native static void lammpsExtractAtomLong_(long aLmpPtr, @NotNull String aName, int aDataType, int aAtomNum, int aCount, long @NotNull[] rData) throws LmpException;
    private native static long lammpsExtractAtomCPointer_(long aLmpPtr, @NotNull String aName) throws LmpException;
    
    
    /**
     * 获取指定名称的任意的 compute 属性，会自动检测列数和行数，
     * 并自动对 atom 的情况进行 MPI 同步
     * @param aName 需要获取的 compute 值名称，不需要 {@code c_} 前缀
     * @param aDataStyle 此名称对应的数据类型，{@link #LMP_STYLE_GLOBAL}, {@link #LMP_STYLE_ATOM} or {@link #LMP_STYLE_LOCAL}
     * @param aDataType 此名称对应的数据种类，{@link #LMP_TYPE_SCALAR}, {@link #LMP_TYPE_VECTOR} or {@link #LMP_TYPE_ARRAY}
     * @return 得到的 compute 属性
     */
    public RowMatrix computeOf(String aName, int aDataStyle, int aDataType) throws LmpException, MPIException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        switch (aDataStyle) {
        case LMP_STYLE_GLOBAL: {
            RowMatrix rData;
            switch (aDataType) {
            case LMP_TYPE_SCALAR: {
                rData = MatrixCache.getMatRow(1, 1);
                break;
            }
            case LMP_TYPE_VECTOR: {
                rData = MatrixCache.getMatRow(lammpsExtractComputeSize_(mLmpPtr.mPtr, aName, aDataStyle, LMP_SIZE_VECTOR), 1);
                break;
            }
            case LMP_TYPE_ARRAY: {
                rData = MatrixCache.getMatRow(lammpsExtractComputeSize_(mLmpPtr.mPtr, aName, aDataStyle, LMP_SIZE_ROWS), lammpsExtractComputeSize_(mLmpPtr.mPtr, aName, aDataStyle, LMP_SIZE_COLS));
                break;
            }
            default: throw new IllegalArgumentException("Invalid DataType: " + aDataType);
            }
            lammpsExtractCompute_(mLmpPtr.mPtr, aName, aDataStyle, aDataType, rData.rowNumber(), rData.columnNumber(), rData.internalData());
            return rData;
        }
        case LMP_STYLE_LOCAL: {
            RowMatrix rData;
            switch (aDataType) {
            case LMP_TYPE_VECTOR: {
                rData = MatrixCache.getMatRow(lammpsExtractComputeSize_(mLmpPtr.mPtr, aName, aDataStyle, LMP_SIZE_VECTOR), 1);
                break;
            }
            case LMP_TYPE_ARRAY: {
                rData = MatrixCache.getMatRow(lammpsExtractComputeSize_(mLmpPtr.mPtr, aName, aDataStyle, LMP_SIZE_ROWS), lammpsExtractComputeSize_(mLmpPtr.mPtr, aName, aDataStyle, LMP_SIZE_COLS));
                break;
            }
            default: throw new IllegalArgumentException("Invalid DataType: " + aDataType);
            }
            lammpsExtractCompute_(mLmpPtr.mPtr, aName, aDataStyle, aDataType, rData.rowNumber(), rData.columnNumber(), rData.internalData());
            return rData;
        }
        case LMP_STYLE_ATOM: {
            int tColNum;
            switch (aDataType) {
            case LMP_TYPE_VECTOR: {
                tColNum = 0;
                break;
            }
            case LMP_TYPE_ARRAY: {
                tColNum = lammpsExtractComputeSize_(mLmpPtr.mPtr, aName, aDataStyle, LMP_SIZE_COLS);
                break;
            }
            default: throw new IllegalArgumentException("Invalid DataType: " + aDataType);
            }
            RowMatrix rData = MatrixCache.getMatRow((int)atomNumber(), tColNum==0?1:tColNum);
            lammpsGatherCompute_(mLmpPtr.mPtr, aName, tColNum, rData.internalData());
            return rData;
        }
        default: throw new IllegalArgumentException("Invalid DataStyle: " + aDataStyle);
        }
    }
    
    /**
     * 直接获取 compute 属性的大小值。
     * @param aName 需要获取的 compute 值名称，不需要 {@code c_} 前缀
     * @param aDataStyle 此名称对应的数据类型，{@link #LMP_STYLE_GLOBAL}, {@link #LMP_STYLE_ATOM} or {@link #LMP_STYLE_LOCAL}
     * @param aDataType 此名称对应的数据种类，{@link #LMP_SIZE_VECTOR}, {@link #LMP_SIZE_ROWS} or {@link #LMP_SIZE_COLS}
     * @return 请求获取的 compute 属性的具体大小值
     */
    public int computeSizeOf(String aName, int aDataStyle, int aDataType) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        if (aDataStyle<0 || aDataStyle>2) throw new IllegalArgumentException("Invalid DataStyle: " + aDataStyle);
        if (aDataType<3 || aDataType>5) throw new IllegalArgumentException("Invalid DataType: " + aDataType);
        return lammpsExtractComputeSize_(mLmpPtr.mPtr, aName, aDataStyle, aDataType);
    }
    /**
     * 直接获取 compute 属性的 C 指针，用于只获取部分数据而不是整体，
     * 可以避免整体值拷贝带来的损耗。
     * @param aName 需要获取的 compute 值名称，不需要 {@code c_} 前缀
     * @param aDataStyle 此名称对应的数据类型，{@link #LMP_STYLE_GLOBAL}, {@link #LMP_STYLE_ATOM} or {@link #LMP_STYLE_LOCAL}
     * @param aDataType 此名称对应的数据种类，{@link #LMP_TYPE_SCALAR}, {@link #LMP_TYPE_VECTOR}, {@link #LMP_TYPE_ARRAY}, {@link #LMP_SIZE_VECTOR}, {@link #LMP_SIZE_ROWS} or {@link #LMP_SIZE_COLS}
     * @return {@link CPointer} of lammps internal compute data
     */
    public CPointer computeCPointerOf(String aName, int aDataStyle, int aDataType) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        if (aDataStyle<0 || aDataStyle>2) throw new IllegalArgumentException("Invalid DataStyle: " + aDataStyle);
        if (aDataType<0 || aDataType>5) throw new IllegalArgumentException("Invalid DataType: " + aDataType);
        return new CPointer(lammpsExtractComputeCPointer_(mLmpPtr.mPtr, aName, aDataStyle, aDataType));
    }
    
    public final static int
          LMP_STYLE_GLOBAL = 0
        , LMP_STYLE_ATOM = 1
        , LMP_STYLE_LOCAL = 2
        ;
    public final static int
          LMP_TYPE_SCALAR = 0
        , LMP_TYPE_VECTOR = 1
        , LMP_TYPE_ARRAY = 2
        , LMP_SIZE_VECTOR = 3
        , LMP_SIZE_ROWS = 4
        , LMP_SIZE_COLS = 5
        ;
    private native static void lammpsGatherCompute_(long aLmpPtr, @NotNull String aName, int aColNum, double @NotNull[] rData) throws LmpException, MPIException;
    private native static void lammpsExtractCompute_(long aLmpPtr, @NotNull String aName, int aDataStyle, int aDataType, int aRowNum, int aColNum, double @NotNull[] rData) throws LmpException;
    private native static int lammpsExtractComputeSize_(long aLmpPtr, @NotNull String aName, int aDataStyle, int aDataType) throws LmpException;
    private native static long lammpsExtractComputeCPointer_(long aLmpPtr, @NotNull String aName, int aDataStyle, int aDataType) throws LmpException;
    
    /**
     * Scatter the named per-atom, per-atom fix, per-atom compute,
     * or fix property/atom-based entity in data to all processes.
     * <p>
     * This is a wrapper around the {@code lammps_scatter()} function of the C-library interface.
     * This subroutine takes data stored in a one-dimensional array supplied by the user and
     * scatters them to all atoms on all processes. The data must be ordered by atom ID,
     * with the requirement that the IDs be consecutive. Use lammps_scatter_subset() to
     * scatter data for some (or all) atoms, unordered.
     * @param aName name of the property
     * @param aData Matrix of data to set
     * @see <a href="https://docs.lammps.org/Library_scatter.html#_CPPv414lammps_scatterPvPKciiPv">
     * lammps_scatter() </a>
     */
    @SuppressWarnings("DuplicateBranchesInSwitch")
    public void setAtomDataOf(String aName, IMatrix aData) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        switch(aName) {
        case "id":          {setAtomDataOf(aName, aData, false); return;}
        case "type":        {setAtomDataOf(aName, aData, false); return;}
        case "mask":        {setAtomDataOf(aName, aData, false); return;}
        case "image":       {setAtomDataOf(aName, aData, false); return;}
        case "x":           {setAtomDataOf(aName, aData, true ); return;}
        case "v":           {setAtomDataOf(aName, aData, true ); return;}
        case "f":           {setAtomDataOf(aName, aData, true ); return;}
        case "molecule":    {setAtomDataOf(aName, aData, false); return;}
        case "q":           {setAtomDataOf(aName, aData, true ); return;}
        case "mu":          {setAtomDataOf(aName, aData, true ); return;}
        case "omega":       {setAtomDataOf(aName, aData, true ); return;}
        case "angmom":      {setAtomDataOf(aName, aData, true ); return;}
        case "torque":      {setAtomDataOf(aName, aData, true ); return;}
        case "radius":      {setAtomDataOf(aName, aData, true ); return;}
        case "rmass":       {setAtomDataOf(aName, aData, true ); return;}
        case "ellipsoid":   {setAtomDataOf(aName, aData, false); return;}
        case "line":        {setAtomDataOf(aName, aData, false); return;}
        case "tri":         {setAtomDataOf(aName, aData, false); return;}
        case "body":        {setAtomDataOf(aName, aData, false); return;}
        case "quat":        {setAtomDataOf(aName, aData, true ); return;}
        case "temperature": {setAtomDataOf(aName, aData, true ); return;}
        case "heatflow":    {setAtomDataOf(aName, aData, true ); return;}
        default: {
            if (aName.startsWith("i_")) {
                setAtomDataOf(aName, aData, false);
            } else
            if (aName.startsWith("d_")) {
                setAtomDataOf(aName, aData, true );
            } else {
                throw new IllegalArgumentException("Unexpected name: "+aName+", use setAtomDataOf(aName, aData, aIsDouble) to scatter this atom data.");
            }
        }}
    }
    public void setAtomDataOf(String aName, IMatrix aData, boolean aIsDouble) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        if (aName == null) throw new NullPointerException();
        checkThread();
        if ((aData instanceof RowMatrix) || ((aData instanceof ColumnMatrix) && aData.columnNumber()==1)) {
            lammpsScatter_(mLmpPtr.mPtr, aName, aIsDouble, aData.rowNumber(), aData.columnNumber(), ((DoubleArrayMatrix)aData).internalData());
        } else {
            lammpsScatter_(mLmpPtr.mPtr, aName, aIsDouble, aData.rowNumber(), aData.columnNumber(), aData.asVecRow().data());
        }
    }
    private native static void lammpsScatter_(long aLmpPtr, @NotNull String aName, boolean aIsDouble, int aAtomNum, int aCount, double @NotNull[] aData) throws LmpException;
    
    /** 提供 {@link Lmpdat} 格式的获取质量 */
    public IVector masses() throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        IVector tMasses = localAtomDataOf("mass", 1, atomTypeNumber()+1, 1).asVecRow();
        return tMasses.subVec(1, tMasses.size());
    }
    public double mass(int aType) throws LmpException {return localAtomDataOf("mass", 1, atomTypeNumber()+1, 1).get(aType, 1);}
    
    /**
     * 通过 {@link #atomDataOf} 直接构造一个 {@link Lmpdat}，
     * 可以避免走文件管理系统
     * @param aNoVelocities 是否关闭速度信息，默认 false（包含速度信息）
     * @return 一个类似于读取 lammps data 文件后得到的 {@link Lmpdat}
     * @author liqa
     */
    public Lmpdat lmpdat(boolean aNoVelocities) throws LmpException, MPIException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        // 获取数据
        RowIntMatrix tID = atomIntDataOf("id");
        RowIntMatrix tType = atomIntDataOf("type");
        RowMatrix tXYZ = atomDataOf("x");
        @Nullable RowMatrix tVelocities = aNoVelocities ? null : atomDataOf("v");
        IVector tMasses = masses();
        // 构造 Lmpdat，其余数据由于可以直接存在 Lmpdat 中，因此不用归还
        return new Lmpdat(tMasses.size(), box(), tMasses, tID.asVecRow(), tType.asVecRow(), tXYZ, tVelocities);
    }
    public Lmpdat lmpdat() throws LmpException, MPIException {
        return lmpdat(false);
    }
    @VisibleForTesting public Lmpdat data(boolean aNoVelocities) throws LmpException, MPIException {return lmpdat(aNoVelocities);}
    @VisibleForTesting public Lmpdat data() throws LmpException, MPIException {return lmpdat();}
    
    /**
     * 更加易用的方法，类似于 lammps 的 {@code read_data} 命令，但是不需要走文件管理器；
     * 实际实现过程有些区别
     * @param aLmpdat 作为输入的原子数据
     * @param aDiscardID 是否直接丢弃原子的 id 信息，让 lammps 自动创建 id，默认为 {@code false}
     */
    public void loadLmpdat(final Lmpdat aLmpdat, boolean aDiscardID) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        checkThread();
        LmpBox tBox = aLmpdat.box();
        if (tBox.isPrism()) {
        command("region          box prism "+tBox.xlo()+" "+tBox.xhi()+" "+tBox.ylo()+" "+tBox.yhi()+" "+tBox.zlo()+" "+tBox.zhi()+" "+tBox.xy()+" "+tBox.xz()+" "+tBox.yz());
        } else {
        command("region          box block "+tBox.xlo()+" "+tBox.xhi()+" "+tBox.ylo()+" "+tBox.yhi()+" "+tBox.zlo()+" "+tBox.zhi());
        }
        int tAtomTypeNum = aLmpdat.atomTypeNumber();
        command("create_box      "+tAtomTypeNum+" box");
        if (aLmpdat.hasMass()) for (int tType = 1; tType <= tAtomTypeNum; ++tType) {
        double tMass = aLmpdat.mass(tType);
        if (!Double.isNaN(tMass)) {
        command("mass            "+tType+" "+tMass);
        }}
        IIntVector tIDs = aLmpdat.ids();
        @Nullable IntVector tBufIDs = aDiscardID ? null : tIDs.toBuf();
        IIntVector tTypes = aLmpdat.types(); IntVector tBufTypes = tTypes.toBuf();
        IVector tPositionsVec = aLmpdat.positions().asVecRow(); Vector tBufPositionsVec = tPositionsVec.toBuf();
        @Nullable IMatrix tVelocities = aLmpdat.velocities();
        @Nullable IVector tVelocitiesVec = tVelocities==null ? null : tVelocities.asVecRow();
        @Nullable Vector tBufVelocitiesVec = tVelocitiesVec==null ? null : tVelocitiesVec.toBuf();
        try {
            lammpsCreateAtoms_(mLmpPtr.mPtr, aLmpdat.atomNumber(), tBufIDs==null ? null : tBufIDs.internalData(), tBufTypes.internalData(), tBufPositionsVec.internalData(), tBufVelocitiesVec==null ? null : tBufVelocitiesVec.internalData(), null, false);
        } finally {
            if (tBufIDs!=null) tIDs.releaseBuf(tBufIDs, true);
            tTypes.releaseBuf(tBufTypes, true);
            tPositionsVec.releaseBuf(tBufPositionsVec, true);
            if (tVelocitiesVec!=null && tBufVelocitiesVec!=null) tVelocitiesVec.releaseBuf(tBufVelocitiesVec, true);
        }
    }
    public void loadLmpdat(Lmpdat aLmpdat) throws LmpException {loadLmpdat(aLmpdat, false);}
    public void loadData(IAtomData aAtomData, boolean aDiscardID) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        checkThread();
        // 需要统一转换成 lammps style 的 box
        if (!aAtomData.isLmpStyle()) {aAtomData = Lmpdat.of(aAtomData);}
        if (aAtomData instanceof Lmpdat) {loadLmpdat((Lmpdat)aAtomData, aDiscardID); return;}
        IBox tBox = aAtomData.box();
        if (tBox.isPrism()) {
        command("region          box prism 0 "+tBox.x()+" 0 "+tBox.y()+" 0 "+tBox.z()+" "+tBox.xy()+" "+tBox.xz()+" "+tBox.yz());
        } else {
        command("region          box block 0 "+tBox.x()+" 0 "+tBox.y()+" 0 "+tBox.z());
        }
        int tAtomTypeNum = aAtomData.atomTypeNumber();
        command("create_box      "+tAtomTypeNum+" box");
        if (aAtomData.hasMass()) for (int tType = 1; tType <= tAtomTypeNum; ++tType) {
        double tMass = aAtomData.mass(tType);
        if (!Double.isNaN(tMass)) {
        command("mass            "+tType+" "+tMass);
        }}
        creatAtoms(aAtomData.atoms(), false, aDiscardID);
    }
    public void loadData(IAtomData aAtomData) throws LmpException {loadData(aAtomData, false);}
    public void loadData(Lmpdat aLmpdat, boolean aDiscardID) throws LmpException {loadLmpdat(aLmpdat, aDiscardID);}
    public void loadData(Lmpdat aLmpdat) throws LmpException {loadLmpdat(aLmpdat);}
    
    /**
     * Create N atoms from list of coordinates and properties
     * <p>
     * This function is a wrapper around the {@code lammps_create_atoms()} function of the C-library interface.
     * @param aAtoms List of Atoms
     * @param aShrinkExceed whether to expand shrink-wrap boundaries if atoms are outside the box (false in default)
     * @param aDiscardID 是否直接丢弃原子的 id 信息，让 lammps 自动创建 id，默认为 {@code false}
     */
    public void creatAtoms(List<? extends IAtom> aAtoms, boolean aShrinkExceed, boolean aDiscardID) throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        checkThread();
        final IAtom tFirst = UT.Code.first(aAtoms);
        final boolean tHasVelocities = tFirst.hasVelocity();
        final boolean tHasID = !aDiscardID && tFirst.hasID();
        final int tAtomNum = aAtoms.size();
        int[] rID = tHasID ? IntArrayCache.getArray(tAtomNum) : null;
        int[] rType = IntArrayCache.getArray(tAtomNum);
        double[] rXYZ = DoubleArrayCache.getArray(tAtomNum*3);
        double[] rVelocities = tHasVelocities ? DoubleArrayCache.getArray(tAtomNum*3) : null;
        int i = 0, j1 = 0, j2 = 0;
        for (IAtom tAtom : aAtoms) {
            if (tHasID) {
                int tID = tAtom.id();
                rID[i] = tID<=0 ? (i+1) : tID;
            }
            rType[i] = tAtom.type();
            ++i;
            rXYZ[j1] = tAtom.x(); ++j1;
            rXYZ[j1] = tAtom.y(); ++j1;
            rXYZ[j1] = tAtom.z(); ++j1;
            if (tHasVelocities) {
                rVelocities[j2] = tAtom.vx(); ++j2;
                rVelocities[j2] = tAtom.vy(); ++j2;
                rVelocities[j2] = tAtom.vz(); ++j2;
            }
        }
        lammpsCreateAtoms_(mLmpPtr.mPtr, tAtomNum, rID, rType, rXYZ, rVelocities, null, aShrinkExceed);
        if (tHasID) IntArrayCache.returnArray(rID);
        IntArrayCache.returnArray(rType);
        DoubleArrayCache.returnArray(rXYZ);
        if (tHasVelocities) DoubleArrayCache.returnArray(rVelocities);
    }
    public void creatAtoms(List<? extends IAtom> aAtoms, boolean aShrinkExceed) throws LmpException {creatAtoms(aAtoms, aShrinkExceed, false);}
    public void creatAtoms(List<? extends IAtom> aAtoms) throws LmpException {creatAtoms(aAtoms, false);}
    private native static void lammpsCreateAtoms_(long aLmpPtr, int aAtomNum, int @Nullable[] aID, int @NotNull[] aType, double @NotNull[] aXYZ, double @Nullable[] aVelocities, int @Nullable[] aImage, boolean aShrinkExceed) throws LmpException;
    
    /**
     * lammps clear 指令
     */
    public void clear() throws LmpException {
        if (mDead) throw new IllegalStateException("This NativeLmp is dead");
        command("clear");
    }
    
    /**
     * Explicitly delete a LAMMPS instance through the C-library interface.
     * <p>
     * This is a wrapper around the {@code lammps_close()} function of the C-library interface.
     */
    public void shutdown() {
        if (!mDead) {
            mDead = true;
            mLmpPtr.dispose();
        }
    }
}
