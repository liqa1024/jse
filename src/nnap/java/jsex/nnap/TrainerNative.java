package jsex.nnap;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IAtomData;
import jse.atom.IHasSymbol;
import jse.cache.IntVectorCache;
import jse.code.IO;
import jse.code.UT;
import jse.code.collection.*;
import jse.code.io.ISavable;
import jse.code.timer.AccumulatedTimer;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jse.opt.IOptimizer;
import jse.opt.LBFGS;
import jsex.nnap.basis.Basis;
import jsex.nnap.nn.FeedForward;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

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
public class TrainerNative implements IHasSymbol, ISavable {
    protected final static String DEFAULT_UNITS = "metal";
    protected final static int[] DEFAULT_HIDDEN_DIMS = {32, 32}; // 现在统一默认为 32, 32
    protected final static double DEFAULT_FORCE_WEIGHT = 0.1;
    protected final static double DEFAULT_STRESS_WEIGHT = 1.0;
    protected final static double DEFAULT_L2_LOSS_WEIGHT = 0.001;
    
    public final static AccumulatedTimer NN_TIMER = new AccumulatedTimer(), NL_TIMER = new AccumulatedTimer(),
        INDEX1_TIMER = new AccumulatedTimer(), INDEX2_TIMER = new AccumulatedTimer();
    
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
    protected final FeedForward[] mNN;
    protected final DataSet mTrainData;
    protected final DataSet mTestData;
    protected boolean mHasData = false;
    protected boolean mHasForce = false;
    protected final Vector[] mNormMu, mNormSigma;
    protected double mNormMuEng = 0.0, mNormSigmaEng = 0.0;
    
    private final Vector[] mFpBuf, mGradFpBuf;
    private final List<List<Vector>> mHiddenOutputsBuf, mHiddenGradsBuf, mHiddenGrads2Buf, mHiddenGrads3Buf, mHiddenGradGradsBuf;
    private final DoubleList mForceNlXBuf, mForceNlYBuf, mForceNlZBuf;
    private final DoubleList mForceXBuf, mForceYBuf, mForceZBuf;
    private final DoubleList mLossGradForceXBuf, mLossGradForceYBuf, mLossGradForceZBuf;
    private final DoubleList mLossGradForceNlXBuf, mLossGradForceNlYBuf, mLossGradForceNlZBuf;
    private final Vector[] mLossGradFp;
    private final int[] mParaSizes, mHiddenSizes;
    
    protected boolean mFullCache = false;
    public TrainerNative setFullCache(boolean aFlag) {mFullCache = aFlag; return this;}
    
    protected double mForceWeight = DEFAULT_FORCE_WEIGHT;
    public TrainerNative setForceWeight(double aWeight) {mForceWeight = aWeight; return this;}
    
    protected String mUnits = DEFAULT_UNITS;
    public TrainerNative setUnits(String aUnits) {mUnits = aUnits; return this;}
    
    private void validHiddenBuf_(IntVector aAtomType, boolean aRequireGradBackward) {
        int tAtomNum = aAtomType.size();
        for (int typei = 0; typei < mTypeNum; ++typei) {
            List<Vector> tHiddenOutputsBuf = mHiddenOutputsBuf.get(typei);
            List<Vector> tHiddenGradsBuf = mHiddenGradsBuf.get(typei);
            List<Vector> tHiddenGrads2Buf = mHiddenGrads2Buf.get(typei);
            List<Vector> tHiddenGrads3Buf = mHiddenGrads3Buf.get(typei);
            List<Vector> tHiddenGradGradsBuf = mHiddenGradGradsBuf.get(typei);
            while (tHiddenOutputsBuf.size() < tAtomNum) tHiddenOutputsBuf.add(Vector.zeros(mHiddenSizes[typei]));
            while (tHiddenGradsBuf.size() < tAtomNum) tHiddenGradsBuf.add(Vector.zeros(mHiddenSizes[typei]));
            if (aRequireGradBackward) {
                while (tHiddenGrads2Buf.size() < tAtomNum) tHiddenGrads2Buf.add(Vector.zeros(mHiddenSizes[typei]));
                while (tHiddenGrads3Buf.size() < tAtomNum) tHiddenGrads3Buf.add(Vector.zeros(mHiddenSizes[typei]));
                while (tHiddenGradGradsBuf.size() < tAtomNum) tHiddenGradGradsBuf.add(Vector.zeros(mHiddenSizes[typei]));
            }
            for (int i = 0; i < tAtomNum; ++i) if (typei == aAtomType.get(i)-1) {
                tHiddenOutputsBuf.get(i).fill(0.0);
                tHiddenGradsBuf.get(i).fill(0.0);
                if (aRequireGradBackward) {
                    tHiddenGrads2Buf.get(i).fill(0.0);
                    tHiddenGrads3Buf.get(i).fill(0.0);
                    tHiddenGradGradsBuf.get(i).fill(0.0);
                }
            }
        }
    }
    
    TrainerNative(String[] aSymbols, IVector aRefEngs, Basis[] aBasis, FeedForward[] aNN, IOptimizer aOptimizer) {
        if (aSymbols.length != aRefEngs.size()) throw new IllegalArgumentException("Symbols length does not match reference energies length.");
        if (aSymbols.length != aBasis.length) throw new IllegalArgumentException("Symbols length does not match reference basis length.");
        if (aSymbols.length != aNN.length) throw new IllegalArgumentException("Symbols length does not match neural network length.");
        mSymbols = aSymbols;
        mRefEngs = aRefEngs;
        mBasis = aBasis;
        mNN = aNN;
        mTypeNum = mSymbols.length;
        
        mTrainData = new DataSet();
        mTestData = new DataSet();
        mNormMu = new Vector[mTypeNum];
        mNormSigma = new Vector[mTypeNum];
        mFpBuf = new Vector[mTypeNum];
        mGradFpBuf = new Vector[mTypeNum];
        mLossGradFp = new Vector[mTypeNum];
        mHiddenOutputsBuf = new ArrayList<>(mTypeNum);
        mHiddenGradsBuf = new ArrayList<>(mTypeNum);
        mHiddenGrads2Buf = new ArrayList<>(mTypeNum);
        mHiddenGrads3Buf = new ArrayList<>(mTypeNum);
        mHiddenGradGradsBuf = new ArrayList<>(mTypeNum);
        for (int i = 0; i < mTypeNum; ++i) {
            int tBasisSize = mBasis[i].size();
            mNormMu[i] = Vector.zeros(tBasisSize);
            mNormSigma[i] = Vector.zeros(tBasisSize);
            mFpBuf[i] = Vector.zeros(tBasisSize);
            mGradFpBuf[i] = Vector.zeros(tBasisSize);
            mLossGradFp[i] = Vector.zeros(tBasisSize);
            mHiddenOutputsBuf.add(new ArrayList<>(16));
            mHiddenGradsBuf.add(new ArrayList<>(16));
            mHiddenGrads2Buf.add(new ArrayList<>(16));
            mHiddenGrads3Buf.add(new ArrayList<>(16));
            mHiddenGradGradsBuf.add(new ArrayList<>(16));
        }
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
        
        mParaSizes = new int[mTypeNum];
        mHiddenSizes = new int[mTypeNum];
        final IVector[] tParas = new IVector[mTypeNum];
        int rTotParaSize = 0;
        for (int i = 0; i < mTypeNum; ++i) {
            IVector tPara = mNN[i].parameters();
            int tParaSize = tPara.size();
            tParas[i] = tPara;
            mParaSizes[i] = tParaSize;
            mHiddenSizes[i] = mNN[i].hiddenSize();
            rTotParaSize += tParaSize;
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
    public TrainerNative(String[] aSymbols, IVector aRefEngs, Basis[] aBasis, IOptimizer aOptimizer) {
        this(aSymbols, aRefEngs, aBasis, nnFrom_(aBasis), aOptimizer);
    }
    public TrainerNative(String[] aSymbols, IVector aRefEngs, Basis[] aBasis) {
        this(aSymbols, aRefEngs, aBasis, new LBFGS(100).setLineSearch());
    }
    private static FeedForward[] nnFrom_(Basis[] aBasis) {
        FeedForward[] rOut = new FeedForward[aBasis.length];
        for (int i = 0; i < rOut.length; ++i) {
            rOut[i] = FeedForward.init(aBasis[i].size(), DEFAULT_HIDDEN_DIMS);
        }
        return rOut;
    }
    public TrainerNative(String[] aSymbols, IVector aRefEngs, Basis aBasis) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length));}
    public TrainerNative(String[] aSymbols, double[] aRefEngs, Basis[] aBasis) {this(aSymbols, Vectors.from(aRefEngs), aBasis);}
    public TrainerNative(String[] aSymbols, double[] aRefEngs, Basis aBasis) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length));}
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
    public FeedForward model(int aType) {return mNN[aType-1];}
    public @Unmodifiable List<FeedForward> models() {return AbstractCollections.from(mNN);}
    public String units() {return mUnits;}
    
    protected int paraShift(int aType) {
        int rShiftPara = 0;
        for (int typei = 0; typei < aType-1; ++typei) {
            rShiftPara += mParaSizes[typei];
        }
        return rShiftPara;
    }
    
    protected double calLoss(DataSet aData, @Nullable Vector rGrad) {
        final boolean tFullCache = mFullCache;
        if (rGrad!=null) rGrad.fill(0.0);
        double rLoss = 0.0;
        for (int i = 0; i < aData.mSize; ++i) {
            //noinspection IfStatementWithIdenticalBranches
            if (!mHasForce) {
                IntVector tAtomType = aData.mAtomType.get(i);
                Vector[] tFp = aData.mFp.get(i);
                final int tAtomNum = tFp.length;
                if (rGrad != null) {
                    validHiddenBuf_(tAtomType, false);
                }
                double rEng = 0.0;
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    Vector tSubFp = tFp[k];
                    Vector tFpBuf = mFpBuf[tType-1];
                    Vector tNormMu = mNormMu[tType-1];
                    Vector tNormSigma = mNormSigma[tType-1];
                    tFpBuf.fill(j -> (tSubFp.get(j) - tNormMu.get(j)) / tNormSigma.get(j));
                    rEng += rGrad==null ? mNN[tType-1].eval(tFpBuf) :
                                          mNN[tType-1].forward(tFpBuf, mHiddenOutputsBuf.get(tType-1).get(k), mHiddenGradsBuf.get(tType-1).get(k));
                }
                rEng /= tAtomNum;
                double tErrEng = rEng - (aData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
                rLoss += tErrEng * tErrEng;
                /// backward
                if (rGrad==null) continue;
                double tLossGradEng = 2.0 * tErrEng / tAtomNum;
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    Vector tSubFp = tFp[k];
                    Vector tFpBuf = mFpBuf[tType-1];
                    Vector tNormMu = mNormMu[tType-1];
                    Vector tNormSigma = mNormSigma[tType-1];
                    tFpBuf.fill(j -> (tSubFp.get(j) - tNormMu.get(j)) / tNormSigma.get(j));
                    int tShiftPara = paraShift(tType);
                    mNN[tType-1].backward(tLossGradEng, tFpBuf, rGrad.subVec(tShiftPara, tShiftPara+mParaSizes[tType-1]),
                                          mHiddenOutputsBuf.get(tType-1).get(k), mHiddenGradsBuf.get(tType-1).get(k));
                }
            } else {
                IntVector tAtomType = aData.mAtomType.get(i);
                Vector[] tFp = aData.mFp.get(i);
                Object[] tFpPx = aData.mFpPx.get(i);
                Object[] tFpPy = aData.mFpPy.get(i);
                Object[] tFpPz = aData.mFpPz.get(i);
                Object[] tFpGradNlIndex = aData.mFpGradNlIndex.get(i);
                Object[] tFpGradFpIndex = aData.mFpGradFpIndex.get(i);
                IntVector[] tNl = aData.mNl.get(i);
                final int tAtomNum = tFp.length;
                mForceXBuf.clear(); mForceXBuf.addZeros(tAtomNum);
                mForceYBuf.clear(); mForceYBuf.addZeros(tAtomNum);
                mForceZBuf.clear(); mForceZBuf.addZeros(tAtomNum);
                if (rGrad != null) {
                    validHiddenBuf_(tAtomType, true);
                }
                double rEng = 0.0;
                for (int k = 0; k < tAtomNum; ++k) {
                    int tType = tAtomType.get(k);
                    Vector tSubFp = tFp[k];
                    Vector tFpBuf = mFpBuf[tType-1];
                    Vector tNormMu = mNormMu[tType-1];
                    Vector tNormSigma = mNormSigma[tType-1];
                    Vector tGradFpBuf = mGradFpBuf[tType-1];
                    tFpBuf.fill(j -> (tSubFp.get(j) - tNormMu.get(j)) / tNormSigma.get(j));
                    // cal energy
                    NN_TIMER.from();
                    rEng += rGrad==null ? mNN[tType-1].evalGrad(tFpBuf, tGradFpBuf) :
                                          mNN[tType-1].forwardGrad(tFpBuf, tGradFpBuf, mHiddenOutputsBuf.get(tType-1).get(k), mHiddenGradsBuf.get(tType-1).get(k),
                                                                   mHiddenGrads2Buf.get(tType-1).get(k), mHiddenGrads3Buf.get(tType-1).get(k), mHiddenGradGradsBuf.get(tType-1).get(k));
                    NN_TIMER.to();
                    // cal force
                    IntVector tSubNl = tNl[k];
                    int tNlSize = tSubNl.size();
                    INDEX1_TIMER.from();
                    mForceNlXBuf.clear(); mForceNlXBuf.addZeros(tNlSize);
                    mForceNlYBuf.clear(); mForceNlYBuf.addZeros(tNlSize);
                    mForceNlZBuf.clear(); mForceNlZBuf.addZeros(tNlSize);
                    if (tFullCache) {
                        forwardForceIndexFMA_(mForceNlXBuf.internalData(), mForceNlYBuf.internalData(), mForceNlZBuf.internalData(), tGradFpBuf.internalData(),
                                              ((DoubleList)tFpPx[k]).internalData(), ((DoubleList)tFpPy[k]).internalData(), ((DoubleList)tFpPz[k]).internalData(), tNormSigma.internalData(),
                                              ((IntList)tFpGradNlIndex[k]).internalData(), ((IntList)tFpGradFpIndex[k]).internalData(), ((DoubleList)tFpPx[k]).size());
                    } else {
                        forwardForceIndexFMA_(mForceNlXBuf.internalData(), mForceNlYBuf.internalData(), mForceNlZBuf.internalData(), tGradFpBuf.internalData(),
                                              ((FloatList)tFpPx[k]).internalData(), ((FloatList)tFpPy[k]).internalData(), ((FloatList)tFpPz[k]).internalData(), tNormSigma.internalData(),
                                              ((ShortList)tFpGradNlIndex[k]).internalData(), ((ShortList)tFpGradFpIndex[k]).internalData(), ((FloatList)tFpPx[k]).size());
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
                double tErrEng = rEng - (aData.mEng.get(i) - mNormMuEng)/mNormSigmaEng;
                rLoss += tErrEng*tErrEng;
                // force error
                double tLossForce = 0.0;
                if (rGrad != null) {
                    mLossGradForceXBuf.clear(); mLossGradForceXBuf.addZeros(tAtomNum);
                    mLossGradForceYBuf.clear(); mLossGradForceYBuf.addZeros(tAtomNum);
                    mLossGradForceZBuf.clear(); mLossGradForceZBuf.addZeros(tAtomNum);
                }
                double tForceLossMul = mForceWeight / (tAtomNum*3);
                Vector tForceX = aData.mForceX.get(i), tForceY = aData.mForceY.get(i), tForceZ = aData.mForceZ.get(i);
                for (int k = 0; k < tAtomNum; ++k) {
                    double tErrForceX = mForceXBuf.get(k) - tForceX.get(k)/mNormSigmaEng;
                    double tErrForceY = mForceYBuf.get(k) - tForceY.get(k)/mNormSigmaEng;
                    double tErrForceZ = mForceZBuf.get(k) - tForceZ.get(k)/mNormSigmaEng;
                    if (rGrad != null) {
                        mLossGradForceXBuf.set(k, 2.0*tForceLossMul*tErrForceX);
                        mLossGradForceYBuf.set(k, 2.0*tForceLossMul*tErrForceY);
                        mLossGradForceZBuf.set(k, 2.0*tForceLossMul*tErrForceZ);
                    }
                    tLossForce += (tErrForceX*tErrForceX + tErrForceY*tErrForceY + tErrForceZ*tErrForceZ);
                }
                rLoss += tForceLossMul * tLossForce;
                /// backward
                if (rGrad==null) continue;
                double tLossGradEng = 2.0 * tErrEng / tAtomNum;
                for (int k = 0; k < tAtomNum; ++k) {
                    // energy loss grad
                    int tType = tAtomType.get(k);
                    Vector tSubFp = tFp[k];
                    Vector tFpBuf = mFpBuf[tType-1];
                    Vector tNormMu = mNormMu[tType-1];
                    Vector tNormSigma = mNormSigma[tType-1];
                    tFpBuf.fill(j -> (tSubFp.get(j) - tNormMu.get(j)) / tNormSigma.get(j));
                    int tShiftPara = paraShift(tType);
                    NN_TIMER.from();
                    mNN[tType-1].backward(tLossGradEng, tFpBuf, rGrad.subVec(tShiftPara, tShiftPara+mParaSizes[tType-1]),
                                          mHiddenOutputsBuf.get(tType-1).get(k), mHiddenGradsBuf.get(tType-1).get(k));
                    NN_TIMER.to();
                    // force loss grad
                    Vector tLossGradFp = mLossGradFp[tType-1];
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
                    tLossGradFp.fill(0.0);
                    INDEX2_TIMER.from();
                    if (tFullCache) {
                        backwardForceIndexFMA_(tLossGradFp.internalData(), mLossGradForceNlXBuf.internalData(), mLossGradForceNlYBuf.internalData(), mLossGradForceNlZBuf.internalData(),
                                               ((DoubleList)tFpPx[k]).internalData(), ((DoubleList)tFpPy[k]).internalData(), ((DoubleList)tFpPz[k]).internalData(), tNormSigma.internalData(),
                                               ((IntList)tFpGradNlIndex[k]).internalData(), ((IntList)tFpGradFpIndex[k]).internalData(), ((DoubleList)tFpPx[k]).size());
                    } else {
                        backwardForceIndexFMA_(tLossGradFp.internalData(), mLossGradForceNlXBuf.internalData(), mLossGradForceNlYBuf.internalData(), mLossGradForceNlZBuf.internalData(),
                                               ((FloatList)tFpPx[k]).internalData(), ((FloatList)tFpPy[k]).internalData(), ((FloatList)tFpPz[k]).internalData(), tNormSigma.internalData(),
                                               ((ShortList)tFpGradNlIndex[k]).internalData(), ((ShortList)tFpGradFpIndex[k]).internalData(), ((FloatList)tFpPx[k]).size());
                    }
                    INDEX2_TIMER.to();
                    NN_TIMER.from();
                    mNN[tType-1].gradBackward(tLossGradFp, tFpBuf, rGrad.subVec(tShiftPara, tShiftPara+mParaSizes[tType-1]),
                                              mHiddenOutputsBuf.get(tType-1).get(k), mHiddenGradsBuf.get(tType-1).get(k),
                                              mHiddenGrads2Buf.get(tType-1).get(k), mHiddenGrads3Buf.get(tType-1).get(k), mHiddenGradGradsBuf.get(tType-1).get(k));
                    NN_TIMER.to();
                }
            }
        }
        if (rGrad!=null) rGrad.div2this(aData.mSize);
        return rLoss / aData.mSize;
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
            mNN[i].save(rNN);
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
}
