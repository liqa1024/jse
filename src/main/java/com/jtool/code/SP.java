package com.jtool.code;

import com.jtool.system.ISystemExecutor;
import com.jtool.system.LocalSystemExecutor;
import com.jtool.system.PowerShellSystemExecutor;
import groovy.lang.*;
import jep.*;
import jep.python.PyObject;
import org.codehaus.groovy.runtime.InvokerHelper;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

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
        private static GroovyClassLoader CLASS_LOADER = null;
        
        /** 直接运行文本的脚本，底层不会进行缓存 */
        public synchronized static Object runText(String aText, String... aArgs) throws Exception {return getTaskOfText(aText, aArgs).call();}
        /** 运行脚本文件，底层会自动进行缓存 */
        public synchronized static Object run(String aScriptPath, String... aArgs) throws Exception {return runScript(aScriptPath, aArgs);}
        public synchronized static Object runScript(String aScriptPath, String... aArgs) throws Exception {return getTaskOfScript(aScriptPath, aArgs).call();}
        /** 调用指定脚本中的方法，会进行缓存 */
        public synchronized static Object invoke(String aScriptPath, String aMethodName, Object... aArgs) throws Exception {return getTaskOfScriptMethod(aScriptPath, aMethodName, aArgs).call();}
        /** 创建脚本类的实例 */
        public synchronized static IScriptObject newInstance(String aScriptPath, Object... aArgs) throws Exception {
            // 获取脚本的类，底层自动进行了缓存，并且在文件修改时会自动更新
            Class<?> tScriptClass = CLASS_LOADER.parseClass(UT.IO.toFile(aScriptPath));
            // 获取 ScriptClass 的实例
            return newInstance_(tScriptClass, aArgs);
        }
        
        /** 提供一个手动关闭 CLASS_LOADER 的接口 */
        public synchronized static void close() throws IOException {if (CLASS_LOADER != null) {CLASS_LOADER.close(); CLASS_LOADER = null;}}
        public synchronized static boolean isClosed() {return CLASS_LOADER == null;}
        /** 提供一个手动刷新 CLASS_LOADER 的接口，可以将关闭的重新打开，清除缓存和文件的依赖 */
        public synchronized static void refresh() throws IOException {close(); initClassLoader_();}
        
        
        /** 获取脚本相关的 task，对于脚本的内容请使用这里的接口而不是 {@link UT.Hack}.getTaskOfStaticMethod */
        public synchronized static TaskCall<?> getTaskOfText(String aText, String... aArgs) {
            // 获取文本脚本的类，由于是文本底层自动不进行缓存
            Class<?> tScriptClass = CLASS_LOADER.parseClass(aText);
            // 获取 ScriptClass 的执行 Task
            return getTaskOfScript_(tScriptClass, aArgs);
        }
        public synchronized static TaskCall<?> getTaskOfScript(String aScriptPath, String... aArgs) throws IOException {
            // 获取脚本的类，底层自动进行了缓存
            Class<?> tScriptClass = CLASS_LOADER.parseClass(UT.IO.toFile(aScriptPath));
            // 获取 ScriptClass 的执行 Task
            return getTaskOfScript_(tScriptClass, aArgs);
        }
        /** 注意是脚本中的方法或者是类中静态方法，成员方法可以获取对象后直接用 {@link UT.Hack}.getTaskOfMethod */
        public synchronized static TaskCall<?> getTaskOfScriptMethod(String aScriptPath, final String aMethodName, Object... aArgs) throws IOException {
            // 获取脚本的类，底层自动进行了缓存
            Class<?> tScriptClass = CLASS_LOADER.parseClass(UT.IO.toFile(aScriptPath));
            // 获取 ScriptClass 中具体方法的 Task
            return getTaskOfScriptMethod_(tScriptClass, aMethodName, aArgs);
        }
        
        
        /** 内部使用的方法，用于减少重复代码 */
        public synchronized static ScriptObjectGroovy newInstance_(Class<?> aScriptClass, Object... aArgs) throws InvocationTargetException, InstantiationException, IllegalAccessException {
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            // 获取兼容输入参数的构造函数来创建实例
            Constructor<?> tConstructor = UT.Hack.findConstructor_(aScriptClass, fArgs);
            if (tConstructor == null) throw new GroovyRuntimeException("Cannot find constructor with compatible args: " + aScriptClass.getName());
            return new ScriptObjectGroovy((GroovyObject)tConstructor.newInstance(aArgs));
        }
        public synchronized static TaskCall<?> getTaskOfScript_(final Class<?> aScriptClass, String... aArgs) {
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            // 和 runScriptOrMainOrTestOrRunnable 保持一样的逻辑，不过现在是线程安全的了，不考虑 Test 和 Runnable 的情况
            if (Script.class.isAssignableFrom(aScriptClass)) {
                // 这样保证 tContext 是干净的
                Binding tContext = new Binding();
                tContext.setProperty("args", fArgs);
                // treat it just like a script if it is one
                try {
                    @SuppressWarnings("unchecked")
                    Script tScript = InvokerHelper.newScript((Class<? extends Script>) aScriptClass, tContext);
                    return new TaskCall<>(tScript::run);
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
            return new TaskCall<>(() -> InvokerHelper.invokeMethod(aScriptClass, MAIN_METHOD_NAME, new Object[]{fArgs}));
        }
        public synchronized static TaskCall<?> getTaskOfScriptMethod_(final Class<?> aScriptClass, final String aMethodName, Object... aArgs) {
            final Object[] fArgs = (aArgs == null) ? new Object[0] : aArgs;
            // 如果是脚本则使用脚本的调用方法的方式
            if (Script.class.isAssignableFrom(aScriptClass)) {
                // treat it just like a script if it is one
                try {
                    @SuppressWarnings("unchecked")
                    final Script tScript = InvokerHelper.newScript((Class<? extends Script>) aScriptClass, new Binding()); // 这样保证 tContext 是干净的
                    return new TaskCall<>(() -> tScript.invokeMethod(aMethodName, fArgs)); // 脚本的方法原则上不需要考虑类型兼容的问题
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
            return new TaskCall<>(() -> InvokerHelper.invokeMethod(aScriptClass, aMethodName, fArgs));
        }
        
        static {
            // 手动加载 UT，会自动重新设置工作目录，会在调用静态函数 get 或者 load 时自动加载保证路径的正确性
            UT.IO.init();
            // 初始化 CLASS_LOADER
            initClassLoader_();
            // 在 JVM 关闭时关闭 CLASS_LOADER
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {close();} catch (Exception ignored) {}
            }));
        }
        /** 初始化内部的 CLASS_LOADER，主要用于减少重复代码 */
        private synchronized static void initClassLoader_() {
            // 重新指定 ClassLoader 为这个类的实际加载器
            CLASS_LOADER = new GroovyClassLoader(SP.class.getClassLoader());
            // 指定默认的 Groovy 脚本的类路径
            CLASS_LOADER.addClasspath(UT.IO.toAbsolutePath("script/groovy/"));
        }
    }
    
    
    
    /** Python 脚本运行支持，完全基于 jep */
    public static class Python {
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
        public synchronized static void run(String aScriptPath) throws JepException {runScript(aScriptPath);}
        public synchronized static void runScript(String aScriptPath) throws JepException {JEP_INTERP.runScript(aScriptPath);}
        /** 调用方法，python 中需要结合 import 使用 */
        public synchronized static Object invoke(String aMethodName, Object... aArgs) throws JepException {return JEP_INTERP.invoke(aMethodName, aArgs);}
        public synchronized static Object invoke(String aMethodName, Map<String, Object> aMapArgs) throws JepException {return JEP_INTERP.invoke(aMethodName, aMapArgs);}
        public synchronized static Object invoke(String aMethodName, Object[] aArgs, Map<String, Object> aMapArgs) throws JepException {return JEP_INTERP.invoke(aMethodName, aArgs, aMapArgs);}
        public synchronized static void importModule(String aPyModuleName) throws JepException {JEP_INTERP.exec("import "+aPyModuleName);}
        /** 创建 Python 实例，这里同样外套一层 */
        public synchronized static IScriptObject newInstance(String aClassName, Object... aArgs) throws JepException {return new ScriptObjectPython((PyObject)JEP_INTERP.invoke(aClassName, aArgs));}
        public synchronized static IScriptObject newInstance(String aClassName, Map<String, Object> aMapArgs) throws JepException {return new ScriptObjectPython((PyObject)JEP_INTERP.invoke(aClassName, aMapArgs));}
        public synchronized static IScriptObject newInstance(String aClassName, Object[] aArgs, Map<String, Object> aMapArgs) throws JepException {return new ScriptObjectPython((PyObject)JEP_INTERP.invoke(aClassName, aArgs, aMapArgs));}
        
        /** 提供一个手动关闭 JEP_INTERP 的接口 */
        public synchronized static void close() throws JepException {if (JEP_INTERP != null) {JEP_INTERP.close(); JEP_INTERP = null;}}
        public synchronized static boolean isClosed() {return JEP_INTERP == null;}
        /** 提供一个手动刷新 JEP_INTERP 的接口，可以将关闭的重新打开，会清空所有创建的 python 变量 */
        public synchronized static void refresh() throws JepException {close(); initInterpreter_();}
        
        // 由于 Python 语言特性，不能并行执行 python 函数，因此这里不提供获取相关 task 的接口
        
        
        static {
            // 手动加载 UT，会自动重新设置工作目录，会在调用静态函数 get 或者 load 时自动加载保证路径的正确性
            UT.IO.init();
            // 设置 Jep 非 java 库的路径，考虑到 WSL，windows 和 linux 使用不同的名称
            initJepLib_(System.getProperty("os.name").toLowerCase().contains("windows"));
            // 配置 Jep，这里只能配置一次
            SharedInterpreter.setConfig(new JepConfig()
                .addIncludePaths(UT.IO.toAbsolutePath("script/python/"))
                .addIncludePaths(UT.IO.toAbsolutePath("lib/"))
                .setClassLoader(SP.class.getClassLoader())
                .redirectStdout(System.out)
                .redirectStdErr(System.err));
            // 初始化 JEP_INTERP
            initInterpreter_();
            // 在 JVM 关闭时关闭 JEP_INTERP
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {close();} catch (Exception ignored) {}
            }));
        }
        /** 初始化内部的 JEP_INTERP，主要用于减少重复代码 */
        private synchronized static void initInterpreter_() {JEP_INTERP = new SharedInterpreter();}
        /** 初始化 JEP 需要使用的外部库，需要平台至少拥有 python3 环境 */
        private synchronized static void initJepLib_(boolean aIsWindows) {
            // 考虑到 WSL，windows 和 linux 使用不同的名称
            String tLibPath = aIsWindows ? "lib/jep.dll" : "lib/jep.so";
            // 如果不存在则需要重新通过源码编译
            if (!UT.IO.isFile(tLibPath)) {
                System.out.println("JEP INIT INFO: Jep libraries not found. Compiling from source code...");
                // 首先获取源码路径，这里只检测 jep 开头的文件夹
                String[] tList = UT.IO.list(".src/");
                String tJepDirName = null;
                for (String tName : tList) if (tName.contains("jep")) {
                    tJepDirName = tName;
                }
                if (tJepDirName == null) throw new RuntimeException("JEP INIT ERROR: No Jep source code in .src");
                // 直接通过系统指令来编译 Jep 的库，windows 下使用 powershell 统一指令
                try (ISystemExecutor tEXE = aIsWindows ? new PowerShellSystemExecutor() : new LocalSystemExecutor()) {
                    tEXE.setNoSTDOutput().setNoERROutput();
                    tEXE.system(String.format("cd .src/%s; python setup.py clean", tJepDirName)); // 编译之前先移除旧的结果
                    tEXE.system(String.format("cd .src/%s; python setup.py build", tJepDirName));
                }
                // 获取 build 目录下的 lib 文件夹
                tList = UT.IO.list(String.format(".src/%s/build", tJepDirName));
                String tJepLibDirName = null;
                for (String tName : tList) if (tName.contains("lib")) {
                    tJepLibDirName = tName;
                }
                if (tJepLibDirName == null) throw new RuntimeException(String.format("JEP BUILD ERROR: No Jep lib in .src/%s/build", tJepDirName));
                // 获取 lib 文件夹下的 lib 名称
                tList = UT.IO.list(String.format(".src/%s/build/%s/jep", tJepDirName, tJepLibDirName));
                String tJepLibName = null;
                for (String tName : tList) if (tName.contains("jep") && (tName.endsWith(".dll") || tName.endsWith(".so") || tName.endsWith(".jnilib"))) {
                    tJepLibName = tName;
                }
                if (tJepLibName == null) throw new RuntimeException(String.format("JEP BUILD ERROR: No Jep lib in .src/%s/build/%s/jep", tJepDirName, tJepLibDirName));
                try {
                    // 将 build 的输出拷贝到 lib 目录下
                    UT.IO.copy(String.format(".src/%s/build/%s/jep/%s", tJepDirName, tJepLibDirName, tJepLibName), tLibPath);
                    // 顺便拷贝生成的 python 源码
                    UT.IO.makeDir("lib/jep");
                    tList = UT.IO.list(String.format(".src/%s/build/%s/jep", tJepDirName, tJepLibDirName));
                    for (String tName : tList) if (tName.endsWith(".py")) {
                        UT.IO.copy(String.format(".src/%s/build/%s/jep/%s", tJepDirName, tJepLibDirName, tName), String.format("lib/jep/%s", tName));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("JEP INIT INFO: Jep libraries successfully installed.");
            }
            // 设置路径
            MainInterpreter.setJepLibraryPath(UT.IO.toAbsolutePath(tLibPath));
        }
    }
}
