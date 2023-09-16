package com.jtool.math.matrix;

import com.jtool.math.vector.IVector;
import com.jtool.math.vector.IVectorGetter;
import groovy.lang.Closure;

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
    public static IMatrix NaN(int aSize) {return NaN(aSize, aSize);}
    public static IMatrix NaN(int aRowNum, int aColNum) {
        IMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fill(Double.NaN);
        return rMatrix;
    }
    
    public static IMatrix from(int aSize, IMatrixGetter aMatrixGetter) {return from(aSize, aSize, aMatrixGetter);}
    public static IMatrix from(int aRowNum, int aColNum, IMatrixGetter aMatrixGetter) {
        IMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fill(aMatrixGetter);
        return rMatrix;
    }
    public static IMatrix from(IMatrix aMatrix) {
        if (aMatrix instanceof ColumnMatrix) {
            return aMatrix.copy();
        } else {
            IMatrix rMatrix = zeros(aMatrix.rowNumber(), aMatrix.columnNumber());
            rMatrix.fill(aMatrix);
            return rMatrix;
        }
    }
    /** Groovy stuff */
    public static IMatrix from(int aSize, final Closure<? extends Number> aGroovyTask) {return from(aSize, (i, j) -> aGroovyTask.call(i, j).doubleValue());}
    public static IMatrix from(int aRowNum, int aColNum, final Closure<? extends Number> aGroovyTask) {return from(aRowNum, aColNum, (i, j) -> aGroovyTask.call(i, j).doubleValue());}
    
    /** Matrix 暂时没有相关 Builder，因此不支持 Iterable 构造 */
    public static IMatrix from(Collection<? extends Collection<? extends Number>> aRows) {return fromRows(aRows);}
    public static IMatrix fromRows(Collection<? extends Collection<? extends Number>> aRows) {
        IMatrix rMatrix = zeros(aRows.size(), aRows.iterator().next().size());
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    public static IMatrix fromCols(Collection<? extends Collection<? extends Number>> aCols) {
        IMatrix rMatrix = zeros(aCols.iterator().next().size(), aCols.size());
        rMatrix.fillWithCols(aCols);
        return rMatrix;
    }
    
    /** Matrix 特有的构造 */
    public static IMatrix diag(IVector aVector) {return diag(aVector.size(), aVector);}
    public static IMatrix diag(int aSize, IVectorGetter aVector) {
        IMatrix rMatrix = zeros(aSize, aSize);
        rMatrix.refSlicer().diag().fill(aVector);
        return rMatrix;
    }
    public static IMatrix diag(Collection<? extends Number> aList) {return diag(aList.size(), aList);}
    public static IMatrix diag(int aSize, Iterable<? extends Number> aList) {
        IMatrix rMatrix = zeros(aSize, aSize);
        rMatrix.refSlicer().diag().fill(aList);
        return rMatrix;
    }
    public static IMatrix diag(double... aDiags) {
        IMatrix rMatrix = zeros(aDiags.length, aDiags.length);
        rMatrix.refSlicer().diag().fill(aDiags);
        return rMatrix;
    }
    /** Groovy stuff */
    public static IMatrix diag(int aSize, final Closure<? extends Number> aGroovyTask) {return diag(aSize, i -> aGroovyTask.call(i).doubleValue());}
}
