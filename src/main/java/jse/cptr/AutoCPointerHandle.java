package jse.cptr;

import jse.code.ReferenceChecker;
import org.jetbrains.annotations.ApiStatus;

/**
 * 用来自动回收 c 指针，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
@ApiStatus.Internal
public class AutoCPointerHandle extends ReferenceChecker {
    public final long mPtr, mCount, mSize;
    AutoCPointerHandle(PointerManager aMng, long aCount, long aSize) {
        super(aMng);
        mPtr = CPointer.calloc0(aCount, aSize);
        mCount = aCount;
        mSize = aSize;
    }
    
    @Override protected void dispose_() {
        CPointer.free0(mPtr);
    }
}
