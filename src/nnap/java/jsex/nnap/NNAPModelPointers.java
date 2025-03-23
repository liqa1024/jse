package jsex.nnap;

import jse.code.ReferenceChecker;

/**
 * 用来自动回收 {@link NNAP} 内部的 model 指针，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
class NNAPModelPointers extends ReferenceChecker {
    final long[] mPtrs;
    NNAPModelPointers(NNAP.SingleNNAP aNNAP, long[] aPtrs) {
        super(aNNAP);
        mPtrs = aPtrs;
    }
    
    @Override protected void dispose_() {
        for (long tPtr : mPtrs) dispose0(tPtr);
    }
    static native void dispose0(long aModelPtr);
}
