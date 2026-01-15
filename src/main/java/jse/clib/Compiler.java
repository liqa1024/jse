package jse.clib;

import jse.code.IO;
import jse.code.OS;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

import static jse.code.OS.EXEC;
import static jse.code.OS.IS_WINDOWS;

/**
 * 编译 jni 需要的 C/C++ 编译器，目前仅用于辅助环境检测以及安装提示
 * @author liqa
 */
public class Compiler {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link Compiler} 相关的参数是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link Compiler} 相关的参数 */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(VALID);
        }
    }
    
    public final static class Conf {
        /** 是否强制检测编译器，关闭后则不进行报错，默认开启 */
        public static boolean FORCE = OS.envZ("JSE_FORCE_COMPILER", true);
    }
    
    /** 自动检测到的编译器种类，目前只考虑 windows 下的 msvc, linux 下的 gcc，mac 下的 clang */
    public final static String TYPE;
    /** 自动检测到的编译器可执行路径 */
    public final static String EXE_PATH;
    /** 拼接后可以执行的命令，对于 windows 和 linux 专门适配 */
    public final static String EXE_CMD;
    /** 自动检测到编译器是否合适，包括是否有合适的 c++ 编译器 */
    public final static boolean VALID;
    
    /**
     * 用于指定传给 cmake 的 C/C++ 编译器符号，确保在神秘 intel
     * 编译器环境下也能总是使用兼容的 gcc
     * <p>
     * {@code null} 表示不会传递
     * <p>
     * 对于 msvc 不会传递，对于关闭了 {@link Conf#FORCE} 时同样不会传递
     */
    public final static String C_COMPILER, CXX_COMPILER;
    
    
    private static boolean FIRST_PRINT = true;
    /** 由于总是需要路径来确定库路径，提供一个延迟打印接口，仅第一次构建时打印提示 */
    public static void printInfo() {
        if (!FIRST_PRINT) return;
        FIRST_PRINT = false;
        if (EXE_PATH!=null) {
            System.out.printf("JNI INIT INFO: C/C++ compiler detected in %s\n", EXE_PATH);
        }
    }
    
    private static @Nullable String getExePath_() {
        if (!Conf.FORCE) return null;
        if (!IS_WINDOWS) {
            // 非 windows 下统一简单通过执行命令检测，优先 gcc 后 clang
            // 由于版本判断较为麻烦，这里不去检测版本
            EXEC.setNoSTDOutput().setNoERROutput();
            boolean tHasGcc = EXEC.system("gcc --version") == 0;
            EXEC.setNoSTDOutput(false).setNoERROutput(false);
            if (tHasGcc) {
                String tGccPath = EXEC.system_str("which gcc").get(0);
                if (IO.exists(tGccPath)) return tGccPath;
            }
            // fallback clang
            EXEC.setNoSTDOutput().setNoERROutput();
            boolean tHasClang = EXEC.system("clang --version") == 0;
            EXEC.setNoSTDOutput(false).setNoERROutput(false);
            if (tHasClang) {
                String tClangPath = EXEC.system_str("which clang").get(0);
                if (IO.exists(tClangPath)) return tClangPath;
            }
            return null;
        }
        // windows 下总是检测 msvc
        String tVswhere = "C:\\Program Files (x86)\\Microsoft Visual Studio\\Installer\\vswhere.exe";
        if (!IO.exists(tVswhere)) return null;
        List<String> tLines = EXEC.system_str("& \""+tVswhere+"\" -latest -products * -requires Microsoft.VisualStudio.Component.VC.Tools.x86.x64 -property installationPath");
        if (tLines.isEmpty()) return null;
        String tVsPath = tLines.get(0);
        if (tVsPath==null || !IO.exists(tVsPath)) return null;
        String tClPath = tVsPath + "\\VC\\Tools\\MSVC\\";
        if (!IO.exists(tClPath)) return null;
        String tVerName;
        try {
            String[] tList = IO.list(tClPath);
            if (tList.length==0) return null;
            tVerName = tList[0];
        } catch (IOException e) {
            return null;
        }
        tClPath = tClPath + tVerName + "\\bin\\Hostx64\\x64\\cl.exe";
        if (!IO.exists(tClPath)) return null;
        return tClPath;
    }
    
    static {
        InitHelper.INITIALIZED = true;
        
        EXE_PATH = getExePath_();
        if (EXE_PATH==null) {
            System.err.println(IO.Text.red("JNI INIT ERROR:")+" No suitable C/C++ compiler detected,");
            if (IS_WINDOWS) {
                System.err.println("  For Windows, you need MSVC: "+IO.Text.underline("https://visualstudio.microsoft.com/vs/features/cplusplus/"));
            } else {
                System.err.println("  For Liunx/Mac, you can use GCC: "+IO.Text.underline("https://gcc.gnu.org/"));
                System.err.println("  For Ubuntu, you can use `sudo apt install g++`");
            }
            if (Conf.FORCE) throw new RuntimeException("No suitable C/C++ compiler");
            EXE_CMD = null;
            TYPE = "unknown";
            VALID = false;
            C_COMPILER = null;
            CXX_COMPILER = null;
        } else {
            EXE_CMD = (IS_WINDOWS?"& \"":"\"") + EXE_PATH + "\"";
            if (IS_WINDOWS) {
                TYPE = "msvc";
                VALID = true;
                C_COMPILER = null;
                CXX_COMPILER = null;
            } else {
                if (EXE_PATH.endsWith("gcc")) {
                    TYPE = "gcc";
                    C_COMPILER = "gcc";
                    // 还需要额外检测 g++
                    EXEC.setNoSTDOutput().setNoERROutput();
                    boolean tHasGxx = EXEC.system("g++ --version") == 0;
                    EXEC.setNoSTDOutput(false).setNoERROutput(false);
                    if (tHasGxx) {
                        CXX_COMPILER = "g++";
                        VALID = true;
                    } else {
                        System.err.println(IO.Text.red("JNI INIT ERROR:")+" No g++ for gcc,");
                        System.err.println("  For Ubuntu, you can use `sudo apt install g++`");
                        if (Conf.FORCE) throw new RuntimeException("No suitable C/C++ compiler");
                        CXX_COMPILER = null;
                        VALID = false;
                    }
                } else {
                    TYPE = "clang";
                    VALID = true;
                    C_COMPILER = "clang";
                    CXX_COMPILER = "clang++";
                }
            }
        }
    }
}
