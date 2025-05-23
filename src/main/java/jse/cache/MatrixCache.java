package jse.cache;

import jse.code.collection.AbstractCollections;
import jse.math.matrix.ColumnMatrix;
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
    
    interface ICacheableMatrix {
        double[] internalData();
        void setReturned();
    }
    final static class CacheableColumnMatrix extends ColumnMatrix implements ICacheableMatrix {
        public CacheableColumnMatrix(int aRowNum, int aColNum, double[] aData) {super(aRowNum, aColNum, aData);}
        /** 重写这些方法来让这个 cache 可以顺利相互转换 */
        @Override public VectorCache.CacheableVector asVecCol() {return new VectorCache.CacheableVector(internalDataSize(), internalData());}
        /** 从缓存中获取的数据一律不允许进行后续修改 */
        @Override public void setInternalData(double[] aData) {throw new UnsupportedOperationException();}
        @Override public void setReturned() {mData = null;}
    }
    final static class CacheableRowMatrix extends RowMatrix implements ICacheableMatrix {
        public CacheableRowMatrix(int aRowNum, int aColNum, double[] aData) {super(aRowNum, aColNum, aData);}
        /** 重写这些方法来让这个 cache 可以顺利相互转换 */
        @Override public VectorCache.CacheableVector asVecRow() {return new VectorCache.CacheableVector(internalDataSize(), internalData());}
        /** 从缓存中获取的数据一律不允许进行后续修改 */
        @Override public void setInternalData(double[] aData) {throw new UnsupportedOperationException();}
        @Override public void setReturned() {mData = null;}
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isFromCache(IMatrix aMatrix) {
        return (aMatrix instanceof ICacheableMatrix);
    }
    
    public static void returnMat(@NotNull IMatrix aMatrix) {
        if (!isFromCache(aMatrix)) throw new IllegalArgumentException("Return Matrix MUST be from cache");
        ICacheableMatrix tCacheableMatrix = (ICacheableMatrix)aMatrix;
        double @Nullable[] tData = tCacheableMatrix.internalData();
        if (tData == null) throw new IllegalStateException("Redundant return Matrix");
        tCacheableMatrix.setReturned();
        DoubleArrayCache.returnArray(tData);
    }
    public static void returnMat(final @NotNull List<? extends @NotNull IMatrix> aMatrixList) {
        if (aMatrixList.isEmpty()) return;
        // 这里不实际缓存 List<IMatrix>，而是直接统一归还内部值，这样实现会比较简单
        DoubleArrayCache.returnArrayFrom(aMatrixList.size(), i -> {
            IMatrix tMatrix = aMatrixList.get(i);
            if (!isFromCache(tMatrix)) throw new IllegalArgumentException("Return Matrix MUST be from cache");
            ICacheableMatrix tCacheableMatrix = (ICacheableMatrix)tMatrix;
            double @Nullable[] tData = tCacheableMatrix.internalData();
            if (tData == null) throw new IllegalStateException("Redundant return Matrix");
            tCacheableMatrix.setReturned();
            return tData;
        });
    }
    
    
    public static @NotNull RowMatrix getZeros(int aRowNum, int aColNum) {
        return getZerosRow(aRowNum, aColNum);
    }
    public static @NotNull List<RowMatrix> getZeros(int aRowNum, int aColNum, int aMultiple) {
        return getZerosRow(aRowNum, aColNum, aMultiple);
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
    public static @NotNull RowMatrix getMat(int aRowNum, int aColNum) {
        return getMatRow(aRowNum, aColNum);
    }
    public static @NotNull List<RowMatrix> getMat(int aRowNum, int aColNum, int aMultiple) {
        return getMatRow(aRowNum, aColNum, aMultiple);
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
