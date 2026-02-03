package jse.jit;

import jse.parallel.IAutoShutdown;
import org.intellij.lang.annotations.Language;

import java.util.Collection;

public interface IJITEngine extends IAutoShutdown {
    /** 完全关闭 jit 的优化，主要用于调试或需要精确结果而不是速度 */
    int OPTIM_NONE = -1;
    /** 兼容性的 jit 的优化，只开启 fmath，理论上保持相同的跨机器兼容性 */
    int OPTIM_COMPAT = 0;
    /** （默认）基本的 jit 的优化，会开启一般 x86 cpu 都有的 avx2 指令集 */
    int OPTIM_BASE = 1;
    /** 最高的 jit 的优化，会开启 avx512 指令集，有时可能会更慢 */
    int OPTIM_MAX = 2;
    
    IJITEngine setSrc(@Language(value="C++", prefix="extern \"C\" {", suffix="}") String aSrc);
    IJITEngine setOptimLevel(int aOptimLevel);
    IJITEngine setMethodNames(String... aMethodNames);
    IJITEngine setMethodNames(Collection<? extends CharSequence> aMethodNames);
    IJITEngine addMethodName(CharSequence aMethodName);
    IJITEngine removeMethodName(CharSequence aMethodName);
    
    /// utils
    boolean hasMethod(CharSequence aMethodName);
    
    /// workflow, comile() -> findMethod(name)
    void compile() throws Exception;
    IJITMethod findMethod(CharSequence aMethodName) throws JITException;
    
    void shutdown();
    boolean isShutdown();
}
