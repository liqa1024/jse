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
import jsex.nnap.basis.Basis;
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
    protected final static double DEFAULT_FORCE_WEIGHT = 0.1;
    protected final static double DEFAULT_STRESS_WEIGHT = 1.0;
    protected final static double DEFAULT_L2_LOSS_WEIGHT = 0.001;
    protected final static int DEFAULT_THREAD_NUMBER = 4;
    
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
        /** 原子种类，每个原子结构一组 */
        public final List<IntVector> mAtomType = new ArrayList<>(64);
    }
    
    protected final IOptimizer mOptimizer;
    protected final int mTypeNum;
    protected final String[] mSymbols;
    protected final IVector mRefEngs;
    protected final Basis[] mBasis;
    protected final FeedForward[][] mNN;
    protected final DataSet mTrainData;
    protected final DataSet mTestData;
    protected boolean mHasData = false;
    protected boolean mHasForce = false;
    protected final Vector[] mNormMu, mNormSigma;
    protected double mNormMuEng = 0.0, mNormSigmaEng = 0.0;
    
    private final Vector[][] mFpBuf, mGradFpBuf;
    private final List<List<List<Vector>>> mHiddenOutputsBuf, mHiddenGradsBuf, mHiddenGrads2Buf, mHiddenGrads3Buf, mHiddenGradGradsBuf;
    private final DoubleList[] mForceNlXBuf, mForceNlYBuf, mForceNlZBuf;
    private final DoubleList[] mForceXBuf, mForceYBuf, mForceZBuf;
    private final DoubleList[] mLossGradForceXBuf, mLossGradForceYBuf, mLossGradForceZBuf;
    private final DoubleList[] mLossGradForceNlXBuf, mLossGradForceNlYBuf, mLossGradForceNlZBuf;
    private final Vector[][] mLossGradFp;
    private final Vector[] mGradParaBuf;
    private final int[] mParaSizes, mHiddenSizes;
    
    protected boolean mFullCache = false;
    public Trainer setFullCache(boolean aFlag) {mFullCache = aFlag; return this;}
    
    protected double mForceWeight = DEFAULT_FORCE_WEIGHT;
    public Trainer setForceWeight(double aWeight) {mForceWeight = aWeight; return this;}
    
    protected String mUnits = DEFAULT_UNITS;
    public Trainer setUnits(String aUnits) {mUnits = aUnits; return this;}
    
    private void validHiddenBuf_(int aThreadID, IntVector aAtomType, boolean aRequireGradBackward) {
        int tAtomNum = aAtomType.size();
        List<List<Vector>> tHiddenOutputsBuf = mHiddenOutputsBuf.get(aThreadID);
        List<List<Vector>> tHiddenGradsBuf = mHiddenGradsBuf.get(aThreadID);
        List<List<Vector>> tHiddenGrads2Buf = mHiddenGrads2Buf.get(aThreadID);
        List<List<Vector>> tHiddenGrads3Buf = mHiddenGrads3Buf.get(aThreadID);
        List<List<Vector>> tHiddenGradGradsBuf = mHiddenGradGradsBuf.get(aThreadID);
        for (int typei = 0; typei < mTypeNum; ++typei) {
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
            for (int i = 0; i < tAtomNum; ++i) if (typei == aAtomType.get(i)-1) {
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
    
    Trainer(@Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber, String[] aSymbols, IVector aRefEngs, Basis[] aBasis, FeedForward[] aNN, IOptimizer aOptimizer) {
        super(new ParforThreadPool(aThreadNumber));
        if (aSymbols.length != aRefEngs.size()) throw new IllegalArgumentException("Symbols length does not match reference energies length.");
        if (aSymbols.length != aBasis.length) throw new IllegalArgumentException("Symbols length does not match reference basis length.");
        if (aSymbols.length != aNN.length) throw new IllegalArgumentException("Symbols length does not match neural network length.");
        mSymbols = aSymbols;
        mRefEngs = aRefEngs;
        mBasis = aBasis;
        mTypeNum = mSymbols.length;
        
        mNN = new FeedForward[aThreadNumber][mTypeNum];
        for (int i = 0; i < mTypeNum; ++i) {
            mNN[0][i] = aNN[i];
            for (int ti = 1; ti < aThreadNumber; ++ti) {
                mNN[ti][i] = aNN[i].threadSafeRef();
            }
        }
        
        mTrainData = new DataSet();
        mTestData = new DataSet();
        
        mNormMu = new Vector[mTypeNum];
        mNormSigma = new Vector[mTypeNum];
        for (int i = 0; i < mTypeNum; ++i) {
            int tBasisSize = mBasis[i].size();
            mNormMu[i] = Vector.zeros(tBasisSize);
            mNormSigma[i] = Vector.zeros(tBasisSize);
        }
        mFpBuf = new Vector[aThreadNumber][];
        mGradFpBuf = new Vector[aThreadNumber][];
        mLossGradFp = new Vector[aThreadNumber][];
        mHiddenOutputsBuf = new ArrayList<>(aThreadNumber);
        mHiddenGradsBuf = new ArrayList<>(aThreadNumber);
        mHiddenGrads2Buf = new ArrayList<>(aThreadNumber);
        mHiddenGrads3Buf = new ArrayList<>(aThreadNumber);
        mHiddenGradGradsBuf = new ArrayList<>(aThreadNumber);
        for (int ti = 0; ti < aThreadNumber; ++ti) {
            Vector[] tFpBuf = new Vector[mTypeNum];
            Vector[] tGradFpBuf = new Vector[mTypeNum];
            Vector[] tLossGradFp = new Vector[mTypeNum];
            List<List<Vector>> tHiddenOutputsBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGradsBuf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGrads2Buf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGrads3Buf = new ArrayList<>(mTypeNum);
            List<List<Vector>> tHiddenGradGradsBuf = new ArrayList<>(mTypeNum);
            mFpBuf[ti] = tFpBuf;
            mGradFpBuf[ti] = tGradFpBuf;
            mLossGradFp[ti] = tLossGradFp;
            mHiddenOutputsBuf.add(tHiddenOutputsBuf);
            mHiddenGradsBuf.add(tHiddenGradsBuf);
            mHiddenGrads2Buf.add(tHiddenGrads2Buf);
            mHiddenGrads3Buf.add(tHiddenGrads3Buf);
            mHiddenGradGradsBuf.add(tHiddenGradGradsBuf);
            for (int i = 0; i < mTypeNum; ++i) {
                int tBasisSize = mBasis[i].size();
                tFpBuf[i] = Vector.zeros(tBasisSize);
                tGradFpBuf[i] = Vector.zeros(tBasisSize);
                tLossGradFp[i] = Vector.zeros(tBasisSize);
                tHiddenOutputsBuf.add(new ArrayList<>(16));
                tHiddenGradsBuf.add(new ArrayList<>(16));
                tHiddenGrads2Buf.add(new ArrayList<>(16));
                tHiddenGrads3Buf.add(new ArrayList<>(16));
                tHiddenGradGradsBuf.add(new ArrayList<>(16));
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
        
        mParaSizes = new int[mTypeNum];
        mHiddenSizes = new int[mTypeNum];
        final IVector[] tParas = new IVector[mTypeNum];
        int rTotParaSize = 0;
        for (int i = 0; i < mTypeNum; ++i) {
            IVector tPara = mNN[0][i].parameters();
            int tParaSize = tPara.size();
            tParas[i] = tPara;
            mParaSizes[i] = tParaSize;
            mHiddenSizes[i] = mNN[0][i].hiddenSize();
            rTotParaSize += tParaSize;
        }
        mGradParaBuf = new Vector[aThreadNumber];
        for (int ti = 1; ti < aThreadNumber; ++ti) {
            mGradParaBuf[ti] = Vector.zeros(rTotParaSize);
        }
        
        final int fTotParaSize = rTotParaSize;
        mOptimizer = aOptimizer;
        mOptimizer.setParameter(new RefVector() {
            @Override public double get(int aIdx) {
                int tIdx = aIdx;
                for (int i = 0; i < mTypeNum; ++i) {
                    int tParaSize = mParaSizes[i];
                    if (tIdx < tParaSize) {
                        return tParas[i].get(tIdx);
                    }
                    tIdx -= tParaSize;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public void set(int aIdx, double aValue) {
                int tIdx = aIdx;
                for (int i = 0; i < mTypeNum; ++i) {
                    int tParaSize = mParaSizes[i];
                    if (tIdx < tParaSize) {
                        tParas[i].set(tIdx, aValue);
                        return;
                    }
                    tIdx -= tParaSize;
                }
                throw new IndexOutOfBoundsException(String.valueOf(aIdx));
            }
            @Override public int size() {
                return fTotParaSize;
            }
        });
        mOptimizer.setLossFunc(() -> calLoss(mTrainData, null));
        mOptimizer.setLossFuncGrad(grad -> calLoss(mTrainData, grad));
    }
    public Trainer(String[] aSymbols, IVector aRefEngs, Basis[] aBasis, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) {
        this(aThreadNumber, aSymbols, aRefEngs, aBasis, nnFrom_(aBasis), new LBFGS(100).setLineSearch());
    }
    public Trainer(String[] aSymbols, IVector aRefEngs, Basis aBasis, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aThreadNumber);}
    public Trainer(String[] aSymbols, double[] aRefEngs, Basis[] aBasis, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) {this(aSymbols, Vectors.from(aRefEngs), aBasis, aThreadNumber);}
    public Trainer(String[] aSymbols, double[] aRefEngs, Basis aBasis, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aThreadNumber);}
    public Trainer(String[] aSymbols, IVector aRefEngs, Basis[] aBasis) {this(aSymbols, aRefEngs, aBasis, DEFAULT_THREAD_NUMBER);}
    public Trainer(String[] aSymbols, IVector aRefEngs, Basis aBasis) {this(aSymbols, aRefEngs, aBasis, DEFAULT_THREAD_NUMBER);}
    public Trainer(String[] aSymbols, double[] aRefEngs, Basis[] aBasis) {this(aSymbols, aRefEngs, aBasis, DEFAULT_THREAD_NUMBER);}
    public Trainer(String[] aSymbols, double[] aRefEngs, Basis aBasis) {this(aSymbols, aRefEngs, aBasis, DEFAULT_THREAD_NUMBER);}
    private static FeedForward[] nnFrom_(Basis[] aBasis) {
        FeedForward[] rOut = new FeedForward[aBasis.length];
        for (int i = 0; i < rOut.length; ++i) {
            rOut[i] = FeedForward.init(aBasis[i].size(), DEFAULT_HIDDEN_DIMS);
        }
        return rOut;
    }
    private static Basis[] repeatBasis_(Basis aBasis, int aLen) {
        Basis[] rOut = new Basis[aLen];
        Arrays.fill(rOut, aBasis);
        return rOut;
    }
    
    @Override public int atomTypeNumber() {return mTypeNum;}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
    public Basis basis(int aType) {return mBasis[aType-1];}
    public @Unmodifiable List<Basis> basis() {return AbstractCollections.from(mBasis);}
    public FeedForward model(int aType) {return mNN[0][aType-1];}
    public @Unmodifiable List<FeedForward> models() {return AbstractCollections.from(mNN[0]);}
    public String units() {return mUnits;}
    
    protected int paraShift(int aType) {
        int rShiftPara = 0;
        for (int typei = 0; typei < aType-1; ++typei) {
            rShiftPara += mParaSizes[typei];
        }
        return rShiftPara;
    }
    
    protected double calLoss(DataSet aData, @Nullable Vector rGrad) {
        final boolean tRequireGrad = rGrad!=null;
        final boolean tFullCache = mFullCache;
        final int tThreadNum = threadNumber();
        if (tRequireGrad) {
            mGradParaBuf[0] = rGrad;
            for (Vector tGradPara : mGradParaBuf) tGradPara.fill(0.0);
        }
        Vector rLossPar = VectorCache.getZeros(tThreadNum);
        pool().parfor(aData.mSize, (i, threadID) -> {
            Vector tGradPara = mGradParaBuf[threadID];
            FeedForward[] tNN = mNN[threadID];
            Vector[] tFpBuf = mFpBuf[threadID];
            List<List<Vector>> tHiddenOutputsBuf = mHiddenOutputsBuf.get(threadID);
            List<List<Vector>> tHiddenGradsBuf = mHiddenGradsBuf.get(threadID);
            IntVector tAtomType = aData.mAtomType.get(i);
            Vector[] tFp = aData.mFp.get(i);
            int tAtomNum = tFp.length;
            if (!mHasForce) {
                if (tRequireGrad) {
                    validHiddenBuf_(threadID, tAtomType, false);
                }
                double rEng = 0.0;
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    Vector tSubFp = tFp[k];
                    Vector tSubFpBuf = tFpBuf[tType-1];
                    Vector tNormMu = mNormMu[tType-1];
                    Vector tNormSigma = mNormSigma[tType-1];
                    tSubFpBuf.fill(j -> (tSubFp.get(j) - tNormMu.get(j)) / tNormSigma.get(j));
                    rEng += tRequireGrad ? tNN[tType-1].forward(tSubFpBuf, tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k))
                                         : tNN[tType-1].eval(tSubFpBuf);
                }
                rEng /= tAtomNum;
                double tErrEng = rEng - (aData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
                rLossPar.add(threadID, tErrEng*tErrEng);
                /// backward
                if (!tRequireGrad) return;
                double tLossGradEng = 2.0 * tErrEng / tAtomNum;
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    Vector tSubFp = tFp[k];
                    Vector tSubFpBuf = tFpBuf[tType-1];
                    Vector tSubNormMu = mNormMu[tType-1];
                    Vector tSubNormSigma = mNormSigma[tType-1];
                    tSubFpBuf.fill(j -> (tSubFp.get(j) - tSubNormMu.get(j)) / tSubNormSigma.get(j));
                    int tShiftPara = paraShift(tType);
                    tNN[tType-1].backward(tLossGradEng, tSubFpBuf, tGradPara.subVec(tShiftPara, tShiftPara+mParaSizes[tType-1]),
                                          tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k));
                }
                return;
            }
            Vector[] tGradFpBuf = mGradFpBuf[threadID];
            Vector[] tLossGradFp = mLossGradFp[threadID];
            List<List<Vector>> tHiddenGrads2Buf = mHiddenGrads2Buf.get(threadID);
            List<List<Vector>> tHiddenGrads3Buf = mHiddenGrads3Buf.get(threadID);
            List<List<Vector>> tHiddenGradGradsBuf = mHiddenGradGradsBuf.get(threadID);
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
            Object[] tFpPx = aData.mFpPx.get(i);
            Object[] tFpPy = aData.mFpPy.get(i);
            Object[] tFpPz = aData.mFpPz.get(i);
            Object[] tFpGradNlIndex = aData.mFpGradNlIndex.get(i);
            Object[] tFpGradFpIndex = aData.mFpGradFpIndex.get(i);
            IntVector[] tNl = aData.mNl.get(i);
            tForceXBuf.clear(); tForceXBuf.addZeros(tAtomNum);
            tForceYBuf.clear(); tForceYBuf.addZeros(tAtomNum);
            tForceZBuf.clear(); tForceZBuf.addZeros(tAtomNum);
            if (tRequireGrad) {
                validHiddenBuf_(threadID, tAtomType, true);
            }
            double rEng = 0.0;
            for (int k = 0; k < tAtomNum; ++k) {
                int tType = tAtomType.get(k);
                Vector tSubFp = tFp[k];
                Vector tSubFpBuf = tFpBuf[tType-1];
                Vector tSubNormMu = mNormMu[tType-1];
                Vector tSubNormSigma = mNormSigma[tType-1];
                Vector tSubGradFpBuf = tGradFpBuf[tType-1];
                tSubFpBuf.fill(j -> (tSubFp.get(j) - tSubNormMu.get(j)) / tSubNormSigma.get(j));
                // cal energy
                rEng += tRequireGrad ? tNN[tType-1].forwardGrad(tSubFpBuf, tSubGradFpBuf, tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k),
                                                                tHiddenGrads2Buf.get(tType-1).get(k), tHiddenGrads3Buf.get(tType-1).get(k), tHiddenGradGradsBuf.get(tType-1).get(k))
                                     : tNN[tType-1].evalGrad(tSubFpBuf, tSubGradFpBuf);
                // cal force
                IntVector tSubNl = tNl[k];
                int tNlSize = tSubNl.size();
                tForceNlXBuf.clear(); tForceNlXBuf.addZeros(tNlSize);
                tForceNlYBuf.clear(); tForceNlYBuf.addZeros(tNlSize);
                tForceNlZBuf.clear(); tForceNlZBuf.addZeros(tNlSize);
                if (tFullCache) {
                    forwardForceIndexFMA_(tForceNlXBuf.internalData(), tForceNlYBuf.internalData(), tForceNlZBuf.internalData(), tSubGradFpBuf.internalData(),
                                          ((DoubleList)tFpPx[k]).internalData(), ((DoubleList)tFpPy[k]).internalData(), ((DoubleList)tFpPz[k]).internalData(), tSubNormSigma.internalData(),
                                          ((IntList)tFpGradNlIndex[k]).internalData(), ((IntList)tFpGradFpIndex[k]).internalData(), ((DoubleList)tFpPx[k]).size());
                } else {
                    forwardForceIndexFMA_(tForceNlXBuf.internalData(), tForceNlYBuf.internalData(), tForceNlZBuf.internalData(), tSubGradFpBuf.internalData(),
                                          ((FloatList)tFpPx[k]).internalData(), ((FloatList)tFpPy[k]).internalData(), ((FloatList)tFpPz[k]).internalData(), tSubNormSigma.internalData(),
                                          ((ShortList)tFpGradNlIndex[k]).internalData(), ((ShortList)tFpGradFpIndex[k]).internalData(), ((FloatList)tFpPx[k]).size());
                }
                for (int j = 0; j < tNlSize; ++j) {
                    double fx = tForceNlXBuf.get(j);
                    double fy = tForceNlYBuf.get(j);
                    double fz = tForceNlZBuf.get(j);
                    tForceXBuf.set(k, tForceXBuf.get(k) - fx);
                    tForceYBuf.set(k, tForceYBuf.get(k) - fy);
                    tForceZBuf.set(k, tForceZBuf.get(k) - fz);
                    int nlk = tSubNl.get(j);
                    tForceXBuf.set(nlk, tForceXBuf.get(nlk) + fx);
                    tForceYBuf.set(nlk, tForceYBuf.get(nlk) + fy);
                    tForceZBuf.set(nlk, tForceZBuf.get(nlk) + fz);
                }
            }
            // energy error
            rEng /= tAtomNum;
            double tErrEng = rEng - (aData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
            rLossPar.add(threadID, tErrEng*tErrEng);
            // force error
            double tLossForce = 0.0;
            if (tRequireGrad) {
                tLossGradForceXBuf.clear(); tLossGradForceXBuf.addZeros(tAtomNum);
                tLossGradForceYBuf.clear(); tLossGradForceYBuf.addZeros(tAtomNum);
                tLossGradForceZBuf.clear(); tLossGradForceZBuf.addZeros(tAtomNum);
            }
            double tForceLossMul = mForceWeight / (tAtomNum*3);
            Vector tForceX = aData.mForceX.get(i), tForceY = aData.mForceY.get(i), tForceZ = aData.mForceZ.get(i);
            for (int k = 0; k < tAtomNum; ++k) {
                double tErrForceX = tForceXBuf.get(k) - tForceX.get(k)/mNormSigmaEng;
                double tErrForceY = tForceYBuf.get(k) - tForceY.get(k)/mNormSigmaEng;
                double tErrForceZ = tForceZBuf.get(k) - tForceZ.get(k)/mNormSigmaEng;
                if (tRequireGrad) {
                    tLossGradForceXBuf.set(k, 2.0*tForceLossMul*tErrForceX);
                    tLossGradForceYBuf.set(k, 2.0*tForceLossMul*tErrForceY);
                    tLossGradForceZBuf.set(k, 2.0*tForceLossMul*tErrForceZ);
                }
                tLossForce += (tErrForceX*tErrForceX + tErrForceY*tErrForceY + tErrForceZ*tErrForceZ);
            }
            rLossPar.add(threadID, tForceLossMul*tLossForce);
            /// backward
            if (!tRequireGrad) return;
            double tLossGradEng = 2.0 * tErrEng / tAtomNum;
            for (int k = 0; k < tAtomNum; ++k) {
                // energy loss grad
                int tType = tAtomType.get(k);
                Vector tSubFp = tFp[k];
                Vector tSubFpBuf = tFpBuf[tType-1];
                Vector tSubNormMu = mNormMu[tType-1];
                Vector tSubNormSigma = mNormSigma[tType-1];
                tSubFpBuf.fill(j -> (tSubFp.get(j) - tSubNormMu.get(j)) / tSubNormSigma.get(j));
                int tShiftPara = paraShift(tType);
                tNN[tType-1].backward(tLossGradEng, tSubFpBuf, tGradPara.subVec(tShiftPara, tShiftPara+mParaSizes[tType-1]),
                                      tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k));
                // force loss grad
                Vector tSubLossGradFp = tLossGradFp[tType-1];
                IntVector tSubNl = tNl[k];
                int tNlSize = tSubNl.size();
                double tLossGradForceX = tLossGradForceXBuf.get(k);
                double tLossGradForceY = tLossGradForceYBuf.get(k);
                double tLossGradForceZ = tLossGradForceZBuf.get(k);
                tLossGradForceNlXBuf.clear(); tLossGradForceNlXBuf.addZeros(tNlSize);
                tLossGradForceNlYBuf.clear(); tLossGradForceNlYBuf.addZeros(tNlSize);
                tLossGradForceNlZBuf.clear(); tLossGradForceNlZBuf.addZeros(tNlSize);
                for (int j = 0; j < tNlSize; ++j) {
                    int nlk = tSubNl.get(j);
                    tLossGradForceNlXBuf.set(j, tLossGradForceXBuf.get(nlk) - tLossGradForceX);
                    tLossGradForceNlYBuf.set(j, tLossGradForceYBuf.get(nlk) - tLossGradForceY);
                    tLossGradForceNlZBuf.set(j, tLossGradForceZBuf.get(nlk) - tLossGradForceZ);
                }
                tSubLossGradFp.fill(0.0);
                if (tFullCache) {
                    backwardForceIndexFMA_(tSubLossGradFp.internalData(), tLossGradForceNlXBuf.internalData(), tLossGradForceNlYBuf.internalData(), tLossGradForceNlZBuf.internalData(),
                                           ((DoubleList)tFpPx[k]).internalData(), ((DoubleList)tFpPy[k]).internalData(), ((DoubleList)tFpPz[k]).internalData(), tSubNormSigma.internalData(),
                                           ((IntList)tFpGradNlIndex[k]).internalData(), ((IntList)tFpGradFpIndex[k]).internalData(), ((DoubleList)tFpPx[k]).size());
                } else {
                    backwardForceIndexFMA_(tSubLossGradFp.internalData(), tLossGradForceNlXBuf.internalData(), tLossGradForceNlYBuf.internalData(), tLossGradForceNlZBuf.internalData(),
                                           ((FloatList)tFpPx[k]).internalData(), ((FloatList)tFpPy[k]).internalData(), ((FloatList)tFpPz[k]).internalData(), tSubNormSigma.internalData(),
                                           ((ShortList)tFpGradNlIndex[k]).internalData(), ((ShortList)tFpGradFpIndex[k]).internalData(), ((FloatList)tFpPx[k]).size());
                }
                tNN[tType-1].gradBackward(tSubLossGradFp, tSubFpBuf, tGradPara.subVec(tShiftPara, tShiftPara+mParaSizes[tType-1]),
                                          tHiddenOutputsBuf.get(tType-1).get(k), tHiddenGradsBuf.get(tType-1).get(k),
                                          tHiddenGrads2Buf.get(tType-1).get(k), tHiddenGrads3Buf.get(tType-1).get(k), tHiddenGradGradsBuf.get(tType-1).get(k));
            }
        });
        if (tRequireGrad) {
            for (int ti = 1; ti < tThreadNum; ++ti) {
                rGrad.plus2this(mGradParaBuf[ti]);
            }
            rGrad.div2this(aData.mSize);
        }
        double tLoss = rLossPar.sum() / aData.mSize;
        VectorCache.returnVec(rLossPar);
        return tLoss;
    }
    
    static void forwardForceIndexFMA_(double[] rForceNlX, double[] rForceNlY, double[] rForceNlZ, double[] aGradFp,
                                      float[] aFpPx, float[] aFpPy, float[] aFpPz, double[] aNormSigma, short[] aFpGradNlIndex, short[] aFpGradFpIndex, int aFpGradSize) {
        for (int ii = 0; ii < aFpGradSize; ++ii) {
            int j = aFpGradNlIndex[ii];
            int fpi = aFpGradFpIndex[ii];
            double tMul = aGradFp[fpi] / aNormSigma[fpi];
            rForceNlX[j] += aFpPx[ii]*tMul;
            rForceNlY[j] += aFpPy[ii]*tMul;
            rForceNlZ[j] += aFpPz[ii]*tMul;
        }
    }
    static void forwardForceIndexFMA_(double[] rForceNlX, double[] rForceNlY, double[] rForceNlZ, double[] aGradFp,
                                      double[] aFpPx, double[] aFpPy, double[] aFpPz, double[] aNormSigma, int[] aFpGradNlIndex, int[] aFpGradFpIndex, int aFpGradSize) {
        for (int ii = 0; ii < aFpGradSize; ++ii) {
            int j = aFpGradNlIndex[ii];
            int fpi = aFpGradFpIndex[ii];
            double tMul = aGradFp[fpi] / aNormSigma[fpi];
            rForceNlX[j] += aFpPx[ii]*tMul;
            rForceNlY[j] += aFpPy[ii]*tMul;
            rForceNlZ[j] += aFpPz[ii]*tMul;
        }
    }
    static void backwardForceIndexFMA_(double[] rLossGradFp, double[] aLossGradForceNlX, double[] aLossGradForceNlY, double[] aLossGradForceNlZ,
                                       float[] aFpPx, float[] aFpPy, float[] aFpPz, double[] aNormSigma, short[] aFpGradNlIndex, short[] aFpGradFpIndex, int aFpGradSize) {
        for (int ii = 0; ii < aFpGradSize; ++ii) {
            int j = aFpGradNlIndex[ii];
            int fpi = aFpGradFpIndex[ii];
            double tNormSigma = aNormSigma[fpi];
            rLossGradFp[fpi] += (aLossGradForceNlX[j]*aFpPx[ii]
                               + aLossGradForceNlY[j]*aFpPy[ii]
                               + aLossGradForceNlZ[j]*aFpPz[ii])/tNormSigma;
        }
    }
    static void backwardForceIndexFMA_(double[] rLossGradFp, double[] aLossGradForceNlX, double[] aLossGradForceNlY, double[] aLossGradForceNlZ,
                                       double[] aFpPx, double[] aFpPy, double[] aFpPz, double[] aNormSigma, int[] aFpGradNlIndex, int[] aFpGradFpIndex, int aFpGradSize) {
        for (int ii = 0; ii < aFpGradSize; ++ii) {
            int j = aFpGradNlIndex[ii];
            int fpi = aFpGradFpIndex[ii];
            double tNormSigma = aNormSigma[fpi];
            rLossGradFp[fpi] += (aLossGradForceNlX[j]*aFpPx[ii]
                               + aLossGradForceNlY[j]*aFpPy[ii]
                               + aLossGradForceNlZ[j]*aFpPz[ii])/tNormSigma;
        }
    }
    
    
    @ApiStatus.Internal
    protected void calRefEngFpAndAdd_(IAtomData aAtomData, double aEnergy, DataSet rData) {
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            IntVector rAtomType = IntVector.zeros(tAtomNum);
            Vector[] rFp = new Vector[tAtomNum];
            rData.mAtomType.add(rAtomType);
            rData.mFp.add(rFp);
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                rAtomType.set(i, tType);
                Basis tBasis = basis(tType);
                Vector tFp = Vectors.zeros(tBasis.size());
                tBasis.eval(tAPC, i, tTypeMap, tFp);
                rFp[i] = tFp;
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
            }
        }
        // 这里后添加能量，这样 rData.mEng.size() 对应正确的索引
        rData.mEng.add(aEnergy/tAtomNum);
        ++rData.mSize;
    }
    @ApiStatus.Internal
    protected void calRefEngFpPartialAndAdd_(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, DataSet rData) {
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            IntVector rAtomType = IntVector.zeros(tAtomNum);
            Vector[] rFp = new Vector[tAtomNum];
            Object[] rFpPx = new Object[tAtomNum], rFpPy = new Object[tAtomNum], rFpPz = new Object[tAtomNum];
            Object[] rFpGradNlIndex = new Object[tAtomNum], rFpGradFpIndex = new Object[tAtomNum];
            IntVector[] rNl = new IntVector[tAtomNum];
            rData.mAtomType.add(rAtomType);
            rData.mFp.add(rFp);
            rData.mFpPx.add(rFpPx); rData.mFpPy.add(rFpPy); rData.mFpPz.add(rFpPz);
            rData.mFpGradNlIndex.add(rFpGradNlIndex); rData.mFpGradFpIndex.add(rFpGradFpIndex);
            rData.mNl.add(rNl);
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                rAtomType.set(i, tType);
                Basis tBasis = basis(tType);
                Vector tFp = Vectors.zeros(tBasis.size());
                DoubleList tFpPx = new DoubleList(1024), tFpPy = new DoubleList(1024), tFpPz = new DoubleList(1024);
                IntList tFpGradNlIndex = new IntList(1024), tFpGradFpIndex = new IntList(1024);
                tBasis.evalGrad(tAPC, i, tTypeMap, tFp, tFpGradNlIndex, tFpGradFpIndex, tFpPx, tFpPy, tFpPz);
                rFp[i] = tFp;
                if (mFullCache) {
                    tFpPx.trimToSize(); tFpPy.trimToSize(); tFpPz.trimToSize();
                    tFpGradNlIndex.trimToSize(); tFpGradFpIndex.trimToSize();
                    rFpPx[i] = tFpPx; rFpPy[i] = tFpPy; rFpPz[i] = tFpPz;
                    rFpGradNlIndex[i] = tFpGradNlIndex; rFpGradFpIndex[i] = tFpGradFpIndex;
                } else {
                    rFpPx[i] = floatFrom_(tFpPx); rFpPy[i] = floatFrom_(tFpPy); rFpPz[i] = floatFrom_(tFpPz);
                    rFpGradNlIndex[i] = shortFrom_(tFpGradNlIndex); rFpGradFpIndex[i] = shortFrom_(tFpGradFpIndex);
                }
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
                // 增加近邻列表，这里直接重新添加
                rNl[i] = tAPC.getNeighborList(i, tBasis.rcut());
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
        for (int i = 0; i < mTypeNum; ++i) {
            mNormMu[i].fill(0.0);
            mNormSigma[i].fill(0.0);
        }
        IntVector tDiv = IntVectorCache.getZeros(mTypeNum);
        for (int i = 0; i < mTrainData.mSize; ++i) {
            IntVector tAtomType = mTrainData.mAtomType.get(i);
            Vector[] tFp = mTrainData.mFp.get(i);
            int tAtomNum = tFp.length;
            for (int k = 0; k < tAtomNum; ++k) {
                int tType = tAtomType.get(k);
                Vector tSubFp = tFp[k];
                mNormMu[tType-1].plus2this(tSubFp);
                mNormSigma[tType-1].operation().operate2this(tSubFp, (lhs, rhs) -> lhs + rhs * rhs);
                tDiv.increment(tType-1);
            }
        }
        for (int i = 0; i < mTypeNum; ++i) {
            int tDivI = tDiv.get(i);
            if (tDivI == 0) {
                UT.Code.warning("number of atoms of type `"+mSymbols[i]+"` is zero, check your input or dataset.");
                mNormMu[i].fill(0.0);
                mNormSigma[i].fill(1.0);
            } else {
                mNormMu[i].div2this(tDivI);
                mNormSigma[i].div2this(tDivI);
                mNormSigma[i].operation().operate2this(mNormMu[i], (lhs, rhs) -> lhs - rhs*rhs);
                mNormSigma[i].operation().map2this(v -> MathEX.Code.numericEqual(v, 0.0) ? 1.0 : MathEX.Fast.sqrt(v));
            }
        }
        IntVectorCache.returnVec(tDiv);
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
    
    
    /** 保存训练的势函数 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        List rModels = new ArrayList();
        for (int i = 0; i < mTypeNum; ++i) {
            Map rBasis = new LinkedHashMap();
            mBasis[i].save(rBasis);
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
