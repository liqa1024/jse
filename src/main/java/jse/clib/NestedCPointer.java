package jse.clib;

import org.jetbrains.annotations.ApiStatus;

/**
 * 当作 C 中的 {@code void **} 处理的指针，
 * 用于处理一般的 C 数组
 */
public class NestedCPointer extends CPointer {
    @ApiStatus.Internal public NestedCPointer(long aPtr) {super(aPtr);}
    
    public static NestedCPointer malloc(int aCount) {
        return new NestedCPointer(malloc_(aCount * typeSize()));
    }
    public static NestedCPointer calloc(int aCount) {
        return new NestedCPointer(calloc_(aCount * typeSize()));
    }
    public native static int typeSize();
    
    
    public CPointer get() {
        if (isNull()) throw new NullPointerException();
        return new CPointer(get_(mPtr));
    }
    private native static long get_(long aPtr);
    
    public CPointer getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new CPointer(getAt_(mPtr, aIdx));
    }
    private native static long getAt_(long aPtr, int aIdx);
    
    public void set(CPointer aValue) {
        if (isNull()) throw new NullPointerException();
        set_(mPtr, aValue.mPtr);
    }
    private native static void set_(long aPtr, long aValue);
    
    public void putAt(int aIdx, CPointer aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aIdx, aValue.mPtr);
    }
    private native static void putAt_(long aPtr, int aIdx, long aValue);
    
    
    public void next() {
        if (isNull()) throw new NullPointerException();
        mPtr = next_(mPtr);
    }
    private native static long next_(long aPtr);
    
    public void rightShift(int aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = rightShift_(mPtr, aCount);
    }
    private native static long rightShift_(long aPtr, int aCount);
    public NestedCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedCPointer(rightShift_(mPtr, aCount));
    }
    
    public void previous() {
        if (isNull()) throw new NullPointerException();
        mPtr = previous_(mPtr);
    }
    private native static long previous_(long aPtr);
    
    public void leftShift(int aCount) {
        if (isNull()) throw new NullPointerException();
        mPtr = leftShift_(mPtr, aCount);
    }
    private native static long leftShift_(long aPtr, int aCount);
    public NestedCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedCPointer(leftShift_(mPtr, aCount));
    }
    
    @Override public NestedCPointer copy() {
        return new NestedCPointer(mPtr);
    }
}
