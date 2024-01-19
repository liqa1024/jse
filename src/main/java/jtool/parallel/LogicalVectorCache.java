package jtool.parallel;

import jtool.code.collection.AbstractCollections;
import jtool.math.vector.ILogicalVector;
import jtool.math.vector.BooleanArrayVector;
import jtool.math.vector.LogicalVector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static jtool.code.CS.NO_CACHE;

/**
 * 专门针对 {@link ILogicalVector} 和 {@code List<ILogicalVector>} 的全局线程独立缓存，
 * 基于 {@link BooleanArrayCache} 实现
 * <p>
 * 会在内存不足时自动回收缓存
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class LogicalVectorCache {
    private LogicalVectorCache() {}
    
    public static void returnVec(@NotNull ILogicalVector aVector) {
        if (NO_CACHE) return;
        BooleanArrayCache.returnArray(((BooleanArrayVector)aVector).internalData());
    }
    public static void returnVec(final @NotNull List<? extends @NotNull ILogicalVector> aVectorList) {
        if (NO_CACHE) return;
        if (aVectorList.isEmpty()) return;
        // 这里不实际缓存 List<IVector>，而是直接统一归还内部值，这样实现会比较简单
        BooleanArrayCache.returnArrayFrom(aVectorList.size(), i -> ((BooleanArrayVector)aVectorList.get(i)).internalData());
    }
    
    
    public static @NotNull LogicalVector getZeros(int aSize) {
        return new LogicalVector(aSize, BooleanArrayCache.getZeros(aSize));
    }
    public static @NotNull List<LogicalVector> getZeros(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<LogicalVector> rOut = new ArrayList<>(aMultiple);
        BooleanArrayCache.getZerosTo(aSize, aMultiple, (i, arr) -> rOut.add(new LogicalVector(aSize, arr)));
        return rOut;
    }
    public static @NotNull LogicalVector getVec(int aSize) {
        return new LogicalVector(aSize, BooleanArrayCache.getArray(aSize));
    }
    public static @NotNull List<LogicalVector> getVec(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<LogicalVector> rOut = new ArrayList<>(aMultiple);
        BooleanArrayCache.getArrayTo(aSize, aMultiple, (i, arr) -> rOut.add(new LogicalVector(aSize, arr)));
        return rOut;
    }
}
