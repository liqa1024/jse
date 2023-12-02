package jtool.system;

import org.jetbrains.annotations.NotNull;

/**
 * @author liqa
 * <p> 在本地的 wsl 上执行，这里直接使用在命令前增加 wsl 来实现 </p>
 */
public class WSLSystemExecutor extends LocalSystemExecutor {
    public WSLSystemExecutor() {super();}
    
    /** wsl 反而不需要使用这种写法，为了简单兼容这里再调用一次 bash */
    private final static String[] WSL_ARGS = {"wsl", "bash", "-c"};
    @Override protected String @NotNull[] programAndArgs_() {return WSL_ARGS;}
}
