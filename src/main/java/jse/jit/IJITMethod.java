package jse.jit;

import jse.clib.UnsafeJNI;
import jse.cptr.IPointer;

public interface IJITMethod extends IPointer {
    @UnsafeJNI("Inputs mismatch or invalid usage will result in JVM SIGSEGV")
    int invoke(IPointer aDataIn, IPointer rDataOut);
}
