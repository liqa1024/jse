package jtool;

import groovy.lang.GroovySystem;
import jtool.code.SP;
import jtool.code.UT;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static jtool.code.CS.VERSION;

/**
 * @author liqa
 * <p> 直接运行 jtool 时的主要类，根据输入参数来决定具体操作 </p>
 */
public class Main {
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
    
    public static void main(String[] aArgs) throws Exception {
        try {
            // 没有输入时启动 groovysh
            if (aArgs.length == 0) {SP.Groovy.runShell(); return;}
            // 获取第一个值
            String tValue = aArgs[0];
            if (!tValue.startsWith("-")) {
                // 默认执行脚本文件
                String[] tArgs = new String[aArgs.length-1];
                if (tArgs.length > 0) System.arraycopy(aArgs, 1, tArgs, 0, tArgs.length);
                SP.Groovy.run(tValue, tArgs);
                return;
            }
            // 一般行为
            String tOption = tValue;
            switch (tOption) {
            case "-v": case "-version": {
                System.out.println("jtool version: "+VERSION+String.format(" (groovy: %s, java: %s)", GroovySystem.getVersion(), System.getProperty("java.version")));
                return;
            }
            case "-?": case "-help": {
                printHelp();
                return;
            }
            default: {
                if (aArgs.length < 2) {printHelp(); return;}
                tValue = aArgs[1];
                String[] tArgs = new String[aArgs.length-2];
                if (tArgs.length > 0) System.arraycopy(aArgs, 2, tArgs, 0, tArgs.length);
                switch (tOption) {
                case "-t": case "-text": {
                    SP.Groovy.runText(tValue, tArgs);
                    return;
                }
                case "-f": case "-file": {
                    SP.Groovy.runScript(tValue, tArgs);
                    return;
                }
                case "-i": case "-invoke": {
                    int tLastDot = tValue.lastIndexOf(".");
                    if (tLastDot < 0) {
                        System.err.println("ERROR: Invalid method name: " + tValue);
                        return;
                    }
                    UT.Hack.getRunnableOfStaticMethod(tValue.substring(0, tLastDot), tValue.substring(tLastDot+1), (Object[])tArgs).run();
                    return;
                }
                default: {
                    printHelp();
                    return;
                }}
            }}
        } finally {
            closeAllAutoCloseable();
        }
    }
    
    private static void printHelp() {
        System.out.println("Usage:    jtool [-option] value [args...]");
        System.out.println("Such as:  jtool path/to/script.groovy [argsOfGroovyScript...]");
        System.out.println("Or:       jtool -t \"println('hello world')\"");
        System.out.println();
        System.out.println("The options can be:");
        System.out.println("    -t -text      Run the groovy text script");
        System.out.println("    -f -file      Run the groovy file script (default behavior when left blank)");
        System.out.println("    -i -invoke    Invoke the internal java static method directly");
        System.out.println("    -v -version   Print version number");
        System.out.println("    -? -help      Print help message");
        System.out.println();
        System.out.println("You can also using another scripting language such as MATLAB or Python with Py4J and import jtool-*.jar");
    }
}
