package jse.clib;

import jse.code.OS;
import jse.code.UT;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static jse.code.CS.VERSION;
import static jse.code.Conf.*;
import static jse.code.OS.EXEC;
import static jse.code.OS.JAR_DIR;

/**
 * 直接访问使用 C 指针的类，不进行自动内存回收和各种检查从而保证最大的兼容性；
 * 此类因此是 {@code Unsafe} 的。
 * <p>
 * 内部默认会统一使用 {@link MiMalloc} 来加速内存分配和释放的过程。
 * @author liqa
 */
public class CPointer {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            if (!INITIALIZED) String.valueOf(LIB_PATH);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义构建 cpointer 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new HashMap<>();
        
        /**
         * 自定义构建 cpointer 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_CPOINTER"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_CPOINTER", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_CPOINTER"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_CPOINTER"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 对于 cpointer，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
         * 这对于 java 数组和 c 数组的转换很有效
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_CPOINTER", jse.code.Conf.USE_MIMALLOC);
        
        /** 重定向 cpointer 动态库的路径，用于自定义编译这个库的过程，或者重新实现 cpointer 的接口 */
        public static @Nullable String REDIRECT_CPOINTER_LIB = OS.env("JSE_REDIRECT_CPOINTER_LIB");
    }
    
    private final static String LIB_DIR = JAR_DIR+"cpointer/" + UT.Code.uniqueID(VERSION, Conf.USE_MIMALLOC, Conf.CMAKE_C_COMPILER, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    private final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jse_clib_CPointer.c"
        , "jse_clib_CPointer.h"
        , "jse_clib_IntCPointer.c"
        , "jse_clib_IntCPointer.h"
        , "jse_clib_DoubleCPointer.c"
        , "jse_clib_DoubleCPointer.h"
        , "jse_clib_NestedCPointer.c"
        , "jse_clib_NestedCPointer.h"
        , "jse_clib_NestedIntCPointer.c"
        , "jse_clib_NestedIntCPointer.h"
        , "jse_clib_NestedDoubleCPointer.c"
        , "jse_clib_NestedDoubleCPointer.h"
    };
    
    private static String cmakeInitCmd_() {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        // 这里设置 C/C++ 编译器（如果有）
        if (Conf.CMAKE_C_COMPILER   != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="  + Conf.CMAKE_C_COMPILER);}
        if (Conf.CMAKE_CXX_COMPILER != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+ Conf.CMAKE_CXX_COMPILER);}
        if (Conf.CMAKE_C_FLAGS      != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS='"    + Conf.CMAKE_C_FLAGS  +"'");}
        if (Conf.CMAKE_CXX_FLAGS    != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"  + Conf.CMAKE_CXX_FLAGS+"'");}
        // 初始化使用上一个目录的 CMakeList.txt
        rCommand.add("..");
        return String.join(" ", rCommand);
    }
    private static String cmakeSettingCmd_() throws IOException {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        rCommand.add("-D"); rCommand.add("JSE_USE_MIMALLOC="+(Conf.USE_MIMALLOC?"ON":"OFF"));
        // 设置构建输出目录为 lib
        UT.IO.makeDir(LIB_DIR); // 初始化一下这个目录避免意料外的问题
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH='"+ LIB_DIR +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH='"+ LIB_DIR +"'");
        // 添加额外的设置参数
        for (Map.Entry<String, String> tEntry : Conf.CMAKE_SETTING.entrySet()) {
        rCommand.add("-D"); rCommand.add(String.format("%s=%s", tEntry.getKey(), tEntry.getValue()));
        }
        rCommand.add(".");
        return String.join(" ", rCommand);
    }
    
    private static @NotNull String initCPointer_() throws Exception {
        // 检测 cmake，为了简洁并避免问题，现在要求一定要有 cmake 环境
        EXEC.setNoSTDOutput().setNoERROutput();
        boolean tNoCmake = EXEC.system("cmake --version") != 0;
        EXEC.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCmake) throw new Exception("CPOINTER BUILD ERROR: No cmake environment.");
        // 从内部资源解压到临时目录
        String tWorkingDir = WORKING_DIR_OF("cpointer");
        // 如果已经存在则先删除
        UT.IO.removeDir(tWorkingDir);
        for (String tName : SRC_NAME) {
            UT.IO.copy(UT.IO.getResource("cpointer/src/"+tName), tWorkingDir+tName);
        }
        // 这里对 CMakeLists.txt 特殊处理
        UT.IO.map(UT.IO.getResource("cpointer/src/CMakeLists.txt"), tWorkingDir+"CMakeLists.txt", line -> {
            // 替换其中的 jniutil 库路径为设置好的路径
            line = line.replace("$ENV{JSE_JNIUTIL_INCLUDE_DIR}", JNIUtil.INCLUDE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
            // 替换其中的 mimalloc 库路径为设置好的路径
            if (Conf.USE_MIMALLOC) {
                line = line.replace("$ENV{JSE_MIMALLOC_INCLUDE_DIR}", MiMalloc.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                           .replace("$ENV{JSE_MIMALLOC_LIB_PATH}"   , MiMalloc.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
            }
            return line;
        });
        System.out.println("CPOINTER INIT INFO: Building cpointer from source code...");
        String tBuildDir = tWorkingDir+"build/";
        UT.IO.makeDir(tBuildDir);
        // 直接通过系统指令来编译 cpointer 的库，关闭输出
        EXEC.setNoSTDOutput().setWorkingDir(tBuildDir);
        // 初始化 cmake
        EXEC.system(cmakeInitCmd_());
        // 设置参数
        EXEC.system(cmakeSettingCmd_());
        // 最后进行构造操作
        EXEC.system("cmake --build . --config Release");
        EXEC.setNoSTDOutput(false).setWorkingDir(null);
        // 简单检测一下是否编译成功
        @Nullable String tLibName = LIB_NAME_IN(LIB_DIR, "cpointer");
        if (tLibName == null) throw new Exception("CPOINTER BUILD ERROR: Build Failed, No cpointer lib in '"+LIB_DIR+"'");
        // 完事后移除临时解压得到的源码
        UT.IO.removeDir(tWorkingDir);
        System.out.println("CPOINTER INIT INFO: cpointer successfully installed.");
        // 输出安装完成后的库名称
        return tLibName;
    }
    
    static {
        InitHelper.INITIALIZED = true;
        // 依赖 jniutil
        JNIUtil.InitHelper.init();
        // 如果开启了 USE_MIMALLOC 则增加 MiMalloc 依赖
        if (Conf.USE_MIMALLOC) MiMalloc.InitHelper.init();
        
        if (Conf.REDIRECT_CPOINTER_LIB == null) {
            @Nullable String tLibName = LIB_NAME_IN(LIB_DIR, "cpointer");
            // 如果不存在 jni lib 则需要重新通过源码编译
            if (tLibName == null) {
                System.out.println("CPOINTER INIT INFO: cpointer libraries not found. Reinstalling...");
                try {tLibName = initCPointer_();} catch (Exception e) {throw new RuntimeException(e);}
            }
            LIB_PATH = LIB_DIR + tLibName;
        } else {
            if (DEBUG) System.out.println("CPOINTER INIT INFO: cpointer libraries are redirected to '" + Conf.REDIRECT_CPOINTER_LIB + "'");
            LIB_PATH = Conf.REDIRECT_CPOINTER_LIB;
        }
        // 设置库路径
        System.load(UT.IO.toAbsolutePath(LIB_PATH));
    }
    
    
    protected long mPtr;
    @ApiStatus.Internal public CPointer(long aPtr) {mPtr = aPtr;}
    @ApiStatus.Internal public final long ptr_() {return mPtr;}
    
    public boolean isNull() {return mPtr==0 || mPtr==-1;}
    
    public static CPointer malloc(int aCount) {
        return new CPointer(malloc_(aCount));
    }
    protected native static long malloc_(int aCount);
    
    public static CPointer calloc(int aCount) {
        return new CPointer(calloc_(aCount));
    }
    protected native static long calloc_(int aCount);
    
    public void free() {
        if (isNull()) throw new IllegalStateException("Cannot free a NULL pointer");
        free_(mPtr);
        mPtr = 0;
    }
    private native static void free_(long aPtr);
    
    public CPointer copy() {
        return new CPointer(mPtr);
    }
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof CPointer)) return false;
        
        CPointer tCPointer = (CPointer)aRHS;
        return mPtr == tCPointer.mPtr;
    }
    @Override public final int hashCode() {
        return Long.hashCode(mPtr);
    }
    
    public IntCPointer asIntCPointer() {return new IntCPointer(mPtr);}
    public DoubleCPointer asDoubleCPointer() {return new DoubleCPointer(mPtr);}
    public NestedCPointer asNestedCPointer() {return new NestedCPointer(mPtr);}
    public NestedIntCPointer asNestedIntCPointer() {return new NestedIntCPointer(mPtr);}
    public NestedDoubleCPointer asNestedDoubleCPointer() {return new NestedDoubleCPointer(mPtr);}
    
    static void rangeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
