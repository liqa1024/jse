package jsex.nnap;

import jse.atom.*;
import jse.clib.*;
import jse.code.*;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.vector.*;
import jsex.nnap.basis.*;
import jsex.nnap.model.FeedForward;
import jsex.nnap.model.Model;
import jsex.nnap.model.TorchModel;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.util.*;

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
    
    public final static int VERSION = 3;
    
    static {
        // 依赖 nnapbasis
        Basis.InitHelper.init();
        // 这里不直接依赖 LmpPlugin
    }
    
    @SuppressWarnings("unchecked")
    private @Nullable SingleNNAP initSingleNNAPFrom(int aType, Map<String, ?> aModelInfo) throws TorchException {
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
        
        Model[] aModel = new Model[mThreadNumber];
        Object tModelObj = aModelInfo.get("torch");
        if (tModelObj != null) {
            mIsTorch = true;
            for (int i = 0; i < mThreadNumber; ++i) {
                //noinspection resource
                aModel[i] = new TorchModel(tModelObj.toString());
            }
            return new SingleNNAP(aRefEng, aNormMu, aNormSigma, aNormMuEng, aNormSigmaEng, aBasis, aModel);
        }
        Map<String, ?> tModel = (Map<String, ?>)aModelInfo.get("model");
        Object tModelType = tModel.get("type");
        if (tModelType == null) {
            tModelType = "FFNN";
        }
        if (tModelType.toString().equals("FFNN")) {
            for (int i = 0; i < mThreadNumber; ++i) {
                aModel[i] = FeedForward.load(tModel);
            }
        } else {
            throw new IllegalArgumentException("Unsupported model type: " + tBasisType);
        }
        return new SingleNNAP(aRefEng, aNormMu, aNormSigma, aNormMuEng, aNormSigmaEng, aBasis, aModel);
    }
    @SuppressWarnings("unchecked")
    private @Nullable SingleNNAP postInitSingleNNAPFrom(int aType, Map<String, ?> aModelInfo) throws TorchException {
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
        Model[] aModel = model(tMirrorType).mModel;
        return new SingleNNAP(aRefEng, aNormMu, aNormSigma, aNormMuEng, aNormSigmaEng, aBasis, aModel);
    }
    
    @SuppressWarnings("SameParameterValue")
    public class SingleNNAP {
        private final Model[] mModel;
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
        public Model model() {return model(0);}
        public Model model(int aThreadID) {return mModel[aThreadID];}
        
        /// 现在使用全局的缓存实现，可以进一步减少内存池的调用操作
        private final NNAPCachePointers mCachePtrs;
        
        private DoubleCPointer bufFp(int aThreadID) {return mCachePtrs.mFp[aThreadID];}
        private GrowableDoubleCPointer bufFpPx(int aThreadID) {return mCachePtrs.mFpPx[aThreadID];}
        private GrowableDoubleCPointer bufFpPy(int aThreadID) {return mCachePtrs.mFpPy[aThreadID];}
        private GrowableDoubleCPointer bufFpPz(int aThreadID) {return mCachePtrs.mFpPz[aThreadID];}
        private GrowableDoubleCPointer bufNlDx(int aThreadID) {return mCachePtrs.mNlDx[aThreadID];}
        private GrowableDoubleCPointer bufNlDy(int aThreadID) {return mCachePtrs.mNlDy[aThreadID];}
        private GrowableDoubleCPointer bufNlDz(int aThreadID) {return mCachePtrs.mNlDz[aThreadID];}
        private GrowableIntCPointer bufNlType(int aThreadID) {return mCachePtrs.mNlType[aThreadID];}
        private GrowableIntCPointer bufNlIdx(int aThreadID) {return mCachePtrs.mNlIdx[aThreadID];}
        
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
        
        private SingleNNAP(double aRefEng, IVector aNormMu, IVector aNormSigma, double aNormMuEng, double aNormSigmaEng, Basis[] aBasis, Model[] aModel) throws TorchException {
            mRefEng = aRefEng;
            mNormMu = aNormMu;
            mNormSigma = aNormSigma;
            mNormMuEng = aNormMuEng;
            mNormSigmaEng = aNormSigmaEng;
            mBasis = aBasis;
            mModel = aModel;
            
            mCachePtrs = new NNAPCachePointers(this, mThreadNumber, mBasis[0].size());
            mForceX = new DoubleList[mThreadNumber];
            mForceY = new DoubleList[mThreadNumber];
            mForceZ = new DoubleList[mThreadNumber];
            mNlDx = new DoubleList[mThreadNumber];
            mNlDy = new DoubleList[mThreadNumber];
            mNlDz = new DoubleList[mThreadNumber];
            mNlType = new IntList[mThreadNumber];
            mNlIdx = new IntList[mThreadNumber];
            for (int i = 0; i < mThreadNumber; ++i) {
                mForceX[i] = new DoubleList(16);
                mForceY[i] = new DoubleList(16);
                mForceZ[i] = new DoubleList(16);
                mNlDx[i] = new DoubleList(16);
                mNlDy[i] = new DoubleList(16);
                mNlDz[i] = new DoubleList(16);
                mNlType[i] = new IntList(16);
                mNlIdx[i] = new IntList(16);
            }
        }
        
        /// 统一缓存的近邻列表相关变量，用于快速和 c 内存交互
        private final DoubleList[] mNlDx, mNlDy, mNlDz;
        private final IntList[] mNlType, mNlIdx;
        
        int buildNL(int aThreadID, IDxyzTypeIdxIterable aNL, GrowableDoubleCPointer aNlDx, GrowableDoubleCPointer aNlDy, GrowableDoubleCPointer aNlDz, GrowableIntCPointer aNlType, GrowableIntCPointer aNlIdx) {
            final int tTypeNum = atomTypeNumber();
            final DoubleList tNlDx = mNlDx[aThreadID], tNlDy = mNlDy[aThreadID], tNlDz = mNlDz[aThreadID];
            final IntList tNlType = mNlType[aThreadID], tNlIdx = mNlIdx[aThreadID];
            // 缓存情况需要先清空这些
            tNlDx.clear(); tNlDy.clear(); tNlDz.clear();
            tNlType.clear(); tNlIdx.clear();
            aNL.forEachDxyzTypeIdx(basis(aThreadID).rcut(), (dx, dy, dz, type, idx) -> {
                // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查
                if (type > tTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+tTypeNum+")");
                // 简单缓存近邻列表
                tNlDx.add(dx); tNlDy.add(dy); tNlDz.add(dz);
                tNlType.add(type); tNlIdx.add(idx);
            });
            final int tNN = tNlDx.size();
            aNlDx.ensureCapacity(tNN);
            aNlDy.ensureCapacity(tNN);
            aNlDz.ensureCapacity(tNN);
            aNlType.ensureCapacity(tNN);
            aNlIdx.ensureCapacity(tNN);
            aNlDx.fill(tNlDx.internalData(), tNlDx.internalDataSize());
            aNlDy.fill(tNlDy.internalData(), tNlDy.internalDataSize());
            aNlDz.fill(tNlDz.internalData(), tNlDz.internalDataSize());
            aNlType.fill(tNlType.internalData(), tNlType.internalDataSize());
            aNlIdx.fill(tNlIdx.internalData(), tNlIdx.internalDataSize());
            return tNN;
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
    public NNAP(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws TorchException {
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
    public NNAP(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws TorchException, IOException {
        this(aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath), aThreadNumber);
    }
    public NNAP(Map<?, ?> aModelInfo) throws TorchException {this(aModelInfo, 1);}
    public NNAP(String aModelPath) throws TorchException, IOException {this(aModelPath, 1);}
    
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        for (SingleNNAP tModel : mModels) {
            tModel.mCachePtrs.dispose();
            for (int i = 0; i < mThreadNumber; ++i) {
                tModel.basis(i).shutdown();
            }
            for (int i = 0; i < mThreadNumber; ++i) {
                tModel.mModel[i].shutdown();
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
            Model tModel_ = tModel.model(threadID);
            GrowableDoubleCPointer tNlDx = tModel.bufNlDx(threadID);
            GrowableDoubleCPointer tNlDy = tModel.bufNlDy(threadID);
            GrowableDoubleCPointer tNlDz = tModel.bufNlDz(threadID);
            GrowableIntCPointer tNlType = tModel.bufNlType(threadID);
            GrowableIntCPointer tNlIdx = tModel.bufNlIdx(threadID);
            int tNN = tModel.buildNL(threadID, nl, tNlDx, tNlDy, tNlDz, tNlType, tNlIdx);
            DoubleCPointer tFp = tModel.bufFp(threadID);
            tBasis.eval_(tNlDx, tNlDy, tNlDz, tNlType, tNN, tFp);
            tModel.normBasis(tFp);
            double tPred = tModel_.forward(tFp);
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
        try {
            aNeighborListGetter.forEachNLWithException(threadID -> {
                if (mIsTorch && tThreadNum>1) TorchModel.setSingleThread();
            }, null, (threadID, cIdx, cType, nl) -> {
                SingleNNAP tModel = model(cType);
                Basis tBasis = tModel.basis(threadID);
                Model tModel_ = tModel.model(threadID);
                GrowableDoubleCPointer tNlDx = tModel.bufNlDx(threadID);
                GrowableDoubleCPointer tNlDy = tModel.bufNlDy(threadID);
                GrowableDoubleCPointer tNlDz = tModel.bufNlDz(threadID);
                GrowableIntCPointer tNlType = tModel.bufNlType(threadID);
                GrowableIntCPointer tNlIdx = tModel.bufNlIdx(threadID);
                int tNN = tModel.buildNL(threadID, nl, tNlDx, tNlDy, tNlDz, tNlType, tNlIdx);
                int tBasisSize = tBasis.size();
                DoubleCPointer tFp = tModel.bufFp(threadID);
                GrowableDoubleCPointer tFpPx = tModel.bufFpPx(threadID); tFpPx.ensureCapacity(tBasisSize*tNN);
                GrowableDoubleCPointer tFpPy = tModel.bufFpPy(threadID); tFpPy.ensureCapacity(tBasisSize*tNN);
                GrowableDoubleCPointer tFpPz = tModel.bufFpPz(threadID); tFpPz.ensureCapacity(tBasisSize*tNN);
                tBasis.evalPartial_(tNlDx, tNlDy, tNlDz, tNlType, tNN, tFp, tBasisSize, 0, tFpPx, tFpPy, tFpPz);
                tModel.normBasis(tFp);
                double tPred = tModel_.backward(tFp, tFpGrad);
                
                if (rEnergyAccumulator != null) {
                    tPred = tModel.denormEng(tPred);
                    tPred += tModel.mRefEng;
                    rEnergyAccumulator.add(threadID, cIdx, -1, tPred);
                }
                tModel.normBasisPartial(tFpGrad);
                tModel.denormEngPartial(tFpGrad);
                DoubleList tForceX = tModel.bufForceX(threadID, tNN);
                DoubleList tForceY = tModel.bufForceY(threadID, tNN);
                DoubleList tForceZ = tModel.bufForceZ(threadID, tNN);
                Basis.forceDot(tFpGrad, tFpPx, tFpPy, tFpPz, tForceX, tForceY, tForceZ, tNN);
                // 累加交叉项到近邻
                for (int j = 0; j < tNN; ++j) {
                    double dx = tNlDx.get(j);
                    double dy = tNlDy.get(j);
                    double dz = tNlDz.get(j);
                    int idx = tNlIdx.get(j);
                    // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查
                    double fx = -tForceX.get(j);
                    double fy = -tForceY.get(j);
                    double fz = -tForceZ.get(j);
                    if (rForceAccumulator != null) {
                        rForceAccumulator.add(threadID, cIdx, idx, fx, fy, fz);
                    }
                    if (rVirialAccumulator != null) {
                        rVirialAccumulator.add(threadID, cIdx, -1, dx*fx, dy*fy, dz*fz, dx*fy, dx*fz, dy*fz);
                    }
                }
            });
        } catch (Exception e) {
            throw e;
        }
    }
}
