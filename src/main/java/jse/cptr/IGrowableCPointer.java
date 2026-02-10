package jse.cptr;

/**
 * 可增长的指针通用接口
 * @author liqa
 */
public interface IGrowableCPointer extends ICPointer {
    long count();
    void ensureCapacity(long aMinCount);
}
