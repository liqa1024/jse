package com.jtool.math;

import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * @author liqa
 * <p> 避免值拷贝获取矩阵某一列的类，暂时不检测输入是否合理 </p>
 */
public class RealMatrixColumn extends AbstractList<Double> {
    private final double[][] mMatrix;
    private final int mCol;
    public RealMatrixColumn(double[][] aMatrix, int aColumn) {mMatrix = aMatrix;mCol = aColumn;}
    
    /// AbstractList stuffs
    @Override public int size() {return mMatrix.length;}
    @Override public Double get(int index) {return mMatrix[index][mCol];}
    @Override public Double set(int index, Double element) {return mMatrix[index][mCol] = element;}
    
    /// Deprecated stuffs, use getArray to get the double[]
    @Deprecated @NotNull @Override public Object @NotNull[] toArray() {return super.toArray();}
    @Deprecated @NotNull @Override public <T> T @NotNull[] toArray(@NotNull T @NotNull[] a) {return super.toArray(a);}
    
    /// Extended stuffs
    @Override public boolean isEmpty() {return mMatrix.length == 0 || mMatrix[0].length == 0;}
    
    public boolean contains(double aValue) {return indexOf(aValue) >= 0;}
    public int indexOf(double aValue) {
        for (int i = 0; i < mMatrix.length; ++i) if (aValue == mMatrix[i][mCol]) return i;
        return -1;
    }
    
    public double[] getArray() {
        double[] tOut = new double[mMatrix.length];
        for (int i = 0; i < mMatrix.length; ++i) tOut[i] = mMatrix[i][mCol];
        return tOut;
    }
    
}
