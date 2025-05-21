package jsex.nnap;

import jse.atom.*;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.clib.*;
import jse.code.*;
import jse.code.collection.DoubleList;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jsex.nnap.basis.BASIS;
import jsex.nnap.basis.IBasis;
import jsex.nnap.basis.Mirror;
import jsex.nnap.basis.SphericalChebyshev;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;

import static jse.code.OS.JAR_DIR;

/**
 * jse 实现的 nnap，所有 nnap 相关能量和力的计算都在此实现，
 * 具体定义可以参考：
 * <a href="https://link.springer.com/article/10.1007/s40843-024-2953-9">
 * Efficient and accurate simulation of vitrification in multi-component metallic liquids with neural-network potentials </a>
 * <p>
 * 这里使用 pytorch 来实现神经网络的部分
 * <p>
 * 考虑到 Torch 本身的内存安全性，此类设计时确保不同对象之间线程安全，
 * 而不同线程访问相同的对象线程不安全
 * <p>
 * 由于需要并行来绕开 GIL，并且考虑到效率问题，这里需要使用原生的 pytorch
 * <p>
 * 现在这个类会自动回收内部的 torch 模型指针，因此不需要担心内存泄漏的问题了；
 * 当然即使如此依旧建议手动调用 {@link #shutdown()} 来及时释放资源
 *
 * @author liqa
 */
public class NNAP implements IPairPotential {
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
    
    public final static int VERSION = 3;
    public final static String LIB_DIR = JAR_DIR+"nnap/jni/" + UT.Code.uniqueID(CS.VERSION, VERSION, Torch.HOME, Conf.USE_MIMALLOC, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "jsex_nnap_NNAP.cpp"
        , "jsex_nnap_NNAP.h"
        , "jsex_nnap_NNAPModelPointers.h"
    };
    /** 记录所有已经初始化 torch 线程的开启的线程，因为 torch 设置线程数居然是线程独立的 */
    private final static Set<Long> INITIALIZED_THREAD = Collections.synchronizedSet(new HashSet<>());
    
    static {
        InitHelper.INITIALIZED = true;
        // 依赖 torch
        Torch.InitHelper.init();
        // 依赖 jniutil
        JNIUtil.InitHelper.init();
        // 依赖 nnapbasis
        SphericalChebyshev.InitHelper.init();
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
        setTorchSingleThread();
    }
    
    /** 设置 torch 为单线程，初始 NNAP 时会自动调用，但在不同于初始线程下调用时需要重新设置 */
    @SuppressWarnings({"deprecation", "RedundantSuppression"})
    public static void setTorchSingleThread() {
        long tThreadID = Thread.currentThread().getId();
        if (!INITIALIZED_THREAD.contains(tThreadID)) {
            INITIALIZED_THREAD.add(tThreadID);
            try {setSingleThread0();}
            catch (TorchException ignored) {/* 可能已经设置过，这里就不考虑 */}
        }
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
        
        /// 现在使用全局的缓存实现，可以进一步减少内存池的调用操作
        private final Vector[][] mFp, mFpPx, mFpPy, mFpPz;
        private final DoubleList[][] mFpPxCross, mFpPyCross, mFpPzCross;
        
        private Vector bufFp(int aThreadID) {return mFp[aThreadID][mBatchedSize[aThreadID]];}
        private Vector bufFpPx(int aThreadID) {return mFpPx[aThreadID][mBatchedSize[aThreadID]];}
        private Vector bufFpPy(int aThreadID) {return mFpPy[aThreadID][mBatchedSize[aThreadID]];}
        private Vector bufFpPz(int aThreadID) {return mFpPz[aThreadID][mBatchedSize[aThreadID]];}
        private DoubleList bufFpPxCross(int aThreadID) {return mFpPxCross[aThreadID][mBatchedSize[aThreadID]];}
        private DoubleList bufFpPyCross(int aThreadID) {return mFpPyCross[aThreadID][mBatchedSize[aThreadID]];}
        private DoubleList bufFpPzCross(int aThreadID) {return mFpPzCross[aThreadID][mBatchedSize[aThreadID]];}
        
        private final DoubleList[] mForceX, mForceY, mForceZ;
        private DoubleList bufForceX(int aThreadID, int aSizeMin) {
            DoubleList tForceX = mForceX[aThreadID];
            int tSize = tForceX.size();
            if (tSize < aSizeMin) tForceX.addZeros(aSizeMin - tSize);
            return tForceX;
        }
        private DoubleList bufForceY(int aThreadID, int aSizeMin) {
            DoubleList tForceY = mForceY[aThreadID];
            int tSize = tForceY.size();
            if (tSize < aSizeMin) tForceY.addZeros(aSizeMin - tSize);
            return tForceY;
        }
        private DoubleList bufForceZ(int aThreadID, int aSizeMin) {
            DoubleList tForceZ = mForceZ[aThreadID];
            int tSize = tForceZ.size();
            if (tSize < aSizeMin) tForceZ.addZeros(aSizeMin - tSize);
            return tForceZ;
        }
        
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
            
            mFp = new Vector[mThreadNumber][BATCH_SIZE];
            mFpPx = new Vector[mThreadNumber][BATCH_SIZE];
            mFpPy = new Vector[mThreadNumber][BATCH_SIZE];
            mFpPz = new Vector[mThreadNumber][BATCH_SIZE];
            mFpPxCross = new DoubleList[mThreadNumber][BATCH_SIZE];
            mFpPyCross = new DoubleList[mThreadNumber][BATCH_SIZE];
            mFpPzCross = new DoubleList[mThreadNumber][BATCH_SIZE];
            for (int i = 0; i < mThreadNumber; ++i) for (int j = 0; j < BATCH_SIZE; ++j) {
                mFp[i][j] = VectorCache.getVec(mBasisSize);
                mFpPx[i][j] = VectorCache.getVec(mBasisSize);
                mFpPy[i][j] = VectorCache.getVec(mBasisSize);
                mFpPz[i][j] = VectorCache.getVec(mBasisSize);
                mFpPxCross[i][j] = new DoubleList(1024);
                mFpPyCross[i][j] = new DoubleList(1024);
                mFpPzCross[i][j] = new DoubleList(1024);
            }
            mForceX = new DoubleList[mThreadNumber];
            mForceY = new DoubleList[mThreadNumber];
            mForceZ = new DoubleList[mThreadNumber];
            for (int i = 0; i < mThreadNumber; ++i) {
                mForceX[i] = new DoubleList(16);
                mForceY[i] = new DoubleList(16);
                mForceZ[i] = new DoubleList(16);
            }
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
    @Override public int atomTypeNumber() {return mSymbols.length;}
    public SingleNNAP model(int aType) {return mModels.get(aType-1);}
    public @Unmodifiable List<SingleNNAP> models() {return mModels;}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
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
                for (int i = 0; i < mThreadNumber; ++i) for (int j = 0; j < BATCH_SIZE; ++j) {
                    VectorCache.returnVec(tModel.mFp[i][j]);
                    VectorCache.returnVec(tModel.mFpPx[i][j]);
                    VectorCache.returnVec(tModel.mFpPy[i][j]);
                    VectorCache.returnVec(tModel.mFpPz[i][j]);
                }
                for (int i = 0; i < mThreadNumber; ++i) {
                    tModel.basis(i).shutdown();
                }
                for (int i = 0; i < mThreadNumber; ++i) {
                    tModel.mModelPtrs.dispose();
                }
            }
        }
    }
    @Override public boolean isShutdown() {return mDead;}
    @Override public int threadNumber() {return mThreadNumber;}
    @VisibleForTesting public int nthreads() {return threadNumber();}
    @Override public double rcutMax() {
        double tRCut = 0.0;
        for (SingleNNAP tModel : models()) {
            tRCut = Math.max(tRCut, tModel.basis().rcut());
        }
        return tRCut;
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @param rEnergyAccumulator {@inheritDoc}
     */
    @Override public void calEnergy(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyAccumulator rEnergyAccumulator) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        final int tThreadNum = threadNumber();
        try {
            aNeighborListGetter.forEachNLWithException(threadID -> {
                if (tThreadNum > 1) setTorchSingleThread();
            }, threadID -> {
                for (SingleNNAP tModel : mModels) {
                    tModel.clearSubmittedBatchForward(threadID);
                }
            }, (threadID, cIdx, cType, nl) -> {
                final SingleNNAP tModel = model(cType);
                final IBasis tBasis = tModel.basis(threadID);
                // 目前还是采用缓存实现
                Vector tFp = VectorCache.getVec(tBasis.size());
                tBasis.eval(dxyzTypeDo -> {
                    nl.forEachDxyzTypeIdx(tBasis.rcut(), (dx, dy, dz, type, idx) -> dxyzTypeDo.run(dx, dy, dz, type));
                }, tFp);
                tModel.normBasis(tFp);
                tModel.submitBatchForward(threadID, tFp, pred -> {
                    pred = tModel.denormEng(pred);
                    pred += tModel.mRefEng;
                    rEnergyAccumulator.add(threadID, cIdx, -1, pred);
                });
                VectorCache.returnVec(tFp);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @param rEnergyAccumulator {@inheritDoc}
     * @param rForceAccumulator {@inheritDoc}
     * @param rVirialAccumulator {@inheritDoc}
     */
    @Override public void calEnergyForceVirial(int aAtomNumber, INeighborListGetter aNeighborListGetter, @Nullable IEnergyAccumulator rEnergyAccumulator, @Nullable IForceAccumulator rForceAccumulator, @Nullable IVirialAccumulator rVirialAccumulator) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        final int tThreadNum = threadNumber();
        try {
            aNeighborListGetter.forEachNLWithException(threadID -> {
                if (tThreadNum > 1) setTorchSingleThread();
            }, threadID -> {
                for (SingleNNAP tModel : mModels) {
                    tModel.clearSubmittedBatchBackward(threadID);
                }
            }, (threadID, cIdx, cType, nl) -> {
                final SingleNNAP tModel = model(cType);
                final IBasis tBasis = tModel.basis(threadID);
                final int tBasisSize = tBasis.size();
                final Vector tFp = tModel.bufFp(threadID);
                final Vector tFpPx = tModel.bufFpPx(threadID);
                final Vector tFpPy = tModel.bufFpPy(threadID);
                final Vector tFpPz = tModel.bufFpPz(threadID);
                final DoubleList tFpPxCross = tModel.bufFpPxCross(threadID);
                final DoubleList tFpPyCross = tModel.bufFpPyCross(threadID);
                final DoubleList tFpPzCross = tModel.bufFpPzCross(threadID);
                tBasis.evalPartial(dxyzTypeDo -> {
                    nl.forEachDxyzTypeIdx(tBasis.rcut(), (dx, dy, dz, type, idx) -> dxyzTypeDo.run(dx, dy, dz, type));
                }, tFp, tFpPx, tFpPy, tFpPz, tFpPxCross, tFpPyCross, tFpPzCross);
                final int tNN = tFpPxCross.size()/tBasisSize;
                tModel.normBasis(tFp);
                tModel.submitBatchBackward(threadID, tFp, rEnergyAccumulator==null ? null : pred -> {
                    pred = tModel.denormEng(pred);
                    pred += tModel.mRefEng;
                    rEnergyAccumulator.add(threadID, cIdx, -1, pred);
                }, xGrad -> {
                    tModel.normBasisPartial(xGrad);
                    tModel.denormEngPartial(xGrad);
                    final DoubleList tForceX = tModel.bufForceX(threadID, tNN+1);
                    final DoubleList tForceY = tModel.bufForceY(threadID, tNN+1);
                    final DoubleList tForceZ = tModel.bufForceZ(threadID, tNN+1);
                    BASIS.forceDot0(xGrad.internalData(), tFpPx.internalData(), tFpPy.internalData(), tFpPz.internalData(), xGrad.internalDataShift(), xGrad.internalDataSize(),
                                    tFpPxCross.internalData(), tFpPyCross.internalData(), tFpPzCross.internalData(), tForceX.internalData(), tForceY.internalData(), tForceZ.internalData(), tNN);
                    if (rForceAccumulator != null) {
                        rForceAccumulator.add(threadID, cIdx, -1, tForceX.get(0), tForceY.get(0), tForceZ.get(0));
                    }
                    // 累加交叉项到近邻
                    final int[] j = {1};
                    nl.forEachDxyzTypeIdx(tBasis.rcut(), (dx, dy, dz, type, idx) -> {
                        // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查
                        double fx = -tForceX.get(j[0]);
                        double fy = -tForceY.get(j[0]);
                        double fz = -tForceZ.get(j[0]);
                        if (rForceAccumulator != null) {
                            rForceAccumulator.add(threadID, -1, idx, fx, fy, fz);
                        }
                        if (rVirialAccumulator != null) {
                            rVirialAccumulator.add(threadID, cIdx, -1, dx*fx, dy*fy, dz*fz, dx*fy, dx*fz, dy*fz);
                        }
                        ++j[0];
                    });
                });
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
