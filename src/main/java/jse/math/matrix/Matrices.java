package jse.math.matrix;

import jse.code.UT;
import jse.math.vector.IVector;
import jse.math.vector.IVectorGetter;
import groovy.lang.Closure;

import java.util.Collection;

/**
 * @author liqa
 * <p> 获取矩阵的类，默认获取 {@link ColumnMatrix} </p>
 */
public class Matrices {
    private Matrices() {}
    
    public static ColumnMatrix ones(int aSize) {return ColumnMatrix.ones(aSize);}
    public static ColumnMatrix ones(int aRowNum, int aColNum) {return ColumnMatrix.ones(aRowNum, aColNum);}
    public static ColumnMatrix zeros(int aSize) {return ColumnMatrix.zeros(aSize);}
    public static ColumnMatrix zeros(int aRowNum, int aColNum) {return ColumnMatrix.zeros(aRowNum, aColNum);}
    public static ColumnMatrix NaN(int aSize) {return NaN(aSize, aSize);}
    public static ColumnMatrix NaN(int aRowNum, int aColNum) {
        ColumnMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fill(Double.NaN);
        return rMatrix;
    }
    
    public static ColumnMatrix from(int aSize, IMatrixGetter aMatrixGetter) {return from(aSize, aSize, aMatrixGetter);}
    public static ColumnMatrix from(int aRowNum, int aColNum, IMatrixGetter aMatrixGetter) {
        ColumnMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fill(aMatrixGetter);
        return rMatrix;
    }
    public static ColumnMatrix from(IMatrix aMatrix) {
        ColumnMatrix rMatrix = zeros(aMatrix.rowNumber(), aMatrix.columnNumber());
        rMatrix.fill(aMatrix);
        return rMatrix;
    }
    /** Groovy stuff */
    public static ColumnMatrix from(int aSize, final Closure<? extends Number> aGroovyTask) {return from(aSize, (i, j) -> aGroovyTask.call(i, j).doubleValue());}
    public static ColumnMatrix from(int aRowNum, int aColNum, final Closure<? extends Number> aGroovyTask) {return from(aRowNum, aColNum, (i, j) -> aGroovyTask.call(i, j).doubleValue());}
    
    /** Matrix 暂时没有相关 Builder，因此不支持 Iterable 构造 */
    public static ColumnMatrix from(Collection<?> aRows) {return fromRows(aRows);}
    public static ColumnMatrix fromRows(Collection<?> aRows) {
        int tColNum;
        Object tFirst = UT.Code.first(aRows);
        if (tFirst instanceof Collection) {
            tColNum = ((Collection<?>)tFirst).size();
        } else if (tFirst instanceof IVector) {
            tColNum = ((IVector)tFirst).size();
        } else {
            throw new IllegalArgumentException("Type of Row Must be Collection<? extends Number> or IVector");
        }
        ColumnMatrix rMatrix = zeros(aRows.size(), tColNum);
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    public static ColumnMatrix fromCols(Collection<?> aCols) {
        int tRowNum;
        Object tFirst = UT.Code.first(aCols);
        if (tFirst instanceof Collection) {
            tRowNum = ((Collection<?>)tFirst).size();
        } else if (tFirst instanceof IVector) {
            tRowNum = ((IVector)tFirst).size();
        } else {
            throw new IllegalArgumentException("Type of Column Must be Collection<? extends Number> or IVector");
        }
        ColumnMatrix rMatrix = zeros(tRowNum, aCols.size());
        rMatrix.fillWithCols(aCols);
        return rMatrix;
    }
    
    /** Matrix 特有的构造 */
    public static ColumnMatrix diag(IVector aVector) {return diag(aVector.size(), aVector);}
    public static ColumnMatrix diag(int aSize, IVectorGetter aVector) {
        ColumnMatrix rMatrix = zeros(aSize, aSize);
        rMatrix.refSlicer().diag().fill(aVector);
        return rMatrix;
    }
    public static ColumnMatrix diag(Collection<? extends Number> aList) {return diag(aList.size(), aList);}
    public static ColumnMatrix diag(int aSize, Iterable<? extends Number> aList) {
        ColumnMatrix rMatrix = zeros(aSize, aSize);
        rMatrix.refSlicer().diag().fill(aList);
        return rMatrix;
    }
    public static ColumnMatrix diag(double... aDiags) {
        ColumnMatrix rMatrix = zeros(aDiags.length, aDiags.length);
        rMatrix.refSlicer().diag().fill(aDiags);
        return rMatrix;
    }
    /** Groovy stuff */
    public static ColumnMatrix diag(int aSize, final Closure<? extends Number> aGroovyTask) {return diag(aSize, i -> aGroovyTask.call(i).doubleValue());}
}
