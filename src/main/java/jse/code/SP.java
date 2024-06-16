package jse.code;

import com.google.common.collect.ImmutableList;
import groovy.lang.*;
import groovy.transform.ThreadInterrupt;
import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import groovy.transform.stc.SimpleType;
import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;
import io.github.spencerpark.jupyter.kernel.magic.CellMagicParseContext;
import io.github.spencerpark.jupyter.kernel.magic.MagicParser;
import io.github.spencerpark.jupyter.kernel.util.CharPredicate;
import io.github.spencerpark.jupyter.kernel.util.SimpleAutoCompleter;
import io.github.spencerpark.jupyter.kernel.util.StringSearch;
import io.github.spencerpark.jupyter.messages.Header;
import jep.JepConfig;
import jep.JepException;
import jep.python.PyObject;
import jse.Main;
import jse.atom.AbstractAtoms;
import jse.atom.Structures;
import jse.clib.JNIUtil;
import jse.clib.MiMalloc;
import jse.code.collection.AbstractCollections;
import jse.code.collection.ArrayLists;
import jse.code.collection.Iterables;
import jse.code.collection.NewCollections;
import jse.code.functional.IBinaryFullOperator;
import jse.code.functional.ITernaryConsumer;
import jse.io.IOFiles;
import jse.io.InFiles;
import jse.math.ComplexDouble;
import jse.math.MathEX;
import jse.math.function.Func1;
import jse.math.matrix.Matrices;
import jse.math.table.Tables;
import jse.math.vector.Vectors;
import jse.parallel.ParforThreadPool;
import jse.plot.IPlotter;
import jse.plot.Plotters;
import org.apache.groovy.groovysh.Groovysh;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.tools.shell.IO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Range;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static jse.code.OS.EXEC;
import static jse.code.OS.JAR_DIR;
import static jse.code.CS.*;
import static jse.code.Conf.*;

/**
 * @author liqa
 * <p> 运行脚本（script）的通用类，目前支持运行 Groovy 脚本和 Python 脚本 </p>
 * <p> 为了方便调用这里使用纯静态的类来实现 </p>
 * <p> 根据底层实现，所有方法都是线程不安全的，这里不加上 synchronized 来避免死锁 </p>
 * <p> 虽然经过测试一般情况不会发生死锁，但在尝试中断执行线程时会失败（具体原因未知） </p>
 */
@SuppressWarnings("UnusedReturnValue")
public class SP {
    private SP() {}
    
    private final static String GROOVY_SP_DIR = "script/groovy/";
    private final static String PYTHON_SP_DIR = "script/python/";
    /** groovy 库的路径，这里采用 jar 包所在的绝对路径 */
    private final static String GROOVY_LIB_DIR = JAR_DIR+"groovy/";
    private final static String JAR_LIB_DIR = JAR_DIR+"jar/";
    private final static List<String> JAR_LIB_PATHS;
    /** python 离线包的路径以及 python 库的路径，这里采用 jar 包所在的绝对路径 */
    private final static String PYTHON_PKG_DIR = JAR_DIR+".pypkg/";
    private final static String PYTHON_LIB_DIR = JAR_DIR+"python/";
    
    static {
        JAR_LIB_PATHS = new ArrayList<>();
        try {
            if (UT.IO.isDir(JAR_LIB_DIR)) {
                for (String tName : UT.IO.list(JAR_LIB_DIR)) if (tName.endsWith(".jar")) {
                    JAR_LIB_PATHS.add(UT.IO.toAbsolutePath(JAR_LIB_DIR+tName));
                }
            }
            // 增加外置 jar 的库的路径
            for (String tJarExlibDir : JAR_EXLIB_DIRS) if (UT.IO.isDir(tJarExlibDir)) {
                tJarExlibDir = UT.IO.toInternalValidDir(tJarExlibDir);
                for (String tName : UT.IO.list(tJarExlibDir)) if (tName.endsWith(".jar")) {
                    JAR_LIB_PATHS.add(UT.IO.toAbsolutePath(tJarExlibDir+tName));
                }
            }
        } catch (IOException e) {
            e.printStackTrace(System.err);
        }
    }
    
    
    /**
     * Jupyter Kernel for JSE，
     * 基于 <a href="https://github.com/SpencerPark/jupyter-jvm-basekernel">
     * SpencerPark / jupyter-jvm-basekernel </a> 实现
     * @author liqa
     */
    public static class JupyterKernel extends BaseKernel {
        private static final SimpleAutoCompleter GROOVY_AUTO_COMPLETER = SimpleAutoCompleter.builder()
            .preferLong()
            //Keywords from https://groovy-lang.org/syntax.html
            .withKeywords("abstract", "assert", "break", "case")
            .withKeywords("catch", "class", "continue")
            .withKeywords("def", "default", "do", "else")
            .withKeywords("enum", "extends", "final", "finally")
            .withKeywords("for", "if", "implements")
            .withKeywords("import", "instanceof", "interface", "native")
            .withKeywords("new", "null", "non-sealed", "package")
            .withKeywords("public", "protected", "private", "return")
            .withKeywords("static", "super", "switch")
            .withKeywords("synchronized", "this", "throw")
            .withKeywords("throws", "transient", "try", "while")
            .withKeywords("as", "in", "permits", "record")
            .withKeywords("sealed", "trait", "var", "yields")
            .withKeywords("true", "false", "boolean")
            .withKeywords("char", "byte", "short", "int")
            .withKeywords("long", "float", "double")
            .build();
        private static final SimpleAutoCompleter PYTHON_AUTO_COMPLETER = SimpleAutoCompleter.builder()
            .preferLong()
            //Keywords from `keyword.kwlist`
            .withKeywords("and", "as", "assert", "break", "class", "continue")
            .withKeywords("def", "del", "elif", "else", "except", "finally")
            .withKeywords("for", "from", "False", "global", "if", "import")
            .withKeywords("in", "is", "lambda", "nonlocal", "not", "None")
            .withKeywords("or", "pass", "raise", "return", "try", "True")
            .withKeywords("while", "with", "yield")
            .build();
        private static final CharPredicate ID_CHAR = CharPredicate.builder()
            .inRange('a', 'z')
            .inRange('A', 'Z')
            .inRange('0', '9')
            .match('_')
            .build();
        private static String toDisplayString(@Nullable Object aObj) {
            if (aObj instanceof String) {
                return "'"+aObj+"'";
            } else
            if (aObj instanceof GString) {
                return "\""+aObj+"\"";
            } else
            if (aObj instanceof Iterable) {
                StringBuilder rBuilder = new StringBuilder();
                rBuilder.append("[");
                Iterator<?> it = ((Iterable<?>)aObj).iterator();
                if (it.hasNext()) rBuilder.append(toDisplayString(it.next()));
                while (it.hasNext()) {
                    rBuilder.append(", ");
                    rBuilder.append(toDisplayString(it.next()));
                }
                rBuilder.append("]");
                return rBuilder.toString();
            } else
            if (aObj instanceof Map) {
                return toDisplayString(((Map<?, ?>)aObj).entrySet());
            } else
            if (aObj instanceof Map.Entry) {
                Map.Entry<?, ?> tEntry = (Map.Entry<?, ?>)aObj;
                // 使用可以直接粘贴到 groovy 的格式
                return toDisplayString(tEntry.getKey())+":"+toDisplayString(tEntry.getValue());
            } else
            if (aObj instanceof Object[]) {
                return toDisplayString(AbstractCollections.from((Object[])aObj));
            } else {
                return String.valueOf(aObj);
            }
        }
        
        private final LanguageInfo mLanguageInfo;
        private final String mBanner;
        private final List<LanguageInfo.Help> mHelpLinks;
        private final MagicParser mMagicParser;
        public JupyterKernel() {
            super(StandardCharsets.UTF_8);
            mLanguageInfo = new LanguageInfo.Builder("groovy")
                .version(GroovySystem.getVersion())
//                .mimetype("text/x-groovy")
                .fileExtension(".groovy")
                .pygments("groovy")
                .codemirror("groovy")
                .build();
            mBanner = "jse "+VERSION+String.format(" (java: %s, groovy: %s, jep: %s)", System.getProperty("java.version"), GroovySystem.getVersion(), Python.JEP_VERSION)+"\n"
                +"Protocol v"+Header.PROTOCOL_VERISON+" implementation by "+KERNEL_META.getOrDefault("project", "UNKNOWN")+" "+KERNEL_META.getOrDefault("version", "UNKNOWN");
            mHelpLinks = ImmutableList.of(
                new LanguageInfo.Help("JSE homepage", "https://github.com/CHanzyLazer/jse"),
                new LanguageInfo.Help("Groovy tutorial", "https://groovy-lang.org/learn.html"));
            mMagicParser = new MagicParser();
        }
        @Override public LanguageInfo getLanguageInfo() {return mLanguageInfo;}
        @Override public String getBanner() {return mBanner;}
        @Override public List<LanguageInfo.Help> getHelpLinks() {return mHelpLinks;}
        
        private final static String MAGIC_INFO =
            "Available line magics:\n" +
            "%lsmagic\n" +
            "\n" +
            "Available cell magics:\n" +
            "%%python  %%groovy  %%writefile";
        
        private volatile Thread mEvalThread = null;
        @Override public DisplayData eval(String expr) throws Exception {
            // 先处理 cell magics
            @Nullable CellMagicParseContext tCellCTX = mMagicParser.parseCellMagic(expr);
            boolean tIsPython = false;
            if (tCellCTX != null) {
                String tCellName = tCellCTX.getMagicCall().getName();
                switch(tCellName) {
                case "python": {tIsPython = true; break;}
                case "groovy": {break;}
                case "writefile": {
                    List<String> tArgs = tCellCTX.getMagicCall().getArgs();
                    if (tArgs.isEmpty()) throw new IllegalArgumentException("Magic `%%writefile` Must have at least one argument.");
                    String tPath = tArgs.get(0);
                    UT.IO.write(tPath, tCellCTX.getMagicCall().getBody());
                    return new DisplayData("Cell body has been written to '"+tPath+"' ("+UT.IO.toAbsolutePath(tPath)+")");
                }
                default: {
                    throw new IllegalArgumentException("Invalid cell magic: "+tCellName+"\n\n"+MAGIC_INFO);
                }}
                expr = tCellCTX.getMagicCall().getBody();
            }
            // 再处理 line magics
            expr = mMagicParser.transformLineMagics(expr, ctx -> {
                String tLineName = ctx.getMagicCall().getName();
                if (tLineName.equals("lsmagic")) {
                    display(new DisplayData(MAGIC_INFO));
                } else {
                    throw new IllegalArgumentException("Invalid line magic: "+tLineName+"\n\n"+MAGIC_INFO);
                }
                return ""; // just replace to empty string
            });
            if (expr.isEmpty()) return null;
            Object tOut;
            // 需要保持线程一致，从而保证各种运算正常
            mEvalThread = Thread.currentThread();
            // 即使 python 原本就不支持中断，这里还是提供一样的逻辑
            try {
                // 根据 tIsPython 决定运行器
                if (tIsPython) {
                    Python.exec(expr);
                    tOut = null; // jep 限制目前没有方法获得最后的输出
                } else {
                    tOut = Groovy.eval(expr);
                }
            } finally {
                mEvalThread = null;
                //noinspection ResultOfMethodCallIgnored
                Thread.interrupted(); // 仅用于让线程重新复活
            }
            // 附加输出图像，现在会同时绘制 python 和 groovy 两个，保证相互调用时能正常工作
            // 先绘制 python，如果完全没有初始化（纯 groovy 环境）则不去绘制
            if (Python.InitHelper.initialized()) {
                // 简单的 matplotlab 支持
                if (!KERNEL_SHOW_FIGURE) {
                    //noinspection ConcatenationWithEmptyString
                    Python.exec("" +
                    "__jse_images__ = []\n" +
                    "if __JSE_HAS_MATPLOTLIB__:\n" +
                    "    for __jse_fig_num__ in __JSE_PYPLOT__.get_fignums():\n" +
                    "        __JSE_PYPLOT__.figure(__jse_fig_num__)\n" +
                    "        __jse_buffer__ = io.BytesIO()\n" +
                    "        __JSE_PYPLOT__.savefig(__jse_buffer__, format='png')\n" +
                    "        __jse_buffer__.seek(0)\n" +
                    "        __jse_images__.append(base64.b64encode(__jse_buffer__.read()).decode('utf-8'))\n" +
                    "    __JSE_PYPLOT__.close('all')"
                    );
                    for (Object tImage : Python.getValue("__jse_images__", List.class)) {
                        DisplayData tDisplayData = new DisplayData();
                        tDisplayData.putData(MIMEType.IMAGE_PNG, tImage.toString());
                        display(tDisplayData);
                    }
                    Python.exec("__jse_images__.clear()");
                }
            }
            // 再绘制 groovy 的图像
            if (!KERNEL_SHOW_FIGURE && !IPlotter.SHOWED_PLOTTERS.isEmpty()) {
                for (IPlotter tPlt : IPlotter.SHOWED_PLOTTERS) {
                    DisplayData tDisplayData = new DisplayData();
                    tDisplayData.putData(MIMEType.IMAGE_PNG, Base64.getMimeEncoder().encodeToString(tPlt.encode()));
                    display(tDisplayData);
                }
                // 绘制完成清空存储
                IPlotter.SHOWED_PLOTTERS.clear();
            }
            // 最后输出
            return tOut==null ? null : new DisplayData(toDisplayString(tOut));
        }
        @Override public List<String> formatError(Exception e) {
            if (!DEBUG) e = Main.deepSanitize(e);
            return super.formatError(e);
        }
        @Override public void interrupt() {
            // 只支持中断 groovy，python 原生就没有对中断提供支持
            if (mEvalThread != null) {
                mEvalThread.interrupt();
                mEvalThread = null;
            }
        }
        
        @Override public DisplayData inspect(String code, int at, boolean extraDetail) throws Exception {
            StringSearch.Range tMatch = StringSearch.findLongestMatchingAt(code, at, ID_CHAR);
            if (tMatch == null) return null;
            @Nullable CellMagicParseContext tCellCTX = mMagicParser.parseCellMagic(code);
            boolean tIsPython = tCellCTX!=null && tCellCTX.getMagicCall().getName().equals("python");
            String tID = tMatch.extractSubString(code);
            if (!(tIsPython ? Python.hasValue(tID) : Groovy.hasValue(tID))) return new DisplayData("No memory value for '"+tID+"'");
            Object tVal = tIsPython ? Python.getValue(tID) : Groovy.getValue(tID);
            return new DisplayData(toDisplayString(tVal));
        }
        @SuppressWarnings("RedundantThrows")
        @Override public ReplacementOptions complete(String code, int at) throws Exception {
            StringSearch.Range tMatch = StringSearch.findLongestMatchingAt(code, at, ID_CHAR);
            if (tMatch == null) return null;
            @Nullable CellMagicParseContext tCellCTX = mMagicParser.parseCellMagic(code);
            boolean tIsPython = tCellCTX!=null && tCellCTX.getMagicCall().getName().equals("python");
            String tPrefix = tMatch.extractSubString(code);
            return new ReplacementOptions((tIsPython ? PYTHON_AUTO_COMPLETER : GROOVY_AUTO_COMPLETER).autocomplete(tPrefix), tMatch.getLow(), tMatch.getHigh());
        }
    }
    
    /** 一般的 aScriptPath 合法化，返回 null 表示没有找到文件 */
    static @Nullable String findValidScriptPath(String aScriptPath, String aExtension, String aScriptDir) {
        aScriptDir = UT.IO.toInternalValidDir(aScriptDir);
        // 如果不是指定后缀则有限检测带有后缀的，和 .bat 脚本类似的逻辑，可以保证同名脚本共存
        if (!aScriptPath.endsWith(aExtension)) {
            @Nullable String tPath = findValidScriptPath_(aScriptPath+aExtension, aScriptDir);
            if (tPath != null) return tPath;
        }
        @Nullable String tPath = findValidScriptPath_(aScriptPath, aScriptDir);
        return tPath;
    }
    private static @Nullable String findValidScriptPath_(String aScriptPath, String aScriptDir) {
        // 都转为绝对路径避免意料外的问题
        String tPath = UT.IO.toAbsolutePath(aScriptPath);
        // 首先如果此文件存在则直接返回
        if (UT.IO.isFile(tPath)) return tPath;
        // 如果是绝对路径则不再考虑增加 aScriptDir 的情况
        if (UT.IO.isAbsolutePath(aScriptPath)) return null;
        // 否则增加 aScriptDir 后再次检测
        tPath = UT.IO.toAbsolutePath(aScriptDir+aScriptPath);
        if (UT.IO.isFile(tPath)) return tPath;
        // 否则返回 null
        return null;
    }
    
    /** Wrapper of {@link GroovyObject} for matlab and jep usage */
    public final static class GroovyObjectWrapper implements GroovyObject {
        private final GroovyObject mObj;
        GroovyObjectWrapper(GroovyObject aObj) {mObj = aObj;}
        
        @Override public Object invokeMethod(String name, Object args) {return of(mObj.invokeMethod(name, args));}
        @Override public Object getProperty(String propertyName) {return of(mObj.getProperty(propertyName));}
        @Override public void setProperty(String propertyName, Object newValue) {mObj.setProperty(propertyName, newValue);}
        @Override public MetaClass getMetaClass() {return mObj.getMetaClass();}
        @Override public void setMetaClass(MetaClass metaClass) {mObj.setMetaClass(metaClass);}
        
        @Override public String toString() {return mObj.toString();}
        public GroovyObject unwrap() {return mObj;}
        
        /** 主要用来判断是否需要外包这一层 */
        public static Object of(Object aObj) {
            if ((aObj instanceof GroovyObjectWrapper) || (aObj instanceof PyObject)) return aObj;
            else if (aObj instanceof GroovyObject) return new GroovyObjectWrapper((GroovyObject)aObj);
            else return aObj;
        }
        
        /** jep support */
        private final Object funcUnwrap = new Object() {public GroovyObject __call__() {return unwrap();}};
        public Object __getattribute__(final String attrName) {
            if (!Python.InitHelper.initialized()) {
                return getProperty(attrName);
            }
            // wrapper 带有的特有的函数
            if (attrName.equals("unwrap")) {
                return funcUnwrap;
            }
            // 直接回滚使用 mObj 的 __getattribute__，从而保证一致
            return Python.GET_ATTRIBUTE.apply(mObj, attrName);
        }
        public void __setattr__(String attrName, Object newAttr) {
            if (!Python.InitHelper.initialized()) {
                setProperty(attrName, newAttr);
                return;
            }
            // 直接回滚使用 mObj 的 __setattr__，从而保证一致
            Python.SET_ATTRIBUTE.accept(mObj, attrName, newAttr);
        }
        
        // 现在不再进行运算符重载之类的操作，因为 python 中更可能会使用 unwrap
        // 的对象，为了和 unwrap 的使用保持一致这里不再提供重载；
        // 注意到 python 中的类在 groovy 中会重载运算，而反之不会，这是因为 groovy
        // 的结果更加应该看成是 java 的类，而不是独立的脚本
    }
    
    
    /** 运行任意的脚本，自动检测脚本类型（根据后缀） */
    public static void run(String aScriptPath)                  throws Exception {run(aScriptPath, ZL_STR);}
    public static void run(String aScriptPath, String... aArgs) throws Exception {runScript(aScriptPath, aArgs);}
    public static void runScript(String aScriptPath)                  throws Exception {runScript(aScriptPath, ZL_STR);}
    public static void runScript(String aScriptPath, String... aArgs) throws Exception {
        // 有后缀的情况，直接执行
        if (aScriptPath.endsWith(".groovy")) {
            Groovy.runScript(aScriptPath, aArgs);
            return;
        } else
        if (aScriptPath.endsWith(".py")) {
            Python.runScript(aScriptPath, aArgs);
            return;
        }
        // 没有后缀的情况，优先认为是 groovy 脚本
        @Nullable String
        tPath = findValidScriptPath(aScriptPath, ".groovy", GROOVY_SP_DIR);
        if (tPath != null) {
            Groovy.runScript(aScriptPath, aArgs);
            return;
        }
        tPath = findValidScriptPath(aScriptPath, ".py", PYTHON_SP_DIR);
        if (tPath != null) {
            Python.runScript(aScriptPath, aArgs);
            return;
        }
        throw new FileNotFoundException(aScriptPath + " (" + UT.IO.toAbsolutePath(aScriptPath) + ")");
    }
    
    
    /** Groovy 脚本运行支持 */
    @SuppressWarnings("RedundantThrows")
    public static class Groovy {
        /** 将 aScriptPath 转换成 File，现在可以省略掉 script/groovy/ 以及后缀 */
        private static File toSourceFile(String aScriptPath) throws IOException {
            @Nullable String tPath = findValidScriptPath(aScriptPath, ".groovy", GROOVY_SP_DIR);
            if (tPath == null) throw new FileNotFoundException(aScriptPath + " (" + UT.IO.toAbsolutePath(aScriptPath) + ")");
            return UT.IO.toFile(tPath);
        }
        /** 现在 groovy 也一样统一使用全局的一个解释器 */
        private final static GroovyShell GROOVY_SHELL;
        private final static CompilerConfiguration GROOVY_CONF;
        private final static AtomicInteger COUNTER = new AtomicInteger(0);
        
        /** groovy 特有的一些属性访问 */
        public static Binding context() {return GROOVY_SHELL.getContext();}
        public static Binding binding() {return GROOVY_SHELL.getContext();}
        public static GroovyClassLoader classLoader() {return GROOVY_SHELL.getClassLoader();}
        
        /** 获取 shell 的交互式运行 */
        public static void runShell() throws Exception {
            // 使用这个方法来自动设置种类
            org.apache.groovy.groovysh.Main.setTerminalType("auto", false);
            // 这样手动指定 CLASS_LOADER
            Groovysh tGroovysh = new Groovysh(GROOVY_SHELL.getClassLoader(), GROOVY_SHELL.getContext(), new IO(), null, GROOVY_CONF);
            // 这样直接添加默认 import，shell 会默认导入这些方便使用
            tGroovysh.getImports().add(AbstractAtoms.class.getName());
            tGroovysh.getImports().add(Structures.class.getName());
            tGroovysh.getImports().add(AbstractCollections.class.getName());
            tGroovysh.getImports().add(Iterables.class.getName());
            tGroovysh.getImports().add(NewCollections.class.getName());
            tGroovysh.getImports().add(ArrayLists.class.getName());
            tGroovysh.getImports().add(InFiles.class.getName());
            tGroovysh.getImports().add(IOFiles.class.getName());
            tGroovysh.getImports().add(MathEX.class.getName());
            tGroovysh.getImports().add(ComplexDouble.class.getName());
            tGroovysh.getImports().add(Func1.class.getName());
            tGroovysh.getImports().add(Matrices.class.getName());
            tGroovysh.getImports().add(Tables.class.getName());
            tGroovysh.getImports().add(Vectors.class.getName());
            tGroovysh.getImports().add(Plotters.class.getName());
            tGroovysh.getImports().add("static "+UT.Timer.class.getName()+".*");
            tGroovysh.getImports().add("static "+UT.Par.class.getName()+".*");
            tGroovysh.getImports().add("static "+UT.IO.class.getName()+".*");
            tGroovysh.getImports().add("static "+UT.Math.class.getName()+".*");
            tGroovysh.getImports().add("static "+UT.Plot.class.getName()+".*");
            tGroovysh.getImports().add("static "+UT.Code.class.getName()+".*");
            tGroovysh.getImports().add("static "+CS.class.getName()+".*");
            tGroovysh.getImports().add("static "+OS.class.getName()+".*");
            tGroovysh.run(null);
        }
        
        /** python like stuffs，exec 不会获取返回值，eval 获取返回值 */
        public static void exec(String aText) throws Exception {GROOVY_SHELL.evaluate(aText, "ScriptJSE"+COUNTER.incrementAndGet()+".groovy");}
        public static void execFile(String aFilePath) throws Exception {GROOVY_SHELL.evaluate(toSourceFile(aFilePath));}
        public static Object eval(String aText) throws Exception {return GroovyObjectWrapper.of(GROOVY_SHELL.evaluate(aText, "ScriptJSE"+COUNTER.incrementAndGet()+".groovy"));}
        public static Object evalFile(String aFilePath) throws Exception {return GroovyObjectWrapper.of(GROOVY_SHELL.evaluate(toSourceFile(aFilePath)));}
        
        /** 直接运行文本的脚本 */
        public static Object runText(String aText)                  throws Exception {return runText(aText, ZL_STR);}
        public static Object runText(String aText, String... aArgs) throws Exception {return GroovyObjectWrapper.of(GROOVY_SHELL.run(aText, "ScriptJSE"+COUNTER.incrementAndGet()+".groovy", aArgs));}
        /** Groovy 现在也可以使用 getValue 来获取变量以及 setValue 设置变量（仅限于 Context 变量，这样可以保证效率） */
        public static Object get(String aValueName) throws Exception {return getValue(aValueName);}
        public static void set(String aValueName, Object aValue) throws Exception {setValue(aValueName, aValue);}
        public static void remove(String aValueName) throws Exception {removeValue(aValueName);}
        public static boolean hasValue(String aValueName) throws Exception {return GROOVY_SHELL.getContext().hasVariable(aValueName);}
        public static Object getValue(String aValueName) throws Exception {return GroovyObjectWrapper.of(GROOVY_SHELL.getContext().getVariable(aValueName));}
        public static void setValue(String aValueName, Object aValue) throws Exception {GROOVY_SHELL.getContext().setVariable(aValueName, aValue);}
        public static void removeValue(String aValueName) throws Exception {GROOVY_SHELL.getContext().removeVariable(aValueName);}
        /** 运行脚本文件 */
        public static Object run(String aScriptPath)                  throws Exception {return run(aScriptPath, ZL_STR);}
        public static Object run(String aScriptPath, String... aArgs) throws Exception {return runScript(aScriptPath, aArgs);}
        public static Object runScript(String aScriptPath)                  throws Exception {return runScript(aScriptPath, ZL_STR);}
        public static Object runScript(String aScriptPath, String... aArgs) throws Exception {return GroovyObjectWrapper.of(GROOVY_SHELL.run(toSourceFile(aScriptPath), aArgs));}
        /** 调用指定脚本中的方法，现在统一使用包名的做法 */
        public static Object invoke(String aMethodName)                  throws Exception {return invoke(aMethodName, ZL_OBJ);}
        public static Object invoke(String aMethodName, Object... aArgs) throws Exception {
            Object tObj;
            // 获取开口类名（如果存在的话）
            int tLastDot = aMethodName.lastIndexOf(".");
            if (tLastDot < 0) {
                tObj = null;
            } else {
                tObj = GROOVY_SHELL.evaluate(aMethodName.substring(0, tLastDot));
                aMethodName = aMethodName.substring(tLastDot+1);
            }
            // 现在统一使用使用 Groovy 的 InvokerHelper 来调用，更加通用，如果需要类型转换可以借助 setValue 走 groovy 的接口
            return GroovyObjectWrapper.of(InvokerHelper.invokeMethod(tObj, aMethodName, aArgs));
        }
        /** 创建脚本类的实例 */
        public static Object newInstance(String aClassName)                  throws Exception {return newInstance(aClassName, ZL_OBJ);}
        public static Object newInstance(String aClassName, Object... aArgs) throws Exception {
            return GroovyObjectWrapper.of(InvokerHelper.invokeConstructorOf(getClass(aClassName), aArgs));
        }
        // 现在不再支持关闭 GROOVY_SHELL 了，也没有必要关闭
        
        /** 现在这里也不进行包装，如果需要更加通用的调用可以借助 {@link InvokerHelper} */
        public static Class<?> getClass(String aClassName) throws Exception {
            return GROOVY_SHELL.getClassLoader().loadClass(aClassName);
        }
        public static Class<?> parseClass(String aScriptPath) throws IOException {
            return GROOVY_SHELL.getClassLoader().parseClass(toSourceFile(aScriptPath));
        }
        
        
        static {
            // 手动加载 CS.Exec，会自动重新设置工作目录，保证 Groovy 读取到的工作目录是正确的
            OS.InitHelper.init();
            // 先初始化配置
            GROOVY_CONF = new CompilerConfiguration();
            GROOVY_CONF.setSourceEncoding(StandardCharsets.UTF_8.name()); // 文件统一使用 utf-8 编码
            if (KERNEL_THREAD_INTERRUPT && Main.IS_KERNEL()) {
            GROOVY_CONF.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));
            }
            // 初始化 CLASS_LOADER
            // 重新指定 ClassLoader 为这个类的实际加载器
            GROOVY_SHELL = new GroovyShell(SP.class.getClassLoader(), new Binding(), GROOVY_CONF);
            // 指定默认的 Groovy 脚本的类路径
            GROOVY_SHELL.getClassLoader().addClasspath(UT.IO.toAbsolutePath(GROOVY_SP_DIR));
            // 增加一个 Groovy 的库的路径
            GROOVY_SHELL.getClassLoader().addClasspath(UT.IO.toAbsolutePath(GROOVY_LIB_DIR));
            // 增加外置 Groovy 的库的路径
            for (String tGroovyExlibDir : GROOVY_EXLIB_DIRS) {
            GROOVY_SHELL.getClassLoader().addClasspath(UT.IO.toAbsolutePath(tGroovyExlibDir));
            }
            // 增加 jar 文件夹下的所有 jar 文件到类路径中
            JAR_LIB_PATHS.forEach(path -> GROOVY_SHELL.getClassLoader().addClasspath(path));
        }
    }
    
    
    
    /** Python 脚本运行支持，完全基于 jep */
    public static class Python {
        
        /** 用于判断是否进行了静态初始化以及方便的手动初始化 */
        public final static class InitHelper {
            private static volatile boolean INITIALIZED = false;
            
            public static boolean initialized() {return INITIALIZED;}
            @SuppressWarnings("ResultOfMethodCallIgnored")
            public static void init() {
                // 手动调用此值来强制初始化
                if (!INITIALIZED) String.valueOf(JEP_INTERP);
            }
        }
        
        public final static class Conf {
            /**
             * 自定义构建 jep 时使用的编译器，
             * cmake 有时不能自动检测到希望使用的编译器；
             * <p>
             * 目前 jep 只支持 C 编译器
             */
            public static @Nullable String CMAKE_C_COMPILER = OS.env("JSE_CMAKE_C_COMPILER_JEP", jse.code.Conf.CMAKE_C_COMPILER);
            public static @Nullable String CMAKE_C_FLAGS    = OS.env("JSE_CMAKE_C_FLAGS_JEP"   , jse.code.Conf.CMAKE_C_FLAGS);
            
            /**
             * 对于 jep，是否使用 {@link MiMalloc} 来加速 c 的内存分配，
             * 这对于 java 数组和 c 数组的转换很有效
             */
            public static boolean USE_MIMALLOC = OS.envZ("JSE_USE_MIMALLOC_JEP", jse.code.Conf.USE_MIMALLOC);
            
            /** 重定向 jep 动态库的路径 */
            public static @Nullable String REDIRECT_JEP_LIB = OS.env("JSE_REDIRECT_JEP_LIB");
        }
        
        
        /** 包的版本 */
        private final static String JEP_VERSION = "4.2.0", ASE_VERSION = "3.23.0";
        /** jep 二进制库路径 */
        private final static String JEP_LIB_DIR = JAR_DIR+"jep/" + UT.Code.uniqueID(VERSION, JEP_VERSION, Conf.USE_MIMALLOC) + "/";
        private final static String JEP_LIB_PATH;
        /** 将 aScriptPath 合法化，现在可以省略掉 script/python/ 以及后缀 */
        private static String validScriptPath(String aScriptPath) throws IOException {
            @Nullable String tPath = findValidScriptPath(aScriptPath, ".py", PYTHON_SP_DIR);
            if (tPath == null) throw new FileNotFoundException(aScriptPath + " (" + UT.IO.toAbsolutePath(aScriptPath) + ")");
            return tPath;
        }
        /** 一样这里统一使用全局的一个解释器 */
        private static jep.Interpreter JEP_INTERP = null;
        /** 用于内部使用的 python 函数 */
        private static IBinaryFullOperator<Object, Object, String> GET_ATTRIBUTE = null;
        private static ITernaryConsumer<Object, String, Object> SET_ATTRIBUTE = null;
        
        
        /** python like stuffs，exec 不会获取返回值，eval 获取返回值 */
        public static void exec(String aText) throws JepException {JEP_INTERP.exec(aText);}
        public static void execFile(String aFilePath) throws JepException, IOException {JEP_INTERP.runScript(validScriptPath(aFilePath));}
        /** 由于 jep 的特性，这里可以直接使用 getValue 指定 eval */
        public static Object eval(String aText) throws JepException {return JEP_INTERP.getValue(aText);}
        // python 脚本文件不会有返回值
        
        /** 直接运行文本的脚本 */
        public static void runText(String aText)                  throws JepException {runText(aText, ZL_STR);}
        public static void runText(String aText, String... aArgs) throws JepException {setArgs_("", aArgs); JEP_INTERP.exec(aText);}
        /** Python 还可以使用 getValue 来获取变量以及 setValue 设置变量（原则上同样仅限于 Context 变量，允许可以超出 Context 但是不保证支持） */
        public static Object get(String aValueName) throws JepException {return getValue(aValueName);}
        public static void set(String aValueName, Object aValue) throws JepException {setValue(aValueName, aValue);}
        public static void remove(String aValueName) throws JepException {removeValue(aValueName);}
        public static boolean hasValue(String aValueName) throws JepException {return (Boolean)JEP_INTERP.getValue("('"+aValueName+"' in globals()) or ('"+aValueName+"' in locals())");}
        public static Object getValue(String aValueName) throws JepException {return JEP_INTERP.getValue(aValueName);}
        public static <T> T getValue(String aValueName, Class<T> aClazz) throws JepException {return JEP_INTERP.getValue(aValueName, aClazz);}
        public static void setValue(String aValueName, Object aValue) throws JepException {JEP_INTERP.set(aValueName, aValue);}
        public static void removeValue(String aValueName) throws JepException {JEP_INTERP.exec("del "+aValueName);}
        /** 运行脚本文件 */
        public static void run(String aScriptPath)                  throws JepException, IOException {run(aScriptPath, ZL_STR);}
        public static void run(String aScriptPath, String... aArgs) throws JepException, IOException {runScript(aScriptPath, aArgs);}
        public static void runScript(String aScriptPath)                  throws JepException, IOException {runScript(aScriptPath, ZL_STR);}
        public static void runScript(String aScriptPath, String... aArgs) throws JepException, IOException {
            // 现在不再保存旧的 sys.argv，如果需要可以在外部代码自行保存
            setArgs_(aScriptPath, aArgs);
            JEP_INTERP.runScript(validScriptPath(aScriptPath));
        }
        /** 调用方法，python 中需要结合 import 使用 */
        public static Object invoke(String aMethodName)                  throws JepException {return invoke(aMethodName, ZL_OBJ);}
        public static Object invoke(String aMethodName, Map<String, Object> aKWArgs) throws JepException {return JEP_INTERP.invoke(aMethodName, aKWArgs);}
        public static Object invoke(String aMethodName, Object[] aArgs, Map<String, Object> aKWArgs) throws JepException {return JEP_INTERP.invoke(aMethodName, aArgs, aKWArgs);}
        @SuppressWarnings("unchecked")
        public static Object invoke(String aMethodName, Object... aArgs) throws JepException {
            if (aArgs == null || aArgs.length == 0) return JEP_INTERP.invoke(aMethodName);
            if (aArgs.length == 1 && (aArgs[0] instanceof Map)) return JEP_INTERP.invoke(aMethodName, (Map<String, Object>)aArgs[0]);
            if (aArgs.length > 1 && (aArgs[aArgs.length-1] instanceof Map)) {
                Object[] tArgs = new Object[aArgs.length-1];
                System.arraycopy(aArgs, 0, tArgs, 0, aArgs.length-1);
                return JEP_INTERP.invoke(aMethodName, tArgs, (Map<String, Object>)aArgs[aArgs.length-1]);
            }
            return JEP_INTERP.invoke(aMethodName, aArgs);
        }
        /** 创建 Python 实例，这里可以直接将类名当作函数调用即可 */
        public static Object newInstance(String aClassName)                  throws JepException {return newInstance(aClassName, ZL_OBJ);}
        public static Object newInstance(String aClassName, Map<String, Object> aKWArgs) throws JepException {return invoke(aClassName, aKWArgs);}
        public static Object newInstance(String aClassName, Object[] aArgs, Map<String, Object> aKWArgs) throws JepException {return invoke(aClassName, aArgs, aKWArgs);}
        public static Object newInstance(String aClassName, Object... aArgs) throws JepException {return invoke(aClassName, aArgs);}
        
        /** 提供一个手动关闭 JEP_INTERP 的接口 */
        public static void close() throws JepException {if (JEP_INTERP != null) {JEP_INTERP.close(); JEP_INTERP = null; GET_ATTRIBUTE = null; SET_ATTRIBUTE = null;}}
        public static boolean isClosed() {return JEP_INTERP == null;}
        /** 提供一个手动刷新 JEP_INTERP 的接口，可以将关闭的重新打开，会清空所有创建的 python 变量 */
        public static void refresh() throws JepException {close(); initInterpreter_();}
        
        /** 似乎是不再需要这层包装了 */
        public static PyObject getClass(String aClassName) throws JepException {return JEP_INTERP.getValue(aClassName, PyObject.class);}
        
        /** 现在和 Groovy 逻辑保持一致，调用任何 run 都会全局重置 args 值 */
        private static void setArgs_(String aFirst, String[] aArgs) {
            List<String> tArgs = (aArgs==null || aArgs.length==0) ? Collections.singletonList(aFirst) : AbstractCollections.merge(aFirst, aArgs);
            // 使用这种方法保证设置成功
            setValue("_", tArgs);
            exec("sys.argv = _");
        }
        
        /** Python 提供额外的接口，获取同时做类型检查并转换 */
        public static <T> T getAs(Class<T> aExpectedType, String aValueName) throws JepException {return JEP_INTERP.getValue(aValueName, aExpectedType);}
        
        /** Python 提供额外的接口，提供专门的 parfor 带有线程独立的 Interpreter，这种写法可以保证 jse 的 python 环境一定成功初始化 */
        public static void parforWithInterpreter(int aSize, @ClosureParams(value= FromString.class, options={"jep.Interpreter", "jep.Interpreter,int", "jep.Interpreter,int,int"}) final Closure<?> aGroovyTask) {parforWithInterpreter(aSize, PARFOR_THREAD_NUMBER, aGroovyTask);}
        @SuppressWarnings({"resource", "UnnecessaryReturnStatement"})
        public static void parforWithInterpreter(int aSize, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, @ClosureParams(value= FromString.class, options={"jep.Interpreter", "jep.Interpreter,int", "jep.Interpreter,int,int"}) final Closure<?> aGroovyTask) {
            // 这里约定了线程数为 1 时一定是主线程串行执行，并且其余情况下一定会创建新线程运行，主线程只进行等待
            if (aThreadNum == 1) {
                switch(aGroovyTask.getMaximumNumberOfParameters()) {
                case 3:  {UT.Par.pool(aThreadNum).parfor(aSize, (i, threadID) -> aGroovyTask.call(JEP_INTERP, i, threadID)); return;}
                case 2:  {UT.Par.pool(aThreadNum).parfor(aSize, (i, threadID) -> aGroovyTask.call(JEP_INTERP, i)); return;}
                default: {UT.Par.pool(aThreadNum).parfor(aSize, (i, threadID) -> aGroovyTask.call(JEP_INTERP)); return;}
                }
            }
            final jep.Interpreter[] tInterpreters = new jep.Interpreter[aThreadNum];
            ParforThreadPool.ITaskWithID tInitDo = threadID -> tInterpreters[threadID] = new jep.SharedInterpreter();
            ParforThreadPool.ITaskWithID tFinalDo = threadID -> tInterpreters[threadID].close();
            switch(aGroovyTask.getMaximumNumberOfParameters()) {
            case 3:  {UT.Par.pool(aThreadNum).parfor(aSize, tInitDo, tFinalDo, (i, threadID) -> aGroovyTask.call(tInterpreters[threadID], i, threadID)); return;}
            case 2:  {UT.Par.pool(aThreadNum).parfor(aSize, tInitDo, tFinalDo, (i, threadID) -> aGroovyTask.call(tInterpreters[threadID], i)); return;}
            default: {UT.Par.pool(aThreadNum).parfor(aSize, tInitDo, tFinalDo, (i, threadID) -> aGroovyTask.call(tInterpreters[threadID])); return;}
            }
        }
        public static void parwhileWithInterpreter(ParforThreadPool.IParwhileChecker aChecker, @ClosureParams(value= FromString.class, options={"jep.Interpreter", "jep.Interpreter,int"}) final Closure<?> aGroovyTask) {parwhileWithInterpreter(aChecker, PARFOR_THREAD_NUMBER, aGroovyTask);}
        @SuppressWarnings({"resource", "UnnecessaryReturnStatement"})
        public static void parwhileWithInterpreter(ParforThreadPool.IParwhileChecker aChecker, @Range(from=1, to=Integer.MAX_VALUE) int aThreadNum, @ClosureParams(value= FromString.class, options={"jep.Interpreter", "jep.Interpreter,int"}) final Closure<?> aGroovyTask) {
            // 这里约定了线程数为 1 时一定是主线程串行执行，并且其余情况下一定会创建新线程运行，主线程只进行等待
            if (aThreadNum == 1) {
                if (aGroovyTask.getMaximumNumberOfParameters() == 2) {
                    UT.Par.pool(aThreadNum).parwhile(aChecker, threadID -> aGroovyTask.call(JEP_INTERP, threadID));
                    return;
                }
                UT.Par.pool(aThreadNum).parwhile(aChecker, threadID -> aGroovyTask.call(JEP_INTERP));
                return;
            }
            final jep.Interpreter[] tInterpreters = new jep.Interpreter[aThreadNum];
            ParforThreadPool.ITaskWithID tInitDo = threadID -> tInterpreters[threadID] = new jep.SharedInterpreter();
            ParforThreadPool.ITaskWithID tFinalDo = threadID -> tInterpreters[threadID].close();
            if (aGroovyTask.getMaximumNumberOfParameters() == 2) {
                UT.Par.pool(aThreadNum).parwhile(aChecker, tInitDo, tFinalDo, threadID -> aGroovyTask.call(tInterpreters[threadID], threadID));
                return;
            }
            UT.Par.pool(aThreadNum).parwhile(aChecker, tInitDo, tFinalDo, threadID -> aGroovyTask.call(tInterpreters[threadID]));
            return;
        }
        
        public static <U> Future<U> callAsyncWithInterpreter(@ClosureParams(value=SimpleType.class, options="jep.Interpreter") final Closure<U> aGroovyTask) {
            return UT.Par.callAsync(() -> {
                try (jep.Interpreter interp = new jep.SharedInterpreter()) {return aGroovyTask.call(interp);}
            });
        }
        public static <U> Future<U> supplyAsyncWithInterpreter(@ClosureParams(value=SimpleType.class, options="jep.Interpreter") final Closure<U> aGroovyTask) {
            return UT.Par.supplyAsync(() -> {
                try (jep.Interpreter interp = new jep.SharedInterpreter()) {return aGroovyTask.call(interp);}
            });
        }
        public static Future<Void> runAsyncWithInterpreter(@ClosureParams(value=SimpleType.class, options="jep.Interpreter") final Closure<?> aGroovyTask) {
            return UT.Par.runAsync(() -> {
                try (jep.Interpreter interp = new jep.SharedInterpreter()) {aGroovyTask.call(interp);}
            });
        }
        
        
        static {
            InitHelper.INITIALIZED = true;
            // 手动加载 CS.Exec，会自动重新设置工作目录，保证 jep 读取到的工作目录是正确的
            OS.InitHelper.init();
            // 在 JVM 关闭时关闭 JEP_INTERP，最先添加来避免一些问题
            Main.addGlobalAutoCloseable(Python::close);
            
            // 依赖 jniutil
            JNIUtil.InitHelper.init();
            // 如果开启了 USE_MIMALLOC 则增加 MiMalloc 依赖
            if (Conf.USE_MIMALLOC) MiMalloc.InitHelper.init();
            
            if (Conf.REDIRECT_JEP_LIB == null) {
                // 设置 Jep 非 java 库的路径，考虑到 WSL，windows 和 linux 使用不同的名称
                @Nullable String tLibName = LIB_NAME_IN(JEP_LIB_DIR, "jep");
                // 如果不存在则需要重新通过源码编译
                if (tLibName == null) {
                    System.out.println("JEP INIT INFO: jep libraries not found. Reinstalling...");
                    try {tLibName = installJep_();} catch (Exception e) {throw new RuntimeException(e);}
                }
                JEP_LIB_PATH = JEP_LIB_DIR+tLibName;
            } else {
                if (DEBUG) System.out.println("JEP INIT INFO: jep libraries are redirected to '"+Conf.REDIRECT_JEP_LIB+"'");
                JEP_LIB_PATH = Conf.REDIRECT_JEP_LIB;
            }
            // 设置库路径
            jep.MainInterpreter.setJepLibraryPath(UT.IO.toAbsolutePath(JEP_LIB_PATH));
            
            // 配置 Jep，这里只能配置一次
            jep.SharedInterpreter.setConfig(new JepConfig()
                .addIncludePaths(UT.IO.toAbsolutePath(PYTHON_SP_DIR),
                                 UT.IO.toAbsolutePath(PYTHON_LIB_DIR),
                                 UT.IO.toAbsolutePath(JEP_LIB_DIR))
                .addIncludePaths(NewCollections.mapArray(PYTHON_EXLIB_DIRS, UT.IO::toAbsolutePath))
                .setClassLoader(Groovy.classLoader()) // 指定 Groovy 的 ClassLoader 从而可以直接导入 groovy 的类
                .redirectStdout(System.out)
                .redirectStdErr(System.err));
            // 把 groovy 的类路径也加进去
            jep.ClassList.ADDITIONAL_CLASS_PATHS.add(UT.IO.toAbsolutePath(GROOVY_SP_DIR));
            jep.ClassList.ADDITIONAL_CLASS_PATHS.add(UT.IO.toAbsolutePath(GROOVY_LIB_DIR));
            for (String tGroovyExlibDir : GROOVY_EXLIB_DIRS) {
            jep.ClassList.ADDITIONAL_CLASS_PATHS.add(UT.IO.toAbsolutePath(tGroovyExlibDir));
            }
            jep.ClassList.ADDITIONAL_CLASS_PATHS.addAll(JAR_LIB_PATHS);
            jep.ClassList.ADDITIONAL_CLASS_FILE_EXTENSION.add(".groovy");
            // 初始化 JEP_INTERP
            initInterpreter_();
        }
        /** 初始化内部的 JEP_INTERP，主要用于减少重复代码 */
        @SuppressWarnings("unchecked")
        private static void initInterpreter_() {
            JEP_INTERP = new jep.SharedInterpreter();
            JEP_INTERP.exec("import sys");
            // python 函数获取
            JEP_INTERP.exec("def __JSE_GET_ATTRIBUTE__(obj, name):\n    return obj.__getattribute__(name)");
            JEP_INTERP.exec("def __JSE_SET_ATTRIBUTE__(obj, name, value):\n    return obj.__setattr__(name, value)");
            GET_ATTRIBUTE = (IBinaryFullOperator<Object, Object, String>)JEP_INTERP.getValue("__JSE_GET_ATTRIBUTE__", IBinaryFullOperator.class);
            SET_ATTRIBUTE = (ITernaryConsumer<Object, String, Object>)JEP_INTERP.getValue("__JSE_SET_ATTRIBUTE__", ITernaryConsumer.class);
            // 简单的 matplotlab 支持
            if (!KERNEL_SHOW_FIGURE && Main.IS_KERNEL()) {
                //noinspection ConcatenationWithEmptyString
                JEP_INTERP.exec("" +
                "try:\n" +
                "    import matplotlib\n" +
                "    import matplotlib.pyplot as __JSE_PYPLOT__\n" +
                "    import io\n" +
                "    import base64\n" +
                "    __JSE_HAS_MATPLOTLIB__ = True\n" +
                "except ImportError:\n" +
                "    __JSE_HAS_MATPLOTLIB__ = False\n" +
                "if __JSE_HAS_MATPLOTLIB__:\n" +
                "    matplotlib.use('Agg')"
                );
            }
        }
        
        
        /** 基于 pip 的 python 包管理，下载指定包到 .pypkg */
        public static void downloadPackage(String aRequirement, boolean aIncludeDep, String aPlatform, String aPythonVersion) throws IOException {
            // 组装指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("pip"); rCommand.add("download");
            // 是否顺便下载依赖的库，这里默认不会下载依赖库，因为问题会很多
            if (!aIncludeDep) rCommand.add("--no-deps");
            // 是否指定特定平台和 python 版本，如果同时开启了 aIncludeDep 则会强制开启 --only-binary :all:
            boolean tOnlyBinary = false;
            if (aPlatform != null && !aPlatform.isEmpty()) {
                if (aIncludeDep) {
                    rCommand.add("--only-binary"); rCommand.add(":all:");
                    tOnlyBinary = true;
                }
                rCommand.add("--platform"); rCommand.add(aPlatform);
            }
            if (aPythonVersion != null && !aPythonVersion.isEmpty()) {
                if (aIncludeDep && !tOnlyBinary) {
                    rCommand.add("--only-binary"); rCommand.add(":all:");
                }
                rCommand.add("--python-version"); rCommand.add(aPythonVersion);
            }
            // 不提供强制仅下载源码的选项，因为很多下载的源码都不能编译成功
            // 设置目标路径
            UT.IO.makeDir(PYTHON_PKG_DIR);
            rCommand.add("--dest"); rCommand.add("'"+PYTHON_PKG_DIR+"'");
            // 设置需要的包名
            rCommand.add("'"+aRequirement+"'");
            
            // 直接通过系统指令执行 pip 来下载
            EXEC.system(String.join(" ", rCommand));
        }
        public static void downloadPackage(String aRequirement, String aPlatform, String aPythonVersion) throws IOException {downloadPackage(aRequirement, false, aPlatform, aPythonVersion);}
        public static void downloadPackage(String aRequirement, String aPlatform) throws IOException {downloadPackage(aRequirement, aPlatform, null);}
        public static void downloadPackage(String aRequirement, boolean aIncludeDep, String aPlatform) throws IOException {downloadPackage(aRequirement, aIncludeDep, aPlatform, null);}
        public static void downloadPackage(String aRequirement, boolean aIncludeDep) throws IOException {downloadPackage(aRequirement, aIncludeDep, null);}
        public static void downloadPackage(String aRequirement) throws IOException {downloadPackage(aRequirement, false);}
        
        /** 基于 pip 的 python 包管理，直接安装指定包到 lib */
        public static void installPackage(String aRequirement, boolean aIncludeDep, boolean aIncludeIndex) throws IOException {
            // 组装指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("pip"); rCommand.add("install");
            // 是否顺便下载依赖的库，这里默认不会下载依赖库，因为问题会很多
            if (!aIncludeDep) rCommand.add("--no-deps");
            // 是否开启联网，这里默认不开启联网，因为标准下会使用 downloadPackage 来下载包
            if (!aIncludeIndex) rCommand.add("--no-index");
            // 添加 .pypkg 到搜索路径
            UT.IO.makeDir(PYTHON_PKG_DIR);
            rCommand.add("--find-links"); rCommand.add("'file:"+PYTHON_PKG_DIR+"'");
            // 设置目标路径
            UT.IO.makeDir(PYTHON_LIB_DIR);
            rCommand.add("--target"); rCommand.add("'"+PYTHON_LIB_DIR+"'");
            // 强制开启更新，替换已有的包
            rCommand.add("--upgrade");
            // 设置需要的包名
            rCommand.add("'"+aRequirement+"'");
            
            // 直接通过系统指令执行 pip 来下载
            EXEC.system(String.join(" ", rCommand));
        }
        public static void installPackage(String aRequirement, boolean aIncludeDep) throws IOException {installPackage(aRequirement, aIncludeDep, false);}
        public static void installPackage(String aRequirement) throws IOException {installPackage(aRequirement, false);}
        
        
        private static String cmakeInitCmdJep_() {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("cmake");
            // 这里设置 C 编译器（如果有）
            if (Conf.CMAKE_C_COMPILER != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="+Conf.CMAKE_C_COMPILER  );}
            if (Conf.CMAKE_C_FLAGS    != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS='" +Conf.CMAKE_C_FLAGS+"'");}
            // 初始化使用上一个目录的 CMakeList.txt
            rCommand.add("..");
            return String.join(" ", rCommand);
        }
        private static String cmakeSettingCmdJep_() throws IOException {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("cmake");
            rCommand.add("-D"); rCommand.add("JSE_USE_MIMALLOC="+(Conf.USE_MIMALLOC?"ON":"OFF"));
            // 设置构建输出目录为 lib
            UT.IO.makeDir(JEP_LIB_DIR); // 初始化一下这个目录避免意料外的问题
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH='"+ JEP_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH='"+ JEP_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH='"+ JEP_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH='"+ JEP_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH='"+ JEP_LIB_DIR +"'");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH='"+ JEP_LIB_DIR +"'");
            rCommand.add(".");
            return String.join(" ", rCommand);
        }
        
        /** 内部使用的安装 jep 的操作，和一般的库不同，jep 由于不能离线使用 pip 安装，这里直接使用源码编译 */
        private static @NotNull String installJep_() throws Exception {
            // 检测 cmake，这里要求一定要有 cmake 环境
            EXEC.setNoSTDOutput().setNoERROutput();
            boolean tNoCmake = EXEC.system("cmake --version") != 0;
            EXEC.setNoSTDOutput(false).setNoERROutput(false);
            if (tNoCmake) throw new Exception("JEP BUILD ERROR: No cmake environment.");
            String tWorkingDir = WORKING_DIR_OF("jep");
            // 如果已经存在则先删除
            UT.IO.removeDir(tWorkingDir);
            // 首先获取源码路径，这里直接从 resource 里输出
            String tJepZipPath = tWorkingDir+"jep-"+JEP_VERSION+".zip";
            UT.IO.copy(UT.IO.getResource("jep/jep-"+JEP_VERSION+".zip"), tJepZipPath);
            // 解压 jep 包到临时目录，如果已经存在则直接清空此目录
            String tJepDir = tWorkingDir+"jep/";
            UT.IO.removeDir(tJepDir);
            UT.IO.zip2dir(tJepZipPath, tJepDir);
            // 这里对 CMakeLists.txt 特殊处理，注意要输出到不同的文件
            UT.IO.map(tJepDir+"CMakeLists.txt", tJepDir+"CMakeLists1.txt", line -> {
                // 替换其中的 jniutil 库路径为设置好的路径
                line = line.replace("$ENV{JSE_JNIUTIL_INCLUDE_DIR}", JNIUtil.INCLUDE_DIR.replace("\\", "\\\\")); // 注意反斜杠的转义问题
                // 替换其中的 mimalloc 库路径为设置好的路径
                if (Conf.USE_MIMALLOC) {
                line = line.replace("$ENV{JSE_MIMALLOC_INCLUDE_DIR}", MiMalloc.INCLUDE_DIR.replace("\\", "\\\\"))  // 注意反斜杠的转义问题
                           .replace("$ENV{JSE_MIMALLOC_LIB_PATH}"   , MiMalloc.LLIB_PATH  .replace("\\", "\\\\")); // 注意反斜杠的转义问题
                }
                return line;
            });
            // 覆盖旧的 CMakeLists.txt
            UT.IO.move(tJepDir+"CMakeLists1.txt", tJepDir+"CMakeLists.txt");
            // 安装 jep 包，这里通过 cmake 来安装
            System.out.println("JEP INIT INFO: Installing jep from source code...");
            String tJepBuildDir = tJepDir+"build/";
            UT.IO.makeDir(tJepBuildDir);
            // 直接通过系统指令来编译 Jep 的库，关闭输出
            EXEC.setNoSTDOutput().setWorkingDir(tJepBuildDir);
            // 初始化 cmake
            EXEC.system(cmakeInitCmdJep_());
            // 设置参数
            EXEC.system(cmakeSettingCmdJep_());
            // 最后进行构造操作
            EXEC.system("cmake --build . --config Release");
            EXEC.setNoSTDOutput(false).setWorkingDir(null);
            // 简单检测一下是否编译成功
            @Nullable String tJepLibName = LIB_NAME_IN(JEP_LIB_DIR, "jep");
            if (tJepLibName == null) throw new Exception("JEP BUILD ERROR: No jep lib in "+JEP_LIB_DIR);
            // 拷贝 python 脚本
            String tJepPyDir = tJepDir+"src/main/python/jep/";
            String tJepLibPyDir = JEP_LIB_DIR+"jep/";
            UT.IO.removeDir(tJepLibPyDir); // 如果存在删除一下保证移动成功
            try {
                UT.IO.move(tJepPyDir, tJepLibPyDir);
            } catch (Exception e) {
                // 移动失败则尝试直接拷贝整个目录
                UT.IO.copyDir(tJepPyDir, tJepLibPyDir);
            }
            // 完事后移除临时解压得到的源码
            UT.IO.removeDir(tWorkingDir);
            System.out.println("JEP INIT INFO: jep successfully installed.");
            // 输出安装完成后的库名称
            return tJepLibName;
        }
        
        /** 一些内置的 python 库安装，主要用于内部使用 */
        public static void installAse() throws IOException {
            // 首先获取源码路径，这里直接检测是否是 ase-$ASE_VERSION 开头
            UT.IO.makeDir(PYTHON_PKG_DIR);
            String[] tList = UT.IO.list(PYTHON_PKG_DIR);
            boolean tHasAsePkg = false;
            for (String tName : tList) if (tName.startsWith("ase-"+ASE_VERSION)) {
                tHasAsePkg = true; break;
            }
            // 如果没有 ase 包则直接下载，指定版本 ASE_VERSION 避免因为更新造成的问题
            if (!tHasAsePkg) {
                System.out.printf("ASE INIT INFO: No ase package in %s, downloading...\n", PYTHON_PKG_DIR);
                downloadPackage("ase=="+ASE_VERSION);
                System.out.println("ASE INIT INFO: ase package downloading finished");
            }
            // 安装 ase 包
            System.out.println("ASE INIT INFO: Installing ase from package...");
            installPackage("ase=="+ASE_VERSION);
            System.out.println("ASE INIT INFO: ase Installing finished");
        }
    }
}
