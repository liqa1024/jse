package com.guan.system;

import org.jetbrains.annotations.Nullable;

/**
 * @author liqa
 * <p> 在本地的 powershell 上执行，这里直接使用在命令前增加 powershell 来实现 </p>
 */
public class PowerShellSystemExecutor extends LocalSystemExecutor {
    public PowerShellSystemExecutor(int aThreadNum) {super(aThreadNum);}
    public PowerShellSystemExecutor() {super();}
    
    @Override public int system(String aCommand, @Nullable IPrintln aPrintln) {
        return super.system("powershell " + aCommand, aPrintln);
    }
}
