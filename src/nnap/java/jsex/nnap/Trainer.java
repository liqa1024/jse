package jsex.nnap;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IAtomData;
import jse.atom.IHasSymbol;
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
import jse.opt.IOptimizer;
import jse.opt.LBFGS;
import jse.parallel.AbstractThreadPool;
import jse.parallel.ParforThreadPool;
import jsex.nnap.basis.*;
import jsex.nnap.nn.FeedForward;
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
        public int mAtomSize = 0;
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
        /** 近邻原子坐标差，每个原子结构一组，每个原子对应一个近邻列表向量 */
        public final List<DoubleList[]> mNlDx = new ArrayList<>(64), mNlDy = new ArrayList<>(64), mNlDz = new ArrayList<>(64);
        /** 每个原子数据结构对应的总体积值 */
        public final DoubleList mVolume = new DoubleList(64);
    }
    
    protected final IOptimizer mOptimizer;
    protected final int mTypeNum;
    protected final String[] mSymbols;
    protected final IVector mRefEngs;
    protected final Basis[][] mBasis;
    protected final FeedForward[][] mNN;
    protected final DataSet mTrainData;
    protected final DataSet mTestData;
    protected boolean mHasData = false;
    protected boolean mHasForce = false;
    protected boolean mHasStress = false;
    protected boolean mHasTest = false;
    protected final Vector[] mNormMu, mNormSigma;
    protected double mNormMuEng = 0.0, mNormSigmaEng = 0.0;
    protected final DoubleList mTrainLoss = new DoubleList(64);
    protected final DoubleList mTestLoss = new DoubleList(64);
    protected boolean mNormInit = false;
    
    private final int mTotNNParaSize, mTotBasisParaSize;
    private final int[] mNNParaSizes, mNNParaWeightSizes, mHiddenSizes;
    private final int[] mBasisParaSizes;
    private final IVector[] mNNParas, mBasisParas;
    private final IVector mParas;
    
    protected int mEpoch = -1;
    protected int mSelectEpoch = -1;
    protected double mMinLoss = Double.POSITIVE_INFINITY;
    protected final Vector mSelectParas;
    
    
    /// buffer stuffs
    private final Vector[] mGradParaBuf;
    private final List<List<List<Vector>>> mHiddenOutputsBuf, mHiddenGradsBuf, mHiddenGrads2Buf, mHiddenGrads3Buf, mHiddenGradGradsBuf;
    private final List<List<DoubleList>> mBasisForwardBuf, mBasisForwardForceBuf;
    private final DoubleList[] mBasisBackwardBuf, mBasisBackwardForceBuf;
    
    private final List<List<List<Vector>>> mFpBuf, mGradFpBuf;
    private final DoubleList[] mForceNlXBuf, mForceNlYBuf, mForceNlZBuf;
    private final DoubleList[] mForceXBuf, mForceYBuf, mForceZBuf;
    private final DoubleList[] mLossGradForceXBuf, mLossGradForceYBuf, mLossGradForceZBuf;
    private final DoubleList[] mLossGradForceNlXBuf, mLossGradForceNlYBuf, mLossGradForceNlZBuf;
    
    private final IntList[] mNlType;
    private final Vector[][] mNormFp;
    private final Vector[][] mLossGradFp, mLossGradGradFp;
    
    
    protected boolean mTrainBasis = false;
    public Trainer setTrainBasis(boolean aFlag) {
        if (mTrainBasis == aFlag) return this;
        mTrainBasis = aFlag;
        mOptimizer.markLossFuncChanged();
        mOptimizer.markParameterChanged();
        return this;
    }
    
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
    
    private void validFpBuf_(int aThreadID, int aAtomNum, boolean aRequireGradBackward) {
        List<List<Vector>> tFpBuf = mFpBuf.get(aThreadID);
        List<List<Vector>> tGradFpBuf = mGradFpBuf.get(aThreadID);
        for (int typei = 0; typei < mTypeNum; ++typei) if (!(mBasis[0][typei] instanceof Mirror)) {
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
        for (int typei = 0; typei < mTypeNum; ++typei) if (!(mBasis[0][typei] instanceof Mirror)) {
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
                if (mBasis[0][typeii] instanceof Mirror) {
                    typeii = ((Mirror) mBasis[0][typeii]).mirrorType() - 1;
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
    
    Trainer(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber, Map<String, ?> aArgs) {
        super(new ParforThreadPool(aThreadNumber));
        mSymbols = symbolsFrom_(aArgs);
        mTypeNum = mSymbols.length;
        mRefEngs = refEngsFrom_(mTypeNum, aArgs);
        mBasis = basisFrom_(mSymbols, aThreadNumber, aArgs);
        FeedForward[] tNN = nnFrom_(mBasis[0], aArgs);
        mOptimizer = new LBFGS(100).setLineSearch();
        
        if (mTypeNum != mRefEngs.size()) throw new IllegalArgumentException("Symbols length does not match reference energies length.");
        if (mTypeNum != mBasis[0].length) throw new IllegalArgumentException("Symbols length does not match reference basis length.");
        if (mTypeNum != tNN.length) throw new IllegalArgumentException("Symbols length does not match neural network length.");
        
        // 简单遍历 basis 验证 mirror 的情况
        for (int i = 0; i < mSymbols.length; ++i) if (mBasis[0][i] instanceof Mirror) {
            Mirror tBasis = (Mirror)mBasis[0][i];
            int tMirrorType = tBasis.mirrorType();
            double oRefEng = mRefEngs.get(i);
            double tRefEng = mRefEngs.get(tMirrorType-1);
            if (!Double.isNaN(oRefEng) && !MathEX.Code.numericEqual(oRefEng, tRefEng)) {
                UT.Code.warning("RefEng of mirror mismatch for type: "+(i+1)+", overwrite with mirror values automatically");
            }
            mRefEngs.set(i, tRefEng);
        }
        
        mNN = new FeedForward[aThreadNumber][mTypeNum];
        for (int i = 0; i < mTypeNum; ++i) {
            mNN[0][i] = tNN[i];
            for (int ti = 1; ti < aThreadNumber; ++ti) if (tNN[i] != null) {
                mNN[ti][i] = tNN[i].threadSafeRef();
            }
        }
        mTrainData = new DataSet();
        mTestData = new DataSet();
        
        mNormMu = new Vector[mTypeNum];
        mNormSigma = new Vector[mTypeNum];
        for (int i = 0; i < mTypeNum; ++i) {
            int tBasisSize = mBasis[0][i].size();
            mNormMu[i] = Vector.zeros(tBasisSize);
            mNormSigma[i] = Vector.zeros(tBasisSize);
        }
        mNormFp = new Vector[aThreadNumber][];
        mLossGradFp = new Vector[aThreadNumber][];
        mLossGradGradFp = new Vector[aThreadNumber][];
        mFpBuf = new ArrayList<>(aThreadNumber);
        mGradFpBuf = new ArrayList<>(aThreadNumber);
        mHiddenOutputsBuf = new ArrayList<>(aThreadNumber);
        mHiddenGradsBuf = new ArrayList<>(aThreadNumber);
        mHiddenGrads2Buf = new ArrayList<>(aThreadNumber);
        mHiddenGrads3Buf = new ArrayList<>(aThreadNumber);
        mHiddenGradGradsBuf = new ArrayList<>(aThreadNumber);
        mBasisForwardBuf = new ArrayList<>(aThreadNumber);
        mBasisForwardForceBuf  = new ArrayList<>(aThreadNumber);
        mBasisBackwardBuf = new DoubleList[aThreadNumber];
        mBasisBackwardForceBuf = new DoubleList[aThreadNumber];
        for (int ti = 0; ti < aThreadNumber; ++ti) {
            Vector[] tNormFp = new Vector[mTypeNum];
            Vector[] tLossGradNormFp = new Vector[mTypeNum];
            Vector[] tLossGradGradFp = new Vector[mTypeNum];
            List<List<Vector>> tFpBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tGradFpBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenOutputsBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGradsBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGrads2Buf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGrads3Buf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGradGradsBuf = new ArrayList<>(mTypeNum);
            List<DoubleList> tBasisForwardBuf = new ArrayList<>(16);
            List<DoubleList> tBasisForwardForceBuf = new ArrayList<>(16);
            DoubleList tBasisBackwardBuf = new DoubleList(16);
            DoubleList tBasisBackwardForceBuf = new DoubleList(16);
            mNormFp[ti] = tNormFp;
            mLossGradFp[ti] = tLossGradNormFp;
            mLossGradGradFp[ti] = tLossGradGradFp;
            mFpBuf.add(tFpBuf);
            mGradFpBuf.add(tGradFpBuf);
            mHiddenOutputsBuf.add(tHiddenOutputsBuf);
            mHiddenGradsBuf.add(tHiddenGradsBuf);
            mHiddenGrads2Buf.add(tHiddenGrads2Buf);
            mHiddenGrads3Buf.add(tHiddenGrads3Buf);
            mHiddenGradGradsBuf.add(tHiddenGradGradsBuf);
            mBasisForwardBuf.add(tBasisForwardBuf);
            mBasisForwardForceBuf.add(tBasisForwardForceBuf);
            mBasisBackwardBuf[ti] = tBasisBackwardBuf;
            mBasisBackwardForceBuf[ti] = tBasisBackwardForceBuf;
            for (int i = 0; i < mTypeNum; ++i) {
                int tBasisSize = mBasis[0][i].size();
                if (mBasis[0][i] instanceof Mirror) {
                    tFpBuf.add(null);
                    tGradFpBuf.add(null);
                    tHiddenOutputsBuf.add(null);
                    tHiddenGradsBuf.add(null);
                    tHiddenGrads2Buf.add(null);
                    tHiddenGrads3Buf.add(null);
                    tHiddenGradGradsBuf.add(null);
                } else {
                    tNormFp[i] = Vector.zeros(tBasisSize);
                    tLossGradNormFp[i] = Vector.zeros(tBasisSize);
                    tLossGradGradFp[i] = Vector.zeros(tBasisSize);
                    tFpBuf.add(new ArrayList<>(16));
                    tGradFpBuf.add(new ArrayList<>(16));
                    tHiddenOutputsBuf.add(new ArrayList<>(16));
                    tHiddenGradsBuf.add(new ArrayList<>(16));
                    tHiddenGrads2Buf.add(new ArrayList<>(16));
                    tHiddenGrads3Buf.add(new ArrayList<>(16));
                    tHiddenGradGradsBuf.add(new ArrayList<>(16));
                }
            }
        }
        mNlType = new IntList[aThreadNumber];
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
            mNlType[ti] = new IntList(16);
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
        for (int i = 0; i < mTypeNum; ++i) if (!(mBasis[0][i] instanceof Mirror)) {
            IVector tPara = mNN[0][i].parameters();
            int tParaSize = tPara.size();
            mNNParas[i] = tPara;
            mNNParaSizes[i] = tParaSize;
            rTotParaSize += tParaSize;
            mHiddenSizes[i] = mNN[0][i].hiddenSize();
            mNNParaWeightSizes[i] = mNN[0][i].parameterWeightSize();
        }
        mTotNNParaSize = rTotParaSize;
        
        rTotParaSize = 0;
        mBasisParaSizes = new int[mTypeNum];
        mBasisParas = new IVector[mTypeNum];
        for (int i = 0; i < mTypeNum; ++i) if (!(mBasis[0][i] instanceof Mirror)) {
            IVector tBasisPara = mBasis[0][i].hasParameters() ? mBasis[0][i].parameters() : null;
            int tBasisParaSize = tBasisPara==null ? 0 : tBasisPara.size();
            mBasisParas[i] = tBasisPara;
            mBasisParaSizes[i] = tBasisParaSize;
            rTotParaSize += tBasisParaSize;
        }
        mTotBasisParaSize = rTotParaSize;
        
        mGradParaBuf = new Vector[aThreadNumber];
        for (int ti = 1; ti < aThreadNumber; ++ti) {
            mGradParaBuf[ti] = Vector.zeros(mTotNNParaSize+mTotBasisParaSize);
        }
        mSelectParas = Vector.zeros(mTotNNParaSize+mTotBasisParaSize);
        
        mParas = new RefVector() {
            @Override public double get(int aIdx) {
                int tIdx = aIdx;
                if (tIdx < mTotNNParaSize) {
                    for (int i = 0; i < mTypeNum; ++i) {
                        int tParaSize = mNNParaSizes[i];
                        if (tIdx < tParaSize) {
                            return mNNParas[i].get(tIdx);
                        }
                        tIdx -= tParaSize;
                    }
                    throw new IllegalStateException();
                }
                if (mTrainBasis) {
                    tIdx -= mTotNNParaSize;
                    for (int i = 0; i < mTypeNum; ++i) {
                        int tBasisParaSize = mBasisParaSizes[i];
                        if (tIdx < tBasisParaSize) {
                            IVector tPara = mBasisParas[i];
                            assert tPara != null;
                            return tPara.get(tIdx);
                        }
                        tIdx -= tBasisParaSize;
                    }
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public void set(int aIdx, double aValue) {
                int tIdx = aIdx;
                if (tIdx < mTotNNParaSize) {
                    for (int i = 0; i < mTypeNum; ++i) {
                        int tParaSize = mNNParaSizes[i];
                        if (tIdx < tParaSize) {
                            mNNParas[i].set(tIdx, aValue);
                            return;
                        }
                        tIdx -= tParaSize;
                    }
                    throw new IllegalStateException();
                }
                if (mTrainBasis) {
                    tIdx -= mTotNNParaSize;
                    for (int i = 0; i < mTypeNum; ++i) {
                        int tBasisParaSize = mBasisParaSizes[i];
                        if (tIdx < tBasisParaSize) {
                            IVector tPara = mBasisParas[i];
                            assert tPara != null;
                            tPara.set(tIdx, aValue);
                            return;
                        }
                        tIdx -= tBasisParaSize;
                    }
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                return mTrainBasis ? (mTotNNParaSize+mTotBasisParaSize) : mTotNNParaSize;
            }
        };
        
        mOptimizer.setParameter(mParas)
        .setLossFunc(() -> calLoss(false))
        .setLossFuncGrad(grad -> calLoss(false, grad))
        .setLogPrinter((step, lineSearchStep, loss, printLog) -> {
            mEpoch = step;
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
        })
        .setBreakChecker((step, loss, lastLoss, parameterStep) -> {
            if (step==0 || Double.isNaN(lastLoss)) return false;
            return Math.abs(lastLoss-loss) < Math.abs(lastLoss)*1e-7;
        });
    }
    /**
     * 创建一个 nnap 的训练器
     * @param aArgs 其余训练其参数，具体为：
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
     * </dl>
     */
    public Trainer(Map<String, ?> aArgs) {
        this(threadNumberFrom_(aArgs), aArgs);
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
            setUnits(tUnits.toString());
        }
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
    private static Basis[][] basisFrom_(String[] aSymbols, int aThreadNum, Map<String, ?> aArgs) {
        @Nullable Object tBasis = UT.Code.get(aArgs, "basis");
        if (tBasis == null) {
            tBasis = Maps.of("type", "spherical_chebyshev");
        }
        if (tBasis instanceof Map) {
            Map<?, ?> tSubBasis = (Map<?, ?>)tBasis;
            tBasis = NewCollections.from(aSymbols.length, i -> tSubBasis);
        }
        Basis[][] rBasis = new Basis[aThreadNum][];
        rBasis[0] = Basis.load(aSymbols, (List<?>)tBasis);
        for (Basis tSubBasis : rBasis[0]) if (!(tSubBasis instanceof Mirror)) {
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
    private static int threadNumberFrom_(Map<String, ?> aArgs) {
        return ((Number)UT.Code.getWithDefault(aArgs, DEFAULT_THREAD_NUMBER, "thread_number", "thread_num", "nthreads")).intValue();
    }
    private static FeedForward[] nnFrom_(Basis[] aBasis, Map<String, ?> aArgs) {
        @Nullable Object tNNSetting = UT.Code.get(aArgs, "nn");
        if (tNNSetting == null) {
            tNNSetting = new LinkedHashMap<>();
        }
        if (tNNSetting instanceof Map) {
            Map<?, ?> tSubNNSetting = (Map<?, ?>)tNNSetting;
            tNNSetting = NewCollections.from(aBasis.length, i -> (aBasis[i] instanceof Mirror) ? null : tSubNNSetting);
        }
        FeedForward[] rOut = new FeedForward[aBasis.length];
        List<?> tNNSettingList = (List<?>)tNNSetting;
        for (int i = 0; i < rOut.length; ++i) {
            // mirror 情况延迟初始化
            if (aBasis[i] instanceof Mirror) {
                continue;
            }
            Map<?, ?> tNNSettingMap = (Map<?, ?>)tNNSettingList.get(i);
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
            rOut[i] = new FeedForward(aBasis[i].size(), tHiddenDimsArr);
            rOut[i].initParameters();
        }
        for (int i = 0; i < rOut.length; ++i) if (aBasis[i] instanceof Mirror) {
            @Nullable Map<?, ?> tNNSettingMap = (Map<?, ?>)tNNSettingList.get(i);
            if (tNNSettingMap == null) continue;
            @Nullable Object tHiddenDims = UT.Code.get(tNNSettingMap, "hidden_dims", "nnarch");
            if (tHiddenDims == null) continue;
            UT.Code.warning("hidden_dims of mirror for type: "+(i+1)+" will be overwritten with mirror values automatically");
        }
        return rOut;
    }
    
    @Override public int atomTypeNumber() {return mTypeNum;}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
    public Basis basis(int aType) {return mBasis[0][aType-1];}
    public @Unmodifiable List<Basis> basis() {return AbstractCollections.from(mBasis[0]);}
    public FeedForward model(int aType) {return mNN[0][aType-1];}
    public @Unmodifiable List<FeedForward> models() {return AbstractCollections.from(mNN[0]);}
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
    
    protected void initNl(IntList aNl, IntVector aAtomType, IntList rNlTypeBuf) {
        int tNlSize = aNl.size();
        rNlTypeBuf.clear(); rNlTypeBuf.ensureCapacity(tNlSize);
        for (int j = 0; j < tNlSize; ++j) {
            rNlTypeBuf.add(aAtomType.get(aNl.get(j)));
        }
    }
    
    @FunctionalInterface public interface ILossFunc {double call(double aPred, double aReal);}
    @FunctionalInterface public interface ILossFuncGrad {double call(double aPred, double aReal, DoubleWrapper rGrad);}
    
    protected double calLoss(boolean aTest) {
        return calLoss(aTest, null);
    }
    protected double calLoss(boolean aTest, @Nullable Vector rGrad) {
        return calLoss(aTest, null, rGrad);
    }
    protected double calLossDetail(boolean aTest, @Nullable Vector rLossDetail) {
        return calLoss(aTest, rLossDetail, null);
    }
    protected double calLoss(boolean aTest, @Nullable Vector rLossDetail, @Nullable Vector rGrad) {
        return calLoss_(aTest, rLossDetail, rGrad,
                        mLossFuncEng, mLossFuncGradEng,
                        mLossFuncForce, mLossFuncGradForce,
                        mLossFuncStress, mLossFuncGradStress);
    }
    protected void calMAE(boolean aTest, Vector rMAE) {
        calLoss_(aTest, rMAE, null,
                 LOSS_ABSOLUTE, LOSS_ABSOLUTE_G,
                 LOSS_ABSOLUTE, LOSS_ABSOLUTE_G,
                 LOSS_ABSOLUTE, LOSS_ABSOLUTE_G);
        rMAE.multiply2this(mNormSigmaEng);
        rMAE.update(0, v -> v/mEnergyWeight);
        rMAE.update(1, v -> v/mForceWeight);
        rMAE.update(2, v -> v/mStressWeight);
    }
    private double calLoss_(boolean aTest, @Nullable Vector rLossDetail, @Nullable Vector rGrad,
                            ILossFunc aLossFuncEng, ILossFuncGrad aLossFuncGradEng,
                            ILossFunc aLossFuncForce, ILossFuncGrad aLossFuncGradForce,
                            ILossFunc aLossFuncStress, ILossFuncGrad aLossFuncGradStress) {
        final DataSet tData = aTest ? mTestData : mTrainData;
        final boolean tRequireGrad = rGrad!=null;
        final boolean tTrainBasis = mTrainBasis;
        final int tThreadNum = threadNumber();
        if (tRequireGrad) {
            mGradParaBuf[0] = rGrad;
            for (Vector tGradPara : mGradParaBuf) {
                tGradPara.setInternalDataSize(rGrad.size());
                tGradPara.fill(0.0);
            }
        }
        List<Vector> rLossPar = VectorCache.getZeros(4, tThreadNum);
        pool().parfor(tData.mSize, (i, threadID) -> {
            Basis[] tBasis = mBasis[threadID];
            FeedForward[] tNN = mNN[threadID];
            Vector rLoss = rLossPar.get(threadID);
            Vector tGradPara = mGradParaBuf[threadID];
            DoubleWrapper rLossGradEng = new DoubleWrapper(0.0);
            
            IntVector tAtomType = tData.mAtomType.get(i);
            int tAtomNum = tAtomType.size();
            IntList tNlType = mNlType[threadID];
            IntList[] tNl = tData.mNl.get(i);
            DoubleList[] tNlDx = tData.mNlDx.get(i), tNlDy = tData.mNlDy.get(i), tNlDz = tData.mNlDz.get(i);
            
            Vector[] tNormFp = mNormFp[threadID];
            Vector[] tLossGradFp = mLossGradFp[threadID];
            
            List<List<Vector>> tHiddenOutputsBuf = mHiddenOutputsBuf.get(threadID);
            List<List<Vector>> tHiddenGradsBuf = mHiddenGradsBuf.get(threadID);
            List<DoubleList> tBaisForwardBuf = mBasisForwardBuf.get(threadID);
            DoubleList tBaisBackwardBuf = mBasisBackwardBuf[threadID];
            while (tBaisForwardBuf.size() < tAtomNum) tBaisForwardBuf.add(new DoubleList(128));
            
            List<List<Vector>> tFpBuf = mFpBuf.get(threadID);
            validFpBuf_(threadID, tAtomNum, mHasForce||mHasStress);
            if (tRequireGrad) validHiddenBuf_(threadID, tAtomType, mHasForce||mHasStress);
            
            if (!mHasForce && !mHasStress) {
                double rEng = 0.0;
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    Basis tSubBasis = tBasis[tType-1];
                    if (tSubBasis instanceof Mirror) {
                        tType = ((Mirror)tSubBasis).mirrorType();
                    }
                    Vector tSubFp = tFpBuf.get(tType-1).get(k);
                    Vector tSubNormFp = tNormFp[tType-1];
                    Vector tNormMu = mNormMu[tType-1];
                    Vector tNormSigma = mNormSigma[tType-1];
                    initNl(tNl[k], tAtomType, tNlType);
                    tSubBasis.forward(tNlDx[k], tNlDy[k], tNlDz[k], tNlType, tSubFp, tBaisForwardBuf.get(k), tRequireGrad);
                    tSubNormFp.fill(j -> (tSubFp.get(j) - tNormMu.get(j)) / tNormSigma.get(j));
                    rEng += tRequireGrad ? tNN[tType-1].forward(tSubNormFp, tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k))
                                         : tNN[tType-1].eval(tSubNormFp);
                }
                rEng /= tAtomNum;
                double tLossEng = tRequireGrad ? aLossFuncGradEng.call(rEng, (tData.mEng.get(i) - mNormMuEng)/mNormSigmaEng, rLossGradEng)
                                               : aLossFuncEng.call(rEng, (tData.mEng.get(i) - mNormMuEng)/mNormSigmaEng);
                rLoss.add(0, mEnergyWeight * tLossEng / tData.mSize);
                /// backward
                if (!tRequireGrad) return;
                double tLossGradEng = mEnergyWeight * rLossGradEng.value() / (tAtomNum*tData.mSize);
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    Basis tSubBasis = tBasis[tType-1];
                    if (tSubBasis instanceof Mirror) {
                        tType = ((Mirror)tSubBasis).mirrorType();
                    }
                    Vector tSubFp = tFpBuf.get(tType-1).get(k);
                    Vector tSubNormFp = tNormFp[tType-1];
                    Vector tSubNormMu = mNormMu[tType-1];
                    Vector tSubNormSigma = mNormSigma[tType-1];
                    tSubNormFp.fill(j -> (tSubFp.get(j) - tSubNormMu.get(j)) / tSubNormSigma.get(j));
                    int tShiftPara = paraShift(tType);
                    Vector rSubLossGradFp = null;
                    if (tTrainBasis) {
                        rSubLossGradFp = tLossGradFp[tType-1];
                        rSubLossGradFp.fill(0.0);
                    }
                    tNN[tType-1].backward(tLossGradEng, tSubNormFp, rSubLossGradFp, tGradPara.subVec(tShiftPara, tShiftPara+ mNNParaSizes[tType-1]),
                                          tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k));
                    if (tTrainBasis) {
                        rSubLossGradFp.div2this(tSubNormSigma);
                        int tShiftBasisPara = basisParaShift(tType);
                        initNl(tNl[k], tAtomType, tNlType);
                        tSubBasis.backward(tNlDx[k], tNlDy[k], tNlDz[k], tNlType, rSubLossGradFp,
                                           tGradPara.subVec(tShiftBasisPara, tShiftBasisPara+mBasisParaSizes[tType-1]),
                                           tBaisForwardBuf.get(k), tBaisBackwardBuf, false);
                    }
                }
                return;
            }
            double tVolume = tData.mVolume.get(i);
            DoubleWrapper rLossGradForceX = new DoubleWrapper(0.0), rLossGradForceY = new DoubleWrapper(0.0), rLossGradForceZ = new DoubleWrapper(0.0);
            DoubleWrapper rLossGradStressXX = new DoubleWrapper(0.0), rLossGradStressYY = new DoubleWrapper(0.0), rLossGradStressZZ = new DoubleWrapper(0.0);
            DoubleWrapper rLossGradStressXY = new DoubleWrapper(0.0), rLossGradStressXZ = new DoubleWrapper(0.0), rLossGradStressYZ = new DoubleWrapper(0.0);
            
            List<List<Vector>> tHiddenGrads2Buf = mHiddenGrads2Buf.get(threadID);
            List<List<Vector>> tHiddenGrads3Buf = mHiddenGrads3Buf.get(threadID);
            List<List<Vector>> tHiddenGradGradsBuf = mHiddenGradGradsBuf.get(threadID);
            List<DoubleList> tBaisForwardForceBuf = mBasisForwardForceBuf.get(threadID);
            DoubleList tBaisBackwardForceBuf = mBasisBackwardForceBuf[threadID];
            while (tBaisForwardForceBuf.size() < tAtomNum) tBaisForwardForceBuf.add(new DoubleList(128));
            
            List<List<Vector>> tGradFpBuf = mGradFpBuf.get(threadID);
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
                if (tSubBasis instanceof Mirror) {
                    tType = ((Mirror)tSubBasis).mirrorType();
                }
                IntList tSubNl = tNl[k];
                DoubleList tSubNlDx = tNlDx[k], tSubNlDy = tNlDy[k], tSubNlDz = tNlDz[k];
                initNl(tSubNl, tAtomType, tNlType);
                
                DoubleList tSubBaisForwardBuf = tBaisForwardBuf.get(k);
                Vector tSubFp = tFpBuf.get(tType-1).get(k);
                Vector tSubGradFp = tGradFpBuf.get(tType-1).get(k);
                Vector tSubNormFp = tNormFp[tType-1];
                Vector tSubNormMu = mNormMu[tType-1];
                Vector tSubNormSigma = mNormSigma[tType-1];
                
                tSubBasis.forward(tSubNlDx, tSubNlDy, tSubNlDz, tNlType, tSubFp, tSubBaisForwardBuf, true);
                tSubNormFp.fill(j -> (tSubFp.get(j) - tSubNormMu.get(j)) / tSubNormSigma.get(j));
                // cal energy
                rEng += tRequireGrad ? tNN[tType-1].forwardGrad(tSubNormFp, tSubGradFp, tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k),
                                                                tHiddenGrads2Buf.get(tType-1).get(k), tHiddenGrads3Buf.get(tType-1).get(k), tHiddenGradGradsBuf.get(tType-1).get(k))
                                     : tNN[tType-1].evalGrad(tSubNormFp, tSubGradFp);
                tSubGradFp.div2this(tSubNormSigma);
                // cal force
                int tNlSize = tSubNl.size();
                tForceNlXBuf.ensureCapacity(tNlSize); tForceNlXBuf.setInternalDataSize(tNlSize);
                tForceNlYBuf.ensureCapacity(tNlSize); tForceNlYBuf.setInternalDataSize(tNlSize);
                tForceNlZBuf.ensureCapacity(tNlSize); tForceNlZBuf.setInternalDataSize(tNlSize);
                tSubBasis.forwardForce(tSubNlDx, tSubNlDy, tSubNlDz, tNlType, tSubGradFp, tForceNlXBuf, tForceNlYBuf, tForceNlZBuf,
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
            rLoss.add(0, mEnergyWeight * tLossEng / tData.mSize);
            // force error
            if (mHasForce) {
                double tLossForce = 0.0;
                if (tRequireGrad) {
                    tLossGradForceXBuf.clear(); tLossGradForceXBuf.addZeros(tAtomNum);
                    tLossGradForceYBuf.clear(); tLossGradForceYBuf.addZeros(tAtomNum);
                    tLossGradForceZBuf.clear(); tLossGradForceZBuf.addZeros(tAtomNum);
                }
                double tForceLossMul = mForceWeight / (tData.mAtomSize*3);
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
                rLoss.add(2, mStressWeight*tLossStress/(tData.mSize*6));
            }
            /// backward
            if (!tRequireGrad) return;
            double tLossGradEng = mEnergyWeight * rLossGradEng.value() / (tAtomNum*tData.mSize);
            double tLossGradStressXX = 0.0, tLossGradStressYY = 0.0, tLossGradStressZZ = 0.0;
            double tLossGradStressXY = 0.0, tLossGradStressXZ = 0.0, tLossGradStressYZ = 0.0;
            if (mHasStress) {
                double tMul = -mStressWeight / (tVolume*tData.mSize*6);
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
                if (tSubBasis instanceof Mirror) {
                    tType = ((Mirror)tSubBasis).mirrorType();
                }
                // force loss grad
                Vector tSubLossGradGradFp = tLossGradGradFp[tType-1];
                IntList tSubNl = tNl[k];
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
                    int tShiftBasisPara = basisParaShift(tType);
                    tLossGradBasisPara = tGradPara.subVec(tShiftBasisPara, tShiftBasisPara+mBasisParaSizes[tType-1]);
                }
                tSubLossGradGradFp.fill(0.0);
                initNl(tSubNl, tAtomType, tNlType);
                tSubBasis.backwardForce(tSubNlDx, tSubNlDy, tSubNlDz, tNlType, tGradFpBuf.get(tType-1).get(k), tLossGradForceNlXBuf, tLossGradForceNlYBuf, tLossGradForceNlZBuf,
                                        tSubLossGradGradFp, tLossGradBasisPara, tBaisForwardBuf.get(k), tBaisForwardForceBuf.get(k), tBaisBackwardBuf, tBaisBackwardForceBuf, false, !tTrainBasis);
                
                Vector tSubFp = tFpBuf.get(tType-1).get(k);
                Vector tSubNormFp = tNormFp[tType-1];
                Vector tSubNormMu = mNormMu[tType-1];
                Vector tSubNormSigma = mNormSigma[tType-1];
                tSubNormFp.fill(j -> (tSubFp.get(j) - tSubNormMu.get(j)) / tSubNormSigma.get(j));
                tSubLossGradGradFp.div2this(tSubNormSigma);
                
                int tShiftPara = paraShift(tType);
                DoubleArrayVector tLossGradNNPara = tGradPara.subVec(tShiftPara, tShiftPara+mNNParaSizes[tType-1]);
                Vector rSubLossGradFp = null;
                if (tTrainBasis) {
                    rSubLossGradFp = tLossGradFp[tType-1];
                    rSubLossGradFp.fill(0.0);
                }
                tNN[tType-1].gradBackward(tSubLossGradGradFp, tSubNormFp, rSubLossGradFp, tLossGradNNPara,
                                          tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k),
                                          tHiddenGrads2Buf.get(tType-1).get(k), tHiddenGrads3Buf.get(tType-1).get(k), tHiddenGradGradsBuf.get(tType-1).get(k));
                // energy loss grad
                tNN[tType-1].backward(tLossGradEng, tSubNormFp, rSubLossGradFp, tLossGradNNPara,
                                      tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k));
                if (tTrainBasis) {
                    rSubLossGradFp.div2this(tSubNormSigma);
                    tSubBasis.backward(tSubNlDx, tSubNlDy, tSubNlDz, tNlType, rSubLossGradFp,
                                       tLossGradBasisPara, tBaisForwardBuf.get(k), tBaisBackwardBuf, true);
                }
            }
        });
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
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            IntVector rAtomType = IntVector.zeros(tAtomNum);
            IntList[] rNl = new IntList[tAtomNum];
            DoubleList[] rNlDx = new DoubleList[tAtomNum], rNlDy = new DoubleList[tAtomNum], rNlDz = new DoubleList[tAtomNum];
            rData.mAtomType.add(rAtomType);
            rData.mNl.add(rNl);
            rData.mNlDx.add(rNlDx); rData.mNlDy.add(rNlDy); rData.mNlDz.add(rNlDz);
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                rAtomType.set(i, tType);
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
                // 增加近邻列表，这里直接重新添加
                IntList tNl = new IntList(16);
                DoubleList tNlDx = new DoubleList(16);
                DoubleList tNlDy = new DoubleList(16);
                DoubleList tNlDz = new DoubleList(16);
                tAPC.nl_().forEachNeighbor(i, basis(tType).rcut(), (dx, dy, dz, idx) -> {
                    tNl.add(idx);
                    tNlDx.add(dx); tNlDy.add(dy); tNlDz.add(dz);
                });
                tNl.trimToSize(); rNl[i] = tNl;
                tNlDx.trimToSize(); rNlDx[i] = tNlDx;
                tNlDy.trimToSize(); rNlDy[i] = tNlDy;
                tNlDz.trimToSize(); rNlDz[i] = tNlDz;
            }
        }
        // 这里后添加能量，这样 rData.mEng.size() 对应正确的索引
        rData.mEng.add(aEnergy/tAtomNum);
        // 这里后添加力，这样 rData.mForce.size() 对应正确的索引
        if (mHasForce) {
            assert aForces != null;
            rData.mForceX.add(Vectors.from(aForces.col(0)));
            rData.mForceY.add(Vectors.from(aForces.col(1)));
            rData.mForceZ.add(Vectors.from(aForces.col(2)));
        }
        // 这里后添加应力应力，这样 rData.mStress.size() 对应正确的索引
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
        rData.mAtomSize += tAtomNum;
        rData.mVolume.add(aAtomData.volume());
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
    
    
    private void initNormBasis() {
        for (int i = 0; i < mTypeNum; ++i) {
            mNormMu[i].fill(0.0);
            mNormSigma[i].fill(0.0);
        }
        final int tThreadNum = threadNumber();
        List<IntVector> tDivPar = IntVectorCache.getZeros(mTypeNum, tThreadNum);
        Vector[][] tNormMuPar = new Vector[tThreadNum][mTypeNum];
        Vector[][] tNormSigmaPar = new Vector[tThreadNum][mTypeNum];
        tNormMuPar[0] = mNormMu;
        tNormSigmaPar[0] = mNormSigma;
        for (int ti = 1; ti < tThreadNum; ++ti) {
            for (int i = 0; i < mTypeNum; ++i) {
                tNormMuPar[ti][i] = VectorCache.getZeros(mNormMu[i].size());
                tNormSigmaPar[ti][i] = VectorCache.getZeros(mNormSigma[i].size());
            }
        }
        pool().parfor(mTrainData.mSize, (i, threadID) -> {
            Basis[] tBasis = mBasis[threadID];
            IntVector tAtomType = mTrainData.mAtomType.get(i);
            int tAtomNum = tAtomType.size();
            
            IntList tNlType = mNlType[threadID];
            IntList[] tNl = mTrainData.mNl.get(i);
            DoubleList[] tNlDx = mTrainData.mNlDx.get(i), tNlDy = mTrainData.mNlDy.get(i), tNlDz = mTrainData.mNlDz.get(i);
            
            Vector[] tFp = mNormFp[threadID];
            Vector[] tNormMu = tNormMuPar[threadID];
            Vector[] tNormSigma = tNormSigmaPar[threadID];
            IntVector tDiv = tDivPar.get(threadID);
            
            for (int k = 0; k < tAtomNum; ++k) {
                int tType = tAtomType.get(k);
                // 这里需要考虑 mirror 的情况，对于 mirror 的同时和对应的数据一起公用归一化向量
                Basis tSubBasis = tBasis[tType-1];
                if (tSubBasis instanceof Mirror) {
                    tType = ((Mirror)tSubBasis).mirrorType();
                }
                // 现在实时计算基组而不是缓存
                Vector tSubFp = tFp[tType-1];
                initNl(tNl[k], tAtomType, tNlType);
                tSubBasis.eval(tNlDx[k], tNlDy[k], tNlDz[k], tNlType, tSubFp);
                // 统计归一化系数
                tNormMu[tType-1].plus2this(tSubFp);
                tNormSigma[tType-1].operation().operate2this(tSubFp, (lhs, rhs) -> lhs + rhs * rhs);
                tDiv.increment(tType-1);
            }
        });
        for (int ti = 1; ti < tThreadNum; ++ti) {
            for (int i = 0; i < mTypeNum; ++i) {
                mNormMu[i].plus2this(tNormMuPar[ti][i]);
                mNormSigma[i].plus2this(tNormSigmaPar[ti][i]);
                VectorCache.returnVec(tNormMuPar[ti][i]);
                VectorCache.returnVec(tNormSigmaPar[ti][i]);
            }
            tDivPar.get(0).plus2this(tDivPar.get(ti));
        }
        for (int i = 0; i < mTypeNum; ++i) if (!(mBasis[0][i] instanceof Mirror)) {
            int tDivI = tDivPar.get(0).get(i);
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
                        rOut = 1.0;
                    } else {
                        rOut = MathEX.Fast.sqrt(v);
                    }
                    return rOut;
                });
            }
        }
        IntVectorCache.returnVec(tDivPar);
        for (int i = 0; i < mTypeNum; ++i) if (mBasis[0][i] instanceof Mirror) {
            int tMirrorIdx = ((Mirror)mBasis[0][i]).mirrorType()-1;
            mNormMu[i] = mNormMu[tMirrorIdx];
            mNormSigma[i] = mNormSigma[tMirrorIdx];
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
    public void train(int aEpochs, boolean aEarlyStop, boolean aPrintLog) {
        // 清空旧的早停存储
        mMinLoss = Double.POSITIVE_INFINITY;
        // 初始化归一化参数，现在只会初始化一次
        if (!mNormInit) {
            if (aPrintLog) System.out.println("Init train data...");
            initNormEng();
            initNormBasis();
            mNormInit = true;
        }
        // 开始训练
        if (aPrintLog) {
            UT.Timer.progressBar(Maps.of(
                "name", mTrainBasis ? "train(B)" : "train",
                "max", aEpochs,
                "length", 100
            ));
        }
        mOptimizer.run(aEpochs, aPrintLog);
        if (aPrintLog) for (int i = mEpoch +1; i < aEpochs; ++i) {
            UT.Timer.progressBar(mHasTest ? String.format("loss: %.4g | %.4g", mTrainLoss.last(), mTestLoss.last()) : String.format("loss: %.4g", mTrainLoss.last()));
        }
        // 应用早停
        if (aEarlyStop && mSelectEpoch>=0) {
            mParas.fill(mSelectParas);
            mOptimizer.markParameterChanged();
            if (aPrintLog) System.out.printf("Model at epoch = %d selected, test loss = %.4g\n", mSelectEpoch+1, mMinLoss);
            mSelectEpoch = -1;
            mMinLoss = Double.POSITIVE_INFINITY;
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
            if (mBasis[0][i] instanceof Mirror) {
                rModels.add(Maps.of(
                    "symbol", mSymbols[i],
                    "basis", rBasis
                ));
                continue;
            }
            Map rNN = new LinkedHashMap();
            mNN[0][i].save(rNN);
            rModels.add(Maps.of(
                "symbol", mSymbols[i],
                "basis", rBasis,
                "ref_eng", mRefEngs.get(i),
                "norm_mu", mNormMu[i].asList(),
                "norm_sigma", mNormSigma[i].asList(),
                "norm_mu_eng", mNormMuEng,
                "norm_sigma_eng", mNormSigmaEng,
                "nn", rNN
            ));
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
