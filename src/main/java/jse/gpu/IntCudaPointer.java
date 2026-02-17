package jse.gpu;

import jse.clib.UnsafeJNI;
import jse.cptr.CPointer;
import jse.cptr.IntCPointer;
import jse.math.IDataShell;
import org.jetbrains.annotations.ApiStatus;

/**
 * 当作 cuda 中的 {@code int *} 处理的指针，
 * 注意 c 中的 {@code int} 并不一定总是 32 位的，
 * 但这里统一获取时转换为 java 的 {@code int}。
 * 即使超过了 32 位，实际程序中为了保持一般性，依旧也不会用到超出的部分。
 * @see CudaPointer CudaPointer: 一般的 cuda 指针包装类
 * @author liqa
 */
public class IntCudaPointer extends CudaPointer {
    /**
     * 直接从一个任意的 cuda 指针初始化一个 {@link IntCudaPointer} 对象
     * @param aPtr 需要包装的 cuda 指针值
     */
    @ApiStatus.Internal public IntCudaPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 {@link CudaCore#cudaMalloc} 来分配内存创建一个 cuda 指针
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(int))}
     * @return 创建的整数浮点 cuda 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static IntCudaPointer malloc(long aCount) throws CudaException {
        return new IntCudaPointer(CudaCore.cudaMalloc(aCount*TYPE_SIZE));
    }
    
    /**
     * {@inheritDoc}
     * @return {@code sizeof(int)}
     */
    @Override public long typeSize() {return TYPE_SIZE;}
    public final static long TYPE_SIZE = IntCPointer.TYPE_SIZE;
    
    /**
     * 将 jse 的 {@code IDataShell<int[]>} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code IDataShell<int[]>} 数据
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void fill(IDataShell<int[]> aData) throws CudaException {
        if (isNull()) throw new NullPointerException();
        fill0(mPtr, aData.internalDataWithLengthCheck(), aData.internalDataShift(), aData.internalDataSize());
    }
    /**
     * 将 java 的 {@code int[]} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code int[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(int[] aData, int aStart, int aCount) throws CudaException {
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(aData.length, aStart+aCount);
        fill0(mPtr, aData, aStart, aCount);
    }
    private native static void fill0(long rPtr, int[] aData, int aStart, int aCount) throws CudaException;
    
    /**
     * 调用 {@link CudaCore#cudaMemcpyH2D} 将另一个 c 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 c 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(IntCPointer aData, long aCount) throws CudaException {
        memcpy2this(aData, aCount*TYPE_SIZE);
    }
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2D} 将另一个 cuda 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 cuda 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(IntCudaPointer aData, long aCount) throws CudaException {
        memcpy2this(aData, aCount*TYPE_SIZE);
    }
    
    /**
     * 将此 cuda 指针对应的显存数值写入 jse 的 {@code IDataShell<int[]>} 中
     *
     * @param rDest 需要写入的 {@code IDataShell<int[]>}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void parse2dest(IDataShell<int[]> rDest) {
        if (isNull()) throw new NullPointerException();
        parse2dest0(mPtr, rDest.internalDataWithLengthCheck(), rDest.internalDataShift(), rDest.internalDataSize());
    }
    /**
     * 将此 cuda 指针对应的显存数值写入 java 的 {@code int[]} 中
     *
     * @param rDest 需要写入的 {@code int[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aCount 需要写入的 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(int[] rDest, int aStart, int aCount) {
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(rDest.length, aStart+aCount);
        parse2dest0(mPtr, rDest, aStart, aCount);
    }
    private native static void parse2dest0(long aPtr, int[] rDest, int aStart, int aCount);
    
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2H} 将此 cuda 指针对应的显存数值写入 c 指针对应内存中
     *
     * @param rDest 需要写入的任意 c 指针
     * @param aCount 需要写入 rDest 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(IntCPointer rDest, long aCount) throws CudaException {
        memcpy2dest(rDest, aCount*TYPE_SIZE);
    }
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2D} 将此 cuda 指针对应的显存数值写入另一个 cuda 指针
     *
     * @param rDest 需要写入的任意 cuda 指针
     * @param aCount 需要写入 rDest 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(IntCudaPointer rDest, long aCount) throws CudaException {
        memcpy2dest(rDest, aCount*TYPE_SIZE);
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public IntCudaPointer copy() {
        return new IntCudaPointer(mPtr);
    }
}
