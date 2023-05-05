package com.guan;


import com.guan.script.ScriptRunner;

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
            ScriptRunner tRunner = new  ScriptRunner();
            String[] tArgs = new String[aArgs.length-1];
            if (tArgs.length > 0) System.arraycopy(aArgs, 1, tArgs, 0, tArgs.length);
            tRunner.run(tValue, tArgs);
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
            ScriptRunner tRunner = new  ScriptRunner();
            tRunner.runText(tValue, tArgs);
            break;
        }
        case "-f": case "-file": {
            ScriptRunner tRunner = new  ScriptRunner();
            tRunner.runScript(tValue, tArgs);
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
        System.out.println("    -? -help    Print help message");
        System.out.println();
        System.out.println("You can also using another scripting language such as MATLAB or Python with Py4J and import jTool-*.jar");
    }
}
