package jse.clib;

import jse.code.CS;
import jse.code.UT;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static jse.code.CS.Exec.EXE;
import static jse.code.CS.Exec.JAR_DIR;
import static jse.code.Conf.*;

/**
 * 其他 jni 库或者此项目需要依赖的 c 库；
 * 一种加速 c 中 malloc 和 free 的库。
 * @see <a href="https://github.com/microsoft/mimalloc"> microsoft/mimalloc </a>
 * @author liqa
 */
public class MiMalloc {
    private MiMalloc() {}
    
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(HOME);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义构建 mimalloc 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = UT.Exec.env("JSE_CMAKE_C_COMPILER_MIMALLOC"  , jse.code.Conf.CMAKE_C_COMPILER  );
        public static @Nullable String CMAKE_CXX_COMPILER = UT.Exec.env("JSE_CMAKE_CXX_COMPILER_MIMALLOC", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = UT.Exec.env("JSE_CMAKE_C_FLAGS_MIMALLOC"     , jse.code.Conf.CMAKE_C_FLAGS     );
        public static @Nullable String CMAKE_CXX_FLAGS    = UT.Exec.env("JSE_CMAKE_CXX_FLAGS_MIMALLOC"   , jse.code.Conf.CMAKE_CXX_FLAGS   );
    }
    
    
    public final static String VERSION = "2.1.2";
    
    public final static String HOME = JAR_DIR+"mimalloc/" + UT.Code.uniqueID(CS.VERSION, MiMalloc.VERSION) + "/";
    public final static String LIB_DIR = HOME+"lib/";
    public final static String INCLUDE_DIR = HOME+"include/";
    public final static String LIB_PATH;
    public final static String LLIB_PATH;
    
    
    private static String cmakeInitCmd_(String aMiBuildDir) {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cd"); rCommand.add("\""+aMiBuildDir+"\""); rCommand.add(";");
        rCommand.add("cmake");
        // 这里设置 C/C++ 编译器（如果有）
        if (Conf.CMAKE_C_COMPILER   != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="  +Conf.CMAKE_C_COMPILER    );}
        if (Conf.CMAKE_CXX_COMPILER != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+Conf.CMAKE_CXX_COMPILER  );}
        if (Conf.CMAKE_C_FLAGS      != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS=\""   +Conf.CMAKE_C_FLAGS  +"\"");}
        if (Conf.CMAKE_CXX_FLAGS    != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_FLAGS=\"" +Conf.CMAKE_CXX_FLAGS+"\"");}
        // 初始化使用上一个目录的 CMakeList.txt
        rCommand.add("..");
        return String.join(" ", rCommand);
    }
    private static String cmakeSettingCmd_(String aMiBuildDir) throws IOException {
        // 设置参数，这里使用 List 来构造这个长指令
        List<String> rCommand = new ArrayList<>();
        rCommand.add("cd"); rCommand.add("\""+aMiBuildDir+"\""); rCommand.add(";");
        rCommand.add("cmake");
        // 设置只输出动态链接库
        rCommand.add("-D"); rCommand.add("MI_BUILD_SHARED=ON");
        rCommand.add("-D"); rCommand.add("MI_BUILD_STATIC=OFF");
        rCommand.add("-D"); rCommand.add("MI_BUILD_OBJECT=OFF");
        rCommand.add("-D"); rCommand.add("MI_BUILD_TESTS=OFF");
        // 目前不需要 override 和 redirect
        rCommand.add("-D"); rCommand.add("MI_OVERRIDE=OFF");
        rCommand.add("-D"); rCommand.add("MI_WIN_REDIRECT=OFF");
        rCommand.add("-D"); rCommand.add("MI_OSX_INTERPOSE=OFF");
        rCommand.add("-D"); rCommand.add("MI_OSX_ZONE=OFF");
        // 虽然在 msvc 上设置 c++ 编译器可以得到更好的性能，但是这个是自动设置的，因此这里保持默认来保证有最大的兼容性
//      rCommand.add("-D"); rCommand.add("MI_USE_CXX=ON");
        // 设置编译模式 Release
        rCommand.add("-D"); rCommand.add("CMAKE_BUILD_TYPE=Release");
        // 设置构建输出目录为 lib
        UT.IO.makeDir(LIB_DIR); // 初始化一下这个目录避免意料外的问题
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH=\""+ LIB_DIR +"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH=\""+ LIB_DIR +"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH=\""+ LIB_DIR +"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH=\""+ LIB_DIR +"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH=\""+ LIB_DIR +"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH=\""+ LIB_DIR +"\"");
        rCommand.add(".");
        return String.join(" ", rCommand);
    }
    private static @NotNull String initMiMalloc_() throws Exception {
        // 检测 cmake，这里要求一定要有 cmake 环境
        EXE.setNoSTDOutput().setNoERROutput();
        boolean tNoCmake = EXE.system("cmake --version") != 0;
        EXE.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCmake) throw new Exception("MIMALLOC BUILD ERROR: No camke environment.");
        String tWorkingDir = WORKING_DIR_OF("mimalloc");
        // 如果已经存在则先删除
        UT.IO.removeDir(tWorkingDir);
        // 首先获取源码路径，这里直接从 resource 里输出
        String tMiZipPath = tWorkingDir+"mimalloc-"+VERSION+".zip";
        UT.IO.copy(UT.IO.getResource("mimalloc/mimalloc-"+VERSION+".zip"), tMiZipPath);
        // 解压 mimalloc 包到临时目录，如果已经存在则直接清空此目录
        String tMiDir = tWorkingDir+"mimalloc/";
        UT.IO.removeDir(tMiDir);
        UT.IO.zip2dir(tMiZipPath, tMiDir);
        // 安装 mimalloc 包，这里通过 cmake 来安装
        System.out.println("MIMALLOC INIT INFO: Installing mimalloc from source code...");
        String tMiBuildDir = tMiDir+"build/";
        UT.IO.makeDir(tMiBuildDir);
        // 直接通过系统指令来编译 mimalloc 的库，关闭输出
        EXE.setNoSTDOutput();
        // 初始化 cmake
        EXE.system(cmakeInitCmd_(tMiBuildDir));
        // 设置参数
        EXE.system(cmakeSettingCmd_(tMiBuildDir));
        // 最后进行构造操作
        EXE.system(String.format("cd \"%s\"; cmake --build . --config Release", tMiBuildDir));
        EXE.setNoSTDOutput(false);
        // 简单检测一下是否编译成功
        @Nullable String tLibName = LIB_NAME_IN(LIB_DIR, "mimalloc");
        if (tLibName == null) throw new Exception("MIMALLOC BUILD ERROR: No mimalloc lib in "+LIB_DIR);
        // 手动拷贝头文件到指定目录
        UT.IO.copy(tMiDir+"include/mimalloc.h", INCLUDE_DIR+"mimalloc.h");
        // 完事后移除临时解压得到的源码
        UT.IO.removeDir(tWorkingDir);
        System.out.println("MIMALLOC INIT INFO: mimalloc successfully installed.");
        // 输出安装完成后的库名称
        return tLibName;
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        @Nullable String tLibName = LIB_NAME_IN(LIB_DIR, "mimalloc");
        // 如果不存在 mimalloc lib 则需要重新通过源码编译
        if (tLibName == null) {
            System.out.println("MIMALLOC INIT INFO: mimalloc libraries not found. Reinstalling...");
            try {tLibName = initMiMalloc_();} catch (Exception e) {throw new RuntimeException(e);}
        }
        LIB_PATH = LIB_DIR+tLibName;
        @Nullable String tLLibName = LLIB_NAME_IN(LIB_DIR, "mimalloc");
        LLIB_PATH = tLLibName==null ? LIB_PATH : (LIB_DIR+tLLibName);
        // 设置库路径
        System.load(UT.IO.toAbsolutePath(LIB_PATH));
    }
}
