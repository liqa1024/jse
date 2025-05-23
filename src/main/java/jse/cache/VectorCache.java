package jse.cache;

import jse.code.collection.AbstractCollections;
import jse.math.vector.IVector;
import jse.math.vector.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 专门针对 {@link IVector} 和 {@code List<IVector>} 的全局线程独立缓存，
 * 基于 {@link DoubleArrayCache} 实现
 * <p>
 * 会在内存不足时自动回收缓存
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class VectorCache {
    private VectorCache() {}
    
    interface ICacheableVector {
        double[] internalData();
        void setReturned();
    }
    final static class CacheableVector extends Vector implements ICacheableVector {
        public CacheableVector(int aSize, double[] aData) {super(aSize, aData);}
        /** 从缓存中获取的数据一律不允许进行后续修改 */
        @Override public void setInternalData(double[] aData) {throw new UnsupportedOperationException();}
        @Override public void setReturned() {mData = null;}
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isFromCache(IVector aVector) {
        return (aVector instanceof ICacheableVector);
    }
    
    public static void returnVec(@NotNull IVector aVector) {
        if (!isFromCache(aVector)) throw new IllegalArgumentException("Return Vector MUST from cache");
        ICacheableVector tCacheableVector = (ICacheableVector)aVector;
        double @Nullable[] tData = tCacheableVector.internalData();
        if (tData == null) throw new IllegalStateException("Redundant return Vector");
        tCacheableVector.setReturned();
        DoubleArrayCache.returnArray(tData);
    }
    public static void returnVec(final @NotNull List<? extends @NotNull IVector> aVectorList) {
        if (aVectorList.isEmpty()) return;
        // 这里不实际缓存 List<IVector>，而是直接统一归还内部值，这样实现会比较简单
        DoubleArrayCache.returnArrayFrom(aVectorList.size(), i -> {
            IVector tVector = aVectorList.get(i);
            if (!isFromCache(tVector)) throw new IllegalArgumentException("Return Vector MUST from cache");
            ICacheableVector tCacheableVector = (ICacheableVector)tVector;
            double @Nullable[] tData = tCacheableVector.internalData();
            if (tData == null) throw new IllegalStateException("Redundant return Vector");
            tCacheableVector.setReturned();
            return tData;
        });
    }
    
    
    public static @NotNull Vector getZeros(int aSize) {
        return new CacheableVector(aSize, DoubleArrayCache.getZeros(aSize));
    }
    public static @NotNull List<Vector> getZeros(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<Vector> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getZerosTo(aSize, aMultiple, (i, arr) -> rOut.add(new CacheableVector(aSize, arr)));
        return rOut;
    }
    public static @NotNull Vector getVec(int aSize) {
        return new CacheableVector(aSize, DoubleArrayCache.getArray(aSize));
    }
    public static @NotNull List<Vector> getVec(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<Vector> rOut = new ArrayList<>(aMultiple);
        DoubleArrayCache.getArrayTo(aSize, aMultiple, (i, arr) -> rOut.add(new CacheableVector(aSize, arr)));
        return rOut;
    }
}
