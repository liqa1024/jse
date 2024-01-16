package jtool.parallel;

import jtool.code.collection.AbstractCollections;
import jtool.math.matrix.BiDoubleArrayMatrix;
import jtool.math.matrix.IComplexMatrix;
import jtool.math.matrix.RowComplexMatrix;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static jtool.code.CS.NO_CACHE;

/**
 * 专门针对 {@link IComplexMatrix} 和 {@code List<IComplexMatrix>} 的全局线程独立缓存，
 * 基于 {@link DoubleArrayCache} 实现
 * <p>
 * 会在内存不足时自动回收缓存
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class ComplexMatrixCache {
    private ComplexMatrixCache() {}
    
    public static void returnMat(@NotNull IComplexMatrix aComplexMatrix) {
        if (NO_CACHE) return;
        final double[][] tData = ((BiDoubleArrayMatrix)aComplexMatrix).internalData();
        DoubleArrayCache.returnArrayFrom(2, i -> tData[1-i]);
    }
    public static void returnMat(final @NotNull List<? extends @NotNull IComplexMatrix> aComplexMatrixList) {
        if (NO_CACHE) return;
        if (aComplexMatrixList.isEmpty()) return;
        // 这里不实际缓存 List<IComplexMatrix>，而是直接统一归还内部值，这样实现会比较简单
        final double[][] tArrayBuffer = {null};
        DoubleArrayCache.returnArrayFrom(aComplexMatrixList.size()*2, i -> {
            double[] tArrayReal = tArrayBuffer[0];
            if (tArrayReal == null) {
                double[][] tData = ((BiDoubleArrayMatrix)aComplexMatrixList.get(i/2)).internalData();
                tArrayBuffer[0] = tData[0];
                return tData[1];
            } else {
                tArrayBuffer[0] = null;
                return tArrayReal;
            }
        });
    }
    
    
    public static @NotNull RowComplexMatrix getZerosRow(int aRowNum, int aColNum) {
        final double[][] rData = new double[2][];
        DoubleArrayCache.getZerosTo(aRowNum*aColNum, 2, (i, arr) -> rData[i] = arr);
        return new RowComplexMatrix(aRowNum, aColNum, rData);
    }
    public static @NotNull List<RowComplexMatrix> getZerosRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowComplexMatrix> rOut = new ArrayList<>(aMultiple);
        final double[][] tArrayBuffer = {null};
        DoubleArrayCache.getZerosTo(aRowNum*aColNum, aMultiple*2, (i, arr) -> {
            double[] tArrayReal = tArrayBuffer[0];
            if (tArrayReal == null) {
                tArrayBuffer[0] = arr;
            } else {
                rOut.add(new RowComplexMatrix(aRowNum, aColNum, new double[][]{tArrayReal, arr}));
                tArrayBuffer[0] = null;
            }
        });
        return rOut;
    }
    public static @NotNull RowComplexMatrix getMatRow(int aRowNum, int aColNum) {
        final double[][] rData = new double[2][];
        DoubleArrayCache.getArrayTo(aRowNum*aColNum, 2, (i, arr) -> rData[i] = arr);
        return new RowComplexMatrix(aRowNum, aColNum, rData);
    }
    public static @NotNull List<RowComplexMatrix> getMatRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowComplexMatrix> rOut = new ArrayList<>(aMultiple);
        final double[][] tArrayBuffer = {null};
        DoubleArrayCache.getArrayTo(aRowNum*aColNum, aMultiple*2, (i, arr) -> {
            double[] tArrayReal = tArrayBuffer[0];
            if (tArrayReal == null) {
                tArrayBuffer[0] = arr;
            } else {
                rOut.add(new RowComplexMatrix(aRowNum, aColNum, new double[][]{tArrayReal, arr}));
                tArrayBuffer[0] = null;
            }
        });
        return rOut;
    }
}
