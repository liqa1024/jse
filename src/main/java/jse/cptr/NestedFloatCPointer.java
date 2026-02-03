package jse.cptr;

import jse.clib.MiMalloc;
import jse.clib.UnsafeJNI;
import jse.code.collection.AbstractRandomAccessList;
import jse.math.IDataShell;
import jse.math.matrix.RowMatrix;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 c 中的 {@code float **} 处理的指针，
 * 用于优化对于 {@code float **} 的访问
 * @see CPointer CPointer: 一般的 c 指针包装类
 * @see NestedCPointer NestedCPointer: 一般的嵌套指针包装类
 * @author liqa
 */
public class NestedFloatCPointer extends NestedCPointer {
    /**
     * 直接从一个任意的 c 指针初始化一个 {@link NestedFloatCPointer} 对象
     * @param aPtr 需要包装的 c 指针值
     */
    @ApiStatus.Internal public NestedFloatCPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 c 中的 {@code malloc} 来分配内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(float *))}
     * @return 创建的嵌套指针的 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static NestedFloatCPointer malloc(int aCount) {
        return new NestedFloatCPointer(malloc_(aCount, TYPE_SIZE));
    }
    /**
     * 调用 c 中的 {@code calloc} 来分配全零内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code calloc(aCount, sizeof(float *))}
     * @return 创建的嵌套指针的 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static NestedFloatCPointer calloc(int aCount) {
        return new NestedFloatCPointer(calloc_(aCount, TYPE_SIZE));
    }
    
    /**
     * 将 jse 的 {@code IDataShell<float[]>} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code IDataShell<float[]>} 数据
     * @param aRowNum 需要读取的 aData 的行数
     * @param aColNum 需要读取的 aData 的列数
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void fill(IDataShell<float[]> aData, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        fill0(mPtr, aData.internalDataWithLengthCheck(aRowNum*aColNum), aData.internalDataShift(), aRowNum, aColNum);
    }
    /**
     * 将 java 的 {@code float[]} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code float[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aRowNum 需要读取的 aData 的行数
     * @param aColNum 需要读取的 aData 的列数
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void fill(float[] aData, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart + aRowNum*aColNum);
        fill0(mPtr, aData, aStart, aRowNum, aColNum);
    }
    private native static void fill0(long rPtr, float[] aData, int aStart, int aRowNum, int aColNum);
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 jse 的 {@link RowMatrix} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@link RowMatrix} 数据
     * @see RowMatrix
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void fillD(RowMatrix aData) {
        fillD(aData, aData.rowNumber(), aData.columnNumber());
    }
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 jse 的 {@code IDataShell<double[]>} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code IDataShell<double[]>} 数据
     * @param aRowNum 需要读取的 aData 的行数
     * @param aColNum 需要读取的 aData 的列数
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void fillD(IDataShell<double[]> aData, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        fillD0(mPtr, aData.internalDataWithLengthCheck(aRowNum*aColNum), aData.internalDataShift(), aRowNum, aColNum);
    }
    /**
     * 方便使用的兼容接口
     * <p>
     * 将 java 的 {@code double[]} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code double[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aRowNum 需要读取的 aData 的行数
     * @param aColNum 需要读取的 aData 的列数
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void fillD(double[] aData, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart + aRowNum*aColNum);
        fillD0(mPtr, aData, aStart, aRowNum, aColNum);
    }
    private native static void fillD0(long rPtr, double[] aData, int aStart, int aRowNum, int aColNum);
    
    /**
     * 将给定输入数值填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aValue 需要填充的数值
     * @param aColNum 需要读取的 aData 的列数
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void fill(float aValue, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        fill1(mPtr, aValue, aRowNum, aColNum);
    }
    private native static void fill1(long rPtr, float aValue, int aRowNum, int aColNum);
    
    /**
     * 将此嵌套 c 指针对应的内存数值写入 jse 的 {@code IDataShell<float[]>} 中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code IDataShell<float[]>}
     * @param aRowNum 需要写入的 rDest 的行数
     * @param aColNum 需要写入的 rDest 的列数
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void parse2dest(IDataShell<float[]> rDest, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        parse2dest_(mPtr, rDest.internalDataWithLengthCheck(aRowNum*aColNum), rDest.internalDataShift(), aRowNum, aColNum);
    }
    /**
     * 将此嵌套 c 指针对应的内存数值写入 java 的 {@code float[]} 中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code float[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aRowNum 需要写入的 rDest 的行数
     * @param aColNum 需要写入的 rDest 的列数
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void parse2dest(float[] rDest, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart + aRowNum*aColNum);
        parse2dest_(mPtr, rDest, aStart, aRowNum, aColNum);
    }
    private native static void parse2dest_(long aPtr, float[] rDest, int aStart, int aRowNum, int aColNum);
    
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此嵌套 c 指针对应的内存数值写入 jse 的 {@link RowMatrix} 中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@link RowMatrix}
     * @see RowMatrix
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void parse2destD(RowMatrix rDest) {
        parse2destD(rDest, rDest.rowNumber(), rDest.columnNumber());
    }
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此嵌套 c 指针对应的内存数值写入 jse 的 {@code IDataShell<double[]>} 中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code IDataShell<double[]>}
     * @param aRowNum 需要写入的 rDest 的行数
     * @param aColNum 需要写入的 rDest 的列数
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void parse2destD(IDataShell<double[]> rDest, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        parse2destD_(mPtr, rDest.internalDataWithLengthCheck(aRowNum*aColNum), rDest.internalDataShift(), aRowNum, aColNum);
    }
    /**
     * 方便使用的兼容接口
     * <p>
     * 将此嵌套 c 指针对应的内存数值写入 java 的 {@code double[]} 中，认为数据按行排列且每个内部的
     * float 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code double[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aRowNum 需要写入的 rDest 的行数
     * @param aColNum 需要写入的 rDest 的列数
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void parse2destD(double[] rDest, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart + aRowNum*aColNum);
        parse2destD_(mPtr, rDest, aStart, aRowNum, aColNum);
    }
    private native static void parse2destD_(long aPtr, double[] rDest, int aStart, int aRowNum, int aColNum);
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    @Override public FloatCPointer get() {
        if (isNull()) throw new NullPointerException();
        return new FloatCPointer(get_(mPtr));
    }
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    @Override public FloatCPointer getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new FloatCPointer(getAt_(mPtr, aIdx));
    }
    /**
     * 将此嵌套指针当作一个 c 的矩阵，获取内部指定行列的值，即对应 c 中的 {@code ptr[aRow][aCol]}
     * @param aRow 需要获取的行
     * @param aCol 需要获取的列
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input row/col may directly result in JVM SIGSEGV")
    public float getAt(int aRow, int aCol) {
        if (isNull()) throw new NullPointerException();
        return getAt_(mPtr, aRow, aCol);
    }
    private native static float getAt_(long aPtr, int aRow, int aCol);
    
    /**
     * 将此嵌套指针当作一个 c 的矩阵，设置内部指定行列的值，即对应 c 中的 {@code ptr[aRow][aCol] = aValue}
     * @param aRow 需要设置的行
     * @param aCol 需要设置的列
     * @param aValue 需要设置的值
     */
    @UnsafeJNI("Invalid input row/col may directly result in JVM SIGSEGV")
    public void putAt(int aRow, int aCol, float aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aRow, aCol, aValue);
    }
    native static void putAt_(long aPtr, int aRow, int aCol, float aValue);
    
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedFloatCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedFloatCPointer(rightShift_(mPtr, aCount));
    }
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedFloatCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedFloatCPointer(leftShift_(mPtr, aCount));
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedFloatCPointer copy() {
        return new NestedFloatCPointer(mPtr);
    }
    
    /**
     * {@inheritDoc}
     * @param aSize {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Invalid input size may result in JVM SIGSEGV")
    @Override public List<FloatCPointer> asList(final int aSize) {
        return new AbstractRandomAccessList<FloatCPointer>() {
            @Override public FloatCPointer get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public FloatCPointer set(int index, @NotNull FloatCPointer element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                FloatCPointer oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
