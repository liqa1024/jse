package com.jtool.system;

import org.jetbrains.annotations.NotNull;

/**
 * @author liqa
 * <p> 在本地的 powershell 上执行，这里直接使用在命令前增加 powershell 来实现 </p>
 */
public class PowerShellSystemExecutor extends LocalSystemExecutor {
    public PowerShellSystemExecutor(int aThreadNum) {super(aThreadNum);}
    public PowerShellSystemExecutor() {super();}
    
    @Override public int system_(String aCommand, @NotNull IPrintlnSupplier aPrintln) {
        return super.system_("powershell " + aCommand, aPrintln);
    }
}
