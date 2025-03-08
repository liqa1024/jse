package jsex.nnap;

import com.google.common.collect.Lists;
import jep.JepException;
import jep.NDArray;
import jep.python.PyObject;
import jse.ase.AseAtoms;
import jse.atom.*;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.clib.*;
import jse.code.*;
import jse.code.collection.AbstractCollections;
import jse.code.collection.ISlice;
import jse.code.collection.IntList;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import jsex.nnap.basis.IBasis;
import jsex.nnap.basis.Mirror;
import jsex.nnap.basis.SphericalChebyshev;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntUnaryOperator;

import static jse.code.OS.JAR_DIR;

/**
 * jse 实现的 nnap 计算器，所有
 * nnap 相关能量和力的计算都在此实现
 * <p>
 * 考虑到 Torch 本身的内存安全性，此类设计时确保不同对象之间线程安全，
 * 而不同线程访问相同的对象线程不安全
 * <p>
 * 由于需要并行来绕开 GIL，并且考虑到效率问题，这里需要使用原生的 pytorch
 * <p>
 * 现在这个类会自动回收内部的 torch 模型指针，因此不需要担心内存泄漏的问题了；
 * 当然即使如此依旧建议手动调用 {@link #shutdown()} 来及时释放资源
 * @author liqa
 */
public class NNAP implements IAutoShutdown {
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
         * 自定义构建 nnap 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = new LinkedHashMap<>();
        
        /**
         * 自定义构建 nnap 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_NNAP", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_NNAP"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /**
         * 对于 nnap，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
         * 这对于 java 数组和 c 数组的转换很有效
         */
        public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_NNAP", jse.code.Conf.USE_MIMALLOC);
        
        /** 重定向 nnap 动态库的路径 */
        public static @Nullable String REDIRECT_NNAP_LIB = OS.env("JSE_REDIRECT_NNAP_LIB");
    }
    
    public final static int VERSION = 2;
    public final static String LIB_DIR = JAR_DIR+"nnap/" + UT.Code.uniqueID(CS.VERSION, VERSION, Torch.HOME, Conf.USE_MIMALLOC, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jsex_nnap_NNAP.cpp"
        , "jsex_nnap_NNAP.h"
        , "jsex_nnap_NNAPModelPointers.h"
    };
    
    static {
        InitHelper.INITIALIZED = true;
        // 依赖 torch
        Torch.InitHelper.init();
        // 依赖 jniutil
        JNIUtil.InitHelper.init();
        // 这里不直接依赖 LmpPlugin
        
        // 现在直接使用 JNIUtil.buildLib 来统一初始化
        LIB_PATH = new JNIUtil.LibBuilder("nnap", "NNAP", LIB_DIR, Conf.CMAKE_SETTING)
            .setSrc("nnap", SRC_NAME)
            .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setUseMiMalloc(Conf.USE_MIMALLOC).setRedirectLibPath(Conf.REDIRECT_NNAP_LIB)
            .setCmakeLineOpt(line -> {
                // 替换其中的 torch 库路径为设置好的路径
                line = line.replace("$ENV{JSE_TORCH_CMAKE_DIR}", Torch.CMAKE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
                return line;
            }).get();
        // 设置库路径
        System.load(IO.toAbsolutePath(LIB_PATH));
        
        // 这里需要 torch 单线程
        try {setSingleThread0();}
        catch (TorchException ignored) {/* 可能已经设置过，这里就不考虑 */}
    }
    private static native void setSingleThread0() throws TorchException;
    
    /** 中间缓存批次数量，一次传入多个批次进入神经网络可以大大提高效率 */
    public final static int BATCH_SIZE = 64;
    
    @SuppressWarnings("unchecked")
    private @Nullable SingleNNAP initSingleNNAPFrom(int aType, Map<String, ?> aModelInfo) throws TorchException {
        Map<String, ?> tBasis = (Map<String, ?>)aModelInfo.get("basis");
        if (tBasis == null) {
            tBasis = Maps.of("type", "spherical_chebyshev");
        }
        Object tBasisType = tBasis.get("type");
        if (tBasisType == null) {
            tBasisType = "spherical_chebyshev";
        }
        if (tBasisType.equals("mirror")) return null; // mirror 情况延迟初始化
        if (!tBasisType.equals("spherical_chebyshev")) throw new IllegalArgumentException("Unsupported basis type: " + tBasisType);
        IBasis[] aBasis = new IBasis[mThreadNumber];
        for (int i = 0; i < mThreadNumber; ++i) {
            //noinspection resource
            aBasis[i] = SphericalChebyshev.load(mSymbols, tBasis);
        }
        
        Number tRefEng = (Number)aModelInfo.get("ref_eng");
        if (tRefEng == null) throw new IllegalArgumentException("No ref_eng in ModelInfo");
        double aRefEng = tRefEng.doubleValue();
        List<? extends Number> tNormSigma = (List<? extends Number>)UT.Code.get(aModelInfo, "norm_sigma", "norm_vec");
        if (tNormSigma == null) throw new IllegalArgumentException("No norm_sigma/norm_vec in ModelInfo");
        IVector aNormSigma = Vectors.from(tNormSigma);
        List<? extends Number> tNormMu = (List<? extends Number>)aModelInfo.get("norm_mu");
        IVector aNormMu = tNormMu==null ? Vectors.zeros(tNormSigma.size()) : Vectors.from(tNormMu);
        Number tNormSigmaEng = (Number)aModelInfo.get("norm_sigma_eng");
        double aNormSigmaEng = tNormSigmaEng==null ? 1.0 : tNormSigmaEng.doubleValue();
        Number tNormMuEng = (Number)aModelInfo.get("norm_mu_eng");
        double aNormMuEng = tNormMuEng==null ? 0.0 : tNormMuEng.doubleValue();
        
        Object tModel = aModelInfo.get("torch");
        if (tModel == null) throw new IllegalArgumentException("No torch data in ModelInfo");
        String aModel = tModel.toString();
        return new SingleNNAP(aRefEng, aNormMu, aNormSigma, aNormMuEng, aNormSigmaEng, aBasis, aModel);
    }
    @SuppressWarnings("unchecked")
    private @Nullable SingleNNAP postInitSingleNNAPFrom(int aType, Map<String, ?> aModelInfo) throws TorchException {
        Map<String, ?> tBasis = (Map<String, ?>)aModelInfo.get("basis");
        if (tBasis == null) return null;
        Object tBasisType = tBasis.get("type");
        // 目前只考虑 mirror 的情况
        if (!tBasisType.equals("mirror")) return null;
        Object tMirror = tBasis.get("mirror");
        if (tMirror == null) throw new IllegalArgumentException("Key `mirror` required for basis mirror");
        int tMirrorType = ((Number)tMirror).intValue();
        IBasis[] aBasis = new IBasis[mThreadNumber];
        for (int i = 0; i < mThreadNumber; ++i) {
            aBasis[i] = new Mirror(model(tMirrorType).basis(i), tMirrorType, aType);
        }
        // mirror 会强制这些额外值缺省
        Number tRefEng = (Number)aModelInfo.get("ref_eng");
        if (tRefEng != null) throw new IllegalArgumentException("ref_eng in mirror ModelInfo MUST be empty");
        double aRefEng = model(tMirrorType).refEng();
        
        List<? extends Number> tNormVec = (List<? extends Number>)aModelInfo.get("norm_vec");
        if (tNormVec != null) throw new IllegalArgumentException("norm_vec in mirror ModelInfo MUST be empty");
        IVector aNormMu = model(tMirrorType).normMu();
        IVector aNormSigma = model(tMirrorType).normSigma();
        double aNormMuEng = model(tMirrorType).normMuEng();
        double aNormSigmaEng = model(tMirrorType).normSigmaEng();
        
        Object tModel = aModelInfo.get("torch");
        if (tModel != null) throw new IllegalArgumentException("torch data in mirror ModelInfo MUST be empty");
        String aModel = model(tMirrorType).mModel;
        
        return new SingleNNAP(aRefEng, aNormMu, aNormSigma, aNormMuEng, aNormSigmaEng, aBasis, aModel);
    }
    
    @SuppressWarnings("SameParameterValue")
    public class SingleNNAP {
        private final NNAPModelPointers mModelPtrs;
        private final String mModel;
        private final double mRefEng;
        private final IVector mNormMu, mNormSigma;
        private final double mNormMuEng, mNormSigmaEng;
        private final IBasis[] mBasis;
        private final int mBasisSize;
        
        public double refEng() {return mRefEng;}
        public IVector normMu() {return mNormMu;}
        public IVector normSigma() {return mNormSigma;}
        public double normMuEng() {return mNormMuEng;}
        public double normSigmaEng() {return mNormSigmaEng;}
        public IBasis basis() {return basis(0);}
        public IBasis basis(int aThreadID) {return mBasis[aThreadID];}
        
        public void normBasis(IVector rFp) {
            rFp.minus2this(mNormMu);
            rFp.div2this(mNormSigma);
        }
        public void normBasisPartial(IVector rFp) {
            rFp.div2this(mNormSigma);
        }
        public double denormEng(double aEng) {
            return aEng*mNormSigmaEng + mNormMuEng;
        }
        public void denormEngPartial(IVector rEngPartial) {
            rEngPartial.multiply2this(mNormSigmaEng);
        }
        
        @SuppressWarnings("unchecked")
        private SingleNNAP(double aRefEng, IVector aNormMu, IVector aNormSigma, double aNormMuEng, double aNormSigmaEng, IBasis[] aBasis, String aModel) throws TorchException {
            mRefEng = aRefEng;
            mNormMu = aNormMu;
            mNormSigma = aNormSigma;
            mNormMuEng = aNormMuEng;
            mNormSigmaEng = aNormSigmaEng;
            mBasis = aBasis;
            mBasisSize = mBasis[0].size();
            mModel = aModel;
            byte[] tModelBytes = Base64.getDecoder().decode(aModel);
            long[] rModelPtrs = new long[mThreadNumber];
            for (int i = 0; i < mThreadNumber; ++i) {
                long tModelPtr = load1(tModelBytes, tModelBytes.length);
                if (tModelPtr==0 || tModelPtr==-1) {
                    for (int j = 0; j < i; ++j) {
                        NNAPModelPointers.dispose0(rModelPtrs[j]);
                    }
                    throw new TorchException("Failed to load Torch Model");
                }
                rModelPtrs[i] = tModelPtr;
            }
            mModelPtrs = new NNAPModelPointers(this, rModelPtrs);
            mBatchedX = MatrixCache.getMatRow(BATCH_SIZE, mBasisSize, mThreadNumber);
            mBatchedXGrad = MatrixCache.getMatRow(BATCH_SIZE, mBasisSize, mThreadNumber);
            mBatchedY = VectorCache.getVec(BATCH_SIZE, mThreadNumber);
            mBatchedPredDo = new DoubleConsumer[mThreadNumber][BATCH_SIZE];
            mBatchedXGradDo = (Consumer<? super IVector>[][]) new Consumer[mThreadNumber][BATCH_SIZE];
            mBatchedSize = new int[mThreadNumber];
        }
        
        private final List<RowMatrix> mBatchedX;
        private final List<RowMatrix> mBatchedXGrad;
        private final List<Vector> mBatchedY;
        private final DoubleConsumer[][] mBatchedPredDo;
        private final Consumer<? super IVector>[][] mBatchedXGradDo;
        private final int[] mBatchedSize;
        /** 提交批量 forward 接口，注意由于内部存储共享，因此不能和 {@link #submitBatchBackward} 混用，在使用 backward 前务必使用 {@link #clearSubmittedBatchForward} 清空缓存 */
        public void submitBatchForward(int aThreadID, IVector aX, DoubleConsumer aPredDo) throws TorchException {
            DoubleConsumer[] tBatchedPredDo = mBatchedPredDo[aThreadID];
            RowMatrix tBatchedX = mBatchedX.get(aThreadID);
            int tBatchedSize = mBatchedSize[aThreadID];
            tBatchedPredDo[tBatchedSize] = aPredDo;
            tBatchedX.row(tBatchedSize).fill(aX);
            ++tBatchedSize;
            if (tBatchedSize == BATCH_SIZE) {
                tBatchedSize = 0;
                Vector tBatchedY = mBatchedY.get(aThreadID);
                batchForward(aThreadID, tBatchedX.internalData(), tBatchedX.internalDataShift(), mBasisSize,
                             tBatchedY.internalData(), tBatchedY.internalDataShift(), BATCH_SIZE);
                for (int i = 0; i < BATCH_SIZE; ++i) {
                    tBatchedPredDo[i].accept(tBatchedY.get(i));
                }
            }
            mBatchedSize[aThreadID] = tBatchedSize;
        }
        public void clearSubmittedBatchForward(int aThreadID) throws TorchException {
            int tBatchedSize = mBatchedSize[aThreadID];
            if (tBatchedSize == 0) return;
            DoubleConsumer[] tBatchedPredDo = mBatchedPredDo[aThreadID];
            RowMatrix tBatchedX = mBatchedX.get(aThreadID);
            Vector tBatchedY = mBatchedY.get(aThreadID);
            batchForward(aThreadID, tBatchedX.internalData(), tBatchedX.internalDataShift(), mBasisSize,
                         tBatchedY.internalData(), tBatchedY.internalDataShift(), tBatchedSize);
            for (int i = 0; i < tBatchedSize; ++i) {
                tBatchedPredDo[i].accept(tBatchedY.get(i));
            }
            mBatchedSize[aThreadID] = 0;
        }
        public void submitBatchForward(IVector aX, DoubleConsumer aPredDo) throws TorchException {submitBatchForward(0, aX, aPredDo);}
        public void clearSubmittedBatchForward() throws TorchException {clearSubmittedBatchForward(0);}
        
        /** 提交批量 forward 接口，注意由于内部存储共享，因此不能和 {@link #submitBatchForward} 混用，在使用 forward 前务必使用 {@link #clearSubmittedBatchBackward} 清空缓存 */
        public void submitBatchBackward(int aThreadID, IVector aX, @Nullable DoubleConsumer aPredDo, Consumer<? super ShiftVector> aXGradDo) throws TorchException {
            @Nullable DoubleConsumer[] tBatchedPredDo = mBatchedPredDo[aThreadID];
            Consumer<? super ShiftVector>[] tBatchedXGradDo = mBatchedXGradDo[aThreadID];
            RowMatrix tBatchedX = mBatchedX.get(aThreadID);
            int tBatchedSize = mBatchedSize[aThreadID];
            tBatchedPredDo[tBatchedSize] = aPredDo;
            tBatchedXGradDo[tBatchedSize] = aXGradDo;
            tBatchedX.row(tBatchedSize).fill(aX);
            ++tBatchedSize;
            if (tBatchedSize == BATCH_SIZE) {
                tBatchedSize = 0;
                Vector tBatchedY = mBatchedY.get(aThreadID);
                RowMatrix tBatchedXGrad = mBatchedXGrad.get(aThreadID);
                batchBackward(aThreadID, tBatchedX.internalData(), tBatchedX.internalDataShift(), tBatchedXGrad.internalData(), tBatchedXGrad.internalDataShift(), mBasisSize,
                              tBatchedY.internalData(), tBatchedY.internalDataShift(), BATCH_SIZE);
                for (int i = 0; i < BATCH_SIZE; ++i) {
                    @Nullable DoubleConsumer tPredDo = tBatchedPredDo[i];
                    if (tPredDo != null) tPredDo.accept(tBatchedY.get(i));
                    tBatchedXGradDo[i].accept(tBatchedXGrad.row(i));
                }
            }
            mBatchedSize[aThreadID] = tBatchedSize;
        }
        public void clearSubmittedBatchBackward(int aThreadID) throws TorchException {
            int tBatchedSize = mBatchedSize[aThreadID];
            if (tBatchedSize == 0) return;
            @Nullable DoubleConsumer[] tBatchedPredDo = mBatchedPredDo[aThreadID];
            Consumer<? super IVector>[] tBatchedXGradDo = mBatchedXGradDo[aThreadID];
            RowMatrix tBatchedX = mBatchedX.get(aThreadID);
            Vector tBatchedY = mBatchedY.get(aThreadID);
            RowMatrix tBatchedXGrad = mBatchedXGrad.get(aThreadID);
            batchBackward(aThreadID, tBatchedX.internalData(), tBatchedX.internalDataShift(), tBatchedXGrad.internalData(), tBatchedXGrad.internalDataShift(), mBasisSize,
                          tBatchedY.internalData(), tBatchedY.internalDataShift(), tBatchedSize);
            for (int i = 0; i < tBatchedSize; ++i) {
                @Nullable DoubleConsumer tPredDo = tBatchedPredDo[i];
                if (tPredDo != null) tPredDo.accept(tBatchedY.get(i));
                tBatchedXGradDo[i].accept(tBatchedXGrad.row(i));
            }
            mBatchedSize[aThreadID] = 0;
        }
        public void submitBatchBackward(IVector aX, @Nullable DoubleConsumer aPredDo, Consumer<? super ShiftVector> aXGradDo) throws TorchException {submitBatchBackward(0, aX, aPredDo, aXGradDo);}
        public void clearSubmittedBatchBackward() throws TorchException {clearSubmittedBatchBackward(0);}
        
        
        public double forward(int aThreadID, double[] aX, int aStart, int aCount) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount);
            return forward0(mModelPtrs.mPtrs[aThreadID], aX, aStart, aCount);
        }
        public double forward(double[] aX, int aStart, int aCount) throws TorchException {return forward(0, aX, aStart, aCount);}
        public double forward(double[] aX, int aCount) throws TorchException {return forward(aX, 0, aCount);}
        public double forward(double[] aX) throws TorchException {return forward(aX, aX.length);}
        public double forward(int aThreadID, DoubleCPointer aX, int aCount) throws TorchException {return forward1(mModelPtrs.mPtrs[aThreadID], aX.ptr_(), aCount);}
        public double forward(DoubleCPointer aX, int aCount) throws TorchException {return forward(0, aX, aCount);}
        
        public void batchForward(int aThreadID, double[] aX, int aStart, int aCount, double[] rY, int rYStart, int aBatchSize) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount);
            rangeCheck(rY.length, rYStart+aBatchSize);
            batchForward0(mModelPtrs.mPtrs[aThreadID], aX, aStart, aCount, rY, rYStart, aBatchSize);
        }
        public void batchForward(double[] aX, int aStart, int aCount, double[] rY, int rYStart, int aBatchSize) throws TorchException {batchForward(0, aX, aStart, aCount, rY, rYStart, aBatchSize);}
        public void batchForward(double[] aX, int aCount, double[] rY, int aBatchSize) throws TorchException {batchForward(aX, 0, aCount, rY, 0, aBatchSize);}
        public void batchForward(double[] aX, double[] rY) throws TorchException {batchForward(aX, aX.length/rY.length, rY, rY.length);}
        public void batchForward(int aThreadID, DoubleCPointer aX, int aCount, DoubleCPointer rY, int aBatchSize) throws TorchException {batchForward1(mModelPtrs.mPtrs[aThreadID], aX.ptr_(), aCount, rY.ptr_(), aBatchSize);}
        public void batchForward(DoubleCPointer aX, int aCount, DoubleCPointer rY, int aBatchSize) throws TorchException {batchForward(0, aX, aCount, rY, aBatchSize);}
        
        public double backward(int aThreadID, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount);
            rangeCheck(rGradX.length, rStart+aCount);
            return backward0(mModelPtrs.mPtrs[aThreadID], aX, aStart, rGradX, rStart, aCount);
        }
        public double backward(double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {return backward(0, aX, aStart, rGradX, rStart, aCount);}
        public double backward(double[] aX, double[] rGradX, int aCount) throws TorchException {return backward(aX, 0, rGradX, 0, aCount);}
        public double backward(double[] aX, double[] rGradX) throws TorchException {return backward(aX, rGradX, aX.length);}
        public double backward(int aThreadID, DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward1(mModelPtrs.mPtrs[aThreadID], aX.ptr_(), rGradX.ptr_(), aCount);}
        public double backward(DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward(0, aX, rGradX, aCount);}
        
        public void batchBackward(int aThreadID, double[] aX, int aStart, double[] rGradX, int rStart, int aCount, double @Nullable[] rY, int rYStart, int aBatchSize) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount*aBatchSize);
            rangeCheck(rGradX.length, rStart+aCount*aBatchSize);
            if (rY != null) rangeCheck(rY.length, rYStart+aBatchSize);
            batchBackward0(mModelPtrs.mPtrs[aThreadID], aX, aStart, rGradX, rStart, aCount, rY, rYStart, aBatchSize);
        }
        public void batchBackward(double[] aX, int aStart, double[] rGradX, int rStart, int aCount, double @Nullable[] rY, int rYStart, int aBatchSize) throws TorchException {batchBackward(0, aX, aStart, rGradX, rStart, aCount, rY, rYStart, aBatchSize);}
        public void batchBackward(double[] aX, double[] rGradX, int aCount, double @Nullable[] rY, int aBatchSize) throws TorchException {batchBackward(aX, 0, rGradX, 0, aCount, rY, 0, aBatchSize);}
        public void batchBackward(double[] aX, double[] rGradX, double[] rY) throws TorchException {batchBackward(aX, rGradX, aX.length/rY.length, rY, rY.length);}
        public void batchBackward(int aThreadID, DoubleCPointer aX, DoubleCPointer rGradX, int aCount, @Nullable DoubleCPointer rY, int aBatchSize) throws TorchException {batchBackward1(mModelPtrs.mPtrs[aThreadID], aX.ptr_(), rGradX.ptr_(), aCount, rY==null ? 0 : rY.ptr_(), aBatchSize);}
        public void batchBackward(DoubleCPointer aX, DoubleCPointer rGradX, int aCount, @Nullable DoubleCPointer rY, int aBatchSize) throws TorchException {batchBackward(0, aX, rGradX, aCount, rY, aBatchSize);}
        
    }
    private final List<SingleNNAP> mModels;
    private final String[] mSymbols;
    private final @Nullable String mUnits;
    private boolean mDead = false;
    private final int mThreadNumber;
    public int atomTypeNumber() {return mSymbols.length;}
    public SingleNNAP model(int aType) {return mModels.get(aType-1);}
    public @Unmodifiable List<SingleNNAP> models() {return mModels;}
    public String symbol(int aType) {return mSymbols[aType-1];}
    public @Unmodifiable List<String> symbols() {return AbstractCollections.from(mSymbols);}
    public String units() {return mUnits;}
    
    @SuppressWarnings("unchecked")
    public NNAP(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws TorchException {
        mThreadNumber = aThreadNumber;
        Number tVersion = (Number)aModelInfo.get("version");
        if (tVersion != null) {
            int tVersionValue = tVersion.intValue();
            if (tVersionValue > VERSION) throw new IllegalArgumentException("Unsupported version: " + tVersionValue);
        }
        mUnits = UT.Code.toString(aModelInfo.get("units"));
        List<? extends Map<String, ?>> tModelInfos = (List<? extends Map<String, ?>>)aModelInfo.get("models");
        if (tModelInfos == null) throw new IllegalArgumentException("No models in ModelInfo");
        int tModelSize = tModelInfos.size();
        mSymbols = new String[tModelSize];
        for (int i = 0; i < tModelSize; ++i) {
            Object tSymbol = tModelInfos.get(i).get("symbol");
            if (tSymbol == null) throw new IllegalArgumentException("No symbol in ModelInfo");
            mSymbols[i] = tSymbol.toString();
        }
        mModels = new ArrayList<>(tModelSize);
        for (int i = 0; i < tModelSize; ++i) {
            mModels.add(initSingleNNAPFrom(i+1, tModelInfos.get(i)));
        }
        for (int i = 0; i < tModelSize; ++i) {
            SingleNNAP tModel = postInitSingleNNAPFrom(i+1, tModelInfos.get(i));
            if (tModel != null) mModels.set(i, tModel);
        }
        for (int i = 0; i < tModelSize; ++i) {
            if (mModels.get(i) == null) throw new IllegalArgumentException("Model init fail for type "+(i+1));
        }
    }
    public NNAP(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws TorchException, IOException {
        this(aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath), aThreadNumber);
    }
    public NNAP(Map<?, ?> aModelInfo) throws TorchException {this(aModelInfo, 1);}
    public NNAP(String aModelPath) throws TorchException, IOException {this(aModelPath, 1);}
    private static native long load0(String aModelPath) throws TorchException;
    private static native long load1(byte[] aModelBytes, int aSize) throws TorchException;
    
    @Override public void shutdown() {
        if (!mDead) {
            mDead = true;
            for (SingleNNAP tModel : mModels) {
                MatrixCache.returnMat(tModel.mBatchedX);
                MatrixCache.returnMat(tModel.mBatchedXGrad);
                VectorCache.returnVec(tModel.mBatchedY);
                for (int i = 0; i < mThreadNumber; ++i) {
                    tModel.basis(i).shutdown();
                }
                for (int i = 0; i < mThreadNumber; ++i) {
                    tModel.mModelPtrs.dispose();
                }
            }
        }
    }
    public boolean isShutdown() {return mDead;}
    public int threadNumber() {return mThreadNumber;}
    @VisibleForTesting public int nthreads() {return threadNumber();}
    
    public IntUnaryOperator typeMap(IAtomData aAtomData) {return IAtomData.typeMap_(symbols(), aAtomData);}
    public boolean sameOrder(Collection<? extends CharSequence> aSymbolsIn) {return IAtomData.sameSymbolOrder_(symbols(), aSymbolsIn);}
    public int typeOf(String aSymbol) {return IAtomData.typeOf_(symbols(), aSymbol);}
    
    /**
     * 转换为一个 ase 的计算器，可以方便接入已有的代码直接计算；
     * 注意这里计算的压力统一按照 ase 的排序，也就是
     * {@code [xx, yy, zz, yz, xz, xy]}
     * <p>
     * 创建的 ase 计算器和原本的 NNAP 为引用关系，因此两者只要关闭了其中一个，另一个也会同时关闭；
     * 为了方便管理，这里统一采用手动关闭，因此也不实现 {@code __del__} 方法
     * @return ase 计算器的 python 对象
     */
    public PyObject asAseCalculator() throws JepException {return asAseCalculator(SP.Python.interpreter());}
    public PyObject asAseCalculator(@NotNull jep.Interpreter aInterpreter) throws JepException {
        // 先定义这个计算器类，这里不考虑性能损失
        //noinspection ConcatenationWithEmptyString
        aInterpreter.exec("" +
        "from ase.calculators.calculator import Calculator, all_changes, PropertyNotImplementedError\n" +
        "class __NNAP_AseCalculator__(Calculator):\n" +
        "    implemented_properties = ['energy', 'free_energy', 'energies', 'forces', 'stress', 'stresses']\n" +
        "    \n" +
        "    def __init__(self, jnnap, **kwargs):\n" +
        "        super().__init__(**kwargs)\n" +
        "        self.jnnap = jnnap\n" +
        "    \n" +
        "    def release(self):\n" +
        "        self.jnnap.shutdown()\n" +
        "    def shutdown(self):\n" +
        "        self.jnnap.shutdown()\n" +
        "    \n" +
        "    def calculate(self, atoms=None, properties=['energy'], system_changes=all_changes):\n" +
        "        super().calculate(atoms, properties, system_changes)\n" +
        "        for p in properties:\n" +
        "            if p not in self.implemented_properties:\n" +
        "                raise PropertyNotImplementedError(p)\n" +
        "        self.results = self.jnnap.calculate_(self.results, self.atoms, properties, len(system_changes)>0)"
        );
        return (PyObject)aInterpreter.invoke("__NNAP_AseCalculator__", this);
    }
    @ApiStatus.Internal
    public Map<String, Object> calculate_(Map<String, Object> rResults, PyObject aPyAseAtoms, String[] aProperties, boolean aSystemChanges) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        boolean tAllInResults = true;
        for (String tProperty : aProperties) {
            if (!rResults.containsKey(tProperty)) {
                tAllInResults = false;
                break;
            }
        }
        if (!aSystemChanges && tAllInResults) return rResults;
        IAtomData tAtoms = AseAtoms.of(aPyAseAtoms);
        // 遍历统计需要的量
        boolean tRequireEnergy = false;
        boolean tRequireForces = false;
        boolean tRequireStress = false;
        for (String tProperty : aProperties) {
            if (tProperty.equals("energy") || tProperty.equals("energies") || tProperty.equals("free_energy")) tRequireEnergy = true;
            if (tProperty.equals("forces")) tRequireForces = true;
            if (tProperty.equals("stress") || tProperty.equals("stresses")) tRequireStress = true;
        }
        // 只需要能量则直接使用简单的计算能量接口
        if (!tRequireForces && !tRequireStress) {
            assert tRequireEnergy;
            Vector tEnergies = calEnergies(tAtoms);
            double tEnergy = tEnergies.sum();
            rResults.put("energy", tEnergy);
            rResults.put("energies", new NDArray<>(tEnergies.internalData(), tEnergies.size()));
            rResults.put("free_energy", tEnergy);
            VectorCache.returnVec(tEnergies);
            return rResults;
        }
        // 其余情况则统一全部计算
        Vector rEnergies = VectorCache.getZeros(tAtoms.atomNumber());
        RowMatrix rForces = MatrixCache.getZerosRow(tAtoms.atomNumber(), 3);
        Vector rStress = VectorCache.getZeros(6);
        RowMatrix rStresses = MatrixCache.getZerosRow(tAtoms.atomNumber(), 6);
        calEnergyForceVirials(tAtoms, rEnergies, rForces.col(0), rForces.col(1), rForces.col(2),
                              rStresses.col(0), rStresses.col(1), rStresses.col(2), rStresses.col(5), rStresses.col(4), rStresses.col(3));
        rStresses.operation().negative2this();
        for (int i = 0; i < 6; ++i) {
            rStress.set(i, rStresses.col(i).sum());
        }
        rStress.div2this(tAtoms.volume());
        double tEnergy = rEnergies.sum();
        rResults.put("energy", tEnergy);
        rResults.put("energies", new NDArray<>(rEnergies.internalData(), rEnergies.size()));
        rResults.put("free_energy", tEnergy);
        rResults.put("forces", new NDArray<>(rForces.internalData(), rForces.rowNumber(), rForces.columnNumber()));
        rResults.put("stress", new NDArray<>(rStress.internalData(), rStress.size()));
        rResults.put("stresses", new NDArray<>(rStresses.internalData(), rStresses.rowNumber(), rStresses.columnNumber()));
        VectorCache.returnVec(rEnergies);
        MatrixCache.returnMat(rForces);
        VectorCache.returnVec(rStress);
        MatrixCache.returnMat(rStresses);
        return rResults;
    }
    
    /**
     * 使用 nnap 计算给定原子结构每个原子的能量值
     * @param aAPC 此原子的 apc 或者原子结构本身
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 每个原子能量组成的向量
     * @author liqa
     */
    public Vector calEnergies(final AtomicParameterCalculator aAPC, final IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 统一存储常量
        final int tAtomNumber = aAPC.atomNumber();
        Vector rEngs = VectorCache.getVec(tAtomNumber);
        try {
            aAPC.pool_().parforWithException(tAtomNumber, null, threadID -> {
                for (SingleNNAP tModel : mModels) {
                    tModel.clearSubmittedBatchForward(threadID);
                }
            }, (i, threadID) -> {
                final SingleNNAP tModel = model(aTypeMap.applyAsInt(aAPC.atomType_().get(i)));
                final IBasis tBasis = tModel.basis(threadID);
                Vector tBasisValue = tBasis.eval(aAPC, i, aTypeMap);
                tModel.normBasis(tBasisValue);
                tModel.submitBatchForward(threadID, tBasisValue, pred -> {
                    pred = tModel.denormEng(pred);
                    pred += tModel.mRefEng;
                    rEngs.set(i, pred);
                });
                VectorCache.returnVec(tBasisValue);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rEngs;
    }
    public Vector calEnergies(AtomicParameterCalculator aAPC) throws TorchException {return calEnergies(aAPC, type->type);}
    public Vector calEnergies(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergies(tAPC, tTypeMap);}
    }
    /**
     * 使用 nnap 计算给定原子结构指定原子的能量
     * @param aAPC 此原子的 apc 或者原子结构本身
     * @param aIndices 需要计算的原子的索引（从 0 开始）
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 此原子的能量组成的向量
     * @author liqa
     */
    public Vector calEnergiesAt(final AtomicParameterCalculator aAPC, final ISlice aIndices, final IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 统一存储常量
        final int tSize = aIndices.size();
        Vector rEngs = VectorCache.getVec(tSize);
        try {
            aAPC.pool_().parforWithException(tSize, null, threadID -> {
                for (SingleNNAP tModel : mModels) {
                    tModel.clearSubmittedBatchForward(threadID);
                }
            }, (i, threadID) -> {
                final int cIdx = aIndices.get(i);
                final SingleNNAP tModel = model(aTypeMap.applyAsInt(aAPC.atomType_().get(cIdx)));
                final IBasis tBasis = tModel.basis(threadID);
                Vector tBasisValue = tBasis.eval(aAPC, cIdx, aTypeMap);
                tModel.normBasis(tBasisValue);
                tModel.submitBatchForward(threadID, tBasisValue, pred -> {
                    pred = tModel.denormEng(pred);
                    pred += tModel.mRefEng;
                    rEngs.set(i, pred);
                });
                VectorCache.returnVec(tBasisValue);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rEngs;
    }
    public Vector calEnergiesAt(AtomicParameterCalculator aAPC, ISlice aIndices) throws TorchException {return calEnergiesAt(aAPC, aIndices, type->type);}
    public Vector calEnergiesAt(IAtomData aAtomData, ISlice aIndices) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergiesAt(tAPC, aIndices, tTypeMap);}
    }
    /**
     * 使用 nnap 计算给定原子结构的总能量
     * @param aAPC 此原子的 apc 或者原子结构本身
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 总能量
     * @author liqa
     */
    public double calEnergy(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        Vector tEngs = calEnergies(aAPC, aTypeMap);
        double tTotEng = tEngs.sum();
        VectorCache.returnVec(tEngs);
        return tTotEng;
    }
    public double calEnergy(AtomicParameterCalculator aAPC) throws TorchException {return calEnergy(aAPC, type->type);}
    public double calEnergy(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergy(tAPC, tTypeMap);}
    }
    
    /**
     * 计算移动前后的能量差，只计算移动影响的近邻列表中的原子，可以加快计算速度
     * @param aAPC 此原子的 apc 或者原子结构本身
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @param aRestoreAPC 计算完成后是否还原 APC 的状态，默认为 {@code true}
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 移动后能量 - 移动前能量
     */
    public double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, double aDx, double aDy, double aDz, boolean aRestoreAPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 采用最大的截断半径从而包含所有可能涉及发生了能量变换的原子
        double tRCut = 0.0;
        for (SingleNNAP tModel : models()) {
            tRCut = Math.max(tRCut, tModel.basis().rcut());
        }
        XYZ oXYZ = new XYZ(aAPC.atomDataXYZ_().row(aI));
        IIntVector oNL = aAPC.getNeighborList(oXYZ, tRCut);
        XYZ nXYZ = oXYZ.plus(aDx, aDy, aDz);
        IIntVector nNL = aAPC.getNeighborList(nXYZ, tRCut);
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(oNL.size());
        tNL.addAll(oNL);
        nNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        Vector oEngs = calEnergiesAt(aAPC, tNL, aTypeMap);
        double oEng = oEngs.sum();
        VectorCache.returnVec(oEngs);
        Vector nEngs = calEnergiesAt(aAPC.setAtomXYZ(aI, nXYZ), tNL, aTypeMap);
        double nEng = nEngs.sum();
        VectorCache.returnVec(nEngs);
        if (aRestoreAPC) aAPC.setAtomXYZ(aI, oXYZ);
        return nEng - oEng;
    }
    public double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, double aDx, double aDy, double aDz, IntUnaryOperator aTypeMap) throws TorchException {return calEnergyDiffMove(aAPC, aI, aDx, aDy, aDz, true, aTypeMap);}
    public double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, double aDx, double aDy, double aDz, boolean aRestoreAPC) throws TorchException {return calEnergyDiffMove(aAPC, aI, aDx, aDy, aDz, aRestoreAPC, type->type);}
    public double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, double aDx, double aDy, double aDz) throws TorchException {return calEnergyDiffMove(aAPC, aI, aDx, aDy, aDz, type->type);}
    public double calEnergyDiffMove(IAtomData aAtomData, int aI, double aDx, double aDy, double aDz) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyDiffMove(tAPC, aI, aDx, aDy, aDz, false, tTypeMap);}
    }
    public double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, IXYZ aDxyz, IntUnaryOperator aTypeMap) throws TorchException {return calEnergyDiffMove(aAPC, aI, aDxyz.x(), aDxyz.y(), aDxyz.z(), aTypeMap);}
    public double calEnergyDiffMove(AtomicParameterCalculator aAPC, int aI, IXYZ aDxyz) throws TorchException {return calEnergyDiffMove(aAPC, aI, aDxyz, type->type);}
    public double calEnergyDiffMove(IAtomData aAtomData, int aI, IXYZ aDxyz) throws TorchException {return calEnergyDiffMove(aAtomData, aI, aDxyz.x(), aDxyz.y(), aDxyz.z());}
    /**
     * 计算交换种类前后的能量差，只计算交换影响的近邻列表中的原子，可以加快计算速度
     * @param aAPC 此原子的 apc 或者原子结构本身
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @param aRestoreAPC 计算完成后是否还原 APC 的状态，默认为 {@code true}
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 交换后能量 - 交换前能量
     */
    public double calEnergyDiffSwap(AtomicParameterCalculator aAPC, int aI, int aJ, boolean aRestoreAPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        int oTypeI = aAPC.atomType_().get(aI);
        int oTypeJ = aAPC.atomType_().get(aJ);
        if (oTypeI == oTypeJ) return 0.0;
        // 采用最大的截断半径从而包含所有可能涉及发生了能量变换的原子
        double tRCut = 0.0;
        for (SingleNNAP tModel : models()) {
            tRCut = Math.max(tRCut, tModel.basis().rcut());
        }
        IIntVector iNL = aAPC.getNeighborList(aI, tRCut);
        IIntVector jNL = aAPC.getNeighborList(aJ, tRCut);
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(iNL.size()+1);
        tNL.add(aI);
        tNL.addAll(iNL);
        if (!tNL.contains(aJ)) tNL.add(aJ);
        jNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        Vector oEngs = calEnergiesAt(aAPC, tNL, aTypeMap);
        double oEng = oEngs.sum();
        VectorCache.returnVec(oEngs);
        Vector nEngs = calEnergiesAt(aAPC.setAtomType(aI, oTypeJ).setAtomType(aJ, oTypeI), tNL, aTypeMap);
        double nEng = nEngs.sum();
        VectorCache.returnVec(nEngs);
        if (aRestoreAPC) aAPC.setAtomType(aI, oTypeI).setAtomType(aJ, oTypeJ);
        return nEng - oEng;
    }
    public double calEnergyDiffSwap(AtomicParameterCalculator aAPC, int aI, int aJ, IntUnaryOperator aTypeMap) throws TorchException {return calEnergyDiffSwap(aAPC, aI, aJ, true, aTypeMap);}
    public double calEnergyDiffSwap(AtomicParameterCalculator aAPC, int aI, int aJ, boolean aRestoreAPC) throws TorchException {return calEnergyDiffSwap(aAPC, aI, aJ, aRestoreAPC, type->type);}
    public double calEnergyDiffSwap(AtomicParameterCalculator aAPC, int aI, int aJ) throws TorchException {return calEnergyDiffSwap(aAPC, aI, aJ, type->type);}
    public double calEnergyDiffSwap(IAtomData aAtomData, int aI, int aJ) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyDiffSwap(tAPC, aI, aJ, false, tTypeMap);}
    }
    /**
     * 计算翻转摸个元素种类前后的能量差，只计算翻转影响的近邻列表中的原子，可以加快计算速度
     * @param aAPC 此原子的 apc 或者原子结构本身
     * @param aI 需要翻转种类的原子索引
     * @param aType 此原子需要翻转的种类编号，对应输入原子数据原始的种类编号，没有经过 aTypeMap（如果有的话）
     * @param aRestoreAPC 计算完成后是否还原 APC 的状态，默认为 {@code true}
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 翻转后能量 - 翻转前能量
     */
    public double calEnergyDiffFlip(AtomicParameterCalculator aAPC, int aI, int aType, boolean aRestoreAPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        int oType = aAPC.atomType_().get(aI);
        if (oType == aType) return 0.0;
        // 采用最大的截断半径从而包含所有可能涉及发生了能量变换的原子
        double tRCut = 0.0;
        for (SingleNNAP tModel : models()) {
            tRCut = Math.max(tRCut, tModel.basis().rcut());
        }
        IIntVector iNL = aAPC.getNeighborList(aI, tRCut);
        // 增加一个自身，这里简单创建新的列表实现
        final IntList tNL = new IntList(iNL.size()+1);
        tNL.add(aI);
        tNL.addAll(iNL);
        Vector oEngs = calEnergiesAt(aAPC, tNL, aTypeMap);
        double oEng = oEngs.sum();
        VectorCache.returnVec(oEngs);
        Vector nEngs = calEnergiesAt(aAPC.setAtomType(aI, aType), tNL, aTypeMap);
        double nEng = nEngs.sum();
        VectorCache.returnVec(nEngs);
        if (aRestoreAPC) aAPC.setAtomType(aI, oType);
        return nEng - oEng;
    }
    public double calEnergyDiffFlip(AtomicParameterCalculator aAPC, int aI, int aType, IntUnaryOperator aTypeMap) throws TorchException {return calEnergyDiffFlip(aAPC, aI, aType, true, aTypeMap);}
    public double calEnergyDiffFlip(AtomicParameterCalculator aAPC, int aI, int aType, boolean aRestoreAPC) throws TorchException {return calEnergyDiffFlip(aAPC, aI, aType, aRestoreAPC, type->type);}
    public double calEnergyDiffFlip(AtomicParameterCalculator aAPC, int aI, int aType) throws TorchException {return calEnergyDiffFlip(aAPC, aI, aType, type->type);}
    public double calEnergyDiffFlip(IAtomData aAtomData, int aI, int aType) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyDiffFlip(tAPC, aI, aType, false, tTypeMap);}
    }
    
    
    
    /**
     * 使用 nnap 计算给定原子结构每个原子和受力
     * @param aAPC 此原子的 apc 或者原子结构本身
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 每个原子力组成的矩阵，按行排列
     * @author liqa
     */
    public RowMatrix calForces(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        RowMatrix rForces = MatrixCache.getMatRow(aAPC.atomNumber(), 3);
        calEnergyForceVirials(aAPC, null, rForces.col(0), rForces.col(1), rForces.col(2), null, null, null, null, null, null, aTypeMap);
        return rForces;
    }
    public RowMatrix calForces(AtomicParameterCalculator aAPC) throws TorchException {return calForces(aAPC, type->type);}
    public RowMatrix calForces(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calForces(tAPC, tTypeMap);}
    }
    
    /**
     * 使用 nnap 计算给定结构所有原子的单独应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAtomData 原子结构本身或者 APC，使用 APC 时统一不考虑速度部分
     * @param aIdealGas 是否考虑理想气体部分（速度效应部分），默认为 {@code false}
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力向量
     * @author liqa
     */
    public List<Vector> calStresses(IAtomData aAtomData, boolean aIdealGas) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        final int tAtomNum = aAtomData.atomNumber();
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {
            List<Vector> tStresses = calStresses(tAPC, tTypeMap);
            if (!aIdealGas || !aAtomData.hasMass() || !aAtomData.hasVelocity()) return tStresses;
            // 累加速度项，这里需要消去整体的平动
            double vxTot = 0.0, vyTot = 0.0, vzTot = 0.0;
            for (int i = 0; i < tAtomNum; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                vxTot += tAtom.vx(); vyTot += tAtom.vy(); vzTot += tAtom.vz();
            }
            vxTot /= (double)tAtomNum;
            vyTot /= (double)tAtomNum;
            vzTot /= (double)tAtomNum;
            for (int i = 0; i < tAtomNum; ++i) {
                IAtom tAtom = aAtomData.atom(i);
                if (!tAtom.hasMass()) continue;
                double vx = tAtom.vx() - vxTot, vy = tAtom.vy() - vyTot, vz = tAtom.vz() - vzTot;
                double tMass = tAtom.mass();
                tStresses.get(0).add(i, -tMass * vx*vx);
                tStresses.get(1).add(i, -tMass * vy*vy);
                tStresses.get(2).add(i, -tMass * vz*vz);
                tStresses.get(3).add(i, -tMass * vx*vy);
                tStresses.get(4).add(i, -tMass * vx*vz);
                tStresses.get(5).add(i, -tMass * vy*vz);
            }
            return tStresses;
        }
    }
    public List<Vector> calStresses(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        final int tAtomNum = aAPC.atomNumber();
        List<Vector> rStresses = VectorCache.getVec(tAtomNum, 6);
        calEnergyForceVirials(aAPC, null, null, null, null, rStresses.get(0), rStresses.get(1), rStresses.get(2), rStresses.get(3), rStresses.get(4), rStresses.get(5), aTypeMap);
        for (int i = 0; i < 6; ++i) {
            rStresses.get(i).operation().negative2this();
        }
        return rStresses;
    }
    public List<Vector> calStresses(AtomicParameterCalculator aAPC) throws TorchException {return calStresses(aAPC, type->type);}
    /**
     * 使用 nnap 计算给定原子结构的应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAtomData 原子结构本身或者 APC，使用 APC 时统一不考虑速度部分
     * @param aIdealGas 是否考虑理想气体部分（速度效应部分），默认为 {@code false}
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力值
     * @author liqa
     */
    public List<Double> calStress(IAtomData aAtomData, boolean aIdealGas) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        List<Vector> tStresses = calStresses(aAtomData, aIdealGas);
        double tStressXX = tStresses.get(0).sum();
        double tStressYY = tStresses.get(1).sum();
        double tStressZZ = tStresses.get(2).sum();
        double tStressXY = tStresses.get(3).sum();
        double tStressXZ = tStresses.get(4).sum();
        double tStressYZ = tStresses.get(5).sum();
        VectorCache.returnVec(tStresses);
        double tVolume = aAtomData.volume();
        tStressXX /= tVolume;
        tStressYY /= tVolume;
        tStressZZ /= tVolume;
        tStressXY /= tVolume;
        tStressXZ /= tVolume;
        tStressYZ /= tVolume;
        return Lists.newArrayList(tStressXX, tStressYY, tStressZZ, tStressXY, tStressXZ, tStressYZ);
    }
    public List<Double> calStress(IAtomData aAtomData) throws TorchException {return calStress(aAtomData, false);}
    public List<Double> calStress(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        List<Vector> tStresses = calStresses(aAPC, aTypeMap);
        double tStressXX = tStresses.get(0).sum();
        double tStressYY = tStresses.get(1).sum();
        double tStressZZ = tStresses.get(2).sum();
        double tStressXY = tStresses.get(3).sum();
        double tStressXZ = tStresses.get(4).sum();
        double tStressYZ = tStresses.get(5).sum();
        VectorCache.returnVec(tStresses);
        double tVolume = aAPC.volume();
        tStressXX /= tVolume;
        tStressYY /= tVolume;
        tStressZZ /= tVolume;
        tStressXY /= tVolume;
        tStressXZ /= tVolume;
        tStressYZ /= tVolume;
        return Lists.newArrayList(tStressXX, tStressYY, tStressZZ, tStressXY, tStressXZ, tStressYZ);
    }
    public List<Double> calStress(AtomicParameterCalculator aAPC) throws TorchException {return calStress(aAPC, type->type);}
    
    /**
     * 使用 nnap 同时计算给定原子结构每个原子的能量和受力
     * @param aAPC 此原子的 apc 或者原子结构本身
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 按照 {@code [eng, fx, fy, fz]} 的顺序返回结果
     * @author liqa
     */
    public List<Vector> calEnergyForces(AtomicParameterCalculator aAPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        Vector rEngs = VectorCache.getVec(aAPC.atomNumber());
        Vector rForcesX = VectorCache.getVec(aAPC.atomNumber());
        Vector rForcesY = VectorCache.getVec(aAPC.atomNumber());
        Vector rForcesZ = VectorCache.getVec(aAPC.atomNumber());
        calEnergyForceVirials(aAPC, rEngs, rForcesX, rForcesY, rForcesZ, null, null, null, null, null, null, aTypeMap);
        return Lists.newArrayList(rEngs, rForcesX, rForcesY, rForcesZ);
    }
    public List<Vector> calEnergyForces(AtomicParameterCalculator aAPC) throws TorchException {return calEnergyForces(aAPC, type->type);}
    public List<Vector> calEnergyForces(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyForces(tAPC, tTypeMap);}
    }
    
    /**
     * 使用 nnap 计算所有需要的热力学量，需要注意的是，这里位力采用
     * lammps 一致的定义，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @author liqa
     */
    public void calEnergyForceVirials(IAtomData aAtomData, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData, mThreadNumber)) {calEnergyForceVirials(tAPC, rEnergies, rForcesX, rForcesY, rForcesZ, rVirialsXX, rVirialsYY, rVirialsZZ, rVirialsXY, rVirialsXZ, rVirialsYZ, tTypeMap);}
    }
    public void calEnergyForceVirials(final AtomicParameterCalculator aAPC, final @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aAPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of APC: " + aAPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 统一存储常量
        final int tAtomNumber = aAPC.atomNumber();
        final int tThreadNumber = aAPC.threadNumber();
        // 力需要累加统计，所以统一设置为 0
        if (rForcesX != null) rForcesX.fill(0.0);
        if (rForcesY != null) rForcesY.fill(0.0);
        if (rForcesZ != null) rForcesZ.fill(0.0);
        // 位力需要累加统计，所以统一设置为 0
        if (rVirialsXX != null) rVirialsXX.fill(0.0);
        if (rVirialsYY != null) rVirialsYY.fill(0.0);
        if (rVirialsZZ != null) rVirialsZZ.fill(0.0);
        if (rVirialsXY != null) rVirialsXY.fill(0.0);
        if (rVirialsXZ != null) rVirialsXZ.fill(0.0);
        if (rVirialsYZ != null) rVirialsYZ.fill(0.0);
        // 并行情况下存在并行写入的问题，因此需要这样操作
        IVector @Nullable[] rForcesXPar = rForcesX!=null ? new IVector[tThreadNumber] : null; if (rForcesX != null) {rForcesXPar[0] = rForcesX; for (int i = 1; i < tThreadNumber; ++i) {rForcesXPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rForcesYPar = rForcesY!=null ? new IVector[tThreadNumber] : null; if (rForcesY != null) {rForcesYPar[0] = rForcesY; for (int i = 1; i < tThreadNumber; ++i) {rForcesYPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rForcesZPar = rForcesZ!=null ? new IVector[tThreadNumber] : null; if (rForcesZ != null) {rForcesZPar[0] = rForcesZ; for (int i = 1; i < tThreadNumber; ++i) {rForcesZPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsXXPar = rVirialsXX!=null ? new IVector[tThreadNumber] : null; if (rVirialsXX != null) {rVirialsXXPar[0] = rVirialsXX; for (int i = 1; i < tThreadNumber; ++i) {rVirialsXXPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsYYPar = rVirialsYY!=null ? new IVector[tThreadNumber] : null; if (rVirialsYY != null) {rVirialsYYPar[0] = rVirialsYY; for (int i = 1; i < tThreadNumber; ++i) {rVirialsYYPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsZZPar = rVirialsZZ!=null ? new IVector[tThreadNumber] : null; if (rVirialsZZ != null) {rVirialsZZPar[0] = rVirialsZZ; for (int i = 1; i < tThreadNumber; ++i) {rVirialsZZPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsXYPar = rVirialsXY!=null ? new IVector[tThreadNumber] : null; if (rVirialsXY != null) {rVirialsXYPar[0] = rVirialsXY; for (int i = 1; i < tThreadNumber; ++i) {rVirialsXYPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsXZPar = rVirialsXZ!=null ? new IVector[tThreadNumber] : null; if (rVirialsXZ != null) {rVirialsXZPar[0] = rVirialsXZ; for (int i = 1; i < tThreadNumber; ++i) {rVirialsXZPar[i] = VectorCache.getZeros(tAtomNumber);}}
        IVector @Nullable[] rVirialsYZPar = rVirialsYZ!=null ? new IVector[tThreadNumber] : null; if (rVirialsYZ != null) {rVirialsYZPar[0] = rVirialsYZ; for (int i = 1; i < tThreadNumber; ++i) {rVirialsYZPar[i] = VectorCache.getZeros(tAtomNumber);}}
        try {
            aAPC.pool_().parforWithException(tAtomNumber, null, threadID -> {
                for (SingleNNAP tModel : mModels) {
                    tModel.clearSubmittedBatchBackward(threadID);
                }
            }, (i, threadID) -> {
                final @Nullable IVector tForcesX = rForcesX!=null ? rForcesXPar[threadID] : null;
                final @Nullable IVector tForcesY = rForcesY!=null ? rForcesYPar[threadID] : null;
                final @Nullable IVector tForcesZ = rForcesZ!=null ? rForcesZPar[threadID] : null;
                final @Nullable IVector tVirialsXX = rVirialsXX!=null ? rVirialsXXPar[threadID] : null;
                final @Nullable IVector tVirialsYY = rVirialsYY!=null ? rVirialsYYPar[threadID] : null;
                final @Nullable IVector tVirialsZZ = rVirialsZZ!=null ? rVirialsZZPar[threadID] : null;
                final @Nullable IVector tVirialsXY = rVirialsXY!=null ? rVirialsXYPar[threadID] : null;
                final @Nullable IVector tVirialsXZ = rVirialsXZ!=null ? rVirialsXZPar[threadID] : null;
                final @Nullable IVector tVirialsYZ = rVirialsYZ!=null ? rVirialsYZPar[threadID] : null;
                final SingleNNAP tModel = model(aTypeMap.applyAsInt(aAPC.atomType_().get(i)));
                final IBasis tBasis = tModel.basis(threadID);
                final List<@NotNull Vector> tOut = tBasis.evalPartial(true, true, aAPC, i, aTypeMap);
                Vector tBasisValue = tOut.get(0); tModel.normBasis(tBasisValue);
                tModel.submitBatchBackward(threadID, tBasisValue, rEnergies==null ? null : pred -> {
                    pred = tModel.denormEng(pred);
                    pred += tModel.mRefEng;
                    rEnergies.set(i, pred);
                }, xGrad -> {
                    tModel.normBasisPartial(xGrad);
                    tModel.denormEngPartial(xGrad);
                    final XYZ rBuf = new XYZ();
                    forceDot_(xGrad.internalData(), xGrad.internalDataShift(), tOut.get(1).internalData(), tOut.get(2).internalData(), tOut.get(3).internalData(), xGrad.internalDataSize(), rBuf);
                    if (tForcesX != null) tForcesX.add(i, -rBuf.mX);
                    if (tForcesY != null) tForcesY.add(i, -rBuf.mY);
                    if (tForcesZ != null) tForcesZ.add(i, -rBuf.mZ);
                    // 累加交叉项到近邻
                    final int tNN = (tOut.size()-4)/3;
                    final int[] j = {0};
                    aAPC.nl_().forEachNeighbor(i, tBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                        forceDot_(xGrad.internalData(), xGrad.internalDataShift(), tOut.get(4+j[0]).internalData(), tOut.get(4+tNN+j[0]).internalData(), tOut.get(4+tNN+tNN+j[0]).internalData(), xGrad.internalDataSize(), rBuf);
                        double fx = -rBuf.mX;
                        double fy = -rBuf.mY;
                        double fz = -rBuf.mZ;
                        if (tForcesX != null) tForcesX.add(idx, fx);
                        if (tForcesY != null) tForcesY.add(idx, fy);
                        if (tForcesZ != null) tForcesZ.add(idx, fz);
                        if (tVirialsXX != null) tVirialsXX.add(idx, dx*fx);
                        if (tVirialsYY != null) tVirialsYY.add(idx, dy*fy);
                        if (tVirialsZZ != null) tVirialsZZ.add(idx, dz*fz);
                        if (tVirialsXY != null) tVirialsXY.add(idx, dx*fy);
                        if (tVirialsXZ != null) tVirialsXZ.add(idx, dx*fz);
                        if (tVirialsYZ != null) tVirialsYZ.add(idx, dy*fz);
                        ++j[0];
                    });
                    // 注意要在这里归还中间变量，实际为延迟归还操作
                    VectorCache.returnVec(tOut);
                });
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 累加其余线程的数据然后归还临时变量
        if (rForcesZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rForcesZ.plus2this(rForcesZPar[i]); VectorCache.returnVec(rForcesZPar[i]);}}
        if (rForcesY != null) {for (int i = 1; i < tThreadNumber; ++i) {rForcesY.plus2this(rForcesYPar[i]); VectorCache.returnVec(rForcesYPar[i]);}}
        if (rForcesX != null) {for (int i = 1; i < tThreadNumber; ++i) {rForcesX.plus2this(rForcesXPar[i]); VectorCache.returnVec(rForcesXPar[i]);}}
        if (rVirialsYZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsYZ.plus2this(rVirialsYZPar[i]); VectorCache.returnVec(rVirialsYZPar[i]);}}
        if (rVirialsXZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsXZ.plus2this(rVirialsXZPar[i]); VectorCache.returnVec(rVirialsXZPar[i]);}}
        if (rVirialsXY != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsXY.plus2this(rVirialsXYPar[i]); VectorCache.returnVec(rVirialsXYPar[i]);}}
        if (rVirialsZZ != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsZZ.plus2this(rVirialsZZPar[i]); VectorCache.returnVec(rVirialsZZPar[i]);}}
        if (rVirialsYY != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsYY.plus2this(rVirialsYYPar[i]); VectorCache.returnVec(rVirialsYYPar[i]);}}
        if (rVirialsXX != null) {for (int i = 1; i < tThreadNumber; ++i) {rVirialsXX.plus2this(rVirialsXXPar[i]); VectorCache.returnVec(rVirialsXXPar[i]);}}
    }
    public void calEnergyForceVirials(AtomicParameterCalculator aAPC, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ) throws TorchException {
        calEnergyForceVirials(aAPC, rEnergies, rForcesX, rForcesY, rForcesZ, rVirialsXX, rVirialsYY, rVirialsZZ, rVirialsXY, rVirialsXZ, rVirialsYZ, type->type);
    }
    
    @ApiStatus.Internal
    public static void forceDot_(double[] aXGrad, int aShift, double[] aFpPx, double[] aFpPy, double[] aFpPz, int aLength, XYZ rBuf) {
        double rDotX = 0.0, rDotY = 0.0, rDotZ = 0.0;
        for (int i = 0, j = aShift; i < aLength; ++i, ++j) {
            double tXGrad = aXGrad[j];
            rDotX += tXGrad * aFpPx[i];
            rDotY += tXGrad * aFpPy[i];
            rDotZ += tXGrad * aFpPz[i];
        }
        rBuf.setXYZ(rDotX, rDotY, rDotZ);
    }
    
    
    private static native double forward0(long aModelPtr, double[] aX, int aStart, int aCount) throws TorchException;
    private static native double forward1(long aModelPtr, long aXPtr, int aCount) throws TorchException;
    private static native void batchForward0(long aModelPtr, double[] aX, int aStart, int aCount, double[] rY, int rYStart, int aBatchSize) throws TorchException;
    private static native void batchForward1(long aModelPtr, long aXPtr, int aCount, long rYPtr, int aBatchSize) throws TorchException;
    private static native double backward0(long aModelPtr, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException;
    private static native double backward1(long aModelPtr, long aXPtr, long rGradXPtr, int aCount) throws TorchException;
    private static native void batchBackward0(long aModelPtr, double[] aX, int aStart, double[] rGradX, int rStart, int aCount, double @Nullable[] rY, int rYStart, int aBatchSize) throws TorchException;
    private static native void batchBackward1(long aModelPtr, long aXPtr, long rGradXPtr, int aCount, long rYPtr, int aBatchSize) throws TorchException;
    
    static void rangeCheck(int jArraySize, int aCount) {
        if (aCount > jArraySize) throw new IndexOutOfBoundsException(aCount+" > "+jArraySize);
    }
}
