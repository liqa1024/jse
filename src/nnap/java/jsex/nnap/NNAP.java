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
import jse.code.OS;
import jse.code.SP;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.ISlice;
import jse.code.collection.IntList;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import jsex.nnap.basis.IBasis;
import jsex.nnap.basis.SphericalChebyshev;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntUnaryOperator;

import static jse.code.CS.VERSION;
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
    
    private final static String LIB_DIR = JAR_DIR+"nnap/" + UT.Code.uniqueID(VERSION, Torch.HOME, Conf.USE_MIMALLOC, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    private final static String LIB_PATH;
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
        System.load(UT.IO.toAbsolutePath(LIB_PATH));
        
        // 这里需要 torch 单线程
        try {setSingleThread0();}
        catch (TorchException ignored) {/* 可能已经设置过，这里就不考虑 */}
    }
    private static native void setSingleThread0() throws TorchException;
    
    /** 中间缓存批次数量，一次传入多个批次进入神经网络可以大大提高效率 */
    public final static int BATCH_SIZE = 64;
    
    @SuppressWarnings("SameParameterValue")
    public class SingleNNAP {
        private final NNAPModelPointers mModelPtrs;
        private final double mRefEng;
        private final IVector mNormVec;
        private final IBasis[] mBasis;
        private final int mBasisSize;
        public double refEng() {return mRefEng;}
        public IVector normVec() {return mNormVec;}
        public IBasis basis() {return basis(0);}
        public IBasis basis(int aThreadID) {return mBasis[aThreadID];}
        
        @SuppressWarnings("unchecked")
        private SingleNNAP(Map<String, ?> aModelInfo) throws TorchException {
            Number tRefEng = (Number)aModelInfo.get("ref_eng");
            if (tRefEng == null) throw new IllegalArgumentException("No ref_eng in ModelInfo");
            mRefEng = tRefEng.doubleValue();
            List<? extends Number> tNormVec = (List<? extends Number>)aModelInfo.get("norm_vec");
            if (tNormVec == null) throw new IllegalArgumentException("No norm_vec in ModelInfo");
            mNormVec = Vectors.from(tNormVec);
            
            Map<String, ?> tBasis = (Map<String, ?>)aModelInfo.get("basis");
            if (tBasis == null) {
                tBasis = Maps.of(
                    "type", "spherical_chebyshev",
                    "nmax", SphericalChebyshev.DEFAULT_NMAX,
                    "lmax", SphericalChebyshev.DEFAULT_LMAX,
                    "rcut", SphericalChebyshev.DEFAULT_RCUT
                );
            }
            Object tBasisType = tBasis.get("type");
            if (tBasisType == null) {
                tBasisType = "spherical_chebyshev";
            }
            if (!tBasisType.equals("spherical_chebyshev")) throw new IllegalArgumentException("Unsupported basis type: " + tBasisType);
            mBasis = new IBasis[mThreadNumber];
            for (int i = 0; i < mThreadNumber; ++i) {
                //noinspection resource
                mBasis[i] = SphericalChebyshev.load(mSymbols, tBasis);
            }
            mBasisSize = mBasis[0].rowNumber()*mBasis[0].columnNumber();
            Object tModel = aModelInfo.get("torch");
            if (tModel == null) throw new IllegalArgumentException("No torch data in ModelInfo");
            byte[] tModelBytes = Base64.getDecoder().decode(tModel.toString());
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
        protected void submitBatchForward(int aThreadID, IVector aX, DoubleConsumer aPredDo) throws TorchException {
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
        protected void clearSubmittedBatchForward(int aThreadID) throws TorchException {
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
        protected void submitBatchForward(IVector aX, DoubleConsumer aPredDo) throws TorchException {submitBatchForward(0, aX, aPredDo);}
        protected void clearSubmittedBatchForward() throws TorchException {clearSubmittedBatchForward(0);}
        
        /** 提交批量 forward 接口，注意由于内部存储共享，因此不能和 {@link #submitBatchForward} 混用，在使用 forward 前务必使用 {@link #clearSubmittedBatchBackward} 清空缓存 */
        protected void submitBatchBackward(int aThreadID, IVector aX, @Nullable DoubleConsumer aPredDo, Consumer<? super IVector> aXGradDo) throws TorchException {
            @Nullable DoubleConsumer[] tBatchedPredDo = mBatchedPredDo[aThreadID];
            Consumer<? super IVector>[] tBatchedXGradDo = mBatchedXGradDo[aThreadID];
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
        protected void clearSubmittedBatchBackward(int aThreadID) throws TorchException {
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
        protected void submitBatchBackward(IVector aX, @Nullable DoubleConsumer aPredDo, Consumer<? super IVector> aXGradDo) throws TorchException {submitBatchBackward(0, aX, aPredDo, aXGradDo);}
        protected void clearSubmittedBatchBackward() throws TorchException {clearSubmittedBatchBackward(0);}
        
        
        protected double forward(int aThreadID, double[] aX, int aStart, int aCount) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount);
            return forward0(mModelPtrs.mPtrs[aThreadID], aX, aStart, aCount);
        }
        protected double forward(double[] aX, int aStart, int aCount) throws TorchException {return forward(0, aX, aStart, aCount);}
        protected double forward(double[] aX, int aCount) throws TorchException {return forward(aX, 0, aCount);}
        protected double forward(double[] aX) throws TorchException {return forward(aX, aX.length);}
        protected double forward(int aThreadID, DoubleCPointer aX, int aCount) throws TorchException {return forward1(mModelPtrs.mPtrs[aThreadID], aX.ptr_(), aCount);}
        protected double forward(DoubleCPointer aX, int aCount) throws TorchException {return forward(0, aX, aCount);}
        
        protected void batchForward(int aThreadID, double[] aX, int aStart, int aCount, double[] rY, int rYStart, int aBatchSize) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount);
            rangeCheck(rY.length, rYStart+aBatchSize);
            batchForward0(mModelPtrs.mPtrs[aThreadID], aX, aStart, aCount, rY, rYStart, aBatchSize);
        }
        protected void batchForward(double[] aX, int aStart, int aCount, double[] rY, int rYStart, int aBatchSize) throws TorchException {batchForward(0, aX, aStart, aCount, rY, rYStart, aBatchSize);}
        protected void batchForward(double[] aX, int aCount, double[] rY, int aBatchSize) throws TorchException {batchForward(aX, 0, aCount, rY, 0, aBatchSize);}
        protected void batchForward(double[] aX, double[] rY) throws TorchException {batchForward(aX, aX.length/rY.length, rY, rY.length);}
        protected void batchForward(int aThreadID, DoubleCPointer aX, int aCount, DoubleCPointer rY, int aBatchSize) throws TorchException {batchForward1(mModelPtrs.mPtrs[aThreadID], aX.ptr_(), aCount, rY.ptr_(), aBatchSize);}
        protected void batchForward(DoubleCPointer aX, int aCount, DoubleCPointer rY, int aBatchSize) throws TorchException {batchForward(0, aX, aCount, rY, aBatchSize);}
        
        protected double backward(int aThreadID, double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount);
            rangeCheck(rGradX.length, rStart+aCount);
            return backward0(mModelPtrs.mPtrs[aThreadID], aX, aStart, rGradX, rStart, aCount);
        }
        protected double backward(double[] aX, int aStart, double[] rGradX, int rStart, int aCount) throws TorchException {return backward(0, aX, aStart, rGradX, rStart, aCount);}
        protected double backward(double[] aX, double[] rGradX, int aCount) throws TorchException {return backward(aX, 0, rGradX, 0, aCount);}
        protected double backward(double[] aX, double[] rGradX) throws TorchException {return backward(aX, rGradX, aX.length);}
        protected double backward(int aThreadID, DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward1(mModelPtrs.mPtrs[aThreadID], aX.ptr_(), rGradX.ptr_(), aCount);}
        protected double backward(DoubleCPointer aX, DoubleCPointer rGradX, int aCount) throws TorchException {return backward(0, aX, rGradX, aCount);}
        
        protected void batchBackward(int aThreadID, double[] aX, int aStart, double[] rGradX, int rStart, int aCount, double @Nullable[] rY, int rYStart, int aBatchSize) throws TorchException {
            if (mDead) throw new IllegalStateException("This NNAP is dead");
            rangeCheck(aX.length, aStart+aCount*aBatchSize);
            rangeCheck(rGradX.length, rStart+aCount*aBatchSize);
            if (rY != null) rangeCheck(rY.length, rYStart+aBatchSize);
            batchBackward0(mModelPtrs.mPtrs[aThreadID], aX, aStart, rGradX, rStart, aCount, rY, rYStart, aBatchSize);
        }
        protected void batchBackward(double[] aX, int aStart, double[] rGradX, int rStart, int aCount, double @Nullable[] rY, int rYStart, int aBatchSize) throws TorchException {batchBackward(0, aX, aStart, rGradX, rStart, aCount, rY, rYStart, aBatchSize);}
        protected void batchBackward(double[] aX, double[] rGradX, int aCount, double @Nullable[] rY, int aBatchSize) throws TorchException {batchBackward(aX, 0, rGradX, 0, aCount, rY, 0, aBatchSize);}
        protected void batchBackward(double[] aX, double[] rGradX, double[] rY) throws TorchException {batchBackward(aX, rGradX, aX.length/rY.length, rY, rY.length);}
        protected void batchBackward(int aThreadID, DoubleCPointer aX, DoubleCPointer rGradX, int aCount, @Nullable DoubleCPointer rY, int aBatchSize) throws TorchException {batchBackward1(mModelPtrs.mPtrs[aThreadID], aX.ptr_(), rGradX.ptr_(), aCount, rY==null ? 0 : rY.ptr_(), aBatchSize);}
        protected void batchBackward(DoubleCPointer aX, DoubleCPointer rGradX, int aCount, @Nullable DoubleCPointer rY, int aBatchSize) throws TorchException {batchBackward(0, aX, rGradX, aCount, rY, aBatchSize);}
        
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
            if (tVersionValue != 1) throw new IllegalArgumentException("Unsupported version: " + tVersionValue);
        }
        mUnits = UT.Code.toString(aModelInfo.get("units"));
        List<? extends Map<String, ?>> tModelInfos = (List<? extends Map<String, ?>>)aModelInfo.get("models");
        if (tModelInfos == null) throw new IllegalArgumentException("No models in ModelInfo");
        int tModelSize = tModelInfos.size();
        mSymbols = new String[tModelSize];
        for (int i = 0; i < tModelSize; i++) {
            Object tSymbol = tModelInfos.get(i).get("symbol");
            if (tSymbol == null) throw new IllegalArgumentException("No symbol in ModelInfo");
            mSymbols[i] = tSymbol.toString();
        }
        mModels = new ArrayList<>(tModelSize);
        for (Map<String, ?> tModelInfo : tModelInfos) {
            mModels.add(new SingleNNAP(tModelInfo));
        }
    }
    public NNAP(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws TorchException, IOException {
        this(aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? UT.IO.yaml2map(aModelPath) : UT.IO.json2map(aModelPath), aThreadNumber);
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
    
    public IntUnaryOperator typeMap(IAtomData aAtomData) {return IBasis.typeMap_(symbols(), aAtomData);}
    public boolean sameOrder(List<String> aDataSymbols) {return IBasis.sameOrder_(symbols(), aDataSymbols);}
    public int indexOf(String aSymbol) {return IBasis.indexOf_(symbols(), aSymbol);}
    
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
        IAtomData tAtoms = AseAtoms.of(aPyAseAtoms).setSymbolOrder(mSymbols);
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
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(tAtoms)) {
            calEnergyForceVirials(tMPC, rEnergies, rForces.col(0), rForces.col(1), rForces.col(2),
                                  rStresses.col(0), rStresses.col(1), rStresses.col(2), rStresses.col(5), rStresses.col(4), rStresses.col(3));
        }
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
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 每个原子能量组成的向量
     * @author liqa
     */
    public Vector calEnergies(final MonatomicParameterCalculator aMPC, final IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 统一存储常量
        final int tAtomNumber = aMPC.atomNumber();
        Vector rEngs = VectorCache.getVec(tAtomNumber);
        try {
            aMPC.pool_().parforWithException(tAtomNumber, null, threadID -> {
                for (SingleNNAP tModel : mModels) {
                    tModel.clearSubmittedBatchForward(threadID);
                }
            }, (i, threadID) -> {
                final SingleNNAP tModel = model(aTypeMap.applyAsInt(aMPC.atomType_().get(i)));
                final IBasis tBasis = tModel.basis(threadID);
                RowMatrix tBasisValue = tBasis.eval(aMPC, i, aTypeMap);
                tBasisValue.asVecRow().div2this(tModel.mNormVec);
                tModel.submitBatchForward(threadID, tBasisValue.asVecRow(), pred -> {
                    pred += tModel.mRefEng;
                    rEngs.set(i, pred);
                });
                MatrixCache.returnMat(tBasisValue);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rEngs;
    }
    public Vector calEnergies(MonatomicParameterCalculator aMPC) throws TorchException {return calEnergies(aMPC, type->type);}
    public Vector calEnergies(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergies(tMPC, tTypeMap);}
    }
    /**
     * 使用 nnap 计算给定原子结构指定原子的能量
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aIndices 需要计算的原子的索引（从 0 开始）
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 此原子的能量组成的向量
     * @author liqa
     */
    public Vector calEnergiesAt(final MonatomicParameterCalculator aMPC, final ISlice aIndices, final IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 统一存储常量
        final int tSize = aIndices.size();
        Vector rEngs = VectorCache.getVec(tSize);
        try {
            aMPC.pool_().parforWithException(tSize, null, threadID -> {
                for (SingleNNAP tModel : mModels) {
                    tModel.clearSubmittedBatchForward(threadID);
                }
            }, (i, threadID) -> {
                final int cIdx = aIndices.get(i);
                final SingleNNAP tModel = model(aTypeMap.applyAsInt(aMPC.atomType_().get(cIdx)));
                final IBasis tBasis = tModel.basis(threadID);
                RowMatrix tBasisValue = tBasis.eval(aMPC, cIdx, aTypeMap);
                tBasisValue.asVecRow().div2this(tModel.mNormVec);
                tModel.submitBatchForward(threadID, tBasisValue.asVecRow(), pred -> {
                    pred += tModel.mRefEng;
                    rEngs.set(i, pred);
                });
                MatrixCache.returnMat(tBasisValue);
            });
        } catch (TorchException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rEngs;
    }
    public Vector calEnergiesAt(MonatomicParameterCalculator aMPC, ISlice aIndices) throws TorchException {return calEnergiesAt(aMPC, aIndices, type->type);}
    public Vector calEnergiesAt(IAtomData aAtomData, ISlice aIndices) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergiesAt(tMPC, aIndices, tTypeMap);}
    }
    /**
     * 使用 nnap 计算给定原子结构的总能量
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 总能量
     * @author liqa
     */
    public double calEnergy(MonatomicParameterCalculator aMPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        Vector tEngs = calEnergies(aMPC, aTypeMap);
        double tTotEng = tEngs.sum();
        VectorCache.returnVec(tEngs);
        return tTotEng;
    }
    public double calEnergy(MonatomicParameterCalculator aMPC) throws TorchException {return calEnergy(aMPC, type->type);}
    public double calEnergy(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergy(tMPC, tTypeMap);}
    }
    
    /**
     * 计算移动前后的能量差，只计算移动影响的近邻列表中的原子，可以加快计算速度
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aI 移动原子的索引
     * @param aDx x 方向移动的距离
     * @param aDy y 方向移动的距离
     * @param aDz z 方向移动的距离
     * @param aRestoreMPC 计算完成后是否还原 MPC 的状态，默认为 {@code true}
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 移动后能量 - 移动前能量
     */
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, double aDx, double aDy, double aDz, boolean aRestoreMPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 采用最大的截断半径从而包含所有可能涉及发生了能量变换的原子
        double tRCut = 0.0;
        for (SingleNNAP tModel : models()) {
            tRCut = Math.max(tRCut, tModel.basis().rcut());
        }
        XYZ oXYZ = new XYZ(aMPC.atomDataXYZ_().row(aI));
        IIntVector oNL = aMPC.getNeighborList(oXYZ, tRCut);
        XYZ nXYZ = oXYZ.plus(aDx, aDy, aDz);
        IIntVector nNL = aMPC.getNeighborList(nXYZ, tRCut);
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(oNL.size());
        oNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        nNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        Vector oEngs = calEnergiesAt(aMPC, tNL, aTypeMap);
        double oEng = oEngs.sum();
        VectorCache.returnVec(oEngs);
        Vector nEngs = calEnergiesAt(aMPC.setAtomXYZ(aI, nXYZ), tNL, aTypeMap);
        double nEng = nEngs.sum();
        VectorCache.returnVec(nEngs);
        if (aRestoreMPC) aMPC.setAtomXYZ(aI, oXYZ);
        return nEng - oEng;
    }
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, double aDx, double aDy, double aDz, IntUnaryOperator aTypeMap) throws TorchException {return calEnergyDiffMove(aMPC, aI, aDx, aDy, aDz, true, aTypeMap);}
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, double aDx, double aDy, double aDz, boolean aRestoreMPC) throws TorchException {return calEnergyDiffMove(aMPC, aI, aDx, aDy, aDz, aRestoreMPC, type->type);}
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, double aDx, double aDy, double aDz) throws TorchException {return calEnergyDiffMove(aMPC, aI, aDx, aDy, aDz, type->type);}
    public double calEnergyDiffMove(IAtomData aAtomData, int aI, double aDx, double aDy, double aDz) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyDiffMove(tMPC, aI, aDx, aDy, aDz, false, tTypeMap);}
    }
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, IXYZ aDxyz, IntUnaryOperator aTypeMap) throws TorchException {return calEnergyDiffMove(aMPC, aI, aDxyz.x(), aDxyz.y(), aDxyz.z(), aTypeMap);}
    public double calEnergyDiffMove(MonatomicParameterCalculator aMPC, int aI, IXYZ aDxyz) throws TorchException {return calEnergyDiffMove(aMPC, aI, aDxyz, type->type);}
    public double calEnergyDiffMove(IAtomData aAtomData, int aI, IXYZ aDxyz) throws TorchException {return calEnergyDiffMove(aAtomData, aI, aDxyz.x(), aDxyz.y(), aDxyz.z());}
    /**
     * 计算交换种类前后的能量差，只计算交换影响的近邻列表中的原子，可以加快计算速度
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aI 需要交换种类的第一个原子索引
     * @param aJ 需要交换种类的第二个原子索引
     * @param aRestoreMPC 计算完成后是否还原 MPC 的状态，默认为 {@code true}
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 交换后能量 - 交换前能量
     */
    public double calEnergyDiffSwap(MonatomicParameterCalculator aMPC, int aI, int aJ, boolean aRestoreMPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        int oTypeI = aMPC.atomType_().get(aI);
        int oTypeJ = aMPC.atomType_().get(aJ);
        if (oTypeI == oTypeJ) return 0.0;
        // 采用最大的截断半径从而包含所有可能涉及发生了能量变换的原子
        double tRCut = 0.0;
        for (SingleNNAP tModel : models()) {
            tRCut = Math.max(tRCut, tModel.basis().rcut());
        }
        IIntVector iNL = aMPC.getNeighborList(aI, tRCut);
        IIntVector jNL = aMPC.getNeighborList(aJ, tRCut);
        // 合并近邻列表，这里简单遍历实现
        final IntList tNL = new IntList(iNL.size()+1);
        tNL.add(aI);
        iNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        if (!tNL.contains(aJ)) tNL.add(aJ);
        jNL.forEach(idx -> {
            if (!tNL.contains(idx)) tNL.add(idx);
        });
        Vector oEngs = calEnergiesAt(aMPC, tNL, aTypeMap);
        double oEng = oEngs.sum();
        VectorCache.returnVec(oEngs);
        Vector nEngs = calEnergiesAt(aMPC.setAtomType(aI, oTypeJ).setAtomType(aJ, oTypeI), tNL, aTypeMap);
        double nEng = nEngs.sum();
        VectorCache.returnVec(nEngs);
        if (aRestoreMPC) aMPC.setAtomType(aI, oTypeI).setAtomType(aJ, oTypeJ);
        return nEng - oEng;
    }
    public double calEnergyDiffSwap(MonatomicParameterCalculator aMPC, int aI, int aJ, IntUnaryOperator aTypeMap) throws TorchException {return calEnergyDiffSwap(aMPC, aI, aJ, true, aTypeMap);}
    public double calEnergyDiffSwap(MonatomicParameterCalculator aMPC, int aI, int aJ, boolean aRestoreMPC) throws TorchException {return calEnergyDiffSwap(aMPC, aI, aJ, aRestoreMPC, type->type);}
    public double calEnergyDiffSwap(MonatomicParameterCalculator aMPC, int aI, int aJ) throws TorchException {return calEnergyDiffSwap(aMPC, aI, aJ, type->type);}
    public double calEnergyDiffSwap(IAtomData aAtomData, int aI, int aJ) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyDiffSwap(tMPC, aI, aJ, false, tTypeMap);}
    }
    
    
    /**
     * 使用 nnap 计算给定原子结构每个原子和受力
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 每个原子力组成的矩阵，按行排列
     * @author liqa
     */
    public RowMatrix calForces(MonatomicParameterCalculator aMPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        RowMatrix rForces = MatrixCache.getMatRow(aMPC.atomNumber(), 3);
        calEnergyForceVirials(aMPC, null, rForces.col(0), rForces.col(1), rForces.col(2), null, null, null, null, null, null, aTypeMap);
        return rForces;
    }
    public RowMatrix calForces(MonatomicParameterCalculator aMPC) throws TorchException {return calForces(aMPC, type->type);}
    public RowMatrix calForces(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calForces(tMPC, tTypeMap);}
    }
    
    /**
     * 使用 nnap 计算给定结构所有原子的单独应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAtomData 原子结构本身或者 MPC，使用 MPC 时统一不考虑速度部分
     * @param aIdealGas 是否考虑理想气体部分（速度效应部分），默认为 {@code false}
     * @return 按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列的应力向量
     * @author liqa
     */
    public List<Vector> calStresses(IAtomData aAtomData, boolean aIdealGas) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        final int tAtomNum = aAtomData.atomNumber();
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {
            List<Vector> tStresses = calStresses(tMPC, tTypeMap);
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
    public List<Vector> calStresses(MonatomicParameterCalculator aMPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        final int tAtomNum = aMPC.atomNumber();
        List<Vector> rStresses = VectorCache.getVec(tAtomNum, 6);
        calEnergyForceVirials(aMPC, null, null, null, null, rStresses.get(0), rStresses.get(1), rStresses.get(2), rStresses.get(3), rStresses.get(4), rStresses.get(5), aTypeMap);
        for (int i = 0; i < 6; ++i) {
            rStresses.get(i).operation().negative2this();
        }
        return rStresses;
    }
    public List<Vector> calStresses(MonatomicParameterCalculator aMPC) throws TorchException {return calStresses(aMPC, type->type);}
    /**
     * 使用 nnap 计算给定原子结构的应力，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @param aAtomData 原子结构本身或者 MPC，使用 MPC 时统一不考虑速度部分
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
    public List<Double> calStress(MonatomicParameterCalculator aMPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        List<Vector> tStresses = calStresses(aMPC, aTypeMap);
        double tStressXX = tStresses.get(0).sum();
        double tStressYY = tStresses.get(1).sum();
        double tStressZZ = tStresses.get(2).sum();
        double tStressXY = tStresses.get(3).sum();
        double tStressXZ = tStresses.get(4).sum();
        double tStressYZ = tStresses.get(5).sum();
        VectorCache.returnVec(tStresses);
        double tVolume = aMPC.volume();
        tStressXX /= tVolume;
        tStressYY /= tVolume;
        tStressZZ /= tVolume;
        tStressXY /= tVolume;
        tStressXZ /= tVolume;
        tStressYZ /= tVolume;
        return Lists.newArrayList(tStressXX, tStressYY, tStressZZ, tStressXY, tStressXZ, tStressYZ);
    }
    public List<Double> calStress(MonatomicParameterCalculator aMPC) throws TorchException {return calStress(aMPC, type->type);}
    
    /**
     * 使用 nnap 同时计算给定原子结构每个原子的能量和受力
     * @param aMPC 此原子的 mpc 或者原子结构本身
     * @param aTypeMap 计算器中元素种类到基组定义的种类序号的一个映射，默认不做映射
     * @return 按照 {@code [eng, fx, fy, fz]} 的顺序返回结果
     * @author liqa
     */
    public List<Vector> calEnergyForces(MonatomicParameterCalculator aMPC, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        Vector rEngs = VectorCache.getVec(aMPC.atomNumber());
        Vector rForcesX = VectorCache.getVec(aMPC.atomNumber());
        Vector rForcesY = VectorCache.getVec(aMPC.atomNumber());
        Vector rForcesZ = VectorCache.getVec(aMPC.atomNumber());
        calEnergyForceVirials(aMPC, rEngs, rForcesX, rForcesY, rForcesZ, null, null, null, null, null, null, aTypeMap);
        return Lists.newArrayList(rEngs, rForcesX, rForcesY, rForcesZ);
    }
    public List<Vector> calEnergyForces(MonatomicParameterCalculator aMPC) throws TorchException {return calEnergyForces(aMPC, type->type);}
    public List<Vector> calEnergyForces(IAtomData aAtomData) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        try (MonatomicParameterCalculator tMPC = MonatomicParameterCalculator.of(aAtomData, mThreadNumber)) {return calEnergyForces(tMPC, tTypeMap);}
    }
    
    /**
     * 使用 nnap 计算所有需要的热力学量，需要注意的是，这里位力采用
     * lammps 一致的定义，具体可以参见：
     * <a href="https://en.wikipedia.org/wiki/Virial_stress">
     * Virial stress - Wikipedia </a>
     * @author liqa
     */
    public void calEnergyForceVirials(final MonatomicParameterCalculator aMPC, final @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ, IntUnaryOperator aTypeMap) throws TorchException {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (atomTypeNumber() < aMPC.atomTypeNumber()) throw new IllegalArgumentException("Invalid atom type number of MPC: " + aMPC.atomTypeNumber() + ", model: " + atomTypeNumber());
        // 统一存储常量
        final int tAtomNumber = aMPC.atomNumber();
        final int tThreadNumber = aMPC.threadNumber();
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
            aMPC.pool_().parforWithException(tAtomNumber, null, threadID -> {
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
                final SingleNNAP tModel = model(aTypeMap.applyAsInt(aMPC.atomType_().get(i)));
                final IBasis tBasis = tModel.basis(threadID);
                final List<@NotNull RowMatrix> tOut = tBasis.evalPartial(true, true, aMPC, i, aTypeMap);
                RowMatrix tBasisValue = tOut.get(0); tBasisValue.asVecRow().div2this(tModel.mNormVec);
                tModel.submitBatchBackward(threadID, tBasisValue.asVecRow(), rEnergies==null ? null : pred -> {
                    pred += tModel.mRefEng;
                    rEnergies.set(i, pred);
                }, xGrad -> {
                    xGrad.div2this(tModel.mNormVec);
                    if (tForcesX != null) tForcesX.add(i, -xGrad.opt().dot(tOut.get(1).asVecRow()));
                    if (tForcesY != null) tForcesY.add(i, -xGrad.opt().dot(tOut.get(2).asVecRow()));
                    if (tForcesZ != null) tForcesZ.add(i, -xGrad.opt().dot(tOut.get(3).asVecRow()));
                    // 累加交叉项到近邻
                    final int tNN = (tOut.size()-4)/3;
                    final int[] j = {0};
                    aMPC.nl_().forEachNeighbor(i, tBasis.rcut(), false, (x, y, z, idx, dx, dy, dz) -> {
                        double fx = -xGrad.opt().dot(tOut.get(4+j[0]).asVecRow());
                        double fy = -xGrad.opt().dot(tOut.get(4+tNN+j[0]).asVecRow());
                        double fz = -xGrad.opt().dot(tOut.get(4+tNN+tNN+j[0]).asVecRow());
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
                    MatrixCache.returnMat(tOut);
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
    public void calEnergyForceVirials(MonatomicParameterCalculator aMPC, @Nullable IVector rEnergies, @Nullable IVector rForcesX, @Nullable IVector rForcesY, @Nullable IVector rForcesZ, @Nullable IVector rVirialsXX, @Nullable IVector rVirialsYY, @Nullable IVector rVirialsZZ, @Nullable IVector rVirialsXY, @Nullable IVector rVirialsXZ, @Nullable IVector rVirialsYZ) throws TorchException {
        calEnergyForceVirials(aMPC, rEnergies, rForcesX, rForcesY, rForcesZ, rVirialsXX, rVirialsYY, rVirialsZZ, rVirialsXY, rVirialsXZ, rVirialsYZ, type->type);
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
