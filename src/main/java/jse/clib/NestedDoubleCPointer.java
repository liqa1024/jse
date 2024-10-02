package jse.clib;

import jse.code.collection.AbstractRandomAccessList;
import jse.math.matrix.AbstractMatrix;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RefMatrix;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * 当作 C 中的 {@code double **} 处理的指针，
 * 用于优化对于 {@code double **} 的访问
 */
public class NestedDoubleCPointer extends NestedCPointer {
    @ApiStatus.Internal public NestedDoubleCPointer(long aPtr) {super(aPtr);}
    
    public static NestedDoubleCPointer malloc(int aCount) {
        return new NestedDoubleCPointer(malloc_(aCount * typeSize()));
    }
    public static NestedDoubleCPointer calloc(int aCount) {
        return new NestedDoubleCPointer(calloc_(aCount * typeSize()));
    }
    
    public void fill(double[] aData, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(aData.length, aStart + aRowNum*aColNum);
        fill0(mPtr, aData, aStart, aRowNum, aColNum);
    }
    public void fill(double[] aData, int aRowNum, int aColNum) {fill(aData, 0, aRowNum, aColNum);}
    public void fill(double[] aData, int aColNum) {fill(aData, 0, aData.length/aColNum, aColNum);}
    private native static void fill0(long rPtr, double[] aData, int aStart, int aRowNum, int aColNum);
    public void fill(double aValue, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        fill1(mPtr, aValue, aRowNum, aColNum);
    }
    private native static void fill1(long rPtr, double aValue, int aRowNum, int aColNum);
    
    public void parse2dest(double[] rDest, int aStart, int aRowNum, int aColNum) {
        if (isNull()) throw new NullPointerException();
        rangeCheck(rDest.length, aStart + aRowNum*aColNum);
        parse2dest_(mPtr, rDest, aStart, aRowNum, aColNum);
    }
    public void parse2dest(double[] rDest, int aRowNum, int aColNum) {parse2dest(rDest, 0, aRowNum, aColNum);}
    public void parse2dest(double[] rDest, int aColNum) {parse2dest(rDest, 0, rDest.length/aColNum, aColNum);}
    private native static void parse2dest_(long aPtr, double[] rDest, int aStart, int aRowNum, int aColNum);
    
    
    @Override public DoubleCPointer get() {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(get_(mPtr));
    }
    @Override public DoubleCPointer getAt(int aIdx) {
        if (isNull()) throw new NullPointerException();
        return new DoubleCPointer(getAt_(mPtr, aIdx));
    }
    public double getAt(int aRow, int aCol) {
        if (isNull()) throw new NullPointerException();
        return getAt_(mPtr, aRow, aCol);
    }
    private native static double getAt_(long aPtr, int aRow, int aCol);
    
    public void putAt(int aRow, int aCol, double aValue) {
        if (isNull()) throw new NullPointerException();
        putAt_(mPtr, aRow, aCol, aValue);
    }
    native static void putAt_(long aPtr, int aRow, int aCol, double aValue);
    
    @Override public NestedDoubleCPointer plus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedDoubleCPointer(rightShift_(mPtr, aCount));
    }
    @Override public NestedDoubleCPointer minus(int aCount) {
        if (isNull()) throw new NullPointerException();
        return new NestedDoubleCPointer(leftShift_(mPtr, aCount));
    }
    
    @Override public NestedDoubleCPointer copy() {
        return new NestedDoubleCPointer(mPtr);
    }
    
    public IMatrix asMat(final int aRowNum, final int aColNum) {
        return new RefMatrix() {
            @Override public double get(int aRow, int aCol) {
                AbstractMatrix.rangeCheckRow(aRow, aRowNum);
                AbstractMatrix.rangeCheckCol(aCol, aColNum);
                return NestedDoubleCPointer.this.getAt(aRow, aCol);
            }
            @Override public void set(int aRow, int aCol, double aValue) {
                AbstractMatrix.rangeCheckRow(aRow, aRowNum);
                AbstractMatrix.rangeCheckCol(aCol, aColNum);
                NestedDoubleCPointer.this.putAt(aRow, aCol, aValue);
            }
            @Override public int rowNumber() {return aRowNum;}
            @Override public int columnNumber() {return aColNum;}
        };
    }
    @Override public List<DoubleCPointer> asList(final int aSize) {
        return new AbstractRandomAccessList<DoubleCPointer>() {
            @Override public DoubleCPointer get(int index) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                return getAt(index);
            }
            @Override public DoubleCPointer set(int index, @NotNull DoubleCPointer element) {
                if (index >= aSize) throw new IndexOutOfBoundsException("Index: "+index+", Size: "+aSize);
                DoubleCPointer oValue = getAt(index);
                putAt(index, element);
                return oValue;
            }
            @Override public int size() {return aSize;}
        };
    }
}
