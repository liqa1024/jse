package jse.jit;

import jse.code.IO;
import jse.code.ReferenceChecker;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * 用来自动回收 {@link SimpleJIT.Engine} 加载的动态库 handle，以及可能存在的非长期缓存库，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
class JITLibHandle extends ReferenceChecker {
    final @Nullable String mDirToRemove;
    final long mPtr;
    JITLibHandle(SimpleJIT.Engine aEngine, long aPtr, @Nullable String aDirToRemove) {
        super(aEngine);
        mPtr = aPtr;
        mDirToRemove = aDirToRemove;
    }
    boolean isNull() {return mPtr==0 || mPtr==-1;}
    
    @Override protected void dispose_() {
        // free lib，并在不缓存时清理文件
        if (!isNull()) freeLibrary0(mPtr);
        if (mDirToRemove!=null) {
            try {IO.removeDir(mDirToRemove);}
            catch (IOException ignore) {}
        }
    }
    
    private static native void freeLibrary0(long aLibHandle);
}
