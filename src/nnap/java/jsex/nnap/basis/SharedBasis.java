package jsex.nnap.basis;

import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IVector;
import jse.parallel.ParforThreadPool;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * 基于其他元素基组的共享基组，和 {@link MirrorBasis}
 * 的区别主要是不进行种类映射，并且依旧保持神经网络的独立性。
 * 和单纯拷贝基组的主要区别是可拟合参数共享。
 * <p>
 * 现在是常用的其他种类默认基组
 * @author liqa
 */
public class SharedBasis extends Basis {
    
    private final Basis mSharedBasis;
    private final int mSharedType;
    public SharedBasis(Basis aSharedBasis, int aSharedType) {
        if (aSharedBasis instanceof SharedBasis) throw new IllegalArgumentException("SharedBasis MUST NOT be Shared");
        mSharedBasis = aSharedBasis;
        mSharedType = aSharedType;
    }
    public Basis sharedBasis() {return mSharedBasis;}
    public int sharedType() {return mSharedType;}
    
    @Override public SharedBasis threadSafeRef() {
        return new SharedBasis(mSharedBasis.threadSafeRef(), mSharedType);
    }
    // 虽然 shared 本身没有参数，但是为了逻辑一致这里依旧转发这些接口
    @Override public void initParameters() {mSharedBasis.initParameters();}
    @Override public IVector parameters() {return mSharedBasis.parameters();}
    @Override public boolean hasParameters() {return mSharedBasis.hasParameters();}
    @Override public void initScale(List<DoubleList> aNlDxList, List<DoubleList> aNlDyList, List<DoubleList> aNlDzList, List<IntList> aNlTypeList, ParforThreadPool aPool) {
        throw new UnsupportedOperationException("initScale for SharedBasis");
    }
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "share");
        rSaveTo.put("share", mSharedType);
    }
    
    @Override public double rcut() {return mSharedBasis.rcut();}
    @Override public int size() {return mSharedBasis.size();}
    @Override public int atomTypeNumber() {return mSharedBasis.atomTypeNumber();}
    @Override public boolean hasSymbol() {return mSharedBasis.hasSymbol();}
    @Override public String symbol(int aType) {return mSharedBasis.symbol(aType);}
    
    @Override protected void shutdown_() {
        mSharedBasis.shutdown();
    }
    
    @Override
    public final void forward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleList rForwardCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        mSharedBasis.forward(aNlDx, aNlDy, aNlDz, aNlType, rFp, rForwardCache, aFullCache);
    }
    @Override
    public final void backward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleList aForwardCache, DoubleList rBackwardCache, boolean aKeepCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        mSharedBasis.backward(aNlDx, aNlDy, aNlDz, aNlType, aGradFp, rGradPara, aForwardCache, rBackwardCache, aKeepCache);
    }
    @Override
    public final void forwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleList aForwardCache, DoubleList rForwardForceCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        mSharedBasis.forwardForce(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache);
    }
    @Override
    public final void backwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList aGradFx, DoubleList aGradFy, DoubleList aGradFz, DoubleArrayVector rGradNNGrad, @Nullable DoubleArrayVector rGradPara,
                                    DoubleList aForwardCache, DoubleList aForwardForceCache, DoubleList rBackwardCache, DoubleList rBackwardForceCache, boolean aKeepCache, boolean aFixBasis) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        mSharedBasis.backwardForce(aNlDx, aNlDy, aNlDz, aNlType, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aKeepCache, aFixBasis);
    }
}
