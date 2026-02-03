package jse.jit;

import jse.clib.UnsafeJNI;
import jse.cptr.ICPointer;

public interface IJITMethod extends ICPointer {
    @UnsafeJNI("Inputs mismatch or invalid usage will result in JVM SIGSEGV")
    int invoke(ICPointer aDataIn, ICPointer rDataOut);
}
