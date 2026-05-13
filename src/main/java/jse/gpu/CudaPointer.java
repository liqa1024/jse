package jse.gpu;

import jse.clib.UnsafeJNI;
import jse.cptr.ICPointer;
import jse.cptr.IPointer;
import org.jetbrains.annotations.ApiStatus;

/**
 * 直接访问 cuda 指针的类，不进行自动内存回收和各种检查从而保证最大的兼容性；
 * 此类因此是 {@code Unsafe} 的。
 * @author liqa
 */
public class CudaPointer implements ICudaPointer {
    
    static {
        // 依赖 CudaCore
        CudaCore.InitHelper.init();
    }
    
    protected long mPtr;
    /**
     * 直接从一个任意的 cuda 指针初始化一个 {@link CudaPointer} 对象
     * @param aPtr 需要包装的 cuda 指针值
     */
    @ApiStatus.Internal public CudaPointer(long aPtr) {
        mPtr = aPtr;
    }
    /** @return {@inheritDoc} */
    @Override @ApiStatus.Internal public final long ptr_() {
        return mPtr;
    }
    /**
     * {@inheritDoc}
     * @param aPtr {@inheritDoc}
     */
    @Override public void setPtr_(long aPtr) {
        mPtr = aPtr;
    }
    
    /**
     * 调用 {@link CudaCore#cudaMalloc} 来分配内存创建一个 cuda 指针
     *
     * @param aCount 需要分配的内存大小
     * @return 创建的 cuda 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static CudaPointer malloc(long aCount) throws CudaException {
        return new CudaPointer(CudaCore.cudaMalloc(aCount));
    }
    
    /**
     * {@inheritDoc}
     * @return {@code sizeof(char)}, 1
     */
    @Override public long typeSize() {return TYPE_SIZE;}
    public final static long TYPE_SIZE = 1;
    
    /**
     * {@inheritDoc}
     * @throws IllegalStateException {@inheritDoc}
     */
    @UnsafeJNI("Free wild pointer will directly result in JVM SIGSEGV")
    @Override public void free() throws CudaException {
        if (isNull()) throw new IllegalStateException("Cannot free a NULL pointer");
        CudaCore.cudaFree(mPtr);
        mPtr = 0;
    }
    
    /**
     * 拷贝一份 cuda 指针包装类，注意此方法不会实际拷贝内部
     * cuda 指针对应的显存，因此返回对象内部存储了相同的 cuda 指针
     * @return 拷贝的 cuda 指针包装类，包含相同的 cuda 指针
     */
    public CudaPointer copy() {
        return new CudaPointer(mPtr);
    }
    @Override public final boolean equals(Object aRHS) {
        if (this == aRHS) return true;
        if (!(aRHS instanceof CudaPointer)) return false;
        
        CudaPointer tCPointer = (CudaPointer)aRHS;
        return mPtr == tCPointer.mPtr;
    }
    @Override public final int hashCode() {
        return Long.hashCode(mPtr);
    }
    
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void memcpy2dest(ICudaPointer rDest, long aCount) throws CudaException {
        if (isNull() || rDest.isNull()) throw new NullPointerException();
        CudaCore.cudaMemcpyD2D(mPtr, rDest.ptr_(), aCount);
    }
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void memcpy2dest(ICPointer rDest, long aCount) throws CudaException {
        if (isNull() || rDest.isNull()) throw new NullPointerException();
        CudaCore.cudaMemcpyD2H(mPtr, rDest.ptr_(), aCount);
    }
    
    /**
     * {@inheritDoc}
     * @param aSrc {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void memcpy2this(ICudaPointer aSrc, long aCount) throws CudaException {
        if (isNull() || aSrc.isNull()) throw new NullPointerException();
        CudaCore.cudaMemcpyD2D(aSrc.ptr_(), mPtr, aCount);
    }
    /**
     * {@inheritDoc}
     * @param aSrc {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void memcpy2this(ICPointer aSrc, long aCount) throws CudaException {
        if (isNull() || aSrc.isNull()) throw new NullPointerException();
        CudaCore.cudaMemcpyH2D(aSrc.ptr_(), mPtr, aCount);
    }
    
    /**
     * {@inheritDoc}
     * @param aValue {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void memset(int aValue, long aCount) throws CudaException {
        if (isNull()) throw new NullPointerException();
        CudaCore.cudaMemset(mPtr, aValue, aCount);
    }
}
