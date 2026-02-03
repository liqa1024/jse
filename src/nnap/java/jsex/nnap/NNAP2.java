package jsex.nnap;

import jse.atom.IPairPotential;
import jse.clib.*;
import jse.code.IO;
import jse.code.OS;
import jse.code.SP;
import jse.code.UT;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.code.collection.NewCollections;
import jse.code.functional.IUnaryFullOperator;
import jse.math.vector.IVector;
import jse.math.vector.Vectors;
import jse.cptr.*;
import jsex.nnap.basis.Basis2;
import jsex.nnap.nn.NeuralNetwork2;
import org.apache.groovy.util.Maps;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.*;

import java.io.BufferedReader;
import java.net.URL;
import java.util.*;
import java.util.regex.Pattern;

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
        public static int OPTIM_LEVEL = OS.envI("JSE_NNAP_OPTIM_LEVEL", SimpleJIT.OPTIM_BASE);
        /**
         * 设置 lammps 会使用的近邻列表数目大小限制，默认为 2000
         */
        public static int LAMMPS_NL_MAX = OS.envI("JSE_NNAP_LAMMPS_NL_MAX", 2000);
        /**
         * 设置 NNAP 内部计算的默认精度，默认为 double
         */
        public static String PRECISION = OS.env("JSE_NNAP_PRECISION", "double");
    }
    
    public final static int VERSION = 6;
    private static final String JIT_NAME = "nnapjit";
    private final static String INTERFACE_NAME = "nnap_interface.cpp";
    private final static String[] SRC_NAME = {
          "nnap_util.hpp"
        , "nn_FeedForward.hpp"
        , "basis_Chebyshev.hpp"
        , "basis_ChebyshevUtil.hpp"
        , "basis_SphericalChebyshev.hpp"
        , "basis_SphericalUtil.hpp"
        , "basis_SphericalUtil0.hpp"
        , "nnap_interface.h"
        , INTERFACE_NAME
    };
    
    private final String[] mSymbols;
    private final @Nullable String mUnits;
    private final boolean mSinglePrecision;
    private boolean mDead = false;
    private final int mThreadNumber;
    private final Basis2[] mBasis;
    private final NeuralNetwork2[] mNN;
    public int atomTypeNumber() {return mSymbols.length;}
    @Override public boolean hasSymbol() {return true;}
    @Override public String symbol(int aType) {return mSymbols[aType-1];}
    public String units() {return mUnits;}
    public String precision() {return mSinglePrecision ? "single" : "double";}
    // jit stuffs
    private static final String NAME_CAL_ENERGY = "jse_nnap_calEnergy", NAME_CAL_ENERGYFORCE = "jse_nnap_calEnergyForce", NAME_COMPUTE_LAMMPS = "jse_nnap_computeLammps";
    private final SimpleJIT.Engine mEngine;
    private final SimpleJIT.Method mCalEnergy, mCalEnergyForce, mComputeLammps;
    // 现在所有数据都改为 c 指针
    private final NestedCPointer mDataIn, mDataOut;
    private final IntCPointer mInNums;
    private final NestedCPointer mFpHyperParam, mFpParam, mNnParam, mNormParam, mNnForwardCache, mNnBackwardCache;
    private final CPointer mOutEng;
    private final IGrowableCPointer mNlDx, mNlDy, mNlDz, mGradNlDx, mGradNlDy, mGradNlDz, mFpForwardCache, mFpBackwardCache;
    private final GrowableIntCPointer mNlType, mNlIdx;
    
    @SuppressWarnings("unchecked")
    NNAP2(String aLibDir, String aProjectName, Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber, @Nullable String aPrecision) throws Exception {
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
        // 不管怎么样先初始化数组
        mDataIn = NestedCPointer.calloc(20);
        mDataOut = NestedCPointer.calloc(20);
        mInNums = IntCPointer.calloc(20);
        mNlDx = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlDy = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlDz = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mNlType = new GrowableIntCPointer(16);
        mNlIdx = new GrowableIntCPointer(16);
        mGradNlDx = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mGradNlDy = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mGradNlDz = mSinglePrecision ? new GrowableFloatCPointer(16) : new GrowableDoubleCPointer(16);
        mOutEng = mSinglePrecision ? FloatCPointer.malloc(1) : DoubleCPointer.malloc(1);
        mFpForwardCache = mSinglePrecision ? new GrowableFloatCPointer(128) : new GrowableDoubleCPointer(128);
        mFpBackwardCache = mSinglePrecision ? new GrowableFloatCPointer(128) : new GrowableDoubleCPointer(128);
        
        mBasis = Basis2.load(NewCollections.map(tModels, info -> {
            Object tBasisInfo = info.get("basis");
            return tBasisInfo!=null ? tBasisInfo : Maps.of("type", "spherical_chebyshev");
        }));
        mNN = NeuralNetwork2.load(mBasis, NewCollections.map(tModels, info -> {
            Object tNNInfo = info.get("nn");
            if (tNNInfo ==null) throw new IllegalArgumentException("No nn in model, torch model is invalid now.");
            return tNNInfo;
        }));
        // 继续初始化参数数组
        mFpHyperParam = NestedCPointer.malloc(tModelSize);
        mFpParam = NestedCPointer.malloc(tModelSize);
        mNnParam = NestedCPointer.malloc(tModelSize);
        mNnForwardCache = NestedCPointer.malloc(tModelSize);
        mNnBackwardCache = NestedCPointer.malloc(tModelSize);
        for (int i = 0; i < tModelSize; ++i) {
            if (mSinglePrecision) {
                FloatCPointer tFpHyperParam = FloatCPointer.malloc(mBasis[i].hyperParameterSize());
                fill_(tFpHyperParam, mBasis[i].hyperParameters());
                mFpHyperParam.putAt(i, tFpHyperParam);
                
                FloatCPointer tFpParam = FloatCPointer.malloc(mBasis[i].parameterSize());
                fill_(tFpParam, mBasis[i].parameters());
                mFpParam.putAt(i, tFpParam);
                
                FloatCPointer tNnParam = FloatCPointer.malloc(mNN[i].parameterSize());
                fill_(tNnParam, mNN[i].parameters());
                mNnParam.putAt(i, tNnParam);
                mNnForwardCache.putAt(i, FloatCPointer.malloc(mNN[i].forwardCacheSize()));
                mNnBackwardCache.putAt(i, FloatCPointer.malloc(mNN[i].backwardCacheSize()));
            } else {
                DoubleCPointer tFpHyperParam = DoubleCPointer.malloc(mBasis[i].hyperParameterSize());
                fill_(tFpHyperParam, mBasis[i].hyperParameters());
                mFpHyperParam.putAt(i, tFpHyperParam);
                
                DoubleCPointer tFpParam = DoubleCPointer.malloc(mBasis[i].parameterSize());
                fill_(tFpParam, mBasis[i].parameters());
                mFpParam.putAt(i, tFpParam);
                
                DoubleCPointer tNnParam = DoubleCPointer.malloc(mNN[i].parameterSize());
                fill_(tNnParam, mNN[i].parameters());
                mNnParam.putAt(i, tNnParam);
                mNnForwardCache.putAt(i, DoubleCPointer.malloc(mNN[i].forwardCacheSize()));
                mNnBackwardCache.putAt(i, DoubleCPointer.malloc(mNN[i].backwardCacheSize()));
            }
        }
        // 归一化系数读取
        mNormParam = NestedCPointer.malloc(tModelSize);
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
            if (mSinglePrecision) {
                FloatCPointer tNormParam = FloatCPointer.malloc(mBasis[i].size()*2 + 2);
                tNormParam.putAt(0, (float)(aNormMuEng+aRefEng));
                tNormParam.putAt(1, (float)aNormSigmaEng);
                fill_(tNormParam.plus(2), aNormMu);
                fill_(tNormParam.plus(mBasis[i].size()+2), aNormSigma);
                mNormParam.putAt(i, tNormParam);
            } else {
                DoubleCPointer tNormParam = DoubleCPointer.malloc(mBasis[i].size()*2 + 2);
                tNormParam.putAt(0, aNormMuEng+aRefEng);
                tNormParam.putAt(1, aNormSigmaEng);
                fill_(tNormParam.plus(2), aNormMu);
                fill_(tNormParam.plus(mBasis[i].size()+2), aNormSigma);
                mNormParam.putAt(i, tNormParam);
            }
        }
        // 代码生成，先针对相同系数的进行优化合并
        List<List<Integer>> tSwitchListFp = new ArrayList<>(); // [position][type]
        List<List<Integer>> tSwitchListNN = new ArrayList<>();
        for (int type = 1; type <= tModelSize; ++type) {
            final int ti = type-1;
            updateSwitchList_(tSwitchListFp, type, caseList -> mBasis[ti].hasSameGenMap(mBasis[caseList.get(0)-1]));
            updateSwitchList_(tSwitchListNN, type, caseList -> mNN[ti].hasSameGenMap(mNN[caseList.get(0)-1]));
        }
        final Map<String, Object> tGenMap = new LinkedHashMap<>();
        tGenMap.put("[PRECISION]", mSinglePrecision ? "single" : "double");
        tGenMap.put("[FP TYPE]", tSwitchListFp);
        tGenMap.put("[NN TYPE]", tSwitchListNN);
        // 只添加不同的，降低 code gen 的压力
        for (int i = 0; i < tSwitchListFp.size(); ++i) {
            mBasis[tSwitchListFp.get(i).get(0)-1].updateGenMap(tGenMap, i);
        }
        for (int i = 0; i < tSwitchListNN.size(); ++i) {
            mNN[tSwitchListNN.get(i).get(0)-1].updateGenMap(tGenMap, i);
        }
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
        mEngine.setMethodNames(NAME_CAL_ENERGY, NAME_CAL_ENERGYFORCE, NAME_COMPUTE_LAMMPS).compile();
        mCalEnergy = mEngine.findMethod(NAME_CAL_ENERGY);
        mCalEnergyForce = mEngine.findMethod(NAME_CAL_ENERGYFORCE);
        mComputeLammps = mEngine.findMethod(NAME_COMPUTE_LAMMPS);
    }
    public NNAP2(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber, String aPrecision) throws Exception {
        this(OS.WORKING_DIR, JIT_NAME, aModelInfo, aThreadNumber, aPrecision);
    }
    public NNAP2(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber, String aPrecision) throws Exception {
        this(IO.toParentPath(aModelPath), toValidProjectName(IO.toFileName(aModelPath)),
             aModelPath.endsWith(".yaml") || aModelPath.endsWith(".yml") ? IO.yaml2map(aModelPath) : IO.json2map(aModelPath), aThreadNumber, aPrecision);
    }
    public NNAP2(String aModelPath, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws Exception {this(aModelPath, aThreadNumber, null);}
    public NNAP2(Map<?, ?> aModelInfo, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNumber) throws Exception {this(aModelInfo, aThreadNumber, null);}
    public NNAP2(Map<?, ?> aModelInfo) throws Exception {this(aModelInfo, 1);}
    public NNAP2(String aModelPath) throws Exception {this(aModelPath, 1);}
    
    private final static Pattern PROJECT_INVALID_NAME = Pattern.compile("[^a-zA-Z0-9_\\-]");
    private static String toValidProjectName(String aProjectName) {
        aProjectName = aProjectName.replace(".yaml", "").replace(".yml", "").replace(".json", "").replace(".jnn", "").replace(".nn", "");
        aProjectName = PROJECT_INVALID_NAME.matcher(aProjectName).replaceAll("");
        return aProjectName.isEmpty() ? JIT_NAME : aProjectName;
    }
    
    
    private static void fill_(DoubleCPointer rPtr, @Nullable IVector aVec) {
        if (aVec == null) return;
        final int tSize = aVec.size();
        for (int i = 0; i < tSize; ++i) {
            rPtr.putAt(i, aVec.get(i));
        }
    }
    private static void fill_(FloatCPointer rPtr, @Nullable IVector aVec) {
        if (aVec == null) return;
        final int tSize = aVec.size();
        for (int i = 0; i < tSize; ++i) {
            rPtr.putAt(i, (float)aVec.get(i));
        }
    }
    private static void updateSwitchList_(List<List<Integer>> rSwitchList, int aType, IUnaryFullOperator<Boolean, List<Integer>> aChecker) {
        for (List<Integer> tCaseList : rSwitchList) {
            if (aChecker.apply(tCaseList)) {
                tCaseList.add(aType);
                return;
            }
        }
        // 所有现有的 case 都没有，则新增一列
        List<Integer> tCaseList = new ArrayList<>(1);
        tCaseList.add(aType);
        rSwitchList.add(tCaseList);
    }
    
    /**
     * NNAP GEN 语法备忘录：
     * <p>
     * 标记逻辑:
     *   <p>
     *   {@code NNAPGEN}: 通用标识，正文不出现此序列来保证不会被代码生成；替换宏以 NNAPGEN_
     *     开头，从而避免意外的替换
     *   <p>
     *   {@code NNAPGENX}: 循环替换宏标识，会在带有 swich/repeat 块中替换为
     *     i:NNAPGEN，此语法可以保证在嵌套时 NNAPGENXX 自动替换为 i:j:NNAPGEN
     *   <p>
     *   {@code NNAPGENS}: 循环替换变量标识，会在 swich/repeat 块中直接替换为特定变量，其中
     *     NNAPGENS_X 会直接替换为 i，而 swich 中可以使用 NNAPGENS_{swicher}
     *     替换为第一个 case
     *   <p>
     *   {@code NNAPGENO}: 循环替换保护标识，会在带有 swich/repeat 块中替换为
     *     NNAPGEN，从而保护内层循环中的 NNAPGEN 替换为需要的变量
     * <p>
     * 语法逻辑：
     *   <p>
     *   {@code // >>> NNAPGEN}: 替换块开头标识
     *   <p>
     *   {@code // <<< NNAPGEN}: 替换块结尾标识
     *   <p>
     *   {@code // --- NNAPGEN}: 替换块中间标识
     *   <p>
     *   {@code []}: 替换块内特殊参数标识，会进行 gen map 查询
     *   <p>
     *   {@code ()}: 替换块内通用参数标识，一般不进行 gen map 查询
     */
    private static final String MARKER_REMOVE_START = "// >>> NNAPGEN REMOVE";
    private static final String MARKER_REMOVE_END = "// <<< NNAPGEN REMOVE";
    private static final String MARKER_REPEAT_START = "// >>> NNAPGEN REPEAT";
    private static final String MARKER_REPEAT_END = "// <<< NNAPGEN REPEAT";
    private static final String MARKER_SWITCH_START = "// >>> NNAPGEN SWITCH";
    private static final String MARKER_SWITCH_END = "// <<< NNAPGEN SWITCH";
    private static final String MARKER_PICK_START = "// >>> NNAPGEN PICK";
    private static final String MARKER_PICK_CASE = "// --- NNAPGEN PICK:";
    private static final String MARKER_PICK_END = "// <<< NNAPGEN PICK";
    private static final String MARKER_IF_START = "// >>> NNAPGEN IF";
    private static final String MARKER_IF_HAS = "// --- NNAPGEN HAS:";
    private static final String MARKER_IF_ELSE = "// --- NNAPGEN ELSE:";
    private static final String MARKER_IF_END = "// <<< NNAPGEN IF";
    private static final String MARKER_ANY_CASE = "// --- NNAPGEN ";
    
    private static final int STATE_NORMAL = 0, STATE_REMOVE = 1, STATE_REPEAT = 2, STATE_SWITCH = 3, STATE_PICK = 4, STATE_IF = 5;
    
    private static void codeGen_(URL aSourceURL, String aTargetPath, Map<String, Object> aGenMap) throws Exception {
        List<String> tLines;
        try (BufferedReader tReader = IO.toReader(aSourceURL)) {
            tLines = IO.readAllLines(tReader);
        }
        IO.write(aTargetPath, processLines_(tLines, aGenMap));
    }
    @SuppressWarnings("unchecked")
    private static List<String> processLines_(List<String> aLines, Map<String, Object> aGenMap) throws Exception {
        int tState = STATE_NORMAL;
        List<String> rBuf0 = new ArrayList<>(), rBuf1 = new ArrayList<>();
        List<String> rOutLines = new ArrayList<>(aLines.size());
        for (String tLine : aLines) {
            switch(tState) {
            case STATE_NORMAL: {
                switch(tLine.trim()) {
                case MARKER_REMOVE_START: {
                    tState = STATE_REMOVE;
                    break;
                }
                case MARKER_REPEAT_START: {
                    tState = STATE_REPEAT;
                    break;
                }
                case MARKER_SWITCH_START: {
                    tState = STATE_SWITCH;
                    break;
                }
                case MARKER_PICK_START: {
                    tState = STATE_PICK;
                    break;
                }
                case MARKER_IF_START: {
                    tState = STATE_IF;
                    break;
                }
                default: {
                    rOutLines.add(baseReplace_(tLine, aGenMap));
                    break;
                }}
                break;
            }
            case STATE_REMOVE: {
                if (tLine.trim().equals(MARKER_REMOVE_END)) {
                    tState = STATE_NORMAL;
                }
                break;
            }
            case STATE_REPEAT: {
                if (tLine.trim().startsWith(MARKER_REPEAT_END)) {
                    tState = STATE_NORMAL;
                    String tRangeStr = tLine.trim().substring(MARKER_REPEAT_END.length()).trim();
                    List<Integer> tRange = parseRepeatRange_(tRangeStr, aGenMap);
                    rBuf1.clear();
                    for (int i : tRange) {
                        for (String tBufLine : rBuf0) {
                            rBuf1.add(
                                tBufLine.replace("__NNAPGENS_X__", String.valueOf(i))
                                        .replace("NNAPGENX", i+":NNAPGEN")
                                        .replace("NNAPGENO", "NNAPGEN")
                            );
                        }
                    }
                    rBuf0.clear();
                    // 内部只核心逻辑，全部完成后递归处理后续
                    rOutLines.addAll(processLines_(rBuf1, aGenMap));
                } else {
                    rBuf0.add(tLine);
                }
                break;
            }
            case STATE_SWITCH: {
                if (tLine.trim().startsWith(MARKER_SWITCH_END)) {
                    tState = STATE_NORMAL;
                    String[] tArgs = parseSwitchArgs_(tLine);
                    String tSwitcher = tArgs[0];
                    String tKey = tArgs[1];
                    Object tValue = aGenMap.get(tKey);
                    if (tValue==null) throw new IllegalStateException("Missing switch key: "+tKey);
                    List<List<Integer>> tSwitchList = (List<List<Integer>>)tValue;
                    final int tLoop = tSwitchList.size();
                    rBuf1.clear();
                    rBuf1.add("switch ("+tSwitcher+") {");
                    for (int i = 0; i < tLoop; ++i) {
                        List<Integer> tSubList = tSwitchList.get(i);
                        StringBuilder tCases = new StringBuilder();
                        for (int tCase : tSubList) {
                            tCases.append("case ").append(tCase).append(": ");
                        }
                        rBuf1.add(tCases+"{");
                        for (String tBufLine : rBuf0) {
                            rBuf1.add(
                                tBufLine.replace("__NNAPGENS_"+tSwitcher+"__", tSubList.get(0).toString()) // 总是合并到第一个组
                                        .replace("__NNAPGENS_X__", String.valueOf(i))
                                        .replace("NNAPGENX", i+":NNAPGEN")
                                        .replace("NNAPGENO", "NNAPGEN")
                            );
                        }
                        rBuf1.add("break;");
                        rBuf1.add("}");
                    }
                    rBuf0.clear();
                    rBuf1.add("}");
                    // 内部只核心逻辑，全部完成后递归处理后续
                    rOutLines.addAll(processLines_(rBuf1, aGenMap));
                } else {
                    rBuf0.add(tLine);
                }
                break;
            }
            case STATE_PICK: {
                if (tLine.trim().startsWith(MARKER_PICK_END)) {
                    tState = STATE_NORMAL;
                    String tKey = tLine.trim().substring(MARKER_PICK_END.length()).trim();
                    Object tValue = aGenMap.get(tKey);
                    if (tValue==null) throw new IllegalStateException("Missing pick key: "+tKey);
                    rBuf1.clear();
                    boolean tInCase = false;
                    for (String tBufLine : rBuf0) {
                        if (tInCase) {
                            if (tBufLine.trim().startsWith(MARKER_PICK_CASE)) {
                                break;
                            } else {
                                rBuf1.add(tBufLine.replace("NNAPGENO", "NNAPGEN"));
                            }
                        } else {
                            if (tBufLine.trim().startsWith(MARKER_PICK_CASE)) {
                                String tCase = tBufLine.trim().substring(MARKER_PICK_CASE.length()).trim();
                                if (tCase.equals(tValue)) tInCase = true;
                            }
                        }
                    }
                    rBuf0.clear();
                    // 内部只核心逻辑，全部完成后递归处理后续
                    rOutLines.addAll(processLines_(rBuf1, aGenMap));
                } else {
                    rBuf0.add(tLine);
                }
                break;
            }
            case STATE_IF: {
                if (tLine.trim().equals(MARKER_IF_END)) {
                    tState = STATE_NORMAL;
                    rBuf1.clear();
                    boolean tInCase = false;
                    for (String tBufLine : rBuf0) {
                        if (tInCase) {
                            if (tBufLine.trim().startsWith(MARKER_ANY_CASE)) {
                                break;
                            } else {
                                rBuf1.add(tBufLine.replace("NNAPGENO", "NNAPGEN"));
                            }
                        } else {
                            if (tBufLine.trim().startsWith(MARKER_IF_HAS)) {
                                String tKey = tBufLine.trim().substring(MARKER_IF_HAS.length()).trim();
                                if (aGenMap.containsKey(tKey)) tInCase = true;
                            } else
                            if (tBufLine.trim().startsWith(MARKER_IF_ELSE)) {
                                tInCase = true;
                            }
                        }
                    }
                    rBuf0.clear();
                    // 内部只核心逻辑，全部完成后递归处理后续
                    rOutLines.addAll(processLines_(rBuf1, aGenMap));
                } else {
                    rBuf0.add(tLine);
                }
                break;
            }
            default: {
                throw new IllegalStateException();
            }}
        }
        if (tState!=STATE_NORMAL) throw new IllegalStateException();
        return rOutLines;
    }
    
    private static String[] parseSwitchArgs_(String aLine) {
        String tLine = aLine.trim().substring(MARKER_SWITCH_END.length()).trim();
        String[] tArgs = new String[2];
        if (!tLine.startsWith("(")) throw new IllegalArgumentException("invalid switch argument: "+tLine);
        int tSplit = tLine.indexOf(')');
        tArgs[0] = tLine.substring(1, tSplit).trim();
        tArgs[1] = tLine.substring(tSplit+1).trim();
        return tArgs;
    }
    @SuppressWarnings("unchecked")
    private static List<Integer> parseRepeatRange_(String aRangeStr, Map<String, Object> aGenMap) throws Exception {
        @Language("Groovy") String tRangeStr = scriptReplace_(aRangeStr, aGenMap);
        return (List<Integer>)SP.Groovy.runText(tRangeStr);
    }
    private static String scriptReplace_(String aScriptStr, Map<String, Object> aGenMap) {
        int tStart = aScriptStr.indexOf('[');
        if (tStart < 0) return aScriptStr;
        int tEnd = aScriptStr.indexOf(']');
        if (tEnd < 0) throw new IllegalArgumentException("invalid script: "+aScriptStr);
        String tKey = aScriptStr.substring(tStart, tEnd+1);
        Object tValue = aGenMap.get(tKey);
        if (tValue==null) throw new IllegalStateException("Missing script key: "+tKey);
        // 递归实现多个 key 的替换
        return scriptReplace_(aScriptStr.replace(tKey, tValue.toString()), aGenMap);
    }
    
    private static String baseReplace_(String aLine, Map<String, Object> aGenMap) {
        // 简单串联，在没有遇到性能问题前都就这样做好了
        for (Map.Entry<String, Object> tEntry : aGenMap.entrySet()) {
            String tKey = tEntry.getKey();
            if (tKey.startsWith("[") && tKey.endsWith("]")) continue;
            aLine = aLine.replace("__"+tKey+"__", tEntry.getValue().toString());
        }
        return aLine;
    }
    
    @Override public void shutdown() {
        if (mDead) return;
        mDead = true;
        
        for (int i = 0; i < mSymbols.length; ++i) {
            mFpHyperParam.getAt(i).free();
            mFpParam.getAt(i).free();
            mNnParam.getAt(i).free();
            mNormParam.getAt(i).free();
        }
        mInNums.free();
        ((CPointer)mNlDx).free(); ((CPointer)mNlDy).free(); ((CPointer)mNlDz).free();
        mNlType.free();
        mFpParam.free(); mNnParam.free(); mNormParam.free();
        mDataIn.free();
        
        for (int i = 0; i < mSymbols.length; ++i) {
            mNnForwardCache.getAt(i).free();
            mNnBackwardCache.getAt(i).free();
        }
        mOutEng.free();
        ((CPointer)mFpForwardCache).free(); mNnForwardCache.free();
        ((CPointer)mFpBackwardCache).free(); mNnBackwardCache.free();
        mDataOut.free();
        
        mEngine.shutdown();
    }
    @Override public boolean isShutdown() {return mDead;}
    @Override public int threadNumber() {return mThreadNumber;}
    @VisibleForTesting public int nthreads() {return threadNumber();}
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
        final int tTypeNum = atomTypeNumber();
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
        mNlDx.ensureCapacity(tNeiNum);
        mNlDy.ensureCapacity(tNeiNum);
        mNlDz.ensureCapacity(tNeiNum);
        if (mSinglePrecision) {
            ((FloatCPointer)mNlDx).fillD(mNlDxBuf);
            ((FloatCPointer)mNlDy).fillD(mNlDyBuf);
            ((FloatCPointer)mNlDz).fillD(mNlDzBuf);
        } else {
            ((DoubleCPointer)mNlDx).fill(mNlDxBuf);
            ((DoubleCPointer)mNlDy).fill(mNlDyBuf);
            ((DoubleCPointer)mNlDz).fill(mNlDzBuf);
        }
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
            mFpForwardCache.ensureCapacity(mBasis[cType-1].forwardCacheSize(tNeiNum, false));
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
            mDataOut.putAt(1, mFpForwardCache);
            mDataOut.putAt(2, mNnForwardCache.getAt(cType-1));
            // 调用 jit 方法获取结果
            int tCode = mCalEnergy.invoke(mDataIn, mDataOut);
            if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
            double tEng = mSinglePrecision ? ((FloatCPointer)mOutEng).get() : ((DoubleCPointer)mOutEng).get();
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
            mFpForwardCache.ensureCapacity(mBasis[cType-1].forwardCacheSize(tNeiNum, true));
            mFpBackwardCache.ensureCapacity(mBasis[cType-1].backwardCacheSize(tNeiNum, false));
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
            mDataOut.putAt(4, mFpForwardCache);
            mDataOut.putAt(5, mNnForwardCache.getAt(cType-1));
            mDataOut.putAt(6, mFpBackwardCache);
            mDataOut.putAt(7, mNnBackwardCache.getAt(cType-1));
            // 调用 jit 方法获取结果
            int tCode = mCalEnergyForce.invoke(mDataIn, mDataOut);
            if (tCode!=0) throw new IllegalStateException("Exit code: "+tCode);
            double tEng = mSinglePrecision ? ((FloatCPointer)mOutEng).get() : ((DoubleCPointer)mOutEng).get();
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
                final double fx, fy, fz;
                if (mSinglePrecision) {
                    fx = ((FloatCPointer)mGradNlDx).getAt(j);
                    fy = ((FloatCPointer)mGradNlDy).getAt(j);
                    fz = ((FloatCPointer)mGradNlDz).getAt(j);
                } else {
                    fx = ((DoubleCPointer)mGradNlDx).getAt(j);
                    fy = ((DoubleCPointer)mGradNlDy).getAt(j);
                    fz = ((DoubleCPointer)mGradNlDz).getAt(j);
                }
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
    
    private boolean mNlLammpsValid = false;
    private void validNlLammps_() {
        if (mNlLammpsValid) return;
        mNlLammpsValid = true;
        // 合法化近邻列表 size，这里采用简单固定大小（可以保证极限速度并且和 lammps 逻辑一致）
        mNlDx.ensureCapacity(Conf.LAMMPS_NL_MAX);
        mNlDy.ensureCapacity(Conf.LAMMPS_NL_MAX);
        mNlDz.ensureCapacity(Conf.LAMMPS_NL_MAX);
        mNlType.ensureCapacity(Conf.LAMMPS_NL_MAX);
        mNlIdx.ensureCapacity(Conf.LAMMPS_NL_MAX);
        mGradNlDx.ensureCapacity(Conf.LAMMPS_NL_MAX);
        mGradNlDy.ensureCapacity(Conf.LAMMPS_NL_MAX);
        mGradNlDz.ensureCapacity(Conf.LAMMPS_NL_MAX);
        for (int i = 0; i < mSymbols.length; ++i) {
            mFpForwardCache.ensureCapacity(mBasis[i].forwardCacheSize(Conf.LAMMPS_NL_MAX, true));
            mFpBackwardCache.ensureCapacity(mBasis[i].backwardCacheSize(Conf.LAMMPS_NL_MAX, false));
        }
    }
    
    void computeLammps(PairNNAP2 aPair) {
        // 种类的缓存优化
        final int inum = aPair.listInum();
        for (int type = 1; type <= aPair.mTypeNum; ++type) {
            GrowableIntCPointer tList = aPair.mTypeIlistBuf[type];
            tList.ensureCapacity(inum);
            aPair.mTypeIlist.putAt(type, tList);
        }
        // 虽然原则上这里依旧可以在 java 层进行 nlocal 的遍历，并实时更新 nl；
        // 但是考虑到未来登录 cuda 不能使用这种架构，并且可以简化部分实现
        validNlLammps_();
        mInNums.putAt(0, Conf.LAMMPS_NL_MAX);
        mInNums.putAt(1, inum);
        mInNums.putAt(2, aPair.mTypeNum);
        mInNums.putAt(3, aPair.eflagEither()?1:0);
        mInNums.putAt(4, aPair.vflagEither()?1:0);
        mInNums.putAt(5, aPair.eflagAtom()?1:0);
        mInNums.putAt(6, aPair.vflagAtom()?1:0);
        mInNums.putAt(7, aPair.cvflagAtom()?1:0);
        
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
        mDataIn.putAt(12, aPair.listIlist());
        mDataIn.putAt(13, aPair.listNumneigh());
        mDataIn.putAt(14, aPair.listFirstneigh());
        mDataIn.putAt(15, aPair.mCutsq);
        mDataIn.putAt(16, aPair.mLmpType2NNAPType);
        mDataIn.putAt(17, aPair.mTypeIlist);
        mDataIn.putAt(18, aPair.mTypeInum);
        
        mDataOut.putAt(0, aPair.atomF());
        mDataOut.putAt(1, mGradNlDx);
        mDataOut.putAt(2, mGradNlDy);
        mDataOut.putAt(3, mGradNlDz);
        mDataOut.putAt(4, mFpForwardCache);
        mDataOut.putAt(5, mNnForwardCache);
        mDataOut.putAt(6, mFpBackwardCache);
        mDataOut.putAt(7, mNnBackwardCache);
        mDataOut.putAt(8, aPair.engVdwl());
        mDataOut.putAt(9, aPair.eatom());
        mDataOut.putAt(10, aPair.virial());
        mDataOut.putAt(11, aPair.vatom());
        mDataOut.putAt(12, aPair.cvatom());
        
        // 调用 jit 方法计算
        int tCode = mComputeLammps.invoke(mDataIn, mDataOut);
        if (tCode>0) throw new IllegalStateException("Exit code: "+tCode);
        if (tCode<0) throw new IllegalStateException("The number of neighbors ("+(-tCode)+") is greater than "+Conf.LAMMPS_NL_MAX+",\n" +
                                                     "  adjust it through JSE_NNAP_LAMMPS_NL_MAX, like `export JSE_NNAP_LAMMPS_NL_MAX="+tCode+"`");
    }
}
