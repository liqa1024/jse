package jse.gpu;

import jse.clib.UnsafeJNI;
import jse.cptr.ICPointer;
import jse.cptr.IPointer;
import org.jetbrains.annotations.ApiStatus;

/**
 * 针对 cuda 指针通用接口
 * @author liqa
 */
@ApiStatus.Experimental
public interface ICudaPointer extends IPointer {
    /**
     * 直接设置内部的包装指针
     * @param aPtr 需要设置的内部指针值
     */
    @ApiStatus.Internal void setPtr_(long aPtr);
    
    /**
     * 调用 {@link CudaCore#cudaFree} 来释放一个 cuda 指针对应的内存
     *
     * @throws IllegalStateException 如果此 cuda 指针是空指针
     */
    @UnsafeJNI("Free wild pointer will directly result in JVM SIGSEGV")
    void free() throws CudaException;
    
    /**
     * 直接调用 {@link CudaCore#cudaMemcpyD2D} 来将此数组值拷贝到另一个 cuda 数组中
     * @param rDest 需要拷贝的目标 cuda 指针
     * @param aCount 需要拷贝的数据长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void memcpy2dest(ICudaPointer rDest, long aCount) throws CudaException;
    /**
     * 直接调用 {@link CudaCore#cudaMemcpyD2H} 来将此数组值拷贝到另一个 c 数组中
     * @param rDest 需要拷贝的目标 c 指针
     * @param aCount 需要拷贝的数据长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void memcpy2dest(ICPointer rDest, long aCount) throws CudaException;
    
    /**
     * 直接调用 {@link CudaCore#cudaMemcpyD2D} 来将输入 cuda 数组值拷贝到此数组中
     * @param aSrc 需要拷贝的目标 cuda 指针
     * @param aCount 需要拷贝的数据长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void memcpy2this(ICudaPointer aSrc, long aCount) throws CudaException;
    /**
     * 直接调用 {@link CudaCore#cudaMemcpyH2D} 来将输入 c 数组值拷贝到此数组中
     * @param aSrc 需要拷贝的目标 c 指针
     * @param aCount 需要拷贝的数据长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void memcpy2this(ICPointer aSrc, long aCount) throws CudaException;
    
    /**
     * 直接调用 {@link CudaCore#cudaMemset} 来将指定字节值设置到整个 cuda 内存中
     * @param aValue 需要设置的字节值
     * @param aCount 需要设置的数据长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void memset(int aValue, long aCount) throws CudaException;
    
    /**
     * 获取此指针对应类型的长度
     * @return {@code sizeof(xxx)}
     */
    long typeSize();
}
