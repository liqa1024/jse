package jsex.nnap;

import jse.atom.IPairPotential;
import jse.clib.JNIUtil;
import jse.code.CS;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.IDataShell;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import jsex.nnap.basis.*;
import jsex.nnap.nn.*;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
@SuppressWarnings("deprecation")
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
        public static int OPT_LEVEL = OS.envI("JSE_NNAP_OPT_LEVEL", BASE);
        
        /**
         * 自定义构建 native nnap basis 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_C_COMPILER   = OS.env("JSE_CMAKE_C_COMPILER_NNAP"  , jse.code.Conf.CMAKE_C_COMPILER);
        public static @Nullable String CMAKE_C_FLAGS      = OS.env("JSE_CMAKE_C_FLAGS_NNAP"     , jse.code.Conf.CMAKE_C_FLAGS);
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_NNAP", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_NNAP"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        
        /** 重定向 native nnap basis 动态库的路径 */
        public static @Nullable String REDIRECT_NNAPBASIS_LIB = OS.env("JSE_REDIRECT_NNAP_LIB");
    }
    
    public final static int VERSION = 4;
    public final static String LIB_DIR = JAR_DIR+"nnap/jni/" + UT.Code.uniqueID(CS.VERSION, VERSION, Conf.OPT_LEVEL, Conf.CMAKE_C_COMPILER, Conf.CMAKE_C_FLAGS, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING) + "/";
    public final static String LIB_PATH;
    private final static String[] SRC_NAME = {
          "nnap_util.h"
        , "jsex_nnap_NNAP.c"
        , "jsex_nnap_NNAP.h"
        , "jsex_nnap_nn_FeedForward.c"
        , "jsex_nnap_nn_FeedForward.h"
        , "jsex_nnap_basis_SphericalChebyshev.c"
        , "jsex_nnap_basis_SphericalChebyshev.h"
        , "jsex_nnap_basis_Chebyshev.c"
        , "jsex_nnap_basis_Chebyshev.h"
    };
    
    static {
        InitHelper.INITIALIZED = true;
        // 这里不直接依赖 LmpPlugin
        
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
        LIB_PATH = new JNIUtil.LibBuilder("nnap", "NNAP", LIB_DIR, rCmakeSetting)
            .setSrc("nnap", SRC_NAME)
            .setCmakeCCompiler(Conf.CMAKE_C_COMPILER).setCmakeCFlags(Conf.CMAKE_C_FLAGS)
            .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(Conf.CMAKE_CXX_FLAGS)
            .setRedirectLibPath(Conf.REDIRECT_NNAPBASIS_LIB)
            .get();
        // 设置库路径
        System.load(IO.toAbsolutePath(LIB_PATH));
    }
    
    static void forceDot(IDataShell<double[]> aXGrad, IDataShell<double[]> aFpPx, IDataShell<double[]> aFpPy, IDataShell<double[]> aFpPz,
                         IDataShell<double[]> rFx, IDataShell<double[]> rFy, IDataShell<double[]> rFz, int aNN) {
        int tLength = aXGrad.internalDataSize();
        int tShift = aXGrad.internalDataShift();
        forceDot1(aXGrad.internalDataWithLengthCheck(tLength, tShift), tShift, tLength,
                  aFpPx.internalDataWithLengthCheck(tLength*aNN, 0), aFpPy.internalDataWithLengthCheck(tLength*aNN, 0), aFpPz.internalDataWithLengthCheck(tLength*aNN, 0),
                  rFx.internalDataWithLengthCheck(aNN, 0), rFy.internalDataWithLengthCheck(aNN, 0), rFz.internalDataWithLengthCheck(aNN, 0), aNN);
    }
    private static native void forceDot1(double[] aXGrad, int aShift, int aLength, double[] aFpPx, double[] aFpPy, double[] aFpPz, double[] rFx, double[] rFy, double[] rFz, int aNN);
    
    
    @SuppressWarnings("unchecked")
    private @Nullable SingleNNAP initSingleNNAPFrom(int aType, Map<String, ?> aModelInfo) throws Exception {
        Map<String, ?> tBasis = (Map<String, ?>)aModelInfo.get("basis");
        if (tBasis == null) {
            tBasis = Maps.of("type", "spherical_chebyshev");
        }
        Object tBasisType = tBasis.get("type");
        if (tBasisType == null) {
            tBasisType = "spherical_chebyshev";
        }
        Basis[] aBasis = new Basis[mThreadNumber];
        switch(tBasisType.toString()) {
        case "mirror": {
            return null; // mirror 情况延迟初始化
        }
        case "spherical_chebyshev": {
            for (int i = 0; i < mThreadNumber; ++i) {
                aBasis[i] = SphericalChebyshev.load(mSymbols, tBasis);
            }
            break;
        }
        case "chebyshev": {
            for (int i = 0; i < mThreadNumber; ++i) {
                aBasis[i] = Chebyshev.load(mSymbols, tBasis);
            }
            break;
        }
        case "merge": {
            for (int i = 0; i < mThreadNumber; ++i) {
                aBasis[i] = Merge.load(mSymbols, tBasis);
            }
            break;
        }
        default: {
            throw new IllegalArgumentException("Unsupported basis type: " + tBasisType);
        }}
        
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
        
        NeuralNetwork[] aNN = new NeuralNetwork[mThreadNumber];
        Object tModelObj = aModelInfo.get("torch");
        if (tModelObj != null) {
            mIsTorch = true;
            for (int i = 0; i < mThreadNumber; ++i) {
                //noinspection resource
                aNN[i] = new TorchModel(tModelObj.toString());
            }
            return new SingleNNAP(aRefEng, aNormMu, aNormSigma, aNormMuEng, aNormSigmaEng, aBasis, aNN);
        }
        Map<String, ?> tModel = (Map<String, ?>)aModelInfo.get("nn");
        Object tModelType = tModel.get("type");
        if (tModelType == null) {
            tModelType = "feed_forward";
        }
        switch(tModelType.toString()) {
        case "feed_forward": {
            for (int i = 0; i < mThreadNumber; ++i) {
                aNN[i] = FeedForward.load(tModel);
            }
            break;
        }
        case "torch": {
            mIsTorch = true;
            for (int i = 0; i < mThreadNumber; ++i) {
                //noinspection resource
                aNN[i] = new TorchModel(tModel.get("model").toString());
            }
            break;
        }
        default: {
            throw new IllegalArgumentException("Unsupported model type: " + tBasisType);
        }}
        return new SingleNNAP(aRefEng, aNormMu, aNormSigma, aNormMuEng, aNormSigmaEng, aBasis, aNN);
    }
    @SuppressWarnings("unchecked")
    private @Nullable SingleNNAP postInitSingleNNAPFrom(int aType, Map<String, ?> aModelInfo) {
        Map<String, ?> tBasis = (Map<String, ?>)aModelInfo.get("basis");
        if (tBasis == null) return null;
        Object tBasisType = tBasis.get("type");
        // 目前只考虑 mirror 的情况
        if (!tBasisType.equals("mirror")) return null;
        Object tMirror = tBasis.get("mirror");
        if (tMirror == null) throw new IllegalArgumentException("Key `mirror` required for basis mirror");
        int tMirrorType = ((Number)tMirror).intValue();
        Basis[] aBasis = new Basis[mThreadNumber];
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
        tModel = aModelInfo.get("model");
        if (tModel != null) throw new IllegalArgumentException("model data in mirror ModelInfo MUST be empty");
        // 现在直接采用引用写法，因为不会存在同时调用的情况
        NeuralNetwork[] aNN = model(tMirrorType).mNN;
        return new SingleNNAP(aRefEng, aNormMu, aNormSigma, aNormMuEng, aNormSigmaEng, aBasis, aNN);
    }
    
    @SuppressWarnings("SameParameterValue")
    public class SingleNNAP {
        private final NeuralNetwork[] mNN;
        private final double mRefEng;
        private final IVector mNormMu, mNormSigma;
        private final double mNormMuEng, mNormSigmaEng;
        private final Basis[] mBasis;
        
        public double refEng() {return mRefEng;}
        public IVector normMu() {return mNormMu;}
        public IVector normSigma() {return mNormSigma;}
        public double normMuEng() {return mNormMuEng;}
        public double normSigmaEng() {return mNormSigmaEng;}
        public Basis basis() {return basis(0);}
        public Basis basis(int aThreadID) {return mBasis[aThreadID];}
        public NeuralNetwork nn() {return nn(0);}
        public NeuralNetwork nn(int aThreadID) {return mNN[aThreadID];}
        
        /// 现在使用全局的缓存实现，可以进一步减少内存池的调用操作
        private final Vector[] mFp, mFpGrad;
        private final DoubleList[] mFpPx, mFpPy, mFpPz;
        private final DoubleList[] mNlDx, mNlDy, mNlDz;
        private final IntList[] mNlType, mNlIdx;
        private DoubleList bufFpPx(int aThreadID, int aSizeMin) {
            DoubleList tFpPx = mFpPx[aThreadID];
            tFpPx.ensureCapacity(aSizeMin);
            tFpPx.setInternalDataSize(aSizeMin);
            return tFpPx;
        }
        private DoubleList bufFpPy(int aThreadID, int aSizeMin) {
            DoubleList tFpPy = mFpPy[aThreadID];
            tFpPy.ensureCapacity(aSizeMin);
            tFpPy.setInternalDataSize(aSizeMin);
            return tFpPy;
        }
        private DoubleList bufFpPz(int aThreadID, int aSizeMin) {
            DoubleList tFpPz = mFpPz[aThreadID];
            tFpPz.ensureCapacity(aSizeMin);
            tFpPz.setInternalDataSize(aSizeMin);
            return tFpPz;
        }
        
        private final DoubleList[] mForceX, mForceY, mForceZ;
        private DoubleList bufForceX(int aThreadID, int aSizeMin) {
            DoubleList tForceX = mForceX[aThreadID];
            tForceX.ensureCapacity(aSizeMin);
            tForceX.setInternalDataSize(aSizeMin);
            return tForceX;
        }
        private DoubleList bufForceY(int aThreadID, int aSizeMin) {
            DoubleList tForceY = mForceY[aThreadID];
            tForceY.ensureCapacity(aSizeMin);
            tForceY.setInternalDataSize(aSizeMin);
            return tForceY;
        }
        private DoubleList bufForceZ(int aThreadID, int aSizeMin) {
            DoubleList tForceZ = mForceZ[aThreadID];
            tForceZ.ensureCapacity(aSizeMin);
            tForceZ.setInternalDataSize(aSizeMin);
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
        
        private SingleNNAP(double aRefEng, IVector aNormMu, IVector aNormSigma, double aNormMuEng, double aNormSigmaEng, Basis[] aBasis, NeuralNetwork[] aNN) {
            mRefEng = aRefEng;
            mNormMu = aNormMu;
            mNormSigma = aNormSigma;
            mNormMuEng = aNormMuEng;
            mNormSigmaEng = aNormSigmaEng;
            mBasis = aBasis;
            mNN = aNN;
            int tBasisSize = mBasis[0].size();
            
            mFp = new Vector[mThreadNumber];
            mFpGrad = new Vector[mThreadNumber];
            mFpPx = new DoubleList[mThreadNumber];
            mFpPy = new DoubleList[mThreadNumber];
            mFpPz = new DoubleList[mThreadNumber];
            mNlDx = new DoubleList[mThreadNumber];
            mNlDy = new DoubleList[mThreadNumber];
            mNlDz = new DoubleList[mThreadNumber];
            mNlType = new IntList[mThreadNumber];
            mNlIdx = new IntList[mThreadNumber];
            mForceX = new DoubleList[mThreadNumber];
            mForceY = new DoubleList[mThreadNumber];
            mForceZ = new DoubleList[mThreadNumber];
            for (int i = 0; i < mThreadNumber; ++i) {
                mFp[i] = Vectors.zeros(tBasisSize);
                mFpGrad[i] = Vectors.zeros(tBasisSize);
                mFpPx[i] = new DoubleList(1024);
                mFpPy[i] = new DoubleList(1024);
                mFpPz[i] = new DoubleList(1024);
                mNlDx[i] = new DoubleList(16);
                mNlDy[i] = new DoubleList(16);
                mNlDz[i] = new DoubleList(16);
                mNlType[i] = new IntList(16);
                mNlIdx[i] = new IntList(16);
                mForceX[i] = new DoubleList(16);
                mForceY[i] = new DoubleList(16);
                mForceZ[i] = new DoubleList(16);
            }
        }
    }
    private boolean mIsTorch = false;
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
    public NNAP(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws Exception {
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
    public NNAP(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws Exception {
        this(aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath), aThreadNumber);
    }
    public NNAP(Map<?, ?> aModelInfo) throws Exception {this(aModelInfo, 1);}
    public NNAP(String aModelPath) throws Exception {this(aModelPath, 1);}
    
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        for (SingleNNAP tModel : mModels) {
            for (int i = 0; i < mThreadNumber; ++i) {
                tModel.basis(i).shutdown();
            }
            for (int i = 0; i < mThreadNumber; ++i) {
                tModel.mNN[i].shutdown();
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
    
    
    private void buildNL_(IDxyzTypeIdxIterable aNL, double aRCut, DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, IntList aNlIdx) {
        final int tTypeNum = atomTypeNumber();
        // 缓存情况需要先清空这些
        aNlDx.clear(); aNlDy.clear(); aNlDz.clear();
        aNlType.clear(); aNlIdx.clear();
        aNL.forEachDxyzTypeIdx(aRCut, (dx, dy, dz, type, idx) -> {
            // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查
            if (type > tTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+tTypeNum+")");
            // 简单缓存近邻列表
            aNlDx.add(dx); aNlDy.add(dy); aNlDz.add(dz);
            aNlType.add(type); aNlIdx.add(idx);
        });
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @param rEnergyAccumulator {@inheritDoc}
     */
    @Override public void calEnergy(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyAccumulator rEnergyAccumulator) throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        final int tThreadNum = threadNumber();
        aNeighborListGetter.forEachNLWithException(threadID -> {
            if (mIsTorch && tThreadNum>1) TorchModel.setSingleThread();
        }, null, (threadID, cIdx, cType, nl) -> {
            SingleNNAP tModel = model(cType);
            Basis tBasis = tModel.basis(threadID);
            NeuralNetwork tNN = tModel.nn(threadID);
            DoubleList tNlDx = tModel.mNlDx[threadID];
            DoubleList tNlDy = tModel.mNlDy[threadID];
            DoubleList tNlDz = tModel.mNlDz[threadID];
            IntList tNlType = tModel.mNlType[threadID];
            IntList tNlIdx = tModel.mNlIdx[threadID];
            buildNL_(nl, tBasis.rcut(), tNlDx, tNlDy, tNlDz, tNlType, tNlIdx);
            Vector tFp = tModel.mFp[threadID];
            tBasis.eval_(tNlDx, tNlDy, tNlDz, tNlType, tFp);
            tModel.normBasis(tFp);
            double tPred = tNN.forward(tFp);
            tPred = tModel.denormEng(tPred);
            tPred += tModel.mRefEng;
            rEnergyAccumulator.add(threadID, cIdx, -1, tPred);
        });
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @param rEnergyAccumulator {@inheritDoc}
     * @param rForceAccumulator {@inheritDoc}
     * @param rVirialAccumulator {@inheritDoc}
     */
    @Override public void calEnergyForceVirial(int aAtomNumber, INeighborListGetter aNeighborListGetter, @Nullable IEnergyAccumulator rEnergyAccumulator, @Nullable IForceAccumulator rForceAccumulator, @Nullable IVirialAccumulator rVirialAccumulator) throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        final int tThreadNum = threadNumber();
        aNeighborListGetter.forEachNLWithException(threadID -> {
            if (mIsTorch && tThreadNum>1) TorchModel.setSingleThread();
        }, null, (threadID, cIdx, cType, nl) -> {
            SingleNNAP tModel = model(cType);
            Basis tBasis = tModel.basis(threadID);
            final int tBasisSize = tBasis.size();
            NeuralNetwork tNN = tModel.nn(threadID);
            DoubleList tNlDx = tModel.mNlDx[threadID];
            DoubleList tNlDy = tModel.mNlDy[threadID];
            DoubleList tNlDz = tModel.mNlDz[threadID];
            IntList tNlType = tModel.mNlType[threadID];
            IntList tNlIdx = tModel.mNlIdx[threadID];
            buildNL_(nl, tBasis.rcut(), tNlDx, tNlDy, tNlDz, tNlType, tNlIdx);
            final int tNeiNum = tNlDx.size();
            Vector tFp = tModel.mFp[threadID];
            Vector tFpGrad = tModel.mFpGrad[threadID];
            DoubleList tFpPx = tModel.bufFpPx(threadID, tNeiNum*tBasisSize);
            DoubleList tFpPy = tModel.bufFpPy(threadID, tNeiNum*tBasisSize);
            DoubleList tFpPz = tModel.bufFpPz(threadID, tNeiNum*tBasisSize);
            tBasis.evalPartial_(tNlDx, tNlDy, tNlDz, tNlType, tFp, tFpPx, tFpPy, tFpPz);
            tModel.normBasis(tFp);
            double tPred = tNN.backward(tFp, tFpGrad);
            
            if (rEnergyAccumulator != null) {
                tPred = tModel.denormEng(tPred);
                tPred += tModel.mRefEng;
                rEnergyAccumulator.add(threadID, cIdx, -1, tPred);
            }
            tModel.normBasisPartial(tFpGrad);
            tModel.denormEngPartial(tFpGrad);
            DoubleList tForceX = tModel.bufForceX(threadID, tNeiNum);
            DoubleList tForceY = tModel.bufForceY(threadID, tNeiNum);
            DoubleList tForceZ = tModel.bufForceZ(threadID, tNeiNum);
            forceDot(tFpGrad, tFpPx, tFpPy, tFpPz, tForceX, tForceY, tForceZ, tNeiNum);
            // 累加交叉项到近邻
            for (int j = 0; j < tNeiNum; ++j) {
                double dx = tNlDx.get(j);
                double dy = tNlDy.get(j);
                double dz = tNlDz.get(j);
                int idx = tNlIdx.get(j);
                // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查
                double fx = tForceX.get(j);
                double fy = tForceY.get(j);
                double fz = tForceZ.get(j);
                if (rForceAccumulator != null) {
                    rForceAccumulator.add(threadID, cIdx, idx, fx, fy, fz);
                }
                if (rVirialAccumulator != null) {
                    rVirialAccumulator.add(threadID, cIdx, -1, dx*fx, dy*fy, dz*fz, dx*fy, dx*fz, dy*fz);
                }
            }
        });
    }
}
