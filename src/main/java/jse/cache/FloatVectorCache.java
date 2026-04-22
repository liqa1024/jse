package jse.cache;

import jse.code.collection.AbstractCollections;
import jse.math.vector.IFloatVector;
import jse.math.vector.FloatVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 专门针对 {@link IFloatVector} 和 {@code List<IFloatVector>} 的全局线程独立缓存，
 * 基于 {@link FloatArrayCache} 实现
 * <p>
 * 会在内存不足时自动回收缓存
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class FloatVectorCache {
    private FloatVectorCache() {}
    
    interface ICacheableFloatVector {
        float[] internalData();
        void setReturned();
    }
    final static class CacheableFloatVector extends FloatVector implements ICacheableFloatVector {
        public CacheableFloatVector(int aSize, float[] aData) {super(aSize, aData);}
        /** 从缓存中获取的数据一律不允许进行后续修改 */
        @Override public void setInternalData(float[] aData) {throw new UnsupportedOperationException();}
        @Override public void setReturned() {mData = null;}
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isFromCache(IFloatVector aVector) {
        return (aVector instanceof ICacheableFloatVector);
    }
    
    public static void returnVec(@NotNull IFloatVector aVector) {
        if (!isFromCache(aVector)) throw new IllegalArgumentException("Return FloatVector MUST from cache");
        ICacheableFloatVector tCacheableVector = (ICacheableFloatVector)aVector;
        float @Nullable[] tData = tCacheableVector.internalData();
        if (tData == null) throw new IllegalStateException("Redundant return FloatVector");
        tCacheableVector.setReturned();
        FloatArrayCache.returnArray(tData);
    }
    public static void returnVec(final @NotNull List<? extends @NotNull IFloatVector> aVectorList) {
        if (aVectorList.isEmpty()) return;
        // 这里不实际缓存 List<IVector>，而是直接统一归还内部值，这样实现会比较简单
        FloatArrayCache.returnArrayFrom(aVectorList.size(), i -> {
            IFloatVector tVector = aVectorList.get(i);
            if (!isFromCache(tVector)) throw new IllegalArgumentException("Return FloatVector MUST from cache");
            ICacheableFloatVector tCacheableVector = (ICacheableFloatVector)tVector;
            float @Nullable[] tData = tCacheableVector.internalData();
            if (tData == null) throw new IllegalStateException("Redundant return FloatVector");
            tCacheableVector.setReturned();
            return tData;
        });
    }
    
    
    public static @NotNull FloatVector getZeros(int aSize) {
        return new CacheableFloatVector(aSize, FloatArrayCache.getZeros(aSize));
    }
    public static @NotNull List<FloatVector> getZeros(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<FloatVector> rOut = new ArrayList<>(aMultiple);
        FloatArrayCache.getZerosTo(aSize, aMultiple, (i, arr) -> rOut.add(new CacheableFloatVector(aSize, arr)));
        return rOut;
    }
    public static @NotNull FloatVector getVec(int aSize) {
        return new CacheableFloatVector(aSize, FloatArrayCache.getArray(aSize));
    }
    public static @NotNull List<FloatVector> getVec(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<FloatVector> rOut = new ArrayList<>(aMultiple);
        FloatArrayCache.getArrayTo(aSize, aMultiple, (i, arr) -> rOut.add(new CacheableFloatVector(aSize, arr)));
        return rOut;
    }
}
