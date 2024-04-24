package jse.lmp;

import jse.atom.IAtom;
import jse.atom.IAtomData;
import jse.atom.IXYZ;
import jse.cache.DoubleArrayCache;
import jse.cache.IntArrayCache;
import jse.cache.IntMatrixCache;
import jse.cache.MatrixCache;
import jse.clib.JNIUtil;
import jse.clib.MiMalloc;
import jse.code.OS;
import jse.code.UT;
import jse.io.IInFile;
import jse.math.matrix.*;
import jse.math.vector.IIntVector;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import jse.parallel.MPI;
import jse.parallel.MPIException;
import jse.vasp.IVaspCommonData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.net.URL;
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
 * <a href="https://docs.lammps.org/Library.html/">
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
        private Conf() {}
        private final static String DEFAULT_LMP_TAG = "stable_2Aug2023_update2";
        
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
        
        /**
         * 自定义构建 lammps 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new HashMap<>();
        
        /**
         * 自定义构建 lammps 以及 lmpjni 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_LMP"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_LMP", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_LMP"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_LMP"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
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
         * 在编译 lammps 之前是否进行 clean 操作，
         * 这会大大增加二次编译的时间
         */
        public static boolean CLEAN = false;
        
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
    }
    
    private final static String LMP_ROOT = JAR_DIR+"lmp/";
    private final static String LMPJNI_LIB_DIR;
    private final static String LMPJNI_LIB_PATH;
    private final static String[] LMPJNI_SRC_NAME = {
          "jse_lmp_NativeLmp.c"
        , "jse_lmp_NativeLmp.h"
    };
    private final static @Nullable String NATIVELMP_SRC_DIR; // 记录自动下载的 lammps 的目录，包含可以重新编译的 lammps 源码，和 Conf.LMP_HOME 的 null 恰好相反
    private final static String NATIVELMP_INCLUDE_DIR;
    private final static String NATIVELMP_LIB_DIR;
    private final static String NATIVELMP_LIB_PATH;
    private final static String NATIVELMP_LLIB_PATH;
    private final static String NATIVE_DIR_NAME = "native", BUILD_DIR_NAME = IS_WINDOWS ? "build-win" : (IS_MAC ? "build-mac" : "build");
    
    private final static String EXECUTABLE_NAME;
    private final static String[] DEFAULT_ARGS;
    
    private static String cmakeInitCmdNativeLmp_() {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        // 这里设置 C/C++ 编译器（如果有）
        if (Conf.CMAKE_C_COMPILER   != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="  + Conf.CMAKE_C_COMPILER   );}
        if (Conf.CMAKE_CXX_COMPILER != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+ Conf.CMAKE_CXX_COMPILER );}
        if (Conf.CMAKE_C_FLAGS      != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS='"    + Conf.CMAKE_C_FLAGS  +"'");}
        if (Conf.CMAKE_CXX_FLAGS    != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"  + Conf.CMAKE_CXX_FLAGS+"'");}
        // 初始化使用上一个目录的 CMakeList.txt
        rCommand.add("../cmake");
        return String.join(" ", rCommand);
    }
    private static String cmakeInitCmdLmpJni_() {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        // 这里设置 C/C++ 编译器（如果有）
        if (Conf.CMAKE_C_COMPILER_LMPJNI  !=null || Conf.CMAKE_C_COMPILER  !=null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="  + (Conf.CMAKE_C_COMPILER_LMPJNI  ==null ? Conf.CMAKE_C_COMPILER   : Conf.CMAKE_C_COMPILER_LMPJNI  )    );}
        if (Conf.CMAKE_CXX_COMPILER_LMPJNI!=null || Conf.CMAKE_CXX_COMPILER!=null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+ (Conf.CMAKE_CXX_COMPILER_LMPJNI==null ? Conf.CMAKE_CXX_COMPILER : Conf.CMAKE_CXX_COMPILER_LMPJNI)    );}
        if (Conf.CMAKE_C_FLAGS_LMPJNI     !=null || Conf.CMAKE_C_FLAGS     !=null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS='"    + (Conf.CMAKE_C_FLAGS_LMPJNI     ==null ? Conf.CMAKE_C_FLAGS      : Conf.CMAKE_C_FLAGS_LMPJNI     )+"'");}
        if (Conf.CMAKE_CXX_FLAGS_LMPJNI   !=null || Conf.CMAKE_CXX_FLAGS   !=null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"  + (Conf.CMAKE_CXX_FLAGS_LMPJNI   ==null ? Conf.CMAKE_CXX_FLAGS    : Conf.CMAKE_CXX_FLAGS_LMPJNI   )+"'");}
        // 初始化使用上一个目录的 CMakeList.txt
        rCommand.add("..");
        return String.join(" ", rCommand);
    }
    private static String cmakeSettingCmdNativeLmp_() throws IOException {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        // 设置输出动态链接库
        rCommand.add("-D"); rCommand.add("BUILD_SHARED_LIBS=ON");
        // 设置抛出错误
        rCommand.add("-D"); rCommand.add("LAMMPS_EXCEPTIONS:BOOL=ON");
        // 设置编译模式 Release
        rCommand.add("-D"); rCommand.add("CMAKE_BUILD_TYPE=Release");
        // 设置构建输出目录为 lib
        UT.IO.makeDir(NATIVELMP_LIB_DIR); // 初始化一下这个目录避免意料外的问题
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH='"+ NATIVELMP_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH='"+ NATIVELMP_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH='"+ NATIVELMP_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH='"+ NATIVELMP_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH='"+ NATIVELMP_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH='"+ NATIVELMP_LIB_DIR +"'");
        // 添加额外的设置参数
        for (Map.Entry<String, String> tEntry : Conf.CMAKE_SETTING.entrySet()) {
            rCommand.add("-D"); rCommand.add(String.format("%s=%s", tEntry.getKey(), tEntry.getValue()));
        }
        rCommand.add(".");
        return String.join(" ", rCommand);
    }
    private static String cmakeSettingCmdLmpJni_() throws IOException {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        rCommand.add("-D"); rCommand.add("JSE_USE_MIMALLOC="                  +(Conf.USE_MIMALLOC           ?"ON":"OFF"));
        rCommand.add("-D"); rCommand.add("JSE_LAMMPS_IS_OLD="                 +(Conf.IS_OLD                 ?"ON":"OFF"));
        rCommand.add("-D"); rCommand.add("JSE_LAMMPS_HAS_EXCEPTIONS="         +(Conf.HAS_EXCEPTIONS         ?"ON":"OFF"));
        rCommand.add("-D"); rCommand.add("JSE_LAMMPS_EXCEPTIONS_NULL_SUPPORT="+(Conf.EXCEPTIONS_NULL_SUPPORT?"ON":"OFF"));
        // 设置构建输出目录为 lib
        UT.IO.makeDir(LMPJNI_LIB_DIR); // 初始化一下这个目录避免意料外的问题
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH='"+ LMPJNI_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH='"+ LMPJNI_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH='"+ LMPJNI_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH='"+ LMPJNI_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH='"+ LMPJNI_LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH='"+ LMPJNI_LIB_DIR +"'");
        rCommand.add(".");
        return String.join(" ", rCommand);
    }
    
    private static @NotNull String initNativeLmp_() throws Exception {
        // 检测 cmake，这里要求一定要有 cmake 环境
        EXEC.setNoSTDOutput().setNoERROutput();
        boolean tNoCmake = EXEC.system("cmake --version") != 0;
        EXEC.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCmake) throw new Exception("NATIVE_LMP BUILD ERROR: No cmake environment.");
        String tWorkingDir = WORKING_DIR_OF("nativelmp");
        // 如果已经存在则先删除
        UT.IO.removeDir(tWorkingDir);
        // 如果有 NATIVELMP_SRC_DIR 但是不合法，则需要下载 lammps
        if (NATIVELMP_SRC_DIR!=null && !UT.IO.isDir(NATIVELMP_SRC_DIR)) {
            String tNativeLmpZipPath = tWorkingDir+"lammps-"+Conf.LMP_TAG+".zip";
            System.out.printf("NATIVE_LMP INIT INFO: No lammps in %s, downloading the source code...\n", NATIVELMP_SRC_DIR);
            UT.IO.copy(new URL(String.format("https://github.com/lammps/lammps/archive/refs/tags/%s.zip", Conf.LMP_TAG)), tNativeLmpZipPath);
            System.out.println("NATIVE_LMP INIT INFO: Lammps source code downloading finished.");
            // 解压 lammps 到临时目录，如果已经存在则直接清空此目录
            String tNativeLmpTempSrcDir = tWorkingDir+"temp/";
            UT.IO.removeDir(tNativeLmpTempSrcDir);
            UT.IO.zip2dir(tNativeLmpZipPath, tNativeLmpTempSrcDir);
            String tNativeLmpTempSrcDir2 = null;
            for (String tName : UT.IO.list(tNativeLmpTempSrcDir)) {
                if (tName!=null && !tName.isEmpty() && !tName.equals(".") && !tName.equals("..")) {
                    String tNativeLmpTempDir1 = tNativeLmpTempSrcDir + tName + "/";
                    if (UT.IO.isDir(tNativeLmpTempDir1)) {
                        tNativeLmpTempSrcDir2 = tNativeLmpTempDir1;
                        break;
                    }
                }
            }
            if (tNativeLmpTempSrcDir2 == null) throw new Exception("NATIVE_LMP INIT ERROR: No lammps in "+tNativeLmpTempSrcDir);
            // 移动到需要的目录
            UT.IO.move(tNativeLmpTempSrcDir2, NATIVELMP_SRC_DIR);
        }
        // 执行 lammps 构建
        System.out.println("NATIVE_LMP INIT INFO: Building lammps from source code...");
        // 创建一下 LMP_HOME 文件夹（构建目录）
        UT.IO.makeDir(Conf.LMP_HOME);
        // 编译 lammps，直接通过系统指令来编译，关闭输出
        EXEC.setNoSTDOutput().setWorkingDir(Conf.LMP_HOME);
        // 初始化 cmake
        EXEC.system(cmakeInitCmdNativeLmp_());
        // 设置参数
        EXEC.system(cmakeSettingCmdNativeLmp_());
        // 如果设置 CLEAN 则进行 clean 操作
        if (Conf.CLEAN) EXEC.system("cmake --build . --target clean");
        // 最后进行构造操作
        EXEC.system("cmake --build . --config Release");
        EXEC.setNoSTDOutput(false).setWorkingDir(null);
        // 简单检测一下是否编译成功
        @Nullable String tLibName = LIB_NAME_IN(NATIVELMP_LIB_DIR, "lammps");
        if (tLibName == null) throw new Exception("NATIVE_LMP BUILD ERROR: Lammps build Failed, No lammps lib in '"+NATIVELMP_LIB_DIR+"'");
        // 完事后移除临时解压得到的源码（以及可能存在的临时下载的 lammps 源码压缩包）
        UT.IO.removeDir(tWorkingDir);
        System.out.println("NATIVE_LMP INIT INFO: lammps libraries successfully installed.");
        // 输出安装完成后的库名称
        return tLibName;
    }
    private static @NotNull String initLmpJni_() throws Exception {
        // 检测 cmake，这里要求一定要有 cmake 环境
        EXEC.setNoSTDOutput().setNoERROutput();
        boolean tNoCmake = EXEC.system("cmake --version") != 0;
        EXEC.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCmake) throw new Exception("NATIVE_LMP BUILD ERROR: No cmake environment.");
        String tWorkingDir = WORKING_DIR_OF("lmpjni");
        // 如果已经存在则先删除
        UT.IO.removeDir(tWorkingDir);
        // 从内部资源解压到临时目录
        String tSrcDir = tWorkingDir+"src/";
        for (String tName : LMPJNI_SRC_NAME) {
            UT.IO.copy(UT.IO.getResource("lmp/src/"+tName), tSrcDir+tName);
        }
        // 这里对 CMakeLists.txt 特殊处理
        UT.IO.map(UT.IO.getResource("lmp/src/CMakeLists.txt"), tSrcDir+"CMakeLists.txt", line -> {
            // 替换其中的 jniutil 库路径为设置好的路径
            line = line.replace("$ENV{JSE_JNIUTIL_INCLUDE_DIR}", JNIUtil.INCLUDE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
            // 替换其中的 lammps 库路径为设置好的路径
            line = line.replace("$ENV{JSE_LMP_INCLUDE_DIR}", NATIVELMP_INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                       .replace("$ENV{JSE_LMP_LIB_PATH}"   , NATIVELMP_LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
            // 替换其中的 mimalloc 库路径为设置好的路径
            if (Conf.USE_MIMALLOC) {
            line = line.replace("$ENV{JSE_MIMALLOC_INCLUDE_DIR}", MiMalloc.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                       .replace("$ENV{JSE_MIMALLOC_LIB_PATH}"   , MiMalloc.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
            }
            return line;
        });
        // 执行 lmpjni 构建
        System.out.println("NATIVE_LMP INIT INFO: Building lmpjni from source code...");
        String tBuildDir = tSrcDir+"build/";
        UT.IO.makeDir(tBuildDir);
        // 直接通过系统指令来编译 lmpjni 的库，关闭输出
        EXEC.setNoSTDOutput().setWorkingDir(tBuildDir);
        // 初始化 cmake
        EXEC.system(cmakeInitCmdLmpJni_());
        // 设置参数
        EXEC.system(cmakeSettingCmdLmpJni_());
        // 最后进行构造操作
        EXEC.system("cmake --build . --config Release");
        EXEC.setNoSTDOutput(false).setWorkingDir(null);
        // 简单检测一下是否编译成功
        @Nullable String tLibName = LIB_NAME_IN(LMPJNI_LIB_DIR, "lmpjni");
        if (tLibName == null) throw new Exception("NATIVE_LMP BUILD ERROR: lmpjni build Failed, No lmpjni lib in '"+ LMPJNI_LIB_DIR+"'");
        // 完事后移除临时解压得到的源码
        UT.IO.removeDir(tWorkingDir);
        System.out.println("NATIVE_LMP INIT INFO: lmpjni libraries successfully installed.");
        // 输出安装完成后的库名称
        return tLibName;
    }
    
    static {
        InitHelper.INITIALIZED = true;
        // 依赖 jniutil
        JNIUtil.InitHelper.init();
        // 如果开启了 USE_MIMALLOC 则增加 MiMalloc 依赖
        if (Conf.USE_MIMALLOC) MiMalloc.InitHelper.init();
        
        // 先规范化 LMP_HOME 的格式
        if (Conf.LMP_HOME == null) {
            String tNativeDir;
            if (Conf.LMP_TAG != null) {
                // 如果有手动设定 LMP_TAG 则永远使用带有 tag 的
                tNativeDir = LMP_ROOT+NATIVE_DIR_NAME+"-"+Conf.LMP_TAG+"/";
            } else {
                // 否则需要设置 DEFAULT_LMP_TAG 并尝试不带 tag 的
                Conf.LMP_TAG = Conf.DEFAULT_LMP_TAG;
                tNativeDir = LMP_ROOT+NATIVE_DIR_NAME+"-"+Conf.LMP_TAG+"/";
                // 如果不存在并且 tag 没变则会尝试检测不带 tag 保持兼容
                if (!UT.IO.isDir(tNativeDir)) {
                    String tShortLmpDir = LMP_ROOT+NATIVE_DIR_NAME+"/";
                    if (UT.IO.isDir(tShortLmpDir)) {
                        tNativeDir = tShortLmpDir;
                    }
                }
            }
            NATIVELMP_SRC_DIR = tNativeDir;
            Conf.LMP_HOME = NATIVELMP_SRC_DIR+BUILD_DIR_NAME+"/";
        } else {
            NATIVELMP_SRC_DIR = null;
            Conf.LMP_HOME = UT.IO.toInternalValidDir(UT.IO.toAbsolutePath(Conf.LMP_HOME));
        }
        NATIVELMP_INCLUDE_DIR = Conf.LMP_HOME+"includes/";
        NATIVELMP_LIB_DIR = Conf.LMP_HOME+"lib/";
        if (Conf.REDIRECT_LMP_LIB == null) {
            @Nullable String tNativeLmpLibName = LIB_NAME_IN(NATIVELMP_LIB_DIR, "lammps");
            // 如果不存在 native lib 则需要重新通过源码编译；这里分别初始化，而不是像原本一样两者混在一起
            if (Conf.REBUILD || tNativeLmpLibName==null) {
                System.out.println("NATIVE_LMP INIT INFO: Lammps libraries not found. Reinstalling (it will take a lot of time)...");
                try {tNativeLmpLibName = initNativeLmp_();} catch (Exception e) {throw new RuntimeException(e);}
            }
            NATIVELMP_LIB_PATH = NATIVELMP_LIB_DIR+tNativeLmpLibName;
            @Nullable String tNativeLmpLLibName = LLIB_NAME_IN(NATIVELMP_LIB_DIR, "lammps");
            NATIVELMP_LLIB_PATH = tNativeLmpLLibName==null ? NATIVELMP_LIB_PATH : (NATIVELMP_LIB_DIR+tNativeLmpLLibName);
        } else {
            if (DEBUG) System.out.println("NATIVE_LMP INIT INFO: Lammps libraries are redirected to '"+Conf.REDIRECT_LMP_LIB+"'");
            NATIVELMP_LIB_PATH = Conf.REDIRECT_LMP_LIB;
            NATIVELMP_LLIB_PATH = Conf.REDIRECT_LMP_LLIB==null ? Conf.REDIRECT_LMP_LIB : Conf.REDIRECT_LMP_LLIB;
        }
        
        // 现在 uniqueID 不再包含这个 tag（确实当时也想到了），因为已经包含到了 LMP_HOME 中
        // 在这里初始化保证顺序合理
        LMPJNI_LIB_DIR = LMP_ROOT + UT.Code.uniqueID(VERSION, Conf.LMP_HOME, Conf.USE_MIMALLOC, Conf.HAS_EXCEPTIONS, Conf.EXCEPTIONS_NULL_SUPPORT) + "/";
        if (Conf.REDIRECT_LMPJNI_LIB == null) {
            @Nullable String tLmpJniLibName = LIB_NAME_IN(LMPJNI_LIB_DIR, "lmpjni");
            // 如果不存在 jni lib 则需要重新通过源码编译
            if (tLmpJniLibName == null) {
                System.out.println("NATIVE_LMP INIT INFO: lmpjni libraries not found. Reinstalling...");
                try {tLmpJniLibName = initLmpJni_();} catch (Exception e) {throw new RuntimeException(e);}
            }
            LMPJNI_LIB_PATH = LMPJNI_LIB_DIR+tLmpJniLibName;
        } else {
            if (DEBUG) System.out.println("NATIVE_LMP INIT INFO: lmpjni libraries are redirected to '"+Conf.REDIRECT_LMPJNI_LIB+"'");
            LMPJNI_LIB_PATH = Conf.REDIRECT_LMPJNI_LIB;
        }
        
        // 设置库路径
        System.load(UT.IO.toAbsolutePath(NATIVELMP_LIB_PATH));
        System.load(UT.IO.toAbsolutePath(LMPJNI_LIB_PATH));
        
        // 设置 EXECUTABLE_NAME
        String tExecutableName = UT.IO.toFileName(NATIVELMP_LIB_PATH);
        int tFirstDot = tExecutableName.indexOf(".");
        EXECUTABLE_NAME = tFirstDot>=0 ? tExecutableName.substring(0, tFirstDot) : tExecutableName;
        DEFAULT_ARGS = new String[] {EXECUTABLE_NAME, "-log", "none"};
    }
    
    
    private final long mLmpPtr;
    private final long mInitTheadID; // lammps 需要保证初始化时的线程和调用时的是相同的
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
        if (aArgs != null) System.arraycopy(aArgs, 0, tArgs, 1, aArgs.length);
        mLmpPtr = aComm==null ? lammpsOpen_(tArgs) : lammpsOpen_(tArgs, aComm.ptr_());
        if (mLmpPtr==0 || mLmpPtr==-1) throw new LmpException("Failed to init a NativeLmp: "+mLmpPtr);
        mInitTheadID = Thread.currentThread().getId();
    }
    public NativeLmp(Collection<? extends CharSequence> aArgs, @Nullable MPI.Comm aComm) throws LmpException {this(UT.Text.toArray(aArgs), aComm);}
    public NativeLmp(String... aArgs) throws LmpException {this(aArgs, null);}
    public NativeLmp(@Nullable MPI.Comm aComm, String... aArgs) throws LmpException {this(aArgs, aComm);}
    public NativeLmp(@Nullable MPI.Comm aComm) throws LmpException {this((String[])null, aComm);}
    public NativeLmp() throws LmpException {this((String[])null);}
    private native static long lammpsOpen_(String[] aArgs, long aComm) throws LmpException;
    private native static long lammpsOpen_(String[] aArgs) throws LmpException;
    
    public boolean threadValid() {
        return Thread.currentThread().getId() == mInitTheadID;
    }
    public void checkThread() throws LmpException {
        long tCurrentThreadID = Thread.currentThread().getId();
        if (tCurrentThreadID != mInitTheadID) throw new LmpException("Thread of NativeLmp MUST be SAME: "+tCurrentThreadID+" vs "+mInitTheadID);
    }
    
    /**
     * Return a numerical representation of the LAMMPS version in use.
     * <p>
     * This is a wrapper around the {@code lammps_version()} function of the C-library interface.
     * @return version number
     */
    public int version() throws LmpException {
        checkThread();
        return lammpsVersion_(mLmpPtr);
    }
    private native static int lammpsVersion_(long aLmpPtr) throws LmpException;
    
    /**
     * Get the MPI communicator in use by the current LAMMPS instance
     * <p>
     * This is a wrapper around the {@code lammps_file()} function of the C-library interface.
     * It will return {@code null} if the LAMMPS library was compiled without MPI support.
     * @return the {@link MPI.Comm} of this NativeLmp
     */
    public MPI.Comm comm() throws LmpException {return MPI.Comm.of(lammpsComm_(mLmpPtr));}
    private native static long lammpsComm_(long aLmpPtr) throws LmpException;
    
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
        checkThread();
        lammpsFile_(mLmpPtr, UT.IO.toAbsolutePath(aPath));
    }
    private native static void lammpsFile_(long aLmpPtr, String aPath) throws LmpException;
    
    /**
     * 提供一个更加易用的直接使用 {@link IInFile}
     * 作为输入的 file 方法，底层会使用 {@link #commands}
     * 来执行多行命令
     * @param aLmpIn 需要读取的 lammps in 文件
     * @author liqa
     */
    public void file(IInFile aLmpIn) throws IOException, LmpException {
        checkThread();
        commands(aLmpIn.toLines().toArray(ZL_STR));
    }
    
    /**
     * Process a single LAMMPS input command from a string.
     * <p>
     * This is a wrapper around the {@code lammps_command()} function of the C-library interface.
     * @param aCmd a single lammps command
     */
    public void command(String aCmd) throws LmpException {
        checkThread();
        lammpsCommand_(mLmpPtr, aCmd);
    }
    private native static void lammpsCommand_(long aLmpPtr, String aCmd) throws LmpException;
    
    /**
     * Process multiple LAMMPS input commands from a list of strings.
     * <p>
     * This is a wrapper around the {@code lammps_commands_list()} function of the C-library interface.
     * @param aCmds a list of lammps commands
     */
    public void commands(String... aCmds) throws LmpException {
        checkThread();
        lammpsCommandsList_(mLmpPtr, aCmds);
    }
    private native static void lammpsCommandsList_(long aLmpPtr, String[] aCmds) throws LmpException;
    
    /**
     * Process a block of LAMMPS input commands from a string.
     * <p>
     * This is a wrapper around the {@code lammps_commands_string()} function of the C-library interface.
     * @param aMultiCmd text block of lammps commands
     */
    public void commands(String aMultiCmd) throws LmpException {
        checkThread();
        lammpsCommandsString_(mLmpPtr, aMultiCmd);
    }
    private native static void lammpsCommandsString_(long aLmpPtr, String aMultiCmd) throws LmpException;
    
    /**
     * Get the total number of atoms in the LAMMPS instance.
     * <p>
     * This is a wrapper around the {@code lammps_get_natoms()} function of the C-library interface.
     * @return number of atoms
     */
    public int atomNumber() throws LmpException {
        checkThread();
        return (int)lammpsGetNatoms_(mLmpPtr);
    }
    /** @deprecated use {@link #atomNumber} or {@link #natoms} */ @Deprecated public final int atomNum() throws LmpException {return atomNumber();}
    @VisibleForTesting public int natoms() throws LmpException {return atomNumber();}
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
        checkThread();
        double[] rBox = DoubleArrayCache.getArray(15);
        lammpsExtractBox_(mLmpPtr, rBox);
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
    private native static void lammpsExtractBox_(long aLmpPtr, double[] rBox) throws LmpException;
    
    /**
     * Reset simulation box parameters
     * <p>
     * This is a wrapper around the {@code lammps_reset_box()} function of the C-library interface,
     * but in {@link LmpBoxPrism} order.
     */
    public void resetBox(double aXlo, double aXhi, double aYlo, double aYhi, double aZlo, double aZhi, double aXY, double aXZ, double aYZ) throws LmpException {
        checkThread();
        lammpsResetBox_(mLmpPtr, aXlo, aYlo, aZlo, aXhi, aYhi, aZhi, aXY, aYZ, aXZ);
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
        checkThread();
        return lammpsGetThermo_(mLmpPtr, aName);
    }
    private native static double lammpsGetThermo_(long aLmpPtr, String aName) throws LmpException;
    
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
        checkThread();
        return lammpsExtractSetting_(mLmpPtr, aName);
    }
    private native static int lammpsExtractSetting_(long aLmpPtr, String aName) throws LmpException;
    
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
        checkThread();
        RowMatrix rData = MatrixCache.getMatRow(atomNumber(), aColNum);
        lammpsGatherConcat_(mLmpPtr, aName, aIsDouble, aColNum, rData.internalData());
        return rData;
    }
    public RowIntMatrix fullAtomIntDataOf(String aName, int aColNum) throws LmpException, MPIException {
        checkThread();
        RowIntMatrix rData = IntMatrixCache.getMatRow(atomNumber(), aColNum);
        lammpsGatherConcatInt_(mLmpPtr, aName, aColNum, rData.internalData());
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
        checkThread();
        RowMatrix rData = MatrixCache.getMatRow(aRowNum, aColNum);
        lammpsExtractAtom_(mLmpPtr, aName, aDataType, aRowNum, aColNum, rData.internalData());
        return rData;
    }
    public RowIntMatrix localAtomIntDataOf(String aName, int aDataType, int aRowNum, int aColNum) throws LmpException {
        checkThread();
        RowIntMatrix rData = IntMatrixCache.getMatRow(aRowNum, aColNum);
        lammpsExtractAtomInt_(mLmpPtr, aName, aDataType, aRowNum, aColNum, rData.internalData());
        return rData;
    }
    private native static void lammpsGatherConcat_(long aLmpPtr, String aName, boolean aIsDouble, int aCount, double[] rData) throws LmpException, MPIException;
    private native static void lammpsGatherConcatInt_(long aLmpPtr, String aName, int aCount, int[] rData) throws LmpException, MPIException;
    private native static void lammpsExtractAtom_(long aLmpPtr, String aName, int aDataType, int aAtomNum, int aCount, double[] rData) throws LmpException;
    private native static void lammpsExtractAtomInt_(long aLmpPtr, String aName, int aDataType, int aAtomNum, int aCount, int[] rData) throws LmpException;
    private native static void lammpsExtractAtomLong_(long aLmpPtr, String aName, int aDataType, int aAtomNum, int aCount, long[] rData) throws LmpException;
    
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
        checkThread();
        if ((aData instanceof RowMatrix) || ((aData instanceof ColumnMatrix) && aData.columnNumber()==1)) {
            lammpsScatter_(mLmpPtr, aName, aIsDouble, aData.rowNumber(), aData.columnNumber(), ((DoubleArrayMatrix)aData).internalData());
        } else {
            lammpsScatter_(mLmpPtr, aName, aIsDouble, aData.rowNumber(), aData.columnNumber(), aData.asVecRow().data());
        }
    }
    private native static void lammpsScatter_(long aLmpPtr, String aName, boolean aIsDouble, int aAtomNum, int aCount, double[] aData) throws LmpException;
    
    /** 提供 {@link Lmpdat} 格式的获取质量 */
    public IVector masses() throws LmpException {
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
     * @author liqa
     */
    public void loadLmpdat(final Lmpdat aLmpdat) throws LmpException {
        checkThread();
        LmpBox tBox = aLmpdat.box();
        if (aLmpdat.isPrism()) {
        command(String.format("region          box prism %f %f %f %f %f %f %f %f %f", tBox.xlo(), tBox.xhi(), tBox.ylo(), tBox.yhi(), tBox.zlo(), tBox.zhi(), tBox.xy(), tBox.xz(), tBox.yz()));
        } else {
        command(String.format("region          box block %f %f %f %f %f %f",          tBox.xlo(), tBox.xhi(), tBox.ylo(), tBox.yhi(), tBox.zlo(), tBox.zhi()));
        }
        int tAtomTypeNum = aLmpdat.atomTypeNumber();
        command(String.format("create_box      %d box", tAtomTypeNum));
        IVector tMasses = aLmpdat.masses();
        if (tMasses != null) for (int i = 0; i < tAtomTypeNum; ++i) {
        command(String.format("mass            %d %f", i+1, tMasses.get(i)));
        }
        IIntVector tIDs = aLmpdat.ids(); IntVector tBufIDs = tIDs.toBuf();
        IIntVector tTypes = aLmpdat.types(); IntVector tBufTypes = tTypes.toBuf();
        IVector tPositionsVec = aLmpdat.positions().asVecRow(); Vector tBufPositionsVec = tPositionsVec.toBuf();
        @Nullable IMatrix tVelocities = aLmpdat.velocities();
        @Nullable IVector tVelocitiesVec = tVelocities==null ? null : tVelocities.asVecRow();
        @Nullable Vector tBufVelocitiesVec = tVelocitiesVec==null ? null : tVelocitiesVec.toBuf();
        try {
            lammpsCreateAtoms_(mLmpPtr, aLmpdat.atomNumber(), tBufIDs.internalData(), tBufTypes.internalData(), tBufPositionsVec.internalData(), tBufVelocitiesVec==null ? null : tBufVelocitiesVec.internalData(), null, false);
        } finally {
            tIDs.releaseBuf(tBufIDs, true);
            tTypes.releaseBuf(tBufTypes, true);
            tPositionsVec.releaseBuf(tBufPositionsVec, true);
            if (tVelocitiesVec!=null && tBufVelocitiesVec!=null) tVelocitiesVec.releaseBuf(tBufVelocitiesVec, true);
        }
    }
    public void loadData(IAtomData aAtomData) throws LmpException {
        checkThread();
        if (aAtomData instanceof Lmpdat) {loadLmpdat((Lmpdat)aAtomData); return;}
        IXYZ tBox = aAtomData.box();
        command(String.format("region          box block 0 %f 0 %f 0 %f", tBox.x(), tBox.y(), tBox.z()));
        int tAtomTypeNum = aAtomData.atomTypeNumber();
        command(String.format("create_box      %d box", tAtomTypeNum));
        // IVaspCommonData 包含原子种类字符，可以自动获取到质量
        if (aAtomData instanceof IVaspCommonData) {
        String[] tAtomTypes = ((IVaspCommonData)aAtomData).typeNames();
        if (tAtomTypes!=null && tAtomTypes.length>=tAtomTypeNum) for (int i = 0; i < tAtomTypeNum; ++i) {
        command(String.format("mass            %d %f", i+1, MASS.getOrDefault(tAtomTypes[i], -1.0)));
        }}
        creatAtoms(aAtomData.atoms());
    }
    public void loadData(Lmpdat aLmpdat) throws LmpException {loadLmpdat(aLmpdat);}
    
    /**
     * Create N atoms from list of coordinates and properties
     * <p>
     * This function is a wrapper around the {@code lammps_create_atoms()} function of the C-library interface.
     * @param aAtoms List of Atoms
     * @param aShrinkExceed whether to expand shrink-wrap boundaries if atoms are outside the box (false in default)
     */
    public void creatAtoms(List<? extends IAtom> aAtoms, boolean aShrinkExceed) throws LmpException {
        checkThread();
        final boolean tHasVelocities = UT.Code.first(aAtoms).hasVelocities();
        final int tAtomNum = aAtoms.size();
        int[] rID = IntArrayCache.getArray(tAtomNum);
        int[] rType = IntArrayCache.getArray(tAtomNum);
        double[] rXYZ = DoubleArrayCache.getArray(tAtomNum*3);
        double[] rVelocities = tHasVelocities ? DoubleArrayCache.getArray(tAtomNum*3) : null;
        int i = 0, j1 = 0, j2 = 0;
        for (IAtom tAtom : aAtoms) {
            rID[i] = tAtom.id();
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
        lammpsCreateAtoms_(mLmpPtr, tAtomNum, rID, rType, rXYZ, rVelocities, null, aShrinkExceed);
        IntArrayCache.returnArray(rID);
        IntArrayCache.returnArray(rType);
        DoubleArrayCache.returnArray(rXYZ);
        if (tHasVelocities) DoubleArrayCache.returnArray(rVelocities);
    }
    public void creatAtoms(List<? extends IAtom> aAtoms) throws LmpException {
        creatAtoms(aAtoms, false);
    }
    private native static void lammpsCreateAtoms_(long aLmpPtr, int aAtomNum, int[] aID, int[] aType, double[] aXYZ, double[] aVelocities, int[] aImage, boolean aShrinkExceed) throws LmpException;
    
    /**
     * lammps clear 指令
     */
    public void clear() throws LmpException {
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
            try {
                checkThread();
                try {lammpsClose_(mLmpPtr);} catch (LmpException ignored) {}
            } catch (LmpException e) {
                e.printStackTrace(System.err);
            }
        }
    }
    private native static void lammpsClose_(long aLmpPtr) throws LmpException;
}
