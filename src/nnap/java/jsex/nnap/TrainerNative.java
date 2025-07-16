package jsex.nnap;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IAtomData;
import jse.code.collection.DoubleList;
import jse.code.collection.FloatList;
import jse.code.collection.IntList;
import jse.code.collection.ShortList;
import jse.code.timer.AccumulatedTimer;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.opt.Adam;
import jse.opt.IOptimizer;
import jsex.nnap.basis.Basis;
import jsex.nnap.nn.FeedForward;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 纯 jse 实现的 nnap 训练器，不借助 pytorch
 * 来实现更高的优化效果
 * <p>
 * 由于是纯 jse 实现，写法可以更加灵活并且避免了重复代码
 *
 * @author liqa
 */
@ApiStatus.Experimental
public class TrainerNative {
    protected final static String DEFAULT_UNITS = "metal";
    protected final static int[] DEFAULT_HIDDEN_DIMS = {32, 32}; // 现在统一默认为 32, 32
    protected final static double DEFAULT_FORCE_WEIGHT = 0.1;
    protected final static double DEFAULT_STRESS_WEIGHT = 1.0;
    protected final static double DEFAULT_L2_LOSS_WEIGHT = 0.001;
    
    public final static AccumulatedTimer INIT_TIMER = new AccumulatedTimer(), NN_TIMER = new AccumulatedTimer(), NL_TIMER = new AccumulatedTimer(),
        INDEX1_TIMER = new AccumulatedTimer(), INDEX2_TIMER = new AccumulatedTimer(), MPLUS_TIMER = new AccumulatedTimer();
    
    protected static class DataSet {
        public int mSize = 0;
        /** 按照原子结构排列，每个原子结构中每个原子对应一个向量（不同种类可能长度不等）*/
        public final List<Vector[]> mFp = new ArrayList<>(64);
        /** 按照原子结构排列，每个原子结构中每个原子对应一个向量，根据近邻列表展开（不同原子长度不等）*/
        public final List<Object[]> mFpPx = new ArrayList<>(64), mFpPy = new ArrayList<>(64), mFpPz = new ArrayList<>(64);
        /** 按照原子结构排列，每个原子结构中每个原子对应一个向量，根据近邻列表展开（不同原子长度不等）*/
        public final List<Object[]> mFpGradNlIndex = new ArrayList<>(64), mFpGradFpIndex = new ArrayList<>(64);
        /** 每个原子数据结构对应的能量值 */
        public final DoubleList mEng = new DoubleList(64);
        /** 这里力数据使用 List 存储，每个原子结构一组 */
        public final List<Vector> mForceX = new ArrayList<>(64), mForceY = new ArrayList<>(64), mForceZ = new ArrayList<>(64);
        /** 近邻列表，每个原子结构一组，每个原子对应一个近邻列表向量 */
        public final List<IntVector[]> mNl = new ArrayList<>(64);
    }
    
    protected IOptimizer mOptimizer;
    protected final Basis mBasis;
    protected final FeedForward mNN;
    protected final DataSet mTrainData;
    protected final DataSet mTestData;
    protected boolean mHasData = false;
    protected boolean mHasForce = false;
    protected final Vector mNormMu, mNormSigma;
    protected double mNormMuEng = 0.0, mNormSigmaEng = 0.0;
    protected final double mRefEng;
    
    private final Vector mFpBuf, mGradFpBuf;
    private final Vector mGradParaBuf;
    private final List<RowMatrix> mGradFpGradParaBuf;
    private final DoubleList mForceNlXBuf, mForceNlYBuf, mForceNlZBuf;
    private final DoubleList mForceXBuf, mForceYBuf, mForceZBuf;
    private final DoubleList mLossGradForceXBuf, mLossGradForceYBuf, mLossGradForceZBuf;
    private final DoubleList mLossGradForceNlXBuf, mLossGradForceNlYBuf, mLossGradForceNlZBuf;
    private final Vector mLossGradFp;
    private final int mBasisSize, mParaSize;
    
    protected double mForceWeight = DEFAULT_FORCE_WEIGHT;
    protected boolean mHalfCache = true;
    
    private static void validSize_(DoubleList aData, int aSize) {
        aData.ensureCapacity(aSize);
        aData.setInternalDataSize(aSize);
    }
    private void validGradFpGradPara_(int aSize) {
        int tSize = Math.min(mGradFpGradParaBuf.size(), aSize);
        for (int i = 0; i < tSize; ++i) mGradFpGradParaBuf.get(i).fill(0.0);
        while (mGradFpGradParaBuf.size() < aSize) mGradFpGradParaBuf.add(RowMatrix.zeros(mBasisSize, mParaSize));
    }
    
    TrainerNative(double aRefEng, Basis aBasis, FeedForward aNN, IOptimizer aOptimizer) {
        mRefEng = aRefEng;
        mBasis = aBasis;
        mNN = aNN;
        
        mTrainData = new DataSet();
        mTestData = new DataSet();
        mBasisSize = mBasis.size();
        mNormMu = Vector.zeros(mBasisSize);
        mNormSigma = Vector.zeros(mBasisSize);
        mFpBuf = Vector.zeros(mBasisSize);
        mGradFpBuf = Vector.zeros(mBasisSize);
        mForceNlXBuf = new DoubleList(16);
        mForceNlYBuf = new DoubleList(16);
        mForceNlZBuf = new DoubleList(16);
        mForceXBuf = new DoubleList(16);
        mForceYBuf = new DoubleList(16);
        mForceZBuf = new DoubleList(16);
        mLossGradForceNlXBuf = new DoubleList(16);
        mLossGradForceNlYBuf = new DoubleList(16);
        mLossGradForceNlZBuf = new DoubleList(16);
        mLossGradForceXBuf = new DoubleList(16);
        mLossGradForceYBuf = new DoubleList(16);
        mLossGradForceZBuf = new DoubleList(16);
        mLossGradFp = Vector.zeros(mBasisSize);
        
        mOptimizer = aOptimizer;
        IVector tPara = mNN.parameters();
        mParaSize = tPara.size();
        mGradParaBuf = Vectors.zeros(mParaSize);
        mGradFpGradParaBuf = new ArrayList<>(16);
        mOptimizer.setParameter(tPara);
        mOptimizer.setLossFunc(() -> calLoss(null));
        mOptimizer.setLossFuncGrad(this::calLoss);
    }
    public TrainerNative(double aRefEng, Basis aBasis, IOptimizer aOptimizer) {
        this(aRefEng, aBasis, FeedForward.init(aBasis.size(), DEFAULT_HIDDEN_DIMS), aOptimizer);
    }
    public TrainerNative(double aRefEng, Basis aBasis) {
        this(aRefEng, aBasis, new Adam());
    }
    
    protected double calLoss(@Nullable Vector rGrad) {
        final boolean tHalfCache = mHalfCache;
        if (rGrad!=null) rGrad.fill(0.0);
        double rLoss = 0.0;
        for (int i = 0; i < mTrainData.mSize; ++i) {
            //noinspection IfStatementWithIdenticalBranches
            if (!mHasForce) {
                double rEng = 0.0;
                Vector[] tFp = mTrainData.mFp.get(i);
                if (rGrad!=null) mGradParaBuf.fill(0.0);
                for (Vector tSubFp : tFp) {
                    mFpBuf.fill(j -> (tSubFp.get(j) - mNormMu.get(j)) / mNormSigma.get(j));
                    rEng += rGrad==null ? mNN.eval(mFpBuf) : mNN.forwardBackward(mFpBuf, mGradParaBuf);
                }
                rEng /= tFp.length;
                double tErr = rEng - (mTrainData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
                if (rGrad!=null) rGrad.operation().mplus2this(mGradParaBuf, 2.0 * tErr / tFp.length);
                rLoss += tErr*tErr;
            } else {
                double rEng = 0.0;
                Vector[] tFp = mTrainData.mFp.get(i);
                Object[] tFpPx = mTrainData.mFpPx.get(i);
                Object[] tFpPy = mTrainData.mFpPy.get(i);
                Object[] tFpPz = mTrainData.mFpPz.get(i);
                Object[] tFpGradNlIndex = mTrainData.mFpGradNlIndex.get(i);
                Object[] tFpGradFpIndex = mTrainData.mFpGradFpIndex.get(i);
                IntVector[] tNl = mTrainData.mNl.get(i);
                final int tAtomNum = tFp.length;
                mForceXBuf.clear(); mForceXBuf.addZeros(tAtomNum);
                mForceYBuf.clear(); mForceYBuf.addZeros(tAtomNum);
                mForceZBuf.clear(); mForceZBuf.addZeros(tAtomNum);
                if (rGrad!=null) {
                    mGradParaBuf.fill(0.0);
                    INIT_TIMER.from();
                    validGradFpGradPara_(tAtomNum);
                    INIT_TIMER.to();
                }
                for (int k = 0; k < tAtomNum; ++k) {
                    // cal energy
                    Vector tSubFp = tFp[k];
                    mFpBuf.fill(j -> (tSubFp.get(j) - mNormMu.get(j)) / mNormSigma.get(j));
                    NN_TIMER.from();
                    rEng += rGrad==null ? mNN.evalGrad(mFpBuf, mGradFpBuf) : mNN.forwardGradBackward(mFpBuf, mGradFpBuf, mGradParaBuf, mGradFpGradParaBuf.get(k).asVecRow());
                    NN_TIMER.to();
                    // cal force
                    IntVector tSubNl = tNl[k];
                    int tNlSize = tSubNl.size();
                    INDEX1_TIMER.from();
                    mForceNlXBuf.clear(); mForceNlXBuf.addZeros(tNlSize);
                    mForceNlYBuf.clear(); mForceNlYBuf.addZeros(tNlSize);
                    mForceNlZBuf.clear(); mForceNlZBuf.addZeros(tNlSize);
                    if (tHalfCache) {
                        forwardForceIndexFMA_(mForceNlXBuf.internalData(), mForceNlYBuf.internalData(), mForceNlZBuf.internalData(), mGradFpBuf.internalData(),
                                              ((FloatList)tFpPx[k]).internalData(), ((FloatList)tFpPy[k]).internalData(), ((FloatList)tFpPz[k]).internalData(),
                                              ((ShortList)tFpGradNlIndex[k]).internalData(), ((ShortList)tFpGradFpIndex[k]).internalData(), ((FloatList)tFpPx[k]).size());
                    } else {
                        forwardForceIndexFMA_(mForceNlXBuf.internalData(), mForceNlYBuf.internalData(), mForceNlZBuf.internalData(), mGradFpBuf.internalData(),
                                              ((DoubleList)tFpPx[k]).internalData(), ((DoubleList)tFpPy[k]).internalData(), ((DoubleList)tFpPz[k]).internalData(),
                                              ((IntList)tFpGradNlIndex[k]).internalData(), ((IntList)tFpGradFpIndex[k]).internalData(), ((DoubleList)tFpPx[k]).size());
                    }
                    INDEX1_TIMER.to();
                    NL_TIMER.from();
                    for (int j = 0; j < tNlSize; ++j) {
                        double fx = mForceNlXBuf.get(j);
                        double fy = mForceNlYBuf.get(j);
                        double fz = mForceNlZBuf.get(j);
                        mForceXBuf.set(k, mForceXBuf.get(k) - fx);
                        mForceYBuf.set(k, mForceYBuf.get(k) - fy);
                        mForceZBuf.set(k, mForceZBuf.get(k) - fz);
                        int nlk = tSubNl.get(j);
                        mForceXBuf.set(nlk, mForceXBuf.get(nlk) + fx);
                        mForceYBuf.set(nlk, mForceYBuf.get(nlk) + fy);
                        mForceZBuf.set(nlk, mForceZBuf.get(nlk) + fz);
                    }
                    NL_TIMER.to();
                }
                // energy error
                rEng /= tAtomNum;
                double tErr = rEng - (mTrainData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
                if (rGrad!=null) rGrad.operation().mplus2this(mGradParaBuf, 2.0 * tErr / tAtomNum);
                rLoss += tErr*tErr;
                // force error
                double tLossForce = 0.0;
                mGradParaBuf.fill(0.0);
                if (rGrad != null) {
                    mLossGradForceXBuf.clear(); mLossGradForceXBuf.addZeros(tAtomNum);
                    mLossGradForceYBuf.clear(); mLossGradForceYBuf.addZeros(tAtomNum);
                    mLossGradForceZBuf.clear(); mLossGradForceZBuf.addZeros(tAtomNum);
                }
                Vector tForceX = mTrainData.mForceX.get(i), tForceY = mTrainData.mForceY.get(i), tForceZ = mTrainData.mForceZ.get(i);
                for (int k = 0; k < tAtomNum; ++k) {
                    double tErrX = mForceXBuf.get(k) - tForceX.get(k)/mNormSigmaEng;
                    double tErrY = mForceYBuf.get(k) - tForceY.get(k)/mNormSigmaEng;
                    double tErrZ = mForceZBuf.get(k) - tForceZ.get(k)/mNormSigmaEng;
                    if (rGrad != null) {
                        mLossGradForceXBuf.set(k, mForceWeight*2.0*tErrX);
                        mLossGradForceYBuf.set(k, mForceWeight*2.0*tErrY);
                        mLossGradForceZBuf.set(k, mForceWeight*2.0*tErrZ);
                    }
                    tLossForce += (tErrX*tErrX + tErrY*tErrY + tErrZ*tErrZ);
                }
                rLoss += mForceWeight * tLossForce;
                // backward for force error
                if (rGrad==null) continue;
                for (int k = 0; k < tAtomNum; ++k) {
                    IntVector tSubNl = tNl[k];
                    int tNlSize = tSubNl.size();
                    double tLossGradForceX = mLossGradForceXBuf.get(k);
                    double tLossGradForceY = mLossGradForceYBuf.get(k);
                    double tLossGradForceZ = mLossGradForceZBuf.get(k);
                    NL_TIMER.from();
                    mLossGradForceNlXBuf.clear(); mLossGradForceNlXBuf.addZeros(tNlSize);
                    mLossGradForceNlYBuf.clear(); mLossGradForceNlYBuf.addZeros(tNlSize);
                    mLossGradForceNlZBuf.clear(); mLossGradForceNlZBuf.addZeros(tNlSize);
                    for (int j = 0; j < tNlSize; ++j) {
                        int nlk = tSubNl.get(j);
                        mLossGradForceNlXBuf.set(j, mLossGradForceXBuf.get(nlk) - tLossGradForceX);
                        mLossGradForceNlYBuf.set(j, mLossGradForceYBuf.get(nlk) - tLossGradForceY);
                        mLossGradForceNlZBuf.set(j, mLossGradForceZBuf.get(nlk) - tLossGradForceZ);
                    }
                    NL_TIMER.to();
                    mLossGradFp.fill(0.0);
                    INDEX2_TIMER.from();
                    if (tHalfCache) {
                        backwardForceIndexFMA_(mLossGradFp.internalData(), mLossGradForceNlXBuf.internalData(), mLossGradForceNlYBuf.internalData(), mLossGradForceNlZBuf.internalData(),
                                               ((FloatList)tFpPx[k]).internalData(), ((FloatList)tFpPy[k]).internalData(), ((FloatList)tFpPy[k]).internalData(),
                                               ((ShortList)tFpGradNlIndex[k]).internalData(), ((ShortList)tFpGradFpIndex[k]).internalData(), ((FloatList)tFpPx[k]).size());
                    } else {
                        backwardForceIndexFMA_(mLossGradFp.internalData(), mLossGradForceNlXBuf.internalData(), mLossGradForceNlYBuf.internalData(), mLossGradForceNlZBuf.internalData(),
                                               ((DoubleList)tFpPx[k]).internalData(), ((DoubleList)tFpPy[k]).internalData(), ((DoubleList)tFpPy[k]).internalData(),
                                               ((IntList)tFpGradNlIndex[k]).internalData(), ((IntList)tFpGradFpIndex[k]).internalData(), ((DoubleList)tFpPx[k]).size());
                    }
                    INDEX2_TIMER.to();
                    MPLUS_TIMER.from();
                    RowMatrix tGradFpGradPara = mGradFpGradParaBuf.get(k);
                    backwardForceParaFMA_(rGrad.internalData(), mLossGradFp.internalData(), tGradFpGradPara.internalData(), mBasisSize, mParaSize);
                    MPLUS_TIMER.to();
                }
            }
        }
        if (rGrad!=null) rGrad.div2this(mTrainData.mSize);
        return rLoss / mTrainData.mSize;
    }
    
    static void forwardForceIndexFMA_(double[] rForceNlX, double[] rForceNlY, double[] rForceNlZ, double[] aGradFp,
                                      float[] aFpPx, float[] aFpPy, float[] aFpPz, short[] aFpGradNlIndex, short[] aFpGradFpIndex, int aFpGradSize) {
        for (int ii = 0; ii < aFpGradSize; ++ii) {
            int j = aFpGradNlIndex[ii];
            double tGradFpI = aGradFp[aFpGradFpIndex[ii]];
            rForceNlX[j] += aFpPx[ii]*tGradFpI;
            rForceNlY[j] += aFpPy[ii]*tGradFpI;
            rForceNlZ[j] += aFpPz[ii]*tGradFpI;
        }
    }
    static void forwardForceIndexFMA_(double[] rForceNlX, double[] rForceNlY, double[] rForceNlZ, double[] aGradFp,
                                      double[] aFpPx, double[] aFpPy, double[] aFpPz, int[] aFpGradNlIndex, int[] aFpGradFpIndex, int aFpGradSize) {
        for (int ii = 0; ii < aFpGradSize; ++ii) {
            int j = aFpGradNlIndex[ii];
            double tGradFpI = aGradFp[aFpGradFpIndex[ii]];
            rForceNlX[j] += aFpPx[ii]*tGradFpI;
            rForceNlY[j] += aFpPy[ii]*tGradFpI;
            rForceNlZ[j] += aFpPz[ii]*tGradFpI;
        }
    }
    static void backwardForceIndexFMA_(double[] rLossGradFp, double[] aLossGradForceNlX, double[] aLossGradForceNlY, double[] aLossGradForceNlZ,
                                       float[] aFpPx, float[] aFpPy, float[] aFpPz, short[] aFpGradNlIndex, short[] aFpGradFpIndex, int aFpGradSize) {
        for (int ii = 0; ii < aFpGradSize; ++ii) {
            int j = aFpGradNlIndex[ii];
            int fpi = aFpGradFpIndex[ii];
            rLossGradFp[fpi] += aLossGradForceNlX[j]*aFpPx[ii]
                              + aLossGradForceNlY[j]*aFpPy[ii]
                              + aLossGradForceNlZ[j]*aFpPz[ii];
        }
    }
    static void backwardForceIndexFMA_(double[] rLossGradFp, double[] aLossGradForceNlX, double[] aLossGradForceNlY, double[] aLossGradForceNlZ,
                                       double[] aFpPx, double[] aFpPy, double[] aFpPz, int[] aFpGradNlIndex, int[] aFpGradFpIndex, int aFpGradSize) {
        for (int ii = 0; ii < aFpGradSize; ++ii) {
            int j = aFpGradNlIndex[ii];
            int fpi = aFpGradFpIndex[ii];
            rLossGradFp[fpi] += aLossGradForceNlX[j]*aFpPx[ii]
                              + aLossGradForceNlY[j]*aFpPy[ii]
                              + aLossGradForceNlZ[j]*aFpPz[ii];
        }
    }
    static void backwardForceParaFMA_(double[] rGradPara, double[] aLossGradFp, double[] aGradFpGradPara, int aBasisSize, int aParaSize) {
        for (int fpi = 0, tShift = 0; fpi < aBasisSize; ++fpi, tShift+=aParaSize) {
            double tLossGradFp = aLossGradFp[fpi];
            for (int parai = 0, i = tShift; parai < aParaSize; ++parai, ++i) {
                rGradPara[parai] += tLossGradFp * aGradFpGradPara[i];
            }
        }
    }
    
    
    @ApiStatus.Internal
    protected void calRefEngFpAndAdd_(IAtomData aAtomData, double aEnergy, DataSet rData) {
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            Vector[] rFp = new Vector[tAtomNum];
            rData.mFp.add(rFp);
            for (int i = 0; i < tAtomNum; ++i) {
                Vector tFp = Vectors.zeros(mBasis.size());
                mBasis.eval(tAPC, i, tFp);
                rFp[i] = tFp;
                // 计算相对能量值
                aEnergy -= mRefEng;
            }
        }
        // 这里后添加能量，这样 rData.mEng.size() 对应正确的索引
        rData.mEng.add(aEnergy/tAtomNum);
        ++rData.mSize;
    }
    @ApiStatus.Internal
    protected void calRefEngFpPartialAndAdd_(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, DataSet rData) {
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            Vector[] rFp = new Vector[tAtomNum];
            Object[] rFpPx = new Object[tAtomNum], rFpPy = new Object[tAtomNum], rFpPz = new Object[tAtomNum];
            Object[] rFpGradNlIndex = new Object[tAtomNum], rFpGradFpIndex = new Object[tAtomNum];
            IntVector[] rNl = new IntVector[tAtomNum];
            rData.mFp.add(rFp);
            rData.mFpPx.add(rFpPx); rData.mFpPy.add(rFpPy); rData.mFpPz.add(rFpPz);
            rData.mFpGradNlIndex.add(rFpGradNlIndex); rData.mFpGradFpIndex.add(rFpGradFpIndex);
            rData.mNl.add(rNl);
            for (int i = 0; i < tAtomNum; ++i) {
                Vector tFp = Vectors.zeros(mBasis.size());
                DoubleList tFpPx = new DoubleList(1024), tFpPy = new DoubleList(1024), tFpPz = new DoubleList(1024);
                IntList tFpGradNlIndex = new IntList(1024), tFpGradFpIndex = new IntList(1024);
                mBasis.evalGrad(tAPC, i, tFp, tFpGradNlIndex, tFpGradFpIndex, tFpPx, tFpPy, tFpPz);
                rFp[i] = tFp;
                if (mHalfCache) {
                    rFpPx[i] = floatFrom_(tFpPx); rFpPy[i] = floatFrom_(tFpPy); rFpPz[i] = floatFrom_(tFpPz);
                    rFpGradNlIndex[i] = shortFrom_(tFpGradNlIndex); rFpGradFpIndex[i] = shortFrom_(tFpGradFpIndex);
                } else {
                    tFpPx.trimToSize(); tFpPy.trimToSize(); tFpPz.trimToSize();
                    tFpGradNlIndex.trimToSize(); tFpGradFpIndex.trimToSize();
                    rFpPx[i] = tFpPx; rFpPy[i] = tFpPy; rFpPz[i] = tFpPz;
                    rFpGradNlIndex[i] = tFpGradNlIndex; rFpGradFpIndex[i] = tFpGradFpIndex;
                }
                // 计算相对能量值
                aEnergy -= mRefEng;
                // 增加近邻列表，这里直接重新添加
                rNl[i] = tAPC.getNeighborList(i, mBasis.rcut());
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
        ++rData.mSize;
    }
    
    static FloatList floatFrom_(DoubleList aDoubleList) {
        FloatList rList = new FloatList(aDoubleList.size());
        aDoubleList.forEach(v -> rList.add((float)v));
        return rList;
    }
    static ShortList shortFrom_(IntList aIntList) {
        ShortList rList = new ShortList(aIntList.size());
        aIntList.forEach(v -> rList.add((short)v));
        return rList;
    }
    
    public void addTrainData(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces) {
        if (!mHasData) {
            mHasData = true;
            mHasForce = aForces!=null;
        } else {
            if (mHasForce && aForces==null) throw new IllegalArgumentException("All data MUST has forces when add force");
            if (!mHasForce && aForces!=null) throw new IllegalArgumentException("All data MUST NOT has forces when not add force");
        }
        // 添加数据
        if (mHasForce) {
            calRefEngFpPartialAndAdd_(aAtomData, aEnergy, aForces, mTrainData);
        } else {
            calRefEngFpAndAdd_(aAtomData, aEnergy, mTrainData);
        }
        mHasData = true;
    }
    
    protected void initNormBasis() {
        mNormMu.fill(0.0);
        mNormSigma.fill(0.0);
        double tDiv = 0.0;
        for (Vector[] tFp : mTrainData.mFp) for (Vector tSubFp : tFp) {
            mNormMu.plus2this(tSubFp);
            mNormSigma.operation().operate2this(tSubFp, (lhs, rhs) -> lhs + rhs*rhs);
            ++tDiv;
        }
        mNormMu.div2this(tDiv);
        mNormSigma.div2this(tDiv);
        mNormSigma.operation().operate2this(mNormMu, (lhs, rhs) -> lhs - rhs*rhs);
        mNormSigma.operation().map2this(MathEX.Fast::sqrt);
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
    }
    
    /** 开始训练模型，这里直接训练给定的步数 */
    public void train(int aEpochs, boolean aPrintLog) {
        // 重新构建归一化参数
        initNormBasis();
        initNormEng();
        // 开始训练
        mOptimizer.run(aEpochs, aPrintLog);
    }
}
