package jtool.parallel;

import jtool.code.collection.AbstractCollections;
import jtool.math.matrix.ColumnMatrix;
import jtool.math.matrix.DoubleArrayMatrix;
import jtool.math.matrix.IMatrix;
import jtool.math.matrix.RowMatrix;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static jtool.code.CS.NO_CACHE;

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
    
    public static void returnMat(@NotNull IMatrix aMatrix) {
        if (NO_CACHE) return;
        DoubleArrayCache.returnArray(((DoubleArrayMatrix)aMatrix).internalData());
    }
    public static void returnMat(final @NotNull List<? extends @NotNull IMatrix> aMatrixList) {
        if (NO_CACHE) return;
        if (aMatrixList.isEmpty()) return;
        // 这里不实际缓存 List<IMatrix>，而是直接统一归还内部值，这样实现会比较简单
        DoubleArrayCache.returnArrayFrom(aMatrixList.size(), i -> ((DoubleArrayMatrix)aMatrixList.get(i)).internalData());
    }
    
    
    public static @NotNull ColumnMatrix getZeros(int aRowNum, int aColNum) {
        return getZerosCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnMatrix> getZeros(int aRowNum, int aColNum, int aMultiple) {
        return getZerosCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnMatrix getZerosCol(int aRowNum, int aColNum) {
        return new ColumnMatrix(aRowNum, aColNum, DoubleArrayCache.getZeros(aRowNum*aColNum));
    }
    public static @NotNull List<ColumnMatrix> getZerosCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getZerosTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new ColumnMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull RowMatrix getZerosRow(int aRowNum, int aColNum) {
        return new RowMatrix(aRowNum, aColNum, DoubleArrayCache.getZeros(aRowNum*aColNum));
    }
    public static @NotNull List<RowMatrix> getZerosRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getZerosTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new RowMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull ColumnMatrix getMat(int aRowNum, int aColNum) {
        return getMatCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnMatrix> getMat(int aRowNum, int aColNum, int aMultiple) {
        return getMatCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnMatrix getMatCol(int aRowNum, int aColNum) {
        return new ColumnMatrix(aRowNum, aColNum, DoubleArrayCache.getArray(aRowNum*aColNum));
    }
    public static @NotNull List<ColumnMatrix> getMatCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getArrayTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new ColumnMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull RowMatrix getMatRow(int aRowNum, int aColNum) {
        return new RowMatrix(aRowNum, aColNum, DoubleArrayCache.getArray(aRowNum*aColNum));
    }
    public static @NotNull List<RowMatrix> getMatRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowMatrix> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getArrayTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new RowMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
}
