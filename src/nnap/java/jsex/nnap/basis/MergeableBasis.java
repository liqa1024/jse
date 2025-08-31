package jsex.nnap.basis;

import jse.code.Conf;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.vector.DoubleArrayVector;

public abstract class MergeableBasis extends Basis {
    
    /** @return {@inheritDoc} */
    @Override public abstract MergeableBasis threadSafeRef();
    
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
    @Override protected final void evalForce_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz) {
        // 这里需要手动清空旧值
        clearForce_(rFx,  rFy, rFz);
        evalForceAccumulate_(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, rFx, rFy, rFz);
    }
    /** 累加版本的计算力，此时不会清空计算的力值，防止多次写入时旧值被自动清理 */
    protected abstract void evalForceAccumulate_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz);
    
    
    @Override @Deprecated
    protected final void evalGrad_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        evalGradWithShift_(aNlDx, aNlDy, aNlDz, aNlType, 0, 0, rFpPx, rFpPy, rFpPz);
    }
    /** 支持合并的基组专门实现，需要专门传入 aShiftFp 和 aRestFp 用来指定偏导数写入的位置以及需要后续保留的长度，避免多个写入导致折叠 */
    @Deprecated
    protected abstract void evalGradWithShift_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType,
                                               int aShiftFp, int aRestFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz);
}
