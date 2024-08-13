package jse.clib;

import org.jetbrains.annotations.ApiStatus;

/**
 * 当作 C 中的 {@code double *} 处理的指针，
 * 虽然一般来说 C 中的 {@code double} 和 java 的 {@code jdouble}
 * 等价，但这里为了不失一般性两者都提供一套支持。
 */
public class DoubleCPointer extends CPointer {
    @ApiStatus.Internal public DoubleCPointer(long aPtr) {super(aPtr);}
    
    public static DoubleCPointer malloc(int aCount) {
        return new DoubleCPointer(malloc_(aCount * typeSize()));
    }
    public static DoubleCPointer calloc(int aCount) {
        return new DoubleCPointer(calloc_(aCount * typeSize()));
    }
    public native static int typeSize();
    
    public void parse2dest(double[] rDest, int aStart, int aCount) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart+aCount);
        parse2dest_(mPtr, rDest, aStart, aCount);
    }
    public void parse2dest(double[] rDest, int aCount) {parse2dest(rDest, 0, aCount);}
    public void parse2dest(double[] rDest) {parse2dest(rDest, 0, rDest.length);}
    private native static void parse2dest_(long aPtr, double[] rDest, int aStart, int aCount);
    
    
    public double get() {
        if (isNull()) throw new NullPointerException();
        return get_(mPtr);
    }
    private native static double get_(long aPtr);
    
    public double getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return getAt_(mPtr, aIdx);
    }
    private native static double getAt_(long aPtr, int aIdx);
    
    public void set(double aValue) {
        if (isNull()) throw new NullPointerException();
        set_(mPtr, aValue);
    }
    private native static void set_(long aPtr, double aValue);
    
    public void putAt(int aIdx, double aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aIdx, aValue);
    }
    private native static void putAt_(long aPtr, int aIdx, double aValue);
    
    
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
    public DoubleCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(rightShift_(mPtr, aCount));
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
    public DoubleCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(leftShift_(mPtr, aCount));
    }
    
    @Override public DoubleCPointer copy() {
        return new DoubleCPointer(mPtr);
    }
}
