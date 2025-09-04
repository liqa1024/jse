package jsex.nnap.basis;

import jse.code.Conf;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.Vector;

public abstract class MergeableBasis extends Basis {
    
    private final Vector mForwardCacheShell = new Vector(0, null);
    private final Vector mBackwardCacheShell = new Vector(0, null);
    private final Vector mForwardForceCacheShell = new Vector(0, null);
    
    /** @return {@inheritDoc} */
    @Override public abstract MergeableBasis threadSafeRef();
    
    /// MergeableBasis 需要给出 cache 的大小用来让 Merge 可以合并
    protected abstract int forwardCacheSize_(int aNN, boolean aFullCache);
    protected abstract int backwardCacheSize_(int aNN);
    protected abstract int forwardForceCacheSize_(int aNN, boolean aFullCache);
    
    @Override
    public final void forward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleList rForwardCache, boolean aFullCache) {
        validCache_(rForwardCache, forwardCacheSize_(aNlDx.size(), aFullCache));
        mForwardCacheShell.setInternalData(rForwardCache.internalData()); mForwardCacheShell.setInternalDataSize(rForwardCache.size());
        forward_(aNlDx, aNlDy, aNlDz, aNlType, rFp, mForwardCacheShell, aFullCache);
    }
    protected abstract void forward_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleArrayVector rForwardCache, boolean aFullCache);
    
    @Override
    public final void backward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleList aForwardCache, DoubleList rBackwardCache, boolean aKeepCache) {
        mForwardCacheShell.setInternalData(aForwardCache.internalData()); mForwardCacheShell.setInternalDataSize(aForwardCache.size());
        validCache_(rBackwardCache, backwardCacheSize_(aNlDx.size()));
        mBackwardCacheShell.setInternalData(rBackwardCache.internalData()); mBackwardCacheShell.setInternalDataSize(rBackwardCache.size());
        backward_(aNlDx, aNlDy, aNlDz, aNlType, aGradFp, rGradPara, mForwardCacheShell, mBackwardCacheShell, aKeepCache);
    }
    protected abstract void backward_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleArrayVector aForwardCache, DoubleArrayVector rBackwardCache, boolean aKeepCache);
    
    
    static void clearForce_(DoubleList rFx, DoubleList rFy, DoubleList rFz) {
        final int tSize = rFx.internalDataSize();
        if (Conf.OPERATION_CHECK) {
            if (rFy.internalDataSize() != tSize) throw new IllegalArgumentException("data size mismatch");
            if (rFz.internalDataSize() != tSize) throw new IllegalArgumentException("data size mismatch");
        } else {
            if (rFy.internalDataSize() < tSize) throw new IllegalArgumentException("data size mismatch");
            if (rFz.internalDataSize() < tSize) throw new IllegalArgumentException("data size mismatch");
        }
        final double[] tFx = rFx.internalData();
        final double[] tFy = rFy.internalData();
        final double[] tFz = rFz.internalData();
        for (int i = 0; i < tSize; ++i) {
            tFx[i] = 0.0;
            tFy[i] = 0.0;
            tFz[i] = 0.0;
        }
    }
    @Override
    public final void forwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleList aForwardCache, DoubleList rForwardForceCache, boolean aFullCache) {
        // 这里需要手动清空旧值
        clearForce_(rFx,  rFy, rFz);
        mForwardCacheShell.setInternalData(aForwardCache.internalData()); mForwardCacheShell.setInternalDataSize(aForwardCache.size());
        validCache_(rForwardForceCache, forwardForceCacheSize_(aNlDx.size(), aFullCache));
        mForwardForceCacheShell.setInternalData(rForwardForceCache.internalData()); mForwardForceCacheShell.setInternalDataSize(rForwardForceCache.size());
        forwardForceAccumulate_(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, rFx, rFy, rFz, mForwardCacheShell, mForwardForceCacheShell, aFullCache);
    }
    /** 累加版本的计算力，此时不会清空计算的力值，防止多次写入时旧值被自动清理 */
    protected abstract void forwardForceAccumulate_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleArrayVector aForwardCache, DoubleArrayVector rForwardForceCache, boolean aFullCache);
}
