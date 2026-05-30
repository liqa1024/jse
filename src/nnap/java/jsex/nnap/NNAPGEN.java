package jsex.nnap;

import jse.clib.Compiler;
import jse.code.IO;
import jse.code.OS;
import jse.code.SP;
import jse.code.UT;
import jse.code.functional.IUnaryFullOperator;
import jse.gpu.CudaJIT;
import jse.jit.IJITEngine;
import jse.jit.SimpleJIT;
import jsex.nnap.basis.Basis;
import jsex.nnap.nn.NeuralNetwork;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static jse.code.CS.VERSION_NUMBER;
import static jse.code.Conf.VERSION_MASK;
import static jse.code.OS.JAVA_HOME;

/**
 * 独立出来的 NNAP 代码生成类
 * <p>
 * 语法备忘录：
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
 *
 * @author liqa
 */
class NNAPGEN {
    
    private static final String JIT_NAME = "nnapjit";
    private final static String INTERFACE_NAME = "nnap_interface.cpp";
    private final static String INTERFACE_HEAD_NAME = "nnap_interface.h";
    private final static String[] SRC_NAME = {
          "nnap_util.hpp"
        , "nnap_main.hpp"
        , "nn_FeedForward.hpp"
        , "basis_Chebyshev.hpp"
        , "basis_ChebyshevUtil.hpp"
        , "basis_SphericalChebyshev.hpp"
        , "basis_SphericalUtil.hpp"
        , "basis_SphericalUtil0.hpp"
    };
    
    private final Basis[] mBasis;
    private final NeuralNetwork[] mNN;
    private final String mLibDir, mProjectName;
    
    NNAPGEN(@Nullable String aLibDir, @Nullable String aProjectName, Basis[] aBasis, NeuralNetwork[] aNN) {
        mLibDir = aLibDir!=null ? aLibDir : OS.WORKING_DIR;
        mProjectName = aProjectName!=null ? aProjectName : JIT_NAME;
        mBasis = aBasis;
        mNN = aNN;
    }
    
    private Map<String, Object> initGenMap_() {
        final int tNumTypes = mBasis.length;
        Map<String, Object> rGenMap = new LinkedHashMap<>();
        // 一些公用参数
        rGenMap.put("NNAPGEN_NTYPES", tNumTypes);
        // 代码生成，先针对相同系数的进行优化合并
        List<List<Integer>> tSwitchListFp = new ArrayList<>(); // [position][type]
        List<List<Integer>> tSwitchListNN = new ArrayList<>();
        List<List<Integer>> tSwitchListFpNN = new ArrayList<>();
        for (int type = 1; type <= tNumTypes; ++type) {
            final int ti = type-1;
            updateSwitchList_(tSwitchListFp, type, caseList -> mBasis[ti].hasSameGenMap(mBasis[caseList.get(0)-1]));
            updateSwitchList_(tSwitchListNN, type, caseList -> mNN[ti].hasSameGenMap(mNN[caseList.get(0)-1]));
            updateSwitchList_(tSwitchListFpNN, type, caseList -> mBasis[ti].hasSameGenMap(mBasis[caseList.get(0)-1]) && mNN[ti].hasSameGenMap(mNN[caseList.get(0)-1]));
        }
        rGenMap.put("[FP TYPE]", tSwitchListFp);
        rGenMap.put("[NN TYPE]", tSwitchListNN);
        rGenMap.put("[FP NN TYPE]", tSwitchListFpNN);
        // 只添加不同的，降低 code gen 的压力
        int tGenIdx = 0;
        for (List<Integer> tSubList : tSwitchListFpNN) {
            mBasis[tSubList.get(0)-1].updateGenMap(rGenMap, tGenIdx);
            mNN[tSubList.get(0)-1].updateGenMap(rGenMap, tGenIdx);
            tGenIdx += tSubList.size();
        }
        return rGenMap;
    }
    
    @SuppressWarnings("SameParameterValue")
    IJITEngine initEngine(boolean aSinglePrecision) throws Exception {
        Map<String, Object> rGenMap = initGenMap_();
        rGenMap.put("[PRECISION]", aSinglePrecision ? "single" : "double");
        rGenMap.put("[ARCH]", "cpu");
        String tUniqueID = UT.Code.uniqueID(OS.OS_NAME, Compiler.EXE_PATH, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, NNAP.VERSION,
                                            rGenMap, NNAP.Conf.OPTIM_LEVEL, NNAP.Conf.CMAKE_CXX_COMPILER, NNAP.Conf.CMAKE_CXX_FLAGS, NNAP.Conf.CMAKE_SETTING);
        return SimpleJIT.engine()
            .setCmakeCxxCompiler(NNAP.Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(NNAP.Conf.CMAKE_CXX_FLAGS)
            .setCmakeSettings(NNAP.Conf.CMAKE_SETTING).setOptimLevel(NNAP.Conf.OPTIM_LEVEL)
            .addTypeMap("JSE_NNAP::flt_t", aSinglePrecision?"float":"double")
            .setLibDir(mLibDir).setProjectName(mProjectName+"_"+tUniqueID)
            .setSrc(codeGenStr_(IO.getResource("nep/src/"+INTERFACE_NAME), rGenMap)).setNoExtern()
            .setSrcDirIniter((wd, engine) -> {
                for (String tName : SRC_NAME) {
                    codeGen_(IO.getResource("nnap/src/"+tName), wd+tName, rGenMap);
                }
                // 其余操作使用 jit 通用操作，确保 project name 同步
                engine.writeCmakeFile(wd, INTERFACE_NAME);
                engine.writeHeadFile(wd, INTERFACE_HEAD_NAME);
                engine.writeSrcFile(wd, INTERFACE_NAME, INTERFACE_HEAD_NAME);
                return wd;
            });
    }
    @SuppressWarnings("SameParameterValue")
    IJITEngine initEngineCuda() throws Exception {
        Map<String, Object> rGenMap = initGenMap_();
        rGenMap.put("NNAPGEN_CUDA_BLOCKSIZE", NNAP_cuda.Conf.CUDA_BLOCKSIZE);
        rGenMap.put("[PRECISION]", "single");
        rGenMap.put("[ARCH]", "cuda");
        String tUniqueID = UT.Code.uniqueID(OS.OS_NAME, Compiler.EXE_PATH, JAVA_HOME, VERSION_NUMBER, VERSION_MASK, NNAP.VERSION,
                                            rGenMap, NNAP_cuda.Conf.OPTIM_LEVEL, NNAP_cuda.Conf.CMAKE_CXX_COMPILER, NNAP_cuda.Conf.CMAKE_CXX_FLAGS, NNAP_cuda.Conf.CMAKE_CUDA_COMPILER, NNAP_cuda.Conf.CMAKE_CUDA_FLAGS, NNAP_cuda.Conf.CMAKE_SETTING);
        return CudaJIT.engine()
            .setCmakeCudaCompiler(NNAP_cuda.Conf.CMAKE_CUDA_COMPILER).setCmakeCudaFlags(NNAP_cuda.Conf.CMAKE_CUDA_FLAGS)
            .setCmakeCxxCompiler(NNAP_cuda.Conf.CMAKE_CXX_COMPILER).setCmakeCxxFlags(NNAP_cuda.Conf.CMAKE_CXX_FLAGS)
            .setCmakeSettings(NNAP_cuda.Conf.CMAKE_SETTING).setOptimLevel(NNAP_cuda.Conf.OPTIM_LEVEL)
            .addTypeMap("JSE_NNAP::flt_t", "float")
            .setLibDir(mLibDir).setProjectName(mProjectName+"_"+tUniqueID)
            .setSrc(codeGenStr_(IO.getResource("nep/src/nnap_interface_cuda.cu"), rGenMap)).setNoExtern()
            .setSrcDirIniter((wd, engine) -> {
                for (String tName : SRC_NAME) {
                    codeGen_(IO.getResource("nnap/src/"+tName), wd+tName, rGenMap);
                }
                // 其余操作使用 jit 通用操作，确保 project name 同步
                engine.writeCmakeFile(wd, "nnap_interface_cuda.cu");
                engine.writeHeadFile(wd, "nnap_interface_cuda.h");
                engine.writeSrcFile(wd, "nnap_interface_cuda.cu", "nnap_interface_cuda.h");
                return wd;
            });
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
    private static String codeGenStr_(URL aSourceURL, Map<String, Object> aGenMap) throws Exception {
        List<String> tLines;
        try (BufferedReader tReader = IO.toReader(aSourceURL)) {
            tLines = IO.readAllLines(tReader);
        }
        return String.join("\n", processLines_(tLines, aGenMap));
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
                                    .replace("__NNAPGENS_XPP__", String.valueOf(i+1))
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
                    rBuf1.clear();
                    rBuf1.add("switch ("+tSwitcher+") {");
                    int tGenIdx = 0;
                    for (List<Integer> tSubList : tSwitchList) {
                        StringBuilder tCases = new StringBuilder();
                        for (int tCase : tSubList) {
                            tCases.append("case ").append(tCase).append(": ");
                        }
                        rBuf1.add(tCases+"{");
                        for (String tBufLine : rBuf0) {
                            rBuf1.add(
                                tBufLine.replace("__NNAPGENS_"+tSwitcher+"__", tSubList.get(0).toString()) // 总是合并到第一个组
                                    .replace("__NNAPGENS_X__", String.valueOf(tGenIdx))
                                    .replace("__NNAPGENS_XPP__", String.valueOf(tGenIdx+1))
                                    .replace("NNAPGENX", tGenIdx+":NNAPGEN")
                                    .replace("NNAPGENO", "NNAPGEN")
                            );
                        }
                        rBuf1.add("break;");
                        rBuf1.add("}");
                        tGenIdx += tSubList.size();
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
        return (List<Integer>) SP.Groovy.runText(tRangeStr);
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
}
