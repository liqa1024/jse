package jse.cptr;

import jse.clib.MiMalloc;
import jse.clib.UnsafeJNI;
import jse.code.collection.AbstractRandomAccessList;
import jse.math.IDataShell;
import jse.math.vector.AbstractVector;
import jse.math.vector.IFloatVector;
import jse.math.vector.RefFloatVector;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 c 中的 {@code float *} 处理的指针，
 * 虽然一般来说 c 中的 {@code float} 和 java 的 {@code jfloat}
 * 等价，但这里为了不失一般性依旧单指 c 中的 {@code float}。
 * @see CPointer CPointer: 一般的 c 指针包装类
 * @author liqa
 */
public class FloatCPointer extends CPointer implements IDoubleOrFloatCPointer {
    /**
     * 直接从一个任意的 c 指针初始化一个 {@link FloatCPointer} 对象
     * @param aPtr 需要包装的 c 指针值
     */
    @ApiStatus.Internal public FloatCPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 c 中的 {@code malloc} 来分配内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(float))}
     * @return 创建的单精度浮点 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static FloatCPointer malloc(long aCount) {
        return new FloatCPointer(malloc0(aCount, TYPE_SIZE));
    }
    /**
     * 调用 c 中的 {@code calloc} 来分配全零内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code calloc(aCount, sizeof(float))}
     * @return 创建的单精度浮点 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static FloatCPointer calloc(long aCount) {
        return new FloatCPointer(calloc0(aCount, TYPE_SIZE));
    }
    /**
     * {@inheritDoc}
     * @return {@code sizeof(float)}
     */
    @Override public long typeSize() {return TYPE_SIZE;}
    public final static long TYPE_SIZE = typeSize0();
    private native static long typeSize0();
    
    /**
     * 将 jse 的 {@code IDataShell<float[]>} 填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code IDataShell<float[]>} 数据
     * @see IDataShell
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    public void fill(IDataShell<float[]> aData) {
        if (aData.internalDataSize()==0) return;
        if (isNull()) throw new NullPointerException();
        fill0(mPtr, aData.internalDataWithLengthCheck(), aData.internalDataShift(), aData.internalDataSize());
    }
    /**
     * 将 java 的 {@code float[]} 填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 {@code float[]} 数据
     * @param aStart 需要读取的 aData 开始的索引
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(float[] aData, int aStart, int aCount) {
        if (aCount==0) return;
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart+aCount);
        fill0(mPtr, aData, aStart, aCount);
    }
    @ApiStatus.Internal
    public native static void fill0(long rPtr, float[] aData, int aStart, int aCount);
    
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    @Override public void fillF(IDataShell<float[]> aData) {
        fill(aData);
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aStart {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillF(float[] aData, int aStart, int aCount) {
        fill(aData, aStart, aCount);
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     */
    @UnsafeJNI("Invalid input size may directly result in JVM SIGSEGV")
    @Override public void fillD(IDataShell<double[]> aData) {
        if (aData.internalDataSize()==0) return;
        if (isNull()) throw new NullPointerException();
        fillD0(mPtr, aData.internalDataWithLengthCheck(), aData.internalDataShift(), aData.internalDataSize());
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aStart {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillD(double[] aData, int aStart, int aCount) {
        if (aCount==0) return;
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart+aCount);
        fillD0(mPtr, aData, aStart, aCount);
    }
    @ApiStatus.Internal
    public native static void fillD0(long rPtr, double[] aData, int aStart, int aCount);
    
    /**
     * 将另一个 c 指针的数据填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 c 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(FloatCPointer aData, long aCount) {
        if (aCount==0) return;
        memcpy2this(aData, aCount*TYPE_SIZE);
    }
    
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillF(FloatCPointer aData, long aCount) {
        fill(aData, aCount);
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillD(DoubleCPointer aData, long aCount) {
        if (aCount==0) return;
        if (isNull() || aData.isNull()) throw new NullPointerException();
        fillD1(mPtr, aData.mPtr, aCount);
    }
    @ApiStatus.Internal
    public native static void fillD1(long rPtr, long aData, long aCount);
    
    /**
     * 将给定输入数值填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aValue 需要填充的数值
     * @param aCount 需要读取的 aData 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(float aValue, long aCount) {
        if (aCount==0) return;
        if (isNull()) throw new NullPointerException();
        fill2(mPtr, aValue, aCount);
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillD(double aData, long aCount) {
        fill((float)aData, aCount);
    }
    /**
     * {@inheritDoc}
     * @param aData {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void fillF(float aData, long aCount) {
        fill(aData, aCount);
    }
    @ApiStatus.Internal
    public native static void fill2(long rPtr, float aValue, long aCount);
    
    
    /**
     * 将此 c 指针对应的内存数值写入 jse 的 {@code IDataShell<float[]>} 中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
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
     * 将此 c 指针对应的内存数值写入 java 的 {@code float[]} 中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 需要写入的 {@code float[]}
     * @param aStart 需要写入的 rDest 开始的索引
     * @param aCount 需要写入的 rDest 的长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(float[] rDest, int aStart, int aCount) {
        if (aCount==0) return;
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart+aCount);
        parse2dest0(mPtr, rDest, aStart, aCount);
    }
    @ApiStatus.Internal
    public native static void parse2dest0(long aPtr, float[] rDest, int aStart, int aCount);
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
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
    @Override public void parse2destF(float[] rDest, int aStart, int aCount) {
        if (aCount==0) return;
        parse2dest(rDest, aStart, aCount);
    }
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
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
     * @param aStart {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void parse2destD(double[] rDest, int aStart, int aCount) {
        if (aCount==0) return;
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart+aCount);
        parse2destD0(mPtr, rDest, aStart, aCount);
    }
    @ApiStatus.Internal
    public native static void parse2destD0(long aPtr, double[] rDest, int aStart, int aCount);
    
    /**
     * 将此 c 指针的数据填充到另一个 c 指针中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 输入的 c 指针数据
     * @param aCount 需要写入 rDest 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(FloatCPointer rDest, long aCount) {
        if (aCount==0) return;
        memcpy2dest(rDest, aCount*TYPE_SIZE);
    }
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void parse2destF(FloatCPointer rDest, long aCount) {
        parse2dest(rDest, aCount);
    }
    /**
     * {@inheritDoc}
     * @param rDest {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    @Override public void parse2destD(DoubleCPointer rDest, long aCount) {
        if (aCount==0) return;
        if (isNull() || rDest.isNull()) throw new NullPointerException();
        parse2destD1(mPtr, rDest.mPtr, aCount);
    }
    @ApiStatus.Internal
    public native static void parse2destD1(long aPtr, long rDest, long aCount);
    
    
    /**
     * 对此指针解引用，获取内部数值，即对应 c 中的 {@code *ptr}
     * @return 此指针对应的数值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public float get() {
        if (isNull()) throw new NullPointerException();
        return get0(mPtr);
    }
    @ApiStatus.Internal
    public native static float get0(long aPtr);
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    @Override public float getF() {
        return get();
    }
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    @Override public double getD() {
        return (double)get();
    }
    
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，即对应 c 中的 {@code ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此索引对应的数值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public float getAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return getAt0(mPtr, aIdx);
    }
    @ApiStatus.Internal
    public native static float getAt0(long aPtr, long aIdx);
    /** 用于兼容 groovy 运算符重载，这是 groovy 的 bug */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public float getAt(int aIdx) {return getAt((long)aIdx);}
    
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    @Override public float getAtF(long aIdx) {
        return getAt(aIdx);
    }
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @return {@inheritDoc}
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    @Override public double getAtD(long aIdx) {
        return (double)getAt(aIdx);
    }
    
    /**
     * 设置此指针对应的数值，即对应 c 中的 {@code *ptr = aValue}
     * @param aValue 需要设置的数值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public void set(float aValue) {
        if (isNull()) throw new NullPointerException();
        set0(mPtr, aValue);
    }
    @ApiStatus.Internal
    public native static void set0(long aPtr, float aValue);
    /**
     * {@inheritDoc}
     * @param aValue {@inheritDoc}
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    @Override public void setF(float aValue) {
        set(aValue);
    }
    /**
     * {@inheritDoc}
     * @param aValue {@inheritDoc}
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    @Override public void setD(double aValue) {
        set((float)aValue);
    }
    
    /**
     * 将此指针当作一个 c 的数组，设置内部指定位置的数值，即对应 c 中的 {@code ptr[aIdx] = aValue}
     * @param aIdx 需要设置的索引位置
     * @param aValue 需要设置的数值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public void putAt(long aIdx, float aValue) {
        if (isNull()) throw new NullPointerException();
        putAt0(mPtr, aIdx, aValue);
    }
    @ApiStatus.Internal
    public native static void putAt0(long aPtr, long aIdx, float aValue);
    /** 用于兼容 groovy 运算符重载，这是 groovy 的 bug */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public void putAt(int aIdx, float aValue) {putAt((long)aIdx, aValue);}
    
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @param aValue {@inheritDoc}
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    @Override public void putAtF(long aIdx, float aValue) {
        putAt(aIdx, aValue);
    }
    /**
     * {@inheritDoc}
     * @param aIdx {@inheritDoc}
     * @param aValue {@inheritDoc}
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    @Override public void putAtD(long aIdx, double aValue) {
        putAt(aIdx, (float)aValue);
    }
    
    /**
     * {@inheritDoc}
     */
    @Override public void next() {
        if (isNull()) throw new NullPointerException();
        mPtr = next0(mPtr);
    }
    @ApiStatus.Internal
    public native static long next0(long aPtr);
    
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @Override public void rightShift(long aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = rightShift0(mPtr, aCount);
    }
    @ApiStatus.Internal
    public native static long rightShift0(long aPtr, long aCount);
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public FloatCPointer plus(long aCount) {
        if (isNull()) throw new NullPointerException();
        return new FloatCPointer(rightShift0(mPtr, aCount));
    }
    
    /**
     * {@inheritDoc}
     */
    @Override public void previous() {
        if (isNull()) throw new NullPointerException();
        mPtr = previous0(mPtr);
    }
    @ApiStatus.Internal
    public native static long previous0(long aPtr);
    
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     */
    @Override public void leftShift(long aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = leftShift0(mPtr, aCount);
    }
    @ApiStatus.Internal
    public native static long leftShift0(long aPtr, long aCount);
    /**
     * {@inheritDoc}
     * @param aCount {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public FloatCPointer minus(long aCount) {
        if (isNull()) throw new NullPointerException();
        return new FloatCPointer(leftShift0(mPtr, aCount));
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public FloatCPointer copy() {
        return new FloatCPointer(mPtr);
    }
    
    /**
     * 将此 float 指针转换成 jse 的向量 {@link IFloatVector}，为这个指针对应数组的引用
     * @param aSize 此指针的对应数组的长度
     * @return 转换后的向量
     * @see IFloatVector
     */
    @UnsafeJNI("Invalid input size may result in JVM SIGSEGV")
    public IFloatVector asVec(final int aSize) {
        return new RefFloatVector() {
            @Override public float get(int aIdx) {AbstractVector.rangeCheck(aIdx, aSize); return FloatCPointer.this.getAt(aIdx);}
            @Override public void set(int aIdx, float aValue) {AbstractVector.rangeCheck(aIdx, aSize); FloatCPointer.this.putAt(aIdx, aValue);}
            @Override public int size() {return aSize;}
        };
    }
    /**
     * 将此 float 指针转换成 java 的列表 {@link List}，为这个指针对应数组的引用
     * @param aSize 此指针的对应数组的长度
     * @return 转换后的列表
     * @see List
     */
    @UnsafeJNI("Invalid input size may result in JVM SIGSEGV")
    public List<Float> asList(final int aSize) {
        return new AbstractRandomAccessList<Float>() {
            @Override public Float get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public Float set(int index, @NotNull Float element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                float oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
