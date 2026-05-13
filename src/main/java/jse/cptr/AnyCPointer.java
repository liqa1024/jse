package jse.cptr;

import jse.clib.MiMalloc;
import jse.clib.UnsafeJNI;
import jse.code.collection.AbstractRandomAccessList;
import jse.gpu.CudaPointer;
import jse.gpu.DoubleCudaPointer;
import jse.gpu.FloatCudaPointer;
import jse.gpu.IntCudaPointer;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 c 中的 {@code void **} 处理的指针，
 * 用于处理任意的指针数组
 * @see CPointer CPointer: 一般的 c 指针包装类
 * @author liqa
 */
public class AnyCPointer extends CPointer {
    /**
     * 直接从一个任意的 c 指针初始化一个 {@link AnyCPointer} 对象
     * @param aPtr 需要包装的 c 指针值
     */
    @ApiStatus.Internal public AnyCPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 c 中的 {@code malloc} 来分配内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(void *))}
     * @return 创建的嵌套指针的 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static AnyCPointer malloc(long aCount) {
        return new AnyCPointer(malloc0(aCount, TYPE_SIZE));
    }
    /**
     * 调用 c 中的 {@code calloc} 来分配全零内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code calloc(aCount, sizeof(void *))}
     * @return 创建的嵌套指针的 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static AnyCPointer calloc(long aCount) {
        return new AnyCPointer(calloc0(aCount, TYPE_SIZE));
    }
    /** {@code sizeof(void *)} */
    public final static long TYPE_SIZE = typeSize0();
    private native static long typeSize0();
    
    /**
     * 将另一个 c 指针的数据填充到此 c 指针对应的内存中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param aData 输入的 c 指针数据
     * @param aCount 需要读取的 aData 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void fill(AnyCPointer aData, long aCount) {
        memcpy2this(aData, aCount*TYPE_SIZE);
    }
    /**
     * 将此 c 指针的数据填充到另一个 c 指针中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     *
     * @param rDest 输入的 c 指针数据
     * @param aCount 需要写入 rDest 的长度，实际为 {@code aCount * TYPE_SIZE}
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    public void parse2dest(AnyCPointer rDest, long aCount) {
        memcpy2dest(rDest, aCount*TYPE_SIZE);
    }
    
    /**
     * 对此指针解引用，获取内部值，即对应 c 中的 {@code *ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public IPointer get() {
        if (isNull()) throw new NullPointerException();
        return new Pointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成标准 c 指针，即对应 c 中的 {@code (void *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public CPointer getAsCPointer() {
        if (isNull()) throw new NullPointerException();
        return new CPointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 double 或 float 指针
     * @param aSingle 是否转换为单精度
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public IDoubleOrFloatCPointer getAsDoubleOrFloatCPointer(boolean aSingle) {
        if (isNull()) throw new NullPointerException();
        return aSingle ? new FloatCPointer(get0(mPtr)) : new DoubleCPointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 double 指针，即对应 c 中的 {@code (double *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public DoubleCPointer getAsDoubleCPointer() {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 float 指针，即对应 c 中的 {@code (float *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public FloatCPointer getAsFloatCPointer() {
        if (isNull()) throw new NullPointerException();
        return new FloatCPointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 int 指针，即对应 c 中的 {@code (int *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public IntCPointer getAsIntCPointer() {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成嵌套指针，即对应 c 中的 {@code (void **)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public AnyCPointer getAsAnyCPointer() {
        if (isNull()) throw new NullPointerException();
        return new AnyCPointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 cuda 指针，即对应 c 中的 {@code (void *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public CudaPointer getAsCudaPointer() {
        if (isNull()) throw new NullPointerException();
        return new CudaPointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 cuda int 指针，即对应 c 中的 {@code (int *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public IntCudaPointer getAsIntCudaPointer() {
        if (isNull()) throw new NullPointerException();
        return new IntCudaPointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 cuda float 指针，即对应 c 中的 {@code (float *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public FloatCudaPointer getAsFloatCudaPointer() {
        if (isNull()) throw new NullPointerException();
        return new FloatCudaPointer(get0(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 cuda double 指针，即对应 c 中的 {@code (double *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public DoubleCudaPointer getAsDoubleCudaPointer() {
        if (isNull()) throw new NullPointerException();
        return new DoubleCudaPointer(get0(mPtr));
    }
    native static long get0(long aPtr);
    
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的值，即对应 c 中的 {@code ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public IPointer getAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new Pointer(getAt0(mPtr, aIdx));
    }
    /** 用于兼容 groovy 运算符重载，这是 groovy 的 bug */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public IPointer getAt(int aIdx) {return getAt((long)aIdx);}
    
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成标准 c 指针，即对应 c 中的 {@code (void *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public CPointer getAsCPointerAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new CPointer(getAt0(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 double 或者 float 指针
     * @param aSingle 是否转换为单精度
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public IDoubleOrFloatCPointer getAsDoubleOrFloatCPointerAt(boolean aSingle, long aIdx) {
        if (isNull()) throw new NullPointerException();
        return aSingle ? new FloatCPointer(getAt0(mPtr, aIdx)) : new DoubleCPointer(getAt0(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 double 指针，即对应 c 中的 {@code (double *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public DoubleCPointer getAsDoubleCPointerAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(getAt0(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 float 指针，即对应 c 中的 {@code (float *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public FloatCPointer getAsFloatCPointerAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new FloatCPointer(getAt0(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 int 指针，即对应 c 中的 {@code (int *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public IntCPointer getAsIntCPointerAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(getAt0(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成嵌套指针，即对应 c 中的 {@code (void **)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public AnyCPointer getAsAnyCPointerAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new AnyCPointer(getAt0(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 cuda 指针，即对应 c 中的 {@code (void *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public CudaPointer getAsCudaPointerAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new CudaPointer(getAt0(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 cuda int 指针，即对应 c 中的 {@code (int *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public IntCudaPointer getAsIntCudaPointerAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new IntCudaPointer(getAt0(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 cuda float 指针，即对应 c 中的 {@code (float *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public FloatCudaPointer getAsFloatCudaPointerAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new FloatCudaPointer(getAt0(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 cuda double 指针，即对应 c 中的 {@code (double *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public DoubleCudaPointer getAsDoubleCudaPointerAt(long aIdx) {
        if (isNull()) throw new NullPointerException();
        return new DoubleCudaPointer(getAt0(mPtr, aIdx));
    }
    native static long getAt0(long aPtr, long aIdx);
    
    /**
     * 设置此指针对应的值，即对应 c 中的 {@code *ptr = aValue}
     * @param aValue 需要设置的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public void set(@NotNull IPointer aValue) {
        if (isNull()) throw new NullPointerException();
        set0(mPtr, aValue.ptr_());
    }
    native static void set0(long aPtr, long aValue);
    
    /**
     * 将此指针当作一个 c 的数组，设置内部指定位置的值，即对应 c 中的 {@code ptr[aIdx] = aValue}
     * @param aIdx 需要设置的索引位置
     * @param aValue 需要设置的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public void putAt(long aIdx, @NotNull IPointer aValue) {
        if (isNull()) throw new NullPointerException();
        putAt0(mPtr, aIdx, aValue.ptr_());
    }
    native static void putAt0(long aPtr, long aIdx, long aValue);
    /** 用于兼容 groovy 运算符重载，这是 groovy 的 bug */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public void putAt(int aIdx, @NotNull IPointer aValue) {putAt((long)aIdx, aValue);}
    
    /**
     * 向后移动指针，即对应 c 中的 {@code ++ptr}
     */
    public void next() {
        if (isNull()) throw new NullPointerException();
        mPtr = next0(mPtr);
    }
    native static long next0(long aPtr);
    
    /**
     * 指针向后移动指定步数，即对应 c 中的 {@code ptr += aCount}
     * @param aCount 需要移动的步数
     */
    public void rightShift(long aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = rightShift0(mPtr, aCount);
    }
    native static long rightShift0(long aPtr, long aCount);
    /**
     * 计算并返回向后移动指定步数的指针，即对应 c 中的 {@code ptr + aCount}
     * @param aCount 需要移动的步数
     * @return 移动后的指针对象
     */
    public AnyCPointer plus(long aCount) {
        if (isNull()) throw new NullPointerException();
        return new AnyCPointer(rightShift0(mPtr, aCount));
    }
    
    /**
     * 向前移动指针，即对应 c 中的 {@code --ptr}
     */
    public void previous() {
        if (isNull()) throw new NullPointerException();
        mPtr = previous0(mPtr);
    }
    native static long previous0(long aPtr);
    
    /**
     * 指针向前移动指定步数，即对应 c 中的 {@code ptr -= aCount}
     * @param aCount 需要移动的步数
     */
    public void leftShift(long aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = leftShift0(mPtr, aCount);
    }
    native static long leftShift0(long aPtr, long aCount);
    /**
     * 计算并返回向前移动指定步数的指针，即对应 c 中的 {@code ptr - aCount}
     * @param aCount 需要移动的步数
     * @return 移动后的指针对象
     */
    public AnyCPointer minus(long aCount) {
        if (isNull()) throw new NullPointerException();
        return new AnyCPointer(leftShift0(mPtr, aCount));
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public AnyCPointer copy() {
        return new AnyCPointer(mPtr);
    }
    
    /**
     * 将此嵌套指针转换成 java 的列表 {@link List}，为这个指针对应数组的引用
     * @param aSize 此指针的对应数组的长度
     * @return 转换后的列表
     * @see List
     */
    @UnsafeJNI("Invalid input size may result in JVM SIGSEGV")
    public List<? extends IPointer> asList(final int aSize) {
        return new AbstractRandomAccessList<IPointer>() {
            @Override public IPointer get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public IPointer set(int index, @NotNull IPointer element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                IPointer oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
