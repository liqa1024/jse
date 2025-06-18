package jse.clib;

import jse.code.IO;
import jse.code.UT;
import jse.code.functional.IUnaryFullOperator;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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
    
    /** 现在将一些通用的 cmake 编译 jni 库流程统一放在这里，减少重复的代码 */
    @ApiStatus.Internal
    public static class LibBuilder implements Supplier<String> {
        private final String mProjectName, mInfoProjectName;
        private IEnvChecker mEnvChecker = null;
        private IDirIniter mSrcDirIniter = null;
        private IDirIniter mBuildDirIniter = sd -> {
            String tBuildDir = sd + BUILD_DIR_NAME + "/";
            IO.makeDir(tBuildDir);
            return tBuildDir;
        };
        private final String mLibDir;
        private String mCmakeInitDir = "..";
        private @Nullable String mCmakeCCompiler = null, mCmakeCxxCompiler = null, mCmakeCFlags = null, mCmakeCxxFlags = null;
        private @Nullable Boolean mUseMiMalloc = null;
        private boolean mRebuild = false;
        private final Map<String, String> mCmakeSettings;
        private @Nullable String mRedirectLibPath = null;
        private @Nullable IUnaryFullOperator<? extends CharSequence, ? super String> mCmakeLineOpt = line -> line;
        
        public LibBuilder(String aProjectName, String aInfoProjectName, String aLibDir, Map<String, String> aCmakeSettings) {
            mProjectName = aProjectName;
            mInfoProjectName = aInfoProjectName;
            mLibDir = aLibDir;
            mCmakeSettings = aCmakeSettings;
        }
        public LibBuilder setMPIChecker() {return setMPIChecker(false);}
        public LibBuilder setMPIChecker(final boolean aForce) {
            // 通用的检测 mpi 接口
            mEnvChecker = () -> {
                EXEC.setNoSTDOutput().setNoERROutput();
                boolean tNoMpi = EXEC.system("mpiexec --version") != 0;
                if (tNoMpi) {
                    tNoMpi = EXEC.system("mpiexec -?") != 0;
                }
                EXEC.setNoSTDOutput(false).setNoERROutput(false);
                if (tNoMpi) {
                    if (aForce) {
                        System.err.println("No MPI found");
                    } else {
                        System.err.println(mInfoProjectName +" INIT WARNING: No MPI found for "+ mProjectName +" build");
                    }
                    if (IS_WINDOWS) {
                        System.err.println("  For Windows, you can use MS-MPI: https://www.microsoft.com/en-us/download/details.aspx?id=105289");
                        System.err.println("  BOTH 'msmpisetup.exe' and 'msmpisdk.msi' are needed.");
                    } else {
                        System.err.println("  For Liunx/Mac, you can use OpenMPI: https://www.open-mpi.org/");
                        System.err.println("  For Ubuntu, you can use `sudo apt install libopenmpi-dev`");
                    }
                    if (aForce) {
                        throw new Exception(mInfoProjectName +" INIT ERROR: No MPI environment.");
                    }
                    System.out.println("build "+ mProjectName +" without MPI support? (y/N)");
                    BufferedReader tReader = IO.toReader(System.in, Charset.defaultCharset());
                    String tLine = tReader.readLine();
                    while (!tLine.equalsIgnoreCase("y")) {
                        if (tLine.isEmpty() || tLine.equalsIgnoreCase("n")) {
                            throw new Exception(mInfoProjectName +" INIT ERROR: No MPI environment.");
                        }
                        System.out.println("build "+ mProjectName +" without MPI support? (y/N)");
                    }
                }
            };
            return this;
        }
        public LibBuilder setEnvChecker(IEnvChecker aEnvChecker) {mEnvChecker = aEnvChecker; return this;}
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
        public LibBuilder setCmakeInitDir(String aCmakeInitDir) {mCmakeInitDir = aCmakeInitDir; return this;}
        public LibBuilder setCmakeCCompiler(@Nullable String aCmakeCCompiler) {mCmakeCCompiler = aCmakeCCompiler; return this;}
        public LibBuilder setCmakeCxxCompiler(@Nullable String aCmakeCxxCompiler) {mCmakeCxxCompiler = aCmakeCxxCompiler; return this;}
        public LibBuilder setCmakeCFlags(@Nullable String aCmakeCFlags) {mCmakeCFlags = aCmakeCFlags; return this;}
        public LibBuilder setCmakeCxxFlags(@Nullable String aCmakeCxxFlags) {mCmakeCxxFlags = aCmakeCxxFlags; return this;}
        public LibBuilder setUseMiMalloc(@Nullable Boolean aUseMiMalloc) {mUseMiMalloc = aUseMiMalloc; return this;}
        public LibBuilder setRebuild(boolean aRebuild) {mRebuild = aRebuild; return this;}
        public LibBuilder setRedirectLibPath(@Nullable String aRedirectLibPath) {mRedirectLibPath = aRedirectLibPath; return this;}
        public LibBuilder setCmakeLineOpt(@Nullable IUnaryFullOperator<? extends CharSequence, ? super String> aCmakeLineOpt) {mCmakeLineOpt = aCmakeLineOpt; return this;}
        
        @Override public String get() {
            // 如果开启了 USE_MIMALLOC 则增加 MiMalloc 依赖
            if (mUseMiMalloc!=null && mUseMiMalloc) MiMalloc.InitHelper.init();
            String tLibPath;
            if (mRedirectLibPath == null) {
                @Nullable String tLibName = LIB_NAME_IN(mLibDir, mProjectName);
                // 如果不存在 jni lib 则需要重新通过源码编译
                if (mRebuild || tLibName == null) {
                    System.out.println(mInfoProjectName +" INIT INFO: "+ mProjectName +" libraries not found. Reinstalling...");
                    try {
                        tLibName = initLib_();
                    } catch (Exception e) {throw new RuntimeException(e);}
                }
                tLibPath = mLibDir + tLibName;
            } else {
                if (DEBUG) System.out.println(mInfoProjectName +" INIT INFO: "+ mProjectName +" libraries are redirected to '" + mRedirectLibPath + "'");
                tLibPath = mRedirectLibPath;
            }
            return tLibPath;
        }
        
        
        private String cmakeInitCmd_() throws IOException {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("cmake");
            // 这里设置 C/C++ 编译器（如果有）
            if (mCmakeCCompiler   != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="  + mCmakeCCompiler);}
            if (mCmakeCxxCompiler != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+ mCmakeCxxCompiler);}
            if (mCmakeCFlags      != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS='"    + mCmakeCFlags +"'");}
            if (mCmakeCxxFlags    != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"  + mCmakeCxxFlags +"'");}
            // 配置其余的参数设置
            if (mUseMiMalloc != null) {
                rCommand.add("-D"); rCommand.add("JSE_USE_MIMALLOC="+(mUseMiMalloc ?"ON":"OFF"));
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
            // 优先检测环境
            if (mEnvChecker != null) mEnvChecker.check();
            // 检测 cmake，为了简洁并避免问题，现在要求一定要有 cmake 环境
            EXEC.setNoSTDOutput().setNoERROutput();
            boolean tNoCmake = EXEC.system("cmake --version") != 0;
            EXEC.setNoSTDOutput(false).setNoERROutput(false);
            if (tNoCmake) {
                System.err.println("No cmake found, you can download cmake from: https://cmake.org/download/");
                throw new Exception(mInfoProjectName +" BUILD ERROR: No cmake environment.");
            }
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
                    System.err.println(mInfoProjectName +" INIT WARNING: Build directory ("+tWorkingDir+") contains inappropriate characters, build may fail.");
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
                    if (mUseMiMalloc!=null && mUseMiMalloc) {
                        line = line.replace("$ENV{JSE_MIMALLOC_INCLUDE_DIR}", MiMalloc.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                                   .replace("$ENV{JSE_MIMALLOC_LIB_PATH}"   , MiMalloc.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
                    }
                    return mCmakeLineOpt.apply(line);
                });
                // 覆盖旧的 CMakeLists.txt
                IO.move(tSrcDir+tCmakeListsDir+"CMakeLists1.txt", tSrcDir+tCmakeListsDir+"CMakeLists.txt");
            }
            // 开始通过 cmake 编译
            System.out.println(mInfoProjectName +" INIT INFO: Building "+ mProjectName +" from source code...");
            String tBuildDir = mBuildDirIniter.init(tSrcDir);
            // 直接通过系统指令来编译库，关闭输出
            EXEC.setNoSTDOutput().setWorkingDir(tBuildDir);
            // 现在初始化 cmake 和参数设置放在一起执行
            EXEC.system(cmakeInitCmd_());
            // 最后进行构造操作
            EXEC.system("cmake --build . --config Release");
            EXEC.setNoSTDOutput(false).setWorkingDir(null);
            // 简单检测一下是否编译成功
            @Nullable String tLibName = LIB_NAME_IN(mLibDir, mProjectName);
            if (tLibName == null) {
                if (tWorkingDirValid) {
                    System.err.println("Build Failed, this may be caused by the lack of a C/C++ compiler");
                    if (IS_WINDOWS) {
                        System.err.println("  For Windows, you can use MSVC: https://visualstudio.microsoft.com/vs/features/cplusplus/");
                    } else {
                        System.err.println("  For Liunx/Mac, you can use GCC: https://gcc.gnu.org/");
                        System.err.println("  For Ubuntu, you can use `sudo apt install g++`");
                    }
                } else {
                    System.err.println("Build Failed, this may be caused by the inappropriate characters in build directory: "+tWorkingDir);
                }
                throw new Exception(mInfoProjectName +" BUILD ERROR: Build Failed, No "+ mProjectName +" lib in '"+ mLibDir +"'");
            }
            // 完事后移除临时解压得到的源码
            IO.removeDir(tWorkingDir);
            System.out.println(mInfoProjectName +" INIT INFO: "+ mProjectName +" successfully installed.");
            System.out.println(mInfoProjectName +" LIB DIR: " + mLibDir);
            // 输出安装完成后的库名称
            return tLibName;
        }
    }
}
