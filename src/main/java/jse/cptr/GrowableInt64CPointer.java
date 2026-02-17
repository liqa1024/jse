package jse.cptr;

import jse.clib.UnsafeJNI;

/**
 * 可以增长的 {@link Int64CPointer}，在增长时会自动释放旧的内存，并自动额外申请内存来加速；
 * 此外会记录当前的长度来判断是否需要增长
 * <p>
 * 仅仅进行内存管理，不会有任何初始值，并且扩容时不保留旧值
 * @author liqa
 */
public class GrowableInt64CPointer extends Int64CPointer implements IGrowableCPointer {
    protected long mCount;
    @UnsafeJNI("Manual free required")
    public GrowableInt64CPointer(long aInitCount) {
        super(CPointer.malloc0(aInitCount, TYPE_SIZE));
        mCount = aInitCount;
    }
    public GrowableInt64CPointer() {
        super(0);
        mCount = 0;
    }
    @Override public long count() {return mCount;}
    
    private void grow_(long aMinCount) {
        final long oCount = mCount;
        if (mPtr != 0) CPointer.free0(mPtr);
        mCount = Math.max(aMinCount, oCount + (oCount>>1));
        mPtr = CPointer.malloc0(mCount, TYPE_SIZE);
    }
    @Override public void ensureCapacity(long aMinCount) {
        if (aMinCount > mCount) grow_(aMinCount);
    }
}
