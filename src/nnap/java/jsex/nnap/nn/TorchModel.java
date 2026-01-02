package jsex.nnap.nn;

import jse.clib.JNIUtil;
import jse.clib.MiMalloc;
import jse.code.OS;
import jse.code.UT;
import jse.code.IO;
import jse.math.vector.DoubleArrayVector;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.Conf.VERSION_MASK;
import static jse.code.OS.JAR_DIR;
import static jse.code.OS.JAVA_HOME;

/**
 * 旧的 torch 实现的模型保留兼容，现在统一不做 batch，因此性能会十分受限
 * <p>
 * 由于内部会缓存中间结果，因此此类一般来说相同实例线程不安全，而不同实例之间线程安全
 * @author liqa
 */
@Deprecated
@SuppressWarnings("DeprecatedIsStillUsed")
public class TorchModel extends NeuralNetwork {
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
         * 自定义构建 nnap torch 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_NNAPTORCH");
        
        /**
         * 自定义构建 nnap torch 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_NNAPTORCH", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_NNAPTORCH"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 对于 nnap torch，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
         * 这对于 java 数组和 c 数组的转换很有效
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_NNAPTORCH", jse.code.Conf.USE_MIMALLOC);
    }
    
    public final static String LIB_DIR = JAR_DIR+"nnap/torch/" + UT.Code.uniqueID(OS.OS_NAME, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, jse.clib.Torch.HOME, Conf.USE_MIMALLOC, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jsex_nnap_nn_TorchModel.cpp"
        , "jsex_nnap_nn_TorchModel.h"
        , "jsex_nnap_nn_TorchPointer.h"
    };
    /** 记录所有已经初始化 torch 线程的开启的线程，因为 torch 设置线程数居然是线程独立的 */
    private final static Set<Long> INITIALIZED_THREAD = Collections.synchronizedSet(new HashSet<>());
    
    static {
        InitHelper.INITIALIZED = true;
        UT.Code.warning("TorchModel in nnap has been deprecated due to efficiency issues and is now only running in compatibility mode, which limits its speed. \n" +
                        "It is recommended to convert the existing potential file or retrain it.\n" +
                        "Note: you can convert old potential file via `jse -i jsex.nnap.TrainerTorch.convert path/to/old/nnpot.json path/to/new/nnpot.json`");
        // 依赖 torch
        jse.clib.Torch.InitHelper.init();
        
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("nnaptorch", "NNAP_TORCH", LIB_DIR, Conf.CMAKE_SETTING)
            .setSrc("nnap/torch", SRC_NAME)
            .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setUseMiMalloc(Conf.USE_MIMALLOC)
            .setCmakeLineOp(line -> {
                // 替换其中的 torch 库路径为设置好的路径
                line = line.replace("$ENV{JSE_TORCH_CMAKE_DIR}", jse.clib.Torch.CMAKE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
                return line;
            }).get();
        // 设置库路径
        System.load(IO.toAbsolutePath(LIB_PATH));
        
        // 这里需要 torch 单线程
        setSingleThread();
    }
    
    /** 设置 torch 为单线程，初始 NNAP 时会自动调用，但在不同于初始线程下调用时需要重新设置 */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static void setSingleThread() {
        long tThreadID = Thread.currentThread().getId();
        if (!INITIALIZED_THREAD.contains(tThreadID)) {
            INITIALIZED_THREAD.add(tThreadID);
            try {setSingleThread0();}
            catch (jse.clib.TorchException ignored) {/* 可能已经设置过，这里就不考虑 */}
        }
    }
    private static native void setSingleThread0() throws jse.clib.TorchException;
    
    
    private final TorchPointer mPtr;
    private final String mModelStr;
    private final int mInputDim;
    
    public TorchModel(int aInputDim, String aModel) throws jse.clib.TorchException {
        mInputDim = aInputDim;
        mModelStr = aModel;
        byte[] tModelBytes = Base64.getDecoder().decode(aModel);
        long tModelPtr = load1(tModelBytes, tModelBytes.length);
        if (tModelPtr==0 || tModelPtr==-1) {
            throw new jse.clib.TorchException("Failed to load Torch Model");
        }
        mPtr = new TorchPointer(this, tModelPtr);
    }
    @Override public TorchModel threadSafeRef() throws jse.clib.TorchException {
        return new TorchModel(mInputDim, mModelStr);
    }
    
    @Override protected void shutdown_() {
        mPtr.dispose();
    }
    
    @Override public int inputSize() {
        return mInputDim;
    }
    @Override public double eval(DoubleArrayVector aX) throws jse.clib.TorchException {
        if (isShutdown()) throw new IllegalStateException("This Model is dead");
        return forward0(mPtr.mPtr, aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(), mInputDim);
    }
    
    @Override public double evalGrad(DoubleArrayVector aX, DoubleArrayVector rGradX) throws jse.clib.TorchException {
        if (isShutdown()) throw new IllegalStateException("This Model is dead");
        return backward0(mPtr.mPtr, aX.internalDataWithLengthCheck(mInputDim), aX.internalDataShift(),
                         rGradX.internalDataWithLengthCheck(mInputDim), rGradX.internalDataShift(), mInputDim);
    }
    
    private static native long load0(String aModelPath) throws jse.clib.TorchException;
    private static native long load1(byte[] aModelBytes, int aSize) throws jse.clib.TorchException;
    
    private static native double forward0(long aModelPtr, double[] aX, int aStart, int aCount) throws jse.clib.TorchException;
    private static native double forward1(long aModelPtr, long aXPtr, int aCount) throws jse.clib.TorchException;
    private static native void batchForward0(long aModelPtr, double[] aX, int aStart, int aCount, double[] rY, int rYStart, int aBatchSize) throws jse.clib.TorchException;
    private static native void batchForward1(long aModelPtr, long aXPtr, int aCount, long rYPtr, int aBatchSize) throws jse.clib.TorchException;
    private static native double backward0(long aModelPtr, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws jse.clib.TorchException;
    private static native double backward1(long aModelPtr, long aXPtr, long rGradXPtr, int aCount) throws jse.clib.TorchException;
    private static native void batchBackward0(long aModelPtr, double[] aX, int aStart, double[] rGradX, int rStart, int aCount, double @Nullable[] rY, int rYStart, int aBatchSize) throws jse.clib.TorchException;
    private static native void batchBackward1(long aModelPtr, long aXPtr, long rGradXPtr, int aCount, long rYPtr, int aBatchSize) throws jse.clib.TorchException;
}
