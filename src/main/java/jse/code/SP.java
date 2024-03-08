package jse.code;

import com.google.common.collect.ImmutableList;
import groovy.lang.*;
import groovy.transform.ThreadInterrupt;
import io.github.spencerpark.jupyter.kernel.BaseKernel;
import io.github.spencerpark.jupyter.kernel.LanguageInfo;
import io.github.spencerpark.jupyter.kernel.ReplacementOptions;
import io.github.spencerpark.jupyter.kernel.display.DisplayData;
import io.github.spencerpark.jupyter.kernel.display.mime.MIMEType;
import io.github.spencerpark.jupyter.kernel.util.CharPredicate;
import io.github.spencerpark.jupyter.kernel.util.SimpleAutoCompleter;
import io.github.spencerpark.jupyter.kernel.util.StringSearch;
import io.github.spencerpark.jupyter.messages.Header;
import jep.JepConfig;
import jep.JepException;
import jline.Terminal;
import jline.WindowsTerminal;
import jse.Main;
import jse.atom.AbstractAtoms;
import jse.atom.Structures;
import jse.code.collection.AbstractCollections;
import jse.code.collection.ArrayLists;
import jse.code.collection.Iterables;
import jse.code.collection.NewCollections;
import jse.io.IOFiles;
import jse.io.InFiles;
import jse.math.ComplexDouble;
import jse.math.MathEX;
import jse.math.function.Func1;
import jse.math.matrix.Matrices;
import jse.math.table.Tables;
import jse.math.vector.Vectors;
import jse.plot.IPlotter;
import jse.plot.Plotters;
import org.apache.groovy.groovysh.Groovysh;
import org.apache.groovy.groovysh.InteractiveShellRunner;
import org.apache.groovy.groovysh.Interpreter;
import org.codehaus.groovy.control.CompilerConfiguration;
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.tools.shell.IO;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import static jse.code.CS.Exec.EXE;
import static jse.code.CS.Exec.JAR_DIR;
import static jse.code.CS.VERSION;
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
    
    
    /** 运行任意的脚本，自动检测脚本类型（根据后缀） */
    public static void run(String aScriptPath, String... aArgs) throws Exception {runScript(aScriptPath, aArgs);}
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
        /** Jupyter Kernel for Groovy */
        public static class JupyterKernel extends BaseKernel {
            private static final SimpleAutoCompleter AUTO_COMPLETER = SimpleAutoCompleter.builder()
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
            private static final CharPredicate ID_CHAR = CharPredicate.builder()
                .inRange('a', 'z')
                .inRange('A', 'Z')
                .inRange('0', '9')
                .match('_')
                .build();
            private static String toDisplayString(Object aObj) {
                if (aObj instanceof String) {
                    return "'"+aObj+"'";
                } else
                if (aObj instanceof GString) {
                    return "\""+aObj+"\"";
                } else
                if (aObj instanceof CharSequence) {
                    return "'"+aObj+"'";
                } else {
                    return aObj.toString();
                }
            }
            
            private final LanguageInfo mLanguageInfo;
            private final String mBanner;
            private final List<LanguageInfo.Help> mHelpLinks;
            public JupyterKernel() {
                super(StandardCharsets.UTF_8);
                mLanguageInfo = new LanguageInfo.Builder("groovy")
                    .version(GroovySystem.getVersion())
                    .fileExtension(".groovy")
                    .pygments("groovy")
                    .codemirror("groovy")
                    .build();
                mBanner = "jse "+VERSION+String.format(" (groovy: %s, java: %s)", GroovySystem.getVersion(), System.getProperty("java.version"))+"\n"
                         +"Protocol v"+Header.PROTOCOL_VERISON+" implementation by "+KERNEL_META.getOrDefault("project", "UNKNOWN")+" "+KERNEL_META.getOrDefault("version", "UNKNOWN");
                mHelpLinks = ImmutableList.of(
                    new LanguageInfo.Help("Groovy tutorial", "https://groovy-lang.org/learn.html"),
                    new LanguageInfo.Help("JSE homepage", "https://github.com/CHanzyLazer/jse"));
            }
            @Override public LanguageInfo getLanguageInfo() {return mLanguageInfo;}
            @Override public String getBanner() {return mBanner;}
            @Override public List<LanguageInfo.Help> getHelpLinks() {return mHelpLinks;}
            
            private volatile Future<?> mEvalTask = null;
            @Override public DisplayData eval(String expr) throws Exception {
                mEvalTask = UT.Par.callAsync(() -> Groovy.eval(expr));
                try {
                    Object tOut = mEvalTask.get();
                    mEvalTask = null;
                    // 附加输出图像
                    if (!KERNEL_SHOW_FIGURE && !IPlotter.SHOWED_PLOTTERS.isEmpty()) {
                        for (IPlotter tPlt : IPlotter.SHOWED_PLOTTERS) {
                            DisplayData tDisplayData = new DisplayData();
                            tDisplayData.putData(MIMEType.IMAGE_PNG, Base64.getMimeEncoder().encodeToString(tPlt.encode()));
                            display(tDisplayData);
                        }
                        // 绘制完成清空存储
                        IPlotter.SHOWED_PLOTTERS.clear();
                    }
                    return tOut==null ? null : (tOut instanceof DisplayData) ? (DisplayData)tOut : getRenderer().render(tOut);
                } catch (ExecutionException e) {
                    Throwable t = e.getCause();
                    if (t instanceof Exception) throw (Exception)t;
                    else throw e;
                }
            }
            @Override public List<String> formatError(Exception e) {
                return super.formatError(Main.deepSanitize(e));
            }
            @Override public void interrupt() {
                if (mEvalTask != null) {
                    mEvalTask.cancel(true);
                    mEvalTask = null;
                }
            }
            
            @Override public DisplayData inspect(String code, int at, boolean extraDetail) throws Exception {
                StringSearch.Range tMatch = StringSearch.findLongestMatchingAt(code, at, ID_CHAR);
                if (tMatch == null) return null;
                String tID = tMatch.extractSubString(code);
                if (!GROOVY_INTERP.getContext().hasVariable(tID)) return new DisplayData("No memory value for '"+tID+"'");
                Object tVal = GROOVY_INTERP.getContext().getVariable(tID);
                return new DisplayData(toDisplayString(tVal));
            }
            @Override public ReplacementOptions complete(String code, int at) throws Exception {
                StringSearch.Range tMatch = StringSearch.findLongestMatchingAt(code, at, ID_CHAR);
                if (tMatch == null) return null;
                String tPrefix = tMatch.extractSubString(code);
                return new ReplacementOptions(AUTO_COMPLETER.autocomplete(tPrefix), tMatch.getLow(), tMatch.getHigh());
            }
        }
        
        
        /** Wrapper of {@link GroovyObject} for matlab usage */
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
            public static Object of(Object aObj) {return (!(aObj instanceof GroovyObjectWrapper) && (aObj instanceof GroovyObject)) ? (new GroovyObjectWrapper((GroovyObject)aObj)) : aObj;}
        }
        
        /** 将 aScriptPath 转换成 File，现在可以省略掉 script/groovy/ 以及后缀 */
        private static File toSourceFile(String aScriptPath) throws IOException {
            @Nullable String tPath = findValidScriptPath(aScriptPath, ".groovy", GROOVY_SP_DIR);
            if (tPath == null) throw new FileNotFoundException(aScriptPath + " (" + UT.IO.toAbsolutePath(aScriptPath) + ")");
            return UT.IO.toFile(tPath);
        }
        /** 现在 groovy 也一样统一使用全局的一个解释器 */
        private static Interpreter GROOVY_INTERP = null;
        private final static CompilerConfiguration GROOVY_CONF;
        private final static AtomicInteger COUNTER = new AtomicInteger(0);
        
        /** 获取 shell 的交互式运行 */
        public static void runShell() throws Exception {
            // 使用这个方法来自动设置种类
            org.apache.groovy.groovysh.Main.setTerminalType("auto", false);
            // 这样手动指定 CLASS_LOADER
            Groovysh tGroovysh = new Groovysh(GROOVY_INTERP.getClassLoader(), GROOVY_INTERP.getContext(), new IO(), null, GROOVY_CONF, GROOVY_INTERP) {
                /** 直接重写 displayWelcomeBanner 来将 jse 的版本添加进去 */
                @Override public void displayWelcomeBanner(InteractiveShellRunner runner) {
                    IO io = getIo();
                    if (!log.isDebug() && io.isQuiet()) {
                        // nothing to do here
                        return;
                    }
                    
                    Terminal term = runner.getReader().getTerminal();
                    if (log.isDebug()) {
                        log.debug("Terminal ("+term+")");
                        log.debug("    Supported:  "+term.isSupported());
                        log.debug("    ECHO:       (enabled: "+term.isEchoEnabled()+")");
                        log.debug("    H x W:      "+term.getHeight()+" x "+term.getWidth());
                        log.debug("    ANSI:       "+term.isAnsiSupported());
                        
                        if (term instanceof WindowsTerminal) {
                            WindowsTerminal winterm = (WindowsTerminal) term;
                            log.debug("    Direct:     "+winterm.getDirectConsole());
                        }
                    }
                    
                    // Display the welcome banner
                    if (!io.isQuiet()) {
                        int width = term.getWidth();
                        
                        // If we can't tell, or have something bogus then use a reasonable default
                        if (width < 1) {
                            width = 80;
                        }
                        
                        io.out.println(MessageFormat.format("@|green JSE Shell|@ ({0}, Groovy: {1}, JVM: {2})", VERSION, GroovySystem.getVersion(), System.getProperty("java.version")));
                        io.out.println("Type '@|bold :help|@' or '@|bold :h|@' for help.");
                        io.out.println(UT.Text.repeat('-', width-1));
                    }
                }
            };
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
            tGroovysh.getImports().add("static "+UT.Exec.class.getName()+".*");
            tGroovysh.getImports().add("static "+UT.IO.class.getName()+".*");
            tGroovysh.getImports().add("static "+UT.Math.class.getName()+".*");
            tGroovysh.getImports().add("static "+UT.Plot.class.getName()+".*");
            tGroovysh.getImports().add("static "+UT.Code.class.getName()+".*");
            tGroovysh.getImports().add("static "+CS.class.getName()+".*");
            tGroovysh.getImports().add("static "+CS.Exec.class.getName()+".*");
            tGroovysh.run(null);
        }
        
        /** python like stuffs，exec 不会获取返回值，eval 获取返回值 */
        public static void exec(String aText) throws Exception {GROOVY_INTERP.getShell().evaluate(aText, "ScriptJSE"+COUNTER.incrementAndGet()+".groovy");}
        public static void execFile(String aFilePath) throws Exception {GROOVY_INTERP.getShell().evaluate(toSourceFile(aFilePath));}
        public static Object eval(String aText) throws Exception {return GroovyObjectWrapper.of(GROOVY_INTERP.getShell().evaluate(aText, "Script"+COUNTER.incrementAndGet()+".groovy"));}
        public static Object evalFile(String aFilePath) throws Exception {return GroovyObjectWrapper.of(GROOVY_INTERP.getShell().evaluate(toSourceFile(aFilePath)));}
        
        /** 直接运行文本的脚本 */
        public static Object runText(String aText, String... aArgs) throws Exception {return GroovyObjectWrapper.of(GROOVY_INTERP.getShell().run(aText, "ScriptJSE"+COUNTER.incrementAndGet()+".groovy", aArgs));}
        /** Groovy 现在也可以使用 getValue 来获取变量以及 setValue 设置变量（仅限于 Context 变量，这样可以保证效率） */
        public static Object get(String aValueName) throws Exception {return getValue(aValueName);}
        public static void set(String aValueName, Object aValue) throws Exception {setValue(aValueName, aValue);}
        public static void remove(String aValueName) throws Exception {removeValue(aValueName);}
        public static Object getValue(String aValueName) throws Exception {return GroovyObjectWrapper.of(GROOVY_INTERP.getContext().getVariable(aValueName));}
        public static void setValue(String aValueName, Object aValue) throws Exception {GROOVY_INTERP.getContext().setVariable(aValueName, aValue);}
        public static void removeValue(String aValueName) throws Exception {GROOVY_INTERP.getContext().removeVariable(aValueName);}
        /** 运行脚本文件 */
        public static Object run(String aScriptPath, String... aArgs) throws Exception {return runScript(aScriptPath, aArgs);}
        public static Object runScript(String aScriptPath, String... aArgs) throws Exception {return GroovyObjectWrapper.of(GROOVY_INTERP.getShell().run(toSourceFile(aScriptPath), aArgs));}
        /** 调用指定脚本中的方法 */
        public static Object invoke(String aScriptPath, String aMethodName, Object... aArgs) throws Exception {
            // 获取脚本的类
            Class<?> tScriptClass = GROOVY_INTERP.getClassLoader().parseClass(toSourceFile(aScriptPath));
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            // 如果是脚本则使用脚本的调用方法的方式
            if (Script.class.isAssignableFrom(tScriptClass)) {
                // treat it just like a script if it is one
                try {
                    @SuppressWarnings({"unchecked", "CastCanBeRemovedNarrowingVariableType"})
                    final Script tScript = InvokerHelper.newScript((Class<? extends Script>) tScriptClass, new Binding()); // 这样保证 tContext 是干净的
                    return GroovyObjectWrapper.of(tScript.invokeMethod(aMethodName, fArgs)); // 脚本的方法原则上不需要考虑类型兼容的问题
                } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                    // ignore instantiation errors, try to run the static method in class
                }
            }
            // 获取兼容输入的类
            Method m = UT.Hack.findMethod_(tScriptClass, aMethodName, fArgs);
            // 如果有找到则需要转换参数到兼容的方法的参数类型上，避免无法转换的错误
            if (m == null) throw new GroovyRuntimeException("Cannot find method with compatible args: " + aMethodName);
            UT.Hack.convertArgs_(fArgs, m.getParameterTypes());
            // 注意使用 Groovy 的 InvokerHelper 来调用，避免意外的问题
            return GroovyObjectWrapper.of(InvokerHelper.invokeMethod(tScriptClass, aMethodName, fArgs));
        }
        /** 创建脚本类的实例 */
        public static Object newInstance(String aScriptPath, Object... aArgs) throws Exception {
            // 获取脚本的类
            Class<?> tScriptClass = GROOVY_INTERP.getClassLoader().parseClass(toSourceFile(aScriptPath));
            // 获取 ScriptClass 的实例
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            // 获取兼容输入参数的构造函数来创建实例
            Constructor<?> tConstructor = UT.Hack.findConstructor_(tScriptClass, fArgs);
            if (tConstructor == null) throw new GroovyRuntimeException("Cannot find constructor with compatible args: " + tScriptClass.getName());
            return GroovyObjectWrapper.of(tConstructor.newInstance(aArgs));
        }
        
        /** 提供一个手动关闭 GROOVY_INTERP 的接口，似乎不需要手动关闭 Interpreter，但是这里还是关闭一下内部的 ClassLoader */
        public static void close() throws IOException {if (GROOVY_INTERP != null) {GROOVY_INTERP.getClassLoader().close(); GROOVY_INTERP = null;}}
        public static boolean isClosed() {return GROOVY_INTERP == null;}
        /** 提供一个手动刷新 GROOVY_INTERP 的接口，可以将关闭的重新打开，清除缓存和文件的依赖 */
        public static void refresh() throws IOException {close(); initInterpreter_();}
        
        
        static {
            // 手动加载 CS.Exec，会自动重新设置工作目录，保证 Groovy 读取到的工作目录是正确的
            CS.Exec.InitHelper.init();
            // 在程序结束时关闭 CLASS_LOADER，最先添加来避免一些问题
            Main.addGlobalAutoCloseable(Groovy::close);
            // 先初始化配置
            GROOVY_CONF = new CompilerConfiguration();
            GROOVY_CONF.setSourceEncoding(StandardCharsets.UTF_8.name()); // 文件统一使用 utf-8 编码
            if (KERNEL_THREAD_INTERRUPT && Main.IS_KERNEL()) {
            GROOVY_CONF.addCompilationCustomizers(new ASTTransformationCustomizer(ThreadInterrupt.class));
            }
            // 初始化 CLASS_LOADER
            initInterpreter_();
        }
        /** 初始化内部的 CLASS_LOADER，主要用于减少重复代码 */
        private static void initInterpreter_() {
            // 重新指定 ClassLoader 为这个类的实际加载器
            GROOVY_INTERP = new Interpreter(SP.class.getClassLoader(), new Binding(), GROOVY_CONF);
            // 指定默认的 Groovy 脚本的类路径
            GROOVY_INTERP.getClassLoader().addClasspath(UT.IO.toAbsolutePath(GROOVY_SP_DIR));
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
            public static @Nullable String CMAKE_C_COMPILER = UT.Exec.env("JSE_CMAKE_C_COMPILER_JEP", jse.code.Conf.CMAKE_C_COMPILER);
            public static @Nullable String CMAKE_C_FLAGS    = UT.Exec.env("JSE_CMAKE_C_FLAGS_JEP"   , jse.code.Conf.CMAKE_C_FLAGS   );
            
            /** 重定向 jep 动态库的路径 */
            public static @Nullable String REDIRECT_JEP_LIB = UT.Exec.env("JSE_REDIRECT_JEP_LIB");
        }
        
        
        /** 包的版本 */
        private final static String JEP_VERSION = "4.2.0", ASE_VERSION = "3.22.1";
        /** python 离线包的路径以及 python 库的路径，这里采用 jar 包所在的绝对路径 */
        private final static String PYPKG_DIR = JAR_DIR+".pypkg/";
        private final static String PYLIB_DIR = JAR_DIR+"python/";
        private final static String JEP_LIB_DIR = JAR_DIR+"jep/" + UT.Code.uniqueID(VERSION, JEP_VERSION) + "/";
        private final static String JEP_LIB_PATH;
        /** 将 aScriptPath 合法化，现在可以省略掉 script/python/ 以及后缀 */
        private static String validScriptPath(String aScriptPath) throws IOException {
            @Nullable String tPath = findValidScriptPath(aScriptPath, ".py", PYTHON_SP_DIR);
            if (tPath == null) throw new FileNotFoundException(aScriptPath + " (" + UT.IO.toAbsolutePath(aScriptPath) + ")");
            return tPath;
        }
        /** 一样这里统一使用全局的一个解释器 */
        private static jep.Interpreter JEP_INTERP = null;
        
        
        /** python like stuffs，exec 不会获取返回值，eval 获取返回值 */
        public static void exec(String aText) throws JepException {JEP_INTERP.exec(aText);}
        public static void execFile(String aFilePath) throws JepException, IOException {JEP_INTERP.runScript(validScriptPath(aFilePath));}
        /** 由于 jep 的特性，这里可以直接使用 getValue 指定 eval */
        public static Object eval(String aText) throws JepException {return JEP_INTERP.getValue(aText);}
        // python 脚本文件不会有返回值
        
        /** 直接运行文本的脚本 */
        public static void runText(String aText, String... aArgs) throws JepException {setArgs_("", aArgs); JEP_INTERP.exec(aText);}
        /** Python 还可以使用 getValue 来获取变量以及 setValue 设置变量（原则上同样仅限于 Context 变量，允许可以超出 Context 但是不保证支持） */
        public static Object get(String aValueName) throws JepException {return getValue(aValueName);}
        public static void set(String aValueName, Object aValue) throws JepException {setValue(aValueName, aValue);}
        public static void remove(String aValueName) throws JepException {removeValue(aValueName);}
        public static Object getValue(String aValueName) throws JepException {return JEP_INTERP.getValue(aValueName);}
        public static void setValue(String aValueName, Object aValue) throws JepException {JEP_INTERP.set(aValueName, aValue);}
        public static void removeValue(String aValueName) throws JepException {JEP_INTERP.exec("del "+aValueName);}
        /** 运行脚本文件 */
        public static void run(String aScriptPath, String... aArgs) throws JepException, IOException {runScript(aScriptPath, aArgs);}
        public static void runScript(String aScriptPath, String... aArgs) throws JepException, IOException {
            // 现在不再保存旧的 sys.argv，如果需要可以在外部代码自行保存
            setArgs_(aScriptPath, aArgs);
            JEP_INTERP.runScript(validScriptPath(aScriptPath));
        }
        /** 调用方法，python 中需要结合 import 使用 */
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
        public static Object newInstance(String aClassName, Object... aArgs) throws JepException {return invoke(aClassName, aArgs);}
        
        /** 提供一个手动关闭 JEP_INTERP 的接口 */
        public static void close() throws JepException {if (JEP_INTERP != null) {JEP_INTERP.close(); JEP_INTERP = null;}}
        public static boolean isClosed() {return JEP_INTERP == null;}
        /** 提供一个手动刷新 JEP_INTERP 的接口，可以将关闭的重新打开，会清空所有创建的 python 变量 */
        public static void refresh() throws JepException {close(); initInterpreter_();}
        
        
        /** 提供一个方便 Groovy 中使用的直接访问类型的接口 */
        public static Object getClass(final String aClassName) throws JepException {
            return new GroovyObject() {
                @Override public Object invokeMethod(String name, Object args) throws JepException {return invoke(aClassName+"."+name, (Object[])args);}
                @Override public Object getProperty(String propertyName) throws JepException {return eval(aClassName+"."+propertyName);}
                @Override public void setProperty(String propertyName, Object newValue) throws JepException {setValue("_", newValue); exec(aClassName+"."+propertyName+" = _");}
                
                private MetaClass mDelegate = InvokerHelper.getMetaClass(getClass());
                @Override public MetaClass getMetaClass() {return mDelegate;}
                @Override public void setMetaClass(MetaClass metaClass) {mDelegate = metaClass;}
                
                /** 对于一般的类，call 直接调用构造函数而不是重载运算符 */
                public Object call(Object... aArgs) throws JepException {return newInstance(aClassName, aArgs);}
            };
        }
        /** 现在和 Groovy 逻辑保持一致，调用任何 run 都会全局重置 args 值 */
        private static void setArgs_(String aFirst, String[] aArgs) {
            List<String> tArgs = (aArgs==null || aArgs.length==0) ? Collections.singletonList(aFirst) : AbstractCollections.merge(aFirst, aArgs);
            // 使用这种方法保证设置成功
            setValue("_", tArgs);
            exec("sys.argv = _");
        }
        
        
        static {
            InitHelper.INITIALIZED = true;
            
            // 手动加载 CS.Exec，会自动重新设置工作目录，保证 jep 读取到的工作目录是正确的
            CS.Exec.InitHelper.init();
            // 在 JVM 关闭时关闭 JEP_INTERP，最先添加来避免一些问题
            Main.addGlobalAutoCloseable(Python::close);
            
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
                .addIncludePaths(UT.IO.toAbsolutePath(PYTHON_SP_DIR))
                .addIncludePaths(UT.IO.toAbsolutePath(PYLIB_DIR))
                .addIncludePaths(UT.IO.toAbsolutePath(JEP_LIB_DIR))
                .setClassLoader(SP.class.getClassLoader())
                .redirectStdout(System.out)
                .redirectStdErr(System.err));
            // 初始化 JEP_INTERP
            initInterpreter_();
        }
        /** 初始化内部的 JEP_INTERP，主要用于减少重复代码 */
        private static void initInterpreter_() {
            JEP_INTERP = new jep.SharedInterpreter();
            JEP_INTERP.exec("import sys");
        }
        
        
        /** 基于 pip 的 python 包管理，下载指定包到 .pypkg */
        public static void downloadPackage(String aRequirement, boolean aIncludeDep, String aPlatform, String aPythonVersion) {
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
            rCommand.add("--dest"); rCommand.add(String.format("\"%s\"", PYPKG_DIR));
            // 设置需要的包名
            rCommand.add(String.format("\"%s\"", aRequirement));
            
            // 直接通过系统指令执行 pip 来下载
            EXE.system(String.join(" ", rCommand));
        }
        public static void downloadPackage(String aRequirement, String aPlatform, String aPythonVersion) {downloadPackage(aRequirement, false, aPlatform, aPythonVersion);}
        public static void downloadPackage(String aRequirement, String aPlatform) {downloadPackage(aRequirement, aPlatform, null);}
        public static void downloadPackage(String aRequirement, boolean aIncludeDep, String aPlatform) {downloadPackage(aRequirement, aIncludeDep, aPlatform, null);}
        public static void downloadPackage(String aRequirement, boolean aIncludeDep) {downloadPackage(aRequirement, aIncludeDep, null);}
        public static void downloadPackage(String aRequirement) {downloadPackage(aRequirement, false);}
        
        /** 基于 pip 的 python 包管理，直接安装指定包到 lib */
        public static void installPackage(String aRequirement, boolean aIncludeDep, boolean aIncludeIndex) {
            // 组装指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("pip"); rCommand.add("install");
            // 是否顺便下载依赖的库，这里默认不会下载依赖库，因为问题会很多
            if (!aIncludeDep) rCommand.add("--no-deps");
            // 是否开启联网，这里默认不开启联网，因为标准下会使用 downloadPackage 来下载包
            if (!aIncludeIndex) rCommand.add("--no-index");
            // 添加 .pypkg 到搜索路径
            rCommand.add("--find-links"); rCommand.add(String.format("\"file:%s\"", PYPKG_DIR));
            // 设置目标路径
            rCommand.add("--target"); rCommand.add(String.format("\"%s\"", PYLIB_DIR));
            // 强制开启更新，替换已有的包
            rCommand.add("--upgrade");
            // 设置需要的包名
            rCommand.add(String.format("\"%s\"", aRequirement));
            
            // 直接通过系统指令执行 pip 来下载
            EXE.system(String.join(" ", rCommand));
        }
        public static void installPackage(String aRequirement, boolean aIncludeDep) {installPackage(aRequirement, aIncludeDep, false);}
        public static void installPackage(String aRequirement) {installPackage(aRequirement, false);}
        
        
        private static String cmakeInitCmdJep_(String aJepBuildDir) {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("cd"); rCommand.add("\""+aJepBuildDir+"\""); rCommand.add(";");
            rCommand.add("cmake");
            // 这里设置 C 编译器（如果有）
            if (Conf.CMAKE_C_COMPILER != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="+Conf.CMAKE_C_COMPILER  );}
            if (Conf.CMAKE_C_FLAGS    != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_FLAGS=\"" +Conf.CMAKE_C_FLAGS+"\"");}
            // 初始化使用上一个目录的 CMakeList.txt
            rCommand.add("..");
            return String.join(" ", rCommand);
        }
        private static String cmakeSettingCmdJep_(String aJepBuildDir) throws IOException {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("cd"); rCommand.add("\""+aJepBuildDir+"\""); rCommand.add(";");
            rCommand.add("cmake");
            // 设置构建输出目录为 lib
            UT.IO.makeDir(JEP_LIB_DIR); // 初始化一下这个目录避免意料外的问题
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY:PATH=\""+ JEP_LIB_DIR +"\"");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY:PATH=\""+ JEP_LIB_DIR +"\"");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY:PATH=\""+ JEP_LIB_DIR +"\"");
            rCommand.add("-D"); rCommand.add("CMAKE_ARCHIVE_OUTPUT_DIRECTORY_RELEASE:PATH=\""+ JEP_LIB_DIR +"\"");
            rCommand.add("-D"); rCommand.add("CMAKE_LIBRARY_OUTPUT_DIRECTORY_RELEASE:PATH=\""+ JEP_LIB_DIR +"\"");
            rCommand.add("-D"); rCommand.add("CMAKE_RUNTIME_OUTPUT_DIRECTORY_RELEASE:PATH=\""+ JEP_LIB_DIR +"\"");
            rCommand.add(".");
            return String.join(" ", rCommand);
        }
        
        /** 内部使用的安装 jep 的操作，和一般的库不同，jep 由于不能离线使用 pip 安装，这里直接使用源码编译 */
        private static @NotNull String installJep_() throws Exception {
            // 检测 cmake，这里要求一定要有 cmake 环境
            EXE.setNoSTDOutput().setNoERROutput();
            boolean tNoCmake = EXE.system("cmake --version") != 0;
            EXE.setNoSTDOutput(false).setNoERROutput(false);
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
            // 安装 jep 包，这里通过 cmake 来安装
            System.out.println("JEP INIT INFO: Installing jep from source code...");
            String tJepBuildDir = tJepDir+"build/";
            UT.IO.makeDir(tJepBuildDir);
            // 直接通过系统指令来编译 Jep 的库，关闭输出
            EXE.setNoSTDOutput();
            // 初始化 cmake
            EXE.system(cmakeInitCmdJep_(tJepBuildDir));
            // 设置参数
            EXE.system(cmakeSettingCmdJep_(tJepBuildDir));
            // 最后进行构造操作
            EXE.system(String.format("cd \"%s\"; cmake --build . --config Release", tJepBuildDir));
            EXE.setNoSTDOutput(false);
            // 简单检测一下是否编译成功
            @Nullable String tJepLibName = LIB_NAME_IN(JEP_LIB_DIR, "jep");
            if (tJepLibName == null) throw new Exception("JEP BUILD ERROR: No jep lib in "+JEP_LIB_DIR);
            // 拷贝 python 脚本
            String tJepPyDir = tJepDir+"src/main/python/jep/";
            String tJepLibPyDir = JEP_LIB_DIR+"jep/";
            UT.IO.removeDir(tJepLibPyDir); // 如果存在删除一下保证移动成功
            UT.IO.move(tJepPyDir, tJepLibPyDir);
            // 完事后移除临时解压得到的源码
            UT.IO.removeDir(tWorkingDir);
            System.out.println("JEP INIT INFO: jep successfully installed.");
            // 输出安装完成后的库名称
            return tJepLibName;
        }
        
        /** 一些内置的 python 库安装，主要用于内部使用 */
        public static void installAse() throws IOException {
            // 首先获取源码路径，这里直接检测是否是 ase-$ASE_VERSION 开头
            String[] tList = UT.IO.list(PYPKG_DIR);
            boolean tHasAsePkg = false;
            for (String tName : tList) if (tName.startsWith("ase-"+ASE_VERSION)) {
                tHasAsePkg = true; break;
            }
            // 如果没有 ase 包则直接下载，指定版本 ASE_VERSION 避免因为更新造成的问题
            if (!tHasAsePkg) {
                System.out.printf("ASE INIT INFO: No ase package in %s, downloading...\n", PYPKG_DIR);
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
