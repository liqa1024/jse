package jtool.parallel;

import jtool.code.collection.AbstractCollections;
import jtool.math.matrix.ColumnIntMatrix;
import jtool.math.matrix.IIntMatrix;
import jtool.math.matrix.IntArrayMatrix;
import jtool.math.matrix.RowIntMatrix;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static jtool.code.CS.NO_CACHE;

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
    
    public static void returnMat(@NotNull IIntMatrix aMatrix) {
        if (NO_CACHE) return;
        IntArrayCache.returnArray(((IntArrayMatrix)aMatrix).internalData());
    }
    public static void returnMat(final @NotNull List<? extends @NotNull IIntMatrix> aMatrixList) {
        if (NO_CACHE) return;
        if (aMatrixList.isEmpty()) return;
        // 这里不实际缓存 List<IMatrix>，而是直接统一归还内部值，这样实现会比较简单
        IntArrayCache.returnArrayFrom(aMatrixList.size(), i -> ((IntArrayMatrix)aMatrixList.get(i)).internalData());
    }
    
    
    public static @NotNull ColumnIntMatrix getZeros(int aRowNum, int aColNum) {
        return getZerosCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnIntMatrix> getZeros(int aRowNum, int aColNum, int aMultiple) {
        return getZerosCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnIntMatrix getZerosCol(int aRowNum, int aColNum) {
        return new ColumnIntMatrix(aRowNum, aColNum, IntArrayCache.getZeros(aRowNum*aColNum));
    }
    public static @NotNull List<ColumnIntMatrix> getZerosCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnIntMatrix> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getZerosTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new ColumnIntMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull RowIntMatrix getZerosRow(int aRowNum, int aColNum) {
        return new RowIntMatrix(aRowNum, aColNum, IntArrayCache.getZeros(aRowNum*aColNum));
    }
    public static @NotNull List<RowIntMatrix> getZerosRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowIntMatrix> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getZerosTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new RowIntMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull ColumnIntMatrix getMat(int aRowNum, int aColNum) {
        return getMatCol(aRowNum, aColNum);
    }
    public static @NotNull List<ColumnIntMatrix> getMat(int aRowNum, int aColNum, int aMultiple) {
        return getMatCol(aRowNum, aColNum, aMultiple);
    }
    public static @NotNull ColumnIntMatrix getMatCol(int aRowNum, int aColNum) {
        return new ColumnIntMatrix(aRowNum, aColNum, IntArrayCache.getArray(aRowNum*aColNum));
    }
    public static @NotNull List<ColumnIntMatrix> getMatCol(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ColumnIntMatrix> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getArrayTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new ColumnIntMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
    public static @NotNull RowIntMatrix getMatRow(int aRowNum, int aColNum) {
        return new RowIntMatrix(aRowNum, aColNum, IntArrayCache.getArray(aRowNum*aColNum));
    }
    public static @NotNull List<RowIntMatrix> getMatRow(final int aRowNum, final int aColNum, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<RowIntMatrix> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getArrayTo(aRowNum*aColNum, aMultiple, (i, arr) -> rOut.add(new RowIntMatrix(aRowNum, aColNum, arr)));
        return rOut;
    }
}
