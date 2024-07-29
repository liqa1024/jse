package jse.system;

import org.jetbrains.annotations.NotNull;

import java.nio.charset.Charset;

/**
 * @author liqa
 * <p> 在本地的 cmd 上执行 </p>
 */
public class CMDSystemExecutor extends LocalSystemExecutor {
    public CMDSystemExecutor() {super();}
    
    private final static String[] CMD_ARGS = {"cmd", "/c"};
    @Override protected String @NotNull[] programAndArgs_() {return CMD_ARGS;}
    /** 注意自 jdk18 起，默认 charset 统一成了 UTF-8，因此对于 cmd 需要手动指定为 GBK */
    @Override protected Charset charset_() {return Charset.forName("GBK");}
}
