package jsex.nnap.basis;

import jse.clib.JNIUtil;
import jse.code.CS;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jsex.nnap.NNAP;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

import static jse.code.OS.JAR_DIR;

/**
 * 基组工具类，主要用来初始化基组相关的 jni 库，以及一些通用的方法
 * @author liqa
 */
public final class BASIS {
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(LIB_PATH);
        }
    }
    
    public final static class Conf {
        /**
         * 自定义构建 native nnap basis 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new LinkedHashMap<>();
        
        public static final int NONE = -1;
        public static final int COMPAT = 0;
        public static final int BASE = 1;
        public static final int MAX = 2;
        /**
         * 自定义 native nnap basis 需要采用的优化等级，默认为 1（基础优化），
         * 会开启 AVX2 指令集，在大多数现代处理器上能兼容运行
         */
        public static int OPT_LEVEL = OS.envI("JSE_NNAPBASIS_OPT_LEVEL", BASE);
        
        /**
         * 自定义构建 native nnap basis 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_NNAPBASIS"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_NNAPBASIS"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_NNAPBASIS", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_NNAPBASIS"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /** 重定向 native nnap basis 动态库的路径 */
        public static @Nullable String REDIRECT_NNAPBASIS_LIB = OS.env("JSE_REDIRECT_NNAPBASIS_LIB");
    }
    
    public final static String LIB_DIR = JAR_DIR+"nnap/basis/" + UT.Code.uniqueID(CS.VERSION, NNAP.VERSION, Conf.OPT_LEVEL, Conf.CMAKE_C_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "basis_util.h"
        , "jsex_nnap_basis_BASIS.c"
        , "jsex_nnap_basis_BASIS.h"
        , "jsex_nnap_basis_SphericalChebyshev.c"
        , "jsex_nnap_basis_SphericalChebyshev.h"
        , "jsex_nnap_basis_Chebyshev.c"
        , "jsex_nnap_basis_Chebyshev.h"
    };
    
    static {
        InitHelper.INITIALIZED = true;
        // 不直接依赖 nnap
        
        // 先添加 Conf.CMAKE_SETTING，这样保证确定的优先级
        Map<String, String> rCmakeSetting = new LinkedHashMap<>(Conf.CMAKE_SETTING);
        switch(Conf.OPT_LEVEL) {
        case Conf.MAX: {
            rCmakeSetting.put("JSE_OPT_MAX",    "ON");
            rCmakeSetting.put("JSE_OPT_BASE",   "OFF");
            rCmakeSetting.put("JSE_OPT_COMPAT", "OFF");
            break;
        }
        case Conf.BASE: {
            rCmakeSetting.put("JSE_OPT_MAX",    "OFF");
            rCmakeSetting.put("JSE_OPT_BASE",   "ON");
            rCmakeSetting.put("JSE_OPT_COMPAT", "OFF");
            break;
        }
        case Conf.COMPAT: {
            rCmakeSetting.put("JSE_OPT_MAX",    "OFF");
            rCmakeSetting.put("JSE_OPT_BASE",   "OFF");
            rCmakeSetting.put("JSE_OPT_COMPAT", "ON");
            break;
        }
        case Conf.NONE: {
            rCmakeSetting.put("JSE_OPT_MAX",    "OFF");
            rCmakeSetting.put("JSE_OPT_BASE",   "OFF");
            rCmakeSetting.put("JSE_OPT_COMPAT", "OFF");
            break;
        }}
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("nnapbasis", "NATIVE_NNAP_BASIS", LIB_DIR, rCmakeSetting)
            .setSrc("nnap/basis", SRC_NAME)
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS)
            .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setRedirectLibPath(Conf.REDIRECT_NNAPBASIS_LIB)
            .get();
        // 设置库路径
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    @ApiStatus.Internal
    public static void forceDot0(double[] aXGrad, double[] aFpPx, double[] aFpPy, double[] aFpPz, int aShift, int aLength, double[] aFpPxCross, double[] aFpPyCross, double[] aFpPzCross, double[] rFx, double[] rFy, double[] rFz, int aNN) {
        sizeCheck(aXGrad.length, aShift+aLength);
        sizeCheck(aFpPx.length, aLength);
        sizeCheck(aFpPy.length, aLength);
        sizeCheck(aFpPz.length, aLength);
        sizeCheck(aFpPxCross.length, aLength*aNN);
        sizeCheck(aFpPyCross.length, aLength*aNN);
        sizeCheck(aFpPzCross.length, aLength*aNN);
        sizeCheck(rFx.length, aNN+1);
        sizeCheck(rFy.length, aNN+1);
        sizeCheck(rFz.length, aNN+1);
        forceDot1(aXGrad, aFpPx, aFpPy, aFpPz, aShift, aLength, aFpPxCross, aFpPyCross, aFpPzCross, rFx, rFy, rFz, aNN);
    }
    private static native void forceDot1(double[] aXGrad, double[] aFpPx, double[] aFpPy, double[] aFpPz, int aShift, int aLength, double[] aFpPxCross, double[] aFpPyCross, double[] aFpPzCross, double[] rFx, double[] rFy, double[] rFz, int aNN);
    
    static void sizeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
