package jse.clib;

import org.jetbrains.annotations.ApiStatus;

/**
 * 可以增长的 {@link FloatCPointer}，在增长时会自动释放旧的内存，并自动额外申请内存来加速；
 * 此外会记录当前的长度来判断是否需要增长
 * <p>
 * 仅仅进行内存管理，不会有任何初始值，并且扩容时不保留旧值
 * @author liqa
 */
@ApiStatus.Experimental
public class GrowableFloatCPointer extends FloatCPointer implements IGrowableCPointer {
    protected int mCount = 0;
    public GrowableFloatCPointer(int aInitCount) {
        super(malloc_(aInitCount, TYPE_SIZE));
        mCount = aInitCount;
    }
    public GrowableFloatCPointer() {
        super(0);
    }
    public int count() {return mCount;}
    
    private void grow_(int aMinCount) {
        final int oCount = mCount;
        if (mPtr != 0) free_(mPtr);
        mCount = Math.max(aMinCount, oCount + (oCount>>1));
        mPtr = malloc_(mCount, TYPE_SIZE);
    }
    public void ensureCapacity(int aMinCount) {
        if (aMinCount > mCount) grow_(aMinCount);
    }
}
