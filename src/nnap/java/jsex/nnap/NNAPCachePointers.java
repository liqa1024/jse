package jsex.nnap;

import jse.clib.DoubleCPointer;
import jse.clib.GrowableDoubleCPointer;
import jse.clib.GrowableIntCPointer;
import jse.code.ReferenceChecker;

/**
 * 用来自动回收 {@link NNAP} 内部缓存的 c 指针，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
class NNAPCachePointers extends ReferenceChecker {
    private final int mThreadNum;
    
    final DoubleCPointer[] mFp;
    final GrowableDoubleCPointer[] mFpPx, mFpPy, mFpPz;
    final GrowableDoubleCPointer[] mNlDx, mNlDy, mNlDz;
    final GrowableIntCPointer[] mNlType, mNlIdx;
    
    NNAPCachePointers(NNAP.SingleNNAP aNNAP, int aThreadNum, int aBasisSize) {
        super(aNNAP);
        mThreadNum = aThreadNum;
        
        mFp = new DoubleCPointer[mThreadNum];
        mFpPx = new GrowableDoubleCPointer[mThreadNum];
        mFpPy = new GrowableDoubleCPointer[mThreadNum];
        mFpPz = new GrowableDoubleCPointer[mThreadNum];
        mNlDx = new GrowableDoubleCPointer[mThreadNum];
        mNlDy = new GrowableDoubleCPointer[mThreadNum];
        mNlDz = new GrowableDoubleCPointer[mThreadNum];
        mNlType = new GrowableIntCPointer[mThreadNum];
        mNlIdx = new GrowableIntCPointer[mThreadNum];
        for (int i = 0; i < mThreadNum; ++i) {
            mFp[i] = DoubleCPointer.malloc(aBasisSize);
            mFpPx[i] = new GrowableDoubleCPointer(1024);
            mFpPy[i] = new GrowableDoubleCPointer(1024);
            mFpPz[i] = new GrowableDoubleCPointer(1024);
            mNlDx[i] = new GrowableDoubleCPointer(16);
            mNlDy[i] = new GrowableDoubleCPointer(16);
            mNlDz[i] = new GrowableDoubleCPointer(16);
            mNlType[i] = new GrowableIntCPointer(16);
            mNlIdx[i] = new GrowableIntCPointer(16);
        }
    }
    
    @Override protected void dispose_() {
        for (int i = 0; i < mThreadNum; ++i) {
            mFp[i].free();
            mFpPx[i].free();
            mFpPy[i].free();
            mFpPz[i].free();
            mNlDx[i].free();
            mNlDy[i].free();
            mNlDz[i].free();
            mNlType[i].free();
            mNlIdx[i].free();
        }
    }
}
