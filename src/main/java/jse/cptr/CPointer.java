package jse.cptr;

import jse.clib.Compiler;
import jse.clib.JNIUtil;
import jse.clib.MiMalloc;
import jse.clib.UnsafeJNI;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.Conf.VERSION_MASK;
import static jse.code.OS.JAR_DIR;
import static jse.code.OS.JAVA_HOME;

/**
 * 直接访问使用 c 指针的类，不进行自动内存回收和各种检查从而保证最大的兼容性；
 * 此类因此是 {@code Unsafe} 的。
 * <p>
 * 内部默认会统一使用 {@link MiMalloc} 来加速内存分配和释放的过程。
 * @author liqa
 */
public class CPointer implements ICPointer {
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
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_CPOINTER");
        
        /**
         * 自定义构建 cpointer 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_C_COMPILER_CPOINTER} 来设置
         */
        public static @Nullable String CMAKE_C_COMPILER = OS.env("JSE_CMAKE_C_COMPILER_CPOINTER", jse.code.Conf.CMAKE_C_COMPILER);
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
        public static @Nullable String CMAKE_C_FLAGS = OS.env("JSE_CMAKE_C_FLAGS_CPOINTER", jse.code.Conf.CMAKE_C_FLAGS);
        /**
         * 自定义构建 cpointer 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         * <p>
         * 也可使用环境变量 {@code JSE_CMAKE_CXX_FLAGS_CPOINTER} 来设置
         */
        public static @Nullable String CMAKE_CXX_FLAGS = OS.env("JSE_CMAKE_CXX_FLAGS_CPOINTER", jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 对于 cpointer，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
         * 这对于 java 数组和 c 数组的转换很有效
         * <p>
         * 也可使用环境变量 {@code JSE_USE_MIMALLOC_CPOINTER} 来设置
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_CPOINTER", jse.code.Conf.USE_MIMALLOC);
    }
    
    /** 当前 {@link CPointer} JNI 库所在的文件夹路径，结尾一定存在 {@code '/'} */
    public final static String LIB_DIR = JAR_DIR+"cpointer/" + UT.Code.uniqueID(OS.OS_NAME, Compiler.EXE_PATH, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, Conf.USE_MIMALLOC, Conf.CMAKE_C_COMPILER, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    /** 当前 {@link CPointer} JNI 库的路径 */
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jse_cptr_CPointer.c"
        , "jse_cptr_CPointer.h"
        , "jse_cptr_IntCPointer.c"
        , "jse_cptr_IntCPointer.h"
        , "jse_cptr_Int64CPointer.c"
        , "jse_cptr_Int64CPointer.h"
        , "jse_cptr_DoubleCPointer.c"
        , "jse_cptr_DoubleCPointer.h"
        , "jse_cptr_FloatCPointer.c"
        , "jse_cptr_FloatCPointer.h"
        , "jse_cptr_AnyCPointer.c"
        , "jse_cptr_AnyCPointer.h"
        , "jse_cptr_NestedIntCPointer.c"
        , "jse_cptr_NestedIntCPointer.h"
        , "jse_cptr_NestedDoubleCPointer.c"
        , "jse_cptr_NestedDoubleCPointer.h"
    };
    public final static IPointer NULL = new IPointer() {
        @Override public long ptr_() {return 0;}
        @Override public boolean isNull() {return true;}
    };
    public final static IPointer nullptr = NULL;
    
    static {
        InitHelper.INITIALIZED = true;
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("cpointer", "CPOINTER", LIB_DIR, Conf.CMAKE_SETTING)
            .setSrc("cpointer", SRC_NAME)
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setUseMiMalloc(Conf.USE_MIMALLOC)
            .get();
        // 设置库路径
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    
    protected long mPtr;
    /**
     * 直接从一个任意的 c 指针初始化一个 {@link CPointer} 对象
     * @param aPtr 需要包装的 c 指针值
     */
    @ApiStatus.Internal public CPointer(long aPtr) {
        mPtr = aPtr;
    }
    /** @return {@inheritDoc} */
    @Override @ApiStatus.Internal public final long ptr_() {
        return mPtr;
    }
    /**
     * {@inheritDoc}
     * @param aPtr {@inheritDoc}
     */
    @Override public void setPtr_(long aPtr) {
        mPtr = aPtr;
    }
    
    /**
     * 调用 c 中的 {@code malloc} 来分配内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的内存大小
     * @return 创建的 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static CPointer malloc(long aCount) {
        return new CPointer(malloc0(aCount, TYPE_SIZE));
    }
    @ApiStatus.Internal
    public static long malloc0(long aCount, long aSize) {
        if (aCount<=0) throw new IllegalArgumentException("Input count must be positive");
        return malloc1(aCount, aSize);
    }
    private native static long malloc1(long aCount, long aSize);
    
    /**
     * 调用 c 中的 {@code calloc} 来分配全零内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的内存大小
     * @return 创建的 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static CPointer calloc(long aCount) {
        return new CPointer(calloc0(aCount, TYPE_SIZE));
    }
    @ApiStatus.Internal
    public static long calloc0(long aCount, long aSize) {
        if (aCount<=0) throw new IllegalArgumentException("Input count must be positive");
        return calloc1(aCount, aSize);
    }
    private native static long calloc1(long aCount, long aSize);
    
    /**
     * {@inheritDoc}
     * @return {@code sizeof(char)}, 1
     */
    @Override public long typeSize() {return TYPE_SIZE;}
    public final static long TYPE_SIZE = 1;
    
    /**
     * {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    @UnsafeJNI("Free wild pointer will directly result in JVM SIGSEGV")
    @Override public void free() {
        if (isNull()) throw new IllegalStateException("Cannot free a NULL pointer");
        free0(mPtr);
        mPtr = 0;
    }
    @ApiStatus.Internal
    public native static void free0(long aPtr);
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public CPointer copy() {
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
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void memcpy2dest(ICPointer rDest, long aCount) {
        if (isNull() || rDest.isNull()) throw new NullPointerException();
        memcpy0(mPtr, rDest.ptr_(), aCount);
    }
    /**
     * {@inheritDoc}
     * @param aSrc {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void memcpy2this(ICPointer aSrc, long aCount) {
        if (isNull() || aSrc.isNull()) throw new NullPointerException();
        memcpy0(aSrc.ptr_(), mPtr, aCount);
    }
    @ApiStatus.Internal
    public native static void memcpy0(long aSrc, long rDest, long aCount);
    
    @Override public void next() {throw new UnsupportedOperationException();}
    @Override public void rightShift(long aCount) {throw new UnsupportedOperationException();}
    @Override public CPointer plus(long aCount) {throw new UnsupportedOperationException();}
    @Override public void previous() {throw new UnsupportedOperationException();}
    @Override public void leftShift(long aCount) {throw new UnsupportedOperationException();}
    @Override public CPointer minus(long aCount) {throw new UnsupportedOperationException();}
    
    @ApiStatus.Internal
    public static void rangeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
