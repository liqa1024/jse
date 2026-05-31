package jse.jit;

import jse.code.IO;
import jse.code.UT;
import jse.cptr.*;
import jse.gpu.*;
import org.jetbrains.annotations.ApiStatus;

import java.util.*;

/**
 * 针对 C 源码的扫描器，获取标记的需要导出的函数，并进行需要的替换
 * <p>
 * 由于逻辑比较复杂这里单独一个类
 * @author liqa
 */
@ApiStatus.Internal
class SourceScanner {
    class FuncInfo {
        public final String mName;
        public final Map<String, String> mParams;
        public final int mMemCount;
        FuncInfo(String aName, Map<String, String> aParams) {
            mName = aName;
            mParams = aParams;
            // 简单验证类型合法性，计算输入的内存占用
            int tMemCount = 0;
            for (String tTypeStr : mParams.values()) {
                // 简单处理 const
                if (tTypeStr.startsWith("const")) {
                    tTypeStr = tTypeStr.substring(5).trim();
                }
                if (tTypeStr.endsWith("*")) {
                    tMemCount += (int)AnyCPointer.TYPE_SIZE;
                    continue;
                }
                // 简单处理 type map
                String tNewType = mTypeMap.get(tTypeStr);
                if (tNewType!=null) tTypeStr = tNewType;
                switch(tTypeStr) {
                case "int": case "jint": {
                    tMemCount += (int)IntCPointer.TYPE_SIZE;
                    break;
                }
                case "int64_t": case "jlong": {
                    tMemCount += (int)Int64CPointer.TYPE_SIZE;
                    break;
                }
                case "double": case "jdouble": {
                    tMemCount += (int)DoubleCPointer.TYPE_SIZE;
                    break;
                }
                case "float": case "jfloat": {
                    tMemCount += (int)FloatCPointer.TYPE_SIZE;
                    break;
                }
                default: {
                    throw new IllegalArgumentException("Invalid C type: "+tTypeStr);
                }}
            }
            mMemCount = tMemCount;
        }
        @ApiStatus.Internal
        public void parseArgs_(long aPtr, Object... aArgs) {
            if (mParams.size() != aArgs.length) {
                throw new IllegalArgumentException("Size of parameters mismatch: "+mParams.size()+" vs "+aArgs.length);
            }
            long tPtr = aPtr;
            int i = 0;
            for (Map.Entry<String, String> tEntry : mParams.entrySet()) {
                String tKey = tEntry.getKey();
                String tTypeStr = tEntry.getValue();
                // 简单处理 const
                if (tTypeStr.startsWith("const")) {
                    tTypeStr = tTypeStr.substring(5).trim();
                }
                Object tArg = aArgs[i]; ++i;
                if (tTypeStr.endsWith("*")) {
                    if (!(tArg instanceof IPointer)) {
                        invalidType_(tKey, "? extends IPointer", tArg.getClass().getName());
                    }
                    // 对于 NULL 不需要判断
                    if (tArg == CPointer.NULL) {
                        AnyCPointer.set0(tPtr, 0);
                        tPtr = AnyCPointer.next0(tPtr);
                        continue;
                    }
                    String tElseTypeStr = tTypeStr.substring(0, tTypeStr.length()-1).trim();
                    if (tElseTypeStr.endsWith("*")) {
                        // 针对嵌套指针特殊处理，这里反向优先级
                        String tElseTypeStr2 = tElseTypeStr.substring(0, tElseTypeStr.length()-1).trim();
                        String tNewType = mTypeMap.get(tElseTypeStr2);
                        if (tNewType!=null) tElseTypeStr2 = tNewType;
                        if (tArg instanceof NestedDoubleCPointer) {
                            if (!tElseTypeStr2.equals("double") && !tElseTypeStr2.equals("jdouble")) {
                                throw new IllegalArgumentException("Invalid args type for '"+tKey+"' in func '"+mName+"', input expected: double ** (for NestedDoubleCPointer), real: "+tTypeStr);
                            }
                        } else
                        if (tArg instanceof NestedIntCPointer) {
                            if (!tElseTypeStr2.equals("int") && !tElseTypeStr2.equals("jint")) {
                                throw new IllegalArgumentException("Invalid args type for '"+tKey+"' in func '"+mName+"', input expected: int ** (for NestedIntCPointer), real: "+tTypeStr);
                            }
                        } else {
                            checkType_(tKey, AnyCPointer.class, CudaPointer.class, tArg);
                        }
                    } else {
                        // 简单处理 type map
                        String tNewType = mTypeMap.get(tElseTypeStr);
                        if (tNewType!=null) tElseTypeStr = tNewType;
                        switch(tElseTypeStr) {
                        case "int": case "jint": {
                            checkType_(tKey, IntCPointer.class, IntCudaPointer.class, tArg);
                            break;
                        }
                        case "int64_t": case "jlong": {
                            checkType_(tKey, Int64CPointer.class, Int64CudaPointer.class, tArg);
                            break;
                        }
                        case "double": case "jdouble": {
                            checkType_(tKey, DoubleCPointer.class, DoubleCudaPointer.class, tArg);
                            break;
                        }
                        case "float": case "jfloat": {
                            checkType_(tKey, FloatCPointer.class, FloatCudaPointer.class, tArg);
                            break;
                        }
                        case "void": case "char": {
                            checkType_(tKey, CPointer.class, CudaPointer.class, tArg);
                            break;
                        }
                        default: {
                            UT.Code.warning("Unchecked casts: "+tArg.getClass().getName()+" to "+tTypeStr);
                            break;
                        }}
                    }
                    AnyCPointer.set0(tPtr, ((IPointer)tArg).ptr_());
                    tPtr = AnyCPointer.next0(tPtr);
                    continue;
                }
                // 简单处理 type map
                String tNewType = mTypeMap.get(tTypeStr);
                if (tNewType!=null) tTypeStr = tNewType;
                switch(tTypeStr) {
                case "int": case "jint": {
                    checkType_(tKey, Integer.class, tArg);
                    IntCPointer.set0(tPtr, (Integer)tArg);
                    tPtr = IntCPointer.next0(tPtr);
                    break;
                }
                case "int64_t": case "jlong": {
                    checkType_(tKey, Long.class, tArg);
                    Int64CPointer.set0(tPtr, (Long)tArg);
                    tPtr = Int64CPointer.next0(tPtr);
                    break;
                }
                case "double": case "jdouble": {
                    checkType_(tKey, Number.class, tArg); // 浮点数使用更加粗略的判断
                    DoubleCPointer.set0(tPtr, ((Number)tArg).doubleValue());
                    tPtr = DoubleCPointer.next0(tPtr);
                    break;
                }
                case "float": case "jfloat": {
                    checkType_(tKey, Number.class, tArg); // 浮点数使用更加粗略的判断
                    FloatCPointer.set0(tPtr, ((Number)tArg).floatValue());
                    tPtr = FloatCPointer.next0(tPtr);
                    break;
                }
                default: {
                    throw new IllegalStateException();
                }}
            }
        }
        private void checkType_(String aKey, Class<?> aClazz, Object aArg) {
            if (!aClazz.isInstance(aArg)) {
                invalidType_(aKey, aClazz.getName(), aArg.getClass().getName());
            }
        }
        private void checkType_(String aKey, Class<?> aClazz, Class<?> aClazzCuda, Object aArg) {
            if (!aClazz.isInstance(aArg) && !aClazzCuda.isInstance(aArg)) {
                invalidType_(aKey, aClazz.getName(), aArg.getClass().getName());
            }
        }
        private void invalidType_(String aKey, String aExpected, String aInput) {
            throw new IllegalArgumentException("Invalid args type for '"+aKey+"' in func '"+mName+"', expected: "+aExpected+", input: "+aInput);
        }
        
        void write(String aBody, StringBuilder rBuf) {
            rBuf.append("JSE_PLUGINEXPORT int JSE_PLUGINCALL ").append(mName).append("(void *__jsefunc_data__) {\n");
            rBuf.append("void *__jsefunc_ptr__ = __jsefunc_data__;\n");
            for (Map.Entry<String, String> tEntry : mParams.entrySet()) {
                String tKey = tEntry.getKey();
                String tType = tEntry.getValue();
                // 简单处理 const
                String tTypeRaw = tType.startsWith("const") ? tType.substring(5).trim() : tType;
                rBuf.append(tType).append(" ").append(tKey).append(" = *(").append(tType).append("*)__jsefunc_ptr__;");
                rBuf.append(" __jsefunc_ptr__ = (").append(tTypeRaw).append("*)__jsefunc_ptr__ + 1;\n");
            }
            rBuf.append(aBody).append("\n}");
        }
    }
    private final Map<String, FuncInfo> mFuncs = new LinkedHashMap<>();
    private final Map<String, String> mTypeMap = new HashMap<>();
    private final String mMarker;
    private final int mMarkerLen;
    SourceScanner(String aMarker) {
        mMarker = aMarker;
        mMarkerLen = mMarker.length();
    }
    public void addTypeMap(String aSrcType, String aTargetType) {
        mTypeMap.put(aSrcType, aTargetType);
    }
    
    public Map<String, FuncInfo> funcs() {
        return mFuncs;
    }
    public String apply(String aSrc) {
        StringBuilder rBuf = new StringBuilder();
        final int tEnd = aSrc.length();
        int tIdx = 0;
        
        while (tIdx < tEnd) {
            int tIdx2 = findMarker(aSrc, tIdx, tEnd);
            rBuf.append(aSrc, tIdx, tIdx2);
            tIdx = tIdx2;
            if (tIdx >= tEnd) return rBuf.toString();
            tIdx += mMarkerLen;
            if (tIdx >= tEnd) {
                throw new IllegalArgumentException("Incomplete marker function: '"+subSrc(aSrc, tIdx, tEnd)+"'");
            }
            // 中间一定要有一个空格
            if (!Character.isWhitespace(aSrc.charAt(tIdx))) {
                throw new IllegalArgumentException("Invalid marker formular: '"+subSrc(aSrc, tIdx-mMarkerLen, tEnd)+"'");
            }
            ++tIdx;
            tIdx = skipWhitespace(aSrc, tIdx, tEnd);
            // 认为后面就是返回值，并且一定要求是 int
            if (!aSrc.startsWith("int", tIdx)) {
                throw new IllegalArgumentException("Invalid marker function return type: '"+subSrc(aSrc, tIdx, tEnd)+"'");
            }
            tIdx += 3;
            if (tIdx >= tEnd) {
                throw new IllegalArgumentException("Incomplete marker function: '"+subSrc(aSrc, tIdx, tEnd)+"'");
            }
            // 中间一定要有一个空格
            if (!Character.isWhitespace(aSrc.charAt(tIdx))) {
                throw new IllegalArgumentException("Invalid function formular: '"+subSrc(aSrc, tIdx-3, tEnd)+"'");
            }
            ++tIdx;
            tIdx = skipWhitespace(aSrc, tIdx, tEnd);
            if (tIdx >= tEnd) {
                throw new IllegalArgumentException("Incomplete marker function: '"+subSrc(aSrc, tIdx, tEnd)+"'");
            }
            // 获取函数名称
            tIdx2 = findIdentifierEnd(aSrc, tIdx, tEnd);
            String tName = aSrc.substring(tIdx, tIdx2);
            tIdx = skipWhitespace(aSrc, tIdx2, tEnd);
            if (tIdx >= tEnd) {
                throw new IllegalArgumentException("Incomplete marker function: '"+subSrc(aSrc, tIdx, tEnd)+"'");
            }
            // 读取参数
            Map<String, String> tParam = new LinkedHashMap<>();
            tIdx = parseParameterList(aSrc, tIdx, tEnd, tParam);
            // 读取函数体
            tIdx = skipWhitespace(aSrc, tIdx, tEnd);
            if (tIdx >= tEnd) {
                throw new IllegalArgumentException("Incomplete marker function: '"+subSrc(aSrc, tIdx, tEnd)+"'");
            }
            tIdx2 = findClosureEnd(aSrc, tIdx, tEnd);
            String tBody = aSrc.substring(tIdx+1, tIdx2);
            tIdx = tIdx2+1;
            // 创建函数，并进行自定义修改后写入
            FuncInfo tFunc = new FuncInfo(tName, tParam);
            mFuncs.put(tName, tFunc);
            tFunc.write(tBody, rBuf);
        }
        return rBuf.toString();
    }
    
    
    
    private int findClosureEnd(String aSrc, int aFrom, int aTo) {
        int tIdx = aFrom;
        if (tIdx >= aTo) {
            throw new IllegalArgumentException("Unexpected EOF: '"+subSrc(aSrc, tIdx, aTo)+"'");
        }
        char tFirst = aSrc.charAt(tIdx);
        if (tFirst != '{') {
            throw new IllegalArgumentException("Expected `{`: '"+subSrc(aSrc, tIdx, aTo)+"'");
        }
        ++tIdx;
        int depth = 1;
        while (tIdx < aTo) {
            char c = aSrc.charAt(tIdx);
            // 还需要注意忽略掉中间注释里和字符串里的花括号
            if (aSrc.startsWith("//", tIdx)) {
                tIdx = skipLine(aSrc, tIdx, aTo);
                continue;
            }
            if (aSrc.startsWith("/*", tIdx)){
                tIdx = skipBlockComment(aSrc, tIdx, aTo);
                continue;
            }
            // 字符串内注意跳过
            if (c == '"') {
                tIdx = skipString(aSrc, tIdx, aTo);
                continue;
            }
            if (c == '\'') {
                tIdx = skipCharLiteral(aSrc, tIdx, aTo);
                continue;
            }
            if (c == '{') {
                ++depth;
                ++tIdx;
                continue;
            }
            if (c == '}') {
                --depth;
                if (depth == 0) {
                    return tIdx;
                }
                ++tIdx;
                continue;
            }
            ++tIdx;
        }
        throw new IllegalArgumentException("Unclosed function body("+depth+"): '"+subSrc(aSrc, tIdx, aTo)+"'");
    }
    
    private int parseParameterList(String aSrc, int aFrom, int aTo, Map<String, String> rParam) {
        int tIdx = aFrom;
        if (tIdx >= aTo) {
            throw new IllegalArgumentException("Unexpected EOF: '"+subSrc(aSrc, tIdx, aTo)+"'");
        }
        char tFirst = aSrc.charAt(tIdx);
        if (tFirst != '(') {
            throw new IllegalArgumentException("Expected `(`: '"+subSrc(aSrc, tIdx, aTo)+"'");
        }
        ++tIdx;
        int tStart = tIdx;
        int tEnd = -1;
        // 定位到 )
        while (tIdx < aTo) {
            char c = aSrc.charAt(tIdx);
            if (c == ')') {
                tEnd = tIdx;
                break;
            }
            ++tIdx;
        }
        if (tEnd < 0) {
            throw new IllegalArgumentException("Expected `)`: '"+subSrc(aSrc, tIdx, aTo)+"'");
        }
        ++tIdx;
        // 直接走完整 String 处理
        String tParamStr = aSrc.substring(tStart, tEnd);
        String[] tTokens = IO.Text.splitComma(tParamStr);
        // 反向来读取 identifier 方式获取名称
        for (String token : tTokens) {
            token = token.trim();
            int tKeyStart = 0;
            int tKeyEnd = token.length();
            int i = tKeyEnd - 1;
            while (i >= 0) {
                char c = token.charAt(i);
                if (Character.isLetterOrDigit(c) || c=='_') {
                    --i;
                } else {
                    tKeyStart = i+1;
                    break;
                }
            }
            if (tKeyStart <= 0) {
                throw new IllegalArgumentException("Invalid parameter token: '"+token+"'");
            }
            rParam.put(token.substring(tKeyStart, tKeyEnd), token.substring(0, tKeyStart).trim());
        }
        return tIdx;
    }
    
    private int findIdentifierEnd(String aSrc, int aFrom, int aTo) {
        int tIdx = aFrom;
        if (tIdx >= aTo) {
            throw new IllegalArgumentException("Unexpected EOF: '"+subSrc(aSrc, tIdx, aTo)+"'");
        }
        char tFirst = aSrc.charAt(tIdx);
        if (!(Character.isLetter(tFirst) || tFirst=='_')) {
            throw new IllegalArgumentException("Invalid identifier: '"+subSrc(aSrc, tIdx, aTo)+"'");
        }
        ++tIdx;
        while (tIdx < aTo) {
            char c = aSrc.charAt(tIdx);
            if (Character.isLetterOrDigit(c) || c=='_') {
                ++tIdx;
            } else {
                break;
            }
        }
        return tIdx;
    }
    
    private int findMarker(String aSrc, int aFrom, int aTo) {
        int tIdx = aFrom;
        boolean tValid = true;
        while (tIdx < aTo) {
            char c = aSrc.charAt(tIdx);
            if (Character.isWhitespace(c)) {
                ++tIdx;
                tIdx = skipWhitespace(aSrc, tIdx, aTo);
                tValid = true;
                continue;
            }
            if (aSrc.startsWith("//", tIdx)) {
                tIdx = skipLine(aSrc, tIdx, aTo);
                tValid = true;
                continue;
            }
            if (aSrc.startsWith("/*", tIdx)){
                tIdx = skipBlockComment(aSrc, tIdx, aTo);
                tValid = true;
                continue;
            }
            if (c == '#') {
                tIdx = skipLine(aSrc, tIdx, aTo); // 不考虑反斜杠换行的 define
                tValid = true;
                continue;
            }
            // 简单考虑 marker 以合理的方式开头出现，不考虑在语义中间的情况
            if (tValid && aSrc.startsWith(mMarker, tIdx)) {
                return tIdx;
            } else {
                ++tIdx;
                tValid = false;
            }
        }
        return tIdx;
    }
    
    private int skipWhitespace(String aSrc, int aFrom, int aTo) {
        int tIdx = aFrom;
        while (tIdx < aTo) {
            char c = aSrc.charAt(tIdx);
            if (!Character.isWhitespace(c)) {
                return tIdx;
            }
            ++tIdx;
        }
        return tIdx;
    }
    private int skipLine(String aSrc, int aFrom, int aTo) {
        int tIdx = aFrom;
        while (tIdx < aTo) {
            char c = aSrc.charAt(tIdx);
            ++tIdx;
            if (c == '\n') {
                return tIdx;
            }
        }
        // 没有换行不算错误
        return tIdx;
    }
    private int skipBlockComment(String aSrc, int aFrom, int aTo) {
        int tIdx = aFrom;
        tIdx += 2;
        while (tIdx < aTo) {
            if (aSrc.startsWith("*/", tIdx)) {
                tIdx += 2;
                return tIdx;
            }
            ++tIdx;
        }
        throw new IllegalArgumentException("Unclosed block comment: '"+subSrc(aSrc, aFrom, aTo)+"'");
    }
    
    private int skipString(String aSrc, int aFrom, int aTo) {
        int tIdx = aFrom;
        ++tIdx;
        while (tIdx < aTo) {
            char c = aSrc.charAt(tIdx);
            if (c == '\\') {
                tIdx += 2;
            } else
            if (c == '"') {
                ++tIdx;
                return tIdx;
            } else {
                ++tIdx;
            }
        }
        throw new IllegalArgumentException("Unclosed string: '"+subSrc(aSrc, aFrom, aTo)+"'");
    }
    private int skipCharLiteral(String aSrc, int aFrom, int aTo) {
        int tIdx = aFrom;
        ++tIdx;
        while (tIdx < aTo) {
            char c = aSrc.charAt(tIdx);
            if (c == '\\') {
                tIdx += 2;
            } else
            if (c == '\'') {
                ++tIdx;
                return tIdx;
            } else {
                ++tIdx;
            }
        }
        throw new IllegalArgumentException("Unclosed char literal: '"+subSrc(aSrc, aFrom, aTo)+"'");
    }
    
    private String subSrc(String aSrc, int aFrom, int aTo) {
        if (aFrom >= aTo) aFrom = Math.max(0, aFrom-12);
        int tEnd = aFrom+12;
        if (tEnd > aTo) return aSrc.substring(aFrom, aTo);
        return aSrc.substring(aFrom, tEnd)+"...";
    }
}
