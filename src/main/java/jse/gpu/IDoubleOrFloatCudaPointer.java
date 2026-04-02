package jse.gpu;

import jse.clib.UnsafeJNI;
import jse.cptr.DoubleCPointer;
import jse.cptr.FloatCPointer;
import jse.math.IDataShell;
import org.jetbrains.annotations.ApiStatus;

/**
 * 浮点数 cuda 指针通用接口，用来简化和统一两种精度混合使用的情况
 * @author liqa
 */
@ApiStatus.Experimental
public interface IDoubleOrFloatCudaPointer extends ICudaPointer {
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 jse 的 {@code IDataShell<double[]>} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code IDataShell<double[]>} 数据
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    void fillD(IDataShell<double[]> aData) throws CudaException;
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 jse 的 {@code IDataShell<float[]>} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code IDataShell<float[]>} 数据
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    void fillF(IDataShell<float[]> aData) throws CudaException;
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
    void fillD(double[] aData, int aStart, int aCount) throws CudaException;
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 java 的 {@code float[]} 填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的 {@code float[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void fillF(float[] aData, int aStart, int aCount) throws CudaException;
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将另一个 double c 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 c 指针数据
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void fillD(DoubleCPointer aData, long aCount) throws CudaException;
    /**
     * 方便使用的兼容接口
     * <p>
     * 将另一个 float c 指针的数据填充到此 cuda 指针对应的显存中
     *
     * @param aData 输入的任意 c 指针数据
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void fillF(FloatCPointer aData, long aCount) throws CudaException;
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 cuda 指针对应的显存数值写入 jse 的 {@code IDataShell<double[]>} 中
     *
     * @param rDest 需要写入的 {@code IDataShell<double[]>}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    void parse2destD(IDataShell<double[]> rDest) throws CudaException;
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 cuda 指针对应的显存数值写入 jse 的 {@code IDataShell<float[]>} 中
     *
     * @param rDest 需要写入的 {@code IDataShell<float[]>}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    void parse2destF(IDataShell<float[]> rDest) throws CudaException;
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
    void parse2destD(double[] rDest, int aStart, int aCount) throws CudaException;
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 cuda 指针对应的显存数值写入 java 的 {@code float[]} 中
     *
     * @param rDest 需要写入的 {@code float[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aCount 需要写入的 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void parse2destF(float[] rDest, int aStart, int aCount) throws CudaException;
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 cuda 指针对应的显存数值写入 double c 指针对应内存中
     *
     * @param rDest 需要写入的任意 c 指针
     * @param aCount 需要写入 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void parse2destD(DoubleCPointer rDest, long aCount) throws CudaException;
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 cuda 指针对应的显存数值写入 float c 指针对应内存中
     *
     * @param rDest 需要写入的任意 c 指针
     * @param aCount 需要写入 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void parse2destF(FloatCPointer rDest, long aCount) throws CudaException;
}
