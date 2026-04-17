package jse.cptr;

import jse.clib.MiMalloc;
import jse.clib.UnsafeJNI;
import org.jetbrains.annotations.ApiStatus;

/**
 * 针对 c 指针通用接口
 * @author liqa
 */
@ApiStatus.Experimental
public interface ICPointer extends IPointer {
    /**
     * 调用 c 中的 {@code free} 来释放一个 c 指针对应的内存
     * <p>
     * 实际内部默认会统一使用 {@link MiMalloc} 来加速内存释放的过程
     *
     * @throws IllegalStateException 如果此 c 指针是空指针
     */
    @UnsafeJNI("Free wild pointer will directly result in JVM SIGSEGV")
    void free();
    
    /**
     * 直接调用 c 中的 {@code memcpy} 来将此数组值拷贝到另一个 c 数组中
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     * <p>
     * 为了避免歧义，特定类型的指针直接提供对应的 {@code fill} 方法，从而这里的
     * {@code aCount} 永远和 c 中的 {@code memcpy} 参数一致
     *
     * @param rDest 需要拷贝的目标 c 指针
     * @param aCount 需要拷贝的数据长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void memcpy2dest(ICPointer rDest, long aCount);
    
    /**
     * 直接调用 c 中的 {@code memcpy} 来将输入 c 指针值拷贝到此数组
     * <p>
     * 注意此方法和 c 一致，并不会对此 c 指针对应的内存的长度进行检测（内部不会存储内存长度）
     * <p>
     * 为了避免歧义，特定类型的指针直接提供对应的 {@code fill} 方法，从而这里的
     * {@code aCount} 永远和 c 中的 {@code memcpy} 参数一致
     *
     * @param aSrc 需要拷贝的目标 c 指针
     * @param aCount 需要拷贝的数据长度
     */
    @UnsafeJNI("Invalid input count may directly result in JVM SIGSEGV")
    void memcpy2this(ICPointer aSrc, long aCount);
    
    /**
     * 获取此指针对应类型的长度
     * @return {@code sizeof(xxx)}
     */
    long typeSize();
    
    /**
     * 向后移动指针，即对应 c 中的 {@code ++ptr}
     */
    void next();
    /**
     * 指针向后移动指定步数，即对应 c 中的 {@code ptr += aCount}
     * @param aCount 需要移动的步数
     */
    void rightShift(long aCount);
    /**
     * 计算并返回向后移动指定步数的指针，即对应 c 中的 {@code ptr + aCount}
     * @param aCount 需要移动的步数
     * @return 移动后的指针对象
     */
    ICPointer plus(long aCount);
    
    /**
     * 向前移动指针，即对应 c 中的 {@code --ptr}
     */
    void previous();
    /**
     * 指针向前移动指定步数，即对应 c 中的 {@code ptr -= aCount}
     * @param aCount 需要移动的步数
     */
    void leftShift(long aCount);
    /**
     * 计算并返回向前移动指定步数的指针，即对应 c 中的 {@code ptr - aCount}
     * @param aCount 需要移动的步数
     * @return 移动后的指针对象
     */
    ICPointer minus(long aCount);
    
    /**
     * 拷贝一份 c 指针包装类，注意此方法不会实际拷贝内部
     * c 指针对应的内存，因此返回对象内部存储了相同的 c 指针
     * @return 拷贝的 c 指针包装类，包含相同的 c 指针
     */
    ICPointer copy();
}
