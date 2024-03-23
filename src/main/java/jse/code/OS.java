package jse.code;

import jse.Main;
import jse.system.BashSystemExecutor;
import jse.system.ISystemExecutor;
import jse.system.PowerShellSystemExecutor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Future;

/**
 * 系统相关操作，包括 jar 包的位置，执行系统指令，判断系统类型，获取工作目录等；
 * 现在变为独立的类而不是放在 {@link CS} 或 {@link UT} 中
 * @author liqa
 */
public class OS {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(WORKING_DIR);
        }
    }
    
    public final static boolean IS_WINDOWS = System.getProperty("os.name").toLowerCase().contains("windows");
    public final static boolean IS_MAC = System.getProperty("os.name").toLowerCase().contains("mac");
    public final static String NO_LOG_LINUX = "/dev/null";
    public final static String NO_LOG_WIN = "NUL";
    public final static String NO_LOG = IS_WINDOWS ? NO_LOG_WIN : NO_LOG_LINUX;
    
    public final static ISystemExecutor EXE;
    public final static String JAR_PATH;
    public final static String JAR_DIR;
    public final static String USER_HOME;
    public final static String USER_HOME_DIR;
    public final static String WORKING_DIR;
    final static Path WORKING_DIR_PATH;
    
    static {
        InitHelper.INITIALIZED = true;
        // 先获取 user.home
        USER_HOME = System.getProperty("user.home"); // user.home 这里统一认为 user.home 就是绝对路径
        USER_HOME_DIR = UT.IO.toInternalValidDir(USER_HOME);
        // 然后通过执行指令来初始化 WORKING_DIR；
        // 这种写法可以保证有最大的兼容性，即使后续 EXE 可能非法（不是所有平台都有 bash）
        String wd = USER_HOME;
        Process tProcess = null;
        try {tProcess = Runtime.getRuntime().exec(IS_WINDOWS ? "cmd /c cd" : "pwd");}
        catch (IOException ignored) {}
        if (tProcess != null) {
            try (BufferedReader tReader = new BufferedReader(new InputStreamReader(tProcess.getInputStream()))) {
                tProcess.waitFor();
                wd = tReader.readLine().trim();
            } catch (Exception ignored) {
            } finally {
                tProcess.destroy();
            }
        }
        // 全局修改工作目录为正确的目录
        System.setProperty("user.dir", wd);
        // jse 内部使用的 dir 需要末尾增加 `/`
        WORKING_DIR = UT.IO.toInternalValidDir(wd);
        WORKING_DIR_PATH = Paths.get(WORKING_DIR);
        
        // 获取此 jar 的路径
        // 默认这样获取而不是通过 System.getProperty("java.class.path")，为了避免此属性有多个 jar
        Path tJarPath;
        try {
            URI tJarURI = CS.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            tJarPath = WORKING_DIR_PATH.resolve(Paths.get(tJarURI));
        } catch (Exception e) {
            // 在 linux 中这个路径可能是相对路径，为了避免库安装错误这里统一获取一下绝对路径
            // 现在应该可以随意使用 UT.IO 而不会循环初始化
            tJarPath = UT.IO.toAbsolutePath_(System.getProperty("java.class.path"));
        }
        JAR_PATH = tJarPath.toString();
        Path tJarDirPath = tJarPath.getParent();
        String tJarDir = tJarDirPath==null ? "" : tJarDirPath.toString();
        tJarDir = UT.IO.toInternalValidDir(tJarDir);
        JAR_DIR = tJarDir;
        // 创建默认 EXE，无内部线程池，windows 下使用 powershell 而 linux 下使用 bash 统一指令；
        // 这种选择可以保证指令使用统一，即使这些终端不一定所有平台都有
        EXE = IS_WINDOWS ? new PowerShellSystemExecutor() : new BashSystemExecutor();
        // 在程序结束时关闭 EXE
        Main.addGlobalAutoCloseable(EXE);
    }
    
    
    /** 更加易用的获取环境变量的接口 */
    public static @Nullable String env(String aName) {
        try {return System.getenv(aName);}
        catch (Throwable ignored) {} // 获取失败不抛出错误，在 jse 中获取环境变量都是非必要的
        return null;
    }
    @Contract("_, !null -> !null")
    public static String env(String aName, String aDefault) {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : tEnv;
    }
    public static int envI(String aName, int aDefault) throws NumberFormatException {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : Integer.parseInt(tEnv);
    }
    public static double envD(String aName, double aDefault) throws NumberFormatException {
        String tEnv = env(aName);
        return tEnv==null ? aDefault : Double.parseDouble(tEnv);
    }
    public static boolean envZ(String aName, boolean aDefault) throws NumberFormatException {
        String tEnv = env(aName);
        if (tEnv == null) return aDefault;
        tEnv = tEnv.toLowerCase();
        switch(tEnv) {
        case "true" : case "t": case "on" : case "yes": case "1": {return true ;}
        case "false": case "f": case "off": case "no" : case "0": {return false;}
        default: {throw new NumberFormatException("For input string: \""+tEnv+"\"");}
        }
    }
    
    /** 提供这些接口方便外部调用使用 */
    @VisibleForTesting public static ISystemExecutor exec() {return jse.code.OS.EXE;}
    @VisibleForTesting public static int system(String aCommand) {return exec().system(aCommand);}
    @VisibleForTesting public static int system(String aCommand, String aOutFilePath) {return exec().system(aCommand, aOutFilePath);}
    @VisibleForTesting public static Future<Integer> submitSystem(String aCommand) {return exec().submitSystem(aCommand);}
    @VisibleForTesting public static Future<Integer> submitSystem(String aCommand, String aOutFilePath) {return exec().submitSystem(aCommand, aOutFilePath);}
    @VisibleForTesting public static List<String> system_str(String aCommand) {return exec().system_str(aCommand);}
    @VisibleForTesting public static Future<List<String>> submitSystem_str(String aCommand) {return exec().submitSystem_str(aCommand);}
}
