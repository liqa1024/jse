package jse.gpu;

import jse.clib.UnsafeJNI;
import jse.cptr.CPointer;
import jse.cptr.DoubleCPointer;
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
public class FloatCudaPointer extends CudaPointer implements IDoubleOrFloatCudaPointer {
    /**
     * 直接从一个任意的 cuda 指针初始化一个 {@link FloatCudaPointer} 对象
     * @param aPtr 需要包装的 cuda 指针值
     */
    @ApiStatus.Internal public FloatCudaPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 {@link CudaCore#cudaMalloc} 来分配内存创建一个 cuda 指针
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(float))}
     * @return 创建的单精度浮点 cuda 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static FloatCudaPointer malloc(long aCount) throws CudaException {
        return new FloatCudaPointer(CudaCore.cudaMalloc(aCount*TYPE_SIZE));
    }
    
    /**
     * {@inheritDoc}
     * @return {@code sizeof(float)}
     */
    @Override public long typeSize() {return TYPE_SIZE;}
    public final static long TYPE_SIZE = FloatCPointer.TYPE_SIZE;
    
    /**
     * 将 jse 的 {@code IDataShell<float[]>} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code IDataShell<float[]>} 数据
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void fill(IDataShell<float[]> aData) throws CudaException {
        if (aData.internalDataSize()==0) return;
        if (isNull()) throw new NullPointerException();
        fill0(mPtr, aData.internalDataWithLengthCheck(), aData.internalDataShift(), aData.internalDataSize());
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
        if (aCount==0) return;
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(aData.length, aStart+aCount);
        fill0(mPtr, aData, aStart, aCount);
    }
    private native static void fill0(long rPtr, float[] aData, int aStart, int aCount) throws CudaException;
    
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    @Override public void fillD(IDataShell<double[]> aData) throws CudaException {
        if (aData.internalDataSize()==0) return;
        if (isNull()) throw new NullPointerException();
        fillD0(mPtr, aData.internalDataWithLengthCheck(), aData.internalDataShift(), aData.internalDataSize());
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    @Override public void fillF(IDataShell<float[]> aData) throws CudaException {
        fill(aData);
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aStart {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillD(double[] aData, int aStart, int aCount) throws CudaException {
        if (aCount==0) return;
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(aData.length, aStart+aCount);
        fillD0(mPtr, aData, aStart, aCount);
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aStart {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillF(float[] aData, int aStart, int aCount) throws CudaException {
        fill(aData, aStart, aCount);
    }
    private native static void fillD0(long rPtr, double[] aData, int aStart, int aCount) throws CudaException;
    
    /**
     * 调用 {@link CudaCore#cudaMemcpyH2D} 将另一个 c 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 c 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(FloatCPointer aData, long aCount) throws CudaException {
        if (aCount==0) return;
        memcpy2this(aData, aCount*TYPE_SIZE);
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillD(DoubleCPointer aData, long aCount) throws CudaException {
        if (aCount==0) return;
        if (isNull() || aData.isNull()) throw new NullPointerException();
        fillD1(mPtr, aData.ptr_(), aCount);
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillF(FloatCPointer aData, long aCount) throws CudaException {
        fill(aData, aCount);
    }
    private native static void fillD1(long rPtr, long aData, long aCount) throws CudaException;
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2D} 将另一个 cuda 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 cuda 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(FloatCudaPointer aData, long aCount) throws CudaException {
        if (aCount==0) return;
        memcpy2this(aData, aCount*TYPE_SIZE);
    }
    
    
    /**
     * 将此 cuda 指针对应的显存数值写入 jse 的 {@code IDataShell<float[]>} 中
     *
     * @param rDest 需要写入的 {@code IDataShell<float[]>}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void parse2dest(IDataShell<float[]> rDest) {
        if (rDest.internalDataSize()==0) return;
        if (isNull()) throw new NullPointerException();
        parse2dest0(mPtr, rDest.internalDataWithLengthCheck(), rDest.internalDataShift(), rDest.internalDataSize());
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
        if (aCount==0) return;
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(rDest.length, aStart+aCount);
        parse2dest0(mPtr, rDest, aStart, aCount);
    }
    private native static void parse2dest0(long aPtr, float[] rDest, int aStart, int aCount);
    
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    @Override public void parse2destD(IDataShell<double[]> rDest) {
        if (rDest.internalDataSize()==0) return;
        if (isNull()) throw new NullPointerException();
        parse2destD0(mPtr, rDest.internalDataWithLengthCheck(), rDest.internalDataShift(), rDest.internalDataSize());
    }
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    @Override public void parse2destF(IDataShell<float[]> rDest) {
        parse2dest(rDest);
    }
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @param aStart {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void parse2destD(double[] rDest, int aStart, int aCount) {
        if (aCount==0) return;
        if (isNull()) throw new NullPointerException();
        CPointer.rangeCheck(rDest.length, aStart+aCount);
        parse2destD0(mPtr, rDest, aStart, aCount);
    }
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @param aStart {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void parse2destF(float[] rDest, int aStart, int aCount) {
        parse2dest(rDest, aStart, aCount);
    }
    private native static void parse2destD0(long aPtr, double[] rDest, int aStart, int aCount);
    
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2H} 将此 cuda 指针对应的显存数值写入 c 指针对应内存中
     *
     * @param rDest 需要写入的任意 c 指针
     * @param aCount 需要写入 rDest 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(FloatCPointer rDest, long aCount) throws CudaException {
        if (aCount==0) return;
        memcpy2dest(rDest, aCount*TYPE_SIZE);
    }
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void parse2destD(DoubleCPointer rDest, long aCount) throws CudaException {
        if (aCount==0) return;
        if (isNull() || rDest.isNull()) throw new NullPointerException();
        parse2destD1(mPtr, rDest.ptr_(), aCount);
    }
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void parse2destF(FloatCPointer rDest, long aCount) throws CudaException {
        parse2dest(rDest, aCount);
    }
    private native static void parse2destD1(long aPtr, long rDest, long aCount) throws CudaException;
    /**
     * 调用 {@link CudaCore#cudaMemcpyD2D} 将此 cuda 指针对应的显存数值写入另一个 cuda 指针
     *
     * @param rDest 需要写入的任意 cuda 指针
     * @param aCount 需要写入 rDest 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(FloatCudaPointer rDest, long aCount) throws CudaException {
        if (aCount==0) return;
        memcpy2dest(rDest, aCount*TYPE_SIZE);
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public FloatCudaPointer copy() {
        return new FloatCudaPointer(mPtr);
    }
}
