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
    public static RowMatrix ones(int aNumRows, int aNumCols) {return RowMatrix.ones(aNumRows, aNumCols);}
    public static RowMatrix zeros(int aSize) {return RowMatrix.zeros(aSize);}
    public static RowMatrix zeros(int aNumRows, int aNumCols) {return RowMatrix.zeros(aNumRows, aNumCols);}
    public static RowMatrix NaN(int aSize) {return NaN(aSize, aSize);}
    public static RowMatrix NaN(int aNumRows, int aNumCols) {
        RowMatrix rMatrix = zeros(aNumRows, aNumCols);
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
        final int tNumRows = tDims[0], tNumCols = tDims[1];
        Object tData = aNDArray.getData();
        if (tData instanceof double[]) {
            return new RowMatrix(tNumRows, tNumCols, (double[])tData);
        } else
        if (tData instanceof int[]) {
            return new RowIntMatrix(tNumRows, tNumCols, (int[])tData);
        } else
        if (tData instanceof boolean[]) {
            return new RowLogicalMatrix(tNumRows, tNumCols, (boolean[])tData);
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
    public static RowMatrix from(int aNumRows, int aNumCols, IMatrixGetter aMatrixGetter) {return fromDouble(aNumRows, aNumCols, aMatrixGetter);}
    public static RowMatrix from(IMatrix aMatrix) {return fromDouble(aMatrix);}
    public static RowMatrix fromDouble(int aSize, IMatrixGetter aMatrixGetter) {return fromDouble(aSize, aSize, aMatrixGetter);}
    public static RowMatrix fromDouble(int aNumRows, int aNumCols, IMatrixGetter aMatrixGetter) {
        RowMatrix rMatrix = zeros(aNumRows, aNumCols);
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
    public static RowMatrix from(int aNumRows, int aNumCols, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {
        return fromDouble(aNumRows, aNumCols, aGroovyTask);
    }
    public static RowMatrix fromDouble(int aSize, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {
        return fromDouble(aSize, (i, j) -> UT.Code.doubleValue(aGroovyTask.call(i, j)));
    }
    public static RowMatrix fromDouble(int aNumRows, int aNumCols, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<? extends Number> aGroovyTask) {
        return fromDouble(aNumRows, aNumCols, (i, j) ->UT.Code.doubleValue( aGroovyTask.call(i, j)));
    }
    
    public static RowMatrix from(Collection<?> aRows) {return fromDouble(aRows);}
    public static RowMatrix fromRows(Collection<?> aRows) {return fromRowsDouble(aRows);}
    public static RowMatrix fromCols(Collection<?> aCols) {return fromColsDouble(aCols);}
    public static RowMatrix fromDouble(Collection<?> aRows) {return fromRowsDouble(aRows);}
    public static RowMatrix fromRowsDouble(Collection<?> aRows) {
        int tNumCols;
        if (aRows.isEmpty()) {
            tNumCols = 0;
        } else {
            Object tFirst = UT.Code.first(aRows);
            if (tFirst instanceof Collection) {
                tNumCols = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof IVector) {
                tNumCols = ((IVector)tFirst).size();
            } else if (tFirst instanceof double[]) {
                tNumCols = ((double[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Row Must be Collection<? extends Number>, IVector or double[]");
            }
        }
        RowMatrix rMatrix = zeros(aRows.size(), tNumCols);
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    public static RowMatrix fromColsDouble(Collection<?> aCols) {
        int tNumRows;
        if (aCols.isEmpty()) {
            tNumRows = 0;
        } else {
            Object tFirst = UT.Code.first(aCols);
            if (tFirst instanceof Collection) {
                tNumRows = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof IVector) {
                tNumRows = ((IVector)tFirst).size();
            } else if (tFirst instanceof double[]) {
                tNumRows = ((double[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Column Must be Collection<? extends Number>, IVector or double[]");
            }
        }
        RowMatrix rMatrix = zeros(tNumRows, aCols.size());
        rMatrix.fillWithCols(aCols);
        return rMatrix;
    }
    
    
    /// LogicalMatrix stuffs
    public static RowLogicalMatrix fromBoolean(int aSize, ILogicalMatrixGetter aMatrixGetter) {return fromBoolean(aSize, aSize, aMatrixGetter);}
    public static RowLogicalMatrix fromBoolean(int aNumRows, int aNumCols, ILogicalMatrixGetter aMatrixGetter) {
        RowLogicalMatrix rMatrix = RowLogicalMatrix.zeros(aNumRows, aNumCols);
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
    public static RowLogicalMatrix fromBoolean(int aNumRows, int aNumCols, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<Boolean> aGroovyTask) {
        return fromBoolean(aNumRows, aNumCols, aGroovyTask::call);
    }
    
    public static RowLogicalMatrix fromBoolean(Collection<?> aRows) {return fromRowsBoolean(aRows);}
    public static RowLogicalMatrix fromRowsBoolean(Collection<?> aRows) {
        int tNumCols;
        if (aRows.isEmpty()) {
            tNumCols = 0;
        } else {
            Object tFirst = UT.Code.first(aRows);
            if (tFirst instanceof Collection) {
                tNumCols = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof ILogicalVector) {
                tNumCols = ((ILogicalVector)tFirst).size();
            } else if (tFirst instanceof boolean[]) {
                tNumCols = ((boolean[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Row Must be Collection<Boolean>, ILogicalVector or boolean[]");
            }
        }
        RowLogicalMatrix rMatrix = RowLogicalMatrix.zeros(aRows.size(), tNumCols);
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    public static RowLogicalMatrix fromColsBoolean(Collection<?> aCols) {
        int tNumRows;
        if (aCols.isEmpty()) {
            tNumRows = 0;
        } else {
            Object tFirst = UT.Code.first(aCols);
            if (tFirst instanceof Collection) {
                tNumRows = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof ILogicalVector) {
                tNumRows = ((ILogicalVector)tFirst).size();
            } else if (tFirst instanceof boolean[]) {
                tNumRows = ((boolean[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Column Must be Collection<Boolean>, ILogicalVector or boolean[]");
            }
        }
        RowLogicalMatrix rMatrix = RowLogicalMatrix.zeros(tNumRows, aCols.size());
        rMatrix.fillWithCols(aCols);
        return rMatrix;
    }
    
    /// IntMatrix stuffs
    public static RowIntMatrix fromInt(int aSize, IIntMatrixGetter aMatrixGetter) {return fromInt(aSize, aSize, aMatrixGetter);}
    public static RowIntMatrix fromInt(int aNumRows, int aNumCols, IIntMatrixGetter aMatrixGetter) {
        RowIntMatrix rMatrix = RowIntMatrix.zeros(aNumRows, aNumCols);
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
    public static RowIntMatrix fromInt(int aNumRows, int aNumCols, @ClosureParams(value=FromString.class, options={"int,int"}) final Closure<Integer> aGroovyTask) {
        return fromInt(aNumRows, aNumCols, aGroovyTask::call);
    }
    
    public static RowIntMatrix fromInt(Collection<?> aRows) {return fromRowsInt(aRows);}
    public static RowIntMatrix fromRowsInt(Collection<?> aRows) {
        int tNumCols;
        if (aRows.isEmpty()) {
            tNumCols = 0;
        } else {
            Object tFirst = UT.Code.first(aRows);
            if (tFirst instanceof Collection) {
                tNumCols = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof IIntVector) {
                tNumCols = ((IIntVector)tFirst).size();
            } else if (tFirst instanceof int[]) {
                tNumCols = ((int[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Row Must be Collection<Integer>, IIntVector or int[]");
            }
        }
        RowIntMatrix rMatrix = RowIntMatrix.zeros(aRows.size(), tNumCols);
        rMatrix.fillWithRows(aRows);
        return rMatrix;
    }
    public static RowIntMatrix fromColsInt(Collection<?> aCols) {
        int tNumRows;
        if (aCols.isEmpty()) {
            tNumRows = 0;
        } else {
            Object tFirst = UT.Code.first(aCols);
            if (tFirst instanceof Collection) {
                tNumRows = ((Collection<?>)tFirst).size();
            } else if (tFirst instanceof IIntVector) {
                tNumRows = ((IIntVector)tFirst).size();
            } else if (tFirst instanceof int[]) {
                tNumRows = ((int[])tFirst).length;
            } else {
                throw new IllegalArgumentException("Type of Column Must be Collection<Integer>, IIntVector or int[]");
            }
        }
        RowIntMatrix rMatrix = RowIntMatrix.zeros(tNumRows, aCols.size());
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
