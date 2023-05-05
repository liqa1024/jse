package com.guan.script;

import com.guan.code.UT;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;

/**
 * @author liqa
 * <p> 运行 Groovy 脚本 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public class Groovy {
    public static Object run(String aScriptPath) throws IOException {return runScript(aScriptPath);}
    public static Object runScript(String aScriptPath) throws IOException {
        aScriptPath = UT.IO.toAbsolutePath(aScriptPath);
        GroovyShell tShell = new GroovyShell();
        return tShell.evaluate(new File(aScriptPath));
    }
}
