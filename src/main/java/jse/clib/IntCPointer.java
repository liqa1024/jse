package jse.clib;

import jse.code.collection.AbstractRandomAccessList;
import jse.math.vector.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 C 中的 {@code int *} 处理的指针，
 * 注意 C 中的 {@code int} 并不一定总是 32 位的，
 * 但这里统一获取时转换为 java 的 {@code int}。
 * 即使超过了 32 位，实际程序中为了保持一般性，依旧也不会用到超出的部分。
 */
public class IntCPointer extends CPointer {
    @ApiStatus.Internal public IntCPointer(long aPtr) {super(aPtr);}
    
    public static IntCPointer malloc(int aCount) {
        return new IntCPointer(malloc_(aCount * typeSize()));
    }
    public static IntCPointer calloc(int aCount) {
        return new IntCPointer(calloc_(aCount * typeSize()));
    }
    public native static int typeSize();
    
    public void fill(int[] aData, int aStart, int aCount) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart+aCount);
        fill0(mPtr, aData, aStart, aCount);
    }
    public void fill(int[] aData, int aCount) {fill(aData, 0, aCount);}
    public void fill(int[] aData) {fill(aData, 0, aData.length);}
    private native static void fill0(long rPtr, int[] aData, int aStart, int aCount);
    public void fill(int aValue, int aCount) {
        if (isNull()) throw new NullPointerException();
        fill1(mPtr, aValue, aCount);
    }
    private native static void fill1(long rPtr, int aValue, int aCount);
    
    public void parse2dest(int[] rDest, int aStart, int aCount) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart+aCount);
        parse2dest_(mPtr, rDest, aStart, aCount);
    }
    public void parse2dest(int[] rDest, int aCount) {parse2dest(rDest, 0, aCount);}
    public void parse2dest(int[] rDest) {parse2dest(rDest, 0, rDest.length);}
    private native static void parse2dest_(long aPtr, int[] rDest, int aStart, int aCount);
    
    
    public int get() {
        if (isNull()) throw new NullPointerException();
        return get_(mPtr);
    }
    private native static int get_(long aPtr);
    
    public int getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return getAt_(mPtr, aIdx);
    }
    private native static int getAt_(long aPtr, int aIdx);
    
    public void set(int aValue) {
        if (isNull()) throw new NullPointerException();
        set_(mPtr, aValue);
    }
    private native static void set_(long aPtr, int aValue);
    
    public void putAt(int aIdx, int aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aIdx, aValue);
    }
    private native static void putAt_(long aPtr, int aIdx, int aValue);
    
    
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
    public IntCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(rightShift_(mPtr, aCount));
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
    public IntCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(leftShift_(mPtr, aCount));
    }
    
    @Override public IntCPointer copy() {
        return new IntCPointer(mPtr);
    }
    
    public IIntVector asVec(final int aSize) {
        return new RefIntVector() {
            @Override public int get(int aIdx) {AbstractVector.rangeCheck(aIdx, aSize); return IntCPointer.this.getAt(aIdx);}
            @Override public void set(int aIdx, int aValue) {AbstractVector.rangeCheck(aIdx, aSize); IntCPointer.this.putAt(aIdx, aValue);}
            @Override public int size() {return aSize;}
        };
    }
    public List<Integer> asList(final int aSize) {
        return new AbstractRandomAccessList<Integer>() {
            @Override public Integer get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public Integer set(int index, @NotNull Integer element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                int oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
