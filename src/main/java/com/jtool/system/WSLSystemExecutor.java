package com.jtool.system;

import org.jetbrains.annotations.NotNull;

/**
 * @author liqa
 * <p> 在本地的 wsl 上执行，这里直接使用在命令前增加 wsl 来实现 </p>
 */
public class WSLSystemExecutor extends LocalSystemExecutor {
    public WSLSystemExecutor(int aThreadNum) {super(aThreadNum);}
    public WSLSystemExecutor() {super();}
    
    @Override public int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln) {
        return super.system_("wsl " + aCommand, aPrintln);
    }
}
