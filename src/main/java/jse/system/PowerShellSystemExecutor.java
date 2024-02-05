package jse.system;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * @author liqa
 * <p> 在本地的 powershell 上执行，这里直接使用在命令前增加 powershell 来实现 </p>
 */
public class PowerShellSystemExecutor extends LocalSystemExecutor {
    private final boolean mUseGBK;
    public PowerShellSystemExecutor(boolean aUseGBK) {super(); mUseGBK = aUseGBK;}
    public PowerShellSystemExecutor() {this(true);} // 默认使用 GBK
    
    private final static String[] PS_ARGS = {"powershell"};
    @Override protected String @NotNull[] programAndArgs_() {return PS_ARGS;}
    @Override protected Charset charset_() {return mUseGBK ? Charset.forName("GBK") : super.charset_();}
}
