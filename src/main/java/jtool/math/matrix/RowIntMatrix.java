package jtool.math.matrix;

import jtool.math.vector.IntVector;
import jtool.math.vector.ShiftIntVector;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

import static jtool.math.matrix.AbstractMatrix.rangeCheckCol;
import static jtool.math.matrix.AbstractMatrix.rangeCheckRow;


/**
 * @author liqa
 * <p> 按照行排序的整数矩阵 </p>
 */
public final class RowIntMatrix extends IntArrayMatrix {
    /** 提供默认的创建 */
    public static RowIntMatrix ones(int aSize) {return ones(aSize, aSize);}
    public static RowIntMatrix ones(int aRowNum, int aColNum) {
        int[] tData = new int[aRowNum*aColNum];
        Arrays.fill(tData, 1);
        return new RowIntMatrix(aRowNum, aColNum, tData);
    }
    public static RowIntMatrix zeros(int aSize) {return zeros(aSize, aSize);}
    public static RowIntMatrix zeros(int aRowNum, int aColNum) {return new RowIntMatrix(aRowNum, aColNum, new int[aRowNum*aColNum]);}
    
    
    private final int mRowNum;
    private final int mColNum;
    
    public RowIntMatrix(int aRowNum, int aColNum, int[] aData) {
        super(aData);
        mRowNum = aRowNum;
        mColNum = aColNum;
    }
    public RowIntMatrix(int aColNum, int[] aData) {this(aData.length/aColNum, aColNum, aData);}
    
    
    /** IComplexMatrix stuffs */
    @Override public int get(int aRow, int aCol) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        return mData[aCol + aRow*mColNum];
    }
    @Override public void set(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        mData[aCol + aRow*mColNum] = aValue;
    }
    @Override public int getAndSet(int aRow, int aCol, int aValue) {
        rangeCheckRow(aRow, mRowNum);
        rangeCheckCol(aCol, mColNum);
        int tIdx = aCol + aRow*mColNum;
        int oValue = mData[tIdx];
        mData[tIdx] = aValue;
        return oValue;
    }
    @Override public int rowNumber() {return mRowNum;}
    @Override public int columnNumber() {return mColNum;}
    
    @Override public RowIntMatrix newShell() {return new RowIntMatrix(mRowNum, mColNum, null);}
    @Override public int @Nullable[] getIfHasSameOrderData(Object aObj) {
        // 只有同样是 RowMatrix 并且列数相同才会返回 mData
        if (aObj instanceof RowIntMatrix && ((RowIntMatrix)aObj).mColNum == mColNum) return ((RowIntMatrix)aObj).mData;
        return null;
    }
    
    /** Optimize stuffs，行向展开的向量直接返回 */
    @Override public IntVector asVecRow() {return new IntVector(mRowNum*mColNum, mData);}
    
    /** Optimize stuffs，重写这个提高行向的索引速度 */
    @Override public ShiftIntVector row(final int aRow) {
        rangeCheckRow(aRow, mRowNum);
        return new ShiftIntVector(mColNum, aRow*mColNum, mData);
    }
}
