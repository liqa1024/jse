package jse.clib;

import jse.code.Conf;
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
import java.util.regex.Pattern;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.Conf.*;
import static jse.code.OS.*;

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
    public final static String HOME = JAR_DIR+"jniutil/" + UT.Code.uniqueID(VERSION_NUMBER) + "/";
    /** 当前 {@link JNIUtil} JNI 库的头文件所在文件夹，结尾一定存在 {@code '/'} */
    public final static String INCLUDE_DIR = HOME+"include/";
    /** 当前 {@link JNIUtil} JNI 库的头文件名称 */
    public final static String HEADER_NAME = "jniutil.h";
    /** 当前 {@link JNIUtil} JNI 库的头文件路径 */
    public final static String HEADER_PATH = INCLUDE_DIR+HEADER_NAME;
    
    /** jse 自动下载的一些离线包的路径，这里采用 jar 包所在的绝对路径 */
    public final static String PKG_DIR = JAR_DIR+".jnipkg/";
    
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
    /** 检测 java 数组类型 */
    public static int jarrayType(Object aJArray) {
        if (aJArray == null) {
            return JTYPE_NULL;
        } else
        if (aJArray instanceof byte[]) {
            return JTYPE_BYTE;
        } else
        if (aJArray instanceof double[]) {
            return JTYPE_DOUBLE;
        } else
        if (aJArray instanceof boolean[]) {
            return JTYPE_BOOLEAN;
        } else
        if (aJArray instanceof char[]) {
            return JTYPE_CHAR;
        } else
        if (aJArray instanceof short[]) {
            return JTYPE_SHORT;
        } else
        if (aJArray instanceof int[]) {
            return JTYPE_INT;
        } else
        if (aJArray instanceof long[]) {
            return JTYPE_LONG;
        } else
        if (aJArray instanceof float[]) {
            return JTYPE_FLOAT;
        } else {
            throw new IllegalArgumentException("Invalid array type: " + aJArray.getClass().getName());
        }
    }
    
    private static void initJNIUtil_() throws Exception {
        // 直接从内部资源解压到需要目录，如果已经存在则先删除
        IO.removeDir(INCLUDE_DIR);
        IO.copy(IO.getResource("jniutil/src/"+ HEADER_NAME), HEADER_PATH);
        System.out.println(IO.Text.green("JNIUTIL INIT INFO:")+" jniutil successfully installed.");
        System.out.println(IO.Text.green("JNIUTIL INCLUDE DIR: ") + INCLUDE_DIR);
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        // windows 上建议使用微软构建的 jdk
        if (Conf.JDK_CHECK && IS_WINDOWS) {
            boolean tIsMsJdk = System.getProperty("java.vendor").toLowerCase().contains("microsoft");
            if (!tIsMsJdk) {
                UT.Code.warning("There may be CRT conflict issues when calling JNI on Windows, \n" +
                                "using Microsoft Build of OpenJDK can to some extent avoid this: \n" +
                                "https://learn.microsoft.com/zh-cn/java/openjdk/download");
            }
        }
        // 现在在这里检测编译器环境和 cmake
        Compiler.InitHelper.init();
        CMake.InitHelper.init();
        // 总是事先合法化这个目录，让用户可以快速找到
        try {IO.makeDir(PKG_DIR);}
        catch (Exception e) {throw new RuntimeException(e);}
        // 如果不存在 jniutil.h 则需要重新通过源码编译
        if (!IO.isFile(HEADER_PATH)) {
            System.out.println(IO.Text.green("JNIUTIL INIT INFO:")+" jniutil.h not found. Reinstalling...");
            try {initJNIUtil_();}
            catch (Exception e) {throw new RuntimeException(e);}
        }
        // header only 库不需要设置库路径
    }
    
    
    private final static String BUILD_DIR_NAME = IS_WINDOWS ? "build-win" : (IS_MAC ? "build-mac" : "build");
    private final static Pattern BUILD_DIR_INVALID_NAME = Pattern.compile("[^a-zA-Z0-9_\\-./\\\\@!#$%^&*()+=]");
    private static boolean containsAnyInvalidChar_(String aDir) {
        if (IS_WINDOWS && IO.isAbsolutePath(aDir)) {
            aDir = aDir.substring(2);
        }
        return BUILD_DIR_INVALID_NAME.matcher(aDir).find();
    }
    
    
    @ApiStatus.Internal
    @FunctionalInterface public interface IEnvChecker {void check() throws Exception;}
    @ApiStatus.Internal
    @FunctionalInterface public interface IDirIniter {String init(String aInput) throws Exception;}
    @ApiStatus.Internal
    @FunctionalInterface public interface IDirConsumer {void apply(String aInput) throws Exception;}
    
    /** 现在将一些通用的 cmake 编译 jni 库流程统一放在这里，减少重复的代码 */
    @ApiStatus.Internal
    public static class LibBuilder implements Supplier<String> {
        private final String mProjectName, mInfoProjectName;
        private final List<IEnvChecker> mEnvChecker = new ArrayList<>();
        private IDirIniter mSrcDirIniter = null;
        private IDirIniter mBuildDirIniter = sd -> {
            String tBuildDir = sd + BUILD_DIR_NAME + "/";
            IO.makeDir(tBuildDir);
            return tBuildDir;
        };
        private IDirConsumer mPostBuildDir = bd -> {};
        private int mParallel = 0;
        private final String mLibDir;
        private String mCmakeInitDir = "..";
        private boolean mUsedCmakeCCompiler = false, mUsedCmakeCxxCompiler = false;
        private @Nullable String mCmakeCCompiler = null, mCmakeCxxCompiler = null, mCmakeCFlags = null, mCmakeCxxFlags = null;
        private @Nullable Boolean mUseMiMalloc = null;
        private boolean mMT = false;
        private final Map<String, String> mCmakeSettings;
        private @Nullable IUnaryFullOperator<? extends CharSequence, ? super String> mCmakeLineOpt = line -> line;
        
        public LibBuilder(String aProjectName, String aInfoProjectName, String aLibDir, Map<String, String> aCmakeSettings) {
            mProjectName = aProjectName;
            mInfoProjectName = aInfoProjectName;
            mLibDir = aLibDir;
            mCmakeSettings = aCmakeSettings;
        }
        public LibBuilder setParallel(int aParallel) {mParallel = aParallel; return this;}
        public LibBuilder setMT() {return setMT(true);}
        public LibBuilder setMT(boolean aMT) {mMT = aMT; return this;}
        public LibBuilder setEnvChecker(IEnvChecker aEnvChecker) {mEnvChecker.add(aEnvChecker); return this;}
        public LibBuilder setSrc(final String aAssetsDirName, final String[] aSrcNames) {
            mSrcDirIniter = wd -> {
                for (String tName : aSrcNames) {IO.copy(IO.getResource(aAssetsDirName+"/src/"+tName), wd+tName);}
                // 注意增加这个被省略的 CMakeLists.txt
                IO.copy(IO.getResource(aAssetsDirName+"/src/CMakeLists.txt"), wd+"CMakeLists.txt");
                return wd;
            };
            return this;
        }
        public LibBuilder setSrcDirIniter(IDirIniter aSrcDirIniter) {mSrcDirIniter = aSrcDirIniter; return this;}
        public LibBuilder setBuildDirIniter(IDirIniter aBuildDirIniter) {mBuildDirIniter = aBuildDirIniter; return this;}
        public LibBuilder setPostBuildDir(IDirConsumer aPostBuildDir) {mPostBuildDir = aPostBuildDir; return this;}
        public LibBuilder setCmakeInitDir(String aCmakeInitDir) {mCmakeInitDir = aCmakeInitDir; return this;}
        public LibBuilder setCmakeCCompiler(@Nullable String aCmakeCCompiler) {mUsedCmakeCCompiler = true; mCmakeCCompiler = aCmakeCCompiler; return this;}
        public LibBuilder setCmakeCxxCompiler(@Nullable String aCmakeCxxCompiler) {mUsedCmakeCxxCompiler = true; mCmakeCxxCompiler = aCmakeCxxCompiler; return this;}
        public LibBuilder setCmakeCFlags(@Nullable String aCmakeCFlags) {mCmakeCFlags = aCmakeCFlags; return this;}
        public LibBuilder setCmakeCxxFlags(@Nullable String aCmakeCxxFlags) {mCmakeCxxFlags = aCmakeCxxFlags; return this;}
        public LibBuilder setUseMiMalloc(@Nullable Boolean aUseMiMalloc) {mUseMiMalloc = aUseMiMalloc; return this;}
        public LibBuilder setCmakeLineOp(@Nullable IUnaryFullOperator<? extends CharSequence, ? super String> aCmakeLineOpt) {mCmakeLineOpt = aCmakeLineOpt; return this;}
        
        @Override public String get() {
            // 现在总是优先依赖 jniutil 用来确保一些通用的检测总是优先执行
            JNIUtil.InitHelper.init();
            // 如果开启了 USE_MIMALLOC 则增加 MiMalloc 依赖
            if ((!Compiler.Conf.FORCE || !Compiler.GCC_OLD) && mUseMiMalloc!=null && mUseMiMalloc) MiMalloc.InitHelper.init();
            @Nullable String tLibName = LIB_NAME_IN(mLibDir, mProjectName);
            // 如果不存在 jni lib 则需要重新通过源码编译
            if (tLibName == null) {
                System.out.println(IO.Text.green(mInfoProjectName +" INIT INFO: ")+ mProjectName +" libraries not found. Reinstalling...");
                try {
                    tLibName = initLib_();
                } catch (Exception e) {throw new RuntimeException(e);}
            }
            return mLibDir + tLibName;
        }
        
        
        private String cmakeInitCmd_() throws IOException {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add(CMake.EXE_CMD);
            // 这里设置 C/C++ 编译器（如果有）
            if (mUsedCmakeCCompiler) {
                String tCmakeCCompiler = mCmakeCCompiler==null ? Compiler.C_COMPILER : mCmakeCCompiler;
                if (tCmakeCCompiler!=null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="+tCmakeCCompiler);}
            }
            if (mUsedCmakeCxxCompiler) {
                String tCmakeCxxCompiler = mCmakeCxxCompiler==null ? Compiler.CXX_COMPILER : mCmakeCxxCompiler;
                if (tCmakeCxxCompiler!=null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+tCmakeCxxCompiler);}
            }
            if (mCmakeCFlags!=null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS='"+mCmakeCFlags+"'");}
            if (mCmakeCxxFlags!=null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"+mCmakeCxxFlags+"'");}
            // 配置其余的参数设置
            if (mUseMiMalloc!=null) {
                rCommand.add("-D"); rCommand.add("JSE_USE_MIMALLOC="+(((!Compiler.Conf.FORCE || !Compiler.GCC_OLD) && mUseMiMalloc) ? "ON" : "OFF"));
            }
            // windows 下可选开启 /MT 保证静态链接
            if (IS_WINDOWS && mMT) {
                rCommand.add("-D"); rCommand.add("CMAKE_MSVC_RUNTIME_LIBRARY=MultiThreaded");
            }
            // 设置构建输出目录为 lib
            IO.makeDir(mLibDir); // 初始化一下这个目录避免意料外的问题
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH='"+ mLibDir +"'");
            // 添加额外的设置参数
            for (Map.Entry<String, String> tEntry : mCmakeSettings.entrySet()) {
                rCommand.add("-D"); rCommand.add(String.format("%s=%s", tEntry.getKey(), tEntry.getValue()));
            }
            // 初始化使用上一个目录的 CMakeList.txt
            rCommand.add(mCmakeInitDir);
            return String.join(" ", rCommand);
        }
        private @NotNull String initLib_() throws Exception {
            // 这里先输出编译器信息和可能的 cmake 信息，以及顺便的延迟环境检测
            Compiler.printInfo();
            CMake.printInfo(); // 目前默认不使用系统库则不会再输出
            // 自定义的环境检测
            if (!mEnvChecker.isEmpty()) for (IEnvChecker tChecker : mEnvChecker) tChecker.check();
            // 从内部资源解压到临时目录，现在编译任务统一放到 jse 安装目录
            boolean tWorkingDirValid = true;
            String tWorkingDirName = "build-"+ mProjectName +"@"+UT.Code.randID() + "/";
            String tWorkingDir = JAR_DIR + tWorkingDirName;
            // 判断路径是否存在非法字符，如果存在则改为到用户目录编译
            if (containsAnyInvalidChar_(tWorkingDir)) {
                String tWorkingDir2 = USER_HOME_DIR + tWorkingDirName;
                if (!containsAnyInvalidChar_(tWorkingDir2)) {
                    tWorkingDir = tWorkingDir2;
                } else {
                    System.err.println(IO.Text.yellow(mInfoProjectName +" INIT WARNING:")+" Build directory ("+tWorkingDir+") contains inappropriate characters, build may fail.");
                    tWorkingDirValid = false;
                }
            }
            // 如果已经存在则先删除
            IO.removeDir(tWorkingDir);
            // 初始化工作目录，默认操作为把源码拷贝到目录下；
            // 对于较大的项目则会是一个 zip 的源码，多一个解压的步骤
            String tSrcDir = mSrcDirIniter.init(tWorkingDir);
            // 这里对 CMakeLists.txt 特殊处理
            if (mCmakeLineOpt != null) {
                String tCmakeListsDir = IO.toInternalValidDir(mCmakeInitDir).substring(3); // 为了让讨论简单，这里约定要求 aCmakeInitDir 一定要 `..` 开头
                IO.map(tSrcDir+tCmakeListsDir+"CMakeLists.txt", tSrcDir+tCmakeListsDir+"CMakeLists1.txt", line -> {
                    // 替换其中的 jniutil 库路径为设置好的路径
                    line = line.replace("$ENV{JSE_JNIUTIL_INCLUDE_DIR}", JNIUtil.INCLUDE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
                    // 替换其中的 mimalloc 库路径为设置好的路径
                    if ((!Compiler.Conf.FORCE || !Compiler.GCC_OLD) && mUseMiMalloc!=null && mUseMiMalloc) {
                        line = line.replace("$ENV{JSE_MIMALLOC_INCLUDE_DIR}", MiMalloc.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                                   .replace("$ENV{JSE_MIMALLOC_LIB_PATH}"   , MiMalloc.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
                    }
                    return mCmakeLineOpt.apply(line);
                });
                // 覆盖旧的 CMakeLists.txt
                IO.move(tSrcDir+tCmakeListsDir+"CMakeLists1.txt", tSrcDir+tCmakeListsDir+"CMakeLists.txt");
            }
            // 开始通过 cmake 编译
            System.out.println(IO.Text.green(mInfoProjectName +" INIT INFO:")+" Building "+ mProjectName +" from source code...");
            String tBuildDir = mBuildDirIniter.init(tSrcDir);
            // 直接通过系统指令来编译库，关闭输出
            EXEC.setWorkingDir(tBuildDir);
            if (!DEBUG) EXEC.setNoSTDOutput();
            // 现在初始化 cmake 和参数设置放在一起执行
            EXEC.system(cmakeInitCmd_());
            // 最后进行构造操作
            String tCmd = CMake.EXE_CMD+" --build . --config Release";
            if (mParallel > 1) tCmd += " --parallel "+mParallel;
            EXEC.system(tCmd);
            EXEC.setNoSTDOutput(false).setWorkingDir(null);
            // 简单检测一下是否编译成功
            @Nullable String tLibName = LIB_NAME_IN(mLibDir, mProjectName);
            if (tLibName == null) {
                System.err.println("Build Failed, build directory: "+tBuildDir);
                if (!tWorkingDirValid) {
                    System.err.println("  This may be caused by the inappropriate characters in working directory: "+tWorkingDir);
                }
                if (IS_WINDOWS) {
                    System.err.println("  You can use `$env:JSE_DEBUG = 1` (in powershell) to make the build output complete information");
                } else {
                    System.err.println("  You can use `export JSE_DEBUG=1` to make the build output complete information");
                }
                throw new Exception(mProjectName+" build Failed, No lib in '"+ mLibDir +"'");
            }
            mPostBuildDir.apply(tBuildDir);
            // 完事后移除临时解压得到的源码，这里需要对于神秘文件系统专门处理
            if (JAR_DIR_BAD_FILESYSTEM && !IS_WINDOWS) {
                OS.printFilesystemInfo();
                EXEC.system("rm -rf \""+tWorkingDir+"\"");
            } else {
                IO.removeDir(tWorkingDir);
            }
            System.out.println(IO.Text.green(mInfoProjectName +" INIT INFO: ")+ mProjectName +" successfully installed.");
            System.out.println(IO.Text.green(mInfoProjectName +" LIB DIR: ") + mLibDir);
            // 输出安装完成后的库名称
            return tLibName;
        }
    }
}
