package jse.lmp;

import jse.code.ReferenceChecker;
import jse.code.UT;

/**
 * 用来自动回收 {@link NativeLmp} 内部的指针，这里采取的策略是每次创建之前清理旧的数据
 * @see <a href="http://www.oracle.com/technetwork/articles/java/finalization-137655.htm">
 * How to Handle Java Finalization's Memory-Retention Issues </a>
 * @author liqa
 */
class NativeLmpPointer extends ReferenceChecker {
    final long mPtr;
    final Thread mInitThead; // lammps 需要保证初始化时的线程和释放时是相同的
    NativeLmpPointer(NativeLmp aNativeLmp, long aPtr) {
        super(aNativeLmp);
        mPtr = aPtr;
        mInitThead = Thread.currentThread();
    }
    
    @Override protected void dispose_() {
        try {
            checkThread();
            try {lammpsClose_(mPtr);} catch (LmpException ignored) {}
        } catch (LmpException e) {
            UT.Code.printStackTrace(e);
        }
    }
    void checkThread() throws LmpException {
        Thread tCurrentThread = Thread.currentThread();
        if (tCurrentThread != mInitThead) throw new LmpException("Thread of NativeLmp MUST be SAME: "+tCurrentThread+" vs "+mInitThead);
    }
    static native void lammpsClose_(long aModelPtr) throws LmpException;
}
