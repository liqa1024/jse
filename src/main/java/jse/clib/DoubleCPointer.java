package jse.clib;

import jse.code.collection.AbstractRandomAccessList;
import jse.math.IDataShell;
import jse.math.vector.AbstractVector;
import jse.math.vector.IVector;
import jse.math.vector.RefVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 c 中的 {@code double *} 处理的指针，
 * 虽然一般来说 c 中的 {@code double} 和 java 的 {@code jdouble}
 * 等价，但这里为了不失一般性依旧单指 c 中的 {@code double}。
 * @see CPointer CPointer: 一般的 c 指针包装类
 * @author liqa
 */
public class DoubleCPointer extends CPointer {
    /**
     * 直接从一个任意的 c 指针初始化一个 {@link DoubleCPointer} 对象
     * @param aPtr 需要包装的 c 指针值
     */
    @ApiStatus.Internal public DoubleCPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 c 中的 {@code malloc} 来分配内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(double))}
     * @return 创建的双精度浮点 c 指针对象
     */
    public static DoubleCPointer malloc(int aCount) {
        return new DoubleCPointer(malloc_(aCount, TYPE_SIZE));
    }
    /**
     * 调用 c 中的 {@code calloc} 来分配全零内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code calloc(aCount, sizeof(double))}
     * @return 创建的双精度浮点 c 指针对象
     */
    public static DoubleCPointer calloc(int aCount) {
        return new DoubleCPointer(calloc_(aCount, TYPE_SIZE));
    }
    /** {@code sizeof(double)} */
    public final static int TYPE_SIZE = typeSize_();
    private native static int typeSize_();
    
    /**
     * 将 jse 的 {@code IDataShell<double[]>} 填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code IDataShell<double[]>} 数据
     * @see IDataShell
     */
    public void fill(IDataShell<double[]> aData) {
        if (isNull()) throw new NullPointerException();
        int tStart = aData.internalDataShift();
        int tCount = aData.internalDataSize();
        fill0(mPtr, aData.internalDataWithLengthCheck(tCount, tStart), tStart, tCount);
    }
    /**
     * 将 java 的 {@code double[]} 填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code double[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aCount 需要读取的 aData 的长度
     */
    public void fill(double[] aData, int aStart, int aCount) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart+aCount);
        fill0(mPtr, aData, aStart, aCount);
    }
    private native static void fill0(long rPtr, double[] aData, int aStart, int aCount);
    /**
     * 将给定输入数值填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aValue 需要填充的数值
     * @param aCount 需要读取的 aData 的长度
     */
    public void fill(double aValue, int aCount) {
        if (isNull()) throw new NullPointerException();
        fill1(mPtr, aValue, aCount);
    }
    private native static void fill1(long rPtr, double aValue, int aCount);
    /**
     * 将另一个 c 指针的数据填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的任意 c 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    public void fill(CPointer aData, int aCount) {
        aData.memcpy(this, aCount*TYPE_SIZE);
    }
    
    /**
     * 将此 c 指针对应的内存数值写入 jse 的 {@code IDataShell<double[]>} 中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code IDataShell<double[]>}
     * @see IDataShell
     */
    public void parse2dest(IDataShell<double[]> rDest) {
        if (isNull()) throw new NullPointerException();
        int tStart = rDest.internalDataShift();
        int tCount = rDest.internalDataSize();
        parse2dest_(mPtr, rDest.internalDataWithLengthCheck(tCount, tStart), tStart, tCount);
    }
    /**
     * 将此 c 指针对应的内存数值写入 java 的 {@code double[]} 中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code double[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aCount 需要写入的 rDest 的长度
     */
    public void parse2dest(double[] rDest, int aStart, int aCount) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart+aCount);
        parse2dest_(mPtr, rDest, aStart, aCount);
    }
    private native static void parse2dest_(long aPtr, double[] rDest, int aStart, int aCount);
    
    
    /**
     * 对此指针解引用，获取内部数值，即对应 c 中的 {@code *ptr}
     * @return 此指针对应的数值
     */
    public double get() {
        if (isNull()) throw new NullPointerException();
        return get_(mPtr);
    }
    private native static double get_(long aPtr);
    
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，即对应 c 中的 {@code ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此索引对应的数值
     */
    public double getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return getAt_(mPtr, aIdx);
    }
    private native static double getAt_(long aPtr, int aIdx);
    
    /**
     * 设置此指针对应的数值，即对应 c 中的 {@code *ptr = aValue}
     * @param aValue 需要设置的数值
     */
    public void set(double aValue) {
        if (isNull()) throw new NullPointerException();
        set_(mPtr, aValue);
    }
    private native static void set_(long aPtr, double aValue);
    
    /**
     * 将此指针当作一个 c 的数组，设置内部指定位置的数值，即对应 c 中的 {@code ptr[aIdx] = aValue}
     * @param aIdx 需要设置的索引位置
     * @param aValue 需要设置的数值
     */
    public void putAt(int aIdx, double aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aIdx, aValue);
    }
    private native static void putAt_(long aPtr, int aIdx, double aValue);
    
    /**
     * 向后移动指针，即对应 c 中的 {@code ++ptr}
     */
    public void next() {
        if (isNull()) throw new NullPointerException();
        mPtr = next_(mPtr);
    }
    private native static long next_(long aPtr);
    
    /**
     * 指针向后移动指定步数，即对应 c 中的 {@code ptr += aCount}
     * @param aCount 需要移动的步数
     */
    public void rightShift(int aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = rightShift_(mPtr, aCount);
    }
    private native static long rightShift_(long aPtr, int aCount);
    /**
     * 计算并返回向后移动指定步数的指针，即对应 c 中的 {@code ptr + aCount}
     * @param aCount 需要移动的步数
     * @return 移动后的指针对象
     */
    public DoubleCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(rightShift_(mPtr, aCount));
    }
    
    /**
     * 向前移动指针，即对应 c 中的 {@code --ptr}
     */
    public void previous() {
        if (isNull()) throw new NullPointerException();
        mPtr = previous_(mPtr);
    }
    private native static long previous_(long aPtr);
    
    /**
     * 指针向前移动指定步数，即对应 c 中的 {@code ptr -= aCount}
     * @param aCount 需要移动的步数
     */
    public void leftShift(int aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = leftShift_(mPtr, aCount);
    }
    private native static long leftShift_(long aPtr, int aCount);
    /**
     * 计算并返回向前移动指定步数的指针，即对应 c 中的 {@code ptr - aCount}
     * @param aCount 需要移动的步数
     * @return 移动后的指针对象
     */
    public DoubleCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(leftShift_(mPtr, aCount));
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public DoubleCPointer copy() {
        return new DoubleCPointer(mPtr);
    }
    
    /**
     * 将此 double 指针转换成 jse 的向量 {@link IVector}，为这个指针对应数组的引用
     * @param aSize 此指针的对应数组的长度
     * @return 转换后的向量
     * @see IVector
     */
    public IVector asVec(final int aSize) {
        return new RefVector() {
            @Override public double get(int aIdx) {AbstractVector.rangeCheck(aIdx, aSize); return DoubleCPointer.this.getAt(aIdx);}
            @Override public void set(int aIdx, double aValue) {AbstractVector.rangeCheck(aIdx, aSize); DoubleCPointer.this.putAt(aIdx, aValue);}
            @Override public int size() {return aSize;}
        };
    }
    /**
     * 将此 double 指针转换成 java 的列表 {@link List}，为这个指针对应数组的引用
     * @param aSize 此指针的对应数组的长度
     * @return 转换后的列表
     * @see List
     */
    public List<Double> asList(final int aSize) {
        return new AbstractRandomAccessList<Double>() {
            @Override public Double get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public Double set(int index, @NotNull Double element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                double oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
