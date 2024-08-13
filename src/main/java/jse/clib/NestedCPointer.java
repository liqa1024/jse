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
    
    
    public CPointer get() {return new CPointer(get_(mPtr));}
    private native static long get_(long aPtr);
    
    public CPointer getAt(int aIdx) {return new CPointer(getAt_(mPtr, aIdx));}
    private native static long getAt_(long aPtr, int aIdx);
    
    public void set(CPointer aValue) {set_(mPtr, aValue.mPtr);}
    private native static void set_(long aPtr, long aValue);
    
    public void putAt(int aIdx, CPointer aValue) {putAt_(mPtr, aIdx, aValue.mPtr);}
    private native static void putAt_(long aPtr, int aIdx, long aValue);
    
    
    public void next() {mPtr = next_(mPtr);}
    private native static long next_(long aPtr);
    
    public void rightShift(int aCount) {mPtr = rightShift_(mPtr, aCount);}
    private native static long rightShift_(long aPtr, int aCount);
    public NestedCPointer plus(int aCount) {return new NestedCPointer(rightShift_(mPtr, aCount));}
    
    public void previous() {mPtr = previous_(mPtr);}
    private native static long previous_(long aPtr);
    
    public void leftShift(int aCount) {mPtr = leftShift_(mPtr, aCount);}
    private native static long leftShift_(long aPtr, int aCount);
    public NestedCPointer minus(int aCount) {return new NestedCPointer(leftShift_(mPtr, aCount));}
    
    @Override public NestedCPointer copy() {
        return new NestedCPointer(mPtr);
    }
}
