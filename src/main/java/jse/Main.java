package jse;

import io.github.spencerpark.jupyter.channels.JupyterConnection;
import io.github.spencerpark.jupyter.channels.JupyterSocket;
import io.github.spencerpark.jupyter.kernel.KernelConnectionProperties;
import jse.code.SP;
import jse.code.UT;
import org.apache.groovy.util.Maps;
import org.codehaus.groovy.control.CompilationFailedException;
import org.codehaus.groovy.runtime.InvokerInvocationException;
import org.codehaus.groovy.runtime.StackTraceUtils;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Level;

import static jse.code.OS.JAR_PATH;
import static jse.code.CS.VERSION;
import static jse.code.Conf.DEBUG;
import static jse.code.Conf.WORKING_DIR_OF;

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
    private static void closeAllAutoCloseable() {
        // 需要先遍历添加到 List 固定，避免在 close 过程中被再次修改
        List<AutoCloseable> tGlobalAutoCloseable;
        synchronized(GLOBAL_AUTO_CLOSEABLE) {
            tGlobalAutoCloseable = new ArrayList<>(GLOBAL_AUTO_CLOSEABLE);
        }
        for (AutoCloseable tAutoCloseable : tGlobalAutoCloseable) {
            try {tAutoCloseable.close();} catch (Exception e) {e.printStackTrace(System.err);}
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
                System.out.println("jse version: "+VERSION+String.format(" (java: %s)", System.getProperty("java.version")));
                return;
            }
            case "-?": case "-help": {
                printHelp();
                return;
            }
            case "-jupyter": {
                // 获取可选参数
                StringBuilder rArgs = new StringBuilder();
                for (int i = 2; i < aArgs.length; ++i) rArgs.append(", ").append(aArgs[i]);
                // 使用写入到临时目录然后走 jupyter 的方法来安装，这样应该是符合规范的
                String tWorkingDir = WORKING_DIR_OF("jupyterkernel");
                UT.IO.removeDir(tWorkingDir);
                // 构造 kernel.json 并写入指定位置
                UT.IO.map2json(Maps.of(
                      "argv", new String[]{"java", "-jar", JAR_PATH, "JUPYTER", "-jupyterkernel", "{connection_file}"}
                    , "display_name", "jse"
                    , "language", "groovy"
                    , "interrupt_mode", "message"), tWorkingDir+"kernel.json");
                // 写入 logo
                UT.IO.copy(UT.IO.getResource("jupyter/logo-32x32.png"), tWorkingDir+"logo-32x32.png");
                UT.IO.copy(UT.IO.getResource("jupyter/logo-64x64.png"), tWorkingDir+"logo-64x64.png");
                // 使用这种方法来安装，不走 jep 来避免安装失败的问题
                // 这里改用 ProcessBuilder 直接调用 python 而不是 CS.Exec.EXE 来通过 shell 调用，因为 powershell 有双引号的问题
                Process tProcess = new ProcessBuilder("python", "-c", "import sys;from jupyter_client.kernelspec import KernelSpecManager;KernelSpecManager().install_kernel_spec('"+tWorkingDir.replace("\\", "\\\\")+"', 'jse'"+rArgs+")").start();
                // 只读取错误输出
                Future<Void> tErrTask = UT.Par.redirectStream(tProcess.getErrorStream(), true, System.err);
                int tExitValue;
                try {tErrTask.get(); tExitValue = tProcess.waitFor();}
                catch (Exception e) {tProcess.destroy(); throw e;}
                if (tExitValue != 0) {System.exit(tExitValue); return;}
                UT.IO.removeDir(tWorkingDir);
                // 由于 java 中不能正常关闭 jupyter，因此不在这里运行 jupyter
                System.out.println("The jupyter kernel for JSE has been initialized,");
                System.out.println("now you can open the jupyter notebook through `jupyter notebook`");
                return;
            }
            default: {
                if (aArgs.length < 3) {printHelp(); return;}
                tValue = aArgs[2];
                String[] tArgs = new String[aArgs.length-3];
                if (tArgs.length > 0) System.arraycopy(aArgs, 3, tArgs, 0, tArgs.length);
                switch (tOption) {
                case "-t": case "-text": {
                    SP.Groovy.runText(tValue, tArgs);
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
                    
                    String tContents = UT.IO.readAllText(tValue);
                    KernelConnectionProperties tConnProps = KernelConnectionProperties.parse(tContents);
                    JupyterConnection tConnection = new JupyterConnection(tConnProps);
                    
                    SP.JupyterKernel tKernel = new SP.JupyterKernel();
                    tKernel.becomeHandlerForConnection(tConnection);
                    tConnection.connect();
                    tConnection.waitUntilClose();
                    return;
                }
                default: {
                    printHelp();
                    return;
                }}
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
            if (!DEBUG) ex = deepSanitize(ex);
            ex.printStackTrace(System.err);
            System.exit(1);
            return;
        } finally {
            closeAllAutoCloseable();
        }
    }
    
    /** stuffs from {@link StackTraceUtils} */
    @ApiStatus.Internal public static <T extends Throwable> T deepSanitize(T t) {
        Throwable tCurrent = t;
        while (tCurrent.getCause() != null) {
            tCurrent = sanitize(tCurrent.getCause());
        }
        return sanitize(t);
    }
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
    
    
    private static void printHelp() {
        System.out.println("Usage:    jse [-option] value [args...]");
        System.out.println("Such as:  jse path/to/script.groovy [args...]");
        System.out.println("Or:       jse -t \"println('hello world')\"");
        System.out.println();
        System.out.println("The options can be:");
        System.out.println("    -t -text      Run the groovy text script");
        System.out.println("    -f -file      Run the groovy/python file script (default behavior when left blank)");
        System.out.println("    -i -invoke    Invoke the internal java static method directly");
        System.out.println("    -v -version   Print version number");
        System.out.println("    -? -help      Print help message");
        System.out.println("    -groovy       Run the groovy file script");
        System.out.println("    -python       Run the python file script");
        System.out.println("    -jupyter      Install current jse to the jupyter kernel");
        System.out.println();
        System.out.println("You can also using another scripting language such as MATLAB or Python with Py4J and import jse-*.jar");
    }
}
