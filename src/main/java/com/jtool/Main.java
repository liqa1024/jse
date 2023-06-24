package com.jtool;


import com.jtool.code.SP;
import com.jtool.code.UT;

/**
 * @author liqa
 * <p> 直接运行 jTool 时的主要类，根据输入参数来决定具体操作 </p>
 */
public class Main {
    public static void main(String[] aArgs) throws Exception {
        // 没有输入时给出使用方法
        if (aArgs.length == 0) {printHelp(); return;}
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
        if (tOption.equals("-?") || tOption.equals("-help")) {printHelp(); return;}
        if (aArgs.length < 2) {printHelp(); return;}
        tValue = aArgs[1];
        String[] tArgs = new String[aArgs.length-2];
        if (tArgs.length > 0) System.arraycopy(aArgs, 2, tArgs, 0, tArgs.length);
        switch (tOption) {
        case "-t": case "-text": {
            SP.Groovy.runText(tValue, tArgs);
            break;
        }
        case "-f": case "-file": {
            SP.Groovy.runScript(tValue, tArgs);
            break;
        }
        case "-i": case "-invoke": {
            int tLastDot = tValue.lastIndexOf(".");
            if (tLastDot < 0) {
                System.err.println("ERROR: Invalid method name: " + tValue);
                return;
            }
            UT.Hack.getRunnableOfStaticMethod(tValue.substring(0, tLastDot), tValue.substring(tLastDot+1), (Object[])tArgs).run();
            break;
        }
        default: {
            printHelp();
            break;
        }}
    }
    
    private static void printHelp() {
        System.out.println("Usage:   jTool [-option] value [args...]");
        System.out.println("Such as: jTool path/to/script.groovy [argsOfGroovyScript...]");
        System.out.println("Or:      jTool -t \"print('hello world')\"");
        System.out.println();
        System.out.println("The options can be:");
        System.out.println("    -t -text    Run the groovy text script");
        System.out.println("    -f -file    Run the groovy file script (default behavior when left blank)");
        System.out.println("    -i -invoke  Invoke the internal java static method directly");
        System.out.println("    -? -help    Print help message");
        System.out.println();
        System.out.println("You can also using another scripting language such as MATLAB or Python with Py4J and import jTool-*.jar");
    }
}
