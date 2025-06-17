package jsex.nnap.basis;

import jse.code.Conf;
import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.vector.DoubleArrayVector;

public abstract class MergeableBasis extends Basis {
    
    @Override protected final void evalPartial_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz) {
        evalPartialWithShift_(aNlDx, aNlDy, aNlDz, aNlType, 0, 0, rFpPx, rFpPy, rFpPz);
    }
    /** 支持合并的基组专门实现，需要专门传入 aShiftFp 和 aRestFp 用来指定偏导数写入的位置以及需要后续保留的长度，避免多个写入导致折叠 */
    protected abstract void evalPartialWithShift_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType,
                                                  int aShiftFp, int aRestFp, DoubleList rFpPx, DoubleList rFpPy, DoubleList rFpPz);
    
    final void clearForce_(DoubleList rFx, DoubleList rFy, DoubleList rFz) {
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
    @Override protected final void evalPartialAndForceDot_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aFpGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz) {
        // 这里需要手动清空旧值
        clearForce_(rFx,  rFy, rFz);
        evalPartialAndForceDotAccumulate_(aNlDx, aNlDy, aNlDz, aNlType, aFpGrad, rFx, rFy, rFz);
    }
    /** 累加版本的计算力，此时不会清空计算的力值，防止多次写入时旧值被自动清理 */
    protected abstract void evalPartialAndForceDotAccumulate_(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aFpGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz);
}
