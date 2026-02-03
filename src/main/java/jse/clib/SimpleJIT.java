package jse.clib;

import jse.cptr.ICPointer;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.parallel.IAutoShutdown;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.*;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.Conf.DEBUG;
import static jse.code.Conf.LIB_NAME_IN;
import static jse.code.Conf.VERSION_MASK;
import static jse.code.OS.*;

/**
 * 简单的 JIT 实现，这里实际写入文件到临时目录，组装代码后调用系统编译器编译
 * <p>
 * 这里给一个通用的函数调用接口（需要编译的函数都需要满足此接口），为了实际使用方便没有做极限的压缩
 * @author liqa
 */
@ApiStatus.Experimental
public class SimpleJIT {
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link SimpleJIT} 相关的 JNI 库是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link SimpleJIT} 相关的 JNI 库 */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            if (!INITIALIZED) String.valueOf(LIB_PATH);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义构建 jitengine 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_JIT");
        
        /**
         * 自定义构建 jitengine 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_C_COMPILER_JIT} 来设置
         */
        public static @Nullable String CMAKE_C_COMPILER = OS.env("JSE_CMAKE_C_COMPILER_JIT", jse.code.Conf.CMAKE_C_COMPILER);
        /**
         * 自定义构建 jitengine 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_C_FLAGS_JIT} 来设置
         */
        public static @Nullable String CMAKE_C_FLAGS    = OS.env("JSE_CMAKE_C_FLAGS_JIT"   , jse.code.Conf.CMAKE_C_FLAGS);
        
        /**
         * 控制是否在 jit 编译完成后自动清理工作目录，可以用于 debug
         * <p>
         * 也可使用环境变量 {@code JSE_JIT_CLEAN} 来设置
         */
        public static boolean CLEAN = OS.envZ("JSE_JIT_CLEAN", true);
    }
    
    /** 完全关闭 jit 的优化，主要用于调试或需要精确结果而不是速度 */
    public static final int OPTIM_NONE = -1;
    /** 兼容性的 jit 的优化，只开启 fmath，理论上保持相同的跨机器兼容性 */
    public static final int OPTIM_COMPAT = 0;
    /** （默认）基本的 jit 的优化，会开启一般 x86 cpu 都有的 avx2 指令集 */
    public static final int OPTIM_BASE = 1;
    /** 最高的 jit 的优化，会开启 avx512 指令集，有时可能会更慢 */
    public static final int OPTIM_MAX = 2;
    
    /** 当前 {@link SimpleJIT} JNI 库所在的文件夹路径，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR = JAR_DIR+"jit/engine/" + UT.Code.uniqueID(OS.OS_NAME, Compiler.EXE_PATH, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, Conf.CMAKE_C_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_SETTING) + "/";
    public final static String CACHE_LIB_DIR = JAR_DIR+"jit/cache/";
    /** 当前 {@link SimpleJIT} JNI 库的路径 */
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jse_clib_SimpleJIT.c"
        , "jse_clib_SimpleJIT.h"
        , "jse_clib_JITLibHandle.h"
    };
    private final static String JIT_SRC_NAME = "jitsrc.cpp", JIT_HEAD_NAME = "jitsrc.h";
    
    static {
        InitHelper.INITIALIZED = true;
        LIB_PATH = new JNIUtil.LibBuilder("jitengine", "JIT", LIB_DIR, Conf.CMAKE_SETTING)
            .setSrc("jitengine", SRC_NAME)
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS)
            .setCmakeLineOp(null)
            .get();
        // 设置库路径，这里直接使用 System.load
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    public static Engine engine() {return new Engine();}
    
    public static class Method implements ICPointer {
        private final Engine mEngine;
        private final long mMethodPtr;
        Method(long aMethodPtr, Engine aEngine) {
            mMethodPtr = aMethodPtr;
            mEngine = aEngine;
        }
        
        @UnsafeJNI("Inputs mismatch or invalid usage will result in JVM SIGSEGV")
        public int invoke(ICPointer aDataIn, ICPointer rDataOut) {
            if (mEngine.isShutdown()) throw new RuntimeException("this JIT engine is dead");
            if (isNull()) throw new NullPointerException();
            return invokeMethod0(mMethodPtr, aDataIn.ptr_(), rDataOut.ptr_());
        }
        @Override public long ptr_() {return mMethodPtr;}
    }
    
    @FunctionalInterface public interface IDirIniter {String init(String aInput) throws Exception;}
    public static class Engine implements IAutoShutdown {
        /// compiler stuffs
        private String mLibDir = null;
        private String mProjectName = null;
        private @Nullable String mSrc = null;
        private @Nullable IDirIniter mSrcDirIniter = null;
        private @Nullable String mCmakeCxxCompiler = null, mCmakeCxxFlags = null;
        private final Map<String, String> mCmakeSettings = new LinkedHashMap<>();
        private int mOptimLevel = OPTIM_BASE;
        
        /// jit engins stuffs
        private Boolean mCacheLib = null;
        private String mLibPath = null;
        private @Nullable JITLibHandle mLibHandle = null;
        private final Set<String> mMethodNames = new LinkedHashSet<>();
        private boolean mDead = false;
        
        /// initer
        Engine() {}
        public Engine setSrc(@Language(value="C++", prefix="extern \"C\" {", suffix="}") String aSrc) {
            mSrc = aSrc;
            return this;
        }
        public Engine setProjectName(String aProjectName) {
            mProjectName = aProjectName;
            return this;
        }
        public Engine setLibDir(String aLibDir) {
            mLibDir = aLibDir;
            return this;
        }
        public Engine setOptimLevel(int aOptimLevel) {
            mOptimLevel = aOptimLevel;
            return this;
        }
        public Engine setMethodNames(String... aMethodNames) {
            mMethodNames.clear();
            mMethodNames.addAll(AbstractCollections.from(aMethodNames));
            return this;
        }
        public Engine setMethodNames(Collection<? extends CharSequence> aMethodNames) {
            mMethodNames.clear();
            mMethodNames.addAll(AbstractCollections.map(aMethodNames, Object::toString));
            return this;
        }
        public Engine addMethodName(CharSequence aMethodName) {
            mMethodNames.add(aMethodName.toString());
            return this;
        }
        public Engine removeMethodName(CharSequence aMethodName) {
            mMethodNames.remove(aMethodName.toString());
            return this;
        }
        public Engine setCmakeSettings(Map<? extends CharSequence, ? extends CharSequence> aCmakeSettings) {
            mCmakeSettings.clear();
            aCmakeSettings.forEach((key, value) -> mCmakeSettings.put(key.toString(), value.toString()));
            return this;
        }
        public Engine putCmakeSetting(CharSequence aKey, CharSequence aValue) {
            mCmakeSettings.put(aKey.toString(), aValue.toString());
            return this;
        }
        public Engine removeCmakeSetting(CharSequence aKey) {
            mCmakeSettings.remove(aKey.toString());
            return this;
        }
        public Engine setSrcDirIniter(IDirIniter aSrcDirIniter) {
            mSrcDirIniter = aSrcDirIniter;
            return this;
        }
        public Engine setCmakeCxxCompiler(@Nullable String aCmakeCxxCompiler) {
            mCmakeCxxCompiler = aCmakeCxxCompiler;
            return this;
        }
        public Engine setCmakeCxxFlags(@Nullable String aCmakeCxxFlags) {
            mCmakeCxxFlags = aCmakeCxxFlags;
            return this;
        }
        
        /// utils
        public boolean hasMethod(CharSequence aMethodName) {
            return mMethodNames.contains(aMethodName.toString());
        }
        public boolean isNull() {
            return mLibHandle==null || mLibHandle.isNull();
        }
        
        /// workflow, comile() -> findMethod(name)
        public void compile() throws Exception {
            if (mDead) throw new RuntimeException("this JIT engine is dead");
            // 现在总是优先依赖 jniutil 用来确保一些通用的检测总是优先执行
            JNIUtil.InitHelper.init();
            // 不能重复编译
            if (mLibHandle!=null) throw new IllegalStateException("Repeated compile().");
            // 合理化目录，确认是否使用缓存
            validLibCache_();
            // 没有检测到缓存，开始编译
            if (mLibPath==null) {
                if (mCacheLib) {
                    System.out.println(IO.Text.cyan("JIT INIT INFO:")+" No cache lib "+mProjectName+" found in "+mLibDir+", re-compile...");
                } else {
                    if (DEBUG) System.out.println(IO.Text.cyan("JIT INIT INFO:")+" Compile (no-cache mode)...");
                }
                // 开始运行时编译
                String tLibName = initLib_();
                mLibPath = mLibDir + tLibName;
            }
            // 加载库
            long tPtr = loadLibrary0(mLibPath);
            mLibHandle = new JITLibHandle(this, tPtr, mCacheLib?null:mLibDir);
        }
        public Method findMethod(CharSequence aMethodName) throws JITException {
            if (mDead) throw new RuntimeException("this JIT engine is dead");
            if (mLibHandle==null) throw new IllegalStateException("Require compile() first.");
            if (mLibHandle.isNull()) throw new NullPointerException();
            if (!hasMethod(aMethodName)) return null;
            return new Method(findMethod0(mLibHandle.mPtr, aMethodName.toString()), this);
        }
        @Override public void shutdown() {
            if (mDead) return;
            mDead = true;
            if (mLibHandle!=null) mLibHandle.dispose();
        }
        public boolean isShutdown() {return mDead;}
        
        
        private void validLibCache_() {
            if (mCacheLib!=null) return;
            // 有设置 lib dir 的情况优先检查是否有缓存
            if (mLibDir!=null && mProjectName!=null) {
                mLibDir = IO.toInternalValidDir(IO.toAbsolutePath(mLibDir));
                mCacheLib = true;
            } else {
                // 使用默认值
                if (mLibDir==null) mLibDir = CACHE_LIB_DIR + UT.Code.randID()+"/";
                if (mProjectName==null) mProjectName = "jit";
                mCacheLib = false;
            }
            if (mCacheLib) {
                @Nullable String tLibName = LIB_NAME_IN(mLibDir, mProjectName);
                // 如果存在 jit lib 则直接使用缓存的 lib，自动跳过编译过程
                if (tLibName!=null) {
                    mLibPath = mLibDir + tLibName;
                    if (DEBUG) System.out.println(IO.Text.cyan("JIT INIT INFO:")+" Use cache: "+mLibPath);
                }
            }
        }
        private String cmakeInitCmd_() throws IOException {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add(CMake.EXE_CMD);
            // 这里设置 C++ 编译器
            String tCmakeCxxCompiler = mCmakeCxxCompiler==null ? Compiler.CXX_COMPILER : mCmakeCxxCompiler;
            if (tCmakeCxxCompiler!=null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+tCmakeCxxCompiler);}
            if (mCmakeCxxFlags!=null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS='"+mCmakeCxxFlags+"'");}
            // 设置构建输出目录为 lib
            IO.makeDir(mLibDir); // 初始化一下这个目录避免意料外的问题
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH='"+ mLibDir +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH='"+ mLibDir +"'");
            // 添加优化等级设置参数
            switch(mOptimLevel) {
            case OPTIM_MAX: {
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_MAX=ON");
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_BASE=OFF");
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_COMPAT=OFF");
                break;
            }
            case OPTIM_BASE: {
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_MAX=OFF");
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_BASE=ON");
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_COMPAT=OFF");
                break;
            }
            case OPTIM_COMPAT: {
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_MAX=OFF");
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_BASE=OFF");
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_COMPAT=ON");
                break;
            }
            case OPTIM_NONE: {
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_MAX=OFF");
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_BASE=OFF");
                rCommand.add("-D"); rCommand.add("JSE_OPTIM_COMPAT=OFF");
                break;
            }}
            // 添加额外的设置参数
            for (Map.Entry<String, String> tEntry : mCmakeSettings.entrySet()) {
                rCommand.add("-D"); rCommand.add(String.format("%s=%s", tEntry.getKey(), tEntry.getValue()));
            }
            // 初始化使用上一个目录的 CMakeList.txt
            rCommand.add("..");
            return String.join(" ", rCommand);
        }
        private @NotNull String initLib_() throws Exception {
            // 这里先输出编译器信息和可能的 cmake 信息，以及顺便的延迟环境检测
            Compiler.printInfo();
            CMake.printInfo(); // 目前默认不使用系统库则不会再输出
            // 从内部资源解压到临时目录，现在编译任务统一放到 jse 安装目录
            boolean tWorkingDirValid = true;
            String tWorkingDirName = "build-jit@"+UT.Code.randID() + "/";
            String tWorkingDir = JAR_DIR + tWorkingDirName;
            // 判断路径是否存在非法字符，如果存在则改为到用户目录编译
            if (JNIUtil.containsAnyInvalidChar_(tWorkingDir)) {
                String tWorkingDir2 = USER_HOME_DIR + tWorkingDirName;
                if (!JNIUtil.containsAnyInvalidChar_(tWorkingDir2)) {
                    tWorkingDir = tWorkingDir2;
                } else {
                    System.err.println(IO.Text.yellow("JIT INIT WARNING:")+" Build directory ("+tWorkingDir+") contains inappropriate characters, build may fail.");
                    tWorkingDirValid = false;
                }
            }
            // 如果已经存在则先删除
            IO.removeDir(tWorkingDir);
            // 初始化工作目录，优先尝试自定义的 mSrcDirIniter
            String tSrcDir;
            if (mSrcDirIniter!=null) {
                tSrcDir = mSrcDirIniter.init(tWorkingDir);
            } else
            if (mSrc!=null) {
                // 直接从字符串源码构建，这里需要各种配套
                tSrcDir = tWorkingDir;
                writeCmakeFile(tSrcDir, JIT_SRC_NAME);
                writeHeadFile(tSrcDir, JIT_HEAD_NAME);
                writeSrcFile(tSrcDir, JIT_SRC_NAME, JIT_HEAD_NAME);
            } else {
                throw new IllegalStateException("No source code");
            }
            // 开始通过 cmake 编译
            String tBuildDir = tSrcDir + "build/";
            IO.makeDir(tBuildDir);
            // 直接通过系统指令来编译库，关闭输出
            EXEC.setWorkingDir(tBuildDir);
            if (!DEBUG) EXEC.setNoSTDOutput();
            // 现在初始化 cmake 和参数设置放在一起执行
            EXEC.system(cmakeInitCmd_());
            // 最后进行构造操作
            String tCmd = CMake.EXE_CMD+" --build . --config Release";
            EXEC.system(tCmd);
            EXEC.setNoSTDOutput(false).setWorkingDir(null);
            // 简单检测一下是否编译成功
            @Nullable String tLibName = LIB_NAME_IN(mLibDir, mProjectName);
            if (tLibName == null) {
                System.err.println(IO.Text.red("JIT INIT ERROR:")+" Build Failed, build directory: "+tBuildDir);
                if (!tWorkingDirValid) {
                    System.err.println("  This may be caused by the inappropriate characters in working directory: "+tWorkingDir);
                }
                if (IS_WINDOWS) {
                    System.err.println("  You can use `$env:JSE_DEBUG = 1` (in powershell) to make the build output complete information");
                } else {
                    System.err.println("  You can use `export JSE_DEBUG=1` to make the build output complete information");
                }
                throw new JITException("JIT Build Failed");
            }
            // 完事后移除临时解压得到的源码，这里需要对于神秘文件系统专门处理
            if (Conf.CLEAN) {
                if (JAR_DIR_BAD_FILESYSTEM && !IS_WINDOWS) {
                    OS.printFilesystemInfo();
                    EXEC.system("rm -rf \""+tWorkingDir+"\"");
                } else {
                    IO.removeDir(tWorkingDir);
                }
            }
            // jit 情况下不需要 lib 库，这里直接手动清理
            if (IS_WINDOWS && mCacheLib) {
                for (String tName : IO.list(mLibDir)) {
                    if (tName.contains(mProjectName) && (tName.endsWith(".lib") || tName.endsWith(".exp"))) {
                        IO.delete(mLibDir+tName);
                    }
                }
            }
            if (mCacheLib) {
                System.out.println(IO.Text.cyan("JIT INIT INFO:")+" Successfully compiled lib to: "+mLibDir+tLibName+".");
            } else {
                if (DEBUG) System.out.println(IO.Text.cyan("JIT INIT INFO:")+" Compiled successfully.");
            }
            // 输出安装完成后的库名称
            return tLibName;
        }
        
        @ApiStatus.Internal
        public void writeCmakeFile(String aSrcDir, String aSrcName) throws IOException {
            IO.write(aSrcDir+"CMakeLists.txt",
                "#####################################################",
                "#  DO NOT EDIT THIS FILE - it is machine generated  #",
                "#####################################################",
                "cmake_minimum_required(VERSION 3.15)",
                "project("+mProjectName+" CXX)",
                "set(CMAKE_CXX_STANDARD 11)",
                "set(CMAKE_CXX_STANDARD_REQUIRED ON)",
                "",
                "if(NOT CMAKE_BUILD_TYPE)",
                "    set(CMAKE_BUILD_TYPE Release)",
                "endif()",
                "set(SOURCE_FILES "+aSrcName+")",
                "add_library("+mProjectName+" SHARED ${SOURCE_FILES})",
                "",
                "if(CMAKE_SYSTEM_NAME MATCHES \"Windows\")",
                "    set(BUILD_SHARED_LIBS ON)",
                "endif()",
                "set_target_properties("+mProjectName+" PROPERTIES",
                "    C_VISIBILITY_PRESET hidden",
                "    CXX_VISIBILITY_PRESET hidden",
                "    VISIBILITY_INLINES_HIDDEN YES",
                ")",
                "",
                "option(JSE_OPTIM_MAX \"Use the most aggressive optimization settings (like AVX512)\" OFF)",
                "option(JSE_OPTIM_BASE \"Use basic optimization settings\" ON)",
                "option(JSE_OPTIM_COMPAT \"Use the most compatible optimization settings\" OFF)",
                "if(CMAKE_CXX_COMPILER_ID MATCHES \"MSVC\")",
                "    if(JSE_OPTIM_MAX)",
                "        set(CMAKE_CXX_FLAGS \"${CMAKE_CXX_FLAGS} /fp:fast /arch:AVX512\")",
                "    elseif(JSE_OPTIM_BASE)",
                "        set(CMAKE_CXX_FLAGS \"${CMAKE_CXX_FLAGS} /fp:fast /arch:AVX2\")",
                "    elseif(JSE_OPTIM_COMPAT)",
                "        set(CMAKE_CXX_FLAGS \"${CMAKE_CXX_FLAGS} /fp:fast\")",
                "    endif()",
                "endif()",
                "if(CMAKE_CXX_COMPILER_ID MATCHES \"GNU\")",
                "    if(JSE_OPTIM_MAX)",
                "        set(CMAKE_CXX_FLAGS \"${CMAKE_CXX_FLAGS} -ffast-math -march=native -mavx512f -mfma\")",
                "    elseif(JSE_OPTIM_BASE)",
                "        set(CMAKE_CXX_FLAGS \"${CMAKE_CXX_FLAGS} -ffast-math -march=native -mfma\")",
                "    elseif(JSE_OPTIM_COMPAT)",
                "        set(CMAKE_CXX_FLAGS \"${CMAKE_CXX_FLAGS} -ffast-math\")",
                "    endif()",
                "endif()"
            );
        }
        @ApiStatus.Internal
        public void writeHeadFile(String aSrcDir, String aHeadName) throws IOException {
            List<String> rLines = new ArrayList<>();
            rLines.add("/* DO NOT EDIT THIS FILE - it is machine generated */");
            rLines.add("#ifndef JITSRC_H");
            rLines.add("#define JITSRC_H");
            rLines.add("");
            rLines.add("#if defined(WIN32) || defined(_WIN64) || defined(_WIN32)");
            rLines.add("#define JSE_PLUGINEXPORT __declspec(dllexport)");
            rLines.add("#define JSE_PLUGINCALL __cdecl");
            rLines.add("#else");
            rLines.add("#define JSE_PLUGINEXPORT __attribute__((visibility(\"default\")))");
            rLines.add("#define JSE_PLUGINCALL");
            rLines.add("#endif");
            rLines.add("");
            rLines.add("extern \"C\" {");
            for (String tMethodName : mMethodNames) {
            rLines.add("JSE_PLUGINEXPORT int JSE_PLUGINCALL "+tMethodName+"(void *, void *);");
            }
            rLines.add("}");
            rLines.add("#endif //JITSRC_H");
            IO.write(aSrcDir+aHeadName, rLines);
        }
        @ApiStatus.Internal
        public void writeSrcFile(String aSrcDir, String aSrcName, String aHeadName) throws IOException {
            IO.write(aSrcDir+aSrcName,
                "/* DO NOT EDIT THIS FILE - it is machine generated */",
                "#include \""+aHeadName+"\"",
                "extern \"C\" {",
                mSrc,
                "}"
            );
        }
    }
    
    private static native long loadLibrary0(String aLibPath) throws JITException;
    private static native long findMethod0(long aLibHandle, String aMethodName) throws JITException;
    private static native int invokeMethod0(long aMethodPtr, long aInPtr, long rOutPtr);
}
