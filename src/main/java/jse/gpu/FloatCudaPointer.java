package jse.gpu;

import jse.clib.UnsafeJNI;
import jse.cptr.CPointer;
import jse.cptr.FloatCPointer;
import jse.math.IDataShell;
import org.jetbrains.annotations.ApiStatus;

/**
 * 当作 cuda 中的 {@code float *} 处理的指针，
 * 虽然一般来说 c 中的 {@code float} 和 java 的 {@code jfloat}
 * 等价，但这里为了不失一般性依旧单指 c 中的 {@code float}。
 * @see CudaPointer CudaPointer: 一般的 cuda 指针包装类
 * @author liqa
 */
public class FloatCudaPointer extends CudaPointer {
    /**
     * 直接从一个任意的 cuda 指针初始化一个 {@link FloatCudaPointer} 对象
     * @param aPtr 需要包装的 cuda 指针值
     */
    @ApiStatus.Internal public FloatCudaPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 {@link CudaCore#cudaMalloc} 来分配内存创建一个 cuda 指针
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(float))}
     * @return 创建的双精度浮点 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static FloatCudaPointer malloc(int aCount) throws CudaException {
        return new FloatCudaPointer(CudaCore.cudaMalloc(aCount*TYPE_SIZE));
    }
    /** {@code sizeof(float)} */
    public final static int TYPE_SIZE = FloatCPointer.TYPE_SIZE;
    
    /**
     * 将 jse 的 {@code IDataShell<float[]>} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code IDataShell<float[]>} 数据
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void fill(IDataShell<float[]> aData) throws CudaException {
        if (isNull()) throw new NullPointerException();
        fill_(mPtr, aData.internalDataWithLengthCheck(), aData.internalDataShift(), aData.internalDataSize());
    }
    /**
     * 将 java 的 {@code float[]} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code float[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(float[] aData, int aStart, int aCount) throws CudaException {
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(aData.length, aStart+aCount);
        fill_(mPtr, aData, aStart, aCount);
    }
    private native static void fill_(long rPtr, float[] aData, int aStart, int aCount) throws CudaException;
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 jse 的 {@code IDataShell<double[]>} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code IDataShell<double[]>} 数据
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void fillD(IDataShell<double[]> aData) throws CudaException {
        if (isNull()) throw new NullPointerException();
        fillD_(mPtr, aData.internalDataWithLengthCheck(), aData.internalDataShift(), aData.internalDataSize());
    }
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 java 的 {@code double[]} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code double[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fillD(double[] aData, int aStart, int aCount) throws CudaException {
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(aData.length, aStart+aCount);
        fillD_(mPtr, aData, aStart, aCount);
    }
    private native static void fillD_(long rPtr, double[] aData, int aStart, int aCount) throws CudaException;
    
    /**
     * 调用 {@link CudaCore#cudaMemcpyH2D} 将另一个 c 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 c 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(CPointer aData, int aCount) throws CudaException {
        if (isNull() || aData.isNull()) throw new NullPointerException();
        CudaCore.cudaMemcpyH2D(aData.ptr_(), mPtr, aCount*TYPE_SIZE);
    }
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2D} 将另一个 cuda 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 cuda 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(CudaPointer aData, int aCount) throws CudaException {
        if (isNull() || aData.isNull()) throw new NullPointerException();
        CudaCore.cudaMemcpyD2D(aData.ptr_(), mPtr, aCount*TYPE_SIZE);
    }
    
    /**
     * 将此 cuda 指针对应的显存数值写入 jse 的 {@code IDataShell<float[]>} 中
     *
     * @param rDest 需要写入的 {@code IDataShell<float[]>}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void parse2dest(IDataShell<float[]> rDest) {
        if (isNull()) throw new NullPointerException();
        parse2dest_(mPtr, rDest.internalDataWithLengthCheck(), rDest.internalDataShift(), rDest.internalDataSize());
    }
    /**
     * 将此 cuda 指针对应的显存数值写入 java 的 {@code float[]} 中
     *
     * @param rDest 需要写入的 {@code float[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aCount 需要写入的 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(float[] rDest, int aStart, int aCount) {
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(rDest.length, aStart+aCount);
        parse2dest_(mPtr, rDest, aStart, aCount);
    }
    private native static void parse2dest_(long aPtr, float[] rDest, int aStart, int aCount);
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 cuda 指针对应的显存数值写入 jse 的 {@code IDataShell<double[]>} 中
     *
     * @param rDest 需要写入的 {@code IDataShell<double[]>}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void parse2destD(IDataShell<double[]> rDest) {
        if (isNull()) throw new NullPointerException();
        parse2destD_(mPtr, rDest.internalDataWithLengthCheck(), rDest.internalDataShift(), rDest.internalDataSize());
    }
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 cuda 指针对应的显存数值写入 java 的 {@code double[]} 中
     *
     * @param rDest 需要写入的 {@code double[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aCount 需要写入的 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2destD(double[] rDest, int aStart, int aCount) {
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(rDest.length, aStart+aCount);
        parse2destD_(mPtr, rDest, aStart, aCount);
    }
    private native static void parse2destD_(long aPtr, double[] rDest, int aStart, int aCount);
    
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2H} 将此 cuda 指针对应的显存数值写入 c 指针对应内存中
     *
     * @param rDest 需要写入的任意 c 指针
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(CPointer rDest, int aCount) throws CudaException {
        if (isNull() || rDest.isNull()) throw new NullPointerException();
        CudaCore.cudaMemcpyD2H(mPtr, rDest.ptr_(), aCount*TYPE_SIZE);
    }
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2D} 将此 cuda 指针对应的显存数值写入另一个 cuda 指针
     *
     * @param rDest 需要写入的任意 cuda 指针
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(CudaPointer rDest, int aCount) throws CudaException {
        if (isNull() || rDest.isNull()) throw new NullPointerException();
        CudaCore.cudaMemcpyD2D(mPtr, rDest.ptr_(), aCount*TYPE_SIZE);
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public FloatCudaPointer copy() {
        return new FloatCudaPointer(mPtr);
    }
}
