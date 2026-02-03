package jse.cptr;

import jse.clib.MiMalloc;
import jse.clib.UnsafeJNI;
import jse.code.collection.AbstractRandomAccessList;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 c 中的 {@code void **} 处理的指针，
 * 用于处理一般的 c 数组
 * @see CPointer CPointer: 一般的 c 指针包装类
 * @author liqa
 */
public class NestedCPointer extends CPointer {
    /**
     * 直接从一个任意的 c 指针初始化一个 {@link NestedCPointer} 对象
     * @param aPtr 需要包装的 c 指针值
     */
    @ApiStatus.Internal public NestedCPointer(long aPtr) {super(aPtr);}
    
    /**
     * 调用 c 中的 {@code malloc} 来分配内存创建一个 c 指针
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存分配的过程
     *
     * @param aCount 需要分配的数组长度，即等价于 {@code malloc(aCount*sizeof(void *))}
     * @return 创建的嵌套指针的 c 指针对象
     */
    @UnsafeJNI("Manual free required")
    public static NestedCPointer malloc(int aCount) {
        return new NestedCPointer(malloc_(aCount, TYPE_SIZE));
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
    public static NestedCPointer calloc(int aCount) {
        return new NestedCPointer(calloc_(aCount, TYPE_SIZE));
    }
    /** {@code sizeof(void *)} */
    public final static int TYPE_SIZE = typeSize_();
    private native static int typeSize_();
    
    
    /**
     * 对此指针解引用，获取内部值，即对应 c 中的 {@code *ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public CPointer get() {
        if (isNull()) throw new NullPointerException();
        return new CPointer(get_(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 double 指针，即对应 c 中的 {@code (double *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public DoubleCPointer getAsDoubleCPointer() {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(get_(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成 int 指针，即对应 c 中的 {@code (int *)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public IntCPointer getAsIntCPointer() {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(get_(mPtr));
    }
    /**
     * 对此指针解引用，获取内部值，并转换成嵌套指针，即对应 c 中的 {@code (void **)*ptr}
     * @return 此指针对应的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public NestedCPointer getAsNestedCPointer() {
        if (isNull()) throw new NullPointerException();
        return new NestedCPointer(get_(mPtr));
    }
    native static long get_(long aPtr);
    
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的值，即对应 c 中的 {@code ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public CPointer getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new CPointer(getAt_(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 double 指针，即对应 c 中的 {@code (double *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public DoubleCPointer getAsDoubleCPointerAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(getAt_(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成 int 指针，即对应 c 中的 {@code (int *)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public IntCPointer getAsIntCPointerAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(getAt_(mPtr, aIdx));
    }
    /**
     * 将此指针当作一个 c 的数组，获取内部指定位置的数值，并转换成嵌套指针，即对应 c 中的 {@code (void **)ptr[aIdx]}
     * @param aIdx 需要获取的索引位置
     * @return 此指针对应的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public NestedCPointer getAsNestedCPointerAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new NestedCPointer(getAt_(mPtr, aIdx));
    }
    native static long getAt_(long aPtr, int aIdx);
    
    /**
     * 设置此指针对应的值，即对应 c 中的 {@code *ptr = aValue}
     * @param aValue 需要设置的值
     */
    @UnsafeJNI("Access wild pointer will directly result in JVM SIGSEGV")
    public void set(@NotNull ICPointer aValue) {
        if (isNull()) throw new NullPointerException();
        set_(mPtr, aValue.ptr_());
    }
    native static void set_(long aPtr, long aValue);
    
    /**
     * 将此指针当作一个 c 的数组，设置内部指定位置的值，即对应 c 中的 {@code ptr[aIdx] = aValue}
     * @param aIdx 需要设置的索引位置
     * @param aValue 需要设置的值
     */
    @UnsafeJNI("Invalid input index may directly result in JVM SIGSEGV")
    public void putAt(int aIdx, @NotNull ICPointer aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aIdx, aValue.ptr_());
    }
    native static void putAt_(long aPtr, int aIdx, long aValue);
    
    
    /**
     * 向后移动指针，即对应 c 中的 {@code ++ptr}
     */
    public void next() {
        if (isNull()) throw new NullPointerException();
        mPtr = next_(mPtr);
    }
    native static long next_(long aPtr);
    
    /**
     * 指针向后移动指定步数，即对应 c 中的 {@code ptr += aCount}
     * @param aCount 需要移动的步数
     */
    public void rightShift(int aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = rightShift_(mPtr, aCount);
    }
    native static long rightShift_(long aPtr, int aCount);
    /**
     * 计算并返回向后移动指定步数的指针，即对应 c 中的 {@code ptr + aCount}
     * @param aCount 需要移动的步数
     * @return 移动后的指针对象
     */
    public NestedCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedCPointer(rightShift_(mPtr, aCount));
    }
    
    /**
     * 向前移动指针，即对应 c 中的 {@code --ptr}
     */
    public void previous() {
        if (isNull()) throw new NullPointerException();
        mPtr = previous_(mPtr);
    }
    native static long previous_(long aPtr);
    
    /**
     * 指针向前移动指定步数，即对应 c 中的 {@code ptr -= aCount}
     * @param aCount 需要移动的步数
     */
    public void leftShift(int aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = leftShift_(mPtr, aCount);
    }
    native static long leftShift_(long aPtr, int aCount);
    /**
     * 计算并返回向前移动指定步数的指针，即对应 c 中的 {@code ptr - aCount}
     * @param aCount 需要移动的步数
     * @return 移动后的指针对象
     */
    public NestedCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedCPointer(leftShift_(mPtr, aCount));
    }
    
    /**
     * {@inheritDoc}
     * @return {@inheritDoc}
     */
    @Override public NestedCPointer copy() {
        return new NestedCPointer(mPtr);
    }
    
    /**
     * 将此嵌套指针转换成 java 的列表 {@link List}，为这个指针对应数组的引用
     * @param aSize 此指针的对应数组的长度
     * @return 转换后的列表
     * @see List
     */
    @UnsafeJNI("Invalid input size may result in JVM SIGSEGV")
    public List<? extends CPointer> asList(final int aSize) {
        return new AbstractRandomAccessList<CPointer>() {
            @Override public CPointer get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public CPointer set(int index, @NotNull CPointer element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                CPointer oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
