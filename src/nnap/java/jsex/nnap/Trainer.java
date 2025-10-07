package jsex.nnap;

import jse.atom.*;
import jse.cache.IntVectorCache;
import jse.cache.VectorCache;
import jse.code.IO;
import jse.code.UT;
import jse.code.collection.*;
import jse.code.io.ISavable;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jse.optim.Adam;
import jse.optim.IOptimizer;
import jse.optim.LBFGS;
import jse.parallel.AbstractThreadPool;
import jse.parallel.ParforThreadPool;
import jsex.nnap.basis.*;
import jsex.nnap.nn.FeedForward;
import jsex.nnap.nn.NeuralNetwork;
import jsex.nnap.nn.SharedFeedForward;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.util.*;
import java.util.function.IntUnaryOperator;

/**
 * 纯 jse 实现的 nnap 训练器，不借助 pytorch
 * 来实现更高的优化效果
 * <p>
 * 由于是纯 jse 实现，写法可以更加灵活并且避免了重复代码
 *
 * @author liqa
 */
@ApiStatus.Experimental
public class Trainer extends AbstractThreadPool<ParforThreadPool> implements IHasSymbol, ISavable {
    protected final static String DEFAULT_UNITS = "metal";
    protected final static int[] DEFAULT_HIDDEN_DIMS = {32, 32}; // 现在统一默认为 32, 32
    protected final static double DEFAULT_ENERGY_WEIGHT = 1.0;
    protected final static double DEFAULT_FORCE_WEIGHT = 0.1;
    protected final static double DEFAULT_STRESS_WEIGHT = 1.0;
    protected final static double DEFAULT_L2_LOSS_WEIGHT = 0.001;
    protected final static int DEFAULT_THREAD_NUMBER = 4;
    
    public final static ILossFunc LOSS_SQUARE = (pred, real) -> {
        double tErr = pred - real;
        return tErr*tErr;
    };
    public final static ILossFuncGrad LOSS_SQUARE_G = (pred, real, grad) -> {
        double tErr = pred - real;
        grad.mValue = 2.0*tErr;
        return tErr*tErr;
    };
    public final static ILossFunc LOSS_ABSOLUTE = (pred, real) -> Math.abs(pred - real);
    public final static ILossFuncGrad LOSS_ABSOLUTE_G = (pred, real, grad) -> {
        double tErr = pred - real;
        grad.mValue = (pred-real)>=0 ? 1.0 : -1.0;
        return Math.abs(tErr);
    };
    public final static ILossFunc LOSS_SMOOTHL1 = (pred, real) -> {
        double tErr = Math.abs(pred - real);
        return tErr>=1.0 ? (tErr-0.5) : (0.5*tErr*tErr);
    };
    public final static ILossFuncGrad LOSS_SMOOTHL1_G = (pred, real, grad) -> {
        double tErr = pred - real;
        double tErrAbs = Math.abs(tErr);
        grad.mValue = tErrAbs>=1.0 ? (tErr>=0?1.0:-1.0) : tErr;
        return tErrAbs>=1.0 ? (tErrAbs-0.5) : (0.5*tErr*tErr);
    };
    
    protected static class DataSet {
        public int mSize = 0;
        /** 每个原子数据结构对应的每原子能量值 */
        public final DoubleList mEng = new DoubleList(64);
        /** 这里力数据使用 List 存储，每个原子结构一组 */
        public final List<Vector> mForceX = new ArrayList<>(64), mForceY = new ArrayList<>(64), mForceZ = new ArrayList<>(64);
        /** 每个原子数据结构对应的压力值 */
        public final DoubleList mStressXX = new DoubleList(64), mStressYY = new DoubleList(64), mStressZZ = new DoubleList(64),
                                mStressXY = new DoubleList(64), mStressXZ = new DoubleList(64), mStressYZ = new DoubleList(64);
        /** 原子种类，每个原子结构一组 */
        public final List<IntVector> mAtomType = new ArrayList<>(64);
        /** 近邻列表，每个原子结构一组，每个原子对应一个近邻列表向量 */
        public final List<IntList[]> mNl = new ArrayList<>(64);
        /** 现在存储近邻的种类列表，这样不用每次遍历都临时构造 */
        public final List<IntList[]> mNlType = new ArrayList<>(64);
        /** 近邻原子坐标差，每个原子结构一组，每个原子对应一个近邻列表向量 */
        public final List<DoubleList[]> mNlDx = new ArrayList<>(64), mNlDy = new ArrayList<>(64), mNlDz = new ArrayList<>(64);
        /** 每个原子数据结构对应的总体积值 */
        public final DoubleList mVolume = new DoubleList(64);
    }
    
    protected final int mTypeNum;
    protected final String[] mSymbols;
    protected final IVector mRefEngs;
    protected final Basis[][] mBasis;
    protected final NeuralNetwork[][] mNN;
    protected final DataSet mTrainData;
    protected final DataSet mTestData;
    protected final boolean mIsRetrain;
    protected boolean mHasData = false;
    protected boolean mHasForce = false;
    protected boolean mHasStress = false;
    protected boolean mHasTest = false;
    protected final Vector[] mNormMu, mNormSigma;
    protected final IntVector mNormDiv;
    protected double mNormMuEng = 0.0, mNormSigmaEng = 0.0;
    protected final DoubleList mTrainLoss = new DoubleList(64);
    protected final DoubleList mTestLoss = new DoubleList(64);
    protected boolean mNormInit = false;
    
    protected final int mTotNNParaSize, mTotBasisParaSize, mTotBasisSize;
    protected final int[] mNNParaSizes, mNNParaWeightSizes, mHiddenSizes;
    protected final int[] mBasisParaSizes, mBasisSizes;
    protected final IVector[] mNNParas, mBasisParas;
    protected final Vector[] mNormMu2, mNormSigma2;
    protected final IVector mParas;
    
    protected int mStepsPerEpoch = 1;
    protected int mStep = -1;
    protected int mEpoch = -1, mNEpochs = -1;
    protected int mSelectEpoch = -1;
    protected double mMinLoss = Double.POSITIVE_INFINITY;
    protected final Vector mSelectParas;
    private IntVector mAllSliceTrain = null;
    
    
    /// buffer stuffs
    private final Vector[] mGradParaBuf;
    private final List<List<List<Vector>>> mHiddenOutputsBuf, mHiddenGradsBuf, mHiddenGrads2Buf, mHiddenGrads3Buf, mHiddenGradGradsBuf;
    private final List<List<DoubleList>> mBasisForwardAtomsBuf, mBasisForwardForceAtomsBuf;
    private final DoubleList[] mBasisForwardBuf, mBasisBackwardBuf, mBasisBackwardForceBuf;
    
    private final DoubleList[] mForceNlXBuf, mForceNlYBuf, mForceNlZBuf;
    private final DoubleList[] mForceXBuf, mForceYBuf, mForceZBuf;
    private final DoubleList[] mLossGradForceXBuf, mLossGradForceYBuf, mLossGradForceZBuf;
    private final DoubleList[] mLossGradForceNlXBuf, mLossGradForceNlYBuf, mLossGradForceNlZBuf;
    
    private final List<List<List<Vector>>> mFpAtomsBuf, mGradNormFpAtomsBuf;
    private final Vector[][] mFpBuf, mGradFpBuf;
    private final Vector[][] mLossGradFp, mLossGradGradFp;
    private final Vector[][] mLossGradMu, mLossGradSigma;
    
    private boolean mBasisParaChanged = false;
    private boolean mFullBufInit = false;
    private final List<Vector[]> mLossGradFpFullBuf = new ArrayList<>(64);
    private final List<DoubleList[]> mBasisBackwardFullBuf = new ArrayList<>(64);
    
    private final IntList[] mTypeIlist;
    
    
    protected IOptimizer mOptimizer = new LBFGS(100).setLineSearch();
    protected int mBatchSize = -1;
    public Trainer setOptimizer(Map<String, ?> aOptArgs) {
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
        if (mBatchSize > 0) setForceNormFormula(false);
        initOptimizer_();
        return this;
    }
    public Trainer setLearningRate(double aLearningRate) {
        mOptimizer.setLearningRate(aLearningRate);
        return this;
    }
    public Trainer setBatchSize(int aBatchSize) {
        mBatchSize = aBatchSize;
        if (mOptimizer instanceof LBFGS) {
            if (mBatchSize > 0) mOptimizer.setNoLineSearch(); // batch 情况下不能线搜索，因为线搜索会使用上一步的梯度
            else mOptimizer.setLineSearch();
        }
        if (mBatchSize > 0) setForceNormFormula(false);
        return this;
    }
    
    /**
     * 设置开启训练基组内可拟合参数
     * <p>
     * 注意在训练后期，基组参数相关梯度数非常不稳定，
     * 因此在最终收敛时需要注意关闭基组训练来保证收敛到局部极小点
     * @param aFlag 设置值
     * @return 自身方便链式调用
     */
    public Trainer setTrainBasis(boolean aFlag) {
        if (mTrainBasis == aFlag) return this;
        mTrainBasis = aFlag;
        mOptimizer.markParameterChanged();
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected boolean mTrainBasis = false;
    
    /**
     * 设置所有种类共享相同的归一化系数，只在初始化归一化系数之前设置才能影响初始化
     * <p>
     * 会同时影响训练归一化系数时的行为
     * @param aFlag 设置值
     * @return 自身方便链式调用
     */
    public Trainer setShareNorm(boolean aFlag) {
        if (mShareNorm == aFlag) return this;
        if (aFlag) {
            // 检测基组长度是否相等
            int tBasisSize = mBasisSizes[0];
            for (int i = 1; i < mTypeNum; ++i) {
                if (tBasisSize != mBasisSizes[i]) throw new IllegalArgumentException("Basis sizes mismatch for share norm");
            }
        }
        // 对于训练 norm 的情况下参数长度会发生变化
        if (mTrainNorm) {
            mOptimizer.markParameterChanged();
            mOptimizer.markLossFuncChanged();
        }
        mShareNorm = aFlag;
        return this;
    }
    protected boolean mShareNorm = false;
    /**
     * 设置开启训练基组的可拟合参数，在无法使用 {@link #setForceNormFormula(boolean)}
     * 时可采用的折中高效方案
     * @param aFlag 设置值
     * @return 自身方便链式调用
     */
    public Trainer setTrainNorm(boolean aFlag) {
        if (mTrainNorm == aFlag) return this;
        if (aFlag) setForceNormFormula(false);
        mTrainNorm = aFlag;
        mOptimizer.markParameterChanged();
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected boolean mTrainNorm = false;
    /**
     * 设置强制要求归一化系数满足表达式定义，此时反向传播会穿透归一化系数的计算公式，
     * 从而导致更高的计算复杂度，理论上这应该会有更好的收敛速度。
     * @param aFlag 设置值
     * @return 自身方便链式调用
     */
    public Trainer setForceNormFormula(boolean aFlag) {
        if (mForceNormFormula == aFlag) return this;
        if (aFlag) {
            if (mIsRetrain) throw new UnsupportedOperationException("force norm formula is invalid for retraining");
            if (mBatchSize > 0) throw new UnsupportedOperationException("force norm formula is invalid for batch training");
            setTrainNorm(false);
        } else {
            clearFullBuf_();
        }
        mForceNormFormula = aFlag;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected boolean mForceNormFormula = false;
    
    protected double mEnergyWeight = DEFAULT_ENERGY_WEIGHT;
    public Trainer setEnergyWeight(double aWeight) {
        mEnergyWeight = aWeight;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected double mForceWeight = DEFAULT_FORCE_WEIGHT;
    public Trainer setForceWeight(double aWeight) {
        mForceWeight = aWeight;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected double mStressWeight = DEFAULT_STRESS_WEIGHT;
    public Trainer setStressWeight(double aWeight) {
        mStressWeight = aWeight;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected double mL2LossWeight = DEFAULT_L2_LOSS_WEIGHT;
    public Trainer setL2LossWeight(double aWeight) {
        mL2LossWeight = aWeight;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected String mUnits = DEFAULT_UNITS;
    public Trainer setUnits(String aUnits) {
        mUnits = aUnits;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    
    protected ILossFunc mLossFuncEng = LOSS_SMOOTHL1;
    protected ILossFuncGrad mLossFuncGradEng = LOSS_SMOOTHL1_G;
    public Trainer setLossFuncEnergy(ILossFunc aLossFunc, ILossFuncGrad aLossFuncGrad) {
        mLossFuncEng = aLossFunc; mLossFuncGradEng = aLossFuncGrad;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected ILossFunc mLossFuncForce = LOSS_SMOOTHL1;
    protected ILossFuncGrad mLossFuncGradForce = LOSS_SMOOTHL1_G;
    public Trainer setLossFuncForce(ILossFunc aLossFunc, ILossFuncGrad aLossFuncGrad) {
        mLossFuncForce = aLossFunc; mLossFuncGradForce = aLossFuncGrad;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    protected ILossFunc mLossFuncStress = LOSS_SMOOTHL1;
    protected ILossFuncGrad mLossFuncGradStress = LOSS_SMOOTHL1_G;
    public Trainer setLossFuncStress(ILossFunc aLossFunc, ILossFuncGrad aLossFuncGrad) {
        mLossFuncStress = aLossFunc; mLossFuncGradStress = aLossFuncGrad;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    public Trainer setLossFunc(ILossFunc aLossFunc, ILossFuncGrad aLossFuncGrad) {
        mLossFuncEng = aLossFunc; mLossFuncGradEng = aLossFuncGrad;
        mLossFuncForce = aLossFunc; mLossFuncGradForce = aLossFuncGrad;
        mLossFuncStress = aLossFunc; mLossFuncGradStress = aLossFuncGrad;
        mOptimizer.markLossFuncChanged();
        return this;
    }
    public Trainer reset() {
        mOptimizer.reset();
        return this;
    }
    
    private void clearFullBuf_() {
        mLossGradFpFullBuf.clear();
        mBasisBackwardFullBuf.clear();
        mFullBufInit = false;
    }
    private void validFullBuf_(boolean aRequireGradBackward) {
        if (mFullBufInit) return;
        mFullBufInit = true;
        mLossGradFpFullBuf.clear();
        mBasisBackwardFullBuf.clear();
        int tSize = mTrainData.mSize;
        for (int i = 0; i < tSize; ++i) {
            IntVector tAtomType = mTrainData.mAtomType.get(i);
            int tAtomNum = tAtomType.size();
            Vector[] rLossGradFp = new Vector[tAtomNum];
            mLossGradFpFullBuf.add(rLossGradFp);
            for (int k = 0; k < tAtomNum; ++k) {
                int tType = tAtomType.get(k);
                rLossGradFp[k] = Vector.zeros(mBasisSizes[tType-1]);
            }
            if (aRequireGradBackward) {
                DoubleList[] rBackwardCache = new DoubleList[tAtomNum];
                mBasisBackwardFullBuf.add(rBackwardCache);
                for (int k = 0; k < tAtomNum; ++k) {
                    rBackwardCache[k] = new DoubleList(16);
                }
            }
        }
    }
    private void validFpAtomsBuf_(int aThreadID, int aAtomNum, boolean aRequireGradBackward) {
        List<List<Vector>> tFpBuf = mFpAtomsBuf.get(aThreadID);
        List<List<Vector>> tGradFpBuf = mGradNormFpAtomsBuf.get(aThreadID);
        for (int typei = 0; typei < mTypeNum; ++typei) if (!(mBasis[0][typei] instanceof MirrorBasis)) {
            int tSize = mBasis[0][typei].size();
            List<Vector> tSubFpBuf = tFpBuf.get(typei);
            List<Vector> tSubGradFpBuf = tGradFpBuf.get(typei);
            while (tSubFpBuf.size() < aAtomNum) tSubFpBuf.add(Vectors.zeros(tSize));
            if (aRequireGradBackward) {
                while (tSubGradFpBuf.size() < aAtomNum) tSubGradFpBuf.add(Vectors.zeros(tSize));
            }
        }
    }
    private void validHiddenBuf_(int aThreadID, IntVector aAtomType, boolean aRequireGradBackward) {
        int tAtomNum = aAtomType.size();
        List<List<Vector>> tHiddenOutputsBuf = mHiddenOutputsBuf.get(aThreadID);
        List<List<Vector>> tHiddenGradsBuf = mHiddenGradsBuf.get(aThreadID);
        List<List<Vector>> tHiddenGrads2Buf = mHiddenGrads2Buf.get(aThreadID);
        List<List<Vector>> tHiddenGrads3Buf = mHiddenGrads3Buf.get(aThreadID);
        List<List<Vector>> tHiddenGradGradsBuf = mHiddenGradGradsBuf.get(aThreadID);
        for (int typei = 0; typei < mTypeNum; ++typei) if (!(mBasis[0][typei] instanceof MirrorBasis)) {
            List<Vector> tHiddenOutputs = tHiddenOutputsBuf.get(typei);
            List<Vector> tHiddenGrads = tHiddenGradsBuf.get(typei);
            List<Vector> tHiddenGrads2 = tHiddenGrads2Buf.get(typei);
            List<Vector> tHiddenGrads3 = tHiddenGrads3Buf.get(typei);
            List<Vector> tHiddenGradGrads = tHiddenGradGradsBuf.get(typei);
            while (tHiddenOutputs.size() < tAtomNum) tHiddenOutputs.add(Vector.zeros(mHiddenSizes[typei]));
            while (tHiddenGrads.size() < tAtomNum) tHiddenGrads.add(Vector.zeros(mHiddenSizes[typei]));
            if (aRequireGradBackward) {
                while (tHiddenGrads2.size() < tAtomNum) tHiddenGrads2.add(Vector.zeros(mHiddenSizes[typei]));
                while (tHiddenGrads3.size() < tAtomNum) tHiddenGrads3.add(Vector.zeros(mHiddenSizes[typei]));
                while (tHiddenGradGrads.size() < tAtomNum) tHiddenGradGrads.add(Vector.zeros(mHiddenSizes[typei]));
            }
            for (int i = 0; i < tAtomNum; ++i) {
                int typeii = aAtomType.get(i)-1;
                if (mBasis[0][typeii] instanceof MirrorBasis) {
                    typeii = ((MirrorBasis) mBasis[0][typeii]).mirrorType() - 1;
                }
                if (typei != typeii) continue;
                tHiddenOutputs.get(i).fill(0.0);
                tHiddenGrads.get(i).fill(0.0);
                if (aRequireGradBackward) {
                    tHiddenGrads2.get(i).fill(0.0);
                    tHiddenGrads3.get(i).fill(0.0);
                    tHiddenGradGrads.get(i).fill(0.0);
                }
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    Trainer(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber, Map<String, ?> aArgs, @Nullable Map<String, ?> aModelInfo) throws Exception {
        super(new ParforThreadPool(aThreadNumber));
        final @Nullable List<? extends Map<String, ?>> tModelInfos;
        if (aModelInfo != null) {
            mIsRetrain = true;
            // 为了避免代码耦合，这里还是重新实现读取
            Number tVersion = (Number)aModelInfo.get("version");
            if (tVersion != null) {
                int tVersionValue = tVersion.intValue();
                if (tVersionValue > NNAP.VERSION) throw new IllegalArgumentException("Unsupported version: " + tVersionValue);
            }
            mUnits = UT.Code.toString(aModelInfo.get("units"));
            tModelInfos = (List<? extends Map<String, ?>>)aModelInfo.get("models");
            if (tModelInfos == null) throw new IllegalArgumentException("No models in ModelInfo");
            mSymbols = symbolsFromModelInfo_(aArgs, tModelInfos);
            mTypeNum = mSymbols.length;
            mBasis = basisFromModelInfo_(mSymbols, aThreadNumber, aArgs, tModelInfos);
            mRefEngs = refEngsFromModelInfo_(mBasis[0], aArgs, tModelInfos);
            mNN = nnFromModelInfo_(mBasis[0], aThreadNumber, aArgs, tModelInfos);
        } else {
            mIsRetrain = false;
            tModelInfos = null;
            mSymbols = symbolsFrom_(aArgs);
            mTypeNum = mSymbols.length;
            mRefEngs = refEngsFrom_(mTypeNum, aArgs);
            mBasis = basisFrom_(mSymbols, aThreadNumber, aArgs);
            mNN = nnFrom_(mBasis[0], aThreadNumber, aArgs);
        }
        
        if (mTypeNum != mRefEngs.size()) throw new IllegalArgumentException("Symbols length does not match reference energies length.");
        if (mTypeNum != mBasis[0].length) throw new IllegalArgumentException("Symbols length does not match reference basis length.");
        if (mTypeNum != mNN[0].length) throw new IllegalArgumentException("Symbols length does not match neural network length.");
        
        // 简单遍历 basis 验证 mirror 的情况
        for (int i = 0; i < mSymbols.length; ++i) if (mBasis[0][i] instanceof MirrorBasis) {
            MirrorBasis tBasis = (MirrorBasis)mBasis[0][i];
            int tMirrorType = tBasis.mirrorType();
            double oRefEng = mRefEngs.get(i);
            double tRefEng = mRefEngs.get(tMirrorType-1);
            if (!Double.isNaN(oRefEng) && !MathEX.Code.numericEqual(oRefEng, tRefEng)) {
                UT.Code.warning("RefEng of mirror mismatch for type: "+(i+1)+", overwrite with mirror values automatically");
            }
            mRefEngs.set(i, tRefEng);
        }
        
        mTrainData = new DataSet();
        mTestData = new DataSet();
        
        mTypeIlist = new IntList[mTypeNum];
        for (int i = 0; i < mTypeNum; ++i) {
            mTypeIlist[i] = new IntList();
        }
        
        mNormDiv = IntVector.zeros(mTypeNum);
        mNormMu = new Vector[mTypeNum];
        mNormSigma = new Vector[mTypeNum];
        mNormMu2 = new Vector[mTypeNum];
        mNormSigma2 = new Vector[mTypeNum];
        mBasisSizes = new int[mTypeNum];
        int rTotBasisSize = 0;
        for (int i = 0; i < mTypeNum; ++i) {
            int tBasisSize = mBasis[0][i].size();
            mBasisSizes[i] = tBasisSize;
            mNormMu[i] = Vector.zeros(tBasisSize);
            mNormSigma[i] = Vector.zeros(tBasisSize);
            mNormMu2[i] = Vector.zeros(tBasisSize);
            mNormSigma2[i] = Vector.zeros(tBasisSize);
            rTotBasisSize += tBasisSize;
        }
        mTotBasisSize = rTotBasisSize;
        // 对于重新训练的情况，在这里读取已有的归一化系数
        if (tModelInfos != null) {
            initNormFromModelInfo_(tModelInfos);
            mNormInit = true;
        }
        
        mLossGradMu = new Vector[aThreadNumber][];
        mLossGradSigma = new Vector[aThreadNumber][];
        mFpBuf = new Vector[aThreadNumber][];
        mGradFpBuf = new Vector[aThreadNumber][];
        mLossGradFp = new Vector[aThreadNumber][];
        mLossGradGradFp = new Vector[aThreadNumber][];
        mFpAtomsBuf = new ArrayList<>(aThreadNumber);
        mGradNormFpAtomsBuf = new ArrayList<>(aThreadNumber);
        mHiddenOutputsBuf = new ArrayList<>(aThreadNumber);
        mHiddenGradsBuf = new ArrayList<>(aThreadNumber);
        mHiddenGrads2Buf = new ArrayList<>(aThreadNumber);
        mHiddenGrads3Buf = new ArrayList<>(aThreadNumber);
        mHiddenGradGradsBuf = new ArrayList<>(aThreadNumber);
        mBasisForwardAtomsBuf = new ArrayList<>(aThreadNumber);
        mBasisForwardForceAtomsBuf = new ArrayList<>(aThreadNumber);
        mBasisForwardBuf = new DoubleList[aThreadNumber];
        mBasisBackwardBuf = new DoubleList[aThreadNumber];
        mBasisBackwardForceBuf = new DoubleList[aThreadNumber];
        for (int ti = 0; ti < aThreadNumber; ++ti) {
            Vector[] tGradMu = new Vector[mTypeNum];
            Vector[] tGradSigma = new Vector[mTypeNum];
            Vector[] tFpBuf = new Vector[mTypeNum];
            Vector[] tGradFpBuf = new Vector[mTypeNum];
            Vector[] tLossGradFp = new Vector[mTypeNum];
            Vector[] tLossGradGradFp = new Vector[mTypeNum];
            List<List<Vector>> tFpAtomsBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tGradNormFpAtomsBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenOutputsBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGradsBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGrads2Buf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGrads3Buf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGradGradsBuf = new ArrayList<>(mTypeNum);
            List<DoubleList> tBasisForwardAtomsBuf = new ArrayList<>(16);
            List<DoubleList> tBasisForwardForceAtomsBuf = new ArrayList<>(16);
            DoubleList tBasisForwardBuf = new DoubleList(128);
            DoubleList tBasisBackwardBuf = new DoubleList(16);
            DoubleList tBasisBackwardForceBuf = new DoubleList(16);
            mLossGradMu[ti] = tGradMu;
            mLossGradSigma[ti] = tGradSigma;
            mFpBuf[ti] = tFpBuf;
            mGradFpBuf[ti] = tGradFpBuf;
            mLossGradFp[ti] = tLossGradFp;
            mLossGradGradFp[ti] = tLossGradGradFp;
            mFpAtomsBuf.add(tFpAtomsBuf);
            mGradNormFpAtomsBuf.add(tGradNormFpAtomsBuf);
            mHiddenOutputsBuf.add(tHiddenOutputsBuf);
            mHiddenGradsBuf.add(tHiddenGradsBuf);
            mHiddenGrads2Buf.add(tHiddenGrads2Buf);
            mHiddenGrads3Buf.add(tHiddenGrads3Buf);
            mHiddenGradGradsBuf.add(tHiddenGradGradsBuf);
            mBasisForwardAtomsBuf.add(tBasisForwardAtomsBuf);
            mBasisForwardForceAtomsBuf.add(tBasisForwardForceAtomsBuf);
            mBasisForwardBuf[ti] = tBasisForwardBuf;
            mBasisBackwardBuf[ti] = tBasisBackwardBuf;
            mBasisBackwardForceBuf[ti] = tBasisBackwardForceBuf;
            for (int i = 0; i < mTypeNum; ++i) {
                int tBasisSize = mBasisSizes[i];
                if (mBasis[0][i] instanceof MirrorBasis) {
                    tFpAtomsBuf.add(null);
                    tGradNormFpAtomsBuf.add(null);
                    tHiddenOutputsBuf.add(null);
                    tHiddenGradsBuf.add(null);
                    tHiddenGrads2Buf.add(null);
                    tHiddenGrads3Buf.add(null);
                    tHiddenGradGradsBuf.add(null);
                } else {
                    tGradMu[i] = Vector.zeros(tBasisSize);
                    tGradSigma[i] = Vector.zeros(tBasisSize);
                    tFpBuf[i] = Vector.zeros(tBasisSize);
                    tGradFpBuf[i] = Vector.zeros(tBasisSize);
                    tLossGradFp[i] = Vector.zeros(tBasisSize);
                    tLossGradGradFp[i] = Vector.zeros(tBasisSize);
                    tFpAtomsBuf.add(new ArrayList<>(16));
                    tGradNormFpAtomsBuf.add(new ArrayList<>(16));
                    tHiddenOutputsBuf.add(new ArrayList<>(16));
                    tHiddenGradsBuf.add(new ArrayList<>(16));
                    tHiddenGrads2Buf.add(new ArrayList<>(16));
                    tHiddenGrads3Buf.add(new ArrayList<>(16));
                    tHiddenGradGradsBuf.add(new ArrayList<>(16));
                }
            }
        }
        mForceNlXBuf = new DoubleList[aThreadNumber];
        mForceNlYBuf = new DoubleList[aThreadNumber];
        mForceNlZBuf = new DoubleList[aThreadNumber];
        mForceXBuf = new DoubleList[aThreadNumber];
        mForceYBuf = new DoubleList[aThreadNumber];
        mForceZBuf = new DoubleList[aThreadNumber];
        mLossGradForceNlXBuf = new DoubleList[aThreadNumber];
        mLossGradForceNlYBuf = new DoubleList[aThreadNumber];
        mLossGradForceNlZBuf = new DoubleList[aThreadNumber];
        mLossGradForceXBuf = new DoubleList[aThreadNumber];
        mLossGradForceYBuf = new DoubleList[aThreadNumber];
        mLossGradForceZBuf = new DoubleList[aThreadNumber];
        for (int ti = 0; ti < aThreadNumber; ++ti) {
            mForceNlXBuf[ti] = new DoubleList(16);
            mForceNlYBuf[ti] = new DoubleList(16);
            mForceNlZBuf[ti] = new DoubleList(16);
            mForceXBuf[ti] = new DoubleList(16);
            mForceYBuf[ti] = new DoubleList(16);
            mForceZBuf[ti] = new DoubleList(16);
            mLossGradForceNlXBuf[ti] = new DoubleList(16);
            mLossGradForceNlYBuf[ti] = new DoubleList(16);
            mLossGradForceNlZBuf[ti] = new DoubleList(16);
            mLossGradForceXBuf[ti] = new DoubleList(16);
            mLossGradForceYBuf[ti] = new DoubleList(16);
            mLossGradForceZBuf[ti] = new DoubleList(16);
        }
        
        mNNParaSizes = new int[mTypeNum];
        mHiddenSizes = new int[mTypeNum];
        mNNParaWeightSizes = new int[mTypeNum];
        mNNParas = new IVector[mTypeNum];
        int rTotParaSize = 0;
        for (int i = 0; i < mTypeNum; ++i) if (!(mBasis[0][i] instanceof MirrorBasis)) {
            IVector tPara;
            NeuralNetwork tNN = mNN[0][i];
            if (tNN instanceof FeedForward) {
                tPara = ((FeedForward)tNN).parameters();
                mHiddenSizes[i] = ((FeedForward)tNN).hiddenSize();
                mNNParaWeightSizes[i] = ((FeedForward)tNN).parameterWeightSize();
            } else
            if (tNN instanceof SharedFeedForward) {
                tPara = ((SharedFeedForward)tNN).parameters(false);
                mHiddenSizes[i] = ((SharedFeedForward)tNN).hiddenSize(true);
                mNNParaWeightSizes[i] = ((SharedFeedForward)tNN).parameterWeightSize(false);
            } else {
                throw new IllegalStateException();
            }
            int tParaSize = tPara.size();
            mNNParas[i] = tPara;
            mNNParaSizes[i] = tParaSize;
            rTotParaSize += tParaSize;
        }
        mTotNNParaSize = rTotParaSize;
        
        rTotParaSize = 0;
        mBasisParaSizes = new int[mTypeNum];
        mBasisParas = new IVector[mTypeNum];
        for (int i = 0; i < mTypeNum; ++i) if (!(mBasis[0][i] instanceof MirrorBasis) && !(mBasis[0][i] instanceof SharedBasis)) {
            IVector tBasisPara = mBasis[0][i].hasParameters() ? mBasis[0][i].parameters() : null;
            int tBasisParaSize = tBasisPara==null ? 0 : tBasisPara.size();
            mBasisParas[i] = tBasisPara;
            mBasisParaSizes[i] = tBasisParaSize;
            rTotParaSize += tBasisParaSize;
        }
        mTotBasisParaSize = rTotParaSize;
        
        mGradParaBuf = new Vector[aThreadNumber];
        for (int ti = 1; ti < aThreadNumber; ++ti) {
            mGradParaBuf[ti] = Vector.zeros(mTotNNParaSize+mTotBasisParaSize+mTotBasisSize+mTotBasisSize);
        }
        mSelectParas = Vector.zeros(mTotNNParaSize+mTotBasisParaSize+mTotBasisSize+mTotBasisSize);
        
        mParas = new RefVector() {
            @Override public double get(int aIdx) {
                int tIdx = aIdx;
                for (int i = 0; i < mTypeNum; ++i) {
                    int tParaSize = mNNParaSizes[i];
                    if (tIdx < tParaSize) {
                        return mNNParas[i].get(tIdx);
                    }
                    tIdx -= tParaSize;
                }
                if (mTrainBasis) for (int i = 0; i < mTypeNum; ++i) {
                    int tBasisParaSize = mBasisParaSizes[i];
                    if (tIdx < tBasisParaSize) {
                        IVector tPara = mBasisParas[i];
                        assert tPara != null;
                        return tPara.get(tIdx);
                    }
                    tIdx -= tBasisParaSize;
                }
                if (mTrainNorm) for (int i = 0; i < mTypeNum; ++i) if (!mShareNorm || i==0) {
                    int tBasisSize = mBasisSizes[i];
                    if (tIdx < tBasisSize) {
                        return mNormSigma2[i].get(tIdx);
                    }
                    tIdx -= tBasisSize;
                    if (tIdx < tBasisSize) {
                        return mNormMu2[i].get(tIdx);
                    }
                    tIdx -= tBasisSize;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public void set(int aIdx, double aValue) {
                int tIdx = aIdx;
                for (int i = 0; i < mTypeNum; ++i) {
                    int tParaSize = mNNParaSizes[i];
                    if (tIdx < tParaSize) {
                        mNNParas[i].set(tIdx, aValue);
                        return;
                    }
                    tIdx -= tParaSize;
                }
                if (mTrainBasis) for (int i = 0; i < mTypeNum; ++i) {
                    int tBasisParaSize = mBasisParaSizes[i];
                    if (tIdx < tBasisParaSize) {
                        IVector tPara = mBasisParas[i];
                        assert tPara != null;
                        tPara.set(tIdx, aValue);
                        // 修改基组系数时增加标记，用于告知需要重新初始化归一化系数
                        mBasisParaChanged = true;
                        return;
                    }
                    tIdx -= tBasisParaSize;
                }
                if (mTrainNorm) for (int i = 0; i < mTypeNum; ++i) if (!mShareNorm || i==0) {
                    int tBasisSize = mBasisSizes[i];
                    if (tIdx < tBasisSize) {
                        mNormSigma2[i].set(tIdx, aValue);
                        return;
                    }
                    tIdx -= tBasisSize;
                    if (tIdx < tBasisSize) {
                        mNormMu2[i].set(tIdx, aValue);
                        return;
                    }
                    tIdx -= tBasisSize;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                if (mTrainBasis) {
                    if (mTrainNorm) {
                        if (mShareNorm) return mTotNNParaSize+mTotBasisParaSize+mBasisSizes[0]+mBasisSizes[0];
                        else return mTotNNParaSize+mTotBasisParaSize+mTotBasisSize+mTotBasisSize;
                    } else {
                        return mTotNNParaSize+mTotBasisParaSize;
                    }
                } else {
                    if (mTrainNorm) {
                        if (mShareNorm) return mTotNNParaSize+mBasisSizes[0]+mBasisSizes[0];
                        else return mTotNNParaSize+mTotBasisSize+mTotBasisSize;
                    } else {
                        return mTotNNParaSize;
                    }
                }
            }
        };
        initOptimizer_();
    }
    /**
     * 创建一个 nnap 的训练器
     * @param aArgs 训练参数
     * @param aModelInfo 可选的旧的模型数据，用于继续训练。当传入时训练参数仅可选：
     * <dl>
     *   <dt>thread_number (可选，默认为 4):</dt>
     *     <dd>指定训练时使用的线程数</dd>
     *   <dt>energy_weight (可选，默认为 1.0):</dt>
     *     <dd>指定 loss 函数中能量的权重</dd>
     *   <dt>force_weight (可选，默认为 0.1):</dt>
     *     <dd>指定 loss 函数中力的权重</dd>
     *   <dt>stress_weight (可选，默认为 1.0):</dt>
     *     <dd>指定 loss 函数中压力的权重</dd>
     *   <dt>l2_weight (可选，默认为 0.001):</dt>
     *     <dd>指定 loss 函数中 l2 正则化的权重</dd>
     *   <dt>share_norm (可选，默认为 false):</dt>
     *     <dd>指定不同种类的归一化系数是否共享</dd>
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
    public Trainer(Map<String, ?> aArgs, @Nullable Map<String, ?> aModelInfo) throws Exception {
        this(threadNumberFrom_(aArgs), aArgs, aModelInfo);
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
        @Nullable Number tL2Weight = (Number)UT.Code.get(aArgs, "l2_weight", "l2_loss_weight", "l2loss_weight");
        if (tL2Weight != null) {
            setL2LossWeight(tL2Weight.doubleValue());
        }
        @Nullable Object tUnits = UT.Code.get(aArgs, "units");
        if (tUnits != null) {
            if (aModelInfo!=null) throw new IllegalArgumentException("args of trainer can NOT contain `units` for retraining");
            setUnits(tUnits.toString());
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
     *   <dt>thread_number (可选，默认为 4):</dt>
     *     <dd>指定训练时使用的线程数</dd>
     *   <dt>energy_weight (可选，默认为 1.0):</dt>
     *     <dd>指定 loss 函数中能量的权重</dd>
     *   <dt>force_weight (可选，默认为 0.1):</dt>
     *     <dd>指定 loss 函数中力的权重</dd>
     *   <dt>stress_weight (可选，默认为 1.0):</dt>
     *     <dd>指定 loss 函数中压力的权重</dd>
     *   <dt>l2_weight (可选，默认为 0.001):</dt>
     *     <dd>指定 loss 函数中 l2 正则化的权重</dd>
     *   <dt>units (可选，默认为 'metal'):</dt>
     *     <dd>指定势函数的单位</dd>
     *   <dt>share_norm (可选，默认为 false):</dt>
     *     <dd>指定不同种类的归一化系数是否共享</dd>
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
     *       </dl>
     *       输入列表形式则为每个种类单独设置不同的基组参数
     *     </dd>
     *   <dt>nn (可选):</dt>
     *     <dd>
     *       指定神经网络具体结构，包含：
     *       <dl>
     *       <dt>hidden_dims (可选，默认为 [32, 32]):</dt>
     *          <dd>指定神经网络每个隐藏层的神经元数目</dd>
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
    public Trainer(Map<String, ?> aArgs) throws Exception {
        this(aArgs, null);
    }
    
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static String[] symbolsFrom_(Map<String, ?> aArgs) {
        @Nullable Object tSymbols = UT.Code.get(aArgs, "symbols", "elems", "species");
        if (tSymbols == null) throw new IllegalArgumentException("args of trainer MUST contain `symbols`");
        if (tSymbols instanceof Collection) {
            return IO.Text.toArray((Collection)tSymbols);
        } else
        if (tSymbols instanceof Object[]) {
            return IO.Text.toArray((List)AbstractCollections.from((Object[])tSymbols));
        } else {
            throw new IllegalArgumentException("invalid type of symbols: " + tSymbols.getClass().getName());
        }
    }
    private static String[] symbolsFromModelInfo_(Map<String, ?> aArgs, List<? extends Map<String, ?>> aModelInfos) {
        @Nullable Object tObj = UT.Code.get(aArgs, "symbols", "elems", "species");
        if (tObj != null) throw new IllegalArgumentException("args of trainer can NOT contain `symbols` for retraining");
        final int tModelSize = aModelInfos.size();
        String[] tSymbols = new String[tModelSize];
        for (int i = 0; i < tModelSize; ++i) {
            Object tSymbol = aModelInfos.get(i).get("symbol");
            if (tSymbol == null) throw new IllegalArgumentException("No symbol in ModelInfo");
            tSymbols[i] = tSymbol.toString();
        }
        return tSymbols;
    }
    @SuppressWarnings("unchecked")
    private static IVector refEngsFrom_(int aTypeNum, Map<String, ?> aArgs) {
        @Nullable Object refEngs = UT.Code.get(aArgs, "ref_engs", "reference_energies", "erefs");
        if (refEngs == null) return Vectors.zeros(aTypeNum);
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
    private static IVector refEngsFromModelInfo_(Basis[] aBasis, Map<String, ?> aArgs, List<? extends Map<String, ?>> aModelInfos) {
        @Nullable Object tObj = UT.Code.get(aArgs, "ref_engs", "reference_energies", "erefs");
        if (tObj != null) throw new IllegalArgumentException("args of trainer can NOT contain `ref_engs` for retraining");
        final int tTypeNum = aBasis.length;
        IVector tRefEngs = Vectors.zeros(tTypeNum);
        for (int i = 0; i < tTypeNum; ++i) {
            Number tRefEng = (Number)aModelInfos.get(i).get("ref_eng");
            if (aBasis[i] instanceof MirrorBasis) {
                // mirror 会强制这些额外值缺省
                if (tRefEng != null) throw new IllegalArgumentException("ref_eng in mirror ModelInfo MUST be empty");
                tRefEngs.set(i, Double.NaN);
            } else {
                tRefEngs.set(i, tRefEng==null?0.0:tRefEng.doubleValue());
            }
        }
        return tRefEngs;
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Basis[][] basisFrom_(String[] aSymbols, int aThreadNum, Map<String, ?> aArgs) {
        @Nullable Object tBasis = UT.Code.get(aArgs, "basis");
        if (tBasis == null) {
            tBasis = Maps.of("type", "spherical_chebyshev");
        }
        if (tBasis instanceof Map) {
            // 现在这种情况其余的种类采用 share 基组
            Map<?, ?> tSubBasis = (Map<?, ?>)tBasis;
            tBasis = new ArrayList(aSymbols.length);
            ((List)tBasis).add(tSubBasis);
            for (int i = 1; i < aSymbols.length; ++i) {
                ((List)tBasis).add(Maps.of("type", "share", "share", 1));
            }
        }
        Basis[][] rBasis = new Basis[aThreadNum][];
        rBasis[0] = Basis.load(aSymbols, (List<?>)tBasis);
        for (Basis tSubBasis : rBasis[0]) if (!(tSubBasis instanceof MirrorBasis) && !(tSubBasis instanceof SharedBasis)) {
            tSubBasis.initParameters();
        }
        for (int ti = 1; ti < aThreadNum; ++ti) {
            rBasis[ti] = new Basis[aSymbols.length];
            for (int i = 0; i < aSymbols.length; ++i) {
                rBasis[ti][i] = rBasis[0][i].threadSafeRef();
            }
        }
        return rBasis;
    }
    private static Basis[][] basisFromModelInfo_(String[] aSymbols, int aThreadNum, Map<String, ?> aArgs, List<? extends Map<String, ?>> aModelInfos) {
        @Nullable Object tObj = UT.Code.get(aArgs, "basis");
        if (tObj != null) throw new IllegalArgumentException("args of trainer can NOT contain `basis` for retraining");
        Basis[][] rBasis = new Basis[aThreadNum][];
        rBasis[0] = Basis.load(aSymbols, NewCollections.map(aModelInfos, info -> {
            Object tBasisInfo = info.get("basis");
            return tBasisInfo!=null ? tBasisInfo : Maps.of("type", "spherical_chebyshev");
        }));
        for (int ti = 1; ti < aThreadNum; ++ti) {
            rBasis[ti] = new Basis[aSymbols.length];
            for (int i = 0; i < aSymbols.length; ++i) {
                rBasis[ti][i] = rBasis[0][i].threadSafeRef();
            }
        }
        return rBasis;
    }
    private static int threadNumberFrom_(Map<String, ?> aArgs) {
        return ((Number)UT.Code.getWithDefault(aArgs, DEFAULT_THREAD_NUMBER, "thread_number", "thread_num", "nthreads")).intValue();
    }
    @SuppressWarnings({"rawtypes", "unchecked"})
    private static NeuralNetwork[][] nnFrom_(Basis[] aBasis, int aThreadNum, Map<String, ?> aArgs) throws Exception {
        @Nullable Object tNNSetting = UT.Code.get(aArgs, "nn");
        if (tNNSetting == null) {
            tNNSetting = new LinkedHashMap<>();
        }
        if (tNNSetting instanceof Map) {
            Map tSubNNSetting = (Map)tNNSetting;
            tNNSetting = new ArrayList(aBasis.length);
            // 单 map 输入下特殊处理，只需要单个 shared_flags 即可
            @Nullable Object tSharedFlags = tSubNNSetting.remove("shared_flags");
            tSubNNSetting.put("type", "feed_forward");
            ((List)tNNSetting).add(tSubNNSetting);
            if (tSharedFlags != null) {
                tSubNNSetting = Maps.of("type", "shared_feed_forward", "share", 1, "shared_flags", tSharedFlags);
            }
            for (int i = 1; i < aBasis.length; ++i) {
                ((List)tNNSetting).add(tSubNNSetting);
            }
        }
        NeuralNetwork[][] rOut = new NeuralNetwork[aThreadNum][aBasis.length];
        List<?> tNNSettingList = (List<?>)tNNSetting;
        if (aBasis.length!=tNNSettingList.size()) {
            throw new IllegalArgumentException("Input size of symbols and nn mismatch");
        }
        for (int i = 0; i < aBasis.length; ++i) {
            // mirror 情况延迟初始化
            if (aBasis[i] instanceof MirrorBasis) {
                continue;
            }
            Map<?, ?> tNNSettingMap = (Map<?, ?>)tNNSettingList.get(i);
            Object tModelType = tNNSettingMap.get("type");
            if (tModelType == null) {
                tModelType = "feed_forward";
            }
            switch(tModelType.toString()) {
            case "shared_feed_forward": {
                // share 情况延迟初始化
                break;
            }
            case "feed_forward": {
                @Nullable Object tHiddenDims = UT.Code.get(tNNSettingMap, "hidden_dims", "nnarch");
                int[] tHiddenDimsArr;
                if (tHiddenDims == null) {
                    tHiddenDimsArr = DEFAULT_HIDDEN_DIMS;
                } else
                if (tHiddenDims instanceof int[]) {
                    tHiddenDimsArr = Arrays.copyOf((int[])tHiddenDims, ((int[])tHiddenDims).length);
                } else
                if (tHiddenDims instanceof List) {
                    tHiddenDimsArr = new int[((List<?>)tHiddenDims).size()];
                    for (int j = 0; j < tHiddenDimsArr.length; ++j) {
                        tHiddenDimsArr[j] = ((Number)((List<?>)tHiddenDims).get(j)).intValue();
                    }
                } else {
                    throw new IllegalArgumentException("invalid type of hidden_dims: " + tHiddenDims.getClass().getName());
                }
                //noinspection resource
                FeedForward tNN = new FeedForward(aBasis[i].size(), tHiddenDimsArr);
                tNN.initParameters();
                rOut[0][i] = tNN;
                break;
            }
            default: {
                throw new IllegalArgumentException("Unsupported nn type: " + tModelType);
            }}
        }
        for (int i = 0; i < aBasis.length; ++i) {
            Map<?, ?> tNNSettingMap = (Map<?, ?>)tNNSettingList.get(i);
            if (aBasis[i] instanceof MirrorBasis) {
                if (tNNSettingMap == null) continue;
                @Nullable Object tHiddenDims = UT.Code.get(tNNSettingMap, "hidden_dims", "nnarch");
                if (tHiddenDims == null) continue;
                UT.Code.warning("hidden_dims of mirror for type: "+(i+1)+" will be overwritten with mirror values automatically");
                continue;
            }
            Object tModelType = tNNSettingMap.get("type");
            if (tModelType.equals("shared_feed_forward")) {
                Object tShare = tNNSettingMap.get("share");
                if (tShare == null) throw new IllegalArgumentException("Key `share` required for shared_feed_forward");
                int tSharedType = ((Number)tShare).intValue();
                Object tSharedFlags = UT.Code.get(tNNSettingMap, "shared_flags");
                boolean[] tSharedFlagsArr;
                if (tSharedFlags instanceof boolean[]) {
                    tSharedFlagsArr = Arrays.copyOf((boolean[])tSharedFlags, ((boolean[])tSharedFlags).length);
                } else
                if (tSharedFlags instanceof List) {
                    tSharedFlagsArr = new boolean[((List<?>)tSharedFlags).size()];
                    for (int j = 0; j < tSharedFlagsArr.length; ++j) {
                        tSharedFlagsArr[j] = (Boolean)((List<?>)tSharedFlags).get(j);
                    }
                } else {
                    throw new IllegalArgumentException("invalid type of shared_flags: " + tSharedFlags.getClass().getName());
                }
                SharedFeedForward tNN = new SharedFeedForward(aBasis[i].size(), (FeedForward)rOut[0][tSharedType-1].threadSafeRef(), tSharedType, tSharedFlagsArr);
                tNN.initParameters(false);
                rOut[0][i] = tNN;
            }
        }
        for (int ti = 1; ti < aThreadNum; ++ti) for (int i = 0; i < aBasis.length; ++i) {
            rOut[ti][i] = rOut[0][i].threadSafeRef();
        }
        return rOut;
    }
    @SuppressWarnings("deprecation")
    private static NeuralNetwork[][] nnFromModelInfo_(Basis[] aBasis, int aThreadNum, Map<String, ?> aArgs, List<? extends Map<String, ?>> aModelInfos) throws Exception {
        @Nullable Object tObj = UT.Code.get(aArgs, "nn");
        if (tObj != null) throw new IllegalArgumentException("args of trainer can NOT contain `nn` for retraining");
        final int tTypeNum = aBasis.length;
        NeuralNetwork[][] rOut = new NeuralNetwork[aThreadNum][];
        rOut[0] = NeuralNetwork.load(aBasis, NewCollections.map(aModelInfos, info -> {
            Object tModelInfo = info.get("torch");
            if (tModelInfo != null) throw new IllegalArgumentException("nn type in Model info can NOT be torch");
            return info.get("nn");
        }));
        for (int i = 0; i < tTypeNum; ++i) {
            if (rOut[0][i] instanceof jsex.nnap.nn.TorchModel) throw new IllegalArgumentException("nn type in Model info can NOT be torch");
        }
        for (int ti = 1; ti < aThreadNum; ++ti) {
            rOut[ti] = new NeuralNetwork[tTypeNum];
            for (int i = 0; i < tTypeNum; ++i) {
                rOut[ti][i] = rOut[0][i].threadSafeRef();
            }
        }
        return rOut;
    }
    @SuppressWarnings("unchecked")
    private void initNormFromModelInfo_(List<? extends Map<String, ?>> aModelInfos) {
        for (int i = 0; i < mTypeNum; ++i) {
            mNormMu2[i].fill(0.0);
            mNormSigma2[i].fill(1.0);
        }
        Number tNormSigmaEng = null, tNormMuEng = null;
        for (int i = 0; i < mTypeNum; ++i) {
            Map<String, ?> tModelInfo = aModelInfos.get(i);
            // 现在优先读取 norm eng
            if (tNormSigmaEng == null) tNormSigmaEng = (Number)tModelInfo.get("norm_sigma_eng");
            if (tNormMuEng == null) tNormMuEng = (Number)tModelInfo.get("norm_mu_eng");
            if (mBasis[0][i] instanceof MirrorBasis) {
                // mirror 会强制这些额外值缺省
                Object tNormObj = UT.Code.get(tModelInfo, "norm_vec", "norm_sigma", "norm_mu");
                if (tNormObj != null) throw new IllegalArgumentException("norm_vec/norm_sigma/norm_mu in mirror ModelInfo MUST be empty");
            } else {
                List<? extends Number> tNormSigma = (List<? extends Number>)UT.Code.get(tModelInfo, "norm_sigma", "norm_vec");
                if (tNormSigma == null) throw new IllegalArgumentException("No norm_sigma/norm_vec in ModelInfo");
                mNormSigma[i].fill(tNormSigma);
                List<? extends Number> tNormMu = (List<? extends Number>)tModelInfo.get("norm_mu");
                if (tNormMu != null) {
                    mNormMu[i].fill(tNormMu);
                } else {
                    mNormMu[i].fill(0.0);
                }
            }
        }
        mNormSigmaEng = tNormSigmaEng==null ? 1.0 : tNormSigmaEng.doubleValue();
        mNormMuEng = tNormMuEng==null ? 0.0 : tNormMuEng.doubleValue();
        for (int i = 0; i < mTypeNum; ++i) if (mBasis[0][i] instanceof MirrorBasis) {
            int tMirrorIdx = ((MirrorBasis)mBasis[0][i]).mirrorType()-1;
            mNormMu[i] = mNormMu[tMirrorIdx];
            mNormSigma[i] = mNormSigma[tMirrorIdx];
        }
    }
    
    private String epochStr_(int aEpoch) {
        String tNEpochs = String.valueOf(mNEpochs);
        String tCEpochs = String.format("%0"+tNEpochs.length()+"d", aEpoch+1);
        return "epoch: "+tCEpochs;
    }
    private void initOptimizer_() {
        final int[] tLossDiv = {0};
        final double[] tLossTot = {0.0};
        mOptimizer.setParameter(mParas)
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
                        mSelectParas.setInternalDataSize(mParas.size());
                        mSelectParas.fill(mParas);
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
            if (mBatchSize > 0) return false; // 分 batch 情况永远不跳出，因为梯度随机
            if (step==0 || Double.isNaN(lastLoss)) return false;
            return Math.abs(lastLoss-loss) < Math.abs(lastLoss)*1e-7;
        });
    }
    
    @Override public int atomTypeNumber() {return mTypeNum;}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
    public Basis basis(int aType) {return mBasis[0][aType-1];}
    public @Unmodifiable List<Basis> basis() {return AbstractCollections.from(mBasis[0]);}
    public NeuralNetwork model(int aType) {return mNN[0][aType-1];}
    public @Unmodifiable List<NeuralNetwork> models() {return AbstractCollections.from(mNN[0]);}
    public String units() {return mUnits;}
    
    protected int paraShift(int aType) {
        int rShiftPara = 0;
        for (int typei = 0; typei < aType-1; ++typei) {
            rShiftPara += mNNParaSizes[typei];
        }
        return rShiftPara;
    }
    protected int basisParaShift(int aType) {
        int rShiftPara = mTotNNParaSize;
        for (int typei = 0; typei < aType-1; ++typei) {
            rShiftPara += mBasisParaSizes[typei];
        }
        return rShiftPara;
    }
    protected int normShift(int aType) {
        int rShiftPara = mTotNNParaSize;
        if (mTrainBasis) rShiftPara += mTotBasisParaSize;
        if (mShareNorm) return rShiftPara;
        for (int typei = 0; typei < aType-1; ++typei) {
            rShiftPara += 2*mBasisSizes[typei];
        }
        return rShiftPara;
    }
    
    
    @FunctionalInterface public interface ILossFunc {double call(double aPred, double aReal);}
    @FunctionalInterface public interface ILossFuncGrad {double call(double aPred, double aReal, DoubleWrapper rGrad);}
    
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
                        mLossFuncEng, mLossFuncGradEng,
                        mLossFuncForce, mLossFuncGradForce,
                        mLossFuncStress, mLossFuncGradStress);
    }
    protected void calMAE(boolean aTest, Vector rMAE) {
        calLoss_(aTest, aTest?mFullSliceTest:mFullSliceTrain, rMAE, null,
                 LOSS_ABSOLUTE, LOSS_ABSOLUTE_G,
                 LOSS_ABSOLUTE, LOSS_ABSOLUTE_G,
                 LOSS_ABSOLUTE, LOSS_ABSOLUTE_G);
        rMAE.multiply2this(mNormSigmaEng);
        rMAE.update(0, v -> v/mEnergyWeight);
        rMAE.update(1, v -> v/mForceWeight);
        rMAE.update(2, v -> v/mStressWeight);
    }
    private double calLoss_(boolean aTest, ISlice aSlice, @Nullable Vector rLossDetail, @Nullable Vector rGrad,
                            ILossFunc aLossFuncEng, ILossFuncGrad aLossFuncGradEng,
                            ILossFunc aLossFuncForce, ILossFuncGrad aLossFuncGradForce,
                            ILossFunc aLossFuncStress, ILossFuncGrad aLossFuncGradStress) {
        final DataSet tData = aTest ? mTestData : mTrainData;
        final boolean tRequireGrad = rGrad!=null;
        if (aTest && tRequireGrad) throw new IllegalStateException();
        final boolean tTrainBasis = mTrainBasis;
        final boolean tTrainNorm = mTrainNorm;
        final boolean tForceNormFormula = mForceNormFormula;
        if (tTrainNorm && tForceNormFormula) throw new IllegalStateException();
        final int tThreadNum = threadNumber();
        if (tTrainBasis && tForceNormFormula) {
            if (mBasisParaChanged) {
                // 这里简单每次调用重新计算 fp 并更新归一化系数
                initNormBasis();
                mBasisParaChanged = false;
            }
            if (tRequireGrad) {
                // 在这里初始化需要的缓存
                validFullBuf_(mHasForce||mHasStress);
                for (int ti = 0; ti < tThreadNum; ++ti) for (int i = 0; i < mTypeNum; ++i) {
                    mLossGradSigma[ti][i].fill(0.0);
                    mLossGradMu[ti][i].fill(0.0);
                }
            }
        }
        if (tRequireGrad) {
            mGradParaBuf[0] = rGrad;
            for (Vector tGradPara : mGradParaBuf) {
                tGradPara.setInternalDataSize(rGrad.size());
                tGradPara.fill(0.0);
            }
        }
        // 遍历统计总原子数
        final int tSliceSize = aSlice.size();
        int tAtomSize = 0;
        for (int si = 0; si < tSliceSize; ++si) {
            final int i = aSlice.get(si);
            tAtomSize += tData.mAtomType.get(i).size();
        }
        final int fAtomSize = tAtomSize;
        List<Vector> rLossPar = VectorCache.getZeros(4, tThreadNum);
        pool().parfor(tSliceSize, (si, threadID) -> {
            final int i = aSlice.get(si);
            Basis[] tBasis = mBasis[threadID];
            NeuralNetwork[] tNN = mNN[threadID];
            Vector rLoss = rLossPar.get(threadID);
            Vector tGradPara = mGradParaBuf[threadID];
            DoubleWrapper rLossGradEng = new DoubleWrapper(0.0);
            
            IntVector tAtomType = tData.mAtomType.get(i);
            int tAtomNum = tAtomType.size();
            IntList[] tNlType = tData.mNlType.get(i);
            DoubleList[] tNlDx = tData.mNlDx.get(i), tNlDy = tData.mNlDy.get(i), tNlDz = tData.mNlDz.get(i);
            
            List<List<Vector>> tHiddenOutputsBuf = mHiddenOutputsBuf.get(threadID);
            List<List<Vector>> tHiddenGradsBuf = mHiddenGradsBuf.get(threadID);
            List<DoubleList> tBaisForwardBuf = mBasisForwardAtomsBuf.get(threadID);
            DoubleList tBaisBackwardBuf = mBasisBackwardBuf[threadID];
            while (tBaisForwardBuf.size() < tAtomNum) tBaisForwardBuf.add(new DoubleList(128));
            
            Vector[] rLossGradMu = null, rLossGradSigma = null;
            Vector[] rLossGradFpFullBuf = null;
            if (tRequireGrad && tTrainBasis && tForceNormFormula) {
                rLossGradMu = mLossGradMu[threadID];
                rLossGradSigma = mLossGradSigma[threadID];
                rLossGradFpFullBuf = mLossGradFpFullBuf.get(i);
            }
            Vector[] tNormFp = mFpBuf[threadID];
            Vector[] tLossGradFp = mLossGradFp[threadID];
            List<List<Vector>> tFpBuf = mFpAtomsBuf.get(threadID);
            validFpAtomsBuf_(threadID, tAtomNum, mHasForce||mHasStress);
            if (tRequireGrad) validHiddenBuf_(threadID, tAtomType, mHasForce||mHasStress);
            
            if (!mHasForce && !mHasStress) {
                double rEng = 0.0;
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    Basis tSubBasis = tBasis[tType-1];
                    if (tSubBasis instanceof MirrorBasis) {
                        tType = ((MirrorBasis)tSubBasis).mirrorType();
                    }
                    Vector tSubFp = tFpBuf.get(tType-1).get(k);
                    Vector tSubNormFp = tNormFp[tType-1];
                    Vector tNormMu = mNormMu[tType-1];
                    Vector tNormSigma = mNormSigma[tType-1];
                    
                    tSubBasis.forward(tNlDx[k], tNlDy[k], tNlDz[k], tNlType[k], tSubFp, tBaisForwardBuf.get(k), tRequireGrad);
                    tSubNormFp.fill(ii -> (tSubFp.get(ii) - tNormMu.get(ii)) / tNormSigma.get(ii));
                    if (tTrainNorm) {
                        Vector tNormMu2 = mNormMu2[mShareNorm?0:(tType-1)];
                        Vector tNormSigma2 = mNormSigma2[mShareNorm?0:(tType-1)];
                        tSubNormFp.fill(ii -> (tSubNormFp.get(ii) - tNormMu2.get(ii)) / tNormSigma2.get(ii));
                    }
                    if (tNN[tType-1] instanceof FeedForward) {
                        FeedForward tSubNN = (FeedForward)tNN[tType-1];
                        rEng += tRequireGrad ? tSubNN.forward(tSubNormFp, tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k))
                                             : tSubNN.eval(tSubNormFp);
                    } else
                    if (tNN[tType-1] instanceof SharedFeedForward) {
                        SharedFeedForward tSubNN = (SharedFeedForward)tNN[tType-1];
                        rEng += tRequireGrad ? tSubNN.forward(tSubNormFp, tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k))
                                             : tSubNN.eval(tSubNormFp);
                    } else {
                        throw new IllegalStateException();
                    }
                }
                rEng /= tAtomNum;
                double tLossEng = tRequireGrad ? aLossFuncGradEng.call(rEng, (tData.mEng.get(i) - mNormMuEng)/mNormSigmaEng, rLossGradEng)
                                               : aLossFuncEng.call(rEng, (tData.mEng.get(i) - mNormMuEng)/mNormSigmaEng);
                rLoss.add(0, mEnergyWeight * tLossEng / tSliceSize);
                /// backward
                if (!tRequireGrad) return;
                double tLossGradEng = mEnergyWeight * rLossGradEng.value() / (tAtomNum*tSliceSize);
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    Basis tSubBasis = tBasis[tType-1];
                    if (tSubBasis instanceof MirrorBasis) {
                        tType = ((MirrorBasis)tSubBasis).mirrorType();
                    }
                    Vector tSubFp = tFpBuf.get(tType-1).get(k);
                    Vector tSubNormFp = tNormFp[tType-1];
                    Vector tNormMu = mNormMu[tType-1];
                    Vector tNormSigma = mNormSigma[tType-1];
                    tSubNormFp.fill(ii -> (tSubFp.get(ii) - tNormMu.get(ii)) / tNormSigma.get(ii));
                    if (tTrainNorm) {
                        Vector tNormMu2 = mNormMu2[mShareNorm?0:(tType-1)];
                        Vector tNormSigma2 = mNormSigma2[mShareNorm?0:(tType-1)];
                        tSubNormFp.fill(ii -> (tSubNormFp.get(ii) - tNormMu2.get(ii)) / tNormSigma2.get(ii));
                    }
                    Vector rSubLossGradFp = null;
                    if (tTrainBasis || tTrainNorm) {
                        rSubLossGradFp = tLossGradFp[tType-1];
                        rSubLossGradFp.fill(0.0);
                    }
                    int tShiftPara = paraShift(tType);
                    if (tNN[tType-1] instanceof FeedForward) {
                        FeedForward tSubNN = (FeedForward) tNN[tType-1];
                        tSubNN.backward(tLossGradEng, tSubNormFp, rSubLossGradFp, tGradPara.subVec(tShiftPara, tShiftPara+mNNParaSizes[tType-1]),
                                        tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k));
                    } else
                    if (tNN[tType-1] instanceof SharedFeedForward) {
                        SharedFeedForward tSubNN = (SharedFeedForward)tNN[tType-1];
                        int tSharedType = tSubNN.sharedType();
                        int tShiftSharedPara = paraShift(tSharedType);
                        tSubNN.backward(tLossGradEng, tSubNormFp, rSubLossGradFp, tGradPara.subVec(tShiftPara, tShiftPara+mNNParaSizes[tType-1]),
                                        tGradPara.subVec(tShiftSharedPara, tShiftSharedPara+mNNParaSizes[tSharedType-1]),
                                        tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k));
                    } else {
                        throw new IllegalStateException();
                    }
                    if (tTrainNorm) {
                        int tShiftNormPara = normShift(tType);
                        int tBasisSize = mBasisSizes[tType-1];
                        Vector tNormMu2 = mNormMu2[mShareNorm?0:(tType-1)];
                        Vector tNormSigma2 = mNormSigma2[mShareNorm?0:(tType-1)];
                        DoubleArrayVector tLossGradSigma = tGradPara.subVec(tShiftNormPara, tShiftNormPara+tBasisSize);
                        DoubleArrayVector tLossGradMu = tGradPara.subVec(tShiftNormPara+tBasisSize, tShiftNormPara+tBasisSize+tBasisSize);
                        for (int ii = 0; ii < tBasisSize; ++ii) {
                            double mu = tNormMu.get(ii);
                            double sigma = tNormSigma.get(ii);
                            double mu2 = tNormMu2.get(ii);
                            double sigma2 = tNormSigma2.get(ii);
                            double fp = tSubFp.get(ii);
                            double lossGradFp = rSubLossGradFp.get(ii);
                            fp = (fp - mu) / sigma;
                            tLossGradMu.add(ii, -lossGradFp/sigma2);
                            tLossGradSigma.add(ii, -lossGradFp*(fp-mu2)/(sigma2*sigma2));
                        }
                    }
                    if (tTrainBasis && !tForceNormFormula) {
                        if (tTrainNorm) {
                            rSubLossGradFp.div2this(mNormSigma2[mShareNorm?0:(tType-1)]);
                        }
                        rSubLossGradFp.div2this(tNormSigma);
                        int tBasisType = (tSubBasis instanceof SharedBasis) ? ((SharedBasis)tSubBasis).sharedType() : tType;
                        int tShiftBasisPara = basisParaShift(tBasisType);
                        tSubBasis.backward(tNlDx[k], tNlDy[k], tNlDz[k], tNlType[k], rSubLossGradFp,
                                           tGradPara.subVec(tShiftBasisPara, tShiftBasisPara+mBasisParaSizes[tBasisType-1]),
                                           tBaisForwardBuf.get(k), tBaisBackwardBuf, false);
                    }
                    if (tTrainBasis && tForceNormFormula) {
                        Vector rSubGradSigma = rLossGradSigma[mShareNorm?0:(tType-1)];
                        Vector rSubGradMu = rLossGradMu[mShareNorm?0:(tType-1)];
                        final int tSizeFp = mBasisSizes[tType-1];
                        for (int ii = 0; ii < tSizeFp; ++ii) {
                            double sigma = tNormSigma.get(ii);
                            double gradNormFp = rSubLossGradFp.get(ii);
                            double rhs1 = (tSubFp.get(ii)-tNormMu.get(ii)) / (sigma*sigma) * gradNormFp;
                            rSubGradSigma.add(ii, -rhs1);
                            double rhs2 = gradNormFp / sigma;
                            rSubGradMu.add(ii, -rhs2);
                        }
                        rSubLossGradFp.operation().div2dest(tNormSigma, rLossGradFpFullBuf[k]);
                    }
                }
                return;
            }
            IntList[] tNl = tData.mNl.get(i);
            double tVolume = tData.mVolume.get(i);
            
            DoubleWrapper rLossGradForceX = new DoubleWrapper(0.0), rLossGradForceY = new DoubleWrapper(0.0), rLossGradForceZ = new DoubleWrapper(0.0);
            DoubleWrapper rLossGradStressXX = new DoubleWrapper(0.0), rLossGradStressYY = new DoubleWrapper(0.0), rLossGradStressZZ = new DoubleWrapper(0.0);
            DoubleWrapper rLossGradStressXY = new DoubleWrapper(0.0), rLossGradStressXZ = new DoubleWrapper(0.0), rLossGradStressYZ = new DoubleWrapper(0.0);
            
            List<List<Vector>> tHiddenGrads2Buf = mHiddenGrads2Buf.get(threadID);
            List<List<Vector>> tHiddenGrads3Buf = mHiddenGrads3Buf.get(threadID);
            List<List<Vector>> tHiddenGradGradsBuf = mHiddenGradGradsBuf.get(threadID);
            List<DoubleList> tBaisForwardForceBuf = mBasisForwardForceAtomsBuf.get(threadID);
            DoubleList tBaisBackwardForceBuf = mBasisBackwardForceBuf[threadID];
            while (tBaisForwardForceBuf.size() < tAtomNum) tBaisForwardForceBuf.add(new DoubleList(128));
            
            DoubleList[] tBasisBackwardFullBuf = null;
            if (tRequireGrad && tTrainBasis && tForceNormFormula) {
                tBasisBackwardFullBuf = mBasisBackwardFullBuf.get(i);
            }
            Vector[] tGradFp = mGradFpBuf[threadID];
            List<List<Vector>> tGradNormFpBuf = mGradNormFpAtomsBuf.get(threadID);
            Vector[] tLossGradGradFp = mLossGradGradFp[threadID];
            
            DoubleList tForceXBuf = mForceXBuf[threadID];
            DoubleList tForceYBuf = mForceYBuf[threadID];
            DoubleList tForceZBuf = mForceZBuf[threadID];
            DoubleList tForceNlXBuf = mForceNlXBuf[threadID];
            DoubleList tForceNlYBuf = mForceNlYBuf[threadID];
            DoubleList tForceNlZBuf = mForceNlZBuf[threadID];
            DoubleList tLossGradForceXBuf = mLossGradForceXBuf[threadID];
            DoubleList tLossGradForceYBuf = mLossGradForceYBuf[threadID];
            DoubleList tLossGradForceZBuf = mLossGradForceZBuf[threadID];
            DoubleList tLossGradForceNlXBuf = mLossGradForceNlXBuf[threadID];
            DoubleList tLossGradForceNlYBuf = mLossGradForceNlYBuf[threadID];
            DoubleList tLossGradForceNlZBuf = mLossGradForceNlZBuf[threadID];
            if (mHasForce) {
                tForceXBuf.clear(); tForceXBuf.addZeros(tAtomNum);
                tForceYBuf.clear(); tForceYBuf.addZeros(tAtomNum);
                tForceZBuf.clear(); tForceZBuf.addZeros(tAtomNum);
            }
            double rEng = 0.0;
            double rStressXX = 0.0, rStressYY = 0.0, rStressZZ = 0.0, rStressXY = 0.0, rStressXZ = 0.0, rStressYZ = 0.0;
            for (int k = 0; k < tAtomNum; ++k) {
                int tType = tAtomType.get(k);
                Basis tSubBasis = tBasis[tType-1];
                if (tSubBasis instanceof MirrorBasis) {
                    tType = ((MirrorBasis)tSubBasis).mirrorType();
                }
                IntList tSubNl = tNl[k], tSubNlType = tNlType[k];
                DoubleList tSubNlDx = tNlDx[k], tSubNlDy = tNlDy[k], tSubNlDz = tNlDz[k];
                
                DoubleList tSubBaisForwardBuf = tBaisForwardBuf.get(k);
                Vector tSubFp = tFpBuf.get(tType-1).get(k);
                Vector tSubNormFp = tNormFp[tType-1];
                Vector tSubGradNormFp = tGradNormFpBuf.get(tType-1).get(k);
                Vector tSubGradFp = tGradFp[tType-1];
                Vector tNormMu = mNormMu[tType-1];
                Vector tNormSigma = mNormSigma[tType-1];
                
                tSubBasis.forward(tSubNlDx, tSubNlDy, tSubNlDz, tSubNlType, tSubFp, tSubBaisForwardBuf, true);
                tSubNormFp.fill(ii -> (tSubFp.get(ii) - tNormMu.get(ii)) / tNormSigma.get(ii));
                if (tTrainNorm) {
                    Vector tNormMu2 = mNormMu2[mShareNorm?0:(tType-1)];
                    Vector tNormSigma2 = mNormSigma2[mShareNorm?0:(tType-1)];
                    tSubNormFp.fill(ii -> (tSubNormFp.get(ii) - tNormMu2.get(ii)) / tNormSigma2.get(ii));
                }
                // cal energy
                if (tNN[tType-1] instanceof FeedForward) {
                    FeedForward tSubNN = (FeedForward)tNN[tType-1];
                    rEng += tRequireGrad ? tSubNN.forwardGrad(tSubNormFp, tSubGradNormFp, tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k),
                                                              tHiddenGrads2Buf.get(tType-1).get(k), tHiddenGrads3Buf.get(tType-1).get(k), tHiddenGradGradsBuf.get(tType-1).get(k))
                                         : tSubNN.evalGrad(tSubNormFp, tSubGradNormFp);
                } else
                if (tNN[tType-1] instanceof SharedFeedForward) {
                    SharedFeedForward tSubNN = (SharedFeedForward)tNN[tType-1];
                    rEng += tRequireGrad ? tSubNN.forwardGrad(tSubNormFp, tSubGradNormFp, tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k),
                                                              tHiddenGrads2Buf.get(tType-1).get(k), tHiddenGrads3Buf.get(tType-1).get(k), tHiddenGradGradsBuf.get(tType-1).get(k))
                                         : tSubNN.evalGrad(tSubNormFp, tSubGradNormFp);
                } else {
                    throw new IllegalStateException();
                }
                tSubGradNormFp.operation().div2dest(tNormSigma, tSubGradFp);
                if (tTrainNorm) {
                    tSubGradFp.div2this(mNormSigma2[mShareNorm?0:(tType-1)]);
                }
                // cal force
                int tNlSize = tSubNl.size();
                tForceNlXBuf.ensureCapacity(tNlSize); tForceNlXBuf.setInternalDataSize(tNlSize);
                tForceNlYBuf.ensureCapacity(tNlSize); tForceNlYBuf.setInternalDataSize(tNlSize);
                tForceNlZBuf.ensureCapacity(tNlSize); tForceNlZBuf.setInternalDataSize(tNlSize);
                tSubBasis.forwardForce(tSubNlDx, tSubNlDy, tSubNlDz, tSubNlType, tSubGradFp, tForceNlXBuf, tForceNlYBuf, tForceNlZBuf,
                                       tSubBaisForwardBuf, tBaisForwardForceBuf.get(k), tRequireGrad);
                for (int j = 0; j < tNlSize; ++j) {
                    double fx = tForceNlXBuf.get(j);
                    double fy = tForceNlYBuf.get(j);
                    double fz = tForceNlZBuf.get(j);
                    int nlk = tSubNl.get(j);
                    if (mHasForce) {
                        tForceXBuf.set(k, tForceXBuf.get(k) - fx);
                        tForceYBuf.set(k, tForceYBuf.get(k) - fy);
                        tForceZBuf.set(k, tForceZBuf.get(k) - fz);
                        tForceXBuf.set(nlk, tForceXBuf.get(nlk) + fx);
                        tForceYBuf.set(nlk, tForceYBuf.get(nlk) + fy);
                        tForceZBuf.set(nlk, tForceZBuf.get(nlk) + fz);
                    }
                    // cal stress here
                    if (mHasStress) {
                        double dx = tSubNlDx.get(j);
                        double dy = tSubNlDy.get(j);
                        double dz = tSubNlDz.get(j);
                        rStressXX += dx * fx; rStressYY += dy * fy; rStressZZ += dz * fz;
                        rStressXY += dx * fy; rStressXZ += dx * fz; rStressYZ += dy * fz;
                    }
                }
            }
            // cal stress here
            if (mHasStress) {
                rStressXX = -rStressXX/tVolume; rStressYY = -rStressYY/tVolume; rStressZZ = -rStressZZ/tVolume;
                rStressXY = -rStressXY/tVolume; rStressXZ = -rStressXZ/tVolume; rStressYZ = -rStressYZ/tVolume;
            }
            // energy error
            rEng /= tAtomNum;
            double tLossEng = tRequireGrad ? aLossFuncGradEng.call(rEng, (tData.mEng.get(i) - mNormMuEng)/mNormSigmaEng, rLossGradEng)
                                           : aLossFuncEng.call(rEng, (tData.mEng.get(i) - mNormMuEng)/mNormSigmaEng);
            rLoss.add(0, mEnergyWeight * tLossEng / tSliceSize);
            // force error
            if (mHasForce) {
                double tLossForce = 0.0;
                if (tRequireGrad) {
                    tLossGradForceXBuf.clear(); tLossGradForceXBuf.addZeros(tAtomNum);
                    tLossGradForceYBuf.clear(); tLossGradForceYBuf.addZeros(tAtomNum);
                    tLossGradForceZBuf.clear(); tLossGradForceZBuf.addZeros(tAtomNum);
                }
                double tForceLossMul = mForceWeight / (fAtomSize*3);
                Vector tForceX = tData.mForceX.get(i), tForceY = tData.mForceY.get(i), tForceZ = tData.mForceZ.get(i);
                for (int k = 0; k < tAtomNum; ++k) {
                    if (tRequireGrad) {
                        tLossForce += aLossFuncGradForce.call(tForceXBuf.get(k), tForceX.get(k)/mNormSigmaEng, rLossGradForceX);
                        tLossForce += aLossFuncGradForce.call(tForceYBuf.get(k), tForceY.get(k)/mNormSigmaEng, rLossGradForceY);
                        tLossForce += aLossFuncGradForce.call(tForceZBuf.get(k), tForceZ.get(k)/mNormSigmaEng, rLossGradForceZ);
                        tLossGradForceXBuf.set(k, tForceLossMul*rLossGradForceX.value());
                        tLossGradForceYBuf.set(k, tForceLossMul*rLossGradForceY.value());
                        tLossGradForceZBuf.set(k, tForceLossMul*rLossGradForceZ.value());
                    } else {
                        tLossForce += aLossFuncForce.call(tForceXBuf.get(k), tForceX.get(k)/mNormSigmaEng);
                        tLossForce += aLossFuncForce.call(tForceYBuf.get(k), tForceY.get(k)/mNormSigmaEng);
                        tLossForce += aLossFuncForce.call(tForceZBuf.get(k), tForceZ.get(k)/mNormSigmaEng);
                    }
                }
                rLoss.add(1, tForceLossMul*tLossForce);
            }
            // stress error
            if (mHasStress) {
                double tLossStress = 0.0;
                if (tRequireGrad) {
                    tLossStress += aLossFuncGradStress.call(rStressXX, tData.mStressXX.get(i)/mNormSigmaEng, rLossGradStressXX);
                    tLossStress += aLossFuncGradStress.call(rStressYY, tData.mStressYY.get(i)/mNormSigmaEng, rLossGradStressYY);
                    tLossStress += aLossFuncGradStress.call(rStressZZ, tData.mStressZZ.get(i)/mNormSigmaEng, rLossGradStressZZ);
                    tLossStress += aLossFuncGradStress.call(rStressXY, tData.mStressXY.get(i)/mNormSigmaEng, rLossGradStressXY);
                    tLossStress += aLossFuncGradStress.call(rStressXZ, tData.mStressXZ.get(i)/mNormSigmaEng, rLossGradStressXZ);
                    tLossStress += aLossFuncGradStress.call(rStressYZ, tData.mStressYZ.get(i)/mNormSigmaEng, rLossGradStressYZ);
                } else {
                    tLossStress += aLossFuncStress.call(rStressXX, tData.mStressXX.get(i)/mNormSigmaEng);
                    tLossStress += aLossFuncStress.call(rStressYY, tData.mStressYY.get(i)/mNormSigmaEng);
                    tLossStress += aLossFuncStress.call(rStressZZ, tData.mStressZZ.get(i)/mNormSigmaEng);
                    tLossStress += aLossFuncStress.call(rStressXY, tData.mStressXY.get(i)/mNormSigmaEng);
                    tLossStress += aLossFuncStress.call(rStressXZ, tData.mStressXZ.get(i)/mNormSigmaEng);
                    tLossStress += aLossFuncStress.call(rStressYZ, tData.mStressYZ.get(i)/mNormSigmaEng);
                }
                rLoss.add(2, mStressWeight*tLossStress/(tSliceSize*6));
            }
            /// backward
            if (!tRequireGrad) return;
            double tLossGradEng = mEnergyWeight * rLossGradEng.value() / (tAtomNum*tSliceSize);
            double tLossGradStressXX = 0.0, tLossGradStressYY = 0.0, tLossGradStressZZ = 0.0;
            double tLossGradStressXY = 0.0, tLossGradStressXZ = 0.0, tLossGradStressYZ = 0.0;
            if (mHasStress) {
                double tMul = -mStressWeight / (tVolume*tSliceSize*6);
                tLossGradStressXX = tMul * rLossGradStressXX.value();
                tLossGradStressYY = tMul * rLossGradStressYY.value();
                tLossGradStressZZ = tMul * rLossGradStressZZ.value();
                tLossGradStressXY = tMul * rLossGradStressXY.value();
                tLossGradStressXZ = tMul * rLossGradStressXZ.value();
                tLossGradStressYZ = tMul * rLossGradStressYZ.value();
            }
            for (int k = 0; k < tAtomNum; ++k) {
                int tType = tAtomType.get(k);
                Basis tSubBasis = tBasis[tType-1];
                if (tSubBasis instanceof MirrorBasis) {
                    tType = ((MirrorBasis)tSubBasis).mirrorType();
                }
                // force loss grad
                IntList tSubNl = tNl[k], tSubNlType = tNlType[k];
                DoubleList tSubNlDx = tNlDx[k], tSubNlDy = tNlDy[k], tSubNlDz = tNlDz[k];
                int tNlSize = tSubNl.size();
                double tLossGradForceX = 0.0, tLossGradForceY = 0.0, tLossGradForceZ = 0.0;
                if (mHasForce) {
                    tLossGradForceX = tLossGradForceXBuf.get(k);
                    tLossGradForceY = tLossGradForceYBuf.get(k);
                    tLossGradForceZ = tLossGradForceZBuf.get(k);
                }
                tLossGradForceNlXBuf.clear(); tLossGradForceNlXBuf.addZeros(tNlSize);
                tLossGradForceNlYBuf.clear(); tLossGradForceNlYBuf.addZeros(tNlSize);
                tLossGradForceNlZBuf.clear(); tLossGradForceNlZBuf.addZeros(tNlSize);
                for (int j = 0; j < tNlSize; ++j) {
                    int nlk = tSubNl.get(j);
                    double rLossGradForceNlX = 0.0, rLossGradForceNlY = 0.0, rLossGradForceNlZ = 0.0;
                    if (mHasForce) {
                        rLossGradForceNlX += tLossGradForceXBuf.get(nlk) - tLossGradForceX;
                        rLossGradForceNlY += tLossGradForceYBuf.get(nlk) - tLossGradForceY;
                        rLossGradForceNlZ += tLossGradForceZBuf.get(nlk) - tLossGradForceZ;
                    }
                    // stress loss grad here
                    if (mHasStress) {
                        double dx = tSubNlDx.get(j);
                        double dy = tSubNlDy.get(j);
                        double dz = tSubNlDz.get(j);
                        rLossGradForceNlX += dx*tLossGradStressXX;
                        rLossGradForceNlY += dy*tLossGradStressYY + dx*tLossGradStressXY;
                        rLossGradForceNlZ += dz*tLossGradStressZZ + dx*tLossGradStressXZ + dy*tLossGradStressYZ;
                    }
                    tLossGradForceNlXBuf.set(j, rLossGradForceNlX);
                    tLossGradForceNlYBuf.set(j, rLossGradForceNlY);
                    tLossGradForceNlZBuf.set(j, rLossGradForceNlZ);
                }
                DoubleArrayVector tLossGradBasisPara = null;
                if (tTrainBasis) {
                    int tBasisType = (tSubBasis instanceof SharedBasis) ? ((SharedBasis)tSubBasis).sharedType() : tType;
                    int tShiftBasisPara = basisParaShift(tBasisType);
                    tLossGradBasisPara = tGradPara.subVec(tShiftBasisPara, tShiftBasisPara+mBasisParaSizes[tBasisType-1]);
                }
                DoubleArrayVector tLossGradSigma = null, tLossGradMu = null;
                if (tTrainNorm) {
                    int tShiftNormPara = normShift(tType);
                    int tBasisSize = mBasisSizes[tType-1];
                    tLossGradSigma = tGradPara.subVec(tShiftNormPara, tShiftNormPara+tBasisSize);
                    tLossGradMu = tGradPara.subVec(tShiftNormPara+tBasisSize, tShiftNormPara+tBasisSize+tBasisSize);
                }
                DoubleList tSubBaisForwardBuf;
                if (tTrainBasis && tForceNormFormula) {
                    tSubBaisForwardBuf = tBasisBackwardFullBuf[k];
                } else {
                    tSubBaisForwardBuf = tBaisBackwardBuf;
                }
                Vector tSubFp = tFpBuf.get(tType-1).get(k);
                Vector tSubNormFp = tNormFp[tType-1];
                Vector tSubGradNormFp = tGradNormFpBuf.get(tType-1).get(k);
                Vector tSubGradFp = tGradFp[tType-1];
                Vector tNormMu = mNormMu[tType-1];
                Vector tNormSigma = mNormSigma[tType-1];
                tSubNormFp.fill(ii -> (tSubFp.get(ii) - tNormMu.get(ii)) / tNormSigma.get(ii));
                tSubGradNormFp.operation().div2dest(tNormSigma, tSubGradFp);
                if (tTrainNorm) {
                    Vector tNormMu2 = mNormMu2[mShareNorm?0:(tType-1)];
                    Vector tNormSigma2 = mNormSigma2[mShareNorm?0:(tType-1)];
                    tSubNormFp.fill(ii -> (tSubNormFp.get(ii) - tNormMu2.get(ii)) / tNormSigma2.get(ii));
                    tSubGradFp.div2this(tNormSigma2);
                }
                
                Vector tSubLossGradGradFp = tLossGradGradFp[tType-1];
                tSubLossGradGradFp.fill(0.0);
                tSubBasis.backwardForce(tSubNlDx, tSubNlDy, tSubNlDz, tSubNlType, tSubGradFp, tLossGradForceNlXBuf, tLossGradForceNlYBuf, tLossGradForceNlZBuf,
                                        tSubLossGradGradFp, tLossGradBasisPara, tBaisForwardBuf.get(k), tBaisForwardForceBuf.get(k), tSubBaisForwardBuf, tBaisBackwardForceBuf, false, !tTrainBasis);
                if (tTrainNorm) {
                    final int tBasisSize = mBasisSizes[tType-1];
                    Vector tNormSigma2 = mNormSigma2[mShareNorm?0:(tType-1)];
                    for (int ii = 0; ii < tBasisSize; ++ii) {
                        double sigma = tNormSigma.get(ii);
                        double sigma2 = tNormSigma2.get(ii);
                        double lossgradgradFp = tSubLossGradGradFp.get(ii);
                        double gradNormFp = tSubGradNormFp.get(ii);
                        tLossGradSigma.add(ii, -lossgradgradFp*gradNormFp/(sigma*sigma2*sigma2));
                    }
                }
                if (tTrainBasis && tForceNormFormula) {
                    Vector rSubGradSigma = rLossGradSigma[mShareNorm?0:(tType-1)];
                    final int tSizeFp = mBasisSizes[tType-1];
                    for (int ii = 0; ii < tSizeFp; ++ii) {
                        double sigma = tNormSigma.get(ii);
                        double rhs = tSubGradNormFp.get(ii) / (sigma*sigma) * tSubLossGradGradFp.get(ii);
                        rSubGradSigma.add(ii, -rhs);
                    }
                }
                if (tTrainNorm) {
                    tSubLossGradGradFp.div2this(mNormSigma2[mShareNorm?0:(tType-1)]);
                }
                tSubLossGradGradFp.div2this(tNormSigma);
                
                int tShiftPara = paraShift(tType);
                DoubleArrayVector tLossGradNNPara = tGradPara.subVec(tShiftPara, tShiftPara+mNNParaSizes[tType-1]);
                Vector rSubLossGradFp = null;
                if (tTrainBasis || tTrainNorm) {
                    rSubLossGradFp = tLossGradFp[tType-1];
                    rSubLossGradFp.fill(0.0);
                }
                if (tNN[tType-1] instanceof FeedForward) {
                    FeedForward tSubNN = (FeedForward) tNN[tType-1];
                    tSubNN.gradBackward(tSubLossGradGradFp, tSubNormFp, rSubLossGradFp, tLossGradNNPara,
                                        tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k),
                                        tHiddenGrads2Buf.get(tType-1).get(k), tHiddenGrads3Buf.get(tType-1).get(k), tHiddenGradGradsBuf.get(tType-1).get(k));
                    // energy loss grad
                    tSubNN.backward(tLossGradEng, tSubNormFp, rSubLossGradFp, tLossGradNNPara,
                                    tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k));
                } else
                if (tNN[tType-1] instanceof SharedFeedForward) {
                    SharedFeedForward tSubNN = (SharedFeedForward)tNN[tType-1];
                    int tSharedType = tSubNN.sharedType();
                    int tShiftSharedPara = paraShift(tSharedType);
                    DoubleArrayVector tLossGradNNSharedPara = tGradPara.subVec(tShiftSharedPara, tShiftSharedPara+mNNParaSizes[tSharedType-1]);
                    tSubNN.gradBackward(tSubLossGradGradFp, tSubNormFp, rSubLossGradFp, tLossGradNNPara, tLossGradNNSharedPara,
                                        tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k),
                                        tHiddenGrads2Buf.get(tType-1).get(k), tHiddenGrads3Buf.get(tType-1).get(k), tHiddenGradGradsBuf.get(tType-1).get(k));
                    // energy loss grad
                    tSubNN.backward(tLossGradEng, tSubNormFp, rSubLossGradFp, tLossGradNNPara, tLossGradNNSharedPara,
                                    tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k));
                } else {
                    throw new IllegalStateException();
                }
                if (tTrainNorm) {
                    int tBasisSize = mBasisSizes[tType-1];
                    Vector tNormMu2 = mNormMu2[mShareNorm?0:(tType-1)];
                    Vector tNormSigma2 = mNormSigma2[mShareNorm?0:(tType-1)];
                    for (int ii = 0; ii < tBasisSize; ++ii) {
                        double mu = tNormMu.get(ii);
                        double sigma = tNormSigma.get(ii);
                        double mu2 = tNormMu2.get(ii);
                        double sigma2 = tNormSigma2.get(ii);
                        double fp = tSubFp.get(ii);
                        double lossGradFp = rSubLossGradFp.get(ii);
                        fp = (fp - mu) / sigma;
                        tLossGradMu.add(ii, -lossGradFp/sigma2);
                        tLossGradSigma.add(ii, -lossGradFp*(fp-mu2)/(sigma2*sigma2));
                    }
                }
                if (tTrainBasis && !tForceNormFormula) {
                    if (tTrainNorm) {
                        rSubLossGradFp.div2this(mNormSigma2[mShareNorm?0:(tType-1)]);
                    }
                    rSubLossGradFp.div2this(tNormSigma);
                    tSubBasis.backward(tSubNlDx, tSubNlDy, tSubNlDz, tSubNlType, rSubLossGradFp,
                                       tLossGradBasisPara, tBaisForwardBuf.get(k), tSubBaisForwardBuf, true);
                }
                if (tTrainBasis && tForceNormFormula) {
                    Vector rSubGradSigma = rLossGradSigma[mShareNorm?0:(tType-1)];
                    Vector rSubGradMu = rLossGradMu[mShareNorm?0:(tType-1)];
                    final int tSizeFp = mBasisSizes[tType-1];
                    for (int ii = 0; ii < tSizeFp; ++ii) {
                        double sigma = tNormSigma.get(ii);
                        double gradNormFp = rSubLossGradFp.get(ii);
                        double rhs1 = (tSubFp.get(ii)-tNormMu.get(ii)) / (sigma*sigma) * gradNormFp;
                        rSubGradSigma.add(ii, -rhs1);
                        double rhs2 = gradNormFp / sigma;
                        rSubGradMu.add(ii, -rhs2);
                    }
                    rSubLossGradFp.operation().div2dest(tNormSigma, rLossGradFpFullBuf[k]);
                }
            }
        });
        // 如果没有固定归一化向量，在这里进行 train basis 归一化部分的反向传播
        if (tTrainBasis && tForceNormFormula && tRequireGrad) {
            final boolean tHasGradGrad = mHasForce||mHasStress;
            for (int ti = 1; ti < tThreadNum; ++ti) {
                for (int i = 0; i < mTypeNum; ++i) {
                    mLossGradSigma[0][i].plus2this(mLossGradSigma[ti][i]);
                    mLossGradMu[0][i].plus2this(mLossGradMu[ti][i]);
                }
            }
            // 简单遍历更新 grad sigma2
            for (int i = 0; i < mTypeNum; ++i) if (!mShareNorm || i==0) {
                mLossGradSigma[0][i].operation().operate2this(mNormSigma[i], (gs, s) -> gs / (2.0*s));
            }
            // 简单遍历更新 grad mu
            for (int i = 0; i < mTypeNum; ++i) if (!mShareNorm || i==0) {
                Vector tMu = mNormMu[i];
                Vector tLossGradSigma = mLossGradSigma[0][i];
                Vector rLossGradMu = mLossGradMu[0][i];
                final int tSizeFp = mBasisSizes[i];
                for (int ii = 0; ii < tSizeFp; ++ii) {
                    double rhs = 2.0 * tMu.get(ii) * tLossGradSigma.get(ii);
                    rLossGradMu.add(ii, -rhs);
                }
            }
            // 最后全部遍历更新 grad fp，并调用反向传播获取最终的梯度；现在这两步可以合并从而减少缓存使用
            pool().parfor(tSliceSize, (si, threadID) -> {
                final int i = aSlice.get(si);
                Basis[] tBasis = mBasis[threadID];
                IntVector tAtomType = tData.mAtomType.get(i);
                int tAtomNum = tAtomType.size();
                
                IntList[] tNlType = tData.mNlType.get(i);
                DoubleList[] tNlDx = tData.mNlDx.get(i), tNlDy = tData.mNlDy.get(i), tNlDz = tData.mNlDz.get(i);
                
                DoubleList tBaisForwardBuf = mBasisForwardBuf[threadID];
                DoubleList tBaisBackwardBuf = mBasisBackwardBuf[threadID];
                DoubleList[] tBasisBackwardFullBuf = null;
                if (tHasGradGrad) {
                    tBasisBackwardFullBuf = mBasisBackwardFullBuf.get(i);
                }
                
                Vector[] rFp = mFpBuf[threadID];
                Vector[] rLossGradFp = mLossGradFpFullBuf.get(i);
                Vector rGradPara = mGradParaBuf[threadID];
                
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    // 归一化系数同样对于 mirror 的会采用镜像的种类
                    Basis tSubBasis = tBasis[tType-1];
                    if (tSubBasis instanceof MirrorBasis) {
                        tType = ((MirrorBasis)tSubBasis).mirrorType();
                    }
                    // 由于全部缓存前向部分不实际，总是需要重新计算，因此这里就顺便也算基组
                    Vector rSubFp = rFp[tType-1];
                    IntList tSubNlType = tNlType[k];
                    DoubleList tSubNlDx = tNlDx[k], tSubNlDy = tNlDy[k], tSubNlDz = tNlDz[k];
                    tSubBasis.forward(tSubNlDx, tSubNlDy, tSubNlDz, tSubNlType, rSubFp, tBaisForwardBuf, true);
                    // 最后的 更新 grad fp
                    int tDivI = mNormDiv.get(mShareNorm?0:(tType-1));
                    Vector tLossGradSigma = mLossGradSigma[0][mShareNorm?0:(tType-1)];
                    Vector tLossGradMu = mLossGradMu[0][mShareNorm?0:(tType-1)];
                    Vector rSubLossGradFp = rLossGradFp[k];
                    final int tSizeFp = mBasisSizes[tType-1];
                    for (int ii = 0; ii < tSizeFp; ++ii) {
                        double rhs = (2.0*rSubFp.get(ii)*tLossGradSigma.get(ii) + tLossGradMu.get(ii)) / (double)tDivI;
                        rSubLossGradFp.add(ii, rhs);
                    }
                    // 反向传播
                    int tBasisType = (tSubBasis instanceof SharedBasis) ? ((SharedBasis)tSubBasis).sharedType() : tType;
                    int tShiftBasisPara = basisParaShift(tBasisType);
                    tSubBasis.backward(tSubNlDx, tSubNlDy, tSubNlDz, tSubNlType, rSubLossGradFp,
                                       rGradPara.subVec(tShiftBasisPara, tShiftBasisPara+mBasisParaSizes[tBasisType-1]),
                                       tBaisForwardBuf, tHasGradGrad?tBasisBackwardFullBuf[k]:tBaisBackwardBuf, tHasGradGrad);
                }
            });
        }
        if (tRequireGrad) {
            for (int ti = 1; ti < tThreadNum; ++ti) {
                rGrad.plus2this(mGradParaBuf[ti]);
            }
        }
        Vector rLoss = rLossPar.get(0);
        for (int ti = 1; ti < tThreadNum; ++ti) {
            rLoss.plus2this(rLossPar.get(ti));
        }
        // l2 loss here
        if (!aTest) {
            int tShift = 0;
            for (int i = 0; i < mTypeNum; ++i) {
                IVector tParas = mNNParas[i];
                int tWeightSize = mNNParaWeightSizes[i];
                double tMul = mL2LossWeight/MathEX.Fast.sqrt(tWeightSize);
                for (int j = 0; j < tWeightSize; ++j) {
                    double tSubPara = tParas.get(j);
                    rLoss.add(3, tMul*(tSubPara*tSubPara));
                    if (tRequireGrad) rGrad.add(tShift+j, 2.0*tMul*tSubPara);
                }
                tShift += mNNParaSizes[i];
            }
        }
        if (rLossDetail != null) {
            rLossDetail.fill(rLoss);
        }
        double tLoss = rLoss.sum();
        VectorCache.returnVec(rLossPar);
        return tLoss;
    }
    
    
    @ApiStatus.Internal
    protected void calNlAndAddData_(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress, DataSet rData) {
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        final int tAtomNum = aAtomData.atomNumber();
        // 添加数据时按照种类进行排序，从而提高缓存命中率
        for (int ti = 0; ti < mTypeNum; ++ti) {
            mTypeIlist[ti].clear();
        }
        for (int i = 0; i < tAtomNum; ++i) {
            int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
            mTypeIlist[tType-1].add(i);
        }
        // 统一初始化 type
        IntList rAtomType = new IntList(tAtomNum);
        // 这里简单处理，直接重新构造新的 atomdata
        AtomDataBuilder<AtomData> tBuilder = AtomData.builder().setBox(aAtomData.box()).setAtomTypeNumber(aAtomData.atomTypeNumber());
        for (int ti = 0; ti < mTypeNum; ++ti) {
            IntList tSubIlist = mTypeIlist[ti];
            int tSubSize = tSubIlist.size();
            for (int ii = 0; ii < tSubSize; ++ii) {
                int i = tSubIlist.get(ii);
                tBuilder.add(aAtomData.atom(i), ti+1);
                rAtomType.add(ti+1);
            }
            // 计算相对能量值
            aEnergy -= tSubSize*mRefEngs.get(ti);
        }
        rData.mAtomType.add(rAtomType.asVec());
        AtomData tAtomData = tBuilder.build();
        // 再遍历一次初始化近邻列表
        IntList[] rNl = new IntList[tAtomNum];
        IntList[] rNlType = new IntList[tAtomNum];
        DoubleList[] rNlDx = new DoubleList[tAtomNum], rNlDy = new DoubleList[tAtomNum], rNlDz = new DoubleList[tAtomNum];
        rData.mNl.add(rNl);
        rData.mNlType.add(rNlType);
        rData.mNlDx.add(rNlDx); rData.mNlDy.add(rNlDy); rData.mNlDz.add(rNlDz);
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(tAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = rAtomType.get(i);
                // 增加近邻列表，这里直接重新添加
                IntList tNl = new IntList(16);
                IntList tNlType = new IntList(16);
                DoubleList tNlDx = new DoubleList(16);
                DoubleList tNlDy = new DoubleList(16);
                DoubleList tNlDz = new DoubleList(16);
                tAPC.nl_().forEachNeighbor(i, basis(tType).rcut(), (dx, dy, dz, idx) -> {
                    tNl.add(idx);
                    tNlType.add(rAtomType.get(idx));
                    tNlDx.add(dx); tNlDy.add(dy); tNlDz.add(dz);
                });
                tNl.trimToSize(); rNl[i] = tNl;
                tNlType.trimToSize(); rNlType[i] = tNlType;
                tNlDx.trimToSize(); rNlDx[i] = tNlDx;
                tNlDy.trimToSize(); rNlDy[i] = tNlDy;
                tNlDz.trimToSize(); rNlDz[i] = tNlDz;
            }
        }
        // 添加能量
        rData.mEng.add(aEnergy/tAtomNum);
        // 添加力，注意需要同样调整顺序
        if (mHasForce) {
            assert aForces != null;
            DoubleList rForceX = new DoubleList(tAtomNum);
            DoubleList rForceY = new DoubleList(tAtomNum);
            DoubleList rForceZ = new DoubleList(tAtomNum);
            for (int ti = 0; ti < mTypeNum; ++ti) {
                IntList tSubIlist = mTypeIlist[ti];
                int tSubSize = tSubIlist.size();
                for (int ii = 0; ii < tSubSize; ++ii) {
                    int i = tSubIlist.get(ii);
                    rForceX.add(aForces.get(i, 0));
                    rForceY.add(aForces.get(i, 1));
                    rForceZ.add(aForces.get(i, 2));
                }
            }
            rData.mForceX.add(rForceX.asVec());
            rData.mForceY.add(rForceY.asVec());
            rData.mForceZ.add(rForceZ.asVec());
        }
        // 应力
        if (mHasStress) {
            assert aStress != null;
            rData.mStressXX.add(aStress.get(0));
            rData.mStressYY.add(aStress.get(1));
            rData.mStressZZ.add(aStress.get(2));
            rData.mStressXY.add(aStress.get(3));
            rData.mStressXZ.add(aStress.get(4));
            rData.mStressYZ.add(aStress.get(5));
        }
        ++rData.mSize;
        rData.mVolume.add(tAtomData.volume());
    }
    
    /**
     * 增加一个训练集数据
     * <p>
     * 目前方便起见，如果有力则所有数据统一都要有力
     * @param aAtomData 原子结构数据
     * @param aEnergy 此原子结构数据的总能量
     * @param aForces 可选的每个原子的力，按行排列，每列对应 x,y,z 方向的力
     * @param aStress 可选的原子结构数据的应力值，按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列
     * @see IAtomData
     * @see IMatrix
     */
    public void addTrainData(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress) {
        if (!mHasData) {
            mHasData = true;
            mHasForce = aForces!=null;
            mHasStress = aStress!=null;
        } else {
            if (mHasForce && aForces==null) throw new IllegalArgumentException("All data MUST has forces when add force");
            if (!mHasForce && aForces!=null) throw new IllegalArgumentException("All data MUST NOT has forces when not add force");
            if (mHasStress && aStress==null) throw new IllegalArgumentException("All data MUST has stress when add stress");
            if (!mHasStress && aStress!=null) throw new IllegalArgumentException("All data MUST NOT has stress when not add stress");
        }
        // 添加数据
        calNlAndAddData_(aAtomData, aEnergy, aForces, aStress, mTrainData);
        // 训练集发生修改，标记缓存失效
        mFullBufInit = false;
    }
    /**
     * {@code addTrainData(aAtomData, aEnergy, aForces, null)}
     * @see #addTrainData(IAtomData, double, IMatrix, IVector)
     */
    public void addTrainData(IAtomData aAtomData, double aEnergy, IMatrix aForces) {addTrainData(aAtomData, aEnergy, aForces, null);}
    /**
     * {@code addTrainData(aAtomData, aEnergy, null, null)}
     * @see #addTrainData(IAtomData, double, IMatrix, IVector)
     */
    public void addTrainData(IAtomData aAtomData, double aEnergy) {addTrainData(aAtomData, aEnergy, null, null);}
    /**
     * 增加一个测试集数据
     * <p>
     * 目前方便起见，如果有力则所有数据统一都要有力
     * @param aAtomData 原子结构数据
     * @param aEnergy 此原子结构数据的总能量
     * @param aForces 可选的每个原子的力，按行排列，每列对应 x,y,z 方向的力
     * @param aStress 可选的原子结构数据的应力值，按照 {@code [xx, yy, zz, xy, xz, yz]} 顺序排列
     * @see IAtomData
     * @see IMatrix
     */
    public void addTestData(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress) {
        if (!mHasData) {
            mHasData = true;
            mHasForce = aForces!=null;
            mHasStress = aStress!=null;
        } else {
            if (mHasForce && aForces==null) throw new IllegalArgumentException("All data MUST has forces when add force");
            if (!mHasForce && aForces!=null) throw new IllegalArgumentException("All data MUST NOT has forces when not add force");
            if (mHasStress && aStress==null) throw new IllegalArgumentException("All data MUST has stress when add stress");
            if (!mHasStress && aStress!=null) throw new IllegalArgumentException("All data MUST NOT has stress when not add stress");
        }
        // 添加数据
        calNlAndAddData_(aAtomData, aEnergy, aForces, aStress, mTestData);
        if (!mHasTest) mHasTest = true;
    }
    /**
     * {@code addTestData(aAtomData, aEnergy, aForces, null)}
     * @see #addTestData(IAtomData, double, IMatrix, IVector)
     */
    public void addTestData(IAtomData aAtomData, double aEnergy, IMatrix aForces) {addTestData(aAtomData, aEnergy, aForces, null);}
    /**
     * {@code addTestData(aAtomData, aEnergy, null, null)}
     * @see #addTestData(IAtomData, double, IMatrix, IVector)
     */
    public void addTestData(IAtomData aAtomData, double aEnergy) {addTestData(aAtomData, aEnergy, null, null);}
    
    /** 获取历史 loss 值 */
    public IVector trainLoss() {return mTrainLoss.asVec();}
    public IVector testLoss() {return mTestLoss.asVec();}
    
    protected void fuseNormBasis() {
        for (int i = 0; i < mTypeNum; ++i) {
            Vector tMu = mNormMu[i], tSigma = mNormSigma[i];
            Vector tMu2 = mNormMu2[mShareNorm?0:i], tSigma2 = mNormSigma2[mShareNorm?0:i];
            final int tBasisSize = mBasisSizes[i];
            for (int ii = 0; ii < tBasisSize; ++ii) {
                double mu = tMu.get(ii); double sigma = tSigma.get(ii);
                double mu2 = tMu2.get(ii); double sigma2 = tSigma2.get(ii);
                tMu.set(ii, mu + mu2*sigma);
                tSigma.set(ii, sigma*sigma2);
            }
        }
        for (int i = 0; i < mTypeNum; ++i) {
            mNormMu2[i].fill(0.0);
            mNormSigma2[i].fill(1.0);
        }
    }
    protected void initNormBasis() {
        for (int i = 0; i < mTypeNum; ++i) {
            mNormMu[i].fill(0.0);
            mNormSigma[i].fill(0.0);
            mNormMu2[i].fill(0.0);
            mNormSigma2[i].fill(1.0);
        }
        mNormDiv.fill(0);
        final int tThreadNum = threadNumber();
        Vector[][] tMuPar = new Vector[tThreadNum][mTypeNum];
        Vector[][] tSigmaPar = new Vector[tThreadNum][mTypeNum];
        IntVector[] tDivPar = new IntVector[tThreadNum];
        tMuPar[0] = mNormMu;
        tSigmaPar[0] = mNormSigma;
        tDivPar[0] = mNormDiv;
        for (int ti = 1; ti < tThreadNum; ++ti) {
            for (int i = 0; i < mTypeNum; ++i) {
                tMuPar[ti][i] = VectorCache.getZeros(mBasisSizes[i]);
                tSigmaPar[ti][i] = VectorCache.getZeros(mBasisSizes[i]);
            }
            tDivPar[ti] = IntVectorCache.getZeros(mTypeNum);
        }
        pool().parfor(mTrainData.mSize, (i, threadID) -> {
            Basis[] tBasis = mBasis[threadID];
            IntVector tAtomType = mTrainData.mAtomType.get(i);
            int tAtomNum = tAtomType.size();
            
            IntList[] tNlType = mTrainData.mNlType.get(i);
            DoubleList[] tNlDx = mTrainData.mNlDx.get(i), tNlDy = mTrainData.mNlDy.get(i), tNlDz = mTrainData.mNlDz.get(i);
            
            Vector[] tFp = mFpBuf[threadID];
            Vector[] tNormMu = tMuPar[threadID];
            Vector[] tNormSigma = tSigmaPar[threadID];
            IntVector tDiv = tDivPar[threadID];
            
            for (int k = 0; k < tAtomNum; ++k) {
                int tType = tAtomType.get(k);
                // 这里需要考虑 mirror 的情况，对于 mirror 的同时和对应的数据一起公用归一化向量
                Basis tSubBasis = tBasis[tType-1];
                if (tSubBasis instanceof MirrorBasis) {
                    tType = ((MirrorBasis)tSubBasis).mirrorType();
                }
                // 现在实时计算基组而不是缓存
                Vector tSubFp = tFp[tType-1];
                tSubBasis.eval(tNlDx[k], tNlDy[k], tNlDz[k], tNlType[k], tSubFp);
                // 统计归一化系数
                tNormMu[mShareNorm?0:(tType-1)].plus2this(tSubFp);
                tNormSigma[mShareNorm?0:(tType-1)].operation().operate2this(tSubFp, (lhs, rhs) -> lhs + rhs * rhs);
                tDiv.increment(mShareNorm?0:(tType-1));
            }
        });
        for (int ti = 1; ti < tThreadNum; ++ti) {
            for (int i = 0; i < mTypeNum; ++i) {
                mNormMu[i].plus2this(tMuPar[ti][i]);
                mNormSigma[i].plus2this(tSigmaPar[ti][i]);
                VectorCache.returnVec(tMuPar[ti][i]);
                VectorCache.returnVec(tSigmaPar[ti][i]);
            }
            mNormDiv.plus2this(tDivPar[ti]);
            IntVectorCache.returnVec(tDivPar[ti]);
        }
        for (int i = 0; i < mTypeNum; ++i) if ((mShareNorm && i==0) || (!mShareNorm && !(mBasis[0][i] instanceof MirrorBasis))) {
            int tDivI = mNormDiv.get(i);
            if (tDivI == 0) {
                UT.Code.warning("number of atoms of type `"+mSymbols[i]+"` is zero, check your input or dataset.");
                mNormMu[i].fill(0.0);
                mNormSigma[i].fill(1.0);
            } else {
                mNormMu[i].div2this(tDivI);
                mNormSigma[i].div2this(tDivI);
                mNormSigma[i].operation().operate2this(mNormMu[i], (lhs, rhs) -> lhs - rhs*rhs);
                mNormSigma[i].operation().map2this(v -> {
                    double rOut;
                    if (MathEX.Code.numericEqual(v, 0.0)) {
                        if (mTrainBasis && mForceNormFormula) {
                            throw new IllegalArgumentException("The input basis causes ill normalization sigma, which can lead to incorrect results when optimizing the basis with force formula normalization coefficient.\n" +
                                                               "Check your input or dataset or turn-off it with `setForceNormFormula(false)`");
                        }
                        rOut = 1.0;
                    } else {
                        rOut = MathEX.Fast.sqrt(v);
                    }
                    return rOut;
                });
            }
        }
        for (int i = 0; i < mTypeNum; ++i) if ((mShareNorm && i!=0) || (!mShareNorm && (mBasis[0][i] instanceof MirrorBasis))) {
            int tMirrorIdx = mShareNorm ? 0 : (((MirrorBasis)mBasis[0][i]).mirrorType()-1);
            mNormMu[i].fill(mNormMu[tMirrorIdx]);
            mNormSigma[i].fill(mNormSigma[tMirrorIdx]);
        }
    }
    protected void initNormEng() {
        // 这里采用中位数和上下四分位数来归一化能量
        Vector tSortedEng = mTrainData.mEng.copy2vec();
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
        mOptimizer.markLossFuncChanged();
    }
    
    /** 开始训练模型，这里直接训练给定的步数 */
    public void train(int aNEpochs, boolean aEarlyStop, boolean aPrintLog) {
        mNEpochs = aNEpochs;
        // 清空旧的早停存储
        mMinLoss = Double.POSITIVE_INFINITY;
        // 初始化归一化参数，现在只会初始化一次
        if (!mNormInit) {
            if (aPrintLog) System.out.println("Init train data...");
            initNormEng();
            initNormBasis();
            mNormInit = true;
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
        if (aPrintLog) {
            // 只会在不分 batch 时需要补全进度条
            if (mBatchSize <= 0) for (int i = mEpoch + 1; i < aNEpochs; ++i) {
                UT.Timer.progressBar(mHasTest ? String.format("loss: %.4g | %.4g", mTrainLoss.last(), mTestLoss.last()) : String.format("loss: %.4g", mTrainLoss.last()));
            }
        }
        // 应用早停
        if (aEarlyStop && mSelectEpoch>=0) {
            mParas.fill(mSelectParas);
            mOptimizer.markParameterChanged();
            if (aPrintLog) System.out.printf("Model at epoch = %d selected, test loss = %.4g\n", mSelectEpoch+1, mMinLoss);
            mSelectEpoch = -1;
            mMinLoss = Double.POSITIVE_INFINITY;
        }
        // 应用合并归一化系数
        if (mTrainNorm) {
            fuseNormBasis();
            mOptimizer.markLossFuncChanged();
        }
        // 打印训练结果信息
        if (!aPrintLog) return;
        Vector tLossDetail = VectorCache.getVec(4);
        double tLossTot = calLossDetail(false, tLossDetail);
        double tLossE = tLossDetail.get(0);
        double tLossF = tLossDetail.get(1);
        double tLossS = tLossDetail.get(2);
        double tLossL2 = tLossDetail.get(3);
        VectorCache.returnVec(tLossDetail);
        System.out.printf("Loss-L2: %.4g (%s)\n", tLossL2, IO.Text.percent(tLossL2/tLossTot));
        System.out.printf("Loss-E : %.4g (%s)\n", tLossE, IO.Text.percent(tLossE/tLossTot));
        if (mHasForce) {
            System.out.printf("Loss-F : %.4g (%s)\n", tLossF, IO.Text.percent(tLossF/tLossTot));
        }
        if (mHasStress) {
            System.out.printf("Loss-S : %.4g (%s)\n", tLossS, IO.Text.percent(tLossS/tLossTot));
        }
        Vector tMAE = VectorCache.getVec(4);
        calMAE(false, tMAE);
        double tMAE_E = tMAE.get(0);
        double tMAE_F = tMAE.get(1);
        double tMAE_S = tMAE.get(2);
        VectorCache.returnVec(tMAE);
        if (!mHasTest) {
            switch(mUnits) {
            case "metal": {
                System.out.printf("MAE-E: %.4g meV\n", tMAE_E*1000);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g meV/A\n", tMAE_F*1000);
                }
                if (mHasStress) {
                    System.out.printf("MAE-S: %.4g meV/A^3\n", tMAE_S*1000);
                }
                break;
            }
            case "real":{
                System.out.printf("MAE-E: %.4g kcal/mol\n", tMAE_E);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g kcal/mol/A\n", tMAE_F);
                }
                if (mHasStress) {
                    System.out.printf("MAE-S: %.4g kcal/mol/A^3\n", tMAE_S);
                }
                break;
            }
            default: {
                System.out.printf("MAE-E: %.4g\n", tMAE_E);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g\n", tMAE_F);
                }
                if (mHasStress) {
                    System.out.printf("MAE-S: %.4g\n", tMAE_S);
                }
                break;
            }}
            return;
        }
        Vector tTestMAE = VectorCache.getVec(4);
        calMAE(true, tTestMAE);
        double tTestMAE_E = tTestMAE.get(0);
        double tTestMAE_F = tTestMAE.get(1);
        double tTestMAE_S = tTestMAE.get(2);
        VectorCache.returnVec(tTestMAE);
        switch(mUnits) {
        case "metal": {
            System.out.printf("MAE-E: %.4g meV | %.4g meV\n", tMAE_E*1000, tTestMAE_E*1000);
            if (mHasForce) {
                System.out.printf("MAE-F: %.4g meV/A | %.4g meV/A\n", tMAE_F*1000, tTestMAE_F*1000);
            }
            if (mHasStress) {
                System.out.printf("MAE-S: %.4g meV/A^3 | %.4g meV/A^3\n", tMAE_S*1000, tTestMAE_S*1000);
            }
            break;
        }
        case "real":{
            System.out.printf("MAE-E: %.4g kcal/mol | %.4g kcal/mol\n", tMAE_E, tTestMAE_E);
            if (mHasForce) {
                System.out.printf("MAE-F: %.4g kcal/mol/A | %.4g kcal/mol/A\n", tMAE_F, tTestMAE_F);
            }
            if (mHasStress) {
                System.out.printf("MAE-S: %.4g kcal/mol/A^3 | %.4g kcal/mol/A^3\n", tMAE_S, tTestMAE_S);
            }
            break;
        }
        default: {
            System.out.printf("MAE-E: %.4g | %.4g\n", tMAE_E, tTestMAE_E);
            if (mHasForce) {
                System.out.printf("MAE-F: %.4g | %.4g\n", tMAE_F, tTestMAE_F);
            }
            if (mHasStress) {
                System.out.printf("MAE-S: %.4g | %.4g\n", tMAE_S, tTestMAE_S);
            }
            break;
        }}
    }
    public void train(int aEpochs, boolean aEarlyStop) {train(aEpochs, aEarlyStop, true);}
    public void train(int aEpochs) {train(aEpochs, true);}
    
    
    /** 保存训练的势函数 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        List rModels = new ArrayList();
        for (int i = 0; i < mTypeNum; ++i) {
            Map rBasis = new LinkedHashMap();
            mBasis[0][i].save(rBasis);
            Map rModel = new LinkedHashMap();
            rModel.put("symbol", mSymbols[i]);
            rModel.put("basis", rBasis);
            if (i == 0) {
                rModel.put("norm_mu_eng", mNormMuEng);
                rModel.put("norm_sigma_eng", mNormSigmaEng);
            }
            if (mBasis[0][i] instanceof MirrorBasis) {
                rModels.add(rModel);
                continue;
            }
            Map rNN = new LinkedHashMap();
            mNN[0][i].save(rNN);
            rModel.put("ref_eng", mRefEngs.get(i));
            rModel.put("norm_mu", mNormMu[i].asList());
            rModel.put("norm_sigma", mNormSigma[i].asList());
            rModel.put("nn", rNN);
            rModels.add(rModel);
        }
        rSaveTo.put("version", NNAP.VERSION);
        rSaveTo.put("units", mUnits);
        rSaveTo.put("models", rModels);
    }
    @SuppressWarnings({"rawtypes"})
    public void save(String aPath, boolean aPretty) throws IOException {
        Map rJson = new LinkedHashMap();
        save(rJson);
        IO.map2json(rJson, aPath, aPretty);
    }
    public void save(String aPath) throws IOException {save(aPath, false);}
    
    /** 转换旧版势函数到新的版本的工具方法 */
    @SuppressWarnings("deprecation")
    @VisibleForTesting public static void convert(String aOldPath, String aNewPath) throws IOException {
        TrainerTorch.convert(aOldPath, aNewPath);
    }
}
