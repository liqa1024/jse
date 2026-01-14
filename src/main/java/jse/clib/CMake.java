package jse.clib;

import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.charset.Charset;

import static jse.code.OS.*;

/**
 * 编译 jni 需要的 CMake，用于环境检测以及自动安装
 * @author liqa
 */
public class CMake {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link CMake} 相关的参数是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link CMake} 相关的参数 */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(EXE_PATH);
        }
    }
    
    public final static class Conf {
        /** 是否检测使用系统的 cmake，现在默认不使用避免版本老旧带来的问题 */
        public static boolean USE_SYSTEM = OS.envZ("JSE_USE_SYSTEM_CMAKE", false);
    }
    
    /** 自动下载使用的 cmake 版本 */
    public final static String VERSION = "4.2.1";
    /** 内部 cmake 会使用的路径 */
    public final static String INTERNAL_HOME = JAR_DIR+"cmake/core/" + UT.Code.uniqueID(OS.OS_NAME, CMake.VERSION) + "/";
    /** 自动检测到的 cmake 可执行路径 */
    public final static String EXE_PATH;
    /** 拼接后可以执行的命令，对于 windows 和 linux 专门适配 */
    public final static String EXE_CMD;
    
    private static @NotNull String getExePath_() throws Exception {
        if (Conf.USE_SYSTEM) {
            // 优先检测环境变量的 cmake
            EXEC.setNoSTDOutput().setNoERROutput();
            boolean tHasCmake = EXEC.system("cmake --version") == 0;
            EXEC.setNoSTDOutput(false).setNoERROutput(false);
            if (tHasCmake) {
                String tCmakePath;
                if (IS_WINDOWS) {
                    tCmakePath = EXEC.system_str("(Get-Command cmake).Path").get(0);
                } else {
                    tCmakePath = EXEC.system_str("which cmake").get(0);
                }
                if (IO.exists(tCmakePath)) return tCmakePath;
            }
        }
        // 没有系统库则尝试使用内部库
        String tInternalCmakePath = INTERNAL_HOME + "bin/cmake";
        if (IS_WINDOWS) tInternalCmakePath += ".exe";
        if (IO.exists(tInternalCmakePath)) return tInternalCmakePath;
        // 没有则使用缓存的 cmake 压缩包，这里只考虑 x86 64 位的情况
        String tCmakePkgName = "cmake-"+VERSION+"-" + (IS_WINDOWS ? "windows-x86_64.zip" : (IS_MAC ? "macos-universal.tar.gz" : "linux-x86_64.tar.gz"));
        String tCmakeCachePath = JNIUtil.PKG_DIR + tCmakePkgName;
        if (!IO.exists(tCmakeCachePath)) {
            System.out.println("JNI INIT INFO: No correct CMake pkg detected");
            System.out.println("Auto download CMake? (Y/n)");
            BufferedReader tReader = IO.toReader(System.in, Charset.defaultCharset());
            String tLine = tReader.readLine();
            while (true) {
                if (tLine.equalsIgnoreCase("n")) {
                    throw new Exception("user interrupted");
                }
                if (tLine.isEmpty() || tLine.equalsIgnoreCase("y")) {
                    break;
                }
                System.out.println("Auto download CMake? (Y/n)");
            }
            String tCmakeUrl = String.format("https://github.com/Kitware/CMake/releases/download/v%s/%s", VERSION, tCmakePkgName);
            System.out.println("Downloading "+IO.Text.underline(tCmakeUrl));
            System.out.println("  or you can download it manually and put into "+JNIUtil.PKG_DIR);
            String tTempPath = tCmakeCachePath + ".tmp_"+UT.Code.randID();
            IO.copy(URI.create(tCmakeUrl).toURL(), tTempPath);
            IO.move(tTempPath, tCmakeCachePath);
            System.out.println("JNI INIT INFO: CMake pkg downloading finished.");
        }
        // 解压
        System.out.println("JNI INIT INFO: Extracting CMake...");
        String tWorkingDir = JAR_DIR + "build-cmake@"+UT.Code.randID() + "/";
        if (IS_WINDOWS) {
            IO.zip2dir(tCmakeCachePath, tWorkingDir);
        } else {
            // tar.gz 这里直接使用系统命令解压
            IO.makeDir(tWorkingDir);
            EXEC.system("tar -zxf \""+tCmakeCachePath+"\" -C \""+tWorkingDir+"\"");
        }
        String tCmakeDir = null;
        for (String tName : IO.list(tWorkingDir)) {
            String tCmakeDir2 = tWorkingDir + tName + "/";
            if (IO.isDir(tCmakeDir2)) {
                tCmakeDir = tCmakeDir2;
                break;
            }
        }
        if (tCmakeDir == null) throw new Exception("Unzip CMake fail, working dir: " + tWorkingDir);
        // mac 压缩包会多几层，这里都解除嵌套保持一致
        if (IS_MAC) tCmakeDir += "CMake.app/Contents/";
        // 移动到需要的目录，这里需要对于神秘文件系统专门处理
        if (JAR_DIR_BAD_FILESYSTEM && !IS_WINDOWS) {
            printFilesystemInfo();
            IO.makeDir(INTERNAL_HOME);
            IO.removeDir(INTERNAL_HOME);
            int tCode = EXEC.system("mv \""+tCmakeDir+"\" \""+INTERNAL_HOME.substring(0, INTERNAL_HOME.length()-1)+"\"");
            if (tCode != 0) throw new Exception("exit code: "+tCode);
        } else {
            try {
                IO.move(tCmakeDir, INTERNAL_HOME);
            } catch (Exception e) {
                // 移动失败则尝试直接拷贝整个目录
                IO.copyDir(tCmakeDir, INTERNAL_HOME); // 不需要清除旧目录，因为编译完成会自动清理
            }
        }
        IO.removeDir(tWorkingDir);
        return tInternalCmakePath;
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        try {EXE_PATH = getExePath_();}
        catch (Exception e) {throw new RuntimeException(e);}
        System.out.printf("JNI INIT INFO: Use CMake in %s\n", EXE_PATH);
        EXE_CMD = (IS_WINDOWS?"& \"":"\"") + EXE_PATH + "\"";
    }
}
