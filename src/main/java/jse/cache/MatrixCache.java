package jse.cache;

import jse.code.collection.AbstractCollections;
import jse.math.matrix.ColumnMatrix;
import jse.math.matrix.DoubleArrayMatrix;
import jse.math.matrix.IMatrix;
import jse.math.matrix.RowMatrix;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 专门针对 {@link IMatrix} 和 {@code List<IMatrix>} 的全局线程独立缓存，
 * 基于 {@link DoubleArrayCache} 实现
 * <p>
 * 会在内存不足时自动回收缓存
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class MatrixCache {
    private MatrixCache() {}
    
    interface ICacheableMatrix {}
    final static class CacheableColumnMatrix extends ColumnMatrix implements ICacheableMatrix {
        public CacheableColumnMatrix(int aRowNum, int aColNum, double[] aData) {super(aRowNum, aColNum, aData);}
        /** 重写这些方法来让这个 cache 可以顺利相互转换 */
        @Override public VectorCache.CacheableVector asVecCol() {return new VectorCache.CacheableVector(internalDataSize(), internalData());}
    }
    final static class CacheableRowMatrix extends RowMatrix implements ICacheableMatrix {
        public CacheableRowMatrix(int aRowNum, int aColNum, double[] aData) {super(aRowNum, aColNum, aData);}
        /** 重写这些方法来让这个 cache 可以顺利相互转换 */
        @Override public VectorCache.CacheableVector asVecRow() {return new VectorCache.CacheableVector(internalDataSize(), internalData());}
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isFromCache(IMatrix aMatrix) {
        return (aMatrix instanceof ICacheableMatrix);
    }
    
    public static void returnMat(@NotNull IMatrix aMatrix) {
        if (!isFromCache(aMatrix)) throw new IllegalArgumentException("Return Matrix MUST be from cache");
        DoubleArrayMatrix tDoubleArrayMatrix = (DoubleArrayMatrix)aMatrix;
        double @Nullable[] tData = tDoubleArrayMatrix.internalData();
        if (tData == null) throw new IllegalStateException("Redundant return Matrix");
        tDoubleArrayMatrix.setInternalData(null);
        DoubleArrayCache.returnArray(tData);
    }
    public static void returnMat(final @NotNull List<? extends @NotNull IMatrix> aMatrixList) {
        if (aMatrixList.isEmpty()) return;
        // 这里不实际缓存 List<IMatrix>，而是直接统一归还内部值，这样实现会比较简单
        DoubleArrayCache.returnArrayFrom(aMatrixList.size(), i -> {
            IMatrix tMatrix = aMatrixList.get(i);
            if (!isFromCache(tMatrix)) throw new IllegalArgumentException("Return Matrix MUST be from cache");
            DoubleArrayMatrix tDoubleArrayMatrix = (DoubleArrayMatrix)tMatrix;
            double @Nullable[] tData = tDoubleArrayMatrix.internalData();
            if (tData == null) throw new IllegalStateException("Redundant return Matrix");
            tDoubleArrayMatrix.setInternalData(null);
            return tData;
        });
    }
    
    
    public static @NotNull ColumnMatrix getZeros(int aRowNum, int aColNum) {
        return getZerosCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnMatrix> getZeros(int aRowNum, int aColNum, int aMultiple) {
        return getZerosCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnMatrix getZerosCol(int aRowNum, int aColNum) {
        return new CacheableColumnMatrix(aRowNum, aColNum, DoubleArrayCache.getZeros(aRowNum*aColNum));
    }
    public static @NotNull List<ColumnMatrix> getZerosCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getZerosTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new CacheableColumnMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull RowMatrix getZerosRow(int aRowNum, int aColNum) {
        return new CacheableRowMatrix(aRowNum, aColNum, DoubleArrayCache.getZeros(aRowNum*aColNum));
    }
    public static @NotNull List<RowMatrix> getZerosRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getZerosTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new CacheableRowMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull ColumnMatrix getMat(int aRowNum, int aColNum) {
        return getMatCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnMatrix> getMat(int aRowNum, int aColNum, int aMultiple) {
        return getMatCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnMatrix getMatCol(int aRowNum, int aColNum) {
        return new CacheableColumnMatrix(aRowNum, aColNum, DoubleArrayCache.getArray(aRowNum*aColNum));
    }
    public static @NotNull List<ColumnMatrix> getMatCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getArrayTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new CacheableColumnMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull RowMatrix getMatRow(int aRowNum, int aColNum) {
        return new CacheableRowMatrix(aRowNum, aColNum, DoubleArrayCache.getArray(aRowNum*aColNum));
    }
    public static @NotNull List<RowMatrix> getMatRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getArrayTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new CacheableRowMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
}
