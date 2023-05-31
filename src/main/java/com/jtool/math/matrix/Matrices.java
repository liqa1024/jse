package com.jtool.math.matrix;

import java.util.Collection;

/**
 * @author liqa
 * <p> 获取矩阵的类，默认获取 {@link ColumnMatrix} </p>
 */
public class Matrices {
    private Matrices() {}
    
    public static IMatrix ones(int aSize) {return ColumnMatrix.ones(aSize);}
    public static IMatrix ones(int aRowNum, int aColNum) {return ColumnMatrix.ones(aRowNum, aColNum);}
    public static IMatrix zeros(int aSize) {return ColumnMatrix.zeros(aSize);}
    public static IMatrix zeros(int aRowNum, int aColNum) {return ColumnMatrix.zeros(aRowNum, aColNum);}
    
    
    public static IMatrix from(int aSize, IMatrixGetter aMatrixGetter) {return from(aSize, aSize, aMatrixGetter);}
    public static IMatrix from(int aRowNum, int aColNum, IMatrixGetter aMatrixGetter) {
        IMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fill(aMatrixGetter);
        return rMatrix;
    }
    public static IMatrix from(IMatrixAny<?, ?> aMatrix) {
        if (aMatrix instanceof ColumnMatrix) {
            return ((ColumnMatrix) aMatrix).generator().same();
        } else {
            IMatrix rMatrix = zeros(aMatrix.rowNumber(), aMatrix.columnNumber());
            rMatrix.fill(aMatrix);
            return rMatrix;
        }
    }
    
    public static IMatrix from(int aSize, Iterable<? extends Iterable<? extends Number>> aRows) {return fromRows(aSize, aRows);}
    public static IMatrix from(int aRowNum, int aColNum, Iterable<? extends Iterable<? extends Number>> aRows) {return fromRows(aRowNum, aColNum, aRows);}
    public static IMatrix from(Collection<? extends Collection<? extends Number>> aRows) {return fromRows(aRows);}
    
    public static IMatrix fromRows(int aSize, Iterable<? extends Iterable<? extends Number>> aRows) {return fromRows(aSize, aSize, aRows);}
    public static IMatrix fromRows(int aRowNum, int aColNum, Iterable<? extends Iterable<? extends Number>> aRows) {
        IMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    public static IMatrix fromRows(Collection<? extends Collection<? extends Number>> aRows) {
        int tRowNum = aRows.size();
        int tColNum = aRows.iterator().next().size();
        IMatrix rMatrix = zeros(tRowNum, tColNum);
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    
    public static IMatrix fromCols(int aSize, Iterable<? extends Iterable<? extends Number>> aCols) {return fromCols(aSize, aSize, aCols);}
    public static IMatrix fromCols(int aRowNum, int aColNum, Iterable<? extends Iterable<? extends Number>> aCols) {
        IMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fillWithCols(aCols);
        return rMatrix;
    }
    public static IMatrix fromCols(Collection<? extends Collection<? extends Number>> aCols) {
        int tRowNum = aCols.iterator().next().size();
        int tColNum = aCols.size();
        IMatrix rMatrix = zeros(tRowNum, tColNum);
        rMatrix.fillWithCols(aCols);
        return rMatrix;
    }
}
