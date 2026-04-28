package jsex.nnap;

import jse.atom.IPairPotential;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.jit.IJITEngine;
import jse.jit.IJITMethod;
import jse.cptr.*;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import jse.math.vector.Vectors;
import jsex.nnap.basis.Basis2;
import jsex.nnap.nn.NeuralNetwork2;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.util.*;
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
public class NNAP2 implements IPairPotential {
    public final static class Conf {
        /**
         * 自定义构建 nnap 的 cmake 参数设置，
         * 会在构建时使用 -D ${key}=${value} 传入
         */
        public final static Map<String, String> CMAKE_SETTING = OS.envMap("JSE_CMAKE_SETTING_NNAP");
        /**
         * 自定义构建 nnap 时使用的编译器，
         * cmake 有时不能自动检测到希望使用的编译器
         */
        public static @Nullable String CMAKE_CXX_COMPILER = OS.env("JSE_CMAKE_CXX_COMPILER_NNAP", jse.code.Conf.CMAKE_CXX_COMPILER);
        public static @Nullable String CMAKE_CXX_FLAGS    = OS.env("JSE_CMAKE_CXX_FLAGS_NNAP"   , jse.code.Conf.CMAKE_CXX_FLAGS);
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
    final boolean mSingle;
    boolean mDead = false;
    final int mNumThreads;
    final Basis2[] mBasis;
    final NeuralNetwork2[] mNN;
    @Override public int ntypes() {return mSymbols.length;}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
    public String units() {return mUnits;}
    public String precision() {return mSingle ? "single" : "double";}
    // 现在所有数据都改为 c 指针
    final AnyCPointer[] mDataIn, mDataOut;
    final IntCPointer[] mInNums, mOutNums;
    final AnyCPointer mFpHyperParam, mFpParam, mNnParam, mNormParam;
    private final IDoubleOrFloatCPointer[] mOutEng;
    private final IGrowableDoubleOrFloatCPointer[] mNlDx, mNlDy, mNlDz, mGradNlDx, mGradNlDy, mGradNlDz, mFpForwardCache;
    private final GrowableIntCPointer[] mNlType, mNlIdx;
    
    private final DoubleList[] mNlDxBuf, mNlDyBuf, mNlDzBuf;
    private final IntList[] mNlTypeBuf, mNlIdxBuf;
    
    final int mTotCParamSize, mTotGradCParamSize, mTotParamSize;
    final IDoubleOrFloatCPointer mTotCParam;
    IDoubleOrFloatCPointer[] mGradTotCParam = null;
    AnyCPointer[] mGradFpParam, mGradNnParam;
    final Vector mTotParam;
    Vector mGradTotParam = null;
    
    @SuppressWarnings("unchecked")
    NNAP2(@Nullable String aLibDir, @Nullable String aProjectName, Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aNumThreads, @Nullable String aPrecision) throws Exception {
        mNumThreads = aNumThreads;
        Number tVersion = (Number)aModelInfo.get("version");
        if (tVersion != null) {
            int tVersionValue = tVersion.intValue();
            if (tVersionValue > VERSION) throw new IllegalArgumentException("Unsupported version: " + tVersionValue);
        }
        mUnits = UT.Code.toString(aModelInfo.get("units"));
        String tPrecision = aPrecision!=null?aPrecision:Conf.PRECISION;
        if (tPrecision.equals("single")) {
            mSingle = true;
        } else
        if (tPrecision.equals("double")) {
            mSingle = false;
        } else {
            throw new IllegalArgumentException("NNAP precision MUST be 'double' or 'single', input: " + tPrecision);
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
        mBasis = Basis2.load(NewCollections.map(tModels, info -> {
            Object tBasisInfo = info.get("basis");
            return tBasisInfo!=null ? tBasisInfo : Maps.of("type", "spherical_chebyshev");
        }));
        mNN = NeuralNetwork2.load(mBasis, NewCollections.map(tModels, info -> {
            Object tNNInfo = info.get("nn");
            if (tNNInfo ==null) throw new IllegalArgumentException("No nn in model, torch model is invalid now.");
            return tNNInfo;
        }));
        mNNAPGEN = new NNAPGEN(aLibDir, aProjectName, mBasis, mNN);
        // 初始化数组
        mDataIn = new AnyCPointer[mNumThreads]; mDataOut = new AnyCPointer[mNumThreads];
        mInNums = new IntCPointer[mNumThreads]; mOutNums = new IntCPointer[mNumThreads];
        mNlDx = new IGrowableDoubleOrFloatCPointer[mNumThreads];
        mNlDy = new IGrowableDoubleOrFloatCPointer[mNumThreads];
        mNlDz = new IGrowableDoubleOrFloatCPointer[mNumThreads];
        mNlType = new GrowableIntCPointer[mNumThreads];
        mNlIdx = new GrowableIntCPointer[mNumThreads];
        mGradNlDx = new IGrowableDoubleOrFloatCPointer[mNumThreads];
        mGradNlDy = new IGrowableDoubleOrFloatCPointer[mNumThreads];
        mGradNlDz = new IGrowableDoubleOrFloatCPointer[mNumThreads];
        mOutEng = new IDoubleOrFloatCPointer[mNumThreads];
        mFpForwardCache = new IGrowableDoubleOrFloatCPointer[mNumThreads];
        mNlDxBuf = new DoubleList[mNumThreads];
        mNlDyBuf = new DoubleList[mNumThreads];
        mNlDzBuf = new DoubleList[mNumThreads];
        mNlTypeBuf = new IntList[mNumThreads];
        mNlIdxBuf = new IntList[mNumThreads];
        for (int ti = 0; ti < mNumThreads; ++ti) {
            mDataIn[ti] = AnyCPointer.calloc(32);
            mDataOut[ti] = AnyCPointer.calloc(32);
            mInNums[ti] = IntCPointer.calloc(32);
            mOutNums[ti] = IntCPointer.calloc(32);
            mNlDx[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mNlDy[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mNlDz[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mNlType[ti] = new GrowableIntCPointer();
            mNlIdx[ti] = new GrowableIntCPointer();
            mGradNlDx[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mGradNlDy[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mGradNlDz[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mOutEng[ti] = mSingle ? FloatCPointer.malloc(1) : DoubleCPointer.malloc(1);
            mFpForwardCache[ti] = mSingle ? new GrowableFloatCPointer(1) : new GrowableDoubleCPointer(1);
            mNlDxBuf[ti] = new DoubleList(16);
            mNlDyBuf[ti] = new DoubleList(16);
            mNlDzBuf[ti] = new DoubleList(16);
            mNlTypeBuf[ti] = new IntList(16);
            mNlIdxBuf[ti] = new IntList(16);
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
        mTotCParam = mSingle ? FloatCPointer.malloc(mTotCParamSize) : DoubleCPointer.malloc(mTotCParamSize);
        mTotParam = Vectors.zeros(mTotParamSize);
        mFpHyperParam = AnyCPointer.malloc(tModelSize);
        mFpParam = AnyCPointer.malloc(tModelSize);
        mNnParam = AnyCPointer.malloc(tModelSize);
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
        mNormParam = AnyCPointer.malloc(tModelSize);
        Number tNormSigmaEng = null, tNormMuEng = null;
        for (int i = 0; i < tModelSize; ++i) {
            if (tNormSigmaEng == null) tNormSigmaEng = (Number)tModels.get(i).get("norm_sigma_eng");
            if (tNormMuEng == null) tNormMuEng = (Number)tModels.get(i).get("norm_mu_eng");
        }
        double aNormSigmaEng = tNormSigmaEng==null ? 1.0 : tNormSigmaEng.doubleValue();
        double aNormMuEng = tNormMuEng==null ? 0.0 : tNormMuEng.doubleValue();
        for (int i = 0; i < tModelSize; ++i) {
            Number tRefEng = (Number)tModels.get(i).get("ref_eng");
            double aRefEng = tRefEng==null ? 0.0 : tRefEng.doubleValue();
            List<? extends Number> tNormSigma = (List<? extends Number>)UT.Code.get(tModels.get(i), "norm_sigma", "norm_vec");
            List<? extends Number> tNormMu = (List<? extends Number>)tModels.get(i).get("norm_mu");
            
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
    }
    public NNAP2(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aNumThreads) throws Exception {
        this(null, null, aModelInfo, aNumThreads, null);
        // 直接开始 jit 编译
        compileJIT_();
    }
    public NNAP2(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aNumThreads) throws Exception {
        this(IO.toParentPath(aModelPath), toValidProjectName(IO.toFileName(aModelPath)),
             aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath), aNumThreads, null);
        // 直接开始 jit 编译
        compileJIT_();
    }
    public NNAP2(Map<?, ?> aModelInfo) throws Exception {this(aModelInfo, 1);}
    public NNAP2(String aModelPath) throws Exception {this(aModelPath, 1);}
    
    private final static Pattern PROJECT_INVALID_NAME = Pattern.compile("[^a-zA-Z0-9_\\-]");
    static String toValidProjectName(String aProjectName) {
        aProjectName = aProjectName.replace(".yaml", "").replace(".yml", "").replace(".json", "").replace(".jnn", "").replace(".nn", "");
        aProjectName = PROJECT_INVALID_NAME.matcher(aProjectName).replaceAll("");
        return aProjectName.isEmpty() ? null : aProjectName;
    }
    
    // jit stuffs
    final NNAPGEN mNNAPGEN;
    IJITEngine mJITEngine = null;
    private static final String NAME_CAL_FP = "jse_nnap_calFp", NAME_CAL_ENERGY = "jse_nnap_calEnergy", NAME_CAL_ENERGYFORCE = "jse_nnap_calEnergyForce";
    private static final String NAME_STAT_NEINUM_LAMMPS = "jse_nnap_statNeiNumLammps", NAME_COMPUTE_LAMMPS = "jse_nnap_computeLammps";
    private static final String NAME_FORWARD_ENERGY = "jse_nnap_forwardEnergyRaw", NAME_BACKWARD_ENERGY = "jse_nnap_backwardEnergyRaw";
    private IJITMethod mCalFp = null, mCalEnergy = null, mCalEnergyForce = null;
    private IJITMethod mStatNeiNumLammps = null, mComputeLammps = null;
    private IJITMethod mForwardEnergyRaw = null, mBackwardEnergyRaw = null;
    private void compileJIT_() throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mJITEngine!=null) throw new IllegalStateException("compileJIT() has already been called");
        // 开始 jit
        mJITEngine = mNNAPGEN.initEngine(mSingle);
        mJITEngine.setMethodNames(NAME_CAL_FP, NAME_CAL_ENERGY, NAME_CAL_ENERGYFORCE, NAME_STAT_NEINUM_LAMMPS, NAME_COMPUTE_LAMMPS, NAME_FORWARD_ENERGY, NAME_BACKWARD_ENERGY).compile();
        mCalFp = mJITEngine.findMethod(NAME_CAL_FP);
        mCalEnergy = mJITEngine.findMethod(NAME_CAL_ENERGY);
        mCalEnergyForce = mJITEngine.findMethod(NAME_CAL_ENERGYFORCE);
        mStatNeiNumLammps = mJITEngine.findMethod(NAME_STAT_NEINUM_LAMMPS);
        mComputeLammps = mJITEngine.findMethod(NAME_COMPUTE_LAMMPS);
        mForwardEnergyRaw = mJITEngine.findMethod(NAME_FORWARD_ENERGY);
        mBackwardEnergyRaw = mJITEngine.findMethod(NAME_BACKWARD_ENERGY);
    }
    
    @Override public void close() throws Exception {
        if (mDead) return;
        mDead = true;
        close_();
    }
    void close_() throws Exception {
        for (int ti = 0; ti < mNumThreads; ++ti) {
            mInNums[ti].free();
            mOutNums[ti].free();
            mNlDx[ti].free(); mNlDy[ti].free(); mNlDz[ti].free();
            mNlType[ti].free();
            mDataIn[ti].free();
        }
        mTotCParam.free();
        mFpHyperParam.free(); mFpParam.free(); mNnParam.free(); mNormParam.free();
        if (mGradTotCParam != null) {
            for (int ti = 0; ti < mNumThreads; ++ti) {
                mGradTotCParam[ti].free();
                mGradFpParam[ti].free(); mGradNnParam[ti].free();
            }
        }
        for (int ti = 0; ti < mNumThreads; ++ti) {
            mOutEng[ti].free();
            mFpForwardCache[ti].free();
            mDataOut[ti].free();
        }
        if (mJITEngine!=null) mJITEngine.close();
    }
    
    @Override public boolean isClosed() {return mDead;}
    @Override public int nthreads() {return mNumThreads;}
    @Override public double rcutMax() {
        double tRCut = 0.0;
        for (Basis2 tBasis : mBasis) {
            tRCut = Math.max(tRCut, tBasis.rcut());
        }
        return tRCut;
    }
    public double rcut(int aType) {
        return mBasis[aType-1].rcut();
    }
    
    
    private int buildNL_(int aThreadID, IDxyzTypeIdxIterable aNL, double aRCut, boolean aRequireGrad) {
        final DoubleList tNlDxBuf = mNlDxBuf[aThreadID], tNlDyBuf = mNlDyBuf[aThreadID], tNlDzBuf = mNlDzBuf[aThreadID];
        final IntList tNlTypeBuf = mNlTypeBuf[aThreadID], tNlIdxBuf = mNlIdxBuf[aThreadID];
        IGrowableDoubleOrFloatCPointer tNlDx = mNlDx[aThreadID], tNlDy = mNlDy[aThreadID], tNlDz = mNlDz[aThreadID];
        IGrowableDoubleOrFloatCPointer tGradNlDx = mGradNlDx[aThreadID], tGradNlDy = mGradNlDy[aThreadID], tGradNlDz = mGradNlDz[aThreadID];
        GrowableIntCPointer tNlType = mNlType[aThreadID];
        final int tTypeNum = ntypes();
        // 缓存情况需要先清空这些
        tNlDxBuf.clear(); tNlDyBuf.clear(); tNlDzBuf.clear();
        tNlTypeBuf.clear(); tNlIdxBuf.clear();
        aNL.forEachDxyzTypeIdx(aRCut, (dx, dy, dz, type, idx) -> {
            // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查
            if (type > tTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+tTypeNum+")");
            // 简单缓存近邻列表
            tNlDxBuf.add(dx); tNlDyBuf.add(dy); tNlDzBuf.add(dz);
            tNlTypeBuf.add(type); tNlIdxBuf.add(idx);
        });
        int tNeiNum = tNlIdxBuf.size();
        tNlDx.ensureCapacity(tNeiNum); tNlDx.fillD(tNlDxBuf);
        tNlDy.ensureCapacity(tNeiNum); tNlDy.fillD(tNlDyBuf);
        tNlDz.ensureCapacity(tNeiNum); tNlDz.fillD(tNlDzBuf);
        tNlType.ensureCapacity(tNeiNum);
        tNlType.fill(tNlTypeBuf);
        if (aRequireGrad) {
            tGradNlDx.ensureCapacity(tNeiNum);
            tGradNlDy.ensureCapacity(tNeiNum);
            tGradNlDz.ensureCapacity(tNeiNum);
        }
        return tNeiNum;
    }
    
    /**
     * {@inheritDoc}
     * @param aAtomNumber {@inheritDoc}
     * @param aNeighborListGetter {@inheritDoc}
     * @param rEnergyAccumulator {@inheritDoc}
     */
    @Override public void calEnergy(int aAtomNumber, INeighborListGetter aNeighborListGetter, IEnergyAccumulator rEnergyAccumulator) throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aNeighborListGetter.forEachNLWithException(null, null, (threadID, cIdx, cType, nl) -> {
            // 近邻列表构建以及相关值设置
            int tNeiNum = buildNL_(threadID, nl, mBasis[cType-1].rcut(), false);
            double tEng = calEnergy(threadID,
                mNlDx[threadID], mNlDy[threadID], mNlDz[threadID],
                mNlType[threadID], tNeiNum, cType
            );
            rEnergyAccumulator.add(threadID, cIdx, -1, tEng);
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
        aNeighborListGetter.forEachNLWithException(null, null, (threadID, cIdx, cType, nl) -> {
            IGrowableDoubleOrFloatCPointer tGradNlDx = mGradNlDx[threadID];
            IGrowableDoubleOrFloatCPointer tGradNlDy = mGradNlDy[threadID];
            IGrowableDoubleOrFloatCPointer tGradNlDz = mGradNlDz[threadID];
            DoubleList tNlDxBuf = mNlDxBuf[threadID];
            DoubleList tNlDyBuf = mNlDyBuf[threadID];
            DoubleList tNlDzBuf = mNlDzBuf[threadID];
            IntList tNlIdxBuf = mNlIdxBuf[threadID];
            // 近邻列表构建以及相关值设置
            int tNeiNum = buildNL_(threadID, nl, mBasis[cType-1].rcut(), true);
            double tEng = calEnergyForce(threadID,
                mNlDx[threadID], mNlDy[threadID], mNlDz[threadID],
                mNlType[threadID], tNeiNum, cType,
                tGradNlDx, tGradNlDy, tGradNlDz
            );
            if (rEnergyAccumulator != null) {
                rEnergyAccumulator.add(threadID, cIdx, -1, tEng);
            }
            // 累加交叉项到近邻
            for (int j = 0; j < tNeiNum; ++j) {
                double dx = tNlDxBuf.get(j);
                double dy = tNlDyBuf.get(j);
                double dz = tNlDzBuf.get(j);
                int idx = tNlIdxBuf.get(j);
                // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查；
                // 直接遍历查询不走合并了，实测专门合并还会影响效率
                double fx = tGradNlDx.getAtD(j);
                double fy = tGradNlDy.getAtD(j);
                double fz = tGradNlDz.getAtD(j);
                if (rForceAccumulator != null) {
                    rForceAccumulator.add(threadID, cIdx, idx, fx, fy, fz);
                }
                if (rVirialAccumulator != null) {
                    // GPUMD 给出的更具对称性的形式要求累加到近邻的 index 上
                    rVirialAccumulator.add(threadID, -1, idx, fx, fy, fz, dx, dy, dz);
                }
            }
        });
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
        mGradTotCParam = new IDoubleOrFloatCPointer[mNumThreads];
        mGradFpParam = new AnyCPointer[mNumThreads];
        mGradNnParam = new AnyCPointer[mNumThreads];
        for (int i = 0; i < tModelSize; ++i) {
            mBasis[i].requireGrad(mNumThreads);
            mNN[i].requireGrad(mNumThreads);
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
        for (int ti = 0; ti < mNumThreads; ++ti) {
            IDoubleOrFloatCPointer tGradTotCParam = mSingle ? FloatCPointer.malloc(mTotGradCParamSize) : DoubleCPointer.malloc(mTotGradCParamSize);
            AnyCPointer tGradFpParam = AnyCPointer.malloc(tModelSize);
            AnyCPointer tGradNnParam = AnyCPointer.malloc(tModelSize);
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
        for (int ti = 0; ti < mNumThreads; ++ti) {
            mGradTotCParam[ti].fillD(0.0, mTotGradCParamSize);
            mGradTotParam.fill(0.0);
        }
    }
    public IDoubleOrFloatCPointer normMu(int aType) {
        IDoubleOrFloatCPointer tParam = mSingle ? mNormParam.getAsFloatCPointerAt(aType-1) : mNormParam.getAsDoubleCPointerAt(aType-1);
        tParam.rightShift(2);
        return tParam;
    }
    public IDoubleOrFloatCPointer normSigma(int aType) {
        IDoubleOrFloatCPointer tParam = mSingle ? mNormParam.getAsFloatCPointerAt(aType-1) : mNormParam.getAsDoubleCPointerAt(aType-1);
        tParam.rightShift(2+mBasis[aType-1].size());
        return tParam;
    }
    
    
    /// jit stuffs
    public void calFp(int aThreadID, IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz,
                      IntCPointer aNlType, int aNumNei, int aCType, IDoubleOrFloatCPointer rFp) {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntCPointer tInNums = mInNums[aThreadID];
        AnyCPointer tDataIn = mDataIn[aThreadID];
        AnyCPointer tDataOut = mDataOut[aThreadID];
        tInNums.putAt(0, aNumNei);
        tInNums.putAt(1, aCType);
        // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
        tDataIn.putAt(0, tInNums);
        tDataIn.putAt(1, aNlDx);
        tDataIn.putAt(2, aNlDy);
        tDataIn.putAt(3, aNlDz);
        tDataIn.putAt(4, aNlType);
        tDataIn.putAt(5, mFpHyperParam);
        tDataIn.putAt(6, mFpParam);
        tDataOut.putAt(0, rFp);
        // 调用 jit 方法获取结果
        int tCode = mCalFp.invoke(tDataIn, tDataOut);
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
    }
    public double calEnergy(int aThreadID, IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz,
                            IntCPointer aNlType, int aNumNei, int aCType) {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntCPointer tInNums = mInNums[aThreadID];
        AnyCPointer tDataIn = mDataIn[aThreadID];
        AnyCPointer tDataOut = mDataOut[aThreadID];
        IDoubleOrFloatCPointer tOutEng = mOutEng[aThreadID];
        tInNums.putAt(0, aNumNei);
        tInNums.putAt(1, aCType);
        // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
        tDataIn.putAt(0, tInNums);
        tDataIn.putAt(1, aNlDx);
        tDataIn.putAt(2, aNlDy);
        tDataIn.putAt(3, aNlDz);
        tDataIn.putAt(4, aNlType);
        tDataIn.putAt(5, mFpHyperParam);
        tDataIn.putAt(6, mFpParam);
        tDataIn.putAt(7, mNnParam);
        tDataIn.putAt(8, mNormParam.getAt(aCType-1));
        tDataOut.putAt(0, tOutEng);
        // 调用 jit 方法获取结果
        int tCode = mCalEnergy.invoke(tDataIn, tDataOut);
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
        return tOutEng.getD();
    }
    public double calEnergyForce(int aThreadID, IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz,
                                 IntCPointer aNlType, int aNumNei, int aCType,
                                 IDoubleOrFloatCPointer rGradNlDx, IDoubleOrFloatCPointer rGradNlDy, IDoubleOrFloatCPointer rGradNlDz) {
        IntCPointer tInNums = mInNums[aThreadID];
        AnyCPointer tDataIn = mDataIn[aThreadID];
        AnyCPointer tDataOut = mDataOut[aThreadID];
        IGrowableDoubleOrFloatCPointer tFpForwardCache = mFpForwardCache[aThreadID];
        IDoubleOrFloatCPointer tOutEng = mOutEng[aThreadID];
        tInNums.putAt(0, aNumNei);
        tInNums.putAt(1, aCType);
        tFpForwardCache.ensureCapacity(mBasis[aCType-1].forwardCacheSize(aNumNei));
        // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
        tDataIn.putAt(0, tInNums);
        tDataIn.putAt(1, aNlDx);
        tDataIn.putAt(2, aNlDy);
        tDataIn.putAt(3, aNlDz);
        tDataIn.putAt(4, aNlType);
        tDataIn.putAt(5, mFpHyperParam);
        tDataIn.putAt(6, mFpParam);
        tDataIn.putAt(7, mNnParam);
        tDataIn.putAt(8, mNormParam.getAt(aCType-1));
        tDataOut.putAt(0, tOutEng);
        tDataOut.putAt(1, rGradNlDx);
        tDataOut.putAt(2, rGradNlDy);
        tDataOut.putAt(3, rGradNlDz);
        tDataOut.putAt(4, tFpForwardCache);
        // 调用 jit 方法获取结果
        int tCode = mCalEnergyForce.invoke(tDataIn, tDataOut);
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
        return tOutEng.getD();
    }
    
    public double forwardEnergyRaw(int aThreadID, IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz,
                                   IntCPointer aNlType, int aNumNei, int aCType, IGrowableDoubleOrFloatCPointer rCaches) {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        IntCPointer tInNums = mInNums[aThreadID];
        AnyCPointer tDataIn = mDataIn[aThreadID];
        AnyCPointer tDataOut = mDataOut[aThreadID];
        IDoubleOrFloatCPointer tOutEngRaw = mOutEng[aThreadID];
        tInNums.putAt(0, aNumNei);
        tInNums.putAt(1, aCType);
        // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
        tDataIn.putAt(0, tInNums);
        tDataIn.putAt(1, aNlDx);
        tDataIn.putAt(2, aNlDy);
        tDataIn.putAt(3, aNlDz);
        tDataIn.putAt(4, aNlType);
        tDataIn.putAt(5, mFpHyperParam);
        tDataIn.putAt(6, mFpParam);
        tDataIn.putAt(7, mNnParam);
        tDataIn.putAt(8, mNormParam.getAt(aCType-1));
        tDataOut.putAt(0, tOutEngRaw);
        int tSizeFpForwardCache = mBasis[aCType-1].forwardCacheSize(aNumNei);
        int tSizeNnForwardCache = mNN[aCType-1].forwardCacheSize();
        rCaches.ensureCapacity(tSizeFpForwardCache + tSizeNnForwardCache);
        tDataOut.putAt(1, rCaches);
        tDataOut.putAt(2, rCaches.plus(tSizeFpForwardCache));
        // 调用 jit 方法获取结果
        int tCode = mForwardEnergyRaw.invoke(tDataIn, tDataOut);
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
        return tOutEngRaw.getD();
    }
    public void backwardEnergyRaw(int aThreadID, double aGradEngRaw, IDoubleOrFloatCPointer aNlDx, IDoubleOrFloatCPointer aNlDy, IDoubleOrFloatCPointer aNlDz,
                                  IntCPointer aNlType, int aNumNei, int aCType, IDoubleOrFloatCPointer aCaches) {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mGradTotParam == null) throw new IllegalStateException("No grad in NNAP, invoke `requireGrad()` first.");
        IntCPointer tInNums = mInNums[aThreadID];
        AnyCPointer tDataIn = mDataIn[aThreadID];
        AnyCPointer tDataOut = mDataOut[aThreadID];
        IDoubleOrFloatCPointer tInGradEngRaw = mOutEng[aThreadID];
        tInNums.putAt(0, aNumNei);
        tInNums.putAt(1, aCType);
        tInGradEngRaw.setD(aGradEngRaw);
        // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
        tDataIn.putAt(0, tInNums);
        tDataIn.putAt(1, aNlDx);
        tDataIn.putAt(2, aNlDy);
        tDataIn.putAt(3, aNlDz);
        tDataIn.putAt(4, aNlType);
        tDataIn.putAt(5, mFpHyperParam);
        tDataIn.putAt(6, mFpParam);
        tDataIn.putAt(7, mNnParam);
        tDataIn.putAt(8, mNormParam.getAt(aCType-1));
        tDataIn.putAt(9, aCaches);
        tDataIn.putAt(10, aCaches.plus(mBasis[aCType-1].forwardCacheSize(aNumNei)));
        tDataOut.putAt(0, tInGradEngRaw);
        tDataOut.putAt(1, mGradFpParam[aThreadID]);
        tDataOut.putAt(2, mGradNnParam[aThreadID]);
        // 调用 jit 方法获取结果
        int tCode = mBackwardEnergyRaw.invoke(tDataIn, tDataOut);
        if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
    }
    
    
    /// lammps stuff
    private void validNlLammps_(int aNeiNum) {
        mNlDx[0].ensureCapacity(aNeiNum);
        mNlDy[0].ensureCapacity(aNeiNum);
        mNlDz[0].ensureCapacity(aNeiNum);
        mNlType[0].ensureCapacity(aNeiNum);
        mNlIdx[0].ensureCapacity(aNeiNum);
        mGradNlDx[0].ensureCapacity(aNeiNum);
        mGradNlDy[0].ensureCapacity(aNeiNum);
        mGradNlDz[0].ensureCapacity(aNeiNum);
        IGrowableDoubleOrFloatCPointer tFpForwardCache = mFpForwardCache[0];
        for (int i = 0; i < mSymbols.length; ++i) {
            tFpForwardCache.ensureCapacity(mBasis[i].forwardCacheSize(aNeiNum));
        }
    }
    void computeLammps(PairNNAP2 aPair) throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        // 种类的缓存优化
        int inum = aPair.listInum();
        for (int type = 1; type <= aPair.mTypeNum; ++type) {
            GrowableIntCPointer tList = aPair.mTypeIlistBuf[type];
            tList.ensureCapacity(inum);
            aPair.mTypeIlist.putAt(type, tList);
        }
        IntCPointer tInNums = mInNums[0];
        IntCPointer tOutNums = mOutNums[0];
        AnyCPointer tDataIn = mDataIn[0];
        AnyCPointer tDataOut = mDataOut[0];
        // 近邻列表大小获取和缓存合理化
        IntCPointer ilist = aPair.listIlist();
        IntCPointer numneigh = aPair.listNumneigh();
        tInNums.putAt(0, inum);
        tDataIn.putAt(0, tInNums);
        tDataIn.putAt(1, ilist);
        tDataIn.putAt(2, numneigh);
        mStatNeiNumLammps.invoke(tDataIn, tOutNums);
        validNlLammps_(tOutNums.getAt(0));
        
        // compute 开始，参数设置
        tInNums.putAt(0, inum);
        tInNums.putAt(1, aPair.mTypeNum);
        tInNums.putAt(2, aPair.eflagEither()?1:0);
        tInNums.putAt(3, aPair.vflagEither()?1:0);
        tInNums.putAt(4, aPair.eflagAtom()?1:0);
        tInNums.putAt(5, aPair.vflagAtom()?1:0);
        tInNums.putAt(6, aPair.cvflagAtom()?1:0);
        
        // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
        tDataIn.putAt(0, tInNums);
        tDataIn.putAt(1, mNlDx[0]);
        tDataIn.putAt(2, mNlDy[0]);
        tDataIn.putAt(3, mNlDz[0]);
        tDataIn.putAt(4, mNlType[0]);
        tDataIn.putAt(5, mNlIdx[0]);
        tDataIn.putAt(6, mFpHyperParam);
        tDataIn.putAt(7, mFpParam);
        tDataIn.putAt(8, mNnParam);
        tDataIn.putAt(9, mNormParam);
        tDataIn.putAt(10, aPair.atomX());
        tDataIn.putAt(11, aPair.atomType());
        tDataIn.putAt(12, ilist);
        tDataIn.putAt(13, numneigh);
        tDataIn.putAt(14, aPair.listFirstneigh());
        tDataIn.putAt(15, aPair.mCutsq);
        tDataIn.putAt(16, aPair.mLmpType2NNAPType);
        tDataIn.putAt(17, aPair.mTypeIlist);
        tDataIn.putAt(18, aPair.mTypeInum);
        
        tDataOut.putAt(0, aPair.atomF());
        tDataOut.putAt(1, mGradNlDx[0]);
        tDataOut.putAt(2, mGradNlDy[0]);
        tDataOut.putAt(3, mGradNlDz[0]);
        tDataOut.putAt(4, mFpForwardCache[0]);
        tDataOut.putAt(5, aPair.engVdwl());
        tDataOut.putAt(6, aPair.eatom());
        tDataOut.putAt(7, aPair.virial());
        tDataOut.putAt(8, aPair.vatom());
        tDataOut.putAt(9, aPair.cvatom());
        
        // 调用 jit 方法计算
        int tCode = mComputeLammps.invoke(tDataIn, tDataOut);
        if (tCode>0) throw new IllegalStateException("Exit code: "+tCode);
    }
}
