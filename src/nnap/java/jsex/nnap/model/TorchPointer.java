package jsex.nnap.model;

import jse.code.ReferenceChecker;

/**
 * 用来自动回收 {@link TorchModel} 内部的 model 指针，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
@Deprecated
class TorchPointer extends ReferenceChecker {
    final long mPtr;
    TorchPointer(TorchModel aTorch, long aPtr) {
        super(aTorch);
        mPtr = aPtr;
    }
    
    @Override protected void dispose_() {
        dispose0(mPtr);
    }
    static native void dispose0(long aModelPtr);
}
