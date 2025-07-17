package jsex.nnap;

import jep.python.PyCallable;
import jep.python.PyObject;
import jse.atom.IAtomData;
import jse.atom.AtomicParameterCalculator;
import jse.atom.IHasSymbol;
import jse.cache.IntMatrixCache;
import jse.cache.IntVectorCache;
import jse.cache.MatrixCache;
import jse.cache.VectorCache;
import jse.clib.Torch;
import jse.code.IO;
import jse.code.SP;
import jse.code.UT;
import jse.code.collection.AbstractCollections;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.code.io.ISavable;
import jse.math.MathEX;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowIntMatrix;
import jse.math.matrix.RowMatrix;
import jse.math.vector.*;
import jse.math.vector.Vector;
import jse.parallel.IAutoShutdown;
import jsex.nnap.basis.Basis;
import jsex.nnap.basis.Mirror;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;
import org.jetbrains.annotations.VisibleForTesting;

import java.io.IOException;
import java.util.*;
import java.util.function.IntUnaryOperator;

/**
 * jse 实现的 nnap 训练器，这里简单起见直接通过 python 训练。
 * 这里直接通过全局的 python 解释器执行
 * @deprecated 现在使用纯 jse 实现 {@link TrainerNative}，效率更高
 * @author liqa
 */
@Deprecated
public class TrainerTorch implements IHasSymbol, IAutoShutdown, ISavable {
    protected final static String DEFAULT_UNITS = "metal";
    protected final static int[] DEFAULT_HIDDEN_DIMS = {32, 32}; // 现在统一默认为 32, 32
    protected final static double DEFAULT_FORCE_WEIGHT = 0.1;
    protected final static double DEFAULT_STRESS_WEIGHT = 1.0;
    protected final static double DEFAULT_L2_LOSS_WEIGHT = 0.001;
    protected final static boolean DEFAULT_TRAIN_IN_FLOAT = true;
    protected final static boolean DEFAULT_CLEAR_DATA_ON_TRAINING = true;
    protected final static PyObject TORCH;
    protected final static PyCallable TRAINER;
    
    /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
    public final static class InitHelper {
        private static volatile boolean INITIALIZED = false;
        
        public static boolean initialized() {return INITIALIZED;}
        @SuppressWarnings({"ResultOfMethodCallIgnored", "UnnecessaryCallToStringValueOf"})
        public static void init() {
            // 手动调用此值来强制初始化
            if (!INITIALIZED) String.valueOf(TRAINER);
        }
    }
    
    public static class Conf {
        /** pytorch 训练时采用的线程数 */
        public static int THREAD_NUMBER = 4;
        /** 全局初始化脚本，默认为设置串行；经过测试并行训练并不会明显更快 */
        public static @Nullable String INIT_SCRIPT = null;
    }
    static {
        InitHelper.INITIALIZED = true;
        // 依赖 Python
        SP.Python.InitHelper.init();
        // 简单直接依赖 Torch
        Torch.InitHelper.init();
        // 拷贝依赖的 python 脚本
        String tPath = SP.Python.JEP_LIB_DIR+"jsexpy/nnap_trainer.py";
        if (!IO.exists(tPath)) {
            try {
                IO.copy(IO.getResource("jsexpy/nnap_trainer.py"), tPath);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        // torch 初始化设置线程
        SP.Python.exec("import torch");
        TORCH = SP.Python.getClass("torch");
        //noinspection ConcatenationWithEmptyString
        SP.Python.exec(Conf.INIT_SCRIPT!=null ? Conf.INIT_SCRIPT : ""+
            "try:\n" +
            "    torch.set_num_threads("+Conf.THREAD_NUMBER+")\n" +
            "    torch.set_num_interop_threads("+Conf.THREAD_NUMBER+")\n" +
            "except RuntimeError:\n" +
            "    pass"
        );
        // 初始化 nnap trainer
        SP.Python.exec("from jsexpy import nnap_trainer");
        TRAINER = SP.Python.getAs(PyCallable.class, "nnap_trainer.Trainer");
    }
    
    private boolean mDead = false;
    public boolean isShutdown() {return mDead;}
    @Override public void shutdown() {
        if (!mDead) {
            mDead = true;
            if (mTrainer != null) {
                mTrainer.close();
                mTrainer = null;
            }
        }
    }
    
    protected PyObject mTrainer = TRAINER.callAs(PyObject.class, DEFAULT_FORCE_WEIGHT, DEFAULT_STRESS_WEIGHT, DEFAULT_L2_LOSS_WEIGHT, DEFAULT_TRAIN_IN_FLOAT);
    public TrainerTorch setForceWeight(double aWeight) {mTrainer.setAttr("force_weight", aWeight); return this;}
    public TrainerTorch setStressWeight(double aWeight) {mTrainer.setAttr("stress_weight", aWeight); return this;}
    public TrainerTorch setL2LossWeight(double aWeight) {mTrainer.setAttr("l2_loss_weight", aWeight); return this;}
    public TrainerTorch setTrainInFloat(boolean aFlag) {mTrainer.setAttr("train_in_float", aFlag); return this;}
    
    protected String mUnits = DEFAULT_UNITS;
    public TrainerTorch setUnits(String aUnits) {mUnits = aUnits; return this;}
    protected boolean mClearDataOnTraining = DEFAULT_CLEAR_DATA_ON_TRAINING;
    public TrainerTorch setClearDataOnTraining(boolean aFlag) {mClearDataOnTraining = aFlag; return this;}
    
    
    /** 所有训练相关的数据放在这里，同来减少训练集和测试集使用时的重复代码 */
    protected class DataSet {
        public final int mAtomTypeNumber;
        /** 按照种类排序，内部是可扩展的具体数据；现在使用这种 DoubleList 展开的形式存 */
        public final DoubleList[] mFp;
        /** mFp 的实际值（按行排列），这个只是缓存结果 */
        public final RowMatrix[] mFpMat;
        /** 每个 fp 对应的能量的索引，按照种类排序 */
        public final IntList[] mEngIndices;
        /** 按照种类排序，内部是每个原子的所有基组偏导值，每个基组偏导值按行排列，和交叉项以及 xyz 共同组成一个矩阵；为了减少内存占用现在统一直接存 torch tensor */
        public final List<List<PyObject>> mFpPartial;
        /** 每个 fp partial 对应的力的索引，按照种类排序；为了减少内存占用现在统一直接存 torch tensor */
        public final List<List<PyObject>> mForceIndices;
        /** 每个 fp partial 对应的应力的索引，按照种类排序；为了减少内存占用现在统一直接存 torch tensor */
        public final List<List<PyObject>> mStressIndices;
        /** 每个 fp partial 对应的用于计算应力的 dxyz 值，按照种类排序；为了减少内存占用现在统一直接存 torch tensor */
        public final List<List<PyObject>> mStressDxyz;
        /** 每个原子的近邻原子数列表 */
        public final IntList[] mNN;
        /** 每个原子数据结构对应的能量值 */
        public final DoubleList mEng = new DoubleList(64);
        /** 这里的力数值直接展开成单个向量，通过 mForceIndices 来获取对应的索引 */
        public final DoubleList mForce = new DoubleList(64);
        /** 这里的应力数值直接展开成单个向量，通过 mStressIndices 来获取对应的索引 */
        public final DoubleList mStress = new DoubleList(64);
        /** 每个原子数据结构对应的原子数 */
        public final DoubleList mAtomNum = new DoubleList(64);
        /** 每个原子数据结构对应的体积大小，目前主要用来计算应力 */
        public final DoubleList mVolume = new DoubleList(64);
        
        protected DataSet(int aAtomTypeNum) {
            mAtomTypeNumber = aAtomTypeNum;
            mFp = new DoubleList[aAtomTypeNum];
            mEngIndices = new IntList[aAtomTypeNum];
            mFpPartial = new ArrayList<>(aAtomTypeNum);
            mForceIndices = new ArrayList<>(aAtomTypeNum);
            mStressIndices = new ArrayList<>(aAtomTypeNum);
            mStressDxyz = new ArrayList<>(aAtomTypeNum);
            mNN = new IntList[aAtomTypeNum];
            for (int i = 0; i < aAtomTypeNum; ++i) {
                mFp[i] = new DoubleList(64);
                mEngIndices[i] = new IntList(64);
                mFpPartial.add(new ArrayList<>(64));
                mForceIndices.add(new ArrayList<>(64));
                mStressIndices.add(new ArrayList<>(64));
                mStressDxyz.add(new ArrayList<>(64));
                mNN[i] = new IntList(64);
            }
            mFpMat = new RowMatrix[aAtomTypeNum];
        }
        protected DataSet() {this(mSymbols.length);}
        
        /** 初始化矩阵数据 */
        @ApiStatus.Internal
        protected void initFpMat_() {
            for (int i = 0; i < mFp.length; ++i) {
                final int tColNum = mBasis[i].size();
                final int tRowNum = mFp[i].size() / tColNum;
                mFpMat[i] = new RowMatrix(tRowNum, tColNum, mFp[i].internalData());
            }
        }
        @ApiStatus.Internal
        protected void putData2Py_(String aPyDataSetName, boolean aClearData) {
            try (PyObject tDataSet = mTrainer.getAttr(aPyDataSetName, PyObject.class)) {
                try (PyCallable fInitEngPart = tDataSet.getAttr("init_eng_part", PyCallable.class)) {
                    fInitEngPart.call(this);
                }
                if (aClearData) {
                    for (int i = 0; i < mAtomTypeNumber; ++i) {
                        mFp[i].clear();
                        mEngIndices[i].clear();
                        mFp[i].clear();
                        mFpMat[i] = null;
                    }
                    mEng.clear();
                    mAtomNum.clear();
                }
                // 训练力和应力需要的额外数据
                if (!mHasForce && !mHasStress) return;
                try (PyCallable fInitForcePart = tDataSet.getAttr("init_force_part1", PyCallable.class)) {
                    fInitForcePart.call(this);
                }
                if (aClearData) {
                    if (mHasForce) {mForce.clear();}
                    if (mHasStress) {mStress.clear(); mVolume.clear();}
                }
                for (int i = 0; i < mAtomTypeNumber; ++i) {
                    // 先根据近邻数排序 FpPartial 和 Indices，拆分成两份来减少内存占用
                    final List<PyObject> tSubFpPartial = mFpPartial.get(i);
                    final List<PyObject> tSubForceIndices = mHasForce ? mForceIndices.get(i) : null;
                    final List<PyObject> tSubStressIndices = mHasStress ? mStressIndices.get(i) : null;
                    final List<PyObject> tSubStressDxyz = mHasStress ? mStressDxyz.get(i) : null;
                    IntVector tSubNN = mNN[i].asVec();
                    IntVector tSortIndices = Vectors.range(tSubNN.size());
                    tSubNN.operation().biSort((ii, jj) -> {
                        tSortIndices.swap(ii, jj);
                        Collections.swap(tSubFpPartial, ii, jj);
                        if (mHasForce) {
                            assert tSubForceIndices != null;
                            Collections.swap(tSubForceIndices, ii, jj);
                        }
                        if (mHasStress) {
                            assert tSubStressIndices != null;
                            Collections.swap(tSubStressIndices, ii, jj);
                            Collections.swap(tSubStressDxyz, ii, jj);
                        }
                    });
                    try (PyCallable fInitForcePart = tDataSet.getAttr("init_force_part2", PyCallable.class)) {
                        fInitForcePart.call(this, i, tSortIndices.numpy(), calBestSplit_(tSubNN));
                    }
                    // 遍历过程中清空数据，进一步减少转换过程的内存占用峰值
                    if (aClearData) {
                        tSubFpPartial.forEach(PyObject::close);
                        tSubFpPartial.clear();
                        mNN[i].clear();
                        if (mHasForce) {
                            assert tSubForceIndices != null;
                            tSubForceIndices.forEach(PyObject::close);
                            tSubForceIndices.clear();
                        }
                        if (mHasStress) {
                            assert tSubStressIndices != null;
                            tSubStressIndices.forEach(PyObject::close);
                            tSubStressIndices.clear();
                            tSubStressDxyz.forEach(PyObject::close);
                            tSubStressDxyz.clear();
                        }
                    }
                }
                try (PyCallable fInitForcePart = tDataSet.getAttr("init_force_part3", PyCallable.class)) {
                    fInitForcePart.call(this);
                }
            }
        }
    }
    
    protected final String[] mSymbols;
    protected final IVector mRefEngs;
    protected final Basis[] mBasis;
    protected final Vector[] mNormMu, mNormSigma;
    protected double mNormMuEng = 0.0, mNormSigmaEng = 0.0;
    protected final DataSet mTrainData;
    protected final DataSet mTestData;
    protected boolean mHasData = false;
    protected boolean mHasForce = false;
    protected boolean mHasStress = false;
    protected boolean mHasTest = false;
    protected boolean mModelInited = false;
    protected final Map<String, ?> mModelSetting;
    protected final DoubleList mTrainLoss = new DoubleList(64);
    protected final DoubleList mTestLoss = new DoubleList(64);
    
    public TrainerTorch(String[] aSymbols, IVector aRefEngs, Basis[] aBasis, Map<String, ?> aModelSetting) {
        if (aSymbols.length != aRefEngs.size()) throw new IllegalArgumentException("Symbols length does not match reference energies length.");
        if (aSymbols.length != aBasis.length) throw new IllegalArgumentException("Symbols length does not match reference basis length.");
        mSymbols = aSymbols;
        mRefEngs = aRefEngs;
        mBasis = aBasis;
        mNormMu = new Vector[mSymbols.length];
        mNormSigma = new Vector[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            int tSize = mBasis[i].size();
            mNormMu[i] = Vector.zeros(tSize);
            mNormSigma[i] = Vector.zeros(tSize);
        }
        mTrainData = new DataSet();
        mTestData = new DataSet();
        mModelSetting = aModelSetting;
        // 简单遍历 basis 处理 mirror 的情况
        for (int i = 0; i < mSymbols.length; ++i) if (mBasis[i] instanceof Mirror) {
            Mirror tBasis = (Mirror)mBasis[i];
            Basis tMirrorBasis = tBasis.mirrorBasis();
            int tMirrorType = tBasis.mirrorType();
            if ((mBasis[tMirrorType-1]!=tMirrorBasis) || (tBasis.thisType()!=(i+1))) {
                throw new IllegalArgumentException("Mirror Basis mismatch for type: "+(i+1));
            }
            double oRefEng = mRefEngs.get(i);
            double tRefEng = mRefEngs.get(tMirrorType-1);
            if (!Double.isNaN(oRefEng) && !MathEX.Code.numericEqual(oRefEng, tRefEng)) {
                UT.Code.warning("RefEng of mirror mismatch for type: "+(i+1)+", overwrite with mirror values automatically");
            }
            mRefEngs.set(i, tRefEng);
        }
        mFp = new Vector[mSymbols.length];
        mFpPx = new Vector[mSymbols.length];
        mFpPy = new Vector[mSymbols.length];
        mFpPz = new Vector[mSymbols.length];
        mFpPxCross = new DoubleList[mSymbols.length];
        mFpPyCross = new DoubleList[mSymbols.length];
        mFpPzCross = new DoubleList[mSymbols.length];
        for (int i = 0; i < mSymbols.length; ++i) {
            int tBasisSize = mBasis[i].size();
            mFp[i] = Vectors.zeros(tBasisSize);
            mFpPx[i] = Vectors.zeros(tBasisSize);
            mFpPy[i] = Vectors.zeros(tBasisSize);
            mFpPz[i] = Vectors.zeros(tBasisSize);
            mFpPxCross[i] = new DoubleList(1024);
            mFpPyCross[i] = new DoubleList(1024);
            mFpPzCross[i] = new DoubleList(1024);
        }
        
    }
    public TrainerTorch(String[] aSymbols, IVector aRefEngs, Basis aBasis, Map<String, ?> aModelSetting) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aModelSetting);}
    public TrainerTorch(String[] aSymbols, double[] aRefEngs, Basis[] aBasis, Map<String, ?> aModelSetting) {this(aSymbols, Vectors.from(aRefEngs), aBasis, aModelSetting);}
    public TrainerTorch(String[] aSymbols, double[] aRefEngs, Basis aBasis, Map<String, ?> aModelSetting) {this(aSymbols, aRefEngs, repeatBasis_(aBasis, aSymbols.length), aModelSetting);}
    private static Basis[] repeatBasis_(Basis aBasis, int aLen) {
        Basis[] rOut = new Basis[aLen];
        Arrays.fill(rOut, aBasis);
        return rOut;
    }
    
    @Override public int atomTypeNumber() {return mSymbols.length;}
    public PyObject model(int aType) {
        try (PyCallable fModel = mTrainer.getAttr("model_at", PyCallable.class)) {
            return fModel.callAs(PyObject.class, aType-1);
        }
    }
    @SuppressWarnings("unchecked")
    public @Unmodifiable List<PyObject> models() {
        try (PyCallable fModels = mTrainer.getAttr("models", PyCallable.class)) {
            return (List<PyObject>)fModels.callAs(List.class);
        }
    }
    public Basis basis(int aType) {return mBasis[aType-1];}
    public @Unmodifiable List<Basis> basis() {return AbstractCollections.from(mBasis);}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
    public String units() {return mUnits;}
    
    protected void initModel() {
        @Nullable List<?> tHiddenDims = (List<?>)UT.Code.get(mModelSetting, "hidden_dims", "nnarch");
        if (tHiddenDims == null) {
            tHiddenDims = NewCollections.from(mSymbols.length, i -> DEFAULT_HIDDEN_DIMS);
        } else
        if (tHiddenDims.get(0) instanceof Integer) {
            final List<?> tSubDims = tHiddenDims;
            tHiddenDims = NewCollections.from(mSymbols.length, i -> tSubDims);
        }
        Map<Integer, Integer> tMirrorMap = new HashMap<>();
        for (int i = 0; i < mSymbols.length; ++i) if (mBasis[i] instanceof Mirror) {
            int tMirror = ((Mirror)mBasis[i]).mirrorType()-1;
            tMirrorMap.put(i, tMirror);
            Object oSubDims = tHiddenDims.get(i);
            Object tSubDims = tHiddenDims.get(tMirror);
            if (oSubDims!=null && !oSubDims.equals(tSubDims)) {
                UT.Code.warning("hidden_dims of mirror mismatch for type: "+(i+1)+", overwrite with mirror values automatically");
            }
        }
        try (PyCallable fInitModel = mTrainer.getAttr("init_model", PyCallable.class)) {
            fInitModel.call(NewCollections.map(mBasis, Basis::size), tHiddenDims, tMirrorMap, mSymbols.length);
        }
    }
    protected void initNormBasis() {
        for (int i = 0; i < mSymbols.length; ++i) {
            mNormMu[i].fill(0.0);
            mNormSigma[i].fill(0.0);
        }
        IVector tDiv = VectorCache.getZeros(mSymbols.length);
        for (int i = 0; i < mSymbols.length; ++i) {
            // 这里需要考虑 mirror 的情况，对于 mirror 的同时和对应的数据一起公用归一化向量
            int j = i;
            if (mBasis[i] instanceof Mirror) {
                j = ((Mirror)mBasis[i]).mirrorType()-1;
            }
            for (IVector tRow : mTrainData.mFpMat[i].rows()) {
                mNormMu[j].plus2this(tRow);
                mNormSigma[j].operation().operate2this(tRow, (lhs, rhs) -> lhs + rhs*rhs);
            }
            tDiv.add(j, mTrainData.mFpMat[i].rowNumber());
        }
        for (int i = 0; i < mSymbols.length; ++i) if (!(mBasis[i] instanceof Mirror)) {
            mNormMu[i].div2this(tDiv.get(i));
            mNormSigma[i].div2this(tDiv.get(i));
            mNormSigma[i].operation().operate2this(mNormMu[i], (lhs, rhs) -> lhs - rhs*rhs);
            mNormSigma[i].operation().map2this(MathEX.Fast::sqrt);
        }
        VectorCache.returnVec(tDiv);
        for (int i = 0; i < mSymbols.length; ++i) if (mBasis[i] instanceof Mirror) {
            int tMirrorIdx = ((Mirror)mBasis[i]).mirrorType()-1;
            mNormMu[i] = mNormMu[tMirrorIdx];
            mNormSigma[i] = mNormSigma[tMirrorIdx];
        }
    }
    protected void initNormEng() {
        // 这里采用中位数和上下四分位数来归一化能量
        Vector tSortedEng = mTrainData.mEng.copy2vec();
        tSortedEng.div2this(mTrainData.mAtomNum.asVec());
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
    @ApiStatus.Internal
    protected int calBestSplit_(IIntVector aSortedNN) {
        final int tSize = aSortedNN.size();
        final int tSizeMM = tSize-1;
        long tMinSizeOut = (long)aSortedNN.last()*tSize;
        int tBest = 0;
        for (int split = 1; split < tSizeMM; ++split) {
            long tSizeOut = (long)aSortedNN.get(split-1)*split + (long)aSortedNN.get(tSizeMM)*(tSize-split);
            if (tSizeOut < tMinSizeOut) {
                tBest = split;
                tMinSizeOut = tSizeOut;
            }
        }
        return tBest;
    }
    /** 开始训练模型，这里直接训练给定的步数 */
    public void train(int aEpochs, boolean aEarlyStop, boolean aPrintLog) {
        // 在这里初始化模型，可以避免构造函数中调用多态方法的一些歧义
        if (!mModelInited) {
            initModel();
            mModelInited = true;
        }
        if (aPrintLog) System.out.println("Init train data...");
        // 初始化矩阵数据
        mTrainData.initFpMat_();
        if (mHasTest) mTestData.initFpMat_();
        // 重新构建归一化参数
        initNormBasis();
        initNormEng();
        // 构造 torch 数据
        mTrainer.setAttr("has_force", mHasForce);
        mTrainer.setAttr("has_stress", mHasStress);
        try (PyCallable fInitNorm = mTrainer.getAttr("init_norm", PyCallable.class)) {
            fInitNorm.call(mNormMu, mNormSigma);
        }
        mTrainer.setAttr("norm_mu_eng", mNormMuEng);
        mTrainer.setAttr("norm_sigma_eng", mNormSigmaEng);
        mTrainData.putData2Py_("train_data", mClearDataOnTraining);
        if (mHasTest) mTestData.putData2Py_("test_data", mClearDataOnTraining);
        // 开始训练
        try (PyCallable fTrainStep = mTrainer.getAttr("train_step", PyCallable.class);
             PyCallable fTestLoss = mTrainer.getAttr("test_loss", PyCallable.class);
             PyCallable fSaveCheckpoint = mTrainer.getAttr("save_checkpoint", PyCallable.class);
             PyCallable fLoadCheckpoint = mTrainer.getAttr("load_checkpoint", PyCallable.class);
             PyCallable fCalLossDetail = mTrainer.getAttr("cal_loss_detail", PyCallable.class);
             PyCallable fCalMae = mTrainer.getAttr("cal_mae", PyCallable.class);) {
            if (aPrintLog) UT.Timer.progressBar("train", aEpochs);
            double tMinLoss = Double.POSITIVE_INFINITY;
            int tSelectEpoch = -1;
            for (int i = 0; i < aEpochs; ++i) {
                double tLoss = fTrainStep.callAs(Number.class).doubleValue();
                double oLoss = mTrainLoss.isEmpty() ? Double.NaN : mTrainLoss.last();
                mTrainLoss.add(tLoss);
                double tTestLoss = Double.NaN;
                if (mHasTest) {
                    tTestLoss = fTestLoss.callAs(Number.class).doubleValue();
                    mTestLoss.add(tTestLoss);
                    if (aEarlyStop && tTestLoss<tMinLoss) {
                        tSelectEpoch = i;
                        tMinLoss = tTestLoss;
                        fSaveCheckpoint.call();
                    }
                }
                if (!Double.isNaN(oLoss) && Math.abs(oLoss-tLoss)<(tLoss*1e-8)) {
                    if (aPrintLog) for (int j = i; j < aEpochs; ++j) {
                        UT.Timer.progressBar(mHasTest ? String.format("loss: %.4g | %.4g", tLoss, tTestLoss) : String.format("loss: %.4g", tLoss));
                    }
                    break;
                }
                if (aPrintLog) {
                    UT.Timer.progressBar(mHasTest ? String.format("loss: %.4g | %.4g", tLoss, tTestLoss) : String.format("loss: %.4g", tLoss));
                }
            }
            if (aEarlyStop && tSelectEpoch>=0) {
                fLoadCheckpoint.call();
                if (aPrintLog) System.out.printf("Model at epoch = %d selected, test loss = %.4g\n", tSelectEpoch, tMinLoss);
            }
            if (!aPrintLog) return;
            List<?> tLossDetail = fCalLossDetail.callAs(List.class);
            double tLossL2 = ((Number)tLossDetail.get(0)).doubleValue();
            double tLossE = ((Number)tLossDetail.get(1)).doubleValue();
            double tLossF = mHasForce ? ((Number)tLossDetail.get(2)).doubleValue() : 0.0;
            double tLossS = mHasStress ? ((Number)tLossDetail.get(3)).doubleValue() : 0.0;
            double tLossTot = tLossL2+tLossE+tLossF+tLossS;
            System.out.printf("Loss-L2: %.4g (%s)\n", tLossL2, IO.Text.percent(tLossL2/tLossTot));
            System.out.printf("Loss-E : %.4g (%s)\n", tLossE, IO.Text.percent(tLossE/tLossTot));
            if (mHasForce) {
                System.out.printf("Loss-F : %.4g (%s)\n", tLossF, IO.Text.percent(tLossF/tLossTot));
            }
            if (mHasStress) {
                System.out.printf("Loss-S : %.4g (%s)\n", tLossS, IO.Text.percent(tLossS/tLossTot));
            }
            List<?> tMAE = fCalMae.callAs(List.class, false);
            double tMAE_E = ((Number)tMAE.get(0)).doubleValue();
            double tMAE_F = mHasForce ? ((Number)tMAE.get(1)).doubleValue() : Double.NaN;
            double tMAE_S = mHasStress ? ((Number)tMAE.get(2)).doubleValue() : Double.NaN;
            if (!mHasTest) {
                System.out.printf("MAE-E: %.4g meV\n", tMAE_E*1000);
                if (mHasForce) {
                    System.out.printf("MAE-F: %.4g meV/A\n", tMAE_F*1000);
                }
                if (mHasStress) {
                    System.out.printf("MAE-S: %.4g meV/A^3\n", tMAE_S*1000);
                }
                return;
            }
            List<?> tTestMAE = fCalMae.callAs(List.class, true);
            double tTestMAE_E = ((Number)tTestMAE.get(0)).doubleValue();
            double tTestMAE_F = mHasForce ? ((Number)tTestMAE.get(1)).doubleValue() : Double.NaN;
            double tTestMAE_S = mHasStress ? ((Number)tTestMAE.get(2)).doubleValue() : Double.NaN;
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
    }
    public void train(int aEpochs, boolean aEarlyStop) {train(aEpochs, aEarlyStop, true);}
    public void train(int aEpochs) {train(aEpochs, true);}
    
    /// 现在全局缓存这些变量
    private final Vector[] mFp, mFpPx, mFpPy, mFpPz;
    private final DoubleList[] mFpPxCross, mFpPyCross, mFpPzCross;
    
    @ApiStatus.Internal
    protected void calRefEngFpAndAdd_(IAtomData aAtomData, double aEnergy, DataSet rData) {
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                Basis tBasis = basis(tType);
                // 这里依旧采用缓存的写法
                Vector tFp = VectorCache.getVec(tBasis.size());
                tBasis.eval(tAPC, i, tTypeMap, tFp);
                rData.mFp[tType-1].addAll(tFp);
                rData.mEngIndices[tType-1].add(rData.mEng.size());
                VectorCache.returnVec(tFp);
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
            }
        }
        // 这里后添加能量，这样 rData.mEng.size() 对应正确的索引
        rData.mEng.add(aEnergy);
    }
    @ApiStatus.Internal
    protected void calRefEngFpPartialAndAdd_(IAtomData aAtomData, double aEnergy, @Nullable IMatrix aForces, @Nullable IVector aStress, DataSet rData) {
        final boolean tTrainInFloat = mTrainer.getAttr("train_in_float", Boolean.class);
        IntUnaryOperator tTypeMap = typeMap(aAtomData);
        // 由于数据集不完整因此这里不去做归一化
        final int tAtomNum = aAtomData.atomNumber();
        try (final AtomicParameterCalculator tAPC = AtomicParameterCalculator.of(aAtomData)) {
            for (int i = 0; i < tAtomNum; ++i) {
                int tType = tTypeMap.applyAsInt(aAtomData.atom(i).type());
                Basis tBasis = basis(tType);
                final int tBasisSize = tBasis.size();
                Vector tFp = mFp[tType-1];
                Vector tFpPx = mFpPx[tType-1];
                Vector tFpPy = mFpPy[tType-1];
                Vector tFpPz = mFpPz[tType-1];
                DoubleList tFpPxCross = mFpPxCross[tType-1];
                DoubleList tFpPyCross = mFpPyCross[tType-1];
                DoubleList tFpPzCross = mFpPzCross[tType-1];
                tBasis.evalGrad(tAPC, i, tTypeMap, tFp, tFpPxCross, tFpPyCross, tFpPzCross);
                // 基组和索引
                rData.mFp[tType-1].addAll(tFp);
                rData.mEngIndices[tType-1].add(rData.mEng.size());
                // 计算相对能量值
                aEnergy -= mRefEngs.get(tType-1);
                // 基组偏导和索引
                final int tNN = tFpPxCross.size()/tBasisSize;
                // 为了减少后续优化过程中的近邻求和次数，因此这里还是使用旧的求力方法
                tFpPx.fill(0.0); tFpPy.fill(0.0); tFpPz.fill(0.0);
                for (int j = 0; j < tNN; ++j) {
                    tFpPx.minus2this(new ShiftVector(tBasisSize, tBasisSize*j, tFpPxCross.internalData()));
                    tFpPy.minus2this(new ShiftVector(tBasisSize, tBasisSize*j, tFpPyCross.internalData()));
                    tFpPz.minus2this(new ShiftVector(tBasisSize, tBasisSize*j, tFpPzCross.internalData()));
                }
                int tRowNum = tNN*3 + 3;
                final RowMatrix tFpPartial = MatrixCache.getMatRow(tRowNum, tBasis.size());
                tFpPartial.row(0).fill(tFpPx);
                tFpPartial.row(1).fill(tFpPy);
                tFpPartial.row(2).fill(tFpPz);
                final IntVector tForceIndices = mHasForce ? IntVectorCache.getVec(tRowNum) : null;
                final int tShiftF = mHasForce ? rData.mForce.size() : -1;
                if (mHasForce) {
                    tForceIndices.set(0, tShiftF + 3*i);
                    tForceIndices.set(1, tShiftF + 3*i + 1);
                    tForceIndices.set(2, tShiftF + 3*i + 2);
                }
                final RowIntMatrix tStressIndices = mHasStress ? IntMatrixCache.getMatRow(tRowNum, 2) : null;
                final RowMatrix tStressDxyz = mHasStress ? MatrixCache.getMatRow(tRowNum, 2) : null;
                final int tShiftS = mHasStress ? rData.mStress.size() : -1;
                if (mHasStress) {
                    // 按照目前展开约定，会是这个顺序
                    tStressIndices.set(0, 0, tShiftS);   tStressIndices.set(0, 1, tShiftS+3);
                    tStressIndices.set(1, 0, tShiftS+1); tStressIndices.set(1, 1, tShiftS+5);
                    tStressIndices.set(2, 0, tShiftS+2); tStressIndices.set(2, 1, tShiftS+4);
                    // 第二列用来计算交叉项，原子自身力不贡献应力
                    tStressDxyz.set(0, 0, 0.0); tStressDxyz.set(0, 1, 0.0);
                    tStressDxyz.set(1, 0, 0.0); tStressDxyz.set(1, 1, 0.0);
                    tStressDxyz.set(2, 0, 0.0); tStressDxyz.set(2, 1, 0.0);
                }
                final int[] j = {0};
                tAPC.nl_().forEachNeighbor(i, tBasis.rcut(), (dx, dy, dz, idx) -> {
                    tFpPartial.row(3 + 3*j[0]).fill(new ShiftVector(tBasisSize, tBasisSize*j[0], tFpPxCross.internalData()));
                    tFpPartial.row(4 + 3*j[0]).fill(new ShiftVector(tBasisSize, tBasisSize*j[0], tFpPyCross.internalData()));
                    tFpPartial.row(5 + 3*j[0]).fill(new ShiftVector(tBasisSize, tBasisSize*j[0], tFpPzCross.internalData()));
                    if (mHasForce) {
                        assert tForceIndices != null;
                        tForceIndices.set(3 + 3*j[0], tShiftF + 3*idx);
                        tForceIndices.set(4 + 3*j[0], tShiftF + 3*idx + 1);
                        tForceIndices.set(5 + 3*j[0], tShiftF + 3*idx + 2);
                    }
                    if (mHasStress) {
                        assert tStressIndices != null;
                        // 按照目前展开约定，会是这个顺序
                        tStressIndices.set(3 + 3*j[0], 0, tShiftS);   tStressIndices.set(3 + 3*j[0], 1, tShiftS+3);
                        tStressIndices.set(4 + 3*j[0], 0, tShiftS+1); tStressIndices.set(4 + 3*j[0], 1, tShiftS+5);
                        tStressIndices.set(5 + 3*j[0], 0, tShiftS+2); tStressIndices.set(5 + 3*j[0], 1, tShiftS+4);
                        assert tStressDxyz != null;
                        // 第二列用来计算交叉项
                        tStressDxyz.set(3 + 3*j[0], 0, dx); tStressDxyz.set(3 + 3*j[0], 1, dy);
                        tStressDxyz.set(4 + 3*j[0], 0, dy); tStressDxyz.set(4 + 3*j[0], 1, dz);
                        tStressDxyz.set(5 + 3*j[0], 0, dz); tStressDxyz.set(5 + 3*j[0], 1, dx);
                    }
                    ++j[0];
                });
                // 将数据转换为 torch 的 tensor，这里最快的方式是利用 torch 的 from_numpy 进行转换
                PyObject tPyFpPartial, tPyForceIndices=null, tPyStressIndices=null, tPyStressDxyz=null;
                try (PyCallable fFromNumpy = TORCH.getAttr("from_numpy", PyCallable.class)) {
                    tPyFpPartial = fFromNumpy.callAs(PyObject.class, tFpPartial.numpy());
                    if (tTrainInFloat) {
                        try (PyObject oPyFpPartial = tPyFpPartial; PyCallable fFloat = oPyFpPartial.getAttr("float", PyCallable.class)) {
                            tPyFpPartial = fFloat.callAs(PyObject.class);
                        }
                    }
                    if (mHasForce) {
                        assert tForceIndices != null;
                        tPyForceIndices = fFromNumpy.callAs(PyObject.class, tForceIndices.numpy());
                    }
                    if (mHasStress) {
                        assert tStressIndices != null;
                        tPyStressIndices = fFromNumpy.callAs(PyObject.class, tStressIndices.numpy());
                        assert tStressDxyz != null;
                        tPyStressDxyz = fFromNumpy.callAs(PyObject.class, tStressDxyz.numpy());
                        if (tTrainInFloat) {
                            try (PyObject oPyStressDxyz = tPyStressDxyz; PyCallable fFloat = oPyStressDxyz.getAttr("float", PyCallable.class)) {
                                tPyStressDxyz = fFloat.callAs(PyObject.class);
                            }
                        }
                    }
                }
                rData.mFpPartial.get(tType-1).add(tPyFpPartial);
                rData.mNN[tType-1].add(tNN);
                MatrixCache.returnMat(tFpPartial);
                if (mHasForce) {
                    assert tForceIndices != null;
                    rData.mForceIndices.get(tType-1).add(tPyForceIndices);
                    IntVectorCache.returnVec(tForceIndices);
                }
                if (mHasStress) {
                    assert tStressIndices != null;
                    rData.mStressIndices.get(tType-1).add(tPyStressIndices);
                    IntMatrixCache.returnMat(tStressIndices);
                    assert tStressDxyz != null;
                    rData.mStressDxyz.get(tType-1).add(tPyStressDxyz);
                    MatrixCache.returnMat(tStressDxyz);
                }
            }
        }
        // 这里后添加能量，这样 rData.mEng.size() 对应正确的索引
        rData.mEng.add(aEnergy);
        // 这里后添加力，这样 rData.mForce.size() 对应正确的索引
        if (mHasForce) {
            assert aForces != null;
            rData.mForce.addAll(aForces.asVecRow());
        }
        // 这里后添加应力应力，这样 rData.mStress.size() 对应正确的索引
        if (mHasStress) {
            assert aStress != null;
            rData.mStress.addAll(aStress);
        }
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
        if (mHasForce || mHasStress) {
            calRefEngFpPartialAndAdd_(aAtomData, aEnergy, aForces, aStress, mTrainData);
        } else {
            calRefEngFpAndAdd_(aAtomData, aEnergy, mTrainData);
        }
        mTrainData.mAtomNum.add(aAtomData.atomNumber());
        mTrainData.mVolume.add(aAtomData.volume());
        mHasData = true;
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
        if (mHasForce || mHasStress) {
            calRefEngFpPartialAndAdd_(aAtomData, aEnergy, aForces, aStress, mTestData);
        } else {
            calRefEngFpAndAdd_(aAtomData, aEnergy, mTestData);
        }
        mTestData.mAtomNum.add(aAtomData.atomNumber());
        mTestData.mVolume.add(aAtomData.volume());
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
    
    /** 保存训练的势函数 */
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        List rModels = new ArrayList();
        for (int i = 0; i < mSymbols.length; ++i) {
            Map rBasis = new LinkedHashMap();
            mBasis[i].save(rBasis);
            if (mBasis[i] instanceof Mirror) {
                rModels.add(Maps.of(
                    "symbol", mSymbols[i],
                    "basis", rBasis
                ));
                continue;
            }
            List<?> tModelState;
            try (PyCallable fSaveModel = mTrainer.getAttr("save_model", PyCallable.class)) {
                tModelState = fSaveModel.callAs(List.class, i);
            }
            rModels.add(Maps.of(
                "symbol", mSymbols[i],
                "basis", rBasis,
                "ref_eng", mRefEngs.get(i),
                "norm_mu", mNormMu[i].asList(),
                "norm_sigma", mNormSigma[i].asList(),
                "norm_mu_eng", mNormMuEng,
                "norm_sigma_eng", mNormSigmaEng,
                "nn", Maps.of(
                    "type", "feed_forward",
                    "input_dim", tModelState.get(0),
                    "hidden_dims", tModelState.get(1),
                    "hidden_weights", tModelState.get(2),
                    "hidden_biases", tModelState.get(3),
                    "output_weight", tModelState.get(4),
                    "output_bias", tModelState.get(5)
                )
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
    @SuppressWarnings({"rawtypes", "unchecked"}) @VisibleForTesting
    public static void convert(String aOldPath, String aNewPath) throws IOException {
        Map rInfo = (aOldPath.endsWith(".yaml") || aOldPath.endsWith(".yml")) ? IO.yaml2map(aOldPath) : IO.json2map(aOldPath);
        rInfo.put("version", NNAP.VERSION); // 更新为最新的版本，保证旧的 nnap 直接报错不兼容
        List<Map> tModels = (List<Map>)rInfo.get("models");
        for (Map tModel : tModels) {
            Object tTorch = tModel.remove("torch");
            if (tTorch == null) continue;
            List<?> tModelState;
            try (PyCallable fConvertBytes = SP.Python.getAs(PyCallable.class, "nnap_trainer.convert_bytes_")) {
                tModelState = fConvertBytes.callAs(List.class, new Object[]{Base64.getDecoder().decode(tTorch.toString())});
            }
            tModel.put("nn", Maps.of(
                "type", "feed_forward",
                "input_dim", tModelState.get(0),
                "hidden_dims", tModelState.get(1),
                "hidden_weights", tModelState.get(2),
                "hidden_biases", tModelState.get(3),
                "output_weight", tModelState.get(4),
                "output_bias", tModelState.get(5)
            ));
        }
        if (aNewPath.endsWith(".yaml") || aNewPath.endsWith(".yml")) IO.map2yaml(rInfo, aNewPath);
        else IO.map2json(rInfo, aNewPath);
    }
}
