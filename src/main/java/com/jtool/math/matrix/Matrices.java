package com.jtool.math.matrix;

/**
 * @author liqa
 * <p> 获取矩阵的类，默认获取 {@link RealColumnMatrix} </p>
 */
public class Matrices {
    public static RealColumnMatrix ones(int aSize) {return RealColumnMatrix.ones(aSize);}
    public static RealColumnMatrix ones(int aRowNum, int aColNum) {return RealColumnMatrix.ones(aRowNum, aColNum);}
    public static RealColumnMatrix zeros(int aSize) {return RealColumnMatrix.zeros(aSize);}
    public static RealColumnMatrix zeros(int aRowNum, int aColNum) {return RealColumnMatrix.zeros(aRowNum, aColNum);}
    
    
    public static RealColumnMatrix from(int aSize, IMatrixGetter<? extends Number> aMatrixGetter) {return from(aSize, aSize, aMatrixGetter);}
    public static RealColumnMatrix from(int aRowNum, int aColNum, IMatrixGetter<? extends Number> aMatrixGetter) {
        RealColumnMatrix rMatrix = zeros(aRowNum, aColNum);
        rMatrix.fillWith(aMatrixGetter);
        return rMatrix;
    }
}
