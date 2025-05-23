package jse.cache;

import jse.code.collection.AbstractCollections;
import jse.math.vector.IIntVector;
import jse.math.vector.IntVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 专门针对 {@link IIntVector} 和 {@code List<IIntVector>} 的全局线程独立缓存，
 * 基于 {@link IntArrayCache} 实现
 * <p>
 * 会在内存不足时自动回收缓存
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class IntVectorCache {
    private IntVectorCache() {}
    
    interface ICacheableIntVector {
        int[] internalData();
        void setReturned();
    }
    final static class CacheableIntVector extends IntVector implements ICacheableIntVector {
        public CacheableIntVector(int aSize, int[] aData) {super(aSize, aData);}
        /** 从缓存中获取的数据一律不允许进行后续修改 */
        @Override public void setInternalData(int[] aData) {throw new UnsupportedOperationException();}
        @Override public void setReturned() {mData = null;}
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isFromCache(IIntVector aVector) {
        return (aVector instanceof ICacheableIntVector);
    }
    
    public static void returnVec(@NotNull IIntVector aVector) {
        if (!isFromCache(aVector)) throw new IllegalArgumentException("Return IntVector MUST be from cache");
        ICacheableIntVector tCacheableIntVector = (ICacheableIntVector)aVector;
        int @Nullable[] tData = tCacheableIntVector.internalData();
        if (tData == null) throw new IllegalStateException("Redundant return IntVector");
        tCacheableIntVector.setReturned();
        IntArrayCache.returnArray(tData);
    }
    public static void returnVec(final @NotNull List<? extends @NotNull IIntVector> aVectorList) {
        if (aVectorList.isEmpty()) return;
        // 这里不实际缓存 List<IVector>，而是直接统一归还内部值，这样实现会比较简单
        IntArrayCache.returnArrayFrom(aVectorList.size(), i -> {
            IIntVector tVector = aVectorList.get(i);
            if (!isFromCache(tVector)) throw new IllegalArgumentException("Return IntVector MUST be from cache");
            ICacheableIntVector tCacheableIntVector = (ICacheableIntVector)tVector;
            int @Nullable[] tData = tCacheableIntVector.internalData();
            if (tData == null) throw new IllegalStateException("Redundant return IntVector");
            tCacheableIntVector.setReturned();
            return tData;
        });
    }
    
    
    public static @NotNull IntVector getZeros(int aSize) {
        return new CacheableIntVector(aSize, IntArrayCache.getZeros(aSize));
    }
    public static @NotNull List<IntVector> getZeros(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<IntVector> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getZerosTo(aSize, aMultiple, (i, arr) -> rOut.add(new CacheableIntVector(aSize, arr)));
        return rOut;
    }
    public static @NotNull IntVector getVec(int aSize) {
        return new CacheableIntVector(aSize, IntArrayCache.getArray(aSize));
    }
    public static @NotNull List<IntVector> getVec(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<IntVector> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getArrayTo(aSize, aMultiple, (i, arr) -> rOut.add(new CacheableIntVector(aSize, arr)));
        return rOut;
    }
}
