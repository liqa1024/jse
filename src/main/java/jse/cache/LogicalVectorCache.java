package jse.cache;

import jse.code.collection.AbstractCollections;
import jse.math.vector.BooleanArrayVector;
import jse.math.vector.ILogicalVector;
import jse.math.vector.LogicalVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

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
    
    interface ICacheableLogicalVector {}
    final static class CacheableLogicalVector extends LogicalVector implements ICacheableLogicalVector {
        public CacheableLogicalVector(int aSize, boolean[] aData) {super(aSize, aData);}
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    static boolean isFromCache(ILogicalVector aVector) {
        return (aVector instanceof ICacheableLogicalVector);
    }
    
    public static void returnVec(@NotNull ILogicalVector aVector) {
        if (!isFromCache(aVector)) throw new IllegalArgumentException("Return LogicalVector MUST be from cache");
        BooleanArrayVector tBooleanArrayVector = (BooleanArrayVector)aVector;
        boolean @Nullable[] tData = tBooleanArrayVector.internalData();
        if (tData == null) throw new IllegalStateException("Redundant return LogicalVector");
        tBooleanArrayVector.setInternalData(null);
        BooleanArrayCache.returnArray(tData);
    }
    public static void returnVec(final @NotNull List<? extends @NotNull ILogicalVector> aVectorList) {
        if (aVectorList.isEmpty()) return;
        // 这里不实际缓存 List<IVector>，而是直接统一归还内部值，这样实现会比较简单
        BooleanArrayCache.returnArrayFrom(aVectorList.size(), i -> {
            ILogicalVector tVector = aVectorList.get(i);
            if (!isFromCache(tVector)) throw new IllegalArgumentException("Return LogicalVector MUST be from cache");
            BooleanArrayVector tBooleanArrayVector = (BooleanArrayVector)tVector;
            boolean @Nullable[] tData = tBooleanArrayVector.internalData();
            if (tData == null) throw new IllegalStateException("Redundant return LogicalVector");
            tBooleanArrayVector.setInternalData(null);
            return tData;
        });
    }
    
    
    public static @NotNull LogicalVector getZeros(int aSize) {
        return new CacheableLogicalVector(aSize, BooleanArrayCache.getZeros(aSize));
    }
    public static @NotNull List<LogicalVector> getZeros(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<LogicalVector> rOut = new ArrayList<>(aMultiple);
        BooleanArrayCache.getZerosTo(aSize, aMultiple, (i, arr) -> rOut.add(new CacheableLogicalVector(aSize, arr)));
        return rOut;
    }
    public static @NotNull LogicalVector getVec(int aSize) {
        return new CacheableLogicalVector(aSize, BooleanArrayCache.getArray(aSize));
    }
    public static @NotNull List<LogicalVector> getVec(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<LogicalVector> rOut = new ArrayList<>(aMultiple);
        BooleanArrayCache.getArrayTo(aSize, aMultiple, (i, arr) -> rOut.add(new CacheableLogicalVector(aSize, arr)));
        return rOut;
    }
}
