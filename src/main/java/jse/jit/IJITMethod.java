package jse.jit;

import jse.clib.UnsafeJNI;
import jse.cptr.IPointer;

public interface IJITMethod extends IPointer {
    @UnsafeJNI("Invalid usage will result in JVM SIGSEGV")
    int invoke(Object... aArgs);
}
