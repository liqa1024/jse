package jse.cache;

import jse.code.collection.AbstractCollections;
import jse.math.matrix.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 专门针对 {@link IIntMatrix} 和 {@code List<IIntMatrix>} 的全局线程独立缓存，
 * 基于 {@link IntArrayCache} 实现
 * <p>
 * 会在内存不足时自动回收缓存
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class IntMatrixCache {
    private IntMatrixCache() {}
    
    interface ICacheableIntMatrix {}
    final static class CacheableColumnIntMatrix extends ColumnIntMatrix implements ICacheableIntMatrix {
        public CacheableColumnIntMatrix(int aRowNum, int aColNum, int[] aData) {super(aRowNum, aColNum, aData);}
        /** 重写这些方法来让这个 cache 可以顺利相互转换 */
        @Override public IntVectorCache.CacheableIntVector asVecCol() {return new IntVectorCache.CacheableIntVector(internalDataSize(), internalData());}
    }
    final static class CacheableRowIntMatrix extends RowIntMatrix implements ICacheableIntMatrix {
        public CacheableRowIntMatrix(int aRowNum, int aColNum, int[] aData) {super(aRowNum, aColNum, aData);}
        /** 重写这些方法来让这个 cache 可以顺利相互转换 */
        @Override public IntVectorCache.CacheableIntVector asVecRow() {return new IntVectorCache.CacheableIntVector(internalDataSize(), internalData());}
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isFromCache(IIntMatrix aIntMatrix) {
        return (aIntMatrix instanceof ICacheableIntMatrix);
    }
    
    public static void returnMat(@NotNull IIntMatrix aMatrix) {
        if (!isFromCache(aMatrix)) throw new IllegalArgumentException("Return IntMatrix MUST be from cache");
        IntArrayMatrix tIntArrayMatrix = (IntArrayMatrix)aMatrix;
        int @Nullable[] tData = tIntArrayMatrix.internalData();
        if (tData == null) throw new IllegalStateException("Redundant return IntMatrix");
        tIntArrayMatrix.setInternalData(null);
        IntArrayCache.returnArray(tData);
    }
    public static void returnMat(final @NotNull List<? extends @NotNull IIntMatrix> aMatrixList) {
        if (aMatrixList.isEmpty()) return;
        // 这里不实际缓存 List<IMatrix>，而是直接统一归还内部值，这样实现会比较简单
        IntArrayCache.returnArrayFrom(aMatrixList.size(), i -> {
            IIntMatrix tMatrix = aMatrixList.get(i);
            if (!isFromCache(tMatrix)) throw new IllegalArgumentException("Return IntMatrix MUST be from cache");
            IntArrayMatrix tIntArrayMatrix = (IntArrayMatrix)tMatrix;
            int @Nullable[] tData = tIntArrayMatrix.internalData();
            if (tData == null) throw new IllegalStateException("Redundant return IntMatrix");
            tIntArrayMatrix.setInternalData(null);
            return tData;
        });
    }
    
    
    public static @NotNull ColumnIntMatrix getZeros(int aRowNum, int aColNum) {
        return getZerosCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnIntMatrix> getZeros(int aRowNum, int aColNum, int aMultiple) {
        return getZerosCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnIntMatrix getZerosCol(int aRowNum, int aColNum) {
        return new CacheableColumnIntMatrix(aRowNum, aColNum, IntArrayCache.getZeros(aRowNum*aColNum));
    }
    public static @NotNull List<ColumnIntMatrix> getZerosCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnIntMatrix> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getZerosTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new CacheableColumnIntMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull RowIntMatrix getZerosRow(int aRowNum, int aColNum) {
        return new CacheableRowIntMatrix(aRowNum, aColNum, IntArrayCache.getZeros(aRowNum*aColNum));
    }
    public static @NotNull List<RowIntMatrix> getZerosRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowIntMatrix> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getZerosTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new CacheableRowIntMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull ColumnIntMatrix getMat(int aRowNum, int aColNum) {
        return getMatCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnIntMatrix> getMat(int aRowNum, int aColNum, int aMultiple) {
        return getMatCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnIntMatrix getMatCol(int aRowNum, int aColNum) {
        return new CacheableColumnIntMatrix(aRowNum, aColNum, IntArrayCache.getArray(aRowNum*aColNum));
    }
    public static @NotNull List<ColumnIntMatrix> getMatCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnIntMatrix> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getArrayTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new CacheableColumnIntMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull RowIntMatrix getMatRow(int aRowNum, int aColNum) {
        return new CacheableRowIntMatrix(aRowNum, aColNum, IntArrayCache.getArray(aRowNum*aColNum));
    }
    public static @NotNull List<RowIntMatrix> getMatRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowIntMatrix> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getArrayTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new CacheableRowIntMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
}
