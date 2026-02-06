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
    void memcpy2dest(ICPointer rDest, int aCount);
    
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
    void memcpy2this(ICPointer aSrc, int aCount);
}
