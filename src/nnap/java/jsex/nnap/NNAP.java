package jsex.nnap;

import groovy.lang.DeprecationException;
import jse.atom.AbstractPairPotential;
import jse.atom.IAtomData;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.NewCollections;
import jse.code.timer.AccumulatedTimer;
import jse.gpu.*;
import jse.jit.IJITEngine;
import jse.jit.IJITMethod;
import jse.cptr.*;
import jse.math.vector.IVector;
import jse.math.vector.IntVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import jsex.nnap.basis.Basis;
import jsex.nnap.basis.MirrorBasis;
import jsex.nnap.nn.NeuralNetwork;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.util.*;
import java.util.function.IntUnaryOperator;
import java.util.regex.Pattern;

/**
 * jse 实现的 nnap，所有 nnap 相关能量和力的计算都在此实现，
 * 具体定义可以参考：
 * <a href="https://link.springer.com/article/10.1007/s40843-024-2953-9">
 * Efficient and accurate simulation of vitrification in multi-component metallic liquids with neural-network potentials </a>
 * <p>
 * 此类设计时确保不同对象之间线程安全，而不同线程访问相同的对象线程不安全
 * <p>
 * 尝试合并所有运算以方便后续 cuda 实现，现在改为运行时编译避免模板展开的问题
 *
 * @author liqa
 */
@ApiStatus.Experimental
public class NNAP extends AbstractPairPotential {
    public final static class Conf {
        /**
         * 自定义 nnap cuda 中使用的 block_size 值，这可能会影响速度；
         * 默认为 {@code 256}
         */
        public static int CUDA_BLOCKSIZE = OS.envI("JSE_NNAP_CUDA_BLOCKSIZE", 256);
        
        /**
         * 自定义构建 nnap 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_NNAP");
        /**
         * 自定义构建 nnap 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_CXX_COMPILER  = OS.env("JSE_CMAKE_CXX_COMPILER_NNAP", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS     = OS.env("JSE_CMAKE_CXX_FLAGS_NNAP"   , jse.code.Conf.CMAKE_CXX_FLAGS);
        public static @Nullable String CMAKE_CUDA_COMPILER = OS.env("JSE_CMAKE_CUDA_COMPILER_NNAP");
        public static @Nullable String CMAKE_CUDA_FLAGS    = OS.env("JSE_CMAKE_CUDA_FLAGS_NNAP");
        public static @Nullable String CMAKE_CUDA_ARCHITECTURES = OS.env("JSE_CMAKE_CUDA_ARCHITECTURES_NNAP");
        /**
         * 自定义构建 nnap 时的优化等级，
         * 默认会使用 BASE 优化
         */
        public static int OPTIM_LEVEL = OS.envI("JSE_NNAP_OPTIM_LEVEL", IJITEngine.OPTIM_BASE);
        /**
         * 设置 NNAP 内部计算的默认精度，默认为 {@code double}，
         * 仅对 cpu 情况有效
         */
        public static String PRECISION = OS.env("JSE_NNAP_PRECISION", "double");
    }
    public final static int VERSION = 6;
    
    final String[] mSymbols;
    final @Nullable String mUnits;
    final boolean mSingle, mCuda;
    final Basis[] mBasis;
    final NeuralNetwork[] mNN;
    final double mRCutMax;
    final int mNMergesMax;
    
    @Override public int ntypes() {return mSymbols.length;}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
    public String units() {return mUnits;}
    public String precision() {return mSingle ? "single" : "double";}
    
    // 现在所有数据都改为 c 指针，并统一使用 PointerManager 管理内存实现自动回收
    final PointerManager mPtrMngTot;
    final PointerManager[] mPtrMngPar; // 增加一个线程独立的避免线程竞争
    final IntCPointer mOutNums;
    final AnyCPointer mFpHyperParam, mFpParam, mNnParam, mNormParam;
    final IDoubleOrFloatCPointer[] mCache;
    private final IDoubleOrFloatCPointer[] mEng;
    private final IDoubleOrFloatCPointer[] mCNlDx, mCNlDy, mCNlDz, mCGradNlDx, mCGradNlDy, mCGradNlDz;
    private final IntCPointer[] mCNlType, mCNlIdx;
    
    final int mTotCParamSize, mTotGradCParamSize, mTotParamSize;
    final IDoubleOrFloatCPointer mTotCParam;
    IDoubleOrFloatCPointer[] mGradTotCParam = null;
    AnyCPointer[] mGradFpParam, mGradNnParam;
    final Vector mTotParam;
    Vector mGradTotParam = null;
    
    // cuda stuff
    private FloatCPointer mFltBuf = null;
    private FloatCudaPointer mCudaF = null, mCudaEatom0 = null, mCudaVatom0 = null, mCudaVatom1 = null;
    private IntCudaPointer mCudaMgNlSize = null, mCudaMgNlIdx = null;
    private FloatCudaPointer mCudaGradNlDx = null, mCudaGradNlDy = null, mCudaGradNlDz = null;
    private IntCudaPointer mCudaNMerges = null, mCudaLmpType2NNAPType = null;
    private CudaPointer mCudaMergeSorted = null, mCudaCutsq = null;
    private CudaPointer mCudaFpHyperParam = null, mCudaFpParam = null, mCudaNnParam = null, mCudaNormParam = null;
    private CudaNeighborListGetter mCudaNlGetter = null;
    
    @SuppressWarnings({"unchecked", "resource"})
    NNAP(@Nullable String aLibDir, @Nullable String aProjectName, Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aNumThreads, String aArch) throws Exception {
        super(aNumThreads);
        Number tVersion = (Number)aModelInfo.get("version");
        if (tVersion != null) {
            int tVersionValue = tVersion.intValue();
            if (tVersionValue > VERSION) throw new IllegalArgumentException("Unsupported version: " + tVersionValue);
        }
        mUnits = UT.Code.toString(aModelInfo.get("units"));
        if (aArch.equals("cpu")) {
            mCuda = false;
        } else
        if (aArch.equals("cuda")) {
            mCuda = true;
        } else {
            throw new IllegalArgumentException("NNAP architecture MUST be 'cpu' or 'cuda', input: " + aArch);
        }
        if (mCuda) {
            mSingle = true;
        } else {
            if (Conf.PRECISION.equals("single")) {
                mSingle = true;
            } else
            if (Conf.PRECISION.equals("double")) {
                mSingle = false;
            } else {
                throw new IllegalArgumentException("NNAP precision MUST be 'double' or 'single', input: " + Conf.PRECISION);
            }
        }
        List<? extends Map<String, ?>> tModels = (List<? extends Map<String, ?>>)aModelInfo.get("models");
        if (tModels == null) throw new IllegalArgumentException("No models in ModelInfo");
        int tModelSize = tModels.size();
        mSymbols = new String[tModelSize];
        for (int i = 0; i < tModelSize; ++i) {
            Object tSymbol = tModels.get(i).get("symbol");
            if (tSymbol == null) throw new IllegalArgumentException("No symbol in model");
            mSymbols[i] = tSymbol.toString();
        }
        mBasis = Basis.load(NewCollections.map(tModels, info -> {
            Object tBasisInfo = info.get("basis");
            return tBasisInfo!=null ? tBasisInfo : Maps.of("type", "spherical_chebyshev");
        }));
        mNN = NeuralNetwork.load(mBasis, NewCollections.map(tModels, info -> {
            Object tNNInfo = info.get("torch");
            if (tNNInfo != null) throw new IllegalArgumentException("torch model is invalid now.");
            return info.get("nn");
        }));
        // 常规参数缓存
        double tRCutMax = 0.0;
        int tNMergesMax = 0;
        for (Basis tBasis : mBasis) {
            tRCutMax = Math.max(tRCutMax, tBasis.rcutMax());
            tNMergesMax = Math.max(tNMergesMax, tBasis.mergeSize());
        }
        mRCutMax = tRCutMax;
        mNMergesMax = tNMergesMax;
        
        mNNAPGEN = new NNAPGEN(aLibDir, aProjectName, mBasis, mNN);
        // 初始化数组
        mPtrMngTot = new PointerManager();
        mPtrMngPar = new PointerManager[aNumThreads];
        for (int ti = 0; ti < aNumThreads; ++ti) {
            mPtrMngPar[ti] = new PointerManager();
        }
        mOutNums = mPtrMngTot.newIntCPointer(16);
        mCNlDx = new IDoubleOrFloatCPointer[aNumThreads];
        mCNlDy = new IDoubleOrFloatCPointer[aNumThreads];
        mCNlDz = new IDoubleOrFloatCPointer[aNumThreads];
        mCNlType = new IntCPointer[aNumThreads];
        mCNlIdx = new IntCPointer[aNumThreads];
        mCGradNlDx = new IDoubleOrFloatCPointer[aNumThreads];
        mCGradNlDy = new IDoubleOrFloatCPointer[aNumThreads];
        mCGradNlDz = new IDoubleOrFloatCPointer[aNumThreads];
        mEng = new IDoubleOrFloatCPointer[aNumThreads];
        mCache = new IDoubleOrFloatCPointer[aNumThreads];
        for (int ti = 0; ti < aNumThreads; ++ti) {
            mCNlDx[ti] = mPtrMngPar[ti].newDoubleOrFloatCPointer(mSingle);
            mCNlDy[ti] = mPtrMngPar[ti].newDoubleOrFloatCPointer(mSingle);
            mCNlDz[ti] = mPtrMngPar[ti].newDoubleOrFloatCPointer(mSingle);
            mCNlType[ti] = mPtrMngPar[ti].newIntCPointer();
            mCNlIdx[ti] = mPtrMngPar[ti].newIntCPointer();
            mCGradNlDx[ti] = mPtrMngPar[ti].newDoubleOrFloatCPointer(mSingle);
            mCGradNlDy[ti] = mPtrMngPar[ti].newDoubleOrFloatCPointer(mSingle);
            mCGradNlDz[ti] = mPtrMngPar[ti].newDoubleOrFloatCPointer(mSingle);
            mEng[ti] = mPtrMngPar[ti].newDoubleOrFloatCPointer(mSingle, 1);
            mCache[ti] = mPtrMngPar[ti].newDoubleOrFloatCPointer(mSingle);
        }
        // 初始化参数数组
        int tTotCParamSize = 0, tTotGradCParamSize = 0, tTotParamSize = 0;
        for (int i = 0; i < tModelSize; ++i) {
            tTotCParamSize += mBasis[i].cptrHyperParameterSize();
            tTotCParamSize += mBasis[i].cptrParameterSize();
            tTotCParamSize += mNN[i].cptrParameterSize();
            tTotCParamSize += mBasis[i].size()*2 + 2; // norm size
            
            tTotGradCParamSize += mBasis[i].cptrParameterSize();
            tTotGradCParamSize += mNN[i].cptrParameterSize();
            
            tTotParamSize += mBasis[i].parameterSize();
            tTotParamSize += mNN[i].parameterSize();
        }
        mTotCParamSize = tTotCParamSize;
        mTotGradCParamSize = tTotGradCParamSize;
        mTotParamSize = tTotParamSize;
        mTotCParam = mPtrMngTot.newDoubleOrFloatCPointer(mSingle, mTotCParamSize);
        mTotParam = Vectors.zeros(mTotParamSize);
        mFpHyperParam = mPtrMngTot.newAnyCPointer(tModelSize);
        mFpParam = mPtrMngTot.newAnyCPointer(tModelSize);
        mNnParam = mPtrMngTot.newAnyCPointer(tModelSize);
        IDoubleOrFloatCPointer tParam = mTotCParam.copy();
        int tShift = 0;
        for (int i = 0; i < tModelSize; ++i) {
            mFpParam.putAt(i, tParam);
            mBasis[i].mountCptrParameter(tParam);
            tParam.rightShift(mBasis[i].cptrParameterSize());
            mFpHyperParam.putAt(i, tParam);
            mBasis[i].mountCptrHyperParameter(tParam);
            tParam.rightShift(mBasis[i].cptrHyperParameterSize());
            
            int tSize = mBasis[i].parameterSize();
            mBasis[i].mountParameter(mTotParam.subVec(tShift, tShift+tSize));
            tShift += tSize;
        }
        for (int i = 0; i < tModelSize; ++i) {
            mNnParam.putAt(i, tParam);
            mNN[i].mountCptrParameter(tParam);
            tParam.rightShift(mNN[i].cptrParameterSize());
            
            int tSize = mNN[i].parameterSize();
            mNN[i].mountParameter(mTotParam.subVec(tShift, tShift+tSize));
            tShift += tSize;
        }
        // 归一化系数读取
        mNormParam = mPtrMngTot.newAnyCPointer(tModelSize);
        Number tNormSigmaEng = null, tNormMuEng = null;
        for (int i = 0; i < tModelSize; ++i) {
            if (tNormSigmaEng == null) tNormSigmaEng = (Number)tModels.get(i).get("norm_sigma_eng");
            if (tNormMuEng == null) tNormMuEng = (Number)tModels.get(i).get("norm_mu_eng");
        }
        double aNormSigmaEng = tNormSigmaEng==null ? 1.0 : tNormSigmaEng.doubleValue();
        double aNormMuEng = tNormMuEng==null ? 0.0 : tNormMuEng.doubleValue();
        for (int i = 0; i < tModelSize; ++i) {
            Map<String, ?> tModel = tModels.get(i);
            if (mBasis[i] instanceof MirrorBasis) {
                // mirror 会强制这些额外值缺省
                if (tModel.get("ref_eng") != null) {
                    throw new IllegalArgumentException("ref_eng in mirror_basis MUST be empty");
                }
                if (UT.Code.get(tModel, "norm_vec", "norm_sigma", "norm_mu") != null) {
                    throw new IllegalArgumentException("norm_vec/norm_sigma/norm_mu in mirror_basis MUST be empty");
                }
                // 读取 mirror 的属性
                tModel = tModels.get(((MirrorBasis)mBasis[i]).mirrorType()-1);
            }
            Number tRefEng = (Number)tModel.get("ref_eng");
            double aRefEng = tRefEng==null ? 0.0 : tRefEng.doubleValue();
            List<? extends Number> tNormSigma = (List<? extends Number>)UT.Code.get(tModel, "norm_sigma", "norm_vec");
            List<? extends Number> tNormMu = (List<? extends Number>)tModel.get("norm_mu");
            
            mNormParam.putAt(i, tParam);
            tParam.setD(aNormMuEng+aRefEng); tParam.next();
            tParam.setD(aNormSigmaEng); tParam.next();
            int tBasisSize = mBasis[i].size();
            for (int j = 0; j < tBasisSize; ++j) {
                tParam.putAtD(j, tNormMu==null ? 0.0 : tNormMu.get(j).doubleValue());
            }
            tParam.rightShift(tBasisSize);
            for (int j = 0; j < tBasisSize; ++j) {
                tParam.putAtD(j, tNormSigma==null ? 1.0 : tNormSigma.get(j).doubleValue());
            }
            tParam.rightShift(tBasisSize);
        }
        // 这里初始化 cuda 数据
        if (mCuda) {
            AnyCPointer tCudaFpHyperParam = AnyCPointer.calloc(tModelSize);
            AnyCPointer tCudaFpParam = AnyCPointer.calloc(tModelSize);
            AnyCPointer tCudaNnParam = AnyCPointer.calloc(tModelSize);
            AnyCPointer tCudaNormParam = AnyCPointer.calloc(tModelSize);
            mCudaFpHyperParam = mPtrMngTot.newCudaPointer(tModelSize*AnyCPointer.TYPE_SIZE);
            mCudaFpParam = mPtrMngTot.newCudaPointer(tModelSize*AnyCPointer.TYPE_SIZE);
            mCudaNnParam = mPtrMngTot.newCudaPointer(tModelSize*AnyCPointer.TYPE_SIZE);
            mCudaNormParam = mPtrMngTot.newCudaPointer(tModelSize*AnyCPointer.TYPE_SIZE);
            // 现在也改为单个指针，因此需要确保顺序和上面一致
            FloatCudaPointer tTotCudaParam = mPtrMngTot.newFloatCudaPointer(mTotCParamSize);
            tTotCudaParam.fill((FloatCPointer)mTotCParam, mTotCParamSize);
            FloatCudaPointer tCudaParam = tTotCudaParam.copy();
            for (int i = 0; i < tModelSize; ++i) {
                tCudaFpParam.putAt(i, tCudaParam);
                tCudaParam.rightShift(mBasis[i].cptrParameterSize());
                tCudaFpHyperParam.putAt(i, tCudaParam);
                tCudaParam.rightShift(mBasis[i].cptrHyperParameterSize());
            }
            for (int i = 0; i < tModelSize; ++i) {
                tCudaNnParam.putAt(i, tCudaParam);
                tCudaParam.rightShift(mNN[i].cptrParameterSize());
            }
            for (int i = 0; i < tModelSize; ++i) {
                tCudaNormParam.putAt(i, tCudaParam);
                tCudaParam.rightShift(mBasis[i].size()*2L + 2L);
            }
            mCudaFpHyperParam.memcpy2this(tCudaFpHyperParam, tModelSize*AnyCPointer.TYPE_SIZE);
            mCudaFpParam.memcpy2this(tCudaFpParam, tModelSize*AnyCPointer.TYPE_SIZE);
            mCudaNnParam.memcpy2this(tCudaNnParam, tModelSize*AnyCPointer.TYPE_SIZE);
            mCudaNormParam.memcpy2this(tCudaNormParam, tModelSize*AnyCPointer.TYPE_SIZE);
            tCudaFpHyperParam.free();
            tCudaFpParam.free();
            tCudaNnParam.free();
            tCudaNormParam.free();
            // GPU 部分特有的 merge cutoff 对应近邻特殊处理
            mCudaNMerges = mPtrMngTot.newIntCudaPointer(tModelSize);
            mCudaCutsq = mPtrMngTot.newCudaPointer(tModelSize*AnyCPointer.TYPE_SIZE);
            mCudaMergeSorted = mPtrMngTot.newCudaPointer(tModelSize*AnyCPointer.TYPE_SIZE);
            IntCPointer tNMerges = IntCPointer.calloc(tModelSize);
            AnyCPointer tCudaCutsq = AnyCPointer.calloc(tModelSize);
            AnyCPointer tCudaMergeSorted = AnyCPointer.calloc(tModelSize);
            for (int i = 0; i < tModelSize; ++i) {
                Basis tSubBasis = mBasis[i];
                int tSubNMerges = tSubBasis.mergeSize();
                tNMerges.putAt(i, tSubNMerges);
                FloatCudaPointer tSubCudaCutsq = mPtrMngTot.newFloatCudaPointer(tSubNMerges);
                FloatCPointer tSubCutsq = FloatCPointer.calloc(tSubNMerges);
                for (int k = 0; k < tSubNMerges; ++k) {
                    double tRCut = tSubBasis.rcut(k);
                    tSubCutsq.putAt(k, (float)(tRCut*tRCut));
                }
                tSubCudaCutsq.fill(tSubCutsq, tSubNMerges);
                tCudaCutsq.putAt(i, tSubCudaCutsq);
                tSubCutsq.free();
                // 选择排序获取排序的索引
                IntCudaPointer tSubCudaMergeSorted = mPtrMngTot.newIntCudaPointer(tSubNMerges);
                IntCPointer tSubMergeSorted = IntCPointer.calloc(tSubNMerges);
                for (int k = 0; k < tSubNMerges; ++k) {
                    tSubMergeSorted.putAt(k, k);
                }
                for (int ki = 0; ki < tSubNMerges-1; ++ki) {
                    int tMinIdx = ki;
                    double tMinValue = tSubBasis.rcut(tSubMergeSorted.getAt(tMinIdx));
                    for (int kj = ki+1; kj < tSubNMerges; ++kj) {
                        double tValue = tSubBasis.rcut(tSubMergeSorted.getAt(kj));
                        if (tValue < tMinValue) {
                            tMinIdx = kj;
                            tMinValue = tValue;
                        }
                    }
                    if (tMinIdx != ki) {
                        int tmp = tSubMergeSorted.getAt(ki);
                        tSubMergeSorted.putAt(ki, tMinIdx);
                        tSubMergeSorted.putAt(tMinIdx, tmp);
                    }
                }
                tSubCudaMergeSorted.fill(tSubMergeSorted, tSubNMerges);
                tCudaMergeSorted.putAt(i, tSubCudaMergeSorted);
                tSubMergeSorted.free();
            }
            mCudaNMerges.fill(tNMerges, tModelSize);
            mCudaCutsq.memcpy2this(tCudaCutsq, tModelSize*AnyCPointer.TYPE_SIZE);
            mCudaMergeSorted.memcpy2this(tCudaMergeSorted, tModelSize*AnyCPointer.TYPE_SIZE);
            tNMerges.free();
            tCudaCutsq.free();
            tCudaMergeSorted.free();
        }
    }
    public NNAP(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aNumThreads) throws Exception {
        this(null, null, aModelInfo, aNumThreads, "cpu");
        // 直接开始 jit 编译
        compileJIT_();
    }
    public NNAP(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aNumThreads) throws Exception {
        this(IO.toParentPath(aModelPath), toValidProjectName(IO.toFileName(aModelPath)),
             aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath), aNumThreads, "cpu");
        // 直接开始 jit 编译
        compileJIT_();
    }
    public NNAP(Map<?, ?> aModelInfo) throws Exception {this(aModelInfo, 1);}
    public NNAP(String aModelPath) throws Exception {this(aModelPath, 1);}
    
    NNAP(Map<?, ?> aModelInfo, String aArch) throws Exception {
        this(null, null, aModelInfo, 1, aArch);
        // 直接开始 jit 编译
        compileJIT_();
    }
    NNAP(String aModelPath, String aArch) throws Exception {
        this(IO.toParentPath(aModelPath), toValidProjectName(IO.toFileName(aModelPath)),
             aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath), 1, aArch);
        // 直接开始 jit 编译
        compileJIT_();
    }
    
    private final static Pattern PROJECT_INVALID_NAME = Pattern.compile("[^a-zA-Z0-9_\\-]");
    static String toValidProjectName(String aProjectName) {
        aProjectName = aProjectName.replace(".yaml", "").replace(".yml", "").replace(".json", "").replace(".jnn", "").replace(".nn", "");
        aProjectName = PROJECT_INVALID_NAME.matcher(aProjectName).replaceAll("");
        return aProjectName.isEmpty() ? null : aProjectName;
    }
    
    // jit stuffs
    final NNAPGEN mNNAPGEN;
    IJITEngine mJITEngine = null;
    private IJITMethod mCalFp = null, mCalEnergy = null, mCalEnergyForce = null;
    private IJITMethod mStatNlSizeLammps = null, mComputeLammps = null;
    private IJITMethod mForwardEnergy = null, mBackwardEnergy = null;
    private IJITMethod mForwardEnergyForce = null, mBackwardEnergyForce = null;
    private IJITMethod mForwardForceCollect = null, mBackwardForceCollect = null;
    // cuda stuff
    private IJITMethod mCuda2Lammps = null, mComputeLammpsCuda = null;
    private IJITMethod mComputeGPUMD = null;
    
    private void compileJIT_() throws Exception {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mJITEngine!=null) throw new IllegalStateException("compileJIT() has already been called");
        // 开始 jit
        if (mCuda) {
            mJITEngine = mNNAPGEN.initEngineCuda(mSingle);
            mJITEngine.compile();
            mCuda2Lammps = mJITEngine.findMethod("jse_nnap_cuda2lammps");
            mComputeLammpsCuda = mJITEngine.findMethod("jse_nnap_computeLammpsCuda");
            mComputeGPUMD = mJITEngine.findMethod("jse_nnap_computeGPUMD");
        } else {
            mJITEngine = mNNAPGEN.initEngine(mSingle);
            mJITEngine.compile();
            mCalFp = mJITEngine.findMethod("jse_nnap_calFp");
            mCalEnergy = mJITEngine.findMethod("jse_nnap_calEnergy");
            mCalEnergyForce = mJITEngine.findMethod("jse_nnap_calEnergyForce");
            mStatNlSizeLammps = mJITEngine.findMethod("jse_nnap_statNlSizeLammps");
            mComputeLammps = mJITEngine.findMethod("jse_nnap_computeLammps");
            mForwardEnergy = mJITEngine.findMethod("jse_nnap_forwardEnergy");
            mBackwardEnergy = mJITEngine.findMethod("jse_nnap_backwardEnergy");
            mForwardEnergyForce = mJITEngine.findMethod("jse_nnap_forwardEnergyForce");
            mBackwardEnergyForce = mJITEngine.findMethod("jse_nnap_backwardEnergyForce");
            mForwardForceCollect = mJITEngine.findMethod("jse_nnap_forwardForceCollect");
            mBackwardForceCollect = mJITEngine.findMethod("jse_nnap_backwardForceCollect");
        }
    }
    
    @Override public void close() throws Exception {
        if (isClosed()) return;
        super.close();
        // 只需手动释放 mPtrMng 即可
        mPtrMngTot.close();
        final int tNumThreads = nthreads();
        for (int ti = 0; ti < tNumThreads; ++ti) {
            mPtrMngPar[ti].close();
        }
        if (mJITEngine!=null) mJITEngine.close();
        if (mCudaNlGetter!=null) mCudaNlGetter.close();
    }
    
    @Override public double rcutMax() {
        return mRCutMax;
    }
    @Override public double rcut(int aType) {
        return mBasis[aType-1].rcutMax();
    }
    @Override public boolean typewiseCutoff() {
        return true;
    }
    public double fpSize(int aType) {
        return mBasis[aType-1].size();
    }
    
    
    @ApiStatus.Experimental @Override
    public double calEnergySingle(int aThreadID, int aCType,
                                  Vector aNlDx, Vector aNlDy, Vector aNlDz, IntVector aNlType) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        checkType(aCType);
        PointerManager tPtrMng = mPtrMngPar[aThreadID];
        IDoubleOrFloatCPointer tCNlDx = mCNlDx[aThreadID], tCNlDy = mCNlDy[aThreadID], tCNlDz = mCNlDz[aThreadID];
        IntCPointer tCNlType = mCNlType[aThreadID];
        int tNlSize = aNlDx.size();
        tPtrMng.ensureCapacity(tCNlDx, tNlSize); tCNlDx.fillD(aNlDx);
        tPtrMng.ensureCapacity(tCNlDy, tNlSize); tCNlDy.fillD(aNlDy);
        tPtrMng.ensureCapacity(tCNlDz, tNlSize); tCNlDz.fillD(aNlDz);
        tPtrMng.ensureCapacity(tCNlType, tNlSize); tCNlType.fill(aNlType);
        return calEnergySingle(
            aThreadID, aCType, tNlSize,
            tCNlDx, tCNlDy, tCNlDz, tCNlType
        );
    }
    @ApiStatus.Experimental @Override
    public double calEnergyForceSingle(int aThreadID, int aCType,
                                       Vector aNlDx, Vector aNlDy, Vector aNlDz, IntVector aNlType,
                                       Vector rGradNlDx, Vector rGradNlDy, Vector rGradNlDz) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        checkType(aCType);
        PointerManager tPtrMng = mPtrMngPar[aThreadID];
        IDoubleOrFloatCPointer tCNlDx = mCNlDx[aThreadID], tCNlDy = mCNlDy[aThreadID], tCNlDz = mCNlDz[aThreadID];
        IDoubleOrFloatCPointer rCGradNlDx = mCGradNlDx[aThreadID], rCGradNlDy = mCGradNlDy[aThreadID], rCGradNlDz = mCGradNlDz[aThreadID];
        IntCPointer tCNlType = mCNlType[aThreadID];
        int tNlSize = aNlDx.size();
        tPtrMng.ensureCapacity(tCNlDx, tNlSize); tCNlDx.fillD(aNlDx);
        tPtrMng.ensureCapacity(tCNlDy, tNlSize); tCNlDy.fillD(aNlDy);
        tPtrMng.ensureCapacity(tCNlDz, tNlSize); tCNlDz.fillD(aNlDz);
        tPtrMng.ensureCapacity(tCNlType, tNlSize); tCNlType.fill(aNlType);
        tPtrMng.ensureCapacity(rCGradNlDx, tNlSize);
        tPtrMng.ensureCapacity(rCGradNlDy, tNlSize);
        tPtrMng.ensureCapacity(rCGradNlDz, tNlSize);
        double tEng = calEnergyForceSingle(
            aThreadID, aCType, tNlSize,
            tCNlDx, tCNlDy, tCNlDz, tCNlType,
            rCGradNlDx, rCGradNlDy, rCGradNlDz
        );
        rCGradNlDx.parse2destD(rGradNlDx);
        rCGradNlDy.parse2destD(rGradNlDy);
        rCGradNlDz.parse2destD(rGradNlDz);
        return tEng;
    }
    @ApiStatus.Experimental
    public Vector calFpSingle(int aThreadID, int aCType,
                              Vector aNlDx, Vector aNlDy, Vector aNlDz, IntVector aNlType) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        checkType(aCType);
        PointerManager tPtrMng = mPtrMngPar[aThreadID];
        IDoubleOrFloatCPointer tCNlDx = mCNlDx[aThreadID], tCNlDy = mCNlDy[aThreadID], tCNlDz = mCNlDz[aThreadID];
        IntCPointer tCNlType = mCNlType[aThreadID];
        int tNlSize = aNlDx.size();
        tPtrMng.ensureCapacity(tCNlDx, tNlSize); tCNlDx.fillD(aNlDx);
        tPtrMng.ensureCapacity(tCNlDy, tNlSize); tCNlDy.fillD(aNlDy);
        tPtrMng.ensureCapacity(tCNlDz, tNlSize); tCNlDz.fillD(aNlDz);
        tPtrMng.ensureCapacity(tCNlType, tNlSize); tCNlType.fill(aNlType);
        int tFpSize = mBasis[aCType-1].size();
        IDoubleOrFloatCPointer rFpPtr = mCache[aThreadID];
        mPtrMngPar[aThreadID].ensureCapacity(rFpPtr, tFpSize);
        calFpSingle(
            aThreadID, aCType, tNlSize,
            tCNlDx, tCNlDy, tCNlDz, tCNlType,
            rFpPtr
        );
        Vector rFp = Vectors.zeros(tFpSize);
        rFpPtr.parse2destD(rFp);
        return rFp;
    }
    
    /**
     * 简单遍历计算给定原子数据所有基组的实现，此实现适合对相同基组计算大量的原子结构；
     * 由于基组存储了元素排序，因此可以自动修正多个原子结构中元素排序不一致的问题
     * @param aAtomData 原子结构数据
     * @return 原子描述符向量组成的列表
     */
    public final List<Vector> calFp(IAtomData aAtomData) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mCuda) throw new IllegalStateException();
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        int tNumAtoms = aAtomData.natoms();
        List<Vector> rFps = NewCollections.nulls(tNumAtoms);
        mNl.setData(aAtomData).setRCut(rcutMax()).build();
        mPool.parfor(tNumAtoms, (i, threadID) -> {
            final int ctype = tTypeMap.applyAsInt(mNl.typeAt(i));
            checkType(ctype);
            initBufNl(threadID, i, false);
            Vector tFp = calFpSingle(
                threadID, ctype,
                mNlDxPar[threadID], mNlDyPar[threadID], mNlDzPar[threadID], mNlTypePar[threadID]
            );
            rFps.set(i, tFp);
        });
        return rFps;
    }
    
    public IVector parameters() {
        return mTotParam;
    }
    public IVector gradParameters() {
        if (mGradTotParam == null) throw new IllegalStateException("No grad in NNAP, invoke `requireGrad()` first.");
        return mGradTotParam;
    }
    public void initParameters() {
        int tModelSize = mSymbols.length;
        for (int i = 0; i < tModelSize; ++i) {
            mBasis[i].initParameters();
        }
        for (int i = 0; i < tModelSize; ++i) {
            mNN[i].initParameters();
        }
    }
    public void updateParameters() {
        int tModelSize = mSymbols.length;
        for (int i = 0; i < tModelSize; ++i) {
            mBasis[i].updateParameters();
        }
        for (int i = 0; i < tModelSize; ++i) {
            mNN[i].updateParameters();
        }
    }
    public void backwardParameter() {
        if (mGradTotParam == null) throw new IllegalStateException("No grad in NNAP, invoke `requireGrad()` first.");
        int tModelSize = mSymbols.length;
        for (int i = 0; i < tModelSize; ++i) {
            mBasis[i].backwardParameter();
        }
        for (int i = 0; i < tModelSize; ++i) {
            mNN[i].backwardParameter();
        }
    }
    public void requireGrad() {
        if (mGradTotParam!=null) return;
        int tModelSize = mSymbols.length;
        final int tNumThreads = nthreads();
        mGradTotCParam = new IDoubleOrFloatCPointer[tNumThreads];
        mGradFpParam = new AnyCPointer[tNumThreads];
        mGradNnParam = new AnyCPointer[tNumThreads];
        for (int i = 0; i < tModelSize; ++i) {
            mBasis[i].requireGrad(tNumThreads);
            mNN[i].requireGrad(tNumThreads);
        }
        mGradTotParam = Vectors.zeros(mTotParamSize);
        int tShift = 0;
        for (int i = 0; i < tModelSize; ++i) {
            int tSize = mBasis[i].parameterSize();
            mBasis[i].mountGradParameter(mGradTotParam.subVec(tShift, tShift+tSize));
            tShift += tSize;
        }
        for (int i = 0; i < tModelSize; ++i) {
            int tSize = mNN[i].parameterSize();
            mNN[i].mountGradParameter(mGradTotParam.subVec(tShift, tShift+tSize));
            tShift += tSize;
        }
        for (int ti = 0; ti < tNumThreads; ++ti) {
            IDoubleOrFloatCPointer tGradTotCParam = mPtrMngPar[ti].newDoubleOrFloatCPointer(mSingle, mTotGradCParamSize);
            AnyCPointer tGradFpParam = mPtrMngPar[ti].newAnyCPointer(tModelSize);
            AnyCPointer tGradNnParam = mPtrMngPar[ti].newAnyCPointer(tModelSize);
            IDoubleOrFloatCPointer tGradParam = tGradTotCParam.copy();
            for (int i = 0; i < tModelSize; ++i) {
                tGradFpParam.putAt(i, tGradParam);
                mBasis[i].mountGradCptrParameter(ti, tGradParam);
                tGradParam.rightShift(mBasis[i].cptrParameterSize());
            }
            for (int i = 0; i < tModelSize; ++i) {
                tGradNnParam.putAt(i, tGradParam);
                mNN[i].mountGradCptrParameter(ti, tGradParam);
                tGradParam.rightShift(mNN[i].cptrParameterSize());
            }
            mGradTotCParam[ti] = tGradTotCParam;
            mGradFpParam[ti] = tGradFpParam;
            mGradNnParam[ti] = tGradNnParam;
        }
    }
    public void zeroGrad() {
        if (mGradTotParam == null) throw new IllegalStateException("No grad in NNAP, invoke `requireGrad()` first.");
        final int tNumThreads = nthreads();
        for (int ti = 0; ti < tNumThreads; ++ti) {
            mGradTotCParam[ti].fillD(0.0, mTotGradCParamSize);
            mGradTotParam.fill(0.0);
        }
    }
    public double normMuEng(int aType) {
        IDoubleOrFloatCPointer tParam = mNormParam.getAsDoubleOrFloatCPointerAt(mSingle, aType-1);
        return tParam.getD();
    }
    public void setNormMuEng(int aType, double aValue) {
        IDoubleOrFloatCPointer tParam = mNormParam.getAsDoubleOrFloatCPointerAt(mSingle, aType-1);
        tParam.setD(aValue);
    }
    public double normSigmaEng(int aType) {
        IDoubleOrFloatCPointer tParam = mNormParam.getAsDoubleOrFloatCPointerAt(mSingle, aType-1);
        return tParam.getAtD(1);
    }
    public void setNormSigmaEng(int aType, double aValue) {
        IDoubleOrFloatCPointer tParam = mNormParam.getAsDoubleOrFloatCPointerAt(mSingle, aType-1);
        tParam.putAtD(1, aValue);
    }
    public IDoubleOrFloatCPointer normMu(int aType) {
        IDoubleOrFloatCPointer tParam = mNormParam.getAsDoubleOrFloatCPointerAt(mSingle, aType-1);
        tParam.rightShift(2);
        return tParam;
    }
    public IDoubleOrFloatCPointer normSigma(int aType) {
        IDoubleOrFloatCPointer tParam = mNormParam.getAsDoubleOrFloatCPointerAt(mSingle, aType-1);
        tParam.rightShift(2+mBasis[aType-1].size());
        return tParam;
    }
    
    
    /// jit stuffs
    public void calFpSingle(int aThreadID, int aCType, int aNlSize,
                            IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz, IntCPointer aNlType,
                            IDoubleOrFloatCPointer rFp) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mCuda) throw new IllegalStateException();
        // 调用 jit 方法获取结果
        int tCode = mCalFp.invoke(aCType,
            aNlSize, aNlDx, aNlDy, aNlDz, aNlType,
            rFp, mFpHyperParam, mFpParam
        );
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
    }
    public double calEnergySingle(int aThreadID, int aCType, int aNlSize,
                                  IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz, IntCPointer aNlType) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mCuda) throw new IllegalStateException();
        IDoubleOrFloatCPointer tEng = mEng[aThreadID];
        // 调用 jit 方法获取结果
        int tCode = mCalEnergy.invoke(aCType,
            aNlSize, aNlDx, aNlDy, aNlDz, aNlType,
            tEng,
            mFpHyperParam, mFpParam, mNnParam, mNormParam
        );
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
        return tEng.getD();
    }
    public double calEnergyForceSingle(int aThreadID, int aCType, int aNlSize,
                                       IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz, IntCPointer aNlType,
                                       IDoubleOrFloatCPointer rGradNlDx, IDoubleOrFloatCPointer rGradNlDy, IDoubleOrFloatCPointer rGradNlDz) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mCuda) throw new IllegalStateException();
        IDoubleOrFloatCPointer tEng = mEng[aThreadID];
        IDoubleOrFloatCPointer tFpForwardCache = mCache[aThreadID];
        mPtrMngPar[aThreadID].ensureCapacity(tFpForwardCache, mBasis[aCType-1].forwardCacheSize(aNlSize));
        // 调用 jit 方法获取结果
        int tCode = mCalEnergyForce.invoke(aCType,
            aNlSize, aNlDx, aNlDy, aNlDz, aNlType,
            rGradNlDx, rGradNlDy, rGradNlDz, tEng,
            mFpHyperParam, mFpParam, mNnParam, mNormParam,
            tFpForwardCache
        );
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
        return tEng.getD();
    }
    
    public int forwardEnergyCacheSize(int aNlSize, int aCType) {
        return mBasis[aCType-1].forwardCacheSize(aNlSize) + mNN[aCType-1].forwardCacheSize();
    }
    public double forwardEnergy(int aThreadID, int aCType, int aNlSize,
                                IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz, IntCPointer aNlType,
                                IDoubleOrFloatCPointer rCaches) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mCuda) throw new IllegalStateException();
        IDoubleOrFloatCPointer tEng = mEng[aThreadID];
        // 调用 jit 方法获取结果
        int tCode = mForwardEnergy.invoke(aCType,
            aNlSize, aNlDx, aNlDy, aNlDz, aNlType,
            tEng,
            mFpHyperParam, mFpParam, mNnParam, mNormParam,
            rCaches, rCaches.plus(mBasis[aCType-1].forwardCacheSize(aNlSize))
        );
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
        return tEng.getD();
    }
    public void backwardEnergy(int aThreadID, int aCType, int aNlSize,
                               IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz, IntCPointer aNlType,
                               double aGradEng, IDoubleOrFloatCPointer aCaches) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mCuda) throw new IllegalStateException();
        if (mGradTotParam == null) throw new IllegalStateException("No grad in NNAP, invoke `requireGrad()` first.");
        // 调用 jit 方法获取结果
        int tCode = mBackwardEnergy.invoke(aCType,
            aNlSize, aNlDx, aNlDy, aNlDz, aNlType,
            aGradEng,
            mFpHyperParam, mFpParam, mNnParam, mNormParam,
            mGradFpParam[aThreadID], mGradNnParam[aThreadID],
            aCaches, aCaches.plus(mBasis[aCType-1].forwardCacheSize(aNlSize))
        );
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
    }
    
    public int forwardEnergyForceCacheSize(int aNlSize, int aCType) {
        return mBasis[aCType-1].forwardCacheSize(aNlSize)  + mNN[aCType-1].forwardCacheSize()
             + mBasis[aCType-1].backwardCacheSize(aNlSize) + mNN[aCType-1].backwardCacheSize();
    }
    public double forwardEnergyForce(int aThreadID, int aCType, int aNlSize,
                                     IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz, IntCPointer aNlType,
                                     IDoubleOrFloatCPointer rAGradNlDx, IDoubleOrFloatCPointer rAGradNlDy, IDoubleOrFloatCPointer rAGradNlDz,
                                     IDoubleOrFloatCPointer rCaches) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mCuda) throw new IllegalStateException();
        IDoubleOrFloatCPointer tEng = mEng[aThreadID];
        int tSizeFpForwardCache = mBasis[aCType-1].forwardCacheSize(aNlSize);
        int tSizeNnForwardCache = mNN[aCType-1].forwardCacheSize();
        int tSizeFpBackwardCache = mBasis[aCType-1].backwardCacheSize(aNlSize);
        // 调用 jit 方法获取结果
        int tCode = mForwardEnergyForce.invoke(aCType,
            aNlSize, aNlDx, aNlDy, aNlDz, aNlType,
            rAGradNlDx, rAGradNlDy, rAGradNlDz, tEng,
            mFpHyperParam, mFpParam, mNnParam, mNormParam,
            rCaches, rCaches.plus(tSizeFpForwardCache),
            rCaches.plus(tSizeFpForwardCache+tSizeNnForwardCache),
            rCaches.plus(tSizeFpForwardCache+tSizeNnForwardCache+tSizeFpBackwardCache)
        );
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
        return tEng.getD();
    }
    public void backwardEnergyForce(int aThreadID, int aCType, int aNlSize,
                                    IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz, IntCPointer aNlType,
                                    IDoubleOrFloatCPointer aBGradAGradNlDx, IDoubleOrFloatCPointer aBGradAGradNlDy, IDoubleOrFloatCPointer aBGradAGradNlDz,
                                    double aBGradEng, IDoubleOrFloatCPointer aCaches) {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mCuda) throw new IllegalStateException();
        if (mGradTotParam == null) throw new IllegalStateException("No grad in NNAP, invoke `requireGrad()` first.");
        IDoubleOrFloatCPointer tFpBackwardBackwardCache = mCache[aThreadID];
        mPtrMngPar[aThreadID].ensureCapacity(tFpBackwardBackwardCache, mBasis[aCType-1].backwardBackwardCacheSize(aNlSize));
        int tSizeFpForwardCache = mBasis[aCType-1].forwardCacheSize(aNlSize);
        int tSizeNnForwardCache = mNN[aCType-1].forwardCacheSize();
        int tSizeFpBackwardCache = mBasis[aCType-1].backwardCacheSize(aNlSize);
        // 调用 jit 方法获取结果
        int tCode = mBackwardEnergyForce.invoke(aCType,
            aNlSize, aNlDx, aNlDy, aNlDz, aNlType,
            aBGradAGradNlDx, aBGradAGradNlDy, aBGradAGradNlDz, aBGradEng,
            mFpHyperParam, mFpParam, mNnParam, mNormParam,
            mGradFpParam[aThreadID], mGradNnParam[aThreadID],
            aCaches, aCaches.plus(tSizeFpForwardCache),
            aCaches.plus(tSizeFpForwardCache+tSizeNnForwardCache),
            aCaches.plus(tSizeFpForwardCache+tSizeNnForwardCache+tSizeFpBackwardCache),
            tFpBackwardBackwardCache
        );
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
    }
    public void forwardForceCollect(int i, int aNlSize,
                                    IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz, IntCPointer aNlIdx,
                                    IDoubleOrFloatCPointer aAGradNlDx, IDoubleOrFloatCPointer aAGradNlDy, IDoubleOrFloatCPointer aAGradNlDz,
                                    IDoubleOrFloatCPointer rFx, IDoubleOrFloatCPointer rFy, IDoubleOrFloatCPointer rFz, IDoubleOrFloatCPointer rV) {
        // 调用 jit 方法获取结果
        int tCode = mForwardForceCollect.invoke(
            i, aNlSize,
            aNlDx, aNlDy, aNlDz, aNlIdx,
            aAGradNlDx, aAGradNlDy, aAGradNlDz,
            rFx, rFy, rFz, rV
        );
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
    }
    public void backwardForceCollect(int i, int aNlSize,
                                     IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz, IntCPointer aNlIdx,
                                     IDoubleOrFloatCPointer rBGradAGradNlDx, IDoubleOrFloatCPointer rBGradAGradNlDy, IDoubleOrFloatCPointer rBGradAGradNlDz,
                                     IDoubleOrFloatCPointer aBGradFx, IDoubleOrFloatCPointer aBGradFy, IDoubleOrFloatCPointer aBGradFz, IDoubleOrFloatCPointer aBGradV) {
        // 调用 jit 方法获取结果
        int tCode = mBackwardForceCollect.invoke(
            i, aNlSize,
            aNlDx, aNlDy, aNlDz, aNlIdx,
            rBGradAGradNlDx, rBGradAGradNlDy, rBGradAGradNlDz,
            aBGradFx, aBGradFy, aBGradFz, aBGradV
        );
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
    }
    
    /// lammps stuff
    private void validNlLammps_(int aNlSize) {
        mPtrMngPar[0].ensureCapacity(mCNlDx[0], aNlSize);
        mPtrMngPar[0].ensureCapacity(mCNlDy[0], aNlSize);
        mPtrMngPar[0].ensureCapacity(mCNlDz[0], aNlSize);
        mPtrMngPar[0].ensureCapacity(mCNlType[0], aNlSize);
        mPtrMngPar[0].ensureCapacity(mCNlIdx[0], aNlSize);
        mPtrMngPar[0].ensureCapacity(mCGradNlDx[0], aNlSize);
        mPtrMngPar[0].ensureCapacity(mCGradNlDy[0], aNlSize);
        mPtrMngPar[0].ensureCapacity(mCGradNlDz[0], aNlSize);
        for (int i = 0; i < mSymbols.length; ++i) {
            mPtrMngPar[0].ensureCapacity(mCache[0], mBasis[i].forwardCacheSize(aNlSize));
        }
    }
    void computeLammps(PairNNAP aPair) throws Exception {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (mCuda) throw new IllegalStateException();
        final int inum = aPair.listInum();
        // 种类的缓存优化
        for (int type = 1; type <= aPair.mNumTypes; ++type) {
            IntCPointer tList = aPair.getTypeIlistBuf(type, inum);
            aPair.mTypeIlist.putAt(type, tList);
        }
        // 近邻列表大小获取和缓存合理化
        IntCPointer ilist = aPair.listIlist();
        IntCPointer numneigh = aPair.listNumneigh();
        mStatNlSizeLammps.invoke(inum, ilist, numneigh, mOutNums);
        validNlLammps_(mOutNums.getAt(0));
        
        // 调用 jit 方法计算
        int tCode = mComputeLammps.invoke(
            inum, aPair.mNumTypes, aPair.eflagEither()?1:0, aPair.eflagAtom()?1:0, aPair.vflagEither()?1:0, aPair.vflagAtom()?1:0, aPair.cvflagAtom()?1:0,
            aPair.atomX(), aPair.atomF(), aPair.atomType(), ilist,
            numneigh, aPair.listFirstneigh(), aPair.mCutsq,
            aPair.mLmpType2NNAPType, aPair.mTypeIlist, aPair.mTypeInum,
            aPair.engVdwl(), aPair.eatom(), aPair.virial(), aPair.vatom(), aPair.cvatom(),
            mCNlDx[0], mCNlDy[0], mCNlDz[0], mCNlType[0], mCNlIdx[0],
            mCGradNlDx[0], mCGradNlDy[0], mCGradNlDz[0],
            mFpHyperParam, mFpParam, mNnParam, mNormParam,
            mCache[0]
        );
        if (tCode>0) throw new IllegalStateException("Exit code: "+tCode);
    }
    
    // cuda stuff
    private final AccumulatedTimer mCudaCopyTimer = new AccumulatedTimer(), mCudaComputeTimer = new AccumulatedTimer();
    public double cudaNlTime() {
        return mCudaNlGetter.nlTime() + mCudaNlGetter.cellTime();
    }
    public double cudaCopyTime() {
        return mCudaCopyTimer.get() + mCudaNlGetter.copyTime();
    }
    public double cudaComputeTime() {
        return mCudaComputeTimer.get();
    }
    public void resetCudaTimer() {
        mCudaCopyTimer.reset();
        mCudaComputeTimer.reset();
        mCudaNlGetter.resetTimer();
    }
    private boolean mCudaLmpInited = false;
    private void initLmpDataCuda_(PairNNAP aPair) throws CudaException {
        if (mCudaLmpInited) return;
        mCudaLmpInited = true;
        
        mFltBuf = mPtrMngTot.newFloatCPointer();
        mCudaF = mPtrMngTot.newFloatCudaPointer();
        mCudaEatom0 = mPtrMngTot.newFloatCudaPointer();
        mCudaVatom0 = mPtrMngTot.newFloatCudaPointer();
        mCudaVatom1 = mPtrMngTot.newFloatCudaPointer();
        mCudaMgNlSize = mPtrMngTot.newIntCudaPointer();
        mCudaMgNlIdx = mPtrMngTot.newIntCudaPointer();
        mCudaGradNlDx = mPtrMngTot.newFloatCudaPointer();
        mCudaGradNlDy = mPtrMngTot.newFloatCudaPointer();
        mCudaGradNlDz = mPtrMngTot.newFloatCudaPointer();
        
        mCudaNlGetter = new CudaNeighborListGetter(mRCutMax, true); // 总是重新调整原子种类顺序来进行优化
        mCudaLmpType2NNAPType = mPtrMngTot.newIntCudaPointer(aPair.mNumTypes+1);
        mCudaLmpType2NNAPType.fill(aPair.mLmpType2NNAPType, aPair.mNumTypes+1);
    }
    void computeLammpsCuda(PairNNAP aPair) throws CudaException {
        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
        if (!mCuda) throw new IllegalStateException();
        initLmpDataCuda_(aPair);
        // 常规缓存向量长度规范
        final int nlocal = aPair.atomNlocal();
        final int nghost = aPair.atomNghost();
        final int nlocalghost = nlocal + nghost;
        mPtrMngTot.ensureCapacity(mFltBuf, (long)nlocalghost*9L);
        mPtrMngTot.ensureCapacity(mCudaF, (long)nlocalghost*3L);
        mPtrMngTot.ensureCapacity(mCudaEatom0, (long)nlocal);
        mPtrMngTot.ensureCapacity(mCudaVatom0, (long)nlocal*6L);
        mPtrMngTot.ensureCapacity(mCudaVatom1, (long)nlocalghost*9L);
        mPtrMngTot.ensureCapacity(mCudaMgNlSize, (long)nlocal*(mNMergesMax+1));
        // GPU 近邻列表构建
        mCudaNlGetter.build(aPair);
        // 近邻列表缓存向量长度规范
        final int tTotNlSize = nlocal*mCudaNlGetter.nlMax();
        mPtrMngTot.ensureCapacity(mCudaMgNlIdx, tTotNlSize);
        mPtrMngTot.ensureCapacity(mCudaGradNlDx, tTotNlSize);
        mPtrMngTot.ensureCapacity(mCudaGradNlDy, tTotNlSize);
        mPtrMngTot.ensureCapacity(mCudaGradNlDz, tTotNlSize);
        
        // cuda compute
        mCudaComputeTimer.from();
        final boolean eflagEither = aPair.eflagEither();
        final boolean vflagEither = aPair.vflagEither();
        final boolean vflagAtom = aPair.vflagAtom();
        final boolean cvflagAtom = aPair.cvflagAtom();
        int tCode = mComputeLammpsCuda.invoke(
            nlocal, nghost, eflagEither?1:0, vflagEither?1:0, (vflagAtom||cvflagAtom)?1:0,
            mCudaNlGetter.posX(), mCudaNlGetter.posY(), mCudaNlGetter.posZ(), mCudaNlGetter.type(),
            mCudaNMerges, mCudaMergeSorted, mCudaCutsq, mCudaNlGetter.nlSize(), mCudaNlGetter.nlIdx(), mCudaLmpType2NNAPType,
            mCudaFpHyperParam, mCudaFpParam, mCudaNnParam, mCudaNormParam,
            mCudaF, mCudaEatom0, mCudaVatom0, mCudaVatom1,
            mCudaGradNlDx, mCudaGradNlDy, mCudaGradNlDz,
            mCudaMgNlSize, mCudaMgNlIdx
        );
        CudaCore.cudaExceptionCheck(tCode);
        mCudaComputeTimer.to();
        
        // cuda -> lammps
        mCudaCopyTimer.from();
        tCode = mCuda2Lammps.invoke(
            nlocal, nghost, eflagEither?1:0, aPair.eflagAtom()?1:0, vflagEither?1:0, vflagAtom?1:0, cvflagAtom?1:0,
            aPair.atomF(), aPair.engVdwl(), aPair.eatom(), aPair.virial(), aPair.vatom(), aPair.cvatom(),
            mCudaNlGetter.ilist(), mFltBuf, mCudaF, mCudaEatom0, mCudaVatom0, mCudaVatom1
        );
        CudaCore.cudaExceptionCheck(tCode);
        mCudaCopyTimer.to();
    }
    
    private boolean mCudaGpumdInited = false;
    private void initGpumdDataCuda_() throws CudaException {
        if (mCudaGpumdInited) return;
        mCudaGpumdInited = true;
        
//        mCudaBufNlSize = mPtrMngTot.newIntCudaPointer();
//        mCudaBufNlFx = mPtrMngTot.newFloatCudaPointer();
//        mCudaBufNlFy = mPtrMngTot.newFloatCudaPointer();
//        mCudaBufNlFz = mPtrMngTot.newFloatCudaPointer();
    }
    void computeGPUMD(int number_of_particles, int N1, int N2, int neighnumMax,
                      long g_neighbor_number, long g_neighbor_list,
                      long nl_dx, long nl_dy, long nl_dz,
                      long g_type,
                      long g_fx, long g_fy, long g_fz,
                      long g_virial, long g_potential) throws CudaException {
        throw new DeprecationException("Use newer version for GPUMD support");
//        if (isClosed()) throw new IllegalStateException("This NNAP is dead");
//        if (!mCuda) throw new IllegalStateException();
//        // GPUMD 传来的 nl dxyz 总是单精度的，这里直接使用因此强制要求内部一定为单精度
//        if (!mSingle) throw new IllegalStateException();
//
//        initGpumdDataCuda_();
//        // 近邻列表缓存向量长度规范
////        mPtrMngTot.ensureCapacity(mCudaBufNlSize, (long)number_of_particles*(mNMergesMax+1));
////        int tTotNlSize = number_of_particles*neighnumMax;
////        mPtrMngTot.ensureCapacity(mCudaBufNlFx, tTotNlSize);
////        mPtrMngTot.ensureCapacity(mCudaBufNlFy, tTotNlSize);
////        mPtrMngTot.ensureCapacity(mCudaBufNlFz, tTotNlSize);
//
//        int tCode = mComputeGPUMD.invoke(
//            number_of_particles, N1, N2,
//            new IntCudaPointer(g_neighbor_number), new IntCudaPointer(g_neighbor_list),
//            new FloatCudaPointer(nl_dx), new FloatCudaPointer(nl_dy), new FloatCudaPointer(nl_dz), new IntCudaPointer(g_type),
//            mCudaNMerges, mCudaMergeSorted, mCudaCutsq,
//            mCudaFpHyperParam, mCudaFpParam, mCudaNnParam, mCudaNormParam,
//            new DoubleCudaPointer(g_fx), new DoubleCudaPointer(g_fy), new DoubleCudaPointer(g_fz),
//            new DoubleCudaPointer(g_virial), new DoubleCudaPointer(g_potential)
////            mCudaBufNlDx, mCudaBufNlDy, mCudaBufNlDz, mCudaBufNlType, mCudaBufNlIdx, mCudaBufNlSize,
////            mCudaBufNlFx, mCudaBufNlFy, mCudaBufNlFz
//        );
//        CudaCore.cudaExceptionCheck(tCode);
    }
}
