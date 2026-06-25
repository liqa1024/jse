package jse.cptr;

import jse.clib.UnsafeJNI;
import jse.math.IDataShell;
import org.jetbrains.annotations.ApiStatus;

/**
 * 浮点数指针通用接口，用来简化和统一两种精度混合使用的情况
 * @author liqa
 */
@ApiStatus.Experimental
public interface IDoubleOrFloatCPointer extends ICPointer {
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 jse 的 {@code IDataShell<double[]>} 填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code IDataShell<double[]>} 数据
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    void fillD(IDataShell<double[]> aData);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 jse 的 {@code IDataShell<float[]>} 填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code IDataShell<float[]>} 数据
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    void fillF(IDataShell<float[]> aData);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 java 的 {@code double[]} 填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code double[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void fillD(double[] aData, int aStart, int aCount);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 java 的 {@code float[]} 填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code float[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void fillF(float[] aData, int aStart, int aCount);
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将另一个 double c 指针的数据填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 c 指针数据
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void fillD(DoubleCPointer aData, long aCount);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将另一个 float c 指针的数据填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 c 指针数据
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void fillF(FloatCPointer aData, long aCount);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将另一个 double 或者 float c 指针的数据填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 c 指针数据
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    default void fillDF(IDoubleOrFloatCPointer aData, long aCount) {
        if (aData instanceof DoubleCPointer) {
            fillD((DoubleCPointer)aData, aCount);
        } else
        if (aData instanceof FloatCPointer) {
            fillF((FloatCPointer)aData, aCount);
        } else {
            throw new IllegalArgumentException("Invalid class: "+aData.getClass().getName());
        }
    }
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将给定输入数值填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aValue 需要填充的数值
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void fillD(double aValue, long aCount);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将给定输入数值填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aValue 需要填充的数值
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void fillF(float aValue, long aCount);
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 c 指针对应的内存数值写入 jse 的 {@code IDataShell<double[]>} 中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code IDataShell<double[]>}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    void parse2destD(IDataShell<double[]> rDest);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 c 指针对应的内存数值写入 jse 的 {@code IDataShell<float[]>} 中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code IDataShell<float[]>}
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    void parse2destF(IDataShell<float[]> rDest);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 c 指针对应的内存数值写入 java 的 {@code double[]} 中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code double[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aCount 需要写入的 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void parse2destD(double[] rDest, int aStart, int aCount);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 c 指针对应的内存数值写入 java 的 {@code float[]} 中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code float[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aCount 需要写入的 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void parse2destF(float[] rDest, int aStart, int aCount);
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 c 指针的数据填充到另一个 double c 指针中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 输入的 c 指针数据
     * @param aCount 需要写入 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void parse2destD(DoubleCPointer rDest, long aCount);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 c 指针的数据填充到另一个 float c 指针中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 输入的 c 指针数据
     * @param aCount 需要写入 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void parse2destF(FloatCPointer rDest, long aCount);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此 c 指针的数据填充到另一个 double 或者 float c 指针中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 输入的 c 指针数据
     * @param aCount 需要写入 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    default void parse2destDF(IDoubleOrFloatCPointer rDest, long aCount) {
        if (rDest instanceof DoubleCPointer) {
            parse2destD((DoubleCPointer)rDest, aCount);
        } else
        if (rDest instanceof FloatCPointer) {
            parse2destF((FloatCPointer)rDest, aCount);
        } else {
            throw new IllegalArgumentException("Invalid class: "+rDest.getClass().getName());
        }
    }
    
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 对此指针解引用，获取内部数值，即对应 c 中的 {@code *ptr}
     * @return 此指针对应的数值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    double getD();
    /**
     * 方便使用的兼容接口
     * <p>
     * 对此指针解引用，获取内部数值，即对应 c 中的 {@code *ptr}
     * @return 此指针对应的数值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    float getF();
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，即对应 c 中的 {@code ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此索引对应的数值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    double getAtD(long aIdx);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，即对应 c 中的 {@code ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此索引对应的数值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    float getAtF(long aIdx);
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 设置此指针对应的数值，即对应 c 中的 {@code *ptr = aValue}
     * @param aValue 需要设置的数值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    void setD(double aValue);
    /**
     * 方便使用的兼容接口
     * <p>
     * 设置此指针对应的数值，即对应 c 中的 {@code *ptr = aValue}
     * @param aValue 需要设置的数值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    void setF(float aValue);
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此指针当作一个 c 的数组，设置内部指定位置的数值，即对应 c 中的 {@code ptr[aIdx] = aValue}
     * @param aIdx 需要设置的索引位置
     * @param aValue 需要设置的数值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    void putAtD(long aIdx, double aValue);
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此指针当作一个 c 的数组，设置内部指定位置的数值，即对应 c 中的 {@code ptr[aIdx] = aValue}
     * @param aIdx 需要设置的索引位置
     * @param aValue 需要设置的数值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    void putAtF(long aIdx, float aValue);
    
    @Override void next();
    @Override void rightShift(long aCount);
    @Override IDoubleOrFloatCPointer plus(long aCount);
    @Override void previous();
    @Override void leftShift(long aCount);
    @Override IDoubleOrFloatCPointer minus(long aCount);
    @Override IDoubleOrFloatCPointer copy();
}
