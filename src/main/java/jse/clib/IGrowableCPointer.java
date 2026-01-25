package jse.clib;

import org.jetbrains.annotations.ApiStatus;

/**
 * 可增长的指针通用接口
 * @author liqa
 */
@ApiStatus.Experimental
public interface IGrowableCPointer extends ICPointer {
    int count();
    void ensureCapacity(int aMinCount);
}
