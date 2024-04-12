package jse.code;

import jse.clib.MiMalloc;
import jse.code.collection.NewCollections;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

import static jse.code.OS.IS_MAC;
import static jse.code.OS.IS_WINDOWS;

/**
 * 全局的可以设置的常量值
 * @author liqa
 */
public class Conf {
    private Conf() {}
    
    /** 是否开启 debug 模式，一般这会有更加完整的错误信息（当然也会让阅读更加困难） */
    public static boolean DEBUG = OS.envZ("JSE_DEBUG", false);
    /** 运算是否开启边界检测，在 {@code jse 2.7.7} 以及更早的版本下不会直接检测 */
    public static boolean OPERATION_CHECK = OS.envZ("JSE_OPERATION_CHECK", true);
    
    /** 是否在 kernel 模式下开启 ThreadInterrupt，会在所有循环检测是否中断从而让中断总是有效，当然会影响性能 */
    public static boolean KERNEL_THREAD_INTERRUPT = OS.envZ("JSE_KERNEL_THREAD_INTERRUPT", true);
    /** 是否在 kernel 模式下显示 figure 窗口，关闭后则会在 jupyter notebook 上渲染 */
    public static boolean KERNEL_SHOW_FIGURE = OS.envZ("JSE_KERNEL_SHOW_FIGURE", false);
    
    /** 设置 jse 许多类工作的临时目录，一般来说使用 .jse/ 目录应该会更加合适，这里为了兼容并减少目录数还是默认保持使用 .temp；现在为了保证不带 / 也能使用，统一增加一个 toInternalValidDir */
    public static String TEMP_WORKING_DIR = UT.IO.toInternalValidDir(OS.env("JSE_TEMP_WORKING_DIR", ".temp/%n/"));
    public static String WORKING_DIR_OF(String aUniqueName) {return WORKING_DIR_OF(aUniqueName, false);}
    public static String WORKING_DIR_OF(String aUniqueName, boolean aRelative) {
        String tRelativeWD = TEMP_WORKING_DIR.replaceAll("%n", aUniqueName);
        if (aRelative) return tRelativeWD;
        return UT.IO.toInternalValidDir(UT.IO.toAbsolutePath(tRelativeWD));
    }
    
    /** {@link System#out} 和 {@link System#err} 是否支持复杂的 unicode 字符，禁用后可以避免乱码问题 */
    public static boolean UNICODE_SUPPORT = OS.envZ("JSE_UNICODE_SUPPORT", true);
    /** {@link UT.Timer#pbar} 默认情况下是否使用 {@link System#err} 流输出，这是一般的行为，在 {@code jse 2.7.4} 以及更早的版本下默认会使用 {@link System#out} 流输出 */
    public static boolean PBAR_ERR_STREAM = OS.envZ("JSE_PBAR_ERR_STREAM", true);
    
    /** 控制 parfor 的模式，在竞争模式下不同线程分配到的任务是不一定的，而关闭后是一定的，有时需要保证重复运行结果一致 */
    public static boolean PARFOR_NO_COMPETITIVE = OS.envZ("JSE_PARFOR_NO_COMPETITIVE", false);
    /** 设置 parfor 默认的线程数 */
    public static int PARFOR_THREAD_NUMBER = OS.envI("JSE_PARFOR_THREAD_NUMBER", Runtime.getRuntime().availableProcessors());
    
    /** 设置外置的 Groovy 库的路径，这里保留原始输入不进行预处理 */
    public static String @NotNull[] GROOVY_EXLIB_DIRS = OS.envPath("JSE_GROOVY_EXLIB_DIRS");
    /** 设置外置的 jar 库的路径，这里保留原始输入不进行预处理 */
    public static String @NotNull[] JAR_EXLIB_DIRS = OS.envPath("JSE_JAR_EXLIB_DIRS");
    /** 设置外置的 Python 库的路径，这里保留原始输入不进行预处理 */
    public static String @NotNull[] PYTHON_EXLIB_DIRS = OS.envPath("JSE_PYTHON_EXLIB_DIRS");
    
    /** 设置是否开启缓存，关闭后可以让内存管理全权交给 jvm，目前 jse 的内存效率和 jvm 基本类似 */
    public static boolean NO_CACHE = OS.envZ("JSE_NO_CACHE", false);
    
    /** 是否使用 {@link MiMalloc} 来加速 c 的内存分配，这对于 java 数组和 c 数组的转换很有效 */
    public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC", true);
    /** 设置 cmake 使用的 C/C++ 编译器以及 flag */
    public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER");
    public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER");
    public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS");
    public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS");
    /** 设置编译得到的 C/C++ 动态库的后缀，不同平台格式不同 */
    public static String LIB_EXTENSION = IS_WINDOWS ? ".dll" : (IS_MAC ? ".dylib" : ".so");
    /** 设置用于编译 C/C++ 时链接的库的后缀，不同平台格式不同（L: Link） */
    public static String LLIB_EXTENSION = IS_WINDOWS ? ".lib" : (IS_MAC ? ".dylib" : ".so");
    /** 根据后缀在和项目名称，在指定文件夹中找到合适的库名称，这种写法考虑到了 可选的 `lib` 开头 */
    public static @Nullable String LIB_NAME_IN(String aLibDir, String aProjectName) {
        try {
            for (String tName : UT.IO.list(aLibDir)) {
                // 固定后缀保证不会加载到其他平台的动态库
                if (tName.endsWith(LIB_EXTENSION) && tName.contains(aProjectName)) return tName;
            }
        } catch (IOException e) {
            // 失败时返回 null 而不抛出错误
            return null;
        }
        return null;
    }
    public static @Nullable String LLIB_NAME_IN(String aLibDir, String aProjectName) {
        try {
            for (String tName : UT.IO.list(aLibDir)) {
                // 固定后缀保证不会加载到其他平台的动态库
                if (tName.endsWith(LLIB_EXTENSION) && tName.contains(aProjectName)) return tName;
            }
        } catch (IOException e) {
            // 失败时返回 null 而不抛出错误
            return null;
        }
        return null;
    }
}
