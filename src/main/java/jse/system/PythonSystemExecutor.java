package jse.system;

import org.jetbrains.annotations.NotNull;

/**
 * @author liqa
 * <p> 在本地的 python 上执行 </p>
 */
public class PythonSystemExecutor extends LocalSystemExecutor {
    public PythonSystemExecutor() {super();}
    
    private final static String[] PY_ARGS = {"python", "-c"};
    @Override protected String @NotNull[] programAndArgs_() {return PY_ARGS;}
}
