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
import jse.math.vector.IVector;
import jse.math.vector.Vectors;
import jse.cptr.*;
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
         * 设置 NNAP 内部计算的默认精度，默认为 double
         */
        public static String PRECISION = OS.env("JSE_NNAP_PRECISION", "double");
    }
    public final static int VERSION = 6;
    
    final String[] mSymbols;
    final @Nullable String mUnits;
    final boolean mSinglePrecision;
    boolean mDead = false;
    final int mThreadNumber;
    final Basis2[] mBasis;
    final NeuralNetwork2[] mNN;
    public int ntypes() {return mSymbols.length;}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
    public String units() {return mUnits;}
    public String precision() {return mSinglePrecision ? "single" : "double";}
    // 现在所有数据都改为 c 指针
    final AnyCPointer mDataIn, mDataOut;
    final IntCPointer mInNums, mOutNums;
    final AnyCPointer mFpHyperParam, mFpParam, mNnParam, mNormParam;
    final IDoubleOrFloatCPointer mOutEng;
    final IGrowableDoubleOrFloatCPointer mNlDx, mNlDy, mNlDz, mGradNlDx, mGradNlDy, mGradNlDz;
    final GrowableIntCPointer mNlType, mNlIdx;
    
    @SuppressWarnings("unchecked")
    NNAP2(@Nullable String aLibDir, @Nullable String aProjectName, Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber, @Nullable String aPrecision) throws Exception {
        mThreadNumber = aThreadNumber;
        Number tVersion = (Number)aModelInfo.get("version");
        if (tVersion != null) {
            int tVersionValue = tVersion.intValue();
            if (tVersionValue > VERSION) throw new IllegalArgumentException("Unsupported version: " + tVersionValue);
        }
        mUnits = UT.Code.toString(aModelInfo.get("units"));
        String tPrecision = aPrecision!=null?aPrecision:Conf.PRECISION;
        if (tPrecision.equals("single")) {
            mSinglePrecision = true;
        } else
        if (tPrecision.equals("double")) {
            mSinglePrecision = false;
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
        mDataIn = AnyCPointer.calloc(32);
        mDataOut = AnyCPointer.calloc(32);
        mInNums = IntCPointer.calloc(32);
        mOutNums = IntCPointer.calloc(32);
        mNlDx = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlDy = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlDz = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlType = new GrowableIntCPointer(16);
        mNlIdx = new GrowableIntCPointer(16);
        mGradNlDx = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mGradNlDy = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mGradNlDz = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mOutEng = mSinglePrecision ? FloatCPointer.malloc(1) : DoubleCPointer.malloc(1);
        // 初始化参数数组
        mFpHyperParam = AnyCPointer.malloc(tModelSize);
        mFpParam = AnyCPointer.malloc(tModelSize);
        mNnParam = AnyCPointer.malloc(tModelSize);
        for (int i = 0; i < tModelSize; ++i) {
            int tSize = mBasis[i].hyperParameterSize();
            IDoubleOrFloatCPointer tFpHyperParam = mSinglePrecision ? FloatCPointer.malloc(Math.max(1, tSize)) : DoubleCPointer.malloc(Math.max(1, tSize));
            fill_(tFpHyperParam, mBasis[i].hyperParameters());
            mFpHyperParam.putAt(i, tFpHyperParam);
            
            tSize = mBasis[i].parameterSize();
            IDoubleOrFloatCPointer tFpParam = mSinglePrecision ? FloatCPointer.malloc(Math.max(1, tSize)) : DoubleCPointer.malloc(Math.max(1, tSize));
            fill_(tFpParam, mBasis[i].parameters());
            mFpParam.putAt(i, tFpParam);
            
            tSize = mNN[i].parameterSize();
            IDoubleOrFloatCPointer tNnParam = mSinglePrecision ? FloatCPointer.malloc(Math.max(1, tSize)) : DoubleCPointer.malloc(Math.max(1, tSize));
            fill_(tNnParam, mNN[i].parameters());
            mNnParam.putAt(i, tNnParam);
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
            IVector aNormSigma = tNormSigma==null ? null : Vectors.from(tNormSigma);
            List<? extends Number> tNormMu = (List<? extends Number>)tModels.get(i).get("norm_mu");
            IVector aNormMu = tNormMu==null ? null : Vectors.from(tNormMu);
            
            int tSize = mBasis[i].size()*2 + 2;
            IDoubleOrFloatCPointer tNormParam = mSinglePrecision ? FloatCPointer.malloc(tSize) : DoubleCPointer.malloc(tSize);
            tNormParam.putAtD(0, (aNormMuEng+aRefEng));
            tNormParam.putAtD(1, aNormSigmaEng);
            fill_(tNormParam.plus(2), aNormMu);
            fill_(tNormParam.plus(mBasis[i].size()+2), aNormSigma);
            mNormParam.putAt(i, tNormParam);
        }
    }
    public NNAP2(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber, String aPrecision) throws Exception {
        this(null, null, aModelInfo, aThreadNumber, aPrecision);
        // 直接开始 jit 编译
        compileJIT_();
    }
    public NNAP2(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber, String aPrecision) throws Exception {
        this(IO.toParentPath(aModelPath), toValidProjectName(IO.toFileName(aModelPath)),
             aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath), aThreadNumber, aPrecision);
        // 直接开始 jit 编译
        compileJIT_();
    }
    public NNAP2(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws Exception {this(aModelPath, aThreadNumber, null);}
    public NNAP2(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws Exception {this(aModelInfo, aThreadNumber, null);}
    public NNAP2(Map<?, ?> aModelInfo) throws Exception {this(aModelInfo, 1);}
    public NNAP2(String aModelPath) throws Exception {this(aModelPath, 1);}
    
    private final static Pattern PROJECT_INVALID_NAME = Pattern.compile("[^a-zA-Z0-9_\\-]");
    static String toValidProjectName(String aProjectName) {
        aProjectName = aProjectName.replace(".yaml", "").replace(".yml", "").replace(".json", "").replace(".jnn", "").replace(".nn", "");
        aProjectName = PROJECT_INVALID_NAME.matcher(aProjectName).replaceAll("");
        return aProjectName.isEmpty() ? null : aProjectName;
    }
    
    private static void fill_(IDoubleOrFloatCPointer rPtr, @Nullable IVector aVec) {
        if (aVec == null) return;
        final int tSize = aVec.size();
        for (int i = 0; i < tSize; ++i) {
            rPtr.putAtD(i, aVec.get(i));
        }
    }
    
    // jit stuffs
    final NNAPGEN mNNAPGEN;
    IJITEngine mJITEngine = null;
    private static final String NAME_CAL_ENERGY = "jse_nnap_calEnergy", NAME_CAL_ENERGYFORCE = "jse_nnap_calEnergyForce";
    private static final String NAME_STAT_NEINUM_LAMMPS = "jse_nnap_statNeiNumLammps", NAME_COMPUTE_LAMMPS = "jse_nnap_computeLammps";
    private IJITMethod mCalEnergy = null, mCalEnergyForce = null;
    private IJITMethod mStatNeiNumLammps = null, mComputeLammps = null;
    private void compileJIT_() throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        if (mJITEngine!=null) throw new IllegalStateException("compileJIT() has already been called");
        // 开始 jit
        mJITEngine = mNNAPGEN.initEngine(mSinglePrecision, Conf.OPTIM_LEVEL, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING);
        mJITEngine.setMethodNames(NAME_CAL_ENERGY, NAME_CAL_ENERGYFORCE, NAME_STAT_NEINUM_LAMMPS, NAME_COMPUTE_LAMMPS).compile();
        mCalEnergy = mJITEngine.findMethod(NAME_CAL_ENERGY);
        mCalEnergyForce = mJITEngine.findMethod(NAME_CAL_ENERGYFORCE);
        mStatNeiNumLammps = mJITEngine.findMethod(NAME_STAT_NEINUM_LAMMPS);
        mComputeLammps = mJITEngine.findMethod(NAME_COMPUTE_LAMMPS);
    }
    
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        shutdown_();
    }
    void shutdown_() {
        for (int i = 0; i < mSymbols.length; ++i) {
            mFpHyperParam.getAsCPointerAt(i).free();
            mFpParam.getAsCPointerAt(i).free();
            mNnParam.getAsCPointerAt(i).free();
            mNormParam.getAsCPointerAt(i).free();
        }
        mInNums.free();
        mOutNums.free();
        mNlDx.free(); mNlDy.free(); mNlDz.free();
        mNlType.free();
        mFpParam.free(); mNnParam.free(); mNormParam.free();
        mDataIn.free();
        
        mOutEng.free();
        mDataOut.free();
        
        if (mJITEngine!=null) mJITEngine.shutdown();
    }
    
    @Override public boolean isShutdown() {return mDead;}
    @Override public int nthreads() {return mThreadNumber;}
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
    
    private final DoubleList mNlDxBuf = new DoubleList(16), mNlDyBuf = new DoubleList(16), mNlDzBuf = new DoubleList(16);
    private final IntList mNlTypeBuf = new IntList(16), mNlIdxBuf = new IntList(16);
    
    
    private int buildNL_(IDxyzTypeIdxIterable aNL, double aRCut, boolean aRequireGrad) {
        final int tTypeNum = ntypes();
        // 缓存情况需要先清空这些
        mNlDxBuf.clear(); mNlDyBuf.clear(); mNlDzBuf.clear();
        mNlTypeBuf.clear(); mNlIdxBuf.clear();
        aNL.forEachDxyzTypeIdx(aRCut, (dx, dy, dz, type, idx) -> {
            // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查
            if (type > tTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+tTypeNum+")");
            // 简单缓存近邻列表
            mNlDxBuf.add(dx); mNlDyBuf.add(dy); mNlDzBuf.add(dz);
            mNlTypeBuf.add(type); mNlIdxBuf.add(idx);
        });
        int tNeiNum = mNlIdxBuf.size();
        mNlDx.ensureCapacity(tNeiNum); mNlDx.fillD(mNlDxBuf);
        mNlDy.ensureCapacity(tNeiNum); mNlDy.fillD(mNlDyBuf);
        mNlDz.ensureCapacity(tNeiNum); mNlDz.fillD(mNlDzBuf);
        mNlType.ensureCapacity(tNeiNum);
        mNlType.fill(mNlTypeBuf);
        if (aRequireGrad) {
            mGradNlDx.ensureCapacity(tNeiNum);
            mGradNlDy.ensureCapacity(tNeiNum);
            mGradNlDz.ensureCapacity(tNeiNum);
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
            int tNeiNum = buildNL_(nl, mBasis[cType-1].rcut(), false);
            mInNums.putAt(0, tNeiNum);
            mInNums.putAt(1, cType);
            // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
            mDataIn.putAt(0, mInNums);
            mDataIn.putAt(1, mNlDx);
            mDataIn.putAt(2, mNlDy);
            mDataIn.putAt(3, mNlDz);
            mDataIn.putAt(4, mNlType);
            mDataIn.putAt(5, mFpHyperParam);
            mDataIn.putAt(6, mFpParam);
            mDataIn.putAt(7, mNnParam);
            mDataIn.putAt(8, mNormParam.getAt(cType-1));
            mDataOut.putAt(0, mOutEng);
            // 调用 jit 方法获取结果
            int tCode = mCalEnergy.invoke(mDataIn, mDataOut);
            if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
            double tEng = mOutEng.getD();
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
            // 近邻列表构建以及相关值设置
            int tNeiNum = buildNL_(nl, mBasis[cType-1].rcut(), true);
            mInNums.putAt(0, tNeiNum);
            mInNums.putAt(1, cType);
            // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
            mDataIn.putAt(0, mInNums);
            mDataIn.putAt(1, mNlDx);
            mDataIn.putAt(2, mNlDy);
            mDataIn.putAt(3, mNlDz);
            mDataIn.putAt(4, mNlType);
            mDataIn.putAt(5, mFpHyperParam);
            mDataIn.putAt(6, mFpParam);
            mDataIn.putAt(7, mNnParam);
            mDataIn.putAt(8, mNormParam.getAt(cType-1));
            mDataOut.putAt(0, mOutEng);
            mDataOut.putAt(1, mGradNlDx);
            mDataOut.putAt(2, mGradNlDy);
            mDataOut.putAt(3, mGradNlDz);
            // 调用 jit 方法获取结果
            int tCode = mCalEnergyForce.invoke(mDataIn, mDataOut);
            if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
            double tEng = mOutEng.getD();
            if (rEnergyAccumulator != null) {
                rEnergyAccumulator.add(threadID, cIdx, -1, tEng);
            }
            // 累加交叉项到近邻
            for (int j = 0; j < tNeiNum; ++j) {
                double dx = mNlDxBuf.get(j);
                double dy = mNlDyBuf.get(j);
                double dz = mNlDzBuf.get(j);
                int idx = mNlIdxBuf.get(j);
                // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查；
                // 直接遍历查询不走合并了，实测专门合并还会影响效率
                double fx = mGradNlDx.getAtD(j);
                double fy = mGradNlDy.getAtD(j);
                double fz = mGradNlDz.getAtD(j);
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
    
    private void validNlLammps_(int aNeiNum) {
        mNlDx.ensureCapacity(aNeiNum);
        mNlDy.ensureCapacity(aNeiNum);
        mNlDz.ensureCapacity(aNeiNum);
        mNlType.ensureCapacity(aNeiNum);
        mNlIdx.ensureCapacity(aNeiNum);
        mGradNlDx.ensureCapacity(aNeiNum);
        mGradNlDy.ensureCapacity(aNeiNum);
        mGradNlDz.ensureCapacity(aNeiNum);
    }
    
    void computeLammps(PairNNAP2 aPair) throws Exception {
        // 种类的缓存优化
        int inum = aPair.listInum();
        for (int type = 1; type <= aPair.mTypeNum; ++type) {
            GrowableIntCPointer tList = aPair.mTypeIlistBuf[type];
            tList.ensureCapacity(inum);
            aPair.mTypeIlist.putAt(type, tList);
        }
        // 近邻列表大小获取和缓存合理化
        IntCPointer ilist = aPair.listIlist();
        IntCPointer numneigh = aPair.listNumneigh();
        mInNums.putAt(0, inum);
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, ilist);
        mDataIn.putAt(2, numneigh);
        mStatNeiNumLammps.invoke(mDataIn, mOutNums);
        validNlLammps_(mOutNums.getAt(0));
        
        // compute 开始，参数设置
        mInNums.putAt(0, inum);
        mInNums.putAt(1, aPair.mTypeNum);
        mInNums.putAt(2, aPair.eflagEither()?1:0);
        mInNums.putAt(3, aPair.vflagEither()?1:0);
        mInNums.putAt(4, aPair.eflagAtom()?1:0);
        mInNums.putAt(5, aPair.vflagAtom()?1:0);
        mInNums.putAt(6, aPair.cvflagAtom()?1:0);
        
        // 统一指定所有的位置，这样保证一致和避免其他调用导致的意外结果
        mDataIn.putAt(0, mInNums);
        mDataIn.putAt(1, mNlDx);
        mDataIn.putAt(2, mNlDy);
        mDataIn.putAt(3, mNlDz);
        mDataIn.putAt(4, mNlType);
        mDataIn.putAt(5, mNlIdx);
        mDataIn.putAt(6, mFpHyperParam);
        mDataIn.putAt(7, mFpParam);
        mDataIn.putAt(8, mNnParam);
        mDataIn.putAt(9, mNormParam);
        mDataIn.putAt(10, aPair.atomX());
        mDataIn.putAt(11, aPair.atomType());
        mDataIn.putAt(12, ilist);
        mDataIn.putAt(13, numneigh);
        mDataIn.putAt(14, aPair.listFirstneigh());
        mDataIn.putAt(15, aPair.mCutsq);
        mDataIn.putAt(16, aPair.mLmpType2NNAPType);
        mDataIn.putAt(17, aPair.mTypeIlist);
        mDataIn.putAt(18, aPair.mTypeInum);
        
        mDataOut.putAt(0, aPair.atomF());
        mDataOut.putAt(1, mGradNlDx);
        mDataOut.putAt(2, mGradNlDy);
        mDataOut.putAt(3, mGradNlDz);
        mDataOut.putAt(4, aPair.engVdwl());
        mDataOut.putAt(5, aPair.eatom());
        mDataOut.putAt(6, aPair.virial());
        mDataOut.putAt(7, aPair.vatom());
        mDataOut.putAt(8, aPair.cvatom());
        
        // 调用 jit 方法计算
        int tCode = mComputeLammps.invoke(mDataIn, mDataOut);
        if (tCode>0) throw new IllegalStateException("Exit code: "+tCode);
    }
}
