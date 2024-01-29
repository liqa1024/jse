package jtool.clib;

import jtool.code.UT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static jtool.code.CS.Exec.EXE;
import static jtool.code.CS.Exec.JAR_DIR;
import static jtool.code.CS.*;

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
            if (!INITIALIZED) String.valueOf(MIMALLOC_DIR);
        }
    }
    
    public final static String MIMALLOC_VERSION = "2.1.2+4e50d67";
    
    public final static String MIMALLOC_DIR = JAR_DIR+"mimalloc/" + UT.Code.uniqueID(VERSION, MIMALLOC_VERSION) + "/";
    public final static String MIMALLOC_LIB_DIR = MIMALLOC_DIR+"lib/";
    public final static String MIMALLOC_INCLUDE_DIR = MIMALLOC_DIR+"include/";
    public final static String MIMALLOC_LIB_PATH = MIMALLOC_LIB_DIR + (IS_WINDOWS ? "mimalloc.dll" : (IS_MAC ? "libmimalloc.dylib" : "libmimalloc.so"));
    
    
    private static String initCmakeSettingCmd_(String aMiBuildDir) throws IOException {
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
        // 设置编译模式 Release
        rCommand.add("-D"); rCommand.add("CMAKE_BUILD_TYPE=Release");
        // 设置构建输出目录为 lib
        UT.IO.makeDir(MIMALLOC_LIB_DIR); // 初始化一下这个目录避免意料外的问题
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH=\""+MIMALLOC_LIB_DIR+"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH=\""+MIMALLOC_LIB_DIR+"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH=\""+MIMALLOC_LIB_DIR+"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH=\""+MIMALLOC_LIB_DIR+"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH=\""+MIMALLOC_LIB_DIR+"\"");
        rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH=\""+MIMALLOC_LIB_DIR+"\"");
        rCommand.add(".");
        return String.join(" ", rCommand);
    }
    private static void initMiMalloc_() throws Exception {
        // 检测 cmake，这里要求一定要有 cmake 环境
        EXE.setNoSTDOutput().setNoERROutput();
        boolean tNoCmake = EXE.system("cmake --version") != 0;
        EXE.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoCmake) throw new Exception("MIMALLOC BUILD ERROR: No camke environment.");
        String tWorkingDir = WORKING_DIR.replaceAll("%n", "mimalloc");
        // 如果已经存在则先删除
        UT.IO.removeDir(tWorkingDir);
        // 首先获取源码路径，这里直接从 resource 里输出
        String tMiZipPath = tWorkingDir+"mimalloc.zip";
        UT.IO.copy(UT.IO.getResource("mimalloc/mimalloc.zip"), tMiZipPath);
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
        EXE.system(String.format("cd \"%s\"; cmake ..", tMiBuildDir));
        // 设置参数
        EXE.system(initCmakeSettingCmd_(tMiBuildDir));
        // 最后进行构造操作
        EXE.system(String.format("cd \"%s\"; cmake --build . --config Release", tMiBuildDir));
        EXE.setNoSTDOutput(false);
        // 简单检测一下是否编译成功
        if (!UT.IO.isFile(MIMALLOC_LIB_PATH)) throw new Exception("MIMALLOC BUILD ERROR: No mimalloc lib in "+MIMALLOC_LIB_PATH);
        // 手动拷贝头文件到指定目录
        UT.IO.copy(tMiDir+"include/mimalloc.h", MIMALLOC_INCLUDE_DIR+"mimalloc.h");
        // 完事后移除临时解压得到的源码
        UT.IO.removeDir(tWorkingDir);
        System.out.println("MIMALLOC INIT INFO: mimalloc successfully installed.");
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        // 如果不存在 mimalloc lib 则需要重新通过源码编译
        if (!UT.IO.isFile(MIMALLOC_LIB_PATH)) {
            System.out.println("MIMALLOC INIT INFO: mimalloc libraries not found. Reinstalling...");
            try {initMiMalloc_();}
            catch (Exception e) {throw new RuntimeException(e);}
        }
        // 设置库路径
        System.load(UT.IO.toAbsolutePath(MIMALLOC_LIB_PATH));
    }
}
