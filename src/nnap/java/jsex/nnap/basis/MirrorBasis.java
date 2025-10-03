package jsex.nnap.basis;

import jse.code.collection.DoubleList;
import jse.code.collection.IntList;
import jse.math.vector.DoubleArrayVector;
import jse.math.vector.IVector;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

/**
 * 基于其他元素基组的一个镜像基组，其对于自身元素和对应的镜像元素种类会进行交换
 * <p>
 * 目前主要用于实现 ising 模型
 * @author liqa
 */
public class MirrorBasis extends Basis {
    
    private final Basis mMirrorBasis;
    private final int mMirrorType, mThisType;
    public MirrorBasis(Basis aMirrorBasis, int aMirrorType, int aThisType) {
        if (aMirrorBasis instanceof MirrorBasis) throw new IllegalArgumentException("MirrorBasis MUST NOT be Mirror");
        mMirrorBasis = aMirrorBasis;
        mMirrorType = aMirrorType;
        mThisType = aThisType;
    }
    public Basis mirrorBasis() {return mMirrorBasis;}
    public int mirrorType() {return mMirrorType;}
    public int thisType() {return mThisType;}
    
    @Override public MirrorBasis threadSafeRef() {
        return new MirrorBasis(mMirrorBasis.threadSafeRef(), mMirrorType, mThisType);
    }
    // 虽然 mirror 本身没有参数，但是为了逻辑一致这里依旧转发这些接口
    @Override public void initParameters() {mMirrorBasis.initParameters();}
    @Override public IVector parameters() {return mMirrorBasis.parameters();}
    @Override public boolean hasParameters() {return mMirrorBasis.hasParameters();}
    
    @SuppressWarnings({"rawtypes", "unchecked"})
    @Override public void save(Map rSaveTo) {
        rSaveTo.put("type", "mirror");
        rSaveTo.put("mirror", mMirrorType);
    }
    
    @Override public double rcut() {return mMirrorBasis.rcut();}
    @Override public int size() {return mMirrorBasis.size();}
    @Override public int atomTypeNumber() {return mMirrorBasis.atomTypeNumber();}
    @Override public boolean hasSymbol() {return mMirrorBasis.hasSymbol();}
    @Override public String symbol(int aType) {return mMirrorBasis.symbol(aType);}
    
    @Override protected void shutdown_() {
        mMirrorBasis.shutdown();
    }
    
    private final IntList mMirrorNlType = new IntList(16);
    private void buildNlType_(IntList aNlType) {
        mMirrorNlType.clear();
        mMirrorNlType.ensureCapacity(aNlType.size());
        aNlType.forEach(type -> {
            if (type == mThisType) mMirrorNlType.add(mMirrorType);
            else if (type == mMirrorType) mMirrorNlType.add(mThisType);
            else mMirrorNlType.add(type);
        });
    }
    
    @Override
    public final void forward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector rFp, DoubleList rForwardCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 现在可以有任意的调用顺序，因此这里简单处理都进行一次缓存
        buildNlType_(aNlType);
        mMirrorBasis.forward(aNlDx, aNlDy, aNlDz, mMirrorNlType, rFp, rForwardCache, aFullCache);
    }
    @Override
    public final void backward(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aGradFp, DoubleArrayVector rGradPara, DoubleList aForwardCache, DoubleList rBackwardCache, boolean aKeepCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 现在可以有任意的调用顺序，因此这里简单处理都进行一次缓存
        buildNlType_(aNlType);
        mMirrorBasis.backward(aNlDx, aNlDy, aNlDz, mMirrorNlType, aGradFp, rGradPara, aForwardCache, rBackwardCache, aKeepCache);
    }
    @Override
    public final void forwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList rFx, DoubleList rFy, DoubleList rFz, DoubleList aForwardCache, DoubleList rForwardForceCache, boolean aFullCache) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 现在可以有任意的调用顺序，因此这里简单处理都进行一次缓存
        buildNlType_(aNlType);
        mMirrorBasis.forwardForce(aNlDx, aNlDy, aNlDz, mMirrorNlType, aNNGrad, rFx, rFy, rFz, aForwardCache, rForwardForceCache, aFullCache);
    }
    @Override
    public final void backwardForce(DoubleList aNlDx, DoubleList aNlDy, DoubleList aNlDz, IntList aNlType, DoubleArrayVector aNNGrad, DoubleList aGradFx, DoubleList aGradFy, DoubleList aGradFz, DoubleArrayVector rGradNNGrad, @Nullable DoubleArrayVector rGradPara,
                                    DoubleList aForwardCache, DoubleList aForwardForceCache, DoubleList rBackwardCache, DoubleList rBackwardForceCache, boolean aKeepCache, boolean aFixBasis) {
        if (isShutdown()) throw new IllegalStateException("This Basis is dead");
        // 现在可以有任意的调用顺序，因此这里简单处理都进行一次缓存
        buildNlType_(aNlType);
        mMirrorBasis.backwardForce(aNlDx, aNlDy, aNlDz, mMirrorNlType, aNNGrad, aGradFx, aGradFy, aGradFz, rGradNNGrad, rGradPara, aForwardCache, aForwardForceCache, rBackwardCache, rBackwardForceCache, aKeepCache, aFixBasis);
    }
}
