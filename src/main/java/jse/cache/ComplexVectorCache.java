package jse.cache;

import jse.code.collection.AbstractCollections;
import jse.math.vector.BiDoubleArrayVector;
import jse.math.vector.ComplexVector;
import jse.math.vector.IComplexVector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * 专门针对 {@link IComplexVector} 和 {@code List<IComplexVector>} 的全局线程独立缓存，
 * 基于 {@link DoubleArrayCache} 实现
 * <p>
 * 会在内存不足时自动回收缓存
 * <p>
 * 注意归还线程和借用线程一致
 * @author liqa
 */
public class ComplexVectorCache {
    private ComplexVectorCache() {}
    
    interface ICacheableComplexVector {}
    final static class CacheableComplexVector extends ComplexVector implements ICacheableComplexVector {
        public CacheableComplexVector(int aSize, double[][] aData) {super(aSize, aData);}
    }
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isFromCache(IComplexVector aVector) {
        return (aVector instanceof ICacheableComplexVector);
    }
    
    public static void returnVec(@NotNull IComplexVector aComplexVector) {
        if (!isFromCache(aComplexVector)) throw new IllegalArgumentException("Return ComplexVector MUST be from cache");
        BiDoubleArrayVector tBiDoubleArrayVector = (BiDoubleArrayVector)aComplexVector;
        double @Nullable[][] tData = tBiDoubleArrayVector.internalData();
        if (tData == null) throw new IllegalStateException("Redundant return ComplexVector");
        tBiDoubleArrayVector.setInternalData(null);
        DoubleArrayCache.returnArrayFrom(2, i -> tData[1-i]);
    }
    public static void returnVec(final @NotNull List<? extends @NotNull IComplexVector> aComplexVectorList) {
        if (aComplexVectorList.isEmpty()) return;
        // 这里不实际缓存 List<IComplexVector>，而是直接统一归还内部值，这样实现会比较简单
        final double[][] tArrayBuffer = {null};
        DoubleArrayCache.returnArrayFrom(aComplexVectorList.size()*2, i -> {
            double[] tArrayReal = tArrayBuffer[0];
            if (tArrayReal == null) {
                IComplexVector tComplexVector = aComplexVectorList.get(i/2);
                if (!isFromCache(tComplexVector)) throw new IllegalArgumentException("Return ComplexVector MUST be from cache");
                BiDoubleArrayVector tBiDoubleArrayVector = (BiDoubleArrayVector)tComplexVector;
                double @Nullable[][] tData = tBiDoubleArrayVector.internalData();
                if (tData == null) throw new IllegalStateException("Redundant return ComplexVector");
                tBiDoubleArrayVector.setInternalData(null);
                tArrayBuffer[0] = tData[0];
                return tData[1];
            } else {
                tArrayBuffer[0] = null;
                return tArrayReal;
            }
        });
    }
    
    
    public static @NotNull ComplexVector getZeros(int aSize) {
        final double[][] rData = new double[2][];
        DoubleArrayCache.getZerosTo(aSize, 2, (i, arr) -> rData[i] = arr);
        return new CacheableComplexVector(aSize, rData);
    }
    public static @NotNull List<ComplexVector> getZeros(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ComplexVector> rOut = new ArrayList<>(aMultiple);
        final double[][] tArrayBuffer = {null};
        DoubleArrayCache.getZerosTo(aSize, aMultiple*2, (i, arr) -> {
            double[] tArrayReal = tArrayBuffer[0];
            if (tArrayReal == null) {
                tArrayBuffer[0] = arr;
            } else {
                rOut.add(new CacheableComplexVector(aSize, new double[][]{tArrayReal, arr}));
                tArrayBuffer[0] = null;
            }
        });
        return rOut;
    }
    public static @NotNull ComplexVector getVec(int aSize) {
        final double[][] rData = new double[2][];
        DoubleArrayCache.getArrayTo(aSize, 2, (i, arr) -> rData[i] = arr);
        return new CacheableComplexVector(aSize, rData);
    }
    public static @NotNull List<ComplexVector> getVec(final int aSize, int aMultiple) {
        if (aMultiple <= 0) return AbstractCollections.zl();
        final List<ComplexVector> rOut = new ArrayList<>(aMultiple);
        final double[][] tArrayBuffer = {null};
        DoubleArrayCache.getArrayTo(aSize, aMultiple*2, (i, arr) -> {
            double[] tArrayReal = tArrayBuffer[0];
            if (tArrayReal == null) {
                tArrayBuffer[0] = arr;
            } else {
                rOut.add(new CacheableComplexVector(aSize, new double[][]{tArrayReal, arr}));
                tArrayBuffer[0] = null;
            }
        });
        return rOut;
    }
}
