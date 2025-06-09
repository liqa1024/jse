package jse.clib;

import jse.code.collection.AbstractRandomAccessList;
import jse.math.IDataShell;
import jse.math.matrix.AbstractMatrix;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RefMatrix;
import jse.math.matrix.RowMatrix;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 c 中的 {@code double **} 处理的指针，
 * 用于优化对于 {@code double **} 的访问
 * @see CPointer CPointer: 一般的 c 指针包装类
 * @see NestedCPointer NestedCPointer: 一般的嵌套指针包装类
 * @author liqa
 */
public class NestedDoubleCPointer extends NestedCPointer {
    /**
     * 直接从一个任意的 c 指针初始化一个 {@link NestedDoubleCPointer} 对象
     * @param aPtr 需要包装的 c 指针值
     */
    @ApiStatus.Internal public NestedDoubleCPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 c 中的 {@code malloc} 来分配内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(double *))}
     * @return 创建的嵌套指针的 c 指针对象
     */
    public static NestedDoubleCPointer malloc(int aCount) {
        return new NestedDoubleCPointer(malloc_(aCount, TYPE_SIZE));
    }
    /**
     * 调用 c 中的 {@code calloc} 来分配全零内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code calloc(aCount, sizeof(double *))}
     * @return 创建的嵌套指针的 c 指针对象
     */
    public static NestedDoubleCPointer calloc(int aCount) {
        return new NestedDoubleCPointer(calloc_(aCount, TYPE_SIZE));
    }
    
    /**
     * 将 jse 的 {@link RowMatrix} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@link RowMatrix} 数据
     * @see RowMatrix
     */
    public void fill(RowMatrix aData) {
        fill(aData, aData.rowNumber(), aData.columnNumber());
    }
    /**
     * 将 jse 的 {@code IDataShell<double[]>} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code IDataShell<double[]>} 数据
     * @param aRowNum 需要读取的 aData 的行数
     * @param aColNum 需要读取的 aData 的列数
     * @see IDataShell
     */
    public void fill(IDataShell<double[]> aData, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        fill0(mPtr, aData.internalDataWithLengthCheck(aRowNum*aColNum), aData.internalDataShift(), aRowNum, aColNum);
    }
    /**
     * 将 java 的 {@code double[]} 填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code double[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aRowNum 需要读取的 aData 的行数
     * @param aColNum 需要读取的 aData 的列数
     */
    public void fill(double[] aData, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart + aRowNum*aColNum);
        fill0(mPtr, aData, aStart, aRowNum, aColNum);
    }
    private native static void fill0(long rPtr, double[] aData, int aStart, int aRowNum, int aColNum);
    /**
     * 将给定输入数值填充到此嵌套 c 指针对应的内存中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aValue 需要填充的数值
     * @param aColNum 需要读取的 aData 的列数
     */
    public void fill(double aValue, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        fill1(mPtr, aValue, aRowNum, aColNum);
    }
    private native static void fill1(long rPtr, double aValue, int aRowNum, int aColNum);
    
    /**
     * 将此嵌套 c 指针对应的内存数值写入 jse 的 {@link RowMatrix} 中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@link RowMatrix}
     * @see RowMatrix
     */
    public void parse2dest(RowMatrix rDest) {parse2dest(rDest, rDest.rowNumber(), rDest.columnNumber());}
    /**
     * 将此嵌套 c 指针对应的内存数值写入 jse 的 {@code IDataShell<double[]>} 中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code IDataShell<double[]>}
     * @param aRowNum 需要写入的 rDest 的行数
     * @param aColNum 需要写入的 rDest 的列数
     * @see IDataShell
     */
    public void parse2dest(IDataShell<double[]> rDest, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        parse2dest_(mPtr, rDest.internalDataWithLengthCheck(aRowNum*aColNum), rDest.internalDataShift(), aRowNum, aColNum);
    }
    /**
     * 将此嵌套 c 指针对应的内存数值写入 java 的 {@code double[]} 中，认为数据按行排列且每个内部的
     * double 指针对应的长度一致
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code double[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aRowNum 需要写入的 rDest 的行数
     * @param aColNum 需要写入的 rDest 的列数
     */
    public void parse2dest(double[] rDest, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart + aRowNum*aColNum);
        parse2dest_(mPtr, rDest, aStart, aRowNum, aColNum);
    }
    private native static void parse2dest_(long aPtr, double[] rDest, int aStart, int aRowNum, int aColNum);
    
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public DoubleCPointer get() {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(get_(mPtr));
    }
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public DoubleCPointer getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(getAt_(mPtr, aIdx));
    }
    /**
     * 将此嵌套指针当作一个 c 的矩阵，获取内部指定行列的值，即对应 c 中的 {@code ptr[aRow][aCol]}
     * @param aRow 需要获取的行
     * @param aCol 需要获取的列
     * @return 此指针对应的值
     */
    public double getAt(int aRow, int aCol) {
        if (isNull()) throw new NullPointerException();
        return getAt_(mPtr, aRow, aCol);
    }
    private native static double getAt_(long aPtr, int aRow, int aCol);
    
    /**
     * 将此嵌套指针当作一个 c 的矩阵，设置内部指定行列的值，即对应 c 中的 {@code ptr[aRow][aCol] = aValue}
     * @param aRow 需要设置的行
     * @param aCol 需要设置的列
     * @param aValue 需要设置的值
     */
    public void putAt(int aRow, int aCol, double aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aRow, aCol, aValue);
    }
    native static void putAt_(long aPtr, int aRow, int aCol, double aValue);
    
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedDoubleCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedDoubleCPointer(rightShift_(mPtr, aCount));
    }
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedDoubleCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedDoubleCPointer(leftShift_(mPtr, aCount));
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedDoubleCPointer copy() {
        return new NestedDoubleCPointer(mPtr);
    }
    
    /**
     * 将此嵌套指针转换成 jse 的矩阵 {@link IMatrix}，为这个嵌套指针对应矩阵的引用
     * @param aRowNum 此指针的对应矩阵的行数
     * @param aColNum 此指针的对应矩阵的列数
     * @return 转换后的矩阵
     * @see IMatrix
     */
    public IMatrix asMat(final int aRowNum, final int aColNum) {
        return new RefMatrix() {
            @Override public double get(int aRow, int aCol) {
                AbstractMatrix.rangeCheckRow(aRow, aRowNum);
                AbstractMatrix.rangeCheckCol(aCol, aColNum);
                return NestedDoubleCPointer.this.getAt(aRow, aCol);
            }
            @Override public void set(int aRow, int aCol, double aValue) {
                AbstractMatrix.rangeCheckRow(aRow, aRowNum);
                AbstractMatrix.rangeCheckCol(aCol, aColNum);
                NestedDoubleCPointer.this.putAt(aRow, aCol, aValue);
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
    @Override public List<DoubleCPointer> asList(final int aSize) {
        return new AbstractRandomAccessList<DoubleCPointer>() {
            @Override public DoubleCPointer get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public DoubleCPointer set(int index, @NotNull DoubleCPointer element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                DoubleCPointer oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
