package jse;

import io.github.spencerpark.jupyter.channels.JupyterConnection;
import io.github.spencerpark.jupyter.channels.JupyterSocket;
import io.github.spencerpark.jupyter.kernel.KernelConnectionProperties;
import jse.code.IO;
import jse.code.SP;
import jse.code.UT;
import jse.system.PythonSystemExecutor;
import org.apache.groovy.util.Maps;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import static jse.code.Conf.UNICODE_SUPPORT;
import static jse.code.CS.VERSION;
import static jse.code.Conf.WORKING_DIR_OF;
import static jse.code.OS.*;
import static jse.code.SP.GROOVY_LIB_DIR;
import static jse.code.SP.JAR_LIB_DIR;

/**
 * @author liqa
 * <p> 直接运行 jse 时的主要类，根据输入参数来决定具体操作 </p>
 */
public class Main {
    /** 记录 jse 是通过何种方式运行的，null 表示通过将 jar 添加到 class-path 运行 */
    private volatile static @Nullable String RUN_FROM = null;
    public static @Nullable String RUN_FROM() {return RUN_FROM;}
    private volatile static boolean IS_KERNEL = false;
    public static boolean IS_KERNEL() {return IS_KERNEL;}
    
    /** 使用这个替代原本的 addShutdownHook 的使用，注意要求线程安全以及避免死锁 */
    private final static Set<AutoCloseable> GLOBAL_AUTO_CLOSEABLE = new HashSet<>();
    public static void addGlobalAutoCloseable(@Nullable AutoCloseable aAutoCloseable) {
        if (aAutoCloseable != null) synchronized(GLOBAL_AUTO_CLOSEABLE) {GLOBAL_AUTO_CLOSEABLE.add(aAutoCloseable);}
    }
    public static void removeGlobalAutoCloseable(@Nullable AutoCloseable aAutoCloseable) {
        if (aAutoCloseable != null) synchronized(GLOBAL_AUTO_CLOSEABLE) {GLOBAL_AUTO_CLOSEABLE.remove(aAutoCloseable);}
    }
    public static void closeAllAutoCloseable() {
        // 需要先遍历添加到 List 固定，避免在 close 过程中被再次修改
        List<AutoCloseable> tGlobalAutoCloseable;
        synchronized(GLOBAL_AUTO_CLOSEABLE) {
            tGlobalAutoCloseable = new ArrayList<>(GLOBAL_AUTO_CLOSEABLE);
            GLOBAL_AUTO_CLOSEABLE.clear();
        }
        for (AutoCloseable tAutoCloseable : tGlobalAutoCloseable) {
            try {tAutoCloseable.close();} catch (Exception e) {UT.Code.printStackTrace(e);}
        }
    }
    
    @SuppressWarnings({"ThrowablePrintedToSystemOut", "UnnecessaryReturnStatement"})
    public static void main(String[] aArgs) throws Exception {
        try {
            // 完全没有输入时（双击运行）直接结束
            if (aArgs==null || aArgs.length==0) return;
            // 第 0 参数记录从何处运行的 jse
            RUN_FROM = aArgs[0];
            // 没有后续输入时启动 groovysh
            if (aArgs.length == 1) {SP.Groovy.runShell(); return;}
            // 获取第一个值
            String tValue = aArgs[1];
            if (!tValue.startsWith("-")) {
                // 默认执行脚本文件
                String[] tArgs = new String[aArgs.length-2];
                if (tArgs.length > 0) System.arraycopy(aArgs, 2, tArgs, 0, tArgs.length);
                SP.runScript(tValue, tArgs);
                return;
            }
            // 一般行为
            String tOption = tValue;
            switch (tOption) {
            case "-v": case "-version": {
                printLogo();
                return;
            }
            case "-?": case "-help": {
                printHelp();
                return;
            }
            case "-groovy": {
                if (aArgs.length < 3) {SP.Groovy.runShell(); return;}
                break;
            }
            case "-python": {
                if (aArgs.length < 3) {SP.Python.runShell(); return;}
                break;
            }
            case "-idea": {
                // 先是项目文件
                String tDirName = IO.toFileName(WORKING_DIR);
                IO.write(tDirName+".iml",
                         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                         "<module type=\"JAVA_MODULE\" version=\"4\">",
                         "  <component name=\"NewModuleRootManager\" inherit-compiler-output=\"true\">",
                         "    <exclude-output />",
                         "    <content url=\"file://$MODULE_DIR$\">",
                         "      <sourceFolder url=\"file://$MODULE_DIR$\" isTestSource=\"false\" />",
                         "      <sourceFolder url=\"file://$MODULE_DIR$/script/groovy\" isTestSource=\"false\" />",
                         "      <excludeFolder url=\"file://$MODULE_DIR$/.temp\" />",
                         "      <excludeFolder url=\"file://$MODULE_DIR$/script\" />",
                         "    </content>",
                         "    <content url=\"file://"+GROOVY_LIB_DIR+"\">",
                         "      <sourceFolder url=\"file://"+GROOVY_LIB_DIR+"\" isTestSource=\"false\" />",
                         "    </content>",
                         "    <orderEntry type=\"inheritedJdk\" />",
                         "    <orderEntry type=\"sourceFolder\" forTests=\"false\" />",
                         "    <orderEntry type=\"library\" name=\"jse-all\" level=\"project\" />",
                         "    <orderEntry type=\"library\" name=\"jars\" level=\"project\" />",
                         "  </component>",
                         "</module>");
                // 然后是运行配置
                IO.write(".run/jse-RunCurrentScript.run.xml",
                         "<component name=\"ProjectRunConfigurationManager\">",
                         "  <configuration default=\"false\" name=\"jse-RunCurrentScript\" type=\"JarApplication\" singleton=\"false\">",
                         "    <option name=\"JAR_PATH\" value=\""+JAR_PATH+"\" />",
                         "    <option name=\"PROGRAM_PARAMETERS\" value=\"IDEA -f $FileRelativePath$\" />",
                         "    <option name=\"WORKING_DIRECTORY\" value=\"$ProjectFileDir$\" />",
                         "    <option name=\"ALTERNATIVE_JRE_PATH\" />",
                         "    <method v=\"2\" />",
                         "  </configuration>",
                         "</component>");
                // 最后是 idea 配置
                IO.write(".idea/modules.xml",
                         "<?xml version=\"1.0\" encoding=\"UTF-8\"?>",
                         "<project version=\"4\">",
                         "  <component name=\"ProjectModuleManager\">",
                         "    <modules>",
                         "      <module fileurl=\"file://$PROJECT_DIR$/"+tDirName+".iml\" filepath=\"$PROJECT_DIR$/"+tDirName+".iml\" />",
                         "    </modules>",
                         "  </component>",
                         "</project>");
                String tSrcLines = "    <SOURCES />";
                @Nullable String tJseRootDir = IO.toParentPath(JAR_DIR);
                if (tJseRootDir != null) {
                    tJseRootDir = IO.toInternalValidDir(tJseRootDir);
                    String tSrcDir = tJseRootDir+"src/";
                    String tJseSrcPath = null;
                    String tGroovySrcPath = null;
                    if (IO.isDir(tSrcDir)) for (String tName : IO.list(tSrcDir)) {
                        if (tName.contains("jse")) tJseSrcPath = tSrcDir+tName;
                        else if (tName.contains("groovy")) tGroovySrcPath = tSrcDir+tName;
                    }
                    if (tJseSrcPath!=null || tGroovySrcPath!=null) {
                        tSrcLines = "    <SOURCES>\n";
                        if (tJseSrcPath != null) {
                        tSrcLines += "      <root url=\"jar://"+tJseSrcPath+"!/\" />\n";
                        }
                        if (tGroovySrcPath != null) {
                        tSrcLines += "      <root url=\"jar://"+tGroovySrcPath+"!/\" />\n";
                        }
                        tSrcLines += "    </SOURCES>";
                    }
                }
                IO.write(".idea/libraries/jse_all.xml",
                         "<component name=\"libraryTable\">",
                         "  <library name=\"jse-all\">",
                         "    <CLASSES>",
                         "      <root url=\"jar://"+JAR_PATH+"!/\" />",
                         "    </CLASSES>",
                         "    <JAVADOC />",
                         tSrcLines,
                         "  </library>",
                         "</component>");
                IO.write(".idea/libraries/jars.xml",
                         "<component name=\"libraryTable\">",
                         "  <library name=\"jars\">",
                         "    <CLASSES>",
                         "      <root url=\"file://"+JAR_LIB_DIR+"!/\" />",
                         "    </CLASSES>",
                         "    <JAVADOC />",
                         "    <SOURCES />",
                         "    <jarDirectory url=\"file://"+JAR_LIB_DIR+"\" recursive=\"false\" />",
                         "  </library>",
                         "</component>");
                System.out.println("The current directory has been initialized as an Intellij IDEA project,");
                System.out.println("now you can open this directory through Intellij IDEA.");
                return;
            }
            case "-jupyter": {
                // 获取可选参数
                StringBuilder rArgs = new StringBuilder();
                for (int i = 2; i < aArgs.length; ++i) rArgs.append(", ").append(aArgs[i]);
                // 使用写入到临时目录然后走 jupyter 的方法来安装，这样应该是符合规范的
                String tWorkingDir = WORKING_DIR_OF("jupyterkernel");
                IO.removeDir(tWorkingDir);
                // 构造 kernel.json 并写入指定位置
                IO.map2json(Maps.of(
                      "argv", new String[]{"java", "-jar", JAR_PATH, "JUPYTER", "-jupyterkernel", "{connection_file}"}
                    , "display_name", "jse"
                    , "language", "groovy"
                    , "interrupt_mode", "message"), tWorkingDir+"kernel.json");
                // 写入 logo
                IO.copy(IO.getResource("jupyter/logo-32x32.png"), tWorkingDir+"logo-32x32.png");
                IO.copy(IO.getResource("jupyter/logo-64x64.png"), tWorkingDir+"logo-64x64.png");
                // 使用这种方法来安装，不走 jep 来避免安装失败的问题；
                // 现在支持直接使用 PythonExec 来执行
                try (PythonSystemExecutor tPython = new PythonSystemExecutor()) {
                    int tExitValue = tPython.system(
                        "import sys\n" +
                            "from jupyter_client.kernelspec import KernelSpecManager\n" +
                            "KernelSpecManager().install_kernel_spec('"+tWorkingDir.replace("\\", "\\\\")+"', 'jse'"+rArgs+")"
                    );
                    if (tExitValue != 0) {System.exit(tExitValue); return;}
                }
                IO.removeDir(tWorkingDir);
                // 由于 java 中不能正常关闭 jupyter，因此不在这里运行 jupyter
                System.out.println("The jupyter kernel for JSE has been initialized,");
                System.out.println("now you can open the jupyter notebook through `jupyter notebook`");
                return;
            }
            default: {
                break;
            }}
            if (aArgs.length < 3) {printHelp(System.err); return;}
            tValue = aArgs[2];
            String[] tArgs = new String[aArgs.length-3];
            if (tArgs.length > 0) System.arraycopy(aArgs, 3, tArgs, 0, tArgs.length);
            switch (tOption) {
            case "-t": case "-text": case "-groovytext": {
                SP.Groovy.runText(tValue, tArgs);
                return;
            }
            case "-pythontext": {
                SP.Python.runText(tValue, tArgs);
                return;
            }
            case "-f": case "-file": {
                SP.runScript(tValue, tArgs);
                return;
            }
            case "-groovy": {
                SP.Groovy.runScript(tValue, tArgs);
                return;
            }
            case "-python": {
                SP.Python.runScript(tValue, tArgs);
                return;
            }
            case "-i": case "-invoke": {
                int tLastDot = tValue.lastIndexOf(".");
                if (tLastDot < 0) {
                    System.err.println("ERROR: Invalid method name: " + tValue);
                    return;
                }
                String tPackageName = tValue.substring(0, tLastDot);
                String tMethodName = tValue.substring(tLastDot+1);
                try {
                    UT.Hack.getRunnableOfStaticMethod(tPackageName, tMethodName, (Object[])tArgs).run();
                } catch (ClassNotFoundException e) {
                    tLastDot = tPackageName.lastIndexOf(".");
                    String tClassName = tPackageName.substring(0, tLastDot);
                    String tFieldName = tPackageName.substring(tLastDot+1);
                    Class<?> aClazz;
                    try {
                        aClazz = Class.forName(tClassName);
                    } catch (ClassNotFoundException ex) {
                        throw new ClassNotFoundException(tPackageName+" nor "+tClassName);
                    }
                    Object tInstance;
                    try {
                        tInstance = aClazz.getField(tFieldName).get(null);
                    } catch (NoSuchFieldException | NullPointerException ex) {
                        throw e;
                    }
                    UT.Hack.getRunnableOfMethod(tInstance, tMethodName, (Object[])tArgs).run();
                }
                return;
            }
            case "-jupyterkernel": {
                IS_KERNEL = true;
                JupyterSocket.JUPYTER_LOGGER.setLevel(Level.WARNING);
                
                String tContents = IO.readAllText(tValue);
                KernelConnectionProperties tConnProps = KernelConnectionProperties.parse(tContents);
                JupyterConnection tConnection = new JupyterConnection(tConnProps);
                
                SP.JupyterKernel tKernel = new SP.JupyterKernel();
                tKernel.becomeHandlerForConnection(tConnection);
                tConnection.connect();
                tConnection.waitUntilClose();
                return;
            }
            default: {
                printHelp(System.err);
                return;
            }}
        } catch (CompilationFailedException e) {
            System.err.println(e);
            System.exit(1);
            return;
        } catch (Throwable e) {
            Throwable ex = e;
            if (ex instanceof InvokerInvocationException) {
                InvokerInvocationException iie = (InvokerInvocationException) ex;
                ex = iie.getCause();
            }
            UT.Code.printStackTrace(ex);
            System.exit(1);
            return;
        } finally {
            closeAllAutoCloseable();
        }
    }
    
    /** stuffs from {@link StackTraceUtils} */
    @SuppressWarnings("UnusedReturnValue")
    @Contract("_ -> param1")
    @ApiStatus.Internal public static <T extends Throwable> T deepSanitize(T t) {
        Throwable tCurrent = t;
        while (tCurrent.getCause() != null) {
            tCurrent = sanitize(tCurrent.getCause());
        }
        return sanitize(t);
    }
    @Contract("_ -> param1")
    @ApiStatus.Internal public static <T extends Throwable> T sanitize(T t) {
        StackTraceElement[] tTrace = t.getStackTrace();
        List<StackTraceElement> nTrace = new ArrayList<>();
        for (StackTraceElement tElement : tTrace) if (isApplicationClass(tElement.getClassName())) {
            nTrace.add(tElement);
        }
        StackTraceElement[] tClean = new StackTraceElement[nTrace.size()];
        nTrace.toArray(tClean);
        t.setStackTrace(tClean);
        return t;
    }
    private static boolean isApplicationClass(String className) {
        for (String tPackage : JSE_PACKAGES) if (className.startsWith(tPackage)) {
            return false;
        }
        return true;
    }
    private static final String[] JSE_PACKAGES = {
          "jse."
        , "jsex."
        , "jep."
        , "me.tongfei.progressbar."
        , "shade.com.jcraft.jsch."
        , "shade.com.jcraft.jzlib."
        , "com.fasterxml.jackson."
        , "org.yaml.snakeyaml."
        , "com.google.common."
        , "com.mastfrog.util.preconditions."
        , "com.mastfrog.util.sort."
        , "net.jafama."
        , "org.apache.commons.csv."
        , "org.jfree.chart."
        , "org.jfree.data."
        , "io.github.spencerpark.jupyter."
        , "com.google.gson."
        , "zmq."
        , "groovy."
        , "org.codehaus.groovy."
        , "java."
        , "javax."
        , "sun."
        , "gjdk.groovy."
        , "groovyjarjar"
        , "com.sun."
        , "org.apache.groovy."
        , "jdk.internal."
    };
    
    
    private static void printLogo(PrintStream aPrinter) {
        aPrinter.println("jse version: "+VERSION+String.format(" (java: %s)", System.getProperty("java.version"))+", Java Simulation Environment");
        aPrinter.println("Copyright (C) 2023-present, Li Qing'an");
        aPrinter.println("       __  ____  ____ ");
        aPrinter.println("     _(  )/ ___)(  __)");
        aPrinter.println("    / \\) \\\\___ \\ ) _) ");
        aPrinter.println("    \\____/(____/(____)");
        aPrinter.println("        by liqa, CHanzyLazer");
        aPrinter.println();
        String tURL = "https://github.com/CHanzyLazer/jse";
        if (UNICODE_SUPPORT) {
            tURL = "\u001b[4m"+tURL+"\u001b[0m";
        }
        aPrinter.println("    GitHub: "+tURL);
    }
    private static void printLogo() {printLogo(System.out);}
    
    private static void printHelp(PrintStream aPrinter) {
        printLogo(aPrinter);
        aPrinter.println();
        aPrinter.println("Usage:    jse [-option] value [args...]");
        aPrinter.println("Such as:  jse path/to/script.groovy [args...]");
        aPrinter.println("Or:       jse -t 'println(/hello world/)'");
        aPrinter.println();
        aPrinter.println("The options can be:");
        aPrinter.println("    -t -text      Run the groovy text script");
        aPrinter.println("    -f -file      Run the groovy/python file script (default behavior when left blank)");
        aPrinter.println("    -i -invoke    Invoke the internal java static method directly");
        aPrinter.println("    -v -version   Print version number");
        aPrinter.println("    -? -help      Print help message");
        aPrinter.println("    -idea         Initialize the current directory to Intellij IDEA project");
        aPrinter.println("    -groovy       Run the groovy file script, or open the groovy interactive shell when no file input");
        aPrinter.println("    -python       Run the python file script, or open the python interactive shell when no file input");
        aPrinter.println("    -groovytext   Run the groovy text script");
        aPrinter.println("    -pythontext   Run the python text script");
        aPrinter.println("    -jupyter      Install current jse to the jupyter kernel");
        aPrinter.println();
        aPrinter.println("You can also using another scripting language such as MATLAB or Python with Py4J and import jse-*.jar");
    }
    private static void printHelp() {printHelp(System.out);}
}
