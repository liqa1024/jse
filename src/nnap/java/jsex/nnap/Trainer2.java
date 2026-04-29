package jsex.nnap;

import jse.atom.*;
import jse.cache.IntVectorCache;
import jse.cache.LogicalVectorCache;
import jse.cache.VectorCache;
import jse.code.IO;
import jse.code.UT;
import jse.code.collection.*;
import jse.code.io.ISavable;
import jse.code.timer.AccumulatedTimer;
import jse.cptr.*;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jse.optim.Adam;
import jse.optim.IOptimizer;
import jse.optim.LBFGS;
import jse.parallel.ParforThreadPool;
import jsex.nnap.basis.SharedBasis2;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.IOException;
import java.util.*;
import java.util.function.IntUnaryOperator;

/**
 * 纯 jse + jit 实现的 nnap 训练器，从而实现更高的优化效果
 * <p>
 * 新结构大幅简化了训练器的实现
 * @author liqa
 */
public class Trainer2 implements IHasSymbol, ISavable, AutoCloseable {
    protected final static String DEFAULT_UNITS = "metal";
    protected final static double DEFAULT_ENERGY_WEIGHT = 1.0;
    protected final static double DEFAULT_FORCE_WEIGHT = 0.1;
    protected final static double DEFAULT_STRESS_WEIGHT = 1.0;
    protected final static int DEFAULT_NTHREADS = 4;
    protected final static double DEFAULT_BASIS_MAX = 5.0;
    protected final static int DEFAULT_POST_FUSE_SIZE = 6;
    protected final static String DEFAULT_WTYPE = "exfull";
    
    public final static ILossFunc LOSS_SQUARE = (pred, real, grad) -> {
        double tErr = pred - real;
        grad.mValue = 2.0*tErr;
        return tErr*tErr;
    };
    public final static ILossFunc LOSS_ABSOLUTE = (pred, real, grad) -> {
        double tErr = pred - real;
        grad.mValue = (pred-real)>=0 ? 1.0 : -1.0;
        return Math.abs(tErr);
    };
    public final static ILossFunc LOSS_SMOOTHL1 = (pred, real, grad) -> {
        double tErr = pred - real;
        double tErrAbs = Math.abs(tErr);
        grad.mValue = tErrAbs>=1.0 ? (tErr>=0?1.0:-1.0) : tErr;
        return tErrAbs>=1.0 ? (tErrAbs-0.5) : (0.5*tErr*tErr);
    };
    
    protected static class DataSet implements AutoCloseable {
        public int mSize = 0;
        /** 原始的原子结构数据，简单引用因为初始化后就会自动清空 */
        public final List<IAtomData> mAtomData = new ArrayList<>(64);
        /** 每个原子数据结构对应的每原子能量值 */
        public final DoubleList mEng = new DoubleList(64);
        /** 这里力数据使用 List 存储，每个原子结构一组 */
        public final List<Vector> mForceX = new ArrayList<>(64), mForceY = new ArrayList<>(64), mForceZ = new ArrayList<>(64);
        /** 每个原子数据结构对应的压力值 */
        public final DoubleList mStressXX = new DoubleList(64), mStressYY = new DoubleList(64), mStressZZ = new DoubleList(64),
                                mStressXY = new DoubleList(64), mStressXZ = new DoubleList(64), mStressYZ = new DoubleList(64);
        /** 原子种类，每个原子结构一组 */
        public final List<IntVector> mAtomType = new ArrayList<>(64);
        /** 每个原子数据结构对应的总体积值 */
        public final DoubleList mVolume = new DoubleList(64);
        
        /** 原子近邻原子数，每个原子结构一组 */
        public final List<IntVector> mNumNei = new ArrayList<>(64);
        /** 近邻列表，每个原子结构一组，每个原子对应一个近邻列表向量 */
        public final List<IntCPointer[]> mNl = new ArrayList<>(64);
        /** 现在存储近邻的种类列表，这样不用每次遍历都临时构造 */
        public final List<IntCPointer[]> mNlType = new ArrayList<>(64);
        /** 近邻原子坐标差，每个原子结构一组，每个原子对应一个近邻列表向量 */
        public final List<IDoubleOrFloatCPointer[]> mNlDx = new ArrayList<>(64);
        public final List<IDoubleOrFloatCPointer[]> mNlDy = new ArrayList<>(64);
        public final List<IDoubleOrFloatCPointer[]> mNlDz = new ArrayList<>(64);
        
        @Override public void close() {
            for (int i = 0; i < mSize; ++i) {
                final int tNumAtoms = mAtomType.get(i).size();
                IntCPointer[] tNl = mNl.get(i), tNlType = mNlType.get(i);
                IDoubleOrFloatCPointer[] tNlDx = mNlDx.get(i), tNlDy = mNlDy.get(i), tNlDz = mNlDz.get(i);
                for (int k = 0; k < tNumAtoms; ++k) {
                    tNl[k].free(); tNlType[k].free();
                    tNlDx[k].free(); tNlDy[k].free(); tNlDz[k].free();
                }
            }
            mNl.clear(); mNlType.clear();
            mNlDx.clear(); mNlDy.clear(); mNlDz.clear();
        }
    }
    
    /// ParforThreadPool stuffs
    protected final ParforThreadPool mPool;
    @Override public void close() {
        mPool.close();
        
        final int tNumThreads = mPool.nthreads();
        for (int ti = 0; ti < tNumThreads; ++ti) {
            mAGradNlDxBuf[ti].free(); mAGradNlDyBuf[ti].free(); mAGradNlDzBuf[ti].free();
            mBGradAGradNlDxBuf[ti].free(); mBGradAGradNlDyBuf[ti].free(); mBGradAGradNlDzBuf[ti].free();
            for (IGrowableDoubleOrFloatCPointer tSubCache : mCacheBuf.get(ti)) {
                tSubCache.free();
            }
        }
        mTrainData.close();
        mTestData.close();
    }
    /// IHasSymbol stuffs
    @Override public int ntypes() {return mNNAP.ntypes();}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mNNAP.symbol(aType);}
    public NNAP2 model() {return mNNAP;}
    public String units() {return mNNAP.units();}
    public String precision() {return mNNAP.precision();}
    
    
    protected final boolean mSingle;
    protected final DataSet mTrainData, mTestData;
    protected final boolean mIsRetrain;
    protected final NNAP2 mNNAP;
    protected final IVector mRefEngs;
    protected double mNormMuEng = 0.0, mNormSigmaEng = 1.0;
    protected boolean mHasForce = false;
    protected boolean mHasStress = false;
    protected boolean mHasTest = false;
    protected final DoubleList mTrainLoss = new DoubleList(64);
    protected final DoubleList mTestLoss = new DoubleList(64);
    protected boolean mNormInit = false;
    protected boolean mFirstTrain = true;
    
    protected int mStepsPerEpoch = 1;
    protected int mStep = -1;
    protected int mEpoch = -1, mNEpochs = -1;
    protected int mSelectEpoch = -1;
    protected double mMinLoss = Double.POSITIVE_INFINITY;
    protected final IVector mSelectParas;
    private IntVector mAllSliceTrain = null;
    
    /// buffer stuffs
    private final List<List<IGrowableDoubleOrFloatCPointer>> mCacheBuf;
    private final IGrowableDoubleOrFloatCPointer[] mAGradNlDxBuf, mAGradNlDyBuf, mAGradNlDzBuf;
    private final IGrowableDoubleOrFloatCPointer[] mBGradAGradNlDxBuf, mBGradAGradNlDyBuf, mBGradAGradNlDzBuf;
    private final DoubleList[] mForceXBuf, mForceYBuf, mForceZBuf;
    private final DoubleList[] mBGradForceXBuf, mBGradForceYBuf, mBGradForceZBuf;
    
    
    protected double mEnergyWeight = DEFAULT_ENERGY_WEIGHT;
    public Trainer2 setEnergyWeight(double aWeight) {
        mEnergyWeight = aWeight;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected double mForceWeight = DEFAULT_FORCE_WEIGHT;
    public Trainer2 setForceWeight(double aWeight) {
        mForceWeight = aWeight;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected double mStressWeight = DEFAULT_STRESS_WEIGHT;
    public Trainer2 setStressWeight(double aWeight) {
        mStressWeight = aWeight;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    
    protected ILossFunc mLossFuncEng = LOSS_SMOOTHL1;
    public Trainer2 setLossFuncEnergy(ILossFunc aLossFunc) {
        mLossFuncEng = aLossFunc;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected ILossFunc mLossFuncForce = LOSS_SMOOTHL1;
    public Trainer2 setLossFuncForce(ILossFunc aLossFunc) {
        mLossFuncForce = aLossFunc;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected ILossFunc mLossFuncStress = LOSS_SMOOTHL1;
    public Trainer2 setLossFuncStress(ILossFunc aLossFunc) {
        mLossFuncStress = aLossFunc;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    public Trainer2 setLossFunc(ILossFunc aLossFunc) {
        mLossFuncEng = aLossFunc;
        mLossFuncForce = aLossFunc;
        mLossFuncStress = aLossFunc;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    public Trainer2 reset() {
        mOptimizer.reset();
        return this;
    }
    
    protected boolean mAutoBreak = true;
    public Trainer2 setAutoBreak(boolean aFlag) {
        mAutoBreak = aFlag;
        return this;
    }
    
    protected IOptimizer mOptimizer = new LBFGS(100).setLineSearch();
    protected int mBatchSize = -1;
    public Trainer2 setOptimizer(Map<String, ?> aOptArgs) {
        if (aOptArgs == null) {
            aOptArgs = Maps.of("type", "lbfgs");
        }
        Object tOptType = aOptArgs.get("type");
        if (tOptType == null) {
            tOptType = "lbfgs";
        }
        switch(tOptType.toString()) {
        case "lbfgs": case "LBFGS": {
            mBatchSize = ((Number)UT.Code.getWithDefault(aOptArgs, -1, "batch_size", "batchsize")).intValue();
            double tEta = ((Number)UT.Code.getWithDefault(aOptArgs, 0.001, "learning_rate", "lr", "eta")).doubleValue();
            int tM = ((Number)UT.Code.getWithDefault(aOptArgs, 100, "history_size", "history", "m")).intValue();
            mOptimizer = new LBFGS(tM).setLearningRate(tEta);
            if (mBatchSize > 0) mOptimizer.setNoLineSearch(); // batch 情况下不能线搜索，因为线搜索会使用上一步的梯度
            else mOptimizer.setLineSearch();
            break;
        }
        case "adam": case "Adam": {
            mBatchSize = ((Number)UT.Code.getWithDefault(aOptArgs, 512, "batch_size", "batchsize")).intValue();
            double tEta = ((Number)UT.Code.getWithDefault(aOptArgs, 0.001, "learning_rate", "lr", "eta")).doubleValue();
            double tBeta1 = ((Number)UT.Code.getWithDefault(aOptArgs, 0.9, "beta1")).doubleValue();
            double tBeta2 = ((Number)UT.Code.getWithDefault(aOptArgs, 0.999, "beta2")).doubleValue();
            double tEps = ((Number)UT.Code.getWithDefault(aOptArgs, 1e-8, "epsilon", "eps")).doubleValue();
            mOptimizer = new Adam(tEta, tBeta1, tBeta2, tEps);
            break;
        }
        default: {
            throw new IllegalArgumentException("Unsupported optimizer type: " + tOptType);
        }}
        initOptimizer_();
        return this;
    }
    public Trainer2 setLearningRate(double aLearningRate) {
        mOptimizer.setLearningRate(aLearningRate);
        return this;
    }
    public Trainer2 setBatchSize(int aBatchSize) {
        mBatchSize = aBatchSize;
        if (mOptimizer instanceof LBFGS) {
            if (mBatchSize > 0) mOptimizer.setNoLineSearch(); // batch 情况下不能线搜索，因为线搜索会使用上一步的梯度
            else mOptimizer.setLineSearch();
        }
        return this;
    }
    
    /**
     * 设置基组最大值限制，现在会根据此值在基组归一化时进行缩放限制，提高描述能力
     * @param aValue 设置值
     * @return 自身方便链式调用
     */
    public Trainer2 setBasisMax(double aValue) {
        mBasisMax = aValue;
        return this;
    }
    protected double mBasisMax = DEFAULT_BASIS_MAX;
    /**
     * 设置所有种类共享相同的归一化系数，只在初始化归一化系数之前设置才能影响初始化
     * <p>
     * 现在默认不会进行基组归一化，因此直接采用 {@link SharedBasis2}
     * 的写法即可自动实现这个效果
     * @param aFlag 设置值
     * @return 自身方便链式调用
     */
    public Trainer2 setShareNorm(boolean aFlag) {
        if (mShareNorm!=null &&  mShareNorm==aFlag) return this;
        if (aFlag) {
            // 检测基组长度是否相等
            final int tNumTypes = ntypes();
            final int tBasisSize = mNNAP.mBasis[0].size();
            for (int i = 1; i < tNumTypes; ++i) {
                if (tBasisSize != mNNAP.mBasis[i].size()) throw new IllegalArgumentException("Basis sizes mismatch for share norm");
            }
        }
        mShareNorm = aFlag;
        return this;
    }
    protected @Nullable Boolean mShareNorm = null;
    protected boolean mSharedBasis = true;
    
    @SuppressWarnings("unchecked")
    Trainer2(@Range(from=1, to=Integer.MAX_VALUE) int aNumThreads, Map<String, ?> aArgs, @Nullable Map<String, ?> aModelInfo) throws Exception {
        mPool = new ParforThreadPool(aNumThreads);
        if (aModelInfo != null) {
            mIsRetrain = true;
            argsCheckRetrain_(aArgs);
            mNNAP = new NNAP2(aModelInfo, aNumThreads);
            // 重新实现部分读取来降低不必要的代码耦合
            List<? extends Map<String, ?>> tModelInfos = (List<? extends Map<String, ?>>)aModelInfo.get("models");
            if (tModelInfos == null) throw new IllegalArgumentException("No models in ModelInfo");
            mRefEngs = refEngsFromModelInfo_(mNNAP, tModelInfos);
            initNormFromModelInfo_(mNNAP, tModelInfos);
            // 标记 retrain 不需要重新设置归一化系数
            mNormInit = true;
        } else {
            mIsRetrain = false;
            mNNAP = nnapFromArgs_(aArgs, aNumThreads);
            mRefEngs = refEngsFromArgs_(mNNAP, aArgs);
            if (mNNAP.ntypes() != mRefEngs.size()) throw new IllegalArgumentException("Symbols length does not match reference energies length.");
            // 初始化 nnap 内部参数
            mNNAP.initParameters();
        }
        mSingle = mNNAP.mSingle;
        final int tNumTypes = mNNAP.ntypes();
        mSelectParas = mNNAP.parameters().copy();
        
        // 简单遍历识别 shared 基组情况
        for (int i = 1; i < tNumTypes; ++i) {
            if (!(mNNAP.mBasis[i] instanceof SharedBasis2)) {
                mSharedBasis = false;
                break;
            }
        }
        
        mTrainData = new DataSet();
        mTestData = new DataSet();
        
        mCacheBuf = new ArrayList<>(aNumThreads);
        mAGradNlDxBuf = new IGrowableDoubleOrFloatCPointer[aNumThreads];
        mAGradNlDyBuf = new IGrowableDoubleOrFloatCPointer[aNumThreads];
        mAGradNlDzBuf = new IGrowableDoubleOrFloatCPointer[aNumThreads];
        mBGradAGradNlDxBuf = new IGrowableDoubleOrFloatCPointer[aNumThreads];
        mBGradAGradNlDyBuf = new IGrowableDoubleOrFloatCPointer[aNumThreads];
        mBGradAGradNlDzBuf = new IGrowableDoubleOrFloatCPointer[aNumThreads];
        mForceXBuf = new DoubleList[aNumThreads];
        mForceYBuf = new DoubleList[aNumThreads];
        mForceZBuf = new DoubleList[aNumThreads];
        mBGradForceXBuf = new DoubleList[aNumThreads];
        mBGradForceYBuf = new DoubleList[aNumThreads];
        mBGradForceZBuf = new DoubleList[aNumThreads];
        for (int ti = 0; ti < aNumThreads; ++ti) {
            List<IGrowableDoubleOrFloatCPointer> tCacheBuf = new ArrayList<>(16);
            mCacheBuf.add(tCacheBuf);
            mAGradNlDxBuf[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mAGradNlDyBuf[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mAGradNlDzBuf[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mBGradAGradNlDxBuf[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mBGradAGradNlDyBuf[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mBGradAGradNlDzBuf[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mForceXBuf[ti] = new DoubleList();
            mForceYBuf[ti] = new DoubleList();
            mForceZBuf[ti] = new DoubleList();
            mBGradForceXBuf[ti] = new DoubleList();
            mBGradForceYBuf[ti] = new DoubleList();
            mBGradForceZBuf[ti] = new DoubleList();
        }
        initOptimizer_();
    }
    /**
     * 创建一个 nnap 的训练器
     * @param aArgs 训练参数
     * @param aModelInfo 可选的旧的模型数据，用于继续训练。当传入时训练参数仅可选：
     * <dl>
     *   <dt>nthreads (可选，默认为 4):</dt>
     *     <dd>指定训练时使用的线程数</dd>
     *   <dt>energy_weight (可选，默认为 1.0):</dt>
     *     <dd>指定 loss 函数中能量的权重</dd>
     *   <dt>force_weight (可选，默认为 0.1):</dt>
     *     <dd>指定 loss 函数中力的权重</dd>
     *   <dt>stress_weight (可选，默认为 1.0):</dt>
     *     <dd>指定 loss 函数中压力的权重</dd>
     *   <dt>optimizer (可选):</dt>
     *     <dd>
     *       指定优化器的具体参数，包含：
     *       <dl>
     *       <dt>type (可选，默认为 "lbfgs", 可选 "adam"):</dt>
     *          <dd>指定优化器的种类</dd>
     *       <dt>lr (可选，默认为 0.001):</dt>
     *          <dd>指定随机优化器采用的学习率</dd>
     *       <dt>batch_size (可选，lbfgs 默认为 -1, adam 默认为 512):</dt>
     *          <dd>指定随机优化器采用的 batch_size</dd>
     *       </dl>
     *     </dd>
     * </dl>
     */
    @SuppressWarnings("unchecked")
    public Trainer2(Map<String, ?> aArgs, @Nullable Map<String, ?> aModelInfo) throws Exception {
        this(nthreadsFromArgs_(aArgs), aArgs, aModelInfo);
        @Nullable Map<String, ?> tOptim = (Map<String, ?>)UT.Code.get(aArgs, "optimizer", "optim", "opt");
        if (tOptim != null) {
            setOptimizer(tOptim);
        }
        @Nullable Number tEnergyWeight = (Number)UT.Code.get(aArgs, "energy_weight", "eng_weight");
        if (tEnergyWeight != null) {
            setEnergyWeight(tEnergyWeight.doubleValue());
        }
        @Nullable Number tForceWeight = (Number)UT.Code.get(aArgs, "force_weight");
        if (tForceWeight != null) {
            setForceWeight(tForceWeight.doubleValue());
        }
        @Nullable Number tStressWeight = (Number)UT.Code.get(aArgs, "stress_weight");
        if (tStressWeight != null) {
            setStressWeight(tStressWeight.doubleValue());
        }
        @Nullable Boolean tShareNorm = (Boolean)UT.Code.get(aArgs, "share_norm");
        if (tShareNorm != null) {
            setShareNorm(tShareNorm);
        }
    }
    /**
     * 创建一个 nnap 的训练器
     * @param aArgs 训练参数，具体为：
     * <dl>
     *   <dt>symbols:</dt>
     *     <dd>指定元素列表</dd>
     *   <dt>ref_engs (可选):</dt>
     *     <dd>指定每个元素的参考能量</dd>
     *   <dt>nthreads (可选，默认为 4):</dt>
     *     <dd>指定训练时使用的线程数</dd>
     *   <dt>energy_weight (可选，默认为 1.0):</dt>
     *     <dd>指定 loss 函数中能量的权重</dd>
     *   <dt>force_weight (可选，默认为 0.1):</dt>
     *     <dd>指定 loss 函数中力的权重</dd>
     *   <dt>stress_weight (可选，默认为 1.0):</dt>
     *     <dd>指定 loss 函数中压力的权重</dd>
     *   <dt>units (可选，默认为 'metal'):</dt>
     *     <dd>指定势函数的单位</dd>
     *   <dt>basis (可选):</dt>
     *     <dd>
     *       指定基组的具体参数，包含：
     *       <dl>
     *       <dt>type (可选，默认为 "spherical_chebyshev"):</dt>
     *          <dd>指定基组的种类</dd>
     *       <dt>lmax (可选，默认为 6):</dt>
     *          <dd>指定基组角向序使用的最大 l</dd>
     *       <dt>nmax (可选，默认为 5):</dt>
     *          <dd>指定基组径向序使用的最大 n</dd>
     *       <dt>rcut (可选，默认为 6.0):</dt>
     *          <dd>指定基组的截断半径</dd>
     *       <dt>post_fuse_size (可选，默认为 6):</dt>
     *          <dd>指定基组针对等变量的线性混合后的维度</dd>
     *       <dt>wtype (可选，默认为 "exfull"):</dt>
     *          <dd>指定基组的化学编码方式</dd>
     *       </dl>
     *       输入列表形式则为每个种类单独设置不同的基组参数
     *     </dd>
     *   <dt>nn (可选):</dt>
     *     <dd>
     *       指定神经网络具体结构，包含：
     *       <dl>
     *       <dt>hidden_dims (可选，默认为 [32, 32]):</dt>
     *          <dd>指定神经网络每个隐藏层的神经元数目</dd>
     *       <dt>shared_hidden_dims (可选，默认不开启):</dt>
     *          <dd>指定神经网络每个隐藏层共享的神经元数目</dd>
     *       </dl>
     *       输入列表形式则为每个种类单独设置不同的神经网络参数
     *     </dd>
     *   <dt>optimizer (可选):</dt>
     *     <dd>
     *       指定优化器的具体参数，包含：
     *       <dl>
     *       <dt>type (可选，默认为 "lbfgs", 可选 "adam"):</dt>
     *          <dd>指定优化器的种类</dd>
     *       <dt>lr (可选，默认为 0.001):</dt>
     *          <dd>指定随机优化器采用的学习率</dd>
     *       <dt>batch_size (可选，lbfgs 默认为 -1, adam 默认为 512):</dt>
     *          <dd>指定随机优化器采用的 batch_size</dd>
     *       </dl>
     *     </dd>
     * </dl>
     */
    public Trainer2(Map<String, ?> aArgs) throws Exception {
        this(aArgs, null);
    }
    
    private static int nthreadsFromArgs_(Map<String, ?> aArgs) {
        return ((Number)UT.Code.getWithDefault(aArgs, DEFAULT_NTHREADS, "number_of_threads", "nthreads")).intValue();
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static NNAP2 nnapFromArgs_(Map<String, ?> aArgs, int aNumThreads) throws Exception {
        /// symbols
        String[] aSymbols;
        @Nullable Object tSymbols = UT.Code.get(aArgs, "symbols", "elems", "species");
        if (tSymbols == null) throw new IllegalArgumentException("args of trainer MUST contain `symbols`");
        if (tSymbols instanceof Collection) {
            aSymbols = IO.Text.toArray((Collection)tSymbols);
        } else
        if (tSymbols instanceof Object[]) {
            aSymbols = IO.Text.toArray((List) AbstractCollections.from((Object[])tSymbols));
        } else {
            throw new IllegalArgumentException("invalid type of symbols: " + tSymbols.getClass().getName());
        }
        /// basis
        // 为了输入参数安全，这里统一采用创建新值的方式避免修改
        List<Map> aBasisSetting = new ArrayList<>(aSymbols.length);
        @Nullable Object tBasisSetting = UT.Code.get(aArgs, "basis");
        if (tBasisSetting == null) {
            tBasisSetting = Maps.of("type", "spherical_chebyshev");
        }
        // 遍历读取塞入 aBasisSetting
        if (tBasisSetting instanceof Map) {
            // 现在这种情况其余的种类采用 share 基组
            Map<?, ?> tSubBasis = (Map<?, ?>)tBasisSetting;
            aBasisSetting.add(new HashMap(tSubBasis));
            for (int i = 1; i < aSymbols.length; ++i) {
                aBasisSetting.add(Maps.of("type", "share", "share", 1));
            }
        } else {
            for (Object tObj : (List<?>)tBasisSetting) {
                aBasisSetting.add(new HashMap((Map<?, ?>)tObj));
            }
        }
        if (aSymbols.length != aBasisSetting.size()) throw new IllegalArgumentException("Symbols length does not match reference basis length.");
        // 现在默认塞入 post_fuse 和 exfull，这里现在就可以简单实现不用考虑引用问题
        for (Object tObj : aBasisSetting) {
            Map tSubBasis = (Map)tObj;
            // 只塞 spherical_chebyshev 和 chebyshev
            Object tBasisType = tSubBasis.get("type");
            if (tBasisType == null) {
                tBasisType = "spherical_chebyshev";
            }
            switch(tBasisType.toString()) {
            case "chebyshev": case "cheby":
            case "spherical_chebyshev": case "sph_cheby": {
                // 在不存在这些参数时塞入默认 post_fuse
                if (!tSubBasis.containsKey("post_fuse") && !tSubBasis.containsKey("post_fuse_size") && !tSubBasis.containsKey("post_fuse_weight")) {
                    tSubBasis.put("post_fuse", true);
                    tSubBasis.put("post_fuse_size", DEFAULT_POST_FUSE_SIZE);
                }
                // 在不存在 wtype 时塞入 exfull
                if (!tSubBasis.containsKey("wtype")) {
                    tSubBasis.put("wtype", DEFAULT_WTYPE);
                }
                break;
            }}
        }
        /// nn
        // 为了输入参数安全，这里统一采用创建新值的方式避免修改
        List<Map> aNNSetting = new ArrayList<>(aSymbols.length);
        @Nullable Object tNNSetting = UT.Code.get(aArgs, "nn");
        if (tNNSetting == null) {
            tNNSetting = new HashMap<>();
        }
        // 遍历读取塞入 aNNSetting
        if (tNNSetting instanceof Map) {
            Map tSubNNSetting = new HashMap<>((Map<?, ?>)tNNSetting);
            // 单 map 输入下特殊处理，只需要单个 shared_hidden_dims 即可
            @Nullable Object tSharedHiddenDims = tSubNNSetting.remove("shared_hidden_dims");
            if (tSharedHiddenDims == null) {
                tSharedHiddenDims = tSubNNSetting.remove("shared_nnarch");
            }
            tSubNNSetting.put("type", "feed_forward");
            aNNSetting.add(tSubNNSetting);
            if (tSharedHiddenDims != null) {
                tSubNNSetting = Maps.of("type", "shared_feed_forward", "share", 1, "shared_hidden_dims", tSharedHiddenDims);
            }
            for (int i = 1; i < aSymbols.length; ++i) {
                aNNSetting.add(tSubNNSetting);
            }
        } else {
            for (Object tObj : (List<?>)tNNSetting) {
                aNNSetting.add(new HashMap((Map<?, ?>)tObj));
            }
        }
        if (aSymbols.length != aNNSetting.size()) throw new IllegalArgumentException("Symbols length does not match neural network length.");
        /// nnap input
        Map rModelInfos = new HashMap<>();
        List rModels = new ArrayList();
        for (int i = 0; i < aSymbols.length; ++i) {
            Map rModel = new HashMap();
            rModel.put("symbol", aSymbols[i]);
            rModel.put("basis", aBasisSetting.get(i));
            rModel.put("nn", aNNSetting.get(i));
            rModels.add(rModel);
        }
        rModelInfos.put("version", NNAP2.VERSION);
        rModelInfos.put("units", UT.Code.getWithDefault(aArgs, DEFAULT_UNITS, "units").toString());
        rModelInfos.put("models", rModels);
        return new NNAP2(rModelInfos, aNumThreads);
    }
    @SuppressWarnings("unchecked")
    private static IVector refEngsFromArgs_(NNAP2 aNNAP, Map<String, ?> aArgs) {
        @Nullable Object refEngs = UT.Code.get(aArgs, "ref_engs", "reference_energies", "erefs");
        if (refEngs == null) return Vectors.zeros(aNNAP.ntypes());
        if (refEngs instanceof Collection) {
            return Vectors.from((Collection<? extends Number>)refEngs);
        } else
        if (refEngs instanceof double[]) {
            return Vectors.from((double[])refEngs);
        } else
        if (refEngs instanceof IVector) {
            return Vectors.from((IVector)refEngs);
        } else {
            throw new IllegalArgumentException("invalid type of ref_engs: " + refEngs.getClass().getName());
        }
    }
    
    private static void argsCheckRetrain_(Map<String, ?> aArgs) {
        @Nullable Object tObj = UT.Code.get(aArgs, "symbols", "elems", "species");
        if (tObj != null) throw new IllegalArgumentException("args of trainer can NOT contain `symbols` for retraining");
        tObj = UT.Code.get(aArgs, "ref_engs", "reference_energies", "erefs");
        if (tObj != null) throw new IllegalArgumentException("args of trainer can NOT contain `ref_engs` for retraining");
        tObj = UT.Code.get(aArgs, "basis");
        if (tObj != null) throw new IllegalArgumentException("args of trainer can NOT contain `basis` for retraining");
        tObj = UT.Code.get(aArgs, "nn");
        if (tObj != null) throw new IllegalArgumentException("args of trainer can NOT contain `nn` for retraining");
        tObj = UT.Code.get(aArgs, "units");
        if (tObj != null) throw new IllegalArgumentException("args of trainer can NOT contain `units` for retraining");
    }
    private static IVector refEngsFromModelInfo_(NNAP2 aNNAP, List<? extends Map<String, ?>> aModelInfos) {
        final int tNumTypes = aNNAP.ntypes();
        IVector tRefEngs = Vectors.zeros(tNumTypes);
        for (int i = 0; i < tNumTypes; ++i) {
            Number tRefEng = (Number)aModelInfos.get(i).get("ref_eng");
            tRefEngs.set(i, tRefEng==null?0.0:tRefEng.doubleValue());
        }
        return tRefEngs;
    }
    private void initNormFromModelInfo_(NNAP2 aNNAP, List<? extends Map<String, ?>> aModelInfos) {
        final int tNumTypes = aNNAP.ntypes();
        Number tNormSigmaEng = null, tNormMuEng = null;
        for (int i = 0; i < tNumTypes; ++i) {
            Map<String, ?> tModelInfo = aModelInfos.get(i);
            // 现在优先读取 norm eng
            if (tNormSigmaEng == null) tNormSigmaEng = (Number)tModelInfo.get("norm_sigma_eng");
            if (tNormMuEng == null) tNormMuEng = (Number)tModelInfo.get("norm_mu_eng");
        }
        mNormSigmaEng = tNormSigmaEng==null ? 1.0 : tNormSigmaEng.doubleValue();
        mNormMuEng = tNormMuEng==null ? 0.0 : tNormMuEng.doubleValue();
    }
    
    private String epochStr_(int aEpoch) {
        String tNEpochs = String.valueOf(mNEpochs);
        String tCEpochs = String.format("%0"+tNEpochs.length()+"d", aEpoch+1);
        return "epoch: "+tCEpochs;
    }
    private void initOptimizer_() {
        final int[] tLossDiv = {0};
        final double[] tLossTot = {0.0};
        mOptimizer.setParameter(mNNAP.parameters())
        .setLossFunc(() -> calLoss(false))
        .setLossFuncGrad(grad -> calLoss(false, grad))
        .setLogPrinter((step, lineSearchStep, loss, printLog) -> {
            mStep = step;
            mEpoch = step / mStepsPerEpoch;
            int tStepIdx = step % mStepsPerEpoch;
            if (mBatchSize > 0) {
                int tRestSize = mTrainData.mSize % mBatchSize;
                int tBatchSize = (tStepIdx==mStepsPerEpoch-1) ? (tRestSize+mBatchSize) : mBatchSize;
                tLossDiv[0] += tBatchSize;
                tLossTot[0] += tBatchSize * loss;
                if (tStepIdx == mStepsPerEpoch-1) {
                    if (mEpoch != mNEpochs-1) {
                        mAllSliceTrain.shuffle();
                        mSliceTrain = mAllSliceTrain.subVec(0, (mStepsPerEpoch==1) ? (tRestSize+mBatchSize) : mBatchSize);
                    }
                } else {
                    int tStart = (tStepIdx+1) * mBatchSize;
                    int tEnd = tStart + mBatchSize;
                    if (tStepIdx+1 == mStepsPerEpoch-1) tEnd += tRestSize;
                    mSliceTrain = mAllSliceTrain.subVec(tStart, tEnd);
                    if (printLog) UT.Timer.progressBar(String.format("loss: %.4g", tLossTot[0]/tLossDiv[0]));
                }
            } else {
                mSliceTrain = mFullSliceTrain;
            }
            if (tStepIdx == mStepsPerEpoch-1) {
                if (mBatchSize > 0) {
                    loss = tLossTot[0]/tLossDiv[0];
                    tLossDiv[0] = 0;
                    tLossTot[0] = 0.0;
                }
                mTrainLoss.add(loss);
                if (mHasTest) {
                    double tLossTest = calLoss(true);
                    mTestLoss.add(tLossTest);
                    if (tLossTest < mMinLoss) {
                        mSelectEpoch = mEpoch;
                        mMinLoss = tLossTest;
                        mSelectParas.fill(mNNAP.parameters());
                    }
                    if (printLog) UT.Timer.progressBar(String.format("loss: %.4g | %.4g", loss, tLossTest));
                } else {
                    if (printLog) UT.Timer.progressBar(String.format("loss: %.4g", loss));
                }
                if (printLog && mBatchSize>0 && mEpoch!=mNEpochs-1) {
                    UT.Timer.progressBar(Maps.of(
                        "name", epochStr_(mEpoch+1),
                        "max", mStepsPerEpoch,
                        "length", 100
                    ));
                }
            }
        })
        .setBreakChecker((step, loss, lastLoss, parameterStep) -> {
            if (!mAutoBreak) return false; // 现在允许直接关闭自动跳出，在训练末期重新开始训练时步长会非常小
            if (mBatchSize > 0) return false; // 分 batch 情况永远不跳出，因为梯度随机
            if (step==0 || Double.isNaN(lastLoss)) return false;
            return Math.abs(lastLoss-loss) < Math.abs(lastLoss)*1e-7;
        });
    }
    
    @FunctionalInterface public interface ILossFunc {
        double call(double aPred, double aReal, DoubleWrapper rGrad);
    }
    
    private final ISlice mFullSliceTest = new ISlice() {
        @Override public int get(int aIdx) {
            if (aIdx >= mTestData.mSize) throw new IndexOutOfBoundsException();
            return aIdx;
        }
        @Override public int size() {
            return mTestData.mSize;
        }
    };
    private final ISlice mFullSliceTrain = new ISlice() {
        @Override public int get(int aIdx) {
            if (aIdx >= mTrainData.mSize) throw new IndexOutOfBoundsException();
            return aIdx;
        }
        @Override public int size() {
            return mTrainData.mSize;
        }
    };
    private ISlice mSliceTrain = null;
    
    protected double calLoss(boolean aTest) {
        return calLoss(aTest, null);
    }
    protected double calLoss(boolean aTest, @Nullable Vector rGrad) {
        return calLoss(aTest, false, null, rGrad);
    }
    protected double calLossDetail(boolean aTest, @Nullable Vector rLossDetail) {
        return calLoss(aTest, true, rLossDetail, null);
    }
    protected double calLoss(boolean aTest, boolean aFull, @Nullable Vector rLossDetail, @Nullable Vector rGrad) {
        return calLoss_(aTest, aTest ? mFullSliceTest : (aFull?mFullSliceTrain:mSliceTrain), rLossDetail, rGrad,
                        mLossFuncEng, mLossFuncForce, mLossFuncStress);
    }
    protected void calMAE(boolean aTest, Vector rMAE) {
        calLoss_(aTest, aTest?mFullSliceTest:mFullSliceTrain, rMAE, null,
                 LOSS_ABSOLUTE, LOSS_ABSOLUTE, LOSS_ABSOLUTE);
        rMAE.multiply2this(mNormSigmaEng);
        rMAE.update(0, v -> v/mEnergyWeight);
        rMAE.update(1, v -> v/mForceWeight);
        rMAE.update(2, v -> v/mStressWeight);
    }
    private double calLoss_(boolean aTest, ISlice aSlice, @Nullable Vector rLossDetail, @Nullable Vector rGrad,
                            ILossFunc aLossFuncEng, ILossFunc aLossFuncForce, ILossFunc aLossFuncStress) {
        final DataSet tData = aTest ? mTestData : mTrainData;
        final boolean tRequireGrad = rGrad!=null;
        if (aTest && tRequireGrad) throw new IllegalStateException();
        final int tNumThreads = mPool.nthreads();
        if (tRequireGrad) {
            mNNAP.requireGrad();
            mNNAP.zeroGrad();
        }
        // 目前简单处理，在任何计算之前总是先更新一下参数
        mNNAP.updateParameters();
        // 遍历统计有效数据数目
        final int tSliceSize = aSlice.size();
        int tEngSize = 0, tForceSize = 0, tStressSize = 0;
        for (int si = 0; si < tSliceSize; ++si) {
            final int i = aSlice.get(si);
            if (!Double.isNaN(tData.mEng.get(i))) {
                ++tEngSize;
            }
            if (mHasForce) {
                Vector tFxReal = tData.mForceX.get(i);
                if (tFxReal!=null) {
                    tForceSize += tFxReal.size();
                }
            }
            if (mHasStress) {
                if (!Double.isNaN(tData.mStressXX.get(i))) {
                    ++tStressSize;
                }
            }
        }
        final int fEngSize = tEngSize, fForceSize = tForceSize, fStressSize = tStressSize;
        List<Vector> rLossPar = VectorCache.getZeros(3, tNumThreads);
        mPool.parfor(tSliceSize, (si, threadID) -> {
            final int i = aSlice.get(si);
            Vector rLoss = rLossPar.get(threadID);
            
            double tEngReal = tData.mEng.get(i);
            final boolean tHasEng = !Double.isNaN(tEngReal);
            
            Vector tFxReal = null, tFyReal = null, tFzReal = null;
            if (mHasForce) {
                tFxReal = tData.mForceX.get(i); tFyReal = tData.mForceY.get(i); tFzReal = tData.mForceZ.get(i);
            }
            final boolean tHasForce = tFxReal!=null;
            
            double tSxxReal = Double.NaN, tSyyReal = Double.NaN, tSzzReal = Double.NaN;
            double tSxyReal = Double.NaN, tSxzReal = Double.NaN, tSyzReal = Double.NaN;
            if (mHasStress) {
                tSxxReal = tData.mStressXX.get(i); tSyyReal = tData.mStressYY.get(i); tSzzReal = tData.mStressZZ.get(i);
                tSxyReal = tData.mStressXY.get(i); tSxzReal = tData.mStressXZ.get(i); tSyzReal = tData.mStressYZ.get(i);
            }
            final boolean tHasStress = !Double.isNaN(tSxxReal);
            
            IntVector tAtomType = tData.mAtomType.get(i);
            IntVector tNumNei = tData.mNumNei.get(i);
            int tNumAtoms = tAtomType.size();
            IntCPointer[] tNlType = tData.mNlType.get(i);
            IDoubleOrFloatCPointer[] tNlDx = tData.mNlDx.get(i);
            IDoubleOrFloatCPointer[] tNlDy = tData.mNlDy.get(i);
            IDoubleOrFloatCPointer[] tNlDz = tData.mNlDz.get(i);
            
            DoubleWrapper rBGradEng = new DoubleWrapper(0.0);
            List<IGrowableDoubleOrFloatCPointer> rCache = mCacheBuf.get(threadID);
            while (rCache.size() < tNumAtoms) rCache.add(mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1));
            
            if (!tHasForce && !tHasStress) {
                if (!tHasEng) return;
                
                double rEng = 0.0;
                for (int k = 0; k < tNumAtoms; ++k) {
                    final int cType = tAtomType.get(k);
                    double tSubEng = mNNAP.forwardEnergy(threadID,
                        tNlDx[k], tNlDy[k], tNlDz[k], tNlType[k],
                        tNumNei.get(k), cType, rCache.get(k)
                    );
                    // 注意 nnap 内部获取能量会是准确值，这里需要再次归一化
                    rEng += (tSubEng - mRefEngs.get(cType-1));
                }
                rEng /= tNumAtoms;
                // 这样确保两者操作总是一致/等价的
                rEng = (rEng - mNormMuEng) / mNormSigmaEng;
                tEngReal = (tEngReal - mNormMuEng) / mNormSigmaEng;
                double tLossEng = aLossFuncEng.call(rEng, tEngReal, rBGradEng);
                rLoss.add(0, mEnergyWeight * tLossEng / fEngSize);
                /// backward
                if (!tRequireGrad) return;
                double tBGradEng = mEnergyWeight * rBGradEng.value() / fEngSize;
                tBGradEng /= (mNormSigmaEng * tNumAtoms);
                for (int k = 0; k < tNumAtoms; ++k) {
                    mNNAP.backwardEnergy(threadID,
                        tBGradEng, tNlDx[k], tNlDy[k], tNlDz[k], tNlType[k],
                        tNumNei.get(k), tAtomType.get(k), rCache.get(k)
                    );
                }
                return;
            }
            IntCPointer[] tNl = tData.mNl.get(i);
            double tVolume = tData.mVolume.get(i);
            
            DoubleWrapper rSubBGradFx = new DoubleWrapper(0.0), rSubBGradFy = new DoubleWrapper(0.0), rSubBGradFz = new DoubleWrapper(0.0);
            DoubleWrapper rBGradSxx = new DoubleWrapper(0.0), rBGradSyy = new DoubleWrapper(0.0), rBGradSzz = new DoubleWrapper(0.0);
            DoubleWrapper rBGradSxy = new DoubleWrapper(0.0), rBGradSxz = new DoubleWrapper(0.0), rBGradSyz = new DoubleWrapper(0.0);
            
            IGrowableDoubleOrFloatCPointer rAGradNlDx = mAGradNlDxBuf[threadID];
            IGrowableDoubleOrFloatCPointer rAGradNlDy = mAGradNlDyBuf[threadID];
            IGrowableDoubleOrFloatCPointer rAGradNlDz = mAGradNlDzBuf[threadID];
            DoubleList rFx = mForceXBuf[threadID];
            DoubleList rFy = mForceYBuf[threadID];
            DoubleList rFz = mForceZBuf[threadID];
            IGrowableDoubleOrFloatCPointer rBGradAGradNlDx = mBGradAGradNlDxBuf[threadID];
            IGrowableDoubleOrFloatCPointer rBGradAGradNlDy = mBGradAGradNlDyBuf[threadID];
            IGrowableDoubleOrFloatCPointer rBGradAGradNlDz = mBGradAGradNlDzBuf[threadID];
            DoubleList rBGradFx = mBGradForceXBuf[threadID];
            DoubleList rBGradFy = mBGradForceYBuf[threadID];
            DoubleList rBGradFz = mBGradForceZBuf[threadID];
            
            double rEng = 0.0;
            if (tHasForce) {
                rFx.clear(); rFx.addZeros(tNumAtoms);
                rFy.clear(); rFy.addZeros(tNumAtoms);
                rFz.clear(); rFz.addZeros(tNumAtoms);
            }
            double rSxx = 0.0, rSyy = 0.0, rSzz = 0.0, rSxy = 0.0, rSxz = 0.0, rSyz = 0.0;
            for (int k = 0; k < tNumAtoms; ++k) {
                final int tNlSize = tNumNei.get(k);
                final int cType = tAtomType.get(k);
                IntCPointer tSubNl = tNl[k], tSubNlType = tNlType[k];
                IDoubleOrFloatCPointer tSubNlDx = tNlDx[k], tSubNlDy = tNlDy[k], tSubNlDz = tNlDz[k];
                
                rAGradNlDx.ensureCapacity(tNlSize);
                rAGradNlDy.ensureCapacity(tNlSize);
                rAGradNlDz.ensureCapacity(tNlSize);
                
                double tSubEng = mNNAP.forwardEnergyForce(threadID,
                    tSubNlDx, tSubNlDy, tSubNlDz, tSubNlType,
                    tNlSize, cType, rCache.get(k),
                    rAGradNlDx, rAGradNlDy, rAGradNlDz
                );
                // 注意 nnap 内部获取能量会是准确值，这里需要再次归一化
                if (tHasEng) {
                    rEng += (tSubEng - mRefEngs.get(cType-1));
                }
                // 这里累加来得到力和压力值
                for (int j = 0; j < tNlSize; ++j) {
                    double fx = rAGradNlDx.getAtD(j);
                    double fy = rAGradNlDy.getAtD(j);
                    double fz = rAGradNlDz.getAtD(j);
                    int nlk = tSubNl.getAt(j);
                    if (tHasForce) {
                        rFx.set(k, rFx.get(k) - fx);
                        rFy.set(k, rFy.get(k) - fy);
                        rFz.set(k, rFz.get(k) - fz);
                        rFx.set(nlk, rFx.get(nlk) + fx);
                        rFy.set(nlk, rFy.get(nlk) + fy);
                        rFz.set(nlk, rFz.get(nlk) + fz);
                    }
                    // cal stress here
                    if (tHasStress) {
                        double dx = tSubNlDx.getAtD(j);
                        double dy = tSubNlDy.getAtD(j);
                        double dz = tSubNlDz.getAtD(j);
                        rSxx += dx*fx; rSyy += dy*fy; rSzz += dz*fz;
                        rSxy += dx*fy; rSxz += dx*fz; rSyz += dy*fz;
                    }
                }
            }
            // cal stress here
            if (tHasStress) {
                rSxx = -rSxx/tVolume; rSyy = -rSyy/tVolume; rSzz = -rSzz/tVolume;
                rSxy = -rSxy/tVolume; rSxz = -rSxz/tVolume; rSyz = -rSyz/tVolume;
            }
            // 能量队归一化以及 loss 计算
            if (tHasEng) {
                rEng /= tNumAtoms;
                rEng = (rEng - mNormMuEng) / mNormSigmaEng;
                tEngReal = (tData.mEng.get(i) - mNormMuEng) / mNormSigmaEng;
                double tLossEng = aLossFuncEng.call(rEng, tEngReal, rBGradEng);
                rLoss.add(0, mEnergyWeight * tLossEng / fEngSize);
            }
            // 力和压力需要这样归一化
            if (tHasForce) {
                for (int k = 0; k < tNumAtoms; ++k) {
                    rFx.set(k, rFx.get(k) / mNormSigmaEng);
                    rFy.set(k, rFy.get(k) / mNormSigmaEng);
                    rFz.set(k, rFz.get(k) / mNormSigmaEng);
                }
            }
            if (tHasStress) {
                rSxx /= mNormSigmaEng; rSyy /= mNormSigmaEng; rSzz /= mNormSigmaEng;
                rSxy /= mNormSigmaEng; rSxz /= mNormSigmaEng; rSyz /= mNormSigmaEng;
            }
            // 力和压力 loss 计算
            if (tHasForce) {
                if (tRequireGrad) {
                    rBGradFx.clear(); rBGradFx.addZeros(tNumAtoms);
                    rBGradFy.clear(); rBGradFy.addZeros(tNumAtoms);
                    rBGradFz.clear(); rBGradFz.addZeros(tNumAtoms);
                }
                double tLossForce = 0.0;
                for (int k = 0; k < tNumAtoms; ++k) {
                    tLossForce += aLossFuncForce.call(rFx.get(k), tFxReal.get(k)/mNormSigmaEng, rSubBGradFx);
                    tLossForce += aLossFuncForce.call(rFy.get(k), tFyReal.get(k)/mNormSigmaEng, rSubBGradFy);
                    tLossForce += aLossFuncForce.call(rFz.get(k), tFzReal.get(k)/mNormSigmaEng, rSubBGradFz);
                    if (tRequireGrad) {
                        rBGradFx.set(k, rSubBGradFx.value());
                        rBGradFy.set(k, rSubBGradFy.value());
                        rBGradFz.set(k, rSubBGradFz.value());
                    }
                }
                rLoss.add(1, mForceWeight * tLossForce / (fForceSize*3));
            }
            if (tHasStress) {
                double tLossStress = 0.0;
                tLossStress += aLossFuncStress.call(rSxx, tSxxReal/mNormSigmaEng, rBGradSxx);
                tLossStress += aLossFuncStress.call(rSyy, tSyyReal/mNormSigmaEng, rBGradSyy);
                tLossStress += aLossFuncStress.call(rSzz, tSzzReal/mNormSigmaEng, rBGradSzz);
                tLossStress += aLossFuncStress.call(rSxy, tSxyReal/mNormSigmaEng, rBGradSxy);
                tLossStress += aLossFuncStress.call(rSxz, tSxzReal/mNormSigmaEng, rBGradSxz);
                tLossStress += aLossFuncStress.call(rSyz, tSyzReal/mNormSigmaEng, rBGradSyz);
                rLoss.add(2, mStressWeight * tLossStress / (fStressSize*6));
            }
            /// backward
            if (!tRequireGrad) return;
            double tBGradEng = 0.0;
            if (tHasEng) {
                tBGradEng = mEnergyWeight * rBGradEng.value() / fEngSize;
                tBGradEng /= (mNormSigmaEng * tNumAtoms);
            }
            if (tHasForce) {
                final double tMul = mForceWeight / (mNormSigmaEng * fForceSize*3);
                for (int k = 0; k < tNumAtoms; ++k) {
                    rBGradFx.set(k, tMul*rBGradFx.get(k));
                    rBGradFy.set(k, tMul*rBGradFy.get(k));
                    rBGradFz.set(k, tMul*rBGradFz.get(k));
                }
            }
            double tBGradSxx = 0.0, tBGradSyy = 0.0, tBGradSzz = 0.0;
            double tBGradSxy = 0.0, tBGradSxz = 0.0, tBGradSyz = 0.0;
            if (tHasStress) {
                final double tMul = -mStressWeight / (mNormSigmaEng * tVolume * fStressSize*6);
                tBGradSxx = tMul*rBGradSxx.value(); tBGradSyy = tMul*rBGradSyy.value(); tBGradSzz = tMul*rBGradSzz.value();
                tBGradSxy = tMul*rBGradSxy.value(); tBGradSxz = tMul*rBGradSxz.value(); tBGradSyz = tMul*rBGradSyz.value();
            }
            for (int k = 0; k < tNumAtoms; ++k) {
                final int tNlSize = tNumNei.get(k);
                final int cType = tAtomType.get(k);
                IntCPointer tSubNl = tNl[k], tSubNlType = tNlType[k];
                IDoubleOrFloatCPointer tSubNlDx = tNlDx[k], tSubNlDy = tNlDy[k], tSubNlDz = tNlDz[k];
                
                rBGradAGradNlDx.ensureCapacity(tNlSize);
                rBGradAGradNlDy.ensureCapacity(tNlSize);
                rBGradAGradNlDz.ensureCapacity(tNlSize);
                // 反向传播力和压力的构造过程
                double tBGradFxk = 0.0, tBGradFyk = 0.0, tBGradFzk = 0.0;
                if (tHasForce) {
                    tBGradFxk = rBGradFx.get(k);
                    tBGradFyk = rBGradFy.get(k);
                    tBGradFzk = rBGradFz.get(k);
                }
                for (int j = 0; j < tNlSize; ++j) {
                    int nlk = tSubNl.getAt(j);
                    double rSubBGradAGradNlDx = 0.0, rSubBGradAGradNlDy = 0.0, rSubBGradAGradNlDz = 0.0;
                    if (tHasForce) {
                        rSubBGradAGradNlDx += rBGradFx.get(nlk) - tBGradFxk;
                        rSubBGradAGradNlDy += rBGradFy.get(nlk) - tBGradFyk;
                        rSubBGradAGradNlDz += rBGradFz.get(nlk) - tBGradFzk;
                    }
                    // stress loss grad here
                    if (tHasStress) {
                        double dx = tSubNlDx.getAtD(j);
                        double dy = tSubNlDy.getAtD(j);
                        double dz = tSubNlDz.getAtD(j);
                        rSubBGradAGradNlDx += dx*tBGradSxx;
                        rSubBGradAGradNlDy += dy*tBGradSyy + dx*tBGradSxy;
                        rSubBGradAGradNlDz += dz*tBGradSzz + dx*tBGradSxz + dy*tBGradSyz;
                    }
                    rBGradAGradNlDx.putAtD(j, rSubBGradAGradNlDx);
                    rBGradAGradNlDy.putAtD(j, rSubBGradAGradNlDy);
                    rBGradAGradNlDz.putAtD(j, rSubBGradAGradNlDz);
                }
                mNNAP.backwardEnergyForce(threadID,
                    tBGradEng, tSubNlDx, tSubNlDy, tSubNlDz, tSubNlType,
                    tNlSize, cType, rCache.get(k),
                    rBGradAGradNlDx, rBGradAGradNlDy, rBGradAGradNlDz
                );
            }
        });
        if (tRequireGrad) {
            mNNAP.backwardParameter();
            rGrad.fill(mNNAP.gradParameters());
        }
        Vector rLoss = rLossPar.get(0);
        for (int ti = 1; ti < tNumThreads; ++ti) {
            rLoss.plus2this(rLossPar.get(ti));
        }
        if (rLossDetail != null) {
            rLossDetail.fill(rLoss);
        }
        double tLoss = rLoss.sum();
        VectorCache.returnVec(rLossPar);
        return tLoss;
    }
    
    
    private void addData_(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress, DataSet rData) {
        rData.mAtomData.add(aAtomData);
        final boolean tHasEng = !Double.isNaN(aEnergy);
        // 简单处理（不需要近邻列表）的数据添加在这里实现
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        final int tNumAtoms = aAtomData.natoms();
        IntList rAtomType = new IntList(tNumAtoms);
        for (int i = 0; i < tNumAtoms; ++i) {
            int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
            rAtomType.add(tType);
            // 计算相对能量值
            if (tHasEng) {
                aEnergy -= mRefEngs.get(tType-1);
            }
        }
        rData.mAtomType.add(rAtomType.asVec());
        rData.mVolume.append(aAtomData.volume());
        rData.mEng.append(tHasEng ? (aEnergy/tNumAtoms) : Double.NaN);
        // 添加力
        if (mHasForce) {
            if (aForces==null) {
                rData.mForceX.add(null);
                rData.mForceY.add(null);
                rData.mForceZ.add(null);
            } else {
                DoubleList rForceX = new DoubleList(tNumAtoms);
                DoubleList rForceY = new DoubleList(tNumAtoms);
                DoubleList rForceZ = new DoubleList(tNumAtoms);
                for (int i = 0; i < tNumAtoms; ++i) {
                    rForceX.add(aForces.get(i, 0));
                    rForceY.add(aForces.get(i, 1));
                    rForceZ.add(aForces.get(i, 2));
                }
                rData.mForceX.add(rForceX.asVec());
                rData.mForceY.add(rForceY.asVec());
                rData.mForceZ.add(rForceZ.asVec());
            }
        }
        // 应力
        if (mHasStress) {
            if (aStress==null) {
                rData.mStressXX.add(Double.NaN);
                rData.mStressYY.add(Double.NaN);
                rData.mStressZZ.add(Double.NaN);
                rData.mStressXY.add(Double.NaN);
                rData.mStressXZ.add(Double.NaN);
                rData.mStressYZ.add(Double.NaN);
            } else {
                rData.mStressXX.add(aStress.get(0));
                rData.mStressYY.add(aStress.get(1));
                rData.mStressZZ.add(aStress.get(2));
                rData.mStressXY.add(aStress.get(3));
                rData.mStressXZ.add(aStress.get(4));
                rData.mStressYZ.add(aStress.get(5));
            }
        }
        ++rData.mSize;
    }
    private void validForceData_() {
        if (mHasForce) return;
        for (int i = 0; i < mTrainData.mSize; ++i) {
            mTrainData.mForceX.add(null);
            mTrainData.mForceY.add(null);
            mTrainData.mForceZ.add(null);
        }
        if (mHasTest) {
            for (int i = 0; i < mTestData.mSize; ++i) {
                mTestData.mForceX.add(null);
                mTestData.mForceY.add(null);
                mTestData.mForceZ.add(null);
            }
        }
        mHasForce = true;
    }
    private void validStressData_() {
        if (mHasStress) return;
        for (int i = 0; i < mTrainData.mSize; ++i) {
            mTrainData.mStressXX.add(Double.NaN);
            mTrainData.mStressYY.add(Double.NaN);
            mTrainData.mStressZZ.add(Double.NaN);
            mTrainData.mStressXY.add(Double.NaN);
            mTrainData.mStressXZ.add(Double.NaN);
            mTrainData.mStressYZ.add(Double.NaN);
        }
        if (mHasTest) {
            for (int i = 0; i < mTestData.mSize; ++i) {
                mTestData.mStressXX.add(Double.NaN);
                mTestData.mStressYY.add(Double.NaN);
                mTestData.mStressZZ.add(Double.NaN);
                mTestData.mStressXY.add(Double.NaN);
                mTestData.mStressXZ.add(Double.NaN);
                mTestData.mStressYZ.add(Double.NaN);
            }
        }
        mHasStress = true;
    }
    /**
     * 增加一个训练集数据
     * @param aAtomData 原子结构数据
     * @param aEnergy 此原子结构数据的总能量
     * @param aForces 可选的每个原子的力，按行排列，每列对应 x,y,z 方向的力
     * @param aStress 可选的原子结构数据的应力值，按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列
     * @see IAtomData
     * @see IMatrix
     */
    public void addTrainData(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress) {
        if (!mHasForce && aForces!=null) {
            // 现在支持部分数据缺省，因此这里需要补全缺省的数据占位
            validForceData_();
        }
        if (!mHasStress && aStress!=null) {
            // 现在支持部分数据缺省，因此这里需要补全缺省的数据占位
            validStressData_();
        }
        // 添加数据
        addData_(aAtomData, aEnergy, aForces, aStress, mTrainData);
    }
    /**
     * {@code addTrainData(aAtomData, aEnergy, aForces, null)}
     * @see #addTrainData(IAtomData, double, IMatrix, IVector)
     */
    public void addTrainData(IAtomData aAtomData, double aEnergy, IMatrix aForces) {
        addTrainData(aAtomData, aEnergy, aForces, null);
    }
    /**
     * {@code addTrainData(aAtomData, aEnergy, null, null)}
     * @see #addTrainData(IAtomData, double, IMatrix, IVector)
     */
    public void addTrainData(IAtomData aAtomData, double aEnergy) {
        addTrainData(aAtomData, aEnergy, null, null);
    }
    /**
     * 增加一个测试集数据
     * @param aAtomData 原子结构数据
     * @param aEnergy 此原子结构数据的总能量
     * @see IAtomData
     * @see IMatrix
     */
    public void addTestData(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress) {
        if (!mHasForce && aForces!=null) {
            // 现在支持部分数据缺省，因此这里需要补全缺省的数据占位
            validForceData_();
        }
        if (!mHasStress && aStress!=null) {
            // 现在支持部分数据缺省，因此这里需要补全缺省的数据占位
            validStressData_();
        }
        addData_(aAtomData, aEnergy, aForces, aStress, mTestData);
        if (!mHasTest) mHasTest = true;
    }
    /**
     * {@code addTestData(aAtomData, aEnergy, aForces, null)}
     * @see #addTestData(IAtomData, double, IMatrix, IVector)
     */
    public void addTestData(IAtomData aAtomData, double aEnergy, IMatrix aForces) {
        addTestData(aAtomData, aEnergy, aForces, null);
    }
    /**
     * {@code addTestData(aAtomData, aEnergy, null, null)}
     * @see #addTestData(IAtomData, double, IMatrix, IVector)
     */
    public void addTestData(IAtomData aAtomData, double aEnergy) {
        addTestData(aAtomData, aEnergy, null, null);
    }
    
    
    protected void initDataNl(boolean aPrintLog) {
        final int tTrainSize = mTrainData.mAtomData.size();
        final int tTestSize = mTestData.mAtomData.size();
        final int tTrainStart = mTrainData.mSize - tTrainSize;
        final int tTestStart = mTestData.mSize - tTestSize;
        // 预先添加占位，因为需要保证并行线程安全且顺序一致
        for (int ai = 0; ai < tTrainSize; ++ai) {
            mTrainData.mNumNei.add(null);
            mTrainData.mNl.add(null);
            mTrainData.mNlType.add(null);
            mTrainData.mNlDx.add(null);
            mTrainData.mNlDy.add(null);
            mTrainData.mNlDz.add(null);
        }
        for (int ai = 0; ai < tTestSize; ++ai) {
            mTestData.mNumNei.add(null);
            mTestData.mNl.add(null);
            mTestData.mNlType.add(null);
            mTestData.mNlDx.add(null);
            mTestData.mNlDy.add(null);
            mTestData.mNlDz.add(null);
        }
        // 创建近邻列表构建器缓存
        final int tNumThreads = mPool.nthreads();
        IntList[] tNlPar = new IntList[tNumThreads];
        IntList[] tNlTypePar = new IntList[tNumThreads];
        DoubleList[] tNlDxPar = new DoubleList[tNumThreads];
        DoubleList[] tNlDyPar = new DoubleList[tNumThreads];
        DoubleList[] tNlDzPar = new DoubleList[tNumThreads];
        for (int ti = 0; ti < tNumThreads; ++ti) {
            tNlPar[ti] = new IntList(16);
            tNlTypePar[ti] = new IntList(16);
            tNlDxPar[ti] = new DoubleList(16);
            tNlDyPar[ti] = new DoubleList(16);
            tNlDzPar[ti] = new DoubleList(16);
        }
        if (aPrintLog) UT.Timer.progressBar("init nl", tTrainSize+tTestSize);
        mPool.parfor(tTrainSize+tTestSize, (ii, threadID) -> {
            DataSet rData = ii<tTrainSize ? mTrainData : mTestData;
            int ai = ii<tTrainSize ? ii : ii-tTrainSize;
            int i = ai + (ii<tTrainSize ? tTrainStart : tTestStart);
            
            IAtomData tAtomData = rData.mAtomData.get(ai);
            IntVector tAtomType = rData.mAtomType.get(i);
            int tNumAtoms = tAtomData.natoms();
            
            IntVector rNumNei = IntVector.zeros(tNumAtoms);
            IntCPointer[] rNl = new IntCPointer[tNumAtoms];
            IntCPointer[] rNlType = new IntCPointer[tNumAtoms];
            IDoubleOrFloatCPointer[] rNlDx = new IDoubleOrFloatCPointer[tNumAtoms];
            IDoubleOrFloatCPointer[] rNlDy = new IDoubleOrFloatCPointer[tNumAtoms];
            IDoubleOrFloatCPointer[] rNlDz = new IDoubleOrFloatCPointer[tNumAtoms];
            rData.mNumNei.set(i, rNumNei);
            rData.mNl.set(i, rNl);
            rData.mNlType.set(i, rNlType);
            rData.mNlDx.set(i, rNlDx);
            rData.mNlDy.set(i, rNlDy);
            rData.mNlDz.set(i, rNlDz);
            
            IntList tNlBuf = tNlPar[threadID], tNlTypeBuf = tNlTypePar[threadID];
            DoubleList tNlDxBuf = tNlDxPar[threadID], tNlDyBuf = tNlDyPar[threadID], tNlDzBuf = tNlDzPar[threadID];
            
            try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(tAtomData)) {
                for (int k = 0; k < tNumAtoms; ++k) {
                    int tType = tAtomType.get(k);
                    // 增加近邻列表，这里直接重新添加
                    tNlBuf.clear(); tNlTypeBuf.clear();
                    tNlDxBuf.clear(); tNlDyBuf.clear(); tNlDzBuf.clear();
                    tAPC.nl_().forEachNeighbor(k, mNNAP.rcut(tType), (dx, dy, dz, idx) -> {
                        tNlBuf.add(idx);
                        tNlTypeBuf.add(tAtomType.get(idx));
                        tNlDxBuf.add(dx); tNlDyBuf.add(dy); tNlDzBuf.add(dz);
                    });
                    int tNlSize = tNlBuf.size();
                    rNumNei.set(k, tNlSize);
                    IntCPointer tNlPtr = IntCPointer.malloc(tNlSize);
                    IntCPointer tNlTypePtr = IntCPointer.malloc(tNlSize);
                    IDoubleOrFloatCPointer tNlDxPtr = mSingle ? FloatCPointer.malloc(tNlSize) : DoubleCPointer.malloc(tNlSize);
                    IDoubleOrFloatCPointer tNlDyPtr = mSingle ? FloatCPointer.malloc(tNlSize) : DoubleCPointer.malloc(tNlSize);
                    IDoubleOrFloatCPointer tNlDzPtr = mSingle ? FloatCPointer.malloc(tNlSize) : DoubleCPointer.malloc(tNlSize);
                    tNlPtr.fill(tNlBuf); rNl[k] = tNlPtr;
                    tNlTypePtr.fill(tNlTypeBuf); rNlType[k] = tNlTypePtr;
                    tNlDxPtr.fillD(tNlDxBuf); rNlDx[k] = tNlDxPtr;
                    tNlDyPtr.fillD(tNlDyBuf); rNlDy[k] = tNlDyPtr;
                    tNlDzPtr.fillD(tNlDzBuf); rNlDz[k] = tNlDzPtr;
                }
            }
            if (aPrintLog) UT.Timer.progressBar();
        });
        mTrainData.mAtomData.clear();
        mTestData.mAtomData.clear();
    }
    
    protected void initNormBasis() {
        final boolean tShareNorm = mShareNorm==null ? mSharedBasis : mShareNorm;
        final int tNumTypes = ntypes();
        final int tThreadNum = mPool.nthreads();
        Vector[][] tMuPar = new Vector[tThreadNum][tNumTypes];
        Vector[][] tSigmaPar = new Vector[tThreadNum][tNumTypes];
        Vector[][] tMaxPar = new Vector[tThreadNum][tNumTypes];
        Vector[][] tMinPar = new Vector[tThreadNum][tNumTypes];
        Vector[][] tFpPar = new Vector[tThreadNum][tNumTypes];
        IntVector[] tDivPar = new IntVector[tThreadNum];
        IGrowableDoubleOrFloatCPointer[] tFpPtrPar = new IGrowableDoubleOrFloatCPointer[tThreadNum];
        for (int ti = 0; ti < tThreadNum; ++ti) {
            for (int i = 0; i < tNumTypes; ++i) {
                int tBasisSize = mNNAP.mBasis[i].size();
                tMuPar[ti][i] = VectorCache.getZeros(tBasisSize);
                tSigmaPar[ti][i] = VectorCache.getZeros(tBasisSize);
                tMaxPar[ti][i] = VectorCache.getVec(tBasisSize); tMaxPar[ti][i].fill(Double.NEGATIVE_INFINITY);
                tMinPar[ti][i] = VectorCache.getVec(tBasisSize); tMinPar[ti][i].fill(Double.POSITIVE_INFINITY);
                tFpPar[ti][i] = VectorCache.getVec(tBasisSize);
            }
            tDivPar[ti] = IntVectorCache.getZeros(tNumTypes);
            tFpPtrPar[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
        }
        mPool.parfor(mTrainData.mSize, (i, threadID) -> {
            IntVector tNumNei = mTrainData.mNumNei.get(i);
            IntVector tAtomType = mTrainData.mAtomType.get(i);
            int tNumAtoms = tAtomType.size();
            
            IntCPointer[] tNlType = mTrainData.mNlType.get(i);
            IDoubleOrFloatCPointer[] tNlDx = mTrainData.mNlDx.get(i);
            IDoubleOrFloatCPointer[] tNlDy = mTrainData.mNlDy.get(i);
            IDoubleOrFloatCPointer[] tNlDz = mTrainData.mNlDz.get(i);
            
            IGrowableDoubleOrFloatCPointer rFpPtr = tFpPtrPar[threadID];
            Vector[] tFp = tFpPar[threadID];
            Vector[] tNormMu = tMuPar[threadID];
            Vector[] tNormSigma = tSigmaPar[threadID];
            Vector[] tMax = tMaxPar[threadID];
            Vector[] tMin = tMinPar[threadID];
            IntVector tDiv = tDivPar[threadID];
            
            for (int k = 0; k < tNumAtoms; ++k) {
                int tType = tAtomType.get(k);
                // 现在实时计算基组而不是缓存
                rFpPtr.ensureCapacity(mNNAP.mBasis[tType-1].size());
                mNNAP.calFp(threadID,
                    tNlDx[k], tNlDy[k], tNlDz[k], tNlType[k],
                    tNumNei.get(k), tType, rFpPtr
                );
                Vector tSubFp = tFp[tType-1];
                rFpPtr.parse2destD(tSubFp);
                // 统计归一化系数
                int tNormIdx = tShareNorm ? 0 : (tType-1);
                tNormMu[tNormIdx].plus2this(tSubFp);
                tNormSigma[tNormIdx].operation().operate2this(tSubFp, (lhs, rhs) -> lhs + rhs * rhs);
                tMax[tNormIdx].operation().operate2this(tSubFp, Math::max);
                tMin[tNormIdx].operation().operate2this(tSubFp, Math::min);
                tDiv.increment(tNormIdx);
            }
        });
        for (int ti = 1; ti < tThreadNum; ++ti) {
            for (int i = 0; i < tNumTypes; ++i) {
                tMuPar[0][i].plus2this(tMuPar[ti][i]);
                tSigmaPar[0][i].plus2this(tSigmaPar[ti][i]);
                tMaxPar[0][i].operation().operate2this(tMaxPar[ti][i], Math::max);
                tMinPar[0][i].operation().operate2this(tMinPar[ti][i], Math::min);
            }
            tDivPar[0].plus2this(tDivPar[ti]);
        }
        for (int i = 0; i < tNumTypes; ++i) if (!tShareNorm || i==0) {
            int tDivI = tDivPar[0].get(i);
            if (tDivI == 0) {
                tMuPar[0][i].fill(0.0);
                tSigmaPar[0][i].fill(1.0);
            } else {
                tMuPar[0][i].div2this(tDivI);
                tSigmaPar[0][i].div2this(tDivI);
                tMaxPar[0][i].minus2this(tMuPar[0][i]);
                tMinPar[0][i].minus2this(tMuPar[0][i]);
                tMinPar[0][i].abs2this();
                tMaxPar[0][i].operation().operate2this(tMinPar[0][i], Math::max);
                tSigmaPar[0][i].operation().operate2this(tMuPar[0][i], (lhs, rhs) -> lhs - rhs*rhs);
                tSigmaPar[0][i].operation().operate2this(tMaxPar[0][i], (v, max) -> {
                    v = MathEX.Code.numericEqual(v, 0.0) ? 1.0 : MathEX.Fast.sqrt(v);
                    return Math.max(v, max/mBasisMax);
                });
            }
        }
        for (int i = 0; i < tNumTypes; ++i) if (tShareNorm && i!=0) {
            tMuPar[0][i].fill(tMuPar[0][0]);
            tSigmaPar[0][i].fill(tSigmaPar[0][0]);
        }
        // put to nnap
        for (int i = 0; i < tNumTypes; ++i) {
            mNNAP.normMu(i+1).fillD(tMuPar[0][i]);
            mNNAP.normSigma(i+1).fillD(tSigmaPar[0][i]);
        }
        // return caches
        for (int ti = 0; ti < tThreadNum; ++ti) {
            for (int i = 0; i < tNumTypes; ++i) {
                VectorCache.returnVec(tFpPar[ti][i]);
                VectorCache.returnVec(tMaxPar[ti][i]);
                VectorCache.returnVec(tMinPar[ti][i]);
                VectorCache.returnVec(tMuPar[ti][i]);
                VectorCache.returnVec(tSigmaPar[ti][i]);
            }
            IntVectorCache.returnVec(tDivPar[ti]);
            tFpPtrPar[ti].free();
        }
    }
    protected void initNormEng() {
        // 这里采用中位数和上下四分位数来归一化能量
        // 手动遍历拷贝来去除掉 nan
        final Vector.Builder rBuilder = Vector.builder();
        mTrainData.mEng.forEach(v -> {
            if (!Double.isNaN(v)) rBuilder.add(v);
        });
        Vector tSortedEng = rBuilder.build();
        if (tSortedEng.size() < 10) {
            UT.Code.warning("too less input energy ("+tSortedEng.size()+"), check your input or dataset.");
            return;
        }
        tSortedEng.sort();
        int tSize = tSortedEng.size();
        int tSize2 = tSize/2;
        mNormMuEng = tSortedEng.get(tSize2);
        if ((tSize&1)==1) {
            mNormMuEng = (mNormMuEng + tSortedEng.get(tSize2+1))*0.5;
        }
        int tSize4 = tSize2/2;
        double tEng14 = tSortedEng.get(tSize4);
        double tEng14R = tSortedEng.get(tSize4+1);
        int tSize34 = tSize2+tSize4;
        if ((tSize&1)==1) ++tSize34;
        double tEng34 = tSortedEng.get(tSize34);
        double tEng34R = tSortedEng.get(tSize34+1);
        if ((tSize&1)==1) {
            if ((tSize2&1)==1) {
                tEng14 = (tEng14 + 3*tEng14R)*0.25;
                tEng34 = (3*tEng34 + tEng34R)*0.25;
            } else {
                tEng14 = (3*tEng14 + tEng14R)*0.25;
                tEng34 = (tEng34 + 3*tEng34R)*0.25;
            }
        } else {
            if ((tSize2&1)==1) {
                tEng14 = (tEng14 + tEng14R)*0.5;
                tEng34 = (tEng34 + tEng34R)*0.5;
            }
        }
        mNormSigmaEng = tEng34 - tEng14;
        final int tNumTypes = ntypes();
        for (int type = 1; type <= tNumTypes; ++type) {
            mNNAP.setNormMuEng(type, mNormMuEng+mRefEngs.get(type-1));
            mNNAP.setNormSigmaEng(type, mNormSigmaEng);
        }
        mOptimizer.markLossFuncChanged();
    }
    
    protected void checkDataSet() {
        final int tNumTypes = ntypes();
        ILogicalVector tHasData = LogicalVectorCache.getZeros(tNumTypes);
        for (int i = 0; i < mTrainData.mSize; ++i) {
            IntVector tAtomType = mTrainData.mAtomType.get(i);
            int tAtomNum = tAtomType.size();
            for (int k = 0; k < tAtomNum; ++k) {
                // 这里不特地考虑 mirror，虽然 mirror 原则上可以没有，但是这也是一种不太合适的数据集，给出警告没有问题
                tHasData.set(tAtomType.get(k)-1, true);
            }
        }
        for (int i = 0; i < tNumTypes; ++i) {
            if (!tHasData.get(i)) {
                UT.Code.warning("number of atoms of type `"+symbol(i+1)+"` is zero, check your input or dataset.");
            }
        }
        LogicalVectorCache.returnVec(tHasData);
    }
    
    
    /**
     * 统计势函数的计算力的速度，会强制串行来保证结果有效性
     * <p>
     * 一般需要二次调用确保预热来得到正确的测量结果
     *
     * @param aTest 是否使用测试集进行速度统计，默认在有测试集时总是使用测试集
     * @param aMaxTimeSecond 统计的最长时间，单位为秒
     * @return 统计得到的平均每毫秒（ms）调用的原子力的次数
     */
    public double statSpeed(boolean aTest, double aMaxTimeSecond) {
        DataSet tData = aTest ? mTestData : mTrainData;
        final int tDataSize = tData.mSize;
        IntVector tSlice = Vectors.range(tDataSize);
        tSlice.shuffle();
        
        IGrowableDoubleOrFloatCPointer rAGradNlDx = mAGradNlDxBuf[0];
        IGrowableDoubleOrFloatCPointer rAGradNlDy = mAGradNlDyBuf[0];
        IGrowableDoubleOrFloatCPointer rAGradNlDz = mAGradNlDzBuf[0];
        
        AccumulatedTimer tTimer = new AccumulatedTimer();
        long tSteps = 0;
        while (tTimer.get() < aMaxTimeSecond) for (int si = 0; si < tDataSize; ++si) {
            final int i = tSlice.get(si);
            IntVector tNumNei = tData.mNumNei.get(i);
            IntVector tAtomType = tData.mAtomType.get(i);
            final int tNumAtoms = tAtomType.size();
            IntCPointer[] tNlType = tData.mNlType.get(i);
            IDoubleOrFloatCPointer[] tNlDx = tData.mNlDx.get(i);
            IDoubleOrFloatCPointer[] tNlDy = tData.mNlDy.get(i);
            IDoubleOrFloatCPointer[] tNlDz = tData.mNlDz.get(i);
            
            tTimer.from();
            for (int k = 0; k < tNumAtoms; ++k) {
                int tNlSize = tNumNei.get(k);
                rAGradNlDx.ensureCapacity(tNlSize);
                rAGradNlDy.ensureCapacity(tNlSize);
                rAGradNlDz.ensureCapacity(tNlSize);
                
                mNNAP.calEnergyForce(
                    0, tNlDx[k], tNlDy[k], tNlDz[k], tNlType[k],
                    tNlSize, tAtomType.get(k),
                    rAGradNlDx, rAGradNlDy, rAGradNlDz
                );
            }
            tTimer.to();
            tSteps += tNumAtoms;
            if (tTimer.get() >= aMaxTimeSecond) break;
        }
        return tSteps / (tTimer.get()*1000);
    }
    /**
     * 统计势函数的计算力的速度，会强制串行来保证结果有效性
     * <p>
     * 一般需要二次调用确保预热来得到正确的测量结果
     *
     * @param aMaxTimeSecond 统计的最长时间，单位为秒
     * @return 统计得到的平均每毫秒（ms）调用的原子力的次数
     */
    public double statSpeed(double aMaxTimeSecond) {
        return statSpeed(mHasTest, aMaxTimeSecond);
    }
    
    
    /** 获取历史 loss 值 */
    public IVector trainLoss() {return mTrainLoss.asVec();}
    public IVector testLoss() {return mTestLoss.asVec();}
    
    /** 开始训练模型，这里直接训练给定的步数 */
    public void train(int aNEpochs, boolean aEarlyStop, boolean aPrintLog) {
        mNEpochs = aNEpochs;
        // 清空旧的早停存储
        mMinLoss = Double.POSITIVE_INFINITY;
        // 数据近邻列表初始化
        if (!mTrainData.mAtomData.isEmpty() || !mTestData.mAtomData.isEmpty()) {
            initDataNl(aPrintLog);
        }
        // 初始化归一化参数，现在只会初始化一次
        if (!mNormInit) {
            if (aPrintLog) System.out.println("Init norm...");
            initNormEng();
            initNormBasis();
            mNormInit = true;
        }
        if (mFirstTrain) {
            mFirstTrain = false;
            if (!mIsRetrain) {
                // 这里独立检测输入是否合适
                checkDataSet();
            }
        }
        if (mBatchSize > 0) {
            // 统计 batch 情况
            mStepsPerEpoch = mTrainData.mSize/mBatchSize;
            // 初始化 batch 分割
            mAllSliceTrain = Vectors.range(mTrainData.mSize);
            mAllSliceTrain.shuffle();
            mSliceTrain = mAllSliceTrain.subVec(0, mStepsPerEpoch==1 ? mTrainData.mSize : mBatchSize);
        } else {
            mStepsPerEpoch = 1;
            mSliceTrain = mFullSliceTrain;
        }
        // 开始训练
        if (aPrintLog) {
            if (mBatchSize > 0) {
                UT.Timer.progressBar(Maps.of(
                    "name", epochStr_(0),
                    "max", mStepsPerEpoch,
                    "length", 100
                ));
            } else {
                UT.Timer.progressBar(Maps.of(
                    "name", mIsRetrain ? "retrain" : "train",
                    "max", aNEpochs,
                    "length", mHasTest ? 100 : 80
                ));
            }
        }
        mOptimizer.run(aNEpochs*mStepsPerEpoch, aPrintLog);
        mNNAP.updateParameters();
        if (aPrintLog) {
            // 只会在不分 batch 时需要补全进度条
            if (mBatchSize <= 0) for (int i = mEpoch + 1; i < aNEpochs; ++i) {
                UT.Timer.progressBar(mHasTest ? String.format("loss: %.4g | %.4g", mTrainLoss.last(), mTestLoss.last()) : String.format("loss: %.4g", mTrainLoss.last()));
            }
        }
        // 应用早停
        if (aEarlyStop && mSelectEpoch>=0) {
            mNNAP.parameters().fill(mSelectParas);
            mNNAP.updateParameters();
            mOptimizer.markParameterChanged();
            if (aPrintLog) System.out.printf("Model at epoch = %d selected, test loss = %.4g\n", mSelectEpoch+1, mMinLoss);
            mSelectEpoch = -1;
            mMinLoss = Double.POSITIVE_INFINITY;
        }
        // 打印训练结果信息
        if (!aPrintLog) return;
        Vector tLossDetail = VectorCache.getVec(3);
        double tLossTot = calLossDetail(false, tLossDetail);
        double tLossE = tLossDetail.get(0);
        double tLossF = tLossDetail.get(1);
        double tLossS = tLossDetail.get(2);
        VectorCache.returnVec(tLossDetail);
        System.out.printf("Loss-E : %.4g (%s)\n", tLossE, IO.Text.percent(tLossE/tLossTot));
        if (mHasForce) System.out.printf("Loss-F : %.4g (%s)\n", tLossF, IO.Text.percent(tLossF/tLossTot));
        if (mHasStress) System.out.printf("Loss-S : %.4g (%s)\n", tLossS, IO.Text.percent(tLossS/tLossTot));
        Vector tMAE = VectorCache.getVec(3);
        calMAE(false, tMAE);
        double tMAE_E = tMAE.get(0);
        double tMAE_F = tMAE.get(1);
        double tMAE_S = tMAE.get(2);
        VectorCache.returnVec(tMAE);
        String tUnits = units();
        if (tUnits==null) tUnits = "";
        if (!mHasTest) {
            switch(tUnits) {
            case "metal": {
                System.out.printf("MAE-E: %.4g meV\n", tMAE_E*1000);
                if (mHasForce) System.out.printf("MAE-F: %.4g meV/A\n", tMAE_F*1000);
                if (mHasStress) System.out.printf("MAE-S: %.4g meV/A^3\n", tMAE_S*1000);
                break;
            }
            case "real":{
                System.out.printf("MAE-E: %.4g kcal/mol\n", tMAE_E);
                if (mHasForce) System.out.printf("MAE-F: %.4g kcal/mol/A\n", tMAE_F);
                if (mHasStress) System.out.printf("MAE-S: %.4g kcal/mol/A^3\n", tMAE_S);
                break;
            }
            default: {
                System.out.printf("MAE-E: %.4g\n", tMAE_E);
                if (mHasForce) System.out.printf("MAE-F: %.4g\n", tMAE_F);
                if (mHasStress) System.out.printf("MAE-S: %.4g\n", tMAE_S);
                break;
            }}
        } else {
            Vector tTestMAE = VectorCache.getVec(3);
            calMAE(true, tTestMAE);
            double tTestMAE_E = tTestMAE.get(0);
            double tTestMAE_F = tTestMAE.get(1);
            double tTestMAE_S = tTestMAE.get(2);
            VectorCache.returnVec(tTestMAE);
            switch(tUnits) {
            case "metal": {
                System.out.printf("MAE-E: %.4g meV | %.4g meV\n", tMAE_E*1000, tTestMAE_E*1000);
                if (mHasForce) System.out.printf("MAE-F: %.4g meV/A | %.4g meV/A\n", tMAE_F*1000, tTestMAE_F*1000);
                if (mHasStress) System.out.printf("MAE-S: %.4g meV/A^3 | %.4g meV/A^3\n", tMAE_S*1000, tTestMAE_S*1000);
                break;
            }
            case "real":{
                System.out.printf("MAE-E: %.4g kcal/mol | %.4g kcal/mol\n", tMAE_E, tTestMAE_E);
                if (mHasForce) System.out.printf("MAE-F: %.4g kcal/mol/A | %.4g kcal/mol/A\n", tMAE_F, tTestMAE_F);
                if (mHasStress) System.out.printf("MAE-S: %.4g kcal/mol/A^3 | %.4g kcal/mol/A^3\n", tMAE_S, tTestMAE_S);
                break;
            }
            default: {
                System.out.printf("MAE-E: %.4g | %.4g\n", tMAE_E, tTestMAE_E);
                if (mHasForce) System.out.printf("MAE-F: %.4g | %.4g\n", tMAE_F, tTestMAE_F);
                if (mHasStress) System.out.printf("MAE-S: %.4g | %.4g\n", tMAE_S, tTestMAE_S);
                break;
            }}
        }
        // 打印参数数目信息
        System.out.printf("N-Paras: %,d\n", mNNAP.parameters().size());
        // 测试速度并打印速度信息
        statSpeed(1.0); // 预热 1 s
        double tSpeed = statSpeed(2.0);
        System.out.printf("Speed: %.4g atom-steps/ms\n", tSpeed);
    }
    public void train(int aEpochs, boolean aEarlyStop) {
        train(aEpochs, aEarlyStop, true);
    }
    public void train(int aEpochs) {
        train(aEpochs, true);
    }
    
    
    /** 保存训练的势函数 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        final int tNumTypes = ntypes();
        List rModels = new ArrayList();
        for (int i = 0; i < tNumTypes; ++i) {
            Map rBasis = new LinkedHashMap();
            mNNAP.mBasis[i].save(rBasis);
            Map rModel = new LinkedHashMap();
            rModel.put("symbol", symbol(i+1));
            rModel.put("basis", rBasis);
            if (i == 0) {
                rModel.put("norm_mu_eng", mNormMuEng);
                rModel.put("norm_sigma_eng", mNormSigmaEng);
            }
//            if (mBasis[0][i] instanceof MirrorBasis) {
//                rModels.add(rModel);
//                continue;
//            }
            Map rNN = new LinkedHashMap();
            mNNAP.mNN[i].save(rNN);
            rModel.put("ref_eng", mRefEngs.get(i));
            rModel.put("norm_mu", toList_(mNNAP.normMu(i+1), mNNAP.mBasis[i].size()));
            rModel.put("norm_sigma", toList_(mNNAP.normSigma(i+1), mNNAP.mBasis[i].size()));
            rModel.put("nn", rNN);
            rModels.add(rModel);
        }
        rSaveTo.put("version", NNAP2.VERSION);
        String tUnits = units();
        if (tUnits != null) {
            rSaveTo.put("units", tUnits);
        }
        rSaveTo.put("models", rModels);
    }
    @SuppressWarnings({"rawtypes"})
    public void save(String aPath, boolean aPretty) throws IOException {
        Map rJson = new LinkedHashMap();
        save(rJson);
        IO.map2json(rJson, aPath, aPretty);
    }
    public void save(String aPath) throws IOException {save(aPath, false);}
    
    
    private static List<Double> toList_(IDoubleOrFloatCPointer aPtr, int aCount) {
        List<Double> rList = new ArrayList<>(aCount);
        for (int i = 0; i < aCount; ++i) {
            rList.add(aPtr.getAtD(i));
        }
        return rList;
    }
}
