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
        public CacheableColumnMatrix(int aNumRows, int aNumCols, double[] aData) {super(aNumRows, aNumCols, aData);}
        /** 重写这些方法来让这个 cache 可以顺利相互转换 */
        @Override public VectorCache.CacheableVector asVecCol() {return new VectorCache.CacheableVector(internalDataSize(), internalData());}
        /** 从缓存中获取的数据一律不允许进行后续修改 */
        @Override public void setInternalData(double[] aData) {throw new UnsupportedOperationException();}
        @Override public void setReturned() {mData = null;}
    }
    final static class CacheableRowMatrix extends RowMatrix implements ICacheableMatrix {
        public CacheableRowMatrix(int aNumRows, int aNumCols, double[] aData) {super(aNumRows, aNumCols, aData);}
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
    
    
    public static @NotNull RowMatrix getZeros(int aNumRows, int aNumCols) {
        return getZerosRow(aNumRows, aNumCols);
    }
    public static @NotNull List<RowMatrix> getZeros(int aNumRows, int aNumCols, int aMultiple) {
        return getZerosRow(aNumRows, aNumCols, aMultiple);
    }
    public static @NotNull ColumnMatrix getZerosCol(int aNumRows, int aNumCols) {
        return new CacheableColumnMatrix(aNumRows, aNumCols, DoubleArrayCache.getZeros(aNumRows*aNumCols));
    }
    public static @NotNull List<ColumnMatrix> getZerosCol(final int aNumRows, final int aNumCols, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getZerosTo(aNumRows*aNumCols, aMultiple, (i, arr) -> rOut.add(new CacheableColumnMatrix(aNumRows, aNumCols, arr)));
        return rOut;
    }
    public static @NotNull RowMatrix getZerosRow(int aNumRows, int aNumCols) {
        return new CacheableRowMatrix(aNumRows, aNumCols, DoubleArrayCache.getZeros(aNumRows*aNumCols));
    }
    public static @NotNull List<RowMatrix> getZerosRow(final int aNumRows, final int aNumCols, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getZerosTo(aNumRows*aNumCols, aMultiple, (i, arr) -> rOut.add(new CacheableRowMatrix(aNumRows, aNumCols, arr)));
        return rOut;
    }
    public static @NotNull RowMatrix getMat(int aNumRows, int aNumCols) {
        return getMatRow(aNumRows, aNumCols);
    }
    public static @NotNull List<RowMatrix> getMat(int aNumRows, int aNumCols, int aMultiple) {
        return getMatRow(aNumRows, aNumCols, aMultiple);
    }
    public static @NotNull ColumnMatrix getMatCol(int aNumRows, int aNumCols) {
        return new CacheableColumnMatrix(aNumRows, aNumCols, DoubleArrayCache.getArray(aNumRows*aNumCols));
    }
    public static @NotNull List<ColumnMatrix> getMatCol(final int aNumRows, final int aNumCols, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getArrayTo(aNumRows*aNumCols, aMultiple, (i, arr) -> rOut.add(new CacheableColumnMatrix(aNumRows, aNumCols, arr)));
        return rOut;
    }
    public static @NotNull RowMatrix getMatRow(int aNumRows, int aNumCols) {
        return new CacheableRowMatrix(aNumRows, aNumCols, DoubleArrayCache.getArray(aNumRows*aNumCols));
    }
    public static @NotNull List<RowMatrix> getMatRow(final int aNumRows, final int aNumCols, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getArrayTo(aNumRows*aNumCols, aMultiple, (i, arr) -> rOut.add(new CacheableRowMatrix(aNumRows, aNumCols, arr)));
        return rOut;
    }
}
