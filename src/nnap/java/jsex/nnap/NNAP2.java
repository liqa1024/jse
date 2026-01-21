package jsex.nnap;

import jse.atom.AtomicParameterCalculator;
import jse.atom.IPairPotential;
import jse.clib.*;
import jse.code.IO;
import jse.code.OS;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;
import jse.parallel.IAutoShutdown;
import jsex.nnap.basis.Chebyshev2;
import jsex.nnap.nn.FeedForward2;
import org.apache.groovy.util.Maps;
import org.jetbrains.annotations.*;

import java.io.IOException;
import java.net.URL;
import java.util.*;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.Conf.VERSION_MASK;
import static jse.code.OS.JAVA_HOME;

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
public class NNAP2 implements IAutoShutdown {
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
        public static int OPTIM_LEVEL = OS.envI("JSE_NNAP_OPTIM_LEVEL", SimpleJIT.OPTIM_BASE);
    }
    
    public final static int VERSION = 6;
    private final static String INTERFACE_NAME = "nnap_interface.cpp";
    private final static String[] SRC_NAME = {
          "nnap_util.hpp"
        , "nn_FeedForward.hpp"
        , "basis_Chebyshev.hpp"
        , "basis_ChebyshevUtil.hpp"
        , "nnap_interface.h"
        , INTERFACE_NAME
    };
    
    private final String[] mSymbols;
    private final @Nullable String mUnits;
    private boolean mDead = false;
    private final int mThreadNumber;
    private final Chebyshev2 mBasis;
    private final FeedForward2 mNN;
    public int atomTypeNumber() {return mSymbols.length;}
    // jit stuffs
    private static final String NAME_FORWARD = "jse_nnap_forward";
    private final SimpleJIT.Engine mEngine;
    private final SimpleJIT.Method mMethodForward;
    // 现在所有数据都改为 c 指针
    private final NestedCPointer mDataIn, mDataOut;
    private final IntCPointer mNumParam;
    private final DoubleCPointer mOut, mFp, mFpParam, mNnParam, mNormParam;
    private final GrowableDoubleCPointer mNlDx, mNlDy, mNlDz, mFpCache, mNnCache;
    private final GrowableIntCPointer mNlType;
    
    @SuppressWarnings("unchecked")
    NNAP2(String aLibDir, String aProjectName, Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws Exception {
        mThreadNumber = aThreadNumber;
        Number tVersion = (Number)aModelInfo.get("version");
        if (tVersion != null) {
            int tVersionValue = tVersion.intValue();
            if (tVersionValue > VERSION) throw new IllegalArgumentException("Unsupported version: " + tVersionValue);
        }
        mUnits = UT.Code.toString(aModelInfo.get("units"));
        List<? extends Map<String, ?>> tModels = (List<? extends Map<String, ?>>)aModelInfo.get("models");
        if (tModels == null) throw new IllegalArgumentException("No models in ModelInfo");
        int tModelSize = tModels.size();
        mSymbols = new String[tModelSize];
        for (int i = 0; i < tModelSize; ++i) {
            Object tSymbol = tModels.get(i).get("symbol");
            if (tSymbol == null) throw new IllegalArgumentException("No symbol in model");
            mSymbols[i] = tSymbol.toString();
        }
        // 不管怎么样先初始化数组
        mDataIn = NestedCPointer.malloc(8);
        mDataOut = NestedCPointer.malloc(4);
        mNumParam = IntCPointer.malloc(2); // NeighNum, ntypes
        mNumParam.putAt(1, mSymbols.length);
        mDataIn.putAt(0, mNumParam);
        mNlDx = new GrowableDoubleCPointer(16);
        mNlDy = new GrowableDoubleCPointer(16);
        mNlDz = new GrowableDoubleCPointer(16);
        mNlType = new GrowableIntCPointer(16);
        mOut = DoubleCPointer.malloc(1);
        mDataOut.putAt(0, mOut);
        mFpCache = new GrowableDoubleCPointer(128);
        mNnCache = new GrowableDoubleCPointer(128);
        
        // 临时实现的简单加载模型
        Map<String, ?> tModel = tModels.get(0);
        Map<?, ?> tBasisMap = (Map<?, ?>)tModel.get("basis");
        if (tBasisMap==null) tBasisMap = Maps.of("type", "chebyshev");
        mBasis = Chebyshev2.load(mSymbols, tBasisMap);
        Map<?, ?> tNNMap = (Map<?, ?>)tModel.get("nn");
        if (tNNMap ==null) throw new IllegalArgumentException("No nn in model, torch model is invalid now.");
        mNN = FeedForward2.load(tNNMap);
        // 继续初始化参数数组
        final int tFpSize = mBasis.size();
        mFp = DoubleCPointer.malloc(tFpSize);
        mDataOut.putAt(1, mFp);
        mFpParam = DoubleCPointer.malloc(mBasis.parameterSize()+1);
        mFpParam.putAt(0, mBasis.rcut());
        fill_(mFpParam.plus(1), mBasis.parameters());
        mDataIn.putAt(5, mFpParam);
        mNnParam = DoubleCPointer.malloc(mNN.parameterSize());
        fill_(mNnParam, mNN.parameters());
        mDataIn.putAt(6, mNnParam);
        // 归一化系数读取
        mNormParam = DoubleCPointer.malloc(tFpSize+tFpSize + 2);
        Number tNormSigmaEng = null, tNormMuEng = null;
        for (int i = 0; i < tModelSize; ++i) {
            if (tNormSigmaEng == null) tNormSigmaEng = (Number)tModels.get(i).get("norm_sigma_eng");
            if (tNormMuEng == null) tNormMuEng = (Number)tModels.get(i).get("norm_mu_eng");
        }
        double aNormSigmaEng = tNormSigmaEng==null ? 1.0 : tNormSigmaEng.doubleValue();
        double aNormMuEng = tNormMuEng==null ? 0.0 : tNormMuEng.doubleValue();
        Number tRefEng = (Number)tModel.get("ref_eng");
        double aRefEng = tRefEng==null ? 0.0 : tRefEng.doubleValue();
        List<? extends Number> tNormSigma = (List<? extends Number>)UT.Code.get(tModel, "norm_sigma", "norm_vec");
        IVector aNormSigma = tNormSigma==null ? null : Vectors.from(tNormSigma);
        List<? extends Number> tNormMu = (List<? extends Number>)tModel.get("norm_mu");
        IVector aNormMu = tNormMu==null ? null : Vectors.from(tNormMu);
        fill_(mNormParam, aNormMu);
        fill_(mNormParam.plus(tFpSize), aNormSigma);
        mNormParam.putAt(tFpSize+tFpSize, aNormMuEng+aRefEng);
        mNormParam.putAt(tFpSize+tFpSize+1, aNormSigmaEng);
        mDataIn.putAt(7, mNormParam);
        
        // 代码生成，先收集参数
        final Map<String, Object> tGenMap = new LinkedHashMap<>();
        mBasis.updateGenMap(tGenMap);
        mNN.updateGenMap(tGenMap);
        // 开始 jit
        String tUniqueID = UT.Code.uniqueID(OS.OS_NAME, Compiler.EXE_PATH, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, tGenMap, NNAP2.VERSION, Conf.OPTIM_LEVEL, Conf.CMAKE_CXX_COMPILER, Conf.CMAKE_CXX_FLAGS, Conf.CMAKE_SETTING);
        mEngine = SimpleJIT.engine().setLibDir(aLibDir).setProjectName(aProjectName+"_"+tUniqueID)
            .setOptimLevel(Conf.OPTIM_LEVEL).setCmakeSettings(Conf.CMAKE_SETTING)
            .setCmakeCxxCompiler(Conf.CMAKE_CXX_COMPILER).setCmakeCxxCompiler(Conf.CMAKE_CXX_FLAGS);
        // 源码处理完全重写，直接使用现有项目，并进行代码生成
        mEngine.setSrcDirIniter(wd -> {
            for (String tName : SRC_NAME) {
                codeGen_(IO.getResource("nnap2/src/"+tName), wd+tName, tGenMap);
            }
            // 注意这里需要使用 jit 中的通用 CMakeLists，确保 project name 同步
            mEngine.writeCmakeFile(wd, INTERFACE_NAME);
            return wd;
        });
        mEngine.setMethodNames(NAME_FORWARD).compile();
        mMethodForward = mEngine.findMethod(NAME_FORWARD);
    }
    public NNAP2(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws Exception {
        this(OS.WORKING_DIR, "nnapjit", aModelInfo, aThreadNumber);
    }
    public NNAP2(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws Exception {
        this(IO.toParentPath(aModelPath), IO.toFileName(aModelPath).replace(".yaml", "").replace(".yml", "").replace(".json", "").replace(".jnn", "").replace(".nn", ""),
             aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath), aThreadNumber);
    }
    public NNAP2(Map<?, ?> aModelInfo) throws Exception {this(aModelInfo, 1);}
    public NNAP2(String aModelPath) throws Exception {this(aModelPath, 1);}
    
    
    private static void fill_(DoubleCPointer rPtr, @Nullable IVector aVec) {
        if (aVec == null) return;
        final int tSize = aVec.size();
        for (int i = 0; i < tSize; ++i) {
            rPtr.putAt(i, aVec.get(i));
        }
    }
    
    private static final String MARKER_REMOVE_START = "// >>> NNAPGEN REMOVE";
    private static final String MARKER_REMOVE_END = "// <<< NNAPGEN REMOVE";
    private static final String MARKER_REPEAT_START = "// >>> NNAPGEN REPEAT";
    private static final String MARKER_REPEAT_END = "// <<< NNAPGEN REPEAT";
    
    private static void codeGen_(URL aSourceURL, String aTargetPath, Map<String, Object> aGenMap) throws IOException {
        final int[] tState = {0}; // 0: line replace, 1: remove, 2: repeat
        final List<String> rBuf0 = new ArrayList<>(), rBuf1 = new ArrayList<>();
        IO.map(aSourceURL, aTargetPath, line -> {
            switch(tState[0]) {
            case 0: {
                if (line.trim().equals(MARKER_REMOVE_START)) {
                    tState[0] = 1;
                    return null;
                } else
                if (line.trim().equals(MARKER_REPEAT_START)) {
                    tState[0] = 2;
                    return null;
                } else {
                    return baseReplace_(line, aGenMap);
                }
            }
            case 1: {
                if (line.trim().equals(MARKER_REMOVE_END)) {
                    tState[0] = 0;
                }
                return null;
            }
            case 2: {
                if (line.trim().startsWith(MARKER_REPEAT_END)) {
                    tState[0] = 0;
                    String tKey = line.trim().substring(MARKER_REPEAT_END.length()).trim();
                    Object tValue = aGenMap.get(tKey);
                    if (tValue==null) throw new IllegalStateException("Missing repeat key: "+tKey);
                    int tLoop = ((Number)tValue).intValue();
                    rBuf1.clear();
                    for (int i = 0; i < tLoop; ++i) {
                        for (String tBufLine : rBuf0) {
                            String tLine = tBufLine.replace("NNAPGENX_", "NNAPGEN"+i+"_");
                            rBuf1.add(baseReplace_(tLine, aGenMap));
                        }
                    }
                    rBuf0.clear();
                    return String.join("\n", rBuf1);
                } else {
                    rBuf0.add(line);
                    return null;
                }
            }
            default: {
                throw new IllegalStateException();
            }}
        });
    }
    private static String baseReplace_(String aLine, Map<String, Object> aGenMap) {
        // 简单串联，在没有遇到性能问题前都就这样做好了
        for (Map.Entry<String, Object> tEntry : aGenMap.entrySet()) {
            String tKey = tEntry.getKey();
            if (tKey.startsWith("[") && tKey.endsWith("]")) continue;
            aLine = aLine.replace(tKey, tEntry.getValue().toString());
        }
        return aLine;
    }
    
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        
        mNumParam.free();
        mNlDx.free(); mNlDy.free(); mNlDz.free();
        mNlType.free();
        mFpParam.free(); mNnParam.free(); mNormParam.free();
        mDataIn.free();
        
        mOut.free();
        mFp.free();
        mFpCache.free(); mNnCache.free();
        mDataOut.free();
        
        mEngine.shutdown();
    }
    
    
    private final DoubleList mNlDxBuf = new DoubleList(16), mNlDyBuf = new DoubleList(16), mNlDzBuf = new DoubleList(16);
    private final IntList mNlTypeBuf = new IntList(16);
    
    private int buildNL_(IPairPotential.IDxyzTypeIdxIterable aNL, double aRCut) {
        final int tTypeNum = atomTypeNumber();
        // 缓存情况需要先清空这些
        mNlDxBuf.clear(); mNlDyBuf.clear(); mNlDzBuf.clear();
        mNlTypeBuf.clear();
        aNL.forEachDxyzTypeIdx(aRCut, (dx, dy, dz, type, idx) -> {
            // 为了效率这里不进行近邻检查，因此需要上层近邻列表提供时进行检查
            if (type > tTypeNum) throw new IllegalArgumentException("Exist type ("+type+") greater than the input typeNum ("+tTypeNum+")");
            // 简单缓存近邻列表
            mNlDxBuf.add(dx); mNlDyBuf.add(dy); mNlDzBuf.add(dz);
            mNlTypeBuf.add(type);
        });
        int tNeiNum = mNlTypeBuf.size();
        mNlDx.ensureCapacity(tNeiNum); mNlDx.fill(mNlDxBuf);
        mNlDy.ensureCapacity(tNeiNum); mNlDy.fill(mNlDyBuf);
        mNlDz.ensureCapacity(tNeiNum); mNlDz.fill(mNlDzBuf);
        mNlType.ensureCapacity(tNeiNum); mNlType.fill(mNlTypeBuf);
        return tNeiNum;
    }
    
    public void calEnergy(int aAtomNumber, IPairPotential.INeighborListGetter aNeighborListGetter, IPairPotential.IEnergyAccumulator rEnergyAccumulator) throws Exception {
        if (mDead) throw new IllegalStateException("This NNAP is dead");
        aNeighborListGetter.forEachNLWithException(null, null, (threadID, cIdx, cType, nl) -> {
            int tNeiNum = buildNL_(nl, mBasis.rcut());
            mNumParam.putAt(0, tNeiNum);
            mFpCache.ensureCapacity(mBasis.forwardCacheSize(tNeiNum, 0));
            mNnCache.ensureCapacity(mNN.forwardCacheSize(0));
            // 对于可扩张的数组，指针是非持久的，因此每次计算要重新获取
            mDataIn.putAt(1, mNlDx);
            mDataIn.putAt(2, mNlDy);
            mDataIn.putAt(3, mNlDz);
            mDataIn.putAt(4, mNlType);
            mDataOut.putAt(2, mFpCache);
            mDataOut.putAt(3, mNnCache);
            mMethodForward.invoke(mDataIn, mDataOut);
            double tEng = mOut.get();
            rEnergyAccumulator.add(threadID, cIdx, -1, tEng);
        });
    }
    
    /// IPairPotential stuffs
    public double calEnergy(AtomicParameterCalculator aAPC) throws Exception {
        if (mDead) throw new IllegalStateException("This Potential is dead");
        final int oThreadNum = aAPC.threadNumber();
        final int tAtomNum = aAPC.atomNumber();
        final int tTypeNum = atomTypeNumber();
        
        final double[] rTotEng = {0.0};
        
        aAPC.setThreadNumber(mThreadNumber);
        calEnergy(tAtomNum, (initDo, finalDo, neighborListDo) -> {
            aAPC.pool_().parforWithException(tAtomNum, initDo, finalDo, (i, threadID) -> {
                final int cType = tTypeNum<=0 ? 0 : aAPC.atomType_().get(i);
                neighborListDo.run(threadID, i, cType, (rmax, dxyzTypeDo) -> {
                    // 根据 neighborListHalf 来确定是否开启半数优化
                    aAPC.nl_().forEachNeighbor(i, rmax, false, true, (dx, dy, dz, idx) -> {
                        int tType = tTypeNum<=0 ? 0 : aAPC.atomType_().get(idx);
                        dxyzTypeDo.run(dx, dy, dz, tType, idx);
                    });
                });
            });
        }, (threadID, cIdx, idx, eng) -> {
            rTotEng[0] += eng;
        });
        aAPC.setThreadNumber(oThreadNum);
        return rTotEng[0];
    }
}
