package com.guan.system;

import org.jetbrains.annotations.Nullable;

import java.io.PrintStream;

/**
 * @author liqa
 * <p> 在本地的 wsl 上执行，这里直接使用在命令前增加 wsl 来实现 </p>
 */
public class WSLSystemExecutor extends LocalSystemExecutor {
    public WSLSystemExecutor(int aThreadNum) {super(aThreadNum);}
    
    @Override public int system(String aCommand, @Nullable PrintStream aOutPrintStream) {
        return super.system("wsl " + aCommand, aOutPrintStream);
    }
}
