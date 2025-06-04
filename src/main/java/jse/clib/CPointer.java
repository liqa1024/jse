package jse.clib;

import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static jse.code.CS.VERSION;
import static jse.code.OS.JAR_DIR;

/**
 * 直接访问使用 c 指针的类，不进行自动内存回收和各种检查从而保证最大的兼容性；
 * 此类因此是 {@code Unsafe} 的。
 * <p>
 * 内部默认会统一使用 {@link MiMalloc} 来加速内存分配和释放的过程。
 * @author liqa
 */
public class CPointer {
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        /** @return {@link CPointer} 相关的 JNI 库是否已经初始化完成 */
        public static boolean initialized() {return INITIALIZED;}
        /** 初始化 {@link CPointer} 相关的 JNI 库 */
        @SuppressWarnings("ResultOfMethodCallIgnored")
        public static void init() {
            if (!INITIALIZED) String.valueOf(LIB_PATH);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义构建 cpointer 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new LinkedHashMap<>();
        
        /**
         * 自定义构建 cpointer 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_C_COMPILER_CPOINTER} 来设置
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_CPOINTER"  , jse.code.Conf.CMAKE_C_COMPILER);
        /**
         * 自定义构建 cpointer 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CXX_COMPILER_CPOINTER} 来设置
         */
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_CPOINTER", jse.code.Conf.CMAKE_CXX_COMPILER);
        /**
         * 自定义构建 cpointer 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_C_FLAGS_CPOINTER} 来设置
         */
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_CPOINTER"     , jse.code.Conf.CMAKE_C_FLAGS);
        /**
         * 自定义构建 cpointer 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CXX_FLAGS_CPOINTER} 来设置
         */
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_CPOINTER"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 对于 cpointer，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
         * 这对于 java 数组和 c 数组的转换很有效
         * <p>
         * 也可使用环境变量 {@code JSE_USE_MIMALLOC_CPOINTER} 来设置
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_CPOINTER", jse.code.Conf.USE_MIMALLOC);
        
        /**
         * 重定向 cpointer 动态库的路径，用于自定义编译这个库的过程，或者重新实现 cpointer 的接口
         * <p>
         * 也可使用环境变量 {@code JSE_REDIRECT_CPOINTER_LIB} 来设置
         */
        public static @Nullable String REDIRECT_CPOINTER_LIB = OS.env("JSE_REDIRECT_CPOINTER_LIB");
    }
    
    /** 当前 {@link CPointer} JNI 库所在的文件夹路径，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR = JAR_DIR+"cpointer/" + UT.Code.uniqueID(VERSION, Conf.USE_MIMALLOC, Conf.CMAKE_C_COMPILER, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    /** 当前 {@link CPointer} JNI 库的路径 */
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jse_clib_CPointer.c"
        , "jse_clib_CPointer.h"
        , "jse_clib_IntCPointer.c"
        , "jse_clib_IntCPointer.h"
        , "jse_clib_DoubleCPointer.c"
        , "jse_clib_DoubleCPointer.h"
        , "jse_clib_NestedCPointer.c"
        , "jse_clib_NestedCPointer.h"
        , "jse_clib_NestedIntCPointer.c"
        , "jse_clib_NestedIntCPointer.h"
        , "jse_clib_NestedDoubleCPointer.c"
        , "jse_clib_NestedDoubleCPointer.h"
    };
    
    static {
        InitHelper.INITIALIZED = true;
        // 依赖 jniutil
        JNIUtil.InitHelper.init();
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("cpointer", "CPOINTER", LIB_DIR, Conf.CMAKE_SETTING)
            .setSrc("cpointer", SRC_NAME)
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setUseMiMalloc(Conf.USE_MIMALLOC).setRedirectLibPath(Conf.REDIRECT_CPOINTER_LIB)
            .get();
        // 设置库路径
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    
    protected long mPtr;
    /**
     * 直接从一个任意的 c 指针初始化一个 {@link CPointer} 对象
     * @param aPtr 需要包装的 c 指针值
     */
    @ApiStatus.Internal public CPointer(long aPtr) {mPtr = aPtr;}
    /** @return 内部存储的 c 指针值 */
    @ApiStatus.Internal public final long ptr_() {return mPtr;}
    
    /** @return 内部存储的 c 指针是否是空的 */
    public boolean isNull() {return mPtr==0 || mPtr==-1;}
    
    /**
     * 调用 c 中的 {@code malloc} 来分配内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的内存大小
     * @return 创建的 c 指针对象
     */
    public static CPointer malloc(int aCount) {
        return new CPointer(malloc_(aCount, 1));
    }
    protected native static long malloc_(int aCount, int aSize);
    
    /**
     * 调用 c 中的 {@code calloc} 来分配全零内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的内存大小
     * @return 创建的 c 指针对象
     */
    public static CPointer calloc(int aCount) {
        return new CPointer(calloc_(aCount, 1));
    }
    protected native static long calloc_(int aCount, int aSize);
    
    /**
     * 调用 c 中的 {@code free} 来释放一个 c 指针对应的内存
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存释放的过程
     *
     * @throws IllegalStateException 如果此 c 指针是空指针
     */
    public void free() {
        if (isNull()) throw new IllegalStateException("Cannot free a NULL pointer");
        free_(mPtr);
        mPtr = 0;
    }
    protected native static void free_(long aPtr);
    
    /**
     * 拷贝一份 c 指针包装类，注意此方法不会实际拷贝内部
     * c 指针对应的内存，因此返回对象内部存储了相同的 c 指针
     * @return 拷贝的 c 指针包装类，包含相同的 c 指针
     */
    public CPointer copy() {
        return new CPointer(mPtr);
    }
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof CPointer)) return false;
        
        CPointer tCPointer = (CPointer)aRHS;
        return mPtr == tCPointer.mPtr;
    }
    @Override public final int hashCode() {
        return Long.hashCode(mPtr);
    }
    
    /**
     * 直接调用 c 中的 {@code memcpy} 来将此数组值拷贝到另一个 c 数组中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     * <p>
     * 为了避免歧义，特定类型的指针直接提供对应的 {@code fill} 方法，从而这里的
     * {@code aCount} 永远和 c 中的 {@code memcpy} 参数一致
     *
     * @param rDest 需要拷贝的目标 c 指针
     * @param aCount 需要拷贝的数据长度
     */
    public void memcpy(CPointer rDest, int aCount) {
        if (isNull() || rDest.isNull()) throw new NullPointerException();
        memcpy_(mPtr, rDest.mPtr, aCount);
    }
    protected native static void memcpy_(long aSrc, long rDest, int aCount);
    
    /**
     * 将此对象转换成一个整数的 c 指针 {@link IntCPointer}，类似在 c
     * 中使用 {@code (int *)ptr} 来进行强制类型转换
     * @return 整数的 c 指针包装类
     */
    public IntCPointer asIntCPointer() {return new IntCPointer(mPtr);}
    /**
     * 将此对象转换成一个双精度浮点的 c 指针 {@link DoubleCPointer}，类似在 c
     * 中使用 {@code (double *)ptr} 来进行强制类型转换
     * @return 双精度浮点的 c 指针包装类
     */
    public DoubleCPointer asDoubleCPointer() {return new DoubleCPointer(mPtr);}
    /**
     * 将此对象转换成一个嵌套指针的 c 指针 {@link NestedCPointer}，类似在 c
     * 中使用 {@code (void **)ptr} 来进行强制类型转换
     * @return 嵌套指针的 c 指针包装类
     */
    public NestedCPointer asNestedCPointer() {return new NestedCPointer(mPtr);}
    /**
     * 将此对象转换成一个嵌套整数指针的 c 指针 {@link NestedCPointer}，类似在 c
     * 中使用 {@code (int **)ptr} 来进行强制类型转换
     * @return 嵌套整数指针的 c 指针包装类
     */
    public NestedIntCPointer asNestedIntCPointer() {return new NestedIntCPointer(mPtr);}
    /**
     * 将此对象转换成一个嵌套双精度浮点指针的 c 指针 {@link DoubleCPointer}，类似在 c
     * 中使用 {@code (double **)ptr} 来进行强制类型转换
     * @return 嵌套双精度浮点指针的 c 指针包装类
     */
    public NestedDoubleCPointer asNestedDoubleCPointer() {return new NestedDoubleCPointer(mPtr);}
    
    static void rangeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
