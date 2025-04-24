package jse.clib;

import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.functional.IUnaryFullOperator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static jse.code.Conf.*;
import static jse.code.CS.VERSION;
import static jse.code.OS.*;
import static jse.code.OS.IS_MAC;

/**
 * 其他 jni 库或者此项目需要依赖的 c 库；
 * 包含编写 jni 库需要的一些通用方法。
 * <p>
 * 使用 header only 的写法来简化编译，并且提高效率
 * @author liqa
 */
public class JNIUtil {
    private JNIUtil() {}
    
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link JNIUtil} 相关的 JNI 库是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link JNIUtil} 相关的 JNI 库 */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(HOME);
        }
    }
    
    /** 当前 {@link JNIUtil} JNI 库的根目录，结尾一定存在 {@code '/'} */
    public final static String HOME = JAR_DIR+"jniutil/" + UT.Code.uniqueID(VERSION) + "/";
    /** 当前 {@link JNIUtil} JNI 库的头文件所在文件夹，结尾一定存在 {@code '/'} */
    public final static String INCLUDE_DIR = HOME+"include/";
    /** 当前 {@link JNIUtil} JNI 库的头文件名称 */
    public final static String HEADER_NAME = "jniutil.h";
    /** 当前 {@link JNIUtil} JNI 库的头文件路径 */
    public final static String HEADER_PATH = INCLUDE_DIR+HEADER_NAME;
    
    /** {@link JNIUtil} 内部定义的常量，这里重新定义一次从而避免 JNI 通讯 */
    public final static int
      JTYPE_NULL    = 0
    , JTYPE_BYTE    = 1
    , JTYPE_DOUBLE  = 2
    , JTYPE_BOOLEAN = 3
    , JTYPE_CHAR    = 4
    , JTYPE_SHORT   = 5
    , JTYPE_INT     = 6
    , JTYPE_LONG    = 7
    , JTYPE_FLOAT   = 8
    ;
    
    private static void initJNIUtil_() throws Exception {
        // 直接从内部资源解压到需要目录，如果已经存在则先删除
        IO.removeDir(INCLUDE_DIR);
        IO.copy(IO.getResource("jniutil/src/"+ HEADER_NAME), HEADER_PATH);
        System.out.println("JNIUTIL INIT INFO: jniutil successfully installed.");
        System.out.println("JNIUTIL INCLUDE DIR: " + INCLUDE_DIR);
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        // 如果不存在 jniutil.h 则需要重新通过源码编译
        if (!IO.isFile(HEADER_PATH)) {
            System.out.println("JNIUTIL INIT INFO: jniutil.h not found. Reinstalling...");
            try {initJNIUtil_();}
            catch (Exception e) {throw new RuntimeException(e);}
        }
        // header only 库不需要设置库路径
    }
    
    
    
    private final static String BUILD_DIR_NAME = IS_WINDOWS ? "build-win" : (IS_MAC ? "build-mac" : "build");
    /** 现在将一些通用的 cmake 编译 jni 库流程统一放在这里，减少重复的代码 */
    @ApiStatus.Internal
    public static class LibBuilder implements Supplier<String> {
        private final String aProjectName, aInfoProjectName;
        private IDirIniter aSrcDirIniter = null;
        private IDirIniter aBuildDirIniter = sd -> {
            String tBuildDir = sd + BUILD_DIR_NAME + "/";
            IO.makeDir(tBuildDir);
            return tBuildDir;
        };
        private final String aLibDir;
        private String aCmakeInitDir = "..";
        private @Nullable String aCmakeCCompiler = null, aCmakeCxxCompiler = null, aCmakeCFlags = null, aCmakeCxxFlags = null;
        private @Nullable Boolean aUseMiMalloc = null;
        private boolean aRebuild = false;
        private final Map<String, String> aCmakeSettings;
        private @Nullable String aRedirectLibPath = null;
        private @Nullable IUnaryFullOperator<? extends CharSequence, ? super String> aCmakeLineOpt = line -> line;
        
        public LibBuilder(String aProjectName, String aInfoProjectName, String aLibDir, Map<String, String> aCmakeSettings) {
            this.aProjectName = aProjectName;
            this.aInfoProjectName = aInfoProjectName;
            this.aLibDir = aLibDir;
            this.aCmakeSettings = aCmakeSettings;
        }
        public LibBuilder setSrc(final String aAssetsDirName, final String[] aSrcNames) {
            aSrcDirIniter = wd -> {
                for (String tName : aSrcNames) {IO.copy(IO.getResource(aAssetsDirName+"/src/"+tName), wd+tName);}
                // 注意增加这个被省略的 CMakeLists.txt
                IO.copy(IO.getResource(aAssetsDirName+"/src/CMakeLists.txt"), wd+"CMakeLists.txt");
                return wd;
            };
            return this;
        }
        public LibBuilder setSrcDirIniter(IDirIniter aSrcDirIniter) {this.aSrcDirIniter = aSrcDirIniter; return this;}
        public LibBuilder setBuildDirIniter(IDirIniter aBuildDirIniter) {this.aBuildDirIniter = aBuildDirIniter; return this;}
        public LibBuilder setCmakeInitDir(String aCmakeInitDir) {this.aCmakeInitDir = aCmakeInitDir; return this;}
        public LibBuilder setCmakeCCompiler(@Nullable String aCmakeCCompiler) {this.aCmakeCCompiler = aCmakeCCompiler; return this;}
        public LibBuilder setCmakeCxxCompiler(@Nullable String aCmakeCxxCompiler) {this.aCmakeCxxCompiler = aCmakeCxxCompiler; return this;}
        public LibBuilder setCmakeCFlags(@Nullable String aCmakeCFlags) {this.aCmakeCFlags = aCmakeCFlags; return this;}
        public LibBuilder setCmakeCxxFlags(@Nullable String aCmakeCxxFlags) {this.aCmakeCxxFlags = aCmakeCxxFlags; return this;}
        public LibBuilder setUseMiMalloc(@Nullable Boolean aUseMiMalloc) {this.aUseMiMalloc = aUseMiMalloc; return this;}
        public LibBuilder setRebuild(boolean aRebuild) {this.aRebuild = aRebuild; return this;}
        public LibBuilder setRedirectLibPath(@Nullable String aRedirectLibPath) {this.aRedirectLibPath = aRedirectLibPath; return this;}
        public LibBuilder setCmakeLineOpt(@Nullable IUnaryFullOperator<? extends CharSequence, ? super String> aCmakeLineOpt) {this.aCmakeLineOpt = aCmakeLineOpt; return this;}
        
        @Override public String get() {
            // 如果开启了 USE_MIMALLOC 则增加 MiMalloc 依赖
            if (aUseMiMalloc!=null && aUseMiMalloc) MiMalloc.InitHelper.init();
            String tLibPath;
            if (aRedirectLibPath == null) {
                @Nullable String tLibName = LIB_NAME_IN(aLibDir, aProjectName);
                // 如果不存在 jni lib 则需要重新通过源码编译
                if (aRebuild || tLibName == null) {
                    System.out.println(aInfoProjectName+" INIT INFO: "+aProjectName+" libraries not found. Reinstalling...");
                    try {
                        tLibName = initLib_(aProjectName, aInfoProjectName, aSrcDirIniter, aBuildDirIniter, aLibDir,
                                            aCmakeInitDir, aCmakeCCompiler, aCmakeCxxCompiler, aCmakeCFlags, aCmakeCxxFlags,
                                            aUseMiMalloc, aCmakeSettings, aCmakeLineOpt);
                    } catch (Exception e) {throw new RuntimeException(e);}
                }
                tLibPath = aLibDir + tLibName;
            } else {
                if (DEBUG) System.out.println(aInfoProjectName+" INIT INFO: "+aProjectName+" libraries are redirected to '" + aRedirectLibPath + "'");
                tLibPath = aRedirectLibPath;
            }
            return tLibPath;
        }
    }
    
    @ApiStatus.Internal
    private static String cmakeInitCmd_(String aCmakeInitDir, @Nullable String aCmakeCCompiler, @Nullable String aCmakeCxxCompiler, @Nullable String aCmakeCFlags, @Nullable String aCmakeCxxFlags) {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        // 这里设置 C/C++ 编译器（如果有）
        if (aCmakeCCompiler   != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="  + aCmakeCCompiler);}
        if (aCmakeCxxCompiler != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+ aCmakeCxxCompiler);}
        if (aCmakeCFlags      != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS='"    + aCmakeCFlags  +"'");}
        if (aCmakeCxxFlags    != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"  + aCmakeCxxFlags+"'");}
        // 初始化使用上一个目录的 CMakeList.txt
        rCommand.add(aCmakeInitDir);
        return String.join(" ", rCommand);
    }
    @ApiStatus.Internal
    private static String cmakeSettingCmd_(String aLibDir, @Nullable Boolean aUseMiMalloc, Map<String, String> aCmakeSettings) throws IOException {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cmake");
        if (aUseMiMalloc != null) {
        rCommand.add("-D"); rCommand.add("JSE_USE_MIMALLOC="+(aUseMiMalloc?"ON":"OFF"));
        }
        // 设置构建输出目录为 lib
        IO.makeDir(aLibDir); // 初始化一下这个目录避免意料外的问题
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH='"+ aLibDir +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH='"+ aLibDir +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH='"+ aLibDir +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH='"+ aLibDir +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH='"+ aLibDir +"'");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH='"+ aLibDir +"'");
        // 添加额外的设置参数
        for (Map.Entry<String, String> tEntry : aCmakeSettings.entrySet()) {
        rCommand.add("-D"); rCommand.add(String.format("%s=%s", tEntry.getKey(), tEntry.getValue()));
        }
        rCommand.add(".");
        return String.join(" ", rCommand);
    }
    
    @FunctionalInterface public interface IDirIniter {String init(String aInput) throws Exception;}
    private static @NotNull String initLib_(String aProjectName, String aInfoProjectName, IDirIniter aSrcDirIniter, IDirIniter aBuildDirIniter, String aLibDir,
                                            String aCmakeInitDir, @Nullable String aCmakeCCompiler, @Nullable String aCmakeCxxCompiler, @Nullable String aCmakeCFlags, @Nullable String aCmakeCxxFlags,
                                            final @Nullable Boolean aUseMiMalloc, Map<String, String> aCmakeSettings,
                                            final @Nullable IUnaryFullOperator<? extends CharSequence, ? super String> aCmakeLineOpt) throws Exception {
        // 检测 cmake，为了简洁并避免问题，现在要求一定要有 cmake 环境
        EXEC.setNoSTDOutput().setNoERROutput();
        boolean tNoCmake = EXEC.system("cmake --version") != 0;
        EXEC.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCmake) {
            System.err.println("No cmake found, you can download cmake from: https://cmake.org/download/");
            throw new Exception(aInfoProjectName+" BUILD ERROR: No cmake environment.");
        }
        // 从内部资源解压到临时目录
        String tWorkingDir = WORKING_DIR_OF(aProjectName+"@"+UT.Code.randID());
        // 如果已经存在则先删除
        IO.removeDir(tWorkingDir);
        // 初始化工作目录，默认操作为把源码拷贝到目录下；
        // 对于较大的项目则会是一个 zip 的源码，多一个解压的步骤
        String tSrcDir = aSrcDirIniter.init(tWorkingDir);
        // 这里对 CMakeLists.txt 特殊处理
        if (aCmakeLineOpt != null) {
            String tCmakeListsDir = IO.toInternalValidDir(aCmakeInitDir).substring(3); // 为了让讨论简单，这里约定要求 aCmakeInitDir 一定要 `..` 开头
            IO.map(tSrcDir+tCmakeListsDir+"CMakeLists.txt", tSrcDir+tCmakeListsDir+"CMakeLists1.txt", line -> {
                // 替换其中的 jniutil 库路径为设置好的路径
                line = line.replace("$ENV{JSE_JNIUTIL_INCLUDE_DIR}", JNIUtil.INCLUDE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
                // 替换其中的 mimalloc 库路径为设置好的路径
                if (aUseMiMalloc!=null && aUseMiMalloc) {
                line = line.replace("$ENV{JSE_MIMALLOC_INCLUDE_DIR}", MiMalloc.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                           .replace("$ENV{JSE_MIMALLOC_LIB_PATH}"   , MiMalloc.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
                }
                return aCmakeLineOpt.apply(line);
            });
            // 覆盖旧的 CMakeLists.txt
            IO.move(tSrcDir+tCmakeListsDir+"CMakeLists1.txt", tSrcDir+tCmakeListsDir+"CMakeLists.txt");
        }
        // 开始通过 cmake 编译
        System.out.println(aInfoProjectName+" INIT INFO: Building "+aProjectName+" from source code...");
        String tBuildDir = aBuildDirIniter.init(tSrcDir);
        // 直接通过系统指令来编译库，关闭输出
        EXEC.setNoSTDOutput().setWorkingDir(tBuildDir);
        // 初始化 cmake
        EXEC.system(cmakeInitCmd_(aCmakeInitDir, aCmakeCCompiler, aCmakeCxxCompiler, aCmakeCFlags, aCmakeCxxFlags));
        // 设置参数
        EXEC.system(cmakeSettingCmd_(aLibDir, aUseMiMalloc, aCmakeSettings));
        // 最后进行构造操作
        EXEC.system("cmake --build . --config Release");
        EXEC.setNoSTDOutput(false).setWorkingDir(null);
        // 简单检测一下是否编译成功
        @Nullable String tLibName = LIB_NAME_IN(aLibDir, aProjectName);
        if (tLibName == null) {
            System.err.println("Build Failed, this may be caused by the lack of a C/C++ compiler");
            if (IS_WINDOWS) {
                System.err.println("  For Windows, you can use MSVC: https://visualstudio.microsoft.com/vs/features/cplusplus/");
            } else {
                System.err.println("  For Liunx/Mac, you can use GCC: https://gcc.gnu.org/");
                System.err.println("  For Ubuntu, you can use `sudo apt install g++`");
            }
            throw new Exception(aInfoProjectName+" BUILD ERROR: Build Failed, No "+aProjectName+" lib in '"+aLibDir+"'");
        }
        // 完事后移除临时解压得到的源码
        IO.removeDir(tWorkingDir);
        System.out.println(aInfoProjectName+" INIT INFO: "+aProjectName+" successfully installed.");
        System.out.println(aInfoProjectName+" LIB DIR: " + aLibDir);
        // 输出安装完成后的库名称
        return tLibName;
    }
}
