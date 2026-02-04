package jse.clib;

import jse.code.IO;
import jse.code.OS;
import jse.code.UT;

import java.io.BufferedReader;
import java.net.URI;
import java.nio.charset.Charset;

import static jse.code.OS.*;

/**
 * 编译 jni 需要的 ninja，用于加速和规范化 cmake 的构建生成过程
 * @author liqa
 */
public class Ninja {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link Ninja} 相关的参数是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link Ninja} 相关的参数 */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(EXE_PATH);
        }
    }
    
    public final static class Conf {
        /** 是否检测使用系统的 ninja，现在默认不使用避免版本老旧带来的问题 */
        public static boolean USE_SYSTEM = OS.envZ("JSE_USE_SYSTEM_NINJA", false);
    }
    
    /** 自动下载使用的 ninja 版本 */
    public final static String VERSION = "1.13.2";
    /** 内部 ninja 会使用的路径 */
    public final static String INTERNAL_HOME = JAR_DIR+"ninja/core/" + UT.Code.uniqueID(OS.OS_NAME, Ninja.VERSION) + "/";
    private final static boolean USE_SYSTEM_ = Conf.USE_SYSTEM;
    /** 自动检测到的 ninja 可执行路径 */
    public final static String EXE_PATH;
    /** 拼接后可以执行的命令，对于 windows 和 linux 专门适配 */
    public final static String EXE_CMD;
    
    
    private static boolean FIRST_PRINT = true;
    /** 虽然不需要 ninja 路径来确定路径，但为了统一还是提供一个延迟打印接口，仅第一次构建时打印提示 */
    public static void printInfo() {
        if (!FIRST_PRINT) return;
        FIRST_PRINT = false;
        if (USE_SYSTEM_ && EXE_PATH!=null) {
            System.out.printf(IO.Text.cyan("COMPILER INFO:")+" Use Ninja in %s\n", EXE_PATH);
        }
    }
    
    private static String getExePath_() throws Exception {
        if (USE_SYSTEM_) {
            EXEC.setNoSTDOutput().setNoERROutput();
            boolean tHasNinja = EXEC.system("ninja --version") == 0;
            EXEC.setNoSTDOutput(false).setNoERROutput(false);
            if (tHasNinja) {
                String tNinjaPath;
                if (IS_WINDOWS) {
                    tNinjaPath = EXEC.system_str("(Get-Command ninja).Path").get(0);
                } else {
                    tNinjaPath = EXEC.system_str("which ninja").get(0);
                }
                if (IO.exists(tNinjaPath)) return tNinjaPath;
            }
        }
        // 没有系统库则尝试使用内部库
        String tInternalNinjaPath = INTERNAL_HOME + "ninja";
        if (IS_WINDOWS) tInternalNinjaPath += ".exe";
        if (IO.exists(tInternalNinjaPath)) return tInternalNinjaPath;
        // 没有则使用缓存的 ninja 压缩包，这里只考虑 x86 的情况
        String tNinjaPkgName = "ninja-" + (IS_WINDOWS ? "win" : (IS_MAC ? "mac" : "linux")) + ".zip";
        String tNinjaCachePath = JNIUtil.PKG_DIR + tNinjaPkgName;
        if (!IO.exists(tNinjaCachePath)) {
            System.out.println(IO.Text.green("JNI INIT INFO:")+" No correct Ninja pkg detected");
            System.out.println(IO.Text.yellow("Auto download Ninja? (Y/n)"));
            BufferedReader tReader = IO.toReader(System.in, Charset.defaultCharset());
            String tLine = tReader.readLine();
            while (true) {
                if (tLine.equalsIgnoreCase("n")) {
                    throw new Exception("user interrupted");
                }
                if (tLine.isEmpty() || tLine.equalsIgnoreCase("y")) {
                    break;
                }
                System.out.println(IO.Text.yellow("Auto download Ninja? (Y/n)"));
            }
            String tNinjaUrl = String.format("https://github.com/ninja-build/ninja/releases/download/v%s/%s", VERSION, tNinjaPkgName);
            System.out.println("Downloading "+IO.Text.underline(tNinjaUrl));
            System.out.println("  or you can download it manually and put into "+JNIUtil.PKG_DIR);
            String tTempPath = tNinjaCachePath + ".tmp_"+UT.Code.randID();
            IO.copy(URI.create(tNinjaUrl).toURL(), tTempPath);
            IO.move(tTempPath, tNinjaCachePath);
            System.out.println(IO.Text.green("JNI INIT INFO:")+" Ninja pkg downloading finished.");
        }
        // 解压
        System.out.println(IO.Text.green("JNI INIT INFO:")+" Extracting Ninja...");
        IO.zip2dir(tNinjaCachePath, INTERNAL_HOME);
        return tInternalNinjaPath;
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        try {EXE_PATH = getExePath_();}
        catch (Exception e) {throw new RuntimeException(e);}
        EXE_CMD = (IS_WINDOWS?"& \"":"\"") + EXE_PATH + "\"";
    }
}
