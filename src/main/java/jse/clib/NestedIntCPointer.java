package jse.clib;

import jse.code.collection.AbstractRandomAccessList;
import jse.math.matrix.*;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 C 中的 {@code int **} 处理的指针，
 * 用于优化对于 {@code int **} 的访问
 */
public class NestedIntCPointer extends NestedCPointer {
    @ApiStatus.Internal public NestedIntCPointer(long aPtr) {super(aPtr);}
    
    public static NestedIntCPointer malloc(int aCount) {
        return new NestedIntCPointer(malloc_(aCount * typeSize()));
    }
    public static NestedIntCPointer calloc(int aCount) {
        return new NestedIntCPointer(calloc_(aCount * typeSize()));
    }
    
    public void fill(int[] aData, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart + aRowNum*aColNum);
        fill_(mPtr, aData, aStart, aRowNum, aColNum);
    }
    public void fill(int[] aData, int aRowNum, int aColNum) {fill(aData, 0, aRowNum, aColNum);}
    public void fill(int[] aData, int aColNum) {fill(aData, 0, aData.length/aColNum, aColNum);}
    private native static void fill_(long rPtr, int[] aData, int aStart, int aRowNum, int aColNum);
    
    public void parse2dest(int[] rDest, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart + aRowNum*aColNum);
        parse2dest_(mPtr, rDest, aStart, aRowNum, aColNum);
    }
    public void parse2dest(int[] rDest, int aRowNum, int aColNum) {parse2dest(rDest, 0, aRowNum, aColNum);}
    public void parse2dest(int[] rDest, int aColNum) {parse2dest(rDest, 0, rDest.length/aColNum, aColNum);}
    private native static void parse2dest_(long aPtr, int[] rDest, int aStart, int aRowNum, int aColNum);
    
    
    @Override public IntCPointer get() {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(get_(mPtr));
    }
    @Override public IntCPointer getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new IntCPointer(getAt_(mPtr, aIdx));
    }
    public int getAt(int aRow, int aCol) {
        if (isNull()) throw new NullPointerException();
        return getAt_(mPtr, aRow, aCol);
    }
    private native static int getAt_(long aPtr, int aRow, int aCol);
    
    public void putAt(int aRow, int aCol, int aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aRow, aCol, aValue);
    }
    native static void putAt_(long aPtr, int aRow, int aCol, int aValue);
    
    @Override public NestedIntCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedIntCPointer(rightShift_(mPtr, aCount));
    }
    @Override public NestedIntCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedIntCPointer(leftShift_(mPtr, aCount));
    }
    
    @Override public NestedIntCPointer copy() {
        return new NestedIntCPointer(mPtr);
    }
    
    public IIntMatrix asMat(final int aRowNum, final int aColNum) {
        return new RefIntMatrix() {
            @Override public int get(int aRow, int aCol) {
                AbstractMatrix.rangeCheckRow(aRow, aRowNum);
                AbstractMatrix.rangeCheckCol(aCol, aColNum);
                return NestedIntCPointer.this.getAt(aRow, aCol);
            }
            @Override public void set(int aRow, int aCol, int aValue) {
                AbstractMatrix.rangeCheckRow(aRow, aRowNum);
                AbstractMatrix.rangeCheckCol(aCol, aColNum);
                NestedIntCPointer.this.putAt(aRow, aCol, aValue);
            }
            @Override public int rowNumber() {return aRowNum;}
            @Override public int columnNumber() {return aColNum;}
        };
    }
    @Override public List<IntCPointer> asList(final int aSize) {
        return new AbstractRandomAccessList<IntCPointer>() {
            @Override public IntCPointer get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public IntCPointer set(int index, @NotNull IntCPointer element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                IntCPointer oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
