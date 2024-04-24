package jse.system;

import org.jetbrains.annotations.NotNull;

import static jse.code.OS.JAR_PATH;

/**
 * @author liqa
 * <p> 通过 jse 执行，会使用和此实例相同的 jar </p>
 */
public class JSESystemExecutor extends LocalSystemExecutor {
    final boolean mIsPython;
    public JSESystemExecutor(boolean aIsPython) {super(); mIsPython = aIsPython;}
    public JSESystemExecutor() {this(false);}
    
    private final static String[] JSE_ARGS = {"java", "-jar", JAR_PATH, "JSE", "-t"};
    private final static String[] JSE_PY_ARGS = {"java", "-jar", JAR_PATH, "JSE", "-pythontext"};
    @Override protected String @NotNull[] programAndArgs_() {return mIsPython ? JSE_PY_ARGS : JSE_ARGS;}
}
