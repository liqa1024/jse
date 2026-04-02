package jse.math.matrix;

import groovy.transform.stc.ClosureParams;
import groovy.transform.stc.FromString;
import groovy.transform.stc.SimpleType;
import jep.NDArray;
import jse.code.UT;
import jse.math.vector.*;
import groovy.lang.Closure;

import java.util.Arrays;
import java.util.Collection;

/**
 * 方便创建矩阵的工具类，默认获取 {@link RowMatrix}
 * @author liqa
 */
public class Matrices {
    private Matrices() {}
    
    public static RowMatrix ones(int aSize) {return RowMatrix.ones(aSize);}
    public static RowMatrix ones(int aRowNum, int aColNum) {return RowMatrix.ones(aRowNum, aColNum);}
    public static RowMatrix zeros(int aSize) {return RowMatrix.zeros(aSize);}
    public static RowMatrix zeros(int aRowNum, int aColNum) {return RowMatrix.zeros(aRowNum, aColNum);}
    public static RowMatrix NaN(int aSize) {return NaN(aSize, aSize);}
    public static RowMatrix NaN(int aRowNum, int aColNum) {
        RowMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fill(Double.NaN);
        return rMatrix;
    }
    
    /**
     * 从 numpy 的 {@link NDArray} 创建矩阵，自动检测类型
     * <p>
     * 和下面 {@code from} 相关方法不同的是，这里在 java
     * 侧不会进行值拷贝，考虑到 {@link NDArray}
     * 实际总是会经过一次值拷贝，因此在 python
     * 中使用不会有引用的问题
     *
     * @param aNDArray 输入的 numpy 数组
     * @param aUnsignedWarning 是否开启检测无符号的警告，如果这不是问题则可以关闭，默认为 {@code true}
     * @return 从 {@link NDArray} 创建的矩阵
     * @throws UnsupportedOperationException 当输入的 {@link NDArray}
     * 类型还不存在对应的 jse 矩阵类型
     * @throws IllegalArgumentException 当输入的 {@link NDArray} 不是二维的
     */
    public static Object fromNumpy(NDArray<?> aNDArray, boolean aUnsignedWarning) {
        int[] tDims = aNDArray.getDimensions();
        if (tDims.length != 2) throw new IllegalArgumentException("Invalid numpy shape: " + Arrays.toString(tDims));
        if (aUnsignedWarning && aNDArray.isUnsigned()) {
            UT.Code.warning("Input numpy is unsigned, which is not supported in jse, so the original signed value will be directly obtained");
        }
        final int tRowNum = tDims[0], tColNum = tDims[1];
        Object tData = aNDArray.getData();
        if (tData instanceof double[]) {
            return new RowMatrix(tRowNum, tColNum, (double[])tData);
        } else
        if (tData instanceof int[]) {
            return new RowIntMatrix(tRowNum, tColNum, (int[])tData);
        } else
        if (tData instanceof boolean[]) {
            return new RowLogicalMatrix(tRowNum, tColNum, (boolean[])tData);
        } else {
            throw new UnsupportedOperationException("Invalid numpy dtype: " + tData.getClass().getName());
        }
    }
    /**
     * 从 numpy 的 {@link NDArray} 创建矩阵，自动检测类型
     * <p>
     * 和下面 {@code from} 相关方法不同的是，这里在 java
     * 侧不会进行值拷贝，考虑到 {@link NDArray}
     * 实际总是会经过一次值拷贝，因此在 python
     * 中使用不会有引用的问题
     *
     * @param aNDArray 输入的 numpy 数组
     * @return 从 {@link NDArray} 创建的矩阵
     * @throws UnsupportedOperationException 当输入的 {@link NDArray}
     * 类型还不存在对应的 jse 矩阵类型
     * @throws IllegalArgumentException 当输入的 {@link NDArray} 不是二维的
     */
    public static Object fromNumpy(NDArray<?> aNDArray) {return fromNumpy(aNDArray, true);}
    
    
    public static RowMatrix from(int aSize, IMatrixGetter aMatrixGetter) {return fromDouble(aSize, aMatrixGetter);}
    public static RowMatrix from(int aRowNum, int aColNum, IMatrixGetter aMatrixGetter) {return fromDouble(aRowNum, aColNum, aMatrixGetter);}
    public static RowMatrix from(IMatrix aMatrix) {return fromDouble(aMatrix);}
    public static RowMatrix fromDouble(int aSize, IMatrixGetter aMatrixGetter) {return fromDouble(aSize, aSize, aMatrixGetter);}
    public static RowMatrix fromDouble(int aRowNum, int aColNum, IMatrixGetter aMatrixGetter) {
        RowMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fill(aMatrixGetter);
        return rMatrix;
    }
    public static RowMatrix fromDouble(IMatrix aMatrix) {
        RowMatrix rMatrix = zeros(aMatrix.nrows(), aMatrix.ncols());
        rMatrix.fill(aMatrix);
        return rMatrix;
    }
    /// Groovy stuff
    public static RowMatrix from(int aSize, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {
        return fromDouble(aSize, aGroovyTask);
    }
    public static RowMatrix from(int aRowNum, int aColNum, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {
        return fromDouble(aRowNum, aGroovyTask);
    }
    public static RowMatrix fromDouble(int aSize, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {
        return fromDouble(aSize, (i, j) -> UT.Code.doubleValue(aGroovyTask.call(i, j)));
    }
    public static RowMatrix fromDouble(int aRowNum, int aColNum, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {
        return fromDouble(aRowNum, aColNum, (i, j) ->UT.Code.doubleValue( aGroovyTask.call(i, j)));
    }
    
    public static RowMatrix from(Collection<?> aRows) {return fromDouble(aRows);}
    public static RowMatrix fromRows(Collection<?> aRows) {return fromRowsDouble(aRows);}
    public static RowMatrix fromCols(Collection<?> aCols) {return fromColsDouble(aCols);}
    public static RowMatrix fromDouble(Collection<?> aRows) {return fromRowsDouble(aRows);}
    public static RowMatrix fromRowsDouble(Collection<?> aRows) {
        int tColNum;
        if (aRows.isEmpty()) {
            tColNum = 0;
        } else {
            Object tFirst = UT.Code.first(aRows);
            if (tFirst instanceof Collection) {
                tColNum = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof IVector) {
                tColNum = ((IVector)tFirst).size();
            } else if (tFirst instanceof double[]) {
                tColNum = ((double[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Row Must be Collection<? extends Number>, IVector or double[]");
            }
        }
        RowMatrix rMatrix = zeros(aRows.size(), tColNum);
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    public static RowMatrix fromColsDouble(Collection<?> aCols) {
        int tRowNum;
        if (aCols.isEmpty()) {
            tRowNum = 0;
        } else {
            Object tFirst = UT.Code.first(aCols);
            if (tFirst instanceof Collection) {
                tRowNum = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof IVector) {
                tRowNum = ((IVector)tFirst).size();
            } else if (tFirst instanceof double[]) {
                tRowNum = ((double[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Column Must be Collection<? extends Number>, IVector or double[]");
            }
        }
        RowMatrix rMatrix = zeros(tRowNum, aCols.size());
        rMatrix.fillWithCols(aCols);
        return rMatrix;
    }
    
    
    /// LogicalMatrix stuffs
    public static RowLogicalMatrix fromBoolean(int aSize, ILogicalMatrixGetter aMatrixGetter) {return fromBoolean(aSize, aSize, aMatrixGetter);}
    public static RowLogicalMatrix fromBoolean(int aRowNum, int aColNum, ILogicalMatrixGetter aMatrixGetter) {
        RowLogicalMatrix rMatrix = RowLogicalMatrix.zeros(aRowNum, aColNum);
        rMatrix.fill(aMatrixGetter);
        return rMatrix;
    }
    public static RowLogicalMatrix fromBoolean(ILogicalMatrix aMatrix) {
        RowLogicalMatrix rMatrix = RowLogicalMatrix.zeros(aMatrix.nrows(), aMatrix.ncols());
        rMatrix.fill(aMatrix);
        return rMatrix;
    }
    /// Groovy stuff
    public static RowLogicalMatrix fromBoolean(int aSize, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<Boolean> aGroovyTask) {
        return fromBoolean(aSize, aGroovyTask::call);
    }
    public static RowLogicalMatrix fromBoolean(int aRowNum, int aColNum, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<Boolean> aGroovyTask) {
        return fromBoolean(aRowNum, aColNum, aGroovyTask::call);
    }
    
    public static RowLogicalMatrix fromBoolean(Collection<?> aRows) {return fromRowsBoolean(aRows);}
    public static RowLogicalMatrix fromRowsBoolean(Collection<?> aRows) {
        int tColNum;
        if (aRows.isEmpty()) {
            tColNum = 0;
        } else {
            Object tFirst = UT.Code.first(aRows);
            if (tFirst instanceof Collection) {
                tColNum = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof ILogicalVector) {
                tColNum = ((ILogicalVector)tFirst).size();
            } else if (tFirst instanceof boolean[]) {
                tColNum = ((boolean[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Row Must be Collection<Boolean>, ILogicalVector or boolean[]");
            }
        }
        RowLogicalMatrix rMatrix = RowLogicalMatrix.zeros(aRows.size(), tColNum);
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    public static RowLogicalMatrix fromColsBoolean(Collection<?> aCols) {
        int tRowNum;
        if (aCols.isEmpty()) {
            tRowNum = 0;
        } else {
            Object tFirst = UT.Code.first(aCols);
            if (tFirst instanceof Collection) {
                tRowNum = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof ILogicalVector) {
                tRowNum = ((ILogicalVector)tFirst).size();
            } else if (tFirst instanceof boolean[]) {
                tRowNum = ((boolean[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Column Must be Collection<Boolean>, ILogicalVector or boolean[]");
            }
        }
        RowLogicalMatrix rMatrix = RowLogicalMatrix.zeros(tRowNum, aCols.size());
        rMatrix.fillWithCols(aCols);
        return rMatrix;
    }
    
    /// IntMatrix stuffs
    public static RowIntMatrix fromInt(int aSize, IIntMatrixGetter aMatrixGetter) {return fromInt(aSize, aSize, aMatrixGetter);}
    public static RowIntMatrix fromInt(int aRowNum, int aColNum, IIntMatrixGetter aMatrixGetter) {
        RowIntMatrix rMatrix = RowIntMatrix.zeros(aRowNum, aColNum);
        rMatrix.fill(aMatrixGetter);
        return rMatrix;
    }
    public static RowIntMatrix fromInt(IIntMatrix aMatrix) {
        RowIntMatrix rMatrix = RowIntMatrix.zeros(aMatrix.nrows(), aMatrix.ncols());
        rMatrix.fill(aMatrix);
        return rMatrix;
    }
    /// Groovy stuff
    public static RowIntMatrix fromInt(int aSize, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<Integer> aGroovyTask) {
        return fromInt(aSize, aGroovyTask::call);
    }
    public static RowIntMatrix fromInt(int aRowNum, int aColNum, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<Integer> aGroovyTask) {
        return fromInt(aRowNum, aColNum, aGroovyTask::call);
    }
    
    public static RowIntMatrix fromInt(Collection<?> aRows) {return fromRowsInt(aRows);}
    public static RowIntMatrix fromRowsInt(Collection<?> aRows) {
        int tColNum;
        if (aRows.isEmpty()) {
            tColNum = 0;
        } else {
            Object tFirst = UT.Code.first(aRows);
            if (tFirst instanceof Collection) {
                tColNum = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof IIntVector) {
                tColNum = ((IIntVector)tFirst).size();
            } else if (tFirst instanceof int[]) {
                tColNum = ((int[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Row Must be Collection<Integer>, IIntVector or int[]");
            }
        }
        RowIntMatrix rMatrix = RowIntMatrix.zeros(aRows.size(), tColNum);
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    public static RowIntMatrix fromColsInt(Collection<?> aCols) {
        int tRowNum;
        if (aCols.isEmpty()) {
            tRowNum = 0;
        } else {
            Object tFirst = UT.Code.first(aCols);
            if (tFirst instanceof Collection) {
                tRowNum = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof IIntVector) {
                tRowNum = ((IIntVector)tFirst).size();
            } else if (tFirst instanceof int[]) {
                tRowNum = ((int[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Column Must be Collection<Integer>, IIntVector or int[]");
            }
        }
        RowIntMatrix rMatrix = RowIntMatrix.zeros(tRowNum, aCols.size());
        rMatrix.fillWithCols(aCols);
        return rMatrix;
    }
    
    /** Matrix 特有的构造 */
    public static RowMatrix diag(IVector aVector) {return diag(aVector.size(), aVector);}
    public static RowMatrix diag(int aSize, IVectorGetter aVector) {
        RowMatrix rMatrix = zeros(aSize, aSize);
        rMatrix.refSlicer().diag().fill(aVector);
        return rMatrix;
    }
    public static RowMatrix diag(Collection<? extends Number> aList) {return diag(aList.size(), aList);}
    public static RowMatrix diag(int aSize, Iterable<? extends Number> aList) {
        RowMatrix rMatrix = zeros(aSize, aSize);
        rMatrix.refSlicer().diag().fill(aList);
        return rMatrix;
    }
    public static RowMatrix diag(double... aDiags) {
        RowMatrix rMatrix = zeros(aDiags.length, aDiags.length);
        rMatrix.refSlicer().diag().fill(aDiags);
        return rMatrix;
    }
    /** Groovy stuff */
    public static RowMatrix diag(int aSize, @ClosureParams(value=SimpleType.class, options="int") final Closure<? extends Number> aGroovyTask) {
        return diag(aSize, i -> UT.Code.doubleValue(aGroovyTask.call(i)));
    }
}
