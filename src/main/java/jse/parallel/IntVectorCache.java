package jse.parallel;

import jse.code.collection.AbstractCollections;
import jse.math.vector.IIntVector;
import jse.math.vector.IntArrayVector;
import jse.math.vector.IntVector;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import static jse.code.CS.NO_CACHE;

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
    
    public static void returnVec(@NotNull IIntVector aVector) {
        if (NO_CACHE) return;
        IntArrayCache.returnArray(((IntArrayVector)aVector).internalData());
    }
    public static void returnVec(final @NotNull List<? extends @NotNull IIntVector> aVectorList) {
        if (NO_CACHE) return;
        if (aVectorList.isEmpty()) return;
        // 这里不实际缓存 List<IVector>，而是直接统一归还内部值，这样实现会比较简单
        IntArrayCache.returnArrayFrom(aVectorList.size(), i -> ((IntArrayVector)aVectorList.get(i)).internalData());
    }
    
    
    public static @NotNull IntVector getZeros(int aSize) {
        return new IntVector(aSize, IntArrayCache.getZeros(aSize));
    }
    public static @NotNull List<IntVector> getZeros(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<IntVector> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getZerosTo(aSize, aMultiple, (i, arr) -> rOut.add(new IntVector(aSize, arr)));
        return rOut;
    }
    public static @NotNull IntVector getVec(int aSize) {
        return new IntVector(aSize, IntArrayCache.getArray(aSize));
    }
    public static @NotNull List<IntVector> getVec(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<IntVector> rOut = new ArrayList<>(aMultiple);
        IntArrayCache.getArrayTo(aSize, aMultiple, (i, arr) -> rOut.add(new IntVector(aSize, arr)));
        return rOut;
    }
}
