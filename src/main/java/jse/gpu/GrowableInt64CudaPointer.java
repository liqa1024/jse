package jse.gpu;

import jse.clib.UnsafeJNI;

/**
 * 可以增长的 {@link Int64CudaPointer}，在增长时会自动释放旧的内存，并自动额外申请内存来加速；
 * 此外会记录当前的长度来判断是否需要增长
 * <p>
 * 仅仅进行内存管理，不会有任何初始值，并且扩容时不保留旧值
 * @author liqa
 */
public class GrowableInt64CudaPointer extends Int64CudaPointer {
    protected long mCount;
    @UnsafeJNI("Manual free required")
    public GrowableInt64CudaPointer(long aInitCount) throws CudaException {
        super(CudaCore.cudaMalloc(aInitCount*TYPE_SIZE));
        mCount = aInitCount;
    }
    public GrowableInt64CudaPointer() {
        super(0);
        mCount = 0;
    }
    public long count() {return mCount;}
    
    private void grow_(long aMinCount) throws CudaException {
        final long oCount = mCount;
        if (mPtr != 0) CudaCore.cudaFree(mPtr);
        mCount = Math.max(aMinCount, oCount + (oCount>>1));
        mPtr = CudaCore.cudaMalloc(mCount*TYPE_SIZE);
    }
    public void ensureCapacity(long aMinCount) throws CudaException {
        if (aMinCount > mCount) grow_(aMinCount);
    }
}
