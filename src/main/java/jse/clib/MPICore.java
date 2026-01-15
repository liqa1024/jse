package jse.clib;

import jse.code.IO;
import org.jetbrains.annotations.Nullable;

import static jse.code.OS.EXEC;
import static jse.code.OS.IS_WINDOWS;

/**
 * 部分库需要的 MPI 支持，注意区分 {@link jse.parallel.MPI}
 * 这里只进行系统库的检测和警告，不负责编译 jni
 * @author liqa
 */
public class MPICore {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link MPICore} 相关的参数是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link MPICore} 相关的参数 */
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(EXE_PATH);
        }
    }
    
    /** 自动检测到的 mpiexec 可执行路径 */
    public final static String EXE_PATH;
    /** 拼接后可以执行的命令，对于 windows 和 linux 专门适配 */
    public final static String EXE_CMD;
    /** 是否有自动检测到 mpiexec */
    public final static boolean VALID;
    
    
    private static boolean FIRST_PRINT = true;
    /** 由于总是需要路径来确定库路径，提供一个延迟打印接口，仅第一次构建时打印提示；由于是可选的，下载相关的提示也延迟输出 */
    public static void printInfo() {
        if (!FIRST_PRINT) return;
        FIRST_PRINT = false;
        if (EXE_PATH!=null) {
            System.out.printf("JNI INIT INFO: Use MPI in %s\n", EXE_PATH);
        } else {
            System.err.println(IO.Text.yellow("JNI INIT WARNING:")+" No MPI found,");
            if (IS_WINDOWS) {
                System.err.println("  For Windows, you can use MS-MPI: "+IO.Text.underline("https://www.microsoft.com/en-us/download/details.aspx?id=105289"));
                System.err.println("  BOTH 'msmpisetup.exe' and 'msmpisdk.msi' are needed.");
            } else {
                System.err.println("  For Liunx/Mac, you can use OpenMPI: "+IO.Text.underline("https://www.open-mpi.org/"));
                System.err.println("  For Ubuntu, you can use `sudo apt install libopenmpi-dev`");
            }
        }
    }
    
    private static @Nullable String getExePath_() {
        // 检测环境变量的 mpiexec
        EXEC.setNoSTDOutput().setNoERROutput();
        boolean tNoMpi = EXEC.system("mpiexec --version") != 0;
        if (tNoMpi) {
            tNoMpi = EXEC.system("mpiexec -?") != 0;
        }
        EXEC.setNoSTDOutput(false).setNoERROutput(false);
        if (tNoMpi) return null;
        String tMpiPath;
        if (IS_WINDOWS) {
            tMpiPath = EXEC.system_str("(Get-Command mpiexec).Path").get(0);
        } else {
            tMpiPath = EXEC.system_str("which mpiexec").get(0);
        }
        return IO.exists(tMpiPath) ? tMpiPath : null;
    }
    
    static {
        InitHelper.INITIALIZED = true;
        try {EXE_PATH = getExePath_();}
        catch (Exception e) {throw new RuntimeException(e);}
        if (EXE_PATH!=null) {
            EXE_CMD = (IS_WINDOWS?"& \"":"\"") + EXE_PATH + "\"";
            VALID = true;
        } else {
            EXE_CMD = null;
            VALID = false;
        }
    }
}
