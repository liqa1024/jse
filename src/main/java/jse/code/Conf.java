package jse.code;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;

import static jse.code.OS.IS_MAC;
import static jse.code.OS.IS_WINDOWS;

/**
 * 全局的可以设置的常量值，用于全局控制 jse 的行为
 * <p>
 * 可以通过相应的环境变量来设置，或者在脚本开头进行设置：
 * <pre> {@code
 * import jse.code.Conf
 *
 * Conf.DEBUG = true
 *
 * // The following code will run in debug mode, for example,
 * // exceptions will include the stack information of the jse kernel
 * } </pre>
 *
 * @author liqa
 */
public class Conf {
    private Conf() {}
    
    /**
     * 是否开启 debug 模式，一般这会有更加完整的错误信息（当然也会让阅读更加困难）
     * <p>
     * 默认为 {@code true}
     * <p>
     * 也可使用环境变量 {@code JSE_DEBUG} 来设置
     */
    public static boolean DEBUG = OS.envZ("JSE_DEBUG", false);
    /**
     * 是否开启 jdk 版本检测，主要提供一些兼容性的警告信息，关闭即可抑制警告
     * <p>
     * 默认为 {@code true}
     * <p>
     * 也可使用环境变量 {@code JDK_CHECK} 来设置
     */
    public static boolean JDK_CHECK = OS.envZ("JDK_CHECK", true);
    /**
     * 运算是否开启边界检测，在 {@code jse 2.7.7} 以及更早的版本下不会主动检测
     * <p>
     * 默认为 {@code true}
     * <p>
     * 也可使用环境变量 {@code JSE_OPERATION_CHECK} 来设置
     */
    public static boolean OPERATION_CHECK = OS.envZ("JSE_OPERATION_CHECK", true);
    /**
     * 部分运算是否开启 native 优化来尽可能提高速度，开启后需要有编译器环境保证实时编译，
     * 并且为了保证速度计算结果不再符合 ieee 标准（不同机器结果不完全一致）
     * <p>
     * 默认为 {@code false}
     * <p>
     * 也可使用环境变量 {@code JSE_NATIVE_OPERATION} 来设置
     */
    public static boolean NATIVE_OPERATION = OS.envZ("JSE_NATIVE_OPERATION", false);
    /**
     * 是否将 groovy 的脚本库也添加到 jep 的 {@code import hook} 中，在 {@code jse 3.2.2}
     * 之后不再默认包含，因此 python 中使用 groovy 包需要调用 {@link SP.Groovy#getClass(String)} 来导入
     * <p>
     * 默认为 {@code false}，这样在很多时候可以加速 jep 的初始化速度
     * <p>
     * 也可使用环境变量 {@code JSE_JEP_ADD_GROOVY_HOOK} 来设置
     * @see jep.ClassList
     */
    public static boolean JEP_ADD_GROOVY_HOOK = OS.envZ("JSE_JEP_ADD_GROOVY_HOOK", false);
    /**
     * 是否直接包含工作目录到类的搜索路径，在 {@code jse 3.0.0} 之后会默认包含，
     * 这样可以像一般 python 项目一样创建 jse 的项目。
     * <p>
     * 默认为 {@code true}，这在复杂目录下运行脚本可能因为这个操作而存在延迟，特别是在开启了 {@link #JEP_ADD_GROOVY_HOOK} 后
     * <p>
     * 也可使用环境变量 {@code JSE_INCLUDE_WORKING_DIR} 来设置
     */
    public static boolean INCLUDE_WORKING_DIR = OS.envZ("JSE_INCLUDE_WORKING_DIR", true);
    
    /**
     * 是否在 kernel 模式下（jupyter 运行 groovy 脚本时）开启
     * ThreadInterrupt，会在所有循环检测是否中断从而让中断操作总是有效。
     * <p>
     * 默认为 {@code true}，这会在一定程度影响在 jupyter 运行 groovy 脚本时的性能
     * <p>
     * 也可使用环境变量 {@code JSE_KERNEL_THREAD_INTERRUPT} 来设置
     */
    public static boolean KERNEL_THREAD_INTERRUPT = OS.envZ("JSE_KERNEL_THREAD_INTERRUPT", true);
    /**
     * 是否在 kernel 模式下（jupyter 运行时）绘图时弹出 figure 窗口，关闭后则会直接在 jupyter notebook
     * 上渲染，从而像一般的 jupyter 中运行 python 绘图一样的效果
     * <p>
     * 默认为 {@code false}
     * <p>
     * 也可使用环境变量 {@code JSE_KERNEL_SHOW_FIGURE} 来设置
     */
    public static boolean KERNEL_SHOW_FIGURE = OS.envZ("JSE_KERNEL_SHOW_FIGURE", false);
    
    /**
     * jse 的默认临时工作目录，现在统一为 {@code .temp/} 目录
     * <p>
     * 默认为 {@code ".temp/%n/"}，其中 {@code %n} 代表可以替换的具体子目录名称
     * <p>
     * 也可使用环境变量 {@code JSE_TEMP_WORKING_DIR} 来设置，可以不包含最后的斜杠 {@code "/"}
     */
    public static String TEMP_WORKING_DIR = IO.toInternalValidDir(OS.env("JSE_TEMP_WORKING_DIR", ".temp/%n/"));
    /**
     * 获取一个给定独立名称的临时工作目录，为了避免目录重复一般会调用
     * {@link UT.Code#randID()} 来创建一个随机的 id 来作为输入。
     * <p>
     * 此操作仅获取路径，不会实际创建文件夹，也不会检查文件夹是否存在或者为空。
     *
     * @param aUniqueName 希望的工作目录的特殊名称
     * @return 返回临时工作目录的路径，内部会调用 {@link IO#toInternalValidDir(String)} 确保得到的路径可以直接拼接文件名称
     */
    public static String WORKING_DIR_OF(String aUniqueName) {return WORKING_DIR_OF(aUniqueName, false);}
    /**
     * 获取一个给定独立名称的临时工作目录，为了避免目录重复一般会调用
     * {@link UT.Code#randID()} 来创建一个随机的 id 来作为输入。
     * <p>
     * 此操作仅获取路径，不会实际创建文件夹，也不会检查文件夹是否存在或者为空。
     *
     * @param aUniqueName 希望的工作目录的特殊名称
     * @param aRelative 是否需要一个相对路径，默认为 {@code false}，即总是会返回绝对路径
     * @return 返回临时工作目录的路径，内部会调用 {@link IO#toInternalValidDir(String)} 确保得到的路径可以直接拼接文件名称
     */
    public static String WORKING_DIR_OF(String aUniqueName, boolean aRelative) {
        String tRelativeWD = TEMP_WORKING_DIR.replaceAll("%n", aUniqueName);
        if (aRelative) return tRelativeWD;
        return IO.toInternalValidDir(IO.toAbsolutePath(tRelativeWD));
    }
    
    /**
     * {@link System#out} 和 {@link System#err} 是否支持复杂的 unicode 字符，禁用后可以避免乱码问题，这主要影响进度条的渲染行为；
     * jse 只会在使用特殊字符时检测此值查看兼容性，而不会去修改 {@link System#out} 和 {@link System#err} 的行为。
     * <p>
     * 默认为 {@code true}
     * <p>
     * 也可使用环境变量 {@code JSE_UNICODE_SUPPORT} 来设置
     */
    public static boolean UNICODE_SUPPORT = OS.envZ("JSE_UNICODE_SUPPORT", true);
    /**
     * {@link UT.Timer#pbar} 是否使用 {@link System#err} 流输出，在 {@code jse 2.7.4} 以及更早的版本下默认会使用 {@link System#out} 流输出；
     * 切换为 {@link System#out} 流输出后可以结合 {@link System#setOut(PrintStream)} 操作来将进度条输出也定向到相同的输出文件中。
     * <p>
     * 默认为 {@code true}
     * <p>
     * 也可使用环境变量 {@code JSE_PBAR_ERR_STREAM} 来设置
     * @see jse.code.io.RefreshableFilePrintStream
     */
    public static boolean PBAR_ERR_STREAM = OS.envZ("JSE_PBAR_ERR_STREAM", true);
    
    /**
     * 控制 {@link UT.Par#parfor} 以及 {@link jse.parallel.ParforThreadPool} 的模式，
     * 在竞争模式下不同线程完成任务后会竞争获取新的任务，从而每次运行下分配到的任务是不一致的，
     * 而关闭后则总是是一致的，有时需要关闭竞争从而保证重复运行结果一致。
     * <p>
     * 默认为 {@code false}
     * <p>
     * 也可使用环境变量 {@code JSE_PARFOR_NO_COMPETITIVE} 来设置
     */
    public static boolean PARFOR_NO_COMPETITIVE = OS.envZ("JSE_PARFOR_NO_COMPETITIVE", false);
    /**
     * 设置 {@link UT.Par#parfor} 默认的线程数
     * <p>
     * 默认为 {@code Runtime.getRuntime().availableProcessors()}
     * <p>
     * 也可使用环境变量 {@code JSE_PARFOR_THREAD_NUMBER} 来设置
     */
    public static int PARFOR_THREAD_NUMBER = OS.envI("JSE_PARFOR_THREAD_NUMBER", Runtime.getRuntime().availableProcessors());
    
    /**
     * 设置外置的 Groovy 库的路径，通过此值来添加自定义位置的 Groovy 库；
     * 默认 jse 会搜索 {@link SP#GROOVY_LIB_DIR} 位置的 Groovy 库。
     * <p>
     * 默认为空
     * <p>
     * 可使用环境变量 {@code JSE_GROOVY_EXLIB_DIRS} 来设置，通过 {@link File#pathSeparator}
     * 分割多个路径，这类似于 python 的 {@code PYTHONPATH} 环境变量
     * <p>
     * 由于加载顺序的原因，直接在脚本中修改此值不会生效，因为此时已经完成了外部库的读取初始化
     */
    public static String @NotNull[] GROOVY_EXLIB_DIRS = OS.envPath("JSE_GROOVY_EXLIB_DIRS");
    /**
     * 设置外置的 jar 库的路径，通过此值来添加自定义位置的 jar 库；
     * 默认 jse 会搜索 {@link SP#JAR_LIB_DIR} 位置的 jar 库。
     * <p>
     * 默认为空
     * <p>
     * 可使用环境变量 {@code JSE_JAR_EXLIB_DIRS} 来设置，通过 {@link File#pathSeparator}
     * 分割多个路径，这类似于 python 的 {@code PYTHONPATH} 环境变量
     * <p>
     * 由于加载顺序的原因，直接在脚本中修改此值不会生效，因为此时已经完成了外部库的读取初始化
     */
    public static String @NotNull[] JAR_EXLIB_DIRS = OS.envPath("JSE_JAR_EXLIB_DIRS");
    /**
     * 设置外置的 python 库的路径，通过此值来添加自定义位置的 python 库；
     * 默认 jse 会搜索 {@link SP#PYTHON_LIB_DIR} 位置的 python 库。
     * <p>
     * 默认为空
     * <p>
     * 可使用环境变量 {@code JSE_PYTHON_EXLIB_DIRS} 来设置，通过 {@link File#pathSeparator}
     * 分割多个路径，这类似于 python 的 {@code PYTHONPATH} 环境变量
     * <p>
     * 由于加载顺序的原因，直接在脚本中修改此值不会生效，因为此时已经完成了外部库的读取初始化
     */
    public static String @NotNull[] PYTHON_EXLIB_DIRS = OS.envPath("JSE_PYTHON_EXLIB_DIRS");
    
    /**
     * 是否关闭缓存操作，为了提高在 jdk8 等老版本下的性能，jse 会对高频的数组内存分配开启缓存操作；
     * 关闭后可能可以提高内存的利用率，减少内存泄漏风险，在较新版本的 jdk 下基本不会有性能损失
     * <p>
     * 默认为 {@code false}
     * <p>
     * 也可使用环境变量 {@code JSE_NO_CACHE} 来设置
     */
    public static boolean NO_CACHE = OS.envZ("JSE_NO_CACHE", false);
    
    /**
     * 用户自定义的版本掩码，用于对于完全不同的环境进行设置来达到独立的 jni 依赖。
     * <p>
     * 默认为 {@code 0}
     * <p>
     * 也可使用环境变量 {@code JSE_VERSION_MASK} 来设置
     */
    public static int VERSION_MASK = OS.envI("JSE_VERSION_MASK", 0);
    /**
     * 是否使用 {@link jse.clib.MiMalloc} 来加速 c 的内存分配，这对于 java 数组和 c 数组的转换很有效；
     * 这只控制全局的默认行为，如果子项的 JNI 库有设置 {@code USE_MIMALLOC} 相关值将会覆盖此设置。
     * <p>
     * 默认为 {@code true}
     * <p>
     * 也可使用环境变量 {@code JSE_USE_MIMALLOC} 来设置
     */
    public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC", true);
    /**
     * 设置全局 cmake 默认使用的 C/C++ 编译器以及 flag；
     * 这只控制全局的默认行为，如果子项的 JNI 库有设置 {@code CMAKE_C_COMPILER} 相关值将会覆盖此设置。
     * <p>
     * 也可使用环境变量 {@code JSE_CMAKE_C_COMPILER} 来设置
     */
    public static @Nullable String CMAKE_C_COMPILER = OS.env("JSE_CMAKE_C_COMPILER");
    /**
     * 设置全局 cmake 默认使用的 C/C++ 编译器以及 flag；
     * 这只控制全局的默认行为，如果子项的 JNI 库有设置 {@code CMAKE_CXX_COMPILER} 相关值将会覆盖此设置。
     * <p>
     * 也可使用环境变量 {@code JSE_CMAKE_CXX_COMPILER} 来设置
     */
    public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER");
    /**
     * 设置全局 cmake 默认使用的 C/C++ 编译器以及 flag；
     * 这只控制全局的默认行为，如果子项的 JNI 库有设置 {@code CMAKE_C_FLAGS} 相关值将会覆盖此设置。
     * <p>
     * 也可使用环境变量 {@code JSE_CMAKE_C_FLAGS} 来设置
     */
    public static @Nullable String CMAKE_C_FLAGS = OS.env("JSE_CMAKE_C_FLAGS");
    /**
     * 设置全局 cmake 默认使用的 C/C++ 编译器以及 flag；
     * 这只控制全局的默认行为，如果子项的 JNI 库有设置 {@code CMAKE_CXX_FLAGS} 相关值将会覆盖此设置。
     * <p>
     * 也可使用环境变量 {@code JSE_CMAKE_CXX_FLAGS} 来设置
     */
    public static @Nullable String CMAKE_CXX_FLAGS = OS.env("JSE_CMAKE_CXX_FLAGS");
    /**
     * 设置编译得到的 C/C++ 动态库的后缀，不同平台格式不同
     * <p>
     * 默认在 windows 下为 {@code .dll}，macos 下为 {@code .dylib}，linux 下为 {@code .so}
     */
    public static String LIB_EXTENSION = IS_WINDOWS ? ".dll" : (IS_MAC ? ".dylib" : ".so");
    /**
     * 设置编译得到的 C/C++ 时链接的库的后缀，不同平台格式不同（L: Link）
     * <p>
     * 默认在 windows 下为 {@code .lib}，macos 下为 {@code .dylib}，linux 下为 {@code .so}
     */
    public static String LLIB_EXTENSION = IS_WINDOWS ? ".lib" : (IS_MAC ? ".dylib" : ".so");
    /**
     * 根据后缀在和项目名称，在指定文件夹中找到合适的库名称，这种写法考虑到了可能存在的 `lib` 开头
     * @param aLibDir 需要寻找库的文件夹
     * @param aProjectName 库的工程名称，去除了可能存在的 `lib` 开头
     * @return 查找到的库路径，如果没有找到则返回 {@code null}
     */
    public static @Nullable String LIB_NAME_IN(String aLibDir, String aProjectName) {
        try {
            for (String tName : IO.list(aLibDir)) {
                // 固定后缀保证不会加载到其他平台的动态库
                if (tName.endsWith(LIB_EXTENSION) && tName.contains(aProjectName)) return tName;
            }
        } catch (IOException e) {
            // 失败时返回 null 而不抛出错误
            return null;
        }
        return null;
    }
    /**
     * 根据后缀在和项目名称，在指定文件夹中找到合适的链接的库名称，这种写法考虑到了可能存在的 `lib` 开头（L: Link）
     * @param aLibDir 需要寻找库的文件夹
     * @param aProjectName 库的工程名称，去除了可能存在的 `lib` 开头
     * @return 查找到的库路径，如果没有找到则返回 {@code null}
     */
    public static @Nullable String LLIB_NAME_IN(String aLibDir, String aProjectName) {
        try {
            for (String tName : IO.list(aLibDir)) {
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
