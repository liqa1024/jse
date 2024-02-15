package jse.code;

import groovy.lang.*;
import jep.*;
import jline.Terminal;
import jline.WindowsTerminal;
import jse.Main;
import jse.atom.AbstractAtoms;
import jse.atom.Structures;
import jse.code.collection.AbstractCollections;
import jse.code.collection.ArrayLists;
import jse.code.collection.Iterables;
import jse.code.collection.NewCollections;
import jse.code.task.TaskCall;
import jse.io.IOFiles;
import jse.io.InFiles;
import jse.math.ComplexDouble;
import jse.math.MathEX;
import jse.math.function.Func1;
import jse.math.matrix.Matrices;
import jse.math.table.Tables;
import jse.math.vector.Vectors;
import jse.plot.Plotters;
import org.apache.groovy.groovysh.Groovysh;
import org.apache.groovy.groovysh.InteractiveShellRunner;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.tools.shell.IO;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static jse.code.CS.Exec.*;
import static jse.code.CS.VERSION;
import static jse.code.Conf.WORKING_DIR_OF;
import static org.codehaus.groovy.runtime.InvokerHelper.MAIN_METHOD_NAME;

/**
 * @author liqa
 * <p> 运行脚本（script）的通用类，目前支持运行 Groovy 脚本和 Python 脚本 </p>
 * <p> 为了方便调用这里使用纯静态的类来实现，为了线程安全所有方法都加上锁 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public class SP {
    private SP() {}
    
    /** Groovy 脚本运行支持 */
    public static class Groovy {
        /** Wrapper of {@link GroovyObject} for matlab usage */
        public final static class GroovyObjectWrapper implements GroovyObject {
            private final GroovyObject mObj;
            GroovyObjectWrapper(GroovyObject aObj) {mObj = aObj;}
            
            @Override public Object invokeMethod(String name, Object args) {return of(mObj.invokeMethod(name, args));}
            @Override public Object getProperty(String propertyName) {return of(mObj.getProperty(propertyName));}
            @Override public void setProperty(String propertyName, Object newValue) {mObj.setProperty(propertyName, newValue);}
            @Override public MetaClass getMetaClass() {return mObj.getMetaClass();}
            @Override public void setMetaClass(MetaClass metaClass) {mObj.setMetaClass(metaClass);}
            
            /** 主要用来判断是否需要外包这一层 */
            public static Object of(Object aObj) {return (!(aObj instanceof GroovyObjectWrapper) && (aObj instanceof GroovyObject)) ? (new GroovyObjectWrapper((GroovyObject)aObj)) : aObj;}
        }
        
        private final static String GROOVY_SP_DIR = "script/groovy/";
        /** 将 aScriptPath 转换成 GroovyCodeSource，现在可以省略掉 script/groovy/ 以及后缀 */
        private static GroovyCodeSource toSourceFile(String aScriptPath) throws IOException {
            // 如果不是 .groovy 后缀则有限检测带有后缀的，和 .bat 脚本类似的逻辑，可以保证同名脚本共存
            if (!aScriptPath.endsWith(".groovy")) {
                @Nullable GroovyCodeSource tFile = toScriptFile_(aScriptPath+".groovy");
                if (tFile != null) return tFile;
            }
            @Nullable GroovyCodeSource tFile = toScriptFile_(aScriptPath);
            if (tFile == null) throw new FileNotFoundException(aScriptPath + " (" + UT.IO.toAbsolutePath(aScriptPath) + ")");
            return tFile;
        }
        /** 返回 null 表示没有找到文件 */
        private static @Nullable GroovyCodeSource toScriptFile_(String aScriptPath) throws IOException {
            // 首先如果此文件存在则直接返回
            File tFile = UT.IO.toFile(aScriptPath);
            if (tFile.isFile()) return new GroovyCodeSource(tFile, "UTF-8"); // 文件统一使用 utf-8 编码
            // 否则增加 script/groovy/ 后再次检测
            tFile = UT.IO.toFile(GROOVY_SP_DIR+aScriptPath);
            if (tFile.isFile()) return new GroovyCodeSource(tFile, "UTF-8"); // 文件统一使用 utf-8 编码
            // 否则返回 null
            return null;
        }
        private static GroovyClassLoader CLASS_LOADER = null;
        
        /** 获取 shell 的交互式运行 */
        public synchronized static void runShell() throws Exception {
            // 使用这个方法来自动设置种类
            org.apache.groovy.groovysh.Main.setTerminalType("auto", false);
            // 这样手动指定 CLASS_LOADER
            Groovysh tGroovysh = new Groovysh(CLASS_LOADER, new Binding(), new IO()) {
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
        
        /** 直接运行文本的脚本，底层不会进行缓存 */
        public synchronized static Object runText(String aText, String... aArgs) throws Exception {return getCallableOfText(aText, aArgs).call();}
        /** 运行脚本文件，底层会自动进行缓存 */
        public synchronized static Object run(String aScriptPath, String... aArgs) throws Exception {return runScript(aScriptPath, aArgs);}
        public synchronized static Object runScript(String aScriptPath, String... aArgs) throws Exception {return getCallableOfScript(aScriptPath, aArgs).call();}
        /** 调用指定脚本中的方法，会进行缓存 */
        public synchronized static Object invoke(String aScriptPath, String aMethodName, Object... aArgs) throws Exception {return getCallableOfScriptMethod(aScriptPath, aMethodName, aArgs).call();}
        /** 创建脚本类的实例 */
        public synchronized static Object newInstance(String aScriptPath, Object... aArgs) throws Exception {
            // 获取脚本的类，底层自动进行了缓存，并且在文件修改时会自动更新
            Class<?> tScriptClass = CLASS_LOADER.parseClass(toSourceFile(aScriptPath));
            // 获取 ScriptClass 的实例
            return newInstance_(tScriptClass, aArgs);
        }
        
        /** 提供一个手动关闭 CLASS_LOADER 的接口 */
        public synchronized static void close() throws IOException {if (CLASS_LOADER != null) {CLASS_LOADER.close(); CLASS_LOADER = null;}}
        public synchronized static boolean isClosed() {return CLASS_LOADER == null;}
        /** 提供一个手动刷新 CLASS_LOADER 的接口，可以将关闭的重新打开，清除缓存和文件的依赖 */
        public synchronized static void refresh() throws IOException {close(); initClassLoader_();}
        
        
        /** 获取脚本相关的 task，对于脚本的内容请使用这里的接口而不是 {@link UT.Hack}.getTaskOfStaticMethod */
        public synchronized static TaskCall<?> getCallableOfText(String aText, String... aArgs) {
            // 获取文本脚本的类，由于是文本底层自动不进行缓存
            Class<?> tScriptClass = CLASS_LOADER.parseClass(aText);
            // 获取 ScriptClass 的执行 Task
            return getCallableOfScript_(tScriptClass, aArgs);
        }
        public synchronized static TaskCall<?> getCallableOfScript(String aScriptPath, String... aArgs) throws IOException {
            // 获取脚本的类，底层自动进行了缓存
            Class<?> tScriptClass = CLASS_LOADER.parseClass(toSourceFile(aScriptPath));
            // 获取 ScriptClass 的执行 Task
            return getCallableOfScript_(tScriptClass, aArgs);
        }
        /** 注意是脚本中的方法或者是类中静态方法，成员方法可以获取对象后直接用 {@link UT.Hack}.getTaskOfMethod */
        public synchronized static TaskCall<?> getCallableOfScriptMethod(String aScriptPath, final String aMethodName, Object... aArgs) throws IOException {
            // 获取脚本的类，底层自动进行了缓存
            Class<?> tScriptClass = CLASS_LOADER.parseClass(toSourceFile(aScriptPath));
            // 获取 ScriptClass 中具体方法的 Task
            return getCallableOfScriptMethod_(tScriptClass, aMethodName, aArgs);
        }
        
        
        /** 内部使用的方法，用于减少重复代码 */
        public synchronized static Object newInstance_(Class<?> aScriptClass, Object... aArgs) throws InvocationTargetException, InstantiationException, IllegalAccessException {
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            // 获取兼容输入参数的构造函数来创建实例
            Constructor<?> tConstructor = UT.Hack.findConstructor_(aScriptClass, fArgs);
            if (tConstructor == null) throw new GroovyRuntimeException("Cannot find constructor with compatible args: " + aScriptClass.getName());
            return GroovyObjectWrapper.of(tConstructor.newInstance(aArgs));
        }
        public synchronized static TaskCall<?> getCallableOfScript_(final Class<?> aScriptClass, String... aArgs) {
            final String[] fArgs = (aArgs == null) ? new String[0] : aArgs;
            // 和 runScriptOrMainOrTestOrRunnable 保持一样的逻辑，不过现在是线程安全的了，不考虑 Test 和 Runnable 的情况
            if (Script.class.isAssignableFrom(aScriptClass)) {
                // 这样保证 tContext 是干净的
                Binding tContext = new Binding();
                tContext.setProperty("args", fArgs);
                // treat it just like a script if it is one
                try {
                    @SuppressWarnings("unchecked")
                    final Script tScript = InvokerHelper.newScript((Class<? extends Script>) aScriptClass, tContext);
                    return new TaskCall<>(() -> GroovyObjectWrapper.of(tScript.run()));
                } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                    // ignore instantiation errors, try to do main
                }
            }
            // let's find a main method
            try {
                aScriptClass.getMethod(MAIN_METHOD_NAME, String[].class);
            } catch (NoSuchMethodException e) {
                String tMsg = "This script or class could not be run.\n" +
                    "It should either:\n" +
                    "- be a script format\n" +
                    "- have a main method\n";
                throw new GroovyRuntimeException(tMsg);
            }
            // if that main method exist, invoke it
            return new TaskCall<>(() -> GroovyObjectWrapper.of(InvokerHelper.invokeMethod(aScriptClass, MAIN_METHOD_NAME, new Object[]{fArgs})));
        }
        public synchronized static TaskCall<?> getCallableOfScriptMethod_(final Class<?> aScriptClass, final String aMethodName, Object... aArgs) {
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            // 如果是脚本则使用脚本的调用方法的方式
            if (Script.class.isAssignableFrom(aScriptClass)) {
                // treat it just like a script if it is one
                try {
                    @SuppressWarnings("unchecked")
                    final Script tScript = InvokerHelper.newScript((Class<? extends Script>) aScriptClass, new Binding()); // 这样保证 tContext 是干净的
                    return new TaskCall<>(() -> GroovyObjectWrapper.of(tScript.invokeMethod(aMethodName, fArgs))); // 脚本的方法原则上不需要考虑类型兼容的问题
                } catch (InstantiationException | InvocationTargetException | IllegalAccessException | NoSuchMethodException e) {
                    // ignore instantiation errors, try to run the static method in class
                }
            }
            // 获取兼容输入的类
            Method m = UT.Hack.findMethod_(aScriptClass, aMethodName, fArgs);
            // 如果有找到则需要转换参数到兼容的方法的参数类型上，避免无法转换的错误
            if (m == null) throw new GroovyRuntimeException("Cannot find method with compatible args: " + aMethodName);
            UT.Hack.convertArgs_(fArgs, m.getParameterTypes());
            // 注意使用 Groovy 的 InvokerHelper 来调用，避免意外的问题
            return new TaskCall<>(() -> GroovyObjectWrapper.of(InvokerHelper.invokeMethod(aScriptClass, aMethodName, fArgs)));
        }
        
        static {
            // 手动加载 CS.Exec，会自动重新设置工作目录，保证 Groovy 读取到的工作目录是正确的
            CS.Exec.InitHelper.init();
            // 在程序结束时关闭 CLASS_LOADER，最先添加来避免一些问题
            Main.addGlobalAutoCloseable(Groovy::close);
            // 初始化 CLASS_LOADER
            initClassLoader_();
        }
        /** 初始化内部的 CLASS_LOADER，主要用于减少重复代码 */
        private synchronized static void initClassLoader_() {
            // 重新指定 ClassLoader 为这个类的实际加载器
            CLASS_LOADER = new GroovyClassLoader(SP.class.getClassLoader());
            // 指定默认的 Groovy 脚本的类路径
            CLASS_LOADER.addClasspath(UT.IO.toAbsolutePath(GROOVY_SP_DIR));
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
             * cmake 有时不能自动检测到希望使用的编译器
             */
            public static @Nullable String CMAKE_C_COMPILER   = UT.Exec.env("JSE_CMAKE_C_COMPILER");
            public static @Nullable String CMAKE_CXX_COMPILER = UT.Exec.env("JSE_CMAKE_CXX_COMPILER");
        }
        
        
        /** 包的版本 */
        private final static String JEP_VERSION = "4.2.0", ASE_VERSION = "3.22.1";
        /** python 离线包的路径以及 python 库的路径，这里采用 jar 包所在的绝对路径 */
        private final static String PYPKG_DIR = JAR_DIR+".pypkg/";
        private final static String PYLIB_DIR = JAR_DIR+"python/";
        private final static String JEPLIB_DIR = JAR_DIR+"jep/" + UT.Code.uniqueID(VERSION, JEP_VERSION) + "/";
        private final static String JEPLIB_PATH = JEPLIB_DIR + "jepjni" + JNILIB_EXTENSION;
        private final static String PYTHON_SP_DIR = "script/python/";
        /** 将 aScriptPath 合法化，现在可以省略掉 script/python/ 以及后缀 */
        private static String validScriptPath(String aScriptPath) throws IOException {
            // 如果不是 .py 后缀则有限检测带有后缀的，和 .bat 脚本类似的逻辑，可以保证同名脚本共存
            if (!aScriptPath.endsWith(".py")) {
                @Nullable String tPath = validScriptPath_(aScriptPath+".py");
                if (tPath != null) return tPath;
            }
            @Nullable String tPath = validScriptPath_(aScriptPath);
            if (tPath == null) throw new FileNotFoundException(aScriptPath + " (" + UT.IO.toAbsolutePath(aScriptPath) + ")");
            return tPath;
        }
        /** 返回 null 表示没有找到文件 */
        private static @Nullable String validScriptPath_(String aScriptPath) {
            // 都转为绝对路径避免意料外的问题
            String tPath = UT.IO.toAbsolutePath(aScriptPath);
            // 首先如果此文件存在则直接返回
            if (UT.IO.isFile(tPath)) return tPath;
            // 否则增加 script/python/ 后再次检测
            tPath = UT.IO.toAbsolutePath(PYTHON_SP_DIR+aScriptPath);
            if (UT.IO.isFile(tPath)) return tPath;
            // 否则返回 null
            return null;
        }
        /** 一样这里统一使用全局的一个解释器 */
        private static Interpreter JEP_INTERP = null;
        
        /** 直接运行文本的脚本 */
        public synchronized static void runText(String aText) throws JepException {JEP_INTERP.exec(aText);}
        /** Python 还可以使用 getValue 来获取变量以及 setValue 设置变量 */
        public synchronized static Object getValue(String aValueName) throws JepException {return JEP_INTERP.getValue(aValueName);}
        public synchronized static void setValue(String aValueName, Object aValue) throws JepException {JEP_INTERP.set(aValueName, aValue);}
        public synchronized static Object get(String aValueName) throws JepException {return getValue(aValueName);}
        public synchronized static void set(String aValueName, Object aValue) throws JepException {setValue(aValueName, aValue);}
        /** 运行脚本文件 */
        public synchronized static void run(String aScriptPath) throws JepException, IOException {runScript(aScriptPath);}
        public synchronized static void runScript(String aScriptPath) throws JepException, IOException {JEP_INTERP.runScript(validScriptPath(aScriptPath));}
        /** 调用方法，python 中需要结合 import 使用 */
        @SuppressWarnings("unchecked")
        public synchronized static Object invoke(String aMethodName, Object... aArgs) throws JepException {
            if (aArgs == null || aArgs.length == 0) return JEP_INTERP.invoke(aMethodName);
            if (aArgs.length == 1 && (aArgs[0] instanceof Map)) return JEP_INTERP.invoke(aMethodName, (Map<String, Object>)aArgs[0]);
            if (aArgs.length > 1 && (aArgs[aArgs.length-1] instanceof Map)) {
                Object[] tArgs = new Object[aArgs.length-1];
                System.arraycopy(aArgs, 0, tArgs, 0, aArgs.length-1);
                return JEP_INTERP.invoke(aMethodName, tArgs, (Map<String, Object>)aArgs[aArgs.length-1]);
            }
            return JEP_INTERP.invoke(aMethodName, aArgs);
        }
        public synchronized static void importAny(String aName) throws JepException {runText("import " + aName);}
        public synchronized static Object importModule(String aModuleName) throws JepException {return invoke("import_module", aModuleName);}
        /** 创建 Python 实例，这里可以直接将类名当作函数调用即可 */
        public synchronized static Object newInstance(String aClassName, Object... aArgs) throws JepException {return invoke(aClassName, aArgs);}
        /** 提供一个方便直接访问类型的接口，由于 jep 没有提供相关接口这里直接使用字符串的形式来实现 */
        public synchronized static Object getClass(final String aClassName) throws JepException {
            return new GroovyObject() {
                @Override public Object invokeMethod(String name, Object args) throws JepException {return invoke(aClassName+"."+name, (Object[])args);}
                @Override public Object getProperty(String propertyName) throws JepException {return getValue(aClassName+"."+propertyName);}
                @Override public void setProperty(String propertyName, Object newValue) throws JepException {setValue(aClassName+"."+propertyName, newValue);}
                
                private MetaClass mDelegate = InvokerHelper.getMetaClass(getClass());
                @Override public MetaClass getMetaClass() {return mDelegate;}
                @Override public void setMetaClass(MetaClass metaClass) {mDelegate = metaClass;}
                
                /** 对于一般的类，call 直接调用构造函数而不是重载运算符 */
                public Object call(Object... aArgs) throws JepException {return newInstance(aClassName, aArgs);}
            };
        }
        
        /** 提供一个手动关闭 JEP_INTERP 的接口 */
        public synchronized static void close() throws JepException {if (JEP_INTERP != null) {JEP_INTERP.close(); JEP_INTERP = null;}}
        public synchronized static boolean isClosed() {return JEP_INTERP == null;}
        /** 提供一个手动刷新 JEP_INTERP 的接口，可以将关闭的重新打开，会清空所有创建的 python 变量 */
        public synchronized static void refresh() throws JepException {close(); initInterpreter_();}
        
        // 由于 Python 语言特性，不能并行执行 python 函数，因此这里不提供获取相关 task 的接口
        
        
        static {
            InitHelper.INITIALIZED = true;
            
            // 手动加载 CS.Exec，会自动重新设置工作目录，保证 jep 读取到的工作目录是正确的
            CS.Exec.InitHelper.init();
            // 在 JVM 关闭时关闭 JEP_INTERP，最先添加来避免一些问题
            Main.addGlobalAutoCloseable(Python::close);
            // 设置 Jep 非 java 库的路径，考虑到 WSL，windows 和 linux 使用不同的名称
            initJepLib_();
            // 配置 Jep，这里只能配置一次
            SharedInterpreter.setConfig(new JepConfig()
                .addIncludePaths(UT.IO.toAbsolutePath(PYTHON_SP_DIR))
                .addIncludePaths(UT.IO.toAbsolutePath(PYLIB_DIR))
                .addIncludePaths(UT.IO.toAbsolutePath(JEPLIB_DIR))
                .setClassLoader(SP.class.getClassLoader())
                .redirectStdout(System.out)
                .redirectStdErr(System.err));
            // 初始化 JEP_INTERP
            initInterpreter_();
        }
        /** 初始化内部的 JEP_INTERP，主要用于减少重复代码 */
        private synchronized static void initInterpreter_() {
            JEP_INTERP = new SharedInterpreter();
            JEP_INTERP.exec("from importlib import import_module");
        }
        /** 初始化 JEP 需要使用的外部库，需要平台至少拥有 python3 环境 */
        private synchronized static void initJepLib_() {
            // 如果不存在则需要重新通过源码编译
            if (!UT.IO.isFile(JEPLIB_PATH)) {
                System.out.println("JEP INIT INFO: jep libraries not found. Reinstalling...");
                try {installJep_();}
                catch (Exception e) {throw new RuntimeException(e);}
            }
            // 设置库路径
            MainInterpreter.setJepLibraryPath(UT.IO.toAbsolutePath(JEPLIB_PATH));
        }
        
        
        /** 基于 pip 的 python 包管理，下载指定包到 .pypkg */
        public synchronized static void downloadPackage(String aRequirement, boolean aIncludeDep, String aPlatform, String aPythonVersion) {
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
        public synchronized static void installPackage(String aRequirement, boolean aIncludeDep, boolean aIncludeIndex) {
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
        
        
        private static String cmakeInitCmd_(String aJepBuildDir) {
            // 设置参数，这里使用 List 来构造这个长指令
            List<String> rCommand = new ArrayList<>();
            rCommand.add("cd"); rCommand.add("\""+aJepBuildDir+"\""); rCommand.add(";");
            rCommand.add("cmake");
            // 这里设置 C/C++ 编译器（如果有）
            if (Conf.CMAKE_C_COMPILER   != null) {rCommand.add("-D"); rCommand.add("CMAKE_C_COMPILER="  + Conf.CMAKE_C_COMPILER  );}
            if (Conf.CMAKE_CXX_COMPILER != null) {rCommand.add("-D"); rCommand.add("CMAKE_CXX_COMPILER="+ Conf.CMAKE_CXX_COMPILER);}
            // 初始化使用上一个目录的 CMakeList.txt
            rCommand.add("..");
            return String.join(" ", rCommand);
        }
        
        /** 内部使用的安装 jep 的操作，和一般的库不同，jep 由于不能离线使用 pip 安装，这里直接使用源码编译 */
        private synchronized static void installJep_() throws Exception {
            // 检测 cmake，这里要求一定要有 cmake 环境
            EXE.setNoSTDOutput().setNoERROutput();
            boolean tNoCmake = EXE.system("cmake --version") != 0;
            EXE.setNoSTDOutput(false).setNoERROutput(false);
            if (tNoCmake) throw new Exception("JEP BUILD ERROR: No camke environment.");
            String tWorkingDir = WORKING_DIR_OF("jepsrc");
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
            EXE.system(cmakeInitCmd_(tJepBuildDir));
            // 最后进行构造操作
            EXE.system(String.format("cd \"%s\"; cmake --build . --config Release", tJepBuildDir));
            EXE.setNoSTDOutput(false);
            // 获取 build 目录下的 lib 文件夹
            String tJepLibDir = tJepBuildDir+"lib/";
            if (!UT.IO.isDir(tJepLibDir)) throw new Exception("JEP BUILD ERROR: No Jep lib in "+tJepBuildDir);
            // 获取 lib 文件夹下的 lib 名称
            String[] tList = UT.IO.list(tJepLibDir);
            String tJepLibPath = null;
            for (String tName : tList) if (tName.contains("jep") && (tName.endsWith(".dll") || tName.endsWith(".so") || tName.endsWith(".jnilib") || tName.endsWith(".dylib"))) {
                tJepLibPath = tName;
            }
            if (tJepLibPath == null) throw new Exception("JEP BUILD ERROR: No Jep lib in "+tJepLibDir);
            tJepLibPath = tJepLibDir+tJepLibPath;
            // 将 build 的输出拷贝到 lib 目录下
            UT.IO.copy(tJepLibPath, JEPLIB_PATH);
            // 顺便拷贝 python 脚本
            String tJepPyDir = tJepDir+"src/main/python/jep/";
            String tJepLibPyDir = JEPLIB_DIR+"jep/";
            UT.IO.removeDir(tJepLibPyDir); // 如果存在删除一下保证移动成功
            UT.IO.move(tJepPyDir, tJepLibPyDir);
            // 完事后移除临时解压得到的源码
            UT.IO.removeDir(tWorkingDir);
            System.out.println("JEP INIT INFO: jep successfully installed.");
        }
        
        /** 一些内置的 python 库安装，主要用于内部使用 */
        public synchronized static void installAse() throws IOException {
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
