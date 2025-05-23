package jse.cache;

import jse.code.collection.AbstractCollections;
import jse.math.matrix.BiDoubleArrayMatrix;
import jse.math.matrix.ColumnComplexMatrix;
import jse.math.matrix.IComplexMatrix;
import jse.math.matrix.RowComplexMatrix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    
    interface ICacheableComplexMatrix {
        double[][] internalData();
        void setReturned();
    }
    final static class CacheableColumnComplexMatrix extends ColumnComplexMatrix implements ICacheableComplexMatrix {
        public CacheableColumnComplexMatrix(int aRowNum, int aColNum, double[][] aData) {super(aRowNum, aColNum, aData);}
        /** 重写这些方法来让这个 cache 可以顺利相互转换 */
        @Override public ComplexVectorCache.CacheableComplexVector asVecCol() {return new ComplexVectorCache.CacheableComplexVector(internalDataSize(), internalData());}
        /** 从缓存中获取的数据一律不允许进行后续修改 */
        @Override public void setInternalData(double[][] aData) {throw new UnsupportedOperationException();}
        @Override public void setReturned() {mData = null;}
    }
    final static class CacheableRowComplexMatrix extends RowComplexMatrix implements ICacheableComplexMatrix {
        public CacheableRowComplexMatrix(int aRowNum, int aColNum, double[][] aData) {super(aRowNum, aColNum, aData);}
        /** 重写这些方法来让这个 cache 可以顺利相互转换 */
        @Override public ComplexVectorCache.CacheableComplexVector asVecRow() {return new ComplexVectorCache.CacheableComplexVector(internalDataSize(), internalData());}
        /** 从缓存中获取的数据一律不允许进行后续修改 */
        @Override public void setInternalData(double[][] aData) {throw new UnsupportedOperationException();}
        @Override public void setReturned() {mData = null;}
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isFromCache(IComplexMatrix aComplexMatrix) {
        return (aComplexMatrix instanceof ICacheableComplexMatrix);
    }
    
    public static void returnMat(@NotNull IComplexMatrix aComplexMatrix) {
        if (!isFromCache(aComplexMatrix)) throw new IllegalArgumentException("Return ComplexMatrix MUST be from cache");
        ICacheableComplexMatrix tCacheableComplexMatrix = (ICacheableComplexMatrix)aComplexMatrix;
        double @Nullable[][] tData = tCacheableComplexMatrix.internalData();
        if (tData == null) throw new IllegalStateException("Redundant return ComplexMatrix");
        tCacheableComplexMatrix.setReturned();
        DoubleArrayCache.returnArrayFrom(2, i -> tData[1-i]);
    }
    public static void returnMat(final @NotNull List<? extends @NotNull IComplexMatrix> aComplexMatrixList) {
        if (aComplexMatrixList.isEmpty()) return;
        // 这里不实际缓存 List<IComplexMatrix>，而是直接统一归还内部值，这样实现会比较简单
        final double[][] tArrayBuffer = {null};
        DoubleArrayCache.returnArrayFrom(aComplexMatrixList.size()*2, i -> {
            double[] tArrayReal = tArrayBuffer[0];
            if (tArrayReal == null) {
                IComplexMatrix tComplexMatrix = aComplexMatrixList.get(i/2);
                if (!isFromCache(tComplexMatrix)) throw new IllegalArgumentException("Return ComplexMatrix MUST be from cache");
                ICacheableComplexMatrix tCacheableComplexMatrix = (ICacheableComplexMatrix)tComplexMatrix;
                double @Nullable[][] tData = tCacheableComplexMatrix.internalData();
                if (tData == null) throw new IllegalStateException("Redundant return ComplexMatrix");
                tCacheableComplexMatrix.setReturned();
                tArrayBuffer[0] = tData[0];
                return tData[1];
            } else {
                tArrayBuffer[0] = null;
                return tArrayReal;
            }
        });
    }
    
    
    public static @NotNull ColumnComplexMatrix getZeros(int aRowNum, int aColNum) {
        return getZerosCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnComplexMatrix> getZeros(int aRowNum, int aColNum, int aMultiple) {
        return getZerosCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnComplexMatrix getZerosCol(int aRowNum, int aColNum) {
        final double[][] rData = new double[2][];
        DoubleArrayCache.getZerosTo(aRowNum*aColNum, 2, (i, arr) -> rData[i] = arr);
        return new CacheableColumnComplexMatrix(aRowNum, aColNum, rData);
    }
    public static @NotNull List<ColumnComplexMatrix> getZerosCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnComplexMatrix> rOut = new ArrayList<>(aMultiple);
        final double[][] tArrayBuffer = {null};
        DoubleArrayCache.getZerosTo(aRowNum*aColNum, aMultiple*2, (i, arr) -> {
            double[] tArrayReal = tArrayBuffer[0];
            if (tArrayReal == null) {
                tArrayBuffer[0] = arr;
            } else {
                rOut.add(new CacheableColumnComplexMatrix(aRowNum, aColNum, new double[][]{tArrayReal, arr}));
                tArrayBuffer[0] = null;
            }
        });
        return rOut;
    }
    public static @NotNull RowComplexMatrix getZerosRow(int aRowNum, int aColNum) {
        final double[][] rData = new double[2][];
        DoubleArrayCache.getZerosTo(aRowNum*aColNum, 2, (i, arr) -> rData[i] = arr);
        return new CacheableRowComplexMatrix(aRowNum, aColNum, rData);
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
                rOut.add(new CacheableRowComplexMatrix(aRowNum, aColNum, new double[][]{tArrayReal, arr}));
                tArrayBuffer[0] = null;
            }
        });
        return rOut;
    }
    public static @NotNull ColumnComplexMatrix getMat(int aRowNum, int aColNum) {
        return getMatCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnComplexMatrix> getMat(int aRowNum, int aColNum, int aMultiple) {
        return getMatCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnComplexMatrix getMatCol(int aRowNum, int aColNum) {
        final double[][] rData = new double[2][];
        DoubleArrayCache.getArrayTo(aRowNum*aColNum, 2, (i, arr) -> rData[i] = arr);
        return new CacheableColumnComplexMatrix(aRowNum, aColNum, rData);
    }
    public static @NotNull List<ColumnComplexMatrix> getMatCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnComplexMatrix> rOut = new ArrayList<>(aMultiple);
        final double[][] tArrayBuffer = {null};
        DoubleArrayCache.getArrayTo(aRowNum*aColNum, aMultiple*2, (i, arr) -> {
            double[] tArrayReal = tArrayBuffer[0];
            if (tArrayReal == null) {
                tArrayBuffer[0] = arr;
            } else {
                rOut.add(new CacheableColumnComplexMatrix(aRowNum, aColNum, new double[][]{tArrayReal, arr}));
                tArrayBuffer[0] = null;
            }
        });
        return rOut;
    }
    public static @NotNull RowComplexMatrix getMatRow(int aRowNum, int aColNum) {
        final double[][] rData = new double[2][];
        DoubleArrayCache.getArrayTo(aRowNum*aColNum, 2, (i, arr) -> rData[i] = arr);
        return new CacheableRowComplexMatrix(aRowNum, aColNum, rData);
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
                rOut.add(new CacheableRowComplexMatrix(aRowNum, aColNum, new double[][]{tArrayReal, arr}));
                tArrayBuffer[0] = null;
            }
        });
        return rOut;
    }
}
