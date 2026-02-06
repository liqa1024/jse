package jse.cptr;

import org.jetbrains.annotations.ApiStatus;

/**
 * 任意指针的简单包装类
 * @author liqa
 */
@ApiStatus.Experimental
public final class Pointer implements IPointer {
    private final long mPtr;
    /**
     * 直接从一个任意的指针初始化一个 {@link Pointer} 对象
     * @param aPtr 需要包装的指针值
     */
    @ApiStatus.Internal public Pointer(long aPtr) {mPtr = aPtr;}
    /** @return {@inheritDoc} */
    @Override @ApiStatus.Internal public long ptr_() {return mPtr;}
}
