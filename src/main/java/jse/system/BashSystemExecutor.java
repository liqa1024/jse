package jse.system;

import org.jetbrains.annotations.NotNull;

/**
 * @author liqa
 * <p> 在本地的 bash 上执行 </p>
 */
public class BashSystemExecutor extends LocalSystemExecutor {
    public BashSystemExecutor() {super();}
    
    private final static String[] BASH_ARGS = {"bash", "-c"};
    @Override protected String @NotNull[] programAndArgs_() {return BASH_ARGS;}
}
