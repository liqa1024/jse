package jse.system;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * @author liqa
 * <p> 在本地的 cmd 上执行 </p>
 */
public class CMDSystemExecutor extends LocalSystemExecutor {
    private final boolean mUseGBK;
    public CMDSystemExecutor(boolean aUseGBK) {super(); mUseGBK = aUseGBK;}
    public CMDSystemExecutor() {this(true);} // 默认使用 GBK
    
    private final static String[] CMD_ARGS = {"cmd", "/c"};
    @Override protected String @NotNull[] programAndArgs_() {return CMD_ARGS;}
    @Override protected Charset charset_() {return mUseGBK ? Charset.forName("GBK") : super.charset_();}
}
