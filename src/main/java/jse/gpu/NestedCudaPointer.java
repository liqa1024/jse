package jse.gpu;

import jse.clib.UnsafeJNI;
import jse.cptr.NestedCPointer;
import org.jetbrains.annotations.ApiStatus;

/**
 * 当作 cuda 中的 {@code void **} 处理的指针，
 * 用于处理一般的 cuda 数组
 * @see CudaPointer CudaPointer: 一般的 cuda 指针包装类
 * @author liqa
 */
public class NestedCudaPointer extends CudaPointer {
    /**
     * 直接从一个任意的 cuda 指针初始化一个 {@link NestedCudaPointer} 对象
     * @param aPtr 需要包装的 cuda 指针值
     */
    @ApiStatus.Internal public NestedCudaPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 {@link CudaCore#cudaMalloc} 来分配内存创建一个 cuda 指针
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(void *))}
     * @return 创建的嵌套指针的 cuda 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static NestedCudaPointer malloc(int aCount) throws CudaException {
        return new NestedCudaPointer(CudaCore.cudaMalloc(aCount*TYPE_SIZE));
    }
    /** {@code sizeof(void *)} */
    public final static int TYPE_SIZE = typeSize_();
    private native static int typeSize_();
    
    /**
     * 调用 {@link CudaCore#cudaMemcpyH2D} 将另一个 c 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 c 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(NestedCPointer aData, int aCount) throws CudaException {
        memcpy2this(aData, aCount*TYPE_SIZE);
    }
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2D} 将另一个 cuda 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 cuda 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(NestedCudaPointer aData, int aCount) throws CudaException {
        memcpy2this(aData, aCount*TYPE_SIZE);
    }
    
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2H} 将此 cuda 指针对应的显存数值写入 c 指针对应内存中
     *
     * @param rDest 需要写入的任意 c 指针
     * @param aCount 需要写入 rDest 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(NestedCPointer rDest, int aCount) throws CudaException {
        memcpy2dest(rDest, aCount*TYPE_SIZE);
    }
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2D} 将此 cuda 指针对应的显存数值写入另一个 cuda 指针
     *
     * @param rDest 需要写入的任意 cuda 指针
     * @param aCount 需要写入 rDest 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(NestedCudaPointer rDest, int aCount) throws CudaException {
        memcpy2dest(rDest, aCount*TYPE_SIZE);
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedCudaPointer copy() {
        return new NestedCudaPointer(mPtr);
    }
}
