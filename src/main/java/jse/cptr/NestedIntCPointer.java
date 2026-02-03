package jse.cptr;

import jse.clib.MiMalloc;
import jse.clib.UnsafeJNI;
import jse.code.collection.AbstractRandomAccessList;
import jse.math.IDataShell;
import jse.math.matrix.AbstractMatrix;
import jse.math.matrix.IIntMatrix;
import jse.math.matrix.RefIntMatrix;
import jse.math.matrix.RowIntMatrix;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 c 中的 {@code int **} 处理的指针，
 * 用于优化对于 {@code int **} 的访问
 * @see CPointer CPointer: 一般的 c 指针包装类
 * @see NestedCPointer NestedCPointer: 一般的嵌套指针包装类
 * @author liqa
 */
public class NestedIntCPointer extends NestedCPointer {
    /**
     * 直接从一个任意的 c 指针初始化一个 {@link NestedIntCPointer} 对象
     * @param aPtr 需要包装的 c 指针值
     */
    @ApiStatus.Internal public NestedIntCPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 c 中的 {@code malloc} 来分配内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(int *))}
     * @return 创建的嵌套指针的 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static NestedIntCPointer malloc(int aCount) {
        return new NestedIntCPointer(malloc_(aCount, TYPE_SIZE));
    }
    /**
     * 调用 c 中的 {@code calloc} 来分配全零内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code calloc(aCount, sizeof(int *))}
     * @return 创建的嵌套指针的 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static NestedIntCPointer calloc(int aCount) {
        return new NestedIntCPointer(calloc_(aCount, TYPE_SIZE));
    }
    
    /**
     * 将 jse 的 {@link RowIntMatrix} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@link RowIntMatrix} 数据
     * @see RowIntMatrix
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void fill(RowIntMatrix aData) {
        fill(aData, aData.rowNumber(), aData.columnNumber());
    }
    /**
     * 将 jse 的 {@code IDataShell<int[]>} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * int 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code IDataShell<int[]>} 数据
     * @param aRowNum 需要读取的 aData 的行数
     * @param aColNum 需要读取的 aData 的列数
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void fill(IDataShell<int[]> aData, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        fill0(mPtr, aData.internalDataWithLengthCheck(aRowNum*aColNum), aData.internalDataShift(), aRowNum, aColNum);
    }
    /**
     * 将 java 的 {@code int[]} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * int 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code int[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aRowNum 需要读取的 aData 的行数
     * @param aColNum 需要读取的 aData 的列数
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void fill(int[] aData, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart + aRowNum*aColNum);
        fill0(mPtr, aData, aStart, aRowNum, aColNum);
    }
    private native static void fill0(long rPtr, int[] aData, int aStart, int aRowNum, int aColNum);
    /**
     * 将给定输入数值填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * int 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aValue 需要填充的数值
     * @param aColNum 需要读取的 aData 的列数
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void fill(int aValue, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        fill1(mPtr, aValue, aRowNum, aColNum);
    }
    private native static void fill1(long rPtr, int aValue, int aRowNum, int aColNum);
    
    /**
     * 将此嵌套 c 指针对应的内存数值写入 jse 的 {@link RowIntMatrix} 中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@link RowIntMatrix}
     * @see RowIntMatrix
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void parse2dest(RowIntMatrix rDest) {parse2dest(rDest, rDest.rowNumber(), rDest.columnNumber());}
    /**
     * 将此嵌套 c 指针对应的内存数值写入 jse 的 {@code IDataShell<int[]>} 中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code IDataShell<int[]>}
     * @param aRowNum 需要写入的 rDest 的行数
     * @param aColNum 需要写入的 rDest 的列数
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void parse2dest(IDataShell<int[]> rDest, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        parse2dest_(mPtr, rDest.internalDataWithLengthCheck(aRowNum*aColNum), rDest.internalDataShift(), aRowNum, aColNum);
    }
    /**
     * 将此嵌套 c 指针对应的内存数值写入 java 的 {@code int[]} 中，认为数据按行排列且每个内部的
     * int 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code int[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aRowNum 需要写入的 rDest 的行数
     * @param aColNum 需要写入的 rDest 的列数
     */
    @UnsafeJNI("Invalid input nrows/ncols may directly result in JVM SIGSEGV")
    public void parse2dest(int[] rDest, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart + aRowNum*aColNum);
        parse2dest_(mPtr, rDest, aStart, aRowNum, aColNum);
    }
    private native static void parse2dest_(long aPtr, int[] rDest, int aStart, int aRowNum, int aColNum);
    
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    @Override public IntCPointer get() {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(get_(mPtr));
    }
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    @Override public IntCPointer getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(getAt_(mPtr, aIdx));
    }
    /**
     * 将此嵌套指针当作一个 c 的矩阵，获取内部指定行列的值，即对应 c 中的 {@code ptr[aRow][aCol]}
     * @param aRow 需要获取的行
     * @param aCol 需要获取的列
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input row/col may directly result in JVM SIGSEGV")
    public int getAt(int aRow, int aCol) {
        if (isNull()) throw new NullPointerException();
        return getAt_(mPtr, aRow, aCol);
    }
    private native static int getAt_(long aPtr, int aRow, int aCol);
    
    /**
     * 将此嵌套指针当作一个 c 的矩阵，设置内部指定行列的值，即对应 c 中的 {@code ptr[aRow][aCol] = aValue}
     * @param aRow 需要设置的行
     * @param aCol 需要设置的列
     * @param aValue 需要设置的值
     */
    @UnsafeJNI("Invalid input row/col may directly result in JVM SIGSEGV")
    public void putAt(int aRow, int aCol, int aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aRow, aCol, aValue);
    }
    native static void putAt_(long aPtr, int aRow, int aCol, int aValue);
    
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedIntCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedIntCPointer(rightShift_(mPtr, aCount));
    }
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedIntCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedIntCPointer(leftShift_(mPtr, aCount));
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedIntCPointer copy() {
        return new NestedIntCPointer(mPtr);
    }
    
    /**
     * 将此嵌套指针转换成 jse 的整数矩阵 {@link IIntMatrix}，为这个嵌套指针对应矩阵的引用
     * @param aRowNum 此指针的对应矩阵的行数
     * @param aColNum 此指针的对应矩阵的列数
     * @return 转换后的矩阵
     * @see IIntMatrix
     */
    @UnsafeJNI("Invalid input nrows/ncols may result in JVM SIGSEGV")
    public IIntMatrix asMat(final int aRowNum, final int aColNum) {
        return new RefIntMatrix() {
            @Override public int get(int aRow, int aCol) {
                AbstractMatrix.rangeCheckRow(aRow, aRowNum);
                AbstractMatrix.rangeCheckCol(aCol, aColNum);
                return NestedIntCPointer.this.getAt(aRow, aCol);
            }
            @Override public void set(int aRow, int aCol, int aValue) {
                AbstractMatrix.rangeCheckRow(aRow, aRowNum);
                AbstractMatrix.rangeCheckCol(aCol, aColNum);
                NestedIntCPointer.this.putAt(aRow, aCol, aValue);
            }
            @Override public int rowNumber() {return aRowNum;}
            @Override public int columnNumber() {return aColNum;}
        };
    }
    /**
     * {@inheritDoc}
     * @param aSize {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Invalid input size may result in JVM SIGSEGV")
    @Override public List<IntCPointer> asList(final int aSize) {
        return new AbstractRandomAccessList<IntCPointer>() {
            @Override public IntCPointer get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public IntCPointer set(int index, @NotNull IntCPointer element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                IntCPointer oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
