package jsex.nnap;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IAtomData;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
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
    
    protected static class DataSet {
        public int mSize = 0;
        /** 按照原子结构排列，每个原子结构中每个原子对应一个向量（不同种类可能长度不等）*/
        public final List<Vector[]> mFp = new ArrayList<>(64);
        /** 按照原子结构排列，每个原子结构中每个原子对应一个向量，根据近邻列表展开（不同原子长度不等）*/
        public final List<Vector[]> mFpPx = new ArrayList<>(64), mFpPy = new ArrayList<>(64), mFpPz = new ArrayList<>(64);
        /** 按照原子结构排列，每个原子结构中每个原子对应一个向量，根据近邻列表展开（不同原子长度不等）*/
        public final List<IntVector[]> mFpGradNlIndex = new ArrayList<>(64), mFpGradFpIndex = new ArrayList<>(64);
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
    private final Vector mGradParaBuf1, mGradParaBuf2;
    private final RowMatrix mGradFpGradParaBuf;
    private final DoubleList mForceNlXBuf, mForceNlYBuf, mForceNlZBuf;
    private final DoubleList mForceXBuf, mForceYBuf, mForceZBuf;
    private final List<Vector> mForceGradNlXBuf, mForceGradNlYBuf, mForceGradNlZBuf;
    private final List<Vector> mForceGradXBuf, mForceGradYBuf, mForceGradZBuf;
    private final int mParaSize;
    
    protected double mForceWeight = DEFAULT_FORCE_WEIGHT;
    
    private static void validSize_(DoubleList aData, int aSize) {
        aData.ensureCapacity(aSize);
        aData.setInternalDataSize(aSize);
    }
    private void validForceGradNl_(int aSize) {
        for (Vector tGrad : mForceGradNlXBuf) tGrad.fill(0.0);
        for (Vector tGrad : mForceGradNlYBuf) tGrad.fill(0.0);
        for (Vector tGrad : mForceGradNlZBuf) tGrad.fill(0.0);
        while (mForceGradNlXBuf.size() < aSize) mForceGradNlXBuf.add(Vectors.zeros(mParaSize));
        while (mForceGradNlYBuf.size() < aSize) mForceGradNlYBuf.add(Vectors.zeros(mParaSize));
        while (mForceGradNlZBuf.size() < aSize) mForceGradNlZBuf.add(Vectors.zeros(mParaSize));
    }
    private void validForceGrad_(int aSize) {
        for (Vector tGrad : mForceGradXBuf) tGrad.fill(0.0);
        for (Vector tGrad : mForceGradYBuf) tGrad.fill(0.0);
        for (Vector tGrad : mForceGradZBuf) tGrad.fill(0.0);
        while (mForceGradXBuf.size() < aSize) mForceGradXBuf.add(Vectors.zeros(mParaSize));
        while (mForceGradYBuf.size() < aSize) mForceGradYBuf.add(Vectors.zeros(mParaSize));
        while (mForceGradZBuf.size() < aSize) mForceGradZBuf.add(Vectors.zeros(mParaSize));
    }
    
    TrainerNative(double aRefEng, Basis aBasis, FeedForward aNN, IOptimizer aOptimizer) {
        mRefEng = aRefEng;
        mBasis = aBasis;
        mNN = aNN;
        
        mTrainData = new DataSet();
        mTestData = new DataSet();
        int tSize = mBasis.size();
        mNormMu = Vector.zeros(tSize);
        mNormSigma = Vector.zeros(tSize);
        mFpBuf = Vector.zeros(tSize);
        mGradFpBuf = Vector.zeros(tSize);
        mForceNlXBuf = new DoubleList(16);
        mForceNlYBuf = new DoubleList(16);
        mForceNlZBuf = new DoubleList(16);
        mForceXBuf = new DoubleList(16);
        mForceYBuf = new DoubleList(16);
        mForceZBuf = new DoubleList(16);
        mForceGradNlXBuf = new ArrayList<>(16);
        mForceGradNlYBuf = new ArrayList<>(16);
        mForceGradNlZBuf = new ArrayList<>(16);
        mForceGradXBuf = new ArrayList<>(16);
        mForceGradYBuf = new ArrayList<>(16);
        mForceGradZBuf = new ArrayList<>(16);
        
        mOptimizer = aOptimizer;
        IVector tPara = mNN.parameters();
        mParaSize = tPara.size();
        mGradParaBuf1 = Vectors.zeros(mParaSize);
        mGradParaBuf2 = Vectors.zeros(mParaSize);
        mGradFpGradParaBuf = RowMatrix.zeros(tSize, mParaSize);
        mOptimizer.setParameter(tPara);
        mOptimizer.setLossFunc(() -> {
            double rLoss = 0.0;
            for (int i = 0; i < mTrainData.mSize; ++i) {
                //noinspection IfStatementWithIdenticalBranches
                if (!mHasForce) {
                    double rEng = 0.0;
                    Vector[] tFp = mTrainData.mFp.get(i);
                    for (Vector tSubFp : tFp) {
                        mFpBuf.fill(j -> (tSubFp.get(j) - mNormMu.get(j)) / mNormSigma.get(j));
                        rEng += mNN.forward(mFpBuf);
                    }
                    rEng /= tFp.length;
                    double tErr = rEng - (mTrainData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
                    rLoss += tErr*tErr;
                } else {
                    double rEng = 0.0;
                    Vector[] tFp = mTrainData.mFp.get(i);
                    Vector[] tFpPx = mTrainData.mFpPx.get(i);
                    Vector[] tFpPy = mTrainData.mFpPy.get(i);
                    Vector[] tFpPz = mTrainData.mFpPz.get(i);
                    IntVector[] tFpGradNlIndex = mTrainData.mFpGradNlIndex.get(i);
                    IntVector[] tFpGradFpIndex = mTrainData.mFpGradFpIndex.get(i);
                    IntVector[] tNl = mTrainData.mNl.get(i);
                    final int tAtomNum = tFp.length;
                    mForceXBuf.clear(); mForceXBuf.addZeros(tAtomNum);
                    mForceYBuf.clear(); mForceYBuf.addZeros(tAtomNum);
                    mForceZBuf.clear(); mForceZBuf.addZeros(tAtomNum);
                    for (int k = 0; k < tAtomNum; ++k) {
                        // cal energy
                        Vector tSubFp = tFp[k];
                        mFpBuf.fill(j -> (tSubFp.get(j) - mNormMu.get(j)) / mNormSigma.get(j));
                        rEng += mNN.backward(mFpBuf, mGradFpBuf);
                        // cal force
                        Vector tSubFpPx = tFpPx[k], tSubFpPy = tFpPy[k], tSubFpPz = tFpPz[k];
                        IntVector tSubFpGradNlIndex = tFpGradNlIndex[k], tSubFpGradFpIndex = tFpGradFpIndex[k];
                        IntVector tSubNl = tNl[k];
                        int tNlSize = tSubNl.size();
                        mForceNlXBuf.clear(); mForceNlXBuf.addZeros(tNlSize);
                        mForceNlYBuf.clear(); mForceNlYBuf.addZeros(tNlSize);
                        mForceNlZBuf.clear(); mForceNlZBuf.addZeros(tNlSize);
                        int tFpGradSize = tSubFpPx.size();
                        for (int ii = 0; ii < tFpGradSize; ++ii) {
                            int j = tSubFpGradNlIndex.get(ii);
                            double tGradFpI = mGradFpBuf.get(tSubFpGradFpIndex.get(ii));
                            mForceNlXBuf.set(j, mForceNlXBuf.get(j) + tSubFpPx.get(ii)*tGradFpI);
                            mForceNlYBuf.set(j, mForceNlYBuf.get(j) + tSubFpPy.get(ii)*tGradFpI);
                            mForceNlZBuf.set(j, mForceNlZBuf.get(j) + tSubFpPz.get(ii)*tGradFpI);
                        }
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
                    }
                    // energy error
                    rEng /= tAtomNum;
                    double tErr = rEng - (mTrainData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
                    rLoss += tErr*tErr;
                    // force error
                    double tLossForce = 0.0;
                    Vector tForceX = mTrainData.mForceX.get(i), tForceY = mTrainData.mForceY.get(i), tForceZ = mTrainData.mForceZ.get(i);
                    for (int k = 0; k < tAtomNum; ++k) {
                        double tErrX = mForceXBuf.get(k) - tForceX.get(k)/mNormSigmaEng;
                        double tErrY = mForceYBuf.get(k) - tForceY.get(k)/mNormSigmaEng;
                        double tErrZ = mForceZBuf.get(k) - tForceZ.get(k)/mNormSigmaEng;
                        tLossForce += (tErrX*tErrX + tErrY*tErrY + tErrZ*tErrZ);
                    }
                    rLoss += mForceWeight * tLossForce;
                }
            }
            return rLoss / mTrainData.mSize;
        });
        mOptimizer.setLossFuncGrad(grad -> {
            grad.fill(0.0);
            double rLoss = 0.0;
            for (int i = 0; i < mTrainData.mSize; ++i) {
                //noinspection IfStatementWithIdenticalBranches
                if (!mHasForce) {
                    double rEng = 0.0;
                    Vector[] tFp = mTrainData.mFp.get(i);
                    mGradParaBuf2.fill(0.0);
                    for (Vector tSubFp : tFp) {
                        mFpBuf.fill(j -> (tSubFp.get(j) - mNormMu.get(j)) / mNormSigma.get(j));
                        rEng += aNN.backwardFull(mFpBuf, null, mGradParaBuf1);
                        mGradParaBuf2.plus2this(mGradParaBuf1);
                    }
                    rEng /= tFp.length;
                    double tErr = rEng - (mTrainData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
                    grad.operation().mplus2this(mGradParaBuf2, 2.0 * tErr / tFp.length);
                    rLoss += tErr*tErr;
                } else {
                    double rEng = 0.0;
                    Vector[] tFp = mTrainData.mFp.get(i);
                    Vector[] tFpPx = mTrainData.mFpPx.get(i);
                    Vector[] tFpPy = mTrainData.mFpPy.get(i);
                    Vector[] tFpPz = mTrainData.mFpPz.get(i);
                    IntVector[] tFpGradNlIndex = mTrainData.mFpGradNlIndex.get(i);
                    IntVector[] tFpGradFpIndex = mTrainData.mFpGradFpIndex.get(i);
                    IntVector[] tNl = mTrainData.mNl.get(i);
                    final int tAtomNum = tFp.length;
                    mForceXBuf.clear(); mForceXBuf.addZeros(tAtomNum);
                    mForceYBuf.clear(); mForceYBuf.addZeros(tAtomNum);
                    mForceZBuf.clear(); mForceZBuf.addZeros(tAtomNum);
                    validForceGrad_(tAtomNum);
                    mGradParaBuf2.fill(0.0);
                    for (int k = 0; k < tAtomNum; ++k) {
                        // cal energy
                        Vector tSubFp = tFp[k];
                        mFpBuf.fill(j -> (tSubFp.get(j) - mNormMu.get(j)) / mNormSigma.get(j));
                        rEng += aNN.backwardDoubleFull(mFpBuf, mGradFpBuf, mGradParaBuf1, mGradFpGradParaBuf.asVecRow());
                        mGradParaBuf2.plus2this(mGradParaBuf1);
                        // cal force
                        Vector tSubFpPx = tFpPx[k], tSubFpPy = tFpPy[k], tSubFpPz = tFpPz[k];
                        IntVector tSubFpGradNlIndex = tFpGradNlIndex[k], tSubFpGradFpIndex = tFpGradFpIndex[k];
                        IntVector tSubNl = tNl[k];
                        int tNlSize = tSubNl.size();
                        mForceNlXBuf.clear(); mForceNlXBuf.addZeros(tNlSize);
                        mForceNlYBuf.clear(); mForceNlYBuf.addZeros(tNlSize);
                        mForceNlZBuf.clear(); mForceNlZBuf.addZeros(tNlSize);
                        validForceGradNl_(tNlSize);
                        int tFpGradSize = tSubFpPx.size();
                        for (int ii = 0; ii < tFpGradSize; ++ii) {
                            int j = tSubFpGradNlIndex.get(ii);
                            double tGradFpI = mGradFpBuf.get(tSubFpGradFpIndex.get(ii));
                            ShiftVector tGradFpIGradPara = mGradFpGradParaBuf.row(tSubFpGradFpIndex.get(ii));
                            mForceNlXBuf.set(j, mForceNlXBuf.get(j) + tSubFpPx.get(ii)*tGradFpI);
                            mForceNlYBuf.set(j, mForceNlYBuf.get(j) + tSubFpPy.get(ii)*tGradFpI);
                            mForceNlZBuf.set(j, mForceNlZBuf.get(j) + tSubFpPz.get(ii)*tGradFpI);
                            mForceGradNlXBuf.get(j).operation().mplus2this(tGradFpIGradPara, tSubFpPx.get(ii));
                            mForceGradNlYBuf.get(j).operation().mplus2this(tGradFpIGradPara, tSubFpPy.get(ii));
                            mForceGradNlZBuf.get(j).operation().mplus2this(tGradFpIGradPara, tSubFpPz.get(ii));
                        }
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
                            
                            mForceGradXBuf.get(k).operation().minus2this(mForceGradNlXBuf.get(j));
                            mForceGradYBuf.get(k).operation().minus2this(mForceGradNlYBuf.get(j));
                            mForceGradZBuf.get(k).operation().minus2this(mForceGradNlZBuf.get(j));
                            mForceGradXBuf.get(nlk).operation().plus2this(mForceGradNlXBuf.get(j));
                            mForceGradYBuf.get(nlk).operation().plus2this(mForceGradNlYBuf.get(j));
                            mForceGradZBuf.get(nlk).operation().plus2this(mForceGradNlZBuf.get(j));
                        }
                    }
                    // energy error
                    rEng /= tAtomNum;
                    double tErr = rEng - (mTrainData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
                    grad.operation().mplus2this(mGradParaBuf2, 2.0 * tErr / tAtomNum);
                    rLoss += tErr*tErr;
                    // force error
                    double tLossForce = 0.0;
                    mGradParaBuf2.fill(0.0);
                    Vector tForceX = mTrainData.mForceX.get(i), tForceY = mTrainData.mForceY.get(i), tForceZ = mTrainData.mForceZ.get(i);
                    for (int k = 0; k < tAtomNum; ++k) {
                        double tErrX = mForceXBuf.get(k) - tForceX.get(k)/mNormSigmaEng;
                        double tErrY = mForceYBuf.get(k) - tForceY.get(k)/mNormSigmaEng;
                        double tErrZ = mForceZBuf.get(k) - tForceZ.get(k)/mNormSigmaEng;
                        mGradParaBuf2.operation().mplus2this(mForceGradXBuf.get(k), 2.0*tErrX);
                        mGradParaBuf2.operation().mplus2this(mForceGradYBuf.get(k), 2.0*tErrY);
                        mGradParaBuf2.operation().mplus2this(mForceGradZBuf.get(k), 2.0*tErrZ);
                        tLossForce += (tErrX*tErrX + tErrY*tErrY + tErrZ*tErrZ);
                    }
                    grad.operation().mplus2this(mGradParaBuf2, mForceWeight);
                    rLoss += mForceWeight * tLossForce;
                }
            }
            grad.div2this(mTrainData.mSize);
            return rLoss / mTrainData.mSize;
        });
    }
    public TrainerNative(double aRefEng, Basis aBasis, IOptimizer aOptimizer) {
        this(aRefEng, aBasis, FeedForward.init(aBasis.size(), DEFAULT_HIDDEN_DIMS), aOptimizer);
    }
    public TrainerNative(double aRefEng, Basis aBasis) {
        this(aRefEng, aBasis, new Adam());
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
            Vector[] rFpPx = new Vector[tAtomNum], rFpPy = new Vector[tAtomNum], rFpPz = new Vector[tAtomNum];
            IntVector[] rFpGradNlIndex = new IntVector[tAtomNum], rFpGradFpIndex = new IntVector[tAtomNum];
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
                tFpPx.trimToSize(); tFpPy.trimToSize(); tFpPz.trimToSize();
                tFpGradNlIndex.trimToSize(); tFpGradFpIndex.trimToSize();
                rFp[i] = tFp;
                rFpPx[i] = tFpPx.asVec(); rFpPy[i] = tFpPy.asVec(); rFpPz[i] = tFpPz.asVec();
                rFpGradNlIndex[i] = tFpGradNlIndex.asVec(); rFpGradFpIndex[i] = tFpGradFpIndex.asVec();
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
