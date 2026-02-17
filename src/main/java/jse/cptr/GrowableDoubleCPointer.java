package jse.cptr;

import jse.clib.UnsafeJNI;

/**
 * 可以增长的 {@link DoubleCPointer}，在增长时会自动释放旧的内存，并自动额外申请内存来加速；
 * 此外会记录当前的长度来判断是否需要增长
 * <p>
 * 仅仅进行内存管理，不会有任何初始值，并且扩容时不保留旧值
 * @author liqa
 */
public class GrowableDoubleCPointer extends DoubleCPointer implements IGrowableDoubleOrFloatCPointer {
    protected long mCount;
    @UnsafeJNI("Manual free required")
    public GrowableDoubleCPointer(long aInitCount) {
        super(malloc0(aInitCount, TYPE_SIZE));
        mCount = aInitCount;
    }
    public GrowableDoubleCPointer() {
        super(0);
        mCount = 0;
    }
    @Override public long count() {return mCount;}
    
    private void grow_(long aMinCount) {
        final long oCount = mCount;
        if (mPtr != 0) free0(mPtr);
        mCount = Math.max(aMinCount, oCount + (oCount>>1));
        mPtr = malloc0(mCount, TYPE_SIZE);
    }
    @Override public void ensureCapacity(long aMinCount) {
        if (aMinCount > mCount) grow_(aMinCount);
    }
}
