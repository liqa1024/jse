package jse.clib;

import org.jetbrains.annotations.ApiStatus;

/**
 * C 指针通用接口，方便进行抽象的使用
 * @author liqa
 */
public interface ICPointer {
    /** @return 内部存储的 c 指针值 */
    @ApiStatus.Internal long ptr_();
    /** @return 内部存储的 c 指针是否是空的 */
    default boolean isNull() {return ptr_()==0 || ptr_()==-1;}
}
