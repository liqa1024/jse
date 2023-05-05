package com.guan.script;

import com.guan.code.UT;
import groovy.lang.GroovyShell;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author liqa
 * <p> 运行脚本的通用类，目前是运行 Groovy 脚本 </p>
 */
@SuppressWarnings("UnusedReturnValue")
public class ScriptRunner {
    private final AtomicInteger mCounter = new AtomicInteger(0);
    
    private final GroovyShell mShell;
    public ScriptRunner() {mShell = new GroovyShell();}
    
    public Object runText(String aText, String[] aArgs) {return mShell.run(aText, "TextScript" + mCounter.incrementAndGet() + ".groovy", aArgs);}
    public Object run(String aScriptPath, String[] aArgs) throws IOException {return runScript(aScriptPath, aArgs);}
    public Object runScript(String aScriptPath, String[] aArgs) throws IOException {
        aScriptPath = UT.IO.toAbsolutePath(aScriptPath);
        return mShell.run(new File(aScriptPath), aArgs);
    }
}
